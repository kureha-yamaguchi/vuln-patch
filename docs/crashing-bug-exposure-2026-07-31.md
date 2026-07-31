# Crashing bugs: what the semantic-bug cycles changed underneath them

For a reader who knows the codebase but did not follow the July semantic-bug work.

**The question:** seven cycles of work targeted semantic bugs. Roughly half the
pipeline is shared. No crashing-bug suite has run since 2026-07-16, which predates all
of it. What is exposed, and what needs checking?

**The answer in three lines.** One crashing leg was run through the current build on
2026-07-31 to find out. Three suspected risks are now confirmed safe; three remain
open; the run surfaced two problems that reading code had missed — a pipeline-wide
observability gap (since fixed) and an unfiltered exception-leak path that makes the
old crashing baseline unreliable. Nothing found is a confirmed regression.

**Verification note.** Every code claim here — gate locations, line numbers, which
module is kind-aware, constant values, log keys — was re-checked against source on
2026-07-31, after earlier drafts got a gate backwards and cited several wrong line
numbers. Line numbers are as of commit 6599404. Numbers carried over from prior runs are
marked. If you find a discrepancy, trust the code and fix this file.

---

## 1. The one difference everything follows from

**Semantic bug.** The buggy program runs to completion and returns a wrong *value*.
Nothing observable happens unless the harness supplies an expectation. A semantic
harness is therefore a comparison machine: call the API, establish what the answer must
be, compare, and on disagreement throw **our own alarm**, `FuzzerSecurityIssueLow`,
tagged `[oracle:<id>]`. These harnesses deliberately wrap API calls in broad `catch`
blocks that `return` — an exception means "this fuzzed input was invalid", not "the
patch is wrong". **Detection = our alarm escapes.**

**Crashing bug.** The buggy program throws. No expectation is needed; the JVM supplies
the event, and the harness's job is to *reach* the faulty state. **Detection = a
foreign exception escapes** and Jazzer reports it.

Two consequences explain most of this document:

- **The same measurement means opposite things.** "This fires identically on the buggy
  build too" means *no evidence about the patch* on a semantic leg, and *the patch
  failed to fix the bug* — the strongest possible catch — on a crashing leg.
- **A crashing verdict is decided mechanically**, usually with no LLM call at all,
  whereas a semantic verdict normally passes through the judge. So when something goes
  wrong on a crashing leg, there is no judge transcript to reconstruct from.

## 2. What runs, and what does not

**Skipped entirely on a crashing leg** — no risk, but also no benefit from work done on
them. Confirmed live: the trace run passed `--synthesize_relations` and
`--replay_relations_on_patched` and produced zero events from any of these.

| Component | Gate |
|---|---|
| Relation synthesis → screening → patched-side replay (the biggest semantic recall mechanism) | `run.py:1181` |
| Mined sibling-test oracles (also off by default) | `run.py:1093` |
| Semantic class-context assembly | `run.py:1039` |
| Lifted-assertion path + trigger-test-lift note | `run.py:1328` |
| Identical-drop ladder + `family_duty()` (calls at 2704, 2884) | `run.py:2647` |
| `classify_differential_replay()` — the INTRODUCED/PREEXISTING/SHADOWED classifier. It must not run here: every accepted harness reproduces the crash on buggy *by construction*, so "same crash on buggy" is the true-positive condition for a crashing bug, and the classifier would read every genuine catch as pre-existing surface. | `run.py:2006` |

**Crashing-only:** trigger-gate handling (`run.py:1065`), the `expected_exceptions`
list built from the failing tests' exception types (`1598`), and the defect-family
dismissal (`2519`).

**Shared — crashing legs run all of this**, with July rework flagged:

- **`harness/campaign.py`** — generation and acceptance. Contains **no `bug_kind`
  reference at all**, so it is fully kind-agnostic: the self-swallow lint, alarm-ID
  requirement, gate 0b (a caught-and-rethrown crash must carry its cause, line 538) and
  the family-novelty steering all apply. *Reworked:* novelty gate (new in July,
  extractor bug fixed 07-26); repair-in-place wired in ahead of the gates.
