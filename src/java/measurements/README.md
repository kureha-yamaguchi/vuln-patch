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

## 2. Code locations and the three granularities

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

On top of those two kinds of location there is a third **granularity**,
which is not a third kind of location at all:

- **Branch granularity.** The same *line* sets, counted by **branch
  outcome** instead of by line. The coverage tool already tells us, for
  every source line, how many branch outcomes that line's decision point
  has and how many of them were taken — a two-way `if` on a line has two,
  a three-case `switch` has three, and `a && b` on one line has four. So a
  "branch set" is *the branch outcomes on a set of lines*: for a set of
  lines L,

  - `total(L)` = the outcomes present on those lines,
  - `taken(L)` = the outcomes among them that some harness actually took.

  A line with no decision point on it contributes nothing to either count,
  so a region of fifty straight-line statements has a branch denominator of
  zero and reports "undefined" rather than a misleading 1.0. Section 4.3
  gives the metric formulas.

The paper also mentions *control-flow edge* granularity. The branch
granularity is the nearest thing we can actually observe, and it is not the
same thing: it counts **the outgoing edges of decision points only**. The
single edge out of a straight-line statement into the next one is a
control-flow edge but not a branch, so it is not represented anywhere in
these numbers. A real control-flow-edge version would mean walking the
bytecode with ASM and placing our own probes at every edge instead of
relying on the branch counters the coverage tool already emits; that is
future work, and until it exists no row or column anywhere in this package
is labelled with the paper's word for it. Lines and branches are what we
report.

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

At **line** granularity there is a third variant, and it is the one to read
first:

- **R̂body** — every line of the *body* of each developer-changed method.

R̂₀'s lines are the lines the fix itself touched, and R̂body's are the whole
methods those lines sit in, so R̂₀ ⊆ R̂body always. The two answer different
questions:

- R̂₀ asks **did the harnesses execute the fix's own lines** — a strict test,
  and the one that matches the paper's ℝ most literally.
- R̂body asks **how thoroughly is the fixed method exercised** — which is the
  fairer question to ask of a fuzzer, because the fuzzer was never shown the
  fix. Nothing in the harness generator knows which lines of a method the
  developer later changed; it can only aim at the method. A harness that
  drives the fixed method hard but happens to take a branch that skips the
  changed line scores 0 on R̂₀ and something high on R̂body, and calling that
  a total failure of root-cause coverage would be measuring the fuzzer
  against knowledge it was denied.

Neither is a replacement for the other, so **both are reported**: every
line-level metric is emitted for `R0`, `full` and `Rbody`, and the paper's
tables can be rendered against any of the three (`--rvar`, section 6.5).

R̂body exists at line granularity only. At method granularity "the body of
each changed method" is just "each changed method", which is R̂₀ under
another name, so no `*__method__Rbody__*` key is ever emitted.

It is computed inside `root_cause.compute`, by handing the seeds alone to
`patch_derived.lines_for` against the buggy source root — the same machinery
that turns the pipeline's own patch-derived method set into lines, so the
developer's line set and the pipeline's stay comparable. The rings are
deliberately left out: a caller's or callee's body is not part of the fix.
The result is stored on `RootCause.body_lines` and serialised under
`body_lines`; a `root_cause.json` written before the field existed has no
such key, loads as an empty set, and gets no `Rbody` metric keys at all
(which is different from getting a region of size zero).

The rings around R̂₀ let us ask an empirical question the paper leaves open:
*how wide is the real root-cause region?* For every overfitting patch the
pipeline catches, the crash lands in R̂'s seeds, its callers, its callees, or
outside all three. That histogram is the closest we can get to observing ℝ.

**Is R̂ measurable at all? The triggering-test gate.** Every Defects4J bug
comes with a *triggering test*: a test that fails on the buggy code and
passes on the fixed code. That test fails *because of* the code the
developer fix changed, so it must run every method in R̂₀. When it does not,
one of two things is wrong — R̂ was extracted from the wrong place, or the
coverage plumbing is spelling methods differently on the two sides — and
this bug's RCC would then be a number about our tooling rather than about
the harness set. Without the check, a name mismatch gives RCC = 0 on every
bug, which reads exactly like a real finding.

