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

import hashlib
import os
import platform
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass, field
from typing import Dict, List, Optional

import config
from oss_fuzz.bugclass import (BugClass, ORACLE_HARNESS,
                               ORACLE_PROJECT_ASSERT, ORACLE_SANITIZER)
from oss_fuzz.crash_evidence import reached_functions

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

# Where a sanitizer's crash name ends and this run's addresses begin. Hex
# addresses are alphanumeric, so _SIG_RE's name capture ran straight through
# them: open62541's real overflow signed itself "heap-buffer-overflow on address
# 0x7b9c0d9e6591 at pc 0x5585a37fac0a bp 0x7ffd... sp 0x7ffd...", which is a
# different string every run. The distinct-finding gate compares signatures, so
# that made every re-find of one bug look like a new one.
_SIG_ADDRESSES_RE = re.compile(r"\s+(?:on (?:unknown )?address|at pc)\b.*$")


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

# Generous: this is a network clone of every submodule, recursively, and some
# projects vendor large ones (grok pulls google/highway). Bounded all the same,
# because an unattended sweep must not sit on a hung fetch for its whole cap.
SUBMODULE_TIMEOUT = 900

# Every OSS-Fuzz project image is built FROM this, so it is on the machine
# before any container can leave a root-owned file behind. Used only to delete
# such files; see ``OssFuzz._remove_as_root``.
CLEANUP_IMAGE = "gcr.io/oss-fuzz-base/base-builder"


def cache_name(main_repo: str) -> str:
    """Directory name for a cloned upstream repo: readable, unique per repo.

    The basename alone collides. cfengine and libreoffice both end in ``/core``,
    boost-json and nlohmann/json both in ``/json``, ibmswtpm2 and tpm2 both in
    ``/tpm2`` -- so the second of a pair to run reuses the first's clone. The
    ``has_commit`` guard in ``_clone_fix_source`` catches it, but only by
    skipping the project and blaming the repo rather than the cache.

    The hash is taken over a canonical form -- scheme dropped, host lowercased,
    trailing ``/`` and ``.git`` stripped -- so URLs naming the same repo keep
    sharing one clone. That is what the llvm projects need: llvm and llvm_libcxx
    say ``llvm-project.git`` where llvm_libcxxabi says ``llvm-project``, and
    splitting them would mean three copies of a multi-GB checkout.

    The basename still leads, because these directory names are read by humans
    while debugging a sweep; the digest only breaks ties.
    """
    url = main_repo.strip().rstrip("/")
    url = url[:-4] if url.endswith(".git") else url
    canon = re.sub(r"^[A-Za-z+]+://", "", url).lower()
    digest = hashlib.sha256(canon.encode()).hexdigest()[:8]
    name = re.sub(r"[^A-Za-z0-9_.-]", "_", url.split("/")[-1])
    return f"src__{name}__{digest}"


@dataclass
class Checkout:
    label: str        # 'vuln' | 'head'
    path: str         # worktree path (mounted as $SRC/<project>)
    commit: str


def _built_since(path: str, since: float) -> bool:
    """Whether ``path`` is a file this build produced, rather than an old one.

    ``build/out/<project>`` is never cleared: helper.py keeps existing artifacts,
    and we overwrite the same harness file every attempt, so a target from an
    earlier run sits there under exactly the name the next one expects. A build
    that reports success while producing nothing — librawspeed does this — would
    otherwise pass the "is my binary there?" check on that leftover, and the
    campaign would fuzz a previous run's harness and judge this one by it.

    An mtime beats deleting $OUT first: the leftovers are root-owned (the
    container writes them), and this also covers the target whose name the build
    decorated, which we cannot delete in advance because we do not know it.
    """
    try:
        return os.path.getmtime(path) >= since
    except OSError:
        return False


# Tools OSS-Fuzz copies into $OUT beside the real targets. Counting them as
# targets is what let an empty build masquerade as a naming problem: librawspeed
# built nothing at all, $OUT held only this, and the diagnosis told the reader to
# go and pick a different --base-harness.
_OUT_TOOLS = frozenset({"llvm-symbolizer"})

# Source extensions that can hold a libFuzzer harness.
_HARNESS_EXTS = (".cc", ".cpp", ".cxx", ".c", ".C")

# What libFuzzer names the input it stopped on, by what stopped it. All of them,
# not just 'crash-': a leak or an out-of-memory kill is a finding too when that
# is the bug being chased (see incidental_finding).
_ARTIFACT_PREFIXES = ("crash-", "leak-", "oom-", "timeout-")

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


_INCLUDE_RE = re.compile(r'^[ \t]*#[ \t]*include[ \t]*[<"]([^>"]+)[>"]',
                         re.MULTILINE)


