# Pipeline trace — Chart-5

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /source/org/jfree/data/xy/XYSeries.java
+++ /source/org/jfree/data/xy/XYSeries.java
@@ -560,12 +560,7 @@
             // Collections.binarySearch() and tells us where to insert the
             // new item...otherwise it will be just -1 and we should just
             // append the value to the list...
-            if (this.autoSort) {
-                this.data.add(-index - 1, new XYDataItem(x, y));
-            }
-            else {
-                this.data.add(new XYDataItem(x, y));
-            }
+        		add(x,y,true);
             // check if this addition will exceed the maximum item count...
             if (getItemCount() > this.maximumItemCount) {
                 this.data.remove(0);
```

64 sequential steps — 30 LLM calls, 34 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.jfree.data.xy.junit.XYSeriesTests', test_method='testBug1955483', source (~761 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.jfree.data.xy.junit.XYSeriesTests', test_method='testBug1955483', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/Chart_5_buggy/tests/org/jfree/data/xy/junit/XYSeriesTests.java', method_source='    public void testBug1955483() {\\n        XYSeries series = new XYSeries(\"Series\", true, true);\\n        series.addOrUpdate(1.0, 1.0);\\n        series.addOrUpdate(1.0, 2.0);\\n        assertEquals(new Double(1.0), series.getY(0));\\n        assertEquals(new Double(2.0), series.getY(1));\\n        assertEquals(2, series.getItemCount());\\n    }', exception_type='java.lang.IndexOutOfBoundsException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~25,484 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "source/org/jfree/data/xy/XYSeries.java"
  ],
  "patch_text": "--- /source/org/jfree/data/xy/XYSeries.java\n+++ /source/org/jfree/data/xy/XYSeries.java\n@@ -560,12 +560,7 @@\n             // Collections.binarySearch() and tells us where to insert the\n             // new item...otherwise it will be just -1 and we should just\n             // append the value to the list...\n-            if (this.autoSort) {\n-                this.data.add(-index - 1, new XYDataItem(x, y));\n-            }\n-            else {\n-                this.data.add(new XYDataItem(x, y));\n-            }\n+        \t\tadd(x,y,true);\n             // check if this addition will exceed the maximum item count...\n             if (getItemCount() > this.maximumItemCount) {\n                 this.data.remove(0);\n\n\n",
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
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[IllegalArgumentException].<init>(String)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[org.jfree.data.xy.XYSeries].indexOf(Number)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[java.util.List].get(int)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "existing.clone()"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[org.jfree.data.general.SeriesException].<init>(String)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "existing.setY(Number)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[java.util.List].add(int,org.jfree.data.xy.XYDataItem)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[java.util.List].add(org.jfree.data.xy.XYDataItem)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[org.jfree.data.xy.XYSeries].getItemCount()"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[java.util.List].remove(int)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
          "[org.jfree.data.xy.XYSeries].fireSeriesChanged()"
        ],
        [
          "[org.jfree.data.xy.XYSeries].indexOf(Number)",
          "[org.jfree.data.xy.XYDataItem].<init>(Number,null)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].indexOf(Number)",
          "[java.util.Collections].binarySearch(java.util.List,org.jfree.data.xy.XYDataItem)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].indexOf(Number)",
          "[java.util.List].size()"
        ],
        [
          "[org.jfree.data.xy.XYSeries].indexOf(Number)",
          "[java.util.List].get(org.jfree.data.xy.XYSeries)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].indexOf(Number)",
          "item.getX()"
        ],
        [
          "[org.jfree.data.xy.XYSeries].indexOf(Number)",
          "[org.jfree.data.xy.XYSeries].equals(Number)"
        ],
        [
          "[org.jfree.data.general.SeriesException].<init>(String)",
          "[RuntimeException].<init>(String)"
        ],
        [
          "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)",
          "[IllegalArgumentException].<init>(String)"
        ],
        [
          "[org.jfree.data.xy.XYSeries].getItemCount()",
          "[java.util.List].size()"
        ]
      ],
      "reachable_depth": {
        "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)": 0,
        "[IllegalArgumentException].<init>(String)": 1,
        "[org.jfree.data.xy.XYSeries].indexOf(Number)": 1,
        "[java.util.List].get(int)": 1,
        "existing.clone()": 1,
        "[org.jfree.data.general.SeriesException].<init>(String)": 1,
        "existing.setY(Number)": 1,
        "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)": 1,
        "[java.util.List].add(int,org.jfree.data.xy.XYDataItem)": 1,
        "[java.util.List].add(org.jfree.data.xy.XYDataItem)": 1,
        "[org.jfree.data.xy.XYSeries].getItemCount()": 1,
        "[java.util.List].remove(int)": 1,
        "[org.jfree.data.xy.XYSeries].fireSeriesChanged()": 1,
        "[org.jfree.data.xy.XYDataItem].<init>(Number,null)": 2,
        "[java.util.Collections].binarySearch(java.util.List,org.jfree.data.xy.XYDataItem)": 2,
        "[java.util.List].size()": 2,
        "[java.util.List].get(org.jfree.data.xy.XYSeries)": 2,
        "item.getX()": 2,
        "[org.jfree.data.xy.XYSeries].equals(Number)": 2,
        "[RuntimeException].<init>(String)": 2
      },
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
        "[org.jfree.data.general.Series].fireSeriesChanged()",
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
    "Series.fireSeriesChanged",
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
  ],
  "reachable_edges": [
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[IllegalArgumentException].<init>(String)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[org.jfree.data.xy.XYSeries].indexOf(Number)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[java.util.List].get(int)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "existing.clone()"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[org.jfree.data.general.SeriesException].<init>(String)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "existing.setY(Number)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[java.util.List].add(int,org.jfree.data.xy.XYDataItem)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[java.util.List].add(org.jfree.data.xy.XYDataItem)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[org.jfree.data.xy.XYSeries].getItemCount()"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[java.util.List].remove(int)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)",
      "[org.jfree.data.xy.XYSeries].fireSeriesChanged()"
    ],
    [
      "[org.jfree.data.xy.XYSeries].indexOf(Number)",
      "[org.jfree.data.xy.XYDataItem].<init>(Number,null)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].indexOf(Number)",
      "[java.util.Collections].binarySearch(java.util.List,org.jfree.data.xy.XYDataItem)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].indexOf(Number)",
      "[java.util.List].size()"
    ],
    [
      "[org.jfree.data.xy.XYSeries].indexOf(Number)",
      "[java.util.List].get(org.jfree.data.xy.XYSeries)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].indexOf(Number)",
      "item.getX()"
    ],
    [
      "[org.jfree.data.xy.XYSeries].indexOf(Number)",
      "[org.jfree.data.xy.XYSeries].equals(Number)"
    ],
    [
      "[org.jfree.data.general.SeriesException].<init>(String)",
      "[RuntimeException].<init>(String)"
    ],
    [
      "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)",
      "[IllegalArgumentException].<init>(String)"
    ],
    [
      "[org.jfree.data.xy.XYSeries].getItemCount()",
      "[java.util.List].size()"
    ]
  ],
  "reachable_depth": {
    "[org.jfree.data.xy.XYSeries].addOrUpdate(Number,Number)": 0,
    "[IllegalArgumentException].<init>(String)": 1,
    "[org.jfree.data.xy.XYSeries].indexOf(Number)": 1,
    "[java.util.List].get(int)": 1,
    "existing.clone()": 1,
    "[org.jfree.data.general.SeriesException].<init>(String)": 1,
    "existing.setY(Number)": 1,
    "[org.jfree.data.xy.XYDataItem].<init>(Number,Number)": 1,
    "[java.util.List].add(int,org.jfree.data.xy.XYDataItem)": 1,
    "[java.util.List].add(org.jfree.data.xy.XYDataItem)": 1,
    "[org.jfree.data.xy.XYSeries].getItemCount()": 1,
    "[java.util.List].remove(int)": 1,
    "[org.jfree.data.xy.XYSeries].fireSeriesChanged()": 1,
    "[org.jfree.data.xy.XYDataItem].<init>(Number,null)": 2,
    "[java.util.Collections].binarySearch(java.util.List,org.jfree.data.xy.XYDataItem)": 2,
    "[java.util.List].size()": 2,
    "[java.util.List].get(org.jfree.data.xy.XYSeries)": 2,
    "item.getX()": 2,
    "[org.jfree.data.xy.XYSeries].equals(Number)": 2,
    "[RuntimeException].<init>(String)": 2
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~4,308 chars)</summary>

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

Codebase: `Chart_5_buggy`. Write a fuzz harness for the function(s) shown below.

Call them through the library's real public API, with inputs built from the FuzzedDataProvider, and exercise as much of each function's behaviour as you can: vary lengths, sizes, signs and contents, cover empty and boundary values, and try the argument combinations the signature allows. Construct whatever real library objects the call needs. A throwable that escapes fuzzerTestOneInput is reported as a finding, so let exceptions from the code under test propagate.

Function `addOrUpdate` — declared in `org.jfree.data.xy.XYSeries`:
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
<details open><summary>▸ Output (~8,627 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(20);
        XYSeries series = new XYSeries(key1, data.consumeBoolean(), data.consumeBoolean());
        series.setMaximumItemCount(data.consumeInt(0, 8));

        int ops = data.consumeInt(0, 16);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        x = Double.valueOf((double) data.consumeInt());
                        break;
                    case 3:
                        x = Float.valueOf((float) data.consumeInt());
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        x = Double.valueOf(Double.NaN);
                        break;
                    case 7:
                        x = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    case 8:
                        x = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                    default:
                        x = Integer.valueOf(0);
                        break;
                }
            }

            Number y;
            switch (data.consumeInt(0, 10)) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    y = Long.valueOf((long) data.consumeInt());
                    break;
                case 3:
                    y = Double.valueOf((double) data.consumeInt());
                    break;
                case 4:
                    y = Float.valueOf((float) data.consumeInt());
                    break;
                case 5:
                    y = Short.valueOf((short) data.consumeInt());
                    break;
                case 6:
                    y = Byte.valueOf(data.consumeByte());
                    break;
                case 7:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 8:
                    y = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 9:
                    y = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    y = Integer.valueOf(0);
                    break;
            }

            series.addOrUpdate(x, y);

            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 8));
            }
        }

        String key2 = data.consumeString(20);
        XYSeries bugSeries = new XYSeries(key2, true, true);
        bugSeries.setMaximumItemCount(data.consumeInt(1, 8));

        Number bugX;
        switch (data.consumeInt(0, 9)) {
            case 0:
                bugX = Integer.valueOf(data.consumeInt());
                break;
            case 1:
                bugX = Long.valueOf((long) data.consumeInt());
                break;
            case 2:
                bugX = Double.valueOf((double) data.consumeInt());
                break;
            case 3:
                bugX = Float.valueOf((float) data.consumeInt());
                break;
            case 4:
                bugX = Short.valueOf((short) data.consumeInt());
                break;
            case 5:
                bugX = Byte.valueOf(data.consumeByte());
                break;
            case 6:
                bugX = Double.valueOf(Double.NaN);
                break;
            case 7:
                bugX = Double.valueOf(Double.POSITIVE_INFINITY);
                break;
            case 8:
                bugX = Double.valueOf(Double.NEGATIVE_INFINITY);
                break;
            default:
                bugX = Integer.valueOf(0);
                break;
        }

        Number bugY1;
        switch (data.consumeInt(0, 10)) {
            case 0:
                bugY1 = null;
                break;
            case 1:
                bugY1 = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                bugY1 = Long.valueOf((long) data.consumeInt());
                break;
            case 3:
                bugY1 = Double.valueOf((double) data.consumeInt());
                break;
            case 4:
                bugY1 = Float.valueOf((float) data.consumeInt());
                break;
            case 5:
                bugY1 = Short.valueOf((short) data.consumeInt());
                break;
            case 6:
                bugY1 = Byte.valueOf(data.consumeByte());
                break;
            case 7:
                bugY1 = Double.valueOf(Double.NaN);
                break;
            case 8:
                bugY1 = Double.valueOf(Double.POSITIVE_INFINITY);
                break;
            case 9:
                bugY1 = Double.valueOf(Double.NEGATIVE_INFINITY);
                break;
            default:
                bugY1 = Integer.valueOf(0);
                break;
        }

        Number bugY2;
        switch (data.consumeInt(0, 10)) {
            case 0:
                bugY2 = null;
                break;
            case 1:
                bugY2 = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                bugY2 = Long.valueOf((long) data.consumeInt());
                break;
            case 3:
                bugY2 = Double.valueOf((double) data.consumeInt());
                break;
            case 4:
                bugY2 = Float.valueOf((float) data.consumeInt());
                break;
            case 5:
                bugY2 = Short.valueOf((short) data.consumeInt());
                break;
            case 6:
                bugY2 = Byte.valueOf(data.consumeByte());
                break;
            case 7:
                bugY2 = Double.valueOf(Double.NaN);
                break;
            case 8:
                bugY2 = Double.valueOf(Double.POSITIVE_INFINITY);
                break;
            case 9:
                bugY2 = Double.valueOf(Double.NEGATIVE_INFINITY);
                break;
            default:
                bugY2 = Integer.valueOf(0);
                break;
        }

        bugSeries.addOrUpdate(bugX, bugY1);

        if (data.consumeBoolean()) {
            bugSeries.addOrUpdate(bugX, bugY2);
        } else {
            Number otherX;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    otherX = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    otherX = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    otherX = Double.valueOf((double) data.consumeInt());
                    break;
                case 3:
                    otherX = Float.valueOf((float) data.consumeInt());
                    break;
                case 4:
                    otherX = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    otherX = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    otherX = Double.valueOf(Double.NaN);
                    break;
                case 7:
                    otherX = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 8:
                    otherX = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    otherX = Integer.valueOf(1);
                    break;
                }
            bugSeries.addOrUpdate(otherX, bugY2);
        }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,545 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Comparable key;
        if (data.consumeBoolean()) {
            key = data.consumeAsciiString(32);
        } else {
            key = Integer.valueOf(data.consumeInt());
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        series.setMaximumItemCount(data.consumeInt(0, 32));

        int initialAdds = data.consumeInt(0, 24);
        for (int i = 0; i < initialAdds; i++) {
            Number x = consumeNumber(data, false);
            Number y = consumeNumber(data, true);
            if (data.consumeBoolean()) {
                series.add(x, y, data.consumeBoolean());
            } else {
                series.addOrUpdate(x, y);
            }
        }

        int ops = data.consumeInt(1, 32);
        for (int i = 0; i < ops; i++) {
            int action = data.consumeInt(0, 5);
            switch (action) {
                case 0: {
                    Number x = consumeNumber(data, false);
                    Number y = consumeNumber(data, true);
                    series.addOrUpdate(x, y);
                    break;
                }
                case 1: {
                    Number x;
                    if (series.getItemCount() > 0 && data.consumeBoolean()) {
                        int idx = data.consumeInt(0, series.getItemCount() - 1);
                        x = series.getX(idx);
                    } else {
                        x = consumeNumber(data, false);
                    }
                    Number y = consumeNumber(data, true);
                    series.addOrUpdate(x, y);
                    break;
                }
                case 2: {
                    Number x = consumeNumber(data, false);
                    Number y = consumeNumber(data, true);
                    series.add(x, y, data.consumeBoolean());
                    break;
                }
                case 3: {
                    series.setMaximumItemCount(data.consumeInt(0, 32));
                    break;
                }
                case 4: {
                    if (series.getItemCount() > 0) {
                        int idx = data.consumeInt(0, series.getItemCount() - 1);
                        Number x = series.getX(idx);
                        Number y = consumeNumber(data, true);
                        series.addOrUpdate(x, y);
                    }
                    break;
                }
                case 5: {
                    if (series.getItemCount() > 0 && data.consumeBoolean()) {
                        series.remove(data.consumeInt(0, series.getItemCount() - 1));
                    } else {
                        series.clear();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static Number consumeNumber(FuzzedDataProvider data, boolean allowNull) {
        if (allowNull && data.consumeInt(0, 9) == 0) {
            return null;
        }

        switch (data.consumeInt(0, 11)) {
            case 0:
                return Integer.valueOf(data.consumeInt());
            case 1:
                return Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            case 2:
                return Double.valueOf(Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
            case 3:
                return Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
            case 4:
                return Short.valueOf((short) data.consumeInt());
            case 5:
                return Byte.valueOf(data.consumeByte());
            case 6:
                return Integer.valueOf(0);
            case 7:
                return Integer.valueOf(1);
            case 8:
                return Integer.valueOf(-1);
            case 9:
                return Double.valueOf(Double.NaN);
            case 10:
                return Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
            case 11:
            default:
                return Long.valueOf(data.consumeBoolean() ? Long.MIN_VALUE : Long.MAX_VALUE);
        }
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,202 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeAsciiString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 32));

            int operations = data.consumeInt(1, 24);
            for (int i = 0; i < operations; i++) {
                Number x;
                switch (data.consumeInt(0, 13)) {
                    case 0:
                        x = Integer.valueOf(0);
                        break;
                    case 1:
                        x = Integer.valueOf(1);
                        break;
                    case 2:
                        x = Integer.valueOf(-1);
                        break;
                    case 3:
                        x = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 4:
                        x = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 5:
                        x = Long.valueOf(data.consumeInt());
                        break;
                    case 6:
                        x = Double.valueOf(0.0d);
                        break;
                    case 7:
                        x = Double.valueOf(-0.0d);
                        break;
                    case 8:
                        x = Double.valueOf(Double.NaN);
                        break;
                    case 9:
                        x = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    case 10:
                        x = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                    case 11:
                        x = Double.valueOf((double) data.consumeInt());
                        break;
                    case 12:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    default:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                }

                Number y;
                switch (data.consumeInt(0, 14)) {
                    case 0:
                        y = null;
                        break;
                    case 1:
                        y = Integer.valueOf(0);
                        break;
                    case 2:
                        y = Integer.valueOf(1);
                        break;
                    case 3:
                        y = Integer.valueOf(-1);
                        break;
                    case 4:
                        y = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 5:
                        y = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 6:
                        y = Long.valueOf(data.consumeInt());
                        break;
                    case 7:
                        y = Double.valueOf(0.0d);
                        break;
                    case 8:
                        y = Double.valueOf(-0.0d);
                        break;
                    case 9:
                        y = Double.valueOf(Double.NaN);
                        break;
                    case 10:
                        y = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    case 11:
                        y = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                    case 12:
                        y = Double.valueOf((double) data.consumeInt());
                        break;
                    case 13:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    default:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                }

                if (i > 0 && data.consumeBoolean() && series.getItemCount() > 0) {
                    int existingIndex = data.consumeInt(0, series.getItemCount() - 1);
                    XYDataItem existing = series.getDataItem(existingIndex);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            x = existing.getX();
                            break;
                        case 1:
                            x = Integer.valueOf(existing.getX().intValue());
                            break;
                        case 2:
                            x = Double.valueOf(existing.getX().doubleValue());
                            break;
                        default:
                            x = Long.valueOf(existing.getX().longValue());
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);

                if (overwritten != null && data.consumeBoolean()) {
                    Number ox = overwritten.getX();
                    Number oy = overwritten.getY();
                    if (data.consumeBoolean()) {
                        series.addOrUpdate(ox, oy);
                    }
                }

                if (data.consumeBoolean()) {
                    int count = series.getItemCount();
                    if (count > 0) {
                        XYDataItem item = series.getDataItem(data.consumeInt(0, count - 1));
                        Number rx = item.getX();
                        series.addOrUpdate(rx, y);
                    }
                }

                if (data.consumeBoolean()) {
                    series.setMaximumItemCount(data.consumeInt(0, 32));
                }
            }
        }
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,489 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeAsciiString(32);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 32));

        int poolSize = data.consumeInt(1, 8);
        Number[] xPool = new Number[poolSize];

        for (int i = 0; i < poolSize; i++) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
                case 0:
                    xPool[i] = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    xPool[i] = Long.valueOf(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 2:
                    xPool[i] = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    break;
                case 3:
                    xPool[i] = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                    break;
                case 4:
                    xPool[i] = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    xPool[i] = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    xPool[i] = Integer.valueOf(i);
                    break;
                case 7:
                    xPool[i] = Integer.valueOf(-i);
                    break;
                case 8:
                    xPool[i] = Integer.valueOf(0);
                    break;
                default:
                    xPool[i] = Double.valueOf((double) data.consumeInt());
                    break;
            }
        }

        int ops = data.consumeInt(0, 48);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (data.consumeBoolean()) {
                x = xPool[data.consumeInt(0, xPool.length - 1)];
            } else {
                int kind = data.consumeInt(0, 9);
                switch (kind) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Long.valueOf(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 3:
                        x = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        x = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 7:
                        x = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 8:
                        x = Double.valueOf(Double.NaN);
                        break;
                    default:
                        x = Double.valueOf(0.0d);
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 9);
                switch (yKind) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Long.valueOf(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 3:
                        y = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        y = Integer.valueOf(0);
                        break;
                    case 7:
                        y = Integer.valueOf(-1);
                        break;
                    case 8:
                        y = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    default:
                        y = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                }
            }

            series.addOrUpdate(x, y);
        }

        if (xPool.length > 0) {
            Number x0 = xPool[0];
            series.addOrUpdate(x0, Integer.valueOf(1));
            series.addOrUpdate(x0, null);
            series.addOrUpdate(x0, Double.valueOf(Double.NaN));
        }

        if (xPool.length > 1) {
            Number x1 = xPool[1];
            series.addOrUpdate(x1, Integer.valueOf(-1));
            series.addOrUpdate(x1, Double.valueOf(Double.POSITIVE_INFINITY));
        }

        series.addOrUpdate(Integer.valueOf(0), Integer.valueOf(0));
        series.addOrUpdate(Integer.valueOf(-1), Integer.valueOf(1));
        series.addOrUpdate(Integer.valueOf(1), Integer.valueOf(-1));
        series.addOrUpdate(Double.valueOf(-0.0d), Double.valueOf(0.0d));
        series.addOrUpdate(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN));
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,057 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(chooseMaxItemCount(data));

        java.util.ArrayList<Number> seenX = new java.util.ArrayList<Number>();

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(chooseMaxItemCount(data));
            }

            Number x = chooseX(data, seenX);
            Number y = chooseY(data);

            series.addOrUpdate(x, y);

            if (x != null && (allowDuplicateXValues || !containsEquivalent(seenX, x))) {
                seenX.add(x);
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (item != null && item.getX() != null) {
                    seenX.add(item.getX());
                }
            }
        }

        if (data.consumeBoolean()) {
            Number finalX = chooseX(data, seenX);
            Number finalY = chooseY(data);
            series.addOrUpdate(finalX, finalY);
        }
    }

    private static int chooseMaxItemCount(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 7);
        switch (choice) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 10;
            case 5:
                return 100;
            case 6:
                return Integer.MAX_VALUE;
            default:
                return data.consumeInt(0, 32);
        }
    }

    private static Number chooseX(FuzzedDataProvider data, java.util.List<Number> seenX) {
        int choice = data.consumeInt(0, 12);
        if (!seenX.isEmpty() && choice == 0) {
            return seenX.get(data.consumeInt(0, seenX.size() - 1));
        }
        switch (choice) {
            case 1:
                return Integer.valueOf(0);
            case 2:
                return Integer.valueOf(1);
            case 3:
                return Integer.valueOf(-1);
            case 4:
                return Integer.valueOf(Integer.MIN_VALUE);
            case 5:
                return Integer.valueOf(Integer.MAX_VALUE);
            case 6:
                return Long.valueOf(0L);
            case 7:
                return Double.valueOf(-0.0d);
            case 8:
                return Double.valueOf(Double.NaN);
            case 9:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            case 10:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 11:
                return Double.valueOf((double) data.consumeInt());
            case 12:
                return Float.valueOf((float) data.consumeInt());
            default:
                return Byte.valueOf(data.consumeByte());
        }
    }

    private static Number chooseY(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 12);
        switch (choice) {
            case 0:
                return null;
            case 1:
                return Integer.valueOf(0);
            case 2:
                return Integer.valueOf(1);
            case 3:
                return Integer.valueOf(-1);
            case 4:
                return Integer.valueOf(Integer.MIN_VALUE);
            case 5:
                return Integer.valueOf(Integer.MAX_VALUE);
            case 6:
                return Long.valueOf(data.consumeInt());
            case 7:
                return Double.valueOf(-0.0d);
            case 8:
                return Double.valueOf(Double.NaN);
            case 9:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            case 10:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 11:
                return Double.valueOf((double) data.consumeInt());
            case 12:
                return Float.valueOf((float) data.consumeInt());
            default:
                return Byte.valueOf(data.consumeByte());
        }
    }

    private static boolean containsEquivalent(java.util.List<Number> values, Number candidate) {
        for (int i = 0; i < values.size(); i++) {
            Number n = values.get(i);
            if (n == candidate) {
                return true;
            }
            if (n != null && candidate != null && n.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,056 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);
        for (int i = 0; i < seriesCount; i++) {
            String key = data.consumeAsciiString(32);
            XYSeries series = new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());
            series.setMaximumItemCount(data.consumeInt(0, 8));

            int setupOps = data.consumeInt(0, 12);
            for (int j = 0; j < setupOps; j++) {
                Number x;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        x = Double.valueOf((double) data.consumeInt());
                        break;
                    case 3:
                        x = Float.valueOf((float) data.consumeInt());
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    default:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            y = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            y = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            y = Double.valueOf((double) data.consumeInt());
                            break;
                        case 3:
                            y = Float.valueOf((float) data.consumeInt());
                            break;
                        case 4:
                            y = Short.valueOf((short) data.consumeInt());
                            break;
                        default:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                    }
                }

                series.addOrUpdate(x, y);
            }

            int targetCalls = data.consumeInt(1, 6);
            for (int j = 0; j < targetCalls; j++) {
                Number x;
                if (series.getItemCount() > 0 && data.consumeBoolean()) {
                    x = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1)).getX();
                } else if (data.consumeBoolean()) {
                    x = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            x = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            x = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            x = Double.valueOf((double) data.consumeInt());
                            break;
                        case 3:
                            x = Float.valueOf((float) data.consumeInt());
                            break;
                        case 4:
                            x = Short.valueOf((short) data.consumeInt());
                            break;
                        default:
                            x = Byte.valueOf(data.consumeByte());
                            break;
                    }
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            y = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            y = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            y = Double.valueOf((double) data.consumeInt());
                            break;
                        case 3:
                            y = Float.valueOf((float) data.consumeInt());
                            break;
                        case 4:
                            y = Short.valueOf((short) data.consumeInt());
                            break;
                        default:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                    }
                }

                series.addOrUpdate(x, y);
            }
        }
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,896 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(16);
        String key2 = data.consumeAsciiString(16);
        String key3 = data.consumeRemainingAsString();
        if (key1 == null) {
            key1 = "";
        }
        if (key2 == null) {
            key2 = "";
        }
        if (key3 == null) {
            key3 = "";
        }

        XYSeries sortedNoDup = new XYSeries(key1, true, false);
        XYSeries unsortedNoDup = new XYSeries(key2, false, false);
        XYSeries dupSeries = new XYSeries(key3, data.consumeBoolean(), true);

        sortedNoDup.setMaximumItemCount(data.consumeInt(0, 8));
        unsortedNoDup.setMaximumItemCount(data.consumeInt(0, 8));
        dupSeries.setMaximumItemCount(data.consumeInt(0, 8));

        int sortedCount = data.consumeInt(0, 8);
        int[] sortedXs = new int[sortedCount];
        for (int i = 0; i < sortedCount; i++) {
            int base = data.consumeInt();
            int x = base + i;
            sortedXs[i] = x;
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            sortedNoDup.addOrUpdate(Integer.valueOf(x), y);
        }

        int unsortedCount = data.consumeInt(0, 8);
        int[] unsortedXs = new int[unsortedCount];
        for (int i = 0; i < unsortedCount; i++) {
            int base = data.consumeInt();
            int x = base + i;
            unsortedXs[i] = x;
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            unsortedNoDup.addOrUpdate(Integer.valueOf(x), y);
        }

        int dupCount = data.consumeInt(0, 8);
        int repeatedX = data.consumeInt();
        for (int i = 0; i < dupCount; i++) {
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            dupSeries.addOrUpdate(Integer.valueOf((i % 2 == 0) ? repeatedX : data.consumeInt()), y);
        }

        int sortedUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < sortedUpdates; i++) {
            Integer x;
            if (sortedXs.length > 0 && data.consumeBoolean()) {
                x = Integer.valueOf(sortedXs[data.consumeInt(0, sortedXs.length - 1)]);
            } else {
                x = Integer.valueOf(data.consumeInt());
            }

            Number y;
            int yKind = data.consumeInt(0, 4);
            if (yKind == 0) {
                y = null;
            } else if (yKind == 1) {
                y = Integer.valueOf(data.consumeInt());
            } else if (yKind == 2) {
                y = Long.valueOf((long) data.consumeInt());
            } else if (yKind == 3) {
                y = Double.valueOf((double) data.consumeInt());
            } else {
                y = Byte.valueOf(data.consumeByte());
            }
            sortedNoDup.addOrUpdate(x, y);
        }

        int unsortedUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < unsortedUpdates; i++) {
            Integer x;
            if (unsortedXs.length > 0 && data.consumeBoolean()) {
                x = Integer.valueOf(unsortedXs[data.consumeInt(0, unsortedXs.length - 1)]);
            } else {
                x = Integer.valueOf(data.consumeInt());
            }

            Number y;
            int yKind = data.consumeInt(0, 4);
            if (yKind == 0) {
                y = null;
            } else if (yKind == 1) {
                y = Integer.valueOf(data.consumeInt());
            } else if (yKind == 2) {
                y = Long.valueOf((long) data.consumeInt());
            } else if (yKind == 3) {
                y = Double.valueOf((double) data.consumeInt());
            } else {
                y = Byte.valueOf(data.consumeByte());
            }
            unsortedNoDup.addOrUpdate(x, y);
        }

        int dupUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < dupUpdates; i++) {
            Integer x = Integer.valueOf(data.consumeBoolean() ? repeatedX : data.consumeInt());
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            dupSeries.addOrUpdate(x, y);
        }

        if (data.remainingBytes() > 0) {
            sortedNoDup.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
        if (data.remainingBytes() > 0) {
            unsortedNoDup.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
        if (data.remainingBytes() > 0) {
            dupSeries.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: This harness compiled, but its execution produced no finding. Try different inputs using the supplied function source and fuzzer API. Let unexpected throwables escape. Return the full FuzzHarness.java

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,293 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(16);
        String key2 = data.consumeAsciiString(16);
        String key3 = data.consumeRemainingAsString();
        if (key1 == null) {
            key1 = "";
        }
        if (key2 == null) {
            key2 = "";
        }
        if (key3 == null) {
            key3 = "";
        }

        XYSeries sortedNoDup = new XYSeries(key1, true, false);
        XYSeries unsortedNoDup = new XYSeries(key2, false, false);
        XYSeries dupSeries = new XYSeries(key3, data.consumeBoolean(), true);

        sortedNoDup.setMaximumItemCount(data.consumeInt(0, 8));
        unsortedNoDup.setMaximumItemCount(data.consumeInt(0, 8));
        dupSeries.setMaximumItemCount(data.consumeInt(0, 8));

        int sortedCount = data.consumeInt(0, 8);
        int[] sortedXs = new int[sortedCount];
        for (int i = 0; i < sortedCount; i++) {
            int base = data.consumeInt();
            int x = base + i;
            sortedXs[i] = x;
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            sortedNoDup.addOrUpdate(Integer.valueOf(x), y);
        }

        int unsortedCount = data.consumeInt(0, 8);
        int[] unsortedXs = new int[unsortedCount];
        for (int i = 0; i < unsortedCount; i++) {
            int base = data.consumeInt();
            int x = base + i;
            unsortedXs[i] = x;
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            unsortedNoDup.addOrUpdate(Integer.valueOf(x), y);
        }

        int dupCount = data.consumeInt(0, 8);
        int repeatedX = data.consumeInt();
        for (int i = 0; i < dupCount; i++) {
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            dupSeries.addOrUpdate(Integer.valueOf((i % 2 == 0) ? repeatedX : data.consumeInt()), y);
        }

        int sortedUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < sortedUpdates; i++) {
            Integer x;
            if (sortedXs.length > 0 && data.consumeBoolean()) {
                x = Integer.valueOf(sortedXs[data.consumeInt(0, sortedXs.length - 1)]);
            } else {
                x = Integer.valueOf(data.consumeInt());
            }

            Number y;
            int yKind = data.consumeInt(0, 4);
            if (yKind == 0) {
                y = null;
            } else if (yKind == 1) {
                y = Integer.valueOf(data.consumeInt());
            } else if (yKind == 2) {
                y = Long.valueOf((long) data.consumeInt());
            } else if (yKind == 3) {
                y = Double.valueOf((double) data.consumeInt());
            } else {
                y = Byte.valueOf(data.consumeByte());
            }
            sortedNoDup.addOrUpdate(x, y);
        }

        int unsortedUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < unsortedUpdates; i++) {
            Integer x;
            if (unsortedXs.length > 0 && data.consumeBoolean()) {
                x = Integer.valueOf(unsortedXs[data.consumeInt(0, unsortedXs.length - 1)]);
            } else {
                x = Integer.valueOf(data.consumeInt());
            }

            Number y;
            int yKind = data.consumeInt(0, 4);
            if (yKind == 0) {
                y = null;
            } else if (yKind == 1) {
                y = Integer.valueOf(data.consumeInt());
            } else if (yKind == 2) {
                y = Long.valueOf((long) data.consumeInt());
            } else if (yKind == 3) {
                y = Double.valueOf((double) data.consumeInt());
            } else {
                y = Byte.valueOf(data.consumeByte());
            }
            unsortedNoDup.addOrUpdate(x, y);
        }

        int dupUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < dupUpdates; i++) {
            Integer x = Integer.valueOf(data.consumeBoolean() ? repeatedX : data.consumeInt());
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            dupSeries.addOrUpdate(x, y);
        }

        if (data.remainingBytes() > 0) {
            sortedNoDup.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
        if (data.remainingBytes() > 0) {
            unsortedNoDup.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
        if (data.remainingBytes() > 0) {
            dupSeries.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
    }
}
```
**[user]**
```
This harness compiled, but its execution produced no finding. Try different inputs using the supplied function source and fuzzer API. Let unexpected throwables escape. Return the full FuzzHarness.java as raw Java source, without markdown.
```

</details>
<details open><summary>▸ Output (~4,282 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static Number pickNumber(FuzzedDataProvider data, boolean allowNull) {
        int choice = data.consumeInt(0, allowNull ? 15 : 14);
        switch (choice) {
            case 0:
                return allowNull ? null : Integer.valueOf(0);
            case 1:
                return Integer.valueOf(data.consumeInt());
            case 2:
                return Long.valueOf((long) data.consumeInt());
            case 3:
                return Byte.valueOf(data.consumeByte());
            case 4:
                return Short.valueOf((short) data.consumeInt());
            case 5:
                return Float.valueOf((float) data.consumeInt());
            case 6:
                return Double.valueOf((double) data.consumeInt());
            case 7:
                return Double.valueOf(Double.NaN);
            case 8:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 9:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            case 10:
                return Double.valueOf(-0.0d);
            case 11:
                return Double.valueOf(0.0d);
            case 12:
                return Integer.valueOf(Integer.MIN_VALUE);
            case 13:
                return Integer.valueOf(Integer.MAX_VALUE);
            case 14:
                return Long.valueOf(Long.MIN_VALUE);
            case 15:
                return null;
            default:
                return Integer.valueOf(0);
        }
    }

    private static XYSeries makeSeries(FuzzedDataProvider data) {
        String key = data.consumeString(32);
        if (key == null) {
            key = "";
        }
        return new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());
    }

    private static void exerciseSeries(FuzzedDataProvider data, XYSeries series) {
        if (data.consumeBoolean()) {
            series.setMaximumItemCount(data.consumeInt());
        }

        int prepopulate = data.consumeInt(0, 12);
        for (int i = 0; i < prepopulate; i++) {
            Number x = pickNumber(data, false);
            Number y = pickNumber(data, true);
            try {
                series.add(x, y);
            } catch (RuntimeException e) {
            }
        }

        int ops = data.consumeInt(1, 16);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (i == 0 || data.consumeBoolean()) {
                x = pickNumber(data, true);
            } else if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                x = pickNumber(data, false);
            }

            Number y = pickNumber(data, true);
            series.addOrUpdate(x, y);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        XYSeries s1 = makeSeries(data);
        XYSeries s2 = makeSeries(data);
        XYSeries s3 = new XYSeries("", true, false);
        XYSeries s4 = new XYSeries("", false, false);
        XYSeries s5 = new XYSeries("", true, true);
        XYSeries s6 = new XYSeries("", false, true);

        exerciseSeries(data, s1);
        exerciseSeries(data, s2);
        exerciseSeries(data, s3);
        exerciseSeries(data, s4);
        exerciseSeries(data, s5);
        exerciseSeries(data, s6);

        XYSeries boundary = new XYSeries("boundary", true, false);
        if (data.consumeBoolean()) {
            boundary.setMaximumItemCount(data.consumeInt());
        }
        boundary.addOrUpdate(Integer.valueOf(0), null);
        boundary.addOrUpdate(Double.valueOf(-0.0d), Integer.valueOf(1));
        boundary.addOrUpdate(Double.valueOf(0.0d), Integer.valueOf(2));
        boundary.addOrUpdate(Double.valueOf(Double.NaN), Integer.valueOf(3));
        boundary.addOrUpdate(Double.valueOf(Double.POSITIVE_INFINITY), Integer.valueOf(4));
        boundary.addOrUpdate(Double.valueOf(Double.NEGATIVE_INFINITY), Integer.valueOf(5));

        if (data.remainingBytes() > 0) {
            boundary.addOrUpdate(pickNumber(data, true), pickNumber(data, true));
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IllegalArgumentException: Null 'x' argument.

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,511 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 3);
        XYSeries[] seriesArray = new XYSeries[seriesCount];

        for (int s = 0; s < seriesCount; s++) {
            String key;
            if (data.consumeBoolean()) {
                key = data.consumeString(32);
            } else {
                key = data.consumeAsciiString(32);
            }

            boolean autoSort;
            boolean allowDuplicateXValues;

            if (s == 0) {
                autoSort = true;
                allowDuplicateXValues = false;
            } else if (s == 1) {
                autoSort = false;
                allowDuplicateXValues = false;
            } else {
                autoSort = data.consumeBoolean();
                allowDuplicateXValues = data.consumeBoolean();
            }

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 12));
            seriesArray[s] = series;
        }

        int poolSize = data.consumeInt(1, 16);
        Number[] xPool = new Number[poolSize];
        Number[] yPool = new Number[poolSize];

        for (int i = 0; i < poolSize; i++) {
            int a = data.consumeInt();
            int b = data.consumeInt();
            long bits = (((long) a) << 32) ^ (b & 0xffffffffL);

            switch (data.consumeInt(0, 5)) {
                case 0:
                    xPool[i] = Integer.valueOf(a);
                    break;
                case 1:
                    xPool[i] = Long.valueOf(bits);
                    break;
                case 2:
                    xPool[i] = Double.valueOf((double) a);
                    break;
                case 3:
                    xPool[i] = Double.valueOf(Double.longBitsToDouble(bits));
                    break;
                case 4:
                    xPool[i] = Short.valueOf((short) a);
                    break;
                default:
                    xPool[i] = Byte.valueOf((byte) a);
                    break;
            }

            if (data.consumeBoolean()) {
                yPool[i] = null;
            } else {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        yPool[i] = Integer.valueOf(b);
                        break;
                    case 1:
                        yPool[i] = Long.valueOf((((long) b) << 32) ^ (a & 0xffffffffL));
                        break;
                    case 2:
                        yPool[i] = Double.valueOf((double) b);
                        break;
                    case 3:
                        yPool[i] = Double.valueOf(Double.longBitsToDouble(((((long) b) << 32) ^ (a & 0xffffffffL))));
                        break;
                    case 4:
                        yPool[i] = Float.valueOf(Float.intBitsToFloat(b));
                        break;
                    default:
                        yPool[i] = Short.valueOf((short) b);
                        break;
                }
            }
        }

        int operations = data.consumeInt(0, 64);
        for (int op = 0; op < operations; op++) {
            XYSeries series = seriesArray[data.consumeInt(0, seriesArray.length - 1)];

            if (data.consumeInt(0, 9) == 0) {
                series.setMaximumItemCount(data.consumeInt(0, 12));
            }

            Number x;
            if (data.consumeBoolean()) {
                x = xPool[data.consumeInt(0, xPool.length - 1)];
            } else {
                int a = data.consumeInt();
                int b = data.consumeInt();
                long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        x = Integer.valueOf(a);
                        break;
                    case 1:
                        x = Long.valueOf(bits);
                        break;
                    case 2:
                        x = Double.valueOf((double) a);
                        break;
                    case 3:
                        x = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    case 4:
                        x = Short.valueOf((short) a);
                        break;
                    default:
                        x = Byte.valueOf((byte) a);
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = yPool[data.consumeInt(0, yPool.length - 1)];
            } else if (data.consumeBoolean()) {
                y = null;
            } else {
                int a = data.consumeInt();
                int b = data.consumeInt();
                long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        y = Integer.valueOf(a);
                        break;
                    case 1:
                        y = Long.valueOf(bits);
                        break;
                    case 2:
                        y = Double.valueOf((double) a);
                        break;
                    case 3:
                        y = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(a));
                        break;
                    default:
                        y = Short.valueOf((short) a);
                        break;
                }
            }

            XYDataItem result = series.addOrUpdate(x, y);

            if (result != null && data.consumeBoolean()) {
                Number rx = result.getX();
                Number ry = result.getY();
                if (rx != null && data.consumeBoolean()) {
                    series.addOrUpdate(rx, ry);
                }
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (item != null) {
                    series.addOrUpdate(item.getX(), item.getY());
                }
            }
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,145 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 8));

        int operations = data.consumeInt(0, 20);
        for (int i = 0; i < operations; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 8));
            }

            Number x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = Integer.valueOf(data.consumeInt(-3, 3));
                    break;
                case 1:
                    x = Long.valueOf(data.consumeInt(-3, 3));
                    break;
                case 2:
                    x = Double.valueOf((double) data.consumeInt(-3, 3));
                    break;
                case 3:
                    x = Float.valueOf((float) data.consumeInt(-3, 3));
                    break;
                case 4:
                    x = Double.valueOf(Double.NaN);
                    break;
                case 5:
                    x = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                case 6:
                    x = Short.valueOf((short) data.consumeInt(-3, 3));
                    break;
                default:
                    x = new java.math.BigDecimal(Integer.toString(data.consumeInt(-3, 3)));
                    break;
            }

            Number y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    y = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    y = Double.valueOf((double) data.consumeInt());
                    break;
                case 4:
                    y = Float.valueOf((float) data.consumeInt());
                    break;
                case 5:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 6:
                    y = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                default:
                    y = new java.math.BigDecimal(Integer.toString(data.consumeInt()));
                    break;
            }

            series.addOrUpdate(x, y);

            if (data.consumeBoolean()) {
                Number y2;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        y2 = null;
                        break;
                    case 1:
                        y2 = Integer.valueOf(data.consumeInt(-10, 10));
                        break;
                    case 2:
                        y2 = Long.valueOf(data.consumeInt(-10, 10));
                        break;
                    case 3:
                        y2 = Double.valueOf((double) data.consumeInt(-10, 10));
                        break;
                    case 4:
                        y2 = Double.valueOf(Double.NaN);
                        break;
                    default:
                        y2 = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                        break;
                }
                series.addOrUpdate(x, y2);
            }

            int count = series.getItemCount();
            if (count > 0) {
                int idx = data.consumeInt(0, count - 1);
                series.getDataItem(idx);
                series.getX(idx);
                series.getY(idx);
            }

            series.indexOf(x);
        }

        if (data.remainingBytes() > 0) {
            Number finalX;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    finalX = Integer.valueOf(data.consumeInt(-1, 1));
                    break;
                case 1:
                    finalX = Double.valueOf((double) data.consumeInt(-1, 1));
                    break;
                case 2:
                    finalX = Double.valueOf(Double.NaN);
                    break;
                case 3:
                    finalX = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                default:
                    finalX = new java.math.BigDecimal(Integer.toString(data.consumeInt(-1, 1)));
                    break;
            }

            Number finalY = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            series.addOrUpdate(finalX, finalY);
        }
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,850 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 8));

            int operations = data.consumeInt(0, 24);
            for (int i = 0; i < operations; i++) {
                Number x;
                int xKind = data.consumeInt(0, 11);
                switch (xKind) {
                    case 0:
                        x = Integer.valueOf(0);
                        break;
                    case 1:
                        x = Integer.valueOf(-1);
                        break;
                    case 2:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        x = Long.valueOf(data.consumeInt());
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 7: {
                        long bits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                        x = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    }
                    case 8:
                        x = Integer.valueOf(i - (operations / 2));
                        break;
                    case 9:
                        x = Integer.valueOf(series.getItemCount());
                        break;
                    case 10:
                        if (series.getItemCount() > 0 && data.consumeBoolean()) {
                            x = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1)).getX();
                        } else {
                            x = Integer.valueOf(data.consumeInt(-3, 3));
                        }
                        break;
                    default:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                }

                Number y;
                int yKind = data.consumeInt(0, 9);
                switch (yKind) {
                    case 0:
                        y = null;
                        break;
                    case 1:
                        y = Integer.valueOf(0);
                        break;
                    case 2:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        y = Long.valueOf(data.consumeInt());
                        break;
                    case 4:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 7: {
                        long bits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                        y = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    }
                    case 8:
                        y = Integer.valueOf(i);
                        break;
                    default:
                        y = Integer.valueOf(data.consumeInt(-16, 16));
                        break;
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                    overwritten.compareTo(new XYDataItem(x, y));
                }

                if (series.getItemCount() > 0 && data.consumeBoolean()) {
                    XYDataItem item = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1));
                    item.getX();
                    item.getY();
                    series.indexOf(item.getX());
                }
            }

            if (series.getItemCount() > 0) {
                int extraUpdates = data.consumeInt(0, 8);
                for (int i = 0; i < extraUpdates; i++) {
                    Number existingX = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1)).getX();
                    Number newY;
                    int mode = data.consumeInt(0, 4);
                    if (mode == 0) {
                        newY = null;
                    } else if (mode == 1) {
                        newY = Integer.valueOf(data.consumeInt());
                    } else if (mode == 2) {
                        newY = Double.valueOf((double) data.consumeInt());
                    } else if (mode == 3) {
                        newY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    } else {
                        newY = Long.valueOf(data.consumeInt());
                    }
                    series.addOrUpdate(existingX, newY);
                }
            }
        }
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,360 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);

        Number[] pool = new Number[16];
        pool[0] = Integer.valueOf(0);
        pool[1] = Integer.valueOf(1);
        pool[2] = Integer.valueOf(-1);
        pool[3] = Integer.valueOf(Integer.MIN_VALUE);
        pool[4] = Integer.valueOf(Integer.MAX_VALUE);
        pool[5] = Byte.valueOf((byte) 0);
        pool[6] = Byte.valueOf(data.consumeByte());
        pool[7] = Short.valueOf((short) data.consumeInt());
        pool[8] = Integer.valueOf(data.consumeInt());
        pool[9] = Long.valueOf((long) data.consumeInt());
        pool[10] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        pool[11] = Float.valueOf((float) data.consumeInt());
        pool[12] = Double.valueOf((double) data.consumeInt());
        pool[13] = Double.valueOf(-0.0d);
        pool[14] = Double.valueOf(0.0d);
        pool[15] = Float.valueOf(-0.0f);

        XYSeries[] serieses = new XYSeries[] {
            new XYSeries(key, false, false),
            new XYSeries(key + "A", true, false),
            new XYSeries(key + "B", false, true),
            new XYSeries(key + "C", true, true),
            new XYSeries(key + "D", data.consumeBoolean(), data.consumeBoolean())
        };

        for (int s = 0; s < serieses.length; s++) {
            serieses[s].setMaximumItemCount(data.consumeInt(0, 16));
        }

        Number[] seen = new Number[32];
        int seenCount = 0;
        int ops = data.consumeInt(1, 24);

        for (int i = 0; i < ops; i++) {
            Number x;
            int xMode = data.consumeInt(0, 5);
            if (xMode == 0 && seenCount > 0) {
                x = seen[data.consumeInt(0, seenCount - 1)];
            } else if (xMode == 1 && seenCount > 0) {
                Number prior = seen[data.consumeInt(0, seenCount - 1)];
                if (prior == null) {
                    x = pool[data.consumeInt(0, pool.length - 1)];
                } else if (prior instanceof Integer) {
                    x = Double.valueOf(((Integer) prior).doubleValue());
                } else if (prior instanceof Long) {
                    x = Integer.valueOf((int) ((Long) prior).longValue());
                } else if (prior instanceof Byte) {
                    x = Long.valueOf(((Byte) prior).longValue());
                } else if (prior instanceof Short) {
                    x = Float.valueOf(((Short) prior).floatValue());
                } else if (prior instanceof Float) {
                    x = Double.valueOf(((Float) prior).doubleValue());
                } else if (prior instanceof Double) {
                    x = Integer.valueOf((int) ((Double) prior).doubleValue());
                } else {
                    x = pool[data.consumeInt(0, pool.length - 1)];
                }
            } else {
                x = pool[data.consumeInt(0, pool.length - 1)];
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                y = pool[data.consumeInt(0, pool.length - 1)];
            }

            XYSeries target = serieses[data.consumeInt(0, serieses.length - 1)];
            XYDataItem overwritten = target.addOrUpdate(x, y);

            if (seenCount < seen.length) {
                seen[seenCount++] = x;
            } else {
                seen[data.consumeInt(0, seen.length - 1)] = x;
            }

            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
            }

            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                target.setMaximumItemCount(data.consumeInt(0, 16));
            }
        }

        for (int s = 0; s < serieses.length; s++) {
            Number x = pool[data.consumeInt(0, pool.length - 1)];
            Number y = data.consumeBoolean() ? null : pool[data.consumeInt(0, pool.length - 1)];
            XYDataItem overwritten = serieses[s].addOrUpdate(x, y);
            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
            }
        }
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,256 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 32));

        Number[] seenX = new Number[32];
        int seenCount = 0;

        int operations = data.consumeInt(1, 40);
        for (int i = 0; i < operations; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 32));
            }

            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    case 2:
                        x = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 3:
                        x = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 7:
                        x = Double.valueOf((double) data.consumeInt(-3, 3));
                        break;
                    default:
                        x = Long.valueOf(data.consumeInt(-3, 3));
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    case 2:
                        y = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 3:
                        y = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-3, 3));
                        break;
                }
            }

            series.addOrUpdate(x, y);

            if (!allowDuplicateXValues && data.consumeBoolean()) {
                Number y2;
                if (data.consumeBoolean()) {
                    y2 = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            y2 = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            y2 = Double.valueOf(Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                            break;
                        case 2:
                            y2 = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 3:
                            y2 = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                            break;
                        case 4:
                            y2 = Byte.valueOf(data.consumeByte());
                            break;
                        default:
                            y2 = Integer.valueOf(data.consumeInt(-3, 3));
                            break;
                    }
                }
                series.addOrUpdate(x, y2);
            }
        }
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,445 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));

        Number[] seenX = new Number[32];
        int seenCount = 0;

        int ops = data.consumeInt(0, 32);
        for (int i = 0; i < ops; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }

            Number x;
            if (data.consumeInt(0, 9) == 0) {
                x = null;
            } else if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Double.valueOf(data.consumeBoolean() ? Double.NaN : data.consumeInt());
                        break;
                    case 2:
                        x = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : -0.0d);
                        break;
                    case 3:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        x = Float.valueOf(data.consumeBoolean() ? Float.NEGATIVE_INFINITY : (float) data.consumeInt());
                        break;
                    case 5:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        x = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, 16)));
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Double.valueOf(data.consumeBoolean() ? Double.NaN : data.consumeInt());
                        break;
                    case 2:
                        y = Double.valueOf(data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                        break;
                    case 3:
                        y = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        y = Float.valueOf(data.consumeBoolean() ? Float.NaN : (float) data.consumeInt());
                        break;
                    case 5:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, 16)));
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (x != null && seenCount < seenX.length && data.consumeBoolean()) {
                seenX[seenCount++] = x;
            }

            if (overwritten != null && data.consumeBoolean()) {
                series.addOrUpdate(overwritten.getX(), overwritten.getY());
            }

            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (data.consumeBoolean()) {
                    series.addOrUpdate(item.getX(), y);
                } else {
                    series.addOrUpdate(item.getX(), item.getY());
                }
            }
        }
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IllegalArgumentException: Null 'x' argument.

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,792 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 16));

            int ops = data.consumeInt(0, 24);
            Number[] seenX = new Number[ops > 0 ? ops : 1];
            int seenCount = 0;

            for (int i = 0; i < ops; i++) {
                Number x;
                if (seenCount > 0 && data.consumeBoolean()) {
                    x = seenX[data.consumeInt(0, seenCount - 1)];
                } else {
                    int xKind = data.consumeInt(0, 9);
                    int raw = data.consumeInt();
                    switch (xKind) {
                        case 0:
                            x = Integer.valueOf(raw);
                            break;
                        case 1:
                            x = Long.valueOf((long) raw);
                            break;
                        case 2: {
                            int divisor = data.consumeInt(1, 1024);
                            x = Double.valueOf(((double) raw) / divisor);
                            break;
                        }
                        case 3: {
                            int divisor = data.consumeInt(1, 1024);
                            x = Float.valueOf(((float) raw) / divisor);
                            break;
                        }
                        case 4:
                            x = Short.valueOf((short) raw);
                            break;
                        case 5:
                            x = Byte.valueOf(data.consumeByte());
                            break;
                        case 6:
                            x = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            x = Double.valueOf(raw < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            x = Double.valueOf(-0.0d);
                            break;
                        default:
                            x = Double.valueOf(0.0d);
                            break;
                    }
                    if (seenCount < seenX.length) {
                        seenX[seenCount++] = x;
                    }
                }

                Number y;
                if (data.consumeInt(0, 5) == 0) {
                    y = null;
                } else {
                    int yKind = data.consumeInt(0, 9);
                    int rawY = data.consumeInt();
                    switch (yKind) {
                        case 0:
                            y = Integer.valueOf(rawY);
                            break;
                        case 1:
                            y = Long.valueOf((long) rawY);
                            break;
                        case 2: {
                            int divisor = data.consumeInt(1, 1024);
                            y = Double.valueOf(((double) rawY) / divisor);
                            break;
                        }
                        case 3: {
                            int divisor = data.consumeInt(1, 1024);
                            y = Float.valueOf(((float) rawY) / divisor);
                            break;
                        }
                        case 4:
                            y = Short.valueOf((short) rawY);
                            break;
                        case 5:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                        case 6:
                            y = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            y = Double.valueOf(rawY < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            y = Double.valueOf(-0.0d);
                            break;
                        default:
                            y = Double.valueOf(0.0d);
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }

                if (data.consumeInt(0, 7) == 0) {
                    series.setMaximumItemCount(data.consumeInt(0, 16));
                }
            }

            if (data.consumeBoolean() && seenCount > 0) {
                Number x = seenX[data.consumeInt(0, seenCount - 1)];

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int rawY = data.consumeInt();
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            y = Integer.valueOf(rawY);
                            break;
                        case 1:
                            y = Long.valueOf((long) rawY);
                            break;
                        case 2:
                            y = Double.valueOf((double) rawY);
                            break;
                        case 3:
                            y = Float.valueOf((float) rawY);
                            break;
                        case 4:
                            y = Double.valueOf(Double.NaN);
                            break;
                        default:
                            y = Double.valueOf(rawY < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }
            }

            if (data.consumeBoolean()) {
                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    y = Integer.valueOf(data.consumeInt());
                }
                series.addOrUpdate(null, y);
            }
        }
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,146 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeAsciiString(32);
        if (key.length() == 0) {
            key = "k";
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));

        int initialAdds = data.consumeInt(0, 24);
        for (int i = 0; i < initialAdds; i++) {
            Number x;
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                int xKind = data.consumeInt(0, 7);
                switch (xKind) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt(-8, 8));
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt(-8, 8));
                        break;
                    case 2:
                        x = Short.valueOf((short) data.consumeInt(-8, 8));
                        break;
                    case 3:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        x = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    default:
                        x = Double.valueOf((double) data.consumeInt(-8, 8));
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 7);
                switch (yKind) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 3:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        y = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        y = Integer.valueOf(data.consumeInt(-16, 16));
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-16, 16));
                        break;
                }
            }

            series.addOrUpdate(x, y);
        }

        int calls = data.consumeInt(1, 8);
        for (int i = 0; i < calls; i++) {
            Number x;
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                int xKind = data.consumeInt(0, 9);
                switch (xKind) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt(-1, 1));
                        break;
                    case 2:
                        x = Short.valueOf((short) data.consumeInt(-1, 1));
                        break;
                    case 3:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        x = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 7:
                        x = Double.valueOf((double) data.consumeInt(-4, 4));
                        break;
                    case 8:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    default:
                        x = Integer.valueOf(series.getItemCount());
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 7);
                switch (yKind) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    case 1:
                        y = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 3:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        y = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-4, 4));
                        break;
                }
            }

            series.addOrUpdate(x, y);
        }
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -2, Size: 2

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,092 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 32));

        int initialOps = data.consumeInt(0, 24);
        Number[] seenX = new Number[initialOps + 8];
        int seenCount = 0;

        for (int i = 0; i < initialOps; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int selector = data.consumeInt(0, 7);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        x = Integer.valueOf(raw);
                        break;
                    case 1:
                        x = Long.valueOf((long) raw);
                        break;
                    case 2:
                        x = Short.valueOf((short) raw);
                        break;
                    case 3:
                        x = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        x = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    default:
                        x = Double.valueOf((double) data.consumeInt(-3, 3));
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int selector = data.consumeInt(0, 6);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        y = Integer.valueOf(raw);
                        break;
                    case 1:
                        y = Long.valueOf((long) raw);
                        break;
                    case 2:
                        y = Short.valueOf((short) raw);
                        break;
                    case 3:
                        y = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        y = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-10, 10));
                        break;
                }
            }

            if (data.consumeBoolean()) {
                series.addOrUpdate(x, y);
            } else {
                series.add(x, y);
            }
        }

        int ops = data.consumeInt(1, 32);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int selector = data.consumeInt(0, 8);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        x = Integer.valueOf(raw);
                        break;
                    case 1:
                        x = Long.valueOf((long) raw);
                        break;
                    case 2:
                        x = Short.valueOf((short) raw);
                        break;
                    case 3:
                        x = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        x = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    case 7:
                        x = Double.valueOf((double) data.consumeInt(-1, 1));
                        break;
                    default:
                        x = Double.valueOf(0.0d);
                        break;
                }
                if (seenCount < seenX.length && data.consumeBoolean()) {
                    seenX[seenCount++] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int selector = data.consumeInt(0, 7);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        y = Integer.valueOf(raw);
                        break;
                    case 1:
                        y = Long.valueOf((long) raw);
                        break;
                    case 2:
                        y = Short.valueOf((short) raw);
                        break;
                    case 3:
                        y = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        y = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    case 6:
                        y = Double.valueOf((double) data.consumeInt(-100, 100));
                        break;
                    default:
                        y = Integer.valueOf(data.consumeInt(-100, 100));
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (overwritten != null && data.consumeBoolean()) {
                series.addOrUpdate(overwritten.getX(), overwritten.getY());
            }

            if (data.consumeBoolean()) {
                series.indexOf(x);
            }
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                series.getDataItem(data.consumeInt(0, series.getItemCount() - 1));
            }
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 32));
            }
        }
    }
}
```

</details>

---
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.jfree.data.general.SeriesException: X-value already exists.

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,204 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 3);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(16);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 8));

            int ops = data.consumeInt(1, 16);
            Number[] seenX = new Number[ops + 4];
            int seenCount = 0;

            for (int i = 0; i < ops; i++) {
                Number x;
                if (seenCount > 0 && data.consumeBoolean()) {
                    x = seenX[data.consumeInt(0, seenCount - 1)];
                } else {
                    int xKind = data.consumeInt(0, 6);
                    int xv = data.consumeInt();
                    switch (xKind) {
                        case 0:
                            x = Integer.valueOf(xv);
                            break;
                        case 1:
                            x = Long.valueOf(xv);
                            break;
                        case 2:
                            x = Short.valueOf((short) xv);
                            break;
                        case 3:
                            x = Byte.valueOf((byte) xv);
                            break;
                        case 4:
                            x = Float.valueOf((float) xv);
                            break;
                        case 5:
                            x = Double.valueOf((double) xv);
                            break;
                        default:
                            long bits = (((long) data.consumeInt()) << 32)
                                    | (((long) data.consumeInt()) & 0xffffffffL);
                            x = Double.valueOf(Double.longBitsToDouble(bits));
                            break;
                    }
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int yKind = data.consumeInt(0, 6);
                    int yv = data.consumeInt();
                    switch (yKind) {
                        case 0:
                            y = Integer.valueOf(yv);
                            break;
                        case 1:
                            y = Long.valueOf(yv);
                            break;
                        case 2:
                            y = Short.valueOf((short) yv);
                            break;
                        case 3:
                            y = Byte.valueOf((byte) yv);
                            break;
                        case 4:
                            y = Float.valueOf((float) yv);
                            break;
                        case 5:
                            y = Double.valueOf((double) yv);
                            break;
                        default:
                            long bits = (((long) data.consumeInt()) << 32)
                                    | (((long) data.consumeInt()) & 0xffffffffL);
                            y = Double.valueOf(Double.longBitsToDouble(bits));
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }

                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }

                int itemCount = series.getItemCount();
                if (itemCount > 0) {
                    XYDataItem item = series.getDataItem(data.consumeInt(0, itemCount - 1));
                    item.getX();
                    item.getY();
                }

                series.indexOf(x);
            }

            if (!allowDuplicateXValues && seenCount > 0) {
                Number x = seenX[data.consumeInt(0, seenCount - 1)];
                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int v = data.consumeInt();
                    y = data.consumeBoolean() ? Integer.valueOf(v) : Double.valueOf((double) v);
                }
                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }
            }

            int finalCount = series.getItemCount();
            if (finalCount > 0) {
                XYDataItem first = series.getDataItem(0);
                first.getX();
                first.getY();
                XYDataItem last = series.getDataItem(finalCount - 1);
                last.getX();
                last.getY();
            }
        }
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 2

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,527 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(16);
        if (key1.length() == 0) {
            key1 = "S1";
        }

        XYSeries series = new XYSeries(
                key1,
                data.consumeBoolean(),
                data.consumeBoolean());

        series.setMaximumItemCount(data.consumeInt(0, 32));

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            Number x;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    x = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    x = Long.valueOf(data.consumeInt());
                    break;
                case 2:
                    x = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                    break;
                case 3:
                    x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    break;
                case 4:
                    x = Short.valueOf((short) data.consumeInt());
                    break;
                default:
                    x = Byte.valueOf(data.consumeByte());
                    break;
            }

            Number y;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    y = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    y = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                    break;
                case 4:
                    y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    break;
                case 5:
                    y = Short.valueOf((short) data.consumeInt());
                    break;
                default:
                    y = Byte.valueOf(data.consumeByte());
                    break;
            }

            series.addOrUpdate(x, y);
        }

        String key2 = data.consumeAsciiString(16);
        if (key2.length() == 0) {
            key2 = "BUG";
        }

        XYSeries buggySeries = new XYSeries(key2, true, true);
        buggySeries.setMaximumItemCount(data.consumeInt(1, 16));

        Integer duplicateX = Integer.valueOf(data.consumeInt());

        Number firstY;
        switch (data.consumeInt(0, 5)) {
            case 0:
                firstY = null;
                break;
            case 1:
                firstY = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                firstY = Long.valueOf(data.consumeInt());
                break;
            case 3:
                firstY = Double.valueOf(Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                break;
            case 4:
                firstY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                break;
            default:
                firstY = Byte.valueOf(data.consumeByte());
                break;
        }

        Number secondY;
        switch (data.consumeInt(0, 5)) {
            case 0:
                secondY = null;
                break;
            case 1:
                secondY = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                secondY = Long.valueOf(data.consumeInt());
                break;
            case 3:
                secondY = Double.valueOf(Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                break;
            case 4:
                secondY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                break;
            default:
                secondY = Byte.valueOf(data.consumeByte());
                break;
        }

        buggySeries.addOrUpdate(duplicateX, firstY);
        buggySeries.addOrUpdate(duplicateX, secondY);
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,243 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));
        series.setNotify(data.consumeBoolean());

        int operations = data.consumeInt(1, 24);

        Number[] xs = new Number[operations + 8];
        Number[] ys = new Number[operations + 8];

        xs[0] = Integer.valueOf(0);
        xs[1] = Integer.valueOf(1);
        xs[2] = Integer.valueOf(-1);
        xs[3] = Integer.valueOf(Integer.MIN_VALUE);
        xs[4] = Integer.valueOf(Integer.MAX_VALUE);
        xs[5] = Double.valueOf(0.0d);
        xs[6] = Double.valueOf(-0.0d);
        xs[7] = Long.valueOf(1L);

        ys[0] = null;
        ys[1] = Integer.valueOf(0);
        ys[2] = Integer.valueOf(1);
        ys[3] = Integer.valueOf(-1);
        ys[4] = Integer.valueOf(Integer.MIN_VALUE);
        ys[5] = Integer.valueOf(Integer.MAX_VALUE);
        ys[6] = Double.valueOf(0.0d);
        ys[7] = Long.valueOf(-1L);

        for (int i = 8; i < xs.length; i++) {
            int selector = data.consumeInt(0, 7);
            int v = data.consumeInt();
            switch (selector) {
                case 0:
                    xs[i] = Integer.valueOf(v);
                    break;
                case 1:
                    xs[i] = Long.valueOf((long) v);
                    break;
                case 2:
                    xs[i] = Double.valueOf((double) v);
                    break;
                case 3:
                    xs[i] = Float.valueOf((float) v);
                    break;
                case 4:
                    xs[i] = Short.valueOf((short) v);
                    break;
                case 5:
                    xs[i] = Byte.valueOf((byte) v);
                    break;
                case 6:
                    xs[i] = Double.valueOf(v / 3.0d);
                    break;
                default:
                    xs[i] = Integer.valueOf(v % 5);
                    break;
            }

            if (data.consumeBoolean()) {
                ys[i] = null;
            } else {
                int ySelector = data.consumeInt(0, 7);
                int yv = data.consumeInt();
                switch (ySelector) {
                    case 0:
                        ys[i] = Integer.valueOf(yv);
                        break;
                    case 1:
                        ys[i] = Long.valueOf((long) yv);
                        break;
                    case 2:
                        ys[i] = Double.valueOf((double) yv);
                        break;
                    case 3:
                        ys[i] = Float.valueOf((float) yv);
                        break;
                    case 4:
                        ys[i] = Short.valueOf((short) yv);
                        break;
                    case 5:
                        ys[i] = Byte.valueOf((byte) yv);
                        break;
                    case 6:
                        ys[i] = Double.valueOf(yv / 7.0d);
                        break;
                    default:
                        ys[i] = Integer.valueOf(yv % 5);
                        break;
                }
            }
        }

        for (int i = 0; i < operations; i++) {
            int mode = data.consumeInt(0, 5);
            Number x;
            Number y;

            switch (mode) {
                case 0:
                    x = xs[data.consumeInt(0, xs.length - 1)];
                    y = ys[data.consumeInt(0, ys.length - 1)];
                    break;
                case 1:
                    x = xs[i % xs.length];
                    y = ys[(i + 1) % ys.length];
                    break;
                case 2:
                    x = xs[Math.max(0, i - 1)];
                    y = ys[i % ys.length];
                    break;
                case 3:
                    x = xs[0];
                    y = ys[i % ys.length];
                    break;
                case 4:
                    x = xs[1];
                    y = ys[(i * 3) % ys.length];
                    break;
                default:
                    x = xs[(i * 5) % xs.length];
                    y = ys[(i * 7) % ys.length];
                    break;
            }

            series.addOrUpdate(x, y);

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }
            if (data.consumeBoolean()) {
                series.setNotify(data.consumeBoolean());
            }
        }

        if (operations > 0) {
            Number duplicateX = xs[data.consumeInt(0, xs.length - 1)];
            Number y1 = ys[data.consumeInt(0, ys.length - 1)];
            Number y2 = ys[data.consumeInt(0, ys.length - 1)];
            series.addOrUpdate(duplicateX, y1);
            series.addOrUpdate(duplicateX, y2);
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,601 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean[][] configs = new boolean[][] {
            {false, false},
            {false, true},
            {true, false},
            {true, true}
        };

        for (int c = 0; c < configs.length; c++) {
            String key = data.consumeString(32);
            XYSeries series = new XYSeries(key, configs[c][0], configs[c][1]);
            series.setMaximumItemCount(data.consumeInt(0, 8));

            java.util.ArrayList<Number> xPool = new java.util.ArrayList<Number>();
            int operations = data.consumeInt(0, 16);

            for (int i = 0; i < operations; i++) {
                Number x;
                if (!xPool.isEmpty() && data.consumeBoolean()) {
                    x = xPool.get(data.consumeInt(0, xPool.size() - 1));
                } else {
                    int kind = data.consumeInt(0, 11);
                    int v = data.consumeInt();
                    switch (kind) {
                        case 0:
                            x = Byte.valueOf((byte) v);
                            break;
                        case 1:
                            x = Short.valueOf((short) v);
                            break;
                        case 2:
                            x = Integer.valueOf(v);
                            break;
                        case 3:
                            x = Long.valueOf((long) v);
                            break;
                        case 4:
                            x = Float.valueOf((float) v);
                            break;
                        case 5:
                            x = Double.valueOf((double) v);
                            break;
                        case 6:
                            x = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            x = Double.valueOf(Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            x = Double.valueOf(Double.NEGATIVE_INFINITY);
                            break;
                        case 9:
                            x = Integer.valueOf(0);
                            break;
                        case 10:
                            x = Integer.valueOf(-1);
                            break;
                        default:
                            x = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                    }
                    if (xPool.size() < 32) {
                        xPool.add(x);
                    }
                }

                Number y;
                if (data.consumeInt(0, 4) == 0) {
                    y = null;
                } else {
                    int kind = data.consumeInt(0, 12);
                    int v = data.consumeInt();
                    switch (kind) {
                        case 0:
                            y = Byte.valueOf((byte) v);
                            break;
                        case 1:
                            y = Short.valueOf((short) v);
                            break;
                        case 2:
                            y = Integer.valueOf(v);
                            break;
                        case 3:
                            y = Long.valueOf((long) v);
                            break;
                        case 4:
                            y = Float.valueOf((float) v);
                            break;
                        case 5:
                            y = Double.valueOf((double) v);
                            break;
                        case 6:
                            y = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            y = Double.valueOf(Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            y = Double.valueOf(Double.NEGATIVE_INFINITY);
                            break;
                        case 9:
                            y = Integer.valueOf(0);
                            break;
                        case 10:
                            y = Integer.valueOf(-1);
                            break;
                        case 11:
                            y = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                        default:
                            y = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                    }
                }

                series.addOrUpdate(x, y);
            }

            if (!xPool.isEmpty()) {
                Number existingX = xPool.get(data.consumeInt(0, xPool.size() - 1));
                Number overwriteY;
                if (data.consumeBoolean()) {
                    overwriteY = null;
                } else {
                    int kind = data.consumeInt(0, 8);
                    int v = data.consumeInt();
                    switch (kind) {
                        case 0:
                            overwriteY = Integer.valueOf(v);
                            break;
                        case 1:
                            overwriteY = Long.valueOf((long) v);
                            break;
                        case 2:
                            overwriteY = Double.valueOf((double) v);
                            break;
                        case 3:
                            overwriteY = Float.valueOf((float) v);
                            break;
                        case 4:
                            overwriteY = Double.valueOf(Double.NaN);
                            break;
                        case 5:
                            overwriteY = Double.valueOf(Double.POSITIVE_INFINITY);
                            break;
                        case 6:
                            overwriteY = Double.valueOf(Double.NEGATIVE_INFINITY);
                            break;
                        case 7:
                            overwriteY = Integer.valueOf(0);
                            break;
                        default:
                            overwriteY = Integer.valueOf(-1);
                            break;
                    }
                }
                series.addOrUpdate(existingX, overwriteY);
            }
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 2

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,623 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String baseKey = data.consumeString(32);
        int sharedMax = data.consumeInt(0, 32);

        boolean[] autoSortOptions = new boolean[] { false, true, data.consumeBoolean() };
        boolean[] allowDupOptions = new boolean[] { false, true, data.consumeBoolean() };

        for (int a = 0; a < autoSortOptions.length; a++) {
            for (int d = 0; d < allowDupOptions.length; d++) {
                XYSeries series = new XYSeries(
                    baseKey + ":" + a + ":" + d + ":" + data.consumeAsciiString(8),
                    autoSortOptions[a],
                    allowDupOptions[d]
                );

                series.setMaximumItemCount(data.consumeInt(0, Math.max(0, sharedMax)));

                int operations = data.consumeInt(0, 24);
                Number[] priorX = new Number[Math.max(1, operations)];
                int priorCount = 0;

                for (int i = 0; i < operations; i++) {
                    boolean reuseExistingX = priorCount > 0 && data.consumeBoolean();
                    Number x;

                    if (reuseExistingX) {
                        x = priorX[data.consumeInt(0, priorCount - 1)];
                    } else {
                        int xKind = data.consumeInt(0, 8);
                        switch (xKind) {
                            case 0:
                                x = Integer.valueOf(data.consumeInt());
                                break;
                            case 1:
                                x = Long.valueOf((long) data.consumeInt());
                                break;
                            case 2:
                                x = Double.valueOf((double) data.consumeInt());
                                break;
                            case 3:
                                x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                                break;
                            case 4:
                                x = Short.valueOf((short) data.consumeInt());
                                break;
                            case 5:
                                x = Byte.valueOf(data.consumeByte());
                                break;
                            case 6:
                                x = Integer.valueOf(data.consumeInt(-1, 1));
                                break;
                            case 7:
                                x = Double.valueOf((double) Float.intBitsToFloat(data.consumeInt()));
                                break;
                            default:
                                x = Long.valueOf((long) data.consumeInt(-16, 16));
                                break;
                        }
                        priorX[priorCount++] = x;
                    }

                    Number y;
                    int yKind = data.consumeInt(0, 9);
                    switch (yKind) {
                        case 0:
                            y = null;
                            break;
                        case 1:
                            y = Integer.valueOf(data.consumeInt());
                            break;
                        case 2:
                            y = Long.valueOf((long) data.consumeInt());
                            break;
                        case 3:
                            y = Double.valueOf((double) data.consumeInt());
                            break;
                        case 4:
                            y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 5:
                            y = Short.valueOf((short) data.consumeInt());
                            break;
                        case 6:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                        case 7:
                            y = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 8:
                            y = Double.valueOf((double) Float.intBitsToFloat(data.consumeInt()));
                            break;
                        default:
                            y = Long.valueOf((long) data.consumeInt(-16, 16));
                            break;
                    }

                    series.addOrUpdate(x, y);

                    if (priorCount > 0 && data.consumeBoolean()) {
                        Number dupX = priorX[data.consumeInt(0, priorCount - 1)];
                        Number dupY;
                        int dupYKind = data.consumeInt(0, 5);
                        switch (dupYKind) {
                            case 0:
                                dupY = null;
                                break;
                            case 1:
                                dupY = Integer.valueOf(data.consumeInt());
                                break;
                            case 2:
                                dupY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                                break;
                            case 3:
                                dupY = Double.valueOf((double) data.consumeInt());
                                break;
                            case 4:
                                dupY = Long.valueOf((long) data.consumeInt());
                                break;
                            default:
                                dupY = Byte.valueOf(data.consumeByte());
                                break;
                        }
                        series.addOrUpdate(dupX, dupY);
                    }

                    if (data.consumeBoolean()) {
                        series.setMaximumItemCount(data.consumeInt(0, 32));
                    }
                }

                if (priorCount > 0) {
                    Number x = priorX[data.consumeInt(0, priorCount - 1)];
                    Number y = data.consumeBoolean() ? null : Double.valueOf((double) data.consumeInt());
                    series.addOrUpdate(x, y);
                }

                if (data.consumeBoolean()) {
                    series.addOrUpdate(Integer.valueOf(0), null);
                    series.addOrUpdate(Integer.valueOf(0), Integer.valueOf(data.consumeInt()));
                }
            }
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,988 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 3);
        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            if (key == null) {
                key = "";
            }

            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();
            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

            series.setMaximumItemCount(data.consumeInt(0, 16));

            Number[] seenX = new Number[64];
            int seenCount = 0;

            int operations = data.consumeInt(0, 24);
            for (int i = 0; i < operations; i++) {
                if (data.consumeBoolean()) {
                    series.setMaximumItemCount(data.consumeInt(0, 16));
                }

                Number x;
                boolean reuseX = seenCount > 0 && data.consumeBoolean();
                if (reuseX) {
                    x = seenX[data.consumeInt(0, seenCount - 1)];
                } else {
                    int xKind = data.consumeInt(0, 9);
                    switch (xKind) {
                        case 0:
                            x = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            x = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            x = Short.valueOf((short) data.consumeInt());
                            break;
                        case 3:
                            x = Byte.valueOf(data.consumeByte());
                            break;
                        case 4:
                            x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 5:
                            x = Double.valueOf(Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32)
                                            | (((long) data.consumeInt()) & 0xffffffffL)));
                            break;
                        case 6:
                            x = java.math.BigInteger.valueOf((long) data.consumeInt());
                            break;
                        case 7:
                            x = java.math.BigDecimal.valueOf((long) data.consumeInt(), data.consumeInt(0, 6));
                            break;
                        case 8:
                            x = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            x = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    if (seenCount < seenX.length) {
                        seenX[seenCount++] = x;
                    } else {
                        seenX[data.consumeInt(0, seenX.length - 1)] = x;
                    }
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int yKind = data.consumeInt(0, 9);
                    switch (yKind) {
                        case 0:
                            y = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            y = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            y = Short.valueOf((short) data.consumeInt());
                            break;
                        case 3:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                        case 4:
                            y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 5:
                            y = Double.valueOf(Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32)
                                            | (((long) data.consumeInt()) & 0xffffffffL)));
                            break;
                        case 6:
                            y = java.math.BigInteger.valueOf((long) data.consumeInt());
                            break;
                        case 7:
                            y = java.math.BigDecimal.valueOf((long) data.consumeInt(), data.consumeInt(0, 6));
                            break;
                        case 8:
                            y = Integer.valueOf(0);
                            break;
                        default:
                            y = Integer.valueOf(-1);
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);

                if (overwritten != null && data.consumeBoolean()) {
                    Number ox = overwritten.getX();
                    Number oy = overwritten.getY();
                    if (ox != null) {
                        series.indexOf(ox);
                    }
                    if (oy != null && data.consumeBoolean()) {
                        series.addOrUpdate(ox, oy);
                    }
                }

                if (series.getItemCount() > 0 && data.consumeBoolean()) {
                    int idx = data.consumeInt(0, series.getItemCount() - 1);
                    XYDataItem item = series.getDataItem(idx);
                    if (item != null && item.getX() != null && data.consumeBoolean()) {
                        series.addOrUpdate(item.getX(), item.getY());
                    }
                }
            }
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,840 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.function.Supplier<Number> nextNumber = () -> {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    return Integer.valueOf(data.consumeInt());
                case 1:
                    return Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                case 2:
                    return Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                case 3:
                    return Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                case 4:
                    return Short.valueOf((short) data.consumeInt());
                case 5:
                    return Byte.valueOf(data.consumeByte());
                case 6:
                    return new java.math.BigDecimal(data.consumeAsciiString(12).isEmpty() ? "0" : data.consumeAsciiString(12));
                default:
                    return new java.math.BigInteger(data.consumeAsciiString(12).isEmpty() ? "0" : data.consumeAsciiString(12), 36);
            }
        };

        java.util.function.Supplier<Number> nextNullableNumber = () -> {
            if (data.consumeBoolean()) {
                return null;
            }
            return nextNumber.get();
        };

        String key = data.consumeString(32);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));

        Number[] seenX = new Number[32];
        int seenCount = 0;

        int initialOps = data.consumeInt(0, 12);
        for (int i = 0; i < initialOps; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                x = nextNumber.get();
            }

            Number y = nextNullableNumber.get();
            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (seenCount < seenX.length) {
                seenX[seenCount++] = x;
            }

            if (overwritten != null && data.consumeBoolean()) {
                overwritten.getX();
                overwritten.getY();
            }
        }

        int targetOps = data.consumeInt(1, 10);
        for (int i = 0; i < targetOps; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                x = nextNumber.get();
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }
            }

            Number y = nextNullableNumber.get();
            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
            }

            if (seenCount > 0 && data.consumeBoolean()) {
                series.indexOf(seenX[data.consumeInt(0, seenCount - 1)]);
            } else {
                series.indexOf(nextNumber.get());
            }
        }

        if (series.getItemCount() > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, series.getItemCount() - 1);
            series.getDataItem(idx);
            series.getX(idx);
            series.getY(idx);
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NumberFormatException: For input string: "     "

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,137 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String baseKey = data.consumeString(32);

        XYSeries[] serieses = new XYSeries[] {
            new XYSeries(baseKey + "_ff", false, false),
            new XYSeries(baseKey + "_ft", false, true),
            new XYSeries(baseKey + "_tf", true, false),
            new XYSeries(baseKey + "_tt", true, true)
        };

        for (int i = 0; i < serieses.length; i++) {
            serieses[i].setMaximumItemCount(data.consumeInt(0, 16));
        }

        Number[] seenX = new Number[16];
        int seenCount = 0;

        int operations = data.consumeInt(0, 40);
        for (int op = 0; op < operations; op++) {
            XYSeries series = serieses[data.consumeInt(0, serieses.length - 1)];

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }

            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int xv = data.consumeInt();
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        x = Integer.valueOf(xv);
                        break;
                    case 1:
                        x = Long.valueOf((long) xv);
                        break;
                    case 2:
                        x = Double.valueOf((double) xv);
                        break;
                    case 3:
                        x = Float.valueOf((float) xv);
                        break;
                    case 4:
                        x = Short.valueOf((short) xv);
                        break;
                    case 5:
                        x = Byte.valueOf((byte) xv);
                        break;
                    case 6:
                        x = Double.valueOf(Double.longBitsToDouble((((long) xv) << 32) ^ (xv & 0xffffffffL)));
                        break;
                    default:
                        switch (xv & 3) {
                            case 0:
                                x = Double.valueOf(Double.NaN);
                                break;
                            case 1:
                                x = Double.valueOf(Double.POSITIVE_INFINITY);
                                break;
                            case 2:
                                x = Double.valueOf(Double.NEGATIVE_INFINITY);
                                break;
                            default:
                                x = Double.valueOf(-0.0d);
                                break;
                        }
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                } else if (data.consumeBoolean()) {
                    seenX[data.consumeInt(0, seenX.length - 1)] = x;
                }
            }

            Number y;
            if (data.consumeInt(0, 7) == 0) {
                y = null;
            } else {
                int yv = data.consumeInt();
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = Integer.valueOf(yv);
                        break;
                    case 1:
                        y = Long.valueOf((long) yv);
                        break;
                    case 2:
                        y = Double.valueOf((double) yv);
                        break;
                    case 3:
                        y = Float.valueOf(Float.intBitsToFloat(yv));
                        break;
                    case 4:
                        y = Short.valueOf((short) yv);
                        break;
                    case 5:
                        y = Byte.valueOf((byte) yv);
                        break;
                    case 6:
                        y = Double.valueOf(Double.longBitsToDouble((((long) yv) << 32) ^ (yv & 0xffffffffL)));
                        break;
                    default:
                        switch (yv & 3) {
                            case 0:
                                y = Double.valueOf(Double.NaN);
                                break;
                            case 1:
                                y = Double.valueOf(Double.POSITIVE_INFINITY);
                                break;
                            case 2:
                                y = Double.valueOf(Double.NEGATIVE_INFINITY);
                                break;
                            default:
                                y = Double.valueOf(-0.0d);
                                break;
                        }
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);
            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
                overwritten.compareTo(new XYDataItem(x, y));
            }

            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (item != null && data.consumeBoolean()) {
                    series.addOrUpdate(item.getX(), y);
                }
            }

            if (data.consumeBoolean()) {
                series.indexOf(x);
            }
        }

        for (int i = 0; i < serieses.length; i++) {
            XYSeries series = serieses[i];
            if (series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                series.addOrUpdate(item.getX(), item.getY());
            } else if (data.remainingBytes() > 0) {
                series.addOrUpdate(Integer.valueOf(data.consumeInt()), null);
            }
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,449 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = 1 + Math.abs(data.consumeInt() % 3);
        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 16));

            Number pivotX = consumeNumber(data, false);
            Number pivotY1 = consumeNumber(data, true);
            Number pivotY2 = consumeNumber(data, true);

            series.addOrUpdate(pivotX, pivotY1);
            series.addOrUpdate(pivotX, pivotY2);

            Number smallerX = relatedNumber(pivotX, -1, data);
            Number largerX = relatedNumber(pivotX, 1, data);
            series.addOrUpdate(smallerX, consumeNumber(data, true));
            series.addOrUpdate(largerX, consumeNumber(data, true));

            int ops = data.consumeInt(0, 32);
            for (int i = 0; i < ops; i++) {
                Number x;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        x = pivotX;
                        break;
                    case 1:
                        x = smallerX;
                        break;
                    case 2:
                        x = largerX;
                        break;
                    case 3:
                        x = relatedNumber(pivotX, data.consumeBoolean() ? -1 : 1, data);
                        break;
                    default:
                        x = consumeNumber(data, false);
                        break;
                }
                Number y = consumeNumber(data, true);
                XYDataItem overwritten = series.addOrUpdate(x, y);

                if (overwritten != null && data.consumeBoolean()) {
                    Number ox = overwritten.getX();
                    Number oy = overwritten.getY();
                    series.addOrUpdate(ox, oy);
                }

                if (data.consumeBoolean()) {
                    series.setMaximumItemCount(data.consumeInt(0, 16));
                }
            }

            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                series.addOrUpdate(item.getX(), consumeNumber(data, true));
            }
        }
    }

    private static Number consumeNumber(FuzzedDataProvider data, boolean allowNull) {
        if (allowNull && data.consumeBoolean()) {
            return null;
        }
        switch (data.consumeInt(0, 9)) {
            case 0:
                return Integer.valueOf(data.consumeInt());
            case 1:
                return Long.valueOf(data.consumeInt());
            case 2:
                return Double.valueOf((double) data.consumeInt());
            case 3:
                return Float.valueOf((float) data.consumeInt());
            case 4:
                return Short.valueOf((short) data.consumeInt());
            case 5:
                return Byte.valueOf(data.consumeByte());
            case 6:
                return Double.valueOf(Double.NaN);
            case 7:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 8:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            default:
                return Double.valueOf(data.consumeBoolean() ? 0.0d : -0.0d);
        }
    }

    private static Number relatedNumber(Number base, int delta, FuzzedDataProvider data) {
        double b = base.doubleValue();
        switch (data.consumeInt(0, 5)) {
            case 0:
                return Integer.valueOf((int) b + delta);
            case 1:
                return Long.valueOf((long) b + delta);
            case 2:
                return Double.valueOf(b + delta);
            case 3:
                return Float.valueOf((float) b + delta);
            case 4:
                return Short.valueOf((short) (((int) b) + delta));
            default:
                return Byte.valueOf((byte) (((int) b) + delta));
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,172 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Comparable key;
        if (data.consumeBoolean()) {
            key = data.consumeAsciiString(32);
        } else {
            key = Integer.valueOf(data.consumeInt());
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        int maxCountChoice = data.consumeInt(0, 6);
        if (maxCountChoice == 0) {
            series.setMaximumItemCount(0);
        } else if (maxCountChoice == 1) {
            series.setMaximumItemCount(1);
        } else if (maxCountChoice == 2) {
            series.setMaximumItemCount(2);
        } else if (maxCountChoice == 3) {
            series.setMaximumItemCount(3);
        } else if (maxCountChoice == 4) {
            series.setMaximumItemCount(10);
        } else if (maxCountChoice == 5) {
            series.setMaximumItemCount(Math.abs(data.consumeInt()));
        } else {
            series.setMaximumItemCount(Integer.MAX_VALUE);
        }

        int preload = data.consumeInt(0, 16);
        for (int i = 0; i < preload && data.remainingBytes() > 0; i++) {
            int xKind = data.consumeInt(0, 9);
            Number x;
            switch (xKind) {
                case 0:
                    x = Integer.valueOf(0);
                    break;
                case 1:
                    x = Integer.valueOf(1);
                    break;
                case 2:
                    x = Integer.valueOf(-1);
                    break;
                case 3:
                    x = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    x = Double.valueOf(0.0d);
                    break;
                case 5:
                    x = Double.valueOf(-0.0d);
                    break;
                case 6:
                    x = Double.valueOf(Double.NaN);
                    break;
                case 7:
                    x = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 8:
                    x = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    x = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
            }

            int yKind = data.consumeInt(0, 9);
            Number y;
            switch (yKind) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(0);
                    break;
                case 2:
                    y = Integer.valueOf(1);
                    break;
                case 3:
                    y = Integer.valueOf(-1);
                    break;
                case 4:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 5:
                    y = Double.valueOf(0.0d);
                    break;
                case 6:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 7:
                    y = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 8:
                    y = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                default:
                    y = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
            }

            if (data.consumeBoolean()) {
                series.addOrUpdate(x, y);
            } else {
                series.add(x, y);
            }
        }

        int ops = data.consumeInt(1, 32);
        for (int i = 0; i < ops && data.remainingBytes() > 0; i++) {
            int xKind = data.consumeInt(0, 11);
            Number x;
            switch (xKind) {
                case 0:
                    x = Integer.valueOf(0);
                    break;
                case 1:
                    x = Integer.valueOf(1);
                    break;
                case 2:
                    x = Integer.valueOf(-1);
                    break;
                case 3:
                    x = Integer.valueOf(Integer.MIN_VALUE);
                    break;
                case 4:
                    x = Integer.valueOf(Integer.MAX_VALUE);
                    break;
                case 5:
                    x = Double.valueOf(0.0d);
                    break;
                case 6:
                    x = Double.valueOf(-0.0d);
                    break;
                case 7:
                    x = Double.valueOf(Double.NaN);
                    break;
                case 8:
                    x = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 9:
                    x = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                case 10:
                    x = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
                default:
                    if (series.getItemCount() > 0 && data.consumeBoolean()) {
                        x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
                    } else {
                        x = Integer.valueOf(data.consumeInt());
                    }
                    break;
            }

            int yKind = data.consumeInt(0, 11);
            Number y;
            switch (yKind) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(0);
                    break;
                case 2:
                    y = Integer.valueOf(1);
                    break;
                case 3:
                    y = Integer.valueOf(-1);
                    break;
                case 4:
                    y = Integer.valueOf(Integer.MIN_VALUE);
                    break;
                case 5:
                    y = Integer.valueOf(Integer.MAX_VALUE);
                    break;
                case 6:
                    y = Double.valueOf(0.0d);
                    break;
                case 7:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 8:
                    y = Double.valueOf(Double.POSITIVE_INFINITY);
                    break;
                case 9:
                    y = Double.valueOf(Double.NEGATIVE_INFINITY);
                    break;
                case 10:
                    y = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE)));
                    break;
                default:
                    y = Double.valueOf(data.consumeInt());
                    break;
            }

            XYDataItem old = series.addOrUpdate(x, y);

            if (old != null && data.consumeBoolean()) {
                series.addOrUpdate(old.getX(), old.getY());
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                series.indexOf(series.getX(idx));
            }

            if (data.consumeBoolean()) {
                int newMaxMode = data.consumeInt(0, 4);
                if (newMaxMode == 0) {
                    series.setMaximumItemCount(0);
                } else if (newMaxMode == 1) {
                    series.setMaximumItemCount(1);
                } else if (newMaxMode == 2) {
                    series.setMaximumItemCount(2);
                } else if (newMaxMode == 3) {
                    series.setMaximumItemCount(3);
                } else {
                    series.setMaximumItemCount(Math.abs(data.consumeInt()));
                }
            }
        }
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.jfree.data.general.SeriesException: X-value already exists.

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,088 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        XYSeries series = new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());
        series.setMaximumItemCount(data.consumeInt(0, 32));

        int plannedOps = data.consumeInt(0, 64);
        Number[] seenX = new Number[32];
        int seenCount = 0;

        for (int i = 0; i < plannedOps && data.remainingBytes() >= 0; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int kind = data.consumeInt(0, 11);
                int v = data.consumeInt();
                switch (kind) {
                    case 0:
                        x = Integer.valueOf(v);
                        break;
                    case 1:
                        x = Long.valueOf((long) v);
                        break;
                    case 2:
                        x = Short.valueOf((short) v);
                        break;
                    case 3:
                        x = Byte.valueOf((byte) v);
                        break;
                    case 4:
                        x = Float.valueOf((float) v);
                        break;
                    case 5:
                        x = Double.valueOf((double) v);
                        break;
                    case 6:
                        x = Double.valueOf(v / 3.0d);
                        break;
                    case 7:
                        x = Double.valueOf(Double.NaN);
                        break;
                    case 8:
                        x = Double.valueOf(v < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                        break;
                    case 9:
                        x = new java.math.BigDecimal(data.consumeAsciiString(16).isEmpty() ? "0" : data.consumeAsciiString(16));
                        break;
                    case 10:
                        x = new java.math.BigInteger(data.consumeAsciiString(16).replaceAll("[^0-9-]", "").isEmpty() ? "0" : data.consumeAsciiString(16).replaceAll("[^0-9-]", ""));
                        break;
                    default:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                } else {
                    seenX[data.consumeInt(0, seenX.length - 1)] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int kind = data.consumeInt(0, 10);
                int v = data.consumeInt();
                switch (kind) {
                    case 0:
                        y = Integer.valueOf(v);
                        break;
                    case 1:
                        y = Long.valueOf((long) v);
                        break;
                    case 2:
                        y = Short.valueOf((short) v);
                        break;
                    case 3:
                        y = Byte.valueOf((byte) v);
                        break;
                    case 4:
                        y = Float.valueOf((float) v);
                        break;
                    case 5:
                        y = Double.valueOf((double) v);
                        break;
                    case 6:
                        y = Double.valueOf(v / 7.0d);
                        break;
                    case 7:
                        y = Double.valueOf(Double.NaN);
                        break;
                    case 8:
                        y = Double.valueOf(v < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                        break;
                    case 9:
                        y = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    default:
                        y = new java.math.BigDecimal(Integer.toString(v));
                        break;
                }
            }

            series.addOrUpdate(x, y);

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 32));
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                Number existingX = series.getX(idx);
                Number newY;
                if (data.consumeBoolean()) {
                    newY = null;
                } else {
                    int v = data.consumeInt();
                    newY = data.consumeBoolean() ? Double.valueOf(v / 5.0d) : Integer.valueOf(v);
                }
                series.addOrUpdate(existingX, newY);
            }
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NumberFormatException

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,933 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        XYSeries series = new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());

        series.setMaximumItemCount(data.consumeInt(0, 16));

        int ops = data.consumeInt(1, 24);
        Number[] seenX = new Number[ops + 4];
        int seenCount = 0;

        for (int i = 0; i < ops; i++) {
            if (data.remainingBytes() <= 0) {
                break;
            }

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }

            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int xKind = data.consumeInt(0, 7);
                switch (xKind) {
                    case 0:
                        x = new Integer(data.consumeInt());
                        break;
                    case 1:
                        x = new Long((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        x = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 3:
                        x = new Float(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 4:
                        x = new Short((short) data.consumeInt());
                        break;
                    case 5:
                        x = new Byte(data.consumeByte());
                        break;
                    case 6:
                        x = new Double((double) data.consumeInt());
                        break;
                    default:
                        x = new Long((long) data.consumeInt());
                        break;
                }
                seenX[seenCount++] = x;
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 7);
                switch (yKind) {
                    case 0:
                        y = new Integer(data.consumeInt());
                        break;
                    case 1:
                        y = new Long((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        y = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 3:
                        y = new Float(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 4:
                        y = new Short((short) data.consumeInt());
                        break;
                    case 5:
                        y = new Byte(data.consumeByte());
                        break;
                    case 6:
                        y = new Double((double) data.consumeInt());
                        break;
                    default:
                        y = new Long((long) data.consumeInt());
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (overwritten != null && data.consumeBoolean()) {
                overwritten.getX();
                overwritten.getY();
            }

            if (series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                series.getDataItem(idx);
            }

            if (data.consumeBoolean()) {
                series.indexOf(x);
            }
        }

        if (data.consumeBoolean()) {
            Number finalX;
            if (data.consumeBoolean()) {
                finalX = null;
            } else if (seenCount > 0 && data.consumeBoolean()) {
                finalX = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                finalX = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                        ^ (data.consumeInt() & 0xffffffffL)));
            }

            Number finalY;
            if (data.consumeBoolean()) {
                finalY = null;
            } else {
                finalY = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                        ^ (data.consumeInt() & 0xffffffffL)));
            }

            series.addOrUpdate(finalX, finalY);
        }
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IllegalArgumentException: Null 'x' argument.

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,781 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Comparable key;
        if (data.consumeBoolean()) {
            key = data.consumeAsciiString(32);
        } else {
            key = Integer.valueOf(data.consumeInt());
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (data.consumeBoolean()) {
            series.setMaximumItemCount(data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        }

        int ops = data.consumeInt(0, 64);
        for (int i = 0; i < ops; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
            }

            Number x;
            int xKind = data.consumeInt(0, 6);
            switch (xKind) {
                case 0:
                    x = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    x = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    x = Double.valueOf((double) data.consumeInt());
                    break;
                case 3:
                    x = Float.valueOf((float) data.consumeInt());
                    break;
                case 4:
                    x = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    x = Byte.valueOf(data.consumeByte());
                    break;
                default:
                    x = null;
                    break;
            }

            Number y;
            int yKind = data.consumeInt(0, 6);
            switch (yKind) {
                case 0:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    y = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    y = Double.valueOf((double) data.consumeInt());
                    break;
                case 3:
                    y = Float.valueOf((float) data.consumeInt());
                    break;
                case 4:
                    y = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    y = Byte.valueOf(data.consumeByte());
                    break;
                default:
                    y = null;
                    break;
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                Number existingX = series.getX(idx);
                if (data.consumeBoolean()) {
                    x = existingX;
                } else if (existingX != null) {
                    double v = existingX.doubleValue();
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            x = Double.valueOf(v);
                            break;
                        case 1:
                            x = Float.valueOf((float) v);
                            break;
                        case 2:
                            x = Long.valueOf((long) v);
                            break;
                        default:
                            x = Integer.valueOf((int) v);
                            break;
                    }
                }
            }

            series.addOrUpdate(x, y);
        }
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: 0, Size: 0

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **30/30 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov; 30 raw fuzz log(s)**
<details><summary>▸ detail (~15,151 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 311}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 220}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 218}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 313}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 312}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 216}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 367}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 312}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 313}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 370}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 314}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 311}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
