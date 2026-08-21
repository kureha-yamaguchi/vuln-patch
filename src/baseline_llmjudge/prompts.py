"""The prompt versions of the two-stage dev protocol.

TWO POOLS, TWO FAMILIES OF DESIGN. A crashing bug is reported at run time by a
throwable. A semantic bug is reported by nothing: its trigger test fails a
JUnit assertion because the code returns a wrong value. The question is the
same for both, and the output space is the same, but the failure shapes an
overfitting patch can take are not. So each pool has its own stage-A designs:

    crashing   v1, v2, v3   and the iterations v1.1, v1.2, ...
    semantic   s1, s2, s3   and the iterations s1.1, s1.2, ...

The two families share `SYSTEM`, `CONTRACT` and `GROUND_RULES` byte for byte,
so the two pools are not two experiments with two output contracts. A version
belongs to exactly one pool, and `kind_of` is the one place that says which.

STAGE A — blind bake-off. Three INDEPENDENT designs, each run once on the dev
side. No error log is read during this stage: reading one design's errors before
the others have run would make the comparison unfair, because the later designs
would carry information the earlier ones did not. Pick the best dev F1.

    v1  ─┐
    v2  ─┼─  each run once on dev, blind  ─→  best dev F1 wins
    v3  ─┘

STAGE B — refinement of the stage-A winner, three turns. Each turn DOES read the
previous run's DEV errors. An iteration is named `<winner>.<n>`:

    v2  ──→ read v2's dev errors ──→ v2.1 ──→ read v2.1's dev errors ──→ v2.2 ──→ ...

Every iteration is then run on both sides. The dev run feeds the next turn. The
holdout run selects: the iteration with the best HOLDOUT F1 wins.

The holdout gives one number per iteration and never gives a sentence, because
`errors.py` refuses holdout records. So no holdout evidence reaches any prompt.
The bias that does remain is a selection bias: the winner's holdout F1 is a
maximum over three, so all three rows must be published.

The numbers in v1, v2, v3 and in s1, s2, s3 are labels, not an order: the
three stage-A designs of a pool run independently. On the crashing side a
fourth draft was dropped before any run, because it was a strict subset of v2.

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
  v1, v2, v3   crashing stage-A designs, authored blind. Scored; see the
               README's iteration log.
  v1.1 - v1.3  crashing stage-B iterations of the stage-A winner.
  s1, s2, s3   semantic stage-A designs, authored blind before any semantic
               run. Scored; s3 won on dev F1.
  s3.1         binds the judgement to the reported observable: adds a REPORTED
               FAULT section and four exclusions. Cut dev FP 20 -> 13, raised
               dev FN 5 -> 17.
  s3.2         repairs s3.1's sibling clause, which asked a sibling to produce
               the reported fault, and adds a PATCH SCOPE section. Recovered
               some recall, and handed the Math-59 FP cluster back.
  s3.3         the s3 form plus one paragraph, after two turns bought no
               precision. Parent is s3, not s3.2. Did not move the FP core.
  <winner>.1-3 stage-B iterations. Record here what each one repaired, and
               which error class the DEV log showed. Never cite a holdout
               error here: an iteration's stated reason is its audit trail,
               and a holdout reason would prove the holdout leaked.
"""
import hashlib
import re
from dataclasses import dataclass
from typing import Dict, List, Tuple

#: The two bug pools. A bug kind is the coarse split of the dataset: a
#: crashing bug reports itself at run time with a throwable, a semantic bug
#: reports itself with nothing at all.
KIND_CRASHING = 'crashing'
KIND_SEMANTIC = 'semantic'
KINDS = (KIND_CRASHING, KIND_SEMANTIC)

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

#: The crashing pool's class definitions. A crashing bug's fault is a
#: throwable, so the positive class is about an input that still reaches it.
DEFINITIONS = (
    "Decide between two classes:\n"
    "- OVERFITTING: the patch removes the reported failure but leaves the root"
    " cause in place. Some other input reaches the same fault, in the patched"
    " method or elsewhere in the reachable region above.\n"
    "- CORRECT: the patch removes the root cause. No input reaches that fault"
    " any more."
)

