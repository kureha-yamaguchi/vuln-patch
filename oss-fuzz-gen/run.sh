
#!/usr/bin/env bash
set -euo pipefail
# Create log file with timestamp
LOG_DIR="logs"
mkdir -p "$LOG_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${LOG_DIR}/run_${TIMESTAMP}.log"

# Log to both file and stdout
exec > >(tee -a "$LOG_FILE") 2>&1

echo "Started at: $(date)"
echo "Log file: $LOG_FILE"
echo ""


BASE_OFG=$PWD
# To start, you need to specify which model you will
# be using for the experiments. You need to set the MODEL
# environment variable now.
export MODEL='openai/gpt-oss-20b'

export HF_HOME=./.cache/huggingface

export STRONGREJECT_VLLM_URL=http://localhost:8000/v1
export OPENBLAS_NUM_THREADS=1
export MKL_NUM_THREADS=1
export OMP_NUM_THREADS=1
export HF_DATASETS_NUM_PROC=1
export TOKENIZERS_PARALLELISM=false

export PIP_CACHE_DIR=/datadrive/pip-cache
export PYTHONUSERBASE=/datadrive/python-user

# Create a working set up.
./scripts/run-new-oss-fuzz-project/setup.sh

# Create an OSS-Fuzz project that will be used for the experiment.
cd work/oss-fuzz/projects
git clone https://github.com/AdaLogics/oss-fuzz-auto

cd $BASE_OFG

# Now run our the generation on our newly created OSS-Fuzz project.
./scripts/run-new-oss-fuzz-project/run-project.sh oss-fuzz-auto

# Once finished, check results
python3 -m report.web -r results -s

# Navigate to localhost:8012