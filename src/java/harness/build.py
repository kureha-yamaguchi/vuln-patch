"""Extract Java source from the LLM response, write it to disk, and
compile it against the buggy Defects4J project's classpath plus the
Jazzer API jar."""
import os
import re
import subprocess
from dataclasses import dataclass, asdict
from typing import Dict, Optional


@dataclass
class BuildResult:
    """Outcome of compiling a single generated harness. Suitable for
    serialising and aggregating across many runs when evaluating how
    well the LLM's harnesses build."""
    harness_path: str
    class_name: str
    classpath: str
    compiled: bool
    returncode: int
    stdout: str
    stderr: str
    attempt_label: str = ''   # e.g. 'attempt_001'; empty for one-shot use

    def as_dict(self) -> dict:
        return asdict(self)


class HarnessBuilder:
    """Saves the LLM-generated harness next to the buggy project and
    invokes javac against the project's test classpath + the Jazzer API
    jar.

    Designed to be reused across many attempts in a single campaign:

      * the project's classpath is resolved (via `defects4j compile`
        + `defects4j export -p cp.test`) only the first time we see a
        given buggy_dir, then cached;
      * each call to `build` accepts an `output_subdir` so successful
        attempts don't overwrite each other on disk.
    """

    _FENCED_RE = re.compile(r'```(?:java)?\s*\n(.*?)```', re.DOTALL)
    _CLASS_RE  = re.compile(
        r'public\s+(?:final\s+|abstract\s+)?class\s+(\w+)'
    )
    # `package org.apache.commons.lang;` → captures the dotted package name.
    _PACKAGE_RE = re.compile(r'(?m)^\s*package\s+([\w.]+)\s*;')

    def __init__(self, jazzer_api_jar: str):
        self.jazzer_api_jar = jazzer_api_jar
        # buggy_dir -> resolved classpath string. Avoids re-running
        # `defects4j compile` (slow) on every attempt of a campaign.
        self._classpath_cache: Dict[str, str] = {}

    # --- response parsing ------------------------------------------------

    @classmethod
    def extract_source(cls, llm_response: str) -> str:
        """Strip optional markdown fences from an LLM response and return
        the Java code as a single string. The prompt forbids fences, but
        gpt-oss-20b sometimes adds them anyway."""
        fenced = cls._FENCED_RE.search(llm_response)
        if fenced:
            return fenced.group(1).strip()
        return llm_response.strip()

    @classmethod
    def primary_class_name(cls, java_source: str):
        """Return the first public-class name declared in `java_source`,
        or None. javac requires the file name to match this."""
        m = cls._CLASS_RE.search(java_source)
        return m.group(1) if m else None

    @classmethod
    def package_name(cls, java_source: str):
        """Return the declared package (e.g. 'org.apache.commons.lang'),
        or None if the harness is in the default package."""
        m = cls._PACKAGE_RE.search(java_source)
        return m.group(1) if m else None

    @classmethod
    def fully_qualified_class_name(cls, java_source: str) -> str:
        """Return the FQ class name Jazzer needs for `--target_class`.

        javac writes the .class into a package-structured subtree under
        the output dir, so Jazzer must be told the *fully-qualified* name
        (e.g. 'org.apache.commons.lang.FuzzHarness'), not the simple name,
        or it reports the class as not found on the classpath."""
        simple = cls.primary_class_name(java_source) or 'FuzzHarness'
        pkg = cls.package_name(java_source)
        return f'{pkg}.{simple}' if pkg else simple

    # The exact entrypoint Jazzer calls. The FQ prefix on FuzzedDataProvider
    # is optional because same-package harnesses often import it instead.
    _ENTRYPOINT_RE = re.compile(
        r'void\s+fuzzerTestOneInput\s*\(\s*'
        r'(?:com\.code_intelligence\.jazzer\.api\.)?FuzzedDataProvider\s+\w+\s*\)'
    )

    @classmethod
    def looks_like_harness(cls, llm_response: str) -> Optional[str]:
        """Return None if the response is structurally a usable harness, or
        a short human-readable reason string if it is not.

        This is a fast, javac-free reject for the dominant gpt-oss-20b
        failure mode: returning prose, a markdown-wrapped explanation, a
        `main()` demo, or a JUnit test instead of a FuzzHarness. It is NOT
        a compilation check — it only filters responses that *cannot* be
        the artifact we want, so the caller can regenerate without spending
        a javac run (and, if it chooses, without spending an attempt slot).

        Deliberately strict on two things the extractor can't salvage:
          * an empty body, and
          * a surviving '```' fence — `extract_source` only unwraps a
            *well-formed* ```...``` block; a leading `/* */` comment
            followed by an unterminated fence (attempts 26/27/39) slips
            through as a stray backtick that javac chokes on. Rejecting and
            regenerating is cheaper than parsing every malformed variant.
        """
        src = cls.extract_source(llm_response)
        if not src.strip():
            return "empty response"
        if '```' in src:
            return "contains markdown fence"
        if 'class FuzzHarness' not in src:
            return "no `class FuzzHarness` declaration"
        if not cls._ENTRYPOINT_RE.search(src):
            return "no fuzzerTestOneInput(FuzzedDataProvider) entrypoint"
        return None

    # --- compilation -----------------------------------------------------

    def build(self, harness_source: str, buggy_dir: str,
              output_subdir: str = '', extra_classpath=()) -> BuildResult:
        """Compile `harness_source` against `buggy_dir`'s classpath.

        If `output_subdir` is given, the .java and .class files land in
        `<buggy_dir>/fuzz/<output_subdir>/` rather than directly in
        `<buggy_dir>/fuzz/` — useful when running many attempts so the
        successful ones survive past the next overwrite.

        `extra_classpath` entries are prepended to javac's -cp. Roll 10:
        the reference driver compiles against a class the PREVIOUS build()
        call produced, but the -d output dir was never on the compile
        classpath — `cannot find symbol: class ReferenceImpl` with the
        .class sitting right there. Roll 6's seam, one phase over: the
        runtime classpath was fixed, the compile classpath was not.
        """
        simple_name = self.primary_class_name(harness_source) or 'FuzzHarness'
        # Jazzer's --target_class needs the fully-qualified name; javac
        # places the .class under a package subtree, so the simple name
        # alone won't resolve on the classpath.
        fq_name = self.fully_qualified_class_name(harness_source)

        fuzz_dir = (os.path.join(buggy_dir, 'fuzz', output_subdir)
                    if output_subdir
                    else os.path.join(buggy_dir, 'fuzz'))
        os.makedirs(fuzz_dir, exist_ok=True)
        # The source file name must match the public class's simple name.
        harness_path = os.path.join(fuzz_dir, f'{simple_name}.java')
        with open(harness_path, 'w') as fh:
            fh.write(harness_source)

        classpath = os.pathsep.join(
            [str(p) for p in extra_classpath if p] + [
                self._test_classpath(buggy_dir),
                self.jazzer_api_jar,
            ])

        result = subprocess.run(
            # -encoding UTF-8: harness/relation sources can carry non-ASCII
            # (string literals lifted from tests, unicode probe inputs);
            # without it javac falls back to the platform charset and a
            # US-ASCII locale rejects the file outright.
            ['javac', '-encoding', 'UTF-8', '-cp', classpath,
             '-d', fuzz_dir, harness_path],
            capture_output=True, text=True,
        )
        return BuildResult(
            harness_path=harness_path,
            class_name=fq_name,
            classpath=classpath,
            compiled=result.returncode == 0,
            returncode=result.returncode,
            stdout=result.stdout,
            stderr=result.stderr,
            attempt_label=output_subdir,
        )

    def test_classpath(self, buggy_dir: str) -> str:
        """Public accessor for the project's (cached) test classpath.
        The in-campaign verifier needs this to run the harness against
        the buggy checkout it was compiled against."""
        return self._test_classpath(buggy_dir)

    def _test_classpath(self, buggy_dir: str) -> str:
        """Compile the Defects4J project once and return its test
        classpath. cp.test is preferable to cp.compile here because it
        includes the project's own compiled classes in addition to its
        dependencies, which is what the harness actually needs to link
        against.

        Cached per buggy_dir so a campaign of N attempts only pays the
        `defects4j compile` cost once.
        """
        if buggy_dir in self._classpath_cache:
            return self._classpath_cache[buggy_dir]

        subprocess.run(['defects4j', 'compile'], cwd=buggy_dir, check=True)
        cp = subprocess.run(
            ['defects4j', 'export', '-p', 'cp.test'],
            cwd=buggy_dir, check=True, capture_output=True, text=True,
        ).stdout.strip()
        self._classpath_cache[buggy_dir] = cp
        return cp