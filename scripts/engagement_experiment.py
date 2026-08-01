#!/usr/bin/env python3
"""Cycle-8 engagement experiment — ONE SHOT, criteria pre-committed.

Protocol: docs/replay/backtrack/EXPERIMENT-PROTOCOL.md

Hypothesis: the reviewer CAN find the case-specific deciding fact (it does so in
some rolls and not others on identical input), so the gap is engagement, not
capability. Isolating the question and forcing a verbatim quote may close it —
the move the CITATION line already made on the dismissal side.

Populations, replayed with the exact material the reviewer originally saw,
extracted from the archived judge prompts (nothing reconstructed):
  * accusing checks on the 7 decisive false-accusation cases
  * kept alarms on genuine-catch legs — the guard set, which must answer NONE

NO PHRASING ITERATION. One run against the fixed criteria; a miss is the answer.
"""
import json
import os
import re
import sys
import glob
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from llm import HarnessGenerator  # noqa: E402

RUNS = ["runs-archive/runs/final30A_20260729_121819",
        "runs-archive/runs/final30B_20260729_145001"]

FALSE_ACCUSERS = {"Closure-62", "Math-30", "Math-65",
                  "Math-39", "Lang-60", "Math-73", "Chart-26"}
# legs that ended as genuine catches, per roll (the guard set)
CATCH_A = {'Math-2', 'Lang-41', 'Lang-50', 'Lang-60', 'Chart-7', 'Closure-92',
           'Math-68', 'Math-74', 'Math-82'}
CATCH_B = {'Math-2', 'Lang-41', 'Lang-50', 'Chart-7', 'Closure-92', 'Closure-38',
           'Lang-63', 'Math-68', 'Math-73', 'Math-74', 'Math-82'}

SYSTEM = (
    "You audit a single claim made by an automated code check. You are not "
    "judging the patch and you are not judging whether the check fired "
    "correctly. You answer exactly one question, in the required format."
)

# THE question — fixed by the protocol, not to be varied afterwards.
TEMPLATE = """A generated check asserts a premise about what ANY correct implementation of this API must do.

PREMISE (from the check that fired):
{premise}

THE PATCHED CLASS AND ITS DOCUMENTATION (what the reviewer was shown):
{context}

Question: is that premise a legitimate universal contract for this API — true of every correct implementation — or does the shown material show it to be OVER-TIGHT, i.e. true only of some implementations?

Answer on two lines EXACTLY:
VERDICT: LEGITIMATE | OVER-TIGHT
QUOTE: "<verbatim line from the shown material that establishes it is over-tight>" | NONE

If you answer OVER-TIGHT you must supply a quote that appears literally in the shown material. If you cannot, answer LEGITIMATE with QUOTE: NONE."""


def collect():
    """Pull kept-alarm cases with the exact prompt material, from the archive."""
    out = []
    for run in RUNS:
        roll = 'A' if 'final30A' in run else 'B'
        catches = CATCH_A if roll == 'A' else CATCH_B
        for t in glob.glob(os.path.join(run, '*', 'trace.md')):
            leg = os.path.basename(os.path.dirname(t))
            m = re.search(r'patch1-([A-Za-z]+-\d+)', leg)
            if not m:
                continue
            bug = m.group(1)
            correct_leg = leg.endswith('_c')
            if correct_leg and bug not in FALSE_ACCUSERS:
                continue
            if not correct_leg and bug not in catches:
                continue
            txt = open(t, errors='ignore').read()
            for s in re.split(r'\n(?=## \[\d+\])', txt):
                if 'verifier / judge' not in s.split('\n')[0]:
                    continue
                o = re.search(r'▸ Output.*?```\s*(.*?)```', s, re.S)
                p = re.search(r'▸ Prompt(.*?)(?=▸ Output)', s, re.S)
                if not o or not p or not re.search(r'VERDICT:\s*SOUND', o.group(1)):
                    continue
                body = p.group(1)
                fired = re.search(
                    r'The assertion that ACTUALLY fired on the patched code is:'
                    r'\s*\n\s*(.{0,400})', body)
                ctx = re.search(r'<codebase_context>(.*?)</codebase_context>',
                                body, re.S)
                harness = re.search(r'<harness>(.*?)</harness>', body, re.S)
                if not fired:
                    continue
                material = ((ctx.group(1) if ctx else '')
                            + "\n\n// the check's own source:\n"
                            + (harness.group(1)[:4000] if harness else ''))
                out.append({
                    'bug': bug, 'roll': roll,
                    'population': 'false-accusation' if correct_leg else 'guard',
                    'premise': ' '.join(fired.group(1).split())[:400],
                    'material': material[:60000],
                })
    return out


