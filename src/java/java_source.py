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


# ---------------------------------------------------------------------------
# P0.2: self-swallow lint.
#
# A check raises its alarm by throwing (RuntimeException("... violated ..."),
# FuzzerSecurityIssue*, AssertionError). In the Lang-7 runs every generated
# check threw its alarm INSIDE its own catch-everything block, so the alarm
# was thrown, immediately caught, and discarded — a rule that should fire on
# every buggy input measured 0 firings and was promoted as "well-behaved".
# This lint finds alarm throws that cannot escape: lexically inside a try
# whose catch clause catches Throwable/Exception/RuntimeException and never
# rethrows.

_ALARM_THROW_RE = re.compile(
    r'\bthrow\b[^;]{0,400}?(?:violated|FuzzerSecurityIssue'
    r'|semantic\s+mismatch)', re.I | re.S)
_BROAD_CATCH_TYPES = ('Throwable', 'Exception', 'RuntimeException')
# Word-bounded: `catch (NumberFormatException e)` must NOT count as broad
# just because 'Exception' is a substring of the type name.
_BROAD_CATCH_RE = re.compile(
    r'\b(?:java\.lang\.)?(?:Throwable|Exception|RuntimeException)\b')


def strip_comments(src: str) -> str:
    """Remove // and /* */ comments, preserving string/char literals and
    the character count of nothing in particular (positions shift)."""
    out = []
    i, n = 0, len(src or '')
    while i < n:
        c = src[i]
        if c in '"\'':
            j = skip_literal(src, i)
            out.append(src[i:j])
            i = j
        elif src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j < 0 else j
        elif src.startswith('/*', i):
            j = src.find('*/', i)
            out.append(' ')
            i = n if j < 0 else j + 2
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def _try_blocks(src: str):
    """[(open_idx, close_idx, [(catch_params, catch_body), ...])] for every
    `try { ... }` in src (comments must already be stripped)."""
    blocks = []
    for m in re.finditer(r'\btry\b', src):
        i, n = m.end(), len(src)
        while i < n and src[i] in ' \t\r\n':
            i += 1
        if i < n and src[i] == '(':          # try-with-resources header
            depth = 0
            while i < n:
                if src[i] in '"\'':
                    i = skip_literal(src, i) - 1
                elif src[i] == '(':
                    depth += 1
                elif src[i] == ')':
                    depth -= 1
                    if depth == 0:
                        i += 1
                        break
                i += 1
            while i < n and src[i] in ' \t\r\n':
                i += 1
        if i >= n or src[i] != '{':
            continue
        open_idx = i
        close_idx = match_brace(src, open_idx)
        if close_idx < 0:
            continue
        catches = []
        j = close_idx + 1
        while True:
            mm = re.match(r'\s*catch\s*\(', src[j:])
            if not mm:
                break
            k = j + mm.end()
            depth, start = 1, k
            while k < n and depth:
                if src[k] in '"\'':
                    k = skip_literal(src, k) - 1
                elif src[k] == '(':
                    depth += 1
                elif src[k] == ')':
                    depth -= 1
                k += 1
            params = src[start:k - 1]
            b = src.find('{', k)
            if b < 0:
                break
            bend = match_brace(src, b)
            if bend < 0:
                break
            catches.append((params, src[b + 1:bend]))
            j = bend + 1
        blocks.append((open_idx, close_idx, catches))
    return blocks


