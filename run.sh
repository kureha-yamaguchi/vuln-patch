#!/usr/bin/env bash
set -euo pipefail

PATCH_FLAG="--correct"
PROJECT_NAME="Closure"

# Create log file with timestamp
LOG_DIR="logs"
mkdir -p "$LOG_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${LOG_DIR}/experiment_${TIMESTAMP}.log"

# Log to both file and stdout
exec > >(tee -a "$LOG_FILE") 2>&1

echo "Started at: $(date)"
echo "Log file: $LOG_FILE"
echo ""

cd src && uv run -m run $PATCH_FLAG --project_name $PROJECT_NAME