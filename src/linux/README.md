# Linux Kernel Harness Pipeline

Pipeline for generating and verifying C trigger programs for Linux kernel CVE
sibling pairs. Works alongside `src/cve_sibling_db_linux/` which provides the
ground-truth dataset of 26 incomplete-fix pairs.

## Modules

| File | Role |
|---|---|
| `analysis_linux.py` | Load patch context from DB — reads `metadata.json`, `fix0.patch`, `fix0_context/` source |
| `prompts_linux.py` | Build chat-completion prompts (syscall-sequence or libFuzzer style) |
| `build_linux.py` | Extract `c ...` block from LLM response, compile with gcc or clang |
| `verify_linux.py` | 3-state verifier: run harness against pre_fix / fix0 / fix1 kernel states |
| `run_linux.py` | CLI entry point — orchestrates the full generate → compile → verify loop |

## Quick start

```bash
# Set your OpenAI key (or set LOCAL_LLM_BASE_URL for Ollama)
export OPENAI_API_KEY=sk-...

# Generate harnesses, skip verification (no kernel checkouts needed)
cd src && uv run linux/run_linux.py \
  --pair CVE-2011-2482__CVE-2011-4348 \
  --target_successes 3 \
  --max_attempts 20 \
  --no-verify

# With full 3-state verification (requires Option B below)
cd src && uv run linux/run_linux.py \
  --pair CVE-2011-2482__CVE-2011-4348 \
  --target_successes 3
```

## Verification options

The 3-state check requires running the compiled harness against three
independent kernel states:

| State | Commit | Expected | Meaning |
|---|---|---|---|
| `pre_fix` | `fix0_parent_commit` | CRASH | Harness targets the real bug |
| `fix0` | `fix0_commit` | CRASH | Incomplete patch confirmed |
| `fix1` | `fix1_commit` | NO CRASH | Corrective fix verified |

All three states run the same binary but against different kernel versions,
which means they must run inside VMs — Docker containers share the host kernel
and cannot provide different kernel versions per state.

---

### Option A — Docker for compilation only

**Useful for:** getting Linux kernel headers (`<netinet/sctp.h>`, `<linux/sctp.h>`
etc.) so harnesses compile at near-100% rate on macOS. Compile rate on macOS
without Docker is ~60% (headers missing); with Linux headers it would be ~95%+.

**Not useful for:** actual 3-state verification. All containers share the host
kernel.

Since Option B's build container already provides Linux headers, Option A has
no standalone value once Option B is in place. Skip it.

---

### Option B — Docker + virtme-ng (recommended)

**What it gives you:** full 3-state verification. Build a minimal kernel image
per commit in a Docker container, then launch micro-VMs with `virtme-ng`
(a lightweight QEMU wrapper), run the harness inside the VM, and capture the
result.

**Build time:** 3-8 minutes per kernel commit on modern hardware (minimal
config, not a full defconfig). With ccache and a cache keyed on commit hash,
each commit is built only once. For 26 pairs × 3 states = 78 unique commits,
the one-time cost is ~5-10 hours of builds (parallelisable). After that,
verification of a single harness is ~30 seconds per state (VM boot + run).

---

#### B.1 Prerequisites

```bash
# On the host (Mac)
brew install qemu
pip install virtme-ng        # or: pip install git+https://github.com/arighi/virtme-ng

# Docker Desktop must be running
```

---

#### B.2 Dockerfile.kernel-build

Create `src/linux/docker/Dockerfile.kernel-build`:

```dockerfile
FROM ubuntu:22.04

# Kernel build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    git ca-certificates \
    build-essential gcc gcc-12 gcc-9 gcc-7 gcc-5 \
    bc flex bison \
    libssl-dev libelf-dev libncurses-dev \
    ccache cpio xz-utils \
    python3 \
 && rm -rf /var/lib/apt/lists/*

# Shared kernel source (bind-mount or clone once)
WORKDIR /build

COPY build_kernel.sh /build/build_kernel.sh
RUN chmod +x /build/build_kernel.sh

ENTRYPOINT ["/build/build_kernel.sh"]
```

Create `src/linux/docker/build_kernel.sh`:

```bash
#!/usr/bin/env bash
# Usage: build_kernel.sh <commit> <subsystem> <output_dir>
# Example: build_kernel.sh abc1234 net/sctp /out
set -euo pipefail

COMMIT="$1"
SUBSYSTEM="$2"
OUTPUT_DIR="$3"

KERNEL_REPO="${LINUX_KERNEL_REPO:-/kernel/linux.git}"
CCACHE_DIR="${CCACHE_DIR:-/ccache}"
BUILD_DIR="/tmp/kernel-build-${COMMIT:0:12}"

mkdir -p "$OUTPUT_DIR" "$CCACHE_DIR"
export CCACHE_DIR PATH="/usr/lib/ccache:$PATH"

# --- Clone/fetch at the right commit ---
if [ ! -d "$BUILD_DIR" ]; then
    git clone --filter=blob:none "$KERNEL_REPO" "$BUILD_DIR"
fi
cd "$BUILD_DIR"
git fetch --filter=blob:none origin "$COMMIT"
git checkout "$COMMIT"

# --- Select gcc version by kernel era ---
YEAR=$(git log -1 --format='%ai' | cut -c1-4)
if   [ "$YEAR" -le 2009 ]; then CC="gcc-5";   # gcc 4.x preferable; 5 is closest available
elif [ "$YEAR" -le 2013 ]; then CC="gcc-7";
elif [ "$YEAR" -le 2017 ]; then CC="gcc-9";
else                             CC="gcc-12";
fi
export CC

# --- Minimal kernel config ---
make CC="$CC" allnoconfig

# Enable common base options
./scripts/config \
    --enable CONFIG_64BIT \
    --enable CONFIG_SMP \
    --enable CONFIG_NET \
    --enable CONFIG_INET \
    --enable CONFIG_UNIX \
    --enable CONFIG_PROC_FS \
    --enable CONFIG_PRINTK \
    --enable CONFIG_BUG \
    --enable CONFIG_EARLY_PRINTK \
    --enable CONFIG_SERIAL_8250 \
    --enable CONFIG_SERIAL_8250_CONSOLE \
    --enable CONFIG_VIRTIO \
    --enable CONFIG_VIRTIO_PCI \
    --enable CONFIG_VIRTIO_NET

# Enable subsystem-specific options (see B.3 below)
/build/enable_subsystem_config.sh "$SUBSYSTEM"

make CC="$CC" olddefconfig

# --- Build bzImage ---
make CC="$CC" -j"$(nproc)" bzImage 2>&1 | tail -20

# --- Build minimal initramfs with busybox ---
/build/build_initramfs.sh "$OUTPUT_DIR"

cp arch/x86/boot/bzImage "$OUTPUT_DIR/bzImage"
echo "Built kernel at $COMMIT → $OUTPUT_DIR"
```

---

#### B.3 Subsystem config mapping

Create `src/linux/docker/enable_subsystem_config.sh`:

```bash
#!/usr/bin/env bash
# Enable CONFIG_* options needed per kernel subsystem.
# Called with the subsystem path (e.g. 'net/sctp', 'block', 'fs/ext4').
SUBSYSTEM="$1"

enable() { ./scripts/config --enable "$@"; }

case "$SUBSYSTEM" in
  net/sctp)
    enable CONFIG_IP_SCTP ;;
  net/rds)
    enable CONFIG_RDS CONFIG_RDS_TCP CONFIG_RDS_RDMA ;;
  net/sched)
    enable CONFIG_NET_SCHED CONFIG_NET_CLS_ACT ;;
  block|block/*)
    enable CONFIG_BLOCK CONFIG_BLK_DEV CONFIG_SCSI CONFIG_BLK_DEV_SD ;;
  fs/ext4)
    enable CONFIG_EXT4_FS ;;
  fs/nfs|fs/nfs/*)
    enable CONFIG_NFS_FS CONFIG_NFS_V4 ;;
  fs/notify|fs/notify/inotify)
    enable CONFIG_INOTIFY_USER CONFIG_FSNOTIFY ;;
  fs/partitions)
    enable CONFIG_PARTITION_ADVANCED CONFIG_LDM_PARTITION ;;
  virt/kvm|arch/x86/kvm)
    enable CONFIG_KVM CONFIG_KVM_INTEL CONFIG_KVM_IOMMU ;;
  kernel/events)
    enable CONFIG_PERF_EVENTS CONFIG_HW_PERF_EVENTS ;;
  drivers/net)
    enable CONFIG_NET_VENDOR_INTEL CONFIG_E1000 ;;
  drivers/gpu)
    enable CONFIG_DRM CONFIG_DRM_I915 ;;
  drivers/acpi)
    enable CONFIG_ACPI CONFIG_ACPI_DEBUGFS ;;
  drivers/block)
    enable CONFIG_XEN_BLKDEV_BACKEND CONFIG_XEN ;;
  mm)
    enable CONFIG_SHMEM CONFIG_SECURITY ;;
  include/net|include/linux)
    # Header-only change; enable general networking
    enable CONFIG_NET CONFIG_INET ;;
  *)
    echo "Warning: no specific config for subsystem '$SUBSYSTEM', using base config" ;;
esac
```

