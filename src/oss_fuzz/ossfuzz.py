"""The minimal OSS-Fuzz build/run substrate: everything that shells out to
``infra/helper.py``, git, and the project's own ``build.sh``.

This is the C/C++ analogue of the Java pipeline's HarnessBuilder + Jazzer
runner, but instead of ``javac`` against a classpath we:

  1. clone the target's upstream repo once, then make a *self-contained*
     checkout of the vulnerable commit (parent of the fix) and of HEAD — a
     local clone, deliberately NOT a git worktree, because a worktree's ``.git``
     pointer dangles once the directory is bind-mounted into Docker (see
     ``OssFuzz.checkout``);
  2. get a generated libFuzzer harness compiled into that checkout by one of
     two placement strategies (see ``plan_harness``);
  3. build it via ``helper.py build_fuzzers <project> <worktree>``;
  4. run it with ``helper.py run_fuzzer`` (fuzzing) or ``reproduce`` (a fixed
     PoC), detecting crashes from libFuzzer/sanitizer output markers.

Step 2 is the non-obvious part: compiling a brand-new fuzz target for an
arbitrary project normally means knowing its include paths and link libraries.
There are two ways to avoid guessing, and OSS-Fuzz projects split cleanly
between them:

**crib** — write a NEW source file and append a compile line copied off a line
that already builds one of the project's targets (any line mentioning
``$LIB_FUZZING_ENGINE`` in ``build.sh``), swapping in our source/output names.
Needs such a line to exist, which requires the project to compile its harness
with an explicit command.

**overwrite** — replace the contents of an existing harness source *in place*,
keeping its path and extension, and run the project's own build completely
untouched. The build system compiles the same file it always compiles and never
learns the contents changed, so every include path, flag and library comes for
free. This is the only strategy that works for the majority of projects, whose
harnesses are built by CMake/Meson or by a script inside the upstream repo and
therefore expose no compile line to copy: libxml2's entire ``build.sh`` is the
single line ``fuzz/oss-fuzz-build.sh``. Measured over the 579 C/C++ projects in
a checkout, 305 have no cribbable compile line and a further 49 have no
``build.sh`` at all.

Everything that mutates a tree — the build.sh crib line, the overwritten
harness source — is done under try/finally so it is restored. ``dry_run=True``
prints every external command and skips execution, which is how the offline
tests exercise the wiring without Docker.
"""
from __future__ import annotations

import os
import platform
import re
import shutil
import signal
import subprocess
import sys
from dataclasses import dataclass, field
from typing import List, Optional

import config
from oss_fuzz.bugclass import (BugClass, ORACLE_HARNESS,
                               ORACLE_PROJECT_ASSERT, ORACLE_SANITIZER)

# Sanitizer reports proper: the runtime found the fault and explained it. These
# are the strongest evidence available and outrank everything below, because
# they cannot be produced by harness code deciding for itself that something
# looked wrong.
_SANITIZER_REPORT_MARKERS = (
    "ERROR: AddressSanitizer",
    "ERROR: LeakSanitizer",
    "ERROR: MemorySanitizer",
    "SUMMARY: AddressSanitizer",
    "SUMMARY: MemorySanitizer",
    "SUMMARY: UndefinedBehaviorSanitizer",
    "runtime error:",              # UBSan
    "SEGV on unknown address",
    "==ERROR==",
    "AddressSanitizer: heap-buffer-overflow",
    "AddressSanitizer: heap-use-after-free",
    "AddressSanitizer: stack-buffer-overflow",
    "AddressSanitizer: global-buffer-overflow",
)

# The process died but nothing explained why in sanitizer terms — libFuzzer
# noticed the signal, the timeout or the RSS limit. Still runtime-detected, so
# still a sanitizer-class finding, but it is also what an abort() from an
# assert or from a harness oracle looks like from the outside, which is why the
# two sets are kept apart: the markers below only decide *whether* something
# fired, never *what*.
_RUNTIME_ABORT_MARKERS = (
    "ERROR: libFuzzer: deadly signal",
    "ERROR: libFuzzer: timeout",
    "ERROR: libFuzzer: out-of-memory",
    "ERROR: libFuzzer: fuzz target exited",
)

_CRASH_MARKERS = _SANITIZER_REPORT_MARKERS + _RUNTIME_ABORT_MARKERS

# The project's own invariant checks. glibc's assert prints
# "f.c:12: fn: Assertion `x > 0' failed."; absl/glog print "Check failed: ...".
# Matched case-insensitively against the whole run output.
_ASSERT_MARKERS = (
    "assertion `",
    "assertion failed",
    "check failed:",
    "check failure:",
    "fatal error:",
)

# A harness-supplied oracle alarm, e.g.
#     fprintf(stderr, "[oracle:round-trip] decode(encode(x)) != x\n"); abort();
# The tag is mandatory for semantic bugs (see campaign.oracle_tag_missing) and
# is what makes one harness's claim distinguishable from another's — the
# variant-analysis steering feeds on those signatures, and "deadly signal" is
# the same string for every oracle in every harness.
_ORACLE_TAG_RE = re.compile(r"\[oracle:\s*([A-Za-z0-9_.\-]{1,48})\s*\]")

# First sanitizer/summary line makes a decent stable signature.
_SIG_RE = re.compile(
    r"(?:ERROR|SUMMARY):\s*"
    r"(AddressSanitizer|MemorySanitizer|UndefinedBehaviorSanitizer|libFuzzer):\s*"
    r"([A-Za-z0-9 _\-]+)"
)

# The assert text itself is the signature for a project-assert finding: two
# different violated invariants are two different bugs, and both would
# otherwise reduce to "libFuzzer: deadly signal".
_ASSERT_SIG_RE = re.compile(
    r"(?:Assertion\s+`([^']{1,80})'\s+failed"
    r"|Check\s+fail(?:ed|ure):\s*([^\n]{1,80}))", re.IGNORECASE)


# OSS-Fuzz's own defaults for keys a project.yaml omits, taken from the
# upstream checkout so our gate agrees with what infra/ would actually build:
# infra/constants.py (DEFAULT_LANGUAGE) and
# infra/build/functions/build_project.py (DEFAULT_SANITIZERS/DEFAULT_ENGINES).
_DEFAULT_LANGUAGE = "c++"
_DEFAULT_SANITIZERS = ("address", "undefined")
_DEFAULT_ENGINES = ("libfuzzer", "afl", "honggfuzz", "centipede")

# The only languages whose fuzz targets are libFuzzer C/C++ translation units,
# i.e. the ones this front-end can generate a harness for. Most OSS-Fuzz
# projects are NOT in this set (python/go/jvm/rust/javascript/swift/ruby), so
# the language check is what stops us burning a clone, a Docker image build and
# an LLM budget on a target we could never compile a .c/.cc harness into.
NATIVE_LANGUAGES = ("c", "c++")


