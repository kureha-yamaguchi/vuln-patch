"""Offline replay study for Mechanism A of the reportable-exception
pre-registration (`docs/reportable-exception-prereg-2026-08-09.md`).

STATION: relation body shape (what `relation_synth.py` mandates) + the
relation screen/replay execution path (`relation_screen.py`).
FAILURE MODE ADDRESSED: a relation that calls the patch-changed class on
an input it declared valid by construction, and whose mandated
`catch (Exception e) { return; }` swallows the exception the patched
build throws — the relation reports nothing (the Chart-19 FORK-ORACLE
read, `docs/draw05-routing-reread-2026-08-09.md` §4).

THE QUESTION: had the archived runs' KEPT relations used the two-tier
catch shape, which legs would now fire on the patched build, and does
any CORRECT leg start firing?

Two phases, one file:

  extract  (runs anywhere; no JVM)
      Reads each archived leg's own `trace.md` and recovers the kept
      relations (name / kind / contract / input / check), the leg's
      package + source imports, and the failing tests' method sources.
      Cross-checks the kept set against the leg's `replay-on-patched`
      step names — a mismatch is reported, never papered over. Emits one
      JSONL line per leg (the study input).

  run      (VM; needs defects4j + Jazzer)
      Groups legs by (patch file), builds each distinct (bug, patch)
      pair ONCE via the shipped `PatchSelector` / `PatchedProjectBuilder`,
      mechanically rewrites each relation's tier-2 catch, screens every
      rewritten relation on the BUGGY build via the shipped
      `measure_single_check`, and replays it on the PATCHED build via the
      shipped `replay_on_patched`. Zero LLM calls.

THE TRANSFORM (mechanical, label-blind, identical for every leg):

  * The patch-changed class comes from the patch file's own `---` header.
    Subclasses of it (resolved by reading the checkout's `extends`
    clauses, transitively) count too — `ObjectList` delegating to
    `AbstractObjectList` is the shape the pre-registration names.
  * A PROBE statement is a top-level simple statement inside a
    catch-and-return try whose text contains a call on the patch-changed
    class or one of its subclasses (static owner, or a local variable
    whose declared type resolves there). Constructor calls do NOT count:
    the pre-registration puts "build inputs/receivers" in tier 1.
  * Each probe statement is wrapped in its own try whose catch rethrows
      RuntimeException("relation <name> violated: unexpected <Exc> on
      valid-by-construction input: <msg>")
    and every enclosing catch-and-return catch gets a one-line guard that
    lets a `violated` RuntimeException through instead of returning.
    A declaration-with-initialiser has its declaration hoisted out of the
    new try so nothing changes scope; Java's definite-assignment rule is
    preserved because the new catch always completes abruptly.
  * Relations that already use a targeted `catch (SomeException ok)`
    (an expected-rejection contract) are LEFT UNTOUCHED, as are relations
    whose broad catch already throws a violation.
  * Fail-closed: anything the transform cannot isolate confidently is
    SKIPPED with a recorded reason. Nothing is silently dropped.

Usage:
  # Mac
  python3 java/studies/rex_replay.py extract \
      --runs-root runs-archive/runs \
      --suite invdiv_20260808_203424 --suite varbase_20260808_183839 \
      --suite diffcov_reach_20260808_233005 \
      --out /tmp/rex_study_input.jsonl

  # VM, from /home/code/experiments-vuln-patch/src, after `source /home/code/vpenv.sh`
  D4J_CHECKOUT_ROOT=/home/code/scratch/co/rex_replay \
  uv run python java/studies/rex_replay.py run \
      --input /home/code/scratch/rex_study_input.jsonl \
      --out /home/code/scratch/runs/rex_replay_<stamp> --workers 4
"""
import argparse
import json
import os
import re
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

# --------------------------------------------------------------------------
# PART 1 — extraction from an archived leg's trace.md
# --------------------------------------------------------------------------

_STEP_RE = re.compile(r'^## \[(\d+)\] (.*)$', re.M)
_TICKED_RE = re.compile(r'`([^`]+)`')


def _steps(text):
    """Yield (index, header, body) for every trace step, in order."""
    marks = list(_STEP_RE.finditer(text))
    for i, m in enumerate(marks):
        end = marks[i + 1].start() if i + 1 < len(marks) else len(text)
        yield int(m.group(1)), m.group(2).strip(), text[m.end():end]


def _llm_output(body):
    """The fenced block inside a step's `▸ Output` details section."""
    i = body.find('<summary>▸ Output')
    if i < 0:
        return None
    j = body.find('</details>', i)
    seg = body[i:j if j > 0 else len(body)]
    m = re.search(r'```[a-zA-Z]*\n(.*?)\n```', seg, re.S)
    return m.group(1) if m else None


def _det_json(body):
    """The fenced JSON payload of a deterministic step's output."""
    m = re.search(r'\*\*output:\*\*\s*\n?```(?:json)?\n(.*?)\n```', body, re.S)
    return m.group(1) if m else None


def _det_line(body):
    m = re.search(r'\*\*output:\*\*\s*(.*)', body)
    return m.group(1).strip() if m else ''


def _parse_relation_array(payload):
    """Relation objects out of one rule-synthesis output (tolerant)."""
    if not payload:
        return []
    try:
        arr = json.loads(payload)
    except Exception:
        m = re.search(r'\[.*\]', payload, re.S)
        if not m:
            return []
        try:
            arr = json.loads(m.group(0))
        except Exception:
            return []
    return [o for o in arr if isinstance(o, dict) and o.get('name')]


def _failure_method_sources(payload):
    """`method_source=` values out of the failing-tests-found step.

    The step stores Python reprs of FailureTest inside a JSON array, so
    the source is a repr'd string inside a JSON string: decode the JSON,
    then re-evaluate the single-quoted repr body.
    """
    out = []
    try:
        entries = json.loads(payload)
    except Exception:
        return out
    for entry in entries:
        for m in re.finditer(r"method_source='((?:[^'\\]|\\.)*)'", str(entry)):
            try:
                out.append(eval("'" + m.group(1) + "'"))  # noqa: S307
            except Exception:
                continue
    return out


