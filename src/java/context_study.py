"""Context study: does CLASS-LEVEL context change (a) what the relation
synthesizer proposes and (b) how the relation verifier judges?

Motivation (task inspection, 2026-07-15): across 12 bugs in 4 categories
the hand-derived strongest relation routinely depends on code OUTSIDE the
patched method — constructor invariants, complementary sibling functions,
class-javadoc contracts — none of which the synthesizer or verifier
currently sees. This study measures the effect of adding
code_context.assemble_class_context to both stages.

Design:
  * SYNTH ARM — for each study bug (study/study_bugs.json, with GOLD
    relations hand-derived before any LLM run): synthesize once WITHOUT
    class context (V0 = production behaviour before this change) and once
    WITH it (V1). Output both proposal lists next to the gold list; the
    gold-hit judgment (is a proposal equivalent to a gold relation?) is
    made by the human/agent reading the study output, not automated.
  * VERIFIER ARM — for each logged replay case (the same cases.jsonl the
    replay harness uses, mapped to bugs via replay_map): verify once with
    code_context (the no-context verdicts already exist from the earlier
    vreplay runs and serve as baseline). Output verdict + reasoning so the
    use (or non-use) of domain facts is auditable.

Usage (on the VM, from src/):
  uv run python java/context_study.py \
      --bugs ../study/study_bugs.json \
      --out /home/code/scratch/runs/ctxstudy_$(date +%Y%m%d_%H%M%S) \
      --model gpt-5.4 \
      [--cases /home/code/scratch/replay/cases.jsonl] \
      [--skip-synth] [--skip-verifier]
"""
import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from analysis import TargetAnalyzer  # noqa: E402
from code_context import assemble_class_context  # noqa: E402
from llm import HarnessGenerator, token_usage, usage_totals  # noqa: E402
from patches import PatchSelector  # noqa: E402
from relation_synth import RelationSynthesizer, javadoc_for  # noqa: E402
from relation_verifier import RelationVerifier  # noqa: E402


def parse_args():
    p = argparse.ArgumentParser(description="Class-context ablation study "
                                            "for synthesizer + verifier.")
    p.add_argument("--bugs", required=True, help="study_bugs.json")
    p.add_argument("--out", required=True, help="output dir")
    p.add_argument("--model", default="gpt-5.4")
    p.add_argument("--cases", default=None,
                   help="replay cases.jsonl for the verifier arm")
    p.add_argument("--skip-synth", action="store_true")
    p.add_argument("--skip-verifier", action="store_true")
    return p.parse_args()


def _bug_context(entry):
    """Checkout + analyze one study bug; returns (selection, context,
    javadocs, class_ctx_blocks). Raises on failure (caller records it)."""
    selector = PatchSelector(project_name=entry["project"],
                             overfitting=(entry.get("label", "overfitting")
                                          == "overfitting"),
                             correct=(entry.get("label") == "correct"),
                             bug_id=str(entry["bug_id"]),
                             apr_tool=entry.get("apr_tool"))
    selection = selector.select()
    context = TargetAnalyzer().analyze(patch_path=selection.patch_path,
                                       buggy_dir=selection.buggy_dir)
    javadocs = []
    for rel in (context.modified_files or []):
        try:
            src_text = (Path(selection.buggy_dir) / rel).read_text(
                encoding="utf-8", errors="replace")
        except OSError:
            continue
        for fn in context.functions:
            jd = javadoc_for(src_text, fn.func_name)
            if jd and jd not in javadocs:
                javadocs.append(jd)
    class_ctx = assemble_class_context(
        selection.buggy_dir, context.modified_files or [],
        [fn.func_name for fn in context.functions])
    return selection, context, javadocs, class_ctx


def _rels_brief(rels):
    return [{"name": r.name, "kind": r.kind, "contract": r.contract,
             "check_first_line": (r.check or "").strip().splitlines()[0]
             if (r.check or "").strip() else ""}
            for r in rels]


