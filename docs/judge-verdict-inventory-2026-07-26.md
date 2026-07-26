# Judge decisions on fired checks — population inventory across five archived runs

> Produced 2026-07-26 by a full-trace sweep (subagent + main-session commissioning) of
> pool30, poolA, poolB, night20, width5 — 115 leg dirs, every trace.md parsed. A judged
> firing = "The assertion that ACTUALLY fired…" block + evidence facts + VERDICT/WHY.
> 228 verdicts across 78 legs (37 legs had no judged firings). Verdict count matched
> assertion-marker count 1:1 per trace; leg↔outcome mapping cross-checked against each
> run's summary.md. This file is the fixture source for any judge-side change: rows
> here ARE the verifier_replay case population. Companion analysis: the 2026-07-26
> sections of docs/plan.md.

**Facts legend:** `lat+shad` = latent oracle + buggy replay SHADOWED (no per-input
attribution). `IDENT` = identical-on-both-builds computed fact. `muted` /
`muted:fires-buggy` = muted re-replay ran / fired on buggy too. `fires-on-buggy-same-check`
= same check fires on buggy at this input (values not compared). `fires-both-DIFFvals` =
fires on both builds with different values. `PATCH-INTRODUCED` = buggy build handles this
input without the exception. `RRb X/N` = relation screened on buggy fired X/N; `trig det`
= deterministic 2/2 replay on the failing test's own literals on patched; `pf X/N` =
patched-build fuzz fire count. `TRIGTIER` = [trigger-tier fact] (fires on test's own
literals yet real failing test passes on patched — worded to push dismissal). `FRb../..
p../..` = [fire-rate fact] buggy/patched. `screendec` = firing is same check as an
already-screened relation. `UNIVSCREEN` = [universal-screen fact] claim never held on
buggy. `diffreplay-*` = [differential replay] variants. **Class** (UNSOUND = hypothetical
type; SOUND = keep grounds). `V`: S/U. `n` = near-identical verdicts collapsed.

## (a) Full inventory (228 verdicts collapsed to 206 rows)

