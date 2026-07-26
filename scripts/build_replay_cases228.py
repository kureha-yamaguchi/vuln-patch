#!/usr/bin/env python3
"""Build the verifier_replay fixture population from the judge-verdict inventory.

Source of truth for WHICH firings exist and what their gold verdict SHOULD be:
  docs/judge-verdict-inventory-2026-07-26.md  (228 judged firings, 206 collapsed rows)
Source of the mechanical fields the verifier saw (harness, fired assertion, evidence,
trusted values, code context):
  runs-archive/runs/<run-dir>/<leg-dir>/trace.md  — the `verifier / judge` LLM blocks.

Output: one JSONL case per reconstructable judge firing, carrying the fields
src/java/verifier_replay.py load_cases() needs (harness_source + fired_assertion +
concrete_evidence + trusted_values + code_context as available) PLUS a derived GOLD
verdict and provenance (run, leg, oracle id, inventory row number).

GOLD DERIVATION (from the inventory's own analysis + the plan.md §5D validation gate and
the 1d02859 dev-fix adjudication — NOT from the judge's actual verdict V):
  * `_c` leg (correct patch): EVERY firing is a false accusation -> gold = UNSOUND.
      (Covers the 26 SOUND-on-c FP keeps that should drop AND the 44 UNSOUND-on-c
       correct dismissals that should stay dropped.)
  * `_o` leg (overfitting patch):
      - the 4 confirmed drift-kills (§c/§5D "must flip to kept") -> gold = SOUND.
      - the parked / rider-pending ambiguous set (Math-104 complement family adjudicated
        judge-drift-but-PARKED; the two pinned-UTC Lang-63 relations awaiting the dev-fix
        rider; Lang-63 day-shift step-4b flag) -> gold = UNRESOLVED (excluded from scoring).
      - otherwise gold follows the inventory `class` grounds column:
          keep-grounds class (contract-backed/trusted-lift/observed-impossible/consistency)
            -> gold = SOUND (genuine catch of the overfit)
          hypothetical class (tolerance-floor/lazy-state/format-freedom/parse-error-latitude/
            invented-generalization/preexisting-identical/broken-check/timezone-env/other)
            -> gold = UNSOUND (correct kill of a bad check).

verifier_replay scores by `label` in {overfitting, correct}: label=overfitting => KEEPING
is scored correct (a drop is an over-kill); label=correct => DROPPING is scored correct (a
keep is a leak). We map gold onto label so the replay's over-kill/leak rates measure the
judge against gold directly:
    gold SOUND      -> label = 'overfitting'   (keeping is correct)
    gold UNSOUND    -> label = 'correct'       (dropping is correct)
    gold UNRESOLVED -> label = 'unresolved'    (neither bucket; still loaded+run, unscored)
"""
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
INVENTORY = REPO / "docs" / "judge-verdict-inventory-2026-07-26.md"
RUNS = REPO / "runs-archive" / "runs"

RUN_DIR = {
    "night20": "night20_20260725_155442",
    "pool30": "pool30_20260724_162730",
    "poolA": "poolA_20260725_090434",
    "poolB": "poolB_20260725_103258",
    "width5": "width5_20260725_144608",
}

KEEP_GROUNDS = {  # SOUND grounds classes
    "contract-backed", "trusted-lift", "observed-impossible", "consistency",
}
# every other class is a hypothetical/unsound-grounds class

# --- gold overrides keyed on (run, legnum, normalized-check-prefix) -----------
# The 4 confirmed drift-kills (plan.md §5D "must flip to kept"): genuine catches
# the judge wrongly killed. gold = SOUND despite the hypothetical class.
DRIFT_KILL_SOUND = {
    ("pool30", 16, "subtractionpositiveintegerhasnoextraseparator"),
    ("night20", 8, "minuspositivezerohasnoforcedspace"),
    ("night20", 8, "minuspositiveintegerhasnoforcedspace"),
    ("width5", 4, "containsdoesnotchangecapacity"),
}