def synth_arm(bugs, model, out_dir):
    results = []
    for entry in bugs:
        tag = f'{entry["project"]}-{entry["bug_id"]}({entry["apr_tool"]})'
        print(f"\n=== SYNTH {tag} [{entry['category']}] ===")
        rec = {"bug": tag, "category": entry["category"],
               "gold": entry["gold"]}
        try:
            selection, context, javadocs, class_ctx = _bug_context(entry)
            if not context.functions:
                raise RuntimeError("no touched functions extracted")
            rec["class_ctx_blocks"] = len(class_ctx)
            rec["class_ctx_chars"] = sum(len(b) for b in class_ctx)
            synth = RelationSynthesizer(
                HarnessGenerator(model=model, temperature=0.3, top_p=1.0))
            common = dict(
                patched_sources=[fn.func_source for fn in context.functions],
                class_name="", reachable=context.root_cause_reachable or [],
                mined_tests=[], trigger_summary="",
                patch_text=context.patch_text or "", javadocs=javadocs)
            v0 = synth.synthesize(**common, class_context=None)
            v1 = synth.synthesize(**common, class_context=class_ctx)
            rec["v0_no_class_ctx"] = _rels_brief(v0)
            rec["v1_with_class_ctx"] = _rels_brief(v1)
            print(f"  V0: {[r.name for r in v0]}")
            print(f"  V1: {[r.name for r in v1]}")
        except Exception as e:
            rec["error"] = str(e)
            print(f"  ERROR: {e}")
        results.append(rec)
        (out_dir / "synth_arm.json").write_text(
            json.dumps(results, indent=2) + "\n")
    return results


def verifier_arm(cases_path, replay_map, model, out_dir):
    rv = RelationVerifier(
        HarnessGenerator(model=model, temperature=0.0, top_p=1.0))
    ctx_cache = {}
    results = []
    with open(cases_path, encoding="utf-8") as fh:
        cases = [json.loads(ln) for ln in fh if ln.strip()]
    for c in cases:
        cid = c.get("id")
        m = replay_map.get(cid)
        rec = {"id": cid, "label": c.get("label")}
        if not m:
            rec["error"] = "no replay_map entry"
            results.append(rec)
            continue
        key = (m["project"], m["bug_id"], m["apr_tool"], m["label"])
        print(f"\n=== VERIFY {cid} ===")
        try:
            if key not in ctx_cache:
                _sel, _ctx, _jd, blocks = _bug_context(m)
                ctx_cache[key] = "\n\n".join(blocks) if blocks else None
            code_ctx = ctx_cache[key]
            rec["ctx_chars"] = len(code_ctx or "")
            ok, why = rv.verify(
                c["harness_source"],
                fired_assertion=c.get("fired_assertion"),
                trusted_values=c.get("trusted_values"),
                concrete_evidence=c.get("concrete_evidence"),
                code_context=code_ctx)
            rec["kept_with_ctx"] = bool(ok)
            rec["reason"] = why
            print(f"  kept={ok}: {why[:160]}")
        except Exception as e:
            rec["error"] = str(e)
            print(f"  ERROR: {e}")
        results.append(rec)
        (out_dir / "verifier_arm.json").write_text(
            json.dumps(results, indent=2) + "\n")
    return results


def main():
    args = parse_args()
    spec = json.loads(Path(args.bugs).read_text())
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    synth_results = verifier_results = None
    if not args.skip_synth:
        synth_results = synth_arm(spec["bugs"], args.model, out_dir)
    if not args.skip_verifier and args.cases:
        verifier_results = verifier_arm(args.cases, spec["replay_map"],
                                        args.model, out_dir)

    tot = usage_totals()
    lines = [f"# context study ({out_dir.name})", ""]
    if synth_results is not None:
        lines += ["## Synth arm (V0 = no class ctx, V1 = with)", "",
                  "| bug | category | V0 proposals | V1 proposals |"
                  " gold (needs) |", "|---|---|---|---|---|"]
        for r in synth_results:
            if "error" in r:
                lines.append(f"| {r['bug']} | {r['category']} |"
                             f" ERROR: {r['error']} | | |")
                continue
            v0 = "; ".join(x["name"] for x in r["v0_no_class_ctx"])
            v1 = "; ".join(x["name"] for x in r["v1_with_class_ctx"])
            gold = "; ".join(f"{g['name']} ({g['needs']})"
                             for g in r["gold"])
            lines.append(f"| {r['bug']} | {r['category']} | {v0} |"
                         f" {v1} | {gold} |")
        lines.append("")
    if verifier_results is not None:
        lines += ["## Verifier arm (with class ctx; baseline = earlier"
                  " vreplay runs)", "",
                  "| case | label | kept_with_ctx |", "|---|---|---|"]
        for r in verifier_results:
            lines.append(f"| {r['id']} | {r.get('label')} |"
                         f" {r.get('kept_with_ctx', r.get('error'))} |")
        lines.append("")
    lines += [f"Tokens: {tot['total_tokens']:,} total"
              f" ({tot['prompt_tokens']:,} in +"
              f" {tot['completion_tokens']:,} out, {tot['calls']} calls)",
              f"By model: {json.dumps(token_usage())}"]
    (out_dir / "summary.md").write_text("\n".join(lines) + "\n")
    print(f"\nsummary: {out_dir / 'summary.md'}")


if __name__ == "__main__":
    main()
