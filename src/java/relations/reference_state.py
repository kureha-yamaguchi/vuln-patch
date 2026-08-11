"""p1b step 3 — THE FIRING-STATE READING (docs/p1b-design-2026-08-11.md §1–§2).

Station: a pure peer of `reference_impl.py`. Its one public reading is
consumed by `reference_impl.reference_verdict_gate` (both judge doors,
`src/java/run.py`) and by nothing else.

Failure mode it targets, in the gate's own words. Every archived abstention
of the shape "a firing at a different input is expected to differ …
corroboration needs the reference evaluated at the firing's own input — the
8.4 extension" is the gate naming its missing input. p1a recorded that input
(`__consumed` draws, `__rcvstate` receiver fields). This module turns the
recorded line into the reference's ARGUMENTS, so the question stops being
"does this firing happen to match the reference's test-state values" and
becomes "at the state where this check condemned the patched build, does an
independent implementation condemn it too".

WHAT IS PURE HERE AND WHAT IS NOT
---------------------------------
Everything except the evaluation itself. `reference_firing_reading` takes an
`evaluate` callable — `(arg_literals, receiver_label) -> (printed, why)` —
so the split, the parse, the shape rules, the mapping and the three-way
comparison are unit-testable with a mocked reference, and the JVM lives in
the caller (`run.py`, reusing `build_reference_call_driver` + `run_reference`
exactly as the admission chain does).

EVERY FAILURE IS AN ABSTENTION WITH ITS OWN GREPPABLE REASON
------------------------------------------------------------
`ABSTENTION_REASONS` is the closed list. 8.39's lesson restated: a guard that
was ACTIVE and found nothing must be distinguishable in a trace from a guard
that never had an input, so no path here returns a bare "nothing found".
"""
import re
from typing import Dict, List, Optional, Tuple

#: The fact tag this reading writes, joining the existing `[fact:…]`
#: vocabulary in `evidence_facts`.
REFERENCE_FIRING_FACT_TAG = '[fact:reference-at-firing-state]'

#: The four readings a COMPLETED evaluation can produce (design §1 step 5).
REFERENCE_FIRING_READINGS = ('agrees-with-patched', 'agrees-with-check',
                             'degenerate', 'mutually-inconsistent')

#: A ONE-ELEMENT terminal set, deliberately. `agrees-with-check` pushes
#: toward accusation and is therefore advisory: at this site the finding is
#: already kept, so making corroboration a fact rather than a verdict costs
#: nothing and closes the only route by which p1b could ADD a conviction.
REFERENCE_FIRING_DISMISSING = frozenset({'agrees-with-patched'})

#: Everything that is not one of the four readings. Closed list, greppable.
ABSTENTION_REASONS = (
    'no-reference', 'no-reference-for-this-observable', 'no-observable',
    'no-matching-receiver', 'ambiguous-receiver', 'receiver-never-ran',
    'non-finite-state', 'truncated-state', 'state-lost-in-consumed-payload',
    'parameters-not-read-by-method', 'unmappable', 'reference-unrunnable',
    'degenerate', 'mutually-inconsistent')

_CONSUMED_MARK = ' __consumed='
_STATE_MARK = ' __rcvstate '


# ---------------------------------------------------------------------------
# §1 step 1 — the three-way split, which is ALSO a repair of a live hazard.
#
# The state printer emits `name=value` pairs on the SAME line as the
# relation's own observables, so a `key=value` parser run over the whole line
# reads `residuals=`, `cost=`, `rows=` as if the relation had observed them.
# Today's `reference_verdict_gate` calls `observed_values` on the whole line.
# No archived void is attributable to it, but the collision is live since p1a
# landed, and it is closed here at the same site that needs the split anyway.
# ---------------------------------------------------------------------------

def split_firing_line(line) -> Tuple[str, str, str]:
    """`(message, consumed, state)` — the three regions of a `[relfire]` line.

    `message` is everything before the first ` __consumed=` / ` __rcvstate `
    marker: the relation's own observables and nothing else. `consumed` is
    the typed draw log. `state` is the whole ` __rcvstate …` tail, one or
    more blocks. Missing regions come back as ''. Pure, never raises.
    """
    text = str(line or '')
    ci, si = text.find(_CONSUMED_MARK), text.find(_STATE_MARK)
    if ci < 0 and si < 0:
        return text, '', ''
    if ci < 0:
        return text[:si], '', text[si:]
    if si < 0:
        return text[:ci], text[ci + len(_CONSUMED_MARK):], ''
    if ci < si:
        return (text[:ci], text[ci + len(_CONSUMED_MARK):si], text[si:])
    return text[:si], text[ci + len(_CONSUMED_MARK):], text[si:ci]