#: The semantic pool's class definitions. A semantic bug throws nothing, so
#: the positive class is about an input that still gets a wrong value. The two
#: class NAMES and the output contract are the same in both pools.
SEMANTIC_DEFINITIONS = (
    "Decide between two classes:\n"
    "- OVERFITTING: the patch corrects the value the failing test reports, but"
    " it leaves the root cause in place. Some other input still gets a wrong"
    " value out of the same computation, in the patched method or elsewhere in"
    " the reachable region above.\n"
    "- CORRECT: the patch removes the root cause. No input gets a wrong value"
    " out of that computation any more."
)

_SHARED_TASK = (
    "Below is a Java bug, the test that reports it, and a candidate patch"
    " for it. Decide whether the patch fixes the root cause, or only the"
    " reported symptom."
)

#: The same task for the semantic pool. It names the failure shape, because a
#: semantic bug reports itself through a wrong value rather than a throwable.
_SHARED_SEMANTIC_TASK = (
    "Below is a Java bug, the test that reports it, and a candidate patch"
    " for it. The bug is not a crash: the test fails because the code returns"
    " a wrong value. Decide whether the patch fixes the root cause, or only"
    " the reported symptom."
)


@dataclass(frozen=True)
class PromptVersion:
    name: str
    hypothesis: str      # what this version's design bets on
    task: str            # text before the evidence
    instruction: str     # text after the evidence
    #: Which pool this version judges: 'crashing' or 'semantic'. It defaults
    #: to the crashing pool, so the four scored crashing versions below keep
    #: their recorded text and their recorded digest unchanged.
    kind: str = KIND_CRASHING


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

# --- Stage A, semantic pool: three independent designs ----------------------
#
# Authored blind, before any semantic run. They mirror the three crashing bets
# one for one, so a difference between the two pools is a difference in the
# bugs and not in the design space that was searched:
#
#   s1 mirrors v1   the floor: evidence, question, a gloss, the contract
#   s2 mirrors v2   a method plus guard rails
#   s3 mirrors v3   a form the answer must fill in
#
# What changes is the failure shape. A crashing bug's fault is a throwable, so
# v2 asks about an input that still reaches a throw site. A semantic bug throws
# nothing, so s2 asks about an input that still gets a wrong value out. The
# shapes in step 4 changed for the same reason: a swallowed throwable cannot be
# the shape of a semantic overfit, and a hard-coded expected value can.

S1 = PromptVersion(
    name='s1',
    kind=KIND_SEMANTIC,
    hypothesis='Floor. Evidence, the question, a one-sentence gloss of each '
               'class, and the output contract. No plausibility premise, no '
               'method, no required sections — what the model does unaided.',
    task=(
        "Below is a Java bug, the test that reports it, and a candidate patch."
        " The bug is not a crash: the test fails because the code returns a"
        " wrong value. Decide whether the patch is correct or overfitting."
    ),
    instruction=(
        "An overfitting patch corrects the value the failing test reports, but"
        " it leaves the root cause in place. Some other input still gets a"
        " wrong value out of the same computation. A correct patch removes the"
        " root cause, so no input gets a wrong value out of that computation"
        " any more.\n\n"
        "Is this patch correct, or is it overfitting?\n\n" + CONTRACT
    ),
)

S2 = PromptVersion(
    name='s2',
    kind=KIND_SEMANTIC,
    hypothesis='Definitions, the plausibility premise, a five-step method, and '
               'two calibration rules. Bets that a method plus guard rails '
               'beats an unaided answer.',
    task=_SHARED_SEMANTIC_TASK,
    instruction=(
        GROUND_RULES + "\n\n" + SEMANTIC_DEFINITIONS + "\n\n"
        "Work through these steps:\n"
        "1. State the root cause in one sentence: which condition makes the"
        " original code compute the wrong value?\n"
        "2. State what the patch changes.\n"
        "3. Ask whether step 2 removes the condition in step 1, or only"
        " corrects the value for the reported input.\n"
        "4. Check for these shapes, each of which leaves the root cause in"
        " place:\n"
        "   - a branch, a lookup or a table entry keyed on the exact input the"
        " failing test uses, rather than on the property that makes the"
        " computation wrong;\n"
        "   - the expected value returned directly for the reported input,"
        " while the computation behind it is unchanged;\n"
        "   - a fix on one overload, one family member or one path, while a"
        " sibling in the reachable region keeps the old computation;\n"
        "   - the reported observable corrected, while a related accessor, a"
        " round trip or a second read of the same state still reports the old"
        " value;\n"
        "   - a bound, a rounding rule or a sign test that is off by one, or"
        " that covers one direction of the condition only.\n"
        "5. Decide.\n\n"
        "Two calibration rules:\n"
        "- Judge behaviour, not style. A patch that no human would write is"
        " still CORRECT when it removes the root cause.\n"
        "- Answer OVERFITTING only when you can name a concrete input that"
        " still gets a wrong value out, and say which value is wrong. A patch"
        " being short or narrow is not by itself evidence.\n\n" + CONTRACT
    ),
)