# UNRESOLVED (ambiguous / parked / rider-pending): keep in file, exclude from scoring.
# Math-104 complement family (adjudicated judge-drift but PARKED, firewall warning);
# the two pinned-UTC Lang-63 relations (dev-fix rider not yet run); Lang-63 day-shift
# (step-4b flag, check-soundness not established).
def is_unresolved(run, legnum, bench, norm_check):
    if bench == "Math-104":
        # complement-property checks: P vs 1-Q / P+Q=1 comparisons
        if "complement" in norm_check or norm_check in (
                "pqcompssametuning", "ppqsametuning", "pqsametuning",
                "qcfsum", "fuzzedcomplement", "ppqcomplement"):
            return True
        if "sametuning" in norm_check:
            return True
    if bench == "Lang-63":
        if "utc" in norm_check or "periodandduration" in norm_check \
                or "periodequalsduration" in norm_check:
            return True
        if "dayshift" in norm_check:
            return True
    return False


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())


# ---------------------------------------------------------------------------
# 1. Parse the inventory table.
# ---------------------------------------------------------------------------
def parse_inventory():
    rows = []
    text = INVENTORY.read_text(encoding="utf-8")
    for line in text.splitlines():
        m = re.match(r"^\|\s*(\d+)\s*\|", line)
        if not m:
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 12:
            continue
        (num, run, leg, L, check, fired, facts, V, klass, why, out, n) = cells[:12]
        legnum = int(re.match(r"\s*(\d+)", leg).group(1))
        rows.append({
            "row": int(num), "run": run, "leg": leg, "legnum": legnum,
            "L": L, "check": check, "fired_values": fired, "facts": facts,
            "V": V, "class": klass, "why": why, "out": out,
            "n": int(n) if n.isdigit() else 1,
        })
    return rows


def check_identity(check):
    """Return (kind, normalized-name) for an inventory `check` cell."""
    c = check.strip()
    if c.startswith("oracle:"):
        return ("oracle", norm(c[len("oracle:"):]))
    if c.startswith("relation "):
        return ("relation", norm(c[len("relation "):]))
    if c.startswith("crash:"):
        return ("crash", norm(c[len("crash:"):]))
    return ("other", norm(c))


# ---------------------------------------------------------------------------
# 2. Parse verifier/judge blocks out of a trace.md.
# ---------------------------------------------------------------------------
JUDGE_HDR = re.compile(r"^## \[\d+\].*LLM call — \*\*verifier / judge\*\*")
ANY_HDR = re.compile(r"^## \[\d+\]")

STOP_ASSERT = ("Judge ONLY", "CONCRETE EVIDENCE", "<evidence>",
               "THE CODE UNDER TEST", "<codebase_context>",
               "These expected values were lifted")


def extract_block(text, start, end):
    block = text[start:end]

    def between(a, b):
        i = block.find(a)
        if i < 0:
            return None
        i += len(a)
        j = block.find(b, i)
        if j < 0:
            return None
        return block[i:j]

    harness = between("<harness>\n", "\n</harness>")
    evidence = between("<evidence>\n", "\n</evidence>")
    code_ctx = between("<codebase_context>\n", "\n</codebase_context>")

    fired = None
    marker = "The assertion that ACTUALLY fired on the patched code is:"
    mi = block.find(marker)
    if mi >= 0:
        rest = block[mi + len(marker):]
        lines = []
        for ln in rest.splitlines():
            s = ln.strip()
            if any(s.startswith(p) for p in STOP_ASSERT):
                break
            if s.startswith("== ") or s.startswith("<evidence"):
                break
            if s:
                lines.append(s)
            elif lines:
                break
        fired = "\n".join(lines).strip() or None

    trusted = None
    tm = block.find("These expected values were lifted")
    if tm >= 0:
        seg = block[tm:]
        cm = re.search(r"iteration-dependent:\s*\n\s*(.+)", seg)
        if cm:
            vals = [v.strip() for v in cm.group(1).split(";") if v.strip()]
            trusted = vals or None

    verdict = None
    vm = re.search(r"VERDICT:\s*(SOUND|UNSOUND)", block)
    if vm:
        verdict = vm.group(1)

    return {"harness": harness, "fired": fired, "evidence": evidence,
            "code_ctx": code_ctx, "trusted": trusted, "verdict": verdict}


