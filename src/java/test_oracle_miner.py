"""Mine trusted oracles from a bug's existing test class — the SIBLING tests.

The trigger test is the ONE test that fails on the buggy version; the
lifted-assertion oracle already covers it. But the same test class holds
OTHER test methods that exercise the patched class/method with DIFFERENT
inputs — and (bar the reported failure) the buggy code already passes them,
so a correct patch must too. An overfit patch that special-cases only the
reported input still returns the wrong value for one of these siblings.

We surface those sibling test METHODS (whole bodies), not individual assert
lines: a test commonly stores the call result in a local and asserts on it
across several lines (`Fraction f = Fraction.getFraction(50,75);
assertEquals(2, f.getNumerator());`), which no statement-level scan can
reconnect. Handing the model the method body lets it lift the input->expected
pairs itself — the very thing it already does for the trigger test.

Provenance: these are the project's ordinary regression tests, independent of
the Dcorrect/Doverfitting labels — same source we lift the trigger from.
Nothing here reads the developer fix.

Pure text scanning (no JUnit, no compile). Fails soft: anything unparse-able
is simply not mined.
"""
from dataclasses import dataclass, field
from typing import List, Set
import re

_LINE_COMMENT = re.compile(r'//[^\n]*')
_BLOCK_COMMENT = re.compile(r'/\*.*?\*/', re.DOTALL)
_STRING_LIT = re.compile(r'"(?:\\.|[^"\\])*"')
_CHAR_LIT = re.compile(r"'(?:\\.|[^'\\])'")

_ASSERT_CALL = re.compile(
    r'\bassert(?:Equals|ArrayEquals|Same|NotSame|True|False|Null|NotNull)\b')

# Method declaration whose body we might mine. Optional @Test on the line(s)
# above; captures the method name.
_METHOD_DECL = re.compile(
    r'(?:@Test[^\n]*\n\s*)?'
    r'(?:public|protected|private)\s+'
    r'(?:static\s+)?(?:final\s+)?void\s+(\w+)\s*\([^;{]*\)\s*'
    r'(?:throws [\w\s,\.]+)?\{')


@dataclass
class MinedTest:
    """One sibling test method that exercises the patched code."""
    name: str
    source: str                 # full method source (declaration + body)
    num_asserts: int
    references: Set[str] = field(default_factory=set)


def _blank_strings(s: str) -> str:
    """Blank string/char literal CONTENT (length preserved) so a target name
    inside an assert message isn't mistaken for a real reference."""
    s = _STRING_LIT.sub(lambda m: '"' + ' ' * (len(m.group(0)) - 2) + '"', s)
    s = _CHAR_LIT.sub(lambda m: "'" + ' ' * (len(m.group(0)) - 2) + "'", s)
    return s


def _strip_comments(src: str) -> str:
    def blank(m: 're.Match') -> str:
        return re.sub(r'[^\n]', ' ', m.group(0))
    src = _BLOCK_COMMENT.sub(blank, src)
    src = _LINE_COMMENT.sub(lambda m: ' ' * len(m.group(0)), src)
    return src


def _skip_literal(src: str, i: int) -> int:
    quote = src[i]
    i += 1
    n = len(src)
    while i < n:
        if src[i] == '\\':
            i += 2
            continue
        if src[i] == quote:
            return i + 1
        i += 1
    return i


def _match_brace(src: str, open_idx: int) -> int:
    depth = 0
    i = open_idx
    n = len(src)
    while i < n:
        c = src[i]
        if c in '"\'':
            i = _skip_literal(src, i)
            continue
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def mine_sibling_tests(class_source: str,
                       target_tokens: List[str],
                       exclude_methods: List[str] = (),
                       max_methods: int = 4,
                       max_chars: int = 3000,
                       max_total_asserts: int = 10) -> List[MinedTest]:
    """Sibling test methods in `class_source` whose body references one of
    `target_tokens` (patched method or class simple-name) and contains at
    least one assertion. `exclude_methods` (the trigger tests) are skipped —
    their pairs are already lifted. Ranked by assertion count (more inputs =
    more chances to catch an overfit), then trimmed to `max_methods` /
    `max_chars` / `max_total_asserts` to bound prompt size.

    `max_total_asserts` is the hard cap the earlier per-method-count cap
    failed to provide: 4 methods once carried 36 assertions into a prompt
    and a previously-caught bug regressed to a miss (the flood of pairs
    distracted the generator from the trigger oracle). Methods that would
    push the running total past the cap are skipped in favour of smaller
    ones further down the ranking."""
    if not class_source or not target_tokens:
        return []
    stripped = _strip_comments(class_source)
    tokens = [t for t in dict.fromkeys(target_tokens) if t]
    token_res = {t: re.compile(r'\b%s\b' % re.escape(t)) for t in tokens}
    exclude = set(exclude_methods or ())

    found: List[MinedTest] = []
    for m in _METHOD_DECL.finditer(stripped):
        name = m.group(1)
        if name in exclude or not name.lower().startswith('test'):
            # Restrict to JUnit-style test methods; helpers rarely carry
            # liftable literal pairs and add noise.
            continue
        brace = stripped.find('{', m.end() - 1)
        if brace < 0:
            continue
        end = _match_brace(stripped, brace)
        if end < 0:
            continue
        body_code = _blank_strings(stripped[brace:end + 1])
        refs = {t for t, rx in token_res.items() if rx.search(body_code)}
        if not refs:
            continue
        n_assert = len(_ASSERT_CALL.findall(body_code))
        if n_assert == 0:
            continue
        source = class_source[m.start():end + 1] if end < len(class_source) \
            else stripped[m.start():end + 1]
        found.append(MinedTest(name=name, source=source,
                               num_asserts=n_assert, references=refs))

    found.sort(key=lambda t: t.num_asserts, reverse=True)
    out: List[MinedTest] = []
    budget = max_chars
    asserts_used = 0
    for t in found:
        if len(out) >= max_methods:
            break
        if len(t.source) > budget:
            continue
        if asserts_used + t.num_asserts > max_total_asserts:
            # Descending sort means later methods are smaller — keep
            # scanning for one that still fits under the assertion cap.
            continue
        out.append(t)
        budget -= len(t.source)
        asserts_used += t.num_asserts
    return out