- **`harness/repair.py`** — *new 07-29/30.* Fixes three mechanical defects instead of
  discarding the attempt. 101 of 235 archived rejects cleared, 0 compile regressions
  over 111 pairs; live effect outcome-neutral, −2.3 attempts/leg. (Figures as reported
  in `docs/replay/`; per-leg attribution independently re-checked, offline counts not.)
  **All validation data came from semantic runs.**
- **`execution/oracle_mute.py`** — alarm muting and the diversion counter
  (`instrument_diversion`, 07-30), which distinguishes "the check did not fire" from
  "execution never reached it". *Reworked heavily.*
- **`execution/fuzz_runner.py`** — `replay_input_report`, `replay_input_muted`, the
  bounded `iterate_muted_replay` (07-30); `crash_signature` and `cause_signature`
  unchanged.
- **`relations/judge_decision.py`** — `adjudicate()`, the single judge entrypoint: base
  verify, then the citation/pinned-parameter re-ask lint, the intrinsic-rate drop at
  `INTRINSIC_FIRE_RATIO` = 0.95 (`evidence_facts.py:38`; escape via
  `_family_duty_escape`, `judge_decision.py:168`), and the terminal-identical gate.

---

## 3. Risk status

Evidence throughout: `runs-archive/runs/crashtrace1_20260731_141054/` — Lang-27
DeepRepair, one overfit leg, current standard config.

### Confirmed safe — observed live

1. **The semantic gates hold under the semantic flags** (see §2).
2. **Repair-in-place on crash-shaped harnesses.** Fired once (`attempt_008`,
   swallowed-alarm, `still_failing: []`); the harness compiled and was accepted, crash
   signatures unaffected. Consistent with the code: the repair rethrows *only*
   `FuzzerSecurityIssueLow`, leaving every other exception caught as before.
3. **Diversion instrumentation.** Ran three times (`instrumented=True` →
   `diverted=False`); its `[relscreen] skipped=` line did not disturb
   `crash_signature()`.

### Still open

1. **The defect-family dismissal (`run.py:2519`) — top risk, and the trace could not
   observe it.** Two harnesses fired on the patched build, the judge was called **zero**
   times, `crashed_on_patch` came out false — a mechanical drop decided the verdict and
   left no record. The likely path is this dismissal (both firings are metamorphic
   checks on observables unrelated to the defect), but that is inference, not
   observation. Its own logic is old and crash-specific; its *inputs* — the buggy-side
   and muted replays — were all reworked in July. A rerun now answers this directly,
   thanks to the fix below.
2. **`_real_test_passes = (bug_kind != 'crashing')` (`run.py:2440`).** A hard-coded
   kind-dependent input to evidence construction, in repeatedly-changed code.
   Unexercised.
3. **The intrinsic-rate and terminal-identical gates in `adjudicate()`.** No gate events
   appear in the trace, which is consistent with inertness but proves little — the judge
   was never called, so they had no opportunity. Pin with unit tests (§5).

### Found by the trace, not visible from reading code

4. **Outcome decisions were print-only pipeline-wide — FIXED (commit 6599404).** Nine
   sites changed a finding's outcome by direct assignment with only a `print`, and
   `run_suite.sh` deletes `run.log` on success. **Six of the nine are not kind-gated**,
   so this was never crashing-specific — it simply bit crashing legs hardest, for the
   reason in §1 (no judge transcript to fall back on). All mutations now route through
   `drop_finding()` / `flag_overfitting()`, which emit `outcome-drop` / `outcome-flag`
   events with a site tag; `tests/test_outcome_events.py` fails the build if a new
   direct site appears.
5. **Legitimate escaping exceptions are not filtered on the crashing path.** July's
   Lang-27 "catch" came from a harness accepted on an escaping `NumberFormatException`
   — the documented-correct behaviour of the method under test, i.e. a check that would
   fire on the developer fix too. The same leak mechanism is on record as the cause of
   Lang-27's historical *false accusation* on its correct patch. Recorded as an
   observation only; no filter is proposed here. Any future work must start from the
   asymmetry in §1 — a semantic filter cannot simply be un-gated — and needs its own
   evidence and guard set.

