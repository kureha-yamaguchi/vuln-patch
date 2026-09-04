# Pipeline trace — Chart-5

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /source//org/jfree/data/xy/XYSeries.java
+++ /source//org/jfree/data/xy/XYSeries.java
@@ -561,7 +561,7 @@
             // new item...otherwise it will be just -1 and we should just
             // append the value to the list...
             if (this.autoSort) {
-                this.data.add(-index - 1, new XYDataItem(x, y));
+                org.jfree.data.xy.XYSeries.this.data.add(new org.jfree.data.xy.XYDataItem(x, y));
             }
             else {
                 this.data.add(new XYDataItem(x, y));
```

17 sequential steps — 4 LLM calls, 13 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)
- **patched-fuzz** — fuzzes an accepted harness against the PATCHED build — FIRED (with the input + mismatch) means the overfit was caught; quiet means it escaped

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.jfree.data.xy.junit.XYSeriesTests', test_method='testBug1955483', source (~668 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.jfree.data.xy.junit.XYSeriesTests', test_method='testBug1955483', source_path='/tmp/d4j/Chart_5_buggy/tests/org/jfree/data/xy/junit/XYSeriesTests.java', method_source='    public void testBug1955483() {\\n        XYSeries series = new XYSeries(\"Series\", true, true);\\n        series.addOrUpdate(1.0, 1.0);\\n        series.addOrUpdate(1.0, 2.0);\\n        assertEquals(new Double(1.0), series.getY(0));\\n        assertEquals(new Double(2.0), series.getY(1));\\n        assertEquals(2, series.getItemCount());\\n    }', exception_type='java.lang.IndexOutOfBoundsException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.jfree.data.xy.junit.XYSeriesTests::testBug1955483",
    "failure_message": "--- org.jfree.data.xy.junit.XYSeriesTests::testBug1955483\njava.lang.IndexOutOfBoundsException: Index: -1, Size: 1",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~17,369 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "source//org/jfree/data/xy/XYSeries.java"
  ],
  "patch_text": "--- /source//org/jfree/data/xy/XYSeries.java\n+++ /source//org/jfree/data/xy/XYSeries.java\n@@ -561,7 +561,7 @@\n             // new item...otherwise it will be just -1 and we should just\n             // append the value to the list...\n             if (this.autoSort) {\n-                this.data.add(-index - 1, new XYDataItem(x, y));\n+                org.jfree.data.xy.XYSeries.this.data.add(new org.jfree.data.xy.XYDataItem(x, y));\n             }\n             else {\n                 this.data.add(new XYDataItem(x, y));\n",
  "functions": [
    {
      "func_name": "addOrUpdate",
      "func_signature": "public XYDataItem addOrUpdate(Number x, Number y)",
      "func_source": "    public XYDataItem addOrUpdate(Number x, Number y) {\n        if (x == null) {\n            throw new IllegalArgumentException(\"Null 'x' argument.\");\n        }\n\n        // if we get to here, we know that duplicate X values are not permitted\n        XYDataItem overwritten = null;\n        int index = indexOf(x);\n        if (index >= 0 && !this.allowDuplicateXValues) {\n            XYDataItem existing = (XYDataItem) this.data.get(index);\n            try {\n                overwritten = (XYDataItem) existing.clone();\n            }\n            catch (CloneNotSupportedException e) {\n                throw new SeriesException(\"Couldn't clone XYDataItem!\");\n            }\n            existing.setY(y);\n        }\n        else {\n            // if the series is sorted, the negative index is a result from\n            // Collections.binarySearch() and tells us where to insert the\n            // new item...otherwise it will be just -1 and we should just\n            // append the value to the list...\n            if (this.autoSort) {\n                this.data.add(-index - 1, new XYDataItem(x, y));\n            }\n            else {\n                this.data.add(new XYDataItem(x, y));\n            }\n            // check if this addition will exceed the maximum item count...\n            if (getItemCount() > this.maximumItemCount) {\n                this.data.remove(0);\n            }\n        }\n        fireSeriesChanged();\n        return overwritten;\n    }",
      "func_class": "XYSeries",
      "func_class_fq": "org.jfree.data.xy.XYSeries",
      "func_param_types": [
        "Number",
        "Number"
      ],
      "fi_name": "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "overload_types": [
        [
          "double",
          "double"
        ],
        [
          "Number",
          "Number"
        ]
      ],
      "xrefs": [],
      "reachable": [
        "[IllegalArgumentException].<init>(String)",
        "[org.jfree.data.xy.XYSeries].indexOf(Number)",
        "[java.util.List].get(int)",
        "existing.clone()",
        "[org.jfree.data.general.SeriesException].<init>(String)",
        "existing.setY(Number)",
        "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)",
        "[java.util.List].add(int,org.jfree.data.xy.XYDataItem)",
        "[java.util.List].add(org.jfree.data.xy.XYDataItem)",
        "[org.jfree.data.xy.XYSeries].getItemCount()",
        "[java.util.List].remove(int)",
        "[org.jfree.data.xy.XYSeries].fireSeriesChanged()",
        "[org.jfree.data.xy.XYDataItem].<init>(Number,null)",
        "[java.util.Collections].binarySearch(java.util.List,org.jfree.data.xy.XYDataItem)",
        "[java.util.List].size()",
        "[java.util.List].get(org.jfree.data.xy.XYSeries)",
        "item.getX()",
        "[org.jfree.data.xy.XYSeries].equals(Number)",
        "[RuntimeException].<init>(String)",
        "[org.jfree.data.xy.XYSeries].add(org.jfree.data.xy.XYDataItem)",
        "[org.jfree.data.time.DynamicTimeSeriesCollection].fireSeriesChanged()",
        "[org.jfree.data.gantt.TaskSeries].get(int)",
        "[org.jfree.data.xy.XYSeries].remove(int)"
      ],
      "related_callees": [
        {
          "name": "indexOf",
          "source_file": "DefaultXYDataset.java",
          "signature": "public int indexOf(Comparable seriesKey)",
          "source": "    public int indexOf(Comparable seriesKey) {\n        return this.seriesKeys.indexOf(seriesKey);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "DefaultXYDataset.java",
              "    public int indexOf(Comparable seriesKey) {\n        return this.seriesKeys.indexOf(seriesKey);\n    }"
            ],
            [
              "DefaultXYZDataset.java",
              "    public int indexOf(Comparable seriesKey) {\n        return this.seriesKeys.indexOf(seriesKey);\n    }"
            ],
            [
              "VectorSeriesCollection.java",
              "    public int indexOf(VectorSeries series) {\n        if (series == null) {\n            throw new IllegalArgumentException(\"Null 'series' argument.\");\n        }\n        return this.data.indexOf(series);\n    }"
            ],
            [
              "XYSeries.java",
              "    public int indexOf(Number x) {\n        if (this.autoSort) {\n            return Collections.binarySearch(this.data, new XYDataItem(x, null));\n        }\n        else {\n            for (int i = 0; i < this.data.size(); i++) {\n                XYDataItem item = (XYDataItem) this.data.get(i);\n                if (item.getX().equals(x)) {\n                    return i;\n                }\n            }\n            return -1;\n        }\n    }"
            ]
          ]
        },
        {
          "name": "get",
          "source_file": "MatrixSeries.java",
          "signature": "public double get(int i, int j)",
          "source": "    public double get(int i, int j) {\n        return this.data[i][j];\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "clone",
          "source_file": "CategoryTableXYDataset.java",
          "signature": "public Object clone() throws CloneNotSupportedException",
          "source": "    public Object clone() throws CloneNotSupportedException {\n        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();\n        clone.values = (DefaultKeyedValues2D) this.values.clone();\n        clone.intervalDelegate = new IntervalXYDelegate(clone);\n        // need to configure the intervalDelegate to match the original\n        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());\n        clone.intervalDelegate.setAutoWidth(isAutoWidth());\n        clone.intervalDelegate.setIntervalPositionFactor(\n                getIntervalPositionFactor());\n        return clone;\n    }",
          "is_abstract": false,
          "impls": [
            [
              "CategoryTableXYDataset.java",
              "    public Object clone() throws CloneNotSupportedException {\n        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();\n        clone.values = (DefaultKeyedValues2D) this.values.clone();\n        clone.intervalDelegate = new IntervalXYDelegate(clone);\n        // need to configure the intervalDelegate to match the original\n        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());\n        clone.intervalDelegate.setAutoWidth(isAutoWidth());\n        clone.intervalDelegate.setIntervalPositionFactor(\n                getIntervalPositionFactor());\n        return clone;\n    }"
            ],
            [
              "DefaultIntervalXYDataset.java",
              "    public Object clone() throws CloneNotSupportedException {\n        DefaultIntervalXYDataset clone\n                = (DefaultIntervalXYDataset) super.clone();\n        clone.seriesKeys = new java.util.ArrayList(this.seriesKeys);\n        clone.seriesList = new ArrayList(this.seriesList.size());\n        for (int i = 0; i < this.seriesList.size(); i++) {\n            double[][] data = (double[][]) this.seriesList.get(i);\n            double[] x = data[0];\n            double[] xStart = data[1];\n            double[] xEnd = data[2];\n            double[] y = data[3];\n            double[] yStart = data[4];\n            double[] yEnd = data[5];\n            double[] xx = new double[x.length];\n            double[] xxStart = new double[xStart.length];\n            double[] xxEnd = new double[xEnd.length];\n            double[] yy = new double[y.length];\n            double[] yyStart = new double[yStart.length];\n            double[] yyEnd = new double[yEnd.length];\n            System.arraycopy(x, 0, xx, 0, x.length);\n            System.arraycopy(xStart, 0, xxStart, 0, xStart.length);\n            System.arraycopy(xEnd, 0, xxEnd, 0, xEnd.length);\n            System.arraycopy(y, 0, yy, 0, y.length);\n            System.arraycopy(yStart, 0, yyStart, 0, yStart.length);\n            System.arraycopy(yEnd, 0, yyEnd, 0, yEnd.length);\n            clone.seriesList.add(i, new double[][] {xx, xxStart, xxEnd, yy,\n                    yyStart, yyEnd});\n        }\n        return clone;\n    }"
            ],
            [
              "DefaultOHLCDataset.java",
              "    public Object clone() throws CloneNotSupportedException {\n        DefaultOHLCDataset clone = (DefaultOHLCDataset) super.clone();\n        clone.data = new OHLCDataItem[this.data.length];\n        System.arraycopy(this.data, 0, clone.data, 0, this.data.length);\n        return clone;\n    }"
            ],
            [
              "DefaultTableXYDataset.java",
              "    public Object clone() throws CloneNotSupportedException {\n        DefaultTableXYDataset clone = (DefaultTableXYDataset) super.clone();\n        int seriesCount = this.data.size();\n        clone.data = new java.util.ArrayList(seriesCount);\n        for (int i = 0; i < seriesCount; i++) {\n            XYSeries series = (XYSeries) this.data.get(i);\n            clone.data.add(series.clone());\n        }\n\n        clone.intervalDelegate = new IntervalXYDelegate(clone);\n        // need to configure the intervalDelegate to match the original\n        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());\n        clone.intervalDelegate.setAutoWidth(isAutoWidth());\n        clone.intervalDelegate.setIntervalPositionFactor(\n                getIntervalPositionFactor());\n        clone.updateXPoints();\n        return clone;\n    }"
            ]
          ]
        },
        {
          "name": "setY",
          "source_file": "XYDataItem.java",
          "signature": "public void setY(double y)",
          "source": "    public void setY(double y) {\n        setY(new Double(y));\n    }",
          "is_abstract": false,
          "impls": [
            [
              "XYDataItem.java",
              "    public void setY(double y) {\n        setY(new Double(y));\n    }"
            ],
            [
              "XYDataItem.java",
              "    public void setY(Number y) {\n        this.y = y;\n    }"
            ]
          ]
        },
        {
          "name": "add",
          "source_file": "CategoryTableXYDataset.java",
          "signature": "public void add(double x, double y, String seriesName)",
          "source": "    public void add(double x, double y, String seriesName) {\n        add(new Double(x), new Double(y), seriesName, true);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "CategoryTableXYDataset.java",
              "    public void add(double x, double y, String seriesName) {\n        add(new Double(x), new Double(y), seriesName, true);\n    }"
            ],
            [
              "CategoryTableXYDataset.java",
              "    public void add(Number x, Number y, String seriesName, boolean notify) {\n        this.values.addValue(y, (Comparable) x, seriesName);\n        if (notify) {\n            fireDatasetChanged();\n        }\n    }"
            ],
            [
              "VectorSeries.java",
              "    public void add(double x, double y, double deltaX, double deltaY) {\n        super.add(new VectorDataItem(x, y, deltaX, deltaY), true);\n    }"
            ],
            [
              "XIntervalSeries.java",
              "    public void add(double x, double xLow, double xHigh, double y) {\n        super.add(new XIntervalDataItem(x, xLow, xHigh, y), true);\n    }"
            ]
          ]
        },
        {
          "name": "getItemCount",
          "source_file": "TableXYDataset.java",
          "signature": "public int getItemCount()",
          "source": "    public int getItemCount();",
          "is_abstract": true,
          "impls": [
            [
              "CategoryTableXYDataset.java",
              "    public int getItemCount() {\n        return this.values.getRowCount();\n    }"
            ],
            [
              "CategoryTableXYDataset.java",
              "    public int getItemCount(int series) {\n        return getItemCount();  // all series have the same number of items in\n                                // this dataset\n    }"
            ],
            [
              "DefaultHighLowDataset.java",
              "    public int getItemCount(int series) {\n        return this.date.length;\n    }"
            ],
            [
              "DefaultIntervalXYDataset.java",
              "    public int getItemCount(int series) {\n        if ((series < 0) || (series >= getSeriesCount())) {\n            throw new IllegalArgumentException(\"Series index out of bounds\");\n        }\n        double[][] seriesArray = (double[][]) this.seriesList.get(series);\n        return seriesArray[0].length;\n    }"
            ]
          ]
        }
      ],
      "field_siblings": [
        {
          "name": "equals",
          "signature": "public boolean equals(Object obj)",
          "shared_fields": [
            "allowDuplicateXValues",
            "autoSort",
            "data",
            "maximumItemCount"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Tests this series for equality with an arbitrary object. @param obj  the object to test against for equality (<code>null</code> permitted). @return A boolean."
        },
        {
          "name": "XYSeries",
          "signature": "public XYSeries(Comparable key, boolean autoSort, boolean allowDuplicateXValues)",
          "shared_fields": [
            "allowDuplicateXValues",
            "autoSort",
            "data"
          ],
          "is_constructor": true,
          "source": "    public XYSeries(Comparable key,\n                    boolean autoSort,\n                    boolean allowDuplicateXValues) {\n        super(key);\n        this.data = new java.util.ArrayList();\n        this.autoSort = autoSort;\n        this.allowDuplicateXValues = allowDuplicateXValues;\n    }",
          "javadoc": "Constructs a new xy-series that contains no data.  You can specify whether or not duplicate x-values are allowed for the series. @param key  the series key (<code>null</code> not permitted). @param autoSort  a flag that controls whether or not the items in the series are sorted. @param allowDuplicateXValues  a flag tha…"
        },
        {
          "name": "hashCode",
          "signature": "public int hashCode()",
          "shared_fields": [
            "allowDuplicateXValues",
            "autoSort",
            "maximumItemCount"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Returns a hash code. @return A hash code."
        },
        {
          "name": "setMaximumItemCount",
          "signature": "public void setMaximumItemCount(int maximum)",
          "shared_fields": [
            "data",
            "maximumItemCount"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Sets the maximum number of items that will be retained in the series. If you add a new item to the series such that the number of items will exceed the maximum item count, then the first element in the series is automatically removed, ensuring that the maximum item count is not exceeded. <p> Typically this value is set…"
        },
        {
          "name": "getAutoSort",
          "signature": "public boolean getAutoSort()",
          "shared_fields": [
            "autoSort"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Returns the flag that controls whether the items in the series are automatically sorted.  There is no setter for this flag, it must be defined in the series constructor. @return A boolean."
        }
      ]
    }
  ],
  "package": "org.jfree.data.xy",
  "root_cause_reachable": [
    "XYSeries.indexOf",
    "SeriesException.<init>",
    "XYDataItem.<init>",
    "XYSeries.getItemCount",
    "XYSeries.fireSeriesChanged",
    "XYSeries.equals",
    "XYSeries.add",
    "DynamicTimeSeriesCollection.fireSeriesChanged",
    "TaskSeries.get",
    "XYSeries.remove"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.io.Serializable;",
    "import java.util.Collections;",
    "import java.util.List;",
    "import org.jfree.chart.util.ObjectUtilities;",
    "import org.jfree.data.general.Series;",
    "import org.jfree.data.general.SeriesChangeEvent;",
    "import org.jfree.data.general.SeriesException;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~35,319 chars)</summary>

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
- Package: `org.jfree.data.xy` (`package org.jfree.data.xy;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_5_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source//org/jfree/data/xy/XYSeries.java
+++ /source//org/jfree/data/xy/XYSeries.java
@@ -561,7 +561,7 @@
             // new item...otherwise it will be just -1 and we should just
             // append the value to the list...
             if (this.autoSort) {
-                this.data.add(-index - 1, new XYDataItem(x, y));
+                org.jfree.data.xy.XYSeries.this.data.add(new org.jfree.data.xy.XYDataItem(x, y));
             }
             else {
                 this.data.add(new XYDataItem(x, y));

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.data.general.Series;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesException;
</source_imports>

Function `addOrUpdate`:
<signature>
public XYDataItem addOrUpdate(Number x, Number y)
</signature>
<code>
    public XYDataItem addOrUpdate(Number x, Number y) {
        if (x == null) {
            throw new IllegalArgumentException("Null 'x' argument.");
        }

        // if we get to here, we know that duplicate X values are not permitted
        XYDataItem overwritten = null;
        int index = indexOf(x);
        if (index >= 0 && !this.allowDuplicateXValues) {
            XYDataItem existing = (XYDataItem) this.data.get(index);
            try {
                overwritten = (XYDataItem) existing.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new SeriesException("Couldn't clone XYDataItem!");
            }
            existing.setY(y);
        }
        else {
            // if the series is sorted, the negative index is a result from
            // Collections.binarySearch() and tells us where to insert the
            // new item...otherwise it will be just -1 and we should just
            // append the value to the list...
            if (this.autoSort) {
                this.data.add(-index - 1, new XYDataItem(x, y));
            }
            else {
                this.data.add(new XYDataItem(x, y));
            }
            // check if this addition will exceed the maximum item count...
            if (getItemCount() > this.maximumItemCount) {
                this.data.remove(0);
            }
        }
        fireSeriesChanged();
        return overwritten;
    }
</code>
Methods called by `addOrUpdate` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="indexOf" from="DefaultXYDataset.java">
<signature>
public int indexOf(Comparable seriesKey)
</signature>
<code>
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</code>
<implementation in="DefaultXYDataset.java">
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</implementation>
<implementation in="DefaultXYZDataset.java">
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</implementation>
<implementation in="VectorSeriesCollection.java">
    public int indexOf(VectorSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Null 'series' argument.");
        }
        return this.data.indexOf(series);
    }
</implementation>
<implementation in="XYSeries.java">
    public int indexOf(Number x) {
        if (this.autoSort) {
            return Collections.binarySearch(this.data, new XYDataItem(x, null));
        }
        else {
            for (int i = 0; i < this.data.size(); i++) {
                XYDataItem item = (XYDataItem) this.data.get(i);
                if (item.getX().equals(x)) {
                    return i;
                }
            }
            return -1;
        }
    }
</implementation>
</callee>
<callee name="get" from="MatrixSeries.java">
<signature>
public double get(int i, int j)
</signature>
<code>
    public double get(int i, int j) {
        return this.data[i][j];
    }
</code>
</callee>
<callee name="clone" from="CategoryTableXYDataset.java">
<signature>
public Object clone() throws CloneNotSupportedException
</signature>
<code>
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        return clone;
    }