def violation_swallowed(source: str) -> Optional[str]:
    """Return a human-readable reason if ANY alarm throw in `source` is
    lexically inside a try whose broad catch (Throwable / Exception /
    RuntimeException) absorbs it without rethrowing; None when every alarm
    can escape. Walks enclosing tries innermost-outward: a rethrowing broad
    catch passes the alarm to the next enclosing try."""
    src = strip_comments(source or '')
    alarm_positions = [m.start() for m in _ALARM_THROW_RE.finditer(src)]
    if not alarm_positions:
        return None
    tries = _try_blocks(src)
    for pos in alarm_positions:
        enclosing = sorted(
            (t for t in tries if t[0] < pos < t[1]),
            key=lambda t: t[1] - t[0])          # innermost first
        for open_idx, _close_idx, catches in enclosing:
            handler = next(
                (body for params, body in catches
                 if _BROAD_CATCH_RE.search(params)),
                None)
            if handler is None:
                continue                        # escapes to outer try
            if 'throw' not in handler:
                line = src[:pos].count('\n') + 1
                return (f'the alarm throw near line {line} is inside a '
                        f'try whose catch ({_catch_types_of(catches)}) '
                        f'swallows it — the alarm can never be heard. '
                        f'Rethrow the violation from that catch, or move '
                        f'the throw outside the try.')
            # broad catch rethrows: the alarm continues outward — the next
            # enclosing try must be checked too, it may swallow it there
    return None


def _catch_types_of(catches) -> str:
    return ', '.join(p.strip().split()[0] if p.strip() else '?'
                     for p, _b in catches)


# ---------------------------------------------------------------------------
# P0.3: caught-crash re-throws must carry the original as their cause.
#
# A harness may catch a library crash and re-throw it as its own alarm
# (FuzzerSecurityIssue*). Without the original attached as the `cause`,
# the alarm's stack trace has no `Caused by:` chain, the underlying
# crash's identity is erased, and the attribution check can no longer ask
# "does this same crash happen on the unpatched build too?" — that is how
# the pre-existing Chart-26 text-measuring crash was laundered into a
# false alarm.

_SECISSUE_NEW_RE = re.compile(r'new\s+(FuzzerSecurityIssue\w*)\s*\(')


def rethrow_without_cause(source: str) -> Optional[str]:
    """Return a reason string if any `new FuzzerSecurityIssue*(...)`
    constructed inside a `catch (X e) { ... }` block does not pass the
    caught exception as a constructor argument (cause); None otherwise.
    String-concatenating `e` into the message does NOT count — that
    copies the text but erases the stack chain."""
    src = strip_comments(source or '')
    for _open, _close, catches in _try_blocks(src):
        for params, body in catches:
            var = params.strip().split()[-1] if params.strip() else ''
            if not var:
                continue
            for m in _SECISSUE_NEW_RE.finditer(body):
                # capture the argument list via paren matching
                k, depth, n = m.end(), 1, len(body)
                start = k
                while k < n and depth:
                    if body[k] in '"\'':
                        k = skip_literal(body, k) - 1
                    elif body[k] == '(':
                        depth += 1
                    elif body[k] == ')':
                        depth -= 1
                    k += 1
                args = body[start:k - 1]
                # the caught variable must appear OUTSIDE string literals
                # as a standalone argument (typically the last one), not
                # inside a "..."+var message concatenation. Strip strings,
                # then require `var` after a '(' or ',' boundary.
                stripped = re.sub(r'"(?:[^"\\]|\\.)*"', '""', args)
                ok = (re.search(r'(?:^|,)\s*' + re.escape(var)
                                + r'\s*(?:,|$)', stripped.strip())
                      or re.search(r'\.initCause\s*\(\s*' + re.escape(var)
                                   + r'\s*\)', body))
                if not ok:
                    return (f'a {m.group(1)} is thrown inside '
                            f'`catch ({params.strip()})` without passing '
                            f'`{var}` as its cause argument — the caught '
                            f'crash\'s identity is erased and attribution '
                            f'cannot check it against the unpatched build. '
                            f'Use `new {m.group(1)}("<message>", {var})`.')
    return None


# ---------------------------------------------------------------------------
# P0.4: per-oracle IDs.
#
# A harness usually contains several checks. Acceptance is whole-harness
# ("did ANYTHING fire on buggy?"), so a check that never ran can ride in on
# the strength of an earlier one and meet its first-ever execution on the
# correct patch (the Chart-26 attempt_003 false alarm). Naming every alarm
# lets us ask WHICH check fired where. Two accepted shapes:
#   [oracle:<id>] ...            (mandated prefix for harness alarms)
#   relation <name> violated ...  (the synthesis format, already named)
#   "[oracle:" + id + "] ..."     (ID built at runtime; resolved in output)

