# Addressing the Root Cause: Automated Fuzzing Harness Generator for Variant Analysis

Pipelines for automatically generating fuzzing harnesses that expose the **root
cause** of a known bug, and then re-running them against the fixed code. A
harness that still triggers after the fix means the fix was incomplete — an
overfitting patch, or a sibling bug.

## Scope: what it currently works on

There are **two working front-ends, and they are language-specific** — Java and
C/C++. There is no polyglot pipeline.

| Front-end | Targets | Engine | Status |
|---|---|---|---|
| [`src/java/`](src/java) | [Defects4J](https://github.com/rjust/defects4j) bugs + APR patches from [drr](https://github.com/ASSERT-KTH/drr) | Jazzer | Mature |
| [`src/oss_fuzz/`](src/oss_fuzz) | OSS-Fuzz projects with a disclosed OSV bug | libFuzzer | Working |

Both share the LLM half — [`src/llm.py`](src/llm.py),
[`src/variant.py`](src/variant.py) (the steering rule), and
[`src/config.py`](src/config.py) — so the research method cannot quietly drift
apart between them. What is language-specific is the prompt wording, the diff
analysis, and everything that builds and runs a harness.

Two further directories are **not usable pipelines**:

- [`src/linux/`](src/linux) — kernel C trigger programs. Partial and out of
  date; the C/C++ work happens in `src/oss_fuzz/` instead.
- [`src/project_zero/`](src/project_zero) — a design note only. No pipeline code.

**On the C/C++ side, the language filter is not the only gate.** `NATIVE_LANGUAGES`
in [ossfuzz.py](src/oss_fuzz/ossfuzz.py#L101) is `("c", "c++")`, and anything else
(python/go/jvm/rust/javascript/swift/ruby) is rejected before a clone, a Docker
build or an LLM call costs anything. That leaves 588 of the ~1365 projects in an
oss-fuzz checkout — but a project also has to use libFuzzer, have a disclosed bug
with a fix commit that touches source, be buildable from a local checkout, and be
compilable by one of the two harness-build strategies. The set that actually runs
is much smaller than 588; see [src/oss_fuzz/README.md](src/oss_fuzz/README.md).

## Overview (Java front-end)

For a selected patch, the pipeline:

1. Checks out the buggy Defects4J project version
2. Parses the patch and runs fuzz-introspector to identify touched functions, their call-graph cross-references, and the **statically reachable region downstream of the root cause** (where sibling bugs are most likely to live)
3. Builds a chat-completion prompt — including a *variant-analysis* section telling the model which of the reachable functions and which crash signatures the harness set already covers — and sends it to an LLM
4. Extracts and compiles the generated Jazzer harness
5. **Verifies the harness against the buggy checkout with a short Jazzer run, and only accepts it if it crashes.** A harness that compiles but does not trigger the bug is rejected and fed back as a repair turn — the convergence criterion is "compiles AND triggers", not "compiles"
6. Repeats until a target number of harnesses are accepted, steering each new harness toward the still-uncovered part of the root-cause region so the set interrogates the fault from many angles
7. Copies the buggy checkout, applies the DRR patch, and runs each accepted harness against the patched code with Jazzer — harnesses that still crash indicate the patch is overfitting

## Dependencies

Everything below is what the **Java** front-end needs. The C/C++ front-end needs
a different set — a `google/oss-fuzz` clone, Docker, and an x86_64 Linux host —
listed in [src/oss_fuzz/README.md](src/oss_fuzz/README.md#setup). Both share the
LLM configuration.

### Repositories

```bash
git clone git@github.com:kureha-yamaguchi/vuln-patch.git && cd vuln-patch # this repository
git clone git@github.com:ASSERT-KTH/drr.git
git clone git@github.com:rjust/defects4j.git
cd defects4j && git reset --hard 486e2b49d806cdd3288a64ee3c10b3a25632e991
```

### System tools

```bash
brew install llvm
brew install ollama
```

Install [uv](https://github.com/astral-sh/uv) for Python dependency management.

### Local LLM

```bash
# Start the server (leave running in a terminal)
ollama serve

# In another terminal, pull the model
ollama pull gpt-oss:20b
```

The pipeline targets any OpenAI-compatible local server (Ollama or LM Studio). See [Configuration](#configuration) to point it elsewhere.

### Python packages

```bash
uv sync
```

**Optional — fuzz-introspector (root-cause reachable region).** The reachable-set
steering uses `fuzz-introspector`, an optional extra. Its `lxml 4.9.1` pin only
builds on **Python ≤ 3.11**, so create the venv on 3.11 and install the extra:

```bash
# system deps for lxml/atheris (Debian/Ubuntu)
sudo apt-get install -y clang libxml2-dev libxslt1-dev zlib1g-dev python3-dev
uv venv --clear --python 3.11
uv sync --extra introspector
```

Without it the pipeline still runs — it degrades gracefully, skipping only the
variant-analysis steering block (see [analysis.py](src/java/analysis.py)).

## Configuration

All settings are env-driven via [src/config.py](src/config.py):

| Variable | Default | Description |
|---|---|---|
| `LOCAL_LLM_BASE_URL` | `http://localhost:11434/v1` | LLM server URL |
| `LOCAL_LLM_MODEL` | `gpt-oss:20b` | Model name |
| `LOCAL_LLM_API_KEY` | `not-needed` | API key (dummy for local servers) |
| `JAZZER_VERSION` | `0.22.1` | Jazzer release to fetch from Maven Central |
| `JAZZER_API_JAR` | `~/.cache/jazzer/jazzer-api-<version>.jar` | Path to cached jar |
| `VERIFY_TIMEOUT_SECONDS` | `20` | Per-harness Jazzer budget for the buggy-version trigger gate |
| `FUZZ_TIMEOUT_SECONDS` | `30` | Per-harness Jazzer budget for the patched-version overfitting check |
| `MAX_REACHABLE_IN_PROMPT` | `60` | Cap on root-cause-reachable function names spliced into the prompt |
| `REACHABLE_NODE_CAP` | `200` | Budget for the reachable-set BFS: max functions visited (also `--reachable_node_cap`) |
| `REACHABLE_MAX_DEPTH` | `3` | Max call-graph depth for the reachable-set BFS; direct callees are depth 1 (also `--reachable_max_depth`) |

Example — use LM Studio instead of Ollama:

```bash
export LOCAL_LLM_BASE_URL=http://localhost:1234/v1
export LOCAL_LLM_MODEL=openai/gpt-oss-20b
```

## Usage

### Java (Defects4J + Jazzer)

Run from the `src/` directory with `uv`:

```bash
# Correct patch, choose from Java project (Chart/Closure/Lang/Math/Time), default settings (5 successes / 50 attempts)
cd src && uv run java/run.py --correct --project_name Closure -n 5 -m 50

# Overfitting patch, choose from Java project (Chart/Closure/Lang/Math/Time), default settings (5 successes / 50 attempts)
cd src && uv run java/run.py --overfitting --project_name Lang -n 5 -m 50
```

**Flags:**

| Flag | Description |
|---|---|
| `-c` / `--correct` | Select from semantically correct patches |
| `-o` / `--overfitting` | Select from overfitting patches |
| `--project_name` | Defects4J project: `Chart`, `Closure`, `Lang`, `Math`, or `Time` |
| `-n` / `--target_successes` | Stop after this many **accepted** harnesses (compile + trigger) (default: 5) |
| `-m` / `--max_attempts` | Hard cap on LLM calls (default: 50) |
| `--max_repair_failures` | Consecutive failures before resetting context (default: 3) |
| `--reachable_node_cap` | Budget for the root-cause reachable-set BFS: max functions visited (default: `REACHABLE_NODE_CAP`). Higher = wider neighbourhood, slower analysis |
| `--reachable_max_depth` | Max call-graph depth for the reachable-set BFS (default: `REACHABLE_MAX_DEPTH`); direct callees are depth 1 |
| `--fuzz_timeout` | Seconds Jazzer runs per harness against the *patched* code (default: 60; 0 to skip) |
| `--verify_timeout` | Seconds Jazzer runs per harness against the *buggy* code to verify it triggers before acceptance (default: `VERIFY_TIMEOUT_SECONDS`, 20) |
| `--no-require-trigger` | Accept harnesses on compile alone (old behaviour); skips the buggy-version trigger gate. For ablation experiments |

### C/C++ (OSS-Fuzz + libFuzzer)

Also run from `src/` (the gitignored `oss-fuzz/` clone lives at the repo root):

```bash
export OSS_FUZZ_DIR=$PWD/oss-fuzz OPENAI_API_KEY=sk-...
cd src && uv run -m oss_fuzz.run --project libxml2 -n 5 --fuzz-timeout 300

# Which projects are actually usable (C/C++ *and* a disclosed bug with a fix commit)
cd src && uv run -m oss_fuzz.run --list-candidates --max-projects 60
```

Full flag list, the `crib` vs `overwrite` build strategies, and the limits are in
[src/oss_fuzz/README.md](src/oss_fuzz/README.md).

## Architecture

Shared by both front-ends, flat in `src/`:

| Module | Responsibility |
|---|---|
| [config.py](src/config.py) | Env-driven settings for every pipeline |
| [llm.py](src/llm.py) | OpenAI-compatible LLM wrapper — send messages, get text back |
| [variant.py](src/variant.py) | The steering rule: aim the next harness at the part of the root-cause region earlier harnesses missed |

Java front-end, under [`src/java/`](src/java) (full breakdown in
[java/ARCHITECTURE.md](src/java/ARCHITECTURE.md)):

| Package | Responsibility |
|---|---|
| [java/run.py](src/java/run.py) | CLI entry point / orchestrator |
| [parsing/](src/java/parsing) | Java source and AST parsing (javalang) |
| [bug_context/](src/java/bug_context) | Patch selection, trigger test, crash input, call graph, source context |
| [relations/](src/java/relations) | Metamorphic/contract relations for semantic bugs: synthesise → screen → judge |
| [harness/](src/java/harness) | Prompt construction, harness compilation, the generate → build → verify campaign |
| [execution/](src/java/execution) | Jazzer environment and runs, fired-oracle classification |
| [dataset/](src/java/dataset) | Offline dataset tooling (detectability certification, bug classification) |

C/C++ front-end, under [`src/oss_fuzz/`](src/oss_fuzz) — `run.py` is the only
entry point; see [its README](src/oss_fuzz/README.md) for the per-module table,
the `crib` vs `overwrite` build strategies, and its own settings.

## Related work

- [Minimal LLM-based fuzz harness generator](https://adalogics.com/blog/minimal-llm-based-fuzz-harness-generator)
- [Fuzz Introspector: enabling rapid fuzz introspection tool development](https://adalogics.com/blog/fuzz-introspection-as-python-library)