def extract_leg(trace_path):
    """Recover one leg's kept relations + context from its own trace.

    Returns a dict; `extraction` records the accounting (how many kept
    decisions were seen, how many relay steps, whether they agree, and
    every relation whose body could not be recovered).
    """
    text = Path(trace_path).read_text(errors='replace')
    current = {}          # name -> latest relation object seen
    kept = {}             # name -> relation object at its last `kept` screen
    kept_order = []
    replay_names = []
    synth_rounds = repairs = repairs_unmatched = 0
    package = None
    imports = []
    failing_sources = []
    patch_text = None

    for _n, hdr, body in _steps(text):
        if hdr.startswith('⚙️ failing-tests-found'):
            failing_sources = _failure_method_sources(_det_json(body) or '')
        elif hdr.startswith('⚙️ analysis (TargetAnalyzer)'):
            try:
                ctx = json.loads(_det_json(body) or '{}')
            except Exception:
                ctx = {}
            package = ctx.get('package')
            imports = ctx.get('source_imports') or []
            patch_text = ctx.get('patch_text')
        elif 'LLM call' in hdr and 'rule synthesis' in hdr:
            payload = _llm_output(body)
            if payload is None:
                continue
            synth_rounds += 1
            for obj in _parse_relation_array(payload):
                current[obj['name']] = obj
        elif 'LLM call' in hdr and 'compile-repair' in hdr:
            repairs += 1
            payload = _llm_output(body)
            if not payload:
                repairs_unmatched += 1
                continue
            m = (re.search(r'relation ([\w\-.]+) violated', payload)
                 or re.search(r'relation ([\w\-.]+) violated', body))
            if not m or m.group(1) not in current:
                repairs_unmatched += 1
                continue
            fixed = dict(current[m.group(1)])
            fixed['check'] = payload
            current[m.group(1)] = fixed
        elif hdr.startswith('⚙️ screen ·'):
            nm = _TICKED_RE.search(hdr).group(1)
            if not _det_line(body).startswith('**kept**'):
                continue
            # Screen decisions carry the relation name TRUNCATED to a fixed
            # width; fall back to a unique prefix match before giving up.
            obj = current.get(nm)
            if obj is None:
                cands = [k for k in current if k.startswith(nm)]
                obj = current[cands[0]] if len(cands) == 1 else None
            kept[nm] = obj
            if nm not in kept_order:
                kept_order.append(nm)
        elif hdr.startswith('⚙️ replay-on-patched ·'):
            replay_names.append(_TICKED_RE.search(hdr).group(1))

    bodyless = sorted(n for n, o in kept.items() if not o or not o.get('check'))
    return {
        'trace_path': str(trace_path),
        'package': package,
        'source_imports': imports,
        'failing_test_sources': failing_sources,
        'patch_text_from_trace': patch_text,
        'relations': [
            dict(kept[n] or {}, name_in_screen=n) for n in kept_order
            if kept.get(n) and kept[n].get('check')
        ],
        'extraction': {
            'kept_screen_decisions': len(kept),
            'replay_steps': len(set(replay_names)),
            'kept_matches_replay': sorted(kept) == sorted(set(replay_names)),
            'kept_only': sorted(set(kept) - set(replay_names)),
            'replay_only': sorted(set(replay_names) - set(kept)),
            'bodies_recovered': len(kept) - len(bodyless),
            'bodies_missing': bodyless,
            'synthesis_rounds': synth_rounds,
            'compile_repairs': repairs,
            'compile_repairs_unmatched': repairs_unmatched,
        },
    }


# Leg dir names look like `04_patch1-Chart-19-Arja-plausible_o`: the label
# suffix picks the dataset half, the file stem names tool / project / bug.
_LEG_RE = re.compile(r'^\d+_(?P<stem>.+)_(?P<label>[oc])$')


def leg_identity(leg_dir_name, drr_root):
    m = _LEG_RE.match(leg_dir_name)
    if not m:
        raise ValueError(f'unparseable leg dir name: {leg_dir_name}')
    stem, label = m.group('stem'), m.group('label')
    parts = stem.split('-')          # patch1 / <Proj> / <bug> / <Tool> / ...
    project, bug_id, tool = parts[1], parts[2], parts[3]
    half = 'Dcorrect' if label == 'c' else 'Doverfitting'
    return {
        'label': 'correct' if label == 'c' else 'overfitting',
        'project': project,
        'bug_id': bug_id,
        'apr_tool': tool,
        'patch_path': os.path.join(drr_root, half, tool, project,
                                   stem + '.patch'),
    }


def archived_leg_outcome(trace_path):
    """The leg's archived verdict line (`**Outcome:** ...`)."""
    m = re.search(r'\*\*Outcome:\*\*\s*(.*)',
                  Path(trace_path).read_text(errors='replace'))
    return m.group(1).strip() if m else '?'


def archived_replay_outcomes(trace_path):
    """`relation name -> archived replay-on-patched output line`, so the
    study can say what each relation did BEFORE the rewrite."""
    text = Path(trace_path).read_text(errors='replace')
    out = {}
    for _n, hdr, body in _steps(text):
        if hdr.startswith('⚙️ replay-on-patched ·'):
            out[_TICKED_RE.search(hdr).group(1)] = _det_line(body)
    return out


