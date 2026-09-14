"""Kureha's Defects4J five-metric sweep over the crashing split.

An earlier, self-contained implementation of the five root-cause region
metrics — RCC, RCR, RCP, PSC and CSM — at function level, built directly on
Defects4J, JaCoCo and Jazzer. The sets come from `region` (R-hat, from the
developer fix), `patchset` (P, from the APR patch and its static
neighbourhood), `reached` (F(H), from JaCoCo plus stack-frame repair) and
`crashes` (C, from the Jazzer output); `scores` divides them and holds the
triggering-test gate, `keys` gives both sides one spelling per method, and
`collect` builds the projects and gathers the coverage. The entry points are
`sweep_gate` (R-hat and the gate, no pipeline), `sweep_full` (the end-to-end
H_R experiment), `rescore` (re-score a finished run) and `score_one` (one
bug from reports already on disk).

The general, language-agnostic definitions of every metric live in
metrics.core.definitions; the pieces this sweep had that the general layer
lacked (JaCoCo exit-probe frame repair, trigger-test gate,
fixed-input-budget re-measurement) were ported into
java.measurements.coverage / root_cause. Kept so the sweep and its results
stay reproducible.
"""
