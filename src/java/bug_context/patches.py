"""Pick a patch from the drr dataset (deterministically pinned) and check out the buggy
Defects4J project version it corresponds to."""
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
import config


class DeprecatedBugError(RuntimeError):
    """Raised when defects4j refuses to check out a deprecated bug."""


@dataclass
class PatchIdentity:
    """Project/bug/patch identity derived purely from a patch's path —
    no checkout performed. Lets callers classify the bug (crashing vs.
    semantic, see `classify_bug_kind` in failure_test.py) before paying
    for the checkout a semantic-skip run would just discard."""
    project_name: str
    apr_tool: str
    bug_id: str
    patch_path: str


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
    """Picks a patch for a project from the drr dataset — always the
    first in sorted order (pinned; see suites/DATASET_AUDIT.md §6) — and
    ensures the buggy version of the project is checked out via the
    defects4j CLI."""

    def __init__(self, project_name: str = None, correct: bool = False,
                 overfitting: bool = False, patch_file: str = None,
                 bug_id=None, apr_tool: str = None):
        """``bug_id``/``apr_tool`` narrow the candidate set: only patches
        for that bug (and, when given, from that tool) are considered.
        Selection within the candidate set is ALWAYS deterministic
        (sorted-first, i.e. patch1); random sampling was removed
        2026-07-16 because sibling patches of one bug/tool can have
        opposite audit verdicts."""
        if correct and overfitting:
            raise ValueError("pass only one of correct/overfitting, not both")
        if correct:
            self.patch_dir = config.DRR_CORRECT_DIR
        elif overfitting:
            self.patch_dir = config.DRR_OVERFITTING_DIR
        else:
            raise ValueError("must pass one of correct/overfitting")
        self.project_name = project_name
        self.patch_file = patch_file
        self.bug_id = bug_id
        self.apr_tool = apr_tool

    def select(self) -> PatchSelection:
        if self.patch_file:
            return self._select_explicit(self.patch_file)

        apr_tool, target_dir = self._sample_apr_tool()
        candidates = os.listdir(target_dir)
        if self.bug_id is not None:
            candidates = [f for f in candidates
                          if f.split('-')[2] == str(self.bug_id)]
            if not candidates:
                raise ValueError(
                    f'no {self.project_name}-{self.bug_id} patch under '
                    f'{target_dir}')
        # PINNED (2026-07-16): always the first file in sorted order —
        # alphabetical puts patch1-* first even when patch10+ exist. The
        # dataset audit is per patch FILE (sibling patches of one bug/tool
        # can have opposite verdicts, e.g. Closure-86 patch2 vs patch3),
        # so a random pick silently changes what a run measures and breaks
        # comparability with certified verdicts and historical suites.
        chosen_file = sorted(candidates)[0]
        bug_id = chosen_file.split('-')[2]
        patch_path = os.path.join(target_dir, chosen_file)
        buggy_dir = self._ensure_checkout(self.project_name, bug_id)

        return PatchSelection(
            project_name=self.project_name,
            apr_tool=apr_tool,
            bug_id=bug_id,
            patch_path=patch_path,
            buggy_dir=buggy_dir,
        )

    @staticmethod
    def peek_patch_file(patch_file: str) -> PatchIdentity:
        """Parse project/apr_tool/bug_id out of an explicit patch path
        without checking anything out — the identity half of
        `_select_explicit` below, split out so callers can classify the
        bug before committing to a checkout."""
        patch_path = os.path.abspath(patch_file)
        if not os.path.isfile(patch_path):
            raise ValueError(f"patch file not found: {patch_path}")
        filename = os.path.basename(patch_path)
        project_name = os.path.basename(os.path.dirname(patch_path))
        apr_tool = os.path.basename(os.path.dirname(os.path.dirname(patch_path)))
        bug_id = filename.rsplit('.', 1)[0].split('-')[2]
        return PatchIdentity(
            project_name=project_name,
            apr_tool=apr_tool,
            bug_id=bug_id,
            patch_path=patch_path,
        )

    def _select_explicit(self, patch_file: str) -> PatchSelection:
        """Bypass random sampling and use exactly the given patch, e.g.
        .../Patches/Dcorrect/SimFix/Math/patch1-Math-5-SimFix.patch. project,
        apr_tool, and bug_id are all derived from its path/filename so
        callers only need to point at the file."""
        identity = self.peek_patch_file(patch_file)
        buggy_dir = self._ensure_checkout(identity.project_name, identity.bug_id)

        return PatchSelection(
            project_name=identity.project_name,
            apr_tool=identity.apr_tool,
            bug_id=identity.bug_id,
            patch_path=identity.patch_path,
            buggy_dir=buggy_dir,
        )

    def _sample_apr_tool(self):
        """Keep sampling APR tools until we find one that has at least
        one patch for the requested project (and, when pinned, for the
        requested bug id). Most tools only fix a subset of the d4j
        projects. A pinned apr_tool is validated once and returned."""
        def has_match(target_dir):
            if not (os.path.isdir(target_dir) and os.listdir(target_dir)):
                return False
            if self.bug_id is None:
                return True
            return any(f.split('-')[2] == str(self.bug_id)
                       for f in os.listdir(target_dir))

        if self.apr_tool is not None:
            target_dir = os.path.join(self.patch_dir, self.apr_tool,
                                      self.project_name)
            if not has_match(target_dir):
                raise ValueError(
                    f'no matching patch for tool={self.apr_tool} '
                    f'project={self.project_name} bug_id={self.bug_id} '
                    f'under {target_dir}')
            return self.apr_tool, target_dir

        eligible = [t for t in config.APR_TOOLS
                    if has_match(os.path.join(self.patch_dir, t,
                                              self.project_name))]
        if not eligible:
            raise ValueError(
                f'no APR tool has a matching patch for '
                f'project={self.project_name} bug_id={self.bug_id}')
        # PINNED (2026-07-16): deterministic tool choice for the same
        # reason as the file pick above — reruns must resolve to the same
        # patch file or results are not comparable across runs.
        apr_tool = sorted(eligible)[0]
        return apr_tool, os.path.join(self.patch_dir, apr_tool,
                                      self.project_name)

    def _ensure_checkout(self, project_name: str, bug_id: str) -> str:
        """Generates buggy directory from defects4j that corresponds with the patch
        """
        # The checkout root must exist before `defects4j checkout` runs.
        # d4j resolves the `-w` path with Cwd::abs_path, which returns undef
        # when the PARENT directory is missing; that undef reaches
        # Vcs::checkout_vid and the checkout dies with "Failed to create
        # working directory", which we would then report as a deprecated bug.
        os.makedirs(config.D4J_CHECKOUT_ROOT, exist_ok=True)
        buggy_dir = os.path.join(
            config.D4J_CHECKOUT_ROOT,
            f'{project_name}_{bug_id}_buggy',
        )
        config_file = os.path.join(buggy_dir, '.defects4j.config')
        # Reuse a cached checkout ONLY if it is the right version. A stale
        # dir — e.g. a fixed-version (`vid=<id>f`) checkout left in the
        # buggy path — otherwise gets trusted, and `defects4j export` then
        # fails with "not a Defects4J working directory", silently dropping
        # the bug. Verify the config's vid matches `<bug_id>b` before trusting.
        want_vid = f'{bug_id}b'
        cached_ok = False
        if os.path.isfile(config_file):
            try:
                with open(config_file) as fh:
                    cached_ok = f'vid={want_vid}' in fh.read()
            except OSError:
                cached_ok = False
        if not cached_ok:
            if os.path.isdir(buggy_dir):
                shutil.rmtree(buggy_dir)
            result = subprocess.run(
                ['defects4j', 'checkout',
                 '-p', project_name, '-v', f'{bug_id}b',
                 '-w', buggy_dir],
                capture_output=True,
            )
            if result.returncode != 0:
                raise DeprecatedBugError(
                    f'{project_name}-{bug_id} checkout failed '
                    f'(exit {result.returncode}): '
                    f'{result.stderr.decode(errors="replace").strip()}'
                )
        return buggy_dir