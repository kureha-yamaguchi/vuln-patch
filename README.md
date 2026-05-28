# Addressing the Root Cause: Automated Fuzzing Harness Generator for Variant Analysis

Pipeline for automatically generating Jazzer fuzzing harnesses for [Defects4J](https://github.com/rjust/defects4j) bugs, given an APR tool generated patch from the [drr](https://github.com/ASSERT-KTH/drr) dataset. The goal is to detect semantically incorrect (overfitting) patches by verifying that a set of harnesses exposing the root cause of the vulnerability still triggers on the patched version.

## Overview

For a selected patch, the pipeline:

1. Checks out the buggy Defects4J project version
2. Parses the patch and runs fuzz-introspector to identify touched functions and their call-graph context
3. Builds a chat-completion prompt and sends it to an LLM
4. Extracts and compiles the generated Jazzer harness
5. Repeats until a target number of harnesses compile successfully
6. Copies the buggy checkout, applies the DRR patch, and runs each compiled harness against the patched code with Jazzer — harnesses that still crash indicate the patch is overfitting

## Dependencies

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

## Configuration

All settings are env-driven via [src/config.py](src/config.py):

| Variable | Default | Description |
|---|---|---|
| `LOCAL_LLM_BASE_URL` | `http://localhost:11434/v1` | LLM server URL |
| `LOCAL_LLM_MODEL` | `gpt-oss:20b` | Model name |
| `LOCAL_LLM_API_KEY` | `not-needed` | API key (dummy for local servers) |
| `JAZZER_VERSION` | `0.22.1` | Jazzer release to fetch from Maven Central |
| `JAZZER_API_JAR` | `~/.cache/jazzer/jazzer-api-<version>.jar` | Path to cached jar |

Example — use LM Studio instead of Ollama:

```bash
export LOCAL_LLM_BASE_URL=http://localhost:1234/v1
export LOCAL_LLM_MODEL=openai/gpt-oss-20b
```

## Usage

Run from the `src/` directory with `uv`:

```bash
# Correct patch, choose from Java project (Chart/Closure/Lang/Math/Time), default settings (5 successes / 50 attempts)
cd src && uv run -m run --correct --project_name Closure -n 5 -m 50

# Overfitting patch, choose from Java project (Chart/Closure/Lang/Math/Time), default settings (5 successes / 50 attempts)
cd src && uv run -m run --overfitting --project_name Lang -n 5 -m 50
```

**Flags:**

| Flag | Description |
|---|---|
| `-c` / `--correct` | Select from semantically correct patches |
| `-o` / `--overfitting` | Select from overfitting patches |
| `--project_name` | Defects4J project: `Chart`, `Closure`, `Lang`, `Math`, or `Time` |
| `-n` / `--target_successes` | Stop after this many compiling harnesses (default: 5) |
| `-m` / `--max_attempts` | Hard cap on LLM calls (default: 50) |
| `--max_repair_failures` | Consecutive compile failures before resetting context (default: 2) |

## Architecture

| Module | Class | Responsibility |
|---|---|---|
| [config.py](src/config.py) | — | Env-driven constants |
| [patches.py](src/patches.py) | `PatchSelector` | Random patch selection + Defects4J checkout |
| [analysis.py](src/analysis.py) | `TargetAnalyzer` | Patch parsing + fuzz-introspector call-graph analysis |
| [prompts.py](src/prompts.py) | `PromptBuilder` | Chat-completion message assembly |
| [llm.py](src/llm.py) | `HarnessGenerator` | OpenAI-compatible LLM wrapper |
| [build.py](src/build.py) | `HarnessBuilder` | Java source extraction + `javac` compilation |
| [campaign.py](src/campaign.py) | `HarnessCampaign` | Generate → build loop until convergence |
| [jazzer.py](src/jazzer.py) | `JazzerEnvironment` | Jazzer API jar resolution / download |
| [run.py](src/run.py) | — | CLI entry point |

## Related work

- [Minimal LLM-based fuzz harness generator](https://adalogics.com/blog/minimal-llm-based-fuzz-harness-generator)
- [Fuzz Introspector: enabling rapid fuzz introspection tool development](https://adalogics.com/blog/fuzz-introspection-as-python-library)
