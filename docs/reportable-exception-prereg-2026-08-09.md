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

---

## Addendum (2026-08-10, registered before the build): documented-exception guard

The 8.38 roll showed the tier-2 rule fires on DOCUMENTED exceptions
(Math-65-c: 14 tier-2 firings, all `OptimizationException` on
"maximal number of iterations exceeded" — an outcome the javadoc
explicitly permits a correct implementation; all five verdicts were
saved by the judge alone). Fix, per the compute-the-fact rule:

- Mechanically extract, from the buggy checkout, the documented
  exceptions of every method of the patched classes + subtype closure
  (`throws` clauses + `@throws`/`@exception` javadoc tags).
- The tier-2 guard (shipped stack-frame guard AND the study transform,
  one shared generator) treats a thrown exception as a REJECTION when
  the innermost patched-class frame's method documents that type (or a
  supertype — runtime hierarchy walk by name). Undocumented → violation,
  as before. One matching prompt sentence.
- Symmetric cost accepted: an overfit patch that adds a throw of a
  documented type on the wrong input reverts to status quo (the
  rejection-independence companion's territory). Fail-closed toward
  precision.

**Gates (judged on a full re-run of the rex replay over all four
archived suites — invdiv, varbase, diffcov_reach, mechb roll — zero
LLM):**
- G-D1 (the fix works): mechb Math-65-c tier-2 firings 14 → 0.
- G-D2 (no recall paid): every previously-observed Chart-19 tier-2
  firing (11 in the original study + the mechb roll's live ones) still
  fires — `indexOf` documents nothing, so any loss is a bug in the
  guard, not the trade-off.
- G-D3: full pytest green; no verifier/judge/gate code touched.

### Gate correction (2026-08-10, after the first validation run `rexd_20260810`)

G-D2 PASSED (all 11 archived Chart-19 tier-2 firings survive, verbatim
relation set). G-D1 came back "0" VACUOUSLY: every mechb relation is
`untouched-already-reports` — the roll's relations were written by the
new prompt already in tier-2 shape, the study transform only retrofits
blanket catches, so the guard never touched the exact relations that
false-alarmed. That is a coverage hole in the shipped fix too: an
LLM-WRITTEN tier-2 catch carries no mechanical documented-exception
protection (prompt sentence + judge only).

Extension, registered before building it: the shared documented check is
also INSERTED at the head of any broad catch whose body rethrows a
`violated: unexpected` alarm (the tier-2 signature) — documented type at
the innermost patched-class frame returns (rejection) before the LLM's
rethrow; undocumented and no-patched-frame cases fall through unchanged.
Applied in the screen (new normalisation branch, recorded per relation)
and in the study transform (new status `doc-guarded`, executed).

Corrected gates, judged on a mechb-only study re-run:
- **G-D1':** mechb Math-65-c doc-guarded relations replay with ZERO
  tier-2 patched-only firings (the live roll produced 14).
- **G-D2':** mechb Chart-19-o doc-guarded relations STILL fire tier-2
  on the patched build (indexOf documents nothing) — the guard must not
  eat the genuine catch while sparing the documented one.

---

## Pre-registration (2026-08-11): the valid-by-construction probe

8.41's Chart-7-c/Chart-26-c FPs (confirmed NOT doubly-flagged in 8.43):
a tier-2/unexpected-exception firing on an input the relation wrongly
declared valid. Fix: before such a firing may convict, replay the SAME
input through the SAME check compiled against the BUGGY build; if the
buggy build raises the SAME exception type at the same patched-class
frame, the input was never valid — record `[fact:input-invalid-on-both]`
and DEMOTE the firing to a rejection (not terminal-dismiss; judge sees
the fact). Different/no exception on buggy → fact states that
(discriminating, conviction stands). Infrastructure failure → no fact,
unchanged. Gates: G-V1 pytest green, no judge-prompt changes; G-V2
(offline where archived inputs allow, else unit + live canary):
Chart-7-c/Chart-26-c tier-2 firings read invalid-on-both; G-V3 zero
demotions of any archived genuine tier-2 catch (the 11 Chart-19
firings must all read discriminating — buggy returns -1, no throw).
