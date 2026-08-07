# Roll 8 — how to read it (written BEFORE launch, deliberately)

Written so the read does not depend on anyone's memory of the conversation.

## Where it lands

`runs-archive/runs/ladder1i_*` on the Mac after rsync; on the VM at
`/home/code/scratch/runs/ladder1i_*`. Log: `/home/code/scratch/ladder1i.log`.

## Protocol, unchanged for seven rolls

1. `rsync -az -e ssh hetzner:/home/code/scratch/runs/ladder1i_<stamp>/ runs-archive/runs/ladder1i_<stamp>/`
2. Copy summary/config/result into `docs/replay/backtrack/ladder1-raw/roll8/`
3. **Commit raw BEFORE reading it.** Raw-before-interpretation has held every roll.
4. Then read.

## The read: the per-event chain

```python
import re
t = 'runs-archive/runs/ladder1i_<stamp>/01_patch1-Math-65-CapGen_c/trace.md'
for s in re.split(r'\n(?=## \[\d+\])', open(t, errors='ignore').read()):
    if 'reference-impl' not in s.split('\n')[0]:
        continue
    m = re.search(r'\*\*output:\*\*\s*\**([^*\n]+)', s)
    r = re.search(r'- reason: ([^\n]{0,320})', s)
    d = re.search(r'- detail: ([^\n]{0,140})', s)
    print(s.split('\n')[0][:50], '->', (m.group(1).strip() if m else '?')[:130])
    if r: print('    why:', r.group(1)[:300])
    if d: print('    det:', d.group(1)[:130])
```

Skip `memoized result reused` lines — they are later firings reusing the
per-leg resolution, not new information.

## Expected chain, and what each outcome means

```
disputed observable detected      -> ['getChiSquare']
reference generated               -> chars
signature declared                -> named OR bare types; BOTH now map
screening surface resolved        -> 6 computed siblings
reference observables matched     -> getChiSquare/getRMS, declared_only
signature mapped                  -> N parameters to state fields
twin setup extracted              -> receiver `optimizer`
twin built                        -> helpers/imports/package
--- everything above has ALREADY worked on real material ---
twin ran                          -> THE FIRST UNPROVEN STEP (roll 6's
                                     classpath seam; roll 7 died upstream)
screen ADMITTED / DISCARDED       -> THE REAL QUESTION
pin-check PASSED / DISCARDED
fact emitted
```

### If the twin fails
`_jvm_failure_reason` now puts the JVM's last lines into the discard reason, so
a missing class, a thrown exception and a silent exit read differently. The
reason IS the diagnosis; no re-roll needed to get it.

### If the screen runs — this is what stage 1 exists to learn
* **admits** -> a fact should emit. Then gate (c) is live: does the judge's WHY
  address the fact's CONTENT, not merely contain it? (Measured the hard way on
  the Math-65 block: delivered at char 27,051 of a 59,830-char prompt, read by
  chance.)
* **discards on a sibling** -> read the reason: a real semantic gap, or a
  convention mismatch (units/rounding)? Only the second is worth iterating.
* **disagrees at the disputed point only** -> the mechanism working exactly as
  designed on its first live case.

## Gate status to carry forward

* (a) canaries — unexercised live
* (b) zero facts from discarded references — **13-for-13**, must hold
* (c) fact engages the disputed formula — unreached
* (d) rule 7 — counter NOT started; every roll so far changed a different
  mechanism. Roll 8 must not repeat a known failure.

## Standing context

Ten integration defects across seven rolls and five VM walkthroughs, all one
shape: correct pieces, wrong seam. Walkthrough-before-launch is standing
practice — eight of the ten were found at desk cost rather than ~250k each.
