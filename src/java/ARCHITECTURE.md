# src/java — package layout

The pipeline runs as `cd src && python java/run.py …`. `run.py` inserts `src/`
onto `sys.path`, so every module is imported absolutely as `java.<subpkg>.<mod>`
and `config` / `llm` (which live in `src/`) resolve too.

## Subpackages (by pipeline station)

| Package | Modules | Role |
|---|---|---|
| `java/` (root) | `run.py`, `verifier_replay.py` | Orchestrator entry point; replay driver. |
| `parsing/` | `java_source` | Java source/AST parsing (javalang). |
| `bug_context/` | `analysis`, `failure_test`, `crash_input`, `call_graph`, `code_context`, `patches` | Extract the bug's context: trigger test, crash input, call graph, source context, patch selection. |
| `relations/` | `relation_synth`, `relation_screen`, `relation_verifier` | Semantic-bug metamorphic/contract relations: synthesise → screen → the soundness+attribution judges. |
| `harness/` | `campaign`, `prompts`, `build`, `repair` | Build & compile Jazzer harnesses; prompt construction; harness repair. |
| `execution/` | `fuzz_runner`, `jazzer`, `oracle_strength` | Run harnesses under Jazzer; classify fired oracles. |
| `dataset/` | `certify_detectability`, `classify_bugs`, `eval_candidates` | Offline dataset tooling: label-verification (detectability certifier), crashing/semantic classification, candidate selection. |
| `studies/` | `j1_judge_audit`, `rulegen_join*` | One-off offline analyses (not in the live path). |

`config.py` and `llm.py` stay in `src/` (shared, imported flat as `config` / `llm`).

## Import rule
Intra-package imports are absolute: `from java.execution.fuzz_runner import …`.
Direct-entry scripts (`run`, `verifier_replay`, `dataset/*`) anchor `sys.path`
to the dir containing `config.py`, so they work regardless of depth.
