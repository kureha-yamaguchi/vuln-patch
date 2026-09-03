"""The prompt versions of the Project Zero baseline.

WHY THIS IS A SEPARATE MODULE. `prompts.version_text` joins `SYSTEM` with a
version's own wording, and `version_sha256` digests the result. So one edit to
`prompts.SYSTEM` would silently change the recorded digest of every scored
Defects4J version. This module holds its own system message, its own registry
and its own `resolve`, and it imports the `PromptVersion` shape and the output
contract. Nothing here can move a Defects4J number.

WHY THE QUESTION CHANGES. The Defects4J prompt shows a failing test and asks
whether a candidate patch fixes the root cause of it. There is no test here,
and no candidate patch either. Every fix in this dataset is a real upstream
security fix that shipped. So the question becomes: did this fix remove the
whole root cause, or did it leave a sibling bug in the same code?

WHAT IS HELD CONSTANT ACROSS EVERY VERSION
  * the system message,
  * the evidence, which `evidence` renders from the diff and the source,
  * the output contract — the last line names one of two classes.
Only the task wording and the instruction move.

THE TWO CLASS NAMES ARE UNCHANGED. `OVERFITTING` and `CORRECT` are the same
two words the Defects4J baseline uses, so `verdict.py` parses both datasets
with one function and no translation step. `.claude/CONTEXT.md` defines an
overfitting patch as one that removes the reported symptom without the root
cause, and that is exactly what a prior fix with a sibling bug did.

STAGE A — blind bake-off. Three independent designs, p1, p2, p3. They mirror
the three Defects4J bets one for one, so a difference between the two datasets
is a difference in the data and not in the design space that was searched:

    p1 mirrors v1   the floor: evidence, question, a gloss, the contract
    p2 mirrors v2   a method plus guard rails
    p3 mirrors v3   a form the answer must fill in

STAGE B — refinement of the winner. An iteration is `<base>.<n>`, for example
`p2.1`. Register it with `register()` after you read the DEV errors. The dev
error log never enters a model call: a person reads it and writes the new
version by hand. `build_messages` takes a version name and the evidence text,
and there is no third parameter, so an error log has no way in.

FREEZING. A version is frozen once it has run on dev, because its recorded
score refers to its text. A later idea is a new version, never an edit.

CHANGELOG
  p1, p2, p3   stage-A designs, authored blind before any Project Zero run.
"""
import hashlib
import re
from typing import Dict, List, Tuple

from baseline_llmjudge.shared.version import CONTRACT, PromptVersion

SYSTEM = (
    'You are an expert C and C++ security engineer who reviews security '
    'fixes for completeness.'
)

#: Facts the reader must be told, or it can reason from a false premise. Two
#: of them matter most. The fix is real and it shipped, so "this would not
#: compile" is never a valid reason. And the fix did stop the reported
#: vulnerability, so "the original proof of concept still works" is not one
#: either. Neither sentence reveals the label: BOTH classes shipped, and BOTH
#: stopped the vulnerability that was reported at the time.
GROUND_RULES = (
    "Facts about this fix:\n"
    "- Upstream maintainers wrote it, reviewed it and shipped it.\n"
    "- It stopped the vulnerability that was reported at the time. The"
    " proof-of-concept input from that report no longer works.\n"
    "- Dates, bug numbers and CVE identifiers are masked in the evidence"
    " above. You cannot tell when this fix shipped, and you must not guess.\n"
    "- You cannot compile or run anything. Decide from the code alone."
)

#: The class definitions. `.claude/CONTEXT.md` defines a sibling bug as a
#: second bug that lives in the root-cause region and stems from the same root
#: cause. That is the whole positive class here.
DEFINITIONS = (
    "Decide between two classes:\n"
    "- OVERFITTING: the fix removes the reported failure but leaves the root"
    " cause in place. A sibling bug survives it — a second bug in this same"
    " code, reachable by some other input, stemming from the same root"
    " cause.\n"
    "- CORRECT: the fix removes the root cause. No sibling bug of that root"
    " cause survives in this code."
)

_SHARED_TASK = (
    "Below is an upstream security fix for a C or C++ project, and the source"
    " it applies to. Decide whether the fix removes the root cause, or only"
    " the reported symptom."
)

# --- Stage A: three independent designs -------------------------------------

P1 = PromptVersion(
    name='p1',
    hypothesis='Floor. Evidence, the question, a one-sentence gloss of each '
               'class, and the output contract. No premise, no method, no '
               'required sections — what the model does unaided.',
    task=(
        "Below is an upstream security fix for a C or C++ project, and the"
        " source it applies to. Decide whether the fix is correct or"
        " overfitting."
    ),
    instruction=(
        "An overfitting fix stops the reported failure, but it leaves the"
        " root cause in place, so a sibling bug survives it. A correct fix"
        " removes the root cause, so no sibling bug of it survives.\n\n"
        "Is this fix correct, or is it overfitting?\n\n" + CONTRACT
    ),
)

