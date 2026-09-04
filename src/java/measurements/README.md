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

**Where the caller ring comes from.** The call graph is built by
fuzz-introspector, whose Java frontend records a call site only when it can
say statically which method is called. A call through an interface, or to a
method some subclass overrides, is not recorded at all. In practice that
empties the caller ring completely: on the first real measurement run
`PolygonsSet.computeGeometricalProperties()`, `Axis.drawLabel(...)` and
`SimplexSolver.getPivotRow(...)` each came back with no caller, although
the library plainly calls all three. An empty ring reads as "nothing calls
this method", which is not a finding but a tool limitation.

So when the call graph yields no caller at all for a seed, and a checkout
of the code is available, that seed's callers are read out of the **source
text** instead. Every `.java` file under the checkout is parsed, and a
method whose body contains a call written `<seed name>(` with the seed's
number of arguments becomes a caller, up to the same per-seed cap. Files
under a `test`/`tests` directory and classes whose name ends in `Test` are
skipped: a test calling the seed is not part of the library's caller ring,
and the seed's own body never counts, so a recursive call adds nothing.

This match is textual, so it **over-approximates**: the text does not say
what type the receiver has, so a call to a *different* method that happens
to share the seed's name and argument count — on another class entirely —
is counted as a caller too, as is a method declared inside an anonymous
class in some other method's body. The alternative was a caller ring that
is empty for most bugs, which understates P and R̂ in a way no reader can
see. Every member therefore records **how** it was found, `introspector`
or `source-scan`, and each metrics row reports the caller ring split by the
two (`sizes.P_caller_provenance`, `sizes.R_caller_provenance`), so any
number that leans on callers can be re-read with the scanned ones removed.
Archived sets written before this existed carry no such record and count
entirely as `introspector`.

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
builds; line identities are defined on the buggy tree. A third coverage set
is collected for *every harness that compiled*, not only the ones the
pipeline kept — section 3.3.1 says why.

**Static variant.** F is *dynamic*: it is what ran. Its static
counterpart F_stat — what the harnesses *could* have run — is section
3.3.2.

### 3.3.1 Kept versus all compiled harnesses

The pipeline does not keep every harness the model writes. Each harness
that compiles is run once against the unfixed build, and it is kept only
if it *crashes* there — the *acceptance check*. Harnesses that compile but
do not crash are thrown away and never run again.

That filter sits between the prompt and every number in section 4. If we
measure only the harnesses that survived it, a high RCC can mean two very
different things: the prompt sent the harnesses to the root cause, or the
prompt sent them anywhere at all and the acceptance check quietly kept the
few that happened to land on it. A crash on the unfixed build is already
strong evidence of being near the bug, so the kept set is close to
"harnesses selected for reaching the root cause", whatever the prompt did.
The naive set (section 4.2) is where this matters most: its whole
hypothesis is that unconditioned prompts miss the root cause, and the
filter would hide exactly that.

So coverage is collected for both sets:

| harness set | build token | which harnesses | what it answers |
|---|---|---|---|
| kept | `buggy`, `patched` | the ones the acceptance check admitted | where the harnesses the pipeline actually uses go |
| all compiled | `compiled` | every candidate that compiled, kept or not | where the *prompt* sent the harnesses, before the filter |

`compiled` is not a third build of the code: the acceptance check runs on
the buggy build, so its coverage is reported against the buggy classes and
sources, and `compiled` is a separate name only because it is a different
*set of harnesses*. It is collected in the acceptance check's own run,
because that is the only time a rejected candidate is ever executed.

Two things follow. The metrics that read F (RCC, RCP, PSC) are computed
for `compiled` exactly as for the other two builds, and the rendered
Table 3 gets a second block for it. Crash sites do not follow: the
acceptance check's crashes are *by definition* crashes on the unfixed
build — that is what the check tests — so counting them in CSM would
measure the check rather than the harness set. They are recorded, and
counted per build, but kept out of CSM's denominator.

### 3.3.2 F_stat — what the harnesses *could* reach

