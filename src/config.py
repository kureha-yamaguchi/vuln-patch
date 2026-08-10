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

  Option C — Azure OpenAI (takes precedence over A and B):
      export AZURE_OPENAI_API_KEY=...
      export AZURE_OPENAI_ENDPOINT=https://vuln-patch-resource.cognitiveservices.azure.com/
      # the *deployment* name (often equals the model name):
      export AZURE_OPENAI_DEPLOYMENT=gpt-5.4
      # optionally override the api version:
      export AZURE_OPENAI_API_VERSION=2024-12-01-preview
"""
import os


def _load_dotenv() -> None:
    """Load KEY=VALUE pairs from a repo-root .env into os.environ.

    Zero-dependency (no python-dotenv): so every entry point that imports
    config — run.sh, evaluate.sh, or a manual `uv run` — picks up the same
    settings without a --env-file flag. Real exported environment variables
    always win (we use setdefault), matching python-dotenv's default and
    keeping CI/shell overrides authoritative. The .env file itself is
    gitignored; commit .env.example as the template instead."""
    env_path = os.path.join(os.path.dirname(__file__), '..', '.env')
    try:
        with open(env_path, 'r') as fh:
            lines = fh.readlines()
    except FileNotFoundError:
        return
    for raw in lines:
        line = raw.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        if line.startswith('export '):
            line = line[len('export '):]
        key, _, value = line.partition('=')
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ('"', "'"):
            value = value[1:-1]
        if key:
            os.environ.setdefault(key, value)


_load_dotenv()


# --- LLM ------------------------------------------------------------------
# Backend precedence: Azure OpenAI (if AZURE_OPENAI_API_KEY is set) >
# OpenAI direct (if OPENAI_API_KEY is set) > local Ollama/LM-Studio server.
# LOCAL_LLM_MODEL can override the model in all modes; --model on the CLI
# overrides it further at runtime.
_AZURE_KEY  = os.getenv('AZURE_OPENAI_API_KEY')
_OPENAI_KEY = os.getenv('OPENAI_API_KEY')

# Azure-specific settings. Only meaningful when AZURE_OPENAI_API_KEY is set.
# On Azure the request's `model` field carries the *deployment* name, which
# frequently (but not always) matches the underlying model name.
USE_AZURE              = bool(_AZURE_KEY)
AZURE_OPENAI_API_KEY     = _AZURE_KEY
AZURE_OPENAI_ENDPOINT    = os.getenv('AZURE_OPENAI_ENDPOINT', 'https://vuln-patch-resource.cognitiveservices.azure.com/')
AZURE_OPENAI_API_VERSION = os.getenv('AZURE_OPENAI_API_VERSION', '2024-12-01-preview')
AZURE_OPENAI_DEPLOYMENT  = os.getenv('AZURE_OPENAI_DEPLOYMENT', 'gpt-5.4')

if USE_AZURE:
    # base_url/api_key are unused for Azure (the AzureOpenAI client takes
    # azure_endpoint + api_version instead) but we populate the shared names
    # so the rest of the pipeline keeps working unchanged.
    LOCAL_LLM_BASE_URL = None
    LOCAL_LLM_API_KEY  = _AZURE_KEY
    # The "model" here is the Azure deployment name. LOCAL_LLM_MODEL still
    # wins if explicitly set, so --model continues to work.
    LOCAL_LLM_MODEL    = os.getenv('LOCAL_LLM_MODEL', AZURE_OPENAI_DEPLOYMENT)
elif _OPENAI_KEY:
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
OPENAI_REASONING_EFFORT = os.getenv('OPENAI_REASONING_EFFORT', 'low')

# --- Two-tier harness generation --------------------------------------------

# Votes for the relation verifier's soundness review (run.py 6b). 1 = a
# single review (default). >1 = diverse-lens ensemble: the finding is
# dropped only when a strict majority of lenses judge the fired oracle
# unsound. Turn up only after the offline replay harness (verifier_replay)
# shows the single review leaks or over-kills on the logged cases — each
# extra vote is an extra LLM call per fired oracle.
# Cycle 4a-ii tried votes=3 by default; the offline verifier-replay A/B
# (2026-07-25) measured IDENTICAL over-kill (17%) and leak (93%) rates vs
# votes=1 on the logged hard cases, at 3x judging cost -> reverted to 1.
RELATION_VERIFIER_VOTES = int(os.getenv('RELATION_VERIFIER_VOTES', '1'))

# Output token cap for reasoning models (GPT-5.x, o-series). Reasoning
# models spend tokens on hidden reasoning before emitting the answer, so
# without a generous cap the visible harness can come back truncated or
# empty. Sent as `max_completion_tokens` (the parameter reasoning models
# use; `max_tokens` is rejected). Ignored for standard sampling models
# and local servers.
OPENAI_MAX_COMPLETION_TOKENS = int(
    os.getenv('OPENAI_MAX_COMPLETION_TOKENS', '16384'))

# Per-request timeout (seconds) and automatic retry count for the LLM
# client. Reasoning models with high effort can take minutes to return
# the first byte on a synchronous call; a generous read timeout plus a
# couple of retries absorbs slow responses and transient connection
# drops (e.g. a proxy closing an idle connection -> APIConnectionError).
LLM_TIMEOUT_SECONDS = float(os.getenv('LLM_TIMEOUT_SECONDS', '6000'))
LLM_MAX_RETRIES     = int(os.getenv('LLM_MAX_RETRIES', '0'))

# Stream the chat-completion response instead of waiting for the whole
# body at once. Streaming keeps bytes flowing during generation, which
# stops an intermediary (proxy / Azure gateway / load balancer) from
# killing the connection as "idle" while a slow reasoning model is still
# thinking — the failure mode behind RemoteProtocolError ("Server
# disconnected without sending a response") -> APIConnectionError.
#   'auto'  (default) -> stream only for reasoning models, where the
#                        long pre-output thinking phase triggers the drop.
#   'always' / '1' / 'true'  -> stream for every model.
#   'never' / '0' / 'false'  -> never stream (old synchronous behaviour).
# Note: with streaming the SDK does NOT transparently retry a mid-stream
# disconnect the way it retries a non-streamed call, so keep your own
# retry/try-except around generate() (the campaign loop already does).
LLM_STREAM = os.getenv('LLM_STREAM', 'auto').strip().lower()

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

# Max DISTINCT call-site examples (xrefs) per touched function spliced into
# the prompt. fuzz-introspector returns one xref per call site and the whole
# enclosing method for each, so a method called many times duplicates its
# caller's source (e.g. NumberUtils.createNumber -> ~120KB of repeats). We
# dedupe and cap to keep the prompt lean without losing distinct callers.
MAX_XREFS_PER_FUNCTION = int(os.getenv('MAX_XREFS_PER_FUNCTION', '5'))

# Reachable-set computation is a budget-bounded BFS over immediate call-sites
# (see analysis.TargetAnalyzer._reachable_of), NOT introspector's unbounded
# transitive walk (which explodes to minutes of CPU on hub functions). The
# node cap makes it O(cap) regardless of call-graph size; depth floats up to
# REACHABLE_MAX_DEPTH within that budget. Evidence from the Defects4J bugs:
# every downstream manifest-site / sibling sits at depth 1, so a shallow,
# capped walk is both faster and more focused than going deep.
REACHABLE_NODE_CAP = int(os.getenv('REACHABLE_NODE_CAP', '200'))
REACHABLE_MAX_DEPTH = int(os.getenv('REACHABLE_MAX_DEPTH', '3'))
# Hard timeout (s) for the legacy get_reachable_functions fallback only, so an
# older introspector without base_callsites degrades to [] instead of hanging.
REACHABLE_TIMEOUT_SECONDS = int(os.getenv('REACHABLE_TIMEOUT_SECONDS', '30'))

# Wall-clock cap (s) on the one-time fuzz-introspector project parse
# (analyse_end_to_end). It has been seen to stall on some checkouts (Math-2);
# on timeout the run degrades to no-steering rather than hanging.
INTROSPECTOR_TIMEOUT_SECONDS = int(
    os.getenv('INTROSPECTOR_TIMEOUT_SECONDS', '120'))

# Depth cap for fuzz-introspector's method-depth metric. Its stock
# calculate_method_depth is an unbounded O(N^2+) DFS that stalls on large
# libraries (Math-2's commons-math3). We patch it to a depth-bounded recursion
# (returns min(true_depth, cap)) — fast, and still a meaningful metric rather
# than a 0 stub. Also settable via --introspector_depth_cap.
INTROSPECTOR_METHOD_DEPTH_CAP = int(
    os.getenv('INTROSPECTOR_METHOD_DEPTH_CAP', '3'))

# --- diff-hit instrumentation (--diffcov) ----------------------------------
# Inject a hit counter into every method the patch under evaluation changed,
# so a measurement run can read whether generated inputs actually REACH the
# changed code (docs/witness-study-2026-08-08.md §7, caveat (b)). OFF by
# default: with it on the patched build is recompiled into its own directory
# and its sources differ from the frozen guard-fixture baselines'.
# MEASUREMENT ONLY — never an input to a prompt, oracle, verifier or verdict.
DIFFCOV = os.getenv('VULNPATCH_DIFFCOV', '').strip().lower() in (
    '1', 'true', 'yes', 'on')

# How often the instrumented JVM dumps its counters to the collection file.
# It flushes on a timer rather than only at shutdown because the fuzz runner
# SIGKILLs the JVM on its subprocess timeout and libFuzzer ends a finding run
# from native code — neither runs JVM shutdown hooks.
DIFFCOV_FLUSH_SECONDS = float(os.getenv('DIFFCOV_FLUSH_SECONDS', '2'))

# --- divergence capture (--divcap) -----------------------------------------
# Run the bug's own trigger tests through TWO instrumented builds (buggy and
# patched) and record, per patch-changed method, the observable at the diff
# boundary. The pairs whose value MOVED are handed to relation SYNTHESIS as
# facts about WHICH observable the patch touches. OFF by default: with it on
# two extra trees are materialised and compiled.
# The values steer attention, never expectations — see
# docs/divcap-build-2026-08-10.md and the soundness rule in divcap.py.
DIVCAP = os.getenv('VULNPATCH_DIVCAP', '').strip().lower() in (
    '1', 'true', 'yes', 'on')

DIVCAP_FLUSH_SECONDS = float(os.getenv('DIVCAP_FLUSH_SECONDS', '2'))

# Distinct argument tuples watched per changed method. Capped COUNT, never
# capped width: the 8.31 truncation read (a value cut mid-array is a value
# nobody can pair, and the capped copy was the only copy).
DIVCAP_MAX_SHAPES = int(os.getenv('DIVCAP_MAX_SHAPES', '64'))

# Divergences handed to synthesis, ranked by distinct-shape diversity first
# and frequency second (pre-registration decision 3).
DIVCAP_TOP_K = int(os.getenv('DIVCAP_TOP_K', '8'))

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

# Where buggy checkouts get materialised. Overridable via env so concurrent
# or replay runs can isolate their checkouts (and preserve them for post-hoc
# inspection) instead of sharing /tmp/d4j and wiping each other's evidence.
D4J_CHECKOUT_ROOT = os.getenv('D4J_CHECKOUT_ROOT', '/tmp/d4j')

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

# --- OSS-Fuzz / libFuzzer front-end ---------------------------------------
# Local google/oss-fuzz checkout. Everything in src/oss_fuzz shells out to its
# infra/helper.py for builds and runs, and reads projects/<name>/project.yaml
# for the target's language, upstream repo, and supported sanitizers/engines,
# so this path — not a network API — is what "an OSS-Fuzz project" resolves
# against. The tree is only ever read, except for the build.sh crib line, which
# ossfuzz.build_harness restores under try/finally (the overwrite strategy
# touches only the target's own checkout, never this tree).
OSS_FUZZ_DIR = os.getenv('OSS_FUZZ_DIR', os.path.expanduser('~/oss-fuzz'))

# Where the target's upstream repo is cloned and the vuln/HEAD worktrees are
# added. Kept out of the oss-fuzz checkout so a wiped work dir can never
# damage the user's tree, and overridable so concurrent project runs don't
# fight over the same wt__vuln / wt__head paths.
OSS_FUZZ_WORK_DIR = os.getenv(
    'OSS_FUZZ_WORK_DIR', os.path.expanduser('~/.cache/vuln-patch/oss-fuzz'))

# Sanitizer used when neither --sanitizer nor the CVE's own OSV record names
# one. 'address' matches OSS-Fuzz's constants.DEFAULT_SANITIZER and covers the
# memory-safety bug classes that dominate C/C++ CVEs; a UBSan-only CVE needs
# --sanitizer undefined or its harness will compile and never trigger.
OSS_FUZZ_SANITIZER = os.getenv('OSS_FUZZ_SANITIZER', 'address')

# Per-harness budget (seconds) for the in-campaign trigger gate on the
# VULNERABLE build — the C/C++ analogue of VERIFY_TIMEOUT_SECONDS. Longer than
# the Java gate because libFuzzer starts from an empty corpus and has to
# discover the crashing input itself rather than replaying a unit test.
OSS_FUZZ_VERIFY_TIMEOUT = int(os.getenv('OSS_FUZZ_VERIFY_TIMEOUT', '120'))

# Per-harness budget (seconds) for the sibling hunt on HEAD. This is where the
# research signal comes from, so it is deliberately larger than the gate: the
# gate only has to re-find a known bug, HEAD has to find an unknown one.
OSS_FUZZ_FUZZ_TIMEOUT = int(os.getenv('OSS_FUZZ_FUZZ_TIMEOUT', '600'))

# Wall-clock cap (s) on a single `helper.py build_fuzzers` invocation. Generous
# by default because build time swings wildly with host architecture: on an
# x86_64 Linux host a mid-size project builds in minutes, but under amd64
# emulation on an arm64 Mac assimp took ~2 minutes PER OBJECT FILE (51 of 248
# in 98 minutes), so anything tighter kills builds that would have succeeded.
# On timeout the run is reported as an infrastructure failure, never as a
# harness that "failed to compile".
OSS_FUZZ_BUILD_TIMEOUT = int(os.getenv('OSS_FUZZ_BUILD_TIMEOUT', '5400'))

# OSV REST endpoint used to pick the target CVE (osv.py, stdlib urllib only).
# OSS-Fuzz's disclosed bugs land here via google/oss-fuzz-vulns, which is why
# OSV — not the oss-fuzz repo — is the CVE source of truth.
OSV_API_URL = os.getenv('OSV_API_URL', 'https://api.osv.dev/v1')