# src/java/studies — offline analysis & measurement of the Java detection pipeline

Standalone scripts that MEASURE the pipeline (they read archived run outputs);
none are imported by the pipeline itself. Run from anywhere — paths resolve to
the repo root automatically.

- **`j1_judge_audit.py`** — J1: audits the two LLM judges (soundness +
  attribution) across every archived run in `runs-archive/runs/`, cross-tabulating
  each verdict vs ground truth. No VM, no LLM calls. `python src/java/studies/j1_judge_audit.py`.
  Latest snapshot: `j1_output.txt`.
- **`rulegen_join.py`** — joins a `--rulegen_only` suite's per-leg records into
  per-bug rule-generation quality (does a relation convict the overfit via replay
  while staying quiet on the correct sibling).
- **`rulegen_join_multi.py`** — aggregates several `--rulegen_only` sample runs of
  ONE variant into a per-bug convict-rate (k of N), to beat single-sample noise.
- **`RULEGEN_LOOP.md`** — how to run the cheap rule-generation iteration loop
  (synth → screen → replay, skipping harness-gen + judge).
- **`study_bugs.json`** — context study: per-bug hand-derived GOLD relations, to
  measure whether the synthesizer proposes them with vs without class context.
- **`study_tasks.py`** — the shared 25-method task set those studies run over.