F as described above is *dynamic*: it is what ran. Its static counterpart,
**F_stat**, is what the harnesses could have run at all. It answers a
question the dynamic set alone cannot: when a method in the root-cause
region was never executed, was it because no harness ever calls anything
that leads there, or because a harness does lead there and the fuzzer's
inputs never drove it that far? The first is a harness-writing failure,
the second a fuzzing one, and only F_stat separates them.

**How it is built.** For one harness, its `FuzzHarness.java` is parsed and
every method call and `new` written in it is read out. A call gives a name
and a number of arguments; where the receiver is a variable whose declared
type the file shows (a local, a parameter, a field) or is written as a
class name, it gives a receiver class too. Each call is then matched
against the project's own method list — the same list and the same
matching rule (`locations.MethodIndex`) every other set is matched with,
including its receiver-rescue rule for a call whose receiver we cannot
name. The methods that match are the **entry methods**: what the harness
calls directly. From them the call graph is walked *downwards* with the
same traversal and the same two caps as P's callee ring
(`REACHABLE_NODE_CAP` nodes, `REACHABLE_MAX_DEPTH` levels, per entry).
F_stat(H) is the union of that over the harness set, and the entry methods
are reported separately (`entries`), because "the harness calls this" and
"the harness could get to this" are different, and the second is far
weaker.

Calls to the JDK, to the Jazzer API the harness is written against, and to
the harness's own helper methods are dropped before matching: none of them
is library code. Everything else that matches nothing is counted in
`unmatched`, so a small F_stat can be traced to a resolution gap rather
than read as a finding.

**Which harness set.** The same two as the dynamic side (section 3.3.1):
`kept`, the harnesses the acceptance check admitted, and `compiled`, every
candidate that compiled. The sources come from
`<leg>/harness_src/<attempt>.java`, which a `--coverage` run saves for
every compiled candidate at the moment the acceptance check runs it — the
one time a rejected candidate is ever seen. For a leg archived before that
existed, the accepted harnesses are recovered from the text of `trace.md`
instead (the file records each accepted attempt and the model output that
produced it), which gives the `kept` set only; those files record
`source: "trace"` so the two provenances stay distinguishable.

**No build, and no lines.** F_stat is read off the source, so it is the
same on the buggy and the patched build, and a call graph names methods,
so there is no line-level static set. Both facts show up in the metric
keys: a `stat` key exists only at method granularity, and its build slot
is a filing convention (`buggy` for the kept set, `compiled` for the
all-compiled set) rather than an observation. Section 6.3 says it again
where the keys are listed.

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
| `neighbourhood.py` | the seed/caller/callee builder used for both P and R̂, on the pipeline's fuzz-introspector call graph, with the pipeline's caps; `SourceScan` is the source-text caller fallback of section 3.1 | no |
| `patch_derived.py` | P from a run's `context.json` (or, for older runs, the same JSON block inside `trace.md`); `lines_for` turns methods into line sets | no |
| `root_cause.py` | R̂: reads the Defects4J developer patch (`<D4J_HOME>/framework/projects/<Project>/patches/<bug>.src.patch`, fixed→buggy direction, verified at run time) or falls back to diffing a fixed checkout; seeds, lines, and the manifestation frames from `failing_tests` | **yes — the only one** |
| `coverage.py` | F(H): parses JaCoCo XML reports, runs the JaCoCo command-line tool on the `.exec` dumps a `--coverage` run leaves behind, unions per build | no |
| `static_reach.py` | F_stat: the library methods a harness source calls (javalang), and the bounded call-graph walk down from them; the `kept`/`compiled` harness sets, with the `trace.md` fallback for archived legs | no |
| `crash_sites.py` | crash sites from raw Jazzer output (`fuzz_out/` of a `--coverage` run) or from the evidence blocks archived in `trace.md` | no |
| `metrics.py` | the five metrics per leg, aggregate and per ring, from the JSON files below only | no |
| `aggregate.py` | macro-averages, the H_N/H_R/delta table, the RCC-versus-caught table | no |
| `paper_tables.py` | the paper's Table 3 and Table 4, in markdown or LaTeX, from a measured run (section 6.5) | no |
| `cli.py` | runs everything over a run directory | imports `root_cause` (allowed here only) |