def failing_test_block(trace_path, cap_each=2000):
    """The judge's `[REAL FAILING TEST ...]` block for one leg, rebuilt
    from its trace in the same shape `run.py::_j3_failing_test_block`
    renders live."""
    text = Path(trace_path).read_text(errors='replace')
    payload = None
    for _n, hdr, body in _steps(text):
        if hdr.startswith('⚙️ failing-tests-found'):
            payload = _det_json(body)
            break
    if not payload:
        return ''
    try:
        entries = json.loads(payload)
    except Exception:
        return ''
    lines = []
    for entry in entries[:2]:
        entry = str(entry)
        cls = re.search(r"test_class='([^']*)'", entry)
        meth = re.search(r"test_method='([^']*)'", entry)
        src = re.search(r"method_source='((?:[^'\\]|\\.)*)'", entry)
        msg = re.search(r"failure_message='((?:[^'\\]|\\.)*)'", entry)
        if src:
            try:
                body_src = eval("'" + src.group(1) + "'")  # noqa: S307
            except Exception:
                body_src = ''
            lines.append(
                f"[REAL FAILING TEST {cls.group(1) if cls else '?'}::"
                f"{meth.group(1) if meth else '?'} — trust source #1, "
                f"verbatim]\n" + body_src[:cap_each])
        if msg:
            try:
                m = eval("'" + msg.group(1) + "'")  # noqa: S307
            except Exception:
                m = ''
            lines.append('On the BUGGY build this test fails with: ' + m[:400])
    return '\n'.join(lines)


def cmd_cases(args):
    """Phase-2 input: one `verifier_replay.py` case per NEW patched-only
    firing, so the verdict-level question ("would the verifier keep it?")
    can be asked without re-running phase 1."""
    rows = annotate([json.loads(l) for l in open(args.results) if l.strip()],
                    args.runs_root)
    runs_root = Path(args.runs_root)
    written = 0
    with open(args.out, 'w') as fh:
        for r in rows:
            if not r.get('rewrite_created_firing'):
                continue
            trace = runs_root / r['leg_id'] / 'trace.md'
            fired_line = (r.get('firing_messages') or [None])[0]
            fired = (f"relation {r['relation']} violated "
                     f"[replay-on-patched, {r.get('patched_tier')} tier]"
                     + (f" — {fired_line}" if fired_line else ''))
            evidence = (
                "[relation replay] the check below was mechanically screened "
                f"on the buggy build (fired on {r.get('buggy_violated')}/"
                f"{r.get('buggy_checked')} fuzzed inputs — silent on the "
                "buggy build) and, the SAME source compiled against the "
                f"patched build, {r.get('patched_note')}. A correct patch makes a "
                "sound contract relation go quiet; judge whether the "
                "relation itself is sound for ANY correct implementation "
                "(tolerances generous, inputs fenced).\n"
                "[rewrite provenance] the only edit to this check versus the "
                "archived run is the two-tier catch: calls on the "
                f"patch-changed class {r['patched_class']} now rethrow an "
                "unexpected exception as a violation instead of returning.\n"
                f"Relation contract as stated by its author: "
                f"{r.get('contract', '')}")
            case = {
                'id': f"rex__{r['leg_id'].replace('/', '__')}__"
                      f"{r['relation']}",
                'harness_source': r['transformed_check'],
                'fired_assertion': fired,
                'concrete_evidence': evidence,
                'failing_test': (failing_test_block(trace)
                                 if trace.exists() else ''),
                'label': r['label'],
                'note': (f"phase-2 of the reportable-exception replay study; "
                         f"{r['project']}-{r['bug_id']} "
                         f"({r['label']}), leg {r['leg']}"),
            }
            fh.write(json.dumps(case) + '\n')
            written += 1
    print(f'wrote {written} case(s) to {args.out}')


# The exact text the tier-2 catch throws. A firing carrying it is a
# reportable unexpected exception (Mechanism A); a firing without it is the
# relation's own value comparison, which the rewrite cannot create.
TIER2_MARK = 'violated: unexpected '


def annotate(rows, runs_root):
    """Add the two facts the gates are actually about: is the firing a
    TIER-2 (unexpected-exception) firing, and was this relation QUIET in
    the archived run? Only `tier-2 AND archived-quiet AND patched-only`
    is a firing the rewrite created."""
    runs_root = Path(runs_root)
    archived = {}
    for leg_id in {r['leg_id'] for r in rows}:
        t = runs_root / leg_id / 'trace.md'
        archived[leg_id] = archived_replay_outcomes(t) if t.exists() else {}
    for r in rows:
        msgs = r.get('firing_messages') or []
        r['archived_replay'] = archived.get(r['leg_id'], {}).get(
            r['relation'], '?')
        r['archived_quiet'] = r['archived_replay'].startswith('**quiet')
        r['tier2_firing'] = any(TIER2_MARK in m for m in msgs)
        r['rewrite_created_firing'] = bool(
            r.get('new_patched_only_firing') and r['tier2_firing']
            and r['archived_quiet'])
    return rows