P2 = PromptVersion(
    name='p2',
    hypothesis='Definitions, the shipped-fix premise, a five-step method, and '
               'two calibration rules. Bets that a method plus guard rails '
               'beats an unaided answer.',
    task=_SHARED_TASK,
    instruction=(
        GROUND_RULES + "\n\n" + DEFINITIONS + "\n\n"
        "Work through these steps:\n"
        "1. State the root cause in one sentence: which condition makes the"
        " unfixed code unsafe?\n"
        "2. State what the fix changes.\n"
        "3. Ask whether step 2 removes the condition in step 1, or only"
        " blocks the reported input from reaching it.\n"
        "4. Check for these shapes, each of which leaves the root cause in"
        " place:\n"
        "   - a check added at one call site while a sibling call site in the"
        " same file keeps the old behaviour;\n"
        "   - a guard on the exact value the report used, rather than on the"
        " property that makes it unsafe;\n"
        "   - a bound or length check that is off by one, or that covers one"
        " direction of the condition only;\n"
        "   - a lifetime or refcount fix on one path, while another path can"
        " still reach the freed object;\n"
        "   - a type check added before one use of a value, while a later use"
        " still assumes the type.\n"
        "5. Decide.\n\n"
        "Two calibration rules:\n"
        "- Judge behaviour, not style. A narrow fix that removes the root"
        " cause is still CORRECT.\n"
        "- Answer OVERFITTING only when you can name the surviving sibling"
        " bug: which code, and roughly what input reaches it. A fix being"
        " small is not by itself evidence.\n\n" + CONTRACT
    ),
)

P3 = PromptVersion(
    name='p3',
    hypothesis='Definitions, the shipped-fix premise, and five REQUIRED '
               'output sections. Bets that a form the answer must fill in '
               'beats a method it may skip.',
    task=_SHARED_TASK,
    instruction=(
        GROUND_RULES + "\n\n" + DEFINITIONS + "\n\n"
        "Answer in exactly these five sections, each one heading followed by"
        " at most three sentences:\n\n"
        "ROOT CAUSE: which condition makes the unfixed code unsafe.\n"
        "FIX EFFECT: what the fix changes, in behavioural terms.\n"
        "SURVIVING SIBLING: one concrete sibling bug that survives this fix —"
        " name the code and roughly what input reaches it — or the words"
        " 'none found', and if none, say why the condition can no longer"
        " hold.\n"
        "SIBLING SITES: whether any other site in the shown source still"
        " carries the unfixed behaviour.\n"
        "DECISION: one sentence.\n\n"
        "Judge behaviour, not style. Answer OVERFITTING only when SURVIVING"
        " SIBLING or SIBLING SITES names something concrete.\n\n" + CONTRACT
    ),
)

#: The stage-A designs. Each runs once on the dev side, blind, in any order.
BASE_VERSIONS: Tuple[str, ...] = ('p1', 'p2', 'p3')

VERSIONS: Dict[str, PromptVersion] = {v.name: v for v in (P1, P2, P3)}

# An iteration is '<base>.<n>', e.g. 'p2.1'. The letter differs from the
# Defects4J grammar, so a name cannot be valid in both registries.
_ITERATION_RE = re.compile(r'^(?P<base>p\d+)\.(?P<n>[1-9]\d*)$')


def is_iteration(name: str) -> bool:
    return bool(_ITERATION_RE.match(name))


def base_of(name: str) -> str:
    """The stage-A design a version belongs to ('p2.1' -> 'p2')."""
    m = _ITERATION_RE.match(name)
    return m.group('base') if m else name


def stage_of(name: str) -> str:
    return 'B' if is_iteration(name) else 'A'


def known_versions() -> List[str]:
    return list(BASE_VERSIONS) + [n for n in VERSIONS if is_iteration(n)]


def register(version: PromptVersion) -> PromptVersion:
    """Add a stage-B iteration.

    The name must be `<base>.<n>` for one of the stage-A designs, so the
    lineage of every score is readable from its name alone. Re-registration is
    refused: a scored version is frozen, and a new idea is a new iteration."""
    m = _ITERATION_RE.match(version.name)
    if not m:
        raise ValueError(
            f"{version.name!r} is not an iteration name; expected "
            f"'<base>.<n>' with base in {BASE_VERSIONS}, e.g. 'p2.1'")
    if m.group('base') not in BASE_VERSIONS:
        raise ValueError(f"unknown base {m.group('base')!r}; "
                         f"expected one of {BASE_VERSIONS}")
    if version.name in VERSIONS:
        raise ValueError(f"{version.name!r} is already registered; a scored "
                         f"version is frozen — add a new iteration instead")
    VERSIONS[version.name] = version
    return version


