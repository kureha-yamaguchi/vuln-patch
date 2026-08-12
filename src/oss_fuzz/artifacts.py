"""What a run was fed, and what the fuzzing engine said back.

The per-project log a sweep keeps (``runs/<sweep>/logs/<project>.log``) is the
pipeline's own commentary. It records that a harness was accepted and what
signature it found, but not the two things needed to judge either claim:

  * The **generation input** — the fix diff (or the PoC), the original bug's
    triggering evidence, and the extracted reachable-function set — is the
    whole research heuristic. ``run.py`` prints only a summary of it (function
    *names*), so a result cannot be re-derived from a log: the diff text, the
    touched-function bodies and the exact prompt the model saw are gone the
    moment the process exits. Without them "3 harnesses accepted" is a number
    with nothing behind it.
  * The **fuzzing engine output** is captured into ``RunOutcome`` and thrown
    away. Everything downstream reads two lines out of it — a crash signature
    and a crash reason — while the sanitizer report itself (the faulting stack,
    the allocation site, libFuzzer's final stats, the coverage/exec counters
    that say whether a clean run even got anywhere) never reaches the log. A
    sibling claim therefore arrives with no evidence attached, and a harness
    that ran clean gives no hint whether it was starved or simply wrong.

Nothing depends on this module: every entry point takes ``artifacts=None`` and
keeps its previous behaviour, so a run without ``--artifacts-dir`` is unchanged.

Layout, for ``--artifacts-dir runs/<sweep>/artifacts``::

    artifacts/<project>/
      inputs/generation-input.json   the generator's input, as one record
      inputs/fix.diff                the fix diff, verbatim and uncapped
      inputs/trigger.txt             the original bug's triggering evidence
      inputs/poc.bin                 the PoC testcase, when --reproducer gave one
      inputs/reachable.txt           the reachable-function set, one per line
      prompts/attempt_003.txt        the exact messages sent to the LLM
      harnesses/vp_harness_3.cc      what came back
      fuzz/verify_vp_harness_3.log   engine output, vulnerable build (the gate)
      fuzz/head_vp_harness_3.log     engine output, HEAD (the sibling claim)
      build/vuln_vp_harness_3.log    compiler output, when a build failed

``inputs/`` is written once per project, before the first LLM call, so an
interrupted run still records what it set out to do.
"""
from __future__ import annotations

import json
import os
import shutil
from typing import Dict, List, Optional

# A fuzzer's output is unbounded and not always text: ogre's image_fuzz emitted
# a raw 0xff 167MB into one run (see OssFuzz._run_with_timeout). Saved verbatim
# that is one harness filling the disk a sweep needs for its clones and build
# trees, so a log is clipped — keeping the head, where the command line and the
# seed are, and the much larger tail, where the sanitizer report and libFuzzer's
# final stats are. Raise MAX_LOG_BYTES if a report is ever cut in half.
MAX_LOG_BYTES = int(os.getenv("OSS_FUZZ_MAX_LOG_BYTES", str(1 << 20)))
_HEAD_SHARE = 0.25


def clip(text: str, limit: int = MAX_LOG_BYTES) -> str:
    """``text`` shortened to ``limit`` characters, head and tail kept, with the
    cut marked — never silently, since a truncated sanitizer report that looks
    complete is worse than no log at all."""
    if limit <= 0 or len(text) <= limit:
        return text
    head = int(limit * _HEAD_SHARE)
    tail = limit - head
    return (text[:head]
            + f"\n\n... [{len(text) - limit} characters clipped by "
              f"oss_fuzz/artifacts.py; raise OSS_FUZZ_MAX_LOG_BYTES "
              f"(currently {limit}) to keep more] ...\n\n"
            + text[-tail:])


