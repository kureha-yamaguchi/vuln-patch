"""Rough URL-based source-code overlap check for candidate pairs.

Sibling to the LLM verifier: for each `(later, prior)` candidate, try to
locate the fixing commits of both bugs on a public host (GitHub,
git.kernel.org, chromium-review.googlesource.com), fetch their unified
diffs, and compute file-level overlap. If the two patches touch any of
the same files, that's a strong code-level signal that the bugs are
related — independent of whatever the prose-based LLM verifier said.

A pair is promoted to a candidate seed if EITHER the LLM verifier or
this overlap check confirms it. The much-deeper diff-relatability
analysis (gpt-5-mini reading both diffs and judging causality) is a
later phase and lives outside this module.

This is the "rough" pass: file-level set intersection. Function-level
overlap is left to the deep phase.
"""
from __future__ import annotations

import base64
import binascii
import json
import os
import re
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass, field
from html.parser import HTMLParser
from typing import Dict, List, Optional, Tuple

from . import config


# --------------------------------------------------------------------------
# Patch-URL recognition

# Three host families we know how to dereference to a raw .patch/.diff form.
# Each entry maps a "commit URL" we might find in an RCA / bug page to the
# canonical raw-patch URL we'll actually fetch.

# Note the lookbehind/lookahead constraints — we don't want to match a URL
# embedded inside a longer token (e.g. URL-encoded inside another URL).
GITHUB_COMMIT_RE = re.compile(
    r'https?://github\.com/([^/\s)"\'<>]+)/([^/\s)"\'<>]+)/commit/([0-9a-f]{6,40})',
    re.IGNORECASE,
)
# cgit-style hosts (kernel.org, freedesktop.org) use `?id=SHA` URLs.
KERNELORG_COMMIT_RE = re.compile(
    r'https?://(?:git\.kernel\.org|cgit\.freedesktop\.org)'
    r'/[^\s)"\'<>]*(?:[?&;]id=|commit/[?]id=)([0-9a-f]{6,40})',
    re.IGNORECASE,
)
# GitLab-style hosts (git.codelinaro.org, gitlab.freedesktop.org, ...)
# use `/-/commit/SHA` URLs. Add `.patch` to the end for the raw diff.
GITLAB_COMMIT_RE = re.compile(
    r'https?://([a-zA-Z0-9.-]+)/(?:[^\s)"\'<>]*?)/\-/commit/([0-9a-f]{6,40})',
    re.IGNORECASE,
)
# Gitiles-style hosts (chromium.googlesource.com, android.googlesource.com).
# Commit URLs look like `.../+/SHA[/path][?...]` or `.../+/refs/heads/...`.
# We only want commit-SHA forms here.
GITILES_COMMIT_RE = re.compile(
    r'https?://(chromium\.googlesource\.com|android\.googlesource\.com)'
    r'/([^\s)"\'<>?#]+?)/\+/([0-9a-f]{6,40})\b',
    re.IGNORECASE,
)
# Chromium Gerrit (chromium-review.googlesource.com). We accept either
# `/+/NNNNNN`, `/c/.../+/NNNNNN`, or the short `/c/NNNNNN` form. The
# change number is what the REST API actually keys on.
CHROMIUM_REVIEW_RE = re.compile(
    r'https?://chromium-review\.googlesource\.com/[^\s)"\'<>]*?'
    r'(?:\+/(\d+)|/c/(?:[^/\s]+/)*\+/(\d+)|/c/(\d+))',
    re.IGNORECASE,
)
# Bugzilla (currently only mozilla); recognise both the show_bug URL and
# direct attachment URLs.
BUGZILLA_MOZILLA_BUG_RE = re.compile(
    r'https?://bugzilla\.mozilla\.org/show_bug\.cgi\?id=(\d+)',
    re.IGNORECASE,
)
BUGZILLA_MOZILLA_ATTACH_RE = re.compile(
    r'https?://bugzilla\.mozilla\.org/attachment\.cgi\?id=(\d+)',
    re.IGNORECASE,
)


# --------------------------------------------------------------------------
# Dataclasses

@dataclass
class Patch:
    """A fetched unified diff and the list of files it touches."""
    bug_id: str                  # the bug this patch was found for
    host: str                    # 'github' | 'kernelorg' | 'chromium-review' | ...
    commit_url: str              # the human-readable commit URL
    raw_url: str                 # the URL we actually fetched
    sha: Optional[str]           # commit sha (None for Gerrit/Bugzilla)
    files: List[str] = field(default_factory=list)
    local_path: Optional[str] = None

    def as_dict(self) -> dict:
        return asdict(self)

    def identity_keys(self) -> List[str]:
        """All tokens that uniquely identify this patch for self-pair
        detection. Two patches are the "same fix" iff any of their
        identity keys match (with sha-prefix tolerance, see
        :func:`_patches_collide`).

        - Git patches contribute ``sha:<sha>``.
        - Gerrit patches contribute ``gerrit:<change-num>`` extracted
          from the chromium-review URL.
        - Bugzilla patches contribute ``bugzilla:<bug-num>``.
        - Anything else falls back to ``url:<normalised-commit-url>``
          (fragment stripped, trailing slash removed, lower-cased —
          query string preserved because for many trackers it carries
          identity, e.g. ``?id=NNN``).

        The url fallback is ONLY emitted when no more-specific token was
        extracted — otherwise sibling Bugzilla bugs would collide on
        the host (``bz.moz/show_bug.cgi`` part) even though their bug
        numbers differ."""
        out: List[str] = []
        if self.sha:
            out.append(f'sha:{self.sha}')
        if self.host == 'chromium-review':
            m = CHROMIUM_REVIEW_RE.search(self.commit_url or '')
            if m:
                change = m.group(1) or m.group(2) or m.group(3)
                if change:
                    out.append(f'gerrit:{change}')
        if self.host == 'bugzilla-mozilla':
            m = BUGZILLA_MOZILLA_BUG_RE.search(self.commit_url or '')
            if m:
                out.append(f'bugzilla:{m.group(1)}')
        if not out and self.commit_url:
            norm = re.sub(r'#.*$', '', self.commit_url).rstrip('/').lower()
            out.append(f'url:{norm}')
        return out


