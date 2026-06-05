# `src/project_zero/` — patch-verification pipeline (design, not yet built)

The third verification vertical, alongside [`src/java/`](../java) (Jazzer /
Defects4J, mature) and [`src/linux/`](../linux) (kernel, partial). It would
consume the **43 READY Project Zero variant-pairs** materialised in
[`src/db/project_zero/pairs/`](../db/project_zero/pairs) and check, per pair,
whether the prior fix was genuinely incomplete by reproducing the bug at the
**Fix-0** state and confirming it is gone at **Fix-1**.

**Status: this directory is a design note only.** No pipeline code yet — the
point of this document is to lay out the *categories of apps and bugs* in the
dataset and the *handling each would need* before any module is written,
because (unlike Java/Linux) the P0 dataset spans many codebases with no single
build system or harness framework.

## The verification model (carried over from `src/linux/`)

Each pair stores `fix0 = prior` (the incomplete fix) and `fix1 = later` (the
corrective fix); see [pairs/README.md](../db/project_zero/pairs/README.md).
The check depends on `relationship_kind` in `metadata.json`:

- **`incomplete_fix` (20 pairs) → 3-state ground truth.** A reproducer for the
  *later* bug must **trigger** at `fix0_parent` (unpatched) **and** at `fix0`
  (because the prior fix was incomplete), then be **clean** at `fix1`. This is
  the gold signal — it proves the Fix-0 patch left the bug reachable.
- **`same_root_cause` / `one_extends_other` (23 pairs) → 2-state regression.**
  The later bug is independent code sharing a root-cause class, so it need not
  trigger at `fix0`. Verification is the ordinary "triggers before its own
  fix, clean after" — useful as variant-analysis corpus, weaker as a
  patch-incompleteness proof.

Everything below is about the hard part: **how you actually build and trigger
a reproducer**, which differs entirely by codebase.

## Categories of apps (codebases)

Derived from `affected_files` / `repo_url` across the 43 pairs (the `codebase`
label in metadata is coarser and mislabels a few — trust the files).

| family | pairs | lang | build system | reproducer form | crash oracle | weight |
|---|---|---|---|---|---|---|
| **JS engines** — V8 (`d8`), SpiderMonkey (`js`) | ~24 | C++ | gn+ninja / mach, but only the **shell** is needed (~1 GB) | a **JS PoC** fed to the shell | ASAN abort / `DCHECK`/`CHECK` failure, nonzero exit | **light, no VM** |
| **Browser renderer** — Chrome/Blink, Gecko DOM/media/cache | ~16 | C++ | full Chrome (gn, 61 GB) / Firefox (mach) | HTML+JS page driven through `content_shell` / a gtest, or a Mojo/IPC harness | ASAN under `content_shell` | **heavy build** |
| **Standalone libs** — FreeType, Skia | 2 | C / C++ | small cmake / gn | a **malformed input file** + a tiny CLI/libFuzzer driver | ASAN OOB/overflow report | light |
| **Kernel / GPU drivers** — Linux (binder, af_unix), Qualcomm Adreno, ARM Mali | ~4 | C | full kernel build per device tree | **syscall sequence** (ioctl-heavy for GPU) | KASAN in dmesg — needs a **VM** | heavy, blocked on `src/linux` |

Concrete examples per family:

- **JS engine** — `CVE-2021-30551__CVE-2022-1096` and `CVE-2022-1096__CVE-2022-1232`
  (V8 object interceptor side-effects, `src/objects/objects.cc` →
  `js-objects.cc`); `CVE-2019-9810__CVE-2019-11707__CVE-2019-17026`
  (SpiderMonkey IonMonkey `AliasAnalysis.cpp` type confusion).
- **Browser renderer** — `CVE-2019-13720__CVE-2020-642x` (Blink WebAudio UAF /
  thread races, `webaudio/*.cc`); `chromium:1066893__CVE-2020-6572`
  (Chrome Mojo audio-decoder service).
- **Standalone lib** — `chromium-p0:168__CVE-2020-15999` (FreeType sbix PNG
  integer overflow → OOB); `CVE-2023-2136__CVE-2023-6345` (Skia).
