#!/usr/bin/env python3
"""
Enrich each pair's metadata.json with fields derivable from patch text and git history.

Fields added:
  affected_files_fix0      paths changed in Fix-0 (from patch diff headers)
  affected_functions_fix0  functions patched in Fix-0 (from patch hunk headers)
  kernel_subsystem         top-level subsystem (first 2 path components of primary file)
  fix0_parent_commit       SHA of Fix-0's parent — the unpatched baseline state
  fuzzing_excluded         true if fix1_commit is null (pipeline should skip this pair)
  required_kconfig         CONFIG_ symbols mentioned in Fix-0 patch (best-effort, [] if none)

fix0_parent_commit is fetched from the GitHub torvalds/linux mirror API (no local clone needed).
All other fields are derived locally from existing patch files.

Usage:
    python3 enrich_metadata.py
    python3 enrich_metadata.py --pair CVE-2011-1017__CVE-2011-2182
    python3 enrich_metadata.py --dry-run
    python3 enrich_metadata.py --token ghp_xxxx   # avoid GitHub rate limits
"""

import argparse
import json
import os
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

GITHUB_API = "https://api.github.com/repos/torvalds/linux/commits/{sha}"
DEFAULT_DIR = Path(__file__).parent.parent / "pairs"  # see linux_kernel/README.md
REQUEST_DELAY = 1.2  # stay well under 60 req/hour unauthenticated


def parse_affected_files(patch_text: str) -> list[str]:
    return re.findall(r"^diff --git a/(.*) b/", patch_text, re.MULTILINE)


def parse_affected_functions(patch_text: str) -> list[str]:
    """Extract function names from @@ hunk header context lines.

    Hunk headers look like:
        @@ -64,6 +64,7 @@ static int ldm_validate_partition_table(struct block_device *bdev)
    The text after the second @@ is the enclosing function/context.
    """
    raw = re.findall(r"^@@[^@]*@@[ \t]*(.+)$", patch_text, re.MULTILINE)
    # C type/storage keywords that may prefix function names in hunk headers.
    # \s* inside the group lets it consume whitespace between consecutive tokens.
    TYPE_PREFIX = re.compile(
        r"^(\s*(?:static|inline|__inline__|extern|noinline|notrace|"
        r"void|int|long|short|char|bool|unsigned|signed|"
        r"size_t|ssize_t|loff_t|u8|u16|u32|u64|s8|s16|s32|s64|"
        r"__u8|__u16|__u32|__u64|__s8|__s16|__s32|__s64|"
        r"struct|union|enum|const|volatile|__[a-z_]+)\s*)+"
    )
    seen: set[str] = set()
    funcs: list[str] = []
    for r in raw:
        # Take everything up to the first '(' or '{' to get the bare identifier
        name = re.split(r"[\({]", r.strip())[0].strip()
        name = TYPE_PREFIX.sub("", name).strip()
        # Skip C labels (end with ':'), comments, preprocessor, closing braces
        if not name or name.endswith(":") or name.startswith(("/*", "*", "#", "}")):
            continue
        # Must start with a valid C identifier character
        if not re.match(r"^[a-zA-Z_]", name):
            continue
        if name not in seen:
            seen.add(name)
            funcs.append(name)
    return funcs


def derive_subsystem(files: list[str]) -> str | None:
    if not files:
        return None
    parts = Path(files[0]).parts
    # Return up to 2 directory components, excluding the filename
    dirs = parts[:-1]
    return "/".join(dirs[:2]) if dirs else None


def extract_kconfig(patch_text: str) -> list[str]:
    return sorted(set(re.findall(r"\bCONFIG_[A-Z0-9_]+\b", patch_text)))


def fetch_parent_commit(sha: str, token: str | None, timeout: int = 20) -> str | None:
    url = GITHUB_API.format(sha=sha)
    headers = {
        "User-Agent": "cve-sibling-db/1.0",
        "Accept": "application/vnd.github+json",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read())
            parents = data.get("parents", [])
            return parents[0]["sha"] if parents else None
    except urllib.error.HTTPError as e:
        if e.code == 422:
            print(f"    GitHub 422: {sha[:12]} not found in torvalds/linux")
        elif e.code == 403:
            body = e.read().decode()
            if "rate limit" in body.lower():
                print(f"    GitHub rate limited — pass --token or wait")
            else:
                print(f"    GitHub 403 for {sha[:12]}")
        elif e.code == 404:
            print(f"    GitHub 404: {sha[:12]} not in torvalds/linux history")
        else:
            print(f"    GitHub HTTP {e.code} for {sha[:12]}")
        return None
    except Exception as e:
        print(f"    Error fetching parent for {sha[:12]}: {e}")
        return None


def enrich_pair(pair_dir: Path, token: str | None, dry_run: bool, force: bool) -> None:
    meta_path = pair_dir / "metadata.json"
    if not meta_path.exists():
        return

    with meta_path.open() as f:
        entry = json.load(f)

    patch_path = pair_dir / "fix0.patch"
    patch_text = patch_path.read_text() if patch_path.exists() else ""

    files = parse_affected_files(patch_text)
    funcs = parse_affected_functions(patch_text)
    subsystem = derive_subsystem(files)
    kconfig = extract_kconfig(patch_text)
    fix0_commit = entry.get("fix0_commit")

    # Fetch parent only if missing or --force
    if force or not entry.get("fix0_parent_commit"):
        parent = fetch_parent_commit(fix0_commit, token) if fix0_commit else None
        time.sleep(REQUEST_DELAY)
    else:
        parent = entry["fix0_parent_commit"]

    additions = {
        "affected_files_fix0":     files,
        "affected_functions_fix0": funcs,
        "kernel_subsystem":        subsystem,
        "fix0_parent_commit":      parent,
        "fuzzing_excluded":        entry.get("fix1_commit") is None,
        "required_kconfig":        kconfig,
    }

    changed = {k: v for k, v in additions.items() if entry.get(k) != v}
    if changed:
        print(f"  {pair_dir.name}")
        for k, v in changed.items():
            old = entry.get(k, "<absent>")
            print(f"    {k}: {old!r} → {v!r}")
    else:
        print(f"  {pair_dir.name}  (no changes)")

    if not dry_run:
        entry.update(additions)
        meta_path.write_text(json.dumps(entry, indent=2))


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--pair", help="Process only this pair, e.g. CVE-2011-1017__CVE-2011-2182")
    parser.add_argument("--dry-run", action="store_true", help="Print changes without writing")
    parser.add_argument("--force", action="store_true", help="Re-fetch even if fields already set")
    parser.add_argument(
        "--token",
        default=os.environ.get("GITHUB_TOKEN"),
        help="GitHub personal access token (default: $GITHUB_TOKEN)",
    )
    parser.add_argument("--dir", type=Path, default=DEFAULT_DIR)
    args = parser.parse_args()

    if args.pair:
        pair_dirs = [args.dir / args.pair]
    else:
        pair_dirs = sorted(args.dir.glob("CVE-*__CVE-*/"))

    if not args.token:
        print("Note: no GitHub token set — unauthenticated (60 req/hour limit). Pass --token or set $GITHUB_TOKEN.")

    for pair_dir in pair_dirs:
        enrich_pair(pair_dir, args.token, args.dry_run, args.force)

    if args.dry_run:
        print("\n(dry run — nothing written)")


if __name__ == "__main__":
    main()