@dataclass
class CodeOverlap:
    """Result of a rough overlap check for one candidate pair."""
    later: str
    prior: str
    later_patches: List[Patch] = field(default_factory=list)
    prior_patches: List[Patch] = field(default_factory=list)
    overlap_files: List[str] = field(default_factory=list)
    status: str = 'no_patches'
    # status ∈ {
    #   'overlap'           – at least one file is touched by both sides
    #   'no_overlap'        – both sides fetched but disjoint files
    #   'partial'           – one side fetched, the other unavailable
    #   'no_patches'        – no patch URLs found for either side
    #   'fetch_error'       – URLs were found but at least one fetch failed
    # }
    note: str = ''

    @property
    def has_overlap(self) -> bool:
        return self.status == 'overlap' and bool(self.overlap_files)

    def as_dict(self) -> dict:
        d = asdict(self)
        d['later_patches'] = [p.as_dict() if isinstance(p, Patch) else p
                              for p in self.later_patches]
        d['prior_patches'] = [p.as_dict() if isinstance(p, Patch) else p
                              for p in self.prior_patches]
        d['has_overlap'] = self.has_overlap
        return d


# --------------------------------------------------------------------------
# HTML scraping — pull a bug-tracker page and collect commit URLs out of it

class _LinkCollector(HTMLParser):
    """Strip HTML, return every http(s) URL found in either tag attrs
    (e.g. <a href=...>) or in raw text content."""

    URL_IN_TEXT = re.compile(r'https?://[^\s)"\'<>]+')

    def __init__(self):
        super().__init__()
        self.urls: List[str] = []
        self._skip = 0

    def handle_starttag(self, tag, attrs):
        if tag in ('script', 'style', 'noscript'):
            self._skip += 1
        for _, v in attrs:
            if v and v.startswith(('http://', 'https://')):
                self.urls.append(v)

    def handle_endtag(self, tag):
        if tag in ('script', 'style', 'noscript'):
            self._skip = max(0, self._skip - 1)

    def handle_data(self, data):
        if self._skip:
            return
        for m in self.URL_IN_TEXT.finditer(data):
            self.urls.append(m.group(0))


def _collect_urls_in_html(html: str) -> List[str]:
    p = _LinkCollector()
    p.feed(html)
    out = []
    seen = set()
    for u in p.urls:
        if u not in seen:
            seen.add(u)
            out.append(u)
    return out


def _commit_urls_from_text(text: str) -> List[Tuple[str, str]]:
    """Return [(host, commit_url)] for every recognised commit URL in
    `text`. Preserves order; dedupes within the input."""
    out: List[Tuple[str, str]] = []
    seen = set()
    for m in GITHUB_COMMIT_RE.finditer(text):
        url = m.group(0)
        if url not in seen:
            seen.add(url)
            out.append(('github', url))
    for m in KERNELORG_COMMIT_RE.finditer(text):
        url = m.group(0)
        if url not in seen:
            seen.add(url)
            out.append(('kernelorg', url))
    for m in CHROMIUM_REVIEW_RE.finditer(text):
        url = m.group(0)
        if url not in seen:
            seen.add(url)
            out.append(('chromium-review', url))
    for m in GITILES_COMMIT_RE.finditer(text):
        url = m.group(0)
        if url not in seen:
            seen.add(url)
            out.append(('gitiles', url))
    for m in GITLAB_COMMIT_RE.finditer(text):
        url = m.group(0)
        if url not in seen:
            seen.add(url)
            out.append(('gitlab', url))
    for m in BUGZILLA_MOZILLA_BUG_RE.finditer(text):
        url = m.group(0)
        if url not in seen:
            seen.add(url)
            out.append(('bugzilla-mozilla', url))
    return out


# --------------------------------------------------------------------------
# Patch fetching + diff parsing

# Files modified appear in unified diffs as `+++ b/<path>` (or `+++ <path>`
# for diffs without a/b prefixes). We also accept `--- a/<path>` for the
# rare deletion-only case.
DIFF_NEW_FILE_RE = re.compile(r'^\+\+\+ (?:b/)?([^\s]+)', re.MULTILINE)
DIFF_OLD_FILE_RE = re.compile(r'^--- (?:a/)?([^\s]+)', re.MULTILINE)


# Boilerplate paths that get touched by nearly every commit to their
# subtree (release notes, browser test expectations, package locks, ...).
# Overlap on these tells us nothing about code-level relatedness; we
# require at least one non-boilerplate file in `overlap_files` before
# calling a pair `overlap`.
BOILERPLATE_FILE_RES = [
    re.compile(r'(^|/)ChangeLog$', re.IGNORECASE),
    re.compile(r'(^|/)CHANGELOG(\.[a-z]+)?$', re.IGNORECASE),
    re.compile(r'(^|/)NEWS$', re.IGNORECASE),
    re.compile(r'-expected\.(txt|html|png)$', re.IGNORECASE),
    re.compile(r'TestExpectations$', re.IGNORECASE),
    re.compile(r'(^|/)package-lock\.json$', re.IGNORECASE),
    re.compile(r'(^|/)yarn\.lock$', re.IGNORECASE),
    re.compile(r'(^|/)Cargo\.lock$', re.IGNORECASE),
    # CVE-metadata repos (cvelistV5 etc.) — the search fallback occasionally
    # returns commits to these repos because they mention the CVE ID.
    re.compile(r'^cves/.*\.json$', re.IGNORECASE),
    re.compile(r'^advisories/.*\.(json|yaml|yml)$', re.IGNORECASE),
]


def _is_boilerplate_file(path: str) -> bool:
    return any(p.search(path) for p in BOILERPLATE_FILE_RES)


def _patches_collide(lp: List['Patch'], pp: List['Patch']) -> bool:
    """True if any (later, prior) patch pair shares an identity key.

    Two patches share an identity iff any of:
      - Their SHAs prefix-match (handles 12-char ↔ 40-char forms);
      - Their Gerrit change numbers are equal;
      - Their Bugzilla bug numbers are equal;
      - Their canonical commit URLs are equal.
    """
    if not lp or not pp:
        return False

    # Split SHA identities out so we can do prefix matching across
    # variable-length forms; the other identity tokens are matched as
    # plain set intersections.
    def _split(patches: List['Patch']):
        shas, others = [], set()
        for p in patches:
            for tok in p.identity_keys():
                if tok.startswith('sha:'):
                    shas.append(tok[4:])
                else:
                    others.add(tok)
        return shas, others

    later_shas, later_others = _split(lp)
    prior_shas, prior_others = _split(pp)

    if later_others & prior_others:
        return True
    for ls in later_shas:
        for ps in prior_shas:
            if ls.startswith(ps) or ps.startswith(ls):
                return True
    return False


