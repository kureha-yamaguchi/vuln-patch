# The bypass qualifier + the isolation re-aim — the build (2026-08-10)

Implements "Pre-registration round 2" at the bottom of
`docs/math65-formula-read-2026-08-10.md`, registered after plan item 8.42's
RESOLVED note. Code plus tests only. The gates G-B1/G-B2 need an archived
replay and are NOT run here; G-B3's pytest half is run and green below.
Nothing committed, nothing pushed to the VM, no judge or verifier prompt
touched.

## Stations this build targets

| Station | Module | Target / Failure mode |
|---|---|---|
| Judge decision — the terminal gate ladder | `src/java/relations/judge_decision.py` (`direction_confirmed_bypass`, `adjudicate`) | **Target:** decide, mechanically, which firings skip 5C/6B/6C. **Failure mode fixed:** the direction-confirmed exemption was unconditional, so a firing that also carries `[fact:rate-indiscriminate]` — one that fires on the buggy build's trigger inputs *because* it fires on nearly every input — skipped every value gate by design, and with them the shadow-isolation reading that lives behind them. |
| Buggy-side replay — the isolation hook | `src/java/run.py` ~3490-3600 | **Target:** measure the buggy-side value of the relation that actually convicts. **Failure mode fixed:** the hook isolated `sorted(_fired_ids)[0]`, so in `gs1_isolation_20260810_175503` leg 03 both KEPT convictions had no `isolated-buggy-replay` event at all — the single target chosen by name was a different check. |
| Evidence facts — the isolation arithmetic | `src/java/relations/evidence_facts.py` (`isolated_value_reading`, `isolation_reading_fact`) | **Target:** produce a reading for an AGREEMENT check. **Failure mode fixed:** gs1's two `fired` isolations both died `ambiguous` with real numbers in hand: `chi-vs-rms` prints two observables and no `expected=`, so the closeness reading had no yardstick and the identity reading had nothing to say about differing values. |

## Where the bypass lived

**Not in `run.py`.** `run.py` only computes the flag —

```python
_dirconf = (getattr(rel, 'screen_direction', None) == 'confirmed')   # ~4497
...
is_direction_confirmed=_dirconf)                                     # ~4545
```

— and hands it to the one shared judge entrypoint. The bypass itself is three
lines of `adjudicate` in `src/java/relations/judge_decision.py`: the
`cycle6_gates_entry` audit event, then `if not is_direction_confirmed:` around
the `(_terminal_identical_gate, _indiscriminate_rate_gate,
_confirmed_fires_on_both_gate)` loop. That single `if` is the whole leak, and
it is the only line build A changes. The second judge site (`run.py` ~4116)
and `verifier_replay.py` never pass the flag, so they were already on the
value path and are untouched.

The screen-time half of the exemption (`relation_screen.py`, where
`direction == 'confirmed'` also exempts a relation from the 20% out-of-domain
cap) is deliberately NOT touched: the desk read found no mechanical separator
at screen time (§7 item 4), and this build works at judgement time instead.

## The predicate, exactly as implemented

`judge_decision.direction_confirmed_bypass(is_direction_confirmed,
evidence_text) -> (bypass, reason)`:

```
not direction-confirmed                      -> (False, None)
direction-confirmed, no rate-indiscriminate  -> (True,  "…skipped by design")
direction-confirmed AND rate-indiscriminate  -> (False, "…routing through …")
reading the evidence raised                  -> (True,  "…the bypass stands")
```

with the rate flag read as

```python
indiscriminate = 'rate-indiscriminate' in fact_tags(evidence_text)
```

Three properties worth stating:

* **TAG-ONLY.** `rate_profile`'s prose fallback is *not* consulted. Only
  evidence carrying the literal `[fact:rate-indiscriminate]` tag reroutes;
  untagged text (older runs, replayed fixtures, notes written elsewhere)
  keeps the old bypass byte for byte. Rerouting on a keyword match is the
  one way this could move a firing the measurement did not.
