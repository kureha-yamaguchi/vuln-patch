#!/usr/bin/env python3
"""
Compute the root-cause neighbourhood of each Project Zero fix, from a real
checkout, and store it as JSON beside the pair.

WHAT THIS CLOSES. The Java front-end computes a reachable-function set per
touched function with fuzz-introspector, and the C/C++ front-end does the same
through `oss_fuzz/callgraph.py` and `oss_fuzz/analysis.py`. The Project Zero
baseline had neither: its evidence was the diff plus whole touched files, and
`_routes_block` sat in `WITHHELD_PIPELINE_EVIDENCE` because a call graph needs
a tree and this dataset stores none. This tool fetches the tree, runs the
front-end's own analyzer, and writes the result:

    pairs/<PAIR>/fix0_region.json
    pairs/<PAIR>/fix1_region.json

A DEPTH-1 FETCH IS CHEAP, AND THAT WAS NOT OBVIOUS. `pairs/README.md` rejects
checkouts because chromium/src is 61 GB. That figure is a full clone with
history. Measured here, `git fetch --depth 1 origin <sha>` costs 33 MB and 12
seconds for v8, 14 MB for freetype, and 974 MB and four minutes for
chromium/src. So the tree is affordable; only the INDEX is the real budget.

THE WORKTREE IS DELETED AFTER EACH FIX. The artifact is the region, which is a
few kilobytes of JSON. Keeping 34 checkouts would cost tens of gigabytes for
nothing. The per-repository object store is kept, because two fixes of one
repository share it.

THE INDEX IS SCOPED, AND THAT IS THE ONE REAL DESIGN CHOICE.
`config.INDEX_FILE_CAP` is 4000 files, and `callgraph._source_files` takes the
4000 SHALLOWEST paths of whatever root it is given. On a large repository that
silently excludes the patched file itself — `third_party/blink/renderer/core/
streams/readable_stream.cc` is six levels deep — and the region then describes
code unrelated to the fix. So this tool picks a root instead of trusting the
cap:

  1. Start at the deepest directory that contains every touched source file.
  2. Walk UP while the source-file count under the candidate stays inside the
     cap.
  3. Index that root.

The patched file is therefore always indexed, and the region is as wide as the
budget allows. The cost is stated rather than hidden: call edges that leave the
chosen subtree are not followed, so a sibling in another subsystem is out of
reach. `region.json` records `index_root` so every region says how wide it was.

PATHS MUST STAY REPOSITORY-RELATIVE. The analyzer resolves a diff's paths
against the root it indexes, so handing it a subtree would break every seed.
This tool therefore builds a pruned mirror: the chosen subtree is hardlinked
into a temporary directory AT ITS ORIGINAL RELATIVE PATH. Hardlinks cost no
disk and no copy time, and `os.walk` treats them as ordinary files.

LABEL-BLIND BY CONSTRUCTION. Each side's region is computed from that side's
own diff and its own tree. Nothing reads `affected_files_fix1` when it computes
`fix0_region.json`. A rule that used the later fix's file list would be reading
the answer.

Usage, from src/db/project_zero/:
    python tools/checkout_region.py --dry_run
    python tools/checkout_region.py
    python tools/checkout_region.py --pair CVE-2021-30551__CVE-2022-1096
    python tools/checkout_region.py --keep_worktree   # to inspect a tree
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                              # noqa: E402
from oss_fuzz.analysis import DiffAnalyzer                  # noqa: E402

DEFAULT_PAIRS = Path(__file__).parent.parent / "pairs"
GERRIT_RESOLVED = Path(__file__).parent.parent / "gerrit_resolved.json"
# Object stores live outside the repository, because they are large and they are
# rebuildable from the network.
DEFAULT_STORE = Path("/datadrive/vuln-patch-checkouts")

#: Repositories a checkout is run for, and why each one is affordable. A
#: repository absent from this list is skipped with its reason printed, so the
#: population of the region experiment is auditable.
FEASIBLE = {
    "/v8/v8": "33 MB per commit, 170 MB tree, 14.5k files",
    "torvalds/linux": "kernel, depth-1 tree is a few hundred MB",
    "skia.googlesource.com/skia": "moderate tree",
    "freetype": "14 MB per commit",
    "msm-4.19": "kernel fork",
    "kernel/common": "android kernel",
    "google-modules/gpu": "small driver tree",
    "WebKit/WebKit": "large but single-row",
}

#: Repositories deliberately left out, with the reason recorded rather than
#: implied by absence.
EXCLUDED = {
    "chromium/src": "974 MB and 4 minutes per commit, and ~350k files, so the "
                    "index root would have to be a narrow subtree",
    "hg.mozilla.org": "Mercurial, so git fetch does not apply",
    "mozilla/gecko-dev": "very large tree for a single row",
}

#: Read-only mirrors. Same map as fetch_context.py, and for the same reason: a
#: git mirror shares the object ids of its origin, so a SHA addresses the same
#: tree in both.
MIRRORS = {
    "git.savannah.gnu.org/git/freetype/freetype2.git":
        "https://github.com/freetype/freetype",
}

# A FULL sha is required, not an abbreviation. `git fetch --depth 1 origin
# <sha>` resolves a 40-character id only; one pair records a 12-character one
# (`v8@e677a6f6b257`), and that side gets no region. Expanding it would need a
# host API lookup, which is not worth a dependency for one row.
_SHA = re.compile(r"^[0-9a-f]{40}$")
_ABBREV = re.compile(r"^[0-9a-f]{7,39}$")
_C_EXTS = (".c", ".h")
_CPP_EXTS = (".cc", ".cpp", ".cxx", ".c", ".h", ".hh", ".hpp", ".hxx")
_SKIP_DIRS = {"third_party", "out", "build", "test", "tests", "testing",
              "node_modules", "docs", "tools"}


# --- selection ---------------------------------------------------------------

def read_pairs(pairs_dir: Path) -> list[tuple[Path, dict]]:
    out = []
    for meta_path in sorted(list(pairs_dir.glob("*/metadata.json"))
                            + list(pairs_dir.glob("*/*/metadata.json"))):
        out.append((meta_path.parent, json.loads(meta_path.read_text())))
    return out


def clone_url(repo_url: str) -> str:
    repo = (repo_url or "").rstrip("/")
    for canonical, mirror in MIRRORS.items():
        if canonical in repo:
            return mirror
    # A repo_url copied from the Gerrit UI carries a `/c/` segment after the
    # host, which is not part of the git path.
    return repo.replace(".googlesource.com/c/", ".googlesource.com/")


def skip_reason(repo_url: str, commit: str) -> str | None:
    """Why this side cannot get a region, or None when it can."""
    for name, why in EXCLUDED.items():
        if name in (repo_url or ""):
            return f"{name} excluded: {why}"
    if not any(f in (repo_url or "") for f in FEASIBLE):
        return f"no checkout rule for {repo_url}"
    if commit and _ABBREV.match(commit):
        return (f"commit {commit!r} is an abbreviated SHA, and a depth-1 "
                f"fetch needs all 40 characters")
    if not (commit and _SHA.match(commit)):
        return f"commit {commit or 'empty'!r} is not a SHA"
    return None


def store_name(repo_url: str) -> str:
    """A short, stable directory name for one repository's object store."""
    url = clone_url(repo_url)
    return re.sub(r"[^A-Za-z0-9]+", "-", url.split("//", 1)[-1]).strip("-")


