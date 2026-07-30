# Is there a computable fact that separates genuine catches from chronic false accusations?

Design study, zero token cost, run entirely against the recorded pair data.
The question behind cycle 8: accusations face no evidence requirement, and neither
adjacent verbatim source (Math-65) nor a revived dismissal rule (Closure-62) can
dislodge them. Before designing an enforcement mechanism, does any *computable*
fact split the two populations?

**Population:** every alarm the reviewer KEPT (`VERDICT: SOUND`) across both
paired rolls — 67 on legs that ended as genuine catches (TP), 23 on legs that
ended as false accusations (FP). Kept verdicts, not raised alarms: the earlier
finding that alarm *count* does not predict outcomes measured raised alarms, and
the kept-verdict version was untested until now.

## Answer: no. None of the tested facts separate.

### Candidate 1 — corroboration count

Do genuine catches rest on several independent check families while chronic false
accusations rest on one?

| | legs | median kept families | resting on exactly ONE |
|---|---|---|---|
| genuine catches (TP) | 19 | 3 | 4 (21%) |
| false accusations (FP) | 10 | 2 | 4 (40%) |

Directionally right, and useless as a gate. Requiring ≥2 kept families would drop
**4 false accusations and 4 genuine catches** — an exact wash. Requiring ≥3 drops
8 and 8. The distributions overlap across their whole range.

### Candidate 2 — firing location and evidence shape

Do genuine catches fire at or near the failing test's own scenario, while chronic
accusations fire on scenarios the harness invented?

| feature of the kept alarm | in TP | in FP |
|---|---|---|
| fires at the test's OWN input literals (trigger-tier) | 40% | 35% |
| lifts the failing test | 1% | 9% |
| carries a fire-rate measurement | 55% | 57% |
| replay-confirmed on the patched build | 63% | 52% |

Nothing separates. The largest gap is 11 points on replay-confirmation, in a
sample of 67 versus 23, and it runs only slightly in the expected direction.

**This is the structural-ceiling answer, in writing.** On this trap set, under our
current rules, the evidence we record does not distinguish a kept genuine catch
from a kept false accusation. Any enforcement mechanism built on these facts would
be a coin flip dressed as a rule — which is precisely the mistake the citation
filter would have been (rejected in 2b at 12% vs 4%).

## Candidate 3 is different, and it is buildable

The raw-value recording contract is not a classifier — it is a *soundness* fix for
a rung that is currently unsound.

Measured over accepted harnesses carrying an alarm: **10 of 60 (17%) normalise
before comparing** (whitespace stripping, trim, case folding), and only 2 of those
10 report a raw value alongside the normalised one.

For every normalising check, the exact-match dismissal rung is unsound by
construction: the fired value is a *derivative* of the pinned value, so it can
never equal it, so the rung can never fire — regardless of whether the alarm
deserves dismissal. That is exactly Closure-62:

```
test pins  : '...error description here\nassert (1;\n          ^\n'
alarm says : 'lhs=javascript/complex.js:1:ERROR-errordescriptionhere'
```

If the harness also recorded the raw pre-normalisation value, the comparison could
ask the sound question — *does the patch's actual output equal what the test
pins?* — instead of the unanswerable one. On Closure-62 that plausibly resolves
correctly: if the raw output matches the pinned string, the alarm concerns
something the test does not pin, and dismissal is right.

**Scope: ~17% of harnesses. Generation-side, so it cannot be validated against the
recorded data — it needs regenerated harnesses. Cycle-8 build, not a today build.**

## What this licenses, and what it forbids

* **Forbidden:** any accusation-side gate built on corroboration count, firing
  location, fire-rate presence, or replay-confirmation. Measured non-separating;
  building one would repeat the citation-filter error with worse evidence.
* **Licensed for design:** the raw-value recording contract, as a soundness repair
  to the exact-match rung — narrow, honest about its 17% reach, and the only
  candidate that would change what a fact *means* rather than how loudly it is
  delivered.
* **Still open:** whether enforcement can work at all when no recorded fact
  separates the populations. It may be that the accusation side needs a different
  *kind* of evidence than we currently collect, rather than a rule over what we
  have. That is the honest cycle-8 starting point.
