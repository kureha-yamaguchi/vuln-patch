"""Offline tests for the OSS-Fuzz front-end: everything that does not need
Docker, the network, or an LLM. Run with:  python -m pytest src/oss_fuzz/tests
or plain:  python src/oss_fuzz/tests/test_offline.py
"""
import json
import os
import re
import sys

SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, SRC)

from variant import variant_analysis_directive
from oss_fuzz.osv import select_from_records, CveTarget
from oss_fuzz.analysis import DiffAnalyzer
from oss_fuzz.ossfuzz import (OssFuzz, Checkout, HarnessPlacement,
                              crash_signature, find_base_harness,
                              _looks_like_crash)
from oss_fuzz.campaign import extract_source
from oss_fuzz.prompts import LibFuzzerPromptBuilder

FIX = json.load(open(os.path.join(os.path.dirname(__file__), "fixture_osv.json")))


def test_osv_selects_newest_usable_entry():
    # Default policy: a CVE alias is NOT required, because real OSS-Fuzz OSV
    # records never carry one. Newest usable entry wins — here the 2024 one,
    # which has a fix commit but no CVE (exactly the real-world shape).
    t = select_from_records("demo", FIX["vulns"])
    assert t is not None
    assert t.osv_id == "OSV-2024-555"
    assert t.cve_id == ""
    assert t.fixed_commit == "cccc3333"
    assert t.main_repo == "https://github.com/example/demo"
    print("ok  osv selection (newest usable, CVE not required)")


def test_osv_require_cve_opt_in():
    # --require-cve narrows to alias-bearing entries: the 2023 one, not 2024.
    t = select_from_records("demo", FIX["vulns"], require_cve=True)
    assert t is not None and t.cve_id == "CVE-2023-9999"
    assert t.fixed_commit == "beef2222"
    assert t.fuzz_target == "demo_fuzzer"
    assert t.sanitizer == "address"
    assert t.reproducer_url and "testcase_id=55555" in t.reproducer_url
    # And the CVE-less entry alone yields nothing under that policy.
    assert select_from_records("demo", [FIX["vulns"][2]],
                              require_cve=True) is None
    print("ok  osv --require-cve opt-in")


def test_osv_ranks_all_usable_records_newest_first():
    # The driver needs the whole ranking, not just the winner, so it can walk
    # past a 'fixed' commit whose diff turns out to touch no source.
    from oss_fuzz.osv import rank_records
    ranked = rank_records("demo", FIX["vulns"])
    assert [t.osv_id for t in ranked] == ["OSV-2024-555", "OSV-2023-777",
                                          "OSV-2020-001"], ranked
    # select_from_records stays the head of that ranking.
    assert select_from_records("demo", FIX["vulns"]).osv_id == ranked[0].osv_id
    # And the CVE filter narrows the same ordering.
    assert [t.osv_id for t in rank_records("demo", FIX["vulns"],
                                           require_cve=True)] == \
        ["OSV-2023-777", "OSV-2020-001"]
    print("ok  osv full ranking (fallback order)")


def test_osv_skips_entries_without_a_fix_boundary():
    # No 'fixed' event => no vuln/fix boundary to diff => unusable, whatever
    # its CVE status.
    no_fix = {
        "id": "OSV-2025-1", "published": "2025-01-01T00:00:00Z",
        "aliases": ["CVE-2025-1111"],
        "affected": [{"ranges": [{"type": "GIT",
                                  "repo": "https://github.com/example/demo",
                                  "events": [{"introduced": "0"}]}]}],
    }
    assert select_from_records("demo", [no_fix]) is None
    assert select_from_records("demo", [no_fix], require_cve=True) is None
    print("ok  osv skips entries with no fix commit")


# A real OSS-Fuzz OSV details blob (libxml2 OSV-2020-1623), verbatim shape.
_DETAILS = (
    "OSS-Fuzz report: "
    "https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=24925\n\n"
    "```\nCrash type: Heap-use-after-free READ 4\nCrash state:\n"
    "xmlXIncludeIncludeNode\nxmlXIncludeDoProcess\nxmlXIncludeLoadFallback\n```\n"
)


def test_osv_parses_crash_metadata_from_details():
    # OSS-Fuzz records leave database_specific empty and put the crash info in
    # prose; the crashing frames are the steering signal, so they must survive.
    rec = {
        "id": "OSV-2020-1623", "published": "2020-01-01T00:00:00Z",
        "details": _DETAILS,
        "affected": [{"ranges": [{"type": "GIT",
                                  "repo": "https://github.com/example/demo",
                                  "events": [{"introduced": "0"},
                                             {"fixed": "dddd4444"}]}]}],
    }
    t = CveTarget.from_osv("demo", rec)
    assert t.crash_type == "Heap-use-after-free READ 4"
    assert t.crash_state == ["xmlXIncludeIncludeNode",
                             "xmlXIncludeDoProcess",
                             "xmlXIncludeLoadFallback"], t.crash_state
    assert t.report_url and "id=24925" in t.report_url
    # Heap-use-after-free is an ASan class, so no sanitizer override.
    assert t.sanitizer is None
    print("ok  osv crash-metadata parsing")


def test_osv_infers_sanitizer_from_crash_type():
    def target_for(crash_type):
        return CveTarget.from_osv("demo", {
            "id": "x", "details": f"```\nCrash type: {crash_type}\n```",
            "affected": [{"ranges": [{"type": "GIT", "repo": "r",
                                      "events": [{"fixed": "f"}]}]}],
        })
    # A UBSan/MSan-only bug class must not be run under ASan, where the
    # harness would compile and never trigger.
    assert target_for("Undefined-shift").sanitizer == "undefined"
    assert target_for("Use-of-uninitialized-value").sanitizer == "memory"
    assert target_for("Heap-buffer-overflow READ 1").sanitizer is None
    print("ok  osv sanitizer inference")


DIFF = """diff --git a/src/parser.c b/src/parser.c
index 111..222 100644
--- a/src/parser.c
+++ b/src/parser.c
@@ -8,7 +8,7 @@ int demo_parse(const char *in, size_t n) {
     char buf[16];
-    memcpy(buf, in, n);
+    memcpy(buf, in, n < sizeof(buf) ? n : sizeof(buf));
     return process(buf);
 }
"""

PARSER_C = """#include <string.h>

static int process(char *b) { return b[0]; }

int demo_parse(const char *in, size_t n) {
    char buf[16];
    memcpy(buf, in, n);
    return process(buf);
}
"""


def test_diff_analysis_extracts_enclosing_function(tmp_path=None):
    import tempfile
    d = tempfile.mkdtemp()
    os.makedirs(os.path.join(d, "src"))
    with open(os.path.join(d, "src", "parser.c"), "w") as fh:
        fh.write(PARSER_C)
    ctx = DiffAnalyzer(language="c").analyze(DIFF, d)
    names = [f.name for f in ctx.functions]
    assert "demo_parse" in names, names
    fn = next(f for f in ctx.functions if f.name == "demo_parse")
    assert "memcpy" in fn.source and "return process" in fn.source
    # Reachable set = the touched fn + its PROJECT callees. memcpy is in the
    # body but is libc, so it is not a steering target (see _LIBC_NAMES).
    assert "demo_parse" in ctx.root_cause_reachable
    assert "process" in ctx.root_cause_reachable
    assert "memcpy" not in ctx.root_cause_reachable, ctx.root_cause_reachable
    print("ok  diff analysis")


BUILD_SH = """#!/bin/bash -eu
cd $SRC/demo
make -j$(nproc)
$CC $CFLAGS -I$SRC/demo/include -c fuzzer.c -o fuzzer.o
$CC $CFLAGS $LIB_FUZZING_ENGINE fuzzer.o -o $OUT/demo_fuzzer $SRC/demo/libdemo.a
"""


def test_build_sh_crib_reuses_flags():
    of = OssFuzz(oss_fuzz_dir="/nonexistent", work_dir="/tmp/vp_test_wd",
                 dry_run=True)
    line = of._crib_compile_line(BUILD_SH, "demo", "vp_harness_1", ".c")
    # keeps the include/link flags, swaps in our source + output
    assert "$OUT/vp_harness_1" in line
    assert "$SRC/demo/vp_harness_1.c" in line
    assert "$LIB_FUZZING_ENGINE" in line
    assert "libdemo.a" in line  # link flag inherited
    print("ok  build.sh crib:", line)


def test_crash_detection_and_signature():
    asan = ("==12==ERROR: AddressSanitizer: heap-buffer-overflow on address\n"
            "    #0 0x55 in demo_parse src/parser.c:10\n"
            "SUMMARY: AddressSanitizer: heap-buffer-overflow src/parser.c:10")
    assert _looks_like_crash(1, asan) is not None
    assert _looks_like_crash(0, "clean run, no findings") is None
    sig = crash_signature(asan)
    assert sig and "AddressSanitizer" in sig and "demo_parse" in sig, sig
    print("ok  crash detection:", sig)


def test_extract_source_from_fence_and_raw():
    fenced = "sure:\n```c\nint LLVMFuzzerTestOneInput(const uint8_t*d,size_t n){return 0;}\n```\n"
    assert "LLVMFuzzerTestOneInput" in extract_source(fenced)
    raw = "int LLVMFuzzerTestOneInput(const uint8_t*d,size_t n){return 0;}"
    assert extract_source(raw) == raw
    assert extract_source("no code here") is None
    print("ok  source extraction")


def test_prompt_builder_uses_shared_steering():
    import tempfile
    d = tempfile.mkdtemp()
    os.makedirs(os.path.join(d, "src"))
    with open(os.path.join(d, "src", "parser.c"), "w") as fh:
        fh.write(PARSER_C)
    ctx = DiffAnalyzer(language="c").analyze(DIFF, d)
    msgs = LibFuzzerPromptBuilder("c").build(
        context=ctx, covered_functions=["process"],
        found_signatures=["AddressSanitizer:heap-buffer-overflow@demo_parse"],
        harness_name="vp_harness")
    assert len(msgs) == 2 and msgs[0]["role"] == "system"
    user = msgs[1]["content"]
    # The steering block must be the shared function's output verbatim, not a
    # local reimplementation of it — that delegation is the only thing keeping
    # the rule in one place.
    assert variant_analysis_directive(
        ctx.root_cause_reachable, ["process"],
        ["AddressSanitizer:heap-buffer-overflow@demo_parse"]) in user
    assert "Uncovered functions to steer toward" in user
    assert "LLVMFuzzerTestOneInput" in user
    print("ok  prompt builder + steering")


def test_analysis_excludes_tests_harnesses_and_dedupes():
    # Regression from the capstone run: a broad upstream sync touched four
    # different main() functions plus the project's own LLVMFuzzerTestOneInput,
    # all of which landed in the steering list as reachability goals.
    import tempfile
    d = tempfile.mkdtemp()
    for rel in ("utils.c", "suite/fuzz/fuzz_harness.c", "cstool/cstool.c",
                "tests/test_x.c"):
        os.makedirs(os.path.join(d, os.path.dirname(rel)) if os.path.dirname(rel)
                    else d, exist_ok=True)
    with open(os.path.join(d, "utils.c"), "w") as fh:
        fh.write("int readBytes16(const char *p, int n) {\n"
                 "    return p[n];\n}\n")
    with open(os.path.join(d, "suite/fuzz/fuzz_harness.c"), "w") as fh:
        fh.write("int LLVMFuzzerTestOneInput(const char *d, int s) {\n"
                 "    return 0;\n}\n")
    with open(os.path.join(d, "cstool/cstool.c"), "w") as fh:
        fh.write("int main(int argc, char **argv) {\n    return 0;\n}\n")
    with open(os.path.join(d, "tests/test_x.c"), "w") as fh:
        fh.write("int main(void) {\n    return 0;\n}\n")

    diff = "".join(
        f"--- a/{p}\n+++ b/{p}\n@@ -1,2 +1,2 @@\n+changed\n"
        for p in ("utils.c", "suite/fuzz/fuzz_harness.c", "cstool/cstool.c",
                  "tests/test_x.c"))
    ctx = DiffAnalyzer(language="c").analyze(diff, d)
    names = [f.name for f in ctx.functions]
    assert "readBytes16" in names, names
    # The project's own harness is what we are writing, not a target.
    assert "LLVMFuzzerTestOneInput" not in names, names
    # main() cannot be called from a libFuzzer harness.
    assert "main" not in names, names
    assert len(names) == len(set(names)), names
    assert "suite/fuzz/fuzz_harness.c" in ctx.skipped_paths
    assert "tests/test_x.c" in ctx.skipped_paths
    assert "LLVMFuzzerTestOneInput" not in ctx.root_cause_reachable
    print("ok  analysis excludes tests/harnesses, dedupes, drops main")


