"""Pure Java-source lexing/parsing utilities, shared across the pipeline.

These are string-level helpers over Java source text — brace matching that
respects string/char literals, top-level argument splitting, and extraction
of literals / trigger lines from a method body. They carry NO prompt-building
or patch-analysis logic; they are the common substrate that prompts.py,
analysis.py, code_context.py and test_oracle_miner.py all previously
re-implemented (three near-identical brace matchers among them). Keeping them
here removes that duplication and gives "how do we parse Java text" one home.

Everything is best-effort and fails soft (returns -1 / '' / []) on malformed
input rather than raising — callers rely on that to degrade gracefully.
"""
import re
from typing import List, Optional


def skip_literal(src: str, i: int) -> int:
    """Given `src[i]` is an opening quote (" or '), return the index just
    PAST the closing quote, honouring backslash escapes."""
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


def match_brace(src: str, open_idx: int) -> int:
    """Index of the brace closing `src[open_idx]` ('{'), skipping string and
    char literals; -1 if unbalanced or `open_idx` is invalid."""
    if open_idx < 0 or open_idx >= len(src) or src[open_idx] != '{':
        return -1
    depth, i, n = 0, open_idx, len(src)
    while i < n:
        c = src[i]
        if c in '"\'':
            quote = c
            i += 1
            while i < n:
                if src[i] == '\\':
                    i += 1
                elif src[i] == quote:
                    break
                i += 1
        elif c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


# assertEquals / assertSame / assertArrayEquals call with its raw argument
# list captured up to the closing paren of the call (greedy enough for literal
# args; nested calls make the split heuristic bail for that call rather than
# mis-split).
ASSERT_EQ_RE = re.compile(
    r'\bassert(?:Equals|Same|ArrayEquals)\s*\(([^;]*?)\)\s*;')
# A literal argument: quoted string, char, number (int/float/exp, optional
# f/d/L suffix), or boolean.
LITERAL_ARG_RE = re.compile(
    r'^(?:"(?:[^"\\]|\\.)*"'
    r"|'(?:[^'\\]|\\.)'"
    r'|[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?[fFdDlL]?'
    r'|true|false)$')
BOUNDS_EXCEPTIONS = frozenset({
    'java.lang.StringIndexOutOfBoundsException',
    'java.lang.ArrayIndexOutOfBoundsException',
    'java.lang.IndexOutOfBoundsException',
})
INT_PARAM_RE = re.compile(r'\b(?:int|long)\b')