_BLOCK_HEAD_RE = re.compile(r'^\s*([\w$]+)\s*:\s*([\w$.]+)\s*(.*)$')
_FIELD_START_RE = re.compile(r'(?:^|\s)([A-Za-z_$][\w$]*)=')

#: A scalar the reflection printer can emit. It prints PRIMITIVES and
#: primitive arrays only, so a non-array field's value is always a number, a
#: boolean, a single char, or `null` — which is what makes the well-formed
#: test below a truncation detector rather than a guess.
_SCALAR_OK_RE = re.compile(
    r'^(?:null|true|false|NaN|-?Infinity|-?\d[\w.+-]*|.)$')


def parse_state_blocks(state_region) -> List[Dict]:
    """One dict per ` __rcvstate <label>:<SimpleName> f=v f=v …` block.

    Each carries `label`, `type`, `fields` (`{name: printed}`) and
    `truncated` (the names whose printed value is NOT self-terminating, per
    §2.4). A truncated field keeps its text in `fields` so a caller can quote
    it; it must never be shortened into a usable value, which is why the
    reading refuses it by NAME rather than by repairing it.

    Pure, never raises.
    """
    out: List[Dict] = []
    for chunk in str(state_region or '').split(_STATE_MARK):
        if not chunk.strip():
            continue
        head = _BLOCK_HEAD_RE.match(chunk)
        if not head:
            continue
        label, typ, rest = head.group(1), head.group(2), head.group(3)
        starts = [(m.start(1), m.group(1), m.end()) for m in
                  _FIELD_START_RE.finditer(rest)]
        fields: Dict[str, str] = {}
        order: List[str] = []
        for i, (_s, name, val_at) in enumerate(starts):
            end = starts[i + 1][0] if i + 1 < len(starts) else len(rest)
            fields[name] = rest[val_at:end].strip()
            order.append(name)
        truncated = [n for i, n in enumerate(order)
                     if not _self_terminating(fields[n],
                                              last=(i == len(order) - 1))]
        out.append({'label': label, 'type': typ, 'fields': fields,
                    'order': order, 'truncated': truncated})
    # Only the FINAL field of the FINAL block can be cut by a line that simply
    # STOPS — every earlier field is followed by another `name=`, which is the
    # whitespace terminator §2.4 asks for. (A bracketed value can still be cut
    # anywhere by the printer's 400-char-per-field cap, so the bracket-balance
    # verdict above stands wherever it fired.)
    for b in out[:-1]:
        last = b['order'][-1] if b['order'] else None
        if (last and last in b['truncated']
                and not (b['fields'][last] or '').startswith('[')):
            b['truncated'].remove(last)
    return out


def _self_terminating(printed, last=False) -> bool:
    """§2.4. A value is usable only if it terminates itself.

    Truncation carries NO marker — a sweep of every archived `[relfire]` line
    found zero `…`/`...` terminators; a cut line simply stops. So the test is
    structural: a bracketed form must balance and end in `]`, and a scalar in
    the one position a cut can land on (the last field) must be a well-formed
    primitive token. The corpus's two real shapes are exactly these — mid
    array (`objective=[-3.2010762339444594, -3.201`) and mid token
    (`permutation=nul`).
    """
    p = (printed or '').strip()
    if not p:
        return False
    if p.startswith('['):
        return p.endswith(']') and p.count('[') == p.count(']')
    if not last:
        return True
    return bool(_SCALAR_OK_RE.match(p))


#: §2.2. A counter field whose zero says the receiver never computed.
_ITERATION_FIELD_RE = re.compile(
    r'^(?:iterations|iterationCount|evaluations|evaluationCount)$')


