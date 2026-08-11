# p1b step 3 — admission widening + the firing-state gate (build, 2026-08-11)

Mac only. No VM, no commit, no `docs/plan.md` edit. Full suite green at
**1037 passed, 7 skipped** (baseline 1013 passed, 7 skipped; the 24 new tests
are the difference).

Step 1 built the per-observable admission store (§11 addendum). Step 2's
coverage roll (plan 8.44) measured what that store had to hold: one admission
per Math-65 leg, zero `getRMS` references anywhere, zero admissions on the
Chart legs, zero `SUBSTITUTED` events. So step 3 is the two halves the
addendum named — widen the REQUEST so a leg holds a reference for each
observable its kept checks probe, and build the gate that evaluates that
reference at the firing's own recorded state — plus the removal of step 1's
one back-compat pass-through.

## Stations this build targets

| Station | Module | Target / Failure mode |
|---|---|---|
| Reference request | `src/java/run.py` `_widen_admissions` (new), called from the screening block at `run.py:2267`; enumeration in `src/java/relations/reference_impl.py` `observables_probed_by` / `widening_targets` | **Target:** which observables a leg asks for a reference for. **Failure mode:** the chain asked about the observable the FIRST firing it saw disputed and returned at the first fact, so a leg whose convictions dispute two observables got one of them by arrival order — 8.44's "ONE admission per Math-65 leg … ZERO rms references anywhere". |
| Firing-state reading | `src/java/relations/reference_state.py` (new, pure) | **Target:** the input every archived gate abstention names as missing ("corroboration needs the reference evaluated at the firing's own input — the 8.4 extension"). **Failure mode under repair:** the state printer emits `name=value` on the SAME line as the observables, so a `key=value` parser over the whole line reads `residuals=`, `cost=`, `rows=` as observations. |
| Verdict gate | `src/java/relations/reference_impl.py` `reference_verdict_gate` (+ `firing_state_reading_applies`); JVM adapter `src/java/run.py` `_firing_state_reading` (`run.py:187`); call sites `run.py:4453` (harness door) and `run.py:4856` (replay door) | **Target:** the gate that could only compare a firing against the reference's TEST-STATE values. **Failure mode avoided:** a new terminal that can only be reached by arithmetic, never by an LLM answer (8.43's lost catch was lost to a terminal defended by a family-duty answer that came back wrong once in three). |
| Admission lookup | `src/java/relations/reference_impl.py` `admitted_reference_for` | **Target:** step 1's single back-compat pass-through. **Failure mode:** returning the leg's one record on a lookup MISS was defensible while the gate compared test-state values; with the reference now evaluated at the firing's state it would not be a weaker answer but a wrong one. |
| Compiled-reference reuse | `src/java/relations/reference_run.py` `run_reference` | **Target:** §11.4's open item ("`ref_dir` is `None` … step 3 must plumb it"). **Failure mode:** without it the reference recompiles once per firing instead of once per leg, which is not the cost envelope §8.3 pre-registered. |
| Terminal gate ladder | `src/java/relations/judge_decision.py` | **Target:** deliberately NOT extended, and a test now pins that (`test_the_firing_state_reading_is_wired_at_both_doors_before_the_keep` asserts the module names neither `reference_firing` nor `firing_reading`). |

---

## 1. Widening mechanics

**Where.** Immediately after relation screening, inside the
`if synthesized_relations:` block, before any harness generation and before
either judge door. That is the earliest point at which the KEPT checks are
known, so every later lookup — by the firing's own observable, at both doors
— finds whatever this produced. Skipped in `--rulegen_only` mode, which
deliberately stops before the expensive stages.

**Enumeration.** `observables_probed_by(check_sources, code_context)` runs
`disputed_observables` once per kept check, with the check source in BOTH
positions (message and `check_source=`). Nothing new parses Java: the
call-site matcher (`_METHOD_CALL_RE`), the message-name matcher
(`_methods_named_by`) and the declaration filter (`_method_declared`) are the
ones the chain already applies to a firing. Results are deduped by
`admission_key`, so `chiSquare` and `getChiSquare` are one slot exactly as
everywhere else, and kept in arrival order across checks — which is the
screen's own best-first order, so the cap is spent on the checks most likely
to convict.

