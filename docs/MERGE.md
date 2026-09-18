# Merging the two root-cause metric implementations (September 2026)

Two implementations of the paper's root-cause metrics were written in
parallel: Kureha's `src/metrics` sweep and Hanna's `src/java/measurements`
layer. This note records what was merged, what moved where, where the two
agreed and disagreed, and what to run now. It is the reference for the
merge commits `6abf492`, `9c576a7` and `98145aa` on `main`.

## 1. Layout after the merge

| path | what it is | language |
|---|---|---|
| `src/metrics/core/` | the general layer: code-location sets and rings (`locations.py`), **the definitions of all five metrics** (`definitions.py`), aggregation (`aggregate.py`), paper tables (`paper_tables.py`) | none |
| `src/java/measurements/` | the Java backend: extractors for P, R̂, F, crash sites, static reach, the judge's view, and the measurement CLI | Java / Defects4J / JaCoCo / Jazzer |
| `src/java/measurements/d4j_rcc_sweep/` | Kureha's sweep, unchanged in content, moved here with her latest file names (`scores`, `patchset`, `crashes`, `rescore`, `score_one`, `sweep_full`, `sweep_gate`, `region`, `reached`, `keys`, `collect`) and her README | Java / Defects4J |

Rules, enforced by tests (`tests/test_metrics_core_layering.py`,
`tests/test_measurements_firewall.py`): the core imports nothing from
`java.*`; backends import the core, never the reverse; the pipeline
(`src/java/run.py` and everything it imports) imports neither package; the
developer fix is read only inside the measurement layer.

The old import paths `java.measurements.{locations,metrics,aggregate,paper_tables}`
still work as thin shims. The old `src/metrics/*.py` module paths do **not**;
use `java.measurements.d4j_rcc_sweep.*`.

## 2. What to run now

```bash
# Kureha's sweep (all five metrics, one leg per bug), new paths:
python -m java.measurements.d4j_rcc_sweep.sweep_gate --split ... --side holdout --out ...
python -m java.measurements.d4j_rcc_sweep.sweep_full --split ... --side holdout --out ... --drr drr --model gpt-5.4
python -m java.measurements.d4j_rcc_sweep.rescore   --run results/rcc_hr_crashing_holdout_20260904_001615 --drr drr

# The general measurement pass over any run_suite.sh run directory:
python -m java.measurements.cli <run_dir> --checkout_root <dir> --d4j_home /home/code/defects4j \
    --introspector --coverage [--trigger_gate] [--remeasure] [--naive_run <run_dir>]
python -m java.measurements.paper_tables --hr <run_dir> [--hn <run_dir>] --fmt md|latex
```

Full definitions and every key name: `src/java/measurements/README.md`.
Kureha's write-up of her sweep: `src/java/measurements/d4j_rcc_sweep/README.md`.

## 3. What had to be merged

- **Round one** (`6abf492`, her commits up to `56735ae`): one content conflict,
  `run_jazzer` in `src/java/execution/fuzz_runner.py`, where both sides had
  added a coverage-dump argument under different names. Resolved onto one
  code path: `coverage_dump` + `coverage_include` (alias
  `instrumentation_includes`); both flags are added only when both are
  given; the off path is byte-identical and pinned by tests.
- **Round two** (`9c576a7`, her commits `6991891..b66d37b`): fifteen conflicted
  paths, all structural. Her files had been moved into the backend on our
  side while she edited and renamed them in place. Each was placed by hand:
  her newest content, at the backend location, under her new names, with
  intra-package imports repointed. Her results directories and `.gitignore`
  merged cleanly.
- **One code fix to her last commit**: `keys.py` contained
  `from typing import tuple`, which is an `ImportError` on every Python
  version (there is no `typing.tuple`; the annotations are builtin generics
  and need no import). The line was removed. Nothing importing that module
  could have run since that commit.
- **Alignment commit** (`98145aa`): the general layer adopted her stricter
  crash-site rules (section 5).

## 4. Where the two implementations were the same

Arrived at independently, without coordination:

- R̂ = the methods the Defects4J developer patch changes, mapped with the
  pipeline's own diff-to-method mapper, using the stored fixed→buggy
  direction.
- F = Jazzer's JaCoCo dump through the JaCoCo command-line tool, restricted
  to the project package, per-harness dumps unioned.
- Method identity = class + name + simple parameter types, with an
  arity-only fallback that is counted separately.
- The same five ratios, the same "an empty denominator is undefined, never
  zero", the same unweighted per-bug mean, and the same observation that
  RCC saturates on crashing bugs because acceptance requires a crash.

**Cross-check** (`tests/test_measurements_crosscheck.py`, on her archived run
`results/rcc_hr_crashing_holdout_20260904_001615`): both implementations give
identical RCC on all nine scored bugs. |F| differs by exactly one method on
four bugs because her coverage loader (fuzz-introspector's JaCoCo reader)
credits a method with the next N covered lines from its declaration and
over-runs into the following method; ours uses JaCoCo's own method counter
and is a strict subset. RCC is unaffected; RCP from her |F| would be
slightly pessimistic.

## 5. Where they differed, and what was decided

| aspect | Kureha's sweep | general layer | decision |
|---|---|---|---|
| Scope | method level; crashing holdout; one overfitting patch per bug; H_R only | method, line and branch; all patches; kept and all-compiled harness sets; naive arm | general layer is the reference; sweep kept as is |
| P | touched methods + reachable callees | plus the caller ring (source-scan fallback, provenance-tagged) | both kept |
| F | fixed-input-budget re-run of the kept harnesses (`-runs=20000`) after the pipeline | as-run coverage during the pipeline, both builds, all candidates | both: her mode ported as build token `remeasure` (`--remeasure`); on the pilot both gave identical method sets leg by leg |
| JaCoCo exit-probe miss (method throws before its probe) | repaired from stack frames | not repaired | hers adopted (`coverage.frame_methods`) |
| Trigger-test gate (does the bug's own test reach R̂) | yes, bug excluded on failure | none | hers adopted (`--trigger_gate`); 20/20 pilot legs pass |
| Crash site | deepest `Caused by:`, exact overload by source line, harness identified by record class names | headline frame, nearest overload by name, harness by naming pattern | hers adopted for all three (`98145aa`); resolution rule recorded per site |
| CSM denominator | all reported crashes, harness-only alarms included | crashes with a library site; harness-only counted beside | both reported: `csm` (ours) and `csm_strict` (hers). She counts reports; we deduplicate on (build, exception, frames). Documented in README §4 |
| Population check without model calls | `sweep_gate.py` | none | hers kept; `root_cause.population_check` mirrors its status table |
| Frame exclusions | `javax.`, `org.junit.` excluded | not excluded | hers adopted |

Nothing in the sweep competes with the general layer on definitions: the
sweep's R̂ is the general layer's R̂₀ (seed ring), its P is the seed plus
callee rings, its F is the `remeasure` build, and its metrics are the
method-granularity rows.

## 6. Verification at the time of the push

- Mac: `pytest tests/ -k "not test_metrics"` → 1787 passed (the deselected
  file needs `fuzz_introspector`, which the Mac lacks).
- VM (has `fuzz_introspector`): full suite → 1806 passed, 26 skipped,
  including `tests/test_metrics.py` (hers) and the cross-check.
- `origin/main` = `98145aa`, 21 commits on top of `b66d37b`; all six of
  her commits included.

## 7. For anyone continuing

- New work on the sweep goes in `src/java/measurements/d4j_rcc_sweep/`; new
  general definitions go in `src/metrics/core/definitions.py`; new Java
  extractors go in `src/java/measurements/`.
- A C backend for Project Zero would supply its own extractors (diff to
  functions, call graph, coverage, sanitizer crash sites) and reuse the
  core unchanged; the static half (RCR between a sibling pair's two fixes)
  needs no harnesses.
- The record of every run and decision behind this is in `docs/plan.md`
  items 8.49–8.52.
