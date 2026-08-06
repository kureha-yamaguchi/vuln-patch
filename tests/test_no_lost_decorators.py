"""Every method that needs a decorator still has one.

WHY THIS FILE EXISTS. Stage 1's first roll died at
`RelationSynthesizer._parse() takes 1 positional argument but 2 were given`.
Cause: the delete-batch removal of `harden_for_soundness` sliced from its `def`
to "the next `def`" -- and the `@staticmethod` belonging to `_parse` sits ABOVE
that def, so it was swallowed. `self._parse(out)` then passed self as `out`.

Third time that slicing method bit in one batch: it swallowed a module constant
(_FACT_TAG_RE, 114 failures), absorbed a helper defined after a dropped test, and
now a decorator. Regex surgery on code cannot see structure; AST can.

633 tests passed with the bug present, because nothing exercised
`synthesize() -> _parse` without a live model. This file closes that gap
structurally rather than by adding one more path test.
"""
import ast
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))


def _methods_taking_no_self():
    """Methods whose first parameter is not self/cls -- they MUST carry
    staticmethod, or every call through an instance passes self as the first
    real argument."""
    bad = []
    for f in sorted((ROOT / 'src').rglob('*.py')):
        if '__pycache__' in str(f):
            continue
        try:
            tree = ast.parse(f.read_text())
        except SyntaxError:
            continue
        for cls in [n for n in ast.walk(tree) if isinstance(n, ast.ClassDef)]:
            for fn in [n for n in cls.body
                       if isinstance(n, (ast.FunctionDef, ast.AsyncFunctionDef))]:
                decs = {d.id if isinstance(d, ast.Name) else getattr(d, 'attr', '')
                        for d in fn.decorator_list}
                if decs & {'staticmethod', 'classmethod', 'property'}:
                    continue
                args = fn.args.posonlyargs + fn.args.args
                first = args[0].arg if args else None
                if first not in ('self', 'cls'):
                    bad.append(f'{f.relative_to(ROOT)}::{cls.name}.{fn.name} '
                               f'(first arg {first!r})')
    return bad


def test_no_method_lost_its_staticmethod_decorator():
    bad = _methods_taking_no_self()
    assert not bad, (
        'these methods take no self/cls and carry no staticmethod decorator, '
        'so an instance call passes self as the first argument:\n  '
        + '\n  '.join(bad))


def test_the_specific_regression_parse_is_static():
    """The one that actually broke stage 1's first roll."""
    from java.relations.relation_synth import RelationSynthesizer
    import inspect
    assert isinstance(
        inspect.getattr_static(RelationSynthesizer, '_parse'), staticmethod)
