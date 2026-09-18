"""DEPRECATED path — this module now lives at `metrics.core.aggregate`.

Kept as a re-export shim so existing imports (`from java.measurements
import aggregate as A`) keep working unchanged. New code should import
`metrics.core.aggregate`.
"""
import sys as _sys

from metrics.core import aggregate as _moved
from metrics.core.aggregate import *          # noqa: F401,F403
from metrics.core.aggregate import (           # noqa: F401
    BUG_KINDS, DEFAULT_BUILD, DEFAULT_F_KIND, GRANULARITIES, TABLE3_COLUMNS,
    aggregate, aggregate_legs, bug_key, delta, leg_key, leg_values,
    render_markdown, render_rcc_vs_caught, split_key, table3_columns,
)

_sys.modules[__name__] = _moved
