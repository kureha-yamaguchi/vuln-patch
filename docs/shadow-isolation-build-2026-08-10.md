# Shadow isolation — the build (2026-08-10)

Implements the pre-registration at the bottom of
`docs/math65-formula-read-2026-08-10.md`. Code plus tests plus G-S2 only;
G-S1 (the 6-leg replay) needs the VM and is not run here. Nothing pushed,
nothing committed, no prompt touched.

## Stations this build targets

| Station | Module | Target / Failure mode |
|---|---|---|
| Buggy-side replay | `src/java/run.py` ~3487-3570 | **Target:** compute the buggy-side value that the full-harness replay could not. **Failure mode fixed:** a sibling oracle throws first, the JVM dies there, the firing check's own message is never printed, `value_verdict` comes back `unknown`, and `unknown` is the state in which the judge is told to fall back on the check's stated contract — so a check with a javadoc quotation keeps. |
| Oracle isolation transform | `src/java/execution/oracle_mute.py` (`instrument_for_counting`, new `record_firing`) | **Target:** keep exactly one check live in a full harness AND keep its message. **Failure mode fixed:** M-v2 already isolated one oracle, but replaced its throw with a bare tally — it could answer "how often" and never "with what value", which is the only thing this decision needs. |
| Isolated single-input replay | `src/java/execution/fuzz_runner.py` (`FuzzRunner.replay_input_isolated`) | **Target:** compile the isolated variant against the buggy build and run the one crashing input. **Failure mode fixed:** the existing muted ladder silences only the shadows it has SEEN fire and gives up the moment its mute set stops growing (verbatim from `stack_confirm/06`: `pass=1/4 mute_set_size=1 ... -> stop: mute set stopped growing, UNKNOWN kept`). |
| Evidence facts | `src/java/relations/evidence_facts.py` (`isolated_value_reading`, `isolation_dismisses`, `isolation_reading_fact`) | **Target:** state the reading as a computed mechanical fact naming both values. **Failure mode fixed:** the standing rule that every false-positive class is closed by computing a fact into the evidence, never by asking the judge to judge harder. |

## Where the hook sits

