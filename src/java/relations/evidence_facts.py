"""Pure evidence-fact builders for the soundness / attribution judges.

Every function here is a pure function of primitives (strings, sets,
lists, None) — no I/O, no subprocess, no LLM. The wrong-fact bugs that
shipped (a replay that said "clean" when it errored; a shadowed replay
that extrapolated a screening confirmation; a lifted-test note that said
"dismiss" without ever comparing values) all lived inline in run.py where
they could not be unit-tested. Moving the fact logic here makes it
testable against extracted trace strings without a JVM.

The three note-text families are generated ONLY in this module:
  * differential replay          -> classify_differential_replay
  * semantic buggy-replay ladder -> semantic_buggy_replay_note
  * trigger-test lift            -> trigger_lift_note (+ fired_value_vs_trusted)

run.py computes the mechanical inputs (replay outcome, fired ids, exception
types) and does the printing; the note wording is assembled here.
"""

import math
import re

# Fire-rate thresholds (Spec H). MAX_FIRE_RATIO lives HERE, not in
# relation_screen, so this pure module stays importable without the
# JVM/harness stack (relation_screen pulls in HarnessBuilder, fuzz_runner and
# llm); relation_screen imports the constant from here. No import cycle exists
# in either direction (each module is imported lazily only by run.py and
# neither imports the other today), so the direction is chosen purely to keep
# evidence_facts unit-testable in isolation.
#
#   MAX_FIRE_RATIO      — a relation violated on more than this share of random
#     valid inputs on the buggy build is out-of-domain (the buggy build is
#     known-correct on the overwhelming majority of inputs); on the PATCHED
#     build the same share indicts the check rather than the patch.
#   INTRINSIC_FIRE_RATIO — at or above this share the firing is structural
#     (fires on essentially every input), not a detection of the defect.
MAX_FIRE_RATIO = 0.20
INTRINSIC_FIRE_RATIO = 0.95

# Oracle-id shapes, reimplemented locally so this module stays dependency
# light (java.parsing.java_source.oracle_ids_in_text uses the same two
# regexes). An "[oracle:<id>]" tag or a "relation <name> violated" phrase
# marks a throw the harness raised itself (its own check), as opposed to a
# library exception that merely escaped.
_ORACLE_ID_RE = re.compile(r'\[oracle:([-\w]+)\]')
_RELATION_ID_RE = re.compile(r'relation\s+([-\w]+)\s+violated')


def _oracle_ids_in_text(text):
    """Every oracle ID mentioned in `text`, under either accepted shape."""
    ids = set(_ORACLE_ID_RE.findall(text or ''))
    ids.update(_RELATION_ID_RE.findall(text or ''))
    return ids


def _is_harness_alarm(sig):
    """True when `sig` is one of the harness's OWN checks firing, not a
    library exception that escaped: a `FuzzerSecurityIssue*` throwable or a
    string carrying an `[oracle:<id>]` / `relation <name> violated` tag."""
    if not sig:
        return False
    if 'FuzzerSecurityIssue' in sig:
        return True
    return bool(_oracle_ids_in_text(sig))


# ---------------------------------------------------------------------------
# Spec B — differential replay (generic-escape attribution)
# ---------------------------------------------------------------------------

def classify_differential_replay(patched_sig, buggy_status, buggy_sig):
    """Attribute a patched-build generic crash by replaying its exact input
    on the buggy build.

    `buggy_status` in {"crashed", "clean", "error"} (from
    FuzzRunner.replay_input_result). `buggy_sig` is the buggy-build
    crash_signature when it crashed, else None. The caller has already
    guaranteed `patched_sig` carries a stack-frame anchor ('@').

    Returns (verdict, note) with verdict in
    {"INTRODUCED", "PREEXISTING", "SHADOWED", "ABSTAIN"}:

      * error   -> ABSTAIN: the replay itself failed, so it manufactures no
        fact for or against the patch.
      * crashed with the SAME signature -> PREEXISTING (the caller keeps its
        existing drop behaviour, including the non-generic-sibling rescue).
      * crashed at the harness's OWN alarm while the patched firing is an
        escaped (non-alarm) exception -> SHADOWED: the buggy run died at a
        check before ever reaching the patched crash site, so the comparison
        is uninformative, not exculpatory-for-buggy.
      * a DIFFERENT non-alarm signature, or a clean completion -> INTRODUCED
        (now trustworthy: "clean" really means "ran to completion").
    """
    if buggy_status == "error":
        return ("ABSTAIN",
                "differential replay ABSTAINED: replaying the exact firing "
                "input on the buggy build was unavailable (the replay itself "
                "errored) — no attribution fact either way. Judge "
                "sceptically; do NOT read the absence of a buggy-side crash "
                "here as evidence the patch introduced this one.")

    if buggy_status == "crashed" and buggy_sig == patched_sig \
            and '@' in (buggy_sig or ''):
        return ("PREEXISTING",
                "differential replay: the exact firing input reproduces this "
                "same crash on the buggy build (pre-existing surface, not "
                "patch-caused).")

    if (buggy_status == "crashed"
            and _is_harness_alarm(buggy_sig)
            and not _is_harness_alarm(patched_sig)):
        return ("SHADOWED",
                "differential replay SHADOWED: on the buggy build this exact "
                "input triggers the harness's own check " + str(buggy_sig)
                + " before execution reaches the site that crashed on the "
                "patched build (" + str(patched_sig) + ") — the replay is "
                "uninformative about whether this crash pre-exists. Do NOT "
                "treat this as evidence the patch introduced the crash. If "
                "the crash site lies in harness code that only executes when "
                "all checks pass, the firing is a harness artifact that can "
                "only ever appear on a correct build.")

    return ("INTRODUCED",
            "differential replay: the exact firing input does NOT reproduce "
            "this crash on the buggy build (patched=" + str(patched_sig)
            + ", buggy=" + (buggy_sig or 'no crash') + ") — the crash is "
            "introduced by the patch")


# ---------------------------------------------------------------------------
# Spec C — semantic buggy-replay note ladder (port of the inline ladder,
# with the shadowed branch rewritten so it no longer extrapolates a
# screening DIRECTION-CONFIRMED fact from screening inputs to this firing).
# ---------------------------------------------------------------------------