---

#### B.4 Minimal initramfs

Create `src/linux/docker/build_initramfs.sh`:

```bash
#!/usr/bin/env bash
# Build a busybox-based initramfs. The harness binary is injected at runtime
# via a 9p/virtio mount, not baked into the image.
set -euo pipefail
OUTPUT_DIR="$1"
WORK="/tmp/initramfs"

apt-get install -y --no-install-recommends busybox-static 2>/dev/null || true

rm -rf "$WORK" && mkdir -p "$WORK"/{bin,sbin,proc,sys,dev,tmp,mnt}

cp /bin/busybox "$WORK/bin/"
ln -s busybox "$WORK/bin/sh"
ln -s busybox "$WORK/bin/ls"
ln -s busybox "$WORK/bin/cat"
ln -s busybox "$WORK/bin/dmesg"

cat > "$WORK/init" << 'INIT'
#!/bin/sh
mount -t proc none /proc
mount -t sysfs none /sys
mount -t devtmpfs none /dev 2>/dev/null || mdev -s

# Mount shared directory containing the harness binary (passed via virtio-9p)
mkdir -p /harness
mount -t 9p -o trans=virtio harness /harness 2>/dev/null

echo "=== Running harness ===" > /dev/ttyS0
/harness/trigger
RESULT=$?
echo "=== Harness exit: $RESULT ===" > /dev/ttyS0
dmesg | grep -E "BUG|WARN|OOPS|kernel BUG|general protection" > /dev/ttyS0 || true

# Signal result to host and power off
echo "$RESULT" > /dev/ttyS0
poweroff -f
INIT

chmod +x "$WORK/init"

cd "$WORK"
find . | cpio -H newc -o | gzip > "$OUTPUT_DIR/initramfs.cpio.gz"
echo "Initramfs built → $OUTPUT_DIR/initramfs.cpio.gz"
```

---

#### B.5 Python runner — `docker_kernel_builder.py`

Create `src/linux/docker_kernel_builder.py`:

```python
"""Build a Linux kernel at a specific commit inside Docker and cache the result.

Cache layout:
    ~/.cache/vuln_patch_kernels/<commit12>/
        bzImage
        initramfs.cpio.gz
        build.log
"""
import hashlib
import subprocess
import sys
from pathlib import Path

CACHE_ROOT = Path.home() / ".cache" / "vuln_patch_kernels"
DOCKER_IMAGE = "vuln-patch-kernel-builder"
SCRIPT_DIR = Path(__file__).parent / "docker"


def build_kernel(
    commit: str,
    subsystem: str,
    kernel_repo: str,
    force: bool = False,
) -> Path:
    """Build kernel at commit and return the cache directory containing bzImage."""
    key = commit[:12]
    cache_dir = CACHE_ROOT / key

    if cache_dir.exists() and (cache_dir / "bzImage").exists() and not force:
        print(f"  Kernel {key} already cached at {cache_dir}")
        return cache_dir

    cache_dir.mkdir(parents=True, exist_ok=True)
    print(f"  Building kernel {key} for subsystem {subsystem} ...")

    # Ensure Docker image is built
    subprocess.run(
        ["docker", "build", "-t", DOCKER_IMAGE,
         "-f", str(SCRIPT_DIR / "Dockerfile.kernel-build"),
         str(SCRIPT_DIR)],
        check=True,
    )

    # Run the build container
    subprocess.run(
        [
            "docker", "run", "--rm",
            "-v", f"{kernel_repo}:/kernel/linux.git:ro",
            "-v", f"{cache_dir}:/out",
            "-v", f"{CACHE_ROOT}/ccache:/ccache",
            "-e", f"LINUX_KERNEL_REPO=/kernel/linux.git",
            "-e", f"CCACHE_DIR=/ccache",
            DOCKER_IMAGE,
            commit, subsystem, "/out",
        ],
        check=True,
    )

    print(f"  Kernel {key} built → {cache_dir}")
    return cache_dir
```