The pipeline side has three flag-gated hooks (`src/java/run.py`,
`src/java/execution/fuzz_runner.py`, `src/java/harness/prompts.py`), all
measurement-only: `context.json` is written for every leg; `--coverage` adds
the two Jazzer flags from `src/java/execution/coverage_flags.py` to every
Jazzer run — the patched-side fuzz and the buggy-side scan of the kept
harnesses (`FuzzRunner`) and the acceptance check of every compiled
candidate (`HarnessVerifier`, section 3.3.1) — snapshots the compiled
classes, saves raw fuzzer output, and copies every compiled candidate's
harness source to `<leg>/harness_src/<attempt>.java` (the source F_stat is
read from, section 3.3.2); `--naive` removes the root-cause
context from the prompts. With the flags off the pipeline's
prompts and commands are byte-for-byte unchanged, and tests pin that.

### 6.2 Running it

```bash
# on the machine that has Defects4J (the VM), from src/:
python -m java.measurements.cli <run_dir> \
    --checkout_root <dir for Project_bug_buggy checkouts> \
    --d4j_home /home/code/defects4j \
    --introspector          # build the call graph so R̂ gets its rings
    [--coverage]            # also turn cov/*.exec into coverage JSON
    #  (F_stat is computed whenever --introspector is on: it needs the
    #   call graph, and it reads harness_src/ or falls back to trace.md)
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
`coverage_patched.json`, `coverage_compiled.json`, `crash_sites.json`,
`errors.json`. Each set file is a list of locations with their ring tags,
how each was found (`provenance`: `introspector` or `source-scan`), and the
names that could not be matched. The three coverage files are the
three harness-set/build combinations of section 3.3.1: the kept harnesses
on the buggy and the patched build, and every harness that compiled (on the
buggy build). A leg's `result.jsonl` says which harnesses each set is over,
under `coverage`: `compiled_attempts` (every candidate the acceptance check
ran), `accepted_attempts` (the ones it kept) and `sources` (the saved
harness sources).

Two more files hold F_stat (section 3.3.2): `static_kept.json` and
`static_compiled.json`, one per harness set. Each holds `methods` (the set
itself), `entries` (the library methods the harness sources call directly),
`edges` (the call-graph edges the walk crossed), `unmatched` (calls that
resolved to no project method, as written), `harnesses` (which harnesses
the set is the union over) and `source` (`harness_src` when the saved
sources were read, `trace` when they were recovered from the trace).

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
`rcp__line__full__patched__dyn`, `psc__method__na__buggy__dyn`, and for the
all-compiled harness set of section 3.3.1 the same keys with `compiled` in
the build slot: `rcc__method__R0__compiled__dyn`,
`psc__line__na__compiled__dyn`. Two kinds exist (`F_KINDS` in
`metrics.py`). `dyn` is the dynamic set of section 3.3 — what the harnesses
actually executed, from JaCoCo coverage. `stat` is the static set of
section 3.3.2 — what they could have executed — and its keys are
`rcc__method__R0__buggy__stat`, `rcp__method__full__buggy__stat`,
`psc__method__na__buggy__stat` and the same three with `compiled` in the
build slot. Two things are true of every `stat` key and of no `dyn` key:
it exists at **method granularity only** (a call graph has no lines), and
its **build slot is a convention, not an observation** — static reach is
read off the source and is identical on the buggy and the patched build, so
the kept set is filed under `buggy` and the all-compiled set under
`compiled` purely so that each harness set's static and dynamic numbers
land in the same table column.

The kind is in the key so that a table can never put a dynamic number and a
static one in the same column; the rendered Table 3 shows it in those three
column headers, as `RCC (dyn)`, `RCP (dyn)`, `PSC (dyn)`, and the functions
in `aggregate.py` that pick those columns take an `fkind` argument that
defaults to `dyn`. A run that measured F_stat gets its own extra Table 3
block, whose header says `static reach`.

Each value is an object `{value, num, den, by_ring}`; `value` is `null` when
the denominator is empty; `by_ring` holds one ratio per ring for RCR/RCC/PSC
and a seed/caller/callee/outside decomposition summing to one for RCP/CSM.
The R-variants are `R0` (developer-changed methods), `R1` (R0 plus the
manifestation frames; method granularity only) and `full` (with rings).
`rcr_cross__…` is the ring-of-R̂ by ring-of-P count table. Every line also
carries `available` (which inputs existed), `sizes` (|P|, |R̂|, |F| and
their per-ring counts, plus `F_kind`, which records per build slot which
*kinds* of F that slot carries — a list, `["dyn"]`, `["stat"]` or both;
`Fstat_method` / `Fstat_entries` / `Fstat_harnesses` / `Fstat_unmatched` /
`Fstat_source`, the static set's size, its entry count, how many harnesses
it is over, how many calls resolved to nothing and where the sources came
from, all per harness set; `R_stat_only` and `R_dyn_only`, per build slot
and R̂ variant, the counts |R̂ ∩ F_stat − F_dyn| and |R̂ ∩ F_dyn − F_stat| —
the methods of the root-cause region the harnesses could reach but never
ran (a fuzzing failure: the inputs never drove them there) and the ones
they ran although no call in their source leads there (the static
analysis's blind spot: reflection, a lambda, an unresolved virtual call);
and `P_caller_provenance` /
`R_caller_provenance`, the caller ring split into the members the call
graph found and the ones the source scan of section 3.1 did), `matching`
(how many names could not be matched, how many were ambiguous, and
`reclassified`, the mislabelled receivers re-attributed to the class that
really declares the method), `jdk_dropped` (JDK members removed from P and
R̂, and under `*_mislabelled` the ones removed by the second,
population-aware pass), the crash counts, and the leg's identity and
outcome (`caught`, `missed`, `false_alarm`, `clean`).
`crash_by_build` and `sizes.crash_sites_compiled` count the acceptance
check's own crashes; `csm__…` never does (section 3.3.1).

The printed Table 3 is the kept harnesses' by default. When the run also
measured the all-compiled set, a second table follows it; each names its
harness set in its header ("kept harnesses" / "all compiled harnesses"),
and `render_markdown`'s `build` argument renders one of them on its own.

### 6.5 Filling the paper's tables

The paper's two result tables (Table 3, the main result; Table 4, coverage
of the root-cause region plus the classification) are rendered straight
from a measured run by `paper_tables.py`:

```bash
# from src/, on any machine that has the archived run directory:
python -m java.measurements.paper_tables \
    --hr <conditioned run_dir> \
    [--hn <naive run_dir>]      # without it every H_N cell prints an en dash
    [--fkind dyn|stat]          # which kind of F(H) the RCC/RCP/PSC columns read
    [--build buggy|patched|compiled]   # i.e. which harness set
    [--rvar R0|R1|full]         # which root-cause region variant
    [--fmt md|latex]            # markdown for the writeup, LaTeX for the paper
    [--dp 2]                    # decimal places; RCP often needs 3
    [--out FILE]
