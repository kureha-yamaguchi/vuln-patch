"""End-to-end: most-recent-CVE PoC → variant harnesses on the vulnerable
version → run them on HEAD to surface siblings the fix missed.

Pipeline (mirrors src/java/run.py, but for OSS-Fuzz / libFuzzer):

    OsvClient          (osv.py)       pick newest public CVE + fix commit
    OssFuzz            (ossfuzz.py)    clone repo, check out vuln(=fix~1) & HEAD
    [reproduce]        (ossfuzz.py)    optional: confirm PoC crashes on vuln
    DiffAnalyzer       (analysis.py)   fix diff → touched functions + reachable
    LibFuzzerPrompt…   (prompts.py)    build the steered prompt
    HarnessGenerator   (llm.py)        call the LLM  [shared with Java]
    HarnessCampaign    (campaign.py)   generate→build→verify on the vuln build
    OssFuzz.run_fuzzer (ossfuzz.py)    run accepted harnesses on HEAD → siblings

Example:
    export OSS_FUZZ_DIR=~/oss-fuzz OPENAI_API_KEY=sk-...
    uv run -m oss_fuzz.run --project libxml2 -n 5 --fuzz-timeout 300

Offline wiring check (no Docker, no network, no LLM):
    uv run -m oss_fuzz.run --project libxml2 --osv-json fixture.json --dry-run
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config
from typing import List

from oss_fuzz.artifacts import RunArtifacts
from oss_fuzz.bugclass import SEMANTIC, ORACLE_HARNESS, classify_forced
from oss_fuzz.osv import OsvClient, CveTarget, rank_records
from oss_fuzz.ossfuzz import OssFuzz, harness_includes
from oss_fuzz.analysis import DiffAnalyzer
from oss_fuzz.prompts import LibFuzzerPromptBuilder
from oss_fuzz.campaign import HarnessCampaign
from oss_fuzz.targets import find_candidates


def parse_args():
    p = argparse.ArgumentParser(
        description="Find sibling bugs an OSS-Fuzz CVE's fix missed.")
    p.add_argument("--project", default=None,
                   help="OSS-Fuzz project name (projects/<name>/). Omit with "
                        "--list-candidates to discover one, or with "
                        "--auto-project to run the newest CVE found.")
    p.add_argument("--list-candidates", action="store_true",
                   help="list OSS-Fuzz projects this front-end can drive (C/C++ "
                        "+ libFuzzer + the chosen sanitizer) that have a public "
                        "CVE with a fix commit, then exit")
    p.add_argument("--auto-project", action="store_true",
                   help="pick the project automatically: the newest CVE among "
                        "the discovered candidates")
    p.add_argument("--candidate-limit", type=int, default=10,
                   help="stop the candidate sweep after this many hits "
                        "(default 10; each hit costs one OSV query)")
    p.add_argument("--max-projects", type=int, default=None,
                   help="cap how many projects the candidate sweep probes")
    p.add_argument("--osv-json", default=None,
                   help="load OSV records from a JSON file instead of querying "
                        "the network (a list of records, or {'vulns': [...]}). "
                        "Useful for reproducibility and offline runs.")
    p.add_argument("--cve", default=None,
                   help="force a specific CVE id (must be in the OSV results)")
    p.add_argument("--require-cve", action="store_true",
                   help="only consider OSV entries carrying a CVE alias. "
                        "OSS-Fuzz's own OSV records (OSV-YYYY-NNNN) do not, so "
                        "this usually selects nothing; off by default, where "
                        "any disclosed bug with a fix commit is eligible.")
    p.add_argument("--sanitizer", default=None,
                   help="address/undefined/memory (default: the CVE's own, "
                        "else config.OSS_FUZZ_SANITIZER)")
    p.add_argument("--bug-kind", choices=("auto", "crashing", "semantic"),
                   default="auto",
                   help="override how the bug manifests. 'crashing' = a "
                        "sanitizer reports it; 'semantic' = it does not crash, "
                        "so the harness must carry its own oracle. Default "
                        "'auto' reads the OSV record's crash type "
                        "(see oss_fuzz/bugclass.py)")
    p.add_argument("--skip-semantic", action="store_true",
                   help="only run on bugs the runtime reports: skip OSV "
                        "records whose crash type says NOTHING at run time "
                        "would report a sibling, so the harness would have to "
                        "supply the oracle. Project-assert bugs are kept — the "
                        "library aborts by itself. Mirrors the Java "
                        "front-end's --skip_semantic")
    p.add_argument("--max-target-tries", type=int, default=5,
                   help="how many OSV records (newest first) to try before "
                        "giving up on finding one whose fix diff touches C/C++ "
                        "source (default 5)")
    p.add_argument("--allow-empty-context", action="store_true",
                   help="run even when the fix diff yields no touched "
                        "functions. The prompt then carries no variant-analysis "
                        "steering, so the run does not test the heuristic.")
    p.add_argument("-n", "--target-successes", type=int, default=5)
    p.add_argument("-m", "--max-attempts", type=int, default=30)
    p.add_argument("--reachable-node-cap", type=int, default=None,
                   help="max functions in the introspector reachable-set BFS "
                        "(default: config.REACHABLE_NODE_CAP)")
    p.add_argument("--reachable-max-depth", type=int, default=None,
                   help="max call-graph depth for the reachable-set BFS "
                        "(default: config.REACHABLE_MAX_DEPTH)")
    p.add_argument("--verify-timeout", type=int,
                   default=config.OSS_FUZZ_VERIFY_TIMEOUT,
                   help="seconds to run each harness on the VULN build (gate)")
    p.add_argument("--fuzz-timeout", type=int,
                   default=config.OSS_FUZZ_FUZZ_TIMEOUT,
                   help="seconds to run each accepted harness on HEAD")
    p.add_argument("--reproducer", default=None,
                   help="path to the original PoC testcase; if given, we "
                        "confirm it crashes the vuln build and not HEAD")
    p.add_argument("--harness-build", choices=("auto", "crib", "overwrite"),
                   default="auto",
                   help="how to get the generated harness compiled. 'crib' "
                        "copies a $LIB_FUZZING_ENGINE compile line out of "
                        "build.sh (needs one to exist); 'overwrite' replaces an "
                        "existing harness source in place and lets the "
                        "project's own build system compile it (works for "
                        "CMake/Meson/script-driven projects, which are the "
                        "majority); 'auto' (default) cribs when possible and "
                        "overwrites otherwise")
    p.add_argument("--base-harness", default=None,
                   help="harness source to overwrite, relative to the target's "
                        "repo root. Only used by the overwrite strategy; "
                        "auto-detected when omitted")
    p.add_argument("--oss-fuzz-dir", default=None)
    p.add_argument("--work-dir", default=None)
    p.add_argument("--dry-run", action="store_true",
                   help="print external commands and skip Docker/git/LLM exec")
    p.add_argument("--results-json", default=None,
                   help="append a one-line JSON summary to this path")
    p.add_argument("--artifacts-dir", default=None,
                   help="keep this run's evidence under <dir>/<project>/: the "
                        "generator's input (fix diff, the original bug's "
                        "triggering evidence, the reachable-function set), the "
                        "exact prompt per attempt, every generated harness, "
                        "and the fuzzing engine's own output for every verify "
                        "and HEAD run. Off by default; see oss_fuzz/artifacts.py")
    return p.parse_args()


def _ranked_targets(args) -> List[CveTarget]:
    """All usable OSV targets for the project, newest first."""
    if args.osv_json:
        raw = json.loads(Path(args.osv_json).read_text())
        records = raw.get("vulns", raw) if isinstance(raw, dict) else raw
    else:
        records = OsvClient().query_project(args.project)

    # Pinning a CVE id only makes sense against records that carry the alias,
    # so --cve implies --require-cve.
    if args.cve:
        records = [r for r in records
                   if args.cve in (r.get("aliases") or [])]
    ranked = rank_records(args.project, records,
                          require_cve=args.require_cve or bool(args.cve))
    if not ranked:
        what = "CVE" if (args.require_cve or args.cve) else "disclosed bug"
        hint = ""
        if args.require_cve or args.cve:
            hint = ("\n  OSS-Fuzz OSV records carry no CVE alias; drop "
                    "--require-cve/--cve to accept any disclosed bug that has "
                    "a fix commit.")
        sys.exit(f"No usable public {what} with a fix commit found for project "
                 f"'{args.project}' ({len(records)} OSV record(s) considered)."
                 + hint)
    return ranked


def _emit(path, **rec):
    if not path:
        return
    with open(path, "a") as fh:
        fh.write(json.dumps(rec) + "\n")


def _fail(msg: str, dry_run: bool) -> None:
    """Abort on a targeting problem — unless this is a --dry-run wiring check,
    where the point is to exercise the control flow without a real checkout."""
    if dry_run:
        print(f"WARNING (ignored under --dry-run): {msg}")
        return
    sys.exit(msg)


def _abort_environment(msg: str) -> None:
    """Exit 2: the environment is broken, so this is not a result about the fix.

    A suite has to be able to tell "could not run" from "ran and found nothing"
    (0) and from a usage error (1); collapsing the first into either of the
    others reports a broken box as a clean sweep.
    """
    sys.stderr.write(f"\nRUN ABORTED — {msg}\n"
                     "This is not a result about the fix; fix the environment "
                     "and re-run.\n")
    sys.exit(2)


def _clone_fix_source(of, cand, project_yaml_repo):
    """Clone the repo the fix landed in, or return None if none of them serve it.

    Two URLs can name that repo and they go stale in opposite ways. The OSV
    record carries the repo the fix landed in — historically exact, but frozen at
    disclosure. ``project.yaml`` carries where OSS-Fuzz builds the project from
    today — maintained, but it can be a different repo entirely. cryptofuzz needs
    both: it moved from ``guidovranken/`` to ``MozillaSecurity/``, OSS-Fuzz
    updated project.yaml, OSV still points at the old URL (which now 404s), and
    the fix commit is present in the new repo.

    So try each in turn, and require the fix commit to actually be there — a
    fallback clone of a repo that lacks it would otherwise sail on with a bad
    revision, since ``parent_commit`` resolves a missing commit to a literal
    string.
    """
    seen = set()
    for url in (cand.main_repo, project_yaml_repo):
        if not url or url in seen:
            continue
        seen.add(url)
        try:
            repo = of.clone_source(url)
        except RuntimeError as exc:
            print(f"  cannot clone {url}: {exc}")
            continue
        if not of.has_commit(repo, cand.fixed_commit):
            print(f"  {url} has no commit {cand.fixed_commit[:12]}; "
                  "it is not the repo this fix landed in")
            continue
        cand.main_repo = url
        return repo
    return None


def _print_candidates(cands) -> None:
    print(f"\n{len(cands)} candidate project(s) — newest first:\n")
    print(f"  {'project':<24} {'lang':<5} {'advisory':<18} {'published':<11} "
          f"{'kind':<9} crash type")
    for c in cands:
        # Most OSS-Fuzz records have no CVE, so show whichever id exists and
        # the crash type — that, not the id, is what tells you if the bug class
        # matches the sanitizer you are about to run. The kind column says
        # whether a sanitizer will report a sibling at all, which decides
        # whether the run needs a harness-written oracle.
        advisory = c.cve_id or c.target.osv_id
        print(f"  {c.project:<24} {c.language:<5} {advisory:<18} "
              f"{c.published[:10]:<11} {c.target.bug_class.kind:<9} "
              f"{c.target.crash_type or '-'}")
    print("\nRun one with:  uv run -m oss_fuzz.run --project <project>")


def main():
    args = parse_args()

    of = OssFuzz(oss_fuzz_dir=args.oss_fuzz_dir, work_dir=args.work_dir,
                 dry_run=args.dry_run)

    # 0) The checkout has to be a real oss-fuzz clone before anything below
    #    means anything — otherwise this surfaces much later as a confusing
    #    helper.py traceback or a silently empty candidate list.
    problems = of.checkout_problems()
    if problems:
        _fail("Unusable OSS-Fuzz checkout:\n"
              + "\n".join(f"  - {p}" for p in problems)
              + "\n  Point --oss-fuzz-dir or $OSS_FUZZ_DIR at a "
                "google/oss-fuzz clone.", args.dry_run)

    for warning in of.host_warnings():
        print(f"WARNING: {warning}")

    sanitizer_pref = args.sanitizer or config.OSS_FUZZ_SANITIZER

    # 0b) Target discovery. Without this you have to already know a project
    #     name that is C/C++, builds with libFuzzer, and has a public CVE —
    #     true for only a small fraction of the ~1300 projects in a checkout.
    preselected = None
    if args.list_candidates or (args.auto_project and not args.project):
        print(f"scanning OSS-Fuzz projects for {sanitizer_pref}-capable C/C++ "
              "targets with a public CVE ...")
        cands = find_candidates(
            of, sanitizer=sanitizer_pref, limit=args.candidate_limit,
            max_projects=args.max_projects,
            require_cve=args.require_cve or bool(args.cve),
            verbose=True)
        if not cands:
            sys.exit("No viable candidate projects found. Widen the sweep with "
                     "--max-projects / --candidate-limit, or try another "
                     "--sanitizer.")
        if args.list_candidates:
            _print_candidates(cands)
            sys.exit(0)
        preselected = cands[0]
        args.project = preselected.project
        print(f"\nauto-selected project '{args.project}' "
              f"({preselected.cve_id or preselected.target.osv_id})")

    if not args.project:
        sys.exit("--project is required (or use --list-candidates to discover "
                 "one, or --auto-project to pick the newest automatically).")

    # 0c) Preflight the chosen project: language, engine, sanitizer, main_repo.
    #     This is the check that stops a python/go/jvm project — most of the
    #     OSS-Fuzz corpus — from consuming a clone, a Docker image build and an
    #     LLM budget before failing to compile a C/C++ harness.
    sup = of.check_support(args.project, sanitizer_pref)
    if not sup.supported:
        _fail(f"Project '{args.project}' is not a target this front-end can "
              "drive:\n" + "\n".join(f"  - {r}" for r in sup.reasons)
              + "\n  See viable projects with --list-candidates.", args.dry_run)

    # 0d) Somewhere to keep this run's evidence. Only now, because the files go
    #     under the project name and --auto-project does not know it until 0b —
    #     and there is nothing worth keeping about a project that fails 0c.
    artifacts = (RunArtifacts(args.artifacts_dir, args.project)
                 if args.artifacts_dir else None)
    of.artifacts = artifacts
    if artifacts:
        print(f"artifacts    : {artifacts.dir}")

    # 1) Pick a target whose fix diff is actually analysable.
    #
    #    OSS-Fuzz 'fixed' commits come from automated bisection and a real
    #    fraction of them do not touch source: c-blosc2's newest record points
    #    at a commit that only adds PNG/SVG diagrams. Those produce an empty
    #    root-cause context, hence a prompt with no variant-analysis steering —
    #    the whole research heuristic — which would then be spent on a full
    #    Docker build + LLM + fuzzing budget and recorded as "0 siblings" as if
    #    the method had been fairly tested. So we walk the ranking (newest
    #    first) and take the first record that yields touched functions.
    ranked = _ranked_targets(args)
    if preselected:
        ranked = ([preselected.target]
                  + [t for t in ranked
                     if t.osv_id != preselected.target.osv_id])
    tries = min(max(1, args.max_target_tries), len(ranked))
    print(f"usable OSV records: {len(ranked)} (will try up to {tries})")

    target = None
    context = None
    repo = vuln = None
    vuln_commit = head_commit = None

    for i, cand in enumerate(ranked[:tries], 1):
        cand.language = cand.language or sup.language or "c++"
        cand.main_repo = cand.main_repo or sup.main_repo
        print(f"\n-- candidate {i}/{tries}: {cand.osv_id} "
              f"{cand.cve_id or '(no CVE)'} --")
        print(f"published    : {cand.published[:10]}")
        print(f"fixed commit : {cand.fixed_commit}")
        if cand.crash_type:
            print(f"crash type   : {cand.crash_type}")
        print(f"bug kind     : {cand.bug_class.describe()}")
        # Semantic bugs cost the same clone, build and LLM budget as crashing
        # ones but answer a different question, so a suite measuring the
        # crash-gated method can exclude them here — before the clone.
        # Gated on needs_harness_oracle rather than is_semantic: the two are
        # equivalent by BugClass's invariant, but this is the fact being relied
        # on — "the runtime reports nothing, so the crash gate cannot work".
        # A project-assert bug is a logic defect that the runtime DOES report,
        # so it stays in scope, matching the Java front-end's treatment of an
        # escaping invariant-check throwable.
        if args.skip_semantic and cand.bug_class.needs_harness_oracle:
            print("  SKIPPED: --skip-semantic and nothing at run time would "
                  "report a sibling of this bug")
            continue
        if cand.crash_state:
            print(f"crash state  : {' <- '.join(cand.crash_state)}")
        if cand.report_url:
            print(f"report       : {cand.report_url}")
        if not cand.main_repo:
            print("  no main_repo resolvable from OSV or project.yaml; skipping")
            continue

        # An unclonable source is a broken environment, not a bad harness and
        # not a usage error: the raw failure used to surface as an unhandled
        # traceback under the suite's catch-all exit 1.
        repo = _clone_fix_source(of, cand, sup.main_repo)
        if repo is None:
            reason = (f"no repo serving the fix commit "
                      f"{(cand.fixed_commit or '')[:12]} for '{args.project}'")
            _emit(args.results_json, project=args.project, osv_id=cand.osv_id,
                  cve=cand.cve_id, sanitizer=cand.sanitizer or "",
                  bug_kind=cand.bug_class.kind, oracle=cand.bug_class.oracle,
                  attempts=0, harnesses_accepted=0, siblings=[],
                  oracle_claims=[], infra_error=reason)
            _abort_environment(
                f"{reason}.\n  Tried: {cand.main_repo}"
                + (f", {sup.main_repo}" if sup.main_repo != cand.main_repo
                   else "")
                + "\n  The repo may have moved or been deleted — check "
                f"projects/{args.project}/project.yaml against upstream.")
        vc = of.parent_commit(repo, cand.fixed_commit)
        hc = of.head_commit(repo)
        print(f"vuln commit  : {vc}")
        print(f"head commit  : {hc}")
        wt = of.checkout(repo, vc, "vuln")

        diff = of.diff(repo, vc, cand.fixed_commit)
        ctx = DiffAnalyzer(
            language=cand.language,
            reachable_node_cap=args.reachable_node_cap,
            reachable_max_depth=args.reachable_max_depth,
        ).analyze(diff, wt.path)
        print("-- root-cause context --")
        print(json.dumps(ctx.as_dict(), indent=2))

        if ctx.functions or args.allow_empty_context or args.dry_run:
            if not ctx.functions:
                print("WARNING: no touched functions extracted; prompt will "
                      "rely on the raw diff only and carries NO steering.")
            target, context = cand, ctx
            vuln, vuln_commit, head_commit = wt, vc, hc
            break

        # Say which of the two it is. "Touches no C/C++ function" over a diff of
        # 773 lines of C++ sent a reader looking for a parser bug when the real
        # situation was an OSV 'fixed' commit that only adds a regression test
        # (wt, 20260812) — a different problem with a different answer.
        why = "touches no C/C++ function"
        if ctx.skipped_paths:
            why = ("touches only tests, harnesses or tooling: "
                   + ", ".join(ctx.skipped_paths[:3]))
        print(f"  REJECTED: the fix diff ({len(diff)} bytes) {why}, so the "
              "prompt would carry no steering. Trying the next-newest record.")

    if target is None:
        extra = (" or was skipped as semantic (--skip-semantic)"
                 if args.skip_semantic else "")
        sys.exit(
            f"None of the {tries} newest OSV record(s) for '{args.project}' has "
            "a fix diff that touches C/C++ source" + extra + ", so no steered "
            "prompt is possible.\n  Raise --max-target-tries, pick another "
            "--project (--list-candidates), or pass --allow-empty-context to "
            "run unsteered anyway.")

    sanitizer = (args.sanitizer or target.sanitizer
                 or config.OSS_FUZZ_SANITIZER)
    # How this bug manifests decides the prompt's oracle contract, the
    # campaign's pre-build gate, the fuzzing flags and how a HEAD finding is
    # reported. Everything downstream reads it from here.
    bug_class = (target.bug_class if args.bug_kind == "auto"
                 else classify_forced(args.bug_kind, target.crash_type))
    ext = of.harness_ext(target.language)
    print(f"\nselected     : {target.osv_id} "
          f"{target.cve_id or '(no CVE)'}  [{target.language}]")
    print(f"sanitizer    : {sanitizer}")
    print(f"bug kind     : {bug_class.describe()}")
    if bug_class.needs_harness_oracle:
        print("               → harnesses must carry a tagged [oracle:<id>] "
              "check; findings are claims, not sanitizer reports")

    # How the harness will be compiled. Decided here, before build_image, because
    # a demanded-but-impossible strategy should fail before pulling gigabytes.
    placement = of.plan_harness(args.project, vuln, target.fuzz_target, ext,
                                mode=args.harness_build,
                                base_harness=args.base_harness)
    if placement is None:
        _fail(f"--harness-build overwrite needs an existing libFuzzer harness "
              f"in the vulnerable checkout of '{args.project}', and none was "
              f"found (looked for a file defining LLVMFuzzerTestOneInput). "
              f"Point at one with --base-harness, or use --harness-build auto.",
              args.dry_run)
        placement = of.plan_harness(args.project, vuln, target.fuzz_target, ext,
                                    mode="crib")
    # Overwrite keeps the replaced file's extension, which may not be the one
    # the project's language implies; everything downstream must use it.
    ext = placement.ext
    print(f"harness build: {placement.describe()}")

    # Record the generator's whole input now — before the image pull, the
    # builds and the LLM budget — so a run killed by the suite's wall-clock cap
    # still says what it was steered by. This is the triple the method rests
    # on: the fix diff (and the PoC when one was supplied), the evidence the
    # original bug fired, and the reachable-function set the variant-analysis
    # block ranges over.
    if artifacts:
        path = artifacts.record_generation_input(
            target, context, sanitizer=sanitizer, bug_class=bug_class,
            vuln_commit=vuln_commit, head_commit=head_commit,
            placement=placement, reproducer=args.reproducer)
        if path:
            print(f"generation input recorded: {path}")

    # The bug's own OSV sanitizer can differ from the one we preflighted, so
    # re-check whatever we ended up with rather than the preference.
    if sup.exists and sanitizer not in sup.sanitizers:
        _fail(f"{target.osv_id} was found with the '{sanitizer}' sanitizer, "
              f"which project '{args.project}' does not build "
              f"(project.yaml sanitizers: {', '.join(sup.sanitizers)}). "
              "Override with --sanitizer.", args.dry_run)

    # 2) HEAD worktree + the project's build image. Deliberately after the
    #    analysis above: build_image pulls gigabytes, and there is no point
    #    paying that before we know we have a steerable target.
    head = of.checkout(repo, head_commit, "head")
    of.build_image(args.project)

    # 3) Optional PoC sanity: crashes on vuln, clean on HEAD (needs the harness
    #    name the bug was found on + a local testcase).
    if args.reproducer and target.fuzz_target:
        print("\n-- PoC sanity check --")
        vc_out = of.reproduce(args.project, target.fuzz_target,
                              args.reproducer, sanitizer)
        print(f"  vuln build: {'CRASH' if vc_out.triggered else 'no crash'} "
              f"({vc_out.crash_reason}, found by {vc_out.found_by})")

    # 5) Campaign: generate + build + trigger-gate on the vulnerable build.
    prompt_builder = LibFuzzerPromptBuilder(language=target.language)
    repro_hint = None  # bytes of the PoC could be summarised here if desired

    # Under overwrite the harness IS the project's existing target file, so name
    # it in the prompt rather than inventing one.
    harness_label = "vp_harness"
    if placement.mode == "overwrite" and placement.rel_path:
        harness_label = os.path.splitext(os.path.basename(placement.rel_path))[0]

    # The file we are about to overwrite compiles in this project's own build,
    # so its include block is the one thing in the prompt that is known to
    # resolve. Without it the model guesses header paths, and a wrong guess
    # costs a full Docker build to find out.
    base_includes = harness_includes(vuln.path, placement.rel_path)

    def prompt_factory(covered, signatures):
        return prompt_builder.build(
            context=context, covered_functions=covered,
            found_signatures=signatures, harness_name=harness_label,
            reproducer_hint=repro_hint,
            crash_type=target.crash_type, crash_state=target.crash_state,
            harness_ext=placement.ext, bug_class=bug_class,
            base_harness=placement.rel_path, base_includes=base_includes)

    if args.dry_run:
        generator = _StubGenerator()
    else:
        from llm import HarnessGenerator  # deferred: pulls in openai SDK
        generator = HarnessGenerator(temperature=0.6, top_p=1.0)
    campaign = HarnessCampaign(
        generator=generator, oss_fuzz=of, project=args.project,
        vuln_checkout=vuln, sanitizer=sanitizer, ext=ext, placement=placement,
        target_successes=args.target_successes,
        max_attempts=args.max_attempts, verify_seconds=args.verify_timeout,
        bug_class=bug_class, artifacts=artifacts)
    result = campaign.run(prompt_factory)

    print(f"\n== campaign: {result.achieved}/{result.target_successes} "
          f"accepted in {result.attempts} attempts ==")

    # An environment that cannot build anything is not a negative result. Exit
    # distinctly (2) so a suite run does not average it in as "0 siblings".
    if result.infra_error and result.achieved == 0:
        _emit(args.results_json, project=args.project, osv_id=target.osv_id,
              cve=target.cve_id, sanitizer=sanitizer, attempts=result.attempts,
              bug_kind=bug_class.kind, oracle=bug_class.oracle,
              harnesses_accepted=0, siblings=[], oracle_claims=[],
              infra_error=result.infra_error,
              artifacts=artifacts.dir if artifacts else None)
        _abort_environment("the build environment could not build any "
                           f"harness:\n  {result.infra_error}")

    # 6) Run each accepted harness on HEAD. A crash here = sibling the fix missed.
    #    HEAD needs its own placement: under overwrite the harness file may have
    #    been renamed or moved upstream between the vulnerable commit and HEAD.
    head_placement = of.plan_harness(args.project, head, target.fuzz_target,
                                     ext, mode=placement.mode,
                                     base_harness=args.base_harness)
    if head_placement is None:
        print(f"\nWARNING: '{placement.rel_path}' does not exist in the HEAD "
              "checkout (renamed or removed upstream?), so accepted harnesses "
              "cannot be rebuilt there. Re-run with --base-harness naming the "
              "HEAD path. Reporting 0 siblings, which is NOT a result about "
              "the fix.")
    elif head_placement.mode == "overwrite" and \
            head_placement.rel_path != placement.rel_path:
        print(f"\nnote: HEAD's harness is {head_placement.rel_path} "
              f"(vuln used {placement.rel_path})")

    siblings = []   # runtime-confirmed: a sanitizer or the project's own check
    claims = []     # the harness's own oracle fired; true only if it is right
    head_runs = 0   # accepted harnesses that actually got to run on HEAD
    for gen in result.successful if head_placement else []:
        print(f"\n-- HEAD run: {gen.harness_name} --")
        out_bin = of.build_harness(args.project, head, gen.harness_name,
                                   gen.source, gen.ext, sanitizer,
                                   placement=head_placement)
        if out_bin is None:
            print("  did not build against HEAD (API drift?); skipping")
            continue
        head_runs += 1
        outcome = of.run_fuzzer(args.project,
                                head_placement.runtime_name(gen.harness_name),
                                args.fuzz_timeout, sanitizer,
                                bug_class=bug_class,
                                log_tag=f"head_{gen.harness_name}")
        if outcome.triggered:
            # A sanitizer report on HEAD is a bug, full stop. A harness's own
            # oracle firing is a bug *if the relation it asserts is true*, and
            # nothing here can decide that — reporting the two as one number
            # would put unreviewed claims into the sibling count.
            label = ("SIBLING BUG CLAIM (needs triage)" if outcome.needs_triage
                     else "SIBLING BUG on HEAD")
            print(f"  *** {label} *** [{outcome.signature}] "
                  f"found by {outcome.found_by} "
                  f"artifact={outcome.artifact_path}")
            (claims if outcome.needs_triage else siblings).append({
                "harness": gen.harness_name,
                "signature": outcome.signature,
                "found_by": outcome.found_by,
                "artifact": outcome.artifact_path,
                # The engine output behind this claim. `artifact` is the input
                # that crashed; this is the report that says what it did.
                "fuzz_log": (os.path.join(artifacts.dir, "fuzz",
                                          f"head_{gen.harness_name}.log")
                             if artifacts else None)})
        else:
            print("  clean on HEAD (fix covers this variant)")

    # 7) Report. Harnesses that triggered on the vulnerable build but never ran
    #    on HEAD answer nothing about the fix, and "0 siblings" reads as "the fix
    #    covers this": the 20260812 run reported open62541 clean after all three
    #    of its HEAD builds failed on generated sources left over from the
    #    vulnerable commit. Distinct exit code, distinct line, distinct status
    #    in the suite's table.
    head_untested = result.achieved > 0 and head_runs == 0
    print("\n" + "#" * 50)
    if head_untested:
        print(f"{target.cve_id or target.osv_id} [{bug_class.kind}]: "
              f"INCONCLUSIVE — {result.achieved} harness(es) triggered on the "
              "vulnerable build and none of them could be run on HEAD, so HEAD "
              "was never tested. This is not a result about the fix.")
    print(f"{target.cve_id or target.osv_id} [{bug_class.kind}]: "
          f"{len(siblings)} confirmed sibling(s) on HEAD from "
          f"{head_runs} harness(es) run there ({result.achieved} accepted)")
    for s in siblings:
        print(f"  - {s['harness']}: {s['signature']}  ({s['artifact']})")
    if claims:
        print(f"\n{len(claims)} unconfirmed oracle claim(s) — each one is only "
              "as good as the relation its harness asserts; read the harness "
              "before reporting any of these upstream:")
        for c in claims:
            print(f"  - {c['harness']}: {c['signature']}  ({c['artifact']})")
    print("#" * 50)
    if artifacts:
        print(f"\ninput, prompts and engine logs: {artifacts.dir}")

    _emit(args.results_json, cve=target.cve_id, project=args.project,
          osv_id=target.osv_id, sanitizer=sanitizer,
          crash_type=target.crash_type, bug_kind=bug_class.kind,
          oracle=bug_class.oracle, vuln_commit=vuln_commit,
          head_commit=head_commit, harnesses_accepted=result.achieved,
          attempts=result.attempts, siblings=siblings,
          oracle_claims=claims, harnesses_run_on_head=head_runs,
          # Which reachable set steered the prompts. The variant-analysis
          # heuristic is the method under test, and it degrades silently when
          # fuzz-introspector times out — all five projects in the 20260812 run
          # were steered by the fallback, which only the per-project log said.
          reachable_source=context.reachable_source,
          artifacts=artifacts.dir if artifacts else None)

    if not args.dry_run:
        of.cleanup_checkouts(repo)
    # 3 stays "the fix missed something, confirmed by the runtime". Oracle
    # claims get their own code rather than being folded into 3 (which would
    # inflate the headline result with unreviewed relations) or into 0 (which
    # would hide the only findings a semantic run can produce). 5 is "HEAD was
    # never tested", which is not a finding and not a clean bill of health.
    if siblings:
        sys.exit(3)
    if claims:
        sys.exit(4)
    sys.exit(5 if head_untested else 0)


class _StubGenerator:
    """Deterministic stand-in for the LLM under --dry-run: returns a minimal
    well-formed harness so the whole control flow can be exercised offline.

    It carries a tagged, aborting oracle it never actually reaches, so that
    --dry-run also exercises the semantic path: without one, the pre-build
    oracle gate would reject every stub harness and a semantic wiring check
    would never reach the build at all."""
    def generate(self, messages):
        return ('```c\n#include <stdint.h>\n#include <stddef.h>\n'
                '#include <stdio.h>\n#include <stdlib.h>\n'
                'int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)'
                ' {\n  (void)data;\n'
                '  if (size == (size_t)-1) {\n'
                '    fprintf(stderr, "[oracle:stub] unreachable\\n");\n'
                '    abort();\n  }\n  return 0;\n}\n```')


if __name__ == "__main__":
    main()