S3 = PromptVersion(
    name='s3',
    kind=KIND_SEMANTIC,
    hypothesis='Definitions, the plausibility premise, and five REQUIRED '
               'output sections. Bets that a form the answer must fill in '
               'beats a method it may skip.',
    task=_SHARED_SEMANTIC_TASK,
    instruction=(
        GROUND_RULES + "\n\n" + SEMANTIC_DEFINITIONS + "\n\n"
        "Answer in exactly these five sections, each one heading followed by"
        " at most three sentences:\n\n"
        "ROOT CAUSE: which condition makes the original code compute the wrong"
        " value.\n"
        "PATCH EFFECT: what the patch changes, in behavioural terms.\n"
        "SURVIVING INPUT: one concrete input that still gets a wrong value out"
        " under this patch, with the wrong value named — or the words 'none"
        " found', and then why the condition can no longer hold.\n"
        "SIBLING PATHS: whether any other function in the reachable region, or"
        " any other reader of the same state, still carries the unpatched"
        " computation.\n"
        "DECISION: one sentence.\n\n"
        "Judge behaviour, not style. Answer OVERFITTING only when SURVIVING"
        " INPUT or SIBLING PATHS names something concrete.\n\n" + CONTRACT
    ),
)

#: The stage-A designs of each pool. Each runs once on its pool's dev side,
#: blind, in any order. A version name belongs to exactly one pool, so a run
#: directory, a summary row and an error log all name their pool implicitly.
BASE_VERSIONS_BY_KIND: Dict[str, Tuple[str, ...]] = {
    KIND_CRASHING: ('v1', 'v2', 'v3'),
    KIND_SEMANTIC: ('s1', 's2', 's3'),
}

VERSIONS: Dict[str, PromptVersion] = {
    v.name: v for v in (V1, V2, V3, S1, S2, S3)}

# An iteration is '<base>.<n>', e.g. 'v2.1' or 's1.2'. One grammar covers both
# pools, because the base letter already says which pool it belongs to.
_ITERATION_RE = re.compile(r'^(?P<base>[vs]\d+)\.(?P<n>[1-9]\d*)$')


def base_versions(kind: str = None) -> Tuple[str, ...]:
    """The stage-A designs of one pool, or of both when kind is None."""
    if kind is None:
        return tuple(n for k in KINDS for n in BASE_VERSIONS_BY_KIND[k])
    if kind not in BASE_VERSIONS_BY_KIND:
        raise ValueError(f"unknown bug kind {kind!r}; expected one of {KINDS}")
    return BASE_VERSIONS_BY_KIND[kind]


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


def kind_of(name: str) -> str:
    """The pool a version judges: 'crashing' or 'semantic'.

    A registered version states its own kind. An unregistered base is read
    off `BASE_VERSIONS_BY_KIND`, so a stage-B iteration can ask about its
    base before that base has been looked up itself."""
    if name in VERSIONS:
        return VERSIONS[name].kind
    base = base_of(name)
    for kind, names in BASE_VERSIONS_BY_KIND.items():
        if base in names:
            return kind
    raise ValueError(f"unknown prompt version {name!r}; cannot tell which "
                     f"pool it judges")


def known_versions(kind: str = None) -> List[str]:
    """Every registered version, or every version of one pool."""
    bases = base_versions(kind)
    iterations = [n for n in VERSIONS if is_iteration(n)
                  and base_of(n) in bases]
    return list(bases) + iterations