**The cap.** `config.P1B_MAX_REFERENCES`, default 3, overridable by the
environment variable of the same name. It bounds WHAT THE LEG MAY HOLD, not
what one call may ask: `room = cap - len(store)`. That is the same quantity
§8.3's cost envelope is stated in (one generation, one javac, one JVM chain
per admitted reference per leg) and it makes the bound hold however many
times the widening runs.

**The request.** `_reference_impl_fact(..., target_methods=[method])`. The
chain was already per-method; the only thing that changes is which methods it
is aimed at, and that with targets named it attempts EVERY one rather than
returning at the first fact (the store, not the fact, is what it is filling).
Same prompt, same screening surface, same `too_thin_to_screen` bar, same
state twin, same off-defect screen, same corroboration attribution, same pin
check, same fail-closed discards.

**How the reference prompt names its per-observable target.** It always did,
in one place: `build_reference_prompt`'s `method=` parameter, whose first
prompt line is `Implement \`{method}\` from its specification.` Everything
else in that prompt is derived from the same argument — `types_declaring(ctx,
method)` scopes the receiver, `sibling_observables(ctx, method,
declaring_types=…)` names the screening surface, `match_observable_names`
requires the reply to declare `compute_<method>`, and `fields_read_by(ctx,
method, canonical)` orders the parameters. So "generalise the request" means
calling that chain once per target observable; no prompt text changed.

**Events, per attempt.** All through the trace recorder, never `print()`.

| output | when |
|---|---|
| `<n> observable(s) to widen onto` / `nothing to widen` | once, with `targets`, `already_admitted`, `cap`, `checks_read` in the detail |
| `` reference REQUESTED for observable `<key>` `` | before each attempt |
| `` reference ADMITTED for observable `<key>` `` | the store gained that key; reason is the screen's own `screen_why` |
| `` widening REJECTED for observable `<key>` `` | it did not; reason is the chain's OWN last step for that target (`screen DISCARDED`, `pin-check DISCARDED`, `reference too thin to screen`, …), captured by wrapping the chain's event recorder rather than by asking the 15 early exits to report twice |

---

## 2. The gate hook point

`reference_verdict_gate` keeps its two-tuple return and its existing
test-state reading byte for byte. It gains one optional argument,
`firing_reading`, and consumes it before anything else:

* `agrees-with-patched` → `('void', reason)`. The one terminal.
* `agrees-with-check` → `('abstain', reason)`, saying corroborated. It does
  NOT fall through, because falling through could void a firing an
  independent implementation just sided with.
* anything else, including `None` → the existing reading runs unchanged.

The reading is computed by the caller because it needs a JVM. `run.py`'s
`_firing_state_reading(fired, admitted, evidence, builder, buggy_dir)`:

1. `firing_state_reading_applies(evidence)` — **tag-only**: the literal
   `[fact:rate-indiscriminate]` via `evidence_facts.fact_tags`, read exactly
   as `direction_confirmed_bypass` reads it. No rate is recomputed, nothing is
   inferred from prose. Without the tag the reading is not run and the event
   says so.
2. no admitted record → not run, reason `no-reference` naming the tag.
3. otherwise it builds an `evaluate` closure over
   `build_reference_call_driver('ReferenceImpl', [(method, declared)],
   literals)` and `run_reference(..., reference_dir=admitted['ref_dir'])`, and
   hands it to the pure reading. The driver builder is reused verbatim
   because it already hoists array literals into static fields to stay under
   the JVM's 64 KB bytecode-per-method cap.
4. one `reference-firing-state` event per gate entry, carrying the reading,
   the reason, `observed`/`claimed`/`reference`/`receiver`, the JVM run
   count, the `[fact:reference-at-firing-state]` text and the `__consumed`
   log.

Both doors call it with their own firing and their own evidence (`fired`/
`evid` on the harness track, `_fired`/`_evid` on the replay track) before the
gate decides, and the gate still runs only inside `if ok:` — it can void a
keep, never manufacture one.

Fail-open in the only direction that matters: any exception returns `None`,
and a `None` reading leaves the verdict identical to today's.