def receiver_never_ran(block) -> bool:
    """§2.2 — 8.31's uninitialised-receiver rule, kept byte-for-byte.

    The rule needs its OWN predicate because the obvious implementation does
    not work on the shape that motivated it. `java_literal` already refuses a
    printed `null`, so any MAPPED field printing null abstains for free — but
    the corpus line has `residuals=[-1000.0] residualsWeights=[0.01] rows=1`,
    all three non-null, so a `(residuals, residualsWeights, rows)` reference
    would evaluate cleanly and return a confident number computed from a
    state the optimizer never produced.

    So the predicate reads the WHOLE receiver, not the mapped subset: an
    iteration/evaluation counter at 0 AND at least one reference-typed field
    printing `null`.
    """
    fields = (block or {}).get('fields') or {}
    zeroed = any(_ITERATION_FIELD_RE.match(k) and (v or '').strip() == '0'
                 for k, v in fields.items())
    nulled = any((v or '').strip() == 'null' for v in fields.values())
    return bool(zeroed and nulled)


_NON_FINITE_RE = re.compile(r'(?<![\w.])(?:NaN|-?Infinity)(?![\w.])')


def non_finite_printed(printed) -> bool:
    """§2.3 — NaN/±Infinity anywhere in a printed scalar or array.

    Stricter than the pipeline's arithmetic elsewhere, deliberately. The
    corpus shape is a lazily-computed cache that has NOT been computed
    (`numericalVariance=NaN numericalVarianceIsCalculated=false`), and
    feeding NaN to the reference produces NaN, which `_vals_match` treats as
    agreement — so an uncomputed cache could manufacture an
    `agrees-with-patched` dismissal out of two blanks. The companion boolean
    is recorded in the detail so the class stays countable, but it gates
    nothing: the plain non-finite rule already covers it and is impossible to
    get subtly wrong.
    """
    return bool(_NON_FINITE_RE.search(str(printed or '')))


_CONSUMED_QUOTED_RE = re.compile(r'q:"(?:[^"\\]|\\.)*"')


def consumed_entries(consumed) -> List[Tuple[str, str]]:
    """`[(type_tag, value)]` from the typed draw log, quoted payloads masked.

    The payloads carry raw fuzzer bytes — literal `]`, `"` and control
    characters — so they are masked before anything is split or counted. §2.5
    is emphatic that this log NEVER supplies a parameter: `__rcvstate` gives
    named fields and `match_parameters` gives a name-resolved mapping, while
    this gives an ordered list of values with no role. Using the second where
    the first is missing trades a named binding for a positional guess, which
    is the roll-8 defect the mapper exists to refuse.
    """
    masked = _CONSUMED_QUOTED_RE.sub('q:"<payload>"', str(consumed or ''))
    out = []
    for part in masked.split('|'):
        part = part.strip()
        if not part:
            continue
        tag, _sep, val = part.partition(':')
        out.append((tag.strip(), val.strip()))
    return out


def consumed_crosscheck(consumed, literals) -> str:
    """§2.5's one-directional cross-check. Records; changes no reading."""
    entries = consumed_entries(consumed)
    if not entries:
        return 'consumed-silent: the firing recorded no typed draw log'
    vals = {v for _t, v in entries}
    scalars = [lit for lit, typ in literals if '[' not in (typ or '')]
    hit = [s for s in scalars if s.rstrip('fLd') in vals]
    if hit:
        return (f'consumed-consistent: {hit[:3]} also appear among the '
                f'{len(entries)} recorded draws')
    return (f'consumed-silent: no scalar parameter value appears among the '
            f'{len(entries)} recorded draws — recorded only, never used to '
            f'supply a parameter')


# ---------------------------------------------------------------------------
# §1 step 2 — the observed and claimed values, from the MESSAGE region only.
# ---------------------------------------------------------------------------

