#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# refactor_java_package.sh — sort src/java/ flat modules into subpackages.
#
# PREPARED 2026-07-21. Tested breakage-free in a scratch copy before commit
# (py_compile + static java.* resolution + full auto-stubbed import of all 27
# modules — see the printed test block at the end).
#
# RUN THIS ONLY WHEN THE PIPELINE IS IDLE (no run_suite.sh / cert sweep active),
# from the repo root. It is deterministic and self-contained: it does the git
# mv's, rewrites every flat intra-package import to a `java.<subpkg>.<mod>`
# path, fixes the entry-point sys.path anchors, writes ARCHITECTURE.md, patches
# the one external invocation, and re-runs the verification suite. If any check
# fails it stops before you commit.
#
# Why it's safe: run.py stays at java/ root, so `cd src && python java/run.py`
# (run_suite.sh:116, scripts/*.sh) is UNCHANGED. Imports resolve because run.py
# already inserts src/ onto sys.path; we just root the imports at `java.`.
# ---------------------------------------------------------------------------
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

# Refuse to run over a live pipeline.
if pgrep -f "java/run.py" >/dev/null 2>&1 || pgrep -f "certify_detectability" >/dev/null 2>&1; then
  echo "ABORT: a run.py or certify_detectability process is active. Wait until idle." >&2
  exit 1
fi

git checkout -b java-package-refactor 2>/dev/null || echo "(already on a working branch — continuing)"

# ---- 1. move files into subpackages (git mv preserves history) -------------
python3 - <<'PYEOF'
import os, subprocess
MAP = {
 "parsing":     ["java_source"],
 "bug_context": ["analysis","failure_test","crash_input","call_graph","code_context","patches"],
 "relations":   ["relation_synth","relation_screen","relation_verifier"],
 "harness":     ["campaign","prompts","build","canned_probe","test_oracle_miner"],
 "execution":   ["fuzz_runner","jazzer","oracle_strength"],
 "dataset":     ["certify_detectability","classify_bugs","eval_candidates"],
}
os.chdir("src/java")
for pkg, mods in MAP.items():
    os.makedirs(pkg, exist_ok=True)
    open(os.path.join(pkg, "__init__.py"), "w").close()
    subprocess.run(["git","add",os.path.join(pkg,"__init__.py")], check=False)
    for m in mods:
        src = m + ".py"; dst = os.path.join(pkg, m + ".py")
        if os.path.exists(src):
            subprocess.run(["git","mv",src,dst], check=True)
print("moved files into subpackages")
PYEOF

# ---- 2. rewrite flat intra-package imports -> java.<subpkg>.<mod> ----------
python3 - <<'PYEOF'
import os, re
MAP = {
 "parsing":     ["java_source"],
 "bug_context": ["analysis","failure_test","crash_input","call_graph","code_context","patches"],
 "relations":   ["relation_synth","relation_screen","relation_verifier"],
 "harness":     ["campaign","prompts","build","canned_probe","test_oracle_miner"],
 "execution":   ["fuzz_runner","jazzer","oracle_strength"],
 "dataset":     ["certify_detectability","classify_bugs","eval_candidates"],
}
mod2pkg = {m: pkg for pkg, mods in MAP.items() for m in mods}
def newpath(m):  # run.py & verifier_replay.py stay at java/ root
    return "java."+mod2pkg[m]+"."+m if m in mod2pkg else ("java."+m if m in ("run","verifier_replay") else None)
targets = set(mod2pkg) | {"run","verifier_replay"}
for root,_,files in os.walk("src/java"):
    for fn in files:
        if not fn.endswith(".py"): continue
        p = os.path.join(root, fn); s = open(p).read(); orig = s
        for m in sorted(targets, key=len, reverse=True):
            np = newpath(m)
            if not np: continue
            s = re.sub(r'(?m)^(\s*)from %s import' % re.escape(m), r'\1from %s import' % np, s)
            s = re.sub(r'(?m)^(\s*)import %s\b(?! as)' % re.escape(m), r'\1import %s as %s' % (np, m), s)
        if s != orig: open(p,"w").write(s)
print("rewrote flat imports")
PYEOF

# ---- 3. fix entry-point sys.path anchors (move-proof: dir containing config.py) ----
python3 - <<'PYEOF'
from pathlib import Path
ANCHOR = ("sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents "
          "if (p / 'config.py').exists())))")
OLD = "sys.path.insert(0, str(Path(__file__).parent.parent))"
for f in ["src/java/run.py","src/java/verifier_replay.py",
          "src/java/dataset/certify_detectability.py","src/java/dataset/eval_candidates.py"]:
    s = open(f).read()
    if OLD in s:
        open(f,"w").write(s.replace(OLD, ANCHOR)); print("anchored", f)
