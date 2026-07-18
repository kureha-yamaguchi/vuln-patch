"""R4 input-kind detection — decides which variation-menu entries a leg gets.

Design (see semantic-recall-brainstorm.md R4(b-EXACT)):
  Tier 1 DETERMINISTIC — read the entry-point signatures (touched methods
    + reachable API). Numeric/collection/datetime/date types and
    format/parse method pairs are settled here with zero model calls.
  Tier 2 ONE LLM CALL — only when a String parameter is left unresolved
    (program text? query? plain text? a name? — the TYPE cannot tell).
    A constrained classifier returns labels from the closed vocabulary.
  Tier 3 FAIL-SAFE — unknown/empty resolves to no kind, so nothing is
    injected rather than something wrong.

HARD INVARIANT — detection is ADVISORY FOR RELATION SELECTION ONLY. It
NEVER restricts what the fuzzer feeds or what inputs the harness builds.
Detecting `number` does NOT mean the fuzzer stops trying strings; the
fuzzer always explores the full byte space, and a method that takes a
String holding a number (NumberUtils.createNumber) is still fuzzed with
arbitrary strings. The detected kind only chooses which menu RELATIONS
are offered to the rule-writer as candidates; every candidate is then
condition-checked, screened on the buggy build, and judged. So a wrong
kind at worst offers a less-relevant candidate in a slot — it can never
narrow input generation or change a verdict. Any future caller that lets
a detected kind gate input construction is a bug.

The detected kinds drive variation_menu.entries_for_kinds (capped,
priority-ranked) and a fixed TEMPLATE sentence stated atop rule-writing.
This module does the detection and the sentence; it does NOT yet wire
into the synthesis prompt — that is R4's measurement point.
"""
import re
from typing import List, Optional, Set, Tuple

# ---- Tier 1 type vocabularies (simple names, matched case-sensitively) ----
_NUMERIC = {
    'int', 'long', 'short', 'byte', 'float', 'double',
    'Integer', 'Long', 'Short', 'Byte', 'Float', 'Double', 'Number',
    'BigInteger', 'BigDecimal', 'AtomicInteger', 'AtomicLong',
    'Complex', 'RealMatrix', 'RealVector', 'Fraction', 'BigFraction',
    'Dfp', 'Vector2D', 'Vector3D',
}
_COLLECTION = {
    'List', 'Set', 'Map', 'Collection', 'Iterable', 'Queue', 'Deque',
    'ArrayList', 'LinkedList', 'HashSet', 'TreeSet', 'LinkedHashSet',
    'HashMap', 'TreeMap', 'LinkedHashMap', 'Iterator', 'Enumeration',
    'SortedSet', 'SortedMap', 'NavigableMap', 'NavigableSet',
    'StrBuilder', 'StringBuilder', 'StringBuffer',
}
_DATETIME = {
    'Date', 'Calendar', 'Instant', 'LocalDate', 'LocalDateTime',
    'LocalTime', 'ZonedDateTime', 'OffsetDateTime', 'Duration', 'Period',
    'TimeZone', 'DateTime', 'DateTimeZone', 'DateMidnight', 'Partial',
    'YearMonth', 'MonthDay', 'Chronology', 'DateTimeFormatter',
    'ReadableInstant', 'ReadablePartial', 'ReadableDuration', 'Interval',
}
_STRINGY = {'String', 'CharSequence', 'char'}
_TYPE_REFLECT = {'Class', 'Method', 'Field', 'Constructor', 'Type',
                 'ParameterizedType', 'TypeVariable', 'Package', 'Member',
                 'AnnotatedElement', 'ClassLoader'}
_GEOMETRY = {'Rectangle2D', 'Point2D', 'Area', 'AffineTransform', 'Shape',
             'Range', 'Rectangle', 'Line2D', 'Ellipse2D', 'Path2D',
             'ValueAxis', 'Bounds', 'RectangularShape', 'Dimension2D'}

# format/parse method-name sides for the encode_decode_pair kind
_FORMAT_SIDE = re.compile(r'\b(format|to[A-Z]\w*|write|encode|serialize|'
                          r'print|toString|marshal)\w*', re.I)
_PARSE_SIDE = re.compile(r'\b(parse|from[A-Z]\w*|read|decode|deserialize|'
                         r'valueOf|unmarshal)\w*', re.I)

_TYPE_TOKEN = re.compile(r'\b([A-Za-z_][A-Za-z0-9_]*)\b(\s*\[\s*\])?')
_METHOD_NAME = re.compile(r'\b([a-z]\w*)\s*\(')

CLOSED_STRING_KINDS = ('program_text', 'plain_text', 'query_or_filter')

_TEMPLATE = {
    'number': 'numeric values',
    'collection': 'a collection of elements',
    'datetime': 'date-time values',
    'encode_decode_pair': 'a value with a matched format/parse pair',
    'program_text': 'program or markup source text',
    'plain_text': 'human-readable text',
    'query_or_filter': 'a search query or filter',
    'web_api': 'a web-API request',
    'security': 'a security-sensitive request',
    'type_or_reflection': 'Java types / reflection objects',
    'geometry': '2D geometry / bounds values',
}


