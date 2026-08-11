"""Generate Jazzer harnesses for a Defects4J bug given a patch from the
ASSERT-KTH/drr dataset. Keep regenerating with the same prompt until a
target number of harnesses compile against the buggy project, then fuzz
the successful harnesses against the patched code to check for overfitting.

Pipeline stages (each lives in its own module):

    PatchSelector       (patches.py)     pick a random patch + d4j checkout
    FailureTestExtractor(failure_test.py) read the d4j bug-triggering test
    TargetAnalyzer      (analysis.py)    parse patch + run fuzz-introspector
    PromptBuilder       (prompts.py)     build the chat-completion prompt
    HarnessGenerator    (llm.py)         call the local LLM
    HarnessBuilder      (build.py)       extract + javac the generated source
    HarnessCampaign     (campaign.py)    loop generate→build until N succeed
    JazzerEnvironment   (jazzer.py)      resolve jazzer jars
    FuzzRunner          (fuzz_runner.py) run harnesses against patched code
    config              (config.py)      env-driven constants

Example usage (choose project_name from Chart/Closure/Lang/Math/Time):
    uv run -m run -o --project_name Lang -n 5 -m 50 --fuzz_timeout 60
"""
import argparse
import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents if (p / 'config.py').exists())))

import config
from java.bug_context.analysis import TargetAnalyzer
from java.harness.build import HarnessBuilder
from java.harness.campaign import HarnessCampaign, CampaignResult
from java.bug_context.crash_input import CrashInputExtractor
from java.bug_context.failure_test import FailureTestExtractor, classify_bug_kind, is_crashing_bug
from java.execution.fuzz_runner import (FuzzRunner, HarnessVerifier, PatchApplyError,
                         PatchedProjectBuilder, TriggerVerificationError)
from java.execution.jazzer import JazzerEnvironment
from llm import (HarnessGenerator, reset_token_usage, token_usage,
                 usage_totals, enable_recording, reset_events, get_events,
                 record_event)
from java.bug_context.patches import DeprecatedBugError, PatchSelector
from java.harness.prompts import PromptBuilder
from java.parsing.java_source import candidate_anchor_literals, expected_assert_literals
from java.relations.judge_decision import adjudicate


# Where a fired message genuinely ENDS in Jazzer output: the stack trace, the
# next exception record, or the libFuzzer banner. Deliberately not "the next
# newline" — see below.
_MSG_END_RE = re.compile(r'\n\s*(?:at\s+\S+\(|==\s|Caused by:|#\d+\s)')


def _extract_oracle_msg(output, oid, cap=20000):
    """Pull the first `[oracle:<id>]` fired message out of a replay's raw
    output, so the SAME check's observed value on the BUGGY build can be
    compared to the patched firing. Returns None when the id's message is not
    present — the caller then treats the comparison as UNKNOWN (never
    over-claims 'identical').

    8.3: this used to cap at 200 characters AND stop at the first newline —
    **both cutters that the batch-8 smoke found on the patched side**, sitting
    here on the buggy side where nobody had looked. Either one silently
    discards the trailing key/value block, which is precisely the observed
    VALUE this function exists to deliver, and which 8.2's authority screen and
    8.20's scope fact both consume.

    So the message now ends where it actually ends — at the stack trace, the
    next exception record, or the libFuzzer banner — and the cap is a runaway
    guard rather than a formatting rule. A bare newline no longer terminates
    it: 8.4's escaping keeps alarms on one line, but relying on that would make
    this function's correctness depend on another component's compliance."""
    if not output or not oid:
        return None
    tag = "[oracle:" + str(oid) + "]"
    idx = output.find(tag)
    if idx < 0:
        return None
    seg = output[idx:idx + cap]
    end = _MSG_END_RE.search(seg)
    if end:
        seg = seg[:end.start()]
    return seg.strip() or None


def _admitted_for(fired, class_ctx, check_src):
    """The admitted reference for THIS firing's own observable, plus an event.

    p1b step 1: admission is now a per-observable store
    (`_reference_impl_fact._admitted_by_method`), so the verdict gate must
    say WHICH reference it is reading and why that one. The lookup is
    recorded at every call — a gate that abstained because the leg admitted
    nothing and a gate that abstained because the leg admitted a reference
    for some OTHER observable are different findings, and the archive could
    not tell them apart.
    """
    from java.relations.reference_impl import admitted_reference_for
    from llm import record_event as _re
    rec, why = admitted_reference_for(
        getattr(_reference_impl_fact, '_admitted_by_method', None),
        fired, '\n\n'.join(class_ctx) if class_ctx else '',
        check_source=check_src)
    _re('deterministic', method='reference-admission-lookup',
        target=(fired or '')[:80],
        output=('admitted reference found' if rec else
                'no admitted reference for this firing'),
        reason=why,
        detail={'admitted': sorted(
            getattr(_reference_impl_fact, '_admitted_by_method', None) or {}),
            'used': (rec or {}).get('method')})
    return rec, why


def _patched_method_names(context) -> list:
    """The methods the PATCH changed, off the pipeline's own diff read.

    `TargetAnalyzer.analyze` maps each changed line to its smallest enclosing
    declaration (`bug_context/analysis.py:_enclosing_methods`, with the regex
    fallback behind it) and every downstream stage that keys on "the patched
    method" already reads this list. READ 2's Fix 2 needs exactly that set and
    computes nothing new. Empty when the context is degraded, which leaves the
    off-defect screen reading as it did before the fix.
    """
    return [fn.func_name for fn in (getattr(context, 'functions', None) or [])
            if getattr(fn, 'func_name', None)]


def _widen_admissions(*, args, checks, class_ctx, failure_tests, builder,
                      buggy_dir, patch_path, package=None, imports=None,
                      patched_methods=None):
    """p1b step 3 part 1: one reference per CONTESTED OBSERVABLE, not one per
    leg. Returns the store's observable keys after widening.

    8.44 measured the constraint the gate runs into: the store retains every
    admission, and there was only ever ONE admission per Math-65 leg to
    retain, none of them `getRMS`, none at all on the Chart legs. The cause
    is the request, not the store — the chain asks about the observable the
    first firing it sees disputes and stops at the first fact. So the request
    is re-aimed here, right after relation screening, where the KEPT checks
    are known and before any judging has happened.

    Nothing about admission changes: the same prompt (parameterised by target
    method, which is what it always was), the same screen, the same
    corroboration attribution, the same pin check, the same fail-closed
    discards. Every attempt records requested -> admitted/rejected with the
    chain's own reason, because a widening that asked and was refused and a
    widening that never asked are different findings.

    Fails OPEN: any error leaves the store exactly as it was.
    """
    from llm import record_event as _re
    from java.relations.reference_impl import admission_key, widening_targets
    ctx = '\n\n'.join(class_ctx) if class_ctx else ''
    store = getattr(_reference_impl_fact, '_admitted_by_method', None)
    if store is None:
        store = _reference_impl_fact._admitted_by_method = {}
    cap = getattr(config, 'P1B_MAX_REFERENCES', 3)
    # READ 2 Fix 1: the enumeration is scoped to the patched class's own
    # no-argument readers, and what that scope DROPPED is recorded here —
    # a rank spent on a constructor or a collaborator's method is the thing
    # that kept `getRMS` (rank 8) behind a cap of 3.
    _excluded = []
    targets, why = widening_targets(store, checks, ctx, cap,
                                    excluded=_excluded)
    _re('deterministic', method='reference-widening', target='enumerate',
        output=(f'{len(targets)} observable(s) to widen onto' if targets
                else 'nothing to widen'),
        reason=why,
        detail={'targets': [admission_key(t) for t in targets],
                'already_admitted': sorted(store), 'cap': cap,
                'checks_read': len([c for c in (checks or []) if c]),
                'excluded': [{'key': e['key'], 'why': e['why']}
                             for e in _excluded][:12],
                'excluded_count': len(_excluded)})
    for method in targets:
        key = admission_key(method)
        _re('deterministic', method='reference-widening', target=method,
            output=f'reference REQUESTED for observable `{key}`',
            reason='a kept relation probes this observable and the leg holds '
                   'no reference for it; the existing generation + screening '
                   '+ admission chain runs unchanged, aimed at this method')
        try:
            _reference_impl_fact(
                args=args, fired='', class_ctx=class_ctx,
                failure_tests=failure_tests, builder=builder,
                buggy_dir=buggy_dir, patch_path=patch_path,
                trusted_values=[], package=package, imports=imports,
                check_source=None, target_methods=[method],
                patched_methods=patched_methods)
        except Exception as exc:
            _re('deterministic', method='reference-widening', target=method,
                output=f'widening REJECTED for observable `{key}`',
                reason=f'the chain raised {type(exc).__name__}: {exc}'[:300])
            continue
        _step, _step_why = (
            getattr(_reference_impl_fact, '_last_outcome', None) or {}
        ).get(method, (None, ''))
        _admitted = key in store
        _re('deterministic', method='reference-widening', target=method,
            output=(f'reference ADMITTED for observable `{key}`' if _admitted
                    else f'widening REJECTED for observable `{key}`'),
            reason=((store[key].get('screen_why') or 'admitted') if _admitted
                    else (f'{_step} — {_step_why}' if _step else
                          'the chain produced no admission and recorded no '
                          'step for this target')),
            detail={'admitted_observables': sorted(store),
                    'last_step': _step})
    return sorted(store)


def _firing_state_reading(fired, admitted, evidence, builder, buggy_dir):
    """p1b step 3 part 2: the admitted reference RE-EVALUATED at this firing's
    own recorded receiver state. The reading dict, or None.

    The decision logic is pure and lives in `relations/reference_state.py`;
    this is the adapter that owns the JVM. It reuses the admission chain's own
    driver builder (`build_reference_call_driver`, which hoists array literals
    into static fields to stay under the JVM's 64KB bytecode-per-method cap)
    and `run_reference`, and it hands `run_reference` the compiled-reference
    directory the admission recorded, so a leg compiles its reference once and
    rebuilds only the driver per firing.

    Fails OPEN in the only direction that matters: any error returns None,
    and a None reading leaves the verdict gate reading exactly as it does
    today, byte for byte.
    """
    from llm import record_event as _re
    from java.relations.reference_impl import firing_state_reading_applies
    from java.relations.reference_state import (reference_firing_fact,
                                                reference_firing_reading)
    from java.relations.reference_run import (build_reference_call_driver,
                                              observable_key, run_reference)
    try:
        applies, why = firing_state_reading_applies(evidence)
        if not applies:
            _re('deterministic', method='reference-firing-state',
                target=(fired or '')[:80],
                output='firing-state reading NOT RUN', reason=why)
            return None
        if not admitted:
            _re('deterministic', method='reference-firing-state',
                target=(fired or '')[:80],
                output='firing-state reading NOT RUN',
                reason='no-reference: this conviction carries '
                       '[fact:rate-indiscriminate], but the leg admitted no '
                       'reference for the observable it disputes')
            return None
        method = admitted.get('method')
        declared = (admitted.get('matched') or {}).get(method) or method
        runs = {'n': 0}

        def _evaluate(literals, label):
            runs['n'] += 1
            try:
                driver_src = build_reference_call_driver(
                    'ReferenceImpl', [(method, declared)], list(literals))
            except (ValueError, TypeError) as e:
                return None, f'driver source invalid: {type(e).__name__}: {e}'
            obs, run_why = run_reference(
                builder, buggy_dir, admitted.get('src') or '', driver_src,
                reference_dir=admitted.get('ref_dir') or None)
            if not obs:
                return None, run_why
            by_key = {observable_key(k): v for k, v in obs.items()}
            vals = by_key.get(observable_key(method)) or []
            if not vals:
                return None, (f'the reference ran but printed no value for '
                              f'`{method}` (printed {sorted(obs)[:4]})')
            return vals[0], run_why

        reading = reference_firing_reading(fired, admitted, _evaluate)
        _re('deterministic', method='reference-firing-state',
            target=(fired or '')[:80],
            output=f"reading: {reading.get('reading')}",
            reason=reading.get('reason'),
            detail={'observed': reading.get('observed'),
                    'claimed': reading.get('claimed'),
                    'reference': reading.get('reference'),
                    'receiver': reading.get('receiver'),
                    'jvm_runs': runs['n'],
                    'fact': reference_firing_fact(reading),
                    **(reading.get('detail') or {})})
        return reading
    except Exception as exc:                     # pragma: no cover - defensive
        _re('deterministic', method='reference-firing-state',
            target=(fired or '')[:80],
            output='firing-state reading RAISED',
            reason=f'{type(exc).__name__}: {exc}'[:300])
        return None


