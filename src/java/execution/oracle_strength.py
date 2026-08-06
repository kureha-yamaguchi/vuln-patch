"""Jazzer output parsing: the human-readable headline(s) of the throwable(s)
that fired.

Used to ATTRIBUTE an accepted trigger: a headline mentioning the harness's
own message tells us WHICH oracle fired on the buggy version, so runs can
report whether acceptance came via the reported symptom (which a band-aid
patch will silence) or via an independent consistency check (which survives
it).

(This module previously also held a static "weak oracle" gate — gate 2.5 —
that bounced harnesses which fetched a hinted summary value and then checked
it only for finiteness/null. It was removed: it was structurally gated on a
Math-shaped method-name whitelist and fired 0 times across the diagnostic
runs. The general replacement is a codebase-aware relation-synthesis stage
grounded by mined passing-test oracles, not a static name-matched gate.)
"""
import re
from typing import Optional

_HEADLINE_RES = [
    re.compile(r'==\s*Java Exception:\s*(.+)'),
    re.compile(r'\bUncaught exception:\s*(.+)'),
]


def exception_headline(output: str, max_len: int = 200) -> Optional[str]:
    """The human-readable first line of the throwable that fired, e.g.
    'java.lang.RuntimeException: consistency violation: ...'. Used to
    ATTRIBUTE an accepted trigger: a headline mentioning the harness's own
    message tells us WHICH oracle fired on the buggy version, so runs can
    report whether acceptance came via the reported symptom (which a
    band-aid patch will silence) or via an independent consistency check
    (which survives it). Returns None if no recognisable headline."""
    if not output:
        return None
    for pat in _HEADLINE_RES:
        m = pat.search(output)
        if m:
            line = m.group(1).strip()
            return line[:max_len] + ('…' if len(line) > max_len else '')
    return None


def crash_excerpt(output: str, context_lines: int = 20,
                  max_chars: int = 2000) -> str:
    """The crash block around the first reported throwable: the headline
    plus the following stack/detail lines. Handed to the relation verifier
    as CONCRETE EVIDENCE of what actually happened at the firing — the
    observed exception message and stack beat the critic's hypotheticals
    about what a correct implementation might do (a genuine detection was
    dropped because the critic reasoned abstractly about the relation while
    the message itself showed an object state no correct implementation
    could produce). Empty string when no crash marker is found."""
    if not output:
        return ''
    lines = output.splitlines()
    for i, line in enumerate(lines):
        if any(pat.search(line) for pat in _HEADLINE_RES):
            block = lines[i:i + 1 + context_lines]
            excerpt = '\n'.join(block)
            return excerpt[:max_chars]
    return ''


def exception_headline_pairs(output: str, max_len: int = 200) -> list:
    """`[(capped, full)]` for every distinct throwable headline.

    THE CONSUMERS ARE SPLIT, and this is why. `max_len` exists for the human
    and prompt-facing consumers: a headline is displayed, de-duplicated, and
    embedded in prompts, and an unbounded one hurts all three. None of those
    reasons apply to a MECHANICAL reader, and 8.4's raw-vs-pinned comparison is
    a mechanical reader whose input is a key/value block appended at the END of
    the message — so the cap deleted exactly what it needed.

    Measured in the batch smoke (`batch8_20260802_123712`): of 4 headlines
    reporting a normalized value, only 1 still carried `actualRaw=`, and that
    one was 198 characters — two under the cap.

    De-duplication keys on the CAPPED form, unchanged, so every existing
    consumer sees exactly the list it saw before. The full form rides alongside.
    """
    if not output:
        return []
    seen, out = set(), []
    for pat in _HEADLINE_RES:
        for m in pat.finditer(output):
            full = m.group(1).strip()
            capped = full[:max_len] + ('…' if len(full) > max_len else '')
            if capped not in seen:          # dedup on the capped form: unchanged
                seen.add(capped)
                out.append((capped, full))
    return out


def exception_headlines(output: str, max_len: int = 200) -> list:
    """EVERY distinct throwable headline in `output`, in order. A
    `--keep_going` Jazzer run reports several findings, each on its own
    `== Java Exception:` line; a multi-oracle harness thus surfaces one
    headline per oracle that fired on the patched code. Relation
    verification judges all of them and keeps the finding if ANY is sound
    or trusted — so a sound oracle is not hidden behind an unsound sibling
    that merely fired on some other input first. De-duplicated, order
    preserved. Empty list if none recognised."""
    return [capped for capped, _full
            in exception_headline_pairs(output, max_len)]


# --------------------------------------------------------------------------
# H3: does a test-copy ("lifted") check observe the SAME wrong value the
# real trigger test observes on the buggy build? If not, the harness has
# rebuilt the test's scenario wrong, and the DIFFERENCE — not the patch —
# is what its check measures. That divergence caused the Closure-62-c /
# Closure-73-c false alarms and was, until now, a judgment call made at
# station 7; here it becomes a station-5 string comparison.

# JUnit3 "expected:<X> but was:<Y>" and ComparisonFailure variants (whose
# X/Y may carry [] diff brackets around the differing region).
_EXPECTED_ACTUAL_RE = re.compile(
    r'expected:?\s*<(.*?)>\s*but was:?\s*<(.*?)>', re.S)


