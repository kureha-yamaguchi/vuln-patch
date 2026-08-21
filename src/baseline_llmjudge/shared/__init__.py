"""What both datasets use.

A module belongs here when the two dataset baselines must not be able to
disagree about it. Six do:

  * `verdict.py`    — the output space: one bit, two classes, the parse rule,
                      the three vote rules, and what an unparsed sample counts
                      as.
  * `scoring.py`    — the confusion matrix, the headline rule, and the printed
                      summary form.
  * `budget.py`     — tokens to cost, with the price and cache rules.
  * `blocks.py`     — the evidence block type, and the rule that joins blocks
                      into one prompt string.
  * `version.py`    — the prompt-version shape, and the output contract the
                      model must satisfy.
  * `provenance.py` — which commit produced a record.

Nothing here knows about Defects4J or about Project Zero. A module that needs
to know belongs in one of the two dataset subpackages instead.
"""
