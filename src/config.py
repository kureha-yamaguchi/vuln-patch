"""Environment-driven configuration for the harness pipeline.

Override any of these with env vars before launching the script — for
example to point at LM Studio instead of Ollama:

    export LOCAL_LLM_BASE_URL=http://localhost:1234/v1
    export LOCAL_LLM_MODEL=openai/gpt-oss-20b
"""
import os


# --- LLM (local OpenAI-compatible server) ---------------------------------
# By default we target a local Ollama server on port 11434. Both Ollama
# and LM Studio speak the /v1/chat/completions protocol, so only the URL
# and model name need to change between them.
LOCAL_LLM_BASE_URL = os.getenv('LOCAL_LLM_BASE_URL', 'http://localhost:11434/v1')
LOCAL_LLM_API_KEY  = os.getenv('LOCAL_LLM_API_KEY', 'not-needed')  # dummy
LOCAL_LLM_MODEL    = os.getenv('LOCAL_LLM_MODEL', 'gpt-oss:20b')

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