#!/usr/bin/env python3
"""Classify Defects4J bugs as "semantic" or "crashing" from their trigger-test stack traces.

A bug's trigger test(s) fail either with a plain JUnit assertion mismatch
(junit.framework.AssertionFailedError / *.ComparisonFailure / java.lang.AssertionError
directly, no message about an unexpected exception) -- meaning the program ran to
completion but produced the wrong value ("semantic") -- or with some other
uncaught exception/error propagating out of the code under test (NullPointerException,
IndexOutOfBoundsException, StackOverflowError, project-specific exceptions, etc.)
-- meaning the program crashed ("crashing").

If a bug has multiple trigger tests and they disagree, the bug is classified as
"crashing" (a crash anywhere is treated as evidence of a crashing defect).

Usage:
    python3 classify_bugs.py [--projects-dir PATH] [--csv OUT.csv]
"""
import argparse
import csv
import re
from collections import Counter, defaultdict
from pathlib import Path

from failure_test import classify_exceptions

TRIGGER_HEADER_RE = re.compile(r"^---\s*(\S+)")
EXCEPTION_CLASS_RE = re.compile(r"^([\w.$]+)")


def iter_active_bugs(project_dir: Path):
    """Yield bug ids for a project, restricted to its active-bugs.csv."""
    csv_path = project_dir / "active-bugs.csv"
    if not csv_path.exists():
        return
    with open(csv_path, newline="") as fh:
        for row in csv.DictReader(fh):
            yield row["bug.id"]


def classify_trigger_file(path: Path) -> str:
    """Return "semantic", "crashing", or "unknown" for one bug's
    trigger_tests file, via the same crashing/semantic reduction
    `failure_test.classify_exceptions` uses for the `defects4j info`
    path — the two data sources agree on triggers but format them
    differently, so only the parsing here is bespoke.

    A trigger_tests file holds one or more "--- test::method" blocks; the line
    right after each header is the exception that failed that test, e.g.
    "junit.framework.AssertionFailedError: expected:<1> but was:<2>" or
    "java.lang.NullPointerException".
    """
    exceptions = []
    with open(path, encoding="utf-8", errors="replace") as fh:
        expect_exception = False
        for line in fh:
            if TRIGGER_HEADER_RE.match(line):
                expect_exception = True
                continue
            if not expect_exception:
                continue
            expect_exception = False
            m = EXCEPTION_CLASS_RE.match(line.strip())
            if m:
                exceptions.append(m.group(1))
    return classify_exceptions(exceptions)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--projects-dir",
        type=Path,
        default=Path(__file__).resolve().parent.parent.parent
        / "defects4j" / "framework" / "projects",
        help="Path to defects4j/framework/projects",
    )
    parser.add_argument("--csv", type=Path, help="Optional path to dump per-bug results as CSV")
    args = parser.parse_args()

    per_project = defaultdict(Counter)
    rows = []

    for project_dir in sorted(args.projects_dir.iterdir()):
        if not project_dir.is_dir():
            continue
        trigger_dir = project_dir / "trigger_tests"
        if not trigger_dir.exists():
            continue
        for bug_id in iter_active_bugs(project_dir):
            trigger_file = trigger_dir / bug_id
            if not trigger_file.exists():
                continue
            verdict = classify_trigger_file(trigger_file)
            per_project[project_dir.name][verdict] += 1
            rows.append((project_dir.name, bug_id, verdict))

    total = Counter()
    print(f"{'Project':<16}{'Semantic':>10}{'Crashing':>10}{'Unknown':>10}{'Total':>8}")
    print("-" * 54)
    for project, counts in sorted(per_project.items()):
        s, c, u = counts["semantic"], counts["crashing"], counts["unknown"]
        print(f"{project:<16}{s:>10}{c:>10}{u:>10}{s + c + u:>8}")
        total.update(counts)

    print("-" * 54)
    s, c, u = total["semantic"], total["crashing"], total["unknown"]
    print(f"{'TOTAL':<16}{s:>10}{c:>10}{u:>10}{s + c + u:>8}")

    if args.csv:
        with open(args.csv, "w", newline="") as fh:
            writer = csv.writer(fh)
            writer.writerow(["project", "bug_id", "classification"])
            writer.writerows(rows)
        print(f"\nPer-bug results written to {args.csv}")


if __name__ == "__main__":
    main()
