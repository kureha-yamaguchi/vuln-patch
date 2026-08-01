"""Rule 8's two directions: the record is not the thing.

Cycle 8 met both, five times:

* INFLATION x4 — a grep counted text that merely MENTIONED what it measured
  (the judge prompt's `VERDICT: SOUND | UNSOUND` template; the codegen prompt
  naming `expectedRaw=`; the Java source that constructs an alarm message).
* DEFLATION x1 — a count read TRUNCATED alarm records and scored content after
  the cut as absent, reporting 8.4's compliance as 38% when source-level
  counting showed 100%.

`count_in_fired_alarms_only` guards both: it scopes to runtime alarm records
(inflation) and refuses to answer on a truncated one (deflation).
"""
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))

from java.parsing.java_source import (  # noqa: E402
    TruncatedRecord, count_in_fired_alarms_only, normalized_without_raw)

ALARM = 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'


def test_prompt_text_mentioning_the_needle_is_not_counted():
    """The inflation direction. The codegen prompt names `expectedRaw=`; that is
    an instruction, not an observation."""
    text = ('instruction: every normalizing check must emit expectedRaw=\n'
            f'{ALARM}: [oracle:x] semantic mismatch: expectedRaw=abc')
    assert text.count('expectedRaw=') == 2
    assert count_in_fired_alarms_only(text, 'expectedRaw=') == 1


def test_source_constructing_the_message_is_not_counted():
    """`throw new ...("expectedRaw=" + v)` is code, not a fired alarm."""
    text = (f'throw new {ALARM}("[oracle:x] expectedRaw=" + v);\n'
            f'{ALARM}: [oracle:x] semantic mismatch: expectedRaw=abc')
    assert count_in_fired_alarms_only(text, 'expectedRaw=') == 1


def test_a_truncated_record_is_REFUSED_not_undercounted():
    """The deflation direction, and the reason it is a raise rather than a
    smaller number: a silent undercount is indistinguishable from real absence.
    That is exactly how 38% was reported for a 100% rate."""
    text = f'{ALARM}: [oracle:x] semantic mismatch: expectedNormalized=a actualRaw=b…'
    with pytest.raises(TruncatedRecord):
        count_in_fired_alarms_only(text, 'Raw=')


def test_the_opt_out_exists_but_must_be_asked_for():
    text = f'{ALARM}: [oracle:x] expectedNormalized=a actualRaw=b…'
    assert count_in_fired_alarms_only(text, 'Raw=', on_truncation='count') == 1


def test_complete_records_count_normally():
    text = f'{ALARM}: [oracle:x] expectedRaw=1 actualRaw=2'
    assert count_in_fired_alarms_only(text, 'Raw=') == 2


# --- the 8.4 lint ---------------------------------------------------------

NORMALIZING = f'''public class FuzzHarness {{
  static void f(String a, String b) {{
    String na = a.replaceAll("\\\\s+", ""), nb = b.replaceAll("\\\\s+", "");
    if (!na.equals(nb)) throw new {ALARM}(
        "[oracle:x] semantic mismatch: expectedNormalized=" + na
        + " actualNormalized=" + nb);
  }}
}}'''


def test_lint_flags_a_normalizing_check_that_omits_raw():
    r = normalized_without_raw(NORMALIZING)
    assert r and 'expectedRaw=' in r and 'RECORD raw' in r


def test_lint_spares_the_compliant_form():
    ok = NORMALIZING.replace(
        '+ " actualNormalized=" + nb);',
        '+ " actualNormalized=" + nb + " expectedRaw=" + a + " actualRaw=" + b);')
    assert normalized_without_raw(ok) is None


def test_lint_ignores_checks_that_never_normalize():
    """Nothing normalised means nothing owed — the lint must not demand Raw keys
    of a numeric check."""
    plain = f'''public class FuzzHarness {{
      static void f(int a, int b) {{
        if (a != b) throw new {ALARM}("[oracle:x] semantic mismatch: a=" + a);
      }}
    }}'''
    assert normalized_without_raw(plain) is None


# --- the lint WIRED at acceptance (rule 15's corollary) -------------------

def test_the_lint_is_actually_called_by_the_campaign():
    """A detector nothing calls guards nothing. This was named as rule-15's
    shape before it could bite, and wired before the comparison was built so
    the comparison is developed against the contract it will run under."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    assert 'normalized_without_raw(source)' in src
    assert 'gate 0c2' in src


def test_the_gate_runs_before_acceptance():
    """Ordering, checked rather than assumed — the 8.8 lesson."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    gate = src.find('normalized_without_raw(source)')
    accept = src.find('# --- accepted ---')
    assert gate != -1 and accept != -1 and gate < accept


def test_the_gate_tells_the_model_to_KEEP_the_comparison():
    """The failure mode to avoid: a model reading 'record raw' as 'compare raw'
    would undo the normalization the prompt correctly requires."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    seg = src[src.find('gate 0c2'):src.find('gate 0d')]
    assert 'compare' in seg.lower() and 'NORMALIZED' in seg


def test_lint_fires_on_a_REAL_archived_harness_stripped_of_its_raw_keys():
    """The corollary: exercise the gate on a population where it CAN fire.

    A synthetic-only test would leave open whether the lint copes with real
    generated source. This takes an actual compliant harness from
    c84_20260801_174840, removes its Raw keys, and requires the lint to notice.
    """
    import glob
    import re
    traces = glob.glob(str(Path(__file__).resolve().parents[1]
                           / 'runs-archive' / 'runs' / 'c84_*' / '*'
                           / 'trace.md'))
    if not traces:
        pytest.skip('compliance-smoke archive not present')
    real = None
    for t in traces:
        txt = open(t, errors='ignore').read()
        for step in re.split(r'\n(?=## \[\d+\])', txt):
            if 'harness generation' not in step.split('\n')[0]:
                continue
            m = re.search(r'▸ Output.*?```(?:java)?\s*(.*?)```', step, re.S)
            if m and 'expectedRaw=' in m.group(1) and 'replaceAll' in m.group(1):
                real = m.group(1)
                break
        if real:
            break
    assert real, 'no compliant normalizing harness found in the archive'
    # as generated: compliant, so the lint must stay silent
    assert normalized_without_raw(real) is None
    # strip the Raw keys: the lint must now object
    stripped = (real.replace('expectedRaw=', 'xxRemovedxx=')
                    .replace('actualRaw=', 'yyRemovedyy='))
    assert normalized_without_raw(stripped), \
        'lint missed a real harness with its Raw keys removed'
