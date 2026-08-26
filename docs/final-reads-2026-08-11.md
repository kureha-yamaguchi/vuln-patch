# Final reads — 2026-08-11

Three desk reads against the archive. Analysis only: no runs launched,
nothing pushed to the VM, no fixes derived from holdout output.

**Stations this read targets**

| Station | Module | Why it is named here |
|---|---|---|
| verdict gates | `src/java/run.py` cycle-6 blocks, `evidence_facts.py` | 5 of 7 holdout FP convictions passed gates that abstained for parseability or bypass reasons (§1) |
| relation screening | `relation_screen.py` | tripwire-inversion relations screen as perfect tripwires (§1.1) |
| divergence capture | `src/java/execution/divcap.py`, `run.py::_record_divcap` | attribution of the first Lang-63 catch (§2) |
| valid-input probe | `run.py` harness-alarm track | present but structurally unreachable for its motivating cases (§2.3) |
| offline gate tests | `tests/test_read2_offline_gates.py` | growing-archive test re-pinned (§3) |

---

## 1. Holdout FP/FN class tally (plan 8.48)

Analyse-only, per the hard rule: findings for the writeup; no fix may be
derived from holdout output. All seven FP legs and both FN legs were
trace-read with verbatim anchors (four reader agents; anchors below are
`<leg dir>/trace.md:<line>` in `runs-archive/runs/holdout_20260811_115650/`).

### 1.1 The seven false positives — only ONE matches a documented class

| leg | class | one-line mechanism |
|---|---|---|
| Math-33 (CapGen) | **NEW: tripwire inversion** | relation re-encodes the PRE-patch predicate (`entry > 0.0`, no epsilon) at exactly the boundary the patch changed; silent on buggy (0/20000 → kept "silent on buggy (tripwire)"), fires 100% on the correct patch; `[fact:rate-catch-signal]` then argued FOR keeping it |
| Closure-115 (SimFix) | **NEW: tripwire inversion** | same shape: relations assert the patch's REMOVED guard as a contract; 0/20000 buggy → tripwire; 20000/20000 patched read as a "strong discrimination signal". Second, independent conviction in the leg is a generic-exception leak kept despite a `[differential replay]` fact showing the crash pre-exists on buggy (a contradictory `[buggy-replay fact]` won) |
| Math-71 (SimFix) | **NEW: prose-blind gates** (+ tolerance) | fired on BOTH builds (132/20000 buggy, kept "selective firer"); the fires-on-both evidence sat in a `[replay comparison fact]` PROSE block, but 6B/6C parse only `[fire-rate fact]`/`[fact:fires-on-both-confirmed]` tags → "the fire rate was never measured", gates abstained. Judge kept an absolute `1.0e-3` time tolerance at ~1e12 magnitude after dismissing the IDENTICAL defect twice in the same leg |
| Math-80 (CapGen) | **NEW: prose-blind gates** (+ NaN) | fires on both builds (29/20000 buggy); same tag-vs-prose blindness; the check's `Arrays.sort` parks NaN last so index 0 compares 0.0 vs -213.3 — the judge's WHY paraphrases the values with the NaN unaccounted |
| Time-19 (HDRepair) | **NEW: prose-blind gates** (+ rounding) | fired on both builds (17/20000 buggy, "selective firer"); the prose fact ROUNDED the buggy rate to "0%" in the same sentence stating it fires on both; judge kept it purely because the relation quotes a javadoc round-trip formula (pre-1912 LMT regime of Africa/Abidjan, genuine Joda behaviour on both builds) |
| Closure-31 (Jaid) | **NEW: message-less crash** | harness crash with NO oracle text on either build (`trigger: None`, empty `accepted_trigger_details`); no firing line reached the judge prompt, every mechanical guard read "nothing to measure", judge re-derived SOUND from the trusted test literal alone |
| Closure-57 (SimFix) | **valid-by-construction** (documented class) | relations splice fuzzer bytes into JS source (`"goog.require(" + ident + ")"`); reserved-word junk → parse fails → harness's own NPE reported as "unexpected NPE on valid-by-construction input". Kept via the DIRECTION-CONFIRMED BYPASS with `[fact:rate-indiscriminate]` stamped (buggy 20000/20000) — the exact doubly-flagged shape the disabled 8.43 reroute targets |

