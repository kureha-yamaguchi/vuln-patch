#!/usr/bin/env python3
"""
Resolve a Gerrit change number to the git SHA it merged as.

WHY THIS EXISTS. A raw-file endpoint addresses a file at a commit, so it needs
a SHA. 21 of the 43 `fix0_commit` values in this dataset are Gerrit change
numbers (`CL/1888103`), and 13 of the `fix1_commit` values are. Those pairs are
unreachable by fetch_context.py until a SHA is known. That single fact, not
repository size, is what caps the fetchable population at 14 pairs.

WHAT IT WRITES, AND WHAT IT DOES NOT TOUCH. The result goes to a separate
override file:

    src/db/project_zero/gerrit_resolved.json      {"CL/1888103": "<sha>", ...}

`metadata.json` is never rewritten. Two reasons, and the second one matters
more than tidiness:

  1. The dataset stays as the harvester produced it, so a resolution can be
     re-run or discarded.
  2. `baseline_llmjudge/project_zero/firewall.py` derives `fix_id` from the
     commit id it reads in `metadata.json`. If a resolution changed that id,
     every fix id would change with it, and `bug_kind.jsonl` would stop to
     match. So the resolution stays out of the metadata, and the ids stay
     stable.

So this tool serves the fetch step alone.

SCOPE. `--only_repo` defaults to `v8/v8`. v8 is the largest repository this
project still fetches from at file level, and it carries the most Gerrit-only
pairs. `chromium/src` is left out by default for the same cost reason
fetch_context.py excludes it from tier 1.

Usage, from src/db/project_zero/:
    python tools/resolve_gerrit.py --dry_run
    python tools/resolve_gerrit.py
    python tools/resolve_gerrit.py --only_repo '' --force   # every repository
"""

import argparse
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

DEFAULT_PAIRS = Path(__file__).parent.parent / "pairs"
DEFAULT_OUT = Path(__file__).parent.parent / "gerrit_resolved.json"

GERRIT_HOST = "https://chromium-review.googlesource.com"
REQUEST_DELAY = 0.4
USER_AGENT = "vuln-patch/1.0 (project-zero gerrit resolve)"

# Gerrit prefixes every JSON body with this line, to defeat cross-site script
# inclusion. It has to come off before the body parses.
_XSSI_PREFIX = ")]}'"

_CL = re.compile(r"^CL/(\d+)$")


def change_number(commit: str) -> str | None:
    """The change number of a `CL/<n>` id, or None for anything else."""
    m = _CL.match(str(commit or ""))
    return m.group(1) if m else None


def resolve(number: str, timeout: int = 30) -> str | None:
    """The merged SHA of one change, or None when Gerrit does not give one.

    `current_revision` is the change's latest patch set. For a merged change
    that is the commit that landed."""
    url = f"{GERRIT_HOST}/changes/{number}/?o=CURRENT_REVISION"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        print(f"    HTTP {e.code} for change {number}")
        return None
    except Exception as e:
        print(f"    Error resolving change {number}: {e}")
        return None
    if body.startswith(_XSSI_PREFIX):
        body = body[len(_XSSI_PREFIX):]
    try:
        data = json.loads(body)
    except json.JSONDecodeError as e:
        print(f"    Cannot parse the reply for change {number}: {e}")
        return None
    sha = data.get("current_revision")
    status = data.get("status")
    if status != "MERGED":
        print(f"    change {number} is {status}, not MERGED — skipping")
        return None
    return sha


def read_pairs(pairs_dir: Path) -> list[tuple[Path, dict]]:
    out = []
    for meta_path in sorted(list(pairs_dir.glob("*/metadata.json"))
                            + list(pairs_dir.glob("*/*/metadata.json"))):
        out.append((meta_path.parent, json.loads(meta_path.read_text())))
    return out


def wanted(pairs: list[tuple[Path, dict]], only_repo: str) -> dict[str, str]:
    """`{CL id: repository}` for every unresolved change worth resolving."""
    out: dict[str, str] = {}
    for _, meta in pairs:
        for commit_key, repo_key in (("fix0_commit", "repo_url"),
                                     ("fix1_commit", "later_repo_url")):
            commit = str(meta.get(commit_key) or "")
            repo = meta.get(repo_key) or ""
            if not change_number(commit):
                continue
            if only_repo and only_repo not in repo:
                continue
            out[commit] = repo
    return out


def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pairs_dir", type=Path, default=DEFAULT_PAIRS)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT,
                    help=f"default: {DEFAULT_OUT}")
    ap.add_argument("--only_repo", default="v8/v8",
                    help="substring of repo_url to accept; '' means every "
                         "repository (default: v8/v8)")
    ap.add_argument("--force", action="store_true",
                    help="re-resolve a change already in the output file")
    ap.add_argument("--dry_run", action="store_true",
                    help="list what would be resolved and stop")
    args = ap.parse_args()

    known = json.loads(args.out.read_text()) if args.out.exists() else {}
    todo = wanted(read_pairs(args.pairs_dir), args.only_repo)
    if not args.force:
        todo = {cl: repo for cl, repo in todo.items() if cl not in known}

    print(f"already resolved : {len(known)}")
    print(f"to resolve       : {len(todo)}")
    for cl, repo in sorted(todo.items()):
        print(f"  {cl}  {repo}")
    if args.dry_run:
        print("\n--dry_run — no request made.")
        return 0

    resolved = 0
    for cl in sorted(todo):
        sha = resolve(change_number(cl))
        if sha:
            known[cl] = sha
            resolved += 1
            print(f"  OK   {cl} -> {sha}")
        else:
            print(f"  FAIL {cl}")
        time.sleep(REQUEST_DELAY)

    args.out.write_text(json.dumps(dict(sorted(known.items())), indent=2)
                        + "\n")
    print(f"\nresolved {resolved} of {len(todo)}")
    print(f"Wrote {args.out}")
    print("Now re-run: python tools/fetch_context.py --tier all")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