_ORACLE_ID_RE = re.compile(r'\[oracle:([-\w]+)\]')
_RELATION_ID_RE = re.compile(r'relation\s+([-\w]+)\s+violated')
# A dynamically-built ID: a string literal ending in `[oracle:` followed by
# concatenation, e.g. `"[oracle:" + oracleId + "] ..."`. Perfectly valid at
# runtime (the fuzzer output carries the resolved ID, which is what the
# latent scan and acceptance parse) — rejecting it cost Chart-3-o every
# harness attempt in the p23gate run.
_DYNAMIC_ORACLE_ID_RE = re.compile(r'\[oracle:"\s*\+')
_ALARM_STMT_RE = re.compile(
    r'throw\s+new\s+[\w.]*(?:FuzzerSecurityIssue\w*|RuntimeException)'
    r'\s*\(', re.S)


def oracle_ids_in_text(text: str) -> set:
    """Every oracle ID mentioned in `text` (harness source OR fuzzer
    output), under either accepted shape."""
    ids = set(_ORACLE_ID_RE.findall(text or ''))
    ids.update(_RELATION_ID_RE.findall(text or ''))
    return ids


def alarm_ids_missing(source: str) -> Optional[str]:
    """Return a reason string if any alarm throw in `source` has a message
    that carries NO oracle ID (neither `[oracle:<id>]` prefix nor
    `relation <name> violated`); None when every alarm is identifiable.
    Alarms without an ID cannot be told apart at acceptance, so a
    never-exercised check cannot be flagged."""
    src = strip_comments(source or '')
    for m in _ALARM_STMT_RE.finditer(src):
        # the throw's argument region: up to the statement's semicolon
        end = src.find(';', m.end())
        stmt = src[m.start():end if end > 0 else m.end() + 400]
        if ('violated' not in stmt and 'violation' not in stmt
                and 'semantic mismatch' not in stmt
                and 'FuzzerSecurityIssue' not in stmt):
            continue                       # not an alarm (plain rethrow)
        if _DYNAMIC_ORACLE_ID_RE.search(stmt):
            continue                       # ID built at runtime — named
        if not oracle_ids_in_text(stmt):
            line = src[:m.start()].count('\n') + 1
            snippet = ' '.join(stmt.split())[:120]
            return (f'the alarm near line {line} has no oracle ID '
                    f'(`{snippet}...`) — start its message with '
                    f'"[oracle:<short-id>]" so acceptance can tell '
                    f'which check earned its place')
    return None


# ---------------------------------------------------------------------------
# Harness-bug lint: Math.abs(consume…()) used before % is NOT a safe index.
# Math.abs(Integer.MIN_VALUE) is negative (two's complement has no positive
# counterpart), so `Math.abs(data.consumeInt()) % n` eventually produces a
# negative index and the harness itself crashes with an
# ArrayIndexOutOfBoundsException that has nothing to do with the patch —
# the sole patched-side firing of Lang-41-o in the p23gate run was exactly
# this, and it cost the leg its verdict.

_NEG_MOD_RE = re.compile(
    r'Math\s*\.\s*abs\s*\(\s*\w+\s*\.\s*consume\w*\s*\([^()]*\)\s*\)\s*%')


def negative_modulo_index(source: str) -> Optional[str]:
    """Return a reason string when the harness computes an index as
    `Math.abs(<fuzzed int>) % n` (negative for Integer.MIN_VALUE); None
    when the pattern is absent."""
    src = strip_comments(source or '')
    m = _NEG_MOD_RE.search(src)
    if not m:
        return None
    line = src[:m.start()].count('\n') + 1
    snippet = ' '.join(m.group(0).split())
    return (f'near line {line}: `{snippet}` — Math.abs(Integer.MIN_VALUE) '
            f'is NEGATIVE, so this index eventually goes out of bounds and '
            f'crashes the harness itself. Use Math.floorMod(value, n) or a '
            f'bounded consume (e.g. data.consumeInt(0, n - 1)) instead')