def cmd_report(args):
    """Tables for the write-up, computed from the results file only."""
    rows = annotate([json.loads(l) for l in open(args.results) if l.strip()],
                    args.runs_root)
    runs_root = Path(args.runs_root)
    archived = {}
    for leg_id in {r['leg_id'] for r in rows}:
        t = runs_root / leg_id / 'trace.md'
        archived[leg_id] = archived_replay_outcomes(t) if t.exists() else {}

    out = []
    out.append('## per-relation (every kept relation of every leg)\n')
    out.append('| leg | relation | transform | fires-buggy | fires-patched '
               '| both | tier-2? | archived replay | firing message |')
    out.append('|---|---|---|---|---|---|---|---|---|')
    for r in sorted(rows, key=lambda x: (x['leg_id'], x['relation'])):
        msg = (r.get('firing_messages') or [''])[0].replace('|', '/')
        out.append(
            f"| {r['leg_id']} | {r['relation']} | {r['transform_status']} "
            f"| {r.get('fires_on_buggy')} | {r.get('fires_on_patched')} "
            f"| {r.get('fires_on_both')} | {r['tier2_firing']} "
            f"| {r['archived_replay']} | {msg[:400]} |")

    out.append('\n## per-leg\n')
    out.append('| leg | label | archived outcome | relations | transformed | '
               'executed | patched-only firings | of those, created by the '
               'rewrite | converted a MISS? |')
    out.append('|---|---|---|---|---|---|---|---|---|')
    legs = {}
    for r in rows:
        legs.setdefault(r['leg_id'], []).append(r)
    conv_o, conv_c, upside = [], [], []
    for leg_id in sorted(legs):
        rs = legs[leg_id]
        pofire = [r for r in rs if r.get('new_patched_only_firing')]
        created = [r for r in rs if r.get('rewrite_created_firing')]
        label = rs[0]['label']
        t = runs_root / leg_id / 'trace.md'
        outcome = archived_leg_outcome(t) if t.exists() else '?'
        was_miss = outcome.lower().startswith('overfit missed')
        converted = bool(created) and was_miss
        if converted:
            (conv_c if label == 'correct' else conv_o).append(leg_id)
        elif created:
            upside.append(leg_id)
        out.append(
            f"| {leg_id} | {label} | {outcome[:46]} | {len(rs)} | "
            f"{sum(1 for r in rs if r['transform_status'] == 'transformed')} "
            f"| {sum(1 for r in rs if r.get('executed') is True)} | "
            f"{len(pofire)} | {len(created)} | "
            f"{'YES' if converted else 'no'} |")

    out.append('\n## hard stop — CORRECT legs (G-P)\n')
    out.append('| leg | relation | patched-only | tier-2 (rewrite-created)? '
               '| archived replay | message |')
    out.append('|---|---|---|---|---|---|')
    hard = 0
    for r in sorted(rows, key=lambda x: (x['leg_id'], x['relation'])):
        if r['label'] != 'correct' or not r.get('new_patched_only_firing'):
            continue
        if r['rewrite_created_firing']:
            hard += 1
        out.append(
            f"| {r['leg_id']} | {r['relation']} | YES | "
            f"{'YES' if r['rewrite_created_firing'] else 'no'} | "
            f"{r['archived_replay']} | "
            f"{(r.get('firing_messages') or [''])[0][:400]} |")

    out.append('\n## transform / skip accounting\n')
    acc = {}
    for r in rows:
        acc.setdefault(r['transform_status'], 0)
        acc[r['transform_status']] += 1
    out.append('| status | relations |')
    out.append('|---|---|')
    for k in sorted(acc, key=lambda k: -acc[k]):
        out.append(f'| {k} | {acc[k]} |')
    out.append(f'\nTOTAL relations: {len(rows)}')
    out.append(f'patched-only firings (any kind): '
               f'{sum(1 for r in rows if r.get("new_patched_only_firing"))}')
    out.append(f'firings the rewrite CREATED (tier-2 + archived-quiet): '
               f'{sum(1 for r in rows if r.get("rewrite_created_firing"))}')
    out.append(f'CORRECT-leg rewrite-created firings (G-P): {hard}')
    out.append(f'ARCHIVED MISSES converted (G-R): {len(conv_o)} — {conv_o}')
    out.append(f'CORRECT legs converted: {len(conv_c)} — {conv_c}')
    out.append(f'already-caught legs that gained a rewrite firing (upside): '
               f'{len(upside)} — {upside}')
    text = '\n'.join(out)
    if args.out:
        Path(args.out).write_text(text + '\n')
    print(text)


def cmd_extract(args):
    runs_root = Path(args.runs_root)
    out_lines = []
    for suite in args.suite:
        for leg in sorted(os.listdir(runs_root / suite)):
            trace = runs_root / suite / leg / 'trace.md'
            if not trace.exists():
                continue
            rec = extract_leg(trace)
            rec.update(leg_identity(leg, args.drr_root))
            rec['suite'] = suite
            rec['leg'] = leg
            rec['leg_id'] = f'{suite}/{leg}'
            out_lines.append(rec)
    with open(args.out, 'w') as fh:
        for rec in out_lines:
            fh.write(json.dumps(rec) + '\n')
    total = sum(len(r['relations']) for r in out_lines)
    mismatched = [r['leg_id'] for r in out_lines
                  if not r['extraction']['kept_matches_replay']]
    print(f'legs={len(out_lines)} relations={total}')
    print(f'legs whose kept set disagrees with their replay steps: '
          f'{mismatched or "none"}')
    for r in out_lines:
        e = r['extraction']
        print(f"  {r['leg_id']}: kept={e['kept_screen_decisions']} "
              f"replay={e['replay_steps']} bodies={e['bodies_recovered']} "
              f"missing={e['bodies_missing']}")
    print(f'wrote {args.out}')


# --------------------------------------------------------------------------
# PART 2 — the mechanical transform
# --------------------------------------------------------------------------

_BROAD_CATCH_TYPES = {'Exception', 'Throwable', 'RuntimeException',
                      'java.lang.Exception', 'java.lang.Throwable',
                      'java.lang.RuntimeException'}


def _mask(src):
    """`src` with every string literal, char literal and comment replaced
    by same-length filler, so brace/paren scanning cannot be fooled."""
    out = list(src)
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"' or c == "'":
            q = c
            j = i + 1
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == q:
                    break
                j += 1
            for k in range(i, min(j + 1, n)):
                out[k] = ' '
            i = j + 1
        elif c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            j = n if j < 0 else j
            for k in range(i, j):
                out[k] = ' '
            i = j
        elif c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i)
            j = n if j < 0 else j + 2
            for k in range(i, j):
                out[k] = ' '
            i = j
        else:
            i += 1
    return ''.join(out)


def _match_brace(masked, open_idx):
    """Index just past the `}` closing the `{` at `open_idx`."""
    depth = 0
    for i in range(open_idx, len(masked)):
        if masked[i] == '{':
            depth += 1
        elif masked[i] == '}':
            depth -= 1
            if depth == 0:
                return i + 1
    return -1


def _match_paren(masked, open_idx):
    depth = 0
    for i in range(open_idx, len(masked)):
        if masked[i] == '(':
            depth += 1
        elif masked[i] == ')':
            depth -= 1
            if depth == 0:
                return i + 1
    return -1


