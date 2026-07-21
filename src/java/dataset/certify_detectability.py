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
must not (0 across probes); Chart-1/Arja patch1 certifies (472 — the
earlier "likely undetectable" triage examined a different patch file).

--label correct (the B1 mislabel probe) runs the same differential on a
"Dcorrect" patch vs the developer fix: >0 STRONG divergences means the
patch is behaviorally distinct from the fix — mislabeled, or at least
behaviorally wrong — and the leak metric computed against that label is
partly the verifier being right. Divergences are therefore CLASSIFIED by
kind: a value difference or a different exception class is STRONG; the
same exception class with different message text is WEAK (both builds
rejected the input; wording legitimately varies between a correct patch
and the dev fix) and never certifies on its own. Stochastic APIs must be
seeded through their public API or probed only for deterministic
properties (see the probe instructions).

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
import re
import shutil
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents if (p / 'config.py').exists())))

import config  # noqa: E402
from java.bug_context.analysis import TargetAnalyzer  # noqa: E402
from java.bug_context.failure_test import FailureTestExtractor  # noqa: E402
from java.execution.fuzz_runner import (PatchApplyError, PatchedProjectBuilder,  # noqa: E402
                         TriggerVerificationError)
from llm import HarnessGenerator, usage_totals  # noqa: E402
from java.bug_context.patches import PatchSelector  # noqa: E402

_PROBE_SYSTEM = (
    "You write deterministic Java PROBE programs that print a program's"
    " observable behaviour, with NO test oracle and NO judgment. Output a"
    " single compilable .java file, no markdown fences, no prose."
)

_PROBE_INSTRUCTIONS = """\
Write a single Java file:
- package {package};
- public class DivergenceProbe with public static void main(String[] args).
- Enumerate AT LEAST 200 DETERMINISTIC inputs covering TWO surfaces:
  (a) the patched method's domain: a fixed grid over the relevant parameter
      ranges, densely covering the boundary values of the condition the
      patch changed;
  (b) the failing test's surface: construct the EXACT objects the test
      below constructs (same constructor arguments, same setup) and print
      the result of EVERY public observable/accessor method of those
      objects — not only the patched method. An overfit patch often
      silences the test's symptom while leaving a sibling observable of
      the same object broken; only (b) reveals that. Repeat (b) on a small
      grid of parameter variations around the test's values (including
      extreme magnitudes of the same shape).
  NO randomness, NO current time, NO identity hashCodes — every run must
  print byte-identical output.
- For EACH input print EXACTLY ONE line:
    IN=<compact input description> OUT=<stable result representation>
  For results, print primitives/Strings directly; for objects print a
  STABLE representation (toString only if it is value-based; otherwise
  print the individual accessor values you care about).
- Wrap each input's calls in try/catch(Throwable t) and on a throw print:
    IN=<input> OUT=EXC:<t.getClass().getSimpleName()>:<t.getMessage()>
- If any probed method is STOCHASTIC (a sampler, anything backed by a
  random generator), SEED it through its public API (a reseed method or a
  seeded constructor) with a fixed constant BEFORE each input's calls so
  every run is byte-identical. If it cannot be seeded, do not print raw
  draws — print only deterministic properties of them (e.g. whether each
  draw lies within the documented support/bounds).
- Reach the behaviour through the REAL public API exactly as the test
  below does. Do not subclass/mock library types.
- Print nothing else (no headers, no timing).
"""


def parse_args():
    p = argparse.ArgumentParser(
        description="Certify overfit patches as behaviorally detectable "
                    "(dataset construction only — reads the developer fix).")
    p.add_argument("--candidates", help="JSONL from eval_candidates.py "
                                        "(rows matching --label are "
                                        "certified; others pass through)")
    p.add_argument("--patch_file", help="certify exactly one patch file")
    p.add_argument("--label", choices=["overfitting", "correct"],
                   default="overfitting",
                   help="which patch label to certify. 'overfitting' "
                        "(default) asks the detectability question. "
                        "'correct' is the mislabel probe: >0 STRONG "
                        "divergences vs the developer fix means the "
                        "'correct' label is suspect (behaviorally distinct "
                        "from the fix).")
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
    # -encoding UTF-8: probes may embed non-ASCII string inputs; don't let
    # the platform charset decide whether they compile.
    r = _run(["javac", "-encoding", "UTF-8", "-cp", cp, "-d", workdir, path])
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


def _keyed_outputs(lines):
    """Map IN=<desc> -> OUT=<repr> for probe output lines (first
    occurrence wins on a duplicated input description)."""
    m = {}
    for ln in lines:
        if " OUT=" in ln:
            k, v = ln.split(" OUT=", 1)
            m.setdefault(k, v)
    return m


