#!/usr/bin/env python3
"""Reconstruct production's family-duty prior (``fd_prior``) for every replay
fixture case, from the ORIGINAL run's recorded trace — never re-derived.

WHY. ``verifier_replay.py`` used to pass ``fd_prior=None`` for every case, so
the 5C terminal gate freshly consulted ``family_duty`` on any firing carrying an
identical-on-both-builds fact. Production does not do that: run.py's Spec-J
ladder may already have settled the question for that firing (trigger-input
exemption -> YES, an explicit family-duty call -> YES/NO) and passes the answer
down as ``fd_prior``. The replay was therefore STRICTER than the pipeline it
claims to measure.

WHAT IS RECOVERABLE. The J-ladder's rung is printed to stdout, and stdout is not
archived (one-file-trace: each run emits only trace.md + result.jsonl). Three
recorded artefacts nevertheless pin the rung:

  1. the family-duty rung is an LLM call, so trace.md carries its prompt (which
     quotes the firing's assertion message verbatim) and its ``DUTY: YES|NO``
     answer -> fd_prior True / False;
  2. the ladder runs only when the buggy-replay VALUE comparison returned
     "identical", and the replay note that the judge was shown records that
     verdict in wording emitted by that same branch ("...with the SAME observed
     values" vs "DIFFERENT observed values" / "observed values were not
     compared"). No identical wording => the ladder never ran => production's
     fd_prior for that firing WAS None, and the replay's None is faithful;
  3. the relation-replay judge site in run.py hard-codes ``fd_prior=None`` (that
     track has no ladder), so every relation-replay case is faithful at None by
     construction.

MAPPING:
    family-duty YES                    -> fd_prior = True
    family-duty NO                     -> fd_prior = False
    ladder provably never ran (2 or 3) -> fd_prior = None  (faithful, scored)
    ladder ran, rung unrecorded        -> fd_prior = None + fd_prior_unresolved

The unresolved bucket is exactly the third case: the value comparison said
"identical" (so the ladder ran) but no family-duty call was recorded, meaning the
run took the trigger-input exemption (fd_prior=True) or the setup-divergence rung
(fd_prior left None) and no artefact distinguishes them. Those cases are excluded
from the scored totals rather than guessed.

Also regenerates the hard-criteria subset with its original filter
(gold==SOUND, or gold==UNSOUND and provenance.leg_label=='c').

    python3 scripts/reconstruct_fd_prior.py [--dry-run]
"""
import argparse
import collections
import glob
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, 'src'))

from java.relations.evidence_facts import (  # noqa: E402
    carries_terminal_identical_fact)

CASES = os.path.join(REPO, 'tests', 'fixtures', 'cases228.jsonl')
SUBSET = os.path.join(REPO, 'tests', 'fixtures', 'cases_subset150.jsonl')
RUNS = os.path.join(REPO, 'runs-archive', 'runs')

# --- trace.md structure ----------------------------------------------------
STEP_RE = re.compile(r'^## \[(\d+)\] ', re.M)
IDENT_USER_RE = re.compile(r'\*\[user\] message: identical to step \[(\d+)\]')
# RelationVerifier.family_duty's user prompt, verbatim tail.
FD_MARK = 'Answer on two lines: DUTY: YES | NO'
FD_ASSERT_MARK = 'THE ASSERTION MESSAGE it threw on the patched build:'
# RelationVerifier.verify's user prompt.
VERIFY_MARK = 'The assertion that ACTUALLY fired on the patched code is:'
DUTY_RE = re.compile(r'^DUTY:\s*(YES|NO)', re.M | re.I)


def norm(s):
    return re.sub(r'\s+', ' ', (s or '')).strip()


def _split_steps(text):
    marks = [(m.start(), int(m.group(1))) for m in STEP_RE.finditer(text)]
    out = []
    for i, (pos, idx) in enumerate(marks):
        end = marks[i + 1][0] if i + 1 < len(marks) else len(text)
        head, _, body = text[pos:end].partition('\n')
        out.append((idx, head, body))
    return out


def _first_line_after(body, marker):
    i = body.find(marker)
    if i < 0:
        return None
    for line in body[i + len(marker):].splitlines():
        if line.strip():
            return line.strip()
    return None


def parse_trace(path):
    """Recorded judge events of one leg, in file order.

    Each event: {kind: 'family-duty'|'verify', step, assertion, duty}."""
    with open(path, encoding='utf-8', errors='replace') as fh:
        text = fh.read()
    steps = _split_steps(text)
    bodies = {idx: body for idx, _h, body in steps}
    events = []
    for idx, head, body in steps:
        if 'LLM call' not in head:
            continue
        eff = body
        m = IDENT_USER_RE.search(body)
        if m and FD_MARK not in body and VERIFY_MARK not in body:
            # the user message was elided as a duplicate — follow the pointer
            eff = bodies.get(int(m.group(1)), body)
        if FD_MARK in eff:
            out_pos = body.find('▸ Output')
            dm = DUTY_RE.search(body[out_pos:] if out_pos >= 0 else body)
            events.append({
                'kind': 'family-duty', 'step': idx,
                'assertion': _first_line_after(eff, FD_ASSERT_MARK),
                'duty': dm.group(1).upper() if dm else None})
        elif VERIFY_MARK in eff:
            events.append({
                'kind': 'verify', 'step': idx,
                'assertion': _first_line_after(eff, VERIFY_MARK),
                'duty': None})
    return events


