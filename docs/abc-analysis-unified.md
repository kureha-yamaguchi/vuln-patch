# The 3-Arm Experiment (2026-07-21) — Unified Analysis, Plain-English Edition

> **What this document is.** Two independent deep-dives of the same experiment were written:
> `abc-flag-experiment-analysis.md` ("**analysis O**", Opus, 2026-07-22) and
> `abc-analysis-fable.md` ("**analysis F**", Fable, 2026-07-23) — both deleted 2026-07-26 as
> fully superseded by this merge (recoverable from git history). This document merges them,
> settles every point where they *disagree* by going back to the raw run logs and source code
> (re-checked 2026-07-23, not decided by which write-up sounded more confident), adds a
> step-by-step audit of whether each stage of the pipeline earns its keep, and says it all in
> plain language. Where a term is unavoidable, it's defined where it first appears.
> The fix names G1–G5 match the other documents and the project memory.

---

## 1. What the system does, step by step — and whether each step is necessary

For each bug we have a **broken version** of a Java program, the **failing test** that exposes
the bug, and a **candidate repair**. The question: is the repair real, or a **fake fix** — one
that makes the failing test pass while leaving the bug (or new damage) behind? We must answer
*without* peeking at the official developer fix, because that's the ground truth we're trying
to predict.

The pipeline runs these stages in order. For each one: what it does, what this experiment
showed about it, and a verdict — **keep**, **strengthen**, or **drop**.