def included_paths(source: str) -> List[str]:
    """The header paths a translation unit includes, in first-seen order."""
    out: List[str] = []
    for m in _INCLUDE_RE.finditer(source):
        if m.group(1) not in out:
            out.append(m.group(1))
    return out


def harness_includes(root: str, rel_path: Optional[str]) -> List[str]:
    """The ``#include`` lines of an existing harness source, in file order.

    Ground truth about where this project's headers live. The overwrite
    placement replaces a file that compiles TODAY under the project's own build
    with the project's own include path, so its include block is known to
    resolve — and nothing else in the prompt carries that information, which is
    why guessing at it is the most common way a generated harness fails to
    build. The 20260812 run spent 3 of ogre's 8 attempts and most of libxaac's
    30 on missing headers, while the files being replaced said
    ``#include "OgreRoot.h"`` and ``#include "ixheaac_type_def.h"`` — neither of
    them the form the model guessed.
    """
    if not rel_path:
        return []
    try:
        with open(os.path.join(root, rel_path), errors="ignore") as fh:
            text = fh.read(_MAX_HARNESS_SCAN_BYTES)
    except OSError:
        return []
    return [m.group(0).strip() for m in _INCLUDE_RE.finditer(text)]


def harness_source(root: str, rel_path: Optional[str]) -> Optional[str]:
    """A whole existing harness file, or None.

    The include block alone was already the most reliable statement in the
    prompt; the rest of the file is more of the same and then some. It is a
    complete, compiling example of how this project turns ``(data, size)`` into
    whatever its API wants — the object initialisation, the teardown, the
    ``if (size < N) return 0`` guard, the type the bytes get wrapped in — and it
    is the nearest thing this corpus has to the trigger test the Java front-end
    puts in front of the model. Rendered by
    ``prompts.LibFuzzerPromptBuilder._reference_harness_block``.
    """
    if not rel_path:
        return None
    try:
        with open(os.path.join(root, rel_path), errors="replace") as fh:
            text = fh.read(_MAX_HARNESS_SCAN_BYTES)
    except OSError:
        return None
    return text or None


def seed_corpus_from_poc(poc_path: Optional[str]) -> Optional[str]:
    """A directory holding the PoC, to seed the trigger gate's fuzz runs.

    The verify run currently starts from an empty corpus and has its whole budget
    (60s by default) to rediscover the input shape from scratch, which is the
    likeliest reason a harness that does reach the code still gets rejected as
    "compiled but did not trigger". The Java front-end does not accept that
    handicap: it hands the model the exact crashing input and tells it to call
    with that first.

    The C analogue is weaker and worth being precise about. Under the overwrite
    placement the PoC's bytes were shaped for the *original* contents of the file
    we replace, so our harness may parse them completely differently — this is a
    seed, not an anchor, and it is not expected to reproduce anything by itself.
    What it does supply is real structured input (file magic, plausible lengths,
    real keywords) for libFuzzer to mutate, which is strictly better than
    starting from the empty string.

    Returns None when there is no local PoC, which is the common case: OSV's
    reproducer URLs are login-gated, so a testcase only exists here if the user
    passed ``--reproducer``.
    """
    if not poc_path or not os.path.isfile(poc_path):
        return None
    seeds = tempfile.mkdtemp(prefix="vp-seeds-")
    try:
        shutil.copyfile(poc_path, os.path.join(seeds, "poc"))
    except OSError as exc:
        print(f"  WARNING: could not stage the PoC as a seed: {exc}")
        return None
    return seeds


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
    # Project functions this run demonstrably entered, innermost first, read off
    # the crash stack. The steering input the C front-end never computed: the
    # campaign hands these to the variant block as the covered part of the
    # root-cause region, exactly as the Java pipeline does with
    # FuzzRunResult.reached_functions. Empty on a clean run, which prints no
    # stack — see the honest limits noted at crash_evidence.reached_functions.
    reached: List[str] = field(default_factory=list)

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
    kind = m.group(1)
    what = _SIG_ADDRESSES_RE.sub("", m.group(2)).strip()
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


# What a line has to say to count as a build failure. Anchored on the colon
# forms compilers, linkers and make actually emit, because a bare search for the
# word 'error' also matches the '-Wno-error=...' entries in the CFLAGS banner
# OSS-Fuzz prints before every build — which is how the 20260812 run reported a
# flag list as the reason grok would not build, and handed the model the same
# flag list as the error to repair.
_ERROR_LINE_RE = re.compile(
    r"(?:fatal )?error:"                    # clang/gcc/ld diagnostics
    r"|undefined (?:reference to|symbol)"
    r"|No such file or directory"
    r"|\bError \d+\b"                       # make/ninja recipe failure
    r"|^[\w./-]+: cannot ",                 # mkdir/cp/ld refusing outright
    re.IGNORECASE | re.MULTILINE)

