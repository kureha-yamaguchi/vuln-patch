#!/usr/bin/env python3
"""
For each pair, parse fix0.patch and fix1.patch to identify changed source files,
then fetch those files from kernel.org at the corresponding commits.

Stores files in:
  <pair>/fix0_context/<path/to/file.c>   — source at fix0 state
  <pair>/fix1_context/<path/to/file.c>   — source at fix1 state

SCOPE: Only the directly modified files are fetched. This is useful for
browsing the dataset and lightweight LLM prompting, but it is NOT sufficient
for harness generation or fuzzing — those require the full codebase including
transitive headers, build system, and call-graph tools (fuzz-introspector).

For a full buildable kernel checkout at fix0/fix1, use checkout_pair.py.

Usage:
    python fetch_context.py
    python fetch_context.py --pair CVE-2011-1017__CVE-2011-2182
    python fetch_context.py --force
"""

import argparse
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

KERNEL_PLAIN_URL = (
    "https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git"
    "/plain/{filepath}?id={commit}"
)
# See linux_kernel/README.md for the directory layout: this file lives
# in linux_kernel/tools/ and writes per-pair context into the sibling
# linux_kernel/pairs/ directory.
DEFAULT_OUT = Path(__file__).parent.parent / "pairs"
REQUEST_DELAY = 0.4


def parse_changed_files(patch_text: str) -> list[str]:
    return re.findall(r"^diff --git a/(.*) b/", patch_text, re.MULTILINE)


def fetch_file(filepath: str, commit: str, timeout: int = 30) -> str | None:
    url = KERNEL_PLAIN_URL.format(filepath=filepath, commit=commit)
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "cve-sibling-db/1.0"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        if e.code != 404:
            print(f"    HTTP {e.code} for {filepath}")
        return None
    except Exception as e:
        print(f"    Error fetching {filepath}: {e}")
        return None


def fetch_context_for_label(
    pair_dir: Path,
    label: str,
    commit: str,
    force: bool,
) -> dict[str, str]:
    patch_path = pair_dir / f"{label}.patch"
    if not patch_path.exists():
        print(f"  SKIP  {pair_dir.name}/{label}  (no patch file — run fetch_patches.py first)")
        return {}

    patch_text = patch_path.read_text()
    changed_files = parse_changed_files(patch_text)
    if not changed_files:
        print(f"  SKIP  {pair_dir.name}/{label}  (no changed files found in patch)")
        return {}

    context_dir = pair_dir / f"{label}_context"
    context_dir.mkdir(exist_ok=True)

    results = {}
    for filepath in changed_files:
        dest = context_dir / filepath
        if dest.exists() and not force:
            results[filepath] = "exists"
            continue

        dest.parent.mkdir(parents=True, exist_ok=True)
        content = fetch_file(filepath, commit)
        if content:
            dest.write_text(content)
            print(f"  OK    {pair_dir.name}/{label}/{filepath}")
            results[filepath] = "fetched"
        else:
            print(f"  FAIL  {pair_dir.name}/{label}/{filepath}")
            results[filepath] = "failed"
        time.sleep(REQUEST_DELAY)

    return results


def process_pair(pair_dir: Path, force: bool) -> None:
    meta_path = pair_dir / "metadata.json"
    if not meta_path.exists():
        return

    with meta_path.open() as f:
        entry = json.load(f)

    fix0_commit = entry.get("fix0_commit")
    fix1_commit = entry.get("fix1_commit")

    if fix0_commit:
        fetch_context_for_label(pair_dir, "fix0", fix0_commit, force)
    if fix1_commit:
        fetch_context_for_label(pair_dir, "fix1", fix1_commit, force)


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        "--out", type=Path, default=DEFAULT_OUT,
        help="linux_kernel/pairs directory (default: the sibling pairs/ dir)",
    )
    parser.add_argument("--force", action="store_true", help="Re-fetch even if files exist")
    parser.add_argument("--pair", help="Process only this pair, e.g. CVE-2011-1017__CVE-2011-2182")
    args = parser.parse_args()

    if args.pair:
        pair_dirs = [args.out / args.pair]
    else:
        pair_dirs = sorted(
            p for p in args.out.iterdir()
            if p.is_dir() and p.name.startswith("CVE-")
        )

    for pair_dir in pair_dirs:
        process_pair(pair_dir, args.force)


if __name__ == "__main__":
    main()