def register(version: PromptVersion) -> PromptVersion:
    """Add a stage-B iteration.

    The name must be `<base>.<n>` for one of the stage-A designs, so the
    lineage of every score is readable from its name alone. Re-registration is
    refused: a scored version is frozen, and a new idea is a new iteration.

    The `kind` must match the base's own pool. A crashing iteration of a
    semantic design would be scored against the wrong split, and the mismatch
    would only show up as an unexplained population change."""
    all_bases = base_versions()
    m = _ITERATION_RE.match(version.name)
    if not m:
        raise ValueError(
            f"{version.name!r} is not an iteration name; expected "
            f"'<base>.<n>' with base in {all_bases}, e.g. 'v2.1'")
    base = m.group('base')
    if base not in all_bases:
        raise ValueError(f"unknown base {base!r}; "
                         f"expected one of {all_bases}")
    if version.name in VERSIONS:
        raise ValueError(f"{version.name!r} is already registered; a scored "
                         f"version is frozen — add a new iteration instead")
    base_kind = kind_of(base)
    if version.kind != base_kind:
        raise ValueError(
            f"{version.name!r} declares kind {version.kind!r}, but its base "
            f"{base!r} is a {base_kind} design. Pass kind={base_kind!r}.")
    VERSIONS[version.name] = version
    return version


# --- Stage B: iterations of the stage-A winner -------------------------------
# Add one entry per turn, AFTER you have read the previous run's DEV errors:
#     uv run -m baseline_llmjudge.errors --records <dev run>/records.jsonl
# Set `hypothesis` to the error class the DEV log showed. Copy this shape:
#
# register(PromptVersion(
#     name='v2.1',
#     hypothesis='v2 produced 7 FP and 2 FN on dev. It called a correct patch '
#                'overfitting whenever the fix was narrower than the developer '
#                'fix, so require the surviving input to be spelled out.',
#     task=V2.task,
#     instruction=V2.instruction.replace(
#         'Answer OVERFITTING only when you can name a concrete input',
#         'Answer OVERFITTING only when you WRITE OUT a concrete input'),
# ))
#
# An iteration of a SEMANTIC design must also declare its pool. `register`
# refuses a mismatch, so the argument is a check and not a reminder:
#
# register(PromptVersion(
#     name='s2.1',
#     kind=KIND_SEMANTIC,
#     hypothesis='...',
#     task=S2.task,
#     instruction=S2.instruction.replace('...', '...'),
# ))


register(PromptVersion(
    name='v1.1',
    hypothesis='v1 produced 7 FP and 2 FN on dev, so FP dominated. Six of the '
               'seven FP were Lang-6 patches, and the reasoning rejected each '
               'one for a collateral reason: the patch advances the index from '
               'the wrong base, so traversal elsewhere can be wrong and no '
               'human would write it. None of the six named an input that '
               'still reaches the reported fault. So make fault reachability '
               'the only ground for the positive class, and rule the style '
               'and collateral-behaviour objection out.',
    task=V1.task,
    instruction=V1.instruction.replace(
        "Is this patch correct, or is it overfitting?",
        "Answer OVERFITTING only when you can name a concrete input that still"
        " reaches the same fault under this patch. A patch that is clumsy,"
        " that no human would write, or that changes other behaviour is still"
        " CORRECT when no input reaches that fault any more.\n\n"
        "Is this patch correct, or is it overfitting?"),
))


register(PromptVersion(
    name='v1.2',
    hypothesis='v1.1 produced 1 FP and 8 FN on dev, so the class flipped and '
               'FN dominated. Every one of the eight cited v1.1\'s own bar '
               'back at it: "per the criterion you gave, it is only '
               'OVERFITTING if the same fault is still reachable, and I cannot '
               'name such an input". The patches it excused only disabled the '
               'failing path — an always-true disjunct in Lang-51 Elixir and '
               'Jaid, an always-false comparison of two types in Math-32 '
               'Jaid, a deleted loop in Lang-39 Arja. So qualify the '
               'carve-out: a disabled path is not a removed root cause.',
    task=V1.task,
    instruction=VERSIONS['v1.1'].instruction.replace(
        "A patch that is clumsy, that no human would write, or that changes"
        " other behaviour is still CORRECT when no input reaches that fault"
        " any more.",
        "A patch that is clumsy or that no human would write is still CORRECT"
        " when it removes the root cause. But a patch that only disables the"
        " failing path leaves the root cause in place: a condition made always"
        " true or always false, a comparison of two types that can never be"
        " equal, or the deletion of the computation that failed. Call that"
        " OVERFITTING even when you cannot name an input that reaches the old"
        " throw site."),
))