def semantic_buggy_replay_note(fired_ids, breplay_status, breplay_ids,
                               bt_all, bt_defect, esc_type, idline="",
                               value_verdict="unknown",
                               buggy_msg_excerpt=None,
                               patched_msg_excerpt=None):
    """Build the "[buggy-replay fact]" note for one semantic-leg firing.

    `breplay_status` in {"crashed", "clean", "error", "unavailable"}; the old
    `_breplay_ids is None` case maps to "error"/"unavailable". `fired_ids`
    are the oracle ids the patched firing carries (empty for an escaped
    exception, in which case `esc_type` names the escaped throwable).
    `breplay_ids` is the set of oracle ids that fired on the buggy replay
    (may be empty), `bt_all` every exception type seen in that replay, and
    `bt_defect` the subset that is the reported defect. `idline` is a
    pre-assembled exception-identity fragment appended only to the
    same-check / no-defect branch (its data needs the raw replay output and
    so is built by the caller).

    Returns the note text, or None when there is nothing to say.
    """
    fired_ids = fired_ids or set()
    bt_all = bt_all or set()
    bt_defect = bt_defect or set()
    breplay_ids = breplay_ids if breplay_ids is not None else set()
    unavailable = breplay_status in ("error", "unavailable")

    if fired_ids:
        if unavailable:
            return ("[buggy-replay fact] replaying this firing's exact input "
                    "on the buggy build was unavailable — no attribution "
                    "fact; judge on soundness alone, sceptically.")
        if fired_ids & breplay_ids:
            if bt_defect:
                return ("[buggy-replay fact] the exact firing input fires "
                        "the SAME check on the BUGGY build AND the reported "
                        "defect exception appears there ("
                        + ", ".join(sorted(bt_defect)[:4])
                        + ") — the input lies inside the reported bug's own "
                        "family and the patch did not change the outcome: "
                        "the patch-failed-to-fix pattern, IF the harness "
                        "constructed this input as valid; on fuzzed junk "
                        "both builds merely reject and nothing is convicted.")
            # Spec I: the same check firing on BOTH builds is NOT the same as
            # identical VALUES. The value verdict (from compare_fired_values)
            # decides which wording is licensed; only "identical" earns the
            # identical-on-both-builds claim the MECHANICAL-FACTS rule binds on.
            if value_verdict == "different":
                return ("[buggy-replay fact] the SAME check fires on BOTH "
                        "builds but with DIFFERENT observed values (buggy: "
                        + _excerpt(buggy_msg_excerpt) + " vs patched: "
                        + _excerpt(patched_msg_excerpt) + ") — the patch "
                        "changed behaviour at this input without restoring "
                        "the expected value: the partial-fix pattern; this "
                        "firing remains evidence against the patch."
                        + (idline or ""))
            if value_verdict == "identical":
                return ("[buggy-replay fact] the exact firing input fires the "
                        "SAME check on the BUGGY build with the SAME observed "
                        "values — behaviour at this input is identical on "
                        "both builds; the patch did not cause or preserve "
                        "anything here. No contract argument can rescue this "
                        "finding: if the asserted contract is real, the "
                        "UNPATCHED code violates it identically at this "
                        "input, making it pre-existing surface by "
                        "definition. The REAL failing test was rerun on this "
                        "patched build and PASSES, so the test's own scenario "
                        "is settled in the patch's favour. Keep this finding "
                        "ONLY if it asserts the very behaviour the failing "
                        "test shows is wrong, at inputs the real test does "
                        "NOT itself exercise; otherwise dismiss."
                        + (idline or ""))
            # unknown: no observed value could be compared — state the
            # fires-on-both fact WITHOUT the "identical" over-claim, and keep
            # the bug's-own-family keep/dismiss guidance.
            return ("[buggy-replay fact] the exact firing input fires the "
                    "SAME check on the BUGGY build (observed values were not "
                    "compared, so no identical-value claim is made). The REAL "
                    "failing test was rerun on this patched build and PASSES, "
                    "so the test's own scenario is settled in the patch's "
                    "favour. Keep this finding ONLY if it asserts the very "
                    "behaviour the failing test shows is wrong, at inputs the "
                    "real test does NOT itself exercise; otherwise it measures "
                    "pre-existing surface — dismiss." + (idline or ""))
        if bt_defect:
            return ("[buggy-replay fact] on this exact input the BUGGY build "
                    "produces the reported defect exception ("
                    + ", ".join(sorted(bt_defect)[:4])
                    + ") while the patched build completes. Turning a defect "
                    "input's crash into completion is exactly what a FIX "
                    "does — and also what a crash-suppressing overfit does; "
                    "they differ ONLY in whether the completed value is "
                    "correct. The crash's disappearance is not evidence "
                    "either way. Judge solely the condemned completed value: "
                    "if it violates a documented contract, this is the "
                    "overfit pattern (SOUND); if the check demands more than "
                    "the documented contract guarantees, it is UNSOUND.")
        if breplay_ids:
            # Spec C: a DIFFERENT check fired first on the buggy build at
            # THIS input, so whether THIS check fires there is UNKNOWN. The
            # old wording licensed extrapolating a screening
            # DIRECTION-CONFIRMED fact (established at screening inputs) to
            # this firing input; that convicted two correct patches.
            return ("[buggy-replay fact] on this exact input a DIFFERENT "
                    "check fired first on the buggy build ("
                    + ", ".join(sorted(breplay_ids)[:4])
                    + "), so whether THIS check fires there is UNKNOWN — the "
                    "replay is shadowed, not confirming. The screening "
                    "DIRECTION-CONFIRMED fact was established on screening "
                    "inputs, which may lie in a different input regime than "
                    "this firing; it does NOT by itself establish the buggy "
                    "build violates this check at THIS input. With no "
                    "per-input attribution fact, judge on soundness alone, "
                    "sceptically: to keep, the check's expected value must be "
                    "justified by a shown contract or trusted value that "
                    "covers THIS input's regime.")
        if bt_all:
            return ("[buggy-replay fact] on this exact input the buggy build "
                    "neither fires this check nor shows the reported defect "
                    "(it raises " + ", ".join(sorted(bt_all)[:4])
                    + " instead) — attribution unclear. Judge the check "
                    "against the documented contract, and weigh whether its "
                    "observable is the very behaviour the failing test shows "
                    "is wrong: a DIFFERENT feature's contract with no "
                    "buggy-side evidence behind it is the classic "
                    "false-positive shape — keep only with a shown contract "
                    "the observed value contradicts.")
        return ("[buggy-replay fact] the buggy build handles this exact "
                "input cleanly WITHOUT firing this check — the patch "
                "INTRODUCED the violation here, and the buggy build is an "
                "existence proof that real code satisfies the asserted "
                "property on this input. 'A correct implementation might "
                "legitimately violate it' is not available as grounds; to "
                "answer UNSOUND you must point at a documented contract the "
                "assertion contradicts.")

    if esc_type:
        if unavailable:
            return ("[buggy-replay fact] replaying this escaped exception's "
                    "exact input on the buggy build was unavailable — no "
                    "attribution fact; judge on soundness alone, "
                    "sceptically.")
        esc_same_on_buggy = esc_type in bt_all
        if esc_type in bt_defect:
            return ("[buggy-replay fact] this escaped exception IS the "
                    "reported defect type and the SAME exception occurs on "
                    "the BUGGY build at this exact input — identical crash "
                    "on both builds. Two readings, decided by the INPUT: if "
                    "the harness constructed this input to be valid by "
                    "construction, the patch left the reported defect "
                    "unfixed here; if it is fuzzed junk no implementation is "
                    "obliged to accept, this is pre-existing malformed-input "
                    "surface that even a correct fix retains — dismiss.")
        if esc_same_on_buggy:
            return ("[buggy-replay fact] the BUGGY build raises this same "
                    "exception type (" + esc_type + ") at this exact input "
                    "— identical rejection on both builds; the patch did not "
                    "change this behaviour. Pre-existing input-rejection "
                    "surface — dismiss.")
        if bt_defect:
            return ("[buggy-replay fact] at this exact input the BUGGY build "
                    "produces the reported defect ("
                    + ", ".join(sorted(bt_defect)[:4])
                    + ") while the patched build raises " + esc_type
                    + " instead — the patch changed the failure mode at a "
                    "defect input. Judge whether the new exception is a "
                    "documented, acceptable rejection or nonsense wearing an "
                    "exception type.")
        return ("[buggy-replay fact] the buggy build handles this exact "
                "input WITHOUT raising " + esc_type + " — the patch "
                "INTRODUCED this exception here. On an input the harness "
                "constructed to be valid, that is strong evidence against "
                "the patch; on fuzzed junk it is ordinary rejection surface "
                "— decide by the harness's input construction.")

    return None


# ---------------------------------------------------------------------------
# Spec D — trigger-test-lift note (compare values before saying "dismiss")
# ---------------------------------------------------------------------------