class RunArtifacts:
    """Files under ``<root>/<project>/``. Every write is best-effort.

    Bookkeeping must never end a run that is otherwise working — a full disk
    here costs evidence, not the result — so an ``OSError`` is warned about and
    swallowed, and every method returns ``None`` instead of a path when it did
    not manage to write.
    """

    def __init__(self, root: str, project: str):
        self.project = project
        self.root = os.path.abspath(root)
        self.dir = os.path.join(self.root, project)

    # -- low level ---------------------------------------------------------
    def _write(self, rel: str, text: str) -> Optional[str]:
        path = os.path.join(self.dir, rel)
        try:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            # Explicit utf-8: a fuzzer's output is not text (see
            # OssFuzz._run_with_timeout), and under a POSIX/C locale the
            # default encoding is ASCII, which would raise on the very
            # sanitizer report we are here to keep.
            with open(path, "w", encoding="utf-8", errors="replace") as fh:
                fh.write(text)
        except OSError as exc:
            print(f"  WARNING: could not write artifact {rel}: {exc}")
            return None
        return path

    def _rel(self, path: Optional[str]) -> Optional[str]:
        """A path relative to the artifacts root, for the JSON index."""
        return os.path.relpath(path, self.root) if path else None

    # -- the generator's input ---------------------------------------------
    def record_generation_input(self, target, context, *, sanitizer: str,
                                bug_class, vuln_commit: Optional[str],
                                head_commit: Optional[str],
                                placement=None,
                                reproducer: Optional[str] = None
                                ) -> Optional[str]:
        """Everything the harness generator is steered by, before it is asked.

        The three parts the method rests on, in the order the prompt uses them:
        the fix diff, the evidence the original bug fired, and the reachable
        set the variant-analysis block ranges over. They are written as
        separate files (readable, diffable, greppable across a sweep) plus one
        JSON record tying them together with the commits they came from.
        """
        diff_path = self._write("inputs/fix.diff", context.patch_text)
        trig_path = self._write("inputs/trigger.txt",
                                self._trigger_text(target, bug_class,
                                                   reproducer))
        reach_path = self._write(
            "inputs/reachable.txt",
            "".join(f"{fn}\n" for fn in context.root_cause_reachable))

        # The PoC itself when there is one: it is the other half of the
        # "PoC or patch diff" the generator can be steered by, and a URL in the
        # OSV record is not it — that testcase is fetched by hand and would not
        # survive the run otherwise.
        poc_path = None
        if reproducer and os.path.isfile(reproducer):
            poc_path = os.path.join(self.dir, "inputs", "poc.bin")
            try:
                os.makedirs(os.path.dirname(poc_path), exist_ok=True)
                shutil.copyfile(reproducer, poc_path)
            except OSError as exc:
                print(f"  WARNING: could not copy the PoC {reproducer}: {exc}")
                poc_path = None

        record = {
            "project": self.project,
            "osv_id": target.osv_id,
            "cve": target.cve_id,
            "published": target.published,
            "main_repo": target.main_repo,
            "fixed_commit": target.fixed_commit,
            "vuln_commit": vuln_commit,
            "head_commit": head_commit,
            "sanitizer": sanitizer,
            "language": target.language,
            "bug_kind": bug_class.kind if bug_class else None,
            "oracle": bug_class.oracle if bug_class else None,
            "harness_build": placement.describe() if placement else None,
            # 1) the patch — plus what the analyser pulled out of it, which is
            #    what actually reaches the prompt as function bodies.
            "patch": {
                "file": self._rel(diff_path),
                "bytes": len(context.patch_text),
                "touched_functions": [
                    {"name": fn.name, "file": fn.file, "line": fn.start_line}
                    for fn in context.functions],
                "headers": context.headers,
                "skipped_non_library": context.skipped_paths,
            },
            # 2) the triggering evidence. OSS-Fuzz keeps no test case, so the
            #    stand-in for a triggering test is ClusterFuzz's crash report —
            #    type plus the crashing stack, which is the call path a variant
            #    harness has to re-enter — and the PoC input when we have one.
            "trigger": {
                "file": self._rel(trig_path),
                "crash_type": target.crash_type,
                "crash_state": list(target.crash_state or []),
                "fuzz_target": target.fuzz_target,
                "report_url": target.report_url,
                "reproducer_url": target.reproducer_url,
                "poc_file": self._rel(poc_path),
            },
            # 3) the reachable set: the neighbourhood the variant-analysis
            #    directive steers across, and the one input whose quality
            #    silently degrades (introspector -> heuristic fallback).
            "reachable": {
                "file": self._rel(reach_path),
                "source": context.reachable_source,
                "count": len(context.root_cause_reachable),
                "functions": context.root_cause_reachable,
            },
        }
        return self._write("inputs/generation-input.json",
                           json.dumps(record, indent=2) + "\n")

    @staticmethod
    def _trigger_text(target, bug_class, reproducer: Optional[str]) -> str:
        lines = [
            "# The evidence that the ORIGINAL bug fired: this corpus's",
            "# stand-in for a triggering test. OSS-Fuzz publishes no test",
            "# case, so what stands in for one is the crash ClusterFuzz saw",
            "# (type + crashing stack, innermost frame first) and, when",
            "# --reproducer named a local file, the PoC input beside it.",
            "",
            f"osv id       : {target.osv_id}",
            f"cve          : {target.cve_id or '(none)'}",
            f"fuzz target  : {target.fuzz_target or '(not recorded)'}",
            f"crash type   : {target.crash_type or '(not recorded)'}",
        ]
        if bug_class:
            lines.append(f"bug kind     : {bug_class.describe()}")
        if target.crash_state:
            lines.append("crash stack  : "
                         + " <- ".join(target.crash_state))
        if target.report_url:
            lines.append(f"report       : {target.report_url}")
        if target.reproducer_url:
            lines.append(f"testcase url : {target.reproducer_url}")
        lines.append(f"poc supplied : {reproducer or '(none)'}")
        if target.summary:
            lines += ["", "summary:", target.summary]
        return "\n".join(lines) + "\n"

    # -- per attempt --------------------------------------------------------
    def record_prompt(self, attempt: int,
                      messages: List[Dict[str, str]]) -> Optional[str]:
        """The exact messages sent to the model on this attempt.

        Saved per attempt rather than once, because the prompt is not fixed:
        the campaign re-steers it with the covered functions and the signatures
        already found, and a build failure replaces it entirely with a repair
        message. Comparing attempt N to attempt N-1 is the only way to see what
        the steering actually said.
        """
        body = "\n\n".join(f"----- {m.get('role', '?')} -----\n"
                           f"{m.get('content', '')}" for m in messages)
        return self._write(f"prompts/attempt_{attempt:03d}.txt", body + "\n")

    def record_harness(self, name: str, ext: str,
                       source: str) -> Optional[str]:
        """The generated harness — kept for every attempt, not just accepted
        ones, since the rejected ones are what explain a campaign that spent 30
        attempts and accepted nothing."""
        return self._write(f"harnesses/{name}{ext}", source)

    # -- engine and compiler output ----------------------------------------
    def record_fuzz_log(self, tag: str, command: str, returncode: int,
                        stdout: str, stderr: str) -> Optional[str]:
        """One libFuzzer run's output, verbatim.

        stdout and stderr are kept apart: helper.py's own messages go to
        stderr while the container's — the sanitizer report and libFuzzer's
        stats among them — arrive on stdout, and merging them interleaves two
        unrelated streams at whatever point the buffers happened to flush.
        """
        # Clipped per stream, so a chatty stdout cannot push helper.py's
        # stderr — where "target not found" and the like appear — out of the
        # file entirely.
        text = (f"# fuzzing engine output — {tag}\n"
                f"# command  : {command}\n"
                f"# exit code: {returncode}\n"
                "\n===== stdout =====\n" + clip(stdout or "")
                + "\n===== stderr =====\n" + clip(stderr or "") + "\n")
        return self._write(f"fuzz/{tag}.log", text)

    def record_build_log(self, tag: str, text: str) -> Optional[str]:
        return self._write(f"build/{tag}.log", clip(text))
