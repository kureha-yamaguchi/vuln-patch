"""Read one Project Zero fix, and strip the answer out of it.

The Project Zero dataset is a set of variant pairs. Each pair holds two
upstream security fixes for one root cause:

  * `fix0` is the PRIOR fix. It shipped, and it left a sibling bug behind.
    A later CVE reported that sibling. So `fix0` is an overfitting patch.
  * `fix1` is the LATER fix. It removed what `fix0` missed. No third CVE
    followed it, so `fix1` is the correct patch of the pair.

That convention is the label. It is also written all over the data, so a
renderer that reads `metadata.json` or a raw `.patch` file hands the model the
answer. This module is the one place that reads either. It returns two views,
and only one of them is safe to render:

  * `PairRecord` is the SELECTOR view. It carries the two CVE ids and the two
    commits. The queue builder and the bug-kind classifier read it. Nothing
    renders it.
  * `Fix` is the CLEAN view. It carries the diff, the touched files, the
    fetched source and the codebase label. `evidence` renders this one.

Six leak channels exist, and each one has its own rule here.

  1. The directory name is `<PRIOR>__<LATER>`, so it names the later CVE. The
     clean view carries `fix_id` instead — the first eight hex digits of the
     commit digest. Two pairs that share one fix therefore share one id, which
     is also what the queue builder needs for its dedup.
  2. The file name is `fix0.patch` or `fix1.patch`, which is the label spelled
     out. The clean view has no file name.
  3. Most `metadata.json` fields state the verdict outright: `relationship_kind`
     is one of `incomplete_fix`, `same_root_cause` or `one_extends_other`, and
     `deep_reasoning` explains the choice in prose. `read_pairs` names the two
     fields it keeps, `codebase` and `software`, and leaves every other field
     behind. So a field the harvester adds later cannot leak by default.
  4. `fix0_date` and `fix1_date` order the two fixes in time. Neither view
     carries a date.
  5. The commit message is the richest channel of all. Real examples from this
     dataset: "This is CVE-2020-15999", "commit 5eeb2ca0 upstream",
     "Bug 1607443 - Fix some alias sets", and a `Fixes:` tag that names the
     commit being repaired. Message presence is not even symmetric: 49 of the
     86 patch files carry one. So the clean view starts at the first
     `diff --git` line and drops the message whole.
  6. The diff BODY still leaks. One patch adds a ChangeLog entry that reads
     "This is CVE-2020-15999" and carries the release date. `scrub` masks
     every CVE id, tracker id, ISO date and blob hash in the body, and in the
     fetched source as well.

Rule 5 costs real evidence, and the README records it as withheld. The
alternative was worse: the message asymmetry alone would let a judge separate
the two classes without reading one line of code.

`Fix.label` exists, because a record needs its ground truth to be scored. It
reaches the record and the confusion matrix, never a rendered block.
`tests/test_llmjudge_pz.py` asserts that.
"""
import hashlib
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple

REPO = Path(__file__).resolve().parents[3]
PAIRS = REPO / 'src' / 'db' / 'project_zero' / 'pairs'

#: The two sides of a pair, and the label each one carries.
WHICH = ('fix0', 'fix1')
LABEL_BY_WHICH = {'fix0': 'overfitting', 'fix1': 'correct'}

#: Cap on the fetched source carried per fix, across all of its files. A large
#: fix would otherwise dominate the prompt and cost.
MAX_SOURCE_CHARS = 20000

# The clean view begins here. Everything before it is the commit message.
_FIRST_DIFF = re.compile(r'^diff --git ', re.M)

# Files whose content is never useful as source evidence.
_SKIP_SOURCE_SUFFIXES = ('.md', '.txt', '.json', '.gn', '.gni', '.mk')

#: Metadata lines dropped wherever they appear, not only in the leading
#: message. Four Bugzilla attachments in this dataset concatenate several
#: Mercurial changeset patches, so a second `# HG changeset patch` header sits
#: AFTER the first `diff --git` line. Every pattern here anchors at column 0,
#: and a diff content line always begins with `+`, `-` or a space, so none of
#: these can match code.
_DROP_LINE = re.compile(
    r'^(?:'
    r'# (?:HG changeset patch|User |Date |Node ID |Parent )'
    r'|From [0-9a-f]{7,40} '
    r'|commit [0-9a-f]{7,40}'
    r'|(?:Date|Subject|Change-Id|Reviewed-on|Reviewed-by|Commit-Queue'
    r'|Auto-Submit|Cr-Commit-Position|Cr-Original-Commit-Position'
    r'|Differential Revision|Fixes|Bug|Fixed|Link|Signed-off-by'
    r'|Reported-by|Cc|Acked-by|Tested-by|Reviewed-and-tested-by):'
    r')')

