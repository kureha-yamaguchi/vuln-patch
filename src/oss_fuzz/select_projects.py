#!/usr/bin/env python3
"""Pick the OSS-Fuzz projects a sweep should run, newest disclosure first.

Replaces the hand-maintained ``suites/ossfuzz_cpp20.projects`` list: nobody has
to keep a list up to date, and a sweep records what it selected and why.

Two orders, ``--order recent`` (the default) and ``--order shuffle``.

**recent** ranks the whole eligible pool by the publication date of its newest
usable OSV record and takes the top ``-n``. Recency is not a tie-breaker here,
it is the point: OSS-Fuzz's build recipe for a project moves on, the vulnerable
commit does not, and the older the bug the likelier it is that today's build.sh
compiles a harness file that did not exist back then. llamacpp in the 20260811
sweep is the case in point -- a September 2024 checkout built with a build.sh
that names ``fuzzers/fuzz_json_to_grammar.cpp``, which fails before a harness of
ours is even involved. Newest-first spends sweep slots where that skew is
smallest. Note this is a *project* ordering; within a project the driver already
walks records newest-first (``osv.rank_records``).

**shuffle** is the old behaviour, kept for a sweep that wants an unbiased sample
of the ecosystem rather than the freshest bugs: the same seed and the same
OSS-Fuzz checkout always yield the same projects.

Eligibility is not re-derived here. ``OssFuzz.check_support`` is the pipeline's
own answer to "can this front-end drive that project", covering language,
fuzzing engine, sanitizer, ``main_repo`` and the Dockerfile ``WORKDIR`` rule
that makes ``helper.py`` refuse a local checkout. Sampling over anything looser
would spend sweep slots on projects that provably cannot build, which reads as
a pipeline failure rather than what it is. The one filter added on top is
``disabled: true``, which OSS-Fuzz uses for projects it has stopped building.

Those checks all read files on disk, so they cannot know that a repo has been
deleted or that a project has no disclosed bugs -- both of which only surface an
hour into a sweep. Two probes ask the outside world: ``repo_is_gone`` and
``no_usable_bug``. Each caught one project in the 20260811 sweep, and neither
would have caught the other's: cryptofuzz has 19 usable OSV records behind a
repo URL that 404s, capnproto has a healthy repo and no OSV records at all. A
probe that cannot get an answer keeps the project -- see ``repo_is_gone``.

Under ``--order recent`` the OSV probe is not a probe at all but the ranking
itself, asked of every eligible project rather than only the sampled ones: a
project with no usable record simply has no date to rank on. Only the repo probe
still runs per candidate, and only until ``-n`` are accepted.

Reproducibility has a boundary worth stating, and it differs by order. A shuffle
sample is a function of the seed *and* of the projects/ tree, so pulling the
OSS-Fuzz checkout can change it. A recent selection is a function of that tree
*and of OSV's contents on the day*, which move without anyone touching this
repo -- so it is reproducible only through the run's own projects.list, which is
why a resumed sweep reuses that file rather than re-selecting. The provenance
header records the checkout commit and each pick's record date for exactly this
reason; quote them when a run is reported.

Both orders take a prefix of one ordering rather than sampling, so raising -n
extends the previous selection instead of replacing it: the first 20 of a run of
25 are the run of 20.

Usage:
    uv run -m oss_fuzz.select_projects                  # 20 C++ projects, newest bugs first
    uv run -m oss_fuzz.select_projects -n 5             # the top 5
    uv run -m oss_fuzz.select_projects --order shuffle --seed 7
    uv run -m oss_fuzz.select_projects --language c,c++ --quiet
"""

import argparse
import os
import random
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from typing import Callable, List, Optional, Tuple

from oss_fuzz.osv import select_from_records
from oss_fuzz.ossfuzz import OssFuzz

DEFAULT_COUNT = 20
DEFAULT_SEED = 42
DEFAULT_LANGUAGE = "c++"
DEFAULT_ORDER = "recent"


# OSS-Fuzz's own tutorial and CI fixtures, not real targets: each has a
# fake@example.com contact, points main_repo at the oss-fuzz repo itself, or is
# a stand-in built from someone else's source ('bad_example' is zlib, used to
# demonstrate a misconfigured project). 'vulnerable-project' in particular ships
# planted bugs, so letting a seed pick it would quietly contaminate a sweep
# meant to measure real-world findings.
#
# Not every *-example dir belongs here: 'fuzztest-example' is the real
# google/fuzztest integration with a real maintainer, so it stays in the pool.
TEST_FIXTURES = frozenset({
    "example", "cifuzz-example", "vulnerable-project", "bad_example"})


