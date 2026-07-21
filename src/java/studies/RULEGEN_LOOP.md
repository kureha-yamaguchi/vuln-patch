# Rule-generation iteration loop — how to run it

Cheap, isolated measurement of RULE GENERATION quality (synth → screen →
replay), skipping harness-gen + judge. ~8k tokens/leg (~$0.03) vs a full
leg (~50-100k). Use it to iterate R1/R2/R3 and context changes.

## Run settings (standing defaults for the loop)
- `PARALLEL=4` — the VM has 8 cores and jazzer is multi-threaded; 6 was
  oversubscribed (load ~20). 4 is faster on this box. A bigger VM is the
  only real way to run more legs at once (CPU-bound, not API-bound).
- `--screen_runs 5000` — fuzz iterations per candidate in screening AND
  replay. Faster than the 20000 default, slightly noisier fire-ratio.
  Keep 20000 only for a final apples-to-apples measurement.
- Both set in the cases file's COMMON; PARALLEL is a launch env var.

## Cases file COMMON template
    COMMON="-n 1 -m 1 --fuzz_timeout 20 --synthesize_relations \
            --rulegen_only --screen_runs 5000 [--rule_compile_repair]"

## Metrics (study/rulegen_join.py <run_dir>)
- convict / clean-convict: a screened relation fires on the overfit-patch
  build (replay) and stays quiet on the correct one.
- false-fire: a relation fires on a correct patch (precision).
- quantity: candidates and survivors per leg.

## Baseline (2026-07-18, 20000 runs, 1 sample/leg)
convict 6/16, false-fire 0/16, 2.2 survivors/leg. Two miss buckets:
- compile-deaths (Closure-33, Closure-92 lost ALL candidates) -> R1.
- replay input-coverage (Lang-41, Lang-60 generated the right relation
  but replay couldn't fire it) -> separate BND-style fix, not rule-gen.

## Notes
- Single-sample rule-gen is noisy per leg (Math-2 stochastic). The
  aggregate over 16 bugs is steadier; bump to 2-3 samples per leg if a
  change's effect is within the per-leg noise.
- One change per measurement (p23gate lesson). Compare convict + false-
  fire against the baseline each time; keep only what raises convict
  without adding false-fires. Check every change is a GENERAL category,
  never a benchmark-shaped hint.
