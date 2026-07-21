> **Machine-readable source: `suites/labels/verified_incorrect.jsonl`** (mislabels, both directions) and **`excluded.jsonl`** (correctly-labeled-but-unusable). This file is the human-readable EVIDENCE those rows point to — if they disagree, the jsonl wins.

# Incorrect labels — explained (both directions)

An **incorrect label** = the drr directory label (`Dcorrect/` vs `Doverfitting/`) does NOT match the patch's actual behaviour vs the DEVELOPER FIX (defects4j `<id>f`). Verdicts are **per patch file** (a bug can have one mislabeled and one correctly-labeled patch of the same directory). Fix these before computing eval denominators.

## A. Labeled `overfitting`, but really CORRECT (behaviourally == the dev fix)
These pass the failing test AND match the dev fix on every input, so **no sound oracle can distinguish them** — exclude from the recall denominator. Most are drr LABEL ERRORS (the 'overfit' is extensionally the dev fix).

| Patch | Why it equals the dev fix | Verification |
|---|---|---|
| Closure-86 / SequenceR (`patch2-Closure-86-SequenceR.patch`) | return isImmutableValue(NEW-node) can never match Token.NEW == return false (the dev fix); NOTE siblings patch3/patch5 are NOT equivalent | UNDETECTABLE.md (July audit; UNDETECTABLE.md) |
| Closure-115 / ssFix (`patch1-Closure-115-ssFix-plausible.patch`) | dead-pure leftover only | UNDETECTABLE.md; b3_certified_overfit.jsonl 0div |
| Lang-22 / Arja (`patch1-Lang-22-Arja-plausible.patch`) | redundant deleted guard (0/909+0/1700) | UNDETECTABLE.md; b3_certified_overfit.jsonl 0div |
| Lang-39 / Elixir (`patch1-Lang-39-Elixir-plausible.patch`) | i>searchList.length makes size loop dead (capacity-only); output+exceptions byte-identical | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |
| Lang-43 / CapGen (`patch1-Lang-43-CapGen-plausible.patch`) | injected getQuotedString(..,false) reduces to single next(pos) (first char QUOTE) = dev fix | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |
| Lang-45 / Jaid (`patch1-Lang-45-Jaid-plausible.patch`) | empty-check reshaped as else of same clamp; abbreviate('',5,10,..)='' like dev; 3200 inputs | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |
| Lang-51 / Arja (`patch1-Lang-51-Arja-plausible.patch`) | equalsIgnoreCase chain recognizes identical {true,on,yes}; false/off/no dead; total function = dev fix | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |
| Math-30 / ssFix (`patch1-Math-30-ssFix-plausible.patch`) | cast-at-use==cast-at-declaration | UNDETECTABLE.md; b3_certified_overfit.jsonl 0div |
| Math-30 / ssFix (`patch1-Math-30-ssFix.patch`) | cast-at-use == cast-at-declaration (int->double widening exact at both use sites); residual n1*n2 int overflow identical in dev fix | UNDETECTABLE.md (July audit; UNDETECTABLE.md) |
| Math-50 / Jaid (`patch1-Math-50-Jaid-plausible.patch`) | recompute block dead (contradictory else/if guard); redundant x1=x overwritten; 4800 cases | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |
| Math-59 / SequenceR (`patch1-Math-59-SequenceR.patch`) | textually the dev fix | UNDETECTABLE.md; b3_certified_overfit.jsonl 0div |
| Math-63 / CapGen (`patch1-Math-63-CapGen-plausible.patch`) | dev fix + dead \|\|x==y disjunct subsumed by equals(x,y,1) for all doubles incl ±0.0; 60M trials | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |
| Math-63 / CapGen (`patch1-Math-63-CapGen.patch`) | patch = dev fix + redundant \|\| x==y disjunct (implied by ulp-equals incl. +/-0.0) | UNDETECTABLE.md (July audit; UNDETECTABLE.md) |
| Math-70 / SketchFix (`patch1-Math-70-SketchFix-plausible.patch`) | functional change byte-identical to dev fix; \|\|i<0 dead (monotonic int counter, no overflow) | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |

## B. Labeled `correct`, but really OVERFITTING (behaviourally wrong / partial fix)
These are in `Dcorrect/` but diverge from the dev fix — a partial fix or behaviourally distinct patch that only happens to pass the trigger test. Do NOT use them as precision (correct) legs.

| Patch | Why it's really overfit | Verification |
|---|---|---|
| Lang-10 / DeepRepair (`patch1-Lang-10-DeepRepair.patch`) | keeps whitespace-collapse but emits first char instead of \s*+ : parse('3  Tue' vs pattern 'M  E') diverges from BOTH dev fix and SimpleDateFormat; passes full FastDateParserTest incl. trigger testLANG_831 (plausible-but-wrong) | /tmp/d4j/wit_anom/WLang10.java on hetzner; unpaired_correct.jsonl 22 strong (July audit; UNDETECTABLE.md) |
| Lang-41 / SimFix (`patch1-Lang-41-SimFix.patch`) | reroutes Class overloads only; root-cause String helpers left broken (getShortClassName('[Ljava.lang.String;') -> 'String;' vs dev 'String[]'); byte-identical to Doverfitting patch1-Lang-41-Arja-plausible; correct-side cert 176 strong value-div | UNDETECTABLE.md; /tmp/l41_*.txt, /tmp/wit/WLang41.java on hetzner; runs-archive/certification/2026-07-21_correct-side/ |
| Math-63 / SimFix (`patch1-Math-63-SimFix.patch`) | dev fix is `equals(x,y,1)`; SimFix adds `\|\| FastMath.abs(y-x)<=SAFE_MIN`, so it returns **equal for UNEQUAL subnormals** (x=-2.225e-308, y=-4.9e-324: `eq1=false` but `absLeSafeMin=true`) — a real widening of the equality contract, not subsumed by 1-ULP equals; 37 strong value-div | cert 2026-07-21 correct-side -> runs-archive/certification/2026-07-21_correct-side/verdicts.md |