@dataclass
class Checkout:
    label: str        # 'vuln' | 'head'
    path: str         # worktree path (mounted as $SRC/<project>)
    commit: str


# Source extensions that can hold a libFuzzer harness.
_HARNESS_EXTS = (".cc", ".cpp", ".cxx", ".c", ".C")

# A harness DEFINES LLVMFuzzerTestOneInput; a standalone driver merely declares
# and calls it, supplying its own main() so the target runs without libFuzzer.
# rawspeed's fuzz/libFuzzer_dummy_main.cpp and wireshark's
# fuzz/StandaloneFuzzTargetMain.c are drivers, and both beat the real harnesses
# on the tie-breaks below (shallower path; 'S' < 'f'). Overwriting one replaces
# the driver rather than a harness, so the build emits no target of that name.
# Requiring the parameter list to be followed by a body is what separates them:
# a declaration ends in ';', which the character class cannot cross.
_HARNESS_DEF_RE = re.compile(
    r"\bLLVMFuzzerTestOneInput\s*\([^;{)]*\)\s*\{", re.DOTALL)

# Don't read megabyte generated sources looking for LLVMFuzzerTestOneInput.
_MAX_HARNESS_SCAN_BYTES = 512 * 1024

# Path components that hint a directory holds fuzz targets, used to break ties
# when the OSV record didn't name a fuzz target.
_FUZZ_DIR_HINTS = ("fuzz", "ossfuzz", "oss-fuzz")

# Vendored trees ship their dependency's OWN harness; overwriting one builds a
# harness for the wrong library. Only a tie-break penalty, not a hard skip —
# a few projects really do keep their targets under such a path, and
# ``--base-harness`` overrides either way. Note 'contrib' is deliberately absent:
# libpng's real harness lives in contrib/oss-fuzz/.
_VENDOR_DIR_HINTS = frozenset({
    "third_party", "thirdparty", "third-party", "external", "externals",
    "vendor", "vendored", "node_modules", "subprojects", "deps",
})


def _harness_rank(rel_path: str, fuzz_target: Optional[str]) -> tuple:
    """Sort key for candidate harness sources; lowest wins.

    Name agreement with the reported fuzz target dominates, because in OSS-Fuzz
    the target name is conventionally the harness file's stem — that is also
    what makes the built binary's name predictable.
    """
    parts = rel_path.split(os.sep)
    stem = os.path.splitext(parts[-1])[0]
    tgt = (fuzz_target or "").strip()
    if tgt and stem == tgt:
        name_rank = 0
    elif tgt and tgt in stem:
        name_rank = 1
    elif tgt and stem in tgt:
        name_rank = 2
    else:
        name_rank = 3
    dirs = [p.lower() for p in parts[:-1]]
    vendor = 1 if any(d in _VENDOR_DIR_HINTS for d in dirs) else 0
    fuzzy = 0 if any(h in d for d in dirs for h in _FUZZ_DIR_HINTS) else 1
    # A file called main.cpp is named for its role in the build, not after a
    # target: rawspeed's fuzz/rawspeed/main.cpp builds 'RawSpeedFuzzer'.
    # Overwriting it works, but nothing can predict the binary's name, so prefer
    # a sibling whose stem could plausibly be one. Deliberately just 'main' —
    # a project with fuzz/fuzzer.c may well build a target called 'fuzzer'.
    untargetable = 1 if stem == "main" else 0
    # Shallower path, then lexicographic, so the choice is deterministic.
    return (name_rank, vendor, fuzzy, untargetable, len(parts), rel_path)


def find_base_harness(root: str, fuzz_target: Optional[str] = None,
                      override: Optional[str] = None) -> Optional[str]:
    """Path *relative to* ``root`` of an existing libFuzzer harness source in a
    checkout, or None if there is none to overwrite.

    This is what the ``overwrite`` placement strategy replaces. Identification
    is by a *definition* of ``LLVMFuzzerTestOneInput`` (see ``_HARNESS_DEF_RE``)
    rather than by filename, then ranked by ``_harness_rank``.
    """
    if override:
        cand = override if os.path.isabs(override) else os.path.join(root, override)
        if not os.path.isfile(cand):
            return None
        return os.path.relpath(cand, root)
    if not os.path.isdir(root):
        return None

    hits: List[str] = []
    for dirpath, dirnames, filenames in os.walk(root):
        # .git holds packed objects, not source; never worth scanning.
        dirnames[:] = [d for d in dirnames if d != ".git"]
        for fn in filenames:
            if not fn.endswith(_HARNESS_EXTS):
                continue
            full = os.path.join(dirpath, fn)
            try:
                if os.path.getsize(full) > _MAX_HARNESS_SCAN_BYTES:
                    continue
                with open(full, errors="ignore") as fh:
                    if not _HARNESS_DEF_RE.search(fh.read()):
                        continue
            except OSError:
                continue
            hits.append(os.path.relpath(full, root))
    if not hits:
        return None
    return min(hits, key=lambda p: _harness_rank(p, fuzz_target))


@dataclass
class HarnessPlacement:
    """How a generated harness gets into a project's build.

    ``mode`` is ``'crib'`` (append a copied compile line to build.sh and build a
    new file) or ``'overwrite'`` (replace an existing harness source in place
    and leave the build alone). See the module docstring and ``plan_harness``.
    """
    mode: str
    ext: str                            # extension the harness MUST be written as
    rel_path: Optional[str] = None      # overwrite: harness file, rel. to checkout
    target_name: Optional[str] = None   # overwrite: name the built binary gets
    cribbable: bool = False             # did build.sh expose a compile line?
    reason: str = ""

    def runtime_name(self, generated_name: str) -> str:
        """The name ``run_fuzzer``/``reproduce`` must use.

        Under ``overwrite`` the project's build system names the binary after
        the file we replaced, not after the name we generated — asking
        helper.py for ``vp_harness_3`` would look for a target that does not
        exist in ``$OUT``.
        """
        if self.mode == "overwrite" and self.target_name:
            return self.target_name
        return generated_name

    def describe(self) -> str:
        if self.mode == "overwrite":
            return (f"overwrite {self.rel_path} in place -> target "
                    f"'{self.target_name}' ({self.reason})")
        return f"crib a compile line from build.sh ({self.reason})"