def leg_trace(run, leg):
    hits = glob.glob(os.path.join(RUNS, f'{run}_*', leg, 'trace.md'))
    return hits[0] if len(hits) == 1 else None


RELATION_FIRING_RE = re.compile(
    r'^relation .+ violated \[replay-on-patched, \w+ tier\]$')
# emitted ONLY by the value_verdict=="identical" branch of
# semantic_buggy_replay_note / muted_replay_note — i.e. the exact condition
# (_value_verdict / _mvv == "identical") that arms the Spec-J ladder.
LADDER_ARMED_MARK = 'SAME observed values'


def reconstruct(case, events, trace_rel):
    """-> (fd_prior, source, unresolved)."""
    fa = norm(case.get('fired_assertion'))
    fds = [e for e in events
           if e['kind'] == 'family-duty' and norm(e['assertion']) == fa]
    if fds:
        e = fds[0]
        verifies = [v['step'] for v in events
                    if v['kind'] == 'verify' and norm(v['assertion']) == fa]
        # the ladder asks BEFORE the soundness judge for that firing; the 5C
        # gate asks after. Record which, so the provenance is auditable.
        rung = ('ladder' if verifies and e['step'] < min(verifies)
                else '5C-gate')
        src = (f"{trace_rel} step[{e['step']}] family-duty "
               f"DUTY: {e['duty']} ({rung})")
        if e['duty'] == 'YES':
            return True, src, False
        # NO, or an unparseable answer, which family_duty itself treats as NO.
        return False, src, False
    if RELATION_FIRING_RE.match(fa):
        return None, ('relation-replay judge site: run.py passes '
                      'fd_prior=None (no J-ladder on this track)'), False
    if LADDER_ARMED_MARK in (case.get('concrete_evidence') or ''):
        # value comparison returned "identical" -> the ladder RAN, and since no
        # family-duty call was recorded it took the trigger-input exemption
        # (True) or setup-divergence (None). Nothing recorded separates them.
        return None, ('ladder armed (identical value verdict in evidence) but '
                      'no family-duty event recorded — rung not distinguishable'
                      ), True
    return None, ('no identical value verdict in the recorded evidence — the '
                  'J-ladder never ran, production fd_prior was None'), False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true',
                    help='report only; do not rewrite the fixtures')
    args = ap.parse_args()

    trace_cache, events_cache = {}, {}
    stats = collections.Counter()
    resolved_rows, unresolved_rows = [], []
    tmp = CASES + '.tmp'
    subset_tmp = SUBSET + '.tmp'
    n = 0
    # stream: one JSON object per line, in and out — never load the 8 MB whole
    with open(CASES, encoding='utf-8') as fin, \
            open(tmp, 'w', encoding='utf-8') as fout, \
            open(subset_tmp, 'w', encoding='utf-8') as fsub:
        for line in fin:
            line = line.strip()
            if not line:
                continue
            c = json.loads(line)
            p = c['provenance']
            key = (p['run'], p['leg'])
            if key not in events_cache:
                path = leg_trace(*key)
                trace_cache[key] = (os.path.relpath(path, REPO)
                                    if path else 'no-trace')
                events_cache[key] = parse_trace(path) if path else []
            fd, src, unres = reconstruct(
                c, events_cache[key], trace_cache[key])
            c['fd_prior'] = fd
            c['fd_prior_source'] = src
            c['fd_prior_unresolved'] = bool(unres)
            n += 1
            stats[fd] += 1
            stats['unresolved'] += bool(unres)
            stats['gate-relevant'] += carries_terminal_identical_fact(
                c.get('concrete_evidence'))
            row = (p.get('inventory_row'), c['gold'], fd, src)
            (unresolved_rows if unres else resolved_rows).append(row)
            out = json.dumps(c, ensure_ascii=False) + '\n'
            fout.write(out)
            if (c['gold'] in ('keep-finding', 'SOUND')
                    or (c['gold'] in ('dismiss-finding', 'UNSOUND') and p.get('leg_label') == 'c')):
                fsub.write(out)

    if args.dry_run:
        os.remove(tmp)
        os.remove(subset_tmp)
    else:
        os.replace(tmp, CASES)
        os.replace(subset_tmp, SUBSET)

    print(f'cases: {n}')
    print(f'  fd_prior=True   {stats[True]}')
    print(f'  fd_prior=False  {stats[False]}')
    print(f'  fd_prior=None   {stats[None]}')
    print(f'  gate-relevant (IDENT-carrying): {stats["gate-relevant"]}')
    print(f'  UNRESOLVED (IDENT-carrying, no recorded ladder event): '
          f'{stats["unresolved"]}')
    print('resolved from trace:')
    for r in resolved_rows:
        if r[2] is not None:
            print(f'   row {r[0]:>4} gold={r[1]:<9} fd_prior={r[2]}  {r[3]}')
    print('unresolved (excluded from the scored totals):')
    for r in unresolved_rows:
        print(f'   row {r[0]:>4} gold={r[1]:<9} {r[3]}')


if __name__ == '__main__':
    main()