`src/java/run.py`, inside the per-firing loop, immediately after the muted
re-replay block and before the firing-input note. It sees everything the
existing replay ladder computed: `_value_verdict` (plain replay), `_mvv_seen`
(the muted ladder's verdict, newly hoisted out of its `try`),
`_breplay_status`, `_shadowed`, `_fired_ids`, `r.artifact_path` (the exact
crashing input), `buggy_cp`, `builder`, `selection.buggy_dir`, `fr`.

Arming condition, verbatim:

```python
_iso_vv = (_mvv_seen if _mvv_seen != "unknown" else _value_verdict)
if (_fired_ids and _iso_vv == "unknown"
        and _breplay_status != "clean"):
```

On a dismissal it appends to `drop_reasons` and `continue`s — the same
terminal shape `defect-family` and `crash-pin` already use. On the
corroborating reading it appends the fact to `_fact_notes` **and** to `evid`,
which is what actually reaches the judge (`concrete_evidence=evid`); several
older sites append to `_fact_notes` alone and are effectively write-only.

## How shadowing is detected

Mechanically, from two values the code already has, and it is a superset of
strict shadowing rather than an attempt to name it exactly:

* **the value verdict is still `unknown`** after both the plain and the muted
  replay — the reading was not obtained;
* **the full-harness buggy replay did not run `clean`** — some check or
  exception ended that run.

Together those two are the signature of a reading that was *prevented*. The
existing `_shadowed` flag (a different oracle fired, disjoint from ours, no
defect exception) is strictly narrower: it misses the case where our own
oracle fired on buggy but its message could not be extracted, and the case
where the replay errored. Both of those are equally unread, and the
prereg's fail-closed rule makes the wider arming safe — an unread firing that
the isolation also cannot read changes nothing.

The one case deliberately EXCLUDED is `_breplay_status == "clean"`: there the
buggy build ran this exact input and this check demonstrably did not fire.
That is the catch signal, not a shadowed reading, and it must not spend a
Jazzer run or acquire a fact.

`_shadowed` is still recorded in the `isolated-buggy-replay` event, so the
trace distinguishes "strictly shadowed" from "unread for another reason".

## What the isolated harness is

`instrument_for_counting(harness_source, target_id, record_firing=True)`:

* every alarm throw except the target's is muted to `;` — tagged siblings and
  untagged `FuzzerSecurityIssue` throws alike, in one shot, so nothing left in
  the harness can end the run first;
* the target's own throw is replaced by
  `{ __vpViolated++; try { <the original throw statement, verbatim> } catch
  (Throwable __vpAlarm) { if (__vpViolated <= 5) System.err.println("[relfire] "
  + String.valueOf(__vpAlarm.getMessage())); } }` — the alarm still gets
  constructed (that is where the value lives) but reports instead of killing
  the JVM. The statement is spliced unchanged, so no message expression is
  re-parsed and no new source transform exists.

Deviation from the prereg's letter, and why: the prereg says "a
SINGLE-relation harness (only that relation's check body)". Isolating the
*body* would strip the harness's shared setup and, worse, change how the
`FuzzedDataProvider` is consumed — a check extracted out of a multi-oracle
harness reads different bytes from the same input file and would compare two
different scenarios. `instrument_for_counting` is what the brief pointed at as
"how M-v2 isolates one oracle in a full harness", and it gives the same
guarantee (one live check, nothing can shadow it) while keeping the input
consumption byte-identical. `measure_single_check` is not reused: it is a
`-runs=N` fuzz over a standalone body, not a one-input replay of an existing
harness.

The `[relfire]` marker is `relation_screen`'s own, so `harvest_relfire_lines`
reads the line with no second format to keep in step.

`replay_input_isolated` returns `(status, message, output)` with status in
`fired` / `silent` / `error` / `isolate_failed`. Only `fired` carries a
message; every other status carries `None`, so a caller can never reason from
a reading it did not obtain.

## The dismissal conditions, exactly

`isolated_value_reading(patched_msg, buggy_msg)` returns one of `identical`,
`patched-closer`, `buggy-closer`, `ambiguous`. The first two dismiss.

**Normalisation is the module's existing one**, not a new one: `_kv_values`
(every `key=value` numeric pair, NaN/Infinity parsed as their IEEE floats),
`_close(a, b) = |a-b| <= 1e-9 * max(1, |a|, |b|)`, `_vals_match` (NaN-safe,
`+Inf != -Inf`), and `compare_fired_values` for identity.

**(i) identical.** `compare_fired_values(patched, buggy) == "identical"` —
which is textual identity, or every shared `key=value` pair matching within
the rounding floor — AND one precondition that comparator does not carry: at
least one shared key must be an OBSERVED key. `expected=` and `tol=` are the
check's own constants and are equal on both builds by construction; a pair of
messages sharing only those would compare identical while nothing the builds
observed had been compared at all. Keys matching `expect` (case-insensitive)
or `^tol` / `^eps` / `tolerance` / `epsilon` are reference keys.

**(ii) patched-closer.** All of:

* exactly one shared single-valued key matches `expect`, and the two builds
  state the same value for it (`_vals_match`) — one shared yardstick, or no
  reading;
* exactly one shared single-valued non-reference key DIFFERS between the two
  builds — one observable, or no reading;
* both observed values and the expected value are finite;
* `|patched − expected| < |buggy − expected|` and the two distances are not
  `_close` to each other.

The mirror (`|buggy − expected| < |patched − expected|`) is **buggy-closer**:
stated as a fact, dismissing nothing. Everything else is **ambiguous** and
changes nothing — no fact, no verdict move, no note.

## The fact wording, verbatim

Tag: `[fact:isolation-reading]` (`ISOLATION_FACT_TAG`). Deliberately NOT a
member of `_TERMINAL_FACT_TAGS`, so `terminal_profile` is unchanged by it.

Shared opening, every version (`<who>` = the fired oracle ids, or
`this check`):

> `[isolation fact] [fact:isolation-reading] the buggy-side reading for <who>
> was SHADOWED in the full-harness replay (a sibling check ended the run
> before it could report), so it was recomputed in ISOLATION: the harness was
> rebuilt against the BUGGY build with every other check silenced — nothing
> left that could stop the run first — and this exact firing input was
> replayed through it. `

**identical** (dismisses):

> `The check fires on the buggy build at this input too, and reports the SAME
> value it reported on the patched build (<key>: buggy <b>, patched <p>). The
> patch did not change what this check measures at this input, so the firing
> reports pre-existing behaviour, not the patch.`

**patched-closer** (dismisses):

> `The check fires on the buggy build at this input too, with a DIFFERENT
> value, and the patched build's value is strictly CLOSER to the check's own
> expected value than the buggy build's is (<key>: expected <e>, patched <p>,
> buggy <b>; patched is <dp> from expected, buggy is <db>). By the check's own
> yardstick the patch moved this observable toward the value the check
> demands, so this firing is not evidence that the patch broke it.`

**buggy-closer** (corroborating; dismisses nothing):

> `The check fires on the buggy build at this input too, with a DIFFERENT
> value, and the BUGGY build's value is closer to the check's own expected
> value than the patched build's is (<key>: expected <e>, patched <p>, buggy
> <b>; patched is <dp> from expected, buggy is <db>). By the check's own
> yardstick the patch moved this observable AWAY from the value the check
> demands. That is corroborating arithmetic only — it decides nothing by
> itself, and whether the check's expected value is the right one to demand is
> still yours to judge.`

**ambiguous**: `isolation_reading_fact` returns `None`. An unresolved
measurement leaves the judge's input byte-for-byte as it was.

Numbers are formatted with `repr(float)` (shortest round-trip), so the fact
quotes the value the harness printed rather than a re-rounded copy the judge
cannot match against the firing.

## No flag

The prereg does not ask for one and the path cannot be armed on a healthy
verdict: it runs only where the value verdict is ALREADY `unknown`, which is
the degraded path, and only the two affirmative readings act. `args.` does not
appear anywhere in the block, and a test pins that.

## G-S2 — no genuine catch killed (offline, before any VM run)

`tests/test_shadow_isolation.py`, section 6. **Result: PASS. Zero rows tripped
either dismissal condition, in any arm.**

| Arm | Population | Rows | Rows the arithmetic could read | Dismissals |
|---|---|---|---|---|
| 1 — no buggy-side reading available | `docs/replay/backtrack/guard_population.json` (genuine catches) | 67 | 67 | **0** |
| 2 — buggy build satisfies the check | same 67 | 67 | **0** | 0 |
| 3 — buggy build satisfies the check | `tests/fixtures/cases228.jsonl`, `gold=keep-finding` | 71 | 2 | **0** |
| 4 — buggy build reads differently | same 71 | 71 | 19 | **0** read as identical |

Arm 1 is the property that matters most in practice: with no isolated reading,
no catch can be dismissed. That is the state the mechanism is in whenever the
isolated harness fails to compile, the input does not fire it, or the values
do not parse — which the desk read expects to be common.

Arm 2 has **zero reach on the guard population, and that is a finding rather
than a pass**: the population stores each row's judge *claim*, truncated to
~150 characters, not the alarm text, so no row carries an expected/observed
pair. The count is pinned at 0 in the test so a fixture change cannot make it
silently vacuous.

Arms 3 and 4 exist because of that gap. `cases228.jsonl` stores
`fired_assertion` verbatim, values included. Arm 3 takes each keep-finding
alarm, moves one observed value onto the check's own `expected` (the shape a
genuine catch has: the buggy build honours the property the patched build
violates) and asserts the reading is `buggy-closer` — corroboration — on all 2
rows that carry a movable pair. Arm 4 covers the identity condition's own
failure mode on all 19 keep-findings that print numbers: a buggy reading that
genuinely differs must never compare `identical`. Both counts (2, 19) are
pinned, so if the fixtures move, these numbers expire rather than drift.

Nothing here reaches a redesign signal. The residual thinness is honest and
worth stating: only 2 archived genuine catches carry a readable
expected/observed pair, so arm 3's evidence is 2 rows deep, not 67.

## Tests

`tests/test_shadow_isolation.py`, 46 tests, no JVM, no LLM, no tokens:

1. the arithmetic — identity, the rounding floor, strictly-closer both ways,
   and nine ambiguous shapes (no buggy message, no expected key, disagreeing
   expected values, two observables differing, equidistant, NaN, Infinity, no
   numbers, reference-keys-only);
2. the fact wording — tag present, both values and the expected named,
   ambiguous states nothing, and the corroborating fact does not read as a
   terminal identical-on-both fact (which would drop the firing it
   corroborates);
3. the transform — on both real archived harness sources
   (`tests/fixtures/harness_sources.json`), message kept, siblings muted,
   braces balanced, marker readable by the existing harvester, `record_firing`
   off by default leaving the M-v2 counting path byte-identical;
4. the composition — stub builder, stub Jazzer: the isolated variant is what
   gets compiled, the message comes back, and every one of six setup failures
   (no builder, no dir, no target, unreadable source, unknown oracle id,
   does-not-compile) plus a raising transform, a non-zero exit and a raising
   Jazzer returns no message and therefore an ambiguous reading. That is G4's
   offline half;
5. the hook — arming condition and ordering pinned by reading `run.py`; no
   flag consulted, exactly one `drop_reasons.append`, no `kept_reason`.

`uv run pytest -q`: **977 passed, 7 skipped** (baseline 931 passed, 7 skipped;
+46 new, none pre-existing broken). G-S3's other two clauses hold by
inspection: no file under `prompts` or any judge/verifier prompt was touched,
and the dismissal path is reachable only from `identical` / `patched-closer`.

## What G-S1 needs from the VM run

Replay the 6 archived Math-65 FP legs (`mechb` 07-09, `stack_confirm` 04-06)
after the flagship sweep, zero-LLM where the harness allows. What to read:

1. **Conversion count.** ≥3 of 6 legs flip FP→TN. The desk read predicts
   mechb/07 (R2), mechb/08 (R1 or R2), mechb/09 (R1), stack/06 (R1) convert;
   stack/04 and stack/05 survive on their family-B relation, whose `expected`
   IS the buggy formula, so the buggy build sits closer and R2 correctly does
   not fire.
2. **The mechanism is live, not idle** (the 8.39 lesson). Every leg's trace
   must carry an `isolated-buggy-replay` event for every firing that previously
   recorded `buggy_replay_status='crashed'` with `value_verdict='unknown'`.
   Zero such firings without one. The event carries `status`, `shadowed`,
   `reading`, `key`, `expected`, `patched_value`, `buggy_value` and the raw
   buggy message, so a leg that converts can be checked arithmetically from
   the trace alone.
3. **The six RMS relations.** `getRMS()` and `optimize()` are byte-identical
   across the two builds, so these compute the same `actual` and the same
   `expected` on both by construction. Every one of them must read `identical`
   in every leg where it appears. Any that reads `ambiguous` instead means the
   isolated harness did not compile or did not fire, and the isolation is not
   reaching the case it was built for.
4. **No catch regresses.** The archived TP legs (Chart-19 ×8, Lang-41 ×3, plus
   the Math-2 and Chart-26 correct-side legs) must still convict and every
   current TN stay TN. A single TP→FN is REJECTED, not a threshold to tune.
   Watch specifically for `isolated-buggy-replay` events on those legs: the
   arming condition is wider than strict shadowing, so they will fire more
   often than `_shadowed` alone would, and each one must resolve to
   `ambiguous` or `buggy-closer`.
5. **Cost.** One extra javac plus one extra single-input Jazzer run per firing
   whose value verdict is unread and whose buggy replay was not clean. If that
   count is large on the TP legs, narrowing the arming condition to `_shadowed`
   is the lever — it costs recall of the mechanism, not safety.