def find_try_blocks(src):
    """Every `try { ... } catch (...) { ... }` in `src`, with spans.

    Returned innermost-last in source order; each entry records the try
    body span and one entry per catch clause (type, variable, body span).
    """
    masked = _mask(src)
    blocks = []
    for m in re.finditer(r'\btry\b\s*\{', masked):
        body_open = masked.index('{', m.start())
        body_end = _match_brace(masked, body_open)
        if body_end < 0:
            continue
        catches = []
        pos = body_end
        while True:
            cm = re.match(r'\s*catch\s*\(', masked[pos:])
            if not cm:
                break
            par_open = pos + cm.end() - 1
            par_end = _match_paren(masked, par_open)
            if par_end < 0:
                break
            decl = src[par_open + 1:par_end - 1].strip()
            dm = re.match(r'(?:final\s+)?([\w.$|\s]+?)\s+(\w+)\s*$', decl)
            if not dm:
                break
            cb_open = masked.find('{', par_end)
            if cb_open < 0:
                break
            cb_end = _match_brace(masked, cb_open)
            if cb_end < 0:
                break
            catches.append({
                'clause_start': pos + cm.start() + (len(cm.group(0))
                                                    - len(cm.group(0).lstrip())),
                'type': dm.group(1).strip(),
                'var': dm.group(2),
                'body_open': cb_open,
                'body_end': cb_end,
            })
            pos = cb_end
        blocks.append({
            'try_start': m.start(),
            'body_open': body_open,
            'body_end': body_end,
            'catches': catches,
            'end': pos,
        })
    return blocks


def split_statements(src, start, end):
    """Top-level statement spans inside `src[start:end]` (a block body)."""
    masked = _mask(src)
    spans = []
    depth_b = depth_p = 0
    i = start
    stmt_start = None
    while i < end:
        c = masked[i]
        if stmt_start is None and not c.isspace():
            stmt_start = i
        if c == '(':
            depth_p += 1
        elif c == ')':
            depth_p -= 1
        elif c == '{':
            depth_b += 1
        elif c == '}':
            depth_b -= 1
            if depth_b == 0 and depth_p == 0 and stmt_start is not None:
                # a block-shaped statement; consume a trailing `;`/else/catch
                j = i + 1
                while j < end and masked[j].isspace():
                    j += 1
                if j < end and masked[j] == ';':
                    i = j
                elif re.match(r'\s*(else|catch|finally)\b', masked[i + 1:end]):
                    i += 1
                    continue
                spans.append((stmt_start, i + 1))
                stmt_start = None
        elif c == ';' and depth_b == 0 and depth_p == 0:
            if stmt_start is not None:
                spans.append((stmt_start, i + 1))
                stmt_start = None
        i += 1
    return spans


_DECL_RE = re.compile(
    r'^\s*(?:final\s+)?([A-Za-z_][\w.$]*(?:\s*<[^;]*>)?(?:\s*\[\s*\])*)'
    r'\s+([A-Za-z_]\w*)\s*(?:=|;)')

# `T v = <init>;` — the one statement shape whose declaration has to be
# hoisted out of the new tier-2 try so `v` keeps the scope it had.
_DECL_INIT_RE = re.compile(
    r'^\s*(?:final\s+)?([A-Za-z_][\w.$]*(?:\s*<[^;]*>)?(?:\s*\[\s*\])*)'
    r'\s+([A-Za-z_]\w*)\s*=\s*(.+);\s*$', re.S)


def _has_top_level_comma(expr):
    masked = _mask(expr)
    depth = 0
    for c in masked:
        if c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
        elif c == ',' and depth == 0:
            return True
    return False


def declared_types(src):
    """`var -> declared type` for every local declaration in `src`."""
    out = {}
    masked = _mask(src)
    for m in re.finditer(r'[^;{}]+[;]', masked):
        raw = src[m.start():m.end()]
        d = _DECL_RE.match(raw)
        if d:
            typ = re.sub(r'\s*<.*', '', d.group(1)).strip()
            typ = re.sub(r'\s*\[\s*\]', '', typ).strip()
            if typ not in ('return', 'new', 'throw', 'if', 'else', 'for',
                           'while', 'case', 'catch'):
                out.setdefault(d.group(2), typ)
    return out


def subclass_closure(checkout_dir, fq_class):
    """Simple names of `fq_class` plus every class in the checkout that
    extends it, transitively. Conservative: matched on simple names,
    which over-approximates only when two packages reuse a name — and an
    over-approximation here only ever ADDS probe candidates, which the
    per-relation skip rules then have to isolate anyway."""
    simple = fq_class.rsplit('.', 1)[-1]
    parents = {}
    for root, _dirs, files in os.walk(checkout_dir):
        if os.sep + 'fuzz' in root:
            continue
        for f in files:
            if not f.endswith('.java'):
                continue
            try:
                txt = Path(root, f).read_text(errors='replace')
            except OSError:
                continue
            for m in re.finditer(
                    r'\bclass\s+(\w+)\s*(?:<[^{}]*>)?\s+extends\s+([\w.]+)',
                    txt):
                parents.setdefault(m.group(1), m.group(2).rsplit('.', 1)[-1])
    names = {simple}
    changed = True
    while changed:
        changed = False
        for child, parent in parents.items():
            if parent in names and child not in names:
                names.add(child)
                changed = True
    return names


def patch_changed_class(patch_path):
    """(fully-qualified class, changed method names) from the patch file."""
    text = Path(patch_path).read_text(errors='replace')
    m = re.search(r'^---\s+(\S+)', text, re.M)
    if not m:
        raise ValueError(f'no --- header in {patch_path}')
    path = m.group(1).strip()
    parts = [p for p in path.replace('\\', '/').split('/') if p]
    if not parts[-1].endswith('.java'):
        raise ValueError(f'patch header is not a .java path: {path}')
    cls = parts[-1][:-len('.java')]
    # the package is whatever follows the last source-root marker
    for marker in ('java', 'source', 'src'):
        if marker in parts[:-1]:
            idx = len(parts) - 1 - parts[:-1][::-1].index(marker)
            pkg = '.'.join(parts[idx:-1])
            break
    else:
        pkg = ''
    return (f'{pkg}.{cls}' if pkg else cls)