</code>
<implementation in="CategoryTableXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        return clone;
    }
</implementation>
<implementation in="DefaultIntervalXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultIntervalXYDataset clone
                = (DefaultIntervalXYDataset) super.clone();
        clone.seriesKeys = new java.util.ArrayList(this.seriesKeys);
        clone.seriesList = new ArrayList(this.seriesList.size());
        for (int i = 0; i < this.seriesList.size(); i++) {
            double[][] data = (double[][]) this.seriesList.get(i);
            double[] x = data[0];
            double[] xStart = data[1];
            double[] xEnd = data[2];
            double[] y = data[3];
            double[] yStart = data[4];
            double[] yEnd = data[5];
            double[] xx = new double[x.length];
            double[] xxStart = new double[xStart.length];
            double[] xxEnd = new double[xEnd.length];
            double[] yy = new double[y.length];
            double[] yyStart = new double[yStart.length];
            double[] yyEnd = new double[yEnd.length];
            System.arraycopy(x, 0, xx, 0, x.length);
            System.arraycopy(xStart, 0, xxStart, 0, xStart.length);
            System.arraycopy(xEnd, 0, xxEnd, 0, xEnd.length);
            System.arraycopy(y, 0, yy, 0, y.length);
            System.arraycopy(yStart, 0, yyStart, 0, yStart.length);
            System.arraycopy(yEnd, 0, yyEnd, 0, yEnd.length);
            clone.seriesList.add(i, new double[][] {xx, xxStart, xxEnd, yy,
                    yyStart, yyEnd});
        }
        return clone;
    }
</implementation>
<implementation in="DefaultOHLCDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultOHLCDataset clone = (DefaultOHLCDataset) super.clone();
        clone.data = new OHLCDataItem[this.data.length];
        System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
        return clone;
    }
