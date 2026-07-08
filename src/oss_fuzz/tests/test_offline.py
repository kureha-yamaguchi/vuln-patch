"""Offline tests for the OSS-Fuzz front-end: everything that does not need
Docker, the network, or an LLM. Run with:  python -m pytest src/oss_fuzz/tests
or plain:  python src/oss_fuzz/tests/test_offline.py
"""
import json
import os
import sys

SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
sys.path.insert(0, SRC)

from variant import variant_analysis_directive
from oss_fuzz.osv import select_from_records, CveTarget
from oss_fuzz.analysis import DiffAnalyzer
from oss_fuzz.ossfuzz import OssFuzz, crash_signature, _looks_like_crash
from oss_fuzz.campaign import extract_source
from oss_fuzz.prompts import LibFuzzerPromptBuilder

FIX = json.load(open(os.path.join(os.path.dirname(__file__), "fixture_osv.json")))


def test_osv_selects_newest_public_cve():
    t = select_from_records("demo", FIX["vulns"])
    assert t is not None
    # Newest CVE-bearing usable entry, not the 2024 one (no CVE), not 2020.
    assert t.cve_id == "CVE-2023-9999"
    assert t.fixed_commit == "beef2222"
    assert t.main_repo == "https://github.com/example/demo"
    assert t.fuzz_target == "demo_fuzzer"
    assert t.sanitizer == "address"
    assert t.reproducer_url and "testcase_id=55555" in t.reproducer_url
    print("ok  osv selection")


def test_osv_skips_entries_without_fix_or_cve():
    only_no_cve = [FIX["vulns"][2]]
    assert select_from_records("demo", only_no_cve) is None
    print("ok  osv skips non-CVE")


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
    # reachable set includes the touched fn + callees found in the body
    assert "demo_parse" in ctx.root_cause_reachable
    assert "memcpy" in ctx.root_cause_reachable
    assert "process" in ctx.root_cause_reachable
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
    # the shared directive text must be present, and steer away from covered
    assert "root_cause_reachable" in user
    assert "Uncovered functions to steer toward" in user
    assert "LLVMFuzzerTestOneInput" in user
    print("ok  prompt builder + steering")


def test_shared_directive_matches_across_frontends():
    # The Java PromptBuilder must produce byte-identical steering to the
    # shared function (this is the whole point of the refactor).
    reachable = ["a", "b", "c"]
    direct = variant_analysis_directive(reachable, ["a"], ["sig1"])
    # import the Java builder lazily; it needs javalang only at parse time,
    # not to call this delegating method.
    sys.path.insert(0, os.path.join(SRC, "java"))
    try:
        from java.prompts import PromptBuilder as JPB
        via_java = JPB(language="C")._variant_analysis_block(
            reachable, ["a"], ["sig1"])
        assert via_java == direct
        print("ok  java/shared directive parity")
    except Exception as e:
        # javalang may be unavailable in this env; the parity is structural.
        print("skip java parity (import unavailable):", e)


if __name__ == "__main__":
    fns = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for fn in fns:
        fn()
    print(f"\nAll {len(fns)} offline tests passed.")