register(PromptVersion(
    name='v1.3',
    hypothesis='v1.2 produced 7 FP and 4 FN on dev, so FP dominated again, and '
               'six of the seven FP were the Lang-6 cluster. Each one did name '
               'a concrete input, but the failure it named was not the '
               'reported one: the counterexamples skip a character or corrupt '
               'the traversal, and none of them throws the '
               'StringIndexOutOfBoundsException that the trigger test '
               'reports. So require the named input to reproduce the same '
               'exception at the same place, and rule a merely wrong result '
               'out.',
    task=V1.task,
    instruction=VERSIONS['v1.2'].instruction.replace(
        "Answer OVERFITTING only when you can name a concrete input that still"
        " reaches the same fault under this patch.",
        "Answer OVERFITTING only when you can name a concrete input that still"
        " reaches the same fault under this patch — the same exception, thrown"
        " at the same place as the reported failure. An input that only makes"
        " the patched code return a wrong result, skip data or compute a wrong"
        " value is not that input."),
))


S3_1 = register(PromptVersion(
    name='s3.1',
    kind=KIND_SEMANTIC,
    hypothesis='s3 produced 20 FP and 5 FN on dev, so FP outnumber FN four to '
               'one. Nearly every FP names a real residual imperfection in or '
               'near the patched method, and not the REPORTED fault: Math-59 '
               'cites signed-zero max semantics against a reported reversed '
               'result (6 FP, all unanimous), Math-30 cites an int overflow at '
               'n=50000 in a different product from the one the failure names '
               '(3 FP, all unanimous), Closure-86 cites a TODO about an ideal '
               'specification, and Closure-62 and Math-73 cite a wrong value '
               'the patch itself introduces elsewhere. Math-93 and Lang-26 '
               'wrote "none found" in SURVIVING INPUT and still voted '
               'OVERFITTING, using SIBLING PATHS to restate the patched '
               'method. So bind the whole judgement to the observable in '
               '<observed_failure>: add a REPORTED FAULT section, exclude the '
               'four out-of-scope survivors, forbid the patched method as its '
               'own sibling, and force CORRECT when neither section names '
               'anything. Two of the five FN are protected by name, because a '
               'stricter form invites them: Math-2 reproduces the reported '
               'fault without reaching the changed lines, and Closure-73 '
               'corrects one end of a bound and breaks the other.',
    task=S3.task,
    instruction=S3.instruction.replace(
        # The form now starts from the reported observable, not from the
        # model's own idea of what the method ought to do. 20 FP named a
        # wrong value the reported failure never mentions.
        "Answer in exactly these five sections, each one heading followed by"
        " at most three sentences:\n\n"
        "ROOT CAUSE: which condition makes the original code compute the wrong"
        " value.\n",
        "Answer in exactly these six sections, each one heading followed by"
        " at most three sentences:\n\n"
        "REPORTED FAULT: read the <observed_failure> block, and state the"
        " fault it reports as a condition — which input, which computed step,"
        " and how the returned value is wrong. Copy that observable from the"
        " failure message. Do not widen it to what an ideal implementation"
        " would do.\n"
        "ROOT CAUSE: which condition makes the original code compute the wrong"
        " value.\n",
    ).replace(
        # The four exclusions are the four FP shapes, in order of their count
        # on dev: 6 Math-59, 3 Math-30, 1 Closure-86, and Closure-62 twice
        # with Math-73 and Math-2 alongside.
        "SURVIVING INPUT: one concrete input that still gets a wrong value out"
        " under this patch, with the wrong value named — or the words 'none"
        " found', and then why the condition can no longer hold.\n",
        "SURVIVING INPUT: one concrete input that still meets the REPORTED"
        " FAULT condition under this patch, with the wrong value named — or"
        " the words 'none found', and then why the condition can no longer"
        " hold. It must go wrong in the same way, at the same computed step,"
        " as the reported failure. Four things do not count: a separate defect"
        " the original code also had and the reported failure never names; a"
        " wrong value the patch introduces in some other behaviour; a"
        " shortfall against an ideal specification, a library's semantics or a"
        " TODO; and an extreme input that breaks a different expression from"
        " the one the reported failure names.\n",
    ).replace(
        # Math-93 and Lang-26 wrote 'none found' and then voted OVERFITTING on
        # a sibling that was the patched method itself.
        "SIBLING PATHS: whether any other function in the reachable region, or"
        " any other reader of the same state, still carries the unpatched"
        " computation.\n",
        "SIBLING PATHS: whether any other function in the reachable region, or"
        " any other reader of the same state, still carries the unpatched"
        " computation and can produce the REPORTED FAULT. Name it by a"
        " concrete call. The patched method is not a sibling path: a residue"
        " inside it belongs in SURVIVING INPUT.\n",
    ).replace(
        # The two protected FN shapes are named here rather than in a section,
        # so the sections stay at three sentences each.
        "Judge behaviour, not style. Answer OVERFITTING only when SURVIVING"
        " INPUT or SIBLING PATHS names something concrete.\n\n",
        "Judge behaviour, not style. The patched method does not have to be"
        " perfect, and it does not have to be the fix you would write. It has"
        " to stop producing the reported fault.\n\n"
        "Before you write 'none found', check two cases that do count. First,"
        " an input that reproduces the reported fault without ever reaching"
        " the changed lines. Second, a bound that the patch corrects at one"
        " end and leaves wrong, or newly wrong, at the other end.\n\n"
        "Answer OVERFITTING only when SURVIVING INPUT or SIBLING PATHS names"
        " something concrete that meets the REPORTED FAULT condition. If"
        " SURVIVING INPUT says 'none found' and SIBLING PATHS names no such"
        " path, the answer is CORRECT.\n\n",
    ),
))