# --- the checkout ------------------------------------------------------------

def ensure_commit(store: Path, repo_url: str, commit: str) -> bool:
    """Fetch one commit at depth 1 into a shared object store."""
    store.mkdir(parents=True, exist_ok=True)
    if not (store / ".git").exists():
        _git(store, "init", "-q")
        _git(store, "remote", "add", "origin", clone_url(repo_url))
    # Already present from an earlier fix of the same repository.
    if _git(store, "cat-file", "-e", f"{commit}^{{commit}}", check=False) == 0:
        return True
    return _git(store, "fetch", "--depth", "1", "-q", "origin", commit,
                check=False, timeout=2400) == 0


def _git(cwd: Path, *args: str, check: bool = True,
         timeout: int = 600) -> int:
    proc = subprocess.run(["git", *args], cwd=str(cwd), timeout=timeout,
                          capture_output=True, text=True)
    if check and proc.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed: {proc.stderr[:300]}")
    return proc.returncode


def add_worktree(store: Path, commit: str, dest: Path) -> None:
    _git(store, "worktree", "add", "--detach", "-q", str(dest), commit,
         timeout=1800)


def remove_worktree(store: Path, dest: Path) -> None:
    _git(store, "worktree", "remove", "--force", str(dest), check=False)
    shutil.rmtree(dest, ignore_errors=True)


