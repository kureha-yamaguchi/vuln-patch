"""The prompt-version shape, and the output contract.

Both datasets run a two-stage protocol over hand-written prompt versions, and
both demand the same two-class answer. So the shape and the contract live here.

WHAT DOES NOT LIVE HERE. Each dataset keeps its own `SYSTEM` message, its own
registry and its own `version_sha256`. That separation is not tidiness, it is
the reason a recorded digest is stable: `version_sha256` digests `SYSTEM` with
the version's own wording, so one shared `SYSTEM` would tie every recorded
digest of one dataset to an edit made for the other.

`CONTRACT` is shared and it is safe to share. Its VALUE is what the digest
covers, and moving a string between modules does not change its value. Editing
it would change every digest in both datasets, so treat it as frozen.
"""
from dataclasses import dataclass

#: The parseable contract. `verdict.parse` reads the last `VERDICT:` line.
CONTRACT = (
    "End your answer with a final line in exactly one of these two forms, "
    "and write nothing after it:\n"
    "VERDICT: OVERFITTING\n"
    "VERDICT: CORRECT"
)


@dataclass(frozen=True)
class PromptVersion:
    name: str
    hypothesis: str      # what this version's design bets on
    task: str            # text before the evidence
    instruction: str     # text after the evidence
    #: Which pool this version judges. The Defects4J baseline runs two pools,
    #: 'crashing' and 'semantic', and it defaults to the first so that the
    #: four scored crashing versions keep their recorded text and their
    #: recorded digest unchanged. The Project Zero baseline runs one pool and
    #: leaves this at its default.
    kind: str = 'crashing'