def test_analysis_mines_headers_for_inline_functions():
    """C++ fixes often land in headers, and those must not be discarded.

    Regression from the assimp run: OSV-2026-505 fixes only
    include/assimp/StreamReader.h (inline template code). Treating headers as
    declaration-only produced zero touched functions, so the fix-quality gate
    rejected a perfectly good target as "touches no C/C++ function".
    """
    import tempfile
    d = tempfile.mkdtemp()
    os.makedirs(os.path.join(d, "include"))
    hdr = "include/StreamReader.h"
    with open(os.path.join(d, hdr), "w") as fh:
        fh.write(
            "#pragma once\n"
            "class StreamReader {\n"
            "public:\n"
            "    void IncPtr(size_t plus) {\n"
            "        if (mCurrent + plus > mLimit) throw DeadlyImportError();\n"
            "        mCurrent += plus;\n"
            "    }\n"
            "    int OtherDecl(int x);\n"   # a prototype, not a definition
            "};\n")
    diff = (f"--- a/{hdr}\n+++ b/{hdr}\n@@ -4,3 +4,3 @@\n"
            "+        if (mCurrent + plus > mLimit) throw DeadlyImportError();\n")
    ctx = DiffAnalyzer(language="c++").analyze(diff, d)
    names = [f.name for f in ctx.functions]
    assert "IncPtr" in names, names
    # The header is still advertised for #include purposes.
    assert "StreamReader.h" in ctx.headers
    # A bare prototype must not be mistaken for a touched definition.
    assert "OtherDecl" not in names, names
    # And a header under a non-library dir is still skipped.
    os.makedirs(os.path.join(d, "test"), exist_ok=True)
    with open(os.path.join(d, "test", "helper.h"), "w") as fh:
        fh.write("inline int helper() {\n    return 1;\n}\n")
    diff2 = ("--- a/test/helper.h\n+++ b/test/helper.h\n@@ -1,2 +1,2 @@\n"
             "+    return 2;\n")
    ctx2 = DiffAnalyzer(language="c++").analyze(diff2, d)
    assert [f.name for f in ctx2.functions] == [], ctx2.functions
    assert "test/helper.h" in ctx2.skipped_paths
    print("ok  analysis mines headers for inline functions")


def test_analysis_drops_unnameable_functions_and_libc_noise():
    # From the coturn run: an unnameable header yielded a function called '?',
    # and the heuristic reachable set listed strstr/strtoul/strlen/tolower as
    # steering targets. Neither is something a harness can be steered toward.
    import tempfile
    d = tempfile.mkdtemp()
    with open(os.path.join(d, "ns_turn_msg.c"), "w") as fh:
        fh.write(
            "int is_http(const char *s, size_t n) {\n"
            "    if (strstr(s, \"HTTP\") && strlen(s) > n) return tolower(*s);\n"
            "    return findstr(s, n);\n"
            "}\n"
            "MACRO_DEFINED_THING(x)\n"
            "{\n"
            "    changed();\n"
            "}\n")
    diff = ("--- a/ns_turn_msg.c\n+++ b/ns_turn_msg.c\n"
            "@@ -1,3 +1,3 @@\n+    if (strstr(s, \"HTTP\")) return 1;\n")
    ctx = DiffAnalyzer(language="c").analyze(diff, d)
    names = [f.name for f in ctx.functions]
    assert "?" not in names and "" not in names, names
    assert "is_http" in names, names
    for libc in ("strstr", "strlen", "tolower"):
        assert libc not in ctx.root_cause_reachable, ctx.root_cause_reachable
    # A project-local callee must survive the denylist.
    assert "findstr" in ctx.root_cause_reachable, ctx.root_cause_reachable
    print("ok  analysis drops '?' functions and libc noise")


def test_prompt_includes_original_crash_stack():
    import tempfile
    d = tempfile.mkdtemp()
    os.makedirs(os.path.join(d, "src"))
    with open(os.path.join(d, "src", "parser.c"), "w") as fh:
        fh.write(PARSER_C)
    ctx = DiffAnalyzer(language="c").analyze(DIFF, d)
    user = LibFuzzerPromptBuilder("c").build(
        context=ctx, covered_functions=[], found_signatures=[],
        harness_name="vp_harness",
        crash_type="Heap-use-after-free READ 4",
        crash_state=["xmlXIncludeIncludeNode", "xmlXIncludeDoProcess"],
    )[1]["content"]
    assert "Heap-use-after-free READ 4" in user
    assert "xmlXIncludeIncludeNode" in user
    assert "xmlXIncludeDoProcess" in user
    # Omitting the crash info must not emit an empty section.
    plain = LibFuzzerPromptBuilder("c").build(
        context=ctx, covered_functions=[], found_signatures=[],
        harness_name="vp_harness")[1]["content"]
    assert "ORIGINAL bug this fix addressed" not in plain
    print("ok  prompt carries the original crash stack")


# -- project targeting -------------------------------------------------------

def _fake_checkout(projects: dict) -> str:
    """Build a throwaway oss-fuzz-shaped tree: {project: project.yaml text}."""
    import tempfile
    root = tempfile.mkdtemp()
    os.makedirs(os.path.join(root, "infra"))
    with open(os.path.join(root, "infra", "helper.py"), "w") as fh:
        fh.write("# stub\n")
    for name, yaml_text in projects.items():
        pdir = os.path.join(root, "projects", name)
        os.makedirs(pdir)
        with open(os.path.join(pdir, "project.yaml"), "w") as fh:
            fh.write(yaml_text)
    return root


_NATIVE_YAML = """\
homepage: "https://example.com"
language: c++
vendor_ccs:
  - "someone@example.com"
sanitizers:
  - address
  - undefined
fuzzing_engines:
  - libfuzzer
  - afl
main_repo: 'https://github.com/example/native.git'
"""

_PY_YAML = """\
language: python
main_repo: "https://github.com/example/py.git"
"""


def _of(root):
    # work_dir is always a real temp dir: OssFuzz creates it eagerly, and some
    # cases deliberately point oss_fuzz_dir at a path that does not exist.
    import tempfile
    return OssFuzz(oss_fuzz_dir=root, work_dir=tempfile.mkdtemp(),
                   dry_run=True)


def test_project_yaml_parses_scalars_and_block_lists():
    root = _fake_checkout({"native": _NATIVE_YAML})
    info = _of(root).project_yaml("native")
    assert info["language"] == "c++"
    assert info["main_repo"] == "https://github.com/example/native.git"
    assert info["sanitizers"] == ["address", "undefined"]
    assert info["fuzzing_engines"] == ["libfuzzer", "afl"]
    # A following column-0 key must terminate the preceding list.
    assert info["vendor_ccs"] == ["someone@example.com"]
    print("ok  project.yaml scalars + block lists")


def test_checkout_problems_detects_a_bad_oss_fuzz_dir():
    good = _fake_checkout({"native": _NATIVE_YAML})
    assert _of(good).checkout_problems() == []
    missing = _of("/nonexistent/oss-fuzz-xyz").checkout_problems()
    assert len(missing) == 1 and "does not exist" in missing[0]
    import tempfile
    empty = tempfile.mkdtemp()          # a real dir that is not a checkout
    problems = _of(empty).checkout_problems()
    assert any("helper.py" in p for p in problems)
    assert any("projects/" in p for p in problems)
    print("ok  checkout validation")


def test_check_support_gates_language_engine_and_sanitizer():
    root = _fake_checkout({"native": _NATIVE_YAML, "pyproj": _PY_YAML})
    of = _of(root)

    ok = of.check_support("native", "address")
    assert ok.supported and ok.is_native and ok.reasons == []
    assert ok.main_repo == "https://github.com/example/native.git"

    # Non-C/C++ project: the check that saves a clone + image build + LLM spend.
    py = of.check_support("pyproj", "address")
    assert not py.supported and py.language == "python"
    assert any("not C/C++" in r for r in py.reasons)

    # Sanitizer the project does not build.
    mem = of.check_support("native", "memory")
    assert not mem.supported
    assert any("memory" in r and "sanitizer" in r for r in mem.reasons)

    # Engine the project does not build.
    eng = of.check_support("native", "address", engine="centipede")
    assert not eng.supported
    assert any("centipede" in r for r in eng.reasons)

    # Unknown project name (a typo) is reported as such, not as a repo problem.
    nope = of.check_support("libxml", "address")
    assert not nope.supported and not nope.exists
    assert any("no such OSS-Fuzz project" in r for r in nope.reasons)
    print("ok  project support gate")


def test_check_support_uses_oss_fuzz_defaults_when_keys_absent():
    # project.yaml omitting language/sanitizers/fuzzing_engines must be treated
    # the way OSS-Fuzz infra treats it: c++ / address+undefined / libfuzzer+...
    root = _fake_checkout({"bare": 'main_repo: "https://github.com/e/b.git"\n'})
    sup = _of(root).check_support("bare", "address")
    assert sup.supported, sup.reasons
    assert sup.language == "c++"
    assert "undefined" in sup.sanitizers and "libfuzzer" in sup.engines
    # ...and a project with no main_repo cannot be checked out.
    root2 = _fake_checkout({"norepo": "language: c\n"})
    sup2 = _of(root2).check_support("norepo", "address")
    assert not sup2.supported
    assert any("main_repo" in r for r in sup2.reasons)
    print("ok  project.yaml default fallbacks")


def test_list_projects_filters_to_native_languages():
    root = _fake_checkout({"native": _NATIVE_YAML, "pyproj": _PY_YAML,
                           "cproj": "language: c\nmain_repo: 'https://x/y.git'\n"})
    of = _of(root)
    assert of.list_projects(native_only=True) == ["cproj", "native"]
    assert of.list_projects(native_only=False) == ["cproj", "native", "pyproj"]
    print("ok  native-only project listing")


def test_workdir_preflight_rejects_shared_src_root():
    # Regression: capstone's Dockerfile has 'WORKDIR $SRC', so helper.py exits
    # with "Cannot use local checkout with WORKDIR: /src" before running any
    # compiler. 79/1329 projects are like this; catching it costs one file read
    # instead of a Docker image build plus the whole LLM attempt budget.
    import tempfile
    root = _fake_checkout({"shared": _NATIVE_YAML, "ownsubdir": _NATIVE_YAML,
                           "nodockerfile": _NATIVE_YAML})
    for name, workdir in (("shared", "$SRC"), ("ownsubdir", "$SRC/ownsubdir/")):
        with open(os.path.join(root, "projects", name, "Dockerfile"), "w") as fh:
            fh.write("FROM gcr.io/oss-fuzz-base/base-builder\n"
                     "RUN git clone https://example.com/x\n"
                     f"WORKDIR {workdir}\n")
    of = _of(root)
    assert of.dockerfile_workdir("shared") == "$SRC"
    assert not of.builds_from_local_checkout("shared")
    assert of.builds_from_local_checkout("ownsubdir")
    # No Dockerfile at all: don't guess, don't block.
    assert of.builds_from_local_checkout("nodockerfile")

    bad = of.check_support("shared", "address")
    assert not bad.supported
    assert any("WORKDIR" in r and "local checkout" in r for r in bad.reasons)
    assert of.check_support("ownsubdir", "address").supported
    print("ok  Dockerfile WORKDIR preflight")


def test_infra_errors_are_not_mistaken_for_compile_errors():
    # helper.py exits nonzero for both; only one is the harness's fault.
    from oss_fuzz.ossfuzz import _infra_error
    assert _infra_error(
        'ERROR:__main__:Cannot use local checkout with "WORKDIR: /src".')
    assert _infra_error("Cannot connect to the Docker daemon at unix:///x.sock")
    # A real compile error must NOT be classified as infrastructure...
    assert _infra_error(
        "/src/capstone/vp_harness_1.cc:12:3: error: use of undeclared "
        "identifier 'cs_open'") is None
    assert _infra_error("undefined reference to `cs_disasm'") is None
    # helper.py's generic failure line is emitted for real compile errors too,
    # so it must NOT abort the repair loop (coturn run: it did).
    assert _infra_error("INFO:__main__:Running: docker run ...\n"
                        "ERROR:__main__:Building fuzzers failed.") is None
    # But a failure to build the *image* is genuinely not the harness's fault.
    assert _infra_error("ERROR:__main__:Docker build failed.")
    # The project's own build system failing before our harness is compiled is
    # equally unrepairable. Both of these are verified real failures: bluez's
    # STOCK build (no harness of ours) produces the configure error on arm64,
    # and coturn's CMake error came from a dangling worktree .git.
    assert _infra_error(
        "checking whether we are cross compiling... configure: error: in "
        "`/src/bluez':\nconfigure: error: cannot run C compiled programs.")
    assert _infra_error("CMake Error at CMakeLists.txt:70 (string):")
    assert _infra_error("-- Configuring incomplete, errors occurred!")
    # ...even when an infra-shaped line is also present in the same log.
    mixed = ("ERROR:__main__:something noisy\n"
             "/src/x/vp_harness_1.cc:5:1: fatal error: 'capstone.h' file not "
             "found")
    assert _infra_error(mixed) is None
    # But an undefined reference from inside a prebuilt system archive is a
    # toolchain mismatch: glibc 2.39's libm.a ifunc resolvers want
    # _dl_x86_cpu_features, and wireshark's OWN fuzzshark targets fail this way,
    # so no harness rewrite can help. Verified against the 20260811 run, which
    # spent 15/15 attempts on it.
    libm = ("/usr/bin/ld: /usr/lib/x86_64-linux-gnu/libm-2.39.a(s_ceil.o): in "
            "function `__ceil_ifunc':\n"
            "(.text+0x6): undefined reference to `_dl_x86_cpu_features'\n"
            "clang++: error: linker command failed with exit code 1")
    assert _infra_error(libm)
    print("ok  infra vs compile error classification")


