"""Join a --rulegen_only suite's per-leg records into per-bug rule-gen quality.

For each bug, pair its overfit (-o) and correct (-c) legs and compute:
  convict     — a relation fired on the OVERFIT-patch build (via replay);
                the rule-gen stage produced something that catches the overfit.
  clean-convict — convict AND nothing fired on the CORRECT-patch build
                (would not have false-accused the correct sibling).
  false-fire  — a relation fired on the CORRECT-patch build (would false-accuse).
Also aggregates quantity (candidates, survivors).

Usage: python study/rulegen_join.py <suite_run_dir>
"""
import sys, os, json, glob, re
from collections import defaultdict

def load(run_dir):
    legs = {}
    for rj in glob.glob(os.path.join(run_dir, "*", "result.jsonl")):
        txt = open(rj).read().strip()
        if not txt:
            continue
        d = json.loads(txt.splitlines()[-1])
        key = f"{d.get('project')}-{d.get('bug_id')}"
        legs.setdefault(key, {})[d.get('label')] = d
    return legs

def fired_names(rec):
    return {f['name'] for f in (rec or {}).get('relation_replay_fired', []) or []}

def main():
    run_dir = sys.argv[1]
    legs = load(run_dir)
    n_bugs = 0
    convict = clean = false_fire = 0
    tot_cand = tot_surv = 0
    tot_legs = 0
    print(f"{'bug':16s} {'cand/surv(o)':13s} {'fires-on-overfit':32s} {'fires-on-correct':24s} verdict")
    for bug, d in sorted(legs.items()):
        o = d.get('overfitting'); c = d.get('correct')
        for rec in (o, c):
            if rec:
                tot_legs += 1
                tot_cand += rec.get('synth_candidates', 0)
                tot_surv += rec.get('synth_survivors', 0)
        if not o:
            continue
        n_bugs += 1
        of = fired_names(o); cf = fired_names(c)
        did_convict = bool(of)
        did_false = bool(cf)
        is_clean = did_convict and not cf
        convict += did_convict
        clean += is_clean
        false_fire += did_false
        verdict = ('CLEAN-CONVICT' if is_clean else
                   'CONVICT+FF' if did_convict and did_false else
                   'CONVICT(unchecked-c)' if did_convict else
                   'FALSE-FIRE-ONLY' if did_false else 'MISS')
        print(f"{bug:16s} {str(o.get('synth_candidates'))+'/'+str(o.get('synth_survivors')):13s} "
              f"{','.join(sorted(of))[:31]:32s} {','.join(sorted(cf))[:23]:24s} {verdict}")
    print("\n==== RULE-GEN QUALITY ====")
    print(f"bugs with overfit leg: {n_bugs}")
    print(f"convict (a relation fires on the overfit): {convict}/{n_bugs}")
    print(f"clean-convict (fires on overfit, quiet on correct): {clean}/{n_bugs}")
    print(f"false-fire (a relation fires on a correct patch): {false_fire}/{n_bugs}")
    if tot_legs:
        print(f"avg candidates/leg: {tot_cand/tot_legs:.1f}   avg survivors/leg: {tot_surv/tot_legs:.1f}")

if __name__ == "__main__":
    main()