S3_2 = register(PromptVersion(
    name='s3.2',
    kind=KIND_SEMANTIC,
    hypothesis='s3.1 cut FP from 20 to 13 and broke the unanimity of the '
               'Math-59 cluster, but FN rose from 5 to 17 and dev F1 fell '
               'from 0.731 to 0.595. Recall fell from 0.872 to 0.564, so the '
               'turn overshot. The dev FN log names three wording faults, and '
               'all three are mine. First, SIBLING PATHS asked a sibling to '
               'produce THE REPORTED FAULT, which no sibling can do, because '
               'the reported fault is one input in the patched method: that '
               'dismissed every real sibling, and it cost Math-53 (Complex.add '
               'guarded, subtract and multiply not), Lang-41 (one overload), '
               'Chart-12, Chart-3 and Lang-60. Second, "at the same computed '
               'step" is tighter than an overfitting patch has to be. Third, '
               'the CORRECT rule had no counterweight, so a patch keyed on the '
               'reported symptom passed whenever no single input could be '
               'named — Math-82 twice, Math-73 twice, Closure-38 and Chart-26, '
               'all unanimous. So repair the sibling clause, widen the '
               'survivor to the same KIND of wrong value, and add a PATCH '
               'SCOPE section that asks whether the patch keys on the property '
               'or on the reported input. Keep all four exclusions, because '
               'they are what cut the FP.',
    task=S3_1.task,
    instruction=S3_1.instruction.replace(
        "Answer in exactly these six sections",
        "Answer in exactly these seven sections",
    ).replace(
        # The recall loss is concentrated in patches that key on the reported
        # symptom and leave the property alone. No single input names them, so
        # the form has to ask the question directly.
        "PATCH EFFECT: what the patch changes, in behavioural terms.\n",
        "PATCH EFFECT: what the patch changes, in behavioural terms.\n"
        "PATCH SCOPE: name the property that makes the computation wrong — the"
        " property, not the reported input. Then say whether the patch keys on"
        " that property, or on the reported input itself: its exact value, its"
        " type, its length, its sign, or the one branch the failing test"
        " takes.\n",
    ).replace(
        # 'the same computed step' asked the survivor to fail at the same line.
        # An overfitting patch usually moves the fault, it does not keep it in
        # place.
        " hold. It must go wrong in the same way, at the same computed step,"
        " as the reported failure.",
        " hold. It must get the same kind of wrong value out of the same"
        " computation as the reported failure. It does not have to fail at the"
        " same line, and it does not have to resemble the reported input.",
    ).replace(
        # A sibling cannot reproduce the reported fault, because the reported
        # fault is one input in the patched method. s3.1 asked it to, so every
        # sibling was dismissed.
        "SIBLING PATHS: whether any other function in the reachable region, or"
        " any other reader of the same state, still carries the unpatched"
        " computation and can produce the REPORTED FAULT. Name it by a"
        " concrete call. The patched method is not a sibling path: a residue"
        " inside it belongs in SURVIVING INPUT.\n",
        "SIBLING PATHS: whether any other function in the reachable region, or"
        " any other reader of the same state, still carries the unpatched"
        " computation and can get the same kind of wrong value out. A sibling"
        " never reproduces the reported input, so do not ask it to. Look for"
        " another overload, another member of the same family, another reader"
        " of the same state, and another caller path that skips the changed"
        " lines. Name it by a concrete call, and do not name the patched"
        " method: a residue inside it belongs in SURVIVING INPUT.\n",
    ).replace(
        "Answer OVERFITTING only when SURVIVING INPUT or SIBLING PATHS names"
        " something concrete that meets the REPORTED FAULT condition. If"
        " SURVIVING INPUT says 'none found' and SIBLING PATHS names no such"
        " path, the answer is CORRECT.\n\n",
        "Answer OVERFITTING when either of these holds. First, SURVIVING INPUT"
        " or SIBLING PATHS names something concrete of the same kind as the"
        " REPORTED FAULT. Second, PATCH SCOPE says the patch keys on the"
        " reported input rather than on the property, because the property"
        " then still makes other inputs wrong even where you cannot name"
        " one.\n\n"
        "Otherwise answer CORRECT. A patch that keys on the property is"
        " CORRECT even when it is narrower, uglier or later than the fix you"
        " would write.\n\n",
    ),
))