---

#### B.6 VM runner — `vm_runner.py`

Create `src/linux/vm_runner.py`:

```python
"""Run a harness binary inside a micro-VM using virtme-ng and capture results."""
import re
import subprocess
import tempfile
from pathlib import Path


def run_harness_in_vm(
    harness_path: str,
    kernel_cache_dir: Path,
    timeout: int = 60,
) -> tuple[bool, str]:
    """Run harness inside a QEMU VM with the given kernel.

    Returns:
        (triggered, log) where triggered=True means crash detected.
    """
    bzimage   = kernel_cache_dir / "bzImage"
    initramfs = kernel_cache_dir / "initramfs.cpio.gz"
    harness_dir = Path(harness_path).parent

    cmd = [
        "qemu-system-x86_64",
        "-kernel", str(bzimage),
        "-initrd", str(initramfs),
        "-nographic",
        "-serial", "mon:stdio",
        "-no-reboot",
        "-m", "512M",
        "-smp", "2",
        # Share harness directory into VM via virtio-9p
        "-fsdev", f"local,id=harness,path={harness_dir},security_model=none",
        "-device", "virtio-9p-pci,fsdev=harness,mount_tag=harness",
        "-append",
            "console=ttyS0 panic=1 oops=panic "
            "root=/dev/ram0 rdinit=/init loglevel=7",
    ]

    # Rename harness binary to 'trigger' so /init can find it
    trigger = Path(harness_dir) / "trigger"
    trigger.unlink(missing_ok=True)
    trigger.symlink_to(Path(harness_path).name)

    try:
        result = subprocess.run(
            cmd, capture_output=True, text=True, timeout=timeout,
        )
        log = result.stdout + result.stderr
    except subprocess.TimeoutExpired as e:
        log = (e.stdout or "") + (e.stderr or "")
        # Timeout = hang = crash for DoS-class bugs
        return True, log + "\n[TIMEOUT — treated as crash]"
    finally:
        trigger.unlink(missing_ok=True)

    triggered = _detect_crash(log, result.returncode)
    return triggered, log


def _detect_crash(log: str, exit_code: int) -> bool:
    """Return True if the log contains kernel crash indicators."""
    crash_patterns = [
        r"BUG:",
        r"kernel BUG at",
        r"OOPS:",
        r"general protection fault",
        r"WARN_ON",
        r"=== Harness exit: [^0]",    # non-zero exit from harness
        r"Kernel panic",
        r"Oops:",
    ]
    for pat in crash_patterns:
        if re.search(pat, log):
            return True
    # Check harness exit code embedded in serial output
    m = re.search(r"=== Harness exit: (\d+) ===", log)
    if m and int(m.group(1)) != 0:
        return True
    return False
```

---

#### B.7 Wire into `verify_linux.py`

In `verify_linux.py`, replace the `_run_state` method's direct `subprocess.run`
with a VM-based run when kernel cache directories are available:

```python
# In HarnessVerifier.__init__, accept optional kernel_cache_dirs:
def __init__(
    self,
    pre_fix_checkout=None,   # existing: git worktrees (used when no VM)
    fix0_checkout=None,
    fix1_checkout=None,
    pre_fix_kernel_cache=None,   # new: Path to cached kernel for VM run
    fix0_kernel_cache=None,
    fix1_kernel_cache=None,
    timeout=config.VERIFY_TIMEOUT_SECONDS,
):
    ...

def _run_state(self, state, harness_path, checkout_path, kernel_cache=None):
    if kernel_cache and Path(kernel_cache, "bzImage").exists():
        from linux.vm_runner import run_harness_in_vm
        triggered, log = run_harness_in_vm(harness_path, Path(kernel_cache), self.timeout)
        return StateResult(state, str(kernel_cache), triggered, -1 if triggered else 0,
                           log[:2000], "", False)
    # Fallback: direct run (works for non-kernel-crash bugs on host Linux)
    ...existing subprocess.run logic...
```

