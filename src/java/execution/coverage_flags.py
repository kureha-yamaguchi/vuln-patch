"""Jazzer flags for coverage collection — pipeline-side, MEASUREMENT ONLY.

Lives in the pipeline package (not in java.measurements) so that the rule
"the pipeline never imports the measurement layer" holds with no exception:
the measurement layer imports THIS module, never the other way round.
"""
from typing import List

def jazzer_coverage_args(dump_path: str, include_glob: str) -> List[str]:
    """The two Jazzer flags that make a fuzzing run emit JaCoCo data.

    `--coverage_dump` writes a `.exec` execution-data file when the run
    ends; `--instrumentation_includes` restricts instrumentation to the
    library under test, which both speeds the run up and keeps JDK and
    Jazzer classes out of the report. `include_glob` is Jazzer's own
    glob syntax, e.g. 'org.jfree.**'.

    Pure function: it builds the argument list and nothing else, so a
    caller can append it to a command without this module ever touching
    a subprocess. Jazzer's default command line is unchanged unless a
    caller explicitly adds these.
    """
    return [f'--coverage_dump={dump_path}',
            f'--instrumentation_includes={include_glob}']
