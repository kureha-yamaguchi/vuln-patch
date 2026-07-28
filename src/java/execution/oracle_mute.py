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

`instrument_diversion` (cycle-6) is the second transform here. It answers a
different but equally load-bearing question: did the replayed input actually
REACH the check, or did the harness's own `catch (...) { return; }` swallow an
exception and return early? See its docstring.
"""
import re

# `[oracle:<id>]` must appear at the very start of the message literal.
_ORACLE_PREFIX_RE = re.compile(r'\[oracle:([-\w]+)\]')
# The relation-screen wrapper message shape (id = the relation name).
_RELATION_MSG_RE = re.compile(r'relation\s+([-\w]+)\s+violated')
# Jazzer entrypoint: the class body brace we count-instrument is the one
# enclosing this method, and its own body is where the check-increment goes.
_ENTRYPOINT_RE = re.compile(
    r'\bvoid\s+fuzzerTestOneInput\s*\(\s*'
    r'(?:com\.code_intelligence\.jazzer\.api\.)?FuzzedDataProvider\s+\w+\s*\)')
# A type/class/interface/enum declaration whose body brace may enclose the
# entrypoint. (`Foo.class`, `int.class` never match: no ident follows `class`.)
_CLASS_DECL_RE = re.compile(r'\b(?:class|interface|enum)\s+[A-Za-z_$][\w$]*')


def _is_ident_char(ch: str) -> bool:
    return ch.isalnum() or ch == '_' or ch == '$'


def _kw_at(src: str, i: int, kw: str) -> bool:
    """True when the keyword `kw` starts at `src[i]` on identifier
    boundaries (so `returnValue` never reads as `return`)."""
    if not src.startswith(kw, i):
        return False
    if i > 0 and _is_ident_char(src[i - 1]):
        return False
    j = i + len(kw)
    return j >= len(src) or not _is_ident_char(src[j])


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


# Counting scaffolding injected by `instrument_for_counting`. It mirrors
# relation_screen._screen_harness_source EXACTLY so relation_screen._STATS_RE
# parses the emitted line: the same `[relscreen] checked=.. violated=..` shape,
# the same shutdown-hook final-stats mechanism, printed to System.err. The two
# `long` fields are static so the muted/replaced throws — which may live in a
# helper method, not just fuzzerTestOneInput — can reach them.
_COUNT_FIELDS = (
    '\n    static long __vpChecked = 0, __vpViolated = 0;'
    '\n    static {'
    '\n        Runtime.getRuntime().addShutdownHook(new Thread(() ->'
    '\n            System.err.println("[relscreen] checked=" + __vpChecked'
    '\n                + " violated=" + __vpViolated)));'
    '\n    }')
# Injected at the START of fuzzerTestOneInput's body: count every input and
# print the running stats every 1000 checks (the shutdown hook guarantees a
# final line for the trailing partial batch, exactly as relation_screen does).
_COUNT_INCR = (
    '\n        __vpChecked++;'
    '\n        if (__vpChecked % 1000 == 0) {'
    '\n            System.err.println("[relscreen] checked=" + __vpChecked'
    '\n                + " violated=" + __vpViolated);'
    '\n        }')


def _match_brace(src: str, open_idx: int) -> int:
    """Index of the `}` closing `src[open_idx]` ('{'), or -1. Comment- and
    literal-aware (unlike java_source.match_brace), reusing this module's own
    scanners so a `{`/`}` — or a lone apostrophe like `library's` — inside a
    comment or string never skews the depth."""
    n = len(src)
    depth = 0
    i = open_idx
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
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def _class_open_brace_enclosing(src: str, target_idx: int) -> int:
    """Index of the opening `{` of the INNERMOST class/interface/enum body
    that encloses `target_idx`, or -1. Used to place the counting fields on
    the class that declares fuzzerTestOneInput."""
    best = -1
    for m in _CLASS_DECL_RE.finditer(src):
        brace = src.find('{', m.end())
        if brace < 0:
            continue
        close = _match_brace(src, brace)
        if close < 0:
            continue
        if brace < target_idx < close and brace > best:
            best = brace
    return best


def instrument_for_counting(java_source: str, target_id: str):
    """Return `java_source` rewritten into a COUNTING harness that measures how
    often the `target_id` oracle's claim is violated on a build, or None when
    the harness can't be instrumented (fail-open at the call site).

    The transform, all mechanical and literal-aware (reusing `mute_oracles`'
    throw scanner), is:

      * every alarm throw EXCEPT the target is muted to `;` (siblings would
        end the run first and hide the target); untagged `FuzzerSecurityIssue`
        throws are muted too, since they'd surface and pollute the count;
      * every throw carrying the target id is replaced by `{ __vpViolated++; }`
        — the alarm becomes a tally instead of a fatal throw;
      * a `static long __vpChecked, __vpViolated` pair plus a shutdown hook
        that prints `[relscreen] checked=N violated=M` are injected on the
        class declaring fuzzerTestOneInput, and a per-input increment + a
        periodic print of the same line are injected at the top of
        fuzzerTestOneInput — copied from relation_screen's wrapper so its
        `_STATS_RE` parses the output unchanged.

    Returns None if fuzzerTestOneInput, its enclosing class brace, or a throw
    carrying `target_id` cannot be located. The class name is kept (the variant
    compiles in its own output dir, like the muted-replay variants), so the
    result is drop-in for HarnessBuilder.build. Pure: no I/O."""
    if not java_source or not target_id:
        return None

    # Classify every throw once; collect the edit spans.
    alarms = []            # (start, semi, oracle_id) for alarm throws
    target_found = False
    for start, semi, type_region, first_literal in _find_throw_statements(
            java_source):
        is_alarm, oracle_id = _classify(type_region, first_literal)
        if not is_alarm:
            continue
        alarms.append((start, semi, oracle_id))
        if oracle_id == target_id:
            target_found = True
    if not target_found:
        return None

    entry = _ENTRYPOINT_RE.search(java_source)
    if not entry:
        return None
    fuzz_body_open = java_source.find('{', entry.end())
    if fuzz_body_open < 0:
        return None
    class_open = _class_open_brace_enclosing(java_source, entry.start())
    if class_open < 0:
        return None

    # Build all edits as (start, end, replacement), then splice from the last
    # position backwards so earlier indices stay valid (same discipline as
    # mute_oracles). Insertions use start == end.
    edits = []
    for start, semi, oracle_id in alarms:
        if oracle_id == target_id:
            edits.append((start, semi + 1, '{ __vpViolated++; }'))
        else:
            cid = oracle_id if oracle_id is not None else 'all'
            edits.append((start, semi + 1, '; /* muted:%s */' % cid))
    edits.append((class_open + 1, class_open + 1, _COUNT_FIELDS))
    edits.append((fuzz_body_open + 1, fuzz_body_open + 1, _COUNT_INCR))

    result = java_source
    for start, end, replacement in sorted(edits, key=lambda e: e[0],
                                          reverse=True):
        result = result[:start] + replacement + result[end:]
    return result


# ---------------------------------------------------------------------------
# Diversion instrumentation (cycle-6): make a SWALLOWED exception observable.
#
# A harness that wraps its call in `try { ... } catch (Exception e) { return; }`
# ends the input's run early when the library throws — the checks BELOW that
# catch are never evaluated. A replay of such an input reports "no oracle fired
# / ran clean", which the note builders used to word as "the buggy build runs
# this exact input WITHOUT firing this check — the patch INTRODUCED the
# violation". That is a FALSE fact whenever the run was diverted: the check was
# never reached, so the replay says nothing either way. It convicted the
# night20b Chart-26 correct patch.
#
# The fix is to make the diversion OBSERVABLE: a static `__vpSkipped` counter,
# incremented (and printed) at the top of every swallow-return catch body, plus
# a shutdown hook that prints the final count — so an uneventful run reports
# `skipped=0` and the "clean" reading is earned rather than assumed.
# ---------------------------------------------------------------------------

# Same `[relscreen]` stats-line protocol as the counting wrapper above, so the
# parsing lives in one place. The key is `skipped=` and the line never carries
# `checked=`, so relation_screen._STATS_RE cannot match it (no cross-talk).
_SKIP_FIELDS = (
    '\n    static long __vpSkipped = 0;'
    '\n    static {'
    '\n        Runtime.getRuntime().addShutdownHook(new Thread(() ->'
    '\n            System.err.println("[relscreen] skipped=" + __vpSkipped)));'
    '\n    }')
# Injected immediately after a swallow-return catch's `{`. It prints as well as
# counts so the fact survives a run that never reaches the shutdown hook.
_SKIP_INCR = (' __vpSkipped++;'
              ' System.err.println("[relscreen] skipped=" + __vpSkipped);')
_SKIPPED_RE = re.compile(r'\[relscreen\]\s+skipped=(\d+)')


def parse_skipped(output):
    """The LAST `[relscreen] skipped=N` count in `output`, or None when the
    instrumented line never appeared (run never started, JVM halted before the
    hook, or the variant was not instrumented). None means UNKNOWN and callers
    must degrade to "diversion unavailable" — never to "ran clean"."""
    if not output:
        return None
    last = None
    for m in _SKIPPED_RE.finditer(output):
        last = m.group(1)
    if last is None:
        return None
    try:
        return int(last)
    except ValueError:
        return None


def _match_paren(src: str, open_idx: int) -> int:
    """Index of the `)` closing `src[open_idx]` ('('), or -1. Comment- and
    literal-aware, like `_match_brace`."""
    n = len(src)
    depth = 0
    i = open_idx
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
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def _skip_ws_and_comments(src: str, i: int) -> int:
    """First index at or after `i` that is neither whitespace nor comment."""
    n = len(src)
    while i < n:
        c = src[i]
        if c.isspace():
            i += 1
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        break
    return i


def find_catch_blocks(src: str):
    """Every `catch (...) { ... }` in `src`, as `(kw_start, body_open,
    body_close)` index triples. The walk is string/char/comment aware (the same
    scanners `mute_oracles` uses), so a `catch` inside a literal or a comment is
    never picked up and a brace inside a string never skews the match."""
    out = []
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
        if c == 'c' and _kw_at(src, i, 'catch'):
            j = _skip_ws_and_comments(src, i + len('catch'))
            if j < n and src[j] == '(':
                close_p = _match_paren(src, j)
                if close_p > 0:
                    k = _skip_ws_and_comments(src, close_p + 1)
                    if k < n and src[k] == '{':
                        close_b = _match_brace(src, k)
                        if close_b > 0:
                            out.append((i, k, close_b))
                            # Nested catches inside this body are found by
                            # continuing the walk from just past the `{`.
                            i = k + 1
                            continue
            i += len('catch')
            continue
        i += 1
    return out


def _has_bare_return(body: str) -> bool:
    """True when `body` contains a `return;` (no value) statement, scanning
    outside strings, chars and comments."""
    n = len(body)
    i = 0
    while i < n:
        c = body[i]
        if c == '/' and i + 1 < n and body[i + 1] == '/':
            j = body.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and i + 1 < n and body[i + 1] == '*':
            j = body.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if c == '"':
            i = _string_end(body, i)
            continue
        if c == "'":
            i = _char_end(body, i)
            continue
        if c == 'r' and _kw_at(body, i, 'return'):
            j = _skip_ws_and_comments(body, i + len('return'))
            if j < n and body[j] == ';':
                return True
            i += len('return')
            continue
        i += 1
    return False


def is_swallow_catch(body: str) -> bool:
    """True when a catch body SWALLOWS: it returns early and never throws.

    Deliberately conservative — a body containing ANY `throw` (a library
    rethrow `throw e;`, a wrapped rethrow, or the harness's own
    `[oracle:...]` / `FuzzerSecurityIssue` alarm) is NEVER treated as a
    swallow, because such a catch does not silently divert the run: it ends it
    visibly, and the replay already sees that."""
    if _find_throw_statements(body):
        return False
    return _has_bare_return(body)


def instrument_diversion(java_source: str):
    """Return `java_source` rewritten so a SWALLOWED exception is observable,
    or None when the harness cannot be instrumented (caller must then report
    diversion as UNKNOWN — never as "ran clean").

    The transform, all mechanical and literal-aware:

      * every `catch (...) { ... }` whose body contains a bare `return;` and NO
        `throw` at all gets `__vpSkipped++;` plus an immediate
        `[relscreen] skipped=N` print injected right after its `{`;
      * catches that RETHROW (`throw e;`) and the harness's own alarm catches
        (`throw new FuzzerSecurityIssue...`) are left untouched — they are not
        silent diversions;
      * a `static long __vpSkipped` field and a shutdown hook printing the same
        `[relscreen] skipped=N` line are injected on the class declaring
        fuzzerTestOneInput, so a run with NO diversion still reports
        `skipped=0` (the difference between "did not divert" and "we could not
        tell", which is the whole point of the fix).

    Returns None if fuzzerTestOneInput or its enclosing class brace cannot be
    located. The class name is kept, so the result is drop-in for
    HarnessBuilder.build. Pure: no I/O, no mutation of inputs."""
    if not java_source:
        return None

    entry = _ENTRYPOINT_RE.search(java_source)
    if not entry:
        return None
    class_open = _class_open_brace_enclosing(java_source, entry.start())
    if class_open < 0:
        return None

    edits = [(class_open + 1, class_open + 1, _SKIP_FIELDS)]
    for _kw, body_open, body_close in find_catch_blocks(java_source):
        if is_swallow_catch(java_source[body_open + 1:body_close]):
            edits.append((body_open + 1, body_open + 1, _SKIP_INCR))

    result = java_source
    for start, end, replacement in sorted(edits, key=lambda e: e[0],
                                          reverse=True):
        result = result[:start] + replacement + result[end:]
    return result