# Numbers tagged as the OBSERVED value, and a bare trailing number.
_TAGGED_NUM_RE = re.compile(
    r'(?:actual|got|was)\s*=\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)', re.I)
_TRAILING_NUM_RE = re.compile(
    r'(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*$')
# Any numeric literal (used to mine trusted / lifted literals).
_NUM_RE = re.compile(r'-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?')


def _close(a, b, rel=1e-9):
    """Rounding-floor equality: |a - b| <= rel * max(1, |a|, |b|)."""
    return abs(a - b) <= rel * max(1.0, abs(a), abs(b))


def _fired_numbers(fired_msg):
    """Numeric values from a fired message: those tagged actual=/got=/was=
    plus a bare trailing number (the shapes an observed value prints in)."""
    if not fired_msg:
        return []
    raw = _TAGGED_NUM_RE.findall(fired_msg)
    m = _TRAILING_NUM_RE.search(fired_msg.rstrip())
    if m:
        raw.append(m.group(1))
    out = []
    for s in raw:
        try:
            out.append(float(s))
        except (TypeError, ValueError):
            pass
    return out


def _trusted_numbers(trusted_values):
    """Every numeric literal appearing in the trusted/lifted values."""
    out = []
    for v in (trusted_values or []):
        for s in _NUM_RE.findall(str(v)):
            try:
                out.append(float(s))
            except (TypeError, ValueError):
                pass
    return out


def fired_value_vs_trusted(fired_msg, trusted_values):
    """Mechanically compare the fired message's observed value against the
    values the trigger test itself pins.

    Returns:
      * "matches"  — some fired number equals some trusted number within the
        rounding floor.
      * "differs"  — numeric values are present on BOTH sides but none match.
      * "unknown"  — no numeric values are extractable on one or both sides.
    """
    fired_nums = _fired_numbers(fired_msg)
    trusted_nums = _trusted_numbers(trusted_values)
    if not fired_nums or not trusted_nums:
        return "unknown"
    for a in fired_nums:
        for b in trusted_nums:
            if _close(a, b):
                return "matches"
    return "differs"


# ---------------------------------------------------------------------------
# Spec I (cycle-2b hotfix) — "identical" requires a VALUE comparison. The
# same-check buggy-replay fact and the muted-replay fires-on-both fact claimed
# "behaviour at this input is identical on both builds" knowing only that the
# same check FIRED on both. Firing on both != identical values: a
# partially-unfixed overfit fires the same check on both builds with DIFFERENT
# observed values (both wrong, differently). With the MECHANICAL-FACTS rule
# binding, that over-claim mechanically dismissed genuine catches (c2flag
# Math-68). This compares the observed numerics before any identical claim.
# ---------------------------------------------------------------------------

def _observed_numbers(msg):
    """Numbers that are the check's OBSERVED value, for cross-build
    comparison: the actual=/got=/was=-tagged numbers when any exist, else the
    bare trailing number as a fallback. Deliberately NARROWER than
    `_fired_numbers`: a message like "actual=4.94 expected=6.99" shares its
    expected= reference with the other build's message, and including that
    shared literal would make two DIFFERENT observations compare "identical"
    (the false match that would have re-killed a partial-fix catch)."""
    if not msg:
        return []
    raw = _TAGGED_NUM_RE.findall(msg)
    if not raw:
        m = _TRAILING_NUM_RE.search(msg.rstrip())
        if m:
            raw = [m.group(1)]
    out = []
    for s in raw:
        try:
            out.append(float(s))
        except (TypeError, ValueError):
            pass
    return out


# Any `<name>=<value>` pair in a fired message, numeric or the IEEE tokens.
# Same-oracle messages share a format, so pairwise comparison of the SHARED
# keys is the general cross-build comparison: reference keys (expected=,
# tol=) and input keys (n=) are equal on both sides by construction and
# cannot cause a false "different"; an observed key (actual=, p=, whatever
# the oracle author named it) that differs beyond the floor is a real
# behavioural difference at this input.
_KV_PAIR_RE = re.compile(
    r'([A-Za-z_]\w*)\s*=\s*'
    r'(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|NaN|-?Infinity)')


def _kv_values(msg):
    """{key: [float values]} for every key=value pair in `msg` (NaN/Infinity
    parsed as their IEEE floats; a repeated key keeps every occurrence)."""
    out = {}
    for k, v in _KV_PAIR_RE.findall(msg or ''):
        try:
            out.setdefault(k, []).append(float(v))
        except (TypeError, ValueError):
            pass
    return out


def _vals_match(a, b):
    if math.isnan(a) or math.isnan(b):
        return math.isnan(a) and math.isnan(b)
    if math.isinf(a) or math.isinf(b):
        # The rounding floor is meaningless at infinity (inf <= inf), and
        # +Infinity vs -Infinity must never match.
        return a == b
    return _close(a, b)


def compare_fired_values(patched_msg, buggy_msg):
    """Compare the observed values of the SAME check firing on both builds.

    Comparison ladder:
    1. Textually identical messages -> "identical".
    2. key=value pairwise: on the keys BOTH messages carry, every shared key
       matching (NaN-safe, `_close` floor; multi-valued keys match as
       multisets-with-floor) -> "identical"; any shared key differing ->
       "different". Requires at least one shared key.
    3. Fallback for tag-less messages: `_observed_numbers`
       (actual=/got=/was=-tagged, trailing-number fallback) any-match ->
       "identical" / no-match -> "different"; no numbers on a side ->
       "unknown".
    """
    # Textually identical messages fire identically even with no numerics
    # (e.g. a NaN message: "...expected p-value 1.0 but got NaN").
    if patched_msg and buggy_msg \
            and str(patched_msg).strip() == str(buggy_msg).strip():
        return "identical"

    p_kv, b_kv = _kv_values(patched_msg), _kv_values(buggy_msg)
    shared = [k for k in p_kv if k in b_kv]
    if shared:
        for k in shared:
            pv, bv = list(p_kv[k]), list(b_kv[k])
            for a in pv:
                hit = next((i for i, b in enumerate(bv)
                            if _vals_match(a, b)), None)
                if hit is None:
                    return "different"
                bv.pop(hit)
        return "identical"

    p_nums = _observed_numbers(patched_msg)
    b_nums = _observed_numbers(buggy_msg)
    if not p_nums or not b_nums:
        return "unknown"

    for a in p_nums:
        for b in b_nums:
            if _vals_match(a, b):
                return "identical"
    return "different"


# ---------------------------------------------------------------------------
# Spec J (cycle-3) — re-armed mechanical identical-drop with the trigger-input
# exemption. Two pure flags decide the ladder in run.py; both share ONE
# distinctiveness rule so a trivially-common literal (a bare 0/1, a two-char
# string) can never satisfy either — the guard that Math-2/Math-30 need.
#
# Distinctiveness rule (shared):
#   * NUMERIC literal qualifies only with >=4 significant digits — strip the
#     sign, any type suffix (LlFfDd), the exponent, underscores, the decimal
#     point and leading zeros, then count the digits that remain. So "6.283"
#     -> "6283" (4, YES), "50" -> "50" (2, NO), "0.0" -> "" (0, NO).
#   * STRING literal qualifies only if len(whitespace-normalized) >= 8.
# ---------------------------------------------------------------------------

_WS_RE = re.compile(r'\s+')
_TYPE_SUFFIX = 'LlFfDd'
# A token that is a bare numeric literal (Java number, underscores allowed).
_NUMERIC_LITERAL_RE = re.compile(
    r'^[+-]?[\d_]+(?:\.[\d_]+)?(?:[eE][+-]?\d+)?[LlFfDd]?$')