def _reference_impl_fact(*, args, fired, class_ctx, failure_tests, builder,
                         buggy_dir, patch_path, trusted_values,
                         package=None, imports=None, check_source=None,
                         target_methods=None, patched_methods=None):
    """8.2: generate, screen and compare an independent reference. Fact or None.

    Every exit records an event with its REASON. A step that produced nothing
    must SAY nothing-and-why, because the stage read-out is per-event and an
    absent step is indistinguishable from a step that silently failed -- the
    failure this cycle met six times.

    Fails CLOSED at every point: no disputed observable, prompt refused for an
    implementation leak, generation error, compile/run failure, screen discard,
    pin-check discard -> None.

    `target_methods` (p1b step 3) NAMES the observables to attempt instead of
    detecting them from `fired`. The chain is already per-method — the
    reference prompt names its target through build_reference_prompt's
    `method=` argument, "Implement `<method>` from its specification" — so
    widening changes only which methods are asked about, never the asking.
    With targets given, EVERY one is attempted (the detection path stops at
    the first fact, because there its job is to produce a fact for one
    firing; here the job is to fill the store).
    """
    from llm import record_event as _record_event, HarnessGenerator as _HG
    # The last `reference-impl` event per target, so the widening wrapper can
    # say WHY an attempt was rejected without the 15 early exits inside
    # `_attempt` each having to report twice. Reading the event the chain
    # already records keeps one description of each failure.
    _outcomes = {}

    def _re(kind, **kw):
        if kw.get('method') == 'reference-impl' and kw.get('target'):
            _outcomes[kw['target']] = (kw.get('output'), kw.get('reason'))
        return _record_event(kind, **kw)
    _reference_impl_fact._last_outcome = _outcomes
    from java.relations.reference_impl import (
        admission_key, admit_reference,
        disputed_observables, exempt_patch_touched, pin_check,
        pins_for_disputed, reference_comparison_fact, screen_reference,
        test_corroboration_pins, too_thin_to_screen)
    from java.relations.reference_gen import (
        ImplementationLeak, build_reference_prompt, sibling_observables,
        strip_bodies)
    from java.relations.reference_run import (
        build_reference_call_driver, build_state_twin_driver, canonical_state,
        declared_observable_names, declared_signature, extract_test_dependencies,
        extract_test_setup, fields_read_by, java_literal, match_parameters,
        merge_declared_parameter_names, parse_parameters,
        match_observable_names, plausible_class_names, run_reference,
        run_twin, test_package, types_declaring)

    ctx = '\n\n'.join(class_ctx) if class_ctx else ''
    if target_methods is not None:
        disputed = [m for m in target_methods if m]
        if not disputed:
            return None
    else:
        try:
            # Stage 2 (Math-2): the fired MESSAGE may print only
            # actual=/expected= while the check SOURCE calls the disputed
            # method by name — the same artifact the judge receives, so
            # scanning it adds no new authority.
            disputed = disputed_observables(fired, ctx,
                                            check_source=check_source)
        except Exception as e:
            _re('deterministic', method='reference-impl', target='detect',
                output=f'ERROR: {type(e).__name__}', reason=str(e)[:200])
            return None
        if not disputed:
            _re('deterministic', method='reference-impl', target='detect',
                output='no disputed observable',
                reason='the firing (and its check) names no method the context '
                       'declares; the mechanism has nothing to reimplement')
            return None

    # The chain is expensive (a generation plus three JVM runs) and BOTH judge
    # doors may ask about the same method several times per leg. One process =
    # one leg, so a process-level memo is run-local by construction.
    memo = getattr(_reference_impl_fact, '_memo', None)
    if memo is None:
        memo = _reference_impl_fact._memo = {}
    # THE ADMISSION STORE, one slot PER OBSERVABLE (p1b step 1). Same
    # lifetime argument as the memo: one process = one leg, so function-level
    # storage is run-local by construction and nothing pools across runs.
    store = getattr(_reference_impl_fact, '_admitted_by_method', None)
    if store is None:
        store = _reference_impl_fact._admitted_by_method = {}

    def _attempt(method):
        """One candidate, full chain; a fact or None. Every early exit is
        an event with its reason, exactly as before — only the CALLER
        changed (stage-4 roll 2): `disputed[0]` was the whole attempt
        policy, and on the SOFix leg the productive candidate sat in both
        lists, attempted in neither."""
        # The screening surface is resolved BEFORE the generation (roll 8
        # pre-walk): a broken declaring-type parse should cost zero model calls,
        # and the prompt must NAME the siblings — rolls 6/7/8 all declared one
        # countable sibling against a bar of three, so every mechanically
        # perfect roll would still have discarded at the screen. Siblings are
        # scoped to the RECEIVER's own type, or the twin calls a collaborator's
        # method on the optimizer and cannot compile (VM re-walk #4:
        # getPoint/getPointRef/getArgument).
        declaring = types_declaring(ctx, method)
        if declaring and not plausible_class_names(declaring):
            _re('deterministic', method='reference-impl', target=method,
                output='declaring-type PARSE BROKEN — DISCARDED',
                reason=f'parsed type names are not plausible Java classes: '
                       f'{sorted(declaring)[:6]} — this is an extractor failure, '
                       f'not a property of this leg',
                detail={'parsed': sorted(declaring)[:6]})
            return None
        siblings = sibling_observables(ctx, method, declaring_types=declaring)
        # READ 2 Fix 2: an observable the PATCH CHANGED is on-defect for every
        # reference on this class, not only for a reference aimed at it. It
        # comes out of the screening surface here, once, so the early count
        # bar and `screen_reference`'s own count cannot disagree about what
        # the off-defect set is. `siblings` itself is untouched — the twin
        # still prints them, the corroboration pins still attach to them.
        screened_siblings, _exempted = exempt_patch_touched(siblings,
                                                            patched_methods)
        _re('deterministic', method='reference-impl', target=method,
            output='screening surface resolved',
            reason=f'{len(siblings)} computed sibling observable(s) on the '
                   f'receiver\'s own type (stored settings excluded — they '
                   f'agree for free)'
                   + (f'; {len(_exempted)} of them PATCH-TOUCHED '
                      f'({_exempted}) and therefore not screened on — the '
                      f'buggy build is the wrong answer key where the defect '
                      f'lives' if _exempted else ''),
            detail={'siblings': siblings[:8],
                    'patch_touched_exempted': _exempted,
                    'screened_siblings': screened_siblings[:8],
                    'declaring_types': sorted(declaring)[:4]})

        try:
            skeleton = strip_bodies(ctx)
            # Javadoc arrives with its /** */ delimiters ALREADY stripped
            # (assemble_class_context) — filtering chunks on '/**' selects
            # nothing. Doc-ish means bare-star lines or @param/@return/@throws.
            _docish = re.compile(r'^\s*\*|@param|@return|@throws', re.M)
            msgs = build_reference_prompt(
                method=method, skeleton=skeleton,
                docs=[strip_bodies(c) for c in (class_ctx or [])
                      if _docish.search(c)][:4],
                failing_test='\n\n'.join(
                    (getattr(ft, 'method_source', '') or '') for ft in failure_tests),
                other_tests=[], shown_examples=None, siblings=siblings)
        except ImplementationLeak as e:
            _re('deterministic', method='reference-impl', target=method,
                output='REFUSED: implementation leak', reason=str(e)[:300])
            return None
        except Exception as e:
            _re('deterministic', method='reference-impl', target=method,
                output=f'prompt build ERROR: {type(e).__name__}', reason=str(e)[:200])
            return None

        try:
            src = _HG(model=args.model or config.LOCAL_LLM_MODEL,
                      temperature=0.0, top_p=1.0).generate(msgs) or ''
        except Exception as e:
            _re('deterministic', method='reference-impl', target=method,
                output=f'generation ERROR: {type(e).__name__}', reason=str(e)[:200])
            return None
        if 'class' not in src:
            _re('deterministic', method='reference-impl', target=method,
                output='generation produced no class', reason=src[:200])
            return None
        _re('deterministic', method='reference-impl', target=method,
            output='reference generated', detail={'chars': len(src)})

        # The generator declares signature AND observable list; we read both.
        sig = declared_signature(src)
        if sig is None:
            _re('deterministic', method='reference-impl', target=method,
                output='no declared signature — DISCARDED',
                reason='the reply carried no `// compute(<types>)` line, so the '
                       'driver cannot know how to call it')
            return None
        _re('deterministic', method='reference-impl', target=method,
            output='signature declared', detail={'signature': sig})
        # PARAMETER NAMES FROM THE DECLARATIONS (roll 8 pre-walk). Roll 8's
        # comment line was bare types while its own `compute_*` declarations
        # named both parameters — in the OPPOSITE order from the buggy body's
        # read order, so the positional fallback would have fed the reference
        # swapped arrays: same type, same length, runs cleanly, computes
        # garbage. A name in the model's own declaration is evidence; the
        # read-order guess is for references that name nothing anywhere.
        _canon = canonical_state(ctx)
        _named = merge_declared_parameter_names(src, sig, _canon)
        if _named != sig:
            _re('deterministic', method='reference-impl', target=method,
                output='parameter names recovered from the declarations',
                reason='the `// compute(...)` line is bare types; every '
                       'compute_* declaration agrees on one named list',
                detail={'declared': sig, 'named': _named})
            sig = _named
        ref_names = declared_observable_names(src)
        # NAME NORMALIZATION (VM re-walk #6). The model wrote `compute_chiSquare`
        # where the chain wanted `compute_getChiSquare` and the reference was
        # discarded on SPELLING, having implemented the observable correctly.
        # The codebase already matches `chiSquare` to `getChiSquare`
        # (_methods_named_by, since P0); the reference matcher now does too, and
        # the driver calls what the model DECLARED while keying by the canonical
        # name. Mechanism, not an instruction: the prompt already asked.
        matched = match_observable_names(ref_names, [method] + siblings)
        if method not in matched:
            _re('deterministic', method='reference-impl', target=method,
                output='reference omits the disputed observable — DISCARDED',
                reason=f'declared: {ref_names[:8]} — none normalizes to '
                       f'`{method}` (accessor prefixes are stripped both ways)')
            return None
        targets = list(matched)          # canonical keys, in wanted order
        _re('deterministic', method='reference-impl', target=method,
            output='reference observables matched',
            detail={'matched': {k: v for k, v in list(matched.items())[:6]},
                    'declared_only': [n for n in ref_names
                                      if n not in matched.values()][:4]})
        # THE SCREEN'S COUNT BAR, DECIDED HERE (roll 8 pre-walk): the shared
        # siblings are known the moment the observables are matched, and the
        # run can only shrink that set. The late path bought the twin build and
        # two JVM runs before `screen_reference` said "1 shared; 3 required".
        thin, thin_why = too_thin_to_screen(matched, screened_siblings)
        _re('deterministic', method='reference-impl', target=method,
            output=('reference too thin to screen — DISCARDED' if thin else
                    'screen bar reachable'), reason=thin_why)
        if thin:
            return None

        # Signature -> canonical state fields, nominally. Unmappable = discard,
        # never a guessed call (roll 4: five attempts, five different signatures).
        mapping, map_why = match_parameters(parse_parameters(sig), _canon,
                                            fields_read_by(ctx, method, _canon))
        _re('deterministic', method='reference-impl', target=method,
            output=('signature mapped' if mapping else
                    'signature unmappable — DISCARDED'), reason=map_why)
        if not mapping:
            return None

        # THE STATE TWIN: everything is compared at the failing test's own state.
        test_src = next((getattr(ft, 'method_source', '') or ''
                         for ft in failure_tests
                         if getattr(ft, 'method_source', None)), '')
        # Receiver by DECLARING TYPE, never by usage pattern (VM re-walk #2);
        # `declaring` was resolved above, before the screening surface.
        setup, receiver, setup_why = extract_test_setup(test_src, method,
                                                        siblings, declaring)
        _re('deterministic', method='reference-impl', target=method,
            output=('twin setup extracted' if setup else
                    'twin underivable — DISCARDED'), reason=setup_why,
            detail={'declaring_types': sorted(declaring)[:4]})
        if not setup:
            return None
        # Fixture classes live only in the TEST FILE (`new Circle()`); the twin
        # cannot compile without them, so they ride along as package-private
        # top-level classes. The test file's own imports come with them.
        test_file_src = ''
        _tf = next((getattr(ft, 'source_path', None) for ft in failure_tests
                    if getattr(ft, 'source_path', None)), None)
        if _tf:
            try:
                with open(_tf, encoding='utf-8', errors='replace') as fh:
                    test_file_src = fh.read()
            except OSError:
                test_file_src = ''
        t_imports, helpers = extract_test_dependencies(test_file_src, setup)
        # The twin is the test method, so it must resolve names the way the test
        # did: emitted INTO the test's own package (the class under test is
        # referenced by simple name because they share it).
        twin_pkg = test_package(test_file_src) or package
        try:
            twin_src = build_state_twin_driver(
                setup, receiver, [method] + siblings, mapping,
                package=twin_pkg, imports=(t_imports or imports),
                helper_classes=helpers)
        except (ValueError, TypeError) as e:
            _re('deterministic', method='reference-impl', target=method,
                output='twin source invalid — DISCARDED', reason=str(e)[:200])
            return None
        _re('deterministic', method='reference-impl', target=method,
            output='twin built',
            detail={'helpers': len(helpers), 'imports': len(t_imports or []),
                    'package': twin_pkg, 'receiver': receiver})
        buggy_vals, twin_why = run_twin(builder, buggy_dir, twin_src)
        _re('deterministic', method='reference-impl', target=method,
            output=('buggy twin ran' if buggy_vals else
                    'buggy twin FAILED — DISCARDED'), reason=twin_why)
        if not buggy_vals:
            return None

        # The reference's inputs come from the twin itself (reflection-printed).
        params = parse_parameters(sig)
        lits = []
        for (typ, _n), field in zip(params, mapping):
            printed = (buggy_vals.get(f'__param_{field}') or ['ABSENT'])[0]
            lit = java_literal(typ, printed)
            if lit is None:
                _re('deterministic', method='reference-impl', target=method,
                    output='reference inputs unrecoverable — DISCARDED',
                    reason=f'state field `{field}` ({typ}) printed as '
                           f'{printed[:80]!r}')
                return None
            lits.append((lit, typ))
        try:
            driver_src = build_reference_call_driver(
                'ReferenceImpl', list(matched.items()), lits)
        except (ValueError, TypeError) as e:
            _re('deterministic', method='reference-impl', target=method,
                output='driver source invalid — DISCARDED', reason=str(e)[:200])
            return None
        # `out` carries the compiled-reference directory back out (p1b step 3;
        # the addendum's §11.4 left it None). With it in the record, the gate
        # re-evaluates this reference at a firing's state by rebuilding only
        # the DRIVER — §8.3's cost envelope assumes one javac of the
        # reference per leg, not one per firing.
        _refout = {}
        obs, why = run_reference(builder, buggy_dir, src, driver_src,
                                 out=_refout)
        _re('deterministic', method='reference-impl', target=method,
            output=('reference ran' if obs else 'reference DISCARDED'), reason=why)
        if not obs:
            return None

        # THE SCREEN: reference vs the BUGGY build, on siblings only — the
        # disputed point is on-defect by definition and cannot vouch for itself.
        buggy_obs = {k: v for k, v in buggy_vals.items()
                     if not k.startswith('__')}
        off = [k for k in screened_siblings if k in obs and k in buggy_obs]
        # OPTION B (user decision 2026-08-07): a sibling the failing test itself
        # shows diverging is a rigged screen question — the buggy build is the
        # wrong answer key there. The pin attaches only where the failure
        # message's observed value appears verbatim in the twin's buggy print
        # (state identity), and it re-grades a disagreement only when the
        # reference matches the test's literal AND buggy fails it.
        corro = test_corroboration_pins(
            [getattr(ft, 'failure_message', '') or '' for ft in failure_tests],
            [getattr(ft, 'method_source', '') or '' for ft in failure_tests],
            buggy_obs, siblings)
        if corro:
            _re('deterministic', method='reference-impl', target=method,
                output='test-corroboration pins resolved',
                reason='failing-test asserted value(s) attributed to defect-'
                       'reached sibling(s) by verbatim state match',
                detail={'pins': corro})
        ok, screen_why = screen_reference(obs, buggy_obs, off_defect_keys=set(off),
                                          test_corroboration=corro)
        _re('deterministic', method='reference-impl', target=method,
            output=('screen ADMITTED' if ok else 'screen DISCARDED'),
            reason=screen_why,
            detail={'construct': (buggy_vals.get('__construct0') or ['?'])[0],
                    'off_defect_shared': len(off),
                    'off_defect_exempted': _exempted})
        if not ok:
            return None

        # VALIDATOR 3: at test state the failing test's pinned answer applies to
        # the disputed observable — the bug-copying catch. Roll 11 (defect 19):
        # blanket `{method: trusted_values}` mapped every test literal onto the
        # disputed observable, so a correctly-diverging reference was discarded
        # against a NEIGHBOURING assertion's literal. Pins now attach only by
        # the same attribution discipline as corroboration (state identity, or
        # an assertion calling the disputed method directly); `trusted_values`
        # is deliberately NOT consulted here — its provenance is exactly the
        # misattribution vector.
        pins_d = pins_for_disputed(
            method,
            [getattr(ft, 'failure_message', '') or '' for ft in failure_tests],
            [getattr(ft, 'method_source', '') or '' for ft in failure_tests],
            buggy_obs)
        _re('deterministic', method='reference-impl', target=method,
            output=('disputed-observable pins resolved' if pins_d
                    else 'no pin attaches to the disputed observable'),
            reason=('attributed by state identity or a direct assertion on the '
                    'disputed method' if pins_d else
                    'no failure-message or direct-assertion attribution — the '
                    'pin check will ABSTAIN rather than borrow a neighbouring '
                    'assertion\'s literal'),
            detail={'pins': {k: v[:4] for k, v in pins_d.items()}})
        pin_ok, pin_why = pin_check(obs, pins_d, [method])
        _re('deterministic', method='reference-impl', target=method,
            output=('pin-check PASSED' if pin_ok else 'pin-check DISCARDED'),
            reason=pin_why)
        if not pin_ok:
            return None

        # ADMISSION is complete (screen + corroboration + pin). Stored at
        # admission, not at fact emission: the gate needs the reference's
        # validated VALUES, which stand even if the patched twin later fails
        # and no fact is emitted.
        #
        # ONE SLOT PER OBSERVABLE (p1b step 1). The record carries what
        # re-evaluating this reference at a FIRING's state will need, so the
        # gate build never has to recompute — and never gets to disagree with
        # the screen about what the reference's arguments mean.
        #
        # `fields_read_by` is read again here rather than hoisted out of the
        # mapper call above: it is a pure regex read of the same context, and
        # the mapper's call site is pinned literally by a seam test that
        # exists because production once ran a whole roll with the read-order
        # argument silently missing.
        _read_fields = [n for _t, n in fields_read_by(ctx, method, _canon)]
        _admitted_record = {
            'method': method, 'obs': obs, 'buggy': buggy_obs,
            # for re-evaluation at a firing's state (design §3)
            'src': src, 'sig': sig, 'mapping': list(mapping),
            'matched': dict(matched),
            # for the standing sentence the fact writes
            'screen_why': screen_why, 'screened': len(off),
            'siblings': list(siblings),
            # §2.7 material: which fields the DISPUTED METHOD's own body
            # reads. Recorded, NOT gated on — the design places that rule at
            # the gate ("p1b is where a wrong binding turns into a verdict"),
            # and screening on it here would change what admissions the
            # coverage roll is measuring. `None` means the body is not
            # visible, which is undetermined and never a failure.
            'fields_read': _read_fields,
            'reads_what_method_reads': (
                None if not _read_fields
                else all(f in _read_fields for f in mapping)),
            # §2.1's type filter needs to know which receiver in a firing's
            # `__rcvstate` line is the disputed method's own.
            'declaring': sorted(declaring or ()),
            # the compiled reference classes, so the gate rebuilds only the
            # driver per firing (§8.3).
            'ref_dir': _refout.get('ref_dir'),
        }
        _stored, _store_why = admit_reference(store, method, _admitted_record)
        _re('deterministic', method='reference-impl', target=method,
            output=('admission STORED for observable '
                    f'`{admission_key(method)}`' if _stored else
                    'admission SET ASIDE — duplicate observable (keep-first)'),
            reason=_store_why,
            detail={'observable': admission_key(method),
                    'admitted_observables': sorted(store),
                    'mapping': list(mapping),
                    'fields_read': _read_fields,
                    'reads_what_method_reads':
                        _admitted_record['reads_what_method_reads']})

        # THE OTHER SIDE OF THE FACT: the SAME twin on the PATCHED build.
        try:
            pdir = PatchedProjectBuilder().build_patched_dir(buggy_dir, patch_path)
        except Exception as e:
            _re('deterministic', method='reference-impl', target=method,
                output='patched build unavailable — no fact',
                reason=f'{type(e).__name__}: {e}'[:200])
            return None
        patched_vals, ptwin_why = run_twin(builder, pdir, twin_src,
                                           work_subdir='reference_twin_patched')
        _re('deterministic', method='reference-impl', target=method,
            output=('patched twin ran' if patched_vals else
                    'patched twin FAILED — no fact'), reason=ptwin_why)
        if not patched_vals:
            return None
        patched_obs = {k: v for k, v in patched_vals.items()
                       if not k.startswith('__')}

        fact = reference_comparison_fact(
            method, True, screen_why, patched_obs, obs,
            screened_count=len(off))
        _re('deterministic', method='reference-impl', target=method,
            output=('fact emitted' if fact else 'no fact (nothing comparable)'),
            detail={'chars': len(fact or '')})
        return fact

    # ORDERED, BOUNDED FALLBACK (stage-4 roll 2, rule 7): try up to three
    # candidates in the detector's ranked order; each attempt is memoized
    # individually, so across firings a leg spends at most one generation
    # per distinct method and never revisits a failure. Widening (p1b step 3)
    # attempts every NAMED target instead of stopping at the first fact —
    # the store, not the fact, is what it is filling.
    _widening = target_methods is not None
    _first_fact = None
    for method in disputed[:len(disputed) if _widening else 3]:
        if method in memo:
            if memo[method] is not None:
                _re('deterministic', method='reference-impl', target=method,
                    output='memoized result reused',
                    reason='this disputed observable was already resolved '
                           'this leg')
                if not _widening:
                    return memo[method]
                _first_fact = _first_fact or memo[method]
            continue                     # known failure — try the next
        _re('deterministic', method='reference-impl', target=method,
            output=('widening target requested' if _widening
                    else 'disputed observable detected'),
            detail={'candidates': disputed[:4], 'attempting': method})
        memo[method] = None      # every early exit inside leaves None
        fact = _attempt(method)
        memo[method] = fact
        if fact and not _widening:
            return fact
        _first_fact = _first_fact or fact
    return _first_fact if not _widening else None


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate and compile Jazzer harnesses for a "
                    "Defects4J bug from the drr dataset.",
    )
    parser.add_argument("-c", "--correct", action="store_true",
                        help="Flag for semantically correct patch")
    parser.add_argument("-o", "--overfitting", action="store_true",
                        help="Flag for semantically incorrect patch")
    parser.add_argument("--project_name", type=str,
                        help="Choose from Chart/Closure/Lang/Math/Time")
    parser.add_argument("--patch_file", type=str, default=None,
                        metavar="PATH",
                        help="evaluate exactly this .patch file instead of "
                             "randomly sampling one (project/apr_tool/"
                             "bug_id are all derived from its path); "
                             "--project_name is not needed when this is set")
    parser.add_argument("--skip_semantic", action="store_true",
                        help="bail out right after bug-kind classification "
                             "if the bug is semantic (no crash signature), "
                             "before the costly TargetAnalyzer/LLM campaign. "
                             "Default is to run semantic bugs too.")
    parser.add_argument("--language", type=str, nargs='?', default='Java',
                        help='Programming language of project')
    parser.add_argument("--model", type=str, default=None, metavar="DEPLOYMENT",
                        help="model/deployment used for harness "
                             "generation, synthesis and judging. Without it, "
                             "config.LOCAL_LLM_MODEL. E.g. --model gpt-5.4.")
    parser.add_argument("-n", "--target_successes", type=int, default=5,
                        help="Stop once this many harnesses compile "
                             "(default: 5)")
    parser.add_argument("-m", "--max_attempts", type=int, default=50,
                        help="Hard cap on total generation attempts "
                             "(default: 50)")
    parser.add_argument("--max_repair_failures", type=int, default=3,
                        help="maximum number of failures in a row before resetting the prompt context")
    parser.add_argument("--reachable_node_cap", type=int, default=None,
                        metavar="N",
                        help="budget for the root-cause reachable-set BFS: "
                             "max functions to visit (default: config."
                             "REACHABLE_NODE_CAP). Higher = wider neighbourhood "
                             "but slower analysis.")
    parser.add_argument("--reachable_max_depth", type=int, default=None,
                        metavar="D",
                        help="max call-graph depth for the reachable-set BFS "
                             "(default: config.REACHABLE_MAX_DEPTH). Direct "
                             "callees are depth 1.")
    parser.add_argument("--introspector_depth_cap", type=int, default=None,
                        metavar="D",
                        help="cap for fuzz-introspector's method-depth metric "
                             "(default: config.INTROSPECTOR_METHOD_DEPTH_CAP). "
                             "Bounds the otherwise-O(N^2) DFS that stalls on "
                             "large libraries; lower = faster parse.")
    parser.add_argument("--verify_relations", action="store_true",
                        help="before counting a harness that crashed the "
                             "patched code as overfitting evidence, ask an "
                             "LLM critic whether its oracle is SOUND (true for "
                             "any correct implementation). Drops unsound "
                             "findings (invented relations that fire on correct "
                             "code) — a non-cheating false-positive filter. "
                             "Off by default.")
    parser.add_argument("--focused_synthesis", action="store_true",
                        help="run relation synthesis as SEVERAL focused "
                             "passes (documented-formula, @throws, "
                             "sibling-agreement, read-only/invariant) and "
                             "union the screened survivors, instead of one "
                             "broad 'up to N relations' call. Fixes the "
                             "roll-variance where the convicting relation "
                             "(e.g. Math-2's mean-formula) is proposed on "
                             "some rolls and dropped on others. More LLM "
                             "calls per leg, each narrow; the union is "
                             "screened identically so there is no new FP "
                             "risk. Within-leg only — no pooling.")
    parser.add_argument("--fuzz_timeout", type=int, default=60,
                        metavar="SECONDS",
                        help="seconds Jazzer runs per harness against the "
                             "patched code (default: 30; 0 to skip fuzzing)")
    parser.add_argument("--verify_timeout", type=int,
                        default=None, metavar="SECONDS",
                        help="seconds Jazzer runs per harness against the "
                             "BUGGY code to verify it triggers before "
                             "accepting it (default: config."
                             "VERIFY_TIMEOUT_SECONDS)")
    parser.add_argument("--no-require-trigger", dest="require_trigger",
                        action="store_false",
                        help="accept harnesses on compile alone (old "
                             "behaviour); skip the buggy-version trigger "
                             "gate. Default is to require a trigger.")
    parser.add_argument("--synthesize_relations", action="store_true",
                        help="synthesize codebase-specific invariants/"
                             "metamorphic relations for the patched method "
                             "(semantic bugs), mechanically screen them on "
                             "the BUGGY build (relation_screen: drop "
                             "candidates that fire indiscriminately on "
                             "known-mostly-correct behaviour), and inject "
                             "only the survivors as screened candidates. "
                             "Targets overfits whose discriminating input is "
                             "in no test. Off by default (adds an LLM call + "
                             "screening builds). Synthesis always uses the "
                             "flagship model — proposing sound "
                             "relations is the hardest reasoning step in the "
                             "pipeline and the cheap model demonstrably "
                             "invents unsound ones. "
                             "Run WITH --replay_relations_on_patched: rule "
                             "INJECTION into harness prompts is not a "
                             "contributor (2026-07-19 ablation, 5 bugs, "
                             "identical recall/precision — consistent with "
                             "p23gate), but rule REPLAY is: full30's "
                             "result.jsonl records verifier-kept replay "
                             "convictions on 5 of 8 caught overfits, and "
                             "Math-2-o is caught ONLY this way (fuzzed-tier "
                             "replay; the overfit passes the trigger scenario "
                             "by construction, so no lifted test can see it). "
                             "The 2026-07-19 'not a contributor' conclusion "
                             "was measured with replay accidentally OFF in "
                             "both ablation arms and is retracted — see the "
                             "CORRECTION section in "
                             "docs/plan-history.md.")
    parser.add_argument("--replay_relations_on_patched", action="store_true",
                        help="P3.2 replay: execute every screened relation "
                             "(own-leg only; pooling removed 2026-07-19) directly "
                             "against the PATCHED "
                             "build — trigger-literal replay (deterministic) "
                             "plus a fuzzed pass — and hand firings to the "
                             "relation verifier as candidate findings. "
                             "Removes the two coin flips (harness must "
                             "implement the relation AND fuzzing must find "
                             "the inputs) that cost Math-2-o its verdict. "
                             "Requires --synthesize_relations and "
                             "--verify_relations (replay never convicts "
                             "without the verifier).")
    parser.add_argument("--screen_runs", type=int, default=20000,
                        help="fuzz iterations per candidate during relation "
                             "screening AND patched-side replay. Default "
                             "20000. Drop to ~5000 for the cheap rule-gen "
                             "iteration loop (faster, slightly noisier "
                             "fire-ratio); keep 20000 for a measurement that "
                             "is compared apples-to-apples.")
    parser.add_argument("--synth_max_rules", type=int, default=8,
                        help="how many candidate relations synthesis may "
                             "propose per leg (default 8). Raising it is a "
                             "numbers game against generation variance — more "
                             "draws, higher odds the discriminating relation "
                             "appears. Only the count changes; the guidance is "
                             "unchanged. Compare 4 vs 8 over MULTIPLE samples "
                             "(single-sample convict noise is +-1-2 legs).")
    parser.add_argument("--rule_compile_repair", action="store_true",
                        help="R1: on a rule candidate's compile failure, make "
                             "ONE model call to fix it before dropping it "
                             "(recovers fixable typos; ~22%% of candidates die "
                             "at compile today). Measured on/off in "
                             "--rulegen_only mode.")
    parser.add_argument("--rulegen_only", action="store_true",
                        help="RULE-GENERATION QUALITY MODE. Run synthesis + "
                             "screening (on buggy) + replay (on THIS leg's "
                             "patched build), emit the rule-gen metrics, and "
                             "STOP before harness generation and the judge. "
                             "The cheap iterate-on-rules loop: ~10-15k tokens "
                             "vs ~50-100k for a full leg. Requires "
                             "--synthesize_relations. Join a bug's -o and -c "
                             "records offline: convict = a relation fires on "
                             "the overfit-patch build and stays quiet on the "
                             "correct one; false-fire = fires on the correct "
                             "one.")
    parser.add_argument("--diffcov", action="store_true",
                        default=config.DIFFCOV,
                        help="MEASUREMENT ONLY. Inject a hit counter into "
                             "every method the patch changed, build the "
                             "patched project from the instrumented sources "
                             "(into its own _diffcov directory), and record "
                             "per-harness `[diffcov] method=... hits=N` "
                             "counts in result.jsonl and the trace. Answers "
                             "'did any generated input REACH the changed "
                             "code' — the load-bearing caveat of "
                             "docs/witness-study-2026-08-08.md. OFF by "
                             "default; the counts feed no prompt, no "
                             "verifier evidence, and no gate or verdict.")
    parser.add_argument("--divcap", action="store_true",
                        default=config.DIVCAP,
                        help="Divergence capture at the diff boundary. Build "
                             "the patched AND the buggy sources with an "
                             "observation call in every patch-changed method "
                             "(their own _divcap directories), run the bug's "
                             "own trigger tests through both, and hand "
                             "relation SYNTHESIS the observables whose value "
                             "moved. Steers which observable the invented "
                             "relations target; the values are NOT "
                             "expectations (the prompt forbids it and the "
                             "screen demotes a check that anchors on one). "
                             "OFF by default; feeds no verifier evidence and "
                             "no gate or verdict. See "
                             "docs/divcap-build-2026-08-10.md.")
    parser.add_argument("--results_json", type=str, default=None,
                        metavar="PATH",
                        help="append a one-line JSON record describing this "
                             "run's outcome to PATH (machine-readable; used "
                             "by the batch evaluation harness)")
    parser.add_argument("--reference_impl", action="store_true",
                        help="8.2 ladder: for a DISPUTED observable (the "
                             "firing names a method whose body is shown), ask "
                             "the model for an independent implementation "
                             "written from the DOCUMENTATION ONLY, screen it "
                             "against the buggy build on held-out off-defect "
                             "observables plus the failing test's pinned "
                             "answer, and attach a two-sided computed "
                             "comparison fact. OFF by default; ladder-gated.")
    parser.set_defaults(require_trigger=True)
    return parser.parse_args()


def _record_diffcov(runner, fuzz_results, record_extras) -> None:
    """Persist the diff-hit counts for this leg — one record per harness
    execution — into result.jsonl (`diffcov`) and the trace.

    MEASUREMENT ONLY, and this is the boundary: `diffcov` is written to the
    run artifacts and read by humans. It is deliberately NOT passed to any
    prompt, to the relation verifier's evidence, or to any gate or verdict
    computation. Adding it to one of those would make a decision depend on a
    signal that has never been validated against the guard fixtures.
    """
    if not getattr(runner, 'diffcov', False):
        return
    plan = getattr(runner, 'diffcov_plan', None)
    if plan:
        record_extras['diffcov_methods'] = plan
    records = []
    for _fr in (fuzz_results or []):
        counts = getattr(_fr, 'diffcov', None)
        if counts is None:
            continue
        rec = {'diffcov': counts, 'phase': 'patched-fuzz',
               'harness': getattr(_fr, 'attempt_label', '') or
                          os.path.basename(getattr(_fr, 'harness_path', ''))}
        records.append(rec)
        record_event('deterministic', method='diffcov',
                     target=rec['harness'],
                     output=(f"{sum(1 for n in counts.values() if n)}/"
                             f"{len(counts)} changed method(s) reached"),
                     detail=rec)
    if records:
        record_extras['diffcov'] = records


def _record_divcap(result, record_extras) -> None:
    """Persist this leg's divergence capture into result.jsonl (`divcap`)
    and the trace, and return nothing — the caller already holds the records
    it needs to feed synthesis.

    THE BOUNDARY, stated where the records are collected: the divergences go
    to the relation-SYNTHESIS prompt (that is the mechanism) and to the run
    artifacts. They are deliberately NOT passed to the relation verifier's
    evidence, to the judge, or to any gate or verdict computation. A
    divergence is not evidence of a defect — a correct patch diverges from
    the buggy build too — so a decision that consumed one would be reading a
    signal that means nothing about correctness. The only thing that crosses
    into judge-visible territory is the screen's anti-anchoring DEMOTION
    note, which is a statement about the relation, not about the patch.
    """
    if not result:
        return
    plan = result.get('plan')
    if plan:
        record_extras['divcap_methods'] = plan
    divergences = [d.as_dict() for d in (result.get('divergences') or [])]
    record_extras['divcap'] = {
        'status': result.get('status'),
        'buggy_observations': result.get('buggy_observations', 0),
        'patched_observations': result.get('patched_observations', 0),
        'divergences': divergences,
    }
    record_event('deterministic', method='divcap',
                 target='diff-boundary observation',
                 output=(f"{len(divergences)} divergence(s) from "
                         f"{result.get('buggy_observations', 0)} buggy / "
                         f"{result.get('patched_observations', 0)} patched "
                         f"observation(s) — {result.get('status')}"),
                 detail={'divergences': divergences})


def _emit_record(path, *, label, status, selection=None,
                 result=None, fuzz_results=None, bug_kind=None,
                 extras=None):
    """Append one JSON line summarising this run. `label` is the
    ground-truth class ('correct' or 'overfitting'); `status` is one of
    'evaluated', 'non_crashing', 'no_harnesses', 'error'. A run is only
    scoreable when status == 'evaluated'. `bug_kind` ('crashing' /
    'semantic' / None) lets the aggregator score the two oracle types
    separately — their recall ceilings differ, so blending them hides
    which oracle is working."""
    if not path:
        return
    import json as _json
    rec = {
        "label": label,
        "status": status,
        "bug_kind": bug_kind,
        "project": getattr(selection, "project_name", None),
        "bug_id": getattr(selection, "bug_id", None),
        "apr_tool": getattr(selection, "apr_tool", None),
        "converged": bool(getattr(result, "converged", False)),
        "harnesses_built": len(getattr(result, "successful_results", []) or []),
        # What fired on the buggy version per accepted harness (exception
        # headline). Lets the aggregator ask, per FN, "did the set only
        # ever trigger via the reported symptom?" — the masked-symptom
        # failure mode — without rerunning anything.
        "accepted_trigger_details": list(
            getattr(result, "accepted_trigger_details", []) or []),
        "harnesses_run": 0,
        "harnesses_crashed": 0,
        # crashed_on_patch: did ANY harness still crash the patched code?
        # This is the classifier's positive signal ("flagged overfitting").
        "crashed_on_patch": False,
    }
    if fuzz_results is not None:
        triggered = [r for r in fuzz_results if r.triggered]
        rec["harnesses_run"] = len(fuzz_results)
        rec["harnesses_crashed"] = len(triggered)
        rec["crashed_on_patch"] = len(triggered) > 0
    # Exact token spend for this run (all models: harness gen,
    # verifier, synthesis). Lets the aggregator sum real cost per batch.
    rec["tokens_total"] = usage_totals()
    rec["tokens_by_model"] = token_usage()
    # Provenance IN the per-run artifact (variance-baseline read, 2026-08-08):
    # the suite-level config.json carries the sha, but a leg dir compared or
    # shared on its own loses it — and repeated-draw designs REST on the
    # frozen-code premise being checkable from the artifact itself, not from
    # mtimes. GITSHA is exported by run_suite.sh from VERSION.
    rec["git_sha"] = os.environ.get("GITSHA", "unknown")
    # Free-form flags the caller wants queryable per run (e.g.
    # context_degraded when the touched-function extraction came up empty,
    # so an aggregator can tell "feature tested and failed" from "feature
    # never ran" without grepping logs).
    rec.update(extras or {})
    with open(path, "a") as fh:
        fh.write(_json.dumps(rec) + "\n")


def _print_token_usage():
    by_model = token_usage()
    if not by_model:
        return
    tot = usage_totals()
    print("\n" + "=" * 20 + " token usage " + "=" * 20)
    for model, u in by_model.items():
        print(f"  {model}: {u['calls']} calls, "
              f"{u['prompt_tokens']:,} in + {u['completion_tokens']:,} out "
              f"= {u['total_tokens']:,} tokens")
    print(f"  TOTAL: {tot['calls']} calls, {tot['total_tokens']:,} tokens "
          f"({tot['prompt_tokens']:,} in + {tot['completion_tokens']:,} out)")


def _fmt_messages(messages):
    parts = []
    for m in messages or []:
        role = m.get('role', '?') if isinstance(m, dict) else '?'
        content = m.get('content', '') if isinstance(m, dict) else str(m)
        parts.append(f"**[{role}]**\n```\n{content}\n```")
    return "\n\n".join(parts)


def _llm_role(messages):
    """Label an LLM call by its stage, read from its system prompt."""
    sysmsg = ''
    for m in messages or []:
        if isinstance(m, dict) and m.get('role') == 'system':
            sysmsg = (m.get('content') or '').lower()
            break
    if 'software-verification expert' in sysmsg or 'propose relation' in sysmsg:
        return 'rule synthesis'
    if 'skeptical reviewer' in sysmsg or 'prove a java relation' in sysmsg:
        return 'rule soundness-repair'
    if 'jazzer fuzzing harness' in sysmsg or 'security engineer' in sysmsg:
        return 'harness generation'
    if 'failed to compile' in sysmsg or 'fix a java snippet' in sysmsg:
        return 'compile-repair'
    if 'verif' in sysmsg or 'judge' in sysmsg or 'dismiss' in sysmsg:
        return 'verifier / judge'
    return 'LLM'


# What each pipeline component/step in the trace is (only those that appear
# are shown). Keeps the sequential trace self-explanatory.
_STEP_LEGEND = [
    ("failing-tests-found",
     "the project's own tests that expose the bug (extracted from Defects4J)"),
    ("analysis (TargetAnalyzer)",
     "parses the patch and builds the code context the model reasons over — "
     "the touched method(s), their documented contract, the call-graph "
     "reachable set + sibling members (via fuzz-introspector), and imports"),
    ("rule synthesis (LLM)",
     "proposes candidate RELATIONS — invariants / metamorphic properties a "
     "correct implementation must satisfy — from the documented contract"),
    ("screen-fuzz-buggy",
     "compiles each candidate rule and fuzzes it many times on the BUGGY "
     "build; the output is checked/violated/fire-ratio"),
    ("screen",
     "the keep/drop decision for each rule (direction-confirmed, selective, "
     "silent, or dropped by a lint/compile/cap)"),
    ("rule soundness-repair (LLM)",
     "a skeptical reviewer that rewrites a rule which fired on extreme inputs "
     "if it is unsound (asserts more than the contract guarantees)"),
    ("screening-survivors",
     "the final set of rules kept — these are passed to replay / harness "
     "generation"),
    ("replay-on-patched",
     "(rulegen mode) runs each surviving rule directly on the patched build"),
    ("harness generation (LLM)",
     "writes a Jazzer fuzzing harness that embeds the surviving rules plus "
     "oracles lifted from the failing test"),
    ("harness-attempt",
     "accept/reject of one generated harness — ACCEPTED = it compiles AND "
     "crashes the BUGGY build (with the triggering input shown)"),
    ("patched-fuzz",
     "fuzzes an accepted harness against the PATCHED build — FIRED (with the "
     "input + mismatch) means the overfit was caught; quiet means it escaped"),
]


def _fmt_det_output(o):
    """Format a deterministic step's output: pretty-print structured values,
    fence long/multiline strings, bold short scalars."""
    if isinstance(o, (dict, list)):
        return ("\n```json\n"
                + json.dumps(o, indent=2, ensure_ascii=False, default=str)
                + "\n```")
    s = '' if o is None else str(o)
    if len(s) > 200 or '\n' in s:
        return "\n```\n" + s + "\n```"
    return f"**{s}**"