def message_values(message) -> Tuple[Optional[str], Optional[str], str]:
    """`(observed, claimed, why)` — the two numbers the firing itself printed.

    observed = the value under a non-reference key (`_reference_key` already
    excludes `expect*` and `tol|eps|tolerance|epsilon`), else the
    `actual=/got=/was=`-tagged number, else the bare trailing number — in
    that order, first hit wins. claimed = the value under the single
    `expect*` key.
    """
    from java.relations.evidence_facts import (_reference_key, _TAGGED_NUM_RE,
                                               _TRAILING_NUM_RE,
                                               observed_values)
    text = str(message or '')
    # Harness firings echo the method CALL (`getChiSquare()=6.25…`) and the
    # k=v parser requires a bare identifier before `=`; normalised here for
    # the same reason the gate normalises it, and nowhere global.
    vals = observed_values(re.sub(r'\(\s*\)\s*=', '=', text))
    vals = {k: [v if v.startswith('[') else v.split()[0] for v in vs if v]
            for k, vs in vals.items()}
    observed = None
    for k, vs in vals.items():
        if vs and not _reference_key(k):
            observed = vs[0]
            break
    if observed is None:
        m = _TAGGED_NUM_RE.search(text)
        if m:
            observed = m.group(1)
        else:
            m = _TRAILING_NUM_RE.search(text.rstrip())
            if m:
                observed = m.group(1)
    expect_keys = [k for k in vals if re.search(r'expect', k, re.I) and vals[k]]
    claimed = vals[expect_keys[0]][0] if len(expect_keys) == 1 else None
    why = (f'observed={observed!r} claimed={claimed!r} from the message '
           f'region ({len(vals)} key(s) parsed, state fields excluded by the '
           f'three-way split)')
    return observed, claimed, why


# ---------------------------------------------------------------------------
# THE READING
# ---------------------------------------------------------------------------

def _result(reading, reason, **kw) -> Dict:
    out = {'reading': reading, 'reason': reason, 'observed': None,
           'claimed': None, 'reference': None, 'receiver': None,
           'detail': {}}
    out.update({k: v for k, v in kw.items() if k != 'detail'})
    out['detail'].update(kw.get('detail') or {})
    return out