* **Fails toward today.** Any exception leaves the bypass in place.
* **No dismissal is added anywhere.** The rerouted firing meets the three
  shipped gates, each with its existing family-duty escape. A firing whose
  measured buggy rate is 75% — the Math-65 shape — clears 6B's 95% intrinsic
  bar test and survives without the judge being asked anything at all
  (`test_the_reroute_adds_no_dismissal_of_its_own`). Rerouting is not a drop;
  it is permission for the value facts to be read.

## The event wording

One event, unchanged in name and place: `method='cycle6_gates_entry'`,
`target=<oracle id>`. `output` is `skipped` or `running` as before. The
rerouted `reason` names both flags, verbatim:

> direction-confirmed AND [fact:rate-indiscriminate] — the bypass does NOT
> apply: a check that condemns the known-broken build on a large share of
> random inputs also fires on the trigger inputs by construction, so the
> direction confirmation is not evidence that it is aimed at the defect.
> Routing through the ordinary value path (5C -> 6B -> 6C), exactly as a
> non-direction-confirmed firing; running 5C -> 6B -> 6C; base verdict
> ok=True, fd_prior=None

The two unqualified readings keep their shipped wording exactly:
`direction-confirmed firing (mechanical buggy-build catch) — 5C/6B/6C all
skipped by design`, and the plain `running 5C -> 6B -> 6C; base verdict
ok=…, fd_prior=…`. So a trace tells three states apart that used to be two:
bypassed, rerouted, and never-confirmed.

## The isolation re-aim

**Targeting.** The hook now loops:

```python
for _iso_target in sorted(_fired_ids):
```

one `replay_input_isolated` (one Jazzer run) per relation the firing names,
one `isolated-buggy-replay` event each — the event's `detail` gains
`'targets': sorted(_fired_ids)` so a trace shows the full target list beside
each reading. The fact is attributed to the relation it was measured on
(`_irf(_iso_read, {_iso_target})`), not to every id the firing mentions.
The loop `break`s on the first dismissing reading: the firing is dropped at
that point, so the remaining ids would cost a JVM run each for evidence
nothing will read. There is still exactly ONE `drop_reasons.append` in the
block and exactly one `isolation_dismisses` call.

**The new readings.** `ISOLATION_READINGS` gains `buggy-differs`;
`_ISOLATION_DISMISSING` is unchanged (`identical`, `patched-closer`). The
agreement branch runs when the two messages share **two or more observable
keys** and **no shared `expected=` key** (a `tol=` does not count — it is not
a yardstick for closeness):

* every shared observable equal on both builds -> `identical`,
  dismissal-eligible, the same reading and the same dismissal as before: the
  patch changed nothing this check looks at;
* any of them differing -> `buggy-differs`, **not** dismissal-eligible, with
  a fact under `[fact:isolation-reading]` naming every shared observable and
  both builds' values, and saying in its own words that with no expected
  value stated there is no yardstick for which build is closer, so the
  arithmetic settles nothing on its own.

One shared observable and no yardstick stays `ambiguous`, exactly as before —
a single number that differs says nothing without something to compare it to.
A stated `expected=` still takes the shipped closeness path unchanged,
including its refusal when more than one observable differs.

Supporting refactor: `compare_fired_values`'s per-key multiset-with-floor
comparison is extracted as `_key_values_match` and reused by the agreement
branch, so a key is compared identically in both places. Semantics are
unchanged (the extracted body is the original loop).

## Tests

`tests/test_bypass_qualifier.py` (new, 26 cases): the predicate on all three
combinations plus the tag-only and fail-open properties; the same three
combinations end to end through `adjudicate` (doubly flagged meets 6B;
rerouting alone drops nothing; the family-duty escape survives; singly
flagged still skips a blob 6C would drop; non-confirmed unchanged); the
agreement reading (identical pairs, differing pairs, one-key stays old
behaviour, reference-keys-alone, tol-is-not-a-yardstick, every reading name
declared); and the hook targeting, pinned by reading `run.py` (the loop
exists, `sorted(_fired_ids)[0]` is gone from the code, one event per target,
one dismissal path).

