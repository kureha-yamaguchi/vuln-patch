# Diff-hit instrumentation (`--diffcov`) — 2026-08-09

**Status:** built, unit-tested on the Mac, NOT yet run on the VM.
Implements the parked build spec in `docs/witness-study-2026-08-08.md` §7,
under the §5 firewall. Serves caveat (b) in that doc and in `docs/plan.md`
lines 1809–1831.

**Target:** the patched-build materialisation station —
`PatchedProjectBuilder.build_patched_dir` in `src/java/execution/fuzz_runner.py`,
in the gap between "the patch applied" and `defects4j compile`. Collection
hangs off `run_jazzer` / `FuzzRunner._run_one` in the same file; the record is
written by `_record_diffcov` in `src/java/run.py`.

**Failure mode it measures:** a harness compiles, its oracles are sound, it
runs the full fuzz budget against the patched build — and stays quiet because
no generated input ever ENTERED the method the patch changed. From the outside
that is indistinguishable from "the patch fixed the bug". Every recall number
we have is compatible with both readings, and the witness study explicitly
could not tell them apart ("coverable ≠ reached"). Counting entries into each
patch-changed method separates them.

**MEASUREMENT ONLY, this iteration.** The counts are written to the run
artifacts and read by humans. They are not in any prompt, not in the relation
verifier's evidence, not in any gate, screen or verdict. `tests/test_diffcov.py`
has a test (`test_c_diffcov_is_measurement_only`) that greps the prompt,
verifier and judge modules for the word — it is the thing that has to be
deleted first if anyone ever wants to change that.

---

## 1. Changed-method mapping rules

`diffcov.changed_methods(patch_text, root_dir)` — mechanical, bug-agnostic,
reads only the patch text and the post-patch sources.

1. **Line numbers come off the `+` side of the diff.** The tree being
   instrumented is the patched one. Added lines are recorded at their
   post-patch position; a hunk that only DELETES records the post-patch line
   the deletion collapsed onto, so a pure-deletion patch still maps to its
   enclosing method. (This is the one place it differs from
   `bug_context/analysis.py`, which needs the buggy tree and so reads the
   `-` side.)
2. **The hunk stream is parsed by `fuzz_runner._file_sections`** — the counted
   parser that already rejects truncated and out-of-order patches. No second
   diff parser enters the codebase. drr's `/src/...` prefixes and the usual
   `a/`, `b/` are all stripped; `+++ /dev/null` (deleted file) maps to nothing.
3. **Each changed line maps to the smallest declaration whose CHARACTER RANGE
   contains it** (javalang for the AST, brace/paren matching for the range).
   Character ranges, not line ranges, so a method of an anonymous or nested
   class is strictly inside its outer method and the inner one wins — a patch
   landing in an anonymous class is attributed to the anonymous method.
4. **Constructors count as methods.** So do methods of nested and anonymous
   classes.
5. **Overloads are separated by the parameter signature.** The method id is
   `<fq.Class>#<name>(<Type,Type>)` with erased simple type names —
   `org.jfree.data.general.AbstractObjectList#indexOf(Object)`,
   `org.example.Widget#indexOf(String,int)`. A constructor uses its class's
   simple name as the method name. Arrays keep `[]`, varargs keep `...`,
   generic arguments are dropped (erasure is enough to tell overloads apart
   and keeps the id a stable string key).
6. **Hunks with no enclosing method are RECORDED, not dropped.** Field-only,
   import-only and class-level hunks, abstract/native declarations, files that
   are not `.java`, files javalang cannot parse, and post-patch files that are
   missing all land in `plan.unmapped` with a reason. Without that, an empty
   diffcov reads as "nothing was reached" when it actually means "there was
   nothing to count".

## 2. Instrumentation

`diffcov.instrument_patched_dir(patched_dir, patch_path)` rewrites the patched
WORKING COPY in place, after `_apply_patch` and before `defects4j compile`.