def clonable_with_git(main_repo: str) -> bool:
    """Whether ``git clone <main_repo>`` could work.

    ``check_support`` only asks that main_repo is non-empty, but ``clone_source``
    runs ``git clone`` on it, so a Mercurial or Subversion URL fails the moment
    the project is picked. Ten C++ projects are like this -- firefox, nss and
    mercurial (hg), graphicsmagick (Heptapod, an hg forge), and xerces-c, lame,
    xvid, freeimage, libteken (svn) -- and a seed landing on one spends a sweep
    slot on a clone that cannot succeed.

    Deliberately narrow, keying off VCS markers rather than trying to allowlist
    git hosts: Gitiles (*.googlesource.com), Gitea, bare git:// and self-hosted
    paths like bearssl.org/git/ all serve real git and must keep passing --
    Gitiles alone accounts for 21 of the 26 non-GitHub URLs in the pool.

    URL sniffing cannot be airtight; an hg or svn URL with no marker in it would
    still slip through. That costs one sweep slot (the run records a failed
    clone and moves on), not the sweep.
    """
    from urllib.parse import urlparse
    parsed = urlparse(main_repo)
    host, path = parsed.netloc.lower(), parsed.path.lower()
    return not (host.startswith("hg.") or host.startswith("svn.")
                or "heptapod" in host
                or "/svn/" in path or path.rstrip("/").endswith("/repo/hg"))


# Long enough for a slow forge, short enough that a whole sweep's preflight
# stays seconds: a reachable repo answers in well under one.
_PROBE_TIMEOUT = 20

# ``ls-remote`` exits 128 for a deleted repo and for a DNS failure alike, so the
# message is the only thing that separates them. These mean the URL will never
# clone; anything else is treated as "could not tell". Both GitHub and GitLab
# report a missing-or-private repo by asking for a username, which with prompts
# disabled surfaces as 'could not read Username' — verified against a deleted
# repo on each.
_REPO_GONE_RES = (
    re.compile(r"could not read Username", re.IGNORECASE),
    re.compile(r"repository .*not found|repository not found", re.IGNORECASE),
    re.compile(r"Authentication failed", re.IGNORECASE),
    re.compile(r"access denied|permission denied", re.IGNORECASE),
)


def repo_is_gone(main_repo: str) -> Optional[str]:
    """Why ``main_repo`` can never be cloned, or None to keep the project.

    ``git ls-remote`` asks the server for a ref listing and downloads no
    objects, so this costs a fraction of a second. cryptofuzz's project.yaml
    still points at github.com/guidovranken/cryptofuzz, which 404s; the sweep
    found out an hour in, from a clone that died mid-run.

    Two git settings matter. Prompts off, or a missing repo *hangs* waiting for
    a username instead of failing. Credential helpers off, because a stale token
    in ~/.git-credentials answers that prompt with 'Invalid username or token'
    and buries the real 404 — which is exactly what the 20260811 log shows.

    Returns None when the answer is unclear. A timeout or a DNS failure is not
    evidence that a repo is gone, and dropping a project on one flaky moment
    would silently reshape the sweep — a worse failure than the one this fixes.
    """
    try:
        proc = subprocess.run(
            ["git", "-c", "credential.helper=", "ls-remote", "--exit-code",
             main_repo, "HEAD"],
            capture_output=True, text=True, timeout=_PROBE_TIMEOUT,
            env={**os.environ, "GIT_TERMINAL_PROMPT": "0"})
    except (OSError, subprocess.SubprocessError):
        return None
    if proc.returncode == 0:
        return None
    for rx in _REPO_GONE_RES:
        if rx.search(proc.stderr):
            return f"main_repo is unreachable ({main_repo}): {rx.search(proc.stderr).group(0)}"
    return None


def no_usable_bug(project: str, fetch: Callable[[str], List[dict]]
                  ) -> Optional[str]:
    """Why ``project`` has no bug to work on, or None to keep it.

    One OSV query, ~0.3s. capnproto is in the pool, builds fine and has a live
    repo, but OSV holds no record for it at all, so a sweep slot on it can only
    ever report 'no-target'.

    Deliberately the same question the pipeline asks at step 4 and no narrower:
    a public record with a fix commit. Whether the fix *diff* touches C/C++
    source needs a clone, so projects will still stop there — this only removes
    the ones that were never going to start. An unreachable OSV keeps the
    project, for the reason given in ``repo_is_gone``.
    """
    try:
        records = fetch(project)
    except Exception:
        return None
    if select_from_records(project, records) is None:
        return (f"no OSV record with a fix commit "
                f"({len(records)} record(s) considered)")
    return None


