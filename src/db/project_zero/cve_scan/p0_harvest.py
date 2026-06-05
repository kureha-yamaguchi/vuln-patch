"""Phase 1 — harvest variant-pair seeds from Project Zero's public surfaces.

Produces a hand-verifiable table of `(later_cve, prior_cve)` pairs extracted
from the P0 0-day Google Sheet, the `googleprojectzero/0days-in-the-wild`
repository, and a curated set of narrative blog posts. Each candidate pair
is then verified by `gpt-5-mini` (via :mod:`cve_scan.classifier`); only the
LLM-confirmed pairs land in the final seed table.

Run via the CLI in :mod:`cve_scan.run_p0_harvest`.
"""
from __future__ import annotations

import csv
import datetime as _dt
import json
import os
import re
import subprocess
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass, field
from html.parser import HTMLParser
from typing import Dict, List, Optional, Tuple

from . import config
from .classifier import BudgetExceededError, Classifier
from .code_overlap import CodeOverlap, CodeOverlapChecker


# --------------------------------------------------------------------------
# Dataclasses

@dataclass
class EvidenceItem:
    """One source that mentions the (later, prior) pair."""
    url: str
    quote: str
    source_kind: str            # 'rca' | 'narrative' | 'sheet' | 'rendered_index'

    def as_dict(self) -> dict:
        return asdict(self)


@dataclass
class CandidatePair:
    """Pre-LLM. Merged across sources by (later, prior) ordered key."""
    later: str
    prior: str
    evidence: List[EvidenceItem] = field(default_factory=list)
    sheet_year: Optional[int] = None
    vendor: Optional[str] = None
    product: Optional[str] = None
    upstream_commits: List[str] = field(default_factory=list)
    upstream_advisories: List[str] = field(default_factory=list)

    def merge_in(self, other: 'CandidatePair') -> None:
        seen = {(e.url, e.quote) for e in self.evidence}
        for ev in other.evidence:
            if (ev.url, ev.quote) not in seen:
                self.evidence.append(ev)
                seen.add((ev.url, ev.quote))
        self.sheet_year = self.sheet_year or other.sheet_year
        self.vendor = self.vendor or other.vendor
        self.product = self.product or other.product
        for u in other.upstream_commits:
            if u not in self.upstream_commits:
                self.upstream_commits.append(u)
        for u in other.upstream_advisories:
            if u not in self.upstream_advisories:
                self.upstream_advisories.append(u)


@dataclass
class P0SeedPair:
    """Final, LLM-verified pair. Serialized into p0_seeds.json/csv.

    `later_cve` / `prior_cve` field names are kept for backward compat with
    earlier output files, but the prior may be a bug-tracker identifier
    like `chromium-p0:2280` rather than a CVE ID.

    A pair is `confirmed` if EITHER the prose-based LLM verifier ruled it
    seed-worthy OR the rough URL-based code-overlap checker found at least
    one file touched by both fix patches.
    """
    later_cve: str
    prior_cve: str
    evidence: List[EvidenceItem]
    sheet_year: Optional[int]
    vendor: Optional[str]
    product: Optional[str]
    # combined verdict
    confirmed: bool
    # prose-LLM signal
    llm_confirmed: bool                  # seed-worthy by prose alone
    llm_relationship_kind: str           # incomplete_fix | regression | same_root_cause | ...
    llm_same_codebase: bool
    llm_is_incomplete_fix_cause: bool    # raw LLM field — strict incomplete-fix only
    llm_confidence: float
    llm_reasoning: str
    llm_best_evidence_url: str
    llm_cited_sentence: str
    # rough code-overlap signal
    overlap_status: str                  # overlap | no_overlap | partial | no_patches
    overlap_files: List[str]
    later_patch_url: Optional[str]
    prior_patch_url: Optional[str]
    # carried through from RCA prose
    upstream_commits: List[str]
    upstream_advisories: List[str]

    def as_dict(self) -> dict:
        d = asdict(self)
        d['evidence'] = [e.as_dict() if isinstance(e, EvidenceItem) else e
                         for e in self.evidence]
        return d


def is_seed_worthy(relationship_kind: str, same_codebase: bool) -> bool:
    """Per user scope: keep `incomplete_fix` and `regression` always;
    keep `same_root_cause` only when both bugs share a codebase. Drop
    `exploit_chain`, `see_also`, `unrelated`."""
    if relationship_kind in ('incomplete_fix', 'regression'):
        return True
    if relationship_kind == 'same_root_cause' and same_codebase:
        return True
    return False


# --------------------------------------------------------------------------
# Regex / parsing helpers

CVE_RE = re.compile(r'\bCVE-(\d{4})-(\d{4,})\b', re.IGNORECASE)

