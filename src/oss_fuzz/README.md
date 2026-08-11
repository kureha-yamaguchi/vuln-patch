# OSS-Fuzz / libFuzzer front-end

Finds bugs that a security fix left behind.

For an OSS-Fuzz project, this picks a recently disclosed bug, checks out the code as it was **just before the fix**, and asks an LLM to write libFuzzer harnesses that crash there. Harnesses that do are then run against **today's code**. If one still crashes, the fix missed something — we call that a *sibling bug*.

This is the C/C++ version of `src/java` (Defects4J + Jazzer). It shares that pipeline's LLM code and, importantly, its steering logic, so the research method stays the same in both. It does not use the out-of-date `src/linux`.

## How a run works

`oss_fuzz/run.py` is the only entry point. Everything happens in one process, in this order. Cheap checks come first, so a run that cannot possibly work fails before it costs you a clone, a Docker image, or LLM tokens.

| Step | File | What happens |
|---|---|---|
| 1. Check the checkout | `ossfuzz.py` | Is `$OSS_FUZZ_DIR` really an oss-fuzz clone? Takes milliseconds. |
| 2. Find a project *(optional)* | `targets.py` | With `--list-candidates` / `--auto-project`: scan the checkout for C/C++ projects that have a disclosed bug. |
| 3. Check the project | `ossfuzz.py` | Read its `project.yaml` and `Dockerfile`. Reject anything not C/C++, not libFuzzer, or missing the sanitizer. A python project or a typo stops here. |
| 4. Pick a bug | `osv.py` | Ask OSV for the project's bugs, newest first. Keep ones with a fix commit. Pull the crash type and crash stack out of the text. |
| 4b. Classify it | `bugclass.py` | Crashing or semantic — i.e. will a sanitizer report a sibling of this bug, or must the harness notice it itself? Decides the prompt's oracle contract, a pre-build gate, the fuzzing flags and how a HEAD finding is reported. |
| 5. Get the code | `ossfuzz.py` | Clone the project's own repo, then make two checkouts: `vuln` (the commit before the fix) and `head` (latest). |
| 6. Work out what the fix touched | `analysis.py` | Diff the two commits, list the changed functions, and expand them into the surrounding call graph. This is what the prompt steers toward. |
| 7. Decide how to compile | `ossfuzz.py` | Choose `crib` or `overwrite` (see below) — before pulling the Docker image, so an impossible choice fails early. |
| 8. Build the image | `ossfuzz.py` | `helper.py build_image`. Optionally replay a known crashing input with `--reproducer`. |
| 9. Generate and test harnesses | `campaign.py` | The main loop, described below. |
| 10. Run the survivors on HEAD | `run.py` | Rebuild each accepted harness against the latest code and fuzz it. A crash here is a sibling bug. |
| 11. Report | `run.py` | Print a summary; optionally append a JSON line with `--results-json`. |

**Step 6 can reject the bug.** Some fix commits touch no source code at all,
which would leave the prompt with nothing to steer toward. When that happens the
run moves on to the next-newest bug instead (`--max-target-tries`).

**Step 9, the main loop.** Repeat until `-n` harnesses are accepted or `-m`
attempts are used up:

1. `prompts.py` builds the prompt from the fix diff, the crash stack, and the
   list of functions to aim at.
2. `llm.py` returns a reply; the code block is pulled out of it.
3. The harness is put into the `vuln` checkout and built with
   `helper.py build_fuzzers`.
4. It is fuzzed briefly (`--verify-timeout`) against that vulnerable build.

A harness is **accepted only if it triggers on the vulnerable build** —
compiling is not enough. If the build fails, the compiler errors go back to the
model as a fix-it turn. If it builds but does not trigger, the next prompt is
steered somewhere new, away from what earlier harnesses already covered. If the
*build environment* is broken (nothing was ever compiled) the run stops
immediately rather than blaming the model.

Exit codes: **0** = ran fine, no siblings. **2** = `RUN ABORTED`, the
environment is broken — this is not a result about the fix. **3** = confirmed
siblings found. **4** = only unconfirmed oracle claims (see below).