#: Diff header lines, where a 6-to-8 digit run is a tracker id rather than a
#: numeric constant. v8 names a regression test after the bug it covers, for
#: example `test/mjsunit/compiler/regress-crbug-1228407.js`. A bug id rises
#: over time, so it orders the two fixes of a pair.
_PATH_LINE = re.compile(r'^(?:diff --git |--- |\+\+\+ |rename |similarity )')
_PATH_DIGITS = re.compile(r'\d{6,8}')

#: Body substitutions, applied to the diff and to every fetched source file.
#: Each one masks an identifier that could name the fix's position in the pair.
_SUBSTITUTIONS: Tuple[Tuple[re.Pattern, str], ...] = (
    # A CVE id names the vulnerability, and in a pair the later id is the
    # label. One patch in this dataset carries one inside a ChangeLog hunk.
    (re.compile(r'CVE-\d{4}-\d{4,7}'), 'CVE-MASKED'),
    # An ISO date orders the two fixes. The same ChangeLog hunk carries one.
    (re.compile(r'\b\d{4}-\d{2}-\d{2}\b'), 'DATE-MASKED'),
    # Tracker ids: crbug.com/1234, crbug.com/v8/8854, crbug/448711,
    # crbug-1228407, chromium:1234, Bug 1607443, #59308. Any word that
    # begins `crbug` is a tracker reference, so the whole token goes rather
    # than a guessed shape for the part after it.
    (re.compile(r'(?i)\bcrbug[\w./:-]*'), 'BUG-MASKED'),
    (re.compile(r'(?i)\bregress[-_]\d{5,8}'), 'regress-BUG-MASKED'),
    (re.compile(r'(?i)\b(?:chromium|bug|issue)\s*[:#]\s*\d{3,8}'),
     'BUG-MASKED'),
    (re.compile(r'(?i)\bbug\s+\d{4,8}\b'), 'BUG-MASKED'),
    (re.compile(r'#\d{5,8}\b'), 'BUG-MASKED'),
    # A blob hash pair is resolvable to a commit, so it orders the fixes.
    (re.compile(r'^(index )[0-9a-f]{6,40}\.\.[0-9a-f]{6,40}', re.M),
     r'\1HASH-MASKED..HASH-MASKED'),
)


@dataclass(frozen=True)
class PairRecord:
    """The selector view of one variant pair. Never rendered."""
    name: str                 # directory name, relative to PAIRS
    path: Path
    prior_cve: str
    later_cve: str
    fix0_commit: str
    fix1_commit: str
    codebase: str
    software: str

    def commit(self, which: str) -> str:
        return self.fix0_commit if which == 'fix0' else self.fix1_commit


@dataclass(frozen=True)
class Fix:
    """The clean view of one fix. Safe to render."""
    fix_id: str
    label: str                # selector only; asserted absent from prompts
    codebase: str
    software: str
    touched_files: Tuple[str, ...]
    diff: str
    sources: Tuple[Tuple[str, str], ...]     # (path, scrubbed text)
    scrub_report: Dict


class FixUnavailable(Exception):
    """The fix cannot be read. `status` names which of the two reasons."""

    def __init__(self, status: str, detail: str):
        super().__init__(f'{status}: {detail}')
        self.status = status
        self.detail = detail


# --- the selector view -------------------------------------------------------

def read_pairs(pairs_dir: Path = PAIRS) -> List[PairRecord]:
    """Every pair that carries a `metadata.json`, in directory order.

    One pair nests one level deeper than the rest (`github-v8/v8@...`), so
    both depths are searched. A `name` therefore holds a slash sometimes."""
    out: List[PairRecord] = []
    for meta_path in sorted(list(pairs_dir.glob('*/metadata.json'))
                            + list(pairs_dir.glob('*/*/metadata.json'))):
        meta = json.loads(meta_path.read_text())
        pair_dir = meta_path.parent
        out.append(PairRecord(
            name=str(pair_dir.relative_to(pairs_dir)),
            path=pair_dir,
            prior_cve=meta.get('prior_cve') or '',
            later_cve=meta.get('later_cve') or '',
            fix0_commit=str(meta.get('fix0_commit') or ''),
            fix1_commit=str(meta.get('fix1_commit') or ''),
            # The only two metadata fields that reach the clean view. Every
            # other field is left behind here, so a field the harvester adds
            # later cannot leak by default.
            codebase=meta.get('codebase') or 'unknown',
            software=meta.get('software') or 'unknown',
        ))
    return out


def fix_id(commit: str) -> str:
    """A stable opaque id for one fix, derived from its commit alone.

    Two pairs that share a fix get one id, so the queue builder can collapse
    them. The pair directory name never contributes, because it names the
    later CVE."""
    return 'pz-' + hashlib.sha256(commit.encode()).hexdigest()[:8]


# --- the firewall crossing ---------------------------------------------------