def make_repo_probe(of: OssFuzz) -> Callable[[str], Optional[str]]:
    """The repo half alone: why ``project``'s main_repo can never be cloned.

    Split out because ``--order recent`` has already asked OSV about every
    project to build its ranking, so re-asking per candidate would be a second
    round trip for an answer already in hand.
    """
    def probe(project: str) -> Optional[str]:
        main_repo = (of.project_yaml(project).get("main_repo") or "").strip()
        return repo_is_gone(main_repo) if main_repo else None
    return probe


def make_probe(of: OssFuzz, fetch: Callable[[str], List[dict]]
               ) -> Callable[[str], Optional[str]]:
    """Both probes as one callable: why to drop ``project``, or None to keep it.

    Repo first — it is the cheaper question, and a dead repo makes the OSV one
    moot.
    """
    repo = make_repo_probe(of)
    def probe(project: str) -> Optional[str]:
        return repo(project) or no_usable_bug(project, fetch)
    return probe


def eligible_projects(of: OssFuzz, languages: Tuple[str, ...],
                      sanitizer: str, engine: str) -> Tuple[List[str], dict]:
    """Every project this front-end could actually drive, plus reject tallies.

    The tallies are what make a surprising universe size explainable ("411 of
    431") instead of something to go and re-derive by hand.
    """
    rejected = {"language": 0, "disabled": 0, "unsupported": 0,
                "not-git": 0, "fixture": 0}
    keep: List[str] = []
    # native_only=True is just the cheap pre-filter; the language check below
    # is the narrower one, since this front-end's 'native' spans c and c++.
    for name in of.list_projects(native_only=True):
        if name in TEST_FIXTURES:
            rejected["fixture"] += 1
            continue
        info = of.project_yaml(name)
        language = str(info.get("language", DEFAULT_LANGUAGE)).lower()
        if language not in languages:
            rejected["language"] += 1
            continue
        # 'disabled' is a bare YAML scalar here, so it arrives as a string.
        if str(info.get("disabled", "")).lower() == "true":
            rejected["disabled"] += 1
            continue
        if not of.check_support(name, sanitizer, engine).supported:
            rejected["unsupported"] += 1
            continue
        if not clonable_with_git(info.get("main_repo") or ""):
            rejected["not-git"] += 1
            continue
        keep.append(name)
    return keep, rejected


def shuffled(projects: List[str], seed: int) -> List[str]:
    """The eligible projects in the order a sweep consumes them.

    Sorted first so the result depends on the set of eligible projects and not
    on the order the filesystem happened to hand them over.
    """
    pool = sorted(projects)
    random.Random(seed).shuffle(pool)
    return pool


def select(projects: List[str], count: int, seed: int) -> List[str]:
    """A deterministic sample of ``count`` projects."""
    return shuffled(projects, seed)[:count]


def select_probed(projects: List[str], count: int, seed: int,
                  probe: Callable[[str], Optional[str]]
                  ) -> Tuple[List[str], List[Tuple[str, str]]]:
    """The first ``count`` projects in shuffle order that ``probe`` accepts,
    plus the (project, why) pairs it rejected.

    Walking the same order the unprobed sample uses is what keeps ``-n``
    extending rather than replacing a selection: skipping an entry never
    reorders the ones after it, so the first 5 of a seed-42 run of 20 are still
    the seed-42 run of 5. The sample is now a function of the seed, the checkout
    *and* the state of the outside world — one input more than before, which the
    provenance header says out loud.
    """
    chosen: List[str] = []
    dropped: List[Tuple[str, str]] = []
    for name in shuffled(projects, seed):
        if len(chosen) >= count:
            break
        why = probe(name)
        if why:
            dropped.append((name, why))
            continue
        chosen.append(name)
    return chosen, dropped


# One OSV query per *eligible* project, not per sampled one: 378 of them at
# ~0.3s each is two minutes of preflight serially, so they go out concurrently.
# Modest fan-out on purpose — OSV is a free public API and this is preflight,
# not the work. 12 answered in under a second when this was measured.
_RANK_WORKERS = 12


