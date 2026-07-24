#!/usr/bin/env bash
# Archive a VM run to the Mac and prune it from the VM — the run-log-isolation
# protocol as ONE command, so "archive then delete" is mechanical, not a habit:
#   scripts/archive-run.sh c2d_20260724_152639
# Steps: rsync run dir -> runs-archive/runs/, VERIFY file counts match, then
# (and only then) delete the VM run dir, its co/ checkouts, and the launch log.
# Refuses to prune on any mismatch.
set -euo pipefail

RUN="${1:?usage: archive-run.sh <run_dir_name e.g. c2d_20260724_152639>}"
VM_HOST="${VM_HOST:-hetzner}"
VM_RUNS="/home/code/scratch/runs"
VM_CO="/home/code/scratch/co"
DEST="$(cd "$(dirname "$0")/.." && pwd)/runs-archive/runs"

rsync -az "${VM_HOST}:${VM_RUNS}/${RUN}" "${DEST}/"

LOCAL_N=$(find "${DEST}/${RUN}" -type f | wc -l | tr -d ' ')
REMOTE_N=$(ssh "${VM_HOST}" "find ${VM_RUNS}/${RUN} -type f | wc -l" | tr -d ' ')
if [ -z "$LOCAL_N" ] || [ "$LOCAL_N" != "$REMOTE_N" ] || [ "$LOCAL_N" = "0" ]; then
  echo "MISMATCH: local=$LOCAL_N remote=$REMOTE_N — NOT pruning ${RUN}" >&2
  exit 1
fi

SUITE="${RUN%%_2*}"   # c2d_20260724_152639 -> c2d
ssh "${VM_HOST}" "rm -rf ${VM_RUNS}/${RUN} ${VM_CO}/${RUN} /home/code/scratch/${SUITE}.launch.log"
echo "archived ${RUN} (${LOCAL_N} files, verified) -> ${DEST}/${RUN}; VM pruned"