# Open-source bug-tracker identifiers we accept as `prior` (when no CVE
# was assigned to the original bug — common for the Chromium / Mozilla
# RCAs that reference internal bug numbers). Each canonicalises to a
# stable string identifier (e.g. 'chromium-p0:2280') so dedup works.
BUG_ID_PATTERNS = [
    # bugs.chromium.org/p/project-zero/issues/detail?id=NNNN
    (re.compile(r'bugs\.chromium\.org/p/project-zero/issues/detail\?id=(\d+)',
                re.IGNORECASE),
     lambda m: f'chromium-p0:{m.group(1)}'),
    # bugs.chromium.org/p/chromium/issues/detail?id=NNNN
    (re.compile(r'bugs\.chromium\.org/p/chromium/issues/detail\?id=(\d+)',
                re.IGNORECASE),
     lambda m: f'chromium:{m.group(1)}'),
    # crbug.com/NNNN — short form of the Chromium tracker
    (re.compile(r'\bcrbug\.com/(\d+)', re.IGNORECASE),
     lambda m: f'chromium:{m.group(1)}'),
    # bugzilla.mozilla.org/show_bug.cgi?id=NNNN
    (re.compile(r'bugzilla\.mozilla\.org/show_bug\.cgi\?id=(\d+)',
                re.IGNORECASE),
     lambda m: f'mozilla:{m.group(1)}'),
    # GitHub commit — truncate the sha to first 12 hex chars for the key
    (re.compile(r'github\.com/([^/\s)]+)/([^/\s)]+)/commit/([0-9a-f]{6,40})',
                re.IGNORECASE),
     lambda m: f'github:{m.group(1)}/{m.group(2)}@{m.group(3)[:12]}'),
]


def _extract_bug_ids(text: str) -> List[str]:
    """Return canonical open-source bug identifiers mentioned in `text`
    in order of appearance, deduplicated within the input."""
    out: List[str] = []
    seen = set()
    for pat, mk in BUG_ID_PATTERNS:
        for m in pat.finditer(text):
            bid = mk(m)
            if bid not in seen:
                seen.add(bid)
                out.append(bid)
    return out

# Variant/incomplete-fix phrase set. These mark a CandidatePair sentence
# as worth keeping; non-matching co-mentions in narrative text are dropped
# as too noisy. (Sheet pairings and RCA pairings are kept regardless —
# their context is already constrained.)
VARIANT_PHRASES = re.compile(
    r'\b('
    r'incomplete(ly)?\s+(fix|patch|mitigat|address|resolv)'
    r'|variant\s+of'
    r'|same\s+(root\s+cause|bug)\s+as'
    r'|bypass(es|ed)?\s+(the\s+|a\s+)?(fix|patch|mitigation)'
    r'|previously\s+patched\s+(in|as)'
    r'|regression\s+(of|introduced|caused)'
    r'|follow[- ]?up\s+to'
    r'|the\s+(fix|patch)\s+(for|of|in)\s+CVE-\d{4}-\d{4,}\s+(was|is|did)'
    r'|due\s+to\s+an?\s+incomplete'
    r')\b',
    re.IGNORECASE,
)

# Crude sentence splitter — good enough for the prose we scrape. Avoids
# pulling nltk just for this. Splits on . ? ! followed by whitespace.
_SENT_SPLIT = re.compile(r'(?<=[.!?])\s+(?=[A-Z(])')

# URL patterns we want to harvest for downstream Phase 2 use.
COMMIT_URL_RE = re.compile(
    r'https?://[^\s)"\']*'
    r'(github\.com/[^\s)"\']*/commit/[0-9a-f]{6,40}'
    r'|git\.kernel\.org/[^\s)"\']*'
    r'|chromium-review\.googlesource\.com/[^\s)"\']*'
    r'|bugs\.chromium\.org/[^\s)"\']*'
    r')',
    re.IGNORECASE,
)
ADVISORY_URL_RE = re.compile(
    r'https?://[^\s)"\']*'
    r'(security[^\s)"\']*advisor[^\s)"\']*'
    r'|nvd\.nist\.gov/[^\s)"\']*'
    r'|cve\.mitre\.org/[^\s)"\']*'
    r'|support\.apple\.com/[^\s)"\']*'
    r'|msrc\.microsoft\.com/[^\s)"\']*'
    r')',
    re.IGNORECASE,
)


def _split_sentences(text: str) -> List[str]:
    """Cheap sentence split; collapse internal whitespace per sentence."""
    sentences = _SENT_SPLIT.split(text)
    return [re.sub(r'\s+', ' ', s).strip() for s in sentences if s.strip()]


def _find_cves(text: str) -> List[str]:
    """Return canonicalized CVE-IDs in order of appearance."""
    out = []
    for m in CVE_RE.finditer(text):
        cid = f"CVE-{m.group(1)}-{m.group(2)}"
        out.append(cid)
    return out


def _cve_sort_key(cve_id: str) -> Tuple[int, int]:
    """(year, number) tuple from CVE-YYYY-NNNN; used to order pairs so the
    later-disclosed CVE is always on the `later` side. CVE numbers are
    not strictly chronological, but they're a good proxy when the IDs
    share a vendor's numbering scheme; combined with the year they catch
    the obvious inversions like (CVE-2019-2215, CVE-2020-0030).

    Returns (0, 0) for malformed IDs so they sort first; callers can drop
    those if they want strict ordering."""
    m = CVE_RE.match(cve_id)
    if not m:
        return (0, 0)
    return (int(m.group(1)), int(m.group(2)))


def order_pair_by_disclosure(later: str, prior: str) -> Tuple[str, str]:
    """Return (later, prior) with the more recently disclosed CVE on the
    left. Heuristic: order by (year, number). Same-year pairs may still
    be inverted; the LLM verifier's reasoning text usually makes the
    causal direction inspectable manually."""
    if _cve_sort_key(later) < _cve_sort_key(prior):
        return (prior, later)
    return (later, prior)


# --------------------------------------------------------------------------
# Source fetchers