def test_crib_joins_continuations_and_drops_the_old_harness_units():
    """Real build.sh shapes that the single-line crib got wrong.

    bluez/assimp/boringssl write the compile command across backslash-continued
    lines; reading one line yields '$CXX $CXXFLAGS $LIB_FUZZING_ENGINE \\'.
    Keeping the old target's .o also links a second LLVMFuzzerTestOneInput.
    """
    of = OssFuzz(oss_fuzz_dir="/nonexistent", work_dir="/tmp/vp_test_crib",
                 dry_run=True)
    bluez_like = (
        "#!/bin/bash -eu\n"
        "make -j$(nproc)\n"
        "$CC $CFLAGS -c fuzz_textfile.c -o fuzz_textfile.o\n"
        "$CXX $CXXFLAGS $LIB_FUZZING_ENGINE \\\n"
        "    fuzz_textfile.o -o $OUT/fuzz_textfile \\\n"
        "    $STATIC_LIBS -ldl -lpthread\n")
    line = of._crib_compile_line(bluez_like, "bluez", "vp_harness_1", ".cc")
    assert not line.rstrip().endswith("\\"), line
    assert "fuzz_textfile.o" not in line, line       # no duplicate symbol
    assert "$OUT/fuzz_textfile" not in line, line    # not the old target
    assert "-o $OUT/vp_harness_1" in line, line
    assert "$SRC/bluez/vp_harness_1.cc" in line, line
    assert "$STATIC_LIBS" in line and "-lpthread" in line, line  # libs kept

    # assimp quotes the object: "${fuzzer_name}.o" must still be dropped.
    assimp_like = ('$CXX $CXXFLAGS $LIB_FUZZING_ENGINE "${fuzzer_name}.o" \\\n'
                   '    -o "$OUT/${fuzzer_name}" ./lib/libassimp.a -lpthread\n')
    line = of._crib_compile_line(assimp_like, "assimp", "vp_harness_1", ".cc")
    assert ".o" not in line, line
    assert "libassimp.a" in line, line

    # A cmake line mentioning the engine is not a compile command: fall back.
    cmake_only = ('cmake -DFUZZER=ON -DLIB_FUZZING_ENGINE="$LIB_FUZZING_ENGINE"'
                  ' ../.\nmake -j$(nproc)\n')
    line = of._crib_compile_line(cmake_only, "coturn", "vp_harness_1", ".c")
    assert line.startswith("$CC $CFLAGS"), line
    assert "cmake" not in line, line
    print("ok  crib joins continuations, drops old units, skips cmake")


def test_run_timeout_is_enforced_even_when_a_grandchild_holds_the_pipes():
    """A timeout must fire even if a grandchild keeps stdout open.

    Regression: `subprocess.run(timeout=...)` kills only its direct child, then
    waits for the pipes to hit EOF. helper.py spawns `docker run`, which
    inherits them, so the wait never returns — a 30-minute build cap was
    observed still blocking at 98 minutes. _run_with_timeout puts the child in
    its own process group and kills the group.
    """
    import time
    of = OssFuzz(oss_fuzz_dir="/nonexistent", work_dir="/tmp/vp_test_to",
                 dry_run=False)
    # Parent exits immediately; the grandchild lives on holding the pipe.
    script = ("import subprocess,sys;"
              "subprocess.Popen([sys.executable,'-c',"
              "'import time;time.sleep(120);print(\"grandchild\")']);"
              "time.sleep(120)")
    started = time.time()
    proc = of._run_with_timeout([sys.executable, "-c", "import time;" + script],
                                timeout=3)
    elapsed = time.time() - started
    # Must return promptly, not hang for the full 120s sleep.
    assert elapsed < 45, f"timeout did not take effect ({elapsed:.0f}s)"
    assert proc.returncode == 124, proc.returncode
    assert "TIMEOUT: command exceeded 3s" in proc.stdout + proc.stderr
    # ...and such a timeout is infrastructure, never a harness compile error.
    from oss_fuzz.ossfuzz import _infra_error
    assert _infra_error(proc.stdout + proc.stderr)
    print(f"ok  run timeout enforced ({elapsed:.1f}s, not 120s)")


def test_build_error_excerpt_prefers_diagnostics_over_docker_chatter():
    from oss_fuzz.ossfuzz import _build_error_excerpt
    noise = "\n".join(f"#{i} extracting sha256:{i:064x} done" for i in range(200))
    combined = (noise + "\n"
                "/src/coturn/vp_harness_1.c:9:5: error: implicit declaration "
                "of function 'stun_get_message_len_str'\n"
                "    9 |     stun_get_message_len_str(buf);\n"
                + noise)
    out = _build_error_excerpt(combined)
    assert "implicit declaration" in out, out
    # A blind tail would have returned only layer chatter.
    assert out.count("extracting sha256") < 10, out
    # With no diagnostics at all, fall back to the tail rather than nothing.
    assert _build_error_excerpt(noise).strip()
    print("ok  build error excerpt targets diagnostics")


def test_campaign_aborts_on_infra_error_without_burning_attempts():
    from oss_fuzz.campaign import HarnessCampaign

    class _Gen:
        calls = 0
        def generate(self, messages):
            _Gen.calls += 1
            return ("```c\nint LLVMFuzzerTestOneInput(const unsigned char *d, "
                    "unsigned long s){return 0;}\n```")

    class _OfInfra:
        last_build_stderr = "ERROR:__main__:Cannot use local checkout with x"
        last_build_infra_error = "Cannot use local checkout with x"
        def build_harness(self, *a, **k):
            return None

    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_OfInfra(),
                           project="p", vuln_checkout=None, sanitizer="address",
                           ext=".c", target_successes=1, max_attempts=5)
    res = camp.run(lambda covered, sigs: [{"role": "user", "content": "x"}])
    assert res.infra_error and res.achieved == 0
    # One attempt, not five: the point of the abort.
    assert res.attempts == 1, res.attempts
    assert _Gen.calls == 1, _Gen.calls
    print("ok  campaign aborts on infra error")


def test_campaign_aborts_when_the_project_does_not_build_without_us():
    """A compile error the model cannot possibly fix, because the tree is broken.

    llamacpp's build.sh at oss-fuzz HEAD compiles fuzzers/*.cpp, absent at the
    2024 vuln commit: 'clang++: error: no such file or directory'. That matches
    no infra pattern and reads as an ordinary compile error, so the 20260811 run
    fed it back 15 times. One stock build settles it.
    """
    from oss_fuzz.campaign import HarnessCampaign

    calls = []

    class _Gen:
        def generate(self, messages):
            calls.append(1)
            return ("```c\nint LLVMFuzzerTestOneInput(const unsigned char *d, "
                    "unsigned long s){return 0;}\n```")

    class _Of:
        last_build_stderr = ("clang++: error: no such file or directory: "
                             "'fuzzers/fuzz_json_to_grammar.cpp'")
        last_build_infra_error = None    # looks repairable; it is not
        stock_builds = 0

        def build_harness(self, *a, **k):
            return None

        def stock_build_error(self, project, checkout, sanitizer):
            _Of.stock_builds += 1
            return "the project's own build of the vuln checkout fails"

    of = _Of()
    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=of, project="llamacpp",
                           vuln_checkout=None, sanitizer="address", ext=".cc",
                           target_successes=3, max_attempts=15)
    res = camp.run(lambda covered, sigs: [{"role": "user", "content": "x"}])
    assert res.infra_error and res.achieved == 0
    assert res.attempts == 1, res.attempts
    assert len(calls) == 1, calls
    assert _Of.stock_builds == 1, _Of.stock_builds

    # And when the tree DOES build, the failure really is the harness's: keep
    # repairing, and do not pay for the stock build again on every attempt.
    class _OfOk(_Of):
        stock_builds = 0

        def stock_build_error(self, project, checkout, sanitizer):
            _OfOk.stock_builds += 1
            return None

    calls.clear()
    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_OfOk(),
                           project="capstone", vuln_checkout=None,
                           sanitizer="address", ext=".cc", target_successes=3,
                           max_attempts=4)
    res = camp.run(lambda covered, sigs: [{"role": "user", "content": "x"}])
    assert res.infra_error is None and res.attempts == 4, res
    assert _OfOk.stock_builds == 1, _OfOk.stock_builds
    print("ok  campaign aborts when the stock build fails too")


def test_checkout_is_self_contained_for_docker_mounting():
    """A checkout must work as a git repo with only its own directory present.

    Regression from the coturn run: we used `git worktree`, whose .git is a
    *file* pointing at <repo>/.git/worktrees/<name>. helper.py bind-mounts only
    the checkout as $SRC/<project>, so inside the container every git call died
    with "fatal: not a git repository". coturn's CMakeLists runs `git describe`
    for BUILD_VERSION, got nothing, and failed configuration three layers later.
    """
    import subprocess
    import tempfile
    root = tempfile.mkdtemp()
    repo = os.path.join(root, "src__proj")
    os.makedirs(repo)
    env = {**os.environ, "GIT_AUTHOR_NAME": "t", "GIT_AUTHOR_EMAIL": "t@t",
           "GIT_COMMITTER_NAME": "t", "GIT_COMMITTER_EMAIL": "t@t"}
    subprocess.run(["git", "init", "-q", repo], check=True)
    with open(os.path.join(repo, "f.txt"), "w") as fh:
        fh.write("v1\n")
    subprocess.run(["git", "-C", repo, "add", "f.txt"], check=True)
    subprocess.run(["git", "-C", repo, "commit", "-q", "-m", "one"],
                   check=True, env=env)
    subprocess.run(["git", "-C", repo, "tag", "v1.0"], check=True)
    head = subprocess.run(["git", "-C", repo, "rev-parse", "HEAD"],
                          capture_output=True, text=True).stdout.strip()

    of = OssFuzz(oss_fuzz_dir=_fake_checkout({"native": _NATIVE_YAML}),
                 work_dir=root, dry_run=False)
    co = of.checkout(repo, head, "vuln")

    # .git must be a real directory, not a pointer file into the parent repo.
    dotgit = os.path.join(co.path, ".git")
    assert os.path.isdir(dotgit), "checkout .git is not a real directory"
    assert not os.path.isfile(dotgit)

    # The decisive check: git must work with the parent repo GONE, which is
    # what "mounted alone into a container" amounts to.
    import shutil as _sh
    _sh.rmtree(repo)
    for cmd in (["rev-parse", "HEAD"], ["describe", "--tags"],
                ["status", "--porcelain"]):
        p = subprocess.run(["git", "-C", co.path, *cmd],
                           capture_output=True, text=True)
        assert p.returncode == 0, f"git {cmd[0]} failed: {p.stderr}"
        assert "not a git repository" not in p.stderr
    assert subprocess.run(["git", "-C", co.path, "rev-parse", "HEAD"],
                          capture_output=True, text=True).stdout.strip() == head
    print("ok  checkout survives Docker mounting (self-contained .git)")


def test_checkout_populates_submodules():
    """A checkout must carry the source the project only *references*.

    Regression from the grok run: OSS-Fuzz's Dockerfile clones with --recursive,
    our checkout replaces that clone, and 'git clone --local' brings no submodule
    objects — so /src/grok/src/include/spdlog was an empty directory and CMake
    died there, an infra failure charged to the harness.

    Local paths travel git's 'file' transport, which git refuses for submodules
    by default (CVE-2022-39253); the config below is this test's substitute for
    the real thing being an https:// URL.
    """
    import subprocess
    import tempfile
    root = tempfile.mkdtemp()
    env = {**os.environ, "GIT_AUTHOR_NAME": "t", "GIT_AUTHOR_EMAIL": "t@t",
           "GIT_COMMITTER_NAME": "t", "GIT_COMMITTER_EMAIL": "t@t",
           "GIT_CONFIG_COUNT": "1", "GIT_CONFIG_KEY_0": "protocol.file.allow",
           "GIT_CONFIG_VALUE_0": "always"}

    def git(repo, *args, **kw):
        return subprocess.run(["git", "-C", repo, *args], check=True, env=env,
                              capture_output=True, text=True, **kw)

    dep = os.path.join(root, "dep")
    os.makedirs(dep)
    subprocess.run(["git", "init", "-q", dep], check=True)
    with open(os.path.join(dep, "borrowed.txt"), "w") as fh:
        fh.write("the source only referenced\n")
    git(dep, "add", "borrowed.txt")
    git(dep, "commit", "-q", "-m", "dep")

    repo = os.path.join(root, "src__proj")
    os.makedirs(repo)
    subprocess.run(["git", "init", "-q", repo], check=True)
    with open(os.path.join(repo, "own.txt"), "w") as fh:
        fh.write("the project's own source\n")
    git(repo, "add", "own.txt")
    git(repo, "submodule", "add", "-q", dep, "deps/dep")
    git(repo, "commit", "-q", "-m", "with submodule")
    head = git(repo, "rev-parse", "HEAD").stdout.strip()

    of = OssFuzz(oss_fuzz_dir=_fake_checkout({"native": _NATIVE_YAML}),
                 work_dir=root, dry_run=False)
    saved = {k: os.environ.get(k) for k in
             ("GIT_CONFIG_COUNT", "GIT_CONFIG_KEY_0", "GIT_CONFIG_VALUE_0")}
    os.environ.update({k: env[k] for k in saved})
    try:
        co = of.checkout(repo, head, "vuln")
    finally:
        for k, v in saved.items():
            os.environ.pop(k) if v is None else os.environ.update({k: v})

    borrowed = os.path.join(co.path, "deps", "dep", "borrowed.txt")
    assert os.path.isfile(borrowed), f"submodule not populated: {borrowed}"

    # ...and a project that borrows nothing must not pay for the check: no
    # .gitmodules, no submodule command, no network reach.
    plain = os.path.join(root, "src__plain")
    os.makedirs(plain)
    subprocess.run(["git", "init", "-q", plain], check=True)
    with open(os.path.join(plain, "own.txt"), "w") as fh:
        fh.write("no submodules here\n")
    git(plain, "add", "own.txt")
    git(plain, "commit", "-q", "-m", "one")
    plain_head = git(plain, "rev-parse", "HEAD").stdout.strip()

    ran = []
    real_run = of._run
    of._run = lambda cmd, **kw: (ran.append(cmd), real_run(cmd, **kw))[1]
    of.checkout(plain, plain_head, "vuln")
    assert not any("submodule" in c for cmd in ran for c in cmd), ran
    print("ok  checkout populates submodules, and skips them when there are none")


