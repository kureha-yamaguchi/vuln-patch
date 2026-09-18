"""Offline candidate and paired, bug-weighted set-metric reporting. No model calls.

Five method-level set metrics, all against R0 — the methods the developer
fix changed:

    RCC = |R0 n F| / |R0|   how much of the root cause the harnesses ran
    RCP = |R0 n F| / |F|    how much of what the harnesses ran is root cause
    PSC = |P  n F| / |P|    how much of the patch-derived set they ran
    RCR = |R0 n P| / |R0|   how much of the root cause P recovered
    |F|                     the size of the fuzzer-reachable set itself

R0 is the denominator rather than the full ringed region because the
archived `root_cause.json` files disagree between arms on their
caller/callee rings while their seed rings are identical. A region that
changes with the arm cannot serve as a fixed denominator.

P is read from `measurements/patch_derived.json`. The analysis step writes
that file before any prompt is built, so it is the same file in all three
arms: RCR is a property of the patch, not of the arm. What each arm was
SHOWN of P differs; what the pipeline extracted does not.
"""
from __future__ import annotations

import hashlib
import json
import random
from collections import defaultdict
from pathlib import Path
from statistics import mean

from metrics.core import definitions as defs
from metrics.core import locations as loc
from java.measurements import coverage

#: Every per-leg number `paired_summary` averages over bugs. A metric is
#: None for a leg whose denominator is empty; the summary then averages the
#: bugs where it is defined and records how many those were.
METRICS = ('acceptance_rate', 'compile_rate', 'buggy_crash_rate',
           'candidate_rcc', 'candidate_rcc_compiled', 'accepted_set_rcc',
           'candidate_rcp', 'accepted_set_rcp',
           'candidate_psc', 'accepted_set_psc',
           'candidate_f_size', 'accepted_set_f_size',
           'rcr', 'r0_size', 'p_size')


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


def ratio(num, den):
    """A proportion, or None when the denominator is empty.

    An empty denominator is UNKNOWN, never zero: a harness that executed
    nothing has no budget to have spent well or badly."""
    return None if not den else num / den


def mean_or_none(values):
    """The mean of the values that are defined, or None when none are."""
    kept = [v for v in values if v is not None]
    return mean(kept) if kept else None


def resolve_patch_set(patch_set, population, index):
    """P mapped into one identity space: (set of refs, unmatched count).

    The JDK filter and the mislabelled-receiver filter come from
    `metrics.core.definitions` rather than being rewritten here, so both
    measurement implementations agree on what a project method is. P holds
    JDK callees (`StringBuilder.append`) that no project coverage report
    can ever contain; leaving them in would deflate PSC by construction.

    Unmatched refs are counted, not kept. The archived caller ring records
    a caller's SOURCE TEXT and not its declaring class (see
    `java.measurements.patch_derived`), so some callers carry no class and
    cannot be resolved against coverage."""
    if patch_set is None:
        return None, 0
    kept, _ = defs._strip_jdk(patch_set)
    kept, _ = defs._strip_mislabelled(kept, population)
    canon, unmatched = defs._map_set(kept, index, identity=False)
    return set(canon), unmatched