class SheetFetcher:
    """Pull every available year-tab of the P0 0-day tracker as CSV."""

    GVIZ_URL = (
        'https://docs.google.com/spreadsheets/d/{sheet_id}/gviz/tq'
        '?tqx=out:csv&sheet={tab}'
    )

    def __init__(self,
                 sheet_id: str = config.P0_SHEET_ID,
                 cache_dir: str = os.path.join(config.CVE_SCAN_CACHE_DIR, 'p0')):
        self.sheet_id = sheet_id
        self.cache_dir = cache_dir
        os.makedirs(self.cache_dir, exist_ok=True)

    def fetch_all_years(self, refresh: bool = False) -> List[Tuple[int, str]]:
        """Try years from 2014 through this year. Returns [(year, path)]
        for tabs that successfully downloaded with non-trivial content."""
        out: List[Tuple[int, str]] = []
        this_year = _dt.date.today().year
        for year in range(2014, this_year + 1):
            path = self._fetch_year(year, refresh=refresh)
            if path is not None:
                out.append((year, path))
        return out

    def _fetch_year(self, year: int, refresh: bool) -> Optional[str]:
        path = os.path.join(self.cache_dir, f'sheet_{year}.csv')
        if not refresh and os.path.isfile(path) and os.path.getsize(path) > 0:
            return path
        url = self.GVIZ_URL.format(sheet_id=self.sheet_id, tab=year)
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'vuln-patch/0.1'})
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read()
        except urllib.error.HTTPError as e:
            if e.code == 400:
                # No such tab — common for years where P0 has no data.
                return None
            raise
        # Trivial CSV (just headers) → skip.
        if len(body) < 200:
            return None
        with open(path, 'wb') as f:
            f.write(body)
        return path


class RcaRepo:
    """Shallow clone of googleprojectzero/0days-in-the-wild for the RCA
    Markdown/HTML files."""

    def __init__(self,
                 repo_url: str = config.P0_REPO_URL,
                 dest: str = os.path.join(config.CVE_SCAN_CACHE_DIR, 'p0_repo')):
        self.repo_url = repo_url
        self.dest = dest

    def ensure(self, refresh: bool = False) -> str:
        if os.path.isdir(os.path.join(self.dest, '.git')):
            if refresh:
                subprocess.run(['git', '-C', self.dest, 'fetch', '--depth', '1', 'origin'],
                               check=True)
                subprocess.run(['git', '-C', self.dest, 'reset', '--hard', 'origin/HEAD'],
                               check=True)
            return self.dest
        os.makedirs(os.path.dirname(self.dest) or '.', exist_ok=True)
        subprocess.run(
            ['git', 'clone', '--depth', '1', self.repo_url, self.dest],
            check=True,
        )
        return self.dest


class _TextExtractor(HTMLParser):
    """Strip HTML to plain text, preserving block-level breaks."""
    SKIP_TAGS = {'script', 'style', 'noscript', 'svg'}
    BLOCK_TAGS = {'p', 'div', 'br', 'li', 'tr', 'h1', 'h2', 'h3', 'h4'}

    def __init__(self):
        super().__init__()
        self._buf: List[str] = []
        self._skip = 0

    def handle_starttag(self, tag, attrs):
        if tag in self.SKIP_TAGS:
            self._skip += 1
        elif tag in self.BLOCK_TAGS:
            self._buf.append('\n')

    def handle_endtag(self, tag):
        if tag in self.SKIP_TAGS:
            self._skip = max(0, self._skip - 1)
        elif tag in self.BLOCK_TAGS:
            self._buf.append('\n')

    def handle_data(self, data):
        if self._skip == 0:
            self._buf.append(data)

    def text(self) -> str:
        out = ''.join(self._buf)
        out = re.sub(r'\n[ \t]+', '\n', out)
        out = re.sub(r'\n{3,}', '\n\n', out)
        return out.strip()


class _TableRowExtractor(HTMLParser):
    """Walk HTML <table> elements and emit one joined-text string per <tr>.

    Project Zero's narrative posts (Mind the Gap, Déjà vu-lnerability)
    list variant pairings as table rows like
        | 2022 CVE | Variant of 2021 CVE |
        | CVE-2022-X | CVE-2021-Y       |
    Flattening the whole table into prose loses the row structure and
    causes cross-row mispairings; keeping rows preserves the per-pair
    grouping the LLM verifier needs.
    """

    def __init__(self):
        super().__init__()
        self._table_depth = 0
        self._in_cell = False
        self._cur_row: Optional[List[str]] = None
        self._cur_cell: Optional[List[str]] = None
        self.rows: List[str] = []   # one joined string per <tr>

    def handle_starttag(self, tag, attrs):
        if tag == 'table':
            self._table_depth += 1
        elif tag == 'tr' and self._table_depth:
            self._cur_row = []
        elif tag in ('td', 'th') and self._cur_row is not None:
            self._in_cell = True
            self._cur_cell = []

    def handle_endtag(self, tag):
        if tag == 'table':
            self._table_depth = max(0, self._table_depth - 1)
        elif tag == 'tr':
            if self._cur_row is not None:
                joined = ' | '.join(c.strip() for c in self._cur_row if c.strip())
                joined = re.sub(r'\s+', ' ', joined)
                if joined:
                    self.rows.append(joined)
                self._cur_row = None
        elif tag in ('td', 'th') and self._in_cell:
            if self._cur_cell is not None and self._cur_row is not None:
                self._cur_row.append(''.join(self._cur_cell))
            self._cur_cell = None
            self._in_cell = False

    def handle_data(self, data):
        if self._in_cell and self._cur_cell is not None:
            self._cur_cell.append(data)


