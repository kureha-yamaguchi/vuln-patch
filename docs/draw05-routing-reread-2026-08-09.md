# Draw-05 re-read — where the null probes actually went (2026-08-09)

**Status:** complete. Re-read demanded by the 8.32 flag in `docs/witness-study-2026-08-08.md` §6.
**Scope:** desk analysis only. No pipeline runs, no code changes.
**Materials read:** the three Chart-19 traces in `runs-archive/runs/invdiv_20260808_203424/` (draws 04, 05, 06) and their `result.jsonl`; the Chart-19 buggy source, checked out fresh on the VM with `defects4j checkout -p Chart -v 19b -w /tmp/c19b`; the patch file `/home/code/drr/Patches/Doverfitting/Arja/Chart/patch1-Chart-19-Arja-plausible.patch`; and two pipeline source files named below.

**Verdict up front: FORK-ORACLE.** Draw 05 called `AbstractObjectList.indexOf(null)` on the patched build, with null absent from the list — the exact ground-truth distinguishing input — and the patched build threw the exception the overfit patch adds. The relation that made the call swallowed that exception in its own `catch (Exception e) { return; }` and reported nothing. A second, independent leg (the plot-level null probes) is a receiver-state and probe-ordering gap, not an input-value gap. Neither leg is a boundary-value reach failure.

---

## 1. The question, and why it gates the next build

The witness study concluded that a general seeder should start with a universal boundary lattice, whose first atom is `null`. Chart-19 is one of the ten bugs that study said a lattice covers. But draw 05 already invented null probes and still missed, so before building a seeder we have to know which of three things went wrong:

- **FORK-ROUTING** — the null probes never reached the patched method at all (they null-probed something else). Then the lever is diff-targeted probe routing, not richer inputs.
- **FORK-ORACLE** — the probes did call the patched method with null, and the check failed to report what came back. Then the lever is the check's shape, and a seeder would add nothing.
- **FORK-REACH** — the probes call the patched method but never with null in that call's own argument. Then a boundary-lattice seeder is exactly right.

The three answers point at three different builds, so the read has to be mechanical.

### What the patch actually does

Verbatim, the whole patch (also quoted at draw-05 trace lines 11–21):

```diff
--- /source/org/jfree/chart/util/AbstractObjectList.java
+++ /source/org/jfree/chart/util/AbstractObjectList.java
@@ -161,6 +161,9 @@
                 return (index);
             }
         }
+        if (object == null) {
+        	  throw new IllegalArgumentException("Null 'object' argument.");
+        	}
         return -1;
     }
```

The method it edits, read from `/tmp/c19b/source/org/jfree/chart/util/AbstractObjectList.java` lines 158–166:

```java
    protected int indexOf(Object object) {
        for (int index = 0; index < this.size; index++) {
            if (this.objects[index] == object) {
                return (index);
            }
        }
        return -1;
    }
```

The added throw sits **after** the loop. So the patched build's behaviour splits cleanly:

- `indexOf(null)` when a null **is** stored in the list — the loop matches it and returns that index. The patch changes nothing.
- `indexOf(null)` when no null is stored — the loop falls through, the added line runs, and an `IllegalArgumentException` is thrown where the unpatched build returned `-1`.

Only the second case distinguishes the overfit patch. That is also the ground-truth witness's shape (an empty `AbstractObjectList`, then `indexOf((Object) null)`).

### The two ways a probe can get to that method

Both were used by these runs, and both are real:

1. **Directly.** `org.jfree.chart.util.ObjectList` is a public subclass whose only purpose is to re-expose the protected methods. `/tmp/c19b/source/org/jfree/chart/util/ObjectList.java` lines 106–107:
   ```java
    public int indexOf(Object object) {    
        return super.indexOf(object);    
   ```
   So `new ObjectList().indexOf(null)` lands in the patched method with null as the argument at that frame.

