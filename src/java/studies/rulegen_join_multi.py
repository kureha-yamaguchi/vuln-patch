"""Aggregate several --rulegen_only sample runs of ONE variant into a
per-bug convict-RATE (k of N samples), to beat single-sample noise.

Usage: python study/rulegen_join_multi.py <label> <run_dir1> <run_dir2> ...
"""
import sys, os, json, glob
from collections import defaultdict

def load(run_dir):
    legs = {}
    for rj in glob.glob(os.path.join(run_dir, "*", "result.jsonl")):
        txt = open(rj).read().strip()
        if not txt:
            continue
        d = json.loads(txt.splitlines()[-1])
        legs.setdefault(f"{d.get('project')}-{d.get('bug_id')}", {})[d.get('label')] = d
    return legs

def fired(rec):
    return {f['name'] for f in (rec or {}).get('relation_replay_fired', []) or []}

def main():
    label = sys.argv[1]
    dirs = sys.argv[2:]
    N = len(dirs)
    # per bug: convict count, false-fire count, survivor sum
    conv = defaultdict(int); ff = defaultdict(int); surv = defaultdict(list)
    bugs = set()
    for rd in dirs:
        legs = load(rd)
        for bug, d in legs.items():
            o, c = d.get('overfitting'), d.get('correct')
            if not o:
                continue
            bugs.add(bug)
            of, cf = fired(o), fired(c)
            if of and not cf:
                conv[bug] += 1
            if cf:
                ff[bug] += 1
            surv[bug].append(o.get('synth_survivors', 0))
    bugs = sorted(bugs)
    print(f"=== {label}  ({N} samples/leg) ===")
    print(f"{'bug':16s} convict/{N}  false-fire/{N}  avg-surv(o)")
    tot_conv_rate = 0.0
    convict_any = clean_all = 0
    for bug in bugs:
        cr = conv[bug]; fr = ff[bug]
        avg = sum(surv[bug])/len(surv[bug]) if surv[bug] else 0
        tot_conv_rate += cr / N
        convict_any += (cr > 0)
        clean_all += (cr == N and fr == 0)
        print(f"{bug:16s}   {cr}/{N}        {fr}/{N}        {avg:.1f}")
    print(f"\nmean convict-rate across {len(bugs)} bugs: "
          f"{tot_conv_rate/len(bugs):.2f}  "
          f"(= convict-legs/{N} averaged)")
    print(f"bugs convicting in >=1 sample: {convict_any}/{len(bugs)}")
    print(f"bugs convicting in ALL {N} (and never false-firing): {clean_all}/{len(bugs)}")

if __name__ == "__main__":
    main()