# Quoted string contents inside a fired message.
_QUOTED_RE = re.compile(r'"((?:[^"\\]|\\.){0,200})"')
# An EXPECTED-side tag: any key containing "expected" (case-insensitive),
# value running up to the next ` <key>=` pair or end of message. Captures a
# STRING value as readily as a numeric one (the fired copy of a test's pinned
# assertEquals literal is a string).
_EXPECTED_TAG_RE = re.compile(
    r'(\w*expected\w*)\s*=\s*(.*?)(?=\s+\w+\s*=|$)', re.IGNORECASE)


def _ws_norm(s):
    """Whitespace-normalize for matching: remove ALL whitespace. The fired
    trace collapses a multi-line assert literal (newlines/indentation gone)
    while the trusted test literal keeps them, so equality survives only once
    every whitespace character is stripped from BOTH sides."""
    return _WS_RE.sub('', str(s or ''))


def _is_numeric_literal(s):
    return bool(_NUMERIC_LITERAL_RE.match(str(s).strip()))


def _to_float(s):
    """Parse a numeric literal/token to float (type suffix + underscores
    stripped; NaN/Infinity tokens accepted). None on failure."""
    try:
        core = str(s).strip().rstrip(_TYPE_SUFFIX).replace('_', '')
        return float(core)
    except (TypeError, ValueError):
        return None


def _numeric_is_distinctive(s):
    """>=4 significant digits after stripping sign, type suffix, exponent,
    underscores, the decimal point and leading zeros."""
    core = re.split(r'[eE]', str(s).strip().rstrip(_TYPE_SUFFIX), 1)[0]
    core = core.lstrip('+-').replace('.', '').replace('_', '').lstrip('0')
    return len(core) >= 4


def _literal_is_distinctive(s):
    """Shared distinctiveness gate: numeric rule for a numeric literal, string
    rule (>=8 chars whitespace-normalized) otherwise."""
    if _is_numeric_literal(s):
        return _numeric_is_distinctive(s)
    return len(_ws_norm(s)) >= 8


def _quoted_strings(msg):
    return _QUOTED_RE.findall(msg or '')


def expected_is_test_literal(fired_msg, trusted_values):
    """Spec J.2a flag: does the fired message's EXPECTED-side value match one
    of the failing test's own assert literals (`trusted_values`), distinctively?

    Expected-side extraction: numbers tagged with any key containing
    "expected" (case-insensitive, e.g. expected=/expectedX=); for a message
    carrying NO expected-tag at all, the whole message's quoted strings and
    numeric literals. A numeric match counts only within the `_close` floor
    AND when the fired literal is distinctive (>=4 significant digits); a
    string match only by whitespace-normalized containment (either direction)
    AND when the shared (shorter) text is distinctive (>=8 chars). Pure."""
    fired_msg = fired_msg or ''
    trusted = [str(t) for t in (trusted_values or []) if str(t).strip()]
    if not fired_msg or not trusted:
        return False
    # EXPECTED-side candidate values: the expected-tag values (string OR
    # numeric); only when the message carries NO expected-tag at all do we
    # fall back to the whole message's quoted strings and numeric literals.
    exp_vals = [v for _k, v in _EXPECTED_TAG_RE.findall(fired_msg)]
    if not exp_vals:
        exp_vals = _quoted_strings(fired_msg) + _NUM_RE.findall(fired_msg)
    # Numeric pool from the trusted literals (mine embedded numbers too).
    trusted_nums = []
    for t in trusted:
        for m in _NUM_RE.findall(t):
            f = _to_float(m)
            if f is not None:
                trusted_nums.append(f)
    for raw in exp_vals:
        raw = str(raw).strip()
        if not raw:
            continue
        # Numeric match — distinctive literal (>=4 sig digits), `_close` floor.
        if _is_numeric_literal(raw) and _numeric_is_distinctive(raw):
            fe = _to_float(raw)
            if fe is not None:
                for ft in trusted_nums:
                    if _vals_match(fe, ft):
                        return True
        # String match — whitespace-normalized containment, either direction,
        # the shared (shorter) text distinctive (>=8 chars).
        en = _ws_norm(raw)
        if len(en) >= 8:
            for t in trusted:
                tn = _ws_norm(t)
                if not tn:
                    continue
                if en in tn or tn in en:
                    shorter = en if len(en) <= len(tn) else tn
                    if len(shorter) >= 8:
                        return True
    return False


def fired_at_test_input(fired_msg, trigger_literals):
    """Spec J.2b flag: does the fired message fire AT the failing test's own
    input seeds? Parse `trigger_literals` (list of strings — numeric and
    string seeds) and match any DISTINCTIVE one against the fired message's
    key=value VALUES (`_KV_PAIR_RE`, numeric/NaN/Infinity) or its quoted
    strings. Numeric: `_vals_match` floor. String: whitespace-normalized
    containment (either direction, shared text >=8 chars). Pure."""
    fired_msg = fired_msg or ''
    lits = [str(x) for x in (trigger_literals or []) if str(x).strip()]
    if not fired_msg or not lits:
        return False
    fired_nums = []
    for _k, v in _KV_PAIR_RE.findall(fired_msg):
        f = _to_float(v)
        if f is not None:
            fired_nums.append(f)
    fired_strs = [_ws_norm(q) for q in _quoted_strings(fired_msg)]
    for lit in lits:
        if not _literal_is_distinctive(lit):
            continue
        if _is_numeric_literal(lit):
            fl = _to_float(lit)
            if fl is None:
                continue
            for fn in fired_nums:
                if _vals_match(fl, fn):
                    return True
        else:
            ln = _ws_norm(lit)
            for fs in fired_strs:
                if ln and (ln in fs or fs in ln):
                    shorter = ln if len(ln) <= len(fs) else fs
                    if len(shorter) >= 8:
                        return True
    return False


def _excerpt(msg, cap=120):
    """Truncate a fired message to ~`cap` chars for inline quoting in a note."""
    if not msg:
        return ""
    s = str(msg).strip()
    return s if len(s) <= cap else (s[:cap] + "...")


def trigger_lift_note(lifted_names, generic_lift, value_verdict):
    """Note for a fired oracle that LIFTS a trigger test. The name regex is
    only a *detector* of lift provenance; the dismissal instruction is
    licensed by the value comparison alone (Spec D):

      * matches  — the observed value IS the test's own scenario; the test
        passes there, so dismiss (the only branch allowed to say "must be
        dismissed").
      * differs  — the fired value diverges from every value the test pins:
        a candidate generalization catch beyond the test's inputs;
        test-passage does not exonerate it.
      * unknown  — no value could be compared; state the neutral fact only.

    Returns the note text, or None when no lift was detected.
    """
    if not (lifted_names or generic_lift):
        return None
    which = (", ".join(sorted(lifted_names)) if lifted_names
             else "the failing test (generic lift id)")

    if value_verdict == "matches":
        return ("[trigger-test lift] this oracle lifts " + which
                + " — the REAL test was rerun on this patched build and "
                "PASSES. The observed value matches a value the test itself "
                "pins within the rounding floor, so this firing replays the "
                "TEST's own scenario, where a correct patch passes — it is "
                "harness-setup divergence and must be dismissed.")

    if value_verdict == "differs":
        return ("[trigger-test lift] this oracle lifts " + which
                + "; the REAL test passes on this build, BUT the fired value "
                "differs from every value the test itself pins by more than "
                "the rounding floor — this is NOT a replay of the test's "
                "scenario; it is a candidate generalization catch beyond the "
                "test's inputs. Test-passage does NOT exonerate it. Judge "
                "its soundness on the shown contract/trusted values.")

    return ("[trigger-test lift] this oracle lifts " + which
            + "; the REAL test passes on this build. Whether this firing "
            "replays the test's own scenario is undetermined (no numeric "
            "value could be compared) — judge on soundness alone.")