So `root_cause.trigger_gate` runs the bug's own triggering tests on the
buggy build under the JaCoCo agent (one `defects4j test -t <test>` per
triggering test, so exactly the triggering method runs and not its whole
class — a whole class would cover more and make the gate weaker) and checks
R̂₀ against what they reached. It separates the two ways a seed can fail:
one the report holds no method for at all (`unresolved` — a naming or
extraction fault, the one the gate exists to catch) and one the report knows
that the tests simply did not run. The verdict travels in `root_cause.json`
as `trigger_gate`, reaches every metrics row as `available.trigger_gate` and
`sizes.trigger_gate_passed`, and `aggregate.aggregate(gated_only=True)`
drops the legs whose bug failed it. A gate that was **not run** is a third
value, not a failure: the gate is slow, it is off unless `--trigger_gate`
is given, and treating "not asked" as "failed" would empty most runs.

The gate has a second use. R̂₁'s stack frames come from
`<checkout>/failing_tests`, which Defects4J writes only once a test has been
run — so on a fresh checkout the manifestation set is empty for want of a
trace, not for want of frames. The gate runs those very tests, so it leaves
the trace where `trigger_frames` reads it and the CLI re-reads the manifest
afterwards. A run with `--trigger_gate` therefore gets a populated R̂₁ as a
side effect.

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

**The probe limitation, and the frames that repair it.** JaCoCo marks a
line as covered when a *probe* on it executes, and it places a method's
probe **after** the method's exit. A method whose body is a single
`return other(x);` therefore reads as *missed* whenever `other` throws,
because the probe after the call never runs. Every bug in the crashing
split ends in a throw, so this under-reports exactly the path the whole
measurement is about. The recorded case is Math-70: the stack trace names
`BisectionSolver.solve` at line 72 and JaCoCo reports line 72 as never
covered, which would have made that bug's RCC a false 0.

A stack frame is proof that a method was entered, so the two sources are
unioned: the probes are the lower bound, and the frames add what the probes
provably missed. The frames come from the raw fuzzer output the run saved
(`fuzz_out/<harness>_<build>.txt`, so a build is only ever repaired from
its own runs), and each frame is resolved against the report's own method
list by class, method name and **line** — a frame carries no parameter
types, so the line is what tells two overloads of one name apart. Which
lines a method owns is reconstructed from the report: within one source
file the methods are sorted by their declaration line and each owns the gap
to the next (nested classes included, since they share the file).

The two provenances stay apart afterwards. `Coverage.methods` is the union
and is what every metric reads; `methods_from_probes` is the probe half;
`frame_methods` is what the frames proved; and `sizes.F_frame_added` per
build is the size of the probe limitation on that leg — how many methods
would have been reported as never executed on the evidence of the probes
alone. A frame-only hit is never mistaken for a probe hit.

**Static variant.** F is *dynamic*: it is what ran. Its static
counterpart F_stat — what the harnesses *could* have run — is section
3.3.3.

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

### 3.3.2 As-run and re-measured coverage

The pipeline fuzzes each harness under a **wall-clock** budget (`--fuzz_timeout`
seconds). That is the right budget for the pipeline — it is what the verdict
rests on — but it is a poor budget for a measurement, because a loaded
machine gets through fewer inputs in twenty seconds than an idle one. The
coverage under the `buggy` token is therefore the **as-run** coverage: an
honest record of the run that produced the verdict, and machine-dependent.

`remeasure` is the same kept harnesses on the same buggy build, re-run with
a fixed **input** budget: `-runs=20000` inputs per harness and
`--keep_going=1000` findings to run past (an accepted harness crashes the
buggy build by design, so without a large `--keep_going` the run would end
on its first input). The number of inputs is the experiment's parameter, so
two machines agree on the answer. Same build, same harnesses, different
budget — which is why it is a build token of its own and never averaged
into `buggy`.

