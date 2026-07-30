"""Cycle-7: repair mechanically-diagnosed harness defects in place, instead of
discarding the attempt.

WHY
===

Measured over the two paired runs: 750 harness build attempts, 285 accepted
(38%), **240 rejected (32%)**. And 170 of those 240 rejections (71%) are a small
family of purely structural mistakes:

    65  the alarm is thrown inside a try whose broad catch swallows it
    65  an exception is caught into a bare boolean flag, alarm raised on the flag
    34  an alarm carries no oracle ID
     6  a caught exception is re-thrown without preserving the original

Every one of these is already *detected* — that is why the attempt was rejected —
and the rejection message already names the line and states the fix. The attempt
is then thrown away.

That is where Chart-19's rules died. Its winning family was proposed in both rolls
(12 `categoryplot-*` rules in one, 14 in the other) and not one reached the
reviewer; they were rejected at construction, not starved by the rule budget. So
the nominal budget is not the binding constraint — the waste inside it is.

THE RULE, stated generally so it names no bug: *when a generated artefact is
rejected for a mechanically-identified structural defect that the rejection
message can already locate, repair it in place rather than discarding the
attempt.*

DISCIPLINE
==========

* **The project's own detectors are the acceptance test.** A repair is kept only
  if the detector that rejected the harness now passes AND no other detector
  starts failing. No second opinion about what the defect was.
* **Never weaken a check.** Every repair preserves the harness's evident intent:
  a swallowing catch keeps swallowing everything except our own alarm; a missing
  ID is added, never invented from behaviour; a cause is attached, never dropped.
* **Back out on doubt.** If a repair does not clear its detector, or trips a new
  one, the original source is returned unchanged. A failed repair must leave the
  pipeline exactly where it was.
* **Idempotent.** Repairing an already-repaired source is a no-op.

LIMIT OF THE OFFLINE VALIDATION (stated because it is easy to overclaim): the
archived corpus can prove a repair clears the detector that rejected it and trips
no other. It cannot prove the result *compiles* — that needs javac and the
project classpath. Compilation is confirmed by the live smoke, not here.
"""
import re

from java.parsing.java_source import (
    alarm_ids_missing, boolean_swallow, rethrow_without_cause, strip_comments,
    violation_swallowed)

#: The four detectors this module repairs against, in the order applied. Each is
#: (name, detector, repair). The detector is the SAME function the build gate
#: uses, so "repaired" cannot drift from "would now be accepted".
_ALARM_TYPE = 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'

#: Marker so a repaired region is recognisable and idempotent.
_MARK = '/*__vpRepair*/'

_CATCH_RE = re.compile(
    r'catch\s*\(\s*(?:final\s+)?([\w.]+(?:\s*\|\s*[\w.]+)*)\s+(\w+)\s*\)\s*\{')

_BROAD = ('Throwable', 'Exception', 'RuntimeException')


def _is_broad(types):
    return any(re.search(r'\b' + t + r'\b', types) for t in _BROAD)


def _block_end(src, open_idx):
    """Index of the brace matching the one at ``open_idx``, or -1."""
    depth = 0
    for i in range(open_idx, len(src)):
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return i
    return -1


def repair_swallowed_alarm(source):
    """Let our own alarm escape a broad catch that would otherwise absorb it.

    Inserts, at the top of every broad catch body, a guard that rethrows the
    caught value when it IS our alarm. Everything else the harness was catching
    is still caught — the check's intent is preserved exactly, which is why this
    is safe to apply without understanding what the harness does."""
    src = source or ''
    out, shift = src, 0
    for m in _CATCH_RE.finditer(src):
        types, var = m.group(1), m.group(2)
        if not _is_broad(types):
            continue
        body_open = m.end() - 1
        end = _block_end(src, body_open)
        if end < 0:
            continue
        body = src[body_open + 1:end]
        if _MARK in body:
            continue                      # already repaired — idempotent
        guard = (f' {_MARK} if ({var} instanceof {_ALARM_TYPE}) '
                 f'throw ({_ALARM_TYPE}) {var};')
        at = body_open + 1 + shift
        out = out[:at] + guard + out[at:]
        shift += len(guard)
    return out


