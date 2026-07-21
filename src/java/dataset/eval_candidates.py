"""Enumerate candidate patches for expanding the semantic eval set.

The current 8-bug semantic set quantizes recall at 12.5%/bug and contains
two provably-undetectable overfits, so no technique change can be measured
on it. Expansion step 1 (this tool, cheap): walk the DRR patch dirs and
list every (project, bug, tool, label) whose bug classifies as SEMANTIC —
no checkout, no LLM, just `defects4j info` per (project, bug), cached.
Step 2 (certify_detectability.py, per-candidate compute): keep only
overfits that are behaviorally distinguishable from the developer fix.

Usage (on the VM, from src/):
  uv run python java/eval_candidates.py --out candidates.jsonl \
      [--projects Chart Closure Lang Math Time] [--kind semantic]
"""
import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config  # noqa: E402
from failure_test import classify_bug_kind  # noqa: E402


def parse_args():
    p = argparse.ArgumentParser(
        description="List DRR patches whose bug matches a given kind.")
    p.add_argument("--out", required=True, help="output JSONL")
    p.add_argument("--projects", nargs="*",
                   default=["Chart", "Closure", "Lang", "Math", "Time"])
    p.add_argument("--kind", default="semantic",
                   choices=["semantic", "crashing", "any"],
                   help="bug kind to keep (default: semantic)")
    return p.parse_args()


def main():
    args = parse_args()
    kind_cache = {}
    n_out = 0
    with open(args.out, "w", encoding="utf-8") as out:
        for label, root in (("overfitting", config.DRR_OVERFITTING_DIR),
                            ("correct", config.DRR_CORRECT_DIR)):
            if not os.path.isdir(root):
                print(f"missing patch root {root}; skipping {label}")
                continue
            for tool in sorted(os.listdir(root)):
                tool_dir = os.path.join(root, tool)
                if not os.path.isdir(tool_dir):
                    continue
                for proj in args.projects:
                    pdir = os.path.join(tool_dir, proj)
                    if not os.path.isdir(pdir):
                        continue
                    for fname in sorted(os.listdir(pdir)):
                        parts = fname.split("-")
                        # patch1-Math-5-SimFix.patch
                        if len(parts) < 3:
                            continue
                        bug_id = parts[2]
                        key = (proj, bug_id)
                        if key not in kind_cache:
                            try:
                                kind_cache[key] = classify_bug_kind(
                                    proj, bug_id)
                            except Exception as e:
                                print(f"  {proj}-{bug_id}: classify failed "
                                      f"({e}); skipping")
                                kind_cache[key] = None
                        kind = kind_cache[key]
                        if kind is None:
                            continue
                        if args.kind != "any" and kind != args.kind:
                            continue
                        out.write(json.dumps({
                            "project": proj,
                            "bug_id": bug_id,
                            "apr_tool": tool,
                            "label": label,
                            "bug_kind": kind,
                            "patch_path": os.path.join(pdir, fname),
                        }) + "\n")
                        n_out += 1
    kinds = {}
    for k, v in kind_cache.items():
        kinds[v] = kinds.get(v, 0) + 1
    print(f"wrote {n_out} candidate patches to {args.out} "
          f"(distinct bugs classified: {kinds})")


if __name__ == "__main__":
    main()
