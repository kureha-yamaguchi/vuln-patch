"""Extract Java source from the LLM response, write it to disk, and
compile it against the buggy Defects4J project's classpath plus the
Jazzer API jar."""
import os
import re
import subprocess
from dataclasses import dataclass, asdict
from typing import Dict


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

    # --- compilation -----------------------------------------------------

    def build(self, harness_source: str, buggy_dir: str,
              output_subdir: str = '') -> BuildResult:
        """Compile `harness_source` against `buggy_dir`'s classpath.

        If `output_subdir` is given, the .java and .class files land in
        `<buggy_dir>/fuzz/<output_subdir>/` rather than directly in
        `<buggy_dir>/fuzz/` — useful when running many attempts so the
        successful ones survive past the next overwrite.
        """
        cls_name = self.primary_class_name(harness_source) or 'FuzzHarness'

        fuzz_dir = (os.path.join(buggy_dir, 'fuzz', output_subdir)
                    if output_subdir
                    else os.path.join(buggy_dir, 'fuzz'))
        os.makedirs(fuzz_dir, exist_ok=True)
        harness_path = os.path.join(fuzz_dir, f'{cls_name}.java')
        with open(harness_path, 'w') as fh:
            fh.write(harness_source)

        classpath = os.pathsep.join([
            self._test_classpath(buggy_dir),
            self.jazzer_api_jar,
        ])

        result = subprocess.run(
            ['javac', '-cp', classpath, '-d', fuzz_dir, harness_path],
            capture_output=True, text=True,
        )
        return BuildResult(
            harness_path=harness_path,
            class_name=cls_name,
            classpath=classpath,
            compiled=result.returncode == 0,
            returncode=result.returncode,
            stdout=result.stdout,
            stderr=result.stderr,
            attempt_label=output_subdir,
        )

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