@dataclass
class TargetSupport:
    """Whether an OSS-Fuzz project is a target this front-end can drive.

    ``reasons`` is empty iff the project is usable; each entry is a
    human-readable disqualification, so the caller can print all of them at
    once instead of failing one check at a time.
    """
    project: str
    exists: bool = False
    language: Optional[str] = None
    main_repo: Optional[str] = None
    sanitizers: List[str] = field(default_factory=list)
    engines: List[str] = field(default_factory=list)
    reasons: List[str] = field(default_factory=list)

    @property
    def supported(self) -> bool:
        return not self.reasons

    @property
    def is_native(self) -> bool:
        return (self.language or "").lower() in NATIVE_LANGUAGES


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
    # What noticed the failure: 'sanitizer' | 'project-assert' | 'harness'
    # (bugclass.ORACLE_*), or None when nothing did. A harness-supplied alarm
    # is a claim about correctness rather than a memory-safety report, and the
    # two must not be merged in the results — see finding_oracle.
    found_by: Optional[str] = None

    @property
    def combined(self) -> str:
        return f"{self.stdout}\n{self.stderr}"

    @property
    def needs_triage(self) -> bool:
        """A finding only the harness's own oracle saw. Real if the relation it
        asserts is real, which no part of this pipeline can prove — so it is
        reported as a claim, never counted as a confirmed sibling."""
        return self.found_by == ORACLE_HARNESS


def finding_oracle(output: str) -> Optional[str]:
    """*What noticed* the failure in this run output, or None if nothing did.

    The same taxonomy as ``bugclass.BugClass.oracle``, but observed rather than
    predicted — and the two are worth comparing. A semantic bug whose harness
    fires ASan found a memory bug, not the wrong-value bug it was aimed at; a
    crashing bug whose only evidence is a harness alarm found nothing a
    sanitizer would confirm. Both are real outcomes and both are mis-read if
    the run only records "triggered: true".

    Precedence is by strength of evidence: a sanitizer report cannot be
    manufactured by harness code, so it wins even when an oracle tag is also
    present (a harness that logs a tag per input and then hits a real overflow).
    An oracle tag beats a bare assert marker because a harness alarm that
    quotes an assertion in its message would otherwise be misattributed to the
    project.
    """
    if any(m in output for m in _SANITIZER_REPORT_MARKERS):
        return ORACLE_SANITIZER
    if _ORACLE_TAG_RE.search(output):
        return ORACLE_HARNESS
    low = output.lower()
    if any(m in low for m in _ASSERT_MARKERS):
        return ORACLE_PROJECT_ASSERT
    if any(m in output for m in _RUNTIME_ABORT_MARKERS):
        return ORACLE_SANITIZER
    return None


def crash_signature(output: str) -> Optional[str]:
    """Distil a run's failure into a stable-ish signature so the campaign can
    tell a *different* bug from a re-find of the same one. C analogue of the
    Java crash_signature (exception@frame).

    Signatures are also the steering input: ``variant_analysis_directive`` is
    handed the crashes found so far and told to aim elsewhere. That only works
    if distinct findings produce distinct strings, which is why the two
    non-sanitizer classes get their own forms — every harness oracle and every
    violated assert reaches libFuzzer as the identical "deadly signal", and
    collapsing them would tell the model it had already covered ground it had
    not.
    """
    mo = _ORACLE_TAG_RE.search(output)
    if mo and not any(m in output for m in _SANITIZER_REPORT_MARKERS):
        return f"oracle:{mo.group(1)}{_frame_suffix(output)}"

    ma = _ASSERT_SIG_RE.search(output)
    if ma and not any(m in output for m in _SANITIZER_REPORT_MARKERS):
        what = (ma.group(1) or ma.group(2) or "").strip()
        return f"assert:{what}" if what else "assert"

    m = _SIG_RE.search(output)
    if not m:
        return None
    kind, what = m.group(1), m.group(2).strip()
    return f"{kind}:{what}{_frame_suffix(output)}"


def _frame_suffix(output: str) -> str:
    """``@<top frame>`` if libFuzzer printed a stack (#N 0x... in sym)."""
    fm = re.search(r"#\d+\s+0x[0-9a-f]+\s+in\s+([^\s]+)", output)
    return f"@{fm.group(1)}" if fm else ""


# helper.py failures that are NOT the harness's fault. These exit nonzero just
# like a compile error, but no compiler ran, so there is nothing for the model
# to repair — the campaign must abort rather than spend attempts on them.
# Deliberately SPECIFIC. helper.py's own "ERROR:__main__:Building fuzzers
# failed." is emitted for *any* nonzero build, including a genuine compile
# error, so matching it would abort the repair loop on exactly the failures the
# repair loop exists to fix. Only messages that cannot be caused by harness
# source belong here.
_INFRA_ERROR_RES = (
    re.compile(r"Cannot use local checkout with .*", re.IGNORECASE),
    re.compile(r"Cannot connect to the Docker daemon.*", re.IGNORECASE),
    re.compile(r"docker: command not found.*", re.IGNORECASE),
    re.compile(r"Error response from daemon: .*", re.IGNORECASE),
    re.compile(r"no space left on device.*", re.IGNORECASE),
    re.compile(r"exec (?:format error|user process caused).*", re.IGNORECASE),
    re.compile(r"ERROR:__main__:Docker build failed.*", re.IGNORECASE),
    re.compile(r"(?:manifest|image) not found.*", re.IGNORECASE),
    # The *project's* build system failed before reaching our harness. Nothing
    # in the generated source can influence these, so repairing is pointless.
    # 'cannot run C compiled programs' is the signature of running amd64
    # OSS-Fuzz images on an arm64 host: autotools' configure compiles a probe
    # and executes it, and emulation cannot. Verified against bluez, whose
    # STOCK build (no harness of ours) fails identically.
    re.compile(r"configure: error: cannot run C compiled programs.*"),
    re.compile(r"configure: error: .*"),
    re.compile(r"CMake Error at .*"),
    re.compile(r"Configuring incomplete, errors occurred.*"),
    re.compile(r"(?:ninja|make): \*\*\* No rule to make target.*"),
    re.compile(r"\[vuln-patch\] TIMEOUT: command exceeded \d+s.*"),
)

# An undefined reference from *inside* a prebuilt system archive is a toolchain
# mismatch, not a harness bug: nothing in generated source changes what glibc's
# own libm.a references. glibc 2.39 ships ifunc resolvers in libm.a that need
# _dl_x86_cpu_features from a static libc, so a -no-pie link pulling libm.a
# alone fails — wireshark's OWN fuzzshark targets fail exactly this way, and the
# 20260811 run spent all 15 attempts "repairing" a harness that was never at
# fault. ld names the referencing object on the 'in function' line and the
# symbol on the next, so the two are matched separately.
_SYSTEM_ARCHIVE_REF_RE = re.compile(
    r"(/usr/(?:local/)?lib/\S*\.a)\([^)]*\): in function ")