## Crashing vs semantic bugs

Not every OSS-Fuzz bug is a crash, and the difference decides what a *working
harness* even is. The pipeline asks one question about each record — **what
would notice this bug?** — and branches on the answer (`bugclass.py`, the C
analogue of the Java front-end's `classify_exceptions`).

| Bug kind | Oracle | Example crash types | What the harness must do |
|---|---|---|---|
| crashing | `sanitizer` | Heap-buffer-overflow, Use-after-free, Undefined-shift, SEGV, Timeout, Direct-leak | Reach the fault. The runtime supplies the verdict. |
| crashing | `project-assert` | `ASSERT: idx < len`, CHECK failure, Fatal error, Unreachable code | Reach a *state* the invariant does not hold in. The library aborts by itself. |
| semantic | `harness` | Incorrect-result | Carry its own check: nothing else will ever notice. |
| unknown | `sanitizer` | (record has no crash type) | Same as crashing, but printed as `unknown` and hedged — see below. |

`oracle` is the fine-grained fact and is what the prompt reads. `kind` is a
strict coarsening of it — **`semantic` means exactly "the harness must supply
the verdict"** — and exists so that cross-language aggregation compares like
with like. `BugClass.__post_init__` enforces the coarsening, because the reverse
was a real bug: `project-assert` was filed as `semantic` on the
reasonable-sounding grounds that a violated invariant is a logic error rather
than memory corruption. True, but a different question — the library still
aborts by itself, the trigger gate still works unmodified, and filing it as
semantic made `--skip-semantic` silently discard a whole class of workable bugs.
Java calls the same shape (an escaping invariant-check throwable) crashing.

**`unknown` deliberately diverges from Java.** Java treats an undeterminable
bug as semantic; this front-end treats it as crashing. The corpora have opposite
base rates — Defects4J is dominated by wrong-value bugs, the OSS-Fuzz corpus by
memory-safety and UB — so guessing "semantic" here would open the prompt with
"THIS BUG DOES NOT CRASH" for the large majority of records and make the
pre-build oracle gate bounce sanitizer harnesses that need no oracle. The guess
is hedged rather than bet on: an `unknown` record takes the crashing template
but is *also* asked for an optional tagged relation, which costs nothing if the
bug does crash and is the only thing that can save the run if it does not.

Getting this wrong is expensive in a way that looks like a result. A harness for
an `Incorrect-result` bug written to the crashing template compiles, runs, and
returns 0 for every input — so the trigger gate rejects it, every attempt, until
the budget is gone, and the run reports "0 siblings" as if the method had been
tested.

What changes per kind:

- **The prompt.** Wrong-value bugs get a mandatory oracle contract instead of
  the optional metamorphic nudge: pick a relation true of *any* correct
  implementation (round-trip, idempotence, two API paths that must agree, a
  documented postcondition), compute both sides from real library calls, and
  report violations as `[oracle:<id>]` + `abort()`. Invariant bugs are told the
  opposite — don't write a check, the library has one; build the state that
  breaks it, and mind that `-DNDEBUG` would delete the asserts entirely.
- **A pre-build gate.** For harness-oracle bugs, a harness with no tagged,
  aborting alarm is rejected from its source before Docker is invoked
  (`campaign.oracle_tag_missing`). It cannot fail, so building it would spend a
  compile plus a verify run to reach a verdict indistinguishable from an honest
  miss.
- **Signatures.** Every oracle alarm and every failed assert reaches libFuzzer
  as the same `deadly signal`. Since the variant-analysis steering is fed the
  signatures found so far and told to aim elsewhere, they get their own forms
  (`oracle:<id>@frame`, `assert:<expr>`) — otherwise the model is told it has
  covered ground it has not.
- **Fuzzing flags.** Timeout/OOM bugs only reproduce under ClusterFuzz's
  per-input limits (`-timeout=25 -rss_limit_mb=2560`); libFuzzer's own defaults
  are loose enough that the bug never fires.
- **The report.** A crash on HEAD that a *sanitizer* (or the project's own
  assert) reports is a sibling. A crash that only the harness's own oracle
  reports is a **claim**: true if and only if the relation it asserts is true,
  which nothing in this pipeline can establish. Claims are listed separately,
  excluded from the sibling count, and exit 4 rather than 3. Read the harness
  before reporting one upstream.

`--bug-kind {auto,crashing,semantic}` overrides the classification;
`--skip-semantic` skips records that *nothing at run time would report* during
target selection (before the clone), mirroring the Java front-end's
`--skip_semantic`. It keys off `needs_harness_oracle`, so project-assert bugs
stay in scope — they abort by themselves, which is what the flag is really
asking about. The `found_by`
field on every finding records which oracle *actually* fired, which is not
always the predicted one — a harness aimed at a wrong-value bug that trips ASan
found a memory bug instead. That is a real finding, but it is not evidence
about this fix.

### What it shares with the harness-generation pipeline

The LLM half is not rewritten here. It is the same code the Java front-end uses,
called through a deliberately small interface:

| Shared file | How this front-end uses it |
|---|---|
| `src/llm.py` | Send a list of messages, get text back. Nothing else. It is imported only when needed, so `--dry-run` works without an LLM library installed. |
| `src/variant.py` | The steering rule itself: "aim the next harness at a part of the fix's neighbourhood that earlier harnesses missed." `prompts.py` calls it with the reachable functions, the functions already covered, and the crashes already found. Extracted from the Java `PromptBuilder`, which still carries its own copy — see the note below. |
| `src/config.py` | All settings and their environment-variable defaults. |

What is specific to C/C++ and therefore lives here: the prompt wording and byte
handling for `LLVMFuzzerTestOneInput` (`prompts.py`), the diff analysis
(`analysis.py`), the crashing/semantic classification (`bugclass.py` — same
split as the Java front-end, but read off ClusterFuzz crash types rather than
JUnit throwables), and everything that builds and runs harnesses (`ossfuzz.py`).

The rule for accepting a harness — crashes the vulnerable build, not just
compiles — is the same as in the Java pipeline, and `campaign.py` is the only
thing that decides it. The static analysis only steers; it never decides whether
a harness is good.

One condition is on top of the Java rule: the crash has to be one the set does
not already have. `-n 5` asks for five pieces of *evidence*, and without this a
model that returns the same harness five different ways satisfies it with one —
each copy then costing a HEAD build and being counted again in the sibling
total. The gate compares the crash signature (already computed for steering), so
it is a set lookup, and it fails open on a crash whose signature could not be
read: an unreadable report is never treated as a duplicate, because failing
closed there would let one unparseable crash stall the campaign to
`--max-attempts`. When it fires, the model is told which two moves are open to
it — reach a different fault, or keep the path and add a tagged `[oracle:<id>]`
check — since a gate the model cannot see just spends attempts.

Java has no such gate. It had the analogous one on check *families* and deleted
it (2026-08-06) after 458 evaluations rejected nothing; only its prompt-side
pressure survives. That is the likely outcome here too, which is why this gate
is a string comparison rather than an analysis — cheap enough to be worth
keeping even if it never fires, and it fixes the evidence count either way.

**Steering is not byte-identical across the two front-ends, and no longer claims
to be.** `variant.py` was extracted from the Java `PromptBuilder` so this
front-end would not fork a second copy of the rule, but the Java side was never
switched over to it and has since grown steering this one does not have (a
check-family novelty gate and independent-oracle pressure once any trigger has
been found). So the two share the *heuristic*, not the *text*: a Java result and
a C result are comparable in method, and a C campaign will accept a set of
harnesses that a Java campaign would have pushed harder to diversify. Treat
`variant.py` as this front-end's steering, not as a cross-language contract.

### What it uses from your oss-fuzz checkout

`$OSS_FUZZ_DIR` (or `--oss-fuzz-dir`) must be a real `google/oss-fuzz` git clone
— the code reads files out of it and runs its `infra/helper.py`. This repo's
gitignored `oss-fuzz/` directory is the usual place to put it:
`export OSS_FUZZ_DIR=$PWD/oss-fuzz`.

These are the only paths it touches:

| Path | What we do with it |
|---|---|
| `infra/helper.py` | Run it: `build_image`, `build_fuzzers`, `run_fuzzer`, `reproduce`. All Docker work goes through it; we never call `docker` directly. |
| `projects/<p>/project.yaml` | Read: language, fuzzing engines, sanitizers, repo URL. Also what `--list-candidates` scans. |
| `projects/<p>/Dockerfile` | Read: just the `WORKDIR` line, to skip projects that cannot be built from a local checkout. |
| `projects/<p>/build.sh` | Read every time, to see if there is a compile line worth copying. **In `crib` mode only**, a line is appended and then removed again afterwards. `overwrite` mode never edits it. |
| `build/out/<p>/` | Written by `helper.py` inside Docker. We read the built binary and any `crash-*` files from it. |

Nothing else in the checkout is used or changed. The project's own source code
never goes into it: that is cloned into `$OSS_FUZZ_WORK_DIR`, and the `vuln` and
`head` directories there are handed to `helper.py build_fuzzers <project>
<path>`, which mounts them inside the container. The pipeline also never pulls
or switches branches in your checkout — pin it yourself if you want the project
definitions to stay fixed.

## Getting the harness compiled: `crib` vs `overwrite`

To compile a new fuzz target for a project you normally need to know its include
paths and libraries. Both strategies avoid having to know them.
`--harness-build` picks one (`auto` | `crib` | `overwrite`).

**`crib`** — write a new source file, and copy a compile line out of the
project's `build.sh` (any line using `$LIB_FUZZING_ENGINE`), reusing its flags
and libraries. The original target's own source is dropped from the copied line,
or we would link two `LLVMFuzzerTestOneInput`s and fail. Lines split across
backslashes are joined first (bluez, assimp, boringssl all do this). `build.sh`
is restored afterwards.

**`overwrite`** — replace the contents of a harness file the project already
has, keeping its path and name, and let the project build exactly as it always
does. The build system never knows the file changed, so all the include paths,
flags and libraries come for free. The file is restored afterwards, and
`build.sh` is never touched.

Neither works everywhere, but together they cover most projects. Across the 588
C/C++ projects in a checkout:

| C/C++ projects | count |
|---|---|
| have a compile line to copy | **225** |
| have none (CMake/Meson, or a script inside the project's own repo) | **305** |
| have no `build.sh` at all | 58 |

For those 305 there is nothing to copy, and a generic guessed compile command
has no include paths or libraries, so it cannot build anything the model writes.
And that group is where the bugs are: of 35 projects with the most OSV records,
17 have no copyable line — including **libxml2, curl, openssl, harfbuzz,
wireshark, freetype2, libtiff, openjpeg, expat, zstd, ffmpeg, libwebp**.
libxml2's whole `build.sh` is one line calling a script inside its own repo. So
`overwrite` is what makes the `--project libxml2` example work at all.

`auto` uses `crib` when a compile line exists (it leaves the project's sources
alone) and `overwrite` otherwise. The two barely overlap: projects that keep
their harness in `oss-fuzz/projects/<name>/` have to compile it with an explicit
command (130 of 156 are copyable), while projects whose harness lives in their
own repo mostly let their build system handle it (337 of 432 have nothing to
copy).

Three things `overwrite` has to get right:

- **The binary keeps the replaced file's name**, not ours — the build system
  names it after the file it compiled. Asking `helper.py` to run
  `vp_harness_3` would look for a target that does not exist. If the expected
  name is missing after a successful build, the run stops and lists the names
  that *are* there.
- **The file extension decides the language**, not the project's declared
  language. A C++ body written into a `.c` file will not compile, so the prompt
  follows the extension.
- **Finding the right file to replace.** We look for a *definition* of
  `LLVMFuzzerTestOneInput` — a file that merely declares and calls it is a
  standalone driver supplying its own `main()`, and replacing one builds no
  target at all. Then: prefer a file matching the bug's fuzz-target name, avoid
  vendored directories like `third_party/` (those are a dependency's harness),
  prefer fuzz-related directories, and avoid a `main` stem, which no target is
  ever called. `--base-harness` overrides the choice. HEAD is searched
  separately, since the file may have been renamed upstream.

**The `vuln` and `head` directories are full clones, not git worktrees.** A
worktree's `.git` is a pointer to a directory outside the mounted folder, so
inside the container every git command fails with *"fatal: not a git
repository"*. Projects that read their version from git then break in confusing
ways: coturn's `CMakeLists.txt` runs `git describe`, gets nothing, and dies with
*"set_target_properties called with incorrect number of arguments"*.
`git clone --local` shares the object store, so a second full checkout costs
almost no disk and works fine in the container.

## Setup

You need:

- A local `google/oss-fuzz` clone (`OSS_FUZZ_DIR`).
- Docker, which `infra/helper.py` uses.
- An LLM: `OPENAI_API_KEY`, Azure, or a local server — same as the Java
  pipeline, see `src/config.py`.
- **An x86_64 Linux host** (see *Limits*).

Settings live in `src/config.py` and can all be set by environment variable:

| Var | Default | Meaning |
|-----|---------|---------|
| `OSS_FUZZ_DIR` | `~/oss-fuzz` | the checkout project names are looked up in |
| `OSS_FUZZ_WORK_DIR` | `~/.cache/vuln-patch/oss-fuzz` | where the project's own clones and checkouts go |
| `OSS_FUZZ_SANITIZER` | `address` | used when neither the bug nor `--sanitizer` says |
| `OSS_FUZZ_BUILD_TIMEOUT` | `5400` | cap on one build (emulated builds are slow) |
| `OSS_FUZZ_VERIFY_TIMEOUT` | `120` | seconds to test each harness on the vulnerable build |
| `OSS_FUZZ_FUZZ_TIMEOUT` | `600` | seconds to fuzz each accepted harness on HEAD |
| `OSV_API_URL` | `https://api.osv.dev/v1` | where bugs come from |

## Usage

```bash
export OSS_FUZZ_DIR=~/oss-fuzz OPENAI_API_KEY=sk-...
uv run -m oss_fuzz.run --project libxml2 -n 5 --fuzz-timeout 300
```

The run prints which compile strategy it chose and why:

```
harness build: overwrite fuzz/xml.c in place -> target 'xml' (build.sh has no
compile line to crib, so the project's own build system must compile the harness)
```

Override with `--harness-build {auto,crib,overwrite}`, and point `overwrite` at
a specific file with `--base-harness fuzz/xml.c` (path relative to the project's
repo root) if it picks the wrong one.

### Finding a project to run on

Most of the ~1365 projects in a checkout are not usable: 588 are C/C++ (the rest
are python/go/jvm/rust/js/swift/ruby, which this cannot write harnesses for), and
only some of those have a disclosed bug with a fix commit. List the usable ones
instead of guessing:

```bash
uv run -m oss_fuzz.run --list-candidates --max-projects 60
```

```
project                  lang  advisory           published   crash type
apache-logging-log4cxx   c++   OSV-2026-1234      2026-06-05  Heap-buffer-overflow READ 1
assimp                   c++   OSV-2026-999       2026-06-04  Container-overflow READ 4
...
```

`--auto-project` picks the newest one and runs it. Each candidate costs one OSV
query, so `--candidate-limit` (default 10) stops the scan early and
`--max-projects` limits how many are checked. The ranking is newest-first among
what the scan found, not across the whole checkout.

Pin a specific CVE, or supply a known crashing input to sanity-check first:

```bash
uv run -m oss_fuzz.run --project libxml2 --cve CVE-2022-XXXXX \
    --reproducer ./testcase --sanitizer address
```

### Running a batch of projects

`run_ossfuzz_suite.sh` (repo root) sweeps a list of projects, one run each,
crashing bugs only:

```bash
export OSS_FUZZ_DIR=$PWD/oss-fuzz OPENAI_API_KEY=sk-...
./run_ossfuzz_suite.sh                  # 20 sampled C++ projects, seed 42
./run_ossfuzz_suite.sh libxml2 expat    # just these
./run_ossfuzz_suite.sh -d               # dry run: exercises the whole sweep without Docker or an LLM
./run_ossfuzz_suite.sh -o runs/ossfuzz_20260810_120000   # resume an interrupted sweep
NUM_PROJECTS=5 SELECT_SEED=7 ./run_ossfuzz_suite.sh      # a different sample
```

The project list is sampled. `oss_fuzz.select_projects` draws
`NUM_PROJECTS` from the checkout's eligible C++ projects under `SELECT_SEED`, so
a sweep is reproducible without a file anyone has to maintain:

```bash
cd src && uv run -m oss_fuzz.select_projects        # print the selection and why
```

Eligibility is `OssFuzz.check_support` — language, engine, sanitizer,
`main_repo`, and the Dockerfile `WORKDIR` rule that makes `helper.py` refuse a
local checkout — plus three exclusions of its own: `disabled: true`, OSS-Fuzz's
own test fixtures (`vulnerable-project` and friends ship planted bugs), and
`main_repo` URLs that `git clone` cannot handle (10 C++ projects are hg or svn).
That leaves 378 of the checkout's 590 C/C++ projects at commit `8915eb62`.

Reproducibility is per-checkout: the sample is a function of the seed *and* of
`projects/`, so pulling OSS-Fuzz can change it. Both the seed and the checkout
commit go into the run's `summary.md`; quote them together when reporting a run.

Everything lands in `runs/ossfuzz_<timestamp>/`: `projects.list` (the resolved
selection, written before the first project and reused on `-o` so a resumed
sweep cannot silently re-sample), `logs/<project>.log` for every run, the shared
`results.jsonl`, and `summary.md`. Each project's exit code is kept as its
status, so `infra-error` (2) and `timeout` never get averaged in with a real
`clean` (0). Exit 0 covers two opposite outcomes, so a run that never got a
harness to build is reported as `no-harness` rather than `clean`. It runs projects sequentially on purpose — `helper.py` builds into
one shared `build/out/` tree — so budget hours, not minutes, and start it under
tmux.

The project list is a plain data file; every entry in the default one was
checked for `language: c++`, a Dockerfile WORKDIR that is not `$SRC`, and at
least one usable crashing OSV record.

### Trying it without Docker, network, or an LLM

```bash
uv run -m oss_fuzz.run --project demo \
    --osv-json oss_fuzz/tests/fixture_osv.json --dry-run -n 1 -m 2
```

`--dry-run` prints every external command it would run and uses a fixed stub
harness, so you can check the whole flow before spending real time and money.
`--osv-json` reads bugs from a file instead of the network, which is also useful
for repeatable runs. Under `--dry-run` the project checks become warnings, so the
fixture project (`demo`, which is in no checkout) still runs end to end.

## Why bugs are picked without a CVE

OSS-Fuzz bugs are `OSV-YYYY-NNNN` records and **almost never have a CVE**.
Against the live API, ten major C/C++ projects (libxml2, harfbuzz, curl,
openssl, wireshark, …) return 261 records with not one CVE between them. When a
CVE does exist it is usually attached to the upstream ecosystem entry, not the
OSS-Fuzz one.

So a bug qualifies if it has a **fix commit and a repo**, not if it has a CVE.
`--require-cve` demands one anyway, but on OSS-Fuzz that usually finds nothing.
`--cve <id>` implies it.

These records carry something more useful for steering anyway: the **crash type**
and the original **crash stack**, both read out of the record's text and put into
the prompt. The diff says what changed; the crash stack says where it broke. The
crash type also picks the sanitizer when nothing else does, so a UBSan-only bug
is not run under ASan, where the harness would compile and never crash.

## Tests

```bash
python src/oss_fuzz/tests/test_offline.py     # or: pytest src/oss_fuzz/tests
```

48 tests, no Docker, network, or LLM needed. They cover bug selection, crash
parsing and sanitizer choice, `project.yaml` parsing, the checkout and project
checks, candidate discovery, diff analysis, both compile strategies (including
that `overwrite` restores the tree exactly and never edits `build.sh`), crash
detection, prompt assembly, and that the steering matches the Java pipeline's.
The crashing/semantic split is covered end to end: classification over
ClusterFuzz's crash-type vocabulary, evidence ranking in `finding_oracle`,
signature separation, the pre-build oracle gate firing on semantic runs and
staying out of the way on crashing ones.

## Limits and gotchas

- **x86_64 Linux only.** OSS-Fuzz's base images are amd64. On an arm64 Mac they
  run emulated, and any build that *runs* what it just compiled fails —
  autotools' `configure` does exactly that, so bluez dies with `configure:
  error: cannot run C compiled programs`. Its stock build fails the same way
  with no harness of ours involved, which is how we know it is the platform.
  CMake projects do build. `run.py` warns at startup.
- **Fix commits are not always fixes.** They come from automated bisection. Of 9
  audited bugs, 2 pointed at commits touching no code at all — c-blosc2's was
  *"Add diagrams for the new shared thread pool architecture"*, two image files.
  Those give the prompt nothing to steer toward, so the run moves to the next
  bug. `--allow-empty-context` runs anyway (but then it is not testing the
  method).
- **Only library code is used for steering.** A wide upstream sync also touches
  tests, CLI tools, bindings, and the project's own fuzz harnesses. Telling the
  model to "aim at `LLVMFuzzerTestOneInput`" is nonsense — that is the thing it
  is writing. Those directories are skipped, and `main`, unnamed functions and
  libc calls are dropped.
- **`overwrite` needs an existing harness to replace**, inside the project's own
  repo. The 156 projects that keep their harness in `oss-fuzz/projects/<name>/`
  are mostly copyable instead (130 of 156); the 26 that are neither need
  `--base-harness`. Projects whose harness lives in a *third* repo cloned by
  `build.sh` (bearssl builds against `cryptofuzz`) fit neither strategy, and the
  run says so instead of guessing.
- **79 of 1329 projects can never be used**, whatever their language: their
  Dockerfile sets `WORKDIR` to the shared `/src`, and `helper.py` refuses to
  build those from a local checkout at all. Since this pipeline always builds
  from local checkouts, they are rejected up front. capstone is one.
- **A broken environment is not a result.** `helper.py` exits nonzero both for a
  bad harness and for a broken setup. Treating the second as "your harness did
  not compile" would waste every attempt on a file that never reached a
  compiler and then report zero siblings, so those runs stop after one attempt
  and exit 2.
- **Crashing inputs are usually unavailable.** OSS-Fuzz testcases are embargoed
  until disclosure, so the original input is optional here: the pipeline works
  out its own crashing harnesses from the fix diff. Pass `--reproducer <path>`
  if you do have the testcase.
- **The reachable function list is approximate.** It comes from
  fuzz-introspector's lightweight (no-build) parser, which needs the optional
  extra (`uv sync --extra introspector`). Without it, or on timeout, a simpler
  brace-matching fallback is used, and the run prints which one it used. Either
  way this only steers the prompt — the crash test decides what is accepted.
- **A harness oracle can be wrong.** For semantic bugs the harness asserts a
  relation it chose itself, and a relation that is not universally true fires on
  the vulnerable build *and* on HEAD — which looks exactly like a sibling. The
  pre-build gate only checks that an alarm exists and can stop the process, not
  that it is sound; there is no C-side equivalent of the Java pipeline's
  soundness and attribution judges yet. That is why oracle findings are reported
  as claims and exit 4.
- **HEAD may not build.** If the project's API changed between the fix and now,
  a harness may not compile against HEAD. Those are skipped and reported, not
  counted as clean.

## Responsible disclosure

A crash on HEAD is a live, unfixed bug. Report it to the project and to OSS-Fuzz
through coordinated disclosure, and don't publish the crashing input before a
fix ships.