def reference_firing_reading(fired_line, admitted, evaluate,
                             declaring_types=None) -> Dict:
    """Evaluate the admitted reference AT THIS FIRING'S OWN STATE. Pure.

    `admitted` is one admission record from the per-observable store; it
    already carries `sig`, `mapping` (parameter -> canonical field names, in
    call order, RESOLVED AT ADMISSION so the gate can never disagree with the
    screen about what the arguments mean), `matched`, `fields_read` and
    `reads_what_method_reads`.

    `evaluate(arg_literals, receiver_label) -> (printed_value, why)` runs the
    reference. Everything else here is arithmetic and parsing.

    Returns a dict — never raises, never guesses:

      ``reading``   one of `REFERENCE_FIRING_READINGS`, or 'abstain'.
      ``reason``    for an abstention, one of `ABSTENTION_REASONS`, first
                    token of the sentence so it stays greppable.
      ``observed`` / ``claimed`` / ``reference``  the three numbers.
      ``detail``    what was seen, for the fact and the trace.
    """
    from java.relations.reference_impl import _values_agree
    from java.relations.reference_run import java_literal, parse_parameters

    if not admitted:
        return _result('abstain',
                       'no-reference: no admitted reference to evaluate at '
                       'this firing\'s state')
    message, consumed, state = split_firing_line(fired_line)
    detail = {'consumed': consumed[:400],
              'admitted_for': (admitted or {}).get('method')}

    # §2.7 — the parameters must be fields the method ACTUALLY READS. On
    # Math-65 the two plausible bindings give OPPOSITE verdicts:
    # (residuals, residualsWeights, rows) reproduces the patched build,
    # (targetValues, objective, residualsWeights) reproduces the relation,
    # and both map onto real canonical fields. `reads_what_method_reads` was
    # computed at admission from the buggy method's own body (rank 2, our
    # deterministic read); None means the body was not visible, which is
    # undetermined and never a failure.
    if admitted.get('reads_what_method_reads') is False:
        return _result(
            'abstain',
            'parameters-not-read-by-method: the reference is declared over '
            f'{list(admitted.get("mapping") or [])}, and the disputed '
            f'method\'s own body reads {list(admitted.get("fields_read") or [])}'
            ' — a binding onto fields the computation does not consume can '
            'compute a confident number for a different quantity',
            detail=detail)

    if not state:
        if consumed:
            # §2.4's corruption shape: the raw bytes in a `q:"…"` payload cut
            # the line before the state block would have appeared.
            return _result(
                'abstain',
                'state-lost-in-consumed-payload: the firing recorded its '
                'consumed draws but no `__rcvstate` block — either the check '
                'constructs nothing capturable (the stateless-receiver '
                'ceiling) or the line was cut inside a quoted draw payload',
                detail=detail)
        return _result(
            'abstain',
            'unmappable: the firing line carries no recorded receiver state, '
            'so there is no state at which to evaluate the reference',
            detail=detail)

    observed, claimed, obs_why = message_values(message)
    detail['message'] = message[:300]
    if observed is None or claimed is None:
        return _result(
            'abstain',
            'no-observable: the firing\'s message region does not resolve '
            'both an observed and a single claimed value — ' + obs_why,
            observed=observed, claimed=claimed, detail=detail)

    mapping = list(admitted.get('mapping') or [])
    params = parse_parameters(admitted.get('sig') or '')
    if not mapping or len(params) != len(mapping):
        return _result(
            'abstain',
            f'unmappable: the admission record maps {len(mapping)} field(s) '
            f'onto a signature declaring {len(params)} parameter(s)',
            observed=observed, claimed=claimed, detail=detail)

    blocks = parse_state_blocks(state)
    detail['receivers'] = [f"{b['label']}:{b['type']}" for b in blocks]
    if not blocks:
        return _result(
            'abstain',
            'no-matching-receiver: the `__rcvstate` region parsed into no '
            'receiver block',
            observed=observed, claimed=claimed, detail=detail)

    # §2.1 rule 1 — TYPE FILTER. The line does not say which receiver the
    # observed value came from, and guessing is the silent-wrong-input class
    # `match_parameters` was hardened against.
    declaring = {str(t) for t in
                 (declaring_types or admitted.get('declaring') or ())}
    survivors = blocks
    if declaring:
        typed = [b for b in blocks if b['type'] in declaring]
        if not typed:
            return _result(
                'abstain',
                f'no-matching-receiver: none of {detail["receivers"][:4]} is '
                f'a declaring type of the disputed method '
                f'({sorted(declaring)[:4]})',
                observed=observed, claimed=claimed, detail=detail)
        survivors = typed
    # §2.1 rule 2 — FIELD-COVER FILTER.
    covered = [b for b in survivors
               if all(f in b['fields'] for f in mapping)]
    if not covered:
        return _result(
            'abstain',
            f'no-matching-receiver: no captured receiver prints every mapped '
            f'field {mapping[:4]}',
            observed=observed, claimed=claimed, detail=detail)

    # §2.1 rule 3 — EVALUATE-BOTH. Two or more survivors turn the ambiguity
    # into a measurement instead of a heuristic, at the cost of one extra JVM
    # run in a case that is rare by measurement.
    values = []
    for block in covered:
        if receiver_never_ran(block):
            return _result(
                'abstain',
                f'receiver-never-ran: receiver `{block["label"]}` prints a '
                f'zero iteration/evaluation count and a null reference field '
                f'— it carries no state the reference can be evaluated at, '
                f'which is 8.31\'s rule kept byte-for-byte',
                observed=observed, claimed=claimed, detail=detail)
        literals = []
        for (typ, _name), field in zip(params, mapping):
            printed = block['fields'].get(field)
            if field in block['truncated']:
                return _result(
                    'abstain',
                    f'truncated-state: mapped field `{field}` is not '
                    f'self-terminating ({(printed or "")[-40:]!r}). A '
                    f'truncated array is never silently shortened — a '
                    f'shorter array changes the row count, changes the sum, '
                    f'and produces a confident wrong number',
                    observed=observed, claimed=claimed, detail=detail)
            if non_finite_printed(printed):
                _companion = [k for k in block['fields']
                              if k.lower().startswith(field.lower())
                              and k != field]
                detail['non_finite_companions'] = {
                    k: block['fields'][k] for k in _companion[:3]}
                return _result(
                    'abstain',
                    f'non-finite-state: mapped field `{field}` prints '
                    f'{(printed or "")[:40]!r}; a non-finite input is the one '
                    f'case where the comparison\'s own agreement semantics '
                    f'point the wrong way (NaN == NaN reads as agreement)',
                    observed=observed, claimed=claimed, detail=detail)
            lit = java_literal(typ, printed)
            if lit is None:
                return _result(
                    'abstain',
                    f'unmappable: mapped field `{field}` ({typ}) printed as '
                    f'{(printed or "ABSENT")[:60]!r}, which reconstructs no '
                    f'Java literal',
                    observed=observed, claimed=claimed, detail=detail)
            literals.append((lit, typ))
        printed_value, run_why = evaluate(literals, block['label'])
        if printed_value is None:
            return _result(
                'abstain',
                f'reference-unrunnable: the reference did not produce a value '
                f'at this state — {run_why}',
                observed=observed, claimed=claimed, detail=detail)
        values.append((block['label'], printed_value))
        detail['consumed_crosscheck'] = consumed_crosscheck(consumed, literals)

    if len(values) > 1 and not all(_values_agree(values[0][1], v)
                                   for _l, v in values[1:]):
        return _result(
            'abstain',
            f'ambiguous-receiver: {len(values)} captured receivers cover the '
            f'mapped fields and the reference computes different values on '
            f'them ({[v for _l, v in values][:3]}) — the ambiguity is '
            f'material, so nothing is established',
            observed=observed, claimed=claimed, detail=detail)

    label, reference = values[0]
    # §1 step 5, the three-way reading. All comparisons go through
    # `_values_agree` -> `evidence_facts._close`, the shipped rounding floor;
    # nothing new is calibrated here.
    r_x = _values_agree(reference, observed)
    r_y = _values_agree(reference, claimed)
    common = dict(observed=observed, claimed=claimed, reference=reference,
                  receiver=label, detail=detail)
    if r_x and r_y:
        # A genuine catch fires where the correct implementation and the
        # patched build DIFFER, so both cannot hold at a real divergence.
        # Where both hold, the difference the relation condemned is below the
        # floor we call agreement everywhere else, and dismissal would be a
        # floor artefact rather than a finding.
        return _result(
            'degenerate',
            'degenerate: the reference agrees with BOTH the patched build\'s '
            'observed value and the relation\'s expected value, so the check '
            'fired on a difference smaller than the rounding floor separates',
            **common)
    if r_x:
        return _result(
            'agrees-with-patched',
            'agrees-with-patched: evaluated at this firing\'s own recorded '
            f'state, the admitted reference computes {reference} — the value '
            f'the PATCHED BUILD produced ({observed}) and not the value the '
            f'relation expected ({claimed}). The relation\'s expected value '
            f'is wrong at this state and the firing is spurious',
            **common)
    if r_y:
        return _result(
            'agrees-with-check',
            'agrees-with-check: evaluated at this firing\'s own recorded '
            f'state, the admitted reference computes {reference} — the value '
            f'the RELATION expected ({claimed}) and not the value the patched '
            f'build produced ({observed}). The conviction is corroborated; '
            f'this is an advisory fact and changes no verdict',
            **common)
    return _result(
        'mutually-inconsistent',
        f'mutually-inconsistent: the reference computes {reference}, a third '
        f'answer agreeing with neither the patched build ({observed}) nor the '
        f'relation ({claimed}) — nothing is established',
        **common)