# helper.py's own commentary, present on every failed build and saying nothing
# about the cause ("ERROR:__main__:Building fuzzers failed.").
_HELPER_LOG_RE = re.compile(r"^(?:INFO|WARNING|ERROR):(?:__main__|common_utils)")


def _names_an_error(line: str) -> bool:
    return (bool(_ERROR_LINE_RE.search(line))
            and not _HELPER_LOG_RE.match(line.lstrip()))


def _build_error_excerpt(combined: str, limit: int = 2500) -> str:
    """The interesting part of a failed build's output.

    A failed OSS-Fuzz build is mostly Docker layer chatter; the compiler
    diagnostics are a handful of lines in the middle. Prefer those (with a
    little context) over a blind tail, so the repair prompt carries the error
    instead of BuildKit progress bars.
    """
    lines = combined.splitlines()
    hits = [i for i, ln in enumerate(lines) if _names_an_error(ln)]
    if not hits:
        return combined[-limit:]
    keep: set = set()
    for i in hits:
        keep.update(range(max(0, i - 2), min(len(lines), i + 3)))
    excerpt = "\n".join(lines[i] for i in sorted(keep))
    if len(excerpt) > limit:
        # Whole lines only: cutting mid-line opens the excerpt with the tail of
        # whichever line straddled the boundary, which reads as corruption.
        excerpt = excerpt[-limit:].split("\n", 1)[-1]
    return excerpt


def _first_error_line(text: str) -> str:
    """The first line of ``text`` that names an error, for one-line reports."""
    for ln in text.splitlines():
        if _names_an_error(ln):
            return ln.strip()[:200]
    return "see the build log"


_MISSING_INCLUDE_RE = re.compile(
    r"(?:fatal )?error: '([^']+)' file not found"
    r"|(?:fatal )?error: ([\w./+-]+): No such file or directory")


def missing_includes(build_output: str) -> List[str]:
    """Header paths the compiler could not find, in first-seen order.

    The single most common way a generated harness fails to build, and the one
    that repeats: 23 of libxaac's 30 attempts in the 20260812 run died on a
    missing header, and the same three names came back over and over because
    only the last compiler error was ever fed back. A name in here is a fact —
    the compiler looked and it was not there — so the campaign can both ban it
    and refuse to spend a Docker build on a harness that uses it again.
    """
    out: List[str] = []
    for m in _MISSING_INCLUDE_RE.finditer(build_output):
        name = m.group(1) or m.group(2)
        if name and name not in out:
            out.append(name)
    return out


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


# Reports about the harness's own resource use rather than about a fault in the
# library. A generated harness that never frees what it allocated leaks by
# construction, and one that allocates from an unvalidated size field runs the
# process out of memory on almost any input — both fire just as readily on a
# FIXED library. This is how three ogre harnesses "confirmed" a sibling bug on
# HEAD in the 20260812 run: two libFuzzer out-of-memory kills and a 168-byte
# leak in the harness's own Ogre::Mesh objects.
_LEAK_RE = re.compile(r"LeakSanitizer|byte\(s\) leaked")
_RESOURCE_RE = re.compile(r"libFuzzer: (?:out-of-memory|timeout)")

# A fault the library is answerable for: a sanitizer naming a memory error or
# UB, or a fatal signal. Distinguished from the leak summary, which wears the
# same 'AddressSanitizer:' prefix but carries a byte count where the error name
# goes ("SUMMARY: AddressSanitizer: 168 byte(s) leaked").
_HARD_FAULT_RE = re.compile(
    r"(?:ERROR|SUMMARY): (?:Address|Memory)Sanitizer: [A-Za-z]"
    r"|SUMMARY: UndefinedBehaviorSanitizer"
    r"|runtime error:"
    r"|ERROR: libFuzzer: deadly signal"
    r"|SEGV on unknown address")


def incidental_finding(output: str,
                       bug_class: Optional[BugClass]) -> Optional[str]:
    """Why this report is about the harness rather than the library, or None if
    it is admissible evidence about the bug being chased.

    A leak or a resource-limit kill is a real thing to have found, but it is not
    evidence that a fix missed a variant of a memory-safety bug — and it is the
    failure a generated harness is most likely to produce by accident. When the
    bug IS of that class (``bugclass`` reads ``leak``/``resource`` off the OSV
    crash type) the same report is exactly what the run came for, so the test is
    against the class rather than against the marker.
    """
    if _HARD_FAULT_RE.search(output):
        return None
    if _LEAK_RE.search(output) and not (bug_class and bug_class.leak):
        return ("the only report is a memory leak, which a harness that does "
                "not free what it allocated produces on a fixed library too")
    if _RESOURCE_RE.search(output) and not (bug_class and bug_class.resource):
        return ("the only report is libFuzzer's own out-of-memory/timeout "
                "limit, which a harness that allocates from an unvalidated "
                "size field trips on a fixed library too")
    return None