def clean_view(pair: PairRecord, which: str, *,
               max_source_chars: int = MAX_SOURCE_CHARS) -> Fix:
    """The clean view of one side of one pair.

    Raises `FixUnavailable` when the patch file is missing, or when the diff
    names no file."""
    if which not in WHICH:
        raise ValueError(f'which must be one of {WHICH}, not {which!r}')
    patch_path = pair.path / f'{which}.patch'
    if not patch_path.exists():
        raise FixUnavailable('no_patch_file', str(patch_path))

    raw = patch_path.read_text(errors='replace')
    body, message_chars = drop_commit_message(raw)
    body, dropped_lines = drop_metadata_lines(body)

    # Two sets of paths, and the split matters. The REAL paths address the
    # files on disk. The masked ones go in the prompt, because a v8 regression
    # test is named after the bug it covers, and a bug number rises over time.
    real_files = tuple(changed_files(body))
    if not real_files:
        raise FixUnavailable('no_changed_files', str(patch_path))
    diff, diff_masked = scrub(body)
    files = tuple(scrub_path(path) for path in real_files)

    sources, source_masked, source_files = _read_sources(
        pair.path / f'{which}_context', real_files, max_source_chars)

    return Fix(
        fix_id=fix_id(pair.commit(which)),
        label=LABEL_BY_WHICH[which],
        codebase=pair.codebase,
        software=pair.software,
        touched_files=files,
        diff=diff,
        sources=sources,
        scrub_report={
            'commit_message_chars_dropped': message_chars,
            'metadata_lines_dropped': dropped_lines,
            'identifiers_masked_in_diff': diff_masked,
            'identifiers_masked_in_source': source_masked,
            'source_files_read': source_files,
            'source_chars': sum(len(t) for _, t in sources),
        },
    )


def drop_commit_message(patch_text: str) -> Tuple[str, int]:
    """The diff from its first `diff --git` line, and the chars dropped.

    Every one of the 86 patch files in this dataset carries such a line, so
    the fallback below is a guard rather than a path taken."""
    m = _FIRST_DIFF.search(patch_text)
    if m is None:
        return patch_text, 0
    return patch_text[m.start():], m.start()


def drop_metadata_lines(text: str) -> Tuple[str, int]:
    """Drop every commit-metadata line, and the count dropped.

    `drop_commit_message` removes the LEADING message. This removes an
    embedded one. Four Bugzilla attachments here hold several changeset
    patches in one file, so their second header sits inside the diff."""
    kept = [ln for ln in text.splitlines() if not _DROP_LINE.match(ln)]
    dropped = len(text.splitlines()) - len(kept)
    return '\n'.join(kept) + '\n', dropped


def scrub(text: str) -> Tuple[str, int]:
    """Mask every identifier that could order the two fixes of a pair."""
    masked = 0
    for pattern, replacement in _SUBSTITUTIONS:
        text, n = pattern.subn(replacement, text)
        masked += n
    lines = []
    for line in text.splitlines():
        if _PATH_LINE.match(line):
            line, n = _PATH_DIGITS.subn('BUG-MASKED', line)
            masked += n
        lines.append(line)
    return '\n'.join(lines) + '\n', masked


def scrub_path(path: str) -> str:
    """One file path, masked exactly as its diff header line is.

    The two must agree. A path masked in the diff and unmasked in the
    `touched_files` block would hand back the identifier the diff just hid."""
    for pattern, replacement in _SUBSTITUTIONS:
        path = pattern.sub(replacement, path)
    return _PATH_DIGITS.sub('BUG-MASKED', path)


def changed_files(patch_text: str) -> List[str]:
    """The files a diff touches, read from its own `diff --git` lines.

    Read from the diff, not from `affected_files_fix0` / `affected_files_fix1`.
    Two metadata fields are two chances for the two classes to differ in a way
    that has nothing to do with the code."""
    return re.findall(r'^diff --git a/(.*?) b/', patch_text, re.M)


def _read_sources(context_dir: Path, real_files: Tuple[str, ...],
                  cap: int) -> Tuple[Tuple[Tuple[str, str], ...], int, int]:
    """The fetched source of the touched files, scrubbed and capped.

    `real_files` addresses the disk. The returned paths are masked, because
    they go into the prompt."""
    if not context_dir.is_dir():
        return (), 0, 0
    out: List[Tuple[str, str]] = []
    masked = 0
    used = 0
    for path in real_files:
        if path.endswith(_SKIP_SOURCE_SUFFIXES):
            continue
        src = context_dir / path
        if not src.is_file():
            continue
        text, n = scrub(src.read_text(errors='replace'))
        masked += n
        room = cap - used
        if room <= 0:
            break
        if len(text) > room:
            text = text[:room] + '\n/* ... (truncated) */\n'
        used += len(text)
        out.append((scrub_path(path), text))
    return tuple(out), masked, len(out)
