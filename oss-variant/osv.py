"""Query OSV for the most recent public, CVE-tagged OSS-Fuzz vulnerability.

OSV only lists disclosed (public) vulnerabilities, so anything returned here is
already public. Each OSS-Fuzz entry carries a GIT range with the fixing commit,
bisected by OSV; the vulnerable version is simply its parent (``<fixed>^``).
"""
from __future__ import annotations

import json
import urllib.request
from dataclasses import dataclass

OSV_QUERY = "https://api.osv.dev/v1/query"


@dataclass
class Case:
    project: str
    osv_id: str
    cves: list
    repo: str
    fixed: str
    introduced: str | None
    modified: str
    report_url: str | None
    details: str


def _query(project: str) -> list:
    vulns: list = []
    token = None
    while True:
        body = {"package": {"ecosystem": "OSS-Fuzz", "name": project}}
        if token:
            body["page_token"] = token
        req = urllib.request.Request(
            OSV_QUERY,
            data=json.dumps(body).encode(),
            headers={"Content-Type": "application/json"},
        )
        with urllib.request.urlopen(req, timeout=60) as r:
            data = json.load(r)
        vulns.extend(data.get("vulns", []))
        token = data.get("next_page_token")
        if not token:
            return vulns


def _git_range(v: dict):
    """Return (repo, introduced, fixed) from the first GIT range that has a fix."""
    for affected in v.get("affected", []):
        for rng in affected.get("ranges", []):
            if rng.get("type") != "GIT":
                continue
            repo = rng.get("repo")
            introduced = fixed = None
            for event in rng.get("events", []):
                introduced = event.get("introduced", introduced)
                fixed = event.get("fixed", fixed)
            if repo and fixed:
                return repo, introduced, fixed
    return None


def _report_url(v: dict):
    for ref in v.get("references", []):
        if ref.get("type") in ("REPORT", "WEB", "ADVISORY"):
            return ref.get("url")
    refs = v.get("references")
    return refs[0].get("url") if refs else None


def latest_case(project: str, require_cve: bool = True) -> "Case | None":
    """Most recently modified public vuln for ``project`` that has a fix commit
    (and, by default, a CVE alias)."""
    best = None  # (modified_key, Case)
    for v in _query(project):
        cves = [a for a in v.get("aliases", []) if a.upper().startswith("CVE-")]
        if require_cve and not cves:
            continue
        rng = _git_range(v)
        if not rng:
            continue
        repo, introduced, fixed = rng
        key = v.get("modified") or v.get("published") or ""
        if best is None or key > best[0]:
            best = (
                key,
                Case(
                    project=project,
                    osv_id=v.get("id", ""),
                    cves=cves,
                    repo=repo,
                    fixed=fixed,
                    introduced=introduced,
                    modified=key,
                    report_url=_report_url(v),
                    details=v.get("details", ""),
                ),
            )
    return best[1] if best else None
