# Item 2b — did one of last week's rules cause the new false accusations?

The question that could reorder the batch: Math-39 was accused for the first time
ever (roll A only), and Lang-60's and Math-73's correct patches were accused in
roll B only. If a rule we added last week caused any of them, that fix jumps the
queue.

Evidence: the two archived paired runs. No new runs, no LLM calls.

## Answer: no. Not one of them.

None of last week's decision rules fired on any of the three, in either roll:

| case | 5B rescue | 6B drop | 6C keep |
|---|---|---|---|
| Math-39 correct patch (accused roll A) | 0 | 0 | 0 |
| Lang-60 correct patch (accused roll B) | 0 | 0 | 0 |
| Math-73 correct patch (accused roll B) | 0 | 0 | 0 |

Nor did a new *fact* fabricate anything. Chart-26 — the leg where a fabricated
fact previously accused a correct patch — was accused in roll A with a
`was DIVERTED` fact present and cleared in roll B without one, which looks
incriminating until you read the text. It says the replay "says NOTHING about
whether the check would fire on the buggy build — no attribution in either
direction. In particular the absence of a firing here is NOT evidence the patch
caused anything." That is the corrected, conservative wording doing exactly its
job: declining to supply evidence. The earlier fix held.

**So the batch does not need reordering.** Items 3, 7(i), 7(ii) and the
missing-field hardening proceed as planned.

## What did cause them

Two things, in sequence.

**First, upstream luck decides whether any complaint gets raised at all.**
Lang-60's correct patch is the clean demonstration. Every station before the
reviewer ran an identical number of times in both rolls — rule writing 2, harness
generation 9, harness attempts 13, screening 1, replay 11, fuzzing 5. Then the
reviewer was called **0 times in roll A and 2 times in roll B**. In roll A no
complaint was ever raised, so the patch was cleared by default. In roll B two
complaints surfaced and it was accused.

Note that more complaints does not mean more accusations. Math-73's correct patch
drew 7 complaints in roll A and stayed clean; it drew 1 in roll B and was
accused. Chart-26 drew 1 and was accused; it drew 3 and was cleared. Which
complaint gets raised matters; how many does not.

**Second, the reviewer then accepts an over-tight check on its merits.** Every
one of these accusations is the reviewer answering "this check is trustworthy" on
a check that is in fact too strict — that unused buffer slots in a string builder
can never contain a null character, that a solver must never evaluate its
function outside the requested interval, that a root-finder must return exactly
the endpoint. Plausible-sounding, and wrong.

## The structural finding underneath

While reading the verdicts I noticed 7 of the 8 accusing verdicts cited nothing.
Measured across both rolls, all 30 cases:

| verdict direction | cited | uncited | share uncited |
|---|---|---|---|
| accuse (the check is trustworthy) | 9 | 83 | **90%** |
| dismiss (the check is a false alarm) | 98 | 6 | **6%** |

The system demands a verbatim quote from the shown material before it will let a
patch off the hook, and demands essentially nothing before condemning one. This
is deliberate — the prompt says, in `relation_verifier.py`, "For a SOUND verdict,
CITATION: NONE is fine."

**This explains the central puzzle of the whole campaign.** Two weeks of
precision work moved the false-accusation count by exactly zero — 5 in both
rolls. Every mechanism built (the 5B rescue, the 6B and 6C gates, the terminal
gates) acts on the *dismissal* side of the decision. The accusation side has no
evidentiary requirement at all, so none of it could reach the false accusations.

## But the obvious fix is a bad one — checked before proposing it

The tempting conclusion is "require citations on accusations too". Measured
against whether accusing was the right call:

| accusing verdicts on… | cited | uncited | share cited |
|---|---|---|---|
| fake patches (accusing was correct) | 8 | 60 | 12% |
| good patches (accusing was wrong) | 1 | 23 | 4% |

Requiring a citation would prevent **23** wrong accusations at the cost of
**60** correct catches. That is a catastrophic trade, and it would undo the one
side of the ledger that has actually been improving.

The 12%-versus-4% gap points the right way — cited accusations are somewhat more
likely to be correct — but it rests on 9 cited accusations in total. That is far
too thin to gate a decision on, and anyone who built a rule on it would be
fitting noise.

Recorded as: a real structural asymmetry, correctly diagnosed as the reason
precision work has not landed, with the obvious remedy explicitly rejected on
measured evidence. Any future precision lever has to act on the accusation side
*without* using citation presence as the trigger.

## Status of the batch

Unchanged. Nothing here jumps the queue. Proceed to 2c, 2d, 2e.
