"""End-to-end driver: recent CVE -> generate variants on the vulnerable version
-> re-run on HEAD. A crash on HEAD is a sibling input the fix failed to cover.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

from . import generate, osv
from .ossfuzz import OssFuzz

HARNESS_EXTS = (".cc", ".cpp", ".cxx", ".c", ".C")


def git(args, cwd, check=True):
    return subprocess.run(["git", *args], cwd=str(cwd), text=True,
                          capture_output=True, check=check)


def rev_parse(cwd, ref):
    return git(["rev-parse", ref], cwd).stdout.strip()


def find_base_harness(src: Path, fuzz_target: str, override):
    """Locate the existing harness source that the original PoC exercised."""
    if override:
        return src / override
    for ext in HARNESS_EXTS:
        hits = list(src.rglob(fuzz_target + ext))
        if hits:
            return hits[0]
    cands = []
    for ext in HARNESS_EXTS:
        for p in src.rglob("*" + ext):
            try:
                if "LLVMFuzzerTestOneInput" in p.read_text(errors="ignore"):
                    cands.append(p)
            except OSError:
                continue
    for p in cands:
        if fuzz_target in p.stem:
            return p
    return cands[0] if cands else None


def infer_target(case, override):
    if override:
        return override
    m = re.search(r"[Ff]uzz(?:er|[ _]target)[:\s]+([A-Za-z0-9_-]+)", case.details or "")
    return m.group(1) if m else None


def overlay(src, rel, variant):
    shutil.copyfile(variant, Path(src) / rel)


def restore(src, rel):
    git(["checkout", "--", str(rel)], src, check=False)


def main(argv=None):
    ap = argparse.ArgumentParser(
        description="Minimal variant analysis on an OSS-Fuzz project (helper.py based)."
    )
    ap.add_argument("--project", required=True, help="OSS-Fuzz project name")
    ap.add_argument("--oss-fuzz", required=True, help="path to a google/oss-fuzz checkout")
    ap.add_argument("--work", default="./work")
    ap.add_argument("--reproducer", help="path to the original public PoC testcase (recommended)")
    ap.add_argument("--fuzz-target", help="fuzz target name (inferred from OSV if omitted)")
    ap.add_argument("--base-harness", help="harness path relative to repo root (inferred if omitted)")
    ap.add_argument("--sanitizer", default="address", help="address/undefined/memory (default: address)")
    ap.add_argument("--num-variants", type=int, default=20)
    ap.add_argument("--vuln-patch-cmd", help="command that generates variants; omitted => no-op fallback")
    ap.add_argument("--vuln-budget", type=int, default=300, help="seconds per variant on vuln (gate)")
    ap.add_argument("--head-budget", type=int, default=900, help="seconds per variant on HEAD")
    ap.add_argument("--skip-gate", action="store_true", help="skip the 'must crash on vuln version' gate")
    ap.add_argument("--no-require-cve", action="store_true")
    args = ap.parse_args(argv)

    work = Path(args.work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    of = OssFuzz(args.oss_fuzz, sanitizer=args.sanitizer)

    # 1. Most recent public, CVE-tagged OSS-Fuzz vulnerability.
    print("[1] querying OSV ...")
    case = osv.latest_case(args.project, require_cve=not args.no_require_cve)
    if not case:
        sys.exit("no matching public vulnerability found on OSV")
    print(f"    {case.osv_id}  CVE={','.join(case.cves) or '-'}"
          f"  fixed={case.fixed[:12]}  repo={case.repo}")

    # 2. Clone, resolve versions, compute fix diff.
    src_head = work / "src_head"
    if not src_head.exists():
        print("[2] cloning target repo ...")
        subprocess.run(["git", "clone", "--quiet", case.repo, str(src_head)], check=True)
    try:
        vuln_commit = rev_parse(src_head, case.fixed + "^")
    except subprocess.CalledProcessError:
        git(["fetch", "--quiet", "origin", case.fixed], src_head, check=False)
        vuln_commit = rev_parse(src_head, case.fixed + "^")
    head_commit = rev_parse(src_head, "HEAD")
    src_vuln = work / "src_vuln"
    if not src_vuln.exists():
        git(["worktree", "add", "--quiet", "--detach", str(src_vuln), vuln_commit], src_head)
    fix_diff = work / "fix.diff"
    fix_diff.write_text(git(["diff", vuln_commit, case.fixed], src_head).stdout)
    print(f"    vuln={vuln_commit[:12]}  head={head_commit[:12]}")

    target = infer_target(case, args.fuzz_target)
    if not target:
        sys.exit("could not infer fuzz target; pass --fuzz-target")

    print("[*] building base image ...")
    of.build_image(args.project)

    # 3. Reproducer / PoC.
    testcase = None
    if args.reproducer:
        testcase = Path(args.reproducer).resolve()
    else:
        print("[3] no --reproducer given. Download the public testcase from:")
        print(f"    {case.report_url or 'the OSS-Fuzz issue for ' + case.osv_id}")
        print("    then re-run with --reproducer <file>. Skipping baseline PoC checks.")

    # 4. Baseline: PoC crashes on vuln, is clean on HEAD.
    if testcase:
        print("[4] baseline: build + reproduce on vulnerable version ...")
        ok, log = of.build_fuzzers(args.project, src_vuln)
        if not ok:
            sys.exit("build failed on vulnerable version:\n" + log[-2000:])
        crashed, log = of.reproduce(args.project, target, testcase)
        (work / "baseline_vuln.log").write_text(log)
        print(f"    PoC crashes on vuln: {crashed} (expected True)")

        print("    baseline: build + reproduce on HEAD ...")
        ok, log = of.build_fuzzers(args.project, src_head)
        if not ok:
            sys.exit("build failed on HEAD:\n" + log[-2000:])
        crashed_head, log = of.reproduce(args.project, target, testcase)
        (work / "baseline_head.log").write_text(log)
        print(f"    PoC crashes on HEAD: {crashed_head} (expected False)")
        if crashed_head:
            print("    WARNING: original PoC still crashes at HEAD; fix may be absent.")

    # 5. Generate variant harnesses (vuln-patch).
    base_harness = find_base_harness(src_vuln, target, args.base_harness)
    if not base_harness or not base_harness.exists():
        sys.exit("could not locate base harness; pass --base-harness")
    rel_harness = base_harness.resolve().relative_to(src_vuln.resolve())
    print(f"[5] base harness: {rel_harness}")
    context = {
        "project": args.project, "cve": case.cves, "osv_id": case.osv_id,
        "repo": case.repo, "vuln_commit": vuln_commit, "fixed_commit": case.fixed,
        "fuzz_target": target, "vuln_src": str(src_vuln),
        "base_harness": str(base_harness), "fix_diff": str(fix_diff),
        "crash_log": str(work / "baseline_vuln.log") if testcase else "",
        "num_variants": args.num_variants,
    }
    variants = generate.generate(context, work / "variants", args.vuln_patch_cmd)
    print(f"    {len(variants)} variant(s) generated")

    # 6. Differential run: gate on vuln, then test on HEAD.
    siblings = []
    for i, var in enumerate(variants):
        tag = f"variant_{i:03d}"

        if not args.skip_gate and testcase:
            overlay(src_vuln, rel_harness, var)
            ok, _ = of.build_fuzzers(args.project, src_vuln)
            gate = of.run_fuzzer(args.project, target, args.vuln_budget)[0] if ok else False
            restore(src_vuln, rel_harness)
            if not gate:
                print(f"    [{tag}] gate: no crash on vuln -> skip")
                continue
            print(f"    [{tag}] gate: crashes on vuln -> testing HEAD")

        overlay(src_head, rel_harness, var)
        ok, blog = of.build_fuzzers(args.project, src_head)
        if not ok:
            restore(src_head, rel_harness)
            print(f"    [{tag}] build failed on HEAD -> skip")
            continue
        crashed, artifacts, rlog = of.run_fuzzer(args.project, target, args.head_budget)
        restore(src_head, rel_harness)
        (work / f"{tag}_head.log").write_text(rlog)
        if crashed:
            saved = []
            for a in artifacts:
                d = work / f"{tag}_{Path(a).name}"
                shutil.copyfile(a, d)
                saved.append(str(d))
            siblings.append({"variant": str(var), "log": str(work / f"{tag}_head.log"),
                             "inputs": saved})
            print(f"    [{tag}] *** SIBLING CRASH ON HEAD *** inputs={saved}")
        else:
            print(f"    [{tag}] no crash on HEAD")

    # 7. Report.
    report = work / "siblings.json"
    report.write_text(json.dumps(
        {"case": case.osv_id, "cves": case.cves, "head_commit": head_commit,
         "siblings": siblings}, indent=2))
    print(f"\n[done] {len(siblings)} sibling(s) the fix missed. Report: {report}")
    if siblings:
        print("Treat these as live issues: disclose to the project + OSS-Fuzz "
              "before publishing.")


if __name__ == "__main__":
    main()