# --- Stage B: iterations of the stage-A winner -------------------------------
# Add one entry per turn, AFTER you have read the previous DEV run's errors.
# Write the child as a `.replace()` on its parent's instruction, so the single
# change is a literal line in this file rather than a promise. Copy this shape:
#
# register(PromptVersion(
#     name='p2.1',
#     hypothesis='p2 produced N FP and M FN on dev. It called a correct fix '
#                'overfitting whenever the fix was narrow, so require the '
#                'surviving sibling to be named.',
#     task=P2.task,
#     instruction=P2.instruction.replace('old sentence', 'new sentence'),
# ))

#: Turn 1 of the crashing pool.
#:
#: WHY p1 IS THE BASE, AND WHAT THAT IS WORTH. All three stage-A designs scored
#: F1 0.000 on the crashing dev side, and p1 and p2 tied exactly at FP=0,
#: FN=11. `compare.select` breaks a tie on the false-positive count, and with
#: that equal it kept the registry order. So "p1 won" means "p1 and p2 could
#: not be told apart". The choice of base carries no information here, and the
#: iteration log must say so.
#:
#: THE DEV ERROR CLASS. 11 false negatives, 0 false positives. Every one of the
#: 11 overfitting fixes was called correct, and 9 of them unanimously. The
#: reasoning was the same shape every time, in p1's own words: the fix changes
#: "the mechanism rather than one input value", therefore it removes the root
#: cause. The model equates a principled fix with a complete one.
#:
#: Two examples from the dev log. On the WebAudio fix it listed the other
#: readers of `reverb_` itself, then dismissed them because the fix made the
#: null branch match the non-null branch. On the UNIX-socket GC fix it
#: reconstructed the MSG_PEEK race correctly and accepted `unix_peek_fds()` as
#: the cure. The sibling of that pair lives in `fs/file.c`, which the diff
#: never touched and the evidence therefore never showed.
#:
#: SO THE TURN CHANGES TWO THINGS, AND BOTH ARE CALIBRATION, NOT METHOD.
#:   1. It breaks the inference "generic, therefore complete". The positives of
#:      this dataset are mostly well-engineered fixes that still left a
#:      sibling.
#:   2. It states that the shown source may not contain the sibling at all,
#:      because only the touched files are rendered. Without that sentence, "I
#:      can see no other vulnerable site" reads as evidence of completeness,
#:      when it is mostly evidence about the size of the excerpt.
#:
#: Neither sentence names a class, a date or an identifier, so neither leaks a
#: label. `test_no_label_reaches_a_pz_prompt` covers this version too.
register(PromptVersion(
    name='p1.1',
    hypothesis='p1 produced 0 FP and 11 FN on dev: it called every '
               'overfitting fix correct, 9 of them unanimously. Every error '
               'argued that the fix changes the mechanism rather than one '
               'input value, and took that as proof of completeness. This '
               'turn breaks that one inference, and states that the shown '
               'source may not contain the sibling at all.',
    task=P1.task,
    instruction=P1.instruction.replace(
        "Is this fix correct, or is it overfitting?",
        "Two things to weigh before you answer.\n"
        "- A fix can be generic, well engineered and reviewed, and still be"
        " overfitting. The question is not whether the fix is principled. It"
        " is whether the same root cause can still be reached somewhere"
        " else.\n"
        "- You are shown only the files the fix touched. A sibling bug of this"
        " root cause may live in code that is not shown. So finding no second"
        " vulnerable site in the excerpt above is weak evidence about"
        " completeness.\n\n"
        "Both answers are common for fixes like this one.\n\n"
        "Is this fix correct, or is it overfitting?"),
))


def resolve(name: str) -> PromptVersion:
    if name not in VERSIONS:
        raise ValueError(
            f"unknown prompt version {name!r}. Registered: "
            f"{known_versions()}. A stage-B iteration must be added to "
            f"prompts.py with register() before it can be run.")
    return VERSIONS[name]


def version_text(name: str) -> str:
    """The version's own wording: system, task and instruction, no evidence."""
    v = resolve(name)
    return '\n\n'.join([SYSTEM, v.task, v.instruction])


def version_sha256(name: str) -> str:
    """Digest of the wording, so a scored version cannot be edited unnoticed."""
    return hashlib.sha256(version_text(name).encode()).hexdigest()


def build_messages(name: str, evidence_text: str) -> List[Dict[str, str]]:
    """The chat-completion messages for one fix.

    Two parameters, and neither one is an error log. That is what keeps the
    refinement turn a manual edit rather than a matter of discipline."""
    v = resolve(name)
    return [
        {'role': 'system', 'content': SYSTEM},
        {'role': 'user',
         'content': '\n\n'.join([v.task, evidence_text, v.instruction])},
    ]