def test_a_previous_runs_binary_is_never_mistaken_for_this_build():
    """build/out/<project> is never cleared and every attempt overwrites the
    same harness file, so a leftover sits there under exactly the name the next
    build expects. A build that reports success while producing nothing (as
    librawspeed's does) would otherwise pass the 'is my binary there?' check on
    that leftover, and the campaign would fuzz the previous run's harness and
    judge this one by its behaviour."""
    import tempfile
    import time as _time
    from oss_fuzz.ossfuzz import _built_since

    root = tempfile.mkdtemp()
    out = os.path.join(root, "build", "out", "proj")
    os.makedirs(out)
    stale = os.path.join(out, "vp_harness_1")
    with open(stale, "w") as fh:
        fh.write("yesterday's binary\n")
    os.chmod(stale, 0o755)

    started = _time.time()
    assert not _built_since(stale, started), "a leftover must not count"
    assert not _built_since(os.path.join(out, "absent"), started)

    of = OssFuzz(oss_fuzz_dir=root, work_dir=root, dry_run=False)
    # The stale binary is invisible to a caller judging this build...
    assert of._out_targets("proj", since=started) == []
    # ...and still visible to one asking what the project can produce at all.
    assert of._out_targets("proj") == ["vp_harness_1"]

    # A build that really did write it is accepted.
    with open(stale, "w") as fh:
        fh.write("today's binary\n")
    assert _built_since(stale, started)
    assert of._out_targets("proj", since=started) == ["vp_harness_1"]
    print("ok  an earlier run's binary is not mistaken for this build's output")


def test_undeletable_checkout_is_escalated_not_ignored():
    """A build runs as root in the container and many projects build in-tree,
    so the next run finds files rmtree cannot delete. Silently leaving them is
    what turned grok into 'destination path already exists and is not an empty
    directory' -- an error naming nothing that caused it."""
    import shutil
    import subprocess
    import tempfile
    from oss_fuzz.ossfuzz import CLEANUP_IMAGE
    work = tempfile.mkdtemp()
    of = OssFuzz(oss_fuzz_dir=tempfile.mkdtemp(), work_dir=work, dry_run=False)
    path = os.path.join(work, "wt__src__proj__vuln")
    os.makedirs(os.path.join(path, "build"))

    # Stand in for the container: record the removal instead of running docker.
    calls = []

    def fake_run(cmd, **kw):
        calls.append(cmd)
        if cmd[:2] == ["docker", "run"]:
            shutil.rmtree(path)                     # what root would achieve
        return subprocess.CompletedProcess(cmd, 0, "", "")

    of._run = fake_run
    of._remove_as_root(path)
    assert calls and calls[0][:2] == ["docker", "run"], calls
    assert CLEANUP_IMAGE in calls[0], calls[0]
    assert f"/mnt/{os.path.basename(path)}" in calls[0], calls[0]
    assert not os.path.exists(path)

    # A removal that does not remove must raise, not return quietly: the next
    # step is a clone into that path and its error explains nothing.
    os.makedirs(path)
    of._run = lambda cmd, **kw: subprocess.CompletedProcess(cmd, 0, "", "")
    try:
        of._remove_as_root(path)
        assert False, "a failed removal must raise"
    except RuntimeError as exc:
        assert "could not clear" in str(exc), exc

    # And an rm -rf as root must refuse a path outside the work dir.
    try:
        of._remove_as_root("/etc/passwd")
        assert False, "must refuse a path outside work_dir"
    except RuntimeError as exc:
        assert "outside work_dir" in str(exc), exc
    print("ok  a checkout the container left undeletable is escalated")


def test_binary_output_does_not_kill_the_run():
    """A fuzzer echoes the bytes it is holding, so its stdout is not text.

    Regression from the ogre run: image_fuzz emitted a raw 0xff, strict UTF-8
    decoding raised inside communicate(), and because that is our plumbing
    rather than the harness's, it escaped the campaign's per-attempt handling
    and killed a project that had just built and started fuzzing.
    """
    import tempfile
    of = OssFuzz(oss_fuzz_dir=tempfile.mkdtemp(), work_dir=tempfile.mkdtemp(),
                 dry_run=False)
    proc = of._run(["python3", "-c",
                    "import sys; sys.stdout.buffer.write(b'ok\\xff\\xfetail')"])
    assert proc.returncode == 0, proc
    assert "ok" in proc.stdout and "tail" in proc.stdout, repr(proc.stdout)
    print("ok  binary fuzzer output is decoded leniently, not fatally")


def test_an_empty_build_is_not_reported_as_a_naming_problem():
    """librawspeed built nothing at all, $OUT held only llvm-symbolizer, and the
    diagnosis told the reader to go pick a different --base-harness. The tool
    OSS-Fuzz ships beside the targets is not a target, and 'nothing was built'
    is a different instruction to the reader than 'wrong name'."""
    import tempfile
    root = tempfile.mkdtemp()
    of = OssFuzz(oss_fuzz_dir=root, work_dir=root, dry_run=False)
    out = os.path.join(root, "build", "out", "proj")
    os.makedirs(out)

    def put(name):
        path = os.path.join(out, name)
        with open(path, "w") as fh:
            fh.write("#!/bin/sh\n")
        os.chmod(path, 0o755)

    put("llvm-symbolizer")
    assert of._out_targets("proj") == [], of._out_targets("proj")
    put("DngOpcodesFuzzer")
    put("libz.so")                       # not extensionless: not a target
    assert of._out_targets("proj") == ["DngOpcodesFuzzer"], of._out_targets("proj")
    print("ok  llvm-symbolizer is not counted as a fuzz target")


def test_worktrees_are_namespaced_per_repo():
    # Regression: a shared wt__vuln across projects meant the leftover from the
    # previous project was still registered to *its* repo, so this repo could
    # not remove it and 'worktree add' died with "already exists".
    of = _of(_fake_checkout({"native": _NATIVE_YAML}))
    a = of._worktree_path("/w/src__alpha", "vuln")
    b = of._worktree_path("/w/src__beta", "vuln")
    assert a != b
    assert a.endswith("wt__src__alpha__vuln")
    assert of._worktree_path("/w/src__alpha", "head") != a
    print("ok  worktree paths namespaced per repo")


def test_clear_worktree_removes_a_foreign_leftover():
    # A directory owned by another repo (or by nothing) must still be cleared,
    # because 'git -C <this repo> worktree remove' cannot do it.
    import subprocess
    import tempfile
    root = tempfile.mkdtemp()
    other = os.path.join(root, "src__other")
    mine = os.path.join(root, "src__mine")
    for r in (other, mine):
        os.makedirs(r)
        subprocess.run(["git", "init", "-q", r], check=True)
        subprocess.run(["git", "-C", r, "commit", "-q", "--allow-empty",
                        "-m", "init"], check=True,
                       env={**os.environ, "GIT_AUTHOR_NAME": "t",
                            "GIT_AUTHOR_EMAIL": "t@t", "GIT_COMMITTER_NAME": "t",
                            "GIT_COMMITTER_EMAIL": "t@t"})
    of = OssFuzz(oss_fuzz_dir=_fake_checkout({"native": _NATIVE_YAML}),
                 work_dir=root, dry_run=False)
    stale = of._worktree_path(mine, "vuln")
    # Register that exact path as a worktree of the *other* repo.
    subprocess.run(["git", "-C", other, "worktree", "add", "--detach", stale],
                   check=True, capture_output=True)
    assert os.path.isdir(stale)

    of._clear_worktree(mine, stale)
    assert not os.path.exists(stale), "foreign leftover was not cleared"
    # And the path is now usable by our repo.
    wt = of.checkout(mine, "HEAD", "vuln")
    assert os.path.isdir(wt.path)
    print("ok  clears a foreign worktree leftover")


def test_find_candidates_applies_both_filters():
    from oss_fuzz.targets import find_candidates
    root = _fake_checkout({"native": _NATIVE_YAML, "pyproj": _PY_YAML,
                           "quiet": "language: c\nmain_repo: 'https://x/y.git'\n"})
    queried = []

    def fetch(project):
        queried.append(project)
        # Only 'native' has a disclosed bug; 'quiet' has none.
        return FIX["vulns"] if project == "native" else []

    cands = find_candidates(_of(root), sanitizer="address", fetch=fetch)
    assert [c.project for c in cands] == ["native"]
    assert cands[0].target.osv_id == "OSV-2024-555"
    assert cands[0].main_repo == "https://github.com/example/demo"
    # The python project must be rejected locally — never costing an OSV query.
    assert "pyproj" not in queried
    assert sorted(queried) == ["native", "quiet"]
    print("ok  candidate discovery filters")


def test_find_candidates_limit_stops_the_sweep():
    from oss_fuzz.targets import find_candidates
    root = _fake_checkout({
        f"p{i}": f"language: c\nmain_repo: 'https://x/p{i}.git'\n"
        for i in range(5)})
    queried = []

    def fetch(project):
        queried.append(project)
        return FIX["vulns"]

    cands = find_candidates(_of(root), sanitizer="address", fetch=fetch, limit=2)
    assert len(cands) == 2
    assert len(queried) == 2, queried      # stopped early, did not probe all 5
    print("ok  candidate sweep limit")


HARNESS_BODY = ("#include <stddef.h>\nint LLVMFuzzerTestOneInput("
                "const uint8_t *d, size_t s) { return 0; }\n")

# libxml2's real build.sh, in full: it delegates to a script inside the
# upstream repo, so there is no compile line anywhere to crib.
LIBXML2_BUILD_SH = "fuzz/oss-fuzz-build.sh\n"