Aggregate: 2× tripwire inversion, 3× prose-blind gates, 1× message-less
crash, 1× valid-by-construction (the only previously-documented class;
Math-65-type formula convictions: ZERO). Cross-cutting mechanical facts
(verifiable on dev evidence independently): (a) the cycle-6 gates parse
only tagged fact blocks — in three legs the dismissing evidence existed
in prose and was invisible; (b) 5 of 7 kept convictions carry
`CITATION: NONE`, and no guard requires a citation for a KEEP; (c) all
seven arrive via `relation-replay-conviction` or an unattributed crash —
none via an attributed harness-oracle path.

### 1.2 The two false negatives

Both are the assembly's pre-flagged probe-false-zero legs (their
overfit-ness was itself invisible to the first certifier probe; neither
has a witness row in the 8.33 study — family calls are analogic).

- **Closure-18 (SequenceR): NEW gap family — connective-flip /
  config-flag combination.** Patch: `A && B` → `A || B` where the dev
  fix is `A` alone; the two differ only at `closurePass=true` with
  dependency management off. Every generated relation and harness set
  `options.closurePass = false` — copied from the trigger test's own
  setup — collapsing `A||B` to `A`: the patched build is extensionally
  identical to the dev fix on 100% of generated inputs. The pipeline
  even invented the almost-right relation
  (`disabled_dependency_sorting_preserves_given_order`) and neutered it
  with that flag.
- **Math-50 (HDRepair): family 3 (specific numeric content), the Math-85
  shape verbatim.** The near-miss relation replays the trigger's exact
  tuple `solve(3624, exp(x)-π³, 1.0, 10.0)` — the one input the
  plausible patch is built to satisfy; the divergence lives on other
  (function, bracket, budget) triples (1218/4800 in the certifier grid).
  The only fuzzer-varied solver call discards its result inside
  `catch (Throwable ignored)`.

Cross-leg: neither miss is an invention failure — both invented
near-miss checks and neutralized them by inheriting a SETTING from the
trigger test (a boolean flag; an argument tuple). That is a sharper
statement of the miss mechanism than the gap-family taxonomy had.

---

## 2. Divcap attribution (plan 8.47 — this section CORRECTS it)

### 2.1 Divcap ENGAGED in all 5 legs (8.47's "flag path did not engage" hypothesis was wrong)

The 8.47 greps looked for `[divobs]` — an in-JVM stderr marker consumed
by a Python parser (`divcap.py:697/:715/:84`), never a trace string. The
real trace surface is the `## [3] ⚙️ divcap · diff-boundary observation`
event (`run.py::_record_divcap`), present in every leg of
`divcap_roll_20260811_111023`:

    01/02/03 Lang-63: "0 divergence(s) from 7 buggy / 7 patched observation(s) — ok"
    04 Chart-7:       "1 divergence(s) from 15 buggy / 15 patched — ok"  → 1 <divergences> block in the synthesis prompt
    05 Chart-26:      "0 divergence(s) from 0 buggy / 18 patched — ok"

Two degradations flagged: leg 05's 0-buggy/18-patched asymmetry still
reports "ok" (a silent-failure hole in the status vocabulary), and the
anti-anchoring lint never fired anywhere (nothing anchored — inert, not
validated).

### 2.2 The first-ever Lang-63 catch is INVENTION LOTTERY, not divcap

Leg 03's conviction: `formatDurationHMS_shape` fired 5732/20000 fuzzed
("output not in H:m:s.S shape: -1054614901:22:10.058"), judged SOUND,
flagged via `relation-replay-conviction`. Divcap's plan nominated ONE
method (`reduceAndCorrect`, observable `ret`) and captured ZERO
divergences — the design doc's predicted failure mode (trigger-region
inputs don't expose the divergence) happened exactly — so the synthesis
prompt was byte-identical to a no-divcap run on all three Lang-63 legs.
The convicting relation targets a DIFFERENT method than the nomination.
The three legs differ only in what the LLM happened to write: leg 01
invented no HMS-shape relation; leg 02 invented two and both stayed
quiet; leg 03's phrasing caught the negative-hours output.

### 2.3 The valid-input probe was present but STRUCTURALLY UNREACHABLE