```

It reads `metrics.jsonl` and `aggregate.json` from the run directory and
writes nothing back. An `aggregate.json` that is missing any key its own
`metrics.jsonl` carries — an archive made before the kind of F(H) moved
into a fifth key slot, say — is treated as stale and the aggregate is
recomputed from the legs; the table's note line says which happened.

**Paper name → our key.** Every cell names a metric, a granularity, an
R-variant, a build and a kind of F(H); the note under each rendered table
spells out the set actually used, so a number can always be traced back.

| paper | our key | note |
|---|---|---|
| RCR_g | `rcr__<gran>__<rvar>__na` | no coverage, so no build and no F kind |
| RCC_g(H) | `rcc__<gran>__<rvar>__<build>__<fkind>` | |
| RCP_g(H) | `rcp__<gran>__<rvar>__<build>__<fkind>` | |
| PSC_g(H) | `psc__<gran>__na__<build>__<fkind>` | no R-side, so no R-variant |
| CSM_g(H) | `csm__<gran>__<rvar>__na` | crash sites, not coverage |
| F1(H) | none — counted from each leg's `outcome` | caught/missed/false_alarm/clean = TP/FN/FP/TN, overfitting is the positive case |
| ℝ | `<rvar>` = `R0` by default: the methods the developer changed | section 3.2 |
| function granularity | `<gran>` = `method` | |
| **edge granularity** | `<gran>` = `line` — **rendered "Line", never "Edge"** | we do not measure control-flow edges (section 2) |
| H_N / H_R | the `--hn` / `--hr` run directory | |
| n | bugs and legs per bug class, in each table's notes | |

**What prints an en dash.** The RCR difference (RCR judges the
patch-derived set, which is built before any harness exists, so H_R − H_N
is not defined), CSM for semantic bugs (nothing crashes in the library, so
there is no site to match), every cell of an arm that was not given, and
any ratio whose denominator was empty. Table 3's F1 column belongs to a
(harness set, bug class) rather than to a granularity, so it is printed on
the Function row and spans the pair; Table 4's RCR spans the two harness
columns for the same reason — unless the two runs' patch-derived sets
actually disagree, in which case both values are printed and a note says
why.

**Averaging** is the macro-average of section 5, straight out of
`aggregate.py`: the mean over a bug's patches first, then the mean and
standard deviation over bugs. "All" is computed over the complete set of
legs, never as the mean of the crashing and semantic rows. When both arms
are given, they are joined leg by leg on (project, bug id, APR tool, label)
exactly as `delta()` does, and both sides are aggregated over the shared
legs only.

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
- **Source-scanned callers over-approximate.** The fallback of section 3.1
  matches a call by name and number of arguments in the source text, with
  no type information, so a same-named, same-arity method on an unrelated
  class is counted as a caller. A generic type argument spelled out in a
  call (`f(new HashMap<String, Integer>())`) has a comma of its own and
  inflates the argument count, which loses that call instead. The split by
  provenance is in every metrics row so the scanned callers can be
  discounted.
- **Static reach over-approximates in one direction and under-approximates
  in the other.** A harness call is resolved by its name and its number of
  arguments, with a receiver class only where the source spells one out, so
  a same-named, same-arity method on an unrelated class can be taken for
  the one that was meant — the same weakness the source-scanned callers
  have, for the same reason. In the other direction the walk sees only what
  the static call graph records: reflection, dynamic dispatch through an
  interface, and the bodies of lambdas handed to library methods are
  invisible, so a method the harness really can reach may be missing.
  F_stat is therefore not a bound in either direction; it is "the calls
  written down, plus what the call graph says they lead to", and every row
  reports `Fstat_unmatched` beside it.
- **Archived legs get the kept set only.** Without `harness_src/`, F_stat
  falls back to the harness sources in `trace.md`, which names the accepted
  attempts but never ties a *rejected* candidate to an attempt id, so no
  `compiled` static set can be built for such a leg.
- **Mislabelled receivers.** The introspector labels a call with the class
  it was reading, not the class that declares the callee, so a JDK call
  inside a project method comes out wearing the project class's name:
  `Rectangle2D.getCenterX()` inside `Axis.drawLabel` is written
  `org.jfree.chart.axis.Axis.getCenterX()`, and the JDK filter cannot see
  it. Two population-aware passes handle it, both needing the coverage
  method list to say what a class really declares. First, a ref whose
  labelled class has no method of that name at all is re-attributed to the
  unique method with that name and argument count elsewhere in the
  population, if there is exactly one — this recovers *inherited* methods,
  which is the common case (`PolygonsSet.getCut()` is
  `BSPTree.getCut()`), and is counted as `matching.*.reclassified`. A name
  that several classes declare stays unmatched rather than being attributed
  by guess, and constructors are never re-attributed, since `<init>` names
  no method. Second, a short list of well-known `java.awt.geom` /
  `java.lang` accessor names (`getCenterX`, `getWidth`, `getBounds2D`,
  `createTransformedShape`, …) is dropped outright when the labelled class
  does not declare them, counted as `jdk_dropped.R_mislabelled` /
  `P_mislabelled`. Both passes are inert without coverage: with no
  population the question cannot be asked, and nothing is dropped.
