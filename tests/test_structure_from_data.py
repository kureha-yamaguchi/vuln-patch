"""Structure-from-data: the generation directive and its screen-side tooth.

The failure this closes: a check whose PROPERTY depends on the receiver /
container state (a rejection contract, an index/lookup contract, a
size/emptiness contract) can only discriminate a relocated or conditional
guard if the fuzzer actually reaches the container SHAPES where that guard
misbehaves. Fuzzing the labels and values inside a fixed shape is cosmetic:
every iteration rebuilds the same container, so a patch that only misbehaves
for a different shape (a hole, an empty container, a different install index)
is unreachable and the check is silent on BOTH builds by construction.

Two halves, both tested here, offline:

  * PART A — the synthesis directive (java.relations.relation_synth) that
    tells the generator to draw structure from `data`. Asserted present and
    dataset-neutral (no leg vocabulary).
  * PART B — java.parsing.java_source.constant_receiver_state, the screen-side
    lint that flags a receiver-state check built entirely from compile-time
    constants, exercised on REAL archived checks plus synthetic minimal cases.

Fixtures are verbatim relation checks from the archived night20 traces
(tests/fixtures/structure_from_data.json).
"""
import json
import os

from java.parsing.java_source import constant_receiver_state
from java.relations import relation_synth

_FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "fixtures", "structure_from_data.json")


def _load():
    with open(_FIXTURES) as fh:
        return json.load(fh)


# --------------------------------------------------------------------------
# PART B — the lint, on REAL archived checks.
# --------------------------------------------------------------------------

def test_real_constant_structure_checks_are_flagged():
    """The checks that actually reached the harness build a container with
    literal install indices and fuzz only the label strings."""
    cases = _load()["constant_structure"]
    assert cases, "fixture lost its constant-structure cases"
    for case in cases:
        reason = constant_receiver_state(case["check"])
        assert reason, f'{case["name"]} should be flagged'
        assert "receiver-state probe" in reason
        assert "STRUCTURE is compile-time constant" in reason


def test_real_constant_structure_reason_names_what_was_constant():
    """The reason must be actionable: it names the probe and the construction
    sites whose shape is literal."""
    case = _load()["constant_structure"][0]
    reason = constant_receiver_state(case["check"])
    assert "getRangeAxisIndex" in reason        # the probe
    assert "new CategoryPlot" in reason         # the constant construction
    assert "consumeAsciiString" in reason       # the cosmetic draw it DID make
    assert "index, a count" in reason           # what was missing


def test_real_structure_from_data_checks_are_not_flagged():
    """Same APIs, same leg in one case — but the install index / element count
    comes from `data`, so the check CAN reach other shapes."""
    cases = _load()["structure_from_data"]
    assert cases, "fixture lost its structure-from-data cases"
    for case in cases:
        assert constant_receiver_state(case["check"]) is None, (
            f'{case["name"]} draws structure from data and must not be '
            f'flagged')


def test_fixtures_are_real_archived_sources():
    """Guard against the fixtures silently degrading into hand-written toys."""
    data = _load()
    for case in data["constant_structure"] + data["structure_from_data"]:
        assert case["leg"].startswith(("0", "1", "2"))
        assert "data." in case["check"]
        assert "violated" in case["check"]


# --------------------------------------------------------------------------
# PART B — the lint, on synthetic minimal cases (each isolates one condition).
# --------------------------------------------------------------------------

_FUZZED_INDEX = """
java.util.List<String> box = new java.util.ArrayList<String>();
int n = data.consumeInt(0, 4);
int found;
try {
    for (int i = 0; i < n; i++) { box.add("e" + i); }
    found = box.indexOf("absent");
} catch (Exception e) { return; }
if (found != -1) throw new RuntimeException("relation r violated: " + found);
"""

_CONSTANT_STRUCTURE_LOOKUP = """
java.util.List<String> box = new java.util.ArrayList<String>();
int found;
try {
    box.add("a");
    box.add("b");
    found = box.indexOf("absent");
} catch (Exception e) { return; }
if (found != -1) throw new RuntimeException("relation r violated: " + found);
"""

_CONSTANT_STRUCTURE_REJECTION = """
java.util.List<String> box = new java.util.ArrayList<String>();
boolean violated = false;
String outcome = "";
box.add("a");
try {
    box.get(7);
    violated = true;
    outcome = "completed normally";
} catch (IndexOutOfBoundsException ok) {
} catch (Throwable t) {
    violated = true;
    outcome = "wrong exception " + t.getClass().getName();
}
if (violated) throw new RuntimeException("relation r violated: " + outcome);
"""

# Not receiver-state-dependent: a closed-form value property. Constant
# construction is fine here — condition (1) fails, so no flag.
_CONSTANT_BUT_NOT_RECEIVER_STATE = """
java.math.BigDecimal d = new java.math.BigDecimal("2.5");
double a;
double b;
try {
    a = d.doubleValue() * 2.0;
    b = d.add(d).doubleValue();
} catch (Exception e) { return; }
if (Math.abs(a - b) > 1e-9) throw new RuntimeException("relation r violated: "
    + a + " vs " + b);
"""


