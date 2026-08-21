"""One-shot LLM baseline for patch-completeness classification.

The comparison target is the harness pipeline. This package answers the same
question about the same patches with the same model and the same evidence, but
without a fuzzing harness and without any execution of the patched code.

THREE SUBPACKAGES, AND THE SPLIT IS THE DATASET.

  * `shared/` — what both datasets use. The output space, the confusion
    matrix, the spend report, the block type and the prompt-version shape.
  * `defects4j/` — the Defects4J baseline, over the drr patches. This is the
    main experiment. Two pools: crashing and semantic.
  * `project_zero/` — the Project Zero baseline, over the variant-pair
    dataset. Real upstream security fixes in C and C++.

A dataset subpackage imports from `shared/`. Neither dataset subpackage
imports from the other, so a change to one cannot move a number in the other.

See README.md for the protocol, the budget rules and the two populations.
"""