2. **Through `CategoryPlot`.** The plot stores its axes in that same list type — `/tmp/c19b/source/org/jfree/chart/plot/CategoryPlot.java` line 287 `private ObjectList domainAxes;` and line 299 `private ObjectList rangeAxes;` — and the two methods the bug's failing tests exercise are one-liners over it. Line 697–698:
   ```java
    public int getDomainAxisIndex(CategoryAxis axis) {
        return this.domainAxes.indexOf(axis);
   ```
   and line 972–973:
   ```java
    public int getRangeAxisIndex(ValueAxis axis) {
        int result = this.rangeAxes.indexOf(axis);
   ```
   So `plot.getDomainAxisIndex(null)` reaches `AbstractObjectList.indexOf` **with null still as the argument** — nothing replaces or swallows it in between.

### The receiver-state fact that decides the plot path

Whether the plot path exposes the patch depends on whether the axis list contains a null slot. `AbstractObjectList.set`, lines 120–131 of the same file, grows the array and sets `this.size = Math.max(this.size, index + 1);` — it does not fill the skipped slots. So installing an axis at index 4 of a list that holds one axis leaves indices 1, 2 and 3 holding null and `size` at 5. `CategoryPlot.setDomainAxis(int, CategoryAxis, boolean)` at line 659 calls exactly that: `this.domainAxes.set(index, axis);`.

Consequence, on the patched build:

- Plot whose axis list has **no** null slot → `getDomainAxisIndex(null)` throws `IllegalArgumentException`. That is what a null-rejection oracle expects, so the oracle is satisfied and stays quiet. The patch looks correct here.
- Plot whose axis list **has** a null slot (an axis installed at a skipped index) → the loop finds the hole first and returns its index. No exception. A null-rejection oracle fires.

Both builds reach `indexOf` with null. The difference is entirely the container's contents.

---

## 2. Inventory — draw 05's null-family and list-family probes and where each call goes

Draw 05 kept 16 relations (trace steps [162]–[177], lines 11818–11878) and accepted 8 harnesses. Line numbers below are in
`runs-archive/runs/invdiv_20260808_203424/05_patch1-Chart-19-Arja-plausible_o/trace.md`.

### Relations (the channel replayed directly on the patched build)

| relation | trace line | reaches `AbstractObjectList.indexOf`? | argument at that frame | list state at the call | patched-build behaviour | replay result |
|---|---|---|---|---|---|---|
| `objectlist-indexof-null-finds-stored-null` | 1057 | yes, via `ObjectList.indexOf` | **null** | `list.set(n, null)` first — null present | returns `n`, unchanged by the patch | quiet [162] |
| `objectlist-null-indexof-finds-stored-null` | 1230, repaired at 1310 | yes | **null** | `list.set(n, null)` first — null present | returns `n`, unchanged | quiet [168] |
| `objectlist-indexof-null-present` | 1617 | yes | **null** | `list.set(i, null)` first — null present | returns `i`, unchanged | quiet [172] |
| **`objectlist-indexof-null-absent-is-minus-one`** | **1624** | **yes** | **null** | **`n` non-null objects only, `n` drawn from `data.consumeInt(0, 20)` — no null stored** | **throws `IllegalArgumentException`** | **quiet [173] — see below** |
| `objectlist-set-get-indexof-agree` | (screened, step [5]) | yes | a stored non-null `String` | populated | returns the index | quiet [163] |
| `objectlist-set-get-indexof-roundtrip` | 1237 | yes | non-null `target` | populated | returns the index | quiet [169] |
| `objectlist-set-get-indexof-same-reference` | 1631 | yes | non-null `target` | populated | returns the index | quiet [174] |
| `objectlist-indexof-is-read-only` | 1244 | yes | `probePresent ? target : new Object()` — never null | populated | returns index or `-1` | quiet [164] |
| `objectlist-indexof-readonly-state` | 1638 | yes | `refs[p]` or `new Object()` — never null | populated | returns index or `-1` | quiet [175] |
| `objectlist-clone-preserves-lookup` | 1652 | yes | non-null `target` | populated | returns the index | quiet [177] |
| `categoryplot-range-axis-reference-indexing` | 1092 | yes, via `getRangeAxisIndex` | three non-null `NumberAxis` instances | axes at 0 and at a drawn `slot` in 1..3 | returns indices | quiet [167] |
| `categoryplot-rangeaxis-roundtrip` | 1645 | yes, via `getRangeAxisIndex` | non-null `axis` | axis installed at drawn `i` in 1..10 | returns `i` | quiet [176] |
| `categoryaxis-constructor-defaults`, `categoryaxis-constructor-default-margins`, `numberaxis-setters-reflected-by-getters`, `numberaxis-constructor-default-flags` | 1251, 1258 and screened peers | no — constructor/accessor checks | — | — | — | quiet [165][166][170][171] |

