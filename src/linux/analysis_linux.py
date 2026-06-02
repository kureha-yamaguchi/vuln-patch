"""Build C patch context from the cve_sibling_db_linux database.

Reads metadata.json, fix0.patch, and fix0_context/ source files for a given
CVE pair and assembles a LinuxPatchContext ready for prompt construction.
No kernel build or checkout required.

Analogous to src/java/analysis.py but for C / Linux kernel patches.
"""
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

sys.path.insert(0, str(Path(__file__).parent.parent))
import config

_DB_DIR = Path(config.LINUX_DB_DIR).resolve()


@dataclass
class LinuxTouchedFunction:
    name: str
    source: str           # full file content from fix0_context/
    source_file: str      # relative path, e.g. 'net/sctp/socket.c'
    hunk_start: int       # first changed line in the patch (1-indexed)
    hunk_end: int         # last changed line in the patch


@dataclass
class LinuxPatchContext:
    prior_cve: str
    later_cve: str
    patch_text: str                                    # raw fix0.patch
    touched_functions: list[LinuxTouchedFunction]
    subsystem: str                                     # e.g. 'net/sctp'
    harness_style: str                                 # 'syscall' or 'libfuzzer'
    metadata: dict
    poc_available: bool = False                        # True for the 2 known-PoC pairs
    db_pair_dir: Path = field(default_factory=Path)


_POC_PAIRS = {
    ("CVE-2010-4347", "CVE-2011-1021"),   # SecWiki full exploit
    ("CVE-2016-9576", "CVE-2016-10088"),  # Syzkaller trigger
}

# Subsystems best served by libFuzzer (byte-stream interfaces).
_LIBFUZZER_SUBSYSTEMS = {"fs/ext4", "fs/btrfs", "fs/nfs", "fs/jfs"}


def _parse_hunk_ranges(patch_text: str) -> list[tuple[str, int, int]]:
    """Return list of (filepath, hunk_start, hunk_end) from a unified diff."""
    result = []
    current_file = None
    hunk_start = hunk_end = 0
    for line in patch_text.splitlines():
        m = re.match(r"^\+\+\+ b/(.*)", line)
        if m:
            current_file = m.group(1)
            continue
        m = re.match(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", line)
        if m and current_file:
            start = int(m.group(1))
            count = int(m.group(2)) if m.group(2) else 1
            hunk_start = start
            hunk_end = start + count - 1
            result.append((current_file, hunk_start, hunk_end))
    return result


def _load_context_source(pair_dir: Path, filepath: str) -> Optional[str]:
    """Load a source file from fix0_context/ if available."""
    p = pair_dir / "fix0_context" / filepath
    return p.read_text(errors="replace") if p.exists() else None


def build_context(prior_cve: str, later_cve: str) -> LinuxPatchContext:
    """Build a LinuxPatchContext for the given CVE pair."""
    pair_name = f"{prior_cve}__{later_cve}"
    pair_dir = _DB_DIR / pair_name

    if not pair_dir.exists():
        raise FileNotFoundError(
            f"Pair directory not found: {pair_dir}\n"
            f"Run fetch_patches.py first."
        )

    import json
    with (pair_dir / "metadata.json").open() as f:
        meta = json.load(f)

    patch_path = pair_dir / "fix0.patch"
    patch_text = patch_path.read_text(errors="replace") if patch_path.exists() else ""

    subsystem = meta.get("kernel_subsystem", "unknown")
    style = (
        "libfuzzer" if subsystem in _LIBFUZZER_SUBSYSTEMS
        else config.LINUX_HARNESS_STYLE
    )

    hunk_ranges = _parse_hunk_ranges(patch_text)
    # Build one LinuxTouchedFunction per changed file (deduplicated).
    seen_files: set[str] = set()
    touched: list[LinuxTouchedFunction] = []
    func_names: list[str] = meta.get("affected_functions_fix0") or []
    func_iter = iter(func_names)

    for filepath, hunk_start, hunk_end in hunk_ranges:
        if filepath in seen_files:
            continue
        seen_files.add(filepath)
        source = _load_context_source(pair_dir, filepath) or ""
        name = next(func_iter, filepath.split("/")[-1].replace(".c", ""))
        touched.append(LinuxTouchedFunction(
            name=name,
            source=source,
            source_file=filepath,
            hunk_start=hunk_start,
            hunk_end=hunk_end,
        ))

    return LinuxPatchContext(
        prior_cve=prior_cve,
        later_cve=later_cve,
        patch_text=patch_text,
        touched_functions=touched,
        subsystem=subsystem,
        harness_style=style,
        metadata=meta,
        poc_available=(prior_cve, later_cve) in _POC_PAIRS,
        db_pair_dir=pair_dir,
    )