def test_synthetic_fuzz_derived_index_not_flagged():
    assert constant_receiver_state(_FUZZED_INDEX) is None


def test_synthetic_constant_structure_lookup_flagged():
    reason = constant_receiver_state(_CONSTANT_STRUCTURE_LOOKUP)
    assert reason and "indexOf" in reason


def test_synthetic_constant_structure_rejection_flagged():
    reason = constant_receiver_state(_CONSTANT_STRUCTURE_REJECTION)
    assert reason, "a rejection contract on a constant container must flag"


def test_synthetic_non_receiver_state_not_flagged():
    """Condition (1) fails — a value/formula property is not about the
    container's shape, so a constant receiver is not a blind spot."""
    assert constant_receiver_state(_CONSTANT_BUT_NOT_RECEIVER_STATE) is None


def test_lint_is_conservative_on_partial_structural_fuzz():
    """A single consume* value reaching ANY structural position silences the
    lint — the rule is 'flag only when ZERO fuzz reaches structure'."""
    src = _CONSTANT_STRUCTURE_LOOKUP.replace(
        'box.add("b");', 'box.add("b" + data.consumeAsciiString(2));')
    assert constant_receiver_state(src), (
        "a fuzzed LABEL is cosmetic and must not silence the lint")
    src2 = _CONSTANT_STRUCTURE_LOOKUP.replace(
        'box.add("b");', 'box.add(data.consumeInt(0, 3), "b");')
    assert constant_receiver_state(src2) is None, (
        "a fuzzed INSTALL INDEX is structural and must silence the lint")


def test_lint_returns_none_on_empty_and_garbage():
    for src in ("", None, "   ", "not java at all"):
        assert constant_receiver_state(src) is None


# --------------------------------------------------------------------------
# PART A — the synthesis directive.
# --------------------------------------------------------------------------

def _directive():
    text = relation_synth._INSTRUCTIONS
    start = text.index("STANDING STRATEGY — STRUCTURE FROM DATA")
    end = text.index("\n", text.index("structural blind", start))
    return text[start:end]


def test_directive_present_in_synthesis_instructions():
    block = _directive()
    assert "must be drawn from" in block
    assert "`data`" in block
    assert "compile-time constants" in block


def test_directive_names_the_structural_dimensions():
    block = _directive().lower()
    for word in ("how many", "indices", "gaps", "empty", "mutated",
                 "loop bound"):
        assert word in block, f"directive lost the '{word}' dimension"


def test_directive_names_the_receiver_state_families():
    block = _directive().lower()
    for word in ("rejection", "lookup", "size"):
        assert word in block


def test_directive_calls_label_fuzzing_cosmetic():
    assert "COSMETIC" in _directive()


def test_directive_is_dataset_neutral():
    """No bug names, no leg vocabulary, no worked example from any leg."""
    block = _directive().lower()
    for banned in ("chart", "axis", "jfreechart", "plot", "closure", "lang-",
                   "math-", "commons", "jfree"):
        assert banned not in block, f"directive leaked '{banned}'"


def test_directive_sits_with_the_other_standing_strategies():
    text = relation_synth._INSTRUCTIONS
    assert (text.index("REJECTION INDEPENDENCE")
            < text.index("STRUCTURE FROM DATA")
            < text.index("COVERAGE REQUIREMENT"))


def test_directive_is_in_the_selection_half_not_the_output_spec():
    """_OUTPUT_SPEC is reused verbatim by the focused passes; the selection
    guidance must not leak into it."""
    assert "STRUCTURE FROM DATA" not in relation_synth._OUTPUT_SPEC


# --------------------------------------------------------------------------
# Wiring: the screen DEMOTES (keeps + records), never drops.
# --------------------------------------------------------------------------

def test_screen_demotes_rather_than_drops():
    import inspect

    from java.relations import relation_screen

    src = inspect.getsource(relation_screen.screen_relations)
    i = src.index("conststate = constant_receiver_state(")
    window = src[i:src.index("cls = f'RelScreen", i)]
    assert "screen_demotion" in window
    assert "DEMOTED" in window
    # The other static lints all `_mark(..., 'dropped', ...)` + `continue`
    # right after their print; this one must do neither.
    assert "dropped" not in window
    assert "continue" not in window


def test_demotion_suffix_is_appended_to_the_screen_note():
    """_set_note keeps the bucket note's PREFIX intact (run.py filters on
    `startswith('INVERTED-SUSPECT')`) and appends the demotion."""
    import inspect

    from java.relations import relation_screen

    src = inspect.getsource(relation_screen.screen_relations)
    assert "note + demotion" in src
    assert src.count("_set_note(rel, note)") >= 4