# ---------------------------------------------------------------------------
# Cycle-5A — trigger-tier fact, NEUTRALIZED to symmetric. The shipped inline
# wording ("... Dismiss unless the rule's asserted property is justified by
# the documented contract INDEPENDENT of the failing test's specific setup")
# coached the judge to discard its cleanest drift-kill catches (inventory
# 2026-07-26 §c rows 1-4). This states the mechanical fact WITHOUT a dismiss
# lean; keep/dismiss is decided by the value comparison + the contract source,
# not by trigger-tier presence. Wording deliberately avoids any
# identical-on-both / fires-on-buggy phrase so the 5C terminal detector does
# not fire on it.
# ---------------------------------------------------------------------------

def trigger_tier_note():
    """The neutral, symmetric [trigger-tier fact] block. Pure, no inputs."""
    return (
        "\n[trigger-tier fact] this rule fires on the failing test's OWN "
        "input literals on THIS patched build, and the REAL failing test was "
        "rerun here and PASSES. This LOCATES the firing at the test's own "
        "scenario; it is not, by itself, grounds either way. TWO causes are "
        "equally live: (a) benign — the rule reconstructs the scenario "
        "WITHOUT the test's setup wiring (source/registered files/locale), so "
        "it fires where a faithful copy would not; (b) a real catch — the "
        "patch left the test's own defect intact on an observable the test "
        "does not itself pin. Decide between them by comparing the fired "
        "value against what the test pins and by whether the rule's asserted "
        "property has a documented contract — exactly as for any other "
        "firing.")


# ---------------------------------------------------------------------------
# Spec G-G3 — muted per-check replay note (the definitive shadowed-replay
# fact). When a firing's buggy-side replay is shadowed — a DIFFERENT check (or
# the harness's own oracle before an escaped-crash site) throws first — the
# per-input question "does THIS check fire / crash on the buggy build?" is
# uncomputable, and the honest UNKNOWN note left a vacuum the judge filled with
# stories. Silencing the shadowing throws and re-replaying computes the missing
# fact; this builder words the three outcomes.
# ---------------------------------------------------------------------------

def muted_replay_note(target_ids, muted_ids, status, fired_ids,
                      esc_type, bt_all, value_verdict="unknown",
                      buggy_msg_excerpt=None, patched_msg_excerpt=None):
    """Word the outcome of ONE muted re-replay on the buggy build.

    The shadowing checks `muted_ids` were mechanically silenced and the exact
    firing input replayed again. `target_ids` are the oracle ids the patched
    firing carries (empty for an escaped exception, in which case `esc_type`
    names the escaped throwable). `status` in {"crashed", "clean", "error",
    "mute_failed"} is the muted replay's status; `fired_ids` the oracle ids
    that fired on it; `bt_all` every exception type seen in its output.

    Spec I: `value_verdict` (from compare_fired_values on the target check's
    buggy vs patched fired messages) gates the target-fires wording — only
    "identical" earns the identical-on-both-builds claim; "different" yields the
    partial-fix wording; the DEFAULT "unknown" states fires-on-both WITHOUT the
    identical claim, so an unthreaded call can never over-claim.

    Returns:
      * target fires (its oracle id — or, for an escaped firing, its exception
        type — appears in the muted replay) -> the fires-on-both-builds family;
        identical-on-both-builds ONLY when value_verdict=="identical".
      * target quiet + a CLEAN run -> the existence-proof family: the buggy
        build runs this exact input without the violation; the patch
        introduced it.
      * status error/mute_failed -> a one-line note that a muted re-replay was
        attempted and unavailable (the caller appends it to the cycle-1 UNKNOWN
        note, which stands unchanged).
      * target quiet but the run still crashed at something ELSE (neither the
        target nor a clean completion) -> None: nothing new was learned, so the
        cycle-1 UNKNOWN note is left intact.
    """
    target_ids = target_ids or set()
    muted_ids = muted_ids or set()
    fired_ids = fired_ids or set()
    bt_all = bt_all or set()

    if status in ("error", "mute_failed"):
        return ("[muted-replay fact] a muted re-replay was attempted and "
                "unavailable.")

    ids_txt = ", ".join(sorted(muted_ids)[:4]) or "the shadowing check(s)"

    if target_ids:
        target_fired = bool(target_ids & fired_ids)
    elif esc_type:
        target_fired = esc_type in bt_all
    else:
        target_fired = False

    if target_fired:
        # Spec I: firing on both builds is NOT identical values. Only an
        # explicit "identical" value verdict earns the identical-on-both-builds
        # wording; the DEFAULT ("unknown") never over-claims, so an unthreaded
        # call can never assert identical.
        if value_verdict == "different":
            return ("[muted-replay fact] with the shadowing check(s) "
                    + ids_txt + " silenced, the SAME check fires on BOTH "
                    "builds but with DIFFERENT observed values (buggy: "
                    + _excerpt(buggy_msg_excerpt) + " vs patched: "
                    + _excerpt(patched_msg_excerpt) + ") — the patch changed "
                    "behaviour at this input without restoring the expected "
                    "value: the partial-fix pattern; this firing remains "
                    "evidence against the patch.")
        if value_verdict == "identical":
            return ("[muted-replay fact] with the shadowing check(s) "
                    + ids_txt + " silenced, THIS check fires on the BUGGY "
                    "build at this exact input with the SAME observed values "
                    "— behaviour is identical on both builds; the patch did "
                    "not cause this. No contract argument can rescue the "
                    "finding: if the asserted contract is real, the UNPATCHED "
                    "code violates it identically at this input — "
                    "pre-existing surface by definition. Keep ONLY under the "
                    "patch-failed-to-fix pattern (the violated property is "
                    "the failing test's own observable, beyond the test's "
                    "own inputs).")
        return ("[muted-replay fact] with the shadowing check(s) " + ids_txt
                + " silenced, THIS check fires on the BUGGY build at this "
                "exact input — the same check fires on both builds (observed "
                "values were not compared, so no identical-value claim is "
                "made); judge the check's soundness on the shown contract.")

    if status == "clean":
        return ("[muted-replay fact] with the shadowing check(s) " + ids_txt
                + " silenced, the buggy build runs this exact input WITHOUT "
                "firing this check — the patch introduced the violation here.")

    # Target quiet, but the muted run still crashed at some OTHER site: the
    # per-input question stays unanswered, so add no new fact.
    return None


# ---------------------------------------------------------------------------
# Spec H — fire-rate facts. The pipeline already computes the numbers that
# indict a broken check (buggy-side screen ratio and patched-side replay-fuzz
# counts), but they reach the judge unlabelled. A check firing on a large share
# of RANDOM VALID inputs on the patched (or both) build(s) contradicts
# known-good behaviour broadly — that indicts the check, not the patch.
# ---------------------------------------------------------------------------