_CALL_RE = re.compile(r'([A-Za-z_][\w.$]*)\s*\.\s*([A-Za-z_]\w*)\s*\(')
_NEWCALL_RE = re.compile(r'\bnew\s+([\w.$]+)\s*\([^;]*\)\s*\.\s*([A-Za-z_]\w*)\s*\(')


def probe_calls(stmt, var_types, target_names, target_fq):
    """Calls in `stmt` whose static owner / receiver type is the patched
    class or one of its subclasses. Constructors are tier-1 setup and are
    never returned."""
    hits = []
    for m in _CALL_RE.finditer(stmt):
        recv, meth = m.group(1), m.group(2)
        if recv == 'data':
            continue
        # `new Foo(...).bar()` is handled by _NEWCALL_RE below
        typ = var_types.get(recv)
        if typ is None:
            last = recv.rsplit('.', 1)[-1]
            if last and last[0].isupper():
                typ = recv        # a class reference (static call)
            else:
                continue
        last = typ.rsplit('.', 1)[-1]
        if last not in target_names:
            continue
        if '.' in typ and last == target_fq.rsplit('.', 1)[-1] \
                and typ != target_fq:
            continue              # qualified name of a same-named other class
        hits.append(f'{typ}.{meth}')
    for m in _NEWCALL_RE.finditer(stmt):
        if m.group(1).rsplit('.', 1)[-1] in target_names:
            hits.append(f'{m.group(1)}.{m.group(2)}')
    return hits


_TIER2_CATCH = (
    ' catch (Exception {v}) {{ throw new RuntimeException('
    '"relation {name} violated: unexpected " + {v}.getClass().getName()'
    ' + " on valid-by-construction input: " + {v}.getMessage()); }}')


def _tier2_catch(v, name, target_names, documented):
    """The tier-2 catch for one isolated probe statement.

    With a `documented` map ({(class, method): [exception names]}, prereg
    addendum 2026-08-10), the catch walks the stack to the INNERMOST
    patched-class frame — same semantics as the shipped guard — and treats
    a type the docs permit for that frame's method as a rejection
    (return); everything else, including exceptions with no patched frame
    at all, still alarms exactly like the original template. The
    documented decision itself comes from the SHARED generator in
    java_source so study and ship cannot disagree."""
    if not documented:
        return _TIER2_CATCH.format(v=v, name=name)
    from java.parsing.java_source import (_class_name_tests,
                                          documented_exception_test)
    doc_test = documented_exception_test(v, '__rpdf', '__rpdoc', documented)
    if not doc_test:
        return _TIER2_CATCH.format(v=v, name=name)
    name_tests = ' || '.join(_class_name_tests('__rpdc', c)
                             for c in sorted(target_names))
    return (
        ' catch (Exception ' + v + ') {'
        ' for (StackTraceElement __rpdf : ' + v + '.getStackTrace()) {'
        ' String __rpdc = __rpdf.getClassName();'
        ' if ((' + name_tests + ')'
        ' && !"<init>".equals(__rpdf.getMethodName())) {'
        + doc_test
        + ' if (__rpdoc) return;'
        ' break; } }'
        ' throw new RuntimeException('
        '"relation ' + name + ' violated: unexpected " + '
        + v + '.getClass().getName()'
        + ' + " on valid-by-construction input: " + '
        + v + '.getMessage()); }')

_ESCAPE_GUARD = (
    ' if ({v} instanceof RuntimeException'
    ' && String.valueOf({v}.getMessage()).indexOf("violated") >= 0)'
    ' {{ throw (RuntimeException) {v}; }}')


