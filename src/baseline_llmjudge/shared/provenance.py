"""Which commit produced a record.

Every record carries the answer, so a number stays traceable to the code that
made it. `GITSHA` overrides the lookup, for a run inside a container that has
no git directory.
"""
import os
import subprocess


def git_sha() -> str:
    env = os.environ.get('GITSHA')
    if env:
        return env
    try:
        return subprocess.run(['git', 'rev-parse', 'HEAD'],
                              capture_output=True, text=True,
                              timeout=10).stdout.strip() or 'unknown'
    except Exception:
        return 'unknown'