**No relation in draw 05 asserts that a null argument must be rejected.** The null-rejection contract appears only in the harness channel, below.

### The one relation that had the distinguishing input, verbatim

`objectlist-indexof-null-absent-is-minus-one`, trace line 1624, with `\n` expanded:

```java
int n = data.consumeInt(0, 20);
org.jfree.chart.util.ObjectList list = new org.jfree.chart.util.ObjectList();
int result;
try {
  for (int k = 0; k < n; k++) {
    list.set(k, new Object());
  }
  result = list.indexOf(null);
} catch (Exception e) { return; }
if (result != -1) {
  throw new RuntimeException("relation objectlist-indexof-null-absent-is-minus-one violated: got " + result);
}
```

Its own stated contract, quoted from line 1624: *"Since null is a permitted stored value, looking up null when no null has been stored means the object is not in the list, so the result must be -1."*

Trace it against the patched build: `n` is drawn from 0 to 20, the loop stores only `new Object()`, so no null is ever stored (and at `n == 0` the list is empty — the witness's own shape). `list.indexOf(null)` therefore falls through the loop, hits the patch's added line, and throws `IllegalArgumentException`. That throw happens **inside** the `try`, and `IllegalArgumentException` is a `RuntimeException`, which is an `Exception`, so `catch (Exception e) { return; }` catches it and returns. The comparison on the next line never runs. Trace step [173], line 11873: `**quiet (did not fire)**`.

The input arrived. The check could not say so.

### Why the check is shaped that way — it was told to be

The relation-synthesis prompt mandates it. Draw-05 trace line 1037, verbatim:

> "It MUST wrap the API calls in try/catch and RETURN (skip) on ANY caught exception — an exception is a rejection, never a violation."

and line 1038:

> "STRUCTURE RULE (checked mechanically; violations are rejected): the try/catch goes ONLY around the API calls that build/compute the values."

So every relation the pipeline writes puts the call under test inside a catch-all that returns. For a patch whose defect is *adding* a throw on a valid input, that rule makes the relation channel structurally unable to report the defect.

### A second, independent swallow one layer down

Even a relation that let the exception escape would not be counted. The screening and replay wrapper in `src/java/relations/relation_screen.py` counts a firing only when the escaping exception carries a `"violated"` message or is a `FuzzerSecurityIssue*`. Line 293 of that file, verbatim:

```
        '            // other runtime exceptions: input rejection the body',
```

followed on line 294 by `'            // failed to fence — not a violation, not counted',`. A raw `IllegalArgumentException` from the method under test is therefore discarded twice: once by the relation body, once by the wrapper.

The existing self-swallow lint does not cover this. `violation_swallowed` in `src/java/parsing/java_source.py` lines 506–537 only fires when the **alarm throw** is lexically inside a swallowing try — here the alarm throw is correctly outside; what is swallowed is the call under test. The docstring says so: *"Return a human-readable reason if ANY alarm throw in `source` is lexically inside a try whose broad catch ... absorbs it"*. The `boolean-swallow` lint explicitly exempts this shape as *"the mandated catch-and-return input-rejection shape"* (`java_source.py` line 566).

### Harness oracles (the null-rejection family)

`result.jsonl` lists eight accepted triggers, all of the same family — for example:

```
"[oracle:domain-axis-null] semantic mismatch: plot.getDomainAxisIndex(null) should throw IllegalArgumentException"
"[oracle:lifted-domain-axis-null] semantic mismatch: plot.getDomainAxisIndex(null) expected IllegalArgumentException but no exception was thrown"
"[oracle:lifted-rangeaxis-null] semantic mismatch: plot.getRangeAxisIndex(null) expected IllegalArgumentException"
```

Every one of them reaches `AbstractObjectList.indexOf` with null at that frame. Every one of them was quiet on the patched build, and for a legitimate reason: the receiver had no null slot, so the patched build genuinely threw the expected exception.

The accepted harness `attempt_002` (its source is the step-[49] output, trace lines 2421–2578; the null oracle at line 2465) is the clean example:

```java
        CategoryPlot plot = new CategoryPlot(null, domainAxis1, rangeAxis1, null);
        ...
        plot.setDomainAxis(1, domainAxis2);
        ...
        IllegalArgumentException seedNullException = null;
        try {
            plot.getDomainAxisIndex(null);
        } catch (IllegalArgumentException e) {
            seedNullException = e;
        }
        if (seedNullException == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-domain-axis-null] semantic mismatch: expected IllegalArgumentException for null axis");
        }
```

The constructor puts `domainAxis1` at slot 0 (`CategoryPlot.java` line 470, `this.domainAxes.set(0, domainAxis);`) and the literal `setDomainAxis(1, domainAxis2)` puts the second at slot 1. Contents are `[domainAxis1, domainAxis2]`, size 2, no holes. On the patched build `indexOf(null)` throws, `seedNullException` is non-null, the oracle passes. Correct behaviour, correctly reported quiet.

Two other accepted harnesses do draw a fuzzed install index — but **after** the null probe has already run:

- lines 4041–4054: the null probe `plot.getDomainAxisIndex(null)` runs first, and only then `int slot = data.consumeInt(0, 3); plot.setDomainAxis(slot, fuzzAxis);` — the hole-creating mutation. No null probe follows it.
- lines 5611–5644: `[oracle:lifted-domain-axis-null]` (the probe at 5624–5631) runs against the literal `plot.setDomainAxis(1, domainAxis2);` at line 5611, and only afterwards comes `int extraIndex = data.consumeInt(2, 6);` at line 5637 and `plot.setDomainAxis(extraIndex, fuzzAxis);` at 5644. Again no null probe after.

So on the plot path, draw 05 built the exposing state and probed null — in the wrong order.

### Two draw-05 firings that did happen, and why they did not convict

For completeness: two harnesses did fire on the patched build (steps [93] and [96], lines 6321–6340) — `state-get-set` and `domain-axis-installation-postcondition`. Both were dismissed downstream, `domain-axis-installation-postcondition` with the citation *"A correct implementation can fire this check because the harness has already executed `plot.setDomainAxis(1, domainAxis2);` earlier on the same `plot`"* (line 11813). Neither touches the null case. `result.jsonl` records `"harnesses_crashed": 0, "crashed_on_patch": false`.

---

## 3. The draw-04 comparison — what made its probes fire

Draw 04's conviction did **not** come from a harness. The last step of its trace, step [241] at line 17044, reads:

```
**FLAGGED overfitting**
- detail: {'site': 'relation-replay-conviction', 'reason': '1 verifier-kept relation conviction(s)',
   'kept': ['categoryplot_getRangeAxisIndex_null_rejected_independent_of_']}
```

That relation, draw-04 trace line 1092, with `\n` expanded:

```java
boolean violated = false;
String actual = "";
try {
  org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot(
      null,
      new org.jfree.chart.axis.CategoryAxis(data.consumeAsciiString(5)),
      new org.jfree.chart.axis.NumberAxis(data.consumeAsciiString(5)),
      null);
  int c = data.consumeInt(1, 4);
  for (int j = 0; j < c; j++) {
    int idx = data.consumeInt(1, 6);
    plot.setRangeAxis(idx, new org.jfree.chart.axis.NumberAxis(data.consumeAsciiString(4)));
  }
  try {
    plot.getRangeAxisIndex(null);
    violated = true;
    actual = "completed normally";
  } catch (IllegalArgumentException ok) {
  } catch (Throwable t) {
    violated = true;
    actual = "wrong exception class " + t.getClass().getName();
  }
} catch (Exception e) {
  return;
}
if (violated) {
  throw new RuntimeException("relation categoryplot_getRangeAxisIndex_null_rejected_independent_of_axis_state violated: " + actual);
}
```

Its replay result, draw-04 trace step [227] lines 16137–16139:

```
**FIRED [fuzzed] — [relfire] relation categoryplot_getRangeAxisIndex_null_rejected_independent_of_axis_state violated: completed normally __consumed=     ||3|5||1||1|**
- note: fires on 10074/20000 fuzzed inputs on the patched build (silent on the trigger literals)
```

Two things make it fire, and both are absent from draw 05:

1. **The install index is drawn from the fuzzer, in a loop.** `int idx = data.consumeInt(1, 6);` inside `for (int j = 0; j < c; j++)`. Installing at index 5 of a list holding one axis leaves indices 1–4 null (`AbstractObjectList.set` line 130, `this.size = Math.max(this.size, index + 1);`). Then `getRangeAxisIndex(null)` → `rangeAxes.indexOf(null)` finds the first hole and returns it, so the call completes normally instead of throwing. Roughly half the drawn inputs produce such a hole, which matches the measured 10074/20000.
2. **The exception handling is targeted, not a catch-all-return.** The inner `try` names `catch (IllegalArgumentException ok) {}` as the expected outcome and treats "completed normally" and "wrong exception class" as violations. The outer `catch (Exception e) { return; }` wraps only the setup. Nothing that the call under test can do is silently discarded.

That relation is the *rejection-independence companion* mandated by the standing strategy that also appears verbatim in draw 05's own prompt, at draw-05 trace line 1028:

> "STANDING STRATEGY — REJECTION INDEPENDENCE (receiver-state variation): for ANY relation that asserts a rejection/error contract ... also emit a companion relation that holds the probe input FIXED and instead varies the RECEIVER / CONTAINER state around it ... a patch that makes the rejection conditional on the container's contents or size is exactly the overfit this companion catches."

Draw 04 instantiated it. **Draw 06 instantiated it too** — its relation `categoryplot-getrangeaxisindex-null-throws` (draw-06 trace line 1092) has the same two ingredients, `int slot = data.consumeInt(1, 4);` before the probe and `catch (IllegalArgumentException ok) {}` as the expected outcome, and it fired on the patched build as `[oracle:categoryplot-getrangeaxisindex-null-throws] ... getRangeAxisIndex(null) completed normally` (draw-06 step [70], line 6572). Draw 05 did not instantiate it for any contract.

The two harness firings named in the earlier read behave the same way:

- `domain-axis-index-null-independent` (draw-04 trace lines 1769–1805) builds `int installs = data.consumeInt(1, 4);` and inside the loop `int idx = data.consumeInt(1, 6); plot.setDomainAxis(idx, axis);` — holes — and only then probes `plot.getDomainAxisIndex(null)` with the same targeted `catch (IllegalArgumentException ok) {}`. Fired: *"[oracle:domain-axis-index-null-independent] semantic mismatch: completed normally"* (line 4714).
- `state-get-after-set` (line 3033) is a `list.get`/`list.set` identity check on `ObjectList`, unrelated to null. It fired at index 0 and its draw-05 twin `state-get-set` fired too and was dismissed. It is not the distinguisher.

**Draw 06, the third draw, confirms the mechanism from the harness side.** Its harness (trace lines 5495–5526) does the mutation first and the probe second:

```java
        int slot = data.consumeInt(1, 4);
        plot.setDomainAxis(slot, domainAxis2);
        ...
        try {
            plot.getDomainAxisIndex(null);
        } catch (IllegalArgumentException e) {
            threwExpected = true;
        } catch (Throwable t) {
            wrong = t;
        }
```

With `slot` at 3 or 4, index 1 or indices 1–2 are null holes. Fired on patched (step [76], line 6599): *"[oracle:generalized-null-throws] semantic mismatch: plot.getDomainAxisIndex(null) expected IllegalArgumentException"*.

### The result that settles the fork

All three draws invented the exactly-right direct-list relation, and all three were silenced the same way:

| draw | relation name | trace line | shape | patched replay |
|---|---|---|---|---|
| 04 | `objectlist_indexOf_null_absent_is_minus_one` | 1064 | `observed = list.indexOf(null);` inside `try { ... } catch (Exception e) { return; }` | quiet (step [229]) |
| 04 | `objectlist-indexof-absent-null-is-minus-one` | 1250 | same | quiet (step [233]) |
| 05 | `objectlist-indexof-null-absent-is-minus-one` | 1624 | same | quiet (step [173]) |
| 06 | `objectlist-indexof-null-absent` | 1064 | same | quiet |
| 06 | `objectlist-null-index-matches-first-null-slot` | 1230 | same, with the expected index computed from the list's own contents | quiet |

The exactly-right input was produced in every draw and reported in none. The catch/miss difference between draws lives entirely in the *other* channel — the plot-level rejection probe, and specifically in whether the receiver state was varied before the probe ran.

---

## 4. Fork verdict

**FORK-ORACLE**, on the decisive leg, with a second leg that is neither routing nor input-value reach.

Evidence chain for the primary leg, each link checkable:

1. The patch adds a throw that only takes effect when `indexOf` is called with null **and** no null is stored (patch hunk; `AbstractObjectList.java` lines 158–166 show the throw sits after the loop).
2. `ObjectList.indexOf` is public and delegates straight to it (`ObjectList.java` lines 106–107).
3. Draw 05's relation `objectlist-indexof-null-absent-is-minus-one` (trace line 1624) builds a list of `n` non-null objects, `n` drawn from 0 to 20, and calls `list.indexOf(null)` — null argument, null absent, empty list included.
4. That relation was kept (step [42], *"kept — silent on buggy (tripwire)"*) and replayed directly on the patched build (step [173]).
5. On the patched build the call throws `IllegalArgumentException`; the relation's own `catch (Exception e) { return; }` catches it and returns; the comparison never executes. Result: *"quiet (did not fire)"*.
6. The shape is mandated by the synthesis prompt (line 1037: *"an exception is a rejection, never a violation"*) and reinforced by the counting wrapper, which does not count a raw runtime exception as a violation (`relation_screen.py` lines 293–294).

So the answer to (a) is yes — the probe reached `AbstractObjectList.indexOf`, both directly and via `CategoryPlot`. The answer to (b) is yes — null was the argument at that very frame, in the exact list state that distinguishes the patch. FORK-ROUTING is eliminated by (a); FORK-REACH is eliminated by (b).

**The second leg, stated separately so it is not lost.** Draw 05's plot-level null oracles also reached `indexOf` with null at the frame, but always against an axis list with no null slot, where the patched build is genuinely correct. Draws 04 and 06 probed null against a list with a hole, created by a fuzz-drawn install index, and fired. That is not an argument-value gap — it is a *receiver-state and probe-ordering* gap: which container state the probe runs against, and whether the rejection probe is re-run after each state mutation. If a fourth fork label is wanted, this is it; call it **FORK-STATE**. It is emphatically not FORK-REACH as defined, because the null value itself was never the missing ingredient.

Worth stating plainly: for this leg, a boundary-value seeder that supplies `null` more often would change nothing. Draw 05 already supplied null at the patched method's own argument, in both channels — four of its sixteen relations pass null to `indexOf`, and eight accepted harness oracles pass null through `getDomainAxisIndex` / `getRangeAxisIndex`.

---

## 5. What this implies for the build order

**Demote the boundary-lattice seeder for this miss class.** The witness study's coverage claim (a lattice atom would produce Chart-19's distinguishing input) is correct and this read does not contradict it — but on this leg the pipeline *already produced the atom*. Seeding is not the constraint here, so a seeder built now would be validated against a bug it cannot help. Caveat: this read covers one bug across three draws; the seeder's case still rests on the other nine lattice bugs, which this read says nothing about.