**Step 1 — Gather the bug's context** (`bug_context/`): pull out the failing test, the crash
input if any, the patch, the surrounding source code, and which functions call which.
*Everything downstream anchors on this; the strongest catches in this run all trace back to
facts lifted here (the failing test's own expectations, documented error rules).* → **Keep.**

**Step 2 — Invent candidate checks** (`relations/relation_synth`): an AI proposes rules the
correct program should obey — "this solver must throw an error if the interval doesn't bracket
a root", "these two ways of asking the same question must agree". *This is the engine of every
real catch we got. It also invented every false accusation. The problem is never that it
invents; it's that bad inventions survive the next step.* → **Keep** — it's the recall engine;
quality control belongs downstream.

**Step 3 — Screen the checks** (`relations/relation_screen`): compile each proposed check and
run it against the broken version. A check that fires there gets labeled "direction-confirmed"
— treated as evidence it detects the bug. A companion tool (`rule_compile_repair`) rescues
checks that almost compile (~22% of candidates — cheap, real win, keep it).
*This stage is necessary in principle but is currently a rubber stamp. In this run it kept: a
check that fired on **100%** of inputs on the broken version (it was detecting its own setup,
not the bug — Chart-19); checks that fired on **0** inputs (kept as "tripwires" — Lang-63); and
a check comparing a function against another function that just calls the first one, so the two
sides literally run the same code and can never disagree (Lang-63 again). It also let
"confirmed at size 1,500" stand in for "confirmed at size 46,000" (Math-30).* → **Strengthen —
this is where fix G3 goes.** The numbers needed to catch all of the above are already computed
and printed; they're just never used.

**Step 4 — Build fuzzing harnesses** (`harness/`, `execution/`): an AI writes a small test
driver that feeds thousands of random-ish inputs ("fuzzing") into the code with the checks
embedded, and a harness is **accepted** only if it compiles and its checks fire on the broken
version (proof it can see *this* bug — the "tripwire" requirement).
*Two problems surfaced. First, harnesses are a second, separate door for checks to reach the
judge: a check embedded in a harness gets judged even if it never passed Step 3's screening
(that's how Math-39's stretched-too-far check got through in arm B). Second, the acceptance
rule itself creates a trap: since a harness must fire on the broken version to be accepted, any
harness code placed **after** the checks can never execute on the broken version — a check
always fires first. So a bug in that late code shows up **only on correct programs**, which is
exactly what convicted the innocent Closure-70 patch.* → **Keep the harness as the execution
vehicle** (we need fuzzing to explore inputs), **but strengthen: one door, not two** — every
check must pass the same screening facts whether it lives in a relation or a harness, and
replays must be able to reach code past the checks (fix G1).

**Step 5 — Run everything on the repaired version**: the accepted harnesses fuzz the patched
build; surviving checks are also **replayed** there deterministically (same exact inputs,
repeated — this was added in July and made the best catches reproducible instead of lucky).
*A firing here is the accusation: "the repaired program broke a rule."* → **Keep — this is the
detection event itself**, and the deterministic replay was load-bearing for the robust catches.

**Step 6 — Assemble the evidence file**: for each accusation, mechanically computed facts are
attached — does the same thing happen on the broken version? does the real failing test pass on
the repaired build? what did the code's documentation promise? *This is the right idea (the
project's core lesson: judges decide well only when handed computed facts) — and it is where
most of this run's damage happened, because the facts arrived missing, wrong, or ignorable.
Details in §3.* → **Keep, and fix the leaks — G1, G2, G5 all live here.**

**Step 7 — The judge** (`relations/relation_verifier`): an AI reads the accusation plus the
evidence file and rules the alarm REAL (repair is fake) or FALSE (a correct program could
legitimately do this — ignore it). *Necessary — something must separate real alarms from false
ones, and with no developer fix available there's no mechanical substitute for this judgment.
But it's the weakest link, and the fix is never "ask it to judge harder"; it's to constrain it
with facts it cannot talk its way around.* → **Keep, constrained by G5** (a computed fact may
only be overruled by a stronger computed fact, never by a plausible story).

**Step 8 — The second judge** (`--attribution_judge` flag): an extra vetoing judge asking "is
this firing really attributable to the patch?" *In this run it examined 6 cases, changed zero
verdicts, and approved every single wrongful conviction it saw. Its main question — "did the
broken version also misbehave / is this documented?" — is simply not the question that
separates real alarms from false ones here. It also "fails open": anything short of an explicit
veto keeps the conviction, so on real catches it can only ever subtract.* → **Drop. Retire the
flag.** (Both analyses agree, on structural grounds — no amount of re-tuning fixes a judge
asking the wrong question.)

**Step 9 — Final verdict**: if any accusation survives the judge, the patch is declared a fake
fix; otherwise it passes. *One surviving witness convicts. That makes the system exactly as
strong as its single weakest surviving check — one unsound survivor equals one wrongful
conviction (that was all three of this run's false accusations), and one lucky fuzz hit equals
one fragile catch (Math-74 survived on a check that fired on 1.4% of runs).* → **Keep the rule
but label the confidence**: report whether the conviction rests on a deterministic,
contract-anchored check (trustworthy) or a knife-edge/lucky one (fragile) — the numbers to tell
these apart already exist in the logs.

**Verdict on the architecture overall:** no stage is useless — the one genuinely unnecessary
component is the second judge (Step 8). The structure (invent → screen → execute → judge with
facts) is sound. What failed on fresh bugs was not the design but three specific leaks in how
facts flow: they were sometimes never computed (masked), sometimes computed backwards
(shadowed), and sometimes computed right but overruled by a story.

---

## 2. What the experiment was and what happened

15 bugs the system had **never been tuned on** (a fair exam, not memorized homework), 18 tasks
total: 8 fake fixes it should catch, 10 correct fixes it should pass. Three configurations
("arms") differing only in two experimental flags:

| Arm | Caught fakes | Missed fakes | Wrongly convicted | Correctly passed | Precision | Recall |
|---|---|---|---|---|---|---|
| A — both flags off | 5 | 3 | 3 | 7 | 0.62 | 0.62 |
| B — +focused_synthesis | 5 | 3 | 5 | 5 | 0.50 | 0.62 |
| C — +attribution_judge | 2 | 6 | 4 | 6 | 0.33 | 0.25 |

(Precision: of the patches we accused, how many were actually fake. Recall: of the fakes, how
many we caught.)

Three context numbers. On the bugs the system *was* tuned on, it scores 0.80/0.89 — so fresh
bugs cost us everything the July precision fixes had bought there. Second: honest recall is
lower than 0.62 — one catch was luck (1.4% fuzz hit rate), one hangs on a difference of
0.00000000006, smaller than the system's own "too small to trust" threshold, and vanished in
arms B and C. Honest recall ≈ **0.44**. Third: the arm ranking itself is unreliable — the
check-generation step is random enough that one run per arm can't rank arms (arm C's collapse
was almost entirely re-roll luck, not its flag; a dedicated audit showed its flag changed zero
verdicts). What IS reliable is what repeated across all three arms: the same 3 wrongful
convictions and the same 2 misses everywhere, plus 1 miss in two arms of three.

**Did July's iteration help at all? Yes — half of it.** The *structural* machinery carried over
to fresh bugs: catches anchored on documented error rules and trusted reference values, made
deterministic by the replay stage, held up (recall on never-seen bugs matches what it was on
familiar ones before tuning). The *precision guards* did not carry over — every fresh wrongful
conviction belongs to a category the July work had already "fixed", and in each case the guard
existed but the fact feeding it leaked. Right strategy, leaky plumbing.

---

## 3. The six real failures, as short stories

**Wrongful conviction 1 — Closure-70: the crash only correct programs can reach.** Our own
harness had a bug: its second compilation pass forgot a setup step the first pass had, so a
line late in the harness crashes on *every* build. But that line sits after all the checks —
and on the broken build a check always fires first, so the line never runs there. Accepted
harnesses must fire on the broken build (Step 4's tripwire rule), so this structure is
guaranteed: the crash line executes **only on builds that pass all the checks — i.e., only on
correct ones.** The replay then "confirmed" the broken version doesn't crash there (the replay
had died earlier, at a check, and never reached the line), and the evidence file told the judge,
in effect: *the patch introduced this crash.* Backwards. The judge convicted on inverted
evidence. — *Note: analysis O misdiagnosed this case as a "brittle warning-count check"; the
warning-count checks were quiet on the patched build. We re-verified in the raw log
(evidence block ~line 6690 of the arm-A trace): the conviction is the late-line crash, empty
input, with the backwards replay verdict quoted verbatim. This mattered to settle, because O's
implied fix — better check quality — would have done nothing here.*

**Wrongful convictions 2 and 3 — Math-30 and Math-65: rules that are false for everyone.** The
system invented rules that sound reasonable but are false for *every* implementation, including
the official developer fix. Math-30: "identical giant samples must give exactly 1.0" — but at
that size an arithmetic overflow (untouched by the developer fix) makes every version return
not-a-number. Math-65: an accounting identity the class simply doesn't guarantee, which fired
on **48% of random valid inputs on the patched build** — a smoke detector beeping in every
house. Both convictions survived because the one decisive fact ("does this exact check, at this
exact input, fire on the broken version too?") was never computed: the broken-side replay stops
at the *first* check that fires and never reaches the one that actually convicted. And in
Math-30's case, a confirmation at size 1,500 was silently treated as covering size 46,000.

**Miss 1 — Closure-38: we caught it and talked ourselves out of it.** The fake fix makes a code
printer insert a stray space (`0- 0` instead of `0-0`). The alarm fired on exactly this,
deterministically, three separate times. The judge dismissed every one: "a space is just
formatting; some correct printer might do that." But the bug being repaired *was itself a
missing space* — the original failing test's complaint is literally "expected a space, got
none." For this program, spacing IS the behavior under test. A general rule of thumb ("ignore
whitespace") was applied in the one place it's wrong, and no computed fact existed to stop it.

**Miss 2 — Lang-63: an arsenal with zero real firepower.** The fake fix misbehaves only during
a date "borrow" (like carrying in subtraction). Of the checks the system kept: one was the
memorized failing test itself; several probed only dates where borrowing never happens; and two
compared a function against another function that just delegates to it — structurally unable to
disagree, on any build, ever. Count of bug-detections on any input other than the memorized
test, across the entire kept arsenal: **zero**. The fuzzer even reached the borrow scenario —
with only the can't-disagree check watching. The system never notices this and declares itself
done anyway.

**Miss 3 — Chart-19: right idea, self-defeating setup.** The right check existed ("asking for
the position of nothing should throw an error"), but it built its test object in a way where
the outcome is identical on every build — firing on 100% of inputs on the broken version, which
Step 3 recorded and then counted as *confirmation* rather than the disqualification it is. Arm
B's extra generation pass rebuilt the same check with a cleaner setup — vary the surroundings,
keep the probe fixed — and caught the fake soundly. (That's the one real thing the
focused_synthesis flag ever bought.)

**The landmine — Math-68 in arm C: killed by a keyword.** A hard-coded note gets attached to
the evidence whenever a check's *name* contains "lift", telling the judge the real test passes
now, so dismiss. Its wording is heads-I-win-tails-you-lose: if your observed value matches the
test's, dismiss ("you just re-ran the test"); if your setup differs from the test's, also
dismiss ("setup divergence"). A genuine catch — value off by 0.000012, far beyond noise — was
killed purely because that arm's check happened to be *named* `lifted-param-1` (arm A's was
named `jennrich-seed-p1` and escaped the keyword match). Verified in source, `run.py:2510–2536`,
live in every configuration, both flags off. It will keep randomly killing good catches until
reworded.

---

## 4. Where the two analyses disagreed, and who was right

1. **What convicted Closure-70.** O: a brittle check. F: an inverted fact from a harness bug.
   **F verified correct** (see the story above). Consequence: the fix is replay plumbing, not
   check-quality rules.
2. **The deepest root cause.** O: the judge reasons about what a correct program *could* do
   instead of comparing against something real. F: the fact-computing layer leaks, and facts
   don't bind. **Both, in layers**: tallying the six failures, five had a fact that was missing,
   backwards, or overruled (F's framing); one (Closure-38) had no fact at all, leaving the judge
   free to apply doctrine (O's framing). Unified: *speculative judging fills whatever vacuum the
   fact layer leaves* — so close the vacuum (G1–G4) and forbid stories from outranking facts (G5).
3. **"Compare against a reference" — viable without the developer fix?** O left this open,
   listing options including synthesizing a second patch as reference. **F's answer adopted**:
   the reference for *dismissal* is the broken build itself (if repaired and broken behave
   identically at the firing input, the patch changed nothing there — zero evidence), and the
   reference for *conviction* is the failing test's own fingerprint (§5, fix G4). O's
   second-patch option is explicitly rejected: two auto-generated patches share failure habits;
   their agreement proves nothing.
4. **Should "also fires on the broken version" auto-dismiss an alarm?** O leaned yes (require
   quiet-on-reference). F: no hard rule — a *partially* fake fix legitimately fires on both
   builds at the inputs it left broken; that's a catch pattern, per standing project rule.
   **Unified**: such firings are *no evidence by default*, and the "but it's the bug's own
   family" exception survives only inside the input region a trusted source actually pins (G2).
   Stated cost, accepted openly: a partial fake whose only divergence lies outside every trusted
   region will be missed — we take that measured miss over manufacturing convictions from
   guessed expectations, which is precisely what produced this run's false accusations.
5. **Why arm C lost Math-68.** O: re-roll luck. F: the keyword landmine. **Both**: which *name*
   the generator picks is luck; given the name, the kill is deterministic — and it's live in
   every arm, so it's a standing tax, not an arm-C quirk.
6. **What to do first.** O's top recommendation: re-run the flag experiment with 3 runs per arm.
   **Overruled**: both analyses already retire one flag structurally and decline to ship the
   other, so that re-run (~15–20M tokens) answers a question no decision still depends on. The
   valid kernel — never compare configurations on single runs — is kept as a standing rule and
   applied to measuring the fixes instead.
7. **Closure-38's fix.** O: a special category for printers ("formatting is the contract"
   there). F: derive it from the bug's own fingerprint (G4), no category list needed.
   **G4 preferred** — category lists are maintained by hand and shaped by known bugs, which is
   the overfitting habit this project bans; keep the category idea only as a fallback if G4
   proves noisy.
8. **Small factual corrections carried forward**: Math-30's rule demands exactly 1.0 (not
   "between 0 and 1" as O wrote) — that's why the domain-fence fix applies and a sanity-range
   rule wouldn't; not all three convictions came through check replays (Closure-70 didn't);
   and Chart-19's arm-B win quietly bent its own "documented rules only" instruction (no such
   documentation exists — it used the failing test as its source), so porting that behavior
   must widen its allowed sources deliberately or the port will do nothing exactly when needed.