def fire_rate_fact(buggy_checked, buggy_violated, patched_checked,
                   patched_violated, screen_outcome_reason):
    """Build a "[fire-rate fact]" block from the screen/replay counts.

    `*_checked` / `*_violated` are the raw counts from the buggy-side screen
    and the patched-side replay-fuzz (either pair may be None/0 when that
    measurement is unavailable). `screen_outcome_reason` is the screening
    demotion reason (e.g. "above-ratio-cap / inverted (replay-only)"), included
    verbatim when non-empty.

    Returns the fact block, or None when neither the patched nor the buggy rate
    crosses its threshold (or the counts are missing) — no noise.
    """
    # Cycle-5A: per-input denominator normalization. A multi-case oracle can
    # count several firings per input, so `violated` may EXCEED `checked`
    # (night20 Math-68 printed "2997/1000 = 300%"). Clamp the RATE at 1.0 — a
    # check that fires on every input is the ceiling; there is no ">100%".
    def _rate(violated, checked):
        if not checked or checked <= 0 or violated is None:
            return None
        return min(1.0, violated / checked)

    p_rate = _rate(patched_violated, patched_checked)
    b_rate = _rate(buggy_violated, buggy_checked)

    # Cycle-5A: TWO-SIDED interpretation. The old note had only the
    # "patched-high => indicts the check" reading and applied it even to
    # buggy≈0 / patched-high — the STRONGEST catch signal (silent on the
    # known-broken build, fires on the patch = the patch introduced the
    # divergence). That wording coached the judge to discard its best
    # catches (inventory 2026-07-26). Distinguish the two profiles:
    interp = None
    both_high = (p_rate is not None and p_rate >= MAX_FIRE_RATIO
                 and b_rate is not None and b_rate >= MAX_FIRE_RATIO)
    asymmetric = (p_rate is not None and p_rate >= MAX_FIRE_RATIO
                  and b_rate is not None and b_rate < MAX_FIRE_RATIO)
    if both_high:
        interp = ("fires on a large share of random valid inputs on BOTH "
                  "builds (buggy {:.0%}, patched {:.0%}) — indiscriminate; "
                  "the firing is intrinsic to the check/setup construction, "
                  "not a detection of the defect. Keep only with a shown "
                  "contract that makes every one of those inputs a genuine "
                  "violation.".format(b_rate, p_rate))
    elif asymmetric:
        interp = ("fires on {:.0%} of random valid inputs on the PATCHED "
                  "build but only {:.0%} on the buggy build — the check is "
                  "silent (or near-silent) on the known-broken code and "
                  "loud on the patch, i.e. the PATCH introduced this "
                  "divergence. This is a strong discrimination signal, NOT "
                  "grounds to indict the check; dismiss only with a shown "
                  "reason the patched-only firings are legitimate.".format(
                      p_rate, b_rate))
    elif p_rate is not None and p_rate >= MAX_FIRE_RATIO:
        # patched-high, buggy rate UNKNOWN (unmeasured) — cannot claim
        # asymmetry; state the rate without an indictment verdict.
        interp = ("fires on {:.0%} of random valid inputs on the PATCHED "
                  "build; the buggy-build rate is unmeasured, so whether "
                  "this is indiscriminate (check bug) or patch-introduced "
                  "(a catch) is undetermined — judge on the check's shown "
                  "contract.".format(p_rate))
    elif b_rate is not None and b_rate >= INTRINSIC_FIRE_RATIO:
        interp = ("fires on essentially every input on the buggy build "
                  "({:.0%}) — the firing is intrinsic to the check/setup "
                  "construction, not a detection of the defect.".format(b_rate))

    if interp is None:
        return None

    def _cnt(violated, checked):
        # Show raw counts, flag the multi-firing case rather than a >100%.
        if violated is not None and checked and violated > checked:
            return "{}/{} (multi-firing; rate capped at 100%)".format(
                violated, checked)
        return "{}/{}".format(violated, checked)

    parts = []
    if b_rate is not None:
        parts.append("buggy build " + _cnt(buggy_violated, buggy_checked)
                     + " = {:.0%}".format(b_rate))
    if p_rate is not None:
        parts.append("patched build " + _cnt(patched_violated, patched_checked)
                     + " = {:.0%}".format(p_rate))

    text = ("[fire-rate fact] " + "; ".join(parts) + " of random valid "
            "inputs. " + interp)
    if screen_outcome_reason:
        text += (" Screening demotion reason: "
                 + str(screen_outcome_reason).strip() + ".")
    return text


# ---------------------------------------------------------------------------
# Spec M (cycle-3b) — universal-screen "never held" fact. When a fired harness
# oracle's OWN check was screened on the buggy build and it was VIOLATED on
# EVERY input (violated == checked > 0), the asserted property never once held
# on a build that is correct almost everywhere — the strongest form of the
# fire-rate indictment, stated verbatim for the judge.
# ---------------------------------------------------------------------------

def never_held_fact(checked):
    """Build the "[universal-screen fact]" block for a check violated on all
    `checked` buggy inputs (its own declared domain). Pure."""
    return ("[universal-screen fact] this claim has NEVER been observed to "
            "hold on the buggy build (0/" + str(checked) + " inputs in its "
            "own declared domain) — a correctness claim the known-mostly-"
            "correct build never once satisfies is unverified speculation, "
            "not a contract.")


# ---------------------------------------------------------------------------
# Spec K (cycle-3) — one-door fact parity. The replay track carries
# screen-stats / fire-rate facts on a screened relation's firing; a
# harness-track firing of the SAME underlying check reaches the judge with none
# of them, and the judge convicts there (Math-73-c: the identical bogus check,
# ruled UNSOUND on the replay track, kept on the harness track). This pure
# matcher decides whether a harness firing is the same check as a screened
# relation so run.py can attach the same facts. It NEVER guesses: a match must
# be either an exact normalized-id equality or a single distinctive token that
# belongs to exactly one relation name — anything ambiguous returns None.
# ---------------------------------------------------------------------------

# Split a string into lowercase alphanumeric tokens (drop everything else).
_TOKEN_SPLIT_RE = re.compile(r'[^0-9A-Za-z]+')


def _norm_id(s):
    """Lowercase and strip every non-alphanumeric character."""
    return re.sub(r'[^0-9a-z]', '', str(s or '').lower())


def _token_set(s):
    """Lowercase tokens of `s`, split on runs of non-alphanumerics."""
    return {t for t in _TOKEN_SPLIT_RE.split(str(s or '').lower()) if t}


def match_oracle_to_relation(oracle_id, fired_msg, relation_names):
    """Match a fired harness oracle to one of the screened relations, or None.

    Two mechanical routes, tried in order:

    1. Normalized-id equality — the oracle id and a relation name are equal
       once lowercased with all non-alphanumerics stripped (so
       ``exact-endpoint-root`` == ``exactEndpointRoot``). First hit wins.

    2. Shared distinctive token — a token of >=6 characters, drawn from the
       oracle id OR the fired message (split on non-alphanumerics, lowercased),
       that appears in exactly ONE relation name. A relation name "contains" a
       token when the token is a substring of its normalized (lowercased,
       alphanumeric-only) form — this catches the common camelCase relation
       name a hyphenated oracle id shadows (``endpoint`` in
       ``endpointRootConsistency``). Only tokens that land in exactly one
       relation name count; a token shared by two or more names is ambiguous
       and contributes nothing. If, across all distinctive tokens, exactly one
       relation name is singled out, it is returned; zero or several -> None.

    Conservative by construction: None whenever the evidence does not point at
    one and only one relation.
    """
    names = [n for n in (relation_names or []) if n]
    if not names:
        return None

    # (1) normalized-id equality.
    oid_norm = _norm_id(oracle_id)
    if oid_norm:
        for n in names:
            if _norm_id(n) == oid_norm:
                return n

    # (2) shared distinctive token (>=6 chars) from the oracle id or fired msg.
    src_tokens = _token_set(oracle_id) | _token_set(fired_msg)
    src_tokens = {t for t in src_tokens if len(t) >= 6}
    if not src_tokens:
        return None

    name_norms = [(n, _norm_id(n)) for n in names]
    matched = set()
    for t in src_tokens:
        holders = [n for n, norm in name_norms if t in norm]
        if len(holders) == 1:
            matched.add(holders[0])
        # len >= 2: ambiguous token, contributes nothing.
    if len(matched) == 1:
        return next(iter(matched))
    return None


