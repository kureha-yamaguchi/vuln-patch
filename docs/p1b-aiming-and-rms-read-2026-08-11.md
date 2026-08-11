# p1b desk read — where the false convictions actually come from, and why no `getRMS` reference exists (2026-08-11)

Two desk reads asked for at plan 8.45. Analysis only: no code changed, no VM
run, no commit, no `docs/plan.md` edit. Everything below is read out of
`runs-archive/runs/p1b_live_20260811_012407/` (5 legs, full traces),
`p1b_coverage_20260810_205658`, `gs1_isolation_20260810_175503`,
`stack_confirm_20260810_140852`, `mechb_roll_20260809_115543`, plus a sweep
over every archived `result.jsonl`, and out of the source at
`git_sha c99f8cb` (the sha the live run stamped into each `result.jsonl`).

One offline reproduction was run to settle a question the trace truncates:
`src/java/relations/reference_impl.py:observables_probed_by` was replayed on
leg 01's real class context (recovered from `result.jsonl`'s `code_context`)
and leg 01's real kept checks (recovered from the two rule-synthesis outputs
in `trace.md`). It reproduces the live target list exactly, which is what
lets §2 name a rank instead of guessing one.

---

## Correction to plan 8.45 before anything else

Plan 8.45 (`docs/plan.md:1795-1810`) says:

> all 12 reference-firing-state events read NOT RUN — "no
> [fact:rate-indiscriminate] on this conviction" — because THIS draw's
> convictions are HARNESS-channel oracles ([oracle:circle-chisquare]
> FuzzerSecurityIssue), not relation-replay firings

Both halves are wrong in a way that changes the design question.

**The 12 events split 7 / 5, not 12 / 0.** Grepping
`⚙️ reference-firing-state` across the three Math-65 legs:

| leg | step | firing | output |
|---|---|---|---|
| 01 | [148] | `[oracle:circle-chisquar…` | `firing-state reading NOT RUN` |
| 01 | [194] | `[oracle:circle-phase2-c…` | `firing-state reading NOT RUN` |
| 01 | [259] | `relation rms_matches_documented_weighted_mean_formula … [replay-on-patched]` | **`reading: abstain`** |
| 02 | [137] | `[oracle:circle-chi-manu…` | `firing-state reading NOT RUN` |
| 02 | [179] | `[oracle:chi-doc-second]` | `firing-state reading NOT RUN` |
| 02 | [196] | `[oracle:unnamed-check]` | `firing-state reading NOT RUN` |
| 02 | [223] | `relation rms_matches_weighted_residual_mean … [replay-on-patched]` | **`reading: abstain`** |
| 02 | [229] | `relation chiSquare_matches_weighted_squared_residuals …` | **`reading: abstain`** |
| 02 | [235] | `relation rms_matches_documented_weighted_mean_square_formula …` | **`reading: abstain`** |
| 03 | [170] | `[oracle:unnamed-check]` | `firing-state reading NOT RUN` |
| 03 | [188] | `relation chiSquare_matches_weighted_residual_sum …` | **`reading: abstain`** |
| 03 | [237] | `relation chiSquare_matches_documented_weighted_sum …` | `firing-state reading NOT RUN` |

So the reading DID run five times, all five on the relation-replay channel,
and all five abstained — for four different reasons, listed in §1.4. The
harness-channel entries (6 of the 7 NOT RUNs) never got as far as an
abstention reason.

**All three Math-65 convictions in this draw carried a RELATION conviction.**
The verdict has exactly two writers (`src/java/run.py:1549-1566`):
`flag_overfitting`, which records an `outcome-flag` event, and the harness
count at `src/java/run.py:1067`, `rec["crashed_on_patch"] = len(triggered) > 0`,
which records none. Reading both:

| leg | `harnesses_crashed` (= harness triggers that survived) | `outcome-flag` |
|---|---|---|
| 01 | 2 | `[278] relation-replay-conviction … kept: ['rms_matches_documented_weighted_mean_formula']` |
| 02 | 3 | `[237] relation-replay-conviction … kept: ['rms_matches_weighted_residual_mean', 'chiSquare_matches_weighted_squared_residuals', 'rms_matches_documented_weighted_mean_square_formula']` |
| 03 | 0 | `[239] relation-replay-conviction … kept: ['chiSquare_matches_weighted_residual_sum', 'chiSquare_matches_documented_weighted_sum']` |