# Generic JDK exceptions that ESCAPE library code on malformed input
# (simple names — the probe prints getSimpleName). Mirrors
# fuzz_runner.GENERIC_ESCAPE_EXCEPTIONS; see _classify_divergences.
_GENERIC_ESCAPE_SIMPLE = frozenset({
    "StringIndexOutOfBoundsException", "ArrayIndexOutOfBoundsException",
    "IndexOutOfBoundsException", "NullPointerException",
    "ClassCastException", "ArithmeticException",
    "NegativeArraySizeException",
})

_NUM_RE = re.compile(r'-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?')


def _near_equal_numeric(a: str, b: str, rel: float = 1e-9) -> bool:
    """True when two OUT= payloads differ only in the last ulps of their
    embedded numbers: same non-numeric skeleton, same number count, every
    pair within `rel` relative tolerance. A correct patch may evaluate
    n*m/N in a different order than the dev fix and print
    0.6000000000000001 vs 0.6 — a formatting-visible, semantically
    irrelevant difference (measured: all 339 Math-2/SOFix divergences
    were of this kind; see progress.md §9 B1 result)."""
    na, nb = _NUM_RE.findall(a), _NUM_RE.findall(b)
    if not na or len(na) != len(nb):
        return False
    if _NUM_RE.sub('#', a) != _NUM_RE.sub('#', b):
        return False
    for x, y in zip(na, nb):
        try:
            fx, fy = float(x), float(y)
        except ValueError:
            return False
        if fx != fy and abs(fx - fy) > rel * max(abs(fx), abs(fy), 1e-300):
            return False
    return True


def _classify_divergences(lines_f, lines_o):
    """Classify per-input divergences by KIND, keyed on the IN= label.

    value                  -> both builds returned, values differ
                              beyond numeric tolerance             (STRONG)
    exception_class        -> different exception class, or one throws
                              where the other returns — and NEITHER side
                              is a generic JDK escape              (STRONG)
    value_ulp              -> values differ only in the last ulps of
                              embedded floats (evaluation-order noise a
                              tolerance-based oracle can never see) (WEAK)
    exception_generic_latent -> the classes differ but one side is a
                              generic JDK escape (SIOOBE/NPE/...) —
                              pre-existing LATENT crash surface that the
                              dev fix may have incidentally fixed and a
                              minimal correct patch legitimately leaves
                              (measured: dev fix throws NumberFormat-
                              Exception on "0.eE" where Lang-27/SimFix
                              still SIOOBEs; the label is still correct —
                              the latent crash is not the bug)      (WEAK)
    exception_message_only -> same class, different message text   (WEAK)

    The raw line-diff count stays the overfit-side certification signal
    (validated on the known answers); this classification exists so the
    correct-side mislabel probe (--label correct) cannot indict a label
    on wording, float formatting, or latent crash surface."""
    kinds = {"value": 0, "exception_class": 0, "value_ulp": 0,
             "exception_generic_latent": 0, "exception_message_only": 0}
    f, o = _keyed_outputs(lines_f), _keyed_outputs(lines_o)
    for k in f.keys() & o.keys():
        vf, vo = f[k], o[k]
        if vf == vo:
            continue
        exc_f, exc_o = vf.startswith("EXC:"), vo.startswith("EXC:")
        if exc_f and exc_o:
            cls_f = vf.split(":", 2)[1] if vf.count(":") >= 2 else vf
            cls_o = vo.split(":", 2)[1] if vo.count(":") >= 2 else vo
            if cls_f == cls_o:
                kinds["exception_message_only"] += 1
            elif (cls_f in _GENERIC_ESCAPE_SIMPLE
                  or cls_o in _GENERIC_ESCAPE_SIMPLE):
                kinds["exception_generic_latent"] += 1
            else:
                kinds["exception_class"] += 1
        elif exc_f or exc_o:
            exc_side = vf if exc_f else vo
            cls = (exc_side.split(":", 2)[1]
                   if exc_side.count(":") >= 2 else exc_side)
            if cls in _GENERIC_ESCAPE_SIMPLE:
                kinds["exception_generic_latent"] += 1
            else:
                kinds["exception_class"] += 1
        elif _near_equal_numeric(vf, vo):
            kinds["value_ulp"] += 1
        else:
            kinds["value"] += 1
    return kinds