def block_check_identity(fired, harness):
    """(kind, normalized-name) from a firing's assertion text."""
    if not fired:
        return None
    m = re.search(r"\[oracle:([^\]]+)\]", fired)
    if m:
        return ("oracle", norm(m.group(1)))
    m = re.search(r"relation\s+([A-Za-z0-9_-]+)\s+violated", fired)
    if m:
        return ("relation", norm(m.group(1)))
    m = re.match(r"([\w.]+(?:Exception|Error))\b", fired)
    if m:
        simple = m.group(1).rsplit(".", 1)[-1]
        return ("crash", norm(simple))
    return ("other", norm(fired[:40]))


def parse_trace(path):
    text = path.read_text(encoding="utf-8", errors="replace")
    hdrs = [(m.start(), JUDGE_HDR.match(text[m.start():text.find(chr(10), m.start())]))
            for m in re.finditer(r"^## \[\d+\]", text, re.M)]
    # positions of all top-level headers to delimit blocks
    starts = [m.start() for m in re.finditer(r"^## \[\d+\]", text, re.M)]
    starts.append(len(text))
    blocks = []
    for i in range(len(starts) - 1):
        s, e = starts[i], starts[i + 1]
        head = text[s:text.find("\n", s)]
        if "verifier / judge" in head:
            b = extract_block(text, s, e)
            b["ident"] = block_check_identity(b["fired"], b["harness"])
            blocks.append(b)
    return blocks


def _tokens(s):
    return set(re.findall(r"-?\d+(?:\.\d+)?|[A-Za-z_]+", s or ""))


def match_row(bident, fired_text, rows_here, used):
    """Find the inventory row whose check identity matches this block.

    Handles the collision case where a leg has several rows with the SAME
    normalized check name (n-collapsed near-identical firings): disambiguate
    by fired-value token overlap, then round-robin to the least-used candidate
    so each distinct inventory row gets attributed a block."""
    if not bident:
        return None
    bkind, bname = bident
    cands = []
    for r in rows_here:
        rkind, rname = check_identity(r["check"])
        if rkind != bkind:
            continue
        if rname == bname:
            name_score = 10_000
        elif bname.startswith(rname) or rname.startswith(bname):
            name_score = len(rname)
        else:
            continue
        cands.append((name_score, r))
    if not cands:
        return None
    top = max(c[0] for c in cands)
    cands = [r for sc, r in cands if sc == top]
    if len(cands) == 1:
        return cands[0]
    # collision: rank by fired-value overlap, then by fewest prior uses
    btok = _tokens(fired_text)
    def key(r):
        overlap = len(_tokens(r["fired_values"]) & btok)
        return (overlap, -used.get(r["row"], 0))
    return max(cands, key=key)


def derive_gold(row):
    bench = re.sub(r"^\d+\s+", "", row["leg"])
    bench = re.sub(r"-(Arja|ACS|Jaid|CapGen|Elixir|SequenceR|SOFix|SimFix|"
                   r"HDRepair|DeepRepair)\b.*$", "", bench).strip()
    _, ncheck = check_identity(row["check"])
    if row["L"] == "c":
        return "UNSOUND"
    # _o leg
    key = (row["run"], row["legnum"], ncheck)
    for dk in DRIFT_KILL_SOUND:
        if dk[0] == row["run"] and dk[1] == row["legnum"] and \
                (ncheck.startswith(dk[2]) or dk[2].startswith(ncheck)):
            return "SOUND"
    if is_unresolved(row["run"], row["legnum"], bench, ncheck):
        return "UNRESOLVED"
    return "SOUND" if row["class"] in KEEP_GROUNDS else "UNSOUND"


GOLD_LABEL = {"SOUND": "overfitting", "UNSOUND": "correct",
              "UNRESOLVED": "unresolved"}