# ---------------------------------------------------------------------------
# P2.3: constraint parity — no anonymous/local subclass of a library type.
#
# The harness rules forbid subclassing the patched class or its callees (a
# hand-built stand-in proves nothing about real usage). Screening compiles
# the check under NO such rule, so a relation that needs an anonymous
# subclass (Math-2's `new AbstractIntegerDistribution(min,hi){...}`) passes
# the screen and is then silently dropped when the harness can't implement
# it. This lint gives the screen the same constraint, so such a relation is
# caught early — and synthesis is told to use a real library subclass
# (UniformIntegerDistribution) that reaches the same code legally.

# Common JDK/functional-interface anonymous classes are legitimate and must
# NOT be flagged — only project/library CLASS types are the concern.
_JDK_ANON_OK = frozenset({
    'Runnable', 'Callable', 'Comparator', 'Comparable', 'Iterator',
    'Iterable', 'Thread', 'Object', 'Function', 'Supplier', 'Consumer',
    'BiFunction', 'Predicate', 'BiConsumer', 'Runnable', 'ActionListener',
    'Enumeration', 'ThreadLocal', 'TypeReference', 'InputStream',
    'OutputStream', 'Reader', 'Writer', 'TimerTask', 'AbstractList',
    'AbstractMap', 'AbstractSet', 'ArrayList', 'HashMap', 'HashSet',
})
_ANON_SUBCLASS_RE = re.compile(r'\bnew\s+([A-Z]\w*)\s*\([^;{}]*\)\s*\{')
_LOCAL_SUBCLASS_RE = re.compile(r'\bclass\s+\w+\s+extends\s+([A-Z]\w*)')


def library_subclass(check: str) -> Optional[str]:
    """Return the offending type name if `check` declares an anonymous or
    local subclass of a non-JDK (project/library) class; None otherwise.
    Comments/strings are stripped first so a type named in a message can't
    trip it."""
    src = strip_comments(check or '')
    src = re.sub(r'"(?:[^"\\]|\\.)*"', '""', src)
    for rx in (_ANON_SUBCLASS_RE, _LOCAL_SUBCLASS_RE):
        for m in rx.finditer(src):
            t = m.group(1)
            if t not in _JDK_ANON_OK:
                return t
    return None


# --------------------------------------------------------------------------
# H4/H5: mechanically-listed raw material for hidden-state and
# sibling-agreement checks — the two historically-winning invented shapes.
# H5: same-name overloads and shared-prefix factory families (Lang-41's
# getShortClassName(Class)/(String); Lang-27's createNumber/createFloat/
# createDouble family). H4: public no-argument readers ("capacity(),
# length(), size()" — the raw material of Lang-60's convicting check).

_PUB_METHOD_RE = re.compile(
    r'public\s+(?:static\s+|final\s+|synchronized\s+)*'
    r'([\w<>\[\],.\s]+?)\s+(\w+)\s*\(([^)]*)\)')

_STATE_PREFIX_BLOCKLIST = ('main',)


def sibling_and_state_hints(class_source: str, cap: int = 1400) -> str:
    """One rendered block: same-name overload groups, shared-prefix
    method families (3+ members), and the public no-arg readers of the
    class. Empty string when nothing qualifies."""
    sigs = []
    for m in _PUB_METHOD_RE.finditer(class_source or ''):
        ret, name, params = (m.group(1).strip(), m.group(2),
                             m.group(3).strip())
        if name and name[0].islower():
            sigs.append((name, params, ret))
    if not sigs:
        return ''
    parts = []
    # H5a: same-name overloads
    by_name: dict = {}
    for name, params, _ret in sigs:
        by_name.setdefault(name, []).append(params or '')
    overloads = [(n, ps) for n, ps in by_name.items() if len(set(ps)) > 1]
    if overloads:
        lines = [f"  {n}({') / ('.join(dict.fromkeys(ps))})"
                 for n, ps in sorted(overloads)[:8]]
        parts.append(
            "SAME-NAME OVERLOADS (documented to agree where their docs "
            "match — a sibling-agreement check compares them on "
            "equivalent inputs):\n" + "\n".join(lines))
    # H5b: shared-prefix families (createX/createY..., parseA/parseB...)
    fam: dict = {}
    for name in sorted(by_name):
        m = re.match(r'([a-z]+)(?=[A-Z])', name)
        if m:
            fam.setdefault(m.group(1), set()).add(name)
    fam_lines = [f"  {p}* family: " + ", ".join(sorted(ns))
                 for p, ns in sorted(fam.items())
                 if len(ns) >= 3 and p not in ('is', 'set')]
    if fam_lines:
        parts.append(
            "METHOD FAMILIES over the same input space (factory/parser "
            "siblings — where the docs state a selection or agreement "
            "rule between family members, equivalent inputs must "
            "respect it):\n"
            + "\n".join(fam_lines[:6]))
    # H4: readable state
    readers = sorted({n for n, params, ret in sigs
                      if not params and ret != 'void'
                      and n not in _STATE_PREFIX_BLOCKLIST})
    if readers:
        parts.append(
            "STATE YOU CAN READ (public no-argument readers — capture "
            "them BEFORE and AFTER a call documented as read-only or "
            "non-mutating; an unexplained change is a hidden-state "
            "violation): " + ", ".join(readers[:20]))
    out = "\n\n".join(parts)
    return out[:cap]


