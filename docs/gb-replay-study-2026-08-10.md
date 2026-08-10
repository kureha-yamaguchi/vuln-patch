# G-B replay study — the bypass qualifier against the archive (2026-08-10)

Runs the pre-registered gates G-B1 / G-B2 / G-B3 from
`docs/math65-formula-read-2026-08-10.md` ("Pre-registration round 2") against
seven archived suites, using the build committed as `d2483db` and described in
`docs/bypass-qualifier-build-2026-08-10.md`. Everything here was done offline on
the Mac. The VM was not touched and nothing was read from it.

## Stations this study targets

| Station | Module | Target / Failure mode |
|---|---|---|
| Judge decision — the terminal gate ladder | `src/java/relations/judge_decision.py` (`direction_confirmed_bypass`, `adjudicate`) | **Target:** measure what the qualified bypass does to already-recorded convictions. **Failure mode under test:** the direction-confirmed exemption skipped 5C/6B/6C unconditionally, so a firing that also measured rate-indiscriminate never met a value gate. |
| Offline verifier replay | `src/java/verifier_replay.py` (drives `adjudicate`) | **Target:** re-judge archived firings without a build or a fuzz run. **Failure mode under test:** whether re-routing a doubly-flagged firing through the value path converts false convictions and whether it costs any genuine catch. |
| Buggy-side replay — the isolation hook | `src/java/run.py` ~3490-3600 | **Target:** count the archived baseline the re-aim is meant to move. Only the baseline is measurable here; the re-aim itself needs a live leg. |

## What was enumerated, and how

Suites read (all under `runs-archive/runs/`):
`varbase_20260808_183839`, `mechb_roll_20260809_115543`,
`stack_confirm_20260810_140852`, `gs1_isolation_20260810_175503`,
`full30v3_20260810_154612`, `diffcov_reach_20260808_233005`,
`invdiv_20260808_203424`.

- 82 leg directories in those seven suites.
- **282** `cycle6_gates_entry` events, across 61 legs. **124** carry a
  `relation …` target (the replay track, `run.py:4536`, the only site that
  passes `is_direction_confirmed`); **158** carry an oracle id (the harness
  track, `run.py:4135`, which never passes the flag).
- **0 of the 158** harness-track entries say `skipped`. Build A's reach is the
  replay track and nothing else, confirmed by measurement rather than by
  reading the call sites.
