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
    def _rate(violated, checked):
        if not checked or checked <= 0 or violated is None:
            return None
        return violated / checked

    p_rate = _rate(patched_violated, patched_checked)
    b_rate = _rate(buggy_violated, buggy_checked)

    interp = None
    if p_rate is not None and p_rate >= MAX_FIRE_RATIO:
        interp = ("a contract violated by {:.0%} of random valid inputs on "
                  "the PATCHED build indicts the check, not the patch — real "
                  "defect discriminators fire rarely and asymmetrically; keep "
                  "only with a shown contract that makes every one of those "
                  "inputs a genuine violation.".format(p_rate))
    elif b_rate is not None and b_rate >= INTRINSIC_FIRE_RATIO:
        interp = ("fires on essentially every input on the buggy build "
                  "({:.0%}) — the firing is intrinsic to the check/setup "
                  "construction, not a detection of the defect.".format(b_rate))

    if interp is None:
        return None

    parts = []
    if b_rate is not None:
        parts.append("buggy build {}/{} = {:.0%}".format(
            buggy_violated, buggy_checked, b_rate))
    if p_rate is not None:
        parts.append("patched build {}/{} = {:.0%}".format(
            patched_violated, patched_checked, p_rate))

    text = ("[fire-rate fact] " + "; ".join(parts) + " of random valid "
            "inputs. " + interp)
    if screen_outcome_reason:
        text += (" Screening demotion reason: "
                 + str(screen_outcome_reason).strip() + ".")
    return text
