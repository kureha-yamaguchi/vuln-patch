"""The judge's view: what the one-shot LLM baseline was shown, measured
the same way as the pipeline's patch-derived set P.

EVALUATION ONLY. Nothing here feeds back into the pipeline or into the
baseline. It reads finished artifacts — a baseline `records.jsonl` and a
`root_cause.json` a measurement run already wrote — and produces a number.
No module outside `java.measurements` imports it, no prompt, gate, oracle
or verdict can reach it, and it never writes into a leg directory.

WHAT IT MEASURES
================
`src/baseline_llmjudge/` answers the same question as the pipeline (is this
repair-tool patch overfitting?) from the same pre-execution evidence, in one
shot, without running anything. So the baseline has a neighbourhood too: the
set of methods its evidence put in front of the model. Call it J.

The measurement package computes

    RCR = |R n P| / |R|

where R is the developer's region (`root_cause.json`) and P is the
neighbourhood the PIPELINE's model was shown (`patch_derived.json`). This
module computes the same ratio with J in place of P, against the SAME
`root_cause.json`, so the two numbers sit in one column:

    RCR_judge = |R n J| / |R|

Same granularity (method), same R-variants (R0 / R1 / full), same ring
breakdown, same `MethodIndex` matching, same JDK and mislabelled-receiver
filtering, same identity space (the primary build's coverage method list
when the leg has one). The only thing that changes is which side plays P.

HOW J IS BUILT, AND FROM WHICH RECORD FIELDS
============================================
A baseline record does NOT store the evidence text it sent to the model. It
stores a digest of it and a small audit summary. So J is built from two
sources, and the module says which one it used in every row it emits
(`evidence_source`).

1. From the record alone (`evidence_source: 'facts'`). Fields read:

     evidence_facts.touched_functions   method NAMES the patch changed
     evidence_facts.package             the package they live in
     evidence_facts.modified_files      the file(s) the patch changed
     parity_manifest.blocks             fallback for the names above: a
                                        block is called 'touched_function:<name>'
     evidence_facts.reachable_count     how many reachable methods the
                                        evidence carried — a COUNT, not a list
     evidence_facts.neighbourhood_notes lookup failures, recorded as-is

   A seed ref is then `<package>.<class from the modified file>.<name>`
   with UNKNOWN parameter types, and the callee ring is EMPTY: the record
   keeps the size of the reachable list, never its members. So a
   facts-only row measures the seed ring honestly and reports
   `reachable_count` next to it as the callee ring it could not recover.

2. From the rendered evidence text, when it can be recovered and VERIFIED
   (`evidence_source: 'text'`). `baseline_llmjudge.defects4j.context` caches
   the rendered evidence per patch as `<cache_dir>/<patch stem>.json` with a
   `text` field, and the record carries `evidence_sha256`, the digest of
   exactly that string. `evidence_text_for` reads the cache entry and
   returns its text ONLY when the digest matches, because the cache is
   rebuilt in place when the extraction changes: on the runs in this repo
   the semantic cache still matches its records byte for byte and the
   crashing cache no longer does. An unverified cache entry would attribute
   a neighbourhood the judge never saw, so it is refused, and the row falls
   back to source 1.

   With the text, J carries all three rings, parsed out of the blocks the
   renderer emits:

     SEED    `Function \\`name\\`:` + its `<signature>`, which gives the
             real parameter types
     CALLER  each `<xref>...</xref>` body — the calling method's SOURCE,
             which is all the pipeline's own prompt shows. The declaring
             class is not in the text, so the ref is class-less and can
             only be matched by (name, arity), exactly as
             `patch_derived.caller_ref_from_source` does for P.
     CALLEE  each `- <label>` line inside `<root_cause_reachable>`, plus
             each `<callee name=".." from="File.java">` declaration inside
             a function block. Depth is always 1: the evidence records no
             distance, the same limitation `patch_derived` documents.

WHAT J DELIBERATELY LEAVES OUT
==============================
The semantic evidence also carries class skeletons and a state-coupling
block, which name further methods. They are class-level context, not a
caller or reachable list, and `patch_derived` does not read their
counterparts into P either, so counting them would make J wider than P for
reasons that have nothing to do with the comparison. `<callee>`
declarations ARE counted, and `patch_derived` does not read the pipeline's
copy of them into P, so J can be a few methods wider than P on that block
alone. `shown_sources` breaks the set down by which block each member came
from, so any row can be re-read without a source.

Every member of J carries `provenance='judge'`, so a set written by this
module can never be mistaken on disk for one the pipeline's own builder
produced.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from typing import Dict, List, Optional, Tuple

from metrics.core import locations as loc
from metrics.core import definitions as M
from . import patch_derived as PD

__all__ = ['PROVENANCE', 'judge_shown_set', 'shown_sources',
           'evidence_text_for', 'rcr_for_judge', 'pair_records_with_legs',
           'read_records', 'judge_rows', 'render_table', 'main']

#: Every member of J is tagged with this, so a judge set is never mistaken
#: for a pipeline-built one.
PROVENANCE = 'judge'

#: The R-variants this module reports, method granularity only. A judge set
#: is a set of METHOD names; the evidence carries no line numbers, so there
#: is nothing to say at line granularity.
R_VARIANTS = M.R_VARIANTS
GRANULARITY = 'method'


# ---------------------------------------------------------------------------
# reading the record
# ---------------------------------------------------------------------------

def read_records(records_path: str) -> List[dict]:
    """Every JSON object in a baseline `records.jsonl`, in file order."""
    out: List[dict] = []
    with open(records_path, encoding='utf-8') as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            try:
                out.append(json.loads(line))
            except ValueError:
                continue
    return out


def record_identity(record: dict) -> dict:
    """The fields that name which patch a record is about."""
    bug_id = record.get('bug_id')
    return {
        'project': record.get('project'),
        'bug_id': str(bug_id) if bug_id is not None else None,
        'apr_tool': record.get('apr_tool'),
        'label': record.get('label'),
        'patch': record.get('patch'),
        'patch_stem': patch_stem(record),
    }


def patch_stem(record: dict) -> Optional[str]:
    """'patch1-Lang-24-ACS.patch' -> 'patch1-Lang-24-ACS' (the leg name's
    middle part, and the evidence cache's file name)."""
    name = record.get('patch')
    if not name:
        return None
    return os.path.splitext(os.path.basename(name))[0]


def identity_key(record: dict) -> Tuple:
    """The pairing key: project, bug, repair tool, label."""
    ident = record_identity(record)
    return (ident['project'], ident['bug_id'], ident['apr_tool'],
            ident['label'])


def _touched_function_names(record: dict) -> List[str]:
    """The names of the patched functions the evidence showed.

    `evidence_facts.touched_functions` first; the parity manifest's
    `touched_function:<name>` block names when the facts are absent, so a
    record written before the facts existed still yields a seed ring."""
    facts = record.get('evidence_facts') or {}
    names = [str(n) for n in (facts.get('touched_functions') or [])]
    if names:
        return names
    blocks = ((record.get('parity_manifest') or {}).get('blocks') or [])
    out = []
    for b in blocks:
        name = str(b.get('name') or '')
        if name.startswith('touched_function:'):
            out.append(name.split(':', 1)[1])
    return out


def _seed_class(record: dict) -> str:
    """The class the seeds are attributed to, or '' when it is not certain.

    `<package>.<file base name>` when the patch touched exactly one file.
    With several files there is no way to say which function came from
    which, so the class is left empty and the ref is matched by (name,
    arity) alone — the same fallback `patch_derived` uses for a caller."""
    facts = record.get('evidence_facts') or {}
    files = [f for f in (facts.get('modified_files') or []) if f]
    if len(files) != 1:
        return ''
    base = os.path.basename(str(files[0]).replace('\\', '/'))
    simple = base[:-5] if base.endswith('.java') else base
    package = (facts.get('package') or '').strip()
    return f'{package}.{simple}' if package else simple


def has_evidence(record: dict) -> bool:
    """Did this record get the pipeline's evidence at all?

    A record whose status is not `evaluated` carries a reason instead of
    evidence (`semantic_skip`, `bug_not_reproduced`, `error`, ...), and a
    record with neither `evidence_facts` nor a parity manifest has nothing
    to build a set from."""
    if record.get('evidence_facts') or record.get('parity_manifest'):
        return True
    return False


# ---------------------------------------------------------------------------
# recovering the rendered evidence text
# ---------------------------------------------------------------------------

def evidence_text_for(record: dict,
                      cache_dirs) -> Tuple[Optional[str], str]:
    """(the evidence text this record was built from, why not).

    The baseline caches its rendered evidence per patch, and the record
    carries `evidence_sha256`, the digest of that exact string. The cache
    is rebuilt in place whenever the extraction changes, so a cache entry
    is used ONLY when its digest matches the record's. The reason string
    says what happened: 'verified', 'no_digest', 'not_cached',
    'digest_mismatch'.

    An `evidence_text` key on the record itself wins over any cache: a test
    (or a caller that has the string already) can inject it directly."""
    injected = record.get('evidence_text')
    if isinstance(injected, str) and injected:
        return injected, 'injected'
    digest = record.get('evidence_sha256')
    stem = patch_stem(record)
    if not digest or not stem:
        return None, 'no_digest'
    seen = False
    for d in (cache_dirs or []):
        path = os.path.join(d, stem + '.json')
        if not os.path.isfile(path):
            continue
        seen = True
        try:
            with open(path, encoding='utf-8') as fh:
                cached = json.load(fh)
        except (OSError, ValueError):
            continue
        text = cached.get('text')
        if not isinstance(text, str):
            continue
        if hashlib.sha256(text.encode()).hexdigest() == digest:
            return text, 'verified'
    return None, ('digest_mismatch' if seen else 'not_cached')


# ---------------------------------------------------------------------------
# parsing the evidence text
# ---------------------------------------------------------------------------

_FUNCTION_RE = re.compile(
    r'^Function `(?P<name>[^`]+)`:\n<signature>\n(?P<sig>.*?)\n</signature>',
    re.MULTILINE | re.DOTALL)
_REACHABLE_BLOCK_RE = re.compile(
    r'<root_cause_reachable>\n(?P<body>.*?)\n</root_cause_reachable>',
    re.DOTALL)
_REACHABLE_OMITTED_RE = re.compile(
    r'\(\+(?P<n>\d+) more reachable functions omitted\.\)')
_CALLEE_TAG_RE = re.compile(
    r'<callee\s+name="(?P<name>[^"]+)"(?:\s+from="(?P<file>[^"]*)")?[^>]*>')
_XREF_RE = re.compile(r'<xref>\n(?P<body>.*?)\n</xref>', re.DOTALL)


def _params_from_signature(sig: str) -> Optional[Tuple[str, ...]]:
    """'public static boolean toBoolean(String str) throws X' ->
    ('String',). None when the signature has no bracket pair."""
    open_paren = sig.find('(')
    if open_paren < 0:
        return None
    found = PD._balanced_args(sig, open_paren)
    if found is None:
        return None
    args, _close = found
    params = []
    for part in loc._split_top_level(args):
        # `_param_type` is `patch_derived`'s own formal-parameter reader;
        # using it is what keeps a judge seed spelled the way a pipeline
        # seed is.
        t = PD._param_type(part)
        if t:
            params.append(t)
    return tuple(params)


def _callee_ref(name: str, source_file: Optional[str]) -> loc.MethodRef:
    """A `<callee name=".." from="Foo.java">` declaration.

    The tag names the method and the FILE it was read from, so the class is
    the file's base name — a simple name, matched by (simple class, name)
    with the arity ignored, because the tag carries no parameter list."""
    cls = ''
    if source_file:
        base = os.path.basename(source_file)
        cls = base[:-5] if base.endswith('.java') else base
    return loc.MethodRef(cls, name, ())


# ---------------------------------------------------------------------------
# J — the set the judge was shown
# ---------------------------------------------------------------------------

def judge_shown_set(record: dict,
                    evidence_text: Optional[str] = None) -> loc.MethodSet:
    """The methods the baseline's evidence put in front of the model.

    Ring SEED for the patched functions; CALLER and CALLEE for the caller
    and reachable lists the evidence carried. Every member is tagged
    `provenance='judge'`. Names that parse as nothing land in
    `MethodSet.unmatched` rather than being dropped silently.

    With no `evidence_text` the callee and caller rings are empty: the
    record stores the SIZE of the reachable list, not its members. See the
    module docstring."""
    ms, _sources = _build_shown(record, evidence_text)
    return ms


def shown_sources(record: dict,
                  evidence_text: Optional[str] = None) -> dict:
    """How many members of J each block of the evidence contributed.

    Keys: `seed_functions`, `reachable_labels`, `callee_declarations`,
    `xrefs`, plus `reachable_omitted` (the '(+N more ...)' line the
    renderer prints when the reachable list was capped) and
    `reachable_count` (what the record says the list's length was). The
    counts are of names OFFERED, before duplicates collapse, so they add up
    to at least `len(J)`."""
    _ms, sources = _build_shown(record, evidence_text)
    return sources


def _build_shown(record: dict,
                 evidence_text: Optional[str]) -> Tuple[loc.MethodSet, dict]:
    facts = record.get('evidence_facts') or {}
    ms = loc.MethodSet()
    sources = {'seed_functions': 0, 'reachable_labels': 0,
               'callee_declarations': 0, 'xrefs': 0,
               'reachable_omitted': 0,
               'reachable_count': facts.get('reachable_count'),
               'neighbourhood_notes': list(
                   facts.get('neighbourhood_notes') or [])}

    cls = _seed_class(record)
    # -- seeds ---------------------------------------------------------
    signatures: Dict[str, Tuple[str, ...]] = {}
    if evidence_text:
        for m in _FUNCTION_RE.finditer(evidence_text):
            params = _params_from_signature(m.group('sig'))
            if params is not None:
                signatures[m.group('name')] = params
    for name in _touched_function_names(record):
        sources['seed_functions'] += 1
        params = signatures.get(name)
        ms.add(loc.MethodRef(cls, name, params or ()), loc.SEED, 0,
               provenance=PROVENANCE)

    if not evidence_text:
        return ms, sources

    # -- callees: the reachable list, in prompt-label form --------------
    block = _REACHABLE_BLOCK_RE.search(evidence_text)
    if block:
        for line in block.group('body').splitlines():
            line = line.strip()
            if not line.startswith('- '):
                continue
            label = line[2:].strip()
            sources['reachable_labels'] += 1
            ref, _arity_known = PD._ref_from_label(label)
            if ref is None:
                if label not in ms.unmatched:
                    ms.unmatched.append(label)
                continue
            ms.add(ref, loc.CALLEE, 1, provenance=PROVENANCE)
    omitted = _REACHABLE_OMITTED_RE.search(evidence_text)
    if omitted:
        sources['reachable_omitted'] = int(omitted.group('n'))

    # -- callees: the declarations shown inside a function block --------
    for m in _CALLEE_TAG_RE.finditer(evidence_text):
        sources['callee_declarations'] += 1
        ms.add(_callee_ref(m.group('name'), m.group('file')),
               loc.CALLEE, 1, provenance=PROVENANCE)

    # -- callers: the call-site examples, as source text ----------------
    for m in _XREF_RE.finditer(evidence_text):
        sources['xrefs'] += 1
        ref = PD.caller_ref_from_source(m.group('body'))
        if ref is None:
            continue
        ms.add(ref, loc.CALLER, 1, provenance=PROVENANCE)

    return ms, sources


# ---------------------------------------------------------------------------
# RCR against a measured root_cause.json
# ---------------------------------------------------------------------------

def _population_for(root_cause_json_path: str):
    """The identity space to match R and J in.

    `metrics.compute_leg` uses the primary build's coverage method list
    when the leg has one, so RCR_judge uses the same list from the same
    directory. Without coverage the space is R itself, which is what
    `metrics` falls back to as well."""
    mdir = os.path.dirname(os.path.abspath(root_cause_json_path))
    covs = M._load_coverage(mdir)
    primary = next((b for b in M.BUILDS if b in covs), None)
    if primary is None:
        return None, None
    return (covs[primary].all_methods or None), primary


def _map_judge(ms: loc.MethodSet, index: loc.MethodIndex,
               r_refs=None):
    """J mapped into `index`'s identity space, with the arity rule.

    A judge ref that carries no parameter types has UNKNOWN arity, not zero
    — the evidence renderer prints an argument list only when two kept
    methods would otherwise print the same word — so it is looked up with
    `frame=True`, which matches on (simple class, name) and ignores arity.
    A ref that does carry types is looked up normally.

    THE OVERLOAD RETRY. When the identity space is a build's whole method
    list, an arity-less name that the class overloads is ambiguous there and
    resolves to nothing, even when R holds exactly one method by that name.
    Left alone, that reads as "the judge was not shown the developer's
    method" when what happened is that the evidence spelled the method
    without its parameter types. So such a ref is retried against R ALONE,
    and counted separately (`j_by_name_in_r`) so any row can be re-read with
    the retried members removed. The retry is a real risk in one direction:
    if the judge was shown a DIFFERENT overload from the one the developer
    fixed, this credits it wrongly. It fires only when the name is
    ambiguous in the wide space and unique in R."""
    canon: Dict[loc.MethodRef, str] = {}
    retry = loc.MethodIndex(sorted(r_refs)) if r_refs else None
    unmatched = 0
    by_name_in_r = 0
    for ref in ms.refs():
        arity_unknown = (len(ref.params) == 0)
        hit = index.lookup(ref, frame=arity_unknown)
        if hit is None and arity_unknown and retry is not None:
            hit = retry.lookup(ref, frame=True)
            if hit is not None:
                by_name_in_r += 1
        if hit is None:
            unmatched += 1
            continue
        ring = ms.ring_of(ref)
        if hit not in canon or M._rank(ring) < M._rank(canon[hit]):
            canon[hit] = ring
    return canon, unmatched, by_name_in_r


def rcr_for_judge(record: dict, root_cause_json_path: str, *,
                  evidence_text: Optional[str] = None,
                  evidence_source: str = 'facts') -> dict:
    """RCR for one baseline record against one measured `root_cause.json`.

    Method granularity, the three R-variants, aggregate and per ring, each
    reported as `{'value', 'num', 'den', 'by_ring'}` — the same shape and
    the same key names `metrics.py` writes, so a judge row and a pipeline
    row can be read by the same code.

    `available` is False, with a `reason`, whenever the number cannot be
    computed: the record carries no evidence, or the root-cause file is
    missing or unreadable. Never raises for either case."""
    out: dict = dict(record_identity(record),
                     status=record.get('status'),
                     bug_kind=record.get('bug_kind'),
                     prompt_version=record.get('prompt_version'),
                     root_cause=os.path.abspath(root_cause_json_path),
                     evidence_source=evidence_source,
                     granularity=GRANULARITY,
                     available=False, reason=None)

    if not has_evidence(record):
        out['reason'] = ('record carries no evidence '
                         f"(status={record.get('status')!r})")
        return out

    # `_root_cause_parts` returns the method set first and the manifest
    # last, with the line sets in between; it has gained a set before now,
    # so the two ends are taken by position rather than by unpacking a
    # fixed width.
    parts = M._root_cause_parts(root_cause_json_path)
    r_methods, manifest = parts[0], parts[-1]
    if r_methods is None and manifest is None:
        out['reason'] = 'no readable root_cause.json at that path'
        return out

    jset, sources = _build_shown(record, evidence_text)
    out['shown_sources'] = sources

    # The same two filters `metrics.compute_leg` applies to P and R before
    # it intersects them. Skipping either here would compare a filtered R
    # against an unfiltered J.
    population, primary = _population_for(root_cause_json_path)
    r_methods, r_jdk = M._strip_jdk(r_methods)
    manifest, _ = M._strip_jdk(manifest)
    jset, j_jdk = M._strip_jdk(jset)
    r_methods, r_mis = M._strip_mislabelled(r_methods, population)
    manifest, _ = M._strip_mislabelled(manifest, population)
    jset, j_mis = M._strip_mislabelled(jset, population)
    out['jdk_dropped'] = {'J_method': j_jdk, 'R_method': r_jdk,
                          'J_mislabelled': j_mis, 'R_mislabelled': r_mis}

    if r_methods is None:
        out['reason'] = 'root_cause.json carries no method set'
        return out

    rvars = M._r_variants(r_methods, manifest)
    out['sizes'] = {
        'J_method': len(jset),
        'J_method_by_ring': {r: len(jset.refs(r)) for r in M.RING_ORDER},
        'J_unresolved_names': len(jset.unmatched),
        'R_method': {k: len(v) for k, v in rvars.items()},
        'R_method_by_ring': {
            k: {r: len(v.refs(r)) for r in M.RING_ORDER}
            for k, v in rvars.items()},
        'manifest_method': len(manifest) if manifest is not None else None,
    }

    matching: dict = {}
    for rvar, rset in rvars.items():
        if population is None:
            index = loc.MethodIndex(rset.refs())
            r_canon, r_unmatched = M._map_set(rset, index, identity=True)
        else:
            index = loc.MethodIndex(population)
            r_canon, r_unmatched = M._map_set(rset, index, identity=False)
        j_canon, j_unmatched, j_by_name = _map_judge(jset, index,
                                                     set(r_canon))
        r_set, j_set = set(r_canon), set(j_canon)
        matching[f'method__{rvar}__primary'] = {
            'space': ('coverage_all_methods' if population is not None
                      else 'R'),
            'build': primary,
            'r_unmatched': r_unmatched,
            'j_unmatched': j_unmatched,
            # arity-less names the wide space could not disambiguate and R
            # could: see `_map_judge`
            'j_by_name_in_r': j_by_name,
            'ambiguous': index.ambiguous,
            'missing': index.missing,
            'reclassified': index.reclassified,
        }
        ring_of = lambda x, _c=r_canon: _c.get(x, loc.OUTSIDE)
        out[M.metric_key('rcr', GRANULARITY, rvar, None)] = dict(
            M._ratio(len(r_set & j_set), len(r_set)),
            by_ring=M._by_ring_ratio(r_set, ring_of, j_set))
        out[f'rcr_cross__{GRANULARITY}__{rvar}__na'] = M._cross(
            r_set & j_set, ring_of,
            lambda x, _c=j_canon: _c.get(x, loc.OUTSIDE))
    out['matching'] = matching
    out['available'] = True
    return out


# ---------------------------------------------------------------------------
# pairing records with the legs of a measured run
# ---------------------------------------------------------------------------

def _leg_identity(leg_dir: str) -> dict:
    """A leg's (project, bug, tool, label), from its own `result.jsonl`,
    with the leg name as the fallback for the patch stem."""
    result = M.read_result(leg_dir)
    name = os.path.basename(leg_dir)
    m = M.LEG_RE.match(name)
    bug_id = result.get('bug_id')
    return {
        'leg': name,
        'leg_dir': os.path.abspath(leg_dir),
        'patch_stem': m.group('patch') if m else None,
        'arm': m.group('arm') if m else None,
        'project': result.get('project'),
        'bug_id': str(bug_id) if bug_id is not None else None,
        'apr_tool': result.get('apr_tool'),
        'label': result.get('label'),
    }


def pair_records_with_legs(records_path: str, run_dir: str) -> List[dict]:
    """One row per record, with the leg of `run_dir` it belongs to.

    The key is (project, bug_id, apr_tool, label) — the identity the
    baseline and the pipeline both record for a patch. When more than one
    leg carries that key (a bug with two patches from one tool on the same
    side), the patch file's stem breaks the tie, and a tie that stays
    unbroken is reported as `match: 'ambiguous'` with `leg_dir: None`
    rather than resolved by guessing.

    `match` is 'identity', 'patch_stem', 'ambiguous' or 'none'. Rows keep
    file order, so an unpaired record stays visible instead of vanishing."""
    legs = [_leg_identity(d) for d in M.leg_dirs(run_dir)]
    by_key: Dict[Tuple, List[dict]] = {}
    by_stem: Dict[str, List[dict]] = {}
    for leg in legs:
        key = (leg['project'], leg['bug_id'], leg['apr_tool'], leg['label'])
        by_key.setdefault(key, []).append(leg)
        if leg['patch_stem']:
            by_stem.setdefault(leg['patch_stem'], []).append(leg)

    rows = []
    for record in read_records(records_path):
        ident = record_identity(record)
        key = identity_key(record)
        cands = by_key.get(key) or []
        leg, how = None, 'none'
        if len(cands) == 1:
            leg, how = cands[0], 'identity'
        elif len(cands) > 1:
            stem_hit = [c for c in cands
                        if c['patch_stem'] == ident['patch_stem']]
            if len(stem_hit) == 1:
                leg, how = stem_hit[0], 'patch_stem'
            else:
                how = 'ambiguous'
        elif ident['patch_stem'] and len(
                by_stem.get(ident['patch_stem']) or []) == 1:
            # No identity match — a record whose result.jsonl lost a field,
            # for instance. The patch stem names the same patch, so it is
            # reported as its own weaker kind of match, never as 'identity'.
            leg, how = by_stem[ident['patch_stem']][0], 'patch_stem'
        rows.append({
            'key': key,
            'record': record,
            'match': how,
            'leg': leg['leg'] if leg else None,
            'leg_dir': leg['leg_dir'] if leg else None,
            'n_candidates': len(cands),
        })
    return rows


# ---------------------------------------------------------------------------
# the run-level entry point
# ---------------------------------------------------------------------------

def _pipeline_rcr(run_dir: str) -> Dict[str, dict]:
    """The pipeline's own RCR rows from `<run_dir>/metrics.jsonl`, by leg."""
    return {row.get('leg'): row for row in M.read_metrics(run_dir)
            if row.get('leg')}


def judge_rows(records_path: str, run_dir: str,
               cache_dirs=None) -> List[dict]:
    """One judge-RCR row per paired record, with the pipeline's RCR beside
    it.

    A record with no leg, or a leg with no `root_cause.json`, still gets a
    row: `available` is False and `reason` says which input was missing."""
    pipeline = _pipeline_rcr(run_dir)
    rows = []
    for pair in pair_records_with_legs(records_path, run_dir):
        record = pair['record']
        if not pair['leg_dir']:
            row = dict(record_identity(record), available=False,
                       reason=f"no leg in the run matched ({pair['match']})",
                       match=pair['match'], leg=None, leg_dir=None)
            rows.append(row)
            continue
        rc = os.path.join(pair['leg_dir'], M.MEASUREMENTS_DIR, M.F_ROOT_CAUSE)
        text, why = evidence_text_for(record, cache_dirs)
        row = rcr_for_judge(
            record, rc, evidence_text=text,
            evidence_source=('text' if text else 'facts'))
        row['evidence_recovery'] = why
        row['match'] = pair['match']
        row['leg'] = pair['leg']
        row['leg_dir'] = pair['leg_dir']
        peer = pipeline.get(pair['leg']) or {}
        row['pipeline'] = {
            M.metric_key('rcr', GRANULARITY, rvar, None):
                peer.get(M.metric_key('rcr', GRANULARITY, rvar, None))
            for rvar in R_VARIANTS}
        row['pipeline_sizes'] = {
            'P_method': (peer.get('sizes') or {}).get('P_method'),
            'P_method_by_ring': (peer.get('sizes') or {}).get(
                'P_method_by_ring'),
        }
        rows.append(row)
    return rows


def _fmt(ratio) -> str:
    if not isinstance(ratio, dict) or ratio.get('value') is None:
        return '   –  '
    return f"{ratio['value']:.2f}"


def _counts(ratio) -> str:
    if not isinstance(ratio, dict):
        return '-'
    return f"{ratio.get('num')}/{ratio.get('den')}"


def render_table(rows: List[dict]) -> str:
    """Per-patch RCR for the judge next to the pipeline's RCR, as text."""
    head = (f"{'patch':<38} {'src':<6} "
            + ' '.join(f'{v + " J/P":>12}' for v in R_VARIANTS)
            + f" {'|J|':>5} {'|P|':>5}")
    out = [head, '-' * len(head)]
    for row in rows:
        name = (row.get('patch_stem') or row.get('patch') or '?')[:38]
        if not row.get('available'):
            out.append(f"{name:<38} {'-':<6} {row.get('reason') or ''}")
            continue
        cells = []
        for rvar in R_VARIANTS:
            key = M.metric_key('rcr', GRANULARITY, rvar, None)
            j = _fmt(row.get(key))
            p = _fmt((row.get('pipeline') or {}).get(key))
            cells.append(f'{j}/{p}'.rjust(12))
        sizes = row.get('sizes') or {}
        pj = (row.get('pipeline_sizes') or {}).get('P_method')
        out.append(f"{name:<38} {row.get('evidence_source', '-'):<6} "
                   + ' '.join(cells)
                   + f" {sizes.get('J_method', '-'):>5}"
                   + f" {pj if pj is not None else '-':>5}")
    n_avail = sum(1 for r in rows if r.get('available'))
    n_text = sum(1 for r in rows if r.get('evidence_source') == 'text')
    out += ['',
            f'{len(rows)} record(s), {n_avail} measured, '
            f'{n_text} with the rendered evidence text recovered and '
            f'digest-verified.',
            'J = what the LLM judge was shown, P = what the pipeline was '
            'shown, both against the same R. Evaluation only.']
    for rvar in R_VARIANTS:
        key = M.metric_key('rcr', GRANULARITY, rvar, None)
        js = [r[key]['value'] for r in rows
              if r.get('available') and isinstance(r.get(key), dict)
              and r[key].get('value') is not None]
        peers = [(r.get('pipeline') or {}).get(key) for r in rows
                 if r.get('available')]
        ps = [p['value'] for p in peers
              if isinstance(p, dict) and p.get('value') is not None]
        out.append(
            f"  mean RCR {rvar}: judge "
            + (f'{sum(js) / len(js):.3f}' if js else '–')
            + f' (n={len(js)})   pipeline '
            + (f'{sum(ps) / len(ps):.3f}' if ps else '–')
            + f' (n={len(ps)})')
    return '\n'.join(out) + '\n'


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog='python -m java.measurements.judge_view',
        description='RCR for the one-shot LLM judge, next to the '
                    "pipeline's RCR from the same measured run. "
                    'Evaluation only: it writes nothing into the run.')
    p.add_argument('records', help="a baseline run's records.jsonl")
    p.add_argument('run_dir',
                   help='a measured run directory (holding metrics.jsonl '
                        'and per-leg measurements/root_cause.json)')
    p.add_argument('--out', default=None,
                   help='write one JSON row per record here '
                        '(e.g. judge_rcr.jsonl)')
    p.add_argument('--evidence_cache', action='append', default=None,
                   metavar='DIR',
                   help="the baseline's evidence cache directory (repeatable). "
                        'A cache entry is used only when its sha256 matches '
                        "the record's evidence_sha256; without one the judge "
                        'set is the seed ring alone.')
    return p


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)
    rows = judge_rows(args.records, args.run_dir, args.evidence_cache)
    sys.stdout.write(render_table(rows))
    if args.out:
        out = args.out
        if not os.path.isabs(out) and os.path.dirname(out) == '':
            out = os.path.join(args.run_dir, out)
        with open(out, 'w', encoding='utf-8') as fh:
            for row in rows:
                fh.write(json.dumps({k: v for k, v in row.items()
                                     if k != 'record'}, sort_keys=True) + '\n')
        sys.stdout.write(f'\nwrote {out}\n')
    return 0


if __name__ == '__main__':                                  # pragma: no cover
    raise SystemExit(main())
