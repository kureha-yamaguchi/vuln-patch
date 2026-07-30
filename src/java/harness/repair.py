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
    _DYNAMIC_ORACLE_ID_RE, alarm_ids_missing, boolean_swallow, rethrow_without_cause, strip_comments,
    violation_swallowed)

#: The four detectors this module repairs against, in the order applied. Each is
#: (name, detector, repair). The detector is the SAME function the build gate
#: uses, so "repaired" cannot drift from "would now be accepted".
_ALARM_TYPE = 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'

#: Marker so a repaired region is recognisable and idempotent.
_MARK = '/*__vpRepair*/'
_MARK_CAUSE = '/*__vpCause*/'

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



def _names_an_oracle_id_variable(args, source):
    """True when an identifier in ``args`` is assigned a literal carrying an
    `[oracle:` tag anywhere in ``source`` — i.e. the alarm IS named, at runtime,
    through a variable. Pure."""
    for ident in set(re.findall(r'[A-Za-z_]\w*', args or '')):
        if re.search(r'\b' + re.escape(ident) + r'\s*=\s*"[^"]*\[oracle:',
                     source or ''):
            return True
    return False


def repair_missing_alarm_id(source, oracle_id=None):
    """Give every unnamed alarm an oracle ID taken from the harness's own
    `// relation: <name>` header, so the ID describes the check rather than
    inventing a claim about it.

    SPAN-BASED, deliberately. The first implementation used
    ``out.replace(literal, tagged, 1)``, which replaces the first occurrence in
    the WHOLE FILE — not necessarily the one in the alarm being repaired. On one
    archived harness that landed the tag after a closing quote and produced
    invalid Java. Detector-clearance could not see it; the compile comparison
    could. Each alarm is now edited inside its own parenthesised span.

    Alarms whose ID is built at runtime (`"[oracle:" + id + "]"`) are skipped
    using the SAME regex the detector uses to call them already-named, so the
    repair and the gate cannot disagree about what counts as named."""
    src = source or ''
    if oracle_id is None:
        m = re.search(r'//\s*relation:\s*([A-Za-z0-9_\-]+)', src)
        oracle_id = m.group(1) if m else 'unnamed-check'
    tag = '[oracle:%s] ' % oracle_id

    pieces, last = [], 0
    for cm in re.finditer(r'new\s+[\w.]*FuzzerSecurityIssue\w*\s*\(', src):
        call_open = cm.end() - 1
        call_end = _block_end_paren(src, call_open)
        if call_end < 0:
            continue
        args = src[call_open + 1:call_end]
        if not args.strip():
            continue
        if '[oracle:' in args or _DYNAMIC_ORACLE_ID_RE.search(args):
            continue                      # already named, or named at runtime
        # A THIRD already-named form, found live by the cycle-7 smoke: the tag
        # is held in a variable — `throw new FuzzerSecurityIssueLow(oracleId +
        # " semantic mismatch: " + what)` where oracleId = "[oracle:circle-err2-0]".
        # Neither guard above sees it, so the repair prepended a redundant
        # fallback and produced "[oracle:unnamed-check] [oracle:circle-err2-0]",
        # polluting the very IDs screening keys on. Resolve identifiers in the
        # argument against their assignments in the source.
        if _names_an_oracle_id_variable(args, src):
            continue
        # No keyword guard: constructing a FuzzerSecurityIssue* IS the alarm
        # signal. An earlier version tested the args for 'violation' and missed
        # `FuzzerSecurityIssueLow(monotonicityViolation)` because the match was
        # case-sensitive — 25 repairs lost to a capital V.
        first = args.lstrip()
        pad = len(args) - len(first)
        if first.startswith('"'):
            close = 1
            while close < len(first):
                if first[close] == '\\':
                    close += 2
                    continue
                if first[close] == '"':
                    break
                close += 1
            else:
                continue
            new_args = (args[:pad] + '"' + tag + first[1:close + 1]
                        + first[close + 1:])
        elif re.match(r'[A-Za-z_]\w*', first):
            # A non-literal message expression: `msgVar`, or
            # `oracleId + " ... "`. Prepending a literal is always valid Java
            # (string concatenation is defined for every type), so the only risk
            # is SEMANTIC: turning the (Throwable) cause constructor into the
            # (String) message one and dropping the cause. So skip when the sole
            # argument is a caught-exception variable.
            ident = re.match(r'[A-Za-z_]\w*', first).group(0)
            sole = first.strip() == ident
            if sole and re.search(r'catch\s*\([^)]*\b' + re.escape(ident)
                                  + r'\s*\)', src):
                continue
            new_args = (args[:pad] + '"' + tag + '" + ' + first)
        else:
            continue
        pieces.append(src[last:call_open + 1])
        pieces.append(new_args)
        last = call_end
    pieces.append(src[last:])
    return ''.join(pieces)


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
                continue                  # this catch's variable already passed
            # The alarm constructors are (), (String), (Throwable) and
            # (String, Throwable) — never more. A call that already has two
            # arguments is full, and appending a cause produces a signature that
            # does not exist. Found by the real-API compile check: one harness
            # already passed a DIFFERENT variable named `cause`, so the
            # variable-name guard above did not fire and the result was
            # FuzzerSecurityIssueLow(String, Throwable, InvocationTargetException).
            depth = 0
            top_level_commas = 0
            for ch in args:
                if ch in '([{':
                    depth += 1
                elif ch in ')]}':
                    depth -= 1
                elif ch == ',' and depth == 0:
                    top_level_commas += 1
            if top_level_commas >= 1:
                continue                  # already (String, Throwable)
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



