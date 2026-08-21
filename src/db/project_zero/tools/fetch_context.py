#!/usr/bin/env python3
"""
Fetch the source of the files each Project Zero fix touches, at that fix's own
commit. Port of linux_kernel/tools/fetch_context.py, with the same output
layout, so the same downstream tooling shape applies:

  pairs/<PAIR>/fix0_context/<path/to/file.cc>   source at the fix0 commit
  pairs/<PAIR>/fix1_context/<path/to/file.cc>   source at the fix1 commit

The Linux twin has one host. This dataset spans six raw-file endpoints, and
one of them is unreachable by path at all. `raw_url` is the whole difference.

SCOPE: only the directly modified files. That is enough for a one-shot LLM
judgement. It is NOT enough for harness generation or fuzzing, which need the
whole tree, the transitive headers and the build system. Chromium's tree alone
is 61 GB, so no full checkout happens here.

WHY A COMMIT ID CAN STOP A PAIR. A raw-file endpoint addresses a file at a
commit, so it needs a git SHA. 21 of the 43 `fix0_commit` values in this
dataset are Gerrit change numbers (`CL/1888103`), and 13 of the `fix1_commit`
values are. A CL number is not a commit, so `resolve_gerrit.py` has to run
first for those pairs. That single fact, not repository size, sets which pairs
are reachable today.

TIERS.

  --tier 1    (default) both sides carry a SHA, both hosts are supported, and
              the repository is not on EXCLUDED_REPOS. 14 pairs.
  --tier all  every pair whose host is supported and whose commit is a SHA.

`chromium/src` sits on EXCLUDED_REPOS. Its trees are the largest here, and 11
of its 13 `fix0` ids are CL numbers anyway.

Usage, from src/db/project_zero/:
    python tools/fetch_context.py --dry_run
    python tools/fetch_context.py
    python tools/fetch_context.py --tier all --force
    python tools/fetch_context.py --pair CVE-2021-30551__CVE-2022-1096
"""

import argparse
import base64
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

DEFAULT_PAIRS = Path(__file__).parent.parent / "pairs"
# Written by resolve_gerrit.py: {"CL/1888103": "<sha>"}. Absent until that
# tool runs, and then every CL it holds becomes fetchable.
GERRIT_RESOLVED = Path(__file__).parent.parent / "gerrit_resolved.json"
REQUEST_DELAY = 0.4
USER_AGENT = "vuln-patch/1.0 (project-zero context fetch)"

# Caps. One fix in this dataset touches 89 files, and a prompt cannot carry
# that. Both caps are reported per pair rather than applied silently.
MAX_FILES = 15
MAX_BYTES = 1_000_000

# Repositories left out of tier 1. Not a correctness rule — a cost one.
EXCLUDED_REPOS = ("chromium/src",)

# Read-only mirrors, used when the canonical host does not answer. A git
# mirror shares the object ids of its origin, so a SHA addresses the same file
# in both. git.savannah.gnu.org does not answer from every network.
MIRRORS = {
    "git.savannah.gnu.org/git/freetype/freetype2.git":
        "https://github.com/freetype/freetype",
}

# Same rule as changed_files() in baseline_llmjudge/project_zero/firewall.py.
# The two must agree, or the renderer looks for a file this tool never wrote.
_CHANGED_FILE = re.compile(r"^diff --git a/(.*?) b/", re.M)

_SHA = re.compile(r"^[0-9a-f]{7,40}$")


def is_sha(commit: str) -> bool:
    """True for a git SHA. False for a Gerrit change number or an empty id."""
    return bool(commit) and bool(_SHA.match(str(commit)))


def load_gerrit_resolved(path: Path = GERRIT_RESOLVED) -> dict[str, str]:
    """`{CL id: SHA}` from resolve_gerrit.py, or `{}` when it has not run."""
    if not path.exists():
        return {}
    return json.loads(path.read_text())


def commit_sha(commit: str, resolved: dict[str, str]) -> str | None:
    """The SHA to fetch at, or None when the id cannot become one.

    A `CL/<n>` id is a Gerrit change number, not a commit. It becomes
    fetchable once resolve_gerrit.py has written its SHA."""
    commit = str(commit or "")
    if is_sha(commit):
        return commit
    return resolved.get(commit)


def parse_changed_files(patch_text: str) -> list[str]:
    return _CHANGED_FILE.findall(patch_text)