- One `vulnpatch.DiffCov.hit("<id>");` at each changed method's entry.
- **Inserted inline, with no newline**, so every line in the file keeps its
  number. Stack traces, the trigger-test safety net and any later read of the
  patched tree still line up with the diff.
- **Constructors: the call goes AFTER an explicit `this(...)`/`super(...)`.**
  Java requires that invocation to be the first statement; a counter in front
  of it does not compile.
- Abstract, interface and native declarations have no body and are skipped
  (recorded as unmapped).
- One generated `vulnpatch/DiffCov.java` per build, written into the source
  root derived by stripping a changed file's own package path off its own path
  (`source/org/jfree/X.java` + `package org.jfree;` → `source`). No
  `defects4j export` call, works for every layout in the dataset. Exactly ONE
  copy: a second under another root that the same javac invocation also
  compiles is a duplicate-class error.
- Instrumentation is best-effort. A failure prints and leaves the build
  uninstrumented rather than costing the run its patched build.

**Deviation from the spec's letter, forced by the dataset.** The spec asks for
`ConcurrentHashMap<String,LongAdder>`. Several Defects4J projects compile at
their own historical `-source` level (1.3/1.4), under which **generics are a
syntax error**. The generated helper therefore uses RAW `ConcurrentHashMap`
plus `LongAdder` (the Java 8 CLASS still links, because the JDK running the
build is modern; only the source LEVEL is old), and avoids for-each,
autoboxing, annotations, the diamond, `StringBuilder` and static imports. It is
also strictly ASCII — ant compiles with the platform charset, and a non-ASCII
byte in a comment fails the whole build on an ASCII locale.

Semantics-neutrality of the hot path: `hit()` is one map lookup plus one
`LongAdder.increment()`. Every id is pre-registered in the static initializer,
so a miss is a no-op rather than an insert — no allocation, no I/O, no throw.
The static initializer is wrapped in `catch (Throwable)`: an initializer that
threw would turn every instrumented class into a `NoClassDefFoundError` and
change what the run measures.

## 3. Flush vs shutdown hook — decision and why

**Decision: flush to a FILE on a timer (default 2 s), AND print to stderr from
a shutdown hook. At collection time the file wins; stderr is the fallback.**

Grounded in two things in `fuzz_runner.py`:

1. `run_jazzer` runs Jazzer as
   `subprocess.run(cmd, ..., timeout=timeout_seconds + 15)`. On
   `TimeoutExpired`, `subprocess.run` **kills** the child — SIGKILL on POSIX.
   No JVM shutdown hook runs. And the margin is only 15 s over libFuzzer's own
   `-max_total_time`, so a JVM that is slow to unwind gets killed on a
   perfectly ordinary run, not just a hung one. `JazzerOutcome.timed_out` is a
   routine outcome in this pipeline, not an error case.
2. The other exit is a finding. `config.JAZZER_CRASH_EXIT_CODE = 77`, and
   `_run_one` fuzzes with `--keep_going=8`, so a patched-side run that fires
   ends via libFuzzer's native exit path. That path does not run JVM shutdown
   hooks either.

Between them, the two exits that matter most — timeout on a quiet leg, finding
on a firing leg — are exactly the two where a shutdown hook is unreliable. A
shutdown-hook-only design would lose the measurement precisely on the legs the
measurement exists for. So the counters are dumped to
`<harness_dir>/diffcov.out` by a daemon thread every `DIFFCOV_FLUSH_SECONDS`
(write to `.tmp`, then rename), and the collector prefers that file. The hit
path stays I/O-free — the timer thread does the writing, not `hit()`.

Cost of the choice: up to one flush interval of counts is lost to the SIGKILL.
That is acceptable for a reach/no-reach reading, where the question is "is this
zero?" and not "is it 41 or 43".

The file path is handed to the JVM through the `VULNPATCH_DIFFCOV_OUT`
environment variable rather than a `-D` flag, so the java command line is
byte-for-byte unchanged. `run_jazzer` passes `env=None` when the flag is off.

