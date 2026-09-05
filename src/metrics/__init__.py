"""Metrics over the root-cause region. Measurement only.

Two things live here:

* `metrics.core` — the language-agnostic CORE of the measurement layer:
  the code-identity model, the five metrics (RCR, RCC, RCP, PSC, CSM),
  the averaging and the paper tables. It computes numbers from JSON a
  language backend already wrote. Backends import it; it imports no
  backend. The Java backend is `src/java/measurements/`.
* the modules beside this file (`rcc`, `reached`, `region`, `keys`,
  `collect`, `sweep`, `rcc_sweep`, `cli`) — the older, separate,
  method-level RCC sweep, which is independent of `core`.

See README.md in this directory for both, and
`src/java/measurements/README.md` for what each metric means and how to
produce the inputs it needs.
"""