**Build first: make an unexpected exception from the call under test reportable.** The relation synthesis contract currently says an exception is always a rejection. That is right when the input might be invalid, and wrong when the relation has declared its input valid by construction — which the prompt already requires it to do (line 1035: *"input: how to build a VALID input by construction ... so a violation is a real defect, not a rejection of bad input"*). The narrow change: when the API call is on the patch-changed class or method and the relation's declared input is valid by construction, an exception from that call becomes a violation reported with its cause attached, instead of a silent return. Both layers need it — the relation body (which currently returns) and the counting wrapper (`relation_screen.py` lines 288–296, which currently does not count a raw runtime exception).

Two things make this cheaper and safer than it sounds:
- The pipeline already knows how to write the unwrapped shape. Draw-05 step [18] shows the compile-repair returning a check with no try/catch at all (trace lines 1322–1332) — it compiled, screened and ran.
- The existing attribution machinery is the precision guard. A newly-reportable exception that the buggy build also throws is caught by the `[buggy-replay fact]` / `fires-on-both-confirmed` path already visible in these traces. The change must still be validated both-signs on the frozen guard fixtures like every mechanism before it, because it does widen what counts as a finding.

**Build second: probe rejection contracts after every state mutation, not once.** The rejection-independence standing strategy (line 1028) exists and works — it is what convicted draw 04. Two gaps in how it lands:
- It is stated for *relations*, and draw 05 produced no rejection relation at all, so it had nothing to attach to. The lifted-test null oracles that live in harnesses got no equivalent treatment.
- Even where a harness varies the container, ordering can defeat it: draw 05 built the exposing state at trace lines 4053 and 5638, both times *after* the null probe. A rule that re-runs the rejection probe after each state-changing call would have converted draw 05 on the plot path alone.