def rank_by_recency(projects: List[str], fetch: Callable[[str], List[dict]],
                    workers: int = _RANK_WORKERS
                    ) -> Tuple[List[Tuple[str, str, str]], List[Tuple[str, str]]]:
    """``(project, published, osv_id)`` newest first, plus what was dropped.

    Ties break on project name so the order is a function of OSV's contents and
    nothing else — two projects disclosed the same day must not swap places
    because the thread pool finished them in a different sequence.

    A project OSV answers for but has no usable record on is dropped, the same
    call ``no_usable_bug`` makes. A project OSV *fails* to answer for is kept
    with an empty date, which sorts it to the tail: fail-open, as everywhere
    else here, but ranked last rather than ahead of projects with a real date,
    since an outage is not evidence of a fresh bug.
    """
    def ask(project: str) -> Tuple[str, str, str, Optional[str]]:
        try:
            records = fetch(project)
        except Exception:                            # fail open; see docstring
            return project, "", "", None
        target = select_from_records(project, records)
        if target is None:
            return project, "", "", (f"no OSV record with a fix commit "
                                     f"({len(records)} record(s) considered)")
        return project, (target.published or ""), (target.osv_id or ""), None

    with ThreadPoolExecutor(max_workers=max(1, workers)) as pool:
        answers = list(pool.map(ask, projects))

    dropped = [(name, why) for name, _, _, why in answers if why]
    ranked = [(name, published, osv_id)
              for name, published, osv_id, why in answers if not why]
    ranked.sort(key=lambda r: r[0])                  # tie-break, then...
    ranked.sort(key=lambda r: r[1], reverse=True)    # ...newest first (stable)
    return ranked, dropped


def select_recent(projects: List[str], count: int,
                  fetch: Callable[[str], List[dict]],
                  probe: Callable[[str], Optional[str]],
                  workers: int = _RANK_WORKERS
                  ) -> Tuple[List[Tuple[str, str, str]], List[Tuple[str, str]]]:
    """The ``count`` newest-disclosed projects ``probe`` accepts, and the rest.

    ``probe`` is the repo probe only — the OSV question was answered by the
    ranking. It is asked in rank order and only until ``count`` are accepted, so
    a preflight costs one ls-remote per candidate reached, not per project.
    """
    ranked, dropped = rank_by_recency(projects, fetch, workers)
    chosen: List[Tuple[str, str, str]] = []
    for entry in ranked:
        if len(chosen) >= count:
            break
        why = probe(entry[0])
        if why:
            dropped.append((entry[0], why))
            continue
        chosen.append(entry)
    return chosen, dropped


