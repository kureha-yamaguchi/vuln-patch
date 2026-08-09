# Pre-registration — reportable patched-only exceptions (+ probe-after-mutation) — 2026-08-09

**Status:** registered BEFORE any build. Follows 8.35/8.36: reach is measured
saturated; the recall lever is oracle-side. Two mechanisms, one replay study.

**Station:** relation synthesis prompt + relation screen lints
(`src/java/relations/relation_synth.py`, `relation_screen.py`,
`src/java/parsing/java_source.py`). The counting wrapper is NOT changed:
the new shape rethrows with a `violated` message, so the existing counter
and all downstream attribution see a normal firing.
**Failure mode addressed:** a relation whose valid-by-construction input
makes the PATCHED build throw is today silenced twice (mandated
catch-and-return, then the wrapper's rejection rule) — the exact Chart-19
FORK-ORACLE mechanism; 17/381 overfit patches textually add a throw (lower
bound).

## Mechanism A — reportable patched-only unexpected exceptions

New relation-body shape (two-tier catch), mandated by the synthesis prompt
and enforced by a mechanical lint:

- Tier 1 (setup): build inputs/receivers — `try { ... } catch (Exception e)
  { return; }` (unchanged: setup failure is a rejection).
- Tier 2 (probe): the call(s) on the PATCH-CHANGED class only —
  `try { probe } catch (Exception e) { throw new RuntimeException(
  "relation <name> violated: unexpected " + e.getClass().getName() +
  " on valid-by-construction input: " + e.getMessage()); }`.
- Relations whose CONTRACT is an expected rejection keep the targeted
  `catch (ExpectedException ok) {}` shape (draw-04's convicting shape).

Precision guards, all existing and mechanical: (1) buggy-side screening —
a relation whose tier-2 rethrow fires on the buggy build is handled by the
existing fired-on-buggy logic (not kept silent-tripwire; fires-on-both is
already a dismissal fact); (2) replay attribution facts unchanged; (3) the
judge sees the exception class + message as the firing message.

## Mechanism B — rejection-probe-after-mutation

Prompt standing-strategy extension (+ lint): any rejection-contract probe
must be re-run AFTER each receiver-state mutation in the body, and the
rejection-independence companion applies to harness-channel oracles, not
only relations. NOT replay-testable (changes invention); validated on the
next fresh roll under the same gates as every prompt mechanism.

## Gates (registered now, judged after)

- **G-P (precision, hard stop):** on the archived CORRECT legs replayed
  under the Mechanism-A rewrite (varbase: Math-65-c ×3, Math-2-SOFix-c ×3,
  Chart-26-c ×3; invdiv Math-65-c ×3; diffcov_reach Math-65-c), ZERO new
  patched-only tier-2 firings that survive the existing attribution facts.
  Any survivor = the mechanism does not ship as-is.
- **G-R (recall, the point):** count archived OVERFIT legs converted
  quiet→patched-only-firing by the rewrite. Chart-19 legs are the
  prediction (8.35); any other conversion is recorded upside.
- **G-F (no regression):** full pytest green; the frozen guard fixtures
  (38-row `tests/fixtures/correct_dismissals.jsonl`, 67-row
  `docs/replay/backtrack/guard_population.json`) decide identically
  through unchanged decision code.
- **Clean-leg hard-stop:** unchanged, applies to the next live roll.

## The replay study ("would the last few runs pass")

Offline, execution-only, zero-LLM (phase 1): for every leg of the three
archived suites (invdiv 9, varbase 15, diffcov_reach 5 — 29 legs):

1. Extract that leg's KEPT relations (its own run artifacts only — no
   cross-run pooling; nothing from this study is banked for future runs).
2. Mechanically rewrite tier-2 catches: ONLY around calls whose static
   owner / receiver type is the patch-changed class of THAT leg's patch.
   If the transform cannot confidently isolate the probe call, SKIP the
   relation and record why (fail-closed; skips are reported, not silent).
3. Rebuild the leg (defects4j checkout + patch), screen rewritten
   relations on BUGGY, replay on PATCHED — the shipped execution path.
4. Report per relation {fires-buggy, fires-patched, both, skipped} and per
   leg: converted? Under the identical transform for correct and overfit
   legs (label-blind mechanics; labels used only to SCORE, as always).

Phase 2 (small LLM spend): new patched-only firings go through
`verifier_replay.py` so the answer is "would the VERDICT flip", not just
"would something fire".

**Bound stated up front:** replaying archived relations tests Mechanism A
only. Mechanism B and the un-invented relations of past draws are outside
what a replay can show; the next fresh roll carries them.