class NarrativeFetcher:
    """Cache narrative HTML posts to disk; honor ETag for conditional GETs."""

    def __init__(self,
                 urls: List[str] = None,
                 cache_dir: str = os.path.join(config.CVE_SCAN_CACHE_DIR, 'p0', 'narrative')):
        self.urls = urls or list(config.P0_NARRATIVE_URLS)
        self.cache_dir = cache_dir
        os.makedirs(self.cache_dir, exist_ok=True)

    def fetch_all(self, refresh: bool = False) -> List[Tuple[str, str]]:
        """Returns list of (url, local_html_path)."""
        out = []
        for url in self.urls:
            path = self._fetch_one(url, refresh=refresh)
            if path is not None:
                out.append((url, path))
        return out

    def _slug(self, url: str) -> str:
        return re.sub(r'[^a-z0-9]+', '_',
                      url.lower().replace('https://', '').replace('http://', ''))

    def _fetch_one(self, url: str, refresh: bool) -> Optional[str]:
        slug = self._slug(url)
        html_path = os.path.join(self.cache_dir, slug + '.html')
        meta_path = os.path.join(self.cache_dir, slug + '.meta.json')

        headers = {'User-Agent': 'vuln-patch/0.1'}
        if not refresh and os.path.isfile(meta_path) and os.path.isfile(html_path):
            try:
                with open(meta_path, 'r', encoding='utf-8') as f:
                    meta = json.load(f)
                if meta.get('etag'):
                    headers['If-None-Match'] = meta['etag']
                if meta.get('last_modified'):
                    headers['If-Modified-Since'] = meta['last_modified']
            except (OSError, json.JSONDecodeError):
                pass

        req = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read()
                etag = resp.headers.get('ETag')
                lm = resp.headers.get('Last-Modified')
        except urllib.error.HTTPError as e:
            if e.code == 304 and os.path.isfile(html_path):
                return html_path
            print(f"  ! failed to fetch {url}: HTTP {e.code}")
            return None
        except urllib.error.URLError as e:
            print(f"  ! failed to fetch {url}: {e}")
            return None

        with open(html_path, 'wb') as f:
            f.write(body)
        with open(meta_path, 'w', encoding='utf-8') as f:
            json.dump({'etag': etag, 'last_modified': lm, 'url': url}, f)
        return html_path

    @staticmethod
    def html_to_text(path: str) -> str:
        with open(path, 'r', encoding='utf-8', errors='replace') as f:
            html = f.read()
        ext = _TextExtractor()
        ext.feed(html)
        return ext.text()

    @staticmethod
    def html_table_rows(path: str) -> List[str]:
        """Return one joined-text string per <tr> across every <table> in
        the HTML at `path`. Used to extract pair candidates from variant
        tables in narrative posts."""
        with open(path, 'r', encoding='utf-8', errors='replace') as f:
            html = f.read()
        ext = _TableRowExtractor()
        ext.feed(html)
        return ext.rows


# --------------------------------------------------------------------------
# Candidate extraction