---

## 3. Reading definitions, verbatim

`REFERENCE_FIRING_READINGS = ('agrees-with-patched', 'agrees-with-check',
'degenerate', 'mutually-inconsistent')` and
`REFERENCE_FIRING_DISMISSING = frozenset({'agrees-with-patched'})`. With
`R` the reference, `X` the observed value and `Y` the claimed value, all
compared through `reference_impl._values_agree` →
`evidence_facts._close(a, b, rel=1e-9)`:

| condition | reading | gate effect |
|---|---|---|
| `R ≈ X` and not `R ≈ Y` | `agrees-with-patched` | **void** |
| `R ≈ Y` and not `R ≈ X` | `agrees-with-check` | abstain, fact says corroborated |
| `R ≈ X` and `R ≈ Y` | `degenerate` | abstain |
| neither | `mutually-inconsistent` | abstain |

`ABSTENTION_REASONS`, the closed list, each the first token of its sentence:
`no-reference`, `no-reference-for-this-observable`, `no-observable`,
`no-matching-receiver`, `ambiguous-receiver`, `receiver-never-ran`,
`non-finite-state`, `truncated-state`, `state-lost-in-consumed-payload`,
`parameters-not-read-by-method`, `unmappable`, `reference-unrunnable`, plus
`degenerate` and `mutually-inconsistent`.

The shape rules behind them, as built:

* **§1 split** — at the first ` __consumed=` and the first ` __rcvstate `,
  before ANY observable parsing. Observed and claimed come from the message
  region only.
* **§2.1 two receivers** — type filter (the disputed method's declaring
  types, stored on the admission record), then field-cover filter, then
  evaluate-both: same `R` on every candidate means the ambiguity is
  immaterial, different `R` is `ambiguous-receiver`.
* **§2.2 uninitialised receiver** — a zero `iterations|iterationCount|
  evaluations|evaluationCount` AND at least one field printing `null`,
  read off the WHOLE receiver rather than the mapped subset (on the corpus
  line all three mapped fields are non-null, so the mapped-subset version
  would have evaluated cleanly and returned a confident number).
* **§2.3 non-finite** — `NaN`/`±Infinity` in any mapped value or array
  element abstains unconditionally; the `…IsCalculated=false` companion is
  recorded in the detail and gates nothing.
* **§2.4 truncation** — structural, because a cut line carries no marker: a
  bracketed value must balance and end in `]`, and a scalar in the one
  position a cut can land on (last field of the last block) must be a
  well-formed primitive token. A truncated array is refused by NAME and never
  shortened. `__consumed` payloads are masked before any bracket counting;
  `__consumed` present with no `__rcvstate` is
  `state-lost-in-consumed-payload`.
* **§2.5 consumed** — never supplies a parameter; recorded verbatim, plus a
  one-directional `consumed-consistent` / `consumed-silent` note.
* **§2.7 reads-what-the-method-reads** — `reads_what_method_reads is False`
  abstains; `None` (body not visible) is undetermined and proceeds.

The fact is `[fact:reference-at-firing-state]`, emitted for the four computed
readings and `None` otherwise.

---

## 4. What the validation roll must read, per the referee prereg

The referee in §6 of the design stands unchanged. Nothing below is a new
prediction.

**Referee A (Math-65).** All 4 evaluable rows must read
`agrees-with-patched`, and ZERO Math-65 formula convictions may read
`agrees-with-check`. A single `agrees-with-check` there means the mapping is
feeding the reference the wrong fields — §0.1 establishes the right answer by
hand — and that is a REDESIGN signal, not a threshold to move. Both Family B
rows (`stack/04`, `stack/05`) must convert.

**Referee B (varbase/10).** Must read `agrees-with-check` or an abstention,
and must NEVER read `agrees-with-patched`. One dismissal there is a redesign.

**Per-event read-out the roll must count.** Every line below is now emitted
through the trace recorder.

