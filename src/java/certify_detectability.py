"""Certify whether an overfitting patch is DETECTABLE — i.e. behaviorally
distinguishable from the developer-fixed version on at least one input.

===========================================================================
DATASET-CONSTRUCTION TOOL ONLY. This reads the DEVELOPER FIX (the `<id>f`
checkout), which the detection pipeline itself must never see — using it in
a verdict is cheating. Its sole legitimate use is offline eval-set
curation: an overfit patch that is output-equivalent to the fix on every
input (observed: a deleted-but-redundant guard) is UNDETECTABLE by ANY
harness, and keeping such bugs in the eval set turns recall into noise.
===========================================================================

Method: an LLM writes a deterministic PRINTER PROBE — a plain main() that
enumerates a few hundred fixed inputs across the patched method's domain
(especially the boundaries of the changed condition) and prints one stable
line per input: the result, or the thrown exception class+message. No
oracle, no judgment — just observable behaviour. The probe is compiled once
and run against (a) the overfit-patched build and (b) the developer-fixed
build; the line-diff count is the divergence count.

  divergences > 0  -> CERTIFIED DETECTABLE (a perfect harness could catch
                      it; a miss is a technique failure worth debugging)
  divergences == 0 -> NOT CERTIFIED (probe found nothing; more probes may —
                      rerun with --probes N — but repeated zeros mean the
                      bug belongs OUT of the recall denominator)

Known-answer validation cases: Time-4/Arja must certify (>0); Lang-22/Arja
must not (0 across probes); Chart-1/Arja expected 0.

Usage (on the VM, from src/):
  uv run python java/certify_detectability.py \
      --candidates candidates.jsonl --out certified.jsonl [--limit 10]
  # or a single patch:
  uv run python java/certify_detectability.py \
      --patch_file /path/patch1-Time-4-Arja.patch --out certified.jsonl
"""
import argparse
import difflib
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config  # noqa: E402
from analysis import TargetAnalyzer  # noqa: E402
from failure_test import FailureTestExtractor  # noqa: E402
from fuzz_runner import PatchedProjectBuilder  # noqa: E402
from llm import HarnessGenerator, usage_totals  # noqa: E402
from patches import PatchSelector  # noqa: E402

_PROBE_SYSTEM = (
    "You write deterministic Java PROBE programs that print a program's"
    " observable behaviour, with NO test oracle and NO judgment. Output a"
    " single compilable .java file, no markdown fences, no prose."
)

_PROBE_INSTRUCTIONS = """\
Write a single Java file:
- package {package};
- public class DivergenceProbe with public static void main(String[] args).
- Enumerate AT LEAST 200 DETERMINISTIC inputs for the patched method's
  domain: a fixed grid over the relevant parameter ranges, densely covering
  the boundary values of the condition the patch changed, plus the inputs
  used by the test shown below. NO randomness, NO current time, NO
  identity hashCodes — every run must print byte-identical output.
- For EACH input print EXACTLY ONE line:
    IN=<compact input description> OUT=<stable result representation>
  For results, print primitives/Strings directly; for objects print a
  STABLE representation (toString only if it is value-based; otherwise
  print the individual accessor values you care about).
- Wrap each input's calls in try/catch(Throwable t) and on a throw print:
    IN=<input> OUT=EXC:<t.getClass().getSimpleName()>:<t.getMessage()>
- Reach the behaviour through the REAL public API exactly as the test
  below does. Do not subclass/mock library types.
- Print nothing else (no headers, no timing).
"""


def parse_args():
    p = argparse.ArgumentParser(
        description="Certify overfit patches as behaviorally detectable "
                    "(dataset construction only — reads the developer fix).")
    p.add_argument("--candidates", help="JSONL from eval_candidates.py "
                                        "(overfitting rows are certified; "
                                        "others pass through)")
    p.add_argument("--patch_file", help="certify exactly one patch file")
    p.add_argument("--out", required=True, help="output JSONL (appended)")
    p.add_argument("--limit", type=int, default=0,
                   help="max candidates to certify this run (0 = all)")
    p.add_argument("--probes", type=int, default=1,
                   help="independent probes per candidate (a zero stays "
                        "zero only if ALL probes find nothing)")
    p.add_argument("--model", default=None,
                   help="probe-writing model (default: escalation model)")
    p.add_argument("--timeout", type=int, default=90,
                   help="seconds per probe run (default 90)")
    return p.parse_args()


