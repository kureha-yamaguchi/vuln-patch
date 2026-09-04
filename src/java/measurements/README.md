# Root-cause measurements

This package measures *where* the pipeline's fuzzing harnesses go, relative
to *where the bug actually is*. It answers questions like "did the harnesses
ever execute the code the developer had to change?" and "of everything the
fuzzer ran, how much was anywhere near the bug?". It is a measurement layer
only: nothing here changes how the pipeline decides whether a patch is
correct or overfitting.

It is written so that someone who has not worked on this project (a CS
graduate student, say) can read this file, then the code, and understand
what each number means and how it was computed. Terms are defined the first
time they appear.

## 1. The setting, in one paragraph

An automatic program-repair tool produces a *patch* for a known bug. The
patch makes the bug's failing test pass, but it may be *overfitting*: it
silences the reported symptom while leaving the underlying cause in place,
so a *sibling bug* (a different input reaching the same broken logic) still
exists. The pipeline in this repository tries to expose such patches by
asking a language model to write *fuzzing harnesses* (small programs that
feed generated inputs to the library) aimed at the bug's *root cause*, then
running them with a fuzzer (Jazzer) on the patched code. If a harness still
fails on the patched build, the patch is flagged as overfitting. The
harnesses are meant to be *conditioned on the root cause*: the prompt shows
the model the code around the patch so its harnesses head for the right
region. This package checks whether that actually happens.

## 2. Code locations and the two granularities

Everything below is a *set of code locations*. We use two kinds of location:

- **Method granularity.** A location is one Java method or constructor,
  identified by its class, its name and its parameter types, e.g.
  `org.jfree.chart.plot.PiePlot3D.draw(Graphics2D,int)`. Constructors are
  written `<init>`.
- **Line granularity.** A location is one source line, identified by the
  top-level class of its file and the line number, e.g.
  `org.jfree.chart.plot.PiePlot3D:412`.

Three different tools name the same method three different ways
(the static call-graph tool, the coverage tool, and the Java parser we use
on diffs). `locations.py` converts all of them into one form and matches
them across tools. Matching is exact when both sides carry the full package
name, and otherwise falls back to "same simple class name, same method name,
same number of parameters". Every count that depends on matching also
records how many names could not be matched, so a low number can be traced
to a naming gap rather than mistaken for a real finding.

The paper also mentions *control-flow edge* granularity. We do not measure
that: it would need bytecode-level control-flow analysis nobody has built.
Lines are the fine granularity we can actually observe for all three sets
below, and coverage of *branches* (the two outcomes of each `if`) is reported
alongside as a coarse stand-in.

## 3. The three sets

### 3.1 P — the patch-derived set

**What it is.** The neighbourhood of code around the patch under test,
built by walking the program's *call graph* (which method calls which)
starting from the methods the patch changes. It is exactly the context the
pipeline shows the language model when it asks for harnesses, so P is
"where we told the model the bug lives".

**How it is built.** Three *rings*:

| ring | meaning | how far | cap |
|---|---|---|---|
| seed | the methods whose bodies the patch changes | – | – |
| caller | methods that call a seed | one step up | 5 per seed |
| callee | methods a seed calls, and what those call, … | up to 3 steps down | 200 methods in total |

The caps and depths are the pipeline's own settings (`MAX_XREFS_PER_FUNCTION`,
`REACHABLE_MAX_DEPTH`, `REACHABLE_NODE_CAP` in `src/config.py`); the
measurement reuses them so P is the set the model saw, not a re-imagined
one. Note that the prompt itself shows at most 60 of the callees
(`MAX_REACHABLE_IN_PROMPT`), so P as measured is slightly wider than P as
shown.

A method can qualify for two rings (a caller of one seed may be a callee of
another). It is counted once, in the ring closest to the seeds, and the
other membership is recorded separately, so per-ring counts never
double-count.

**Where it comes from.** From the pipeline's own context dump for the run
(`context.json` in each run directory; for older runs, the same JSON block
inside `trace.md`). It is never recomputed from scratch, so it cannot drift
from what the model was shown.

### 3.2 R̂ — the approximated root-cause region

