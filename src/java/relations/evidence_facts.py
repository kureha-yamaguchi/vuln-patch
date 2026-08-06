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
#   SILENT_FIRE_RATIO — the bar for calling a check SILENT on the buggy build.
#     The catch-signal reading ("silent on the known-broken code, loud on the
#     patch") is only true near zero; 'under the 20% indiscriminate cap' is NOT
#     silence. Set at 2%: a handful of hits in 20k inputs is noise, 4k is not.
SILENT_FIRE_RATIO = 0.02

# ---------------------------------------------------------------------------
# Cycle-6 machine tags. The full tag registry (and the terminal/non-terminal
# combination rule) lives further down, next to ``terminal_profile``; these
# four are declared HERE because the notes that stamp them
# (``semantic_buggy_replay_note``, ``muted_replay_note``, ``fire_rate_fact``)
# are defined above that registry.
#
# The fire-rate note used to be readable only as PROSE, so every consumer that
# wanted "which branch did fire_rate_fact take?" had to keyword-match its
# interpretation sentence. ``fire_rate_fact`` already KNOWS its branch, so it
# now stamps it:
#   [fact:rate-catch-signal]   buggy <= SILENT_FIRE_RATIO and patched high —
#                              silent on the known-broken build, loud on the
#                              patch: the discrimination signal, a CATCH.
#   [fact:rate-indiscriminate] both sides high, or buggy >= INTRINSIC_FIRE_RATIO
#                              — the check condemns the known-broken build
#                              broadly, so it reports something PRE-EXISTING.
#   [fact:rate-ambiguous]      the low-but-not-silent gap, and the
#                              patched-high / buggy-UNMEASURED case: the rates
#                              decide nothing.
RATE_CATCH_FACT_TAG = '[fact:rate-catch-signal]'
RATE_INDISCRIMINATE_FACT_TAG = '[fact:rate-indiscriminate]'
RATE_AMBIGUOUS_FACT_TAG = '[fact:rate-ambiguous]'
# Stamped by the two replay notes when a replay MECHANICALLY CONFIRMED that
# this same check fires on the buggy build too (the direct same-check replay,
# or the muted re-replay once the shadowing check is silenced). It says only
# "fires on both" — never what the observed values were. The value question is
# answered by the tag that rides ALONGSIDE it
# ([fact:identical-on-both] / [fact:fires-both-different-values] /
# [fact:not-compared]), which is what ``confirmed_fires_on_both_verdict``
# reads. Confirmation without a value verdict is NOT a drop.
CONFIRMED_BOTH_FACT_TAG = '[fact:fires-on-both-confirmed]'

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

_DIVERTED_NOTE = (
    " on the buggy build execution at this exact input was DIVERTED before "
    "this check was evaluated: something threw and the harness's own "
    "`catch (...) { return; }` swallowed it, so the run returned early and "
    "NEVER REACHED this check. The replay therefore says NOTHING about "
    "whether the check would fire on the buggy build — no attribution in "
    "either direction. In particular the absence of a firing here is NOT "
    "evidence the patch caused anything. Judge on soundness alone, "
    "sceptically.")

_DIVERSION_UNKNOWN_NOTE = (
    " the buggy build did not fire this check on this exact input, but "
    "whether execution actually REACHED the check could not be determined "
    "(the harness swallows exceptions in its own `catch (...) { return; }` "
    "and the diversion probe was unavailable) — no attribution fact; judge "
    "on soundness alone, sceptically. Do NOT read the quiet run as evidence "
    "against the patch.")


