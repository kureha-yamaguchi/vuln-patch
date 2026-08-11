#!/usr/bin/env python3
"""Pick a reproducible random sample of OSS-Fuzz projects for a sweep.

Replaces the hand-maintained ``suites/ossfuzz_cpp20.projects`` list: the same
seed and the same OSS-Fuzz checkout always yield the same projects, so a sweep
is reproducible without a file anyone has to keep up to date.

Eligibility is not re-derived here. ``OssFuzz.check_support`` is the pipeline's
own answer to "can this front-end drive that project", covering language,
fuzzing engine, sanitizer, ``main_repo`` and the Dockerfile ``WORKDIR`` rule
that makes ``helper.py`` refuse a local checkout. Sampling over anything looser
would spend sweep slots on projects that provably cannot build, which reads as
a pipeline failure rather than what it is. The one filter added on top is
``disabled: true``, which OSS-Fuzz uses for projects it has stopped building.

Reproducibility has a boundary worth stating: the sample is a function of the
seed *and* of the projects/ tree, so pulling the OSS-Fuzz checkout can change
it. The provenance header written to stderr records the checkout commit for
exactly that reason -- quote it alongside the seed when a run is reported.

Selection shuffles the eligible list and takes a prefix rather than calling
random.sample, so raising -n extends the previous selection instead of
replacing it: the first 20 of a seed-42 run of 25 are the seed-42 run of 20.

Usage:
    uv run -m oss_fuzz.select_projects                  # 20 C++ projects, seed 42
    uv run -m oss_fuzz.select_projects -n 5 --seed 7
    uv run -m oss_fuzz.select_projects --language c,c++ --quiet
"""

import argparse
import os
import random
import sys
from typing import List, Tuple

from oss_fuzz.ossfuzz import OssFuzz

DEFAULT_COUNT = 20
DEFAULT_SEED = 42
DEFAULT_LANGUAGE = "c++"


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


def select(projects: List[str], count: int, seed: int) -> List[str]:
    """A deterministic sample of ``count`` projects.

    Sorted first so the result depends on the set of eligible projects and not
    on the order the filesystem happened to hand them over.
    """
    pool = sorted(projects)
    random.Random(seed).shuffle(pool)
    return pool[:count]


def main(argv: List[str] = None) -> int:
    ap = argparse.ArgumentParser(
        description="Reproducibly sample OSS-Fuzz projects for a sweep.")
    ap.add_argument("-n", "--count", type=int, default=DEFAULT_COUNT,
                    help=f"projects to select (default {DEFAULT_COUNT})")
    ap.add_argument("--seed", type=int, default=DEFAULT_SEED,
                    help=f"RNG seed (default {DEFAULT_SEED})")
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
    args = ap.parse_args(argv)

    if args.count < 1:
        ap.error("--count must be >= 1")

    languages = tuple(s.strip().lower()
                      for s in args.language.split(",") if s.strip())
    if not languages:
        ap.error("--language must name at least one language")

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

    chosen = select(pool, args.count, args.seed)

    if not args.quiet:
        # stderr so stdout stays a clean project-per-line list to read from.
        considered = len(pool) + sum(rejected.values())
        sys.stderr.write(
            f"# oss-fuzz checkout : {of.oss_fuzz_dir}\n"
            f"# checkout commit   : {_checkout_commit(of.oss_fuzz_dir)}\n"
            f"# seed / count      : {args.seed} / {args.count}\n"
            f"# language          : {','.join(languages)}\n"
            f"# eligible pool     : {len(pool)} of {considered} C/C++ projects"
            f" (rejected: {rejected['language']} language,"
            f" {rejected['disabled']} disabled,"
            f" {rejected['unsupported']} unsupported,"
            f" {rejected['not-git']} non-git main_repo,"
            f" {rejected['fixture']} test fixture)\n")
        if len(chosen) < args.count:
            sys.stderr.write(
                f"# NOTE: asked for {args.count}, pool holds only {len(pool)}\n")

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
