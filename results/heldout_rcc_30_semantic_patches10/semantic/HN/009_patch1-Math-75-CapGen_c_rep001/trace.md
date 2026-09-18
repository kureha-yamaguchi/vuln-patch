# Pipeline trace — Math-75

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/stat/Frequency.java
+++ /src/main/java/org/apache/commons/math/stat/Frequency.java
@@ -300,7 +300,7 @@ public class Frequency implements Serializable {
      */
     @Deprecated
     public double getPct(Object v) {
-        return getCumPct((Comparable<?>) v);
+        return getPct((Comparable<?>) v);
     }
 
     /**
```

107 sequential steps — 33 LLM calls, 74 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **rule synthesis (LLM)** — proposes candidate RELATIONS — invariants / metamorphic properties a correct implementation must satisfy — from the documented contract
- **screen-fuzz-buggy** — compiles each candidate rule and fuzzes it many times on the BUGGY build; the output is checked/violated/fire-ratio
- **screen** — the keep/drop decision for each rule (direction-confirmed, selective, silent, or dropped by a lint/compile/cap)
- **screening-survivors** — the final set of rules kept — these are passed to replay / harness generation
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.stat.FrequencyTest', test_method='testPcts', source_ (~1,711 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.stat.FrequencyTest', test_method='testPcts', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/Math_75_buggy/src/test/java/org/apache/commons/math/stat/FrequencyTest.java', method_source='    public void testPcts() {\\n        f.addValue(oneL);\\n        f.addValue(twoL);\\n        f.addValue(oneI);\\n        f.addValue(twoI);\\n        f.addValue(threeL);\\n        f.addValue(threeL);\\n        f.addValue(3);\\n        f.addValue(threeI);\\n        assertEquals(\"one pct\",0.25,f.getPct(1),tolerance);\\n        assertEquals(\"two pct\",0.25,f.getPct(Long.valueOf(2)),tolerance);\\n        assertEquals(\"three pct\",0.5,f.getPct(threeL),tolerance);\\n        // MATH-329\\n        assertEquals(\"three (Object) pct\",0.5,f.getPct((Object) (Integer.valueOf(3))),tolerance);\\n        assertEquals(\"five pct\",0,f.getPct(5),tolerance);\\n        assertEquals(\"foo pct\",0,f.getPct(\"foo\"),tolerance);\\n        assertEquals(\"one cum pct\",0.25,f.getCumPct(1),tolerance);\\n        assertEquals(\"two cum pct\",0.50,f.getCumPct(Long.valueOf(2)),tolerance);\\n        assertEquals(\"Integer argument\",0.50,f.getCumPct(Integer.valueOf(2)),tolerance);\\n        assertEquals(\"three cum pct\",1.0,f.getCumPct(threeL),tolerance);\\n        assertEquals(\"five cum pct\",1.0,f.getCumPct(5),tolerance);\\n        assertEquals(\"zero cum pct\",0.0,f.getCumPct(0),tolerance);\\n        assertEquals(\"foo cum pct\",0,f.getCumPct(\"foo\"),tolerance);\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math.stat.FrequencyTest::testPcts",
    "failure_message": "--- org.apache.commons.math.stat.FrequencyTest::testPcts\njunit.framework.AssertionFailedError: three (Object) pct expected:<0.5> but was:<1.0>",
    "support_chars": 355
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~16,795 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/stat/Frequency.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/stat/Frequency.java\n+++ /src/main/java/org/apache/commons/math/stat/Frequency.java\n@@ -300,7 +300,7 @@ public class Frequency implements Serializable {\n      */\n     @Deprecated\n     public double getPct(Object v) {\n-        return getCumPct((Comparable<?>) v);\n+        return getPct((Comparable<?>) v);\n     }\n \n     /**",
  "functions": [
    {
      "func_name": "getPct",
      "func_signature": "public double getPct(Object v)",
      "func_source": "    public double getPct(Object v) {\n        return getCumPct((Comparable<?>) v);\n    }",
      "func_class": "Frequency",
      "func_class_fq": "org.apache.commons.math.stat.Frequency",
      "func_param_types": [
        "Object"
      ],
      "fi_name": "[org.apache.commons.math.stat.Frequency].getPct(Object)",
      "overload_types": [
        [
          "Object"
        ],
        [
          "Comparable<?>"
        ],
        [
          "int"
        ],
        [
          "long"
        ],
        [
          "char"
        ]
      ],
      "xrefs": [
        "public void testPcts() {\n        f.addValue(oneL);\n        f.addValue(twoL);\n        f.addValue(oneI);\n        f.addValue(twoI);\n        f.addValue(threeL);\n        f.addValue(threeL);\n        f.addValue(3);\n        f.addValue(threeI);\n        assertEquals(\"one pct\",0.25,f.getPct(1),tolerance);\n        assertEquals(\"two pct\",0.25,f.getPct(Long.valueOf(2)),tolerance);\n        assertEquals(\"three pct\",0.5,f.getPct(threeL),tolerance);\n        // MATH-329\n        assertEquals(\"three (Object) pct\",0.5,f.getPct((Object) (Integer.valueOf(3))),tolerance);\n        assertEquals(\"five pct\",0,f.getPct(5),tolerance);\n        assertEquals(\"foo pct\",0,f.getPct(\"foo\"),tolerance);\n        assertEquals(\"one cum pct\",0.25,f.getCumPct(1),tolerance);\n        assertEquals(\"two cum pct\",0.50,f.getCumPct(Long.valueOf(2)),tolerance);\n        assertEquals(\"Integer argument\",0.50,f.getCumPct(Integer.valueOf(2)),tolerance);\n        assertEquals(\"three cum pct\",1.0,f.getCumPct(threeL),tolerance);\n        assertEquals(\"five cum pct\",1.0,f.getCumPct(5),tolerance);\n        assertEquals(\"zero cum pct\",0.0,f.getCumPct(0),tolerance);\n        assertEquals(\"foo cum pct\",0,f.getCumPct(\"foo\"),tolerance);\n    }"
      ],
      "xref_names": [
        "[org.apache.commons.math.stat.FrequencyTest].testPcts()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.math.stat.Frequency].getPct(Object)",
          "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].getSumFreq()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getSumFreq()",
          "[java.util.TreeMap<Comparable<?>,Long>].values()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getSumFreq()",
          "[org.apache.commons.math.stat.Frequency].iterator()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getSumFreq()",
          "[org.apache.commons.math.stat.Frequency].hasNext()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getSumFreq()",
          "[org.apache.commons.math.stat.Frequency].next()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getSumFreq()",
          "[org.apache.commons.math.stat.Frequency].longValue()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].getSumFreq()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[Integer].longValue()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].getCumFreq(org.apache.commons.math.stat.Frequency)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[java.util.TreeMap<Comparable<?>,Long>].comparator()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency.NaturalComparator].<init>()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[java.util.TreeMap<Comparable<?>,Long>].get(Comparable<?>)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].longValue()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[java.util.TreeMap<Comparable<?>,Long>].firstKey()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[java.util.TreeMap<Comparable<?>,Long>].lastKey()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].getSumFreq()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].valuesIterator()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[java.util.Iterator<Comparable<?>>].hasNext()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[java.util.Iterator<Comparable<?>>].next()"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)"
        ],
        [
          "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
          "[org.apache.commons.math.stat.Frequency].getCount(org.apache.commons.math.stat.Frequency)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math.stat.Frequency].getPct(Object)": 0,
        "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)": 1,
        "[org.apache.commons.math.stat.Frequency].getSumFreq()": 2,
        "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)": 2,
        "[java.util.TreeMap<Comparable<?>,Long>].values()": 3,
        "[org.apache.commons.math.stat.Frequency].iterator()": 3,
        "[org.apache.commons.math.stat.Frequency].hasNext()": 3,
        "[org.apache.commons.math.stat.Frequency].next()": 3,
        "[org.apache.commons.math.stat.Frequency].longValue()": 3,
        "[Integer].longValue()": 3,
        "[org.apache.commons.math.stat.Frequency].getCumFreq(org.apache.commons.math.stat.Frequency)": 3,
        "[java.util.TreeMap<Comparable<?>,Long>].comparator()": 3,
        "[org.apache.commons.math.stat.Frequency.NaturalComparator].<init>()": 3,
        "[java.util.TreeMap<Comparable<?>,Long>].get(Comparable<?>)": 3,
        "[java.util.TreeMap<Comparable<?>,Long>].firstKey()": 3,
        "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)": 3,
        "[java.util.TreeMap<Comparable<?>,Long>].lastKey()": 3,
        "[org.apache.commons.math.stat.Frequency].valuesIterator()": 3,
        "[java.util.Iterator<Comparable<?>>].hasNext()": 3,
        "[java.util.Iterator<Comparable<?>>].next()": 3,
        "[org.apache.commons.math.stat.Frequency].getCount(org.apache.commons.math.stat.Frequency)": 3
      },
      "reachable": [
        "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)",
        "[org.apache.commons.math.stat.Frequency].getSumFreq()",
        "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
        "[java.util.TreeMap<Comparable<?>,Long>].values()",
        "[org.apache.commons.math.stat.Frequency].iterator()",
        "[org.apache.commons.math.stat.Frequency].hasNext()",
        "[org.apache.commons.math.stat.Frequency].next()",
        "[org.apache.commons.math.stat.Frequency].longValue()",
        "[Integer].longValue()",
        "[org.apache.commons.math.stat.Frequency].getCumFreq(org.apache.commons.math.stat.Frequency)",
        "[java.util.TreeMap<Comparable<?>,Long>].comparator()",
        "[org.apache.commons.math.stat.Frequency.NaturalComparator].<init>()",
        "[java.util.TreeMap<Comparable<?>,Long>].get(Comparable<?>)",
        "[java.util.TreeMap<Comparable<?>,Long>].firstKey()",
        "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)",
        "[java.util.TreeMap<Comparable<?>,Long>].lastKey()",
        "[org.apache.commons.math.stat.Frequency].valuesIterator()",
        "[java.util.Iterator<Comparable<?>>].hasNext()",
        "[java.util.Iterator<Comparable<?>>].next()",
        "[org.apache.commons.math.stat.Frequency].getCount(org.apache.commons.math.stat.Frequency)",
        "[org.apache.commons.math.stat.Frequency].getCumPct(Object)"
      ],
      "related_callees": [
        {
          "name": "getCumPct",
          "source_file": "Frequency.java",
          "signature": "public double getCumPct(Object v)",
          "source": "    public double getCumPct(Object v) {\n        return getCumPct((Comparable<?>) v);\n\n    }",
          "is_abstract": false,
          "impls": [
            [
              "Frequency.java",
              "    public double getCumPct(Object v) {\n        return getCumPct((Comparable<?>) v);\n\n    }"
            ],
            [
              "Frequency.java",
              "    public double getCumPct(Comparable<?> v) {\n        final long sumFreq = getSumFreq();\n        if (sumFreq == 0) {\n            return Double.NaN;\n        }\n        return (double) getCumFreq(v) / (double) sumFreq;\n    }"
            ],
            [
              "Frequency.java",
              "    public double getCumPct(int v) {\n        return getCumPct(Long.valueOf(v));\n    }"
            ],
            [
              "Frequency.java",
              "    public double getCumPct(long v) {\n        return getCumPct(Long.valueOf(v));\n    }"
            ]
          ]
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.stat",
  "root_cause_reachable": [
    "Frequency.getCumPct",
    "Frequency.getSumFreq",
    "Frequency.getCumFreq",
    "Frequency.iterator",
    "Frequency.hasNext",
    "Frequency.next",
    "Frequency.longValue",
    "NaturalComparator.<init>",
    "NaturalComparator.compare",
    "Frequency.valuesIterator",
    "Frequency.getCount"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.io.Serializable;",
    "import java.text.NumberFormat;",
    "import java.util.Iterator;",
    "import java.util.Comparator;",
    "import java.util.TreeMap;",
    "import org.apache.commons.math.MathRuntimeException;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math.stat.Frequency].getPct(Object)",
      "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].getSumFreq()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getSumFreq()",
      "[java.util.TreeMap<Comparable<?>,Long>].values()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getSumFreq()",
      "[org.apache.commons.math.stat.Frequency].iterator()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getSumFreq()",
      "[org.apache.commons.math.stat.Frequency].hasNext()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getSumFreq()",
      "[org.apache.commons.math.stat.Frequency].next()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getSumFreq()",
      "[org.apache.commons.math.stat.Frequency].longValue()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].getSumFreq()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[Integer].longValue()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].getCumFreq(org.apache.commons.math.stat.Frequency)"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[java.util.TreeMap<Comparable<?>,Long>].comparator()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency.NaturalComparator].<init>()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[java.util.TreeMap<Comparable<?>,Long>].get(Comparable<?>)"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].longValue()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[java.util.TreeMap<Comparable<?>,Long>].firstKey()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[java.util.TreeMap<Comparable<?>,Long>].lastKey()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].valuesIterator()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[java.util.Iterator<Comparable<?>>].hasNext()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[java.util.Iterator<Comparable<?>>].next()"
    ],
    [
      "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)",
      "[org.apache.commons.math.stat.Frequency].getCount(org.apache.commons.math.stat.Frequency)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math.stat.Frequency].getPct(Object)": 0,
    "[org.apache.commons.math.stat.Frequency].getCumPct(Comparable<?>)": 1,
    "[org.apache.commons.math.stat.Frequency].getSumFreq()": 2,
    "[org.apache.commons.math.stat.Frequency].getCumFreq(Comparable<?>)": 2,
    "[java.util.TreeMap<Comparable<?>,Long>].values()": 3,
    "[org.apache.commons.math.stat.Frequency].iterator()": 3,
    "[org.apache.commons.math.stat.Frequency].hasNext()": 3,
    "[org.apache.commons.math.stat.Frequency].next()": 3,
    "[org.apache.commons.math.stat.Frequency].longValue()": 3,
    "[Integer].longValue()": 3,
    "[org.apache.commons.math.stat.Frequency].getCumFreq(org.apache.commons.math.stat.Frequency)": 3,
    "[java.util.TreeMap<Comparable<?>,Long>].comparator()": 3,
    "[org.apache.commons.math.stat.Frequency.NaturalComparator].<init>()": 3,
    "[java.util.TreeMap<Comparable<?>,Long>].get(Comparable<?>)": 3,
    "[java.util.TreeMap<Comparable<?>,Long>].firstKey()": 3,
    "[org.apache.commons.math.stat.Frequency.NaturalComparator].compare(Comparable<?>,org.apache.commons.math.stat.Frequency)": 3,
    "[java.util.TreeMap<Comparable<?>,Long>].lastKey()": 3,
    "[org.apache.commons.math.stat.Frequency].valuesIterator()": 3,
    "[java.util.Iterator<Comparable<?>>].hasNext()": 3,
    "[java.util.Iterator<Comparable<?>>].next()": 3,
    "[org.apache.commons.math.stat.Frequency].getCount(org.apache.commons.math.stat.Frequency)": 3
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
**output:** **documented exceptions: {'Frequency#addValue': ['IllegalArgumentException'], 'Frequency#compare': ['ClassCastException', 'NullPointerException']}**
- detail: {'documented': {'Frequency#addValue': ['IllegalArgumentException'], 'Frequency#compare': ['ClassCastException', 'NullPointerException']}}

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~41,247 chars)</summary>

**[system]**
```
You are a software-verification expert. Given a patched Java method and its API, you propose RELATIONS that must hold for EVERY correct implementation — never merely for the reference or for the tested inputs. Two kinds only:
  * METAMORPHIC: two real API calls whose results must agree (or relate in a fixed way) for any valid input — e.g. parse(format(x)) == x; f(a,b) == f(b,a) when documented commutative; reduce(reduce(x)) == reduce(x).
  * INVARIANT: a property the output must satisfy for every valid input, read straight off the documented contract — e.g. a reduced fraction has gcd(numerator,denominator)==1; a returned index is within bounds.
Each relation must be SOUND: if any correct implementation could violate it, do not propose it. Avoid comparisons to a different library, and avoid boundary-fragile claims (division by zero, NaN, overflow, empty/null) unless you explicitly exclude those inputs.
```
**[user]**
```
THE BUG'S OWN FAILING TEST (most trusted — this is ground truth for the correct behaviour on its inputs). The patch under analysis MAY be an overfit; where this test pins a direction, the TEST is right and the patched code is not evidence. Read the exact call it makes and the value it expects, and make your relations agree with it:
<failing_test>
// org.apache.commons.math.stat.FrequencyTest::testPcts
    public void testPcts() {
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);
        assertEquals("one pct",0.25,f.getPct(1),tolerance);
        assertEquals("two pct",0.25,f.getPct(Long.valueOf(2)),tolerance);
        assertEquals("three pct",0.5,f.getPct(threeL),tolerance);
        // MATH-329
        assertEquals("three (Object) pct",0.5,f.getPct((Object) (Integer.valueOf(3))),tolerance);
        assertEquals("five pct",0,f.getPct(5),tolerance);
        assertEquals("foo pct",0,f.getPct("foo"),tolerance);
        assertEquals("one cum pct",0.25,f.getCumPct(1),tolerance);
        assertEquals("two cum pct",0.50,f.getCumPct(Long.valueOf(2)),tolerance);
        assertEquals("Integer argument",0.50,f.getCumPct(Integer.valueOf(2)),tolerance);
        assertEquals("three cum pct",1.0,f.getCumPct(threeL),tolerance);
        assertEquals("five cum pct",1.0,f.getCumPct(5),tolerance);
        assertEquals("zero cum pct",0.0,f.getCumPct(0),tolerance);
        assertEquals("foo cum pct",0,f.getCumPct("foo"),tolerance);
    }
// values this test asserts as CORRECT (the patched code must reproduce these): 0.25, 0.5, 0.50, 1.0, 0.0
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.apache.commons.math.stat.FrequencyTest::testPcts
// junit.framework.AssertionFailedError: three (Object) pct expected:<0.5> but was:<1.0>
// The test DEPENDS on this setup from its test class (helpers, fields, fixtures). A relation that reconstructs the test's scenario must REPLICATE this setup exactly — a relation that cannot must assert only setup-independent properties (counts, kinds, contract formulas), or it will fire on every build including correct ones:
// --- setUp() (test setup) ---
    @Override
    public void setUp() {
        f = new Frequency();
    }

// --- class fields/constants the test uses ---
private long oneL = 1;
private long twoL = 2;
private long threeL = 3;
private int oneI = 1;
private int twoI = 2;
private int threeI=3;
private double tolerance = 10E-15;
private Frequency f = null;
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addValue(Object v) / (Comparable<?> v) / (int v) / (Integer v) / (long v) / (char v)
  getCount(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getCumFreq(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getCumPct(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getPct(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getCount, getCumFreq, getCumPct, getPct, getSumFreq

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getSumFreq, hashCode, toString
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    public double getPct(Object v) {
        return getCumPct((Comparable<?>) v);
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="Frequency" role="patched">
     * Maintains a frequency distribution.
     * <p>
     * Accepts int, long, char or Comparable values.  New values added must be
     * comparable to those that have been added, otherwise the add method will
     * throw an IllegalArgumentException.</p>
     * <p>
     * Integer values (int, long, Integer, Long) are not distinguished by type --
     * i.e. <code>addValue(Long.valueOf(2)), addValue(2), addValue(2l)</code> all have
     * the same effect (similarly for arguments to <code>getCount,</code> etc.).</p>
     * <p>
     * char values are converted by <code>addValue</code> to Character instances.
     * As such, these values are not comparable to integral values, so attempts
     * …
public class Frequency implements Serializable {
     * Serializable version identifier
    private static final long serialVersionUID = -3845586908418844111L;
     * underlying collection
    private final TreeMap<Comparable<?>, Long> freqTable;
     * Default constructor.
    public Frequency() {
        freqTable = new TreeMap<Comparable<?>, Long>();
    }
     * Constructor allowing values Comparator to be specified.
     * @param comparator Comparator used to order values
    @SuppressWarnings("unchecked") public Frequency(Comparator<?> comparator) { … }
     * Return a string representation of this frequency
     * distribution.
     * @return a string representation.
    @Override public String toString() { … }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not Comparable,
     * or is not comparable with previous entries
     * @deprecated use {@link #addValue(Comparable)} instead
    @Deprecated public void addValue(Object v) { … }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not comparable with previous entries
    public void addValue(Comparable<?> v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(int v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
     * @deprecated to be removed in math 3.0
    public void addValue(Integer v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(long v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(char v) { … }
     * Clears the frequency table
    public void clear() { … }
     * Returns an Iterator over the set of values that have been added.
     * <p>
     * If added values are integral (i.e., integers, longs, Integers, or Longs),
     * they are converted to Longs when they are added, so the objects returned
     * by the Iterator will in this case be Longs.</p>
     * @return values Iterator
    public Iterator<Comparable<?>> valuesIterator() { … }
     * Returns the sum of all frequencies.
     * @return the total frequency count.
    public long getSumFreq() { … }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
     * @deprecated replaced by {@link #getCount(Comparable)} as of 2.0
    @Deprecated public long getCount(Object v) { … }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(Comparable<?> v) { … }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(int v) { … }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(long v) { … }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(char v) { … }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
     * @deprecated replaced by {@link #getPct(Comparable)} as of 2.0
    @Deprecated public double getPct(Object v) {
        return getCumPct((Comparable<?>) v);
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(Comparable<?> v) {
        final long sumFreq = getSumFreq();
        if (sumFreq == 0) {
            return Double.NaN;
        }
        return (double) getCount(v) / (double) sumFreq;
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(int v) {
        return getPct(Long.valueOf(v));
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(long v) {
        return getPct(Long.valueOf(v));
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(char v) {
        return getPct(Character.valueOf(v));
    }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
     * @deprecated replaced by {@link #getCumFreq(Comparable)} as of 2.0
    @Deprecated public long getCumFreq(Object v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
    @SuppressWarnings("unchecked") public long getCumFreq(Comparable<?> v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(int v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(long v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(char v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
     * @deprecated replaced by {@link #getCumPct(Comparable)} as of 2.0
    @Deprecated public double getCumPct(Object v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(Comparable<?> v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(int v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(long v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(char v) { … }
     * A Comparator that compares comparable objects using the
     * natural order.  Copied from Commons Collections ComparableComparator.
    private static class NaturalComparator<T extends Comparable<T>> implements Comparator<Comparable<T>>, Serializable { … }
     * {@inheritDoc}
    @Override public int hashCode() { … }
     * {@inheritDoc}
    @Override public boolean equals(Object obj) { … }
}
</class>
<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) {
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) {
        super(rootCause);
        this.pattern   = getMessage();
        this.arguments = new Object[0];
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) {
        super(rootCause);
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { … }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { … }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { … }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { … }
     * {@inheritDoc}
    @Override public String getMessage() { … }
     * {@inheritDoc}
    @Override public String getLocalizedMessage() { … }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { … }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { … }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
THE PATCH-CHANGED CLASS: Frequency. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/apache/commons/math/stat/Frequency.java
+++ /src/main/java/org/apache/commons/math/stat/Frequency.java
@@ -300,7 +300,7 @@ public class Frequency implements Serializable {
      */
     @Deprecated
     public double getPct(Object v) {
-        return getCumPct((Comparable<?>) v);
+        return getPct((Comparable<?>) v);
     }
 
     /**
</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    REMOVED: return getCumPct((Comparable<?>) v);
    ADDED  : return getPct((Comparable<?>) v);
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
Maintains a frequency distribution.
<p>
Accepts int, long, char or Comparable values.  New values added must be
comparable to those that have been added, otherwise the add method will
throw an IllegalArgumentException.</p>
<p>
Integer values (int, long, Integer, Long) are not distinguished by type --
i.e. <code>addValue(Long.valueOf(2)), addValue(2), addValue(2l)</code> all have
the same effect (similarly for arguments to <code>getCount,</code> etc.).</p>
<p>
char values are converted by <code>addValue</code> to Character instances.
As such, these values are not comparable to integral values, so attempts
to combine integral types with chars in a frequency distribution will fail.
</p>
<p>
The values are ordered using the default (natural order), unless a
<code>Comparator</code> is supplied in the constructor.</p>

@version $Revision$ $Date$
/
public class Frequency implements Serializable {

/** Serializable version identifier */
private static final long serialVersionUID = -3845586908418844111L;

/** underlying collection */
private final TreeMap<Comparable<?>, Long> freqTable;

/**
Default constructor.
/
public Frequency() {
freqTable = new TreeMap<Comparable<?>, Long>();
}

/**
Constructor allowing values Comparator to be specified.

@param comparator Comparator used to order values
/
@SuppressWarnings("unchecked")
public Frequency(Comparator<?> comparator) {
freqTable = new TreeMap<Comparable<?>, Long>((Comparator<? super Comparable<?>>) comparator);
}

/**
Return a string
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Comparator;
import java.util.TreeMap;
import org.apache.commons.math.MathRuntimeException;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    addValue, getPct, getCumPct

ANCHOR REQUIREMENT — READ FIRST. The patch changed a SPECIFIC expression in a SPECIFIC method. An overfit usually differs from a correct implementation WHERE the code was changed (the one exception: a method the FAILING TEST reads can stay wrong when the patch edited elsewhere — that is what the secondary list below is for). Your FIRST relation MUST directly constrain the OUTPUT of a changed method, exercising the exact input the change is about — the prefix / sign / boundary value / edge case named in the changed condition — and asserting only what that method's OWN documented contract guarantees there. (E.g. if the change adds a guard on inputs starting with a token, your relation must feed such inputs and assert the documented result; if the change rewrites a formula the javadoc states, assert the method's output equals that formula recomputed independently.) Do NOT fill the slots with generic round-trip or bound properties of untouched sibling methods.
If the changed expression is a NUMERIC formula, a correct fix may reassociate it (a*(b/c) vs (a*b)/c) and differ by rounding, so assert APPROXIMATE equality with a GENEROUS relative tolerance — never exact ==, or you will falsely flag a correct reassociation. Use `Math.abs(a-b) <= tol * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)))` with tol AT LEAST 1e-9; and when the computation multiplies or sums large integer/long intermediates (products near or above 2^31, or billion-scale inputs), use tol = 1e-6 — at those magnitudes the library's OWN double rounding exceeds 1e-9, so a tighter tolerance false-fires on a correct patch. A too-tight tolerance is as unsound as exact ==; the real bug you are hunting diverges by a LARGE amount (e.g. a sign flip or a value 100x off), not by rounding.
STANDING STRATEGY — DOCUMENTED FORMULAS (checked first, before anything else): scan the javadoc of the touched class's numeric getters AND of every class shown with role="test-subject" (the classes the failing test itself constructs and reads — the defect's observable surface often lives there, not in the patched class) for a stated closed-form formula ('the value equals a * b / c of the constructor parameters', 'returns x * (1 - x)', ...). If ANY such formula exists, your FIRST relation MUST be that formula: recompute it independently from the object's own parameters and compare with the generous magnitude-scaled tolerance above. A documented formula is the strongest relation class there is — it is deterministic, it holds for every correct implementation by definition, and a patch that leaves the value wrong anywhere in the domain cannot pass it. Only when no documented formula exists does the anchor requirement below decide your first relation.
STANDING STRATEGY — DOCUMENTED @THROWS (checked second): scan the touched method's javadoc (and its parent declaration's, if shown) for declared exceptions — '@throws X if/when <condition>'. For EACH such declaration whose triggering input you can construct (a null argument, a malformed string, an out-of-range value), one relation MUST construct exactly that input and assert the documented throw: call the method, and if it COMPLETES NORMALLY (or throws a different, non-subclass exception), throw the violation. MANDATORY check shape for these documented-throw relations — catching broadly and returning DEFEATS the relation (a patch whose reordered guard throws the WRONG exception class then silently skips instead of firing):
    try { obj.method(rejectedInput); violated = true; /* completed normally */ }
    catch (TheDocumentedException ok) { /* contract honoured */ }
    catch (Throwable t) { violated = true; /* wrong exception class — also a contract violation */ }
  then throw the violation OUTSIDE the try when `violated` is set, naming what actually happened (completed vs wrong class <type>). This is a tripwire: it stays silent on any build that honours the contract and fires on a patch whose added or reordered guard swallows the documented rejection — a classic overfit shape (a guard inserted BEFORE an argument check silently returns where the contract says throw). Use ONLY declared @throws with their stated conditions — never invent an undocumented rejection; a subclass of the declared exception counts as honouring the contract. ISOLATE THE ONE VIOLATION: build the input so it breaks ONLY this @throws condition and satisfies every OTHER documented precondition — when several are broken at once a correct implementation may throw a different documented exception first, making a wrong-class assertion unsound (it fires on correct code). IMPORTANT — vary the RECEIVER and the OTHER arguments across their special states while holding the rejected input fixed: an overfit often reorders the check behind a guard that only triggers for a special receiver state (a NaN/empty/zero receiver), so a plain rejected input alone misses it — assert the documented throw on an ORDINARY receiver AND on each special-state receiver the class admits (empty, extreme, identity, NaN), since a patch may return early only in the special-state case.
STANDING STRATEGY — REJECTION INDEPENDENCE (receiver-state variation): for ANY relation that asserts a rejection/error contract — asking for something absent, invalid, or out of range MUST throw — also emit a companion relation that holds the probe input FIXED and instead varies the RECEIVER / CONTAINER state around it: its contents, its size, which elements are installed / registered / present, whether it is empty or populated, freshly built or already mutated. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so the documented throw MUST fire identically across every such state; a patch that makes the rejection conditional on the container's contents or size is exactly the overfit this companion catches. Build each varied state by construction through the real public API (draw the variation from `data`) and assert the SAME documented throw on each — one extra relation per rejection contract.
    ORDERING (mandatory, and the half most checks get wrong): re-run the rejection probe AFTER EVERY state-changing call in the check body, not once on the freshly built receiver. Mutate, then probe; mutate again, then probe again — asserting the same documented outcome each time. A probe that runs before the mutations has only ever observed the state a state-conditional patch still gets right, so the check is silent on both builds no matter how well the container is fuzzed; the divergence lives in the state AFTER the container has been filled, emptied, or left with a gap. Draw WHICH slot/key/index each mutation targets, and how many mutations happen, from `data` (never from literals), so the sequence of states the probe sees varies across iterations. This is checked mechanically at screening: a rejection probe with no probe after the receiver's last state change is reported as an ordering blind spot.
