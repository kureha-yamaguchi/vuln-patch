"""Offline candidate and paired, bug-weighted RCC reporting. No model calls."""
from __future__ import annotations

import hashlib
import json
import random
from collections import defaultdict
from pathlib import Path
from statistics import mean

from metrics.core import locations as loc
from java.measurements import coverage

METRICS = ('acceptance_rate', 'compile_rate', 'buggy_crash_rate',
           'candidate_rcc', 'accepted_set_rcc')


def read_json(path):
    return json.loads(Path(path).read_text())


def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True, allow_nan=False) + '\n')


def read_record(leg, budget):
    records = [json.loads(s) for s in (Path(leg) / 'result.jsonl').read_text().splitlines()
               if s.strip()]
    if len(records) != 1:
        raise ValueError('expected exactly one result record')
    result = records[0]
    attempts = result.get('candidate_attempts', [])
    if (result.get('status') != 'candidate_experiment'
            or result.get('candidate_budget') != budget or len(attempts) != budget):
        raise ValueError(f'incomplete candidate budget: wanted {budget}, got {len(attempts)}')
    expected = [f'attempt_{i:03d}' for i in range(1, budget + 1)]
    if [r.get('attempt_label') for r in attempts] != expected:
        raise ValueError('duplicate, missing or unordered candidate IDs')
    for r in attempts:
        if any(type(r.get(k)) is not bool for k in ('compiled', 'crashed_buggy', 'accepted')):
            raise ValueError('candidate outcomes must be booleans')
        if r['crashed_buggy'] and not r['compiled']:
            raise ValueError('uncompiled candidate marked as crashing')
        if r['accepted'] and not (r['compiled'] and r['crashed_buggy']):
            raise ValueError('accepted candidate did not compile and crash')
    return result


def score_leg(leg, budget, require_gate=True, replay_dir=None):
    """RCC uses R0 methods and each candidate's buggy acceptance execution.

    Pre-execution rejects contribute zero. A compiled candidate with missing
    execution evidence is unknown and fails this leg; it never becomes zero.
    """
    leg = Path(leg)
    result = read_record(leg, budget)
    root = read_json(leg / 'measurements/root_cause.json')
    if require_gate and not (root.get('trigger_gate') or {}).get('passed'):
        raise ValueError('triggering-test region gate missing or failed')
    seeds = loc.MethodSet.from_dict(root['methods']).refs(loc.SEED)
    if not seeds:
        raise ValueError('R0 has no developer-changed methods; RCC is undefined')
    attempts = result['candidate_attempts']
    compiled = [a for a in attempts if a['compiled']]
    prefix = None
    if compiled:
        cp = read_json(leg / 'cov/classpath.json')
        prefix = coverage._normalise_prefix(cp.get('include_glob') or '') or None
    hits_by_candidate = {}
    replayed = set()
    canonical = None
    for attempt in compiled:
        name = attempt['attempt_label']
        xml = leg / 'cov' / f'{name}_compiled.xml'
        raw = leg / 'fuzz_out' / f'{name}_compiled.txt'
        if not xml.exists() and replay_dir is not None:
            if attempt['accepted']:
                raise ValueError('rejected-only replay cannot replace accepted coverage')
            replay = Path(replay_dir) / name
            status = read_json(replay / 'status.json')
            if (status['attempt_label'] != name or
                    status['source_sha256'] != hashlib.sha256(
                        (leg / 'harness_src' / f'{name}.java').read_bytes()).hexdigest()):
                raise ValueError('replay does not match archived candidate')
            xml, raw = replay / 'coverage.xml', replay / 'fuzzer.log'
            replayed.add(name)
        cov = coverage.parse_jacoco_xml(str(xml), include_prefix=prefix)
        # Without raw output, throws that miss JaCoCo's exit probes cannot
        # be repaired consistently across all arms.
        coverage.repair_from_frames(cov, str(xml), raw.read_text(errors='replace'), prefix)
        index = loc.MethodIndex(cov.all_methods)
        resolved = {index.lookup(seed) for seed in seeds}
        if None in resolved or len(resolved) != len(seeds):
            raise ValueError('R0 methods unresolved or collapsed in coverage population')
        if canonical is not None and resolved != canonical:
            raise ValueError('inconsistent R0 method identities across candidate reports')
        canonical = resolved
        # Retain only root hits, not N complete Closure-sized Coverage objects.
        hits_by_candidate[name] = canonical & cov.methods
    if canonical is None:
        canonical = set(seeds)
    accepted_reached, all_reached = set(), set()
    rows = []
    for attempt in attempts:
        hits = hits_by_candidate.get(attempt['attempt_label'], set())
        row = {**attempt, 'rcc': len(hits) / len(canonical),
               'coverage_origin': 'replay' if attempt['attempt_label'] in replayed else
                                  ('original' if attempt['compiled'] else 'not_compiled'),
               'root_methods_hit': sorted(map(str, hits))}
        rows.append(row)
        all_reached |= hits
        if attempt['accepted']:
            accepted_reached |= hits
    return {
        'attempts': budget, 'compiled': len(compiled), 'replayed_candidates': len(replayed),
        'crashed_buggy': sum(a['crashed_buggy'] for a in attempts),
        'accepted': sum(a['accepted'] for a in attempts),
        'compile_rate': len(compiled) / budget,
        'acceptance_rate': sum(a['accepted'] for a in attempts) / budget,
        'buggy_crash_rate': sum(a['crashed_buggy'] for a in attempts) / budget,
        'candidate_rcc': mean(a['rcc'] for a in rows),
        'accepted_set_rcc': len(canonical & accepted_reached) / len(canonical),
        'compiled_set_rcc': len(canonical & all_reached) / len(canonical),
        'root_methods': sorted(map(str, canonical)), 'candidates': rows,
    }