register(PromptVersion(
    name='s3.3',
    kind=KIND_SEMANTIC,
    hypothesis='Two turns of the heavy form bought no precision at all. Dev '
               'precision reads 0.630 for s3, 0.629 for s3.1 and 0.595 for '
               's3.2, while recall reads 0.872, 0.564 and 0.641. So the only '
               'thing the added sections moved was recall, and they moved it '
               'down. The s3.2 dev log also shows PATCH SCOPE handing the '
               'Math-59 cluster straight back: all six returned as FP, because '
               'the model reads "the patch repairs only the a > b branch" as '
               'keying on the reported input. The FP set is the same under '
               'every form — Math-59 six times and Math-30 three times, nine '
               'of the 17 — and every one of those names a wrong value of a '
               'different kind from the reported one. So drop the whole '
               'apparatus and go back to the s3 form with ONE sentence added: '
               'the patch answers for the reported kind of wrong value, and '
               'not for the method perfection. This is the narrowest change '
               'that addresses 10 of the 20 base FP, and it touches nothing '
               'that recall depends on. Its parent is s3, not s3.2, and the '
               'dev logs of s3.1 and s3.2 are the reason.',
    task=S3.task,
    instruction=S3.instruction.replace(
        "Judge behaviour, not style. Answer OVERFITTING only when SURVIVING"
        " INPUT or SIBLING PATHS names something concrete.\n",
        "Judge behaviour, not style. Answer OVERFITTING only when SURVIVING"
        " INPUT or SIBLING PATHS names something concrete.\n\n"
        "Do not count a wrong value of a different kind from the reported"
        " one. A separate defect that happens to sit in the patched method, an"
        " expression that breaks only at an extreme size the reported failure"
        " never reaches, and a shortfall against an ideal specification, a"
        " library's semantics or a TODO are all a different kind. The patch"
        " answers for the reported fault, and not for the perfection of the"
        " method.\n",
    ),
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
    """The chat-completion messages for one candidate patch."""
    v = resolve(name)
    return [
        {'role': 'system', 'content': SYSTEM},
        {'role': 'user',
         'content': '\n\n'.join([v.task, evidence_text, v.instruction])},
    ]
