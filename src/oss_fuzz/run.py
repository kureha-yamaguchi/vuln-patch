"""End-to-end: most-recent-CVE PoC → variant harnesses on the vulnerable
version → run them on HEAD to surface siblings the fix missed.

Pipeline (mirrors src/java/run.py, but for OSS-Fuzz / libFuzzer):

    OsvClient          (osv.py)       pick newest public CVE + fix commit
    OssFuzz            (ossfuzz.py)    clone repo, worktree vuln(=fix~1) & HEAD
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
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config
from oss_fuzz.osv import OsvClient, CveTarget, select_from_records
from oss_fuzz.ossfuzz import OssFuzz
from oss_fuzz.analysis import DiffAnalyzer
from oss_fuzz.prompts import LibFuzzerPromptBuilder
from oss_fuzz.campaign import HarnessCampaign


def parse_args():
    p = argparse.ArgumentParser(
        description="Find sibling bugs an OSS-Fuzz CVE's fix missed.")
    p.add_argument("--project", required=True,
                   help="OSS-Fuzz project name (projects/<name>/)")
    p.add_argument("--osv-json", default=None,
                   help="load OSV records from a JSON file instead of querying "
                        "the network (a list of records, or {'vulns': [...]}). "
                        "Useful for reproducibility and offline runs.")
    p.add_argument("--cve", default=None,
                   help="force a specific CVE id (must be in the OSV results)")
    p.add_argument("--sanitizer", default=None,
                   help="address/undefined/memory (default: the CVE's own, "
                        "else config.OSS_FUZZ_SANITIZER)")
    p.add_argument("-n", "--target-successes", type=int, default=5)
    p.add_argument("-m", "--max-attempts", type=int, default=30)
    p.add_argument("--verify-timeout", type=int,
                   default=config.OSS_FUZZ_VERIFY_TIMEOUT,
                   help="seconds to run each harness on the VULN build (gate)")
    p.add_argument("--fuzz-timeout", type=int,
                   default=config.OSS_FUZZ_FUZZ_TIMEOUT,
                   help="seconds to run each accepted harness on HEAD")
    p.add_argument("--reproducer", default=None,
                   help="path to the original PoC testcase; if given, we "
                        "confirm it crashes the vuln build and not HEAD")
    p.add_argument("--oss-fuzz-dir", default=None)
    p.add_argument("--work-dir", default=None)
    p.add_argument("--dry-run", action="store_true",
                   help="print external commands and skip Docker/git/LLM exec")
    p.add_argument("--results-json", default=None,
                   help="append a one-line JSON summary to this path")
    return p.parse_args()


def _select_target(args) -> CveTarget:
    if args.osv_json:
        raw = json.loads(Path(args.osv_json).read_text())
        records = raw.get("vulns", raw) if isinstance(raw, dict) else raw
        if args.cve:
            records = [r for r in records
                       if args.cve in (r.get("aliases") or [])]
        target = select_from_records(args.project, records)
    else:
        client = OsvClient()
        records = client.query_project(args.project)
        if args.cve:
            records = [r for r in records
                       if args.cve in (r.get("aliases") or [])]
        target = select_from_records(args.project, records)
    if target is None:
        sys.exit(f"No usable public CVE found for project '{args.project}'.")
    return target


def _emit(path, **rec):
    if not path:
        return
    with open(path, "a") as fh:
        fh.write(json.dumps(rec) + "\n")


def main():
    args = parse_args()

    # 1) Most recent public CVE + fix boundary.
    target = _select_target(args)
    of = OssFuzz(oss_fuzz_dir=args.oss_fuzz_dir, work_dir=args.work_dir,
                 dry_run=args.dry_run)

    # Fill language / main_repo / sanitizer from project.yaml when OSV lacked them.
    info = of.project_yaml(args.project)
    target.language = target.language or info.get("language", "c++")
    target.main_repo = target.main_repo or info.get("main_repo")
    sanitizer = (args.sanitizer or target.sanitizer
                 or config.OSS_FUZZ_SANITIZER)
    ext = of.harness_ext(target.language)

    print(f"target CVE   : {target.cve_id}  ({target.osv_id})")
    print(f"project      : {target.project}  [{target.language}]")
    print(f"main_repo    : {target.main_repo}")
    print(f"fixed commit : {target.fixed_commit}")
    print(f"sanitizer    : {sanitizer}")
    if not target.main_repo:
        sys.exit("No main_repo resolvable from OSV or project.yaml; cannot "
                 "check out the vulnerable/HEAD sources.")

    # 2) Clone + worktrees: vuln = parent of the fix, head = current HEAD.
    repo = of.clone_source(target.main_repo)
    vuln_commit = of.parent_commit(repo, target.fixed_commit)
    head_commit = of.head_commit(repo)
    print(f"vuln commit  : {vuln_commit}")
    print(f"head commit  : {head_commit}")
    vuln = of.worktree(repo, vuln_commit, "vuln")
    head = of.worktree(repo, head_commit, "head")
    of.build_image(args.project)

    # 3) Optional PoC sanity: crashes on vuln, clean on HEAD (needs the harness
    #    name the bug was found on + a local testcase).
    if args.reproducer and target.fuzz_target:
        print("\n-- PoC sanity check --")
        vc = of.reproduce(args.project, target.fuzz_target,
                          args.reproducer, sanitizer)
        print(f"  vuln build: {'CRASH' if vc.triggered else 'no crash'} "
              f"({vc.crash_reason})")

    # 4) Analyse the fix diff on the vulnerable sources.
    diff = of.diff(repo, vuln_commit, target.fixed_commit)
    context = DiffAnalyzer(language=target.language).analyze(diff, vuln.path)
    print("\n-- root-cause context --")
    print(json.dumps(context.as_dict(), indent=2))
    if not context.functions and not args.dry_run:
        print("WARNING: no touched functions extracted; prompt will rely on "
              "the raw diff only.")

    # 5) Campaign: generate + build + trigger-gate on the vulnerable build.
    prompt_builder = LibFuzzerPromptBuilder(language=target.language)
    repro_hint = None  # bytes of the PoC could be summarised here if desired

    def prompt_factory(covered, signatures):
        return prompt_builder.build(
            context=context, covered_functions=covered,
            found_signatures=signatures, harness_name="vp_harness",
            reproducer_hint=repro_hint)

    if args.dry_run:
        generator = _StubGenerator()
    else:
        from llm import HarnessGenerator  # deferred: pulls in openai SDK
        generator = HarnessGenerator(temperature=0.6, top_p=1.0)
    campaign = HarnessCampaign(
        generator=generator, oss_fuzz=of, project=args.project,
        vuln_checkout=vuln, sanitizer=sanitizer, ext=ext,
        target_successes=args.target_successes,
        max_attempts=args.max_attempts, verify_seconds=args.verify_timeout)
    result = campaign.run(prompt_factory)

    print(f"\n== campaign: {result.achieved}/{result.target_successes} "
          f"accepted in {result.attempts} attempts ==")

    # 6) Run each accepted harness on HEAD. A crash here = sibling the fix missed.
    siblings = []
    for gen in result.successful:
        print(f"\n-- HEAD run: {gen.harness_name} --")
        out_bin = of.build_harness(args.project, head, gen.harness_name,
                                   gen.source, gen.ext, sanitizer)
        if out_bin is None:
            print("  did not build against HEAD (API drift?); skipping")
            continue
        outcome = of.run_fuzzer(args.project, gen.harness_name,
                                args.fuzz_timeout, sanitizer)
        if outcome.triggered:
            print(f"  *** SIBLING BUG on HEAD *** [{outcome.signature}] "
                  f"artifact={outcome.artifact_path}")
            siblings.append({"harness": gen.harness_name,
                             "signature": outcome.signature,
                             "artifact": outcome.artifact_path})
        else:
            print("  clean on HEAD (fix covers this variant)")

    # 7) Report.
    print("\n" + "#" * 50)
    print(f"CVE {target.cve_id}: {len(siblings)} sibling(s) on HEAD "
          f"from {result.achieved} harness(es)")
    for s in siblings:
        print(f"  - {s['harness']}: {s['signature']}  ({s['artifact']})")
    print("#" * 50)

    _emit(args.results_json, cve=target.cve_id, project=args.project,
          osv_id=target.osv_id, vuln_commit=vuln_commit,
          head_commit=head_commit, harnesses_accepted=result.achieved,
          attempts=result.attempts, siblings=siblings)

    if not args.dry_run:
        of.cleanup_worktrees(repo)
    sys.exit(0 if not siblings else 3)


class _StubGenerator:
    """Deterministic stand-in for the LLM under --dry-run: returns a minimal
    well-formed harness so the whole control flow can be exercised offline."""
    def generate(self, messages):
        return ('```c\n#include <stdint.h>\n#include <stddef.h>\n'
                'int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)'
                ' {\n  (void)data; (void)size; return 0;\n}\n```')


if __name__ == "__main__":
    main()
