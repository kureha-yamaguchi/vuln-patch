# OSS-Fuzz / libFuzzer front-end

Variant analysis for OSS-Fuzz projects: take the most recent **public disclosed
bug** (CVE if there is one — usually there isn't, see *Target selection*),
generate libFuzzer harnesses on the **vulnerable** version (gated so each one
actually crashes there), then run them on **HEAD**. A crash on HEAD is a
*sibling* input the fix failed to cover.

This is the C/C++ analogue of `src/java` (Defects4J + Jazzer). It reuses the
shared LLM backend (`src/llm.py`), config (`src/config.py`), and — importantly
— the identical variant-analysis steering (`src/variant.py`), so the research
heuristic can't drift between the two front-ends. It does **not** build on the
out-of-date `src/linux`.

## Pipeline

| Stage | Module | Job |
|-------|--------|-----|
| Target discovery | `targets.py` | which projects can this front-end drive at all? (C/C++ + libFuzzer + sanitizer + a disclosed bug) |
| Preflight | `ossfuzz.py` | validate the checkout and the chosen project *before* any clone/build |
| Bug selection | `osv.py` | newest public OSV entry → fix commit, repo, crash type/stack, PoC ref |
| Substrate | `ossfuzz.py` | clone repo, self-contained checkouts `vuln`(=fix~1) & `head`, `helper.py` build/run/reproduce |
| Analysis | `analysis.py` | fix diff → touched functions; fuzz-introspector call graph → bounded reachable set (heuristic fallback) |
| Prompt | `prompts.py` | libFuzzer prompt + shared steering (`variant.py`) |
| LLM | `../llm.py` | shared `HarnessGenerator` |
| Harness placement | `ossfuzz.py` | crib a compile line from `build.sh`, or overwrite an existing harness in place |
| Campaign | `campaign.py` | generate → build → trigger-gate on the vuln build |
| Sibling hunt | `run.py` | build accepted harnesses on HEAD, run, report crashes |

Three non-obvious things in the substrate, all learned the hard way:

### Getting a generated harness compiled: two placement strategies

Compiling a brand-new fuzz target for an arbitrary project normally means
knowing its include paths and link libraries. `plan_harness()` picks whichever
way of avoiding that guess the project actually supports, and `--harness-build`
overrides it (`auto` | `crib` | `overwrite`).

**`crib`** — write a *new* source file and append a compile line copied from an
existing `$LIB_FUZZING_ENGINE` command in the project's `build.sh`, reusing its
flags and libraries while dropping that target's own object/source (or we would
link a second `LLVMFuzzerTestOneInput` and fail on a duplicate symbol) and its
`-o`. Commands are continuation-joined first, because bluez, assimp and
boringssl all span several backslashed lines. `build.sh` is edited under
try/finally and restored.

**`overwrite`** — replace the contents of an *existing* harness source in the
checkout, keeping its path and extension, and run the project's own build
completely untouched. The build system compiles the same file it always
compiles and never learns the contents changed, so every include path, flag and
library comes for free. Restored under try/finally; `build.sh` is never edited.

Measured over the 588 C/C++ projects in this checkout (the same set
`list_projects` returns, so the counts agree with the preflight):

| C/C++ projects | count |
|---|---|
| have a cribbable compile line | **225** |
| have **none** (CMake/Meson/a script inside the upstream repo) | **305** |
| have no `build.sh` at all | 58 |

For the 305 there is nothing to copy, and the generic fallback
(`$CC $CFLAGS harness.c $LIB_FUZZING_ENGINE -o …`) has no include paths and no
libraries, so it cannot compile whatever the model writes. That set is not the
tail of the corpus — it is where the bugs are. Of 35 high-OSV-volume projects,
17 have no cribbable line, including **libxml2, curl, openssl, harfbuzz,
wireshark, freetype2, libtiff, openjpeg, expat, zstd, ffmpeg, libwebp**.
libxml2's entire `build.sh` is the single line `fuzz/oss-fuzz-build.sh`, which
delegates to a script inside the upstream repo. So `overwrite` is what makes
the documented `--project libxml2` example buildable at all.