# Repos whose commits we never want to count as "the fix" for any CVE
# they mention — they're index/metadata stores, not source codebases.
PATCH_REPO_DENYLIST = frozenset({
    'cveproject/cvelistv5',
    'cveproject/cve-services',
    'github/advisory-database',
    'rustsec/advisory-db',
    'osv-vulnerabilities',
    'google/osv.dev',
})

# Pattern-based denial — catches forks of metadata repos and the long
# tail of personal vuln-tracker/CVE-collection repos that pollute
# GitHub commit-search results for any CVE ID.
PATCH_REPO_DENY_PATTERNS = [
    re.compile(r'cvelist',                  re.IGNORECASE),
    re.compile(r'cve[-_]?services?',        re.IGNORECASE),
    re.compile(r'cve[-_]?(db|database|list|tracker|collection)',
                                            re.IGNORECASE),
    re.compile(r'vulnerab[iy]lit[yi]',      re.IGNORECASE),
    re.compile(r'advisor[yi][-_]?(db|database|tracker|list)?$',
                                            re.IGNORECASE),
    re.compile(r'nuclei[-_]?templates',     re.IGNORECASE),
    re.compile(r'security[-_]?(db|database|tracker|alerts)',
                                            re.IGNORECASE),
    re.compile(r'poc[-_]?in[-_]?github',    re.IGNORECASE),
    re.compile(r'exploit[-_]?db',           re.IGNORECASE),
]


def _is_denylisted_repo_url(commit_url: str) -> bool:
    m = GITHUB_COMMIT_RE.search(commit_url)
    if not m:
        return False
    owner = m.group(1).lower()
    repo = m.group(2).lower()
    slug = f'{owner}/{repo}'
    if slug in PATCH_REPO_DENYLIST:
        return True
    return any(p.search(slug) for p in PATCH_REPO_DENY_PATTERNS)


# When the GitHub commit-search fallback fires, results from random
# users / personal forks dominate (a tracker repo will mention any
# CVE ID in its commit messages and out-rank actual fix commits). To
# keep the resolver honest, we only trust search-origin results whose
# repo owner is one of these — i.e. the upstream code lives at that
# owner's GitHub. Other origins (RCA-extracted URLs, derived from a
# `github:owner/repo@sha` identifier, Gerrit/Bugzilla) are trusted
# regardless and bypass this allowlist.
TRUSTED_GITHUB_OWNERS = frozenset({
    # Browsers and JS engines
    'chromium', 'v8', 'mozilla', 'webkit',
    # Project Zero tooling + reports
    'googleprojectzero',
    # Linux + Android mirrors
    'torvalds', 'gregkh', 'aosp-mirror',
    # Vendor orgs that publish their own fixes
    'apple', 'apple-oss-distributions', 'microsoft', 'google',
    # Major OSS projects whose CVE fix commits we'd want
    'systemd', 'curl', 'openssl', 'php', 'php-src',
    'libreoffice', 'wireshark', 'qemu', 'redis',
    'ffmpeg', 'libvpx', 'libpng', 'freetype',
    'sqlite', 'libxml2', 'libxslt',
})


def _is_trusted_github_owner_url(commit_url: str) -> bool:
    """True iff `commit_url` is a github.com URL whose owner is in
    :data:`TRUSTED_GITHUB_OWNERS`."""
    m = GITHUB_COMMIT_RE.search(commit_url)
    if not m:
        return False
    return m.group(1).lower() in TRUSTED_GITHUB_OWNERS


# Code-file suffixes we expect a real fix-of-vuln commit to touch. A
# patch that only modifies metadata (changelogs, JSON/YAML advisory
# entries, lockfiles) is almost certainly NOT the upstream fix commit
# we want to compare against.
SOURCE_FILE_SUFFIXES = (
    '.c', '.cc', '.cpp', '.cxx', '.h', '.hpp', '.hh',
    '.js', '.ts', '.jsx', '.tsx', '.mjs',
    '.py', '.rb', '.go', '.rs', '.java', '.kt', '.scala', '.swift',
    '.m', '.mm',
    '.php', '.cs', '.fs', '.ml', '.hs',
    '.s', '.S', '.asm', '.cl', '.metal',
    '.sh', '.bash', '.zsh', '.ps1',
    '.proto', '.thrift', '.idl',
)


def _patch_touches_source_code(files: List[str]) -> bool:
    """True if at least one file in the diff looks like source code (not
    just metadata/changelog/JSON/YAML/test-expectations)."""
    for f in files:
        if _is_boilerplate_file(f):
            continue
        fl = f.lower()
        if fl.endswith(SOURCE_FILE_SUFFIXES):
            return True
    return False


def parse_patch_files(patch_text: str) -> List[str]:
    """Return the file paths touched by a unified diff, deduplicated and
    in order of first appearance. /dev/null entries are skipped."""
    seen = set()
    out: List[str] = []
    # Prefer the +++ side (the new path); fall back to --- if +++ was
    # /dev/null (i.e. a deletion).
    new_paths = DIFF_NEW_FILE_RE.findall(patch_text)
    old_paths = DIFF_OLD_FILE_RE.findall(patch_text)
    for i, p in enumerate(new_paths):
        candidate = p
        if candidate.endswith('/dev/null') or candidate == '/dev/null':
            # use --- side if available for this hunk
            if i < len(old_paths):
                candidate = old_paths[i]
        if not candidate or candidate == '/dev/null':
            continue
        if candidate not in seen:
            seen.add(candidate)
            out.append(candidate)
    return out