def main():
    rows = parse_inventory()
    by_leg = {}
    for r in rows:
        by_leg.setdefault((r["run"], r["legnum"]), []).append(r)

    cases = []
    skips = []
    used_rows = set()
    matched_blocks = 0
    total_blocks = 0

    for run, run_dir in RUN_DIR.items():
        rd = RUNS / run_dir
        for leg_path in sorted(rd.glob("[0-9]*")):
            if not leg_path.is_dir():
                continue
            trace = leg_path / "trace.md"
            if not trace.exists():
                continue
            legnum = int(re.match(r"(\d+)", leg_path.name).group(1))
            rows_here = by_leg.get((run, legnum), [])
            blocks = parse_trace(trace)
            seen_ident = {}
            used = {}
            for b in blocks:
                total_blocks += 1
                if not b["harness"] or not b["fired"]:
                    skips.append((run, leg_path.name, "no harness/assertion",
                                  b.get("ident")))
                    continue
                row = match_row(b["ident"], b["fired"], rows_here, used)
                if row is not None:
                    used[row["row"]] = used.get(row["row"], 0) + 1
                if row is None:
                    skips.append((run, leg_path.name,
                                  f"no inventory row for {b['ident']}", None))
                    continue
                matched_blocks += 1
                used_rows.add(row["row"])
                gold = derive_gold(row)
                idx = seen_ident.get(row["row"], 0)
                seen_ident[row["row"]] = idx + 1
                slug = norm(row["check"])[:32]
                suffix = f"_{idx}" if idx else ""
                cid = f"{run}_L{legnum:02d}_{slug}_r{row['row']}{suffix}"
                case = {
                    "id": cid,
                    "harness_source": b["harness"],
                    "fired_assertion": b["fired"],
                    "label": GOLD_LABEL[gold],
                    "gold": gold,
                    "provenance": {
                        "run": run, "leg": leg_path.name,
                        "oracle_id": row["check"], "inventory_row": row["row"],
                        "leg_label": row["L"], "leg_outcome": row["out"],
                        "inventory_class": row["class"],
                        "judge_verdict": b["verdict"] or row["V"],
                    },
                    "note": (f"[{run}/{leg_path.name} row {row['row']}] "
                             f"gold={gold} class={row['class']} "
                             f"legoutcome={row['out']} why={row['why']}"),
                }
                if b["evidence"]:
                    case["concrete_evidence"] = b["evidence"]
                if b["trusted"]:
                    case["trusted_values"] = b["trusted"]
                if b["code_ctx"]:
                    case["code_context"] = b["code_ctx"]
                cases.append(case)

    out_path = REPO / "tests" / "fixtures" / "cases228.jsonl"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as fh:
        for c in cases:
            fh.write(json.dumps(c, ensure_ascii=False) + "\n")

    # ---- stats -----------------------------------------------------------
    dist = {}
    for c in cases:
        k = (c["gold"], c["provenance"]["leg_label"])
        dist[k] = dist.get(k, 0) + 1
    unmatched_rows = [r["row"] for r in rows if r["row"] not in used_rows]

    print(f"inventory rows parsed:      {len(rows)}")
    print(f"total judge blocks scanned: {total_blocks}")
    print(f"blocks -> cases (matched):  {matched_blocks}")
    print(f"cases written:              {len(cases)}  -> {out_path}")
    print(f"skipped blocks:             {len(skips)}")
    print(f"inventory rows never hit:   {len(unmatched_rows)} {unmatched_rows}")
    print(f"file size:                  {out_path.stat().st_size/1e6:.1f} MB")
    print("gold x leg-label distribution:")
    for k in sorted(dist):
        print(f"   gold={k[0]:10s} leg={k[1]}  {dist[k]}")
    with_ev = sum(1 for c in cases if "concrete_evidence" in c)
    with_tv = sum(1 for c in cases if "trusted_values" in c)
    with_cc = sum(1 for c in cases if "code_context" in c)
    print(f"cases w/ concrete_evidence: {with_ev}")
    print(f"cases w/ trusted_values:    {with_tv}")
    print(f"cases w/ code_context:      {with_cc}")
    print("skip detail (first 30):")
    for s in skips[:30]:
        print("   ", s)


if __name__ == "__main__":
    main()