def split_top_level_args(arglist: str) -> List[str]:
    """Split an argument list on top-level commas (ignoring commas inside
    parens/quotes). Returns [] when the list contains a nested unbalanced
    construct we can't split safely."""
    args, depth, cur, i, n = [], 0, [], 0, len(arglist)
    while i < n:
        c = arglist[i]
        if c in '"\'':
            quote = c
            cur.append(c)
            i += 1
            while i < n:
                cur.append(arglist[i])
                if arglist[i] == '\\':
                    i += 1
                    if i < n:
                        cur.append(arglist[i])
                elif arglist[i] == quote:
                    break
                i += 1
        elif c in '([':
            depth += 1
            cur.append(c)
        elif c in ')]':
            depth -= 1
            cur.append(c)
        elif c == ',' and depth == 0:
            args.append(''.join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    if depth != 0:
        return []
    if cur:
        args.append(''.join(cur).strip())
    return args


def literal_arg_calls(method_source: str,
                      method_names: List[str]) -> List[str]:
    """Find `targetMethod("literal")` calls and return the literals.

    Conservative: matches only a single string-literal argument with no
    concatenation or extra args, so it stays silent on ambiguous inputs
    rather than guessing wrong."""
    if not method_names:
        return []
    name_alt = '|'.join(re.escape(n) for n in method_names)
    call_re = re.compile(
        r'\b(?:' + name_alt + r')\s*\(\s*'
        r'("(?:[^"\\]|\\.)*")'
        r'\s*\)'
    )
    return call_re.findall(method_source)


def lines_with_null(method_source: str, max_lines: int):
    hits = [ln.strip() for ln in method_source.splitlines()
            if 'null' in ln and '(' in ln]
    if not hits:
        return ''
    if len(hits) > max_lines:
        hits = hits[:max_lines] + ["// ... (further null-bearing calls omitted)"]
    return '\n'.join(hits)


def lines_with_oversized_ints(method_source: str, max_lines: int):
    """Lines where an integer argument exceeds the longest string literal on
    the same line (proxy for an out-of-range index). String literals are
    stripped before the int scan so their digits are not mistaken for
    numeric arguments."""
    str_lit = re.compile(r'"((?:[^"\\]|\\.)*)"')
    int_lit = re.compile(r'(?<![\w.])(-?\d+)(?![\w.])')
    hits = []
    for ln in method_source.splitlines():
        if '(' not in ln:
            continue
        strings = str_lit.findall(ln)
        if not strings:
            continue
        longest = max((len(s) for s in strings), default=0)
        if longest == 0:
            continue
        without_strings = str_lit.sub('""', ln)
        ints = [int(m) for m in int_lit.findall(without_strings)]
        if any(n > longest for n in ints if n != -1):
            hits.append(ln.strip())
    if not hits:
        return ''
    if len(hits) > max_lines:
        hits = hits[:max_lines] + ["// ... (further out-of-range calls omitted)"]
    return '\n'.join(hits)


def highlight_trigger_calls(method_source: str,
                            crash_types: List[str],
                            signatures: List[str],
                            method_names: Optional[List[str]] = None,
                            max_lines: int = 8):
    """Return (hint, lines) for the most likely trigger lines.

    Arm 0 (ground truth): single distinct string-literal call to a target
      method — use verbatim as anchor, fuzz the shape.
    Arm 1 (bounds): integer argument exceeds string-literal length.
    Arm 2 (NPE): null passed where an array is expected.
    Returns ('', '') when no arm fires; caller falls back to full body."""
    names = method_names or []

    literals = literal_arg_calls(method_source, names)
    distinct = sorted(set(literals))
    if len(distinct) == 1:
        hint = (
            "Anchor call — hard-code this literal verbatim as the first"
            " call in your harness, then use FuzzedDataProvider to"
            " generate additional inputs of the same shape:"
        )
        return hint, '\n'.join(distinct)

    has_int_param = any(INT_PARAM_RE.search(s) for s in signatures)
    is_bounds = any(c in BOUNDS_EXCEPTIONS for c in crash_types)

    if is_bounds and has_int_param:
        lines = lines_with_oversized_ints(method_source, max_lines)
        if lines:
            hint = (
                "Trigger lines: numeric argument exceeds the string"
                " length in the same call. Mirror these calls and use"
                " FuzzedDataProvider to vary the numeric arguments"
                " at or beyond the string length:"
            )
            return hint, lines

    has_array_param = any('[]' in s for s in signatures)
    if has_array_param:
        lines = lines_with_null(method_source, max_lines)
        if lines:
            hint = (
                "Trigger lines: null passed as an array element."
                " Mirror these calls:"
            )
            return hint, lines

    return '', ''


def candidate_anchor_literals(method_source: str,
                              method_names: List[str]) -> List[str]:
    """Unquoted string literals passed to target methods in the test source.

    Used as fallback anchor candidates for CrashInputExtractor when the
    runtime trace yields no quotable value from the exception message."""
    quoted = literal_arg_calls(method_source, method_names)
    return [q[1:-1] for q in quoted if len(q) >= 2]


def expected_assert_literals(method_source: str) -> List[str]:
    """EXPECTED-value literals from the test's equality assertions.

    These are the values the correct implementation is KNOWN to produce
    (JUnit convention puts the expected value first), which is what the
    relation verifier's trusted-values channel is for: a fired assertion
    that quotes one of them is checking developer-written ground truth, not
    a speculative relation. This is deliberately distinct from
    `candidate_anchor_literals`, which extracts the INPUT literals passed to
    target methods — feeding inputs into the trusted-values channel made the
    verifier's short-circuit protect the wrong thing.

    Heuristics per assert call (JUnit3/4 overloads):
      * assertEquals(expected, actual)          -> arg 0 if literal
      * assertEquals("msg", expected, actual)   -> arg 1 (string arg 0
        followed by 2 more args reads as the message-first overload)
      * assertEquals(expected, actual, delta)   -> arg 0 (numeric first)
    Non-literal expected args (locals, computed) are skipped — only a
    hard-coded literal is trustworthy provenance. Trivial literals (shorter
    than 3 characters, e.g. 0 / 1 / -1) are dropped: as substrings of a
    fired message they match spuriously."""
    out: List[str] = []
    for m in ASSERT_EQ_RE.finditer(method_source or ''):
        args = split_top_level_args(m.group(1))
        if len(args) < 2:
            continue
        cand = args[0]
        if (len(args) >= 3 and args[0].startswith('"')
                and not args[1].startswith('"')):
            # message-first overload: assertEquals("msg", expected, actual)
            cand = args[1]
        if not LITERAL_ARG_RE.match(cand):
            continue
        literal = cand[1:-1] if cand.startswith(('"', "'")) else cand
        # Strip a numeric suffix so the literal matches the value as a fired
        # message would print it (2.5f -> 2.5).
        if literal and literal[-1] in 'fFdDlL' and any(
                ch.isdigit() for ch in literal):
            literal = literal[:-1]
        if len(literal) >= 3 and literal not in out:
            out.append(literal)
    return out