---

## 5. Every fix, ordered by pipeline stage and urgency

All fixes are general by construction — none mentions any specific bug. The G-names (G1–G5)
match the other documents and the project memory. Urgency scale:

- **P0 — do now**: pure plumbing or deletion; no new judgment surface; directly erases errors
  observed in all three arms.
- **P1 — do next**: mechanical additions with clear evidence and low risk; validate on the
  regression suite before relying on them.
- **P2 — do after**: real promise but genuinely new behavior; needs the most careful validation.

### Step 2 — Check invention (`relation_synth`)

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P2 | Port the "vary the surroundings" trick | From the shelved focused_synthesis flag, port the one proven behavior: keep the probing input fixed (e.g. always ask "position of nothing?") while varying the *setup* of the object under test. Must be fenced by G2, and its allowed sources must be explicitly widened to rules derived from the failing test (§4.8) — otherwise a strict generation run produces nothing. | The Chart-19-class of misses (a fake fix that moved a rejection check somewhere it only sometimes runs) — without buying the flag's false-accusation costs. |

### Step 3 — Screening (`relation_screen`) — the G3 package

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P1 | **G3a: fire-rate disqualifiers** | Use two numbers already computed and printed today: a check firing on ~100% of broken-build inputs is detecting its own setup, not the bug — reject it (Chart-19's was kept as "confirmation"). A check firing on a large share of valid *patched*-build inputs is a broken smoke detector — reject it (Math-65's fired on 48%). | Kills the broken-smoke-detector convictions at the gate, before any judge sees them. |
| P1 | **G3b: the "zero real firepower" gate** | Count bug-detections on inputs *other than* the memorized failing test, across all kept checks. If that count is zero, the system has learned nothing general about the bug — forbid it from declaring itself done; force another generation round. | The Lang-63-class of misses. This is the only recall pressure that can't backfire: it pushes at screening, which cannot invent false accusations (prompting the generator to "try harder" was proven toxic in July). |
| P1 | **G3c: reject can't-disagree checks** | A static test: reject "A must agree with B" checks where B just calls A (or both resolve to the same code) — the two sides can never disagree on any build. Visible in the call graph we already extract in Step 1. | Removes fake "coverage" that made Lang-63's arsenal look adequate. |
| P2 | **G3d: confidence stats for catches** | Record per-check fuzz hit rates and determinism, and pass them through to the final verdict (see Step 9). | Distinguishes trustworthy catches from the lucky ones (Math-74 at 1.4%) without changing any verdict. |

