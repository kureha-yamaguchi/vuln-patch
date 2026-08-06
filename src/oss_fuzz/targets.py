"""Find which OSS-Fuzz projects are actually viable targets for this run.

Everything else in this package assumes you already know the project name.
That is the missing half of *directing* the pipeline at OSS-Fuzz: of the ~1300
projects in a checkout only ~590 are C/C++, and only some of those have a
public CVE recent enough to be worth a variant hunt. Guessing a name and
discovering the mismatch after a clone + Docker image build + LLM budget is
the expensive way to find out.

A candidate has to clear two independent filters:

  1. **Local, free** — ``project.yaml`` says C/C++, builds with libFuzzer,
     supports the requested sanitizer, and names a ``main_repo``
     (``OssFuzz.check_support``). One file read per project, no network.

  2. **Remote, paid** — OSV has a public entry for the project with a ``fixed``
     commit, i.e. a fix boundary we can diff (``osv.select_from_records``). One
     HTTP request per project, so filter 1 runs first and 2 only on survivors.
     Note this does *not* require a CVE alias by default: OSS-Fuzz OSV records
     do not carry one (see ``select_from_records``), so requiring it would
     empty the candidate list entirely.

``fetch`` is injectable so the selection policy is unit-testable offline with
fixture records; pass nothing and it uses the real ``OsvClient``.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, List, Optional

from oss_fuzz.osv import CveTarget, select_from_records
from oss_fuzz.ossfuzz import OssFuzz


@dataclass
class Candidate:
    """A project + the CVE that makes it worth running, ready for --project."""
    project: str
    language: str
    main_repo: Optional[str]
    target: CveTarget

    @property
    def cve_id(self) -> str:
        return self.target.cve_id

    @property
    def published(self) -> str:
        return self.target.published

    def as_dict(self) -> dict:
        return {
            "project": self.project,
            "language": self.language,
            "main_repo": self.main_repo,
            "cve": self.cve_id,
            "osv_id": self.target.osv_id,
            "published": self.published,
            "fixed_commit": self.target.fixed_commit,
            "fuzz_target": self.target.fuzz_target,
            "sanitizer": self.target.sanitizer,
        }


def find_candidates(oss_fuzz: OssFuzz,
                    sanitizer: str,
                    projects: Optional[List[str]] = None,
                    limit: Optional[int] = None,
                    max_projects: Optional[int] = None,
                    fetch: Optional[Callable[[str], List[dict]]] = None,
                    require_cve: bool = False,
                    verbose: bool = False) -> List[Candidate]:
    """Viable (project, CVE) pairs, newest CVE first.

    ``limit`` stops the OSV sweep once that many candidates are found — the
    result is then the newest-first ordering *of what was found*, not of all
    projects in the checkout (a global ranking would mean ~590 HTTP requests).
    ``max_projects`` caps how many projects are probed at all.
    """
    if fetch is None:
        from oss_fuzz.osv import OsvClient
        fetch = OsvClient().query_project

    names = projects if projects is not None else oss_fuzz.list_projects(
        native_only=True)
    if max_projects is not None:
        names = names[:max_projects]

    found: List[Candidate] = []
    for name in names:
        sup = oss_fuzz.check_support(name, sanitizer)
        if not sup.supported:
            if verbose:
                print(f"  skip {name}: {sup.reasons[0]}")
            continue
        try:
            records = fetch(name)
        except Exception as exc:                      # network/OSV hiccup
            if verbose:
                print(f"  skip {name}: OSV query failed ({exc})")
            continue
        target = select_from_records(name, records, require_cve=require_cve)
        if target is None:
            if verbose:
                what = "CVE" if require_cve else "disclosed bug"
                print(f"  skip {name}: no public {what} with a fix commit")
            continue
        # OSV may carry the repo when project.yaml does not, and vice versa.
        target.main_repo = target.main_repo or sup.main_repo
        target.language = target.language or sup.language
        found.append(Candidate(project=name, language=sup.language or "",
                               main_repo=target.main_repo, target=target))
        if verbose:
            print(f"  ok   {name}: {target.cve_id} ({target.published[:10]})")
        if limit is not None and len(found) >= limit:
            break

    found.sort(key=lambda c: c.published, reverse=True)
    return found
