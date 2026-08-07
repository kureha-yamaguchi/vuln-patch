# Stage 2 (ladder2a) — how to read it (written BEFORE launch, deliberately)

Written so the read does not depend on anyone's memory of the conversation.
Suite: `suites/cases/ladder_stage2.cases`, TWO legs, run SERIAL
(`PARALLEL=1`).

## Protocol, unchanged since roll 5

1. `rsync -az -e ssh hetzner:/home/code/scratch/runs/ladder2a_<stamp>/ runs-archive/runs/ladder2a_<stamp>/`
2. Copy summary/config/result into `docs/replay/backtrack/ladder2-raw/roll1/`
3. **Commit raw BEFORE reading it.**
4. Then read — per event, both legs, never from totals.

## The per-event extraction (both legs)

```python
import re
for leg in ['01_patch1-Math-65-CapGen_c', '02_patch1-Math-2-Arja-plausible_o']:
    t = f'runs-archive/runs/ladder2a_<stamp>/{leg}/trace.md'
    print('='*20, leg)
    for s in re.split(r'\n(?=## \[\d+\])', open(t, errors='ignore').read()):
        h = s.split('\n')[0]
        if 'reference-impl' not in h and 'reference-verdict-gate' not in h \
                and 'outcome-flag' not in h:
            continue
        m = re.search(r'\*\*output:\*\*\s*\**([^*\n]+)', s)
        r = re.search(r'- reason: ([^\n]{0,320})', s)
        print(h[:60], '->', (m.group(1).strip() if m else '?')[:110])
        if r: print('    why:', r.group(1)[:300])
```

## Pre-registered gates (fail any → the stage does not pass)

| gate | leg | expectation |
|---|---|---|
| S2-a | Math-2 (overfit) | **ZERO `conviction VOIDED` events.** Any void on a fake patch = the gate destroying a genuine catch = HARD STOP, diagnose before anything else runs. |
| S2-b | Math-2 | TP retained: outcome stays a catch. Both signs recorded. |
| S2-c | Math-2 | Informational: reference admitted? disagreement-side fact emitted? Zero facts from discarded references. |
| S2-d | Math-65 (correct) | A TN counts for the gate ONLY if a `conviction VOIDED` event appears and the kept-conviction set it removed is visible in the trace. A TN with zero gate events is the invention lottery again (roll 13) — record, do not bank. An FP with gate abstains = the gate's reach didn't cover this roll's firings; check WHICH door and WHY (values in the abstain reasons). |
| S2-e | rule 7 | two no-change iterations → stop and report. |

## What each Math-2 outcome means

* Gate abstains everywhere + TP kept → cleanest pass: the gate is inert
  where it must be inert, on held-out material.
* Reference DISCARDED at screen/pin on Math-2 → also fine (fail-closed);
  record where and why — it is the first reach datum on a non-Math-65 bug.
* Reference ADMITTED and the fact shows DISAGREEMENT with the patch →
  the conviction direction exists; note whether the judge engages it
  (expectation from roll 12: it will not — that is not a gate failure).
* ANY `conviction VOIDED` on this leg → hard stop (S2-a).

## Context to carry

The 8.25 gate is wired on BOTH doors (harness-track adjudication + replay
track) as of the door-parity commit; the corpus scan
(`scripts/gate_corpus_scan.py`) measured 3 voids on recorded Math-65
material, zero on the replay door. Gate events are named
`reference-verdict-gate` with outputs `conviction VOIDED` / `gate
abstains`, one per kept conviction.