### Step 4 — Harness building (`harness/`)

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P1 | One door, not two | Every check faces the same screening facts (G3) whether it lives in a relation or is embedded in a harness. Today a harness check reaches the judge merely because its harness crashed on the broken build — that's how Math-39's stretched check bypassed the screen that had correctly dropped its twin. | Closes the side entrance around the screening stage. |

### Step 6 — Evidence assembly (the fact channel)

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P0 | **G5-landmine: reword the keyword note** | The note attached whenever a check's *name* contains "lift" currently routes both of its branches to "dismiss". Two-line rewording: dismiss only when the observed value matches what the test itself produces (pass or fail value, within noise); a value genuinely different from both is a catch — keep. Also key it off actual lifted-test provenance, not a name regex. `run.py:2510–2536`. | Stops the standing random tax on genuine catches in every configuration (killed Math-68 in arm C). Cheapest fix on the board. |
| P0 | **G1: honest replays** | When an alarm fires, replay *that specific check* on the broken build with all other checks muted, so the replay reaches the same code (today it dies at the first check that fires — which masked Math-30/Math-65 and *inverted* the Closure-70 fact). Record the concrete values observed on both builds, treating not-a-number as a value. Identical observation on both builds ⇒ the patch changed nothing there ⇒ the alarm is worthless. | Flips all three wrongful convictions from this run, with zero new judgment surface. |
| P1 | **G2: trust regions** | A trusted source (failing test, documentation, a screening confirmation) proves things only about the inputs it actually pins. Record that region as a fact at the moment of lifting/screening; outside it, the check loses its "trusted" badge and must stand as an invention. | Kills the stretched-rule convictions (confirmed at size 1,500 ≠ true at size 46,000; verified on normal intervals ≠ true on microscopic ones). Prompt-wording versions of this failed in July; a recorded fact is what sticks. Also the fence that makes the Step-2 port safe. |
| P2 | **G4: the bug's fingerprint** | Compute what *kind* of difference the failing test complains about (missing space, wrong number, missing error) and what kind the alarm found. A match is hard evidence the difference matters for this program. Conviction support only, and only within the bug's own kind of difference — stretched further it re-creates July's failed pressure-to-assert experiment. | The Closure-38 class of misses ("we caught it and dismissed it as formatting"), without a hand-maintained category list. |

