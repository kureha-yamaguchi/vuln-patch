"""The prompt versions of the two-stage dev protocol.

STAGE A — blind bake-off. Three INDEPENDENT designs, each run once on the dev
side. No error log is read during this stage: reading one design's errors before
the others have run would make the comparison unfair, because the later designs
would carry information the earlier ones did not. Pick the best dev F1.

    v1  ─┐
    v2  ─┼─  each run once on dev, blind  ─→  best dev F1 wins
    v3  ─┘

STAGE B — refinement of the stage-A winner, three turns. Each turn DOES read the
previous run's errors. An iteration is named `<winner>.<n>`:

    v2  ──→ read v2's errors ──→ v2.1 ──→ read v2.1's errors ──→ v2.2 ──→ ...

Pick the best dev F1 among the three iterations, freeze it, run the holdout once.

The numbers v1, v2 and v3 are labels, not an order: the three stage-A designs
run independently. A fourth draft was dropped before any run, because it was a
strict subset of v2.

WHAT IS HELD CONSTANT ACROSS EVERY VERSION
  * the system message,
  * the evidence (extracted once per patch and cached),
  * the output contract — the last line names one of two classes.
Only the task wording and the instruction move. So a dev score difference has
one cause.

FREEZING
A version is frozen once it has been run on dev, because its recorded score
refers to its text. A later idea becomes a new version, never an edit. Every
record carries `prompt_sha256`, so a silent edit is detectable.

CHANGELOG
  v1, v2, v3   stage-A designs, authored blind. Not yet scored.
  <winner>.1-3 stage-B iterations. Record here what each one repaired, and
               which error class the dev log showed.
"""
import hashlib
import re
from dataclasses import dataclass
from typing import Dict, List, Tuple

# Mirrors the pipeline's framing ("You are an expert Java security engineer
# who writes Jazzer fuzzing harnesses") with the task swapped.
SYSTEM = (
    'You are an expert Java security engineer who reviews bug fixes for '
    'completeness.'
)

# The parseable contract. `verdict.parse` reads the last VERDICT: line.
CONTRACT = (
    "End your answer with a final line in exactly one of these two forms, "
    "and write nothing after it:\n"
    "VERDICT: OVERFITTING\n"
    "VERDICT: CORRECT"
)

# Facts the pipeline holds implicitly and the baseline must be told, or it can
# reason from a false premise. The candidate patch is plausible by
# construction: it compiles, and it makes the shown test pass. "The test still
# fails" is therefore never a valid reason.
GROUND_RULES = (
    "Facts about the patch:\n"
    "- An automated program-repair tool produced it as a candidate fix.\n"
    "- It compiles, and it already makes the failing test above pass. Every"
    " other test in the project's suite also passes.\n"
    "- You cannot compile or run anything. Decide from the code alone."
)

DEFINITIONS = (
    "Decide between two classes:\n"
    "- OVERFITTING: the patch removes the reported failure but leaves the root"
    " cause in place. Some other input reaches the same fault, in the patched"
    " method or elsewhere in the reachable region above.\n"
    "- CORRECT: the patch removes the root cause. No input reaches that fault"
    " any more."
)

_SHARED_TASK = (
    "Below is a Java bug, the test that reports it, and a candidate patch"
    " for it. Decide whether the patch fixes the root cause, or only the"
    " reported symptom."
)


@dataclass(frozen=True)
class PromptVersion:
    name: str
    hypothesis: str      # what this version's design bets on
    task: str            # text before the evidence
    instruction: str     # text after the evidence


# --- Stage A: three independent designs -------------------------------------

# v1 glosses the two classes in one sentence each, and stops there. The word
# 'overfitting' is a term of art: a model that has to guess what it means is
# not an unaided FLOOR, it is a vocabulary test. The gloss is therefore the
# minimum v1 needs to ask its question at all. What v1 still withholds is the
# plausibility premise, the failure shapes, the method and the output form.
V1 = PromptVersion(
    name='v1',
    hypothesis='Floor. Evidence, the question, a one-sentence gloss of each '
               'class, and the output contract. No plausibility premise, no '
               'method, no required sections — what the model does unaided.',
    task=(
        "Below is a Java bug, the test that reports it, and a candidate patch."
        " Decide whether the patch is correct or overfitting."
    ),
    instruction=(
        "An overfitting patch stops the reported failure, but it leaves the"
        " root cause in place. Some other input still reaches the same fault."
        " A correct patch removes the root cause, so no input reaches that"
        " fault any more.\n\n"
        "Is this patch correct, or is it overfitting?\n\n" + CONTRACT
    ),
)

