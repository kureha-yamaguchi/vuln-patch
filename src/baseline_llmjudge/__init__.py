"""One-shot LLM baseline for patch-completeness classification.

The comparison target is the harness pipeline in `src/java/`. This package
answers the same question about the same patches with the same model and the
same evidence, but without a fuzzing harness and without any execution of the
patched code. See README.md for the protocol and the budget rules.
"""
