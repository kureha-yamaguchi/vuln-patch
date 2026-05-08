#!/usr/bin/env bash
set -euo pipefail

# Create log file with timestamp
LOG_DIR="logs"
mkdir -p "$LOG_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${LOG_DIR}/llm_${TIMESTAMP}.log"

# Log to both file and stdout
exec > >(tee -a "$LOG_FILE") 2>&1

echo "Started at: $(date)"
echo "Log file: $LOG_FILE"
echo ""

export VLLM_ATTENTION_BACKEND=TRITON_ATTN_VLLM_V1
export HF_HOME=./.cache/huggingface
mkdir -p $HF_HOME
echo "=== Caching in $HF_HOME ==="
CUDA_VISIBLE_DEVICES=0 vllm serve openai/gpt-oss-20b \
    --tensor-parallel-size 1 \
    --gpu-memory-utilization 0.9