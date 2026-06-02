"""3-state harness verifier for Linux kernel CVE sibling pairs.

Runs a compiled harness against three independent kernel checkout states
and checks whether it triggers (crashes) or exits cleanly at each state:

  pre_fix  (fix0_parent_commit)  → CRASH expected   harness targets the real bug
  fix0     (incomplete fix)      → CRASH expected   incomplete patch confirmed
  fix1     (corrective fix)      → NO CRASH         correct fix verified

A harness that passes all three checks is ground-truth confirmed: it proves
the patch in Fix-0 was incomplete because the bug is still triggerable after
Fix-0 but not after Fix-1.

Crash detection strategy:
  For syscall-sequence harnesses: a non-zero exit code signals a crash.
  The kernel may also emit WARN/BUG output to dmesg, but we can't read
  dmesg from user-space without root. Non-zero exit is sufficient signal
  for DoS-class bugs (BUG_ON, NULL deref visible as SIGSEGV/SIGKILL).
  For ASAN harnesses: ASAN writes to stderr and exits non-zero.

Note on infrastructure:
  For bugs that crash the kernel itself (not just the process), you need
  to run inside a VM with a freshly built kernel image. This module handles
  the process-level interface; VM orchestration is out of scope.

Analogous to src/java/fuzz_runner.py + variant HarnessVerifier.
"""
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

sys.path.insert(0, str(Path(__file__).parent.parent))
import config


@dataclass
class StateResult:
    state: str                # 'pre_fix', 'fix0', or 'fix1'
    checkout_path: str
    triggered: bool           # True = crash / non-zero exit
    exit_code: int
    stdout: str
    stderr: str
    timed_out: bool


@dataclass
class VerifyResult:
    harness_path: str
    attempt_label: str
    pre_fix: Optional[StateResult]
    fix0: Optional[StateResult]
    fix1: Optional[StateResult]

    @property
    def is_valid(self) -> bool:
        """True if harness triggered on pre_fix (proves it targets a real bug)."""
        return self.pre_fix is not None and self.pre_fix.triggered

    @property
    def confirms_incomplete_patch(self) -> bool:
        """True if harness triggered on fix0 (incomplete patch confirmed)."""
        return self.fix0 is not None and self.fix0.triggered

    @property
    def confirms_correct_fix(self) -> bool:
        """True if harness did NOT trigger on fix1 (corrective fix verified)."""
        return self.fix1 is not None and not self.fix1.triggered

    @property
    def ground_truth_confirmed(self) -> bool:
        """True only when all three checks pass."""
        return self.is_valid and self.confirms_incomplete_patch and self.confirms_correct_fix

    def summary(self) -> str:
        lines = [f"Harness: {self.harness_path}"]
        for r in [self.pre_fix, self.fix0, self.fix1]:
            if r is None:
                continue
            tag = "CRASH" if r.triggered else "CLEAN"
            to = " [TIMEOUT]" if r.timed_out else ""
            lines.append(f"  {r.state:<10} {tag}{to}  (exit {r.exit_code})")
        lines.append(f"  ground_truth_confirmed: {self.ground_truth_confirmed}")
        return "\n".join(lines)


class HarnessVerifier:
    """Run a compiled harness against the 3 kernel checkout states."""

    def __init__(
        self,
        pre_fix_checkout: Optional[str] = None,
        fix0_checkout: Optional[str] = None,
        fix1_checkout: Optional[str] = None,
        timeout: int = config.VERIFY_TIMEOUT_SECONDS,
    ):
        self.pre_fix_checkout = pre_fix_checkout
        self.fix0_checkout = fix0_checkout
        self.fix1_checkout = fix1_checkout
        self.timeout = timeout

    def verify(self, harness_path: str, attempt_label: str) -> VerifyResult:
        pre_fix = self._run_state("pre_fix", harness_path, self.pre_fix_checkout)
        fix0 = self._run_state("fix0", harness_path, self.fix0_checkout)
        fix1 = self._run_state("fix1", harness_path, self.fix1_checkout)
        return VerifyResult(
            harness_path=harness_path,
            attempt_label=attempt_label,
            pre_fix=pre_fix,
            fix0=fix0,
            fix1=fix1,
        )

    def _run_state(
        self,
        state: str,
        harness_path: str,
        checkout_path: Optional[str],
    ) -> Optional[StateResult]:
        if checkout_path is None:
            return None

        # Run the harness. We set cwd to the checkout so any relative
        # paths in the harness (e.g. opening /proc/... or /sys/...) resolve
        # correctly. The harness binary itself is an absolute path.
        try:
            result = subprocess.run(
                [harness_path],
                capture_output=True,
                text=True,
                timeout=self.timeout,
                cwd=checkout_path,
            )
            return StateResult(
                state=state,
                checkout_path=checkout_path,
                triggered=result.returncode != 0,
                exit_code=result.returncode,
                stdout=result.stdout[:2000],
                stderr=result.stderr[:2000],
                timed_out=False,
            )
        except subprocess.TimeoutExpired:
            # Timeout counts as a crash signal for DoS-class bugs (the
            # bug caused the kernel to hang, making the process time out).
            return StateResult(
                state=state,
                checkout_path=checkout_path,
                triggered=True,
                exit_code=-1,
                stdout="",
                stderr="",
                timed_out=True,
            )
        except Exception as e:
            return StateResult(
                state=state,
                checkout_path=checkout_path,
                triggered=False,
                exit_code=-1,
                stdout="",
                stderr=str(e),
                timed_out=False,
            )