Git ancestry proves the roll's code (git_sha a455f59) postdates the
probe build (b197ec1, its direct parent). Zero `valid-input-probe`
events in any leg — for a structural reason: the probe is wired only
into the HARNESS-alarm track (`run.py:3966`), while both motivating
Chart FP convictions travel the RELATION-replay track (separate loop,
separate adjudicate). Leg 05's tier-2 firing matches `TIER2_MARK`
exactly but arrived via `replay-on-patched`, which the probe cannot
see. The G-V3 offline PASS exercised the pure `evidence_facts` helpers,
not the live wiring, so it could not catch this. (Dev-leg evidence — a
wiring fix is legitimately actionable, unlike holdout findings.)

---

## 3. G-R2 safety test re-pin, and what it was flagging (finding)

**Target:** offline gate tests, `tests/test_read2_offline_gates.py`
(`test_GR2_the_exemption_touches_no_other_archived_admission`).
**Failure mode:** the test's claim is about the archive as it stood when
the read-2 fix was written, but its glob read the GROWING archive, so a
run made after the fix went live broke it for the expected reason, not a
real one.

### 3.1 What the test claims, and why it started failing

The test replays the read-2 exemption ("the patch-touched observable is
not screened on") over every ADMITTED reference in the archive and
asserts the exemption touches none of them — the safety half of G-R2:
the fix cannot delete any admission that already existed. That claim is
about admissions made WITHOUT the exemption. `p1b_live2_20260811_023425`
ran WITH the exemption live (it was the fix's own confirmation roll), so
its admissions carry exempted surfaces by design, and the test tripped
on the first one:

    AssertionError: ('/Users/hannafoerster/Desktop/code/vuln-patch/runs-archive/runs/p1b_live2_20260811_023425/02_patch1-Math-65-CapGen_c/trace.md', 'getJacobianEvaluations', ['getChiSquare'])

### 3.2 The flagged admission, read from the trace (the finding)

The flagged row is not a safety violation — it is the exemption doing
exactly what it was built to do, on a run that postdates the build.
In `p1b_live2_20260811_023425/02_patch1-Math-65-CapGen_c/trace.md`,
section `## [200] ⚙️ reference-impl · getJacobianEvaluations`
(lines 15445-15448) resolves the screening surface with the exemption
engaged, verbatim:

    - reason: 6 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free); 1 of them PATCH-TOUCHED (['getChiSquare']) and therefore not screened on — the buggy build is the wrong answer key where the defect lives
    - detail: {'siblings': ['getChiSquare', 'getCovariances', 'getEvaluations', 'getIterations', 'getRMS', 'guessParametersErrors'], 'patch_touched_exempted': ['getChiSquare'], 'screened_siblings': ['getCovariances', 'getEvaluations', 'getIterations', 'getRMS', 'guessParametersErrors'], ...}

and the reference is then ADMITTED anyway, on the five remaining
siblings (line 15493-15495, section `## [212]`):

    **output:** **screen ADMITTED**
    - reason: reference reproduces the buggy build on 5 off-defect observable(s) (1 of them defect-reached and re-graded against the failing test's own asserted value, which the reference matches and the buggy build fails: ['guessParametersErrors'])
    - detail: {'construct': 'OK', 'off_defect_shared': 5, 'off_defect_exempted': ['getChiSquare']}

then stored (line 15510): `admission STORED for observable
jacobianevaluations` — the leg holds `['chisquare', 'jacobianevaluations']`,
matching 8.46's live count. So the finding, stated plainly: exempting
`getChiSquare` did NOT delete this admission; the reference cleared the
count bar on 5 shared off-defect observables without it. The pre-fix
safety claim stays true on the pre-fix archive, and the first post-fix
data point shows the exemption engaging without costing an admission.
A sweep over the whole archive found exactly ONE admission the exemption
touches — this one; the two later runs (`divcap_roll_20260811_111023`,
`holdout_20260811_115650`) contribute none.

### 3.3 The re-pin

The test now iterates `_pre_fix_traces()`: run dirs whose
`_YYYYMMDD_HHMMSS` suffix is `>= 20260811_023425` (p1b_live2 and
everything after) are excluded; undated dirs all predate the cutoff and
are kept. Future runs always get a later stamp via `run_suite.sh`, so
the test's universe is now fixed. Full suite after the change:

    1097 passed, 7 skipped in 11.52s