---

#### B.8 Full workflow

```bash
# 1. One-time: ensure shared bare kernel repo exists
python3 cve_sibling_db_linux/checkout_pair.py \
    --pair CVE-2011-2482__CVE-2011-4348   # downloads shared kernel repo

# 2. Build kernel images for the 3 states (one-time per pair, ~5-20 min each)
python3 -c "
from linux.docker_kernel_builder import build_kernel
import json, pathlib

meta = json.load(open('cve_sibling_db_linux/CVE-2011-2482__CVE-2011-4348/metadata.json'))
repo  = '/tmp/linux-kernel-shared.git'
sub   = meta['kernel_subsystem']

build_kernel(meta['fix0_parent_commit'], sub, repo)
build_kernel(meta['fix0_commit'],        sub, repo)
build_kernel(meta['fix1_commit'],        sub, repo)
"

# 3. Run the full pipeline with VM verification
cd src && uv run linux/run_linux.py \
    --pair CVE-2011-2482__CVE-2011-4348 \
    --target_successes 3
```

---

### Option C — GitHub Actions on Linux

**Useful for:** quick sanity checks that harnesses do something real on a Linux
kernel. The runner's kernel is not at the exact CVE commit, but it is a real
Linux kernel, so SCTP socket creation, block device ioctls, etc., will work.

**Not useful for:** differential verification (you can't test fix0 vs fix1).

```yaml
# .github/workflows/harness-check.yml
name: Harness Linux check
on: [push]
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: sudo modprobe sctp  # enable kernel modules as needed
      - run: |
          cd src
          uv run linux/run_linux.py \
            --pair CVE-2011-2482__CVE-2011-4348 \
            --no-verify \
            --target_successes 3
      - run: |
          # Attempt to run compiled harnesses directly on the Linux runner
          for f in /tmp/cve_sibling_checkouts/*/harnesses/attempt_*; do
            [ -x "$f" ] && echo "=== $f ===" && timeout 10 "$f" && echo "exit 0" || echo "exit $?"
          done
```

---

## Toolchain compatibility

Old kernel commits need old gcc versions. Modern gcc rejects code patterns
common in 2005-era kernel C:

| Kernel year | Recommended gcc | Notes |
|---|---|---|
| 2005–2009 | gcc 4.4 / gcc 4.8 | Available via `ubuntu:14.04` base image or `crosstool-ng` |
| 2010–2013 | gcc 4.8 / gcc 5 | `ubuntu:16.04` |
| 2014–2017 | gcc 6 / gcc 7 | `ubuntu:18.04` |
| 2018–2021 | gcc 9 / gcc 12 | `ubuntu:22.04` |

For the oldest pairs (CVE-2005-4881, CVE-2009-1385/1389), use a multi-stage
Docker build with an `ubuntu:14.04` gcc stage, or build via
[crosstool-ng](https://github.com/crosstool-ng/crosstool-ng).

## Caching strategy

Kernel images are cached by commit hash under `~/.cache/vuln_patch_kernels/`.
The `docker_kernel_builder.py` skips the build if `bzImage` already exists.
With 26 pairs × 3 states = 78 unique commits, the one-time build cost is
~5-10 hours parallelised across cores. After that, each verification run is
~30 seconds (VM boot + harness execution).

To pre-build all kernels for all pairs:

```bash
python3 - << 'EOF'
import json, subprocess
from pathlib import Path
from linux.docker_kernel_builder import build_kernel

DB = Path("cve_sibling_db_linux")
REPO = "/tmp/linux-kernel-shared.git"

for meta_path in sorted(DB.glob("CVE-*__CVE-*/metadata.json")):
    m = json.loads(meta_path.read_text())
    if m.get("fuzzing_excluded"):
        continue
    sub = m["kernel_subsystem"]
    for key in ["fix0_parent_commit", "fix0_commit", "fix1_commit"]:
        commit = m.get(key)
        if commit:
            build_kernel(commit, sub, REPO)
EOF
```