- **Kernel/GPU** — `CVE-2019-2215__CVE-2020-0030` (Android binder UAF);
  `CVE-2020-11261__CVE-2023-33107` (Qualcomm Adreno `kgsl_iommu.c`);
  `CVE-2021-28664__CVE-2021-39793` (Mali GPU); `CVE-2021-0920__CVE-2021-4083`
  (Linux `af_unix.c` GC UAF).

## Categories of bugs (bug classes)

The bug class dictates how deterministic the reproducer is — the real driver
of how hard verification is, independent of codebase.

| bug class | where it shows up here | reproducer | determinism / handling |
|---|---|---|---|
| **OOB / overflow** | FreeType sbix PNG, Skia, some V8 | malformed font/image, or a JS array PoC | **most deterministic** — single input triggers ASAN; easiest oracle |
| **Type confusion** | V8 map/JIT (`map-updater.cc`, `simplified-lowering.cc`) | JS that induces a bad type assumption | deterministic JS; ASAN/`CHECK`. Tractable |
| **Callback / interceptor side-effect** | V8 object interceptors (`objects.cc`) | JS defining an interceptor with a side effect | deterministic JS; the cleanest incomplete-fix class in the set |
| **Use-after-free / lifetime** | Blink WebAudio, binder, Adreno, Gecko cache | a sequence that frees then re-uses | often **racy** → flaky; ASAN UAF report; may need many runs |
| **Race / concurrency** | WebAudio threads, cppgc marking | multi-threaded trigger | **nondeterministic** — hardest oracle; may need TSAN or repeated runs |

## Handling a pipeline would need

To turn a pair into a verdict you need three things per pair, all
engine-specific:

1. **A buildable tree at the fix states.** One shared clone per engine + git
   worktrees at `fix0_parent` / `fix0` / `fix1`, built with the sanitizer
   (ASAN for userland, KASAN for kernel). Cheap for the JS shells (~1 GB,
   minutes with ccache), prohibitive for Chrome/Firefox, VM-bound for kernel.
2. **A reproducer generator** (the LLM step, shared `src/llm.py`) emitting the
   right input form: a **JS PoC** for engines, an **HTML/JS page** or gtest for
   the browser, a **malformed file** for libs, a **syscall C program** for the
   kernel. The patch diff + the deep verifier's reasoning in `metadata.json`
   seed the prompt (which function the prior fix missed, which guard the later
   adds).
3. **A crash oracle + the 3-state / 2-state runner.** ASAN/`DCHECK` exit for
   userland; KASAN dmesg inside a VM for the kernel; then apply the relationship
   logic above.

## Tractability and where to start

| tier | families | pairs | verdict |
|---|---|---|---|
| **A** | JS engines + PoC | ~24 | **start here** — userland, light build, no VM; the interceptor/type-confusion classes are deterministic |
| **B** | standalone libs + libFuzzer | 2 | tractable, classic |
| **C** | full browser / Blink | ~16 | hard — 61 GB+ builds; `content_shell` helps but it's an infra project |
| **D** | kernel / GPU + VM | ~4 | hardest — needs VM orchestration; **blocked on finishing `src/linux/`** |

**Recommended first build: a V8 vertical.** It is the largest single slice
(~20 pairs), the most Java-like (one buildable target, a scriptable trigger),
and avoids every hard part — no VM, no 61 GB tree, deterministic crashes for
the dominant bug classes. The `incomplete_fix ∩ V8` subset is the bullseye.

## Proposed module layout (future — not built)

Mirrors `src/linux/`, one set of adapters per engine:

```
src/project_zero/
├── README.md            ← this design note
├── analysis_pz.py       build context from pairs/<>/metadata.json + patches
│                        (touched functions, reachable region, missed site)
├── prompts_pz.py        per-engine reproducer prompt (JS / page / file / syscall)
├── checkout_pz.py       shared clone + 3-state worktrees + sanitizer build
├── build_pz.py          compile/extract the generated reproducer
├── verify_pz.py         crash oracle + 3-state / 2-state runner
├── run_pz.py            orchestrator (generate → build → verify → repeat)
└── engines/             per-engine adapters (v8/d8 first; spidermonkey, …)
```

Shared with the existing pipelines: `src/llm.py` (`HarnessGenerator`),
`src/config.py`, and the dataset contract in
[`src/db/project_zero/pairs/`](../db/project_zero/pairs).