STANDING STRATEGY — STRUCTURE FROM DATA (how the varied state is built): whenever a relation's property depends on the RECEIVER / CONTAINER state — a rejection contract, an index/position/lookup contract, a size/emptiness contract — the container's STRUCTURE must be drawn from `data`, not written as compile-time constants. Structure means: HOW MANY elements it holds, AT WHICH indices or keys they are installed, whether there are GAPS/HOLES between them, whether it is EMPTY or populated, whether it is freshly built or already mutated, and in what ORDER those operations happened. Concretely, the element count, every install index, every loop bound and every branch that decides whether an element is added at all must come from a `data.consume*` draw (e.g. `int n = data.consumeInt(0, 4);` then install at `data.consumeInt(0, n)`), NOT from literals. Fuzzing only the labels/names/values INSIDE a fixed structure is COSMETIC: every iteration rebuilds the same shape, so a check that looks fuzzed is really a single hard-coded scenario, and a patch that misbehaves only for a different shape — a container with a hole in it, an empty one, a larger one, an element installed at a different position — is never exercised, on either build. This is checked mechanically at screening: a receiver-state relation whose construction consumes no fuzz bytes in any index, count or loop bound is reported as a structural blind spot.
Propose up to 6 relations. COVERAGE REQUIREMENT (R2): the set must cover DISTINCT documented observables — after the documented-formula and @throws slots above, spread the remaining slots across different contract sources (a formula, a throws clause, a state/read-only guarantee, a sibling-agreement rule) rather than proposing several variants of one observable. Do NOT anchor every relation on the patched method alone: a patch to one method can leave a DOCUMENTED observable of the test-subject class wrong away from the trigger, and a set of six relations about the patched method's own edge cases will all stay quiet while that documented observable diverges. At least ONE relation must assert a documented guarantee of a test-subject class when any is shown. And when the context lists SAME-NAME OVERLOADS or a METHOD FAMILY that contains the patched method, write one agreement relation PER SIBLING PAIR (patched method, sibling) for up to two siblings — prioritising the siblings that share the most behaviour with the patched method (same output shape, documented shared semantics), NOT just any family member: a patch that changes one member while a close sibling keeps the shared behaviour breaks exactly that pairwise agreement, and an agreement relation aimed at a distant family member can hold on a patch that a close-sibling relation convicts. Each pair relation: the two members given equivalent inputs must produce the documented consistent outcome — identical results up to the documented difference between them.
DEEP-DIVE PROTOCOL — do this enumeration BEFORE proposing: (1) LIST every documented observable in scope — each stated formula, each declared @throws, each documented range/format, each documented family agreement, each read-only/state guarantee — from the patched class AND every test-subject class shown, each with the doc line that states it; (2) MARK which of these the patch text could plausibly affect, directly or through shared state; (3) SPEND your slots on the marked ones per the coverage requirement above. Relations aimed at observables no documentation states are last-resort fills, never first choices. For each relation, give:
  - name: a short slug
  - kind: metamorphic | invariant
  - contract: the ONE documented guarantee that makes it hold for every correct implementation (javadoc sentence or a visible code invariant)
  - input: how to build a VALID input by construction (magnitudes, signs, shapes) that a correct implementation MUST accept — so a violation is a real defect, not a rejection of bad input.
    From the changed condition in the patch, the documented ranges (@param/@throws), and any bounds/size accessors this class exposes, identify the input-domain BOUNDARIES this code defines, and construct each check's inputs to PROBE at and just past them — but assert only what the contract guarantees there. Probing a boundary is free; asserting an unspecified boundary value is the classic unsound oracle.
  - check: a self-contained Java snippet that builds the input from an `int`/`long`/`String` chosen with the FuzzedDataProvider `data`, computes both sides (or the property) via the REAL public API, and `throw new RuntimeException("relation <name> violated: "+...)` on disagreement.
    TWO-TIER CATCH (mandatory; checked mechanically). The two tiers treat an exception differently, because they mean different things:
      TIER 1 — SETUP (constructing the inputs and the receivers): `try { ... } catch (Exception e) { return; }`. An input or receiver that will not build is an input this relation skips — there a caught exception IS a rejection.
      TIER 2 — THE PROBE (the call(s) on the PATCH-CHANGED class named in the context above): its OWN separate try, NOT nested inside the setup try, whose catch RE-THROWS instead of returning:
        try { r = changed.method(validInput); }
        catch (Exception e) { throw new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); }
      You have already declared this input VALID BY CONSTRUCTION, so an exception raised inside the changed code on it is a RESULT the relation must report, not a rejection to skip. A blanket catch-and-return around the probe makes the check quiet on BOTH builds, so a patch that ADDS or MOVES a throw — refusing an input the contract requires it to accept — is invisible to every relation shaped that way. Setup exceptions stay rejections; probe exceptions are reportable.
      EXCEPTION TO TIER 2: a relation whose CONTRACT IS a rejection (the documented-@throws shapes above) keeps its TARGETED `catch (TheDocumentedException ok) { }` — that catch names the ONE exception the contract promises, which is evidence, not a swallow. Never a blanket catch-and-return around a probe either way. And tier 2 covers only exceptions the documentation does NOT permit: an exception the probed method DECLARES or documents (@throws — e.g. a solver's documented non-convergence exception on a hard input) is a legitimate outcome of a correct implementation — treat it exactly like a setup rejection (catch that named type and return), and reserve the tier-2 rethrow for exceptions no documentation permits.
    STRUCTURE RULE (checked mechanically; violations are rejected): the setup try/catch goes ONLY around the API calls that build/compute the values. The comparison and its `throw new RuntimeException(...) violated ...` must sit AFTER/OUTSIDE that try block — a violation thrown inside a catch-everything block that returns is caught by it and silently discarded, making the whole check dead (this is why the tier-2 try must not be nested inside the tier-1 try: its rethrow would land in the setup catch and die there). Pattern:
      T a, b;
      try { a = api(...); b = api2(...); } catch (Exception e) { return; }
      try { b = changed.method(a); } catch (Exception e) { throw new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); }
      if (!close(a, b)) throw new RuntimeException("relation <name> violated: " + a + " vs " + b);