| what to count | where it comes from |
|---|---|
| did widening MOVE admission coverage — 8.44's binding constraint | `reference-widening` events: `<n> observable(s) to widen onto` with its `targets`/`cap`, then one REQUESTED and one ADMITTED/REJECTED per target. Compare `admitted_observables` at the end against 8.44's "one per leg" |
| whether a `getRMS` reference is ever admitted | an ADMITTED event whose observable key is `rms`. The archive has never held one; the six RMS convictions are the ones provable wrong on paper and unreachable until this changes |
| why widening was refused, per target | the REJECTED event's reason, which is the chain's own last step (`screen DISCARDED`, `pin-check DISCARDED`, `reference too thin to screen`, `signature unmappable`, `twin underivable`, …). A widening that asked and was refused and a widening that never asked are different findings |
| per firing: was the reading even attempted | `reference-firing-state` output `firing-state reading NOT RUN` with reason `no [fact:rate-indiscriminate]…` vs `no-reference:…` vs a `reading: <x>` line |
| the reading distribution | `reference-firing-state` output `reading: <one of the four or abstain>`, reason head = the abstention reason. §8.4's inertness risk is read here: a p1b that is completely broken and one that is completely safe must not produce identical traces |
| the numbers behind every reading | the same event's detail: `observed`, `claimed`, `reference`, `receiver`, `jvm_runs`, `fact` |
| voids, and on which legs | `reference-verdict-gate` output `conviction VOIDED`, reason starting `agrees-with-patched:` |
| **the hard stop** | any `conviction VOIDED` on an OVERFIT leg ends the roll and the mechanism is rejected (Arm 2, the standing clean-leg rule applied in the direction p1b can damage) |
| honest-lookup gap, now closed | `reference-admission-lookup` reasons containing `no-reference-for-this-observable`. Under step 1 a single-admission leg would have read `SUBSTITUTED` here; every such line is a firing that now abstains honestly |

G-P1, G-P2, G-P3 and G-P4 are discharged offline by the 24 new tests
(exemplar, each 8.31 shape, the 67-row genuine-catch population, every
failure path). **G-P5 (the archived four) and G-P6 (the live roll) are NOT
discharged by this build** — they need a JVM and a run, and remain
user-gated.

---

## 5. Deviations from the design, recorded rather than hidden

1. **The reading is gated on `[fact:rate-indiscriminate]`** (per the step-3
   brief). The design's §5 has the gate run on every kept conviction; here the
   firing-state half runs only on the tagged population — the one 8.42 and
   8.43 both failed to convert. Untagged convictions read exactly as today.
   Widening the aim later is a config-free one-line change at
   `firing_state_reading_applies`, and would need its own pre-registration.
2. **The advisory fact is emitted through the trace recorder, not appended to
   `_fact_notes`/`_evid`.** §5.1(a) wants the fact in the evidence at both
   doors; the gate runs AFTER `adjudicate`, so an append there reaches no
   judge, and moving the JVM evaluation before adjudication would spend a
   javac and a JVM run on every firing rather than on kept convictions. The
   fact text is in the `reference-firing-state` event's detail, which is
   where a human reading `trace.md` finds it — which the design says is the
   only reason it exists.
3. **`no-observable` abstains when EITHER the observed or the claimed value
   is unresolvable**, not only when both are (§1 step 2 says "neither"). The
   three-way reading needs both numbers, so one of them missing is not a
   weaker reading but no reading.
4. **A lookup miss surfaces two greppable strings, not one.**
   `admitted_reference_for` says `no-reference-for-this-observable` (and the
   gate repeats it as the abstention reason); the firing-state reading, which
   is handed `None`, says `no-reference`. Both are in
   `ABSTENTION_REASONS`.
5. **`run_reference` grew two keyword arguments** (`reference_dir`, `out`)
   rather than a third return value, so every existing caller and every
   stubbed double in the tests keeps its arity. `out['ref_dir']` is what the
   admission record now stores.
6. **Three seam tests were re-pinned, not weakened**:
   `test_wiring_both_doors_pass_patch_path` now asserts 3 chain call sites
   and checks the two DOOR sites separately from the widening one;
   `test_BOTH_judge_doors_carry_the_mechanism` asserts 3 flag reads and adds
   a new assertion that exactly 2 of them are judge doors; two window-width
   assertions grew because the source above them grew.