def _tree(root, files):
    for rel, text in files.items():
        path = os.path.join(root, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as fh:
            fh.write(text)


def _mk_oss_fuzz(tmp, project, build_sh):
    """A minimal fake oss-fuzz checkout with one project's build.sh."""
    pdir = os.path.join(tmp, "oss-fuzz", "projects", project)
    os.makedirs(pdir, exist_ok=True)
    if build_sh is not None:
        with open(os.path.join(pdir, "build.sh"), "w") as fh:
            fh.write(build_sh)
    return os.path.join(tmp, "oss-fuzz")


def test_find_base_harness_prefers_the_named_fuzz_target():
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        _tree(tmp, {
            "src/parser.c": "int parse(void){return 0;}\n",          # not a harness
            "fuzz/xml.c": HARNESS_BODY,                              # the target
            "fuzz/html.c": HARNESS_BODY,                             # another target
            "third_party/zlib/fuzz/zlib_fuzzer.c": HARNESS_BODY,     # vendored
        })
        # Named target wins outright.
        assert find_base_harness(tmp, "xml") == os.path.join("fuzz", "xml.c")
        assert find_base_harness(tmp, "html") == os.path.join("fuzz", "html.c")
        # With no name, a fuzz-ish directory beats a vendored one; never the
        # non-harness source.
        pick = find_base_harness(tmp, None)
        assert pick in (os.path.join("fuzz", "html.c"),
                        os.path.join("fuzz", "xml.c")), pick
        # Explicit override is honoured verbatim, vendored or not.
        assert find_base_harness(
            tmp, "xml", "third_party/zlib/fuzz/zlib_fuzzer.c") == \
            os.path.join("third_party", "zlib", "fuzz", "zlib_fuzzer.c")
        # A bad override does not silently fall back to auto-detection.
        assert find_base_harness(tmp, "xml", "nope/missing.c") is None
        print("ok  base harness discovery ranks target name > vendored")


def test_find_base_harness_skips_standalone_drivers():
    """The 20260811 sweep picked a driver over the real harness, twice.

    rawspeed's fuzz/libFuzzer_dummy_main.cpp and wireshark's
    fuzz/StandaloneFuzzTargetMain.c declare LLVMFuzzerTestOneInput and call it
    from their own main(), so a substring test accepts them — and both win the
    tie-breaks (shallower path for rawspeed, 'S' < 'f' for wireshark). The build
    then produces no target named after the file, wasting the whole project.
    """
    import tempfile
    driver = ('extern "C" int LLVMFuzzerTestOneInput(const uint8_t*, size_t);\n'
              "int main(int argc, char **argv) {\n"
              "  LLVMFuzzerTestOneInput((const uint8_t *)argv[1], 0);\n"
              "  return 0;\n}\n")
    with tempfile.TemporaryDirectory() as tmp:
        _tree(tmp, {
            "fuzz/StandaloneFuzzTargetMain.c": driver,   # sorts before fuzzshark
            "fuzz/libFuzzer_dummy_main.cpp": driver,     # shallower than below
            "fuzz/librawspeed/parsers/main.cpp": HARNESS_BODY,
        })
        assert find_base_harness(tmp, None) == os.path.join(
            "fuzz", "librawspeed", "parsers", "main.cpp")
        # A definition split across lines is still a definition.
        _tree(tmp, {"fuzz/wrapped.c": (
            "int\nLLVMFuzzerTestOneInput(const uint8_t *d,\n"
            "                          size_t s)\n{\n  return 0;\n}\n")})
        assert find_base_harness(tmp, "wrapped") == os.path.join(
            "fuzz", "wrapped.c")
        print("ok  base harness discovery skips standalone drivers")


def test_find_base_harness_avoids_a_stem_that_cannot_name_a_target():
    """rawspeed's fuzz/rawspeed/main.cpp is a real harness, but it builds
    'RawSpeedFuzzer' — no rule predicts that, and no target is called 'main'.
    A sibling whose stem the build can decorate ('DngOpcodes' ->
    'DngOpcodesFuzzer') is usable, so it wins despite the deeper path."""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        _tree(tmp, {
            "fuzz/rawspeed/main.cpp": HARNESS_BODY,
            "fuzz/librawspeed/common/DngOpcodes.cpp": HARNESS_BODY,
        })
        assert find_base_harness(tmp, None) == os.path.join(
            "fuzz", "librawspeed", "common", "DngOpcodes.cpp")
        # Unless the OSV record named it outright, which beats every tie-break.
        assert find_base_harness(tmp, "main") == os.path.join(
            "fuzz", "rawspeed", "main.cpp")
        print("ok  base harness discovery avoids the 'main' stem")


def test_find_base_harness_returns_none_without_a_harness():
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        _tree(tmp, {"src/lib.c": "int f(void){return 1;}\n"})
        assert find_base_harness(tmp, "anything") is None
        assert find_base_harness(os.path.join(tmp, "nonexistent"), None) is None
        print("ok  base harness discovery returns None when there is none")


def test_plan_harness_auto_cribs_when_possible_overwrites_when_not():
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        co_dir = os.path.join(tmp, "checkout")
        _tree(co_dir, {"fuzz/xml.c": HARNESS_BODY})
        co = Checkout(label="vuln", path=co_dir, commit="abc")

        # A project WITH a cribbable line keeps the proven crib path.
        of = OssFuzz(oss_fuzz_dir=_mk_oss_fuzz(tmp, "demo", BUILD_SH),
                     work_dir=os.path.join(tmp, "wd"))
        p = of.plan_harness("demo", co, "xml", ".cc")
        assert p.mode == "crib" and p.cribbable, p
        assert p.ext == ".cc", p          # untouched by crib mode

        # libxml2-shaped: no compile line anywhere -> overwrite the harness.
        of2 = OssFuzz(oss_fuzz_dir=_mk_oss_fuzz(tmp, "libxml2",
                                                LIBXML2_BUILD_SH),
                      work_dir=os.path.join(tmp, "wd2"))
        p2 = of2.plan_harness("libxml2", co, "xml", ".cc")
        assert p2.mode == "overwrite", p2
        assert not p2.cribbable
        assert p2.rel_path == os.path.join("fuzz", "xml.c")
        # The extension follows the file we replace, NOT the project language:
        # C++ written into a .c file would not compile.
        assert p2.ext == ".c", p2
        assert p2.target_name == "xml"
        print("ok  plan_harness:", p2.describe())


def test_plan_harness_respects_forced_modes():
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        co_dir = os.path.join(tmp, "checkout")
        _tree(co_dir, {"fuzz/xml.c": HARNESS_BODY})
        co = Checkout(label="vuln", path=co_dir, commit="abc")
        of = OssFuzz(oss_fuzz_dir=_mk_oss_fuzz(tmp, "demo", BUILD_SH),
                     work_dir=os.path.join(tmp, "wd"))

        # Forced overwrite ignores an available crib line.
        assert of.plan_harness("demo", co, "xml", ".cc",
                              mode="overwrite").mode == "overwrite"
        # Forced crib ignores an available harness to overwrite.
        assert of.plan_harness("demo", co, "xml", ".cc",
                              mode="crib").mode == "crib"
        # Forced overwrite with nothing to overwrite is a caller-visible None,
        # not a silent downgrade to a compile line that cannot work.
        empty = Checkout(label="vuln", path=os.path.join(tmp, "empty"),
                         commit="abc")
        os.makedirs(empty.path, exist_ok=True)
        assert of.plan_harness("demo", empty, "xml", ".cc",
                              mode="overwrite") is None
        # auto with neither option falls back to crib (matches the old pipeline).
        of3 = OssFuzz(oss_fuzz_dir=_mk_oss_fuzz(tmp, "bare", LIBXML2_BUILD_SH),
                      work_dir=os.path.join(tmp, "wd3"))
        assert of3.plan_harness("bare", empty, "xml", ".cc").mode == "crib"
        print("ok  plan_harness forced modes and fallbacks")


def test_overwrite_build_restores_the_original_harness_and_names_the_binary():
    """The trick's two contracts: build.sh is never touched, and the source file
    is byte-identical afterwards."""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        co_dir = os.path.join(tmp, "checkout")
        original = HARNESS_BODY + "/* project's own harness */\n"
        _tree(co_dir, {"fuzz/xml.c": original})
        co = Checkout(label="vuln", path=co_dir, commit="abc")
        of_dir = _mk_oss_fuzz(tmp, "libxml2", LIBXML2_BUILD_SH)
        of = OssFuzz(oss_fuzz_dir=of_dir, work_dir=os.path.join(tmp, "wd"))
        placement = of.plan_harness("libxml2", co, "xml", ".cc")
        assert placement.mode == "overwrite"

        seen = {}

        def fake_build(project, checkout, sanitizer, log_tag):
            # Capture what the build would have compiled.
            with open(os.path.join(checkout.path, "fuzz", "xml.c")) as fh:
                seen["source"] = fh.read()
            out = os.path.join(of_dir, "build", "out", project)
            os.makedirs(out, exist_ok=True)
            binpath = os.path.join(out, "xml")
            with open(binpath, "w") as fh:
                fh.write("#!/bin/sh\n")
            os.chmod(binpath, 0o755)
            return True

        of._run_build = fake_build
        out_bin = of.build_harness("libxml2", co, "vp_harness_1",
                                   "GENERATED HARNESS\n", ".cc", "address",
                                   placement=placement)

        # Our source reached the build...
        assert seen["source"] == "GENERATED HARNESS\n", seen
        # ...the original is restored byte-for-byte...
        with open(os.path.join(co_dir, "fuzz", "xml.c")) as fh:
            assert fh.read() == original
        # ...build.sh was never edited...
        with open(os.path.join(of_dir, "projects", "libxml2", "build.sh")) as fh:
            assert fh.read() == LIBXML2_BUILD_SH
        # ...and the binary is the REPLACED target's name, not ours.
        assert out_bin.endswith(os.path.join("out", "libxml2", "xml")), out_bin
        assert placement.runtime_name("vp_harness_1") == "xml"
        print("ok  overwrite build restores the tree and names the binary 'xml'")


def _overwrite_of(tmp, built):
    """An OssFuzz whose build produces exactly ``built`` as targets."""
    co_dir = os.path.join(tmp, "checkout")
    _tree(co_dir, {"fuzz/xml.c": HARNESS_BODY})
    co = Checkout(label="vuln", path=co_dir, commit="abc")
    of_dir = _mk_oss_fuzz(tmp, "libxml2", LIBXML2_BUILD_SH)
    of = OssFuzz(oss_fuzz_dir=of_dir, work_dir=os.path.join(tmp, "wd"))

    def fake_build(project, checkout, sanitizer, log_tag):
        out = os.path.join(of_dir, "build", "out", project)
        os.makedirs(out, exist_ok=True)
        for name in built:
            path = os.path.join(out, name)
            with open(path, "w") as fh:
                fh.write("#!/bin/sh\n")
            os.chmod(path, 0o755)
        return True

    of._run_build = fake_build
    return of, co, of.plan_harness("libxml2", co, "xml", ".cc")


def test_overwrite_build_adopts_a_decorated_target_name():
    """rawspeed's CMake compiles DngOpcodes.cpp into 'DngOpcodesFuzzer'. The
    stem cannot predict that, but a single built target embedding the stem is
    the binary built from the file we overwrote, so adopt it rather than abort
    the project (the 20260811 run died here)."""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        of, co, placement = _overwrite_of(tmp, ["xmlFuzzer", "htmlFuzzer"])
        out_bin = of.build_harness("libxml2", co, "vp_harness_1", "SRC\n",
                                   ".cc", "address", placement=placement)
        assert out_bin and out_bin.endswith("xmlFuzzer"), out_bin
        # run_fuzzer must ask for the same name, now and on later attempts.
        assert placement.runtime_name("vp_harness_1") == "xmlFuzzer"
        assert of.last_build_infra_error is None
        print("ok  overwrite adopts a decorated target name")


def test_overwrite_build_aborts_when_the_expected_target_is_absent():
    """A build that succeeds but produces no target relatable to the inferred
    name is a configuration problem, not a harness problem: abort, don't burn
    attempts. Two targets embedding the stem are equally unresolvable."""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        of, co, placement = _overwrite_of(tmp, ["totally_unrelated"])
        assert of.build_harness("libxml2", co, "vp_harness_1", "SRC\n", ".cc",
                                "address", placement=placement) is None
        infra = of.last_build_infra_error
        assert infra and "no target named 'xml'" in infra, infra
        assert "totally_unrelated" in infra, infra
        # Classified as infra, so nothing is fed back to the model as a compile
        # error it could never fix.
        assert of.last_build_stderr == ""

    with tempfile.TemporaryDirectory() as tmp:
        of, co, placement = _overwrite_of(tmp, ["xml_a", "xml_b"])
        assert of.build_harness("libxml2", co, "vp_harness_1", "SRC\n", ".cc",
                                "address", placement=placement) is None
        assert of.last_build_infra_error, of.last_build_infra_error
        print("ok  overwrite aborts on a target-name mismatch")


def test_campaign_runs_the_replaced_targets_name_under_overwrite():
    """The plumbing bug this port has to avoid: building as vp_harness_N but
    having to RUN the name the project's build system produced."""
    from oss_fuzz.campaign import HarnessCampaign

    class _Gen:
        def generate(self, messages):
            return ("```c\nint LLVMFuzzerTestOneInput(const unsigned char *d, "
                    "unsigned long s){return 0;}\n```")

    ran = []

    class _Of:
        last_build_stderr = ""
        last_build_infra_error = None

        def build_harness(self, project, checkout, name, source, ext,
                          sanitizer, placement=None):
            return "/out/xml"

        def run_fuzzer(self, project, harness_name, seconds, sanitizer,
                       bug_class=None, log_tag=None):
            ran.append(harness_name)
            from oss_fuzz.ossfuzz import RunOutcome
            return RunOutcome(triggered=True, timed_out=False, returncode=1,
                              crash_reason="ASan", signature="sig",
                              found_by="sanitizer")

    placement = HarnessPlacement(mode="overwrite", ext=".c",
                                 rel_path="fuzz/xml.c", target_name="xml")
    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_Of(), project="libxml2",
                           vuln_checkout=None, sanitizer="address", ext=".c",
                           placement=placement, target_successes=1,
                           max_attempts=2)
    res = camp.run(lambda covered, sigs: [{"role": "user", "content": "x"}])
    assert res.achieved == 1, res
    assert ran == ["xml"], ran           # NOT vp_harness_1
    print("ok  campaign gates on the replaced target's name")


def test_prompt_language_follows_the_overwritten_file_extension():
    """Overwrite cannot change the file's extension, so a C++ project whose
    harness is a .c file must be prompted for C."""
    ctx = _ctx_for_prompt()
    b = LibFuzzerPromptBuilder(language="c++")
    forced_c = b.build(context=ctx, covered_functions=[], found_signatures=[],
                       harness_name="xml", harness_ext=".c")[1]["content"]
    assert "```c\n" in forced_c and "```cpp" not in forced_c, forced_c[-800:]
    assert "xml.c" in forced_c
    assert "valid C)" in forced_c or "valid C." in forced_c, forced_c[-400:]
    assert 'extern "C"' not in forced_c
    # Without a forced extension the project's language still decides.
    default = b.build(context=ctx, covered_functions=[], found_signatures=[],
                      harness_name="vp_harness")[1]["content"]
    assert "```cpp" in default, default[-800:]
    print("ok  prompt language follows the overwritten extension")


def test_bugclass_splits_crashing_from_semantic():
    """The classification the whole split rests on, over ClusterFuzz's real
    crash-type vocabulary."""
    from oss_fuzz.bugclass import (classify, CRASHING, SEMANTIC, UNKNOWN,
                                   ORACLE_HARNESS, ORACLE_PROJECT_ASSERT,
                                   ORACLE_SANITIZER)
    # Memory safety and UB: the runtime is the oracle, as before.
    for ct in ("Heap-buffer-overflow READ 4", "Heap-use-after-free WRITE 1",
               "Undefined-shift", "Use-of-uninitialized-value",
               "Null-dereference READ", "Index-out-of-bounds",
               "Direct-leak", "Stack-overflow"):
        bc = classify(ct)
        assert bc.kind == CRASHING and bc.oracle == ORACLE_SANITIZER, (ct, bc)
        assert not bc.needs_harness_oracle and not bc.resource, (ct, bc)

    # The project checks it itself: a logic defect, but the library aborts by
    # itself, so it is CRASHING — the trigger gate needs no help and nobody has
    # to write an oracle. Java classifies the same shape (an escaping
    # invariant-check throwable) as crashing too.
    for ct in ("ASSERT: idx < len", "CHECK failure: ptr != nullptr",
               "Fatal error: bad state", "Unreachable code",
               "Security check failure"):
        bc = classify(ct)
        assert bc.kind == CRASHING, (ct, bc)
        assert bc.oracle == ORACLE_PROJECT_ASSERT, (ct, bc)
        assert not bc.needs_harness_oracle and not bc.is_semantic, (ct, bc)

    # Nothing observes it: the harness must bring the oracle.
    for ct in ("Incorrect-result", "Wrong result in decode"):
        bc = classify(ct)
        assert bc.kind == SEMANTIC and bc.oracle == ORACLE_HARNESS, (ct, bc)
        assert bc.needs_harness_oracle, (ct, bc)

    # Resource bugs are runtime-detected, but only under the limits they were
    # found with — libFuzzer's own defaults are far looser.
    for ct in ("Timeout", "Out-of-memory (exceeds 2560 MB)"):
        bc = classify(ct)
        assert bc.kind == CRASHING and bc.resource, (ct, bc)
        assert any(f.startswith("-timeout=") for f in bc.libfuzzer_flags()), bc
    assert classify("Heap-buffer-overflow").libfuzzer_flags() == []

    # An unreadable record keeps the pre-split behaviour, but says so. The
    # sanitizer prior is deliberate (this corpus is overwhelmingly memory
    # bugs) and deliberately the opposite of the Java front-end's — so it is
    # hedged, not bet on: `uncertain` is what makes the prompt ask for an
    # optional wrong-value check anyway.
    bc = classify(None)
    assert bc.kind == UNKNOWN and bc.oracle == ORACLE_SANITIZER, bc
    assert not bc.needs_harness_oracle and not bc.is_semantic, bc
    assert bc.uncertain, bc
    assert not classify("Heap-buffer-overflow READ 4").uncertain
    print("ok  bug class: crashing vs semantic vs unknown")