</implementation>
<implementation in="DefaultTableXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultTableXYDataset clone = (DefaultTableXYDataset) super.clone();
        int seriesCount = this.data.size();
        clone.data = new java.util.ArrayList(seriesCount);
        for (int i = 0; i < seriesCount; i++) {
            XYSeries series = (XYSeries) this.data.get(i);
            clone.data.add(series.clone());
        }

        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        clone.updateXPoints();
        return clone;
    }
</implementation>
</callee>
<callee name="setY" from="XYDataItem.java">
<signature>
public void setY(double y)
</signature>
<code>
    public void setY(double y) {
        setY(new Double(y));
    }
</code>
<implementation in="XYDataItem.java">
    public void setY(double y) {
        setY(new Double(y));
    }
</implementation>
<implementation in="XYDataItem.java">
    public void setY(Number y) {
        this.y = y;
    }
</implementation>
</callee>
<callee name="add" from="CategoryTableXYDataset.java">
<signature>
public void add(double x, double y, String seriesName)
</signature>
<code>
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
    }
</code>
<implementation in="CategoryTableXYDataset.java">
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
    }
</implementation>
<implementation in="CategoryTableXYDataset.java">
    public void add(Number x, Number y, String seriesName, boolean notify) {
        this.values.addValue(y, (Comparable) x, seriesName);
        if (notify) {
            fireDatasetChanged();
        }
    }
</implementation>
<implementation in="VectorSeries.java">
    public void add(double x, double y, double deltaX, double deltaY) {
        super.add(new VectorDataItem(x, y, deltaX, deltaY), true);
    }
</implementation>
<implementation in="XIntervalSeries.java">
    public void add(double x, double xLow, double xHigh, double y) {
        super.add(new XIntervalDataItem(x, xLow, xHigh, y), true);
    }
</implementation>
</callee>
<callee name="getItemCount" from="TableXYDataset.java" abstract="true">
<signature>
public int getItemCount()
</signature>
<contract>
    public int getItemCount();
</contract>
<implementation in="CategoryTableXYDataset.java">
    public int getItemCount() {
        return this.values.getRowCount();
    }
</implementation>
<implementation in="CategoryTableXYDataset.java">
    public int getItemCount(int series) {
        return getItemCount();  // all series have the same number of items in
                                // this dataset
    }
</implementation>
<implementation in="DefaultHighLowDataset.java">
    public int getItemCount(int series) {
        return this.date.length;
    }
</implementation>
<implementation in="DefaultIntervalXYDataset.java">
    public int getItemCount(int series) {
        if ((series < 0) || (series >= getSeriesCount())) {
            throw new IllegalArgumentException("Series index out of bounds");
        }
        double[][] seriesArray = (double[][]) this.seriesList.get(series);
        return seriesArray[0].length;
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `addOrUpdate` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `addOrUpdate`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public boolean equals(Object obj)` (shared field(s): allowDuplicateXValues, autoSort, data, maximumItemCount)
      doc: Tests this series for equality with an arbitrary object. @param obj  the object to test against for equality (<code>null</code> permitted). @return A boolean.
  - `public XYSeries(Comparable key, boolean autoSort, boolean allowDuplicateXValues)` (shared field(s): allowDuplicateXValues, autoSort, data)
      doc: Constructs a new xy-series that contains no data.  You can specify whether or not duplicate x-values are allowed for the series. @param key  the series key (<code>null</code> not permitted). @param autoSort  a flag that controls whether or not the items in the series are sorted. @param allowDuplicateXValues  a flag tha…
    <constructor_body>
    public XYSeries(Comparable key,
                    boolean autoSort,
                    boolean allowDuplicateXValues) {
        super(key);
        this.data = new java.util.ArrayList();
        this.autoSort = autoSort;
        this.allowDuplicateXValues = allowDuplicateXValues;
    }
    </constructor_body>
  - `public int hashCode()` (shared field(s): allowDuplicateXValues, autoSort, maximumItemCount)
      doc: Returns a hash code. @return A hash code.
  - `public void setMaximumItemCount(int maximum)` (shared field(s): data, maximumItemCount)
      doc: Sets the maximum number of items that will be retained in the series. If you add a new item to the series such that the number of items will exceed the maximum item count, then the first element in the series is automatically removed, ensuring that the maximum item count is not exceeded. <p> Typically this value is set…
  - `public boolean getAutoSort()` (shared field(s): autoSort)
      doc: Returns the flag that controls whether the items in the series are automatically sorted.  There is no setter for this flag, it must be defined in the series constructor. @return A boolean.

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `XYSeriesTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.IndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `addOrUpdate` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.IndexOutOfBoundsException
message: Index: -1, Size: 1
thrown_at: org.jfree.data.xy.XYSeries.addOrUpdate(XYSeries.java:564)
</ground_truth_crash>
<failing_test class="org.jfree.data.xy.junit.XYSeriesTests" method="testBug1955483">
    public void testBug1955483() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(1.0, 1.0);
        series.addOrUpdate(1.0, 2.0);
        assertEquals(new Double(1.0), series.getY(0));
        assertEquals(new Double(2.0), series.getY(1));
        assertEquals(2, series.getItemCount());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.data.xy.junit.XYSeriesTests::testBug1955483
java.lang.IndexOutOfBoundsException: Index: -1, Size: 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param key  the series key (<code>null</code> not permitted).
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  add(XYDataItem item) / (double x, double y) / (double x, double y, boolean notify) / (double x, Number y) / (double x, Number y, boolean notify) / (Number x, Number y) / (Number x, Number y, boolean notify) / (XYDataItem item, boolean notify)
  addOrUpdate(double x, double y) / (Number x, Number y)
  remove(int index) / (Number x)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getAllowDuplicateXValues, getAutoSort, getDataItem, getItemCount, getItems, getMaximumItemCount, getX, getY

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): clone, getAllowDuplicateXValues, getAutoSort, getItemCount, getItems, getMaximumItemCount, hashCode, toArray

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- XYSeries.indexOf
- SeriesException.<init>
- XYDataItem.<init>
- XYSeries.getItemCount
- XYSeries.fireSeriesChanged
- XYSeries.equals
- XYSeries.add
- DynamicTimeSeriesCollection.fireSeriesChanged
- TaskSeries.get
- XYSeries.remove
</root_cause_reachable>
First harness: establish the most direct path from the fuzz entrypoint through the patched code.
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

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
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,610 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
            runExplore(data);
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
            if (isValidation(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        XYSeries expected = new XYSeries("Series", true, true);

        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
        expected.add(Double.valueOf(1.0), Double.valueOf(1.0));

        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        expected.add(Double.valueOf(1.0), Double.valueOf(2.0));

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: expected 2 items after two duplicate-x insertions, got " + series.getItemCount());
        }
        if (numberToDouble(series.getY(0)) != 1.0d || numberToDouble(series.getY(1)) != 2.0d) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: duplicate-x insertion must retain both y-values in order: y0="
                    + series.getY(0) + " y1=" + series.getY(1));
        }

        checkSeriesConsistency("anchor", series, expected, true);
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int n = data.consumeInt(2, 8);
        int duplicateA = data.consumeInt(0, n - 1);
        int duplicateB = data.consumeInt(0, n - 1);
        if (duplicateA == duplicateB) {
            duplicateB = (duplicateA + 1) % n;
        }

        double duplicateX = boundedDouble(data.consumeInt(-1000, 1000));
        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        for (int i = 0; i < n; i++) {
            double x;
            if (i == duplicateA || i == duplicateB) {
                x = duplicateX;
            } else {
                x = boundedDouble(data.consumeInt(-1000, 1000));
            }
            double y = boundedDouble(data.consumeInt(-1000, 1000));

            viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
            viaAdd.add(Double.valueOf(x), Double.valueOf(y));

            checkSeriesConsistency("step-" + i, viaAddOrUpdate, viaAdd, true);

            int max = data.consumeInt(1, 16);
            viaAddOrUpdate.setMaximumItemCount(max);
            viaAdd.setMaximumItemCount(max);

            checkSeriesConsistency("step-max-" + i, viaAddOrUpdate, viaAdd, true);
        }
    }

    private static void checkSeriesConsistency(String tag, XYSeries actual, XYSeries expected, boolean expectedAutoSort) {
        if (actual.getAutoSort() != expectedAutoSort) {
            throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor-established autoSort changed for " + tag
                    + " actual=" + actual.getAutoSort() + " expected=" + expectedAutoSort);
        }

        /* Contract/invariant used for every correct implementation:
           - getItemCount() reports the number of retained items.
           - getItems() exposes that same retained data.
           A throw-deleting or wrong-insertion patch can leave bookkeeping/data inconsistent. */
        int reportedCount = actual.getItemCount();
        int empiricalCount = actual.getItems().size();
        if (reportedCount != empiricalCount) {
            throw new RuntimeException("[oracle:itemCount] metamorphic violation: reported count disagrees with getItems().size() for "
                    + tag + " reported=" + reportedCount + " empirical=" + empiricalCount);
        }

        /* Independent construction using a sibling public API:
           when allowDuplicateXValues=true, addOrUpdate(x,y) on a not-yet-updated duplicate path must add a new item,
           just as add(x,y) does. Two identically constructed series must therefore compare equal and expose identical
           item-by-item contents. */
        int expectedCount = expected.getItemCount();
        if (reportedCount != expectedCount) {
            throw new RuntimeException("[oracle:count-match] metamorphic violation: addOrUpdate-series count differs from add-series count for "
                    + tag + " lhs=" + reportedCount + " rhs=" + expectedCount);
        }

        for (int i = 0; i < reportedCount; i++) {
            Number ax = actual.getX(i);
            Number ex = expected.getX(i);
            Number ay = actual.getY(i);
            Number ey = expected.getY(i);
            if (!numbersEqual(ax, ex) || !numbersEqual(ay, ey)) {
                throw new RuntimeException("[oracle:item-match] metamorphic violation: addOrUpdate-series differs from add-series at index "
                        + i + " for " + tag + " lhs=(" + ax + "," + ay + ") rhs=(" + ex + "," + ey + ")");
            }
        }

        if (!actual.equals(expected) || !expected.equals(actual)) {
            throw new RuntimeException("[oracle:equals] metamorphic violation: identically constructed series are not equal for " + tag);
        }
        if (actual.hashCode() != expected.hashCode()) {
            throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode for " + tag
                    + " lhs=" + actual.hashCode() + " rhs=" + expected.hashCode());
        }

        try {
            Object cloned = actual.clone();
            if (cloned instanceof XYSeries) {
                XYSeries clone = (XYSeries) cloned;
                if (!actual.equals(clone) || actual.hashCode() != clone.hashCode()) {
                    throw new RuntimeException("[oracle:clone] metamorphic violation: series differs from its clone for " + tag);
                }
            }
        } catch (CloneNotSupportedException e) {
            return;
        }
    }

    private static boolean isOracle(RuntimeException e) {
        String m = e.getMessage();
        return m != null && m.startsWith("[oracle:");
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.endsWith("SeriesException")
                || name.contains("Validation")
                || name.contains("Invalid");
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String c = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(c)) {
                if ("addOrUpdate".equals(m)
                        || "indexOf".equals(m)
                        || "getItemCount".equals(m)
                        || "equals".equals(m)
                        || "add".equals(m)
                        || "remove".equals(m)) {
                    return true;
                }
            } else if ("org.jfree.data.xy.XYDataItem".equals(c) && "<init>".equals(m)) {
                return true;
            } else if ("org.jfree.data.general.SeriesException".equals(c) && "<init>".equals(m)) {
                return true;
            } else if ("org.jfree.data.time.DynamicTimeSeriesCollection".equals(c) && "fireSeriesChanged".equals(m)) {
                return true;
            } else if ("org.jfree.data.gantt.TaskSeries".equals(c) && "get".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean numbersEqual(Number a, Number b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static double numberToDouble(Number n) {
        return n == null ? Double.NaN : n.doubleValue();
    }

    private static double boundedDouble(int v) {
        return (double) v;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,794 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.jfree.data.xy` (`package org.jfree.data.xy;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_5_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source//org/jfree/data/xy/XYSeries.java
+++ /source//org/jfree/data/xy/XYSeries.java
@@ -561,7 +561,7 @@
             // new item...otherwise it will be just -1 and we should just
             // append the value to the list...
             if (this.autoSort) {
-                this.data.add(-index - 1, new XYDataItem(x, y));
+                org.jfree.data.xy.XYSeries.this.data.add(new org.jfree.data.xy.XYDataItem(x, y));
             }
             else {
                 this.data.add(new XYDataItem(x, y));

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.data.general.Series;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesException;
</source_imports>

Function `addOrUpdate`:
<signature>
public XYDataItem addOrUpdate(Number x, Number y)
</signature>
<code>
    public XYDataItem addOrUpdate(Number x, Number y) {
        if (x == null) {
            throw new IllegalArgumentException("Null 'x' argument.");
        }

        // if we get to here, we know that duplicate X values are not permitted
        XYDataItem overwritten = null;
        int index = indexOf(x);
        if (index >= 0 && !this.allowDuplicateXValues) {
            XYDataItem existing = (XYDataItem) this.data.get(index);
            try {
                overwritten = (XYDataItem) existing.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new SeriesException("Couldn't clone XYDataItem!");
            }
            existing.setY(y);
        }
        else {
            // if the series is sorted, the negative index is a result from
            // Collections.binarySearch() and tells us where to insert the
            // new item...otherwise it will be just -1 and we should just
            // append the value to the list...
            if (this.autoSort) {
                this.data.add(-index - 1, new XYDataItem(x, y));
            }
            else {
                this.data.add(new XYDataItem(x, y));
            }
            // check if this addition will exceed the maximum item count...
            if (getItemCount() > this.maximumItemCount) {
                this.data.remove(0);
            }
        }
        fireSeriesChanged();
        return overwritten;
    }
</code>
Methods called by `addOrUpdate` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="indexOf" from="DefaultXYDataset.java">
<signature>
public int indexOf(Comparable seriesKey)
</signature>
<code>
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</code>
<implementation in="DefaultXYDataset.java">
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</implementation>
<implementation in="DefaultXYZDataset.java">
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</implementation>
<implementation in="VectorSeriesCollection.java">
    public int indexOf(VectorSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Null 'series' argument.");
        }
        return this.data.indexOf(series);
    }
</implementation>
<implementation in="XYSeries.java">
    public int indexOf(Number x) {
        if (this.autoSort) {
            return Collections.binarySearch(this.data, new XYDataItem(x, null));
        }
        else {
            for (int i = 0; i < this.data.size(); i++) {
                XYDataItem item = (XYDataItem) this.data.get(i);
                if (item.getX().equals(x)) {
                    return i;
                }
            }
            return -1;
        }
    }
</implementation>
</callee>
<callee name="get" from="MatrixSeries.java">
<signature>
public double get(int i, int j)
</signature>
<code>
    public double get(int i, int j) {
        return this.data[i][j];
    }
</code>
</callee>
<callee name="clone" from="CategoryTableXYDataset.java">
<signature>
public Object clone() throws CloneNotSupportedException
</signature>
<code>
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        return clone;
    }
</code>
<implementation in="CategoryTableXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        return clone;
    }
</implementation>
<implementation in="DefaultIntervalXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultIntervalXYDataset clone
                = (DefaultIntervalXYDataset) super.clone();
        clone.seriesKeys = new java.util.ArrayList(this.seriesKeys);
        clone.seriesList = new ArrayList(this.seriesList.size());
        for (int i = 0; i < this.seriesList.size(); i++) {
            double[][] data = (double[][]) this.seriesList.get(i);
            double[] x = data[0];
            double[] xStart = data[1];
            double[] xEnd = data[2];
            double[] y = data[3];
            double[] yStart = data[4];
            double[] yEnd = data[5];
            double[] xx = new double[x.length];
            double[] xxStart = new double[xStart.length];
            double[] xxEnd = new double[xEnd.length];
            double[] yy = new double[y.length];
            double[] yyStart = new double[yStart.length];
            double[] yyEnd = new double[yEnd.length];
            System.arraycopy(x, 0, xx, 0, x.length);
            System.arraycopy(xStart, 0, xxStart, 0, xStart.length);
            System.arraycopy(xEnd, 0, xxEnd, 0, xEnd.length);
            System.arraycopy(y, 0, yy, 0, y.length);
            System.arraycopy(yStart, 0, yyStart, 0, yStart.length);
            System.arraycopy(yEnd, 0, yyEnd, 0, yEnd.length);
            clone.seriesList.add(i, new double[][] {xx, xxStart, xxEnd, yy,
                    yyStart, yyEnd});
        }
        return clone;
    }