`auto` prefers `crib` when a compile line exists (proven, and it leaves the
checkout's sources untouched) and falls back to `overwrite` otherwise. The two
are near-complementary rather than redundant: projects that ship their harness
inside `oss-fuzz/projects/<name>/` must compile it with an explicit command
(130 of 156 are cribbable), while projects whose harness lives upstream mostly
let their own build system do it (337 of 432 are not cribbable).

Three things the overwrite path has to get right:

- **The binary keeps the *replaced* target's name**, not the generated one —
  the build system names it after the file it compiled. `HarnessPlacement.
  runtime_name()` is what `run_fuzzer`/`reproduce` must be given; asking
  helper.py for `vp_harness_3` would look for a target that does not exist.
  If the expected name is absent from `$OUT` after a successful build, the run
  aborts as an infrastructure problem (it cannot fix itself across attempts)
  and lists what *is* there.
- **The extension is fixed by the file being replaced**, so it — not the
  project's `language` — decides whether the model is asked for C or C++. A C++
  body written into a `.c` file does not compile.
- **The harness is located by `LLVMFuzzerTestOneInput`**, ranked by agreement
  with the OSV fuzz-target name, then away from vendored trees (`third_party/`
  et al ship a *dependency's* harness), then toward fuzz-ish directories.
  `--base-harness` overrides. HEAD is planned separately from the vulnerable
  commit, because the file may have been renamed upstream in between.

**Checkouts are local clones, not git worktrees.** A worktree's `.git` is a file
pointing at `<repo>/.git/worktrees/<name>`, which is outside the directory
`helper.py` bind-mounts as `$SRC/<project>` — so inside the container every git
command fails with *"fatal: not a git repository"*. Any project that stamps a
version from git then breaks far from the cause: coturn's `CMakeLists.txt` runs
`git describe`, gets nothing, and dies with *"set_target_properties called with
incorrect number of arguments"*. `git clone --local` hardlinks the object store,
so a second full checkout costs almost no disk and works inside the container.

## Requirements (real runs)

- A local `google/oss-fuzz` checkout — set `OSS_FUZZ_DIR` (default `~/oss-fuzz`).
- Docker (used by `infra/helper.py`).
- An LLM backend, same as the Java pipeline: `OPENAI_API_KEY`, or Azure, or a
  local server (see `src/config.py`).

Settings live in `src/config.py` (all overridable by env var):

| Var | Default | Meaning |
|-----|---------|---------|
| `OSS_FUZZ_DIR` | `~/oss-fuzz` | the checkout a project name resolves against |
| `OSS_FUZZ_WORK_DIR` | `~/.cache/vuln-patch/oss-fuzz` | clones + `vuln`/`head` worktrees |
| `OSS_FUZZ_SANITIZER` | `address` | fallback when neither the CVE nor `--sanitizer` says |
| `OSS_FUZZ_VERIFY_TIMEOUT` | `120` | per-harness trigger gate on the vuln build |
| `OSS_FUZZ_FUZZ_TIMEOUT` | `600` | per-harness sibling hunt on HEAD |
| `OSV_API_URL` | `https://api.osv.dev/v1` | bug source of truth |

## Usage

```bash
export OSS_FUZZ_DIR=~/oss-fuzz OPENAI_API_KEY=sk-...
uv run -m oss_fuzz.run --project libxml2 -n 5 --fuzz-timeout 300
```

The run prints which placement strategy it chose and why, e.g.

```
harness build: overwrite fuzz/xml.c in place -> target 'xml' (build.sh has no
compile line to crib, so the project's own build system must compile the harness)
```

Force one with `--harness-build {auto,crib,overwrite}`, and point `overwrite` at
a specific file with `--base-harness fuzz/xml.c` (relative to the target's repo
root) when auto-detection picks the wrong harness.

### Which projects can this actually run on?

Only a minority of the ~1365 projects in an OSS-Fuzz checkout are viable: 588
are C/C++ (the rest are python/go/jvm/rust/js/swift/ruby, which this front-end
cannot write a harness for), and of those only some have a disclosed bug with a
fix commit. Discover them instead of guessing:

```bash
uv run -m oss_fuzz.run --list-candidates --max-projects 60
```

```
project                  lang  advisory           published   crash type
apache-logging-log4cxx   c++   OSV-2026-1234      2026-06-05  Heap-buffer-overflow READ 1
assimp                   c++   OSV-2026-999       2026-06-04  Container-overflow READ 4
...
```

`--auto-project` picks the newest and runs it. Each candidate costs one OSV
query, so `--candidate-limit` (default 10) stops the sweep early and
`--max-projects` caps how many are probed.

Every run preflights the target first — checkout is a real `google/oss-fuzz`
clone; the project exists; `project.yaml` says C/C++, builds with libFuzzer, and
supports the sanitizer; a `main_repo` is resolvable. A `--project urllib3`
(python) or a typo now fails in milliseconds with the reason, instead of after a
clone, a Docker image build, and an LLM budget.

Pin a specific CVE, or supply the original PoC for a pre-flight sanity check:

```bash
uv run -m oss_fuzz.run --project libxml2 --cve CVE-2022-XXXXX \
    --reproducer ./testcase --sanitizer address
```

## Target selection: OSS-Fuzz bugs mostly have no CVE

OSS-Fuzz's OSV records are `OSV-YYYY-NNNN` entries and **carry no CVE alias** —
measured against the live API, ten major C/C++ projects (libxml2, harfbuzz,
curl, openssl, wireshark, …) return 261 records with zero CVE aliases between
them. A CVE, when one exists, is usually minted on the *upstream ecosystem*
entry rather than the OSS-Fuzz one.

So selection requires a **fix boundary** (a `fixed` commit + a repo), not a CVE.
`--require-cve` restores the stricter policy, but on the OSS-Fuzz ecosystem it
generally selects nothing. `--cve <id>` implies it.

Those records do carry something better for steering: a **crash type** and the
original **crash stack**. Both are parsed out of the record's prose `details`
(`database_specific` is empty in practice) and spliced into the prompt — the
diff says what changed, the crash stack says where it blew up. The crash type
also picks the sanitizer when the record doesn't name one, so a UBSan-only bug
isn't run under ASan, where its harness would compile and never trigger.

### Offline wiring check (no Docker / network / LLM)

```bash
uv run -m oss_fuzz.run --project demo \
    --osv-json oss_fuzz/tests/fixture_osv.json --dry-run -n 1 -m 2
```

`--dry-run` prints every external command and uses a stub harness, so you can
verify the control flow before spending a real fuzzing budget. `--osv-json`
loads OSV records from a file instead of the network (also good for
reproducible runs). Under `--dry-run` the targeting preflight downgrades to a
warning, so the fixture project (`demo`, which is not in any checkout) still
exercises the whole flow.

## Tests

```bash
python src/oss_fuzz/tests/test_offline.py     # or: pytest src/oss_fuzz/tests
```

40 offline tests, no Docker/network/LLM. Covers bug selection (default and
`--require-cve`), crash-metadata parsing and sanitizer inference,
`project.yaml` scalar/block-list parsing, checkout validation, the
language/engine/sanitizer support gate and its OSS-Fuzz default fallbacks,
candidate discovery (both filters + the sweep limit), diff→function extraction,
the `build.sh` crib, harness placement (base-harness discovery and ranking,
`auto`/forced mode selection, overwrite restoring the tree byte-for-byte and
never touching `build.sh`, the target-name abort, the campaign gating on the
replaced target's name, and the prompt language following the forced
extension), crash detection/signatures, source extraction, prompt assembly, and
Java/shared steering parity.

## Notes & limits

- **Most OSS-Fuzz projects are not targets.** 588 of 1365 are C/C++; the rest
  need a different harness language entirely. The preflight rejects them (and
  unsupported sanitizers/engines, and missing `main_repo`) before spending
  anything. `--list-candidates` narrows further to projects with a fix boundary.
- **Overwrite needs a harness to overwrite.** It is located inside the target's
  *upstream* checkout. The 156 projects that ship their harness inside
  `oss-fuzz/projects/<name>/` instead are mostly cribbable anyway (130 of 156),
  but the 26 that are neither reach nothing automatically — pass
  `--base-harness`. Projects whose harness lives in a *third* repo cloned by
  `build.sh` (bearssl builds a module against `cryptofuzz`) are outside both
  strategies; `--harness-build overwrite` fails fast with the reason rather than
  guessing.
- **79 of 1329 projects can never be driven**, whatever their language: their
  Dockerfile sets `WORKDIR` to the shared `/src` root, and `helper.py
  build_fuzzers <project> <local_path>` refuses that outright ("Cannot use local
  checkout with WORKDIR: /src") — no compiler runs. Since this pipeline builds
  the vulnerable commit and HEAD from local worktrees, the preflight rejects
  them from one Dockerfile read. capstone is one; it was found the expensive way.
- **The fix commit is often not a fix.** OSS-Fuzz `fixed` commits come from
  automated bisection. Of 9 audited candidates, 2 pointed at commits touching no
  source at all (c-blosc2's is *"Add diagrams for the new shared thread pool
  architecture"* — two image files, and its crash symbol isn't even in the repo,
  being vendored zstd). Those yield an empty root-cause context and hence an
  unsteered prompt, so the driver walks to the next-newest record rather than
  running a test of the heuristic that cannot test it. `--max-target-tries`
  bounds the walk; `--allow-empty-context` overrides.
- **Only library code is mined for root cause.** A broad upstream sync also
  touches tests, the project's own fuzz harnesses, CLI tools and bindings.
  capstone's record dragged in four different `main`s and the project's own
  `LLVMFuzzerTestOneInput` — "steer toward `LLVMFuzzerTestOneInput`" is
  incoherent, it is the thing being written. Those directories are skipped and
  listed in `skipped_non_library`; `main`, unnameable (`?`) functions and libc
  calls are dropped from the steering set.
- **Infrastructure failure is not a negative result.** `helper.py` exits nonzero
  for a broken environment as well as a bad harness. Feeding the former back as
  "your harness did not compile" wastes the whole attempt budget on a file that
  was never compiled, then reports `0 siblings`. Such runs now abort after one
  attempt and exit **2** with `RUN ABORTED`, distinct from a genuine clean result.
- **amd64 only — this is a hard ceiling, not just a slowdown.** OSS-Fuzz
  publishes `linux/amd64` base images. On an arm64 host (Apple Silicon) they run
  under emulation, and any build that *executes* what it just compiled fails.
  autotools' `configure` does exactly that, so bluez dies with
  `configure: error: cannot run C compiled programs` — and its **stock** build
  (`helper.py build_fuzzers bluez`, no harness of ours involved) fails
  identically, which is how we know it is the platform and not the pipeline.
  CMake projects do build. `run.py` prints a host warning up front, and such
  failures are classified as infrastructure so the campaign aborts instead of
  asking the model to repair them. Real runs want an x86_64 Linux host.
- **Candidate discovery costs one OSV query per surviving project.** The local
  `project.yaml` filter runs first precisely so the network filter runs on a few
  hundred projects rather than all 1365; bound it with `--candidate-limit` /
  `--max-projects`. Ranking is newest-first *among what the sweep found*, not a
  global ranking over the whole checkout.
- **Reproducer availability.** OSS-Fuzz testcases are embargoed until
  disclosure and OSV doesn't always embed a stable download URL, so the PoC is
  *optional*: the pipeline re-derives triggering harnesses from the fix diff
  and gates on its own crash check. Pass `--reproducer <path>` if you have the
  testcase and want the pre-flight sanity reproduce.
- **Reachable set.** The call graph comes from fuzz-introspector's light
  (tree-sitter, no-build) frontend — a bounded BFS over `base_callsites`
  scoped to project functions, unioned with project-resolved source callees,
  exactly as in `src/java`. It needs the introspector extra
  (`uv sync --extra introspector`); without it, or on timeout, the analyzer
  falls back to the brace-match heuristic and reports which was used
  (`reachable_source` in the printed context). Function *extraction* itself is
  still brace-matching, not a full parse; the trigger gate — not the analysis
  — decides harness validity.
- **HEAD build drift.** If the library's public API changed between the vuln
  commit and HEAD, a harness may not compile against HEAD; those are skipped
  and reported, not counted as clean.

## Responsible disclosure

A crash on HEAD is a live, unfixed issue. Report it to the project and to
OSS-Fuzz through coordinated disclosure; don't publish the sibling input
before a fix ships.