V2 = PromptVersion(
    name='v2',
    hypothesis='Definitions, the plausibility premise, a five-step method, and '
               'two calibration rules. Bets that a method plus guard rails '
               'beats an unaided answer.',
    task=_SHARED_TASK,
    instruction=(
        GROUND_RULES + "\n\n" + DEFINITIONS + "\n\n"
        "Work through these steps:\n"
        "1. State the root cause in one sentence: which condition makes the"
        " original code fail?\n"
        "2. State what the patch changes.\n"
        "3. Ask whether step 2 removes the condition in step 1, or only"
        " blocks the reported input from reaching it.\n"
        "4. Check for these shapes, each of which leaves the root cause in"
        " place:\n"
        "   - a guard on the exact value the failing test uses, rather than on"
        " the property that makes it fail;\n"
        "   - a caught throwable that is swallowed or replaced with a default,"
        " hiding the fault instead of removing it;\n"
        "   - a fix on one path while a sibling path in the reachable region"
        " keeps the old behaviour;\n"
        "   - a bound or length check that is off by one, or that covers one"
        " direction of the condition only.\n"
        "5. Decide.\n\n"
        "Two calibration rules:\n"
        "- Judge behaviour, not style. A patch that no human would write is"
        " still CORRECT when it removes the root cause.\n"
        "- Answer OVERFITTING only when you can name a concrete input that"
        " still reaches the fault. A patch being short or narrow is not by"
        " itself evidence.\n\n" + CONTRACT
    ),
)

V3 = PromptVersion(
    name='v3',
    hypothesis='Definitions, the plausibility premise, and five REQUIRED '
               'output sections. Bets that a form the answer must fill in '
               'beats a method it may skip.',
    task=_SHARED_TASK,
    instruction=(
        GROUND_RULES + "\n\n" + DEFINITIONS + "\n\n"
        "Answer in exactly these five sections, each one heading followed by"
        " at most three sentences:\n\n"
        "ROOT CAUSE: which condition makes the original code fail.\n"
        "PATCH EFFECT: what the patch changes, in behavioural terms.\n"
        "SURVIVING INPUT: one concrete input that still reaches the fault"
        " under this patch, or the words 'none found' — and, if none, say why"
        " the condition can no longer hold.\n"
        "SIBLING PATHS: whether any other function in the reachable region"
        " still carries the unpatched behaviour.\n"
        "DECISION: one sentence.\n\n"
        "Judge behaviour, not style. Answer OVERFITTING only when SURVIVING"
        " INPUT or SIBLING PATHS names something concrete.\n\n" + CONTRACT
    ),
)

#: The stage-A designs. Each runs once on dev, blind, in any order.
BASE_VERSIONS: Tuple[str, ...] = ('v1', 'v2', 'v3')

VERSIONS: Dict[str, PromptVersion] = {v.name: v for v in (V1, V2, V3)}

# An iteration is '<base>.<n>', e.g. 'v2.1'.
_ITERATION_RE = re.compile(r'^(?P<base>v\d+)\.(?P<n>[1-9]\d*)$')


def register(version: PromptVersion) -> PromptVersion:
    """Add a stage-B iteration.

    The name must be `<base>.<n>` for one of the stage-A designs, so the
    lineage of every score is readable from its name alone. Re-registration is
    refused: a scored version is frozen, and a new idea is a new iteration."""
    m = _ITERATION_RE.match(version.name)
    if not m:
        raise ValueError(
            f"{version.name!r} is not an iteration name; expected "
            f"'<base>.<n>' with base in {BASE_VERSIONS}, e.g. 'v2.1'")
    if m.group('base') not in BASE_VERSIONS:
        raise ValueError(f"unknown base {m.group('base')!r}; "
                         f"expected one of {BASE_VERSIONS}")
    if version.name in VERSIONS:
        raise ValueError(f"{version.name!r} is already registered; a scored "
                         f"version is frozen — add a new iteration instead")
    VERSIONS[version.name] = version
    return version


# --- Stage B: iterations of the stage-A winner -------------------------------
# Add one entry per turn, AFTER you have read the previous run's errors with
#     uv run -m baseline_llmjudge.errors --records <run>/records.jsonl
# Set `hypothesis` to the error class the iteration repairs. Copy this shape:
#
# register(PromptVersion(
#     name='v2.1',
#     hypothesis='v2 produced 7 FP and 2 FN. It called a correct patch '
#                'overfitting whenever the fix was narrower than the developer '
#                'fix, so require the surviving input to be spelled out.',
#     task=V2.task,
#     instruction=V2.instruction.replace(
#         'Answer OVERFITTING only when you can name a concrete input',
#         'Answer OVERFITTING only when you WRITE OUT a concrete input'),
# ))


def is_iteration(name: str) -> bool:
    return bool(_ITERATION_RE.match(name))


def base_of(name: str) -> str:
    """The stage-A design a version belongs to ('v2.1' -> 'v2')."""
    m = _ITERATION_RE.match(name)
    return m.group('base') if m else name


def iterations_of(base: str) -> List[str]:
    """Registered iterations of one design, in numeric order."""
    return sorted((n for n in VERSIONS
                   if is_iteration(n) and base_of(n) == base),
                  key=lambda n: int(n.split('.')[1]))


def stage_of(name: str) -> str:
    return 'B' if is_iteration(name) else 'A'


def known_versions() -> List[str]:
    return list(BASE_VERSIONS) + [n for n in VERSIONS
                                  if is_iteration(n)]


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
    """The chat-completion messages for one candidate patch."""
    v = resolve(name)
    return [
        {'role': 'system', 'content': SYSTEM},
        {'role': 'user',
         'content': '\n\n'.join([v.task, evidence_text, v.instruction])},
    ]
