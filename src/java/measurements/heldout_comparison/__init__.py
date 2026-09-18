"""Paired fixed-candidate-budget experiment over frozen Defects4J holdouts."""

# HN retains its historical level-B identity; level C is a separate arm.
ARMS = {'HN': 'neighbourhood', 'HN_C': 'function', 'HR': None}
