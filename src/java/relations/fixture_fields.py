"""Fail-loud access to recorded-case fields, and one unmisreadable vocabulary
for the gold verdict.

WHY THIS EXISTS
===============

Two defects, both of which cost real work in cycle 7, and both of which this
module makes structurally impossible.

**1. Reading a field that is not there.** ``case.get('trusted_values')`` returned
``None`` for all 228 rows because that field does not exist on these records. The
``None``s were read as "the value is empty", a plausible-looking 89% statistic was
computed from nothing, and it was minutes from being published. The same shape had
already invalidated an earlier replay measurement, where every row's failing-test
block turned out to be empty.

``.get()`` on a typo or a renamed field is silent, and silence here produces a
*number*, not an error — which is the worst possible failure mode, because a
number gets believed. ``field()`` raises instead.

**2. A value whose name means the opposite of how it reads.** The gold verdict was
recorded as ``SOUND``/``UNSOUND``. ``SOUND`` naturally parses as "the patch is
sound", but it means "the *check* is sound, so KEEP the finding" — and since a
sound check on a fake patch is exactly a legitimate catch, ``gold=SOUND``
correlates with *fake* patches, not correct ones.

That misreading happened. It was written into a report ("all 10 are on correct
patches, so fixing this can only help precision"), it licensed a fix, the fix was
built, and only a per-item measurement caught that it was net-harmful. Verifying
the population showed all 10 rows were one *fake* patch whose findings should be
kept — the exact opposite of the claim.

So the vocabulary itself is the problem, not the discipline of the reader. The
canonical values are now self-describing:

    keep-finding      the check is legitimate; the finding must NOT be dropped
                      (was: SOUND). Dropping one of these is an over-kill.
    dismiss-finding   the check is a false alarm; the finding SHOULD be dropped
                      (was: UNSOUND). Keeping one of these is a leak.
    unresolved        the correct verdict could not be reconstructed; not scored.

Legacy ``SOUND``/``UNSOUND``/``UNRESOLVED`` are still *accepted on read* so old
recorded artefacts stay readable, but ``gold_verdict()`` always returns a
canonical value, and anything unrecognised raises rather than being guessed at.
"""

#: Canonical, self-describing gold verdicts.
KEEP = 'keep-finding'
DISMISS = 'dismiss-finding'
UNRESOLVED = 'unresolved'

GOLD_VERDICTS = (KEEP, DISMISS, UNRESOLVED)

#: Legacy spellings, accepted on read only. The mapping is the whole point of
#: this module, so it is stated once, here, and nowhere else.
_LEGACY_GOLD = {
    'SOUND': KEEP,
    'UNSOUND': DISMISS,
    'UNRESOLVED': UNRESOLVED,
}


class MissingField(KeyError):
    """A recorded case was asked for a field it does not carry.

    Deliberately loud. The alternative — returning None — produces a plausible
    statistic from absent data, which is how two separate wrong measurements got
    as far as a draft in a single day.
    """


def field(case, name, *, default=_LEGACY_GOLD):
    """``case[name]``, raising :class:`MissingField` when it is absent.

    Pass ``default=`` explicitly to opt into a fallback for a genuinely optional
    field. The sentinel is a private object so that ``default=None`` is a real,
    deliberate choice rather than an accident.
    """
    try:
        return case[name]
    except (KeyError, TypeError):
        pass
    if default is not _LEGACY_GOLD:
        return default
    keys = ', '.join(sorted(map(str, case))) if hasattr(case, 'keys') else '?'
    raise MissingField(
        f"recorded case has no field {name!r}. Present fields: {keys}. "
        f"If this field is genuinely optional, pass default= explicitly — "
        f"do NOT switch to .get(), which turns a typo into a silent None and "
        f"a silent None into a believable number."
    )


def gold_verdict(case):
    """The case's gold verdict, always as one of :data:`GOLD_VERDICTS`.

    Accepts the legacy SOUND/UNSOUND spellings. Raises on a missing field and on
    any value that is neither canonical nor legacy — an unrecognised verdict is
    never silently treated as one of the real ones.
    """
    raw = field(case, 'gold')
    if raw in GOLD_VERDICTS:
        return raw
    if raw in _LEGACY_GOLD:
        return _LEGACY_GOLD[raw]
    raise ValueError(
        f"unrecognised gold verdict {raw!r}; expected one of "
        f"{GOLD_VERDICTS} (or legacy {tuple(_LEGACY_GOLD)}). Refusing to guess."
    )


def must_keep(case):
    """True when the recorded correct action is to KEEP the finding."""
    return gold_verdict(case) == KEEP


def must_dismiss(case):
    """True when the recorded correct action is to DISMISS the finding."""
    return gold_verdict(case) == DISMISS


def is_scored(case):
    """False for rows whose correct verdict could not be reconstructed."""
    return gold_verdict(case) != UNRESOLVED
