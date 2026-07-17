# Semantic-bug detection — the plan

Restructured 2026-07-18: ground rules first, finished work as a list by
pipeline station, current scoreboard, then remaining work by station
ordered by impact-vs-risk, rejected ideas at the bottom. The full
pre-restructure text (with the long per-phase case histories) is
preserved verbatim at the end of `semantic-recall-history.md`.
Companion docs: `suites/DATASET_AUDIT.md` (inventory + verdicts),
`suites/UNDETECTABLE.md` (exclusion evidence),
`suites/pinned_tasks.jsonl` (the verified task set),
`suites/label_annotations.jsonl` (label corrections).

---

## The problem

We are given a bug and a candidate patch for it. Some patches are real
fixes; some are **overfit** — they make the bug's failing test pass
without actually fixing the underlying behavior. Our pipeline writes
small fuzzing programs (**harnesses**) full of **checks** ("this call
must not crash", "the mean must equal n·m/N") and runs them against the
patched program. The checks must be:

- **safe**: they never accuse a genuinely correct patch, and
- **sharp**: they do catch the overfit one.

An overfit patch passes every existing test by construction, so it can
only be caught on inputs no test covers — where "what is correct?" must
be reconstructed from indirect evidence, ranked by trust:

1. the bug's original failing test (definitive, but only for its inputs)
2. the buggy program itself (correct everywhere except at the bug)
3. the documentation comments (the promised contract)
4. universal math/logic rules ("sorting twice = sorting once"), if
   genuinely universal
5. the patch's own code — **least trusted; it may be the overfit**

Every **miss** (an overfit we don't catch) means we failed to check
something the overfit gets wrong. Every **false alarm** (a correct
patch we accuse) means we checked something a correct program is
actually allowed to do differently.

## The pipeline in seven stations (referenced throughout this doc)

1. **Setup** — apply the patch; prove the bug's test fails before it
   and passes after it.
2. **Rule-writing** — a model reads the changed code, its docs and the
   failing test, and proposes general rules a correct program must
   obey.
3. **Rule screening** — each rule is compiled and run ~20,000 times
   against the buggy program to weed out rules that accuse everything.
4. **Harness writing** — a model writes three fuzzing programs full of
   checks: copies of the failing test, the screened rules, and checks
   it invents itself.
5. **Acceptance** — each harness must prove it fires on the buggy
   program; we record which check fired and what crash was underneath.
6. **Judgment day** — the harnesses fuzz the PATCHED program, and every
   screened rule is also compiled on its own and run directly against
   the patched program ("replay"). Any check that fires here is an
   accusation.
7. **The judge** — a model reviews each accusation: would EVERY correct
   program satisfy this check, or could a correct one trip it? Only
   kept accusations count.

---

## GROUND RULES — read before running or changing anything

**Firewall.** The developer's real fix may be used ONLY offline — for
cleaning the dataset, verifying labels, and understanding misses
afterwards. Never in any decision the pipeline makes.

**Substrate.** All experiments run against `pinned_tasks.jsonl`, where
every overfit patch is verified to actually behave differently from the
real fix and every correct label is double-checked. On the pinned set,
every miss is a real technique failure and every false alarm a real
safety failure.

**No cross-run pooling — HARD RULE (2026-07-18).** Never persist
harnesses, oracle checks, relations, corpora or verdicts BETWEEN runs
and reuse them on later legs of the same bug. That farms the benchmark:
dev numbers inflate with repeated attempts while nothing transfers to a
bug seen once — which is what the held-out set and the real world are.
The permitted boundary: sharing WITHIN one run between a bug's several
patches (P3.2 relation pooling; the pool directory is wiped per suite
by `run_suite.sh`) — that mirrors one real deployment holding one bug
report and several candidate patches.

**No dataset overfitting.** Mechanisms may encode general categories
("read-only calls must not mutate state", "program text tolerates
whitespace insertion") — never the shape of a specific benchmark bug.
Anything motivated by staring at a dev bug must still be justified as a
category before it ships.

**Measurement rules.**
1. Change one thing per measurement point. We once turned on several
   untested changes together (p23gate) and could not tell which change
   caused what until a day of forensics.
2. A changed outcome (miss→catch or new false alarm) is believed only
   after one confirming repeat — harness generation is partly random.
3. Results are tied to the environment (JVM: OpenJDK 11.0.31). The
   unwinnable-task list is environment-specific.
4. Held-out hygiene: fixing plumbing on a held-out failure is fine;
   adjusting prompts/checks/thresholds from held-out output silently
   converts a held-out bug into a dev bug. The held-out set is spent
   ONCE, at the very end.
5. Suite mechanics: run legs 4-way parallel (up to 6 for small
   projects; beyond that the model API is the bottleneck). Check free
   disk first (a Chart/Closure-heavy suite once filled the disk and 18
   legs died at checkout). Pooling requires a bug's correct leg to
   FINISH before its overfit sibling STARTS — true for 30-leg
   correct-first suites at PARALLEL=4, false for tiny suites: run
   pairs serial (a 2-leg parallel run silently gave the overfit leg an
   empty pool). After every suite: delete working copies, archive
   results to the Mac under `runs-archive/`, verify the archive, prune
   VM runs.
6. Iterate cheap: day-to-day iteration uses the 2–6 legs relevant to
   the change; between-phase gates use the 30-leg dev set; the
   flagship full sweep is for final confirms only.
7. Stop rule: if two consecutive iterations change no dev outcome,
   stop tuning that area — further tuning fits noise.
8. Launch check: the replay stage needs `--replay_relations_on_patched`
   in the suite's COMMON flags. It is NOT in old cases files — a full30
   was once launched without it and had to be killed and relaunched.

**Two meta-rules distilled from the 2026-07-17/18 cycle.**
- *A mechanism beats an instruction, everywhere.* A recall idea only
  counts if its check reaches the patched build mechanically (replay,
  generated code, within-run pooled rules). Anything delivered as
  prompt advice is multiplied by the harness writer's implementation
  rate and the fuzzer's input luck, and that product is small: Math-2's
  convicting formula was synthesized, screened and pooled for weeks and
  still missed until the replay stage executed it directly.
- *The judge needs computed facts, not exhortations.* Every false-alarm
  class was fixed by computing a fact and putting it in the judge's
  evidence ("this check also fired on buggy", "the real test passes on
  this build", "this exception is not one of your checks") — never by
  asking it to be more careful. And never auto-dismiss on an ambiguous
  signal: both mechanical auto-dismissals we tried (latent firings;
  same-name reconciliation) each killed a true catch and had to be
  narrowed to replay-the-fact-and-let-the-judge-decide.

---

## DONE — shipped and validated, by station

**Current state (2026-07-18): dev set 10 of 16 overfits caught at ZERO
false alarms** (13 correct legs clean; Math-57-c unscored because its
harness generation is fragile). The 2026-07-17 baseline was 9/16 with 1
false alarm — but 3 of those 9 catches were later shown to be luck.
Run history, one line each: p1base 9/16, 13/14 clean → p23gate (Phase
2+3 turned on at once) REGRESSED to 6/14 with 4 false alarms →
minfix_w1 repaired the correct side (6/6 clean; 0/5 catches — found
the latent-auto-dismiss mistake) → minfix_w2/w2b recovered 5 catches
(incl. Lang-50-o for the first time ever) and exposed 3 more
mechanical bugs → math2pair2 proved the deterministic replay catch
(Math-2-o) → full30: precision 1.00, recall 8/16 → recov2 added
Closure-92-o → c62confirm added Closure-62-o = 10/16 at precision
1.00.

**Station 1 — Setup.**
- Patch applier hardening (P0.1): edit blocks applied one at a time,
  sorted; any failure aborts. (Lang-50's reversed-order patch was
  silently half-applied for weeks; Math-2's truncated patch never
  applied at all while scoring counted the do-nothing runs as passes.)
- Safety net (P0.1): the bug's trigger test must FAIL on buggy and
  PASS on patched before anything else runs; otherwise the leg is
  marked `bug_not_reproduced` / `bad_patch`, never silently scored.
- *Quality audit (2026-07-18): quality solid — every failure mode here
  was mechanical and is mechanically guarded. The one criticism: this
  station's BYPRODUCTS are under-used — it produces the bug's real
  failure message and the exact crashing inputs and throws both away
  (H2 and JD1 in TO DO reuse them).*

**Station 2 — Rule-writing.**
- Direction grounding (P2.1): the failing test's source and expected
  values are the top trusted block; the buggy code is labeled BUGGY
  (it was mislabeled "Patched" for the project's whole history, which
  made Lang-7's rules come out exactly backwards); the diff keeps its
  +/- markers.
- Anchoring: first rule constrains the changed method's documented
  contract; the failing-test-neighbourhood methods are a SECONDARY,
  advisory anchor (as a mandate they re-aimed synthesis at internals
  and lost Closure-33's compile-level winner — p23gate).
- Formula-first: if a touched-class numeric getter's javadoc states a
  closed-form formula, the FIRST rule must be that formula (Math-2's
  formula — the strongest rule class — otherwise appears only by
  luck; it skipped 2 of 4 runs before this).
- Numeric tolerances: generous magnitude-scaled tolerance with a
  looser floor for large-integer arithmetic (a 1e-12 tolerance once
  false-fired on the CORRECT Math-2 patch at billion-scale
  parameters).
- JSON double-escape recovery (literal \n → newline): rules used to
  arrive corrupted and die at compile.

**Station 3 — Rule screening.**
- Self-swallow lint + forced-alarm canary (P0.2): a rule whose alarm
  throw sits inside its own catch-everything block is rejected before
  compiling (about half of all Lang-7-era checks silently swallowed
  their own alarms and screened as "well-behaved").
- Direction check (P2.2): every rule is replayed on exactly the
  failing test's input literals, twice (also a determinism test).
  Fires there = direction-confirmed → ranked first, exempt from the
  fire-rate cap.
- INVERTED demotion, not deletion: "silent on the trigger corpus but
  loud on random inputs" cannot distinguish a backwards rule from a
  sound rule whose violation region random bytes never reach — the
  hard drop once deleted Math-2's mean-formula. Such rules become
  replay-only (never prompt-injected).
- Constraint parity (P2.3): rules that need a forbidden custom
  subclass are rejected at screening instead of silently dropped by
  the harness writer later (that silent drop cost Math-2 its convicting
  rule once).
- Negative-modulo lint (also a station-4 gate):
  `Math.abs(consumeInt()) % n` goes negative at Integer.MIN_VALUE —
  this exact harness bug produced Lang-41-o's only firing in one run
  and cost the verdict.
- Survivor cap raised to 8; the harness prompt is sliced separately
  (see station 4), the rest feed the pool and replay.

**Station 4 — Harness writing.**
- Prompt thinning: at most 2 rules injected, own-leg only, never
  pooled ones — injected sibling-leg rule mass crowded out the
  free-form capacity check that convicts Lang-60 (p23gate). Plus an
  explicit "keep inventing your own checks" instruction naming the two
  historically-winning shapes (hidden-state and sibling-agreement
  checks).
- Dynamic oracle IDs accepted: `"[oracle:" + id + "]"` is valid at
  runtime; the static gate that rejected it cost Chart-3-o all three
  harness attempts in one run (zero harnesses, leg unscoreable).
- Every alarm format must carry an ID: the `"metamorphic violation:"`
  format was invisible to every per-check mechanism (latent scan,
  crash pinning, judge notes) and produced a Chart-26-c false alarm.
- Whitespace-normalized text lifts: expected code/text strings are
  compared with whitespace collapsed. Raw-string comparison fired on
  formatting deltas and handed the judge a legitimate dismissal that
  buried the real content difference (all three Closure-92-o firings
  in full30); the normalized version surfaced the actual difference —
  an unrewritten `goog.provide` — and convicted.
- Extreme-magnitude fence: fuzzed numeric parameters are capped to
  moderate ranges unless the contract covers extremes — at
  billion-scale a CORRECT implementation's double arithmetic
  legitimately degrades; three separate Math-2-c false accusations
  came from checks at such magnitudes (a NaN probability at N≈2^31,
  constructor validation on overflowed ranges, twice).
- Setup-faithfulness instruction: replicate the test's environment
  (registered files, locales, modes) exactly or drop that check. (The
  instruction version; the mechanical version is H1–H3 in TO DO.)

**Station 5 — Acceptance.**
- Per-check bookkeeping (P0.4): acceptance records WHICH named check
  fired on the buggy build; checks that never fired are flagged LATENT
  (a never-exercised check once met its first-ever execution on the
  correct patch and false-alarmed — Chart-26).
- Buggy-side crash identity per check (P3.3 data): which exception
  types stood behind each check's firings on buggy.

**Station 6 — Judgment day.**
- Replay (P3.2, `--replay_relations_on_patched`): every screened rule
  — own and pooled — is compiled unchanged against the patched build
  and run two ways: on the failing test's own inputs (deterministic
  tier) and on 20k fuzzed inputs; firings go to the judge like any
  other accusation. The biggest recall mechanism shipped: contributed
  to 5 of 8 catches in full30 and made Math-2-o deterministic (the
  mean-formula fired 7,144/20,000 on the Arja build, two runs in a
  row).
- Within-run pooling with per-suite isolation (P3.2): a bug's screened
  rules are shared between its legs within one run; the pool directory
  is created fresh per suite (a stale cross-run pool would silently
  contaminate every later measurement).
- Phantom-crash fix: libFuzzer `slow-unit-*` artifacts no longer count
  as crashes (a clean exit-0 run was once scored as a false alarm).
- Crash-type pinning (P3.3): a must-not-crash check only counts on the
  patched build if its underlying exception TYPES overlap the ones
  recorded on buggy — a different crash wearing the same alarm is
  dismissed. (Type-level on purpose: a half-fix that moves the same
  exception one frame stays a catch.)

**Station 7 — The judge.**
- Cause-chain rule + differential replay (P0.3, broadened): an alarm
  wrapping a caught crash must carry the original as its cause; an
  escaped exception whose exact input reproduces the same crash on
  buggy is pre-existing surface — dismissed. Broadened to ALL escaped
  non-alarm exceptions on semantic legs (a `NotPositiveException` from
  junk fuzzed constructor input was twice kept as a "conviction" of
  the correct Math-2 patch before this).
- Latent-firing fact: a latent check firing on patched triggers a
  mechanical replay of that exact input on the buggy build; the judge
  is told "fires there too — the patch did not change this behaviour"
  or "quiet there — the patch introduced it". (The first version
  auto-dismissed latents outright and killed the true Lang-60-o
  capacity catch.)
- Symmetric-firing fact: "this check also fired on buggy — keep only
  if the violated contract belongs to the reported bug's own behaviour
  family." Kills the Chart-26-c axis-entity false-alarm class while
  keeping Lang-41-o, whose TRUE catch has the same fires-on-both
  shape — which is exactly why this could not be a mechanical
  dismissal.
- Trigger-test-lift fact (matched in check IDs AND message text): when
  a fired check is a copy of a trigger test, the judge is told the
  REAL test passes on this build — a faithful replay cannot
  legitimately fire. Killed the Closure-62-c / Closure-73-c
  false-alarm class.
- Escaped-exception fact: "this firing carries no check ID — it is not
  one of the harness's checks; junk-input validation is the usual
  cause."
- "Dismissal wins" reconciliation (P4.3), scoped: the same check judged
  unsound on one firing and kept on another → the dismissal wins.
  Scope: across harnesses only for injected-rule names; a generic
  model-invented ID (`lifted-test`) names DIFFERENT checks in
  different harnesses, and the unscoped version transferred an unsound
  verdict onto the sound Closure-62-o catch and killed it (full30).
- Trust hierarchy: "the shown code may be the bug — where a trusted
  failing-test value pins a behaviour, the test outranks the shown
  body's guard logic." The judge had read the buggy `charno < length`
  guard as the contract when that boundary IS Closure-62's bug.
  Confirmed: Closure-62-o flipped to a catch, correct-side guard
  clean.

---

## SCOREBOARD — where every dev leg stands (2026-07-18)

Overfit legs (16):
- **Caught, stable (each confirmed on at least 2 consecutive runs):**
  Chart-7 (5 of 5 runs since Phase 0), Chart-26 (5/5), Closure-73,
  Lang-41 (4 of last 5; the one miss was a harness bug now linted),
  Lang-50 (3/3 since first caught — never caught before this cycle),
  Lang-60 (4/4 since the latent fix), Math-2 (2/2 since replay —
  deterministic via the mean-formula), Time-4 (always).
- **Caught, one confirm still pending (rule #2):** Closure-92 (via
  normalized text lifts), Closure-62 (via the trust hierarchy). Both
  get their repeat for free in the next full pass.
- **Missed, fix owned:** Math-53 → OBS below (the library's own
  equals() treats all-NaN values as equal, hiding the 3-output
  divergence; only field-level reads see it). Math-57 → BND below
  (float-vs-double width, visible only near 1e20; also its harness
  generation died in 3 of 5 runs — javac failures in the repair loop).
  Chart-3 → P4.1 below (missed 4 consecutive runs; its baseline
  "catch" was a lucky loose test-copy — a faithful copy can never
  catch it because the overfit passes the faithful scenario by
  construction). Closure-33 → R3/R4 below (its winning check was
  invented in 3 of 6 runs; pure dice today).
- **Missed, no mechanism yet:** Lang-27 (crash-shaped bug; the overfit
  suppresses the crash everywhere, and the buggy build never returned
  a value there, so there is no trusted answer to compare — candidate
  ideas under CRASH below).
- **Permanent by policy:** Time-11 (cross-thread; one cheap experiment
  listed before final acceptance — T11 below).

Correct legs (14): 13 clean in full30 — ZERO false alarms, the first
full run ever without one (the baseline had 1, and p23gate had 4).
Math-57-c unscored (no harnesses built). Every false-alarm class ever
observed now has a named mechanical guard (see Station 7 DONE); the
residual risk is a brand-new unsound check with no matching fact,
which is what J1 below measures.

---

## TO DO — by station, ordered by impact vs risk

Global order (do top to bottom; each item names its target legs, the
evidence, and the risk; items marked NEW come from the quality audit
above):

1. **H1+H2+H3** — complete the test context, show the real failure
   output, add the fidelity gate (stations 4/5)
2. **R1** — compile-repair for rules (station 2)
3. **JD1** (NEW) — seed the patched-side fuzz with the buggy-side
   firing inputs (station 6; cheap, broad)
4. **J1+J3** — measure the judge offline; show it the failing test
   (station 7)
5. **R2+R4+R-INH** — six rules with an anchor quota; input-kind
   context + closed menu; inherited contracts (station 2; R-INH NEW)
6. **H4+H5+H6** — observables list, sibling map, known-crash list
   (station 4)
7. **ACC1** (NEW) — isolated latent-check scan (station 5)
8. **OBS** — observer code generation [P3.1's delivery] (station 4;
   targets Math-53)
9. **R3** — doc-poor pivot to passing-test extension (station 2;
   targets Closure-33, Closure-92)
10. **BND** — documented-boundary + numeric-literal corpus for
    screening and replay (stations 3/6; targets Math-57)
11. **RETRY** — check-kind checklist with one aimed retry (station 4)
12. **P4.1** — compare-to-buggy (new stage; targets Chart-3) — gated
    on an offline false-alarm measurement first
13. **CRASH** — crash-neighbourhood value rules (stations 2/6; targets
    Lang-27) — uncertain payoff
14. **J2** — trusted-literal short-circuit (station 7) — parked behind
    J1 and H3
15. **T11** — one-hour initialization-order experiment for Time-11
16. **P4.2 / P4.4** — offline certifier split + dataset growth
    (offline; never mixed into a measured run)
17. **Full30 confirm** of the accumulated changes → then **THE
    HELD-OUT RUN** (spent once; the number that counts)

### Stations 4/5 — Harness writing & acceptance

*Quality audit (2026-07-18).* Station 4: a QUALITY problem,
definitively not quantity — rerolls flip flaky legs but every extra
harness is a false-alarm lottery ticket, and zero-FP was measured at
n=3, so raw count is the wrong knob (RETRY is the aimed exception).
The quality split is measured: scenario-rebuild fidelity (fix = the
context items H1/H2 and the gate H3) and check diversity (fix = the
aimed context H4/H5/H6 and OBS codegen). The inherited-contract gap
(R-INH, station 2) applies here too: a hidden-state or sibling check
justified by a parent interface's javadoc is currently unwritable
because the writer never sees that javadoc. Station 5: solid except
one real MEASUREMENT blind spot — the buggy-side scan stops at the
first firing check per input, so a check behind an always-firing seed
check is recorded LATENT, which means "unmeasured", not "quiet". That
one blind spot drove errors in BOTH directions (the true Lang-60-o
capacity catch looked latent and our first auto-dismissal killed it;
the junk Lang-7-c hex check looked latent and the judge kept it). We
compensate downstream with the buggy-replay fact; the honest fix is
upstream — ACC1 below.

**H1 — complete the test's context (the highest-value single fix; do
first).** The problem, found by direct inspection of the prompts: the
harness writer is shown ONLY the failing test's method body.
Closure-62's test method calls `formatter("assert (1;")` — a helper
defined elsewhere in the test class that performs exactly the setup
harnesses keep rebuilding wrong (it wires the source-text provider) —
and uses `FOO_TYPE`, a class constant. Neither appears anywhere in the
prompt; the model must improvise the setup, and every setup-divergence
failure follows from that. Observed cost so far: the two p23gate false
alarms (Closure-62-c, Closure-73-c — lifted checks firing over missing
source wiring / a trailing semicolon), two of the three Closure-62-o
drops in full30/recov2 (judged setup-divergent — correctly!), and
Chart-26's improvised entity wiring across at least 3 runs. The fix is
mechanical: resolve the identifiers the test method uses against its
test class and include what they refer to — setUp()/@Before methods,
helper methods, class constants — plus the content of any fixture
FILE the test references by a path-like string literal. Validate: the
Closure-62 pair (the overfit side finally gets a faithfully-built
scenario; the correct side stays clean) and Chart-26-c as guard. Risk:
low — showing the true setup can only reduce improvisation; watch
prompt size on big test classes.

**H2 — show the real failure output (do together with H1).** Station 1
already runs the failing test on the buggy build; its JUnit message
("expected:<X> but was:<Y>") names the exact observable that diverges
AND the wrong value the bug produces — and today we throw it away.
Put it in the harness prompt. Cost ~zero (the run already happens).
Risk: none identified.

**H3 — mechanical setup-fidelity gate (needs H2).** At acceptance: a
test-copy check firing on the buggy build must observe the SAME wrong
value the real test observed there. A different observed value means
the harness's scenario is NOT the test's scenario — reject with
exactly that message into the existing repair loop. This converts "is
this firing just setup divergence?" — today a judgment call made at
station 7, fallibly — into a station-5 string comparison. Validate:
the archived Closure-62-c false-alarm harness must be auto-rejected.
Risk: low; normalize values that legitimately vary (whitespace — the
same normalization station 4 now mandates).

**H4 — list the touched class's cheap observables.** The raw material
of hidden-state checks — the kind that convicts Lang-60 ("capacity
went from 43 to 6 after a call documented as read-only") — is buried
in a truncated class skeleton today. Mechanically list the public
no-argument getters: "state you can read: capacity(), length(),
size()". Risk: none. Feeds OBS.

**H5 — list the sibling pairs.** Mechanically list same-name overloads
and doc-identical method pairs of the touched class:
"getPackageName(Class) and getPackageName(String) are documented to
agree". Sibling-agreement checks convict Lang-41 — and in 2 of 7 runs
the model did not notice the pair on its own and the leg missed.
Risk: none.

**H6 — tell the writer (and the judge) about known pre-existing
crashes.** Acceptance and the latent scan already OBSERVE the generic
crashes that live in the buggy build — e.g. the text-measuring crash
behind every Chart-26 flag-pattern false alarm (observed in at least 4
runs across the project's history). Collect their identities and state
them in the harness prompt: "these exceptions exist on the buggy build
and are NOT the bug — never convert them into an alarm"; give the
same list to the judge. Kills that class at the source instead of at
judgment. Risk: low — for crashing bugs, exclude the bug's OWN crash
from the list.

**OBS — observer code generation (P3.1's two check-shapes delivered as
code, not advice). Targets Math-53.** Math-53's divergence (3 outputs)
is invisible to `equals()` because commons-math defines all-NaN
complex numbers as equal; only field-level reads (`getReal()` is NaN
vs 4.0) can see it — which is why every run's NaN rules screened
"silent" and the leg missed every time. Rather than asking the model
to please read fields (advice — the same lossy channel that muted
Phase 2), generate the observer block as CODE in the harness template:
call every public no-argument getter before/after each API call,
compare per-field with NaN-bitwise and tolerance semantics. Risk:
medium — an observer with side effects would perturb the scenario
(restrict to getters whose docs read as pure, and say so in the
limitation notes).

**RETRY — check-kind checklist, one aimed retry. Do NOT raise the
blanket harness count.** After the harnesses are written, mechanically
list which check KINDS are present (test-copies / rules /
sibling-agreement / hidden-state — readable from check IDs and
shapes); if an applicable kind is missing, spend ONE extra attempt
asking for exactly the missing kind. The evidence cuts both ways on
"more harnesses": rerolls DO flip flaky legs (minfix_w2's rerolls
recovered Closure-33 and Lang-41 after w1 missed them), but every
extra harness is also a false-alarm lottery ticket on the correct
sibling — each false-alarm class this cycle arrived via ONE harness in
ONE roll — and zero-false-alarms was measured at n=3. Risk:
low-medium (kind detection is heuristic; keep it advisory).

**ACC1 (NEW, from the quality audit) — isolated latent-check scan.**
Fix the station-5 blind spot at its source: after the normal scan,
take each harness's LATENT checks and rerun the buggy-side scan with
the checks that fired DISABLED (mechanically commented out by their
recorded IDs), so the shadowed checks actually execute on the buggy
build. A latent check then becomes either "fires on buggy when
reached" (the Lang-60 capacity case — its later patched-side firing is
the classic overfit signature, symmetric evidence in hand) or "quiet
on buggy even when reached" (the Lang-7 hex case — a first-ever firing
on patched now carries REAL buggy-side evidence against it, not
absence of evidence). Cost: one extra compile plus a short fuzz per
harness that has latents — minutes of VM time, zero model calls.
Risk: low — it only upgrades "unmeasured" to "measured"; the disabled
variant never influences acceptance itself. Validate: rerun the
archived Lang-60-o and Lang-7-c fixtures — the capacity check must
measure as fires-on-buggy, the hex check as quiet-on-buggy.

### Station 2 — Rule-writing

*Quality audit (2026-07-18): quality is CONTRACT-LIMITED, not
model-limited.* Where docs are rich the model writes sound rules
(rules-through-replay convicted 5 of 8 full30 catches); where docs are
sparse it produces zero survivors (4 of 5 Closure rounds). So the
model is not the bottleneck — its INPUT is. Quantity genuinely lacks
on doc-rich legs (R2); context has two real gaps: inherited contracts
(R-INH — the parent interface's javadoc, where Java convention puts
the contract, is invisible today) and the input's kind (R4). And one
principle for ALL context additions: never bulk-add codebase context —
we have measured that indiscriminate prompt mass distracts (the
p23gate crowding; the mined-54 experiment) — additions must be
mechanically SELECTED (parent javadocs, input kind, sibling map),
never "here is more of the repo".

Grounding data (full30, 28 synthesizing legs): 4 candidates per leg,
~1.7 survive screening; 25 of ~112 candidates never compile — and
rules have NO repair round today, a compile error is death; 76 of 78
survivors never fire on random buggy-side inputs (the trigger corpus
and the patched-side replay are where rules work; random fuzzing only
validates them); rules-through-replay contributed to 5 of the 8 full30
catches. Doc-rich legs (Math/Lang/Time) keep 2–5 rules; Closure
synthesis rounds ended with ZERO survivors in 4 of 5 cases.

**R1 — compile-repair round (pure recovery; do first).** On a compile
failure, feed the compiler's error plus the candidate back ONCE for a
corrected version — exactly what the harness repair loop already does.
Recovers up to ~22% of all candidates at one cheap call each.
Validate: compile-death rate halves on a 4-leg micro-suite, survivor
quality unchanged (screening still judges them). Risk: none.

**R2 — six candidates with an ANCHOR-DIVERSITY quota.** Each rule
declares its anchor: (a) the changed method's documented
contract/formula, (b) methods the failing test reads, (c)
sibling-agreement between overloads, (d) domain-level transformation
(see R4). If all candidates share one tag, one retry asks for the
missing kinds; the tag rides into the screen log so we can MEASURE
which anchors actually convict. Why a quota beats raw count: Math-2's
runs produced disjoint 4-rule sets run to run — the contract holds
8–10 distinct rules, but four slots aimed at one anchor never span
them. Validate: doc-rich micro-pair (Math-53): anchor spread rises,
no new false alarm. Risk: low.

**R4 — name the input's KIND up front, and attach its fixed menu of
harmless variations (feeds R2 and R3).** Two parts:
(a) *Input-kind as context, stated at the TOP of rule-writing.* Today
the rule-writer sees code, docs and tests but nothing NAMES what kind
of data the code consumes. It is mechanically readable from the entry
point's signature (a String fed to a compiler = program text;
int/double parameters = numbers; a List = a collection). Compute it
and state it first: "the public entry points consume JavaScript source
text." Knowing the kind changes which rules even make sense to
propose — it belongs at the start of the context, not as an
afterthought.
(b) *The closed menu:* a short FIXED list written once into the
pipeline's standing instructions — not generated per bug, not learned
from this benchmark — one entry per broad input kind, each a
universally-true harmless variation: program/markup text → inserting
whitespace or a comment changes no meaning; parse/print pairs → print
then re-parse returns the input; numbers → only invariances the docs
state (scaling, translation); collections → order must not matter
where the docs say order does not matter; formatters → the output
must parse back to the input. "Closed" means the model may only PICK
from this list when it needs a harmless variation, never invent its
own — a freely-invented "harmless" change that is not actually
harmless is exactly how unsound rules are born. The list sits on the
same trust tier as "sorting twice = sorting once" (source #4 in the
ranking above) and may only ever be extended with entries of that
universality. If a task's input kind is not on the list, R4 simply
contributes nothing for that task — no rule rather than a wrong rule.
Risk: low.

**R-INH (NEW, from the quality audit) — include inherited contracts.**
When a touched method implements or overrides an interface/superclass
method, the contract usually lives on the PARENT's javadoc — classic
Java style documents the interface, not each implementation — and
today only the touched class's own docs are shown. Consequence: some
legs we treat as "doc-poor" may be doc-rich one level up, and rules
that ARE justified ("contains(char) answers membership over the
logical content" may be specified on the parent type) currently cannot
cite their justification. Fix, mechanical: when a touched method has
an @Override or matches a signature in an implemented interface /
extended class, fetch THAT declaration's javadoc and show it beside
the method, labeled "inherited contract". Applies to stations 2 AND 4
(the harness writer has the same blind spot). Risk: low — it is
selected context, not bulk context; keep it to the direct parents.
Validate: count rules citing inherited contracts on a Closure
micro-pair; watch the false-alarm guards.

**R3 — doc-density mode switch + passing-test extension (the doc-poor
answer; medium risk; decisive targets Closure-33 and Closure-92).**
Mechanically measure how much documentation the touched methods have
(javadoc characters per touched method — already extracted, so
measuring is free). BELOW a threshold — the Closure situation, where
4 of 5 synthesis rounds produced zero surviving rules — rule-writing
pivots its primary anchor from "the documented contract" (which barely
exists there) to the project's OWN PASSING TESTS near the changed code
(already mined and shown as usage examples today — wasted as a spec):
each rule takes one passing test's scenario and asserts that the
property the test checks stays true under a harmless variation of the
input, where "harmless" may come ONLY from the R4 menu. This is
exactly the shape of Closure-33's historical winner (append a comment
to testIssue700's program — the compiler's warning count must not
change), which today gets invented in about half the runs by pure
luck.
*Is this general, or benchmark-shaped? The argument, made before
building so held-out can falsify it:* (i) it uses only artifacts every
real task has — a project's own test suite — no bug shapes, nothing
dataset-specific; (ii) the trust logic is structural, not learned: a
passing test holds on the buggy build AND on any correct patch by
definition, so a rule built on it can only convict a patch that breaks
previously-working behaviour beyond the reported bug — a general
failure mode of automated patching; (iii) it is the same trust move
the pipeline already makes with the FAILING test ("tests are
specification"), extended from one test to the suite; (iv) "vary a
tested scenario harmlessly, re-check its property" is standard
metamorphic testing, not something invented for this benchmark. What
it does NOT cover, said plainly: overfits whose damage lies far from
every existing test's neighbourhood — for those, the contract rules
(doc-rich path) remain the only net. Honest caveat: the design is
general but the supporting evidence so far is one benchmark leg; the
held-out run is where the generality claim gets tested, and nothing in
the mechanism may be tuned per-bug on the way there. Validate:
Closure-33-o (its winner becomes derivable-by-recipe instead of a
lucky roll); Closure-92-o second target; Closure-62-c as the
false-alarm guard.

### Stations 3/6 — Screening inputs & judgment day

*Quality audit (2026-07-18).* Station 3: the mechanism is fine; the
INPUTS carry almost no signal — 76 of 78 surviving rules never fire on
the 20,000 random inputs (random bytes essentially never reach a
rule's violation region), so the fire-rate screening ranks by is
mostly "silent vs silent" and all real signal comes from the
trigger-literal replay. And that corpus currently contains ONLY QUOTED
STRINGS from the failing test — Math-2's trigger values are NUMBERS
(N=43130568, m=42976365, n=50) and never enter it, so numeric rules
run their direction check blind (folded into BND below). More random
runs would not fix an input problem. Station 6: replay is the
healthiest component in the pipeline; the harness-side fuzz, however,
wastes evidence it already owns — it starts from an EMPTY corpus
although station 5 captured the exact inputs that fired on the buggy
build (JD1 below).

**JD1 (NEW, from the quality audit) — seed the patched-side fuzz with
the buggy-side firing inputs.** The inputs that actually fired checks
on the buggy build (their crash artifacts are already saved at
acceptance) are exactly the inputs most likely to still fire on an
overfit that special-cased only the reported input — and today the
patched-side fuzz rediscovers them by luck or not at all. Fix: pass
the buggy-side artifact files as the seed corpus of the patched-side
fuzz run. Within-run, firewall-clean, ~zero cost (the files exist; the
fuzzer accepts a corpus directory). Risk: low; one care point — a seed
that fires via a pre-existing generic crash will fire immediately on
patched too, so the existing attribution/differential-replay guards
must stay in the path (they do). Validate: on the archived flaky legs
(Closure-33-o, Lang-41-o), the catch rate across repeats should rise;
correct-side guards stay clean.

**BND — documented-boundary + numeric-literal corpus for screening AND
replay (targets Math-57; fixes the station-3 input gap).** Two parts:
(a) extract NUMERIC literals from the failing test alongside the
quoted strings, so the direction check and the replay's deterministic
tier can steer toward the trigger values (honest note: fuzzer inputs
are bytes, so seeded values steer generation rather than guarantee
exact replay — still far better than blind); (b) mechanically add
inputs at the DOCUMENTED limits — range endpoints from @param, values
around 2^31, 1e20, NaN where the contract admits it — so
width/overflow rules get exercised deterministically instead of hoping
random bytes reach 1e20 (they essentially never do: Math-57 is a
float-vs-double difference visible only near 1e20 and has missed in
every run). Risk: medium — extremes are exactly where CORRECT
implementations degrade (three Math-2-c false accusations lived
there), so every boundary firing carries a "boundary input" note to
the judge and never convicts alone at extreme magnitudes. Interplay
with the station-4 fence: harness fuzzing stays capped; replay rules
whose stated contract covers extremes may use them.

**CRASH — crash-neighbourhood value rules (targets Lang-27; uncertain
payoff — try after everything above).** The problem in plain words:
for this bug the only trusted signal is "this input makes the program
crash". The overfit wraps the code so it never crashes — anywhere.
Nothing fires. And "did it return the RIGHT value instead?" has no
trusted answer, because the buggy program never returned any value
there — it always crashed. Two ideas: (a) when a patch removes a
crash, the method now RETURNS something on the crash-input family —
and the laziest patches return nonsense. Rules do not need the one
right answer to condemn nonsense: a round-trip rule ("format it,
parse it back, you must get the same thing") or a documented-contract
rule works without it. Mechanically seed the screening/replay corpora
with the captured crash inputs and their mutations so the rules get
evaluated exactly where the nonsense lives. (b) Where the javadoc
documents "@throws for malformed input", a patch that silently
returns something on literally-named malformed shapes is convictable.
Honest limit: a patch that removes the crash AND returns
contract-respecting values everywhere is close to a real fix and may
belong in `UNDETECTABLE.md` rather than on this backlog. Risk:
medium-high — contracts are often silent near crash boundaries (that
is why the code crashed there in the first place).

### Station 7 — The judge

*Quality audit (2026-07-18): quality is good WHEN a computed fact
applies — every measured failure followed a missing fact, and every
fact added killed its class. One cheap context omission remains: the
judge is told the failing test's expected LITERALS but never shown the
failing TEST ITSELF (J3 below). Whether more votes help is
deliberately unresolved until J1 measures it — voting was only ever
tested without the facts.*

**J3 (NEW, from the quality audit) — show the judge the failing
test.** In the Closure-62 backwards judgment the judge weighed the
buggy guard code against a bare literal; the test's own source
(assertEquals with the caret string, on an error placed at
end-of-line) would have made the trust hierarchy concrete instead of
abstract. Fix: include the trigger test's source in the judge's
context next to the trusted values. Cost ~zero (the source is already
extracted for stations 2 and 4). Risk: low — it is trust-source #1;
the one care point is prompt length on multi-test bugs (include the
test the fired check lifts, not all of them).

**J1 — measure the judge offline before tuning it (zero pipeline
risk, highest information-per-token; do early).** This cycle's
forensics named, for dozens of archived keep/drop decisions, what the
RIGHT decision was — runs-archive holds them all. Replay those
decisions through the existing `verifier_replay` tool under different
configurations — 1 vote vs 3 diverse lenses, with and without each
computed fact — and MEASURE keep-error and drop-error rates. The old
"majority voting doesn't help" result predates the computed facts and
deserves re-measurement WITH them. Whatever measurably wins becomes
the configuration.

**J2 — trusted-literal short-circuit (parked behind J1 and H3).**
Where an accusation's expected value is literally one the failing test
asserts, bypass the judge — the test outranks it. Tempting, but the
Closure-62-c false alarms were EXACTLY such values fired from a
badly-rebuilt scenario; with H3's fidelity gate in place this becomes
safer. Enable only if J1's measurements show the judge is the weak
link on precisely these.

### New stage / offline / last

**P4.1 — compare the patch to the BUGGY build (targets Chart-3;
impactful, but unmeasured false-alarm risk — measure offline first).**
Chart-3's overfit passes the faithful test scenario by construction
and its generalized checks stay latent; it has missed 4 consecutive
runs and its baseline "catch" was a loose reconstruction that
happened to fire. Its signature is exactly P4.1's: "the edited region
behaves identically to the buggy build everywhere except on the
trigger inputs — your change did nothing, yet the test passes."
Computable with the buggy build only, so firewall-clean. BUT a
correct refactor can also be behaviour-preserving, so before this may
influence any verdict: run it offline over the verified-correct
patches and measure the false-flag rate. Ship only with a measured
rate near zero, and initially as an ESCALATION trigger (spend the
aimed retry on flagged legs) rather than a verdict.

**T11 — one-hour experiment before declaring Time-11 permanent
forever.** The untrustworthy part of thread bugs is TIMING; if this
bug is really about initialization ORDER (which class got set up
first), order can be forced deterministically: run twice in separate
processes with a different forced first-touch order and compare.
Unvetted; may not match the defect's actual shape; explicitly not in
the plan until someone spends the hour.

**P4.2 — probe machinery split (offline certifier).** The model only
constructs interesting objects and call sequences; a fixed piece of
our code enumerates and prints every public observable before/after
each step. Known validation: the five wrongly-cleared patches
(Chart-7, Lang-41, Lang-60, Closure-62, Math-57) must flip to
"difference found" with NO prompt changes. Offline tooling — never
mixed into a measured run.

**P4.4 — certifier-label the 205 unlabeled patch files** (dataset
growth, not pipeline): one file per (bug, tool) first; "difference
found" verdicts trustworthy directly; "no difference" only after the
deep-dive protocol; manual spot-check before anything enters the
pinned set.

**FINAL — the held-out run.** After the above stabilize and one more
full30 confirms the accumulated changes: run the 71 held-out legs
ONCE, flagship model. Targets fixed in advance (no goalpost-moving):
at least 70% of the 28 held-out overfits caught, at most 1 false
alarm on the 43 held-out correct legs. Why the zero-false-alarm work
matters more than it looks: the baseline's 1-in-14 false-alarm rate
would project to ~3 held-out false alarms against a budget of 1; the
current measured rate is 0-in-13.

---

## REJECTED / DEAD ENDS — do not revisit without new evidence

- **Cross-run harness/oracle pooling (REJECTED 2026-07-18 — a hard NO
  by decision, not an evidence question).** Persisting accepted
  harnesses or checks between runs and rerunning them on later legs of
  the same bug farms the benchmark: dev recall inflates monotonically
  with repeated attempts while nothing transfers to a bug seen once.
  The permitted boundary is within-run sharing between a bug's legs
  (P3.2); nothing crosses the run boundary.
- **Mechanical auto-dismissal of latent firings**: a check behind an
  always-firing seed check is latent on buggy precisely because the
  scan stops at the first firing per input — its first real chance to
  run comes when the overfit silences the seed. The auto-dismissal
  killed the true Lang-60-o capacity catch (minfix_w1). Compute the
  buggy-replay fact and let the judge decide.
- **Unscoped "dismissal wins" reconciliation**: transferring an
  unsound verdict across harnesses by check NAME killed the true
  Closure-62-o catch (full30) — a generic name like `lifted-test`
  labels different checks in different harnesses. Cross-harness
  transfer only for injected-rule names.
- **Asking the prompt nicely to "explore beyond the seed input"**:
  tried twice (diag2, diagf), 3 false alarms each time — the
  instruction is ignored in practice.
- **Voting across a bug's several patches** ("if most patches behave
  the same way, trust that behavior"): repair tools tend to make the
  SAME mistake in all their patches for a bug, so agreement proves
  nothing. (Rule pooling shares the checks, never the verdicts.)
- **Coverage-guided differential fuzzing for certification**:
  considered twice; every wrong "no difference" we ever found came
  from looking at the wrong OUTPUT, never from failing to find the
  right INPUT — P4.2 fixes the actual cause.
- **Judge majority voting, as measured WITHOUT computed facts**: error
  rate unmoved at 3× the cost. Superseded by J1: re-measure WITH the
  facts before concluding anything.
- **Raw-string comparison of lifted code/text outputs**: fires on
  formatting deltas, hands the judge a legitimate dismissal, and
  buries the content difference the same comparison would have caught
  (all three Closure-92-o firings in full30). Always
  whitespace-normalize.
- **Blanket increase of harnesses per leg**: every extra harness is a
  false-alarm lottery ticket on the correct sibling; zero false alarms
  was measured at n=3. Extra attempts only when aimed (RETRY).
- **Spending effort on the unwinnable tasks**: Lang-7, Lang-22,
  Math-30, Math-59, Closure-115, Closure-123 (and the mislabeled
  correct sides of Lang-41 / Lang-10) are proven behaviorally
  identical to the real fix in our environment or wrongly labeled —
  there is nothing to catch there.
