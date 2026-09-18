"""The firewall: the pipeline must never be able to see the developer fix.

`src/java/measurements/root_cause.py` is the only module allowed to read a
bug's developer patch. If the pipeline could import it — directly or through
any chain — a prompt, an oracle, a gate or a verdict could be influenced by
the answer, and every number the pipeline produces would stop meaning what
it claims to mean.

Three static checks, all by reading import statements rather than by
importing anything (an import-time check would itself create the edge it is
testing for):

  1. No .py file under src/ outside src/java/measurements/ imports the
     measurements package, apart from the exemptions listed below.
  2. Nothing the pipeline entry point (`java.run`) can reach, through any
     chain of imports, is `java.measurements.root_cause`. This is the check
     that actually protects the measurement: it holds even if check 1 gains
     further exemptions.
  3. Inside the package, only the CLI imports root_cause.
"""
import ast
import os

import pytest

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, 'src')
PACKAGE_DIR = os.path.join(SRC, 'java', 'measurements')

# The one module inside the package that may import root_cause.
CLI_BASENAME = 'cli.py'

# Pipeline entry point; check 2 starts here.
PIPELINE_ROOTS = ('java.run',)

# The quarantined module itself.
QUARANTINED = 'java.measurements.root_cause'

# Deliberate, reviewed exceptions to check 1, as (file relative to the repo,
# module imported). Each one must be a module that cannot reach the
# developer fix — check 2 is what enforces that, so an entry here weakens
# the layering rule but never the measurement's integrity.
#
#   fuzz_runner -> java.measurements.coverage
#       `jazzer_coverage_args` is a pure argv builder (two Jazzer flags,
#       no I/O, no patch reading) and the fuzz runner is where the flags
#       have to be added. Keeping the flag names next to the code that
#       parses the resulting .exec files is why it lives there.
ALLOWED_PIPELINE_IMPORTS = set()   # none: jazzer_coverage_args lives pipeline-side


# --- reading imports without importing anything ---------------------------

def _python_files(root):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames
                             if d not in {'__pycache__', '.git'})
        for name in sorted(filenames):
            if name.endswith('.py'):
                yield os.path.join(dirpath, name)


def _imported_modules(path):
    """Every module name a file imports, as dotted strings.

    Uses the AST so a mention of `java.measurements` in a comment or a
    docstring — this repo has several — is not mistaken for an import, and
    so that function-level ("lazy") imports are seen too. Relative imports
    keep their leading dots."""
    with open(path, encoding='utf-8', errors='replace') as fh:
        source = fh.read()
    try:
        tree = ast.parse(source)
    except SyntaxError:                     # not a module we can vouch for
        return []
    names = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            names.extend(alias.name for alias in node.names)
        elif isinstance(node, ast.ImportFrom):
            base = '.' * (node.level or 0) + (node.module or '')
            names.append(base)
            names.extend(f'{base}.{alias.name}' for alias in node.names)
    return sorted(set(names))


def _module_name(path):
    """`src/java/execution/diffcov.py` -> `java.execution.diffcov`;
    a package's `__init__.py` -> the package name."""
    rel = os.path.relpath(path, SRC)[:-3]
    parts = rel.split(os.sep)
    if parts[-1] == '__init__':
        parts = parts[:-1]
    return '.'.join(parts)


def _mentions_measurements(name):
    return 'measurements' in name.lstrip('.').split('.')


# --- 1. layering ----------------------------------------------------------

def test_no_pipeline_module_imports_the_measurements_package():
    offenders = []
    for path in _python_files(SRC):
        if path.startswith(PACKAGE_DIR + os.sep):
            continue
        rel = os.path.relpath(path, REPO)
        for name in _imported_modules(path):
            if not _mentions_measurements(name):
                continue
            # `from java.measurements.coverage import f` yields both the
            # module and `...coverage.f`; an exemption on the module covers
            # the names taken out of it.
            if any(rel == a and (name == m or name.startswith(m + '.'))
                   for a, m in ALLOWED_PIPELINE_IMPORTS):
                continue
            offenders.append((rel, name))
    assert not offenders, (
        'these pipeline files import the measurement layer, which is how the '
        'developer fix would reach a prompt/oracle/gate/verdict: '
        f'{offenders}')


# --- 2. reachability of the quarantined module ----------------------------

def _import_graph():
    """`{module name: {modules it imports}}` over everything under src/.

    A name is resolved by trying it whole and then dropping trailing
    components, so `from java.measurements import root_cause` and
    `import java.measurements.root_cause` both land on the same node."""
    modules = {_module_name(p): p for p in _python_files(SRC)}
    graph = {}
    for name, path in modules.items():
        edges = set()
        for imported in _imported_modules(path):
            if imported.startswith('.'):      # relative: resolve in-package
                pkg = name.rsplit('.', 1)[0] if '.' in name else ''
                imported = (pkg + '.' + imported.lstrip('.')).strip('.')
            parts = imported.split('.')
            for cut in range(len(parts), 0, -1):
                cand = '.'.join(parts[:cut])
                if cand in modules and cand != name:
                    edges.add(cand)
                    break
        graph[name] = edges
    return graph


def test_the_pipeline_cannot_reach_root_cause_through_any_import_chain():
    graph = _import_graph()
    for root in PIPELINE_ROOTS:
        if root not in graph:
            pytest.fail(f'pipeline entry point {root} not found under src/')
        seen, queue, chain = {root}, [root], {root: [root]}
        while queue:
            node = queue.pop(0)
            for nxt in sorted(graph.get(node, ())):
                if nxt in seen:
                    continue
                seen.add(nxt)
                chain[nxt] = chain[node] + [nxt]
                queue.append(nxt)
        assert QUARANTINED not in seen, (
            f'{root} can reach {QUARANTINED}, the one module that reads the '
            f'developer fix, via {" -> ".join(chain.get(QUARANTINED, []))}')


# --- 3. inside the package ------------------------------------------------

def test_only_the_cli_imports_root_cause_inside_the_package():
    offenders = []
    for path in _python_files(PACKAGE_DIR):
        if os.path.basename(path) in (CLI_BASENAME, 'root_cause.py'):
            continue
        for name in _imported_modules(path):
            if 'root_cause' in name.split('.'):
                offenders.append((os.path.relpath(path, REPO), name))
    assert not offenders, (
        'only the measurement CLI may import root_cause (it reads the '
        f'developer fix): {offenders}')


def test_root_cause_states_the_firewall_reason():
    """A sanity check on the premise: root_cause.py is the file that names
    the developer patch location, and it explains why it is quarantined."""
    with open(os.path.join(PACKAGE_DIR, 'root_cause.py'),
              encoding='utf-8', errors='replace') as fh:
        text = fh.read()
    assert 'src.patch' in text
    assert 'FIREWALL' in text.split('"""')[1], \
        'root_cause.py must state the firewall reason in its module docstring'