_NUM_LITERAL_RE = re.compile(
    r'(?<![\w.])'                       # not mid-identifier / mid-number
    r'-?\d[\d_]*'                       # integer part
    r'(?:\.\d[\d_]*)?'                  # optional fraction
    r'(?:[eE][+-]?\d+)?'                 # optional exponent
    r'[LlFfDd]?'                         # optional type suffix
    r'(?![\w.])')


def trigger_seed_literals(method_sources, cap=48):
    """BND-a: the failing test's own STRING and NUMERIC literals, for the
    screening direction-check and the patched-replay corpora. Numbers were
    previously dropped (only quoted strings were extracted), so a numeric
    bug like Math-2 (population 43,130,568) had its most important trigger
    inputs missing from the direction check. De-duplicated, strings first
    (they carry more meaning), capped. `method_sources` is an iterable of
    test-method source strings."""
    strings, nums = [], []
    for src in method_sources:
        if not src:
            continue
        strings += re.findall(r'"((?:[^"\\]|\\.){1,120})"', src)
        for m in _NUM_LITERAL_RE.findall(src):
            # skip bare 0/1 and the trivial loop indices — they are noise,
            # not bug-triggering magnitudes
            core = m.rstrip('LlFfDd')
            if core.lstrip('-') not in ('0', '1', '2'):
                nums.append(m)
    seen, out = set(), []
    for v in [x for x in strings if x.strip()] + nums:
        if v not in seen:
            seen.add(v)
            out.append(v)
    return out[:cap]


_SUFFIX_CHARS = 'LlFfDd'
_NUMLIKE_RE = re.compile(
    r'^[+-]?\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[LlFfDd]?$')


def literal_variations(literals, cap=48):
    """Mechanical variations of the failing test's own literals, for the
    fuzz seed corpora: type-suffix case swaps, suffix additions/removals
    (including on exponent forms — the cross products a random fuzz
    rarely reaches within budget), sign flips, and +/-1 neighbours of
    pure integers. General by construction: every output is a
    transformation of an input literal from THIS leg's failing test;
    nothing is injected from outside."""
    out, seen = [], set()

    def _add(s):
        if s and s not in seen and len(s) <= 128:
            seen.add(s)
            out.append(s)

    for lit in literals or []:
        _add(lit)
    for lit in list(literals or []):
        s = (lit or '').strip()
        if not s or not _NUMLIKE_RE.match(s):
            continue
        if s[-1] in _SUFFIX_CHARS:
            _add(s[:-1] + s[-1].swapcase())
            _add(s[:-1])
            base = s[:-1]
        else:
            base = s
        for c in _SUFFIX_CHARS:
            _add(base + c)
        _add(base[1:] if base.startswith('-') else '-' + base)
        if re.match(r'^[+-]?\d+$', base):
            try:
                v = int(base)
                _add(str(v + 1))
                _add(str(v - 1))
            except ValueError:
                pass
        if len(out) >= cap:
            break
    return out[:cap]