### Withdrawn after reading the code

- `repair_swallowed_alarm` was never a crash risk: it rethrows only our own alarm class.
- `repair_rethrow_without_cause` is *aligned* with the crashing design, not against it —
  it implements what gate 0b already requires (`campaign.py:538`) and what
  `cause_signature()` exists to read. Also observed harmless live.

### Where interference is structurally impossible

Stated positively, so a reader knows where *not* to look.

| Area | Why |
|---|---|
| Relation synthesis, screening, patched-side replay | Never executes (gated). Confirmed live. |
| Lifted-assertion oracle and its note | Gated; needs an `assertEquals` expected value a crash-shaped test does not provide. |
| Identical-drop ladder, `family_duty()` | Gated `bug_kind != 'crashing'`. |
| Mined sibling-test oracles | Gated *and* off by default. |
| `classify_differential_replay()` | Gated — see §2. |
| Fire-rate facts and the intrinsic-rate drop | Their inputs are relation-screening statistics, never produced for a crashing leg. (An argument, not an assertion — pin it, §5.) |
| Judge prompt-shaping: citation lint, pinned-parameter re-ask, disputed-computation fact | Only reachable from a judge call on a fired alarm; the crash-reproduction path is decided mechanically before any LLM call. |
| Cycle-5/6/7 fixture work — `cases228.jsonl`, the 67-catch guard set, `verifier_replay` | Offline artefacts; they never execute in a pipeline run of either kind. |

**Practical reading:** if a crashing result looks wrong, the semantic machinery is not
where to look. Look at harness generation and acceptance, the buggy-side replay, the
defect-family dismissal, and whether a legitimate escaping exception was scored as a
finding.

---

## 4. Measurement status

- **Last crashing suite:** `crashcheck` (2026-07-16, 14 legs,
  `runs-archive/runs/crashcheck_20260716_012430/`) — TP=6 FN=1 FP=1 TN=6, P=0.86
  R=0.86 F1=0.86. Predates all seven cycles. **Treat as SOFT:** its Lang-27 TP is the
  exception-leak case above, so at least one of the six catches is likely spurious. Not
  a bar to restore without re-adjudicating the individual catches.
- **Single-leg trace:** `runs-archive/runs/crashtrace1_20260731_141054/` — Lang-27 at
  current config, scored FN. Diagnosis: in *both* runs the harnesses probing the real
  defect were clean on the patched build (the patch fixes that path); July's TP came
  from an exception-leak harness shape this roll did not generate. So the July→now
  change is most likely the loss of a spurious mechanism, not a regression in the
  reworked machinery.
- **Suite file:** `suites/crash14.cases` was deleted in the 07-21 cleanup (`db38bd5`);
  recoverable from git history or rebuildable from the archived run's leg list.
- **Label caveat:** the crashing pool is largely uncertified — the July certification
  work covered the semantic pool. Weak per-leg differences are not evidence.

---

## 5. Recommended checks, cheapest first

1. **Two unit tests (free).** (a) The intrinsic-rate and terminal-identical gates return
   None / no-op on crash-leg evidence — turns "still open" item 3 from an argument into
   a pinned invariant. (b) A harness repaired by `repair_rethrow_without_cause` still
   yields the `cause_signature()` the pinning path expects.
2. **Rerun the single crashing leg (~150k).** Now that outcome decisions emit events,
   this answers what dropped Lang-27's two patched-build firings — the top open risk —
   without any archaeology.
3. **Rerun the 14-leg suite (~1–2M).** Rebuild `crash14.cases`, run on the current
   build, compare **per leg** against 07-16 (14 legs, ~7 points each — aggregates
   mislead), and re-adjudicate any catch that rests on an escaping exception which is
   legitimate behaviour. Do this before describing the pipeline's state as covering both
   bug kinds.

Not recommended: crashing-specific development before step 3 says whether anything
actually regressed. These checks price an unmeasured surface; they do not open a work
front.