def percentile(values, q):
    values = sorted(values)
    pos = (len(values) - 1) * q
    lo = int(pos)
    hi = min(lo + 1, len(values) - 1)
    return values[lo] + (values[hi] - values[lo]) * (pos - lo)


def paired_summary(rows, expected_bugs, bootstrap=10000, seed=20260916,
                   arms=('HN', 'HR')):
    """Equal bug weights, equal patch/repetition weights within each bug.

    Resample whole bugs with replacement, retaining every patch, candidate
    and arm of each sampled bug. These are population CIs across bugs, not
    claims of 30 independent model draws or run-to-run confidence intervals.
    """
    if len(set(arms)) != len(arms) or 'HR' not in arms:
        raise ValueError('expected distinct arms including HR')
    by_bug = defaultdict(lambda: defaultdict(list))
    for row in rows:
        if row['arm'] not in arms:
            raise ValueError(f"unexpected arm: {row['arm']}")
        by_bug[(row['project'], row['bug_id'])][row['arm']].append(row)
    bugs = sorted(by_bug)
    if set(bugs) != set(map(tuple, expected_bugs)):
        raise ValueError('scored bug population differs from frozen holdout')
    values = {arm: {m: [] for m in METRICS} for arm in arms}
    per_bug = []
    for bug in bugs:
        samples = by_bug[bug]
        keys = lambda arm: {(r['patch_id'], r['repetition']) for r in samples[arm]}
        for arm in arms:
            if (not samples[arm] or keys(arm) != keys('HR')
                    or len(keys(arm)) != len(samples[arm])):
                raise ValueError(f'unpaired or duplicate patches/repetitions for {bug}: {arm}')
        entry = {'project': bug[0], 'bug_id': bug[1]}
        for arm in arms:
            entry[arm] = {m: mean(r[m] for r in samples[arm]) for m in METRICS}
            for m in METRICS:
                values[arm][m].append(entry[arm][m])
        per_bug.append(entry)
    for arm in arms:
        if arm != 'HR':
            values[f'HR-{arm}'] = {
                m: [r - n for r, n in zip(values['HR'][m], values[arm][m])]
                for m in METRICS}
    draws = {arm: {m: [] for m in METRICS} for arm in values}
    rng = random.Random(seed)
    for _ in range(bootstrap):
        indices = rng.choices(range(len(bugs)), k=len(bugs))
        for arm in values:
            for metric in METRICS:
                draws[arm][metric].append(mean(values[arm][metric][i] for i in indices))
    summary = {}
    for arm in values:
        summary[arm] = {
            m: {'mean': mean(values[arm][m]),
                'ci95': [percentile(draws[arm][m], .025), percentile(draws[arm][m], .975)]
                if len(bugs) > 1 else None}
            for m in METRICS}
    return {'n_bugs': len(bugs), 'n_legs_per_arm': len(rows) // len(arms),
            'estimates': summary, 'per_bug': per_bug,
            'totals': {arm: {k: sum(r[k] for r in rows if r['arm'] == arm)
                             for k in ('attempts', 'compiled', 'accepted', 'crashed_buggy')}
                       for arm in arms}}