def _run(cmd, cwd=None, timeout=None):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True,
                          timeout=timeout)


def _checkout_fixed(project, bug_id):
    """Checkout + compile the developer-FIXED version; return (dir, cp)."""
    fixed_dir = os.path.join(config.D4J_CHECKOUT_ROOT,
                             f"{project}_{bug_id}_fixed")
    cfg = os.path.join(fixed_dir, ".defects4j.config")
    want = f"vid={bug_id}f"
    ok = False
    if os.path.isfile(cfg):
        try:
            ok = want in open(cfg).read()
        except OSError:
            ok = False
    if not ok:
        if os.path.isdir(fixed_dir):
            shutil.rmtree(fixed_dir)
        r = _run(["defects4j", "checkout", "-p", project,
                  "-v", f"{bug_id}f", "-w", fixed_dir])
        if r.returncode != 0:
            raise RuntimeError(f"fixed checkout failed: {r.stderr[-500:]}")
    r = _run(["defects4j", "compile"], cwd=fixed_dir)
    if r.returncode != 0:
        raise RuntimeError(f"fixed compile failed: {r.stderr[-500:]}")
    cp = _run(["defects4j", "export", "-p", "cp.test"],
              cwd=fixed_dir).stdout.strip()
    return fixed_dir, cp


def _probe_prompt(context, failure_tests):
    parts = [_PROBE_INSTRUCTIONS.format(package=context.package or "probe")]
    parts += ["The patch whose behaviour we probe:", "<patch>",
              context.patch_text or "", "</patch>"]
    for fn in context.functions:
        parts += [f"Patched function `{fn.func_name}`:", "<code>",
                  fn.func_source, "</code>"]
    if context.source_imports:
        parts += ["Imports available in the modified file:",
                  *context.source_imports]
    for ft in failure_tests[:2]:
        if getattr(ft, "method_source", None):
            parts += [f"A test that drives this code "
                      f"({ft.test_class}::{ft.test_method}):",
                      "<test>", ft.method_source, "</test>"]
    return "\n".join(parts)


def _compile_probe(source, workdir, cp):
    os.makedirs(workdir, exist_ok=True)
    path = os.path.join(workdir, "DivergenceProbe.java")
    with open(path, "w") as fh:
        fh.write(source)
    r = _run(["javac", "-cp", cp, "-d", workdir, path])
    return r.returncode == 0, r.stderr


def _fq_probe_name(source):
    for line in source.splitlines():
        line = line.strip()
        if line.startswith("package ") and line.endswith(";"):
            return line[len("package "):-1].strip() + ".DivergenceProbe"
    return "DivergenceProbe"


def _run_probe(fq_name, workdir, cp, timeout):
    r = _run(["java", "-cp", os.pathsep.join([workdir, cp]), fq_name],
             timeout=timeout)
    return r.returncode, r.stdout