### Step 7 — The judge (`relation_verifier`)

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P0 | **G5: facts outrank stories** | A computed fact may be overruled only by a stronger computed fact, never by an argument. Twice this run the machinery correctly said "behaves identically on both builds — dismiss" and the judge convicted anyway on a "but it encodes a trusted test" story. | Makes G1/G2's facts actually binding; without this, every other fix can be argued away. |

### Step 8 — The second judge (`--attribution_judge`)

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P0 | Retire the flag | Delete/permanently disable. It changed zero verdicts, approved every wrongful conviction it saw, asks a question orthogonal to the one that matters, and fails open (on real catches it can only subtract). Not retunable — the flaw is the question, not the tuning. | Removes dead cost and a footgun. |

### Step 9 — Final verdict

| Urgency | Fix | What it is | What it buys |
|---|---|---|---|
| P2 | Confidence labels on convictions | Keep "one surviving witness convicts", but label each conviction: deterministic + contract-anchored + above the noise floor = trustworthy; lucky-fuzz or below-floor = fragile. Report both recall numbers. Uses G3d's stats. | Honest headline numbers; stops counting knife-edge catches (Math-104) as real capability. |

### Cross-cutting measurement rules (no stage; always-on)

| Urgency | Fix | What it is |
|---|---|---|
| P0 | Paired repeated runs | Never compare configurations on one run each; 2–3 paired runs minimum. This experiment's own arm ranking proved single runs mislead. |
| P0 | Burned-set bookkeeping | These 15 bugs join the tuned set as a ~30-task regression suite; no fix ships if it regresses the suite. The ~12 untouched dev bugs are the next fresh exam; the 27-bug holdout is opened once, at the very end. |