def render(report):
    lines = ['# Heldout harness comparison', '',
             'Method-level RCC against developer-changed methods (R0), on the buggy build.',
             'Means weight bugs equally, then patches/repetitions equally within each bug.',
             '95% percentile CIs resample paired bugs; they do not treat adaptive candidates as independent.',
             'Noncompiled candidates and empty accepted sets score zero when R0 is defined.',
             'Missing coverage or failed region gates make the report incomplete.', '',
             'HN = level B (no neighbourhood); HN_C = level C (function-only; no patch, failing tests, or neighbourhood); HR = full context.',
             'Semantic level C has no lifted-test oracle; its acceptance/crash rate measures escaping findings without that oracle.', '']
    if report.get('replay'):
        lines += ['**Remeasured coverage:** ' + report['replay']['protocol'],
                  f"{report['replay']['candidates']} rejected candidates selected for replay. "
                  'Candidate RCC combines original and replay executions; acceptance outcomes remain original.', '']
    if not report['complete']:
        lines += ['**INCOMPLETE — some selected measurements are missing.**', '']
        lines += [f"- {e['leg']}: {e['error']}" for e in report['errors']]
    if report.get('source_run'):
        lines += [f"Measured existing artifacts from `{report['source_run']}`.",
                  f"Excluded {len(report.get('excluded_patches', []))} failed or unfinished patch groups; "
                  'details are in manifest.json.', '']
    for kind, summary in report['groups'].items():
        pop = report.get('populations', {}).get(kind, {})
        if 'full_patches' in pop:
            scope = 'Subset' if pop['patches'] < pop['full_patches'] else 'Full heldout set'
            lines += [f"{kind.capitalize()} — **{scope}**: "
                      f"{pop['patches']}/{pop['full_patches']} certified patches; "
                      f"{len(pop['bugs'])}/{len(pop['full_bugs'])} frozen bugs.",
                      f"Selection: {pop['selection']}. "
                      'Subset intervals describe only the selected bugs.', '']
        lines += [f'## {kind.capitalize()}', '',
                  f"{summary['n_bugs']} bugs; {summary['n_legs_per_arm']} patch/repetition legs per arm.", '',
                  '| Arm | Compiled / tries | Accepted / tries | Mean candidate RCC | Accepted-set RCC |',
                  '|---|---:|---:|---:|---:|']
        for arm, label in (('HN', 'H_N'), ('HN_C', 'H_N_C'), ('HR', 'H_R')):
            if arm not in summary['estimates']:
                continue
            estimates = summary['estimates'][arm]
            cells = []
            for metric in ('compile_rate', 'acceptance_rate', 'candidate_rcc', 'accepted_set_rcc'):
                stat = estimates[metric]
                ci = stat['ci95']
                cells.append(f"{stat['mean']:.4f}" +
                             (f" [{ci[0]:.4f}, {ci[1]:.4f}]" if ci else ' [CI undefined]'))
            lines.append('| ' + label + ' | ' + ' | '.join(cells) + ' |')
        lines += ['', 'Raw counts (pooled counts; table rates above are bug-weighted):', '']
        for arm, t in summary['totals'].items():
            lines.append(f"- {arm}: {t['accepted']}/{t['attempts']} accepted; "
                         f"{t['compiled']}/{t['attempts']} compiled; "
                         f"{t['crashed_buggy']} compiled and crashed before later acceptance checks.")
        lines.append('')
    return '\n'.join(lines) + '\n'
