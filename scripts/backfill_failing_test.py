"""Backfill the per-leg REAL FAILING TEST block into the replay fixtures.

WHY. `RelationVerifier.family_duty` asks exactly one question: "is the property
this check asserts THE VERY behaviour the failing test shows is wrong?" It
cannot answer that without the failing test. Production hands it
`run.py:_j3_failing_test_block(failure_tests)`; the replay fixture carried no
such field, so `verifier_replay` passed `failing_block=''` and the family-duty
escape could essentially never answer YES. Every rule that depends on that
escape was therefore measured against a harness that had disabled it.

The block IS recoverable verbatim: run.py renders it into the judge prompt, so
every leg's `trace.md` contains it. This script pulls it out per LEG (the block
is a property of the BUG's trigger tests, constant across a leg's firings) and
writes it into each case as `failing_test`.

Rendered shape (run.py:_j3_failing_test_block), for up to two trigger tests:

    [REAL FAILING TEST <Class>::<method> — trust source #1, verbatim]
    <the test method's source, verbatim, capped at 2000 chars>
    On the BUGGY build this test fails with: <failure message, capped at 400>

Two independent renderings of it exist in a trace and are used as sources:
  1. the family-duty prompt, between `<failing_test>` and `</failing_test>`
     (relation_verifier.family_duty);
  2. the tail of the judge prompt's `<evidence>` section — run.py appends the
     block LAST to the evidence blob, so it runs from the first
     `[REAL FAILING TEST ` marker to `</evidence>`.
Every rendering found for a leg is checked to be a substring of the longest one
(a second `[REAL FAILING TEST ` marker inside a two-test block yields a tail
fragment); the longest is the whole block and is what gets written.

The fixture is ~8 MB, so it is streamed line by line, never loaded whole.

Usage:  python3 scripts/backfill_failing_test.py [--check]
"""
import argparse
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUNS = os.path.join(REPO, 'runs-archive', 'runs')
FULL = os.path.join(REPO, 'tests', 'fixtures', 'cases228.jsonl')
SUBSET = os.path.join(REPO, 'tests', 'fixtures', 'cases_subset150.jsonl')

# provenance.run carries the short run name; the archive keeps the timestamped
# directory. Mapping is 1:1 (night20b is a DIFFERENT, later run and is not a
# source for these fixtures).
RUN_MAP = {
    'night20': 'night20_20260725_155442',
    'poolA': 'poolA_20260725_090434',
    'poolB': 'poolB_20260725_103258',
    'pool30': 'pool30_20260724_162730',
    'width5': 'width5_20260725_144608',
}

FD_OPEN = ("THE REAL FAILING TEST — its name, source, and the message it"
           " fails with on the buggy build:\n<failing_test>\n")
FD_CLOSE = "\n</failing_test>\n\nTHE CHECK THAT FIRED"
MARKER = "[REAL FAILING TEST "


def _from_family_duty_prompt(text):
    out = []
    for m in re.finditer(re.escape(FD_OPEN), text):
        end = text.find(FD_CLOSE, m.end())
        if end != -1:
            out.append(text[m.end():end])
    return out


def _from_evidence_tail(text):
    out = []
    for m in re.finditer(re.escape(MARKER), text):
        end = text.find('\n</evidence>', m.start())
        if end == -1:
            continue
        seg = text[m.start():end]
        if '<evidence>' in seg:      # marker was not inside an evidence blob
            continue
        out.append(seg)
    return out


def block_for_trace(path):
    """The leg's failing-test block, verbatim, or '' if the trace has none."""
    with open(path, encoding='utf-8', errors='replace') as fh:
        text = fh.read()
    cands = [c.strip() for c in
             (_from_family_duty_prompt(text) + _from_evidence_tail(text))
             if c.strip()]
    if not cands:
        return ''
    longest = max(cands, key=len)
    for c in cands:
        if c not in longest:
            raise AssertionError(
                f"{path}: inconsistent failing-test renderings "
                f"({len(c)} chars not contained in the {len(longest)}-char one)")
    if not longest.startswith(MARKER):
        raise AssertionError(f"{path}: block does not start with {MARKER!r}")
    return longest


def leg_blocks():
    """{(run_short, leg_dir_name): block} for every archived leg."""
    blocks = {}
    for short, run_dir in RUN_MAP.items():
        root = os.path.join(RUNS, run_dir)
        if not os.path.isdir(root):
            print(f"  missing archived run dir: {root}", file=sys.stderr)
            continue
        for leg in sorted(os.listdir(root)):
            trace = os.path.join(root, leg, 'trace.md')
            if os.path.isfile(trace):
                blocks[(short, leg)] = block_for_trace(trace)
    return blocks


def in_subset(case):
    """The filter cases_subset150.jsonl has always used."""
    prov = case.get('provenance') or {}
    return (case.get('gold') == 'SOUND'
            or (case.get('gold') == 'UNSOUND'
                and prov.get('leg_label') == 'c'))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true',
                    help="report coverage only; write nothing")
    args = ap.parse_args()

    blocks = leg_blocks()
    print(f"legs scanned: {len(blocks)}  "
          f"with block: {sum(1 for v in blocks.values() if v)}")

    tmp = FULL + '.tmp'
    n = n_block = 0
    missing_legs = {}
    subset = []
    out = None if args.check else open(tmp, 'w', encoding='utf-8')
    try:
        with open(FULL, encoding='utf-8') as fh:
            for line in fh:                       # stream: the file is ~8 MB
                line = line.strip()
                if not line:
                    continue
                case = json.loads(line)
                prov = case.get('provenance') or {}
                key = (prov.get('run'), prov.get('leg'))
                block = blocks.get(key, '')
                if block:
                    n_block += 1
                else:
                    missing_legs[key] = missing_legs.get(key, 0) + 1
                # keep a stable field order: failing_test sits with the other
                # evidence fields, right after code_context when present.
                rebuilt = {}
                for k, v in case.items():
                    rebuilt[k] = v
                    if k == 'code_context':
                        rebuilt['failing_test'] = block
                if 'failing_test' not in rebuilt:
                    rebuilt['failing_test'] = block
                n += 1
                dumped = json.dumps(rebuilt, ensure_ascii=False)
                if out:
                    out.write(dumped + '\n')
                if in_subset(rebuilt):
                    subset.append(dumped)
    finally:
        if out:
            out.close()

    if not args.check:
        os.replace(tmp, FULL)
        with open(SUBSET, 'w', encoding='utf-8') as fh:
            for dumped in subset:
                fh.write(dumped + '\n')

    print(f"cases: {n}  with failing_test: {n_block}  "
          f"without: {n - n_block}")
    if missing_legs:
        print("legs with NO failing-test block in their trace:")
        for key, cnt in sorted(missing_legs.items()):
            print(f"  {key[0]}/{key[1]}: {cnt} case(s)")
    print(f"subset rows: {len(subset)}")


if __name__ == '__main__':
    main()
