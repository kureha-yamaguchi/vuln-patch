"""Configuration for the CVE incomplete-fix scanner.

Standalone from the harness pipeline's `src/config.py`; everything here is
read from environment variables with sensible defaults so the scanner
works out of the box.
"""
import os


# --- Disk layout ----------------------------------------------------------
# Cache root for every artifact pulled by the scanner. Sheet CSVs land in
# {root}/p0/, narrative HTML in {root}/p0/narrative/, the cloned RCA repo
# in {root}/p0_repo/, and LLM responses in {root}/llm/.
CVE_SCAN_CACHE_DIR = os.path.expanduser(
    os.getenv('CVE_SCAN_CACHE_DIR', '~/.cache/cve_scan'),
)
# Root of the output tree.  Pipeline JSON/CSV artifacts land in
# `<output>/pipeline/`; the human-readable seeds_table.md and the
# subdir READMEs sit at the top level.  Manual-research files go in
# `<output>/verified/`.
#
# Default assumes the CLI is invoked from `src/db/project_zero/` (the
# layout documented in cve_scan/README.md), so the relative path lands
# the outputs at `src/db/project_zero/findings/...`.  Override via the
# `CVE_SCAN_OUTPUT_DIR` env var if invoking from elsewhere.
CVE_SCAN_OUTPUT_DIR   = os.getenv('CVE_SCAN_OUTPUT_DIR', './findings')
CVE_SCAN_PIPELINE_DIR = os.path.join(CVE_SCAN_OUTPUT_DIR, 'pipeline')


# --- Project Zero sources -------------------------------------------------
# The "0-day in the wild" tracker. Sheet ID is stable; year-tabs are pulled
# lazily via the gviz CSV-export endpoint.
P0_SHEET_ID = '1lkNJ0uQwbeC1ZTRrxdtuPLCIl7mlUreoKfSIgajnSyY'
P0_REPO_URL = 'https://github.com/googleprojectzero/0days-in-the-wild.git'

# Curated blog posts that enumerate variant 0-days by CVE. These contain
# the densest variant-pair prose; the harvester scrapes them alongside the
# per-CVE RCA Markdown.
P0_NARRATIVE_URLS = [
    'https://projectzero.google/2022/11/mind-the-gap.html',
    'https://projectzero.google/2022/06/2022-0-day-in-wild-exploitationso-far.html',
    'https://projectzero.google/2021/02/deja-vu-lnerability.html',
    'https://projectzero.google/2020/07/detection-deficit-year-in-review-of-0.html',
    'https://googleprojectzero.github.io/0days-in-the-wild/rca.html',
]


# --- LLM edge-classifier --------------------------------------------------
# The OpenAI SDK reads OPENAI_API_KEY from the environment automatically
# when no api_key is passed; we just surface the model + budget knobs here.
LLM_CLASSIFIER_MODEL = os.getenv('LLM_CLASSIFIER_MODEL', 'gpt-5-mini')
LLM_CACHE_DIR = os.path.join(CVE_SCAN_CACHE_DIR, 'llm')
LLM_MAX_BUDGET_USD = float(os.getenv('LLM_MAX_BUDGET_USD', '25.0'))

# Pricing for spend tracking — keep these in sync with OpenAI's pricing
# page. Defaults are placeholders; update once gpt-5-mini's published rate
# is confirmed.
LLM_PRICE_IN_USD_PER_MTOK  = float(os.getenv('LLM_PRICE_IN_USD_PER_MTOK',  '0.25'))
LLM_PRICE_OUT_USD_PER_MTOK = float(os.getenv('LLM_PRICE_OUT_USD_PER_MTOK', '2.00'))
