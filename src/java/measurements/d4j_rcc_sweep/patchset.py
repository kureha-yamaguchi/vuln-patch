"""P — the patch-derived set, at function level.

R-hat comes from the DEVELOPER fix. P comes from the patch that is under
analysis — the APR patch the pipeline is asked to judge. The pipeline never
sees the developer fix, so P is everything the pipeline itself can know
about where the fault lives.

P has two parts, and `java.bug_context.analysis.TargetAnalyzer` already
builds both:

  1. the TOUCHED functions — every method whose body the APR patch changes;
  2. their NEIGHBOURHOOD — the project functions statically reachable from
     a touched function, per the fuzz-introspector call graph, inside the
     same budget (`REACHABLE_NODE_CAP`, `REACHABLE_MAX_DEPTH`) the pipeline
     uses.

So P is the region the harness set is STEERED across, and R-hat is the
region it SHOULD cover. Comparing them is the point of RCR.

Two differences from `PatchContext.root_cause_reachable`, and both are
deliberate:

  * the touched functions stay IN. The prompt drops them because it already
    prints them in full. A set-theory metric must keep them, because they
    are the part of P most likely to be in R-hat.
  * the members are `MethodKey`s, not display labels. A label loses the
    parameter types, and two overloads then read as one function.

JDK functions are dropped, by the same receiver-package test the pipeline
uses. `Math.exp` is not part of anybody's root-cause region.

MEASUREMENT ONLY.
"""
from dataclasses import dataclass, field
from typing import List, Optional, Set

from java.bug_context.analysis import TargetAnalyzer
from java.bug_context.call_graph import is_project_fn, project_prefix
from java.measurements.d4j_rcc_sweep.keys import (
    CONSTRUCTOR, MethodKey, key_from_mangled, normalise_type,
)


@dataclass
class PatchSet:
    """P for one patch."""
    keys: Set[MethodKey] = field(default_factory=set)
    # The touched functions alone — a subset of `keys`. Kept so a report can
    # say how much of P is the patch itself and how much is neighbourhood.
    touched: Set[MethodKey] = field(default_factory=set)
    # One note per problem met while the neighbourhood was built, e.g.
    # 'introspector_unavailable'. An empty P with no note is a patch whose
    # touched functions call nothing; an empty P WITH a note is a failed
    # lookup. The two must not read alike.
    notes: List[str] = field(default_factory=list)

    @property
    def size(self) -> int:
        return len(self.keys)

    @property
    def is_empty(self) -> bool:
        return not self.keys


def key_from_touched(function) -> Optional[MethodKey]:
    """`MethodKey` for one `analysis.TouchedFunction`.

    None when the AST could not name the declaring class, because a key
    without a class cannot match anything on the other side."""
    class_name = function.func_class_fq or function.func_class
    if not class_name:
        return None
    class_name = class_name.replace('$', '.')
    name = function.func_name
    if name == class_name.split('.')[-1]:
        name = CONSTRUCTOR
    return MethodKey(
        class_name=class_name,
        method_name=name,
        param_types=tuple(normalise_type(p)
                          for p in (function.func_param_types or [])),
    )


def patch_set_from_context(context) -> PatchSet:
    """P from an `analysis.PatchContext` the pipeline already built."""
    result = PatchSet(notes=list(context.neighbourhood_notes or []))
    prefix = project_prefix(context.package)
    for function in context.functions:
        key = key_from_touched(function)
        if key is not None:
            result.touched.add(key)
            result.keys.add(key)
        for mangled in function.reachable or []:
            if prefix and not is_project_fn(mangled, prefix):
                continue
            neighbour = key_from_mangled(mangled)
            if neighbour is not None:
                result.keys.add(neighbour)
    return result


def patch_set_from_patch(patch_path: str, buggy_dir: str) -> PatchSet:
    """P for one APR patch, read against the buggy checkout.

    This repeats the pipeline's own static analysis. It is deterministic
    under the same budget, so it reproduces what the pipeline saw. It costs
    one fuzz-introspector pass over the project and no model call."""
    return patch_set_from_context(TargetAnalyzer().analyze(patch_path,
                                                           buggy_dir))