# --- the raw-file endpoint of each host --------------------------------------

def raw_url(repo_url: str, commit: str, filepath: str) -> str | None:
    """The URL that serves one file at one commit, or None for no route.

    Gitiles answers in base64; every other host answers in plain text. The
    caller reads `is_gitiles` to know which."""
    repo = (repo_url or "").rstrip("/")
    for canonical, mirror in MIRRORS.items():
        if canonical in repo:
            repo = mirror
            break

    # Gitiles: chromium, android, skia. A repo_url copied from the Gerrit UI
    # carries a `/c/` segment after the host, which is not part of the path.
    if ".googlesource.com" in repo:
        repo = repo.replace(".googlesource.com/c/", ".googlesource.com/")
        return f"{repo}/+/{commit}/{filepath}?format=TEXT"

    # cgit on kernel.org: the browse path is the repo path itself.
    if "git.kernel.org" in repo:
        return f"{repo}/plain/{filepath}?id={commit}"

    # cgit on savannah: the browse path replaces /git/ with /cgit/.
    if "git.savannah.gnu.org" in repo:
        repo = repo.replace("/git/", "/cgit/")
        return f"{repo}/plain/{filepath}?id={commit}"

    # Mercurial.
    if "hg.mozilla.org" in repo:
        return f"{repo}/raw-file/{commit}/{filepath}"

    # GitHub, through the raw host.
    m = re.match(r"https?://github\.com/([^/]+)/([^/]+)", repo)
    if m:
        owner, name = m.group(1), m.group(2).removesuffix(".git")
        return (f"https://raw.githubusercontent.com/{owner}/{name}/"
                f"{commit}/{filepath}")

    # GitLab.
    if "git.codelinaro.org" in repo or "gitlab" in repo:
        return f"{repo}/-/raw/{commit}/{filepath}"

    # Bugzilla holds attachments, not commits. There is no path-addressed
    # route, so the four Bugzilla pairs stay out under this design.
    return None


def is_gitiles(repo_url: str) -> bool:
    return ".googlesource.com" in (repo_url or "")


def fetch_file(repo_url: str, commit: str, filepath: str,
               timeout: int = 30) -> str | None:
    url = raw_url(repo_url, commit, filepath)
    if url is None:
        return None
    try:
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read(MAX_BYTES + 1)
    except urllib.error.HTTPError as e:
        if e.code != 404:
            print(f"    HTTP {e.code} for {filepath}")
        return None
    except Exception as e:
        print(f"    Error fetching {filepath}: {e}")
        return None
    if len(raw) > MAX_BYTES:
        print(f"    SKIP {filepath} (over {MAX_BYTES} bytes)")
        return None
    if is_gitiles(repo_url):
        try:
            raw = base64.b64decode(raw)
        except Exception as e:
            print(f"    Error decoding {filepath}: {e}")
            return None
    return raw.decode("utf-8", errors="replace")


# --- one side of one pair ----------------------------------------------------

def fetch_side(pair_dir: Path, label: str, commit: str, repo_url: str,
               force: bool) -> dict[str, int]:
    """Fetch one fix's touched files. Returns a per-side tally."""
    tally = {"fetched": 0, "cached": 0, "failed": 0, "over_file_cap": 0}
    patch_path = pair_dir / f"{label}.patch"
    if not patch_path.exists():
        print(f"  SKIP  {pair_dir.name}/{label}  (no patch file)")
        return tally

    files = parse_changed_files(patch_path.read_text(errors="replace"))
    if not files:
        print(f"  SKIP  {pair_dir.name}/{label}  (no changed files in patch)")
        return tally
    if len(files) > MAX_FILES:
        print(f"  CAP   {pair_dir.name}/{label}  touches {len(files)} files, "
              f"cap is {MAX_FILES} — taking the first {MAX_FILES}")
        tally["over_file_cap"] = len(files) - MAX_FILES
        files = files[:MAX_FILES]

    context_dir = pair_dir / f"{label}_context"
    context_dir.mkdir(exist_ok=True)

    for filepath in files:
        dest = context_dir / filepath
        if dest.exists() and not force:
            tally["cached"] += 1
            continue
        content = fetch_file(repo_url, commit, filepath)
        if content is None:
            print(f"  FAIL  {pair_dir.name}/{label}/{filepath}")
            tally["failed"] += 1
        else:
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(content)
            print(f"  OK    {pair_dir.name}/{label}/{filepath}")
            tally["fetched"] += 1
        time.sleep(REQUEST_DELAY)
    return tally