# --- the scoped index root ---------------------------------------------------

def changed_files(patch_text: str) -> list[str]:
    return re.findall(r"^diff --git a/(.*?) b/", patch_text, re.M)


def count_sources(root: Path, exts: tuple) -> int:
    n = 0
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames
                       if d.lower() not in _SKIP_DIRS and not d.startswith(".")]
        n += sum(1 for f in filenames if f.endswith(exts))
        if n > config.INDEX_FILE_CAP * 4:      # far past the cap; stop early
            return n
    return n


def pick_index_root(tree: Path, touched: list[str],
                    exts: tuple) -> tuple[str, int]:
    """`(repo-relative root, source files under it)`.

    Starts at the deepest directory holding every touched file, then walks up
    while the count stays inside the cap. So the patched file is always
    indexed, and the region is as wide as the budget allows."""
    dirs = [os.path.dirname(p) for p in touched] or [""]
    common = os.path.commonpath(dirs) if len(dirs) > 1 else dirs[0]
    best, best_n = common, count_sources(tree / common, exts)
    candidate = common
    while candidate:
        candidate = os.path.dirname(candidate)
        n = count_sources(tree / candidate, exts)
        if n > config.INDEX_FILE_CAP:
            break
        best, best_n = candidate, n
    return best, best_n


def mirror_subtree(tree: Path, root_rel: str, exts: tuple) -> Path:
    """Hardlink the chosen subtree into a temp dir, at its ORIGINAL relative
    path, so the analyzer's repo-relative diff paths still resolve."""
    tmp = Path(tempfile.mkdtemp(prefix="pz-index-"))
    src_root = tree / root_rel
    for dirpath, dirnames, filenames in os.walk(src_root):
        dirnames[:] = [d for d in dirnames
                       if d.lower() not in _SKIP_DIRS and not d.startswith(".")]
        for fn in filenames:
            if not fn.endswith(exts):
                continue
            real = Path(dirpath) / fn
            rel = real.relative_to(tree)
            dest = tmp / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            try:
                os.link(real, dest)
            except OSError:
                shutil.copy2(real, dest)
    return tmp


# --- one side of one pair ----------------------------------------------------

def language_of(touched: list[str]) -> str:
    return "c" if all(p.endswith((".c", ".h")) for p in touched) else "c++"


