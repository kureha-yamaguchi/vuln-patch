#!/usr/bin/env python3
"""
Populate cve_sibling_db/ with per-pair directories containing:
  metadata.json  — structured entry from the seeds file
  fix0.patch     — diff of the incomplete fix
  fix1.patch     — diff of the corrective fix

Fetches patches from kernel.org. Idempotent: skips files that already exist
unless --force is given.

Usage:
    python fetch_patches.py
    python fetch_patches.py --seeds ../cve_scan_out/liu_seeds.json
    python fetch_patches.py --force        # re-download everything
    python fetch_patches.py --pair CVE-2011-1017__CVE-2011-2182  # one pair only
"""

import argparse
import json
import time
import urllib.error
import urllib.request
from pathlib import Path

KERNEL_PATCH_URL = (
    "https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git"
    "/patch/?id={commit}"
)
DEFAULT_SEEDS = Path(__file__).parent.parent / "cve_scan_out" / "liu_seeds_linux.json"
DEFAULT_OUT = Path(__file__).parent
REQUEST_DELAY = 0.4  # seconds between kernel.org requests


def fetch_patch(commit: str, timeout: int = 30) -> str | None:
    url = KERNEL_PATCH_URL.format(commit=commit)
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "cve-sibling-db/1.0"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            content = resp.read().decode("utf-8", errors="replace")
            return content if content.strip().startswith("From ") else None
    except urllib.error.HTTPError as e:
        print(f"    HTTP {e.code} for {commit[:12]}")
        return None
    except Exception as e:
        print(f"    Error fetching {commit[:12]}: {e}")
        return None


def process_entry(entry: dict, out_dir: Path, force: bool) -> dict[str, str]:
    prior = entry["prior_cve"]
    later = entry["later_cve"]
    pair_name = f"{prior}__{later}"
    pair_dir = out_dir / pair_name
    pair_dir.mkdir(parents=True, exist_ok=True)

    results = {"pair": pair_name, "fix0": "skip", "fix1": "skip"}

    meta_path = pair_dir / "metadata.json"
    if not meta_path.exists() or force:
        meta_path.write_text(json.dumps(entry, indent=2))

    for label, commit_key, patch_name in [
        ("fix0", "fix0_commit", "fix0.patch"),
        ("fix1", "fix1_commit", "fix1.patch"),
    ]:
        commit = entry.get(commit_key)
        patch_path = pair_dir / patch_name

        if not commit:
            note = entry.get("fix1_commit_note", "no commit hash")
            print(f"  SKIP  {pair_name}/{label}  ({note[:60]})")
            results[label] = "no_commit"
            continue

        if patch_path.exists() and not force:
            results[label] = "exists"
            continue

        patch = fetch_patch(commit)
        if patch:
            patch_path.write_text(patch)
            print(f"  OK    {pair_name}/{label}")
            results[label] = "fetched"
        else:
            print(f"  FAIL  {pair_name}/{label}  ({commit[:12]})")
            results[label] = "failed"

        time.sleep(REQUEST_DELAY)

    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--seeds", type=Path, default=DEFAULT_SEEDS, help="Path to seeds JSON file")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="Output directory (default: this script's directory)")
    parser.add_argument("--force", action="store_true", help="Re-download even if patch already exists")
    parser.add_argument("--pair", help="Process only this pair, e.g. CVE-2011-1017__CVE-2011-2182")
    args = parser.parse_args()

    with args.seeds.open() as f:
        entries = json.load(f)

    if args.pair:
        prior, later = args.pair.split("__")
        entries = [e for e in entries if e["prior_cve"] == prior and e["later_cve"] == later]
        if not entries:
            print(f"Pair {args.pair} not found in {args.seeds}")
            return

    counts = {"fetched": 0, "failed": 0, "exists": 0, "no_commit": 0}
    for entry in entries:
        r = process_entry(entry, args.out, args.force)
        for v in [r["fix0"], r["fix1"]]:
            if v in counts:
                counts[v] += 1

    print(f"\nfetched={counts['fetched']}  failed={counts['failed']}  "
          f"already_exists={counts['exists']}  no_commit={counts['no_commit']}")


if __name__ == "__main__":
    main()