class Detection:
    def __init__(self, kinds: List[str], string_ambiguous: bool,
                 evidence: dict):
        self.kinds = kinds                    # ordered, own-params first
        self.string_ambiguous = string_ambiguous
        self.evidence = evidence              # {kind: [why,...]}

    def sentence(self) -> str:
        if not self.kinds:
            return ''
        phrases = [_TEMPLATE.get(k, k) for k in self.kinds]
        joined = phrases[0] if len(phrases) == 1 else (
            ', '.join(phrases[:-1]) + ' and ' + phrases[-1])
        return f'The public entry points consume {joined}.'


def _types_in(signature: str) -> List[str]:
    """Simple type names appearing in a Java signature, array-flagged."""
    out = []
    # drop the method name and modifiers noise by scanning tokens; keep
    # any token that is a known type, plus mark arrays.
    for m in _TYPE_TOKEN.finditer(signature):
        tok, arr = m.group(1), bool(m.group(2))
        out.append(tok + ('[]' if arr else ''))
    return out


def detect(signatures: List[str],
           param_priority: Optional[List[str]] = None) -> Detection:
    """Deterministic (Tier 1) detection over method signatures.

    `signatures`: touched-method + reachable-API signatures (as the
    synthesis stage already has them). `param_priority`: signatures of
    the TOUCHED methods' own parameters, listed first so their kinds
    rank ahead of kinds only seen elsewhere (drives the injection cap).
    """
    ordered_sigs = (param_priority or []) + [
        s for s in signatures if s not in (param_priority or [])]
    kinds: List[str] = []
    evid: dict = {}
    string_seen = False
    array_of_string = False

    def add(kind, why):
        if kind not in kinds:
            kinds.append(kind)
        evid.setdefault(kind, [])
        if why not in evid[kind]:
            evid[kind].append(why)

    fmt_methods, parse_methods = [], []
    for sig in ordered_sigs:
        toks = _types_in(sig)
        base = {t.rstrip('[]') for t in toks}
        if base & _NUMERIC:
            add('number', f'numeric type in `{sig.strip()[:60]}`')
        if base & _COLLECTION or any(t.endswith('[]') for t in toks):
            add('collection', f'collection/array in `{sig.strip()[:60]}`')
        if base & _DATETIME:
            add('datetime', f'date/time type in `{sig.strip()[:60]}`')
        if base & _TYPE_REFLECT:
            add('type_or_reflection', f'type/reflection type in `{sig.strip()[:60]}`')
        if base & _GEOMETRY:
            add('geometry', f'geometry/bounds type in `{sig.strip()[:60]}`')
        if base & _STRINGY:
            string_seen = True
            if any(t == 'String[]' or t == 'CharSequence[]' for t in toks):
                array_of_string = True
        mm = _METHOD_NAME.search(sig)
        if mm:
            name = mm.group(1)
            if _FORMAT_SIDE.match(name):
                fmt_methods.append(name)
            if _PARSE_SIDE.match(name):
                parse_methods.append(name)

    if fmt_methods and parse_methods:
        add('encode_decode_pair',
            f'format-side {fmt_methods[0]} + parse-side {parse_methods[0]}')

    # A String[] is a collection of strings — resolves without the LLM.
    if array_of_string and 'collection' not in kinds:
        add('collection', 'String[] parameter (collection of strings)')

    # String present but no string-shaped kind resolved -> ambiguous.
    string_resolved = any(k in kinds for k in
                          ('program_text', 'plain_text', 'query_or_filter'))
    string_ambiguous = string_seen and not string_resolved and not array_of_string
    return Detection(kinds, string_ambiguous, evid)


# ------------------------- Tier 2: the one LLM call -------------------------

_CLASSIFY_SYSTEM = (
    "You label what KIND of data a Java method's String input represents, "
    "to pick metamorphic-testing relations. Reply with ONLY a JSON array of "
    "labels drawn from exactly this closed set: "
    '["program_text", "plain_text", "query_or_filter"]. '
    "program_text = source code / markup a parser or compiler consumes "
    "(JS, XML, a config grammar). query_or_filter = a search query or "
    "filter expression. plain_text = human-readable text (a name, message, "
    "label, description). If none clearly fits or you are unsure, reply []. "
    "Add one short reason per label after the array, on its own line. "
    "Do not invent labels outside the set."
)


def classify_string_kind(signatures: List[str], class_name: str,
                         package: str, class_javadoc: str,
                         example_calls: str, generator=None
                         ) -> Tuple[List[str], str]:
    """Tier 2: the single constrained classification call. Returns
    (labels intersected with the closed set, raw reply). Any parse
    failure or empty -> ([], reply) (fail-safe: no string-kind)."""
    prompt = (
        f"Class: {package}.{class_name}\n"
        f"Class javadoc (first lines): {class_javadoc[:500]}\n"
        f"Entry-point signatures:\n" + "\n".join(f"  {s}" for s in signatures[:12])
        + (f"\nExample calls from the failing test:\n{example_calls[:400]}"
           if example_calls else "")
        + "\n\nLabel the String input(s)."
    )
    if generator is None:
        from llm import HarnessGenerator
        generator = HarnessGenerator(temperature=0.0, top_p=1.0)
    reply = generator.generate([
        {"role": "system", "content": _CLASSIFY_SYSTEM},
        {"role": "user", "content": prompt},
    ])
    m = re.search(r'\[([^\]]*)\]', reply or '')
    labels: List[str] = []
    if m:
        for raw in re.findall(r'"([^"]+)"|\'([^\']+)\'|([a-z_]+)', m.group(1)):
            lab = next(x for x in raw if x)
            if lab in CLOSED_STRING_KINDS and lab not in labels:
                labels.append(lab)
    return labels, (reply or '')