It is a re-run, not a re-reading, so it costs a fuzzing pass per leg and is
off unless `--remeasure` is given. The harnesses come from `result.jsonl`'s
`accepted_harnesses`, which the pipeline records for exactly this purpose; a
leg archived before that field existed has no set to re-run and is skipped
rather than measured as empty. The run itself is `metrics.collect
.harness_coverage` from the sibling package (section 8).

| build token | harness set | budget | build |
|---|---|---|---|
| `buggy` | kept | wall clock, as run | buggy |
| `patched` | kept | wall clock, as run | patched |
| `compiled` | every candidate that compiled | wall clock, as run | buggy |
| `remeasure` | kept | fixed input count | buggy |

### 3.3.3 F_stat — what the harnesses *could* reach

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
computed at every granularity that applies and for both R̂ variants. CSM is
the exception: it has no branch form at all (section 4.3).

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

### 4.3 The branch granularity

The branch granularity re-weights the *line* metrics of section 2: the sets
stay the same line sets, and what changes is what gets counted in them. For
a set of lines L, `total(L)` is the branch outcomes present on those lines
and `taken(L)` the ones some harness took, so:

| metric | formula |
|---|---|
| **RCC** at branch level | `taken(R̂ lines) / total(R̂ lines)` |
| **PSC** at branch level | `taken(P lines) / total(P lines)` |
| **RCP** at branch level | `taken(R̂ lines) / taken(all lines)` |
| **RCR** at branch level | `total(R̂ ∩ P lines) / total(R̂ lines)` |

RCC and PSC read the same way as at line granularity, but ask a harder
question: not "was this line reached at all" but "were the decisions on it
driven both ways". RCP's denominator is every outcome the harnesses took
anywhere, so it decomposes the fuzzing budget across the rings exactly as
the line-level RCP does. RCR is still a statement about the patch-derived
set P and reads no coverage — but the *weights* have to come from somewhere,
and outcome counts only exist inside a coverage report, so it borrows the
primary build's report and records which one under `weights_from`. The
counts are a property of the compiled code, so the two builds agree on them.

**CSM has no branch form.** The site of a crash is a stack frame — a method
and a line — and there is no branch outcome to compare it against, so no
`csm__branch__…` key is ever emitted and the tables print a dash in that
cell.

**What it counts, and what it does not.** Only the outgoing edges of
decision points. A straight-line statement's fallthrough edge into the next
statement is a control-flow edge, and it is invisible here, because the
coverage tool never reports it. This is therefore the nearest *observable*
stand-in for the paper's edge level and not that level itself; a full
control-flow-edge version (walking the bytecode with ASM and placing our
own probes) is future work.

**Why a merged coverage report exists.** Methods and lines are sets of
identities, so the union over a harness set is exact: a method either is or
is not in the set. Branch data is not. The report gives *counts* per line —
"two of this line's four outcomes were taken" — and never says *which* two,
so two harnesses that each took outcome 1 are indistinguishable from two
harnesses that took outcome 1 and outcome 2, and no arithmetic on two
per-harness reports can tell them apart.

The fix is to let the coverage tool merge before it counts. `collect_leg`
hands *all* of a build's `.exec` files to one report call and writes the
result to `<leg>/cov/merged_<build>.xml`; the whole per-build coverage
object is then read from that one report, so the branch counts are the
harness *set*'s and are exact. Each coverage file records which route it
took under `branches_from`:

- `merged` — one report over all of that build's execution data, exact. A
  build with a single harness needs no extra work: its own report already
  is the merged one.
- `union-upper-bound` — no merged report could be made (a run archived with
  its XML reports but without the `.exec` files they came from, or a
  coverage tool that would not run), so the per-harness reports were added
  up per line and capped at each line's own total. Two harnesses taking the
  same outcome are counted twice, so this can only over-count: it is an
  upper bound on the set's real branch coverage.