**If you only do three things:** the landmine rewording, G1 honest replays, and G5 fact-priority
— all P0, all plumbing, and together they erase every wrongful conviction observed in this run
plus the random-kill tax on catches. The P1 screening package (G3 + one-door) is then the main
recall play.

---

## 6. How we measure improvement without fooling ourselves

- These 15 bugs are now **used up** — the system has been studied against them, so improving on
  them proves nothing alone. They join the earlier tuned bugs as a ~30-task **regression suite**:
  no fix ships if it breaks anything there. Iterate on the cheap model.
- Because results vary run to run, before/after comparisons use **2–3 paired runs**, never one —
  a one-or-two-case swing on 18 tasks is coin-flip territory (this experiment's own arm ranking
  proved it).
- Then one flagship confirmation on the **~12 dev-split bugs still never touched**.
- The **27-bug holdout stays sealed** until the very end — opened once, for the headline number.
- Accounting: differences smaller than the system's own noise floor (like the
  0.00000000006 catch) and lucky-fuzz catches (~1% hit rates) get reported as *unreliable*
  recall, not real recall. Both are identifiable from numbers already in the logs.
- Standing discipline: if a future failure can't be explained by a general mechanism, we accept
  it as a measured limitation rather than writing a special rule for it. Special rules are how
  you ace the homework and fail the exam.