Draw inputs ONLY with the Jazzer FuzzedDataProvider API on `data`: data.consumeInt(), data.consumeInt(lo,hi), data.consumeLong(), data.consumeBoolean(), data.consumeAsciiString(n), data.consumeRemainingAsString() — there is NO drawInt/nextInt. The check body runs INSIDE fuzzerTestOneInput, so use the real library types directly (no wrapper class, no imports block).
IMPLEMENTABILITY (checked mechanically; violations are dropped): the check may NOT define an anonymous or local SUBCLASS of a library class to reach a code path (e.g. `new AbstractIntegerDistribution(...){...}`) — a hand-built stand-in is forbidden and the relation will be discarded. If the behaviour you want lives on an abstract base, reach it through a REAL concrete library subclass that already exists (e.g. use `new UniformIntegerDistribution(min, max)` to get an integer distribution over a chosen support, rather than subclassing the abstract base yourself). If no real subclass can construct the input you need, choose a different relation.
Return ONLY a JSON array of objects with keys name, kind, contract, input, check. No prose outside the JSON.
```

</details>
<details open><summary>▸ Output (~11,982 chars)</summary>

```
[
  {
    "name": "pct_object_matches_count_over_sum",
    "kind": "invariant",
    "contract": "getPct returns \"the percentage of values that are equal to v (as a proportion between 0 and 1)\" and returns Double.NaN only if no values have been added; therefore for any non-empty Frequency, getPct(x) must equal getCount(x) / getSumFreq().",
    "input": "Build a non-empty Frequency by adding integral values only, using a mix of int/long/Integer so the documented integral-type normalization is exercised. Query with an Integer passed through the deprecated Object overload; this is valid because Integer is Comparable and integral arguments are accepted.",
    "check": "Frequency f = new Frequency();\nint target = data.consumeInt(-5, 5);\nint copiesTargetInt = data.consumeInt(1, 4);\nint copiesTargetLong = data.consumeInt(0, 4);\nint other = target;\nwhile (other == target) {\n  other = data.consumeInt(-5, 5);\n}\nint copiesOther = data.consumeInt(0, 4);\nfor (int i = 0; i < copiesTargetInt; i++) {\n  try { f.addValue(target); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: unexpected \" + e.getClass().getName() + \" while adding valid int value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < copiesTargetLong; i++) {\n  try { f.addValue((long) target); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: unexpected \" + e.getClass().getName() + \" while adding valid long value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < copiesOther; i++) {\n  try { f.addValue(Integer.valueOf(other)); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: unexpected \" + e.getClass().getName() + \" while adding valid Integer value: \" + e.getMessage()); }\n}\nlong count;\nlong sum;\ndouble actual;\ntry { count = f.getCount(target); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: unexpected \" + e.getClass().getName() + \" from getCount on valid input: \" + e.getMessage()); }\ntry { sum = f.getSumFreq(); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: unexpected \" + e.getClass().getName() + \" from getSumFreq on valid receiver: \" + e.getMessage()); }\ntry { actual = f.getPct((Object) Integer.valueOf(target)); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage()); }\ndouble expected = (double) count / (double) sum;\ndouble tol = 1e-9;\nif (!(Math.abs(actual - expected) <= tol * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) {\n  throw new RuntimeException(\"relation pct_object_matches_count_over_sum violated: actual=\" + actual + \" expected=\" + expected + \" count=\" + count + \" sum=\" + sum);\n}"
  },
  {
    "name": "pct_object_agrees_with_pct_comparable",
    "kind": "metamorphic",
    "contract": "The deprecated getPct(Object) is documented as replaced by getPct(Comparable); for a valid Comparable argument, both overloads describe the same percentage-of-equals result and must agree.",
    "input": "Build a Frequency from integral values only. Query with an Integer value, once through the Object overload and once through the Comparable overload. Integer is a valid Comparable and integral values are explicitly accepted.",
    "check": "Frequency f = new Frequency();\nint a = data.consumeInt(-5, 5);\nint b = a;\nwhile (b == a) {\n  b = data.consumeInt(-5, 5);\n}\nint nA1 = data.consumeInt(0, 4);\nint nA2 = data.consumeInt(0, 4);\nint nB = data.consumeInt(0, 4);\nif (nA1 + nA2 + nB == 0) {\n  nA1 = 1;\n}\nfor (int i = 0; i < nA1; i++) {\n  try { f.addValue(a); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_comparable violated: unexpected \" + e.getClass().getName() + \" while adding valid int value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < nA2; i++) {\n  try { f.addValue((long) a); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_comparable violated: unexpected \" + e.getClass().getName() + \" while adding valid long value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < nB; i++) {\n  try { f.addValue(b); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_comparable violated: unexpected \" + e.getClass().getName() + \" while adding valid int value: \" + e.getMessage()); }\n}\nInteger q = Integer.valueOf(a);\ndouble viaObject;\ndouble viaComparable;\ntry { viaObject = f.getPct((Object) q); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_comparable violated: unexpected \" + e.getClass().getName() + \" from getPct(Object): \" + e.getMessage()); }\ntry { viaComparable = f.getPct((Comparable<?>) q); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_comparable violated: unexpected \" + e.getClass().getName() + \" from getPct(Comparable): \" + e.getMessage()); }\ndouble tol = 1e-9;\nif (!(Math.abs(viaObject - viaComparable) <= tol * Math.max(1.0, Math.max(Math.abs(viaObject), Math.abs(viaComparable))))) {\n  throw new RuntimeException(\"relation pct_object_agrees_with_pct_comparable violated: object=\" + viaObject + \" comparable=\" + viaComparable);\n}"
  },
  {
    "name": "pct_object_agrees_with_pct_long_for_integral_value",
    "kind": "metamorphic",
    "contract": "Class javadoc states that Integer/Long/int/long values are not distinguished by type, similarly for arguments to getCount etc.; therefore the percentage queried for the same integral value through equivalent overloads must agree.",
    "input": "Build a non-empty Frequency using only integral values. Query the same numeric value through getPct(Object) with an Integer and through getPct(long) with the numerically equal long.",
    "check": "Frequency f = new Frequency();\nint target = data.consumeInt(-6, 6);\nint other = target;\nwhile (other == target) {\n  other = data.consumeInt(-6, 6);\n}\nint n1 = data.consumeInt(1, 4);\nint n2 = data.consumeInt(0, 4);\nint n3 = data.consumeInt(0, 4);\nfor (int i = 0; i < n1; i++) {\n  try { f.addValue(Integer.valueOf(target)); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_long_for_integral_value violated: unexpected \" + e.getClass().getName() + \" while adding valid Integer value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < n2; i++) {\n  try { f.addValue((long) target); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_long_for_integral_value violated: unexpected \" + e.getClass().getName() + \" while adding valid long value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < n3; i++) {\n  try { f.addValue(other); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_long_for_integral_value violated: unexpected \" + e.getClass().getName() + \" while adding valid int value: \" + e.getMessage()); }\n}\ndouble viaObject;\ndouble viaLong;\ntry { viaObject = f.getPct((Object) Integer.valueOf(target)); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_long_for_integral_value violated: unexpected \" + e.getClass().getName() + \" from getPct(Object): \" + e.getMessage()); }\ntry { viaLong = f.getPct((long) target); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_agrees_with_pct_long_for_integral_value violated: unexpected \" + e.getClass().getName() + \" from getPct(long): \" + e.getMessage()); }\ndouble tol = 1e-9;\nif (!(Math.abs(viaObject - viaLong) <= tol * Math.max(1.0, Math.max(Math.abs(viaObject), Math.abs(viaLong))))) {\n  throw new RuntimeException(\"relation pct_object_agrees_with_pct_long_for_integral_value violated: object=\" + viaObject + \" long=\" + viaLong);\n}"
  },
  {
    "name": "pct_object_with_integral_query_is_bounded",
    "kind": "invariant",
    "contract": "getPct returns a percentage \"as a proportion between 0 and 1\" for non-empty distributions.",
    "input": "Build a non-empty Frequency using accepted integral values only. Query any integral value through the Object overload with an Integer, whether present or absent; this is a valid accepted Comparable input.",
    "check": "Frequency f = new Frequency();\nint present = data.consumeInt(-4, 4);\nint absent = present;\nwhile (absent == present) {\n  absent = data.consumeInt(-4, 4);\n}\nint nPresent = data.consumeInt(1, 5);\nint nOther = data.consumeInt(0, 5);\nfor (int i = 0; i < nPresent; i++) {\n  try { f.addValue((long) present); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_with_integral_query_is_bounded violated: unexpected \" + e.getClass().getName() + \" while adding valid long value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < nOther; i++) {\n  try { f.addValue(absent + 1); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_with_integral_query_is_bounded violated: unexpected \" + e.getClass().getName() + \" while adding valid int value: \" + e.getMessage()); }\n}\nInteger q = data.consumeBoolean() ? Integer.valueOf(present) : Integer.valueOf(absent);\ndouble pct;\ntry { pct = f.getPct((Object) q); } catch (Exception e) { throw new RuntimeException(\"relation pct_object_with_integral_query_is_bounded violated: unexpected \" + e.getClass().getName() + \" from getPct(Object): \" + e.getMessage()); }\nif (Double.isNaN(pct) || pct < 0.0 || pct > 1.0) {\n  throw new RuntimeException(\"relation pct_object_with_integral_query_is_bounded violated: pct=\" + pct);\n}"
  },
  {
    "name": "getPct_object_is_read_only_on_sumfreq",
    "kind": "invariant",
    "contract": "getPct is documented as a getter returning a percentage; it must not mutate the frequency distribution, so observable state such as getSumFreq must remain unchanged across the call.",
    "input": "Build any valid Frequency from integral values only, then call getPct(Object) with a valid Integer query. Since the query is valid by construction, any change in total frequency indicates hidden mutation.",
    "check": "Frequency f = new Frequency();\nint x = data.consumeInt(-3, 3);\nint y = x;\nwhile (y == x) {\n  y = data.consumeInt(-3, 3);\n}\nint nx = data.consumeInt(1, 4);\nint ny = data.consumeInt(0, 4);\nfor (int i = 0; i < nx; i++) {\n  try { f.addValue(x); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_sumfreq violated: unexpected \" + e.getClass().getName() + \" while adding valid int value: \" + e.getMessage()); }\n}\nfor (int i = 0; i < ny; i++) {\n  try { f.addValue((long) y); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_sumfreq violated: unexpected \" + e.getClass().getName() + \" while adding valid long value: \" + e.getMessage()); }\n}\nlong before;\nlong after;\ntry { before = f.getSumFreq(); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_sumfreq violated: unexpected \" + e.getClass().getName() + \" from getSumFreq before query: \" + e.getMessage()); }\ntry { f.getPct((Object) Integer.valueOf(data.consumeBoolean() ? x : y)); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_sumfreq violated: unexpected \" + e.getClass().getName() + \" from getPct(Object): \" + e.getMessage()); }\ntry { after = f.getSumFreq(); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_sumfreq violated: unexpected \" + e.getClass().getName() + \" from getSumFreq after query: \" + e.getMessage()); }\nif (before != after) {\n  throw new RuntimeException(\"relation getPct_object_is_read_only_on_sumfreq violated: before=\" + before + \" after=\" + after);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `pct_object_matches_count_over_sum`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [6] ⚙️ screen · `pct_object_matches_count_over_sum`
**output:** **dropped**
- reason: no stats line (harness crashed the JVM or never ran)

---
## [7] ⚙️ screen · `pct_object_agrees_with_pct_comparable`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [8] ⚙️ screen · `pct_object_agrees_with_pct_comparable`
**output:** **dropped**
- reason: no stats line (harness crashed the JVM or never ran)

---
## [9] ⚙️ screen · `pct_object_agrees_with_pct_long_for_integral_value`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [10] ⚙️ screen · `pct_object_agrees_with_pct_long_for_integral_value`
**output:** **dropped**
- reason: no stats line (harness crashed the JVM or never ran)

---
## [11] ⚙️ screen · `pct_object_with_integral_query_is_bounded`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [12] ⚙️ screen · `pct_object_with_integral_query_is_bounded`
**output:** **dropped**
- reason: no stats line (harness crashed the JVM or never ran)

---
## [13] ⚙️ screen · `getPct_object_is_read_only_on_sumfreq`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [14] ⚙️ screen · `getPct_object_is_read_only_on_sumfreq`
**output:** **dropped**
- reason: no stats line (harness crashed the JVM or never ran)

---
## [15] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [],
  "count": 0
}
```

---
## [16] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,530 chars)</summary>

```
[
  {
    "name": "getPctObject_matches_count_over_sum_for_integer_object",
    "kind": "invariant",
    "contract": "For getPct(Object): \"Returns the percentage of values that are equal to v (as a proportion between 0 and 1).\" Together with getSumFreq and getCount, this means on any non-empty distribution getPct(v) = getCount(v) / getSumFreq. The class javadoc also states that integer values are not distinguished by type for arguments to getPct etc.",
    "input": "Build a non-empty Frequency using only integral values drawn from data, so all inputs are valid and comparable. Probe the changed Object overload with an Integer object argument, which is the exact cross-type case exercised by the failing test.",
    "check": "Frequency f; int n; int probe; long count; long sum; double actual; double expected; try { f = new Frequency(); n = data.consumeInt(1, 8); for (int i = 0; i < n; i++) { int v = data.consumeInt(-5, 5); switch (data.consumeInt(0, 3)) { case 0: f.addValue(v); break; case 1: f.addValue((long) v); break; case 2: f.addValue(Integer.valueOf(v)); break; default: f.addValue(Long.valueOf(v)); break; } } probe = data.consumeInt(-5, 5); count = f.getCount((Object) Integer.valueOf(probe)); sum = f.getSumFreq(); expected = (double) count / (double) sum; } catch (Exception e) { return; } try { actual = f.getPct((Object) Integer.valueOf(probe)); } catch (Exception e) { throw new RuntimeException(\"relation getPctObject_matches_count_over_sum_for_integer_object violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage()); } if (!(Math.abs(actual - expected) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) { throw new RuntimeException(\"relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=\" + actual + \" expected=\" + expected + \" count=\" + count + \" sum=\" + sum); }"
  },
  {
    "name": "getPct_object_and_comparable_agree_on_same_integer_value",
    "kind": "metamorphic",
    "contract": "The deprecated getPct(Object) is documented as replaced by getPct(Comparable), and the class javadoc says integer values are not distinguished by type for arguments to getPct etc. Therefore equivalent Integer inputs through these two overloads must yield the same percentage.",
    "input": "Build any non-empty Frequency using only integral values from data. Query the same integer value once through getPct(Object) with Integer.valueOf(x) and once through getPct(Comparable) with the same Integer object.",
    "check": "Frequency f; int n; Integer probe; double a; double b; try { f = new Frequency(); n = data.consumeInt(1, 8); for (int i = 0; i < n; i++) { int v = data.consumeInt(-6, 6); if (data.consumeBoolean()) { f.addValue(v); } else { f.addValue((long) v); } } probe = Integer.valueOf(data.consumeInt(-6, 6)); } catch (Exception e) { return; } try { a = f.getPct((Object) probe); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_and_comparable_agree_on_same_integer_value violated: unexpected \" + e.getClass().getName() + \" from getPct(Object): \" + e.getMessage()); } try { b = f.getPct((Comparable<?>) probe); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_and_comparable_agree_on_same_integer_value violated: unexpected \" + e.getClass().getName() + \" from getPct(Comparable): \" + e.getMessage()); } if (!(Math.abs(a - b) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) { throw new RuntimeException(\"relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=\" + a + \" comparable=\" + b); }"
  },
  {
    "name": "getPct_object_and_int_overloads_agree",
    "kind": "metamorphic",
    "contract": "The class javadoc says integer values are not distinguished by type and this applies similarly to arguments to getPct. Therefore getPct(Object) on Integer.valueOf(x) and getPct(int) on x must agree.",
    "input": "Build a non-empty Frequency from integral values only, using data to vary both count and inserted numeric types. Probe one int value x from data through the Object and int overloads.",
    "check": "Frequency f; int n; int probe; double a; double b; try { f = new Frequency(); n = data.consumeInt(1, 10); for (int i = 0; i < n; i++) { int v = data.consumeInt(-4, 4); switch (data.consumeInt(0, 1)) { case 0: f.addValue(v); break; default: f.addValue((long) v); break; } } probe = data.consumeInt(-4, 4); } catch (Exception e) { return; } try { a = f.getPct((Object) Integer.valueOf(probe)); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_and_int_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from getPct(Object): \" + e.getMessage()); } try { b = f.getPct(probe); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_and_int_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from getPct(int): \" + e.getMessage()); } if (!(Math.abs(a - b) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) { throw new RuntimeException(\"relation getPct_object_and_int_overloads_agree violated: object=\" + a + \" int=\" + b + \" probe=\" + probe); }"
  },
  {
    "name": "getCumPct_matches_cumfreq_over_sum_for_integer_object",
    "kind": "invariant",
    "contract": "For getCumPct(Object): \"Returns the cumulative percentage of values less than or equal to v (as a proportion between 0 and 1).\" Together with getCumFreq and getSumFreq, this means on any non-empty distribution getCumPct(v) = getCumFreq(v) / getSumFreq.",
    "input": "Build a non-empty Frequency using only integral values so all values remain mutually comparable. Query an Integer object value from data.",
    "check": "Frequency f; int n; int probe; long cum; long sum; double actual; double expected; try { f = new Frequency(); n = data.consumeInt(1, 8); for (int i = 0; i < n; i++) { int v = data.consumeInt(-5, 5); if (data.consumeBoolean()) { f.addValue(v); } else { f.addValue((long) v); } } probe = data.consumeInt(-5, 5); cum = f.getCumFreq((Object) Integer.valueOf(probe)); sum = f.getSumFreq(); expected = (double) cum / (double) sum; } catch (Exception e) { return; } try { actual = f.getCumPct((Object) Integer.valueOf(probe)); } catch (Exception e) { throw new RuntimeException(\"relation getCumPct_matches_cumfreq_over_sum_for_integer_object violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage()); } if (!(Math.abs(actual - expected) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) { throw new RuntimeException(\"relation getCumPct_matches_cumfreq_over_sum_for_integer_object violated: actual=\" + actual + \" expected=\" + expected + \" cum=\" + cum + \" sum=\" + sum); }"
  },
  {
    "name": "getPct_object_is_read_only_on_frequency_state",
    "kind": "invariant",
    "contract": "getPct is a lookup method: it returns a percentage for a value and does not document any mutation. Therefore calling getPct(Object) on a valid input must not change observable state such as getSumFreq, hashCode, or toString.",
    "input": "Build any non-empty Frequency with valid integral values from data, then call getPct(Object) on an Integer object value from the same domain.",
    "check": "Frequency f; int n; Integer probe; long sumBefore; long sumAfter; int hashBefore; int hashAfter; String strBefore; String strAfter; double pct; try { f = new Frequency(); n = data.consumeInt(1, 8); for (int i = 0; i < n; i++) { int v = data.consumeInt(-3, 3); if (data.consumeBoolean()) { f.addValue(v); } else { f.addValue((long) v); } } probe = Integer.valueOf(data.consumeInt(-3, 3)); sumBefore = f.getSumFreq(); hashBefore = f.hashCode(); strBefore = f.toString(); } catch (Exception e) { return; } try { pct = f.getPct((Object) probe); } catch (Exception e) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_frequency_state violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage()); } sumAfter = f.getSumFreq(); hashAfter = f.hashCode(); strAfter = f.toString(); if (sumBefore != sumAfter || hashBefore != hashAfter || !strBefore.equals(strAfter)) { throw new RuntimeException(\"relation getPct_object_is_read_only_on_frequency_state violated: state changed after getPct(Object), pct=\" + pct + \", sum \" + sumBefore + \"->\" + sumAfter + \", hash \" + hashBefore + \"->\" + hashAfter + \", toString changed=\" + (!strBefore.equals(strAfter))); }"
  }
]
```

</details>

---
## [17] ⚙️ screen · `getPctObject_matches_count_over_sum_for_integer_object`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~720 chars)</summary>

- reason: the probe `f.getCount(...)` on the patch-changed class `Frequency` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Frequency` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [18] ⚙️ screen-fuzz-buggy · `getPctObject_matches_count_over_sum_for_integer_object`
**output:** 
```json
{
  "checked": 20000,
  "violated": 4761,
  "ratio": 0.2381
}
```

---
## [19] ⚙️ screen · `getPct_object_and_comparable_agree_on_same_integer_value`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~720 chars)</summary>

- reason: the probe `f.addValue(...)` on the patch-changed class `Frequency` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Frequency` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [20] ⚙️ screen-fuzz-buggy · `getPct_object_and_comparable_agree_on_same_integer_value`
**output:** 
```json
{
  "checked": 20000,
  "violated": 4820,
  "ratio": 0.241
}
```

---
## [21] ⚙️ screen · `getPct_object_and_int_overloads_agree`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~720 chars)</summary>

- reason: the probe `f.addValue(...)` on the patch-changed class `Frequency` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Frequency` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [22] ⚙️ screen-fuzz-buggy · `getPct_object_and_int_overloads_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 4390,
  "ratio": 0.2195
}
```

---
## [23] ⚙️ screen · `getCumPct_matches_cumfreq_over_sum_for_integer_object`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~722 chars)</summary>

- reason: the probe `f.getCumFreq(...)` on the patch-changed class `Frequency` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Frequency` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [24] ⚙️ screen-fuzz-buggy · `getCumPct_matches_cumfreq_over_sum_for_integer_object`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [25] ⚙️ screen · `getPct_object_is_read_only_on_frequency_state`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~722 chars)</summary>

- reason: the probe `f.getSumFreq(...)` on the patch-changed class `Frequency` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Frequency` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [26] ⚙️ screen-fuzz-buggy · `getPct_object_is_read_only_on_frequency_state`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [27] ⚙️ screen · `getPctObject_matches_count_over_sum_for_integer_object`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [28] ⚙️ screen · `getPct_object_and_comparable_agree_on_same_integer_value`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [29] ⚙️ screen · `getPct_object_and_int_overloads_agree`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [30] ⚙️ screen · `getCumPct_matches_cumfreq_over_sum_for_integer_object`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [31] ⚙️ screen · `getPct_object_is_read_only_on_frequency_state`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [32] ⚙️ reference-widening · `enumerate`
**output:** **1 observable(s) to widen onto**
- reason: the kept checks probe ['sumfreq'] among the patched class's no-argument readers; 1 of those have no admitted reference and 1 fit under the per-leg cap of 3 (the leg already holds []); 7 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['addvalue', 'count', 'message', 'pct', 'frequency', 'cumfreq'])
<details><summary>▸ detail (~1,555 chars)</summary>

- detail: {'targets': ['sumfreq'], 'already_admitted': [], 'cap': 3, 'checks_read': 5, 'excluded': [{'key': 'addvalue', 'why': "the patched class declares `addValue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'count', 'why': "the patched class declares `getCount` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'message', 'why': 'the patched class does not declare `getMessage()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'pct', 'why': "the patched class declares `getPct` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'frequency', 'why': 'the patched class declares `Frequency()` as a constructor or a void method — it reports no observable value'}, {'key': 'cumfreq', 'why': "the patched class declares `getCumFreq` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'cumpct', 'why': "the patched class declares `getCumPct` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}], 'excluded_count': 7}

</details>

---
## [33] ⚙️ reference-widening · `getSumFreq`
**output:** **reference REQUESTED for observable `sumfreq`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [34] ⚙️ reference-impl · `getSumFreq`
**output:** **widening target requested**
- detail: {'candidates': ['getSumFreq'], 'attempting': 'getSumFreq'}

---
## [35] ⚙️ reference-impl · `getSumFreq`
**output:** **screening surface resolved**
- reason: 2 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['hashCode', 'toString'], 'patch_touched_exempted': [], 'screened_siblings': ['hashCode', 'toString'], 'declaring_types': ['Frequency']}

---
## [36] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,388 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
**[user]**
```
Implement `getSumFreq` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="Frequency" role="patched">
     * Maintains a frequency distribution.
     * <p>
     * Accepts int, long, char or Comparable values.  New values added must be
     * comparable to those that have been added, otherwise the add method will
     * throw an IllegalArgumentException.</p>
     * <p>
     * Integer values (int, long, Integer, Long) are not distinguished by type --
     * i.e. <code>addValue(Long.valueOf(2)), addValue(2), addValue(2l)</code> all have
     * the same effect (similarly for arguments to <code>getCount,</code> etc.).</p>
     * <p>
     * char values are converted by <code>addValue</code> to Character instances.
     * As such, these values are not comparable to integral values, so attempts
     * …
public class Frequency implements Serializable {
     * Serializable version identifier
    private static final long serialVersionUID = -3845586908418844111L;
     * underlying collection
    private final TreeMap<Comparable<?>, Long> freqTable;
     * Default constructor.
    public Frequency() { /* body withheld */ }
     * Constructor allowing values Comparator to be specified.
     * @param comparator Comparator used to order values
    @SuppressWarnings("unchecked") public Frequency(Comparator<?> comparator) { /* body withheld */ }
     * Return a string representation of this frequency
     * distribution.
     * @return a string representation.
    @Override public String toString() { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not Comparable,
     * or is not comparable with previous entries
     * @deprecated use { /* body withheld */ } instead
    @Deprecated public void addValue(Object v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not comparable with previous entries
    public void addValue(Comparable<?> v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(int v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
     * @deprecated to be removed in math 3.0
    public void addValue(Integer v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(long v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(char v) { /* body withheld */ }
     * Clears the frequency table
    public void clear() { /* body withheld */ }
     * Returns an Iterator over the set of values that have been added.
     * <p>
     * If added values are integral (i.e., integers, longs, Integers, or Longs),
     * they are converted to Longs when they are added, so the objects returned
     * by the Iterator will in this case be Longs.</p>
     * @return values Iterator
    public Iterator<Comparable<?>> valuesIterator() { /* body withheld */ }
     * Returns the sum of all frequencies.
     * @return the total frequency count.
    public long getSumFreq() { /* body withheld */ }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public long getCount(Object v) { /* body withheld */ }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(Comparable<?> v) { /* body withheld */ }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(int v) { /* body withheld */ }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(long v) { /* body withheld */ }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(char v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public double getPct(Object v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(Comparable<?> v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(int v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(long v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(char v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public long getCumFreq(Object v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
    @SuppressWarnings("unchecked") public long getCumFreq(Comparable<?> v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(int v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(long v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(char v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public double getCumPct(Object v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(Comparable<?> v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(int v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(long v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(char v) { /* body withheld */ }
     * A Comparator that compares comparable objects using the
     * natural order.  Copied from Commons Collections ComparableComparator.
    private static class NaturalComparator<T extends Comparable<T>> implements Comparator<Comparable<T>>, Serializable { … }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object obj) { /* body withheld */ }
}
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { /* body withheld */ }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { /* body withheld */ }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { /* body withheld */ }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { /* body withheld */ }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testPcts() {
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);
        assertEquals("one pct",0.25,f.getPct(1),tolerance);
        assertEquals("two pct",0.25,f.getPct(Long.valueOf(2)),tolerance);
        assertEquals("three pct",0.5,f.getPct(threeL),tolerance);
        // MATH-329
        assertEquals("three (Object) pct",0.5,f.getPct((Object) (Integer.valueOf(3))),tolerance);
        assertEquals("five pct",0,f.getPct(5),tolerance);
        assertEquals("foo pct",0,f.getPct("foo"),tolerance);
        assertEquals("one cum pct",0.25,f.getCumPct(1),tolerance);
        assertEquals("two cum pct",0.50,f.getCumPct(Long.valueOf(2)),tolerance);
        assertEquals("Integer argument",0.50,f.getCumPct(Integer.valueOf(2)),tolerance);
        assertEquals("three cum pct",1.0,f.getCumPct(threeL),tolerance);
        assertEquals("five cum pct",1.0,f.getCumPct(5),tolerance);
        assertEquals("zero cum pct",0.0,f.getCumPct(0),tolerance);
        assertEquals("foo cum pct",0,f.getCumPct("foo"),tolerance);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="Frequency" role="patched">
     * Maintains a frequency distribution.
     * <p>
     * Accepts int, long, char or Comparable values.  New values added must be
     * comparable to those that have been added, otherwise the add method will
     * throw an IllegalArgumentException.</p>
     * <p>
     * Integer values (int, long, Integer, Long) are not distinguished by type --
     * i.e. <code>addValue(Long.valueOf(2)), addValue(2), addValue(2l)</code> all have
     * the same effect (similarly for arguments to <code>getCount,</code> etc.).</p>
     * <p>
     * char values are converted by <code>addValue</code> to Character instances.
     * As such, these values are not comparable to integral values, so attempts
     * …
public class Frequency implements Serializable {
     * Serializable version identifier
    private static final long serialVersionUID = -3845586908418844111L;
     * underlying collection
    private final TreeMap<Comparable<?>, Long> freqTable;
     * Default constructor.
    public Frequency() { /* body withheld */ }
     * Constructor allowing values Comparator to be specified.
     * @param comparator Comparator used to order values
    @SuppressWarnings("unchecked") public Frequency(Comparator<?> comparator) { /* body withheld */ }
     * Return a string representation of this frequency
     * distribution.
     * @return a string representation.
    @Override public String toString() { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not Comparable,
     * or is not comparable with previous entries
     * @deprecated use { /* body withheld */ } instead
    @Deprecated public void addValue(Object v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not comparable with previous entries
    public void addValue(Comparable<?> v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(int v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
     * @deprecated to be removed in math 3.0
    public void addValue(Integer v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(long v) { /* body withheld */ }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(char v) { /* body withheld */ }
     * Clears the frequency table
    public void clear() { /* body withheld */ }
     * Returns an Iterator over the set of values that have been added.
     * <p>
     * If added values are integral (i.e., integers, longs, Integers, or Longs),
     * they are converted to Longs when they are added, so the objects returned
     * by the Iterator will in this case be Longs.</p>
     * @return values Iterator
    public Iterator<Comparable<?>> valuesIterator() { /* body withheld */ }
     * Returns the sum of all frequencies.
     * @return the total frequency count.
    public long getSumFreq() { /* body withheld */ }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public long getCount(Object v) { /* body withheld */ }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(Comparable<?> v) { /* body withheld */ }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(int v) { /* body withheld */ }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(long v) { /* body withheld */ }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(char v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public double getPct(Object v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(Comparable<?> v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(int v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(long v) { /* body withheld */ }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(char v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public long getCumFreq(Object v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
    @SuppressWarnings("unchecked") public long getCumFreq(Comparable<?> v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(int v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(long v) { /* body withheld */ }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(char v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
     * @deprecated replaced by { /* body withheld */ } as of 2.0
    @Deprecated public double getCumPct(Object v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(Comparable<?> v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(int v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(long v) { /* body withheld */ }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(char v) { /* body withheld */ }
     * A Comparator that compares comparable objects using the
     * natural order.  Copied from Commons Collections ComparableComparator.
    private static class NaturalComparator<T extends Comparable<T>> implements Comparator<Comparable<T>>, Serializable { … }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object obj) { /* body withheld */ }
}
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { /* body withheld */ }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { /* body withheld */ }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { /* body withheld */ }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { /* body withheld */ }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `hashCode`, `toString`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~3,788 chars)</summary>

```
// compute(TreeMap<Comparable<?>, Long>) : getSumFreq, hashCode, toString
import java.text.NumberFormat;
import java.util.Map;
import java.util.TreeMap;

public class ReferenceImpl {
    public static long compute_getSumFreq(final TreeMap<Comparable<?>, Long> freqTable) {
        long result = 0L;
        if (freqTable == null) {
            return 0L;
        }
        for (Long value : freqTable.values()) {
            if (value != null) {
                result += value.longValue();
            }
        }
        return result;
    }

    public static int compute_hashCode(final TreeMap<Comparable<?>, Long> freqTable) {
        return 31 + (freqTable == null ? 0 : freqTable.hashCode());
    }

    public static String compute_toString(final TreeMap<Comparable<?>, Long> freqTable) {
        StringBuilder outBuffer = new StringBuilder();
        NumberFormat nf = NumberFormat.getPercentInstance();

        outBuffer.append("Value \tFreq. \tPct. \tCum Pct. \n");
        if (freqTable == null) {
            return outBuffer.toString();
        }

        for (Comparable<?> value : freqTable.keySet()) {
            outBuffer.append(value);
            outBuffer.append('\t');
            outBuffer.append(getCount(freqTable, value));
            outBuffer.append('\t');
            outBuffer.append(nf.format(getPct(freqTable, value)));
            outBuffer.append('\t');
            outBuffer.append(nf.format(getCumPct(freqTable, value)));
            outBuffer.append('\n');
        }

        return outBuffer.toString();
    }

    private static long getCount(final TreeMap<Comparable<?>, Long> freqTable, final Comparable<?> v) {
        if (freqTable == null) {
            return 0L;
        }
        try {
            Long count = freqTable.get(v);
            return count == null ? 0L : count.longValue();
        } catch (ClassCastException ex) {
            return 0L;
        }
    }

    private static double getPct(final TreeMap<Comparable<?>, Long> freqTable, final Comparable<?> v) {
        long sumFreq = compute_getSumFreq(freqTable);
        if (sumFreq == 0L) {
            return Double.NaN;
        }
        return (double) getCount(freqTable, v) / (double) sumFreq;
    }

    @SuppressWarnings("unchecked")
    private static long getCumFreq(final TreeMap<Comparable<?>, Long> freqTable, final Comparable<?> v) {
        if (freqTable == null || freqTable.isEmpty()) {
            return 0L;
        }

        final long total = compute_getSumFreq(freqTable);
        Comparable<?> first = freqTable.firstKey();
        Comparable<?> last = freqTable.lastKey();

        try {
            if (((Comparable<Object>) v).compareTo(first) < 0) {
                return 0L;
            }
            if (((Comparable<Object>) v).compareTo(last) >= 0) {
                return total;
            }
        } catch (ClassCastException ex) {
            return 0L;
        }

        long result = 0L;
        for (Map.Entry<Comparable<?>, Long> entry : freqTable.entrySet()) {
            try {
                if (((Comparable<Object>) entry.getKey()).compareTo(v) <= 0) {
                    Long count = entry.getValue();
                    if (count != null) {
                        result += count.longValue();
                    }
                } else {
                    break;
                }
            } catch (ClassCastException ex) {
                return 0L;
            }
        }
        return result;
    }

    private static double getCumPct(final TreeMap<Comparable<?>, Long> freqTable, final Comparable<?> v) {
        long sumFreq = compute_getSumFreq(freqTable);
        if (sumFreq == 0L) {
            return Double.NaN;
        }
        return (double) getCumFreq(freqTable, v) / (double) sumFreq;
    }
}
```

</details>

---
## [37] ⚙️ reference-impl · `getSumFreq`
**output:** **reference generated**
- detail: {'chars': 3788}

---
## [38] ⚙️ reference-impl · `getSumFreq`
**output:** **signature declared**
- detail: {'signature': 'TreeMap<Comparable<?>, Long>'}

---
## [39] ⚙️ reference-impl · `getSumFreq`
**output:** **reference observables matched**
- detail: {'matched': {'getSumFreq': 'getSumFreq', 'hashCode': 'hashCode', 'toString': 'toString'}, 'declared_only': []}

---
## [40] ⚙️ reference-impl · `getSumFreq`
**output:** **reference too thin to screen — DISCARDED**
- reason: 2 shared sibling observable(s) ['hashCode', 'toString']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [41] ⚙️ reference-widening · `getSumFreq`
**output:** **widening REJECTED for observable `sumfreq`**
- reason: reference too thin to screen — DISCARDED — 2 shared sibling observable(s) ['hashCode', 'toString']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [42] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~29,531 chars)</summary>

**[system]**
```
You are an expert Java security engineer who writes Jazzer fuzzing harnesses. Return a single compilable .java file — no markdown fences, no prose outside the file.
```
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.stat` (`package org.apache.commons.math.stat;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_75_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/stat/Frequency.java
+++ /src/main/java/org/apache/commons/math/stat/Frequency.java
@@ -300,7 +300,7 @@ public class Frequency implements Serializable {
      */
     @Deprecated
     public double getPct(Object v) {
-        return getCumPct((Comparable<?>) v);
+        return getPct((Comparable<?>) v);
     }
 
     /**
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Comparator;
import java.util.TreeMap;
import org.apache.commons.math.MathRuntimeException;
</source_imports>

Function `getPct`:
<signature>
public double getPct(Object v)
</signature>
<code>
    public double getPct(Object v) {
        return getCumPct((Comparable<?>) v);
    }
</code>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param comparator Comparator used to order values
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addValue(Object v) / (Comparable<?> v) / (int v) / (Integer v) / (long v) / (char v)
  getCount(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getCumFreq(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getCumPct(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getPct(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getCount, getCumFreq, getCumPct, getPct, getSumFreq

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getSumFreq, hashCode, toString

THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does not throw — it fails a JUnit assertion because the code returns a WRONG value. There is therefore no exception to catch. Your harness must SUPPLY the oracle:

1. LIFT EVERY ASSERTION (not just one): the failing test almost always checks SEVERAL input/output pairs (e.g. many assertEquals lines). Each pair is a SEPARATE trusted oracle — the correct code is known to produce that exact expected value for that exact input. Find ALL of them (assertEquals / assertTrue / assertSame / assertNull ...), and for each identify (a) the call it makes on the real API and (b) the EXPECTED value it checks against. Copy both VERBATIM — do not infer or recompute; use the literals the test hard-codes.

2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls through the real public API, exactly as the test does, to get each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the test's SETUP: if the expected value depends on environment the test arranges (a registered source file, a Locale or TimeZone, a formatting mode, a fixture object), replicate that setup faithfully — and if you cannot replicate it, DROP that pair rather than assert it, because a half-reconstructed scenario fires on correct code too (that exact mistake caused two false accusations of correct patches).

3. ASSERT ALL OF THEM: if ANY actual value does not equal its expected value, `throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] semantic mismatch: <what differed>")`. Every DISTINCT check gets its own [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs], [oracle:round-trip]) — alarms without an ID are rejected mechanically; the ID is how the pipeline tells which check fired. Check every lifted pair — an overfitting patch that fixed only the one reported input still fails the others, so the more pairs you assert the more likely it is caught. Jazzer reports the throw as a finding, exactly like a crash, so scoring is unchanged. If all match, continue. WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT (compiler/printer output, rendered source, messages with layout): never compare raw strings — normalize BOTH sides first (collapse all whitespace runs, e.g. s.replaceAll("\\s+", "")) so the check fires only on a CONTENT difference. A correct implementation may legitimately differ in spaces, newlines or statement-separator placement, and a raw-string firing over formatting will be dismissed in review — burying the real content difference the same comparison would have caught. COMPARE NORMALIZED, BUT RECORD RAW: whenever you normalize before comparing, the alarm message MUST also carry the PRE-normalization values, under these exact key names: `expectedNormalized=` `actualNormalized=` `expectedRaw=` `actualRaw=`. Emit the two Raw keys ONLY when you actually normalized, so their absence means no normalization happened. Reason: a downstream check asks whether your reported value equals a value the failing test pins, and the test pins the RAW form — a message carrying only the normalized form can never match it, so that check is dead for your alarm and a real dismissal is lost. Named keys, never positional: the reader must never have to guess which form it is holding. KEEP THE WHOLE MESSAGE ON ONE LINE: the raw form of formatted text usually contains newlines and tabs, and the alarm is read line by line — a literal newline inside a value silently cuts the message there, discarding every key after it. So ESCAPE them: emit `\n` and `\t` as the two-character sequences (backslash then n / t), never as real line breaks. In Java: `s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")`. This is also the form the failing test's own source literal is written in, so the comparison lines up exactly.

4. THEN GENERALISE — but keep every assertion TRUSTED. The lifted pair tells you the answer for ONE input only. Do not invent an expected value for an arbitrary fuzzed input: an assertion you cannot justify will fire on CORRECT patches too (a false positive, the worst outcome). Extend coverage only in ways where the correct answer is independently known:

  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with FuzzedDataProvider, build the canonical input that must map to it, and assert you recover it — the answer is trusted because you chose it first. E.g. fuzz an int n, format it, parse it back, assert == n.

  (b) EQUIVALENCE TO THE SEED. The seed input has properties that fix its answer. Generate other inputs that SHARE those properties and must therefore share the seed's expected value, and assert they equal it. E.g. if the contract is case-insensitive and foo("abc")==X, then foo("ABC") and foo("Abc") must also ==X. This turns the one lifted value into an oracle for a whole family of inputs.

  (c) METAMORPHIC RELATION between two real calls (see below) — trusted because it holds for ANY correct implementation.

  GUARDRAIL: for any fuzzed input where you cannot justify the expected value by (a), (b), or (c), still CALL the API on it (this exercises the patched path), but do NOT assert on its result — just return. Assert only trusted answers; explore everything else without asserting.
  CALIBRATION: your assertions will be screened by an automated soundness reviewer before any firing counts, so do NOT hedge to trivially-weak checks to be safe. Write the strongest check you can JUSTIFY from the documented contract, and state that justification in a comment next to the check (the reviewer reads it). The reviewer is a safety net, not a licence to guess — it is imperfect, so an assertion still needs a real justification — but a vacuous assertion (isFinite / not-null / no-throw) can never catch a wrong value and guarantees a miss, which nothing downstream can recover.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `Frequency`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math.stat.FrequencyTest" method="testPcts">
    public void testPcts() {
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);
        assertEquals("one pct",0.25,f.getPct(1),tolerance);
        assertEquals("two pct",0.25,f.getPct(Long.valueOf(2)),tolerance);
        assertEquals("three pct",0.5,f.getPct(threeL),tolerance);
        // MATH-329
        assertEquals("three (Object) pct",0.5,f.getPct((Object) (Integer.valueOf(3))),tolerance);
        assertEquals("five pct",0,f.getPct(5),tolerance);
        assertEquals("foo pct",0,f.getPct("foo"),tolerance);
        assertEquals("one cum pct",0.25,f.getCumPct(1),tolerance);
        assertEquals("two cum pct",0.50,f.getCumPct(Long.valueOf(2)),tolerance);
        assertEquals("Integer argument",0.50,f.getCumPct(Integer.valueOf(2)),tolerance);
        assertEquals("three cum pct",1.0,f.getCumPct(threeL),tolerance);
        assertEquals("five cum pct",1.0,f.getCumPct(5),tolerance);
        assertEquals("zero cum pct",0.0,f.getCumPct(0),tolerance);
        assertEquals("foo cum pct",0,f.getCumPct("foo"),tolerance);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.stat.FrequencyTest::testPcts
junit.framework.AssertionFailedError: three (Object) pct expected:<0.5> but was:<1.0>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.stat.FrequencyTest">
// --- setUp() (test setup) ---
    @Override
    public void setUp() {
        f = new Frequency();
    }

// --- class fields/constants the test uses ---
private long oneL = 1;
private long twoL = 2;
private long threeL = 3;
private int oneI = 1;
private int twoI = 2;
private int threeI=3;
private double tolerance = 10E-15;
private Frequency f = null;
</test_support>
POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

SYNTHESIZED RELATION CANDIDATES (mechanically pre-screened) — invariants/metamorphic relations derived from the documented contract of the patched code. Each was compiled and exercised over many fuzzed inputs on the current build; candidates that fired indiscriminately were dropped as out-of-domain. They are STRONG CANDIDATES, not verified ground truth: implement each one, keep its justification comment next to the check, and fence its inputs exactly as instructed below. These generalise BEYOND the tested inputs, so they can catch an overfit that only special-cased the reported input. Implement each one in your harness in addition to the lifted/mined oracles — but these are EXTRAS, not your whole job: KEEP INVENTING YOUR OWN checks beyond them. Two self-invented shapes have repeatedly been the ones that catch what nothing else does: (a) HIDDEN-STATE checks — after any call documented as a question (get*/is*/contains/indexOf/size, no mention of modifying), re-read the object's cheap observable properties (capacity, size, length, later lookups) and assert they did not change; a 'read-only' call that silently mutates internal state is a classic defect no provided relation will mention. (b) SIBLING-AGREEMENT checks — two methods documented to do the same job (overloads taking char vs String, Class vs class name, etc.) must agree on the same logical input:
<relation name="getPctObject_matches_count_over_sum_for_integer_object" kind="invariant">
  // holds because: For getPct(Object): "Returns the percentage of values that are equal to v (as a proportion between 0 and 1)." Together with getSumFreq and getCount, this means on any non-empty distribution getPct(v) = getCount(v) / getSumFreq. The class javadoc also states that integer values are not distinguished by type for arguments to getPct etc.
  // valid input: Build a non-empty Frequency using only integral values drawn from data, so all inputs are valid and comparable. Probe the changed Object overload with an Integer object argument, which is the exact cross-type case exercised by the failing test.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 4761/20000 on random inputs
Frequency f; int n; int probe; long count; long sum; double actual; double expected; try { f = new Frequency(); n = data.consumeInt(1, 8); for (int i = 0; i < n; i++) { int v = data.consumeInt(-5, 5); switch (data.consumeInt(0, 3)) { case 0: f.addValue(v); break; case 1: f.addValue((long) v); break; case 2: f.addValue(Integer.valueOf(v)); break; default: f.addValue(Long.valueOf(v)); break; } } probe = data.consumeInt(-5, 5); count = f.getCount((Object) Integer.valueOf(probe)); sum = f.getSumFreq(); expected = (double) count / (double) sum; } catch (Exception e) { for (StackTraceElement __rpf : e.getStackTrace()) { String __rpc = __rpf.getClassName(); if ((__rpc.equals("Frequency") || __rpc.endsWith(".Frequency") || __rpc.endsWith("$Frequency")) && !"<init>".equals(__rpf.getMethodName())) { boolean __rpdoc = false; if (!__rpdoc && (__rpf.getClassName().equals("Frequency") || __rpf.getClassName().endsWith(".Frequency") || __rpf.getClassName().endsWith("$Frequency")) && "addValue".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("Frequency") || __rpf.getClassName().endsWith(".Frequency") || __rpf.getClassName().endsWith("$Frequency")) && "compare".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassCastException") || __rpn.endsWith(".ClassCastException") || __rpn.endsWith("$ClassCastException") || __rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpdoc = true; break; } } } if (!__rpdoc) throw new RuntimeException("relation getPctObject_matches_count_over_sum_for_integer_object violated: unexpected " + e.getClass().getName() + " thrown by the patch-changed class on a valid-by-construction input: " + e.getMessage()); break; } } return; } try { actual = f.getPct((Object) Integer.valueOf(probe)); } catch (Exception e) { throw new RuntimeException("relation getPctObject_matches_count_over_sum_for_integer_object violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); } if (!(Math.abs(actual - expected) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) { throw new RuntimeException("relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum); }
</relation>
<relation name="getPct_object_and_comparable_agree_on_same_integer_value" kind="metamorphic">
  // holds because: The deprecated getPct(Object) is documented as replaced by getPct(Comparable), and the class javadoc says integer values are not distinguished by type for arguments to getPct etc. Therefore equivalent Integer inputs through these two overloads must yield the same percentage.
  // valid input: Build any non-empty Frequency using only integral values from data. Query the same integer value once through getPct(Object) with Integer.valueOf(x) and once through getPct(Comparable) with the same Integer object.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 4820/20000 on random inputs
Frequency f; int n; Integer probe; double a; double b; try { f = new Frequency(); n = data.consumeInt(1, 8); for (int i = 0; i < n; i++) { int v = data.consumeInt(-6, 6); if (data.consumeBoolean()) { f.addValue(v); } else { f.addValue((long) v); } } probe = Integer.valueOf(data.consumeInt(-6, 6)); } catch (Exception e) { for (StackTraceElement __rpf : e.getStackTrace()) { String __rpc = __rpf.getClassName(); if ((__rpc.equals("Frequency") || __rpc.endsWith(".Frequency") || __rpc.endsWith("$Frequency")) && !"<init>".equals(__rpf.getMethodName())) { boolean __rpdoc = false; if (!__rpdoc && (__rpf.getClassName().equals("Frequency") || __rpf.getClassName().endsWith(".Frequency") || __rpf.getClassName().endsWith("$Frequency")) && "addValue".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("Frequency") || __rpf.getClassName().endsWith(".Frequency") || __rpf.getClassName().endsWith("$Frequency")) && "compare".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassCastException") || __rpn.endsWith(".ClassCastException") || __rpn.endsWith("$ClassCastException") || __rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpdoc = true; break; } } } if (!__rpdoc) throw new RuntimeException("relation getPct_object_and_comparable_agree_on_same_integer_value violated: unexpected " + e.getClass().getName() + " thrown by the patch-changed class on a valid-by-construction input: " + e.getMessage()); break; } } return; } try { a = f.getPct((Object) probe); } catch (Exception e) { throw new RuntimeException("relation getPct_object_and_comparable_agree_on_same_integer_value violated: unexpected " + e.getClass().getName() + " from getPct(Object): " + e.getMessage()); } try { b = f.getPct((Comparable<?>) probe); } catch (Exception e) { throw new RuntimeException("relation getPct_object_and_comparable_agree_on_same_integer_value violated: unexpected " + e.getClass().getName() + " from getPct(Comparable): " + e.getMessage()); } if (!(Math.abs(a - b) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) { throw new RuntimeException("relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a + " comparable=" + b); }
</relation>
Build each relation's input with the FuzzedDataProvider so it is VALID BY CONSTRUCTION, wrap the API calls in try/catch and SKIP (return) on any caught exception — an exception is a rejection, never a violation. Throw com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("relation <name> violated: ...") only on a genuine disagreement, and include the observed values in the message. Two structural rules (checked mechanically, violations are rejected): (1) the violation throw goes OUTSIDE/AFTER the try/catch that wraps the API calls — thrown inside your own catch-everything block it is caught and discarded, dead code; (2) if you ever convert a CAUGHT exception into your own alarm, pass it as the cause: new FuzzerSecurityIssueLow("...", e) — never bury it in the message text.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~11,430 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedTestOracles();
        runStateIndependentAbsentProbeChecks(data);
        runRelationCountOverSum(data);
        runRelationObjectComparableAgreement(data);
    }

    private static void runLiftedTestOracles() {
        Frequency f = new Frequency();
        String violation = null;
        try {
            long oneL = 1;
            long twoL = 2;
            long threeL = 3;
            int oneI = 1;
            int twoI = 2;
            int threeI = 3;

            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);

            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String stringBefore = f.toString();

            double onePct = f.getPct(1);
            double twoPct = f.getPct(Long.valueOf(2));
            double threePct = f.getPct(threeL);
            double threeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
            double fivePct = f.getPct(5);
            double fooPct = f.getPct("foo");
            double oneCumPct = f.getCumPct(1);
            double twoCumPct = f.getCumPct(Long.valueOf(2));
            double integerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
            double threeCumPct = f.getCumPct(threeL);
            double fiveCumPct = f.getCumPct(5);
            double zeroCumPct = f.getCumPct(0);
            double fooCumPct = f.getCumPct("foo");

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String stringAfter = f.toString();

            // Read-only post-condition: get* methods are queries; they must not mutate observable state.
            // A "fix" that changes bookkeeping or caches incorrectly would break these cheap readers.
            if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
                violation = "[oracle:hidden-state-readonly] semantic mismatch: get* query mutated state sumBefore=" + sumBefore
                        + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                        + " toStringBefore=" + escape(stringBefore) + " toStringAfter=" + escape(stringAfter);
            } else if (!approxEq(onePct, 0.25, TOLERANCE)) {
                violation = "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + onePct;
            } else if (!approxEq(twoPct, 0.25, TOLERANCE)) {
                violation = "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + twoPct;
            } else if (!approxEq(threePct, 0.5, TOLERANCE)) {
                violation = "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + threePct;
            } else if (!approxEq(threeObjectPct, 0.5, TOLERANCE)) {
                violation = "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + threeObjectPct;
            } else if (!approxEq(fivePct, 0.0, TOLERANCE)) {
                violation = "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + fivePct;
            } else if (!approxEq(fooPct, 0.0, TOLERANCE)) {
                violation = "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + fooPct;
            } else if (!approxEq(oneCumPct, 0.25, TOLERANCE)) {
                violation = "[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + oneCumPct;
            } else if (!approxEq(twoCumPct, 0.50, TOLERANCE)) {
                violation = "[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + twoCumPct;
            } else if (!approxEq(integerArgumentCumPct, 0.50, TOLERANCE)) {
                violation = "[oracle:integer-argument-cum-pct] semantic mismatch: expected=0.5 actual=" + integerArgumentCumPct;
            } else if (!approxEq(threeCumPct, 1.0, TOLERANCE)) {
                violation = "[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + threeCumPct;
            } else if (!approxEq(fiveCumPct, 1.0, TOLERANCE)) {
                violation = "[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + fiveCumPct;
            } else if (!approxEq(zeroCumPct, 0.0, TOLERANCE)) {
                violation = "[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + zeroCumPct;
            } else if (!approxEq(fooCumPct, 0.0, TOLERANCE)) {
                violation = "[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + fooCumPct;
            }
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void runStateIndependentAbsentProbeChecks(FuzzedDataProvider data) {
        Frequency f = new Frequency();
        String violation = null;
        try {
            int n = data.consumeInt(1, 8);
            int absentLow = data.consumeInt(-20, -1);
            int base = data.consumeInt(1, 10);

            for (int i = 0; i < n; i++) {
                int v = base + data.consumeInt(0, 10);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }

                // Re-probed after every state change as required for absent-value rejection checks.
                // With only positive integral values inserted, a smaller absent integral probe must have pct 0 and cumPct 0,
                // and an unrelated String probe must have pct 0; these documented query outcomes must not depend on container state.
                double pctAbsentLow = f.getPct(absentLow);
                double cumPctAbsentLow = f.getCumPct(absentLow);
                double pctFoo = f.getPct("foo");

                if (!approxEq(pctAbsentLow, 0.0, TOLERANCE)) {
                    violation = "[oracle:reprobe-absent-pct] semantic mismatch: expected=0.0 actual=" + pctAbsentLow
                            + " afterMutationIndex=" + i + " probe=" + absentLow;
                    break;
                }
                if (!approxEq(cumPctAbsentLow, 0.0, TOLERANCE)) {
                    violation = "[oracle:reprobe-absent-cum-pct] semantic mismatch: expected=0.0 actual=" + cumPctAbsentLow
                            + " afterMutationIndex=" + i + " probe=" + absentLow;
                    break;
                }
                if (!approxEq(pctFoo, 0.0, TOLERANCE)) {
                    violation = "[oracle:reprobe-foo-pct] semantic mismatch: expected=0.0 actual=" + pctFoo
                            + " afterMutationIndex=" + i;
                    break;
                }
            }
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void runRelationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        String violation = null;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }

        // Contract guarantee: getPct(Object) returns the percentage of values equal to v; with non-empty Frequency,
        // that is exactly getCount(v) / getSumFreq(). A patch that redirects to cumulative percentage violates this.
        if (!approxEq(actual, expected, 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) {
            violation = "relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + actual
                    + " expected=" + expected + " count=" + count + " sum=" + sum + " probe=" + probe;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void runRelationObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double a;
        double b;
        String violation = null;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            a = f.getPct((Object) probe);
            b = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        // Contract guarantee: deprecated getPct(Object) is the sibling overload for getPct(Comparable);
        // on the same Integer value they must agree. The buggy implementation returns cumulative pct from the Object overload.
        if (!approxEq(a, b, 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) {
            violation = "relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a
                    + " comparable=" + b + " probe=" + probe;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static boolean approxEq(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [43] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [44] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,715 chars, 1 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.stat` (`package org.apache.commons.math.stat;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_75_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/stat/Frequency.java
+++ /src/main/java/org/apache/commons/math/stat/Frequency.java
@@ -300,7 +300,7 @@ public class Frequency implements Serializable {
      */
     @Deprecated
     public double getPct(Object v) {
-        return getCumPct((Comparable<?>) v);
+        return getPct((Comparable<?>) v);
     }
 
     /**
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Comparator;
import java.util.TreeMap;
import org.apache.commons.math.MathRuntimeException;
</source_imports>

Function `getPct`:
<signature>
public double getPct(Object v)
</signature>
<code>
    public double getPct(Object v) {
        return getCumPct((Comparable<?>) v);
    }
</code>

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="Frequency" role="patched">
     * Maintains a frequency distribution.
     * <p>
     * Accepts int, long, char or Comparable values.  New values added must be
     * comparable to those that have been added, otherwise the add method will
     * throw an IllegalArgumentException.</p>
     * <p>
     * Integer values (int, long, Integer, Long) are not distinguished by type --
     * i.e. <code>addValue(Long.valueOf(2)), addValue(2), addValue(2l)</code> all have
     * the same effect (similarly for arguments to <code>getCount,</code> etc.).</p>
     * <p>
     * char values are converted by <code>addValue</code> to Character instances.
     * As such, these values are not comparable to integral values, so attempts
     * …
public class Frequency implements Serializable {
     * Serializable version identifier
    private static final long serialVersionUID = -3845586908418844111L;
     * underlying collection
    private final TreeMap<Comparable<?>, Long> freqTable;
     * Default constructor.
    public Frequency() {
        freqTable = new TreeMap<Comparable<?>, Long>();
    }
     * Constructor allowing values Comparator to be specified.
     * @param comparator Comparator used to order values
    @SuppressWarnings("unchecked") public Frequency(Comparator<?> comparator) { … }
     * Return a string representation of this frequency
     * distribution.
     * @return a string representation.
    @Override public String toString() { … }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not Comparable,
     * or is not comparable with previous entries
     * @deprecated use {@link #addValue(Comparable)} instead
    @Deprecated public void addValue(Object v) { … }
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must
     * be comparable to those that have already been added.
     * </p>
     * @param v the value to add.
     * @throws IllegalArgumentException if <code>v</code> is not comparable with previous entries
    public void addValue(Comparable<?> v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(int v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
     * @deprecated to be removed in math 3.0
    public void addValue(Integer v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(long v) { … }
     * Adds 1 to the frequency count for v.
     * @param v the value to add.
    public void addValue(char v) { … }
     * Clears the frequency table
    public void clear() { … }
     * Returns an Iterator over the set of values that have been added.
     * <p>
     * If added values are integral (i.e., integers, longs, Integers, or Longs),
     * they are converted to Longs when they are added, so the objects returned
     * by the Iterator will in this case be Longs.</p>
     * @return values Iterator
    public Iterator<Comparable<?>> valuesIterator() { … }
     * Returns the sum of all frequencies.
     * @return the total frequency count.
    public long getSumFreq() { … }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
     * @deprecated replaced by {@link #getCount(Comparable)} as of 2.0
    @Deprecated public long getCount(Object v) { … }
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(Comparable<?> v) { … }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(int v) { … }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(long v) { … }
     * Returns the number of values = v.
     * @param v the value to lookup.
     * @return the frequency of v.
    public long getCount(char v) { … }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
     * @deprecated replaced by {@link #getPct(Comparable)} as of 2.0
    @Deprecated public double getPct(Object v) {
        return getCumPct((Comparable<?>) v);
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(Comparable<?> v) {
        final long sumFreq = getSumFreq();
        if (sumFreq == 0) {
            return Double.NaN;
        }
        return (double) getCount(v) / (double) sumFreq;
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(int v) {
        return getPct(Long.valueOf(v));
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(long v) {
        return getPct(Long.valueOf(v));
    }
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public double getPct(char v) {
        return getPct(Character.valueOf(v));
    }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
     * @deprecated replaced by {@link #getCumFreq(Comparable)} as of 2.0
    @Deprecated public long getCumFreq(Object v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup.
     * @return the proportion of values equal to v
    @SuppressWarnings("unchecked") public long getCumFreq(Comparable<?> v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(int v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(long v) { … }
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values equal to v
    public long getCumFreq(char v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
     * @deprecated replaced by {@link #getCumPct(Comparable)} as of 2.0
    @Deprecated public double getCumPct(Object v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(Comparable<?> v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(int v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(long v) { … }
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.</p>
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
    public double getCumPct(char v) { … }
     * A Comparator that compares comparable objects using the
     * natural order.  Copied from Commons Collections ComparableComparator.
    private static class NaturalComparator<T extends Comparable<T>> implements Comparator<Comparable<T>>, Serializable { … }
     * {@inheritDoc}
    @Override public int hashCode() { … }
     * {@inheritDoc}
    @Override public boolean equals(Object obj) { … }
}
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) {
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) {
        super(rootCause);
        this.pattern   = getMessage();
        this.arguments = new Object[0];
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) {
        super(rootCause);
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { … }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { … }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { … }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { … }
     * {@inheritDoc}
    @Override public String getMessage() { … }
     * {@inheritDoc}
    @Override public String getLocalizedMessage() { … }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { … }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { … }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
</class_skeletons>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param comparator Comparator used to order values
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addValue(Object v) / (Comparable<?> v) / (int v) / (Integer v) / (long v) / (char v)
  getCount(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getCumFreq(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getCumPct(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)
  getPct(Object v) / (Comparable<?> v) / (int v) / (long v) / (char v)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getCount, getCumFreq, getCumPct, getPct, getSumFreq

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getSumFreq, hashCode, toString

THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does not throw — it fails a JUnit assertion because the code returns a WRONG value. There is therefore no exception to catch. Your harness must SUPPLY the oracle:

1. LIFT EVERY ASSERTION (not just one): the failing test almost always checks SEVERAL input/output pairs (e.g. many assertEquals lines). Each pair is a SEPARATE trusted oracle — the correct code is known to produce that exact expected value for that exact input. Find ALL of them (assertEquals / assertTrue / assertSame / assertNull ...), and for each identify (a) the call it makes on the real API and (b) the EXPECTED value it checks against. Copy both VERBATIM — do not infer or recompute; use the literals the test hard-codes.

2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls through the real public API, exactly as the test does, to get each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the test's SETUP: if the expected value depends on environment the test arranges (a registered source file, a Locale or TimeZone, a formatting mode, a fixture object), replicate that setup faithfully — and if you cannot replicate it, DROP that pair rather than assert it, because a half-reconstructed scenario fires on correct code too (that exact mistake caused two false accusations of correct patches).

3. ASSERT ALL OF THEM: if ANY actual value does not equal its expected value, `throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] semantic mismatch: <what differed>")`. Every DISTINCT check gets its own [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs], [oracle:round-trip]) — alarms without an ID are rejected mechanically; the ID is how the pipeline tells which check fired. Check every lifted pair — an overfitting patch that fixed only the one reported input still fails the others, so the more pairs you assert the more likely it is caught. Jazzer reports the throw as a finding, exactly like a crash, so scoring is unchanged. If all match, continue. WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT (compiler/printer output, rendered source, messages with layout): never compare raw strings — normalize BOTH sides first (collapse all whitespace runs, e.g. s.replaceAll("\\s+", "")) so the check fires only on a CONTENT difference. A correct implementation may legitimately differ in spaces, newlines or statement-separator placement, and a raw-string firing over formatting will be dismissed in review — burying the real content difference the same comparison would have caught. COMPARE NORMALIZED, BUT RECORD RAW: whenever you normalize before comparing, the alarm message MUST also carry the PRE-normalization values, under these exact key names: `expectedNormalized=` `actualNormalized=` `expectedRaw=` `actualRaw=`. Emit the two Raw keys ONLY when you actually normalized, so their absence means no normalization happened. Reason: a downstream check asks whether your reported value equals a value the failing test pins, and the test pins the RAW form — a message carrying only the normalized form can never match it, so that check is dead for your alarm and a real dismissal is lost. Named keys, never positional: the reader must never have to guess which form it is holding. KEEP THE WHOLE MESSAGE ON ONE LINE: the raw form of formatted text usually contains newlines and tabs, and the alarm is read line by line — a literal newline inside a value silently cuts the message there, discarding every key after it. So ESCAPE them: emit `\n` and `\t` as the two-character sequences (backslash then n / t), never as real line breaks. In Java: `s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")`. This is also the form the failing test's own source literal is written in, so the comparison lines up exactly.

4. THEN GENERALISE — but keep every assertion TRUSTED. The lifted pair tells you the answer for ONE input only. Do not invent an expected value for an arbitrary fuzzed input: an assertion you cannot justify will fire on CORRECT patches too (a false positive, the worst outcome). Extend coverage only in ways where the correct answer is independently known:

  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with FuzzedDataProvider, build the canonical input that must map to it, and assert you recover it — the answer is trusted because you chose it first. E.g. fuzz an int n, format it, parse it back, assert == n.

  (b) EQUIVALENCE TO THE SEED. The seed input has properties that fix its answer. Generate other inputs that SHARE those properties and must therefore share the seed's expected value, and assert they equal it. E.g. if the contract is case-insensitive and foo("abc")==X, then foo("ABC") and foo("Abc") must also ==X. This turns the one lifted value into an oracle for a whole family of inputs.

  (c) METAMORPHIC RELATION between two real calls (see below) — trusted because it holds for ANY correct implementation.

  GUARDRAIL: for any fuzzed input where you cannot justify the expected value by (a), (b), or (c), still CALL the API on it (this exercises the patched path), but do NOT assert on its result — just return. Assert only trusted answers; explore everything else without asserting.
  CALIBRATION: your assertions will be screened by an automated soundness reviewer before any firing counts, so do NOT hedge to trivially-weak checks to be safe. Write the strongest check you can JUSTIFY from the documented contract, and state that justification in a comment next to the check (the reviewer reads it). The reviewer is a safety net, not a licence to guess — it is imperfect, so an assertion still needs a real justification — but a vacuous assertion (isFinite / not-null / no-throw) can never catch a wrong value and guarantees a miss, which nothing downstream can recover.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `Frequency`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math.stat.FrequencyTest" method="testPcts">
    public void testPcts() {
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);
        assertEquals("one pct",0.25,f.getPct(1),tolerance);
        assertEquals("two pct",0.25,f.getPct(Long.valueOf(2)),tolerance);
        assertEquals("three pct",0.5,f.getPct(threeL),tolerance);
        // MATH-329
        assertEquals("three (Object) pct",0.5,f.getPct((Object) (Integer.valueOf(3))),tolerance);
        assertEquals("five pct",0,f.getPct(5),tolerance);
        assertEquals("foo pct",0,f.getPct("foo"),tolerance);
        assertEquals("one cum pct",0.25,f.getCumPct(1),tolerance);
        assertEquals("two cum pct",0.50,f.getCumPct(Long.valueOf(2)),tolerance);
        assertEquals("Integer argument",0.50,f.getCumPct(Integer.valueOf(2)),tolerance);
        assertEquals("three cum pct",1.0,f.getCumPct(threeL),tolerance);
        assertEquals("five cum pct",1.0,f.getCumPct(5),tolerance);
        assertEquals("zero cum pct",0.0,f.getCumPct(0),tolerance);
        assertEquals("foo cum pct",0,f.getCumPct("foo"),tolerance);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.stat.FrequencyTest::testPcts
junit.framework.AssertionFailedError: three (Object) pct expected:<0.5> but was:<1.0>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.stat.FrequencyTest">
// --- setUp() (test setup) ---
    @Override
    public void setUp() {
        f = new Frequency();
    }

// --- class fields/constants the test uses ---
private long oneL = 1;
private long twoL = 2;
private long threeL = 3;
private int oneI = 1;
private int twoI = 2;
private int threeI=3;
private double tolerance = 10E-15;
private Frequency f = null;
</test_support>
POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~10,238 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();

        f.addValue(oneL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-1] metamorphic violation: addValue must increase total frequency by 1 input=1 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(twoL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-2] metamorphic violation: addValue must increase total frequency by 1 input=2 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(oneI);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-3] metamorphic violation: addValue must increase total frequency by 1 input=1 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(twoI);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-4] metamorphic violation: addValue must increase total frequency by 1 input=2 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(threeL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-5] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(threeL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-6] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(3);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-7] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(threeI);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-8] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }

        double actual;

        actual = f.getPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-one-cum-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-two-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-integer-arg-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-three-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-five-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        try {
            Frequency g = new Frequency();

            int base = data.consumeInt(-1000, 1000);
            int mutationCount = data.consumeInt(1, 16);
            int probe = base + 1000000;

            for (int i = 0; i < mutationCount; i++) {
                int delta = data.consumeInt(-5, 5);
                int added = base + delta;
                if (added == probe) {
                    added = base;
                }

                long sumBefore = g.getSumFreq();
                g.addValue(added);

                if (g.getSumFreq() != sumBefore + 1) {
                    throw new RuntimeException("[oracle:fuzz-sum-increment] metamorphic violation: addValue must increase total frequency by 1 input=" + added + " lhs=" + g.getSumFreq() + " rhs=" + (sumBefore + 1));
                }

                /* Contract used:
                 * - getPct(Object) is deprecated in favor of getPct(Comparable); they document the same result:
                 *   "Returns the percentage of values that are equal to v".
                 * - We choose probe first, then only add values from base-5..base+5, so probe is absent by construction.
                 * Therefore for every non-empty state reached here, both overloads must return the same documented value, 0.0.
                 * A patch that silently routes Object to cumulative percentage instead of exact percentage breaks this without throwing.
                 */
                double objectPct = g.getPct((Object) Integer.valueOf(probe));
                double comparablePct = g.getPct(Integer.valueOf(probe));
                if (Math.abs(objectPct - comparablePct) > tolerance) {
                    throw new RuntimeException("[oracle:overload-equivalence-absent] metamorphic violation: getPct(Object) must agree with getPct(Comparable) for the same Integer input=" + probe + " lhs=" + objectPct + " rhs=" + comparablePct);
                }
                if (Math.abs(objectPct - 0.0d) > tolerance) {
                    throw new RuntimeException("[oracle:absent-probe-zero] metamorphic violation: absent value must have percentage 0.0 input=" + probe + " lhs=" + objectPct + " rhs=0.0");
                }
            }

            if (g.getSumFreq() > 0) {
                int target = base + data.consumeInt(-5, 5);

                /* Contract used:
                 * getPct(Comparable) is documented as the proportion of values equal to v.
                 * The Object overload is deprecated-replaced by that overload, so they must agree on the same Integer value.
                 * This post-condition is directly observable through the public API and catches wrong silent behavior.
                 */
                double pctObj = g.getPct((Object) Integer.valueOf(target));
                double pctCmp = g.getPct(Integer.valueOf(target));
                if (Math.abs(pctObj - pctCmp) > tolerance) {
                    throw new RuntimeException("[oracle:overload-equivalence-present-or-absent] metamorphic violation: equivalent overloads disagree input=" + target + " lhs=" + pctObj + " rhs=" + pctCmp);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [45] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test-three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [46] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,681 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        String beforeToString = f.toString();
        long beforeSumFreq = f.getSumFreq();
        int beforeHash = f.hashCode();

        double actualOnePct = f.getPct(1);
        if (!closeEnough(actualOnePct, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (!closeEnough(actualTwoPct, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (!closeEnough(actualThreePct, 0.5, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (!closeEnough(actualThreeObjectPct, 0.5, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (!closeEnough(actualFivePct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (!closeEnough(actualFooPct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (!closeEnough(actualOneCumPct, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (!closeEnough(actualTwoCumPct, 0.50, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        if (!closeEnough(actualIntegerArgumentCumPct, 0.50, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-integer-argument] semantic mismatch: expected=0.5 actual=" + actualIntegerArgumentCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (!closeEnough(actualThreeCumPct, 1.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (!closeEnough(actualFiveCumPct, 1.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (!closeEnough(actualZeroCumPct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (!closeEnough(actualFooCumPct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        long afterSumFreq = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterToString = f.toString();
        if (beforeSumFreq != afterSumFreq || beforeHash != afterHash || !safeEquals(beforeToString, afterToString)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-read-only] semantic mismatch: getPct/getCumPct are read-only queries, so getSumFreq/hashCode/toString must not change; beforeSumFreq=" + beforeSumFreq + " afterSumFreq=" + afterSumFreq + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeToString=" + escapeOneLine(beforeToString) + " afterToString=" + escapeOneLine(afterToString));
        }

        {
            Frequency relFreq;
            int n;
            int probe;
            long count;
            long sum;
            double actual;
            double expected;
            try {
                relFreq = new Frequency();
                n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-5, 5);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            relFreq.addValue(v);
                            break;
                        case 1:
                            relFreq.addValue((long) v);
                            break;
                        case 2:
                            relFreq.addValue(Integer.valueOf(v));
                            break;
                        default:
                            relFreq.addValue(Long.valueOf(v));
                            break;
                    }
                }
                probe = data.consumeInt(-5, 5);
                count = relFreq.getCount((Object) Integer.valueOf(probe));
                sum = relFreq.getSumFreq();
                expected = (double) count / (double) sum;
                actual = relFreq.getPct((Object) Integer.valueOf(probe));
            } catch (Exception e) {
                return;
            }
            if (!closeEnough(actual, expected, 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) {
                throw new FuzzerSecurityIssueLow("[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe + " count=" + count + " sum=" + sum + " expected=" + expected + " actual=" + actual);
            }
        }

        {
            Frequency relFreq;
            int n;
            Integer probe;
            double a;
            double b;
            try {
                relFreq = new Frequency();
                n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-6, 6);
                    if (data.consumeBoolean()) {
                        relFreq.addValue(v);
                    } else {
                        relFreq.addValue((long) v);
                    }
                }
                probe = Integer.valueOf(data.consumeInt(-6, 6));
                a = relFreq.getPct((Object) probe);
                b = relFreq.getPct((Comparable<?>) probe);
            } catch (Exception e) {
                return;
            }
            if (!closeEnough(a, b, 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) {
                throw new FuzzerSecurityIssueLow("[oracle:object-vs-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe + " object=" + a + " comparable=" + b);
            }
        }

        {
            Frequency relFreq;
            int n;
            int probe;
            double pct;
            long count;
            long sum;
            try {
                relFreq = new Frequency();
                n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-4, 4);
                    if (data.consumeBoolean()) {
                        relFreq.addValue(Integer.valueOf(v));
                    } else {
                        relFreq.addValue(Long.valueOf(v));
                    }
                }
                probe = data.consumeInt(-4, 4);
                pct = relFreq.getPct(probe);
                count = relFreq.getCount(probe);
                sum = relFreq.getSumFreq();
            } catch (Exception e) {
                return;
            }
            double expected = (double) count / (double) sum;
            if (!closeEnough(pct, expected, 1e-9 * Math.max(1.0, Math.max(Math.abs(pct), Math.abs(expected))))) {
                throw new FuzzerSecurityIssueLow("[oracle:int-overload-count-over-sum] metamorphic violation: getPct(int) must equal getCount(int)/getSumFreq on a non-empty distribution; probe=" + probe + " count=" + count + " sum=" + sum + " expected=" + expected + " actual=" + pct);
            }
        }
    }

    private static boolean closeEnough(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [47] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [48] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,737 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        long sumBeforeReads = f.getSumFreq();
        String toStringBeforeReads = f.toString();
        int hashBeforeReads = f.hashCode();

        assertApprox("one pct", 0.25, f.getPct(1), tolerance, "lifted-one-pct");
        assertApprox("two pct", 0.25, f.getPct(Long.valueOf(2)), tolerance, "lifted-two-pct");
        assertApprox("three pct", 0.5, f.getPct(threeL), tolerance, "lifted-three-pct");
        assertApprox("three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "lifted-three-object-pct");
        assertApprox("five pct", 0.0, f.getPct(5), tolerance, "lifted-five-pct");
        assertApprox("foo pct", 0.0, f.getPct("foo"), tolerance, "lifted-foo-pct");
        assertApprox("one cum pct", 0.25, f.getCumPct(1), tolerance, "lifted-one-cum-pct");
        assertApprox("two cum pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "lifted-two-cum-pct");
        assertApprox("Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "lifted-integer-cum-pct");
        assertApprox("three cum pct", 1.0, f.getCumPct(threeL), tolerance, "lifted-three-cum-pct");
        assertApprox("five cum pct", 1.0, f.getCumPct(5), tolerance, "lifted-five-cum-pct");
        assertApprox("zero cum pct", 0.0, f.getCumPct(0), tolerance, "lifted-zero-cum-pct");
        assertApprox("foo cum pct", 0.0, f.getCumPct("foo"), tolerance, "lifted-foo-cum-pct");

        assertLongEquals("read-only get* methods must not change sumFreq", sumBeforeReads, f.getSumFreq(), "read-only-sum");
        assertStringEquals("read-only get* methods must not change toString", toStringBeforeReads, f.toString(), "read-only-string");
        assertIntEquals("read-only get* methods must not change hashCode", hashBeforeReads, f.hashCode(), "read-only-hash");

        try {
            int low = data.consumeInt(-1000, 999);
            int gap = data.consumeInt(1, 1000);
            int high = low + gap;
            int lowCount = data.consumeInt(1, 20);
            int highCount = data.consumeInt(1, 20);

            Frequency g = new Frequency();

            for (int i = 0; i < lowCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        g.addValue(low);
                        break;
                    case 1:
                        g.addValue((long) low);
                        break;
                    default:
                        g.addValue(Integer.valueOf(low));
                        break;
                }
                // Contract: absent incomparable String query returns 0 for getPct/getCumPct when values are present and String is not comparable to numeric values.
                assertApprox("reprobe absent foo pct after mutation", 0.0, g.getPct("foo"), tolerance, "reprobe-foo-pct");
                assertApprox("reprobe absent foo cum pct after mutation", 0.0, g.getCumPct("foo"), tolerance, "reprobe-foo-cum-pct");
            }

            for (int i = 0; i < highCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        g.addValue(high);
                        break;
                    case 1:
                        g.addValue((long) high);
                        break;
                    default:
                        g.addValue(Integer.valueOf(high));
                        break;
                }
                // Same documented rejection as above must remain 0 after every state change, independent of container contents.
                assertApprox("reprobe absent foo pct after mutation", 0.0, g.getPct("foo"), tolerance, "reprobe-foo-pct");
                assertApprox("reprobe absent foo cum pct after mutation", 0.0, g.getCumPct("foo"), tolerance, "reprobe-foo-cum-pct");
            }

            long sumBeforeMeta = g.getSumFreq();
            int hashBeforeMeta = g.hashCode();
            String stringBeforeMeta = g.toString();

            double pctObject = g.getPct((Object) Integer.valueOf(low));
            double pctComparable = g.getPct((Comparable<?>) Integer.valueOf(low));
            double expectedPct = ((double) lowCount) / ((double) (lowCount + highCount));

            // Contract: deprecated getPct(Object) is replaced by getPct(Comparable), so equivalent Integer inputs must produce the same percentage.
            if (!approxEqual(pctObject, pctComparable, tolerance)) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: getPct(Object)==getPct(Comparable) input=" + low + " lhs=" + pctObject + " rhs=" + pctComparable);
            }

            // Contract: getPct returns the proportion of values equal to v; here we constructed exactly lowCount copies of low out of lowCount+highCount total.
            if (!approxEqual(pctObject, expectedPct, tolerance)) {
                throw new RuntimeException("[oracle:constructed-answer] metamorphic violation: constructed percentage mismatch input=" + low + " lhs=" + pctObject + " rhs=" + expectedPct);
            }

            // Contract: get* methods are read-only accessors; they must not mutate observable state.
            if (sumBeforeMeta != g.getSumFreq()) {
                throw new RuntimeException("[oracle:meta-read-only-sum] metamorphic violation: sum changed after reads input=" + low + " lhs=" + sumBeforeMeta + " rhs=" + g.getSumFreq());
            }
            if (hashBeforeMeta != g.hashCode()) {
                throw new RuntimeException("[oracle:meta-read-only-hash] metamorphic violation: hash changed after reads input=" + low + " lhs=" + hashBeforeMeta + " rhs=" + g.hashCode());
            }
            if (!stringBeforeMeta.equals(g.toString())) {
                throw new RuntimeException("[oracle:meta-read-only-string] metamorphic violation: string changed after reads input=" + low + " lhs=" + escape(stringBeforeMeta) + " rhs=" + escape(g.toString()));
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean approxEqual(double expected, double actual, double tolerance) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return Double.isNaN(expected) && Double.isNaN(actual);
        }
        return Math.abs(expected - actual) <= tolerance;
    }

    private static void assertApprox(String what, double expected, double actual, double tolerance, String oracleId) {
        if (!approxEqual(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertLongEquals(String what, long expected, long actual, String oracleId) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertIntEquals(String what, int expected, int actual, String oracleId) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertStringEquals(String what, String expected, String actual, String oracleId) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + escape(expected) + " actual=" + escape(actual)
            );
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [49] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [50] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,991 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();
        hiddenStateOracle();

        if (data.remainingBytes() <= 0) {
            return;
        }

        relationCountOverSum(data);
        relationObjectComparableAgree(data);
        relationObjectIntAgreeAndReadOnly(data);
    }

    private static void liftedTestOracles() {
        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        assertApprox("lifted-reprobe-foo-after-add-1", 0.0d, f.getPct("foo"));
        f.addValue(twoL);
        assertApprox("lifted-reprobe-foo-after-add-2", 0.0d, f.getPct("foo"));
        f.addValue(oneI);
        assertApprox("lifted-reprobe-foo-after-add-3", 0.0d, f.getPct("foo"));
        f.addValue(twoI);
        assertApprox("lifted-reprobe-foo-after-add-4", 0.0d, f.getPct("foo"));
        f.addValue(threeL);
        assertApprox("lifted-reprobe-foo-after-add-5", 0.0d, f.getPct("foo"));
        f.addValue(threeL);
        assertApprox("lifted-reprobe-foo-after-add-6", 0.0d, f.getPct("foo"));
        f.addValue(3);
        assertApprox("lifted-reprobe-foo-after-add-7", 0.0d, f.getPct("foo"));
        f.addValue(threeI);
        assertApprox("lifted-reprobe-foo-after-add-8", 0.0d, f.getPct("foo"));

        assertApprox("one-pct", 0.25d, f.getPct(1));
        assertApprox("two-pct", 0.25d, f.getPct(Long.valueOf(2)));
        assertApprox("three-pct", 0.5d, f.getPct(threeL));
        assertApprox("three-object-pct", 0.5d, f.getPct((Object) Integer.valueOf(3)));
        assertApprox("five-pct", 0.0d, f.getPct(5));
        assertApprox("foo-pct", 0.0d, f.getPct("foo"));
        assertApprox("one-cum-pct", 0.25d, f.getCumPct(1));
        assertApprox("two-cum-pct", 0.50d, f.getCumPct(Long.valueOf(2)));
        assertApprox("integer-argument-cum-pct", 0.50d, f.getCumPct(Integer.valueOf(2)));
        assertApprox("three-cum-pct", 1.0d, f.getCumPct(threeL));
        assertApprox("five-cum-pct", 1.0d, f.getCumPct(5));
        assertApprox("zero-cum-pct", 0.0d, f.getCumPct(0));
        assertApprox("foo-cum-pct", 0.0d, f.getCumPct("foo"));
    }

    private static void hiddenStateOracle() {
        Frequency f = new Frequency();
        f.addValue(1L);
        f.addValue(2L);
        f.addValue(1);
        f.addValue(2);
        f.addValue(3L);
        f.addValue(3L);
        f.addValue(3);
        f.addValue(Integer.valueOf(3));

        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();

        double objectPct = f.getPct((Object) Integer.valueOf(3));

        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterString = f.toString();

        /* getPct/getCumPct are read-only queries by contract ("Returns the percentage..."),
           so cheap observable state readers must not change across the call. A patch that
           deletes bookkeeping or mutates hidden state while answering would violate this. */
        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state] semantic mismatch: getPct(Object) changed read-only state beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString) + " pct=" + objectPct);
        }

        assertApprox("hidden-state-pct-value", 0.5d, objectPct);
    }

    private static void relationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        long beforeSum;
        int beforeHash;
        String beforeString;
        long afterSum;
        int afterHash;
        String afterString;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;

            beforeSum = f.getSumFreq();
            beforeHash = f.hashCode();
            beforeString = f.toString();
            actual = f.getPct((Object) Integer.valueOf(probe));
            afterSum = f.getSumFreq();
            afterHash = f.hashCode();
            afterString = f.toString();
        } catch (Throwable t) {
            return;
        }

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-count-over-sum-hidden-state] semantic mismatch: getPct(Object) changed read-only state beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString));
        }

        /* Contract: getPct(Object) returns the proportion of values equal to v.
           On a non-empty Frequency, that is exactly getCount(v) / getSumFreq().
           This uses only real API results and a valid Integer object probe. */
        if (!approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-count-over-sum] semantic mismatch: getPct(Object)!=getCount/getSumFreq probe=" + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void relationObjectComparableAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double objectPct;
        double comparablePct;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        /* Contract: deprecated getPct(Object) is the Object sibling of getPct(Comparable),
           and integer values are not distinguished by type in Frequency; therefore the same
           Integer logical value must produce the same percentage through both overloads. */
        if (!approxEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-object-comparable-agree] semantic mismatch: same Integer value through getPct(Object) and getPct(Comparable) disagreed probe=" + probe + " objectPct=" + objectPct + " comparablePct=" + comparablePct);
        }
    }

    private static void relationObjectIntAgreeAndReadOnly(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double objectPct;
        double intPct;
        long beforeSum;
        int beforeHash;
        String beforeString;
        long afterSum;
        int afterHash;
        String afterString;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(Integer.valueOf(v));
                } else {
                    f.addValue(Long.valueOf(v));
                }
            }
            probe = data.consumeInt(-6, 6);

            beforeSum = f.getSumFreq();
            beforeHash = f.hashCode();
            beforeString = f.toString();
            objectPct = f.getPct((Object) Integer.valueOf(probe));
            afterSum = f.getSumFreq();
            afterHash = f.hashCode();
            afterString = f.toString();

            intPct = f.getPct(probe);
        } catch (Throwable t) {
            return;
        }

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-object-int-hidden-state] semantic mismatch: getPct(Object) changed read-only state beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString));
        }

        /* Sibling-agreement check: getPct(Object) and getPct(int) are documented overloads
           over the same logical integral input space; Frequency does not distinguish integer
           values by type, so the same numeric value must yield the same percentage. */
        if (!approxEquals(objectPct, intPct)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-object-int-agree] semantic mismatch: same numeric value through getPct(Object) and getPct(int) disagreed probe=" + probe + " objectPct=" + objectPct + " intPct=" + intPct);
        }
    }

    private static void assertApprox(String oracleId, double expected, double actual) {
        if (!approxEquals(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        double scale = Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= Math.max(TOLERANCE, 1e-12d * scale);
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [51] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [52] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,914 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        double actual;

        actual = f.getPct(1);
        if (Math.abs(actual - 0.25) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (Math.abs(actual - 0.25) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (Math.abs(actual - 0.5) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actual - 0.5) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (Math.abs(actual - 0.25) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actual - 0.50) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actual - 0.50) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integer-argument-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (Math.abs(actual - 1.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (Math.abs(actual - 1.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        try {
            Frequency g = new Frequency();
            int pivot = data.consumeInt(-1000000, 1000000);
            int mutations = data.consumeInt(1, 20);

            g.addValue(pivot);

            for (int i = 0; i < mutations; i++) {
                int v = data.consumeInt(-1000000, 1000000);
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        g.addValue(v);
                        break;
                    case 1:
                        g.addValue((long) v);
                        break;
                    case 2:
                        g.addValue(Integer.valueOf(v));
                        break;
                    case 3:
                        g.addValue((Comparable<?>) Long.valueOf(v));
                        break;
                    default:
                        g.addValue((Object) Integer.valueOf(v));
                        break;
                }
            }

            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            String stringBefore = g.toString();

            double lhs = g.getPct((Object) Integer.valueOf(pivot));
            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            String stringAfter = g.toString();
            double rhs = g.getPct(Long.valueOf(pivot));

            if (sumBefore != sumAfter || hashBefore != hashAfter || !stringBefore.equals(stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:read-only-getPct-object] metamorphic violation: getPct(Object) is a read-only query but observable state changed sumBefore=" +
                    sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " toStringBefore=" + stringBefore + " toStringAfter=" + stringAfter);
            }

            if (Math.abs(lhs - rhs) > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:object-vs-comparable-pct] metamorphic violation: deprecated getPct(Object) and getPct(Comparable) must agree for the same integral value because the class contract says integers/longs/Integer/Long are not distinguished by type and getPct(Object) is replaced by getPct(Comparable); input=" +
                    pivot + " lhs=" + lhs + " rhs=" + rhs + " sumFreq=" + g.getSumFreq() +
                    " countAsInt=" + g.getCount(Integer.valueOf(pivot)) +
                    " countAsLong=" + g.getCount(Long.valueOf(pivot)));
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [53] ⚙️ harness-repair · `attempt_006`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [54] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,365 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        long sumBeforeRead = f.getSumFreq();
        int hashBeforeRead = f.hashCode();
        String toStringBeforeRead = f.toString();

        double onePct = f.getPct(1);
        if (!approxEq(onePct, 0.25, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-pct] semantic mismatch: expected=0.25 actual=" + onePct);
        }

        double twoPct = f.getPct(Long.valueOf(2));
        if (!approxEq(twoPct, 0.25, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-pct] semantic mismatch: expected=0.25 actual=" + twoPct);
        }

        double threePct = f.getPct(threeL);
        if (!approxEq(threePct, 0.5, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-pct] semantic mismatch: expected=0.5 actual=" + threePct);
        }

        double threeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (!approxEq(threeObjectPct, 0.5, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=" + threeObjectPct);
        }

        double fivePct = f.getPct(5);
        if (!approxEq(fivePct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-pct] semantic mismatch: expected=0.0 actual=" + fivePct);
        }

        double fooPct = f.getPct("foo");
        if (!approxEq(fooPct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-pct] semantic mismatch: expected=0.0 actual=" + fooPct);
        }

        double oneCumPct = f.getCumPct(1);
        if (!approxEq(oneCumPct, 0.25, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-cum-pct] semantic mismatch: expected=0.25 actual=" + oneCumPct);
        }

        double twoCumPct = f.getCumPct(Long.valueOf(2));
        if (!approxEq(twoCumPct, 0.50, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-cum-pct] semantic mismatch: expected=0.5 actual=" + twoCumPct);
        }

        double integerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (!approxEq(integerArgCumPct, 0.50, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-integer-argument] semantic mismatch: expected=0.5 actual=" + integerArgCumPct);
        }

        double threeCumPct = f.getCumPct(threeL);
        if (!approxEq(threeCumPct, 1.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-cum-pct] semantic mismatch: expected=1.0 actual=" + threeCumPct);
        }

        double fiveCumPct = f.getCumPct(5);
        if (!approxEq(fiveCumPct, 1.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-cum-pct] semantic mismatch: expected=1.0 actual=" + fiveCumPct);
        }

        double zeroCumPct = f.getCumPct(0);
        if (!approxEq(zeroCumPct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + zeroCumPct);
        }

        double fooCumPct = f.getCumPct("foo");
        if (!approxEq(fooCumPct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + fooCumPct);
        }

        // Read-only post-condition: getPct/getCumPct are query methods over the frequency table.
        // A throw-deleting or wrong-bookkeeping patch that mutates hidden state would violate these cheap observable readers.
        long sumAfterRead = f.getSumFreq();
        int hashAfterRead = f.hashCode();
        String toStringAfterRead = f.toString();
        if (sumBeforeRead != sumAfterRead || hashBeforeRead != hashAfterRead || !safeEquals(toStringBeforeRead, toStringAfterRead)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:read-only-state] semantic mismatch: get* query mutated state sumBefore=" + sumBeforeRead +
                " sumAfter=" + sumAfterRead +
                " hashBefore=" + hashBeforeRead +
                " hashAfter=" + hashAfterRead +
                " toStringBefore=" + toStringBeforeRead +
                " toStringAfter=" + toStringAfterRead);
        }

        Frequency f1;
        int n1;
        Integer probe1;
        double actual1;
        double expected1;
        long count1;
        long sum1;
        try {
            f1 = new Frequency();
            n1 = data.consumeInt(1, 8);
            for (int i = 0; i < n1; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f1.addValue(v);
                        break;
                    case 1:
                        f1.addValue((long) v);
                        break;
                    case 2:
                        f1.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f1.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe1 = Integer.valueOf(data.consumeInt(-5, 5));
            count1 = f1.getCount((Object) probe1);
            sum1 = f1.getSumFreq();
            expected1 = (double) count1 / (double) sum1;
        } catch (Throwable e) {
            return;
        }
        try {
            actual1 = f1.getPct((Object) probe1);
        } catch (Throwable e) {
            return;
        }
        if (!approxEq(actual1, expected1, 1e-9 * Math.max(1.0, Math.max(Math.abs(actual1), Math.abs(expected1))))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" +
                actual1 + " expected=" + expected1 + " count=" + count1 + " sum=" + sum1 + " probe=" + probe1);
        }

        Frequency f2;
        int n2;
        Integer probe2;
        double objectPct;
        double comparablePct;
        try {
            f2 = new Frequency();
            n2 = data.consumeInt(1, 8);
            for (int i = 0; i < n2; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f2.addValue(v);
                } else {
                    f2.addValue((long) v);
                }
            }
            probe2 = Integer.valueOf(data.consumeInt(-6, 6));
        } catch (Throwable e) {
            return;
        }
        try {
            objectPct = f2.getPct((Object) probe2);
            comparablePct = f2.getPct((Comparable<?>) probe2);
        } catch (Throwable e) {
            return;
        }
        if (!approxEq(objectPct, comparablePct, 1e-9 * Math.max(1.0, Math.max(Math.abs(objectPct), Math.abs(comparablePct))))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:object-vs-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" +
                objectPct + " comparable=" + comparablePct + " probe=" + probe2);
        }

        Frequency f3;
        Integer canonical;
        double pctInt;
        double pctLong;
        long countInt;
        long countLong;
        try {
            f3 = new Frequency();
            int n3 = data.consumeInt(1, 8);
            for (int i = 0; i < n3; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f3.addValue(Integer.valueOf(v));
                } else {
                    f3.addValue(Long.valueOf(v));
                }
            }
            canonical = Integer.valueOf(data.consumeInt(-6, 6));
            pctInt = f3.getPct((Object) canonical);
            pctLong = f3.getPct((Object) Long.valueOf(canonical.longValue()));
            countInt = f3.getCount((Object) canonical);
            countLong = f3.getCount((Object) Long.valueOf(canonical.longValue()));
        } catch (Throwable e) {
            return;
        }
        // Equivalent-input post-condition from Frequency's integer-cross-type contract:
        // integer values are not distinguished by type for get* methods, so Integer(x) and Long(x) must agree.
        if (countInt != countLong || !approxEq(pctInt, pctLong, 1e-9 * Math.max(1.0, Math.max(Math.abs(pctInt), Math.abs(pctLong))))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integer-cross-type] metamorphic violation: Integer/Long equivalent inputs disagree probe=" + canonical +
                " countInt=" + countInt + " countLong=" + countLong +
                " pctInt=" + pctInt + " pctLong=" + pctLong);
        }
    }

    private static boolean approxEq(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,008 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        // Faithful reconstruction of FrequencyTest.testPcts()
        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertClose("[oracle:seed-one-pct]", 0.25, f.getPct(1), tolerance, "one pct");
        assertClose("[oracle:seed-two-pct]", 0.25, f.getPct(Long.valueOf(2)), tolerance, "two pct");
        assertClose("[oracle:seed-three-pct]", 0.5, f.getPct(Long.valueOf(3)), tolerance, "three pct");
        assertClose("[oracle:seed-three-object-pct]", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "three (Object) pct");
        assertClose("[oracle:seed-five-pct]", 0.0, f.getPct(5), tolerance, "five pct");
        assertClose("[oracle:seed-foo-pct]", 0.0, f.getPct("foo"), tolerance, "foo pct");
        assertClose("[oracle:seed-one-cum-pct]", 0.25, f.getCumPct(1), tolerance, "one cum pct");
        assertClose("[oracle:seed-two-cum-pct]", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "two cum pct");
        assertClose("[oracle:seed-integer-argument-cum-pct]", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "Integer argument");
        assertClose("[oracle:seed-three-cum-pct]", 1.0, f.getCumPct(Long.valueOf(3)), tolerance, "three cum pct");
        assertClose("[oracle:seed-five-cum-pct]", 1.0, f.getCumPct(5), tolerance, "five cum pct");
        assertClose("[oracle:seed-zero-cum-pct]", 0.0, f.getCumPct(0), tolerance, "zero cum pct");
        assertClose("[oracle:seed-foo-cum-pct]", 0.0, f.getCumPct("foo"), tolerance, "foo cum pct");

        // Documented guarantee: getPct(Object) is deprecated and "replaced by getPct(Comparable)".
        // For Comparable inputs, both overloads must therefore agree on the same value.
        // Also, get* methods are read-only accessors, so calling getPct(Object) must not change observable state.
        probePctOverloadAgreementAndReadOnlyState(f, Integer.valueOf(3), "[oracle:seed-overload-agreement]");

        // Generalization on fuzzed states using only trusted relations.
        Frequency g = new Frequency();
        boolean useStrings = data.consumeBoolean();
        int steps = data.consumeInt(1, 8);

        for (int i = 0; i < steps; i++) {
            if (useStrings) {
                String v = data.consumeAsciiString(8);
                if (v.length() == 0) {
                    v = "a";
                }
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                String q = data.consumeAsciiString(8);
                if (q.length() == 0) {
                    q = "b";
                }
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-string-step-" + i + "]");
                } catch (Throwable t) {
                    return;
                }
            } else {
                Integer v = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-int-step-" + i + "]");
                } catch (Throwable t) {
                    return;
                }
            }
        }

        if (data.consumeBoolean()) {
            g.clear();
            try {
                if (useStrings) {
                    String q = data.consumeAsciiString(8);
                    if (q.length() == 0) {
                        q = "c";
                    }
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-after-clear]");
                } else {
                    Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-after-clear]");
                }
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static void probePctOverloadAgreementAndReadOnlyState(Frequency freq, Comparable<?> query, String oracleId) {
        long beforeSum = freq.getSumFreq();
        int beforeHash = freq.hashCode();
        String beforeString = freq.toString();

        double lhs = freq.getPct((Object) query);

        long afterSum = freq.getSumFreq();
        int afterHash = freq.hashCode();
        String afterString = freq.toString();

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new RuntimeException(
                oracleId + " metamorphic violation: getPct(Object) changed read-only state"
                    + " query=" + String.valueOf(query)
                    + " beforeSum=" + beforeSum
                    + " afterSum=" + afterSum
                    + " beforeHash=" + beforeHash
                    + " afterHash=" + afterHash
                    + " beforeToString=" + escape(beforeString)
                    + " afterToString=" + escape(afterString));
        }

        double rhs = freq.getPct(query);
        if (!sameDouble(lhs, rhs, 10E-15)) {
            throw new RuntimeException(
                oracleId + " metamorphic violation: getPct(Object) != getPct(Comparable)"
                    + " query=" + String.valueOf(query)
                    + " lhs=" + lhs
                    + " rhs=" + rhs);
        }
    }

    private static void assertClose(String oracleId, double expected, double actual, double tolerance, String label) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                oracleId + " semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 120 has no oracle ID (`throw new RuntimeException( oracleId + " metamorphic violation: getPct(Object) changed read-only state" + " 

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,750 chars, 2 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        // Faithful reconstruction of FrequencyTest.testPcts()
        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertClose("[oracle:seed-one-pct]", 0.25, f.getPct(1), tolerance, "one pct");
        assertClose("[oracle:seed-two-pct]", 0.25, f.getPct(Long.valueOf(2)), tolerance, "two pct");
        assertClose("[oracle:seed-three-pct]", 0.5, f.getPct(Long.valueOf(3)), tolerance, "three pct");
        assertClose("[oracle:seed-three-object-pct]", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "three (Object) pct");
        assertClose("[oracle:seed-five-pct]", 0.0, f.getPct(5), tolerance, "five pct");
        assertClose("[oracle:seed-foo-pct]", 0.0, f.getPct("foo"), tolerance, "foo pct");
        assertClose("[oracle:seed-one-cum-pct]", 0.25, f.getCumPct(1), tolerance, "one cum pct");
        assertClose("[oracle:seed-two-cum-pct]", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "two cum pct");
        assertClose("[oracle:seed-integer-argument-cum-pct]", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "Integer argument");
        assertClose("[oracle:seed-three-cum-pct]", 1.0, f.getCumPct(Long.valueOf(3)), tolerance, "three cum pct");
        assertClose("[oracle:seed-five-cum-pct]", 1.0, f.getCumPct(5), tolerance, "five cum pct");
        assertClose("[oracle:seed-zero-cum-pct]", 0.0, f.getCumPct(0), tolerance, "zero cum pct");
        assertClose("[oracle:seed-foo-cum-pct]", 0.0, f.getCumPct("foo"), tolerance, "foo cum pct");

        // Documented guarantee: getPct(Object) is deprecated and "replaced by getPct(Comparable)".
        // For Comparable inputs, both overloads must therefore agree on the same value.
        // Also, get* methods are read-only accessors, so calling getPct(Object) must not change observable state.
        probePctOverloadAgreementAndReadOnlyState(f, Integer.valueOf(3), "[oracle:seed-overload-agreement]");

        // Generalization on fuzzed states using only trusted relations.
        Frequency g = new Frequency();
        boolean useStrings = data.consumeBoolean();
        int steps = data.consumeInt(1, 8);

        for (int i = 0; i < steps; i++) {
            if (useStrings) {
                String v = data.consumeAsciiString(8);
                if (v.length() == 0) {
                    v = "a";
                }
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                String q = data.consumeAsciiString(8);
                if (q.length() == 0) {
                    q = "b";
                }
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-string-step-" + i + "]");
                } catch (Throwable t) {
                    return;
                }
            } else {
                Integer v = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-int-step-" + i + "]");
                } catch (Throwable t) {
                    return;
                }
            }
        }

        if (data.consumeBoolean()) {
            g.clear();
            try {
                if (useStrings) {
                    String q = data.consumeAsciiString(8);
                    if (q.length() == 0) {
                        q = "c";
                    }
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-after-clear]");
                } else {
                    Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    probePctOverloadAgreementAndReadOnlyState(g, q, "[oracle:fuzz-after-clear]");
                }
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static void probePctOverloadAgreementAndReadOnlyState(Frequency freq, Comparable<?> query, String oracleId) {
        long beforeSum = freq.getSumFreq();
        int beforeHash = freq.hashCode();
        String beforeString = freq.toString();

        double lhs = freq.getPct((Object) query);

        long afterSum = freq.getSumFreq();
        int afterHash = freq.hashCode();
        String afterString = freq.toString();

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new RuntimeException(
                oracleId + " metamorphic violation: getPct(Object) changed read-only state"
                    + " query=" + String.valueOf(query)
                    + " beforeSum=" + beforeSum
                    + " afterSum=" + afterSum
                    + " beforeHash=" + beforeHash
                    + " afterHash=" + afterHash
                    + " beforeToString=" + escape(beforeString)
                    + " afterToString=" + escape(afterString));
        }

        double rhs = freq.getPct(query);
        if (!sameDouble(lhs, rhs, 10E-15)) {
            throw new RuntimeException(
                oracleId + " metamorphic violation: getPct(Object) != getPct(Comparable)"
                    + " query=" + String.valueOf(query)
                    + " lhs=" + lhs
                    + " rhs=" + rhs);
        }
    }

    private static void assertClose(String oracleId, double expected, double actual, double tolerance, String label) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                oracleId + " semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 120 has no oracle ID (`throw new RuntimeException( oracleId + " metamorphic violation: getPct(Object) changed read-only state" + " query=" + St...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,728 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertClose("seed-one-pct", 0.25, f.getPct(1), tolerance, "one pct");
        assertClose("seed-two-pct", 0.25, f.getPct(Long.valueOf(2)), tolerance, "two pct");
        assertClose("seed-three-pct", 0.5, f.getPct(Long.valueOf(3)), tolerance, "three pct");
        assertClose("seed-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "three (Object) pct");
        assertClose("seed-five-pct", 0.0, f.getPct(5), tolerance, "five pct");
        assertClose("seed-foo-pct", 0.0, f.getPct("foo"), tolerance, "foo pct");
        assertClose("seed-one-cum-pct", 0.25, f.getCumPct(1), tolerance, "one cum pct");
        assertClose("seed-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "two cum pct");
        assertClose("seed-integer-argument-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "Integer argument");
        assertClose("seed-three-cum-pct", 1.0, f.getCumPct(Long.valueOf(3)), tolerance, "three cum pct");
        assertClose("seed-five-cum-pct", 1.0, f.getCumPct(5), tolerance, "five cum pct");
        assertClose("seed-zero-cum-pct", 0.0, f.getCumPct(0), tolerance, "zero cum pct");
        assertClose("seed-foo-cum-pct", 0.0, f.getCumPct("foo"), tolerance, "foo cum pct");

        // Documented guarantee: getPct(Object) is deprecated and replaced by getPct(Comparable),
        // so for Comparable inputs both overloads must agree. Also get* methods are read-only
        // accessors, so calling getPct(Object) must not change observable state.
        probePctOverloadAgreementAndReadOnlyState(f, Integer.valueOf(3), "seed-overload");

        Frequency g = new Frequency();
        boolean useStrings = data.consumeBoolean();
        int steps = data.consumeInt(1, 8);

        for (int i = 0; i < steps; i++) {
            if (useStrings) {
                String v = data.consumeAsciiString(8);
                if (v.length() == 0) {
                    v = "a";
                }
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                String q = data.consumeAsciiString(8);
                if (q.length() == 0) {
                    q = "b";
                }
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-string-step-" + i);
                } catch (Throwable t) {
                    return;
                }
            } else {
                Integer v = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-int-step-" + i);
                } catch (Throwable t) {
                    return;
                }
            }
        }

        if (data.consumeBoolean()) {
            g.clear();
            try {
                if (useStrings) {
                    String q = data.consumeAsciiString(8);
                    if (q.length() == 0) {
                        q = "c";
                    }
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-after-clear");
                } else {
                    Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-after-clear");
                }
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static void probePctOverloadAgreementAndReadOnlyState(Frequency freq, Comparable<?> query, String oracleBase) {
        long beforeSum = freq.getSumFreq();
        int beforeHash = freq.hashCode();
        String beforeString = freq.toString();

        double lhs = freq.getPct((Object) query);

        long afterSum = freq.getSumFreq();
        int afterHash = freq.hashCode();
        String afterString = freq.toString();

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new RuntimeException(
                "[oracle:" + oracleBase + "-readonly] metamorphic violation: getPct(Object) changed read-only state"
                    + " query=" + String.valueOf(query)
                    + " beforeSum=" + beforeSum
                    + " afterSum=" + afterSum
                    + " beforeHash=" + beforeHash
                    + " afterHash=" + afterHash
                    + " beforeToString=" + escape(beforeString)
                    + " afterToString=" + escape(afterString));
        }

        double rhs = freq.getPct(query);
        if (!sameDouble(lhs, rhs, 10E-15)) {
            throw new RuntimeException(
                "[oracle:" + oracleBase + "-agreement] metamorphic violation: getPct(Object) != getPct(Comparable)"
                    + " query=" + String.valueOf(query)
                    + " lhs=" + lhs
                    + " rhs=" + rhs);
        }
    }

    private static void assertClose(String oracleId, double expected, double actual, double tolerance, String label) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,013 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkHiddenStateOnReadOnlyCall();
        checkRelationCountOverSum(data);
        checkRelationObjectComparableAgreement(data);
        checkAdditionalSiblingAgreement(data);
    }

    private static void checkLiftedTestOracles() {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;
        double tolerance = 10E-15;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1), tolerance);
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)), tolerance);
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL), tolerance);
        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance);
        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5), tolerance);
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"), tolerance);
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1), tolerance);
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance);
        assertDoubleEquals("lifted-integer-argument-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance);
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL), tolerance);
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5), tolerance);
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0), tolerance);
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"), tolerance);
    }

    private static void checkHiddenStateOnReadOnlyCall() {
        Frequency f = new Frequency();
        f.addValue(1L);
        f.addValue(2L);
        f.addValue(1);
        f.addValue(2);
        f.addValue(3L);
        f.addValue(3L);
        f.addValue(3);
        f.addValue(Integer.valueOf(3));

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String toStringBefore = f.toString();

        double actual = f.getPct((Object) Integer.valueOf(3));

        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String toStringAfter = f.toString();

        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(toStringBefore, toStringAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-readonly] semantic mismatch: getPct(Object) is a read-only query, so getSumFreq/hashCode/toString must remain unchanged; pct=" + actual + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " toStringBefore=" + escapeOneLine(toStringBefore) + " toStringAfter=" + escapeOneLine(toStringAfter));
        }
    }

    private static void checkRelationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double actual;
        double expected;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            if (sum <= 0) {
                return;
            }
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }

        if (!approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void checkRelationObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double objectPct;
        double comparablePct;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        if (!approxEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow("[oracle:object-vs-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe + " objectPct=" + objectPct + " comparablePct=" + comparablePct);
        }
    }

    private static void checkAdditionalSiblingAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double intPct;
        double longPct;
        long sumBefore;
        long sumAfter;
        int hashBefore;
        int hashAfter;
        String stringBefore;
        String stringAfter;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-7, 7);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = data.consumeInt(-7, 7);

            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            stringBefore = f.toString();

            intPct = f.getPct(probe);
            longPct = f.getPct(Long.valueOf(probe));

            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            stringAfter = f.toString();
        } catch (Throwable t) {
            return;
        }

        if (!approxEquals(intPct, longPct)) {
            throw new FuzzerSecurityIssueLow("[oracle:int-vs-long-sibling] metamorphic violation: equivalent integer inputs through getPct(int) and getPct(Long) must agree; probe=" + probe + " lhs=" + intPct + " rhs=" + longPct);
        }

        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-hidden-state] metamorphic violation: read-only getPct overloads must not mutate observable state; probe=" + probe + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " toStringBefore=" + escapeOneLine(stringBefore) + " toStringAfter=" + escapeOneLine(stringAfter));
        }
    }

    private static void assertDoubleEquals(String oracleId, String label, double expected, double actual, double tolerance) {
        if (!(Math.abs(expected - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual + " tolerance=" + tolerance);
        }
    }

    private static boolean approxEquals(double a, double b) {
        return Math.abs(a - b) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0 tolerance=1.0E-14

---
## [63] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,848 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-1", "after first mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-1", "after first mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(twoL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-2", "after second mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-2", "after second mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(oneI);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-3", "after third mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-3", "after third mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(twoI);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-4", "after fourth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-4", "after fourth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(threeL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-5", "after fifth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-5", "after fifth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(threeL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-6", "after sixth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-6", "after sixth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(3);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-7", "after seventh mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-7", "after seventh mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(threeI);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-8", "after eighth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-8", "after eighth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        assertApprox(0.25, f.getPct(1), TOLERANCE, "testPcts-one-pct", "one pct");
        assertApprox(0.25, f.getPct(Long.valueOf(2)), TOLERANCE, "testPcts-two-pct", "two pct");
        assertApprox(0.5, f.getPct(Long.valueOf(threeL)), TOLERANCE, "testPcts-three-pct", "three pct");
        assertApprox(0.5, f.getPct((Object) (Integer.valueOf(3))), TOLERANCE, "testPcts-three-object-pct", "three (Object) pct");
        assertApprox(0.0, f.getPct(5), TOLERANCE, "testPcts-five-pct", "five pct");
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "testPcts-foo-pct", "foo pct");
        assertApprox(0.25, f.getCumPct(1), TOLERANCE, "testPcts-one-cumpct", "one cum pct");
        assertApprox(0.50, f.getCumPct(Long.valueOf(2)), TOLERANCE, "testPcts-two-cumpct", "two cum pct");
        assertApprox(0.50, f.getCumPct(Integer.valueOf(2)), TOLERANCE, "testPcts-integer-argument", "Integer argument");
        assertApprox(1.0, f.getCumPct(Long.valueOf(threeL)), TOLERANCE, "testPcts-three-cumpct", "three cum pct");
        assertApprox(1.0, f.getCumPct(5), TOLERANCE, "testPcts-five-cumpct", "five cum pct");
        assertApprox(0.0, f.getCumPct(0), TOLERANCE, "testPcts-zero-cumpct", "zero cum pct");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "testPcts-foo-cumpct", "foo cum pct");

        try {
            Frequency g = new Frequency();
            int target = data.consumeInt(-1000, 1000);
            int other = data.consumeInt(-1000, 1000);
            if (other == target) {
                other = target + 1;
            }
            int targetCopies = data.consumeInt(1, 8);
            int otherCopies = data.consumeInt(1, 8);

            for (int i = 0; i < targetCopies; i++) {
                g.addValue(target);
            }
            for (int i = 0; i < otherCopies; i++) {
                g.addValue(other);
            }

            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            String stringBefore = g.toString();

            double objectPct = g.getPct((Object) Integer.valueOf(target));
            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            String stringAfter = g.toString();

            // getPct is documented as a read-only query ("Returns the percentage..."), so it must not mutate observable state.
            if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
                throw new RuntimeException("[oracle:read-only-getPct-object] metamorphic violation: read-only accessor changed receiver state target=" + target + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " toStringBefore=" + escape(stringBefore) + " toStringAfter=" + escape(stringAfter));
            }

            double comparablePct = g.getPct(Integer.valueOf(target));
            // Deprecated getPct(Object) is documented as replaced by getPct(Comparable); equivalent Integer inputs must agree.
            if (!approxEquals(objectPct, comparablePct, TOLERANCE)) {
                throw new RuntimeException("[oracle:object-vs-comparable-getPct] metamorphic violation: equivalent overloads disagreed target=" + target + " lhs=" + objectPct + " rhs=" + comparablePct + " sumFreq=" + g.getSumFreq() + " count=" + g.getCount(Integer.valueOf(target)));
            }

            double constructedExpected = ((double) g.getCount(Integer.valueOf(target))) / ((double) g.getSumFreq());
            // The javadoc for getPct says it returns "the proportion of values equal to v"; for a non-empty frequency built from known counts, that proportion is count/sumFreq.
            if (!approxEquals(comparablePct, constructedExpected, TOLERANCE)) {
                throw new RuntimeException("[oracle:pct-count-over-sum] metamorphic violation: getPct disagreed with getCount/sumFreq target=" + target + " lhs=" + comparablePct + " rhs=" + constructedExpected + " sumFreq=" + g.getSumFreq() + " count=" + g.getCount(Integer.valueOf(target)));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertApprox(double expected, double actual, double tolerance, String id, String what) {
        if (!approxEquals(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b, double tolerance) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
        }
        return Math.abs(a - b) <= tolerance;
        }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [64] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testPcts-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [65] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,641 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Exact reconstruction of FrequencyTest.testPcts setup and assertions.
        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        checkDouble("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        checkDouble("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        checkDouble("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));
        checkDouble("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        checkDouble("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        checkDouble("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        checkDouble("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        checkDouble("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        checkDouble("lifted-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        checkDouble("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        checkDouble("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        checkDouble("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        checkDouble("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // Hidden-state/read-only check:
        // getPct/getCumPct are query methods ("Returns ..."); they must not mutate observable state.
        // A patch that "fixes" the bug by changing bookkeeping or internal contents during a read would violate this.
        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();
        double ignored = f.getPct((Object) Integer.valueOf(3));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-readonly] semantic mismatch: read-only getPct(Object) changed observable state"
                    + " sumBefore=" + sumBefore
                    + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashAfter
                    + " toStringBefore=" + String.valueOf(stringBefore)
                    + " toStringAfter=" + String.valueOf(stringAfter));
        }

        relationGetPctObjectMatchesCountOverSumForIntegerObject(data);
        relationGetPctObjectAndComparableAgreeOnSameIntegerValue(data);

        // Additional trusted generalisation:
        // For any non-empty Frequency, getPct(v) = getCount(v)/getSumFreq by the documented meaning of percentage.
        // We construct the distribution ourselves from chosen integral values, so the expected answer is trusted.
        additionalConstructedOracle(data);
    }

    private static void additionalConstructedOracle(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        long count;
        long sum;
        double actual;
        double expected;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            count = f.getCount((Comparable<?>) probe);
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:constructed-count-over-sum] semantic mismatch: getPct(Comparable) disagrees with getCount/getSumFreq"
                    + " probe=" + probe
                    + " actual=" + actual
                    + " expected=" + expected
                    + " count=" + count
                    + " sum=" + sum);
        }
    }

    private static void relationGetPctObjectMatchesCountOverSumForIntegerObject(FuzzedDataProvider data) {
        Frequency f = null;
        int probe = 0;
        long count = 0L;
        long sum = 0L;
        double expected = 0.0;
        double actual = 0.0;
        boolean ready = false;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
            ready = true;
        } catch (Throwable t) {
            return;
        }
        if (ready && !closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: probe="
                    + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void relationGetPctObjectAndComparableAgreeOnSameIntegerValue(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        double a = 0.0;
        double b = 0.0;
        boolean ready = false;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            a = f.getPct((Object) probe);
            b = f.getPct((Comparable<?>) probe);
            ready = true;
        } catch (Throwable t) {
            return;
        }
        if (ready && !closeEnough(a, b)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe="
                    + probe + " object=" + a + " comparable=" + b);
        }
    }

    private static void checkDouble(String oracleId, String label, double expected, double actual) {
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean closeEnough(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
```

</details>

---
## [66] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [67] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,864 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        checkApprox("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        checkApprox("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        checkApprox("lifted-three-pct", "three pct", 0.5, f.getPct(Long.valueOf(3)));
        checkApprox("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) Integer.valueOf(3)));
        checkApprox("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        checkApprox("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        checkApprox("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        checkApprox("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        checkApprox("lifted-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        checkApprox("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(Long.valueOf(3)));
        checkApprox("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        checkApprox("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        checkApprox("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        int probe = data.consumeInt(-1000, 1000);
        int mutations = data.consumeInt(0, 16);

        for (int i = 0; i < mutations; i++) {
            int v = data.consumeInt(-1000, 1000);
            switch (data.consumeInt(0, 4)) {
                case 0:
                    f.addValue(v);
                    break;
                case 1:
                    f.addValue((long) v);
                    break;
                case 2:
                    f.addValue(Integer.valueOf(v));
                    break;
                case 3:
                    f.addValue((Object) Integer.valueOf(v));
                    break;
                default:
                    f.addValue(Long.valueOf(v));
                    break;
            }

            verifyDeprecatedObjectOverloadAgreementAndReadOnly(f, probe, "after-mutation-" + i);
            verifyDeprecatedCumPctAgreementAndReadOnly(f, probe, "after-mutation-" + i);
            verifyAbsentComparableRejectionInvariant(f, data.consumeAsciiString(8), "after-mutation-" + i);
        }

        verifyDeprecatedObjectOverloadAgreementAndReadOnly(f, probe, "final");
        verifyDeprecatedCumPctAgreementAndReadOnly(f, probe, "final");
        verifyAbsentComparableRejectionInvariant(f, data.consumeAsciiString(8), "final");

        Frequency g = new Frequency();
        int a = data.consumeInt(-1000, 1000);
        int b = data.consumeInt(-1000, 1000);
        int extra = data.consumeInt(0, 8);

        g.addValue(a);
        g.addValue(b);
        for (int i = 0; i < extra; i++) {
            g.addValue(data.consumeInt(-1000, 1000));
        }

        try {
            double lhs = g.getPct((Object) Integer.valueOf(a));
            double rhs = g.getPct(Integer.valueOf(a));
            if (!sameDouble(lhs, rhs)) {
                throw new RuntimeException("[oracle:equiv-object-comparable] metamorphic violation: deprecated getPct(Object) must agree with getPct(Comparable) for the same Comparable input input=" + a + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            double lhs = g.getCumPct((Object) Integer.valueOf(b));
            double rhs = g.getCumPct(Integer.valueOf(b));
            if (!sameDouble(lhs, rhs)) {
                throw new RuntimeException("[oracle:equiv-cumpct-object-comparable] metamorphic violation: deprecated getCumPct(Object) must agree with getCumPct(Comparable) for the same Comparable input input=" + b + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void verifyDeprecatedObjectOverloadAgreementAndReadOnly(Frequency f, int probe, String stage) {
        try {
            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            double lhs = f.getPct((Object) Integer.valueOf(probe));
            double rhs = f.getPct(Integer.valueOf(probe));

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new RuntimeException("[oracle:readonly-getpct] metamorphic violation: getPct is a read-only query and must not change observable state stage=" + stage + " beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + quote(beforeString) + " afterString=" + quote(afterString));
            }

            /* Contract basis:
             * - getPct(Object) is deprecated and replaced by getPct(Comparable), so for a Comparable input they must agree.
             * - getPct is documented as a query ("Returns the percentage..."), so it must not mutate observable state.
             * A patch that redirects to the cumulative method, or deletes/changes bookkeeping incorrectly, violates one or both checks.
             */
            if (!sameDouble(lhs, rhs)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:getpct-object-vs-comparable] semantic mismatch: deprecated getPct(Object) must equal getPct(Comparable) for the same Comparable input stage=" + stage + " input=" + probe + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            }
        }
    }

    private static void verifyDeprecatedCumPctAgreementAndReadOnly(Frequency f, int probe, String stage) {
        try {
            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            double lhs = f.getCumPct((Object) Integer.valueOf(probe));
            double rhs = f.getCumPct(Integer.valueOf(probe));

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new RuntimeException("[oracle:readonly-getcumpct] metamorphic violation: getCumPct is a read-only query and must not change observable state stage=" + stage + " beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + quote(beforeString) + " afterString=" + quote(afterString));
            }

            if (!sameDouble(lhs, rhs)) {
                throw new RuntimeException("[oracle:getcumpct-object-vs-comparable] metamorphic violation: deprecated getCumPct(Object) must equal getCumPct(Comparable) for the same Comparable input stage=" + stage + " input=" + probe + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                String m = t.getMessage();
                if (m != null && m.startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
    }

    private static void verifyAbsentComparableRejectionInvariant(Frequency f, String s, String stage) {
        try {
            String probe = s == null ? "" : s;
            double pct = f.getPct(probe);
            double cumPct = f.getCumPct(probe);

            /* Contract basis:
             * For a non-comparable-to-set query like String against an integral-valued Frequency:
             * - getPct returns 0 because getCount returns 0.
             * - getCumPct returns 0 if at least one value has been added but v is not comparable to the values set.
             * This must stay true after every state change because the set remains integral-only.
             */
            if (f.getSumFreq() > 0) {
                if (!sameDouble(pct, 0.0)) {
                    throw new RuntimeException("[oracle:absent-string-pct] metamorphic violation: non-comparable String probe against integral-only Frequency must have pct 0 stage=" + stage + " input=" + quote(probe) + " actual=" + pct);
                }
                if (!sameDouble(cumPct, 0.0)) {
                    throw new RuntimeException("[oracle:absent-string-cumpct] metamorphic violation: non-comparable String probe against integral-only Frequency must have cumPct 0 stage=" + stage + " input=" + quote(probe) + " actual=" + cumPct);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                String m = t.getMessage();
                if (m != null && m.startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
    }

    private static void checkApprox(String oracleId, String what, double expected, double actual) {
        if (!approxEquals(expected, actual)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double expected, double actual) {
        if (Double.isNaN(expected) && Double.isNaN(actual)) {
            return true;
        }
        return Math.abs(expected - actual) <= TOLERANCE;
    }

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b) || Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [68] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [69] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~13,329 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean eqDouble(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        // Faithful reconstruction of FrequencyTest.testPcts setup and all lifted assertions.
        try {
            long oneL = 1;
            long twoL = 2;
            long threeL = 3;
            int oneI = 1;
            int twoI = 2;
            int threeI = 3;
            Frequency f = new Frequency();

            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);

            // Read-only hidden-state check: getPct/getCumPct are query methods; they must not change
            // cheap observable state such as total frequency, hashCode, or string form.
            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String strBefore = f.toString();

            double actualOnePct = f.getPct(1);
            if (!eqDouble(actualOnePct, 0.25, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
            }

            double actualTwoPct = f.getPct(Long.valueOf(2));
            if (!eqDouble(actualTwoPct, 0.25, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
            }

            double actualThreePct = f.getPct(threeL);
            if (!eqDouble(actualThreePct, 0.5, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
            }

            double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
            if (!eqDouble(actualThreeObjectPct, 0.5, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
            }

            double actualFivePct = f.getPct(5);
            if (!eqDouble(actualFivePct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
            }

            double actualFooPct = f.getPct("foo");
            if (!eqDouble(actualFooPct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
            }

            double actualOneCumPct = f.getCumPct(1);
            if (!eqDouble(actualOneCumPct, 0.25, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
            }

            double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
            if (!eqDouble(actualTwoCumPct, 0.50, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
            }

            double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
            if (!eqDouble(actualIntegerArgCumPct, 0.50, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:integer-argument-cum-pct] semantic mismatch: expected=0.5 actual=" + actualIntegerArgCumPct);
            }

            double actualThreeCumPct = f.getCumPct(threeL);
            if (!eqDouble(actualThreeCumPct, 1.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
            }

            double actualFiveCumPct = f.getCumPct(5);
            if (!eqDouble(actualFiveCumPct, 1.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
            }

            double actualZeroCumPct = f.getCumPct(0);
            if (!eqDouble(actualZeroCumPct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
            }

            double actualFooCumPct = f.getCumPct("foo");
            if (!eqDouble(actualFooCumPct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
            }

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String strAfter = f.toString();
            if (sumBefore != sumAfter || hashBefore != hashAfter || (strBefore == null ? strAfter != null : !strBefore.equals(strAfter))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:read-only-state] semantic mismatch: getPct/getCumPct changed observable state sumBefore=" + sumBefore +
                    " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " strBefore=" + strBefore + " strAfter=" + strAfter);
            }

            // Sibling-agreement post-condition: deprecated getPct(Object) is documented replacement-compatible
            // with getPct(Comparable); equivalent Integer inputs must agree. A "fix" that merely dodges the buggy
            // branch or returns another lookup would violate this without throwing.
            double viaObject = f.getPct((Object) Integer.valueOf(3));
            double viaComparable = f.getPct((Comparable<?>) Integer.valueOf(3));
            if (!eqDouble(viaObject, viaComparable, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:object-vs-comparable-seed] metamorphic violation: getPct(Object)!=getPct(Comparable) value=3 object=" +
                    viaObject + " comparable=" + viaComparable);
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-setup] unexpected exception on valid reconstructed test input", e);
        }

        // Candidate relation 1: getPct(Object Integer) must equal getCount(Object Integer)/getSumFreq()
        // because getPct returns the proportion of values equal to v, and integer values are not distinguished by type.
        Frequency f1 = null;
        Integer probe1 = null;
        long count1 = 0;
        long sum1 = 0;
        double actual1 = 0.0;
        double expected1 = 0.0;
        boolean relation1Ready = false;
        try {
            f1 = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f1.addValue(v);
                        break;
                    case 1:
                        f1.addValue((long) v);
                        break;
                    case 2:
                        f1.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f1.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe1 = Integer.valueOf(data.consumeInt(-5, 5));
            count1 = f1.getCount((Object) probe1);
            sum1 = f1.getSumFreq();
            expected1 = (double) count1 / (double) sum1;
            actual1 = f1.getPct((Object) probe1);
            relation1Ready = true;
        } catch (Throwable e) {
            return;
        }
        if (relation1Ready) {
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(actual1), Math.abs(expected1)));
            if (!(Math.abs(actual1 - expected1) <= tol)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getPctObject_matches_count_over_sum_for_integer_object] relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" +
                    probe1 + " actual=" + actual1 + " expected=" + expected1 + " count=" + count1 + " sum=" + sum1);
            }
        }

        // Candidate relation 2: same logical Integer queried through getPct(Object) and getPct(Comparable)
        // must agree; both are real API calls over the same value space.
        Frequency f2 = null;
        Integer probe2 = null;
        double a2 = 0.0;
        double b2 = 0.0;
        boolean relation2Ready = false;
        try {
            f2 = new Frequency();
            int n2 = data.consumeInt(1, 8);
            for (int i = 0; i < n2; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f2.addValue(v);
                } else {
                    f2.addValue((long) v);
                }
            }
            probe2 = Integer.valueOf(data.consumeInt(-6, 6));
            a2 = f2.getPct((Object) probe2);
            b2 = f2.getPct((Comparable<?>) probe2);
            relation2Ready = true;
        } catch (Throwable e) {
            return;
        }
        if (relation2Ready) {
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a2), Math.abs(b2)));
            if (!(Math.abs(a2 - b2) <= tol)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getPct_object_and_comparable_agree_on_same_integer_value] relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" +
                    probe2 + " object=" + a2 + " comparable=" + b2);
            }
        }

        // Additional trusted generalisation: construct a non-empty integral distribution from fuzzed values,
        // then for a chosen probe getPct(int), getPct(long), getPct(Integer as Comparable), and getPct(Object Integer)
        // must all agree because the class contract says integer values are not distinguished by type.
        Frequency f3 = null;
        int probe3 = 0;
        double pInt = 0.0;
        double pLong = 0.0;
        double pComp = 0.0;
        double pObj = 0.0;
        long sumBefore3 = 0L;
        long sumAfter3 = 0L;
        boolean relation3Ready = false;
        try {
            f3 = new Frequency();
            int n3 = data.consumeInt(1, 10);
            for (int i = 0; i < n3; i++) {
                int v = data.consumeInt(-8, 8);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f3.addValue(v);
                        break;
                    default:
                        f3.addValue((long) v);
                        break;
                }
            }
            probe3 = data.consumeInt(-8, 8);
            sumBefore3 = f3.getSumFreq();
            pInt = f3.getPct(probe3);
            pLong = f3.getPct((long) probe3);
            pComp = f3.getPct((Comparable<?>) Integer.valueOf(probe3));
            pObj = f3.getPct((Object) Integer.valueOf(probe3));
            sumAfter3 = f3.getSumFreq();
            relation3Ready = true;
        } catch (Throwable e) {
            return;
        }
        if (relation3Ready) {
            double base = pInt;
            double tol3 = 1e-9 * Math.max(1.0, Math.max(Math.abs(base), Math.max(Math.abs(pLong), Math.max(Math.abs(pComp), Math.abs(pObj)))));
            if (!(Math.abs(pInt - pLong) <= tol3 && Math.abs(pInt - pComp) <= tol3 && Math.abs(pInt - pObj) <= tol3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:integer-overloads-agree] metamorphic violation: probe=" + probe3 +
                    " int=" + pInt + " long=" + pLong + " comparable=" + pComp + " object=" + pObj);
            }
            if (sumBefore3 != sumAfter3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:query-does-not-mutate-sum] metamorphic violation: sum changed across getPct overload queries probe=" +
                    probe3 + " before=" + sumBefore3 + " after=" + sumAfter3);
            }
        }
    }
}
```

</details>

---
## [70] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [71] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,997 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertApprox("pcts-one", 0.25, f.getPct(1), "f.getPct(1)");
        assertApprox("pcts-two", 0.25, f.getPct(Long.valueOf(2)), "f.getPct(Long.valueOf(2))");
        assertApprox("pcts-three", 0.5, f.getPct(Long.valueOf(threeL)), "f.getPct(threeL)");
        assertApprox("pcts-three-object", 0.5, f.getPct((Object) Integer.valueOf(3)), "f.getPct((Object) Integer.valueOf(3))");
        assertApprox("pcts-five", 0.0, f.getPct(5), "f.getPct(5)");
        assertApprox("pcts-foo", 0.0, f.getPct("foo"), "f.getPct(\"foo\")");
        assertApprox("cumpct-one", 0.25, f.getCumPct(1), "f.getCumPct(1)");
        assertApprox("cumpct-two", 0.50, f.getCumPct(Long.valueOf(2)), "f.getCumPct(Long.valueOf(2))");
        assertApprox("cumpct-two-integer", 0.50, f.getCumPct(Integer.valueOf(2)), "f.getCumPct(Integer.valueOf(2))");
        assertApprox("cumpct-three", 1.0, f.getCumPct(Long.valueOf(threeL)), "f.getCumPct(threeL)");
        assertApprox("cumpct-five", 1.0, f.getCumPct(5), "f.getCumPct(5)");
        assertApprox("cumpct-zero", 0.0, f.getCumPct(0), "f.getCumPct(0)");
        assertApprox("cumpct-foo", 0.0, f.getCumPct("foo"), "f.getCumPct(\"foo\")");

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String strBefore = f.toString();
        double objectPct = f.getPct((Object) Integer.valueOf(3));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String strAfter = f.toString();

        if (sumBefore != sumAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-sum] semantic mismatch: getPct(Object) changed getSumFreq before=" + sumBefore + " after=" + sumAfter);
        }
        if (hashBefore != hashAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-hash] semantic mismatch: getPct(Object) changed hashCode before=" + hashBefore + " after=" + hashAfter);
        }
        if ((strBefore == null && strAfter != null) || (strBefore != null && !strBefore.equals(strAfter))) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-string] semantic mismatch: getPct(Object) changed toString before=" + String.valueOf(strBefore) + " after=" + String.valueOf(strAfter));
        }

        // Documented guarantee: getPct returns "the proportion of values that are equal to v".
        // For non-empty frequency tables this observable post-condition is getCount(v) / getSumFreq().
        double expectedFromCounts = (double) f.getCount(Integer.valueOf(3)) / (double) f.getSumFreq();
        if (!approxEquals(objectPct, expectedFromCounts)) {
            throw new RuntimeException("[oracle:pct-count-ratio] metamorphic violation: getPct(Object 3) must equal getCount(3)/getSumFreq() input=3 lhs=" + objectPct + " rhs=" + expectedFromCounts);
        }

        // Documented guarantee: deprecated getPct(Object) is replaced by getPct(Comparable),
        // so equivalent Integer inputs must produce the same result through both real overloads.
        double comparablePct = f.getPct((Comparable<?>) Integer.valueOf(3));
        if (!approxEquals(objectPct, comparablePct)) {
            throw new RuntimeException("[oracle:object-vs-comparable] metamorphic violation: equivalent getPct overloads disagree input=3 lhs=" + objectPct + " rhs=" + comparablePct);
        }

        try {
            int probe = data.consumeInt(-1000, 1000);
            int lowerCount = data.consumeInt(1, 5);
            int equalCount = data.consumeInt(1, 5);
            int higherCount = data.consumeInt(1, 5);

            Frequency g = new Frequency();

            for (int i = 0; i < lowerCount; i++) {
                g.addValue((long) (probe - 1));
            }
            for (int i = 0; i < equalCount; i++) {
                if ((i & 1) == 0) {
                    g.addValue(probe);
                } else {
                    g.addValue(Integer.valueOf(probe));
                }
            }
            for (int i = 0; i < higherCount; i++) {
                g.addValue((Object) Integer.valueOf(probe + 1));
            }

            double lhs = g.getPct((Object) Integer.valueOf(probe));
            double rhs = g.getPct((Comparable<?>) Integer.valueOf(probe));
            if (!approxEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:fuzz-overload-agreement] metamorphic violation: equivalent getPct overloads disagree input=" + probe + " lhs=" + lhs + " rhs=" + rhs);
            }

            double ratio = (double) g.getCount(Integer.valueOf(probe)) / (double) g.getSumFreq();
            if (!approxEquals(lhs, ratio)) {
                throw new RuntimeException("[oracle:fuzz-pct-ratio] metamorphic violation: getPct(Object) must equal getCount/getSumFreq input=" + probe + " lhs=" + lhs + " rhs=" + ratio);
            }

            long gSumBefore = g.getSumFreq();
            int gHashBefore = g.hashCode();
            String gStrBefore = g.toString();
            g.getPct((Object) Integer.valueOf(probe));
            long gSumAfter = g.getSumFreq();
            int gHashAfter = g.hashCode();
            String gStrAfter = g.toString();
            if (gSumBefore != gSumAfter || gHashBefore != gHashAfter || (gStrBefore == null ? gStrAfter != null : !gStrBefore.equals(gStrAfter))) {
                throw new RuntimeException("[oracle:fuzz-readonly] metamorphic violation: getPct(Object) must be read-only input=" + probe + " sumBefore=" + gSumBefore + " sumAfter=" + gSumAfter + " hashBefore=" + gHashBefore + " hashAfter=" + gHashAfter + " strBefore=" + String.valueOf(gStrBefore) + " strAfter=" + String.valueOf(gStrAfter));
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void assertApprox(String oracleId, double expected, double actual, String what) {
        if (!approxEquals(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
        }
        return Math.abs(a - b) <= TOLERANCE;
    }
}
```

</details>

---
## [72] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:pcts-three-object] semantic mismatch: f.getPct((Object) Integer.valueOf(3)) expected=0.5 actual=1.0

---
## [73] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,117 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct
            );
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct
            );
        }

        double actualThreePct = f.getPct(threeL);
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct
            );
        }

        long sumBeforeObjectPct = f.getSumFreq();
        int hashBeforeObjectPct = f.hashCode();
        String stringBeforeObjectPct = f.toString();
        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        long sumAfterObjectPct = f.getSumFreq();
        int hashAfterObjectPct = f.hashCode();
        String stringAfterObjectPct = f.toString();

        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct
            );
        }

        // getPct is a read-only query ("Returns the percentage..."), so cheap observable state must not change.
        // A patch that redirects to a mutating path or corrupts bookkeeping would violate this post-condition.
        if (sumBeforeObjectPct != sumAfterObjectPct
                || hashBeforeObjectPct != hashAfterObjectPct
                || (stringBeforeObjectPct == null ? stringAfterObjectPct != null : !stringBeforeObjectPct.equals(stringAfterObjectPct))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:hidden-state-getPctObject] semantic mismatch: getPct(Object) changed state"
                    + " sumBefore=" + sumBeforeObjectPct
                    + " sumAfter=" + sumAfterObjectPct
                    + " hashBefore=" + hashBeforeObjectPct
                    + " hashAfter=" + hashAfterObjectPct
                    + " toStringBefore=" + String.valueOf(stringBeforeObjectPct)
                    + " toStringAfter=" + String.valueOf(stringAfterObjectPct)
            );
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct
            );
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct
            );
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct
            );
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct
            );
        }

        double actualIntegerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgumentCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-integer-argument] semantic mismatch: expected=0.5 actual=" + actualIntegerArgumentCumPct
            );
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct
            );
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct
            );
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct
            );
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct
            );
        }

        {
            Frequency fuzzFreq = null;
            Integer probe = null;
            Double lhs = null;
            Double rhs = null;
            boolean skip = false;
            Exception caught = null;
            try {
                fuzzFreq = new Frequency();
                int n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-6, 6);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            fuzzFreq.addValue(v);
                            break;
                        case 1:
                            fuzzFreq.addValue((long) v);
                            break;
                        case 2:
                            fuzzFreq.addValue(Integer.valueOf(v));
                            break;
                        default:
                            fuzzFreq.addValue(Long.valueOf(v));
                            break;
                    }
                }
                probe = Integer.valueOf(data.consumeInt(-6, 6));
                lhs = Double.valueOf(fuzzFreq.getPct((Object) probe));
                rhs = Double.valueOf(fuzzFreq.getPct((Comparable<?>) probe));
            } catch (Exception e) {
                skip = true;
                caught = e;
            }
            if (!skip) {
                // Deprecated getPct(Object) is documented as the same logical query as getPct(Comparable),
                // and integer values are not distinguished by type in Frequency; equivalent Integer inputs must agree.
                double a = lhs.doubleValue();
                double b = rhs.doubleValue();
                double relTol = 1e-9 * Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > relTol) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a + " comparable=" + b + " probe=" + probe
                    );
                }
            } else if (caught != null) {
                return;
            }
        }

        {
            Frequency fuzzFreq = null;
            Integer probe = null;
            Long count = null;
            Long sum = null;
            Double actual = null;
            Double expected = null;
            boolean skip = false;
            Exception caught = null;
            try {
                fuzzFreq = new Frequency();
                int n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-5, 5);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            fuzzFreq.addValue(v);
                            break;
                        case 1:
                            fuzzFreq.addValue((long) v);
                            break;
                        case 2:
                            fuzzFreq.addValue(Integer.valueOf(v));
                            break;
                        default:
                            fuzzFreq.addValue(Long.valueOf(v));
                            break;
                    }
                }
                probe = Integer.valueOf(data.consumeInt(-5, 5));
                count = Long.valueOf(fuzzFreq.getCount((Object) probe));
                sum = Long.valueOf(fuzzFreq.getSumFreq());
                expected = Double.valueOf((double) count.longValue() / (double) sum.longValue());
                actual = Double.valueOf(fuzzFreq.getPct((Object) probe));
            } catch (Exception e) {
                skip = true;
                caught = e;
            }
            if (!skip) {
                // Contract: getPct(v) returns the percentage of values equal to v; on a non-empty Frequency this is getCount(v)/getSumFreq().
                // This catches silent wrong-answer patches even when no exception is thrown.
                double a = actual.doubleValue();
                double e = expected.doubleValue();
                double relTol = 1e-9 * Math.max(1.0d, Math.max(Math.abs(a), Math.abs(e)));
                if (Math.abs(a - e) > relTol) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + a
                            + " expected=" + e
                            + " count=" + count.longValue()
                            + " sum=" + sum.longValue()
                            + " probe=" + probe
                    );
                }
            } else if (caught != null) {
                return;
            }
        }
    }
}
```

</details>

---
## [74] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [75] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,582 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        double actual;

        actual = f.getPct(1);
        if (!(Math.abs(actual - 0.25d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (!(Math.abs(actual - 0.25d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (!(Math.abs(actual - 0.5d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) Integer.valueOf(3));
        if (!(Math.abs(actual - 0.5d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (!(Math.abs(actual - 0.25d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-one-cumpct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (!(Math.abs(actual - 0.50d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-two-cumpct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (!(Math.abs(actual - 0.50d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-integer-cumpct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (!(Math.abs(actual - 1.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-three-cumpct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (!(Math.abs(actual - 1.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-five-cumpct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-zero-cumpct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-foo-cumpct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        Frequency g = new Frequency();
        int mutations = data.consumeInt(1, 8);
        for (int i = 0; i < mutations; i++) {
            int v = data.consumeInt(-1000000, 1000000);

            switch (data.consumeInt(0, 3)) {
                case 0:
                    g.addValue(v);
                    break;
                case 1:
                    g.addValue((long) v);
                    break;
                case 2:
                    g.addValue(Integer.valueOf(v));
                    break;
                default:
                    g.addValue((Comparable<?>) Long.valueOf(v));
                    break;
            }

            try {
                long beforeSum = g.getSumFreq();
                int beforeHash = g.hashCode();
                String beforeString = g.toString();

                Object probe = Integer.valueOf(v);
                double lhs = g.getPct(probe);

                long afterSum = g.getSumFreq();
                int afterHash = g.hashCode();
                String afterString = g.toString();

                if (beforeSum != afterSum || beforeHash != afterHash || !beforeString.equals(afterString)) {
                    throw new RuntimeException(
                        "[oracle:readonly-getpct-object] metamorphic violation: getPct(Object) is documented as a read-only query, but observable state changed input="
                            + v + " sumBefore=" + beforeSum + " sumAfter=" + afterSum
                            + " hashBefore=" + beforeHash + " hashAfter=" + afterHash
                            + " toStringBefore=" + beforeString.replace("\n", "\\n").replace("\t", "\\t")
                            + " toStringAfter=" + afterString.replace("\n", "\\n").replace("\t", "\\t"));
                }

                double rhsOverload = g.getPct((Comparable<?>) Long.valueOf(v));
                if (Double.isNaN(lhs) != Double.isNaN(rhsOverload)
                        || (!Double.isNaN(lhs) && Math.abs(lhs - rhsOverload) > tolerance)) {
                    throw new RuntimeException(
                        "[oracle:object-vs-comparable-overload] metamorphic violation: getPct(Object) and getPct(Comparable) have the same documented meaning for comparable inputs input="
                            + v + " lhs=" + lhs + " rhs=" + rhsOverload);
                }

                long sum = g.getSumFreq();
                if (sum > 0) {
                    double rhsFormula = (double) g.getCount((Comparable<?>) Long.valueOf(v)) / (double) sum;
                    if (Double.isNaN(lhs) != Double.isNaN(rhsFormula)
                            || (!Double.isNaN(lhs) && Math.abs(lhs - rhsFormula) > tolerance)) {
                        throw new RuntimeException(
                            "[oracle:pct-formula] metamorphic violation: getPct is documented as the proportion of values equal to v, so it must equal getCount(v)/getSumFreq() input="
                                + v + " lhs=" + lhs + " rhs=" + rhsFormula + " count="
                                + g.getCount((Comparable<?>) Long.valueOf(v)) + " sum=" + sum);
                    }
                }
            } catch (Throwable t) {
                return;
            }
        }
    }
}
```

</details>

---
## [76] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,012 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TEST_TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency exact = new Frequency();

        exact.addValue(1L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(2L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(1);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(2);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(3L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(3L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(3);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(Integer.valueOf(3));
        reprobeAbsentNumericAfterMutation(exact);

        checkApprox("lifted-one-pct", "one pct", 0.25, exact.getPct(1), TEST_TOLERANCE);
        checkApprox("lifted-two-pct", "two pct", 0.25, exact.getPct(Long.valueOf(2)), TEST_TOLERANCE);
        checkApprox("lifted-three-pct", "three pct", 0.5, exact.getPct(3L), TEST_TOLERANCE);

        long beforeSum = exact.getSumFreq();
        int beforeHash = exact.hashCode();
        String beforeString = exact.toString();
        double objectThreePct = exact.getPct((Object) (Integer.valueOf(3)));
        long afterSum = exact.getSumFreq();
        int afterHash = exact.hashCode();
        String afterString = exact.toString();
        checkApprox("lifted-three-object-pct", "three (Object) pct", 0.5, objectThreePct, TEST_TOLERANCE);
        // Frequency#getPct is a read-only query ("Returns the percentage..."), so observable state readers must not change.
        // A throw-deleting or miswired patch that mutates bookkeeping while answering would violate this post-condition.
        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-getPctObject] semantic mismatch: read-only getPct(Object) changed state beforeSum="
                    + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                    + " beforeToString=" + beforeString + " afterToString=" + afterString);
        }

        checkApprox("lifted-five-pct", "five pct", 0.0, exact.getPct(5), TEST_TOLERANCE);
        checkApprox("lifted-foo-pct", "foo pct", 0.0, exact.getPct("foo"), TEST_TOLERANCE);
        checkApprox("lifted-one-cum-pct", "one cum pct", 0.25, exact.getCumPct(1), TEST_TOLERANCE);
        checkApprox("lifted-two-cum-pct", "two cum pct", 0.50, exact.getCumPct(Long.valueOf(2)), TEST_TOLERANCE);
        checkApprox("lifted-integer-arg-cum-pct", "Integer argument", 0.50, exact.getCumPct(Integer.valueOf(2)), TEST_TOLERANCE);
        checkApprox("lifted-three-cum-pct", "three cum pct", 1.0, exact.getCumPct(3L), TEST_TOLERANCE);
        checkApprox("lifted-five-cum-pct", "five cum pct", 1.0, exact.getCumPct(5), TEST_TOLERANCE);
        checkApprox("lifted-zero-cum-pct", "zero cum pct", 0.0, exact.getCumPct(0), TEST_TOLERANCE);
        checkApprox("lifted-foo-cum-pct", "foo cum pct", 0.0, exact.getCumPct("foo"), TEST_TOLERANCE);

        relationGetPctMatchesCountOverSum(data);
        relationObjectAndComparableAgree(data);
        relationCrossTypeIntegerAndLongAgree(data);
        relationReadOnlyGettersDoNotMutate(data);
    }

    private static void reprobeAbsentNumericAfterMutation(Frequency f) {
        checkApprox("reprobe-five-pct", "five pct after mutation", 0.0, f.getPct(5), TEST_TOLERANCE);
        checkApprox("reprobe-five-cum-pct", "five cum pct after mutation", 1.0, f.getCumPct(5), TEST_TOLERANCE);
    }

    private static void relationGetPctMatchesCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double actual;
        double expected;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }
        if (!approxEquals(actual, expected, 1e-9)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe
                    + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void relationObjectAndComparableAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double objectResult;
        double comparableResult;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectResult = f.getPct((Object) probe);
            comparableResult = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }
        if (!approxEquals(objectResult, comparableResult, 1e-9)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe
                    + " object=" + objectResult + " comparable=" + comparableResult);
        }
    }

    private static void relationCrossTypeIntegerAndLongAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double integerPct;
        double longPct;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-8, 8);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = data.consumeInt(-8, 8);
            // The Frequency class docs state integer values are not distinguished by type for getPct/getCumPct/getCount.
            integerPct = f.getPct((Object) Integer.valueOf(probe));
            longPct = f.getPct(Long.valueOf(probe));
        } catch (Throwable t) {
            return;
        }
        if (!approxEquals(integerPct, longPct, 1e-9)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:cross-type-pct] metamorphic violation: equivalent integer and long inputs must have same percentage probe="
                    + probe + " integerPct=" + integerPct + " longPct=" + longPct);
        }
    }

    private static void relationReadOnlyGettersDoNotMutate(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        long sumBefore;
        int hashBefore;
        String stringBefore;
        double pctObject;
        double pctComparable;
        double cumPctObject;
        long sumAfter;
        int hashAfter;
        String stringAfter;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-4, 4);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f.addValue(v);
                        break;
                    default:
                        f.addValue((long) v);
                        break;
                }
            }
            probe = Integer.valueOf(data.consumeInt(-4, 4));
            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            stringBefore = f.toString();
            pctObject = f.getPct((Object) probe);
            pctComparable = f.getPct((Comparable<?>) probe);
            cumPctObject = f.getCumPct((Object) probe);
            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            stringAfter = f.toString();
            if (Double.isNaN(pctObject) || Double.isNaN(pctComparable) || Double.isNaN(cumPctObject)) {
                return;
            }
        } catch (Throwable t) {
            return;
        }
        // get* methods are queries over existing counts; they are documented as returning frequencies/percentages, not mutating.
        // Therefore these reads must leave cheap observable state unchanged.
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:read-only-getters] metamorphic violation: read-only getters changed receiver state probe=" + probe
                    + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " stringBefore=" + stringBefore + " stringAfter=" + stringAfter);
        }
    }

    private static void checkApprox(String oracleId, String what, double expected, double actual, double tolerance) {
        if (!approxEquals(actual, expected, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b, double tolerance) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= tolerance * scale;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,073 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        // Reconstruct FrequencyTest.testPcts exactly.
        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        checkDouble("lift-one-pct", "one pct", 0.25, f.getPct(1), tolerance);
        checkDouble("lift-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)), tolerance);
        checkDouble("lift-three-pct", "three pct", 0.5, f.getPct(threeL), tolerance);
        checkDouble("lift-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance);
        checkDouble("lift-five-pct", "five pct", 0.0, f.getPct(5), tolerance);
        checkDouble("lift-foo-pct", "foo pct", 0.0, f.getPct("foo"), tolerance);
        checkDouble("lift-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1), tolerance);
        checkDouble("lift-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance);
        checkDouble("lift-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance);
        checkDouble("lift-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL), tolerance);
        checkDouble("lift-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5), tolerance);
        checkDouble("lift-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0), tolerance);
        checkDouble("lift-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"), tolerance);

        // Post-condition / metamorphic check:
        // The docs for getPct(Object) say it is "replaced by getPct(Comparable)"; these overloads
        // must therefore agree on equivalent inputs. A patch that redirects Object to cumulative
        // percentage violates this even when nothing throws.
        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();
        double pctObj3 = f.getPct((Object) Integer.valueOf(3));
        double pctCmp3 = f.getPct((Comparable<?>) Integer.valueOf(3));
        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterString = f.toString();
        if (!sameDouble(pctObj3, pctCmp3, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:overload-agreement] semantic mismatch: getPct(Object) != getPct(Comparable) input=3 lhs=" +
                pctObj3 + " rhs=" + pctCmp3);
        }
        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:read-only] semantic mismatch: read-only getPct changed state beforeSum=" + beforeSum +
                " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash +
                " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString));
        }

        // Generalize with trusted relations on fuzz-constructed inputs.
        Frequency g = new Frequency();
        int mutations = data.consumeInt(1, 32);
        for (int i = 0; i < mutations; i++) {
            int choice = data.consumeInt(0, 2);
            int v = data.consumeInt(-1000, 1000);
            if (choice == 0) {
                g.addValue(v);
            } else if (choice == 1) {
                g.addValue((long) v);
            } else {
                char c = (char) data.consumeInt(0, 127);
                g.addValue(c);
            }
        }

        try {
            // Construct input from known answer: choose a value first, add it exactly k times, then
            // its percentage must be k / sum by the documented definition of getPct.
            Frequency h = new Frequency();
            int target = data.consumeInt(-1000, 1000);
            int targetCount = data.consumeInt(1, 8);
            int otherCount = data.consumeInt(0, 8);
            for (int i = 0; i < targetCount; i++) {
                h.addValue(target);
            }
            for (int i = 0; i < otherCount; i++) {
                int other = data.consumeInt(-1000, 1000);
                if (other == target) {
                    other = target + 1;
                }
                h.addValue(other);
            }
            double expectedPct = (double) targetCount / (double) h.getSumFreq();
            double actualPctComparable = h.getPct((Comparable<?>) Integer.valueOf(target));
            double actualPctObject = h.getPct((Object) Integer.valueOf(target));
            checkDouble("count-derived-comparable", "getPct(Comparable) from constructed counts", expectedPct, actualPctComparable, tolerance);
            checkDouble("count-derived-object", "getPct(Object) from constructed counts", expectedPct, actualPctObject, tolerance);

            // Same-name overload agreement for integral values: integers/longs/Integer/Long are not
            // distinguished by type per class contract, so equivalent representations must agree.
            double pctInt = h.getPct(target);
            double pctLong = h.getPct((long) target);
            if (!sameDouble(pctInt, actualPctComparable, tolerance) || !sameDouble(pctLong, actualPctComparable, tolerance)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:integral-equivalence] semantic mismatch: equivalent integral pct overloads differ target=" +
                    target + " pctInt=" + pctInt + " pctLong=" + pctLong + " pctComparable=" + actualPctComparable);
            }
        } catch (RuntimeException ignored) {
            // If a relation setup or probe unexpectedly throws, skip per harness hygiene.
        }
    }

    private static void checkDouble(String oracleId, String what, double expected, double actual, double tolerance) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [80] ⚙️ harness-repair · `attempt_019`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [81] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lift-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [82] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,140 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();
        double objectPct = f.getPct((Object) (Integer.valueOf(3)));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();

        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, objectPct);
        // Contract justification: getPct is a query ("Returns the percentage..."), and getSumFreq/hashCode/toString are observable readers.
        // A throw-deleting or wrong-routing patch that mutates internal state during a read-only query would violate this post-condition.
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state] semantic mismatch: getPct(Object) changed read-only observable state sumBefore=" + sumBefore
                    + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashAfter
                    + " toStringBefore=" + escape(stringBefore)
                    + " toStringAfter=" + escape(stringAfter)
            );
        }

        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-integer-arg-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // Extra trusted metamorphic/oracle: the class contract says integer values are not distinguished by type.
        // This reproduces the fixture through the real API and checks getPct(Object Integer) against count/sum on the same Frequency.
        long countThree = f.getCount((Object) Integer.valueOf(3));
        long sum = f.getSumFreq();
        if (sum > 0) {
            double expectedFromCount = (double) countThree / (double) sum;
            double actualFromObject = f.getPct((Object) Integer.valueOf(3));
            if (!closeEnough(actualFromObject, expectedFromCount)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:count-over-sum-seed] semantic mismatch: getPct(Object Integer.valueOf(3))="
                        + actualFromObject + " expected=" + expectedFromCount + " count=" + countThree + " sum=" + sum
                );
            }
        }

        relationGetPctObjectMatchesCountOverSumForIntegerObject(data);
        relationGetPctObjectAndComparableAgreeOnSameIntegerValue(data);
    }

    private static void relationGetPctObjectMatchesCountOverSumForIntegerObject(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double actual;
        double expected;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Exception e) {
            return;
        }

        // Contract justification: getPct(Object) returns the percentage equal to v; on any non-empty Frequency this equals getCount(v)/getSumFreq().
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: actual="
                    + actual + " expected=" + expected + " count=" + count + " sum=" + sum + " probe=" + probe
            );
        }
    }

    private static void relationGetPctObjectAndComparableAgreeOnSameIntegerValue(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double a;
        double b;
        long sumBeforeObject;
        long sumAfterObject;
        int hashBeforeObject;
        int hashAfterObject;
        String stringBeforeObject;
        String stringAfterObject;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));

            sumBeforeObject = f.getSumFreq();
            hashBeforeObject = f.hashCode();
            stringBeforeObject = f.toString();
            a = f.getPct((Object) probe);
            sumAfterObject = f.getSumFreq();
            hashAfterObject = f.hashCode();
            stringAfterObject = f.toString();

            b = f.getPct((Comparable<?>) probe);
        } catch (Exception e) {
            return;
        }

        // Contract justification: deprecated getPct(Object) and getPct(Comparable) are sibling overloads for the same logical query.
        if (!closeEnough(a, b)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a
                    + " comparable=" + b + " probe=" + probe
            );
        }

        // Hidden-state check on the fuzzed path as well: read-only query must not alter other public readers.
        if (sumBeforeObject != sumAfterObject
            || hashBeforeObject != hashAfterObject
            || !safeEquals(stringBeforeObject, stringAfterObject)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: hidden-state mutation"
                    + " sumBefore=" + sumBeforeObject
                    + " sumAfter=" + sumAfterObject
                    + " hashBefore=" + hashBeforeObject
                    + " hashAfter=" + hashAfterObject
                    + " toStringBefore=" + escape(stringBeforeObject)
                    + " toStringAfter=" + escape(stringAfterObject)
            );
        }
    }

    private static void assertDoubleEquals(String oracleId, String label, double expected, double actual) {
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static boolean closeEnough(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [83] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [84] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,135 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final long oneL = 1L;
        final long twoL = 2L;
        final long threeL = 3L;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

        Frequency f = new Frequency();

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertApprox("lifted-one-pct", "one pct", 0.25d, f.getPct(1));
        assertApprox("lifted-two-pct", "two pct", 0.25d, f.getPct(Long.valueOf(2)));
        assertApprox("lifted-three-pct", "three pct", 0.5d, f.getPct(threeL));
        assertApprox("lifted-three-object-pct", "three (Object) pct", 0.5d, f.getPct((Object) (Integer.valueOf(3))));
        assertApprox("lifted-five-pct", "five pct", 0.0d, f.getPct(5));
        assertApprox("lifted-foo-pct", "foo pct", 0.0d, f.getPct("foo"));
        assertApprox("lifted-one-cum-pct", "one cum pct", 0.25d, f.getCumPct(1));
        assertApprox("lifted-two-cum-pct", "two cum pct", 0.50d, f.getCumPct(Long.valueOf(2)));
        assertApprox("lifted-integer-arg-cum-pct", "Integer argument", 0.50d, f.getCumPct(Integer.valueOf(2)));
        assertApprox("lifted-three-cum-pct", "three cum pct", 1.0d, f.getCumPct(threeL));
        assertApprox("lifted-five-cum-pct", "five cum pct", 1.0d, f.getCumPct(5));
        assertApprox("lifted-zero-cum-pct", "zero cum pct", 0.0d, f.getCumPct(0));
        assertApprox("lifted-foo-cum-pct", "foo cum pct", 0.0d, f.getCumPct("foo"));

        int maxSeen = 3;
        int extraAdds = data.consumeInt(0, 16);
        for (int i = 0; i < extraAdds; i++) {
            int v = data.consumeInt(-1000, 1000);
            f.addValue(v);
            if (v > maxSeen) {
                maxSeen = v;
            }

            try {
                Integer probePresent = Integer.valueOf(v);
                Integer probeAbsent = Integer.valueOf(maxSeen == 1000 ? -1000 : maxSeen + 1);

                long beforeSum = f.getSumFreq();
                int beforeHash = f.hashCode();
                String beforeString = f.toString();

                double objectPctPresent = f.getPct((Object) probePresent);
                double comparablePctPresent = f.getPct(probePresent);

                long afterSum = f.getSumFreq();
                int afterHash = f.hashCode();
                String afterString = f.toString();

                if (!approxEquals(objectPctPresent, comparablePctPresent)) {
                    throw new RuntimeException("[oracle:object-vs-comparable-present] metamorphic violation: deprecated getPct(Object) is documented as replaced by getPct(Comparable), so equivalent Integer inputs must yield the same percentage input=" + probePresent + " lhs=" + objectPctPresent + " rhs=" + comparablePctPresent);
                }

                if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                    throw new RuntimeException("[oracle:read-only-query] metamorphic violation: getPct is a read-only getter and must not change observable state input=" + probePresent + " beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeToString=" + escape(beforeString) + " afterToString=" + escape(afterString));
                }

                double objectPctAbsent = f.getPct((Object) probeAbsent);
                double comparablePctAbsent = f.getPct(probeAbsent);

                if (!approxEquals(objectPctAbsent, comparablePctAbsent)) {
                    throw new RuntimeException("[oracle:object-vs-comparable-absent] metamorphic violation: deprecated getPct(Object) is documented as replaced by getPct(Comparable), so equivalent absent Integer inputs must yield the same percentage input=" + probeAbsent + " lhs=" + objectPctAbsent + " rhs=" + comparablePctAbsent);
                }

                /* Contract justification:
                 * getCount(v) returns 0 for absent values; getPct(Comparable) returns getCount(v)/getSumFreq when non-empty.
                 * Therefore, for a non-empty Frequency and an absent integral probe, the percentage must be exactly 0.
                 * A patch that redirects getPct(Object) to cumulative percentage breaks this and can return 1.0 for values above the maximum.
                 */
                if (!approxEquals(0.0d, comparablePctAbsent) || !approxEquals(0.0d, objectPctAbsent)) {
                    throw new RuntimeException("[oracle:absent-value-zero-pct] metamorphic violation: absent value must have zero percentage in a non-empty frequency input=" + probeAbsent + " objectPct=" + objectPctAbsent + " comparablePct=" + comparablePctAbsent + " sumFreq=" + f.getSumFreq());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static void assertApprox(String oracleId, String label, double expected, double actual) {
        if (!approxEquals(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [85] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [86] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,409 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestPctsOracles();
        generalizedCountOverSumRelation(data);
        generalizedObjectComparableAgreement(data);
    }

    private static void liftedTestPctsOracles() {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));

        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeToString = f.toString();
        double objectPct = f.getPct((Object) (Integer.valueOf(3)));
        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterToString = f.toString();

        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, objectPct);
        if (beforeSum != afterSum) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-sum] semantic mismatch: getPct(Object) is a read-only query, so getSumFreq must not change; before=" + beforeSum + " after=" + afterSum);
        }
        if (beforeHash != afterHash) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-hash] semantic mismatch: getPct(Object) is a read-only query, so hashCode must not change; before=" + beforeHash + " after=" + afterHash);
        }
        if (!safeEquals(beforeToString, afterToString)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-string] semantic mismatch: getPct(Object) is a read-only query, so toString must not change; before=" + escapeOneLine(beforeToString) + " after=" + escapeOneLine(afterToString));
        }

        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-integer-argument-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // The deprecated Object overload is documented as the same percentage query as the Comparable overload.
        // A patch that merely avoids the original wrong path but returns another wrong value would violate this sibling agreement.
        double comparablePct = f.getPct((Comparable<?>) Integer.valueOf(3));
        if (!approxEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-object-vs-comparable] metamorphic violation: getPct(Object) and getPct(Comparable) must agree for the same Integer value input=3 lhs=" + objectPct + " rhs=" + comparablePct);
        }

        // For a non-empty Frequency, percentage is count divided by total frequency by the documented meaning of getPct.
        long countThree = f.getCount((Object) Integer.valueOf(3));
        long sum = f.getSumFreq();
        double expectedFromCounts = (double) countThree / (double) sum;
        if (!approxEquals(objectPct, expectedFromCounts)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-over-sum] metamorphic violation: getPct(Object) must equal getCount(v)/getSumFreq() for input=3 lhs=" + objectPct + " rhs=" + expectedFromCounts + " count=" + countThree + " sum=" + sum);
        }
    }

    private static void generalizedCountOverSumRelation(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        long count = 0L;
        long sum = 0L;
        double actual = 0.0;
        double expected = 0.0;
        boolean ready = false;
        Exception caught = null;

        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }

            probe = Integer.valueOf(data.consumeInt(-5, 5));

            // get* methods are read-only queries; capture cheap observable state before/after to detect hidden mutation.
            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String stringBefore = f.toString();

            count = f.getCount((Object) probe);
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) probe);

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String stringAfter = f.toString();
            if (sumBefore != sumAfter) {
                throw new FuzzerSecurityIssueLow("[oracle:general-hidden-state-sum] semantic mismatch: read-only getPct(Object) changed getSumFreq before=" + sumBefore + " after=" + sumAfter);
            }
            if (hashBefore != hashAfter) {
                throw new FuzzerSecurityIssueLow("[oracle:general-hidden-state-hash] semantic mismatch: read-only getPct(Object) changed hashCode before=" + hashBefore + " after=" + hashAfter);
            }
            if (!safeEquals(stringBefore, stringAfter)) {
                throw new FuzzerSecurityIssueLow("[oracle:general-hidden-state-string] semantic mismatch: read-only getPct(Object) changed toString before=" + escapeOneLine(stringBefore) + " after=" + escapeOneLine(stringAfter));
            }

            ready = true;
        } catch (Exception e) {
            caught = e;
        }

        if (caught != null) {
            return;
        }
        if (ready && !approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void generalizedObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        double objectResult = 0.0;
        double comparableResult = 0.0;
        boolean ready = false;
        Exception caught = null;

        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectResult = f.getPct((Object) probe);
            comparableResult = f.getPct((Comparable<?>) probe);
            ready = true;
        } catch (Exception e) {
            caught = e;
        }

        if (caught != null) {
            return;
        }
        if (ready && !approxEquals(objectResult, comparableResult)) {
            throw new FuzzerSecurityIssueLow("relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe + " object=" + objectResult + " comparable=" + comparableResult);
        }
    }

    private static void assertDoubleEquals(String oracleId, String what, double expected, double actual) {
        if (!approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [87] ⚙️ harness-repair · `attempt_022`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [88] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,937 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        final long oneL = 1L;
        final long twoL = 2L;
        final long threeL = 3L;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;
        final double tolerance = 10E-15;

        try {
            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);
        } catch (RuntimeException e) {
            return;
        }

        checkDouble("lifted-one-pct", "one pct", 0.25d, safeGetPctInt(f, 1), tolerance);
        checkDouble("lifted-two-pct", "two pct", 0.25d, safeGetPctComparable(f, Long.valueOf(2)), tolerance);
        checkDouble("lifted-three-pct", "three pct", 0.5d, safeGetPctComparable(f, Long.valueOf(3)), tolerance);
        checkDouble("lifted-three-object-pct", "three (Object) pct", 0.5d, safeGetPctObject(f, Integer.valueOf(3)), tolerance);
        checkDouble("lifted-five-pct", "five pct", 0.0d, safeGetPctInt(f, 5), tolerance);
        checkDouble("lifted-foo-pct", "foo pct", 0.0d, safeGetPctObject(f, "foo"), tolerance);
        checkDouble("lifted-one-cum-pct", "one cum pct", 0.25d, safeGetCumPctInt(f, 1), tolerance);
        checkDouble("lifted-two-cum-pct", "two cum pct", 0.50d, safeGetCumPctComparable(f, Long.valueOf(2)), tolerance);
        checkDouble("lifted-int-arg-cum-pct", "Integer argument", 0.50d, safeGetCumPctObject(f, Integer.valueOf(2)), tolerance);
        checkDouble("lifted-three-cum-pct", "three cum pct", 1.0d, safeGetCumPctComparable(f, Long.valueOf(3)), tolerance);
        checkDouble("lifted-five-cum-pct", "five cum pct", 1.0d, safeGetCumPctInt(f, 5), tolerance);
        checkDouble("lifted-zero-cum-pct", "zero cum pct", 0.0d, safeGetCumPctInt(f, 0), tolerance);
        checkDouble("lifted-foo-cum-pct", "foo cum pct", 0.0d, safeGetCumPctObject(f, "foo"), tolerance);

        int extraMutations = data.consumeInt(0, 16);
        for (int i = 0; i < extraMutations; i++) {
            int kind = data.consumeInt(0, 4);
            try {
                if (kind == 0) {
                    f.addValue(data.consumeInt(-1000000, 1000000));
                } else if (kind == 1) {
                    f.addValue((long) data.consumeInt(-1000000, 1000000));
                } else if (kind == 2) {
                    f.addValue(Integer.valueOf(data.consumeInt(-1000000, 1000000)));
                } else if (kind == 3) {
                    f.addValue(Long.valueOf((long) data.consumeInt(-1000000, 1000000)));
                } else {
                    f.addValue((Object) Integer.valueOf(data.consumeInt(-1000000, 1000000)));
                }
            } catch (RuntimeException e) {
                return;
            }

            // Documented contract: integral values are not distinguished by type, and the deprecated
            // getPct(Object) is replaced by getPct(Comparable). Therefore for any integral probe v,
            // getPct((Object) Integer.valueOf(v)), getPct(Integer.valueOf(v)), and getPct(v) must agree.
            // A patch that silently routes the Object overload to cumulative percentage violates this.
            int probe = data.consumeInt(-1000000, 1000000);
            try {
                long sumBefore = f.getSumFreq();
                int hashBefore = f.hashCode();
                String stringBefore = f.toString();

                double pctObject = f.getPct((Object) Integer.valueOf(probe));
                double pctComparable = f.getPct(Integer.valueOf(probe));
                double pctPrimitive = f.getPct(probe);

                long sumAfter = f.getSumFreq();
                int hashAfter = f.hashCode();
                String stringAfter = f.toString();

                if (sumBefore != sumAfter) {
                    throw new RuntimeException("[oracle:readonly-sumfreq] metamorphic violation: getPct changed getSumFreq probe=" + probe + " before=" + sumBefore + " after=" + sumAfter);
                }
                if (hashBefore != hashAfter) {
                    throw new RuntimeException("[oracle:readonly-hash] metamorphic violation: getPct changed hashCode probe=" + probe + " before=" + hashBefore + " after=" + hashAfter);
                }
                if (!safeEquals(stringBefore, stringAfter)) {
                    throw new RuntimeException("[oracle:readonly-tostring] metamorphic violation: getPct changed toString probe=" + probe + " before=" + escape(stringBefore) + " after=" + escape(stringAfter));
                }

                if (!sameDouble(pctObject, pctComparable, tolerance)) {
                    throw new RuntimeException("[oracle:object-comparable-pct] metamorphic violation: getPct((Object)Integer) must equal getPct(Integer) for integral values probe=" + probe + " lhs=" + pctObject + " rhs=" + pctComparable);
                }
                if (!sameDouble(pctObject, pctPrimitive, tolerance)) {
                    throw new RuntimeException("[oracle:object-primitive-pct] metamorphic violation: getPct((Object)Integer) must equal getPct(int) for integral values probe=" + probe + " lhs=" + pctObject + " rhs=" + pctPrimitive);
                }

                double cumObject = f.getCumPct((Object) Integer.valueOf(probe));
                double cumComparable = f.getCumPct(Integer.valueOf(probe));
                double cumPrimitive = f.getCumPct(probe);

                if (!sameDouble(cumObject, cumComparable, tolerance)) {
                    throw new RuntimeException("[oracle:object-comparable-cumpct] metamorphic violation: getCumPct((Object)Integer) must equal getCumPct(Integer) for integral values probe=" + probe + " lhs=" + cumObject + " rhs=" + cumComparable);
                }
                if (!sameDouble(cumObject, cumPrimitive, tolerance)) {
                    throw new RuntimeException("[oracle:object-primitive-cumpct] metamorphic violation: getCumPct((Object)Integer) must equal getCumPct(int) for integral values probe=" + probe + " lhs=" + cumObject + " rhs=" + cumPrimitive);
                }
            } catch (RuntimeException e) {
                if (e instanceof FuzzerSecurityIssueLow) {
                    throw e;
                }
                if (hasOraclePrefix(e)) {
                    throw e;
                }
                return;
            }
        }

        try {
            int known = data.consumeInt(-1000000, 1000000);
            Frequency g = new Frequency();
            int copies = data.consumeInt(1, 8);
            int distractors = data.consumeInt(0, 8);
            for (int i = 0; i < copies; i++) {
                g.addValue(known);
            }
            for (int i = 0; i < distractors; i++) {
                int other = known;
                while (other == known) {
                    other = data.consumeInt(-1000000, 1000000);
                }
                g.addValue(other);
            }

            // Oracle from construction: we chose exactly how many copies of `known` were inserted,
            // so getCount/getSumFreq fixes the expected percentage without any guessed reference implementation.
            double expectedPct = (double) g.getCount(known) / (double) g.getSumFreq();
            double actualObjPct = g.getPct((Object) Integer.valueOf(known));
            double actualCmpPct = g.getPct(Integer.valueOf(known));

            if (!sameDouble(actualCmpPct, expectedPct, tolerance)) {
                throw new RuntimeException("[oracle:constructed-comparable-pct] metamorphic violation: constructed frequency percentage mismatch input=" + known + " expected=" + expectedPct + " actual=" + actualCmpPct);
            }
            if (!sameDouble(actualObjPct, expectedPct, tolerance)) {
                throw new RuntimeException("[oracle:constructed-object-pct] metamorphic violation: constructed frequency percentage mismatch input=" + known + " expected=" + expectedPct + " actual=" + actualObjPct);
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            if (hasOraclePrefix(e)) {
                throw e;
            }
        }
    }

    private static double safeGetPctObject(Frequency f, Object v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPctComparable(Frequency f, Comparable<?> v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPctInt(Frequency f, int v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctObject(Frequency f, Object v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctComparable(Frequency f, Comparable<?> v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctInt(Frequency f, int v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static void checkDouble(String oracleId, String label, double expected, double actual, double tolerance) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        if (a == b) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean hasOraclePrefix(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,044 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();
        relationCountOverSum(data);
        relationObjectComparableAgree(data);
        hiddenStateReadOnlyCheck(data);
        extraSiblingAgreement(data);
    }

    private static void liftedTestOracles() {
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;
        Frequency f = new Frequency();

        try {
            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);
        } catch (Throwable t) {
            return;
        }

        checkLiftedDouble("one-pct", "f.getPct(1)", 0.25, safeGetPctInt(f, 1));
        checkLiftedDouble("two-pct", "f.getPct(Long.valueOf(2))", 0.25, safeGetPctLongObject(f, Long.valueOf(2)));
        checkLiftedDouble("three-pct", "f.getPct(threeL)", 0.5, safeGetPctLongPrimitive(f, threeL));
        checkLiftedDouble("three-object-pct", "f.getPct((Object) Integer.valueOf(3))", 0.5, safeGetPctObject(f, Integer.valueOf(3)));
        checkLiftedDouble("five-pct", "f.getPct(5)", 0.0, safeGetPctInt(f, 5));
        checkLiftedDouble("foo-pct", "f.getPct(\"foo\")", 0.0, safeGetPctObject(f, "foo"));
        checkLiftedDouble("one-cum-pct", "f.getCumPct(1)", 0.25, safeGetCumPctInt(f, 1));
        checkLiftedDouble("two-cum-pct", "f.getCumPct(Long.valueOf(2))", 0.50, safeGetCumPctLongObject(f, Long.valueOf(2)));
        checkLiftedDouble("integer-argument", "f.getCumPct(Integer.valueOf(2))", 0.50, safeGetCumPctObject(f, Integer.valueOf(2)));
        checkLiftedDouble("three-cum-pct", "f.getCumPct(threeL)", 1.0, safeGetCumPctLongPrimitive(f, threeL));
        checkLiftedDouble("five-cum-pct", "f.getCumPct(5)", 1.0, safeGetCumPctInt(f, 5));
        checkLiftedDouble("zero-cum-pct", "f.getCumPct(0)", 0.0, safeGetCumPctInt(f, 0));
        checkLiftedDouble("foo-cum-pct", "f.getCumPct(\"foo\")", 0.0, safeGetCumPctObject(f, "foo"));

        // Documented guarantee used as post-condition: getPct(Object) "Returns the percentage of values that are equal to v"
        // and getCumPct(Object) is the cumulative percentage. Therefore for the same built fixture,
        // getPct((Object) Integer.valueOf(3)) must be the exact per-value percentage, not the cumulative one.
        // A patch that simply redirects to the cumulative path violates this observable contract without throwing.
        double actualPctObj = safeGetPctObject(f, Integer.valueOf(3));
        double expectedFromCount = ((double) f.getCount((Comparable<?>) Integer.valueOf(3))) / ((double) f.getSumFreq());
        if (!doubleEquals(actualPctObj, expectedFromCount, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:post-count-over-sum] semantic mismatch: getPct((Object) Integer.valueOf(3)) expected=" + expectedFromCount + " actual=" + actualPctObj + " count=" + f.getCount((Comparable<?>) Integer.valueOf(3)) + " sum=" + f.getSumFreq());
        }
    }

    private static void relationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Comparable<?>) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }

        if (!doubleEquals(actual, expected, 1e-9)) {
            throw new FuzzerSecurityIssueLow("relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum + " probe=" + probe);
        }
    }

    private static void relationObjectComparableAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double a;
        double b;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            a = f.getPct((Object) probe);
            b = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        if (!doubleEquals(a, b, 1e-9)) {
            throw new FuzzerSecurityIssueLow("relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a + " comparable=" + b + " probe=" + probe);
        }
    }

    private static void hiddenStateReadOnlyCheck(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        long sumBefore;
        long sumAfter;
        int hashBefore;
        int hashAfter;
        String toStringBefore;
        String toStringAfter;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));

            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            toStringBefore = f.toString();

            f.getPct((Object) probe);
            f.getCumPct((Object) probe);
            f.getPct((Comparable<?>) probe);

            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            toStringAfter = f.toString();
        } catch (Throwable t) {
            return;
        }

        // Documented guarantee: get* methods are read-only queries; they must not mutate observable state.
        if (sumBefore != sumAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-sum] semantic mismatch: read-only get* changed getSumFreq before=" + sumBefore + " after=" + sumAfter + " probe=" + probe);
        }
        if (hashBefore != hashAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-hash] semantic mismatch: read-only get* changed hashCode before=" + hashBefore + " after=" + hashAfter + " probe=" + probe);
        }
        if (!safeStringEquals(toStringBefore, toStringAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-string] semantic mismatch: read-only get* changed toString before=" + escapeOneLine(toStringBefore) + " after=" + escapeOneLine(toStringAfter) + " probe=" + probe);
        }
    }

    private static void extraSiblingAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long c1;
        long c2;
        double p1;
        double p2;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-8, 8);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f.addValue(v);
                        break;
                    default:
                        f.addValue((long) v);
                        break;
                }
            }
            probe = data.consumeInt(-8, 8);
            c1 = f.getCount(probe);
            c2 = f.getCount((long) probe);
            p1 = f.getPct(probe);
            p2 = f.getPct((long) probe);
        } catch (Throwable t) {
            return;
        }

        // Class contract used by the tests: integer values are not distinguished by type for these get* methods.
        if (c1 != c2) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-count] semantic mismatch: getCount(int) and getCount(long) disagree probe=" + probe + " intCount=" + c1 + " longCount=" + c2);
        }
        if (!doubleEquals(p1, p2, 1e-9)) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-pct] semantic mismatch: getPct(int) and getPct(long) disagree probe=" + probe + " intPct=" + p1 + " longPct=" + p2);
        }
    }

    private static double safeGetPctInt(Frequency f, int v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetPctLongPrimitive(Frequency f, long v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetPctLongObject(Frequency f, Long v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetPctObject(Frequency f, Object v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctInt(Frequency f, int v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctLongPrimitive(Frequency f, long v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctLongObject(Frequency f, Long v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctObject(Frequency f, Object v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static void checkLiftedDouble(String id, String what, double expected, double actual) {
        if (!doubleEquals(expected, actual, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean doubleEquals(double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        if (a == b) {
            return true;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= tol * scale;
    }

    private static boolean safeStringEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [92] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: f.getPct((Object) Integer.valueOf(3)) expected=0.5 actual=1.0

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,876 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-1] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-1] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(twoL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-2] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-2] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(oneI);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-3] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-3] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(twoI);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-4] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-4] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(threeL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-5] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-5] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(threeL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-6] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-6] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(3);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-7] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-7] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(threeI);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-8] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-8] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();
        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();

        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        if (sumBefore != sumAfter || hashBefore != hashAfter || !stringBefore.equals(stringAfter)) {
            throw new RuntimeException(
                "[oracle:read-only-getpct-object] metamorphic violation: getPct(Object) is a reader and must not mutate observable state input=3 sumBefore="
                    + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " toStringBefore=" + stringBefore.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
                    + " toStringAfter=" + stringAfter.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t"));
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-cumpct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-cumpct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgumentCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integer-argument-cumpct] semantic mismatch: expected=0.5 actual=" + actualIntegerArgumentCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-cumpct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-cumpct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:zero-cumpct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        try {
            // Documented sibling agreement: getPct(Object) is deprecated in favor of getPct(Comparable),
            // so for a Comparable input they must return the same percentage; a wrong delegation to getCumPct breaks this.
            double lhs = f.getPct((Object) Integer.valueOf(3));
            double rhs = f.getPct(Integer.valueOf(3));
            if (Math.abs(lhs - rhs) > tolerance) {
                throw new RuntimeException(
                    "[oracle:object-vs-comparable-getpct] metamorphic violation: equivalent overloads disagree input=3 lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        int extraAdds = data.consumeInt(0, 8);
        for (int i = 0; i < extraAdds; i++) {
            int op = data.consumeInt(0, 2);
            if (op == 0) {
                f.addValue(data.consumeInt(-1000000, 1000000));
            } else if (op == 1) {
                f.addValue((long) data.consumeInt(-1000000, 1000000));
            } else {
                f.addValue((char) data.consumeInt(0, 255));
            }

            if (f.getSumFreq() > 0) {
                double probePct = f.getPct("foo");
                if (Math.abs(probePct - 0.0d) > tolerance) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:foo-pct-reprobe] semantic mismatch: expected=0.0 actual=" + probePct);
                }
                double probeCumPct = f.getCumPct("foo");
                if (Math.abs(probeCumPct - 0.0d) > tolerance) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:foo-cumpct-reprobe] semantic mismatch: expected=0.0 actual=" + probeCumPct);
                }
            }
        }

        try {
            int chosen = data.consumeInt(-1000000, 1000000);
            Frequency g = new Frequency();
            g.addValue(chosen);
            g.addValue(chosen);
            g.addValue(chosen + 1);
            // Constructed-from-known-answer oracle: with counts {chosen:2, chosen+1:1}, pct(chosen) must be 2/3.
            double lhs = g.getPct((Object) Integer.valueOf(chosen));
            double rhs = g.getPct(Integer.valueOf(chosen));
            if (Math.abs(lhs - rhs) > tolerance) {
                throw new RuntimeException(
                    "[oracle:fuzzed-object-vs-comparable-getpct] metamorphic violation: equivalent overloads disagree input="
                        + chosen + " lhs=" + lhs + " rhs=" + rhs);
            }
            double expected = 2.0d / 3.0d;
            if (Math.abs(rhs - expected) > tolerance) {
                throw new RuntimeException(
                    "[oracle:fuzzed-known-answer-getpct] metamorphic violation: constructed distribution has known answer input="
                        + chosen + " lhs=" + rhs + " rhs=" + expected);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,325 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();
        hiddenStateOracle();
        relationGetPctObjectMatchesCountOverSumForIntegerObject(data);
        relationGetPctObjectAndComparableAgreeOnSameIntegerValue(data);
    }

    private static void liftedTestOracles() {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("test-one-pct", 0.25, f.getPct(1), "f.getPct(1)");
        assertDoubleEquals("test-two-pct", 0.25, f.getPct(Long.valueOf(2)), "f.getPct(Long.valueOf(2))");
        assertDoubleEquals("test-three-pct", 0.5, f.getPct(threeL), "f.getPct(threeL)");
        assertDoubleEquals("test-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), "f.getPct((Object) Integer.valueOf(3))");
        assertDoubleEquals("test-five-pct", 0.0, f.getPct(5), "f.getPct(5)");
        assertDoubleEquals("test-foo-pct", 0.0, f.getPct("foo"), "f.getPct(\"foo\")");
        assertDoubleEquals("test-one-cum-pct", 0.25, f.getCumPct(1), "f.getCumPct(1)");
        assertDoubleEquals("test-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)), "f.getCumPct(Long.valueOf(2))");
        assertDoubleEquals("test-integer-argument-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)), "f.getCumPct(Integer.valueOf(2))");
        assertDoubleEquals("test-three-cum-pct", 1.0, f.getCumPct(threeL), "f.getCumPct(threeL)");
        assertDoubleEquals("test-five-cum-pct", 1.0, f.getCumPct(5), "f.getCumPct(5)");
        assertDoubleEquals("test-zero-cum-pct", 0.0, f.getCumPct(0), "f.getCumPct(0)");
        assertDoubleEquals("test-foo-cum-pct", 0.0, f.getCumPct("foo"), "f.getCumPct(\"foo\")");
    }

    private static void hiddenStateOracle() {
        Frequency f = new Frequency();
        f.addValue(1L);
        f.addValue(2L);
        f.addValue(1);
        f.addValue(2);
        f.addValue(3L);
        f.addValue(3L);
        f.addValue(3);
        f.addValue(Integer.valueOf(3));

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();

        double ignored = f.getPct((Object) Integer.valueOf(3));

        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();

        if (ignored < -1.0) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-unreachable] semantic mismatch: impossible guard");
        }

        /* getPct is a read-only query ("Returns the percentage..."), so it must not mutate visible state.
           A patch that dodges the wrong branch by modifying internal bookkeeping would violate these cheap readers. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-getPctObject-readonly] semantic mismatch: getPct((Object) Integer.valueOf(3)) changed read-only state"
                    + " sumBefore=" + sumBefore
                    + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashAfter
                    + " toStringBefore=" + escape(stringBefore)
                    + " toStringAfter=" + escape(stringAfter)
            );
        }
    }

    private static void relationGetPctObjectMatchesCountOverSumForIntegerObject(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        long count = 0L;
        long sum = 0L;
        double actual = Double.NaN;
        double expected = Double.NaN;
        boolean ready = false;

        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = Integer.valueOf(data.consumeInt(-5, 5));
            count = f.getCount((Object) probe);
            sum = f.getSumFreq();
            if (sum <= 0) {
                return;
            }
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) probe);
            ready = true;
        } catch (Throwable e) {
            return;
        }

        /* Contract: getPct(Object) returns the percentage of values equal to v; with non-empty frequency data,
           that observable percentage is exactly getCount(v) / getSumFreq(). This catches a wrong-value patch
           even if it merely suppresses the buggy branch instead of throwing. */
        if (ready && !doubleEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe
                    + " actual=" + actual
                    + " expected=" + expected
                    + " count=" + count
                    + " sum=" + sum
            );
        }
    }

    private static void relationGetPctObjectAndComparableAgreeOnSameIntegerValue(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        double objectPct = Double.NaN;
        double comparablePct = Double.NaN;
        boolean ready = false;

        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);
            ready = true;
        } catch (Throwable e) {
            return;
        }

        /* The deprecated Object overload is documented as the same query as the Comparable overload on the same
           logical value; integer values are not distinguished by numeric wrapper type in Frequency. */
        if (ready && !doubleEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe
                    + " object=" + objectPct
                    + " comparable=" + comparablePct
            );
        }
    }

    private static void assertDoubleEquals(String oracleId, double expected, double actual, String what) {
        if (!doubleEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " expected=" + expected
                    + " actual=" + actual
            );
        }
    }

    private static boolean doubleEquals(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [96] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test-three-object-pct] semantic mismatch: f.getPct((Object) Integer.valueOf(3)) expected=0.5 actual=1.0

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,606 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));
        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        Frequency g = new Frequency();
        int base = data.consumeInt(-1000000, 999999);
        int lower = base;
        int higher = base + 1;
        int lowerCopies = data.consumeInt(1, 5);
        int higherCopies = data.consumeInt(1, 5);

        for (int i = 0; i < lowerCopies; i++) {
            if (data.consumeBoolean()) {
                g.addValue((long) lower);
            } else {
                g.addValue(lower);
            }
        }
        for (int i = 0; i < higherCopies; i++) {
            if (data.consumeBoolean()) {
                g.addValue((long) higher);
            } else {
                g.addValue(higher);
            }
        }

        try {
            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            String strBefore = g.toString();

            double objectPct = g.getPct((Object) Integer.valueOf(higher));
            double comparablePct = g.getPct(Integer.valueOf(higher));

            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            String strAfter = g.toString();

            if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(strBefore, strAfter)) {
                throw new RuntimeException(
                    "[oracle:read-only-state] metamorphic violation: getPct(Object) / getPct(Comparable) are documented read-only accessors, but observable state changed input=" +
                    higher + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter +
                    " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " toStringBefore=" + escape(strBefore) + " toStringAfter=" + escape(strAfter));
            }

            if (!sameDouble(objectPct, comparablePct)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: getPct(Object) and getPct(Comparable) have the same documented meaning for a Comparable argument input=" +
                    higher + " lhs=" + objectPct + " rhs=" + comparablePct +
                    " lowerCopies=" + lowerCopies + " higherCopies=" + higherCopies);
            }

            double expectedPct = ((double) g.getCount(Integer.valueOf(higher))) / ((double) g.getSumFreq());
            if (!sameDouble(comparablePct, expectedPct)) {
                throw new RuntimeException(
                    "[oracle:pct-count-sum] metamorphic violation: getPct(v) must equal getCount(v)/getSumFreq() when sumFreq>0 input=" +
                    higher + " lhs=" + comparablePct + " rhs=" + expectedPct +
                    " count=" + g.getCount(Integer.valueOf(higher)) + " sum=" + g.getSumFreq());
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void assertDoubleEquals(String oracleId, String label, double expected, double actual) {
        if (Double.isNaN(expected) ? !Double.isNaN(actual) : Math.abs(expected - actual) > TOLERANCE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label +
                " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [98] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [99] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~14,069 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedTestOracles();
        runExactPostConditionChecks();
        runRelationCountOverSum(data);
        runRelationObjectComparableAgreement(data);
        runExtraSiblingAgreement(data);
    }

    private static void runLiftedTestOracles() {
        final long oneL = 1;
        final long twoL = 2;
        final long threeL = 3;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

        Frequency f = new Frequency();
        try {
            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:setup-fixture] unexpected exception during exact FrequencyTest fixture setup", e);
        }

        assertDoubleEquals("oracle-one-pct", "one pct", 0.25, safeGetPctInt(f, 1));
        assertDoubleEquals("oracle-two-pct", "two pct", 0.25, safeGetPctLongObject(f, Long.valueOf(2)));
        assertDoubleEquals("oracle-three-pct", "three pct", 0.5, safeGetPctLongObject(f, Long.valueOf(threeL)));
        assertDoubleEquals("oracle-three-object-pct", "three (Object) pct", 0.5, safeGetPctObject(f, (Object) Integer.valueOf(3)));
        assertDoubleEquals("oracle-five-pct", "five pct", 0.0, safeGetPctInt(f, 5));
        assertDoubleEquals("oracle-foo-pct", "foo pct", 0.0, safeGetPctObject(f, "foo"));
        assertDoubleEquals("oracle-one-cum-pct", "one cum pct", 0.25, safeGetCumPctInt(f, 1));
        assertDoubleEquals("oracle-two-cum-pct", "two cum pct", 0.50, safeGetCumPctLongObject(f, Long.valueOf(2)));
        assertDoubleEquals("oracle-integer-argument", "Integer argument", 0.50, safeGetCumPctObject(f, Integer.valueOf(2)));
        assertDoubleEquals("oracle-three-cum-pct", "three cum pct", 1.0, safeGetCumPctLongObject(f, Long.valueOf(threeL)));
        assertDoubleEquals("oracle-five-cum-pct", "five cum pct", 1.0, safeGetCumPctInt(f, 5));
        assertDoubleEquals("oracle-zero-cum-pct", "zero cum pct", 0.0, safeGetCumPctInt(f, 0));
        assertDoubleEquals("oracle-foo-cum-pct", "foo cum pct", 0.0, safeGetCumPctObject(f, "foo"));
    }

    private static void runExactPostConditionChecks() {
        final long oneL = 1;
        final long twoL = 2;
        final long threeL = 3;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

        Frequency f = new Frequency();
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();

        double pctObject;
        double pctComparable;
        long sumMid;
        int hashMid;
        String stringMid;
        try {
            pctObject = f.getPct((Object) Integer.valueOf(3));
            sumMid = f.getSumFreq();
            hashMid = f.hashCode();
            stringMid = f.toString();
            pctComparable = f.getPct((Comparable<?>) Integer.valueOf(3));
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:readonly-post] unexpected exception during read-only post-condition check", e);
        }

        /* Contract justification: getPct/getCumPct/getCount/getSumFreq are query methods ("Returns ...");
           they are read-only. A patch that merely redirects or deletes bookkeeping can silently mutate hidden
           state; therefore cheap observable readers must remain unchanged across the call. */
        if (sumBefore != sumMid || hashBefore != hashMid || !eq(stringBefore, stringMid)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:readonly-post] semantic mismatch: read-only getPct(Object) changed observable state"
                    + " sumBefore=" + sumBefore
                    + " sumAfter=" + sumMid
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashMid
                    + " toStringBefore=" + escapeOneLine(stringBefore)
                    + " toStringAfter=" + escapeOneLine(stringMid));
        }

        /* Contract justification: deprecated getPct(Object) is the sibling of getPct(Comparable), and the
           class/test contract states integer values are not distinguished by type for get* queries. Therefore
           the same Integer logical input through both overloads must agree. */
        if (!approxEqual(pctObject, pctComparable, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exact-sibling] semantic mismatch: getPct(Object) and getPct(Comparable) disagree on Integer.valueOf(3)"
                    + " object=" + pctObject + " comparable=" + pctComparable);
        }
    }

    private static void runRelationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (RuntimeException e) {
            return;
        }

        /* Contract justification: getPct(Object) "Returns the percentage of values that are equal to v".
           Together with getCount and getSumFreq, on any non-empty Frequency this equals count(v)/sumFreq.
           This directly checks the patched Object overload on valid integral inputs. */
        if (!approxEqual(actual, expected, 1e-9)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: "
                    + "probe=" + probe + " actual=" + actual + " expected=" + expected
                    + " count=" + count + " sum=" + sum);
        }
    }

    private static void runRelationObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double objectPct;
        double comparablePct;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);
        } catch (RuntimeException e) {
            return;
        }

        /* Contract justification: deprecated getPct(Object) is replaced by getPct(Comparable), and integer
           values are documented/tested as not distinguished by type for these queries; equivalent inputs must agree. */
        if (!approxEqual(objectPct, comparablePct, 1e-9)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:object-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: "
                    + "probe=" + probe + " object=" + objectPct + " comparable=" + comparablePct);
        }
    }

    private static void runExtraSiblingAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double pctInt;
        double pctObject;
        long sumBefore;
        long sumAfter;
        int hashBefore;
        int hashAfter;
        String strBefore;
        String strAfter;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-8, 8);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f.addValue(v);
                        break;
                    default:
                        f.addValue((long) v);
                        break;
                }
            }
            probe = data.consumeInt(-8, 8);
            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            strBefore = f.toString();
            pctInt = f.getPct(probe);
            pctObject = f.getPct((Object) Integer.valueOf(probe));
            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            strAfter = f.toString();
        } catch (RuntimeException e) {
            return;
        }

        /* Contract justification: same logical integer value through same-name overloads getPct(int) and
           getPct(Object with Integer) must agree; this is a sibling-agreement check over the public API. */
        if (!approxEqual(pctInt, pctObject, 1e-9)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:int-object-sibling] metamorphic violation: getPct(int) and getPct(Object(Integer)) disagree"
                    + " probe=" + probe + " int=" + pctInt + " object=" + pctObject);
        }

        /* Hidden-state check on another read-only query path. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || !eq(strBefore, strAfter)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:hidden-state] metamorphic violation: read-only getPct queries changed state"
                    + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " toStringBefore=" + escapeOneLine(strBefore)
                    + " toStringAfter=" + escapeOneLine(strAfter));
        }
    }

    private static double safeGetPctInt(Frequency f, int v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getPct(int)", e);
        }
    }

    private static double safeGetPctLongObject(Frequency f, Long v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getPct(Long)", e);
        }
    }

    private static double safeGetPctObject(Frequency f, Object v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getPct(Object)", e);
        }
    }

    private static double safeGetCumPctInt(Frequency f, int v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getCumPct(int)", e);
        }
    }

    private static double safeGetCumPctLongObject(Frequency f, Long v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getCumPct(Long)", e);
        }
    }

    private static double safeGetCumPctObject(Frequency f, Object v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getCumPct(Object)", e);
        }
    }

    private static void assertDoubleEquals(String oracleId, String what, double expected, double actual) {
        if (!approxEqual(actual, expected, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEqual(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [100] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:oracle-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [101] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [44] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,738 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        long sumBeforeRead = f.getSumFreq();
        int hashBeforeRead = f.hashCode();
        String stringBeforeRead = f.toString();

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(Long.valueOf(threeL));
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:integer-arg-cum-pct] semantic mismatch: expected=0.5 actual=" + actualIntegerArgCumPct);
        }

        double actualThreeCumPct = f.getCumPct(Long.valueOf(threeL));
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        long sumAfterRead = f.getSumFreq();
        int hashAfterRead = f.hashCode();
        String stringAfterRead = f.toString();
        String escapedBefore = stringBeforeRead == null ? "null" : stringBeforeRead.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
        String escapedAfter = stringAfterRead == null ? "null" : stringAfterRead.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");

        /* Documented guarantee: getPct/getCumPct are readers ("Returns the percentage..."), so a correct implementation must not mutate observable state.
           A patch that merely reroutes/guards the computation incorrectly can still silently change internal state; checking getSumFreq/hashCode/toString catches that. */
        if (sumBeforeRead != sumAfterRead || hashBeforeRead != hashAfterRead || (stringBeforeRead == null ? stringAfterRead != null : !stringBeforeRead.equals(stringAfterRead))) {
            throw new RuntimeException("[oracle:read-only-state] metamorphic violation: read-only percentage query changed state sumBefore=" + sumBeforeRead + " sumAfter=" + sumAfterRead + " hashBefore=" + hashBeforeRead + " hashAfter=" + hashAfterRead + " toStringBefore=" + escapedBefore + " toStringAfter=" + escapedAfter);
        }

        /* Documented guarantee: getPct(Object) is deprecated and replaced by getPct(Comparable); for Comparable inputs they describe the same result.
           Therefore, on any Comparable probe, both overloads must agree. */
        double typedThreePct;
        try {
            typedThreePct = f.getPct((Comparable<?>) Integer.valueOf(3));
        } catch (Throwable t) {
            return;
        }
        if (Math.abs(actualThreeObjectPct - typedThreePct) > tolerance) {
            throw new RuntimeException("[oracle:overload-agreement-seed] metamorphic violation: getPct(Object) != getPct(Comparable) input=3 lhs=" + actualThreeObjectPct + " rhs=" + typedThreePct);
        }

        int target = data.consumeInt(-1000000, 1000000);
        int targetCopies = data.consumeInt(1, 8);
        int otherCopies = data.consumeInt(1, 8);
        int distinctOffset = data.consumeInt(1, 16);
        int other = target + distinctOffset;

        Frequency g = new Frequency();
        for (int i = 0; i < targetCopies; i++) {
            g.addValue(target);
        }
        for (int i = 0; i < otherCopies; i++) {
            g.addValue(other);
        }

        /* Oracle from construction: we choose exactly targetCopies occurrences of target among targetCopies+otherCopies total values,
           so the documented "proportion of values equal to v" must be targetCopies/sum for every correct implementation. */
        double expectedConstructedPct = ((double) targetCopies) / ((double) (targetCopies + otherCopies));
        double actualConstructedObjectPct;
        try {
            actualConstructedObjectPct = g.getPct((Object) Integer.valueOf(target));
        } catch (Throwable t) {
            return;
        }
        if (Math.abs(actualConstructedObjectPct - expectedConstructedPct) > tolerance) {
            throw new RuntimeException("[oracle:constructed-pct] metamorphic violation: constructed frequency percentage wrong input=" + target + " expected=" + expectedConstructedPct + " actual=" + actualConstructedObjectPct + " targetCopies=" + targetCopies + " otherCopies=" + otherCopies + " other=" + other);
        }

        /* Same documented guarantee as above: the Object and Comparable overloads are equivalent for Comparable inputs. */
        double actualConstructedTypedPct;
        try {
            actualConstructedTypedPct = g.getPct((Comparable<?>) Integer.valueOf(target));
        } catch (Throwable t) {
            return;
        }
        if (Math.abs(actualConstructedObjectPct - actualConstructedTypedPct) > tolerance) {
            throw new RuntimeException("[oracle:overload-agreement-fuzz] metamorphic violation: getPct(Object) != getPct(Comparable) input=" + target + " lhs=" + actualConstructedObjectPct + " rhs=" + actualConstructedTypedPct + " targetCopies=" + targetCopies + " otherCopies=" + otherCopies);
        }
    }
}
```

</details>

---
## [102] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:three-object-pct] semantic mismatch: expected=0.5 actual=1.0

---
## [103] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [42] — not reprinted*
- *[user] message: identical to step [42] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,113 chars)</summary>

```
package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleFromTest();
        runCountOverSumRelation(data);
        runObjectComparableAgreementRelation(data);
        runSeedShapedGeneralization(data);
    }

    private static void runLiftedOracleFromTest() {
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        Frequency f = new Frequency();
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertApprox("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertApprox("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertApprox("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));
        assertApprox("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        assertApprox("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertApprox("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertApprox("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertApprox("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertApprox("lifted-integer-arg-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertApprox("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertApprox("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertApprox("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertApprox("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // Contract-based hidden-state check: get* methods are read-only queries, so cheap public readers
        // (sumFreq/hashCode/toString) must not change across getPct/getCumPct calls. A "fix" that silently mutates
        // state instead of returning the correct value would violate this observable post-condition.
        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();

        f.getPct((Object) Integer.valueOf(3));
        f.getPct((Comparable<?>) Integer.valueOf(3));
        f.getCumPct(Integer.valueOf(2));

        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterString = f.toString();

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-lifted] semantic mismatch: read-only get* changed state beforeSum=" + beforeSum
                    + " afterSum=" + afterSum
                    + " beforeHash=" + beforeHash
                    + " afterHash=" + afterHash
                    + " beforeString=" + escape(beforeString)
                    + " afterString=" + escape(afterString));
        }
    }

    private static void runCountOverSumRelation(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        long count;
        long sum;
        double actual;
        double expected;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = Integer.valueOf(data.consumeInt(-5, 5));

            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            count = f.getCount((Object) probe);
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) probe);

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            // Contract-based hidden-state check: on a non-empty distribution, getPct(v) returns the percentage of
            // values equal to v, i.e. getCount(v) / getSumFreq(). Also, query methods are non-mutating.
            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-relation1] semantic mismatch: getCount/getPct changed state beforeSum=" + beforeSum
                        + " afterSum=" + afterSum
                        + " beforeHash=" + beforeHash
                        + " afterHash=" + afterHash
                        + " beforeString=" + escape(beforeString)
                        + " afterString=" + escape(afterString));
            }
        } catch (Throwable e) {
            return;
        }

        if (!approxEqual(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:count-over-sum] metamorphic violation: getPct((Object) Integer) != getCount((Object) Integer)/getSumFreq probe="
                    + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void runObjectComparableAgreementRelation(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        double objectPct;
        double comparablePct;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));

            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            // Contract-based sibling agreement: deprecated getPct(Object) is replaced by getPct(Comparable),
            // so the same Integer logical input must produce the same percentage through both overloads.
            // Query methods are also read-only, so public readers must remain unchanged.
            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-relation2] semantic mismatch: getPct overloads changed state beforeSum=" + beforeSum
                        + " afterSum=" + afterSum
                        + " beforeHash=" + beforeHash
                        + " afterHash=" + afterHash
                        + " beforeString=" + escape(beforeString)
                        + " afterString=" + escape(afterString));
            }
        } catch (Throwable e) {
            return;
        }

        if (!approxEqual(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:object-vs-comparable] metamorphic violation: getPct(Object) and getPct(Comparable) disagree for same Integer probe="
                    + probe + " objectPct=" + objectPct + " comparablePct=" + comparablePct);
        }
    }

    private static void runSeedShapedGeneralization(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        double objectPct;
        double expected;
        try {
            int extraCount = data.consumeInt(0, 6);
            int[] extras = new int[extraCount];
            for (int i = 0; i < extraCount; i++) {
                extras[i] = data.consumeInt(-4, 4);
            }
            probe = Integer.valueOf(data.consumeInt(-4, 4));

            f = new Frequency();

            // Seed-shaped family: create distributions where the chosen probe appears exactly four times out of eight
            // baseline insertions, mirroring the failing test's "same logical integer across int/long/Integer".
            f.addValue((long) probe.intValue());
            f.addValue((long) probe.intValue());
            f.addValue(probe.intValue());
            f.addValue(Integer.valueOf(probe.intValue()));

            int other1 = probe.intValue() == 0 ? 1 : 0;
            int other2 = probe.intValue() == 1 ? 2 : 1;
            f.addValue((long) other1);
            f.addValue(other1);
            f.addValue((long) other2);
            f.addValue(other2);

            for (int i = 0; i < extraCount; i++) {
                int e = extras[i];
                if (data.consumeBoolean()) {
                    f.addValue(e);
                } else {
                    f.addValue((long) e);
                }
            }

            long count = f.getCount((Comparable<?>) probe);
            long sum = f.getSumFreq();
            expected = (double) count / (double) sum;

            objectPct = f.getPct((Object) probe);
        } catch (Throwable e) {
            return;
        }

        if (!approxEqual(objectPct, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-family] metamorphic violation: object overload disagrees with seeded cross-type distribution probe="
                    + probe + " objectPct=" + objectPct + " expected=" + expected);
        }
    }

    private static void assertApprox(String oracleId, String label, double expected, double actual) {
        if (!approxEqual(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEqual(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [104] ⚙️ harness-repair · `attempt_030`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [105] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-three-object-pct] semantic mismatch: three (Object) pct expected=0.5 actual=1.0

---
## [106] ⚙️ coverage · `jazzer coverage dumps`
**output:** **29/29 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov; 29 raw fuzz log(s)**
<details><summary>▸ detail (~16,362 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 263}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 259}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 259}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 262}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 255}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 259}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 259}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 257}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 263}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 263}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 257}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 259}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 262}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 257}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 253}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 258}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 260}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 263}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 264}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 257}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 254}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 263}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 264}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/009_patch1-Math-75-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
