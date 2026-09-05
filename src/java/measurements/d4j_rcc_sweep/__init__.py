"""Kureha's original RCC(H_R) sweep — an earlier, RCC-only implementation
over the crashing Defects4J split. The general definitions of every metric
live in metrics.core.definitions; the pieces this sweep had that the general
layer lacked (JaCoCo exit-probe frame repair, trigger-test gate,
fixed-input-budget re-measurement) were ported into
java.measurements.coverage / root_cause. Kept so the sweep and its results
stay reproducible.
"""
