"""The four frozen prompt versions of the dev iteration protocol.

One version per iteration. Each one is frozen: once a version has been scored
on the dev side, its text must not change, because the recorded dev score
refers to that text. A new idea becomes a new version.

The system message is identical across versions on purpose. The only variable
between versions is the task wording, so a dev score difference cannot come
from two changes at once.

Every version shares one output contract: the last line names the class, and
the class vocabulary never changes. That keeps the output space equal to the
pipeline's — one bit, two values, no abstain, no score.

CHANGELOG
  v0, v1  designed before the first dev run (the baseline and the definition).
  v2, v3  designed before the first dev run as well, from the failure modes
          the drr corpus is known to contain. The protocol allows either to be
          REPLACED (never edited) once the v0/v1 dev errors are read. Record
          any replacement here, with the error class that motivated it.
"""
from dataclasses import dataclass
from typing import Dict, List

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
    "VERDICT: INCOMPLETE\n"
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
    "- INCOMPLETE: the patch removes the reported failure but leaves the root"
    " cause in place. Some other input reaches the same fault, in the patched"
    " method or elsewhere in the reachable region above.\n"
    "- CORRECT: the patch removes the root cause. No input reaches that fault"
    " any more."
)


@dataclass(frozen=True)
class PromptVersion:
    name: str
    hypothesis: str      # why this version differs from the one before it
    task: str            # text before the evidence
    instruction: str     # text after the evidence


V0 = PromptVersion(
    name='v0',
    hypothesis='Floor. Evidence, the question, and the output contract only.',
    task=(
        "Below is a Java bug, the test that reports it, and a candidate patch."
        " Decide whether the patch is a complete fix."
    ),
    instruction=(
        "Is this patch a complete fix, or does it leave the root cause in"
        " place?\n\n" + CONTRACT
    ),
)

V1 = PromptVersion(
    name='v1',
    hypothesis=(
        'v0 leaves the class boundary and the plausibility premise to be '
        'guessed. State both, and ask for a concrete surviving input.'
    ),
    task=(
        "Below is a Java bug, the test that reports it, and a candidate patch"
        " for it. Decide whether the patch fixes the root cause, or only the"
        " reported symptom."
    ),
    instruction=(
        GROUND_RULES + "\n\n" + DEFINITIONS + "\n\n"
        "Name one concrete input that still reaches the fault under this"
        " patch, if one exists. Then give your verdict.\n\n" + CONTRACT
    ),
)

V2 = PromptVersion(
    name='v2',
    hypothesis=(
        'A free-form answer drifts toward style review, and toward calling a '
        'narrow patch incomplete without evidence. Fix the decision procedure '
        'and require a named input for the positive class.'
    ),
    task=(
        "Below is a Java bug, the test that reports it, and a candidate patch"
        " for it. Decide whether the patch fixes the root cause, or only the"
        " reported symptom."
    ),
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
        "- Answer INCOMPLETE only when you can name a concrete input that"
        " still reaches the fault. A patch being short or narrow is not by"
        " itself evidence.\n\n" + CONTRACT
    ),
)

V3 = PromptVersion(
    name='v3',
    hypothesis=(
        "v2's steps are advice the answer can skip. Make them required "
        "output sections, so each one is actually performed."
    ),
    task=(
        "Below is a Java bug, the test that reports it, and a candidate patch"
        " for it. Decide whether the patch fixes the root cause, or only the"
        " reported symptom."
    ),
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
        "Judge behaviour, not style. Answer INCOMPLETE only when SURVIVING"
        " INPUT or SIBLING PATHS names something concrete.\n\n" + CONTRACT
    ),
)

VERSIONS: Dict[str, PromptVersion] = {v.name: v for v in (V0, V1, V2, V3)}
VERSION_ORDER = ('v0', 'v1', 'v2', 'v3')


def build_messages(version: str, evidence_text: str) -> List[Dict[str, str]]:
    """The chat-completion messages for one candidate patch."""
    if version not in VERSIONS:
        raise ValueError(f"unknown prompt version {version!r}; "
                         f"expected one of {VERSION_ORDER}")
    v = VERSIONS[version]
    return [
        {'role': 'system', 'content': SYSTEM},
        {'role': 'user',
         'content': '\n\n'.join([v.task, evidence_text, v.instruction])},
    ]