def test_bugclass_kind_cannot_contradict_its_oracle():
    """SEMANTIC means exactly "the harness must supply the verdict". The
    original bug filed project-assert as SEMANTIC because a violated invariant
    is a logic error — true, but it made --skip-semantic discard bugs the
    trigger gate handles unmodified. The invariant blocks that class of edit."""
    from oss_fuzz.bugclass import (BugClass, CRASHING, SEMANTIC,
                                   ORACLE_HARNESS, ORACLE_PROJECT_ASSERT,
                                   ORACLE_SANITIZER)
    for kind, oracle in ((SEMANTIC, ORACLE_PROJECT_ASSERT),
                         (SEMANTIC, ORACLE_SANITIZER),
                         (CRASHING, ORACLE_HARNESS)):
        try:
            BugClass(kind=kind, oracle=oracle, reason="x")
        except ValueError:
            pass
        else:
            raise AssertionError(f"accepted kind={kind} with oracle={oracle}")
    # And the three consistent combinations still build.
    for kind, oracle in ((CRASHING, ORACLE_SANITIZER),
                         (CRASHING, ORACLE_PROJECT_ASSERT),
                         (SEMANTIC, ORACLE_HARNESS)):
        BugClass(kind=kind, oracle=oracle, reason="x")
    print("ok  bug class kind is a coarsening of its oracle, not a 2nd opinion")


def test_skip_semantic_keeps_runtime_detected_bugs():
    """--skip-semantic means "the crash gate cannot work here", not "the defect
    is logic-shaped". A project assert aborts the process, so those runs are in
    scope; only harness-oracle bugs are out."""
    from oss_fuzz.bugclass import classify
    assert not classify("ASSERT: idx < len").needs_harness_oracle
    assert not classify("Timeout").needs_harness_oracle
    assert not classify(None).needs_harness_oracle
    assert classify("Incorrect-result").needs_harness_oracle
    print("ok  --skip-semantic only drops bugs nothing at run time reports")


def test_bugclass_reaches_the_target_from_the_osv_record():
    """CveTarget derives its class from the crash type it already parses, so
    the two cannot drift apart."""
    rec = {
        "id": "OSV-2025-1", "published": "2025-01-01",
        "affected": [{"ranges": [{"type": "GIT",
                                  "repo": "https://example/x",
                                  "events": [{"fixed": "abc"}]}]}],
        "details": "OSS-Fuzz report: x\n```\nCrash type: Incorrect-result\n"
                   "Crash state:\nxmlDecode\n```",
    }
    t = CveTarget.from_osv("demo", rec)
    assert t.crash_type == "Incorrect-result"
    assert t.bug_class.is_semantic and t.bug_class.needs_harness_oracle
    # And a plain memory bug stays on the old path.
    rec["details"] = rec["details"].replace("Incorrect-result",
                                            "Heap-buffer-overflow READ 4")
    assert CveTarget.from_osv("demo", rec).bug_class.is_crashing
    print("ok  bug class rides along on the OSV target")


def test_finding_oracle_ranks_evidence():
    """What NOTICED the failure, which is not the same question as whether one
    happened. Precedence matters: harness code can print anything, but it
    cannot fake an ASan report."""
    from oss_fuzz.ossfuzz import finding_oracle
    asan = "==1==ERROR: AddressSanitizer: heap-buffer-overflow on address 0x1"
    assert finding_oracle(asan) == "sanitizer"
    # A tagged alarm plus the deadly signal its abort() produced.
    oracle_out = ("[oracle:round-trip] decode(encode(x)) != x\n"
                  "==1==ERROR: libFuzzer: deadly signal")
    assert finding_oracle(oracle_out) == "harness"
    # A sanitizer report outranks a tag left over from an earlier input.
    assert finding_oracle(oracle_out + "\n" + asan) == "sanitizer"
    assert finding_oracle(
        "x.c:12: f: Assertion `n > 0' failed.\n"
        "==1==ERROR: libFuzzer: deadly signal") == "project-assert"
    # Runtime-detected but unexplained is still the runtime's finding.
    assert finding_oracle("==1==ERROR: libFuzzer: timeout") == "sanitizer"
    assert finding_oracle("Done 1000 runs in 3 second(s)") is None
    print("ok  finding oracle ranks evidence by strength")


def test_signatures_distinguish_semantic_findings():
    """Steering feeds on signatures. Every oracle and every assert reaches
    libFuzzer as the same 'deadly signal', so without their own signature
    forms the model would be told it had covered ground it had not."""
    deadly = "\n==1==ERROR: libFuzzer: deadly signal\n#1 0x4 in parse_attr\n"
    a = crash_signature("[oracle:round-trip] mismatch" + deadly)
    b = crash_signature("[oracle:idempotence] mismatch" + deadly)
    assert a != b, (a, b)
    assert a.startswith("oracle:round-trip") and "parse_attr" in a, a
    s = crash_signature("t.c:9: f: Assertion `i < n' failed." + deadly)
    assert s == "assert:i < n", s
    # A real sanitizer report is unchanged by any of this.
    assert crash_signature(
        "==1==ERROR: AddressSanitizer: heap-buffer-overflow\n"
        "#0 0x1 in xmlParse") == "AddressSanitizer:heap-buffer-overflow@xmlParse"
    print("ok  semantic findings get distinguishable signatures")


def test_trigger_gate_sees_alarms_no_sanitizer_reports():
    """An oracle that exits without a signal, or an assert on a build where
    libFuzzer prints nothing extra, still has to count as a trigger."""
    assert _looks_like_crash(1, "[oracle:len] 3 != 4\n") is not None
    assert _looks_like_crash(1, "a.c:1: f: Assertion `x' failed.\n") is not None
    assert _looks_like_crash(0, "Done 500 runs\n") is None
    print("ok  trigger gate sees non-sanitizer alarms")


def test_semantic_prompt_requires_a_tagged_oracle():
    from oss_fuzz.bugclass import classify
    ctx = _ctx_for_prompt()
    b = LibFuzzerPromptBuilder(language="c")

    def build(bug_class):
        return b.build(context=ctx, covered_functions=[], found_signatures=[],
                       harness_name="h", harness_ext=".c",
                       crash_type=bug_class.crash_type if bug_class else None,
                       bug_class=bug_class)[1]["content"]

    wrong_value = build(classify("Incorrect-result"))
    assert "REQUIRED ORACLE" in wrong_value
    assert "[oracle:" in wrong_value
    assert "THIS BUG DOES NOT CRASH" in wrong_value
    assert "METAMORPHIC CHECK (optional" not in wrong_value

    # A project assert is CRASHING, so no oracle is demanded — but the prompt
    # still has to say a sanitizer is not what reports it, or the model goes
    # hunting for memory corruption. That line used to be keyed on the kind,
    # which would have lost it when the kind moved to crashing.
    invariant = build(classify("ASSERT: idx < len"))
    assert "the library checks this itself" in invariant
    assert "ASSERT: idx < len" in invariant
    assert "REQUIRED ORACLE" not in invariant
    assert "THIS BUG DOES NOT CRASH" not in invariant
    assert "detected by: project-assert" in invariant
    assert "own check reports it, not a sanitizer" in invariant

    # Crashing bugs — and callers that pass nothing — keep the old wording.
    for crashing in (build(classify("Heap-buffer-overflow READ 4")),
                     build(None)):
        assert "METAMORPHIC CHECK (optional" in crashing
        assert "REQUIRED ORACLE" not in crashing
        assert "THIS BUG DOES NOT CRASH" not in crashing
        assert "detected by:" not in crashing
        assert "STRONGLY RECOMMENDED HERE" not in crashing

    # An unknown class is a prior, not a reading. It takes the crashing
    # template, but asks for the optional relation as insurance in case the
    # prior is wrong — otherwise a misread record burns the whole budget.
    unknown = build(classify(None))
    assert "REQUIRED ORACLE" not in unknown
    assert "METAMORPHIC CHECK (optional" in unknown
    assert "STRONGLY RECOMMENDED HERE" in unknown
    print("ok  prompt oracle contract follows the bug class")


def test_campaign_rejects_a_harness_that_cannot_fail():
    """A semantic harness with no oracle is rejected from the source, before
    the Docker build — the alternative costs a build and a verify run to reach
    a verdict indistinguishable from an honest miss."""
    from oss_fuzz.campaign import HarnessCampaign, oracle_tag_missing
    from oss_fuzz.bugclass import classify
    from oss_fuzz.ossfuzz import RunOutcome

    assert oracle_tag_missing("int LLVMFuzzerTestOneInput(){return 0;}")
    assert oracle_tag_missing('fprintf(stderr, "[oracle:x] bad\\n");') \
        is not None                      # tagged but never stops
    assert oracle_tag_missing(
        'fprintf(stderr, "[oracle:x] bad\\n"); abort();') is None

    replies = [
        # 1: reaches the code, cannot fail — must not be built.
        "```c\nint LLVMFuzzerTestOneInput(const uint8_t *d, size_t s)"
        "{ parse(d, s); return 0; }\n```",
        # 2: carries a tagged, aborting oracle.
        '```c\nint LLVMFuzzerTestOneInput(const uint8_t *d, size_t s)'
        '{ if (parse(d,s) != reparse(d,s)) { '
        'fprintf(stderr, "[oracle:round-trip] mismatch\\n"); abort(); } '
        'return 0; }\n```',
    ]
    built, asked = [], []

    class _Gen:
        def generate(self, messages):
            asked.append(messages)
            return replies[min(len(asked) - 1, len(replies) - 1)]

    class _Of:
        last_build_stderr = ""
        last_build_infra_error = None

        def build_harness(self, project, checkout, name, source, ext,
                          sanitizer, placement=None):
            built.append(name)
            return "/out/" + name

        def run_fuzzer(self, project, harness_name, seconds, sanitizer,
                       bug_class=None, log_tag=None):
            return RunOutcome(triggered=True, timed_out=False, returncode=1,
                              crash_reason="oracle", signature="oracle:rt",
                              found_by="harness")

    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_Of(), project="demo",
                           vuln_checkout=None, sanitizer="address", ext=".c",
                           target_successes=1, max_attempts=3,
                           bug_class=classify("Incorrect-result"))
    res = camp.run(lambda covered, sigs: [{"role": "user", "content": "x"}])
    assert res.achieved == 1, res
    assert built == ["vp_harness_2"], built    # attempt 1 never reached a build
    assert res.successful[0].found_by == "harness"
    print("ok  campaign rejects a semantic harness that cannot fail")


def test_crashing_campaign_is_unchanged_by_the_split():
    """The oracle gate must not touch crashing runs: their harnesses are not
    supposed to carry a check at all."""
    from oss_fuzz.campaign import HarnessCampaign
    from oss_fuzz.bugclass import classify
    from oss_fuzz.ossfuzz import RunOutcome
    built = []

    class _Gen:
        def generate(self, messages):
            return ("```c\nint LLVMFuzzerTestOneInput(const uint8_t *d, "
                    "size_t s){ parse(d, s); return 0; }\n```")

    class _Of:
        last_build_stderr = ""
        last_build_infra_error = None

        def build_harness(self, project, checkout, name, source, ext,
                          sanitizer, placement=None):
            built.append(name)
            return "/out/" + name

        def run_fuzzer(self, project, harness_name, seconds, sanitizer,
                       bug_class=None, log_tag=None):
            return RunOutcome(triggered=True, timed_out=False, returncode=1,
                              crash_reason="ASan", signature="sig",
                              found_by="sanitizer")

    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_Of(), project="demo",
                           vuln_checkout=None, sanitizer="address", ext=".c",
                           target_successes=1, max_attempts=2,
                           bug_class=classify("Heap-buffer-overflow READ 4"))
    assert camp.run(lambda c, s: [{"role": "user", "content": "x"}]).achieved == 1
    assert built == ["vp_harness_1"], built
    print("ok  crashing campaign unaffected by the oracle gate")