def region_for(pair_dir: Path, label: str, commit: str, repo_url: str,
               store_root: Path, keep_worktree: bool) -> dict | None:
    patch_path = pair_dir / f"{label}.patch"
    if not patch_path.exists():
        print(f"  SKIP  {pair_dir.name}/{label}  (no patch file)")
        return None
    patch_text = patch_path.read_text(errors="replace")
    touched = [p for p in changed_files(patch_text)
               if p.endswith(_CPP_EXTS)]
    if not touched:
        print(f"  SKIP  {pair_dir.name}/{label}  (no C/C++ file touched)")
        return None

    store = store_root / store_name(repo_url)
    if not ensure_commit(store, repo_url, commit):
        print(f"  FAIL  {pair_dir.name}/{label}  (cannot fetch {commit[:12]})")
        return None

    work = store_root / f"wt-{store_name(repo_url)}-{commit[:12]}"
    mirror = None
    try:
        remove_worktree(store, work)
        add_worktree(store, commit, work)
        lang = language_of(touched)
        exts = _C_EXTS if lang == "c" else _CPP_EXTS
        present = [p for p in touched if (work / p).exists()]
        if not present:
            print(f"  SKIP  {pair_dir.name}/{label}  "
                  f"(no touched file exists at that commit)")
            return None
        root_rel, n_files = pick_index_root(work, present, exts)
        mirror = mirror_subtree(work, root_rel, exts)
        ctx = DiffAnalyzer(language=lang).analyze(patch_text, str(mirror))
        out = ctx.as_dict()
        out.update({
            "index_root": root_rel or "<repo root>",
            "index_root_source_files": n_files,
            "language": lang,
            "touched_files_present": present,
            "touched_files_missing": sorted(set(touched) - set(present)),
        })
        print(f"  OK    {pair_dir.name}/{label}  root={root_rel or '<root>'} "
              f"({n_files} files) region={len(ctx.root_cause_reachable)} "
              f"seeds={ctx.seeds_resolved[0]}/{ctx.seeds_resolved[1]} "
              f"src={ctx.reachable_source}")
        return out
    except Exception as exc:                    # a parse or git failure
        print(f"  FAIL  {pair_dir.name}/{label}  "
              f"{type(exc).__name__}: {str(exc)[:160]}")
        return None
    finally:
        if mirror is not None:
            shutil.rmtree(mirror, ignore_errors=True)
        if not keep_worktree:
            remove_worktree(store, work)


def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pairs_dir", type=Path, default=DEFAULT_PAIRS)
    ap.add_argument("--store", type=Path, default=DEFAULT_STORE,
                    help=f"object stores and worktrees (default: "
                         f"{DEFAULT_STORE})")
    ap.add_argument("--pair", help="one pair only, by directory name")
    ap.add_argument("--force", action="store_true",
                    help="recompute a region that is already on disk")
    ap.add_argument("--keep_worktree", action="store_true",
                    help="leave each checkout in place, to inspect it")
    ap.add_argument("--dry_run", action="store_true",
                    help="list what would be computed and stop")
    args = ap.parse_args()

    resolved = (json.loads(GERRIT_RESOLVED.read_text())
                if GERRIT_RESOLVED.exists() else {})
    pairs = read_pairs(args.pairs_dir)
    if args.pair:
        pairs = [(d, m) for d, m in pairs
                 if str(d.relative_to(args.pairs_dir)) == args.pair]
        if not pairs:
            print(f"no pair named {args.pair!r}")
            return 2

    todo, skipped = [], []
    for pair_dir, meta in pairs:
        for label, ck, rk in (("fix0", "fix0_commit", "repo_url"),
                              ("fix1", "fix1_commit", "later_repo_url")):
            raw = str(meta.get(ck) or "")
            commit = raw if _SHA.match(raw) else resolved.get(raw, "")
            repo_url = meta.get(rk) or ""
            why = skip_reason(repo_url, commit)
            if why:
                skipped.append((pair_dir.name, label, why))
                continue
            out = pair_dir / f"{label}_region.json"
            if out.exists() and not args.force:
                skipped.append((pair_dir.name, label, "region already on disk"))
                continue
            todo.append((pair_dir, label, commit, repo_url, out))

    print(f"pairs on disk : {len(pairs)}")
    print(f"to compute    : {len(todo)}")
    print(f"skipped       : {len(skipped)}")
    for name, label, why in skipped:
        print(f"  - {name}/{label}: {why}")
    if args.dry_run:
        print("\n--dry_run — no fetch, no index.")
        for pair_dir, label, commit, repo_url, _out in todo:
            print(f"  would compute {pair_dir.name}/{label} "
                  f"from {clone_url(repo_url)} at {commit[:12]}")
        return 0

    args.store.mkdir(parents=True, exist_ok=True)
    ok = 0
    for pair_dir, label, commit, repo_url, out in todo:
        region = region_for(pair_dir, label, commit, repo_url, args.store,
                            args.keep_worktree)
        if region is not None:
            out.write_text(json.dumps(region, indent=2))
            ok += 1

    print("\n" + "=" * 60)
    print(f"regions written : {ok} of {len(todo)}")
    print(f"object stores   : {args.store}")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
