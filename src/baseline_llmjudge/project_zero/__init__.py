"""The Project Zero baseline, over the variant-pair dataset.

The dataset lives in `src/db/project_zero/pairs/`. A variant pair is two
upstream security fixes for one root cause: the prior fix shipped and left a
sibling bug behind, and the later fix removed what the prior one missed. So
the prior fix is an overfitting patch and the later fix is the correct patch.

Read the modules in this order. Each one depends only on the ones above it:

  1. `firewall.py` — the one reader of `metadata.json` and the raw diffs. The
     pair convention IS the label, so this module returns a selector view and
     a clean view, and only the clean view may be rendered.
  2. `bugkind.py`  — crashing or semantic, per fix, by rule then by model.
  3. `queue.py`    — the scored population: one row per distinct fix commit.
  4. `evidence.py` — the four evidence blocks, and the parity manifest.
  5. `prompts.py`  — the three stage-A designs and the stage-B registry.
  6. `run_one.py`  — one fix, N samples, one record.
  7. `evaluate.py` — the whole population: records, summary, baselines.

This subpackage imports from `baseline_llmjudge.shared` and never from
`baseline_llmjudge.defects4j`. See README.md section 11.
"""
