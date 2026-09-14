# The five region metrics — crashing split, holdout side

Same run as `summary.md`. No harness was regenerated and no fuzzer was
re-run. `src/metrics/report.py` re-read this directory, rebuilt $\mathbb{P}$
from each leg's APR patch with the pipeline's own static analysis, and read
$C$ from the measurement pass's saved Jazzer output. Per-bug records are in
`metrics.jsonl`.

`R̂` is the set of methods the Defects4J developer fix changes. `P` is the
APR patch's changed methods plus their static neighbourhood. `F(H)` is the
set of methods the accepted harness set ran. `C` is the harness set's Jazzer
findings.

| bug | \|R̂\| | \|P\| | \|F(H)\| | \|C\| | RCC | RCR | RCP | PSC | CSM |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Chart-5 | 1 | 11 | 17 | 3 | 1.000 | 1.000 | 0.059 | 0.545 | 1.000 |
| Lang-16 | 1 | 19 | 7 | 3 | 1.000 | 1.000 | 0.143 | 0.474 | 1.000 |
| Lang-20 | 2 | 4 | 14 | 8 | 1.000 | 1.000 | 0.143 | 0.750 | 0.750 |
| Lang-43 | – | – | – | – | – | – | – | – | – |
| Lang-45 | 1 | 3 | 2 | 3 | 1.000 | 1.000 | 0.500 | 0.667 | 1.000 |
| Lang-58 | 1 | 25 | 7 | 3 | 1.000 | 1.000 | 0.143 | 0.280 | 1.000 |
| Lang-59 | 1 | 4 | 3 | 3 | 1.000 | 1.000 | 0.333 | 0.750 | 1.000 |
| Math-58 | 1 | 13 | 82 | 3 | 1.000 | **0.000** | 0.012 | 0.923 | **0.000** |
| Math-70 | 1 | 14 | 10 | 3 | 1.000 | 1.000 | 0.100 | 0.429 | **0.000** |
| Math-85 | 1 | 7 | 48 | 5 | 1.000 | 1.000 | 0.021 | 0.429 | 0.600 |

**mean RCC = 1.000, RCR = 0.889, RCP = 0.162, PSC = 0.583, CSM = 0.706,
each over the 9 scored bugs.**

Lang-43 is not scored on any metric. Its leg accepted no harness, so there
is no H to measure. That is a pipeline outcome, not a low score.

Every crash site resolved to a real method. `crashes_unresolved` is 0 on
all nine bugs, so no number below is a plumbing artefact.

## What the four new metrics add

RCC alone reported 1.000 on every scored bug, because the pipeline accepts a
harness only after it crashes the buggy build, and the crash path runs
through the method the developer changed. The other four separate the bugs
that RCC cannot.

### RCR = 0.000 on Math-58 — the patch-derived set missed the fix region

Math-58's overfitting patch changes
`LevenbergMarquardtOptimizer.determineLMParameter`. The developer fix
changes `GaussianFitter.fit()`, in another package. So $\mathbb{P}$ and
$\hat{\mathbb{R}}$ do not intersect, and RCR is 0.

RCR is static. No harness set can raise it, so on this bug it caps every
patch-conditioned method at the start. It is also the one metric here that
scores the EXTRACTION rather than the harness set, which is why it is worth
reading first.

The harness set still reached `GaussianFitter.fit()`, so RCC is 1.000. The
fitter is on the path to the optimizer. RCC and RCR disagreeing is the
useful signal: the harness got there, but not because $\mathbb{P}$ pointed
at it.

### CSM = 0.000 on Math-70 — the crash site is one call from the fix site

All three of Math-70's crashes are the same NullPointerException, and every
one of them happens in `BisectionSolver.solve(f, min, max)` at line 88.
$\hat{\mathbb{R}}$ is the FOUR-argument overload,
`solve(f, min, max, initial)`, whose body is `return solve(min, max);` — it
drops `f`, and the stored field is null.

So the fault is in the 4-argument overload and the throw is in the
3-argument one, two frames up the stack. `site(c)` is the innermost
non-harness frame, so CSM scores 0 while RCC scores 1.

This is the case the README's narrow-$\hat{\mathbb{R}}$ caveat predicts, now
measured. It is not a parser fault: all three traces resolved cleanly, to
the overload that really threw.

### PSC — the first metric with real spread

PSC runs from 0.280 (Lang-58) to 0.923 (Math-58), mean 0.583. It is the only
metric of the five that varies widely across a run in which every harness
set was accepted the same way.

PSC and $|\mathbb{P}|$ move against each other. Lang-58 has the largest
neighbourhood of the nine ($|\mathbb{P}| = 25$) and the lowest PSC. Lang-20
and Lang-59 have $|\mathbb{P}| = 4$ and PSC 0.750. So PSC is currently as
much a measure of how wide the static neighbourhood is as of how hard the
harness set worked it. Compare PSC between harness sets on ONE bug, not
across bugs.

### RCP is small by construction

Mean RCP is 0.162, and Math-58's is 0.012. The numerator is at most
$|\hat{\mathbb{R}}|$, which is 1 on eight of the nine scored bugs, and the
denominator is every method the harness set ran — 82 on Math-58. So RCP here
is close to $1 / |\mathbb{F}(H)|$ and mostly measures how much code the
harness had to run to get anywhere.

RCP is still usable as a PAIRED comparison: on one bug, a set with a higher
RCP spent more of its budget in the root-cause region. It is not readable as
an absolute score while $|\hat{\mathbb{R}}|$ stays at 1.

## What this still does not measure

1. **RCC remains saturated.** Crash acceptance implies reach, so RCC = 1.000
   confirms the plumbing and does not discriminate between harness sets.
2. **The denominators of RCC and RCR are 1** on eight of nine bugs, so both
   are near-binary per bug and their means are hit rates.
3. **There is no $H_N$.** The comparison $RCC(H_N) \ll RCC(H_R)$ is still
   untested, and $H_N$ carries no crash-acceptance rule, so caveat 1 does not
   apply to it.
4. **One repetition.** No confidence interval is calculable from this run.
5. **$\mathbb{P}$ was rebuilt, not recorded.** The static analysis is
   deterministic under the same budget, so it reproduces what the pipeline
   saw. `rcc_sweep.py` now records $\mathbb{P}$ during the run, so later runs
   will not need the rebuild.