def _write_trace_md(path, bug, label, events, outcome=None):
    """ONE purely SEQUENTIAL markdown transcript: every deterministic step and
    every LLM call, in the exact order they happened. Each LLM step shows its
    full prompt (deduped: a repeat of an earlier prompt is noted, not
    reprinted) and full output; each deterministic step shows its method,
    target and output. Nothing is summarised out of order — the sequence IS
    the record."""
    n_llm = sum(1 for e in events if e.get('kind') == 'llm')
    L = [f"# Pipeline trace — {bug}\n"]
    # Per-run provenance (variance-baseline read): the frozen-code premise
    # must be checkable from the artifact itself.
    L.append(f"**Code:** `{os.environ.get('GITSHA', 'unknown')}`\n")
    L.append(f"**Patch label:** {label}  "
             f"*(the patch under analysis is a "
             f"{'known-OVERFIT' if 'over' in str(label).lower() else 'known-CORRECT'}"
             f" fix — the pipeline is not told this)*")
    if outcome is not None:
        L.append(f"\n**Outcome:** {outcome}")
    # Patch under analysis — pulled from the analysis event's output so it sits
    # up top for orientation (it is also inside step [1] in full).
    _patch = ''
    for e in events:
        out = e.get('output')
        if e.get('kind') != 'llm' and isinstance(out, dict) and out.get(
                'patch_text'):
            _patch = out['patch_text']
            break
    if _patch:
        L.append("\n**Patch under analysis:**\n```diff\n"
                 + _patch.strip() + "\n```")
    L.append(f"\n{len(events)} sequential steps — {n_llm} LLM calls, "
             f"{len(events) - n_llm} deterministic. Read top to bottom.\n")
    # Legend — describe only the step types that actually appear.
    present = set()
    for e in events:
        if e.get('kind') == 'llm':
            present.add(_llm_role(e.get('messages')) + ' (LLM)')
        else:
            present.add(str(e.get('method', '')))
    shown = [(n, d) for n, d in _STEP_LEGEND
             if n in present or n.split(' (')[0] in present]
    if shown:
        L.append("<details><summary>Legend — what each step is</summary>\n")
        for n, d in shown:
            L.append(f"- **{n}** — {d}")
        L.append("\n</details>\n")
    # Per-MESSAGE dedup: the harness-generation calls share a huge identical
    # system + instruction/context message and differ only in the tail (repair
    # feedback, updated coverage). So collapse any message already shown
    # verbatim in an earlier step, and print only the NEW messages of a call.
    seen_msg = {}
    # Collapsible rendering: every step keeps a VISIBLE one-line headline
    # (navigable via the markdown outline), and the bulk sits in native
    # markdown toggles (<details>) — prompts CLOSED by default, LLM outputs
    # OPEN but collapsible, long deterministic dumps CLOSED behind their
    # one-line summary. The blank line after each <summary> is required for
    # markdown (code fences etc.) to render inside the toggle.
    L.append("*Viewing: every ▸ line is a click-to-expand toggle (VS Code "
             "markdown preview / GitHub). Prompts are collapsed by default; "
             "LLM outputs start expanded. The raw file stays fully "
             "greppable.*\n")
    for e in events:
        seq = e.get('seq')
        if e.get('kind') == 'llm':
            msgs = e.get('messages') or []
            L.append(f"\n---\n## [{seq}] 🧠 LLM call — **{_llm_role(msgs)}** "
                     f"— model `{e.get('model', '')}`")
            prompt_parts = []
            _new = 0
            for m in msgs:
                role = m.get('role', '?') if isinstance(m, dict) else '?'
                content = (m.get('content', '') if isinstance(m, dict)
                           else str(m)) or ''
                key = (role, content)
                if key in seen_msg:
                    prompt_parts.append(
                        f"- *[{role}] message: identical to step "
                        f"[{seen_msg[key]}] — not reprinted*")
                else:
                    seen_msg[key] = seq
                    _new += 1
                    prompt_parts.append(f"**[{role}]**\n```\n{content}\n```")
            if _new == 0:
                prompt_parts.append(
                    "*(every message identical to earlier steps)*")
            _pchars = sum(len(p) for p in prompt_parts)
            _dedup = (f", {_new} new" if _new < len(msgs) else "")
            L.append(f"<details><summary>▸ Prompt ({len(msgs)} message(s), "
                     f"~{_pchars:,} chars{_dedup})</summary>\n")
            L.extend(prompt_parts)
            L.append("\n</details>")
            _out = str(e.get('output', '')).strip()
            L.append(f"<details open><summary>▸ Output "
                     f"(~{len(_out):,} chars)</summary>\n")
            L.append("```\n" + _out + "\n```")
            L.append("\n</details>")
        else:
            det = {k: v for k, v in e.items()
                   if k not in ('seq', 'kind', 'method', 'target', 'output')}
            L.append(f"\n---\n## [{seq}] ⚙️ {e.get('method', '')}"
                     + (f" · `{e.get('target')}`" if e.get('target') else ''))
            _body = _fmt_det_output(e.get('output'))
            # Short outputs stay inline — they ARE the skeleton of the
            # trace; anything long/multi-line collapses behind its first
            # meaningful line.
            if '\n' in _body and len(_body) > 400:
                _head = next((ln.strip('`*# {}",') for ln
                              in _body.strip().splitlines()
                              if ln.strip() and not
                              ln.strip().startswith('```')
                              and ln.strip() not in ('{', '[')),
                             'output')
                _head = _head or 'output'
                L.append(f"<details><summary>▸ output — {_head[:100]} "
                         f"(~{len(_body):,} chars)</summary>\n")
                L.append("**output:** " + _body)
                L.append("\n</details>")
            else:
                L.append("**output:** " + _body)
            for k, v in det.items():
                _vs = str(v)
                if len(_vs) > 400:
                    L.append(f"<details><summary>▸ {k} "
                             f"(~{len(_vs):,} chars)</summary>\n")
                    L.append(f"- {k}: {v}")
                    L.append("\n</details>")
                else:
                    L.append(f"- {k}: {v}")
    with open(path, 'w', encoding='utf-8') as fh:
        fh.write("\n".join(L) + "\n")



def _cycle6_ev(method, target=None, output=None, reason=None):
    """Record one cycle-6 audit event. Never raises into the pipeline.

    Imported INSIDE the call (rather than using the module-level
    `record_event`) so the recorder can be stubbed for tests and so a broken
    recorder can never take a leg down."""
    try:
        from llm import record_event as _re
        _re('deterministic', method=method,
            target=('' if target is None else str(target)),
            output=('' if output is None else str(output)),
            reason=('' if reason is None else str(reason)))
    except Exception:  # pragma: no cover - defensive
        pass


def _rate_absent(oid, reason):
    """Cycle-6 item 6 — record that this firing reaches judging with NO
    buggy-side fire rate in its evidence, and WHY.

    smoke30 (docs/replay/smoke30_analysis.md) turned on a firing whose 6B event
    read "no rate found — verdict unchanged" with nothing anywhere saying why
    the rate was missing; the analysis then guessed wrong about the cause twice.
    Every path that ends with no rate in the evidence now says so here, so the
    absence is never again something that has to be inferred. Never raises."""
    _cycle6_ev('cycle6_rate_absent', target=oid, output='no-rate',
               reason=reason)


# Spec M (cycle-3b): per-leg ceiling on how many oracles the universal screen
# will MEASURE. Each measurement is a full instrument + compile + counting-fuzz
# round, so the budget is spent only on oracles that actually reach the
# measuring path (a cached, already-known or unmeasurable oracle costs none).
_UNIVERSAL_SCREEN_CAP = 8


def _universal_screen_step(oid, source, rate_known, cache, measured,
                           instrument, compile_variant, count_violations,
                           cap=_UNIVERSAL_SCREEN_CAP):
    """Spec M / cycle-6 item 6 — measure ONE fired oracle's buggy-side fire
    rate, recording on EVERY path what it decided and why.

    Pure orchestration: the three injected callables do the work —
    ``instrument(source, oid) -> str|None`` (the counting transform),
    ``compile_variant(instrumented) -> build|None`` (None = did not compile)
    and ``count_violations(build) -> (checked, violated)|None`` (None = the
    counting run produced no counts). Extracted from ``main`` so each failure
    path is testable offline and so it can never again fail SILENTLY: before
    this, a missing target id, a non-compiling variant and a countless run all
    looked identical from the outside (nothing attached, no event).

    Returns ``(notes, measured, counts, outcome)`` where ``outcome`` is one of
    ``skipped`` / ``cached`` / ``capped`` / ``not-instrumented`` /
    ``compile-failed`` / ``no-counts`` / ``measured`` / ``raised``.

    FAILS OPEN throughout: any failure — including an exception from any
    injected callable — returns no notes and no counts, and never raises. A
    measurement failure must never manufacture a fact in either direction.

    The "already known" gate is per-ORACLE by construction: this function is
    told about exactly one ``oid`` and consults ``cache`` under that key only,
    so one oracle's known rate can never suppress another's measurement."""
    def _done(outcome, reason, notes=(), counts=None, new_measured=None):
        _cycle6_ev('cycle6_universal_screen_decided', target=oid,
                   output=outcome, reason=reason)
        return (list(notes),
                measured if new_measured is None else new_measured,
                counts, outcome)
    try:
        if oid is None:
            return _done('skipped', 'this firing named no oracle id — '
                                    'nothing to measure')
        if oid in cache:
            return _done('cached',
                         'already measured this oracle in this leg — '
                         're-attaching %d fact(s), no new measurement'
                         % len(cache[oid] or []),
                         notes=cache[oid] or [])
        if rate_known:
            return _done('skipped',
                         'a buggy-side rate is already known for THIS '
                         'oracle — no new measurement needed')
        if measured >= cap:
            return _done('capped',
                         'per-leg cap of %d measured oracles is spent — '
                         "fired oracle '%s' was NOT measured and reaches "
                         'judging with no rate' % (cap, oid))
        instrumented = instrument(source, oid)
        if not instrumented:
            cache[oid] = []
            return _done('not-instrumented',
                         "no counting variant could be built for oracle "
                         "'%s' (target id or entrypoint not found) — "
                         'nothing measured (fail-open)' % oid)
        # The budget is spent here, at the first real measurement cost.
        measured += 1
        build = compile_variant(instrumented)
        if not build:
            cache[oid] = []
            return _done('compile-failed',
                         'the instrumented counting variant did not '
                         'compile — nothing measured (fail-open)',
                         new_measured=measured)
        stats = count_violations(build)
        if not stats:
            cache[oid] = []
            return _done('no-counts',
                         'the counting run produced no [relscreen] counts — '
                         'nothing measured (fail-open)',
                         new_measured=measured)
        checked, violated = stats
        from java.relations.evidence_facts import (
            fire_rate_fact as _frf_u, never_held_fact as _nhf_u)
        notes = []
        _fru = _frf_u(checked, violated, None, None, '')
        if _fru:
            notes.append(_fru)
        if violated == checked and checked > 0:
            notes.append(_nhf_u(checked))
        cache[oid] = notes
        return _done('measured',
                     'buggy-side counts violated=%s/%s' % (violated, checked),
                     notes=notes,
                     counts=((checked, violated) if checked else None),
                     new_measured=measured)
    except Exception as exc:
        return _done('raised',
                     'universal screen raised (%s: %s) — nothing measured, '
                     'nothing attached (fail-open)'
                     % (type(exc).__name__, exc))


def _deliver_buggy_rate(fired_ids, buggy_rate_counts, rate_fact_attached,
                        patched_counts, demote):
    """Cycle-6 item 4 PART B — unconditional delivery of a KNOWN buggy-side
    rate to the harness track. Returns the `[fire-rate fact]` note to attach,
    or None when there is nothing to deliver.

    Whichever branch measured it (the matched relation's buggy screen, or the
    universal screen), a rate that IS known must reach the judging evidence: it
    must never be dropped because the patched-side counts do not exist yet, or
    because a one-door match routed around the block that would have stated it.
    No new threshold and no new wording — `fire_rate_fact`'s own branches
    decide, and a None (the rate is genuinely unremarkable) correctly attaches
    nothing.

    AUDIT: emits `cycle6_buggy_rate_considered` (was a known rate found for
    this firing's oracle) and `cycle6_buggy_rate_decided` (attached /
    none-unremarkable / skipped). 6B keys on exactly this fact, so a trace that
    shows 6B finding no rate can now be traced back to whether one was ever
    delivered. Fail-open: any error delivers nothing and never raises."""
    oid = None
    try:
        oid = (sorted(fired_ids)[0] if fired_ids else None)
        counts = (buggy_rate_counts or {}).get(oid)
        _cycle6_ev('cycle6_buggy_rate_considered', target=oid,
                   output=('rate_known=%s' % bool(counts)),
                   reason=('known buggy-side counts violated=%s/%s'
                           % (counts[1], counts[0]) if counts else
                           'no buggy-side counts were measured for this '
                           'oracle'))
        if not counts:
            _cycle6_ev('cycle6_buggy_rate_decided', target=oid,
                       output='none', reason='no known rate to deliver')
            _rate_absent(oid,
                         'no buggy-side counts exist for this oracle — '
                         'neither a matched relation\'s screen nor the '
                         'universal screen produced any (see the '
                         'cycle6_universal_screen_decided event for which '
                         'path declined)')
            return None
        if rate_fact_attached:
            _cycle6_ev('cycle6_buggy_rate_decided', target=oid,
                       output='skipped',
                       reason='a [fire-rate fact] was already attached '
                              'upstream')
            return None
        from java.relations.evidence_facts import fire_rate_fact as _frf_b
        note = _frf_b(counts[0], counts[1], patched_counts[0],
                      patched_counts[1], demote)
        _dlv = 'attached' if note else 'none (rate unremarkable)'
        print(f"      [buggy-rate delivery] oracle "
              f"'{oid}' buggy={counts[1]}/{counts[0]} "
              f"patched={patched_counts} note={_dlv}")
        _cycle6_ev('cycle6_buggy_rate_decided', target=oid,
                   output=('attached' if note else 'none'),
                   reason='buggy=%s/%s patched=%s -> %s'
                          % (counts[1], counts[0], patched_counts, _dlv))
        if not note:
            _rate_absent(oid,
                         'a buggy-side rate IS known (%s/%s) but '
                         'fire_rate_fact produced no statement for it, so '
                         'this firing reaches judging with no rate'
                         % (counts[1], counts[0]))
        return note
    except Exception as exc:
        _cycle6_ev('cycle6_buggy_rate_decided', target=oid, output='none',
                   reason='delivery raised (%s: %s) — nothing attached '
                          '(fail-open)' % (type(exc).__name__, exc))
        _rate_absent(oid, 'rate delivery raised (%s: %s) — nothing attached '
                          '(fail-open)' % (type(exc).__name__, exc))
        return None


def _j3_failing_test_block(failure_tests, cap_each=2000):
    """J3: the trigger test's own source + its real failure message, for
    the judge's evidence. Trust source #1 — in the Closure-62 backwards
    judgment the judge weighed buggy guard code against a bare literal;
    the test's actual assertion line makes the trust hierarchy concrete.
    Capped; at most two tests (the ones the fired check lifts from are
    always in the first two for our bugs)."""
    lines = []
    for ft in (failure_tests or [])[:2]:
        if getattr(ft, 'method_source', None):
            lines.append(
                f"[REAL FAILING TEST {ft.test_class}::{ft.test_method} "
                f"— trust source #1, verbatim]\n"
                + ft.method_source[:cap_each])
        if getattr(ft, 'failure_message', None):
            lines.append("On the BUGGY build this test fails with: "
                         + ft.failure_message[:400])
    return "\n".join(lines)


# --- outcome mutations: ONE evented path -----------------------------------
# Every change to a finding's outcome (drop it, or flag the patch) goes through
# these two helpers. Before 2026-07-31 there were nine direct mutation sites,
# all print-only — and run_suite.sh deletes run.log on success, so a run's
# decisive step was unknowable from its archived trace.md. That bit hardest on
# CRASHING legs, whose verdict is purely mechanical and therefore leaves no
# judge transcript to reconstruct from (crashtrace1: two patched-build firings,
# crashed_on_patch=false, zero recorded reason).
#
# Do not write `r.triggered = False` or set crashed_on_patch directly;
# tests/test_outcome_events.py fails the build if a new site appears.
def drop_finding(r, site, reason, **detail):
    """Drop one finding, recording WHY into trace.md. `site` names the rule."""
    r.triggered = False
    print(f"  \u2717 [{site}] dropped: {reason}")
    record_event('deterministic', method='outcome-drop',
                 target=getattr(r, 'harness_path', None) or getattr(
                     r, 'attempt_label', '?'),
                 output=f'DROPPED by {site}',
                 detail={'site': site, 'reason': reason, **detail})


def flag_overfitting(record_extras, site, reason, **detail):
    """Set the positive verdict, recording WHY. Never writes False."""
    record_extras['crashed_on_patch'] = True
    print(f"  \u2713 [{site}] flagged overfitting: {reason}")
    record_event('deterministic', method='outcome-flag',
                 target=site, output='FLAGGED overfitting',
                 detail={'site': site, 'reason': reason, **detail})