class PairExtractor:
    """Walks every cached source and emits CandidatePairs."""

    # Sheet columns whose URLs almost always link to the SAME bug's
    # tracker entry / advisory / patch — bug-tracker IDs found here are
    # self-references, not separate priors. We deliberately DO NOT filter
    # `Root Cause Analysis`: that column sometimes cross-links to another
    # CVE's RCA when two bugs share a root cause (e.g. CVE-2019-1429's row
    # points at CVE-2019-1367.html), and we want to keep that signal.
    _SHEET_SELFLINK_COLUMNS = frozenset({
        'analysis url', 'advisory', 'bug link', 'patch', 'patch url',
        'crash link',
    })

    def __init__(self, rca_dir: str):
        self.rca_dir = rca_dir

    # ----- sheet -------------------------------------------------------

    def extract_from_sheet(self, csv_path: str, year: int) -> List[CandidatePair]:
        """Parse the year-tab CSV and emit candidate pairs.

        The CVE column nominally holds the in-the-wild (later) CVE; prior
        CVE IDs can appear in ANY other cell of the row — most often the
        Notes/Reporter field (e.g. the Mali GPU row's "Note: Google Pixel
        assigned this bug CVE-2021-39793 and ARM assigned this vulnerability
        CVE-2022-22706" lives in the Reporter column). So: subject = CVE
        column; priors = every other CVE-ID found anywhere in the row,
        with that cell's text as the evidence quote.
        """
        out: List[CandidatePair] = []
        sheet_view = (
            f'https://docs.google.com/spreadsheets/d/{config.P0_SHEET_ID}/'
            f'edit#gid=0'
        )
        with open(csv_path, 'r', encoding='utf-8', errors='replace') as f:
            reader = csv.DictReader(f)
            for row in reader:
                cve_cell = self._pick_field(row, ('cve', 'cve id'))
                subjects = _find_cves(cve_cell)
                if not subjects:
                    continue
                later = subjects[0]
                vendor = self._pick_field(row, ('vendor',))
                product = self._pick_field(row, ('product',))

                # Scan every cell for additional bug identifiers (CVE IDs
                # or open-source bug-tracker IDs like chromium-p0:NNNN).
                # Skip columns that link to the SAME bug's analysis / RCA /
                # advisory — those URLs aren't separate priors.
                for col_name, cell in row.items():
                    if not cell:
                        continue
                    col_norm = (col_name or '').strip().lower()
                    if col_norm in self._SHEET_SELFLINK_COLUMNS:
                        continue
                    ids_here = _find_cves(cell) + _extract_bug_ids(cell)
                    for cid in ids_here:
                        if cid == later or cid in subjects[1:]:
                            # later, or another "subject" CVE (slash-pairing
                            # in the CVE column itself) — both yield the
                            # same candidate edge with the CVE-column cell
                            # as quote
                            if cid == later:
                                continue
                        out.append(CandidatePair(
                            later=later,
                            prior=cid,
                            evidence=[EvidenceItem(
                                url=sheet_view,
                                quote=f'[{col_name}] {cell.strip()}',
                                source_kind='sheet',
                            )],
                            sheet_year=year,
                            vendor=vendor or None,
                            product=product or None,
                        ))
        return out

    @staticmethod
    def _pick_field(row: dict, candidates: Tuple[str, ...]) -> str:
        for k, v in row.items():
            if k and k.strip().lower() in candidates:
                return (v or '').strip()
        return ''

    # ----- RCA repo ----------------------------------------------------

    def extract_from_rca_repo(self) -> List[CandidatePair]:
        """Walk every text RCA under 0day-RCAs/. The subject CVE is parsed
        from the filename (e.g. `CVE-2022-22706.md`); other CVE IDs
        mentioned in the file's prose are candidate priors."""
        out: List[CandidatePair] = []
        root = os.path.join(self.rca_dir, '0day-RCAs')
        if not os.path.isdir(root):
            return out
        for dirpath, _, filenames in os.walk(root):
            for fn in filenames:
                if not fn.lower().endswith(('.md', '.html', '.htm', '.txt')):
                    continue
                subject = self._subject_cve_from_name(fn) \
                    or self._subject_cve_from_name(dirpath)
                if subject is None:
                    continue
                fpath = os.path.join(dirpath, fn)
                text = self._read_text_any(fpath)
                if not text:
                    continue
                rel = os.path.relpath(fpath, self.rca_dir)
                url = (
                    'https://github.com/googleprojectzero/0days-in-the-wild/'
                    f'blob/main/{rel}'
                )
                commits = [m.group(0) for m in COMMIT_URL_RE.finditer(text)]
                advisories = [m.group(0) for m in ADVISORY_URL_RE.finditer(text)]
                for sentence in _split_sentences(text):
                    cves_here = _find_cves(sentence)
                    bugs_here = _extract_bug_ids(sentence)
                    priors = [c for c in cves_here if c != subject] + bugs_here
                    if not priors:
                        continue
                    for prior in priors:
                        out.append(CandidatePair(
                            later=subject,
                            prior=prior,
                            evidence=[EvidenceItem(
                                url=url,
                                quote=sentence,
                                source_kind='rca',
                            )],
                            upstream_commits=list(commits),
                            upstream_advisories=list(advisories),
                        ))
        return out

    @staticmethod
    def _subject_cve_from_name(name: str) -> Optional[str]:
        m = CVE_RE.search(name)
        if m:
            return f"CVE-{m.group(1)}-{m.group(2)}"
        return None

    @staticmethod
    def _read_text_any(path: str) -> str:
        try:
            with open(path, 'r', encoding='utf-8', errors='replace') as f:
                raw = f.read()
        except OSError:
            return ''
        if path.lower().endswith(('.html', '.htm')):
            ext = _TextExtractor()
            ext.feed(raw)
            return ext.text()
        return raw

    # ----- narrative posts --------------------------------------------

    def extract_from_narrative(self, url: str, html_path: str) -> List[CandidatePair]:
        """Walk a narrative post and emit candidate pairs from two passes:

        1. **Sentence pass** — keep sentences that mention ≥2 bug IDs
           AND a variant/incomplete-fix phrase.
        2. **Table-row pass** — every <tr> that lists ≥2 bug IDs across
           its cells. The first ID in the row is `later`, the others
           are candidate priors. Table rows don't need to contain a
           variant phrase because they're table data — the table header
           or surrounding prose already establishes the relationship.
        """
        out: List[CandidatePair] = []

        # Pass 1: sentences with variant phrase + ≥2 identifiers.
        text = NarrativeFetcher.html_to_text(html_path)
        for sentence in _split_sentences(text):
            if not VARIANT_PHRASES.search(sentence):
                continue
            ids_here = _find_cves(sentence) + _extract_bug_ids(sentence)
            if len(ids_here) < 2:
                continue
            later = ids_here[0]
            for prior in ids_here[1:]:
                if prior == later:
                    continue
                out.append(CandidatePair(
                    later=later, prior=prior,
                    evidence=[EvidenceItem(
                        url=url, quote=sentence, source_kind='narrative',
                    )],
                ))

        # Pass 2: structured table rows.
        for row in NarrativeFetcher.html_table_rows(html_path):
            ids_here = _find_cves(row) + _extract_bug_ids(row)
            if len(ids_here) < 2:
                continue
            later = ids_here[0]
            for prior in ids_here[1:]:
                if prior == later:
                    continue
                out.append(CandidatePair(
                    later=later, prior=prior,
                    evidence=[EvidenceItem(
                        url=url, quote=row, source_kind='narrative_table',
                    )],
                ))
        return out


def merge_candidates(candidates: List[CandidatePair]) -> List[CandidatePair]:
    """Dedupe by (later, prior); union evidence lists.

    Direction is normalised first via :func:`order_pair_by_disclosure`, so
    an RCA at `CVE-2019-2215.md` that describes a follow-up CVE-2020-0030
    arising from its bad patch is recorded with later=2020-0030,
    prior=2019-2215 — matching causal direction.
    """
    by_key: Dict[Tuple[str, str], CandidatePair] = {}
    for c in candidates:
        new_later, new_prior = order_pair_by_disclosure(c.later, c.prior)
        c.later, c.prior = new_later, new_prior
        key = (c.later, c.prior)
        existing = by_key.get(key)
        if existing is None:
            by_key[key] = c
        else:
            existing.merge_in(c)
    return list(by_key.values())


