"""Metrics over the root-cause region. Measurement only.

This package holds one thing: `metrics.core`, the language-agnostic CORE of
the measurement layer — the code-identity model, the five metrics (RCR,
RCC, RCP, PSC, CSM) in `metrics.core.definitions`, the averaging and the
paper tables. It computes numbers from JSON a language backend already
wrote. Backends import it; it imports no backend. The Java backend is
`src/java/measurements/`.

See README.md in this directory, and `src/java/measurements/README.md` for
what each metric means and how to produce the inputs it needs.
"""