# ---------------------------------------------------------------------------
# Cycle-5B — recall-side dismissal lint (step-4b enforcement). Pure decision
# predicates + the re-ask/note text. run.py runs the judge, then applies these
# to VOID-and-re-ask a dismissal that (i) varies a parameter the check pins or
# (ii) is an uncited hypothetical under the drift-kill signature.
# ---------------------------------------------------------------------------

# Conservative synonym sets, keyed by pinned_parameters() category. Only
# categories with a reliable, low-collision vocabulary participate in
# enforcement; 'size' has none, so a size pin is stated in the note but never
# voids a verdict (its "synonyms" — length/capacity/size — are far too common
# in a legitimate WHY to key a void on).
_PIN_SYNONYMS = {
    'timezone': ('timezone', 'time zone', 'time-zone', 'dst', 'daylight',
                 'zoneoffset', 'calendar', 'offset transition',
                 'across the transition'),
    'locale': ('locale', 'language-specific', 'country'),
    'seed': ('random seed', 'rng seed', 'different seed', 'nondetermin',
             'non-determin', 'unseeded'),
    'size': (),
}


def pinned_environment_note(pinned):
    """Cycle-5B(i): the "[pinned-environment fact]" note listing what the
    check's own source fixes. `pinned` is a pinned_parameters() dict. Returns
    None when nothing is pinned. Pure."""
    if not pinned:
        return None
    parts = []
    for cat in sorted(pinned):
        snip = ", ".join(pinned[cat][:3])
        parts.append(cat + " (" + snip + ")")
    return ("[pinned-environment fact] this check's OWN source PINS: "
            + "; ".join(parts) + ". The harness holds these fixed, so a "
            "counterexample that varies one of them (a different timezone/DST "
            "transition, a different locale, a different RNG seed) is "
            "INADMISSIBLE — it cannot occur under this check. To answer "
            "UNSOUND, cite a contract the check contradicts or a demonstrable "
            "check bug that holds with the pinned parameters left fixed.")


def dismissal_invokes_pinned(why, pinned):
    """Cycle-5B(i) predicate: does a dismissal WHY rest on varying a parameter
    the check PINS? `pinned` is a pinned_parameters() dict (or any iterable of
    category names). Conservative keyword match against each pinned category's
    synonyms; categories without synonyms (e.g. 'size') never match. Pure."""
    if not why or not pinned:
        return False
    cats = pinned.keys() if isinstance(pinned, dict) else pinned
    low = str(why).lower()
    for cat in cats:
        for syn in _PIN_SYNONYMS.get(cat, ()):
            if syn and syn in low:
                return True
    return False


# Hedge markers = the "a correct implementation could..." shape. Citation
# markers = a shown contract or a demonstrable check bug. Erring toward NOT
# voiding (a false void could rescue a genuinely unsound check -> FP), the
# citation set is deliberately broad: only a clearly UNCITED hypothetical
# under the drift-kill signature is void.
_HEDGE_MARKERS = (
    'could', 'might', 'may ', 'a correct implementation',
    'a correct printer', 'a correct solver', 'legitimately', 'legal',
    'not guaranteed', 'is permitted', 'permitted', 'not forbid',
    "doesn't forbid", 'optional', 'not obliged',
)
_CITATION_MARKERS = (
    'document', 'javadoc', 'contract', '@throws', 'spec', 'specified',
    'trusted', 'defined as', 'delegat', 'identity', 'reserved', 'keyword',
    ' != ', '!=', ' == ', 'compares', 'impl computes', 'implementation '
    'computes', 'shown impl', 'shown body', 'shown code', 'the source shows',
    'observed', 'formula', 'range', 'per the api', 'per api', 'the api ',
    # Rounding / tolerance-floor citations (5D). A dismissal that names the
    # numeric floor it rests on ("only accurate to 1e-6", "fp round-off",
    # "bit-exact ==") is citing a demonstrable check bug — the check asserts
    # more precision than the API promises — not hypothesising about what a
    # correct implementation "could" do.
    'tolerance', 'accuracy', 'accurate to', 'rounding', 'round-off',
    'roundoff', 'ulp', 'floating-point', 'floating point', 'fp ',
    'epsilon', 'precision', 'underflow', 'bit-exact', 'bit exact',
)

# A quantified magnitude ("8.7e-7", "1E-8", "1e-6") is itself a
# rounding/tolerance citation: the dismissal is pointing at a measured floor.
_TOLERANCE_MAGNITUDE_RE = re.compile(r'\d(?:\.\d+)?\s*[eE]-\d+')


def verdict_needs_citation(evidence_profile, why):
    """Cycle-5B(ii) predicate: under the drift-kill signature {buggy silent +
    deterministic trigger firing + patched firing}, a UNSOUND verdict must
    CITE a shown contract or a demonstrable check bug — an uncited "a correct
    implementation could..." hypothetical is INADMISSIBLE. Returns True when
    the verdict is VOID (a hedge with no citation, under the signature).

    `evidence_profile` is a mapping/object exposing booleans `buggy_silent`,
    `deterministic_trigger`, `patched_firing` (missing => False). Pure."""
    if not why:
        return False

    def _b(k):
        if isinstance(evidence_profile, dict):
            return bool(evidence_profile.get(k))
        return bool(getattr(evidence_profile, k, False))

    signature = (_b('buggy_silent') and _b('deterministic_trigger')
                 and _b('patched_firing'))
    if not signature:
        return False
    low = str(why).lower()
    has_hedge = any(h in low for h in _HEDGE_MARKERS)
    has_citation = (any(c in low for c in _CITATION_MARKERS)
                    or _TOLERANCE_MAGNITUDE_RE.search(low) is not None)
    return has_hedge and not has_citation


# ---------------------------------------------------------------------------
# Cycle-5C — precision-side mirror: IDENTICAL-ON-BOTH / fires-on-buggy is a
# TERMINAL mechanical fact. Curated marker set; deliberately excludes the
# asymmetric fire-rate CATCH wording ("...on the PATCHED build but only X% on
# the buggy build"), which shares no marker.
# ---------------------------------------------------------------------------

_TERMINAL_IDENTICAL_MARKERS = (
    'identical on both',
    'with the same observed values',
    'fires-on-buggy',
    'buggy-scan fact',
)

# A note may SAY "on both builds" while explicitly DENYING the identical-value
# claim — the 5C-cycle notes have two such forms, and both are the opposite of
# terminal:
#   * the partial-fix note ("DIFFERENT observed values … this firing remains
#     evidence AGAINST the patch") — a CONVICTION, not a dismissal;
#   * the unknown note ("observed values were not compared, so no
#     identical-value claim is made") — no fact either way.
# The old bare 'on both builds' / 'same check fires on both' markers matched
# both, so the terminal gate dropped catches whose own evidence convicted the
# patch (fixture rows 32/33/89/94, iteration 1). Deny-first, then affirm.
_TERMINAL_IDENTICAL_VETO = (
    'different observed values',
    'no identical-value claim',
    'were not compared',
    'partial-fix pattern',
    'remains evidence against the patch',
)