class PatchFetcher:
    """Resolves commit URLs to raw unified diffs, with a disk cache."""

    def __init__(self,
                 cache_dir: str = os.path.join(config.CVE_SCAN_CACHE_DIR, 'patches'),
                 github_token: Optional[str] = None):
        self.cache_dir = cache_dir
        os.makedirs(self.cache_dir, exist_ok=True)
        self.github_token = github_token or os.getenv('GITHUB_TOKEN')

    def fetch(self, host: str, commit_url: str, bug_id: str) -> Optional[Patch]:
        """Best-effort fetch. Returns None if the host isn't supported
        for raw diff retrieval or the fetch fails."""
        if host == 'github':
            return self._fetch_github(commit_url, bug_id)
        if host == 'kernelorg':
            return self._fetch_kernelorg(commit_url, bug_id)
        if host == 'chromium-review':
            return self._fetch_gerrit(commit_url, bug_id)
        if host == 'bugzilla-mozilla':
            return self._fetch_bugzilla_mozilla(commit_url, bug_id)
        if host == 'gitiles':
            return self._fetch_gitiles(commit_url, bug_id)
        if host == 'gitlab':
            return self._fetch_gitlab(commit_url, bug_id)
        return None

    # ----- GitHub ------------------------------------------------------

    def _fetch_github(self, commit_url: str, bug_id: str) -> Optional[Patch]:
        m = GITHUB_COMMIT_RE.search(commit_url)
        if not m:
            return None
        owner, repo, sha = m.group(1), m.group(2), m.group(3)
        raw_url = f'https://github.com/{owner}/{repo}/commit/{sha}.patch'
        local = os.path.join(self.cache_dir, f'github_{sha}.patch')
        text = self._read_or_fetch(raw_url, local, want_token=True)
        if text is None:
            return None
        return Patch(
            bug_id=bug_id, host='github',
            commit_url=commit_url, raw_url=raw_url, sha=sha,
            files=parse_patch_files(text), local_path=local,
        )

    # ----- kernel.org cgit --------------------------------------------

    def _fetch_kernelorg(self, commit_url: str, bug_id: str) -> Optional[Patch]:
        # cgit serves the raw patch when you append `?dt=2` to the
        # /commit/ view, or use the /patch/ view directly. We rewrite to
        # the /patch/ form, dropping any existing path components after
        # the project name.
        m = KERNELORG_COMMIT_RE.search(commit_url)
        if not m:
            return None
        sha = m.group(1)
        # Find the project root in the URL: e.g.
        # https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=...
        # → root = .../linux.git/, raw = root + patch/?id=...
        root_m = re.match(
            r'(https?://(?:git\.kernel\.org|cgit\.freedesktop\.org)/[^?#]*?/)'
            r'(?:commit|tree|log|patch)/?',
            commit_url, re.IGNORECASE,
        )
        if not root_m:
            return None
        raw_url = root_m.group(1) + f'patch/?id={sha}'
        local = os.path.join(self.cache_dir, f'kernelorg_{sha}.patch')
        text = self._read_or_fetch(raw_url, local, want_token=False)
        if text is None:
            return None
        return Patch(
            bug_id=bug_id, host='kernelorg',
            commit_url=commit_url, raw_url=raw_url, sha=sha,
            files=parse_patch_files(text), local_path=local,
        )

    # ----- GitLab (.../-/commit/SHA) -----------------------------------

    def _fetch_gitlab(self, commit_url: str, bug_id: str) -> Optional[Patch]:
        m = GITLAB_COMMIT_RE.search(commit_url)
        if not m:
            return None
        host = m.group(1).lower()
        sha = m.group(2)
        # GitLab serves the raw unified diff at `.diff` (or `.patch`).
        raw_url = commit_url.split('?', 1)[0].rstrip('/') + '.diff'
        host_slug = re.sub(r'[^a-z0-9]+', '_', host)
        local = os.path.join(self.cache_dir,
                             f'gitlab_{host_slug}_{sha}.patch')
        text = self._read_or_fetch(raw_url, local, want_token=False)
        if text is None or not text.strip().startswith('diff '):
            return None
        return Patch(
            bug_id=bug_id, host='gitlab',
            commit_url=commit_url, raw_url=raw_url, sha=sha,
            files=parse_patch_files(text), local_path=local,
        )

    # ----- Gitiles (chromium.googlesource.com / android.googlesource.com) ---

    def _fetch_gitiles(self, commit_url: str, bug_id: str) -> Optional[Patch]:
        m = GITILES_COMMIT_RE.search(commit_url)
        if not m:
            return None
        host = m.group(1).lower()
        repo_path = m.group(2)
        sha = m.group(3)
        raw_url = (f'https://{host}/{repo_path}/+/{sha}^!?format=text')
        host_slug = 'chromium' if 'chromium' in host else 'android'
        local = os.path.join(self.cache_dir,
                             f'gitiles_{host_slug}_{sha}.patch')
        text = self._fetch_gitiles_decoded(raw_url, local)
        if text is None:
            return None
        return Patch(
            bug_id=bug_id, host='gitiles',
            commit_url=commit_url, raw_url=raw_url, sha=sha,
            files=parse_patch_files(text), local_path=local,
        )

    def _fetch_gitiles_decoded(self, url: str, local: str) -> Optional[str]:
        if os.path.isfile(local) and os.path.getsize(local) > 0:
            try:
                with open(local, 'r', encoding='utf-8', errors='replace') as f:
                    return f.read()
            except OSError:
                pass
        headers = {'User-Agent': 'vuln-patch/0.1'}
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as e:
            print(f"  ! gitiles fetch failed for {url}: {e}", flush=True)
            return None
        # Gitiles `?format=text` returns the unified diff base64-encoded
        # with newlines. Pad if necessary.
        body_clean = ''.join(body.split())
        body_padded = body_clean + '=' * (-len(body_clean) % 4)
        try:
            decoded = base64.b64decode(body_padded).decode(
                'utf-8', errors='replace',
            )
        except (binascii.Error, ValueError) as e:
            print(f"  ! gitiles base64 decode failed for {url}: {e}",
                  flush=True)
            return None
        with open(local, 'w', encoding='utf-8') as f:
            f.write(decoded)
        return decoded

    # ----- chromium-review.googlesource.com (Gerrit) ------------------

    def _fetch_gerrit(self, commit_url: str, bug_id: str) -> Optional[Patch]:
        m = CHROMIUM_REVIEW_RE.search(commit_url)
        if not m:
            return None
        change_num = m.group(1) or m.group(2) or m.group(3)
        if not change_num:
            return None
        # Gerrit's per-change "current patch" endpoint returns the base64-
        # encoded unified diff. The response includes Gerrit's XSSI
        # prefix `)]}'\n` that we need to strip if present (the patch
        # endpoint omits it, but be defensive).
        raw_url = (f'https://chromium-review.googlesource.com/changes/'
                   f'{change_num}/revisions/current/patch')
        local = os.path.join(self.cache_dir,
                             f'gerrit_chromium_{change_num}.patch')
        text = self._fetch_gerrit_decoded(raw_url, local)
        if text is None:
            return None
        return Patch(
            bug_id=bug_id, host='chromium-review',
            commit_url=commit_url, raw_url=raw_url,
            sha=None,  # Gerrit changes are numeric, not SHAs
            files=parse_patch_files(text), local_path=local,
        )

    def _fetch_gerrit_decoded(self, url: str, local: str) -> Optional[str]:
        if os.path.isfile(local) and os.path.getsize(local) > 0:
            try:
                with open(local, 'r', encoding='utf-8', errors='replace') as f:
                    return f.read()
            except OSError:
                pass
        headers = {'User-Agent': 'vuln-patch/0.1'}
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as e:
            print(f"  ! gerrit fetch failed for {url}: {e}", flush=True)
            return None
        body = body.lstrip(")]}'\n").strip()
        # Pad base64 to a multiple of 4 (Gerrit may omit padding).
        body_padded = body + '=' * (-len(body) % 4)
        try:
            decoded = base64.b64decode(body_padded).decode(
                'utf-8', errors='replace',
            )
        except (binascii.Error, ValueError) as e:
            print(f"  ! gerrit base64 decode failed for {url}: {e}", flush=True)
            return None
        with open(local, 'w', encoding='utf-8') as f:
            f.write(decoded)
        return decoded

    # ----- bugzilla.mozilla.org --------------------------------------

    def _fetch_bugzilla_mozilla(self, commit_url: str,
                                bug_id: str) -> Optional[Patch]:
        m = BUGZILLA_MOZILLA_BUG_RE.search(commit_url)
        if not m:
            return None
        bug_num = m.group(1)
        local = os.path.join(self.cache_dir,
                             f'bugzilla_mozilla_{bug_num}.patch')
        text = self._fetch_bugzilla_patches(bug_num, local)
        if text is None:
            return None
        return Patch(
            bug_id=bug_id, host='bugzilla-mozilla',
            commit_url=commit_url, raw_url=local, sha=None,
            files=parse_patch_files(text), local_path=local,
        )

    # Mozilla migrated to Phabricator for code review in 2019. Bugzilla
    # attachments on modern bugs are typically Phabricator-revision-URL
    # pointers (text/x-phabricator-request content type) rather than the
    # actual unified diff. We follow the URL to the raw diff.
    PHABRICATOR_URL_RE = re.compile(
        r'https://phabricator\.services\.mozilla\.com/D\d+',
        re.IGNORECASE,
    )

    def _fetch_bugzilla_patches(self, bug_num: str,
                                local: str) -> Optional[str]:
        """Fetch all non-obsolete patch attachments on a Bugzilla bug,
        decode + concatenate them as one combined unified-diff buffer.
        Follows Phabricator-URL pointers to the real diffs."""
        if os.path.isfile(local) and os.path.getsize(local) > 0:
            try:
                with open(local, 'r', encoding='utf-8',
                          errors='replace') as f:
                    cached = f.read()
                # Only trust the cache if it actually looks like a diff
                # (legacy caches written before Phabricator-follow may
                # contain only URL pointers).
                if cached.lstrip().startswith(('diff ', '--- ', '+++ ')) \
                        or 'diff --git' in cached[:1000]:
                    return cached
            except OSError:
                pass
        api_url = (f'https://bugzilla.mozilla.org/rest/bug/{bug_num}'
                   f'/attachment')
        headers = {'User-Agent': 'vuln-patch/0.1',
                   'Accept': 'application/json'}
        try:
            req = urllib.request.Request(api_url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError,
                TimeoutError) as e:
            print(f"  ! bugzilla fetch failed for bug {bug_num}: {e}",
                  flush=True)
            return None
        try:
            data = json.loads(body)
        except json.JSONDecodeError:
            return None

        attachments = (data.get('bugs') or {}).get(str(bug_num)) or []
        decoded_patches: List[str] = []
        for att in attachments:
            if att.get('is_obsolete'):
                continue
            ct = (att.get('content_type') or '').lower()
            fn = (att.get('file_name') or '').lower()
            looks_like_patch = (
                'patch' in ct or 'diff' in ct
                or fn.endswith(('.patch', '.diff'))
                or ct in ('text/x-patch', 'text/x-diff',
                          'text/x-phabricator-request')
            )
            if not looks_like_patch:
                continue
            blob = att.get('data')
            if not blob:
                continue
            try:
                decoded = base64.b64decode(blob).decode(
                    'utf-8', errors='replace',
                )
            except (binascii.Error, ValueError):
                continue

            # Recognise Phabricator-URL pointers: follow each linked
            # revision to its raw diff. (Modern Mozilla bugs almost
            # exclusively use this pattern.)
            phab_urls = self.PHABRICATOR_URL_RE.findall(decoded)
            if phab_urls and not decoded.lstrip().startswith(('diff ', '--- ')):
                for purl in phab_urls:
                    diff = self._fetch_phabricator_raw_diff(purl)
                    if diff:
                        decoded_patches.append(diff)
                continue
            decoded_patches.append(decoded)
        if not decoded_patches:
            return None
        combined = '\n\n'.join(decoded_patches)
        with open(local, 'w', encoding='utf-8') as f:
            f.write(combined)
        return combined

    def _fetch_phabricator_raw_diff(self, revision_url: str) -> Optional[str]:
        """Fetch the raw unified diff for a Mozilla Phabricator revision.

        The `?download=true` query on the revision page returns
        text/plain with the diff body."""
        raw_url = revision_url.rstrip('/') + '?download=true'
        headers = {'User-Agent': 'vuln-patch/0.1'}
        try:
            req = urllib.request.Request(raw_url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError,
                TimeoutError) as e:
            print(f"  ! phabricator fetch failed for {revision_url}: {e}",
                  flush=True)
            return None
        if not body.strip():
            return None
        return body

    # ----- shared HTTP fetch with caching -----------------------------

    def _read_or_fetch(self, url: str, local: str,
                       want_token: bool) -> Optional[str]:
        if os.path.isfile(local) and os.path.getsize(local) > 0:
            try:
                with open(local, 'r', encoding='utf-8', errors='replace') as f:
                    return f.read()
            except OSError:
                pass
        headers = {'User-Agent': 'vuln-patch/0.1'}
        if want_token and self.github_token:
            headers['Authorization'] = f'token {self.github_token}'
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as e:
            # Silent on network errors — caller handles None.
            print(f"  ! patch fetch failed for {url}: {e}", flush=True)
            return None
        with open(local, 'w', encoding='utf-8') as f:
            f.write(body)
        return body


