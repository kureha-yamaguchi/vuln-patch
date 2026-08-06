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
import re
import urllib.request
from dataclasses import dataclass, field
from typing import List, Optional

import config

_CVE_PREFIX = "CVE-"

# OSS-Fuzz OSV ``details`` is prose wrapping a fenced block, e.g.
#
#     OSS-Fuzz report: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=24925
#     ```
#     Crash type: Heap-use-after-free READ 4
#     Crash state:
#     xmlXIncludeIncludeNode
#     xmlXIncludeDoProcess
#     xmlXIncludeLoadFallback
#     ```
_CRASH_TYPE_RE = re.compile(r"^Crash type:\s*(.+?)\s*$", re.MULTILINE)
_CRASH_STATE_RE = re.compile(r"^Crash state:\s*\n(.*?)(?:\n\s*```|\Z)",
                             re.MULTILINE | re.DOTALL)
_REPORT_RE = re.compile(r"(https?://\S*oss-fuzz\S*?id=\d+)")
_DETAILS_TARGET_RE = re.compile(
    r"[Ff]uzz(?:er|[ _]target)[:\s]+([A-Za-z0-9_.-]+)")

# Crash types only one sanitizer can report. Used to pick the right --sanitizer
# when the record does not say: building with 'address' for a UBSan-only bug
# yields a harness that compiles and never triggers, which the campaign would
# then (correctly but expensively) reject over and over.
_UBSAN_HINTS = ("undefined", "integer-overflow", "shift", "misaligned",
                "invalid-bool-value", "float-cast-overflow",
                "implicit-integer-sign-change", "index out of bounds",
                "signed-integer-overflow", "division by zero")
_MSAN_HINTS = ("use-of-uninitialized-value", "uninitialized")


def _parse_details(details: str):
    """Pull (crash_type, crash_state frames, report url, fuzz target) out of an
    OSS-Fuzz OSV ``details`` blob. Pure string work; any field may be absent."""
    if not details:
        return None, [], None, None
    m = _CRASH_TYPE_RE.search(details)
    crash_type = m.group(1).strip() if m else None

    frames: List[str] = []
    ms = _CRASH_STATE_RE.search(details)
    if ms:
        for line in ms.group(1).splitlines():
            frame = line.strip().strip("`").strip()
            # Frames are bare symbol names; stop at prose/fence noise.
            if not frame or frame.startswith("Crash "):
                continue
            frames.append(frame)

    mr = _REPORT_RE.search(details)
    report_url = mr.group(1) if mr else None

    mt = _DETAILS_TARGET_RE.search(details)
    fuzz_target = mt.group(1) if mt else None
    return crash_type, frames, report_url, fuzz_target


def _sanitizer_for(crash_type: Optional[str]) -> Optional[str]:
    """Infer the sanitizer that reports ``crash_type``, or None if ambiguous
    (most memory-safety types are ASan, which is already the default)."""
    if not crash_type:
        return None
    low = crash_type.lower()
    if any(h in low for h in _MSAN_HINTS):
        return "memory"
    if any(h in low for h in _UBSAN_HINTS):
        return "undefined"
    return None


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
    # Crash metadata scraped from the record's ``details`` blob. OSS-Fuzz OSV
    # entries leave ``database_specific`` empty and put this in prose, so it
    # has to be parsed out — and it is worth parsing: ``crash_state`` is the
    # original crashing call stack, i.e. the functions a variant harness has to
    # reach, which is the strongest steering signal in the record.
    crash_type: Optional[str] = None
    crash_state: List[str] = field(default_factory=list)
    report_url: Optional[str] = None

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
        details = record.get("details", "") or ""
        crash_type, crash_state, report_url, det_target = _parse_details(details)
        return cls(
            cve_id=cve or "",
            osv_id=record.get("id", ""),
            project=project,
            main_repo=main_repo,
            fixed_commit=fixed,
            introduced_commit=introduced,
            fuzz_target=ds.get("fuzz_target") or det_target,
            sanitizer=ds.get("sanitizer") or _sanitizer_for(crash_type),
            reproducer_url=reproducer,
            summary=record.get("summary", "") or details[:200],
            published=record.get("published", "") or record.get("modified", ""),
            references=refs,
            crash_type=crash_type,
            crash_state=crash_state,
            report_url=report_url,
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


def rank_records(project: str, records: List[dict],
                 require_cve: bool = False) -> List[CveTarget]:
    """Every usable target from a list of OSV records, newest first.

    The pipeline needs the whole ranking, not just the winner: OSS-Fuzz's
    ``fixed`` commits come from automated bisection and a good fraction of them
    do not touch source at all (measured on 9 candidate projects, 2 pointed at
    commits whose diff was images/docs only — c-blosc2's is literally "Add
    diagrams for the new shared thread pool architecture"). Such a commit yields
    an empty root-cause context and therefore an unsteered prompt, so the driver
    walks this list until a record's fix diff actually touches C/C++ source.
    """
    targets = [
        CveTarget.from_osv(project, r)
        for r in records if _is_public(r)
    ]
    usable = [t for t in targets
              if t.is_usable and (t.has_cve or not require_cve)]
    usable.sort(key=lambda t: t.published, reverse=True)
    return usable


def select_from_records(project: str, records: List[dict],
                        require_cve: bool = False) -> Optional[CveTarget]:
    """Newest public, usable target from a list of OSV records.

    Pure function (no network) so the selection policy is unit-testable with
    fixtures. 'Newest' sorts on published/modified date descending.

    ``require_cve`` defaults to **False** because OSS-Fuzz-ecosystem OSV
    entries do not carry CVE aliases: they are ``OSV-YYYY-NNNN`` records for
    disclosed OSS-Fuzz bugs, and a CVE is only ever minted for the small
    subset that gets one — and then usually on the *upstream* ecosystem entry,
    not this one. Measured against the live API, ten major C/C++ projects
    (libxml2, harfbuzz, curl, openssl, wireshark, ...) return 261 records with
    zero CVE aliases between them. Requiring a CVE therefore selects nothing
    for every real project, so it is opt-in (``--require-cve``) rather than the
    default. What the pipeline actually needs is a *fix boundary* — a ``fixed``
    commit plus a repo — which is what ``is_usable`` checks.

    Note this only checks the record; whether the fix commit's *diff* is usable
    needs a clone, so the driver re-ranks with ``rank_records`` and validates.
    """
    ranked = rank_records(project, records, require_cve=require_cve)
    return ranked[0] if ranked else None


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

    def most_recent_public_cve(self, project: str,
                               require_cve: bool = False) -> Optional[CveTarget]:
        return select_from_records(project, self.query_project(project),
                                   require_cve=require_cve)