def main():
    args = parse_args()
    # Token totals are process-global; start this patch's accounting from
    # zero so a future multi-patch-per-process driver can't accumulate.
    reset_token_usage()
    # Record EVERY pipeline event (LLM calls + deterministic decisions) so the
    # leg can dump a complete, ordered, auditable transcript.
    enable_recording()
    reset_events()

    if not (args.correct or args.overfitting):
        print("Please select either --correct flag or --overfitting flag")
        sys.exit(1)

    # 1) When evaluating an explicit patch file, project_name/bug_id are
    #    fully determined by its path — no checkout needed to know them.
    #    Semantic bugs can be classified from defects4j's static root-cause
    #    metadata alone (`defects4j info -b`), so when we're going to
    #    discard semantic bugs anyway (--skip_semantic), check that here
    #    and bail before paying for the checkout, jazzer setup, or test
    #    source extraction below — a ~7s checkout for a bug we'd throw
    #    away regardless. (Random-sampling mode re-samples a fresh patch
    #    on checkout failure, so this shortcut is scoped to the
    #    deterministic --patch_file path. The full classification further
    #    down remains the source of truth and still runs for bugs that
    #    pass this gate — it also covers the "no trigger test at all"
    #    case this metadata-only check can't see.)
    if args.skip_semantic and args.patch_file:
        peek = PatchSelector.peek_patch_file(args.patch_file)
        if classify_bug_kind(peek.project_name, peek.bug_id) == "semantic":
            print(f"\n{peek.project_name} {peek.bug_id} "
                  f"({peek.apr_tool}) is a semantic bug (no crash "
                  "signature) — skipping before checkout.")
            _emit_record(args.results_json,
                         label='correct' if args.correct else 'overfitting',
                         status='semantic_skip', selection=peek,
                         bug_kind='semantic')
            sys.exit(4)

    # 2) Resolve Jazzer jars up front so failures surface before the slow
    #    checkout + LLM campaign. The standalone (driver) jar is needed
    #    both for the final patched-code run AND for the in-campaign
    #    trigger gate, so fetch it if either is active.
    jazzer_env = JazzerEnvironment()
    jazzer_api_jar = jazzer_env.ensure()
    needs_driver = args.fuzz_timeout > 0 or args.require_trigger
    jazzer_standalone_jar = (jazzer_env.ensure_driver()
                             if needs_driver else None)

    # 3) Pick a random patch and check out the corresponding buggy d4j
    #    version.  Retry sampling if we land on a deprecated bug (defects4j
    #    refuses to check it out) so we don't propagate an unhandled error.
    selector = PatchSelector(
        project_name=args.project_name,
        correct=args.correct,
        overfitting=args.overfitting,
        patch_file=args.patch_file,
    )
    while True:
        try:
            selection = selector.select()
            break
        except DeprecatedBugError as exc:
            print(f"  skipping deprecated bug: {exc}")

    # 4a) Extract the bug-triggering test(s) shipped with this d4j bug.
    #     They seed the prompt with a worked example of a crashing
    #     input — the LLM sees what values already drive the buggy code
    #     path and shapes its FuzzedDataProvider calls accordingly.
    failure_tests = FailureTestExtractor().extract(
        selection.buggy_dir,
        project_name=selection.project_name,
        bug_id=selection.bug_id,
    )
    _print_failure_tests(failure_tests)
    record_event('deterministic', method='failing-tests-found',
                 output=[getattr(t, 'method_name', str(t))
                         for t in (failure_tests or [])])

    # 4a-bis) Classify the bug. Crashing bugs fail their trigger test with a
    #     thrown application exception; semantic bugs fail a JUnit assertion
    #     (wrong value, no throw). Both are now in scope — they differ only in
    #     the oracle the harness is built around (see prompt_factory below and
    #     the semantic path in PromptBuilder). If we couldn't determine any
    #     exception type, is_crashing_bug is conservatively False, so such
    #     bugs take the semantic (assertion-lifting) path.
    bug_kind = "crashing" if is_crashing_bug(failure_tests) else "semantic"
    if not failure_tests:
        # With no trigger test at all there is nothing to lift or anchor on;
        # neither oracle can be built. Keep skipping these.
        print(f"\n{selection.project_name} {selection.bug_id} "
              f"({selection.apr_tool}) has no bug-triggering tests — "
              "no oracle can be built. Skipping.")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status='non_crashing', selection=selection)
        sys.exit(3)
    print(f"\nbug kind: {bug_kind}")

    # This pipeline only evaluates crashing bugs. Bail here, before the
    # costly TargetAnalyzer/LLM campaign, rather than after spending time
    # and API calls on a bug we're going to discard anyway.
    if args.skip_semantic and bug_kind != "crashing":
        print(f"\n{selection.project_name} {selection.bug_id} "
              f"({selection.apr_tool}) is a semantic bug (no crash "
              "signature) — skipping.")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status='semantic_skip', selection=selection,
                     bug_kind=bug_kind)
        sys.exit(4)

    # 4a-ter) P0.1 safety net, buggy half: the bug's own trigger tests must
    #     FAIL on the unpatched checkout before we spend a single LLM token
    #     on it. Lang-7 burned weeks because the bug's behavior didn't
    #     exist on our JVM and nothing ever said so. Costs one `defects4j
    #     test -t` per trigger test, cached per checkout.
    try:
        PatchedProjectBuilder().verify_bug_reproduces(selection.buggy_dir)
    except TriggerVerificationError as exc:
        print(f"\nSAFETY NET ({selection.project_name}-{selection.bug_id}): "
              f"{exc}")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status=exc.status, selection=selection,
                     bug_kind=bug_kind,
                     extras={'safety_net': str(exc)})
        sys.exit(5)

    # H2: the safety net just ran every trigger test on the buggy build and
    # its failure message names the diverging observable AND the wrong
    # value the bug produces ('expected:<NaN> but was:<4.0>') — attach it
    # to each FailureTest so the harness prompt and the H3 acceptance gate
    # can use it instead of throwing it away.
    _trigger_msgs = PatchedProjectBuilder.trigger_failure_messages(
        selection.buggy_dir)
    for _ft in failure_tests:
        _ft.failure_message = _trigger_msgs.get(
            f'{_ft.test_class}::{_ft.test_method}')
    # H1: resolve the parts of the test class each trigger test actually
    # uses (setUp/@Before, helpers, constants, fixture files) — the
    # harness writer replicates the real scenario instead of improvising
    # the setup, which was the root of every setup-divergence failure.
    from java.bug_context.failure_test import resolve_test_support
    for _ft in failure_tests:
        try:
            _ft.support_source = resolve_test_support(
                _ft, checkout_dir=selection.buggy_dir)
        except Exception as _exc:   # context is best-effort, never fatal
            print(f"  [test-support] {_ft.test_method}: resolution failed "
                  f"({_exc}) — prompt falls back to the bare method body")
    record_event(
        'deterministic', method='test-context (H1/H2)',
        output=[{'test': f'{t.test_class}::{t.test_method}',
                 'failure_message': t.failure_message,
                 'support_chars': len(t.support_source or '')}
                for t in failure_tests])

    # 4b) Extract the patch + every project function it touches +
    #     cross-references for each of those functions.
    context = TargetAnalyzer(
        reachable_node_cap=args.reachable_node_cap,
        reachable_max_depth=args.reachable_max_depth,
        introspector_depth_cap=args.introspector_depth_cap,
    ).analyze(
        patch_path=selection.patch_path,
        buggy_dir=selection.buggy_dir,
    )
    print(json.dumps(context.as_dict(), indent=2))
    try:
        record_event('deterministic', method='analysis (TargetAnalyzer)',
                     output=context.as_dict())
    except Exception:
        pass

    # Empty touched-function extraction silently disables everything that
    # keys on the patched method — the function blocks in the prompt,
    # mining tokens, and relation synthesis (which returns [] without a
    # word when patched_sources is empty). A whole diagnostic leg once
    # "tested" synthesis that never ran because of this. Say it loudly and
    # stamp the record so the aggregator can exclude/flag the run instead
    # of misreading it as "feature ran and found nothing".
    context_degraded = not context.functions
    if context_degraded:
        print("\n" + "!" * 60)
        print("!! DEGRADED CONTEXT: no touched function could be extracted")
        print("!! from the patch (AST pass AND regex fallback both empty).")
        print("!! Prompt will lack function bodies; mining and relation")
        print("!! synthesis are disabled for this run.")
        print("!" * 60)
    record_extras = {"context_degraded": context_degraded}

    # H4/H5: same-name overloads, shared-prefix method families, and the
    # class's readable no-arg state — the mechanically-listed raw material
    # of the two historically-winning invented check shapes
    # (sibling-agreement: Lang-41, Lang-27's create* family; hidden-state:
    # Lang-60's capacity). Computed once from the touched file's source;
    # injected into BOTH the harness prompt and rule synthesis.
    sibling_hints = ''
    try:
        from java.parsing.java_source import sibling_and_state_hints
        for _mf in (context.modified_files or [])[:1]:
            _mp = os.path.join(selection.buggy_dir, _mf.lstrip('/'))
            if os.path.isfile(_mp):
                with open(_mp, encoding='utf-8', errors='replace') as _fh:
                    sibling_hints = sibling_and_state_hints(_fh.read())
        if sibling_hints:
            print(f"  [H4/H5] sibling/state hints: "
                  f"{len(sibling_hints)} chars")
    except Exception as _exc:
        print(f"  [H4/H5] hint extraction failed ({_exc}) — skipped")

    # Class-level codebase context for the LLM judgment stages: relation
    # synthesis, relation verification, and the 'consistency' harness slot
    # (one skeleton-aware harness per set). Task inspection showed the
    # discriminating invariant routinely lives OUTSIDE the patched method
    # (constructor invariants, complementary sibling functions, class
    # javadoc contracts), and the verifier's measured leaks were
    # domain-knowledge failures. Built once from the buggy checkout —
    # label-free. Assembled for every semantic bug (it is a cheap local
    # parse): gating it on the synthesis/verifier flags silently starved
    # the consistency slot in flag-off configs.
    class_ctx = []
    if bug_kind == "semantic" and not context_degraded:
        from java.bug_context.code_context import assemble_class_context
        class_ctx = assemble_class_context(
            selection.buggy_dir,
            context.modified_files or [],
            [fn.func_name for fn in context.functions],
            test_sources=[getattr(ft, 'method_source', '') or ''
                          for ft in failure_tests])
        if class_ctx:
            print(f"  [class-ctx] {len(class_ctx)} class skeleton(s), "
                  f"{sum(len(b) for b in class_ctx):,} chars")

    # 4c) Capture the GROUND-TRUTH crashing input by running the trigger
    #     test against the buggy checkout and reading the value back out
    #     of the failure output. This removes the model's need to guess
    #     which of (possibly many) test inputs actually crashes — the
    #     single biggest cause of wasted attempts. Bug-type-agnostic and
    #     purely additive: on any capture failure this is None and the
    #     prompt falls back to test-source-only behaviour.
    #
    #     Crashing bugs only: a semantic bug throws nothing, so there is no
    #     crashing value to read back. The semantic path lifts its anchor
    #     (the expected value) straight from the trigger test source instead.
    crash_input = None
    primary_test = next((ft for ft in failure_tests if ft.has_source),
                        failure_tests[0] if failure_tests else None)
    if bug_kind == "crashing" and primary_test is not None:
        # Fallback anchors mined from the test source, scoped to the
        # patched (target) methods — used only when the runtime message
        # does not itself echo the crashing value.
        candidate_literals = []
        if primary_test.method_source:
            candidate_literals = candidate_anchor_literals(
                primary_test.method_source,
                [fn.func_name for fn in context.functions],
            )
        crash_input = CrashInputExtractor().extract(
            buggy_dir=selection.buggy_dir,
            test_class=primary_test.test_class,
            test_method=primary_test.test_method,
            candidate_literals=candidate_literals,
        )
    _print_crash_input(crash_input)

    # 4.5) Mine trusted sibling oracles (semantic bugs). The lifted-assertion
    #      oracle covers ONE trigger test; the same test class holds many more
    #      assertions on the patched method — sibling tests and the trigger
    #      test's other lines. Each is a developer-written literal the buggy
    #      code already passes, so a correct patch must too, while an overfit
    #      patch that special-cases the reported input fails a different one.
    #      Pure text mining (no compile, no model); injected into the prompt
    #      as extra trusted pairs. Same provenance as the lifted seed — uses
    #      the project's own tests, never the developer fix or the label.

    # 4.6) Synthesize codebase-specific relation CANDIDATES (semantic bugs).
    #      Mining only covers TESTED inputs; an overfit can pass every test
    #      yet stay wrong on an untested input whose oracle exists in no
    #      test. Here we ask an LLM to propose invariants/metamorphic
    #      relations over the patched API, grounded in the diff and the
    #      touched methods' javadoc. Candidates are HYPOTHESES: they are
    #      mechanically screened on the buggy build (relation_screen, run
    #      after the builder exists — see step 6) and ONLY survivors ever
    #      reach a prompt. If screening cannot run, nothing is injected.
    # Documented contracts (javadoc) of the touched methods, extracted once
    # from the buggy sources. Consumed twice: relation synthesis grounds its
    # candidates in them, and the harness prompt's documented-preconditions
    # block feeds the valid-by-construction rule the actual @param/@throws
    # contract instead of making the generator guess it from test source.
    # Best-effort — empty on any miss (undocumented code), and every
    # consumer falls back cleanly.
    touched_javadocs = []
    if context.functions:
        from java.relations.relation_synth import javadoc_for
        for rel in (context.modified_files or []):
            full = Path(selection.buggy_dir) / rel
            try:
                src_text = full.read_text(encoding='utf-8', errors='replace')
            except OSError:
                continue
            for fn in context.functions:
                jd = javadoc_for(src_text, fn.func_name)
                if jd and jd not in touched_javadocs:
                    touched_javadocs.append(jd)

    # 4.55) --divcap: what does the patch actually MOVE? Runs before
    #       synthesis because its whole purpose is to steer which observable
    #       the invented relations target — after synthesis it would be a
    #       measurement, not a mechanism. Fail-soft: any build/run failure
    #       leaves `_divergences` empty and the prompt byte-identical.
    _divergences: list = []
    _divergence_values: list = []
    if (getattr(args, 'divcap', False) and bug_kind == "semantic"
            and not context_degraded):
        print("\n" + "#" * 18 + " divergence capture " + "#" * 18)
        from java.execution import divcap as _divcap_mod
        _divcap_result = _divcap_mod.collect_divergences(
            selection.buggy_dir, selection.patch_path,
            top_k=config.DIVCAP_TOP_K)
        _divergences = _divcap_result.get('divergences') or []
        _divergence_values = _divcap_mod.buggy_side_values(_divergences)
        _record_divcap(_divcap_result, record_extras)
        print(f"  [divcap] {len(_divergences)} divergence(s) "
              f"({_divcap_result.get('status')})")
        for _d in _divergences:
            print(f"  [divcap] {_d.method_id} {_d.observable}: "
                  f"{_d.buggy_value} -> {_d.patched_value} "
                  f"on {_d.input_shape} (x{_d.count})")

    synthesized_relations = []
    _all_candidates = []
    # The classes this patch changed — read off the diff headers below and
    # handed to the screen so the 8.35 Mechanism-A normalisation knows which
    # calls are the PROBE tier. Empty (no synthesis, no diff headers) leaves
    # that step inert.
    _patched_classes: list = []
    _doc_exc: dict = {}
    if bug_kind == "semantic" and args.synthesize_relations:
        if context_degraded:
            print("  [synth] skipped: no touched function extracted "
                  "(context degraded — see warning above)")
        else:
            from java.relations.relation_synth import RelationSynthesizer
            # Always the flagship model, regardless of the
            # harness-generation tier: proposing relations that must hold
            # for EVERY correct implementation is the hardest reasoning
            # step in the pipeline, and the nano batch showed the cheap
            # model invents unsound out-of-domain oracles. `--model X`
            # still wins so a forced single-model run stays single-model.
            synth_model = args.model or config.LOCAL_LLM_MODEL
            patched_sources = [fn.func_source for fn in context.functions]
            _syn_cls = sorted(set(re.findall(
                r'^\+\+\+\s+.*?/([A-Za-z_]\w*)\.java',
                context.patch_text or '', re.MULTILINE)))
            class_name = _syn_cls[0] if _syn_cls else ''
            _patched_classes = list(_syn_cls)
            # A patch to a base class is probed through its public subclass
            # (the dataset's normal delegation shape), so the probe tier
            # counts same-tree subtypes too — else the tier misses exactly
            # the delegating probes it exists for (Chart-19's shape).
            if _patched_classes:
                from java.parsing.java_source import subclasses_in_tree
                _sub_cls = subclasses_in_tree(selection.buggy_dir,
                                              _patched_classes)
                if _sub_cls:
                    print(f"  [synth] probe tier includes patched-class "
                          f"subtypes: {_sub_cls}")
                    # Into the trace, not just stdout: run.log is deleted on
                    # success (one-file policy), which made the 8.38 roll's
                    # adherence read unrecoverable from artifacts.
                    record_event('deterministic', method='synth',
                                 target='probe-tier',
                                 output=f'subtypes counted: {_sub_cls}',
                                 detail={'patched_classes': _syn_cls,
                                         'subtypes': _sub_cls})
                    _patched_classes += _sub_cls
            # Documented-exception guard (prereg addendum 2026-08-10): an
            # exception the docs permit for the throwing method is a
            # rejection, not a tier-2 violation (Math-65's
            # OptimizationException on non-convergence).
            if _patched_classes:
                from java.parsing.java_source import (
                    documented_exceptions_in_tree)
                _doc_exc = documented_exceptions_in_tree(
                    selection.buggy_dir, _patched_classes)
                if _doc_exc:
                    _doc_view = {f'{c}#{m}': x
                                 for (c, m), x in sorted(_doc_exc.items())}
                    print(f"  [synth] documented exceptions on probe tier: "
                          f"{_doc_view}")
                    record_event('deterministic', method='synth',
                                 target='probe-tier',
                                 output=f'documented exceptions: {_doc_view}',
                                 detail={'documented': _doc_view})
            synthesizer = RelationSynthesizer(
                HarnessGenerator(model=synth_model,
                                 temperature=0.3, top_p=1.0),
                focused=getattr(args, 'focused_synthesis', False))
            # P2.1: hand synthesis the bug's own failing test — the one
            # trusted source of the correct DIRECTION. Was '' for the whole
            # project history, so synthesis read only the buggy body and
            # inverted relations (Lang-7). Build from the primary test's
            # source plus the exact values it asserts.
            trigger_test_block = ''
            trigger_methods: list = []
            if primary_test is not None and primary_test.has_source:
                exp = expected_assert_literals(
                    primary_test.method_source or '')
                block = [f"// {primary_test.test_class}::"
                         f"{primary_test.test_method}",
                         primary_test.method_source or '']
                if exp:
                    block.append(
                        "// values this test asserts as CORRECT "
                        "(the patched code must reproduce these): "
                        + ", ".join(exp[:12]))
                # H1->station-2 (hfix11 FP fix): the rule-writer had the
                # same setup blind spot as the harness writer — it wrote
                # direction-confirmed rules that rebuild the test's
                # scenario WITHOUT its wiring (Closure-62's source
                # provider), and those fired on every build. Show it the
                # resolved test-class support and the real failure
                # message, with the same replicate-or-drop instruction.
                if getattr(primary_test, 'failure_message', None):
                    block.append(
                        "// On the BUGGY build this test FAILS with "
                        "(names the diverging observable + wrong value):\n"
                        + "// " + primary_test.failure_message.replace(
                            "\n", "\n// "))
                if getattr(primary_test, 'support_source', None):
                    block.append(
                        "// The test DEPENDS on this setup from its test "
                        "class (helpers, fields, fixtures). A relation "
                        "that reconstructs the test's scenario must "
                        "REPLICATE this setup exactly — a relation that "
                        "cannot must assert only setup-independent "
                        "properties (counts, kinds, contract formulas), "
                        "or it will fire on every build including "
                        "correct ones:\n"
                        + primary_test.support_source)
                if sibling_hints:
                    block.append(sibling_hints)
                trigger_test_block = "\n".join(block)
                # P3.2b root-region anchoring: the methods/types the
                # failing test actually exercises. The discriminating
                # relation may constrain ONE of THESE even when the patch
                # edited a different method (Math-2's overflow surfaces in
                # getNumericalMean() though Arja edited elsewhere), so
                # feeding them widens the synthesis anchor past the
                # patch-touched region.
                src_t = primary_test.method_source or ''
                _JUNIT = {'assertEquals', 'assertTrue', 'assertFalse',
                          'assertNull', 'assertNotNull', 'assertSame',
                          'assertArrayEquals', 'assertThat', 'assertThrows',
                          'fail', 'expect', 'valueOf', 'toString', 'equals'}
                for name in re.findall(r'\.([a-zA-Z_]\w*)\s*\(', src_t):
                    if name not in _JUNIT and name not in trigger_methods:
                        trigger_methods.append(name)
                for ctor in re.findall(r'\bnew\s+([A-Z]\w*)\s*\(', src_t):
                    if ctor not in trigger_methods:
                        trigger_methods.append(ctor)
                trigger_methods = trigger_methods[:20]
            candidates = synthesizer.synthesize(
                patched_sources, class_name,
                context.root_cause_reachable or [], [], '',
                patch_text=context.patch_text or '',
                javadocs=touched_javadocs,
                class_context=class_ctx,
                source_imports=context.source_imports,
                trigger_test_block=trigger_test_block,
                trigger_methods=trigger_methods,
                max_rules=getattr(args, 'synth_max_rules', 8),
                divergences=_divergences)
            if candidates:
                print(f"  [synth] {len(candidates)} candidate relation(s) "
                      f"({synth_model}): "
                      f"{', '.join(r.name for r in candidates)}")
            else:
                print("  [synth] WARNING: synthesis returned no candidates "
                      "(after one retry) — nothing to screen or inject")
            # NO pooling (2026-07-19, user decision): sharing relations
            # between a bug's legs — even within one run — is judged the
            # same benchmark-farming shape as cross-run pooling: a leg's
            # verdict must rest on what THIS leg derives from the bug
            # alone, or nothing transfers to a patch seen once. The
            # compensation for synthesis stochasticity is MORE OWN rules
            # per leg (--synth_max_rules default raised, all screened
            # survivors replayed), never a sibling's.
            synthesized_relations = candidates
            record_extras["synth_candidates"] = len(candidates)
            # Keep references to EVERY candidate (pre-screen) so the trace can
            # dump the ones screening later drops, with the reason —
            # screen_relations attaches .screen_decision to
            # these same objects.
            _all_candidates = list(candidates)

    # 5) Build the chat-completion prompt. Rather than a single fixed
    #    prompt, we wrap PromptBuilder in a factory the campaign calls
    #    before each fresh attempt: it injects which reachable functions
    #    and crashes the set already covers, so each new harness is a
    #    *variant* steered at the uncovered part of the root-cause region.
    #
    #    For semantic bugs with several trigger tests, we additionally
    #    round-robin which test each attempt lifts its assertion from, so the
    #    harness set spreads across all of the bug's failing behaviours rather
    #    than piling onto the first. The campaign passes no attempt index, so
    #    the closure keeps its own counter (one tick per fresh prompt build).
    prompt_builder = PromptBuilder(language=args.language)
    # Relations actually shown to the harness generator. Filled after
    # screening (6a-pre): at most 2 of this leg's OWN relations, best-first.
    # Pooled sibling-leg relations never enter the prompt — injected pool
    # mass displaced the generator's own free-form checks in p23gate
    # (Lang-60-o lost the capacity oracle that convicted at baseline).
    prompt_relations = []
    _rr_state = {"i": 0, "s": 0, "m": 0}
    # Rotate the variant strategy across the harness SET so each is tried by
    # at least one harness — otherwise the model picks one stochastically and
    # may never write, e.g., the consistency cross-check that catches a
    # masked-symptom bug. One tick per fresh prompt build.
    _STRATEGIES = ['a', 'b', 'c']

    def prompt_factory(covered_functions, found_signatures,
                       accepted_families=None):
        semantic_test = None
        if bug_kind == "semantic" and failure_tests:
            semantic_test = failure_tests[_rr_state["i"] % len(failure_tests)]
            _rr_state["i"] += 1
            print(f"  [semantic] lifting assertion from "
                  f"{semantic_test.test_class}::{semantic_test.test_method}")
        strategy = _STRATEGIES[_rr_state["s"] % len(_STRATEGIES)]
        _rr_state["s"] += 1
        if context.root_cause_reachable:
            print(f"  [variant] assigned strategy ({strategy})")
        # Mechanism rotation (semantic): each harness carries the lifted
        # trigger block plus ONE extra oracle mechanism, instead of every
        # prompt stacking all of them — stacked blocks contradicted each
        # other and a flood of injected pairs once distracted the generator
        # off a bug it had been catching. Only mechanisms that actually
        # have content this run enter the rotation. NOTE: reads the
        # closure variables at call time, so it automatically sees the
        # post-screen `prompt_relations`.
        mechanism = None
        if bug_kind == "semantic":
            mechs = ['consistency']
            if prompt_relations:
                mechs.append('relations')
            if len(mechs) > 1:
                mechanism = mechs[_rr_state["m"] % len(mechs)]
                _rr_state["m"] += 1
                print(f"  [mechanism] assigned ({mechanism})")
        return prompt_builder.build(
            buggy_dir=selection.buggy_dir,
            context=context,
            failure_tests=failure_tests,
            covered_functions=covered_functions,
            found_signatures=found_signatures,
            accepted_families=accepted_families,
            crash_input=crash_input,
            bug_kind=bug_kind,
            semantic_test=semantic_test,
            variant_strategy=strategy,
            # When the relation verifier will screen fired oracles (6b),
            # the prompt may push for strong, JUSTIFIED assertions instead
            # of hedging to vacuous ones (the verifier is a backstop, and
            # the prompt says so without overpromising).
            verifier_enabled=args.verify_relations,
            synthesized_relations=prompt_relations,
            oracle_mechanism=mechanism,
            touched_javadocs=touched_javadocs,
            # Only the 'consistency' slot renders this (one
            # skeleton-aware harness per set); other slots stay lean.
            class_context=class_ctx,
            sibling_hints=sibling_hints or None,
        )

    # 6) Run the campaign: regenerate, recompile, and (by default) verify
    #    each compiled harness crashes the BUGGY version before accepting
    #    it. Acceptance = "compiles AND triggers".
    builder = HarnessBuilder(jazzer_api_jar=jazzer_api_jar)

    # 6a-pre) Mechanically screen the synthesized relation candidates on
    #    the BUGGY build (needs the builder + jazzer driver, hence here).
    #    Candidates that fire indiscriminately on known-mostly-correct
    #    behaviour are out-of-domain and dropped; only survivors reach the
    #    prompt factory (which reads this variable at call time — the
    #    initial prompt is deliberately built AFTER this point). No driver
    #    jar / screen failure => nothing is injected: unscreened candidates
    #    never reach a prompt under any circumstances.
    if synthesized_relations:
        if jazzer_standalone_jar:
            print("\n" + "#" * 20 + " relation screening " + "#" * 20)
            from java.relations.relation_screen import screen_relations
            # P2.2 direction/determinism check needs the failing test's own
            # input literals (the values the bug is about). Same extraction
            # as the acceptance-gate seed corpus below.
            from java.parsing.java_source import trigger_seed_literals
            _trig_lits = trigger_seed_literals(
                [getattr(ft, 'method_source', '') for ft in failure_tests])
            try:
                # max_keep=8: the old cap of 3 sized the PROMPT; the prompt
                # is now sliced separately (prompt_relations, ≤2 own-leg),
                # so screening may keep more survivors for pool/replay use.
                # R1 compile-repair: one model call to fix a candidate that
                # fails to compile, before dropping it. On behind
                # --rule_compile_repair so it can be measured on/off.
                _repair = None
                if getattr(args, 'rule_compile_repair', False):
                    _imp = context.source_imports
                    _repair = (lambda rel, err:
                               synthesizer.repair_check(rel, err, imports=_imp))
                synthesized_relations = screen_relations(
                    synthesized_relations,
                    builder=builder,
                    buggy_dir=selection.buggy_dir,
                    jazzer_standalone_jar=jazzer_standalone_jar,
                    package=context.package,
                    imports=context.source_imports,
                    jazzer_api_jar=jazzer_api_jar,
                    trigger_literals=_trig_lits,
                    max_keep=12,
                    repair_fn=_repair,
                    runs=args.screen_runs,
                    patched_classes=_patched_classes,
                    documented_exceptions=_doc_exc,
                    divergence_values=_divergence_values,
                )
                record_event('deterministic', method='screening-survivors',
                             output={'kept': [getattr(r, 'name', '?')
                                              for r in synthesized_relations],
                                     'count': len(synthesized_relations)})
            except Exception as exc:
                print(f"  [screen] screening failed ({exc}) — dropping all "
                      "candidates rather than injecting unscreened")
                synthesized_relations = []
            print(f"  [screen] {len(synthesized_relations)} relation(s) "
                  "survived screening")
            # Spec N (cycle 3b) convergence gate: the campaign may not settle
            # for an arsenal with ZERO non-seed detection power — a kept set
            # that only ever fires on the memorized failing-test literals has
            # learned nothing general about the defect. Bounded (+2 rounds),
            # fail-soft (keeps what it has on any error).
            try:
                from java.harness.campaign import converge_nonseed_arsenal
                synthesized_relations = converge_nonseed_arsenal(
                    synthesized_relations,
                    synth_round=lambda: synthesizer.synthesize(
                        patched_sources, class_name,
                        context.root_cause_reachable or [], [], '',
                        patch_text=context.patch_text or '',
                        javadocs=touched_javadocs,
                        class_context=class_ctx,
                        source_imports=context.source_imports,
                        trigger_test_block=trigger_test_block,
                        trigger_methods=trigger_methods,
                        max_rules=getattr(args, 'synth_max_rules', 8),
                        divergences=_divergences),
                    screen_round=lambda cands: screen_relations(
                        cands, builder=builder,
                        buggy_dir=selection.buggy_dir,
                        jazzer_standalone_jar=jazzer_standalone_jar,
                        package=context.package,
                        imports=context.source_imports,
                        jazzer_api_jar=jazzer_api_jar,
                        trigger_literals=_trig_lits,
                        max_keep=12, repair_fn=_repair,
                        runs=args.screen_runs,
                        patched_classes=_patched_classes,
                        documented_exceptions=_doc_exc,
                        divergence_values=_divergence_values),
                    max_extra_rounds=2,
                    min_extra_rounds=1,
                )
            except Exception as _cg_exc:
                print(f"  [convergence] gate unavailable ({_cg_exc}) — "
                      "keeping current arsenal")
            # W1.1 (p23gate regression fix): the harness prompt sees at most
            # 2 relations, best-first (screening returns direction-confirmed
            # first), and ONLY this leg's own — pooled sibling-leg relations
            # are screening/replay material, never prompt material.
            prompt_relations = [
                r for r in synthesized_relations
                if not getattr(r, 'from_pool', False)
                and not getattr(r, 'screen_note', '').startswith(
                    'INVERTED-SUSPECT')][:2]
            if len(prompt_relations) != len(synthesized_relations):
                print(f"  [screen] prompt gets {len(prompt_relations)} "
                      f"relation(s) (own-leg, best-first); the other "
                      f"{len(synthesized_relations) - len(prompt_relations)} "
                      "stay screening/replay-only")
            # (Pool persistence removed 2026-07-19 with the pooling ban —
            # nothing a leg screens is visible to any other leg.)
        else:
            print("  [screen] no jazzer driver available — dropping all "
                  "candidates rather than injecting unscreened")
            synthesized_relations = []
        record_extras["synth_survivors"] = len(synthesized_relations)
        # p1b step 3 part 1 — ADMISSION WIDENING, right after screening.
        # This is the earliest point at which the KEPT checks are known, and
        # it is before any judging, so every door that later looks a
        # reference up by its firing's own observable finds whatever this
        # produced. Bounded by config.P1B_MAX_REFERENCES per leg; fails open.
        if (getattr(args, 'reference_impl', False) and synthesized_relations
                and bug_kind == "semantic"
                and not getattr(args, 'rulegen_only', False)):
            try:
                _widen_admissions(
                    args=args,
                    checks=[getattr(r, 'check', '') or ''
                            for r in synthesized_relations],
                    class_ctx=class_ctx, failure_tests=failure_tests,
                    builder=builder, buggy_dir=selection.buggy_dir,
                    patch_path=selection.patch_path,
                    package=context.package, imports=context.source_imports,
                    patched_methods=_patched_method_names(context))
            except Exception as _wexc:
                print(f"  [widening] admission widening unavailable "
                      f"({_wexc}) — the leg keeps whatever it had")

    # RULE-GENERATION QUALITY MODE: replay the screened relations directly
    # against THIS leg's patched build, record what fired, and stop before
    # the expensive harness-generation + judge stages. Everything here
    # reuses the exact synthesis/screen/replay the full pipeline uses — the
    # only difference is we skip steps 6+. Metrics land in the record so an
    # offline join of a bug's -o and -c legs gives convict-recall and
    # false-fire per bug.
    if args.rulegen_only:
        replay_fired = []
        if (synthesized_relations and jazzer_standalone_jar
                and bug_kind == "semantic"):
            try:
                from java.relations.relation_screen import replay_on_patched
                _pdir = PatchedProjectBuilder().build_patched_dir(
                    selection.buggy_dir, selection.patch_path)
                _tl = []
                for ft in failure_tests:
                    if getattr(ft, 'method_source', None):
                        _tl += re.findall(
                            r'"((?:[^"\\]|\\.){1,120})"', ft.method_source)
                _tl = [s for s in dict.fromkeys(_tl) if s.strip()][:32]
                _f = replay_on_patched(
                    synthesized_relations, builder=builder,
                    patched_dir=_pdir,
                    jazzer_standalone_jar=jazzer_standalone_jar,
                    package=context.package, imports=context.source_imports,
                    jazzer_api_jar=jazzer_api_jar, trigger_literals=_tl,
                    runs=args.screen_runs)
                replay_fired = [
                    {'name': x['name'], 'tier': x['tier'],
                     'note': x['note'],
                     'fired_lines': x.get('fired_lines', [])} for x in _f]
            except (PatchApplyError, TriggerVerificationError) as exc:
                record_extras['rulegen_status'] = getattr(
                    exc, 'status', 'bad_patch')
            except Exception as exc:
                record_extras['rulegen_status'] = f'replay_error: {exc}'
        record_extras['rulegen_only'] = True
        record_extras['relation_replay_fired'] = replay_fired
        record_extras['screened_relation_names'] = [
            getattr(r, 'name', '?') for r in (synthesized_relations or [])]
        print(f"\n[rulegen] candidates={record_extras.get('synth_candidates',0)}"
              f" survivors={len(synthesized_relations or [])}"
              f" replay-fired={len(replay_fired)}: "
              f"{[x['name'] for x in replay_fired]}")
        # Full inspectable trace: the exact prompt + context the model saw,
        # and every surviving rule with its full body (name/kind/contract/
        # input/check/screen_note) — so a run.log's names can be read as
        # actual rules against the actual context.
        try:
            if args.results_json:
                _tp = os.path.join(os.path.dirname(args.results_json),
                                   'trace.md')
                _write_trace_md(
                    _tp, f"{selection.project_name}-{selection.bug_id}",
                    'correct' if args.correct else 'overfitting',
                    get_events(), outcome='rulegen_only (no harness/verdict)')
                print(f"  [trace] wrote {_tp} ({len(get_events())} steps)")
        except Exception as _e:
            print(f"  [trace] dump failed: {_e}")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status='rulegen_only', selection=selection,
                     result=None, bug_kind=bug_kind, extras=record_extras)
        _print_token_usage()
        sys.exit(0)

    # Throwable names this bug raises, gathered from D4J root-cause metadata
    # and the captured runtime crash. Both fully-qualified and simple names
    # are included so detection matches whichever form Jazzer prints. Used by
    # both the in-campaign verifier (buggy code) and the post-campaign
    # FuzzRunner (patched code) so crash detection is symmetric — otherwise a
    # harness accepted as crashing could be wrongly reported clean on the
    # patched code, masking an overfitting patch.
    # Throwable names that count as "the harness fired", used by both the
    # in-campaign verifier (buggy code) and the post-campaign FuzzRunner
    # (patched code) so detection is symmetric — otherwise a harness accepted
    # as triggering could be wrongly reported clean on the patched code,
    # masking an overfitting patch.
    #
    # The two bug kinds fire differently:
    #   * crashing — the bug's OWN throwable escapes the library. Expect the
    #     root-cause exception type (from D4J metadata + captured runtime
    #     crash), both fully-qualified and simple.
    #   * semantic — the library throws nothing; the HARNESS throws when the
    #     lifted assertion fails. Expect the throwable the harness raises
    #     (Jazzer's FuzzerSecurityIssue*, or a bare AssertionError if the
    #     model used assert/JUnit instead). Note Jazzer also flags an
    #     uncaught throwable via its finding exit code and crash markers, so
    #     this list mainly hardens the deterministic-first-input (rc=1) path.
    expected_exceptions = []
    if bug_kind == "crashing":
        for ft in failure_tests:
            if ft.exception_type:
                expected_exceptions.append(ft.exception_type)
                expected_exceptions.append(
                    ft.exception_type.rsplit('.', 1)[-1])
        if crash_input is not None and crash_input.exception_type:
            expected_exceptions.append(crash_input.exception_type)
            expected_exceptions.append(
                crash_input.exception_type.rsplit('.', 1)[-1])
    else:  # semantic
        expected_exceptions = [
            'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow',
            'com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium',
            'com.code_intelligence.jazzer.api.FuzzerSecurityIssueHigh',
            'FuzzerSecurityIssueLow',
            'FuzzerSecurityIssueMedium',
            'FuzzerSecurityIssueHigh',
            'java.lang.AssertionError',
            'AssertionError',
        ]
    seen = set()
    expected_exceptions = [e for e in expected_exceptions
                           if e and not (e in seen or seen.add(e))]

    # Seed corpus for the buggy-version trigger gate: string literals from
    # the trigger tests, written one per file. libFuzzer
    # starts from these instead of from nothing, so the gate's short budget
    # begins in the neighbourhood of known-valid inputs — the region an
    # overfit special-cased — rather than spending it discovering input
    # shape. Best-effort: no literals, no corpus, no behaviour change.
    # SEMANTIC BUGS ONLY: only the acceptance gate is seeded, never the
    # patched-build fuzz run, so for a crashing bug a harness accepted
    # only because a seed reached the trigger may fail to re-find it on
    # the patched build within budget — a TP lost at the second stage.
    # Semantic harnesses construct their inputs in code and are far less
    # corpus-dependent; the asymmetry is harmless there.
    corpus_dir = None
    # ALL bug kinds now (the semantic-only asymmetry existed because only
    # the acceptance gate was seeded; both sides are seeded identically
    # now, so a seed that reaches the trigger at acceptance also starts
    # the patched fuzz). Strings AND numerics, plus their mechanical
    # variations (suffix case/addition/removal incl. exponent forms,
    # sign flips, integer neighbours) — batch5 showed every invented
    # check present yet latent because random fuzz never generated the
    # discriminating literal shape within budget.
    from java.parsing.java_source import literal_variations, trigger_seed_literals
    seed_literals = trigger_seed_literals(
        [getattr(ft, 'method_source', '') or '' for ft in failure_tests],
        cap=48)
    seed_literals = literal_variations(seed_literals, cap=96)
    if seed_literals:
        corpus_path = Path(selection.buggy_dir) / 'fuzz' / 'corpus'
        try:
            corpus_path.mkdir(parents=True, exist_ok=True)
            for i, lit in enumerate(seed_literals):
                (corpus_path / f'seed_{i:03d}').write_text(
                    lit, encoding='utf-8', errors='replace')
            corpus_dir = str(corpus_path)
            print(f"  [corpus] seeded {len(seed_literals)} test literals "
                  f"into {corpus_dir}")
        except OSError as exc:
            print(f"  [corpus] seeding failed ({exc}); continuing without")

    verifier = None
    buggy_cp = None  # resolved lazily; also reused by the attribution check
    if args.require_trigger:
        # Resolve the buggy classpath once (compiles the project), then
        # hand it to the verifier so each gate run is just a Jazzer
        # invocation.
        buggy_cp = builder.test_classpath(selection.buggy_dir)
        verify_timeout = (args.verify_timeout
                          if args.verify_timeout is not None
                          else config.VERIFY_TIMEOUT_SECONDS)
        verifier = HarnessVerifier(
            jazzer_standalone_jar=jazzer_standalone_jar,
            buggy_classpath=buggy_cp,
            timeout_seconds=verify_timeout,
            expected_exceptions=expected_exceptions,
            jazzer_api_jar=jazzer_api_jar,
            corpus_dir=corpus_dir,
        )

    # ONE model, from the input parameters. The two-tier
    # cheap-primary/escalate-to-strong path was deleted 2026-08-06: inert in
    # every measured run (the defaults made primary == escalation, so the
    # guard was always false), and its enabling hypothesis -- a nano primary --
    # was measured dead in cycle 1, where nano could not exercise this
    # machinery at all (Closure-70 ended no_harnesses).
    harness_model = args.model or config.LOCAL_LLM_MODEL
    primary_gen = HarnessGenerator(model=harness_model,
                                   temperature=0.6, top_p=1.0)
    print(f"  [model] {harness_model}")

    # The set-empty prompt used for attempt 1 — built HERE, after relation
    # screening, so the very first prompt already reflects the screened
    # (not raw) candidate set.
    messages = prompt_factory([], [])

    # H3: the wrong values the real trigger tests observe on the buggy
    # build (parsed from their captured failure messages). Semantic bugs
    # only — a crash-shaped failure has no expected/actual pair, and
    # real_wrong_values returns [] there, which disables the gate.
    from java.execution.oracle_strength import real_wrong_values
    trigger_wrong_values = (
        real_wrong_values([ft.failure_message for ft in failure_tests])
        if bug_kind == 'semantic' else [])
    if trigger_wrong_values:
        print(f"  [H3] real wrong value(s) on buggy: {trigger_wrong_values}")

    campaign = HarnessCampaign(
        generator=primary_gen,
        builder=builder,
        target_successes=args.target_successes,
        max_attempts=args.max_attempts,
        max_repair_failures=args.max_repair_failures,
        verifier=verifier,
        require_trigger=args.require_trigger,
        trigger_wrong_values=trigger_wrong_values,
    )
    result = campaign.run(messages, selection.buggy_dir,
                          prompt_factory=prompt_factory,
                          patch_text=context.patch_text)

    _print_summary(selection, result)

    # RETRY (one aimed extra attempt): when the ACCEPTED set is dominated
    # by test-copy / crash-reproduction checks, an overfitting patch
    # passes it by construction — the overfit is built to pass the test's
    # own scenario, so only INVENTED contract checks (sibling-family,
    # metamorphic variation, hidden-state) can convict it. Batch4
    # Lang-27-o: three accepted harnesses, ONE invented check among them,
    # nothing fired on patched. Mechanical trigger, one extra attempt,
    # never more (each extra harness is a false-alarm lottery ticket on
    # the correct sibling — the blanket-increase rejection stands).
    if result.successful_results:
        from java.parsing.java_source import oracle_ids_in_text as _oids_r
        _copyish = re.compile(r'lift|seed|crash|repro|root[-_]?cause',
                              re.I)
        _tnames = {getattr(ft, 'test_method', '') or ''
                   for ft in failure_tests}
        _invented: set = set()
        for _br in result.successful_results:
            try:
                with open(_br.harness_path, encoding='utf-8',
                          errors='replace') as _fh:
                    _ids_here = _oids_r(_fh.read())
            except OSError:
                continue
            for _oid in _ids_here:
                if _copyish.search(_oid):
                    continue
                if any(t and t in _oid for t in _tnames):
                    continue
                _invented.add(_oid)
        if len(_invented) < 2:
            print(f"\n  [RETRY] accepted set has only "
                  f"{len(_invented)} invented check(s) "
                  f"({sorted(_invented) or 'none'}) — one aimed extra "
                  f"attempt for contract-invented checks")
            _retry_messages = list(messages) + [{
                'role': 'user',
                'content': (
                    "AIMED RETRY — the harnesses accepted so far are "
                    "dominated by test-copy and crash-reproduction "
                    "checks. An overfitting patch is BUILT to pass the "
                    "failing test's own scenario, so those checks can "
                    "never convict it; only checks INVENTED from the "
                    "documented contract can. Write ONE more harness "
                    "whose value is invented checks: sibling-family "
                    "agreement (see the method-families block if shown), "
                    "metamorphic input variations (equivalent input "
                    "forms must agree; a documented selection rule must "
                    "hold), and hidden-state/read-only checks (public "
                    "no-argument readers must not change across a "
                    "documented read-only call). Include the minimal "
                    "trigger reproduction so acceptance can verify the "
                    "harness reaches the defect, but add AT LEAST THREE "
                    "distinct invented checks with their own "
                    "[oracle:<short-id>] names. All previous rules "
                    "(compilation, exception fencing, oracle naming, "
                    "whitespace normalization) still apply. Return the "
                    "full FuzzHarness.java. Raw Java source only. No "
                    "markdown fences.")}]
            _retry_campaign = HarnessCampaign(
                generator=primary_gen,
                builder=builder,
                target_successes=1,
                max_attempts=3,
                max_repair_failures=args.max_repair_failures,
                verifier=verifier,
                require_trigger=args.require_trigger,
                trigger_wrong_values=trigger_wrong_values,
            )
            _retry_result = _retry_campaign.run(
                _retry_messages, selection.buggy_dir,
                patch_text=context.patch_text)
            if _retry_result.successful_results:
                result.successful_results.extend(
                    _retry_result.successful_results)
                try:
                    result.accepted_trigger_details.extend(
                        getattr(_retry_result,
                                'accepted_trigger_details', []) or [])
                except Exception:
                    pass
                record_extras['retry_harness_added'] = len(
                    _retry_result.successful_results)
                print(f"  [RETRY] added "
                      f"{len(_retry_result.successful_results)} "
                      f"aimed harness(es) to the set")
            else:
                record_extras['retry_harness_added'] = 0
                print("  [RETRY] aimed attempt produced no accepted "
                      "harness — set unchanged")

    # 6-0) P2.3: injected-but-not-implemented check. A screened relation
    #    handed to harness generation is worthless if no accepted harness
    #    actually contains it — the model can silently drop a relation it
    #    cannot implement (Math-2's convicting mean-relation needed a
    #    forbidden subclass). Diff the injected relation names against the
    #    accepted harness sources and log any that never made it in.
    if synthesized_relations and result.successful_results:
        accepted_src = ''
        for br in result.successful_results:
            try:
                with open(br.harness_path, encoding='utf-8',
                          errors='replace') as fh:
                    accepted_src += fh.read() + '\n'
            except OSError:
                pass
        missing = [getattr(r, 'name', '?') for r in synthesized_relations
                   if getattr(r, 'name', '') and
                   getattr(r, 'name') not in accepted_src]
        if missing:
            print(f"  [synth] INJECTED-BUT-NOT-IMPLEMENTED: {missing} — "
                  f"screened relation(s) that no accepted harness contains "
                  f"(the model dropped them; may be unimplementable under "
                  f"the harness rules)")
            record_extras['relations_not_implemented'] = missing

    # 6-bis) P0.4 latent-oracle scan: acceptance is whole-harness ("did
    #    ANYTHING fire on buggy?"), so a check listed after an
    #    always-firing one may never run at all — and then meets its
    #    first-ever execution on the patched build, where a false alarm
    #    from it has zero evidence behind it (Chart-26 attempt_003).
    #    Re-fuzz each accepted harness on the BUGGY build with keep_going
    #    and record which named oracles ever fire; the rest are LATENT.
    #    v1: flag loudly + hand to the verifier as context. No cutting.
    latent_map: dict = {}
    # P3.3: per-harness map {oracle id -> exception types underlying its
    # BUGGY-side firings}, from the same keep_going scan. On the patched
    # build, a firing of the same oracle from a disjoint set of underlying
    # exception types is a DIFFERENT crash wearing the same alarm.
    buggy_crash_types: dict = {}
    if result.successful_results and jazzer_standalone_jar:
        from java.parsing.java_source import oracle_ids_in_text
        from java.execution.fuzz_runner import per_oracle_crash_types
        _fr_lat = FuzzRunner(
            jazzer_standalone_jar=jazzer_standalone_jar,
            timeout_seconds=args.fuzz_timeout,
            expected_exceptions=expected_exceptions,
            jazzer_api_jar=jazzer_api_jar,
        )
        if buggy_cp is None:
            buggy_cp = builder.test_classpath(selection.buggy_dir)
        print("\n" + "#" * 20 + " latent-oracle scan (buggy) " + "#" * 20)
        for br in result.successful_results:
            try:
                with open(br.harness_path, encoding='utf-8',
                          errors='replace') as fh:
                    declared = oracle_ids_in_text(fh.read())
            except OSError:
                declared = set()
            if not declared:
                print(f"  {br.attempt_label or br.class_name}: no named "
                      f"oracles found in source — cannot scan")
                continue
            out = _fr_lat.keep_going_output(
                br.harness_path, br.class_name, buggy_cp)
            if not out:
                print(f"  {br.attempt_label or br.class_name}: scan run "
                      f"failed — no latent information")
                continue
            fired = oracle_ids_in_text(out)
            latent = declared - fired
            latent_map[br.harness_path] = latent
            _octypes = per_oracle_crash_types(out)
            if _octypes:
                buggy_crash_types[br.harness_path] = _octypes
                print(f"  {br.attempt_label or br.class_name}: buggy-side "
                      f"crash identity per oracle: "
                      + ", ".join(f"{k}={sorted(v)}"
                                  for k, v in sorted(_octypes.items())))
            if latent:
                print(f"  {br.attempt_label or br.class_name}: LATENT "
                      f"oracle(s) never fired on buggy: "
                      f"{sorted(latent)} (fired: {sorted(fired & declared)})"
                      f" — a patched-build firing from these has no "
                      f"buggy-side evidence behind it")
            else:
                print(f"  {br.attempt_label or br.class_name}: all "
                      f"{len(declared)} named oracle(s) exercised on buggy")
        record_extras['latent_oracles'] = {
            os.path.basename(k): sorted(v)
            for k, v in latent_map.items() if v}

    # 7) Fuzz every successful harness against the patched code to check
    #    whether the vulnerability is still reachable (overfitting signal).
    fuzz_results = None
    if args.fuzz_timeout > 0 and result.successful_results:
        print("\n" + "#" * 20 + " fuzzing patched code " + "#" * 20)
        try:
            _runner = FuzzRunner(
                jazzer_standalone_jar=jazzer_standalone_jar,
                timeout_seconds=args.fuzz_timeout,
                expected_exceptions=expected_exceptions,
                jazzer_api_jar=jazzer_api_jar,
                seed_literals=seed_literals,
                diffcov=args.diffcov,
            )
            fuzz_results = _runner.run_all(
                successful_results=result.successful_results,
                patch_path=selection.patch_path,
                buggy_dir=selection.buggy_dir,
            )
            _print_fuzz_summary(fuzz_results)
            _record_diffcov(_runner, fuzz_results, record_extras)
            for _fr in (fuzz_results or []):
                _fired = getattr(_fr, 'triggered', False)
                _kw = {}
                if _fired:
                    _blob = ((getattr(_fr, 'stderr', '') or '') + '\n'
                             + (getattr(_fr, 'stdout', '') or ''))
                    # The oracle message names the DISCRIMINATING INPUT and the
                    # mismatch, e.g. "x.add(new Complex(1, NaN)).getReal()
                    # expected NaN but got 4.0" — i.e. exactly what caught it.
                    _m = re.search(r'\[oracle:[^\]]*\][^\n]*', _blob)
                    _out = ('FIRED — ' + (_m.group(0)[:500] if _m
                                          else 'crash on patched build'))
                    # The raw reproducing input Jazzer persisted (the bytes the
                    # FuzzedDataProvider decoded into that triggering input).
                    _art = getattr(_fr, 'artifact_path', None)
                    if _art:
                        _kw['reproducing_input_file'] = _art
                else:
                    _out = 'quiet on patched build (no overfit signal)'
                record_event(
                    'deterministic', method='patched-fuzz',
                    target=getattr(_fr, 'attempt_label', 'harness'),
                    output=_out, **_kw)
        except (PatchApplyError, TriggerVerificationError) as exc:
            # P0.1 safety net, patched half. These are NOT generic infra
            # hiccups: the program under test is not what we believe it
            # is. Recording 'no_harnesses' here is how do-nothing runs
            # got counted as passes for weeks — mark the run with its
            # specific unscoreable status and stop.
            status = getattr(exc, 'status', 'bad_patch')
            print(f"\nSAFETY NET: {exc}")
            _emit_record(args.results_json,
                         label='correct' if args.correct else 'overfitting',
                         status=status, selection=selection,
                         result=result, bug_kind=bug_kind,
                         extras={**(record_extras or {}),
                                 'safety_net': str(exc)})
            _print_token_usage()
            sys.exit(5)
        except Exception as exc:
            print(f"  patched-code fuzzing failed: {exc}")

    # 7b) Differential-firing ATTRIBUTION check — mechanical, label-free,
    #     and independent of the LLM verifier (which judges oracle
    #     SOUNDNESS; it cannot judge whether the patch CAUSED the firing —
    #     every verifier config kept the Lang-27-shaped FP because "a
    #     correct parser shouldn't expose internal crashes" is plausible
    #     and wrong about that codebase). Scoped by firing KIND:
    #     * Our own oracle throws (FuzzerSecurityIssue*, RuntimeException
    #       relation/consistency messages) firing on buggy are the TP
    #       signal — the patch failed to fix that family member. Never
    #       touched here; is_generic_escape() excludes them by class.
    #     * An escaped GENERIC JDK exception whose EXACT firing input
    #       reproduces the SAME crash signature on the buggy build is
    #       pre-existing crash surface, not patch-caused. Dropped, loudly
    #       — unless a keep_going re-fuzz shows a non-generic oracle also
    #       fires, in which case the finding stands on that oracle.
    #     Abstains (falls through to the verifier with a note) whenever
    #     the artifact is missing, the replay doesn't crash, or either
    #     signature lacks a stack-frame anchor ('Exc@Class.method') —
    #     a frame-less type-only match could equate two different crashes.
    #     SEMANTIC BUGS ONLY. For a crashing bug, "the same crash
    #     reproduces on the buggy build" is the TP condition itself —
    #     every accepted harness reproduces on buggy by construction
    #     (that's the acceptance gate), and an overfitting patch that
    #     fails to fix the crash fires with the identical signature on
    #     the patched build. Running this check there would read exactly
    #     that TP pattern as "pre-existing surface" and flip it to an FN.
    attribution_notes: dict = {}
    if (bug_kind == "semantic"
            and fuzz_results and any(r.triggered for r in fuzz_results)):
        from java.execution.fuzz_runner import (cause_signature, crash_signature,
                                 is_generic_cause, is_generic_escape)
        from java.relations.evidence_facts import classify_differential_replay
        from java.execution.oracle_strength import exception_headline as _headline
        from java.parsing.java_source import oracle_ids_in_text as _oids_attr

        def _non_alarm_escape(h):
            # ANY escaped exception (not our alarm, no oracle ID) on a
            # SEMANTIC leg is differential-replay eligible, not just the
            # JDK generics: a semantic oracle is an alarm throw, so an
            # escaped library exception — including library validation
            # types like NotPositiveException — can only be an input
            # rejection or pre-existing surface. If its exact input
            # reproduces the same crash on buggy, it is not the patch's
            # fault. (minfix_w2b Math-2-c: a constructor
            # NotPositiveException on a junk fuzzed input was kept by the
            # verifier as a conviction of the CORRECT patch.)
            return bool(h) and 'FuzzerSecurityIssue' not in h \
                and not _oids_attr(h)
        generic_hits = [
            r for r in fuzz_results if r.triggered
            and (is_generic_escape(_headline((r.stdout or '') + '\n'
                                             + (r.stderr or '')))
                 or _non_alarm_escape(_headline((r.stdout or '') + '\n'
                                                + (r.stderr or ''))))
        ]
        # P0.3: LAUNDERED firings — the headline is a harness-own alarm
        # (so the loop above skips it by design), but its `Caused by:`
        # chain bottoms out in a generic JDK escape. The alarm is just a
        # re-labelled library crash; whether it wraps or not is model
        # coin-flip, so without this check the same pre-existing crash is
        # dismissed one day and counted the next (the Chart-26 FP).
        laundered_hits = [
            r for r in fuzz_results if r.triggered
            and r not in generic_hits
            and is_generic_cause(cause_signature(
                (r.stdout or '') + '\n' + (r.stderr or '')))
        ]
        if generic_hits or laundered_hits:
            print("\n" + "#" * 20 + " attribution check " + "#" * 20)
            if buggy_cp is None:
                buggy_cp = builder.test_classpath(selection.buggy_dir)
            _fr = FuzzRunner(
                jazzer_standalone_jar=jazzer_standalone_jar,
                timeout_seconds=args.fuzz_timeout,
                expected_exceptions=expected_exceptions,
                jazzer_api_jar=jazzer_api_jar,
            )

            def _apply_preexisting_drop(r, patched_sig, extra_note=""):
                # The pre-existing drop path: the generic crash reproduces on
                # the buggy build, so drop it — UNLESS a NON-generic oracle in
                # the same harness also fires on the patched code, which must
                # not be buried by the pre-existing crash (the non-generic
                # sibling rescue). `extra_note` prefixes the note when the
                # pre-existence was established via a muted re-replay.
                fired_all = _fr.collect_fired_oracles(
                    r.harness_path, r.class_name,
                    selection.patch_path, selection.buggy_dir)
                non_generic = [f for f in fired_all
                               if f and not is_generic_escape(f)]
                if non_generic:
                    attribution_notes[id(r)] = (
                        extra_note
                        + f"differential replay: generic firing {patched_sig} "
                        f"reproduces identically on the buggy build "
                        f"(pre-existing surface, ignore it), but non-generic "
                        f"oracle(s) also fire: {'; '.join(non_generic[:3])}")
                    print(f"  ✓ kept: {patched_sig} is pre-existing, but "
                          f"non-generic oracles also fire: "
                          f"{non_generic[0][:80]}")
                    return
                drop_finding(
                    r, 'differential-preexisting',
                    f"{patched_sig} reproduces on the buggy build "
                    f"(pre-existing surface, not patch-caused)",
                    patched_sig=patched_sig)

            for r in generic_hits:
                out = (r.stdout or '') + '\n' + (r.stderr or '')
                patched_sig = crash_signature(out)
                if not r.artifact_path:
                    attribution_notes[id(r)] = (
                        "differential replay ABSTAINED: crashing input "
                        "artifact not captured")
                    print(f"  ? abstain (no artifact): {r.harness_path}")
                    continue
                if not patched_sig or '@' not in patched_sig:
                    attribution_notes[id(r)] = (
                        "differential replay ABSTAINED: patched-build crash "
                        "signature has no stack-frame anchor")
                    print(f"  ? abstain (frame-less signature): "
                          f"{r.harness_path}")
                    continue
                buggy_status, buggy_sig = _fr.replay_input_result(
                    r.harness_path, r.class_name, buggy_cp, r.artifact_path)
                _verdict, _dr_note = classify_differential_replay(
                    patched_sig, buggy_status, buggy_sig)
                if _verdict == "ABSTAIN":
                    # Spec B: the replay itself errored — attach the note and
                    # let the judge rule; NEVER drop mechanically (an infra
                    # failure must not manufacture evidence against the patch).
                    attribution_notes[id(r)] = _dr_note
                    print(f"  ? abstain (replay errored): {r.harness_path}")
                    continue
                if _verdict == "SHADOWED":
                    # Spec B: buggy died at its own alarm before reaching the
                    # patched crash site — uninformative, not exculpatory.
                    # Attach the note and let the judge rule; do NOT drop.
                    attribution_notes[id(r)] = _dr_note
                    print(f"  ~ shadowed (buggy alarm {buggy_sig} fires "
                          f"first): {r.harness_path}")
                    # Spec G-G3.2: silence ALL of the harness's own checks on
                    # the buggy build and replay this exact input. If the
                    # patched crash site now reproduces with the SAME
                    # signature, the crash pre-exists (it was merely hidden
                    # behind the harness's alarms) — upgrade to PREEXISTING
                    # (existing drop path + non-generic sibling rescue). A
                    # clean muted run is the INTRODUCED family. Bounded to one
                    # muted re-replay; any failure leaves SHADOWED intact.
                    try:
                        (_mstatus, _mfired, _mout,
                         _mdiverted) = _fr.replay_input_muted(
                            r.harness_path, r.class_name, buggy_cp,
                            r.artifact_path, mute_all=True,
                            builder=builder, buggy_dir=selection.buggy_dir)
                        _msig = crash_signature(_mout or '')
                        print(f"  [muted-replay] status={_mstatus} "
                              f"sig={_msig or 'none'} "
                              f"diverted={_mdiverted}")
                        if _mstatus == "crashed" and _msig == patched_sig \
                                and '@' in (_msig or ''):
                            print(f"  ~ shadowed->pre-existing: crash site "
                                  f"reproduces on buggy once the harness's "
                                  f"own checks are silenced: {r.harness_path}")
                            _apply_preexisting_drop(
                                r, patched_sig,
                                extra_note=(
                                    "muted re-replay: with the harness's own "
                                    "checks silenced, this exact input "
                                    "reproduces the SAME crash "
                                    f"({patched_sig}) on the buggy build — "
                                    "the crash pre-exists, it was hidden "
                                    "behind the harness's alarms. "))
                        elif _mstatus == "clean":
                            # Cycle-6: "clean" is only exculpatory-for-buggy
                            # when execution actually REACHED the crash site.
                            # A swallow-return catch that fired returned early
                            # and proves nothing; unknown is treated the same.
                            if _mdiverted is False:
                                attribution_notes[id(r)] += (
                                    " Muted re-replay: with the harness's own "
                                    "checks silenced, the buggy build runs "
                                    "this exact input cleanly — the crash does "
                                    "NOT pre-exist behind the shadow; the "
                                    "patch introduced it here.")
                            else:
                                attribution_notes[id(r)] += (
                                    " Muted re-replay: the buggy build did not "
                                    "crash on this exact input, but execution "
                                    "there was diverted (or possibly diverted "
                                    "— it could not be determined) by an "
                                    "exception the harness's own "
                                    "`catch (...) { return; }` swallowed, so "
                                    "the run may never have REACHED this crash "
                                    "site. No attribution either way; do NOT "
                                    "read the quiet run as evidence the patch "
                                    "introduced the crash.")
                        # error / mute_failed: leave SHADOWED unchanged.
                    except Exception as _mexc:
                        print(f"  [muted-replay] unavailable ({_mexc}) — "
                              f"SHADOWED unchanged")
                    continue
                if _verdict == "INTRODUCED":
                    attribution_notes[id(r)] = _dr_note
                    print(f"  ✓ patch-caused ({patched_sig}): "
                          f"{r.harness_path}")
                    continue
                # _verdict == "PREEXISTING": same generic crash on both builds.
                _apply_preexisting_drop(r, patched_sig)
            for r in laundered_hits:
                out = (r.stdout or '') + '\n' + (r.stderr or '')
                patched_cause = cause_signature(out)
                if not r.artifact_path:
                    attribution_notes[id(r)] = (
                        "laundering check ABSTAINED: harness-own alarm "
                        f"wraps generic cause {patched_cause}, but no "
                        "crashing-input artifact was captured")
                    print(f"  ? abstain (no artifact): {r.harness_path}")
                    continue
                if not patched_cause or '@' not in patched_cause:
                    attribution_notes[id(r)] = (
                        "laundering check ABSTAINED: wrapped cause "
                        f"'{patched_cause}' has no stack-frame anchor")
                    print(f"  ? abstain (frame-less cause): "
                          f"{r.harness_path}")
                    continue
                _lstatus, buggy_sig, buggy_cause = (
                    _fr.replay_input_signatures_result(
                        r.harness_path, r.class_name, buggy_cp,
                        r.artifact_path))
                if _lstatus == "error":
                    # Spec B (same rule as the generic path): an errored
                    # replay is not a clean buggy run — it must not read as
                    # "NOT the same pre-existing crash".
                    attribution_notes[id(r)] = (
                        "laundering check ABSTAINED: replaying the exact "
                        "firing input on the buggy build was unavailable "
                        "(the replay itself errored) — no attribution fact "
                        "either way; judge sceptically.")
                    print(f"  ? abstain (replay errored): {r.harness_path}")
                    continue
                # Pre-existing iff the same underlying crash appears on the
                # unpatched build — either escaping directly (headline) or
                # wrapped by the same alarm (cause). A DIFFERENT crash site
                # on buggy (e.g. the bug's own NPE) is the TP pattern and
                # must survive: that is exactly Chart-26's overfit side.
                if patched_cause in (buggy_sig, buggy_cause):
                    drop_finding(
                        r, 'differential-laundered',
                        f"harness alarm wraps {patched_cause}, which "
                        f"reproduces on the buggy build (pre-existing "
                        f"library surface)",
                        patched_cause=patched_cause)
                else:
                    attribution_notes[id(r)] = (
                        f"laundering check: alarm wraps generic cause "
                        f"{patched_cause}; buggy-build replay gives "
                        f"headline={buggy_sig or 'no crash'}, cause="
                        f"{buggy_cause or 'none'} — NOT the same "
                        f"pre-existing crash")
                    print(f"  ✓ kept (cause not pre-existing): "
                          f"{r.harness_path}")

    # 6b) [optional] Relation verification — a non-cheating FP filter. A
    #     harness that crashed the patched code is only evidence of
    #     overfitting if its ORACLE is sound (true for any correct impl).
    #     Ask an LLM critic; drop findings whose oracle is judged unsound
    #     (invented relations that also fire on correct code). Uses only the
    #     harness source, never the developer fix.
    if args.verify_relations and fuzz_results:
        triggered = [r for r in fuzz_results if r.triggered]
        if triggered:
            print("\n" + "#" * 20 + " relation verification " + "#" * 20)
            from java.relations.relation_verifier import RelationVerifier
            from java.execution.oracle_strength import exception_headline, crash_excerpt
            # Thread the run's model explicitly: the default
            # HarnessGenerator resolves from .env, and a stale deployment
            # there once 404'd EVERY verify call — the verifier then
            # fail-opened on all of them and the whole stage silently
            # became a no-op that "kept" everything. Same tier logic as
            # synthesis: judging soundness is flagship work.
            verifier_model = args.model or config.LOCAL_LLM_MODEL
            print(f"  [verifier] model={verifier_model}, "
                  f"votes={config.RELATION_VERIFIER_VOTES}")
            rv = RelationVerifier(
                HarnessGenerator(model=verifier_model,
                                 temperature=0.0, top_p=1.0),
                votes=config.RELATION_VERIFIER_VOTES)
            fr = FuzzRunner(
                jazzer_standalone_jar=jazzer_standalone_jar,
                timeout_seconds=args.fuzz_timeout,
                expected_exceptions=expected_exceptions,
                jazzer_api_jar=jazzer_api_jar,
            )
            # EXPECTED values lifted from the trigger tests' own equality
            # assertions (assertEquals first-arg literals). An assertion
            # that fires by disagreeing with one of these is checking
            # GROUND TRUTH (the correct code is known to produce these
            # values), so the verifier must not reject it as an over-tight
            # speculative relation. NOT the input literals — feeding inputs
            # into this channel made the short-circuit protect the wrong
            # thing. Bug-agnostic: empty on any extraction miss, which just
            # falls back to plain per-oracle review.
            trusted_values = []
            for ft in failure_tests:
                if getattr(ft, 'method_source', None):
                    trusted_values.extend(
                        expected_assert_literals(
                            ft.method_source))
            trusted_values = list(dict.fromkeys(trusted_values))
            # P4.3 reconciliation state: oracle IDs the verifier judged
            # unsound anywhere this run, and the findings it kept.
            _unsound_oracle_ids: dict = {}
            _unsound_scope: dict = {}      # oracle id -> harness paths
            _kept_findings: list = []
            # Names of INJECTED relations: the same name in two harnesses
            # is genuinely the same check (both implement the injected
            # relation), so a dismissal transfers. A model-invented ID
            # (e.g. `lifted-test`) can name DIFFERENT checks in different
            # harnesses — full30 lost the Closure-62-o catch when an
            # unsound verdict on one harness's `lifted-test` killed the
            # sound keep of another's — so those reconcile only within
            # the same harness.
            _injected_rel_names = {
                getattr(_rel, 'name', '')
                for _rel in (synthesized_relations or [])} - {''}
            # Spec M (cycle-3b): universal screening builds one counting
            # harness per measured oracle; this run-level counter keeps their
            # build subdirs globally unique across legs.
            _universal_seq = 0
            for r in triggered:
                try:
                    with open(r.harness_path) as fh:
                        src = fh.read()
                except OSError:
                    continue
                # Spec M: per-leg cache of universal-screen results, keyed by
                # ORACLE id (so repeat firings of the same oracle never
                # re-measure, and one oracle's result never suppresses
                # another's measurement), plus the per-leg
                # `_UNIVERSAL_SCREEN_CAP` on MEASURED oracles.
                _universal_facts: dict = {}   # oracle id -> [fact notes]
                _universal_measured = 0
                # Cycle-6 item 4 PART B: per-leg record of the KNOWN buggy-side
                # rate per oracle id (oracle id -> (checked, violated)), from
                # whichever measurement produced it — the matched relation's
                # buggy screen or the universal screen. A known rate must reach
                # the judge on the harness track no matter which structural
                # branch measured it.
                _buggy_rate_counts: dict = {}
                # Collect EVERY oracle that fires on the patched code, not
                # just the first Jazzer surfaced — a multi-oracle harness
                # can fire via a sound oracle on one input and an unsound
                # one on another, and judging only the surfaced firing would
                # let the unsound sibling sink the finding. Re-fuzz with
                # --keep_going, and ALWAYS union in the originally captured
                # headline: the re-fuzz is nondeterministic and may surface
                # a different oracle set, and the original firing must never
                # drop out of the judged list just because it didn't
                # re-fire.
                single = exception_headline(
                    (r.stdout or '') + '\n' + (r.stderr or ''))
                fired_all = fr.collect_fired_oracles(
                    r.harness_path, r.class_name,
                    selection.patch_path, selection.buggy_dir)
                if single and single not in fired_all:
                    fired_all.append(single)
                if not fired_all:
                    fired_all = [None]
                # Batch-8 smoke finding: `fired_all` is CAPPED at 200 chars by
                # exception_headlines, and 8.4's Raw keys sit at the END of the
                # message, so the comparison was reading a string its input had
                # already been cut out of. Existence is a property of the
                # producer; ARRIVAL is a property of the journey. This carries
                # the uncapped text to that one mechanical consumer while every
                # other consumer keeps the capped form unchanged.
                _full_headline = dict(
                    getattr(fr, 'last_full_headlines', None) or {})
                if single:
                    _sfull = exception_headline(
                        (r.stdout or '') + '\n' + (r.stderr or ''),
                        max_len=10 ** 9)
                    if _sfull:
                        _full_headline.setdefault(single, _sfull)
                # Concrete evidence of the ORIGINAL firing (exception line
                # + stack). Passed only when judging the oracle it belongs
                # to — evidence from firing A must not colour the judgment
                # of oracle B.
                excerpt = crash_excerpt(
                    (r.stdout or '') + '\n' + (r.stderr or ''))
                # The attribution check's differential outcome is hard
                # evidence about the original firing (does the exact input
                # reproduce on buggy?) — ride it along with the excerpt so
                # it reaches the critic under the same per-oracle gating.
                _attr_note = attribution_notes.get(id(r))
                if _attr_note:
                    excerpt = (excerpt + "\n[differential replay] "
                               + _attr_note).strip()
                # KEEP the finding if ANY fired oracle is sound or trusted —
                # one sound firing is sufficient proof the patch is wrong.
                # DROP only if every fired oracle is judged unsound.
                kept_reason = None
                drop_reasons = []
                _trigger_method_names = {
                    getattr(ft, 'test_method', '') for ft in failure_tests
                    if getattr(ft, 'test_method', '')}
                # Two-judge split: the attribution judge sees ONLY the
                # computed fact notes plus this bug summary — no code.
                _bug_summary = ''
                if failure_tests:
                    _ft0 = failure_tests[0]
                    _bug_summary = (
                        "failing test "
                        + (getattr(_ft0, 'test_method', '') or '?')
                        + " — on the buggy build it fails with: "
                        + ((getattr(_ft0, 'failure_message', '') or
                            getattr(_ft0, 'exception_type', '') or '?')
                           )[:400])
                    _alines = [
                        _l.strip() for _l in
                        (getattr(_ft0, 'method_source', '') or
                         '').splitlines()
                        if 'assert' in _l][:6]
                    if _alines:
                        _bug_summary += (
                            "\nThe failing test's own assertions (the "
                            "pinned observables):\n  "
                            + "\n  ".join(_alines))
                    try:
                        from java.parsing.java_source import (
                            oracle_ids_in_text as _oidsX)
                        _def_id = {e.split('.')[-1]
                                   for e in (expected_exceptions or [])
                                   if e}
                        _trig_oids = set()
                        for _d in (getattr(result,
                                           'accepted_trigger_details',
                                           []) or []):
                            _trig_oids |= _oidsX(_d or '')
                        for _hmap in (buggy_crash_types or {}).values():
                            for _oid in _trig_oids:
                                _def_id |= (_hmap.get(_oid) or set())
                        if _def_id:
                            _bug_summary += (
                                "\nUnderlying exception identity of the "
                                "defect (types recorded beneath the "
                                "trigger-firing checks on the buggy "
                                "build): "
                                + ", ".join(sorted(_def_id)[:6]))
                    except Exception:
                        pass
                # P3.3: underlying exception types per oracle for the
                # ORIGINAL patched-side firing output.
                from java.execution.fuzz_runner import per_oracle_crash_types as _poct
                _patched_types = _poct(
                    (r.stdout or '') + '\n' + (r.stderr or ''))
                _buggy_types = buggy_crash_types.get(r.harness_path) or {}
                # Spec J: the failing test's own seed literals (numeric +
                # string), for the trigger-input exemption rungs below —
                # computed ONCE per leg, fail-open to an empty list.
                try:
                    from java.parsing.java_source import (
                        trigger_seed_literals as _tsl_j)
                    _trig_lits_local = _tsl_j(
                        [getattr(ft, 'method_source', '') or ''
                         for ft in failure_tests])
                except Exception:
                    _trig_lits_local = []
                # Spec J.2a "real failing test passes on the patched build"
                # signal: a PIPELINE INVARIANT here, not a per-firing fact.
                # The P0.1 patched-half safety net reran the REAL failing
                # test(s) on this patched build and they PASS — otherwise the
                # run exited bad_patch (sys.exit(5)) before fuzzing ever
                # reached this firing (same source the buggy-replay notes cite,
                # run.py ~2593 / ~2810). On the semantic path we are on, it is
                # therefore established true.
                _real_test_passes = (bug_kind != 'crashing')
                for fired in fired_all:
                    evid = (excerpt if excerpt and fired
                            and fired[:40] in excerpt else None)
                    _fact_notes = []
                    # Cycle-5C: family-duty result if the identical ladder
                    # below consults it (True=YES, False=NO, None=not asked),
                    # so the terminal gate never double-asks.
                    _fd_consult_result = None
                    from java.parsing.java_source import oracle_ids_in_text as _oids
                    from java.relations.evidence_facts import (
                        semantic_buggy_replay_note, trigger_lift_note,
                        fired_value_vs_trusted)
                    _latent_here = (latent_map.get(r.harness_path) or set())
                    _fired_ids = _oids(fired or '')
                    # P0.4 step 2, REVISED after minfix_w1 and again after
                    # batch3: never dismiss on latency alone, and never
                    # let the judge GUESS what the buggy build does. For
                    # EVERY firing with a persisted input — latent,
                    # symmetric or plain (batch3's Lang-27-o dismissals
                    # cited "already occurs on the buggy build too" with
                    # no fact computed) — replay the exact input on the
                    # BUGGY build once and derive the attribution fact
                    # from what actually happens there.
                    # ESCAPED firings (an exception with no oracle id)
                    # were the one shape with no computed fact at all —
                    # batch4 Lang-27-c kept an escaped exception judged
                    # bare. Extract its type so the same replay serves it.
                    _esc_type = None
                    if fired and not _fired_ids:
                        _m_esc = re.match(
                            r'\s*([\w.$]+(?:Exception|Error))\b', fired)
                        if _m_esc:
                            _esc_type = _m_esc.group(1).rsplit('.', 1)[-1]
                    _breplay_ids, _breplay_out = None, ''
                    # Cycle-6: _breplay_diverted is True/False/None — whether
                    # one of the harness's OWN `catch (...) { return; }`
                    # swallows fired on this input, i.e. whether the buggy run
                    # even reached the check. Without it a swallowed exception
                    # reads as "ran clean" and manufactures the
                    # patch-INTRODUCED-it claim against a correct patch
                    # (night20b Chart-26). None = unknown, which the note
                    # builders treat as conservatively as an errored replay.
                    _breplay_diverted = None
                    if ((_fired_ids or _esc_type)
                            and getattr(r, 'artifact_path', None)):
                        if buggy_cp is None:
                            buggy_cp = builder.test_classpath(
                                selection.buggy_dir)
                        (_breplay_ids,
                         _breplay_out,
                         _breplay_diverted) = fr.replay_input_report(
                            r.harness_path, r.class_name, buggy_cp,
                            r.artifact_path,
                            builder=builder,
                            buggy_dir=selection.buggy_dir)
                    from java.execution.fuzz_runner import (
                        exception_types_in_output as _etio)
                    _bt_all = (_etio(_breplay_out)
                               if _breplay_ids is not None else set())
                    _bt_defect = _bt_all & {
                        e.split('.')[-1]
                        for e in (expected_exceptions or []) if e}
                    # Universal mechanical dismissal for CRASHING bugs —
                    # no longer limited to scan-symmetric checks
                    # (symmetry-in-scan only proved the check fired at
                    # SOME input; the replay proves it at THIS one). Drop
                    # requires every leg computed: the replay succeeded
                    # (errored replay = ABSTAIN), the SAME check fires on
                    # the buggy build at the exact input, and no defect
                    # exception appears anywhere in that run's output
                    # (headlines, cause chains, fenced rethrows). Outside
                    # the reported crash the buggy build is a
                    # reference-correct implementation, so a property it
                    # violates on a completed run is not a real
                    # requirement.
                    _esc_same_on_buggy = bool(
                        _esc_type and _breplay_ids is not None
                        and _esc_type in _bt_all)
                    if (bug_kind == 'crashing' and expected_exceptions
                            and _breplay_ids is not None
                            and not _bt_defect
                            and ((_fired_ids & _breplay_ids)
                                 or _esc_same_on_buggy)):
                        _same_what = (
                            "fires this SAME check there"
                            if (_fired_ids & _breplay_ids)
                            else ("raises this SAME exception type ("
                                  + (_esc_type or '?') + ") there"))
                        _why = (
                            "DEFECT-FAMILY-DISMISSED (mechanical): "
                            "crashing bug; the exact firing input, "
                            "replayed on the buggy build, "
                            + _same_what
                            + " and shows no trace of the "
                            "reported defect exception anywhere in that "
                            "run (headline, cause chain, or fenced "
                            "rethrow; observed: "
                            + (", ".join(sorted(_bt_all)[:4])
                               or 'no exception types')
                            + "). Outside the reported crash the buggy "
                            "build is a reference-correct "
                            "implementation, so a property it violates "
                            "on a completed run is not a real "
                            "requirement — the firing measures "
                            "pre-existing surface, not the patch.")
                        print(f"      [defect-family] auto-dismissed: "
                              f"{(fired or '')[:90]}")
                        drop_reasons.append((fired, _why))
                        continue
                    # One attribution fact, appended to whichever branch
                    # note applies below (latent / symmetric / plain).
                    _breplay_note = None
                    if (_fired_ids or _esc_type) and getattr(
                            r, 'artifact_path', None):
                        # Spec B/C: map the replay outcome to a status the
                        # pure note builder understands. _breplay_ids is None
                        # when the replay never ran / errored (unavailable);
                        # otherwise it ran — crashed if any oracle fired or
                        # any exception surfaced, else clean.
                        _breplay_status = (
                            "unavailable" if _breplay_ids is None
                            else ("crashed" if (_breplay_ids or _bt_all)
                                  else "clean"))
                        # Crash-identity fragment for the same-check /
                        # no-defect branch (batch5 Chart-26-c): NAME the
                        # exception identity on both sides. It needs the raw
                        # replay output and the patched per-oracle types, so
                        # it is assembled here and passed in as data.
                        _idline = ''
                        if (_fired_ids
                                and (_fired_ids & (_breplay_ids or set()))
                                and not _bt_defect):
                            try:
                                from java.execution.fuzz_runner import (
                                    crash_signature as _csig,
                                    cause_signature as _causesig)
                                _s1 = _csig(_breplay_out) or ''
                                _s2 = _causesig(_breplay_out) or ''
                                _ptt = set()
                                for _oid in _fired_ids:
                                    _ptt |= (_patched_types.get(_oid)
                                             or set())
                                if _s1 or _s2 or _ptt:
                                    _idline = (
                                        " Underlying exception "
                                        "identity — on the buggy "
                                        "replay: "
                                        + (_s1 or 'no crash')
                                        + (("; root cause: " + _s2)
                                           if _s2 else '')
                                        + "; under the patched "
                                        "firing: "
                                        + (", ".join(sorted(_ptt))
                                           or 'none recorded')
                                        + ". An identity different "
                                        "from the reported bug's own "
                                        "failure is a different, "
                                        "pre-existing problem.")
                            except Exception:
                                pass
                        # Spec I: when the SAME check fires on both builds,
                        # firing-on-both != identical VALUES. Extract the buggy
                        # replay's fired message for the same oracle id and
                        # compare observed numerics; extraction failure =>
                        # "unknown" (the note then makes no identical claim).
                        _value_verdict = "unknown"
                        _buggy_excerpt = None
                        _patched_excerpt = None
                        if (_fired_ids
                                and (_fired_ids & (_breplay_ids or set()))
                                and not _bt_defect):
                            try:
                                from java.relations.evidence_facts import (
                                    compare_fired_values as _cfv)
                                _bmsg = None
                                for _oid in sorted(
                                        _fired_ids & (_breplay_ids or set())):
                                    _bmsg = _extract_oracle_msg(
                                        _breplay_out, _oid)
                                    if _bmsg:
                                        break
                                if _bmsg:
                                    _value_verdict = _cfv(fired, _bmsg)
                                    _buggy_excerpt = _bmsg
                                    _patched_excerpt = fired
                            except Exception:
                                _value_verdict = "unknown"
                        # 8.3: RECORD the observed values on both sides. They
                        # were computed above and then dropped -- 0 of 1,452
                        # recorded buggy-side steps carried a value, only
                        # fired/counts, which is what made 8.2 untestable and
                        # what forces 6C's values-not-compared abstentions.
                        # Recording only; no verdict reads this yet.
                        # Silent-on-buggy stays VALUELESS by construction (the
                        # values only exist when a check fires there), and that
                        # fails safe: arbitration abstains rather than
                        # inventing a comparison.
                        try:
                            from java.relations.evidence_facts import (
                                observed_values as _ov)
                            _bvals = _ov(_buggy_excerpt)
                            _pvals = _ov(_patched_excerpt or fired)
                            record_event(
                                'deterministic',
                                method='buggy-side-observed-values',
                                target=str(sorted(
                                    _fired_ids & (_breplay_ids or set()))
                                    or sorted(_fired_ids or set()))[:200],
                                output=('recorded '
                                        f'{len(_bvals)} buggy / '
                                        f'{len(_pvals)} patched key(s); '
                                        f'value-verdict={_value_verdict}'),
                                detail={
                                    'buggy_values': _bvals,
                                    'patched_values': _pvals,
                                    'value_verdict': _value_verdict,
                                    'buggy_msg_present': bool(_buggy_excerpt),
                                    'buggy_replay_status': _breplay_status,
                                })
                        except Exception:
                            pass          # recording must never break a run
                        _breplay_note = semantic_buggy_replay_note(
                            _fired_ids, _breplay_status, _breplay_ids,
                            _bt_all, _bt_defect, _esc_type, _idline,
                            value_verdict=_value_verdict,
                            buggy_msg_excerpt=_buggy_excerpt,
                            patched_msg_excerpt=_patched_excerpt,
                            diverted=_breplay_diverted)
                        if _fired_ids:
                            print(f"      [buggy-replay] fact attached "
                                  f"(same-check={bool(_fired_ids & (_breplay_ids or set()))}, "
                                  f"defect={bool(_bt_defect)})")
                        else:
                            print(f"      [buggy-replay] escaped-firing fact "
                                  f"attached ({_esc_type})")
                        # Spec J.2 (cycle-3): re-armed mechanical identical-drop
                        # with the trigger-input exemption. Ladder, in order;
                        # every helper fails open (an error skips that rung,
                        # never dismisses).
                        if (_value_verdict == "identical"
                                and bug_kind != 'crashing'):
                            from java.relations.evidence_facts import (
                                expected_is_test_literal as _eitl,
                                fired_at_test_input as _fati)
                            # (a) the check re-asserts the test's OWN pinned
                            # expectation and the real test passes -> the copy's
                            # setup diverged from the test; mechanical dismiss.
                            _setup_div = False
                            try:
                                _setup_div = (
                                    _real_test_passes
                                    and _eitl(fired, trusted_values))
                            except Exception:
                                _setup_div = False
                            # (b) identical AT the failing test's own inputs on
                            # an observable the test does NOT pin -> the root
                            # defect survived the patch; keep-leaning fact.
                            _at_input = False
                            if not _setup_div:
                                try:
                                    _at_input = _fati(
                                        fired, _trig_lits_local)
                                except Exception:
                                    _at_input = False
                            if _setup_div:
                                _why = (
                                    "IDENTICAL-DISMISSED (setup-divergence): "
                                    "the check re-asserts the failing test's "
                                    "OWN pinned expectation, the REAL failing "
                                    "test passes on this patched build, and "
                                    "this copy fires identically on both "
                                    "builds at this input — its reconstruction "
                                    "of the test's scenario diverged from the "
                                    "test's setup, so the firing measures that "
                                    "divergence, not the patch.")
                                print(f"      [identical-drop] "
                                      f"setup-divergence: "
                                      f"{(fired or '')[:80]}")
                                _fact_notes.append("[defused-drop fact] " + _why + " (KILL-SWITCH: attached as evidence, not dropped)")
                            elif _at_input:
                                _fact_notes.append(
                                    "[patch-failed-to-fix fact] identical "
                                    "behaviour on both builds AT the failing "
                                    "test's own inputs, on an observable the "
                                    "test does not pin — the root defect "
                                    "survived the patch; this firing is "
                                    "evidence AGAINST the patch.")
                                # Cycle-5C consistency: this exemption KEEPS the
                                # firing (patch-failed-to-fix). Record it as a
                                # family-duty YES so the downstream terminal
                                # gate honours the keep instead of re-asking
                                # family_duty and dropping it — the exact Math-2
                                # kill-switch regression (2026-07-25).
                                _fd_consult_result = True
                                print(f"      [patch-failed-to-fix] "
                                      f"at-test-input: {(fired or '')[:80]}")
                            else:
                                _fd_ok, _fd_why = rv.family_duty(
                                    fired,
                                    _j3_failing_test_block(failure_tests), src)
                                _fd_consult_result = _fd_ok
                                if not _fd_ok:
                                    _why = (
                                        "IDENTICAL-DISMISSED (family-duty): "
                                        "kv-certified identical observed "
                                        "behaviour on both builds at this "
                                        "exact firing input; a focused review "
                                        "found the violated property is NOT "
                                        "the failing test's own observable, "
                                        "and the firing does not lie at the "
                                        "test's own inputs — pre-existing "
                                        "surface. " + _fd_why)
                                    # KILL-SWITCH (poolA 2026-07-25): catch
                                    # legs ended missed with DUTY:NO events —
                                    # defused to a fact per the standing rule.
                                    print(f"      [identical-drop] family-duty"
                                          f" DEFUSED: {(fired or '')[:80]}")
                                    _fact_notes.append(
                                        "[defused-drop fact] " + _why)
                                _fact_notes.append(
                                    "[family-duty fact] kv-certified "
                                    "identical observed behaviour on both "
                                    "builds at this exact firing input, and a "
                                    "focused review found the violated "
                                    "property IS this family's duty (the "
                                    "patch-failed-to-fix pattern) — " + _fd_why)
                                print(f"      [family-duty] YES — fact "
                                      f"attached: {(fired or '')[:80]}")
                        # Spec G-G3.1: when a DIFFERENT check fired FIRST on the
                        # buggy replay (shadowed — _breplay_ids non-empty,
                        # disjoint from _fired_ids, no defect exception) the
                        # per-input question "does THIS check fire on buggy?"
                        # is unanswered and semantic_buggy_replay_note returned
                        # the honest UNKNOWN wording. Silence the shadowing
                        # check(s) and replay this exact input to compute the
                        # missing fact, then append it.
                        #
                        # Cycle-6 item 4 PART A: when the muted run crashes at
                        # yet ANOTHER sibling alarm the target still never got
                        # to speak — that sibling is a NEW shadow. Iterate:
                        # add it to the mute set and replay again, bounded to
                        # MAX_EXTRA_MUTED_PASSES passes beyond the first and
                        # stopping early when the mute set stops growing or a
                        # pass errors (each pass costs a Jazzer run; crashing
                        # legs get the single pass they had). Any failure
                        # leaves the cycle-1 UNKNOWN note intact.
                        _shadowed = bool(
                            _fired_ids and (_breplay_ids or set())
                            and not (_fired_ids & (_breplay_ids or set()))
                            and not _bt_defect)
                        # The muted ladder's own value verdict, hoisted so the
                        # shadow-isolation reading below can see whether the
                        # muted re-replay already resolved the value question.
                        _mvv_seen = "unknown"
                        if _shadowed:
                            try:
                                from java.execution.fuzz_runner import (
                                    exception_types_in_output as _etio2,
                                    iterate_muted_replay as _imr)

                                def _muted_pass(_mute_ids, _pass_no,
                                                _r=r, _cp=buggy_cp):
                                    return fr.replay_input_muted(
                                        _r.harness_path, _r.class_name, _cp,
                                        _r.artifact_path,
                                        mute_ids=set(_mute_ids),
                                        builder=builder,
                                        buggy_dir=selection.buggy_dir,
                                        variant_tag=str(_pass_no - 1))

                                (_ms, _mf, _mo, _mdiv,
                                 _mmuted, _mpasses) = _imr(
                                    _muted_pass, _fired_ids, _breplay_ids,
                                    esc_type=_esc_type,
                                    max_extra_passes=(
                                        0 if bug_kind == 'crashing' else 3))
                                _mbt = _etio2(_mo) if _mo else set()
                                print(f"      [muted-replay] status={_ms} "
                                      f"fired={sorted(_mf or set())} "
                                      f"diverted={_mdiv} "
                                      f"passes={_mpasses} "
                                      f"muted={sorted(_mmuted)}")
                                from java.relations.evidence_facts import (
                                    muted_replay_note as _mrn)
                                # Spec I: compare the target check's observed
                                # value on the muted buggy replay against the
                                # patched firing before any identical claim;
                                # extraction failure => "unknown".
                                _mvv = "unknown"
                                _mbe = None
                                _mpe = None
                                try:
                                    from java.relations.evidence_facts import (
                                        compare_fired_values as _cfv2)
                                    _tmsg = None
                                    for _oid in sorted(
                                            _fired_ids & (_mf or set())):
                                        _tmsg = _extract_oracle_msg(_mo, _oid)
                                        if _tmsg:
                                            break
                                    if _tmsg:
                                        _mvv = _cfv2(fired, _tmsg)
                                        _mbe = _tmsg
                                        _mpe = fired
                                except Exception:
                                    _mvv = "unknown"
                                _mvv_seen = _mvv
                                # 8.3 (mirror): the MUTED buggy-side replay is
                                # the other path that computes observed values
                                # and dropped them. Recording on only one of
                                # the two would leave the value channel
                                # half-armed -- populated on plain replays,
                                # silently empty on muted ones -- and a
                                # consumer reading an empty channel cannot tell
                                # "this path records nothing" from "this input
                                # had no values". Same fail-open wrapping.
                                try:
                                    from java.relations.evidence_facts import (
                                        observed_values as _ov2)
                                    _mbvals = _ov2(_mbe)
                                    _mpvals = _ov2(_mpe or fired)
                                    record_event(
                                        'deterministic',
                                        method='buggy-side-observed-values',
                                        target=('muted:' + str(sorted(
                                            _fired_ids & (_mf or set())))[:180]),
                                        output=('recorded '
                                                f'{len(_mbvals)} buggy / '
                                                f'{len(_mpvals)} patched '
                                                f'key(s); '
                                                f'value-verdict={_mvv}'),
                                        detail={
                                            'buggy_values': _mbvals,
                                            'patched_values': _mpvals,
                                            'value_verdict': _mvv,
                                            'buggy_msg_present': bool(_mbe),
                                            'buggy_replay_status': _ms,
                                            'replay_kind': 'muted',
                                            'muted_ids': sorted(_mmuted or []),
                                        })
                                except Exception:
                                    pass      # recording never breaks a run
                                # Name the FINAL mute set (PART A may have
                                # grown it across passes); the note's semantics
                                # are unchanged — it just lists every check
                                # that had to be silenced.
                                _mnote = _mrn(_fired_ids, _mmuted, _ms,
                                              _mf, _esc_type, _mbt,
                                              value_verdict=_mvv,
                                              buggy_msg_excerpt=_mbe,
                                              patched_msg_excerpt=_mpe,
                                              diverted=_mdiv)
                                if _mnote:
                                    _breplay_note = (
                                        (_breplay_note or '') + " " + _mnote)
                                # Spec J.2 (cycle-3, muted-replay site): when
                                # the muted re-replay EARNED "identical" (target
                                # check fires on the buggy build with matching
                                # observed values once the shadowing check is
                                # silenced), run the SAME re-armed ladder with
                                # the trigger-input exemption. Semantic legs
                                # only; every helper fails open.
                                if (_mvv == "identical"
                                        and bug_kind != 'crashing'):
                                    from java.relations.evidence_facts import (
                                        expected_is_test_literal as _eitl,
                                        fired_at_test_input as _fati)
                                    _setup_div = False
                                    try:
                                        _setup_div = (
                                            _real_test_passes
                                            and _eitl(fired, trusted_values))
                                    except Exception:
                                        _setup_div = False
                                    _at_input = False
                                    if not _setup_div:
                                        try:
                                            _at_input = _fati(
                                                fired, _trig_lits_local)
                                        except Exception:
                                            _at_input = False
                                    if _setup_div:
                                        _why = (
                                            "IDENTICAL-DISMISSED "
                                            "(setup-divergence): the check "
                                            "re-asserts the failing test's OWN "
                                            "pinned expectation, the REAL "
                                            "failing test passes on this "
                                            "patched build, and (muted replay) "
                                            "it fires identically on both "
                                            "builds at this input — its "
                                            "reconstruction of the test's "
                                            "scenario diverged from the test's "
                                            "setup, not the patch.")
                                        print(f"      [identical-drop] "
                                              f"setup-divergence (muted): "
                                              f"{(fired or '')[:80]}")
                                        _fact_notes.append("[defused-drop fact] " + _why + " (KILL-SWITCH: attached as evidence, not dropped)")
                                    elif _at_input:
                                        _fact_notes.append(
                                            "[patch-failed-to-fix fact] "
                                            "identical behaviour on both "
                                            "builds (muted replay) AT the "
                                            "failing test's own inputs, on an "
                                            "observable the test does not pin "
                                            "— the root defect survived the "
                                            "patch; this firing is evidence "
                                            "AGAINST the patch.")
                                        # Cycle-5C consistency (see same-check
                                        # site): exemption keeps -> record
                                        # family-duty YES so the terminal gate
                                        # honours it, not the Math-2 regression.
                                        _fd_consult_result = True
                                        print(f"      [patch-failed-to-fix] "
                                              f"at-test-input (muted): "
                                              f"{(fired or '')[:80]}")
                                    else:
                                        _fd_ok, _fd_why = rv.family_duty(
                                            fired,
                                            _j3_failing_test_block(
                                                failure_tests),
                                            src)
                                        _fd_consult_result = _fd_ok
                                        if not _fd_ok:
                                            _why = (
                                                "IDENTICAL-DISMISSED "
                                                "(family-duty): kv-certified "
                                                "identical observed behaviour "
                                                "on both builds (muted replay) "
                                                "at this exact input; a "
                                                "focused review found the "
                                                "violated property is NOT the "
                                                "failing test's own "
                                                "observable, and the firing "
                                                "does not lie at the test's "
                                                "own inputs — pre-existing "
                                                "surface. " + _fd_why)
                                            # KILL-SWITCH: defused (see the
                                            # same-check site).
                                            print(f"      [identical-drop] "
                                                  f"family-duty (muted) "
                                                  f"DEFUSED: "
                                                  f"{(fired or '')[:80]}")
                                            _fact_notes.append(
                                                "[defused-drop fact] "
                                                + _why)
                                        _fact_notes.append(
                                            "[family-duty fact] kv-certified "
                                            "identical observed behaviour on "
                                            "both builds (muted replay), and a "
                                            "focused review found the violated "
                                            "property IS this family's duty "
                                            "(the patch-failed-to-fix pattern) "
                                            "— " + _fd_why)
                                        print(f"      [family-duty] YES — "
                                              f"fact attached (muted): "
                                              f"{(fired or '')[:80]}")
                            except Exception as _mexc:
                                print(f"      [muted-replay] unavailable "
                                      f"({_mexc}) — UNKNOWN note kept")
                        # SHADOW-ISOLATION READING. Pre-registered in
                        # docs/math65-formula-read-2026-08-10.md; built
                        # because six consecutive Math-65 legs convicted a
                        # correct patch on relations whose buggy-side value
                        # was never read.
                        #
                        # Arm it when the value verdict is STILL unknown after
                        # the plain and muted replays AND the full-harness
                        # buggy replay did not run clean. That pair is the
                        # mechanical signature of a PREVENTED reading: some
                        # check ended that run (the muted ladder is allowed to
                        # give up the moment its mute set stops growing) and
                        # this check's own message was never printed. A CLEAN
                        # buggy replay is excluded deliberately — there the
                        # check demonstrably did not fire on the buggy build,
                        # which is the catch signal, not a shadowed reading.
                        #
                        # The isolated variant silences every sibling alarm in
                        # ONE shot and keeps only this check's own throw (as a
                        # printed message, not a fatal one), so nothing left in
                        # the harness can speak before it. Then two numbers
                        # decide; anything they cannot decide leaves the
                        # verdict, the notes and the evidence exactly as today.
                        _iso_vv = (_mvv_seen if _mvv_seen != "unknown"
                                   else _value_verdict)
                        # Isolated buggy-side replays already taken for this
                        # firing, by target id — the valid-by-construction
                        # probe below reuses them instead of paying a second
                        # Jazzer run for the same measurement.
                        _iso_results = {}
                        if (_fired_ids and _iso_vv == "unknown"
                                and _breplay_status != "clean"):
                            from java.relations.evidence_facts import (
                                isolated_value_reading as _ivr,
                                isolation_dismisses as _idis,
                                isolation_reading_fact as _irf)
                            # BUILD B (pre-registration round 2): isolate EVERY
                            # relation this firing names, one Jazzer run each.
                            # The first version isolated `sorted(_fired_ids)[0]`
                            # only, on the reasoning that a firing names one
                            # check; gs1 leg 03 disproved it — the leg's two
                            # KEPT convictions carried no isolation event at
                            # all, because the single target chosen by name was
                            # a different check. A reading that never looks at
                            # the convicting relation cannot reach it.
                            _iso_drop = None
                            for _iso_target in sorted(_fired_ids):
                                _iso_status, _iso_msg = "isolate_failed", None
                                try:
                                    (_iso_status, _iso_msg,
                                     _iso_out) = fr.replay_input_isolated(
                                        r.harness_path, r.class_name, buggy_cp,
                                        r.artifact_path, _iso_target,
                                        builder=builder,
                                        buggy_dir=selection.buggy_dir)
                                except Exception as _iexc:
                                    print(f"      [isolated-replay] "
                                          f"unavailable ({_iexc}) — "
                                          f"UNKNOWN kept")
                                _iso_results[_iso_target] = (_iso_status,
                                                             _iso_msg)
                                _iso_read = _ivr(fired, _iso_msg)
                                print(f"      [isolated-replay] "
                                      f"target={_iso_target} "
                                      f"status={_iso_status} "
                                      f"reading={_iso_read['reading']}")
                                record_event(
                                    'deterministic',
                                    method='isolated-buggy-replay',
                                    target=_iso_target,
                                    output=(f'status={_iso_status}; '
                                            f'reading={_iso_read["reading"]}'),
                                    reason=_iso_read['detail'],
                                    detail={
                                        'status': _iso_status,
                                        'shadowed': _shadowed,
                                        'buggy_replay_status': _breplay_status,
                                        'buggy_msg': (_iso_msg or '')[:800],
                                        'reading': _iso_read['reading'],
                                        'key': _iso_read['key'],
                                        'expected': _iso_read['expected'],
                                        'patched_value': _iso_read['patched'],
                                        'buggy_value': _iso_read['buggy'],
                                        'targets': sorted(_fired_ids),
                                    })
                                # The fact names the relation it was measured
                                # on, not every id the firing mentions: with
                                # one run per id each reading belongs to one
                                # check.
                                _iso_fact = _irf(_iso_read, {_iso_target})
                                if _iso_fact and _idis(_iso_read):
                                    print(f"      [isolated-replay] "
                                          f"auto-dismissed firing: "
                                          f"{(fired or '')[:90]}")
                                    _iso_drop = _iso_fact
                                    # The firing is going; the remaining ids
                                    # would cost a Jazzer run each for evidence
                                    # nothing will read.
                                    break
                                if _iso_fact:
                                    # A stated direction (the buggy build
                                    # closer to the check's own expected value,
                                    # or an agreement check reading differently
                                    # on the two builds): a fact, dismissing
                                    # nothing. Delivered the way every other
                                    # computed fact is — into the concrete
                                    # evidence the judge reads, not only the
                                    # note list.
                                    _fact_notes.append(_iso_fact)
                                    evid = ((evid + "\n" + _iso_fact) if evid
                                            else _iso_fact)
                            if _iso_drop:
                                drop_reasons.append((fired, _iso_drop))
                                continue
                        # VALID-BY-CONSTRUCTION PROBE. Pre-registered in
                        # docs/reportable-exception-prereg-2026-08-09.md
                        # (2026-08-11 section): a tier-2/unexpected-exception
                        # firing convicts on the premise that its input was
                        # valid, and 8.41's Chart-7-c/Chart-26-c show that
                        # premise can simply be wrong (empty strings,
                        # degenerate ranges the relation wrongly declared
                        # valid). Before such a firing may convict, replay
                        # the SAME crashing input through the SAME check
                        # compiled against the BUGGY build — isolated, the
                        # same channel as the reading above, so no sibling
                        # alarm can shadow the answer. Same exception type
                        # from the check's probe tier there -> the input was
                        # never valid: record [fact:input-invalid-on-both]
                        # and DEMOTE (a judge-visible fact, never a terminal
                        # dismissal). Different/no exception -> a
                        # discriminating fact; the conviction stands. Any
                        # failure to measure -> no fact, everything
                        # unchanged (fail-closed). One deterministic event
                        # per probe.
                        from java.relations.evidence_facts import (
                            tier2_exception_type as _t2x,
                            valid_input_probe_reading as _vpr,
                            valid_input_probe_fact as _vpf,
                            valid_input_probe_demotes as _vpd)
                        if _fired_ids and _t2x(fired):
                            for _vp_target in sorted(_fired_ids):
                                (_vp_status,
                                 _vp_msg) = _iso_results.get(
                                    _vp_target, (None, None))
                                if _vp_status is None:
                                    _vp_status, _vp_msg = (
                                        "isolate_failed", None)
                                    try:
                                        (_vp_status, _vp_msg,
                                         _vp_out) = fr.replay_input_isolated(
                                            r.harness_path, r.class_name,
                                            buggy_cp, r.artifact_path,
                                            _vp_target,
                                            builder=builder,
                                            buggy_dir=selection.buggy_dir,
                                            variant_tag='vp')
                                    except Exception as _vpe:
                                        print(f"      [valid-input-probe] "
                                              f"unavailable ({_vpe}) — "
                                              f"unchanged")
                                _vp_read = _vpr(fired, _vp_status, _vp_msg)
                                print(f"      [valid-input-probe] "
                                      f"target={_vp_target} "
                                      f"status={_vp_status} "
                                      f"reading={_vp_read['reading']}")
                                record_event(
                                    'deterministic',
                                    method='valid-input-probe',
                                    target=_vp_target,
                                    output=(f'status={_vp_status}; '
                                            f'reading='
                                            f'{_vp_read["reading"]}'),
                                    reason=_vp_read['detail'],
                                    detail={
                                        'status': _vp_status,
                                        'reading': _vp_read['reading'],
                                        'patched_type':
                                            _vp_read['patched_type'],
                                        'buggy_type':
                                            _vp_read['buggy_type'],
                                        'buggy_msg': (_vp_msg or '')[:800],
                                    })
                                _vp_fact = _vpf(_vp_read, {_vp_target})
                                if not _vp_fact:
                                    continue
                                if _vpd(_vp_read):
                                    # DEMOTED, not dropped: the fact goes
                                    # into the evidence the judge reads,
                                    # the firing itself stays.
                                    print(f"      [valid-input-probe] "
                                          f"DEMOTED to rejection "
                                          f"(invalid-on-both): "
                                          f"{(fired or '')[:90]}")
                                _fact_notes.append(_vp_fact)
                                evid = ((evid + "\n" + _vp_fact) if evid
                                        else _vp_fact)
                    # The firing INPUT itself, verbatim (batch6 Lang-27-c:
                    # attribution ruled 'duty to fix' on a defect-type
                    # crash without ever seeing that the input was fuzzer
                    # junk — the one datum that decides junk-vs-valid).
                    if _breplay_note and getattr(r, 'artifact_path', None):
                        try:
                            with open(r.artifact_path, 'rb') as _afh:
                                _raw = _afh.read(256)
                            _irepr = repr(_raw)[2:-1][:220]
                            if _irepr:
                                _breplay_note += (
                                    "\nThe exact firing input, raw bytes "
                                    "(capped; the harness may consume them "
                                    "as several values): \"" + _irepr
                                    + "\". Judge input validity from THIS, "
                                    "not from assumptions: well-formed "
                                    "content resembling the API's domain "
                                    "suggests a constructed/valid input; "
                                    "control bytes and arbitrary junk "
                                    "suggest fuzzer noise no "
                                    "implementation is obliged to accept.")
                        except OSError:
                            pass
                    _latent_note = None
                    if _fired_ids and _fired_ids <= _latent_here:
                        _latent_note = (
                            "[latent oracle] check(s) "
                            + ", ".join(sorted(_fired_ids))
                            + " never fired during the buggy-side "
                            "acceptance scan (that scan stops at the "
                            "first firing oracle per input, so this "
                            "alone proves nothing). "
                            + (_breplay_note
                               or "No persisted input to replay — judge "
                                  "on soundness alone, sceptically."))
                        _breplay_note = None
                    # P3.3 crash-site pinning (mechanical): the same alarm
                    # fired on both builds, but from DISJOINT underlying
                    # exception types — a different (pre-existing) crash
                    # wearing the alarm the buggy-side crash earned
                    # (Chart-26-c: the axis-label NPE on buggy vs the
                    # unrelated text-measuring crash on patched). Compared
                    # at TYPE level only, so a half-fix that moves the same
                    # exception to a nearby frame stays a catch. Applies
                    # only when BOTH sides carry identity — a value-mismatch
                    # alarm has no underlying exception and is untouched.
                    _pin_mismatch = None
                    for _oid in _fired_ids:
                        _bt = _buggy_types.get(_oid) or set()
                        _pt = _patched_types.get(_oid) or set()
                        if _bt and _pt and not (_bt & _pt):
                            _pin_mismatch = (_oid, _bt, _pt)
                            break
                    if _pin_mismatch:
                        _oid, _bt, _pt = _pin_mismatch
                        _why = ("CRASH-PIN-DISMISSED (mechanical): oracle "
                                f"{_oid} fired on buggy from "
                                f"{sorted(_bt)} but on patched from "
                                f"{sorted(_pt)} — disjoint underlying "
                                "exception types mean this is a different "
                                "crash than the one the check pinned on "
                                "buggy, not the bug surviving the patch.")
                        print(f"      [crash-pin] auto-dismissed firing: "
                              f"{(fired or '')[:100]}")
                        print(f"        buggy={sorted(_bt)} "
                              f"patched={sorted(_pt)}")
                        drop_reasons.append((fired, _why))
                        continue
                    # Escape-shaped firing on a SEMANTIC leg: no oracle ID
                    # and not our alarm type — this exception ESCAPED the
                    # library, it is not one of the harness's checks. The
                    # common case is constructor/argument validation on a
                    # junk fuzzed input (minfix_w2b/w2c Math-2-c: a
                    # NotPositiveException from consumeInt junk was kept
                    # TWICE as a conviction of the correct patch). The
                    # differential-replay path only sees the run's FIRST
                    # firing, so escapes surfaced by the keep-going re-fuzz
                    # need the fact stated here.
                    if (bug_kind == 'semantic' and fired
                            and not _fired_ids
                            and 'FuzzerSecurityIssue' not in fired):
                        _note = ("[escaped exception] this firing is NOT "
                                 "one of the harness's own checks — it is "
                                 "an exception that escaped the library "
                                 "(no oracle ID). On a semantic bug the "
                                 "oracle is an alarm throw; an escaped "
                                 "exception is almost always input "
                                 "rejection (constructor/argument "
                                 "validation of a junk fuzzed value — "
                                 "check the harness's input construction "
                                 "for ranges that can go negative or "
                                 "overflow) or pre-existing crash surface. "
                                 "Judge it UNSOUND unless the patch itself "
                                 "demonstrably introduces this exception "
                                 "on a VALID input.")
                        _fact_notes.append(_note)
                        evid = (evid + "\n" + _note) if evid else _note
                    if _latent_note:
                        _fact_notes.append(_latent_note)
                        evid = ((evid + "\n" + _latent_note)
                                if evid else _latent_note)
                    elif (_fired_ids and r.harness_path in latent_map
                          and _fired_ids <= (_oids(src) - _latent_here)):
                        # SYMMETRIC firing: this check also fired on the
                        # buggy build during acceptance, so the patch did
                        # not change the violated behaviour. That is the
                        # classic overfit-catch pattern ONLY when the
                        # violated contract belongs to the reported bug's
                        # own behaviour family (Lang-41: the sibling
                        # String-variant of the very method the test
                        # fails); when it concerns an unrelated feature or
                        # a setup/guard-dependent observable, it is
                        # pre-existing surface that fires on ANY build
                        # (Chart-26-c: an axis-entity check with no
                        # relation to the null-info crash the bug is
                        # about). The verifier owns that judgment — say so.
                        _note = ("[symmetric firing] check(s) "
                                 + ", ".join(sorted(_fired_ids))
                                 + " ALSO fired on the buggy build during "
                                 "the acceptance scan — the patch did NOT "
                                 "change this behaviour. Keep this finding "
                                 "only if the violated contract is the "
                                 "very behaviour the failing test shows "
                                 "is wrong (the same observable, not "
                                 "merely the same method or class — a "
                                 "method has many independent behaviours "
                                 "and sharing one proves nothing about "
                                 "the patch); if it concerns a different "
                                 "observable or a setup-dependent one "
                                 "that would fire on any build, it is "
                                 "pre-existing surface — dismiss it.")
                        # The computed attribution fact (buggy replay
                        # of the exact firing input) is built once above
                        # for every firing; it carries the SYM-2/SYM-2b
                        # content that used to live here, and the
                        # unambiguous crashing case has already been
                        # mechanically dropped before this branch.
                        if _breplay_note:
                            _note += "\n" + _breplay_note
                            _breplay_note = None
                        _fact_notes.append(_note)
                        evid = (evid + "\n" + _note) if evid else _note
                    elif _fired_ids & _latent_here:
                        _note = ("[latent oracle] check(s) "
                                 + ", ".join(sorted(_fired_ids
                                                    & _latent_here))
                                 + " NEVER fired on the buggy build at "
                                 "acceptance — this patched-build firing "
                                 "is their first-ever execution; there is "
                                 "no buggy-side evidence behind them.")
                        if _breplay_note:
                            _note += "\n" + _breplay_note
                            _breplay_note = None
                        _fact_notes.append(_note)
                        evid = (evid + "\n" + _note) if evid else _note
                    elif _breplay_note:
                        # PLAIN firing — neither latent nor symmetric.
                        # batch3 (Lang-27-o): these reached the judge with
                        # no buggy-side fact at all, and the dismissals
                        # invented one ("already occurs on the buggy build
                        # too"). Attach the computed fact instead.
                        _fact_notes.append(_breplay_note)
                        evid = ((evid + "\n" + _breplay_note)
                                if evid else _breplay_note)
                        _breplay_note = None
                    # W1.5 (p23gate FP fix): when the fired oracle is a lift
                    # of a trigger test, tell the verifier the decisive fact
                    # it otherwise never learns — the REAL trigger test was
                    # rerun on this patched build and PASSES (the pipeline
                    # exits with bad_patch before fuzzing otherwise). If the
                    # harness replays the test's own scenario, a firing here
                    # means the harness's reconstruction diverges from the
                    # real test's setup (missing source/locale/format
                    # wiring), not that the patch is wrong. A lift firing on
                    # OTHER inputs than the test's own remains a legitimate
                    # generalisation catch.
                    _lifted_of = {t for t in _trigger_method_names
                                  if any(t in fid for fid in _fired_ids)
                                  or (fired and t in fired)}
                    # batch3 Closure-62-c FP: the fact never fired because
                    # the oracle was named the GENERIC 'lifted-test' (the
                    # prompt convention allows it) rather than after the
                    # trigger method, so no _trigger_method_names matched.
                    # A generic lift id is still a lifted test — detect it.
                    _generic_lift = bool(
                        _fired_ids and any(
                            re.search(r'lift|seed[-_]?test', fid, re.I)
                            for fid in _fired_ids))
                    if _lifted_of or _generic_lift:
                        # Spec D: the name regex only DETECTS lift provenance;
                        # dismissal wording is licensed by a mechanical value
                        # comparison, never by the name alone. A divergent
                        # value is the definition of a generalisation catch.
                        # The UNCAPPED headline, so 8.4's trailing Raw keys
                        # arrive. Falls back to `fired` when no full form was
                        # recorded — never worse than before.
                        _value_verdict = fired_value_vs_trusted(
                            _full_headline.get(fired, fired), trusted_values)
                        _note = trigger_lift_note(
                            sorted(_lifted_of), bool(_generic_lift),
                            _value_verdict)
                        if _note:
                            print(f"      [trigger-test lift] "
                                  f"value-vs-trusted={_value_verdict}")
                            _fact_notes.append(_note)
                            evid = (evid + "\n" + _note) if evid else _note
                    # Spec K (cycle-3): one-door fact parity. A harness-track
                    # firing of the SAME underlying check the replay track
                    # screens must carry the same fire-rate / screen-decision
                    # facts, or the judge convicts here where the replay track
                    # correctly rules pre-existing (Math-73-c: the identical
                    # bogus endpoint-root check, ruled UNSOUND on the replay
                    # track that got the facts, kept on the harness track that
                    # did not). Match the fired oracle to a screened relation
                    # mechanically and, on a match, attach the identical
                    # fire_rate_fact + the relation's screen-decision reason.
                    # K.3: independently, state the buggy-side scan record for
                    # this oracle (data already computed at acceptance; no new
                    # executions). Fail-open: any error attaches nothing.
                    _one_door_matched = False
                    # Cycle-6 item 4 PART B: did a fire-rate fact actually
                    # reach this firing's evidence, and were buggy-side counts
                    # known at all? A match that measured nothing must NOT
                    # suppress the universal screen (Math-65 harness track:
                    # matched=True gated the screen off), and a known rate must
                    # be delivered even when the one-door block attached
                    # nothing else.
                    _rate_fact_attached = False
                    _one_door_patched_counts = (None, None)
                    _one_door_demote = ''
                    # Always-on entry diagnostic (BEFORE the try/except, so a
                    # NameError here can never be swallowed): proves this block
                    # ran and shows the exact gate values, so it can never again
                    # be silently inert.
                    _oid_dbg = sorted(_fired_ids)[0] if _fired_ids else None
                    print(f"      [one-door] considering oracle "
                          f"'{_oid_dbg}' (fired_ids={sorted(_fired_ids)}, "
                          f"relations={len(synthesized_relations or [])})")
                    record_event('deterministic', method='one_door_entry',
                                 target=str(_oid_dbg),
                                 output=f'relations={len(synthesized_relations or [])}')
                    try:
                        _one_door_notes = []
                        if _fired_ids and synthesized_relations:
                            from java.relations.evidence_facts import (
                                match_oracle_to_relation as _motr,
                                fire_rate_fact as _frf_k)
                            _rel_names_k = [
                                getattr(_rel, 'name', '')
                                for _rel in synthesized_relations
                                if getattr(_rel, 'name', '')]
                            _oid_k = sorted(_fired_ids)[0]
                            _match_k = _motr(_oid_k, fired or '', _rel_names_k)
                            _rel_k = None
                            if _match_k:
                                _rel_k = next(
                                    (_rel for _rel in synthesized_relations
                                     if getattr(_rel, 'name', '') == _match_k),
                                    None)
                            _one_door_matched = _rel_k is not None
                            if _rel_k is not None:
                                _sstats_k = (
                                    getattr(_rel_k, 'screen_stats', None)
                                    or (None, None))
                                _sdec_k = (getattr(_rel_k, 'screen_decision',
                                                   None) or {})
                                _sreason_k = _sdec_k.get('reason', '') or ''
                                # Patched replay counts only exist if the replay
                                # pass has already run this leg (it runs LATER
                                # in main, so normally absent here) — pass None,
                                # fire_rate_fact handles it.
                                _pc_k = _pv_k = None
                                _rf_k = locals().get('_replay_findings')
                                if _rf_k:
                                    for _ff in _rf_k:
                                        if _ff.get('name') == _match_k:
                                            _pc_k = _ff.get('patched_checked')
                                            _pv_k = _ff.get('patched_violated')
                                            break
                                _demote_k = (
                                    _sreason_k
                                    if ('above-ratio-cap' in _sreason_k
                                        or 'inverted' in _sreason_k)
                                    else '')
                                _frfact_k = _frf_k(
                                    _sstats_k[0], _sstats_k[1],
                                    _pc_k, _pv_k, _demote_k)
                                if _frfact_k:
                                    _one_door_notes.append(_frfact_k)
                                    _rate_fact_attached = True
                                # PART B: remember the buggy-side counts this
                                # match KNOWS (and the patched counts / demote
                                # reason it used), so the delivery step below
                                # can state a known rate the structure would
                                # otherwise drop.
                                _one_door_patched_counts = (_pc_k, _pv_k)
                                _one_door_demote = _demote_k
                                if _sstats_k[0]:
                                    _buggy_rate_counts.setdefault(
                                        _oid_k, (_sstats_k[0], _sstats_k[1]))
                                if _sreason_k:
                                    _one_door_notes.append(
                                        "[screen-decision fact] this harness "
                                        "firing is the SAME check as screened "
                                        "relation '" + str(_match_k)
                                        + "', whose screening decision was: "
                                        + str(_sreason_k).strip() + ".")
                                if _one_door_notes:
                                    print(f"      [one-door] matched screened "
                                          f"relation '{_match_k}'")
                        # K.3: the acceptance-time buggy keep-going scan's
                        # per-oracle record for this firing (buggy_crash_types
                        # = the oracles that fired on buggy, with the exception
                        # identity beneath each; no counts are recorded, so we
                        # state only what exists).
                        if _fired_ids and _buggy_types:
                            _scan_hits = {
                                _oid: sorted(_buggy_types.get(_oid) or set())
                                for _oid in sorted(_fired_ids)
                                if _oid in _buggy_types}
                            if _scan_hits:
                                _hit_txt = "; ".join(
                                    _oid + " (" + (", ".join(_ty)
                                                   or "no exception type")
                                    + ")" for _oid, _ty in _scan_hits.items())
                                _one_door_notes.append(
                                    # cycle-5 iter-3: the tag carries the fact
                                    # itself, so terminal_profile no longer has
                                    # to recover it from this prose.
                                    "[buggy-scan fact] "
                                    "[fact:fires-on-buggy-scan] "
                                    "the acceptance-time "
                                    "buggy keep-going scan recorded this "
                                    "oracle firing on the BUGGY build: "
                                    + _hit_txt + " — the same check fires on "
                                    "both builds; keep only if the violated "
                                    "observable is the failing test's own.")
                        for _od in _one_door_notes:
                            _fact_notes.append(_od)
                            evid = (evid + "\n" + _od) if evid else _od
                    except Exception:
                        pass
                    # Spec M (cycle-3b), M-v2: universal screening. A fired
                    # oracle with NO one-door relation match reached the judge
                    # with zero measurements — the residual wrong convictions
                    # ride on exactly such fresh harness inventions. Measure it
                    # by INSTRUMENTING the whole (known-compilable) harness:
                    # mute every sibling alarm, replace the target oracle's
                    # throw with a counter, and run the counting harness on the
                    # buggy build (same relscreen -runs budget and stats parse
                    # as the per-relation screen). Deliver the fire-rate fact,
                    # plus the never-held fact when the claim held on ZERO buggy
                    # inputs. Lazy at judging (fired oracles only), cached per
                    # oracle id per leg (repeat firings never re-measure),
                    # capped at 8 measured oracles per leg. Fail-open: any
                    # failure attaches nothing. (v1 extracted a single check
                    # body and it near-never compiled — see cycle3b outcome.)
                    # Always-on entry diagnostic (BEFORE the try/except, so a
                    # NameError here can never be swallowed): proves this block
                    # ran and shows the exact gate values, so it can never again
                    # be silently inert.
                    #
                    # Cycle-6 item 4 PART B: the gate is now "no buggy-side
                    # rate is KNOWN for this oracle", not "no one-door match".
                    # A match whose relation carried no buggy screen counts
                    # used to suppress the screen and deliver nothing (the
                    # Math-65 harness track); a match that DID measure still
                    # costs no extra Jazzer runs, because its counts are known
                    # and are delivered by the step below.
                    _rate_known = (
                        bool(_fired_ids)
                        and sorted(_fired_ids)[0] in _buggy_rate_counts)
                    print(f"      [universal-screen] considering oracle "
                          f"'{_oid_dbg}' matched={_one_door_matched} "
                          f"rate_known={_rate_known} "
                          f"(fired_ids={bool(_fired_ids)}, "
                          f"jazzer={bool(jazzer_standalone_jar)})")
                    record_event('deterministic', method='universal_screen_entry',
                                 target=str(_oid_dbg),
                                 output=(f'matched={_one_door_matched} '
                                         f'rate_known={_rate_known}'))
                    try:
                        # Cycle-6 item 6: the whole decide-and-measure body now
                        # lives in `_universal_screen_step`, which records
                        # `cycle6_universal_screen_decided` on EVERY path
                        # (skipped / cached / capped / not-instrumented /
                        # compile-failed / no-counts / measured / raised). The
                        # per-leg CACHE still re-attaches on a repeat firing of
                        # an already-measured oracle (a known rate only
                        # suppresses a NEW measurement, never the facts this
                        # leg already paid for).
                        _oid_u = sorted(_fired_ids)[0] if _fired_ids else None
                        if _oid_u is None or not jazzer_standalone_jar:
                            _cycle6_ev(
                                'cycle6_universal_screen_decided',
                                target=_oid_u, output='skipped',
                                reason=('this firing named no oracle id — '
                                        'nothing to measure' if _oid_u is None
                                        else 'no Jazzer standalone jar — the '
                                             'counting run cannot be executed '
                                             '(fail-open)'))
                        else:
                            # The build subdir index is claimed here (outside
                            # the step) so it stays globally unique across legs
                            # whatever the step decides to do.
                            _idx_u = _universal_seq
                            _universal_seq += 1

                            def _u_instrument(_s, _o):
                                from java.execution.oracle_mute import (
                                    instrument_for_counting as _ifc)
                                return _ifc(_s, _o)

                            def _u_compile(_instr, _idx=_idx_u):
                                _b = builder.build(
                                    _instr, selection.buggy_dir,
                                    output_subdir=f'uscreen_{_idx}')
                                return _b if (_b and _b.compiled) else None

                            def _u_count(_build):
                                from java.relations.relation_screen import (
                                    _run_counting_fuzz as _rcf)
                                return _rcf(_build, builder,
                                            selection.buggy_dir,
                                            jazzer_standalone_jar,
                                            jazzer_api_jar,
                                            args.screen_runs, 45)

                            (_u_notes, _universal_measured, _u_counts,
                             _u_outcome) = _universal_screen_step(
                                _oid_u, src, _rate_known, _universal_facts,
                                _universal_measured, _u_instrument,
                                _u_compile, _u_count)
                            if _u_outcome == 'measured':
                                print("      [universal-screen] measured "
                                      "oracle '" + _oid_u + "': violated="
                                      + str(_u_counts[1] if _u_counts else '?')
                                      + "/"
                                      + str(_u_counts[0] if _u_counts
                                            else '?'))
                            elif _u_outcome == 'capped':
                                print("      [universal-screen] cap: "
                                      + str(_UNIVERSAL_SCREEN_CAP)
                                      + " oracles already measured this leg "
                                      "— skipped oracle '" + _oid_u + "'")
                            # PART B: the screen KNOWS this rate now — record
                            # it so the delivery step below can state it
                            # whatever this block does with it.
                            if _u_counts:
                                _buggy_rate_counts.setdefault(_oid_u,
                                                              _u_counts)
                            for _un in _u_notes:
                                _fact_notes.append(_un)
                                evid = (evid + "\n" + _un) if evid else _un
                                if _un.startswith("[fire-rate fact]"):
                                    _rate_fact_attached = True
                    except Exception as _uexc:
                        _cycle6_ev('cycle6_universal_screen_decided',
                                   target=(sorted(_fired_ids)[0]
                                           if _fired_ids else None),
                                   output='raised',
                                   reason='universal-screen call site raised '
                                          '(%s: %s) — nothing attached '
                                          '(fail-open)'
                                          % (type(_uexc).__name__, _uexc))
                    # Cycle-6 item 4 PART B — unconditional delivery of a KNOWN
                    # buggy-side rate to the harness track (see
                    # `_deliver_buggy_rate`: whichever branch measured it, a
                    # rate that IS known must reach the judging evidence, and
                    # 6B keys on exactly this fact). Fail-open; it records both
                    # what it found and what it did.
                    _fr_b = _deliver_buggy_rate(
                        _fired_ids, _buggy_rate_counts, _rate_fact_attached,
                        _one_door_patched_counts, _one_door_demote)
                    if _fr_b:
                        _fact_notes.append(_fr_b)
                        evid = (evid + "\n" + _fr_b) if evid else _fr_b
                        _rate_fact_attached = True
                    # Cycle-5B(i): pinned-environment fact from the harness's
                    # OWN source (UTC/Locale/seed/size). Attached as a fact and
                    # used to void a dismissal that varies a pin.
                    from java.parsing.java_source import (
                        pinned_parameters as _pinp_s)
                    from java.relations.evidence_facts import (
                        pinned_environment_note as _pen_s,
                        disputed_computation_fact)
                    _pinned_s = _pinp_s(src or '')
                    _pin_note_s = _pen_s(_pinned_s)
                    if _pin_note_s:
                        _fact_notes.append(_pin_note_s)
                        evid = ((evid + "\n" + _pin_note_s)
                                if evid else _pin_note_s)
                        print(f"      [pinned-env] pins {sorted(_pinned_s)}")
                    _j3 = _j3_failing_test_block(failure_tests)
                    if _j3:
                        evid = (evid + "\n" + _j3) if evid else _j3
                    # Cycle-5B: recall-side dismissal lint (void-and-re-ask on a
                    # pinned-parameter dismissal). No structured drift-kill
                    # profile on the semantic track, so 5B(ii) is not keyed
                    # here. Fails open.
                    # Cycle-5C (inside adjudicate): IDENTICAL-ON-BOTH /
                    # fires-on-buggy is TERMINAL — no discretionary SOUND keep
                    # on such a firing unless family-duty answers YES. Reuse the
                    # ladder's family-duty result when it already asked; fails
                    # open. Single shared entrypoint: base verify -> 5B -> 5C.
                    # Cycle-7 (Math-65): repeat the shown source of any
                    # method this firing NAMES, beside the firing. The line that
                    # settles such a dispute is already in the skeleton — it sat
                    # once at char 27,051 of a 59,830-char prompt, and every
                    # reviewer that quoted it decided correctly while every one
                    # that missed it asserted the inverse from memory. Duplicates,
                    # never moves: the skeleton below is untouched.
                    _dc = disputed_computation_fact(
                        fired, '\n\n'.join(class_ctx) if class_ctx else None)
                    if _dc:
                        print("      [disputed-computation] repeating shown "
                              "source of the method(s) this firing names")
                        _fact_notes.append(_dc)
                        evid = (evid + "\n" + _dc) if evid else _dc
                    # 8.2 ladder: the reference-implementation fact.
                    # Every step records an event -- the stage read-out is
                    # per-event, and a step that produced nothing must say so
                    # rather than be absent.
                    if getattr(args, 'reference_impl', False):
                        _ref_fact = _reference_impl_fact(
                            args=args, fired=fired, class_ctx=class_ctx,
                            failure_tests=failure_tests, builder=builder,
                            buggy_dir=selection.buggy_dir,
                            patch_path=selection.patch_path,
                            trusted_values=trusted_values,
                            package=context.package,
                            imports=context.source_imports,
                            check_source=src,
                            patched_methods=_patched_method_names(context))
                        if _ref_fact:
                            print("      [reference-impl] fact attached")
                            _fact_notes.append(_ref_fact)
                            evid = ((evid + "\n" + _ref_fact) if evid
                                    else _ref_fact)
                    ok, why = adjudicate(
                        rv,
                        harness_source=src, fired_assertion=fired,
                        trusted_values=trusted_values,
                        concrete_evidence=evid,
                        code_context=('\n\n'.join(class_ctx)
                                      if class_ctx else None),
                        pinned_source=_pinned_s, evidence_profile=None,
                        failing_block=_j3_failing_test_block(failure_tests),
                        check_source=src, fd_prior=_fd_consult_result)
                    if ok:
                        # 8.25 DOOR PARITY (Spec-K, this time measured
                        # first): the corpus scan found the gate's ONLY
                        # reach on THIS door — the fuzz harness replays the
                        # failing test's scenario, so its firings carry
                        # test-state-coincident values; the replay door's
                        # never do. Same contract as the replay site: runs
                        # only on a keep, void skips it, abstain changes
                        # nothing.
                        from java.relations.reference_impl import (
                            reference_verdict_gate)
                        _arec, _awhy = _admitted_for(fired, class_ctx, src)
                        _fsr = _firing_state_reading(
                            fired, _arec, evid, builder,
                            selection.buggy_dir)
                        _gv, _gwhy = reference_verdict_gate(
                            fired, _arec, lookup_why=_awhy,
                            firing_reading=_fsr)
                        record_event(
                            'deterministic', method='reference-verdict-gate',
                            target=(fired or '')[:80],
                            output=('conviction VOIDED' if _gv == 'void'
                                    else 'gate abstains'),
                            reason=_gwhy)
                        if _gv == 'void':
                            print(f"  ∅ harness firing VOIDED by the "
                                  f"reference verdict gate")
                            print(f"      {_gwhy}")
                            drop_reasons.append(
                                (fired, 'reference-verdict-gate: ' + _gwhy))
                            continue
                        kept_reason = (fired, why)
                        break
                    # P4.3: remember which ORACLE the verifier judged
                    # unsound (and where), so a keep of the same check from
                    # another firing (whose message happened to hide the
                    # exculpating detail) can be reconciled below.
                    for _uid in _fired_ids:
                        _unsound_oracle_ids.setdefault(_uid, why)
                        _unsound_scope.setdefault(_uid, set()).add(
                            r.harness_path)
                    drop_reasons.append((fired, why))
                if kept_reason is not None:
                    print(f"  ✓ sound: {r.harness_path}")
                    print(f"      kept via: {kept_reason[0]}")
                    print(f"      {kept_reason[1]}")
                    _kept_findings.append(
                        (r, _oids(kept_reason[0] or ''), kept_reason[0]))
                else:
                    for fired, why in drop_reasons:
                        print(f"      fired: {fired}")
                        print(f"        {why}")
                    drop_finding(
                        r, 'all-fired-oracles-unsound',
                        f"all {len(drop_reasons)} fired oracle(s) dismissed",
                        reasons=[{'fired': (f or '')[:200],
                                  'why': (w or '')[:400]}
                                 for f, w in drop_reasons])
            # P4.3 ("one decision per crash, not per firing"): the same
            # check judged UNSOUND on one firing and kept on another is a
            # contradiction — the messages differ, the oracle doesn't. On
            # contradiction the DISMISSAL wins: the unsound verdict was
            # reached on a firing whose message exposed the exculpating
            # detail (minfix_w2 Lang-60-c: contains('\0') kept once where
            # the message hid that the input really contained '\0', and
            # dropped twice where it showed it).
            for _r, _kids, _kfired in _kept_findings:
                _clash = {
                    k for k in (_kids & set(_unsound_oracle_ids))
                    if k in _injected_rel_names
                    or _r.harness_path in _unsound_scope.get(k, ())}
                if _clash and _r.triggered:
                    _oid = sorted(_clash)[0]
                    drop_finding(
                        _r, 'dismissal-wins-reconciliation',
                        f"oracle {_oid} was judged unsound on another "
                        f"firing of the same check",
                        oracle_id=_oid,
                        unsound_because=(_unsound_oracle_ids[_oid]
                                         or '')[:400])

    # 7d) P3.2 replay: execute every screened relation (own-leg only —
    #     pooling removed 2026-07-19, user rule)
    #     DIRECTLY against the patched build. Until now a relation only
    #     mattered if the harness writer implemented it AND the patched
    #     fuzz found the right inputs — two coin flips that cost Math-2-o
    #     its verdict (the probe-validated mean-formula separates the pair
    #     deterministically, but no harness ever carried it to the right
    #     inputs). Firings NEVER convict on their own: each goes through
    #     the same LLM verifier as a harness firing; only a verifier-kept
    #     finding flips the verdict.
    if (args.replay_relations_on_patched and args.verify_relations
            and synthesized_relations and fuzz_results is not None
            and jazzer_standalone_jar):
        print("\n" + "#" * 20 + " relation replay on patched " + "#" * 20)
        try:
            from java.relations.relation_screen import replay_on_patched
            from java.relations.relation_verifier import RelationVerifier
            # Idempotent: run_all already built+verified this checkout, so
            # this returns the cached patched copy.
            _patched_dir = PatchedProjectBuilder().build_patched_dir(
                selection.buggy_dir, selection.patch_path)
            from java.parsing.java_source import trigger_seed_literals
            _trig_lits_r = trigger_seed_literals(
                [getattr(ft, 'method_source', '') for ft in failure_tests])
            _replay_findings = replay_on_patched(
                synthesized_relations,
                builder=builder,
                patched_dir=_patched_dir,
                jazzer_standalone_jar=jazzer_standalone_jar,
                package=context.package,
                imports=context.source_imports,
                jazzer_api_jar=jazzer_api_jar,
                trigger_literals=_trig_lits_r,
                runs=args.screen_runs,
            )
            record_extras['relation_replay_fired'] = [
                {'name': f['name'], 'tier': f['tier'], 'note': f['note'],
                 # Corpus 2feb27d: the capped trace copy was the ONLY copy
                 # of a firing's snapshot, and it ended mid-value. The
                 # UNTRUNCATED lines now persist in result.jsonl — the
                 # p1b corpus lives in the durable artifact, not in a
                 # display preview.
                 'fired_lines': f.get('fired_lines', [])}
                for f in _replay_findings]
            if _replay_findings:
                _verifier_model = (args.model
                                   or config.LOCAL_LLM_MODEL)
                _rv2 = RelationVerifier(
                    HarnessGenerator(model=_verifier_model,
                                     temperature=0.0, top_p=1.0),
                    votes=config.RELATION_VERIFIER_VOTES)
                _tvals = []
                for ft in failure_tests:
                    if getattr(ft, 'method_source', None):
                        _tvals.extend(
                            expected_assert_literals(ft.method_source))
                _tvals = list(dict.fromkeys(_tvals))
                _kept_replays = []
                _bug_summary_r = ''
                if failure_tests:
                    _ft0r = failure_tests[0]
                    _bug_summary_r = (
                        "failing test "
                        + (getattr(_ft0r, 'test_method', '') or '?')
                        + " — on the buggy build it fails with: "
                        + ((getattr(_ft0r, 'failure_message', '') or
                            getattr(_ft0r, 'exception_type', '') or '?')
                           )[:400])
                    _alines = [
                        _l.strip() for _l in
                        (getattr(_ft0r, 'method_source', '') or
                         '').splitlines()
                        if 'assert' in _l][:6]
                    if _alines:
                        _bug_summary_r += (
                            "\nThe failing test's own assertions (the "
                            "pinned observables):\n  "
                            + "\n  ".join(_alines))
                    try:
                        from java.parsing.java_source import (
                            oracle_ids_in_text as _oidsX)
                        _def_id = {e.split('.')[-1]
                                   for e in (expected_exceptions or [])
                                   if e}
                        _trig_oids = set()
                        for _d in (getattr(result,
                                           'accepted_trigger_details',
                                           []) or []):
                            _trig_oids |= _oidsX(_d or '')
                        for _hmap in (buggy_crash_types or {}).values():
                            for _oid in _trig_oids:
                                _def_id |= (_hmap.get(_oid) or set())
                        if _def_id:
                            _bug_summary_r += (
                                "\nUnderlying exception identity of the "
                                "defect (types recorded beneath the "
                                "trigger-firing checks on the buggy "
                                "build): "
                                + ", ".join(sorted(_def_id)[:6]))
                    except Exception:
                        pass
                for f in _replay_findings:
                    rel = f['relation']
                    # 8.4x p1a: the conviction carries the REAL firing —
                    # the thrown message with its values plus the exact
                    # consumed inputs — instead of a synthesized nameless
                    # line. Feeds the judge, the detector's message route,
                    # and the verdict gate's value comparison.
                    _fline = (f.get('fired_lines') or [None])[0]
                    _fired = (f"relation {f['name']} violated "
                              f"[replay-on-patched, {f['tier']} tier]"
                              + (f" — {_fline}" if _fline else ""))
                    _rfacts = []
                    _evid = ("[relation replay] the check below was "
                             "mechanically screened on the buggy build ("
                             + (getattr(rel, 'screen_note', '') or
                                'no screen note')
                             + ") and, compiled UNCHANGED against the "
                             "patched build, " + f['note'] + ". A correct "
                             "patch makes a sound contract relation go "
                             "quiet; judge whether the relation itself is "
                             "sound for ANY correct implementation "
                             "(tolerances generous, inputs fenced).")
                    _rfacts.append(_evid)
                    # The Closure-62-c FP fact (hfix11): a rule that fires
                    # on the TRIGGER literals on the patched build, while
                    # the REAL failing test passes there (guaranteed by the
                    # safety net, or we'd have exited bad_patch), is firing
                    # on its OWN reconstruction of the test's scenario on
                    # both builds. If the reconstruction were faithful it
                    # could not fire where the real test passes — the
                    # default explanation is that the rule's scenario lacks
                    # the test's setup/wiring, exactly like a divergent
                    # lifted check. A fact for the judge, never an
                    # auto-dismissal (fuzzed-tier firings — Math-2's shape
                    # — are untouched).
                    if f.get('tier') == 'trigger':
                        # Cycle-5A: trigger-tier wording NEUTRALIZED to
                        # symmetric — states the fact without a dismiss lean.
                        from java.relations.evidence_facts import (
                            trigger_tier_note as _ttn)
                        _evid += _ttn()
                    else:
                        # Fuzzed-tier comparison fact (the batch3 open
                        # item: this path produced the fpfix6 62-c FP with
                        # no computed buggy-side comparison). Screening
                        # already measured this same check on the BUGGY
                        # build — state the comparison as data instead of
                        # leaving the judge to reconstruct it from the
                        # screen-note prose. Both the FP shape (invented
                        # premise firing on both builds off-trigger) and
                        # the true-catch shape (a documented relation the
                        # trigger corpus cannot reach) share this firing
                        # signature, so the fact separates them by
                        # CONTRACT SOURCE, never by auto-dismissal.
                        _dirn = getattr(rel, 'screen_direction', None)
                        _bfr = getattr(rel, 'buggy_fire_ratio', 0) or 0
                        if _dirn == 'confirmed':
                            _evid += (
                                "\n[replay comparison fact] at screening "
                                "this relation was DIRECTION-CONFIRMED "
                                "(it fires on the buggy build at the "
                                "failing test's own trigger inputs — it "
                                "mechanically detects the reported "
                                "defect), but on THIS patched build it "
                                "is silent at those trigger inputs and "
                                "fires only on other fuzzed inputs: the "
                                "defect it measures is fixed AT the "
                                "trigger. Keep only if these firings "
                                "show the SAME defect surviving beyond "
                                "the trigger (the same observable at "
                                "inputs the trigger corpus merely "
                                "missed); firings explained by "
                                "edge-input fragility of the check "
                                "itself (special values, tolerances) do "
                                "not convict.")
                        elif _bfr > 0:
                            _evid += (
                                "\n[replay comparison fact] this "
                                "relation fired on "
                                f"{_bfr:.0%} of fuzzed inputs on the "
                                "BUGGY build at screening and was NOT "
                                "direction-confirmed there (no "
                                "mechanical evidence it detects the "
                                "reported defect at the failing test's "
                                "own inputs) — it fires on BOTH builds "
                                "away from the trigger, so the patch did "
                                "not change the behaviour it measures. "
                                "Decide by CONTRACT SOURCE: (a) the "
                                "asserted property is the documented "
                                "behaviour the reported bug is about (a "
                                "stated formula/range/format the failing "
                                "test also pins) — then firing on both "
                                "builds is the patch-failed-to-fix "
                                "pattern: KEEP; (b) it has no documented "
                                "source (an invented plausibility) or "
                                "concerns a feature unrelated to the "
                                "reported bug — pre-existing surface: "
                                "DISMISS.")
                        else:
                            _evid += (
                                "\n[replay comparison fact] this "
                                "relation was SILENT everywhere on the "
                                "buggy build at screening (a tripwire) "
                                "and fires on THIS patched build — the "
                                "patch introduced the violation. Keep "
                                "only if the asserted requirement has a "
                                "stated source in the relation's "
                                "contract line: a declared @throws, a "
                                "documented range/format/formula, or a "
                                "visible code invariant that every "
                                "correct implementation of this API "
                                "must preserve; an invented "
                                "plausibility with no such source does "
                                "not convict.")
                    # Spec H: fire-rate fact — the buggy-side screen ratio and
                    # the patched-side replay-fuzz counts, labelled with
                    # percentages and interpretation so the judge rules against
                    # numbers, not prose. When the relation was demoted at
                    # screening (above-ratio-cap / inverted, replay-only), that
                    # demotion reason rides along in the fact.
                    from java.relations.evidence_facts import fire_rate_fact
                    _sstats = getattr(rel, 'screen_stats', None) or (None, None)
                    _sdec = getattr(rel, 'screen_decision', None) or {}
                    _sreason = _sdec.get('reason', '') or ''
                    _demote = (_sreason
                               if ('above-ratio-cap' in _sreason
                                   or 'inverted' in _sreason)
                               else '')
                    _frfact = fire_rate_fact(
                        _sstats[0], _sstats[1],
                        f.get('patched_checked'), f.get('patched_violated'),
                        _demote)
                    if _frfact:
                        _evid += "\n" + _frfact
                        print(f"      [fire-rate] {_frfact[:120]}")
                    # Cycle-5B(i): pinned-environment fact — what the check's
                    # OWN source fixes (UTC/Locale/seed/size). Attached as a
                    # fact here AND used below to void a dismissal that rests
                    # on varying one of those pins.
                    from java.parsing.java_source import (
                        pinned_parameters as _pinp)
                    from java.relations.evidence_facts import (
                        pinned_environment_note as _pen,
                        disputed_computation_fact)
                    _pinned = _pinp(getattr(rel, 'check', '') or '')
                    _pin_note = _pen(_pinned)
                    if _pin_note:
                        _evid += "\n" + _pin_note
                        print(f"      [pinned-env] pins {sorted(_pinned)}")
                    # Cycle-5B(ii): the drift-kill signature for this firing —
                    # silent on the buggy build, a deterministic trigger-tier
                    # replay, and firing on the patched build.
                    _b_checked, _b_viol = _sstats[0], _sstats[1]
                    _b_silent = bool(
                        _b_checked and _b_checked > 0
                        and (_b_viol or 0) / _b_checked < 0.01)
                    _ev_profile = {
                        'buggy_silent': _b_silent,
                        'deterministic_trigger': f.get('tier') == 'trigger',
                        'patched_firing': bool(f.get('patched_violated')),
                    }
                    # everything appended after the base note is a
                    # computed fact — hand the same text to attribution
                    if len(_evid) > len(_rfacts[0]):
                        _rfacts.append(_evid[len(_rfacts[0]):].strip())
                    _j3r = _j3_failing_test_block(failure_tests)
                    if _j3r:
                        _evid += "\n" + _j3r
                    _src = ("// relation: " + f['name'] + "\n"
                            + "// holds because: "
                            + (getattr(rel, 'contract', '') or '?') + "\n"
                            + "// valid input: "
                            + (getattr(rel, 'input_spec', '') or '?') + "\n"
                            + (getattr(rel, 'check', '') or ''))
                    _dirconf = (getattr(rel, 'screen_direction', None)
                                == 'confirmed')
                    # Single shared entrypoint: base verify -> 5B recall-side
                    # dismissal lint (void-and-re-ask on a pinned-parameter
                    # dismissal or an uncited drift-kill hypothetical) -> 5C
                    # terminal identical gate (a SOUND keep on an
                    # IDENTICAL-ON-BOTH / fires-on-buggy firing needs
                    # family-duty=YES). The 5C gate is skipped for a
                    # direction-confirmed relation (a mechanical catch: it fires
                    # on the buggy build at the trigger inputs). Fails open.
                    # Cycle-7 (Math-65) — same fact at the second judge
                    # site, so one-door fact parity holds.
                    _dc2 = disputed_computation_fact(
                        _fired, '\n\n'.join(class_ctx) if class_ctx else None)
                    if _dc2:
                        print("      [disputed-computation] repeating shown "
                              "source of the method(s) this firing names")
                        _evid = (_evid + "\n" + _dc2) if _evid else _dc2
                    # 8.2: the SAME mechanism on the replay track. Stage-1
                    # roll 2 recorded ZERO reference-impl events because this
                    # leg convicted here and the wiring existed only on the
                    # harness track — Spec K's one-door parity lesson exactly
                    # (Math-73-c: a fact on one door and not the other makes
                    # the two tracks judge the same check differently).
                    if getattr(args, 'reference_impl', False):
                        _ref_fact2 = _reference_impl_fact(
                            args=args, fired=_fired, class_ctx=class_ctx,
                            failure_tests=failure_tests, builder=builder,
                            buggy_dir=selection.buggy_dir,
                            patch_path=selection.patch_path,
                            trusted_values=_tvals,
                            package=context.package,
                            imports=context.source_imports,
                            check_source=_src,
                            patched_methods=_patched_method_names(context))
                        if _ref_fact2:
                            print("      [reference-impl] fact attached "
                                  "(replay track)")
                            _evid = ((_evid + "\n" + _ref_fact2) if _evid
                                     else _ref_fact2)
                    ok, why = adjudicate(
                        _rv2,
                        harness_source=_src, fired_assertion=_fired,
                        trusted_values=_tvals, concrete_evidence=_evid,
                        code_context=('\n\n'.join(class_ctx)
                                      if class_ctx else None),
                        pinned_source=_pinned, evidence_profile=_ev_profile,
                        failing_block=_j3_failing_test_block(failure_tests),
                        check_source=_src, fd_prior=None,
                        is_direction_confirmed=_dirconf)
                    if ok:
                        # 8.25 THE REFERENCE VERDICT GATE (user-approved
                        # 2026-08-07): deterministic, judge-independent.
                        # Runs ONLY on kept convictions — it can void or
                        # corroborate a keep, never manufacture one (the
                        # 38-row correct-dismissals population is untouched
                        # by construction).
                        from java.relations.reference_impl import (
                            reference_verdict_gate)
                        _arec2, _awhy2 = _admitted_for(_fired, class_ctx, _src)
                        _fsr2 = _firing_state_reading(
                            _fired, _arec2, _evid, builder,
                            selection.buggy_dir)
                        _gv, _gwhy = reference_verdict_gate(
                            _fired, _arec2, lookup_why=_awhy2,
                            firing_reading=_fsr2)
                        record_event(
                            'deterministic', method='reference-verdict-gate',
                            target=f['name'],
                            output=('conviction VOIDED' if _gv == 'void'
                                    else 'gate abstains'),
                            reason=_gwhy)
                        if _gv == 'void':
                            print(f"  ∅ replay conviction VOIDED by the "
                                  f"reference verdict gate: {f['name']}")
                            print(f"      {_gwhy}")
                            continue
                        print(f"  ✓ replay conviction kept: {f['name']} "
                              f"[{f['tier']}]")
                        print(f"      {why}")
                        _kept_replays.append(
                            {'name': f['name'], 'tier': f['tier'],
                             'note': f['note'], 'why': why})
                    else:
                        print(f"  ✗ replay firing dropped as unsound: "
                              f"{f['name']} — {why}")
                if _kept_replays:
                    record_extras['relation_replay_kept'] = _kept_replays
                    # extras are applied to the record LAST, so this
                    # overrides the harness-derived False. Only ever set
                    # on conviction — never write False here.
                    flag_overfitting(
                        record_extras, 'relation-replay-conviction',
                        f"{len(_kept_replays)} verifier-kept relation "
                        f"conviction(s)",
                        kept=[k['name'] for k in _kept_replays])
        except Exception as exc:
            print(f"  [replay] failed ({exc}) — replay contributes nothing "
                  "this run")

    # A run is scoreable only if we actually fuzzed at least one harness
    # against the patched code; otherwise we have no overfitting verdict.
    if fuzz_results:
        status = 'evaluated'
    else:
        status = 'no_harnesses'
    # ONE complete markdown transcript for the full run too (harness
    # generation + judge LLM calls are captured via the global recorder).
    try:
        if args.results_json:
            _tp = os.path.join(os.path.dirname(args.results_json), 'trace.md')
            _caught = bool(fuzz_results
                           and any(getattr(r, 'triggered', False)
                                   for r in fuzz_results))
            _lbl = 'overfitting' if args.overfitting else 'correct'
            if _lbl == 'overfitting':
                _verdict = ('OVERFIT CAUGHT (a harness fired on the patched '
                            'build)' if _caught else
                            'overfit MISSED (all harnesses quiet on the '
                            'patched build)')
            else:
                _verdict = ('FALSE ALARM (a harness fired on this CORRECT '
                            'patch)' if _caught else
                            'correctly quiet (no false alarm)')
            _write_trace_md(
                _tp, f"{selection.project_name}-{selection.bug_id}", _lbl,
                get_events(),
                outcome=f"{_verdict}. [{status}; "
                        f"{len(fuzz_results or [])} harness(es) fuzzed on the "
                        f"patched build; campaign converged="
                        f"{getattr(result, 'converged', None)}]")
            print(f"  [trace] wrote {_tp} ({len(get_events())} steps)")
    except Exception as _e:
        print(f"  [trace] dump failed: {_e}")
    # 8.21(c): record the class context the judge was shown.
    #
    # The pair could not answer 8.2's build/no-build question -- its rule named
    # "of trigger rows", the trigger needs code_context to evaluate, and
    # result.jsonl did not carry it. The measurement was lost for want of a
    # field, after the run that could have supplied it had already been paid for.
    #
    # RAW, not a precomputed flag: a flag would freeze today's detector into the
    # archive, and the detector is the thing most likely to change. "The record
    # must not pre-decide what future consumers can use" -- the same rule that
    # made observed_values string-preserving in 8.3.
    try:
        record_extras['code_context'] = ('\n\n'.join(class_ctx)
                                         if class_ctx else None)
    except Exception:
        pass                       # recording never breaks a run
    _emit_record(args.results_json,
                 label='correct' if args.correct else 'overfitting',
                 status=status, selection=selection,
                 result=result, fuzz_results=fuzz_results,
                 bug_kind=bug_kind, extras=record_extras)
    _print_token_usage()

    sys.exit(0 if result.converged else 2)