Leg 03 convicted with **zero** surviving harness triggers. Legs 01 and 02
convicted on both channels at once. A gate that only voided harness
convictions would have converted none of the three.

---

# READ 1 — aiming the firing-state gate at the harness channel

## 1.1 (a) Which channel carries the Math-65 false conviction

Swept every `result.jsonl` under `runs-archive/runs/` for
`project=Math, bug_id=65, label=correct`. 68 legs, 61 of them FP. Channel
attribution uses the two verdict writers above: harness = `harnesses_crashed
> 0`; relation = `relation_replay_kept` non-empty (that list is exactly what
`flag_overfitting` reports as `kept`).

| channel | FP legs | share |
|---|---|---|
| relation only | 20 | 33% |
| harness only | 11 | 18% |
| both channels | 30 | 49% |
| **any relation** | **50** | **82%** |
| **any harness** | **41** | **67%** |

Voiding a leg needs every channel that convicted it to be voided. So the
ceiling on a harness-only gate is **11 of 61 (18%)**; on a relation-only gate,
**20 of 61 (33%)**; the 30 "both" legs need both. Sample of the raw rows
(full sweep reproducible from `result.jsonl` alone):

```
gs1_isolation_20260810_175503  01 FP harness_triggered=3 relation_kept=0  harness
gs1_isolation_20260810_175503  03 FP harness_triggered=3 relation_kept=2  harness+relation
p1b_live_20260811_012407       03 FP harness_triggered=0 relation_kept=2  relation
stack_confirm_20260810_140852  05 FP harness_triggered=0 relation_kept=3  relation
ladder1k_20260807_164149       01 FP harness_triggered=2 relation_kept=0  harness
```

**Read:** the harness channel is real but it is not the majority channel, and
it is almost never the only one. The 8.45 framing — "the FPs changed channel"
— describes a draw-level wobble, not a shift in where the FP population lives.

## 1.2 (b) Does the harness channel have the raw material for a rate fact?

Yes, and it is already built and already runs. The harness track has its own
fire-rate machinery:

* `src/java/run.py:4235-4299` — the **universal screen**: for a fired oracle
  with no rate known, rebuild the harness with every sibling alarm muted and
  the target's `throw` replaced by a counter, run it on the buggy build, and
  parse `[relscreen] checked=N violated=M`. Implementation:
  `src/java/execution/oracle_mute.py:336-420` `instrument_for_counting`.
* `src/java/run.py:1446-1500` `_deliver_buggy_rate` — attach the resulting
  `[fire-rate fact]` to the judging evidence whichever branch measured it.
* `src/java/relations/evidence_facts.py:66` — the fact tag
  `[fact:rate-indiscriminate]`, awarded when the buggy-side rate is at or
  above `INTRINSIC_FIRE_RATIO = 0.95` (`evidence_facts.py:38`).

It produces real measurements. Tallying `cycle6_universal_screen_decided`:

| run | measured | not-instrumented | skipped (rate already known) | compile-failed | cached |
|---|---|---|---|---|---|
| `p1b_live` | 2 | 3 | 3 | — | — |
| `p1b_coverage` | 3 | — | 6 | 1 | — |
| `gs1_isolation` | 4 | 1 | 4 | — | 1 |
| `stack_confirm` | 17 | 3 | 12 | — | — |
| `mechb_roll` | 3 | 3 | 15 | — | 1 |

**But it declined on exactly the oracles that convicted.** Leg 01, step [136]:

> `cycle6_universal_screen_decided · circle-chisquare-formula`
> **output:** **not-instrumented**
> reason: no counting variant could be built for oracle
> 'circle-chisquare-formula' (target id or entrypoint not found) — nothing
> measured (fail-open)

and step [139]:

> `cycle6_rate_absent · circle-chisquare-formula` — **no-rate** — no buggy-side
> counts exist for this oracle — neither a matched relation's screen nor the
> universal screen produced any

so the gate ladder then read, at step [143]:

> `cycle6_6B_indiscriminate_considered` — **no-measurement · rate=None** — no
> [fire-rate fact] block appears in this evidence — the fire rate was never
> measured

The one leg-01 oracle that WAS measured is the one nobody needed: step [202]
`readonly-covariances-state` — **measured**, "buggy-side counts
violated=999/1000" — and by the time 6B saw it, step [222] read
"**alarm-already-discarded** … this alarm was already explained away upstream
of the gate". That harness was then dropped whole at step [226]
(`DROPPED by all-fired-oracles-unsound`).

`instrument_for_counting` returns `None` when "fuzzerTestOneInput, its
enclosing class brace, or a throw carrying `target_id` cannot be located"
(`oracle_mute.py:367-369`). Three not-instrumented in `p1b_live`, three in
`mechb_roll`, three in `stack_confirm` — the miss is recurrent, not a one-off.

**The harder finding about the tag.** `firing_state_reading_applies`
(`reference_impl.py:493+`) is tag-only on `[fact:rate-indiscriminate]`. On the
relation track that tag is not itself terminal. On the harness track it *is*:
6B discards a standing alarm carrying it. So the only harness firings that
would reach an aimed reading are the ones that carry the tag **and survived
6B** — `cycle6_6B_indiscriminate_decided` = `escaped`. Sweeping every archived
trace and joining to each leg's label:

| label | `6B escaped` events | legs |
|---|---|---|
| overfitting (true positive) | 33 | 8 legs, all Chart-19-o / Chart-7-o |
| correct (false positive) | 11 | 7 legs (Chart-26, Math-86, Chart-19-c, Closure-62, Math-73, Time-4) |
| **Math-65 legs** | **0** | — |

Every escape is defended by the same answer, e.g. `stack_confirm/01`:

> **escaped** — reason: family-duty YES: The fired check asserts the same
> observable that the real failing test covers—`CategoryPlot.getDomainAxisIndex(null)`
> must throw `IllegalArgumentException` after a `setDomainAxis(...)`
> mutation—just at a different slot/input.

**Read:** aiming today's entry condition at the harness channel would point
the reading at a population that is 3:1 true positives and that contains zero
Math-65 rows. It is a safety exposure with no measured upside on the target
bug.

## 1.3 (c) Is there recorded state at a harness conviction?

No. The state the reading needs is written by the **relation** harness only.
`src/java/relations/relation_screen.py` installs three things into a relation
check and nothing else:

* the recording data source, whose firing line appends
  `" __consumed=" + ((RecFDP) data).consumed()` (`relation_screen.py:291`);
* `capture_receivers` (`relation_screen.py:205-230`), which splices
  `__rcv.put("var", var);` after every locally constructed object;
* `_STATE_PRINTER` (`relation_screen.py:145-190`), which reflects each captured
  receiver's primitive and primitive-array fields into
  `" __rcvstate <label>:<SimpleName> f=v f=v …"`.

The LLM-written `FuzzHarness` gets none of them. Grepping leg 01's trace,
all 12 `__rcvstate` occurrences sit on `[relfire]` lines
(`trace.md:14558, 14566, 14586, 14594, 14602, 14610` and their six echoes in
the judge prompts). Every harness firing in that leg is state-free, e.g.

> `[oracle:circle-chisquare-formula] metamorphic violation: relation=getChiSquare inverse-weight formula n=634 lhs=1.5633763529538318 rhs=6.253505411815327 tol=6.253505411815327E-12`

The reading's own three-way split (`reference_state.py:75-91`) looks for
` __consumed=` and ` __rcvstate `; with neither present it returns the whole
line as `message` and empty `consumed`/`state`, and the reading exits at
`reference_state.py:363-379`:

> `unmappable: the firing line carries no recorded receiver state, so there is
> no state at which to evaluate the reference`

That is the ONLY thing an aimed harness reading would print today. Two things
that *are* present at harness convictions and would not need repair:

