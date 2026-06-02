#!/usr/bin/env python3
"""
Check out the Linux kernel at three states for one or all CVE pairs,
producing independent working trees for building and fuzzing.

Three states per pair (all use fix0_parent_commit / fix0_commit / fix1_commit
from metadata.json):

  fix0_parent/ → unpatched baseline  → harness MUST crash   (harness is valid)
  fix0/        → incomplete fix      → harness MUST crash   (patch incomplete)
  fix1/        → corrective fix      → harness must NOT crash (patch correct)

Storage strategy — shared repo + git worktrees:
  Cloning the kernel 78 times (26 pairs × 3 states) separately would be huge.
  Instead, one blobless bare clone (~400MB) is shared, and each checkout is a
  lightweight worktree (~1GB source files) on top of it.

  Set LINUX_KERNEL_REPO to reuse an existing kernel bare repo across runs.

Usage:
    python checkout_pair.py --pair CVE-2011-1017__CVE-2011-2182
    python checkout_pair.py --all
    python checkout_pair.py --pair CVE-2016-9576__CVE-2016-10088 --only fix0_parent
    python checkout_pair.py --all --dest /data/checkouts --kernel-repo /data/linux.git

Environment:
    LINUX_KERNEL_REPO   Path to shared bare kernel repo (overrides --kernel-repo default)
"""

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

KERNEL_REMOTE = "https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git"
DEFAULT_KERNEL_REPO = Path(os.environ.get("LINUX_KERNEL_REPO", "/tmp/linux-kernel-shared.git"))
DEFAULT_DEST_BASE = Path("/tmp/cve_sibling_checkouts")


def run(cmd: list[str], cwd: Path | None = None, check: bool = True) -> subprocess.CompletedProcess:
    print(f"  $ {' '.join(str(c) for c in cmd)}")
    return subprocess.run(cmd, cwd=cwd, check=check)


def ensure_shared_repo(kernel_repo: Path) -> None:
    """Blobless bare clone of the kernel if not already present."""
    if (kernel_repo / "HEAD").exists():
        print(f"Shared kernel repo: {kernel_repo}")
        return
    print(f"Cloning kernel (blobless bare) into {kernel_repo} — takes a few minutes...")
    kernel_repo.mkdir(parents=True, exist_ok=True)
    run(["git", "clone", "--filter=blob:none", "--bare", KERNEL_REMOTE, str(kernel_repo)])


def have_commit(kernel_repo: Path, commit: str) -> bool:
    r = subprocess.run(
        ["git", "cat-file", "-t", commit],
        cwd=kernel_repo, capture_output=True, text=True,
    )
    return r.returncode == 0 and r.stdout.strip() == "commit"


def fetch_commit(kernel_repo: Path, commit: str) -> bool:
    if have_commit(kernel_repo, commit):
        return True
    try:
        run(["git", "fetch", "--filter=blob:none", "--depth=1", "origin", commit], cwd=kernel_repo)
        return True
    except subprocess.CalledProcessError:
        print(f"  Failed to fetch {commit[:12]}", file=sys.stderr)
        return False


def add_worktree(kernel_repo: Path, commit: str, dest: Path) -> bool:
    if dest.exists() and any(dest.iterdir()):
        print(f"  Worktree exists at {dest}, skipping  (remove to redo)")
        return True
    dest.parent.mkdir(parents=True, exist_ok=True)
    try:
        run(["git", "worktree", "add", "--detach", str(dest), commit], cwd=kernel_repo)
        return True
    except subprocess.CalledProcessError as e:
        print(f"  Failed to create worktree: {e}", file=sys.stderr)
        return False


def checkout_pair(entry: dict, kernel_repo: Path, dest_base: Path, only: str | None) -> bool:
    prior = entry["prior_cve"]
    later = entry["later_cve"]
    pair_name = f"{prior}__{later}"
    dest = dest_base / pair_name
    ok = True

    states = [
        ("fix0_parent", "fix0_parent_commit"),
        ("fix0",        "fix0_commit"),
        ("fix1",        "fix1_commit"),
    ]
    for label, commit_key in states:
        if only is not None and only != label:
            continue

        commit = entry.get(commit_key)
        if not commit:
            note = entry.get("fix1_commit_note", "no commit hash")
            print(f"  SKIP {pair_name}/{label} — {note[:60]}")
            continue

        print(f"\n--- {pair_name}/{label} ({commit[:12]}) ---")
        if fetch_commit(kernel_repo, commit):
            ok &= add_worktree(kernel_repo, commit, dest / label)
        else:
            ok = False

    return ok


def load_entries(script_dir: Path, pair_name: str | None) -> list[dict]:
    if pair_name:
        meta_path = script_dir / pair_name / "metadata.json"
        if not meta_path.exists():
            print(f"No metadata at {meta_path} — run fetch_patches.py first", file=sys.stderr)
            sys.exit(1)
        with meta_path.open() as f:
            return [json.load(f)]

    entries = []
    for meta_path in sorted(script_dir.glob("CVE-*__CVE-*/metadata.json")):
        with meta_path.open() as f:
            entries.append(json.load(f))
    print(f"Found {len(entries)} pairs in {script_dir}")
    return entries


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    target = parser.add_mutually_exclusive_group(required=True)
    target.add_argument("--pair", help="Single pair, e.g. CVE-2011-1017__CVE-2011-2182")
    target.add_argument("--all", action="store_true", help="Check out all pairs in this DB")
    parser.add_argument(
        "--kernel-repo", type=Path, default=DEFAULT_KERNEL_REPO,
        help=f"Shared bare kernel repo (default: {DEFAULT_KERNEL_REPO})",
    )
    parser.add_argument(
        "--dest", type=Path, default=DEFAULT_DEST_BASE,
        help=f"Parent directory for worktrees (default: {DEFAULT_DEST_BASE})",
    )
    parser.add_argument("--only", choices=["fix0_parent", "fix0", "fix1"],
                        help="Check out only one state")
    args = parser.parse_args()

    script_dir = Path(__file__).parent
    entries = load_entries(script_dir, args.pair if not args.all else None)

    ensure_shared_repo(args.kernel_repo)

    all_ok = True
    for entry in entries:
        all_ok &= checkout_pair(entry, args.kernel_repo, args.dest, args.only)

    if all_ok:
        print(f"\nWorktrees ready under {args.dest}/")
        print("  <pair>/fix0/  — incomplete fix state (harness should still trigger)")
        print("  <pair>/fix1/  — corrective fix state (harness should not trigger)")
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