Because branch counts live only in a coverage report, a leg whose coverage
files were written before this existed carries no branch keys at all,
rather than zero-valued ones.

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
| `root_cause.py` | R̂: reads the Defects4J developer patch (`<D4J_HOME>/framework/projects/<Project>/patches/<bug>.src.patch`, fixed→buggy direction, verified at run time) or falls back to diffing a fixed checkout; seeds, lines, and the manifestation frames from `failing_tests`; `trigger_gate` (section 3.2) and `population_check`, which states a population's exclusions | **yes — the only one** |
| `coverage.py` | F(H): parses JaCoCo XML reports, runs the JaCoCo command-line tool on the `.exec` dumps a `--coverage` run leaves behind, and per build reads one MERGED report over all of them (section 4.3), falling back to a union of the per-harness reports; repairs the probe miss from the run's stack frames (section 3.3) and re-runs the kept set on a fixed input budget (`remeasure_leg`, section 3.3.2) | no |
| `static_reach.py` | F_stat: the library methods a harness source calls (javalang), and the bounded call-graph walk down from them; the `kept`/`compiled` harness sets, with the `trace.md` fallback for archived legs | no |
| `crash_sites.py` | crash sites from raw Jazzer output (`fuzz_out/` of a `--coverage` run) or from the evidence blocks archived in `trace.md` | no |
| `metrics.py` | the five metrics per leg, at method, line and branch granularity, aggregate and per ring, from the JSON files below only | no |
| `aggregate.py` | macro-averages, the H_N/H_R/delta table, the RCC-versus-caught table | no |
| `paper_tables.py` | the paper's Table 3 and Table 4, in markdown or LaTeX, from a measured run (section 6.5) | no |
| `judge_view.py` | RCR for the one-shot LLM judge: the neighbourhood the *baseline* was shown, against the same R̂ (section 7) | no |
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
read from, section 3.3.3); `--naive` removes the root-cause NEIGHBOURHOOD
(level B: the patch diff and the failing test stay; callers, callees and
coverage steering go) from every model-facing prompt of a leg — harness
generation (`harness/prompts.py`: the variant-analysis `<root_cause_reachable>` block
with its covered-functions/found-signatures steering, the `<xref>` call-site
examples, the `<callee>` declarations, and the propagation rule's
reachable-region clause) AND relation synthesis (`relations/relation_synth.py`:
the "Reachable API" line), with the honoured builders listed in the leg
record's `naive_scope`. With the flags off the pipeline's prompts and
commands are byte-for-byte unchanged, and tests pin that.

### 6.2 Running it