def ask(gen, case):
    prompt = TEMPLATE.format(premise=case['premise'], context=case['material'])
    try:
        raw = gen.generate([{'role': 'system', 'content': SYSTEM},
                            {'role': 'user', 'content': prompt}]) or ''
    except Exception as e:                       # fail closed to LEGITIMATE
        return {**case, 'verdict': 'ERROR', 'quote': '', 'grounded': False,
                'error': str(e)[:120]}
    v = re.search(r'VERDICT:\s*(LEGITIMATE|OVER-TIGHT)', raw)
    q = re.search(r'QUOTE:\s*(.+)', raw)
    quote = (q.group(1).strip() if q else '').strip('"').strip()
    verdict = v.group(1) if v else 'UNPARSED'
    # MECHANICAL grounding: the quote must literally appear in what was shown.
    norm = lambda s: re.sub(r'\s+', ' ', s)
    grounded = bool(quote and quote.upper() != 'NONE'
                    and norm(quote) in norm(case['material']))
    return {**case, 'verdict': verdict, 'quote': quote[:160],
            'grounded': grounded}


def main():
    # 8.1 part B re-runs this EXACT question under a different judge model.
    # Model and output path are argv-configurable so the re-run cannot
    # overwrite the incumbent's recorded results; the question, the populations
    # and the grounding check are untouched by design ("do NOT reword").
    model = sys.argv[1] if len(sys.argv) > 1 else None
    out_path = (sys.argv[2] if len(sys.argv) > 2
                else 'docs/replay/backtrack/engagement_results.json')
    cases = collect()
    print(f"collected {len(cases)} kept-alarm cases "
          f"({sum(1 for c in cases if c['population']=='false-accusation')} "
          f"false-accusation / "
          f"{sum(1 for c in cases if c['population']=='guard')} guard)",
          flush=True)
    print(f"model: {model or 'config-default'} -> {out_path}", flush=True)
    gen = (HarnessGenerator(model=model, temperature=0.0, top_p=1.0)
           if model else HarnessGenerator(temperature=0.0, top_p=1.0))
    with ThreadPoolExecutor(max_workers=6) as ex:
        results = list(ex.map(lambda c: ask(gen, c), cases))
    with open(out_path, 'w') as f:
        json.dump(results, f, indent=1)

    def tally(pop):
        r = [x for x in results if x['population'] == pop]
        ot = [x for x in r if x['verdict'] == 'OVER-TIGHT' and x['grounded']]
        return len(r), len(ot)

    fa_n, fa_ot = tally('false-accusation')
    g_n, g_ot = tally('guard')
    print(f"\nFALSE-ACCUSATION cases: {fa_n}")
    print(f"  called OVER-TIGHT with a grounded quote: {fa_ot}")
    print(f"GUARD cases (genuine catches): {g_n}")
    print(f"  wrongly called OVER-TIGHT (grounded): {g_ot}"
          f"   -> guards clean: {(g_n-g_ot)/max(g_n,1)*100:.1f}%")
    err = sum(1 for x in results if x['verdict'] in ('ERROR', 'UNPARSED'))
    print(f"errors/unparsed: {err}")


if __name__ == '__main__':
    main()
