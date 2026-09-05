"""The core/backend boundary: one definition of every metric, no cycles.

The measurement layer is split in two:

  `src/metrics/core/`        language-agnostic CORE — the code-identity
                             model, the metric definitions
                             (`core/definitions.py`), the aggregation and
                             the paper tables. It computes numbers from
                             JSON a backend already wrote.
  `src/java/measurements/`   the Java BACKEND — the extractors that produce
                             that JSON (patch diffs, call graphs, JaCoCo
                             XML, Jazzer stack frames, the Defects4J run
                             layout), including `d4j_rcc_sweep/`, the
                             earlier Defects4J-specific RCC sweep. A future
                             C backend lives elsewhere and reuses the same
                             core.

The rule is one-way: a backend imports core, core never imports a backend.
`src/metrics/` holds nothing but `core/`: anything that knows about a
language's tools belongs in that language's backend.

Checked by reading import statements with `ast` rather than by importing
anything, so a mention in a docstring is not mistaken for an edge and a
function-level ("lazy") import is still seen.
"""
import ast
import os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, 'src')
CORE_DIR = os.path.join(SRC, 'metrics', 'core')
METRICS_DIR = os.path.join(SRC, 'metrics')


def _python_files(root, recursive=True):
    if not recursive:
        for name in sorted(os.listdir(root)):
            if name.endswith('.py'):
                yield os.path.join(root, name)
        return
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames
                             if d not in {'__pycache__', '.git'})
        for name in sorted(filenames):
            if name.endswith('.py'):
                yield os.path.join(dirpath, name)


def _imported_modules(path):
    """Every module name a file imports, as dotted strings; relative
    imports keep their leading dots."""
    with open(path, encoding='utf-8', errors='replace') as fh:
        source = fh.read()
    try:
        tree = ast.parse(source)
    except SyntaxError:
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


def _head(name):
    return name.lstrip('.').split('.')[0]


# --- core imports no backend ----------------------------------------------

def test_metrics_core_imports_nothing_from_java():
    """Core must stay language-agnostic: no `java.*` anywhere in it, not
    even lazily inside a function."""
    offenders = []
    for path in _python_files(CORE_DIR):
        for name in _imported_modules(path):
            if _head(name) == 'java':
                offenders.append((os.path.relpath(path, REPO), name))
    assert not offenders, (
        'metrics.core is the language-agnostic core; it must not import a '
        f'language backend: {offenders}')


def test_metrics_core_imports_no_java_only_third_party_tools():
    """The Java toolchain bindings belong to the backend, not to core."""
    java_only = {'javalang', 'fuzz_introspector'}
    offenders = []
    for path in _python_files(CORE_DIR):
        for name in _imported_modules(path):
            if _head(name) in java_only:
                offenders.append((os.path.relpath(path, REPO), name))
    assert not offenders, (
        'these Java-toolchain imports belong in src/java/measurements/: '
        f'{offenders}')


# --- src/metrics/ is core and nothing else ---------------------------------

def test_src_metrics_contains_nothing_but_core():
    """Everything language-specific has moved into a backend. What is left
    beside `core/` is the package marker and this layer's README, so a new
    module dropped in here is a mistake to catch now rather than a second
    home for the metrics later."""
    allowed_files = {'__init__.py', 'README.md'}
    allowed_dirs = {'core', '__pycache__'}
    strays = []
    for name in sorted(os.listdir(METRICS_DIR)):
        path = os.path.join(METRICS_DIR, name)
        if os.path.isdir(path):
            if name not in allowed_dirs:
                strays.append(name + '/')
        elif name not in allowed_files:
            strays.append(name)
    assert not strays, (
        'src/metrics/ holds the language-agnostic core and nothing else; '
        f'these do not belong here: {strays}')


def test_metrics_package_does_not_import_the_java_measurement_backend():
    """No module under `src/metrics/` may import the Java measurement
    backend — that direction is the backend's to import."""
    offenders = []
    for path in _python_files(METRICS_DIR):
        for name in _imported_modules(path):
            if 'measurements' in name.lstrip('.').split('.'):
                offenders.append((os.path.relpath(path, REPO), name))
    assert not offenders, (
        'src/metrics/ must not import java.measurements; the Java backend '
        f'imports core, not the other way round: {offenders}')


# --- the backend really does use core -------------------------------------

def test_the_java_backend_imports_core():
    """A guard on the premise: if this stops holding, the split has been
    undone and there are two definitions of the metrics again."""
    package = os.path.join(SRC, 'java', 'measurements')
    users = set()
    for path in _python_files(package):
        for name in _imported_modules(path):
            if name.lstrip('.').startswith('metrics.core'):
                users.add(os.path.basename(path))
    assert users, ('no module in src/java/measurements/ imports '
                   'metrics.core — the core/backend split is gone')


def test_the_old_paths_are_re_export_shims_of_the_moved_modules():
    """Backward compatibility: the four moved modules keep working under
    their old names, and resolve to the SAME module object. The metric
    definitions were `metrics.py` here and `core/ratios.py` after the
    split; they are `core/definitions.py` now."""
    from java.measurements import aggregate, locations, metrics, paper_tables
    from metrics.core import aggregate as core_aggregate
    from metrics.core import definitions as core_definitions
    from metrics.core import locations as core_locations
    from metrics.core import paper_tables as core_paper_tables

    assert locations is core_locations
    assert metrics is core_definitions
    assert aggregate is core_aggregate
    assert paper_tables is core_paper_tables


def test_the_d4j_rcc_sweep_lives_in_the_java_backend():
    """Kureha's sweep is Defects4J/JaCoCo/Jazzer-specific, so it is part of
    the Java backend, not of the shared core. It may import core and the
    rest of `java.*`; core must still not import it."""
    sweep_dir = os.path.join(SRC, 'java', 'measurements', 'd4j_rcc_sweep')
    assert os.path.isdir(sweep_dir), (
        'the d4j RCC sweep belongs under src/java/measurements/')

    offenders = []
    for path in _python_files(CORE_DIR):
        for name in _imported_modules(path):
            if 'd4j_rcc_sweep' in name.lstrip('.').split('.'):
                offenders.append((os.path.relpath(path, REPO), name))
    assert not offenders, (
        f'metrics.core must not import the Java sweep: {offenders}')