- **75** of the 124 replay-track firings ended as convictions — they appear in
  the leg's `relation_replay_kept` and in its `outcome-flag ·
  relation-replay-conviction` detail. They sit across **36 legs**: 53 on
  overfitting-patch legs (`_o`), 22 on correct-patch legs (`_c`).

Every conviction's judge-time evidence was recovered from the
`verifier / judge` LLM block that immediately precedes its gate entry (all 36
doubly-flagged ones sit exactly one step after their judge call). Twelve
convictions have **no recoverable judge block at all** — the leg recorded no
verifier LLM call for that firing (four in `stack_confirm` leg 07, four in leg
09, one in leg 08, three in `diffcov_reach` leg 03, all Lang-41). Eleven of the
twelve carry an archived gate entry of `running`, which proves they were not
direction-confirmed and puts them out of build A's reach whatever their
evidence said. The twelfth (`stack_confirm` leg 08,
`shortClassName_class_matches_shortCanonical_on_canonical_name`) shows
`skipped`, so it was on the bypass, but its rate-tag status **cannot be
determined** and it is reported as undetermined rather than assumed.

### The three routing classes

| class | count | what build A does |
|---|---|---|
| doubly-flagged (direction-confirmed AND `[fact:rate-indiscriminate]`) | **36** | reroutes through 5C → 6B → 6C |
| bypass-only (direction-confirmed, no rate tag) | **4** (1 of them undetermined) | unchanged, still skips the gates |
| gates already ran (not direction-confirmed) | **35** | unchanged, out of reach by construction |

The 36 doubly-flagged split **17 on correct-patch legs (the G-B1 population)**
and **19 on overfitting-patch legs (the G-B2 population)**, over 23 legs. By
bug: Math-65 ×17 (all on `_c` legs), Chart-19 ×17, Chart-7 ×1, Lang-50 ×1.

## How the reroute was actually exercised

`verifier_replay.py` calls `adjudicate` **without** `is_direction_confirmed`
(line 254), so the flag defaults to `False`. This was checked before the run,
as instructed, and it matters:

* With the flag `False`, `direction_confirmed_bypass` returns `(False, None)`
  and the gates run.
* With the flag `True` **and** a rate-indiscriminate blob — the doubly-flagged
  case — it returns `(False, <rerouted reason>)` and the gates run.

`adjudicate` uses the returned `bypass` in exactly two places: the
`cycle6_gates_entry` audit event's wording, and `if not _bypass:` around the
gate loop (`judge_decision.py:534-547`). The reason string changes; the
executed decision does not. So the replay reproduces the rerouted path for a
doubly-flagged firing **byte for byte** in everything that can change a
verdict, and differs only in an audit string. **No case was adapted and no
shipped file was edited.** The cases do carry an `is_direction_confirmed: true`
field as provenance; `verifier_replay` ignores it.

The rerouting branch of the predicate itself was then exercised directly,
offline and with zero LLM calls, by calling
`direction_confirmed_bypass(is_direction_confirmed, evidence)` on every one of
the 75 convictions with the flag set to what its archived gate entry proves it
was:

- **36 / 36** doubly-flagged → `bypass = False`, reason beginning
  `direction-confirmed AND [fact:rate-indiscriminate] — the bypass does NOT
  apply: …`. The reroute fires on exactly the population enumerated.
- **4 / 4** bypass-only → `bypass = True`, shipped wording unchanged.
- 35 not direction-confirmed → `(False, None)`, untouched.

### Which gate could act, computed before the run

Deterministically, from each doubly-flagged blob (`indiscriminate_rate_diagnosis`,
`confirmed_fires_on_both_verdict`, `terminal_profile`):

- **6C: `None` on all 36.** Not one conviction in the entire archive carries a
  `[fact:fires-on-both-confirmed]` tag, so 6C can never act on this population.
- **5C: `None` on all 36.** No blob reads as identical-on-both.
- **6B:** the only gate with anything to act on. On the 17 correct-patch
  convictions the measured buggy-side rates are
  0.40385, 0.4052, 0.45565, 0.4591, 0.46145, 0.51005, 0.52285, 0.5235, 0.52415,
  0.5344, 0.6813, 0.74255, 0.76955, 0.77805, 0.8243, 0.8262, 0.90595 — **every
  one below the 0.95 bar**. On the 19 overfitting-patch convictions:
  0.45685, 0.54145, 0.57775, 0.58095, 0.6111, 0.6987, 0.7207, and **eleven at
  1.0**.

So before a single token was spent the arithmetic already said: the reroute has
no gate that can act on any G-B1 conviction, and it hands eleven G-B2
convictions to a gate that drops unless family duty answers YES.

## The replay

```
uv run python java/verifier_replay.py \
  --cases runs-archive/runs/vreplay_gb_20260810/gb_cases.jsonl \
  --out   runs-archive/runs/vreplay_gb_20260810/out \
  --repeats 3 --votes 1
```

36 cases × 3 repeats = 108 adjudications. Case fields come from the archived
judge block: `harness_source` = `<harness>`, `concrete_evidence` = `<evidence>`,
`code_context` = `<codebase_context>`, `fired_assertion` = the "assertion that
ACTUALLY fired" line, `trusted_values` = the lifted-values list,
`failing_test` = the `[REAL FAILING TEST …]` tail of the evidence.
`fd_prior` is `null` for every case, which is faithful and not a
reconstruction: the replay-track site in `run.py` passes `fd_prior=None`
unconditionally, and every archived gate entry that ran the gates confirms it
in its own reason text. No case landed in the unresolved-ladder bucket
(`scored: 36 cases (0 excluded — unresolved ladder, below)`).

Verbatim from `runs-archive/runs/vreplay_gb_20260810/out/summary.md`:

```
**OVER-KILL rate (true findings dropped): 4/57 = 7%**
**LEAK rate (false findings kept): 46/51 = 90%**