```bash
# on the machine that has Defects4J (the VM), from src/:
python -m java.measurements.cli <run_dir> \
    --checkout_root <dir for Project_bug_buggy checkouts> \
    --d4j_home /home/code/defects4j \
    --introspector          # build the call graph so R̂ gets its rings
    [--coverage]            # also turn cov/*.exec into coverage JSON
    [--trigger_gate]        # run the bug's own triggering tests and check
                            #  they reach R̂₀ (slow: one `defects4j test -t`
                            #  each); also fills R̂₁, section 3.2
    [--remeasure]           # re-run the kept harnesses on a fixed input
                            #  budget (--runs / --keep_going), section 3.3.2
    [--gated_only]          # aggregate only the legs whose bug PASSED the
                            #  gate (legs where it was not run are kept)
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

Per leg, under `<leg>/cov/`: one JaCoCo XML report per harness and build,
`<harness>_<build>.xml`, plus a MERGED report per build,
`merged_<build>.xml`, holding all of that build's execution data in one
report. The merged one is what the build's coverage is read from, because
it is the only place the harness set's *branch* counts are right (section
4.3); a build with a single harness needs none, since its own report
already is the merged one. A leg archived without its `.exec` files can
still be measured from the per-harness reports, and its coverage then says
so under `branches_from`.

Per leg, under `<leg>/measurements/`: `patch_derived.json`,
`patch_derived_lines.json`, `root_cause.json` (which holds `methods`,
`lines`, `body_lines`, `manifest` and, with `--trigger_gate`,
`trigger_gate`), `coverage_buggy.json`,
`coverage_patched.json`, `coverage_compiled.json`, `coverage_remeasure.json`
(with `--remeasure`), `crash_sites.json`,
`errors.json`. Each set file is a list of locations with their ring tags,
how each was found (`provenance`: `introspector` or `source-scan`), and the
names that could not be matched. The three coverage files are the
three harness-set/build combinations of section 3.3.1: the kept harnesses
on the buggy and the patched build, and every harness that compiled (on the
buggy build). Each also holds `line_branches` — per source line, how many
branch outcomes it has and how many were taken, which is what the branch
granularity counts — `branches_from`, either `merged` (exact) or
`union-upper-bound` (the per-harness reports added up; section 4.3), and
the two provenance halves of `methods`: `methods_from_probes` (JaCoCo's own
counters) and `frame_methods` (what the run's stack traces proved,
section 3.3). A leg's `result.jsonl` says which harnesses each set is over,
under `coverage`: `compiled_attempts` (every candidate the acceptance check
ran), `accepted_attempts` (the ones it kept) and `sources` (the saved
harness sources).

Two more files hold F_stat (section 3.3.3): `static_kept.json` and
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
section 3.3.3 — what they could have executed — and its keys are
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

A third granularity, `branch`, sits beside `method` and `line`. It counts
branch OUTCOMES on the same line sets rather than lines (section 4.3), so
its keys are `rcc__branch__R0__buggy__dyn`,
`rcp__branch__full__patched__dyn`, `psc__branch__na__compiled__dyn` and
`rcr__branch__R0__na`, with the line R-variants (`R0`, `full`, `Rbody`) and
never `R1`. Two things are true of every `branch` key and of no other:
**there is no CSM at branch level** — a crash site is a stack frame, not a
branch outcome, so no `csm__branch__…` is ever emitted (and neither is
`rcr_cross__branch__…`, which counts locations) — and **every branch key,
RCR included, needs a coverage report**, because outcome counts exist
nowhere else. RCR's branch form therefore borrows the primary build's
report for its weights and names it in the value's `weights_from`. Each
branch value also carries `branches_from`, saying whether the counts came
from a merged report or from the per-harness upper bound. A leg whose
coverage files predate `line_branches` emits no branch keys at all.

Each value is an object `{value, num, den, by_ring}`; `value` is `null` when
the denominator is empty; `by_ring` holds one ratio per ring for RCR/RCC/PSC
and a seed/caller/callee/outside decomposition summing to one for RCP/CSM.
The R-variants are `R0` (developer-changed methods), `R1` (R0 plus the
manifestation frames; method granularity only), `full` (with rings) and
`Rbody` (every line of the body of each developer-changed method; **line
granularity only** — at method granularity it would be `R0` itself). So the
line rows carry three variants and the function rows two, e.g.
`rcc__line__Rbody__buggy__dyn`, `rcp__line__Rbody__patched__dyn`,
`csm__line__Rbody__na` and `rcr__line__Rbody__na` (the last one measured
against P's line set, like every other RCR), with the region's size under
`sizes.R_line['Rbody']`. The branch rows carry the same three variants,
since they count the same line sets. `Rbody` is read from `root_cause.json`'s
`body_lines`; a leg whose file predates that key emits none of these keys,
and `available.root_cause_body_lines` says which case a leg is in.
`rcr_cross__…` is the ring-of-R̂ by ring-of-P count table. Every line also
carries `available` (which inputs existed), `sizes` (|P|, |R̂|, |F| and
their per-ring counts, plus `F_method_probes` and `F_frame_added`, the two
provenances of F per build — the probes' own count, and how many methods
only a stack frame proved ran (section 3.3); `trigger_gate_passed` and
`trigger_gate_missed`, the gate's verdict for this bug and the seeds it
did not reach, both `null` when the gate was not run (section 3.2); plus
`F_kind`, which records per build slot which
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
`branches`, per build slot, which now holds the whole-build
branch counters (`covered`, `total`) it always did plus, beside them,
`source` (the `branches_from` flag), `lines_with_branches`, and the
per-set outcome totals the branch granularity divides — `all_lines`
(RCP's denominator), `R` per R̂ variant and `P`, each `{taken, total}`;
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
from a measured run by `paper_tables.py`. Both carry three granularities:
Table 3 has a Function, a Line and a Branch row per bug class, and Table 4
has a Function-level, a Line-level and a Branch-level column pair.

```bash
# from src/, on any machine that has the archived run directory:
python -m java.measurements.paper_tables \
    --hr <conditioned run_dir> \
    [--hn <naive run_dir>]      # without it every H_N cell prints an en dash
    [--fkind dyn|stat]          # which kind of F(H) the RCC/RCP/PSC columns read
    [--build buggy|patched|compiled]   # i.e. which harness set
    [--rvar R0|R1|full|Rbody]   # which root-cause region variant
                                # (Rbody is line-only: the Line and Branch
                                #  rows read it, the Function rows fall
                                #  back to R0)
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
| CSM_g(H) | `csm__<gran>__<rvar>__na` | crash sites, not coverage; **no branch form** — a crash site is a stack frame, so the Branch row's CSM cell is always a dash |
| F1(H) | none — counted from each leg's `outcome` | caught/missed/false_alarm/clean = TP/FN/FP/TN, overfitting is the positive case |
| ℝ | `<rvar>` = `R0` by default: the methods the developer changed | section 3.2 |
| function granularity | `<gran>` = `method`, rendered "Function" | |
| line granularity | `<gran>` = `line`, rendered "Line" | |
| **edge granularity** | `<gran>` = `branch` — **rendered "Branch", never "Edge"** | branch outcomes on the line sets: decision-point edges only, the nearest observable stand-in (sections 2 and 4.3). A full control-flow-edge version is future work |
| H_N / H_R | the `--hn` / `--hr` run directory | |
| n | bugs and legs per bug class, in each table's notes | |