def real_wrong_values(failure_messages) -> list:
    """The ACTUAL (wrong) values the buggy build produced, parsed out of
    the trigger tests' real failure messages. Empty list when no message
    carries an expected/actual pair (crash-shaped failures)."""
    out = []
    for msg in failure_messages:
        for m in _EXPECTED_ACTUAL_RE.finditer(msg or ''):
            # ComparisonFailure marks the differing region with [] and
            # elides long common prefixes/suffixes with '...' — strip both
            # so the value compares against raw harness output.
            actual = m.group(2).replace('[', '').replace(']', '')
            actual = actual.strip().lstrip('.…').strip()
            if actual:
                out.append(actual)
    return list(dict.fromkeys(out))


def _ws_norm(s: str) -> str:
    return re.sub(r'\s+', ' ', s or '').strip()


def _decode_escapes(s: str) -> str:
    """Java escapes decoded, so an ESCAPED newline compares equal to a real
    one. Imported lazily to keep this module free of a relations import."""
    try:
        from java.relations.evidence_facts import _decode_java_literal
        return _decode_java_literal(s or '')
    except Exception:
        return s or ''


def _values_match(a: str, b: str) -> bool:
    """Whitespace-normalized equality / containment, with numeric-aware
    comparison so '4.0' matches '4' and NaN matches NaN.

    9.1b: BOTH SIDES ARE ESCAPE-DECODED FIRST. A harness message spells a
    newline as the two characters `\n`; a real JUnit failure message carries a
    literal one. Whitespace normalisation cannot bridge those, so H3 rejected a
    FAITHFUL harness on spelling alone (the one wrong rejection of five, read in
    9.1). 8.4 then made escaping MANDATORY for exactly the checks H3 polices --
    lifted, text-comparing checks on formatted output -- so the shape that
    caused one error in 120 legs became the required shape."""
    a, b = _decode_escapes(a), _decode_escapes(b)
    na, nb = _ws_norm(a), _ws_norm(b)
    if not na or not nb:
        return False
    if na == nb or na in nb or nb in na:
        return True
    # The harness prompt tells checks to normalize text by REMOVING all
    # whitespace (replaceAll("\\s+","")), so a lifted check's observed
    # value has no spaces while the real failure message keeps them —
    # collapse-to-one-space comparison then NEVER matches multi-word
    # values and H3 rejects faithful harnesses (batch4 Closure-62-c:
    # all 8 attempts). Compare with whitespace fully removed as well.
    ra, rb = re.sub(r'\s+', '', na), re.sub(r'\s+', '', nb)
    if ra and rb and (ra == rb or ra in rb or rb in ra):
        return True
    try:
        import math
        fa, fb = float(na), float(nb)
        if math.isnan(fa) and math.isnan(fb):
            return True
        return fa == fb or math.isclose(fa, fb, rel_tol=1e-9)
    except (ValueError, OverflowError):
        return False


_OBSERVED_RES = [
    # JUnit style: "... but was:<X>"
    re.compile(r'but was:?\s*<([^>\n]{1,400})>'),
    re.compile(r'but was:?\s*([^<>;\n]{1,400})'),
    # free-form harness style: "expected=X actual=Y" / "observed: Y"
    # (the Closure-62-c FP used exactly 'expected=... actual=...' and the
    # old patterns matched nothing). Multi-line values allowed: capture to
    # end-of-message, compare whitespace-normalized.
    re.compile(r'\bactual\s*[=:]\s*(.{1,600})', re.S),
    re.compile(r'\bobserved\s*[=:]\s*(.{1,600})', re.S),
    re.compile(r'\bwas:?\s*<([^>\n]{1,400})>'),
    re.compile(r'\bwas:?\s*([^<>;\n]{1,400})'),
]


def lifted_observed_mismatch(headline: Optional[str],
                             real_actuals: list) -> Optional[str]:
    """H3 gate decision for ONE accepted-trigger headline.

    Applies ONLY to lifted/test-copy checks (the headline carries the
    oracle id, and lifted checks are named so by prompt convention).
    Returns the observed value when it provably differs from EVERY real
    wrong value — the reject signal — and None to abstain (not a lifted
    check, no observed value extractable, no real value known, or the
    values agree). Conservative on purpose: an abstain keeps today's
    behaviour, a reject sends the harness to the repair loop."""
    if not headline or not real_actuals:
        return None
    if 'lift' not in headline.lower():
        return None
    observed = None
    for pat in _OBSERVED_RES:
        m = pat.search(headline)
        if m and _ws_norm(m.group(1)):
            observed = m.group(1).strip().rstrip('.').strip()
            break
    if not observed:
        return None
    # 9.1b: STOP AT THE `expected=` HALF. `actual=(.{1,600})` with DOTALL runs
    # to end-of-message, so on the `expected=X actual=Y ... expected=Z` shape it
    # swallows a following expected clause -- and the expected half naturally
    # contains the real wrong value as a substring (expected = actual + the
    # missing caret line). Escape-decoding then turns that over-capture into a
    # FALSE agreement, silently excusing the exact divergence this gate exists
    # to catch. This is the same hole the module already refuses to open via
    # headline-wide containment; over-capture is the back door to it.
    _cut = re.search(r'\bexpected\s*[=:]', observed)
    if _cut:
        observed = observed[:_cut.start()].strip().rstrip('.').strip()
    if not observed:
        return None
    if any(_values_match(observed, ra) for ra in real_actuals):
        return None
    # NOTE: no headline-wide containment fallback. The message's EXPECTED
    # half naturally contains the real actual as a prefix/substring
    # (expected = actual + the missing caret line), so headline
    # containment silently excused the exact divergence this gate exists
    # to catch (the Closure-62-c FP). Agreement must come from the
    # extracted OBSERVED value itself.
    return observed
