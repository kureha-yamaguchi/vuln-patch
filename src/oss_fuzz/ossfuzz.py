"""The minimal OSS-Fuzz build/run substrate: everything that shells out to
``infra/helper.py``, git, and the project's own ``build.sh``.

This is the C/C++ analogue of the Java pipeline's HarnessBuilder + Jazzer
runner, but instead of ``javac`` against a classpath we:

  1. clone the target's upstream repo once and add git *worktrees* for the
     vulnerable commit (parent of the fix) and HEAD;
  2. drop a generated libFuzzer harness into a worktree's source tree;
  3. reuse the project's existing fuzz-target compile line — cribbed from its
     ``build.sh`` so we inherit its include/link flags — to build our harness
     via ``helper.py build_fuzzers <project> <worktree>``;
  4. run it with ``helper.py run_fuzzer`` (fuzzing) or ``reproduce`` (a fixed
     PoC), detecting crashes from libFuzzer/sanitizer output markers.

The "crib" in step 3 is the one non-obvious move. Compiling a brand-new fuzz
target for an arbitrary project normally means knowing its include paths and
link libraries. We avoid guessing by copying the flags off a line that
already builds one of the project's targets (any line mentioning
``$LIB_FUZZING_ENGINE``) and swapping in our source/output names.

Everything that mutates the user's oss-fuzz checkout (the build.sh edit) is
done under try/finally so the tree is restored. ``dry_run=True`` prints every
external command and skips execution, which is how the offline tests exercise
the wiring without Docker.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass, field
from typing import List, Optional

import config

# libFuzzer / sanitizer crash markers. A positive requires one of these (or a
# persisted crash artifact) so infra/build errors — which also exit nonzero —
# are not mistaken for findings.
_CRASH_MARKERS = (
    "ERROR: AddressSanitizer",
    "ERROR: LeakSanitizer",
    "ERROR: MemorySanitizer",
    "SUMMARY: AddressSanitizer",
    "SUMMARY: MemorySanitizer",
    "SUMMARY: UndefinedBehaviorSanitizer",
    "runtime error:",              # UBSan
    "ERROR: libFuzzer: deadly signal",
    "ERROR: libFuzzer: timeout",
    "SEGV on unknown address",
    "==ERROR==",
    "AddressSanitizer: heap-buffer-overflow",
    "AddressSanitizer: heap-use-after-free",
    "AddressSanitizer: stack-buffer-overflow",
    "AddressSanitizer: global-buffer-overflow",
)

# First sanitizer/summary line makes a decent stable signature.
_SIG_RE = re.compile(
    r"(?:ERROR|SUMMARY):\s*"
    r"(AddressSanitizer|MemorySanitizer|UndefinedBehaviorSanitizer|libFuzzer):\s*"
    r"([A-Za-z0-9 _\-]+)"
)


@dataclass
class Checkout:
    label: str        # 'vuln' | 'head'
    path: str         # worktree path (mounted as $SRC/<project>)
    commit: str


@dataclass
class RunOutcome:
    triggered: bool
    timed_out: bool
    returncode: int
    stdout: str = ""
    stderr: str = ""
    crash_reason: Optional[str] = None
    signature: Optional[str] = None
    artifact_path: Optional[str] = None

    @property
    def combined(self) -> str:
        return f"{self.stdout}\n{self.stderr}"


def crash_signature(output: str) -> Optional[str]:
    """Distil a sanitizer report into a stable-ish signature so the campaign
    can tell a *different* bug from a re-find of the same one. C analogue of
    the Java crash_signature (exception@frame)."""
    m = _SIG_RE.search(output)
    if not m:
        return None
    kind, what = m.group(1), m.group(2).strip()
    # Attach the top project frame if libFuzzer printed one (#N ... in file).
    frame = None
    fm = re.search(r"#\d+\s+0x[0-9a-f]+\s+in\s+([^\s]+)", output)
    if fm:
        frame = fm.group(1)
    return f"{kind}:{what}@{frame}" if frame else f"{kind}:{what}"


def _looks_like_crash(returncode: int, combined: str) -> Optional[str]:
    for marker in _CRASH_MARKERS:
        if marker in combined:
            return f"output marker: {marker!r}"
    return None


class OssFuzz:
    """Wrapper over a local ``google/oss-fuzz`` checkout."""

    def __init__(self,
                 oss_fuzz_dir: str = None,
                 work_dir: str = None,
                 dry_run: bool = False):
        self.oss_fuzz_dir = os.path.abspath(oss_fuzz_dir or config.OSS_FUZZ_DIR)
        self.work_dir = os.path.abspath(work_dir or config.OSS_FUZZ_WORK_DIR)
        self.dry_run = dry_run
        self.helper = os.path.join(self.oss_fuzz_dir, "infra", "helper.py")
        os.makedirs(self.work_dir, exist_ok=True)

    # -- low-level ---------------------------------------------------------
    def _run(self, cmd: List[str], *, cwd: str = None,
             timeout: int = None, check: bool = False) -> subprocess.CompletedProcess:
        printable = " ".join(cmd)
        print(f"  $ {printable}" + (f"   (cwd={cwd})" if cwd else ""))
        if self.dry_run:
            return subprocess.CompletedProcess(cmd, 0, stdout="", stderr="")
        proc = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True,
                              timeout=timeout)
        if check and proc.returncode != 0:
            sys.stderr.write(proc.stdout + "\n" + proc.stderr + "\n")
            raise RuntimeError(f"command failed ({proc.returncode}): {printable}")
        return proc

    def _helper(self, *args: str, timeout: int = None,
                check: bool = False) -> subprocess.CompletedProcess:
        return self._run([sys.executable, self.helper, *args],
                         timeout=timeout, check=check)

    # -- project metadata --------------------------------------------------
    def project_dir(self, project: str) -> str:
        return os.path.join(self.oss_fuzz_dir, "projects", project)

    def project_yaml(self, project: str) -> dict:
        """Read project.yaml with a tiny hand-rolled parser (avoids a PyYAML
        dependency for the handful of top-level scalars we need)."""
        path = os.path.join(self.project_dir(project), "project.yaml")
        info: dict = {}
        try:
            with open(path) as fh:
                for line in fh:
                    m = re.match(r"^([a-zA-Z_]+):\s*(.*?)\s*$", line)
                    if m and m.group(2):
                        info[m.group(1)] = m.group(2).strip().strip('"\'')
        except FileNotFoundError:
            pass
        return info

    def harness_ext(self, language: Optional[str]) -> str:
        return ".c" if (language or "").lower() == "c" else ".cc"

    # -- git ---------------------------------------------------------------
    def clone_source(self, main_repo: str) -> str:
        """Clone the upstream repo once into work_dir; return its path."""
        name = re.sub(r"[^A-Za-z0-9_.-]", "_", main_repo.rstrip("/").split("/")[-1])
        name = name[:-4] if name.endswith(".git") else name
        repo = os.path.join(self.work_dir, f"src__{name}")
        if not os.path.isdir(os.path.join(repo, ".git")):
            self._run(["git", "clone", main_repo, repo], check=not self.dry_run)
        return repo

    def head_commit(self, repo: str) -> str:
        p = self._run(["git", "-C", repo, "rev-parse", "HEAD"])
        return (p.stdout or "HEAD").strip()

    def parent_commit(self, repo: str, commit: str) -> str:
        p = self._run(["git", "-C", repo, "rev-parse", f"{commit}~1"])
        return (p.stdout or f"{commit}~1").strip()

    def worktree(self, repo: str, commit: str, label: str) -> Checkout:
        path = os.path.join(self.work_dir, f"wt__{label}")
        if os.path.isdir(path):
            self._run(["git", "-C", repo, "worktree", "remove", "--force", path])
        self._run(["git", "-C", repo, "worktree", "add", "--detach", path, commit],
                  check=not self.dry_run)
        return Checkout(label=label, path=path, commit=commit)

    def diff(self, repo: str, a: str, b: str) -> str:
        p = self._run(["git", "-C", repo, "diff", f"{a}..{b}"])
        return p.stdout

    # -- harness build/run -------------------------------------------------
    def build_image(self, project: str) -> None:
        self._helper("build_image", "--pull", project, check=not self.dry_run)

    def _crib_compile_line(self, build_sh: str, project: str,
                           harness_name: str, ext: str) -> str:
        """Build a compile line for our harness by copying flags off an
        existing $LIB_FUZZING_ENGINE line in build.sh, or fall back to a
        generic one."""
        src = f"$SRC/{project}/{harness_name}{ext}"
        out = f"$OUT/{harness_name}"
        for line in build_sh.splitlines():
            if "$LIB_FUZZING_ENGINE" in line and (".c" in line or ".cc" in line
                                                  or ".cpp" in line or "$CXX" in line
                                                  or "$CC" in line):
                # Replace the existing source token(s) with ours and the -o
                # target with ours; keep every include/lib flag intact.
                cribbed = re.sub(r"-o\s+\S+", f"-o {out}", line)
                cribbed = re.sub(r"\$SRC/\S+\.(?:c|cc|cpp)", src, cribbed)
                if src not in cribbed:
                    cribbed = cribbed.replace("$LIB_FUZZING_ENGINE",
                                              f"$LIB_FUZZING_ENGINE {src}", 1)
                return cribbed
        compiler = "$CXX $CXXFLAGS" if ext != ".c" else "$CC $CFLAGS"
        return f'{compiler} {src} $LIB_FUZZING_ENGINE -o {out}'

    def build_harness(self, project: str, checkout: Checkout,
                      harness_name: str, harness_source: str, ext: str,
                      sanitizer: str) -> Optional[str]:
        """Write the harness into the checkout, append a cribbed compile line
        to build.sh (restored afterwards), and build. Returns the path to the
        built binary in build/out/<project>, or None on failure."""
        # 1) place harness in the source tree (mounted as $SRC/<project>).
        harness_path = os.path.join(checkout.path, f"{harness_name}{ext}")
        if not self.dry_run:
            with open(harness_path, "w") as fh:
                fh.write(harness_source)
        print(f"  wrote harness -> {harness_path}")

        # 2) append crib line to build.sh, remember original to restore.
        build_sh_path = os.path.join(self.project_dir(project), "build.sh")
        original = None
        try:
            if not self.dry_run:
                with open(build_sh_path) as fh:
                    original = fh.read()
            else:
                original = "# dry-run: existing $LIB_FUZZING_ENGINE line\n"
            crib = self._crib_compile_line(original, project, harness_name, ext)
            print(f"  crib compile line: {crib}")
            if not self.dry_run:
                with open(build_sh_path, "a") as fh:
                    fh.write(f"\n# --- vuln-patch generated harness ---\n{crib}\n")

            # 3) build against this checkout.
            proc = self._helper(
                "build_fuzzers", "--sanitizer", sanitizer,
                "--engine", "libfuzzer", project, checkout.path,
                timeout=60 * 30,
            )
            if proc.returncode != 0:
                print(f"  build failed:\n{proc.stderr[-2000:]}")
                # attach stderr so the campaign can feed it back for repair
                self.last_build_stderr = proc.stderr
                return None
        finally:
            if original is not None and not self.dry_run:
                with open(build_sh_path, "w") as fh:
                    fh.write(original)

        out_bin = os.path.join(self.oss_fuzz_dir, "build", "out", project,
                               harness_name)
        self.last_build_stderr = ""
        return out_bin

    def run_fuzzer(self, project: str, harness_name: str, seconds: int,
                   sanitizer: str, corpus: Optional[str] = None) -> RunOutcome:
        args = ["run_fuzzer", "--sanitizer", sanitizer]
        if corpus:
            args += ["--corpus-dir", corpus]
        args += [project, harness_name, "--",
                 f"-max_total_time={seconds}", "-print_final_stats=1"]
        timed_out = False
        try:
            proc = self._helper(*args, timeout=seconds + 120)
            rc, out, err = proc.returncode, proc.stdout, proc.stderr
        except subprocess.TimeoutExpired as exc:
            timed_out = True
            rc, out, err = -1, exc.stdout or "", exc.stderr or ""
            out = out.decode() if isinstance(out, bytes) else out
            err = err.decode() if isinstance(err, bytes) else err
        return self._outcome(project, harness_name, rc, out, err, timed_out)

    def reproduce(self, project: str, harness_name: str, testcase: str,
                  sanitizer: str) -> RunOutcome:
        proc = self._helper("reproduce", "--sanitizer", sanitizer,
                            project, harness_name, testcase, timeout=600)
        return self._outcome(project, harness_name, proc.returncode,
                             proc.stdout, proc.stderr, False)

    def _outcome(self, project: str, harness_name: str, rc: int,
                 out: str, err: str, timed_out: bool) -> RunOutcome:
        combined = f"{out}\n{err}"
        # A persisted crash-* artifact is the strongest signal.
        artifact = self._find_artifact(project)
        reason = None if timed_out else _looks_like_crash(rc, combined)
        if artifact and reason is None:
            reason = f"crash artifact: {os.path.basename(artifact)}"
        triggered = reason is not None
        return RunOutcome(
            triggered=triggered, timed_out=timed_out, returncode=rc,
            stdout=out, stderr=err, crash_reason=reason,
            signature=crash_signature(combined) if triggered else None,
            artifact_path=artifact,
        )

    def _find_artifact(self, project: str) -> Optional[str]:
        out_dir = os.path.join(self.oss_fuzz_dir, "build", "out", project)
        if self.dry_run or not os.path.isdir(out_dir):
            return None
        crashes = [os.path.join(out_dir, f) for f in os.listdir(out_dir)
                   if f.startswith("crash-")]
        crashes.sort(key=lambda p: os.path.getmtime(p), reverse=True)
        return crashes[0] if crashes else None

    def cleanup_worktrees(self, repo: str) -> None:
        for label in ("vuln", "head"):
            path = os.path.join(self.work_dir, f"wt__{label}")
            if os.path.isdir(path):
                self._run(["git", "-C", repo, "worktree", "remove",
                           "--force", path])