# --- pair selection ---------------------------------------------------------

def read_pairs(pairs_dir: Path) -> list[tuple[Path, dict]]:
    """Every pair with a metadata file. One pair nests one level deeper."""
    out = []
    for meta_path in sorted(list(pairs_dir.glob("*/metadata.json"))
                            + list(pairs_dir.glob("*/*/metadata.json"))):
        out.append((meta_path.parent, json.loads(meta_path.read_text())))
    return out


def skip_reason(meta: dict, tier: str,
                resolved: dict[str, str]) -> str | None:
    """Why this pair cannot be fetched now, or None when it can."""
    sides = (("fix0_commit", "repo_url"), ("fix1_commit", "later_repo_url"))
    for commit_key, repo_key in sides:
        commit = str(meta.get(commit_key) or "")
        repo = meta.get(repo_key) or ""
        sha = commit_sha(commit, resolved)
        if sha is None:
            return (f"{commit_key} is not a SHA ({commit or 'empty'}) — "
                    f"run tools/resolve_gerrit.py"
                    if commit.startswith("CL/")
                    else f"{commit_key} is not a SHA ({commit or 'empty'})")
        if raw_url(repo, sha, "x") is None:
            return f"no raw-file route for {repo}"
        if tier == "1" and any(x in repo for x in EXCLUDED_REPOS):
            return f"{repo} is excluded from tier 1"
    return None


def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pairs_dir", type=Path, default=DEFAULT_PAIRS,
                    help=f"default: {DEFAULT_PAIRS}")
    ap.add_argument("--tier", default="1", choices=["1", "all"],
                    help="1 = SHA on both sides, supported host, not on "
                         "EXCLUDED_REPOS (default)")
    ap.add_argument("--pair", help="fetch this pair only, by directory name")
    ap.add_argument("--force", action="store_true",
                    help="re-fetch a file that is already on disk")
    ap.add_argument("--dry_run", action="store_true",
                    help="list the selected pairs and stop")
    args = ap.parse_args()

    pairs = read_pairs(args.pairs_dir)
    if args.pair:
        pairs = [(d, m) for d, m in pairs
                 if str(d.relative_to(args.pairs_dir)) == args.pair]
        if not pairs:
            print(f"no pair named {args.pair!r}")
            return 2

    resolved = load_gerrit_resolved()
    selected, skipped = [], []
    for pair_dir, meta in pairs:
        reason = skip_reason(meta, args.tier, resolved)
        if reason and not args.pair:
            skipped.append((pair_dir, reason))
        else:
            selected.append((pair_dir, meta))

    print(f"pairs on disk : {len(pairs)}")
    print(f"tier          : {args.tier}")
    print(f"gerrit resolved: {len(resolved)} change(s)")
    print(f"selected      : {len(selected)}")
    print(f"skipped       : {len(skipped)}")
    for pair_dir, reason in skipped:
        print(f"  - {pair_dir.name}: {reason}")
    if args.dry_run:
        print("\n--dry_run — no request made.")
        for pair_dir, _ in selected:
            print(f"  would fetch {pair_dir.name}")
        return 0

    total = {"fetched": 0, "cached": 0, "failed": 0, "over_file_cap": 0}
    for pair_dir, meta in selected:
        print(f"\n{pair_dir.name}")
        for label, commit_key, repo_key in (
                ("fix0", "fix0_commit", "repo_url"),
                ("fix1", "fix1_commit", "later_repo_url")):
            sha = commit_sha(meta.get(commit_key) or "", resolved)
            if sha is None:
                print(f"  SKIP  {pair_dir.name}/{label}  "
                      f"({commit_key} is not a SHA)")
                continue
            tally = fetch_side(pair_dir, label, sha,
                               meta.get(repo_key) or "", args.force)
            for k in total:
                total[k] += tally[k]

    print("\n" + "=" * 60)
    print(f"files fetched     : {total['fetched']}")
    print(f"files already held: {total['cached']}")
    print(f"files failed      : {total['failed']}")
    print(f"files over the cap: {total['over_file_cap']} "
          f"(cap {MAX_FILES} per fix)")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