def semantic_buggy_replay_note(fired_ids, breplay_status, breplay_ids,
                               bt_all, bt_defect, esc_type, idline="",
                               value_verdict="unknown",
                               buggy_msg_excerpt=None,
                               patched_msg_excerpt=None,
                               diverted=None):
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

    `diverted` (cycle-6) is the mechanical answer to "did the buggy replay
    actually REACH this check?": True = a swallow-return catch fired and the
    run returned early, False = it did not, None = unknown. It gates the
    quiet-run branch ONLY, and it gates it hard: the "buggy build ran this
    input clean WITHOUT firing this check, so the patch INTRODUCED the
    violation" existence-proof claim requires diverted is False. With True the
    note states the diversion and attributes nothing; with the default None it
    falls back to the conservative unavailable wording. An unthreaded caller
    therefore cannot manufacture the false fact that convicted the night20b
    Chart-26 correct patch (execution threw inside `axis.draw`, the harness's
    `catch (Exception e) { return; }` swallowed it, and the never-evaluated
    check was reported as "ran clean").

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
                return ("[buggy-replay fact] [fact:fires-on-both-confirmed] "
                        "[fact:fires-both-different-values]"
                        " the SAME check fires on BOTH "
                        "builds but with DIFFERENT observed values (buggy: "
                        + _excerpt(buggy_msg_excerpt) + " vs patched: "
                        + _excerpt(patched_msg_excerpt) + ") — the patch "
                        "changed behaviour at this input without restoring "
                        "the expected value: the partial-fix pattern; this "
                        "firing remains evidence against the patch."
                        + (idline or ""))
            if value_verdict == "identical":
                return ("[buggy-replay fact] [fact:fires-on-both-confirmed] "
                        "[fact:identical-on-both] the "
                        "exact firing input fires the "
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
            return ("[buggy-replay fact] [fact:fires-on-both-confirmed] "
                    "[fact:not-compared] the exact firing "
                    "input fires the "
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
        # Cycle-6: the quiet run is an existence proof ONLY if execution
        # actually reached the check. A swallowed exception that returned
        # early looks identical from outside and reverses the meaning.
        if diverted is True:
            return "[buggy-replay fact]" + _DIVERTED_NOTE
        if diverted is None:
            return "[buggy-replay fact]" + _DIVERSION_UNKNOWN_NOTE
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
        # Cycle-6: same gate as the fired-check branch above — "the buggy
        # build handled it" is only true if the buggy build ran the code at
        # all, and a swallowed exception that returned early did not.
        if diverted is True:
            return ("[buggy-replay fact] on the buggy build execution at this "
                    "exact input was DIVERTED before " + esc_type + " could "
                    "arise: something threw and the harness's own "
                    "`catch (...) { return; }` swallowed it, so the run "
                    "returned early and NEVER REACHED this site. The replay "
                    "says NOTHING about whether the buggy build raises this "
                    "exception — no attribution in either direction. Judge on "
                    "soundness alone, sceptically.")
        if diverted is None:
            return ("[buggy-replay fact] the buggy build did not raise "
                    + esc_type + " on this exact input, but whether execution "
                    "actually REACHED that site could not be determined (the "
                    "harness swallows exceptions in its own "
                    "`catch (...) { return; }` and the diversion probe was "
                    "unavailable) — no attribution fact; judge on soundness "
                    "alone, sceptically. Do NOT read the quiet run as evidence "
                    "against the patch.")
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


# ---------------------------------------------------------------------------
# NOT SHIPPED — non-numeric token comparison (item 2a fix (ii)). Kept as a
# recorded negative result so it is not rebuilt from the same wrong argument.
#
# WHAT THE SUPPORTING POPULATION ACTUALLY IS (verified against the fixture, not
# narrated). The 10 rows that pin only non-numeric expected values are all the
# SAME leg: patch1-Lang-41-Arja-plausible_o — one FAKE patch, label
# `overfitting`, gold=SOUND on all ten. So the population is ten firings of one
# bug, i.e. n=1 leg, and every one of them is a finding that should be KEPT.
# There is no precision opportunity hiding in the subset; the subset is a single
# overfit patch.
#
# Two earlier descriptions of these rows contradicted each other and one was
# false. "All 10 are on correct patches, so fixing this can only help precision"
# is WRONG — it came from translating gold=SOUND as "good fix". gold describes
# the SOUNDNESS OF THE CHECK, not the correctness of the patch, and the two run
# OPPOSITE: a sound check on a fake patch is a legitimate catch. Per
# score_replay.py, "over-kill (gold=SOUND dropped)" — gold=SOUND means KEEP.
#
# MEASURED TWICE, because fix (i) changed this fix's population.
#
# Against the pre-fix-(i) baseline, in isolation:
#   fix (i) project assertion helpers : dismissal fires 1x — correct 1, wrong 0
#   fix (ii) token comparison         : dismissal fires 1x — correct 0, wrong 1
#
# But fix (i) shipped, and it enlarged this fix's population from 10 rows on ONE
# fake patch to 27 rows across THREE legs (Lang-60 fake 15, Lang-41 fake 10,
# Lang-60 correct 2) — 21 keep-finding, 6 correct-patch rows. So (ii) had to be
# re-measured as a single change on top of shipped HEAD:
#
#   dismissal instruction : correct 1 -> 2, wrong 0 -> 1   (adds one of each: a wash)
#   37 rows move unknown -> differs ("do not exonerate on test-passage"):
#       21 keep-finding    GOOD, protects a legitimate finding
#       16 otherwise       BAD, makes a correct dismissal harder
#
# STILL NOT SHIPPED, but for a different and better reason than first recorded.
# It is not harmful; it is a wash on the decisive instruction with a marginal,
# RECALL-leaning side effect. This batch is precision-motivated, so shipping it
# would confound the paired measurement — a precision change could not be
# attributed. And at 21/16 on a 228-row corpus the effect is well inside the
# measured 27% run-to-run variance.
#
# Park condition: revisit when there is a recall-motivated batch to attribute it
# in, or when the corpus is large enough for 21/16 to mean something. Re-run the
# two measurements above, not just one.
# ---------------------------------------------------------------------------


# ---------------------------------------------------------------------------
# 8.4 — RAW-vs-PINNED comparison. The consumer the raw recording was built for.
#
# The setup-divergence rung asks one question: does the fired value equal a
# value the failing test itself pins? For ~17% of accepted harnesses the check
# normalizes before comparing (the prompt requires it for code/formatted text),
# so the only value the alarm used to report was a normalized derivative. It can
# never equal the test's raw literal, so the rung was structurally dead for
# exactly those checks. 8.4's prompt change + gate 0c2 make the pre-normalization
# value available; this is what reads it.
#
# THIS IS NOT A REVIVAL OF FIX (ii). Fix (ii) compared arbitrary tokens on any
# evidence and measured a wash. This compares ONE designated field against the
# literals the test pins, exactly. Narrower, better-grounded, separately measured.
# ---------------------------------------------------------------------------

# The named keys 8.4's message format defines. Parsing stops at any of them, so
# a raw value containing spaces (`x- -0.0`) is captured whole.
_RAW_KEYS = ('expectedNormalized', 'actualNormalized', 'expectedRaw', 'actualRaw')
_NEXT_KEY_RE = re.compile(r'\s(?:' + '|'.join(_RAW_KEYS) + r')=')
# Any ` name=` token. Real alarms append their own metadata keys after the raw
# pair (`actualRaw=x--0.0 parseErrorCount=0` was observed in the compliance
# smoke), so stopping only at the four 8.4 keys over-captures the metadata into
# the value. Stopping at ANY key risks the opposite, because a raw value that is
# source code may legitimately contain ` x=1` — so where the stop key is not one
# of the four known ones, the capture is marked AMBIGUOUS rather than trusted.
_ANY_KEY_RE = re.compile(r'\s([A-Za-z_]\w*)=')

_JAVA_ESCAPES = {'n': '\n', 't': '\t', 'r': '\r', 'b': '\b', 'f': '\f',
                 '"': '"', "'": "'", '\\': '\\', '0': '\0'}


def _decode_java_literal(text):
    """A Java source literal's escapes decoded into the value it denotes.

    This is the ONE transformation the comparison applies, and it is applied to
    the PINNED side only. It is decoding, not normalization: `\\n` in test source
    and a newline in a runtime string are the same character, written two ways.
    Nothing is collapsed, trimmed, or discarded — which is the whole point, since
    the checks this serves are the ones whose defects ARE whitespace defects.
    Unknown escapes are left verbatim rather than guessed at."""
    if not text or '\\' not in text:
        return text or ''
    out, i = [], 0
    while i < len(text):
        c = text[i]
        if c != '\\' or i + 1 >= len(text):
            out.append(c)
            i += 1
            continue
        nxt = text[i + 1]
        if nxt == 'u' and re.match(r'[0-9a-fA-F]{4}', text[i + 2:i + 6] or ''):
            out.append(chr(int(text[i + 2:i + 6], 16)))
            i += 6
        elif nxt in _JAVA_ESCAPES:
            out.append(_JAVA_ESCAPES[nxt])
            i += 2
        else:
            out.append(c)          # not an escape we recognise: leave it alone
            i += 1
    return ''.join(out)


def _captured_raw_observed(fired_msg):
    """The `actualRaw=` value from a fired message, or None.

    OBSERVED ONLY, deliberately. `expectedRaw=` is the check's own reference
    value, and for a lifted test that reference is frequently DERIVED from the
    very literal we are comparing against — so comparing it would match by
    construction and dismiss every lifted firing. That is the same trap
    `_observed_numbers` was narrowed to avoid (a shared expected= reference
    making two different observations compare identical), reached from the
    string side instead of the numeric one.

    Returns (value, ambiguous). `ambiguous` is True when the value's end could
    not be established with confidence — see `_ANY_KEY_RE`.
    """
    if not fired_msg:
        return None, False
    at = fired_msg.find('actualRaw=')
    if at < 0:
        return None, False
    start = at + len('actualRaw=')
    nxt = _ANY_KEY_RE.search(fired_msg, start)
    if nxt is None:
        # Runs to the end of the string — clean ONLY if the string is the whole
        # message. A trailing ellipsis means the message was truncated, so the
        # value we are holding is a prefix of the real one and any comparison
        # against it is an accident. The batch-8 smoke found the comparison
        # being fed 200-char-capped headlines; the caller now passes the
        # uncapped form, and this makes the failure LOUD rather than silent if
        # a capped string ever reaches here again.
        truncated = fired_msg.rstrip().endswith(('…', '...'))
        return fired_msg[start:], truncated
    value = fired_msg[start:nxt.start()]
    known = nxt.group(1) in _RAW_KEYS
    return value, not known


def raw_value_vs_pinned(fired_msg, trusted_values):
    """Compare a check's PRE-NORMALIZATION observed value against the literals
    the failing test pins. Returns "matches" / "differs" / "unknown".

    Exact equality, no tolerance and no trimming. The rounding floor that the
    numeric comparison rightly applies has no analogue here: these are strings
    whose differences ARE the finding.

    THE ASYMMETRY THIS IS BUILT AROUND. "matches" is the only verdict that
    licenses a dismissal, so every uncertainty in this function must resolve
    away from it:

      * absent key            -> "unknown"  (so the whole comparison is inert on
                                 any record predating 8.4 — 0 of 228 archived
                                 rows carry the key, and none can)
      * mis-captured value    -> cannot be "matches", because "matches" requires
                                 exact equality and a mis-capture is not equal
                                 to anything. Over- and under-capture are both
                                 fail-safe in the dismissal direction.
      * multi-line raw value  -> "matches" if it exactly equals a pinned literal
                                 (a real hit is a real hit), otherwise
                                 "unknown", NOT "differs" — a value that may
                                 have swallowed a following line does not
                                 support the positive claim "differs from every
                                 value the test pins".

    The cost of that asymmetry is missed dismissals, which is the direction the
    rung should fail in: the population it acts on is findings that would
    otherwise be kept.
    """
    raw, ambiguous = _captured_raw_observed(fired_msg)
    if raw is None or not trusted_values:
        return "unknown"
    # BOTH sides are decoded. The alarm is read line by line, so a raw value
    # holding a real newline would cut the message there and discard every key
    # after it -- the batch-8 smoke's actual defect. The prompt therefore
    # requires raw values to be emitted with `\n` / `\t` escaped, which is also
    # the form the test's source literal is written in. Decoding both sides
    # compares the values they denote rather than the spellings they arrived
    # in, and is the identity on values that contain no escapes at all.
    # ORDER MATTERS. The doubt test runs on the CAPTURED text, before
    # decoding: a REAL newline there means the message spanned lines and the
    # capture may be a fragment, while an ESCAPED newline is exactly what the
    # prompt asks for and is not doubtful at all. Decoding first would conflate
    # the two and report every correctly-escaped multi-line value as unknown.
    capture_doubtful = ambiguous or '\n' in raw or '\r' in raw
    decoded = _decode_java_literal(raw)
    pinned = [_decode_java_literal(str(v)) for v in trusted_values]
    if any(decoded == p for p in pinned):
        return "matches"
    if capture_doubtful:
        return "unknown"           # capture not trustworthy: claim nothing
    return "differs"


def fired_value_vs_trusted(fired_msg, trusted_values):
    """Mechanically compare the fired message's observed value against the
    values the trigger test itself pins.

    Returns:
      * "matches"  — some fired number equals some trusted number within the
        rounding floor.
      * "differs"  — numeric values are present on BOTH sides but none match.
      * "unknown"  — no numeric values are extractable on one or both sides.

    Numeric only, deliberately — see the NOT SHIPPED note above.

    8.4: when the alarm carries a pre-normalization `actualRaw=` value, the RAW
    comparison decides and the numeric scrape is not consulted. Precedence, not
    preference: on a normalizing check the numbers in the message are incidental
    to formatted text (line numbers, offsets, column markers) and can collide
    with numbers inside the pinned literal by coincidence — a coincidence that
    would license a dismissal. The raw comparison asks the rung's actual
    question against the actual values, exactly. When there is no raw value the
    behaviour is byte-identical to before, which is why this is inert on every
    record written before 8.4 shipped.
    """
    raw_verdict = raw_value_vs_pinned(fired_msg, trusted_values)
    if raw_verdict != "unknown":
        return raw_verdict
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


# Any `<name>=<value>` pair, value kept as TEXT. Deliberately wider than
# `_KV_PAIR_RE`: that one is numeric-only because it feeds a numeric
# comparison, and a value it cannot parse is correctly invisible to it. This
# one feeds the RECORD, and recording is not judging -- dropping the formatted
# text and string values would discard exactly what 8.2's authority screen and
# 8.20's scope fact were built to consume. Stops at the next ` key=` token or
# end of message, the same rule `_captured_raw_observed` uses.
_KV_TEXT_RE = re.compile(r'([A-Za-z_]\w*)\s*=\s*(.*?)(?=\s+[A-Za-z_]\w*\s*=|$)')


def observed_values(msg):
    """`{key: [value strings]}` for every `key=value` pair in a fired message.

    8.3. The buggy-side replay already computed these values and then dropped
    them: 0 of 1,452 recorded buggy-side steps carried an observed VALUE, only
    fired/counts. That absence is what made 8.2 untestable and what forces 6C's
    values-not-compared abstentions.

    Pure, total, and never raises -- a message it cannot parse yields `{}`,
    which reads as "no values recorded" and never as "no values existed".
    """
    out = {}
    for k, v in _KV_TEXT_RE.findall(str(msg or '')):
        v = v.strip()
        if v:
            out.setdefault(k, []).append(v)
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

# ---------------------------------------------------------------------------
# Cycle-7 (Math-65) — the DISPUTED COMPUTATION fact.
#
# Failure class this addresses: evidence that is delivered but LOST. Not missing,
# not misread — present exactly once, in the middle of a ~60k-character prompt,
# where finding it is chance.
#
# Math-65's whole dispute is one formula. The code computes
# `residual*residual / weight`; the harness's check recomputes it as
# `weight * residual*residual`. Every reviewer that quoted the code's actual line
# dismissed the alarm correctly (3 of 3). Every reviewer that accused cited
# NOTHING and appealed to a remembered javadoc, asserting the inverse (4 of 4).
#
# The line was already in the prompt — citations are checked for literal
# presence, so the dismissals quoting it prove that. It sat at character 27,051
# of 59,830, inside the partial class skeleton.
#
# So the fix is PLACEMENT, not delivery, and it DUPLICATES rather than moves:
# the skeleton is left untouched (other things depend on it) and the method's own
# body is restated as a fact beside the firing. A placement audit over all 230
# archived judge prompts confirmed the evidence block is already adjacent to the
# firing (median 15% of the prompt), so this is a targeted addition and NOT a
# repair of the assembly order.
#
# Generality: the trigger is "the fired message names a method, and that method's
# real body is present in the shown source". No bug-specific content, no project
# names, no formula knowledge.
# ---------------------------------------------------------------------------

#: A skeleton body that was elided rather than shown. The code-context builder
#: replaces untouched bodies with these, so they carry no computation to quote.
_ELIDED_BODY_RE = re.compile(r'^[\s{}…\.]*$')

#: Cap on a quoted body. A runaway method would bloat the prompt and push the
#: rest of the evidence down — the exact failure this fact exists to fix.
_MAX_QUOTED_BODY = 900

#: Method names too generic to be worth quoting even when they match.
_UNINTERESTING_METHODS = frozenset({
    'toString', 'hashCode', 'equals', 'clone', 'compareTo', 'iterator',
    'get', 'set', 'size', 'length', 'valueOf', 'main', 'run',
})

_METHOD_CALL_RE = re.compile(r'\b([a-z][A-Za-z0-9_]{2,})\s*\(')

#: Identifier-ish words in a firing, including the pieces of a snake_case
#: relation name. Needed because a firing often names the disputed quantity
#: WITHOUT calling it: "relation chiSquare_matches_weighted_residual_sum
#: violated" references getChiSquare() but contains no `(`.
_WORD_RE = re.compile(r'[A-Za-z][A-Za-z0-9]{3,}')

#: Accessor prefixes to strip when matching a firing's word against a method
#: name, so `chiSquare` finds `getChiSquare`.
_ACCESSOR_PREFIXES = ('get', 'is', 'has', 'compute', 'calculate')


def _defined_methods(source):
    """Names of methods that have a REAL (non-elided) body in ``source``."""
    out = []
    for m in re.finditer(r'\b([a-zA-Z_][A-Za-z0-9_]*)\s*\([^)]*\)\s*\{',
                         str(source or '')):
        name = m.group(1)
        if name not in out and name not in _UNINTERESTING_METHODS:
            out.append(name)
    return out


def _methods_named_by(fired_msg, code_context):
    """Methods the firing refers to, by call syntax OR by name.

    Call syntax is exact. Name matching is deliberately narrow: a word in the
    firing must equal the method name, or equal it with an accessor prefix
    stripped (`chiSquare` -> `getChiSquare`), case-insensitively. A substring
    match would fire on almost everything, so it is not used."""
    names, seen = [], set()
    for name in _METHOD_CALL_RE.findall(str(fired_msg or '')):
        if name not in _UNINTERESTING_METHODS and name not in seen:
            seen.add(name)
            names.append(name)
    words = {w.lower() for w in _WORD_RE.findall(str(fired_msg or ''))}
    if not words:
        return names
    for method in _defined_methods(code_context):
        if method in seen:
            continue
        low = method.lower()
        candidates = {low}
        for pre in _ACCESSOR_PREFIXES:
            if low.startswith(pre) and len(low) > len(pre) + 2:
                candidates.add(low[len(pre):])
        if candidates & words:
            seen.add(method)
            names.append(method)
    return names


def _method_body(source, name):
    """The verbatim body of ``name`` in ``source``, or None.

    None when the method is absent, when its body was elided to ``{ … }``, or
    when it is longer than the quoting cap. Brace-matched, so a nested block does
    not truncate the body early. Pure."""
    if not source or not name:
        return None
    for m in re.finditer(r'\b' + re.escape(name) + r'\s*\([^)]*\)\s*\{',
                         str(source)):
        start = m.end() - 1           # at the opening brace
        depth, i, n = 0, start, len(source)
        while i < n:
            if source[i] == '{':
                depth += 1
            elif source[i] == '}':
                depth -= 1
                if depth == 0:
                    break
            i += 1
        else:
            continue                  # unbalanced — do not guess
        body = source[start:i + 1]
        if _ELIDED_BODY_RE.match(body):
            continue                  # `{ … }` — nothing shown to quote
        if len(body) > _MAX_QUOTED_BODY:
            continue
        return (source[m.start():start] + body).strip()
    return None


def disputed_computation_fact(fired_msg, code_context):
    """Restate, verbatim, the shown source of any method the firing NAMES.

    Returns the fact text, or None when the firing names no method whose real
    body is shown. Quotes at most three methods, longest-named first, so the
    block stays small.

    This adds no interpretation whatsoever: it locates code the reviewer was
    already given and repeats it next to the firing. Whether the check is sound
    is still entirely the reviewer's call. Pure — no I/O, no LLM."""
    if not fired_msg or not code_context:
        return None
    names = _methods_named_by(fired_msg, code_context)
    quoted = []
    for name in sorted(names, key=len, reverse=True):
        body = _method_body(code_context, name)
        if body:
            quoted.append((name, body))
        if len(quoted) == 3:
            break
    if not quoted:
        return None
    parts = ["\n[disputed-computation fact] the firing names "
             + ("this method" if len(quoted) == 1 else "these methods")
             + ". Its source is copied verbatim below from the class skeleton "
               "already shown to you — repeated here, next to the firing, "
               "because in the full skeleton it appears once and is easy to "
               "miss. Nothing here is new evidence and nothing here is an "
               "instruction.\n"
               "This cuts BOTH ways and is not, by itself, grounds either way. "
               "If the check recomputes a quantity this code also computes, "
               "read what the code actually does rather than what the "
               "documentation appears to promise — the two can disagree, and "
               "the code is what runs. But a check that computes something "
               "DIFFERENT from this method is not thereby unsound: it may be "
               "asserting a property that generalises beyond this method, which "
               "is a legitimate catch. Decide on the shown source and the "
               "contract, exactly as for any other firing."]
    for name, body in quoted:
        parts.append("\n--- " + name + " (as shown in the patched class) ---\n"
                     + body)
    return "\n".join(parts)


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
                      buggy_msg_excerpt=None, patched_msg_excerpt=None,
                      diverted=None):
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
        introduced it — but ONLY when `diverted` is False. `diverted` (cycle-6,
        True/False/None) says whether one of the harness's own
        `catch (...) { return; }` swallows fired on this input: True means
        execution returned early and never reached the check, so the note
        states the diversion and attributes nothing; the default None yields
        the conservative unavailable wording. An unthreaded caller can never
        emit the introduced claim.
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
            return ("[muted-replay fact] [fact:fires-on-both-confirmed] "
                    "[fact:fires-both-different-values] "
                    "with the shadowing check(s) "
                    + ids_txt + " silenced, the SAME check fires on BOTH "
                    "builds but with DIFFERENT observed values (buggy: "
                    + _excerpt(buggy_msg_excerpt) + " vs patched: "
                    + _excerpt(patched_msg_excerpt) + ") — the patch changed "
                    "behaviour at this input without restoring the expected "
                    "value: the partial-fix pattern; this firing remains "
                    "evidence against the patch.")
        if value_verdict == "identical":
            return ("[muted-replay fact] [fact:fires-on-both-confirmed] "
                    "[fact:identical-on-both] with the "
                    "shadowing check(s) "
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
        return ("[muted-replay fact] [fact:fires-on-both-confirmed] "
                "[fact:not-compared] with the shadowing "
                "check(s) " + ids_txt
                + " silenced, THIS check fires on the BUGGY build at this "
                "exact input — the same check fires on both builds (observed "
                "values were not compared, so no identical-value claim is "
                "made); judge the check's soundness on the shown contract.")

    if status == "clean":
        # Cycle-6: identical gate to semantic_buggy_replay_note's quiet-run
        # branch. This is the exact site that convicted the night20b Chart-26
        # correct patch: the muted replay threw inside axis.draw, the
        # harness's own `catch (Exception e) { return; }` swallowed it and
        # returned, and "no firing" was reported as "ran clean".
        if diverted is True:
            return ("[muted-replay fact] with the shadowing check(s) "
                    + ids_txt + " silenced," + _DIVERTED_NOTE)
        if diverted is None:
            return ("[muted-replay fact] with the shadowing check(s) "
                    + ids_txt + " silenced," + _DIVERSION_UNKNOWN_NOTE)
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

    Cycle-6 PART 1: the note carries a MACHINE TAG naming the branch this
    function took (``RATE_CATCH_FACT_TAG`` / ``RATE_INDISCRIMINATE_FACT_TAG`` /
    ``RATE_AMBIGUOUS_FACT_TAG``) alongside the prose. Consumers that need the
    profile read the tag (``rate_profile``); the prose is then free to be
    prose, and the last site that reconstructed a profile by keyword-matching
    this note's wording is retired.
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
    tag = None
    both_high = (p_rate is not None and p_rate >= MAX_FIRE_RATIO
                 and b_rate is not None and b_rate >= MAX_FIRE_RATIO)
    # The catch-signal claim asserts the check is SILENT on the known-broken
    # build. That requires a genuinely near-zero buggy rate — NOT merely "under
    # the indiscriminate cap". The first version used `b_rate < MAX_FIRE_RATIO`
    # (0.20) and so told the judge a check firing on 19.8% of buggy inputs was
    # "silent (or near-silent)", converting a false premise into a PRO-KEEP
    # instruction — it contributed to a chronic false accusation (Math-65,
    # night20b, classified 2026-07-28). Between SILENT and the cap the evidence
    # is genuinely ambiguous, so say so instead of asserting either reading.
    asymmetric = (p_rate is not None and p_rate >= MAX_FIRE_RATIO
                  and b_rate is not None and b_rate <= SILENT_FIRE_RATIO)
    ambiguous_gap = (p_rate is not None and p_rate >= MAX_FIRE_RATIO
                     and b_rate is not None
                     and SILENT_FIRE_RATIO < b_rate < MAX_FIRE_RATIO)
    if both_high:
        tag = RATE_INDISCRIMINATE_FACT_TAG
        interp = ("fires on a large share of random valid inputs on BOTH "
                  "builds (buggy {:.0%}, patched {:.0%}) — indiscriminate; "
                  "the firing is intrinsic to the check/setup construction, "
                  "not a detection of the defect. Keep only with a shown "
                  "contract that makes every one of those inputs a genuine "
                  "violation.".format(b_rate, p_rate))
    elif asymmetric:
        tag = RATE_CATCH_FACT_TAG
        interp = ("fires on {:.0%} of random valid inputs on the PATCHED "
                  "build but only {:.0%} on the buggy build — the check is "
                  "silent (or near-silent) on the known-broken code and "
                  "loud on the patch, i.e. the PATCH introduced this "
                  "divergence. This is a strong discrimination signal, NOT "
                  "grounds to indict the check; dismiss only with a shown "
                  "reason the patched-only firings are legitimate.".format(
                      p_rate, b_rate))
    elif ambiguous_gap:
        tag = RATE_AMBIGUOUS_FACT_TAG
        interp = ("fires on {:.0%} of random valid inputs on the PATCHED "
                  "build and {:.0%} on the buggy build — the buggy-side rate "
                  "is low but NOT silent, so this is neither a clean "
                  "patch-introduced discrimination signal nor an "
                  "indiscriminate check. The rates alone decide nothing "
                  "here; judge on the check's shown contract.".format(
                      p_rate, b_rate))
    elif p_rate is not None and p_rate >= MAX_FIRE_RATIO:
        # patched-high, buggy rate UNKNOWN (unmeasured) — cannot claim
        # asymmetry; state the rate without an indictment verdict.
        tag = RATE_AMBIGUOUS_FACT_TAG
        interp = ("fires on {:.0%} of random valid inputs on the PATCHED "
                  "build; the buggy-build rate is unmeasured, so whether "
                  "this is indiscriminate (check bug) or patch-introduced "
                  "(a catch) is undetermined — judge on the check's shown "
                  "contract.".format(p_rate))
    elif b_rate is not None and b_rate >= INTRINSIC_FIRE_RATIO:
        tag = RATE_INDISCRIMINATE_FACT_TAG
        interp = ("fires on essentially every input on the buggy build "
                  "({:.0%}) — the firing is intrinsic to the check/setup "
                  "construction, not a detection of the defect.".format(b_rate))
    elif (p_rate is None and b_rate is not None
          and b_rate >= MAX_FIRE_RATIO):
        # Cycle-6 item 6 (smoke30): the MIRROR of the patched-high /
        # buggy-unknown branch above — buggy rate MEASURED and above the
        # indiscriminate cap, patched side not measured yet. Without this
        # branch the whole measurement was DISCARDED: Math-30's surviving
        # `u-complement-small` firing had a KNOWN buggy rate of 18420/20000 =
        # 92% and reached the judge carrying no rate at all, after which 6B
        # correctly reported "no rate found — verdict unchanged". No new
        # threshold (MAX_FIRE_RATIO is the shipped indiscriminate cap) and no
        # verdict: state what was measured and say the reading is undetermined,
        # exactly as the patched-side mirror does. Decision-neutral by
        # construction — 6B needs INTRINSIC_FIRE_RATIO (taken by the branch
        # above) and the 5D two-sided bar needs a patched rate, so this branch
        # can reach neither.
        tag = RATE_AMBIGUOUS_FACT_TAG
        interp = ("fires on {:.0%} of random valid inputs on the BUGGY "
                  "build; the patched-build rate is unmeasured, so how much "
                  "of this firing the patch is responsible for is "
                  "undetermined — what IS measured is that the known-broken "
                  "build already violates this claim on {:.0%} of random "
                  "valid inputs. Judge on the check's shown "
                  "contract.".format(b_rate, b_rate))

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
            "inputs. " + ((tag + " ") if tag else "") + interp)
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

# NEGATED citations are the opposite of a citation. Iteration 2 (2026-07-28)
# found 5B never firing on its own targets because the bare-substring matcher
# above counted these as citations:
#   * "an UNdocumented exact-printing contract"  -> 'document' matched INSIDE
#     the negating prefix;
#   * "is NOT CONTRADICTED BY ANY shown contract or trusted test" -> a
#     statement that NO citation exists, matched as 'contract' + 'trusted';
#   * "the trusted test ONLY REQUIRES x, NOT y" -> the trusted source is being
#     cited AGAINST the check.
# Same failure shape as the 5C terminal-marker bug: a substring read in the
# opposite sense. Strip these spans BEFORE looking for citation markers.
# Each alternative consumes the negating phrase AND the citation noun it
# governs, bounded to the clause (stops at , . ;) so it cannot swallow a
# genuine citation in a later clause.
_NEGATED_CITATION_RE = re.compile(
    r'\bundocumented\b[^,.;]{0,60}'
    r'|\b(?:is\s+)?not\s+(?:contradicted|supported|pinned|required|documented'
    r'|specified)\b[^,.;]{0,80}'
    r'|\bno\s+(?:shown\s+)?(?:contract|documentation|javadoc|trusted|spec)'
    r'\w*[^,.;]{0,60}'
    r'|\b(?:trusted\s+test|contract|javadoc|documentation|spec)\w*\s+only\s+'
    r'(?:requires|pins|covers|guarantees)\b[^,.;]{0,80}'
    r'|\bdoes\s+not\s+(?:document|specify|pin|require)\w*[^,.;]{0,60}',
    re.I)


# ---------------------------------------------------------------------------
# Cycle-5 iteration 3, PART A — STRUCTURAL citations: demand the quote, not
# the vibe.
#
# Keyword-matching the judge's prose to decide whether it CITED something has
# now failed three times in this cycle (the one-door delivery gap, the 5C
# terminal markers matching notes that DENY the identical claim, and the 5B
# markers matching 'document' inside "UNdocumented" and "not contradicted by
# any shown contract"). Substrings cannot do semantics.
#
# The replacement asks the judge for a THIRD answer line —
#     CITATION: "<verbatim quote from the shown material>"  |  NONE
# — and then checks it MECHANICALLY: normalise (lowercase, collapse
# whitespace) and test literal containment in the material the judge was
# actually shown. No interpretation is involved: either those characters were
# on the page or they were not.
# ---------------------------------------------------------------------------

# Line-bounded so a quote that runs past the end of the line is truncated
# rather than swallowing the rest of the answer (a truncated prefix of a real
# quote is still a literal substring of the context, so truncation is safe).
_CITATION_LINE_RE = re.compile(r'^[^\S\n]*CITATION[^\S\n]*:(.*)$', re.I | re.M)
# Straight and typographic double quotes only — single quotes are ambiguous
# with apostrophes ("the object's field") and would mis-slice a real citation.
_DQUOTED_RE = re.compile(r'["“]([^"”]+)["”]')
_QUOTE_CHARS = '"“”‘’\'` '
# A left-over answer-format placeholder is not a citation.
_PLACEHOLDER_RE = re.compile(r'^<.*>$|^\.+$|^n/?a$', re.I)


def _cite_norm(s):
    """Normalise for literal comparison: lowercase, whitespace collapsed."""
    return re.sub(r'\s+', ' ', str(s or '')).strip().lower()


def _context_blobs(contexts):
    """Flatten the caller's context arguments into comparable strings."""
    for ctx in contexts or ():
        if ctx is None:
            continue
        if isinstance(ctx, (list, tuple, set, frozenset)):
            yield " ".join(str(x) for x in ctx if x is not None)
        else:
            yield str(ctx)


def citation_line_status(why_or_raw):
    """Classify the answer's CITATION line. Returns ``(status, citation)``:

      * ``('quoted', <text>)``  — a citation was supplied;
      * ``('none', None)``      — the judge explicitly answered ``NONE``;
      * ``('missing', None)``   — no CITATION line, or one that is empty /
        an unfilled placeholder (answer-format NONCOMPLIANCE, which must
        never by itself void a verdict — see ``citation_void_decision``).

    The LAST CITATION line wins, so a re-ask answer appended after an earlier
    one is read. Pure."""
    if not why_or_raw:
        return ('missing', None)
    last = None
    for m in _CITATION_LINE_RE.finditer(str(why_or_raw)):
        last = m
    if last is None:
        return ('missing', None)
    rest = (last.group(1) or '').strip()
    if not rest:
        return ('missing', None)
    if rest.upper().startswith('NONE'):
        return ('none', None)
    # Strip the OUTERMOST quote pair, never the first inner one. Citations are
    # usually source lines and source lines contain quotes
    # (`assertEquals("x-0", print(n));`); taking the first "..." span truncated
    # them at the inner quote, so a legitimate grounded citation read as
    # ungrounded and VOIDED a sound dismissal (over-keeping).
    body = rest
    if len(body) >= 2 and body[0] in _QUOTE_CHARS and body[-1] in _QUOTE_CHARS:
        body = body[1:-1]
    # A model that escapes the inner quotes ("assertEquals(\"x-0\"…") must
    # still match the raw source text.
    body = body.replace('\\"', '"').replace("\\'", "'").strip()
    if not body or _PLACEHOLDER_RE.match(body):
        return ('missing', None)
    return ('quoted', body)


def parse_citation_line(why_or_raw):
    """The verbatim text the judge put on its ``CITATION:`` line, or None when
    it answered ``NONE`` / omitted the line / left a placeholder. Pure."""
    status, citation = citation_line_status(why_or_raw)
    return citation if status == 'quoted' else None


def citation_is_grounded(citation, *contexts):
    """Mechanical ground-truth check: is `citation` LITERALLY present in the
    material the judge was shown?

    True iff the citation, normalised (lowercased, whitespace collapsed), is a
    substring of at least one normalised context (harness source, code
    context, concrete evidence, trusted values — each may be a string or an
    iterable of values). A paraphrase or an invented quote is not a substring,
    so it is not grounded. No interpretation, no keywords. Pure."""
    needle = _cite_norm(citation)
    if not needle:
        return False
    return any(needle in _cite_norm(blob) for blob in _context_blobs(contexts))


def strip_citation_line(text):
    """`text` with any ``CITATION:`` line removed, so the legacy keyword
    detectors judge the WHY sentence alone (a quoted citation may contain
    words those detectors key on). Pure."""
    if not text:
        return text
    return _CITATION_LINE_RE.sub('', str(text)).strip()


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

# ---------------------------------------------------------------------------
# Cycle-5 iteration 3, PART B — carry the fact as a FACT.
#
# The marker/veto lists above try to recover, from prose, a fact the note
# builder KNEW when it wrote the note. That is the same substring-does-
# semantics mistake as the citation matcher, and it produced the same class of
# bug (a note that says "on both builds" while DENYING the identical claim was
# read as terminal, killing catches).
#
# So each note now stamps a machine-readable tag INSIDE its own text at
# emission, and ``terminal_profile`` keys on the TAG. The prose is then free to
# say whatever reads best.
#
#   [fact:identical-on-both]           same check, SAME observed values      -> TERMINAL
#   [fact:fires-on-buggy-scan]         buggy keep-going scan saw this oracle -> TERMINAL
#   [fact:fires-both-different-values] same check, DIFFERENT values          -> NOT terminal
#                                      (the partial-fix CONVICTION)
#   [fact:not-compared]                fires on both, values never compared  -> NOT terminal
#
# Cycle-6 adds four more tags, declared at the top of this module because the
# notes that stamp them are defined above this block. They are deliberately
# NOT members of the two sets below, so ``terminal_profile`` is unchanged by
# them; they are read by ``rate_profile`` / ``confirmed_fires_on_both_verdict``
# and enforced in ``judge_decision``:
#
#   [fact:rate-catch-signal]     buggy silent, patched high  (a CATCH)
#   [fact:rate-indiscriminate]   both high, or buggy >= INTRINSIC_FIRE_RATIO
#   [fact:rate-ambiguous]        low-but-not-silent, or buggy unmeasured
#   [fact:fires-on-both-confirmed] a replay MECHANICALLY confirmed the same
#                                check fires on the buggy build too
#
# ``not-compared`` stays in ``_NON_TERMINAL_FACT_TAGS`` permanently: it may
# only leave when a confirmed fires-on-both has ALSO had its values compared
# and found identical — and in that case the note stamps
# ``[fact:identical-on-both]`` instead, so the membership below never changes.
#
# The marker+veto path below stays EXACTLY as it was, used only when a text
# carries none of these tags (older runs, replayed fixtures, notes written
# elsewhere). Combination rule mirrors the prose path: deny first — any
# non-terminal tag wins over any terminal tag in the same blob.
# ---------------------------------------------------------------------------

_FACT_TAG_RE = re.compile(r'\[fact:([a-z0-9][a-z0-9-]*)\]', re.I)

TERMINAL_FACT_TAG = '[fact:identical-on-both]'
BUGGY_SCAN_FACT_TAG = '[fact:fires-on-buggy-scan]'
DIFFERENT_VALUES_FACT_TAG = '[fact:fires-both-different-values]'
NOT_COMPARED_FACT_TAG = '[fact:not-compared]'

_TERMINAL_FACT_TAGS = frozenset({'identical-on-both', 'fires-on-buggy-scan'})
_NON_TERMINAL_FACT_TAGS = frozenset({'fires-both-different-values',
                                     'not-compared'})


def fact_tags(text):
    """Every ``[fact:<name>]`` tag in `text`, lowercased, as a set. Pure."""
    return {t.lower() for t in _FACT_TAG_RE.findall(str(text or ''))}

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


# Cycle-6 PART 1: the branch names ``fire_rate_fact`` stamps, and the PROSE
# fallback for blocks written before the tags existed (older runs, replayed
# fixtures). Tag first, prose only when no tag is present.
_RATE_PROFILE_BY_TAG = {
    'rate-catch-signal': 'catch-signal',
    'rate-indiscriminate': 'indiscriminate',
    'rate-ambiguous': 'ambiguous',
}
_RATE_PROSE_FALLBACK = (
    # (profile, distinctive phrases from the shipped pre-tag wording)
    ('indiscriminate', ('indiscriminate',
                        'intrinsic to the check/setup construction')),
    ('ambiguous', ('is low but not silent',
                   'buggy-build rate is unmeasured')),
    ('catch-signal', ('silent (or near-silent) on the known-broken code',)),
)
# Deny-first ordering when a text carries several blocks/profiles: the reading
# that REFUSES the catch claim wins, so a mixed blob can never be read as
# "silent on the buggy build".
_RATE_PROFILE_PRECEDENCE = ('indiscriminate', 'ambiguous', 'catch-signal')


def parse_fire_rate_blocks(text):
    """Parse every "[fire-rate fact]" block in `text` into
    ``{'buggy': rate|None, 'patched': rate|None, 'profile': name|None}``.

    Each rate is violated/checked clamped at 1.0 (a multi-case oracle can fire
    more than once per input — the same clamp ``fire_rate_fact`` applies), or
    None when that side is absent/unmeasured. ``profile`` is the block's own
    machine tag (cycle-6) when it carries one, else None — this function does
    NOT guess a profile from prose; ``rate_profile`` owns the fallback.
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

        profile = None
        for name in _RATE_PROFILE_PRECEDENCE:
            if any(_RATE_PROFILE_BY_TAG.get(t) == name
                   for t in fact_tags(block)):
                profile = name
                break
        out.append({'buggy': _rate(_FR_BUGGY_RE.search(block)),
                    'patched': _rate(_FR_PATCHED_RE.search(block)),
                    'profile': profile})
        start = low.find(_FIRE_RATE_TAG, start + len(_FIRE_RATE_TAG))
    return out


def parse_fire_rate_facts(text):
    """Every "[fire-rate fact]" block in `text` as (buggy_rate, patched_rate)
    pairs — the rate-only view of ``parse_fire_rate_blocks``. Pure."""
    return [(b['buggy'], b['patched']) for b in parse_fire_rate_blocks(text)]


def rate_profile(text):
    """Cycle-6 PART 1: which fire-rate branch this evidence carries —
    ``'catch-signal'``, ``'indiscriminate'``, ``'ambiguous'`` or None.

    TAG-FIRST, exactly as ``terminal_profile`` reads ``[fact:...]`` tags: when
    any block carries a cycle-6 rate tag, the TAGS decide and no prose is
    consulted. Only when no block is tagged does the pre-tag prose fallback
    run. Several blocks (or several readings) resolve deny-first via
    ``_RATE_PROFILE_PRECEDENCE``, so the catch reading is never inferred from
    a blob that also reads indiscriminate. Pure."""
    if not text:
        return None
    tagged = [b['profile'] for b in parse_fire_rate_blocks(text)
              if b['profile']]
    if tagged:
        for name in _RATE_PROFILE_PRECEDENCE:
            if name in tagged:
                return name
    # Untagged (pre-cycle-6) text: fall back to the shipped prose.
    low = str(text).lower()
    for name in _RATE_PROFILE_PRECEDENCE:
        for profile, phrases in _RATE_PROSE_FALLBACK:
            if profile == name and any(p in low for p in phrases):
                return name
    return None


#: The distinct situations ``indiscriminate_rate_diagnosis`` tells apart. The
#: old ``indiscriminate_buggy_rate`` collapsed the first FOUR of these into a
#: bare None, and the 6B audit event reported all of them as "no rate found".
#: That reads as "we never measured" when the commonest case is the opposite —
#: we measured, and the number was healthy. A whole paired-run analysis was
#: nearly published off that misreading; see docs/replay/FINAL30-PAIRED.md.
RATE_STATES = ('no-measurement', 'buggy-side-unmeasured', 'below-bar',
               'catch-profile-skipped', 'at-or-above-bar')


def indiscriminate_rate_diagnosis(text):
    """Why 6B can or cannot call a firing indiscriminate — the full reason, not
    just the number.

    Returns ``{'state', 'rate', 'drop_rate', 'detail'}``:

    * ``state`` — one of ``RATE_STATES``.
    * ``rate`` — the highest buggy-side rate actually measured, or None if none
      was. Reported for EVERY state that has one, including the states where no
      drop follows; that is the whole point of this function.
    * ``drop_rate`` — the rate 6B would drop on, or None. Exactly the old
      return value, so behaviour is unchanged by construction.
    * ``detail`` — one plain sentence naming what was seen.

    The states, in the order they are tested:

    ``no-measurement``        no ``[fire-rate fact]`` block in the evidence.
    ``buggy-side-unmeasured`` blocks present, none reporting a buggy-side rate.
    ``at-or-above-bar``       a non-catch block at/above ``INTRINSIC_FIRE_RATIO``
                              — the firing is structural; this is the drop.
    ``catch-profile-skipped`` the only high rates sit in CATCH-tagged blocks,
                              which 6B never drops on.
    ``below-bar``             measured, and below the bar. The HEALTHY case: the
                              check tells the two builds apart.

    Pure — no I/O, no LLM."""
    blocks = parse_fire_rate_blocks(text)
    if not blocks:
        return {'state': 'no-measurement', 'rate': None, 'drop_rate': None,
                'detail': 'no [fire-rate fact] block appears in this evidence '
                          '— the fire rate was never measured'}
    measured = [b for b in blocks if b['buggy'] is not None]
    if not measured:
        return {'state': 'buggy-side-unmeasured', 'rate': None,
                'drop_rate': None,
                'detail': f'{len(blocks)} fire-rate block(s) present but none '
                          f'reports a buggy-side rate'}
    highest = max(b['buggy'] for b in measured)
    drop = None
    for b in measured:
        rate = b['buggy']
        if rate < INTRINSIC_FIRE_RATIO or b['profile'] == 'catch-signal':
            continue
        if drop is None or rate > drop:
            drop = rate
    if drop is not None:
        return {'state': 'at-or-above-bar', 'rate': highest, 'drop_rate': drop,
                'detail': f'buggy-side fire rate {drop:.1%} is at/above the '
                          f'{INTRINSIC_FIRE_RATIO:.0%} intrinsic bar — this '
                          f'check condemns the known-broken build on '
                          f'essentially every input'}
    catch_high = [b['buggy'] for b in measured
                  if b['profile'] == 'catch-signal'
                  and b['buggy'] >= INTRINSIC_FIRE_RATIO]
    if catch_high:
        return {'state': 'catch-profile-skipped', 'rate': highest,
                'drop_rate': None,
                'detail': f'highest buggy-side rate {max(catch_high):.1%} is '
                          f'at/above the bar but sits in a CATCH-tagged block, '
                          f'which 6B never drops on'}
    return {'state': 'below-bar', 'rate': highest, 'drop_rate': None,
            'detail': f'highest buggy-side rate {highest:.1%} is below the '
                      f'{INTRINSIC_FIRE_RATIO:.0%} intrinsic bar — measured, '
                      f'and the check discriminates between the builds'}


def indiscriminate_buggy_rate(text):
    """Cycle-6 PART 2: the MEASURED buggy-side fire rate that condemns the
    known-broken build on essentially every input, or None.

    Returns the highest buggy rate at/above ``INTRINSIC_FIRE_RATIO`` found in
    any ``[fire-rate fact]`` block, skipping any block the tag says is the
    CATCH profile (belt and braces — a catch block's buggy rate is below
    ``SILENT_FIRE_RATIO`` by construction and can never reach this bar). None
    when nothing was measured, when the buggy side is unmeasured, or when the
    rate is below the bar — a missing measurement is never a drop.

    Thin wrapper over ``indiscriminate_rate_diagnosis`` so the drop decision
    and the explanation of it can never drift apart. Pure."""
    return indiscriminate_rate_diagnosis(text)['drop_rate']


def confirmed_fires_on_both_verdict(text):
    """Cycle-6 PART 3: for evidence carrying the mechanical
    ``[fact:fires-on-both-confirmed]`` tag, what the VALUE comparison
    (``compare_fired_values``, cycle-2b) said about the two fired messages:

      * ``'different'``    — the partial-fix pattern (what caught Lang-63).
                             The strongest conviction evidence there is;
                             KEEP-leaning, never a drop.
      * ``'identical'``    — genuinely pre-existing behaviour.
      * ``'not-compared'`` — the messages could not be compared. UNKNOWN, not
                             "identical": dropping on unknown is precisely the
                             marker bug, so it is never a drop.
      * ``None``           — no confirmation, or a confirmation with no value
                             verdict attached. Nothing to decide.

    Precedence when a blob's sites disagree: ``different`` > ``identical`` >
    ``not-compared``. ``different`` is a positive contrary measurement and wins
    over everything — nothing may drop a partial fix. ``identical`` outranks
    ``not-compared`` because the latter is the ABSENCE of a comparison, not a
    contrary one: one site failing to extract a message cannot erase another
    site's successful byte/kv comparison. ``not-compared`` alone stays UNKNOWN
    and is never a drop. Pure — no prose is consulted anywhere."""
    tags = fact_tags(text)
    if 'fires-on-both-confirmed' not in tags:
        return None
    if 'fires-both-different-values' in tags:
        return 'different'
    if 'identical-on-both' in tags:
        return 'identical'
    if 'not-compared' in tags:
        return 'not-compared'
    return None


def terminal_profile(text):
    """Cycle-5C/5D: which terminal profile (if any) the evidence carries.

    Returns ``'identical-on-both'`` (textual byte-comparison fact),
    ``'fires-on-both-rate'`` (measured high buggy-side fire rate) or None.
    The textual profile wins when both are present. Pure.

    TAG-FIRST (cycle-5 iteration 3): when the evidence carries any recognised
    ``[fact:...]`` tag, the tags DECIDE — deny first, exactly as the prose path
    does — and the surrounding wording is ignored. The marker+veto path below
    runs only for untagged text (older or replayed evidence)."""
    if not text:
        return None
    tags = fact_tags(text)
    if tags & (_TERMINAL_FACT_TAGS | _NON_TERMINAL_FACT_TAGS):
        if tags & _NON_TERMINAL_FACT_TAGS:
            return None
        return 'identical-on-both'
    low = str(text).lower()
    # Deny first: a note that explicitly refuses the identical-value claim (or
    # asserts the opposite — the partial-fix conviction) is never terminal on
    # the textual path, whatever other phrasing it contains.
    denied = any(v in low for v in _TERMINAL_IDENTICAL_VETO)
    if not denied and any(m in low for m in _TERMINAL_IDENTICAL_MARKERS):
        return 'identical-on-both'
    # REVERTED 2026-07-28 (iteration-2 evidence, docs/replay/v5d_iter2_analysis.md):
    # the rate-based terminal path was measured NET-NEGATIVE — it dropped 4
    # confirmed catches (fixture rows 66/103/122/133, all `fires-on-both-rate`)
    # and gained ~0 leaks, because of the 16 leak rows only 3 carry a
    # [fire-rate fact] at all and only 1 was rate-terminal. The rates it reasons
    # about live in the inventory, not in the evidence the judge is shown — a
    # DELIVERY problem (cycle 6), not a judging rule. The two pure helpers that
    # encoded the reverted rate rule were deleted 2026-08-06 as dead code; when
    # facts are actually delivered; they are simply not consulted here.
    return None


def carries_terminal_identical_fact(text):
    """Cycle-5C: does the evidence carry a mechanical terminal fact — the
    textual IDENTICAL-ON-BOTH / fires-on-buggy marker set, or (5D) a
    [fire-rate fact] measuring the same thing as rates? Pure."""
    return terminal_profile(text) is not None


# ---------------------------------------------------------------------------
# Cycle-5B — re-ask plumbing (fail-open detection + injected statements).
# ---------------------------------------------------------------------------