def transform_check(check, name, var_types, target_names, target_fq,
                    documented=None):
    """Rewrite the tier-2 catches of one relation check body.

    Returns (new_check_or_None, status, detail). `status` is one of
    transformed / untouched-expected-rejection / untouched-already-reports
    / untouched-no-try / untouched-no-probe / skipped-probe-not-isolable.
    """
    blocks = find_try_blocks(check)
    if not blocks:
        return None, 'untouched-no-try', 'the check has no try block'

    for b in blocks:
        for c in b['catches']:
            if c['type'] not in _BROAD_CATCH_TYPES:
                return (None, 'untouched-expected-rejection',
                        f"targeted catch ({c['type']}) — expected-rejection "
                        f"contract, left as-is per the pre-registration")

    broad = []
    for b in blocks:
        cs = [c for c in b['catches'] if c['type'] in _BROAD_CATCH_TYPES]
        if not cs:
            continue
        if any('throw' in _mask(check[c['body_open']:c['body_end']])
               for c in cs):
            # Gate correction (prereg 2026-08-10): an LLM-written tier-2
            # catch rethrows unconditionally — insert the shared documented
            # check so a documented exception returns as a rejection while
            # everything else still reaches the relation's own rethrow.
            if documented:
                from java.parsing.java_source import guard_llm_tier2
                guarded = guard_llm_tier2(check, sorted(target_names),
                                          documented)
                if guarded is not None:
                    return (guarded, 'doc-guarded',
                            'LLM-written tier-2 catch: documented-exception '
                            'check inserted at the catch head')
            return (None, 'untouched-already-reports',
                    'a broad catch already throws — the check already '
                    'reports what the call under test does')
        broad.append(b)
    if not broad:
        return (None, 'untouched-no-try',
                'no try with a broad catch-and-return')

    edits = []          # (start, end, replacement)
    wrapped, nonisolable = [], []
    for b in broad:
        for (s, e) in split_statements(check, b['body_open'] + 1,
                                       b['body_end'] - 1):
            stmt = check[s:e]
            calls = probe_calls(stmt, var_types, target_names, target_fq)
            if not calls:
                continue
            if '{' in _mask(stmt):
                nonisolable.append(stmt.strip()[:120])
                continue
            v = f'__rexE{len(edits)}'
            body = stmt.strip()
            prefix = ''
            d = _DECL_INIT_RE.match(stmt)
            if d:
                # `T v = init;` becomes `T v; try { v = init; } catch ...`
                # so `v` keeps the scope it had. Definite assignment still
                # holds: the new catch always completes abruptly.
                if _has_top_level_comma(d.group(3)):
                    nonisolable.append(stmt.strip()[:120])
                    continue
                prefix = f'{d.group(1).strip()} {d.group(2)}; '
                body = f'{d.group(2)} = {d.group(3).strip()};'
            repl = (prefix + 'try { ' + body + ' }'
                    + _tier2_catch(v, name, target_names, documented))
            edits.append((s, e, repl))
            wrapped.append(calls)
    if not edits:
        if nonisolable:
            return (None, 'skipped-probe-not-isolable',
                    'the only calls on the patch-changed class sit inside a '
                    'brace-delimited or multi-declarator statement (loop, '
                    'branch, block, array initialiser) that cannot be '
                    'separated from setup: ' + ' | '.join(nonisolable[:2]))
        return (None, 'untouched-no-probe',
                'no call on the patch-changed class inside a '
                'catch-and-return try')

    # every catch-and-return whose try lexically contains a wrapped probe
    # must let the new alarm through instead of returning
    for b in broad:
        if not any(b['body_open'] < s < b['body_end'] for s, _e, _r in edits):
            continue
        for c in b['catches']:
            if c['type'] not in _BROAD_CATCH_TYPES:
                continue
            edits.append((c['body_open'] + 1, c['body_open'] + 1,
                          _ESCAPE_GUARD.format(v=c['var'])))

    out = check
    for s, e, repl in sorted(edits, key=lambda t: -t[0]):
        out = out[:s] + repl + out[e:]
    return out, 'transformed', json.dumps(
        {'probe_calls': [c for cs in wrapped for c in cs],
         'not_isolable': nonisolable})


# --------------------------------------------------------------------------
# PART 3 — execution on the VM, reusing the shipped machinery
# --------------------------------------------------------------------------

class _Rel:
    """The duck-typed relation `replay_on_patched` expects."""

    def __init__(self, name, check):
        self.name = name
        self.check = check


def _setup_pipeline_imports():
    here = Path(__file__).resolve()
    src_root = next(p for p in here.parents if (p / 'config.py').exists())
    sys.path.insert(0, str(src_root))