# --------------------------------------------------------------------------
# Resolving commit URLs for each bug id

class BugPageScraper:
    """Fetch and cache a bug-tracker page (Chromium issues, Mozilla
    Bugzilla), then return every commit URL mentioned inside it. The
    pages are HTML; we use a minimal HTMLParser to walk anchors + text."""

    def __init__(self,
                 cache_dir: str = os.path.join(config.CVE_SCAN_CACHE_DIR,
                                               'bug_pages')):
        self.cache_dir = cache_dir
        os.makedirs(self.cache_dir, exist_ok=True)

    def _slug(self, bug_id: str) -> str:
        return re.sub(r'[^A-Za-z0-9._-]+', '_', bug_id)

    def _read_or_fetch(self, url: str, local: str) -> Optional[str]:
        if os.path.isfile(local) and os.path.getsize(local) > 0:
            try:
                with open(local, 'r', encoding='utf-8', errors='replace') as f:
                    return f.read()
            except OSError:
                pass
        try:
            req = urllib.request.Request(
                url, headers={'User-Agent': 'vuln-patch/0.1'},
            )
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as e:
            print(f"  ! bug page fetch failed for {url}: {e}", flush=True)
            return None
        with open(local, 'w', encoding='utf-8') as f:
            f.write(body)
        return body

    def commits_for(self, bug_id: str) -> List[Tuple[str, str]]:
        """Best-effort: returns [(host, commit_url), ...]. Empty if the
        bug tracker page wasn't fetchable or contained no commit links."""
        if bug_id.startswith('chromium-p0:'):
            num = bug_id.split(':', 1)[1]
            url = (f'https://bugs.chromium.org/p/project-zero/'
                   f'issues/detail?id={num}')
        elif bug_id.startswith('chromium:'):
            num = bug_id.split(':', 1)[1]
            url = (f'https://bugs.chromium.org/p/chromium/'
                   f'issues/detail?id={num}')
        elif bug_id.startswith('mozilla:'):
            num = bug_id.split(':', 1)[1]
            url = f'https://bugzilla.mozilla.org/show_bug.cgi?id={num}'
        else:
            return []
        local = os.path.join(self.cache_dir, self._slug(bug_id) + '.html')
        html = self._read_or_fetch(url, local)
        if not html:
            return []
        # The pages often dynamically render commit links via JS. As a
        # cheap fallback we scan the static HTML for any commit URL
        # patterns; for Chromium-tracker pages the issue comments
        # sometimes embed the URLs in plaintext.
        return _commit_urls_from_text(html)