def repair_boolean_swallow(source):
    """Preserve the caught exception's identity and attach it to the alarm.

    The largest rejection bucket (77 of 240 in the paired runs). The defect: a
    catch records only a bare literal flag, an alarm is later raised on that
    flag, and whatever actually crashed is flattened into `false` — so the
    attribution guards can no longer ask which exception fired or whether the
    unpatched build crashes the same way.

    Two coordinated edits:
      1. a CLASS-LEVEL holder, so it is in scope wherever the alarm lives;
      2. capture in the catch body, and attach as the alarm's cause.

    The holder is a static field rather than a local because the compile check
    showed the alarm is frequently in a DIFFERENT METHOD from the catch (one
    archived harness catches at line 38 and alarms at line 138), which no local
    declaration can bridge. Jazzer drives one input at a time, so a single
    static holder is sound here.

    HONESTY GUARD: capturing alone would clear the detector, because a
    non-literal assignment stops the catch matching the bare-flag shape. That
    would game the acceptance test while preserving nothing, so the repair is
    applied only when the cause actually REACHES an alarm.
    """
    src = source or ''
    if _MARK_CAUSE in src:
        return src                        # idempotent
    from java.parsing.java_source import boolean_swallow
    if not boolean_swallow(src):
        return src
    holder = '__vpCause'
    for cm in list(_CATCH_RE.finditer(src)):
        var = cm.group(2)
        body_open = cm.end() - 1
        end = _block_end(src, body_open)
        if end < 0:
            continue
        body = src[body_open + 1:end]
        if 'throw' in body or re.search(r'\breturn\b', body):
            continue                      # rethrows or skips: not the shape
        # find an alarm AFTER this catch with room for a cause
        alarm = None
        for am in re.finditer(r'new\s+[\w.]*FuzzerSecurityIssue\w*\s*\(', src):
            if am.start() < end:
                continue
            ao = am.end() - 1
            ae = _block_end_paren(src, ao)
            if ae < 0:
                continue
            args = src[ao + 1:ae]
            depth = commas = 0
            for ch in args:
                if ch in '([{':
                    depth += 1
                elif ch in ')]}':
                    depth -= 1
                elif ch == ',' and depth == 0:
                    commas += 1
            if commas >= 1 or not args.strip():
                continue                  # already has a cause, or no message
            alarm = (ao, ae, args)
            break
        if alarm is None:
            continue                      # cannot attach — leave untouched
        # class-level holder: insert after the harness class's opening brace
        cls = re.search(r'\bclass\s+\w+[^{]*\{', src)
        if not cls:
            continue
        ins = cls.end()
        decl = (f'\n    {_MARK_CAUSE} private static Throwable {holder} '
                f'= null;\n')
        ao, ae, args = alarm
        out = (src[:ins] + decl + src[ins:body_open + 1]
               + f' {holder} = {var};' + src[body_open + 1:ao + 1]
               + args + ', ' + holder + src[ae:])
        return out
    return src


#: (name, detector, repair) — applied in order, each gated on its own detector.
REPAIRS = (
    ('swallowed-alarm', violation_swallowed, repair_swallowed_alarm),
    ('missing-alarm-id', alarm_ids_missing, repair_missing_alarm_id),
    ('rethrow-without-cause', rethrow_without_cause,
     repair_rethrow_without_cause),
    ('boolean-swallow', boolean_swallow, repair_boolean_swallow),
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
