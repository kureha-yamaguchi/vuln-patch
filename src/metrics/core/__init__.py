"""Language-agnostic core of the measurement layer.

One definition of every metric, shared by every language backend:

  `locations`     the code-identity model (method/line refs, ringed sets,
                  cross-tool matching) every measurement is built from
  `ratios`        the five metrics (RCR, RCC, RCP, PSC, CSM) over the four
                  sets (P, R, F, C), computed from JSON a backend wrote
  `aggregate`     pooling metrics across the legs of a run
  `paper_tables`  rendering the pooled numbers as paper tables

Nothing here imports a language backend. Backends import core, never the
other way round; `tests/test_metrics_core_layering.py` enforces it. The
Java backend (extraction: patch diffs, call graphs, JaCoCo, Jazzer) is
`src/java/measurements/`, which also carries the full metric definitions
in its README.
"""