**What prints an en dash.** The RCR difference (RCR judges the
patch-derived set, which is built before any harness exists, so H_R − H_N
is not defined), CSM for semantic bugs (nothing crashes in the library, so
there is no site to match), CSM on the Branch row (a crash site is a stack
frame, not a branch outcome), every cell of an arm that was not given, and
any ratio whose denominator was empty. Table 3's F1 column belongs to a
(harness set, bug class) rather than to a granularity, so it is printed on
the Function row and spans the three granularity rows; Table 4's RCR spans
the two harness columns for the same reason — unless the two runs' patch-derived sets
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

## 7. The judge's view

**This section is evaluation only.** Nothing in it feeds back into the
pipeline or into the baseline. It reads two finished artifacts and prints a
number; it writes nothing into a leg and no other module imports it.

`src/baseline_llmjudge/` is the comparison target: it answers the same
question about the same patches with the same model and the same
pre-execution evidence, in one shot, without running anything. Because it
is shown evidence, it has a neighbourhood too — the set of methods its
prompt put in front of the model. Call that set J.

RCR (section 4) asks how much of the developer's region the pipeline's
model was even told about: `|R̂ ∩ P| / |R̂|`. `judge_view.py` asks the same
question of the baseline, against the *same* `root_cause.json`:

```
RCR_judge = |R̂ ∩ J| / |R̂|
```

Same granularity (method), same three R̂-variants, same ring breakdown,
same `MethodIndex` matching, same JDK and mislabelled-receiver filters, and
the same identity space (the primary build's coverage method list when the
leg has one). The only thing that changes is which set plays P. The output
keys are the ones `metrics.py` writes (`rcr__method__full__na` and so on),
so a judge row and a pipeline row can be read by the same code.

### 7.1 Where J comes from

A baseline record does not store the evidence text it sent to the model. It
stores a digest of it and a small audit summary, so J is built from one of
two sources and every row says which one it used.

**From the record alone** (`evidence_source: facts`). The fields read are
`evidence_facts.touched_functions`, `.package`, `.modified_files` (and, when
the facts are missing, the `touched_function:<name>` block names in
`parity_manifest.blocks`). That gives the seed ring: the methods the patch
changed, spelled `<package>.<class of the modified file>.<name>` with
unknown parameter types. It gives nothing else, because the record keeps
only `evidence_facts.reachable_count` — how *many* reachable methods the
evidence carried, never which. A facts-only row therefore measures the seed
ring honestly and reports that count beside an empty callee ring.