> **NOTE (correct-side sweep COMPLETE, 2026-07-21):** all 152 `Dcorrect` patches were
> certified against the dev fix. 142 confirmed correct; **4 mislabels** total — the two
> SEMANTIC ones are above (Lang-41-SimFix, Math-63-SimFix); the two CRASHING ones live in
> `labels/crashing/verified_incorrect.jsonl` (Lang-58-Nopol2015 110 exc-class-div,
> Chart-5-Nopol2015 60 value-div). **3 strong-div corrects were probe FALSE-POSITIVES and
> the label STANDS** (recorded in `verified_correct.jsonl`): Lang-55-Nopol2015 & Lang-55-Jaid
> (StopWatch wall-clock timing noise; both patches are logically the dev fix) and
> Lang-43-CapGen (adds `next(pos)` at the same site as the dev fix). See verdicts.md for evidence.

## C. Correctly labeled `overfitting`, but UNUSABLE (NOT a mislabel — still exclude)
The overfit IS behaviourally distinct from the dev fix, but the divergence cannot be caught by any SOUND black-box oracle (it lives only in a dev-fix bug, non-contractual text/formatting, an environment ceiling, or a deprecated bug). Exclude from recall, but the label is not wrong.

| Patch | Reason unusable | Verification |
|---|---|---|
| Closure-63 / Jaid (`patch1-Closure-63-Jaid-plausible.patch`) | **deprecated_bug** — deprecated in installed Defects4J | DATASET_AUDIT.md §3a EXCLUDED |
| Closure-93 / ? (`?`) | **deprecated_bug** — bug deprecated in installed Defects4J | DATASET_AUDIT.md §3a EXCLUDED |
| Closure-123 / SequenceR (`patch1-Closure-123-SequenceR.patch`) | **no_sound_oracle_divergence** — hardcodes IN_FOR_INIT_CLAUSE: emits REDUNDANT parens in hook branches (f(a?(b in c):d) vs f(a?b in c:d)); outputs parse to identical ASTs — divergence is formatting-only, which a sound oracle may not assert (correct impls legitimately vary). Label defensible only under a textual-output oracle | /tmp/d4j/wit_anom/WC123.java (July audit; UNDETECTABLE.md) |
| Lang-7 / Arja (`patch1-Lang-7-Arja-plausible.patch`) | **environment_conditional** — differs from dev fix only on JVMs with the LANG-822 BigDecimal('--...') quirk; on OpenJDK all divergence is NFE message text (message-only, inadmissible) | UNDETECTABLE.md; /tmp/l7probe/ on hetzner; b3 record 12div/0strong (July audit; UNDETECTABLE.md) |
| Lang-20 / Arja (`patch1-Lang-20-Arja-plausible.patch`) | **detectable_but_no_sound_oracle** — differs only via dev-fix noOfItems*16 int-overflow (NegativeArraySizeException at 2^27); no sound oracle | deepdive 2026-07-21 -> runs-archive/certification/2026-07-21_scoreable-overfits/deepdive_verdicts.md (+ overfit_detectability.jsonl) |

Additional env-ceiling / no-sound-oracle cases from the July curation (see git history of the former `UNDETECTABLE.md`): Lang-7/Arja (LANG-822 JVM quirk, message-only divergence on modern JVMs), Lang-22/Arja (redundant `abs<=1` guard, output-equivalent), Closure-123/SequenceR (redundant-parens formatting only, identical ASTs).

## Appendix — verification-method notes (NOT mislabels)
- **Probe false-zeros are common.** A grid probe over the patched method's domain
  returns 0 divergences when the real divergence lives on a sibling observable,
  a side-effect, an extreme-magnitude input, or a rare branch. Every 0-divergence
  verdict from a patch-method-only probe is UNVERIFIED until deep-dived. Confirmed
  false-zeros that ARE detectable (kept in `verified_correct.jsonl`): Chart-7, Lang-41,
  Lang-60, Closure-62, Math-57 (July); Math-85, Chart-12, Closure-18, Lang-39-Nopol2015,
  Math-50-HDRepair, Math-32-Jaid/Elixir, Lang-6 (2026-07-21).
- **Regression guards** (do NOT re-triage as undetectable): Chart-1/Arja (472 strong div),
  Math-2/Arja (117 strong, visible only via `getNumericalMean` sibling — v1 probe false-zero).
- **Applier caveat:** Lang-50/SimFix once showed 43 strong div — an APPLIER artifact
  (out-of-order hunks silently dropped), NOT a mislabel; label CORRECT stands after fix.