# --------------------------------------------------------------------------
# LLM verification

LLM_SYSTEM_PROMPT = (
    "You are a security-advisory analyst. Given a candidate pair of bug "
    "identifiers (CVE IDs or open-source bug-tracker IDs such as Chromium "
    "Project Zero issues, Mozilla Bugzilla bugs, or Chromium issue-tracker "
    "entries) and excerpts from Project Zero documents that mention them "
    "together, classify their relationship.\n\n"
    "Relationship kinds:\n"
    "  - incomplete_fix: the LATER bug exists because the PRIOR bug's "
    "patch was incomplete, inadequate, or bypassed. This includes any "
    "case where the document indicates the prior CVE/bug had been "
    "patched but the patch was still circumvented, e.g. when the prior "
    "appears in an exploit chain together with explicit language that "
    "the patch was bypassed, ineffective, or missed a call site.\n"
    "  - regression: the prior bug was correctly fixed but a later code "
    "change re-introduced the same bug.\n"
    "  - same_root_cause: the two bugs share the same underlying root "
    "cause (e.g., the same flawed function or call-site family). One "
    "patch only fixed part of the bug class; another exposure remained. "
    "Use this when the prior patch was not described as faulty per se, "
    "but the underlying flaw was not eradicated.\n"
    "  - exploit_chain: the prior bug was paired with the later bug by "
    "an attacker AND there is NO claim that the prior patch was "
    "bypassed or ineffective. The two bugs are independent at the code "
    "level — combined only by the attacker. (If the chain context "
    "instead implies the prior patch was bypassed, prefer "
    "incomplete_fix.)\n"
    "  - see_also: the prior is merely cross-referenced (advisory, "
    "exploitation report) without a causal claim.\n"
    "  - unrelated: the prior is mentioned only as background.\n\n"
    "Also report whether the two bugs live in the same codebase / "
    "product / component (`same_codebase`). Set "
    "`is_incomplete_fix_cause=true` for `incomplete_fix` and "
    "`regression` only.\n\n"
    "Be conservative when classifying as `incomplete_fix`: require "
    "explicit language in the evidence (\"bypassed\", \"incomplete\", "
    "\"still exploitable\", \"missed\", \"the patch did not\", etc.). "
    "Otherwise prefer `same_root_cause`, `see_also`, or `unrelated` as "
    "appropriate. Return JSON only."
)

LLM_SCHEMA = {
    'type': 'object',
    'additionalProperties': False,
    'required': [
        'is_incomplete_fix_cause', 'relationship_kind', 'same_codebase',
        'confidence', 'cited_sentence', 'best_evidence_url', 'reasoning',
    ],
    'properties': {
        'is_incomplete_fix_cause': {'type': 'boolean'},
        'relationship_kind': {
            'type': 'string',
            'enum': ['incomplete_fix', 'regression', 'same_root_cause',
                     'exploit_chain', 'see_also', 'unrelated'],
        },
        'same_codebase': {
            'type': 'boolean',
            'description': 'True iff the two bugs live in the same '
                           'project, component, or codebase (e.g. both '
                           'in Chrome V8). Cross-product variants of a '
                           'bug class (e.g. Spectre across vendors) are '
                           'false.',
        },
        'confidence': {'type': 'number', 'minimum': 0, 'maximum': 1},
        'cited_sentence': {
            'type': 'string',
            'description': 'A literal substring of one of the provided '
                           'evidence quotes that supports the verdict, '
                           'or an empty string if no quote applies.',
        },
        'best_evidence_url': {
            'type': 'string',
            'description': 'The URL of the EvidenceItem the cited '
                           'sentence came from.',
        },
        'reasoning': {'type': 'string'},
    },
}


def _build_user_prompt(cand: CandidatePair) -> str:
    """Compose the user message for one candidate pair."""
    parts = [
        f"LATER CVE: {cand.later}",
        f"PRIOR CVE: {cand.prior}",
    ]
    if cand.vendor or cand.product:
        parts.append(f"VENDOR/PRODUCT: {cand.vendor or ''} / {cand.product or ''}")
    parts.append("")
    parts.append("EVIDENCE:")
    for i, ev in enumerate(cand.evidence, 1):
        parts.append(f"[{i}] {ev.source_kind} — {ev.url}")
        parts.append(f"    \"{ev.quote}\"")
    parts.append("")
    parts.append(
        "Does the evidence explicitly state that the LATER CVE arose "
        "because the PRIOR CVE's patch was incomplete, inadequate, or "
        "bypassed? Be conservative — only answer true when the text "
        "makes the causal link plain."
    )
    return "\n".join(parts)