The `constant_receiver_state` lint already detects the narrower half of this and demotes rather than drops (`relation_screen.py` lines 565–575); its message in draw 05, line 1554, names the exact missing shape: *"A patch that only misbehaves for a different shape (a gap/hole, an empty or larger container, an element installed at a different index) is never exercised."* It fired, the relation was rewritten to draw its slot from `data` — and the rewritten relation still had no null probe in it. The lint watches structure, not whether the rejection contract is probed in that structure.

**Diff-targeted routing is not needed for this bug.** Routing was fine: twelve of the sixteen kept relations and all eight accepted harnesses reached the patched method; the four that did not are constructor/accessor checks on `CategoryAxis` and `NumberAxis`.

---

## 6. What this read could not determine

- **Not executed.** Every claim about patched-build behaviour is derived from the patch text plus the source I read, not from a run. The definitive confirmation is small — narrow the catch in `objectlist-indexof-null-absent-is-minus-one` and replay it on the patched build — but it is a run, out of scope here. Until then, the chain is a very strong inference rather than an observation.
- **Whether the fix is precision-safe.** I can show the exception is swallowed. I cannot show, from desk evidence, how many *correct* patches would start producing findings once unexpected exceptions become reportable. Only the guard fixtures answer that.
- **Whether draw 05's plot-path ordering was chance.** Three draws is too few to say whether "mutate, then probe" versus "probe, then mutate" is a coin flip or is driven by something in the prompt. Draws 04 and 06 both got the good order; draw 05 got the bad one twice in two harnesses, which hints at within-draw correlation, but n=3 supports no claim.
- **Generalisation beyond this bug.** This is one patch whose defect is an *added* throw. How common that patch shape is across the overfit pool is unmeasured here — and it bounds how much the reportable-exception fix buys. That is countable from the patch files without any run, and worth counting before the build.
- **Lang-63 and Lang-41.** Untouched by this read, as by the witness study.

---

## 7. Pointers

- Traces: `runs-archive/runs/invdiv_20260808_203424/{04,05,06}_patch1-Chart-19-Arja-plausible_o/trace.md` and `result.jsonl`.
- Chart-19 buggy source used for every call-graph claim: `defects4j checkout -p Chart -v 19b`; files `source/org/jfree/chart/util/AbstractObjectList.java`, `source/org/jfree/chart/util/ObjectList.java`, `source/org/jfree/chart/plot/CategoryPlot.java`.
- Pipeline code cited: `src/java/relations/relation_screen.py` (screen lints ~505–575; counting wrapper 260–300), `src/java/parsing/java_source.py` (`violation_swallowed` 506–537; `boolean-swallow` notes 546–570).
- Background: `docs/witness-study-2026-08-08.md` §0 and §6; `docs/plan.md` items 8.30, 8.32, 8.33, 8.34.
- Firewall: no witness text from `/home/code/drr/DiffTGen` appears in any prompt, corpus, fixture or run artifact. This document is analysis, the same permitted class as the witness study.