def repair_missing_alarm_id(source, oracle_id=None):
    """Give every unnamed alarm an oracle ID taken from the harness's own
    `// relation: <name>` header, so the ID describes the check rather than
    inventing a claim about it. Falls back to a stable generic id."""
    src = source or ''
    if oracle_id is None:
        m = re.search(r'//\s*relation:\s*([A-Za-z0-9_\-]+)', src)
        oracle_id = m.group(1) if m else 'unnamed-check'
    tag = f'[oracle:{oracle_id}] '
    stripped = strip_comments(src)

    def _needs_id(stmt):
        if not any(k in stmt for k in
                   ('violated', 'violation', 'semantic mismatch',
                    'FuzzerSecurityIssue')):
            return False
        return '[oracle:' not in stmt

    out = src
    for m in re.finditer(r'\bthrow\b[^;]{0,400}?"', stripped):
        end = stripped.find(';', m.end())
        stmt = stripped[m.start():end if end > 0 else m.end()]
        if not _needs_id(stmt):
            continue
        # Locate the first string literal of this throw in the ORIGINAL source
        # and prefix it. Matching on the literal keeps comment offsets irrelevant.
        lit = re.search(r'"([^"\\]*(?:\\.[^"\\]*)*)"', stmt)
        if not lit:
            continue
        original = '"' + lit.group(1) + '"'
        if original not in out:
            continue
        if lit.group(1).startswith('[oracle:'):
            continue
        out = out.replace(original, '"' + tag + lit.group(1) + '"', 1)

    # Alarms whose message is a VARIABLE rather than a literal —
    # `throw new FuzzerSecurityIssueLow(violation);` — which is 25 of the 39
    # unnamed alarms in the archived corpus. Prefix at the construction site so
    # the variable's own content is preserved verbatim after the tag.
    def _tag_variable_message(text):
        res = text
        for cm in re.finditer(
                r'new\s+[\w.]*FuzzerSecurityIssue\w*\s*\(', text):
            call_open = cm.end() - 1
            call_end = _block_end_paren(text, call_open)
            if call_end < 0:
                continue
            args = text[call_open + 1:call_end]
            first = args.split(',')[0].strip()
            if not re.fullmatch(r'[A-Za-z_]\w*', first):
                continue                  # literal or expression: handled above
            if tag in args:
                continue                  # idempotent
            new_args = args.replace(first, f'"{tag}" + {first}', 1)
            whole = text[cm.start():call_end + 1]
            fixed = (text[cm.start():call_open + 1] + new_args
                     + text[call_end:call_end + 1])
            res = res.replace(whole, fixed, 1)
        return res

    return _tag_variable_message(out)


def repair_rethrow_without_cause(source):
    """Attach the caught exception as the cause of the alarm raised inside its
    own catch block, so the original crash identity survives."""
    src = source or ''
    out = src
    for m in _CATCH_RE.finditer(src):
        var = m.group(2)
        body_open = m.end() - 1
        end = _block_end(src, body_open)
        if end < 0:
            continue
        body = src[body_open + 1:end]
        if var in re.sub(r'"[^"]*"', '', body).replace(var + ' ', '', 1):
            pass
        for tm in re.finditer(r'new\s+[\w.]*FuzzerSecurityIssue\w*\s*\(', body):
            call_open = tm.end() - 1
            call_end = _block_end_paren(body, call_open)
            if call_end < 0:
                continue
            args = body[call_open + 1:call_end]
            if var in re.findall(r'\b\w+\b', args):
                continue                  # cause already passed
            new_args = args.rstrip() + ', ' + var
            new_body = body[:call_open + 1] + new_args + body[call_end:]
            out = out.replace(body, new_body, 1)
            body = new_body
    return out


def _block_end_paren(src, open_idx):
    depth = 0
    for i in range(open_idx, len(src)):
        if src[i] == '(':
            depth += 1
        elif src[i] == ')':
            depth -= 1
            if depth == 0:
                return i
    return -1


#: (name, detector, repair) — applied in order, each gated on its own detector.
REPAIRS = (
    ('swallowed-alarm', violation_swallowed, repair_swallowed_alarm),
    ('missing-alarm-id', alarm_ids_missing, repair_missing_alarm_id),
    ('rethrow-without-cause', rethrow_without_cause,
     repair_rethrow_without_cause),
)

#: Detectors consulted to prove a repair introduced nothing new.
_ALL_DETECTORS = (
    ('swallowed-alarm', violation_swallowed),
    ('boolean-swallow', boolean_swallow),
    ('missing-alarm-id', alarm_ids_missing),
    ('rethrow-without-cause', rethrow_without_cause),
)


def _failing(source):
    return {name for name, det in _ALL_DETECTORS if det(source)}


def repair_harness(source):
    """Repair every mechanically-diagnosed defect this module handles.

    Returns ``(repaired_source, applied, remaining)``:
      * ``applied``   — names of repairs that cleared their detector.
      * ``remaining`` — detectors still failing afterwards (e.g. boolean-swallow,
        which this module does not repair).

    A repair that fails to clear its own detector, or that trips a detector which
    was previously passing, is BACKED OUT. So the returned source never fails
    anything the original passed."""
    src = source or ''
    before = _failing(src)
    applied = []
    for name, detector, repair in REPAIRS:
        if not detector(src):
            continue
        try:
            candidate = repair(src)
        except Exception:                 # pragma: no cover - defensive
            continue
        if candidate == src:
            continue
        if detector(candidate):
            continue                      # did not clear — back out
        after = _failing(candidate)
        if after - before - {name}:
            continue                      # introduced something new — back out
        src, applied = candidate, applied + [name]
    return src, applied, sorted(_failing(src))
