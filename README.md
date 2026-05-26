## Dependencies

`brew install llvm`

install `uv`

Run with local gpt-oss-20b model

# 1. Install
`brew install ollama`

# 2. Start the server (leave running in a terminal)
`ollama serve`

# 3. In another terminal, pull the model
`ollama pull gpt-oss:20b`

# If needed
`pip install --upgrade openai`

# 4. Run your script 
Example usage (choose project_name from Chart/Closure/Lang/Math/Time):
`cd src && uv run -m run -c --project_name Closure`
`cd src && uv run -m run -c --project_name Closure -n 5 -m 50`


![Minimal LLM-based fuzz harness generator](https://adalogics.com/blog/minimal-llm-based-fuzz-harness-generator)

![Fuzz Introspector: enabling rapid fuzz introspection tool development](https://adalogics.com/blog/fuzz-introspection-as-python-library)

## Dependencies

`git clone git@github.com:ASSERT-KTH/drr.git`
`git clone git@github.com:rjust/defects4j.git`
`cd defects4j`
`git reset --hard 486e2b49d806cdd3288a64ee3c10b3a25632e991`

## Overview of scripts

config.py — env-driven constants (LLM endpoint, Jazzer version, dataset paths, APR tool list)
jazzer.py — JazzerEnvironment: locates/downloads jazzer-api.jar
patches.py — PatchSelector + PatchSelection dataclass: picks a random patch and checks out the buggy d4j version
analysis.py — TargetAnalyzer + PatchContext/TouchedFunction dataclasses: parses the patch, runs fuzz-introspector, resolves touched functions
prompts.py — PromptBuilder: assembles the chat-completion messages
llm.py — HarnessGenerator: thin wrapper around the local OpenAI-compatible server
build.py — HarnessBuilder + BuildResult dataclass: extracts Java source, runs javac
run.py — entry point: parse_args() + main() that wires the stages together