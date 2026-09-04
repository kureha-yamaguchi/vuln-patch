"""Command line: measure an archived run, then print the tables.

    python -m java.measurements.cli <run_dir> \
        [--checkout_root DIR] [--d4j_home DIR] [--introspector]
        [--coverage] [--naive_run DIR]

For every leg of `run_dir` this writes the JSON files `metrics.py` reads:

    <leg>/measurements/patch_derived.json        MethodSet  (P, methods)
    <leg>/measurements/patch_derived_lines.json  LineSet    (P, lines)
    <leg>/measurements/root_cause.json           RootCause  (R + manifest)
    <leg>/measurements/coverage_<build>.json     Coverage   (F)  --coverage
        <build> is `buggy` / `patched` (the harnesses the acceptance gate
        KEPT) or `compiled` (every candidate that compiled, on the buggy
        build) — see the README, "Kept versus all compiled harnesses"
    <leg>/measurements/crash_sites.json          [CrashSite]     (C)
    <leg>/measurements/errors.json               what failed here, if anything

then `metrics.write_metrics` (-> ``<run_dir>/metrics.jsonl``),
`aggregate.aggregate` (-> ``<run_dir>/aggregate.json``) and, with
``--naive_run``, `aggregate.delta` (-> ``<run_dir>/delta.json``).

Two rules this module exists to enforce:

* **Firewall.**  `root_cause` — the only module that reads the developer's
  fix — is imported HERE and nowhere else.  `metrics.py` never sees it; it
  reads the JSON that this CLI wrote.
* **Robustness.**  Every stage of every leg is wrapped.  A leg whose
  checkout is missing, whose sibling module is not installed, or whose
  trace is unparsable records an error entry and the sweep continues; the
  metrics for that leg simply carry ``available`` flags set to false.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from typing import Dict, List, Optional

from . import aggregate as A
from . import locations as loc
from . import metrics as M

TRACE_FILE = 'trace.md'
CONTEXT_FILE = 'context.json'
ERRORS_FILE = 'errors.json'


# ---------------------------------------------------------------------------
# the buggy checkout
# ---------------------------------------------------------------------------

def default_checkout_root() -> str:
    """Where the pipeline keeps its Defects4J checkouts
    (``config.D4J_CHECKOUT_ROOT``, or ``$D4J_CHECKOUT_ROOT``, or /tmp/d4j)."""
    try:
        import config                                  # src/config.py
        return config.D4J_CHECKOUT_ROOT
    except Exception:
        return os.getenv('D4J_CHECKOUT_ROOT', '/tmp/d4j')


def ensure_buggy_checkout(project: str, bug_id: str,
                          checkout_root: Optional[str] = None) -> str:
    """Path to the buggy checkout of ``<project>-<bug_id>``, checking it out
    if it is not there yet.

    Reuses ``<checkout_root>/<Project>_<bug>_buggy`` when that directory
    already exists — the pipeline's own checkouts live there — and otherwise
    runs the same ``defects4j checkout`` the pipeline runs (see
    `bug_context.patches.PatchSelector._ensure_checkout`).  The checkout root
    must exist first: d4j resolves ``-w`` with ``Cwd::abs_path``, which
    returns undef when the parent is missing, and the checkout then dies.

    Isolated in one function so measurement runs (and tests) can replace it.
    Raises RuntimeError when the checkout is impossible."""
    if not project or not bug_id:
        raise RuntimeError('no project/bug_id in result.jsonl')
    root = checkout_root or default_checkout_root()
    buggy_dir = os.path.join(root, f'{project}_{bug_id}_buggy')
    if os.path.isdir(buggy_dir):
        return buggy_dir
    os.makedirs(root, exist_ok=True)
    proc = subprocess.run(
        ['defects4j', 'checkout', '-p', project, '-v', f'{bug_id}b',
         '-w', buggy_dir],
        capture_output=True)
    if proc.returncode != 0 or not os.path.isdir(buggy_dir):
        raise RuntimeError(
            f'defects4j checkout {project}-{bug_id} failed '
            f'(exit {proc.returncode}): '
            f'{proc.stderr.decode(errors="replace").strip()[:400]}')
    return buggy_dir


# ---------------------------------------------------------------------------
# lazy sibling modules (one accessor each, so tests can replace them)
# ---------------------------------------------------------------------------

def _mod_patch_derived():
    from . import patch_derived
    return patch_derived


def _mod_root_cause():
    """The ONLY import of `root_cause` in the codebase."""
    from . import root_cause
    return root_cause


def _mod_coverage():
    from . import coverage
    return coverage


def _mod_crash_sites():
    from . import crash_sites
    return crash_sites


def build_introspector_project(buggy_dir: str, language: str = 'jvm'):
    """A fuzz-introspector project object for a buggy checkout.

    `root_cause.compute(..., introspector_project=...)` wants the object
    `call_graph.function_map` takes, i.e. the ``light-project`` that
    ``analyse_end_to_end`` returns — the same one the pipeline's
    `bug_context.analysis.TargetAnalyzer._light_project_safe` builds.
    Without it root_cause returns seeds only (a valid, smaller R).
    Raises when fuzz-introspector is not installed or the parse fails."""
    from fuzz_introspector import commands as fi_commands
    _report, report = fi_commands.analyse_end_to_end(
        arg_language=language, target_dir=buggy_dir, module_only=True,
        dump_files=False)
    return report['light-project']


# ---------------------------------------------------------------------------
# one leg
# ---------------------------------------------------------------------------

def measure_leg(leg_dir: str, *, checkout_root: Optional[str] = None,
                d4j_home: Optional[str] = None, introspector: bool = False,
                coverage: bool = False) -> dict:
    """Produce every measurement JSON for one leg.

    Returns a status row: which stages produced a file, and the error text
    of the ones that did not.  Never raises."""
    leg_dir = os.path.abspath(leg_dir)
    mdir = os.path.join(leg_dir, M.MEASUREMENTS_DIR)
    os.makedirs(mdir, exist_ok=True)
    result = M.read_result(leg_dir)
    project = result.get('project')
    bug_id = (str(result.get('bug_id'))
              if result.get('bug_id') is not None else None)
    trace = os.path.join(leg_dir, TRACE_FILE)
    status = {'leg': os.path.basename(leg_dir), 'project': project,
              'bug_id': bug_id, 'done': [], 'errors': {}}

    def stage(name, fn):
        try:
            fn()
            status['done'].append(name)
        except Exception as exc:
            status['errors'][name] = f'{type(exc).__name__}: {exc}'

    # -- the buggy checkout (needed by root_cause and by the line mapping) --
    buggy: Dict[str, Optional[str]] = {'dir': None}

    def _checkout():
        buggy['dir'] = ensure_buggy_checkout(project, bug_id, checkout_root)
    stage('checkout', _checkout)

    # -- P ------------------------------------------------------------------
    pset: Dict[str, object] = {'methods': None}

    def _patch_derived():
        pd = _mod_patch_derived()
        ctx = os.path.join(leg_dir, CONTEXT_FILE)
        if os.path.isfile(ctx):
            ms = pd.from_context_json(ctx)
        elif os.path.isfile(trace):
            ms = pd.from_trace(trace)
        else:
            raise RuntimeError(f'neither {CONTEXT_FILE} nor {TRACE_FILE}')
        loc.dump(ms, os.path.join(mdir, M.F_PATCH_DERIVED))
        pset['methods'] = ms
    stage('patch_derived', _patch_derived)

    def _patch_derived_lines():
        if pset['methods'] is None:
            raise RuntimeError('no patch-derived method set')
        if not buggy['dir']:
            raise RuntimeError('no source root (checkout unavailable)')
        ls = _mod_patch_derived().lines_for(pset['methods'], buggy['dir'])
        loc.dump(ls, os.path.join(mdir, M.F_PATCH_DERIVED_LINES))
    stage('patch_derived_lines', _patch_derived_lines)

    # -- R ------------------------------------------------------------------
    # The call graph is a stage of its own: when it fails, root_cause still
    # runs and returns the seed ring alone, and the errors file says why the
    # caller/callee rings are missing rather than losing R entirely.
    fi: Dict[str, object] = {'project': None}
    if introspector:
        def _introspector():
            if not buggy['dir']:
                raise RuntimeError('no buggy checkout')
            fi['project'] = build_introspector_project(buggy['dir'])
        stage('introspector', _introspector)

    def _root_cause():
        if not buggy['dir']:
            raise RuntimeError('no buggy checkout')
        rc = _mod_root_cause().compute(
            project, bug_id, buggy['dir'], d4j_home=d4j_home,
            introspector_project=fi['project'],
            # The buggy checkout is the source the call graph was built
            # from, so it is also where a seed's callers are read from
            # when the frontend resolved none (neighbourhood.SourceScan).
            source_root=buggy['dir'])
        loc.dump(rc, os.path.join(mdir, M.F_ROOT_CAUSE))
    stage('root_cause', _root_cause)

    # -- F ------------------------------------------------------------------
    if coverage:
        def _coverage():
            _mod_coverage().collect_leg(leg_dir)
        stage('coverage', _coverage)

    # -- C ------------------------------------------------------------------
    def _crash_sites():
        cs = _mod_crash_sites()
        # A `--coverage` run saved the raw Jazzer output of every Jazzer
        # run under fuzz_out/, named `<harness>_<build>.txt` for all three
        # build tokens (`buggy`, `patched`, and `compiled` — the acceptance
        # gate's run of every candidate that compiled); that is the
        # complete record, so it wins over the trace, which keeps full
        # stack traces only inside the verifier's evidence blocks (patched
        # build). The build token comes from the file name, so a
        # `compiled` crash is attributed to that build and metrics.py
        # keeps it out of CSM's denominator.
        fo_dir = os.path.join(leg_dir, 'fuzz_out')
        sites = []
        if os.path.isdir(fo_dir):
            for fname in sorted(os.listdir(fo_dir)):
                if not fname.endswith('.txt') or '_' not in fname:
                    continue
                harness, build = fname[:-4].rsplit('_', 1)
                with open(os.path.join(fo_dir, fname), encoding='utf-8',
                          errors='replace') as fh:
                    text = fh.read()
                for site in cs.from_jazzer_output(text, build=build,
                                                  harness=harness):
                    site.source = f'fuzz_out/{fname}'
                    sites.append(site)
        if not sites:
            if not os.path.isfile(trace):
                raise RuntimeError(f'no {TRACE_FILE}')
            sites = cs.from_trace(trace)
        with open(os.path.join(mdir, M.F_CRASH_SITES), 'w') as fh:
            json.dump([s.to_dict() for s in sites], fh, indent=1)
    stage('crash_sites', _crash_sites)

    if status['errors']:
        with open(os.path.join(mdir, ERRORS_FILE), 'w') as fh:
            json.dump(status['errors'], fh, indent=1, sort_keys=True)
    return status


# ---------------------------------------------------------------------------
# whole run
# ---------------------------------------------------------------------------

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog='python -m java.measurements.cli',
        description='Measure an archived run: root-cause recovery, coverage '
                    'of the root cause, and crash-site match (paper Table 2).')
    p.add_argument('run_dir', help='a run directory holding <NN>_<patch>_<o|c> legs')
    p.add_argument('--checkout_root', default=None,
                   help='where <Project>_<bug>_buggy checkouts live '
                        '(default: the pipeline config / $D4J_CHECKOUT_ROOT)')
    p.add_argument('--d4j_home', default=None,
                   help='Defects4J install, passed through to root_cause')
    p.add_argument('--introspector', action='store_true',
                   help='build a fuzz-introspector call graph per bug so R '
                        'gets its caller/callee rings (slow; without it R is '
                        'the seed ring alone)')
    p.add_argument('--coverage', action='store_true',
                   help='collect JaCoCo coverage per leg (slow)')
    p.add_argument('--naive_run', default=None,
                   help='a naive-harness run to compare against; prints the '
                        'H_R - H_N table instead of the single-run table')
    return p


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)
    run_dir = os.path.abspath(args.run_dir)
    legs = M.leg_dirs(run_dir)
    print(f'{len(legs)} leg(s) in {run_dir}')

    statuses = []
    for leg in legs:
        st = measure_leg(leg, checkout_root=args.checkout_root,
                         d4j_home=args.d4j_home,
                         introspector=args.introspector,
                         coverage=args.coverage)
        statuses.append(st)
        note = (' ; '.join(f'{k}: {v}' for k, v in sorted(
            st['errors'].items())) or 'ok')
        print(f"  {st['leg']}: {'+'.join(st['done']) or '-'} [{note}]")

    rows = M.write_metrics(run_dir)
    print(f"metrics.jsonl: {len(rows)} row(s)")

    agg = A.aggregate(run_dir)
    with open(os.path.join(run_dir, 'aggregate.json'), 'w') as fh:
        json.dump(agg, fh, indent=1, sort_keys=True)

    if args.naive_run:
        d = A.delta(os.path.abspath(args.naive_run), run_dir)
        with open(os.path.join(run_dir, 'delta.json'), 'w') as fh:
            json.dump(d, fh, indent=1, sort_keys=True)
        print()
        print(A.render_markdown(d))
    else:
        print()
        print(A.render_markdown(agg))
    print(A.render_rcc_vs_caught(agg))
    return 0


if __name__ == '__main__':                              # pragma: no cover
    sys.exit(main())