* **The admitted reference is found.** Leg 01 step [147]:
  `reference-admission-lookup` — "admitted reference found … admitted
  reference for `chisquare` — the observable this firing itself disputes (from
  `getChiSquare`)", detail `{'admitted': ['chisquare','valueref'], 'used':
  'getChiSquare'}`.
* **The two numbers are printable.** `message_values`
  (`reference_state.py:261-298`) needs an observed value and a single
  `expect…=` key; the harness firing above prints `lhs=` and `rhs=`, and
  `buggy-side-observed-values` at step [126] already parses that same line
  into `{'relation': …, 'n': ['634'], 'lhs': [...], 'rhs': [...]}`.

So the gap is narrow and specific: **receiver state, and nothing else.**

## 1.4 The relation-channel abstentions, for comparison

The five readings that did run name four distinct blockers, and none of them
is "the reading is wrong":

| leg/step | reason | what is actually missing |
|---|---|---|
| 01 [259] | `no-matching-receiver: none of ['opt:LevenbergMarquardtOptimizer'] is a declaring type of the disputed method (['VectorialPointValuePair'])`, `admitted_for: 'getValueRef'` | the leg held no `getRMS` reference — READ 2 |
| 02 [223], [229], [235] | `no-observable: … observed='14.733245165934543' claimed=None from the message region (0 key(s) parsed…)`; message: `relation rms_matches_weighted_residual_mean violated: expected 15.152400407626196 but got 14.733245165934543` | `message_values` needs `expect…=`; these firings print the prose form `expected X but got Y` |
| 03 [188] | `parameters-not-read-by-method: the reference is declared over ['jacobian','cols','rows','residuals','residualsWeights','cost'], and the disputed method's own body reads ['rows','residuals','residualsWeights']` | the §2.7 guard doing its job on an over-wide signature |

Three of the five are one missing regex; one is READ 2's missing reference.

## 1.5 (d) Recommended next design for READ 1

**Do not aim the current entry condition at the harness channel.** §1.2 shows
that population is 33 TP / 11 FP / 0 Math-65, and §1.3 shows it would abstain
`unmappable` on every row anyway.

Recommend instead, in this order:

**Design R1 — record state on the harness channel, measurement-only.**
Station: `src/java/execution/fuzz_runner.py`'s single-input report-replay
(`fuzz_runner.py:1570-1655`), which already compiles and runs a *variant* of
the accepted harness on a saved input and already has the fail-open shape.
Add one more variant alongside the diversion variant: splice
`relation_screen.capture_receivers` + `_STATE_PRINTER` + the `RecFDP` wrapper
into the harness source and re-run the firing input, harvesting a
`[relfire]`-shaped line that carries `__rcvstate`. Nothing new is written:
`instrument_for_counting(record_firing=True)` already re-executes the target
`throw` inside a local `try` and prints its message as `[relfire] <message>`
(`oracle_mute.py:357-366`), so the seam exists; this adds the state tail to
the same spliced statement.

In this first build the resulting reading has **no verdict effect at all** —
it records `reference-firing-state` events and stops. Cost: one javac plus one
single-input Jazzer run per fired oracle, the same envelope the diversion and
counting variants already spend.

**Design R2 — close the counting miss.** Find why `instrument_for_counting`
cannot locate the throw for `circle-chisquare-formula`,
`circle-phase2-chi-square-formula`, `circle-chi-manual-second` and
`unnamed-check`. Nine not-instrumented events across three archives is enough
sample to diagnose offline from the archived harness sources; the `unnamed-check`
ids suggest the oracle tag is not on the literal the classifier reads.

Risks, stated plainly:
* `capture_receivers` was hardened once already after "zero `__rcvstate` across
  nine traces" (`relation_screen.py:205-218`); an LLM-written harness has
  construction shapes the relation checks do not, so a silent no-capture is
  the likely first outcome. R1 is measurement-only precisely so that failure
  costs nothing but a JVM run.
* Splicing into the harness that produced the verdict is the one place p1b
  could damage a real catch. Running it as a separate compiled variant on a
  saved input, never as the harness that decides, keeps that impossible.
* Cost grows with fired oracles per leg, not with legs. The existing per-leg
  cap of 8 measured oracles (`run.py:1396-1400`) is the precedent to copy.

**Prereg for R1+R2, to be written before any build:**
* *G-H1 (safety, hard stop).* Chart-19-o ×3 + one further overfit leg. Any
  `conviction VOIDED` on an overfit leg ends the roll. R1 has no verdict
  effect, so the prediction is zero voids by construction; a void means the
  build is not measurement-only and the roll is void.