# If the output contains real compiler diagnostics, it IS a harness problem
# even when an infra-looking line is also present.
_COMPILER_DIAG_RE = re.compile(
    r"(?:^|\n)\s*\S+\.(?:c|cc|cpp|cxx|h|hpp):\d+(?::\d+)?:\s*(?:fatal )?error:"
    r"|\berror:\s+(?:no member named|use of undeclared|unknown type name|"
    r"implicit declaration|too few arguments|too many arguments|"
    r"cannot initialize|expected )"
    r"|\bundefined (?:reference to|symbol)\b",
    re.IGNORECASE)


def _build_error_excerpt(combined: str, limit: int = 2500) -> str:
    """The interesting part of a failed build's output.

    A failed OSS-Fuzz build is mostly Docker layer chatter; the compiler
    diagnostics are a handful of lines in the middle. Prefer those (with a
    little context) over a blind tail, so the repair prompt carries the error
    instead of BuildKit progress bars.
    """
    lines = combined.splitlines()
    hits = [i for i, ln in enumerate(lines)
            if re.search(r"\berror\b|\bfatal\b|undefined reference|"
                         r"No such file or directory|Error [0-9]+", ln,
                         re.IGNORECASE)]
    if not hits:
        return combined[-limit:]
    keep: set = set()
    for i in hits:
        keep.update(range(max(0, i - 2), min(len(lines), i + 3)))
    excerpt = "\n".join(lines[i] for i in sorted(keep))
    return excerpt[-limit:]


def _first_error_line(text: str) -> str:
    """The first line of ``text`` that names an error, for one-line reports."""
    for ln in text.splitlines():
        if re.search(r"\berror\b", ln, re.IGNORECASE):
            return ln.strip()[:200]
    return "see the build log"


def _infra_error(combined: str) -> Optional[str]:
    """The infrastructure failure in ``combined``, or None if it looks like a
    genuine compile failure the model could plausibly fix."""
    # Before the compiler-diagnostic gate, which counts every undefined
    # reference as the harness's fault: this one cannot be.
    m = _SYSTEM_ARCHIVE_REF_RE.search(combined)
    if m and "undefined reference to" in combined:
        return (f"the link failed on undefined references from the prebuilt "
                f"system archive {m.group(1)}, which no harness source can "
                f"affect (toolchain/base-image mismatch)")
    if _COMPILER_DIAG_RE.search(combined):
        return None
    for rx in _INFRA_ERROR_RES:
        m = rx.search(combined)
        if m:
            return m.group(0).strip()
    return None