def test_campaign_refuses_a_harness_that_refinds_a_known_crash():
    """`target_successes` is a count of EVIDENCE, so five harnesses that all
    re-find one crash must not satisfy it. Each would also cost a HEAD build
    and be counted again in the sibling total."""
    from oss_fuzz.campaign import HarnessCampaign
    from oss_fuzz.ossfuzz import RunOutcome
    sigs = ["ASan:heap-buffer-overflow@parse",   # accepted: new
            "ASan:heap-buffer-overflow@parse",   # refused: already have it
            None,                                # accepted: unreadable, so we
            None,                                #   cannot show it is a repeat
            "ASan:heap-use-after-free@free_it"]  # accepted: new
    built, prompts_seen = [], []

    class _Gen:
        def generate(self, messages):
            prompts_seen.append(messages[-1]["content"])
            return ("```c\nint LLVMFuzzerTestOneInput(const uint8_t *d, "
                    "size_t s){ parse(d, s); return 0; }\n```")

    class _Of:
        last_build_stderr = ""
        last_build_infra_error = None

        def build_harness(self, project, checkout, name, source, ext,
                          sanitizer, placement=None):
            built.append(name)
            return "/out/" + name

        def run_fuzzer(self, project, harness_name, seconds, sanitizer,
                       bug_class=None, log_tag=None):
            return RunOutcome(triggered=True, timed_out=False, returncode=1,
                              crash_reason="ASan",
                              signature=sigs[len(built) - 1],
                              found_by="sanitizer")

    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_Of(), project="demo",
                           vuln_checkout=None, sanitizer="address", ext=".c",
                           target_successes=4, max_attempts=5)
    res = camp.run(lambda covered, sigs_: [{"role": "user", "content": "x"}])
    assert res.attempts == 5, res.attempts
    assert res.achieved == 4, [g.signature for g in res.successful]
    # The duplicate is gone; the two unreadable ones survive (fail open).
    assert [g.signature for g in res.successful] == [
        "ASan:heap-buffer-overflow@parse", None, None,
        "ASan:heap-use-after-free@free_it"]
    # ...and the rejection told the model what to do instead.
    assert "already found" in prompts_seen[2], prompts_seen[2]
    print("ok  campaign refuses a harness that re-finds a known crash")


def test_distinct_finding_pressure_appears_once_the_set_has_a_crash():
    """The gate is invisible to the model unless the prompt says so — but as the
    FIRST harness's instruction it would read as 'avoid the bug you were sent
    to reach'."""
    ctx = _ctx_for_prompt()
    ctx.root_cause_reachable = ["demo_parse"]
    b = LibFuzzerPromptBuilder(language="c")

    def build(signatures):
        return b.build(context=ctx, covered_functions=[],
                       found_signatures=signatures, harness_name="h",
                       harness_ext=".c")[1]["content"]

    assert "DISTINCT FINDING REQUIRED" not in build([])
    later = build(["ASan:heap-buffer-overflow@parse"])
    assert "DISTINCT FINDING REQUIRED" in later
    assert "[oracle:<id>]" in later     # the second way to win is spelled out
    print("ok  distinct-finding pressure appears only once the set has a crash")


# --- project sampling -------------------------------------------------------
# Built against a synthetic projects/ tree rather than the real checkout: that
# tree is gitignored and moves upstream, so asserting on it would make these
# tests machine-dependent and the selection itself is only reproducible for a
# *fixed* tree anyway.
_FAKE_PROJECTS = {
    # name:            (language, main_repo, extra project.yaml lines, workdir)
    "alpha":           ("c++", "https://github.com/x/alpha", "", "/src/alpha"),
    "bravo":           ("c++", "https://github.com/x/bravo", "", "/src/bravo"),
    "charlie":         ("c++", "https://github.com/x/charlie", "", "/src/charlie"),
    "delta":           ("c++", "https://github.com/x/delta", "", "/src/delta"),
    "echo":            ("c++", "https://github.com/x/echo", "", "/src/echo"),
    "foxtrot":         ("c++", "https://github.com/x/foxtrot", "", "/src/foxtrot"),
    # excluded, one per filter:
    "a-c-project":     ("c", "https://github.com/x/c-proj", "", "/src/c-proj"),
    "a-go-project":    ("go", "https://github.com/x/go-proj", "", "/src/go-proj"),
    "an-hg-project":   ("c++", "https://hg.mozilla.org/thing", "", "/src/hg"),
    "an-svn-project":  ("c++", "https://svn.code.sf.net/p/t/svn/", "", "/src/svn"),
    "a-heptapod-one":  ("c++", "https://foss.heptapod.net/t/t", "", "/src/hept"),
    "an-hg-repo-path": ("c++", "https://www.example.org/repo/hg", "", "/src/hgp"),
    "a-disabled-one":  ("c++", "https://github.com/x/dis", "disabled: true\n", "/src/dis"),
    "no-repo":         ("c++", "", "", "/src/norepo"),
    "shared-workdir":  ("c++", "https://github.com/x/sw", "", "/src"),
    "afl-only":        ("c++", "https://github.com/x/afl",
                        "fuzzing_engines:\n  - afl\n", "/src/afl"),
    "vulnerable-project": ("c++", "https://github.com/google/oss-fuzz", "", "/src/vp"),
    "bad_example":     ("c++", "https://github.com/madler/zlib", "", "/src/zlib"),
}

_EXPECTED_POOL = ["alpha", "bravo", "charlie", "delta", "echo", "foxtrot"]


def _fake_oss_fuzz_tree():
    """A projects/ tree with one project per eligibility outcome."""
    import tempfile
    root = tempfile.mkdtemp()
    for name, (lang, repo, extra, workdir) in _FAKE_PROJECTS.items():
        d = os.path.join(root, "projects", name)
        os.makedirs(d)
        with open(os.path.join(d, "project.yaml"), "w") as fh:
            fh.write(f"language: {lang}\n")
            if repo:
                fh.write(f'main_repo: "{repo}"\n')
            fh.write(extra)
        with open(os.path.join(d, "Dockerfile"), "w") as fh:
            fh.write(f"FROM base\nWORKDIR {workdir}\n")
    return root


def _pool(root):
    from oss_fuzz.select_projects import eligible_projects
    of = OssFuzz(oss_fuzz_dir=root, work_dir="/tmp/vp_test_wd")
    return eligible_projects(of, ("c++",), "address", "libfuzzer")


def test_selection_pool_excludes_every_undrivable_project():
    pool, rejected = _pool(_fake_oss_fuzz_tree())
    assert sorted(pool) == _EXPECTED_POOL, sorted(pool)
    # Each exclusion is attributed, so a surprising pool size is explainable.
    assert rejected["language"] == 1            # the 'c' one (go never reaches us)
    assert rejected["disabled"] == 1
    assert rejected["not-git"] == 4             # hg, svn, heptapod, /repo/hg
    assert rejected["fixture"] == 2             # vulnerable-project, bad_example
    assert rejected["unsupported"] == 3         # no main_repo, /src workdir, afl-only
    print("ok  selection pool excludes non-C++, disabled, non-git, fixtures, unsupported")


def test_selection_is_reproducible_for_a_fixed_tree():
    from oss_fuzz.select_projects import select
    pool, _ = _pool(_fake_oss_fuzz_tree())
    assert select(pool, 4, 42) == select(pool, 4, 42)
    # A different seed must actually reshuffle, or "seed 42" means nothing.
    assert select(pool, 4, 42) != select(pool, 4, 7)
    # Filesystem order must not leak in: the same set in any order selects alike.
    assert select(list(reversed(pool)), 4, 42) == select(pool, 4, 42)
    print("ok  selection is reproducible and order-independent")


def test_raising_the_count_extends_the_selection():
    # shuffle-then-take, not random.sample: growing -n must keep the earlier
    # projects so a widened sweep is a superset of the narrower one.
    from oss_fuzz.select_projects import select
    pool, _ = _pool(_fake_oss_fuzz_tree())
    assert select(pool, 5, 42)[:2] == select(pool, 2, 42)
    # Asking for more than exists yields the whole pool, not an error.
    assert sorted(select(pool, 999, 42)) == _EXPECTED_POOL
    print("ok  raising the count extends rather than replaces the selection")


def test_probed_selection_backfills_and_keeps_extending():
    """Dropping a probed project must not reorder the ones after it, or the
    'raising -n extends the selection' guarantee dies with the probes on."""
    from oss_fuzz.select_projects import select, select_probed
    pool, _ = _pool(_fake_oss_fuzz_tree())
    unprobed = select(pool, 999, 42)
    doomed = {unprobed[1], unprobed[2]}

    def probe(name):
        return "dead repo" if name in doomed else None

    chosen, dropped = select_probed(pool, 3, 42, probe)
    # The two bad picks are replaced by the next survivors, in order.
    assert chosen == [p for p in unprobed if p not in doomed][:3], chosen
    assert sorted(n for n, _ in dropped) == sorted(doomed), dropped
    # Still a prefix relationship, which is the property worth protecting.
    assert select_probed(pool, 2, 42, probe)[0] == chosen[:2]
    # Probing nothing away must agree with the unprobed sample exactly.
    assert select_probed(pool, 3, 42, lambda n: None)[0] == select(pool, 3, 42)
    # A probe is only asked about projects the walk actually reaches.
    asked = []
    select_probed(pool, 2, 42, lambda n: asked.append(n))
    assert len(asked) == 2, asked
    print("ok  probed selection backfills without reordering")


def test_probes_keep_a_project_when_they_cannot_get_an_answer():
    """Fail open. A DNS blip or an OSV outage is not evidence that a project is
    dead, and dropping one on that basis silently reshapes the sweep."""
    import subprocess
    import tempfile
    from oss_fuzz.select_projects import (_REPO_GONE_RES, no_usable_bug,
                                          repo_is_gone)

    # git exits 128 for a deleted repo and a DNS failure alike, so only the
    # message separates them. Both forges answer a deleted-or-private repo by
    # asking for a username, which with prompts disabled reads as the first case.
    for stderr, gone in (
            ("fatal: could not read Username for 'https://github.com': "
             "terminal prompts disabled", True),
            ("remote: Repository not found.", True),
            ("fatal: Authentication failed for 'https://x/'", True),
            # ...and the transient ones, which must NOT drop the project.
            ("fatal: unable to access 'https://x/': Could not resolve host: x",
             False),
            ("fatal: unable to access 'https://x/': Failed to connect", False),
            ("error: RPC failed; curl 92 HTTP/2 stream 0 was not closed",
             False)):
        hit = any(rx.search(stderr) for rx in _REPO_GONE_RES)
        assert hit is gone, (stderr, hit)

    # A reachable repo is kept — a real ls-remote against a local one, so the
    # probe's own plumbing (flags, env, exit code) is exercised without network.
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["git", "init", "-q", "--bare", tmp], check=True)
        assert repo_is_gone(tmp) is None

    # OSV: a record with a fix commit keeps the project, nothing drops it...
    usable = [{"id": "OSV-1", "affected": [{"ranges": [{"type": "GIT",
               "repo": "https://x/y", "events": [{"introduced": "0"},
               {"fixed": "deadbeef"}]}]}]}]
    assert no_usable_bug("p", lambda _: usable) is None
    assert no_usable_bug("capnproto", lambda _: []) is not None
    # ...and an OSV that will not answer keeps it, rather than guessing.
    def boom(_):
        raise OSError("connection reset")
    assert no_usable_bug("p", boom) is None
    print("ok  probes fail open when they cannot get an answer")


def test_clone_falls_back_to_the_repo_that_has_the_fix_commit():
    """cryptofuzz moved from guidovranken/ to MozillaSecurity/. OSS-Fuzz updated
    project.yaml; OSV still names the old URL, which now 404s. The fix commit is
    in the new repo, so the run must fall back to it instead of dying — and must
    not accept a repo that merely clones but lacks the commit."""
    from oss_fuzz.run import _clone_fix_source

    OLD = "https://github.com/guidovranken/cryptofuzz"
    NEW = "https://github.com/MozillaSecurity/cryptofuzz"

    class _Of:
        def __init__(self, clonable, having):
            self.clonable, self.having, self.tried = clonable, having, []

        def clone_source(self, url):
            self.tried.append(url)
            if url not in self.clonable:
                raise RuntimeError("command failed (128): git clone")
            return "/cache/" + url.rsplit("/", 1)[-1]

        def has_commit(self, repo, commit):
            return repo in self.having

    class _Cand:
        main_repo = OLD
        fixed_commit = "0806bc7eaa7a0749585e368876ac723f69fa5e10"

    # The OSV URL is dead, so the project.yaml one is used and recorded.
    cand = _Cand()
    of = _Of(clonable={NEW}, having={"/cache/cryptofuzz"})
    assert _clone_fix_source(of, cand, NEW) == "/cache/cryptofuzz"
    assert of.tried == [OLD, NEW], of.tried
    assert cand.main_repo == NEW, cand.main_repo

    # A repo that clones but does not carry the fix commit is not the right repo:
    # parent_commit would resolve '<sha>~1' to a literal string and sail on.
    of = _Of(clonable={OLD, NEW}, having=set())
    assert _clone_fix_source(of, _Cand(), NEW) is None
    assert of.tried == [OLD, NEW], of.tried

    # One URL named twice is probed once.
    of = _Of(clonable=set(), having=set())
    assert _clone_fix_source(of, _Cand(), OLD) is None
    assert of.tried == [OLD], of.tried
    print("ok  clone falls back to the repo holding the fix commit")


def test_clonable_with_git_keeps_real_git_hosts():
    from oss_fuzz.select_projects import clonable_with_git
    # Gitiles/Gitea/git:// and self-hosted /git/ paths are real git: rejecting
    # them would drop 21 of the pool's 26 non-GitHub projects.
    for ok in ("https://chromium.googlesource.com/angle/angle",
               "https://gitea.osgeo.org/geos/geos",
               "git://people.freedesktop.org/~dvdhrm/libtsm",
               "https://www.bearssl.org/git/BearSSL",
               "https://github.com/madler/zlib"):
        assert clonable_with_git(ok), ok
    for bad in ("https://hg.mozilla.org/projects/nss",
                "https://svn.code.sf.net/p/lame/svn/trunk/lame",
                "https://foss.heptapod.net/graphicsmagick/graphicsmagick",
                "https://www.mercurial-scm.org/repo/hg"):
        assert not clonable_with_git(bad), bad
    print("ok  git-clonability check keeps Gitiles/Gitea/git:// and drops hg/svn")