Two existing tests pinned the behaviour this build deliberately changes and
were re-pointed, not deleted:
`test_cycle6_enforcement.py::test_gates_are_skipped_when_the_firing_is_direction_confirmed`
and
`test_cycle6_observability.py::test_adjudicate_records_that_the_gates_were_skipped`
now assert the bypass on a SINGLY flagged firing (their old evidence blobs
were rate-indiscriminate, i.e. the leak itself), and the observability file
gains `test_adjudicate_records_the_rerouted_bypass_and_names_both_flags`.

Full suite: `uv run pytest -q` -> **1004 passed, 7 skipped** (baseline before
this build: 977 passed, 7 skipped). That is G-B3's pytest half; the "no
judge-prompt changes" half is true by construction — no file under
`relation_synth.py` / `relation_verifier.py` prompt text was touched.

## What the replay study must enumerate

The replay runs offline against archived traces; nothing here needs the VM
until a live roll is approved.

**G-B1 — do the archived Math-65 FP convictions convert?** For each of the 9
legs (varbase 01 ×2 relations, mechb 07-09, stack_confirm 04-06, gs1 01-03),
for each KEPT conviction, enumerate from the leg's `trace.md`:

1. `screen_direction` — was the firing direction-confirmed at all (i.e. was
   it on the bypass)? A conviction that already ran the gates is out of
   build A's reach by construction and must be listed as such.
2. Does its `concrete_evidence` carry the literal
   `[fact:rate-indiscriminate]` tag? Prose-only blocks do NOT reroute; count
   them separately, because they are the population that would need the
   qualifier widened.
3. **Which gate would act, and on what.** Rerouting alone converts nothing.
   Record, per conviction: the measured buggy-side rate (6B drops only at or
   above 95% — leg 03's are 51%/83%, i.e. BELOW the bar, so 6B will not act);
   whether `[fact:fires-on-both-confirmed]` is present and what value verdict
   rides with it (6C drops only on `identical`); and whether
   `terminal_profile` reads the evidence as `identical-on-both` (5C). A
   conviction that is doubly flagged but carries no value fact will NOT
   convert, and that outcome must be reported as a shortfall of the
   mechanism, not hidden inside a leg-level pass/fail.
4. The family-duty answer each acting gate would need. Every gate escapes on
   YES, and the answer is an LLM call — so a replay that cannot reproduce it
   must say so rather than assume NO.

**G-B2 — is any genuine catch lost?** Enumerate EVERY archived TP conviction
(Chart-19 ×8 across invdiv/varbase/mechb/stack, Lang-41 ×3, Math-2 and
Chart-26 correct-side legs, plus the 67-row guard population and the
`cases228` keep-findings) and split them three ways:

* **not direction-confirmed** — untouched by construction; list and stop.
* **direction-confirmed, no rate tag** — bypass unchanged; list and stop.
* **direction-confirmed AND rate-indiscriminate** — the only population at
  risk. Each must survive the value path. For each, record the same four
  facts as G-B1 and show which gate could act. Chart-19's convicting relation
  measures buggy 20000/20000 = 100% and IS in this population by rate; it
  survives 6B today via family-duty YES (it asserts the failing test's own
  observable) and that escape must be shown, not assumed. A single TP that
  the value path would drop = redesign, not a threshold tweak.

**Also reported, per the prereg:** whether Chart-7-c's and Chart-26-c's
convictions are doubly flagged (in reach of build A) or not — if not, the
valid-by-construction probe becomes the named next design, because those
convictions flow through complete evidence that the judge keeps and no
evidence-completeness mechanism can touch them.

**Build B's own reach** is separately observable: after the re-aim, every leg
whose firing named more than one oracle should carry one
`isolated-buggy-replay` event per named relation. Count the legs where the
convicting relation now has an event and did not before — that number is the
whole of the re-aim's effect, and it is measurable without any judgement
call.