def verify_candidates(candidates: List[CandidatePair],
                      classifier: Optional[Classifier],
                      overlap_checker: Optional[CodeOverlapChecker]
                      ) -> List[P0SeedPair]:
    """Run the two rough verifiers (prose-LLM + URL/file-overlap) over each
    candidate and emit a P0SeedPair per candidate. A pair is `confirmed`
    if EITHER signal is positive; the individual signals are also retained
    so the deep diff-relatability check can re-rank later."""
    seeds: List[P0SeedPair] = []
    confirmed_count = 0
    total = len(candidates)
    llm_budget_exhausted = False
    for idx, cand in enumerate(candidates, 1):
        # ----- prose-LLM signal -------------------------------------------------
        llm_confirmed = False
        kind = 'skipped'
        same_codebase = False
        is_incomplete = False
        llm_conf = 0.0
        llm_reasoning = ''
        llm_best_url = ''
        llm_cited = ''
        if classifier is not None and not llm_budget_exhausted:
            # Budget-trim evidence: keep at most ~6 items so the user prompt
            # stays under ~2000 tokens.
            ev_for_prompt = cand.evidence[:6]
            cand_for_prompt = CandidatePair(
                later=cand.later, prior=cand.prior,
                evidence=ev_for_prompt,
                sheet_year=cand.sheet_year, vendor=cand.vendor,
                product=cand.product,
            )
            user_prompt = _build_user_prompt(cand_for_prompt)
            try:
                decision = classifier.classify(
                    system_prompt=LLM_SYSTEM_PROMPT,
                    user_prompt=user_prompt,
                    schema=LLM_SCHEMA,
                    schema_name='p0_variant_pair',
                )
                parsed = decision.parsed
                kind = str(parsed.get('relationship_kind', 'unrelated'))
                same_codebase = bool(parsed.get('same_codebase'))
                is_incomplete = bool(parsed.get('is_incomplete_fix_cause'))
                llm_confirmed = is_seed_worthy(kind, same_codebase)
                llm_conf = float(parsed.get('confidence', 0.0))
                llm_reasoning = str(parsed.get('reasoning', ''))
                llm_best_url = str(parsed.get('best_evidence_url', ''))
                llm_cited = str(parsed.get('cited_sentence', ''))
            except BudgetExceededError:
                print(f"  ! LLM budget exceeded after "
                      f"{classifier.calls['live']} live calls; remaining "
                      f"pairs will rely on overlap only", flush=True)
                llm_budget_exhausted = True
                kind = 'budget_exceeded'

        # ----- rough code-overlap signal ----------------------------------------
        overlap: Optional[CodeOverlap] = None
        overlap_status = 'skipped'
        overlap_files: List[str] = []
        later_patch_url: Optional[str] = None
        prior_patch_url: Optional[str] = None
        if overlap_checker is not None:
            overlap = overlap_checker.check_pair(cand.later, cand.prior)
            overlap_status = overlap.status
            overlap_files = list(overlap.overlap_files)
            later_patch_url = (overlap.later_patches[0].commit_url
                               if overlap.later_patches else None)
            prior_patch_url = (overlap.prior_patches[0].commit_url
                               if overlap.prior_patches else None)

        overlap_confirmed = (overlap is not None and overlap.has_overlap)
        confirmed = llm_confirmed or overlap_confirmed
        if confirmed:
            confirmed_count += 1

        # ----- per-call progress line -------------------------------------------
        marker = '✓' if confirmed else '·'
        codebase_marker = '=' if same_codebase else '≠'
        ov_marker = '○'
        if overlap_status == 'overlap':
            ov_marker = '●'
        elif overlap_status == 'partial':
            ov_marker = '◐'
        elif overlap_status == 'no_overlap':
            ov_marker = '○'
        elif overlap_status == 'no_patches':
            ov_marker = '·'
        spend_str = (f"${classifier.spend_usd:.4f}"
                     if classifier is not None else '$0.0000')
        print(
            f"  [{idx:3d}/{total}] {marker} "
            f"{cand.later} -> {cand.prior}  "
            f"kind={kind:16s} {codebase_marker} "
            f"ov={ov_marker} ({len(overlap_files)}f)  "
            f"conf={llm_conf:.2f}  "
            f"spend={spend_str}  "
            f"confirmed={confirmed_count}",
            flush=True,
        )

        seeds.append(P0SeedPair(
            later_cve=cand.later,
            prior_cve=cand.prior,
            evidence=cand.evidence,
            sheet_year=cand.sheet_year,
            vendor=cand.vendor,
            product=cand.product,
            confirmed=confirmed,
            llm_confirmed=llm_confirmed,
            llm_relationship_kind=kind,
            llm_same_codebase=same_codebase,
            llm_is_incomplete_fix_cause=is_incomplete,
            llm_confidence=llm_conf,
            llm_reasoning=llm_reasoning,
            llm_best_evidence_url=llm_best_url,
            llm_cited_sentence=llm_cited,
            overlap_status=overlap_status,
            overlap_files=overlap_files,
            later_patch_url=later_patch_url,
            prior_patch_url=prior_patch_url,
            upstream_commits=cand.upstream_commits,
            upstream_advisories=cand.upstream_advisories,
        ))
    return seeds


# --------------------------------------------------------------------------
# Orchestrator

@dataclass
class HarvestResult:
    candidates: List[CandidatePair]
    seeds: List[P0SeedPair]
    spend_usd: float
    calls_live: int
    calls_cached: int