def test_clone_cache_separates_repos_that_share_a_basename():
    from oss_fuzz.ossfuzz import cache_name
    # Real collisions in the C++ pool: same last path segment, different repos.
    for a, b in (("https://github.com/cfengine/core",
                  "https://git.libreoffice.org/core"),
                 ("https://github.com/boostorg/json.git",
                  "https://github.com/nlohmann/json.git"),
                 ("https://git.code.sf.net/p/ibmswtpm2/tpm2",
                  "https://chromium.googlesource.com/chromiumos/third_party/tpm2")):
        assert cache_name(a) != cache_name(b), (a, b)
    print("ok  clone cache separates repos sharing a basename")


def test_clone_cache_still_shares_one_clone_per_repo():
    from oss_fuzz.ossfuzz import cache_name
    # llvm/llvm_libcxx/llvm_libcxxabi differ only by '.git'. Splitting them
    # would mean three copies of a multi-GB checkout.
    base = cache_name("https://github.com/llvm/llvm-project")
    for same in ("https://github.com/llvm/llvm-project.git",
                 "https://github.com/llvm/llvm-project/",
                 "http://github.com/llvm/llvm-project",
                 "https://GitHub.com/llvm/llvm-project"):
        assert cache_name(same) == base, same
    print("ok  clone cache shares one directory per repo")


def test_clone_cache_name_stays_readable():
    from oss_fuzz.ossfuzz import cache_name
    # The basename leads: these paths are read by humans debugging a sweep.
    name = cache_name("https://github.com/MozillaSecurity/cryptofuzz")
    assert name.startswith("src__cryptofuzz__"), name
    assert re.fullmatch(r"src__cryptofuzz__[0-9a-f]{8}", name), name
    print("ok  clone cache name keeps the readable basename")


def _osv_record(osv_id, published, fixed="deadbeef"):
    """A minimal OSV record that ``select_from_records`` finds usable."""
    return {"id": osv_id, "published": published,
            "affected": [{"ranges": [{"type": "GIT", "repo": "https://x/y",
                                      "events": [{"introduced": "0"},
                                                 {"fixed": fixed}]}]}]}


def test_recency_ranking_orders_projects_by_newest_disclosure():
    """The whole point of --order recent: freshest bug first, and a project's
    rank comes from its *newest* record, not its oldest or its record count."""
    from oss_fuzz.select_projects import rank_by_recency
    catalogue = {
        # Two records, and the older one must not decide the project's rank.
        "fresh":  [_osv_record("OSV-A", "2026-01-05T00:00:00Z"),
                   _osv_record("OSV-B", "2019-01-01T00:00:00Z")],
        "stale":  [_osv_record("OSV-C", "2020-06-01T00:00:00Z")],
        "middle": [_osv_record("OSV-D", "2024-03-02T00:00:00Z")],
        # Records, but none with a fix boundary: nothing to rank on.
        "nofix":  [{"id": "OSV-E", "published": "2026-08-01T00:00:00Z"}],
        "nobugs": [],
    }
    ranked, dropped = rank_by_recency(sorted(catalogue), catalogue.get, workers=4)

    assert [name for name, _, _ in ranked] == ["fresh", "middle", "stale"], ranked
    assert ranked[0][2] == "OSV-A", ranked[0]      # the record that won it
    assert sorted(n for n, _ in dropped) == ["nobugs", "nofix"], dropped
    assert all(why.startswith("no OSV record") for _, why in dropped), dropped
    print("ok  recency ranking orders projects by their newest usable record")


def test_recency_ranking_is_stable_and_fails_open():
    """Two guarantees that only show up under concurrency and outages: a tie
    must not depend on which thread finished first, and an OSV that will not
    answer must not look like a project with no bugs."""
    from oss_fuzz.select_projects import rank_by_recency
    same_day = {f"p{i}": [_osv_record(f"OSV-{i}", "2026-02-02T00:00:00Z")]
                for i in range(8)}
    order = [rank_by_recency(sorted(same_day), same_day.get, workers=8)[0]
             for _ in range(3)]
    assert order[0] == order[1] == order[2], order
    assert [n for n, _, _ in order[0]] == sorted(same_day), order[0]

    def flaky(project):
        if project == "down":
            raise OSError("connection reset")
        return [_osv_record("OSV-OK", "2026-05-05T00:00:00Z")]

    ranked, dropped = rank_by_recency(["down", "up"], flaky, workers=2)
    # Kept, so an outage cannot silently shrink the pool -- but ranked last,
    # because "we could not ask" is not evidence of a fresh bug.
    assert [n for n, _, _ in ranked] == ["up", "down"], ranked
    assert dropped == [], dropped
    print("ok  recency ranking breaks ties by name and fails open on an outage")


def test_recent_selection_probes_only_the_candidates_it_reaches():
    """The repo probe is the expensive half (a network round trip per project),
    so it must run in rank order and stop as soon as -n are accepted."""
    from oss_fuzz.select_projects import select_recent
    catalogue = {n: [_osv_record(f"OSV-{n}", d)] for n, d in (
        ("a", "2026-08-01T00:00:00Z"), ("b", "2026-07-01T00:00:00Z"),
        ("c", "2026-06-01T00:00:00Z"), ("d", "2026-05-01T00:00:00Z"))}
    asked = []

    def probe(project):
        asked.append(project)
        return "main_repo is unreachable" if project == "b" else None

    chosen, dropped = select_recent(catalogue, 2, catalogue.get, probe)
    assert [n for n, _, _ in chosen] == ["a", "c"], chosen   # 'b' backfilled
    assert dropped == [("b", "main_repo is unreachable")], dropped
    assert asked == ["a", "b", "c"], asked                   # 'd' never probed
    # Same prefix guarantee the shuffle order gives: raising -n extends.
    assert select_recent(catalogue, 4, catalogue.get, lambda _: None)[0][:2] == \
        select_recent(catalogue, 2, catalogue.get, lambda _: None)[0]
    print("ok  recent selection probes in rank order and stops when full")


def test_artifacts_record_the_generators_whole_input():
    """The three parts the method rests on must survive the process: the fix
    diff, the original bug's triggering evidence, and the reachable set. A log
    keeps only the function *names*, so without these a result cannot be
    re-derived from a finished run."""
    import tempfile
    from oss_fuzz.analysis import PatchContext, TouchedFunction
    from oss_fuzz.artifacts import RunArtifacts
    from oss_fuzz.bugclass import classify

    d = tempfile.mkdtemp()
    poc = os.path.join(d, "crash-input")
    with open(poc, "wb") as fh:
        fh.write(b"\xff\x00bad")

    ctx = PatchContext(
        language="c", patch_text="--- a/x.c\n+++ b/x.c\n@@\n-bad\n+good\n",
        functions=[TouchedFunction(file="x.c", name="parse", source="{}",
                                   start_line=12)],
        root_cause_reachable=["parse", "lex", "emit"],
        reachable_source="fuzz-introspector")
    target = CveTarget(cve_id="CVE-2024-1", osv_id="OSV-2024-9", project="demo",
                       fixed_commit="abc123", crash_type="Heap-buffer-overflow",
                       crash_state=["lex", "parse"], fuzz_target="xml_fuzz")

    art = RunArtifacts(os.path.join(d, "artifacts"), "demo")
    path = art.record_generation_input(
        target, ctx, sanitizer="address", bug_class=classify("Heap-buffer-overflow"),
        vuln_commit="abc122", head_commit="head99", reproducer=poc)
    rec = json.load(open(path))

    assert rec["patch"]["touched_functions"][0]["name"] == "parse", rec
    assert open(os.path.join(art.dir, "inputs/fix.diff")).read() == ctx.patch_text
    # The PoC is copied, not referenced: a path into a cache directory the run
    # cleans up is not evidence.
    assert open(os.path.join(art.dir, "inputs/poc.bin"), "rb").read() == b"\xff\x00bad"
    assert rec["trigger"]["crash_state"] == ["lex", "parse"], rec
    assert rec["reachable"]["functions"] == ["parse", "lex", "emit"], rec
    # Which analyser produced the set: an introspector-to-heuristic fallback
    # silently degrades the steering, and this is the only place it is recorded.
    assert rec["reachable"]["source"] == "fuzz-introspector", rec
    assert open(os.path.join(art.dir, "inputs/reachable.txt")).read() == \
        "parse\nlex\nemit\n"
    print("ok  artifacts record the diff, the trigger and the reachable set")


def test_artifacts_clip_marks_the_cut():
    """A fuzzer can emit hundreds of MB (ogre's image_fuzz: 167MB of 0xff), so
    a log is clipped — but never silently: a truncated sanitizer report that
    looks complete is worse than no log."""
    from oss_fuzz.artifacts import clip

    text = "H" * 100 + "M" * 10_000 + "T" * 100
    out = clip(text, limit=400)
    assert len(out) < len(text)
    assert out.startswith("H" * 100), out[:120]
    assert out.endswith("T" * 100), out[-120:]
    assert "clipped" in out
    assert clip("short", limit=400) == "short"      # untouched under the cap
    print("ok  oversized engine output is clipped head-and-tail, and says so")


def test_campaign_records_prompt_harness_and_engine_log_per_attempt():
    """Every attempt leaves its prompt, its harness and the engine's output —
    including the rejected attempts, which are the ones a 0/N campaign needs
    explaining by."""
    import tempfile
    from oss_fuzz.artifacts import RunArtifacts
    from oss_fuzz.campaign import HarnessCampaign
    from oss_fuzz.ossfuzz import RunOutcome

    d = tempfile.mkdtemp()
    art = RunArtifacts(d, "demo")

    class _Gen:
        def generate(self, messages):
            return ("```c\nint LLVMFuzzerTestOneInput(const uint8_t *d, "
                    "size_t s){ return 0; }\n```")

    class _Of:
        last_build_stderr = ""
        last_build_infra_error = None
        artifacts = art

        def build_harness(self, project, checkout, name, source, ext,
                          sanitizer, placement=None):
            return "/out/" + name

        def run_fuzzer(self, project, harness_name, seconds, sanitizer,
                       bug_class=None, log_tag=None):
            # The real OssFuzz writes the log; here we only assert the campaign
            # asked for a tag that separates this run from the HEAD run of the
            # same harness (both would otherwise be 'vp_harness_N.log').
            art.record_fuzz_log(log_tag, "helper.py run_fuzzer", 0, "stats", "")
            return RunOutcome(triggered=False, timed_out=False, returncode=0)

    camp = HarnessCampaign(generator=_Gen(), oss_fuzz=_Of(), project="demo",
                           vuln_checkout=None, sanitizer="address", ext=".c",
                           target_successes=1, max_attempts=2, artifacts=art)
    camp.run(lambda covered, sigs: [{"role": "system", "content": "sys"},
                                    {"role": "user", "content": f"steer {sigs}"}])

    assert sorted(os.listdir(os.path.join(art.dir, "prompts"))) == \
        ["attempt_001.txt", "attempt_002.txt"]
    assert sorted(os.listdir(os.path.join(art.dir, "harnesses"))) == \
        ["vp_harness_1.c", "vp_harness_2.c"]
    assert sorted(os.listdir(os.path.join(art.dir, "fuzz"))) == \
        ["verify_vp_harness_1.log", "verify_vp_harness_2.log"]
    prompt = open(os.path.join(art.dir, "prompts/attempt_001.txt")).read()
    assert "----- system -----" in prompt and "----- user -----" in prompt
    print("ok  campaign records prompt, harness and engine log per attempt")


def _ctx_for_prompt():
    from oss_fuzz.analysis import PatchContext
    return PatchContext(language="c++", patch_text="--- a\n+++ b\n",
                        functions=[], headers=[], root_cause_reachable=[])


# OSS-Fuzz prints this before every build, and every line of it contains the
# word 'error'.
CFLAGS_BANNER = (
    "CFLAGS=-O1 -fno-omit-frame-pointer -Wno-error=int-conversion "
    "-Wno-error=implicit-function-declaration -fsanitize=address\n"
    "CXXFLAGS=-O1 -Wno-error=vla-cxx-extension -stdlib=libc++\n")


def test_error_lines_ignore_the_compiler_flag_banner():
    """'-Wno-error=' is not an error.

    The 20260812 run reported grok's failure as a truncated CFLAGS string, in
    results.jsonl and in the repair prompt handed back to the model, because
    both helpers matched the bare word 'error'.
    """
    from oss_fuzz.ossfuzz import _build_error_excerpt, _first_error_line
    combined = (CFLAGS_BANNER
                + "+ mkdir build\nmkdir: cannot create directory 'build': "
                  "File exists\n"
                + "INFO:__main__:Running: docker run ...\n"
                  "ERROR:__main__:Building fuzzers failed.\n")
    first = _first_error_line(_build_error_excerpt(combined))
    assert first.startswith("mkdir: cannot create directory"), first

    # A real compiler diagnostic still wins over the banner around it.
    diag = (CFLAGS_BANNER
            + "/src/ogre/Tests/fuzz/image_fuzz.cpp:16:10: fatal error: "
              "'OgreMeshFileFormat.h' file not found\n")
    assert "OgreMeshFileFormat.h" in _first_error_line(diag)
    assert "Wno-error" not in _first_error_line(diag)
    print("ok  error extraction ignores the -Wno-error flag banner")



if __name__ == "__main__":
    fns = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for fn in fns:
        fn()
    print(f"\nAll {len(fns)} offline tests passed.")