| # | run | leg | L | check | fired values | facts | V | class | WHY gist | out | n |
|--|--|--|--|--|--|--|--|--|--|--|--|
| 1 | night20 | 01 Math-2-Arja | o | oracle:fuzzed-invcdf-support-point | N=1, m=0, n=1, support=[0,0], p=1.0E-6, x=0 | lat+shad | S | observed-impossible | quantile must be a support point with positive mass; NaN probability impossible | TP | 1 |
| 2 | night20 | 01 Math-2-Arja | o | oracle:mean-formula | expectedMean=5489.55…, N=552824, m=236131 | lat+shad | S | contract-backed | documented mean n*m/N violated on valid input | TP | 1 |
| 3 | night20 | 02 Lang-41-Arja | o | relation shortClassName_class_string_overload_agree | — | RRb0/20k trig det pf14490/20k; TRIGTIER; FRb0/20k p14490/20k | S | contract-backed | Class overload documented to delegate to String overload; disagreement impossible | TP | 1 |
| 4 | night20 | 02 Lang-41-Arja | o | relation packageName_class_string_overload_agree | — | RRb0/20k trig det pf9325/20k; TRIGTIER; FRb0/20k p9325/20k | S | trusted-lift | getPackageName(cls) defined as getPackageName(cls.getName()); trusted tests pin array cases | TP | 1 |
| 5 | night20 | 03 Lang-50-Arja | o | oracle:date-default-pattern-us | expected pattern=EEEE, MMMM d, y actual=M/d/yy | lat+shad | S | contract-backed | FULL date instance must match JDK date-only pattern for same locale | TP | 1 |
| 6 | night20 | 03 Lang-50-Arja | o | oracle:date-helper-identity | equivalent FULL-date Germany formatters same cached inst | lat+shad; FRb999/1000 | U | lazy-state | undocumented object-identity (same cached instance) contract assumed | TP | 1 |
| 7 | night20 | 03 Lang-50-Arja | o | oracle:default-style-jdk-render | getDateInstance(style) must behave as localized | lat+shad | S | contract-backed | must format like JDK localized date-only formatter after setting default locale | TP | 1 |
| 8 | night20 | 03 Lang-50-Arja | o | relation dateInstance_default_overload_agrees_with_explicit_default_l | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | U | lazy-state | check requires `la != lb` — Locale object identity; equal-but-distinct instances legal (real check bug: `!=` compare) | TP | 1 |
| 9 | night20 | 03 Lang-50-Arja | o | relation dateInstance_defaultLocale_agrees_with_explicitDefaultLocale | — | RR trig det pf20k/20k; TRIGTIER; FRb10020/20k p20k/20k | S | trusted-lift | default-locale overload agreement pinned by docs + trusted tests | TP | 1 |
| 10 | night20 | 04 Lang-60-Arja | o | oracle:contains-readonly-seed | contains(char) changed state capacity 43->6 | lat+shad | U | lazy-state | contains could legitimately compact buffer before searching | FN | 1 |
| 11 | night20 | 04 Lang-60-Arja | o | oracle:token-seed-removed-char-contains | deleted char still present | lat+shad; FRb999/1000 | U | invented-generalization | base string already contains probe char; deleteFirst premise wrong | FN | 2 |
| 12 | night20 | 04 Lang-60-Arja | o | oracle:contains-capacity-stable-seed | contains(char) changed capacity from 43 to 6 | lat+shad | U | lazy-state | contains could lazily minimizeCapacity then search | FN | 1 |
| 13 | night20 | 05 Chart-7-Arja | o | oracle:constructed-middle | expected middle-max index 1 but was 0 | lat+shad | S | contract-backed | documented max-middle uniquely determined for concrete constructed periods | TP | 1 |
| 14 | night20 | 06 Closure-92-SequenceR | o | oracle:lifted-testProvideInIndependentModules4 | expected=varapps={};apps.foo={};… (no final ;) | fires-on-buggy-same-check; FRb691/691; UNIVSCREEN | U | format-freedom | trailing-semicolon variant is semantically equivalent; exact literal overclaims | FN | 2 |
| 15 | night20 | 06 Closure-92-SequenceR | o | oracle:lifted-seed-exact | expected exact toSource() literal | fires-on-buggy-same-check; FRb999/1000 | U | format-freedom | correct compiler may emit trailing semicolon | FN | 1 |
| 16 | night20 | 06 Closure-92-SequenceR | o | oracle:lifted-bug261 | expected=varapps={};… | fires-on-buggy-same-check; FRb999/1000 | U | format-freedom | modules may serialize in different order/formatting while contract holds | FN | 1 |
| 17 | night20 | 06 Closure-92-SequenceR | o | oracle:lifted-independent-modules4 | expected=varapps={};… | fires-on-buggy-same-check; FRb899/899; UNIVSCREEN | U | format-freedom | semantically identical source w/ final semicolon would fire | FN | 1 |
| 18 | night20 | 08 Closure-38-SequenceR | o | oracle:separator-generalized | expected=(x)- -0.0 actual=x- -0.0 | lat+shad; FRb999/1000 | U | format-freedom | printer may canonicalize away redundant parentheses | FN | 1 |
| 19 | night20 | 08 Closure-38-SequenceR | o | oracle:separator-generalized | expected=do- -0.0 actual= | lat+shad; FRb999/1000 | U | parse-error-latitude | `do` is keyword; invalid JS may yield empty output | FN | 1 |
| 20 | night20 | 08 Closure-38-SequenceR | o | oracle:minus-negative-zero-family | expected=return- -0.0 actual= | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | parse-error-latitude | `public`/`return` reserved; parse error latitude | FN | 2 |
| 21 | night20 | 08 Closure-38-SequenceR | o | relation minus_positive_zero_has_no_forced_space | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | U | format-freedom | correct printer could emit x-0.0 vs hard-coded x-0 | FN | 1 |
| 22 | night20 | 08 Closure-38-SequenceR | o | relation minus_positive_integer_has_no_forced_space | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | U | format-freedom | "compact" doesn't forbid optional separator `x- 1` | FN | 1 |
| 23 | night20 | 09 Lang-63-Arja | o | oracle:constructed-month-answer | expected=09 actual=08 startYear=1900 | lat+shad; FRb999/1000 | U | invented-generalization | generalizes single trusted case (Dec31→Oct6="09") to all years/dates | FN | 1 |
| 24 | night20 | 09 Lang-63-Arja | o | oracle:day-shift-invariant | 24h shift changed dd-format lhs=06… rhs=08… | lat+shad | U | timezone-env | DST-observing default zone could change result across transition | FN | 2 |
| 25 | night20 | 10 Math-68-Arja | o | oracle:freudenstein-roth-rms | expected=6.9988… actual=4.9489… | IDENT | U | preexisting/identical | same mismatch on unpatched build; not patch-caused | TP | 1 |
| 26 | night20 | 10 Math-68-Arja | o | oracle:seed-rms | expected=11.1517… actual=3.5265… | IDENT | U | preexisting/identical | fires identically on buggy build; real test doesn't pin this RMS | TP | 1 |
| 27 | night20 | 10 Math-68-Arja | o | oracle:seed-rms | expected RMS 20.0124… was 4.9489… | IDENT; FRb2997/1000 | U | other (alt-definition) | RMS=sqrt(c/n) documented; trusted optimum gives the observed value | TP | 1 |
| 28 | night20 | 10 Math-68-Arja | o | oracle:jennrich-seed-param1 | expected=0.257829976764542 actual=0.257817659 | shad | S | trusted-lift | direct lift of trusted regression test; mismatch 1.2e-5 >> fp noise | TP | 2 |
| 29 | night20 | 10 Math-68-Arja | o | oracle:fr1-rms | expected RMS 6.9988… was 4.9489… | IDENT | U | tolerance-floor | hard-coded MINPACK literal under harness's own iteration settings | TP | 1 |
| 30 | night20 | 10 Math-68-Arja | o | oracle:fr-seed-1-rms | expected=20.0124… actual=4.9489… | IDENT; screendec | U | other (alt-definition) | documented RMS definition yields observed value | TP | 1 |
| 31 | night20 | 10 Math-68-Arja | o | relation rms-chi-square-formula-after-optimize | — | RR trig det pf9691/20k; TRIGTIER; FRb9941/20k p9691/20k | U | other (alt-definition) | getRMS weighted vs getChiSquare unweighted definitions may differ | TP | 1 |
| 32 | night20 | 11 Math-73-ACS | o | oracle:mirrored-negative-nonbracketing-4arg | solve(f,-1.5,-1.0,-1.2) completed | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | S | contract-backed | javadoc pins IAE for same-sign triples (DUTY pattern beats IDENT) | TP | 1 |
| 33 | night20 | 11 Math-73-ACS | o | oracle:endpoint-root-in-interval | root must lie in interval | fires-both-DIFFvals; FRb999/1000 | S | observed-impossible | returned root outside requested interval impossible for correct solver | TP | 1 |
| 34 | night20 | 12 Math-74-Arja | o | oracle:poly-closed-form | expectedY=1.0 | IDENT; screendec | U | preexisting/identical | too-tight accuracy; same on unpatched | TP | 1 |
| 35 | night20 | 12 Math-74-Arja | o | oracle:poly-final-state-step1 | expected=6.0 err=8.7e-7 | fires-both-DIFFvals; FRb999/1000 | U | tolerance-floor | approximate solver only; 8.7e-7 within legitimate error | TP | 1 |
| 36 | night20 | 12 Math-74-Arja | o | oracle:closed-form-state | expected=0.75 tol=1.0E-8 | fires-both-DIFFvals; screendec | U | tolerance-floor | invented 1e-8 closed-form accuracy contract | TP | 1 |
| 37 | night20 | 12 Math-74-Arja | o | relation integrate-returns-requested-end-time | — | RRb0/20k; pf1690/20k | S | observed-impossible | no-events integrate must stop at requested t | TP | 1 |
| 38 | night20 | 12 Math-74-Arja | o | relation constant-derivative-exact-solution | — | RRb0/20k; pf435/20k | S | contract-backed | successful no-event integration must return target time | TP | 1 |
| 39 | night20 | 12 Math-74-Arja | o | relation constant-derivative-independent-of-nsteps | — | RRb0/20k; pf312/20k | S | contract-backed | Adams-Moulton formulas collapse to same linear update for any nSteps | TP | 1 |
| 40 | night20 | 12 Math-74-Arja | o | relation zero-derivative-split-composition | — | RRb0/20k; pf896/20k | S | observed-impossible | zero-derivative fenced input must preserve constant state | TP | 1 |
| 41 | night20 | 12 Math-74-Arja | o | relation constant-derivative-forward-backward-roundtrip | — | RRb0/20k trig det pf7824/20k; TRIGTIER; FRb0/20k p7824/20k | U | tolerance-floor | fp time-reversibility not guaranteed for adaptive method | TP | 1 |
| 42 | night20 | 14 Math-104-Elixir | o | oracle:default-complement | P=0.51858…, 1-Q=0.51858… (tiny diff) | lat+shad | U | tolerance-floor | independent approximations at DEFAULT_EPSILON=10e-9 may miss 1e-9 gate | FN | 2 |
| 43 | night20 | 14 Math-104-Elixir | o | oracle:explicit-complement | P+Q≠1, eps=1e-7 | lat+shad | U | tolerance-floor | separate approximations with caller epsilon | FN | 1 |
| 44 | night20 | 14 Math-104-Elixir | o | relation p_q_complement_explicit_overloads | — | RRb25/20k; pf33/20k | U | tolerance-floor | iterative overloads with epsilon up to 1e-6 | FN | 1 |
| 45 | night20 | 14 Math-104-Elixir | o | relation p_q_complement_for_explicit_overloads | — | RRb63/20k; pf56/20k | U | tolerance-floor | different approximate algorithms differ beyond gate | FN | 1 |
| 46 | night20 | 16 Closure-62-Jaid | c | oracle:end-of-line-caret | caret missing/wrong input=x | fires-on-buggy-same-check; FRb20k/20k; screendec | U | format-freedom | oracle's dynamicExpected itself wrong for charno==length | FP | 1 |
| 47 | night20 | 16 Closure-62-Jaid | c | oracle:testFormatErrorSpaceEndOfLine1 | expected=javascript/complex.js:1:ERROR-… | shad | S | trusted-lift | exact trusted unit-test contract for concrete input | FP | 1 |
| 48 | night20 | 16 Closure-62-Jaid | c | oracle:ground-truth | expected=javascript/complex.js:1: ERROR - … | fires-on-buggy-same-check; FRb999/1000 | S | trusted-lift | exact trusted-test oracle (kept despite fires-on-buggy fact) | FP | 1 |
| 49 | night20 | 17 Chart-26-Jaid | c | oracle:chart-draw-domain-axis-vs-fresh | domain axis vs fresh snapshot | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | broken-check | AxisSnapshot.sameAs requires same plot reference; fires on correct code | TN | 1 |
| 50 | night20 | 18 Math-30-CapGen | c | oracle:large-sample-probability | p=NaN for n=46341 | lat+shad; IDENT; muted:fires-buggy; screendec | U | preexisting/identical | buggy build returns NaN at same input too | FP | 1 |
| 51 | night20 | 18 Math-30-CapGen | c | oracle:boundary-consistency | reportedP=NaN independentP=0 | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | preexisting/identical | unrelated n=46341 boundary; buggy does same | FP | 1 |
| 52 | night20 | 18 Math-30-CapGen | c | oracle:u-complement | U(x,y)+U(y,x) must equal n1*n2 | lat+shad; muted | U | other (alt-definition) | mannWhitneyU returns larger of complementary U values by design | FP | 2 |
| 53 | night20 | 18 Math-30-CapGen | c | oracle:overflow-boundary-monotone-p | expected p(n+1) < p(n) | fires-on-buggy-same-check; FRb839/839; UNIVSCREEN | U | tolerance-floor | astronomically small p-values underflow to 0.0 for both sizes | FP | 1 |
| 54 | night20 | 18 Math-30-CapGen | c | oracle:canonical-separated-u | expectedU=2.147580964E9 actualU=2.147483648E9 | shad | U | preexisting/identical | same mismatch fires on buggy; unrelated to trusted bug | FP | 1 |
| 55 | night20 | 18 Math-30-CapGen | c | oracle:canonical-parity-closed-form | NaN instead of ≈0.9954… | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | S | contract-backed | finite mathematically-determined p-value; NaN impossible (kept despite IDENT) | FP | 1 |
| 56 | night20 | 18 Math-30-CapGen | c | relation large-sample-pvalue-is-probability | — | RR trig det pf2783/2783; TRIGTIER; FRb1297/2270 p2783/2783 | S | contract-backed | p-value must be finite in [0,1] | FP | 1 |
| 57 | night20 | 18 Math-30-CapGen | c | relation pvalue-in-unit-interval-near-large-sizes | — | RR; pf1056/2723; FRb746/1435 p1056/2723 | S | contract-backed | p-value in [0,1] for completed calls | FP | 1 |
| 58 | night20 | 19 Math-65-CapGen | c | relation chiSquare_matches_weighted_residual_sum | expected=1710.4699… | lat+shad; screendec | S | consistency | same-state chi² recomputation from returned optimum | FP | 1 |
| 59 | night20 | 19 Math-65-CapGen | c | oracle:linear-chi-weighted-sum | chi=1777.298… | lat+shad; screendec | S | contract-backed | documented chi² = weighted residual sum after optimize | FP | 1 |
| 60 | night20 | 19 Math-65-CapGen | c | relation chiSquare_matches_weighted_residual_sum | — | RR trig det pf9138/20k; TRIGTIER; FRb13867/20k p9138/20k | S | contract-backed | chi² documented as Σ residual²/variance, variance=1/weight | FP | 1 |
| 61 | night20 | 19 Math-65-CapGen | c | relation chiSquare_matches_weighted_squared_residuals | — | RR trig det pf10400/20k; TRIGTIER; FRb14764/20k p10400/20k | S | contract-backed | documented chi² contract exact | FP | 1 |
| 62 | night20 | 20 Math-73-Arja | c | oracle:overload-agreement | overloads disagree | lat+shad; screendec | U | tolerance-floor | overloads only promise root within 1e-6 accuracy | TN | 1 |
| 63 | night20 | 20 Math-73-Arja | c | oracle:endpoint-min-root | expected π got 1.22e-16 | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | preexisting/identical | unpatched build returns same value; pre-existing | TN | 1 |
| 64 | pool30 | 02 Lang-41-Arja | o | relation packageName_class_overload_agrees_with_string_overload | — | RRb0/20k; pf4259/20k; FRb0/20k p4259/20k | S | trusted-lift | overload delegation shown in implementation + trusted array tests | TP | 1 |
| 65 | pool30 | 03 Lang-50-Arja | o | oracle:date-sibling-agreement | overloads disagree chosen=en_US style=0 | lat+shad | S | consistency | sibling overloads with default locale must agree; observed FULL vs SHORT impossible | TP | 1 |
| 66 | pool30 | 03 Lang-50-Arja | o | relation dateInstance_defaultLocaleSiblingAgreement | — | RR trig det pf20k/20k; TRIGTIER; FRb11607/20k p20k/20k | S | trusted-lift | harness sets Locale.setDefault before both calls | TP | 1 |
| 67 | pool30 | 04 Lang-60-Arja | o | oracle:contains-readonly | contains must be observationally read-only | lat+shad | S | contract-backed | capacity 32→0 on same builder impossible for correct contains | TP | 1 |
| 68 | pool30 | 04 Lang-60-Arja | o | oracle:contains-readonly-capacity | capacity 35 to 3 | lat+shad | U | lazy-state | contains could compact storage as optimization | TP | 1 |
| 69 | pool30 | 05 Chart-7-Arja | o | relation maxMiddleIndex_points_to_a_maximum_middle | — | RR; pf3862/20k | U | other (alt-definition) | integer-average middle could differ from check's recomputation | FN | 1 |
| 70 | pool30 | 06 Closure-92-SequenceR | o | oracle:constructed-star | expected=varype={};ype.class=… | lat+shad | U | parse-error-latitude | `class` reserved token in constructed namespace | TP | 1 |
| 71 | pool30 | 06 Closure-92-SequenceR | o | oracle:lifted-test | expected=varapps={};… | fires-on-buggy-same-check | S | trusted-lift | encodes trusted regression contract for fixed input | TP | 1 |
| 72 | pool30 | 06 Closure-92-SequenceR | o | oracle:testProvideInIndependentModules4 | expected=varapps={};… | fires-on-buggy-same-check | U | format-freedom | hard-coded literal missing semicolon vs trusted expected | TP | 1 |
| 73 | pool30 | 07 Math-2-SOFix | c | oracle:mean-formula | expectedMean=2678.80495… | lat+shad; muted | U | tolerance-floor | bit-exact == on n*m/N; different evaluation order legal | TN | 2 |
| 74 | pool30 | 09 Lang-60-SimFix | c | oracle:tokenizer-emptiness | hasNext vs isEmpty disagree | lat+shad | U | format-freedom | delimiter-only content: tokenizer empty while builder non-empty is correct | TN | 2 |
| 75 | pool30 | 10 Closure-62-Jaid | c | oracle:seed-test | expected=javascript/complex.js:1:ERROR-… | fires-on-buggy-same-check | S | trusted-lift | exact lift of trusted test (kept despite fires-on-buggy fact) | FP | 1 |
| 76 | pool30 | 10 Closure-62-Jaid | c | oracle:warning-marker | expected caret-only marker line | lat+shad | U | broken-check | harness's fake getSourceLine embeds \n; setup flaw | FP | 1 |
| 77 | pool30 | 11 Chart-26-Jaid | c | oracle:gantt-null-info | Gantt draw(null info) should succeed | fires-on-buggy-same-check | S | trusted-lift | test-pinned contract; valid chart reaches defect (kept despite fires-on-buggy) | FP | 1 |
| 78 | pool30 | 16 Closure-38-SequenceR | o | oracle:ast-negzero-shape | expected="x- -0.0;" actual="x- -0.0" | fires-on-buggy-same-check | U | format-freedom | trailing semicolon optional for top-level expression | FN | 1 |
| 79 | pool30 | 16 Closure-38-SequenceR | o | oracle:chosen-ident-negzero | expected="enum- -0.0"… | shad | U | parse-error-latitude | `enum` reserved; invalid program latitude | FN | 1 |
| 80 | pool30 | 16 Closure-38-SequenceR | o | relation subtraction_positive_integer_has_no_extra_separator | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | U | format-freedom | printer "could" emit optional separator `1- 2` | FN | 1 |
| 81 | pool30 | 17 Closure-70-Jaid | c | crash:RuntimeException | Parameter var not found: c | diffreplay-generic-preexisting; PATCH-INTRODUCED | U | broken-check | harness built two same-named functions; helper found instead of target | TN | 1 |
| 82 | pool30 | 19 Lang-63-Arja | o | relation period_and_duration_agree_for_days_and_lower_in_utc | — | RR; pf13364/20k; FRb12630/20k p13364/20k | U | invented-generalization | "undocumented equivalence"; calendar-field decomposition latitude (but source pins UTC) | FN | 1 |
| 83 | pool30 | 22 Math-65-CapGen | c | oracle:getters-read-only | repeated getters changed output | lat+shad | U | tolerance-floor | getters may recompute Jacobian each call | FP | 1 |
| 84 | pool30 | 22 Math-65-CapGen | c | oracle:chiSquare_matches_weighted_squared_residuals | expectedWeightedResidualSum=2568.509… | lat+shad | S | consistency | recomputes documented chi² from object's own output | FP | 1 |
| 85 | pool30 | 22 Math-65-CapGen | c | relation chiSquare_matches_weighted_squared_residuals | — | RR trig det pf12185/20k; TRIGTIER; FRb18522/20k p12185/20k | U | other (alt-definition) | shown impl computes Σ residual²/weight, not weight·residual² | FP | 1 |
| 86 | pool30 | 22 Math-65-CapGen | c | relation uniform_weight_scaling_scales_chiSquare_by_same_factor | — | RR trig det pf8028/20k; TRIGTIER; FRb19550/20k p8028/20k | U | other (alt-definition) | with chi²=Σr²/w scaling direction inverts vs check's claim | FP | 1 |
| 87 | pool30 | 23 Math-68-Arja | o | oracle:jennrich-p1 | expected 0.257829976764542 | lat+shad | S | trusted-lift | trusted regression pins second parameter for exact seed | TP | 1 |
| 88 | pool30 | 24 Math-73-ACS | o | oracle:no-bracket-family | non-bracketing accepted input=1.001 | lat+shad | S | contract-backed | documented IAE for non-bracketing triple | TP | 1 |
| 89 | pool30 | 24 Math-73-ACS | o | oracle:overload-agreement | overloads disagree on rejection | lat+shad; IDENT; muted:fires-buggy | S | contract-backed | contract requires both overloads throw (DUTY beats IDENT) | TP | 1 |
| 90 | pool30 | 25 Math-73-Arja | c | oracle:exact-endpoint-root | unique endpoint root not returned | lat+shad | S | observed-impossible | successful solve must return the interval's unique root | FP | 1 |
| 91 | pool30 | 25 Math-73-Arja | c | oracle:pi-root-3arg | expected 3.141592653589793 | lat+shad | U | tolerance-floor | default accuracy 1e-6 permits 3.14159271… | FP | 1 |
| 92 | pool30 | 25 Math-73-Arja | c | oracle:endpoint-root | solve(f, 6.283…, 7.283…) | lat+shad | S | contract-backed | unique root at left endpoint must be returned | FP | 1 |
| 93 | pool30 | 25 Math-73-Arja | c | relation exactEndpointRootReturnedOnUniqueRootInterval | — | RR trig det pf10902/20k; TRIGTIER; FRb10518/20k p10902/20k | U | tolerance-floor | approximate root min+1e-6 legal within accuracy | FP | 1 |
| 94 | pool30 | 27 Math-82-HDRepair | o | oracle:constraint-scaling | scaled constraints changed optimum | lat+shad; muted | S | observed-impossible | positive scaling leaves feasible region unchanged | TP | 1 |
| 95 | pool30 | 30 Math-104-Elixir | o | oracle:p-q-same-tuning | P+Q≠1 same tuning | lat+shad | U | tolerance-floor | loose epsilon=1e-6; independent approximations | FN | 2 |
| 96 | pool30 | 30 Math-104-Elixir | o | oracle:pq-complement-same-tuning | p+q must equal 1 | lat+shad | U | tolerance-floor | 1e-9 gate vs 1e-6 requested accuracy | FN | 1 |
| 97 | pool30 | 30 Math-104-Elixir | o | oracle:q-overload-agreement-cf | overloads disagree | lat+shad | U | tolerance-floor | 2-arg uses DEFAULT_EPSILON=10e-9 vs 1e-15 explicit | FN | 2 |
| 98 | pool30 | 30 Math-104-Elixir | o | relation p_q_complement_same_tuning | — | RRb678/20k; pf187/20k | U | tolerance-floor | epsilon as loose as 1e-6 permitted by harness | FN | 1 |
| 99 | poolA | 01 Math-2-Arja | o | relation hypergeom_mean_formula | — | RRb3201/20k; pf3549/20k | S | contract-backed | javadoc specifies mean = n*m/N | TP | 1 |
| 100 | poolA | 03 Lang-50-Arja | o | oracle:dateInstance_noarg_matches_explicit_default_locale | noarg vs explicit patterns differ | lat+shad; screendec | S | contract-backed | same style+locale must select same formatter | TP | 1 |
| 101 | poolA | 03 Lang-50-Arja | o | oracle:date-overload-pattern | overloads produced different patterns | lat+shad; screendec | S | contract-backed | shown body delegates one overload to the other | TP | 1 |
| 102 | poolA | 03 Lang-50-Arja | o | oracle:date-noarg-matches-explicit-default | en_US/M/d/yy vs explicit | lat+shad | S | contract-backed | API + delegation require same formatter | TP | 1 |
| 103 | poolA | 03 Lang-50-Arja | o | relation dateInstance_noarg_matches_explicit_default_locale | — | RR trig det pf20k/20k; TRIGTIER; FRb14266/20k p20k/20k | S | contract-backed | documented default-locale equivalence | TP | 1 |
| 104 | poolA | 04 Lang-60-Arja | o | oracle:contains-readonly-capacity | read-only query changed capacity | lat+shad; screendec | U | lazy-state | contains could lazily compact empty builder's buffer | FN | 1 |
| 105 | poolA | 05 Chart-7-Arja | o | oracle:constructed-family | fixes max-middle index at 1 | lat+shad; screendec | U | broken-check | check internally wrong about its own constructed periods | FN | 1 |
| 106 | poolA | 06 Closure-92-SequenceR | o | oracle:lifted-pairs | expected=varapps={};… | fires-on-buggy-same-check; FRb999/1000 | U | format-freedom | hard-coded string omits semicolon vs trusted output | FN | 1 |
| 107 | poolA | 06 Closure-92-SequenceR | o | oracle:lifted-seed | expected exact literal | fires-on-buggy-same-check; FRb999/1000 | U | format-freedom | trailing semicolon variant legal | FN | 1 |
| 108 | poolA | 10 Closure-62-Jaid | c | oracle:endOfLineCaretConstructed | expected raw fuzzDesc echoed | lat+shad; FRb999/1000 | U | format-freedom | MessageFormat apostrophe collapsing legal ('t'1'→t1) | TN | 1 |
| 109 | poolA | 10 Closure-62-Jaid | c | oracle:endOfLineCaretConstructed | expected=asser:1: ERROR - t'1 | lat+shad; FRb999/1000 | U | invented-generalization | raw-echo assumption not contractual | TN | 1 |
| 110 | poolA | 11 Chart-26-Jaid | c | relation dataset_addValue_double_matches_Number | — | RR trig det pf20k/20k; TRIGTIER; FRb20k/20k p20k/20k | U | lazy-state | Number subtype preservation legal (Double vs Integer) | TN | 1 |
| 111 | poolA | 14 Chart-19-Arja | o | relation objectList-indexOf-null-absent-is-minus1 | — | RR trig det pf20k/20k; TRIGTIER; FRb20k/20k p20k/20k | U | other (alt-definition) | set(i,·) growth leaves in-list nulls; indexOf(null) may hit them | FN | 1 |
| 112 | poolA | 16 Closure-38-SequenceR | o | oracle:ast-vs-parse | built=static- -0.0 reparsed= | lat+shad | U | parse-error-latitude | `final`/`static` reserved; reparse may error → empty | FN | 2 |
| 113 | poolA | 17 Closure-70-Jaid | c | oracle:alpha-rename | renaming must not change warnings | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | broken-check | harness renaming not capture-avoiding/validity-preserving | TN | 1 |
| 114 | poolA | 20 Math-30-CapGen | c | oracle:u-formula | p-value disagrees with U formula | lat+shad; muted; screendec | S | contract-backed | documented asymptotic p-value from same public U | FP | 1 |
| 115 | poolA | 20 Math-30-CapGen | c | oracle:u-formula-large-product | expectedFromFormula=0.0 n=46… | lat+shad; muted; screendec | S | contract-backed | tie-free input; formula determined | FP | 1 |
| 116 | poolA | 20 Math-30-CapGen | c | relation asymptotic-pvalue-matches-u-formula-large-product | — | RR trig det pf1352/1352; TRIGTIER; FRb613/1167 p1352/1352 | S | contract-backed | Umin-based two-sided p-value determined | FP | 1 |
| 117 | poolA | 22 Math-65-CapGen | c | relation chiSquare_matches_weighted_residual_sum | — | RR trig det pf8378/20k; TRIGTIER; FRb14034/20k p8378/20k | S | consistency | javadoc "variances are reciprocal of weights" | FP | 1 |
| 118 | poolA | 22 Math-65-CapGen | c | relation uniform_weight_scaling_scales_chiSquare_and_rms | — | RR trig det pf5447/20k; TRIGTIER; FRb17217/20k p5447/20k | U | other (alt-definition) | chi²=Σr²/wᵢ inverts the scaling the check asserts | FP | 1 |
| 119 | poolA | 23 Math-68-Arja | o | oracle:seed-rms | expected=11.1517… actual=3.5265… | IDENT | U | preexisting/identical | same call/input yields same value on both builds; custom optimizer config | FN | 1 |
| 120 | poolA | 23 Math-68-Arja | o | relation overdetermined_exact_fit_same_optimum_from_different_starts | — | RR trig det pf18851/20k; TRIGTIER; FRb18611/20k p18851/20k | U | tolerance-floor | approximate minimizer; exact-value contract invented | FN | 1 |
| 121 | poolA | 24 Math-73-ACS | o | oracle:overload-agreement | solve()=3.36e-8 vs solve() other seed | lat+shad; screendec | U | tolerance-floor | overloads may converge to different approximations within 1e-6 | TP | 2 |
| 122 | poolA | 24 Math-73-ACS | o | relation threeSameSignMustThrow | — | RR trig det pf15815/20k; TRIGTIER; FRb20k/20k p15815/20k | S | contract-backed | documented IAE when all three points same sign | TP | 1 |
| 123 | poolA | 26 Math-74-Arja | o | oracle:lifted-evals-2 | expected > 140 for nSteps=2 | IDENT; screendec | U | invented-generalization | generalizes eval-count thresholds to different ODE/handler | FN | 1 |
| 124 | poolA | 26 Math-74-Arja | o | oracle:lifted-evals-6 | expected < 90 for nSteps=6 | shad; screendec | U | invented-generalization | implementation-specific performance profile | FN | 1 |
| 125 | poolA | 30 Math-104-Elixir | o | oracle:p-overload-agreement | overloads must agree a=15.0 | lat+shad | U | tolerance-floor | shown code delegates 2-arg with DEFAULT_EPSILON=10e-9 not 10e-15 | FN | 2 |
| 126 | poolA | 30 Math-104-Elixir | o | oracle:q-overload-agreement | overloads must agree a=17.0 | lat+shad | U | tolerance-floor | comparing different-tolerance computations | FN | 1 |
| 127 | poolB | 01 Math-2-Arja | o | relation hypergeom_mean_formula | — | RR trig det pf8943/20k; TRIGTIER; FRb6320/20k p8943/20k | S | contract-backed | javadoc contracts mean n*m/N; exceptions swallowed | TP | 1 |
| 128 | poolB | 02 Lang-41-Arja | o | oracle:package-overload-agreement | Class vs String overload | lat+shad | S | trusted-lift | grounded by API's JVM-name behavior for String[].class | TP | 1 |
| 129 | poolB | 02 Lang-41-Arja | o | oracle:shortClassName_class_string_overload_agree_on_arrays | class [Lj… | lat+shad; screendec | S | contract-backed | array class overload agreement per implementation | TP | 1 |
| 130 | poolB | 02 Lang-41-Arja | o | relation shortClassName_class_string_overload_agree_on_arrays | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | S | trusted-lift | both documented to compute from same name | TP | 1 |
| 131 | poolB | 02 Lang-41-Arja | o | relation packageName_class_string_overload_agree_on_arrays | — | RRb0/20k trig det pf15522/20k; TRIGTIER; FRb0/20k p15522/20k | S | trusted-lift | Class overload defined via String overload | TP | 1 |
| 132 | poolB | 03 Lang-50-Arja | o | oracle:date-pattern-equivalence | implicit vs explicit pattern | lat+shad | S | contract-backed | after setDefault(L), overloads must match pattern | TP | 1 |
| 133 | poolB | 03 Lang-50-Arja | o | oracle:dateinstance-default-locale | style=0 tz=Africa/Abidjan | lat+shad; FRb999/1000 | S | trusted-lift | javadoc "in the default locale" + trusted test | TP | 1 |
| 134 | poolB | 03 Lang-50-Arja | o | relation dateInstance_matches_explicit_default_locale_overload | — | RR trig det pf20k/20k; TRIGTIER; FRb8666/20k p20k/20k | S | trusted-lift | fenced locales (US/GERMANY); trusted tests pin | TP | 1 |
| 135 | poolB | 04 Lang-60-Arja | o | oracle:contains-readonly-index | contains changed indexOf 1→-1 | lat+shad; screendec | S | contract-backed | query must not change later indexOf on unchanged content | TP | 1 |
| 136 | poolB | 04 Lang-60-Arja | o | oracle:contains-readonly | capacity 43→6, text unchanged | lat+shad | S | contract-backed | completed query changing capacity while text same impossible | TP | 1 |
| 137 | poolB | 05 Chart-7-Arja | o | oracle:max-middle-maximum | idx=1 chosenMid=-992183 maxMid=-986629 | lat+shad; muted; screendec | S | consistency | recomputes max middle from object's own stored periods | TP | 1 |
| 138 | poolB | 05 Chart-7-Arja | o | relation max_middle_index_points_to_a_maximum_middle | — | RRb2764/20k; pf2783/20k | S | trusted-lift | index must point at maximal midpoint; trusted test | TP | 1 |
| 139 | poolB | 06 Closure-92-SequenceR | o | oracle:lifted-testProvideInIndependentModules4 | expected=varapps={};… | fires-on-buggy-same-check; FRb817/817; UNIVSCREEN | U | format-freedom | check's literal malformed (missing ;) vs trusted output | FN | 1 |
| 140 | poolB | 06 Closure-92-SequenceR | o | oracle:seed-regression | expected=varapps={};… | fires-on-buggy-same-check; FRb999/1000 | U | broken-check | hard-codes wrong expected string | FN | 1 |
| 141 | poolB | 06 Closure-92-SequenceR | o | oracle:lifted-test | expected=varapps={};… | fires-on-buggy-same-check; FRb999/1000 | U | format-freedom | malformed expected literal | FN | 1 |
| 142 | poolB | 06 Closure-92-SequenceR | o | relation invalid_provide_name_reports_error | — | RR trig det pf7004/7004; TRIGTIER; FRb8067/8067 p7004/7004 | U | other | `stem+"..bad"` lexes as valid tokens per shown verifyProvide | FN | 1 |
| 143 | poolB | 10 Closure-62-Jaid | c | oracle:end-of-line-caret | expected=javascript/complex.js:1: ERROR… | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | format-freedom | hard-codes all-space caret padding for any fuzzed line | FP | 1 |
| 144 | poolB | 10 Closure-62-Jaid | c | oracle:ground-truth | expected=javascript/complex.js:1:ERROR-… | fires-on-buggy-same-check; FRb999/1000 | U | preexisting/identical | test literal under different setup (fresh Compiler); fires on buggy too | FP | 1 |
| 145 | poolB | 10 Closure-62-Jaid | c | oracle:end-of-line-caret | expected caret after entire line | lat+shad; FRb999/1000 | S | trusted-lift | test-pinned caret-at-end contract; throws skipped | FP | 1 |
| 146 | poolB | 10 Closure-62-Jaid | c | relation formatError_end_of_line_caret | — | RR; pf4133/20k; FRb20k/20k p4133/20k | U | format-freedom | tab-preserving caret padding legal vs hard-coded spaces | FP | 1 |
| 147 | poolB | 10 Closure-62-Jaid | c | relation formatError_warning_only_level_differs | — | RR trig det pf0/20k; TRIGTIER | U | format-freedom | no contract that warning formatting = error formatting ± token | FP | 1 |
| 148 | poolB | 11 Chart-26-Jaid | c | oracle:range-axis-stable | range axis changed across draw | diffreplay-laundering; lat+shad; IDENT; muted:fires-buggy; FRb975/1000 | U | lazy-state | rendering may legitimately update persistent axis state | FP | 1 |
| 149 | poolB | 11 Chart-26-Jaid | c | oracle:bar3d-null-info | draw(g2, rect, null, null) threw | diffreplay-laundering; fires-on-buggy-same-check; buggyscan-fired-at-acceptance; FRb997/1000 | S | trusted-lift | test-pinned "draw with null info must not throw" (kept despite fires-on-buggy + laundering facts) | FP | 1 |
| 150 | poolB | 11 Chart-26-Jaid | c | relation dataset_addValue_double_matches_number | — | RR trig det pf9573/20k; TRIGTIER; FRb9760/20k p9573/20k | U | lazy-state | Number subtype preservation → equals false is legal | FP | 1 |
| 151 | poolB | 15 Chart-19-ACS | c | oracle:query-no-mutation | domainCount 1→2 | lat+shad; FRb999/1000 | U | broken-check | check brackets a deliberate mutator call (setDomainAxis) | TN | 1 |
| 152 | poolB | 16 Closure-38-SequenceR | o | oracle:identifier-family | expected=byte- -0.0 actual= | lat+shad; IDENT; muted:fires-buggy; FRb999/1000 | U | parse-error-latitude | `in`/`byte` reserved → parse error → empty print | FN | 2 |
| 153 | poolB | 16 Closure-38-SequenceR | o | oracle:minus_negative_int_separator | expected=x- -11000 actual=x- -11E3 | lat+shad; muted | U | format-freedom | -11E3 is legal canonicalization of -11000 | FN | 1 |
| 154 | poolB | 17 Closure-70-Jaid | c | crash:IllegalStateException | escaped from TypeCheck | diffreplay-generic-preexisting; IDENT | U | preexisting/identical | buggy build throws same exception at same input | TN | 1 |
| 155 | poolB | 17 Closure-70-Jaid | c | oracle:alpha-rename | expected warning not found after rename | lat+shad; IDENT; muted:fires-buggy; screendec | U | parse-error-latitude | sanitizeIdentifier yields reserved-word names; not semantics-preserving | TN | 1 |
| 156 | poolB | 18 Lang-22-DeepRepair | c | relation reduced_half_denominator_collapses_to_whole | — | RR; pf6012/20k; FRb3877/20k p6012/20k | S | trusted-lift | getReducedFraction(2w,2) must equal whole-number fraction | FP | 1 |
| 157 | poolB | 19 Lang-63-Arja | o | relation utc_period_equals_duration_for_day_and_lower_tokens | — | RR; pf11511/20k; FRb10437/20k p11511/20k | U | invented-generalization | "undocumented equivalence"; calendar decomposition latitude (source pins UTC) | FN | 1 |
| 158 | poolB | 20 Math-30-CapGen | c | oracle:asymptotic-from-u | p disagrees with asymptotic formula | lat+shad | S | contract-backed | documented asymptotic p-value from same U | FP | 1 |
| 159 | poolB | 20 Math-30-CapGen | c | oracle:boundary-family | even/odd family near overflow boundary | lat+shad; IDENT; muted:fires-buggy; FRb819/819; UNIVSCREEN | U | invented-generalization | extrapolates result>0.1 from n=1500 to n=46341 | FP | 1 |
| 160 | poolB | 20 Math-30-CapGen | c | oracle:formula-large-product | expected=0.0 u=3.25e9 | lat+shad; screendec | S | consistency | recomputed documented p-value from public U | FP | 1 |
| 161 | poolB | 20 Math-30-CapGen | c | relation asymptotic-pvalue-matches-u-formula-large-product | — | RR trig det pf1014/1014; TRIGTIER; FRb505/1057 p1014/1014 | U | other (alt-definition) | tie-heavy inputs; tie-corrected asymptotic legal | FP | 1 |
| 162 | poolB | 20 Math-30-CapGen | c | relation u-statistics-complement-under-swap | — | RR trig det pf13503/20k; TRIGTIER; FRb13571/20k p13503/20k | U | other (alt-definition) | mannWhitneyU returns max of complementary Us | FP | 1 |
| 163 | poolB | 21 Math-39-Arja | c | oracle:fuzzed-upper | t=0.01 above end=1e-6 | lat+shad | U | other | adaptive integrator may probe outside interval during step-size estimation | TN | 1 |
| 164 | poolB | 21 Math-39-Arja | c | oracle:integrate-returns-end-time-split | expected=0.007803 actual=0.007802999999999999 | lat+shad | U | tolerance-floor | adjacent double from step accumulation | TN | 1 |
| 165 | poolB | 21 Math-39-Arja | c | oracle:integrate-returns-target-time | expected=0.006849 actual=0.006848999999999999 | lat+shad; screendec | U | tolerance-floor | last-bit rounding; bitwise equality demanded | TN | 2 |
| 166 | poolB | 21 Math-39-Arja | c | oracle:integrate-returns-mid-time | 0.00663255 vs +1ulp | lat+shad | U | tolerance-floor | bitwise-exact equality of accumulated double | TN | 1 |
| 167 | poolB | 21 Math-39-Arja | c | oracle:semigroup-linear-ode | one-shot vs restarted diff ~1.3e-9 | lat+shad | U | tolerance-floor | threshold 1e-9+1e-5·scale too tight for adaptive DP853 | TN | 1 |
| 168 | poolB | 22 Math-65-CapGen | c | oracle:chiSquare-weightedResidualSum | expected=40.0 actual=36.457… | lat+shad; screendec | S | consistency | same-implementation consistency after successful optimize | FP | 1 |
| 169 | poolB | 22 Math-65-CapGen | c | relation chiSquare_matches_weighted_residual_sum | — | RR trig det pf10409/20k; TRIGTIER; FRb13689/20k p10409/20k | S | consistency | documented chi² contract | FP | 1 |
| 170 | poolB | 23 Math-68-Arja | o | oracle:fr-seed-1-rms-exact | expected RMS=6.9988… was 4.9489… | IDENT | U | preexisting/identical | same value on unpatched build; not the fix's duty | TP | 1 |
| 171 | poolB | 23 Math-68-Arja | o | oracle:seed-p1 | expected p1=0.257829976764542 | lat+shad | S | trusted-lift | direct lift of trusted test; observed diff >> noise | TP | 1 |
| 172 | poolB | 23 Math-68-Arja | o | oracle:fr-1-rms | expected RMS=6.9988… was 4.9489… | IDENT | U | preexisting/identical | buggy build also completes with same RMS | TP | 1 |
| 173 | poolB | 24 Math-73-ACS | o | oracle:threeArgAndFourArgAgreeOnUniqueRoot | 1.42e-11 vs other approximation | lat+shad; screendec | U | tolerance-floor | overloads only promise root within 1e-6; no agreement contract | FN | 4 |
| 174 | poolB | 24 Math-73-ACS | o | relation threeArgAndFourArgAgreeOnUniqueRoot | — | RR trig det pf12542/20k; TRIGTIER; FRb15377/20k p12542/20k | U | tolerance-floor | two nonzero approximations of same root within accuracy | FN | 1 |
| 175 | poolB | 25 Math-73-Arja | c | relation unique_endpoint_root_returned_by_solve4 | — | RR trig det pf20k/20k; TRIGTIER; FRb20k/20k p20k/20k | S | observed-impossible | f(x)=x-min with unique zero at endpoint; must return it | FP | 1 |
| 176 | poolB | 26 Math-74-Arja | o | oracle:exact-final-state-separate | expected=-132.0 tol=1.32E-6 | shad | U | other (tolerance) | ~1.2e-7 relative deviation from theoretical value legitimate | TP | 1 |
| 177 | poolB | 26 Math-74-Arja | o | relation integrate_returns_requested_end_time_without_events | — | RRb0/20k; pf326/20k | S | trusted-lift | no-events integrate must stop at requested t | TP | 1 |
| 178 | poolB | 26 Math-74-Arja | o | relation integrate_reaches_requested_end_time_no_events | — | RRb0/20k; pf865/20k | S | contract-backed | API "return stop time" behavior | TP | 1 |
| 179 | poolB | 26 Math-74-Arja | o | relation zero_derivative_direct_vs_two_leg | — | RRb0/20k; pf1666/20k | S | observed-impossible | zero-derivative 1D ODE must return end time/state | TP | 1 |
| 180 | poolB | 27 Math-82-HDRepair | o | oracle:constraint-permutation-invariance | reorder gave 10.0 vs 11.5 | lat+shad; muted; screendec | S | observed-impossible | constraint reordering cannot change LP optimum | TP | 1 |
| 181 | poolB | 30 Math-104-Elixir | o | oracle:probability-range-p | p=1.0000000000000053 | lat+shad; screendec | U | tolerance-floor | ~5e-15 overshoot of [0,1]; zero-tolerance bound | FN | 2 |
| 182 | poolB | 30 Math-104-Elixir | o | oracle:pq-complement | p vs 1-q differ tiny | lat+shad; screendec | U | tolerance-floor | independent doubles within default tolerance | FN | 1 |
| 183 | poolB | 30 Math-104-Elixir | o | oracle:q-cf-sum | P+Q=1 on CF paths | lat+shad; FRb999/1000 | U | tolerance-floor | 1e-9 gate on epsilon-controlled overloads; observed 4e-8 | FN | 2 |
| 184 | poolB | 30 Math-104-Elixir | o | oracle:probability-p | p in [0,1] exact | lat+shad; screendec | U | tolerance-floor | 7e-15 rounding overshoot | FN | 1 |
| 185 | poolB | 30 Math-104-Elixir | o | oracle:fuzzed-complement | P=1-Q a=25.5 | lat+shad; screendec | U | tolerance-floor | custom-precision overloads independent approximations | FN | 1 |
| 186 | poolB | 30 Math-104-Elixir | o | relation p_q_complement_for_valid_inputs | — | RRb80/20k; pf45/20k | U | tolerance-floor (+fires-on-buggy noted) | API promises approximations only (DEFAULT_EPSILON=10e-9) | FN | 1 |
| 187 | poolB | 30 Math-104-Elixir | o | relation regularized_gamma_outputs_are_probabilities | — | RRb683/20k; pf730/20k | U | tolerance-floor | rounding overshoot outside [0,1] legitimate | FN | 1 |
| 188 | width5 | 01 Math-2-Arja | o | relation hypergeom-mean-formula | — | RR trig det pf8872/20k; TRIGTIER; FRb8902/20k p8872/20k | S | contract-backed | documented n*m/N mean | TP | 1 |
| 189 | width5 | 02 Lang-41-Arja | o | oracle:packageName-class-vs-canonical | Class vs canonical-string package | lat+shad; screendec | U | other (alt-definition) | canonical-name string alone can't disambiguate nested classes | TP | 1 |
| 190 | width5 | 02 Lang-41-Arja | o | oracle:pkg-class-canonical-agreement | must agree for real class | lat+shad | S | observed-impossible | for observed java.util.Map.Entry input the values are determined | TP | 1 |
| 191 | width5 | 02 Lang-41-Arja | o | relation packageName_class_overload_agrees_with_canonical_string_over | — | RR trig det pf3186/20k; TRIGTIER | S | trusted-lift | two views of same documented package name | TP | 1 |
| 192 | width5 | 03 Lang-50-Arja | o | oracle:default-vs-explicit-date | default vs explicit instances | lat+shad | S | contract-backed | documented default-locale behavior after setDefault(de_DE) | TP | 1 |
| 193 | width5 | 03 Lang-50-Arja | o | oracle:default-vs-explicit-pattern | patterns must match | lat+shad | S | contract-backed | explicit documentation of default locale use | TP | 1 |
| 194 | width5 | 03 Lang-50-Arja | o | relation dateInstance_defaultLocale_overloadAgreement | — | RR trig det pf20k/20k; TRIGTIER; FRb9994/20k p20k/20k | S | trusted-lift | docs + trusted tests pin | TP | 1 |
| 195 | width5 | 03 Lang-50-Arja | o | relation dateInstance_default_overload_agrees_with_explicit_default_l | — | RR trig det pf20k/20k; TRIGTIER; FRb9906/20k p20k/20k | S | trusted-lift | same formatter locale and pattern required | TP | 1 |
| 196 | width5 | 04 Lang-60-Arja | o | oracle:contains-readonly-capacity | capacity 34→1 | lat+shad; screendec | U | lazy-state | purity contract "not shown by API"; compaction legal | FN | 1 |
| 197 | width5 | 04 Lang-60-Arja | o | oracle:contains-readonly-capacity | capacity 37→4 | lat+shad; screendec | S | observed-impossible | capacity is explicit observable state w/ separate minimizeCapacity mutator (later neutralized by universal screen; leg still FN) | FN | 1 |
| 198 | width5 | 04 Lang-60-Arja | o | oracle:contains-capacity | capacity 43→6 | lat+shad; screendec | U | lazy-state | contains only promises boolean; shrink legal | FN | 1 |
| 199 | width5 | 04 Lang-60-Arja | o | oracle:constructed-absence | deleted suffix char still present | lat+shad; FRb20k/20k; screendec | U | broken-check | fallback `kept="SAFE"` reintroduces needle 'S' | FN | 2 |
| 200 | width5 | 04 Lang-60-Arja | o | relation contains_does_not_change_capacity | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | U | lazy-state | contains "could legally" call minimizeCapacity first | FN | 1 |
| 201 | width5 | 05 Chart-7-Arja | o | relation adding_higher_end_but_lower_middle_does_not_change_maxMiddle | before=1 after=2 delta=-1000000 | lat+shad; screendec | S | observed-impossible | strictly smaller middle can't change max index | TP | 1 |
| 202 | width5 | 05 Chart-7-Arja | o | crash:IndexOutOfBoundsException | Index 1 out of bounds for length 1 (clone()) | diffreplay-muted-same-crash; PATCH-INTRODUCED | S | contract-backed | crash from clone() on validly-built object; patch introduced it | TP | 1 |
| 203 | width5 | 05 Chart-7-Arja | o | oracle:higher-end-lower-middle | before=1 after=2 | lat+shad; screendec | S | contract-backed | documented max-middle semantics for concrete periods | TP | 1 |
| 204 | width5 | 05 Chart-7-Arja | o | oracle:overload-eq | equivalent add overloads unequal series | lat+shad; IDENT; muted:fires-buggy; screendec | U | lazy-state | Number-subtype preservation legal | TP | 1 |
| 205 | width5 | 05 Chart-7-Arja | o | relation adding_higher_end_but_lower_middle_does_not_change_maxMiddle | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | S | trusted-lift | trusted testGetMaxMiddleIndex pins behavior | TP | 1 |
| 206 | width5 | 05 Chart-7-Arja | o | relation adding_higher_middle_but_lower_end_must_change_maxMiddle | — | RRb0/20k trig det pf20k/20k; TRIGTIER; FRb0/20k p20k/20k | S | trusted-lift | uniquely greatest middle must win | TP | 1 |

## (b) Aggregate: verdict × leg-label (raw 228 verdicts)

| cell | count | meaning | hypothetical/grounds distribution |
|---|---|---|---|
| UNSOUND on `_o` | 97 | drift-kill OR correct kill of a bad check | tolerance-floor 34, lazy-state 15, format-freedom 11, parse-error-latitude 9, other/alt-definition 8, invented-generalization 7, preexisting/identical 7, broken-check 4, timezone-env 2 |
| SOUND on `_o` | 61 | correct catch kept | contract-backed 28, trusted-lift 21, observed-impossible 10, consistency 2 |
| UNSOUND on `_c` | 44 | correct dismissal of a firing on a correct patch | tolerance-floor 13, other/alt-definition 13, format-freedom 7, preexisting/identical 5, lazy-state 3, broken-check 1, parse-error-latitude 1, invented-generalization 1 |
| SOUND on `_c` | 26 | **false accusation kept** | contract-backed 11, trusted-lift 7, consistency 6, observed-impossible 2 |

Leg-level: all 26 SOUND-on-`_c` verdicts sit in FP legs (0 in TN legs); the 44
UNSOUND-on-`_c` split 21 TN / 23 FP (dismissals in FP legs were outvoted by a SOUND
sibling). On `_o`: 60/61 SOUND verdicts are in TP legs; 75/97 UNSOUND are in FN legs. Of
the 23 FN `_o` legs with judged firings, **22 had every verdict UNSOUND** — the FN
mechanism is essentially always judge kill, not missing firings. The single exception:
width5 Lang-60 got one SOUND keep (row 197) that a downstream `universal_screen_entry
matched=True` then neutralized → still FN.

Fact-presence by cell (raw counts): shadowed-replay (no per-input attribution)
accompanies 54 U/`_o`, 32 S/`_o`, 29 U/`_c`, 13 S/`_c` — the dominant evidence state
everywhere (148/228). IDENTICAL-ON-BOTH accompanies 15 U/`_o`, 11 U/`_c`, but also 2
SOUND-on-`_o` (DUTY exception applied, both TP: rows 32, 89) and 1 SOUND-on-`_c` (row 55
— kept despite the terminal fact, contributing to an FP). `buggy 0/N` fire-rate: 14
S/`_o` vs 6 U/`_o`. PATCH-INTRODUCED occurred twice total: once kept (width5 Chart-7
crash, TP) and once dismissed (pool30 Closure-70 `_c`, correctly — harness had built two
same-named functions).

## (c) Drift-kill candidates and FP keeps

**UNSOUND on `_o` despite `patch-introduced` or `buggy 0/N → patched high/N` facts
(drift-kill candidates):**

1. pool30 `16 Closure-38` — relation `subtraction_positive_integer_has_no_extra_separator` — b0/20k, trig 2/2 det, p20k/20k — killed on "printer could emit optional separator" (format-freedom) → leg FN.
2. night20 `08 Closure-38` — relation `minus_positive_zero_has_no_forced_space` — b0/20k, trig 2/2, p20k/20k — killed on canonical-spelling freedom → leg FN.
3. night20 `08 Closure-38` — relation `minus_positive_integer_has_no_forced_space` — same fact shape — killed on "could print `x- 1`" → leg FN.
4. width5 `04 Lang-60` — relation `contains_does_not_change_capacity` — b0/20k, trig 2/2, p20k/20k — killed on "contains could call minimizeCapacity first" → leg FN. (Same leg's SOUND keep on the same property was separately screened out.)
5. night20 `12 Math-74` — relation `constant-derivative-forward-backward-roundtrip` — b0/20k, p7824/20k — killed on fp-reversibility tolerance (leg TP anyway via siblings).
6. night20 `03 Lang-50` — relation `dateInstance_default_overload_agrees_with_explicit_default_l` — b0/20k, p20k/20k — JUSTIFIED kill: the relation compares `Locale` objects with `!=` (identity), a genuine check bug (leg TP anyway).

Rows 1–4 share a signature: **silent on buggy across 20k inputs, deterministic 2/2 on
the failing test's own literals on patched, ~100% patched fire-rate** — and each was
killed by an uncorroborated "a correct implementation could…" hypothetical, with the
[trigger-tier fact]'s dismiss-pushing wording and the [fire-rate fact]'s "100% indicts
the check" wording both pushing toward UNSOUND. These are the population's cleanest
drift-kills.

**SOUND on `_c` with weak or contradicting facts (the 26 FP keeps), three profiles:**
- *Shadowed-only (no per-input attribution):* 13 keeps on soundness-alone reasoning —
  the chi² (Math-65), Mann-Whitney (Math-30) and Brent-endpoint (Math-73) families
  recur across runs, plus Closure-62 caret keeps.
- *Kept despite facts pointing at pre-existing behavior:* pool30 Closure-62 `seed-test`
  and night20 Closure-62 `ground-truth` (fires-on-buggy-same-check), pool30/poolB
  Chart-26 `gantt-null-info`/`bar3d-null-info` (fires-on-buggy; poolB's also carried a
  laundering-generic-cause differential fact and a buggy-scan acceptance-time firing),
  night20 Math-30 `canonical-parity-closed-form` (IDENTICAL-ON-BOTH + muted-fires-buggy,
  still kept). In every one, trusted-test-lift provenance overrode the mechanical fact —
  the exact inversion of the "mechanical facts outrank provenance" rule.
- *Trigger-tier relation keeps:* Math-65 chi² relations (×4 across poolA/poolB/night20),
  Math-30 asymptotic/p-value relations, Math-73 `unique_endpoint_root_returned_by_solve4`,
  Lang-22 `reduced_half_denominator_collapses_to_whole`, Closure-62 `end-of-line-caret` —
  TRIGTIER present yet kept because the judge found the contract documented. Notably the
  SAME chi² relation was judged SOUND in poolA/poolB/night20 and UNSOUND in pool30 (rows
  85–86 vs 117/169/60) on the same evidence shape — pure verdict variance (retro-#3).

## (d) WHY vs what the check's own source pins (environment cases)

1. night20 `09 Lang-63` — `oracle:day-shift-invariant` (2 firings, both UNSOUND,
   timezone-env). The WHY invokes a DST transition; the harness does NOT pin TimeZone,
   so environment is nominally in play — but the observed firings differ by 2 DAYS in a
   `dd` field under an exact 24h millis shift; a DST transition (≤1h) cannot produce
   that. The counterexample fails the judge's own step-4b (must reproduce the observed
   firing) yet was accepted, twice. Leg FN.
2. pool30 `19 Lang-63` — `period_and_duration_agree_for_days_and_lower_in_utc` and
   poolB `19 Lang-63` — `utc_period_equals_duration_for_day_and_lower_tokens` (both
   UNSOUND). Both relation sources explicitly pass `DateUtils.UTC_TIME_ZONE` and
   restrict formats to day-and-lower tokens — precisely to fence out timezone/calendar
   variation. The WHYs kill them anyway on calendar-decomposition latitude, i.e. they
   invoke the freedom the pinned UTC + token fencing was designed to exclude. Both FN.
3. The Lang-50 locale family is the positive control: 14/15 verdicts across runs
   correctly note the harness pins `Locale.setDefault(...)` and keep SOUND; the only
   UNSOUND kills there target genuine check bugs (object-identity `!=` compare,
   cached-instance identity assumption).

## Anomalies to account for in any weighting rule

- **[fire-rate fact] denominator bug:** night20 Math-68 shows "buggy 2997/1000 = 300%"
  (multi-case oracle counted 3 firings per input). Normalize per-input before any rule
  keys on rates.
- **Trusted-test fact is universal** (228/228 evidence blocks) — zero discriminating
  power as presence/absence; only what the WHY does with it (lift vs extrapolation)
  discriminates.
- **Dominant evidence state is shadowed replay** (148/228 verdicts, no per-input
  attribution) — cells differ mainly in what the judge does under ignorance; that is
  where a weighting rule has the most room to act (`buggy 0/N + deterministic trigger
  replay` was overridden by hypotheticals in 4 of the 6 UNSOUND cases it appeared in).
- **A SOUND keep can be silently undone downstream:** width5 Lang-60's kept firing was
  discarded by `universal_screen_entry matched=True` after the verdict — judge verdicts
  alone don't fully determine leg outcome.
