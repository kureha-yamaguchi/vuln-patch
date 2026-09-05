"""One spelling for one method, shared by every set the metrics compare.

The two sides of RCC name a method differently:

  * R-hat comes from a Java AST. It knows the class, the method name and the
    parameter types as the SOURCE writes them: ``Widget``, ``resize``,
    ``('String', 'int')``.
  * F(H) comes from a JaCoCo report, read through fuzz-introspector. It
    writes one string, decoded from the bytecode descriptor:
    ``[org.example.Widget].resize(java.lang.String,int)``.

`MethodKey` is what both sides reduce to. Three rules make them agree:

  1. A nested class is written with a dot. The bytecode says ``Widget$Inner``
     and the AST says ``Widget.Inner``.
  2. A parameter type keeps its simple name only. ``java.lang.String`` and
     ``String`` are the same type, and only one side ever says the package.
  3. A varargs parameter is an array. The source writes ``int...`` and the
     descriptor writes ``int[]``.

Overloads stay apart, because the parameter types belong to the key.
"""
from dataclasses import dataclass
from typing import Optional, Tuple

from java.bug_context.call_graph import (
    fi_method_name, mangled_param_types, receiver_of, simple_type,
)

# The JVM, and therefore JaCoCo, calls a constructor `<init>`. javalang names
# it after its own class. We keep the JVM spelling, because it is the one that
# cannot collide with a real method name.
CONSTRUCTOR = '<init>'


def normalise_type(raw: str) -> str:
    """A parameter type in the one spelling both sides can produce.

    Applying this twice changes nothing, so it is safe on a string that
    `mangled_param_types` has already reduced."""
    name = raw.strip().replace('$', '.')
    if name.endswith('...'):
        name = name[:-3] + '[]'
    return simple_type(name)


@dataclass(frozen=True)
class MethodKey:
    """One method or constructor, named the same way on both sides."""
    class_name: str               # fully qualified, nested types joined by '.'
    method_name: str              # simple name, or '<init>' for a constructor
    param_types: Tuple[str, ...]  # simple type names, in declaration order

    @property
    def arity(self) -> int:
        return len(self.param_types)

    @property
    def loose(self) -> Tuple[str, str, int]:
        """The same method without its exact parameter types.

        A fallback only. `metrics.rcc` uses it when no exact match exists,
        so that one unusual type spelling cannot turn a covered method into
        a missed one. It cannot separate two overloads of equal arity."""
        return (self.class_name, self.method_name, self.arity)

    def __str__(self) -> str:
        cls = self.class_name.split('.')[-1]
        return f"{cls}.{self.method_name}({', '.join(self.param_types)})"


def key_from_changed_method(changed) -> MethodKey:
    """`MethodKey` for one `java.execution.diffcov.ChangedMethod`."""
    class_name = changed.class_name.replace('$', '.')
    name = changed.method_name
    if name == class_name.split('.')[-1]:
        name = CONSTRUCTOR
    return MethodKey(
        class_name=class_name,
        method_name=name,
        param_types=tuple(normalise_type(p) for p in changed.param_types),
    )


def key_from_mangled(mangled: str) -> Optional[MethodKey]:
    """`MethodKey` for one fuzz-introspector name.

    None when the name carries no ``[pkg.Class]`` receiver. Such a name is a
    JDK static or a malformed entry, and neither can be in R-hat."""
    receiver = receiver_of(mangled)
    if not receiver:
        return None
    return MethodKey(
        class_name=receiver.replace('$', '.'),
        method_name=fi_method_name(mangled),
        param_types=tuple(normalise_type(p)
                          for p in mangled_param_types(mangled)),
    )