# --- 5D: the MEASURED fires-on-both profile --------------------------------
# The textual markers above only catch the byte-comparison form of the fact
# ("identical on both builds"). The SAME fact can arrive as measured RATES in
# a "[fire-rate fact]" block: a check that condemns the KNOWN-BROKEN build on a
# large share of random valid inputs is reporting something PRE-EXISTING, not
# the patch's defect — terminal for exactly the same reason, with the same
# family-duty escape.
#
# Bars, both derived from the two shipped constants (no new calibration):
#   * buggy >= INTRINSIC_FIRE_RATIO (0.95) — the module already calls this
#     "intrinsic to the check/setup construction, not a detection of the
#     defect"; it stands alone, whether or not the patched side was measured.
#   * TERMINAL_BOTH_FIRE_RATIO — the buggy-side bar for the two-sided case,
#     midway between the indiscriminate cap (MAX_FIRE_RATIO) and the intrinsic
#     ceiling (INTRINSIC_FIRE_RATIO). "Genuinely high": the check must condemn
#     a clear MAJORITY of random valid inputs on the broken build, not merely
#     clear the 20% indiscriminate cap — and the patched side must ALSO be at
#     or above that cap, i.e. it really does fire on BOTH builds.
# The 5A asymmetric CATCH profile (buggy LOW / patched high) can reach neither
# bar by construction: its buggy rate is below MAX_FIRE_RATIO, which is below
# both TERMINAL_BOTH_FIRE_RATIO and INTRINSIC_FIRE_RATIO.
TERMINAL_BOTH_FIRE_RATIO = (MAX_FIRE_RATIO + INTRINSIC_FIRE_RATIO) / 2.0

_FIRE_RATE_TAG = '[fire-rate fact]'
_FR_BUGGY_RE = re.compile(r'buggy build\s+(\d+)\s*/\s*(\d+)', re.I)
_FR_PATCHED_RE = re.compile(r'patched build\s+(\d+)\s*/\s*(\d+)', re.I)
# A fire-rate block is one sentence-run; bound the window so a later, unrelated
# "patched build n/m" phrase elsewhere in the evidence cannot be absorbed.
_FIRE_RATE_WINDOW = 400


def parse_fire_rate_facts(text):
    """Parse every "[fire-rate fact]" block in `text` into (buggy_rate,
    patched_rate) pairs. Each rate is violated/checked clamped at 1.0 (a
    multi-case oracle can fire more than once per input — same clamp
    ``fire_rate_fact`` applies), or None when that side is absent/unmeasured.
    Returns a list; empty when no block parses. Pure."""
    out = []
    if not text:
        return out
    s = str(text)
    low = s.lower()
    start = low.find(_FIRE_RATE_TAG)
    while start != -1:
        block = s[start:start + _FIRE_RATE_WINDOW]

        def _rate(m):
            if not m:
                return None
            violated, checked = int(m.group(1)), int(m.group(2))
            if checked <= 0:
                return None
            return min(1.0, violated / checked)

        out.append((_rate(_FR_BUGGY_RE.search(block)),
                    _rate(_FR_PATCHED_RE.search(block))))
        start = low.find(_FIRE_RATE_TAG, start + len(_FIRE_RATE_TAG))
    return out


def fire_rate_is_terminal(buggy_rate, patched_rate):
    """Cycle-5D: is this MEASURED rate pair the terminal fires-on-both/
    pre-existing profile? Pure, rate-only — no bug, leg or oracle name is
    consulted anywhere.

      * buggy >= INTRINSIC_FIRE_RATIO                       -> terminal
      * buggy >= TERMINAL_BOTH_FIRE_RATIO and patched >= MAX_FIRE_RATIO
                                                            -> terminal
      * anything else (notably the 5A asymmetric CATCH profile, buggy LOW /
        patched high, and any unmeasured buggy side)        -> NOT terminal
    """
    if buggy_rate is None:
        return False
    if buggy_rate >= INTRINSIC_FIRE_RATIO:
        return True
    return (buggy_rate >= TERMINAL_BOTH_FIRE_RATIO
            and patched_rate is not None
            and patched_rate >= MAX_FIRE_RATIO)


def carries_terminal_fire_rate_fact(text):
    """Cycle-5D: does the evidence carry a [fire-rate fact] whose MEASURED
    rates match the terminal fires-on-both / pre-existing profile? Pure."""
    return any(fire_rate_is_terminal(b, p) for b, p in
               parse_fire_rate_facts(text))


def terminal_profile(text):
    """Cycle-5C/5D: which terminal profile (if any) the evidence carries.

    Returns ``'identical-on-both'`` (textual byte-comparison fact),
    ``'fires-on-both-rate'`` (measured high buggy-side fire rate) or None.
    The textual profile wins when both are present. Pure."""
    if not text:
        return None
    low = str(text).lower()
    # Deny first: a note that explicitly refuses the identical-value claim (or
    # asserts the opposite — the partial-fix conviction) is never terminal on
    # the textual path, whatever other phrasing it contains.
    denied = any(v in low for v in _TERMINAL_IDENTICAL_VETO)
    if not denied and any(m in low for m in _TERMINAL_IDENTICAL_MARKERS):
        return 'identical-on-both'
    if carries_terminal_fire_rate_fact(text):
        return 'fires-on-both-rate'
    return None


def carries_terminal_identical_fact(text):
    """Cycle-5C: does the evidence carry a mechanical terminal fact — the
    textual IDENTICAL-ON-BOTH / fires-on-buggy marker set, or (5D) a
    [fire-rate fact] measuring the same thing as rates? Pure."""
    return terminal_profile(text) is not None


# ---------------------------------------------------------------------------
# Cycle-5B — re-ask plumbing (fail-open detection + injected statements).
# ---------------------------------------------------------------------------

def reask_verdict_usable(why):
    """Cycle-5B: did a re-ask produce a USABLE verdict (not a fail-open
    sentinel)? RelationVerifier.verify fails OPEN to KEEP on an LLM error /
    unparseable output; this returns False on those sentinels so the caller
    keeps the ORIGINAL verdict rather than a manufactured flip. Pure."""
    if not why:
        return False
    low = str(why).lower()
    for bad in ('verifier error', 'no verdict parsed', 'keeping finding',
                'unavailable'):
        if bad in low:
            return False
    return True


def pinned_reask_statement(pinned):
    """Cycle-5B(i): the explicit pin statement injected on a pin-void re-ask."""
    cats = sorted(pinned.keys() if isinstance(pinned, dict) else set(pinned))
    names = ", ".join(cats) or "environment parameters"
    return (
        "[PIN-VOID RE-ASK] Your previous dismissal rested on VARYING a "
        "parameter this check's own source PINS (" + names + "). The harness "
        "holds it fixed (e.g. it pins UTC / a fixed Locale / a fixed RNG "
        "seed), so a counterexample that changes it is INADMISSIBLE — it "
        "cannot occur under this check. Re-judge WITHOUT relying on variation "
        "of any pinned parameter: to answer UNSOUND you must cite a contract "
        "the check contradicts or a demonstrable check bug that holds with "
        "the pinned parameters left fixed.")


def citation_reask_statement():
    """Cycle-5B(ii): the citation demand injected on a citation-void re-ask."""
    return (
        "[CITATION-VOID RE-ASK] This firing has the strong-evidence profile: "
        "silent on the buggy build, a deterministic replay on the failing "
        "test's own literals, and firing on the patched build. Under that "
        "profile an uncited 'a correct implementation could...' hypothetical "
        "is INADMISSIBLE. To answer UNSOUND you must cite a SHOWN contract "
        "the check contradicts, or a demonstrable bug in the check itself; "
        "with neither, the verdict is SOUND.")