## 4. Artifact schema

Per Jazzer execution, in `result.jsonl` under `diffcov` and as a
`method=diffcov` trace event:

```json
{"diffcov": {"org.example.Widget#indexOf(Object)": 0,
             "org.example.Widget#scale(double[],int)": 41},
 "phase": "patched-fuzz",
 "harness": "attempt_003"}
```

`result.jsonl` also carries the plan once per leg:

```json
{"diffcov_methods": {
   "methods": [{"method_id": "org.example.Widget#indexOf(Object)",
                "file": "source/org/example/Widget.java", "line": 21}],
   "unmapped": [{"file": "source/org/example/Widget.java", "line": 4,
                 "reason": "no enclosing method (field/import/class level)"}]}}
```

The same plan is written into the build as
`<patched_dir>/.diffcov_methods.json`, so a cached (idempotent-skip) patched
directory can hand it back without re-deriving it.

Every changed method appears in every record, **including the ones with
`hits=0`** — the zeros are the signal. `phase` is `patched-fuzz` (the
`FuzzRunner.run_all` pass); the buggy-side gate, the keep-going re-fuzz and the
relation replay are deliberately NOT instrumented, since diffcov's question is
about the patched build.

## 5. How to enable a measurement run

```
uv run python src/java/run.py ... --diffcov
```

or `VULNPATCH_DIFFCOV=1` in the environment (`config.DIFFCOV`);
`DIFFCOV_FLUSH_SECONDS` tunes the flush interval.

Default is OFF, and off means zero code path: no instrumentation, no
environment variable on the subprocess, no key in `result.jsonl`, and the
patched build is byte-for-byte what it always was. The frozen guard fixtures
and every historical baseline are untouched.

**The instrumented build gets its own directory** —
`<checkout>_patched_<patch-stem>_diffcov`. `build_patched_dir` is idempotent on
directory existence alone, so sharing the path would let a cached
uninstrumented tree be reused as "already built" and the measurement would come
back silently empty. Consequence to budget for: a `--diffcov` run that also
uses `--replay_relations_on_patched` materialises TWO patched trees (that
station builds its own, uninstrumented). Disk, not correctness.

## 6. What is NOT verified yet

There is no JDK on the Mac, so nothing here has been compiled. The Mac-side
checks are: javalang parses the instrumented sources and the generated helper,
the helper is pure ASCII and generic-free, and the counter lands at the entry
of exactly the right methods (`tests/test_diffcov.py`, 23 tests). The first VM
run must confirm three things the Mac cannot:

1. `defects4j compile` accepts `vulnpatch/DiffCov.java` on the oldest
   `-source` project in the suite (Chart / Lang), and picks the new file up
   from the source root at all.
2. The trigger-test safety net still passes on the instrumented patched build
   — proof the injected calls are semantics-neutral end to end.
3. `diffcov.out` is actually written and non-empty on both a timed-out leg and
   a firing leg. If it is empty on the firing leg, the finding exit is killing
   the JVM faster than the first flush and the interval needs to drop.

## 7. Files

| file | role |
|---|---|
| `src/java/execution/diffcov.py` | new — mapping, instrumentation, helper generation, `[diffcov]` parsing |
| `src/java/execution/fuzz_runner.py` | `--diffcov` build hook, `run_jazzer(diffcov_out=…)`, `_collect_diffcov`, `diffcov` on `JazzerOutcome`/`FuzzRunResult` |
| `src/java/run.py` | `--diffcov` flag, `_record_diffcov` (the collection boundary) |
| `src/config.py` | `DIFFCOV`, `DIFFCOV_FLUSH_SECONDS` |
| `tests/test_diffcov.py` | 23 tests across the three steps |
| `tests/fixtures/diffcov_*.patch`, `diffcov_widget.java`, `diffcov_gadget.java` | real `diff -u` fixtures: multi-hunk, constructor, overload, multi-file, field/import-only |
