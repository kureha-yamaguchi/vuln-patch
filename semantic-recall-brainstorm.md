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

(One "leg" = one candidate patch being judged, start to finish. A bug
usually has two legs in our data: its correct patch and its overfit
patch. Every leg passes through these seven stations.)

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

**No pooling AT ALL — HARD RULE (tightened 2026-07-19).** Never share
harnesses, oracle checks, relations, corpora or verdicts between legs
or between runs — every leg is fully self-contained. The 2026-07-18
version of this rule permitted within-run sharing between a bug's legs
(P3.2 pooling); the user closed that boundary on 2026-07-19: a leg
convicting via a sibling leg's rules is still a verdict the leg did
not earn from the bug alone, and nothing transfers to a deployment
that sees one patch. Pooling was removed from run.py the same day.
The sanctioned compensation for synthesis randomness is MORE OWN
rules per leg (`--synth_max_rules` default 8; every screened survivor
feeds the patched-build replay — the prompt stays capped at 2). Side
effect: with no pooling there is no correct-leg-before-overfit-leg
ordering constraint — suites may run fully parallel at any size.

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
   legs died at checkout). (The old pooling ordering constraint —
   correct leg before overfit sibling — is GONE with pooling itself,
   2026-07-19: suites of any size may run fully parallel.) After
   every suite: delete working copies, archive
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
- *Quality check (2026-07-18): this station works well and needs no
  fixing. One criticism: it LEARNS useful things and then throws them
  away. When it runs the failing test on the buggy program, that run
  produces (a) the test's failure message — which literally says which
  value came out wrong and what it should have been — and (b) the
  exact inputs that trigger the bug. Today neither is passed on to the
  later stations. Two TO DO items reuse them: H2 passes the failure
  message to the harness writer, JD1 passes the trigger inputs to the
  patched-side fuzzing.*

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
  (own-leg; pooling removed 2026-07-19) is compiled unchanged against
  the patched build
  and run two ways: on the failing test's own inputs (deterministic
  tier) and on 20k fuzzed inputs; firings go to the judge like any
  other accusation. The biggest recall mechanism shipped: contributed
  to 5 of 8 catches in full30 and made Math-2-o deterministic (the
  mean-formula fired 7,144/20,000 on the Arja build, two runs in a
  row).
- Within-run pooling (P3.2) — REMOVED 2026-07-19 (see the tightened
  no-pooling ground rule): every leg is now fully self-contained;
  synthesis stochasticity is compensated by more own rules per leg
  (--synth_max_rules 8) and by replaying every screened survivor.
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

How this section is organised: the LIST below is ordered by what to
do first — most expected benefit for least risk — so it deliberately
jumps between stations. The DETAILED descriptions after the list are
in station order (2, 3, 4/5, 6, 7, then things outside the pipeline).
Every item has a short reference code in parentheses (H1, OBS, ACC1
…); the code carries no meaning by itself — it only exists so other
parts of the doc can point at the item. Items marked NEW come from the
2026-07-18 quality check.

Do in this order:

1. [Show the harness writer the WHOLE test](#show-the-harness-writer-the-whole-test-h1) — setup, helpers,
   constants, fixture files (H1); [show it the real failure message](#show-the-real-failure-message-h2)
   (H2); and [reject harnesses that rebuild the test wrong](#reject-harnesses-that-rebuild-the-test-wrong-h3) (H3).
   Stations 4/5.
2. [Let rules fix their own compile errors](#let-rules-fix-their-own-compile-errors-r1) (R1). Station 2.
3. NEW: [reuse the bug-triggering inputs on the patched program](#reuse-the-bug-triggering-inputs-on-the-patched-program-jd1-new)
   (JD1). Station 6. Cheap and broad.
4. [Measure the judge offline on archived decisions](#measure-the-judge-offline-j1) (J1) and
   [show the judge the failing test itself](#show-the-judge-the-failing-test-j3-new) (J3). Station 7.
5. [Six rules with a variety requirement](#six-rules-with-a-variety-requirement-r2) (R2);
   [name the input kind plus the fixed list of harmless variations](#name-the-input-kind-with-a-fixed-list-of-harmless-variations-r4)
   (R4); NEW: [show the inherited interface documentation](#show-the-inherited-interface-documentation-r-inh-new)
   (R-INH). Station 2.
6. Hand the harness writer ready-made raw material:
   [the list of readable state](#list-the-readable-state-h4) (H4),
   [the list of look-alike methods](#list-the-look-alike-methods-h5) (H5),
   [the list of known pre-existing crashes](#list-the-known-pre-existing-crashes-h6) (H6). Station 4.
7. NEW: [a second buggy-side scan with the always-firing checks switched off](#second-scan-for-shadowed-checks-acc1-new)
   (ACC1). Station 5.
8. [Generate the state-observer checks as code](#generate-the-state-observer-checks-as-code-obs) (OBS).
   Station 4; targets Math-53.
9. [Doc-poor mode: build rules from the project's own passing tests](#doc-poor-mode-build-rules-from-passing-tests-r3)
   (R3). Station 2; targets Closure-33 and Closure-92.
10. [Better inputs for screening and replay: the test's numbers plus the documented limits](#better-inputs-the-tests-numbers-and-the-documented-limits-bnd)
    (BND). Stations 3/6; targets Math-57.
11. [One aimed extra harness attempt when a whole kind of check is missing](#one-aimed-extra-attempt-when-a-check-kind-is-missing-retry)
    (RETRY). Station 4.
12. [Compare the patch's behaviour to the buggy program's](#compare-the-patch-to-the-buggy-program-p41)
    ("your change did nothing, yet the test passes") (P4.1). New
    stage; targets Chart-3; only after an offline false-alarm
    measurement.
13. [Rules aimed at the crash's own inputs](#rules-aimed-at-the-crashs-own-inputs-crash), for bugs whose
    only symptom is a crash (CRASH). Stations 2/6; targets Lang-27;
    uncertain payoff.
14. [Let a failing-test value bypass the judge](#let-a-failing-test-value-bypass-the-judge-j2) (J2). Station 7;
    parked until J1 and H3 exist.
15. [The one-hour initialization-order experiment for the thread bug](#the-one-hour-experiment-for-the-thread-bug-t11)
    (T11).
16. Offline tooling: [the certifier probe split](#offline-split-the-certifier-probe-machinery-p42) (P4.2) and
    [labeling the 205 unlabeled patches](#offline-label-the-205-unlabeled-patch-files-p44) (P4.4). Never mixed
    into a measured run.
17. A full 30-leg confirm of everything accumulated → then
    [THE HELD-OUT RUN](#the-final-held-out-run-final) (spent once; the number that counts).

### Station 2 — Rule-writing

*Quality check (2026-07-18). The question asked: are the rules bad,
too few, or starved of information?* The answer: the rule-writer is
only as good as the documentation we feed it — the model itself is not
the weak point. Where the code is well documented (the math and text
libraries), the rules it writes are good enough that running them
directly against the patched program produced 5 of our 8 catches in
the last full run. Where the code is barely documented (the Closure
compiler's internals), the same model produced ZERO usable rules in 4
out of 5 attempts — there was simply no written contract to build
rules from. Given that: yes, we are also short on QUANTITY for
well-documented code (the same class produced completely different
rule sets in different runs, proving more good rules exist than the
four slots we ask for — item R2), and we are short on two pieces of
INFORMATION: the documentation of the interface a method implements
(where Java convention actually puts the promises — item R-INH), and a
plain statement of what KIND of data the code consumes (item R4). One
warning for all information additions: do not dump more of the
codebase into the prompt wholesale. We have measured twice that piling
material into a prompt makes the model perform worse, not better. Every
addition must be a specific, mechanically-chosen piece (the parent
interface's documentation, the input kind, the list of look-alike
methods) — never "here is more of the repository".

Grounding data (full30, 28 synthesizing legs): 4 candidates per leg,
~1.7 survive screening; 25 of ~112 candidates never compile — and
rules have NO repair round today, a compile error is death; 76 of 78
survivors never fire on random buggy-side inputs (the trigger corpus
and the patched-side replay are where rules work; random fuzzing only
validates them); rules-through-replay contributed to 5 of the 8 full30
catches. Doc-rich legs (Math/Lang/Time) keep 2–5 rules; Closure
synthesis rounds ended with ZERO survivors in 4 of 5 cases.

#### Let rules fix their own compile errors (R1)

Pure recovery; do first. On a compile
failure, feed the compiler's error plus the candidate back ONCE for a
corrected version — exactly what the harness repair loop already does.
Recovers up to ~22% of all candidates at one cheap call each.
Validate: compile-death rate halves on a 4-leg micro-suite, survivor
quality unchanged (screening still judges them). Risk: none.

#### Six rules with a variety requirement (R2)

Each rule
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

#### Name the input kind, with a fixed list of harmless variations (R4)

Feeds R2 and R3. Two parts:

(a) *Input-kind as context, stated at the TOP of rule-writing.* Today
the rule-writer sees code, docs and tests but nothing NAMES what kind
of data the code consumes. HOW THE KIND IS DETECTED (honest version —
a signature gives the type, not the meaning): a three-tier hybrid.
Tier 1, purely mechanical: type-shaped kinds read straight off
signatures — numeric parameters = number, Collection/List/array =
collection, a documented format*/parse* method pair = encode/decode
pair. Tier 2, one CONSTRAINED classification call for the ambiguous
cases (a String input could be program text, a query, or a person's
name — no grep settles that): a dedicated model call with its own
small fixed prompt — input: the entry-point signatures, class/package
names, the first javadoc lines, and the failing test's call shape;
required output: a SET of labels from the closed vocabulary (usually
one, possibly several — a date formatter consumes numbers AND is a
format/parse pair; empty set = unknown), plus one justification line
per label for the log. The set stays auditable exactly like a single
label — what matters is the closed vocabulary, not the count. Temperature 0;
cached per BUG (both legs share the same buggy entry points, so the
second leg reuses the label free); cost ~2k tokens against a leg's
~50-100k. The labels are the ONLY thing that flows onward — they select
fixed TEMPLATE sentences plus the matching menu entries, taken as the
UNION over the detected kinds, deduplicated, prioritized by the kinds
of the touched method's own parameters, and CAPPED at three entries
(the measured injected-mass lesson applies to menu text like anything
else), so
the classifier has no channel to smuggle free-form advice; its worst
failure is injecting the wrong FIXED text, which the entry's own
APPLIES-ONLY-IF condition, screening, and the judge each defuse. This is a model
used as a narrow detector (like the judge), not as an advice-follower
— the forbidden thing remains putting the whole menu in the
rule-writing prompt and letting the model pick mid-generation. Tier 3,
the fail-safe: "unknown" injects nothing. Layered defense even against
a wrong label: every entry carries its own APPLIES-ONLY-IF condition
the rule-writer must verify against the docs, and the resulting rule
still has to survive screening and the judge. The detected kind is
then stated first in the context: "the public entry points consume
JavaScript source text."

(b) *The closed menu:* a short FIXED list, one entry per broad input
kind — not generated per bug, not composed per task. WHEN AND HOW IT
IS ADDED: the pipeline's input-kind detection from part (a) picks the
ONE entry matching the detected kind and injects it into the
rule-writing instructions when that prompt is built (same for the
harness writer). The model never sees the whole menu and never chooses
a category itself — that would be advice plus a judgment call, the
exact pattern this design avoids. If the detector matches no entry,
nothing is injected (the fail-safe below). "Closed" means the model
may only USE the injected variation, never invent its own: a
freely-invented "harmless" change that is not actually harmless is
exactly how unsound rules are born.
WHERE THE ENTRIES COME FROM — and an admitted bias to correct: the
five starting entries were written by looking at what our tasks
consume, so their SELECTION is benchmark-flavoured even though each
entry is a universal fact (this does not distort dev-vs-held-out
comparisons — held-out is the same five projects — but it narrows
usefulness beyond this benchmark). The fix before the menu is
finalized: populate it from an INDEPENDENT source — the metamorphic-
testing literature's published catalogs of standard relation patterns
— imported wholesale, so our tasks merely activate a subset rather
than dictate the list.
Every entry has three mandatory fields — the variation, the CONDITION
under which it applies (checkable from the task's own docs/grammar,
never assumed), and its KNOWN EXCEPTIONS. The starting entries, with
their conditions and exceptions spelled out:

- *Program/markup text* — inserting spaces or comments changes no
  meaning, CONDITION: the input language's grammar defines whitespace
  and comments as insignificant (true for Java/JS/C-family; NOT true
  for Python, YAML, Markdown — check, don't assume). KNOWN EXCEPTION,
  demonstrated inside our own dev set: inserting a NEWLINE changes
  line numbers, and outputs that report source positions (error
  formatters — Closure-62's output literally contains "line 6")
  legitimately change with them. So: insert only same-line spaces or
  same-line comments when the asserted property could reference
  positions, or assert only position-independent properties (counts,
  kinds, semantic content of the output).
- *Parse/print pairs* — print then re-parse must preserve the input,
  CONDITION: at the level the documentation promises. Printers
  legitimately normalize; assert semantic equivalence or the
  documented normal form, never byte-for-byte identity unless the
  docs promise that.
- *Numbers* — only invariances the docs state (scaling, translation,
  symmetry); no invented algebra.
- *Collections* — order must not matter, CONDITION: only where the
  docs say order does not matter.
- *Formatters* — the output must parse back to the input, CONDITION: a
  parser for the format exists in the library and the docs claim
  compatibility.

*DONE 2026-07-18: the menu is BUILT — not a five-item sketch but a
full literature mine. Five parallel research passes (numerical, string,
collections, datetime, program-text, web-API, security) produced 62
relation families, consolidated into `src/java/variation_menu.json`
(the operational menu) with the cited provenance in
`suites/menu-candidates.md`. 38 are universal (hold for any correct
implementation), 24 documented-property. Every entry carries a
checkable soundness CONDITION and an EXCEPTIONS list (the false-alarm
suppressors — e.g. Java split's trailing-empty drop, Turkish-i case
folding, DST gap/overlap, EMI's undefined-behavior void, line-number
shifts under inserted newlines). Coverage per detected kind: number 19,
plain_text 15, collection 15, query_or_filter 10, datetime 8, web_api
8, security 6, program_text 5, encode_decode_pair 4. The web-API and
security families are REAL (SMRL's IDOR / injection / session /
workflow / CSRF relations with their guard preconditions), not the
deleted placeholder. The `variation_menu.py` loader injects per detected
kind, universal-before-documented, priority-ranked, capped at 3. Key
source anchors per domain:*
- Segura et al., "A Survey on Metamorphic Testing" (IEEE TSE 2016) —
  the field survey; its corpus of published relations across domains
  is the primary import source.
  https://eprints.whiterose.ac.uk/id/eprint/110335/1/segura16-tse.pdf
- Chen et al., "Metamorphic Testing: A Review of Challenges and
  Opportunities" (ACM Computing Surveys 2018).
  https://dl.acm.org/doi/10.1145/3143561
- Segura et al., "Metamorphic Relation Patterns for Query-Based
  Systems" (MET 2019) — seven abstract relation patterns plus six
  output patterns (equivalence, equality, subset, disjoint, complete,
  difference).
  https://personales.us.es/sergiosegura/files/papers/segura19-met.pdf
- "Metamorphic Relation Generation: State of the Art and Research
  Directions" (ACM TOSEM 2025) — recent overview incl. pattern
  hierarchies that organize prior catalogs into one structure.
  https://arxiv.org/pdf/2406.05397
- Ying et al., "Metamorphic Relation Patterns for Metamorphic Testing,
  Exploration and Robustness" (STVR 2025) — symmetry-based patterns as
  reusable abstractions. https://onlinelibrary.wiley.com/doi/10.1002/stvr.70003
- For the PROGRAM-TEXT entry specifically: Le, Afshari & Su, "Compiler
  Validation via Equivalence Modulo Inputs" (PLDI 2014) — the
  principled generalization of our whitespace/comment idea: variants
  that mutate only code paths a given input never executes must not
  change that input's output (the Orion/Athena/Hermes family found
  140+ GCC/LLVM bugs this way).
  https://www.vuminhle.com/pdf/pldi14-emi.pdf
- For rule SHAPES beyond metamorphic: Hughes, "How to Specify It!" —
  five property families (invariants, postconditions, metamorphic,
  inductive, model-based) with round-trip guidance; measured: the
  metamorphic and model-based families catch the most.
  https://research.chalmers.se/publication/517894/file/517894_Fulltext.pdf
- For the NUMBERS/COLLECTIONS entries: Murphy et al.'s six classes for
  numeric and collection data (additive, multiplicative, permutative,
  invertive, inclusive, exclusive), used and validated in Kanewala &
  Bieman's scientific-software work.
  https://onlinelibrary.wiley.com/doi/10.1002/stvr.1594 and the
  fault-detection effectiveness study
  https://arxiv.org/pdf/1904.07348

(b-SAFETY, added 2026-07-18 per review) *Detection is ADVISORY for
relation selection ONLY — it never constrains fuzzing.* Detecting
`number` does not stop the fuzzer feeding strings; input generation is
untouched by the detected kind. The kind only picks which menu
relations are OFFERED to the rule-writer as candidates, and every
candidate is condition-checked, screened on the buggy build, and
judged. A String that holds a number (createNumber) is still fuzzed
with arbitrary strings. Worst case of a wrong kind: a less-relevant
candidate in one of the 3 slots — never a narrowed input space, never
a changed verdict.

(b-EXACT) *The precise mechanism — what decides which entries reach
which leg, deterministic parts vs the one LLM call. This is the answer
to "how do we choose".*

STEP 1 — detect the input kind(s). DETERMINISTIC, no model, over the
entry-point signatures we already extract (touched methods + the
failing test's called methods). For every parameter and return type:
- numeric type (int/long/double/float/BigInteger/BigDecimal and arrays
  of them) -> emit `number`.
- array, or a type whose simple name is in a fixed list
  {List, Set, Map, Collection, Iterable, ...} -> emit `collection`.
- type whose simple name is in a fixed date/time list {Date, Calendar,
  Instant, LocalDate, LocalDateTime, ZonedDateTime, Duration, Period,
  TimeZone, DateTime, DateTimeZone, ...} -> emit `datetime`.
- two entry methods whose names match format-side
  /(format|to|write|encode|serialize|print)/i and parse-side
  /(parse|from|read|decode|deserialize|valueOf)/i over the same type ->
  emit `encode_decode_pair`.
- a String parameter with none of the above resolving it -> mark
  STRING-AMBIGUOUS and go to step 2. (A String could be program text,
  a query, or a person's name -- the TYPE cannot tell them apart, which
  is the one place determinism genuinely cannot decide.)
Everything except the String case is settled here with zero model
calls.

STEP 2 -- resolve the String-ambiguous case only. ONE LLM call,
temperature 0, cached per (project,bug). Its fixed prompt contains:
the entry-point signatures (names + types), the class and package
name, the first ~500 chars of the class javadoc, and one or two
example calls from the failing test. Its instruction: "these methods
consume String input(s); reply with a JSON array of labels drawn ONLY
from [program_text, plain_text, query_or_filter], or [] if unsure, one
short reason each." The output is parsed and INTERSECTED with that
closed set -- anything else, or [], contributes no string-kind (the
fail-safe). The label(s) are the only thing that leaves this call: a
narrow classifier used like the judge, never an advice channel.

STEP 3 -- select the entries. DETERMINISTIC
(variation_menu.entries_for_kinds): union of menu entries whose
input_kinds intersect the detected kinds, deduplicated by id, ranked
(status `menu` before `menu_optional`, then each entry's `priority`
field so the strongest fit for the kind survives the cap), capped at 3
to respect the measured injected-mass limit. Unknown/empty kind -> no
entry (fail-safe).

STEP 4 -- render and inject. DETERMINISTIC: each selected entry becomes
its statement + its APPLIES-ONLY-IF condition + its DO-NOT-APPLY-TO
exceptions + the one example matching the detected kind (never the
statement alone). The detected kinds also produce a fixed TEMPLATE
sentence stated at the top of the context ("The public entry points
consume numeric values and date-time values") -- assembled from the
labels by a lookup table, NOT written by any model.

STEP 5 -- who checks each entry's CONDITION. The kind-match (steps 1-3)
is deterministic and coarse: it says "a monotonicity rule is POSSIBLE
for a number leg", not "this method has a monotone quantity". The
fine check -- does the entry's condition actually hold here? -- is done
by the RULE-WRITER model, which is handed the condition text and
instructed to verify it against the shown docs and SKIP the entry if
it does not hold. Backstops if the model gets it wrong: screening
drops a rule that fires indiscriminately on the buggy build, and the
judge drops an unsound one. So: coarse relevance = deterministic
(kind); fine relevance = LLM-with-two-mechanical-backstops (condition).
OPTIONAL future refinement (not built): a deterministic keyword
pre-filter on the touched javadoc (e.g. drop `documented-monotonicity`
unless the docs contain cumulative/sorted/non-decreasing/monotone)
before the cap, to spend the 3 slots better -- a pure precision aid,
never a soundness mechanism.

(c) *Safeguards that keep the menu general (the anti-overfitting
contract). When each applies: the first three are an EDIT-TIME
checklist — they gate every addition or change to the menu; the next
two are enforced BY THE PIPELINE at run time; the last is a one-time
checkpoint at the held-out boundary.*
- **Provenance rule (edit-time).** An entry may only be added with a written
  justification citing a universal definition — a language grammar, a
  mathematical identity, a documented API-contract category. "Because
  it would catch bug X" is never a justification; an entry whose only
  known use is one benchmark bug family does not belong.
- **Independent-derivability test (edit-time).** Every entry must be one that a
  competent test engineer with NO access to our benchmark would put on
  the same list (all the starting entries are standard metamorphic-
  testing practice). If an entry needs our bugs to be explained, it is
  benchmark-shaped — reject it.
- **Checkable condition (run-time, pipeline-enforced).** An entry applies only when its stated
  condition is verifiable from the task's own artifacts (the language,
  the docs, the signature) at the time of use. No condition, no use.
- **Size discipline (edit-time), stated precisely.** What must stay
  small is the number of distinct PATTERNS and, above all, the rule
  that growth NEVER comes from our misses — a menu that grows an entry
  per newly-missed bug is the overfitting smell in its purest form.
  What MAY grow, from the literature only: the input KINDS each
  pattern covers and worked examples across contexts (the 2026-07-18
  expansion added idempotence and identity-element — both standard
  QuickCheck-tradition properties, one of them literally this doc's
  own trust-source-#4 example — plus datetime/plain-text kinds as new
  coverage of EXISTING patterns). Prompt mass is controlled at
  injection (at most 3 entries for the detected kinds), not by
  starving the menu.
- **Freeze before held-out (one-time checkpoint).** The menu is frozen before the held-out
  run and may not be edited based on anything seen there — same
  hygiene as prompts and thresholds (measurement rule 4).
- **Fail-safe (run-time, pipeline-enforced).** If a task's input kind is not on the list, R4
  contributes nothing for that task — no rule rather than a wrong
  rule.

The list sits on the same trust tier as "sorting twice = sorting once"
(source #4 in the ranking above). Risk after these safeguards: low.

#### Show the inherited interface documentation (R-INH, NEW)

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

#### Doc-poor mode: build rules from passing tests (R3)

Medium risk; decisive targets Closure-33 and Closure-92. Mechanically measure how much documentation the touched methods have
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

### Station 3 — Rule screening

*Quality check (2026-07-18). The question asked: is 20,000 random
tries the right amount, and are they the right tries?* The amount is
fine; the TRIES are the weak point. Of the 78 rules that survived
screening in the last full run, 76 never fired even once during their
20,000 random tries — random inputs almost never wander into the
narrow situations where a rule would object. So the number screening
ranks rules by ("how often did it fire?") is usually zero-vs-zero and
says little. The measurements that DO carry information are the runs
on the failing test's own inputs. And there we found a hole: we
collect those inputs by pulling the QUOTED TEXT out of the failing
test — but for the Math-2 bug the important inputs are NUMBERS
(a population of 43,130,568 and so on), and numbers are currently not
collected at all, so rules about numeric behaviour get tested without
the one set of inputs known to matter. Fix folded into item BND. More
random tries would not help any of this — wrong-tries is not fixed by
more-tries.

#### Better inputs: the test's numbers and the documented limits (BND)

Targets Math-57; fixes the station-3 input gap. Two parts:
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

#### Rules aimed at the crash's own inputs (CRASH)

Targets Lang-27; uncertain payoff — try after everything above. The problem in plain words:
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

### Stations 4/5 — Harness writing & acceptance

*Quality check (2026-07-18). The question asked: is the problem that
we write too FEW harnesses, or that the harnesses are not GOOD enough
— and if quality, are we giving the writer the right information?*

For station 4 the answer is clear: quality, not quantity. We measured
both directions. Writing the same harnesses again in a new run does
sometimes catch a bug the previous run missed (that is how Closure-33
and Lang-41 came back) — but every additional harness is also one more
chance for a wrong check to accuse a correct patch, and our
zero-false-alarm result was measured with exactly three harnesses per
leg. So simply writing more harnesses buys a little and risks a lot.
The quality problems are two, both measured: (a) the harness rebuilds
the failing test's situation slightly wrong, and that DIFFERENCE — not
the patch — makes checks go off (fixed by giving the writer the
missing information, items H1/H2, plus an automatic comparison, item
H3); (b) which checks the writer invents varies run to run (helped by
handing it ready-made raw material, items H4/H5/H6, and by generating
the routine checks as code, item OBS). One more missing piece of
information, shared with station 2: in Java the promised behaviour of
a method is often written on the INTERFACE the class implements, not
on the class itself — and the writer is never shown the interface's
documentation (item R-INH).

For station 5 the answer is: it works, with one real measurement gap.
When we test which checks fire on the buggy program, the test run
stops at the FIRST check that fires for each input. So a check that
sits behind an always-firing check never gets a turn, and we record it
as "never fired on buggy" — which really means "never got to run", not
"stays quiet". This one gap caused mistakes in both directions: a
GOOD check looked unexercised and an earlier version of our code threw
its accusation away (that cost us the Lang-60 catch once), and a BAD
check looked unexercised and the judge believed its first-ever firing
(that caused the Lang-7 false alarm once). Item ACC1 closes the gap by
re-running the test with the always-firing checks switched off, so the
shadowed checks actually get their turn on the buggy program.

#### Show the harness writer the whole test (H1)

The highest-value single fix; do first. The problem, found by direct inspection of the prompts: the
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

#### Show the real failure message (H2)

Do together with H1. Station 1
already runs the failing test on the buggy build; its JUnit message
("expected:<X> but was:<Y>") names the exact observable that diverges
AND the wrong value the bug produces — and today we throw it away.
Put it in the harness prompt. Cost ~zero (the run already happens).
Risk: none identified.

#### Reject harnesses that rebuild the test wrong (H3)

Needs H2. At acceptance: a
test-copy check firing on the buggy build must observe the SAME wrong
value the real test observed there. A different observed value means
the harness's scenario is NOT the test's scenario — reject with
exactly that message into the existing repair loop. This converts "is
this firing just setup divergence?" — today a judgment call made at
station 7, fallibly — into a station-5 string comparison. Validate:
the archived Closure-62-c false-alarm harness must be auto-rejected.
Risk: low; normalize values that legitimately vary (whitespace — the
same normalization station 4 now mandates).

#### List the readable state (H4)

The raw material
of hidden-state checks — the kind that convicts Lang-60 ("capacity
went from 43 to 6 after a call documented as read-only") — is buried
in a truncated class skeleton today. Mechanically list the public
no-argument getters: "state you can read: capacity(), length(),
size()". Risk: none. Feeds OBS.

#### List the look-alike methods (H5)

Mechanically list same-name overloads
and doc-identical method pairs of the touched class:
"getPackageName(Class) and getPackageName(String) are documented to
agree". Sibling-agreement checks convict Lang-41 — and in 2 of 7 runs
the model did not notice the pair on its own and the leg missed.
Risk: none.

#### List the known pre-existing crashes (H6)

Acceptance and the latent scan already OBSERVE the generic
crashes that live in the buggy build — e.g. the text-measuring crash
behind every Chart-26 flag-pattern false alarm (observed in at least 4
runs across the project's history). Collect their identities and state
them in the harness prompt: "these exceptions exist on the buggy build
and are NOT the bug — never convert them into an alarm"; give the
same list to the judge. Kills that class at the source instead of at
judgment. Risk: low — for crashing bugs, exclude the bug's OWN crash
from the list.

#### Generate the state-observer checks as code (OBS)

The P3.1 check-shapes delivered as code, not advice; targets Math-53. Math-53's divergence (3 outputs)
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

#### One aimed extra attempt when a check kind is missing (RETRY)

Do NOT raise the blanket harness count. After the harnesses are written, mechanically
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

#### Second scan for shadowed checks (ACC1, NEW)

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

### Station 6 — Judgment day

*Quality check (2026-07-18), continued.* Station 6 (running everything against the patched
program): the newest part — running rules directly — is the healthiest
piece of the whole pipeline. The older part — running the harnesses —
wastes evidence we already hold: fuzzing on the patched program starts
from scratch, even though station 5 saved the exact inputs that made
checks fire on the buggy program, and those are precisely the inputs
most worth trying first on the patched one (item JD1).

#### Reuse the bug-triggering inputs on the patched program (JD1, NEW)

The inputs that actually fired checks
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

### Station 7 — The judge

*Quality check (2026-07-18). The question asked: how often does the
judge get it wrong, and does it have what it needs?* The pattern from
this whole cycle: whenever we HAND the judge a hard, machine-computed
fact about the accusation ("this same check also fired on the buggy
program", "the real test passes on this patched program", "this
exception is not one of the harness's own checks"), it decides
correctly almost every time — and every class of wrong decision we
found was fixed by adding exactly one such fact. When no fact applies,
the verdict rests on one model opinion, and that is where the
remaining wrong decisions live. Two things follow. First, a cheap
information gap: the judge is told the VALUES the failing test expects
but is never shown the failing test itself — seeing the actual test
line would have prevented at least one wrong dismissal (item J3).
Second, we honestly do not know whether asking the judge several times
and taking a majority would help, because that was only ever measured
BEFORE the facts existed — item J1 measures it properly, offline, on
archived decisions where we know the right answer.*

#### Show the judge the failing test (J3, NEW)

In the Closure-62 backwards judgment the judge weighed the
buggy guard code against a bare literal; the test's own source
(assertEquals with the caret string, on an error placed at
end-of-line) would have made the trust hierarchy concrete instead of
abstract. Fix: include the trigger test's source in the judge's
context next to the trusted values. Cost ~zero (the source is already
extracted for stations 2 and 4). Risk: low — it is trust-source #1;
the one care point is prompt length on multi-test bugs (include the
test the fired check lifts, not all of them).

#### Measure the judge offline (J1)

Zero pipeline risk, highest information per effort; do early. This cycle's
forensics named, for dozens of archived keep/drop decisions, what the
RIGHT decision was — runs-archive holds them all. Replay those
decisions through the existing `verifier_replay` tool under different
configurations — 1 vote vs 3 diverse lenses, with and without each
computed fact — and MEASURE keep-error and drop-error rates. The old
"majority voting doesn't help" result predates the computed facts and
deserves re-measurement WITH them. Whatever measurably wins becomes
the configuration.

#### Let a failing-test value bypass the judge (J2)

Parked behind J1 and H3. Where an accusation's expected value is literally one the failing test
asserts, bypass the judge — the test outranks it. Tempting, but the
Closure-62-c false alarms were EXACTLY such values fired from a
badly-rebuilt scenario; with H3's fidelity gate in place this becomes
safer. Enable only if J1's measurements show the judge is the weak
link on precisely these.

### New stage / offline / last

#### Compare the patch to the buggy program (P4.1)

Targets Chart-3; impactful but with unmeasured false-alarm risk — measure offline first. Chart-3's overfit passes the faithful test scenario by construction
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

#### The one-hour experiment for the thread bug (T11)

The untrustworthy part of thread bugs is TIMING; if this
bug is really about initialization ORDER (which class got set up
first), order can be forced deterministically: run twice in separate
processes with a different forced first-touch order and compare.
Unvetted; may not match the defect's actual shape; explicitly not in
the plan until someone spends the hour.

#### Offline: split the certifier probe machinery (P4.2)

The model only
constructs interesting objects and call sequences; a fixed piece of
our code enumerates and prints every public observable before/after
each step. Known validation: the five wrongly-cleared patches
(Chart-7, Lang-41, Lang-60, Closure-62, Math-57) must flip to
"difference found" with NO prompt changes. Offline tooling — never
mixed into a measured run.

#### Offline: label the 205 unlabeled patch files (P4.4)

Dataset growth, not the pipeline: one file per (bug, tool) first; "difference
found" verdicts trustworthy directly; "no difference" only after the
deep-dive protocol; manual spot-check before anything enters the
pinned set.

#### The final held-out run (FINAL)

After the above stabilize and one more
full30 confirms the accumulated changes: run the 71 held-out legs
ONCE, flagship model. Targets fixed in advance (no goalpost-moving):
at least 70% of the 28 held-out overfits caught, at most 1 false
alarm on the 43 held-out correct legs. Why the zero-false-alarm work
matters more than it looks: the baseline's 1-in-14 false-alarm rate
would project to ~3 held-out false alarms against a budget of 1; the
current measured rate is 0-in-13.

---

## DIRECTION-CHANGING FINDING (2026-07-18): the menu covers only ~18% of
## what the model freely invents — free exploration is the stronger engine

Two experiments settled how much rule-writing should SUGGEST relations
from the R4 menu vs let the model INVENT them from the code
(study/rank_eval.py, study/coverage_eval.py; 25 diverse methods).

**Ranking test.** Deterministic keyword ranking of menu entries agreed
with a nano ranking only 1.26/3 and fell back to bad static defaults
(trig injected for KMeans / Base64 / MessageDigest) in ~7/19 cases.
Fixes applied (demote narrow number relations out of the default; add
Complex/matrix types; add a nano content-aware selector with keyword
fallback). But the next test undercut the premise.

**Coverage test — the decisive one.** For 11 methods the flagship model
FREELY invented metamorphic relations from the code alone (signature +
javadoc + class), no menu shown; each was then mapped to a menu family
or marked NOVEL. Result: **88 relations invented, 16 covered by the
84-entry menu (18%), 72 NOVEL (82%)** — and the novel ones are BETTER:
specific to the exact method's contract where the menu is generic.
- Math-2: the model invented complement-symmetry (swapping
  successes/failures complements the mean), all-successes, full-draw,
  sample-linearity, population-scaling — the menu had only generic
  "distribution-invariants". Complement-symmetry is the very shape that
  convicts the -49.76 mean.
- Lang-7: invented hex-case-invariance (literally the meta-hex-case
  relation from the real runs), plus-sign, leading-zeros. Menu 0/8.
- Closure-62: caret-column-correctness, no-excerpt-without-source-line
  — the actual Closure-62 bug relations. Menu 0/8.
- Lang-60: capacity-irrelevance (the read-only/capacity convictor),
  empty-builder-false, indexOf-equivalence. Menu 2/8.

**What it means (and it reconciles p23gate).** Free invention given the
code is the stronger engine by ~5x and yields the more discriminating
relations. That is exactly WHY injecting menu/pool rule-mass regressed
p23gate — it displaced the model's own better free-form checks. So:
- Relation synthesis stays PRIMARY and free; the model explores the
  contract itself — that is where convicting relations come from.
- The menu is NOT a relation source to inject wholesale. Two defensible
  roles remain: (a) a small CATEGORY-CHECKLIST backstop — only when the
  free output omits an applicable CATEGORY the model measurably forgets
  (hidden-state/read-only, sibling-agreement), spend ONE targeted nudge
  (the RETRY item), never a bulk list; (b) DOMAIN REFERENCE for kinds the
  model may not know well (security SMRL, reflection JLS traps, geometry
  degenerate-shape traps) — at most 1-2, only for the matching kind.
- R4 is therefore DEMOTED from "inject relevant relations" to "category
  checklist + domain reference". Keep the artifact (cheap; the soundness
  conditions/exceptions are genuinely useful reference), but do NOT build
  the pipeline around injecting it. Re-scope R4 to: (1) mechanically
  detect whether free synthesis already emitted each applicable category
  (from oracle shapes); (2) one targeted retry for a missing category;
  (3) optionally show 1 domain relation for a security/reflection/
  geometry leg. The input-kind detector + menu stay as the mechanism for
  (3) only.
- Caveat: the mapper was strict (marked capacity-irrelevance NOVEL though
  it matches read-only), so true coverage is perhaps ~25%. Still low; the
  conclusion stands. And the bigger reassurance for the whole project:
  the existing FREE synthesis is doing the real work (rules-through-replay
  convicted 5/8 in full30), consistent with this finding.

## CORRECTION (2026-07-19): the "relations aren't a detection
## contributor" finding is RETRACTED — it was measured with replay off

The 2026-07-19 commit concluded the rule pipeline contributes nothing
to detection and demoted it to opt-in. That conclusion does not
survive a check against our own archives. Three faults, in order of
severity:

1. **Both ablation arms ran with the replay stage switched off.** Every
   cases file written since the evening of 2026-07-18 (`onefull`,
   `math53_full`, `abl_norel`, `abl_withrel`) was missing
   `--replay_relations_on_patched` — the exact launch-check failure
   measurement rule 8 warns about. So the ablation compared "rules
   injected into the harness prompt" against "no rules" with the one
   mechanism that makes rules matter disabled in BOTH arms. It ablated
   the channel we already knew was weak (p23gate: injection displaces
   the model's own checks) and attributed the null result to the whole
   pipeline.
2. **The 5-bug sample was blind to the question.** Chart-3, Lang-41,
   Lang-60, Math-53, Math-57 — of these, only Lang-41 ever had a
   relation contribute a catch (trigger-tier, redundant with the lifted
   test). All five legs where replay convictions were kept in full30
   (Chart-7, Chart-26, Lang-41, Math-2, Time-4) were absent except
   Lang-41.
3. **The "full30 retrospective: 0/8 caught by a relation" claim is
   contradicted by full30's own records.** `relation_replay_kept` is
   non-empty on 5 of the 8 caught overfits, and Math-2-o's run.log
   verdict line reads "2 verifier-kept relation conviction(s) — patch
   flagged as overfitting". Math-2 is caught ONLY this way: the
   relation fires on the fuzzed tier and is silent on the trigger
   literals, because the overfit passes the trigger scenario by
   construction — no lifted-test check can ever see it. Weeks of
   pre-replay history (math2pair etc.) confirm the harness alone
   missed it every time.

**The corrected statement:** rule INJECTION into harness prompts is
not a detection contributor (the ablation is valid evidence for that,
and it agrees with p23gate). Rule REPLAY is a detection contributor —
load-bearing for Math-2 and margin on four more. Rules stay ON
(with `--replay_relations_on_patched`) in every full-pipeline suite;
injection stays minimal as already shipped.

**Collateral fix 1 — the soundness-harden pass was destroying
convictors.** The `onefull` trace shows Math-53's field-level NaN rule
(the exact check the OBS item exists to build — it fired on 54% of
buggy-side inputs, i.e. it detects the bug) being probed ON THE BUGGY
BUILD, its bug-caused firings read as "fired on ordinary inputs =
strong evidence of unsoundness", and the model rewriting it into
`!z.isNaN()` — a form that is blind to this bug, because the buggy
result keeps NaN in one part. The rewrite was accepted because the
"repair still catches the bug" guard defaults to TRUE when the failing
test has no string literals (Math-53's is numeric-only). Fixed
mechanically in relation_screen: (a) a rule that fired on the buggy
build during screening is never hardened — probe firings on that build
are bug evidence, not unsoundness evidence; (b) a repair that cannot
be verified to still catch the bug (no trigger corpus) is discarded
and the original kept. Same lesson as ever: never auto-weaken on an
ambiguous signal; the replay verifier judges soundness downstream.

**Collateral fix 2 —** `--replay_relations_on_patched` added to every
full-pipeline cases file that synthesizes relations (14 files;
`abl_norel` deliberately left relation-free; `--rulegen_only` suites
replay unconditionally and need no flag).

**Process note:** the ablation runs were never archived to
`runs-archive/`, so the retracted conclusion cannot even be re-audited
— archive every measured run, including (especially) the ones that
justify a direction change.

**Validation result (struggle10, 2026-07-19, 10 legs):** TP=1 FN=4
FP=0 TN=5 — precision 1.00 held, and **Lang-27-o was caught for the
first time in project history** (harness-invented metamorphic
type-contract checks: `"0e0D"` must parse as Double, the overfit
returns BigDecimal; judge kept it with correct contract reasoning).
The repair is mechanically confirmed in every trace: replay runs on
every leg (quiet on all five correct legs — no precision cost), and R1
compile-repair produced 2 surviving Closure rules where history had
zero. Full leg-by-leg reading in
`runs-archive/runs/struggle10_20260719_073304/ANALYSIS.md`. Two
findings that CHANGE the plan:
- **Math-53-o is NOT an OBS bug.** The DeepRepair overfit's NaN
  handling equals the real fix; its only certified divergence (3×
  exception-class) is returning `Complex.NaN` from `add(null)` where
  the javadoc says throw NullArgumentException. No NaN rule of any
  shape can catch it. The mechanism is a documented-@throws rule —
  item R-THROWS below.
- **Lang-27's catch was structural luck, not new invention.** full30's
  harnesses had the same suffix-type checks but let the seed's
  NumberFormatException escape uncaught, so the patched-side fuzz died
  on the first (dismissible) crash before any metamorphic check ran.
  This roll fenced it. H6 makes the fencing deterministic; the
  confirming repeat rides in hfix11.

#### Documented-@throws rules (R-THROWS, NEW 2026-07-19)

Station 2. When a touched method's javadoc declares `@throws X` for a
named input class (null argument, malformed input, out-of-range), one
synthesized rule must construct exactly that input class and assert
the documented throw ("calling add(null) must throw
NullArgumentException; completing normally violates the contract").
Same trust tier as formula-first (the documented contract, source #3).
Shape properties: silent on buggy whenever the buggy build honours the
@throws (a tripwire — the screening change that keeps ALL silent
tripwires is a prerequisite, shipped 2026-07-19), fires on an overfit
whose reordered/added guard swallows the throw. Targets Math-53-o
(direct trace evidence) and generalizes Lang-27's idea (b) from CRASH.
Validate on the Math-53 pair: overfit convicts via replay, correct leg
stays quiet (the correct fix checks null FIRST, so the rule never
fires there). Risk: low — the input class is stated by the docs, not
guessed; the judge still reviews every firing.

## REJECTED / DEAD ENDS — do not revisit without new evidence

- **Pooling of harnesses/oracles/relations, in ANY form (REJECTED —
  cross-run 2026-07-18, within-run 2026-07-19; a hard NO by decision,
  not an evidence question).** Persisting or sharing accepted
  instruments between runs farms the benchmark; sharing them between a
  bug's legs within one run still hands a leg a verdict it did not
  derive from the bug alone. Every leg is fully self-contained; the
  compensation for synthesis randomness is more own rules per leg.
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
