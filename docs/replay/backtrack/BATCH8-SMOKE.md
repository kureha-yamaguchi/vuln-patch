# Cycle-8 batch smoke — RESULT. It found a live defect in 8.4.

`batch8_20260802_123712`, git `2cc051f`, two formatter/printer legs
(Closure-62_c, Closure-38_o). 343,328 tokens.

## Outcome table — RECORDED, NOT SCORED

```
| Closure-62 | correct | semantic | TN |
| Closure-38 | overfit | semantic | FN |
TP=0 FN=1 FP=0 TN=1
```

Rule 16 applies: the only available comparison (c84 at `298d9d9`) differs from
this run by the entire batch, so a verdict difference would measure accumulated
batch effect, not regression. **These verdicts are recorded, not scored.** Two
legs cannot support a rate claim in any case.

## What passed

**The build runs end to end** with all four batch members in place.

**8.7 repair provenance fired live** — 1 `FROM REPAIRED ATTEMPT` marker on
Closure-38. First live occurrence outside the rung-2 smoke.

**8.4's prompt half works.** Source-level lint over the 13 generated harnesses:

```
  Closure-62_c : 7 sources, 6 normalize, 0 flagged by the lint
  Closure-38_o : 6 sources, 3 normalize, 0 flagged by the lint
```

**Gate 0c2 correctly silent** — 0 rejections, and the source-level lint agrees
there was nothing to reject. Silence here is compliance, not unreachability.

## What FAILED — the finding

**8.4's raw-vs-pinned comparison is starved in production.** The Raw keys are
emitted, recorded, and then **stripped before the comparison ever sees them.**

`execution/oracle_strength.py:65` — `exception_headlines(output, max_len=200)`:

```python
line = line[:max_len] + ('…' if len(line) > max_len else '')
```

That output becomes `fired_all` (`run.py:2341`), which becomes `fired`
(`run.py:2441`), which is the exact string passed to `fired_value_vs_trusted`
(`run.py:3120`). So a 200-character cap sits between the alarm and the consumer.

**8.4's message format puts the Raw keys LAST**, so they are the first thing the
cap removes. Running the shipped extractor on this run's real output:

```
  Closure-62_c
    headlines the comparison would receive : 4
      reporting a Normalized value         : 4
      ...still carrying actualRaw=         : 1
      truncated at the 200-char cap        : 2
```

**3 of 4 normalizing firings reach the comparison without their Raw keys.** The
one that keeps them is 198 characters — two under the cap — and it is the only
firing on which the comparison returned anything but `unknown`:

```
  [getsource-line-roundtrip]  actualRaw -> 'null'   raw_value_vs_pinned=differs
```

And the single trigger-lift note delivered on this run took the `unknown`
branch, live: *"no numeric value could be compared"*.

## Why every prior check missed it

Each piece was verified in isolation and each piece is correct:

* the prompt emits the keys — verified at source level, 100%
* the harness records them — verified in the compliance smoke
* the comparison reads them — verified on archived text and synthetic cases
* the lint catches violations — verified on a real harness with its keys stripped

Nobody checked that the keys **survive the journey** between emission and
consumption. Rule 15 in a new shape: I asserted the guard's inputs *exist*, and
never asked whether they *arrive*. An end-to-end run was the only thing that
could have caught this, which is exactly what the smoke is for.

It is also the record-vs-thing lesson a fourth way: the alarm is not the
headline, and the headline is what the consumer gets.

## The fix, and what it must not break

Minimal: stop truncating the string the comparison consumes, while leaving the
displayed/deduplicated headline alone. `max_len` exists for real reasons —
headline de-duplication (`seen`) and prompt size — so raising it globally is not
obviously safe and would need its own measurement.

Options, in order of preference:

1. **Keep the tail.** Truncate the middle, preserving the trailing key/value
   block, so `…Raw=` survives any cap. Smallest blast radius; dedup semantics
   unchanged for short headlines.
2. **Pass a larger cap from the relation-verification call site only**, leaving
   every other caller at 200.
3. Raise `max_len` globally — most invasive, affects prompt sizes everywhere.

**Not applied yet.** This is a pipeline change to a batch that was declared
closed, and applying it means re-running the smoke. That is a decision, not a
detail, so it is recorded here rather than slipped in.

## Status

**The batch does not pass as it stands.** 8.7, 8.8 and 8.12(a) are unaffected —
none has a verdict surface and all behaved. 8.4's prompt and gate work. 8.4's
comparison, the part the pair was meant to guard live, is inert for ~3 of every 4
firings it was built for.
