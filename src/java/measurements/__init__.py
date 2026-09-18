"""Root-cause measurement layer — MEASUREMENT ONLY.

Computes the paper's formal objects (patch-derived set P, approximated
root-cause region R-hat, fuzzer-reachable set F(H)) and the five metrics
over them (RCR, RCC, RCP, PSC, CSM) for an archived run directory.

Nothing in this package is imported by the pipeline (`java.run`), and
`root_cause.py` — the only module that reads the developer fix — is
imported by nothing except this package's own CLI. See README.md.
"""
