# Contradiction experiment — protocol, written before running

Pre-committed per the standing rule. Not yet run; ~1M tokens, pure API calls, does
not touch the VM.

## The hypothesis

The reviewer *can* find the case-specific deciding fact — it does so in some rolls
and not others on identical input (Math-65: 2 dismissals quote
`residualsWeights[i]` verbatim, 4 accusations cite NONE). So the gap is
engagement, not capability, and the fix is task structure — the same move the
CITATION line made on the dismissal side, where compliance went to 100%.

## Two phrasings that DO NOT work, established for free while building the guard set

**(1) "Does the shown source contradict the check's premise?"** — fatal. The
source under review is the *patched* source, and on a fake patch it is buggy. So:

* false accusation: premise misstates the contract, source disagrees → void ✓
* genuine catch: premise states the contract correctly, buggy source disagrees → **void ✗**

"Source disagrees with premise" is the *definition of a catch*. This phrasing
would void genuine catches, and the guard population (67 kept alarms on
genuine-catch legs) is exactly where it would do the damage.

**(2) "Does the JAVADOC contradict the premise?"** — attractive, because
documentation is largely unchanged by a patch and is therefore label-independent,
and javadoc is present in 228/228 fixture rows. But it fails on the motivating
case. Math-65's javadoc says the residuals' "variances are the reciprocal of the
weights", which read naively yields chi² = Σ residual²·weight — the *multiply*
form the accusers asserted. The code divides by `residualsWeights[i]`, correct
only if that field holds variances. The honest dismissals relied on the CODE, not
the docs.

## What that leaves, stated honestly

The distinction the question must draw is "**is this premise a legitimate contract
for this API?**", not "does the code agree with it". And that is very close to the
soundness question the reviewer is already asked and already fails.

**So the experiment's value does not rest on asking something logically new.** It
rests on a narrower claim: that isolating the question, forcing a verbatim quote,
and treating the answer mechanically changes *engagement* — exactly what happened
on the dismissal side. That is a real and testable hypothesis, but it is a weaker
one than "we found the missing question", and the write-up should say so.

## The question to be asked

> Here is a check's premise about what any correct implementation of this API must
> do. Here is the patched class source and its documentation.
> Is the premise a legitimate universal contract for this API, or does the shown
> material show it to be over-tight — true only of some implementations?
> If over-tight, quote verbatim the line that shows it. Otherwise answer NONE.

Answer treated mechanically: a quoted line must appear literally in the shown
material, or it counts as NONE. NONE leaves the accusation standing.

## Populations

* **24 adjudicated accusing checks** across the 7 decisive cases — of which ~11
  are contradiction-class (Lang-60 2, Math-39 4, Math-73 1, Math-65 4 of 5) and
  ~13 are not (Math-30 6, Closure-62 4, Chart-26 2, Math-65 1).
* **67 kept alarms on genuine-catch legs** (12 distinct bugs) — the guard set. All
  must answer NONE.

Each replayed with the exact recorded context the reviewer originally saw. The
fixture carries it; nothing is reconstructed.

## Pass criteria, fixed now

1. **Finds the known contradicting line in ≥7 of the ~11 contradiction-class
   checks.** The backtrack already located the line in each, so this is scored
   against a known answer key, not against a verdict.
2. **Answers NONE on ≥95% of the 67 guard alarms** (≤3 false voids). Voiding
   genuine catches is the failure mode that matters; this is the criterion that
   can kill the mechanism outright.
3. **Answers NONE on the ~13 non-contradiction accusing checks.** Voiding these
   would mean the question is finding contradictions that are not there — a red
   flag, not a bonus, and evidence it would misbehave on the guard set at scale.

Failing (2) kills it regardless of (1). Passing (1) and (2) while failing (3)
means the question works but is over-eager, and needs the answer verified
mechanically before use.

## Standing caveat

Seven of these cases have answers we already know, so a positive result is
promising, not proven. The ladder does not change: recorded cases → guard
population → one live smoke → only then a real run.