class OssFuzz:
    """Wrapper over a local ``google/oss-fuzz`` checkout."""

    def __init__(self,
                 oss_fuzz_dir: str = None,
                 work_dir: str = None,
                 dry_run: bool = False,
                 artifacts=None):
        self.oss_fuzz_dir = os.path.abspath(oss_fuzz_dir or config.OSS_FUZZ_DIR)
        self.work_dir = os.path.abspath(work_dir or config.OSS_FUZZ_WORK_DIR)
        self.dry_run = dry_run
        # artifacts.RunArtifacts, or None to keep the old behaviour: engine
        # output stays in RunOutcome and build logs stay in work_dir. Assigned
        # after construction by run.py, which only knows the project — hence
        # where the files go — once target discovery has finished.
        self.artifacts = artifacts
        self.helper = os.path.join(self.oss_fuzz_dir, "infra", "helper.py")
        # Set while a project's build runs, so a timeout can stop that
        # project's containers rather than guessing at what to kill.
        self._active_project: Optional[str] = None
        self.last_build_stderr = ""
        self.last_build_infra_error: Optional[str] = None
        # The commit each project's shared build directories were last built
        # from; see _needs_clean.
        self._built_commit: Dict[str, str] = {}
        os.makedirs(self.work_dir, exist_ok=True)

    # -- low-level ---------------------------------------------------------
    def _run(self, cmd: List[str], *, cwd: str = None, timeout: int = None,
             check: bool = False,
             env: dict = None) -> subprocess.CompletedProcess:
        printable = " ".join(cmd)
        print(f"  $ {printable}" + (f"   (cwd={cwd})" if cwd else ""))
        if self.dry_run:
            return subprocess.CompletedProcess(cmd, 0, stdout="", stderr="")
        proc = self._run_with_timeout(cmd, cwd=cwd, timeout=timeout, env=env)
        if check and proc.returncode != 0:
            sys.stderr.write(proc.stdout + "\n" + proc.stderr + "\n")
            raise RuntimeError(f"command failed ({proc.returncode}): {printable}")
        return proc

    def _run_with_timeout(self, cmd: List[str], *, cwd: str = None,
                          timeout: int = None,
                          env: dict = None) -> subprocess.CompletedProcess:
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
        # errors='replace' because a fuzzer's output is not text: libFuzzer
        # echoes the input it is holding, and ogre's image_fuzz emitted a raw
        # 0xff 167MB into a run that had just built and started fuzzing. Strict
        # decoding raises UnicodeDecodeError from communicate() -- inside our
        # own plumbing, so it escapes the campaign's per-attempt error handling
        # and takes down the whole project run.
        proc = subprocess.Popen(
            cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            text=True, errors="replace", start_new_session=True, env=env)
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
        """Clone the upstream repo once into work_dir; return its path.

        Non-interactive on purpose. A main_repo that has been deleted, renamed
        or made private looks exactly like one that needs credentials -- GitHub
        answers both by asking for a username -- so a plain ``git clone`` in an
        unattended run parks on a password prompt until something kills it.
        ``GIT_TERMINAL_PROMPT=0`` turns that into an immediate error, and the
        blank credential helper stops a stale token in ~/.gitconfig's 'store'
        from answering with 'Invalid username or token', which buries the real
        404 under an auth error and sends the reader hunting for credentials
        that do not exist. cryptofuzz is the live case.
        """
        repo = os.path.join(self.work_dir, cache_name(main_repo))
        if not os.path.isdir(os.path.join(repo, ".git")):
            self._run(["git", "-c", "credential.helper=", "clone",
                       main_repo, repo], check=not self.dry_run,
                      env={**os.environ, "GIT_TERMINAL_PROMPT": "0"})
        return repo

    def has_commit(self, repo: str, commit: str) -> bool:
        """Whether ``commit`` is present in ``repo``.

        ``parent_commit`` cannot answer this: it resolves with ``check=False``
        and falls back to the literal ``'<sha>~1'`` string, so a missing commit
        turns into a bad revision that only fails several steps later.
        """
        if self.dry_run:
            return True
        proc = self._run(
            ["git", "-C", repo, "cat-file", "-e", f"{commit}^{{commit}}"])
        return proc.returncode == 0

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
            if os.path.exists(path):
                self._remove_as_root(path)
        for r in {owner, repo}:
            if os.path.isdir(os.path.join(r, ".git")):
                self._run(["git", "-C", r, "worktree", "prune"])

    def _remove_as_root(self, path: str) -> None:
        """Remove what the build left us no permission to delete.

        An OSS-Fuzz build runs as root inside the container with the checkout
        bind-mounted, and many projects build in-tree: grok's leaves a
        root-owned build/ of 121 files in the checkout, ogre's 1532.
        ``rmtree(ignore_errors=True)`` deletes what it can and silently keeps
        the rest, so the *next* run's clone dies with "destination path already
        exists and is not an empty directory" and the project stays unrunnable
        until someone clears it by hand. Nothing about that error points at the
        container, which is what makes it expensive to diagnose.

        Borrowing the container runtime to undo what the container did needs no
        sudo, and the image is necessarily local already: root-owned files can
        only be here because a container ran here first.
        """
        parent, base = os.path.split(path.rstrip(os.sep))
        # An rm -rf running as root deserves a guard against a path that is not
        # ours to delete, however it came to be passed in.
        if not base or not os.path.abspath(path).startswith(
                os.path.abspath(self.work_dir) + os.sep):
            raise RuntimeError(f"refusing to remove '{path}': outside work_dir")
        self._run(["docker", "run", "--rm", "-v", f"{parent}:/mnt",
                   CLEANUP_IMAGE, "rm", "-rf", f"/mnt/{base}"])
        if os.path.exists(path):
            raise RuntimeError(
                f"could not clear '{path}'; it holds files owned by the "
                f"container's root that neither this user nor {CLEANUP_IMAGE} "
                f"could remove")

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
        self._init_submodules(path)
        return Checkout(label=label, path=path, commit=commit)

    def _init_submodules(self, path: str) -> None:
        """Fetch the sources this checkout only *references*, if it has any.

        A project's Dockerfile clones it with ``--recursive`` (or runs
        ``git submodule update`` itself) for 39 of the 360 C/C++ projects this
        front-end can drive. Our checkout replaces that clone, so without this
        the referenced source is a set of empty directories and the build dies
        well before a harness of ours is involved: grok's CMake stops at
        "/src/grok/src/include/spdlog does not contain a CMakeLists.txt file",
        open62541's at a generated file whose generator lives in a submodule.

        This reaches the network — ``clone --local`` hardlinks the parent's
        objects, and the submodules' objects are not among them.

        Non-fatal on purpose. A project can carry a submodule its fuzzing build
        never compiles (grok's grok-gpu-plugin is a GPU backend), and failing
        the checkout over one of those would cost a run that would have worked.
        The build speaks for itself if something it needs really is missing.
        """
        if self.dry_run or not os.path.exists(os.path.join(path, ".gitmodules")):
            return
        proc = self._run(["git", "-C", path, "submodule", "update",
                          "--init", "--recursive"], timeout=SUBMODULE_TIMEOUT)
        if proc.returncode != 0:
            print("  WARNING: some submodules did not initialise; a build that "
                  "needs them will fail")

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
        # Before the harness is placed, never after: the crib strategy writes
        # its harness into this tree as an untracked file, which is exactly what
        # a clean removes.
        self._clean_source_tree(checkout)
        if placement is not None and placement.mode == "overwrite":
            return self._build_harness_overwrite(
                project, checkout, harness_name, harness_source, placement,
                sanitizer)
        return self._build_harness_crib(
            project, checkout, harness_name, harness_source, ext, sanitizer)

    def _needs_clean(self, project: str, checkout: Checkout) -> bool:
        """Whether helper.py must wipe ``$OUT`` and ``$WORK`` before this build.

        Both are per-project directories that survive between builds, and
        helper.py reuses them unless told otherwise. That reuse is worth keeping
        *within* one commit — it is the difference between a two-minute attempt
        and a twenty-minute one — but across two different commits it is wrong,
        because what survives is not only object files:

          * ``$WORK`` holds the project's own out-of-source build tree
            (open62541 configures CMake in ``$WORK/open62541``), generated
            sources included. In the 20260812 run all three of its HEAD builds
            failed on ``UA_STATUSCODE_SEMANTICSCHANGED`` — HEAD's sources
            compiled against status-code headers generated from the *vulnerable*
            commit — and the run reported "clean on HEAD" as its result.
          * ``$OUT`` holds every crash artifact the runs have written, which is
            what the trigger gate reads as evidence (see ``_find_artifact``).
        """
        return self._built_commit.get(project) != checkout.commit

    def _clean_source_tree(self, checkout: Checkout) -> None:
        """Remove build leftovers from a checkout, best effort.

        OSS-Fuzz builds each project from a pristine copy of its sources, and
        some build.sh scripts rely on that: grok's runs ``mkdir build``, which
        fails outright on the second attempt because the first one's directory
        is still sitting in the tree. In the 20260812 run that turned an
        ordinary compile error into "the project's own build fails", and the
        campaign aborted after 1 of its 30 attempts.

        ``git clean -xdf`` covers it: build outputs are untracked, so are
        harnesses the crib strategy left behind, and tracked files a build.sh
        edited in place are left alone (they are restored by whoever wrote
        them). Submodules are tracked gitlinks and are not descended into.

        Asked on the host first (``-n`` lists, deletes nothing) and only then
        done in a container, for two reasons. Most projects build in ``$WORK``
        and leave nothing here, so the common case costs one git call instead of
        a container start; and when there IS something, it was written by the
        previous build's container and is root-owned, which a host-side clean
        cannot remove — the same problem ``_remove_as_root`` exists for.
        ``safe.directory`` goes in the container's *global* config because git
        refuses to operate on a repository owned by another uid, and that
        setting is read only from the system and global scopes.
        """
        if self.dry_run:
            return
        listed = self._run(["git", "-C", checkout.path, "clean", "-xdn"])
        leftovers = (listed.stdout or "").splitlines()
        if listed.returncode != 0 or not leftovers:
            return
        print(f"  clearing {len(leftovers)} build leftover(s) from the "
              f"{checkout.label} checkout")
        self._run(["docker", "run", "--rm", "-v", f"{checkout.path}:/mnt",
                   CLEANUP_IMAGE, "bash", "-c",
                   'git config --global --add safe.directory "*" && '
                   'git -C /mnt clean -xdfq'],
                  timeout=600)

    def _run_build(self, project: str, checkout: Checkout, sanitizer: str,
                   log_tag: str, *, clean: Optional[bool] = None) -> bool:
        """``helper.py build_fuzzers`` against a checkout.

        Records diagnostics in ``last_build_stderr`` (fed back to the model as a
        repair prompt) and ``last_build_infra_error`` (set only when no compiler
        ran, so the campaign can abort instead of blaming the harness).

        The timeout has to be generous: under amd64 emulation on an arm64 host,
        assimp compiled ~2 minutes per object file (51 of 248 in 98 minutes), so
        a half-hour cap kills legitimate builds. Configurable because the right
        value depends entirely on host architecture.
        """
        if clean is None:
            clean = self._needs_clean(project, checkout)
        args = ["build_fuzzers", "--sanitizer", sanitizer,
                "--engine", "libfuzzer"]
        if clean:
            args.append("--clean")
        args += [project, checkout.path]
        self._built_commit[project] = checkout.commit
        self._active_project = project
        try:
            proc = self._helper(*args, timeout=config.OSS_FUZZ_BUILD_TIMEOUT)
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
        # Into the run's own artifacts directory when there is one, so the
        # evidence for a project sits under the sweep that produced it rather
        # than in a cache directory shared by every run on the box.
        log_path = None
        if self.artifacts is not None:
            log_path = self.artifacts.record_build_log(
                f"{checkout.label}_{log_tag}", combined)
        if log_path is None:
            # Project-qualified: work_dir is shared across a sweep, so without it
            # the next project's attempt N silently overwrites this one's
            # evidence.
            log_path = os.path.join(
                self.work_dir, f"build_{project}_{checkout.label}_{log_tag}.log")
            try:
                with open(log_path, "w") as fh:
                    fh.write(combined)
            except OSError:
                log_path = None
        if log_path:
            print(f"  build failed; full log: {log_path}")
        else:
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

        The answer is only worth having if the tree is pristine. This runs right
        after a failed harness build, whose leftovers are still in the checkout,
        and grok's build.sh then fails on ``mkdir build`` — "File exists" — with
        no harness of ours anywhere near it. That verdict cost the whole project
        in the 20260812 run: an ordinary stale-API compile error was reported as
        a broken environment and the campaign aborted on attempt 1 of 30.
        """
        if self.dry_run:
            return None
        print("  checking whether the project builds without our harness")
        # From a pristine tree and a pristine $WORK, both: this verdict aborts
        # the whole project, so it must not rest on state the failed harness
        # build left behind. One full rebuild, once, on the failure path only.
        self._clean_source_tree(checkout)
        if self._run_build(project, checkout, sanitizer, "stock", clean=True):
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
            started = time.time()
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
        if not self.dry_run and not _built_since(out_bin, started):
            # The build succeeded but named the binary something other than the
            # source stem. Often it merely decorated the stem — rawspeed's CMake
            # names every target '<Stem>Fuzzer' — and a unique such target is
            # safe to adopt: it is the binary built from the file we just
            # overwrote. Recorded on the placement so run_fuzzer agrees, and so
            # later attempts resolve it directly.
            avail = self._out_targets(project, since=started)
            decorated = [t for t in avail if run_name in t]
            if len(decorated) == 1:
                print(f"  the build named it '{decorated[0]}', not "
                      f"'{run_name}'; using that")
                placement.target_name = decorated[0]
                self.last_build_stderr = ""
                return os.path.join(self.oss_fuzz_dir, "build", "out", project,
                                    decorated[0])
            # Ambiguous or absent: retrying cannot help, so abort rather than
            # spend the attempt budget. Which of the two it is decides what the
            # reader should do next, so say them separately: a build that made
            # no targets at all is an environment to fix, not a name to correct.
            self.last_build_stderr = ""
            if not avail:
                left_over = ("; a binary of that name is present but predates "
                             "this build, so it is an earlier run's and was "
                             "ignored" if os.path.exists(out_bin) else "")
                self.last_build_infra_error = (
                    f"the build reported success but produced no fuzz targets "
                    f"at all (expected '{run_name}'); nothing was built to run, "
                    f"so this is the build environment, not the harness"
                    f"{left_over}")
            else:
                self.last_build_infra_error = (
                    f"the build produced no target named '{run_name}' (present: "
                    f"{', '.join(avail[:12])}); this project names its fuzz "
                    f"binary differently from its harness source — select one "
                    f"of those with --base-harness")
            print(f"  {self.last_build_infra_error}")
            return None
        self.last_build_stderr = ""
        return out_bin

    def _out_targets(self, project: str, since: float = 0.0) -> List[str]:
        """Executable fuzz targets in build/out/<project>, built after ``since``.

        ``since`` defaults to 'any age' for callers that only want to know what
        the project can produce; a caller judging *this* build must pass its
        start time. See ``_built_since``.
        """
        out_dir = os.path.join(self.oss_fuzz_dir, "build", "out", project)
        if not os.path.isdir(out_dir):
            return []
        names = []
        for f in sorted(os.listdir(out_dir)):
            p = os.path.join(out_dir, f)
            # Targets are extensionless executables; skips .so/.dict/.options.
            if (os.path.isfile(p) and os.access(p, os.X_OK) and "." not in f
                    and f not in _OUT_TOOLS and _built_since(p, since)):
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
            started = time.time()
            if not self._run_build(project, checkout, sanitizer, harness_name):
                return None
        finally:
            if original is not None and not self.dry_run:
                with open(build_sh_path, "w") as fh:
                    fh.write(original)

        out_bin = os.path.join(self.oss_fuzz_dir, "build", "out", project,
                               harness_name)
        # This path never checked at all: our compile line is appended to
        # build.sh, so a build.sh that exits 0 before reaching it leaves us
        # returning a path to an earlier run's binary, or to nothing.
        if not self.dry_run and not _built_since(out_bin, started):
            self.last_build_stderr = ""
            self.last_build_infra_error = (
                f"the build reported success but did not produce "
                f"'{harness_name}'; the cribbed compile line appended to "
                f"build.sh did not run, so this is the build environment, not "
                f"the harness")
            print(f"  {self.last_build_infra_error}")
            return None
        self.last_build_stderr = ""
        return out_bin

    def run_fuzzer(self, project: str, harness_name: str, seconds: int,
                   sanitizer: str, corpus: Optional[str] = None,
                   bug_class: Optional[BugClass] = None,
                   log_tag: Optional[str] = None,
                   seeds: Optional[str] = None) -> RunOutcome:
        """``log_tag`` names this run's engine log under ``artifacts/fuzz/``.

        Supplied by the caller rather than derived from ``harness_name``,
        because that name is not ours under the overwrite placement (it is the
        replaced target's) and because the same harness is run twice — once as
        the vulnerable-build gate, once on HEAD. Deriving it would put both
        runs of every harness in one file, each overwriting the last.

        ``seeds`` is a directory of read-only starting inputs (in practice the
        recorded PoC). Each run gets a fresh COPY of it, never the directory
        itself, for two reasons: libFuzzer writes every interesting input it
        discovers into its corpus dir, so sharing one would make each harness's
        gate depend on what the previous harness happened to find — an
        order-dependent, unreproducible verdict — and the HEAD run would inherit
        a corpus grown specifically to reproduce the vulnerable build's crash.
        """
        args = ["run_fuzzer", "--sanitizer", sanitizer]
        work = None
        if not corpus and seeds:
            corpus, work = self._fresh_corpus(seeds), True
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
        started = time.time()
        try:
            proc = self._helper(*args, timeout=seconds + 120)
            rc, out, err = proc.returncode, proc.stdout, proc.stderr
        except subprocess.TimeoutExpired as exc:
            timed_out = True
            rc, out, err = -1, exc.stdout or "", exc.stderr or ""
            out = out.decode() if isinstance(out, bytes) else out
            err = err.decode() if isinstance(err, bytes) else err
        finally:
            if work and corpus:
                shutil.rmtree(corpus, ignore_errors=True)
        tag = log_tag or f"fuzz_{harness_name}"
        self._log_run(tag, args, rc, out, err)
        return self._outcome(project, rc, out, err, timed_out,
                             since=started, tag=tag, bug_class=bug_class)

    def _fresh_corpus(self, seeds: str) -> Optional[str]:
        """A new directory holding a copy of every seed file."""
        if self.dry_run or not os.path.isdir(seeds):
            return None
        dest = tempfile.mkdtemp(prefix="vp-corpus-")
        try:
            for name in os.listdir(seeds):
                src = os.path.join(seeds, name)
                if os.path.isfile(src):
                    shutil.copyfile(src, os.path.join(dest, name))
        except OSError as exc:
            print(f"  WARNING: could not stage the seed corpus: {exc}")
        return dest

    def reproduce(self, project: str, harness_name: str, testcase: str,
                  sanitizer: str) -> RunOutcome:
        args = ["reproduce", "--sanitizer", sanitizer,
                project, harness_name, testcase]
        started = time.time()
        proc = self._helper(*args, timeout=600)
        tag = f"poc_{harness_name}"
        self._log_run(tag, args, proc.returncode, proc.stdout, proc.stderr)
        return self._outcome(project, proc.returncode, proc.stdout, proc.stderr,
                             False, since=started, tag=tag)

    def _log_run(self, tag: str, args: List[str], rc: int, out: str,
                 err: str) -> None:
        """Persist one engine run's output, if this run is keeping artifacts.

        Unconditional on the outcome: a clean run's stats (execs/sec, corpus
        size, coverage counters) are what distinguish a harness that reached
        the code and found nothing from one that never got past the first
        input — a distinction ``RunOutcome.triggered`` cannot make, and the
        one the campaign's re-steering decisions rest on.
        """
        if self.artifacts is None:
            return
        self.artifacts.record_fuzz_log(
            tag, "helper.py " + " ".join(args), rc, out, err)

    def _outcome(self, project: str, rc: int, out: str, err: str,
                 timed_out: bool, *, since: float = 0.0,
                 tag: Optional[str] = None,
                 bug_class: Optional[BugClass] = None) -> RunOutcome:
        combined = f"{out}\n{err}"
        # A persisted artifact this run wrote is the strongest signal.
        artifact = self._find_artifact(project, since)
        reason = None if timed_out else _looks_like_crash(rc, combined)
        found_by = None if timed_out else finding_oracle(combined)
        if artifact and reason is None:
            reason = f"crash artifact: {os.path.basename(artifact)}"
            # An artifact with no readable report still means libFuzzer stopped
            # on an input, which is a runtime detection like any other.
            found_by = found_by or ORACLE_SANITIZER
        # Last: a report can be real and still not be about the library.
        if reason is not None:
            why = incidental_finding(combined, bug_class)
            if why:
                print(f"  not counted as a finding: {why}")
                reason = found_by = artifact = None
        triggered = reason is not None
        if triggered and tag:
            artifact = self._keep_artifact(tag, artifact) or artifact
        return RunOutcome(
            triggered=triggered, timed_out=timed_out, returncode=rc,
            stdout=out, stderr=err, crash_reason=reason,
            signature=crash_signature(combined) if triggered else None,
            artifact_path=artifact, found_by=found_by if triggered else None,
            # Guarded by a substring test, not by `triggered`: a run whose
            # finding was discounted as incidental (a harness leak, an OOM on a
            # fixed library) still walked the library, and that coverage is worth
            # steering off. The guard matters because a fuzzer's output can be
            # enormous — ogre's image_fuzz once emitted 167MB — and a run with no
            # stack at all has nothing to parse.
            reached=(reached_functions(combined)
                     if "#0 0x" in combined else []),
        )

    def _find_artifact(self, project: str, since: float = 0.0) -> Optional[str]:
        """The newest crashing input THIS run wrote, or None.

        ``build/out/<project>`` accumulates artifacts: every attempt in a
        campaign writes into the same directory and nothing clears it between
        them (see ``_needs_clean`` — within one commit we deliberately keep the
        build). So an age test is not a refinement, it is the whole check. Two
        ogre harnesses were accepted in the 20260812 run on the strength of a
        ``crash-`` file that a previous run had left there hours earlier; both
        had in fact run clean for their full 121 seconds.
        """
        out_dir = os.path.join(self.oss_fuzz_dir, "build", "out", project)
        if self.dry_run or not os.path.isdir(out_dir):
            return None
        found = [os.path.join(out_dir, f) for f in os.listdir(out_dir)
                 if f.startswith(_ARTIFACT_PREFIXES)]
        mine = [p for p in found if _built_since(p, since)]
        mine.sort(key=lambda p: os.path.getmtime(p), reverse=True)
        return mine[0] if mine else None

    def _keep_artifact(self, tag: str, path: Optional[str]) -> Optional[str]:
        """Copy a crashing input into this run's artifacts, returning the copy.

        The original lives in ``build/out/<project>``, which is wiped whenever
        the commit being built changes (see ``_needs_clean``) — so a
        vulnerable-build artifact is already gone by the time the HEAD run it
        justified has finished, and a claim in the results cites a path that no
        longer exists.
        """
        if not path or self.artifacts is None:
            return None
        return self.artifacts.record_crash_input(tag, path)

    def cleanup_checkouts(self, repo: str) -> None:
        for label in ("vuln", "head"):
            self._clear_worktree(repo, self._worktree_path(repo, label))
