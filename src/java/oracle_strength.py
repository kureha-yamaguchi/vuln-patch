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


def exception_headlines(output: str, max_len: int = 200) -> list:
    """EVERY distinct throwable headline in `output`, in order. A
    `--keep_going` Jazzer run reports several findings, each on its own
    `== Java Exception:` line; a multi-oracle harness thus surfaces one
    headline per oracle that fired on the patched code. Relation
    verification judges all of them and keeps the finding if ANY is sound
    or trusted — so a sound oracle is not hidden behind an unsound sibling
    that merely fired on some other input first. De-duplicated, order
    preserved. Empty list if none recognised."""
    if not output:
        return []
    seen, out = set(), []
    for pat in _HEADLINE_RES:
        for m in pat.finditer(output):
            line = m.group(1).strip()
            line = line[:max_len] + ('…' if len(line) > max_len else '')
            if line not in seen:
                seen.add(line)
                out.append(line)
    return out