</implementation>
<implementation in="DefaultOHLCDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultOHLCDataset clone = (DefaultOHLCDataset) super.clone();
        clone.data = new OHLCDataItem[this.data.length];
        System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
        return clone;
    }
</implementation>
<implementation in="DefaultTableXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultTableXYDataset clone = (DefaultTableXYDataset) super.clone();
        int seriesCount = this.data.size();
        clone.data = new java.util.ArrayList(seriesCount);
        for (int i = 0; i < seriesCount; i++) {
            XYSeries series = (XYSeries) this.data.get(i);
            clone.data.add(series.clone());
        }

        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        clone.updateXPoints();
        return clone;
    }
</implementation>
</callee>
<callee name="setY" from="XYDataItem.java">
<signature>
public void setY(double y)
</signature>
<code>
    public void setY(double y) {
        setY(new Double(y));
    }
</code>
<implementation in="XYDataItem.java">
    public void setY(double y) {
        setY(new Double(y));
    }
</implementation>
<implementation in="XYDataItem.java">
    public void setY(Number y) {
        this.y = y;
    }
</implementation>
</callee>
<callee name="add" from="CategoryTableXYDataset.java">
<signature>
public void add(double x, double y, String seriesName)
</signature>
<code>
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
    }
</code>
<implementation in="CategoryTableXYDataset.java">
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
    }
</implementation>
<implementation in="CategoryTableXYDataset.java">
    public void add(Number x, Number y, String seriesName, boolean notify) {
        this.values.addValue(y, (Comparable) x, seriesName);
        if (notify) {
            fireDatasetChanged();
        }
    }
</implementation>
<implementation in="VectorSeries.java">
    public void add(double x, double y, double deltaX, double deltaY) {
        super.add(new VectorDataItem(x, y, deltaX, deltaY), true);
    }
</implementation>
<implementation in="XIntervalSeries.java">
    public void add(double x, double xLow, double xHigh, double y) {
        super.add(new XIntervalDataItem(x, xLow, xHigh, y), true);
    }
</implementation>
</callee>
<callee name="getItemCount" from="TableXYDataset.java" abstract="true">
<signature>
public int getItemCount()
</signature>
<contract>
    public int getItemCount();
</contract>
<implementation in="CategoryTableXYDataset.java">
    public int getItemCount() {
        return this.values.getRowCount();
    }
</implementation>
<implementation in="CategoryTableXYDataset.java">
    public int getItemCount(int series) {
        return getItemCount();  // all series have the same number of items in
                                // this dataset
    }
</implementation>
<implementation in="DefaultHighLowDataset.java">
    public int getItemCount(int series) {
        return this.date.length;
    }
