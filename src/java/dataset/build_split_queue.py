import sys
from pathlib import Path
sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents if (p / 'config.py').exists())))
#!/usr/bin/env python3
"""Emit the run queue for one side of a frozen dev/holdout split.

`scripts/evaluate_crashing.sh` normally builds its queue by globbing drr/Patches
and taking a balanced random sample. That sample straddles dev and holdout, so
it cannot be used to tune without leaking the holdout. This script builds the
queue from the frozen split instead:

    suites/splits/<kind>_split.jsonl     -- which bugs are on which side
  x suites/labels/<kind>/verified_correct.jsonl  -- which patches are certified

The split file is READ, never regenerated. It is a frozen artifact (see
suites/splits/README.md and README_crashing.md); re-deriving it on every eval
run would let drift in logs/ or the label files silently reshuffle the holdout.

Only certified patches are queued: a patch listed in verified_incorrect.jsonl or
excluded.jsonl is dropped even when its bug is in the split, matching rule 1 of
make_crashing_split.py.

`--kind` selects the pool, and it defaults to `crashing` so every existing
caller keeps its old behaviour:

  * `crashing` -- suites/splits/crashing_split.jsonl with
    suites/labels/crashing/. Rows are filtered to kind == "crashing".
  * `semantic` -- suites/splits/semantic_split.jsonl with suites/labels/.
    Rows are filtered to kind == "semantic". The semantic label files also
    carry a few crashing and unknown rows, so the filter is not cosmetic.

Every queued bug therefore belongs to the requested kind by construction, and
no classify_bugs.py pre-filter is needed.

Output is one "<flag> <patch_path>" line per patch, the format the shell loop
reads: -c for a drr_label=="correct" patch, -o for "overfitting".

Usage:
    python3 build_split_queue.py --side dev|holdout [--kind crashing|semantic]
                                [--out PATH] [--projects "Lang Math"]
"""
import argparse
import json
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
SPLITS = REPO / "suites" / "splits"
LABELS_ROOT = REPO / "suites" / "labels"
PATCHES = REPO / "drr" / "Patches"

# One entry per bug kind: the frozen split file, and the directory holding the
# certification files for that pool. The semantic labels live at the root of
# suites/labels/ for historical reasons -- the crashing rows were moved into a
# subdirectory so the semantic lists stayed strictly semantic.
POOLS = {
    "crashing": (SPLITS / "crashing_split.jsonl", LABELS_ROOT / "crashing"),
    "semantic": (SPLITS / "semantic_split.jsonl", LABELS_ROOT),
}

# drr_label -> (class directory under drr/Patches, run.py flag)
LABEL_DIR = {"correct": ("Dcorrect", "-c"), "overfitting": ("Doverfitting", "-o")}


def load_jsonl(path):
    if not path.exists():
        return []
    with open(path) as fh:
        return [json.loads(line) for line in fh if line.strip()]


def patch_path(row):
    """drr/Patches/<class>/<apr_tool>/<project>/<patch file>."""
    class_dir, _ = LABEL_DIR[row["drr_label"]]
    return PATCHES / class_dir / row["apr_tool"] / row["project"] / row["patch"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--side", required=True, choices=["dev", "holdout"])
    ap.add_argument("--kind", default="crashing", choices=sorted(POOLS),
                    help="bug pool to queue (default: crashing)")
    ap.add_argument("--out", type=Path, help="write here instead of stdout")
    ap.add_argument("--projects", default="",
                    help="space-separated allow-list; default = every project in the split")
    args = ap.parse_args()

    split_file, labels = POOLS[args.kind]
    if not split_file.exists():
        raise SystemExit(f"FATAL: {split_file} missing — see suites/splits/")

    keep = set(args.projects.split()) if args.projects.strip() else None
    side_of = {}
    for row in load_jsonl(split_file):
        if keep is not None and row["project"] not in keep:
            continue
        side_of[(row["project"], row["bug_id"])] = row["side"]
    wanted = {bug for bug, side in side_of.items() if side == args.side}
    if not wanted:
        raise SystemExit(f"FATAL: no {args.side} bugs in {split_file}"
                         + (f" for projects {sorted(keep)}" if keep else ""))

    # Mislabels and exclusions lose their leg even when the bug is in the split.
    dropped = {
        (r["project"], r["bug_id"], r["patch"])
        for r in load_jsonl(labels / "verified_incorrect.jsonl") + load_jsonl(labels / "excluded.jsonl")
    }

    # One line per CERTIFIED PATCH, not one line per certification row. The
    # semantic file carries 30 rows that repeat a (project, bug, tool, patch)
    # already listed, and a repeated row would put the same patch in the queue
    # twice — counted twice in the confusion matrix, and paid for twice. The
    # crashing file has no repeats, so this changes nothing for that pool.
    lines, missing, legs, bugs_with_legs = [], [], Counter(), set()
    seen, repeats = set(), 0
    for row in load_jsonl(labels / "verified_correct.jsonl"):
        bug = (row["project"], row["bug_id"])
        if row.get("kind") != args.kind or bug not in wanted:
            continue
        if (row["project"], row["bug_id"], row["patch"]) in dropped:
            continue
        leg = (row["project"], row["bug_id"], row["apr_tool"], row["patch"],
               row["drr_label"])
        if leg in seen:
            repeats += 1
            continue
        seen.add(leg)
        path = patch_path(row)
        if not path.exists():
            missing.append(path)
            continue
        lines.append(f"{LABEL_DIR[row['drr_label']][1]} {path}")
        legs[row["drr_label"]] += 1
        bugs_with_legs.add(bug)

    # A certified leg named by the labels but absent on disk means the queue would
    # silently under-run the split. Fail instead: a short queue quietly changes the
    # denominator of every number the eval reports.
    if missing:
        raise SystemExit(
            f"FATAL: {len(missing)} certified patch(es) named in the labels are missing on disk:\n"
            + "\n".join(f"  {p}" for p in missing[:10])
            + ("\n  ..." if len(missing) > 10 else "")
        )

    lines.sort()
    if args.out:
        args.out.write_text("\n".join(lines) + "\n" if lines else "")
    else:
        print("\n".join(lines))

    # Progress goes to stderr so `--out -`-style piping stays clean.
    empty = sorted(wanted - bugs_with_legs)
    print(f"split      : {split_file.relative_to(REPO)} "
          f"(kind={args.kind}, side={args.side})", file=sys.stderr)
    print(f"labels     : {labels.relative_to(REPO)}", file=sys.stderr)
    print(f"bugs       : {len(bugs_with_legs)} of {len(wanted)} on this side contribute a patch",
          file=sys.stderr)
    print(f"patches    : {len(lines)}  (-o overfitting={legs['overfitting']}, "
          f"-c correct={legs['correct']})", file=sys.stderr)
    if repeats:
        print(f"  note: {repeats} repeated certification row(s) collapsed; "
              f"a patch is queued once", file=sys.stderr)
    for project, bug_id in empty:
        print(f"  note: {project}-{bug_id} has no usable certified patch here", file=sys.stderr)


if __name__ == "__main__":
    main()