class GerritBugSearcher:
    """Search chromium-review for changes whose commit message references
    a bug ID. Chromium's commit policy requires `Bug: NNNN` trailers, so
    `?q=bug:NNNN+status:merged` returns the fix commit(s) for any tracked
    issue. Used as a fallback when the bug-tracker page (which is now
    JS-only) can't be scraped for commit links.
    """

    BASE = 'https://chromium-review.googlesource.com/changes/'

    def __init__(self,
                 cache_dir: str = os.path.join(config.CVE_SCAN_CACHE_DIR,
                                               'gerrit_search'),
                 max_results_per_query: int = 5):
        self.cache_dir = cache_dir
        os.makedirs(cache_dir, exist_ok=True)
        self.max_results_per_query = max_results_per_query

    def search_review_urls(self, bug_id: str) -> List[str]:
        """For a `chromium:NNNN` (or `chromium-p0:NNNN`) bug identifier,
        return up to `max_results_per_query` chromium-review URLs whose
        commit messages reference the bug. Empty on failure."""
        query = self._query_for(bug_id)
        if not query:
            return []
        cache = os.path.join(self.cache_dir,
                             re.sub(r'[^A-Za-z0-9._-]+', '_', query) + '.json')
        items = self._read_or_query(query, cache)
        out: List[str] = []
        for item in items[: self.max_results_per_query]:
            change_num = item.get('_number')
            project = item.get('project', 'chromium/src')
            if change_num:
                out.append(
                    f'https://chromium-review.googlesource.com/c/'
                    f'{project}/+/{change_num}'
                )
        return out

    def _query_for(self, bug_id: str) -> str:
        if bug_id.startswith('chromium:'):
            num = bug_id.split(':', 1)[1]
            # Filter to merged so we get fix commits, not abandoned ones.
            return f'bug:{num}+status:merged'
        if bug_id.startswith('chromium-p0:'):
            # P0 issues used to be referenced as `project-zero/NNNN` in
            # commit messages; modern ones may use `b/NNNN` (buganizer).
            # Both forms are uncommon — try the project-zero one first.
            num = bug_id.split(':', 1)[1]
            return f'message:%22project-zero/{num}%22+status:merged'
        return ''

    def _read_or_query(self, query: str, cache: str) -> List[dict]:
        if os.path.isfile(cache) and os.path.getsize(cache) > 0:
            try:
                with open(cache, 'r', encoding='utf-8') as f:
                    cached = json.load(f)
                if not cached.get('error'):
                    return cached.get('items', [])
            except (OSError, json.JSONDecodeError):
                pass
        url = f'{self.BASE}?q={query}&n={self.max_results_per_query}'
        headers = {'User-Agent': 'vuln-patch/0.1'}
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except (urllib.error.HTTPError, urllib.error.URLError,
                TimeoutError) as e:
            print(f"  ! gerrit search for {query!r} failed: {e}",
                  flush=True)
            return []
        # Gerrit's XSSI prefix.
        body = body.lstrip(")]}'\n").strip()
        try:
            data = json.loads(body)
        except json.JSONDecodeError:
            return []
        with open(cache, 'w', encoding='utf-8') as f:
            json.dump({'items': data}, f)
        return data if isinstance(data, list) else []


