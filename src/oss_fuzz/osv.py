"""Select the most recent *public* CVE for an OSS-Fuzz project via OSV.

OSS-Fuzz's disclosed vulnerabilities are the source of truth in OSV (the
``google/oss-fuzz-vulns`` repo feeds it), each carrying OSV-computed
introduced/fixed git commits from automated bisection. We query OSV for the
project, keep only entries that (a) carry a CVE alias and (b) are already
public, and return the newest.

Two things are deliberately robust-not-clever here:

  * Reproducer availability. OSS-Fuzz testcases are embargoed until the bug
    is disclosed; even then OSV does not always embed a stable download URL.
    We surface any reference that looks like a testcase, but the pipeline
    treats the PoC as *optional*: it re-derives triggering harnesses from the
    fix diff and gates them on its own crash check, so a missing reproducer
    only costs us the pre-flight sanity reproduce, not the run.

  * The vulnerable commit. OSV gives the ``fixed`` commit; the last
    vulnerable state is its first parent, resolved later against a real
    clone (``ossfuzz.parent_commit``) rather than guessed here.

Network access is stdlib-only (urllib) so this module has no dependencies.
``select_from_records`` / ``CveTarget.from_osv`` are pure and unit-tested
offline; only ``OsvClient`` touches the network.
"""
from __future__ import annotations

import json
import urllib.request
from dataclasses import dataclass, field
from typing import List, Optional

import config

_CVE_PREFIX = "CVE-"


@dataclass
class CveTarget:
    """A single selected vulnerability, resolved enough to drive the run."""
    cve_id: str
    osv_id: str
    project: str                      # OSS-Fuzz project name
    language: Optional[str] = None    # 'c' / 'c++' (from project.yaml, filled later)
    main_repo: Optional[str] = None   # upstream git URL (from OSV or project.yaml)
    fixed_commit: Optional[str] = None
    introduced_commit: Optional[str] = None
    fuzz_target: Optional[str] = None   # harness that originally found it, if known
    sanitizer: Optional[str] = None
    reproducer_url: Optional[str] = None
    summary: str = ""
    published: str = ""
    references: List[str] = field(default_factory=list)

    @classmethod
    def from_osv(cls, project: str, record: dict) -> "CveTarget":
        """Parse one OSV record (dict) into a CveTarget. Pure; no I/O."""
        aliases = record.get("aliases", []) or []
        cve = next((a for a in aliases if a.startswith(_CVE_PREFIX)), None)

        main_repo = None
        introduced = None
        fixed = None
        for aff in record.get("affected", []) or []:
            pkg = aff.get("package", {}) or {}
            # OSS-Fuzz-native entries carry the upstream repo on the range.
            for rng in aff.get("ranges", []) or []:
                if rng.get("type") != "GIT":
                    continue
                main_repo = main_repo or rng.get("repo")
                for ev in rng.get("events", []) or []:
                    if "introduced" in ev and ev["introduced"] != "0":
                        introduced = introduced or ev["introduced"]
                    if "fixed" in ev:
                        fixed = fixed or ev["fixed"]
            main_repo = main_repo or pkg.get("purl")

        refs = [r.get("url") for r in record.get("references", []) or []
                if r.get("url")]
        reproducer = next(
            (u for u in refs
             if any(tok in u.lower()
                    for tok in ("testcase", "reproduce", "download"))),
            None,
        )

        ds = record.get("database_specific", {}) or {}
        return cls(
            cve_id=cve or "",
            osv_id=record.get("id", ""),
            project=project,
            main_repo=main_repo,
            fixed_commit=fixed,
            introduced_commit=introduced,
            fuzz_target=ds.get("fuzz_target"),
            sanitizer=ds.get("sanitizer"),
            reproducer_url=reproducer,
            summary=record.get("summary", "") or record.get("details", "")[:200],
            published=record.get("published", "") or record.get("modified", ""),
            references=refs,
        )

    @property
    def has_cve(self) -> bool:
        return self.cve_id.startswith(_CVE_PREFIX)

    @property
    def is_usable(self) -> bool:
        # We need a fix commit to define the vulnerable/patched boundary and a
        # repo to check out. Everything else is best-effort.
        return bool(self.fixed_commit and self.main_repo)


def _is_public(record: dict) -> bool:
    """OSV only imports OSS-Fuzz bugs after disclosure, so presence in a
    query result already implies public. We still drop explicitly withdrawn
    entries."""
    return not record.get("withdrawn")


def select_from_records(project: str, records: List[dict]) -> Optional[CveTarget]:
    """Newest public, CVE-bearing, usable target from a list of OSV records.

    Pure function (no network) so the selection policy is unit-testable with
    fixtures. 'Newest' sorts on published/modified date descending.
    """
    targets = [
        CveTarget.from_osv(project, r)
        for r in records if _is_public(r)
    ]
    usable = [t for t in targets if t.has_cve and t.is_usable]
    usable.sort(key=lambda t: t.published, reverse=True)
    return usable[0] if usable else None


class OsvClient:
    """Thin OSV REST client (stdlib urllib only)."""

    def __init__(self, api_url: str = None, timeout: float = 30.0):
        self.api_url = (api_url or config.OSV_API_URL).rstrip("/")
        self.timeout = timeout

    def _post(self, path: str, payload: dict) -> dict:
        data = json.dumps(payload).encode()
        req = urllib.request.Request(
            f"{self.api_url}{path}", data=data,
            headers={"Content-Type": "application/json"},
        )
        with urllib.request.urlopen(req, timeout=self.timeout) as resp:
            return json.loads(resp.read().decode())

    def _get(self, path: str) -> dict:
        req = urllib.request.Request(f"{self.api_url}{path}")
        with urllib.request.urlopen(req, timeout=self.timeout) as resp:
            return json.loads(resp.read().decode())

    def query_project(self, project: str) -> List[dict]:
        """All OSV records for an OSS-Fuzz project (summaries), hydrated to
        full records so aliases/ranges/references are populated."""
        res = self._post("/query", {
            "package": {"ecosystem": "OSS-Fuzz", "name": project},
        })
        vulns = res.get("vulns", []) or []
        # /query may already return full records; if an entry is a stub
        # (id only), hydrate it via /vulns/{id}.
        full = []
        for v in vulns:
            if "affected" in v:
                full.append(v)
            else:
                try:
                    full.append(self._get(f"/vulns/{v['id']}"))
                except Exception:
                    full.append(v)
        return full

    def most_recent_public_cve(self, project: str) -> Optional[CveTarget]:
        return select_from_records(project, self.query_project(project))
