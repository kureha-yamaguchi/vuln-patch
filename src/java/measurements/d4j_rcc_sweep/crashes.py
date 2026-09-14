"""C — the crashes a harness set produced, and where each one happened.

CSM asks a different question from the other four metrics. Those are about
REACH: did the harness set run the right code? CSM is about the FINDING: of
the crashes the set reported, how many landed in R-hat? A harness set that
reaches the root-cause region and then only ever crashes somewhere else has
high RCC and low CSM, and the two numbers together say what one cannot.

One crash is one Jazzer finding. Jazzer prints each new finding under a
``== Java Exception:`` banner and deduplicates repeats, so the banners in a
measurement run's output count the DISTINCT crashes, not the executions.

`site(c)` is the method where the crash really happened, and two rules find
it.

  1. Follow the cause chain to its end. A harness that catches a library
     throwable and rethrows it as its own oracle alarm puts ITSELF at the
     top of the trace. The deepest ``Caused by:`` is the library fault that
     started it, and that is the site the metric means.
  2. Skip the harness. The harness class sits in the project package, so a
     plain "first project frame" rule would name the harness on any crash
     the harness itself throws. Its class names come from the run record.

A frame gives a class, a method name and a line but no parameter types, so
the site is resolved against the JaCoCo report by line, exactly as
`d4j_rcc_sweep.reached.reached_from_stack` resolves a frame.

A crash can end with no site, and the two ways it happens are different.

  * NO FRAME. The trace names the harness and nothing else. The harness's
    metamorphic oracle fired on a wrong VALUE, so no library method threw
    and there is no site to name. This is a normal outcome, not a fault in
    the parser.
  * UNRESOLVED. A library frame is there, but no method in the report owns
    its line. That is a plumbing failure and it is counted apart.

Neither counts in CSM's numerator, and both stay in its denominator, because
a crash we cannot place is not a crash we may claim.

One more thing CSM does not do: it does not deduplicate across harnesses.
Jazzer deduplicates within one process, so two harnesses that find the same
fault report it twice. CSM is therefore a share of REPORTS, and that is the
quantity a person reading the findings would work through.

MEASUREMENT ONLY.
"""
import re
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Set, Tuple

from java.measurements.d4j_rcc_sweep import reached
from java.measurements.d4j_rcc_sweep.keys import MethodKey

# Jazzer's finding banner, with the throwable that caused it.
_BANNER = re.compile(r'^==\s*Java Exception:\s*([\w.$]+)', re.MULTILINE)
# The start of the last link in a `Caused by:` chain.
_CAUSE = re.compile(r'^\s*Caused by:\s*([\w.$]+)', re.MULTILINE)

_NOT_PROJECT = ('java.', 'javax.', 'jdk.', 'sun.', 'junit.', 'org.junit.',
                'com.code_intelligence.jazzer')


@dataclass
class Crash:
    """One Jazzer finding and the method it happened in."""
    exception: str                              # the throwable's class
    frame: Optional[Tuple[str, str, int]] = None   # class, method, line
    site: Optional[MethodKey] = None            # None when unresolved

    @property
    def resolved(self) -> bool:
        return self.site is not None


def crash_blocks(text: str) -> List[Tuple[str, str]]:
    """One (exception type, block text) per finding, in output order."""
    banners = list(_BANNER.finditer(text or ''))
    blocks = []
    for index, match in enumerate(banners):
        end = banners[index + 1].start() if index + 1 < len(banners) \
            else len(text)
        blocks.append((match.group(1), text[match.start():end]))
    return blocks


def root_cause_text(block: str) -> str:
    """The part of a block that describes the deepest cause. Rule 1."""
    causes = list(_CAUSE.finditer(block))
    return block[causes[-1].start():] if causes else block


def site_frame(block: str,
               harness_classes: Iterable[str]) -> Optional[Tuple[str, str, int]]:
    """The frame a crash's site sits in. Rules 1 and 2."""
    skip = {name.replace('$', '.') for name in harness_classes if name}
    for cls, method, line in reached._FRAME_RE.findall(root_cause_text(block)):
        cls = cls.replace('$', '.')
        if cls.startswith(_NOT_PROJECT) or cls in skip:
            continue
        return (cls, method, int(line))
    return None


def crashes(report_path: str, text: str,
            harness_classes: Iterable[str] = ()) -> List[Crash]:
    """C for one harness set, with every site resolved against the report."""
    blocks = crash_blocks(text)
    if not blocks:
        return []
    owners: Dict[MethodKey, Set[int]] = reached.method_lines(report_path)
    found = []
    for exception, block in blocks:
        frame = site_frame(block, harness_classes)
        site = reached.resolve_frame(frame, owners) if frame else None
        found.append(Crash(exception=exception, frame=frame, site=site))
    return found