**What it is.** The paper's ℝ is *the* region of code that carries the
underlying cause of the bug, from which sibling bugs arise. Nobody can
observe that directly; it is latent. R̂ (read "R hat") is our approximation
of it, and it is built from the one piece of ground truth we have but the
pipeline never sees: the **developer's own fix** for the bug.

**How it is built.** With the same three-ring construction as P, applied to
the developer's fix instead of the repair tool's patch. So the seeds are the
methods the developer changed, and callers and callees are walked with the
same caps. Two named variants are used in the metrics:

- **R̂₀** — only the seeds: the methods the developer changed. The
  narrowest defensible definition.
- **R̂₁** — R̂₀ plus the *manifestation frames*: the project methods on the
  stack when the bug's failing tests fail on the unfixed code. This is
  "where the bug shows up", which may differ from "where it is fixed".

The rings around R̂₀ let us ask an empirical question the paper leaves open:
*how wide is the real root-cause region?* For every overfitting patch the
pipeline catches, the crash lands in R̂'s seeds, its callers, its callees, or
outside all three. That histogram is the closest we can get to observing ℝ.

**The firewall.** The developer fix is held back from the pipeline on
purpose: a pipeline that had seen the fix could not be evaluated fairly.
So R̂ is computed *after* a run, by a separate command, from the archived
run directory, and `root_cause.py` is the only file in the whole repository
that reads the developer fix. A test (`tests/test_measurements_firewall.py`)
scans the source tree and fails if any pipeline module ever imports it.

### 3.3 F(H) — the fuzzer-reachable set

**What it is.** The set of locations the harness set H actually executed
while fuzzing. P and R̂ are static (computed from source); F is dynamic
(observed at run time).

