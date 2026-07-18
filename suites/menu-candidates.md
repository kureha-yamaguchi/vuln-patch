# Metamorphic-relation catalog for the R4 menu (consolidated, cited)

Built 2026-07-18 by five parallel literature-mining passes (numerical/math,
string/text, collections, datetime, program-text, web-API, security),
each grounded in primary sources, then consolidated and deduplicated into
`src/java/variation_menu.json` (the operational menu). This file is the
provenance record: what was mined, from where, and the soundness caveats.
(Earlier incremental drafts of this catalog are preserved in git history.)

**62 relation families across 9 input kinds** — 38 universal (hold for
any correct implementation), 24 documented-property (hold only when the
spec asserts the property). Every entry in the JSON carries a checkable
soundness CONDITION and an EXCEPTIONS list; the exceptions are load-
bearing — they are the false-alarm suppressors, drawn from the real edge
cases each paper documents.

## Per-domain sources (verified against the primary papers)

**Numerical / mathematical (19 families).** Chen, Cheung, Yiu,
*Metamorphic Testing: A New Approach for Generating Next Test Cases*,
HKUST-CS98-01 (1998) — the founding sine identities. Murphy, Kaiser, Hu,
*Properties of ML Applications for Use in Metamorphic Testing*, SEKE 2008
— additive/multiplicative/permutative/invertive/inclusive/exclusive.
Kanewala & Bieman, STVR 2016 — 48 numerical functions. Mayer & Guderlei,
COMPSAC 2006 — 16 determinant MRs and the subsumption lesson (MRs that
mirror the implementation's own path are empirically weak — matches this
project's Math-2 experience). Chen/Feng/Tse, PDE MRs, COMPSAC 2002.
Guderlei & Mayer, *Statistical Metamorphic Testing*, 2007. Families: trig
identities, odd/even symmetry, principal-branch inverse round-trip, CDF
monotonicity, distribution axioms, location-scale transforms, permutation
invariance, determinant laws, matrix identities, special functions
(exp/log/gamma/beta), complex arithmetic, number theory / combinatorics,
integration linearity, refinement convergence.

**String / text (14 families).** Hughes, *How to Specify It!* (round-trip,
invariant, model-based); Claessen & Hughes, *QuickCheck*, ICFP 2000;
Segura et al. survey (TSE 2016); Zhou, Xiang, Chen, *MT for Software
Quality Assessment: A Study of Search Engines*, TSE 2016; Unicode UAX #15
/ #29 / #21. Families: encode/decode round-trip, length homomorphism,
concat associativity/identity, split/join inverse (the Java trailing-empty
trap), reverse involution, case-insensitive-search invariance, query-
refinement subset, replace/count consistency, permutation invariance, trim
idempotence, normalization idempotence, escape neutralization, format-parse
locale round-trip. The cross-cutting soundness traps (code-unit vs
code-point vs grapheme; Turkish-i / German-ß case folding; split regex +
trailing-empty drop; trim vs strip; escaper non-idempotence) are recorded
per entry.

**Collections / data structures (15 families).** Murphy et al. 2008;
Hughes 2019 (model-based / insert-insert / insert-find laws); QuickCheck
monoid-law tradition; Segura et al. MET 2019 (subset/cardinality MRPs);
Meyer's Command-Query Separation (for the read-only relation); JDK
contracts. Families: inclusive/exclusive monotonicity, read-only
non-interference (the hidden-state relation that convicts Lang-60),
sibling-agreement (the relation that convicts Lang-41), insert
commutativity, idempotence, identity/monoid laws, round-trip inverse,
model-agreement, subset-under-filter, cardinality conservation, documented
order/stability. The set-vs-bag semantics gate is flagged on every
affected family — the single biggest soundness trap in this domain.

**Datetime (6 families).** Joda-Time User Guide / FAQ / DateTimeZone API;
java.time design; Baeldung DST. Families: instant-zone invariance,
parse/format round-trip, duration monotonic advance, zone-conversion
ordering preservation, DST gap/overlap well-definedness, leap-year
calendar consistency. The dominant false positives — instant-vs-local
category confusion, Period-vs-Duration conflation, DST gap/overlap
non-bijection, Feb-29 clamp non-invertibility, leap seconds unsupported in
Joda — are the exceptions lists (directly relevant: the benchmark's Time
bugs are Joda-Time).

**Program-text (5 families).** Le, Afshari, Su, *Compiler Validation via
Equivalence Modulo Inputs*, PLDI 2014, and the EMI family — Orion (dead-
code prune), Athena (dead-region insertion, OOPSLA 2015), Hermes (live-
code mutation, OOPSLA 2016); Donaldson et al., GraphicsFuzz/GLFuzz,
OOPSLA 2017; Segura CSUR 2018. Families: EMI dead-code prune, EMI dead-
region insertion, EMI live-code mutation, insignificant-whitespace/comment
invariance, formatter idempotence / parse stability. Dominant false
positives — undefined behavior voiding EMI, floating-point freedom, and
(directly relevant to Closure-62) line-number/position outputs changing
with inserted newlines — are recorded as exceptions.

**Web-API (6 families).** Segura, Parejo, Troya, Ruiz-Cortés,
*Metamorphic Testing of RESTful Web APIs*, IEEE TSE 2018 — the six output
patterns (equivalence, equality, subset, disjoint, complete, difference);
Segura et al. MET 2019 — seven query patterns. Families: filter subset,
ordering equivalence, default-parameter equality, disjoint/complete/
difference partitions. Exceptions: pagination/top-N cutoffs, non-
determinism, personalization.

**Security (6 families) — the entries that replaced the deleted stub.**
Bayati Chaleshtari, Pastore, Goknil, Briand, *Metamorphic Testing for Web
System Security*, IEEE TSE 2023 (arXiv:2208.09505) — 76 system-agnostic
MRs, 23 patterns, 39% of OWASP activities and 101 CWE types; and the SMRL
tool paper, ICSE 2020. Families: broken access control (IDOR /
authorization bypass / privilege escalation, CWE-286/22), input
sanitization (XSS/SQLi/code injection, CWE-79/89/94), session integrity
(session fixation, OWASP OTG-SESS-003), workflow precedence (CWE-841),
secure transport / cookie attributes, rate-and-lifetime controls (lockout
/ password aging / CSRF). Each carries the SMRL guard preconditions
(cannotReachThroughGUI, userCanAccess) and the exception suppressors
(admin users, public resources, legitimate rate-limit errors). These are
keyed to the `security` / `web_api` input kinds — they inject only when a
target of that kind is detected (fail-safe), and are recorded here rather
than deleted precisely because "we have no such target today" is not
"never applicable".

## Coverage honesty

Mined: the pattern/survey layer plus, for each domain, its principal
primary sources (the founding papers, the domain surveys, the standard
property catalogs). Not exhaustively mined: every one of the survey's 119
primary studies — deeper domain papers hold more concrete INSTANCE
relations, but the PATTERN layer is close to saturated (the five passes
kept converging on the same families under different names, which is why
dedup collapsed ~66 raw families to 62 distinct entries). If a specific
input kind ever needs more, the next step is a focused mine of that kind's
primary studies, entered through the R4(c) edit-time checklist. The menu
is frozen before the held-out run.

## Governance recap (full contract in semantic-recall-brainstorm.md R4)

Edit-time: provenance from a universal definition / cited paper (never
from a benchmark miss); independently derivable; a checkable condition and
a known-exceptions list required. Run-time: entries inject only for the
mechanically/classifier-detected input kind, capped at 3, universal before
documented, priority-ranked; unknown kind injects nothing. One-time: the
menu is frozen before the held-out run and never edited from held-out
output.