* *G-H2 (recording works).* Math-65-c ×3. At least 6 of the harness-channel
  `reference-firing-state` events must carry a non-empty `__rcvstate` region.
  Fewer than 6 → `capture_receivers` does not fit LLM harnesses and R1 is
  rejected, not patched blind.
* *G-H3 (the reading completes).* Of the events in G-H2, at least 3 must reach
  one of the four readings rather than an abstention. Any abstention reason
  new to `ABSTENTION_REASONS` is a design finding to be written down, not a
  bug to be silenced.
* *G-H4 (counting miss).* After R2, `not-instrumented` must be 0 on the same
  Math-65 draw, and at least one convicting oracle must reach 6B with a
  measured rate.
* *G-H5 (direction check, read-only).* Across G-H1+G-H2 legs, tabulate reading
  by label. Giving the reading a verdict effect on the harness channel is
  gated on: zero `agrees-with-patched` readings on overfit legs, and at least
  one on a Math-65-c leg. Until that table exists, the harness gate stays off.
* Suite green, no judge or verifier prompt touched, no cross-run pooling.

**Ceiling to state in the plan entry:** even a perfect harness gate converts at
most 11 of 61 archived Math-65 FP legs on its own. The relation channel
(§1.4's three fixable abstentions plus READ 2) is where 50 of 61 live.

---

# READ 2 — why no `getRMS` reference is ever admitted

## 2.1 (a) It was never REQUESTED by the widening — the answer, with the rank

Across all five `p1b_live` legs there are exactly 15 `reference REQUESTED`
events (3 per leg, the cap `config.P1B_MAX_REFERENCES = 3`,
`src/config.py:369`). None of them is `rms`:

| leg | requested | outcome |
|---|---|---|
| 01 | `optimize`, `chisquare`, `valueref` | REJECTED, **ADMITTED**, **ADMITTED** |
| 02 | `optimize`, `chisquare`, `valueref` | REJECTED, REJECTED, **ADMITTED** |
| 03 | `optimize`, `chisquare`, `pointref` | REJECTED, **ADMITTED**, **ADMITTED** |
| 04 | `add`, `maxmiddleindex`, `start` | REJECTED, REJECTED, REJECTED |
| 05 | `axis`, `drawlabel`, `rectangleedge` | REJECTED, REJECTED, REJECTED |

The enumeration event (leg 01 step [50]) reads:

> `reference-widening · enumerate` — **3 observable(s) to widen onto**
> reason: the kept checks probe ['optimize', 'chisquare', 'valueref',
> 'abstractleastsquaresoptimizer', 'incrementiterationscounter',
> 'updatejacobian']; 22 of those have no admitted reference and 3 fit under
> the per-leg cap of 3 (the leg already holds [])
> detail: {'targets': ['optimize', 'chisquare', 'valueref'],
> 'already_admitted': [], 'cap': 3, 'checks_read': 11}

That printed list is **truncated to six** by
`reference_impl.py:483` (`{[admission_key(n) for n in probed][:6]}`), so it
does not prove `rms` is absent — it proves `rms` is not in the first six. The
count `22` is the real length of the fresh list.

**Do the kept checks probe `getRMS`?** Yes, unambiguously. Leg 01 kept
`rms_matches_documented_weighted_residual_mean` (step [46], "kept:
direction-confirmed") whose check body contains `rms = opt.getRMS();`, and
`unit_weights_link_chiSquare_and_rms` (step [48]) which calls both
`opt.getChiSquare()` and `opt.getRMS()`. The class context declares it: the
reference chain's own "screening surface resolved" event (step [53]) lists
siblings `['getChiSquare', 'getCovariances', 'getEvaluations', 'getIterations',
'getJacobianEvaluations', 'getRMS', 'guessParametersErrors']`.

**So where does `getRMS` rank?** Replaying `observables_probed_by` on leg 01's
own `code_context` and its 11 kept checks reproduces the live targets exactly
(`['optimize','chisquare','valueref']`) and gives the full ordered list:

```
 0 optimize                     8 getRMS
 1 getChiSquare                 9 getEvaluations
 2 getValueRef                 10 getJacobianEvaluations
 3 LevenbergMarquardtOptimizer 11 getIterations
 4 FunctionEvaluationException 12 getConvergenceChecker
 5 OptimizationException       13 getMaxIterations
 6 VectorialPointValuePair     14 getPointRef
 7 getValue                    15 getPoint
```

**`getRMS` is at rank 8. The cap takes the first 3.**

The mechanism is the ordering rule, not the cap. `observables_probed_by`
(`reference_impl.py:436-450`) is a flat concatenation in **arrival order
across checks**, and `disputed_observables` (`reference_impl.py:135-154`) is
called with the check source in BOTH the message and the `check_source`
positions. Because `_methods_named_by` (`evidence_facts.py:1433-1459`) already
returns every call-syntax hit, the `both` tier swallows the whole check, and
the `msg_only` tier then adds every declared method whose name appears as a
word. So **check #1 alone contributes ranks 0-7**, and `getRMS` — which first
appears in check #2 — cannot be reached. Per-check contributions, same replay:

```
chisquare_matches_documented_inverse_weight_formula -> optimize, chisquare, valueref,
    levenbergmarquardtoptimizer, functionevaluationexception, optimizationexception,
    vectorialpointvaluepair, value
rms_matches_documented_weighted_mean_formula        -> optimize, rms, valueref, …
```

Ranks 3-6 are **type and constructor names**, not observables. They arrive
because `_method_declared` (`reference_impl.py:183-188`) accepts a
constructor's declaration (`protected AbstractLeastSquaresOptimizer() {`
matches `type name(...) {`), and `_WORD_RE` in `_methods_named_by` matches the
class name wherever the check writes `new org.apache…LevenbergMarquardtOptimizer()`.
Leg 05 shows the cost directly: it spent all three of its requests on `Axis`,
`drawLabel` and `RectangleEdge` and was told, verbatim,

> `widening REJECTED for observable 'rectangleedge'` — reason: reference omits
> the disputed observable — DISCARDED — declared: ['toString', 'hashCode',
> 'readResolve'] — none normalizes to `RectangleEdge`

## 2.2 (b) It WAS requested elsewhere — and rejected, always for the same reason

The widening is not the only requester. The on-demand chain still runs at the
judge doors, and it asked for `getRMS` in all three Math-65 legs. Leg 01, step
[241]: `reference-impl · getRMS` — "disputed observable detected", candidates
`['getRMS','optimize','getValueRef','getMaxIterations']`. It generated,
matched, mapped, built the twin, ran both sides — and then, step [254]:

> **screen DISCARDED** — reason: reference disagrees with the buggy build on
> off-defect observable `getChiSquare` (['6.253505411815327'] vs
> ['1.5633763529538318']) — DISCARDED
> detail: {'construct': 'OK', 'off_defect_shared': 3}

Leg 03 step [203] is the same line, same two numbers. Leg 02 failed earlier,
at step [218]: `reference omits the disputed observable — DISCARDED —
declared: [] — none normalizes to 'getRMS'`.

Sweeping every archived trace for `reference-impl · getRMS` outcomes:

| outcome | count |
|---|---|
| `reference disagrees with the buggy build on off-defect observable getChiSquare (['6.253505411815327'] vs ['1.5633763529538318']) — DISCARDED` | **11** |
| `declared: [] — none normalizes to getRMS` | 2 |
| `parameter double[] matches no canonical state field … signature unmappable, DISCARDED` | 1 |
| **admitted** | **0** |

**Why that discard is structural, not bad luck.** `screen_reference`
(`reference_impl.py:735-800`) grades a candidate reference against the BUGGY
build on the observables the family-duty boundary calls off-defect:

> `off_defect_keys` are the observables the family-duty boundary says the
> defect does not touch. Only those are screened — screening ON the defect
> would require the reference to reproduce the BUG, which is backwards.

For a `getChiSquare` reference, `getChiSquare` is excluded from that set — it
is the disputed point. For a `getRMS` reference it is **not** excluded: it is
just another sibling on the same class. But Math-65's defect *is*
`getChiSquare`. Any `getRMS` reference that is correct necessarily computes
chi-square correctly, therefore necessarily disagrees with the buggy build
there, therefore is discarded **for being right**. The two numbers are the
same in all 11 events (`6.2535…` vs `1.5634…`, a factor of exactly 4 — the
weights-versus-reciprocal-weights defect at the failing test's weight of 2).

The escape hatch exists but does not cover this key. `test_corroboration`
re-grades a disagreement as a pass when the failing test pins the value
(`reference_impl.py:753-760`, applied at the `pin` branch of the same
function); leg 01 step [253] shows it firing for
`guessParametersErrors` (`pins: {'guessParametersErrors': ('0.004','0.001')}`).
The failing test asserts `rms` and `errors`, not `chiSquare`, so `getChiSquare`
gets no pin and no rescue.

**A third, independent symptom of the same ordering rule.** Even when a leg
holds references, the wrong one is chosen for an rms firing. Leg 01 step [258]:

> `reference-admission-lookup` — **admitted reference found** — reason:
> admitted reference for `valueref` — the observable this firing itself
> disputes (from `getValueRef`) — detail: {'admitted': ['chisquare',
> 'valueref'], 'used': 'getValueRef'}

on a firing named `relation rms_matches_documented_weighted_mean_formula
violated`. The next event, [259], is the abstention
`no-matching-receiver: none of ['opt:LevenbergMarquardtOptimizer'] is a
declaring type of the disputed method (['VectorialPointValuePair'])`. The
lookup used `disputed_observables` on the firing, `getValueRef` outranked
`getRMS` there for the same reason it does in the widening, and the reading
was handed a reference for a different class.

## 2.3 (c) The smallest fix

It is **two** changes, and neither alone produces an rms reference.

**Fix 1 — scope the widening enumeration to the patched class's own no-argument
readers.** Station: `reference_impl.observables_probed_by` /
`widening_targets`. After building `probed`, keep only names that the
`role="patched"` class block declares with an EMPTY parameter list. Both
ingredients exist: the class context already carries `role="patched"`
(`result.jsonl:code_context`, and it is what `reference_gen.sibling_observables`
already scopes on), and `_method_declared`'s regex already captures the
parameter region — it only needs `\(\s*\)` instead of `\([^)]*\)`. Replayed
offline on all five `p1b_live` legs' own contexts and checks:

| leg | live targets | targets under Fix 1 | eligible pool |
|---|---|---|---|
| 01 | optimize, chisquare, valueref | **chisquare, rms, evaluations** | 7 |
| 02 | optimize, chisquare, valueref | **chisquare, rms, covariances** | 6 |
| 03 | optimize, chisquare, pointref | **chisquare, rms, covariances** | 9 |
| 04 | add, maxmiddleindex, start | maxmiddleindex, itemcount, minmiddleindex | 7 |
| 05 | axis, drawlabel, rectangleedge | visible | 1 |

`rms` enters on all three Math-65 legs **with no change to the cap**, and all
five wasted requests observed live (`optimize` ×3, `add`, `Axis`,
`RectangleEdge`, plus the type names) disappear. Leg 04's `getStart` request
— which died with `javac: cannot find symbol: method getStart() location:
variable s of type TimePeriodValues` (step [78]) — is dropped for the same
reason it failed: `getStart` is not declared on the patched class. Leg 05
honestly shrinks to one target, which is the truth about that class's
surface, and saves two LLM generations.

**Fix 2 — take the patched method's own observable out of `off_defect_keys`.**
Station: `reference_impl.screen_reference`'s caller, wherever
`off_defect_keys` is assembled. The rule today is "siblings minus the target".
It should be "siblings minus the target **minus every observable the patch
touched**". The patch-touched set is already computed and already in the
prompt: the `<code>` block in the synthesis prompt is the buggy body of the
changed method(s), and `evidence_facts._defined_methods` reads exactly that
shape. Without Fix 2, Fix 1 buys three more LLM generations per Math-65 leg
and eleven-for-eleven discards.

**Risks.**
* Fix 1 narrows the surface. On a class whose patched observable takes
  arguments, or whose disputed observable is a mutator, the eligible pool can
  be empty and the leg widens onto nothing. That is a *correct* answer
  (`widening_targets` already has a reasoned empty-list branch), but it turns
  today's three-wasted-requests into zero-requests, so the coverage number
  must be read as "admissions per leg", never as "requests per leg".
* Fix 2 loosens a fail-closed screen. Removing a key from `off_defect_keys`
  reduces the evidence a reference must reproduce, and `screen_reference`
  already refuses below `MIN_SCREENED_OBSERVABLES = 3`
  (`reference_impl.py:81`). On a class with exactly three computed siblings,
  dropping the patched one drops the leg below the bar — fail-closed, which is
  the right direction, but it means Fix 2 can *reduce* admissions on
  small-surface classes. Math-65 has 7 siblings, so it survives; Chart-26 has
  1 and is already below the bar.
* Neither fix makes an rms reference *good*. §2.2's rejected candidates were
  never screened successfully, so their quality is unmeasured. A wrong rms
  reference that passes a weakened screen is the one way this design could
  void a real catch — which is what the prereg below is for.

**Prereg gates.**
* *G-R1 (offline, zero LLM).* Replay `observables_probed_by` + `widening_targets`
  under Fix 1 on all five `p1b_live` legs' archived contexts and checks. Pinned
  prediction, exactly the table above: `rms` in the target list for legs 01,
  02, 03; `optimize`/`add`/`Axis`/`RectangleEdge` absent from all five.
* *G-R2 (offline, zero LLM).* Replay `screen_reference` under Fix 2 on the 11
  archived `getRMS` discards, using each trace's own recorded reference and
  buggy observable maps. Prediction: all 11 stop being discarded *for
  `getChiSquare`*. Any that then fail on a different sibling is a separate
  finding and must be written down before the live roll.
* *G-R3 (live, target arm).* Math-65-c ×3 at the 8.45 flags. Gate:
  `reference ADMITTED for observable 'rms'` on **at least 2 of 3** legs.
  Fewer → the request is not the binding constraint and the design is wrong,
  not under-tuned.
* *G-R4 (live, the reading).* On any leg passing G-R3, the rms relation firings
  must stop abstaining `no-matching-receiver`. At least one must reach a
  reading. `agrees-with-patched` is the hoped-for value and would be the first
  p1b void; `agrees-with-check` on a `correct`-label leg is a redesign trigger
  per design §6.1, not a shrug.
* *G-R5 (safety, hard stop).* Chart-19-o ×3 plus one further overfit leg, same
  flags. Any `conviction VOIDED` on an overfit leg ends the roll and Fix 2 is
  reverted. This is the standing clean-leg rule pointed in the direction Fix 2
  can damage.
* *G-R6 (cost).* Requests per leg must not exceed `P1B_MAX_REFERENCES`, and
  total LLM calls per Math-65 leg must not exceed the `p1b_live` baseline of
  24 (leg 01), 20 (legs 02/03), read verbatim from `tokens_by_model.calls`.
* Full pytest green; no judge or verifier prompt touched; no cross-run pooling.

**Independently of both fixes**, §1.4's second row is the cheapest single
improvement in this whole read: three of the five completed readings abstained
`no-observable` because `message_values` (`reference_state.py:293`) requires an
`expect…=` key and the firing printed `expected 15.152400407626196 but got
14.733245165934543`. One prose matcher for the `expected <num> but got|was
<num>` form, added next to `_TAGGED_NUM_RE`, converts three of five
abstentions in this draw. Worth running as its own offline gate on the
archived firing lines before anything else here is built.

---

## Appendix — what was NOT established

* Whether an admitted `getRMS` reference would read `agrees-with-patched` on
  leg 01's firing. The design's §0.1 hand-computed exemplar says the
  arithmetic works for `stack_confirm/06`'s RMS conviction; this read did not
  redo it for `p1b_live/01`, and G-R4 is written so the live roll answers it.
* Why `instrument_for_counting` cannot find the throw for the four named
  oracles. Named as R2; diagnosable offline from archived harness sources,
  which this read did not open.
* Whether `capture_receivers` fires at all on an LLM-written `FuzzHarness`.
  G-H2 is written to measure it rather than assume it.