class P0Harvester:
    """Top-level driver: fetch sources, extract candidates, optionally
    verify with the LLM."""

    def __init__(self,
                 classifier: Optional[Classifier] = None,
                 cache_dir: str = config.CVE_SCAN_CACHE_DIR,
                 refresh: bool = False,
                 use_overlap: bool = True,
                 github_token: Optional[str] = None):
        self.classifier = classifier
        self.cache_dir = cache_dir
        self.refresh = refresh
        self.use_overlap = use_overlap
        self.github_token = github_token
        os.makedirs(self.cache_dir, exist_ok=True)

    def run(self) -> HarvestResult:
        # 1) sources
        print("[1/4] Fetching P0 sheet year-tabs ...")
        sheet_paths = SheetFetcher().fetch_all_years(refresh=self.refresh)
        print(f"    got {len(sheet_paths)} year-tabs")

        print("[2/4] Ensuring 0days-in-the-wild repo ...")
        rca_dir = RcaRepo().ensure(refresh=self.refresh)

        print("[3/4] Fetching narrative posts ...")
        narrative_paths = NarrativeFetcher().fetch_all(refresh=self.refresh)
        print(f"    got {len(narrative_paths)} narrative posts")

        # 2) extract
        print("[4/4] Extracting candidate pairs ...")
        extractor = PairExtractor(rca_dir=rca_dir)
        candidates: List[CandidatePair] = []
        for year, csv_path in sheet_paths:
            candidates.extend(extractor.extract_from_sheet(csv_path, year))
        candidates.extend(extractor.extract_from_rca_repo())
        for url, html_path in narrative_paths:
            candidates.extend(extractor.extract_from_narrative(url, html_path))
        merged = merge_candidates(candidates)
        print(f"    {len(candidates)} raw candidates -> {len(merged)} unique pairs")

        # 3) verify (LLM-prose + URL/file-overlap rough checks)
        overlap_checker = None
        if self.use_overlap:
            overlap_checker = CodeOverlapChecker(
                rca_dir=rca_dir,
                cache_dir=self.cache_dir,
                github_token=self.github_token,
            )

        if self.classifier is None and overlap_checker is None:
            seeds: List[P0SeedPair] = []
            return HarvestResult(
                candidates=merged, seeds=seeds,
                spend_usd=0.0, calls_live=0, calls_cached=0,
            )

        model_str = self.classifier.model if self.classifier else 'no-llm'
        overlap_str = 'with overlap' if overlap_checker else 'without overlap'
        print(f"Verifying {len(merged)} pairs with {model_str} ({overlap_str}) ...")
        seeds = verify_candidates(merged, self.classifier, overlap_checker)
        confirmed = [s for s in seeds if s.confirmed]
        spend = self.classifier.spend_usd if self.classifier else 0.0
        live = self.classifier.calls['live'] if self.classifier else 0
        cached = self.classifier.calls['cached'] if self.classifier else 0
        print(f"    {len(confirmed)} confirmed / {len(seeds)} verified "
              f"(${spend:.4f} spent, {live} live + {cached} cached LLM calls)")

        return HarvestResult(
            candidates=merged,
            seeds=seeds,
            spend_usd=spend,
            calls_live=live,
            calls_cached=cached,
        )


# --------------------------------------------------------------------------
# Output

def write_outputs(result: HarvestResult, out_dir: str) -> None:
    os.makedirs(out_dir, exist_ok=True)

    # candidates dump (always written so --no-llm runs produce something)
    cands_path = os.path.join(out_dir, 'candidates.json')
    with open(cands_path, 'w', encoding='utf-8') as f:
        json.dump([_cand_as_dict(c) for c in result.candidates], f, indent=2)
    print(f"  wrote {cands_path} ({len(result.candidates)} pairs)")

    if not result.seeds:
        return

    seeds_path = os.path.join(out_dir, 'seeds.json')
    with open(seeds_path, 'w', encoding='utf-8') as f:
        json.dump([s.as_dict() for s in result.seeds], f, indent=2)
    print(f"  wrote {seeds_path} ({len(result.seeds)} verified rows)")

    csv_path = os.path.join(out_dir, 'seeds.csv')
    confirmed = [s for s in result.seeds if s.confirmed]
    with open(csv_path, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow([
            'later_cve', 'prior_cve', 'confirmed_by',
            'relationship_kind', 'same_codebase', 'is_incomplete_fix_cause',
            'llm_confidence', 'overlap_status', 'overlap_file_count',
            'overlap_files', 'later_patch_url', 'prior_patch_url',
            'cited_sentence', 'best_evidence_url',
            'sheet_year', 'vendor', 'product',
            'num_evidence', 'num_upstream_commits',
        ])
        for s in confirmed:
            confirmed_by = []
            if s.llm_confirmed:
                confirmed_by.append('llm')
            if s.overlap_status == 'overlap':
                confirmed_by.append('overlap')
            writer.writerow([
                s.later_cve, s.prior_cve, '|'.join(confirmed_by),
                s.llm_relationship_kind, s.llm_same_codebase,
                s.llm_is_incomplete_fix_cause,
                f"{s.llm_confidence:.2f}",
                s.overlap_status, len(s.overlap_files),
                '|'.join(s.overlap_files[:5]),
                s.later_patch_url or '', s.prior_patch_url or '',
                s.llm_cited_sentence, s.llm_best_evidence_url,
                s.sheet_year or '', s.vendor or '', s.product or '',
                len(s.evidence), len(s.upstream_commits),
            ])
    print(f"  wrote {csv_path} ({len(confirmed)} confirmed pairs)")


def _cand_as_dict(c: CandidatePair) -> dict:
    return {
        'later': c.later,
        'prior': c.prior,
        'evidence': [e.as_dict() for e in c.evidence],
        'sheet_year': c.sheet_year,
        'vendor': c.vendor,
        'product': c.product,
        'upstream_commits': c.upstream_commits,
        'upstream_advisories': c.upstream_advisories,
    }