class GithubCommitSearcher:
    """Fallback patch-URL resolver: queries GitHub's commit-search API for
    a bug identifier when nothing else returned a commit URL.

    Used as the last resort — if the RCA didn't link the fix and the bug
    tracker page was un-scrapable, GitHub search often finds the fixing
    commit because the commit message references the CVE/bug ID. Rate
    limits are 10 req/min unauthenticated, 30 req/min with `GITHUB_TOKEN`
    in the environment.
    """

    API_URL = 'https://api.github.com/search/commits'

    def __init__(self,
                 cache_dir: str = os.path.join(config.CVE_SCAN_CACHE_DIR,
                                               'github_search'),
                 token: Optional[str] = None,
                 max_results_per_query: int = 5):
        self.cache_dir = cache_dir
        os.makedirs(self.cache_dir, exist_ok=True)
        self.token = token or os.getenv('GITHUB_TOKEN')
        self.max_results_per_query = max_results_per_query

    def search_commit_urls(self, bug_id: str) -> List[str]:
        """Return up to `max_results_per_query` GitHub commit URLs whose
        messages match the bug identifier. Empty on rate-limit/404."""
        query = self._query_for(bug_id)
        if not query:
            return []
        cache = os.path.join(self.cache_dir, self._slug(query) + '.json')
        items = self._read_or_query(query, cache)
        if not items:
            return []
        out: List[str] = []
        for item in items[: self.max_results_per_query]:
            url = item.get('html_url')
            if url:
                out.append(url)
        return out

    def _query_for(self, bug_id: str) -> str:
        """Pick the best search string for a bug identifier."""
        if bug_id.startswith('CVE-'):
            return bug_id
        if bug_id.startswith('chromium-p0:'):
            # Search for the issue tracker URL in commit messages.
            num = bug_id.split(':', 1)[1]
            return f'project-zero/issues/detail?id={num}'
        if bug_id.startswith('chromium:'):
            num = bug_id.split(':', 1)[1]
            return f'crbug.com/{num}'
        if bug_id.startswith('mozilla:'):
            num = bug_id.split(':', 1)[1]
            return f'bug {num}'
        return ''

    def _slug(self, q: str) -> str:
        return re.sub(r'[^A-Za-z0-9._-]+', '_', q)[:200]

    def _read_or_query(self, query: str, cache: str) -> List[dict]:
        if os.path.isfile(cache) and os.path.getsize(cache) > 0:
            try:
                with open(cache, 'r', encoding='utf-8') as f:
                    cached = json.load(f)
                # Re-query on cached errors — a rate-limit 403 from a
                # previous run shouldn't poison future runs that have a
                # working token.
                if not cached.get('error'):
                    return cached.get('items', [])
            except (OSError, json.JSONDecodeError):
                pass
        headers = {
            'User-Agent': 'vuln-patch/0.1',
            # The commit-search endpoint used to require this preview
            # header; GitHub still accepts it and it doesn't hurt on
            # versions that have promoted it out of preview.
            'Accept': 'application/vnd.github.cloak-preview+json',
        }
        if self.token:
            headers['Authorization'] = f'token {self.token}'
        url = f'{self.API_URL}?q={urllib.request.quote(query)}'
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = resp.read().decode('utf-8', errors='replace')
        except urllib.error.HTTPError as e:
            # 403 = rate limit; 422 = bad query. Cache an empty hit so we
            # don't hammer the API for the same query repeatedly.
            print(f"  ! github search for {query!r} failed: HTTP {e.code}",
                  flush=True)
            with open(cache, 'w', encoding='utf-8') as f:
                json.dump({'items': [], 'error': f'HTTP {e.code}'}, f)
            return []
        except (urllib.error.URLError, TimeoutError) as e:
            print(f"  ! github search for {query!r} failed: {e}", flush=True)
            return []
        try:
            data = json.loads(body)
        except json.JSONDecodeError:
            return []
        with open(cache, 'w', encoding='utf-8') as f:
            f.write(body)
        return data.get('items', [])


# --------------------------------------------------------------------------
# The orchestrator