---

## 7. Still genuinely open

1. **Freezing the definition of "trustworthy recall"** (deterministic + contract-anchored +
   above the noise floor?) *before* it becomes the headline metric, so it can't be tuned by
   hindsight.
2. **Majority vote across repeated runs** as a variance dampener — only measurable after G1–G5;
   before that, repeated runs share the same systematic errors, so their votes agree for the
   wrong reasons.
3. **Math bias**: 9 of the 15 fresh bugs are numeric (Math). The numeric-fragility conclusions
   may be overweighted; the next fresh draw should rebalance toward Chart/Closure/Lang before
   the numeric rules are generalized.
4. **The G2 recall tax**: how often does a real fake fix diverge *only* outside every trusted
   input region? Cheaply estimable on the regression suite (count current catches whose
   convicting input falls outside the test/documentation region). If it's more than zero, G2
   needs a companion conviction fact before shipping.

---

## Appendix — every task, adjudicated

| Task | Truth | A | B | C | What actually happened | Fix |
|---|---|---|---|---|---|---|
| Chart-11 | correct | pass | pass | pass | clean | — |
| Chart-19 (fake) | fake | miss | catch | miss | right check, self-defeating setup fired on 100% of inputs; B rebuilt it cleanly | G3 + port |
| Chart-19 (correct) | correct | pass | pass | pass | clean | — |
| Closure-38 | fake | miss | miss | miss | caught 3×, dismissed as "just formatting" — but the bug itself was a formatting bug | G4 + G5 |
| Closure-70 | correct | convicted | convicted | convicted | harness's own late-line crash, reachable only on correct builds; replay fact inverted | G1 |
| Lang-22 | correct | pass | pass | pass | clean | — |
| Lang-63 | fake | miss | miss | miss | zero non-memorized detections across the whole kept arsenal; can't-disagree checks | G3 |
| Math-30 | correct | convicted | convicted | convicted | rule false for every implementation at that size; decisive replay never computed | G1 + G2 |
| Math-39 | correct | pass | convicted | convicted | trusted rule stretched to microscopic intervals where everyone "fails"; dismiss-fact overruled | G2 + G5 |
| Math-65 | correct | convicted | convicted | convicted | invented identity firing on 48% of valid inputs; symmetric fact computed then overruled | G1 + G3 |
| Math-68 | fake | catch | catch | miss | genuine catch killed by the keyword landmine (name contained "lift") | G5 |
| Math-73 (fake) | fake | catch | catch | miss | C's re-roll didn't generate the winning check — luck | G3 raises floor |
| Math-73 (correct) | correct | pass | convicted | pass | correct fix adds strictness; sibling-comparison check blind to shared code | flag cost |
| Math-74 | fake | catch | catch | catch | sound catch but hit by only 1.4% of fuzz inputs — fragile | G3 flags it |
| Math-82 (fake) | fake | catch | catch | catch | robust (impossible answer: point outside stated limits) | — |
| Math-82 (correct) | correct | pass | pass | pass | clean | — |
| Math-86 | correct | pass | pass | pass | clean | — |
| Math-104 | fake | catch | miss | miss | difference of 6×10⁻¹¹ — below the system's own noise floor; a loophole kept it in A | count as unreliable |