def cmd_run(args):
    _setup_pipeline_imports()
    import config                                              # noqa: E402
    from java.bug_context.patches import PatchSelector          # noqa: E402
    from java.execution.fuzz_runner import PatchedProjectBuilder  # noqa: E402
    from java.execution.jazzer import JazzerEnvironment          # noqa: E402
    from java.harness.build import HarnessBuilder                # noqa: E402
    from java.parsing.java_source import trigger_seed_literals   # noqa: E402
    from java.relations.relation_screen import (                 # noqa: E402
        measure_single_check, replay_on_patched, _screen_harness_source)

    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    legs = [json.loads(l) for l in open(args.input) if l.strip()]

    jz = JazzerEnvironment()
    api_jar = jz.ensure()
    driver_jar = jz.ensure_driver()
    builder = HarnessBuilder(jazzer_api_jar=api_jar)

    # ---- build every DISTINCT (bug, patch) pair once, serially -----------
    builds = {}
    for leg in legs:
        builds.setdefault(leg['patch_path'], []).append(leg)
    build_info = {}
    for patch_path, members in sorted(builds.items()):
        tag = Path(patch_path).stem
        print(f'\n===== building {tag} ({len(members)} leg(s))')
        sel = PatchSelector(
            patch_file=patch_path,
            correct='Dcorrect' in patch_path,
            overfitting='Doverfitting' in patch_path).select()
        ppb = PatchedProjectBuilder()
        ppb.verify_bug_reproduces(sel.buggy_dir)
        patched_dir = ppb.build_patched_dir(sel.buggy_dir, sel.patch_path)
        fq = patch_changed_class(patch_path)
        names = subclass_closure(sel.buggy_dir, fq)
        # warm the classpath cache so the worker threads never race on
        # `defects4j compile` for a shared checkout (Math-2 has two patches)
        builder.test_classpath(sel.buggy_dir)
        builder.test_classpath(patched_dir)
        from java.parsing.java_source import documented_exceptions_in_tree
        documented = documented_exceptions_in_tree(sel.buggy_dir,
                                                   sorted(names))
        build_info[patch_path] = {
            'tag': tag, 'buggy_dir': sel.buggy_dir,
            'patched_dir': patched_dir, 'patched_class': fq,
            'subclass_names': sorted(names),
            # String-keyed so the summary's json.dump can carry it; the
            # transform call site converts back to (class, method) tuples.
            'documented': {f'{c}#{m}': x
                           for (c, m), x in sorted(documented.items())},
        }
        print(f'  patched class {fq}; receiver types that count: '
              f'{sorted(names)}')
        if documented:
            print(f'  documented exceptions (tier-2 rejections): '
                  + '; '.join(f'{c}#{m}: {x}'
                              for (c, m), x in sorted(documented.items())))

    results_lock = threading.Lock()
    results_path = out_dir / 'results.jsonl'

    def emit(rec):
        with results_lock:
            with open(results_path, 'a') as fh:
                fh.write(json.dumps(rec) + '\n')

    def compiles(check, work_dir, package, imports, cls, subdir, record):
        src = _screen_harness_source(package, imports, cls, check,
                                     record_firings=record)
        try:
            b = builder.build(src, work_dir, output_subdir=subdir)
        except Exception as exc:
            return False, f'{type(exc).__name__}: {exc}'[:400]
        return b.compiled, (b.stderr or '')[-600:]

    def do_build(patch_path):
        info = build_info[patch_path]
        tag = info['tag']
        for leg in builds[patch_path]:
            trig = trigger_seed_literals(leg['failing_test_sources'])
            pkg, imps = leg['package'], leg['source_imports']
            for idx, rel in enumerate(leg['relations']):
                name = rel.get('name_in_screen') or rel['name']
                base = {
                    'leg_id': leg['leg_id'], 'suite': leg['suite'],
                    'leg': leg['leg'], 'label': leg['label'],
                    'project': leg['project'], 'bug_id': leg['bug_id'],
                    'patch_path': patch_path,
                    'patched_class': info['patched_class'],
                    'relation': name,
                    'contract': rel.get('contract', ''),
                    'kind': rel.get('kind', ''),
                    'original_check': rel['check'],
                }
                new_check, status, detail = transform_check(
                    rel['check'], name,
                    declared_types(rel['check']),
                    set(info['subclass_names']), info['patched_class'],
                    documented={tuple(k.split('#', 1)): v
                                for k, v in info['documented'].items()})
                base['transform_status'] = status
                base['transform_detail'] = detail
                if new_check is None:
                    base['executed'] = False
                    emit(base)
                    continue
                base['transformed_check'] = new_check
                sub = f'rex_{tag}_{leg["leg"]}_{idx}'
                ok, err = compiles(new_check, info['buggy_dir'], pkg, imps,
                                   'RexScreen', sub, False)
                if not ok:
                    base['executed'] = False
                    base['transform_status'] = 'skipped-does-not-compile'
                    base['transform_detail'] = err
                    emit(base)
                    continue
                counts = measure_single_check(
                    new_check, builder=builder, buggy_dir=info['buggy_dir'],
                    jazzer_standalone_jar=driver_jar,
                    jazzer_api_jar=api_jar, package=pkg, imports=imps,
                    runs=args.runs, timeout_seconds=args.timeout,
                    class_name='RexScreen', output_subdir=sub)
                base['buggy_checked'] = counts[0] if counts else None
                base['buggy_violated'] = counts[1] if counts else None
                base['fires_on_buggy'] = bool(counts and counts[1])

                pok, perr = compiles(new_check, info['patched_dir'], pkg,
                                     imps, 'RelReplay0',
                                     f'relreplay_pre_{tag}', True)
                if not pok:
                    base['executed'] = 'buggy-only'
                    base['patched_compile_error'] = perr
                    base['fires_on_patched'] = None
                    emit(base)
                    continue
                findings = replay_on_patched(
                    [_Rel(name, new_check)], builder=builder,
                    patched_dir=info['patched_dir'],
                    jazzer_standalone_jar=driver_jar, package=pkg,
                    imports=imps, jazzer_api_jar=api_jar,
                    trigger_literals=trig, runs=args.runs,
                    timeout_seconds=args.timeout)
                base['executed'] = True
                if findings:
                    f = findings[0]
                    base['fires_on_patched'] = True
                    base['patched_tier'] = f['tier']
                    base['patched_note'] = f['note']
                    base['firing_messages'] = f.get('fired_lines', [])
                    base['patched_checked'] = f.get('fuzz_checked')
                    base['patched_violated'] = f.get('fuzz_violated')
                else:
                    base['fires_on_patched'] = False
                    base['firing_messages'] = []
                base['fires_on_both'] = bool(base['fires_on_buggy']
                                             and base['fires_on_patched'])
                base['new_patched_only_firing'] = bool(
                    base['fires_on_patched'] and not base['fires_on_buggy'])
                emit(base)
        return tag

    started = time.time()
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        for tag in pool.map(do_build, sorted(builds)):
            print(f'  [done] {tag}')
    elapsed = time.time() - started

    rows = [json.loads(l) for l in open(results_path)]
    summary = {
        'legs': len(legs),
        'relations_extracted': sum(len(l['relations']) for l in legs),
        'relations_transformed': sum(
            1 for r in rows if r['transform_status'] == 'transformed'),
        'relations_executed': sum(1 for r in rows if r.get('executed') is True),
        'patched_only_firings': sum(
            1 for r in rows if r.get('new_patched_only_firing')),
        'fires_on_both': sum(1 for r in rows if r.get('fires_on_both')),
        'elapsed_seconds': round(elapsed, 1),
        'runs_budget': args.runs,
        'timeout_seconds': args.timeout,
        'builds': build_info,
    }
    with open(out_dir / 'summary.json', 'w') as fh:
        json.dump(summary, fh, indent=2)
    print(json.dumps(summary, indent=2))


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    sub = ap.add_subparsers(dest='cmd', required=True)

    e = sub.add_parser('extract')
    e.add_argument('--runs-root', required=True)
    e.add_argument('--suite', action='append', required=True)
    e.add_argument('--drr-root', default='/home/code/drr/Patches')
    e.add_argument('--out', required=True)
    e.set_defaults(func=cmd_extract)

    r = sub.add_parser('run')
    r.add_argument('--input', required=True)
    r.add_argument('--out', required=True)
    r.add_argument('--workers', type=int, default=4)
    r.add_argument('--runs', type=int, default=20000)
    r.add_argument('--timeout', type=int, default=45)
    r.set_defaults(func=cmd_run)

    c = sub.add_parser('cases')
    c.add_argument('--results', required=True, help='the study results.jsonl')
    c.add_argument('--runs-root', required=True,
                   help='runs-archive/runs — for each leg\'s failing tests')
    c.add_argument('--out', required=True)
    c.set_defaults(func=cmd_cases)

    p = sub.add_parser('report')
    p.add_argument('--results', required=True)
    p.add_argument('--runs-root', required=True)
    p.add_argument('--out', default=None)
    p.set_defaults(func=cmd_report)

    args = ap.parse_args()
    args.func(args)


if __name__ == '__main__':
    main()