def certify_one(cand, gen, probes, timeout):
    project, bug_id = cand["project"], cand["bug_id"]
    patch_path = cand["patch_path"]

    # Buggy checkout via the pipeline's own selector logic (validated,
    # cached), then overfit build on top of it.
    selector = PatchSelector(project_name=project, correct=False,
                             overfitting=True, patch_file=patch_path)
    selection = selector.select()
    builder = PatchedProjectBuilder()
    overfit_dir = builder.build_patched_dir(selection.buggy_dir, patch_path)
    overfit_cp = builder.classpath(overfit_dir,
                                   fallback_buggy_dir=selection.buggy_dir)
    fixed_dir, fixed_cp = _checkout_fixed(project, bug_id)

    context = TargetAnalyzer().analyze(patch_path=patch_path,
                                       buggy_dir=selection.buggy_dir)
    failure_tests = FailureTestExtractor().extract(
        selection.buggy_dir, project_name=project, bug_id=bug_id)

    total_div, examples, probe_status = 0, [], []
    base_prompt = _probe_prompt(context, failure_tests)
    for p in range(probes):
        messages = [{"role": "system", "content": _PROBE_SYSTEM},
                    {"role": "user", "content": base_prompt
                     + (f"\n\nThis is probe #{p + 1}; cover a DIFFERENT "
                        "slice of the input domain than an earlier probe "
                        "might have." if p else "")}]
        source, compiled, err = None, False, ""
        for _repair in range(3):
            out = gen.generate(messages) or ""
            source = out.strip()
            if source.startswith("```"):
                source = source.strip("`").lstrip("java").strip()
            workdir = os.path.join(overfit_dir, "fuzz", f"probe_{p}")
            compiled, err = _compile_probe(source, workdir, overfit_cp)
            if compiled:
                break
            messages += [{"role": "assistant", "content": out[:4000]},
                         {"role": "user", "content":
                          "javac failed:\n" + err[-2000:]
                          + "\nReturn the corrected full file."}]
        if not compiled:
            probe_status.append("compile-failed")
            continue
        fq = _fq_probe_name(source)
        try:
            rc_o, out_o = _run_probe(fq, workdir, overfit_cp, timeout)
            rc_f, out_f = _run_probe(fq, workdir, fixed_cp, timeout)
        except subprocess.TimeoutExpired:
            probe_status.append("timeout")
            continue
        lines_o = [ln for ln in out_o.splitlines() if ln.startswith("IN=")]
        lines_f = [ln for ln in out_f.splitlines() if ln.startswith("IN=")]
        diff = [ln for ln in difflib.unified_diff(lines_f, lines_o,
                                                  lineterm="", n=0)
                if ln.startswith(("+IN=", "-IN="))]
        div = len(diff) // 2 if diff else 0
        total_div += div
        examples += diff[:10]
        probe_status.append(f"ok:{div}div/{len(lines_o)}lines")
    return {
        **cand,
        "divergences": total_div,
        "certified_detectable": total_div > 0,
        "probe_status": probe_status,
        "diff_examples": examples[:10],
    }


def main():
    args = parse_args()
    if not args.candidates and not args.patch_file:
        print("need --candidates or --patch_file")
        sys.exit(1)
    cands = []
    if args.patch_file:
        peek = PatchSelector.peek_patch_file(args.patch_file)
        cands.append({"project": peek.project_name, "bug_id": peek.bug_id,
                      "apr_tool": peek.apr_tool, "label": "overfitting",
                      "patch_path": args.patch_file})
    else:
        with open(args.candidates) as fh:
            for line in fh:
                if line.strip():
                    c = json.loads(line)
                    if c.get("label") == "overfitting":
                        cands.append(c)
    if args.limit:
        cands = cands[:args.limit]

    model = args.model or config.HARNESS_MODEL_ESCALATION
    gen = HarnessGenerator(model=model, temperature=0.2, top_p=1.0)
    print(f"certifying {len(cands)} candidate(s) with {model}")

    with open(args.out, "a", encoding="utf-8") as out:
        for cand in cands:
            tag = (f"{cand['project']}-{cand['bug_id']}"
                   f"({cand.get('apr_tool')})")
            print(f"\n=== {tag} ===")
            try:
                rec = certify_one(cand, gen, args.probes, args.timeout)
            except Exception as e:
                rec = {**cand, "divergences": None,
                       "certified_detectable": None,
                       "probe_status": [f"error:{e}"]}
            out.write(json.dumps(rec) + "\n")
            out.flush()
            print(f"  -> {rec['probe_status']} "
                  f"certified={rec['certified_detectable']}")
    tot = usage_totals()
    print(f"\ntokens: {tot['total_tokens']:,} "
          f"({tot['prompt_tokens']:,} in + {tot['completion_tokens']:,} out)")


if __name__ == "__main__":
    main()
