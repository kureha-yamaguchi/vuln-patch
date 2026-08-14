import sys
from pathlib import Path
sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents if (p / 'config.py').exists())))
#!/usr/bin/env python3
"""Freeze the CRASHING dev/holdout split, mirroring suites/splits/README.md's
semantic method.

The semantic split (frozen 2026-07-21, seed 20260721) had no generator checked
in -- it was produced once and committed. This script implements the same five
rules for the crashing pool so the crashing split is reproducible:

  1. Population: crashing bugs with >=1 usable certified patch in
     suites/labels/crashing/verified_correct.jsonl (kind == "crashing").
     Mislabels (verified_incorrect.jsonl) and excluded.jsonl patches are
     dropped, so a bug counts only for the legs it can legitimately contribute.
  2. Whole-bug holdout: every patch of a bug goes to the same side.
  3. Already-used bugs forced to DEV. For the crashing pipeline the tuning
     trail is logs/ (the experiment logs the pipeline was iterated on), NOT
     suites/cases/*.cases (which is the semantic tuning trail).
  4. Stratified by project: holdout target = round(0.6 * project total),
     drawn (seeded shuffle) from that project's UNUSED bugs only. The fraction
     is 0.6 here, not the semantic split's 0.4: only 5 of the 18 crashing bugs
     are contaminated (vs 16 of 70 semantic), so tuning needs less of the pool,
     and the crashing pool is leg-poor on the overfit side (29 overfit legs
     total) -- a 40% holdout leaves only 6 of them, too few to read a recall
     number off. 60% leaves 14 while keeping 3 untouched bugs in dev as smoke
     checks.
  5. Emit one row per bug: {project, bug_id, side, overfit_legs, correct_legs,
     used_in_tuning}.

Usage:
    python3 make_crashing_split.py [--seed 20260814] [--out PATH] [--dry-run]
"""
import argparse
import json
import re
import random
from collections import Counter, defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
LABELS = REPO / "suites" / "labels" / "crashing"
LOGS = REPO / "logs"
PROJECTS_DIR = REPO / "defects4j" / "framework" / "projects"

HOLDOUT_FRACTION = 0.6

# Bug ids appear in the logs either as a defects4j checkout path
# ("/tmp/d4j/Lang_44_buggy") or as a plain "Lang-44".
CHECKOUT_RE = re.compile(r"\b(Chart|Closure|Lang|Math|Time|Mockito)[-_](\d+)")
# Two early logs name only the trigger test, so the bug is recovered by
# matching that test against defects4j's trigger_tests files.
TRIGGER_LINE_RE = re.compile(r"^\s*[✓x]\s+(\S+::\S+)")


def load_jsonl(path):
    if not path.exists():
        return []
    with open(path) as fh:
        return [json.loads(line) for line in fh if line.strip()]


def build_trigger_test_index():
    """Map "Class::method" -> {(project, bug_id)} from defects4j trigger_tests."""
    index = defaultdict(set)
    for project_dir in sorted(PROJECTS_DIR.iterdir()):
        trigger_dir = project_dir / "trigger_tests"
        if not trigger_dir.is_dir():
            continue
        for bug_file in trigger_dir.iterdir():
            if not bug_file.name.isdigit():
                continue
            for line in bug_file.read_text(errors="ignore").splitlines():
                if line.startswith("---"):
                    index[line[3:].strip()].add((project_dir.name, bug_file.name))
    return index


def bugs_seen_in_logs():
    """The crashing pipeline's tuning trail: every bug touched by logs/*.log."""
    index = build_trigger_test_index()
    seen, provenance = set(), defaultdict(set)
    for log in sorted(LOGS.glob("*.log")):
        text = log.read_text(errors="ignore")
        found = {(m.group(1), m.group(2)) for m in CHECKOUT_RE.finditer(text)}
        if not found:
            # No checkout line -- recover the bug from the trigger test name.
            for line in text.splitlines():
                m = TRIGGER_LINE_RE.match(line)
                if m:
                    found |= index.get(m.group(1), set())
        for bug in found:
            provenance[bug].add(log.name)
        seen |= found
    return seen, provenance