**How it is measured.** The fuzzer runs on the Java virtual machine, and
Jazzer can record coverage with JaCoCo, the standard Java coverage tool.
With the pipeline's `--coverage` flag on, every fuzz run writes a JaCoCo
coverage file, restricted to the library under test (the harness's own code
and the fuzzer's are excluded). After the run, the measurement command turns
those files into a report listing every method, line and branch that was
executed, and unions them over the harness set.

**Which build.** Harnesses are run twice by the pipeline: on the *buggy*
build (to prove they reach the bug) and on the *patched* build (to test the
patch). Coverage is collected on both. The buggy-side coverage is the
primary one for the metrics below, because that is the build on which the
root cause exists exactly as the developer found it; patched-side numbers
are reported as a secondary column. Method identities are the same on both
builds; line identities are defined on the buggy tree.

**Static variant.** Where useful, F_stat is also computed: the callees
reachable, on the static call graph, from the library methods the harness
source calls. It is what the harness *could* reach; F (dynamic) is what it
*did* reach.

## 4. The five metrics

All are ratios of set sizes. Each is reported with its numerator and
denominator, is undefined (`null`) when the denominator is empty, and is
computed at both granularities and for both R̂ variants.

| metric | formula | plain reading |
|---|---|---|
| **RCR** root-cause recovery | \|R̂ ∩ P\| / \|R̂\| | How much of the developer's region did our patch neighbourhood contain? Judges the *extraction*, not the harnesses. Identical for naive and conditioned harnesses. |
| **RCC** root-cause coverage | \|R̂ ∩ F(H)\| / \|R̂\| | How much of the developer's region did the harnesses actually execute? |
| **RCP** root-cause precision | \|R̂ ∩ F(H)\| / \|F(H)\| | Of everything the harnesses executed, what share was root cause? The budget-focus number. |
| **PSC** patch-derived-set coverage | \|P ∩ F(H)\| / \|P\| | How much of our *own* neighbourhood did the harnesses execute? Did they do what the prompt asked? The only metric that never looks at the developer fix. |
| **CSM** crash-site match | \|{c ∈ C : site(c) ∈ R̂}\| / \|C\| | Of the crashes the harnesses produced, how many happened *in* the root-cause region? Are the crashes the right crashes? |

**Crash sites.** The site of a crash is the deepest stack frame inside the
library under test. A harness's own assertion frame does not count. For
bugs that do not crash (the pipeline's *semantic* bugs), the harness detects
a wrong *value* and raises the alarm itself, so the stack often contains no
library frame at all; such crashes are counted separately as
`harness_only` and excluded from CSM's denominator, and the README of the
main pipeline explains why value bugs need different oracles.

### 4.1 Per-ring versions

Every metric is also broken down by ring of the set in its denominator:

- RCR, RCC per ring: is the developer's *seed* ring inside P / reached by F,
  is their *caller* ring, is their *callee* ring. RCC per ring is the direct
  answer to "do callees even matter".
- RCP per ring: the numerator splits by ring of R̂ and the denominator stays
  \|F\|, giving a decomposition of the fuzzing budget into seed / caller /
  callee / outside that sums to one.
- PSC per ring: as RCC, against P.
- CSM per ring: the share of crashes landing in each ring or outside — the
  sibling-location histogram described in 3.2.
- RCR cross-table: ring of R̂ against ring of P, which shows for instance
  that the developer's seed methods sit in *our* callee ring, meaning the
  repair tool patched upstream of where the developer fixed.

Ring sizes are very unequal (a few seeds, tens of callers, up to hundreds of
callees), so per-ring ratios are always shown with their counts and should
not be over-read when the count is tiny.

### 4.2 The hypotheses the metrics test

Let H_N be the *naive* harness set (generated with the root-cause
context removed from the prompt: the pipeline's `--naive` flag) and H_R the
*root-cause-conditioned* set (the normal pipeline). The paper predicts

1. RCC(H_N) ≪ 1 — naive harnesses mostly miss the root cause,
2. RCC(H_R) → 1 — conditioned harnesses mostly reach it,
3. RCR → 1 — the patch neighbourhood contains the root cause.

and asks whether RCC predicts sibling-bug detection: for the overfitting
patches, is a caught patch one whose harnesses had high RCC? The aggregate
command produces the H_N / H_R / difference table and the RCC-versus-caught
table for exactly these questions.

## 5. Averaging

Patches of one bug are not independent samples: they share the bug, its
call graph and its developer fix. So every aggregate is a *macro-average*:
first the mean over a bug's patches, then the mean (and standard deviation)
over bugs, reported separately for crashing bugs, semantic bugs, and all.
Bugs with large call graphs therefore do not dominate the result.

## 6. Files, commands and output

### 6.1 Modules

| file | what it does | reads the developer fix? |
|---|---|---|
| `locations.py` | the shared data model: `MethodRef`, `LineRef`, ring-tagged `MethodSet`/`LineSet`, and `MethodIndex` for matching names across tools | no |
| `neighbourhood.py` | the seed/caller/callee builder used for both P and R̂, on the pipeline's fuzz-introspector call graph, with the pipeline's caps | no |
| `patch_derived.py` | P from a run's `context.json` (or, for older runs, the same JSON block inside `trace.md`); `lines_for` turns methods into line sets | no |
| `root_cause.py` | R̂: reads the Defects4J developer patch (`<D4J_HOME>/framework/projects/<Project>/patches/<bug>.src.patch`, fixed→buggy direction, verified at run time) or falls back to diffing a fixed checkout; seeds, lines, and the manifestation frames from `failing_tests` | **yes — the only one** |
| `coverage.py` | F(H): parses JaCoCo XML reports, runs the JaCoCo command-line tool on the `.exec` dumps a `--coverage` run leaves behind, unions per build | no |
| `crash_sites.py` | crash sites from raw Jazzer output (`fuzz_out/` of a `--coverage` run) or from the evidence blocks archived in `trace.md` | no |
| `metrics.py` | the five metrics per leg, aggregate and per ring, from the JSON files below only | no |
| `aggregate.py` | macro-averages, the H_N/H_R/delta table, the RCC-versus-caught table | no |
| `cli.py` | runs everything over a run directory | imports `root_cause` (allowed here only) |

The pipeline side has three flag-gated hooks (`src/java/run.py`,
`src/java/execution/fuzz_runner.py`, `src/java/harness/prompts.py`), all
measurement-only: `context.json` is written for every leg; `--coverage` adds
the two Jazzer flags from `src/java/execution/coverage_flags.py`, snapshots
the compiled classes, and saves raw fuzzer output; `--naive` removes the
root-cause context from the prompts. With the flags off the pipeline's
prompts and commands are byte-for-byte unchanged, and tests pin that.

### 6.2 Running it

```bash
# on the machine that has Defects4J (the VM), from src/:
python -m java.measurements.cli <run_dir> \
    --checkout_root <dir for Project_bug_buggy checkouts> \
    --d4j_home /home/code/defects4j \
    --introspector          # build the call graph so R̂ gets its rings
    [--coverage]            # also turn cov/*.exec into coverage JSON
    [--naive_run <run_dir>] # a --naive run to diff against (Table 3 delta)
```

`<run_dir>` is a suite directory holding one `<NN>_<patch>_<o|c>/` leg
directory per patch, each with `trace.md` and `result.jsonl` (and, for runs
made after this package existed, `context.json`, `cov/`, `fuzz_out/`).
Checkouts are reused across the legs of one bug; a failing step is recorded
in the leg's `measurements/errors.json` and the run continues.

### 6.3 What it writes

Per leg, under `<leg>/measurements/`: `patch_derived.json`,
`patch_derived_lines.json`, `root_cause.json`, `coverage_buggy.json`,
`coverage_patched.json`, `crash_sites.json`, `errors.json`. Each set file is
a list of locations with their ring tags plus the names that could not be
matched.

Per run: `metrics.jsonl` (one line per leg), `aggregate.json`, and
`delta.json` when `--naive_run` was given. The CLI also prints the Table 3
layout and the RCC-versus-caught table as Markdown.

Metric keys in `metrics.jsonl` come in two shapes, depending on whether the
metric reads the fuzzer-reachable set F(H).

RCR, CSM and the cross-table do not read F, and keep four slots,
`<metric>__<granularity>__<R-variant>__<build>`, with `na` in a slot the
metric does not use: `rcr__method__R0__na`, `csm__method__R0__na`,
`rcr_cross__line__full__na`.

RCC, RCP and PSC are computed from F, and add a fifth slot naming the *kind*
of F the number came from: `rcc__method__R0__buggy__dyn`,
`rcp__line__full__patched__dyn`, `psc__method__na__buggy__dyn`. Two kinds
exist (`F_KINDS` in `metrics.py`). `dyn` is the dynamic set of section 3.3 —
what the harnesses actually executed, from JaCoCo coverage — and is the only
kind anything emits today. `stat` is reserved for the planned static variant
of section 3.3: what the harnesses could reach, from call-graph reachability
out of the library methods the harness source calls. The kind is in the key
so that a table can never put a dynamic number and a static one in the same
column; the rendered Table 3 shows it in those three column headers, as
`RCC (dyn)`, `RCP (dyn)`, `PSC (dyn)`, and the functions in `aggregate.py`
that pick those columns take an `fkind` argument that defaults to `dyn`.

Each value is an object `{value, num, den, by_ring}`; `value` is `null` when
the denominator is empty; `by_ring` holds one ratio per ring for RCR/RCC/PSC
and a seed/caller/callee/outside decomposition summing to one for RCP/CSM.
The R-variants are `R0` (developer-changed methods), `R1` (R0 plus the
manifestation frames; method granularity only) and `full` (with rings).
`rcr_cross__…` is the ring-of-R̂ by ring-of-P count table. Every line also
carries `available` (which inputs existed), `sizes` (|P|, |R̂|, |F| and
their per-ring counts, plus `F_kind`, which records per build how that
build's F was obtained — `dyn` for now), `matching` (how many names could
not be matched, and how many were ambiguous), the crash counts, and the
leg's identity and outcome (`caught`, `missed`, `false_alarm`, `clean`).

### 6.4 Known limits

- **Archived runs made before this package** have no `context.json`, no
  coverage and no raw fuzzer output. For them P comes from the trace's
  context block (callee depths unknown, callers known by name only), R̂ and
  RCR are computed in full, crash sites exist only for the patched build
  (the verifier's evidence blocks are the one place the archive keeps full
  stack traces), and RCC/RCP/PSC are unavailable.
- **Semantic bugs** rarely give library crash sites: the harness raises the
  alarm itself after the library returned a wrong value. CSM is then
  undefined and the `harness_only` count says so.
- **Manifestation frames** need `failing_tests`, which Defects4J writes only
  after `defects4j test` has been run on the checkout.
- **Name matching** is exact when both sides are fully qualified and falls
  back to simple-class-name plus arity. The introspector's parameter labels
  are unreliable, and some of its callee entries are expressions rather
  than method names; those land in `unmatched` and are counted, never
  silently dropped.