def score_leg(leg, budget, require_gate=True, replay_dir=None):
    """The five set metrics for one leg, per candidate and per harness set.

    RCC and PSC use each candidate's buggy acceptance execution. A
    pre-execution reject contributes zero to both: it ran nothing, so it
    reached none of R0 and none of P. RCP has |F| in its denominator, so a
    candidate that ran nothing leaves RCP undefined instead of zero, and
    every per-candidate mean below is taken over COMPILED candidates only.
    The legacy `candidate_rcc` keeps its all-attempts denominator.

    A compiled candidate with missing execution evidence is unknown and
    fails this leg; it never becomes zero.
    """
    leg = Path(leg)
    result = read_record(leg, budget)
    root = read_json(leg / 'measurements/root_cause.json')
    if require_gate and not (root.get('trigger_gate') or {}).get('passed'):
        raise ValueError('triggering-test region gate missing or failed')
    seeds = loc.MethodSet.from_dict(root['methods']).refs(loc.SEED)
    if not seeds:
        raise ValueError('R0 has no developer-changed methods; RCC is undefined')
    patch_file = leg / 'measurements/patch_derived.json'
    patch_set = (loc.MethodSet.from_dict(read_json(patch_file))
                 if patch_file.exists() else None)
    attempts = result['candidate_attempts']
    compiled = [a for a in attempts if a['compiled']]
    prefix = None
    if compiled:
        cp = read_json(leg / 'cov/classpath.json')
        prefix = coverage._normalise_prefix(cp.get('include_glob') or '') or None
    per_candidate = {}
    replayed = set()
    canonical = None
    patch_canon, patch_unmatched = None, 0
    accepted_f, compiled_f = set(), set()
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
        if patch_canon is None:
            # One identity space for the whole leg, fixed by the first
            # compiled candidate's method population, so that the unions
            # below union the same names.
            patch_canon, patch_unmatched = resolve_patch_set(
                patch_set, cov.all_methods, index)
        # Retain sizes and root hits, not N complete Closure-sized Coverage
        # objects. The two unions keep the full sets, and only those.
        per_candidate[name] = (canonical & cov.methods, len(cov.methods),
                               None if patch_canon is None else len(patch_canon & cov.methods))
        compiled_f |= cov.methods
        if attempt['accepted']:
            accepted_f |= cov.methods
    if canonical is None:
        canonical = set(seeds)
        # No candidate ran, so there is no coverage population. R0 becomes
        # the identity space, exactly as `metrics.core.definitions` does.
        patch_canon, patch_unmatched = resolve_patch_set(
            patch_set, None, loc.MethodIndex(sorted(canonical)))
    p_size = None if patch_canon is None else len(patch_canon)
    empty = (set(), 0, None if patch_canon is None else 0)
    rows = []
    for attempt in attempts:
        hits, f_size, p_hits = per_candidate.get(attempt['attempt_label'], empty)
        rows.append({
            **attempt,
            'rcc': len(hits) / len(canonical),
            'rcp': ratio(len(hits), f_size),
            'psc': None if p_hits is None else ratio(p_hits, p_size),
            'f_size': f_size,
            'coverage_origin': 'replay' if attempt['attempt_label'] in replayed else
                               ('original' if attempt['compiled'] else 'not_compiled'),
            'root_methods_hit': sorted(map(str, hits))})
    ran = [r for r in rows if r['compiled']]
    return {
        'attempts': budget, 'compiled': len(compiled), 'replayed_candidates': len(replayed),
        'crashed_buggy': sum(a['crashed_buggy'] for a in attempts),
        'accepted': sum(a['accepted'] for a in attempts),
        'compile_rate': len(compiled) / budget,
        'acceptance_rate': sum(a['accepted'] for a in attempts) / budget,
        'buggy_crash_rate': sum(a['crashed_buggy'] for a in attempts) / budget,
        'candidate_rcc': mean(r['rcc'] for r in rows),
        'candidate_rcc_compiled': mean_or_none([r['rcc'] for r in ran]),
        'candidate_rcp': mean_or_none([r['rcp'] for r in ran]),
        'candidate_psc': mean_or_none([r['psc'] for r in ran]),
        'candidate_f_size': mean_or_none([r['f_size'] for r in ran]),
        'accepted_set_rcc': len(canonical & accepted_f) / len(canonical),
        'accepted_set_rcp': ratio(len(canonical & accepted_f), len(accepted_f)),
        'accepted_set_psc': (None if patch_canon is None
                             else ratio(len(patch_canon & accepted_f), p_size)),
        'accepted_set_f_size': len(accepted_f),
        'compiled_set_rcc': len(canonical & compiled_f) / len(canonical),
        'compiled_set_rcp': ratio(len(canonical & compiled_f), len(compiled_f)),
        'compiled_set_psc': (None if patch_canon is None
                             else ratio(len(patch_canon & compiled_f), p_size)),
        'compiled_set_f_size': len(compiled_f),
        'rcr': (None if patch_canon is None
                else len(canonical & patch_canon) / len(canonical)),
        'r0_size': len(canonical), 'p_size': p_size,
        'p_unmatched': patch_unmatched,
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

    A metric that is undefined for a leg is left out of that bug's mean, and
    a bug with no defined leg is left out of the metric altogether. Each
    estimate records `defined_bugs` so a reader can see which bugs an
    interval describes. A paired difference needs both arms defined.
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
            entry[arm] = {m: mean_or_none([r.get(m) for r in samples[arm]])
                          for m in METRICS}
            for m in METRICS:
                values[arm][m].append(entry[arm][m])
        per_bug.append(entry)
    for arm in arms:
        if arm != 'HR':
            values[f'HR-{arm}'] = {
                m: [None if None in (r, n) else r - n
                    for r, n in zip(values['HR'][m], values[arm][m])]
                for m in METRICS}
    draws = {arm: {m: [] for m in METRICS} for arm in values}
    rng = random.Random(seed)
    for _ in range(bootstrap):
        indices = rng.choices(range(len(bugs)), k=len(bugs))
        for arm in values:
            for metric in METRICS:
                draws[arm][metric].append(
                    mean_or_none([values[arm][metric][i] for i in indices]))
    summary = {}
    for arm in values:
        summary[arm] = {}
        for m in METRICS:
            drawn = [d for d in draws[arm][m] if d is not None]
            summary[arm][m] = {
                'mean': mean_or_none(values[arm][m]),
                'defined_bugs': sum(v is not None for v in values[arm][m]),
                'ci95': ([percentile(drawn, .025), percentile(drawn, .975)]
                         if len(bugs) > 1 and drawn else None)}
    return {'n_bugs': len(bugs), 'n_legs_per_arm': len(rows) // len(arms),
            'estimates': summary, 'per_bug': per_bug,
            'totals': {arm: {k: sum(r[k] for r in rows if r['arm'] == arm)
                             for k in ('attempts', 'compiled', 'accepted', 'crashed_buggy')}
                       for arm in arms}}


ARM_LABELS = (('HN', 'H_N'), ('HN_C', 'H_N_C'), ('HR', 'H_R'))


def _cell(estimates, metric, digits=4):
    """One table cell: the mean, its interval, and any missing bugs."""
    stat = estimates.get(metric)
    if stat is None or stat['mean'] is None:
        return 'undefined'
    ci = stat['ci95']
    text = f"{stat['mean']:.{digits}f}"
    text += f" [{ci[0]:.{digits}f}, {ci[1]:.{digits}f}]" if ci else ' [CI undefined]'
    return text


def _table(summary, header, metrics, digits=4):
    lines = ['| Arm | ' + ' | '.join(header) + ' |',
             '|---' * (len(header) + 1) + '|']
    for arm, label in ARM_LABELS:
        if arm not in summary['estimates']:
            continue
        cells = [_cell(summary['estimates'][arm], m, digits) for m in metrics]
        lines.append('| ' + label + ' | ' + ' | '.join(cells) + ' |')
    return lines


def _undefined_note(summary, metric, label):
    """Name the metrics whose interval describes fewer than all the bugs."""
    short = []
    for arm, name in ARM_LABELS:
        stat = summary['estimates'].get(arm, {}).get(metric)
        if stat and stat['defined_bugs'] < summary['n_bugs']:
            short.append(f"{name} {stat['defined_bugs']}/{summary['n_bugs']}")
    return (f"{label} is undefined for an arm that accepted nothing; "
            f"bugs with a defined value: {', '.join(short)}.") if short else None


def render(report):
    lines = ['# Heldout harness comparison', '',
             'Method-level set metrics against the developer-changed methods (R0), '
             'on the buggy build.',
             'RCC = |R0 n F| / |R0|; RCP = |R0 n F| / |F|; PSC = |P n F| / |P|; '
             'RCR = |R0 n P| / |R0|; |F(H)| is the count of project methods the set ran.',
             'Means weight bugs equally, then patches/repetitions equally within each bug.',
             '95% percentile CIs resample paired bugs; they do not treat adaptive candidates as independent.',
             'Noncompiled candidates and empty accepted sets score zero for RCC and PSC.',
             'RCP divides by |F|, so a candidate that ran nothing leaves it undefined, '
             'and every per-candidate mean except the legacy RCC column is over compiled candidates only.',
             'P is the analysis step\'s extracted set. It is byte-identical across the three arms, '
             'so RCR describes the patch, not the arm; the arms differ in how much of P the prompt showed.',
             'Missing coverage or failed region gates make the report incomplete.', '',
             'HN = level B (no neighbourhood); HN_C = level C (function-only; no patch, failing tests, or neighbourhood); HR = full context.',
             'Semantic level C has no lifted-test oracle; its acceptance/crash rate measures escaping findings without that oracle.', '']
    if report.get('replay'):
        lines += ['**Remeasured coverage:** ' + report['replay']['protocol'],
                  f"{report['replay']['candidates']} rejected candidates selected for replay. "
                  'Candidate metrics combine original and replay executions; acceptance outcomes remain original.', '']
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
                  '### Outcomes and root-cause coverage', '']
        lines += _table(summary,
                        ['Compiled / tries', 'Accepted / tries', 'Mean candidate RCC',
                         'Mean RCC, compiled only', 'Accepted-set RCC'],
                        ['compile_rate', 'acceptance_rate', 'candidate_rcc',
                         'candidate_rcc_compiled', 'accepted_set_rcc'])
        lines += ['', '### Fuzzer-reachable set and precision', '']
        lines += _table(summary,
                        ['Mean candidate \\|F(H)\\|', 'Accepted-set \\|F(H)\\|',
                         'Mean candidate RCP', 'Accepted-set RCP'],
                        ['candidate_f_size', 'accepted_set_f_size',
                         'candidate_rcp', 'accepted_set_rcp'])
        note = _undefined_note(summary, 'accepted_set_rcp', 'Accepted-set RCP')
        lines += (['', note] if note else [])
        lines += ['', '### Patch-derived set', '']
        lines += _table(summary,
                        ['Mean candidate PSC', 'Accepted-set PSC', 'RCR',
                         'Mean \\|R0\\|', 'Mean \\|P\\|'],
                        ['candidate_psc', 'accepted_set_psc', 'rcr',
                         'r0_size', 'p_size'])
        lines += ['', 'RCR, |R0| and |P| do not depend on the harnesses, so the three arms '
                  'report the same numbers whenever every leg was scored.', '',
                  'Per-bug set sizes (H_R legs; the other arms read the same files):', '',
                  '| Bug | \\|R0\\| | \\|P\\| | RCR |', '|---|---:|---:|---:|']
        for entry in summary['per_bug']:
            arm = entry.get('HR', {})
            fmt = lambda v, d=1: 'n/a' if v is None else f'{v:.{d}f}'
            lines.append(f"| {entry['project']}-{entry['bug_id']} | "
                         f"{fmt(arm.get('r0_size'))} | {fmt(arm.get('p_size'))} | "
                         f"{fmt(arm.get('rcr'), 4)} |")
        lines += ['', 'Raw counts (pooled counts; table rates above are bug-weighted):', '']
        for arm, t in summary['totals'].items():
            lines.append(f"- {arm}: {t['accepted']}/{t['attempts']} accepted; "
                         f"{t['compiled']}/{t['attempts']} compiled; "
                         f"{t['crashed_buggy']} compiled and crashed before later acceptance checks.")
        lines.append('')
    return '\n'.join(lines) + '\n'
