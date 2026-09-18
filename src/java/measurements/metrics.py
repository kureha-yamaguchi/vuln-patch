"""DEPRECATED path — this module now lives at `metrics.core.definitions`.

Kept as a re-export shim so existing imports (`from java.measurements
import metrics as M`, `from java.measurements.metrics import is_jdk`) keep
working unchanged. New code should import `metrics.core.definitions`; the
names inside it (`metric_key`, `R_VARIANTS`, `compute_leg`, ...) are
unchanged.
"""
import sys as _sys

from metrics.core import definitions as _moved
from metrics.core.definitions import *         # noqa: F401,F403
from metrics.core.definitions import (         # noqa: F401
    BUILDS, DEFAULT_F_KIND, F_KINDS, GRANULARITIES, METRICS_FILE,
    MEASUREMENTS_DIR, RESULT_FILE, RING_ORDER, RING_ORDER_OUT, R_VARIANTS,
    R_VARIANTS_LINE, STATIC_SETS, is_jdk, metric_key,
)

_sys.modules[__name__] = _moved