</implementation>
<implementation in="DefaultIntervalXYDataset.java">
    public int getItemCount(int series) {
        if ((series < 0) || (series >= getSeriesCount())) {
            throw new IllegalArgumentException("Series index out of bounds");
        }
        double[][] seriesArray = (double[][]) this.seriesList.get(series);
        return seriesArray[0].length;
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `addOrUpdate` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `addOrUpdate`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public boolean equals(Object obj)` (shared field(s): allowDuplicateXValues, autoSort, data, maximumItemCount)
      doc: Tests this series for equality with an arbitrary object. @param obj  the object to test against for equality (<code>null</code> permitted). @return A boolean.
  - `public XYSeries(Comparable key, boolean autoSort, boolean allowDuplicateXValues)` (shared field(s): allowDuplicateXValues, autoSort, data)
      doc: Constructs a new xy-series that contains no data.  You can specify whether or not duplicate x-values are allowed for the series. @param key  the series key (<code>null</code> not permitted). @param autoSort  a flag that controls whether or not the items in the series are sorted. @param allowDuplicateXValues  a flag tha…
    <constructor_body>
    public XYSeries(Comparable key,
                    boolean autoSort,
                    boolean allowDuplicateXValues) {
        super(key);
        this.data = new java.util.ArrayList();
        this.autoSort = autoSort;
        this.allowDuplicateXValues = allowDuplicateXValues;
    }
    </constructor_body>
  - `public int hashCode()` (shared field(s): allowDuplicateXValues, autoSort, maximumItemCount)
      doc: Returns a hash code. @return A hash code.
  - `public void setMaximumItemCount(int maximum)` (shared field(s): data, maximumItemCount)
      doc: Sets the maximum number of items that will be retained in the series. If you add a new item to the series such that the number of items will exceed the maximum item count, then the first element in the series is automatically removed, ensuring that the maximum item count is not exceeded. <p> Typically this value is set…
  - `public boolean getAutoSort()` (shared field(s): autoSort)
      doc: Returns the flag that controls whether the items in the series are automatically sorted.  There is no setter for this flag, it must be defined in the series constructor. @return A boolean.

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `XYSeriesTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.IndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `addOrUpdate` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.IndexOutOfBoundsException
message: Index: -1, Size: 1
thrown_at: org.jfree.data.xy.XYSeries.addOrUpdate(XYSeries.java:564)
</ground_truth_crash>
<failing_test class="org.jfree.data.xy.junit.XYSeriesTests" method="testBug1955483">
    public void testBug1955483() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(1.0, 1.0);
        series.addOrUpdate(1.0, 2.0);
        assertEquals(new Double(1.0), series.getY(0));
        assertEquals(new Double(2.0), series.getY(1));
        assertEquals(2, series.getItemCount());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.data.xy.junit.XYSeriesTests::testBug1955483
java.lang.IndexOutOfBoundsException: Index: -1, Size: 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param key  the series key (<code>null</code> not permitted).
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  add(XYDataItem item) / (double x, double y) / (double x, double y, boolean notify) / (double x, Number y) / (double x, Number y, boolean notify) / (Number x, Number y) / (Number x, Number y, boolean notify) / (XYDataItem item, boolean notify)
  addOrUpdate(double x, double y) / (Number x, Number y)
  remove(int index) / (Number x)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getAllowDuplicateXValues, getAutoSort, getDataItem, getItemCount, getItems, getMaximumItemCount, getX, getY

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): clone, getAllowDuplicateXValues, getAutoSort, getItemCount, getItems, getMaximumItemCount, hashCode, toArray

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- XYSeries.indexOf
- SeriesException.<init>
- XYDataItem.<init>
- XYSeries.getItemCount
- XYSeries.fireSeriesChanged
- XYSeries.equals
- XYSeries.add
- DynamicTimeSeriesCollection.fireSeriesChanged
- TaskSeries.get
- XYSeries.remove
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.jfree.data.xy.FuzzHarness.fuzzerTestOneInput
- org.jfree.data.xy.FuzzHarness.runAnchor
- org.jfree.data.xy.XYSeries.addOrUpdate
Crashes already found:
- java.lang.IndexOutOfBoundsException@org.jfree.data.xy.XYSeries.addOrUpdate
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- XYSeries.indexOf
- SeriesException.<init>
- XYDataItem.<init>
- XYSeries.getItemCount
- XYSeries.fireSeriesChanged
- XYSeries.equals
- XYSeries.add
- DynamicTimeSeriesCollection.fireSeriesChanged
- TaskSeries.get
- XYSeries.remove
Check FAMILIES already covered by accepted harnesses: {anchor-count, anchor-values, autosort, clone, count-match, equals, hash, item-match, itemcount}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.IndexOutOfBoundsException@org.jfree.data.xy.XYSeries.addOrUpdate. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

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
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,769 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorEquivalentOracle();
        runFuzzedAddVsAddOrUpdateOracle(data);
    }

    private static void runAnchorEquivalentOracle() {
        double[] xs = new double[] {1.0, 1.0};
        double[] ys = new double[] {1.0, 2.0};
        compareAddAgainstAddOrUpdate("Series", xs, ys);
    }

    private static void runFuzzedAddVsAddOrUpdateOracle(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 4);
        int suffixCount = data.consumeInt(0, 4);
        int total = prefixCount + 2 + suffixCount;

        double[] xs = new double[total];
        double[] ys = new double[total];

        int dupX = data.consumeInt(-1_000_000, 1_000_000);
        int y1 = data.consumeInt(-1_000_000, 1_000_000);
        int y2 = data.consumeInt(-1_000_000, 1_000_000);

        int pos = 0;
        for (int i = 0; i < prefixCount; i++) {
            int candidate = dupX - (i + 1);
            xs[pos] = candidate;
            ys[pos] = data.consumeInt(-1_000_000, 1_000_000);
            pos++;
        }

        xs[pos] = dupX;
        ys[pos] = y1;
        pos++;

        xs[pos] = dupX;
        ys[pos] = y2;
        pos++;

        for (int i = 0; i < suffixCount; i++) {
            int candidate = dupX + (i + 1);
            xs[pos] = candidate;
            ys[pos] = data.consumeInt(-1_000_000, 1_000_000);
            pos++;
        }

        compareAddAgainstAddOrUpdate(key, xs, ys);
    }

    private static void compareAddAgainstAddOrUpdate(String key, double[] xs, double[] ys) {
        XYSeries viaAdd = new XYSeries(key, true, true);
        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);

        for (int i = 0; i < xs.length; i++) {
            try {
                viaAdd.add(xs[i], ys[i]);
            } catch (RuntimeException e) {
                if (isCleanRejection(e)) {
                    return;
                }
                return;
            }

            try {
                if ((i & 1) == 0) {
                    viaAddOrUpdate.addOrUpdate(xs[i], ys[i]);
                } else {
                    viaAddOrUpdate.addOrUpdate(new Double(xs[i]), new Double(ys[i]));
                }
            } catch (RuntimeException e) {
                if (isCleanRejection(e)) {
                    return;
                }
                if (isRootCause(e)) {
                    throw new RuntimeException(
                            "[oracle:add-vs-update] metamorphic violation: "
                                    + "with allowDuplicateXValues=true, addOrUpdate(Number,Number) "
                                    + "must accept the same valid duplicate-x insertions as add(double,double); "
                                    + "inputIndex=" + i + " x=" + xs[i] + " y=" + ys[i],
                            e);
                }
                return;
            }
        }

        try {
            double[][] a = viaAdd.toArray();
            double[][] b = viaAddOrUpdate.toArray();
            if (!sameArray(a, b)) {
                throw new RuntimeException(
                        "[oracle:add-vs-update] metamorphic violation: "
                                + "series built by add(...) and addOrUpdate(...) disagree for the same valid inputs; "
                                + "add=" + arrayToString(a) + " addOrUpdate=" + arrayToString(b));
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            throw e;
        }

        try {
            XYSeries r1 = (XYSeries) viaAdd.clone();
            XYSeries r2 = (XYSeries) viaAddOrUpdate.clone();
            if (r1.getItemCount() > 0 && r2.getItemCount() > 0) {
                int idx = r1.indexOf(r1.getX(0));
                if (idx >= 0) {
                    XYDataItem byIndex = r1.remove(idx);
                    XYDataItem byNumber = r2.remove(r2.getX(0));
                    if (!sameItem(byIndex, byNumber) || !sameArray(r1.toArray(), r2.toArray())) {
                        throw new RuntimeException(
                                "[oracle:remove-overloads] metamorphic violation: "
                                        + "remove(int) and remove(Number) disagree on equivalent targets after duplicate handling; "
                                        + "byIndex=" + itemToString(byIndex)
                                        + " byNumber=" + itemToString(byNumber)
                                        + " left=" + arrayToString(r1.toArray())
                                        + " right=" + arrayToString(r2.toArray()));
                    }
                }
            }
        } catch (CloneNotSupportedException e) {
            return;
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            throw e;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof org.jfree.data.general.SeriesException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String m = stack[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)) {
                if ("addOrUpdate".equals(m)
                        || "indexOf".equals(m)
                        || "getItemCount".equals(m)
                        || "remove".equals(m)
                        || "add".equals(m)
                        || "equals".equals(m)
                        || "fireSeriesChanged".equals(m)) {
                    return true;
                }
            } else if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m)) {
                return true;
            } else if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameArray(double[][] a, double[][] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || b[i] == null || a[i].length != b[i].length) {
                return false;
            }
            for (int j = 0; j < a[i].length; j++) {
                if (Double.doubleToLongBits(a[i][j]) != Double.doubleToLongBits(b[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean sameItem(XYDataItem a, XYDataItem b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Number ax = a.getX();
        Number bx = b.getX();
        Number ay = a.getY();
        Number by = b.getY();
        return sameNumber(ax, bx) && sameNumber(ay, by);
    }

    private static boolean sameNumber(Number a, Number b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static String itemToString(XYDataItem item) {
        if (item == null) {
            return "null";
        }
        return "(" + item.getX() + "," + item.getY() + ")";
    }

    private static String arrayToString(double[][] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append('[');
            for (int j = 0; j < a[i].length; j++) {
                if (j > 0) {
                    sb.append(',');
                }
                sb.append(a[i][j]);
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:add-vs-update] metamorphic violation: with allowDuplicateXValues=true, addOrUpdate(Number,Number) must accept the same valid duplicate-x insertions as add(double,do…

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~37,121 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.jfree.data.xy` (`package org.jfree.data.xy;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_5_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source//org/jfree/data/xy/XYSeries.java
+++ /source//org/jfree/data/xy/XYSeries.java
@@ -561,7 +561,7 @@
             // new item...otherwise it will be just -1 and we should just
             // append the value to the list...
             if (this.autoSort) {
-                this.data.add(-index - 1, new XYDataItem(x, y));
+                org.jfree.data.xy.XYSeries.this.data.add(new org.jfree.data.xy.XYDataItem(x, y));
             }
             else {
                 this.data.add(new XYDataItem(x, y));

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.data.general.Series;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesException;
</source_imports>

Function `addOrUpdate`:
<signature>
public XYDataItem addOrUpdate(Number x, Number y)
</signature>
<code>
    public XYDataItem addOrUpdate(Number x, Number y) {
        if (x == null) {
            throw new IllegalArgumentException("Null 'x' argument.");
        }

        // if we get to here, we know that duplicate X values are not permitted
        XYDataItem overwritten = null;
        int index = indexOf(x);
        if (index >= 0 && !this.allowDuplicateXValues) {
            XYDataItem existing = (XYDataItem) this.data.get(index);
            try {
                overwritten = (XYDataItem) existing.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new SeriesException("Couldn't clone XYDataItem!");
            }
            existing.setY(y);
        }
        else {
            // if the series is sorted, the negative index is a result from
            // Collections.binarySearch() and tells us where to insert the
            // new item...otherwise it will be just -1 and we should just
            // append the value to the list...
            if (this.autoSort) {
                this.data.add(-index - 1, new XYDataItem(x, y));
            }
            else {
                this.data.add(new XYDataItem(x, y));
            }
            // check if this addition will exceed the maximum item count...
            if (getItemCount() > this.maximumItemCount) {
                this.data.remove(0);
            }
        }
        fireSeriesChanged();
        return overwritten;
    }
</code>
Methods called by `addOrUpdate` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="indexOf" from="DefaultXYDataset.java">
<signature>
public int indexOf(Comparable seriesKey)
</signature>
<code>
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</code>
<implementation in="DefaultXYDataset.java">
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</implementation>
<implementation in="DefaultXYZDataset.java">
    public int indexOf(Comparable seriesKey) {
        return this.seriesKeys.indexOf(seriesKey);
    }
</implementation>
<implementation in="VectorSeriesCollection.java">
    public int indexOf(VectorSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Null 'series' argument.");
        }
        return this.data.indexOf(series);
    }
</implementation>
<implementation in="XYSeries.java">
    public int indexOf(Number x) {
        if (this.autoSort) {
            return Collections.binarySearch(this.data, new XYDataItem(x, null));
        }
        else {
            for (int i = 0; i < this.data.size(); i++) {
                XYDataItem item = (XYDataItem) this.data.get(i);
                if (item.getX().equals(x)) {
                    return i;
                }
            }
            return -1;
        }
    }
</implementation>
</callee>
<callee name="get" from="MatrixSeries.java">
<signature>
public double get(int i, int j)
</signature>
<code>
    public double get(int i, int j) {
        return this.data[i][j];
    }
</code>
</callee>
<callee name="clone" from="CategoryTableXYDataset.java">
<signature>
public Object clone() throws CloneNotSupportedException
</signature>
<code>
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        return clone;
    }
</code>
<implementation in="CategoryTableXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        CategoryTableXYDataset clone = (CategoryTableXYDataset) super.clone();
        clone.values = (DefaultKeyedValues2D) this.values.clone();
        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        return clone;
    }
</implementation>
<implementation in="DefaultIntervalXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultIntervalXYDataset clone
                = (DefaultIntervalXYDataset) super.clone();
        clone.seriesKeys = new java.util.ArrayList(this.seriesKeys);
        clone.seriesList = new ArrayList(this.seriesList.size());
        for (int i = 0; i < this.seriesList.size(); i++) {
            double[][] data = (double[][]) this.seriesList.get(i);
            double[] x = data[0];
            double[] xStart = data[1];
            double[] xEnd = data[2];
            double[] y = data[3];
            double[] yStart = data[4];
            double[] yEnd = data[5];
            double[] xx = new double[x.length];
            double[] xxStart = new double[xStart.length];
            double[] xxEnd = new double[xEnd.length];
            double[] yy = new double[y.length];
            double[] yyStart = new double[yStart.length];
            double[] yyEnd = new double[yEnd.length];
            System.arraycopy(x, 0, xx, 0, x.length);
            System.arraycopy(xStart, 0, xxStart, 0, xStart.length);
            System.arraycopy(xEnd, 0, xxEnd, 0, xEnd.length);
            System.arraycopy(y, 0, yy, 0, y.length);
            System.arraycopy(yStart, 0, yyStart, 0, yStart.length);
            System.arraycopy(yEnd, 0, yyEnd, 0, yEnd.length);
            clone.seriesList.add(i, new double[][] {xx, xxStart, xxEnd, yy,
                    yyStart, yyEnd});
        }
        return clone;
    }
</implementation>
<implementation in="DefaultOHLCDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultOHLCDataset clone = (DefaultOHLCDataset) super.clone();
        clone.data = new OHLCDataItem[this.data.length];
        System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
        return clone;
    }
</implementation>
<implementation in="DefaultTableXYDataset.java">
    public Object clone() throws CloneNotSupportedException {
        DefaultTableXYDataset clone = (DefaultTableXYDataset) super.clone();
        int seriesCount = this.data.size();
        clone.data = new java.util.ArrayList(seriesCount);
        for (int i = 0; i < seriesCount; i++) {
            XYSeries series = (XYSeries) this.data.get(i);
            clone.data.add(series.clone());
        }

        clone.intervalDelegate = new IntervalXYDelegate(clone);
        // need to configure the intervalDelegate to match the original
        clone.intervalDelegate.setFixedIntervalWidth(getIntervalWidth());
        clone.intervalDelegate.setAutoWidth(isAutoWidth());
        clone.intervalDelegate.setIntervalPositionFactor(
                getIntervalPositionFactor());
        clone.updateXPoints();
        return clone;
    }
</implementation>
</callee>
<callee name="setY" from="XYDataItem.java">
<signature>
public void setY(double y)
</signature>
<code>
    public void setY(double y) {
        setY(new Double(y));
    }
</code>
<implementation in="XYDataItem.java">
    public void setY(double y) {
        setY(new Double(y));
    }
</implementation>
<implementation in="XYDataItem.java">
    public void setY(Number y) {
        this.y = y;
    }
</implementation>
</callee>
<callee name="add" from="CategoryTableXYDataset.java">
<signature>
public void add(double x, double y, String seriesName)
</signature>
<code>
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
    }
</code>
<implementation in="CategoryTableXYDataset.java">
    public void add(double x, double y, String seriesName) {
        add(new Double(x), new Double(y), seriesName, true);
    }
</implementation>
<implementation in="CategoryTableXYDataset.java">
    public void add(Number x, Number y, String seriesName, boolean notify) {
        this.values.addValue(y, (Comparable) x, seriesName);
        if (notify) {
            fireDatasetChanged();
        }
    }
</implementation>
<implementation in="VectorSeries.java">
    public void add(double x, double y, double deltaX, double deltaY) {
        super.add(new VectorDataItem(x, y, deltaX, deltaY), true);
    }
</implementation>
<implementation in="XIntervalSeries.java">
    public void add(double x, double xLow, double xHigh, double y) {
        super.add(new XIntervalDataItem(x, xLow, xHigh, y), true);
    }
</implementation>
</callee>
<callee name="getItemCount" from="TableXYDataset.java" abstract="true">
<signature>
public int getItemCount()
</signature>
<contract>
    public int getItemCount();
</contract>
<implementation in="CategoryTableXYDataset.java">
    public int getItemCount() {
        return this.values.getRowCount();
    }
</implementation>
<implementation in="CategoryTableXYDataset.java">
    public int getItemCount(int series) {
        return getItemCount();  // all series have the same number of items in
                                // this dataset
    }
</implementation>
<implementation in="DefaultHighLowDataset.java">
    public int getItemCount(int series) {
        return this.date.length;
    }
</implementation>
<implementation in="DefaultIntervalXYDataset.java">
    public int getItemCount(int series) {
        if ((series < 0) || (series >= getSeriesCount())) {
            throw new IllegalArgumentException("Series index out of bounds");
        }
        double[][] seriesArray = (double[][]) this.seriesList.get(series);
        return seriesArray[0].length;
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `addOrUpdate` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `addOrUpdate`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public boolean equals(Object obj)` (shared field(s): allowDuplicateXValues, autoSort, data, maximumItemCount)
      doc: Tests this series for equality with an arbitrary object. @param obj  the object to test against for equality (<code>null</code> permitted). @return A boolean.
  - `public XYSeries(Comparable key, boolean autoSort, boolean allowDuplicateXValues)` (shared field(s): allowDuplicateXValues, autoSort, data)
      doc: Constructs a new xy-series that contains no data.  You can specify whether or not duplicate x-values are allowed for the series. @param key  the series key (<code>null</code> not permitted). @param autoSort  a flag that controls whether or not the items in the series are sorted. @param allowDuplicateXValues  a flag tha…
    <constructor_body>
    public XYSeries(Comparable key,
                    boolean autoSort,
                    boolean allowDuplicateXValues) {
        super(key);
        this.data = new java.util.ArrayList();
        this.autoSort = autoSort;
        this.allowDuplicateXValues = allowDuplicateXValues;
    }
    </constructor_body>
  - `public int hashCode()` (shared field(s): allowDuplicateXValues, autoSort, maximumItemCount)
      doc: Returns a hash code. @return A hash code.
  - `public void setMaximumItemCount(int maximum)` (shared field(s): data, maximumItemCount)
      doc: Sets the maximum number of items that will be retained in the series. If you add a new item to the series such that the number of items will exceed the maximum item count, then the first element in the series is automatically removed, ensuring that the maximum item count is not exceeded. <p> Typically this value is set…
  - `public boolean getAutoSort()` (shared field(s): autoSort)
      doc: Returns the flag that controls whether the items in the series are automatically sorted.  There is no setter for this flag, it must be defined in the series constructor. @return A boolean.

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `XYSeriesTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.IndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `addOrUpdate` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.IndexOutOfBoundsException
message: Index: -1, Size: 1
thrown_at: org.jfree.data.xy.XYSeries.addOrUpdate(XYSeries.java:564)
</ground_truth_crash>
<failing_test class="org.jfree.data.xy.junit.XYSeriesTests" method="testBug1955483">
    public void testBug1955483() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(1.0, 1.0);
        series.addOrUpdate(1.0, 2.0);
        assertEquals(new Double(1.0), series.getY(0));
        assertEquals(new Double(2.0), series.getY(1));
        assertEquals(2, series.getItemCount());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.data.xy.junit.XYSeriesTests::testBug1955483
java.lang.IndexOutOfBoundsException: Index: -1, Size: 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param key  the series key (<code>null</code> not permitted).
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  add(XYDataItem item) / (double x, double y) / (double x, double y, boolean notify) / (double x, Number y) / (double x, Number y, boolean notify) / (Number x, Number y) / (Number x, Number y, boolean notify) / (XYDataItem item, boolean notify)
  addOrUpdate(double x, double y) / (Number x, Number y)
  remove(int index) / (Number x)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getAllowDuplicateXValues, getAutoSort, getDataItem, getItemCount, getItems, getMaximumItemCount, getX, getY

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): clone, getAllowDuplicateXValues, getAutoSort, getItemCount, getItems, getMaximumItemCount, hashCode, toArray

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- XYSeries.indexOf
- SeriesException.<init>
- XYDataItem.<init>
- XYSeries.getItemCount
- XYSeries.fireSeriesChanged
- XYSeries.equals
- XYSeries.add
- DynamicTimeSeriesCollection.fireSeriesChanged
- TaskSeries.get
- XYSeries.remove
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.jfree.data.xy.FuzzHarness.compareAddAgainstAddOrUpdate
- org.jfree.data.xy.FuzzHarness.fuzzerTestOneInput
- org.jfree.data.xy.FuzzHarness.runAnchor
- org.jfree.data.xy.FuzzHarness.runAnchorEquivalentOracle
- org.jfree.data.xy.XYSeries.addOrUpdate
Crashes already found:
- java.lang.IndexOutOfBoundsException@org.jfree.data.xy.XYSeries.addOrUpdate
- java.lang.RuntimeException@org.jfree.data.xy.FuzzHarness.compareAddAgainstAddOrUpdate
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- XYSeries.indexOf
- SeriesException.<init>
- XYDataItem.<init>
- XYSeries.getItemCount
- XYSeries.fireSeriesChanged
- XYSeries.equals
- XYSeries.add
- DynamicTimeSeriesCollection.fireSeriesChanged
- TaskSeries.get
- XYSeries.remove
Check FAMILIES already covered by accepted harnesses: {add-vs-update, anchor-count, anchor-values, autosort, clone, count-match, equals, hash, item-match, itemcount, remove-overloads}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.IndexOutOfBoundsException@org.jfree.data.xy.XYSeries.addOrUpdate; java.lang.RuntimeException@org.jfree.data.xy.FuzzHarness.compareAddAgainstAddOrUpdate. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

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
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~11,543 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            addOrUpdateMustSucceed(series, new Double(1.0), new Double(2.0), "dup-accept-anchor");
            verifyCommonConsistency(series, 1.0, 2, true);

            double[][] a = series.toArray();
            if (a.length < 2 || a[0].length != 2 || a[1].length != 2
                    || Double.compare(a[0][0], 1.0) != 0
                    || Double.compare(a[0][1], 1.0) != 0
                    || Double.compare(a[1][0], 1.0) != 0
                    || Double.compare(a[1][1], 2.0) != 0) {
                throw new RuntimeException("[oracle:anchor-toarray] metamorphic violation: "
                        + "failing-test duplicate addOrUpdate should leave two visible points in toArray; "
                        + "xs=" + describe(a, 0) + " ys=" + describe(a, 1));
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:dup-accept-anchor] metamorphic violation: "
                        + "valid duplicate addOrUpdate on an auto-sorted series with duplicate x-values allowed "
                        + "must succeed; caught root-cause exception " + e, e);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);

        int pivot = data.consumeInt(-1000, 1000);
        int lowerCount = data.consumeInt(0, 4);
        int higherCount = data.consumeInt(0, 4);
        int duplicateUpdates = data.consumeInt(1, 4);

        try {
            for (int i = lowerCount; i >= 1; i--) {
                double x = pivot - i;
                double y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y), data.consumeBoolean());
                verifyCommonConsistency(series, pivot, 0, false);
            }

            double initialY = bounded(data.consumeInt());
            series.add(new Double((double) pivot), new Double(initialY), data.consumeBoolean());
            verifyCommonConsistency(series, pivot, 1, false);

            for (int i = 1; i <= higherCount; i++) {
                double x = pivot + i;
                double y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y), data.consumeBoolean());
                verifyCommonConsistency(series, pivot, 1, false);
            }

            int idxBefore = series.indexOf(new Double((double) pivot));
            if (idxBefore < 0 || idxBefore >= series.getItemCount()) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf should locate an existing x after insertion; pivot=" + pivot
                        + " idx=" + idxBefore + " count=" + series.getItemCount());
            }
            if (Double.compare(series.getX(idxBefore).doubleValue(), (double) pivot) != 0) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf returned a slot with different x; pivot=" + pivot
                        + " got=" + series.getX(idxBefore));
            }

            for (int i = 0; i < duplicateUpdates; i++) {
                double y = bounded(data.consumeInt());
                addOrUpdateMustSucceed(series, new Double((double) pivot), new Double(y), "dup-accept-explore");
                verifyCommonConsistency(series, pivot, 2 + i, true);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:dup-accept-explore] metamorphic violation: "
                        + "valid duplicate addOrUpdate on an auto-sorted series with duplicate x-values allowed "
                        + "must not fail; pivot=" + pivot + " lower=" + lowerCount + " higher=" + higherCount
                        + " updates=" + duplicateUpdates, e);
            }
        }
    }

    private static void addOrUpdateMustSucceed(XYSeries series, Number x, Number y, String oracleId) {
        try {
            series.addOrUpdate(x, y);
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: "
                        + "valid duplicate addOrUpdate unexpectedly failed for x=" + x + " y=" + y, e);
            }
        }
    }

    private static void verifyCommonConsistency(XYSeries series, double duplicatedX, int minDuplicateOccurrences,
                                                boolean requirePivotVisible) {
        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-flag] metamorphic violation: constructor-set autoSort flag changed");
        }

        double[][] a = series.toArray();
        List items = series.getItems();
        int count = series.getItemCount();

        if (a == null || a.length < 2) {
            throw new RuntimeException("[oracle:view-sync] metamorphic violation: toArray shape invalid");
        }
        if (items.size() != count || a[0].length != count || a[1].length != count) {
            throw new RuntimeException("[oracle:view-sync] metamorphic violation: "
                    + "getItems/getItemCount/toArray disagree count=" + count
                    + " items=" + items.size() + " xs=" + a[0].length + " ys=" + a[1].length);
        }

        int dupSeen = 0;
        for (int i = 0; i < count; i++) {
            XYDataItem item = (XYDataItem) items.get(i);
            double xFromArray = a[0][i];
            double yFromArray = a[1][i];
            double xFromSeries = series.getX(i).doubleValue();
            double yFromSeries = series.getY(i).doubleValue();
            double xFromItem = item.getX().doubleValue();
            double yFromItem = item.getY().doubleValue();

            if (Double.compare(xFromArray, xFromSeries) != 0
                    || Double.compare(yFromArray, yFromSeries) != 0
                    || Double.compare(xFromArray, xFromItem) != 0
                    || Double.compare(yFromArray, yFromItem) != 0) {
                throw new RuntimeException("[oracle:view-sync] metamorphic violation: "
                        + "toArray/getX,getY/getItems disagree at i=" + i
                        + " array=(" + xFromArray + "," + yFromArray + ")"
                        + " series=(" + xFromSeries + "," + yFromSeries + ")"
                        + " item=(" + xFromItem + "," + yFromItem + ")");
            }

            if (i > 0 && Double.compare(a[0][i - 1], a[0][i]) > 0) {
                throw new RuntimeException("[oracle:autosort-order] metamorphic violation: "
                        + "constructor says autoSort controls whether items are automatically sorted; "
                        + "successful insertion left descending x-order at i=" + i
                        + " prev=" + a[0][i - 1] + " curr=" + a[0][i]);
            }

            if (Double.compare(xFromArray, duplicatedX) == 0) {
                dupSeen++;
            }
        }

        if (requirePivotVisible) {
            int idx = series.indexOf(new Double(duplicatedX));
            if (idx < 0 || idx >= count) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf should return a valid index for an existing x; x=" + duplicatedX
                        + " idx=" + idx + " count=" + count);
            }
            if (Double.compare(series.getX(idx).doubleValue(), duplicatedX) != 0) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf returned wrong slot for existing x=" + duplicatedX
                        + " got=" + series.getX(idx));
            }
        }

        if (dupSeen < minDuplicateOccurrences) {
            throw new RuntimeException("[oracle:dup-visible] metamorphic violation: "
                    + "duplicate x-values allowed but too few visible duplicates for x=" + duplicatedX
                    + " expectedAtLeast=" + minDuplicateOccurrences + " seen=" + dupSeen);
        }
    }

    private static boolean isRootCauseCrash(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (("org.jfree.data.xy.XYSeries".equals(cls) && "addOrUpdate".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "indexOf".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "getItemCount".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "add".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "remove".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "fireSeriesChanged".equals(m))
                    || ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m))
                    || ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static double bounded(int v) {
        return (double) (v % 1000000);
    }

    private static String describe(double[][] a, int row) {
        if (a == null || row < 0 || row >= a.length || a[row] == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (int i = 0; i < a[row].length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[row][i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~13,066 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
**[assistant]**
```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            addOrUpdateMustSucceed(series, new Double(1.0), new Double(2.0), "dup-accept-anchor");
            verifyCommonConsistency(series, 1.0, 2, true);

            double[][] a = series.toArray();
            if (a.length < 2 || a[0].length != 2 || a[1].length != 2
                    || Double.compare(a[0][0], 1.0) != 0
                    || Double.compare(a[0][1], 1.0) != 0
                    || Double.compare(a[1][0], 1.0) != 0
                    || Double.compare(a[1][1], 2.0) != 0) {
                throw new RuntimeException("[oracle:anchor-toarray] metamorphic violation: "
                        + "failing-test duplicate addOrUpdate should leave two visible points in toArray; "
                        + "xs=" + describe(a, 0) + " ys=" + describe(a, 1));
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:dup-accept-anchor] metamorphic violation: "
                        + "valid duplicate addOrUpdate on an auto-sorted series with duplicate x-values allowed "
                        + "must succeed; caught root-cause exception " + e, e);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);

        int pivot = data.consumeInt(-1000, 1000);
        int lowerCount = data.consumeInt(0, 4);
        int higherCount = data.consumeInt(0, 4);
        int duplicateUpdates = data.consumeInt(1, 4);

        try {
            for (int i = lowerCount; i >= 1; i--) {
                double x = pivot - i;
                double y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y), data.consumeBoolean());
                verifyCommonConsistency(series, pivot, 0, false);
            }

            double initialY = bounded(data.consumeInt());
            series.add(new Double((double) pivot), new Double(initialY), data.consumeBoolean());
            verifyCommonConsistency(series, pivot, 1, false);

            for (int i = 1; i <= higherCount; i++) {
                double x = pivot + i;
                double y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y), data.consumeBoolean());
                verifyCommonConsistency(series, pivot, 1, false);
            }

            int idxBefore = series.indexOf(new Double((double) pivot));
            if (idxBefore < 0 || idxBefore >= series.getItemCount()) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf should locate an existing x after insertion; pivot=" + pivot
                        + " idx=" + idxBefore + " count=" + series.getItemCount());
            }
            if (Double.compare(series.getX(idxBefore).doubleValue(), (double) pivot) != 0) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf returned a slot with different x; pivot=" + pivot
                        + " got=" + series.getX(idxBefore));
            }

            for (int i = 0; i < duplicateUpdates; i++) {
                double y = bounded(data.consumeInt());
                addOrUpdateMustSucceed(series, new Double((double) pivot), new Double(y), "dup-accept-explore");
                verifyCommonConsistency(series, pivot, 2 + i, true);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:dup-accept-explore] metamorphic violation: "
                        + "valid duplicate addOrUpdate on an auto-sorted series with duplicate x-values allowed "
                        + "must not fail; pivot=" + pivot + " lower=" + lowerCount + " higher=" + higherCount
                        + " updates=" + duplicateUpdates, e);
            }
        }
    }

    private static void addOrUpdateMustSucceed(XYSeries series, Number x, Number y, String oracleId) {
        try {
            series.addOrUpdate(x, y);
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: "
                        + "valid duplicate addOrUpdate unexpectedly failed for x=" + x + " y=" + y, e);
            }
        }
    }

    private static void verifyCommonConsistency(XYSeries series, double duplicatedX, int minDuplicateOccurrences,
                                                boolean requirePivotVisible) {
        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-flag] metamorphic violation: constructor-set autoSort flag changed");
        }

        double[][] a = series.toArray();
        List items = series.getItems();
        int count = series.getItemCount();

        if (a == null || a.length < 2) {
            throw new RuntimeException("[oracle:view-sync] metamorphic violation: toArray shape invalid");
        }
        if (items.size() != count || a[0].length != count || a[1].length != count) {
            throw new RuntimeException("[oracle:view-sync] metamorphic violation: "
                    + "getItems/getItemCount/toArray disagree count=" + count
                    + " items=" + items.size() + " xs=" + a[0].length + " ys=" + a[1].length);
        }

        int dupSeen = 0;
        for (int i = 0; i < count; i++) {
            XYDataItem item = (XYDataItem) items.get(i);
            double xFromArray = a[0][i];
            double yFromArray = a[1][i];
            double xFromSeries = series.getX(i).doubleValue();
            double yFromSeries = series.getY(i).doubleValue();
            double xFromItem = item.getX().doubleValue();
            double yFromItem = item.getY().doubleValue();

            if (Double.compare(xFromArray, xFromSeries) != 0
                    || Double.compare(yFromArray, yFromSeries) != 0
                    || Double.compare(xFromArray, xFromItem) != 0
                    || Double.compare(yFromArray, yFromItem) != 0) {
                throw new RuntimeException("[oracle:view-sync] metamorphic violation: "
                        + "toArray/getX,getY/getItems disagree at i=" + i
                        + " array=(" + xFromArray + "," + yFromArray + ")"
                        + " series=(" + xFromSeries + "," + yFromSeries + ")"
                        + " item=(" + xFromItem + "," + yFromItem + ")");
            }

            if (i > 0 && Double.compare(a[0][i - 1], a[0][i]) > 0) {
                throw new RuntimeException("[oracle:autosort-order] metamorphic violation: "
                        + "constructor says autoSort controls whether items are automatically sorted; "
                        + "successful insertion left descending x-order at i=" + i
                        + " prev=" + a[0][i - 1] + " curr=" + a[0][i]);
            }

            if (Double.compare(xFromArray, duplicatedX) == 0) {
                dupSeen++;
            }
        }

        if (requirePivotVisible) {
            int idx = series.indexOf(new Double(duplicatedX));
            if (idx < 0 || idx >= count) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf should return a valid index for an existing x; x=" + duplicatedX
                        + " idx=" + idx + " count=" + count);
            }
            if (Double.compare(series.getX(idx).doubleValue(), duplicatedX) != 0) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf returned wrong slot for existing x=" + duplicatedX
                        + " got=" + series.getX(idx));
            }
        }

        if (dupSeen < minDuplicateOccurrences) {
            throw new RuntimeException("[oracle:dup-visible] metamorphic violation: "
                    + "duplicate x-values allowed but too few visible duplicates for x=" + duplicatedX
                    + " expectedAtLeast=" + minDuplicateOccurrences + " seen=" + dupSeen);
        }
    }

    private static boolean isRootCauseCrash(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (("org.jfree.data.xy.XYSeries".equals(cls) && "addOrUpdate".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "indexOf".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "getItemCount".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "add".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "remove".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "fireSeriesChanged".equals(m))
                    || ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m))
                    || ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static double bounded(int v) {
        return (double) (v % 1000000);
    }

    private static String describe(double[][] a, int row) {
        if (a == null || row < 0 || row >= a.length || a[row] == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (int i = 0; i < a[row].length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[row][i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/tmp/d4j/Chart_5_buggy/fuzz/attempt_003/FuzzHarness.java:31: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
/tmp/d4j/Chart_5_buggy/fuzz/attempt_003/FuzzHarness.java:98: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
/tmp/d4j/Chart_5_buggy/fuzz/attempt_003/FuzzHarness.java:120: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
3 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~11,351 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            addOrUpdateMustSucceed(series, new Double(1.0), new Double(2.0), "dup-accept-anchor");
            verifyCommonConsistency(series, 1.0, 2, true);

            double[][] a = series.toArray();
            if (a.length < 2 || a[0].length != 2 || a[1].length != 2
                    || Double.compare(a[0][0], 1.0) != 0
                    || Double.compare(a[0][1], 1.0) != 0
                    || Double.compare(a[1][0], 1.0) != 0
                    || Double.compare(a[1][1], 2.0) != 0) {
                throw new RuntimeException("[oracle:anchor-toarray] metamorphic violation: "
                        + "failing-test duplicate addOrUpdate should leave two visible points in toArray; "
                        + "xs=" + describe(a, 0) + " ys=" + describe(a, 1));
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:dup-accept-anchor] metamorphic violation: "
                        + "valid duplicate addOrUpdate on an auto-sorted series with duplicate x-values allowed "
                        + "must succeed; caught root-cause exception " + e, e);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);

        int pivot = data.consumeInt(-1000, 1000);
        int lowerCount = data.consumeInt(0, 4);
        int higherCount = data.consumeInt(0, 4);
        int duplicateUpdates = data.consumeInt(1, 4);

        try {
            for (int i = lowerCount; i >= 1; i--) {
                double x = pivot - i;
                double y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y), data.consumeBoolean());
                verifyCommonConsistency(series, pivot, 0, false);
            }

            double initialY = bounded(data.consumeInt());
            series.add(new Double((double) pivot), new Double(initialY), data.consumeBoolean());
            verifyCommonConsistency(series, pivot, 1, false);

            for (int i = 1; i <= higherCount; i++) {
                double x = pivot + i;
                double y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y), data.consumeBoolean());
                verifyCommonConsistency(series, pivot, 1, false);
            }

            int idxBefore = series.indexOf(new Double((double) pivot));
            if (idxBefore < 0 || idxBefore >= series.getItemCount()) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf should locate an existing x after insertion; pivot=" + pivot
                        + " idx=" + idxBefore + " count=" + series.getItemCount());
            }
            if (Double.compare(series.getX(idxBefore).doubleValue(), (double) pivot) != 0) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf returned a slot with different x; pivot=" + pivot
                        + " got=" + series.getX(idxBefore));
            }

            for (int i = 0; i < duplicateUpdates; i++) {
                double y = bounded(data.consumeInt());
                addOrUpdateMustSucceed(series, new Double((double) pivot), new Double(y), "dup-accept-explore");
                verifyCommonConsistency(series, pivot, 2 + i, true);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:dup-accept-explore] metamorphic violation: "
                        + "valid duplicate addOrUpdate on an auto-sorted series with duplicate x-values allowed "
                        + "must not fail; pivot=" + pivot + " lower=" + lowerCount + " higher=" + higherCount
                        + " updates=" + duplicateUpdates, e);
            }
        }
    }

    private static void addOrUpdateMustSucceed(XYSeries series, Number x, Number y, String oracleId) {
        try {
            series.addOrUpdate(x, y);
        } catch (IllegalArgumentException e) {
            return;
        } catch (org.jfree.data.general.SeriesException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCauseCrash(e)) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: "
                        + "valid duplicate addOrUpdate unexpectedly failed for x=" + x + " y=" + y, e);
            }
        }
    }

    private static void verifyCommonConsistency(XYSeries series, double duplicatedX, int minDuplicateOccurrences,
                                                boolean requirePivotVisible) {
        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-flag] metamorphic violation: constructor-set autoSort flag changed");
        }

        double[][] a = series.toArray();
        List items = series.getItems();
        int count = series.getItemCount();

        if (a == null || a.length < 2) {
            throw new RuntimeException("[oracle:view-sync] metamorphic violation: toArray shape invalid");
        }
        if (items.size() != count || a[0].length != count || a[1].length != count) {
            throw new RuntimeException("[oracle:view-sync] metamorphic violation: "
                    + "getItems/getItemCount/toArray disagree count=" + count
                    + " items=" + items.size() + " xs=" + a[0].length + " ys=" + a[1].length);
        }

        int dupSeen = 0;
        for (int i = 0; i < count; i++) {
            XYDataItem item = (XYDataItem) items.get(i);
            double xFromArray = a[0][i];
            double yFromArray = a[1][i];
            double xFromSeries = series.getX(i).doubleValue();
            double yFromSeries = series.getY(i).doubleValue();
            double xFromItem = item.getX().doubleValue();
            double yFromItem = item.getY().doubleValue();

            if (Double.compare(xFromArray, xFromSeries) != 0
                    || Double.compare(yFromArray, yFromSeries) != 0
                    || Double.compare(xFromArray, xFromItem) != 0
                    || Double.compare(yFromArray, yFromItem) != 0) {
                throw new RuntimeException("[oracle:view-sync] metamorphic violation: "
                        + "toArray/getX,getY/getItems disagree at i=" + i
                        + " array=(" + xFromArray + "," + yFromArray + ")"
                        + " series=(" + xFromSeries + "," + yFromSeries + ")"
                        + " item=(" + xFromItem + "," + yFromItem + ")");
            }

            if (i > 0 && Double.compare(a[0][i - 1], a[0][i]) > 0) {
                throw new RuntimeException("[oracle:autosort-order] metamorphic violation: "
                        + "constructor says autoSort controls whether items are automatically sorted; "
                        + "successful insertion left descending x-order at i=" + i
                        + " prev=" + a[0][i - 1] + " curr=" + a[0][i]);
            }

            if (Double.compare(xFromArray, duplicatedX) == 0) {
                dupSeen++;
            }
        }

        if (requirePivotVisible) {
            int idx = series.indexOf(new Double(duplicatedX));
            if (idx < 0 || idx >= count) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf should return a valid index for an existing x; x=" + duplicatedX
                        + " idx=" + idx + " count=" + count);
            }
            if (Double.compare(series.getX(idx).doubleValue(), duplicatedX) != 0) {
                throw new RuntimeException("[oracle:indexof-live] metamorphic violation: "
                        + "indexOf returned wrong slot for existing x=" + duplicatedX
                        + " got=" + series.getX(idx));
            }
        }

        if (dupSeen < minDuplicateOccurrences) {
            throw new RuntimeException("[oracle:dup-visible] metamorphic violation: "
                    + "duplicate x-values allowed but too few visible duplicates for x=" + duplicatedX
                    + " expectedAtLeast=" + minDuplicateOccurrences + " seen=" + dupSeen);
        }
    }

    private static boolean isRootCauseCrash(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (("org.jfree.data.xy.XYSeries".equals(cls) && "addOrUpdate".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "indexOf".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "getItemCount".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "add".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "remove".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "fireSeriesChanged".equals(m))
                    || ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m))
                    || ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static double bounded(int v) {
        return (double) (v % 1000000);
    }

    private static String describe(double[][] a, int row) {
        if (a == null || row < 0 || row >= a.length || a[row] == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (int i = 0; i < a[row].length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[row][i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:dup-accept-anchor] metamorphic violation: valid duplicate addOrUpdate unexpectedly failed for x=1.0 y=2.0

---
## [11] ⚙️ corpus-seed · `attempt_001`
**output:** **17 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['Series', '1.0', '2.0', '1.0L', '1.0l', '1.0F', '1.0f', '1.0D']}

---
## [12] ⚙️ corpus-seed · `attempt_002`
**output:** **17 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['Series', '1.0', '2.0', '1.0L', '1.0l', '1.0F', '1.0f', '1.0D']}

---
## [13] ⚙️ corpus-seed · `attempt_004`
**output:** **17 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['Series', '1.0', '2.0', '1.0L', '1.0l', '1.0F', '1.0f', '1.0D']}

---
## [14] ⚙️ patched-fuzz · `attempt_001`
**output:** **FIRED — [oracle:item-match] metamorphic violation: addOrUpdate-series differs from add-series at index 0 for step-2 lhs=(-1000.0,-1000.0) rhs=(294.0,-1000.0)**
- reproducing_input_file: /tmp/d4j/Chart_5_buggy/fuzz/attempt_001/crashes/crash-be8bdc5a4c4038f26e15811ee189fca30482669c

---
## [15] ⚙️ patched-fuzz · `attempt_002`
**output:** 
```
FIRED — [oracle:add-vs-update] metamorphic violation: series built by add(...) and addOrUpdate(...) disagree for the same valid inputs; add=[[-1000003.0,-1000002.0,-1000001.0,-1000000.0,-1000000.0];[-1000000.0,-1000000.0,-1000000.0,-1000000.0,-1000000.0]] addOrUpdate=[[-1000001.0,-1000002.0,-1000003.0,-1000000.0,-1000000.0];[-1000000.0,-1000000.0,-1000000.0,-1000000.0,-1000000.0]]
```
- reproducing_input_file: /tmp/d4j/Chart_5_buggy/fuzz/attempt_002/crashes/crash-e77e6c64c8444609713be7ff316fe6bb97afd824

---
## [16] ⚙️ patched-fuzz · `attempt_004`
**output:** 
```
FIRED — [oracle:autosort-order] metamorphic violation: constructor says autoSort controls whether items are automatically sorted; successful insertion left descending x-order at i=9 prev=-967.0 curr=-971.0
```
- reproducing_input_file: /tmp/d4j/Chart_5_buggy/fuzz/attempt_004/crashes/crash-0ec61fff2b4f0908333db0723a95eb977e2cfd21