Tokens: 1,483,192 total (1,413,619 in + 69,573 out, 143 calls)
By model: {"gpt-5.4": {"prompt_tokens": 1413619, "completion_tokens": 69573, "total_tokens": 1483192, "calls": 143}}
```

## Per-conviction table

Columns: the archived `cycle6_gates_entry` output; whether the evidence carries
the literal `[fact:rate-indiscriminate]` tag; the measured buggy/patched fire
percentages from the `[fire-rate fact]` line; the three gates' deterministic
inputs; and for the rerouted population, the replay's keep count over 3 repeats
and which stage dropped it (`6B` = `[6B-INDISCRIMINATE-DROP]`, `base` = the
base soundness verdict, which the reroute does not touch).

### Class 1 — doubly-flagged (36): direction-confirmed AND `[fact:rate-indiscriminate]`

| suite | leg | bench | label | relation | archived gate entry | rate tag | buggy% | patched% | 6B state | 6C verdict | 5C profile | replay kept | drop by |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| varbase | 01 | Math-65 | correct | `chiSquare_matches_weighted_residual_sum` | skipped | yes | 78 | 44 | below-bar (0.77805) | None | None | 3/3 | - |
| varbase | 03 | Math-65 | correct | `chiSquare_matches_weighted_residual_formula` | skipped | yes | 68 | 27 | below-bar (0.6813) | None | None | 2/3 | base |
| mechb | 07 | Math-65 | correct | `chiSquare_matches_weighted_residual_formula` | skipped | yes | 82 | 46 | below-bar (0.8243) | None | None | 3/3 | - |
| mechb | 08 | Math-65 | correct | `chiSquare_matches_weighted_squared_residuals` | skipped | yes | 91 | 39 | below-bar (0.90595) | None | None | 3/3 | - |
| mechb | 08 | Math-65 | correct | `rms_matches_documented_weighted_residual_formula vi` | skipped | yes | 46 | 51 | below-bar (0.45565) | None | None | 3/3 | - |
| mechb | 09 | Math-65 | correct | `rms_matches_weighted_residual_mean` | skipped | yes | 46 | 48 | below-bar (0.4591) | None | None | 3/3 | - |
| stack | 04 | Math-65 | correct | `chiSquare_matches_documented_sum_over_inverse_weigh` | skipped | yes | 52 | 86 | below-bar (0.5235) | None | None | 0/3 | base |
| stack | 04 | Math-65 | correct | `rms_matches_weighted_residual_mean_square` | skipped | yes | 52 | 57 | below-bar (0.52415) | None | None | 3/3 | - |
| stack | 05 | Math-65 | correct | `chiSquare_matches_documented_inverse_weight_formula` | skipped | yes | 53 | 78 | below-bar (0.5344) | None | None | 3/3 | - |
| stack | 05 | Math-65 | correct | `rms_matches_documented_weighted_residual_formula vi` | skipped | yes | 41 | 40 | below-bar (0.4052) | None | None | 3/3 | - |
| stack | 05 | Math-65 | correct | `rms_matches_weighted_residual_mean` | skipped | yes | 52 | 58 | below-bar (0.52285) | None | None | 3/3 | - |
| stack | 06 | Math-65 | correct | `chiSquare_matches_weighted_squared_residuals` | skipped | yes | 74 | 46 | below-bar (0.74255) | None | None | 3/3 | - |
| stack | 06 | Math-65 | correct | `rms_matches_documented_mean_of_weighted_squares vio` | skipped | yes | 40 | 46 | below-bar (0.40385) | None | None | 3/3 | - |
| gs1 | 03 | Math-65 | correct | `chiSquare_matches_weighted_residual_formula` | skipped | yes | 83 | 49 | below-bar (0.8262) | None | None | 2/3 | base |
| gs1 | 03 | Math-65 | correct | `rms_matches_root_mean_weighted_residual_square` | skipped | yes | 51 | 46 | below-bar (0.51005) | None | None | 3/3 | - |
| invdiv | 01 | Math-65 | correct | `rms_matches_documented_mean_weighted_square_formula` | skipped | yes | 46 | 47 | below-bar (0.46145) | None | None | 3/3 | - |
| invdiv | 02 | Math-65 | correct | `chiSquare_matches_weighted_residual_sum` | skipped | yes | 77 | 60 | below-bar (0.76955) | None | None | 3/3 | - |
| varbase | 10 | Chart-19 | overfitting | `categoryplot-null-rangeaxis-rejected-independent-of` | skipped | yes | 100 | 36 | at-or-above-bar (1.0) | None | None | 1/3 | 6B |
| varbase | 12 | Chart-19 | overfitting | `categoryplot-nullRangeAxisProbe-throwsIAE` | skipped | yes | 100 | 42 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| mechb | 01 | Chart-19 | overfitting | `categoryPlot-getRangeAxisIndex-null-always-throws-I` | skipped | yes | 100 | 23 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| mechb | 01 | Chart-19 | overfitting | `categoryPlot-null-rangeAxis-rejected-in-all-states ` | skipped | yes | 100 | 44 | at-or-above-bar (1.0) | None | None | 2/3 | base |
| mechb | 01 | Chart-19 | overfitting | `indexOf-null-absent-is-minus-one` | skipped | yes | 61 | 100 | below-bar (0.6111) | None | None | 3/3 | - |
| mechb | 03 | Chart-19 | overfitting | `categoryplot-null-range-axis-index-throws-after-mut` | skipped | yes | 100 | 53 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| mechb | 03 | Chart-19 | overfitting | `categoryplot-range-axis-reference-and-null-rejectio` | skipped | yes | 100 | 46 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| stack | 01 | Chart-19 | overfitting | `categoryplot_getRangeAxisIndex_null_always_throws_a` | skipped | yes | 100 | 40 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| stack | 01 | Chart-19 | overfitting | `indexOf-null-absent-returns-minus1` | skipped | yes | 46 | 100 | below-bar (0.45685) | None | None | 3/3 | - |
| stack | 01 | Chart-19 | overfitting | `objectlist_indexof_null_absent_returns_minus1` | skipped | yes | 58 | 100 | below-bar (0.58095) | None | None | 3/3 | - |
| stack | 02 | Chart-19 | overfitting | `categoryplot-range-axis-index-null-always-illegal v` | skipped | yes | 100 | 50 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| stack | 02 | Chart-19 | overfitting | `objectlist-null-absent-is-minus-one` | skipped | yes | 54 | 100 | below-bar (0.54145) | None | None | 3/3 | - |
| stack | 03 | Chart-19 | overfitting | `objectlist-null-absent-index-minus-one` | skipped | yes | 58 | 100 | below-bar (0.57775) | None | None | 3/3 | - |
| full30v3 | 04 | Chart-7 | overfitting | `clone_preserves_maxMiddleIndex` | skipped | yes | 72 | 71 | below-bar (0.7207) | None | None | 2/3 | base |
| full30v3 | 19 | Lang-50 | overfitting | `dateInstance_default_overload_agrees_with_explicit_` | skipped | yes | 70 | 100 | below-bar (0.6987) | None | None | 3/3 | - |
| diffcov | 01 | Chart-19 | overfitting | `categoryplot-range-axis-index-null-throws-independe` | skipped | yes | 100 | 37 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| invdiv | 04 | Chart-19 | overfitting | `categoryplot_getRangeAxisIndex_null_rejected_indepe` | skipped | yes | 100 | 50 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| invdiv | 06 | Chart-19 | overfitting | `categoryplot-getRangeAxisIndex-null-always-throws v` | skipped | yes | 100 | 35 | at-or-above-bar (1.0) | None | None | 3/3 | - |
| invdiv | 06 | Chart-19 | overfitting | `categoryplot-getrangeaxisindex-null-throws` | skipped | yes | 100 | 25 | at-or-above-bar (1.0) | None | None | 3/3 | - |

### Class 2 — bypass-only (4): direction-confirmed, no rate tag — untouched by build A

| suite | leg | bench | label | relation | archived gate entry | rate tag | buggy% | patched% | 6B state | 6C verdict | 5C profile |
|---|---|---|---|---|---|---|---|---|---|---|---|
| stack | 02 | Chart-19 | overfitting | `objectlist-indexof-absent-readonly` | skipped | no | - | - | no-measurement (None) | None | None |
| stack | 02 | Chart-19 | overfitting | `objectlist-indexof-inserted-reference-roundtrip vio` | skipped | no | - | - | no-measurement (None) | None | None |
| stack | 08 | Lang-41 | overfitting | `packageName_class_matches_packageCanonical_on_canon` | skipped | no | 38 | - | below-bar (0.3764) | None | None |
| stack | 08 | Lang-41 | overfitting | `shortClassName_class_matches_shortCanonical_on_cano` | skipped | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |

### Class 3 — gates already ran (35): not direction-confirmed — out of build A's reach by construction

| suite | leg | bench | label | relation | archived gate entry | rate tag | buggy% | patched% | 6B state | 6C verdict | 5C profile |
|---|---|---|---|---|---|---|---|---|---|---|---|
| varbase | 01 | Math-65 | correct | `chiSquare_is_inverse_in_uniform_weight_scale` | running | no | 0 | 93 | below-bar (0.0) | None | None |
| varbase | 01 | Math-65 | correct | `chiSquare_matches_documented_formula` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| gs1 | 05 | Chart-26 | correct | `jfreechart_draw_acceptsNullInfo` | running | no | - | - | no-measurement (None) | None | None |
| full30v3 | 03 | Chart-7 | correct | `clone_preserves_series_observables` | running | yes | 62 | 60 | below-bar (0.6194) | None | None |
| full30v3 | 05 | Chart-26 | correct | `draw-does-not-mutate-axis-appearance` | running | no | - | - | no-measurement (None) | None | None |
| mechb | 01 | Chart-19 | overfitting | `objectList-null-absent-returns-minus1` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| mechb | 02 | Chart-19 | overfitting | `indexOf-null-absent-returns-minus1` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| mechb | 02 | Chart-19 | overfitting | `objectlist-indexof-null-absent-is-minus1` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| stack | 02 | Chart-19 | overfitting | `objectlist-indexof-null-empty-minus-one` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| stack | 03 | Chart-19 | overfitting | `objectlist-indexof-null-absent-is-minus-one` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| stack | 07 | Lang-41 | overfitting | `packageName_class_string_overload_agree` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 07 | Lang-41 | overfitting | `packageName_class_string_overloads_agree` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 07 | Lang-41 | overfitting | `shortClassName_class_string_overload_agree` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 07 | Lang-41 | overfitting | `shortClassName_class_string_overloads_agree` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 09 | Lang-41 | overfitting | `packageName-class-vs-jvmName-array` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 09 | Lang-41 | overfitting | `packageName_class_matches_string_overload_on_array_` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 09 | Lang-41 | overfitting | `shortClassName-class-vs-jvmName-array` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| stack | 09 | Lang-41 | overfitting | `shortClassName_class_matches_string_overload_on_arr` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| full30v3 | 06 | Chart-26 | overfitting | `chart_draw_with_null_info_does_not_throw` | running | no | - | - | no-measurement (None) | None | None |
| full30v3 | 06 | Chart-26 | overfitting | `drawLabel_adds_axis_label_entity_when_info_collects` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 06 | Chart-26 | overfitting | `drawLabel_adds_axis_label_entity_when_plot_info_col` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 12 | Closure-73 | overfitting | `escape-0x1f-boundary` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 19 | Lang-50 | overfitting | `dateInstance_defaultLocale_overload_agreement` | running | yes | 41 | 100 | below-bar (0.4088) | None | None |
| full30v3 | 25 | Math-53 | overfitting | `add-null-rhs-throws-nullargumentexception` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 29 | Time-4 | overfitting | `minPartialValuesAgreesNoArg` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 29 | Time-4 | overfitting | `minPartialValuesAgreesPartialOnly` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 29 | Time-4 | overfitting | `minPartialValuesAgreesWithMinPartial` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 29 | Time-4 | overfitting | `minPartialValuesAgreesWithNoArgMin` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 29 | Time-4 | overfitting | `minPartialValuesAlwaysOne` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| full30v3 | 29 | Time-4 | overfitting | `minPartialValuesMatchesSiblingOverload` | running | no | 0 | 100 | below-bar (0.0) | None | None |
| diffcov | 03 | Lang-41 | overfitting | `packageName_class_matches_string_overload` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| diffcov | 03 | Lang-41 | overfitting | `packageName_class_string_overload_agree_on_class_na` | running | no | - | - | no-measurement (None) | None | None |
| diffcov | 03 | Lang-41 | overfitting | `shortClassName_class_matches_string_overload_for_ar` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| diffcov | 03 | Lang-41 | overfitting | `shortClassName_class_string_overload_agree_on_class` | running | *no judge block* | *n/a* | *n/a* | *n/a* | *n/a* | *n/a* |
| invdiv | 07 | Lang-63 | overfitting | `positive_period_month_field_is_not_negative` | running | no | - | - | no-measurement (None) | None | None |

## G-B1 — do the archived false convictions convert? **NO. 0 of 17.**

Pre-registered prediction: "convert to dismissal in the majority … prediction:
all whose archived evidence carries both facts convert."

Measured: **0 of the 17** doubly-flagged correct-patch convictions was
dismissed by any gate, in any of the three repeats. Not one
`[6B-INDISCRIMINATE-DROP]`, `[6C-FIRES-ON-BOTH-DROP]` or terminal-identical
drop appears anywhere in the 51 correct-patch adjudications. The leak rate over
the rerouted population is **46/51 = 90%**.

Per leg (kept over 3 repeats; every drop below is a base-verdict flip, not a
gate):

| suite | leg | convictions | kept 3/3 | kept 2/3 | kept 0/3 | dismissed by a gate |
|---|---|---|---|---|---|---|
| varbase | 01 | 1 | 1 | 0 | 0 | 0 |
| varbase | 03 | 1 | 0 | 1 | 0 | 0 |
| mechb | 07 | 1 | 1 | 0 | 0 | 0 |
| mechb | 08 | 2 | 2 | 0 | 0 | 0 |
| mechb | 09 | 1 | 1 | 0 | 0 | 0 |
| stack | 04 | 2 | 1 | 0 | 1 | 0 |
| stack | 05 | 3 | 3 | 0 | 0 | 0 |
| stack | 06 | 2 | 2 | 0 | 0 | 0 |
| gs1 | 03 | 2 | 1 | 1 | 0 | 0 |
| invdiv | 01 | 1 | 1 | 0 | 0 | 0 |
| invdiv | 02 | 1 | 1 | 0 | 0 | 0 |
| **total** | | **17** | **14** | **2** | **1** | **0** |

The one conviction that came back dismissed in all three repeats
(`stack_confirm` leg 04, `chiSquare_matches_documented_sum_over_inverse_weights`)
was dismissed by the base soundness verdict — the judge simply read the check
differently this time. That is judge variance, present with or without the
reroute, and it must not be scored as a conversion. Two more
(`varbase` 03, `gs1` 03) flipped in one repeat of three, same cause.

This is not a surprise and it is not a threshold that wants nudging. It is the
shortfall the build note named in advance: **rerouting alone converts nothing.**
The Math-65 false convictions carry no value fact for a value gate to read —
6C's confirmation tag is absent from all 36, 5C's identical-on-both reading is
absent from all 36, and every measured buggy rate is below 6B's bar, the closest
being 0.90595 against a 0.95 bar. The gates ran, found nothing to weigh, and
left the verdict alone. The leak is real and the reroute opened the door to it,
but there is nothing standing behind the door for this bug.

Two enumeration corrections to the pre-registration's own leg list, both worth
recording:

* It names "gs1 01-03". Only **gs1 leg 03** has relation-replay convictions.
  Legs 01 and 02 are false alarms too, but their convictions come from the
  **harness track**, which never passes `is_direction_confirmed` and is
  therefore outside build A entirely. Both legs do contain doubly-flagged
  replay-track *firings* (leg 01 two, leg 02 three) — all dropped by the base
  verdict before any gate, so none is a conviction.
* It names "varbase 01 ×2 relations". Leg 01 has **three** convictions, of
  which **one** is doubly-flagged; the other two ran the gates already
  (buggy-side rate 0%, `[fact:rate-catch-signal]`).

Counting every archived Math-65 correct-patch leg across the seven suites: 16
legs, 14 false alarms, of which 11 convict via the replay track and 3 via the
harness track only.

## G-B2 — is any genuine catch lost? **YES. One, and it takes its whole leg with it.**

**The loss, verbatim.** `varbase_20260808_183839` leg 10
(`patch1-Chart-19-Arja-plausible_o`), relation
`categoryplot-null-rangeaxis-rejected-independent-of-state`, measured buggy
20000/20000 = 100%, kept **1 of 3** repeats. Repeats 0 and 1:

> INDISCRIMINATE-RATE TERMINAL [6B-INDISCRIMINATE-DROP] (family-duty NO): this
> check condemns the KNOWN-BROKEN build on 100% of random valid inputs, so the
> behaviour it reports pre-dates the patch, and a focused review found the
> violated property is NOT the failing test's own observable. The failing test
> only establishes that `getRangeAxisIndex(null)` must throw on a plot after
> `setRangeAxis(1, ...)`, while this check additionally requires the same on a
> separate fresh plot and thus is not solely the very observable shown wrong by
> the test.

and

> … The real failing test fixes only that `getRangeAxisIndex(null)` must throw
> on the plot state after its specific setup (including adding a secondary
> range axis), while this check also treats rejection on a separate fresh-state
> receiver as part of the violation, so the firing is not guaranteed to be the
> very same observable.

Repeat 2 kept it, family duty never reached.

That relation is the leg's **only** conviction, and the leg's only
`outcome-flag` is `relation-replay-conviction` with `kept:
['categoryplot-null-rangeaxis-rejected-independent-of-state']`. So in two runs
of three the reroute turns an OVERFIT CAUGHT leg into a miss. This is the
redesign signal the pre-registration asked for, not a threshold to tune: the
family-duty escape was asked the right question with the right failing-test
block and answered NO twice on a check whose subject *is* the failing test's
observable, differing only in also asserting it on a fresh receiver.

The escape's fragility is visible in the same table. `varbase` leg 12's
`categoryplot-nullRangeAxisProbe-throwsIAE` — same bug, same 100% rate, same
failing test — survived 3/3. Ten of the eleven doubly-flagged convictions at or
above the bar survived every repeat via family-duty YES. The escape works;
it is not reliable.

Full G-B2 accounting over the 19 doubly-flagged overfitting-patch convictions:

- **lost to a gate in at least one repeat: 1 / 19** (`varbase` 10, above).
- **lost to a gate in all three repeats: 0 / 19.**
- **at or above 6B's bar (the population actually at risk): 11 / 19**; 10 of
  the 11 survived 3/3 on family-duty YES.
- three further single-repeat drops (`mechb` 01
  `categoryPlot-null-rangeAxis-rejected-in-all-states`, `full30v3` 04
  `clone_preserves_maxMiddleIndex`, and the second `varbase` 10 repeat counted
  above) — of these, `mechb` 01 and `full30v3` 04 are **base-verdict** flips,
  untouched by the reroute.
- over-kill rate as `verifier_replay` scores it: **4/57 = 7%**.

Leg-level exposure, which is what a suite score actually sees:

| suite | leg | doubly-flagged convictions | survived every gate | other convictions on the leg | leg's catch safe |
|---|---|---|---|---|---|
| varbase | 10 | 1 | 0 | 0 | **NO** |
| varbase | 12 | 1 | 1 | 0 | yes |
| mechb | 01 | 3 | 3 | 1 | yes |
| mechb | 03 | 2 | 2 | 0 | yes |
| stack | 01 | 3 | 3 | 0 | yes |
| stack | 02 | 2 | 2 | 3 | yes |
| stack | 03 | 1 | 1 | 1 | yes |
| diffcov | 01 | 1 | 1 | 0 | yes |
| invdiv | 04 | 1 | 1 | 0 | yes |
| invdiv | 06 | 2 | 2 | 0 | yes |
| full30v3 | 04 | 1 | 1 | 0 | yes |
| full30v3 | 19 | 1 | 1 | 1 | yes |

Eight of these twelve legs have **no** conviction other than doubly-flagged
ones, so on those eight the whole catch rides on the family-duty escape holding.

The other two classes are untouched, as designed and as measured: the four
bypass-only convictions still bypass (predicate returns `True` on all four),
and the 35 not-direction-confirmed convictions never reached the predicate.

## G-B3 — suite and prompts

- `uv run pytest -q` from the repo root: **1004 passed, 7 skipped**. (Run from
  `src/` it collects only 40 tests; the full suite lives at the repo root.)
- No judge or verifier prompt file was touched by this study, and none was
  touched by the build (`docs/bypass-qualifier-build-2026-08-10.md`).
- The reroute activates only on the doubly-flagged combination: measured
  36/36 reroute, 4/4 bypass preserved, 35 untouched.

**G-B3: pass.**

## Chart-7-c and Chart-26-c — the enumeration the prereg asked for

**Neither is doubly-flagged. Both are out of build A's reach.**

| leg | conviction | archived gate entry | rate tag | 6B | 6C | 5C |
|---|---|---|---|---|---|---|
| `full30v3` 03 `patch1-Chart-7-SimFix_c` | `clone_preserves_series_observables` | running | **yes** | below-bar (0.6194) | None | None |
| `gs1` 04 `patch1-Chart-7-SimFix_c` | *(none — the leg is clean, no false alarm)* | — | — | — | — | — |
| `gs1` 05 `patch1-Chart-26-Jaid_c` | `jfreechart_draw_acceptsNullInfo` | running | no | no-measurement | None | None |
| `full30v3` 05 `patch1-Chart-26-Jaid_c` | `draw-does-not-mutate-axis-appearance` | running | no | no-measurement | None | None |

Chart-7-c's conviction has the rate fact but was never direction-confirmed, so
the gates already ran on it and kept it — build A changes nothing there.
Chart-26-c's two convictions have neither flag: no rate measurement reached the
evidence at all, and 6B recorded `no-measurement`.

Per the pre-registration, this decides the next design: **the
valid-by-construction probe becomes the named next step.** These convictions
flow through complete evidence the judge keeps on its merits, and no
evidence-completeness mechanism — the bypass qualifier included — can reach
them.

## Build B's reach — the archived baseline only

The isolation re-aim is VM-side and cannot be exercised here. What is
measurable is the baseline it is meant to move: across all seven suites there
are **7** `isolated-buggy-replay` events, all in `gs1_isolation` (leg 01 ×1,
leg 02 ×1, leg 03 ×5). Every one targets a harness oracle id
(`chi-square-formula`, `chi-vs-rms` ×2, `chiSquare-doc-sum`,
`circle-chi-formula-2`, `circle2-chi-from-real-residuals`, `linear-chi-square`)
and **none targets a convicting relation**. Leg 03's two kept convictions
(`rms_matches_root_mean_weighted_residual_square`,
`chiSquare_matches_weighted_residual_formula`) have no isolation event at all,
which is the defect the re-aim fixes. **All seven** readings died `ambiguous`
(3 `status=silent`, 2 `status=isolate_failed`, 2 `status=fired`); the two
`fired` ones are the `chi-vs-rms` pair the agreement branch was added for
("the check prints 0 single-valued expected key(s); a closeness reading needs
exactly one").

## Verdicts

| gate | pre-registered prediction | measured | verdict |
|---|---|---|---|
| G-B1 | the 17 doubly-flagged false convictions convert in the majority | 0 / 17 dismissed by any gate; leak 46/51 = 90% | **FAIL** |
| G-B2 | every doubly-flagged genuine catch survives the value path | 1 / 19 lost to 6B on family-duty NO, in 2 of 3 repeats; that relation is its leg's only conviction | **FAIL** |
| G-B3 | full pytest green, no prompt change, reroute only on the doubly-flagged combination | 1004 passed / 7 skipped; no prompt touched; 36/36, 4/4, 35 untouched | **PASS** |
| Chart probe | report whether Chart-7-c / Chart-26-c are doubly-flagged | neither is | **valid-by-construction probe is the named next design** |

Read together the two failures point the same way. The reroute is mechanically
correct — it fires on exactly the population the measurement identifies, and it
adds no dismissal of its own — but on the bug it was built for it hands the
firing to gates that have nothing to read, while on the bug it was built to
protect it hands the firing to a gate whose only defence is a judgement call
that came back wrong once in three. The mechanism moved the routing; it did not
move a verdict in the intended direction anywhere in the archive.

## Where things live

- cases: `runs-archive/runs/vreplay_gb_20260810/gb_cases.jsonl` (36 cases)
- replay output: `runs-archive/runs/vreplay_gb_20260810/out/`
  (`config.json`, `results.jsonl` 108 rows, `summary.md`)
- this study: `docs/gb-replay-study-2026-08-10.md`

Token spend for the whole study: **1,494,538** — 1,483,192 in the scored run
(143 calls) plus 11,346 in a single-case smoke test. The enumeration, the
predicate check and the gate-input arithmetic cost nothing.
