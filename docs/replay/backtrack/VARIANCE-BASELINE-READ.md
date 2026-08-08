# The variance baseline — the lottery gets a size and a station

`varbase_20260808_183839`, `git=e9868f2`, 15 runs (5 legs × 3 draws),
`PARALLEL=6`, 76 minutes. Measurement only — no gates pass or fail. Raw
committed before this was written.

## The identical-code premise: established, but not the way the handoff asked

The handoff asked that all fifteen runs show `e9868f2`. **They cannot: no
per-run artefact records a sha.** `result.jsonl` has no git field and no trace
header carries one — the sha is recorded once, at suite level, in `config.json`
and the log header.

Established instead, and adequately: the suite ran from one launch and one sync;
`VERSION` on the VM reads `e9868f2` before and after; and nothing under `src/`
or `suites/` has an mtime later than **18:36**, against a run window of
**18:38–19:54**. No pull happened during the suite. The code was frozen.

**Filed as a gap:** a per-run sha belongs in `result.jsonl`. Every repeated-draws
measurement from here — stage 16 included — rests on a premise that currently
cannot be checked from the artefacts alone.

## Outcomes across draws

| leg | label | draws | stable? |
|---|---|---|---|
| Math-65 (CapGen) | correct | **FP / TN / FP** | **no** |
| Math-2 (SOFix) | correct | TN / TN / TN | yes |
| Chart-26 (Jaid) | correct | TN / TN / TN | yes |
| Chart-19 (Arja) | overfit | **TP / FN / TP** | **no** |
| Lang-63 (Arja) | overfit | **TP / FN / FN** | **no** |

**Three of five legs are unstable, in both error directions.** Two of the three
are recall legs: a fake patch escaped on 1 of 3 draws (Chart-19) and on 2 of 3
(Lang-63).

Every recall figure this project has quoted is a single draw. `R=1.00` in stage-4
roll 3 was four draws, not four guarantees — and Lang-63 shows a leg whose
*majority* outcome is a miss. Recall carries the same instability as precision;
nobody had measured it because catches happened to hold on every prior roll.

## Station localization: the divergence starts at the FIRST model station

Compared draw-to-draw, in pipeline order:

**Harness count is identical everywhere** — `built=5, run=5, triggers=5` on all
fifteen runs, unstable and stable legs alike. Nothing diverges in how much work
is done.

**The oracles invented differ in every draw of every leg.** Not their number —
their identity:

    Chart-19 draw10: lifted-domain-axis-null, lifted-null-arg,
                     test-getDomainAxisIndex-null, test-pair-4, …
    Chart-19 draw11: lifted-domain-axis-null, lifted-null,
                     lifted-null-rejected, test-domain-axis-null, …
    Chart-19 draw12: domain-axis-null-throws, lifted-null-throws,
                     lifted-range-axis-null, …

Relation synthesis then differs downstream in the same way — survivor counts of
11 / 18 / 11 on Chart-19, and wholly different relation names on Math-65
(`chiSquare_matches_documented_formula` in draw 1 against
`chiSquare_matches_weighted_residual_formula` in draws 2–3).

**The outcome follows from what was invented.** On both Chart-19 TP draws,
three harnesses crashed the patched build; on the FN draw, **zero** did — from a
set of checks that never probed the null range axis. Lang-63 is the same shape:
`crashedH=1` on the TP draw, `0` on both FN draws.

So the first differing station is **oracle/relation invention — the earliest
model call in the pipeline**, upstream of fuzzing and of judging. This is the
invention lottery, now observed directly rather than inferred as the residual
after other explanations were cleared.

### The nuance that matters for any fix

**Invention variance is universal; outcome variance is not.** The two stable
legs invent just as differently as the unstable ones — Math-2-SOFix produced
three disjoint oracle sets across its draws, Chart-26 likewise, and both
returned TN three times out of three.

So the noise is not "some legs are noisy." Every leg is noisy at the invention
station. A leg is *stable* when its defect is reachable by many different
inventions, and *unstable* when detection depends on inventing one particular
check. That distinction — reachable-many-ways versus reachable-one-way — is what
separates the three unstable legs from the two stable ones, and it is a property
of the bug, not of the run.

## Mechanism events (denominator data only, no attribution)

| leg | draws | generations | admissions |
|---|---|---|---|
| Math-65 | 3 | see per-run traces | varies by draw |
| others | 3 each | — | — |

Recorded for stage 16's denominators; no claim in either direction is made here,
per the measurement's terms.

## What this does and does not license

It licenses: reporting every future single-draw rate with the knowledge that
three legs in five move under repetition, and treating any single-roll F1 change
smaller than that movement as unmeasured.

It does not license: a fix. Seed-pinning, sampling multiple inventions, or
scoring an invention set are all design decisions that need the main session,
and the honest form of the question they now face is *"detection depends on
inventing one particular check; do we attack that by inventing more, or by
selecting better?"*