def certified_pool():
    """Crashing bugs with >=1 usable certified leg, with per-side leg counts."""
    dropped = {
        (r["project"], r["bug_id"], r["patch"])
        for r in load_jsonl(LABELS / "verified_incorrect.jsonl") + load_jsonl(LABELS / "excluded.jsonl")
    }
    legs = defaultdict(Counter)
    for row in load_jsonl(LABELS / "verified_correct.jsonl"):
        if row.get("kind") != "crashing":
            continue
        if (row["project"], row["bug_id"], row["patch"]) in dropped:
            continue  # belt-and-braces: verified_correct should not overlap
        legs[(row["project"], row["bug_id"])][row["drr_label"]] += 1
    return legs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--seed", type=int, default=20260814)
    ap.add_argument("--out", type=Path, default=REPO / "suites" / "splits" / "crashing_split.jsonl")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    legs = certified_pool()
    used_all, provenance = bugs_seen_in_logs()
    used = used_all & set(legs)

    print(f"Certified crashing pool : {len(legs)} bugs")
    print(f"Bugs seen in logs/      : {len(used_all)} ({len(used)} of them in the pool)")
    for bug in sorted(used_all, key=lambda b: (b[0], int(b[1]))):
        mark = "pool" if bug in legs else "not in pool"
        print(f"    {bug[0]}-{bug[1]:<4s} [{mark}] {sorted(provenance[bug])}")

    by_project = defaultdict(list)
    for bug in legs:
        by_project[bug[0]].append(bug)

    side = {}
    for project in sorted(by_project):
        bugs = sorted(by_project[project], key=lambda b: int(b[1]))
        unused = [b for b in bugs if b not in used]
        target = round(HOLDOUT_FRACTION * len(bugs))
        target = min(target, len(unused))
        # Seed per project so adding a project cannot reshuffle the others.
        rng = random.Random(f"{args.seed}:{project}")
        shuffled = unused[:]
        rng.shuffle(shuffled)
        holdout = set(shuffled[:target])
        for b in bugs:
            side[b] = "holdout" if b in holdout else "dev"
        print(f"  {project:<8s} total={len(bugs):2d} used={len(bugs)-len(unused):2d} "
              f"target={target:2d} -> dev={len(bugs)-len(holdout):2d} holdout={len(holdout):2d}")

    rows = [
        {
            "project": p,
            "bug_id": b,
            "side": side[(p, b)],
            "overfit_legs": legs[(p, b)]["overfitting"],
            "correct_legs": legs[(p, b)]["correct"],
            "used_in_tuning": (p, b) in used,
        }
        for (p, b) in sorted(legs, key=lambda x: (x[0], int(x[1])))
    ]

    dev = [r for r in rows if r["side"] == "dev"]
    hold = [r for r in rows if r["side"] == "holdout"]
    print(f"\n  dev     : {len(dev):2d} bugs  overfit_legs={sum(r['overfit_legs'] for r in dev):3d} "
          f"correct_legs={sum(r['correct_legs'] for r in dev):3d}")
    print(f"  holdout : {len(hold):2d} bugs  overfit_legs={sum(r['overfit_legs'] for r in hold):3d} "
          f"correct_legs={sum(r['correct_legs'] for r in hold):3d}")
    leaked = [r for r in hold if r["used_in_tuning"]]
    print(f"  holdout used-bug leakage: {len(leaked)} (must be 0)")
    assert not leaked, "holdout contains a tuned bug"

    if args.dry_run:
        print("\n--dry-run: nothing written")
        return
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with open(args.out, "w") as fh:
        for r in rows:
            fh.write(json.dumps(r) + "\n")
    print(f"\nWrote {len(rows)} rows -> {args.out}")


if __name__ == "__main__":
    main()
