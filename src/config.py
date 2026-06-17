"""Environment-driven configuration for the harness pipeline.

LLM backend (pick one):

  Option A — OpenAI API (same as the project_zero discover pipeline):
      export OPENAI_API_KEY=sk-...
      # optionally override the model:
      export LOCAL_LLM_MODEL=gpt-4o

  Option B — Local server (Ollama or LM Studio):
      export LOCAL_LLM_BASE_URL=http://localhost:11434/v1
      export LOCAL_LLM_MODEL=gpt-oss:20b
      # LOCAL_LLM_API_KEY is ignored for local servers
"""
import os


# --- LLM ------------------------------------------------------------------
# If OPENAI_API_KEY is set the pipeline targets api.openai.com directly
# (same pattern as project_zero/discover). Otherwise it falls back to a local
# Ollama/LM-Studio server. LOCAL_LLM_MODEL can override the model in both
# modes; --model on the CLI overrides it further at runtime.
_OPENAI_KEY = os.getenv('OPENAI_API_KEY')

if _OPENAI_KEY:
    LOCAL_LLM_BASE_URL = None                                    # SDK default → api.openai.com
    LOCAL_LLM_API_KEY  = _OPENAI_KEY
    # Default to a current OpenAI coding/reasoning model. Override with
    # LOCAL_LLM_MODEL or --model. Pin a dated snapshot in production for
    # reproducibility (model prompting behaviour can change between
    # snapshots).
    LOCAL_LLM_MODEL    = os.getenv('LOCAL_LLM_MODEL', 'gpt-5')
else:
    LOCAL_LLM_BASE_URL = os.getenv('LOCAL_LLM_BASE_URL', 'http://localhost:8000/v1')
    LOCAL_LLM_API_KEY  = os.getenv('LOCAL_LLM_API_KEY', 'not-needed')
    LOCAL_LLM_MODEL    = os.getenv('LOCAL_LLM_MODEL', 'gpt-oss-20b')

# Reasoning effort for OpenAI reasoning models (GPT-5.x, o-series):
# 'minimal' | 'low' | 'medium' | 'high'. Ignored for standard sampling
# models (gpt-4o etc.) and for local servers. Higher effort tends to
# help on the harness-reachability inference at the cost of latency and
# tokens — 'high' is a reasonable default for this pipeline.
OPENAI_REASONING_EFFORT = os.getenv('OPENAI_REASONING_EFFORT', 'high')

# --- Jazzer (JVM libFuzzer port) ------------------------------------------
# We compile the generated harness against jazzer-api.jar so symbols
# like FuzzedDataProvider resolve. The jar is fetched once from Maven
# Central and cached. Override JAZZER_VERSION / JAZZER_API_JAR if you
# want a different release or a pre-existing copy on disk.
JAZZER_VERSION = os.getenv('JAZZER_VERSION', '0.22.1')
JAZZER_API_JAR = os.getenv(
    'JAZZER_API_JAR',
    os.path.expanduser(f'~/.cache/jazzer/jazzer-api-{JAZZER_VERSION}.jar'),
)
JAZZER_API_URL = (
    f'https://repo.maven.apache.org/maven2/com/code-intelligence/'
    f'jazzer-api/{JAZZER_VERSION}/jazzer-api-{JAZZER_VERSION}.jar'
)

# Jazzer standalone jar — used to *run* harnesses (not just compile them).
JAZZER_STANDALONE_JAR = os.getenv(
    'JAZZER_STANDALONE_JAR',
    os.path.expanduser(f'~/.cache/jazzer/jazzer-{JAZZER_VERSION}.jar'),
)
JAZZER_STANDALONE_URL = (
    f'https://repo.maven.apache.org/maven2/com/code-intelligence/'
    f'jazzer/{JAZZER_VERSION}/jazzer-{JAZZER_VERSION}.jar'
)

# Jazzer exits with this code when it finds a finding (crash/exception).
JAZZER_CRASH_EXIT_CODE = 77

# Per-harness fuzzing time limit in seconds (used for the final
# run against the *patched* code).
FUZZ_TIMEOUT_SECONDS = int(os.getenv('FUZZ_TIMEOUT_SECONDS', '60'))

# Short per-harness time limit (seconds) for the in-campaign verification
# run against the *buggy* checkout. This gates acceptance: a freshly
# compiled harness is only counted as a success if it crashes the known-
# buggy code within this budget. Kept small so the campaign stays cheap
# in wall-clock — a harness that genuinely reaches the root cause almost
# always crashes the buggy version near-immediately.
VERIFY_TIMEOUT_SECONDS = int(os.getenv('VERIFY_TIMEOUT_SECONDS', '60'))

# Upper bound on how many root-cause-reachable function names we splice
# into the prompt as the "coverage map". Prevents a pathologically large
# reachable set (e.g. a touched function that transitively reaches half
# the project) from dominating the context window.
MAX_REACHABLE_IN_PROMPT = int(os.getenv('MAX_REACHABLE_IN_PROMPT', '60'))

# --- drr patch dataset -----------------------------------------------------
DRR_CORRECT_DIR     = '../drr/Patches/Dcorrect'
DRR_OVERFITTING_DIR = '../drr/Patches/Doverfitting'

# All APR tools shipped in the drr dataset. Not every tool has patches
# for every project, so PatchSelector samples until it finds one that
# does.
APR_TOOLS = [
    'ACS', 'Arja', 'CapGen', 'DeepRepair', 'Elixir', 'HDRepair',
    'JGenProg2015', 'Jaid', 'Nopol2015', 'SOFix', 'SequenceR',
    'SimFix', 'SketchFix', 'ssFix',
]

# Where buggy checkouts get materialised.
D4J_CHECKOUT_ROOT = '/tmp/d4j'

# --- Linux kernel CVE sibling database ------------------------------------
# Root directory for kernel worktrees created by checkout_pair.py.
LINUX_CHECKOUT_ROOT = os.getenv('LINUX_CHECKOUT_ROOT', '/tmp/cve_sibling_checkouts')

# Shared bare kernel repo used by checkout_pair.py (blobless clone, ~400 MB).
LINUX_KERNEL_REPO = os.getenv('LINUX_KERNEL_REPO', '/tmp/linux-kernel-shared.git')

# Path to the cve_sibling_db_linux directory.
LINUX_DB_DIR = os.getenv(
    'LINUX_DB_DIR',
    os.path.join(os.path.dirname(__file__), '..', 'src', 'cve_sibling_db_linux'),
)

# Default harness style: 'syscall' (main()-based) or 'libfuzzer' (LLVMFuzzerTestOneInput).
# 'syscall' is correct for most Linux kernel subsystems (SCTP, block, inotify, etc.).
LINUX_HARNESS_STYLE = os.getenv('LINUX_HARNESS_STYLE', 'syscall')