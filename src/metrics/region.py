"""R-hat — the approximated root-cause region, at function level.

We cannot know the true root-cause region. So we approximate it with the
developer's own fix. The maintainer who fixed the bug knew where it lived,
and they changed exactly those methods.

R-hat is therefore: **every method whose body the developer fix changes**.

Two facts make this short.

  1. `defects4j/framework/projects/<Project>/patches/<id>.src.patch` runs
     from the FIXED code to the BUGGY code — it deletes the fix. Its
     post-patch side is the buggy checkout, and the buggy checkout is the
     build we measure coverage on. So the patch is used as it is stored. No
     reversal.
  2. `diffcov.changed_methods` already maps post-patch lines to the
     enclosing declarations, with javalang. This module imports it rather
     than repeating it, exactly as `divcap` does.

A hunk that only touches a field, an import or a class-level declaration
maps to no method. `changed_methods` records those as `unmapped`. A bug
whose region is empty LEAVES the population. It does not score zero.

MEASUREMENT ONLY. Nothing here feeds a prompt, the verifier, a gate or a
verdict.
"""
import os
from dataclasses import dataclass, field
from typing import List, Set

import config
from java.execution.diffcov import ChangedMethod, changed_methods
from metrics.keys import MethodKey, key_from_changed_method


@dataclass
class Region:
    """R-hat for one bug."""
    keys: Set[MethodKey] = field(default_factory=set)
    # The same methods in patch order, with their file and line. Kept so a
    # report can show WHICH method was missed, not just how many.
    methods: List[ChangedMethod] = field(default_factory=list)
    # Changed lines that mapped to no method. See the module docstring.
    unmapped: List[dict] = field(default_factory=list)

    @property
    def size(self) -> int:
        return len(self.keys)

    @property
    def is_empty(self) -> bool:
        return not self.keys


def developer_fix_path(project: str, bug_id, d4j_home: str = None) -> str:
    """Path to the Defects4J developer fix for one bug."""
    home = d4j_home or config.D4J_HOME
    return os.path.join(home, 'framework', 'projects', project, 'patches',
                        f'{bug_id}.src.patch')


def region_from_patch(patch_text: str, buggy_dir: str) -> Region:
    """R-hat from a developer fix, read against the buggy checkout."""
    plan = changed_methods(patch_text, buggy_dir)
    region = Region(methods=list(plan.methods), unmapped=list(plan.unmapped))
    for method in plan.methods:
        region.keys.add(key_from_changed_method(method))
    return region


def region_from_defects4j(project: str, bug_id, buggy_dir: str,
                          d4j_home: str = None) -> Region:
    """R-hat for one Defects4J bug, read from its stored developer fix."""
    path = developer_fix_path(project, bug_id, d4j_home)
    with open(path, encoding='utf-8', errors='replace') as handle:
        return region_from_patch(handle.read(), buggy_dir)