def _looks_like_crash(returncode: int, combined: str) -> Optional[str]:
    for marker in _CRASH_MARKERS:
        if marker in combined:
            return f"output marker: {marker!r}"
    # Semantic classes fail without any sanitizer saying so. Both normally
    # reach libFuzzer as a deadly signal and are caught above, but not always:
    # a target built with -error_exitcode, or one whose oracle calls exit()
    # rather than abort(), leaves only its own message behind. Missing those
    # would silently reject a harness that did exactly what it was asked to do.
    mo = _ORACLE_TAG_RE.search(combined)
    if mo:
        return f"harness oracle alarm: [oracle:{mo.group(1)}]"
    low = combined.lower()
    for marker in _ASSERT_MARKERS:
        if marker in low:
            return f"project invariant failed: {marker!r}"
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
        # Set while a project's build runs, so a timeout can stop that
        # project's containers rather than guessing at what to kill.
        self._active_project: Optional[str] = None
        self.last_build_stderr = ""
        self.last_build_infra_error: Optional[str] = None
        os.makedirs(self.work_dir, exist_ok=True)

    # -- low-level ---------------------------------------------------------
    def _run(self, cmd: List[str], *, cwd: str = None,
             timeout: int = None, check: bool = False) -> subprocess.CompletedProcess:
        printable = " ".join(cmd)
        print(f"  $ {printable}" + (f"   (cwd={cwd})" if cwd else ""))
        if self.dry_run:
            return subprocess.CompletedProcess(cmd, 0, stdout="", stderr="")
        proc = self._run_with_timeout(cmd, cwd=cwd, timeout=timeout)
        if check and proc.returncode != 0:
            sys.stderr.write(proc.stdout + "\n" + proc.stderr + "\n")
            raise RuntimeError(f"command failed ({proc.returncode}): {printable}")
        return proc

    def _run_with_timeout(self, cmd: List[str], *, cwd: str = None,
                          timeout: int = None) -> subprocess.CompletedProcess:
        """``subprocess.run``, but a timeout actually takes effect.

        ``subprocess.run(timeout=...)`` kills only its direct child and then
        waits for the output pipes to reach EOF. ``helper.py`` spawns
        ``docker run``, which inherits those pipes, so the wait never returns
        and the timeout is silently ineffective — observed live: a 30-minute
        build timeout still blocked for 98 minutes and counting.

        Fix: put the child in its own process group, kill the whole group on
        timeout, and stop the containers started from the project image (killing
        the docker *client* does not stop the container).
        """
        proc = subprocess.Popen(
            cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            text=True, start_new_session=True)
        try:
            out, err = proc.communicate(timeout=timeout)
            return subprocess.CompletedProcess(cmd, proc.returncode, out, err)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
            except (ProcessLookupError, PermissionError):
                proc.kill()
            self._kill_stray_containers()
            try:
                out, err = proc.communicate(timeout=30)
            except subprocess.TimeoutExpired:
                out, err = "", ""
            note = (f"\n[vuln-patch] TIMEOUT: command exceeded {timeout}s and "
                    "was killed: " + " ".join(cmd))
            return subprocess.CompletedProcess(cmd, 124, (out or "") + note,
                                               (err or "") + note)

    def _kill_stray_containers(self) -> None:
        """Stop containers left running from an OSS-Fuzz image we started."""
        try:
            listing = subprocess.run(
                ["docker", "ps", "-q", "--filter", "ancestor=gcr.io/oss-fuzz/"
                 + (self._active_project or "")],
                capture_output=True, text=True, timeout=30)
            for cid in listing.stdout.split():
                subprocess.run(["docker", "kill", cid], capture_output=True,
                               timeout=60)
        except (OSError, subprocess.SubprocessError):
            pass

    def _helper(self, *args: str, timeout: int = None,
                check: bool = False) -> subprocess.CompletedProcess:
        return self._run([sys.executable, self.helper, *args],
                         timeout=timeout, check=check)

    # -- project metadata --------------------------------------------------
    def project_dir(self, project: str) -> str:
        return os.path.join(self.oss_fuzz_dir, "projects", project)

    def project_yaml(self, project: str) -> dict:
        """Read project.yaml with a tiny hand-rolled parser (avoids a PyYAML
        dependency for the handful of top-level keys we need).

        Handles the two shapes that actually occur in the OSS-Fuzz tree:
        top-level scalars (``language: c++``) and top-level block lists
        (``sanitizers:`` followed by ``  - address`` items). Scalars come back
        as ``str`` and lists as ``list[str]``; a column-0 key ends the
        preceding list, which is what keeps a following ``vendor_ccs:`` block
        out of ``sanitizers``.
        """
        path = os.path.join(self.project_dir(project), "project.yaml")
        info: dict = {}
        list_key: Optional[str] = None
        try:
            with open(path) as fh:
                for line in fh:
                    line = line.rstrip("\n")
                    if not line.strip() or line.lstrip().startswith("#"):
                        continue
                    key = re.match(r"^([A-Za-z_][A-Za-z0-9_]*):\s*(.*?)\s*$", line)
                    if key:
                        name, value = key.group(1), key.group(2)
                        if value:
                            info[name] = value.strip().strip('"\'')
                            list_key = None
                        else:
                            # Block list opener: collect the '- item' lines.
                            list_key = name
                            info[name] = []
                        continue
                    item = re.match(r"^\s+-\s*(.+?)\s*$", line)
                    if item and list_key:
                        info[list_key].append(item.group(1).strip().strip('"\''))
        except FileNotFoundError:
            pass
        return info

    def dockerfile_workdir(self, project: str) -> Optional[str]:
        """The effective (last) ``WORKDIR`` from the project's Dockerfile."""
        path = os.path.join(self.project_dir(project), "Dockerfile")
        workdir = None
        try:
            with open(path) as fh:
                for line in fh:
                    m = re.match(r"^\s*WORKDIR\s+(\S+)", line)
                    if m:
                        workdir = m.group(1)
        except FileNotFoundError:
            pass
        return workdir

    def builds_from_local_checkout(self, project: str) -> bool:
        """Whether ``helper.py build_fuzzers <project> <local_path>`` works here.

        This whole pipeline builds from a local worktree (the vulnerable commit,
        then HEAD), and helper.py refuses that when the Dockerfile's WORKDIR is
        the shared ``/src`` root rather than the project's own subdirectory —
        it exits with "Cannot use local checkout with WORKDIR: /src" *before*
        invoking any compiler. 79 of the 1329 projects with a Dockerfile are
        like this (capstone among them), so it is worth one file read up front:
        without this check the campaign mistakes an infrastructure refusal for a
        compile error and spends its whole attempt budget asking the model to
        "fix" a harness that was never compiled.
        """
        workdir = self.dockerfile_workdir(project)
        if workdir is None:
            return True                      # no Dockerfile/WORKDIR: don't guess
        normalised = workdir.rstrip("/")
        return normalised not in ("/src", "$SRC", "${SRC}")

    def project_exists(self, project: str) -> bool:
        """True if projects/<name>/project.yaml is present in the checkout."""
        return os.path.isfile(
            os.path.join(self.project_dir(project), "project.yaml"))

    def host_warnings(self) -> List[str]:
        """Environmental facts that will bite before any project-specific issue.

        OSS-Fuzz publishes ``linux/amd64`` images only. On an arm64 host they
        run under emulation, which breaks any build that *executes* something it
        just compiled — autotools' ``configure`` does exactly that, and dies
        with "cannot run C compiled programs". This is not recoverable from
        here, and it is not the harness's fault, so say so up front rather than
        let it surface as a mysterious build failure per attempt.
        """
        warnings: List[str] = []
        machine = platform.machine().lower()
        if machine in ("arm64", "aarch64"):
            warnings.append(
                f"host architecture is {machine}, but OSS-Fuzz ships "
                "linux/amd64 images only. Builds run under emulation: expect "
                "autotools projects to fail in ./configure with 'cannot run C "
                "compiled programs' regardless of the harness. Real runs want "
                "an x86_64 Linux host.")
        return warnings

    def checkout_problems(self) -> List[str]:
        """Why ``oss_fuzz_dir`` is not a usable google/oss-fuzz checkout.

        Checked before anything expensive so a wrong OSS_FUZZ_DIR reports
        itself instead of surfacing later as a confusing helper.py traceback
        or an empty candidate list.
        """
        problems: List[str] = []
        if not os.path.isdir(self.oss_fuzz_dir):
            return [f"OSS_FUZZ_DIR does not exist: {self.oss_fuzz_dir}"]
        if not os.path.isfile(self.helper):
            problems.append(f"no infra/helper.py under {self.oss_fuzz_dir} "
                            "(not a google/oss-fuzz checkout?)")
        if not os.path.isdir(os.path.join(self.oss_fuzz_dir, "projects")):
            problems.append(f"no projects/ directory under {self.oss_fuzz_dir}")
        return problems

    def list_projects(self, native_only: bool = True) -> List[str]:
        """Every project in the checkout, optionally only the C/C++ ones.

        The language filter is the cheap half of target discovery: it is a
        local file read per project, so it narrows ~1300 projects to the few
        hundred this front-end could compile a harness for without a single
        network call.
        """
        root = os.path.join(self.oss_fuzz_dir, "projects")
        if not os.path.isdir(root):
            return []
        out: List[str] = []
        for name in sorted(os.listdir(root)):
            if not self.project_exists(name):
                continue
            if native_only:
                info = self.project_yaml(name)
                lang = str(info.get("language", _DEFAULT_LANGUAGE)).lower()
                if lang not in NATIVE_LANGUAGES:
                    continue
            out.append(name)
        return out

    def check_support(self, project: str, sanitizer: str,
                      engine: str = "libfuzzer") -> TargetSupport:
        """Can this front-end actually drive ``project`` with ``sanitizer``?

        Collects every disqualification rather than raising on the first, so
        `--project some-go-project` explains itself in one message. Missing
        keys fall back to OSS-Fuzz's own defaults, so a project.yaml that
        simply omits ``sanitizers`` is treated as supporting address+undefined
        exactly as infra/ would.
        """
        sup = TargetSupport(project=project)
        if not self.project_exists(project):
            sup.reasons.append(
                f"no such OSS-Fuzz project: projects/{project}/project.yaml "
                f"not found under {self.oss_fuzz_dir}")
            return sup

        sup.exists = True
        info = self.project_yaml(project)
        sup.language = str(info.get("language", _DEFAULT_LANGUAGE)).lower()
        sup.main_repo = info.get("main_repo") or None
        sup.sanitizers = [str(s).lower() for s in
                          (info.get("sanitizers") or _DEFAULT_SANITIZERS)]
        sup.engines = [str(e).lower() for e in
                       (info.get("fuzzing_engines") or _DEFAULT_ENGINES)]

        if not sup.is_native:
            sup.reasons.append(
                f"language '{sup.language}' is not C/C++; this front-end "
                "generates libFuzzer C/C++ harnesses only "
                f"(supported: {', '.join(NATIVE_LANGUAGES)})")
        if engine not in sup.engines:
            sup.reasons.append(
                f"project does not build with the '{engine}' engine "
                f"(project.yaml fuzzing_engines: {', '.join(sup.engines)})")
        if sanitizer not in sup.sanitizers:
            sup.reasons.append(
                f"project does not support the '{sanitizer}' sanitizer "
                f"(project.yaml sanitizers: {', '.join(sup.sanitizers)})")
        if not sup.main_repo:
            sup.reasons.append(
                "project.yaml has no main_repo; the vulnerable/HEAD sources "
                "cannot be checked out unless OSV supplies the repo URL")
        if not self.builds_from_local_checkout(project):
            sup.reasons.append(
                f"Dockerfile WORKDIR is {self.dockerfile_workdir(project)!r} "
                "(the shared /src root), so helper.py refuses to build from a "
                "local checkout — 'Cannot use local checkout with WORKDIR: "
                "/src'. This pipeline builds the vulnerable commit and HEAD "
                "from local worktrees, so the project cannot be driven at all.")
        return sup

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

    def _worktree_path(self, repo: str, label: str) -> str:
        """Worktree path for (repo, label), namespaced by repo.

        A shared ``wt__vuln`` across projects is a trap: the leftover from a
        previous project is still registered to *that* project's repo, so
        ``git -C <this repo> worktree remove`` refuses it, the directory
        survives, and ``worktree add`` then dies with "already exists".
        """
        return os.path.join(self.work_dir,
                            f"wt__{os.path.basename(repo)}__{label}")

    def _clear_worktree(self, repo: str, path: str) -> None:
        """Free up ``path`` for ``worktree add``, whichever repo owns it.

        A worktree's ``.git`` is a file pointing at
        ``<owner>/.git/worktrees/<name>``, so we can find the real owner and
        ask it to remove the worktree; anything left over (plain directory, or
        a repo that has since been deleted) is removed outright and pruned.
        """
        if not os.path.exists(path):
            return
        owner = repo
        gitfile = os.path.join(path, ".git")
        if os.path.isfile(gitfile):
            try:
                with open(gitfile) as fh:
                    text = fh.read().strip()
                marker = os.path.join(".git", "worktrees")
                if text.startswith("gitdir:") and marker in text:
                    gitdir = text.split(":", 1)[1].strip()
                    owner = gitdir.split(marker)[0].rstrip(os.sep)
            except OSError:
                pass
        self._run(["git", "-C", owner, "worktree", "remove", "--force", path])
        if os.path.exists(path) and not self.dry_run:
            shutil.rmtree(path, ignore_errors=True)
        for r in {owner, repo}:
            if os.path.isdir(os.path.join(r, ".git")):
                self._run(["git", "-C", r, "worktree", "prune"])

    def checkout(self, repo: str, commit: str, label: str) -> Checkout:
        """A self-contained checkout of ``commit``, safe to mount into Docker.

        NOT a git worktree. A worktree's ``.git`` is a *file* pointing at
        ``<repo>/.git/worktrees/<name>``, which lives outside the directory
        helper.py bind-mounts as ``$SRC/<project>`` — so inside the container
        every git command fails with "fatal: not a git repository". That breaks
        any project whose build stamps a version from git, which is common:
        coturn's CMakeLists runs ``git describe``, gets nothing, and dies with
        "set_target_properties called with incorrect number of arguments"
        several layers downstream of the real cause.

        ``git clone --local`` hardlinks the object store, so a full second
        checkout costs almost no disk and yields a real ``.git`` directory that
        works inside the container.
        """
        path = self._worktree_path(repo, label)
        self._clear_worktree(repo, path)
        proc = self._run(["git", "clone", "--local", "--no-checkout",
                          "--quiet", repo, path])
        if proc.returncode != 0 and not self.dry_run:
            # --local needs the same filesystem; fall back to a plain clone.
            self._run(["git", "clone", "--quiet", repo, path],
                      check=True)
        self._run(["git", "-C", path, "checkout", "--detach", "--quiet", commit],
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
        """A compile line for our harness, built from the flags and libraries of
        an existing ``$LIB_FUZZING_ENGINE`` line in build.sh.

        Three things the naive version got wrong on real projects:

        * **Line continuations.** bluez, assimp and boringssl all write the
          compile command across backslash-continued lines, so reading single
          lines yields a truncated command (often just
          ``$CXX $CXXFLAGS $LIB_FUZZING_ENGINE \\``).
        * **The original translation units.** Keeping the existing target's
          ``fuzz_textfile.o`` (or its ``.c``) links a second
          ``LLVMFuzzerTestOneInput`` and the build dies on a duplicate symbol.
        * **Its ``-o`` target.** Must become ours, or the binary never lands in
          ``$OUT`` under our name.

        So: keep the compiler, every flag, and every library; drop the objects,
        the sources and the old ``-o``; then add ours.
        """
        src = f"$SRC/{project}/{harness_name}{ext}"
        out = f"$OUT/{harness_name}"
        line = self._find_fuzz_compile_line(build_sh)
        if line is None:
            compiler = "$CXX $CXXFLAGS" if ext != ".c" else "$CC $CFLAGS"
            return f'{compiler} {src} $LIB_FUZZING_ENGINE -o {out}'

        # Flags that take a separate argument; keep the pair together.
        pair_flags = {"-I", "-L", "-isystem", "-include", "-Xlinker", "-x",
                      "-framework", "-idirafter", "-iquote"}
        kept: List[str] = []
        tokens = line.split()
        skip_next = False
        for i, tok in enumerate(tokens):
            if skip_next:
                skip_next = False
                continue
            if tok == "-o":
                skip_next = True                      # drop the old target
                continue
            # Strip quotes before testing suffixes: assimp writes the object as
            # "${fuzzer_name}.o", which an unquoted check misses entirely.
            bare = tok.strip('"\'')
            if bare.endswith((".o", ".obj")):
                continue                              # the old harness object
            if re.search(r"\.(?:c|cc|cpp|cxx|c\+\+)$", bare):
                continue                              # the old harness source
            if tok in pair_flags:
                kept.append(tok)
                if i + 1 < len(tokens):
                    kept.append(tokens[i + 1])
                    skip_next = True
                continue
            kept.append(tok)

        cribbed = " ".join(kept)
        if "$LIB_FUZZING_ENGINE" not in cribbed:
            cribbed += " $LIB_FUZZING_ENGINE"
        # Our source goes right after the engine so it precedes the libraries.
        cribbed = cribbed.replace("$LIB_FUZZING_ENGINE",
                                  f"$LIB_FUZZING_ENGINE {src}", 1)
        return f"{cribbed} -o {out}"

    def _read_build_sh(self, project: str) -> str:
        try:
            with open(os.path.join(self.project_dir(project), "build.sh"),
                      errors="ignore") as fh:
                return fh.read()
        except OSError:
            return ""

    def plan_harness(self, project: str, checkout: Checkout,
                     fuzz_target: Optional[str], ext: str,
                     mode: str = "auto",
                     base_harness: Optional[str] = None
                     ) -> Optional[HarnessPlacement]:
        """Decide how a generated harness gets compiled for this project.

        ``auto`` prefers ``crib`` when build.sh exposes a compile line to copy —
        it is the proven path and leaves the checkout's sources untouched — and
        falls back to ``overwrite`` otherwise. That fallback is the whole point:
        without it, a project with no cribbable line gets the generic
        ``$CC $CFLAGS harness.c $LIB_FUZZING_ENGINE -o …`` command, which has no
        include paths and no libraries and cannot compile anything that actually
        calls the library.

        Returns None only when ``mode='overwrite'`` was demanded and no existing
        harness could be found to replace — the caller decides whether that is
        fatal.
        """
        cribbable = self._find_fuzz_compile_line(self._read_build_sh(project)) is not None
        # Dry runs use fixture projects that have no build.sh in any checkout,
        # and build_harness fakes a cribbable one; agree with it so the wiring
        # test exercises the same path it always did.
        if self.dry_run and not self._read_build_sh(project):
            cribbable = True

        def _crib(reason: str) -> HarnessPlacement:
            return HarnessPlacement(mode="crib", ext=ext, cribbable=cribbable,
                                    reason=reason)

        def _overwrite(reason: str) -> Optional[HarnessPlacement]:
            rel = find_base_harness(checkout.path, fuzz_target, base_harness)
            if rel is None:
                return None
            # The binary is named by the build system after the file it
            # compiled, so the stem — not the OSV fuzz_target, which may name a
            # different target than the harness we actually matched.
            stem = os.path.splitext(os.path.basename(rel))[0]
            return HarnessPlacement(
                mode="overwrite", ext=os.path.splitext(rel)[1], rel_path=rel,
                target_name=stem, cribbable=cribbable, reason=reason)

        if mode == "crib":
            return _crib("forced by --harness-build crib")
        if mode == "overwrite":
            return _overwrite("forced by --harness-build overwrite")
        if mode != "auto":
            raise ValueError(f"unknown harness build mode: {mode!r}")

        if cribbable:
            return _crib("build.sh has a $LIB_FUZZING_ENGINE compile line")
        placement = _overwrite(
            "build.sh has no compile line to crib, so the project's own build "
            "system must compile the harness")
        if placement is not None:
            return placement
        # Nothing to overwrite either: fall back to the generic compile line so
        # behaviour matches the pre-overwrite pipeline rather than refusing.
        return _crib(
            "no compile line to crib AND no existing harness to overwrite; "
            "falling back to a generic compile line, which usually fails")

    @staticmethod
    def _find_fuzz_compile_line(build_sh: str) -> Optional[str]:
        """The first *logical* (continuation-joined) line that compiles a fuzz
        target, or None. Skips `cmake -DLIB_FUZZING_ENGINE=...`-style lines,
        which mention the engine but are not a compile command."""
        logical: List[str] = []
        buf = ""
        for raw in build_sh.splitlines():
            stripped = raw.rstrip()
            if stripped.endswith("\\"):
                buf += stripped[:-1].rstrip() + " "
                continue
            logical.append(buf + stripped)
            buf = ""
        if buf:
            logical.append(buf)

        for line in logical:
            if "$LIB_FUZZING_ENGINE" not in line:
                continue
            # Must actually invoke a compiler, not merely pass the engine along.
            if not re.search(r"\$CXX\b|\$CC\b|\bclang\+\+|\bclang\b|\bgcc\b|\bg\+\+",
                             line):
                continue
            if re.match(r"\s*(?:cmake|meson|\./configure|make)\b", line):
                continue
            return line.strip()
        return None

    def build_harness(self, project: str, checkout: Checkout,
                      harness_name: str, harness_source: str, ext: str,
                      sanitizer: str,
                      placement: Optional[HarnessPlacement] = None
                      ) -> Optional[str]:
        """Place the generated harness in ``checkout`` and build it. Returns the
        path to the built binary in build/out/<project>, or None on failure.

        Dispatches on ``placement.mode`` (see ``plan_harness``). ``placement=None``
        means the ``crib`` strategy, so existing callers keep their behaviour.
        """
        if placement is not None and placement.mode == "overwrite":
            return self._build_harness_overwrite(
                project, checkout, harness_name, harness_source, placement,
                sanitizer)
        return self._build_harness_crib(
            project, checkout, harness_name, harness_source, ext, sanitizer)

    def _run_build(self, project: str, checkout: Checkout, sanitizer: str,
                   log_tag: str) -> bool:
        """``helper.py build_fuzzers`` against a checkout.

        Records diagnostics in ``last_build_stderr`` (fed back to the model as a
        repair prompt) and ``last_build_infra_error`` (set only when no compiler
        ran, so the campaign can abort instead of blaming the harness).

        The timeout has to be generous: under amd64 emulation on an arm64 host,
        assimp compiled ~2 minutes per object file (51 of 248 in 98 minutes), so
        a half-hour cap kills legitimate builds. Configurable because the right
        value depends entirely on host architecture.
        """
        self._active_project = project
        try:
            proc = self._helper(
                "build_fuzzers", "--sanitizer", sanitizer,
                "--engine", "libfuzzer", project, checkout.path,
                timeout=config.OSS_FUZZ_BUILD_TIMEOUT,
            )
        finally:
            self._active_project = None
        if proc.returncode == 0:
            self.last_build_stderr = ""
            self.last_build_infra_error = None
            return True
        # Compiler diagnostics arrive on stdout (helper.py runs docker with its
        # output inherited) while helper.py's own messages go to stderr, so
        # printing stderr alone loses the actual error. Persist everything and
        # surface the lines that matter.
        combined = f"{proc.stdout}\n{proc.stderr}"
        log_path = os.path.join(
            self.work_dir, f"build_{checkout.label}_{log_tag}.log")
        try:
            with open(log_path, "w") as fh:
                fh.write(combined)
            print(f"  build failed; full log: {log_path}")
        except OSError:
            print("  build failed")
        print(_build_error_excerpt(combined))
        self.last_build_stderr = _build_error_excerpt(combined)
        # ...but flag infrastructure refusals (unusable local checkout, missing
        # image, Docker not running) separately: asking the model to "fix your
        # harness" for one of those burns the whole attempt budget on a file
        # that was never compiled.
        self.last_build_infra_error = _infra_error(combined)
        return False

    def stock_build_error(self, project: str, checkout: Checkout,
                          sanitizer: str) -> Optional[str]:
        """Why the project's OWN build of ``checkout`` fails, or None if it
        builds.

        Answers the question compiler output alone cannot: was it our harness at
        all? Both placement strategies restore what they touched (the base
        harness, or build.sh) before returning, so this compiles the tree as
        upstream would. Any generated harness left in the checkout is a file no
        build rule mentions.

        Worth a build because the pattern lists above cannot keep up: llamacpp's
        build.sh at oss-fuzz HEAD compiles ``fuzzers/*.cpp``, which do not exist
        at a 2024 vuln commit, and that failure looks like an ordinary
        ``clang++: error:``. A tree that does not build without us cannot be
        repaired by rewriting the harness.
        """
        if self.dry_run:
            return None
        print("  checking whether the project builds without our harness")
        if self._run_build(project, checkout, sanitizer, "stock"):
            return None
        return (f"the project's own build of the {checkout.label} checkout "
                f"fails with no harness of ours in it: "
                f"{_first_error_line(self.last_build_stderr)}")

    def _build_harness_overwrite(self, project: str, checkout: Checkout,
                                 harness_name: str, harness_source: str,
                                 placement: HarnessPlacement,
                                 sanitizer: str) -> Optional[str]:
        """Replace an existing harness source in place, then run the project's
        own build untouched.

        Nothing is added to build.sh and nothing is added to the source tree:
        the build system compiles the same file it always compiles, so our
        harness inherits the project's include paths, flags and libraries
        without us ever reconstructing a compile command. The original contents
        are restored under try/finally.
        """
        rel = placement.rel_path
        path = os.path.join(checkout.path, rel)
        run_name = placement.runtime_name(harness_name)
        print(f"  overwriting {rel} in the {checkout.label} checkout "
              f"-> target '{run_name}' (build.sh untouched)")

        original: Optional[str] = None
        try:
            if not self.dry_run:
                try:
                    with open(path, errors="ignore") as fh:
                        original = fh.read()
                    with open(path, "w") as fh:
                        fh.write(harness_source)
                except OSError as exc:
                    # Not a harness problem and it will not fix itself across
                    # attempts, so classify as infrastructure.
                    self.last_build_stderr = ""
                    self.last_build_infra_error = (
                        f"cannot overwrite base harness '{rel}' in the "
                        f"{checkout.label} checkout: {exc}")
                    print(f"  {self.last_build_infra_error}")
                    return None
            if not self._run_build(project, checkout, sanitizer, harness_name):
                return None
        finally:
            if original is not None:
                try:
                    with open(path, "w") as fh:
                        fh.write(original)
                except OSError:
                    print(f"  WARNING: could not restore {rel}; the "
                          f"{checkout.label} checkout is left modified")

        out_bin = os.path.join(self.oss_fuzz_dir, "build", "out", project,
                               run_name)
        if not self.dry_run and not os.path.exists(out_bin):
            # The build succeeded but named the binary something other than the
            # source stem. Often it merely decorated the stem — rawspeed's CMake
            # names every target '<Stem>Fuzzer' — and a unique such target is
            # safe to adopt: it is the binary built from the file we just
            # overwrote. Recorded on the placement so run_fuzzer agrees, and so
            # later attempts resolve it directly.
            avail = self._out_targets(project)
            decorated = [t for t in avail if run_name in t]
            if len(decorated) == 1:
                print(f"  the build named it '{decorated[0]}', not "
                      f"'{run_name}'; using that")
                placement.target_name = decorated[0]
                self.last_build_stderr = ""
                return os.path.join(self.oss_fuzz_dir, "build", "out", project,
                                    decorated[0])
            # Ambiguous or absent: retrying cannot help, so abort rather than
            # spend the attempt budget, and name what IS there so the fix is
            # obvious.
            self.last_build_stderr = ""
            self.last_build_infra_error = (
                f"the build produced no target named '{run_name}' (present: "
                f"{', '.join(avail[:12]) or 'none'}); this project names its "
                f"fuzz binary differently from its harness source — select the "
                f"right harness with --base-harness")
            print(f"  {self.last_build_infra_error}")
            return None
        self.last_build_stderr = ""
        return out_bin

    def _out_targets(self, project: str) -> List[str]:
        """Executable fuzz targets present in build/out/<project>."""
        out_dir = os.path.join(self.oss_fuzz_dir, "build", "out", project)
        if not os.path.isdir(out_dir):
            return []
        names = []
        for f in sorted(os.listdir(out_dir)):
            p = os.path.join(out_dir, f)
            # Targets are extensionless executables; skips .so/.dict/.options.
            if os.path.isfile(p) and os.access(p, os.X_OK) and "." not in f:
                names.append(f)
        return names

    def _build_harness_crib(self, project: str, checkout: Checkout,
                            harness_name: str, harness_source: str, ext: str,
                            sanitizer: str) -> Optional[str]:
        """Write the harness as a NEW file in the checkout, append a cribbed
        compile line to build.sh (restored afterwards), and build."""
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
            if not self._run_build(project, checkout, sanitizer, harness_name):
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
                   sanitizer: str, corpus: Optional[str] = None,
                   bug_class: Optional[BugClass] = None) -> RunOutcome:
        args = ["run_fuzzer", "--sanitizer", sanitizer]
        if corpus:
            args += ["--corpus-dir", corpus]
        args += [project, harness_name, "--",
                 f"-max_total_time={seconds}", "-print_final_stats=1"]
        # Timeout/OOM bugs are only observable under the per-input limits they
        # were found with; libFuzzer's own defaults are loose enough that the
        # bug simply never fires. Every other class adds nothing here.
        if bug_class:
            args += bug_class.libfuzzer_flags()
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
        found_by = None if timed_out else finding_oracle(combined)
        if artifact and reason is None:
            reason = f"crash artifact: {os.path.basename(artifact)}"
            # An artifact with no readable report still means libFuzzer stopped
            # on an input, which is a runtime detection like any other.
            found_by = found_by or ORACLE_SANITIZER
        triggered = reason is not None
        return RunOutcome(
            triggered=triggered, timed_out=timed_out, returncode=rc,
            stdout=out, stderr=err, crash_reason=reason,
            signature=crash_signature(combined) if triggered else None,
            artifact_path=artifact, found_by=found_by if triggered else None,
        )

    def _find_artifact(self, project: str) -> Optional[str]:
        out_dir = os.path.join(self.oss_fuzz_dir, "build", "out", project)
        if self.dry_run or not os.path.isdir(out_dir):
            return None
        crashes = [os.path.join(out_dir, f) for f in os.listdir(out_dir)
                   if f.startswith("crash-")]
        crashes.sort(key=lambda p: os.path.getmtime(p), reverse=True)
        return crashes[0] if crashes else None

    def cleanup_checkouts(self, repo: str) -> None:
        for label in ("vuln", "head"):
            self._clear_worktree(repo, self._worktree_path(repo, label))
