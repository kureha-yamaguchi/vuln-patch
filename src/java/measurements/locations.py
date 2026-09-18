"""DEPRECATED path — this module now lives at `metrics.core.locations`.

Kept as a re-export shim so existing imports (`from java.measurements
import locations`, `from java.measurements.locations import MethodRef`)
keep working unchanged. New code should import `metrics.core.locations`.
"""
import sys as _sys

from metrics.core import locations as _moved
from metrics.core.locations import *          # noqa: F401,F403
from metrics.core.locations import (          # noqa: F401
    CALLEE, CALLER, OUTSIDE, RINGS, SEED,
    LineRef, LineSet, MethodIndex, MethodRef, MethodSet, Tagged,
    class_top_from_source, dump, from_introspector, from_jacoco,
    from_javalang, from_stack_frame, load_line_set, load_method_set,
    simple_type, top_level_of,
)

# Bind the SAME module object to the old name, so module-level state,
# `is`-comparisons and monkeypatching behave identically whichever import
# path a caller used.
_sys.modules[__name__] = _moved