def certify_one(cand, gen, probes, timeout):
    project, bug_id = cand["project"], cand["bug_id"]
    patch_path = cand["patch_path"]
    is_correct = cand.get("label") == "correct"

    # Buggy checkout via the pipeline's own selector logic (validated,
    # cached), then the candidate patch (overfit OR nominally-correct)
    # built on top of it. The differential below is always candidate-build
    # vs developer-fixed build; only the interpretation depends on label.
    selector = PatchSelector(project_name=project, correct=is_correct,
                             overfitting=not is_correct,
                             patch_file=patch_path)
    selection = selector.select()
    builder = PatchedProjectBuilder()
    patched_dir = builder.build_patched_dir(selection.buggy_dir, patch_path)
    patched_cp = builder.classpath(patched_dir,
                                   fallback_buggy_dir=selection.buggy_dir)
    fixed_dir, fixed_cp = _checkout_fixed(project, bug_id)

    context = TargetAnalyzer().analyze(patch_path=patch_path,
                                       buggy_dir=selection.buggy_dir)
    failure_tests = FailureTestExtractor().extract(
        selection.buggy_dir, project_name=project, bug_id=bug_id)

    total_div, examples, probe_status = 0, [], []
    kinds_total = {"value": 0, "exception_class": 0, "value_ulp": 0,
                   "exception_generic_latent": 0,
                   "exception_message_only": 0}
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
            workdir = os.path.join(patched_dir, "fuzz", f"probe_{p}")
            compiled, err = _compile_probe(source, workdir, patched_cp)
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
            rc_o, out_o = _run_probe(fq, workdir, patched_cp, timeout)
            rc_f, out_f = _run_probe(fq, workdir, fixed_cp, timeout)
        except subprocess.TimeoutExpired:
            probe_status.append("timeout")
            continue
        lines_o = [ln for ln in out_o.splitlines() if ln.startswith("IN=")]
        lines_f = [ln for ln in out_f.splitlines() if ln.startswith("IN=")]
        # A probe that failed to LAUNCH (nonzero exit) or printed no IN=
        # lines on either build yields a one-sided diff in which every
        # line reads as a divergence. Abstain — count NOTHING from this
        # probe. A fixed-build classpath error (the probe is compiled
        # against the candidate cp and only RUN on the fixed cp) must
        # never certify a patch as detectable.
        bad_sides = []
        if rc_o != 0 or not lines_o:
            bad_sides.append(f"candidate(rc={rc_o},lines={len(lines_o)})")
        if rc_f != 0 or not lines_f:
            bad_sides.append(f"fixed(rc={rc_f},lines={len(lines_f)})")
        if bad_sides:
            probe_status.append("run-failed:" + "+".join(bad_sides))
            continue
        diff = [ln for ln in difflib.unified_diff(lines_f, lines_o,
                                                  lineterm="", n=0)
                if ln.startswith(("+IN=", "-IN="))]
        # //2 pairs up the +/- sides of a changed line; when add/delete
        # counts are unequal it undercounts. Fine as the boolean
        # certification gate (any diff > 0), NOT an exact divergence count
        # — the per-kind tallies below are the countable signal.
        div = len(diff) // 2 if diff else 0
        total_div += div
        for k, v in _classify_divergences(lines_f, lines_o).items():
            kinds_total[k] += v
        examples += diff[:10]
        probe_status.append(f"ok:{div}div/{len(lines_o)}lines")
    strong = kinds_total["value"] + kinds_total["exception_class"]
    if total_div and not strong:
        print("  NOTE: every divergence is WEAK (message wording, float "
              "ulps, or latent generic-crash surface). Do not read this "
              "as behavioral distinctness.")
    return {
        **cand,
        "divergences": total_div,
        "divergence_kinds": kinds_total,
        "strong_divergences": strong,
        "certified_detectable": total_div > 0,
        # The mislabel-probe verdict (label=correct rows): behaviorally
        # distinct from the developer fix on STRONG evidence only.
        "behaviorally_distinct": strong > 0,
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
                      "apr_tool": peek.apr_tool, "label": args.label,
                      "patch_path": args.patch_file})
    else:
        with open(args.candidates) as fh:
            for line in fh:
                if line.strip():
                    c = json.loads(line)
                    if c.get("label") == args.label:
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
            except (PatchApplyError, TriggerVerificationError) as e:
                # P0.1 safety net: the candidate build is not what it
                # claims to be (patch broken / half-applied / bug absent
                # in this environment). A distinct, queryable status —
                # certifying such a build produced the phantom Lang-50
                # 43-divergence record.
                rec = {**cand, "divergences": None,
                       "certified_detectable": None,
                       "probe_status":
                           [getattr(e, 'status', 'bad_patch')],
                       "safety_net": str(e)}
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
