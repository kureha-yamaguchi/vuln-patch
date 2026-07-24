"""Silence a harness's own alarm throws so a shadowed input can be
re-replayed and the missing per-input fact computed.

When a firing's buggy-side replay is *shadowed* — a different check throws
first, or the harness's own oracle throws before an escaped-crash site — the
question "does THIS check fire / does THIS crash occur on the buggy build?"
is uncomputable from the raw replay: the earlier throw ends the run. Oracle
throws are syntactically identifiable, so mechanically replacing the
shadowing throws with a bare `;` and re-replaying computes the fact without
any prompt or codegen change. This module is the pure source transform;
`FuzzRunner.replay_input_muted` compiles and runs the variant.

`mute_oracles` is a pure function (no I/O). It is statement-aware and
string-literal-aware: it scans each `throw` statement from the keyword to
its terminating `;` at paren/brace depth 0, staying outside string and char
literals and comments, so a multi-line `+`-concatenated alarm message is one
statement and a `;` inside a string is never mistaken for the terminator.

An ALARM throw — the only kind ever touched — is one where any of:
  * the FIRST string literal in the thrown constructor call starts with
    `[oracle:<id>]` (the mandated harness-alarm prefix; `<id>` extracted),
  * the thrown TYPE contains `FuzzerSecurityIssue` (Jazzer's own alarm
    family, used by lifted/relation harnesses), or
  * the first string literal matches `relation <name> violated` (the
    relation-screen wrapper shape; `<name>` extracted as the id).

Library rethrows (`throw ex;`) and input-rejection throws
(`throw new IllegalArgumentException("bad input")`) carry no such marker and
are NEVER modified.
"""
import re

# `[oracle:<id>]` must appear at the very start of the message literal.
_ORACLE_PREFIX_RE = re.compile(r'\[oracle:([-\w]+)\]')
# The relation-screen wrapper message shape (id = the relation name).
_RELATION_MSG_RE = re.compile(r'relation\s+([-\w]+)\s+violated')


def _is_ident_char(ch: str) -> bool:
    return ch.isalnum() or ch == '_' or ch == '$'


def _string_end(src: str, i: int) -> int:
    """Index just past the closing quote of the string literal that opens
    at `src[i] == '"'`, honouring backslash escapes. Unterminated → len."""
    n = len(src)
    i += 1
    while i < n:
        c = src[i]
        if c == '\\':
            i += 2
            continue
        if c == '"':
            return i + 1
        i += 1
    return n


def _char_end(src: str, i: int) -> int:
    """Index just past the closing quote of the char literal at
    `src[i] == "'"`, honouring escapes. Unterminated → len."""
    n = len(src)
    i += 1
    while i < n:
        c = src[i]
        if c == '\\':
            i += 2
            continue
        if c == "'":
            return i + 1
        i += 1
    return n


def _scan_one_throw(src: str, start: int):
    """Scan the throw statement whose `throw` keyword begins at `start`.

    Returns `(start, semi_index, type_region, first_literal, next_index)`
    where `semi_index` is the index of the terminating `;` at paren/brace
    depth 0, `type_region` is the code between `throw` and the constructor's
    opening `(` (used to spot a `FuzzerSecurityIssue` type), `first_literal`
    is the raw content of the first string literal in the statement (or
    None), and `next_index = semi_index + 1`. Returns None if the statement
    has no depth-0 terminator (malformed source)."""
    n = len(src)
    i = start + len('throw')
    depth = 0
    first_literal = None
    type_end = None  # index of the constructor's opening '(' at depth 0
    while i < n:
        c = src[i]
        # comments
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        # string / char literals
        if c == '"':
            end = _string_end(src, i)
            if first_literal is None:
                first_literal = src[i + 1:end - 1]
            i = end
            continue
        if c == "'":
            i = _char_end(src, i)
            continue
        # depth tracking over (), {}, []
        if c == '(':
            if depth == 0 and type_end is None:
                type_end = i
            depth += 1
        elif c in '{[':
            depth += 1
        elif c in ')}]':
            depth -= 1
        elif c == ';' and depth == 0:
            type_region = src[start + len('throw'):type_end if type_end is not None else i]
            return (start, i, type_region, first_literal, i + 1)
        i += 1
    return None


def _find_throw_statements(src: str):
    """Every top-level `throw` statement in `src`, as tuples from
    `_scan_one_throw`. The outer walk is itself string/char/comment aware so
    a `throw` inside a literal or comment is never picked up; a real scanner
    (not a bare regex) supplies each statement's span."""
    results = []
    n = len(src)
    i = 0
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if c == '"':
            i = _string_end(src, i)
            continue
        if c == "'":
            i = _char_end(src, i)
            continue
        if (c == 't' and src.startswith('throw', i)
                and (i == 0 or not _is_ident_char(src[i - 1]))
                and (i + 5 >= n or not _is_ident_char(src[i + 5]))):
            span = _scan_one_throw(src, i)
            if span:
                results.append(span[:4])
                i = span[4]
                continue
        i += 1
    return results


def _classify(type_region: str, first_literal):
    """Classify one throw. Returns `(is_alarm, oracle_id)`; `oracle_id` may
    be None for a `FuzzerSecurityIssue` throw with no `[oracle:]`/relation
    marker in its message."""
    is_alarm = False
    oracle_id = None
    lit = (first_literal or '').lstrip()
    m = _ORACLE_PREFIX_RE.match(lit)
    if lit.startswith('[oracle:') and m:
        is_alarm = True
        oracle_id = m.group(1)
    if oracle_id is None:
        rel = _RELATION_MSG_RE.search(first_literal or '')
        if rel:
            is_alarm = True
            oracle_id = rel.group(1)
    if 'FuzzerSecurityIssue' in (type_region or ''):
        is_alarm = True
    return is_alarm, oracle_id


def mute_oracles(java_source: str,
                 mute_ids=None,
                 mute_all: bool = False) -> str:
    """Return `java_source` with alarm throws silenced.

    Silences every ALARM throw (see module docstring) when `mute_all`, or —
    when `mute_all` is False — those whose extracted id is in `mute_ids`
    (relation-name ids match the `relation <name> violated` shape). Each
    silenced statement, however many lines it spans, is replaced by a single
    `;` plus a trailing `/* muted:<id-or-all> */` comment on the same line;
    the leading indentation before `throw` is preserved. Non-alarm throws
    (library rethrows, input-rejection throws) are never touched. Pure: no
    I/O, no mutation of inputs."""
    if not java_source:
        return java_source
    mute_ids = set(mute_ids) if mute_ids else set()

    result = java_source
    # Rewrite from the last statement backwards so earlier indices stay
    # valid as we splice replacements in.
    for start, semi, type_region, first_literal in sorted(
            _find_throw_statements(java_source),
            key=lambda s: s[0], reverse=True):
        is_alarm, oracle_id = _classify(type_region, first_literal)
        if not is_alarm:
            continue
        if mute_all:
            comment_id = oracle_id if oracle_id is not None else 'all'
        elif oracle_id is not None and oracle_id in mute_ids:
            comment_id = oracle_id
        else:
            continue
        result = (result[:start]
                  + '; /* muted:%s */' % comment_id
                  + result[semi + 1:])
    return result
