"""DEPRECATED path — this module now lives at `metrics.core.paper_tables`.

Kept as a re-export shim so existing imports (`from java.measurements
import paper_tables as PT`) and `python -m java.measurements.paper_tables`
keep working unchanged. New code should import `metrics.core.paper_tables`.
"""
import sys as _sys

from metrics.core import paper_tables as _moved
from metrics.core.paper_tables import *       # noqa: F401,F403
from metrics.core.paper_tables import (        # noqa: F401
    BUG_CLASSES, DEFAULT_BUILD, DEFAULT_F_KIND, DEFAULT_RVAR, GRANULARITIES,
    R_VARIANTS, Arm, build_parser, load_arm, main, metric_field, pair,
    render_both, table3, table3_rows, table4, table4_rows,
)

if __name__ == '__main__':                     # pragma: no cover
    _sys.exit(_moved.main())
else:
    _sys.modules[__name__] = _moved
