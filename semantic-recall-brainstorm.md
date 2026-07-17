# Semantic-bug detection — the plan

Rewritten 2026-07-16 (late). Full forensic history in
`semantic-recall-history.md`. Companion docs: `suites/DATASET_AUDIT.md`
(inventory + verdicts), `suites/UNDETECTABLE.md` (exclusion evidence),
`suites/pinned_tasks.jsonl` (the verified task set),
`suites/label_annotations.jsonl` (label corrections).

---

## The problem

We are given a bug and a candidate patch for it. Some patches are real
fixes; some are **overfit** — they make the bug's failing test pass
without actually fixing the underlying behavior. Our pipeline writes a
small fuzzing program (a **harness**) full of **checks** ("this call must
not crash", "the mean must equal n·p") and runs it against the patched
program. We need the checks to be:

- **safe**: they never accuse a genuinely correct patch, and
- **sharp**: they do catch the overfit one.

And we must do this **without ever consulting the developer's real fix**
in the decision — that would be cheating (in the real world there is no
reference fix). An overfit patch passes every existing test by
construction, so the only place it can be caught is on inputs no test
covers — where "what is correct?" must be reconstructed from indirect
evidence, ranked by how much we trust it:

1. the bug's original failing test (definitive, but only for its inputs)
2. the buggy program itself (correct everywhere except at the bug)
3. the documentation comments (the promised contract)
4. universal math/logic rules ("sorting twice = sorting once"), if
   genuinely universal
5. the patch's own code — **least trusted; it may be the overfit**

Every **miss** (an overfit we don't catch) means we failed to check
something the overfit gets wrong. Every **false alarm** (a correct patch
we accuse) means we checked something a correct program is actually
allowed to do differently.

**Firewall rule**: the developer fix may be used ONLY offline — for
cleaning the dataset, verifying labels, and understanding our misses
afterwards. Never in any decision the pipeline makes.

**Substrate rule**: all experiments run against `pinned_tasks.jsonl`,
where every overfit patch has been verified to actually behave
differently from the real fix, and every correct patch has had its label
double-checked. The audit showed the raw dataset lies: "overfitting"
patches that are literally the developer fix (Math-59), the same file
labeled correct in one folder and overfitting in another (Lang-41), and
"correct" patches that are behaviorally wrong (Lang-10). On the pinned
set, every miss is a real technique failure and every false alarm a real
safety failure — the numbers mean something.

---

## The plan

Four phases. Phase boundaries are measurement points — never turn on
several untested changes at once (we did that once and could no longer
tell which change caused what). Each item: Case (why, with the real
story) / How (file-level) / Validate / Effort.

---

### PHASE 0 — Foundations: make every downstream number trustworthy (~3–4 days)

All four are mechanical plumbing; none changes what the checks assert;
nothing measured later is interpretable until they land.

**P0.1 Verify every patch really got applied, and really fixes the test**
- *Background, in plain words:* a patch file is a list of edits to source
  code; each edit block ("change these lines near line 300") is applied
  separately by the `patch` tool. Some patch files contain several edit
  blocks.
- *Case:* Lang-50's correct patch lists its two edit blocks in reverse
  line order — and our code, which hands the whole file to `patch` in one
  go, applied the FIRST edit and silently skipped the second. No error.
  We then built and tested a program carrying only half the patch: it
  failed the bug's own original test, and certification recorded 43
  phantom behavior differences for it (the fully-applied patch shows
  zero). Separately, Math-2/SOFix's patch file was stored backwards AND
  cut off mid-line — it never applied at all in any run, and a scoring
  bug counted the resulting do-nothing runs as passes for weeks. Two
  different file problems, same root failure: the pipeline tested
  programs that were never what it believed they were.
- *How:* (a) in `fuzz_runner.PatchedProjectBuilder`: apply each edit
  block one at a time, sorted by line number; if ANY single edit fails to
  apply, stop with an error — never continue with a partly-applied patch.
  Afterwards, sanity-check that the number of changed lines in the code
  matches what the patch file promised. (b) the safety net, added to both
  the pipeline (`run.py`) and the certifier: before using any patched
  program, run the bug's original failing test twice — it must FAIL on
  the unpatched code (proves the bug is present in our environment;
  otherwise status `bug_not_reproduced`) and PASS on the patched code
  (proves the patch applied fully and does what a plausible patch must;
  otherwise status `bad_patch`). The unpatched-side result is cached per
  bug since it never changes.
- *Validate:* known answers — the original Lang-50 patch file must be
  caught (`bad_patch`) before the applier fix and sail through after it;
  the archived broken Math-2/SOFix file must be caught. Then sweep all
  patches with multiple edit blocks for the same silent half-application
  in past runs, and redo any affected certifications.
- *Effort:* ~1 day. **Do first — it protects even the audit tooling.**

**P0.2 Make sure a check's alarm can't be silenced by its own error handling**
- *Case:* A check raises its alarm by throwing an error ("rule
  violated"). In the Lang-7 runs, every generated check wrapped its whole
  body — including that alarm throw — inside a try/catch that catches
  everything and quietly gives up. So the alarm was thrown, immediately
  caught by the check's own catch clause, and discarded. A rule that
  should have fired on all 20,000 inputs against the buggy program
  registered zero firings — and the pipeline read "zero firings on buggy"
  as "well-behaved rule, keep it" and promoted the useless check into the
  harness. About half of all generated harnesses had this pattern,
  because our own prompt wording ("wrap in try/catch and skip on
  exception") invites the mistake. Asking the model more firmly won't fix
  what a mechanical code check can.
- *How:* new `relation_screen._violation_swallowed(body)` (parse the
  Java; fall back to brace matching): flag any alarm throw that sits
  inside a try block whose catch clause catches everything and does not
  re-throw the alarm. Apply it at both entry points for candidate checks
  (the screening format gate and the `campaign.py` structural gate) —
  rejection feeds the existing retry loop with a pointed error message.
  Plus a canary in `screen_relations`: for each candidate, also compile a
  variant that is FORCED to raise its alarm and confirm the counting
  machinery records at least one firing — proving the alarm can actually
  be heard end to end.
- *Validate:* the broken snippet from the Lang-7 logs is the test case:
  rejected as-is; the corrected version (alarm re-thrown) passes and
  fires on ~100% of inputs. NOTE: today's rule "discard any check that
  fires on more than 20% of inputs" would then delete exactly that good
  check — until P2.2 fixes that rule, log such checks loudly instead of
  deleting them.
- *Effort:* ~½ day + fixtures.

**P0.3 When a harness re-labels a crash, look at what actually crashed underneath**
- *Case:* The Chart-26 false alarm. The chart library has a pre-existing
  crash that has nothing to do with the bug: give the text-measuring code
  a title containing malformed characters and it crashes on EVERY version
  — buggy, patched, or fixed. Our safety net normally recognizes exactly
  this ("the same crash happens on the unpatched program too, so it's not
  the patch's fault") — but only when the crash reaches it undisguised.
  One harness caught this crash and re-threw it as its own alarm type,
  and the safety net deliberately doesn't question the harness's own
  alarms — so the disguised pre-existing crash walked straight past the
  only mechanical defense and was blamed on the patch. Whether a harness
  re-wraps a crash or reports it directly is model coin-flip, which is
  why the same crash was correctly dismissed one day and wrongly counted
  the next.
- *How:* (a) a rule, checked mechanically in `campaign.py`: when a
  harness converts a caught crash into its alarm, it must attach the
  original crash as the alarm's "caused by" record (Java's standard way
  to say "this error came from that one"). (b) `fuzz_runner` parses that
  record, so every alarm carries the identity of the underlying crash
  (exception type + the library code line it came from) alongside the
  alarm itself. (c) extend the safety-net check in `run.py`: if an
  alarm's underlying crash is a generic library crash, replay the same
  input on the unpatched buggy program — if the same crash appears there
  too, it is pre-existing and the alarm is dismissed.
- *Validate:* replay the saved Chart-26 crashing input — it must now be
  dismissed. The genuine Chart-26 catch on the overfit side (a
  synthesized rule with no underlying library crash) must survive.
- *Effort:* ~1 day. **Highest-value safety fix; predicted to remove the
  one known false alarm on its own.**

**P0.4 Know which individual check earned its place — and which never ran**
- *Case:* A harness usually contains several checks that run one after
  another. In one Chart-26 attempt, the first check fired on every single
  input on the buggy program — so execution never got past it, and the
  checks written after it never ran even once. Our acceptance step only
  asks "did the harness AS A WHOLE fire on the buggy program?", so it
  accepted the harness on the strength of check #1 alone. One of the
  never-ran checks then met its first-ever execution against the correct
  patch — and promptly raised a false alarm. (The same coarseness let
  another harness pass acceptance via the pre-existing crash from P0.3
  rather than via the actual bug.)
- *How:* rule: every alarm message starts with a short name for its check
  (`[oracle:mean-formula]` — checked at the structural gate), so firings
  can be told apart. At acceptance, run the buggy program in keep-going
  mode (already supported as `collect_fired_oracles`) and record which
  named checks ever fire. Step 1: checks that never fired on the buggy
  program are flagged loudly as "never exercised" and the flag is passed
  to the verifier as context. Step 2 (only if step 1 proves useful): cut
  never-exercised checks out of the harness before running it against the
  patch.
- *Validate:* the specific never-ran Chart-26 check must come out flagged
  on a rerun.
- *Effort:* ~1 day for step 1.

---

### PHASE 1 — Baseline measurement (dev set only, ~100k tokens)

We do NOT run the full pinned set here — that costs too much per
iteration, and we need untouched bugs for an honest final number. The
pinned dataset carries a `split` field dividing it in two:

- **dev (30 legs, 17 bugs)** — the working set for all iteration. It
  contains the 12 bugs whose failures we studied in depth to design the
  fixes (Math-2, Chart-26, Lang-7, …). Those HAVE to be here: because we
  built the fixes by staring at exactly these bugs, a good result on
  them can never prove the method generalizes — they are spent as
  evidence. Also included: the 3 bugs named as validation targets in
  Phases 1–3, and 2 untouched pairs (Chart-3, Closure-33) as a smoke
  check that changes don't break ordinary tasks.
- **held-out (71 legs)** — never touched during development: no runs, no
  prompt tuning, no debugging against it. It is spent ONCE, at the very
  end, with the flagship model. That run is the number that counts.
  Expect dev numbers to look better than held-out numbers — that gap is
  itself the measure of how much we over-tuned.

The split is at bug level (both sides of a bug stay on the same side of
the split), because P3.2 shares synthesized rules between patches of the
same bug — a bug with one foot in each set would leak.

**P1.1** Generate `suites/pinned_dev.cases` from the `split: dev` rows of
`pinned_tasks.jsonl` (`patchfile:` case specs; run_suite supports them).
Score against the pinned labels plus `label_annotations.jsonl`.

**P1.2** Run the dev set with the Phase-0 fixes on. Record per task:
outcome, which named checks fired, and every dismissed alarm with its
reason. Between-phase gates rerun only the dev set; day-to-day iteration
should use even less (the 2–6 legs relevant to the change, per the
iterate-cheap rule). Two task-specific notes:
- *Lang-50 correct side* only becomes runnable AFTER the P0.1 applier
  fix — until then the pipeline builds it half-patched and any result is
  meaningless (it's flagged BLOCKED in the dataset).
- *Expected permanent miss:* Time-11's overfit is only visible from a
  second thread, and we deliberately keep threads out of harnesses (a
  check that sometimes fires and sometimes doesn't, depending on thread
  timing, can't be trusted either way). So the realistic dev ceiling is
  15 of 16 overfits, not 16 — don't spend iterations chasing the last
  one (that's the Lang-7 lesson: know which tasks are unwinnable and
  stop paying for them).

**P1.3 Sort the misses by how big the behavior difference is (offline;
uses the dev fix, so never in the decision path)**
- *Case:* For every overfit patch we certified, we know how many probe
  outputs differ between it and the real fix — anywhere from 3 lines to
  2,679. That number tells us what a miss means. If we miss a patch whose
  behavior differs on thousands of inputs (Time-4: 1,764), then almost
  ANY inputs would have hit the difference — our inputs were fine and we
  simply never CHECKED the property that differs. If we miss a patch that
  differs on only a handful of inputs (Math-53: 3; Closure-73: 7), the
  opposite: our checks may be fine but our inputs
  never reached the tiny corner where behavior differs. Today both look
  like the same miss; sorted this way, they demand opposite fixes — and
  the split decides whether Phase 3 starts with better checks (P3.1) or
  better aim (P3.2).
- *Third bucket — the witness-only patches.* Seven dev overfits (Chart-7,
  Lang-41, Lang-60, Closure-62, Math-57, Closure-92, Time-11) have a
  probe count of ZERO — even our certifier's probe missed them, and
  they're only proven catchable by a hand-written program that looks at
  exactly the right thing (overlapping time periods, the buffer's
  capacity, one specific formatter mode…). We EXPECTED these to be the
  hardest. **The baseline falsified that: the pipeline caught 5 of the 7
  (Chart-7, Lang-41, Lang-60, Closure-62, Closure-92) — only Math-57
  (float width) and Time-11 (cross-thread, the expected permanent miss)
  stayed misses.** The lesson: "witness-only" describes the CERTIFIER's
  single probe, not the pipeline — the pipeline generates several diverse
  harnesses and finds surfaces the one probe didn't. Don't treat
  witness-only as pipeline-hard.

- *What the baseline actually showed about the buckets (2026-07-17).*
  Refinement to the dichotomy above. The two BROAD misses (Lang-50: 225
  divergences; Math-2: 117) were NOT "we never checked the property" —
  in both, the discriminating check WAS generated, but it was either
  LATENT (never fired on the buggy build, so unanchored — P0.4 flags this
  exactly) or STOCHASTIC (see below). So a broad-divergence miss is
  three-way, and P0.4's latent data tells them apart: (a) the
  discriminator was never generated; (b) generated but latent →
  P2.1/P2.2 (feed direction, screen on the trigger inputs) + P3.2
  (anchor at the bug); (c) generated but flaky. In EVERY one of the
  seven baseline misses the check that DID fire on buggy was the
  lifted-seed oracle — the reported input, which the overfit
  special-cased — so the patched build passes it. That single mechanism
  unifies the misses and points squarely at Phase 2 as the next step.

- *Stochastic-oracle miss (new; Math-2-o).* Math-2's overfit was a catch
  at the P0 gate and a miss at the baseline — a real flip-flop, not
  noise. Cause: the check that fires on buggy is built on `sample()`, a
  RANDOM draw, so its verdict depends on fuzzing luck (the RNG must hit
  the overflow path AND produce a negative sample). The reliable
  discriminator is `getNumericalMean()` = −49.76, which is DETERMINISTIC
  and which the Arja patch never touches — and it sat latent. Lesson: an
  oracle anchored on a nondeterministic method gives flaky verdicts;
  prefer a deterministic discriminator. This is what P2.2 must add (a
  reproducibility check) and what P3.2's pooled mean-formula rule
  delivers.

*Predictions:* Chart-26 correct side flips false-alarm → clean from P0.3
alone; Math-2 correct side is genuinely clean now (file repaired +
tolerance in the check); Math-2 overfit side is caught only FLAKILY until
P3.2 makes it deterministic; no previously-caught overfit regresses.

---

### PHASE 2 — Direction & grounding: make check-writing trustworthy (~2–3 days)

**P2.1 Give check-writing the one trusted truth we have — the failing test**
- *Case:* To write a good check, the model must know which direction is
  correct: does `createNumber("--1.1")` correctly return null, or
  correctly throw an error? The bug's original failing test answers this
  definitively (it expects the error). But our pipeline never shows that
  test to the check-writing step — the field meant to carry it has been
  empty since forever. Worse: the code that step DOES see is shown under
  the heading "Patched method(s):" while actually being the BUGGY
  version, and the diff we show has its plus/minus markers stripped, so
  added lines and deleted lines look identical. On Lang-7 the model read
  the buggy body, believed the heading that said it was the fix, and
  wrote its rule exactly backwards ("--" must return null). We withheld
  the most trusted source from the one step that needed it, and fed it
  mislabeled code instead.
- *How:* `run.py` passes the failing test's source and its expected
  values into `relation_synth`, displayed at the top as the most trusted
  block ("the patch itself may be the overfit; where the failing test
  pins down a behavior, the test wins"). Fix the heading to "BUGGY
  method(s) (pre-patch):". Keep the plus/minus markers in the diff. Also
  show what the method looks like AFTER the patch (apply it
  mechanically). And in `code_context.assemble_class_context`, when long
  files get trimmed to fit, keep the changed method's documentation
  comment first — Lang-7's method doc states the exact contract, and it
  was trimmed away.
- *Validate:* rerun the Lang-7 correct side (one cheap task): the
  backwards rules are gone and the new candidates cite the test's
  expected error.
- *Result (2026-07-17 p2val, 3 legs): mechanism confirmed, no
  regression.* On Math-2's CORRECT leg synthesis proposed the
  mean-formula and the direction check ranked
  `mean_matches_documented_formula` FIRST as direction-confirmed
  (fires on the failing test's inputs, 20000/20000 on random) — the
  exact deterministic discriminator that was latent at the baseline —
  and the leg stayed clean (the formula holds on the fixed build, so no
  false alarm). Lang-7-c stayed clean too. Math-2's OVERFIT leg,
  however, is STILL a miss — because synthesis that run proposed
  different relations (quantile/sample) and never generated the
  mean-formula on that leg. The convicting relation exists on the
  sibling leg but is not shared → this is the P3.2 pooling gap, not a
  Phase-2 failure. Phase 2 does what it controls; P3.2 is what closes
  Math-2-o.

**P2.2 Test every candidate rule on the failing test's own inputs before trusting it**
- *Case:* two failures, one fix. (a) The backwards Lang-7 rule survived
  screening because screening only asks "does the rule fire on the buggy
  program?" — and a backwards rule stays QUIET on the buggy program,
  because the buggy behavior is exactly what it (wrongly) demands. So
  "quiet on buggy" currently lumps together three very different things:
  rules that are right, rules that encode the bug itself as the truth,
  and rules that never really ran. All three get promoted the same way.
  (b) Once P0.2 un-silences alarms, a correct rule aimed straight at the
  bug will fire on nearly EVERY input on the buggy program — and today's
  "discard anything firing on more than 20% of inputs" rule would delete
  precisely our best rules.
- *How:* in `relation_screen`, a second measurement per candidate, this
  time feeding it the failing test's own input values (the machinery for
  seeding chosen inputs already exists). Outcomes: fires on those inputs
  ⇒ the rule points the right way — rank it first and exempt it from the
  20% discard rule; stays quiet on those inputs while claiming to cover
  the changed behavior ⇒ probably backwards — drop it; the test's inputs
  never reach the rule ⇒ today's unknown case, keep with today's rules.
  The 20% discard rule remains only for unconfirmed rules.
- *Validate:* from saved logs — the backwards Lang-7 rule drops; the
  corrected one ranks first. Depends on: P0.2, P2.1.
- *Add a reproducibility check (new, from the baseline).* Math-2's
  overfit was caught once and missed once because its firing check reads
  `sample()`, a random draw — the verdict depended on fuzzing luck. When
  the trigger-input measurement above runs, run it a few times: a check
  that fires on the trigger inputs on some repeats and not others is
  FLAKY. Rank a deterministic discriminator ahead of a flaky one, and
  when only a flaky check is available, say so in the record (so a
  changed outcome for that leg is known to need the confirm-repeat from
  measurement rule #2). The reliable Math-2 discriminator
  (`getNumericalMean` = −49.76, deterministic) was generated but latent;
  ranking determinism first is what surfaces it.

**P2.3 Don't approve a rule the harness is forbidden to contain**
- *Case:* For Math-2, check-writing actually produced the ONE rule that
  convicts the overfit (the mean of the distribution must match the
  textbook formula). It passed screening — but implementing it in the
  harness required writing a small custom subclass of the patched class,
  which the harness rules forbid (screening compiles under NO such
  rules, so nothing objected). The model resolved the contradiction
  silently: it accepted the rule and then simply didn't put it in the
  harness. Nobody noticed that handed-over ≠ implemented, and the miss
  followed. The irony: a standard library class
  (`UniformIntegerDistribution`) reaches the same behavior perfectly
  legally — nobody told the model to look for one.
- *How:* screening compiles candidates under the same restrictions the
  harness must obey (no custom subclasses of the patched classes — a
  simple syntax check), so an unimplementable rule fails early; the
  check-writing prompt states the restriction and suggests using existing
  library classes; and `run.py` compares the rules we handed to harness
  generation against what actually appears in the harness source, logging
  any rule that was handed over but never implemented — loudly.
- *Validate:* Math-2 overfit side rerun — the convicting rule either
  arrives in implementable form or is rejected at screening; never
  silently dropped.

---

### PHASE 3 — Catching more overfits (~3–4 days; internal order decided by P1.3)

**P3.1 Teach checks the six places overfits actually hide**
- *Case:* Comparing ~75 patches against their real fixes gave us
  measured knowledge of where the behavior differences live: (1) in
  sibling methods that do the same job — Lang-41 fixed the variants of a
  method that take a Class object but left the String variants broken, so
  the two now disagree with each other about the same input; (2) in
  hidden state changed by supposedly read-only calls — Lang-60's
  `contains(char)` silently shrinks the buffer's capacity from 32 to 3,
  and a later lookup reads leftover stale characters; (3) in non-default
  settings — Closure-62 misbehaves only in one particular error-message
  formatting mode; (4) at extreme number sizes — Math-57 changed a
  `double` to a `float`, which only matters for values around 10^20; (5)
  on irregular inputs — Chart-7 misbehaves only when time periods
  OVERLAP; (6) across threads — Time-11 crashes only when called from a
  different thread than the one that loaded the class. The first two are
  catchable WITHOUT the real fix by two generic checks: "a call that
  documents itself as read-only must not change what the object reports
  about itself" and "two methods documented to do the same thing must
  agree". These are facts about APIs in general, not shapes of particular
  bugs — so they respect the no-dataset-overfitting rule.
- *How:* the prompt building blocks in `prompts.py`
  (`_variant_strategy_menu`, `_consistency_hint_block`) enumerate the six
  places in category language. New `_observer_state_block`: after any
  call whose documentation reads like a question (get*/is*/contains/
  indexOf/size, and no mention of modifying), re-read the object's cheap
  properties and assert they didn't change. Check-writing instructions
  gain the sibling-agreement shape (`f(x)` must equal `f(convert(x))`
  where the docs say they're the same). The cross-thread idea stays OUT
  of harnesses (too flaky) and is used only in offline certification.
  And one standing rule for ALL of these: any check that compares
  numbers compares them with a tolerance, never exact equality. Fresh
  evidence for why: Math-39's CORRECT patch differs from the developer
  fix at the 13th significant digit of the integrator's output — two
  correct implementations legitimately disagree at that scale, so an
  exact-equality check there is a guaranteed false alarm
  (`label_annotations.jsonl` has the record).
- *Validate:* measured as the Phase-1-vs-Phase-3 improvement specifically
  on the dev-set patches whose behavior difference is tiny (Math-53: 3
  differing outputs, Closure-73: 7, Chart-26: 10) — the analysis predicts
  those benefit most. The held-out set has its own tiny-difference
  patches (Math-80: 4, Chart-19: 14); they stay untouched and will show
  at the end whether the improvement generalizes.

**P3.2 Aim checks at where the bug lives, not where the patch edited — and share rules between patches of the same bug**
- *Case:* Math-2, the canonical case of a patch that edits the wrong
  place. The real bug is an arithmetic overflow inside
  `getNumericalMean`. The Arja patch edits a DIFFERENT method entirely
  and passes the failing test by coincidence — `getNumericalMean` still
  returns the impossible −49.76 on the patched build. Our checks are
  aimed at whatever the patch touched, so on the Arja task every check
  stared at the edited (irrelevant) method and none ever looked at the
  broken one. Meanwhile, on the OTHER patch for the same bug,
  check-writing produced exactly the rule that would convict Arja — but
  rules aren't shared between tasks, so the Arja task never saw it. Even
  our own certifier made the same aiming mistake once: its first probe
  stared at the patched method (identical outputs on both builds, even at
  overflow-triggering inputs) and reported "no difference"; probing the
  objects the failing test actually uses revealed 117 differences. One
  aiming lesson, proven twice independently.
- *How:* (a) also aim at the failing test's neighborhood: take the class
  and method names that appear in the failing test's body, intersect with
  the project's classes, and add their public methods as a second aiming
  block for check-writing (`analysis.py`). (b) pool rules per bug:
  persist every screened rule keyed by (project, bug), and give every
  patch of the same bug the whole pool. The rules carry no labels or
  verdicts, so nothing leaks between tasks.
- *Validate:* THE decisive experiment — run the Math-2 pair. Prediction:
  the Arja miss becomes a catch via the mean-formula rule; the correct
  SOFix patch stays clean because the rule compares with a tolerance
  rather than exact equality. If this fails, stop and debug before
  building anything more on aiming.
- *Result (2026-07-17 p32val2): mechanism validated, one real bug found
  and one tolerance bug found.* A direct deterministic probe settled the
  ground truth: getNumericalMean at the overflow params is −49.76 on
  BOTH the buggy and Arja-overfit builds (Arja edits elsewhere, never
  fixes it) and 49.82 on the SOFix-correct build (= the formula, = the
  dev fix); sample() is likewise identical between SOFix and the dev fix
  (always in-support) and all-negative on buggy. So the deterministic
  mean-formula SEPARATES the pair perfectly at normal params. The
  pooling/anchoring/direction machinery all fired correctly (pool
  save→load→re-screen confirmed; a relation came back
  direction-confirmed). TWO real bugs surfaced, both now understood:
  (1) the JSON double-escape that made the mean-formula fail to compile
  (fixed — literal `\n` recovery); (2) **the synthesized mean-formula
  used too-tight tolerance (1e-12 relative) and FALSE-FIRED on the
  correct SOFix build at extreme parameters (N=n≈2.1 billion), where
  double rounding exceeds 1e-12** — the FP. This is the standing
  tolerance rule not being honoured generously enough; the fix is in the
  synthesis prompt (magnitude-scaled tolerance, looser floor for
  large-integer intermediates). Math-2-o's residual FN is a separate
  input-coverage issue: the harness must feed the overflow-inducing
  large parameters to the mean-formula on the patched build for it to
  fire there. **Net: the P3.2 hypothesis (a deterministic discriminator
  cleanly separates the pair) is confirmed true by probe; the pipeline
  needs the tolerance fix + reliable large-param generation to realise
  it. Math-2 is a NOISY binary-gate target — its verdict swings on
  tolerance and input luck — so treat the probe as the real evidence.**

**P3.3 A "must not crash" check should insist on the SAME crash it saw before**
- *Case:* Chart-26's other half. One check says "drawing the chart must
  never crash". On the buggy program it fired because of the bug's actual
  crash (a null-pointer error inside axis-label drawing). On the correct
  patch the SAME check fired again — but this time from the unrelated
  pre-existing text-measuring crash (see P0.3). A catch-everything check
  cannot tell those apart; the crash's identity (its exception type and
  the code location it came from) can. And pinning the check to the
  original crash site is still a fair generalization of the failing test:
  a half-fix that still crashes at the same place on OTHER inputs is
  exactly what we want to catch.
- *How:* builds on the P0.3/P0.4 plumbing — at acceptance, record which
  underlying crash made each "must not crash" check fire on the buggy
  program; on the patched build, that check only counts if the crash
  matches the same type and location; a firing from a different crash
  site is ignored.
- *Validate:* ablation — rerun the Chart-26 correct side with P3.3 but
  WITHOUT P0.3: the pinning alone should also remove the false alarm.
  Two independent defenses against the same failure.

---

### PHASE 4 — Advanced (only after Phases 0–3 are measured)

**P4.1 Compare the patch to the BUGGY program — no real fix needed**
- *Case:* Math-2/Arja's edited region behaves IDENTICALLY to the buggy
  program (measured — same outputs even at the overflow-triggering
  inputs). In plain terms: the patch changed code without changing
  behavior, and passed the failing test by pure coincidence. "Your change
  did nothing, yet the test now passes" is a strong overfit signature —
  and it is computable using only the buggy build we already have, so
  using it in decisions doesn't violate the firewall.
- *How:* let the certifier run with the buggy build as the comparison
  point instead of the fixed one (`--baseline buggy`, ~30 lines); rule:
  if the edited region behaves identically to buggy everywhere except on
  the failing test's own inputs (ignoring rounding-level and
  message-text-only differences), flag as suspicious.
- *Validate:* the Math-2 pair (Arja flagged, SOFix clearly different from
  buggy), then measure the false-alarm rate on 3–4 verified-correct
  patches before this may influence any verdict.

**P4.2 Stop asking the probe-writer to be thorough — make the machinery thorough**
- *Case:* Half of our certifier's "no difference found" answers turned
  out to be wrong — and every one of them came from probes where the
  prompt demanded "print EVERY public observable" and the model simply
  didn't do it (never called `capacity()`, never the sibling method,
  never used a second thread). Same lesson as everywhere else in this
  project: a mechanism beats an instruction.
- *How:* split probe writing in two. The model only constructs
  interesting input objects and call sequences. A fixed piece of our own
  code then automatically finds and calls every public no-argument method
  on those objects (sorted, values only) before and after each step, and
  prints all the results. The model cannot forget what it never had to
  remember.
- *Validate:* known answers — the five wrongly-cleared patches (Chart-7,
  Lang-41, Lang-60, Closure-62, Math-57) must flip to "difference found"
  with NO prompt changes; then re-run all remaining "no difference"
  verdicts.

**P4.3 Give the verifier one decision per crash, not one per firing**
- *Case:* In one Chart-26 run the LLM verifier reviewed two firings that
  were the SAME exception from the SAME code location — it correctly
  dismissed the first, then kept the second, one call later. We already
  measured that majority voting (asking three times) doesn't help: cost
  tripled, the error rate didn't move. The fix is structural: stop asking
  the same question twice and hoping for consistency.
- *How:* group firings that share the same check name and the same
  underlying crash identity (both exist once P0.3/P0.4 land); ONE
  verifier call per group; the verdict applies to the whole group; if
  contradictions somehow remain, the dismissal wins.
- *Validate:* replay saved verifier decisions (`verifier_replay`) and
  compare wrong-keep and wrong-dismiss rates before enabling.

**P4.4 Use the certifier to label the 205 unlabeled patch files we can check cheaply** (dataset growth, not pipeline)
- *Case:* The certifier out-performed human labeling exactly where humans
  err — dead code, redundant safety checks, code that is textually
  different but does the same thing — and 205 unlabeled patch files
  belong to bugs whose reference builds we already have cached, so
  checking them is cheap.
- *How:* certify one file per (bug, tool) first (~150k tokens);
  "difference found" verdicts can be trusted directly; "no difference"
  verdicts must go through the deep-dive protocol before anyone believes
  them (we know first-pass zeros are unreliable).
- *Validate:* manually spot-check a sample of machine labels before any
  of them enter the pinned task set.

---

## Rules for every measurement (so the numbers stay believable)

1. **No "before" baseline run.** Don't pay for a pre-Phase-0 measurement
   — the old fixconfirm and diag-24 runs already are the before picture,
   and their failures are fully explained. The first paid run is the
   Phase-1 baseline, with Phase-0 fixes on.
2. **A changed outcome must be confirmed once before we believe it.**
   Harness generation is partly random — we've measured the same patch
   file getting 0 behavior differences from one generated probe and 5
   from the next, with nothing changed. So when a task changes outcome at
   a gate (miss → catch, or a new false alarm), rerun that ONE task once;
   only call it changed if the repeat agrees. Never repeat the whole set
   for this.
3. **Results are tied to the environment they ran in.** Every run records
   the JVM (currently OpenJDK 11.0.31). The unwinnable-task list is
   environment-specific — Lang-7 is unwinnable precisely because OUR JVM
   doesn't have the old parsing quirk. If the JVM or OS image ever
   changes, the exclusion list and the equivalence verdicts must be
   re-checked before comparing numbers across the change.
4. **Held-out hygiene has one exception.** If a held-out task fails for
   plumbing reasons (build broke, patch didn't apply), fixing the
   plumbing is fine. What is never fine: adjusting prompts, checks, or
   thresholds based on anything seen in held-out output. A held-out bug
   we debug against silently becomes a dev bug, whether we relabel it or
   not.
5. **Suite-run mechanics (learned the hard way on 2026-07-17).** Run
   legs as parallel as the model API allows (4-way default, up to 6 for
   the small projects — beyond that the shared API is the bottleneck,
   not the machine). Before launching, check free disk: every leg keeps
   its own working copy of the project (~100 MB for Lang/Math/Time, ~1
   GB for Closure/Chart) until the suite ends — a Chart/Closure-heavy
   suite filled the whole disk mid-run and 18 legs died at checkout.
   After every suite: delete the working copies (pure scratch, always
   reproducible), archive the results (tiny — all history so far
   compresses to ~2 MB) to the Mac under `runs-archive/`, verify the
   archive's checksum (a tar written on a full disk truncates
   silently), and keep on the VM only the suites still needed for
   comparison.
6. **Success targets, fixed now so we can't move the goalposts later.**
   Starting point was 58% of overfits caught. Dev-set goal after Phase 3:
   at least 13 of 16 overfits caught (the ceiling is below 16 — see the
   expected-miss note in Phase 1) with ZERO false alarms on the 14
   correct legs. Held-out goal at the final run: at least 70% of the 28
   overfits caught, at most 1 false alarm on the 43 correct legs. And a
   stop rule against endless tuning: if two consecutive iterations don't
   change any dev outcome, stop iterating that phase — more tuning at
   that point is just fitting noise.

---

### Why this order, one line each

- Phase 0 first: Lang-50 and Math-2 proved that even our AUDIT was
  standing on sand without it — and P0.3 alone should remove the one
  known false alarm.
- A measurement pass between every phase: we once turned on several
  untested changes together and could no longer tell which change caused
  what.
- Phase 2 before Phase 3: the Phase-3 mechanisms amplify whatever
  direction check-writing believes in — ground the direction first, or
  we amplify backwards rules.
- Phase 4 last: powerful but with unmeasured false-alarm risk (P4.1) or
  pure offline tooling (P4.2/P4.4) — never mixed into a run that is also
  measuring Phases 2–3.

---

## Dead ends — do not revisit without new evidence

- **Asking the prompt nicely to "explore beyond the seed input"**: tried
  twice (diag2, diagf), 3 false alarms each time — the instruction is
  ignored in practice.
- **Voting across a bug's several patches** ("if most patches behave the
  same way, trust that behavior"): repair tools tend to make the SAME
  mistake in all their patches for a bug, so agreement proves nothing.
  (Rule POOLING — P3.2 — shares the checks between patches, not the
  verdicts.)
- **Coverage-guided differential fuzzing for certification**: considered
  twice; every wrong "no difference" we ever found came from looking at
  the wrong OUTPUT, never from failing to find the right INPUT — P4.2
  fixes the actual cause.
- **Verifier majority voting**: measured; error rate unmoved at 3× the
  cost.
- **Spending effort on the unwinnable tasks**: Lang-7, Lang-22, Math-30,
  Math-59, Closure-115, Closure-123 (and the mislabeled correct sides of
  Lang-41 / Lang-10) are proven either behaviorally identical to the real
  fix in our environment or wrongly labeled — there is nothing to catch
  there.

## Standing predictions (falsifiable; check at each measurement gate)

**P0 gate outcome (2026-07-17, 5 legs, gpt-5.4, 350k tokens): 2 catches,
0 misses, 1 clean, 2 false alarms — both false alarms mechanistically
explained on the spot.**

**PHASE 2+3 COMBINED GATE (2026-07-17, p23gate, full dev set): NEGATIVE
RESULT — the interventions REGRESSED the pipeline.** Recall 0.56 → 0.43
(9/16 → 6/14); correct-side clean 13/14 → 8/12 (four false alarms).
Prior true-positives flipped to misses (Closure-33, Lang-41, Lang-60);
new false positives appeared (Closure-62, Closure-73, Lang-7); one gain
(Closure-73-o); Math-57 built no harness. Mechanism, confirmed on
Lang-60-o: at baseline it caught the overfit via its seed-anchored
lifted oracle; in the gate it received 3 synthesized + 3 POOLED
relations, fired on the buggy build via a generic `contains` oracle, and
lost the specific witness input that discriminates on the patched build.
This is EXACTLY the documented dead-end — "injected oracle mass
distracts the generator from the seed-anchored oracle" — which P3.2
pooling directly triggers by ADDING sibling-leg relations. The
strict-improvement fixes from this phase (JSON double-escape recovery,
numeric tolerance, the direction/heading corrections) are keepers; the
POOLING and the aggressive direction-confirmed RANKING that displaces
the working lifted oracle are the regressors. Next: revert/gate those,
re-measure. The deterministic probe still shows the Phase-2 direction
logic is individually correct — the failure is systemic (oracle mass),
not per-relation.

**PHASE-1 BASELINE (2026-07-17, full 30-leg dev set, gpt-5.4, ~2.5M
tokens across p1base + b + c): recall 9/16 = 56%, correct-side 13/14
clean (the one FP is the expected Chart-26-c flag pattern), positive-
prediction precision 9/10 = 90%. No unexpected false alarm anywhere.**
This is the number every later phase diffs against. Recall ≈ the old
pre-Phase-0 58% — as designed: Phase 0 bought TRUST (every miss now
explained, zero mystery FPs, full per-oracle attribution), not recall;
the recall gain is Phases 2–3. The 7 misses: Lang-50 & Math-2 (broad
divergence, discriminator generated-but-latent/stochastic), Lang-27
(crashing, lifted-crash-only), Math-53 & Closure-73 (narrow), Math-57
(witness, float-width), Time-11 (witness, cross-thread — the expected
permanent miss). The one unifying cause: the check that fired on buggy
was always the lifted seed, which the overfit special-cased. That is
precisely Phase 2's target, so the baseline validates the plan's
ordering without change.

1. The Chart-26 correct side flips false-alarm → clean from P0.3 alone.
   **PARTIALLY FALSIFIED at the P0 gate**: P0.3 DID mechanically drop the
   wrapped-crash variant (the historical FP), but a second harness
   smuggled the same pre-existing crash through as MESSAGE TEXT with the
   alarm thrown outside any catch (a "flag pattern" — no cause chain for
   P0.3 to see). That variant is exactly what P3.3's crash-site pinning
   addresses; until then Chart-26-c remains an expected false alarm.
2. The Math-2 correct side is genuinely clean now (file repaired + the
   check uses a tolerance). **CONFIRMED after one more P0-class fix**:
   the gate's Math-2-c "false alarm" was a PHANTOM — libFuzzer wrote a
   `slow-unit-*` artifact on a clean exit-0 run and the crash classifier
   counted "Test unit written to"/"artifact_prefix" as crash markers.
   Fixed (crash-* artifacts only); no real oracle fired on the patched
   build.
3. The Math-2 overfit side stays a miss until P3.2, then flips to a
   catch via the mean-formula rule. **RESOLVED with a twist: caught
   FLAKILY.** Math-2-o was a catch at the P0 gate and a miss at the
   baseline — a real flip-flop. The firing check reads `sample()`, a
   random draw, so the verdict depends on fuzzing luck. The spirit of
   the prediction holds — P3.2 is still needed — but not because it is a
   hard FN: because the deterministic discriminator (`getNumericalMean`
   = −49.76) is generated-but-latent, and P3.2/P2.2 must surface it to
   make the catch RELIABLE rather than lucky.
4. The patches with tiny behavior differences show the lowest catch rate
   before P3.1 and the largest improvement from it. **PARTIALLY BORNE
   OUT at the baseline**: the two narrow legs (Math-53: 3, Closure-73:
   7) are both misses — but so are two BROAD legs (Lang-50, Math-2), so
   narrowness is not the sole predictor of a miss; latent/stochastic
   discriminators miss regardless of breadth. The narrow legs remain the
   cleanest P3.1 targets.
5. The five wrongly-cleared patches flip to "difference found" under
   P4.2 with no prompt changes.
6. The witness-only overfits are all misses at the Phase-1 baseline, and
   P3.1's two new check types flip at least Lang-60 and Lang-41.
   **FALSIFIED — in the good direction.** The pipeline caught 5 of the 7
   witness-only overfits at baseline ALREADY (Chart-7, Lang-41, Lang-60,
   Closure-62, Closure-92); only Math-57 (float-width) and Time-11
   (cross-thread, expected) missed. "Witness-only" describes the
   certifier's single probe, not the pipeline's several diverse
   harnesses — the pipeline finds surfaces the one probe didn't. P3.1's
   remaining witness target is really just Math-57.