**From the rendered evidence text** (`evidence_source: text`). The baseline
caches its rendered evidence per patch (`<cache_dir>/<patch stem>.json`,
field `text`), and the record carries `evidence_sha256`, the digest of
exactly that string. A cache entry is used **only when its digest matches**:
the cache is rebuilt in place whenever the extraction changes, and on the
runs in this repository the semantic cache still matches its records byte
for byte while the crashing cache no longer does. Using an unverified entry
would credit the judge with a neighbourhood it never saw, so it is refused
and the row falls back to the facts. With the text, J carries all three
rings, read out of the blocks the renderer emits:

| ring | block | how the name is recovered |
|---|---|---|
| seed | `` Function `name`: `` + its `<signature>` | the signature gives the real parameter types |
| caller | each `<xref>…</xref>` | the calling method's *source*; the declaring class is not in it, so the ref is class-less and can only be matched by (name, arity) — the same limitation `patch_derived` has for P |
| callee | each `- <label>` in `<root_cause_reachable>`, and each `<callee name=… from=…>` declaration | display labels, so mostly no parameter list, so matched with arity ignored; depth is always 1, since the evidence records no distance |

Two things are deliberately left out of J. The semantic evidence also
carries class skeletons and a state-coupling block, which name further
methods; they are class-level context rather than a caller or reachable
list, and `patch_derived` does not read their counterparts into P either.
Conversely the `<callee>` declarations *are* counted here and are *not* read
into P, so J can be a few methods wider than P on that block alone. Every
row carries `shown_sources`, which says how many members came from each
block, so any number can be re-read with a source removed.

### 7.2 What the numbers do and do not mean

A judge set is built from a rendered prompt, and a pipeline P is built from
the analysis step's own dump, so the two are not measured with equal
precision. Three things follow, and each is reported rather than hidden:

- The seed's class is guessed from the modified file, so a seed in a nested
  class can miss. `matching.*.j_unmatched` counts every shown name R̂ had
  nothing for.
- A name shown without a parameter list has *unknown* arity, not zero, so it
  is matched on (simple class, name) with arity ignored. That is looser than
  the pipeline's own matching, and when the class overloads the name it is
  ambiguous in the build's whole method list and resolves to nothing — which
  would read as "the judge was never shown the developer's method" when what
  happened is that the evidence spelled the method without its types. Such a
  name is therefore retried against R̂ alone and counted as
  `matching.*.j_by_name_in_r`, so a row can be re-read with the retried
  members removed. The retry fails closed: a name that R̂ itself overloads
  stays unmatched. It can credit the judge wrongly in one case — it was
  shown a *different* overload from the one the developer fixed — and the
  counter is there so that case can be found.
- The prompt shows at most `MAX_REACHABLE_IN_PROMPT` callees, and the
  renderer prints how many it dropped. That number is reported as
  `shown_sources.reachable_omitted`, so a low RCR_judge on the `full`
  variant can be read against the cap rather than against the model.

### 7.3 Running it

```bash
# from src/, on any machine that has both artifacts:
python -m java.measurements.judge_view \
    ../results/<baseline run>/records.jsonl \
    ../runs-archive/runs/<measured run> \
    [--evidence_cache ../results/llmjudge_cache_semantic] \
    [--out judge_rcr.jsonl]
```

Records are paired with legs by (project, bug id, repair tool, label). When
two legs share that key — one bug with two patches from one tool on the same
side — the patch file's stem breaks the tie, and a tie that stays unbroken
is reported as `ambiguous` rather than resolved by guessing. The table
prints RCR_judge next to the pipeline's RCR for the same leg, read from that
run's `metrics.jsonl`. A record with no matching leg, no root-cause file or
no evidence still gets a row: `available` is false and `reason` says which
input was missing. `--out` writes one JSON object per record; without it
nothing is written at all.


