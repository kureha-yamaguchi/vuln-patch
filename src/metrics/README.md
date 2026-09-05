# `src/metrics` — the language-agnostic core of the measurement layer

This directory holds one thing: `core/`. Nothing else lives here.

`core/` is the shared core of the measurement layer — one definition of
every metric, used by every language backend:

| file | what it is |
|---|---|
| `core/locations.py` | the code-identity model: `MethodRef`, `LineRef`, ring-tagged `MethodSet`/`LineSet`, and `MethodIndex` for matching the same method across tools |
| `core/definitions.py` | **the metric definitions** — the five metrics (RCR, RCC, RCP, PSC, CSM) at method, line and branch granularity, computed from JSON a backend already wrote |
| `core/aggregate.py` | macro-averaging across the legs of a run; the H_N/H_R/delta table |
| `core/paper_tables.py` | the paper's Table 3 and Table 4, in markdown or LaTeX |

If you are looking for what a metric *is*, `core/definitions.py` is the
file. It reads JSON and does set arithmetic; it parses no language's syntax
and runs no tool.

## The backend rule

The **extractors** that produce that JSON are language-specific and live in
a backend. Today there is one backend, `src/java/measurements/` (patch
diffs, call graphs, JaCoCo XML, Jazzer stack frames, the Defects4J run
layout); a C backend would be a sibling package importing the same `core/`.

The rule is one-way: **a backend imports core, core never imports a
backend.** `tests/test_metrics_core_layering.py` enforces it by reading
import statements, and also checks that this directory contains nothing but
`core/`.

## Where to read next

- `src/java/measurements/README.md` — the full write-up: what each metric
  means, what the three sets are, how the three granularities differ, and
  how to run the measurement on a Java run directory. Read that for the
  definitions in prose.
- `src/java/measurements/d4j_rcc_sweep/README.md` — Kureha's original
  RCC(H_R) sweep, an earlier RCC-only implementation over the crashing
  Defects4J split. It used to live beside this file; it is
  Java/Defects4J/JaCoCo/Jazzer-specific, so it now sits inside the Java
  backend. Section 8 of the Java backend README says what was ported out of
  it and whether the two implementations agree.