def main(argv: List[str] = None) -> int:
    ap = argparse.ArgumentParser(
        description="Reproducibly sample OSS-Fuzz projects for a sweep.")
    ap.add_argument("-n", "--count", type=int, default=DEFAULT_COUNT,
                    help=f"projects to select (default {DEFAULT_COUNT})")
    ap.add_argument("--order", choices=("recent", "shuffle"), default=DEFAULT_ORDER,
                    help="'recent' (default) takes the projects whose newest "
                         "usable OSV record is the most recently published; "
                         "'shuffle' takes a seeded random sample")
    ap.add_argument("--seed", type=int, default=DEFAULT_SEED,
                    help=f"RNG seed for --order shuffle (default {DEFAULT_SEED})")
    ap.add_argument("--language", default=DEFAULT_LANGUAGE,
                    help="comma-separated project.yaml languages "
                         f"(default '{DEFAULT_LANGUAGE}')")
    ap.add_argument("--sanitizer", default=os.getenv("OSS_FUZZ_SANITIZER", "address"),
                    help="sanitizer the project must support (default address)")
    ap.add_argument("--engine", default="libfuzzer",
                    help="fuzzing engine the project must support")
    ap.add_argument("--oss-fuzz-dir", default=None,
                    help="OSS-Fuzz checkout (default $OSS_FUZZ_DIR)")
    ap.add_argument("-q", "--quiet", action="store_true",
                    help="suppress the provenance header on stderr")
    ap.add_argument("--no-probe", dest="probe", action="store_false",
                    help="skip the network probes (offline/air-gapped runs); "
                         "sampled projects are then not checked for a live "
                         "main_repo or any disclosed bug")
    args = ap.parse_args(argv)

    if args.count < 1:
        ap.error("--count must be >= 1")

    languages = tuple(s.strip().lower()
                      for s in args.language.split(",") if s.strip())
    if not languages:
        ap.error("--language must name at least one language")
    # The ranking *is* an OSV sweep, so there is no offline form of it. Saying
    # so beats silently returning a sample the flags did not ask for.
    if args.order == "recent" and not args.probe:
        ap.error("--order recent needs OSV; use --order shuffle with --no-probe")

    of = OssFuzz(oss_fuzz_dir=args.oss_fuzz_dir)
    if not os.path.isdir(os.path.join(of.oss_fuzz_dir, "projects")):
        sys.stderr.write(
            f"FATAL: no projects/ under '{of.oss_fuzz_dir}'; set $OSS_FUZZ_DIR "
            "to a google/oss-fuzz clone\n")
        return 2

    pool, rejected = eligible_projects(of, languages, args.sanitizer, args.engine)
    if not pool:
        sys.stderr.write(
            f"FATAL: no eligible projects in '{of.oss_fuzz_dir}' for "
            f"language={'/'.join(languages)} sanitizer={args.sanitizer} "
            f"engine={args.engine}\n")
        return 1

    dropped: List[Tuple[str, str]] = []
    dated: List[Tuple[str, str, str]] = []
    if args.order == "recent":
        from oss_fuzz.osv import OsvClient       # deferred: only probing needs it
        dated, dropped = select_recent(pool, args.count,
                                       OsvClient().query_project,
                                       make_repo_probe(of))
        chosen = [name for name, _, _ in dated]
    elif args.probe:
        from oss_fuzz.osv import OsvClient
        chosen, dropped = select_probed(pool, args.count, args.seed,
                                        make_probe(of, OsvClient().query_project))
    else:
        chosen = select(pool, args.count, args.seed)

    if not args.quiet:
        # stderr so stdout stays a clean project-per-line list to read from.
        considered = len(pool) + sum(rejected.values())
        order = ("newest usable OSV record first" if args.order == "recent"
                 else f"seeded shuffle (seed {args.seed})")
        sys.stderr.write(
            f"# oss-fuzz checkout : {of.oss_fuzz_dir}\n"
            f"# checkout commit   : {_checkout_commit(of.oss_fuzz_dir)}\n"
            f"# order / count     : {order} / {args.count}\n"
            f"# language          : {','.join(languages)}\n"
            f"# eligible pool     : {len(pool)} of {considered} C/C++ projects"
            f" (rejected: {rejected['language']} language,"
            f" {rejected['disabled']} disabled,"
            f" {rejected['unsupported']} unsupported,"
            f" {rejected['not-git']} non-git main_repo,"
            f" {rejected['fixture']} test fixture)\n")
        if args.order == "recent":
            # The ranking queried the whole pool, so "dropped" here is mostly
            # 'no disclosed bug' and is far too long to print in full; the repo
            # rejections are the ones a reader is surprised by.
            no_bug = sum(1 for _, why in dropped if why.startswith("no OSV"))
            sys.stderr.write(
                f"# ranking           : {len(pool)} projects queried on OSV, "
                f"{len(dropped) - no_bug} dropped on a dead main_repo, "
                f"{no_bug} with no usable record\n"
                f"# NOTE: 'recent' depends on OSV's contents today, so it is "
                f"reproducible only via this run's projects.list\n")
            for name, why in dropped:
                if not why.startswith("no OSV"):
                    sys.stderr.write(f"#   dropped {name}: {why}\n")
            for i, (name, published, osv_id) in enumerate(dated, 1):
                sys.stderr.write(
                    f"#   {i}. {name:<24} {(published or '?')[:10]}  "
                    f"{osv_id}\n")
        elif args.probe:
            # Without this line, "asked for 5, got 5" hides that it walked 8.
            sys.stderr.write(
                f"# probes            : live main_repo + a disclosed bug; "
                f"{len(chosen) + len(dropped)} probed, {len(dropped)} dropped\n")
            for name, why in dropped:
                sys.stderr.write(f"#   dropped {name}: {why}\n")
        else:
            sys.stderr.write("# probes            : off (--no-probe)\n")
        if len(chosen) < args.count:
            sys.stderr.write(
                f"# NOTE: asked for {args.count}, pool holds only {len(pool)}"
                f"{' after probing' if args.probe else ''}\n")

    for name in chosen:
        print(name)
    return 0


def _checkout_commit(oss_fuzz_dir: str) -> str:
    """The checkout's HEAD, or a placeholder -- provenance must never be fatal."""
    import subprocess
    try:
        p = subprocess.run(["git", "-C", oss_fuzz_dir, "rev-parse", "HEAD"],
                           capture_output=True, text=True, timeout=10)
        return p.stdout.strip() if p.returncode == 0 else "(not a git checkout)"
    except (OSError, subprocess.SubprocessError):
        return "(unknown)"


if __name__ == "__main__":
    sys.exit(main())