## 8. Relation to `src/metrics`

There are two implementations of root-cause coverage in this repository,
and they are kept apart on purpose.

`src/metrics` is the smaller and older one: RCC at method level for one
harness set, on one bug at a time, plus the sweeps that drive it end to end
(`sweep.py` for the region and the gate, `rcc_sweep.py` for the whole
experiment). Its region comes from `execution.diffcov`, its F(H) is read
through fuzz-introspector's JaCoCo loader, and its method identity is
`metrics.keys.MethodKey`. It is what produced
`results/rcc_hr_crashing_holdout_*`.

`src/java/measurements` — this package — is the superset: five metrics, not
one; three granularities (method, line, branch); the ring-tagged regions
R̂₀/R̂₁/R̂body and the caller/callee neighbourhood; the kept and all-compiled
harness sets; the naive arm and the H_R − H_N delta; and the paper's tables.
It parses the JaCoCo XML itself and its identity is `locations.MethodRef`.

**What was ported from `src/metrics` into this package**

| piece | there | here |
|---|---|---|
| the frame repair of JaCoCo's probe miss | `reached.reached_from_stack` | `coverage.frame_methods` / `repair_from_frames`, wired into `collect_leg` per build (section 3.3) |
| the triggering-test gate | `collect.trigger_coverage` + `rcc.trigger_gate` | `root_cause.trigger_gate`, which **calls hers** to run the tests and does the matching here (section 3.2) |
| the fixed-input-budget measurement pass | `collect.harness_coverage` | `coverage.remeasure_leg`, which **calls hers** to run the harnesses (section 3.3.2) |
| the population table | `sweep.py`'s summary | `root_cause.population_check` |
| the JaCoCo jar and the Defects4J home | `config.JACOCO_CLI_*`, `config.D4J_HOME` | the same constants, read by `coverage.ensure_jacoco_cli` and the CLI's `--d4j_home` default |

The two runners were reused rather than rewritten because both are *how a
process is started*, not *what a number means*: which flags a measurement
pass must pass to Jazzer, and how to reach a forked test JVM with the JaCoCo
agent. Rewriting them would have created a second place for those flags to
drift. The matching, the region and the metric stay here.

**The import rule is one-way.** `java.measurements` may import `metrics`;
`metrics` must never import `java.measurements`. The reason is the firewall
of section 3.2: `root_cause.py` is the only module in the repository allowed
to read a developer fix, and `tests/test_measurements_firewall.py` enforces
that nothing the pipeline can reach imports it. `metrics` is imported by the
sweeps, which the pipeline's own runner is invoked from, so an edge from
`metrics` into this package would put the quarantined module one import
closer to the pipeline. Both imports here are made inside the function that
needs them, so importing `java.measurements.coverage` does not drag the
sibling package (or Defects4J, or Jazzer) in with it.

**Do the two agree?** On the one real dataset both have run —
`results/rcc_hr_crashing_holdout_20260904_001615`, nine scored crashing
bugs — RCC agrees on every bug (1.0 everywhere) and the frame repair
recovers the same two methods on Math-70, the one bug that needs it.
|F(H)| does *not* agree everywhere: on four of the nine bugs `src/metrics`
counts one method more than we do. The difference is fuzz-introspector's,
and it is a false positive rather than something we lose. Its loader decides
which lines belong to a method by taking, from the method's declaration
line, as many entries of the source file's line list as the method's LINE
counter says the method has; that window runs past the method whenever the
two disagree, and a covered line belonging to a *later* method is then
credited to the earlier one. In all four cases the extra method is one
JaCoCo's own METHOD counter reports as never executed
(`StringUtils.<clinit>` on Lang-16 and Lang-20, and the uncovered
`UnivariateRealSolverImpl.<init>` overload that shares its declaration line
with a covered one on Math-70 and Math-85). We count a method when JaCoCo
says it was executed. `tests/test_measurements_crosscheck.py` pins all of
this, per bug, against the real reports, and fails if the gap moves.
