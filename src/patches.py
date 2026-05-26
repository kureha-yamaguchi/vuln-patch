"""Pick a random patch from the drr dataset and check out the buggy
Defects4J project version it corresponds to."""
import os
import random
import subprocess
from dataclasses import dataclass

import config


@dataclass
class PatchSelection:
    """A patch chosen from the drr dataset alongside the checkout it
    applies to."""
    project_name: str   # Chart / Closure / Lang / Math / Time
    apr_tool: str       # e.g. 'SimFix'
    bug_id: str         # e.g. '14'
    patch_path: str     # path to the .patch file on disk
    buggy_dir: str      # working directory where the buggy version lives


class PatchSelector:
    """Picks a random patch for a project from the drr dataset and
    ensures the buggy version of the project is checked out via the
    defects4j CLI."""

    def __init__(self, project_name: str, correct: bool, overfitting: bool):
        if correct and overfitting:
            raise ValueError("pass only one of correct/overfitting, not both")
        if correct:
            self.patch_dir = config.DRR_CORRECT_DIR
        elif overfitting:
            self.patch_dir = config.DRR_OVERFITTING_DIR
        else:
            raise ValueError("must pass one of correct/overfitting")
        self.project_name = project_name

    def select(self) -> PatchSelection:
        apr_tool, target_dir = self._sample_apr_tool()
        chosen_file = random.choice(os.listdir(target_dir))
        bug_id = chosen_file.split('-')[2]
        patch_path = os.path.join(target_dir, chosen_file)
        buggy_dir = self._ensure_checkout(bug_id)

        return PatchSelection(
            project_name=self.project_name,
            apr_tool=apr_tool,
            bug_id=bug_id,
            patch_path=patch_path,
            buggy_dir=buggy_dir,
        )

    def _sample_apr_tool(self):
        """Keep sampling APR tools until we find one that has at least
        one patch for the requested project. Most tools only fix a
        subset of the d4j projects."""
        while True:
            apr_tool = random.choice(config.APR_TOOLS)
            target_dir = os.path.join(self.patch_dir, apr_tool,
                                      self.project_name)
            if os.path.isdir(target_dir) and os.listdir(target_dir):
                return apr_tool, target_dir

    def _ensure_checkout(self, bug_id: str) -> str:
        """Generates buggy directory from defects4j that corresponds with the patch
        """
        buggy_dir = os.path.join(
            config.D4J_CHECKOUT_ROOT,
            f'{self.project_name}_{bug_id}_buggy',
        )
        if not os.path.isdir(buggy_dir):
            subprocess.run(
                ['defects4j', 'checkout',
                 '-p', self.project_name, '-v', f'{bug_id}b',
                 '-w', buggy_dir],
                check=True,
            )
        return buggy_dir