class CodeOverlapChecker:
    """Resolve commit URLs for a bug pair and compute file-level overlap."""

    def __init__(self,
                 rca_dir: str,
                 cache_dir: str = config.CVE_SCAN_CACHE_DIR,
                 github_token: Optional[str] = None,
                 max_commits_per_side: int = 3,
                 use_github_search: bool = True):
        self.rca_dir = rca_dir
        self.fetcher = PatchFetcher(
            cache_dir=os.path.join(cache_dir, 'patches'),
            github_token=github_token,
        )
        self.scraper = BugPageScraper(
            cache_dir=os.path.join(cache_dir, 'bug_pages'),
        )
        self.searcher = GithubCommitSearcher(
            cache_dir=os.path.join(cache_dir, 'github_search'),
            token=github_token,
        ) if use_github_search else None
        self.gerrit_searcher = GerritBugSearcher(
            cache_dir=os.path.join(cache_dir, 'gerrit_search'),
        )
        self.max_commits_per_side = max_commits_per_side
        # Cache patch resolution per bug_id within a run — many candidates
        # share the same later bug.
        self._patches_per_bug: Dict[str, List[Patch]] = {}

    # ----- public ------------------------------------------------------

    def check_pair(self, later: str, prior: str) -> CodeOverlap:
        later_patches  = self._patches_for(later)
        prior_patches  = self._patches_for(prior)
        return self._overlap(later, prior, later_patches, prior_patches)

    # ----- patch resolution per bug -----------------------------------

    def _patches_for(self, bug_id: str) -> List[Patch]:
        if bug_id in self._patches_per_bug:
            return self._patches_per_bug[bug_id]
        urls = [
            (host, url, origin)
            for host, url, origin in self._commit_urls_for(bug_id)
            if not _is_denylisted_repo_url(url)
            # Search-origin github URLs are dominated by random forks /
            # personal trackers that just mention the CVE ID. Demand
            # that those results live under a trusted upstream owner.
            # RCA-extracted / derived / Gerrit / Bugzilla URLs are
            # trusted regardless of host.
            and not (origin == 'search'
                     and host == 'github'
                     and not _is_trusted_github_owner_url(url))
        ][: self.max_commits_per_side]
        patches: List[Patch] = []
        for host, url, origin in urls:
            p = self.fetcher.fetch(host, url, bug_id)
            if p is None or not p.files:
                continue
            # Only apply the source-files-required filter to the noisy
            # github-search fallback. Patches from RCA prose, derived
            # github:owner/repo@sha identifiers, Gerrit, and Bugzilla
            # are canonical fix sources and we trust them even when
            # they only touch tests/metadata.
            if origin == 'search' and not _patch_touches_source_code(p.files):
                continue
            patches.append(p)
        self._patches_per_bug[bug_id] = patches
        return patches

    def _commit_urls_for(self, bug_id: str) -> List[Tuple[str, str, str]]:
        """Return [(host, url, origin)] for a bug identifier. `origin`
        tracks how we found the URL — `'rca'`, `'derived'`, `'scrape'`,
        `'search'` — so callers can apply origin-specific trust
        decisions (e.g. only require source-code-file content for
        search-derived URLs, since RCA/canonical URLs are reliable)."""
        urls: List[Tuple[str, str, str]] = []
        if bug_id.startswith('CVE-'):
            urls.extend(self._urls_from_rca(bug_id))
        elif bug_id.startswith('github:'):
            rest = bug_id.split(':', 1)[1]
            m = re.match(r'([^/]+)/([^@]+)@([0-9a-f]+)', rest)
            if m:
                owner, repo, sha = m.group(1), m.group(2), m.group(3)
                urls.append((
                    'github',
                    f'https://github.com/{owner}/{repo}/commit/{sha}',
                    'derived',
                ))
        elif bug_id.startswith('mozilla:'):
            num = bug_id.split(':', 1)[1]
            urls.append((
                'bugzilla-mozilla',
                f'https://bugzilla.mozilla.org/show_bug.cgi?id={num}',
                'derived',
            ))
            urls.extend((h, u, 'scrape')
                        for h, u in self.scraper.commits_for(bug_id))
        elif bug_id.startswith('chromium'):
            urls.extend((h, u, 'scrape')
                        for h, u in self.scraper.commits_for(bug_id))
            # The static bug-tracker page is JS-only on the new Google
            # issuetracker; fall back to Gerrit's bug-search, which finds
            # the merged fix commit by its `Bug: NNNN` trailer.
            if not urls:
                for review_url in self.gerrit_searcher.search_review_urls(bug_id):
                    urls.append(('chromium-review', review_url, 'search'))

        # Last resort: ask GitHub's commit-search endpoint. Tagged
        # `search` so downstream filters can demand source-code content.
        if not urls and self.searcher is not None:
            for found in self.searcher.search_commit_urls(bug_id):
                urls.append(('github', found, 'search'))

        seen = set()
        deduped: List[Tuple[str, str, str]] = []
        for host, url, origin in urls:
            if url not in seen:
                seen.add(url)
                deduped.append((host, url, origin))
        return deduped

    def _urls_from_rca(self, cve_id: str) -> List[Tuple[str, str, str]]:
        """Find this CVE's RCA file in 0day-RCAs/YYYY/ and pull commit
        URLs out of its prose. Returns 3-tuples tagged origin='rca'."""
        m = re.match(r'CVE-(\d{4})-\d{4,}', cve_id, re.IGNORECASE)
        if not m:
            return []
        year = m.group(1)
        for ext in ('.md', '.html'):
            for sub in (year, ''):
                candidate = os.path.join(
                    self.rca_dir, '0day-RCAs', sub, cve_id + ext,
                )
                if os.path.isfile(candidate):
                    try:
                        with open(candidate, 'r', encoding='utf-8',
                                  errors='replace') as f:
                            pairs = _commit_urls_from_text(f.read())
                        return [(h, u, 'rca') for h, u in pairs]
                    except OSError:
                        continue
        return []

    # ----- overlap computation ----------------------------------------

    @staticmethod
    def _overlap(later: str, prior: str,
                 lp: List[Patch], pp: List[Patch]) -> CodeOverlap:
        # Self-pair detection: drop pairs where both sides resolved to
        # the same upstream fix. Covers four cases:
        #   1. github SHA on both sides (a github:owner/repo@sha "prior"
        #      extracted from the later's own RCA)
        #   2. Gerrit change-number on both sides (when both CVEs link
        #      to the same chromium-review change)
        #   3. Bugzilla bug-number on both sides (one CVE === one bug)
        #   4. Identical canonical commit URLs across hosts
        if _patches_collide(lp, pp):
            return CodeOverlap(
                later=later, prior=prior,
                later_patches=lp, prior_patches=pp,
                status='no_patches',
                note='self-pair: both sides resolved to the same fix',
            )

        later_files = {f for p in lp for f in p.files}
        prior_files = {f for p in pp for f in p.files}

        if not lp and not pp:
            return CodeOverlap(later=later, prior=prior, status='no_patches',
                               note='no patch URLs found for either side')
        if not lp or not pp:
            which = 'later' if not lp else 'prior'
            return CodeOverlap(
                later=later, prior=prior,
                later_patches=lp, prior_patches=pp,
                status='partial',
                note=f'no patch URLs found for {which}',
            )

        raw_overlap = sorted(later_files & prior_files)
        meaningful_overlap = [f for f in raw_overlap
                              if not _is_boilerplate_file(f)]

        if meaningful_overlap:
            return CodeOverlap(
                later=later, prior=prior,
                later_patches=lp, prior_patches=pp,
                overlap_files=meaningful_overlap,
                status='overlap',
            )
        if raw_overlap:
            return CodeOverlap(
                later=later, prior=prior,
                later_patches=lp, prior_patches=pp,
                overlap_files=raw_overlap,
                status='no_overlap',
                note=f'shared files are all boilerplate '
                     f'(changelog/test-expectations/lock files)',
            )
        return CodeOverlap(
            later=later, prior=prior,
            later_patches=lp, prior_patches=pp,
            status='no_overlap',
            note=f'later touches {len(later_files)} file(s), '
                 f'prior touches {len(prior_files)} file(s), '
                 f'disjoint sets',
        )
