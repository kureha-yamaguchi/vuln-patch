"""One rendered evidence section, and the rule that joins them.

Every dataset renderer produces a list of `Block` and joins it with
`evidence_text`. The join rule lives here, in one function, because two
renderers with two join rules would send two differently shaped prompts and
call the difference a result.

`origin` records where a block's text came from. Three values are in use:

  * `reused`        — the pipeline's own method produced this text. A copy
                      would drift the moment the harness prompt changed, so a
                      guard test asserts the equality.
  * `rendered`      — the baseline re-renders a pipeline section, because the
                      pipeline fuses facts with harness instructions inside
                      that one section. The facts are the same, in the same
                      order, from the same fields.
  * `baseline_only` — the pipeline has no counterpart for this block. Marking
                      it `rendered` would claim a parity that does not exist.
"""
from dataclasses import dataclass
from typing import List


@dataclass
class Block:
    """One rendered evidence section."""
    name: str
    origin: str      # 'reused', 'rendered' or 'baseline_only'
    text: str

    @property
    def chars(self) -> int:
        return len(self.text)


def evidence_text(blocks: List[Block]) -> str:
    """The blocks joined exactly as the pipeline joins its sections."""
    return '\n\n'.join(b.text for b in blocks)
