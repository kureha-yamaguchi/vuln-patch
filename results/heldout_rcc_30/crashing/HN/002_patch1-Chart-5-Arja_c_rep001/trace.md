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
<details><summary>▸ output — FailureTest(test_class='org.jfree.data.xy.junit.XYSeriesTests', test_method='testBug1955483', source (~759 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.jfree.data.xy.junit.XYSeriesTests', test_method='testBug1955483', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/Chart_5_buggy/tests/org/jfree/data/xy/junit/XYSeriesTests.java', method_source='    public void testBug1955483() {\\n        XYSeries series = new XYSeries(\"Series\", true, true);\\n        series.addOrUpdate(1.0, 1.0);\\n        series.addOrUpdate(1.0, 2.0);\\n        assertEquals(new Double(1.0), series.getY(0));\\n        assertEquals(new Double(2.0), series.getY(1));\\n        assertEquals(2, series.getItemCount());\\n    }', exception_type='java.lang.IndexOutOfBoundsException', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~21,675 chars)</summary>

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
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `addOrUpdate`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
<details open><summary>▸ Output (~6,480 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario("anchor", 1.0, 1.0, 2.0, 3, data);
        if (data.remainingBytes() <= 0) {
            return;
        }

        int rounds = 1 + Math.abs(data.consumeInt(1, 4));
        for (int i = 0; i < rounds; i++) {
            double x = boundedDouble(data.consumeInt(-1000000, 1000000), data.consumeBoolean());
            double y1 = boundedDouble(data.consumeInt(-1000000, 1000000), data.consumeBoolean());
            double y2 = boundedDouble(data.consumeInt(-1000000, 1000000), data.consumeBoolean());

            if (sameNumber(y1, y2)) {
                y2 = y2 + 1.0;
            }

            int maxCount = data.consumeInt(2, 8);
            runScenario("explore-" + i, x, y1, y2, maxCount, data);
        }
    }

    private static void runScenario(String tag, double x, double y1, double y2, int maximumItemCount, FuzzedDataProvider data) {
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:getAutoSort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=" + tag + " lhs=" + series.getAutoSort() + " rhs=true");
        }

        XYSeries equalPeer = new XYSeries(key, true, true);
        if (!series.equals(equalPeer)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: two fresh series built with same constructor state should be equal input=" + tag);
        }
        if (series.hashCode() != equalPeer.hashCode()) {
            throw new RuntimeException("[oracle:hash-empty] metamorphic violation: equal objects must have equal hash codes input=" + tag + " lhs=" + series.hashCode() + " rhs=" + equalPeer.hashCode());
        }

        try {
            series.setMaximumItemCount(maximumItemCount);
            equalPeer.setMaximumItemCount(maximumItemCount);

            series.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
            equalPeer.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));

            if (!series.equals(equalPeer)) {
                throw new RuntimeException("[oracle:equals-after-first] metamorphic violation: equal series must stay equal after equivalent addOrUpdate input=" + tag);
            }
            if (series.hashCode() != equalPeer.hashCode()) {
                throw new RuntimeException("[oracle:hash-after-first] metamorphic violation: equal objects must have equal hash codes after equivalent mutation input=" + tag + " lhs=" + series.hashCode() + " rhs=" + equalPeer.hashCode());
            }

            series.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));
            equalPeer.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: with allowDuplicateXValues=true, two addOrUpdate calls using the same x must retain two items input="
                        + tag + " x=" + x + " y1=" + y1 + " y2=" + y2 + " lhs=" + series.getItemCount() + " rhs=2");
            }

            Number gotY0 = series.getY(0);
            Number gotY1 = series.getY(1);

            if (!sameNumber(numberToDouble(gotY0), y1) || !sameNumber(numberToDouble(gotY1), y2)) {
                throw new RuntimeException("[oracle:dup-order] metamorphic violation: documented test coverage shows duplicate x-values in an auto-sorted series must preserve both y values in insertion order for equal x input="
                        + tag + " x=" + x + " y1=" + y1 + " y2=" + y2 + " lhs0=" + gotY0 + " lhs1=" + gotY1);
            }

            if (!series.equals(equalPeer)) {
                throw new RuntimeException("[oracle:equals-after-second] metamorphic violation: equal series must agree on shared state written via constructor/setMaximumItemCount/addOrUpdate input=" + tag);
            }
            if (series.hashCode() != equalPeer.hashCode()) {
                throw new RuntimeException("[oracle:hash-after-second] metamorphic violation: equal objects must have equal hash codes after duplicate insertion input=" + tag + " lhs=" + series.hashCode() + " rhs=" + equalPeer.hashCode());
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException || cur instanceof org.jfree.data.general.SeriesException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException || t instanceof ArrayIndexOutOfBoundsException || t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double boundedDouble(int v, boolean halfStep) {
        double d = v;
        if (halfStep) {
            d += 0.5d;
        }
        return d;
    }

    private static double numberToDouble(Number n) {
        return n.doubleValue();
    }

    private static boolean sameNumber(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
<details open><summary>▸ Output (~7,278 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression from XYSeriesTests.testBug1955483.
        XYSeries anchor = new XYSeries("Series", true, true);
        try {
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            boolean fromAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    fromAddOrUpdate = true;
                    break;
                }
            }
            if (fromAddOrUpdate && t instanceof IndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException) {
                return;
            }
            return;
        }

        // Contract/oracle: with allowDuplicateXValues=true and autoSort=true,
        // addOrUpdate() must add a second item for the duplicate x, preserving sorted order.
        if (anchor.getItemCount() != 2
                || !Double.valueOf(1.0).equals(anchor.getY(0))
                || !Double.valueOf(2.0).equals(anchor.getY(1))) {
            throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: duplicate addOrUpdate must retain both items input=x=1.0 ys=[1.0,2.0] count="
                            + anchor.getItemCount()
                            + " y0=" + anchor.getY(0)
                            + " y1=" + anchor.getY(1));
        }
        if (!anchor.getAutoSort()) {
            throw new RuntimeException(
                    "[oracle:autosort] metamorphic violation: constructor-established autoSort flag not reported input=anchor reported="
                            + anchor.getAutoSort());
        }

        // EXPLORE: same root cause property with varied, valid-by-construction inputs.
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 5);
        int suffixCount = data.consumeInt(0, 5);
        int baseX = data.consumeInt(-1000, 1000);
        int y1 = data.consumeInt(-1000, 1000);
        int y2 = data.consumeInt(-1000, 1000);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        int totalAdds = prefixCount + 2 + suffixCount;
        int maxCount = data.consumeInt(totalAdds, totalAdds + 3);
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException(
                    "[oracle:getAutoSort] metamorphic violation: getAutoSort must agree with constructor input=true reportedLeft="
                            + viaAddOrUpdate.getAutoSort()
                            + " reportedRight=" + viaAdd.getAutoSort());
        }

        try {
            for (int i = 0; i < prefixCount; i++) {
                int x = baseX - (prefixCount - i);
                Integer xi = Integer.valueOf(x);
                Integer yi = Integer.valueOf(data.consumeInt(-1000, 1000));
                viaAddOrUpdate.addOrUpdate(xi, yi);
                viaAdd.add(xi, yi);
            }

            viaAddOrUpdate.addOrUpdate(Integer.valueOf(baseX), Integer.valueOf(y1));
            viaAdd.add(Integer.valueOf(baseX), Integer.valueOf(y1));

            viaAddOrUpdate.addOrUpdate(Integer.valueOf(baseX), Integer.valueOf(y2));
            viaAdd.add(Integer.valueOf(baseX), Integer.valueOf(y2));

            for (int i = 0; i < suffixCount; i++) {
                int x = baseX + 1 + i;
                Integer xi = Integer.valueOf(x);
                Integer yi = Integer.valueOf(data.consumeInt(-1000, 1000));
                viaAddOrUpdate.addOrUpdate(xi, yi);
                viaAdd.add(xi, yi);
            }
        } catch (RuntimeException t) {
            boolean fromAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    fromAddOrUpdate = true;
                    break;
                }
            }
            if (fromAddOrUpdate && t instanceof IndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException) {
                return;
            }
            return;
        }

        // Metamorphic relation: when duplicates are allowed, feeding the same sequence
        // through addOrUpdate(Number, Number) and add(Number, Number) must produce
        // equivalent series. A throw-deleting or overwrite patch breaks this.
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException(
                    "[oracle:eq-add-vs-addOrUpdate] metamorphic violation: equivalent inputs via add and addOrUpdate must yield equal series input=baseX="
                            + baseX + " y1=" + y1 + " y2=" + y2
                            + " prefixCount=" + prefixCount
                            + " suffixCount=" + suffixCount
                            + " lhsCount=" + viaAddOrUpdate.getItemCount()
                            + " rhsCount=" + viaAdd.getItemCount());
        }
        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException(
                    "[oracle:hash-eq] metamorphic violation: equal series must have equal hashCode input=baseX="
                            + baseX + " y1=" + y1 + " y2=" + y2
                            + " lhsHash=" + viaAddOrUpdate.hashCode()
                            + " rhsHash=" + viaAdd.hashCode());
        }
        if (viaAddOrUpdate.getItemCount() != totalAdds) {
            throw new RuntimeException(
                    "[oracle:itemCount] metamorphic violation: duplicates allowed should retain every inserted item input=expectedCount="
                            + totalAdds + " actual=" + viaAddOrUpdate.getItemCount());
        }
        for (int i = 1; i < viaAddOrUpdate.getItemCount(); i++) {
            Number prev = viaAddOrUpdate.getX(i - 1);
            Number curr = viaAddOrUpdate.getX(i);
            if (prev.doubleValue() > curr.doubleValue()) {
                throw new RuntimeException(
                        "[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing input=index="
                                + i + " prev=" + prev + " curr=" + curr);
            }
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
<details open><summary>▸ Output (~9,050 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorScenario();

        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int lowerCount = data.consumeInt(0, 6);
        int upperCount = data.consumeInt(0, 6);
        int existingDupCount = data.consumeInt(1, 4);
        int dupXInt = data.consumeInt(-1000, 1000);
        double dupX = dupXInt;

        int maxCountSpare = data.consumeInt(0, 4);
        int configuredMax = lowerCount + upperCount + existingDupCount + 1 + maxCountSpare;

        double newY = data.consumeInt(-1000, 1000);

        XYSeries lhs = new XYSeries(key, true, true);
        XYSeries rhs = new XYSeries(key, true, true);

        try {
            lhs.setMaximumItemCount(configuredMax);
            rhs.setMaximumItemCount(configuredMax);

            // Constructor/getter and setter/getter agreement over shared state.
            if (!lhs.getAutoSort() || !rhs.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor/getAutoSort disagreement key=" + key);
            }
            if (lhs.getMaximumItemCount() != configuredMax || rhs.getMaximumItemCount() != configuredMax) {
                throw new RuntimeException("[oracle:max-count] metamorphic violation: setMaximumItemCount/getMaximumItemCount disagreement configuredMax=" + configuredMax + " lhs=" + lhs.getMaximumItemCount() + " rhs=" + rhs.getMaximumItemCount());
            }

            int total = lowerCount + upperCount + existingDupCount;
            double[] xs = new double[total];
            double[] ys = new double[total];
            int idx = 0;

            for (int i = 0; i < lowerCount; i++) {
                xs[idx] = dupX - (lowerCount - i);
                ys[idx] = data.consumeInt(-1000, 1000);
                idx++;
            }
            for (int i = 0; i < existingDupCount; i++) {
                xs[idx] = dupX;
                ys[idx] = data.consumeInt(-1000, 1000);
                idx++;
            }
            for (int i = 0; i < upperCount; i++) {
                xs[idx] = dupX + i + 1;
                ys[idx] = data.consumeInt(-1000, 1000);
                idx++;
            }

            // Vary surrounding insertion order while keeping inputs valid by construction.
            for (int i = total - 1; i > 0; i--) {
                int j = data.consumeInt(0, i);
                double tx = xs[i];
                xs[i] = xs[j];
                xs[j] = tx;
                double ty = ys[i];
                ys[i] = ys[j];
                ys[j] = ty;
            }

            for (int i = 0; i < total; i++) {
                lhs.add(xs[i], ys[i]);
                rhs.add(xs[i], ys[i]);
            }

            int beforeCount = lhs.getItemCount();

            // Documented behavior from addOrUpdate body: when duplicate x-values are allowed,
            // the method takes the "add new item" path, not the overwrite path. Therefore
            // itemCount must increase by exactly one on valid duplicate-x input.
            lhs.addOrUpdate(new Double(dupX), new Double(newY));

            // Same-name overloads over equivalent inputs must agree.
            rhs.addOrUpdate(dupX, newY);

            int afterCount = lhs.getItemCount();
            if (afterCount != beforeCount + 1) {
                throw new RuntimeException("[oracle:item-count] metamorphic violation: addOrUpdate with duplicates allowed must add one item key=" + key + " x=" + dupX + " before=" + beforeCount + " after=" + afterCount);
            }

            if (!lhs.equals(rhs) || !rhs.equals(lhs)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent addOrUpdate overloads disagree key=" + key + " x=" + dupX + " lhsCount=" + lhs.getItemCount() + " rhsCount=" + rhs.getItemCount());
            }
            if (lhs.hashCode() != rhs.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode key=" + key + " x=" + dupX + " lhsHash=" + lhs.hashCode() + " rhsHash=" + rhs.hashCode());
            }

            int dupCount = 0;
            int lastDupIndex = -1;
            for (int i = 0; i < lhs.getItemCount(); i++) {
                Number x = lhs.getX(i);
                if (x != null && x.doubleValue() == dupX) {
                    dupCount++;
                    lastDupIndex = i;
                }
            }
            if (dupCount != existingDupCount + 1) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x count mismatch key=" + key + " x=" + dupX + " expected=" + (existingDupCount + 1) + " actual=" + dupCount);
            }
            if (lastDupIndex >= 0) {
                Number observedY = lhs.getY(lastDupIndex);
                double observed = observedY == null ? Double.NaN : observedY.doubleValue();
                if (observed != newY) {
                    throw new RuntimeException("[oracle:dup-order] metamorphic violation: newest duplicate should be observable at the last equal-x slot key=" + key + " x=" + dupX + " expectedY=" + newY + " observedY=" + observed);
                }
            }
        } catch (RuntimeException e) {
            if (isOracleViolation(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }
    }

    private static void runAnchorScenario() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));

            // This is the exact observable from the regression test:
            // after adding a duplicate x with duplicates allowed, both items remain.
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: anchor regression itemCount expected=2 actual=" + series.getItemCount());
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            double v0 = y0 == null ? Double.NaN : y0.doubleValue();
            double v1 = y1 == null ? Double.NaN : y1.doubleValue();
            if (v0 != 1.0 || v1 != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchor regression expected y[0]=1.0 y[1]=2.0 actual y[0]=" + v0 + " y[1]=" + v1);
            }

            XYSeries expected = new XYSeries("Series", true, true);
            expected.add(1.0, 1.0);
            expected.add(1.0, 2.0);
            if (!series.equals(expected) || !expected.equals(series)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: anchor series disagrees with equivalent real-API construction");
            }
            if (series.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal anchor series must have equal hashCode");
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: getAutoSort disagrees with constructor");
            }
        } catch (RuntimeException e) {
            if (isOracleViolation(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static boolean isOracleViolation(Throwable t) {
        return t != null
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.endsWith("SeriesException");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~9,024 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: faithful copy of the failing test's real public API path.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            // Documented/observed contract from the regression test:
            // with allowDuplicateXValues=true, two addOrUpdate() calls for the same x
            // must retain both items. A throw-deleting or wrong-insertion patch would
            // violate count/order/content here even if no exception were thrown.
            if (series.getItemCount() != 2
                    || !Double.valueOf(1.0).equals(series.getY(0))
                    || !Double.valueOf(2.0).equals(series.getY(1))) {
                throw new RuntimeException(
                        "[oracle:anchor] metamorphic violation: duplicate addOrUpdate must retain both items in order "
                                + "input=x=1.0 ys=[1.0,2.0] count=" + series.getItemCount()
                                + " y0=" + safeY(series, 0) + " y1=" + safeY(series, 1));
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: valid-by-construction duplicate-x inputs with autoSort=true and
        // allowDuplicateXValues=true, plus varied surrounding content and positions.
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 4);
        int duplicateCount = data.consumeInt(2, 6);
        int suffixCount = data.consumeInt(0, 4);
        int dupBase = data.consumeInt(-1000, 1000);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        try {
            // Constructor/shared-state readers should agree with what construction established.
            if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
                throw new RuntimeException(
                        "[oracle:ctor] metamorphic violation: constructor-established autoSort=true not reported by getAutoSort "
                                + "input=key=" + key + " lhs=" + viaAddOrUpdate.getAutoSort()
                                + " rhs=" + viaAdd.getAutoSort());
            }

            int step = 0;

            for (int i = 0; i < prefixCount; i++) {
                double x = dupBase - (prefixCount - i);
                double y = boundedDouble(data.consumeInt(-1000000, 1000000), step);
                viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                viaAdd(x, y, viaAddOrUpdate, viaAdd, ++step, key);
            }

            for (int i = 0; i < duplicateCount; i++) {
                double x = dupBase;
                double y = boundedDouble(data.consumeInt(-1000000, 1000000), step);
                viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                viaAdd(x, y, viaAddOrUpdate, viaAdd, ++step, key);
            }

            for (int i = 0; i < suffixCount; i++) {
                double x = dupBase + 1 + i;
                double y = boundedDouble(data.consumeInt(-1000000, 1000000), step);
                viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                viaAdd(x, y, viaAddOrUpdate, viaAdd, ++step, key);
            }

            // Sibling-agreement oracle:
            // addOrUpdate(Number, Number) and add(double, double) are same-name family members
            // operating over the same input space. When duplicate x-values are allowed,
            // addOrUpdate() should add a new item rather than overwrite, so building two
            // series from the same valid inputs through these real API siblings must agree.
            if (!viaAddOrUpdate.equals(viaAdd)) {
                throw new RuntimeException(
                        "[oracle:sibling-eq] metamorphic violation: series built via addOrUpdate must equal series built via add "
                                + "input=key=" + key + " dupX=" + dupBase
                                + " prefix=" + prefixCount + " duplicates=" + duplicateCount + " suffix=" + suffixCount
                                + " lhsCount=" + viaAddOrUpdate.getItemCount()
                                + " rhsCount=" + viaAdd.getItemCount());
            }

            if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
                throw new RuntimeException(
                        "[oracle:hash] metamorphic violation: equal series must have equal hashCode "
                                + "input=key=" + key + " dupX=" + dupBase
                                + " lhsHash=" + viaAddOrUpdate.hashCode()
                                + " rhsHash=" + viaAdd.hashCode());
            }

            // Additional observable contract: with autoSort=true, the visible x sequence
            // must be nondecreasing after successful additions.
            for (int i = 1; i < viaAddOrUpdate.getItemCount(); i++) {
                Number prev = viaAddOrUpdate.getX(i - 1);
                Number curr = viaAddOrUpdate.getX(i);
                if (prev.doubleValue() > curr.doubleValue()) {
                    throw new RuntimeException(
                            "[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing "
                                    + "input=key=" + key + " dupX=" + dupBase
                                    + " i=" + i + " prev=" + prev + " curr=" + curr);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void viaAdd(double x, double y, XYSeries viaAddOrUpdate, XYSeries viaAdd,
                               int step, String key) {
        viaAdd.add(x, y);

        if (viaAddOrUpdate.getAutoSort() != viaAdd.getAutoSort() || !viaAddOrUpdate.getAutoSort()) {
            throw new RuntimeException(
                    "[oracle:autosort] metamorphic violation: getAutoSort must consistently report constructor-established state "
                            + "input=key=" + key + " step=" + step
                            + " lhs=" + viaAddOrUpdate.getAutoSort()
                            + " rhs=" + viaAdd.getAutoSort());
        }

        if (viaAddOrUpdate.getItemCount() != viaAdd.getItemCount()) {
            throw new RuntimeException(
                    "[oracle:count] metamorphic violation: addOrUpdate with duplicate-allowed series must grow like add "
                            + "input=key=" + key + " step=" + step
                            + " lhs=" + viaAddOrUpdate.getItemCount()
                            + " rhs=" + viaAdd.getItemCount());
        }
    }

    private static double boundedDouble(int n, int salt) {
        return ((double) n) + (salt * 0.01d);
    }

    private static Number safeY(XYSeries s, int idx) {
        try {
            return s.getY(idx);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null && (name.contains("SeriesException") || name.contains("Invalid"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCauseFromAddOrUpdate(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
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
<details open><summary>▸ Output (~7,572 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorTrigger();

        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int sharedX = data.consumeInt(-1000000, 1000000);
        int firstY = data.consumeInt(-1000000, 1000000);
        int duplicateCount = data.consumeInt(2, 8);

        try {
            exploreDuplicateInsertions(key, sharedX, firstY, duplicateCount, data);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
        }
    }

    private static void anchorTrigger() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on a duplicate-allowing series must retain both items input=x=1.0 ys=[1.0,2.0] lhs=" + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0d || y1.doubleValue() != 2.0d) {
                throw new RuntimeException("[oracle:anchor-order] metamorphic violation: failing test contract requires y values [1.0,2.0] after two duplicate addOrUpdate calls input=x=1.0 lhs=[" + y0 + "," + y1 + "] rhs=[1.0,2.0]");
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            throw t;
        }
    }

    private static void exploreDuplicateInsertions(String key, int sharedX, int firstY, int duplicateCount, FuzzedDataProvider data) {
        XYSeries series = new XYSeries(key, true, true);
        XYSeries mirror = new XYSeries(key, true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor argument autoSort=true must be reported by getAutoSort input=key=" + key + " lhs=" + series.getAutoSort() + " rhs=true");
        }
        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: two fresh series built with identical constructor arguments must compare equal input=key=" + key + " lhs=" + series.equals(mirror) + " rhs=true");
        }

        int maxCount = data.consumeInt(duplicateCount, duplicateCount + 4);
        series.setMaximumItemCount(maxCount);
        mirror.setMaximumItemCount(maxCount);

        if (series.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-reader] metamorphic violation: setMaximumItemCount must be reported by getMaximumItemCount input=max=" + maxCount + " lhs=" + series.getMaximumItemCount() + " rhs=" + maxCount);
        }
        if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:maxcount-equals] metamorphic violation: equal series must remain equal after applying the same maximumItemCount input=max=" + maxCount + " lhsEquals=" + series.equals(mirror) + " lhsHash=" + series.hashCode() + " rhsHash=" + mirror.hashCode());
        }

        for (int i = 0; i < duplicateCount; i++) {
            int yi = (i == 0) ? firstY : data.consumeInt(-1000000, 1000000);

            try {
                if (data.consumeBoolean()) {
                    series.addOrUpdate(new Integer(sharedX), new Integer(yi));
                } else {
                    series.addOrUpdate((double) sharedX, (double) yi);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseFromAddOrUpdate(t)) {
                    throw t;
                }
                return;
            }

            try {
                mirror.addOrUpdate(new Integer(sharedX), new Integer(yi));
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseFromAddOrUpdate(t)) {
                    throw t;
                }
                return;
            }

            int expectedCount = i + 1;
            if (series.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: in a series constructed with allowDuplicateXValues=true and maxCount>=insertions, each duplicate addOrUpdate must increase item count by one input=x=" + sharedX + " step=" + i + " lhs=" + series.getItemCount() + " rhs=" + expectedCount);
            }

            Number lastY = series.getY(series.getItemCount() - 1);
            if (lastY == null || lastY.doubleValue() != yi) {
                throw new RuntimeException("[oracle:dup-lasty] metamorphic violation: with repeated equal x values inserted in order, the just-added duplicate must remain observable at the new tail for this valid duplicate-allowing scenario input=x=" + sharedX + " y=" + yi + " lhs=" + lastY + " rhs=" + yi);
            }

            if (!series.equals(mirror) || !mirror.equals(series)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: addOrUpdate(double,double) and addOrUpdate(Number,Number) on equivalent fresh state must produce equal series input=x=" + sharedX + " step=" + i + " lhsEquals=" + series.equals(mirror) + " rhs=true");
            }
            if (series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:hash-agree] metamorphic violation: equal series after equivalent addOrUpdate overloads must have equal hashCode input=x=" + sharedX + " step=" + i + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name != null && name.contains("SeriesException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromAddOrUpdate(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
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
<details open><summary>▸ Output (~8,333 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            // Contract exercised by the original regression test: with autoSort=true and
            // allowDuplicateXValues=true, addOrUpdate() on the same x adds a second item
            // rather than overwriting the first. A "fix" that only suppresses the throw,
            // skips insertion, or inserts into the wrong state would violate these reads.
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate must retain both items input=x=1.0 ys=[1.0,2.0] lhs=" + series.getItemCount() + " rhs=2");
            }
            if (!Double.valueOf(1.0).equals(series.getY(0))) {
                throw new RuntimeException("[oracle:anchor-y0] metamorphic violation: first duplicate must remain observable input=x=1.0 ys=[1.0,2.0] lhs=" + series.getY(0) + " rhs=1.0");
            }
            if (!Double.valueOf(2.0).equals(series.getY(1))) {
                throw new RuntimeException("[oracle:anchor-y1] metamorphic violation: second duplicate must be appended for equal x input=x=1.0 ys=[1.0,2.0] lhs=" + series.getY(1) + " rhs=2.0");
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort flag changed input=true lhs=false rhs=true");
            }

            XYSeries expected = new XYSeries("Series", true, true);
            expected.add(1.0, 1.0);
            expected.add(1.0, 2.0);
            if (!series.equals(expected)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) when duplicate x values are allowed input=x=1.0 ys=[1.0,2.0] lhs=" + series.getItems() + " rhs=" + expected.getItems());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int duplicateX = data.consumeInt(-1000, 1000);
        int duplicateCount = data.consumeInt(2, 8);
        int prefixCount = data.consumeInt(0, 8);
        int suffixCount = data.consumeInt(0, 8);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        try {
            if (viaAddOrUpdate.getAutoSort() != viaAdd.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: identically constructed series must agree on autoSort input=" + key + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
            }
            if (!viaAddOrUpdate.equals(viaAdd)) {
                throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: identically constructed empty series must be equal input=" + key + " lhs=" + viaAddOrUpdate.getItems() + " rhs=" + viaAdd.getItems());
            }

            int maxCount = data.consumeInt(duplicateCount, duplicateCount + prefixCount + suffixCount + 4);
            viaAddOrUpdate.setMaximumItemCount(maxCount);
            viaAdd.setMaximumItemCount(maxCount);

            for (int i = 0; i < prefixCount; i++) {
                int x = distinctX(duplicateX, data.consumeInt(-1000, 1000), i + 1);
                double y = (double) data.consumeInt(-1000, 1000);
                viaAddOrUpdate.addOrUpdate(Integer.valueOf(x), Double.valueOf(y));
                viaAdd.add((double) x, y);
                assertSeriesAgreement(viaAddOrUpdate, viaAdd, "prefix-" + i);
            }

            for (int i = 0; i < duplicateCount; i++) {
                double y = (double) data.consumeInt(-1000, 1000);
                viaAddOrUpdate.addOrUpdate(Integer.valueOf(duplicateX), Double.valueOf(y));
                viaAdd.add((double) duplicateX, y);
                assertSeriesAgreement(viaAddOrUpdate, viaAdd, "dup-" + i);
            }

            for (int i = 0; i < suffixCount; i++) {
                int x = distinctX(duplicateX, data.consumeInt(-1000, 1000), prefixCount + duplicateCount + i + 1);
                double y = (double) data.consumeInt(-1000, 1000);
                viaAddOrUpdate.addOrUpdate(Integer.valueOf(x), Double.valueOf(y));
                viaAdd.add((double) x, y);
                assertSeriesAgreement(viaAddOrUpdate, viaAdd, "suffix-" + i);
            }

            // Documented behavior from the method body and constructor flags: when duplicate
            // x values are allowed, addOrUpdate() follows the add-new-item path. Therefore,
            // feeding the same valid operation sequence through addOrUpdate(Number,Number)
            // and add(double,double) must leave equal observable series state.
            assertSeriesAgreement(viaAddOrUpdate, viaAdd, "final");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void assertSeriesAgreement(XYSeries lhs, XYSeries rhs, String stage) {
        if (lhs.getItemCount() != rhs.getItemCount()) {
            throw new RuntimeException("[oracle:count] metamorphic violation: addOrUpdate(Number,Number) and add(double,double) must keep the same item count for duplicate-allowed series input=" + stage + " lhs=" + lhs.getItemCount() + " rhs=" + rhs.getItemCount());
        }
        if (lhs.getMaximumItemCount() != rhs.getMaximumItemCount()) {
            throw new RuntimeException("[oracle:max] metamorphic violation: series configured identically must retain maximumItemCount input=" + stage + " lhs=" + lhs.getMaximumItemCount() + " rhs=" + rhs.getMaximumItemCount());
        }
        if (lhs.getAutoSort() != rhs.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort] metamorphic violation: addOrUpdate must not change constructor-established autoSort state input=" + stage + " lhs=" + lhs.getAutoSort() + " rhs=" + rhs.getAutoSort());
        }
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) on valid duplicate-allowed inputs input=" + stage + " lhs=" + lhs.getItems() + " rhs=" + rhs.getItems());
        }
    }

    private static int distinctX(int forbidden, int candidate, int salt) {
        if (candidate != forbidden) {
            return candidate;
        }
        return candidate + salt;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~10,087 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorScenario();

        String key = data.consumeAsciiString(32);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        try {
            exploreDuplicateAddOrUpdate(data, key);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void anchorScenario() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on an auto-sorted series that allows duplicate x-values must retain both items input=1.0 lhs=" + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (!numEq(y0, 1.0) || !numEq(y1, 2.0)) {
                throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression scenario from XYSeriesTests.testBug1955483 must preserve both y-values in order input=1.0 lhs=(" + y0 + "," + y1 + ") rhs=(1.0,2.0)");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreDuplicateAddOrUpdate(FuzzedDataProvider data, String key) {
        boolean autoSort = true;
        boolean allowDup = true;

        XYSeries viaNumber = new XYSeries(key, autoSort, allowDup);
        XYSeries viaDouble = new XYSeries(key, autoSort, allowDup);

        if (!viaNumber.getAutoSort() || !viaDouble.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor argument autoSort=true must be reported by getAutoSort input=true lhs=(" + viaNumber.getAutoSort() + "," + viaDouble.getAutoSort() + ") rhs=true");
        }

        int maximum = data.consumeInt(2, 20);
        viaNumber.setMaximumItemCount(maximum);
        viaDouble.setMaximumItemCount(maximum);

        int uniqueCount = data.consumeInt(1, Math.max(1, maximum));
        double[] xs = new double[uniqueCount];

        int start = data.consumeInt(-1000, 1000);
        int current = start;
        for (int i = 0; i < uniqueCount; i++) {
            current += data.consumeInt(1, 5);
            xs[i] = current;
            double y = data.consumeInt(-1000, 1000);
            try {
                viaNumber.add(xs[i], y);
                viaDouble.add(xs[i], y);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            } catch (Error e) {
                if (isRootCause(e)) {
                    throw e;
                }
                return;
            }
        }

        if (!viaNumber.equals(viaDouble) || viaNumber.hashCode() != viaDouble.hashCode()) {
            throw new RuntimeException("[oracle:prefill-eq] metamorphic violation: two series built with the same constructor state and the same real add calls must agree by equals/hashCode input=prefill lhs=(" + viaNumber.equals(viaDouble) + "," + viaNumber.hashCode() + ") rhs=(true," + viaDouble.hashCode() + ")");
        }

        int targetIndex = data.consumeInt(0, uniqueCount - 1);
        double targetX = xs[targetIndex];
        double dupY = data.consumeInt(-1000, 1000);

        int beforeCount = viaNumber.getItemCount();
        int beforeOccurrences = countX(viaNumber, targetX);

        try {
            viaNumber.addOrUpdate(Double.valueOf(targetX), Double.valueOf(dupY));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        try {
            viaDouble.addOrUpdate(targetX, dupY);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract asserted:
           - constructor stores autoSort/allowDuplicateXValues state;
           - addOrUpdate must add a new item when duplicate x-values are allowed;
           - with autoSort enabled, resulting series remains ordered;
           - equivalent addOrUpdate overloads on equivalent inputs must produce equal series.
           A patch that only suppresses the crash, skips insertion, or inserts incorrectly breaks these observables. */
        try {
            if (viaNumber.getItemCount() != Math.min(beforeCount + 1, maximum)) {
                throw new RuntimeException("[oracle:count-grow] metamorphic violation: addOrUpdate with duplicate x allowed must increase the item count by one unless trimmed by maximumItemCount input=x=" + targetX + " lhs=" + viaNumber.getItemCount() + " rhs=" + Math.min(beforeCount + 1, maximum));
            }
            if (viaNumber.getItemCount() > viaNumber.getMaximumItemCount()) {
                throw new RuntimeException("[oracle:max-bound] metamorphic violation: series size must not exceed maximumItemCount after addOrUpdate input=max=" + viaNumber.getMaximumItemCount() + " lhs=" + viaNumber.getItemCount() + " rhs<=" + viaNumber.getMaximumItemCount());
            }
            if (!isSortedNonDecreasing(viaNumber) || !isSortedNonDecreasing(viaDouble)) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain in nondecreasing x order after addOrUpdate input=x=" + targetX + " lhs=unsorted rhs=sorted");
            }
            int afterOccurrences = countX(viaNumber, targetX);
            int expectedOccurrences = beforeOccurrences + 1;
            if (afterOccurrences != expectedOccurrences) {
                throw new RuntimeException("[oracle:dup-preserve] metamorphic violation: when duplicate x-values are allowed, addOrUpdate on an existing x must retain the old item and add a new one input=x=" + targetX + " lhs=" + afterOccurrences + " rhs=" + expectedOccurrences);
            }
            if (!viaNumber.equals(viaDouble) || viaNumber.hashCode() != viaDouble.hashCode()) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent addOrUpdate overloads must produce equal series on the same valid input input=x=" + targetX + ",y=" + dupY + " lhs=(" + viaNumber.equals(viaDouble) + "," + viaNumber.hashCode() + ") rhs=(true," + viaDouble.hashCode() + ")");
            }
            if (viaNumber.getAutoSort() != autoSort || viaDouble.getAutoSort() != autoSort) {
                throw new RuntimeException("[oracle:autosort-state] metamorphic violation: addOrUpdate must not mutate constructor-defined autoSort state input=true lhs=(" + viaNumber.getAutoSort() + "," + viaDouble.getAutoSort() + ") rhs=true");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            throw t;
        }
    }

    private static int countX(XYSeries s, double x) {
        int count = 0;
        int n = s.getItemCount();
        for (int i = 0; i < n; i++) {
            Number xi = s.getX(i);
            if (xi != null && Double.compare(xi.doubleValue(), x) == 0) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSortedNonDecreasing(XYSeries s) {
        int n = s.getItemCount();
        double prev = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            Number xi = s.getX(i);
            if (xi == null) {
                return false;
            }
            double cur = xi.doubleValue();
            if (i > 0 && cur < prev) {
                return false;
            }
            prev = cur;
        }
        return true;
    }

    private static boolean numEq(Number n, double d) {
        return n != null && Double.compare(n.doubleValue(), d) == 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().endsWith("SeriesException");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException) && !(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,139 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(true, true, 1.0, 1.0, 1.0, 2.0, 3, 2, 5);

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        double dupX = boundedDouble(data.consumeInt());
        double firstY = boundedDouble(data.consumeInt());
        double secondY = boundedDouble(data.consumeInt());

        int extrasBefore = data.consumeInt(0, 4);
        int extrasBetween = data.consumeInt(0, 4);
        int extrasAfter = data.consumeInt(0, 4);

        runScenario(autoSort, allowDuplicateXValues, dupX, firstY, secondY,
                extrasBefore, extrasBetween, extrasAfter);
    }

    private static void runScenario(boolean autoSort, boolean allowDuplicateXValues,
                                    double dupX, double firstY, double secondY,
                                    int extrasBefore, int extrasBetween, int extrasAfter) {
        XYSeries viaAddOrUpdate = new XYSeries("Series", autoSort, allowDuplicateXValues);
        XYSeries viaAdd = new XYSeries("Series", autoSort, allowDuplicateXValues);

        int maxCount = 2 + extrasBefore + extrasBetween + extrasAfter;
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getAutoSort() != autoSort || viaAdd.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-readers] metamorphic violation: constructor-established autoSort disagrees with getAutoSort input="
                    + autoSort + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
        }
        if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-readers] metamorphic violation: setMaximumItemCount disagrees with getMaximumItemCount input="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }
        if (!viaAddOrUpdate.equals(viaAdd) || viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:initial-eq] metamorphic violation: equal fresh series must agree on equals/hashCode input="
                    + autoSort + "/" + allowDuplicateXValues + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        try {
            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBefore, -1000);
            safeAddOrUpdate(viaAddOrUpdate, dupX, firstY);
            viaAdd.add(dupX, firstY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBetween, 0);
            safeAddOrUpdate(viaAddOrUpdate, dupX, secondY);
            viaAdd.add(dupX, secondY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasAfter, 1000);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract/oracle:
         * For duplicate-x-allowed series, addOrUpdate(Number, Number) adds a new item
         * instead of overwriting, and same-name add(...) overloads are documented to
         * agree on equivalent inputs. Therefore, driving the same valid sequence through
         * addOrUpdate(Number,Number) and add(Number,Number) must yield equal series.
         * A "fix" that merely avoids the throw by skipping, overwriting, or appending
         * incorrectly will break this observable equality.
         */
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent add/addOrUpdate sequences produced different series input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhsCount=" + viaAddOrUpdate.getItemCount()
                    + " rhsCount=" + viaAdd.getItemCount());
        }
        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash] metamorphic violation: equal series must have equal hashCode input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhs=" + viaAddOrUpdate.hashCode()
                    + " rhs=" + viaAdd.hashCode());
        }

        if (autoSort && allowDuplicateXValues) {
            int expectedCount = 2 + extrasBefore + extrasBetween + extrasAfter;
            if (viaAddOrUpdate.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate should increase item count on valid duplicate-x input="
                        + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                        + " lhs=" + viaAddOrUpdate.getItemCount()
                        + " rhs=" + expectedCount);
            }
        }
    }

    private static void addExtras(XYSeries viaAddOrUpdate, XYSeries viaAdd,
                                  double dupX, int count, int baseOffset) {
        for (int i = 0; i < count; i++) {
            double x = dupX + baseOffset + i + 1.0;
            if (x == dupX) {
                x += 0.5;
            }
            double y = baseOffset + i + 0.25;
            safeAddOrUpdate(viaAddOrUpdate, x, y);
            viaAdd.add(x, y);
        }
    }

    private static void safeAddOrUpdate(XYSeries s, double x, double y) {
        s.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(f.getClassName())
                    && "addOrUpdate".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException") || name.contains("Validation");
    }

    private static double boundedDouble(int v) {
        return (v % 1000000) / 10.0;
    }

    private static String describe(double dupX, double firstY, double secondY,
                                   int extrasBefore, int extrasBetween, int extrasAfter) {
        return "{x=" + dupX
                + ",y1=" + firstY
                + ",y2=" + secondY
                + ",before=" + extrasBefore
                + ",between=" + extrasBetween
                + ",after=" + extrasAfter + "}";
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,610 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(true, true, 1.0, 1.0, 1.0, 2.0, 3, 2, 5);

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        double dupX = boundedDouble(data.consumeInt());
        double firstY = boundedDouble(data.consumeInt());
        double secondY = boundedDouble(data.consumeInt());

        int extrasBefore = data.consumeInt(0, 4);
        int extrasBetween = data.consumeInt(0, 4);
        int extrasAfter = data.consumeInt(0, 4);

        runScenario(autoSort, allowDuplicateXValues, dupX, firstY, secondY,
                extrasBefore, extrasBetween, extrasAfter);
    }

    private static void runScenario(boolean autoSort, boolean allowDuplicateXValues,
                                    double dupX, double firstY, double secondY,
                                    int extrasBefore, int extrasBetween, int extrasAfter) {
        XYSeries viaAddOrUpdate = new XYSeries("Series", autoSort, allowDuplicateXValues);
        XYSeries viaAdd = new XYSeries("Series", autoSort, allowDuplicateXValues);

        int maxCount = 2 + extrasBefore + extrasBetween + extrasAfter;
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getAutoSort() != autoSort || viaAdd.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-readers] metamorphic violation: constructor-established autoSort disagrees with getAutoSort input="
                    + autoSort + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
        }
        if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-readers] metamorphic violation: setMaximumItemCount disagrees with getMaximumItemCount input="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }
        if (!viaAddOrUpdate.equals(viaAdd) || viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:initial-eq] metamorphic violation: equal fresh series must agree on equals/hashCode input="
                    + autoSort + "/" + allowDuplicateXValues + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        try {
            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBefore, -1000);
            safeAddOrUpdate(viaAddOrUpdate, dupX, firstY);
            viaAdd.add(dupX, firstY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBetween, 0);
            safeAddOrUpdate(viaAddOrUpdate, dupX, secondY);
            viaAdd.add(dupX, secondY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasAfter, 1000);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract/oracle:
         * For duplicate-x-allowed series, addOrUpdate(Number, Number) adds a new item
         * instead of overwriting, and same-name add(...) overloads are documented to
         * agree on equivalent inputs. Therefore, driving the same valid sequence through
         * addOrUpdate(Number,Number) and add(Number,Number) must yield equal series.
         * A "fix" that merely avoids the throw by skipping, overwriting, or appending
         * incorrectly will break this observable equality.
         */
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent add/addOrUpdate sequences produced different series input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhsCount=" + viaAddOrUpdate.getItemCount()
                    + " rhsCount=" + viaAdd.getItemCount());
        }
        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash] metamorphic violation: equal series must have equal hashCode input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhs=" + viaAddOrUpdate.hashCode()
                    + " rhs=" + viaAdd.hashCode());
        }

        if (autoSort && allowDuplicateXValues) {
            int expectedCount = 2 + extrasBefore + extrasBetween + extrasAfter;
            if (viaAddOrUpdate.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate should increase item count on valid duplicate-x input="
                        + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                        + " lhs=" + viaAddOrUpdate.getItemCount()
                        + " rhs=" + expectedCount);
            }
        }
    }

    private static void addExtras(XYSeries viaAddOrUpdate, XYSeries viaAdd,
                                  double dupX, int count, int baseOffset) {
        for (int i = 0; i < count; i++) {
            double x = dupX + baseOffset + i + 1.0;
            if (x == dupX) {
                x += 0.5;
            }
            double y = baseOffset + i + 0.25;
            safeAddOrUpdate(viaAddOrUpdate, x, y);
            viaAdd.add(x, y);
        }
    }

    private static void safeAddOrUpdate(XYSeries s, double x, double y) {
        s.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(f.getClassName())
                    && "addOrUpdate".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException") || name.contains("Validation");
    }

    private static double boundedDouble(int v) {
        return (v % 1000000) / 10.0;
    }

    private static String describe(double dupX, double firstY, double secondY,
                                   int extrasBefore, int extrasBetween, int extrasAfter) {
        return "{x=" + dupX
                + ",y1=" + firstY
                + ",y2=" + secondY
                + ",before=" + extrasBefore
                + ",between=" + extrasBetween
                + ",after=" + extrasAfter + "}";
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/Chart_5_buggy/fuzz/attempt_008/FuzzHarness.java:7: error: method runScenario in class FuzzHarness cannot be applied to given types;
        runScenario(true, true, 1.0, 1.0, 1.0, 2.0, 3, 2, 5);
        ^
  required: boolean,boolean,double,double,double,int,int,int
  found: boolean,boolean,double,double,double,double,int,int,int
  reason: actual and formal argument lists differ in length
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~7,134 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(true, true, 1.0, 1.0, 2.0, 3, 2, 5);

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        double dupX = boundedDouble(data.consumeInt());
        double firstY = boundedDouble(data.consumeInt());
        double secondY = boundedDouble(data.consumeInt());

        int extrasBefore = data.consumeInt(0, 4);
        int extrasBetween = data.consumeInt(0, 4);
        int extrasAfter = data.consumeInt(0, 4);

        runScenario(autoSort, allowDuplicateXValues, dupX, firstY, secondY,
                extrasBefore, extrasBetween, extrasAfter);
    }

    private static void runScenario(boolean autoSort, boolean allowDuplicateXValues,
                                    double dupX, double firstY, double secondY,
                                    int extrasBefore, int extrasBetween, int extrasAfter) {
        XYSeries viaAddOrUpdate = new XYSeries("Series", autoSort, allowDuplicateXValues);
        XYSeries viaAdd = new XYSeries("Series", autoSort, allowDuplicateXValues);

        int maxCount = 2 + extrasBefore + extrasBetween + extrasAfter;
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getAutoSort() != autoSort || viaAdd.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-readers] metamorphic violation: constructor-established autoSort disagrees with getAutoSort input="
                    + autoSort + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
        }
        if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-readers] metamorphic violation: setMaximumItemCount disagrees with getMaximumItemCount input="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }
        if (!viaAddOrUpdate.equals(viaAdd) || viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:initial-eq] metamorphic violation: equal fresh series must agree on equals/hashCode input="
                    + autoSort + "/" + allowDuplicateXValues + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        try {
            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBefore, -1000);
            safeAddOrUpdate(viaAddOrUpdate, dupX, firstY);
            viaAdd.add(dupX, firstY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBetween, 0);
            safeAddOrUpdate(viaAddOrUpdate, dupX, secondY);
            viaAdd.add(dupX, secondY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasAfter, 1000);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract/oracle:
         * For duplicate-x-allowed series, addOrUpdate(Number, Number) adds a new item
         * instead of overwriting, and same-name add(...) overloads are documented to
         * agree on equivalent inputs. Therefore, driving the same valid sequence through
         * addOrUpdate(Number,Number) and add(Number,Number) must yield equal series.
         * A "fix" that merely avoids the throw by skipping, overwriting, or appending
         * incorrectly will break this observable equality.
         */
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent add/addOrUpdate sequences produced different series input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhsCount=" + viaAddOrUpdate.getItemCount()
                    + " rhsCount=" + viaAdd.getItemCount());
        }
        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash] metamorphic violation: equal series must have equal hashCode input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhs=" + viaAddOrUpdate.hashCode()
                    + " rhs=" + viaAdd.hashCode());
        }

        if (autoSort && allowDuplicateXValues) {
            int expectedCount = 2 + extrasBefore + extrasBetween + extrasAfter;
            if (viaAddOrUpdate.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate should increase item count on valid duplicate-x input="
                        + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                        + " lhs=" + viaAddOrUpdate.getItemCount()
                        + " rhs=" + expectedCount);
            }
        }
    }

    private static void addExtras(XYSeries viaAddOrUpdate, XYSeries viaAdd,
                                  double dupX, int count, int baseOffset) {
        for (int i = 0; i < count; i++) {
            double x = dupX + baseOffset + i + 1.0;
            if (x == dupX) {
                x += 0.5;
            }
            double y = baseOffset + i + 0.25;
            safeAddOrUpdate(viaAddOrUpdate, x, y);
            viaAdd.add(x, y);
        }
    }

    private static void safeAddOrUpdate(XYSeries s, double x, double y) {
        s.addOrUpdate(new Double(x), new Double(y));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(f.getClassName())
                    && "addOrUpdate".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("SeriesException") >= 0 || name.indexOf("Validation") >= 0;
    }

    private static double boundedDouble(int v) {
        return (v % 1000000) / 10.0;
    }

    private static String describe(double dupX, double firstY, double secondY,
                                   int extrasBefore, int extrasBetween, int extrasAfter) {
        return "{x=" + dupX
                + ",y1=" + firstY
                + ",y2=" + secondY
                + ",before=" + extrasBefore
                + ",between=" + extrasBetween
                + ",after=" + extrasAfter + "}";
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -4, Size: 6

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,607 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate with allowDuplicateXValues=true must retain both items input=x=1.0 ys=[1.0,2.0] lhsCount=" + anchor.getItemCount() + " rhsCount=2");
            }
            Number y0 = anchor.getY(0);
            Number y1 = anchor.getY(1);
            if (!Double.valueOf(1.0).equals(y0) || !Double.valueOf(2.0).equals(y1)) {
                throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression test output mismatch input=x=1.0 ys=[1.0,2.0] lhsY0=" + y0 + " lhsY1=" + y1 + " rhsY0=1.0 rhsY1=2.0");
            }
            XYSeries anchorExpected = new XYSeries("Series", true, true);
            anchorExpected.add(Double.valueOf(1.0), Double.valueOf(1.0));
            anchorExpected.add(Double.valueOf(1.0), Double.valueOf(2.0));
            if (!anchor.equals(anchorExpected)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: series built via addOrUpdate must equal series built via add for duplicate x when duplicates are allowed input=x=1.0 ys=[1.0,2.0] lhsCount=" + anchor.getItemCount() + " rhsCount=" + anchorExpected.getItemCount());
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                String n = t.getClass().getName();
                if (n.startsWith("org.jfree.") && n.toLowerCase().contains("exception")) {
                    validation = true;
                }
            }
            if (validation) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            boolean viaAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    viaAddOrUpdate = true;
                    break;
                }
            }
            if (rootCause && viaAddOrUpdate) {
                throw t;
            }
        }

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int lessCount = data.consumeInt(0, 6);
        int greaterCount = data.consumeInt(0, 6);
        int dupCount = data.consumeInt(2, 6);
        int totalAdds = lessCount + dupCount + greaterCount;

        int base = data.consumeInt(-1000, 1000);
        int maxCount = data.consumeInt(totalAdds, totalAdds + 3);

        XYSeries lhs = new XYSeries(key, true, true);
        XYSeries rhs = new XYSeries(key, true, true);

        lhs.setMaximumItemCount(maxCount);
        rhs.setMaximumItemCount(maxCount);

        if (lhs.getMaximumItemCount() != rhs.getMaximumItemCount() || lhs.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:max-count] metamorphic violation: setMaximumItemCount must establish the configured bound consistently input=max=" + maxCount + " lhs=" + lhs.getMaximumItemCount() + " rhs=" + rhs.getMaximumItemCount());
        }
        if (!lhs.getAutoSort() || !rhs.getAutoSort()) {
            throw new RuntimeException("[oracle:auto-sort] metamorphic violation: constructor autoSort=true must be observable through getAutoSort input=autoSort=true lhs=" + lhs.getAutoSort() + " rhs=" + rhs.getAutoSort());
        }

        try {
            for (int i = 0; i < lessCount; i++) {
                double x = base - (lessCount - i);
                double y = data.consumeInt(-1000, 1000);
                lhs.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                rhs.add(Double.valueOf(x), Double.valueOf(y));
            }

            double duplicateX = base;
            for (int i = 0; i < dupCount; i++) {
                double y = data.consumeInt(-1000, 1000);
                lhs.addOrUpdate(Double.valueOf(duplicateX), Double.valueOf(y));
                rhs.add(Double.valueOf(duplicateX), Double.valueOf(y));
            }

            for (int i = 0; i < greaterCount; i++) {
                double x = base + 1 + i;
                double y = data.consumeInt(-1000, 1000);
                lhs.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                rhs.add(Double.valueOf(x), Double.valueOf(y));
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                String n = t.getClass().getName();
                if (n.startsWith("org.jfree.") && n.toLowerCase().contains("exception")) {
                    validation = true;
                }
            }
            if (validation) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            boolean viaAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    viaAddOrUpdate = true;
                    break;
                }
            }
            if (rootCause && viaAddOrUpdate) {
                throw t;
            }
            return;
        }

        /* Contract/oracle: with allowDuplicateXValues=true, addOrUpdate(Number, Number) takes the insertion path
           and should preserve the same observable series state as add(Number, Number) for the same valid inputs.
           A throw-deleting or wrong-insertion patch would make the final series differ in item count/order/content. */
        try {
            if (lhs.getItemCount() != rhs.getItemCount()) {
                throw new RuntimeException("[oracle:item-count] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) must yield same item count for duplicate x when duplicates are allowed input=lessCount=" + lessCount + " dupCount=" + dupCount + " greaterCount=" + greaterCount + " lhs=" + lhs.getItemCount() + " rhs=" + rhs.getItemCount());
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) must produce equal series on the same valid duplicate-containing input input=base=" + base + " lessCount=" + lessCount + " dupCount=" + dupCount + " greaterCount=" + greaterCount + " lhsCount=" + lhs.getItemCount() + " rhsCount=" + rhs.getItemCount());
            }
            if (lhs.hashCode() != rhs.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=base=" + base + " lessCount=" + lessCount + " dupCount=" + dupCount + " greaterCount=" + greaterCount + " lhs=" + lhs.hashCode() + " rhs=" + rhs.hashCode());
            }
            for (int i = 0; i < lhs.getItemCount(); i++) {
                Number lx = lhs.getX(i);
                Number ly = lhs.getY(i);
                Number rx = rhs.getX(i);
                Number ry = rhs.getY(i);
                if ((lx == null ? rx != null : !lx.equals(rx)) || (ly == null ? ry != null : !ly.equals(ry))) {
                    throw new RuntimeException("[oracle:items] metamorphic violation: corresponding items must match after equivalent real-library operations input=index=" + i + " lhs=(" + lx + "," + ly + ") rhs=(" + rx + "," + ry + ")");
                }
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                String n = t.getClass().getName();
                if (n.startsWith("org.jfree.") && n.toLowerCase().contains("exception")) {
                    validation = true;
                }
            }
            if (validation) {
                return;
            }
            throw t;
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
<details open><summary>▸ Output (~10,660 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            XYSeries series = new XYSeries("Series", true, true);
            XYSeries mirror = new XYSeries("Series", true, true);

            if (!series.getAutoSort() || !mirror.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor-established autoSort not reported input=anchor lhs="
                        + series.getAutoSort() + " rhs=" + mirror.getAutoSort());
            }

            series.addOrUpdate(1.0, 1.0);
            mirror.addOrUpdate(1.0, 1.0);
            if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:eq-mirror] metamorphic violation: equivalent mutation sequence should yield equal series after first add input=anchor lhsCount="
                        + series.getItemCount() + " rhsCount=" + mirror.getItemCount());
            }

            series.addOrUpdate(1.0, 2.0);
            mirror.addOrUpdate(1.0, 2.0);

            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x via addOrUpdate on auto-sorted duplicate-allowing series must add a second item input=anchor lhs="
                        + series.getItemCount() + " rhs=2");
            }
            if (!sameNumber(series.getY(0), 1.0) || !sameNumber(series.getY(1), 2.0)) {
                throw new RuntimeException("[oracle:anchor-y] metamorphic violation: failing test contract requires y-values [1.0, 2.0] after two duplicate-x updates input=anchor lhs0="
                        + series.getY(0) + " lhs1=" + series.getY(1) + " rhs0=1.0 rhs1=2.0");
            }

            assertSorted(series, "anchor");
            if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:eq-mirror] metamorphic violation: equivalent mutation sequence should yield equal series after duplicate addOrUpdate input=anchor lhsCount="
                        + series.getItemCount() + " rhsCount=" + mirror.getItemCount());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        double targetX = bounded(data.consumeInt(-1000, 1000));
        double firstY = bounded(data.consumeInt(-1000, 1000));
        double secondY = bounded(data.consumeInt(-1000, 1000));
        int surrounding = data.consumeInt(0, 6);

        try {
            XYSeries byNumber = new XYSeries(key, true, true);
            XYSeries byPrimitive = new XYSeries(key, true, true);

            int maximum = surrounding + 4 + data.consumeInt(0, 4);
            byNumber.setMaximumItemCount(maximum);
            byPrimitive.setMaximumItemCount(maximum);

            if (!byNumber.getAutoSort() || !byPrimitive.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor-established autoSort not reported input=explore lhs="
                        + byNumber.getAutoSort() + " rhs=" + byPrimitive.getAutoSort());
            }

            byNumber.addOrUpdate(Double.valueOf(targetX), Double.valueOf(firstY));
            byPrimitive.addOrUpdate(targetX, firstY);

            if (!byNumber.equals(byPrimitive) || byNumber.hashCode() != byPrimitive.hashCode()) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent numeric inputs input=seed x="
                        + targetX + " y=" + firstY + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byPrimitive.getItemCount());
            }

            for (int i = 0; i < surrounding; i++) {
                double x = uniqueAround(targetX, i, data.consumeInt(-20, 20));
                double y = bounded(data.consumeInt(-1000, 1000));
                boolean notify = data.consumeBoolean();

                byNumber.add(x, Double.valueOf(y), notify);
                byPrimitive.add(Double.valueOf(x), Double.valueOf(y), notify);

                if (!byNumber.equals(byPrimitive) || byNumber.hashCode() != byPrimitive.hashCode()) {
                    throw new RuntimeException("[oracle:add-overloads] metamorphic violation: equivalent add overloads must produce equal observable series state input=i="
                            + i + " x=" + x + " y=" + y + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byPrimitive.getItemCount());
                }
            }

            int before = byNumber.getItemCount();
            int beforeMirror = byPrimitive.getItemCount();

            byNumber.addOrUpdate(Double.valueOf(targetX), Double.valueOf(secondY));
            byPrimitive.addOrUpdate(targetX, secondY);

            /* Contract asserted: in a series constructed with autoSort=true and allowDuplicateXValues=true,
               a duplicate x passed to addOrUpdate is valid and should be added as a new item, not dropped or mis-inserted.
               A throw-deleting or guard-skipping patch could avoid the crash by silently not adding the item; count/order/equality checks catch that. */
            if (byNumber.getItemCount() != before + 1) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x should increase item count by one when duplicates are allowed input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " lhs=" + byNumber.getItemCount() + " rhs=" + (before + 1));
            }
            if (byPrimitive.getItemCount() != beforeMirror + 1) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x should increase item count by one for sibling overload too input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " lhs=" + byPrimitive.getItemCount() + " rhs=" + (beforeMirror + 1));
            }

            assertSorted(byNumber, "explore-number");
            assertSorted(byPrimitive, "explore-primitive");

            if (!byNumber.equals(byPrimitive) || byNumber.hashCode() != byPrimitive.hashCode()) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: sibling addOrUpdate overloads must agree after duplicate insertion input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byPrimitive.getItemCount());
            }

            if (!containsYForX(byNumber, targetX, firstY) || !containsYForX(byNumber, targetX, secondY)) {
                throw new RuntimeException("[oracle:dup-visible] metamorphic violation: both old and new y-values for the duplicate x must remain observable after insertion input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " count=" + byNumber.getItemCount());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean containsYForX(XYSeries s, double x, double y) {
        for (int i = 0; i < s.getItemCount(); i++) {
            Number xi = s.getX(i);
            Number yi = s.getY(i);
            if (xi != null && yi != null
                    && Double.compare(xi.doubleValue(), x) == 0
                    && Double.compare(yi.doubleValue(), y) == 0) {
                return true;
            }
        }
        return false;
    }

    private static void assertSorted(XYSeries s, String tag) {
        for (int i = 1; i < s.getItemCount(); i++) {
            Number prev = s.getX(i - 1);
            Number curr = s.getX(i);
            if (prev == null || curr == null) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series exposed null x input="
                        + tag + " idx=" + i + " prev=" + prev + " curr=" + curr);
            }
            if (prev.doubleValue() > curr.doubleValue()) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing by x after addOrUpdate input="
                        + tag + " idx=" + i + " prev=" + prev + " curr=" + curr);
            }
        }
    }

    private static boolean sameNumber(Number n, double expected) {
        return n != null && Double.compare(n.doubleValue(), expected) == 0;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException")
                || name.contains("Validation")
                || name.contains("Invalid");
    }

    private static double uniqueAround(double targetX, int index, int fuzz) {
        int delta = fuzz;
        if (delta == 0) {
            delta = index + 1;
        }
        if (delta == -(index + 1)) {
            delta = index + 2;
        }
        return targetX + delta + ((index % 2 == 0) ? 0.25 : -0.25);
    }

    private static double bounded(int v) {
        if (v > 1000000) {
            return 1000000.0;
        }
        if (v < -1000000) {
            return -1000000.0;
        }
        return (double) v;
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
<details open><summary>▸ Output (~8,890 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int dupX = data.consumeInt(-1000, 1000);
        int firstY = data.consumeInt(-1000, 1000);
        int secondY = data.consumeInt(-1000, 1000);
        int prefixCount = data.consumeInt(0, 6);
        int suffixCount = data.consumeInt(0, 6);
        int maxCount = data.consumeInt(prefixCount + suffixCount + 2, prefixCount + suffixCount + 8);

        exploreDuplicateProperty(dupX, firstY, secondY, prefixCount, suffixCount, maxCount, data);
    }

    private static void runAnchor() {
        XYSeries viaAddOrUpdate = new XYSeries("Series", true, true);
        XYSeries viaAdd = new XYSeries("Series", true, true);
        viaAddOrUpdate.setMaximumItemCount(10);
        viaAdd.setMaximumItemCount(10);

        try {
            viaAddOrUpdate.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            viaAddOrUpdate.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            viaAdd.add(Double.valueOf(1.0), Double.valueOf(1.0));
            viaAdd.add(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            return;
        }

        if (!viaAddOrUpdate.getAutoSort()) {
            throw new RuntimeException("[oracle:autoSort-anchor] metamorphic violation: constructor-established autoSort must be observable via getAutoSort input=anchor lhs="
                    + viaAddOrUpdate.getAutoSort() + " rhs=true");
        }

        /* Contract asserted:
           - constructor stores autoSort/allowDuplicateXValues state;
           - duplicate x-values are allowed for this series;
           - addOrUpdate should retain both items here, matching the real add() behavior for equivalent valid inputs.
           A throw-deleting or wrong-bookkeeping patch can avoid the crash yet still lose an item or order it wrongly. */
        checkEquivalentSeries("anchor", viaAddOrUpdate, viaAdd);

        if (viaAddOrUpdate.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on duplicate-allowed series must retain both items input=anchor lhs="
                    + viaAddOrUpdate.getItemCount() + " rhs=2");
        }
        Number y0 = viaAddOrUpdate.getY(0);
        Number y1 = viaAddOrUpdate.getY(1);
        if (!numEquals(y0, 1.0) || !numEquals(y1, 2.0)) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: failing test contract must hold after duplicate addOrUpdate input=anchor lhs=["
                    + y0 + "," + y1 + "] rhs=[1.0,2.0]");
        }
    }

    private static void exploreDuplicateProperty(int dupX, int firstY, int secondY,
                                                 int prefixCount, int suffixCount, int maxCount,
                                                 FuzzedDataProvider data) {
        XYSeries viaAddOrUpdate = new XYSeries("FuzzKey", true, true);
        XYSeries viaAdd = new XYSeries("FuzzKey", true, true);

        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:autoSort-explore] metamorphic violation: constructor-established autoSort must be observable via getAutoSort input=dupX="
                    + dupX + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=true");
        }

        try {
            for (int i = 0; i < prefixCount; i++) {
                double x = dupX - (prefixCount - i);
                double y = data.consumeInt(-1000, 1000);
                viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                viaAdd.add(Double.valueOf(x), Double.valueOf(y));
            }

            viaAddOrUpdate.addOrUpdate(Double.valueOf(dupX), Double.valueOf(firstY));
            viaAddOrUpdate.addOrUpdate(Double.valueOf(dupX), Double.valueOf(secondY));

            viaAdd.add(Double.valueOf(dupX), Double.valueOf(firstY));
            viaAdd.add(Double.valueOf(dupX), Double.valueOf(secondY));

            for (int i = 0; i < suffixCount; i++) {
                double x = dupX + i + 1;
                double y = data.consumeInt(-1000, 1000);
                viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                viaAdd.add(Double.valueOf(x), Double.valueOf(y));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract asserted:
           For a series constructed with autoSort=true and allowDuplicateXValues=true, two valid duplicate x insertions
           through addOrUpdate(Number, Number) should produce the same observable series as two equivalent add(Number, Number)
           calls, because no update path is taken when duplicates are allowed. This also checks equals/hashCode agreement on the
           shared state written by the constructor, setMaximumItemCount(), and the data mutations. */
        checkEquivalentSeries(
                "dupX=" + dupX + ",y1=" + firstY + ",y2=" + secondY + ",prefix=" + prefixCount + ",suffix=" + suffixCount + ",max=" + maxCount,
                viaAddOrUpdate,
                viaAdd
        );
    }

    private static void checkEquivalentSeries(String inputDesc, XYSeries lhs, XYSeries rhs) {
        if (!lhs.equals(rhs) || !rhs.equals(lhs)) {
            throw new RuntimeException("[oracle:eq-series] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) must yield equal series for duplicate-allowed inputs input="
                    + inputDesc + " lhsCount=" + lhs.getItemCount() + " rhsCount=" + rhs.getItemCount());
        }
        if (lhs.hashCode() != rhs.hashCode()) {
            throw new RuntimeException("[oracle:hash-series] metamorphic violation: equal series must have equal hashCode input="
                    + inputDesc + " lhs=" + lhs.hashCode() + " rhs=" + rhs.hashCode());
        }
        if (lhs.getItemCount() != rhs.getItemCount()) {
            throw new RuntimeException("[oracle:count-series] metamorphic violation: equivalent construction paths must retain the same item count input="
                    + inputDesc + " lhs=" + lhs.getItemCount() + " rhs=" + rhs.getItemCount());
        }
        for (int i = 0; i < lhs.getItemCount(); i++) {
            Number lx = lhs.getX(i);
            Number ly = lhs.getY(i);
            Number rx = rhs.getX(i);
            Number ry = rhs.getY(i);
            if (!numEquals(lx, rx) || !numEquals(ly, ry)) {
                throw new RuntimeException("[oracle:item-series] metamorphic violation: equivalent construction paths must expose the same ordered items input="
                        + inputDesc + " index=" + i + " lhs=(" + lx + "," + ly + ") rhs=(" + rx + "," + ry + ")");
            }
        }
    }

    private static boolean numEquals(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean numEquals(Number a, double b) {
        return a != null && Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.jfree") && name.endsWith("Exception") && !isRootCause(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~7,855 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Oracle asserted below:
           For autoSort=true and allowDuplicateXValues=true, addOrUpdate(Number, Number)
           must add a new item for a duplicate x-value, not update or misplace it.
           Therefore, on equivalent valid inputs, a series built with addOrUpdate(...)
           must equal a series built with add(...). A patch that merely suppresses the
           crash or appends incorrectly will violate this observable post-condition. */

        // ANCHOR: exact trigger from the failing test.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: exact trigger must retain two duplicate x items input=1.0,1.0->1.0,2.0 lhs=" + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-y] metamorphic violation: exact trigger must preserve both y values in order input=1.0,1.0->1.0,2.0 lhs=(" + y0 + "," + y1 + ") rhs=(1.0,2.0)");
            }
        } catch (RuntimeException t) {
            boolean throughAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    throughAddOrUpdate = true;
                    break;
                }
            }
            if ((t instanceof IndexOutOfBoundsException) && throughAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: valid-by-construction duplicate-x cases with surrounding content.
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        double targetX = data.consumeInt(-1000, 1000);
        int leftCount = data.consumeInt(0, 4);
        int rightCount = data.consumeInt(0, 4);
        int maxCount = leftCount + rightCount + 2 + data.consumeInt(0, 3);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        // Constructor/state coupling checks against public readers.
        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor autoSort=true must be reported by getAutoSort() input=" + key + " lhs=" + viaAddOrUpdate.getAutoSort() + "," + viaAdd.getAutoSort() + " rhs=true,true");
        }
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: identically constructed empty series must be equal input=" + key + " lhs=false rhs=true");
        }

        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        try {
            // Surrounding smaller x-values.
            for (int i = 0; i < leftCount; i++) {
                double x = targetX - (i + 1);
                double y = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                    viaAdd.add(Double.valueOf(x), Double.valueOf(y));
                } else {
                    viaAddOrUpdate.add(x, y);
                    viaAdd.add(x, y);
                }
            }

            // Surrounding larger x-values.
            for (int i = 0; i < rightCount; i++) {
                double x = targetX + (i + 1);
                double y = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                    viaAdd.add(Double.valueOf(x), Double.valueOf(y));
                } else {
                    viaAddOrUpdate.add(x, y);
                    viaAdd.add(x, y);
                }
            }

            // Existing target x item so the next addOrUpdate hits the duplicate-x path.
            double firstY = data.consumeInt(-1000, 1000);
            double secondY = data.consumeInt(-1000, 1000);

            if (data.consumeBoolean()) {
                viaAddOrUpdate.addOrUpdate(Double.valueOf(targetX), Double.valueOf(firstY));
            } else {
                viaAddOrUpdate.addOrUpdate(targetX, firstY);
            }

            if (data.consumeBoolean()) {
                viaAdd.add(Double.valueOf(targetX), Double.valueOf(firstY));
            } else {
                viaAdd.add(targetX, firstY);
            }

            // Trigger the patched duplicate insertion path through the real public API.
            if (data.consumeBoolean()) {
                viaAddOrUpdate.addOrUpdate(Double.valueOf(targetX), Double.valueOf(secondY));
            } else {
                viaAddOrUpdate.addOrUpdate(targetX, secondY);
            }

            if (data.consumeBoolean()) {
                viaAdd.add(Double.valueOf(targetX), Double.valueOf(secondY));
            } else {
                viaAdd.add(targetX, secondY);
            }
        } catch (RuntimeException t) {
            boolean throughAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    throughAddOrUpdate = true;
                    break;
                }
            }
            if ((t instanceof IndexOutOfBoundsException) && throughAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // If the real calls above succeeded, the two series must agree exactly.
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException(
                "[oracle:eq-add-vs-addOrUpdate] metamorphic violation: with autoSort=true and duplicate x-values allowed, addOrUpdate(x,y) on an existing x must produce the same series state as add(x,y) input=targetX="
                    + targetX + ", leftCount=" + leftCount + ", rightCount=" + rightCount
                    + ", lhsCount=" + viaAddOrUpdate.getItemCount() + ", rhsCount=" + viaAdd.getItemCount()
                    + ", lhsItems=" + viaAddOrUpdate.getItems() + ", rhsItems=" + viaAdd.getItems());
        }

        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException(
                "[oracle:eq-hash] metamorphic violation: equal series must have equal hashCode input=targetX="
                    + targetX + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-stable] metamorphic violation: add/addOrUpdate must not mutate constructor-defined autoSort flag input=targetX=" + targetX + " lhs=" + viaAddOrUpdate.getAutoSort() + "," + viaAdd.getAutoSort() + " rhs=true,true");
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
<details open><summary>▸ Output (~8,587 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            XYSeries anchorMirror = new XYSeries("Series", true, true);

            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchorMirror.add(Double.valueOf(1.0), Double.valueOf(1.0));

            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
            anchorMirror.add(Double.valueOf(1.0), Double.valueOf(2.0));

            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate with allowDuplicateXValues=true must retain both items input=x=1.0 lhs=" + anchor.getItemCount() + " rhs=2");
            }
            Number ay0 = anchor.getY(0);
            Number ay1 = anchor.getY(1);
            if (ay0 == null || ay1 == null || ay0.doubleValue() != 1.0 || ay1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: failing test contract requires y-values [1.0, 2.0] after duplicate addOrUpdate input=" + ay0 + "," + ay1 + " lhs=" + ay0 + "," + ay1 + " rhs=1.0,2.0");
            }
            if (!anchor.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=true lhs=false rhs=true");
            }
            /* Contract/oracle: with allowDuplicateXValues=true, addOrUpdate(Number, Number) should add a new item
               when the x-value already exists rather than overwrite it; for the same sequence of logical additions,
               a series built via addOrUpdate must therefore be observationally equivalent to one built via add(...). */
            if (!anchor.equals(anchorMirror)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate duplicate path must match add duplicate path input=x=1.0 lhs=" + anchor.getItems() + " rhs=" + anchorMirror.getItems());
            }
            if (anchor.hashCode() != anchorMirror.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal XYSeries must have equal hashCode input=x=1.0 lhs=" + anchor.hashCode() + " rhs=" + anchorMirror.hashCode());
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException;
            if (cleanRejection) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())
                            && "addOrUpdate".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int dupX = data.consumeInt(-1000, 1000);
        int firstY = data.consumeInt(-1000, 1000);
        int secondY = data.consumeInt(-1000, 1000);
        int leftCount = data.consumeInt(0, 4);
        int rightCount = data.consumeInt(0, 4);
        int totalFinal = leftCount + 1 + rightCount + 1;
        int maxCount = data.consumeInt(totalFinal, totalFinal + 4);

        XYSeries series = new XYSeries("Series", true, true);
        XYSeries mirror = new XYSeries("Series", true, true);

        series.setMaximumItemCount(maxCount);
        mirror.setMaximumItemCount(maxCount);

        if (series.getMaximumItemCount() != maxCount || mirror.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount] metamorphic violation: setMaximumItemCount must be reported by getMaximumItemCount input=" + maxCount + " lhs=" + series.getMaximumItemCount() + "," + mirror.getMaximumItemCount() + " rhs=" + maxCount);
        }
        if (!series.getAutoSort() || !mirror.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=true lhs=" + series.getAutoSort() + "," + mirror.getAutoSort() + " rhs=true");
        }

        try {
            for (int i = leftCount; i >= 1; i--) {
                Integer x = Integer.valueOf(dupX - i);
                Integer y = Integer.valueOf(firstY - i);
                series.add(x, y);
                mirror.add(x, y);
            }

            series.add(Integer.valueOf(dupX), Integer.valueOf(firstY));
            mirror.add(Integer.valueOf(dupX), Integer.valueOf(firstY));

            for (int i = 1; i <= rightCount; i++) {
                Integer x = Integer.valueOf(dupX + i);
                Integer y = Integer.valueOf(firstY + i);
                series.add(x, y);
                mirror.add(x, y);
            }

            int before = series.getItemCount();
            series.addOrUpdate(Integer.valueOf(dupX), Integer.valueOf(secondY));
            mirror.add(Integer.valueOf(dupX), Integer.valueOf(secondY));

            /* Contract/oracle: because duplicate x-values are allowed and autoSort is enabled, a duplicate addOrUpdate
               must insert a new item into the sorted data rather than overwrite, skip, or append unsortedly.
               Comparing against the real sibling API add(...) catches throw-deleting or wrong-bookkeeping patches. */
            if (series.getItemCount() != before + 1) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate must increase item count by one when duplicates are allowed input=dupX=" + dupX + " lhs=" + series.getItemCount() + " rhs=" + (before + 1));
            }
            if (!series.equals(mirror)) {
                throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate duplicate path must match add duplicate path input=dupX=" + dupX + ",firstY=" + firstY + ",secondY=" + secondY + " lhs=" + series.getItems() + " rhs=" + mirror.getItems());
            }
            if (series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal XYSeries must have equal hashCode input=dupX=" + dupX + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
            }

            for (int i = 0; i < series.getItemCount(); i++) {
                Number sx = series.getX(i);
                Number sy = series.getY(i);
                Number mx = mirror.getX(i);
                Number my = mirror.getY(i);
                if ((sx == null) != (mx == null) || (sy == null) != (my == null)
                        || (sx != null && sx.doubleValue() != mx.doubleValue())
                        || (sy != null && sy.doubleValue() != my.doubleValue())) {
                    throw new RuntimeException("[oracle:getters] metamorphic violation: getter family must report the same ordered contents as sibling add-based construction input=dupX=" + dupX + " lhs=(" + sx + "," + sy + ") rhs=(" + mx + "," + my + ")");
                }
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException;
            if (cleanRejection) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())
                            && "addOrUpdate".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,166 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            if (shouldPropagate(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int leading = data.consumeInt(0, 5);
        int trailing = data.consumeInt(0, 5);
        int xBase = data.consumeInt(-1000, 1000);
        int y1i = data.consumeInt(-1000, 1000);
        int y2i = data.consumeInt(-1000, 1000);
        int maxCount = leading + trailing + 3 + data.consumeInt(0, 3);

        try {
            runScenario(key, leading, trailing, xBase, y1i, y2i, maxCount);
        } catch (Throwable t) {
            if (shouldPropagate(t, true)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);

        /* Contract asserted:
         * - In a series constructed with autoSort=true and allowDuplicateXValues=true,
         *   addOrUpdate(x, y) on an existing x must add a second item rather than overwrite.
         * - The collection remains sorted.
         * A patch that only suppresses the throw or skips the insertion would violate count/Y checks.
         */
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x should be retained input=x=1.0,y=[1.0,2.0] lhs=" + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression test outcome input=x=1.0,y=[1.0,2.0] lhs=[" + y0 + "," + y1 + "] rhs=[1.0,2.0]");
        }
        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort must be reported true input=anchor lhs=false rhs=true");
        }

        XYSeries emptyTwin = new XYSeries("Series", true, true);
        if (emptyTwin.getAutoSort() != true) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: getAutoSort must report constructor state input=anchor lhs=false rhs=true");
        }
    }

    private static void runScenario(String key, int leading, int trailing, int xBase, int y1i, int y2i, int maxCount) {
        double x = (double) xBase;
        double y1 = (double) y1i;
        double y2 = (double) y2i;

        XYSeries series = new XYSeries(key, true, true);
        XYSeries sameState = new XYSeries(key, true, true);

        /* Shared-state agreement check:
         * equals()/hashCode()/getAutoSort() observe the same constructor-established fields.
         * Two freshly constructed series with the same key/flags/data must agree.
         */
        if (!series.getAutoSort() || !sameState.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-state] metamorphic violation: getAutoSort must reflect constructor-established state input=key=" + key + " lhs=" + series.getAutoSort() + " rhs=true");
        }
        if (!series.equals(sameState) || series.hashCode() != sameState.hashCode()) {
            throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: identically constructed empty series must agree input=key=" + key + " lhs.equals=" + series.equals(sameState) + " lhs.hash=" + series.hashCode() + " rhs.hash=" + sameState.hashCode());
        }

        if (maxCount < leading + trailing + 2) {
            maxCount = leading + trailing + 2;
        }
        series.setMaximumItemCount(maxCount);
        sameState.setMaximumItemCount(maxCount);
        if (!series.equals(sameState) || series.hashCode() != sameState.hashCode()) {
            throw new RuntimeException("[oracle:maxcount-state] metamorphic violation: equal maximumItemCount updates must keep equal series equal input=max=" + maxCount + " lhs.equals=" + series.equals(sameState) + " lhs.hash=" + series.hashCode() + " rhs.hash=" + sameState.hashCode());
        }

        XYSeries peer = new XYSeries(key, true, true);
        peer.setMaximumItemCount(maxCount);

        for (int i = 0; i < leading; i++) {
            double xi = x - (leading - i);
            double yi = y1 - (leading - i);
            series.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            sameState.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            peer.add(xi, yi);
            if (!series.equals(sameState)) {
                throw new RuntimeException("[oracle:state-prefix] metamorphic violation: same mutations via same API must keep equal state input=leading@" + i + " lhs.count=" + series.getItemCount() + " rhs.count=" + sameState.getItemCount());
            }
        }

        series.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
        sameState.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
        peer.add(x, y1);

        for (int i = 0; i < trailing; i++) {
            double xi = x + (i + 1);
            double yi = y2 + (i + 1);
            series.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            sameState.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            peer.add(xi, yi);
            if (!series.equals(sameState)) {
                throw new RuntimeException("[oracle:state-suffix] metamorphic violation: same mutations via same API must keep equal state input=trailing@" + i + " lhs.count=" + series.getItemCount() + " rhs.count=" + sameState.getItemCount());
            }
        }

        int before = series.getItemCount();

        peer.add(x, y2);
        series.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));

        /* Contract asserted:
         * - allowDuplicateXValues=true means adding an existing x via addOrUpdate must retain both items.
         * - autoSort=true means x-values remain nondecreasing.
         * - Building the same content through real sibling API add(double,double) must yield an equal series.
         */
        if (series.getItemCount() != before + 1) {
            throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x via addOrUpdate should increase size by one input=x=" + x + ",before=" + before + " lhs=" + series.getItemCount() + " rhs=" + (before + 1));
        }
        if (!isNonDecreasing(series)) {
            throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain sorted after duplicate insertion input=x=" + x + " lhs=unsorted rhs=nondecreasing");
        }
        int dupCount = 0;
        boolean sawY1 = false;
        boolean sawY2 = false;
        for (int i = 0; i < series.getItemCount(); i++) {
            Number xi = series.getX(i);
            if (xi != null && Double.compare(xi.doubleValue(), x) == 0) {
                dupCount++;
                Number yi = series.getY(i);
                if (yi != null && Double.compare(yi.doubleValue(), y1) == 0) {
                    sawY1 = true;
                }
                if (yi != null && Double.compare(yi.doubleValue(), y2) == 0) {
                    sawY2 = true;
                }
            }
        }
        if (dupCount != 2 || !sawY1 || !sawY2) {
            throw new RuntimeException("[oracle:dup-values] metamorphic violation: both duplicate points must be retained input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhs=count=" + dupCount + ",sawY1=" + sawY1 + ",sawY2=" + sawY2 + " rhs=count=2,sawY1=true,sawY2=true");
        }
        if (!series.equals(peer)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: addOrUpdate on a duplicate in a dup-allowing series must match building the same content with add input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhs.count=" + series.getItemCount() + " rhs.count=" + peer.getItemCount());
        }
    }

    private static boolean isNonDecreasing(XYSeries s) {
        for (int i = 1; i < s.getItemCount(); i++) {
            Number prev = s.getX(i - 1);
            Number cur = s.getX(i);
            if (prev == null || cur == null) {
                return false;
            }
            if (Double.compare(prev.doubleValue(), cur.doubleValue()) > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return validByConstruction && isRootCause(t);
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.endsWith("SeriesException") || name.contains("Invalid");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
<details open><summary>▸ Output (~9,044 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }

        try {
            runExplore(data);
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autoSort] metamorphic violation: constructor-established autoSort not reported input=Series lhs=false rhs=true");
        }

        XYSeries same = new XYSeries("Series", true, true);
        if (!series.equals(same) || !same.equals(series)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: equal empty series built with same constructor args must compare equal input=Series lhs=" + series.equals(same) + " rhs=" + same.equals(series));
        }

        series.addOrUpdate(1.0, 1.0);
        series.addOrUpdate(1.0, 2.0);

        same.addOrUpdate(1.0, 1.0);
        same.addOrUpdate(1.0, 2.0);

        checkDuplicateInsertPostConditions(series, 1.0, 1.0, 2.0, "anchor");
        if (!series.equals(same) || series.hashCode() != same.hashCode()) {
            throw new RuntimeException("[oracle:equals-hash] metamorphic violation: series with same constructor state and same updates must agree on equals/hashCode input=anchor lhsEq=" + series.equals(same) + " lhsHash=" + series.hashCode() + " rhsHash=" + same.hashCode());
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);
        XYSeries mirror = new XYSeries(key, true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autoSort] metamorphic violation: constructor-established autoSort not reported input=" + key + " lhs=false rhs=true");
        }
        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: equal empty series built with same constructor args must compare equal input=" + key + " lhs=" + series.equals(mirror) + " rhs=" + mirror.equals(series));
        }

        int maxCount = data.consumeInt(1, 8);
        series.setMaximumItemCount(maxCount);
        mirror.setMaximumItemCount(maxCount);

        int uniquePrefix = data.consumeInt(0, 6);
        double[] xs = new double[uniquePrefix + 1];

        for (int i = 0; i < uniquePrefix; i++) {
            xs[i] = i * 10.0 + data.consumeInt(0, 3);
            double y = data.consumeInt(-1000, 1000);
            series.addOrUpdate(xs[i], y);
            mirror.addOrUpdate(Double.valueOf(xs[i]), Double.valueOf(y));
        }

        double dupX;
        if (uniquePrefix == 0 || data.consumeBoolean()) {
            dupX = data.consumeInt(-50, 50);
            xs[uniquePrefix] = dupX;
            double firstY = data.consumeInt(-1000, 1000);
            series.addOrUpdate(dupX, firstY);
            mirror.addOrUpdate(Double.valueOf(dupX), Double.valueOf(firstY));
        } else {
            int pick = data.consumeInt(0, uniquePrefix - 1);
            dupX = xs[pick];
        }

        double y1 = data.consumeInt(-1000, 1000);
        double y2 = data.consumeInt(-1000, 1000);

        int before = series.getItemCount();
        series.addOrUpdate(dupX, y1);
        mirror.addOrUpdate(Double.valueOf(dupX), Double.valueOf(y1));

        int mid = series.getItemCount();
        series.addOrUpdate(dupX, y2);
        mirror.addOrUpdate(Double.valueOf(dupX), Double.valueOf(y2));

        if (mid < before || series.getItemCount() < mid) {
            throw new RuntimeException("[oracle:itemCount-monotonic] metamorphic violation: addOrUpdate with duplicates allowed must not decrease item count before maximum-item truncation input=x=" + dupX + " before=" + before + " mid=" + mid + " after=" + series.getItemCount());
        }

        checkSorted(series, "explore-sorted", dupX, y1, y2);

        if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent addOrUpdate overload sequences must yield equal series input=x=" + dupX + ",y1=" + y1 + ",y2=" + y2 + " lhsEq=" + series.equals(mirror) + " lhsHash=" + series.hashCode() + " rhsHash=" + mirror.hashCode());
        }

        if (series.getItemCount() == mirror.getItemCount()) {
            for (int i = 0; i < series.getItemCount(); i++) {
                Number sx = series.getX(i);
                Number sy = series.getY(i);
                Number mx = mirror.getX(i);
                Number my = mirror.getY(i);
                if (!numEq(sx, mx) || !numEq(sy, my)) {
                    throw new RuntimeException("[oracle:overload-items] metamorphic violation: equivalent addOrUpdate overload sequences must yield identical item stream input=index=" + i + ",x=" + dupX + " lhs=(" + sx + "," + sy + ") rhs=(" + mx + "," + my + ")");
                }
            }
        }
    }

    private static void checkDuplicateInsertPostConditions(XYSeries series, double x, double y0, double y1, String tag) {
        checkSorted(series, tag + "-sorted", x, y0, y1);

        Number firstY = series.getY(0);
        Number secondY = series.getY(1);
        int count = series.getItemCount();

        /* Contract asserted: when duplicate x-values are allowed, addOrUpdate() adds a new item
           rather than overwriting the existing one; with autoSort=true the collection remains
           sorted. A throw-deleting or append/skipping patch can avoid the crash but still break
           the documented observable result from the failing test: two items retained, ordered by x,
           with y-values 1.0 then 2.0 for the exact duplicate-x anchor sequence. */
        if (count != 2 || !numEq(firstY, Double.valueOf(y0)) || !numEq(secondY, Double.valueOf(y1))) {
            throw new RuntimeException("[oracle:dup-anchor] metamorphic violation: duplicate-x addOrUpdate must retain both y-values in order for allowed duplicates input=x=" + x + ",y0=" + y0 + ",y1=" + y1 + " lhsCount=" + count + " lhsY0=" + firstY + " lhsY1=" + secondY);
        }
    }

    private static void checkSorted(XYSeries series, String oracleId, double x, double y1, double y2) {
        int count = series.getItemCount();
        for (int i = 1; i < count; i++) {
            Number prev = series.getX(i - 1);
            Number cur = series.getX(i);
            if (prev != null && cur != null && prev.doubleValue() > cur.doubleValue()) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: auto-sorted series must remain sorted after addOrUpdate input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhsPrev=" + prev + " rhsCur=" + cur);
            }
        }
    }

    private static boolean numEq(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean shouldPropagate(RuntimeException t) {
        if (t == null) {
            return false;
        }
        String msg = t.getMessage();
        if (msg != null && msg.startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return isRootCause(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("Validation") || name.contains("Invalid") || name.endsWith("SeriesException"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,579 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.function.Predicate<Throwable> isCleanRejection = t -> {
            Throwable c = t;
            while (c != null) {
                if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                    return true;
                }
                c = c.getCause();
            }
            return false;
        };
        java.util.function.Predicate<Throwable> isRootCause = t -> {
            if (!(t instanceof IndexOutOfBoundsException)) {
                return false;
            }
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    return true;
                }
            }
            return false;
        };

        // ANCHOR: exact failing test input.
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            // Post-condition from the failing test: with autoSort=true and duplicate x-values allowed,
            // addOrUpdate() must retain both items and preserve their observable y-values/order.
            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: exact regression trigger must retain two items input=[(1.0,1.0),(1.0,2.0)] lhs="
                        + anchor.getItemCount() + " rhs=2");
            }
            Number y0 = anchor.getY(0);
            Number y1 = anchor.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: exact regression trigger must expose y-values [1.0,2.0] input=[(1.0,1.0),(1.0,2.0)] lhs=["
                        + y0 + "," + y1 + "] rhs=[1.0,2.0]");
            }

            XYSeries anchorExpected = new XYSeries("Series", true, true);
            anchorExpected.add(1.0, 1.0);
            anchorExpected.add(1.0, 2.0);
            if (!anchor.equals(anchorExpected)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) for duplicate-allowed series on equivalent inputs input=[(1.0,1.0),(1.0,2.0)] lhs="
                        + anchor.getItems() + " rhs=" + anchorExpected.getItems());
            }
            if (anchor.hashCode() != anchorExpected.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hash codes input=[(1.0,1.0),(1.0,2.0)] lhs="
                        + anchor.hashCode() + " rhs=" + anchorExpected.hashCode());
            }
            if (!anchor.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort flag must be observable through getAutoSort() input=true lhs="
                        + anchor.getAutoSort() + " rhs=true");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection.test(t)) {
                return;
            }
            if (isRootCause.test(t)) {
                throw t;
            }
            return;
        }

        String key = data.consumeAsciiString(16);
        if (key.length() == 0) {
            key = "K";
        }

        XYSeries actual = new XYSeries(key, true, true);
        XYSeries expected = new XYSeries(key, true, true);

        int maximum = data.consumeInt(1, 8);
        actual.setMaximumItemCount(maximum);
        expected.setMaximumItemCount(maximum);

        if (actual.getMaximumItemCount() != maximum || expected.getMaximumItemCount() != maximum) {
            throw new RuntimeException("[oracle:maxcount] metamorphic violation: setMaximumItemCount must be reported by getMaximumItemCount input="
                    + maximum + " lhs=" + actual.getMaximumItemCount() + " rhs=" + expected.getMaximumItemCount());
        }
        if (!actual.getAutoSort() || !expected.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-established autoSort flag must remain observable input=true lhs="
                    + actual.getAutoSort() + " rhs=" + expected.getAutoSort());
        }

        int prefixCount = data.consumeInt(0, 3);
        int duplicateCount = data.consumeInt(2, 4);
        int suffixCount = data.consumeInt(0, 3);
        int baseXInt = data.consumeInt(-1000, 1000);

        StringBuilder seq = new StringBuilder();
        seq.append("max=").append(maximum).append(";");

        try {
            for (int i = 0; i < prefixCount; i++) {
                int xi = baseXInt + data.consumeInt(-20, 20);
                if (xi == baseXInt) {
                    xi += (i + 1);
                }
                int yi = data.consumeInt(-1000, 1000);
                Double x = Double.valueOf((double) xi);
                Double y = Double.valueOf((double) yi);
                seq.append("(").append(x).append(",").append(y).append(")");
                actual.addOrUpdate(x, y);
                expected.add(x.doubleValue(), y.doubleValue());
            }

            for (int i = 0; i < duplicateCount; i++) {
                int yi = data.consumeInt(-1000, 1000);
                Double x = Double.valueOf((double) baseXInt);
                Double y = Double.valueOf((double) yi);
                seq.append("(").append(x).append(",").append(y).append(")");
                actual.addOrUpdate(x, y);
                expected.add(x.doubleValue(), y.doubleValue());
            }

            for (int i = 0; i < suffixCount; i++) {
                int xi = baseXInt + data.consumeInt(-20, 20);
                if (xi == baseXInt) {
                    xi -= (i + 1);
                }
                int yi = data.consumeInt(-1000, 1000);
                Double x = Double.valueOf((double) xi);
                Double y = Double.valueOf((double) yi);
                seq.append("(").append(x).append(",").append(y).append(")");
                actual.addOrUpdate(x, y);
                expected.add(x.doubleValue(), y.doubleValue());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection.test(t)) {
                return;
            }
            if (isRootCause.test(t)) {
                throw t;
            }
            return;
        }

        // Contract/oracle: addOrUpdate(x,y) on a series constructed with allowDuplicateXValues=true
        // should add a new item when the x already exists; the add* overload family operates on the
        // same input space, so driving the same sequence through addOrUpdate and add must yield the
        // same observable series state. A throw-deleting or wrong-bookkeeping patch would break this.
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) on equivalent valid inputs for auto-sorted duplicate-allowed series input="
                    + seq + " lhs=" + actual.getItems() + " rhs=" + expected.getItems());
        }

        if (actual.hashCode() != expected.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash] metamorphic violation: equal series must have equal hash codes input="
                    + seq + " lhs=" + actual.hashCode() + " rhs=" + expected.hashCode());
        }

        if (actual.getItemCount() != expected.getItemCount()) {
            throw new RuntimeException("[oracle:itemcount] metamorphic violation: equivalent update/add sequences must expose same item count input="
                    + seq + " lhs=" + actual.getItemCount() + " rhs=" + expected.getItemCount());
        }

        for (int i = 0; i < actual.getItemCount(); i++) {
            Number ax = actual.getX(i);
            Number ex = expected.getX(i);
            Number ay = actual.getY(i);
            Number ey = expected.getY(i);
            double axd = ax == null ? Double.NaN : ax.doubleValue();
            double exd = ex == null ? Double.NaN : ex.doubleValue();
            double ayd = ay == null ? Double.NaN : ay.doubleValue();
            double eyd = ey == null ? Double.NaN : ey.doubleValue();
            if (Double.compare(axd, exd) != 0 || Double.compare(ayd, eyd) != 0) {
                throw new RuntimeException("[oracle:pointwise] metamorphic violation: equivalent valid sequences must expose identical sorted points input="
                        + seq + " index=" + i + " lhs=(" + ax + "," + ay + ") rhs=(" + ex + "," + ey + ")");
            }
            if (i > 0) {
                double prev = actual.getX(i - 1).doubleValue();
                double cur = actual.getX(i).doubleValue();
                if (prev > cur) {
                    throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing by x input="
                            + seq + " index=" + i + " lhsPrev=" + prev + " lhsCur=" + cur);
                }
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
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,544 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            XYSeries series = new XYSeries("Series", true, true);
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor/getAutoSort disagreement input=Series lhs=false rhs=true");
            }

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            XYSeries mirror = new XYSeries("Series", true, true);
            mirror.addOrUpdate(1.0, 1.0);
            mirror.addOrUpdate(1.0, 2.0);

            series.setMaximumItemCount(series.getItemCount());
            mirror.setMaximumItemCount(mirror.getItemCount());

            /* Contract/oracle:
             * The failing test documents the expected observable state after adding the same x twice
             * with autoSort=true and allowDuplicateXValues=true: two retained items, first y then second y.
             * A throw-deleting or silently-wrong patch would violate these reads even if no exception is thrown.
             * Also, equal series must agree under equals()/hashCode(), and getAutoSort() must reflect the constructor.
             */
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate must retain two items input=x=1.0 lhs=" + series.getItemCount() + " rhs=2");
            }
            if (!numEq(series.getY(0), Double.valueOf(1.0))) {
                throw new RuntimeException("[oracle:y0] metamorphic violation: first duplicate must preserve first y input=x=1.0 lhs=" + series.getY(0) + " rhs=1.0");
            }
            if (!numEq(series.getY(1), Double.valueOf(2.0))) {
                throw new RuntimeException("[oracle:y1] metamorphic violation: second duplicate must preserve second y input=x=1.0 lhs=" + series.getY(1) + " rhs=2.0");
            }
            if (!series.equals(mirror)) {
                throw new RuntimeException("[oracle:eq] metamorphic violation: equivalent overload sequences must produce equal series input=x=1.0 lhs=false rhs=true");
            }
            if (series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=x=1.0 lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
            }
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null) {
            key = "K";
        }

        int base = data.consumeInt(-1000, 1000);
        int y1 = data.consumeInt(-1000, 1000);
        int y2 = data.consumeInt(-1000, 1000);
        int prefixCount = data.consumeInt(0, 4);
        int suffixCount = data.consumeInt(0, 4);
        int maxExtra = data.consumeInt(0, 2);

        double x = (double) base;

        try {
            XYSeries a = new XYSeries(key, true, true);
            XYSeries b = new XYSeries(key, true, true);

            if (!a.getAutoSort() || !b.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor/getAutoSort disagreement input=" + key + " lhs=" + a.getAutoSort() + " rhs=true");
            }

            for (int i = prefixCount; i >= 1; i--) {
                double px = x - i;
                double py = y1 - i;
                a.add(px, py);
                b.add(px, py);
            }

            a.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
            b.addOrUpdate(x, (double) y1);

            for (int i = 1; i <= suffixCount; i++) {
                double sx = x + i;
                double sy = y2 + i;
                a.add(sx, sy);
                b.add(sx, sy);
            }

            a.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));
            b.addOrUpdate(x, (double) y2);

            int current = a.getItemCount();
            int max = Math.max(0, current - maxExtra);
            a.setMaximumItemCount(max);
            b.setMaximumItemCount(max);

            if (!a.equals(b)) {
                throw new RuntimeException("[oracle:eq] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent inputs input=key=" + key + ",x=" + x + " lhs=false rhs=true");
            }
            if (a.hashCode() != b.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=key=" + key + ",x=" + x + " lhs=" + a.hashCode() + " rhs=" + b.hashCode());
            }
            if (a.getItemCount() > max) {
                throw new RuntimeException("[oracle:max] metamorphic violation: setMaximumItemCount must cap retained items input=max=" + max + " lhs=" + a.getItemCount() + " rhs<=" + max);
            }

            int dupCount = 0;
            int firstDupIndex = -1;
            for (int i = 0; i < a.getItemCount(); i++) {
                if (numEq(a.getX(i), Double.valueOf(x))) {
                    if (firstDupIndex < 0) {
                        firstDupIndex = i;
                    }
                    dupCount++;
                }
            }

            if (max >= prefixCount + 2 && dupCount < 2) {
                throw new RuntimeException("[oracle:dups] metamorphic violation: duplicate addOrUpdate must retain two x-equal items before max trimming input=x=" + x + ",prefix=" + prefixCount + ",suffix=" + suffixCount + " lhs=" + dupCount + " rhs>=2");
            }
            if (firstDupIndex >= 0 && firstDupIndex + 1 < a.getItemCount() && max >= prefixCount + 2) {
                if (!numEq(a.getX(firstDupIndex + 1), Double.valueOf(x))) {
                    throw new RuntimeException("[oracle:adjacent] metamorphic violation: auto-sorted equal x items must remain adjacent input=x=" + x + " lhs=" + a.getX(firstDupIndex + 1) + " rhs=" + x);
                }
                if (!numEq(a.getY(firstDupIndex), Double.valueOf(y1))) {
                    throw new RuntimeException("[oracle:yfirst] metamorphic violation: first duplicate should keep first y input=x=" + x + " lhs=" + a.getY(firstDupIndex) + " rhs=" + y1);
                }
                if (!numEq(a.getY(firstDupIndex + 1), Double.valueOf(y2))) {
                    throw new RuntimeException("[oracle:ysecond] metamorphic violation: second duplicate should keep second y input=x=" + x + " lhs=" + a.getY(firstDupIndex + 1) + " rhs=" + y2);
                }
            }
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
        }
    }

    private static boolean numEq(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())
                    && "addOrUpdate".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().endsWith("SeriesException");
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,912 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runValidDuplicateScenario(1.0, 1.0, 2.0, true, true, 2);

        double x = boundedDouble(data.consumeInt());
        double y1 = boundedDouble(data.consumeInt());
        double y2 = boundedDouble(data.consumeInt());
        int maxCount = data.consumeInt(2, 8);
        boolean firstPrimitive = data.consumeBoolean();
        boolean secondPrimitive = data.consumeBoolean();

        runValidDuplicateScenario(x, y1, y2, firstPrimitive, secondPrimitive, maxCount);

        int extraPairs = data.consumeInt(0, 6);
        XYSeries series = new XYSeries(nonNullKey(data), true, true);
        XYSeries twin = new XYSeries(series.getKey(), true, true);
        series.setMaximumItemCount(maxCount);
        twin.setMaximumItemCount(maxCount);

        if (!series.getAutoSort() || !twin.getAutoSort()) {
            throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor-established autoSort flag not reported input=" + series.getKey());
        }

        try {
            safeAddOrUpdate(series, x, y1, firstPrimitive);
            safeAddOrUpdate(series, x, y2, secondPrimitive);
            safeAddOrUpdate(twin, x, y1, firstPrimitive);
            safeAddOrUpdate(twin, x, y2, secondPrimitive);

            for (int i = 0; i < extraPairs; i++) {
                double ex = boundedDouble(data.consumeInt());
                double ey = boundedDouble(data.consumeInt());
                boolean primitive = data.consumeBoolean();
                safeAddOrUpdate(series, ex, ey, primitive);
                safeAddOrUpdate(twin, ex, ey, primitive);
            }

            /* Contract asserted: constructor writes autoSort/allowDuplicateXValues/data state,
             * equals() compares that state, and hashCode() is defined over the same flags.
             * Two real XYSeries built with the same constructor and mutated through the same
             * public API sequence must remain equal with the same hash code. A patch that
             * merely suppresses the crash or silently performs a wrong insertion would break
             * equality and/or hash agreement.
             */
            if (!series.equals(twin) || !twin.equals(series)) {
                throw new RuntimeException("[oracle:eq] metamorphic violation: identical mutation sequences produced non-equal series input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + ",extra=" + extraPairs);
            }
            if (series.hashCode() != twin.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + ",extra=" + extraPairs + " lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidDuplicateScenario(double x, double y1, double y2,
                                                  boolean firstPrimitive, boolean secondPrimitive,
                                                  int maximumItemCount) {
        XYSeries series = new XYSeries("Series", true, true);
        series.setMaximumItemCount(maximumItemCount);

        try {
            safeAddOrUpdate(series, x, y1, firstPrimitive);
            safeAddOrUpdate(series, x, y2, secondPrimitive);

            /* Contract asserted from the failing test and the API semantics:
             * with autoSort=true and allowDuplicateXValues=true, addOrUpdate() on the same x
             * must retain both items. A throw-deleting or wrong-bookkeeping patch could avoid
             * the exception but still fail to insert at the sorted position, overwrite instead
             * of adding, or lose an item. The real observable is item count and stored y-values.
             */
            if (series.getItemCount() < 2) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate x-values should be retained when allowed input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " count=" + series.getItemCount());
            }
            Number ry0 = series.getY(0);
            Number ry1 = series.getY(1);
            if (!sameNumber(ry0, y1) || !sameNumber(ry1, y2)) {
                throw new RuntimeException("[oracle:order] metamorphic violation: duplicate insert should preserve both y-values in sorted series input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhs0=" + ry0 + " lhs1=" + ry1);
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort2] metamorphic violation: getAutoSort disagrees with constructor-established flag input=x=" + x);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void safeAddOrUpdate(XYSeries s, double x, double y, boolean primitiveOverload) {
        if (primitiveOverload) {
            s.addOrUpdate(x, y);
        } else {
            s.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
        }
    }

    private static boolean isValidation(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("SeriesException") || name.contains("Invalid"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeAsciiString(16);
        if (s == null || s.length() == 0) {
            return "K";
        }
        return s;
    }

    private static double boundedDouble(int v) {
        return ((double) (v % 100000)) / 10.0;
    }

    private static boolean sameNumber(Number actual, double expected) {
        return actual != null && Double.doubleToLongBits(actual.doubleValue()) == Double.doubleToLongBits(expected);
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
<details open><summary>▸ Output (~10,165 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        explore(data);
    }

    private static void anchor() {
        XYSeries series = new XYSeries("Series", true, true);
        XYSeries twin = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
            twin.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            twin.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        checkSeriesState(series, twin, true, true, 2, 1.0, 2.0);
    }

    private static void explore(FuzzedDataProvider data) {
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries twin = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (series.getAutoSort() != autoSort || twin.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagree input=" + key + " lhs=" + series.getAutoSort() + " rhs=" + autoSort);
        }

        int maxCount = data.consumeInt(2, 8);
        series.setMaximumItemCount(maxCount);
        twin.setMaximumItemCount(maxCount);

        int noiseBefore = data.consumeInt(0, 4);
        for (int i = 0; i < noiseBefore; i++) {
            double x = boundedDouble(data);
            double y = boundedDouble(data);
            safeAddOrUpdate(series, Double.valueOf(x), Double.valueOf(y));
            safeAddOrUpdate(twin, Double.valueOf(x), Double.valueOf(y));
            reprobeAbsentRemoval(series, twin);
        }

        double dupX = boundedDouble(data);
        double firstY = boundedDouble(data);
        double secondY = boundedDouble(data);

        int duplicateCount = data.consumeInt(2, 5);
        int survivingDuplicates = 0;
        double[] expected = new double[duplicateCount];
        for (int i = 0; i < duplicateCount; i++) {
            expected[i] = (i == 0) ? firstY : ((i == 1) ? secondY : boundedDouble(data));
        }

        for (int i = 0; i < duplicateCount; i++) {
            int beforeCount = series.getItemCount();
            safeAddOrUpdate(series, Double.valueOf(dupX), Double.valueOf(expected[i]));
            safeAddOrUpdate(twin, Double.valueOf(dupX), Double.valueOf(expected[i]));
            int afterCount = series.getItemCount();
            if (afterCount == beforeCount + 1) {
                survivingDuplicates++;
            } else if (afterCount != beforeCount) {
                throw new RuntimeException("[oracle:itemcount-step] metamorphic violation: valid duplicate addOrUpdate changed item count unexpectedly input=" + dupX + " lhs=" + afterCount + " rhs=" + (beforeCount + 1));
            }
            reprobeAbsentRemoval(series, twin);
        }

        int noiseAfter = data.consumeInt(0, 4);
        for (int i = 0; i < noiseAfter; i++) {
            double x = boundedDouble(data);
            double y = boundedDouble(data);
            safeAddOrUpdate(series, Double.valueOf(x), Double.valueOf(y));
            safeAddOrUpdate(twin, Double.valueOf(x), Double.valueOf(y));
            reprobeAbsentRemoval(series, twin);
        }

        /*
         * Contract asserted: the failing test demonstrates that with autoSort=true
         * and allowDuplicateXValues=true, addOrUpdate() must retain duplicate x-values
         * as distinct items in sorted order; deleting the throw or silently appending/
         * skipping insertion would violate item count and/or the observed y-values.
         * Also, constructor-established flags and bounded state should be reflected by
         * getAutoSort(), equals(), and hashCode() for two identically-mutated series.
         */
        if (survivingDuplicates >= 2) {
            int first = firstIndexOfX(series, dupX);
            if (first < 0 || first + 1 >= series.getItemCount()) {
                throw new RuntimeException("[oracle:dup-visible] metamorphic violation: duplicate x-values were not retained as visible adjacent items input=" + dupX + " lhs=" + first + " rhs=" + series.getItemCount());
            }
            Number y0 = series.getY(first);
            Number y1 = series.getY(first + 1);
            if (!numberEquals(y0, expected[Math.max(0, duplicateCount - survivingDuplicates)]) ||
                !numberEquals(y1, expected[Math.max(0, duplicateCount - survivingDuplicates) + 1])) {
                throw new RuntimeException("[oracle:dup-order] metamorphic violation: retained duplicate y-values do not match insertion order input=" + dupX + " lhs=" + y0 + "," + y1 + " rhs=" + expected[Math.max(0, duplicateCount - survivingDuplicates)] + "," + expected[Math.max(0, duplicateCount - survivingDuplicates) + 1]);
            }
        }

        if (!series.equals(twin)) {
            throw new RuntimeException("[oracle:equals-state] metamorphic violation: two identically-constructed and identically-mutated series are not equal input=" + key + " lhs=false rhs=true");
        }
        if (series.hashCode() != twin.hashCode()) {
            throw new RuntimeException("[oracle:hash-equals] metamorphic violation: equal series must have equal hashCode input=" + key + " lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
        }
    }

    private static void checkSeriesState(XYSeries series, XYSeries twin, boolean autoSort, boolean allowDup, int expectedCount, double expectedY0, double expectedY1) {
        if (series.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:getAutoSort] metamorphic violation: constructor-established autoSort not reported input=" + autoSort + " lhs=" + series.getAutoSort() + " rhs=" + autoSort);
        }
        if (series.getAllowDuplicateXValues() != allowDup) {
            throw new RuntimeException("[oracle:getAllowDup] metamorphic violation: constructor-established allowDuplicateXValues not reported input=" + allowDup + " lhs=" + series.getAllowDuplicateXValues() + " rhs=" + allowDup);
        }
        if (series.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:itemcount] metamorphic violation: duplicate addOrUpdate should retain both items input=1.0 lhs=" + series.getItemCount() + " rhs=" + expectedCount);
        }
        if (!numberEquals(series.getY(0), expectedY0) || !numberEquals(series.getY(1), expectedY1)) {
            throw new RuntimeException("[oracle:ys] metamorphic violation: duplicate addOrUpdate should preserve both y-values in order input=1.0 lhs=" + series.getY(0) + "," + series.getY(1) + " rhs=" + expectedY0 + "," + expectedY1);
        }
        if (!series.equals(twin)) {
            throw new RuntimeException("[oracle:equals-anchor] metamorphic violation: equal anchor series compare unequal input=Series lhs=false rhs=true");
        }
        if (series.hashCode() != twin.hashCode()) {
            throw new RuntimeException("[oracle:hash-anchor] metamorphic violation: equal anchor series have different hashCode input=Series lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
        }
    }

    private static void safeAddOrUpdate(XYSeries s, Number x, Number y) {
        try {
            s.addOrUpdate(x, y);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static void reprobeAbsentRemoval(XYSeries a, XYSeries b) {
        Number absent = Double.valueOf(999999.125);
        try {
            XYDataItem r1 = a.remove(absent);
            XYDataItem r2 = b.remove(absent);
            if (r1 != null || r2 != null) {
                throw new RuntimeException("[oracle:remove-absent] metamorphic violation: removing an absent x should report absence in every state input=" + absent + " lhs=" + r1 + " rhs=" + r2);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static int firstIndexOfX(XYSeries s, double x) {
        for (int i = 0; i < s.getItemCount(); i++) {
            Number xi = s.getX(i);
            if (numberEquals(xi, x)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean numberEquals(Number n, double d) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(d);
    }

    private static double boundedDouble(FuzzedDataProvider data) {
        int scaled = data.consumeInt(-1000000, 1000000);
        return scaled / 16.0;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~10,011 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries viaAddOrUpdate = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries viaAdd = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (viaAddOrUpdate.getAutoSort() != autoSort || viaAdd.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagreement input=" + key
                    + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + autoSort);
        }

        if (!viaAddOrUpdate.equals(viaAdd) || viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:empty-eq] metamorphic violation: equally constructed empty series must agree input=" + key
                    + " lhsEq=" + viaAddOrUpdate.equals(viaAdd) + " lhsHash=" + viaAddOrUpdate.hashCode()
                    + " rhsHash=" + viaAdd.hashCode());
        }

        int maxCount = data.consumeInt(1, 12);
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-set] metamorphic violation: setMaximumItemCount/getMaximumItemCount disagreement input="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }

        int prefix = data.consumeInt(0, 6);
        int suffix = data.consumeInt(0, 6);
        double dupX = boundedDouble(data.consumeInt());
        double firstDupY = boundedDouble(data.consumeInt());
        double secondDupY = boundedDouble(data.consumeInt());

        try {
            for (int i = 0; i < prefix; i++) {
                double x = dupX - (prefix - i) - 1.0;
                double y = boundedDouble(data.consumeInt());
                viaAddOrUpdate.addOrUpdate(new Double(x), new Double(y));
                viaAdd.add(new Double(x), new Double(y));
                reprobeState(viaAddOrUpdate, viaAdd, maxCount);
            }

            viaAddOrUpdate.addOrUpdate(new Double(dupX), new Double(firstDupY));
            viaAdd.add(new Double(dupX), new Double(firstDupY));
            reprobeState(viaAddOrUpdate, viaAdd, maxCount);

            viaAddOrUpdate.addOrUpdate(new Double(dupX), new Double(secondDupY));
            viaAdd.add(new Double(dupX), new Double(secondDupY));
            reprobeState(viaAddOrUpdate, viaAdd, maxCount);

            for (int i = 0; i < suffix; i++) {
                double x = dupX + i + 1.0;
                double y = boundedDouble(data.consumeInt());
                viaAddOrUpdate.addOrUpdate(new Double(x), new Double(y));
                viaAdd.add(new Double(x), new Double(y));
                reprobeState(viaAddOrUpdate, viaAdd, maxCount);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            return;
        }

        /*
         * Contract/oracle:
         * - In an auto-sorted series that allows duplicate x-values, addOrUpdate(Number, Number)
         *   must add a new item when the x already exists instead of corrupting insertion state.
         * - Therefore, driving the same valid sequence through addOrUpdate(...) and add(...)
         *   must leave identical observable series state. A "fix" that merely suppresses the throw,
         *   skips insertion, appends incorrectly, or updates in place will break equality/item order/count.
         */
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent valid insertion sequence must produce equal series input="
                    + describe(viaAddOrUpdate, viaAdd, dupX, firstDupY, secondDupY, prefix, suffix, maxCount));
        }

        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:hash-eq] metamorphic violation: equal series must have equal hashCode input="
                    + describe(viaAddOrUpdate, viaAdd, dupX, firstDupY, secondDupY, prefix, suffix, maxCount)
                    + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        int expectedCount = prefix + 2 + suffix;
        if (expectedCount > maxCount) {
            expectedCount = maxCount;
        }
        if (viaAddOrUpdate.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:itemcount] metamorphic violation: item count must reflect retained inserts under maximumItemCount input="
                    + describe(viaAddOrUpdate, viaAdd, dupX, firstDupY, secondDupY, prefix, suffix, maxCount)
                    + " lhs=" + viaAddOrUpdate.getItemCount() + " rhs=" + expectedCount);
        }

        assertSorted(viaAddOrUpdate, "final-addOrUpdate");
        assertSorted(viaAdd, "final-add");
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x with allowDuplicateXValues=true must retain both items input=anchor lhs="
                    + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!numEq(y0, 1.0) || !numEq(y1, 2.0)) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchor sequence must preserve both y values in sorted order input=anchor lhs0="
                    + y0 + " lhs1=" + y1 + " rhs0=1.0 rhs1=2.0");
        }
        assertSorted(series, "anchor");
    }

    private static void reprobeState(XYSeries a, XYSeries b, int maxCount) {
        if (a.getAutoSort() != b.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-stable] metamorphic violation: equivalent series must report same autoSort input=maxCount="
                    + maxCount + " lhs=" + a.getAutoSort() + " rhs=" + b.getAutoSort());
        }
        if (a.getMaximumItemCount() != maxCount || b.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-stable] metamorphic violation: maximumItemCount changed unexpectedly input=maxCount="
                    + maxCount + " lhs=" + a.getMaximumItemCount() + " rhs=" + b.getMaximumItemCount());
        }
        if (a.getItemCount() > maxCount || b.getItemCount() > maxCount) {
            throw new RuntimeException("[oracle:maxcount-bound] metamorphic violation: item count exceeded maximumItemCount input=maxCount="
                    + maxCount + " lhs=" + a.getItemCount() + " rhs=" + b.getItemCount());
        }
        assertSorted(a, "reprobe-a");
        assertSorted(b, "reprobe-b");
    }

    private static void assertSorted(XYSeries s, String where) {
        for (int i = 1; i < s.getItemCount(); i++) {
            Number prev = s.getX(i - 1);
            Number curr = s.getX(i);
            if (prev != null && curr != null && prev.doubleValue() > curr.doubleValue()) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain sorted input="
                        + where + " at=" + i + " prev=" + prev + " curr=" + curr);
            }
        }
    }

    private static boolean isRootCauseFromAddOrUpdate(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String n = c.getClass().getName();
            if (n != null && (n.contains("SeriesException") || n.contains("Validation") || n.contains("Invalid"))) {
                return true;
            }
        }
        return false;
    }

    private static double boundedDouble(int x) {
        return (x % 1000000) / 10.0;
    }

    private static boolean numEq(Number n, double d) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(d);
    }

    private static String describe(XYSeries a, XYSeries b, double dupX, double y1, double y2, int prefix, int suffix, int maxCount) {
        return "dupX=" + dupX + ",y1=" + y1 + ",y2=" + y2 + ",prefix=" + prefix + ",suffix=" + suffix
                + ",maxCount=" + maxCount + ",countA=" + a.getItemCount() + ",countB=" + b.getItemCount();
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,197 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int prefixCount = data.consumeInt(0, 6);
        boolean includeDuplicateInPrefix = data.consumeBoolean();
        double duplicateX = (double) data.consumeInt(-1000, 1000);
        double firstY = (double) data.consumeInt(-1000, 1000);
        double secondY = (double) data.consumeInt(-1000, 1000);
        double duplicatePrefixY = (double) data.consumeInt(-1000, 1000);

        int[] prefixOffsets = new int[prefixCount];
        double[] prefixYs = new double[prefixCount];
        for (int i = 0; i < prefixCount; i++) {
            int off = data.consumeInt(-20, 20);
            if (off == 0) {
                off = i + 1;
            }
            prefixOffsets[i] = off;
            prefixYs[i] = (double) data.consumeInt(-1000, 1000);
        }

        try {
            runMetamorphicScenario(duplicateX, firstY, secondY, includeDuplicateInPrefix,
                    duplicatePrefixY, prefixOffsets, prefixYs);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate with duplicates allowed must retain both items input=1.0,1.0/1.0,2.0 count=" + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression test post-condition input=1.0,1.0/1.0,2.0 y0=" + y0 + " y1=" + y1);
        }
    }

    private static void runMetamorphicScenario(double duplicateX, double firstY, double secondY,
                                               boolean includeDuplicateInPrefix, double duplicatePrefixY,
                                               int[] prefixOffsets, double[] prefixYs) {
        XYSeries viaAddOrUpdate = new XYSeries("Series", true, true);
        XYSeries viaAdd = new XYSeries("Series", true, true);

        int max = prefixOffsets.length + (includeDuplicateInPrefix ? 1 : 0) + 8;
        viaAddOrUpdate.setMaximumItemCount(max);
        viaAdd.setMaximumItemCount(max);

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-set autoSort must be reported by getAutoSort input=" + duplicateX);
        }

        for (int i = 0; i < prefixOffsets.length; i++) {
            double x = duplicateX + prefixOffsets[i];
            if (x == duplicateX) {
                x = duplicateX + i + 1.0;
            }
            viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(prefixYs[i]));
            viaAdd.add(Double.valueOf(x), Double.valueOf(prefixYs[i]));
        }

        if (includeDuplicateInPrefix) {
            viaAddOrUpdate.add(Double.valueOf(duplicateX), Double.valueOf(duplicatePrefixY));
            viaAdd.add(Double.valueOf(duplicateX), Double.valueOf(duplicatePrefixY));
        }

        /* Contract/oracle:
           With allowDuplicateXValues=true, addOrUpdate(Number, Number) must add a new item for a duplicate x
           instead of overwriting. Therefore, on identical starting series, performing duplicate insertions with
           addOrUpdate must produce the same observable series state as performing the same insertions with add(...). */
        viaAddOrUpdate.addOrUpdate(Double.valueOf(duplicateX), Double.valueOf(firstY));
        viaAddOrUpdate.addOrUpdate(Double.valueOf(duplicateX), Double.valueOf(secondY));

        viaAdd.add(Double.valueOf(duplicateX), Double.valueOf(firstY));
        viaAdd.add(Double.valueOf(duplicateX), Double.valueOf(secondY));

        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addupdate] metamorphic violation: addOrUpdate with duplicates allowed must agree with add on equivalent inputs input=duplicateX="
                    + duplicateX + ", firstY=" + firstY + ", secondY=" + secondY + ", includeDuplicateInPrefix=" + includeDuplicateInPrefix
                    + " lhsCount=" + viaAddOrUpdate.getItemCount() + " rhsCount=" + viaAdd.getItemCount());
        }

        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:equals-hash] metamorphic violation: equal series must have equal hash codes input=duplicateX="
                    + duplicateX + ", firstY=" + firstY + ", secondY=" + secondY
                    + " lhsHash=" + viaAddOrUpdate.hashCode() + " rhsHash=" + viaAdd.hashCode());
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (isOracleFailure(t)) {
            throw asRuntime(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            throw asRuntime(t);
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof RuntimeException) {
                String msg = cur.getMessage();
                if (msg != null && msg.startsWith("[oracle:")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IndexOutOfBoundsException && hasAddOrUpdateFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasAddOrUpdateFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException asRuntime(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
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
<details open><summary>▸ Output (~9,536 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from XYSeriesTests.testBug1955483.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));

            // Contract/oracle: with allowDuplicateXValues=true, addOrUpdate() must add
            // a second item for an existing x rather than lose it; with autoSort=true the
            // series remains sorted. A throw-deleting or wrong-bookkeeping patch would
            // break these observable results even if no exception is thrown.
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate must retain both items input=seed lhs="
                        + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: duplicate addOrUpdate must preserve both y values in order input=seed lhs=["
                        + y0 + "," + y1 + "] rhs=[1.0,2.0]");
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=seed lhs="
                        + series.getAutoSort() + " rhs=true");
            }

            try {
                XYSeries expected = new XYSeries("Series", true, true);
                expected.add(1.0, 1.0);
                expected.add(1.0, 2.0);
                if (!series.equals(expected)) {
                    throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate with duplicate-x allowed must agree with add on equivalent input input=seed lhs="
                            + series.getItems() + " rhs=" + expected.getItems());
                }
                if (series.hashCode() != expected.hashCode()) {
                    throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hashCode input=seed lhs="
                            + series.hashCode() + " rhs=" + expected.hashCode());
                }
            } catch (RuntimeException ignored) {
                // If the comparison side throws, skip the relation per hygiene rules.
            }
        } catch (RuntimeException t) {
            boolean validation = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)
                    || (t instanceof org.jfree.data.general.SeriesException);
            if (validation) {
                return;
            }
            boolean inAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    inAddOrUpdate = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && inAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: property-based trigger:
        // autoSort=true, allowDuplicateXValues=true, and addOrUpdate() is called for an x
        // already present in the series. Compare against the equivalent add() behavior.
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 8);
        double duplicateX = (double) data.consumeInt(-1000, 1000);
        double duplicateY = (double) data.consumeInt(-1000, 1000);

        try {
            XYSeries lhs = new XYSeries(key, true, true);
            XYSeries rhs = new XYSeries(key, true, true);

            int maxCount = prefixCount + 4 + data.consumeInt(0, 4);
            lhs.setMaximumItemCount(maxCount);
            rhs.setMaximumItemCount(maxCount);

            if (!lhs.getAutoSort() || !rhs.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input="
                        + key + " lhs=" + lhs.getAutoSort() + " rhs=true");
            }

            int duplicatePosition = data.consumeInt(0, prefixCount);
            int added = 0;
            boolean insertedExistingDuplicate = false;

            while (added < prefixCount) {
                if (!insertedExistingDuplicate && added == duplicatePosition) {
                    double firstY = (double) data.consumeInt(-1000, 1000);
                    lhs.add(duplicateX, firstY);
                    rhs.add(duplicateX, firstY);
                    insertedExistingDuplicate = true;
                } else {
                    int raw = data.consumeInt(-1000, 1000);
                    double x = (double) raw;
                    if (x == duplicateX) {
                        x = duplicateX + 0.5 + added;
                    }
                    Double y = new Double((double) data.consumeInt(-1000, 1000));
                    lhs.add(new Double(x), y);
                    rhs.add(new Double(x), y);
                    added++;
                }
            }
            if (!insertedExistingDuplicate) {
                double firstY = (double) data.consumeInt(-1000, 1000);
                lhs.add(duplicateX, firstY);
                rhs.add(duplicateX, firstY);
            }

            // Equivalent-input oracle:
            // For allowDuplicateXValues=true, addOrUpdate(x,y) on an already-present x must
            // behave like add(x,y): it appends a second item at the sorted insertion point
            // instead of overwriting or dropping data. Both sides use only real library APIs.
            lhs.addOrUpdate(new Double(duplicateX), new Double(duplicateY));
            rhs.add(new Double(duplicateX), new Double(duplicateY));

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:eq-add-vs-addOrUpdate] metamorphic violation: addOrUpdate must agree with add when duplicate x values are allowed input=key="
                        + key + ",x=" + duplicateX + ",y=" + duplicateY + ",prefix=" + prefixCount
                        + " lhs=" + lhs.getItems() + " rhs=" + rhs.getItems());
            }
            if (lhs.hashCode() != rhs.hashCode()) {
                throw new RuntimeException("[oracle:hash-add-vs-addOrUpdate] metamorphic violation: equal series must have equal hashCode input=key="
                        + key + ",x=" + duplicateX + ",y=" + duplicateY + " lhs=" + lhs.hashCode()
                        + " rhs=" + rhs.hashCode());
            }
            if (lhs.getItemCount() != rhs.getItemCount()) {
                throw new RuntimeException("[oracle:count-add-vs-addOrUpdate] metamorphic violation: equivalent duplicate insertion must yield same item count input=key="
                        + key + ",x=" + duplicateX + ",y=" + duplicateY + " lhs=" + lhs.getItemCount()
                        + " rhs=" + rhs.getItemCount());
            }
            for (int i = 0; i < lhs.getItemCount(); i++) {
                Number lx = lhs.getX(i);
                Number rx = rhs.getX(i);
                Number ly = lhs.getY(i);
                Number ry = rhs.getY(i);
                boolean sameX = (lx == null && rx == null) || (lx != null && rx != null && lx.doubleValue() == rx.doubleValue());
                boolean sameY = (ly == null && ry == null) || (ly != null && ry != null && ly.doubleValue() == ry.doubleValue());
                if (!sameX || !sameY) {
                    throw new RuntimeException("[oracle:items-add-vs-addOrUpdate] metamorphic violation: equivalent duplicate insertion must yield same ordered items input=key="
                            + key + ",x=" + duplicateX + ",y=" + duplicateY + ",index=" + i
                            + " lhs=(" + lx + "," + ly + ") rhs=(" + rx + "," + ry + ")");
                }
            }
        } catch (RuntimeException t) {
            boolean validation = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)
                    || (t instanceof org.jfree.data.general.SeriesException);
            if (validation) {
                return;
            }
            boolean inAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    inAddOrUpdate = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && inAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
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
<details open><summary>▸ Output (~7,614 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression from XYSeriesTests.testBug1955483.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            XYSeries expected = new XYSeries("Series", true, true);

            series.setMaximumItemCount(10);
            expected.setMaximumItemCount(10);

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            expected.add(Double.valueOf(1.0), Double.valueOf(1.0));
            expected.add(Double.valueOf(1.0), Double.valueOf(2.0));

            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-anchor] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=Series lhs=" + series.getAutoSort() + " rhs=true");
            }

            // Contract/oracle: with autoSort=true and duplicates allowed, addOrUpdate() on a duplicate x
            // must add a new item rather than losing it or reordering it incorrectly; the failing test
            // demonstrates the expected observable state: count 2, y(0)=1.0, y(1)=2.0.
            if (series.getItemCount() != 2
                    || !Double.valueOf(1.0).equals(series.getY(0))
                    || !Double.valueOf(2.0).equals(series.getY(1))) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: duplicate addOrUpdate must preserve both items in sorted series input=x=1.0 lhs=count="
                        + series.getItemCount() + ",y0=" + series.getY(0) + ",y1=" + series.getY(1)
                        + " rhs=count=2,y0=1.0,y1=2.0");
            }

            // Contract/oracle: two series built through real public APIs with equivalent operations
            // (addOrUpdate for the candidate, add for the sibling overload family) must compare equal.
            if (!series.equals(expected) || !expected.equals(series) || series.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) should agree for duplicate-x insertion when duplicates are allowed input=x=1.0 lhs="
                        + series + " rhs=" + expected + " lhsHash=" + series.hashCode() + " rhsHash=" + expected.hashCode());
            }
        } catch (RuntimeException t) {
            boolean addOrUpdateFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    addOrUpdateFrame = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && addOrUpdateFrame) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.jfree")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: vary valid duplicate-x scenarios that exercise the same patched path:
        // autoSort=true, allowDuplicateXValues=true, and the same x added twice via addOrUpdate().
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries actual = new XYSeries(key, true, true);
        XYSeries oracle = new XYSeries(key, true, true);

        int prefixCount = data.consumeInt(0, 6);
        int dupX = data.consumeInt(-1000, 1000);
        int maxCount = prefixCount + 2 + data.consumeInt(0, 4);
        actual.setMaximumItemCount(maxCount);
        oracle.setMaximumItemCount(maxCount);

        try {
            for (int i = 0; i < prefixCount; i++) {
                int raw = data.consumeInt(-1000, 1000);
                if (raw >= dupX) {
                    raw++;
                }
                Double x = Double.valueOf(raw);
                Double y = Double.valueOf(data.consumeInt(-1000, 1000));
                actual.add(x, y);
                oracle.add(x, y);
            }

            Double firstY = Double.valueOf(data.consumeInt(-1000, 1000));
            Double secondY = Double.valueOf(data.consumeInt(-1000, 1000));
            Double duplicateX = Double.valueOf(dupX);

            int before = actual.getItemCount();

            actual.addOrUpdate(duplicateX, firstY);
            actual.addOrUpdate(duplicateX, secondY);

            oracle.add(duplicateX, firstY);
            oracle.add(duplicateX, secondY);

            if (!actual.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-explore] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=" + key + " lhs=" + actual.getAutoSort() + " rhs=true");
            }

            // Contract/oracle: on these valid inputs, duplicate x values are allowed, so two addOrUpdate()
            // calls with the same x must increase the item count by exactly 2 when maximumItemCount permits it.
            if (actual.getItemCount() != before + 2) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate should add two items when duplicates are allowed input=key="
                        + key + ",x=" + duplicateX + ",before=" + before + " lhs=" + actual.getItemCount() + " rhs=" + (before + 2));
            }

            // Contract/oracle using only real library calls: addOrUpdate(Number,Number) should produce the same
            // final series state as add(Number,Number) for these valid duplicate-x inputs.
            if (!actual.equals(oracle) || !oracle.equals(actual) || actual.hashCode() != oracle.hashCode()) {
                throw new RuntimeException("[oracle:eq-sibling] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) should agree on valid duplicate-x inputs input=key="
                        + key + ",x=" + duplicateX + ",y1=" + firstY + ",y2=" + secondY
                        + " lhsCount=" + actual.getItemCount() + " rhsCount=" + oracle.getItemCount()
                        + " lhsHash=" + actual.hashCode() + " rhsHash=" + oracle.hashCode());
            }
        } catch (RuntimeException t) {
            boolean addOrUpdateFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    addOrUpdateFrame = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && addOrUpdateFrame) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.jfree")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,854 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (series.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:getAutoSort] metamorphic violation: constructor flag/getAutoSort disagree input=" + autoSort + " lhs=" + series.getAutoSort() + " rhs=" + autoSort);
        }

        XYSeries mirror = new XYSeries(key, autoSort, allowDuplicateXValues);
        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: equally constructed empty series must be equal input=" + key + " lhs=" + series + " rhs=" + mirror);
        }

        int maximum = data.consumeInt(2, 64);
        series.setMaximumItemCount(maximum);
        mirror.setMaximumItemCount(maximum);
        if (series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:hash-empty] metamorphic violation: equal series must have equal hashCode input=" + maximum + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
        }

        int prefixCount = data.consumeInt(0, 8);
        for (int i = 0; i < prefixCount; i++) {
            double x = data.consumeInt(-1000, 1000);
            double y = data.consumeInt(-1000, 1000);
            try {
                series.add(x, y);
                mirror.add(x, y);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            }
        }

        int duplicateX = data.consumeInt(-1000, 1000);
        double y1 = data.consumeInt(-1000, 1000);
        double y2 = data.consumeInt(-1000, 1000);

        try {
            series.addOrUpdate(new Double(duplicateX), new Double(y1));
            series.addOrUpdate(new Double(duplicateX), new Double(y2));

            mirror.addOrUpdate((double) duplicateX, y1);
            mirror.addOrUpdate((double) duplicateX, y2);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /*
         * Contract asserted:
         * - With allowDuplicateXValues=true, addOrUpdate(Number, Number) must add a new item
         *   when the x-value already exists instead of overwriting.
         * - With autoSort=true, the internal list remains sorted by x after insertion.
         * A "fix" that only suppresses the exception or appends incorrectly breaks these checks.
         */
        int expected = Math.min(maximum, prefixCount + 2);
        if (series.getItemCount() != expected) {
            throw new RuntimeException("[oracle:itemCount] metamorphic violation: duplicate x with allowDuplicateXValues=true must increase size input=" + duplicateX + " lhs=" + series.getItemCount() + " rhs=" + expected);
        }

        if (series.getItemCount() >= 2) {
            Number prev = series.getX(0);
            for (int i = 1; i < series.getItemCount(); i++) {
                Number cur = series.getX(i);
                if (prev.doubleValue() > cur.doubleValue()) {
                    throw new RuntimeException("[oracle:sorted] metamorphic violation: autoSort=true must keep x values nondecreasing input=" + duplicateX + " lhs=" + prev + " rhs=" + cur);
                }
                prev = cur;
            }
        }

        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:overload-equals] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent numeric inputs input=" + duplicateX + "," + y1 + "," + y2 + " lhs=" + series + " rhs=" + mirror);
        }
        if (series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:overload-hash] metamorphic violation: equal series must have equal hashCode input=" + duplicateX + "," + y1 + "," + y2 + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x with allowDuplicateXValues=true must retain both items input=1.0 lhs=" + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchored failing test values must be preserved in sorted duplicate insertion input=1.0 lhs=" + y0 + "," + y1 + " rhs=1.0,2.0");
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("SeriesException") || name.contains("Validation") || name.contains("Invalid"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
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
<details open><summary>▸ Output (~9,420 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            XYSeries series = new XYSeries("Series", true, true);
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-anchor] metamorphic violation: constructor/getAutoSort disagreement input=true lhs="
                        + series.getAutoSort() + " rhs=true");
            }

            probeNullXRejection(series);

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            probeNullXRejection(series);

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
            probeNullXRejection(series);

            /* Contract asserted: with allowDuplicateXValues=true, addOrUpdate(Number, Number)
             * must retain both items for duplicate x-values rather than overwrite or drop one;
             * the failing test shows the observable post-condition: Y values 1.0 then 2.0 and itemCount 2.
             * A throw-deleting or silently-wrong patch that appends/overwrites/skips insertion breaks this.
             */
            assertDuplicateScenario(series, 1.0, 2.0, "anchor");

            XYSeries twin = new XYSeries("Series", true, true);
            twin.addOrUpdate(1.0, 1.0);
            twin.addOrUpdate(1.0, 2.0);
            if (!series.equals(twin) || !twin.equals(series)) {
                throw new RuntimeException("[oracle:equals-anchor] metamorphic violation: equivalent duplicate-add scenarios must be equal input=1.0,2.0 lhs="
                        + series.getItems() + " rhs=" + twin.getItems());
            }
            if (series.hashCode() != twin.hashCode()) {
                throw new RuntimeException("[oracle:hash-anchor] metamorphic violation: equal series must have equal hashCode input=1.0,2.0 lhs="
                        + series.hashCode() + " rhs=" + twin.hashCode());
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries twin = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (series.getAutoSort() != autoSort || twin.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:autoSort-explore] metamorphic violation: constructor/getAutoSort disagreement input="
                    + autoSort + " lhs=" + series.getAutoSort() + " rhs=" + twin.getAutoSort());
        }

        int maximum = data.consumeInt(2, 12);
        try {
            series.setMaximumItemCount(maximum);
            twin.setMaximumItemCount(maximum);
            probeNullXRejection(series);
            probeNullXRejection(twin);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        int leftCount = data.consumeInt(0, 4);
        int rightCount = data.consumeInt(0, 4);
        int dupBase = data.consumeInt(-1000, 1000);
        int step = data.consumeInt(1, 5);

        try {
            for (int i = leftCount; i >= 1; i--) {
                double x = dupBase - (double) i * step;
                double y = data.consumeInt(-1000, 1000);
                series.add(x, y);
                twin.add(x, y);
                probeNullXRejection(series);
                probeNullXRejection(twin);
            }

            double firstY = data.consumeInt(-1000, 1000);
            double secondY = data.consumeInt(-1000, 1000);
            double dupX = dupBase;

            series.addOrUpdate(Double.valueOf(dupX), Double.valueOf(firstY));
            twin.addOrUpdate(dupX, firstY);
            probeNullXRejection(series);
            probeNullXRejection(twin);

            for (int i = 1; i <= rightCount; i++) {
                double x = dupBase + (double) i * step;
                double y = data.consumeInt(-1000, 1000);
                series.add(x, y);
                twin.add(x, y);
                probeNullXRejection(series);
                probeNullXRejection(twin);
            }

            series.addOrUpdate(Double.valueOf(dupX), Double.valueOf(secondY));
            twin.addOrUpdate(dupX, secondY);
            probeNullXRejection(series);
            probeNullXRejection(twin);

            if (!series.equals(twin) || !twin.equals(series)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: Number and double addOrUpdate overloads on equivalent inputs must yield equal series input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + ", left=" + leftCount + ", right=" + rightCount
                        + " lhs=" + series.getItems() + " rhs=" + twin.getItems());
            }
            if (series.hashCode() != twin.hashCode()) {
                throw new RuntimeException("[oracle:overload-hash] metamorphic violation: equal series from equivalent overloads must have equal hashCode input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + " lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
            }

            int dupIndex = series.indexOf(Double.valueOf(dupX));
            if (dupIndex < 0 || dupIndex + 1 >= series.getItemCount()) {
                throw new RuntimeException("[oracle:dup-presence] metamorphic violation: duplicate x inserted via addOrUpdate must leave two accessible entries input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + ", itemCount=" + series.getItemCount() + ", index=" + dupIndex);
            }

            Number yAtFirst = series.getY(dupIndex);
            Number yAtSecond = series.getY(dupIndex + 1);
            if (!sameNumber(yAtFirst, firstY) || !sameNumber(yAtSecond, secondY)) {
                throw new RuntimeException("[oracle:dup-order] metamorphic violation: duplicate x-values must retain both y-values in sorted duplicate-adjacent order input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + ", index=" + dupIndex
                        + " lhs=(" + yAtFirst + "," + yAtSecond + ") rhs=(" + firstY + "," + secondY + ")");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void assertDuplicateScenario(XYSeries series, double firstY, double secondY, String oracleId) {
        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:" + oracleId + "-count] metamorphic violation: duplicate x-values in a duplicate-allowing series must increase itemCount to 2 input=("
                    + firstY + "," + secondY + ") lhs=" + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!sameNumber(y0, firstY) || !sameNumber(y1, secondY)) {
            throw new RuntimeException("[oracle:" + oracleId + "-ys] metamorphic violation: duplicate x-values must retain both y-values in insertion-visible order input=("
                    + firstY + "," + secondY + ") lhs=(" + y0 + "," + y1 + ") rhs=(" + firstY + "," + secondY + ")");
        }
    }

    private static void probeNullXRejection(XYSeries series) {
        try {
            series.addOrUpdate((Number) null, Double.valueOf(0.0));
            throw new RuntimeException("[oracle:null-x] metamorphic violation: addOrUpdate must reject null x input=null lhs=accepted rhs=rejected");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean sameNumber(Number n, double expected) {
        return n != null && Double.compare(n.doubleValue(), expected) == 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~6,059 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(true, 1, 1, 2, 0, 0, 10);

        int duplicateX = data.consumeInt(-1000, 1000);
        int firstY = data.consumeInt(-1000, 1000);
        int secondY = data.consumeInt(-1000, 1000);

        int lowerCount = data.consumeInt(0, 4);
        int upperCount = data.consumeInt(0, 4);
        int maxCount = data.consumeInt(2, 20);

        runScenario(false, duplicateX, firstY, secondY, lowerCount, upperCount, maxCount);
    }

    private static void runScenario(boolean anchor, int duplicateX, int firstY, int secondY,
                                    int lowerCount, int upperCount, int maxCount) {
        try {
            XYSeries actual = new XYSeries(anchor ? "Series" : "FuzzSeries", true, true);
            XYSeries expected = new XYSeries(anchor ? "Series" : "FuzzSeries", true, true);

            actual.setMaximumItemCount(maxCount);
            expected.setMaximumItemCount(maxCount);

            if (actual.getMaximumItemCount() != maxCount || expected.getMaximumItemCount() != maxCount) {
                throw new RuntimeException("[oracle:max-count] metamorphic violation: set/get maximum item count disagree input="
                        + maxCount + " lhs=" + actual.getMaximumItemCount() + " rhs=" + expected.getMaximumItemCount());
            }

            if (!actual.getAutoSort() || !expected.getAutoSort()) {
                throw new RuntimeException("[oracle:auto-sort] metamorphic violation: constructor-established autoSort must be observable input="
                        + duplicateX + " lhs=" + actual.getAutoSort() + " rhs=" + expected.getAutoSort());
            }

            for (int i = 0; i < lowerCount; i++) {
                int x = duplicateX - (i + 1);
                int y = firstY - (i + 1);
                actual.addOrUpdate(Integer.valueOf(x), Integer.valueOf(y));
                expected.add(Integer.valueOf(x), Integer.valueOf(y));
            }

            for (int i = 0; i < upperCount; i++) {
                int x = duplicateX + (i + 1);
                int y = secondY + (i + 1);
                actual.addOrUpdate(Integer.valueOf(x), Integer.valueOf(y));
                expected.add(Integer.valueOf(x), Integer.valueOf(y));
            }

            actual.addOrUpdate(Integer.valueOf(duplicateX), Integer.valueOf(firstY));
            expected.add(Integer.valueOf(duplicateX), Integer.valueOf(firstY));

            actual.addOrUpdate(Integer.valueOf(duplicateX), Integer.valueOf(secondY));
            expected.add(Integer.valueOf(duplicateX), Integer.valueOf(secondY));

            // Contract/oracle: with allowDuplicateXValues=true, addOrUpdate(Number, Number) takes the "add new item"
            // path for an existing x-value, so on valid inputs it must produce the same observable series state as
            // performing the corresponding add(Number, Number) calls. A throw-deleting or wrong-insertion patch would
            // break equality/item order/count while avoiding the original crash.
            if (!actual.equals(expected) || !expected.equals(actual)) {
                throw new RuntimeException("[oracle:eq-add-vs-addOrUpdate] metamorphic violation: duplicate-allowed addOrUpdate should match add on equivalent valid inputs input="
                        + duplicateX + "," + firstY + "," + secondY + " lhsCount=" + actual.getItemCount()
                        + " rhsCount=" + expected.getItemCount());
            }

            if (actual.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:hash-eq] metamorphic violation: equal series must have equal hash codes input="
                        + duplicateX + "," + firstY + "," + secondY + " lhs=" + actual.hashCode()
                        + " rhs=" + expected.hashCode());
            }

            if (anchor) {
                if (actual.getItemCount() != 2) {
                    throw new RuntimeException("[oracle:anchor-count] metamorphic violation: anchored reproduction must retain two duplicate items input="
                            + duplicateX + "," + firstY + "," + secondY + " lhs=" + actual.getItemCount() + " rhs=2");
                }
                Number y0 = actual.getY(0);
                Number y1 = actual.getY(1);
                if (!Integer.valueOf(firstY).equals(y0) || !Integer.valueOf(secondY).equals(y1)) {
                    throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchored reproduction must preserve sorted duplicate insertion order input="
                            + duplicateX + "," + firstY + "," + secondY + " lhs0=" + y0 + " lhs1=" + y1
                            + " rhs0=" + firstY + " rhs1=" + secondY);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.endsWith("SeriesException")
                || name.endsWith("ValidationException")
                || name.endsWith("InvalidParameterException")
                || name.endsWith("IllegalArgumentException");
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,894 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            if (!anchor.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagreement input=Series lhs=false rhs=true");
            }
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            /* Contract asserted: with allowDuplicateXValues=true, addOrUpdate() must insert a new item for an existing x instead of overwriting;
               with autoSort=true, the series remains sorted and both duplicate-x items remain observable. A throw-deleting or wrong-insert patch
               would break the item count / duplicate visibility / equality checks below without necessarily crashing. */
            if (anchor.getItemCount() != 2
                    || toDouble(anchor.getY(0)) != 1.0
                    || toDouble(anchor.getY(1)) != 2.0
                    || !isNonDecreasing(anchor)) {
                throw new RuntimeException(
                        "[oracle:anchor-duplicate] metamorphic violation: duplicate x insertion must preserve both values in sorted order input=x=1.0,y=[1.0,2.0] lhs=count="
                                + anchor.getItemCount()
                                + ",y0=" + valueString(anchor, 0)
                                + ",y1=" + valueString(anchor, 1)
                                + " rhs=count=2,y0=1.0,y1=2.0");
            }

            XYSeries anchorTwin = new XYSeries("Series", true, true);
            anchorTwin.addOrUpdate(1.0, 1.0);
            anchorTwin.addOrUpdate(1.0, 2.0);
            if (!anchor.equals(anchorTwin)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: same-name overloads and shared-state readers must agree input=anchor lhs=false rhs=true");
            }
            if (anchor.hashCode() != anchorTwin.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hashCode input=anchor lhs="
                        + anchor.hashCode() + " rhs=" + anchorTwin.hashCode());
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(1, 6);
        int maximumItemCount = prefixCount + 1 + data.consumeInt(0, 4);

        XYSeries seriesNumber = new XYSeries(key, true, true);
        XYSeries seriesDouble = new XYSeries(key, true, true);

        try {
            seriesNumber.setMaximumItemCount(maximumItemCount);
            seriesDouble.setMaximumItemCount(maximumItemCount);

            if (!seriesNumber.getAutoSort() || !seriesDouble.getAutoSort()) {
                throw new RuntimeException("[oracle:explore-autosort] metamorphic violation: constructor/getAutoSort disagreement input=key="
                        + key + " lhs=" + seriesNumber.getAutoSort() + "," + seriesDouble.getAutoSort() + " rhs=true,true");
            }

            double[] xs = new double[prefixCount];
            for (int i = 0; i < prefixCount; i++) {
                xs[i] = (double) data.consumeInt(-1000, 1000);
                double y = (double) data.consumeInt(-1000, 1000);
                seriesNumber.addOrUpdate(Double.valueOf(xs[i]), Double.valueOf(y));
                seriesDouble.addOrUpdate(Double.valueOf(xs[i]), Double.valueOf(y));
            }

            if (!seriesNumber.equals(seriesDouble)) {
                throw new RuntimeException("[oracle:prefix-equals] metamorphic violation: identical setup through real API must yield equal series input=key="
                        + key + " lhs=false rhs=true");
            }
            if (seriesNumber.hashCode() != seriesDouble.hashCode()) {
                throw new RuntimeException("[oracle:prefix-hash] metamorphic violation: equal series must have equal hashCode input=key="
                        + key + " lhs=" + seriesNumber.hashCode() + " rhs=" + seriesDouble.hashCode());
            }
            if (!isNonDecreasing(seriesNumber) || !isNonDecreasing(seriesDouble)) {
                throw new RuntimeException("[oracle:prefix-sort] metamorphic violation: autoSort series must remain sorted after setup input=key="
                        + key + " lhs=unsorted rhs=sorted");
            }

            int dupSlot = data.consumeInt(0, prefixCount - 1);
            double dupX = xs[dupSlot];
            double dupY = (double) data.consumeInt(-1000, 1000);

            int beforeCount = seriesNumber.getItemCount();
            int beforeDupOccurrences = countOccurrences(seriesNumber, dupX);

            seriesNumber.addOrUpdate(Double.valueOf(dupX), Double.valueOf(dupY));
            seriesDouble.addOrUpdate(dupX, dupY);

            if (!isNonDecreasing(seriesNumber) || !isNonDecreasing(seriesDouble)) {
                throw new RuntimeException("[oracle:post-sort] metamorphic violation: autoSort series must remain sorted after duplicate insertion input=x="
                        + dupX + " lhs=unsorted rhs=sorted");
            }

            int afterCount = seriesNumber.getItemCount();
            int afterDupOccurrences = countOccurrences(seriesNumber, dupX);

            if (afterCount != beforeCount + 1 || afterDupOccurrences != beforeDupOccurrences + 1) {
                throw new RuntimeException(
                        "[oracle:dup-insert] metamorphic violation: allowDuplicateXValues=true means addOrUpdate(existingX, y) adds a new item, not overwrite input=x="
                                + dupX + ",y=" + dupY
                                + " lhs=countBefore=" + beforeCount
                                + ",countAfter=" + afterCount
                                + ",dupBefore=" + beforeDupOccurrences
                                + ",dupAfter=" + afterDupOccurrences
                                + " rhs=countAfter=" + (beforeCount + 1)
                                + ",dupAfter=" + (beforeDupOccurrences + 1));
            }

            if (!seriesNumber.equals(seriesDouble)) {
                throw new RuntimeException("[oracle:overload-equals] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent inputs input=x="
                        + dupX + ",y=" + dupY + " lhs=false rhs=true");
            }
            if (seriesNumber.hashCode() != seriesDouble.hashCode()) {
                throw new RuntimeException("[oracle:overload-hash] metamorphic violation: equal series from overload-equivalent calls must have equal hashCode input=x="
                        + dupX + ",y=" + dupY + " lhs=" + seriesNumber.hashCode() + " rhs=" + seriesDouble.hashCode());
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException
                    || cur instanceof NumberFormatException
                    || cur instanceof org.jfree.data.general.SeriesException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isNonDecreasing(XYSeries s) {
        int n = s.getItemCount();
        for (int i = 1; i < n; i++) {
            Number prev = s.getX(i - 1);
            Number curr = s.getX(i);
            if (prev == null || curr == null) {
                return false;
            }
            if (prev.doubleValue() > curr.doubleValue()) {
                return false;
            }
        }
        return true;
    }

    private static int countOccurrences(XYSeries s, double x) {
        int count = 0;
        int n = s.getItemCount();
        for (int i = 0; i < n; i++) {
            Number xi = s.getX(i);
            if (xi != null && xi.doubleValue() == x) {
                count++;
            }
        }
        return count;
    }

    private static double toDouble(Number n) {
        return n == null ? Double.NaN : n.doubleValue();
    }

    private static String valueString(XYSeries s, int index) {
        if (index < 0 || index >= s.getItemCount()) {
            return "<oob>";
        }
        Number y = s.getY(index);
        return y == null ? "null" : String.valueOf(y.doubleValue());
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,081 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(1.0, 1.0, 2.0, 0, 0, data);

        double dupX = boundedDouble(data.consumeInt());
        double firstY = boundedDouble(data.consumeInt());
        double secondY = boundedDouble(data.consumeInt());
        int prefixCount = data.consumeInt(0, 6);
        int suffixCount = data.consumeInt(0, 6);

        runScenario(dupX, firstY, secondY, prefixCount, suffixCount, data);
    }

    private static void runScenario(double dupX, double firstY, double secondY,
                                    int prefixCount, int suffixCount,
                                    FuzzedDataProvider data) {
        XYSeries series = new XYSeries(nonNullKey(data), true, true);
        try {
            populateDistinctSorted(series, dupX, prefixCount, true);
            series.addOrUpdate(new Double(dupX), new Double(firstY));
            populateDistinctSorted(series, dupX, suffixCount, false);
            series.addOrUpdate(new Double(dupX), new Double(secondY));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            checkPostConditions(series, dupX, firstY, secondY, prefixCount, suffixCount, data);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkPostConditions(XYSeries actual, double dupX, double firstY, double secondY,
                                            int prefixCount, int suffixCount,
                                            FuzzedDataProvider data) {
        int expectedCount = prefixCount + suffixCount + 2;
        if (actual.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:item-count] metamorphic violation: duplicate addOrUpdate on a series that allows duplicate X values must add a second item input="
                    + dupX + "," + firstY + "," + secondY + " lhs=" + actual.getItemCount() + " rhs=" + expectedCount);
        }

        int firstIndex = -1;
        int secondIndex = -1;
        for (int i = 0; i < actual.getItemCount(); i++) {
            Number x = actual.getX(i);
            if (x != null && x.doubleValue() == dupX) {
                if (firstIndex < 0) {
                    firstIndex = i;
                } else if (secondIndex < 0) {
                    secondIndex = i;
                    break;
                }
            }
        }
        if (firstIndex < 0 || secondIndex < 0) {
            throw new RuntimeException("[oracle:dup-visible] metamorphic violation: both duplicate X items must remain observable after addOrUpdate when duplicates are allowed input="
                    + dupX + "," + firstY + "," + secondY + " lhs=" + firstIndex + "," + secondIndex + " rhs=two-visible-items");
        }
        Number y0 = actual.getY(firstIndex);
        Number y1 = actual.getY(secondIndex);
        if (!sameNumber(y0, firstY) || !sameNumber(y1, secondY)) {
            throw new RuntimeException("[oracle:y-order] metamorphic violation: the failing test's contract requires the two duplicate additions to remain as distinct items with their Y values preserved in insertion order for equal X input="
                    + dupX + "," + firstY + "," + secondY + " lhs=" + y0 + "," + y1 + " rhs=" + firstY + "," + secondY);
        }

        /* Contract asserted: with allowDuplicateXValues=true, addOrUpdate(Number, Number) must behave like adding
           another item rather than overwriting. For equivalent valid inputs on the same initial state, using
           addOrUpdate twice should produce the same observable series as using the add(Number, Number) overload twice.
           A throw-deleting or silently-wrong patch would break equality/item counts/order without throwing. */
        XYSeries expected = new XYSeries(nonNullKey(data), true, true);
        try {
            populateDistinctSorted(expected, dupX, prefixCount, true);
            expected.add(new Double(dupX), new Double(firstY));
            populateDistinctSorted(expected, dupX, suffixCount, false);
            expected.add(new Double(dupX), new Double(secondY));
        } catch (RuntimeException t) {
            return;
        }

        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:equiv-overload] metamorphic violation: addOrUpdate and add must agree for duplicate X values when duplicates are allowed input="
                    + dupX + "," + firstY + "," + secondY + " lhsCount=" + actual.getItemCount() + " rhsCount=" + expected.getItemCount());
        }

        if (actual.getAutoSort() != expected.getAutoSort()) {
            throw new RuntimeException("[oracle:auto-sort] metamorphic violation: constructor-established autoSort flag must agree with reader after addOrUpdate input="
                    + dupX + " lhs=" + actual.getAutoSort() + " rhs=" + expected.getAutoSort());
        }
    }

    private static void populateDistinctSorted(XYSeries series, double dupX, int count, boolean lowerThanDup) {
        for (int i = 0; i < count; i++) {
            double x;
            if (lowerThanDup) {
                x = dupX - (count - i);
            } else {
                x = dupX + (i + 1);
            }
            if (x == dupX) {
                x = lowerThanDup ? dupX - 1.0 : dupX + 1.0;
            }
            series.add(new Double(x), new Double(i));
        }
    }

    private static String nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeString(20);
        if (s == null || s.length() == 0) {
            return "K";
        }
        return s;
    }

    private static double boundedDouble(int v) {
        return ((double) (v % 1000000)) / 10.0;
    }

    private static boolean sameNumber(Number n, double v) {
        return n != null && n.doubleValue() == v;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null && name.endsWith("SeriesException");
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,489 chars)</summary>

```
package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input from XYSeriesTests.testBug1955483.
        // Valid by construction: non-null key, autoSort=true, allowDuplicateXValues=true, non-null Number args.
        XYSeries anchor = new XYSeries("Series", true, true);
        if (!anchor.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagreement input=true lhs="
                    + anchor.getAutoSort() + " rhs=true");
        }
        try {
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            return;
        }

        // Contract/oracle: with allowDuplicateXValues=true, addOrUpdate must add a new item for duplicate x-values
        // rather than overwrite. The failing unit test establishes the observable post-condition for this exact input:
        // count==2, Y values preserved in order [1.0, 2.0]. A "fix" that only suppresses the throw or skips insertion
        // would violate this observable state.
        if (anchor.getItemCount() != 2
                || !numEquals(anchor.getY(0), 1.0)
                || !numEquals(anchor.getY(1), 2.0)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: duplicate addOrUpdate must retain both items input=x=1.0,y=[1.0,2.0] lhs=count="
                    + anchor.getItemCount() + ",y0=" + anchor.getY(0) + ",y1=" + anchor.getY(1)
                    + " rhs=count=2,y0=1.0,y1=2.0");
        }

        // Sibling-agreement check on the anchor input using only real library calls.
        // With allowDuplicateXValues=true, addOrUpdate(Number, Number) is on the "add new item" path for every call,
        // so applying the same operation sequence via add(double, double) must yield an equal series.
        XYSeries anchorPeer = new XYSeries("Series", true, true);
        try {
            anchorPeer.add(1.0, 1.0);
            anchorPeer.add(1.0, 2.0);
            if (!anchor.equals(anchorPeer) || !anchorPeer.equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate sequence must equal add sequence input=x=1.0,y=[1.0,2.0] lhs="
                        + anchor + " rhs=" + anchorPeer);
            }
            if (anchor.hashCode() != anchorPeer.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hashCode input=x=1.0,y=[1.0,2.0] lhs="
                        + anchor.hashCode() + " rhs=" + anchorPeer.hashCode());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            return;
        }

        // EXPLORE: varied valid duplicate-x sequences with surrounding content.
        // Root cause property: autoSort=true, allowDuplicateXValues=true, duplicate x inserted through addOrUpdate.
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        int totalOps = data.consumeInt(2, 8);
        int duplicateOps = data.consumeInt(2, totalOps);
        double duplicateX = data.consumeInt(-1000, 1000);

        int maxCount = data.consumeInt(1, 12);
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getMaximumItemCount() != viaAdd.getMaximumItemCount()) {
            throw new RuntimeException("[oracle:maxcount-sync] metamorphic violation: paired series must report same maximumItemCount input=max="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }
        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:explore-autosort] metamorphic violation: constructor/getAutoSort disagreement input=true lhs="
                    + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
        }

        Double[] xs = new Double[totalOps];
        Double[] ys = new Double[totalOps];

        for (int i = 0; i < duplicateOps; i++) {
            xs[i] = Double.valueOf(duplicateX);
            ys[i] = Double.valueOf(data.consumeInt(-1000, 1000));
        }
        for (int i = duplicateOps; i < totalOps; i++) {
            int offset = (i - duplicateOps) + 1;
            int signedOffset = data.consumeBoolean() ? offset : -offset;
            xs[i] = Double.valueOf(duplicateX + signedOffset);
            ys[i] = Double.valueOf(data.consumeInt(-1000, 1000));
        }

        for (int i = totalOps - 1; i > 0; i--) {
            int j = data.consumeInt(0, i);
            Double tx = xs[i];
            xs[i] = xs[j];
            xs[j] = tx;
            Double ty = ys[i];
            ys[i] = ys[j];
            ys[j] = ty;
        }

        for (int i = 0; i < totalOps; i++) {
            try {
                viaAddOrUpdate.addOrUpdate(xs[i], ys[i]);
                viaAdd.add(xs[i].doubleValue(), ys[i].doubleValue());
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            } catch (Error t) {
                return;
            }

            // Contract/oracle: same key, same autoSort, same allowDuplicateXValues, same maximumItemCount and
            // same valid duplicate-permitting inserts => states must agree. This uses real API observables
            // (equals/hashCode/getters) and catches silent wrong-state "fixes".
            try {
                if (viaAddOrUpdate.getItemCount() != viaAdd.getItemCount()) {
                    throw new RuntimeException("[oracle:itemcount] metamorphic violation: addOrUpdate and add must retain same item count input=step="
                            + i + ",x=" + xs[i] + ",y=" + ys[i] + " lhs=" + viaAddOrUpdate.getItemCount() + " rhs=" + viaAdd.getItemCount());
                }
                if (!viaAddOrUpdate.equals(viaAdd) || !viaAdd.equals(viaAddOrUpdate)) {
                    throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate and add must produce equal series for duplicate-permitted input=step="
                            + i + ",x=" + xs[i] + ",y=" + ys[i] + " lhs=" + viaAddOrUpdate + " rhs=" + viaAdd);
                }
                if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
                    throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=step="
                            + i + ",x=" + xs[i] + ",y=" + ys[i] + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
                }
                if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
                    throw new RuntimeException("[oracle:maxcount] metamorphic violation: setMaximumItemCount must be reported consistently input=max="
                            + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
                }
                if (viaAddOrUpdate.getAutoSort() != viaAdd.getAutoSort()) {
                    throw new RuntimeException("[oracle:autosort-agree] metamorphic violation: paired series must report same autoSort input=step="
                            + i + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                throw t;
            }
        }
    }

    private static boolean numEquals(Number n, double expected) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(expected);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IndexOutOfBoundsException: Index: -1, Size: 1

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **29/29 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov; 29 raw fuzz log(s)**
<details><summary>▸ detail (~14,493 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 323}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 319}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 319}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 324}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 323}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 326}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 320}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 321}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 324}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 324}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 324}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 323}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 323}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 323}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 325}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 323}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 324}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 320}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/002_patch1-Chart-5-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