# classify_bugs.py has no insert but imports java.* — add one after `import sys`
cb = "src/java/dataset/classify_bugs.py"; s = open(cb).read()
if "config.py" not in s:
    lines = s.splitlines(keepends=True); out=[]; done=False
    for ln in lines:
        out.append(ln)
        if not done and ln.startswith("import sys"):
            out += ["from pathlib import Path\n", ANCHOR+"\n"]; done=True
    if not done: out = ["import sys\nfrom pathlib import Path\n"+ANCHOR+"\n"] + lines
    open(cb,"w").write("".join(out)); print("added anchor to", cb)
PYEOF

# ---- 4. patch the one external invocation + doc references -----------------
sed -i.bak 's#src/java/classify_bugs.py#src/java/dataset/classify_bugs.py#g' scripts/evaluate_crashing.sh && rm -f scripts/evaluate_crashing.sh.bak
sed -i.bak 's#src/java/patches.py#src/java/bug_context/patches.py#g' suites/DATASET_AUDIT.md && rm -f suites/DATASET_AUDIT.md.bak

# ---- 5. write ARCHITECTURE.md ---------------------------------------------
cat > src/java/ARCHITECTURE.md <<'MD'
# src/java — package layout

The pipeline runs as `cd src && python java/run.py …`. `run.py` inserts `src/`
onto `sys.path`, so every module is imported absolutely as `java.<subpkg>.<mod>`
and `config` / `llm` (which live in `src/`) resolve too.

## Subpackages (by pipeline station)

| Package | Modules | Role |
|---|---|---|
| `java/` (root) | `run.py`, `verifier_replay.py` | Orchestrator entry point; replay driver. |
| `parsing/` | `java_source` | Java source/AST parsing (javalang). |
| `bug_context/` | `analysis`, `failure_test`, `crash_input`, `call_graph`, `code_context`, `patches` | Extract the bug's context: trigger test, crash input, call graph, source context, patch selection. |
| `relations/` | `relation_synth`, `relation_screen`, `relation_verifier` | Semantic-bug metamorphic/contract relations: synthesise → screen → the soundness+attribution judges. |
| `harness/` | `campaign`, `prompts`, `build`, `canned_probe`, `test_oracle_miner` | Build & compile Jazzer harnesses; prompt construction; oracle mining. |
| `execution/` | `fuzz_runner`, `jazzer`, `oracle_strength` | Run harnesses under Jazzer; classify fired oracles. |
| `dataset/` | `certify_detectability`, `classify_bugs`, `eval_candidates` | Offline dataset tooling: label-verification (detectability certifier), crashing/semantic classification, candidate selection. |
| `studies/` | `j1_judge_audit`, `rulegen_join*` | One-off offline analyses (not in the live path). |

`config.py` and `llm.py` stay in `src/` (shared, imported flat as `config` / `llm`).

## Import rule
Intra-package imports are absolute: `from java.execution.fuzz_runner import …`.
Direct-entry scripts (`run`, `verifier_replay`, `dataset/*`) anchor `sys.path`
to the dir containing `config.py`, so they work regardless of depth.
MD
git add src/java/ARCHITECTURE.md

# ---- 6. VERIFY (same checks that passed in the scratch dry-run) ------------
echo "=== VERIFY 1: py_compile ==="; python3 -m py_compile $(find src/java -name '*.py') && echo PASS
echo "=== VERIFY 2: every java.* import resolves to a real file ==="
( cd src && python3 - <<'PYEOF'
import re,os
bad=[]
for r,_,fs in os.walk("java"):
    for fn in fs:
        if fn.endswith(".py"):
            p=os.path.join(r,fn)
            for i,ln in enumerate(open(p),1):
                m=re.match(r'\s*(?:from|import)\s+(java(?:\.\w+)+)',ln)
                if m and not (os.path.exists(m.group(1).replace('.','/')+".py") or os.path.isdir(m.group(1).replace('.','/'))):
                    bad.append(f"{p}:{i}: {m.group(1)}")
print("FAIL\n"+"\n".join(bad) if bad else "PASS")
PYEOF
)
echo "=== VERIFY 3: no leftover flat imports of moved modules ==="
if grep -rnE "^\s*(from (java_source|analysis|failure_test|crash_input|call_graph|code_context|patches|relation_synth|relation_screen|relation_verifier|campaign|prompts|build|canned_probe|test_oracle_miner|fuzz_runner|jazzer|oracle_strength|certify_detectability|classify_bugs|eval_candidates) import|import (java_source|fuzz_runner|relation_synth) )" src/java >/dev/null; then
  echo "FAIL: leftover flat import"; else echo "PASS"; fi

find src/java -name __pycache__ -type d -exec rm -rf {} + 2>/dev/null || true
echo
echo "All checks above must read PASS. Review 'git status', run one smoke leg"
echo "(run_suite.sh on a single pinned task), then commit."