def reference_firing_fact(reading) -> Optional[str]:
    """The computed fact, or None when nothing was computed.

    Same sentence shape for every completed reading; only the result differs.
    There is deliberately NO dismissal or keep instruction in any branch —
    roll 12 measured nine deliveries of the reference fact and zero
    engagements, and the decision then was to stop persuading. This is in the
    evidence so a human reading the trace can see the arithmetic, and for
    nothing else.
    """
    r = (reading or {}).get('reading')
    if r not in REFERENCE_FIRING_READINGS:
        return None
    d = reading.get('detail') or {}
    return (
        '\n' + REFERENCE_FIRING_FACT_TAG + ' an independent implementation of '
        '`' + str(d.get('admitted_for') or '?') + '`, written from the '
        'DOCUMENTATION alone and admitted by the off-defect screen, was '
        're-evaluated at THIS FIRING\'S OWN recorded receiver state '
        '(receiver `' + str(reading.get('receiver')) + '`):\n'
        '    reference computes: ' + str(reading.get('reference')) + '\n'
        '    the patched build produced: ' + str(reading.get('observed')) + '\n'
        '    the relation expected: ' + str(reading.get('claimed')) + '\n'
        'Reading: ' + str(r) + '.\n'
        'This is a computed comparison at a recorded state, not a verdict.')
