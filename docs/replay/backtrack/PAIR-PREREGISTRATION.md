# Cycle-8 closing pair — pre-registered BEFORE the run

Two identical 30-leg rolls, `suites/cases/pool30.cases`, verified byte-identical
to the July pair (`final30A`/`final30B`): same 30 cases, same flags, same model.
Serial, not parallel — disk is the binding constraint at 11G free.

Written and committed before launch. Six questions, each with its criterion
fixed now.

## 1. The 8.10 PASS bar (the headline)

**PASS =** paired mean > 0.685 **AND** ≤5 accusations per roll **AND** zero
accusations on historically clean legs.

The "historically clean" set excludes Math-39, which 8.6 removed after naming it
a fourth residual. Named residuals: Closure-62, Math-30, Math-65, Math-39.

## 2. 8.4's live guard — the DISMISS branch

The raw-vs-pinned comparison is live for the first time at scale. Read
**per-event**, not from totals:

* how many firings produce `matches` (the only branch that licenses dismissal)
* of those, how many are on **fake-patch legs**, where a dismissal voids a
  genuine catch

**Fail condition, fixed now:** any `matches`-driven dismissal on a fake-patch
leg that the July pair kept. That is the dismiss-pushing risk the archived
guards structurally could not test.

## 3. 8.4's live guard — the KEEP branch (added after the second smoke)

`differs` is keep-leaning: it tells the judge test-passage does not exonerate
the firing. Its first live firing landed on a correct patch.

* how often `differs` fires on **correct-patch** legs
* whether those legs accuse

Not a fail condition — a measurement. The batch shipped with this named as
unmeasured, and this is where it gets measured.

## 4. Repair provenance (8.7) at scale

First read beyond two legs. Rate of `FROM REPAIRED ATTEMPT` among accepted
harnesses, and whether repaired-accepted harnesses differ in outcome from
first-try ones. Descriptive; no criterion.

## 5. 8.3's value channel, live

Both replay paths now record observed values. Measure:

* fraction of buggy-side replay steps carrying ≥1 observed value
  (**archived baseline: 0 of 1,452**)
* the per-firing observable count distribution

## 6. Does 8.2's expensive half get built?

The offline reach was **8.6%**, with **32 of 54** trigger rows recording no
comparable observable at all. The open question is whether that 0-observable
rate is a property of these ARCHIVED runs — recorded before 8.3 existed — or of
the mechanism.

**Decision rule, fixed now:**

* live rows reaching ≥3 observables **≥20%** of trigger rows → build the
  execution adapter
* **< 20%** → do not build; record 8.2's core as shipped-and-parked with the
  reach that closed it

## Standing constraints

* Verdicts are the measurement here — unlike the smokes, this pair IS the
  scored comparison, against the July pair on an identical suite.
* Raw committed before scoring.
* Two rolls, because a single roll cannot distinguish a real move from the
  measured 27% per-leg flip rate.
* No mechanism changes between the two rolls. If anything is changed, the pair
  restarts.