def _print_failure_tests(failure_tests) -> None:
    if not failure_tests:
        print("No bug-triggering tests found — continuing without seed.")
        return
    print(f"Found {len(failure_tests)} bug-triggering test(s):")
    for ft in failure_tests:
        marker = '✓' if ft.has_source else '?'
        exc = f"  [{ft.exception_type}]" if ft.exception_type else ""
        print(f"  {marker} {ft.test_class}::{ft.test_method}{exc}")


def _print_crash_input(crash_input) -> None:
    if crash_input is None or not crash_input.has_evidence:
        print("No ground-truth crash input captured — prompt will fall "
              "back to test-source anchoring.")
        return
    print("Captured ground-truth crash input:")
    if crash_input.exception_type:
        print(f"  throwable : {crash_input.exception_type}")
    if crash_input.message:
        print(f"  message   : {crash_input.message}")
    if crash_input.throw_site:
        print(f"  thrown_at : {crash_input.throw_site}")
    if crash_input.literals:
        print(f"  literals  : {crash_input.literals}")


def _print_summary(selection, result: CampaignResult) -> None:
    print("\n" + "#" * 20 + " campaign " + "#" * 20)
    print(f"project       : {selection.project_name}")
    print(f"bug           : {selection.bug_id} ({selection.apr_tool})")
    print(f"buggy dir     : {selection.buggy_dir}")
    print(f"target wins   : {result.target_successes}")
    print(f"attempts used : {result.attempts}")
    print(f"wins          : {result.achieved_successes}")
    print(f"success rate  : {result.success_rate:.1%}")
    print(f"distinct crashes (buggy ver): {result.distinct_signatures}")
    if result.successful_results:
        print("successful harnesses:")
        for br, sig in zip(result.successful_results,
                           result.accepted_signatures):
            tag = f"  [crash: {sig}]" if sig else ""
            print(f"  - {br.harness_path}{tag}")
    print("#" * 50)


def _print_fuzz_summary(fuzz_results) -> None:
    triggered = [r for r in fuzz_results if r.triggered]
    clean     = [r for r in fuzz_results if not r.triggered and not r.timed_out]
    timeouts  = [r for r in fuzz_results if r.timed_out]

    print("\n" + "#" * 20 + " fuzz summary " + "#" * 20)
    print(f"harnesses run  : {len(fuzz_results)}")
    print(f"crashed        : {len(triggered)}  "
          "(vulnerability still reachable — patch may be overfitting)")
    print(f"clean          : {len(clean)}  "
          "(vulnerability not triggered — patch appears to fix the bug)")
    print(f"timed out      : {len(timeouts)}")
    if triggered:
        print("crashing harnesses:")
        for r in triggered:
            print(f"  - {r.harness_path}")
    print("#" * 50)


if __name__ == '__main__':
    main()