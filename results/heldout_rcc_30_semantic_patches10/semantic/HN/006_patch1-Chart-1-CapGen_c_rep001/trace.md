# Pipeline trace — Chart-1

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
+++ /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
@@ -1794,7 +1794,7 @@
         }
         int index = this.plot.getIndexOf(this);
         CategoryDataset dataset = this.plot.getDataset(index);
-        if (dataset != null) {
+        if (dataset == null) {
             return result;
         }
         int seriesCount = dataset.getRowCount();
```

139 sequential steps — 35 LLM calls, 104 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests',  (~1,181 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests', test_method='test2947660', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/tests/org/jfree/chart/renderer/category/junit/AbstractCategoryItemRendererTests.java', method_source='    public void test2947660() {\\n        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();\\n        assertNotNull(r.getLegendItems());\\n        assertEquals(0, r.getLegendItems().getItemCount());\\n\\n        DefaultCategoryDataset dataset = new DefaultCategoryDataset();\\n        CategoryPlot plot = new CategoryPlot();\\n        plot.setDataset(dataset);\\n        plot.setRenderer(r);\\n        assertEquals(0, r.getLegendItems().getItemCount());\\n\\n        dataset.addValue(1.0, \"S1\", \"C1\");\\n        LegendItemCollection lic = r.getLegendItems();\\n        assertEquals(1, lic.getItemCount());\\n        assertEquals(\"S1\", lic.get(0).getLabel());\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests::test2947660",
    "failure_message": "--- org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests::test2947660\njunit.framework.AssertionFailedError: expected:<1> but was:<0>",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~33,921 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java"
  ],
  "patch_text": "--- /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java\n+++ /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java\n@@ -1794,7 +1794,7 @@\n         }\n         int index = this.plot.getIndexOf(this);\n         CategoryDataset dataset = this.plot.getDataset(index);\n-        if (dataset != null) {\n+        if (dataset == null) {\n             return result;\n         }\n         int seriesCount = dataset.getRowCount();\n",
  "functions": [
    {
      "func_name": "getLegendItems",
      "func_signature": "public LegendItemCollection getLegendItems()",
      "func_source": "    public LegendItemCollection getLegendItems() {\n        LegendItemCollection result = new LegendItemCollection();\n        if (this.plot == null) {\n            return result;\n        }\n        int index = this.plot.getIndexOf(this);\n        CategoryDataset dataset = this.plot.getDataset(index);\n        if (dataset != null) {\n            return result;\n        }\n        int seriesCount = dataset.getRowCount();\n        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {\n            for (int i = 0; i < seriesCount; i++) {\n                if (isSeriesVisibleInLegend(i)) {\n                    LegendItem item = getLegendItem(index, i);\n                    if (item != null) {\n                        result.add(item);\n                    }\n                }\n            }\n        }\n        else {\n            for (int i = seriesCount - 1; i >= 0; i--) {\n                if (isSeriesVisibleInLegend(i)) {\n                    LegendItem item = getLegendItem(index, i);\n                    if (item != null) {\n                        result.add(item);\n                    }\n                }\n            }\n        }\n        return result;\n    }",
      "func_class": "AbstractCategoryItemRenderer",
      "func_class_fq": "org.jfree.chart.renderer.category.AbstractCategoryItemRenderer",
      "func_param_types": [],
      "fi_name": "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "overload_types": [
        []
      ],
      "xrefs": [],
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.LegendItemCollection].<init>()"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.plot.CategoryPlot].getIndexOf()"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.plot.CategoryPlot].getDataset(int)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.data.category.CategoryDataset].getRowCount()"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.plot.CategoryPlot].getRowRenderingOrder()"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.util.SortOrder].equals(org.jfree.chart.util.SortOrder)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].isSeriesVisibleInLegend(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].isSeriesVisibleInLegend(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)"
        ],
        [
          "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
          "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)"
        ],
        [
          "[org.jfree.chart.LegendItemCollection].<init>()",
          "[java.util.ArrayList].<init>()"
        ],
        [
          "[org.jfree.chart.plot.CategoryPlot].getDataset(int)",
          "[org.jfree.chart.util.ObjectList].size()"
        ],
        [
          "[org.jfree.chart.plot.CategoryPlot].getDataset(int)",
          "[org.jfree.chart.util.ObjectList].get(int)"
        ],
        [
          "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)",
          "[java.util.List].add(org.jfree.chart.LegendItem)"
        ],
        [
          "[org.jfree.chart.util.ObjectList].get(int)",
          "[org.jfree.chart.util.AbstractObjectList].get(int)"
        ]
      ],
      "reachable_depth": {
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()": 0,
        "[org.jfree.chart.LegendItemCollection].<init>()": 1,
        "[org.jfree.chart.plot.CategoryPlot].getIndexOf()": 1,
        "[org.jfree.chart.plot.CategoryPlot].getDataset(int)": 1,
        "[org.jfree.data.category.CategoryDataset].getRowCount()": 1,
        "[org.jfree.chart.plot.CategoryPlot].getRowRenderingOrder()": 1,
        "[org.jfree.chart.util.SortOrder].equals(org.jfree.chart.util.SortOrder)": 1,
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].isSeriesVisibleInLegend(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)": 1,
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)": 1,
        "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)": 1,
        "[java.util.ArrayList].<init>()": 2,
        "[org.jfree.chart.util.ObjectList].size()": 2,
        "[org.jfree.chart.util.ObjectList].get(int)": 2,
        "[java.util.List].add(org.jfree.chart.LegendItem)": 2,
        "[org.jfree.chart.util.AbstractObjectList].get(int)": 3
      },
      "reachable": [
        "[org.jfree.chart.LegendItemCollection].<init>()",
        "[org.jfree.chart.plot.CategoryPlot].getIndexOf()",
        "[org.jfree.chart.plot.CategoryPlot].getDataset(int)",
        "[org.jfree.data.category.CategoryDataset].getRowCount()",
        "[org.jfree.chart.plot.CategoryPlot].getRowRenderingOrder()",
        "[org.jfree.chart.util.SortOrder].equals(org.jfree.chart.util.SortOrder)",
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].isSeriesVisibleInLegend(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)",
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)",
        "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)",
        "[java.util.ArrayList].<init>()",
        "[org.jfree.chart.util.ObjectList].size()",
        "[org.jfree.chart.util.ObjectList].get(int)",
        "[java.util.List].add(org.jfree.chart.LegendItem)",
        "[org.jfree.chart.util.AbstractObjectList].get(int)",
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].equals(Object)",
        "[org.jfree.chart.plot.CategoryPlot].getIndexOf(org.jfree.chart.renderer.category.CategoryItemRenderer)",
        "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,int)",
        "[org.jfree.chart.renderer.AbstractRenderer].isSeriesVisibleInLegend(int)"
      ],
      "related_callees": [
        {
          "name": "getRowCount",
          "source_file": "AbstractCategoryItemRenderer.java",
          "signature": "public int getRowCount()",
          "source": "    public int getRowCount() {\n        return this.rowCount;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "equals",
          "source_file": "AbstractCategoryItemRenderer.java",
          "signature": "public boolean equals(Object obj)",
          "source": "    public boolean equals(Object obj) {\n\n        if (obj == this) {\n            return true;\n        }\n        if (!(obj instanceof AbstractCategoryItemRenderer)) {\n            return false;\n        }\n        AbstractCategoryItemRenderer that = (AbstractCategoryItemRenderer) obj;\n\n        if (!ObjectUtilities.equal(this.itemLabelGeneratorList,\n                that.itemLabelGeneratorList)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.baseItemLabelGenerator,\n                that.baseItemLabelGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.toolTipGeneratorList,\n                that.toolTipGeneratorList)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.baseToolTipGenerator,\n                that.baseToolTipGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.urlGeneratorList,\n                that.urlGeneratorList)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.baseURLGenerator,\n                that.baseURLGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.legendItemLabelGenerator,\n                that.legendItemLabelGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.legendItemToolTipGenerator,\n                that.legendItemToolTipGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.legendItemURLGenerator,\n                that.legendItemURLGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.backgroundAnnotations,\n                that.backgroundAnnotations)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.foregroundAnnotations,\n                that.foregroundAnnotations)) {\n            return false;\n        }\n        return super.equals(obj);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "AbstractCategoryItemRenderer.java",
              "    public boolean equals(Object obj) {\n\n        if (obj == this) {\n            return true;\n        }\n        if (!(obj instanceof AbstractCategoryItemRenderer)) {\n            return false;\n        }\n        AbstractCategoryItemRenderer that = (AbstractCategoryItemRenderer) obj;\n\n        if (!ObjectUtilities.equal(this.itemLabelGeneratorList,\n                that.itemLabelGeneratorList)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.baseItemLabelGenerator,\n                that.baseItemLabelGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.toolTipGeneratorList,\n                that.toolTipGeneratorList)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.baseToolTipGenerator,\n                that.baseToolTipGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.urlGeneratorList,\n                that.urlGeneratorList)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.baseURLGenerator,\n                that.baseURLGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.legendItemLabelGenerator,\n                that.legendItemLabelGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.legendItemToolTipGenerator,\n                that.legendItemToolTipGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.legendItemURLGenerator,\n                that.legendItemURLGenerator)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.backgroundAnnotations,\n                that.backgroundAnnotations)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.foregroundAnnotations,\n                that.foregroundAnnotations)) {\n            return false;\n        }\n        return super.equals(obj);\n    }"
            ],
            [
              "AreaRenderer.java",
              "    public boolean equals(Object obj) {\n        if (obj == this) {\n            return true;\n        }\n        if (!(obj instanceof AreaRenderer)) {\n            return false;\n        }\n        AreaRenderer that = (AreaRenderer) obj;\n        if (!this.endType.equals(that.endType)) {\n            return false;\n        }\n        return super.equals(obj);\n    }"
            ],
            [
              "BarRenderer.java",
              "    public boolean equals(Object obj) {\n        if (obj == this) {\n            return true;\n        }\n        if (!(obj instanceof BarRenderer)) {\n            return false;\n        }\n        BarRenderer that = (BarRenderer) obj;\n        if (this.base != that.base) {\n            return false;\n        }\n        if (this.itemMargin != that.itemMargin) {\n            return false;\n        }\n        if (this.drawBarOutline != that.drawBarOutline) {\n            return false;\n        }\n        if (this.maximumBarWidth != that.maximumBarWidth) {\n            return false;\n        }\n        if (this.minimumBarLength != that.minimumBarLength) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.gradientPaintTransformer,\n                that.gradientPaintTransformer)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.positiveItemLabelPositionFallback,\n            that.positiveItemLabelPositionFallback)) {\n            return false;\n        }\n        if (!ObjectUtilities.equal(this.negativeItemLabelPositionFallback,\n            that.negativeItemLabelPositionFallback)) {\n            return false;\n        }\n        if (!this.barPainter.equals(that.barPainter)) {\n            return false;\n        }\n        if (this.shadowsVisible != that.shadowsVisible) {\n            return false;\n        }\n        if (!PaintUtilities.equal(this.shadowPaint, that.shadowPaint)) {\n            return false;\n        }\n        if (this.shadowXOffset != that.shadowXOffset) {\n            return false;\n        }\n        if (this.shadowYOffset != that.shadowYOffset) {\n            return false;\n        }\n        return super.equals(obj);\n    }"
            ],
            [
              "BarRenderer3D.java",
              "    public boolean equals(Object obj) {\n        if (obj == this) {\n            return true;\n        }\n        if (!(obj instanceof BarRenderer3D)) {\n            return false;\n        }\n        BarRenderer3D that = (BarRenderer3D) obj;\n        if (this.xOffset != that.xOffset) {\n            return false;\n        }\n        if (this.yOffset != that.yOffset) {\n            return false;\n        }\n        if (!PaintUtilities.equal(this.wallPaint, that.wallPaint)) {\n            return false;\n        }\n        return super.equals(obj);\n    }"
            ]
          ]
        },
        {
          "name": "isSeriesVisibleInLegend",
          "source_file": "CategoryItemRenderer.java",
          "signature": "public boolean isSeriesVisibleInLegend(int series)",
          "source": "    public boolean isSeriesVisibleInLegend(int series);",
          "is_abstract": true,
          "impls": []
        },
        {
          "name": "getLegendItem",
          "source_file": "CategoryItemRenderer.java",
          "signature": "public LegendItem getLegendItem(int datasetIndex, int series)",
          "source": "    public LegendItem getLegendItem(int datasetIndex, int series);",
          "is_abstract": true,
          "impls": [
            [
              "AbstractCategoryItemRenderer.java",
              "    public LegendItem getLegendItem(int datasetIndex, int series) {\n\n        CategoryPlot p = getPlot();\n        if (p == null) {\n            return null;\n        }\n\n        // check that a legend item needs to be displayed...\n        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {\n            return null;\n        }\n\n        CategoryDataset dataset = p.getDataset(datasetIndex);\n        String label = this.legendItemLabelGenerator.generateLabel(dataset,\n                series);\n        String description = label;\n        String toolTipText = null;\n        if (this.legendItemToolTipGenerator != null) {\n            toolTipText = this.legendItemToolTipGenerator.generateLabel(\n                    dataset, series);\n        }\n        String urlText = null;\n        if (this.legendItemURLGenerator != null) {\n            urlText = this.legendItemURLGenerator.generateLabel(dataset,\n                    series);\n        }\n        Shape shape = lookupLegendShape(series);\n        Paint paint = lookupSeriesPaint(series);\n        Paint outlinePaint = lookupSeriesOutlinePaint(series);\n        Stroke outlineStroke = lookupSeriesOutlineStroke(series);\n\n        LegendItem item = new LegendItem(label, description, toolTipText,\n                urlText, shape, paint, outlineStroke, outlinePaint);\n        item.setLabelFont(lookupLegendTextFont(series));\n        Paint labelPaint = lookupLegendTextPaint(series);\n        if (labelPaint != null) {\n            item.setLabelPaint(labelPaint);\n        }\n        item.setSeriesKey(dataset.getRowKey(series));\n        item.setSeriesIndex(series);\n        item.setDataset(dataset);\n        item.setDatasetIndex(datasetIndex);\n        return item;\n    }"
            ],
            [
              "AreaRenderer.java",
              "    public LegendItem getLegendItem(int datasetIndex, int series) {\n\n        // if there is no plot, there is no dataset to access...\n        CategoryPlot cp = getPlot();\n        if (cp == null) {\n            return null;\n        }\n\n        // check that a legend item needs to be displayed...\n        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {\n            return null;\n        }\n\n        CategoryDataset dataset = cp.getDataset(datasetIndex);\n        String label = getLegendItemLabelGenerator().generateLabel(dataset,\n                series);\n        String description = label;\n        String toolTipText = null;\n        if (getLegendItemToolTipGenerator() != null) {\n            toolTipText = getLegendItemToolTipGenerator().generateLabel(\n                    dataset, series);\n        }\n        String urlText = null;\n        if (getLegendItemURLGenerator() != null) {\n            urlText = getLegendItemURLGenerator().generateLabel(dataset,\n                    series);\n        }\n        Shape shape = lookupLegendShape(series);\n        Paint paint = lookupSeriesPaint(series);\n        Paint outlinePaint = lookupSeriesOutlinePaint(series);\n        Stroke outlineStroke = lookupSeriesOutlineStroke(series);\n\n        LegendItem result = new LegendItem(label, description, toolTipText,\n                urlText, shape, paint, outlineStroke, outlinePaint);\n        result.setLabelFont(lookupLegendTextFont(series));\n        Paint labelPaint = lookupLegendTextPaint(series);\n        if (labelPaint != null) {\n            result.setLabelPaint(labelPaint);\n        }\n        result.setDataset(dataset);\n        result.setDatasetIndex(datasetIndex);\n        result.setSeriesKey(dataset.getRowKey(series));\n        result.setSeriesIndex(series);\n        return result;\n\n    }"
            ],
            [
              "BarRenderer.java",
              "    public LegendItem getLegendItem(int datasetIndex, int series) {\n\n        CategoryPlot cp = getPlot();\n        if (cp == null) {\n            return null;\n        }\n\n        // check that a legend item needs to be displayed...\n        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {\n            return null;\n        }\n\n        CategoryDataset dataset = cp.getDataset(datasetIndex);\n        String label = getLegendItemLabelGenerator().generateLabel(dataset,\n                series);\n        String description = label;\n        String toolTipText = null;\n        if (getLegendItemToolTipGenerator() != null) {\n            toolTipText = getLegendItemToolTipGenerator().generateLabel(\n                    dataset, series);\n        }\n        String urlText = null;\n        if (getLegendItemURLGenerator() != null) {\n            urlText = getLegendItemURLGenerator().generateLabel(dataset,\n                    series);\n        }\n        Shape shape = lookupLegendShape(series);\n        Paint paint = lookupSeriesPaint(series);\n        Paint outlinePaint = lookupSeriesOutlinePaint(series);\n        Stroke outlineStroke = lookupSeriesOutlineStroke(series);\n\n        LegendItem result = new LegendItem(label, description, toolTipText,\n                urlText, true, shape, true, paint, isDrawBarOutline(),\n                outlinePaint, outlineStroke, false, new Line2D.Float(),\n                new BasicStroke(1.0f), Color.black);\n        result.setLabelFont(lookupLegendTextFont(series));\n        Paint labelPaint = lookupLegendTextPaint(series);\n        if (labelPaint != null) {\n            result.setLabelPaint(labelPaint);\n        }\n        result.setDataset(dataset);\n        result.setDatasetIndex(datasetIndex);\n        result.setSeriesKey(dataset.getRowKey(series));\n        result.setSeriesIndex(series);\n        if (this.gradientPaintTransformer != null) {\n            result.setFillPaintTransformer(this.gradientPaintTransformer);\n        }\n        return result;\n    }"
            ],
            [
              "BoxAndWhiskerRenderer.java",
              "    public LegendItem getLegendItem(int datasetIndex, int series) {\n\n        CategoryPlot cp = getPlot();\n        if (cp == null) {\n            return null;\n        }\n\n        // check that a legend item needs to be displayed...\n        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {\n            return null;\n        }\n\n        CategoryDataset dataset = cp.getDataset(datasetIndex);\n        String label = getLegendItemLabelGenerator().generateLabel(dataset,\n                series);\n        String description = label;\n        String toolTipText = null;\n        if (getLegendItemToolTipGenerator() != null) {\n            toolTipText = getLegendItemToolTipGenerator().generateLabel(\n                    dataset, series);\n        }\n        String urlText = null;\n        if (getLegendItemURLGenerator() != null) {\n            urlText = getLegendItemURLGenerator().generateLabel(dataset,\n                    series);\n        }\n        Shape shape = lookupLegendShape(series);\n        Paint paint = lookupSeriesPaint(series);\n        Paint outlinePaint = lookupSeriesOutlinePaint(series);\n        Stroke outlineStroke = lookupSeriesOutlineStroke(series);\n        LegendItem result = new LegendItem(label, description, toolTipText,\n                urlText, shape, paint, outlineStroke, outlinePaint);\n        result.setLabelFont(lookupLegendTextFont(series));\n        Paint labelPaint = lookupLegendTextPaint(series);\n        if (labelPaint != null) {\n            result.setLabelPaint(labelPaint);\n        }\n        result.setDataset(dataset);\n        result.setDatasetIndex(datasetIndex);\n        result.setSeriesKey(dataset.getRowKey(series));\n        result.setSeriesIndex(series);\n        return result;\n\n    }"
            ]
          ]
        }
      ],
      "field_siblings": [
        {
          "name": "getPlot",
          "signature": "public CategoryPlot getPlot()",
          "shared_fields": [
            "plot"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Returns the plot that the renderer has been assigned to (where <code>null</code> indicates that the renderer is not currently assigned to a plot). @return The plot (possibly <code>null</code>). @see #setPlot(CategoryPlot)"
        },
        {
          "name": "setPlot",
          "signature": "public void setPlot(CategoryPlot plot)",
          "shared_fields": [
            "plot"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Sets the plot that the renderer has been assigned to.  This method is usually called by the {@link CategoryPlot}, in normal usage you shouldn't need to call this method directly. @param plot  the plot (<code>null</code> not permitted). @see #getPlot()"
        },
        {
          "name": "updateCrosshairValues",
          "signature": "protected void updateCrosshairValues(CategoryCrosshairState crosshairState, Comparable rowKey, Comparable columnKey, double value, int datasetIndex, double transX, double transY, PlotOrientation orientation)",
          "shared_fields": [
            "plot"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Considers the current (x, y) coordinate and updates the crosshair point if it meets the criteria (usually means the (x, y) coordinate is the closest to the anchor point so far). @param crosshairState  the crosshair state (<code>null</code> permitted, but the method does nothing in that case). @param rowKey  the row key…"
        },
        {
          "name": "drawAnnotations",
          "signature": "public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea, CategoryAxis domainAxis, ValueAxis rangeAxis, Layer layer, PlotRenderingInfo info)",
          "shared_fields": [
            "plot"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Draws all the annotations for the specified layer. @param g2  the graphics device. @param dataArea  the data area. @param domainAxis  the domain axis. @param rangeAxis  the range axis. @param layer  the layer. @param info  the plot rendering info. @since 1.2.0"
        }
      ]
    }
  ],
  "package": "org.jfree.chart.renderer.category",
  "root_cause_reachable": [
    "LegendItemCollection.<init>",
    "CategoryPlot.getIndexOf()",
    "CategoryPlot.getDataset",
    "CategoryPlot.getRowRenderingOrder",
    "SortOrder.equals",
    "AbstractCategoryItemRenderer.isSeriesVisibleInLegend",
    "AbstractCategoryItemRenderer.getLegendItem",
    "LegendItemCollection.add",
    "ObjectList.size",
    "ObjectList.get",
    "AbstractObjectList.get",
    "AbstractCategoryItemRenderer.equals",
    "CategoryPlot.getIndexOf(CategoryItemRenderer)",
    "AbstractRenderer.isSeriesVisibleInLegend"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.awt.AlphaComposite;",
    "import java.awt.Composite;",
    "import java.awt.Font;",
    "import java.awt.GradientPaint;",
    "import java.awt.Graphics2D;",
    "import java.awt.Paint;",
    "import java.awt.Rectangle;",
    "import java.awt.Shape;",
    "import java.awt.Stroke;",
    "import java.awt.geom.Ellipse2D;",
    "import java.awt.geom.Line2D;",
    "import java.awt.geom.Point2D;",
    "import java.awt.geom.Rectangle2D;",
    "import java.io.Serializable;",
    "import java.util.ArrayList;",
    "import java.util.Iterator;",
    "import java.util.List;",
    "import org.jfree.chart.ChartRenderingInfo;",
    "import org.jfree.chart.LegendItem;",
    "import org.jfree.chart.LegendItemCollection;",
    "import org.jfree.chart.RenderingSource;",
    "import org.jfree.chart.annotations.CategoryAnnotation;",
    "import org.jfree.chart.axis.CategoryAxis;",
    "import org.jfree.chart.axis.ValueAxis;",
    "import org.jfree.chart.entity.CategoryItemEntity;",
    "import org.jfree.chart.entity.EntityCollection;",
    "import org.jfree.chart.event.RendererChangeEvent;",
    "import org.jfree.chart.labels.CategoryItemLabelGenerator;",
    "import org.jfree.chart.labels.CategorySeriesLabelGenerator;",
    "import org.jfree.chart.labels.CategoryToolTipGenerator;",
    "import org.jfree.chart.labels.ItemLabelPosition;",
    "import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;",
    "import org.jfree.chart.plot.CategoryCrosshairState;",
    "import org.jfree.chart.plot.CategoryMarker;",
    "import org.jfree.chart.plot.CategoryPlot;",
    "import org.jfree.chart.plot.DrawingSupplier;",
    "import org.jfree.chart.plot.IntervalMarker;",
    "import org.jfree.chart.plot.Marker;",
    "import org.jfree.chart.plot.PlotOrientation;",
    "import org.jfree.chart.plot.PlotRenderingInfo;",
    "import org.jfree.chart.plot.ValueMarker;",
    "import org.jfree.chart.renderer.AbstractRenderer;",
    "import org.jfree.chart.text.TextUtilities;",
    "import org.jfree.chart.urls.CategoryURLGenerator;",
    "import org.jfree.chart.util.GradientPaintTransformer;",
    "import org.jfree.chart.util.Layer;",
    "import org.jfree.chart.util.LengthAdjustmentType;",
    "import org.jfree.chart.util.ObjectList;",
    "import org.jfree.chart.util.ObjectUtilities;",
    "import org.jfree.chart.util.PublicCloneable;",
    "import org.jfree.chart.util.RectangleAnchor;",
    "import org.jfree.chart.util.RectangleEdge;",
    "import org.jfree.chart.util.RectangleInsets;",
    "import org.jfree.chart.util.SortOrder;",
    "import org.jfree.data.Range;",
    "import org.jfree.data.category.CategoryDataset;",
    "import org.jfree.data.category.CategoryDatasetSelectionState;",
    "import org.jfree.data.category.SelectableCategoryDataset;",
    "import org.jfree.data.general.DatasetUtilities;"
  ],
  "reachable_edges": [
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.LegendItemCollection].<init>()"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.plot.CategoryPlot].getIndexOf()"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.plot.CategoryPlot].getDataset(int)"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.data.category.CategoryDataset].getRowCount()"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.plot.CategoryPlot].getRowRenderingOrder()"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.util.SortOrder].equals(org.jfree.chart.util.SortOrder)"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].isSeriesVisibleInLegend(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)"
    ],
    [
      "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()",
      "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)"
    ],
    [
      "[org.jfree.chart.LegendItemCollection].<init>()",
      "[java.util.ArrayList].<init>()"
    ],
    [
      "[org.jfree.chart.plot.CategoryPlot].getDataset(int)",
      "[org.jfree.chart.util.ObjectList].size()"
    ],
    [
      "[org.jfree.chart.plot.CategoryPlot].getDataset(int)",
      "[org.jfree.chart.util.ObjectList].get(int)"
    ],
    [
      "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)",
      "[java.util.List].add(org.jfree.chart.LegendItem)"
    ],
    [
      "[org.jfree.chart.util.ObjectList].get(int)",
      "[org.jfree.chart.util.AbstractObjectList].get(int)"
    ]
  ],
  "reachable_depth": {
    "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItems()": 0,
    "[org.jfree.chart.LegendItemCollection].<init>()": 1,
    "[org.jfree.chart.plot.CategoryPlot].getIndexOf()": 1,
    "[org.jfree.chart.plot.CategoryPlot].getDataset(int)": 1,
    "[org.jfree.data.category.CategoryDataset].getRowCount()": 1,
    "[org.jfree.chart.plot.CategoryPlot].getRowRenderingOrder()": 1,
    "[org.jfree.chart.util.SortOrder].equals(org.jfree.chart.util.SortOrder)": 1,
    "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].isSeriesVisibleInLegend(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)": 1,
    "[org.jfree.chart.renderer.category.AbstractCategoryItemRenderer].getLegendItem(int,org.jfree.chart.renderer.category.AbstractCategoryItemRenderer)": 1,
    "[org.jfree.chart.LegendItemCollection].add(org.jfree.chart.LegendItem)": 1,
    "[java.util.ArrayList].<init>()": 2,
    "[org.jfree.chart.util.ObjectList].size()": 2,
    "[org.jfree.chart.util.ObjectList].get(int)": 2,
    "[java.util.List].add(org.jfree.chart.LegendItem)": 2,
    "[org.jfree.chart.util.AbstractObjectList].get(int)": 3
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
<details><summary>▸ output — subtypes counted: ['AreaRenderer', 'BarRenderer', 'BarRenderer3D', 'BoxAndWhiskerRenderer', 'Categor (~504 chars)</summary>

**output:** 
```
subtypes counted: ['AreaRenderer', 'BarRenderer', 'BarRenderer3D', 'BoxAndWhiskerRenderer', 'CategoryStepRenderer', 'DefaultCategoryItemRenderer', 'GanttRenderer', 'GroupedStackedBarRenderer', 'IntervalBarRenderer', 'LayeredBarRenderer', 'LevelRenderer', 'LineAndShapeRenderer', 'LineRenderer3D', 'MinMaxCategoryRenderer', 'ScatterRenderer', 'StackedAreaRenderer', 'StackedBarRenderer', 'StackedBarRenderer3D', 'StatisticalBarRenderer', 'StatisticalLineAndShapeRenderer', 'WaterfallBarRenderer']
```

</details>
<details><summary>▸ detail (~544 chars)</summary>

- detail: {'patched_classes': ['AbstractCategoryItemRenderer'], 'subtypes': ['AreaRenderer', 'BarRenderer', 'BarRenderer3D', 'BoxAndWhiskerRenderer', 'CategoryStepRenderer', 'DefaultCategoryItemRenderer', 'GanttRenderer', 'GroupedStackedBarRenderer', 'IntervalBarRenderer', 'LayeredBarRenderer', 'LevelRenderer', 'LineAndShapeRenderer', 'LineRenderer3D', 'MinMaxCategoryRenderer', 'ScatterRenderer', 'StackedAreaRenderer', 'StackedBarRenderer', 'StackedBarRenderer3D', 'StatisticalBarRenderer', 'StatisticalLineAndShapeRenderer', 'WaterfallBarRenderer']}

</details>

---
## [4] ⚙️ synth · `probe-tier`
<details><summary>▸ output — documented exceptions: {'AbstractCategoryItemRenderer#clone': ['CloneNotSupportedException'], 'AreaR (~1,549 chars)</summary>

**output:** 
```
documented exceptions: {'AbstractCategoryItemRenderer#clone': ['CloneNotSupportedException'], 'AreaRenderer#clone': ['CloneNotSupportedException'], 'BarRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'BarRenderer#writeObject': ['IOException'], 'BarRenderer3D#readObject': ['ClassNotFoundException', 'IOException'], 'BarRenderer3D#writeObject': ['IOException'], 'BoxAndWhiskerRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'BoxAndWhiskerRenderer#writeObject': ['IOException'], 'GanttRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'GanttRenderer#writeObject': ['IOException'], 'LineAndShapeRenderer#clone': ['CloneNotSupportedException'], 'LineRenderer3D#readObject': ['ClassNotFoundException', 'IOException'], 'LineRenderer3D#writeObject': ['IOException'], 'MinMaxCategoryRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'MinMaxCategoryRenderer#writeObject': ['IOException'], 'ScatterRenderer#clone': ['CloneNotSupportedException'], 'ScatterRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'ScatterRenderer#writeObject': ['IOException'], 'StatisticalBarRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'StatisticalBarRenderer#writeObject': ['IOException'], 'StatisticalLineAndShapeRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'StatisticalLineAndShapeRenderer#writeObject': ['IOException'], 'WaterfallBarRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'WaterfallBarRenderer#writeObject': ['IOException']}
```

</details>
<details><summary>▸ detail (~1,533 chars)</summary>

- detail: {'documented': {'AbstractCategoryItemRenderer#clone': ['CloneNotSupportedException'], 'AreaRenderer#clone': ['CloneNotSupportedException'], 'BarRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'BarRenderer#writeObject': ['IOException'], 'BarRenderer3D#readObject': ['ClassNotFoundException', 'IOException'], 'BarRenderer3D#writeObject': ['IOException'], 'BoxAndWhiskerRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'BoxAndWhiskerRenderer#writeObject': ['IOException'], 'GanttRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'GanttRenderer#writeObject': ['IOException'], 'LineAndShapeRenderer#clone': ['CloneNotSupportedException'], 'LineRenderer3D#readObject': ['ClassNotFoundException', 'IOException'], 'LineRenderer3D#writeObject': ['IOException'], 'MinMaxCategoryRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'MinMaxCategoryRenderer#writeObject': ['IOException'], 'ScatterRenderer#clone': ['CloneNotSupportedException'], 'ScatterRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'ScatterRenderer#writeObject': ['IOException'], 'StatisticalBarRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'StatisticalBarRenderer#writeObject': ['IOException'], 'StatisticalLineAndShapeRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'StatisticalLineAndShapeRenderer#writeObject': ['IOException'], 'WaterfallBarRenderer#readObject': ['ClassNotFoundException', 'IOException'], 'WaterfallBarRenderer#writeObject': ['IOException']}}

</details>

---
## [5] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~75,525 chars)</summary>

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
// org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests::test2947660
    public void test2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        assertNotNull(r.getLegendItems());
        assertEquals(0, r.getLegendItems().getItemCount());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        assertEquals(0, r.getLegendItems().getItemCount());

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        assertEquals(1, lic.getItemCount());
        assertEquals("S1", lic.get(0).getLabel());
    }
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests::test2947660
// junit.framework.AssertionFailedError: expected:<1> but was:<0>
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addAnnotation(CategoryAnnotation annotation) / (CategoryAnnotation annotation, Layer layer)
  setBaseItemLabelGenerator(CategoryItemLabelGenerator generator) / (CategoryItemLabelGenerator generator, boolean notify)
  setBaseToolTipGenerator(CategoryToolTipGenerator generator) / (CategoryToolTipGenerator generator,
            boolean notify)
  setBaseURLGenerator(CategoryURLGenerator generator) / (CategoryURLGenerator generator,
            boolean notify)
  setSeriesItemLabelGenerator(int series,
            CategoryItemLabelGenerator generator) / (int series,
            CategoryItemLabelGenerator generator, boolean notify)
  setSeriesToolTipGenerator(int series,
            CategoryToolTipGenerator generator) / (int series,
            CategoryToolTipGenerator generator, boolean notify)
  setSeriesURLGenerator(int series,
            CategoryURLGenerator generator) / (int series,
            CategoryURLGenerator generator, boolean notify)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  draw* family: drawAnnotations, drawBackground, drawDomainLine, drawDomainMarker, drawOutline, drawRangeLine, drawRangeMarker
  get
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * {@link CategoryItemRenderer} you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() {
        this.itemLabelGeneratorList = new ObjectList();
        this.toolTipGeneratorList = new ObjectList();
        this.urlGeneratorList = new ObjectList();
        this.legendItemLabelGenerator
                = new StandardCategorySeriesLabelGenerator();
        this.backgroundAnnotations = new ArrayList();
        this.foregroundAnnotations = new ArrayList();
    }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { … }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { … }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the {@link CategoryPlot}, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { … }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { … }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { … }
     * Sets the item label generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { … }
     * Sets the item label generator for a series and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { … }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { … }
     * Sets the base item label generator and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { … }
     * Sets the base item label generator and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { … }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { … }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { … }
     * Sets the tool tip generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { … }
     * Sets the tool tip generator for a series and sends a
     * {@link org.jfree.chart.event.RendererChangeEvent} to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { … }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { … }
     * Sets the base tool tip generator and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { … }
     * Sets the base tool tip generator and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { … }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { … }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { … }
     * Sets the URL generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { … }
     * Sets the URL generator for a series and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { … }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the {@link #lookupSeriesPaint(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the {@link #lookupSeriesFillPaint(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the {@link #lookupSeriesOutlinePaint(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the {@link #lookupSeriesStroke(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the {@link #lookupSeriesOutlineStroke(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the {@link #lookupSeriesShape(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * {@link AbstractCategoryItemRenderer} class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the {@link CategoryPlot}, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a {@link RendererChangeEvent} to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a {@link RendererChangeEvent} to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a {@link RendererChangeEvent} to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * {@link #isSeriesVisibleInLegend(int)} method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -8168173757291644622L;
     * A storage structure for the data.
    private KeyedObjects2D data;
     * Creates a new (empty) dataset.
    public DefaultCategoryDataset() {
        this.data = new KeyedObjects2D();
        // FIXME: will need to remove this later, because it should be optional
        setSelectionState(this);
    }
     * Returns the number of rows in the table.
     * @return The row count.
     * @see #getColumnCount()
    public int getRowCount() { … }
     * Returns the number of columns in the table.
     * @return The column count.
     * @see #getRowCount()
    public int getColumnCount() { … }
     * Returns a value from the table.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The value (possibly <code>null</code>).
     * @see #addValue(Number, Comparable, Comparable)
     * @see #removeValue(Comparable, Comparable)
    public Number getValue(int row, int column) { … }
     * Returns the key for the specified row.
     * @param row  the row index (zero-based).
     * @return The row key.
     * @see #getRowIndex(Comparable)
     * @see #getRowKeys()
     * @see #getColumnKey(int)
    public Comparable getRowKey(int row) { … }
     * Returns the row index for a given key.
     * @param key  the row key (<code>null</code> not permitted).
     * @return The row index.
     * @see #getRowKey(int)
    public int getRowIndex(Comparable key) { … }
     * Returns the row keys.
     * @return The keys.
     * @see #getRowKey(int)
    public List getRowKeys() { … }
     * Returns a column key.
     * @param column  the column index (zero-based).
     * @return The column key.
     * @see #getColumnIndex(Comparable)
    public Comparable getColumnKey(int column) { … }
     * Returns the column index for a given key.
     * @param key  the column key (<code>null</code> not permitted).
     * @return The column index.
     * @see #getColumnKey(int)
    public int getColumnIndex(Comparable key) { … }
     * Returns the column keys.
     * @return The keys.
     * @see #getColumnKey(int)
    public List getColumnKeys() { … }
     * Returns the value for a pair of keys.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @return The value (possibly <code>null</code>).
     * @throws UnknownKeyException if either key is not defined in the dataset.
     * @see #addValue(Number, Comparable, Comparable)
    public Number getValue(Comparable rowKey, Comparable columnKey) { … }
     * Adds a value to the table.  Performs the same function as setValue().
     * @param value  the value.
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @see #getValue(Comparable, Comparable)
     * @see #removeValue(Comparable, Comparable)
    public void addValue(Number value, Comparable rowKey, Comparable columnKey) { … }
     * Adds a value to the table.
     * @param value  the value.
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @see #getValue(Comparable, Comparable)
    public void addValue(double value, Comparable rowKey, Comparable columnKey) { … }
     * Adds or updates a value in the table and sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     * @param value  the value (<code>null</code> permitted).
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @see #getValue(Comparable, Comparable)
    public void setValue(Number value, Comparable rowKey, Comparable columnKey) { … }
     * Adds or updates a value in the table and sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     * @param value  the value.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @see #getValue(Comparable, Comparable)
    public void setValue(double value, Comparable rowKey, Comparable columnKey) { … }
     * Adds the specified value to an existing value in the dataset (if the
     * existing value is <code>null</code>, it is treated as if it were 0.0).
     * @param value  the value.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @throws UnknownKeyException if either key is not defined in the dataset.
    public void incrementValue(double value, Comparable rowKey, Comparable columnKey) { … }
     * Removes a value from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @see #addValue(Number, Comparable, Comparable)
     * @throws UnknownKeyException if either key is not in the dataset.
    public void removeValue(Comparable rowKey, Comparable columnKey) { … }
     * Removes a row from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * @param rowIndex  the row index.
     * @see #removeColumn(int)
    public void removeRow(int rowIndex) { … }
     * Removes a row from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * @param rowKey  the row key.
     * @see #removeColumn(Comparable)
    public void removeRow(Comparable rowKey) { … }
    public boolean isSelected(int row, int column) { … }
    public void clearSelection() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="CategoryPlot" role="test-subject">
     * A general plotting class that uses data from a {@link CategoryDataset} and
     * renders each data item using a {@link CategoryItemRenderer}.
public class CategoryPlot extends Plot implements ValueAxisPlot, Pannable,
        Selectable, Zoomable, AnnotationChangeListener, RendererChangeListener,
        Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -3537691700434728188L;
     * The default visibility of the grid lines plotted against the domain
     * axis.
    public static final boolean DEFAULT_DOMAIN_GRIDLINES_VISIBLE = false;
     * The default visibility of the grid lines plotted against the range
     * axis.
    public static final boolean DEFAULT_RANGE_GRIDLINES_VISIBLE = true;
     * The default grid line stroke.
    public static final Stroke DEFAULT_GRIDLINE_STROKE = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] { … }
     * The default grid line paint.
    public static final Paint DEFAULT_GRIDLINE_PAINT = Color.WHITE;
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default crosshair visibility.
     * @since 1.0.5
    public static final boolean DEFAULT_CROSSHAIR_VISIBLE = false;
     * The default crosshair stroke.
     * @since 1.0.5
    public static final Stroke DEFAULT_CROSSHAIR_STROKE = DEFAULT_GRIDLINE_STROKE;
     * The default crosshair paint.
     * @since 1.0.5
    public static final Paint DEFAULT_CROSSHAIR_PAINT = Color.blue;
     * The resourceBundle for the localization.
    protected static ResourceBundle localizationResources = ResourceBundleWrapper.getBundle( "org.jfree.chart.plot.LocalizationBundle");
     * The plot orientation.
    private PlotOrientation orientation;
     * The offset between the data area and the axes.
    private RectangleInsets axisOffset;
     * Storage for the domain axes.
    private ObjectList domainAxes;
     * Storage for the domain axis locations.
    private ObjectList domainAxisLocations;
     * A flag that controls whether or not the shared domain axis is drawn
     * (only relevant when the plot is being used as a subplot).
    private boolean drawSharedDomainAxis;
     * Storage for the range axes.
    private ObjectList rangeAxes;
     * Storage for the range axis locations.
    private ObjectList rangeAxisLocations;
     * Storage for the datasets.
    private ObjectList datasets;
     * Storage for keys that map datasets to domain axes.
    private TreeMap datasetToDomainAxesMap;
     * Storage for keys that map datasets to range axes.
    private TreeMap datasetToRangeAxesMap;
     * Storage for the renderers.
    private ObjectList renderers;
     * The dataset rendering order.
    private DatasetRenderingOrder renderingOrder = DatasetRenderingOrder.REVERSE;
     * Controls the order in which the columns are traversed when rendering the
     * data items.
    private SortOrder columnRenderingOrder = SortOrder.ASCENDING;
     * Controls the order in which the rows are traversed when rendering the
     * data items.
    private SortOrder rowRenderingOrder = SortOrder.ASCENDING;
     * A flag that controls whether the grid-lines for the domain axis are
     * visible.
    private boolean domainGridlinesVisible;
     * The position of the domain gridlines relative to the category.
    private CategoryAnchor domainGridlinePosition;
     * The stroke used to draw the domain grid-lines.
    private transient Stroke domainGridlineStroke;
     * The paint used to draw the domain  grid-lines.
    private transient Paint domainGridlinePaint;
     * A flag that controls whether or not the zero baseline against the range
     * axis is visible.
     * @since 1.0.13
    private boolean rangeZeroBaselineVisible;
     * The stroke used for the zero baseline against the range axis.
     * @since 1.0.13
    private transient Stroke rangeZeroBaselineStroke;
     * The paint used for the zero baseline against the range axis.
     * @since 1.0.13
    private transient Paint rangeZeroBaselinePaint;
     * A flag that controls whether the grid-lines for the range axis are
     * visible.
    private boolean rangeGridlinesVisible;
     * The stroke used to draw the range axis grid-lines.
    private transient Stroke rangeGridlineStroke;
     * The paint used to draw the range axis grid-lines.
    private transient Paint rangeGridlinePaint;
     * A flag that controls whether or not gridlines are shown for the minor
     * tick values on the primary range axis.
     * @since 1.0.13
    private boolean rangeMinorGridlinesVisible;
     * The stroke used to draw the range minor grid-lines.
     * @since 1.0.13
    private transient Stroke rangeMinorGridlineStroke;
     * The paint used to draw the range minor grid-lines.
     * @since 1.0.13
    private transient Paint rangeMinorGridlinePaint;
     * The anchor value.
    private double anchorValue;
     * The index for the dataset that the crosshairs are linked to (this
     * determines which axes the crosshairs are plotted against).
     * @since 1.0.11
    private int crosshairDatasetIndex;
     * A flag that controls the visibility of the domain crosshair.
     * @since 1.0.11
    private boolean domainCrosshairVisible;
     * The row key for the crosshair point.
     * @since 1.0.11
    private Comparable domainCrosshairRowKey;
     * The column key for the crosshair point.
     * @since 1.0.11
    private Comparable domainCrosshairColumnKey;
     * The stroke used to draw the domain crosshair if it is visible.
     * @since 1.0.11
    private transient Stroke domainCrosshairStroke;
     * The paint used to draw the domain crosshair if it is visible.
     * @since 1.0.11
    private transient Paint domainCrosshairPaint;
     * A flag that controls whether or not a range crosshair is drawn.
    private boolean rangeCrosshairVisible;
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="LineAndShapeRenderer" role="test-subject">
     * A renderer that draws shapes for each data item, and lines between data
     * items (for use with the {@link CategoryPlot} class).
     * The example shown here is generated by the <code>LineChartDemo1.java</code>
     * program included in the JFreeChart Demo Collection:
     * <br><br>
     * <img src="../../../../../images/LineAndShapeRendererSample.png"
     * alt="LineAndShapeRendererSample.png" />
public class LineAndShapeRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -197749519869226398L;
     * A table of flags that control (per series) whether or not lines are
     * visible.
    private BooleanList seriesLinesVisible;
     * A flag indicating whether or not lines are drawn between non-null
     * points.
    private boolean baseLinesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * visible.
    private BooleanList seriesShapesVisible;
     * The default value returned by the getShapeVisible() method.
    private boolean baseShapesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * filled.
    private BooleanList seriesShapesFilled;
     * The default value returned by the getShapeFilled() method.
    private boolean baseShapesFilled;
     * A flag that controls whether the fill paint is used for filling
     * shapes.
    private boolean useFillPaint;
     * A flag that controls whether outlines are drawn for shapes.
    private boolean drawOutlines;
     * A flag that controls whether the outline paint is used for drawing shape
     * outlines - if not, the regular series paint is used.
    private boolean useOutlinePaint;
     * A flag that controls whether or not the x-position for each item is
     * offset within the category according to the series.
     * @since 1.0.7
    private boolean useSeriesOffset;
     * The item margin used for series offsetting - this allows the positioning
     * to match the bar positions of the {@link BarRenderer} class.
     * @since 1.0.7
    private double itemMargin;
     * Creates a renderer with both lines and shapes visible by default.
    public LineAndShapeRenderer() {
        this(true, true);
    }
     * Creates a new renderer with lines and/or shapes visible.
     * @param lines  draw lines?
     * @param shapes  draw shapes?
    public LineAndShapeRenderer(boolean lines, boolean shapes) {
        super();
        this.seriesLinesVisible = new BooleanList();
        this.baseLinesVisible = lines;
        this.seriesShapesVisible = new BooleanList();
        this.baseShapesVisible = shapes;
        this.seriesShapesFilled = new BooleanList();
        this.baseShapesFilled = true;
        this.useFillPaint = false;
        this.drawOutlines = true;
        this.useOutlinePaint = false;
        this.useSeriesOffset = false;  // preserves old behaviour
        this.itemMargin = 0.0;
    }
     * Returns the flag used to control whether or not the line for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemLineVisible(int series, int item) { … }
     * Returns the flag used to control whether or not the lines for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesLinesVisible(int, Boolean)
    public Boolean getSeriesLinesVisible(int series) { … }
     * Sets the 'lines visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag (<code>null</code> permitted).
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, Boolean flag) { … }
     * Sets the 'lines visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, boolean visible) { … }
     * Returns the base 'lines visible' attribute.
     * @return The base flag.
     * @see #getBaseLinesVisible()
    public boolean getBaseLinesVisible() { … }
     * Sets the base 'lines visible' flag and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param flag  the flag.
     * @see #getBaseLinesVisible()
    public void setBaseLinesVisible(boolean flag) { … }
     * Returns the flag used to control whether or not the shape for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemShapeVisible(int series, int item) { … }
     * Returns the flag used to control whether or not the shapes for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return A boolean.
     * @see #setSeriesShapesVisible(int, Boolean)
    public Boolean getSeriesShapesVisible(int series) { … }
     * Sets the 'shapes visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, boolean visible) { … }
     * Sets the 'shapes visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, Boolean flag) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="PlotOrientation" role="collaborator">
     * Used to indicate the orientation (horizontal or vertical) of a 2D plot.
public final class PlotOrientation implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -2508771828190337782L;
     * For a plot where the range axis is horizontal.
    public static final PlotOrientation HORIZONTAL = new PlotOrientation("PlotOrientation.HORIZONTAL");
     * For a plot where the range axis is vertical.
    public static final PlotOrientation VERTICAL = new PlotOrientation("PlotOrientation.VERTICAL");
     * The name.
    private String name;
     * Private constructor.
     * @param name  the name.
    private PlotOrientation(String name) {
        this.name = name;
    }
     * Returns a string representing the object.
     * @return The string.
    public String toString() { … }
     * Returns <code>true</code> if this object is equal to the specified
     * object, and <code>false</code> otherwise.
     * @param obj  the object (<code>null</code> permitted).
     * @return A boolean.
    public boolean equals(Object obj) { … }
     * Returns a hash code for this instance.
     * @return A hash code.
    public int hashCode() { … }
     * Ensures that serialization returns the unique instances.
     * @return The object.
     * @throws ObjectStreamException if there is a problem.
    private Object readResolve() throws ObjectStreamException { … }
}
</class>
<class name="RendererChangeEvent" role="collaborator">
     * An event that can be forwarded to any {@link RendererChangeListener} to
     * signal a change to a renderer.
public class RendererChangeEvent extends ChartChangeEvent {
     * The renderer that generated the event.
    private Object renderer;
     * A flag that indicates whether this event relates to a change in the
     * series visibility.  If so, the receiver (if it is a plot) may want to
     * update the axis bounds.
     * @since 1.0.13
    private boolean seriesVisibilityChanged;
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
    public RendererChangeEvent(Object renderer) {
        this(renderer, false);
    }
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
     * @param seriesVisibilityChanged  a flag that indicates whether or not
     * the event relates to a change in the series visibility flags.
    public RendererChangeEvent(Object renderer, boolean seriesVisibilityChanged) {
        super(renderer);
        this.renderer = renderer;
        this.seriesVisibilityChanged = seriesVisibilityChanged;
    }
     * Returns the renderer that generated the event.
     * @return The renderer that generated the event.
    public Object getRenderer() { … }
     * Returns the flag that indicates whether or not the event relates to
     * a change in series visibility.
     * @return A boolean.
     * @since 1.0.13
    public boolean getSeriesVisibilityChanged() { … }
}
</class>
<class name="CategorySeriesLabelGenerator" role="collaborator">
     * A generator that creates labels for the series in a {@link CategoryDataset}.
     * <P>
     * Classes that implement this interface should be either (a) immutable, or
     * (b) cloneable via the <code>PublicCloneable</code> interface (defined in
     * the JCommon class library).  This provides a mechanism for the referring
     * renderer to clone the generator if necessary.
public interface CategorySeriesLabelGenerator {
     * Generates a label for the specified series.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param series  the series index.
     * @return A series label.
    public String generateLabel(CategoryDataset dataset, int series);
}
</class>
<class name="CategoryDataset" role="collaborator">
     * The interface for a dataset with one or more series, and values associated
     * with categories.
     * <P>
     * The categories are represented by <code>Comparable</code> instance, with the
     * category label being provided by the <code>toString</code> method.
public interface CategoryDataset extends KeyedValues2D, Dataset {

}
</class>
<class name="CategoryToolTipGenerator" role="collaborator">
     * A <i>category tool tip generator</i> is an object that can be assigned to a
     * {@link org.jfree.chart.renderer.category.CategoryItemRenderer} and that
     * assumes responsibility for creating text items to be used as tooltips for the
     * items in a {@link org.jfree.chart.plot.CategoryPlot}.
     * <p>
     * To assist with cloning charts, classes that implement this interface should
     * also implement the {@link PublicCloneable} interface.
public interface CategoryToolTipGenerator {
     * Generates the tool tip text for an item in a dataset.  Note: in the
     * current dataset implementation, each row is a series, and each column
     * contains values for a particular category.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The tooltip text (possibly <code>null</code>).
    public String generateToolTip(CategoryDataset dataset, int row, int column);
}
</class>
THE PATCH-CHANGED CLASS: AbstractCategoryItemRenderer. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
+++ /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
@@ -1794,7 +1794,7 @@
         }
         int index = this.plot.getIndexOf(this);
         CategoryDataset dataset = this.plot.getDataset(index);
-        if (dataset != null) {
+        if (dataset == null) {
             return result;
         }
         int seriesCount = dataset.getRowCount();

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    REMOVED: if (dataset != null) {
    ADDED  : if (dataset == null) {
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
An abstract base class that you can use to implement a new
{@link CategoryItemRenderer}.  When you create a new
{@link CategoryItemRenderer} you are not required to extend this class,
but it makes the job easier.
/
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
implements CategoryItemRenderer, Cloneable, PublicCloneable,
Serializable {

/** For serialization. */
private static final long serialVersionUID = 1247553218442497391L;

/** The plot that the renderer is assigned to. */
private CategoryPlot plot;

/** A list of item label generators (one per series). */
private ObjectList itemLabelGeneratorList;

/** The base item label generator. */
private CategoryItemLabelGenerator baseItemLabelGenerator;

/** A list of tool tip generators (one per series). */
private ObjectList toolTipGeneratorList;

/** The base tool tip generator. */
private CategoryToolTipGenerator baseToolTipGenerator;

/** A list of label generators (one per series). */
private ObjectList urlGeneratorList;

/** The base label generator. */
private CategoryURLGenerator baseURLGenerator;

/** The legend item label generator. */
private CategorySeriesLabelGenerator legendItemLabelGenerator;

/** The legend item tool tip generator. */
private CategorySeriesLabelGenerator legendItemToolTipGenerator;

/** The legend item URL generator. */
private CategorySeriesLabelGenerator legendItemURLGenerator;

/**
Annotations to be drawn in the background layer ('underneath' the data
items).

@sin
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.RenderingSource;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.plot.CategoryCrosshairState;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.text.TextUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.util.GradientPaintTransformer;
import org.jfree.chart.util.Layer;
import org.jfree.chart.util.LengthAdjustmentType;
import org.jfree.chart.util.ObjectList;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.RectangleAnchor;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.RectangleInsets;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.CategoryDatasetSelectionState;
import org.jfree.data.category.SelectableCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    getLegendItems, getItemCount, setDataset, setRenderer, addValue, get, getLabel, LineAndShapeRenderer, DefaultCategoryDataset, CategoryPlot

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
<details open><summary>▸ Output (~9,955 chars)</summary>

```
[
  {
    "name": "legendItems-singleSeries-nonNullDataset",
    "kind": "invariant",
    "contract": "AbstractCategoryItemRenderer.getLegendItems() returns a (possibly empty) collection of legend items for the series that this renderer is responsible for drawing, never null; for a LineAndShapeRenderer attached to a plot with a non-null dataset containing one series, the failing test fixes the required observable that one legend item is produced for that series.",
    "input": "Build a real org.jfree.chart.renderer.category.LineAndShapeRenderer, a real org.jfree.chart.plot.CategoryPlot, and a real org.jfree.data.category.DefaultCategoryDataset. Use fuzzed non-empty-ish ASCII strings for the single row key and column key, attach dataset and renderer to the plot, then add exactly one value so the dataset is non-null and has exactly one series.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\norg.jfree.data.category.DefaultCategoryDataset dataset;\nString rowKey;\nString columnKey;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  rowKey = data.consumeAsciiString(8);\n  if (rowKey.length() == 0) rowKey = \"S\";\n  columnKey = data.consumeAsciiString(8);\n  if (columnKey.length() == 0) columnKey = \"C\";\n  plot.setDataset(dataset);\n  plot.setRenderer(r);\n  dataset.addValue(1.0, rowKey, columnKey);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems-singleSeries-nonNullDataset violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null) {\n  throw new RuntimeException(\"relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null\");\n}\nif (items.getItemCount() != 1) {\n  throw new RuntimeException(\"relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got \" + items.getItemCount());\n}\nString label = items.get(0).getLabel();\nif (!rowKey.equals(label)) {\n  throw new RuntimeException(\"relation legendItems-singleSeries-nonNullDataset violated: expected legend label \" + rowKey + \" but got \" + label);\n}"
  },
  {
    "name": "legendItems-noPlot-emptyAndNonNull",
    "kind": "invariant",
    "contract": "AbstractCategoryItemRenderer.getLegendItems() returns the legend item collection never null; with no assigned plot there are no series for which this renderer is responsible, so the collection must be empty.",
    "input": "Construct a fresh org.jfree.chart.renderer.category.LineAndShapeRenderer and do not assign it to any plot.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems-noPlot-emptyAndNonNull violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null) {\n  throw new RuntimeException(\"relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null\");\n}\nif (items.getItemCount() != 0) {\n  throw new RuntimeException(\"relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got \" + items.getItemCount());\n}"
  },
  {
    "name": "legendItems-emptyDataset-emptyCollection",
    "kind": "invariant",
    "contract": "AbstractCategoryItemRenderer.getLegendItems() returns a possibly empty, never-null collection of legend items for the series it is responsible for drawing; when the attached dataset has zero rows, there are no series and therefore no legend items.",
    "input": "Construct a real LineAndShapeRenderer, CategoryPlot, and empty DefaultCategoryDataset. Attach dataset and renderer to the plot, but do not add any values. This probes the non-null-dataset / zero-series boundary.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\norg.jfree.data.category.DefaultCategoryDataset dataset;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  plot.setDataset(dataset);\n  plot.setRenderer(r);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems-emptyDataset-emptyCollection violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null) {\n  throw new RuntimeException(\"relation legendItems-emptyDataset-emptyCollection violated: getLegendItems() returned null\");\n}\nif (items.getItemCount() != 0) {\n  throw new RuntimeException(\"relation legendItems-emptyDataset-emptyCollection violated: expected 0 legend items for empty dataset but got \" + items.getItemCount());\n}"
  },
  {
    "name": "baseToolTipGenerator-overloadAgreement",
    "kind": "metamorphic",
    "contract": "The same-name overloads setBaseToolTipGenerator(CategoryToolTipGenerator) and setBaseToolTipGenerator(CategoryToolTipGenerator, boolean) are documented to perform the same setting, differing only in notification behavior.",
    "input": "Construct two fresh LineAndShapeRenderer instances. Use null as the generator, which the javadoc explicitly permits. Apply the one-argument overload to one renderer and the two-argument overload with notify=true to the other.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r1;\norg.jfree.chart.renderer.category.LineAndShapeRenderer r2;\ntry {\n  r1 = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  r2 = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.labels.CategoryToolTipGenerator g1;\norg.jfree.chart.labels.CategoryToolTipGenerator g2;\ntry {\n  r1.setBaseToolTipGenerator((org.jfree.chart.labels.CategoryToolTipGenerator) null);\n  g1 = r1.getBaseToolTipGenerator();\n  r2.setBaseToolTipGenerator((org.jfree.chart.labels.CategoryToolTipGenerator) null, true);\n  g2 = r2.getBaseToolTipGenerator();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation baseToolTipGenerator-overloadAgreement violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (g1 != g2) {\n  throw new RuntimeException(\"relation baseToolTipGenerator-overloadAgreement violated: overloads produced different stored generators: \" + g1 + \" vs \" + g2);\n}"
  },
  {
    "name": "seriesURLGenerator-overloadAgreement",
    "kind": "metamorphic",
    "contract": "The same-name overloads setSeriesURLGenerator(int, CategoryURLGenerator) and setSeriesURLGenerator(int, CategoryURLGenerator, boolean) are documented to perform the same per-series setting, differing only in notification behavior.",
    "input": "Construct two fresh LineAndShapeRenderer instances. Draw a non-negative series index from data and use null as the URL generator, which the javadoc permits. Apply the two overloads on separate renderers with the same inputs.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r1;\norg.jfree.chart.renderer.category.LineAndShapeRenderer r2;\nint series;\ntry {\n  r1 = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  r2 = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  series = data.consumeInt(0, 8);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.urls.CategoryURLGenerator g1;\norg.jfree.chart.urls.CategoryURLGenerator g2;\ntry {\n  r1.setSeriesURLGenerator(series, (org.jfree.chart.urls.CategoryURLGenerator) null);\n  g1 = r1.getSeriesURLGenerator(series);\n  r2.setSeriesURLGenerator(series, (org.jfree.chart.urls.CategoryURLGenerator) null, true);\n  g2 = r2.getSeriesURLGenerator(series);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation seriesURLGenerator-overloadAgreement violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (g1 != g2) {\n  throw new RuntimeException(\"relation seriesURLGenerator-overloadAgreement violated: overloads produced different stored generators for series \" + series + \": \" + g1 + \" vs \" + g2);\n}"
  },
  {
    "name": "setPlot-getPlot-roundTrip",
    "kind": "invariant",
    "contract": "CategoryItemRenderer documents that getPlot() returns the plot the renderer has been assigned to, and setPlot(CategoryPlot) sets that assigned plot.",
    "input": "Construct a fresh LineAndShapeRenderer and a fresh CategoryPlot, then call setPlot(plot) directly with a non-null plot.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.plot.CategoryPlot observed;\ntry {\n  r.setPlot(plot);\n  observed = r.getPlot();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation setPlot-getPlot-roundTrip violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (observed != plot) {\n  throw new RuntimeException(\"relation setPlot-getPlot-roundTrip violated: getPlot() did not return the plot passed to setPlot()\");\n}"
  }
]
```

</details>

---
## [6] ⚙️ screen · `legendItems-singleSeries-nonNullDataset`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [7] ⚙️ screen-fuzz-buggy · `legendItems-singleSeries-nonNullDataset`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [8] ⚙️ screen · `legendItems-noPlot-emptyAndNonNull`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [9] ⚙️ screen-fuzz-buggy · `legendItems-noPlot-emptyAndNonNull`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [10] ⚙️ screen · `legendItems-emptyDataset-emptyCollection`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [11] ⚙️ screen-fuzz-buggy · `legendItems-emptyDataset-emptyCollection`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [12] ⚙️ screen · `baseToolTipGenerator-overloadAgreement`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [13] ⚙️ screen-fuzz-buggy · `baseToolTipGenerator-overloadAgreement`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [14] ⚙️ screen · `seriesURLGenerator-overloadAgreement`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [15] ⚙️ screen-fuzz-buggy · `seriesURLGenerator-overloadAgreement`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [16] ⚙️ screen · `setPlot-getPlot-roundTrip`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [17] ⚙️ screen-fuzz-buggy · `setPlot-getPlot-roundTrip`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [18] ⚙️ screen · `legendItems-singleSeries-nonNullDataset`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [19] ⚙️ screen · `legendItems-noPlot-emptyAndNonNull`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [20] ⚙️ screen · `legendItems-emptyDataset-emptyCollection`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [21] ⚙️ screen · `baseToolTipGenerator-overloadAgreement`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [22] ⚙️ screen · `seriesURLGenerator-overloadAgreement`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [23] ⚙️ screen · `setPlot-getPlot-roundTrip`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [24] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "legendItems-singleSeries-nonNullDataset",
    "legendItems-noPlot-emptyAndNonNull",
    "legendItems-emptyDataset-emptyCollection",
    "baseToolTipGenerator-overloadAgreement",
    "seriesURLGenerator-overloadAgreement",
    "setPlot-getPlot-roundTrip"
  ],
  "count": 6
}
```

---
## [25] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~13,540 chars)</summary>

```
[
  {
    "name": "legendItems_match_getLegendItem_enumeration",
    "kind": "metamorphic",
    "contract": "AbstractCategoryItemRenderer.getLegendItems() returns a collection of legend items for the series that this renderer is responsible for drawing, and its javadoc explicitly points to getLegendItem(int, int); therefore the collection must agree with the per-series legend items that getLegendItem() supplies for the plot's dataset/renderer index.",
    "input": "Construct a real org.jfree.chart.renderer.category.LineAndShapeRenderer, attach it to a real org.jfree.chart.plot.CategoryPlot with a non-null org.jfree.data.category.DefaultCategoryDataset, add 1..4 distinct series (rows) and at least one category, and explicitly mark every present series visible in the legend. Also vary the plot row-rendering order between ASCENDING and DESCENDING.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\norg.jfree.data.category.DefaultCategoryDataset dataset;\nint n;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  n = data.consumeInt(1, 4);\n  for (int i = 0; i < n; i++) {\n    String row = \"S\" + i + \"_\" + data.consumeAsciiString(data.consumeInt(0, 6));\n    String col = \"C\" + data.consumeInt(0, 3);\n    dataset.addValue((double) (i + 1), row, col);\n    r.setSeriesVisibleInLegend(i, Boolean.TRUE);\n  }\n  plot.setDataset(dataset);\n  plot.setRenderer(r);\n  plot.setRowRenderingOrder(data.consumeBoolean() ? org.jfree.chart.util.SortOrder.ASCENDING : org.jfree.chart.util.SortOrder.DESCENDING);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection actual;\ntry {\n  actual = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems_match_getLegendItem_enumeration violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nint rendererIndex = plot.getIndexOf(r);\njava.util.ArrayList expectedLabels = new java.util.ArrayList();\nif (plot.getRowRenderingOrder().equals(org.jfree.chart.util.SortOrder.ASCENDING)) {\n  for (int i = 0; i < dataset.getRowCount(); i++) {\n    if (r.isSeriesVisibleInLegend(i)) {\n      org.jfree.chart.LegendItem item = r.getLegendItem(rendererIndex, i);\n      if (item != null) {\n        expectedLabels.add(item.getLabel());\n      }\n    }\n  }\n} else {\n  for (int i = dataset.getRowCount() - 1; i >= 0; i--) {\n    if (r.isSeriesVisibleInLegend(i)) {\n      org.jfree.chart.LegendItem item = r.getLegendItem(rendererIndex, i);\n      if (item != null) {\n        expectedLabels.add(item.getLabel());\n      }\n    }\n  }\n}\nif (actual.getItemCount() != expectedLabels.size()) {\n  throw new RuntimeException(\"relation legendItems_match_getLegendItem_enumeration violated: collection count=\" + actual.getItemCount() + \" expected=\" + expectedLabels.size());\n}\nfor (int i = 0; i < expectedLabels.size(); i++) {\n  String got = actual.get(i).getLabel();\n  String exp = (String) expectedLabels.get(i);\n  if (!java.util.Objects.equals(got, exp)) {\n    throw new RuntimeException(\"relation legendItems_match_getLegendItem_enumeration violated at index \" + i + \": \" + got + \" vs \" + exp);\n  }\n}"
  },
  {
    "name": "legendItems_never_null_across_plot_dataset_states",
    "kind": "invariant",
    "contract": "The javadoc for AbstractCategoryItemRenderer.getLegendItems() says it returns 'The legend item collection (never null)'.",
    "input": "Use a real LineAndShapeRenderer and exercise three receiver states built only through public API: fresh renderer with no plot, then attached to a plot with no dataset, then attached to a plot with an empty DefaultCategoryDataset, then after adding 1..3 values to that dataset.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\norg.jfree.data.category.DefaultCategoryDataset dataset;\nint n;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  n = data.consumeInt(1, 3);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection lic;\ntry {\n  lic = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: unexpected \" + e.getClass().getName() + \" on fresh renderer: \" + e.getMessage());\n}\nif (lic == null) {\n  throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: null on fresh renderer\");\n}\ntry {\n  plot.setRenderer(r);\n} catch (Exception e) {\n  return;\n}\ntry {\n  lic = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: unexpected \" + e.getClass().getName() + \" with plot/no dataset: \" + e.getMessage());\n}\nif (lic == null) {\n  throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: null with plot/no dataset\");\n}\ntry {\n  plot.setDataset(dataset);\n} catch (Exception e) {\n  return;\n}\ntry {\n  lic = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: unexpected \" + e.getClass().getName() + \" with empty dataset: \" + e.getMessage());\n}\nif (lic == null) {\n  throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: null with empty dataset\");\n}\nfor (int i = 0; i < n; i++) {\n  try {\n    dataset.addValue((double) (i + 1), \"R\" + i + \"_\" + data.consumeAsciiString(data.consumeInt(0, 4)), \"C\" + data.consumeInt(0, 2));\n  } catch (Exception e) {\n    return;\n  }\n  try {\n    lic = r.getLegendItems();\n  } catch (Exception e) {\n    throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: unexpected \" + e.getClass().getName() + \" after dataset mutation: \" + e.getMessage());\n  }\n  if (lic == null) {\n    throw new RuntimeException(\"relation legendItems_never_null_across_plot_dataset_states violated: null after dataset mutation \" + i);\n  }\n}"
  },
  {
    "name": "setPlot_getPlot_roundtrip",
    "kind": "invariant",
    "contract": "CategoryItemRenderer.getPlot()/setPlot(CategoryPlot) are documented as accessor/mutator counterparts: getPlot returns the plot assigned to the renderer, and setPlot sets the plot assigned to the renderer.",
    "input": "Construct a real LineAndShapeRenderer and a real CategoryPlot, then assign the plot to the renderer using setPlot(plot).",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n} catch (Exception e) {\n  return;\n}\ntry {\n  r.setPlot(plot);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation setPlot_getPlot_roundtrip violated: unexpected \" + e.getClass().getName() + \" from setPlot on valid-by-construction input: \" + e.getMessage());\n}\norg.jfree.chart.plot.CategoryPlot got;\ntry {\n  got = r.getPlot();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation setPlot_getPlot_roundtrip violated: unexpected \" + e.getClass().getName() + \" from getPlot on valid-by-construction input: \" + e.getMessage());\n}\nif (got != plot) {\n  throw new RuntimeException(\"relation setPlot_getPlot_roundtrip violated: getPlot() did not return the assigned plot\");\n}"
  },
  {
    "name": "empty_dataset_has_zero_legend_items",
    "kind": "invariant",
    "contract": "AbstractCategoryItemRenderer.getLegendItems() returns a possibly empty collection of legend items for the series the renderer is responsible for drawing; with an empty dataset there are no series, so the returned collection must have item count 0.",
    "input": "Attach a real LineAndShapeRenderer to a CategoryPlot whose dataset is a freshly constructed, empty DefaultCategoryDataset.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\norg.jfree.data.category.DefaultCategoryDataset dataset;\ntry {\n  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  plot.setDataset(dataset);\n  plot.setRenderer(r);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection lic;\ntry {\n  lic = r.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation empty_dataset_has_zero_legend_items violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (lic == null) {\n  throw new RuntimeException(\"relation empty_dataset_has_zero_legend_items violated: null collection\");\n}\nif (lic.getItemCount() != 0) {\n  throw new RuntimeException(\"relation empty_dataset_has_zero_legend_items violated: expected 0 but got \" + lic.getItemCount());\n}"
  },
  {
    "name": "defaultCategoryDataset_addValue_overloads_agree",
    "kind": "metamorphic",
    "contract": "DefaultCategoryDataset.addValue(Number, Comparable, Comparable) and addValue(double, Comparable, Comparable) are both documented to add a value to the table; for the same numeric value and keys they must produce equivalent dataset contents.",
    "input": "Create two fresh DefaultCategoryDataset instances, choose one finite int value and non-null ASCII row/column keys, call the Number overload on one dataset and the double overload on the other with the same logical value and keys.",
    "check": "org.jfree.data.category.DefaultCategoryDataset d1;\norg.jfree.data.category.DefaultCategoryDataset d2;\nString row;\nString col;\nint v;\ntry {\n  d1 = new org.jfree.data.category.DefaultCategoryDataset();\n  d2 = new org.jfree.data.category.DefaultCategoryDataset();\n  row = \"R\" + data.consumeAsciiString(data.consumeInt(1, 8));\n  col = \"C\" + data.consumeAsciiString(data.consumeInt(1, 8));\n  v = data.consumeInt(-1000, 1000);\n  d1.addValue(Integer.valueOf(v), row, col);\n  d2.addValue((double) v, row, col);\n} catch (Exception e) {\n  return;\n}\nNumber n1;\nNumber n2;\ntry {\n  n1 = d1.getValue(row, col);\n  n2 = d2.getValue(row, col);\n} catch (Exception e) {\n  return;\n}\nif (d1.getRowCount() != d2.getRowCount() || d1.getColumnCount() != d2.getColumnCount()) {\n  throw new RuntimeException(\"relation defaultCategoryDataset_addValue_overloads_agree violated: shape differs \" + d1.getRowCount() + \"x\" + d1.getColumnCount() + \" vs \" + d2.getRowCount() + \"x\" + d2.getColumnCount());\n}\nif (n1 == null || n2 == null || n1.doubleValue() != n2.doubleValue()) {\n  throw new RuntimeException(\"relation defaultCategoryDataset_addValue_overloads_agree violated: values differ \" + n1 + \" vs \" + n2);\n}\nif (!java.util.Objects.equals(d1.getRowKey(0), d2.getRowKey(0)) || !java.util.Objects.equals(d1.getColumnKey(0), d2.getColumnKey(0))) {\n  throw new RuntimeException(\"relation defaultCategoryDataset_addValue_overloads_agree violated: keys differ\");\n}"
  },
  {
    "name": "baseToolTipGenerator_null_overloads_agree",
    "kind": "metamorphic",
    "contract": "The overloads setBaseToolTipGenerator(CategoryToolTipGenerator) and setBaseToolTipGenerator(CategoryToolTipGenerator, boolean) are documented to set the same base tool tip generator, differing only in notification behavior; therefore the stored generator returned by getBaseToolTipGenerator() must agree.",
    "input": "Use two fresh LineAndShapeRenderer instances and the valid-by-contract null generator value; call the one-argument overload on one renderer and the two-argument overload with notify=true on the other.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r1;\norg.jfree.chart.renderer.category.LineAndShapeRenderer r2;\ntry {\n  r1 = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n  r2 = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n} catch (Exception e) {\n  return;\n}\ntry {\n  r1.setBaseToolTipGenerator((org.jfree.chart.labels.CategoryToolTipGenerator) null);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation baseToolTipGenerator_null_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from one-arg overload: \" + e.getMessage());\n}\ntry {\n  r2.setBaseToolTipGenerator((org.jfree.chart.labels.CategoryToolTipGenerator) null, true);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation baseToolTipGenerator_null_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from two-arg overload: \" + e.getMessage());\n}\norg.jfree.chart.labels.CategoryToolTipGenerator g1 = r1.getBaseToolTipGenerator();\norg.jfree.chart.labels.CategoryToolTipGenerator g2 = r2.getBaseToolTipGenerator();\nif (g1 != g2) {\n  throw new RuntimeException(\"relation baseToolTipGenerator_null_overloads_agree violated: getters disagree after equivalent null set: \" + g1 + \" vs \" + g2);\n}\nif (g1 != null) {\n  throw new RuntimeException(\"relation baseToolTipGenerator_null_overloads_agree violated: expected null base generator but got \" + g1);\n}"
  }
]
```

</details>

---
## [26] ⚙️ screen · `legendItems_match_getLegendItem_enumeration`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~758 chars)</summary>

- reason: the probe `r.setSeriesVisibleInLegend(...)` on the patch-changed class `LineAndShapeRenderer` runs inside the try at line 5, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `LineAndShapeRenderer` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [27] ⚙️ screen-fuzz-buggy · `legendItems_match_getLegendItem_enumeration`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [28] ⚙️ screen · `legendItems_never_null_across_plot_dataset_states`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [29] ⚙️ screen-fuzz-buggy · `legendItems_never_null_across_plot_dataset_states`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [30] ⚙️ screen · `setPlot_getPlot_roundtrip`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [31] ⚙️ screen-fuzz-buggy · `setPlot_getPlot_roundtrip`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [32] ⚙️ screen · `empty_dataset_has_zero_legend_items`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [33] ⚙️ screen-fuzz-buggy · `empty_dataset_has_zero_legend_items`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [34] ⚙️ screen-fuzz-buggy · `defaultCategoryDataset_addValue_overloads_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [35] ⚙️ screen · `baseToolTipGenerator_null_overloads_agree`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [36] ⚙️ screen-fuzz-buggy · `baseToolTipGenerator_null_overloads_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [37] ⚙️ screen · `legendItems_match_getLegendItem_enumeration`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [38] ⚙️ screen · `legendItems_never_null_across_plot_dataset_states`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [39] ⚙️ screen · `setPlot_getPlot_roundtrip`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [40] ⚙️ screen · `empty_dataset_has_zero_legend_items`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [41] ⚙️ screen · `defaultCategoryDataset_addValue_overloads_agree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [42] ⚙️ screen · `baseToolTipGenerator_null_overloads_agree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [43] ⚙️ reference-widening · `enumerate`
**output:** **3 observable(s) to widen onto**
<details><summary>▸ reason (~445 chars)</summary>

- reason: the kept checks probe ['legenditems', 'plot', 'basetooltipgenerator'] among the patched class's no-argument readers; 3 of those have no admitted reference and 3 fit under the per-leg cap of 3 (the leg already holds []); 16 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['addvalue', 'abstractcategoryitemrenderer', 'defaultcategorydataset', 'rowkey', 'columnkey', 'lineandshaperenderer'])

</details>
<details><summary>▸ detail (~2,742 chars)</summary>

- detail: {'targets': ['legenditems', 'plot', 'basetooltipgenerator'], 'already_admitted': [], 'cap': 3, 'checks_read': 12, 'excluded': [{'key': 'addvalue', 'why': 'the patched class does not declare `addValue()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'abstractcategoryitemrenderer', 'why': 'the patched class declares `AbstractCategoryItemRenderer()` as a constructor or a void method — it reports no observable value'}, {'key': 'defaultcategorydataset', 'why': 'the patched class does not declare `DefaultCategoryDataset()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'rowkey', 'why': 'the patched class does not declare `getRowKey()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'columnkey', 'why': 'the patched class does not declare `getColumnKey()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'lineandshaperenderer', 'why': 'the patched class does not declare `LineAndShapeRenderer()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'renderer', 'why': 'the patched class does not declare `getRenderer()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'setbasetooltipgenerator', 'why': "the patched class declares `setBaseToolTipGenerator` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'setseriesurlgenerator', 'why': "the patched class declares `setSeriesURLGenerator` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'seriesurlgenerator', 'why': "the patched class declares `getSeriesURLGenerator` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'setplot', 'why': "the patched class declares `setPlot` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'setseriesvisibleinlegend', 'why': 'the patched class does not declare `setSeriesVisibleInLegend()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 16}

</details>

---
## [44] ⚙️ reference-widening · `getLegendItems`
**output:** **reference REQUESTED for observable `legenditems`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [45] ⚙️ reference-impl · `getLegendItems`
**output:** **widening target requested**
- detail: {'candidates': ['getLegendItems'], 'attempting': 'getLegendItems'}

---
## [46] ⚙️ reference-impl · `getLegendItems`
**output:** **screening surface resolved**
- reason: 2 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getBaseLinesVisible', 'getPassCount'], 'patch_touched_exempted': [], 'screened_siblings': ['getBaseLinesVisible', 'getPassCount'], 'declaring_types': ['AbstractCategoryItemRenderer', 'LineAndShapeRenderer']}

---
## [47] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~61,736 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
**[user]**
```
Implement `getLegendItems` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * { /* body withheld */ } you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() { /* body withheld */ }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { /* body withheld */ }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { /* body withheld */ }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { /* body withheld */ }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { /* body withheld */ }
     * Sets the item label generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the item label generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { /* body withheld */ }
     * Sets the base item label generator and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the base item label generator and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { /* body withheld */ }
     * Sets the URL generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { /* body withheld */ }
     * Sets the URL generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { /* body withheld */ }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * { /* body withheld */ } class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a { /* body withheld */ } to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a { /* body withheld */ } to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * { /* body withheld */ } method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void test2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        assertNotNull(r.getLegendItems());
        assertEquals(0, r.getLegendItems().getItemCount());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        assertEquals(0, r.getLegendItems().getItemCount());

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        assertEquals(1, lic.getItemCount());
        assertEquals("S1", lic.get(0).getLabel());
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * { /* body withheld */ } you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() { /* body withheld */ }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { /* body withheld */ }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { /* body withheld */ }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { /* body withheld */ }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { /* body withheld */ }
     * Sets the item label generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the item label generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { /* body withheld */ }
     * Sets the base item label generator and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the base item label generator and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { /* body withheld */ }
     * Sets the URL generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { /* body withheld */ }
     * Sets the URL generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { /* body withheld */ }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * { /* body withheld */ } class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a { /* body withheld */ } to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a { /* body withheld */ } to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * { /* body withheld */ } method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryPlot" role="test-subject">
     * A general plotting class that uses data from a {@link CategoryDataset} and
     * renders each data item using a { /* body withheld */ }.
public class CategoryPlot extends Plot implements ValueAxisPlot, Pannable,
        Selectable, Zoomable, AnnotationChangeListener, RendererChangeListener,
        Cloneable, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LineAndShapeRenderer" role="test-subject">
     * A renderer that draws shapes for each data item, and lines between data
     * items (for use with the { /* body withheld */ } class).
     * The example shown here is generated by the <code>LineChartDemo1.java</code>
     * program included in the JFreeChart Demo Collection:
     * <br><br>
     * <img src="../../../../../images/LineAndShapeRendererSample.png"
     * alt="LineAndShapeRendererSample.png" />
public class LineAndShapeRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -197749519869226398L;
     * A table of flags that control (per series) whether or not lines are
     * visible.
    private BooleanList seriesLinesVisible;
     * A flag indicating whether or not lines are drawn between non-null
     * points.
    private boolean baseLinesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * visible.
    private BooleanList seriesShapesVisible;
     * The default value returned by the getShapeVisible() method.
    private boolean baseShapesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * filled.
    private BooleanList seriesShapesFilled;
     * The default value returned by the getShapeFilled() method.
    private boolean baseShapesFilled;
     * A flag that controls whether the fill paint is used for filling
     * shapes.
    private boolean useFillPaint;
     * A flag that controls whether outlines are drawn for shapes.
    private boolean drawOutlines;
     * A flag that controls whether the outline paint is used for drawing shape
     * outlines - if not, the regular series paint is used.
    private boolean useOutlinePaint;
     * A flag that controls whether or not the x-position for each item is
     * offset within the category according to the series.
     * @since 1.0.7
    private boolean useSeriesOffset;
     * The item margin used for series offsetting - this allows the positioning
     * to match the bar positions of the { /* body withheld */ } class.
     * @since 1.0.7
    private double itemMargin;
     * Creates a renderer with both lines and shapes visible by default.
    public LineAndShapeRenderer() { /* body withheld */ }
     * Creates a new renderer with lines and/or shapes visible.
     * @param lines  draw lines?
     * @param shapes  draw shapes?
    public LineAndShapeRenderer(boolean lines, boolean shapes) { /* body withheld */ }
     * Returns the flag used to control whether or not the line for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemLineVisible(int series, int item) { /* body withheld */ }
     * Returns the flag used to control whether or not the lines for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesLinesVisible(int, Boolean)
    public Boolean getSeriesLinesVisible(int series) { /* body withheld */ }
     * Sets the 'lines visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag (<code>null</code> permitted).
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, Boolean flag) { /* body withheld */ }
     * Sets the 'lines visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, boolean visible) { /* body withheld */ }
     * Returns the base 'lines visible' attribute.
     * @return The base flag.
     * @see #getBaseLinesVisible()
    public boolean getBaseLinesVisible() { /* body withheld */ }
     * Sets the base 'lines visible' flag and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param flag  the flag.
     * @see #getBaseLinesVisible()
    public void setBaseLinesVisible(boolean flag) { /* body withheld */ }
     * Returns the flag used to control whether or not the shape for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemShapeVisible(int series, int item) { /* body withheld */ }
     * Returns the flag used to control whether or not the shapes for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return A boolean.
     * @see #setSeriesShapesVisible(int, Boolean)
    public Boolean getSeriesShapesVisible(int series) { /* body withheld */ }
     * Sets the 'shapes visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, boolean visible) { /* body withheld */ }
     * Sets the 'shapes visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, Boolean flag) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="PlotOrientation" role="collaborator">
     * Used to indicate the orientation (horizontal or vertical) of a 2D plot.
public final class PlotOrientation implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -2508771828190337782L;
     * For a plot where the range axis is horizontal.
    public static final PlotOrientation HORIZONTAL = new PlotOrientation("PlotOrientation.HORIZONTAL");
     * For a plot where the range axis is vertical.
    public static final PlotOrientation VERTICAL = new PlotOrientation("PlotOrientation.VERTICAL");
     * The name.
    private String name;
     * Private constructor.
     * @param name  the name.
    private PlotOrientation(String name) { /* body withheld */ }
     * Returns a string representing the object.
     * @return The string.
    public String toString() { /* body withheld */ }
     * Returns <code>true</code> if this object is equal to the specified
     * object, and <code>false</code> otherwise.
     * @param obj  the object (<code>null</code> permitted).
     * @return A boolean.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this instance.
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Ensures that serialization returns the unique instances.
     * @return The object.
     * @throws ObjectStreamException if there is a problem.
    private Object readResolve() throws ObjectStreamException { /* body withheld */ }
}
</class>

<class name="RendererChangeEvent" role="collaborator">
     * An event that can be forwarded to any {@link RendererChangeListener} to
     * signal a change to a renderer.
public class RendererChangeEvent extends ChartChangeEvent {
     * The renderer that generated the event.
    private Object renderer;
     * A flag that indicates whether this event relates to a change in the
     * series visibility.  If so, the receiver (if it is a plot) may want to
     * update the axis bounds.
     * @since 1.0.13
    private boolean seriesVisibilityChanged;
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
    public RendererChangeEvent(Object renderer) { /* body withheld */ }
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
     * @param seriesVisibilityChanged  a flag that indicates whether or not
     * the event relates to a change in the series visibility flags.
    public RendererChangeEvent(Object renderer, boolean seriesVisibilityChanged) { /* body withheld */ }
     * Returns the renderer that generated the event.
     * @return The renderer that generated the event.
    public Object getRenderer() { /* body withheld */ }
     * Returns the flag that indicates whether or not the event relates to
     * a change in series visibility.
     * @return A boolean.
     * @since 1.0.13
    public boolean getSeriesVisibilityChanged() { /* body withheld */ }
}
</class>

<class name="CategorySeriesLabelGenerator" role="collaborator">
     * A generator that creates labels for the series in a {@link CategoryDataset}.
     * <P>
     * Classes that implement this interface should be either (a) immutable, or
     * (b) cloneable via the <code>PublicCloneable</code> interface (defined in
     * the JCommon class library).  This provides a mechanism for the referring
     * renderer to clone the generator if necessary.
public interface CategorySeriesLabelGenerator {
     * Generates a label for the specified series.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param series  the series index.
     * @return A series label.
    public String generateLabel(CategoryDataset dataset, int series);
}
</class>

<class name="CategoryDataset" role="collaborator">
     * The interface for a dataset with one or more series, and values associated
     * with categories.
     * <P>
     * The categories are represented by <code>Comparable</code> instance, with the
     * category label being provided by the <code>toString</code> method.
public interface CategoryDataset extends KeyedValues2D, Dataset {

}
</class>

<class name="CategoryToolTipGenerator" role="collaborator">
     * A <i>category tool tip generator</i> is an object that can be assigned to a
     * {@link org.jfree.chart.renderer.category.CategoryItemRenderer} and that
     * assumes responsibility for creating text items to be used as tooltips for the
     * items in a { /* body withheld */ }.
     * <p>
     * To assist with cloning charts, classes that implement this interface should
     * also implement the {@link PublicCloneable} interface.
public interface CategoryToolTipGenerator {
     * Generates the tool tip text for an item in a dataset.  Note: in the
     * current dataset implementation, each row is a series, and each column
     * contains values for a particular category.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The tooltip text (possibly <code>null</code>).
    public String generateToolTip(CategoryDataset dataset, int row, int column);
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getBaseLinesVisible`, `getPassCount`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,287 chars)</summary>

```
// compute(org.jfree.chart.plot.CategoryPlot, boolean) : getLegendItems, getBaseLinesVisible, getPassCount
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;

public class ReferenceImpl {

    public static LegendItemCollection compute_getLegendItems(
            CategoryPlot plot, boolean baseLinesVisible) {
        LegendItemCollection result = new LegendItemCollection();
        if (plot == null) {
            return result;
        }
        CategoryDataset dataset = plot.getDataset();
        if (dataset == null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        for (int series = 0; series < seriesCount; series++) {
            Comparable key = dataset.getRowKey(series);
            String label = (key != null ? key.toString() : "null");
            result.add(new LegendItem(label));
        }
        return result;
    }

    public static boolean compute_getBaseLinesVisible(
            CategoryPlot plot, boolean baseLinesVisible) {
        return baseLinesVisible;
    }

    public static int compute_getPassCount(
            CategoryPlot plot, boolean baseLinesVisible) {
        return 1;
    }
}
```

</details>

---
## [48] ⚙️ reference-impl · `getLegendItems`
**output:** **reference generated**
- detail: {'chars': 1287}

---
## [49] ⚙️ reference-impl · `getLegendItems`
**output:** **signature declared**
- detail: {'signature': 'org.jfree.chart.plot.CategoryPlot, boolean'}

---
## [50] ⚙️ reference-impl · `getLegendItems`
**output:** **reference observables matched**
- detail: {'matched': {'getLegendItems': 'getLegendItems', 'getBaseLinesVisible': 'getBaseLinesVisible', 'getPassCount': 'getPassCount'}, 'declared_only': []}

---
## [51] ⚙️ reference-impl · `getLegendItems`
**output:** **reference too thin to screen — DISCARDED**
- reason: 2 shared sibling observable(s) ['getBaseLinesVisible', 'getPassCount']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [52] ⚙️ reference-widening · `getLegendItems`
**output:** **widening REJECTED for observable `legenditems`**
- reason: reference too thin to screen — DISCARDED — 2 shared sibling observable(s) ['getBaseLinesVisible', 'getPassCount']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [53] ⚙️ reference-widening · `getPlot`
**output:** **reference REQUESTED for observable `plot`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [54] ⚙️ reference-impl · `getPlot`
**output:** **widening target requested**
- detail: {'candidates': ['getPlot'], 'attempting': 'getPlot'}

---
## [55] ⚙️ reference-impl · `getPlot`
**output:** **screening surface resolved**
- reason: 4 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getBaseLinesVisible', 'getBaseSeriesVisible', 'getBaseSeriesVisibleInLegend', 'getPassCount'], 'patch_touched_exempted': [], 'screened_siblings': ['getBaseLinesVisible', 'getBaseSeriesVisible', 'getBaseSeriesVisibleInLegend', 'getPassCount'], 'declaring_types': ['AbstractCategoryItemRenderer', 'CategoryItemRenderer', 'LineAndShapeRenderer']}

---
## [56] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~61,499 chars, 1 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
**[user]**
```
Implement `getPlot` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * { /* body withheld */ } you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() { /* body withheld */ }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { /* body withheld */ }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { /* body withheld */ }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { /* body withheld */ }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { /* body withheld */ }
     * Sets the item label generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the item label generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { /* body withheld */ }
     * Sets the base item label generator and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the base item label generator and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { /* body withheld */ }
     * Sets the URL generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { /* body withheld */ }
     * Sets the URL generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { /* body withheld */ }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * { /* body withheld */ } class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a { /* body withheld */ } to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a { /* body withheld */ } to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * { /* body withheld */ } method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void test2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        assertNotNull(r.getLegendItems());
        assertEquals(0, r.getLegendItems().getItemCount());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        assertEquals(0, r.getLegendItems().getItemCount());

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        assertEquals(1, lic.getItemCount());
        assertEquals("S1", lic.get(0).getLabel());
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * { /* body withheld */ } you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() { /* body withheld */ }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { /* body withheld */ }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { /* body withheld */ }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { /* body withheld */ }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { /* body withheld */ }
     * Sets the item label generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the item label generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { /* body withheld */ }
     * Sets the base item label generator and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the base item label generator and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { /* body withheld */ }
     * Sets the URL generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { /* body withheld */ }
     * Sets the URL generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { /* body withheld */ }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * { /* body withheld */ } class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a { /* body withheld */ } to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a { /* body withheld */ } to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * { /* body withheld */ } method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryPlot" role="test-subject">
     * A general plotting class that uses data from a {@link CategoryDataset} and
     * renders each data item using a { /* body withheld */ }.
public class CategoryPlot extends Plot implements ValueAxisPlot, Pannable,
        Selectable, Zoomable, AnnotationChangeListener, RendererChangeListener,
        Cloneable, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LineAndShapeRenderer" role="test-subject">
     * A renderer that draws shapes for each data item, and lines between data
     * items (for use with the { /* body withheld */ } class).
     * The example shown here is generated by the <code>LineChartDemo1.java</code>
     * program included in the JFreeChart Demo Collection:
     * <br><br>
     * <img src="../../../../../images/LineAndShapeRendererSample.png"
     * alt="LineAndShapeRendererSample.png" />
public class LineAndShapeRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -197749519869226398L;
     * A table of flags that control (per series) whether or not lines are
     * visible.
    private BooleanList seriesLinesVisible;
     * A flag indicating whether or not lines are drawn between non-null
     * points.
    private boolean baseLinesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * visible.
    private BooleanList seriesShapesVisible;
     * The default value returned by the getShapeVisible() method.
    private boolean baseShapesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * filled.
    private BooleanList seriesShapesFilled;
     * The default value returned by the getShapeFilled() method.
    private boolean baseShapesFilled;
     * A flag that controls whether the fill paint is used for filling
     * shapes.
    private boolean useFillPaint;
     * A flag that controls whether outlines are drawn for shapes.
    private boolean drawOutlines;
     * A flag that controls whether the outline paint is used for drawing shape
     * outlines - if not, the regular series paint is used.
    private boolean useOutlinePaint;
     * A flag that controls whether or not the x-position for each item is
     * offset within the category according to the series.
     * @since 1.0.7
    private boolean useSeriesOffset;
     * The item margin used for series offsetting - this allows the positioning
     * to match the bar positions of the { /* body withheld */ } class.
     * @since 1.0.7
    private double itemMargin;
     * Creates a renderer with both lines and shapes visible by default.
    public LineAndShapeRenderer() { /* body withheld */ }
     * Creates a new renderer with lines and/or shapes visible.
     * @param lines  draw lines?
     * @param shapes  draw shapes?
    public LineAndShapeRenderer(boolean lines, boolean shapes) { /* body withheld */ }
     * Returns the flag used to control whether or not the line for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemLineVisible(int series, int item) { /* body withheld */ }
     * Returns the flag used to control whether or not the lines for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesLinesVisible(int, Boolean)
    public Boolean getSeriesLinesVisible(int series) { /* body withheld */ }
     * Sets the 'lines visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag (<code>null</code> permitted).
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, Boolean flag) { /* body withheld */ }
     * Sets the 'lines visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, boolean visible) { /* body withheld */ }
     * Returns the base 'lines visible' attribute.
     * @return The base flag.
     * @see #getBaseLinesVisible()
    public boolean getBaseLinesVisible() { /* body withheld */ }
     * Sets the base 'lines visible' flag and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param flag  the flag.
     * @see #getBaseLinesVisible()
    public void setBaseLinesVisible(boolean flag) { /* body withheld */ }
     * Returns the flag used to control whether or not the shape for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemShapeVisible(int series, int item) { /* body withheld */ }
     * Returns the flag used to control whether or not the shapes for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return A boolean.
     * @see #setSeriesShapesVisible(int, Boolean)
    public Boolean getSeriesShapesVisible(int series) { /* body withheld */ }
     * Sets the 'shapes visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, boolean visible) { /* body withheld */ }
     * Sets the 'shapes visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, Boolean flag) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="PlotOrientation" role="collaborator">
     * Used to indicate the orientation (horizontal or vertical) of a 2D plot.
public final class PlotOrientation implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -2508771828190337782L;
     * For a plot where the range axis is horizontal.
    public static final PlotOrientation HORIZONTAL = new PlotOrientation("PlotOrientation.HORIZONTAL");
     * For a plot where the range axis is vertical.
    public static final PlotOrientation VERTICAL = new PlotOrientation("PlotOrientation.VERTICAL");
     * The name.
    private String name;
     * Private constructor.
     * @param name  the name.
    private PlotOrientation(String name) { /* body withheld */ }
     * Returns a string representing the object.
     * @return The string.
    public String toString() { /* body withheld */ }
     * Returns <code>true</code> if this object is equal to the specified
     * object, and <code>false</code> otherwise.
     * @param obj  the object (<code>null</code> permitted).
     * @return A boolean.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this instance.
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Ensures that serialization returns the unique instances.
     * @return The object.
     * @throws ObjectStreamException if there is a problem.
    private Object readResolve() throws ObjectStreamException { /* body withheld */ }
}
</class>

<class name="RendererChangeEvent" role="collaborator">
     * An event that can be forwarded to any {@link RendererChangeListener} to
     * signal a change to a renderer.
public class RendererChangeEvent extends ChartChangeEvent {
     * The renderer that generated the event.
    private Object renderer;
     * A flag that indicates whether this event relates to a change in the
     * series visibility.  If so, the receiver (if it is a plot) may want to
     * update the axis bounds.
     * @since 1.0.13
    private boolean seriesVisibilityChanged;
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
    public RendererChangeEvent(Object renderer) { /* body withheld */ }
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
     * @param seriesVisibilityChanged  a flag that indicates whether or not
     * the event relates to a change in the series visibility flags.
    public RendererChangeEvent(Object renderer, boolean seriesVisibilityChanged) { /* body withheld */ }
     * Returns the renderer that generated the event.
     * @return The renderer that generated the event.
    public Object getRenderer() { /* body withheld */ }
     * Returns the flag that indicates whether or not the event relates to
     * a change in series visibility.
     * @return A boolean.
     * @since 1.0.13
    public boolean getSeriesVisibilityChanged() { /* body withheld */ }
}
</class>

<class name="CategorySeriesLabelGenerator" role="collaborator">
     * A generator that creates labels for the series in a {@link CategoryDataset}.
     * <P>
     * Classes that implement this interface should be either (a) immutable, or
     * (b) cloneable via the <code>PublicCloneable</code> interface (defined in
     * the JCommon class library).  This provides a mechanism for the referring
     * renderer to clone the generator if necessary.
public interface CategorySeriesLabelGenerator {
     * Generates a label for the specified series.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param series  the series index.
     * @return A series label.
    public String generateLabel(CategoryDataset dataset, int series);
}
</class>

<class name="CategoryDataset" role="collaborator">
     * The interface for a dataset with one or more series, and values associated
     * with categories.
     * <P>
     * The categories are represented by <code>Comparable</code> instance, with the
     * category label being provided by the <code>toString</code> method.
public interface CategoryDataset extends KeyedValues2D, Dataset {

}
</class>

<class name="CategoryToolTipGenerator" role="collaborator">
     * A <i>category tool tip generator</i> is an object that can be assigned to a
     * {@link org.jfree.chart.renderer.category.CategoryItemRenderer} and that
     * assumes responsibility for creating text items to be used as tooltips for the
     * items in a { /* body withheld */ }.
     * <p>
     * To assist with cloning charts, classes that implement this interface should
     * also implement the {@link PublicCloneable} interface.
public interface CategoryToolTipGenerator {
     * Generates the tool tip text for an item in a dataset.  Note: in the
     * current dataset implementation, each row is a series, and each column
     * contains values for a particular category.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The tooltip text (possibly <code>null</code>).
    public String generateToolTip(CategoryDataset dataset, int row, int column);
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getBaseLinesVisible`, `getBaseSeriesVisible`, `getBaseSeriesVisibleInLegend`, `getPassCount`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~798 chars)</summary>

```
// compute((CategoryPlot), (boolean), ()) : getPlot, getBaseLinesVisible, getBaseSeriesVisible, getBaseSeriesVisibleInLegend, getPassCount
public class ReferenceImpl {

    public static final class CategoryPlot {
    }

    public static CategoryPlot compute_getPlot(CategoryPlot plot) {
        return plot;
    }

    public static boolean compute_getBaseLinesVisible(boolean baseLinesVisible) {
        return baseLinesVisible;
    }

    public static boolean compute_getBaseSeriesVisible(boolean baseSeriesVisible) {
        return baseSeriesVisible;
    }

    public static boolean compute_getBaseSeriesVisibleInLegend(
            boolean baseSeriesVisibleInLegend) {
        return baseSeriesVisibleInLegend;
    }

    public static int compute_getPassCount() {
        return 1;
    }
}
```

</details>

---
## [57] ⚙️ reference-impl · `getPlot`
**output:** **reference generated**
- detail: {'chars': 798}

---
## [58] ⚙️ reference-impl · `getPlot`
**output:** **signature declared**
- detail: {'signature': '(CategoryPlot'}

---
## [59] ⚙️ reference-impl · `getPlot`
**output:** **reference omits the disputed observable — DISCARDED**
- reason: declared: [] — none normalizes to `getPlot` (accessor prefixes are stripped both ways)

---
## [60] ⚙️ reference-widening · `getPlot`
**output:** **widening REJECTED for observable `plot`**
- reason: reference omits the disputed observable — DISCARDED — declared: [] — none normalizes to `getPlot` (accessor prefixes are stripped both ways)
- detail: {'admitted_observables': [], 'last_step': 'reference omits the disputed observable — DISCARDED'}

---
## [61] ⚙️ reference-widening · `getBaseToolTipGenerator`
**output:** **reference REQUESTED for observable `basetooltipgenerator`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [62] ⚙️ reference-impl · `getBaseToolTipGenerator`
**output:** **widening target requested**
- detail: {'candidates': ['getBaseToolTipGenerator'], 'attempting': 'getBaseToolTipGenerator'}

---
## [63] ⚙️ reference-impl · `getBaseToolTipGenerator`
**output:** **screening surface resolved**
- reason: 2 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getBaseLinesVisible', 'getPassCount'], 'patch_touched_exempted': [], 'screened_siblings': ['getBaseLinesVisible', 'getPassCount'], 'declaring_types': ['AbstractCategoryItemRenderer', 'LineAndShapeRenderer']}

---
## [64] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~61,459 chars, 1 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
**[user]**
```
Implement `getBaseToolTipGenerator` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * { /* body withheld */ } you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() { /* body withheld */ }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { /* body withheld */ }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { /* body withheld */ }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { /* body withheld */ }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { /* body withheld */ }
     * Sets the item label generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the item label generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { /* body withheld */ }
     * Sets the base item label generator and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the base item label generator and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { /* body withheld */ }
     * Sets the URL generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { /* body withheld */ }
     * Sets the URL generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { /* body withheld */ }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * { /* body withheld */ } class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a { /* body withheld */ } to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a { /* body withheld */ } to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * { /* body withheld */ } method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void test2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        assertNotNull(r.getLegendItems());
        assertEquals(0, r.getLegendItems().getItemCount());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        assertEquals(0, r.getLegendItems().getItemCount());

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        assertEquals(1, lic.getItemCount());
        assertEquals("S1", lic.get(0).getLabel());
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * { /* body withheld */ } you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() { /* body withheld */ }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { /* body withheld */ }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { /* body withheld */ }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { /* body withheld */ }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { /* body withheld */ }
     * Sets the item label generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the item label generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { /* body withheld */ }
     * Sets the base item label generator and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { /* body withheld */ }
     * Sets the base item label generator and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the tool tip generator for a series and sends a
     * { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { /* body withheld */ }
     * Sets the base tool tip generator and sends a { /* body withheld */ }
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { /* body withheld */ }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { /* body withheld */ }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { /* body withheld */ }
     * Sets the URL generator for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { /* body withheld */ }
     * Sets the URL generator for a series and, if requested, sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { /* body withheld */ }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the { /* body withheld */ } method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * { /* body withheld */ } class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the { /* body withheld */ }, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a { /* body withheld */ } to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a { /* body withheld */ } to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a { /* body withheld */ } to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * { /* body withheld */ } method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryPlot" role="test-subject">
     * A general plotting class that uses data from a {@link CategoryDataset} and
     * renders each data item using a { /* body withheld */ }.
public class CategoryPlot extends Plot implements ValueAxisPlot, Pannable,
        Selectable, Zoomable, AnnotationChangeListener, RendererChangeListener,
        Cloneable, PublicCloneable, Serializable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LineAndShapeRenderer" role="test-subject">
     * A renderer that draws shapes for each data item, and lines between data
     * items (for use with the { /* body withheld */ } class).
     * The example shown here is generated by the <code>LineChartDemo1.java</code>
     * program included in the JFreeChart Demo Collection:
     * <br><br>
     * <img src="../../../../../images/LineAndShapeRendererSample.png"
     * alt="LineAndShapeRendererSample.png" />
public class LineAndShapeRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -197749519869226398L;
     * A table of flags that control (per series) whether or not lines are
     * visible.
    private BooleanList seriesLinesVisible;
     * A flag indicating whether or not lines are drawn between non-null
     * points.
    private boolean baseLinesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * visible.
    private BooleanList seriesShapesVisible;
     * The default value returned by the getShapeVisible() method.
    private boolean baseShapesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * filled.
    private BooleanList seriesShapesFilled;
     * The default value returned by the getShapeFilled() method.
    private boolean baseShapesFilled;
     * A flag that controls whether the fill paint is used for filling
     * shapes.
    private boolean useFillPaint;
     * A flag that controls whether outlines are drawn for shapes.
    private boolean drawOutlines;
     * A flag that controls whether the outline paint is used for drawing shape
     * outlines - if not, the regular series paint is used.
    private boolean useOutlinePaint;
     * A flag that controls whether or not the x-position for each item is
     * offset within the category according to the series.
     * @since 1.0.7
    private boolean useSeriesOffset;
     * The item margin used for series offsetting - this allows the positioning
     * to match the bar positions of the { /* body withheld */ } class.
     * @since 1.0.7
    private double itemMargin;
     * Creates a renderer with both lines and shapes visible by default.
    public LineAndShapeRenderer() { /* body withheld */ }
     * Creates a new renderer with lines and/or shapes visible.
     * @param lines  draw lines?
     * @param shapes  draw shapes?
    public LineAndShapeRenderer(boolean lines, boolean shapes) { /* body withheld */ }
     * Returns the flag used to control whether or not the line for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemLineVisible(int series, int item) { /* body withheld */ }
     * Returns the flag used to control whether or not the lines for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesLinesVisible(int, Boolean)
    public Boolean getSeriesLinesVisible(int series) { /* body withheld */ }
     * Sets the 'lines visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag (<code>null</code> permitted).
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, Boolean flag) { /* body withheld */ }
     * Sets the 'lines visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, boolean visible) { /* body withheld */ }
     * Returns the base 'lines visible' attribute.
     * @return The base flag.
     * @see #getBaseLinesVisible()
    public boolean getBaseLinesVisible() { /* body withheld */ }
     * Sets the base 'lines visible' flag and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param flag  the flag.
     * @see #getBaseLinesVisible()
    public void setBaseLinesVisible(boolean flag) { /* body withheld */ }
     * Returns the flag used to control whether or not the shape for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemShapeVisible(int series, int item) { /* body withheld */ }
     * Returns the flag used to control whether or not the shapes for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return A boolean.
     * @see #setSeriesShapesVisible(int, Boolean)
    public Boolean getSeriesShapesVisible(int series) { /* body withheld */ }
     * Sets the 'shapes visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, boolean visible) { /* body withheld */ }
     * Sets the 'shapes visible' flag for a series and sends a
     * { /* body withheld */ } to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, Boolean flag) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="PlotOrientation" role="collaborator">
     * Used to indicate the orientation (horizontal or vertical) of a 2D plot.
public final class PlotOrientation implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -2508771828190337782L;
     * For a plot where the range axis is horizontal.
    public static final PlotOrientation HORIZONTAL = new PlotOrientation("PlotOrientation.HORIZONTAL");
     * For a plot where the range axis is vertical.
    public static final PlotOrientation VERTICAL = new PlotOrientation("PlotOrientation.VERTICAL");
     * The name.
    private String name;
     * Private constructor.
     * @param name  the name.
    private PlotOrientation(String name) { /* body withheld */ }
     * Returns a string representing the object.
     * @return The string.
    public String toString() { /* body withheld */ }
     * Returns <code>true</code> if this object is equal to the specified
     * object, and <code>false</code> otherwise.
     * @param obj  the object (<code>null</code> permitted).
     * @return A boolean.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this instance.
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Ensures that serialization returns the unique instances.
     * @return The object.
     * @throws ObjectStreamException if there is a problem.
    private Object readResolve() throws ObjectStreamException { /* body withheld */ }
}
</class>

<class name="RendererChangeEvent" role="collaborator">
     * An event that can be forwarded to any {@link RendererChangeListener} to
     * signal a change to a renderer.
public class RendererChangeEvent extends ChartChangeEvent {
     * The renderer that generated the event.
    private Object renderer;
     * A flag that indicates whether this event relates to a change in the
     * series visibility.  If so, the receiver (if it is a plot) may want to
     * update the axis bounds.
     * @since 1.0.13
    private boolean seriesVisibilityChanged;
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
    public RendererChangeEvent(Object renderer) { /* body withheld */ }
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
     * @param seriesVisibilityChanged  a flag that indicates whether or not
     * the event relates to a change in the series visibility flags.
    public RendererChangeEvent(Object renderer, boolean seriesVisibilityChanged) { /* body withheld */ }
     * Returns the renderer that generated the event.
     * @return The renderer that generated the event.
    public Object getRenderer() { /* body withheld */ }
     * Returns the flag that indicates whether or not the event relates to
     * a change in series visibility.
     * @return A boolean.
     * @since 1.0.13
    public boolean getSeriesVisibilityChanged() { /* body withheld */ }
}
</class>

<class name="CategorySeriesLabelGenerator" role="collaborator">
     * A generator that creates labels for the series in a {@link CategoryDataset}.
     * <P>
     * Classes that implement this interface should be either (a) immutable, or
     * (b) cloneable via the <code>PublicCloneable</code> interface (defined in
     * the JCommon class library).  This provides a mechanism for the referring
     * renderer to clone the generator if necessary.
public interface CategorySeriesLabelGenerator {
     * Generates a label for the specified series.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param series  the series index.
     * @return A series label.
    public String generateLabel(CategoryDataset dataset, int series);
}
</class>

<class name="CategoryDataset" role="collaborator">
     * The interface for a dataset with one or more series, and values associated
     * with categories.
     * <P>
     * The categories are represented by <code>Comparable</code> instance, with the
     * category label being provided by the <code>toString</code> method.
public interface CategoryDataset extends KeyedValues2D, Dataset {

}
</class>

<class name="CategoryToolTipGenerator" role="collaborator">
     * A <i>category tool tip generator</i> is an object that can be assigned to a
     * {@link org.jfree.chart.renderer.category.CategoryItemRenderer} and that
     * assumes responsibility for creating text items to be used as tooltips for the
     * items in a { /* body withheld */ }.
     * <p>
     * To assist with cloning charts, classes that implement this interface should
     * also implement the {@link PublicCloneable} interface.
public interface CategoryToolTipGenerator {
     * Generates the tool tip text for an item in a dataset.  Note: in the
     * current dataset implementation, each row is a series, and each column
     * contains values for a particular category.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The tooltip text (possibly <code>null</code>).
    public String generateToolTip(CategoryDataset dataset, int row, int column);
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getBaseLinesVisible`, `getPassCount`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~569 chars)</summary>

```
// compute(CategoryToolTipGenerator, boolean) : getBaseToolTipGenerator, getBaseLinesVisible, getPassCount
import org.jfree.chart.labels.CategoryToolTipGenerator;

public class ReferenceImpl {

    public static CategoryToolTipGenerator compute_getBaseToolTipGenerator(
            CategoryToolTipGenerator baseToolTipGenerator) {
        return baseToolTipGenerator;
    }

    public static boolean compute_getBaseLinesVisible(boolean baseLinesVisible) {
        return baseLinesVisible;
    }

    public static int compute_getPassCount() {
        return 1;
    }
}
```

</details>

---
## [65] ⚙️ reference-impl · `getBaseToolTipGenerator`
**output:** **reference generated**
- detail: {'chars': 569}

---
## [66] ⚙️ reference-impl · `getBaseToolTipGenerator`
**output:** **signature declared**
- detail: {'signature': 'CategoryToolTipGenerator, boolean'}

---
## [67] ⚙️ reference-impl · `getBaseToolTipGenerator`
**output:** **reference observables matched**
- detail: {'matched': {'getBaseToolTipGenerator': 'getBaseToolTipGenerator', 'getBaseLinesVisible': 'getBaseLinesVisible', 'getPassCount': 'getPassCount'}, 'declared_only': []}

---
## [68] ⚙️ reference-impl · `getBaseToolTipGenerator`
**output:** **reference too thin to screen — DISCARDED**
- reason: 2 shared sibling observable(s) ['getBaseLinesVisible', 'getPassCount']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [69] ⚙️ reference-widening · `getBaseToolTipGenerator`
**output:** **widening REJECTED for observable `basetooltipgenerator`**
- reason: reference too thin to screen — DISCARDED — 2 shared sibling observable(s) ['getBaseLinesVisible', 'getPassCount']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [70] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~61,537 chars)</summary>

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
- Package: `org.jfree.chart.renderer.category` (`package org.jfree.chart.renderer.category;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_1_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
+++ /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
@@ -1794,7 +1794,7 @@
         }
         int index = this.plot.getIndexOf(this);
         CategoryDataset dataset = this.plot.getDataset(index);
-        if (dataset != null) {
+        if (dataset == null) {
             return result;
         }
         int seriesCount = dataset.getRowCount();

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.RenderingSource;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.plot.CategoryCrosshairState;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.text.TextUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.util.GradientPaintTransformer;
import org.jfree.chart.util.Layer;
import org.jfree.chart.util.LengthAdjustmentType;
import org.jfree.chart.util.ObjectList;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.RectangleAnchor;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.RectangleInsets;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.CategoryDatasetSelectionState;
import org.jfree.data.category.SelectableCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
</source_imports>

Function `getLegendItems`:
<signature>
public LegendItemCollection getLegendItems()
</signature>
<code>
    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `getLegendItems` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `getLegendItems`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public CategoryPlot getPlot()` (shared field(s): plot)
      doc: Returns the plot that the renderer has been assigned to (where <code>null</code> indicates that the renderer is not currently assigned to a plot). @return The plot (possibly <code>null</code>). @see #setPlot(CategoryPlot)
  - `public void setPlot(CategoryPlot plot)` (shared field(s): plot)
      doc: Sets the plot that the renderer has been assigned to.  This method is usually called by the {@link CategoryPlot}, in normal usage you shouldn't need to call this method directly. @param plot  the plot (<code>null</code> not permitted). @see #getPlot()
  - `protected void updateCrosshairValues(CategoryCrosshairState crosshairState, Comparable rowKey, Comparable columnKey, double value, int datasetIndex, double transX, double transY, PlotOrientation orientation)` (shared field(s): plot)
      doc: Considers the current (x, y) coordinate and updates the crosshair point if it meets the criteria (usually means the (x, y) coordinate is the closest to the anchor point so far). @param crosshairState  the crosshair state (<code>null</code> permitted, but the method does nothing in that case). @param rowKey  the row key…
  - `public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea, CategoryAxis domainAxis, ValueAxis rangeAxis, Layer layer, PlotRenderingInfo info)` (shared field(s): plot)
      doc: Draws all the annotations for the specified layer. @param g2  the graphics device. @param dataArea  the data area. @param domainAxis  the domain axis. @param rangeAxis  the range axis. @param layer  the layer. @param info  the plot rendering info. @since 1.2.0

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addAnnotation(CategoryAnnotation annotation) / (CategoryAnnotation annotation, Layer layer)
  setBaseItemLabelGenerator(CategoryItemLabelGenerator generator) / (CategoryItemLabelGenerator generator, boolean notify)
  setBaseToolTipGenerator(CategoryToolTipGenerator generator) / (CategoryToolTipGenerator generator,
            boolean notify)
  setBaseURLGenerator(CategoryURLGenerator generator) / (CategoryURLGenerator generator,
            boolean notify)
  setSeriesItemLabelGenerator(int series,
            CategoryItemLabelGenerator generator) / (int series,
            CategoryItemLabelGenerator generator, boolean notify)
  setSeriesToolTipGenerator(int series,
            CategoryToolTipGenerator generator) / (int series,
            CategoryToolTipGenerator generator, boolean notify)
  setSeriesURLGenerator(int series,
            CategoryURLGenerator generator) / (int series,
            CategoryURLGenerator generator, boolean notify)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  draw* family: drawAnnotations, drawBackground, drawDomainLine, drawDomainMarker, drawOutline, drawRangeLine, drawRangeMarker
  get

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `AbstractCategoryItemRendererTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests" method="test2947660">
    public void test2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        assertNotNull(r.getLegendItems());
        assertEquals(0, r.getLegendItems().getItemCount());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        assertEquals(0, r.getLegendItems().getItemCount());

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        assertEquals(1, lic.getItemCount());
        assertEquals("S1", lic.get(0).getLabel());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests::test2947660
junit.framework.AssertionFailedError: expected:<1> but was:<0>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
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
<relation name="legendItems-singleSeries-nonNullDataset" kind="invariant">
  // holds because: AbstractCategoryItemRenderer.getLegendItems() returns a (possibly empty) collection of legend items for the series that this renderer is responsible for drawing, never null; for a LineAndShapeRenderer attached to a plot with a non-null dataset containing one series, the failing test fixes the required observable that one legend item is produced for that series.
  // valid input: Build a real org.jfree.chart.renderer.category.LineAndShapeRenderer, a real org.jfree.chart.plot.CategoryPlot, and a real org.jfree.data.category.DefaultCategoryDataset. Use fuzzed non-empty-ish ASCII strings for the single row key and column key, attach dataset and renderer to the plot, then add exactly one value so the dataset is non-null and has exactly one series.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 20000/20000 on random inputs
org.jfree.chart.renderer.category.LineAndShapeRenderer r;
org.jfree.chart.plot.CategoryPlot plot;
org.jfree.data.category.DefaultCategoryDataset dataset;
String rowKey;
String columnKey;
try {
  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();
  plot = new org.jfree.chart.plot.CategoryPlot();
  dataset = new org.jfree.data.category.DefaultCategoryDataset();
  rowKey = data.consumeAsciiString(8);
  if (rowKey.length() == 0) rowKey = "S";
  columnKey = data.consumeAsciiString(8);
  if (columnKey.length() == 0) columnKey = "C";
  plot.setDataset(dataset);
  plot.setRenderer(r);
  dataset.addValue(1.0, rowKey, columnKey);
} catch (Exception e) {
  return;
}
org.jfree.chart.LegendItemCollection items;
try {
  items = r.getLegendItems();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation legendItems-singleSeries-nonNullDataset violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (items == null) {
  throw new RuntimeException("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
}
if (items.getItemCount() != 1) {
  throw new RuntimeException("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount());
}
String label = items.get(0).getLabel();
if (!rowKey.equals(label)) {
  throw new RuntimeException("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label);
}
</relation>
<relation name="legendItems-noPlot-emptyAndNonNull" kind="invariant">
  // holds because: AbstractCategoryItemRenderer.getLegendItems() returns the legend item collection never null; with no assigned plot there are no series for which this renderer is responsible, so the collection must be empty.
  // valid input: Construct a fresh org.jfree.chart.renderer.category.LineAndShapeRenderer and do not assign it to any plot.
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
org.jfree.chart.renderer.category.LineAndShapeRenderer r;
try {
  r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();
} catch (Exception e) {
  return;
}
org.jfree.chart.LegendItemCollection items;
try {
  items = r.getLegendItems();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation legendItems-noPlot-emptyAndNonNull violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (items == null) {
  throw new RuntimeException("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
}
if (items.getItemCount() != 0) {
  throw new RuntimeException("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items.getItemCount());
}
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
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,989 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    private static String nonEmptyAscii(FuzzedDataProvider data, String fallback) {
        String s = data.consumeAsciiString(8);
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r1;
        LegendItemCollection noPlotItems;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection singleSeriesItems;
        CategoryPlot observedPlotAfterAttach;
        CategoryPlot observedPlotAfterReads;
        String firstLabel;

        try {
            r1 = new LineAndShapeRenderer();

            noPlotItems = r1.getLegendItems();

            plot1 = new CategoryPlot();
            dataset1 = new DefaultCategoryDataset();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);

            observedPlotAfterAttach = r1.getPlot();
            emptyDatasetItems = r1.getLegendItems();

            dataset1.addValue(1.0, "S1", "C1");
            singleSeriesItems = r1.getLegendItems();
            observedPlotAfterReads = r1.getPlot();
            firstLabel = singleSeriesItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null-no-plot] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-no-plot] semantic mismatch: expected 0 but actual was " + noPlotItems.getItemCount());
        }
        if (observedPlotAfterAttach != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree] semantic mismatch: setRenderer/setPlot established plot identity, but getPlot() returned a different object");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected 0 but actual was " + emptyDatasetItems.getItemCount());
        }
        if (singleSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but actual was " + singleSeriesItems.getItemCount());
        }
        if (!"S1".equals(firstLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-label] semantic mismatch: expected S1 but actual was " + firstLabel);
        }
        /* Contract justification: getLegendItems() is a read/query method ("Returns the legend items...")
           over renderer/plot state, so repeated reads on unchanged state must not silently detach the plot.
           A throw-deleting or branch-skipping patch that corrupts/ignores state would violate this. */
        if (observedPlotAfterReads != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-plot-stable] semantic mismatch: getLegendItems() changed observable plot state");
        }

        String rowKey = nonEmptyAscii(data, "S");
        String columnKey = nonEmptyAscii(data, "C");
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection itemsA;
        LegendItemCollection itemsB;
        CategoryPlot beforeReadPlot;
        CategoryPlot afterReadPlot;
        String labelA;
        String labelB;

        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            beforeReadPlot = r2.getPlot();

            dataset2.addValue(value, rowKey, columnKey);

            itemsA = r2.getLegendItems();
            itemsB = r2.getLegendItems();
            afterReadPlot = r2.getPlot();
            labelA = itemsA.get(0).getLabel();
            labelB = itemsB.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: for a real LineAndShapeRenderer attached to a plot with a non-null dataset
           containing exactly one series, getLegendItems() must report that one series; this is fixed by the
           lifted JUnit oracle and generalised by constructing the single known series ourselves. */
        if (itemsA == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-null] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (itemsA.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-count] semantic mismatch: expected 1 but actual was " + itemsA.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (!rowKey.equals(labelA)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-label] semantic mismatch: expected " + rowKey + " but actual was " + labelA + " columnKey=" + columnKey);
        }

        /* Metamorphic/post-condition justification: getLegendItems() is a read-only query over unchanged state,
           so two consecutive calls must agree on the observable legend count/label, and the plot written by
           CategoryPlot.setRenderer(r) must still be returned by getPlot(). */
        if (itemsB == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-null] semantic mismatch: second getLegendItems() returned null");
        }
        if (itemsB.getItemCount() != itemsA.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-count] metamorphic violation: repeated getLegendItems() on unchanged state disagreed: first=" + itemsA.getItemCount() + " second=" + itemsB.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (!labelA.equals(labelB)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-label] metamorphic violation: repeated getLegendItems() on unchanged state disagreed: first=" + labelA + " second=" + labelB + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (beforeReadPlot != plot2 || afterReadPlot != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree-generalized] metamorphic violation: getPlot()/setPlot state disagreed with attached plot");
        }

        LineAndShapeRenderer r3;
        LegendItemCollection noPlotGeneral;
        try {
            r3 = new LineAndShapeRenderer();
            noPlotGeneral = r3.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: without an assigned plot there are no series this renderer is responsible for,
           so getLegendItems() must return a non-null empty collection. */
        if (noPlotGeneral == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-emptyAndNonNull-null] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (noPlotGeneral.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-emptyAndNonNull-count] semantic mismatch: expected 0 but actual was " + noPlotGeneral.getItemCount());
        }
    }
}
```

</details>

---
## [71] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [72] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,862 chars, 2 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    private static String nonEmptyAscii(FuzzedDataProvider data, String fallback) {
        String s = data.consumeAsciiString(8);
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r1;
        LegendItemCollection noPlotItems;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection singleSeriesItems;
        CategoryPlot observedPlotAfterAttach;
        CategoryPlot observedPlotAfterReads;
        String firstLabel;

        try {
            r1 = new LineAndShapeRenderer();

            noPlotItems = r1.getLegendItems();

            plot1 = new CategoryPlot();
            dataset1 = new DefaultCategoryDataset();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);

            observedPlotAfterAttach = r1.getPlot();
            emptyDatasetItems = r1.getLegendItems();

            dataset1.addValue(1.0, "S1", "C1");
            singleSeriesItems = r1.getLegendItems();
            observedPlotAfterReads = r1.getPlot();
            firstLabel = singleSeriesItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null-no-plot] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-no-plot] semantic mismatch: expected 0 but actual was " + noPlotItems.getItemCount());
        }
        if (observedPlotAfterAttach != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree] semantic mismatch: setRenderer/setPlot established plot identity, but getPlot() returned a different object");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected 0 but actual was " + emptyDatasetItems.getItemCount());
        }
        if (singleSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but actual was " + singleSeriesItems.getItemCount());
        }
        if (!"S1".equals(firstLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-label] semantic mismatch: expected S1 but actual was " + firstLabel);
        }
        /* Contract justification: getLegendItems() is a read/query method ("Returns the legend items...")
           over renderer/plot state, so repeated reads on unchanged state must not silently detach the plot.
           A throw-deleting or branch-skipping patch that corrupts/ignores state would violate this. */
        if (observedPlotAfterReads != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-plot-stable] semantic mismatch: getLegendItems() changed observable plot state");
        }

        String rowKey = nonEmptyAscii(data, "S");
        String columnKey = nonEmptyAscii(data, "C");
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection itemsA;
        LegendItemCollection itemsB;
        CategoryPlot beforeReadPlot;
        CategoryPlot afterReadPlot;
        String labelA;
        String labelB;

        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            beforeReadPlot = r2.getPlot();

            dataset2.addValue(value, rowKey, columnKey);

            itemsA = r2.getLegendItems();
            itemsB = r2.getLegendItems();
            afterReadPlot = r2.getPlot();
            labelA = itemsA.get(0).getLabel();
            labelB = itemsB.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: for a real LineAndShapeRenderer attached to a plot with a non-null dataset
           containing exactly one series, getLegendItems() must report that one series; this is fixed by the
           lifted JUnit oracle and generalised by constructing the single known series ourselves. */
        if (itemsA == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-null] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (itemsA.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-count] semantic mismatch: expected 1 but actual was " + itemsA.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (!rowKey.equals(labelA)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-label] semantic mismatch: expected " + rowKey + " but actual was " + labelA + " columnKey=" + columnKey);
        }

        /* Metamorphic/post-condition justification: getLegendItems() is a read-only query over unchanged state,
           so two consecutive calls must agree on the observable legend count/label, and the plot written by
           CategoryPlot.setRenderer(r) must still be returned by getPlot(). */
        if (itemsB == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-null] semantic mismatch: second getLegendItems() returned null");
        }
        if (itemsB.getItemCount() != itemsA.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-count] metamorphic violation: repeated getLegendItems() on unchanged state disagreed: first=" + itemsA.getItemCount() + " second=" + itemsB.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (!labelA.equals(labelB)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-label] metamorphic violation: repeated getLegendItems() on unchanged state disagreed: first=" + labelA + " second=" + labelB + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (beforeReadPlot != plot2 || afterReadPlot != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree-generalized] metamorphic violation: getPlot()/setPlot state disagreed with attached plot");
        }

        LineAndShapeRenderer r3;
        LegendItemCollection noPlotGeneral;
        try {
            r3 = new LineAndShapeRenderer();
            noPlotGeneral = r3.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: without an assigned plot there are no series this renderer is responsible for,
           so getLegendItems() must return a non-null empty collection. */
        if (noPlotGeneral == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-emptyAndNonNull-null] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (noPlotGeneral.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-emptyAndNonNull-count] semantic mismatch: expected 0 but actual was " + noPlotGeneral.getItemCount());
        }
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
+++ /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
@@ -1794,7 +1794,7 @@
         }
         int index = this.plot.getIndexOf(this);
         CategoryDataset dataset = this.plot.getDataset(index);
-        if (dataset != null) {
+        if (dataset == null) {
             return result;
         }
         int seriesCount = dataset.getRowCount();


Your last harness did NOT reach any project code on the buggy version that we could observe — it likely threw or returned before getting into the changed method. First priority: construct an input that actually calls the patched method with arguments that get past its initial validation, so execution reaches the changed lines at all.

To make the next harness crash the BUGGY version, work through this:

  1. From the diff, identify the exact behavioural difference between buggy and fixed code — which inputs are handled differently. The crash you want exists only on the buggy side, so target inputs that hit that difference. Do not assume the bug is any particular shape (missing bounds check, wrong branch, off-by-one, null handling, etc.) — read the diff and let it tell you.
  2. Choose input that reaches the changed code AND drives it into the faulting state. Use the FuzzedDataProvider to produce values in the range that exercises the difference, not arbitrary values that are likely rejected early.
  3. Do NOT rely on an exception that BOTH versions throw (e.g. validation that exists in buggy and fixed alike). That fires on the patched code too and so cannot distinguish them. You want a fault that the buggy code reaches and the fixed code prevents.
  4. CRITICAL — let the crash escape. Do NOT wrap the call in a try/catch that swallows the throwable you want Jazzer to report. Catch ONLY checked exceptions the signature forces you to handle, and rethrow them as RuntimeException; let every unchecked exception propagate out of fuzzerTestOneInput so Jazzer can see it.

Return the full corrected FuzzHarness.java — raw Java only, no fences, public class FuzzHarness, entrypoint exactly
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
```

</details>
<details open><summary>▸ Output (~3,581 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    private static void fail(String id, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection lic0 = r.getLegendItems();
        if (lic0 == null) {
            fail("test2947660-not-null", "expected non-null legend items without plot but actual was null");
        }
        if (lic0.getItemCount() != 0) {
            fail("test2947660-no-plot-count", "expected 0 but actual was " + lic0.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getPlot() != plot) {
            fail("plot-writer-reader-agree", "expected getPlot() to return the same plot instance assigned by CategoryPlot.setRenderer()");
        }

        LegendItemCollection lic1 = r.getLegendItems();
        if (lic1 == null) {
            fail("test2947660-empty-dataset-not-null", "expected non-null legend items with empty dataset but actual was null");
        }
        if (lic1.getItemCount() != 0) {
            fail("test2947660-empty-dataset-count", "expected 0 but actual was " + lic1.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");

        LegendItemCollection lic2 = r.getLegendItems();
        if (lic2 == null) {
            fail("test2947660-single-series-not-null", "expected non-null legend items after adding one series but actual was null");
        }
        if (lic2.getItemCount() != 1) {
            fail("test2947660-single-series-count", "expected 1 but actual was " + lic2.getItemCount());
        }
        String label = lic2.get(0).getLabel();
        if (!"S1".equals(label)) {
            fail("test2947660-single-series-label", "expected S1 but actual was " + label);
        }

        /* Contract/post-condition: getLegendItems() is a read/query over renderer state; repeated reads on
           unchanged state must agree, and must not disturb the plot link established by CategoryPlot.setRenderer().
           A patch that merely skips work or corrupts bookkeeping can evade one assertion but violate this. */
        LegendItemCollection lic3 = r.getLegendItems();
        if (lic3 == null) {
            fail("legendItems-repeat-not-null", "expected non-null legend items on repeated read but actual was null");
        }
        if (lic3.getItemCount() != lic2.getItemCount()) {
            fail("legendItems-repeat-count", "expected repeated getLegendItems() count " + lic2.getItemCount() + " but actual was " + lic3.getItemCount());
        }
        if (!lic2.get(0).getLabel().equals(lic3.get(0).getLabel())) {
            fail("legendItems-repeat-label", "expected repeated getLegendItems() label " + lic2.get(0).getLabel() + " but actual was " + lic3.get(0).getLabel());
        }
        if (r.getPlot() != plot) {
            fail("plot-stable-after-read", "expected getLegendItems() not to change the assigned plot");
        }
    }
}
```

</details>

---
## [73] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-series-count] semantic mismatch: expected 1 but actual was 0

---
## [74] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~71,530 chars, 1 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.jfree.chart.renderer.category` (`package org.jfree.chart.renderer.category;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_1_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
+++ /source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
@@ -1794,7 +1794,7 @@
         }
         int index = this.plot.getIndexOf(this);
         CategoryDataset dataset = this.plot.getDataset(index);
-        if (dataset != null) {
+        if (dataset == null) {
             return result;
         }
         int seriesCount = dataset.getRowCount();

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.RenderingSource;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.plot.CategoryCrosshairState;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.text.TextUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.util.GradientPaintTransformer;
import org.jfree.chart.util.Layer;
import org.jfree.chart.util.LengthAdjustmentType;
import org.jfree.chart.util.ObjectList;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.RectangleAnchor;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.RectangleInsets;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.CategoryDatasetSelectionState;
import org.jfree.data.category.SelectableCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
</source_imports>

Function `getLegendItems`:
<signature>
public LegendItemCollection getLegendItems()
</signature>
<code>
    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `getLegendItems` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `getLegendItems`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public CategoryPlot getPlot()` (shared field(s): plot)
      doc: Returns the plot that the renderer has been assigned to (where <code>null</code> indicates that the renderer is not currently assigned to a plot). @return The plot (possibly <code>null</code>). @see #setPlot(CategoryPlot)
  - `public void setPlot(CategoryPlot plot)` (shared field(s): plot)
      doc: Sets the plot that the renderer has been assigned to.  This method is usually called by the {@link CategoryPlot}, in normal usage you shouldn't need to call this method directly. @param plot  the plot (<code>null</code> not permitted). @see #getPlot()
  - `protected void updateCrosshairValues(CategoryCrosshairState crosshairState, Comparable rowKey, Comparable columnKey, double value, int datasetIndex, double transX, double transY, PlotOrientation orientation)` (shared field(s): plot)
      doc: Considers the current (x, y) coordinate and updates the crosshair point if it meets the criteria (usually means the (x, y) coordinate is the closest to the anchor point so far). @param crosshairState  the crosshair state (<code>null</code> permitted, but the method does nothing in that case). @param rowKey  the row key…
  - `public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea, CategoryAxis domainAxis, ValueAxis rangeAxis, Layer layer, PlotRenderingInfo info)` (shared field(s): plot)
      doc: Draws all the annotations for the specified layer. @param g2  the graphics device. @param dataArea  the data area. @param domainAxis  the domain axis. @param rangeAxis  the range axis. @param layer  the layer. @param info  the plot rendering info. @since 1.2.0

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="AbstractCategoryItemRenderer" role="patched">
     * An abstract base class that you can use to implement a new
     * {@link CategoryItemRenderer}.  When you create a new
     * {@link CategoryItemRenderer} you are not required to extend this class,
     * but it makes the job easier.
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {
     * For serialization.
    private static final long serialVersionUID = 1247553218442497391L;
     * The plot that the renderer is assigned to.
    private CategoryPlot plot;
     * A list of item label generators (one per series).
    private ObjectList itemLabelGeneratorList;
     * The base item label generator.
    private CategoryItemLabelGenerator baseItemLabelGenerator;
     * A list of tool tip generators (one per series).
    private ObjectList toolTipGeneratorList;
     * The base tool tip generator.
    private CategoryToolTipGenerator baseToolTipGenerator;
     * A list of label generators (one per series).
    private ObjectList urlGeneratorList;
     * The base label generator.
    private CategoryURLGenerator baseURLGenerator;
     * The legend item label generator.
    private CategorySeriesLabelGenerator legendItemLabelGenerator;
     * The legend item tool tip generator.
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;
     * The legend item URL generator.
    private CategorySeriesLabelGenerator legendItemURLGenerator;
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     * @since 1.2.0
    private List backgroundAnnotations;
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     * @since 1.2.0
    private List foregroundAnnotations;
     * The number of rows in the dataset (temporary record).
    private transient int rowCount;
     * The number of columns in the dataset (temporary record).
    private transient int columnCount;
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
    protected AbstractCategoryItemRenderer() {
        this.itemLabelGeneratorList = new ObjectList();
        this.toolTipGeneratorList = new ObjectList();
        this.urlGeneratorList = new ObjectList();
        this.legendItemLabelGenerator
                = new StandardCategorySeriesLabelGenerator();
        this.backgroundAnnotations = new ArrayList();
        this.foregroundAnnotations = new ArrayList();
    }
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     * @return The pass count.
    public int getPassCount() { … }
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot() { … }
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the {@link CategoryPlot}, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot) { … }
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) { … }
     * Returns the item label generator for a series.
     * @param series  the series index (zero based).
     * @return The generator (possibly <code>null</code>).
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) { … }
     * Sets the item label generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) { … }
     * Sets the item label generator for a series and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesItemLabelGenerator(int)
    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) { … }
     * Returns the base item label generator.
     * @return The generator (possibly <code>null</code>).
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() { … }
     * Sets the base item label generator and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator) { … }
     * Sets the base item label generator and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseItemLabelGenerator()
    public void setBaseItemLabelGenerator( CategoryItemLabelGenerator generator, boolean notify) { … }
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     * @return The generator (possibly <code>null</code>).
     * @since 1.2.0
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) { … }
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     * @param series  the series index (zero-based).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) { … }
     * Sets the tool tip generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) { … }
     * Sets the tool tip generator for a series and sends a
     * {@link org.jfree.chart.event.RendererChangeEvent} to all registered
     * listeners.
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesToolTipGenerator(int)
    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) { … }
     * Returns the base tool tip generator (the "layer 2" generator).
     * @return The tool tip generator (possibly <code>null</code>).
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
    public CategoryToolTipGenerator getBaseToolTipGenerator() { … }
     * Sets the base tool tip generator and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) { … }
     * Sets the base tool tip generator and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getBaseToolTipGenerator()
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) { … }
     * Returns the URL generator for a data item.
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     * @return The URL generator.
     * @since 1.2.0
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) { … }
     * Returns the URL generator for a series.
     * @param series  the series index (zero based).
     * @return The URL generator for the series.
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
    public CategoryURLGenerator getSeriesURLGenerator(int series) { … }
     * Sets the URL generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator.
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) { … }
     * Sets the URL generator for a series and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @since 1.2.0
     * @see #getSeriesURLGenerator(int)
    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) { … }
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     * @return The legend item collection (never <code>null</code>).
     * @see #getLegendItem(int, int)
    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AbstractRenderer" role="supertype-contract">
     * Base class providing common services for renderers.  Most methods that update
     * attributes of the renderer will fire a {@link RendererChangeEvent}, which
     * normally means the plot that owns the renderer will receive notification that
     * the renderer has been changed (the plot will, in turn, notify the chart).
public abstract class AbstractRenderer implements Cloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -828267569428206075L;
     * Zero represented as a <code>Double</code>.
    public static final Double ZERO = new Double(0.0);
     * The default paint.
    public static final Paint DEFAULT_PAINT = Color.blue;
     * The default outline paint.
    public static final Paint DEFAULT_OUTLINE_PAINT = Color.gray;
     * The default stroke.
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
     * The default outline stroke.
    public static final Stroke DEFAULT_OUTLINE_STROKE = new BasicStroke(1.0f);
     * The default shape.
    public static final Shape DEFAULT_SHAPE = new Rectangle2D.Double(-3.0, -3.0, 6.0, 6.0);
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default value label paint.
    public static final Paint DEFAULT_VALUE_LABEL_PAINT = Color.black;
     * A list of flags that controls whether or not each series is visible.
    private BooleanList seriesVisibleList;
     * The default visibility for each series.
    private boolean baseSeriesVisible;
     * A list of flags that controls whether or not each series is visible in
     * the legend.
    private BooleanList seriesVisibleInLegendList;
     * The default visibility for each series in the legend.
    private boolean baseSeriesVisibleInLegend;
     * The paint list.
    private PaintList paintList;
     * A flag that controls whether or not the paintList is auto-populated
     * in the {@link #lookupSeriesPaint(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesPaint;
     * The base paint.
    private transient Paint basePaint;
     * The fill paint list.
    private PaintList fillPaintList;
     * A flag that controls whether or not the fillPaintList is auto-populated
     * in the {@link #lookupSeriesFillPaint(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesFillPaint;
     * The base fill paint.
    private transient Paint baseFillPaint;
     * The outline paint list.
    private PaintList outlinePaintList;
     * A flag that controls whether or not the outlinePaintList is
     * auto-populated in the {@link #lookupSeriesOutlinePaint(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlinePaint;
     * The base outline paint.
    private transient Paint baseOutlinePaint;
     * The stroke list.
    private StrokeList strokeList;
     * A flag that controls whether or not the strokeList is auto-populated
     * in the {@link #lookupSeriesStroke(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesStroke;
     * The base stroke.
    private transient Stroke baseStroke;
     * The outline stroke list.
    private StrokeList outlineStrokeList;
     * The base outline stroke.
    private transient Stroke baseOutlineStroke;
     * A flag that controls whether or not the outlineStrokeList is
     * auto-populated in the {@link #lookupSeriesOutlineStroke(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesOutlineStroke;
     * A shape list.
    private ShapeList shapeList;
     * A flag that controls whether or not the shapeList is auto-populated
     * in the {@link #lookupSeriesShape(int)} method.
     * @since 1.0.6
    private boolean autoPopulateSeriesShape;
     * The base shape.
    private transient Shape baseShape;
     * Visibility of the item labels PER series.
    private BooleanList itemLabelsVisibleList;
     * The base item labels visible.
    private boolean baseItemLabelsVisible;
     * The item label font list (one font per series).
    private ObjectList itemLabelFontList;
     * The base item label font.
    private Font baseItemLabelFont;
     * The item label paint list (one paint per series).
    private PaintList itemLabelPaintList;
     * The base item label paint.
    private transient Paint baseItemLabelPaint;
     * The positive item label position (per series).
    private ObjectList positiveItemLabelPositionList;
     * The fallback positive item label position.
    private ItemLabelPosition basePositiveItemLabelPosition;
     * The negative item label position (per series).
    private ObjectList negativeItemLabelPositionList;
     * The fallback negative item label position.
    private ItemLabelPosition baseNegativeItemLabelPosition;
     * The item label anchor offset.
    private double itemLabelAnchorOffset = 2.0;
     * Flags that control whether or not entities are generated for each
     * series.  This will be overridden by 'createEntities'.
    private BooleanList createEntitiesList;
     * The default flag that controls whether or not entities are generated.
     * This flag is used when both the above flags return null.
    private boolean baseCreateEntities;
     * The per-series legend shape settings.
     * @since 1.0.11
    private ShapeList legendShapeList;
     * The base shape for legend items.  If this is <code>null</code>, the
     * series shape will be used.
     * @since 1.0.11
    private transient Shape baseLegendShape;
     * A special flag that, if true, will cause the getLegendItem() method
     * to configure the legend shape as if it were a line.
     * @since 1.0.14
    private boolean treatLegendShapeAsLine;
     * The per-series legend text font.
     * @since 1.0.11
    private ObjectList legendTextFont;
     * The base legend font.
     * @since 1.0.11
    private Font baseLegendTextFont;
     * Default constructor.
    public AbstractRenderer() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryItemRenderer" role="supertype-contract">
     * A plug-in object that is used by the {@link CategoryPlot} class to display
     * individual data items from a {@link CategoryDataset}.
     * <p>
     * This interface defines the methods that must be provided by all renderers.
     * If you are implementing a custom renderer, you should consider extending the
     * {@link AbstractCategoryItemRenderer} class.
     * <p>
     * Most renderer attributes are defined using a "per-series" approach with
     * a base (default) value to cover cases where no per-series value is
     * defined.
public interface CategoryItemRenderer extends LegendItemSource {
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     * @return The plot (possibly <code>null</code>).
     * @see #setPlot(CategoryPlot)
    public CategoryPlot getPlot();
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the {@link CategoryPlot}, in normal usage you
     * shouldn't need to call this method directly.
     * @param plot  the plot (<code>null</code> not permitted).
     * @see #getPlot()
    public void setPlot(CategoryPlot plot);
     * Returns the number of passes through the dataset required by the
     * renderer.  Usually this will be one, but some renderers may use
     * a second or third pass to overlay items on top of things that were
     * drawn in an earlier pass.
     * @return The pass count.
    public int getPassCount();
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     * @param dataset  the dataset (<code>null</code> permitted).
     * @return The range (or <code>null</code> if the dataset is
     * <code>null</code> or empty).
    public Range findRangeBounds(CategoryDataset dataset);
     * Adds a change listener.
     * @param listener  the listener.
     * @see #removeChangeListener(RendererChangeListener)
    public void addChangeListener(RendererChangeListener listener);
     * Removes a change listener.
     * @param listener  the listener.
     * @see #addChangeListener(RendererChangeListener)
    public void removeChangeListener(RendererChangeListener listener);
     * Returns a boolean that indicates whether or not the specified item
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @param item  the item index.
     * @return A boolean.
    public boolean getItemVisible(int series, int item);
     * Returns a boolean that indicates whether or not the specified series
     * should be drawn (this is typically used to hide an entire series).
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisible(int series);
     * Returns the flag that controls whether a series is visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisible(int, Boolean)
    public Boolean getSeriesVisible(int series);
     * Sets the flag that controls whether a series is visible and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible);
     * Sets the flag that controls whether a series is visible and, if
     * requested, sends a {@link RendererChangeEvent} to all registered
     * listeners.
     * @param series  the series index.
     * @param visible  the flag (<code>null</code> permitted).
     * @param notify  notify listeners?
     * @see #getSeriesVisible(int)
    public void setSeriesVisible(int series, Boolean visible, boolean notify);
     * Returns the base visibility for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisible(boolean)
    public boolean getBaseSeriesVisible();
     * Sets the base visibility and sends a {@link RendererChangeEvent} to all
     * registered listeners.
     * @param visible  the flag.
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible);
     * Sets the base visibility and, if requested, sends
     * a {@link RendererChangeEvent} to all registered listeners.
     * @param visible  the visibility.
     * @param notify  notify listeners?
     * @see #getBaseSeriesVisible()
    public void setBaseSeriesVisible(boolean visible, boolean notify);
     * Returns <code>true</code> if the series should be shown in the legend,
     * and <code>false</code> otherwise.
     * @param series  the series index.
     * @return A boolean.
    public boolean isSeriesVisibleInLegend(int series);
     * Returns the flag that controls whether a series is visible in the
     * legend.  This method returns only the "per series" settings - to
     * do a lookup that falls back to the default value, you need to use the
     * {@link #isSeriesVisibleInLegend(int)} method.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesVisibleInLegend(int, Boolean)
    public Boolean getSeriesVisibleInLegend(int series);
     * Sets the flag that controls whether a series is visible in the legend
     * and sends a {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag (<code>null</code> permitted).
     * @see #getSeriesVisibleInLegend(int)
    public void setSeriesVisibleInLegend(int series, Boolean visible);
     * Returns the base visibility in the legend for all series.
     * @return The base visibility.
     * @see #setBaseSeriesVisibleInLegend(boolean)
    public boolean getBaseSeriesVisibleInLegend();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DefaultCategoryDataset" role="test-subject">
     * A default implementation of the {@link CategoryDataset} interface.
public class DefaultCategoryDataset extends AbstractCategoryDataset
        implements CategoryDataset, SelectableCategoryDataset, 
        CategoryDatasetSelectionState, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -8168173757291644622L;
     * A storage structure for the data.
    private KeyedObjects2D data;
     * Creates a new (empty) dataset.
    public DefaultCategoryDataset() {
        this.data = new KeyedObjects2D();
        // FIXME: will need to remove this later, because it should be optional
        setSelectionState(this);
    }
     * Returns the number of rows in the table.
     * @return The row count.
     * @see #getColumnCount()
    public int getRowCount() { … }
     * Returns the number of columns in the table.
     * @return The column count.
     * @see #getRowCount()
    public int getColumnCount() { … }
     * Returns a value from the table.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The value (possibly <code>null</code>).
     * @see #addValue(Number, Comparable, Comparable)
     * @see #removeValue(Comparable, Comparable)
    public Number getValue(int row, int column) { … }
     * Returns the key for the specified row.
     * @param row  the row index (zero-based).
     * @return The row key.
     * @see #getRowIndex(Comparable)
     * @see #getRowKeys()
     * @see #getColumnKey(int)
    public Comparable getRowKey(int row) { … }
     * Returns the row index for a given key.
     * @param key  the row key (<code>null</code> not permitted).
     * @return The row index.
     * @see #getRowKey(int)
    public int getRowIndex(Comparable key) { … }
     * Returns the row keys.
     * @return The keys.
     * @see #getRowKey(int)
    public List getRowKeys() { … }
     * Returns a column key.
     * @param column  the column index (zero-based).
     * @return The column key.
     * @see #getColumnIndex(Comparable)
    public Comparable getColumnKey(int column) { … }
     * Returns the column index for a given key.
     * @param key  the column key (<code>null</code> not permitted).
     * @return The column index.
     * @see #getColumnKey(int)
    public int getColumnIndex(Comparable key) { … }
     * Returns the column keys.
     * @return The keys.
     * @see #getColumnKey(int)
    public List getColumnKeys() { … }
     * Returns the value for a pair of keys.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @return The value (possibly <code>null</code>).
     * @throws UnknownKeyException if either key is not defined in the dataset.
     * @see #addValue(Number, Comparable, Comparable)
    public Number getValue(Comparable rowKey, Comparable columnKey) { … }
     * Adds a value to the table.  Performs the same function as setValue().
     * @param value  the value.
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @see #getValue(Comparable, Comparable)
     * @see #removeValue(Comparable, Comparable)
    public void addValue(Number value, Comparable rowKey, Comparable columnKey) { … }
     * Adds a value to the table.
     * @param value  the value.
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @see #getValue(Comparable, Comparable)
    public void addValue(double value, Comparable rowKey, Comparable columnKey) { … }
     * Adds or updates a value in the table and sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     * @param value  the value (<code>null</code> permitted).
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @see #getValue(Comparable, Comparable)
    public void setValue(Number value, Comparable rowKey, Comparable columnKey) { … }
     * Adds or updates a value in the table and sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     * @param value  the value.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @see #getValue(Comparable, Comparable)
    public void setValue(double value, Comparable rowKey, Comparable columnKey) { … }
     * Adds the specified value to an existing value in the dataset (if the
     * existing value is <code>null</code>, it is treated as if it were 0.0).
     * @param value  the value.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @throws UnknownKeyException if either key is not defined in the dataset.
    public void incrementValue(double value, Comparable rowKey, Comparable columnKey) { … }
     * Removes a value from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * @param rowKey  the row key (<code>null</code> not permitted).
     * @param columnKey  the column key (<code>null</code> not permitted).
     * @see #addValue(Number, Comparable, Comparable)
     * @throws UnknownKeyException if either key is not in the dataset.
    public void removeValue(Comparable rowKey, Comparable columnKey) { … }
     * Removes a row from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * @param rowIndex  the row index.
     * @see #removeColumn(int)
    public void removeRow(int rowIndex) { … }
     * Removes a row from the dataset and sends a {@link DatasetChangeEvent}
     * to all registered listeners.
     * @param rowKey  the row key.
     * @see #removeColumn(Comparable)
    public void removeRow(Comparable rowKey) { … }
    public boolean isSelected(int row, int column) { … }
    public void clearSelection() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="CategoryPlot" role="test-subject">
     * A general plotting class that uses data from a {@link CategoryDataset} and
     * renders each data item using a {@link CategoryItemRenderer}.
public class CategoryPlot extends Plot implements ValueAxisPlot, Pannable,
        Selectable, Zoomable, AnnotationChangeListener, RendererChangeListener,
        Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -3537691700434728188L;
     * The default visibility of the grid lines plotted against the domain
     * axis.
    public static final boolean DEFAULT_DOMAIN_GRIDLINES_VISIBLE = false;
     * The default visibility of the grid lines plotted against the range
     * axis.
    public static final boolean DEFAULT_RANGE_GRIDLINES_VISIBLE = true;
     * The default grid line stroke.
    public static final Stroke DEFAULT_GRIDLINE_STROKE = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] { … }
     * The default grid line paint.
    public static final Paint DEFAULT_GRIDLINE_PAINT = Color.WHITE;
     * The default value label font.
    public static final Font DEFAULT_VALUE_LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
     * The default crosshair visibility.
     * @since 1.0.5
    public static final boolean DEFAULT_CROSSHAIR_VISIBLE = false;
     * The default crosshair stroke.
     * @since 1.0.5
    public static final Stroke DEFAULT_CROSSHAIR_STROKE = DEFAULT_GRIDLINE_STROKE;
     * The default crosshair paint.
     * @since 1.0.5
    public static final Paint DEFAULT_CROSSHAIR_PAINT = Color.blue;
     * The resourceBundle for the localization.
    protected static ResourceBundle localizationResources = ResourceBundleWrapper.getBundle( "org.jfree.chart.plot.LocalizationBundle");
     * The plot orientation.
    private PlotOrientation orientation;
     * The offset between the data area and the axes.
    private RectangleInsets axisOffset;
     * Storage for the domain axes.
    private ObjectList domainAxes;
     * Storage for the domain axis locations.
    private ObjectList domainAxisLocations;
     * A flag that controls whether or not the shared domain axis is drawn
     * (only relevant when the plot is being used as a subplot).
    private boolean drawSharedDomainAxis;
     * Storage for the range axes.
    private ObjectList rangeAxes;
     * Storage for the range axis locations.
    private ObjectList rangeAxisLocations;
     * Storage for the datasets.
    private ObjectList datasets;
     * Storage for keys that map datasets to domain axes.
    private TreeMap datasetToDomainAxesMap;
     * Storage for keys that map datasets to range axes.
    private TreeMap datasetToRangeAxesMap;
     * Storage for the renderers.
    private ObjectList renderers;
     * The dataset rendering order.
    private DatasetRenderingOrder renderingOrder = DatasetRenderingOrder.REVERSE;
     * Controls the order in which the columns are traversed when rendering the
     * data items.
    private SortOrder columnRenderingOrder = SortOrder.ASCENDING;
     * Controls the order in which the rows are traversed when rendering the
     * data items.
    private SortOrder rowRenderingOrder = SortOrder.ASCENDING;
     * A flag that controls whether the grid-lines for the domain axis are
     * visible.
    private boolean domainGridlinesVisible;
     * The position of the domain gridlines relative to the category.
    private CategoryAnchor domainGridlinePosition;
     * The stroke used to draw the domain grid-lines.
    private transient Stroke domainGridlineStroke;
     * The paint used to draw the domain  grid-lines.
    private transient Paint domainGridlinePaint;
     * A flag that controls whether or not the zero baseline against the range
     * axis is visible.
     * @since 1.0.13
    private boolean rangeZeroBaselineVisible;
     * The stroke used for the zero baseline against the range axis.
     * @since 1.0.13
    private transient Stroke rangeZeroBaselineStroke;
     * The paint used for the zero baseline against the range axis.
     * @since 1.0.13
    private transient Paint rangeZeroBaselinePaint;
     * A flag that controls whether the grid-lines for the range axis are
     * visible.
    private boolean rangeGridlinesVisible;
     * The stroke used to draw the range axis grid-lines.
    private transient Stroke rangeGridlineStroke;
     * The paint used to draw the range axis grid-lines.
    private transient Paint rangeGridlinePaint;
     * A flag that controls whether or not gridlines are shown for the minor
     * tick values on the primary range axis.
     * @since 1.0.13
    private boolean rangeMinorGridlinesVisible;
     * The stroke used to draw the range minor grid-lines.
     * @since 1.0.13
    private transient Stroke rangeMinorGridlineStroke;
     * The paint used to draw the range minor grid-lines.
     * @since 1.0.13
    private transient Paint rangeMinorGridlinePaint;
     * The anchor value.
    private double anchorValue;
     * The index for the dataset that the crosshairs are linked to (this
     * determines which axes the crosshairs are plotted against).
     * @since 1.0.11
    private int crosshairDatasetIndex;
     * A flag that controls the visibility of the domain crosshair.
     * @since 1.0.11
    private boolean domainCrosshairVisible;
     * The row key for the crosshair point.
     * @since 1.0.11
    private Comparable domainCrosshairRowKey;
     * The column key for the crosshair point.
     * @since 1.0.11
    private Comparable domainCrosshairColumnKey;
     * The stroke used to draw the domain crosshair if it is visible.
     * @since 1.0.11
    private transient Stroke domainCrosshairStroke;
     * The paint used to draw the domain crosshair if it is visible.
     * @since 1.0.11
    private transient Paint domainCrosshairPaint;
     * A flag that controls whether or not a range crosshair is drawn.
    private boolean rangeCrosshairVisible;
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LineAndShapeRenderer" role="test-subject">
     * A renderer that draws shapes for each data item, and lines between data
     * items (for use with the {@link CategoryPlot} class).
     * The example shown here is generated by the <code>LineChartDemo1.java</code>
     * program included in the JFreeChart Demo Collection:
     * <br><br>
     * <img src="../../../../../images/LineAndShapeRendererSample.png"
     * alt="LineAndShapeRendererSample.png" />
public class LineAndShapeRenderer extends AbstractCategoryItemRenderer
        implements Cloneable, PublicCloneable, Serializable {
     * For serialization.
    private static final long serialVersionUID = -197749519869226398L;
     * A table of flags that control (per series) whether or not lines are
     * visible.
    private BooleanList seriesLinesVisible;
     * A flag indicating whether or not lines are drawn between non-null
     * points.
    private boolean baseLinesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * visible.
    private BooleanList seriesShapesVisible;
     * The default value returned by the getShapeVisible() method.
    private boolean baseShapesVisible;
     * A table of flags that control (per series) whether or not shapes are
     * filled.
    private BooleanList seriesShapesFilled;
     * The default value returned by the getShapeFilled() method.
    private boolean baseShapesFilled;
     * A flag that controls whether the fill paint is used for filling
     * shapes.
    private boolean useFillPaint;
     * A flag that controls whether outlines are drawn for shapes.
    private boolean drawOutlines;
     * A flag that controls whether the outline paint is used for drawing shape
     * outlines - if not, the regular series paint is used.
    private boolean useOutlinePaint;
     * A flag that controls whether or not the x-position for each item is
     * offset within the category according to the series.
     * @since 1.0.7
    private boolean useSeriesOffset;
     * The item margin used for series offsetting - this allows the positioning
     * to match the bar positions of the {@link BarRenderer} class.
     * @since 1.0.7
    private double itemMargin;
     * Creates a renderer with both lines and shapes visible by default.
    public LineAndShapeRenderer() {
        this(true, true);
    }
     * Creates a new renderer with lines and/or shapes visible.
     * @param lines  draw lines?
     * @param shapes  draw shapes?
    public LineAndShapeRenderer(boolean lines, boolean shapes) {
        super();
        this.seriesLinesVisible = new BooleanList();
        this.baseLinesVisible = lines;
        this.seriesShapesVisible = new BooleanList();
        this.baseShapesVisible = shapes;
        this.seriesShapesFilled = new BooleanList();
        this.baseShapesFilled = true;
        this.useFillPaint = false;
        this.drawOutlines = true;
        this.useOutlinePaint = false;
        this.useSeriesOffset = false;  // preserves old behaviour
        this.itemMargin = 0.0;
    }
     * Returns the flag used to control whether or not the line for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemLineVisible(int series, int item) { … }
     * Returns the flag used to control whether or not the lines for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return The flag (possibly <code>null</code>).
     * @see #setSeriesLinesVisible(int, Boolean)
    public Boolean getSeriesLinesVisible(int series) { … }
     * Sets the 'lines visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag (<code>null</code> permitted).
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, Boolean flag) { … }
     * Sets the 'lines visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesLinesVisible(int)
    public void setSeriesLinesVisible(int series, boolean visible) { … }
     * Returns the base 'lines visible' attribute.
     * @return The base flag.
     * @see #getBaseLinesVisible()
    public boolean getBaseLinesVisible() { … }
     * Sets the base 'lines visible' flag and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param flag  the flag.
     * @see #getBaseLinesVisible()
    public void setBaseLinesVisible(boolean flag) { … }
     * Returns the flag used to control whether or not the shape for an item is
     * visible.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @return A boolean.
    public boolean getItemShapeVisible(int series, int item) { … }
     * Returns the flag used to control whether or not the shapes for a series
     * are visible.
     * @param series  the series index (zero-based).
     * @return A boolean.
     * @see #setSeriesShapesVisible(int, Boolean)
    public Boolean getSeriesShapesVisible(int series) { … }
     * Sets the 'shapes visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param visible  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, boolean visible) { … }
     * Sets the 'shapes visible' flag for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     * @param series  the series index (zero-based).
     * @param flag  the flag.
     * @see #getSeriesShapesVisible(int)
    public void setSeriesShapesVisible(int series, Boolean flag) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="PlotOrientation" role="collaborator">
     * Used to indicate the orientation (horizontal or vertical) of a 2D plot.
public final class PlotOrientation implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -2508771828190337782L;
     * For a plot where the range axis is horizontal.
    public static final PlotOrientation HORIZONTAL = new PlotOrientation("PlotOrientation.HORIZONTAL");
     * For a plot where the range axis is vertical.
    public static final PlotOrientation VERTICAL = new PlotOrientation("PlotOrientation.VERTICAL");
     * The name.
    private String name;
     * Private constructor.
     * @param name  the name.
    private PlotOrientation(String name) {
        this.name = name;
    }
     * Returns a string representing the object.
     * @return The string.
    public String toString() { … }
     * Returns <code>true</code> if this object is equal to the specified
     * object, and <code>false</code> otherwise.
     * @param obj  the object (<code>null</code> permitted).
     * @return A boolean.
    public boolean equals(Object obj) { … }
     * Returns a hash code for this instance.
     * @return A hash code.
    public int hashCode() { … }
     * Ensures that serialization returns the unique instances.
     * @return The object.
     * @throws ObjectStreamException if there is a problem.
    private Object readResolve() throws ObjectStreamException { … }
}
</class>

<class name="RendererChangeEvent" role="collaborator">
     * An event that can be forwarded to any {@link RendererChangeListener} to
     * signal a change to a renderer.
public class RendererChangeEvent extends ChartChangeEvent {
     * The renderer that generated the event.
    private Object renderer;
     * A flag that indicates whether this event relates to a change in the
     * series visibility.  If so, the receiver (if it is a plot) may want to
     * update the axis bounds.
     * @since 1.0.13
    private boolean seriesVisibilityChanged;
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
    public RendererChangeEvent(Object renderer) {
        this(renderer, false);
    }
     * Creates a new event.
     * @param renderer  the renderer that generated the event.
     * @param seriesVisibilityChanged  a flag that indicates whether or not
     * the event relates to a change in the series visibility flags.
    public RendererChangeEvent(Object renderer, boolean seriesVisibilityChanged) {
        super(renderer);
        this.renderer = renderer;
        this.seriesVisibilityChanged = seriesVisibilityChanged;
    }
     * Returns the renderer that generated the event.
     * @return The renderer that generated the event.
    public Object getRenderer() { … }
     * Returns the flag that indicates whether or not the event relates to
     * a change in series visibility.
     * @return A boolean.
     * @since 1.0.13
    public boolean getSeriesVisibilityChanged() { … }
}
</class>

<class name="CategorySeriesLabelGenerator" role="collaborator">
     * A generator that creates labels for the series in a {@link CategoryDataset}.
     * <P>
     * Classes that implement this interface should be either (a) immutable, or
     * (b) cloneable via the <code>PublicCloneable</code> interface (defined in
     * the JCommon class library).  This provides a mechanism for the referring
     * renderer to clone the generator if necessary.
public interface CategorySeriesLabelGenerator {
     * Generates a label for the specified series.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param series  the series index.
     * @return A series label.
    public String generateLabel(CategoryDataset dataset, int series);
}
</class>

<class name="CategoryDataset" role="collaborator">
     * The interface for a dataset with one or more series, and values associated
     * with categories.
     * <P>
     * The categories are represented by <code>Comparable</code> instance, with the
     * category label being provided by the <code>toString</code> method.
public interface CategoryDataset extends KeyedValues2D, Dataset {

}
</class>

<class name="CategoryToolTipGenerator" role="collaborator">
     * A <i>category tool tip generator</i> is an object that can be assigned to a
     * {@link org.jfree.chart.renderer.category.CategoryItemRenderer} and that
     * assumes responsibility for creating text items to be used as tooltips for the
     * items in a {@link org.jfree.chart.plot.CategoryPlot}.
     * <p>
     * To assist with cloning charts, classes that implement this interface should
     * also implement the {@link PublicCloneable} interface.
public interface CategoryToolTipGenerator {
     * Generates the tool tip text for an item in a dataset.  Note: in the
     * current dataset implementation, each row is a series, and each column
     * contains values for a particular category.
     * @param dataset  the dataset (<code>null</code> not permitted).
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @return The tooltip text (possibly <code>null</code>).
    public String generateToolTip(CategoryDataset dataset, int row, int column);
}
</class>
</class_skeletons>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addAnnotation(CategoryAnnotation annotation) / (CategoryAnnotation annotation, Layer layer)
  setBaseItemLabelGenerator(CategoryItemLabelGenerator generator) / (CategoryItemLabelGenerator generator, boolean notify)
  setBaseToolTipGenerator(CategoryToolTipGenerator generator) / (CategoryToolTipGenerator generator,
            boolean notify)
  setBaseURLGenerator(CategoryURLGenerator generator) / (CategoryURLGenerator generator,
            boolean notify)
  setSeriesItemLabelGenerator(int series,
            CategoryItemLabelGenerator generator) / (int series,
            CategoryItemLabelGenerator generator, boolean notify)
  setSeriesToolTipGenerator(int series,
            CategoryToolTipGenerator generator) / (int series,
            CategoryToolTipGenerator generator, boolean notify)
  setSeriesURLGenerator(int series,
            CategoryURLGenerator generator) / (int series,
            CategoryURLGenerator generator, boolean notify)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  draw* family: drawAnnotations, drawBackground, drawDomainLine, drawDomainMarker, drawOutline, drawRangeLine, drawRangeMarker
  get

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `AbstractCategoryItemRendererTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests" method="test2947660">
    public void test2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        assertNotNull(r.getLegendItems());
        assertEquals(0, r.getLegendItems().getItemCount());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        assertEquals(0, r.getLegendItems().getItemCount());

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        assertEquals(1, lic.getItemCount());
        assertEquals("S1", lic.get(0).getLabel());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests::test2947660
junit.framework.AssertionFailedError: expected:<1> but was:<0>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
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
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,120 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        boolean lines = data.consumeBoolean();
        boolean shapes = data.consumeBoolean();

        AbstractCategoryItemRenderer r;
        try {
            r = new LineAndShapeRenderer(lines, shapes);
        } catch (Throwable t) {
            return;
        }

        try {
            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() expected non-null but was null");
            }
            if (initial.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-initial] semantic mismatch: expected 0 but was " + initial.getItemCount());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        try {
            plot.setDataset(dataset);
            plot.setRenderer(r);
        } catch (Throwable t) {
            return;
        }

        try {
            LegendItemCollection withEmptyDataset = r.getLegendItems();
            if (withEmptyDataset.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot] semantic mismatch: expected 0 but was " + withEmptyDataset.getItemCount());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Contract from getPlot()/setPlot(): the renderer reports the plot it has been assigned to.
            // This shared-state check ensures the plot field consulted by getLegendItems agrees with the assigned plot.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: expected renderer plot identity to match assigned plot");
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-count-one] semantic mismatch: expected 1 but was " + lic.getItemCount());
            }
            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-label] semantic mismatch: expected S1 but was " + String.valueOf(label));
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Metamorphic/post-condition check: getLegendItems() is a getter over renderer/plot/dataset state,
            // so repeated calls without intervening mutation must agree on observable contents.
            // A patch that silently drops or corrupts bookkeeping can violate this even if no exception is thrown.
            LegendItemCollection first = r.getLegendItems();
            LegendItemCollection second = r.getLegendItems();
            int c1 = first.getItemCount();
            int c2 = second.getItemCount();
            String l1 = c1 > 0 ? first.get(0).getLabel() : null;
            String l2 = c2 > 0 ? second.get(0).getLabel() : null;
            if (c1 != c2 || (l1 == null ? l2 != null : !l1.equals(l2))) {
                throw new RuntimeException("[oracle:getter-idempotence] metamorphic violation: repeated getLegendItems() calls disagreed count1=" + c1 + " count2=" + c2 + " label1=" + String.valueOf(l1) + " label2=" + String.valueOf(l2));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        if (data.remainingBytes() > 0) {
            try {
                int extraSeries = data.consumeInt(0, 3);
                for (int i = 0; i < extraSeries; i++) {
                    String row = data.consumeAsciiString(8);
                    String col = data.consumeAsciiString(8);
                    if (row.length() == 0) {
                        row = "R" + i;
                    }
                    if (col.length() == 0) {
                        col = "K" + i;
                    }
                    dataset.addValue((double) data.consumeInt(-1000, 1000), row, col);
                    r.getLegendItems();
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
## [75] ⚙️ harness-repair · `attempt_003`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [76] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-count-one] semantic mismatch: expected 1 but was 0

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,121 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r0;
        LegendItemCollection items0;
        try {
            r0 = new LineAndShapeRenderer();
            items0 = r0.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-notnull] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected 0 but actual=" + items0.getItemCount());
        }

        LineAndShapeRenderer r1;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection items1;
        try {
            r1 = new LineAndShapeRenderer();
            dataset1 = new DefaultCategoryDataset();
            plot1 = new CategoryPlot();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);
            items1 = r1.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: plot writer/reader disagree expectedSame=true actualSame=false");
        }
        if (items1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-attached-notnull] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-attached-empty] semantic mismatch: expected 0 but actual=" + items1.getItemCount());
        }

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection items2;
        try {
            r2 = new LineAndShapeRenderer();
            dataset2 = new DefaultCategoryDataset();
            plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(1.0, "S1", "C1");
            items2 = r2.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-notnull] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-itemcount] semantic mismatch: expected 1 but actual=" + items2.getItemCount());
        }
        String label2;
        try {
            label2 = items2.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!"S1".equals(label2)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label2));
        }

        LineAndShapeRenderer r3;
        CategoryPlot plot3;
        DefaultCategoryDataset dataset3;
        LegendItemCollection beforeRead;
        LegendItemCollection afterRead;
        String fuzzRow;
        String fuzzCol;
        try {
            r3 = new LineAndShapeRenderer();
            plot3 = new CategoryPlot();
            dataset3 = new DefaultCategoryDataset();
            fuzzRow = data.consumeAsciiString(8);
            if (fuzzRow.length() == 0) {
                fuzzRow = "S";
            }
            fuzzCol = data.consumeAsciiString(8);
            if (fuzzCol.length() == 0) {
                fuzzCol = "C";
            }
            plot3.setDataset(dataset3);
            plot3.setRenderer(r3);
            dataset3.addValue(1.0, fuzzRow, fuzzCol);
            beforeRead = r3.getLegendItems();
            afterRead = r3.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (beforeRead == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (beforeRead.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + beforeRead.getItemCount());
        }
        String fuzzLabel;
        try {
            fuzzLabel = beforeRead.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!fuzzRow.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + fuzzRow + " but got " + fuzzLabel);
        }
        if (afterRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-second-call-notnull] semantic mismatch: expected non-null legend collection on repeated read but got null");
        }
        if (afterRead.getItemCount() != beforeRead.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-repeated-call] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset must agree on itemCount lhs=" + beforeRead.getItemCount() + " rhs=" + afterRead.getItemCount());
        }
        String fuzzLabel2;
        try {
            fuzzLabel2 = afterRead.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!fuzzLabel.equals(fuzzLabel2)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-repeated-label] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset must agree on first label lhs=" + fuzzLabel + " rhs=" + fuzzLabel2);
        }
        if (r3.getPlot() != plot3) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-after-read] semantic mismatch: getLegendItems() should not disturb assigned plot expectedSame=true actualSame=false");
        }

        LineAndShapeRenderer r4;
        LegendItemCollection items4;
        try {
            r4 = new LineAndShapeRenderer();
            items4 = r4.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items4 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items4.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items4.getItemCount());
        }

        LineAndShapeRenderer r5;
        CategoryPlot plot5;
        DefaultCategoryDataset dataset5;
        LegendItemCollection items5a;
        LegendItemCollection items5b;
        try {
            r5 = new LineAndShapeRenderer();
            plot5 = new CategoryPlot();
            dataset5 = new DefaultCategoryDataset();
            plot5.setDataset(dataset5);
            plot5.setRenderer(r5);
            dataset5.addValue(1.0, "S1", "C1");
            items5a = r5.getLegendItems();
            dataset5.addValue(2.0, "S1", "C2");
            items5b = r5.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items5a == null || items5b == null) {
            throw new FuzzerSecurityIssueLow("[oracle:same-series-extra-column-notnull] semantic mismatch: expected non-null legend collections after valid dataset updates");
        }
        /*
         * Contract justification: getLegendItems() reports legend items for series.
         * Adding another value in the same existing series ("S1") changes columns, not series count,
         * so a correct implementation must still report exactly one legend item with the same label.
         * A patch that merely skips the real bookkeeping/path can silently return 0 or stale data here.
         */
        if (items5a.getItemCount() != 1 || items5b.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:same-series-extra-column-count] metamorphic violation: adding a second column in the same series must preserve one legend item before=" + items5a.getItemCount() + " after=" + items5b.getItemCount());
        }
        String l5a;
        String l5b;
        try {
            l5a = items5a.get(0).getLabel();
            l5b = items5b.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!"S1".equals(l5a) || !"S1".equals(l5b)) {
            throw new FuzzerSecurityIssueLow("[oracle:same-series-extra-column-label] metamorphic violation: adding a second column in the same series must preserve legend label before=" + l5a + " after=" + l5b);
        }
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-itemcount] semantic mismatch: expected 1 but actual=0

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,495 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-notnull] semantic mismatch: getLegendItems() returned null");
            }
            if (initial.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initial.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            // Contract from getPlot()/setPlot() docs: getPlot() returns the plot the renderer
            // has been assigned to. This shared-state check is mandatory because a patch that
            // simply short-circuits getLegendItems() could still leave plot state inconsistent.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: renderer plot disagrees with assigned plot");
            }

            LegendItemCollection emptyOnPlot = r.getLegendItems();
            if (emptyOnPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
            }
            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
            }

            // Metamorphic/post-condition: with unchanged plot/dataset/renderer state, repeated
            // getLegendItems() calls must report the same observable legend content. The method
            // "Returns a (possibly empty) collection of legend items for the series that this
            // renderer is responsible for drawing"; deleting the real logic or replacing it with
            // a wrong constant result would violate this stable-content relation.
            LegendItemCollection lic2 = r.getLegendItems();
            if (lic2.getItemCount() != lic.getItemCount()) {
                throw new RuntimeException("[oracle:legend-idempotence] metamorphic violation: repeated getLegendItems() changed item count lhs=" + lic.getItemCount() + " rhs=" + lic2.getItemCount());
            }
            if (lic2.getItemCount() > 0) {
                String label2 = lic2.get(0).getLabel();
                if (!String.valueOf(label).equals(String.valueOf(label2))) {
                    throw new RuntimeException("[oracle:legend-idempotence] metamorphic violation: repeated getLegendItems() changed first label lhs=" + String.valueOf(label) + " rhs=" + String.valueOf(label2));
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Additional real-code exploration through the same public API without untrusted
            // assertions. This still reaches the patched method on varied states.
            AbstractCategoryItemRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            int rows = data.consumeInt(0, 3);
            int cols = data.consumeInt(0, 3);
            for (int i = 0; i < rows; i++) {
                String rowKey = data.consumeAsciiString(8);
                if (rowKey.length() == 0) {
                    rowKey = "R" + i;
                }
                for (int j = 0; j < cols; j++) {
                    String colKey = data.consumeAsciiString(8);
                    if (colKey.length() == 0) {
                        colKey = "C" + j;
                    }
                    dataset2.addValue(data.consumeInt(-1000, 1000), rowKey, colKey);
                }
            }

            r2.getLegendItems();
            if (data.consumeBoolean()) {
                r2.setPlot(plot2);
                if (r2.getPlot() != plot2) {
                    throw new FuzzerSecurityIssueLow("[oracle:plot-roundtrip-extra] semantic mismatch: renderer plot disagrees after direct setPlot()");
                }
                r2.getLegendItems();
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
## [80] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-one-series-count] semantic mismatch: expected=1 actual=0

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,482 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r0;
        LegendItemCollection items0;
        try {
            r0 = new LineAndShapeRenderer();
            items0 = r0.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-initial] semantic mismatch: getLegendItems() returned null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-initial] semantic mismatch: expected=0 actual=" + items0.getItemCount());
        }

        AbstractCategoryItemRenderer r1;
        DefaultCategoryDataset dataset1;
        CategoryPlot plot1;
        LegendItemCollection items1;
        try {
            r1 = new LineAndShapeRenderer();
            dataset1 = new DefaultCategoryDataset();
            plot1 = new CategoryPlot();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);
            items1 = r1.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree] semantic mismatch: set renderer on plot but renderer.getPlot() did not return that plot");
        }
        if (items1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-empty-dataset] semantic mismatch: getLegendItems() returned null");
        }
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected=0 actual=" + items1.getItemCount());
        }

        AbstractCategoryItemRenderer r2;
        DefaultCategoryDataset dataset2;
        CategoryPlot plot2;
        LegendItemCollection items2;
        try {
            r2 = new LineAndShapeRenderer();
            dataset2 = new DefaultCategoryDataset();
            plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(1.0, "S1", "C1");
            items2 = r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-singleton] semantic mismatch: getLegendItems() returned null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-singleton-count] semantic mismatch: expected=1 actual=" + items2.getItemCount());
        }
        String label2;
        try {
            label2 = items2.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(label2)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-singleton-label] semantic mismatch: expected=S1 actual=" + label2);
        }

        LineAndShapeRenderer r3;
        LegendItemCollection items3 = null;
        Throwable ex3 = null;
        try {
            r3 = new LineAndShapeRenderer();
            items3 = r3.getLegendItems();
        } catch (Throwable t) {
            ex3 = t;
        }
        if (ex3 != null) {
            return;
        }
        if (items3 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items3.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expectedCount=0 actualCount=" + items3.getItemCount());
        }

        LineAndShapeRenderer r4;
        CategoryPlot plot4;
        DefaultCategoryDataset dataset4;
        LegendItemCollection items4 = null;
        Throwable ex4 = null;
        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        double value = data.consumeInt(-1000000, 1000000);
        try {
            r4 = new LineAndShapeRenderer();
            plot4 = new CategoryPlot();
            dataset4 = new DefaultCategoryDataset();
            plot4.setDataset(dataset4);
            plot4.setRenderer(r4);
            dataset4.addValue(value, rowKey, columnKey);
            items4 = r4.getLegendItems();

            // Contract/post-condition: getLegendItems() is a query for the series this renderer is responsible for.
            // For a renderer attached to a plot with exactly one dataset row, deleting the effectful path or guarding it away
            // would silently return the wrong collection; the observable post-state is one legend item labeled with that row key.
            if (r4.getPlot() != plot4) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-writer-reader-agree] semantic mismatch: expected renderer plot identity to match assigned plot");
            }
        } catch (Throwable t) {
            ex4 = t;
        }
        if (ex4 != null) {
            return;
        }
        if (items4 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items4.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expectedCount=1 actualCount=" + items4.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label4;
        try {
            label4 = items4.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!rowKey.equals(label4)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expectedLabel=" + rowKey + " actualLabel=" + label4 + " columnKey=" + columnKey);
        }

        LineAndShapeRenderer r5;
        CategoryPlot plot5;
        DefaultCategoryDataset dataset5;
        LegendItemCollection firstRead = null;
        LegendItemCollection secondRead = null;
        Throwable ex5 = null;
        String rowKey2 = data.consumeAsciiString(8);
        if (rowKey2.length() == 0) {
            rowKey2 = "S2";
        }
        String columnKey2 = data.consumeAsciiString(8);
        if (columnKey2.length() == 0) {
            columnKey2 = "C2";
        }
        try {
            r5 = new LineAndShapeRenderer();
            plot5 = new CategoryPlot();
            dataset5 = new DefaultCategoryDataset();
            plot5.setDataset(dataset5);
            plot5.setRenderer(r5);
            dataset5.addValue(1.0, rowKey2, columnKey2);
            firstRead = r5.getLegendItems();
            secondRead = r5.getLegendItems();
        } catch (Throwable t) {
            ex5 = t;
        }
        if (ex5 != null) {
            return;
        }
        if (firstRead == null || secondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-idempotence] semantic mismatch: repeated getLegendItems() returned null");
        }
        if (firstRead.getItemCount() != secondRead.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-idempotence] metamorphic violation: repeated getLegendItems() changed count first=" + firstRead.getItemCount() + " second=" + secondRead.getItemCount());
        }
        if (firstRead.getItemCount() == 1 && secondRead.getItemCount() == 1) {
            String l1;
            String l2;
            try {
                l1 = firstRead.get(0).getLabel();
                l2 = secondRead.get(0).getLabel();
            } catch (Throwable t) {
                return;
            }
            if (!l1.equals(l2)) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-idempotence] metamorphic violation: repeated getLegendItems() changed label first=" + l1 + " second=" + l2);
            }
        }
    }
}
```

</details>

---
## [82] ⚙️ harness-repair · `attempt_006`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [83] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-singleton-count] semantic mismatch: expected=1 actual=0

---
## [84] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,923 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-null-initial] semantic mismatch: getLegendItems() returned null before plot assignment");
            }
            if (initial.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-initial] semantic mismatch: expected initial legend item count 0 but was " + initial.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            /* Shared-state oracle: CategoryItemRenderer#getPlot()/setPlot() are documented
             * as accessors for the same assigned plot field, and CategoryPlot#setRenderer(r)
             * establishes that assignment in normal usage. A patch that merely suppresses or
             * bypasses getLegendItems() behavior can still leave this shared state inconsistent. */
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: renderer plot accessor disagrees with plot.setRenderer assignment");
            }

            LegendItemCollection emptyOnPlot = r.getLegendItems();
            if (emptyOnPlot == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-null-emptyplot] semantic mismatch: getLegendItems() returned null with empty dataset");
            }
            if (emptyOnPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot] semantic mismatch: expected legend item count 0 for empty dataset but was " + emptyOnPlot.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-null-single] semantic mismatch: getLegendItems() returned null after adding one series");
            }
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-count-single] semantic mismatch: expected legend item count 1 but was " + lic.getItemCount());
            }
            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-label-single] semantic mismatch: expected legend label S1 but was " + label);
            }

            /* Mandatory post-condition/metamorphic check: getLegendItems() is a pure reader over
             * renderer/plot/dataset state and should not mutate that state. Therefore, with no
             * intervening mutation, repeated calls must agree. A throw-deleting or branch-skipping
             * patch could return a stale/wrong collection while never throwing; this idempotence
             * check catches such silent wrong-output behavior through real library calls only. */
            LegendItemCollection lic2 = r.getLegendItems();
            if (lic2 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-nonnull] metamorphic violation: repeated getLegendItems() returned null on second call");
            }
            if (lic2.getItemCount() != lic.getItemCount()) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-count] metamorphic violation: repeated getLegendItems() changed item count lhs=" + lic.getItemCount() + " rhs=" + lic2.getItemCount());
            }
            if (lic.getItemCount() > 0) {
                String label2 = lic2.get(0).getLabel();
                if (label == null ? label2 != null : !label.equals(label2)) {
                    throw new FuzzerSecurityIssueLow("[oracle:idempotent-label] metamorphic violation: repeated getLegendItems() changed first label lhs=" + label + " rhs=" + label2);
                }
            }

            String fuzzSeries = data.consumeString(16);
            String fuzzCategory = data.consumeString(16);
            if (fuzzSeries.length() == 0) {
                fuzzSeries = "FS";
            }
            if (fuzzCategory.length() == 0) {
                fuzzCategory = "FC";
            }
            double fuzzValue = data.consumeInt(-1000000, 1000000);

            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            AbstractCategoryItemRenderer r2 = new LineAndShapeRenderer();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            if (r2.getPlot() != plot2) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: renderer plot accessor disagrees with second plot.setRenderer assignment");
            }

            dataset2.addValue(fuzzValue, fuzzSeries, fuzzCategory);
            LegendItemCollection oneSeries = r2.getLegendItems();

            /* Generalization by construction from a known answer: we construct a dataset with
             * exactly one non-empty row key and one value. getLegendItems() iterates dataset rows,
             * so with one visible series the trusted expected count is 1, and the default legend
             * labeling observed in the lifted test must expose that sole series key as the label. */
            if (oneSeries == null) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-null] semantic mismatch: getLegendItems() returned null for constructed one-series dataset");
            }
            if (oneSeries.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-count] semantic mismatch: expected constructed legend item count 1 but was " + oneSeries.getItemCount() + " series=" + fuzzSeries + " category=" + fuzzCategory);
            }
            String constructedLabel = oneSeries.get(0).getLabel();
            if (!fuzzSeries.equals(constructedLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-label] semantic mismatch: expected constructed legend label " + fuzzSeries + " but was " + constructedLabel);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [85] ⚙️ harness-repair · `attempt_007`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [86] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-count-single] semantic mismatch: expected legend item count 1 but was 0

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,994 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer exactRenderer;
        LegendItemCollection initialItems;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection oneSeriesItems;

        try {
            exactRenderer = new LineAndShapeRenderer();

            initialItems = exactRenderer.getLegendItems();

            DefaultCategoryDataset exactDataset = new DefaultCategoryDataset();
            CategoryPlot exactPlot = new CategoryPlot();
            exactPlot.setDataset(exactDataset);
            exactPlot.setRenderer(exactRenderer);

            emptyDatasetItems = exactRenderer.getLegendItems();

            exactDataset.addValue(1.0, "S1", "C1");
            oneSeriesItems = exactRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (initialItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (initialItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initialItems.getItemCount());
        }
        if (emptyDatasetItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-empty] semantic mismatch: expected=0 actual=" + emptyDatasetItems.getItemCount());
        }
        if (oneSeriesItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (oneSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-count] semantic mismatch: expected=1 actual=" + oneSeriesItems.getItemCount());
        }
        String exactLabel = oneSeriesItems.get(0).getLabel();
        if (!"S1".equals(exactLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-label] semantic mismatch: expected=S1 actual=" + exactLabel);
        }

        LineAndShapeRenderer stateRenderer;
        CategoryPlot statePlot;
        LegendItemCollection stateItems1;
        LegendItemCollection stateItems2;
        int beforeRows;
        int afterRows;
        String stateLabel1;
        String stateLabel2;
        try {
            stateRenderer = new LineAndShapeRenderer();
            statePlot = new CategoryPlot();
            DefaultCategoryDataset stateDataset = new DefaultCategoryDataset();
            statePlot.setDataset(stateDataset);
            statePlot.setRenderer(stateRenderer);

            String rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            String columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }
            double value = data.consumeInt(-1000, 1000);

            stateDataset.addValue(value, rowKey, columnKey);
            beforeRows = stateDataset.getRowCount();
            stateItems1 = stateRenderer.getLegendItems();
            afterRows = stateDataset.getRowCount();
            stateItems2 = stateRenderer.getLegendItems();
            stateLabel1 = stateItems1 != null && stateItems1.getItemCount() > 0 ? stateItems1.get(0).getLabel() : null;
            stateLabel2 = stateItems2 != null && stateItems2.getItemCount() > 0 ? stateItems2.get(0).getLabel() : null;

            if (stateRenderer.getPlot() != statePlot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state-coupling] semantic mismatch: setRenderer/setPlot established plot=" + statePlot + " but getPlot returned=" + stateRenderer.getPlot());
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) {
            return;
        }

        if (stateItems1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (stateItems1.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-count] semantic mismatch: expected=1 actual=" + stateItems1.getItemCount());
        }
        if (stateLabel1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-label-null] semantic mismatch: expected non-null label but got null");
        }
        if (!stateLabel1.equals(stateLabel2)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset must agree on first label lhs=" + stateLabel1 + " rhs=" + stateLabel2);
        }
        if (stateItems2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotence-not-null] semantic mismatch: second getLegendItems() returned null");
        }
        if (stateItems2.getItemCount() != stateItems1.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotence-count] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset must agree on item count lhs=" + stateItems1.getItemCount() + " rhs=" + stateItems2.getItemCount());
        }
        if (beforeRows != afterRows) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-readonly-dataset] metamorphic violation: getLegendItems() is a query and should not change dataset row count before=" + beforeRows + " after=" + afterRows);
        }

        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-empty] semantic mismatch: expected=0 actual=" + noPlotItems.getItemCount());
        }
    }
}
```

</details>

---
## [88] ⚙️ harness-repair · `attempt_008`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [89] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-one-series-count] semantic mismatch: expected=1 actual=0

---
## [90] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,609 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted oracle pair 1 from test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-not-null] semantic mismatch: getLegendItems() returned null");
        }

        // Lifted oracle pair 2 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state consistency oracle over field 'plot':
        // setPlot()/getPlot() are documented as writer/reader for the same field.
        // A correct implementation must report the plot that was assigned.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-reader-writer] semantic mismatch: expected renderer plot identity to equal assigned plot");
        }

        // Lifted oracle pair 3 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-not-null] semantic mismatch: getLegendItems() returned null after plot assignment");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        // Consume fuzz data to vary but keep non-degenerate, moderate inputs.
        String series = data.consumeAsciiString(16);
        if (series.length() == 0) {
            series = "S1";
        }
        String category = data.consumeAsciiString(16);
        if (category.length() == 0) {
            category = "C1";
        }
        int value = data.consumeInt(-1000, 1000);

        // Exact reconstruction of the failing test's trusted scenario.
        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();

        // Lifted oracle pair 4 from test2947660:
        // assertEquals(1, lic.getItemCount());
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-not-null] semantic mismatch: getLegendItems() returned null after adding one series");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }

        // Lifted oracle pair 5 from test2947660:
        // assertEquals("S1", lic.get(0).getLabel());
        String firstLabel = lic.get(0).getLabel();
        if (!"S1".equals(firstLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label] semantic mismatch: expected=S1 actual=" + firstLabel);
        }

        // Mandatory post-condition / metamorphic check:
        // getLegendItems() is a reader over current renderer/plot/dataset state.
        // With no intervening state change, repeated calls must agree. A patch that
        // "fixes" the bug by skipping the real enumeration or returning stale/empty
        // data can violate this observable stability even without throwing.
        try {
            LegendItemCollection lic2 = r.getLegendItems();
            if (lic2 == null) {
                return;
            }
            int c1 = lic.getItemCount();
            int c2 = lic2.getItemCount();
            String l1 = c1 > 0 ? lic.get(0).getLabel() : null;
            String l2 = c2 > 0 ? lic2.get(0).getLabel() : null;
            if (c1 != c2 || (l1 == null ? l2 != null : !l1.equals(l2))) {
                throw new RuntimeException("[oracle:idempotent-reader] metamorphic violation: repeated getLegendItems() without state change must agree count/firstLabel lhsCount="
                        + c1 + " rhsCount=" + c2 + " lhsLabel=" + l1 + " rhsLabel=" + l2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Additional trusted exploration by construction: add one more value in the
        // same series under a fuzzed category. Legend items are per series, so adding
        // another category for existing row key must not change the single-series
        // legend count established by the already-trusted "S1" setup.
        try {
            dataset.addValue((double) value, "S1", category);
            LegendItemCollection lic3 = r.getLegendItems();
            if (lic3 == null) {
                return;
            }
            if (lic3.getItemCount() != 1) {
                throw new RuntimeException("[oracle:same-series-extra-category] metamorphic violation: adding another category for the same existing series must preserve one legend item actual="
                        + lic3.getItemCount() + " category=" + category + " value=" + value);
            }
            String label3 = lic3.get(0).getLabel();
            if (!"S1".equals(label3)) {
                throw new RuntimeException("[oracle:same-series-label-stable] metamorphic violation: sole legend label must remain the existing series label expected=S1 actual="
                        + label3 + " category=" + category + " value=" + value);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Exercise an additional real input without asserting an unjustified answer.
        if (series.length() > 0 && !"S1".equals(series)) {
            try {
                dataset.addValue((double) value, series, category + "_2");
                r.getLegendItems();
            } catch (Throwable t) {
                return;
            }
        }
    }
}
```

</details>

---
## [91] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-single-count] semantic mismatch: expected=1 actual=0

---
## [92] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,304 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted oracle from AbstractCategoryItemRendererTests.test2947660.

        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initialItems = r.getLegendItems();
        if (initialItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (initialItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initialItems.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state agreement check: setPlot()/plot.setRenderer(r) establishes the renderer's plot;
        // getPlot() is documented to report the plot the renderer has been assigned to.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-reader-writer] semantic mismatch: expected renderer.getPlot() to be the assigned plot");
        }

        LegendItemCollection emptyPlotItems = r.getLegendItems();
        if (emptyPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (emptyPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-empty] semantic mismatch: expected=0 actual=" + emptyPlotItems.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        // Post-condition / hidden-state check: getLegendItems() is a getter that should not mutate the
        // renderer's assigned plot; a patch that merely skips the real work or corrupts bookkeeping would
        // violate this observable read-only behavior.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-no-plot-mutation] semantic mismatch: getLegendItems changed renderer plot association");
        }

        // Metamorphic/idempotence check on the same real API:
        // for a fixed renderer/plot/dataset state, repeated getLegendItems() calls must agree on count and label.
        LegendItemCollection lic2 = r.getLegendItems();
        if (lic2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-idempotence-nonnull] semantic mismatch: second getLegendItems returned null");
        }
        if (lic2.getItemCount() != lic.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-idempotence-count] metamorphic violation: repeated getLegendItems() changed item count lhs=" + lic.getItemCount() + " rhs=" + lic2.getItemCount());
        }
        if (lic2.getItemCount() == 1) {
            String label2 = lic2.get(0).getLabel();
            if (!label.equals(label2)) {
                throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-idempotence-label] metamorphic violation: repeated getLegendItems() changed label lhs=" + String.valueOf(label) + " rhs=" + String.valueOf(label2));
            }
        }

        // Generalization on valid-by-construction non-degenerate fuzzed inputs.
        LineAndShapeRenderer fuzzRenderer;
        CategoryPlot fuzzPlot;
        DefaultCategoryDataset fuzzDataset;
        String rowKey;
        String columnKey;
        LegendItemCollection fuzzItems;
        LegendItemCollection fuzzItems2;
        try {
            fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot = new CategoryPlot();
            fuzzDataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }

            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);

            if (fuzzRenderer.getPlot() != fuzzPlot) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-reader-writer] semantic mismatch: expected renderer.getPlot() to equal assigned plot");
            }

            fuzzDataset.addValue(1.0, rowKey, columnKey);
            fuzzItems = fuzzRenderer.getLegendItems();
            fuzzItems2 = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount());
        }
        String fuzzLabel = fuzzItems.get(0).getLabel();
        if (!rowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + String.valueOf(fuzzLabel));
        }
        if (fuzzRenderer.getPlot() != fuzzPlot) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() changed renderer plot association");
        }
        if (fuzzItems2 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: repeated getLegendItems() returned null");
        }
        if (fuzzItems2.getItemCount() != fuzzItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: repeated getLegendItems() changed count lhs=" + fuzzItems.getItemCount() + " rhs=" + fuzzItems2.getItemCount());
        }
        if (fuzzItems2.getItemCount() == 1) {
            String fuzzLabel2 = fuzzItems2.get(0).getLabel();
            if (!rowKey.equals(fuzzLabel2)) {
                throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: repeated getLegendItems() changed label expected=" + rowKey + " actual=" + String.valueOf(fuzzLabel2));
            }
        }

        // Candidate relation: no plot => non-null, empty collection.
        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + noPlotItems.getItemCount());
        }
    }
}
```

</details>

---
## [93] ⚙️ harness-repair · `attempt_010`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [94] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-count] semantic mismatch: expected=1 actual=0

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,654 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract: getPlot() "Returns the plot that the renderer has been assigned to";
        // setPlot()/plot.setRenderer establish that shared field, so reader and writer must agree.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-coupling] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-notnull] semantic mismatch: r.getLegendItems() returned null after plot assignment");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-populated-notnull] semantic mismatch: populated legend collection was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-itemcount] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        try {
            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            int seriesCount = data.consumeInt(1, 4);
            String[] expectedLabels = new String[seriesCount];
            for (int i = 0; i < seriesCount; i++) {
                String base = data.consumeAsciiString(8);
                if (base == null || base.length() == 0) {
                    base = "S";
                }
                expectedLabels[i] = base + "_" + i;
                dataset2.addValue(data.consumeInt(-1000, 1000), expectedLabels[i], "C1");
            }

            LegendItemCollection lic2 = r2.getLegendItems();

            // Post-condition from getLegendItems() contract: it returns legend items for the series
            // this renderer is responsible for drawing. For this constructed input, every series is
            // present, visible by default, and has a non-null row key, so a throw-deleting or
            // branch-skipping patch that returns an empty/stale collection violates this observable.
            if (lic2.getItemCount() != seriesCount) {
                throw new FuzzerSecurityIssueLow("[oracle:postcondition-series-count] semantic mismatch: expected=" + seriesCount + " actual=" + lic2.getItemCount());
            }

            for (int i = 0; i < seriesCount; i++) {
                String actualLabel = lic2.get(i).getLabel();
                if (!expectedLabels[i].equals(actualLabel)) {
                    throw new FuzzerSecurityIssueLow("[oracle:postcondition-label-order] semantic mismatch: index=" + i + " expected=" + expectedLabels[i] + " actual=" + String.valueOf(actualLabel));
                }
            }

            LegendItemCollection lic3 = r2.getLegendItems();
            if (lic3.getItemCount() != lic2.getItemCount()) {
                throw new RuntimeException("[oracle:metamorphic-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset changed itemCount lhs=" + lic2.getItemCount() + " rhs=" + lic3.getItemCount());
            }
            for (int i = 0; i < lic2.getItemCount(); i++) {
                String lhs = lic2.get(i).getLabel();
                String rhs = lic3.get(i).getLabel();
                if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:metamorphic-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset changed label index=" + i + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
                }
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
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
## [96] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-itemcount] semantic mismatch: expected=1 actual=0

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,475 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        liftedTestOracles();
        relationNoPlotEmptyAndNonNull();
        relationSingleSeriesNonNullDataset(data);
    }

    private static void liftedTestOracles() {
        AbstractCategoryItemRenderer r;
        LegendItemCollection itemsNoPlot;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        LegendItemCollection itemsEmptyDataset;
        LegendItemCollection itemsSingleSeries;
        try {
            r = new LineAndShapeRenderer();

            itemsNoPlot = r.getLegendItems();
            if (itemsNoPlot == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-no-plot] semantic mismatch: expected non-null legend items but was null");
            }
            if (itemsNoPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-no-plot] semantic mismatch: expected 0 but was " + itemsNoPlot.getItemCount());
            }

            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            // Shared-state agreement: CategoryPlot.setRenderer(r) establishes the renderer's plot;
            // getPlot() is documented to report the plot assigned to the renderer, so they must agree.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: expected renderer.getPlot()==assigned plot");
            }

            itemsEmptyDataset = r.getLegendItems();
            if (itemsEmptyDataset.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-empty-dataset] semantic mismatch: expected 0 but was " + itemsEmptyDataset.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            itemsSingleSeries = r.getLegendItems();

            if (itemsSingleSeries.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-count] semantic mismatch: expected 1 but was " + itemsSingleSeries.getItemCount());
            }
            if (!"S1".equals(itemsSingleSeries.get(0).getLabel())) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-label] semantic mismatch: expected S1 but was " + itemsSingleSeries.get(0).getLabel());
            }

            // Post-condition / metamorphic check: getLegendItems() is a query method and the setup is unchanged,
            // so repeated calls must agree on the observable legend contents. A "fix" that only skips logic or
            // mutates hidden state during a read would violate this idempotent read-only behaviour.
            LegendItemCollection secondRead = r.getLegendItems();
            if (secondRead == null) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-read-non-null] semantic mismatch: repeated getLegendItems() returned null");
            }
            if (secondRead.getItemCount() != itemsSingleSeries.getItemCount()) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-read-count] semantic mismatch: firstCount="
                        + itemsSingleSeries.getItemCount() + " secondCount=" + secondRead.getItemCount());
            }
            if (secondRead.getItemCount() > 0) {
                String firstLabel = itemsSingleSeries.get(0).getLabel();
                String secondLabel = secondRead.get(0).getLabel();
                if (firstLabel == null ? secondLabel != null : !firstLabel.equals(secondLabel)) {
                    throw new FuzzerSecurityIssueLow("[oracle:idempotent-read-label] semantic mismatch: firstLabel="
                            + firstLabel + " secondLabel=" + secondLabel);
                }
            }
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-plot-state] semantic mismatch: getLegendItems() changed renderer plot association");
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void relationNoPlotEmptyAndNonNull() {
        LineAndShapeRenderer r;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            items = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-noPlot-emptyAndNonNull] relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-noPlot-emptyAndNonNull] relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items.getItemCount());
        }
    }

    private static void relationSingleSeriesNonNullDataset(FuzzedDataProvider data) {
        LineAndShapeRenderer r;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        String rowKey;
        String columnKey;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            plot = new CategoryPlot();
            dataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }

            plot.setDataset(dataset);
            plot.setRenderer(r);
            dataset.addValue(1.0, rowKey, columnKey);

            items = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-singleSeries-nonNullDataset] relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-singleSeries-nonNullDataset] relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount());
        }
        String label = items.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-singleSeries-nonNullDataset] relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label);
        }

        // Additional same-state agreement: the renderer was attached through plot.setRenderer(r),
        // so getPlot() must still report that exact plot after the query.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-plot-agreement] relation plot-state agreement violated: renderer.getPlot() != plot after setRenderer/getLegendItems");
        }
    }
}
```

</details>

---
## [98] ⚙️ harness-repair · `attempt_012`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [99] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-single-series-count] semantic mismatch: expected 1 but was 0

---
## [100] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,195 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-notnull] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-initial-empty] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract: getPlot() "Returns the plot that the renderer has been assigned to";
        // after CategoryPlot#setRenderer(r), the renderer's shared plot field must report that plot.
        if (r.getPlot() != plot) {
            throw new RuntimeException("[oracle:plot-coupling] metamorphic violation: renderer/plot assignment disagreed expectedSame=true actualSame=false");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-empty-dataset] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-one-series-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-one-series-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        try {
            String row1 = data.consumeAsciiString(12);
            String row2 = data.consumeAsciiString(12);
            String col = data.consumeAsciiString(12);
            if (row1.length() == 0) {
                row1 = "R1";
            }
            if (row2.length() == 0) {
                row2 = "R2";
            }
            if (row1.equals(row2)) {
                row2 = row2 + "_2";
            }
            if (col.length() == 0) {
                col = "C";
            }
            double v1 = data.consumeInt(-1000000, 1000000);
            double v2 = data.consumeInt(-1000000, 1000000);

            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            dataset2.addValue(v1, row1, col);
            dataset2.addValue(v2, row2, col);

            // Post-condition from getLegendItems() body and contracts:
            // with a non-null plot, non-null dataset, and both series visible in the legend,
            // the returned collection should contain one item per dataset row, in row rendering order.
            // A "fix" that merely suppresses the failing path by returning an empty collection breaks this.
            plot2.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection asc = r2.getLegendItems();
            if (asc.getItemCount() != 2) {
                throw new RuntimeException("[oracle:two-series-ascending-count] metamorphic violation: expectedCount=2 actualCount=" + asc.getItemCount() + " row1=" + row1 + " row2=" + row2);
            }
            String asc0 = asc.get(0).getLabel();
            String asc1 = asc.get(1).getLabel();
            if (!row1.equals(asc0) || !row2.equals(asc1)) {
                throw new RuntimeException("[oracle:two-series-ascending-order] metamorphic violation: expected0=" + row1 + " actual0=" + String.valueOf(asc0) + " expected1=" + row2 + " actual1=" + String.valueOf(asc1));
            }

            plot2.setRowRenderingOrder(SortOrder.DESCENDING);
            LegendItemCollection desc = r2.getLegendItems();
            if (desc.getItemCount() != 2) {
                throw new RuntimeException("[oracle:two-series-descending-count] metamorphic violation: expectedCount=2 actualCount=" + desc.getItemCount() + " row1=" + row1 + " row2=" + row2);
            }
            String desc0 = desc.get(0).getLabel();
            String desc1 = desc.get(1).getLabel();
            if (!row2.equals(desc0) || !row1.equals(desc1)) {
                throw new RuntimeException("[oracle:two-series-descending-order] metamorphic violation: expected0=" + row2 + " actual0=" + String.valueOf(desc0) + " expected1=" + row1 + " actual1=" + String.valueOf(desc1));
            }

            // Contract from AbstractRenderer: per-series legend visibility controls whether a series should
            // be shown in the legend; getLegendItems() consults isSeriesVisibleInLegend(i) before adding.
            r2.setSeriesVisibleInLegend(0, Boolean.FALSE);
            plot2.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection filtered = r2.getLegendItems();
            if (filtered.getItemCount() != 1) {
                throw new RuntimeException("[oracle:legend-visibility-count] metamorphic violation: expectedCount=1 actualCount=" + filtered.getItemCount() + " hiddenSeries=0");
            }
            String filtered0 = filtered.get(0).getLabel();
            if (!row2.equals(filtered0)) {
                throw new RuntimeException("[oracle:legend-visibility-label] metamorphic violation: expected=" + row2 + " actual=" + String.valueOf(filtered0));
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [101] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-one-series-count] semantic mismatch: expected=1 actual=0

---
## [102] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,842 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer seedRenderer;
        LegendItemCollection seedItemsNoPlot1;
        LegendItemCollection seedItemsNoPlot2;
        DefaultCategoryDataset seedDataset;
        CategoryPlot seedPlot;
        LegendItemCollection seedItemsWithEmptyDataset;
        LegendItemCollection seedItemsWithOneSeries;

        try {
            seedRenderer = new LineAndShapeRenderer();

            seedItemsNoPlot1 = seedRenderer.getLegendItems();
            seedItemsNoPlot2 = seedRenderer.getLegendItems();

            seedDataset = new DefaultCategoryDataset();
            seedPlot = new CategoryPlot();
            seedPlot.setDataset(seedDataset);
            seedPlot.setRenderer(seedRenderer);

            seedItemsWithEmptyDataset = seedRenderer.getLegendItems();

            seedDataset.addValue(1.0, "S1", "C1");
            seedItemsWithOneSeries = seedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (seedItemsNoPlot1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-not-null] semantic mismatch: expected non-null legend items but actual was null");
        }
        if (seedItemsNoPlot1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-count] semantic mismatch: expected 0 but actual was " + seedItemsNoPlot1.getItemCount());
        }
        if (seedRenderer.getPlot() != seedPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: expected renderer.getPlot() to be identical to plot set by plot.setRenderer(r), expectedSame=true actualSame=false");
        }
        if (seedItemsWithEmptyDataset.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-count] semantic mismatch: expected 0 but actual was " + seedItemsWithEmptyDataset.getItemCount());
        }
        if (seedItemsWithOneSeries == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-not-null] semantic mismatch: expected non-null legend items but actual was null");
        }
        if (seedItemsWithOneSeries.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but actual was " + seedItemsWithOneSeries.getItemCount());
        }
        String seedLabel = seedItemsWithOneSeries.get(0).getLabel();
        if (!"S1".equals(seedLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-label] semantic mismatch: expected S1 but actual was " + seedLabel);
        }

        // Documented guarantee used here: getLegendItems() is a query that returns legend items
        // for the renderer's current plot/dataset. A throw-deleting or overfit patch that skips
        // the real legend-building path would often leave the result empty; repeated read-only
        // calls on unchanged state must therefore agree and must not silently mutate renderer.plot.
        LegendItemCollection repeatedItems;
        CategoryPlot plotBeforeRepeat = seedRenderer.getPlot();
        try {
            repeatedItems = seedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (seedRenderer.getPlot() != plotBeforeRepeat) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-readonly] semantic mismatch: getLegendItems() changed renderer plot reference");
        }
        if (repeatedItems == null || repeatedItems.getItemCount() != seedItemsWithOneSeries.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-query-count] semantic mismatch: repeated getLegendItems() disagreed on item count, first=" + seedItemsWithOneSeries.getItemCount() + " second=" + (repeatedItems == null ? "null" : String.valueOf(repeatedItems.getItemCount())));
        }
        if (repeatedItems == null || repeatedItems.getItemCount() > 0 && !seedLabel.equals(repeatedItems.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-query-label] semantic mismatch: repeated getLegendItems() disagreed on first label, first=" + seedLabel + " second=" + (repeatedItems == null || repeatedItems.getItemCount() == 0 ? "missing" : repeatedItems.get(0).getLabel()));
        }

        LineAndShapeRenderer fuzzRenderer;
        CategoryPlot fuzzPlot;
        DefaultCategoryDataset fuzzDataset;
        String rowKey;
        String columnKey;
        LegendItemCollection fuzzItems;

        try {
            fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot = new CategoryPlot();
            fuzzDataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }

            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);
            fuzzDataset.addValue(1.0, rowKey, columnKey);
            fuzzItems = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        // Contract from getLegendItems() and the lifted test: with a non-null dataset containing
        // exactly one series attached to this renderer's plot, the legend collection is non-null
        // and contains exactly one item for that series. An overfit fix that just guards away the
        // branch would keep returning 0 here, so this post-condition catches silent wrong output.
        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String fuzzLabel = fuzzItems.get(0).getLabel();
        if (!rowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + fuzzLabel + " columnKey=" + columnKey);
        }
        if (fuzzRenderer.getPlot() != fuzzPlot) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: renderer.getPlot() did not agree with the plot established by plot.setRenderer(r)");
        }

        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + noPlotItems.getItemCount());
        }
    }
}
```

</details>

---
## [103] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-series-count] semantic mismatch: expected 1 but actual was 0

---
## [104] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,637 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted oracle #1 from test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null] semantic mismatch: r.getLegendItems() returned null");
        }

        // Lifted oracle #2 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        int initialCount = initial.getItemCount();
        if (initialCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initialCount);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state agreement check:
        // getPlot()/setPlot() share the renderer's plot field; after CategoryPlot.setRenderer(r),
        // the renderer must report the same plot it has been assigned to.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expectedSamePlot=true actualSamePlot=false");
        }

        // Lifted oracle #3 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        int emptyPlotCount = r.getLegendItems().getItemCount();
        if (emptyPlotCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected=0 actual=" + emptyPlotCount);
        }

        // Consume fuzz data after faithfully reconstructing the seed test, so the exact bug remains reachable.
        String fuzzSeries = data.consumeString(32);
        if (fuzzSeries == null || fuzzSeries.length() == 0) {
            fuzzSeries = "F";
        }
        String fuzzCategory = data.consumeString(32);
        if (fuzzCategory == null || fuzzCategory.length() == 0) {
            fuzzCategory = "C";
        }
        double fuzzValue = data.consumeInt(-1000000, 1000000);

        // Lifted oracle #4 from test2947660:
        // dataset.addValue(1.0, "S1", "C1");
        // LegendItemCollection lic = r.getLegendItems();
        // assertEquals(1, lic.getItemCount());
        // assertEquals("S1", lic.get(0).getLabel());
        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        int itemCount = lic.getItemCount();
        if (itemCount != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-item] semantic mismatch: expected=1 actual=" + itemCount);
        }
        String label0 = lic.get(0).getLabel();
        if (!"S1".equals(label0)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label0));
        }

        // Mandatory post-condition / metamorphic check on the patched method:
        // For a plot with exactly one series, getLegendItems() iterates that one series regardless
        // of rowRenderingOrder; ASCENDING and DESCENDING must therefore produce the same single item.
        // A patch that merely skips the real dataset-processing branch would violate this observable
        // post-condition because the collection would no longer reflect the established dataset state.
        try {
            plot.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection asc = r.getLegendItems();
            plot.setRowRenderingOrder(SortOrder.DESCENDING);
            LegendItemCollection desc = r.getLegendItems();

            int ascCount = asc.getItemCount();
            int descCount = desc.getItemCount();
            String ascLabel = ascCount > 0 ? asc.get(0).getLabel() : null;
            String descLabel = descCount > 0 ? desc.get(0).getLabel() : null;

            if (ascCount != descCount || (ascCount > 0 && !safeEquals(ascLabel, descLabel))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:single-series-order] metamorphic violation: single-series legend must be order-invariant"
                                + " ascCount=" + ascCount
                                + " descCount=" + descCount
                                + " ascLabel=" + String.valueOf(ascLabel)
                                + " descLabel=" + String.valueOf(descLabel));
            }
            if (ascCount != 1 || !safeEquals("S1", ascLabel)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:single-series-postcondition] semantic mismatch: established single-series dataset must yield one legend item labeled S1"
                                + " count=" + ascCount
                                + " label=" + String.valueOf(ascLabel));
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }

        // Trusted generalization by construction:
        // We build a fresh renderer/plot/dataset with exactly one non-empty series key chosen first.
        // For the same reason as the lifted seed ("S1" -> label "S1"), a correct implementation must
        // report exactly one legend item for a one-row dataset and preserve that row key as the label.
        try {
            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            dataset2.addValue(fuzzValue, fuzzSeries, fuzzCategory);
            LegendItemCollection lic2 = r2.getLegendItems();
            int count2 = lic2.getItemCount();
            if (count2 != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-series-count] semantic mismatch: expected=1 actual=" + count2 + " series=" + escape(fuzzSeries));
            }
            String label2 = lic2.get(0).getLabel();
            if (!safeEquals(fuzzSeries, label2)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-series-label] semantic mismatch: expected=" + escape(fuzzSeries) + " actual=" + escape(label2));
            }

            // Shared-state agreement repeated on the fuzz-constructed instance.
            if (r2.getPlot() != plot2) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-plot-assignment] semantic mismatch: expectedSamePlot=true actualSamePlot=false");
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
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
## [105] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-one-item] semantic mismatch: expected=1 actual=0

---
## [106] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,892 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

/* Jazzer harness for AbstractCategoryItemRenderer.getLegendItems() semantic bug. */
public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r1;
        LegendItemCollection noPlotItems1;
        LegendItemCollection noPlotItems2;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection oneSeriesItems;
        String fuzzRowKey = data.consumeAsciiString(8);
        String fuzzColumnKey = data.consumeAsciiString(8);
        if (fuzzRowKey.length() == 0) {
            fuzzRowKey = "S";
        }
        if (fuzzColumnKey.length() == 0) {
            fuzzColumnKey = "C";
        }
        Exception setupException = null;
        try {
            r1 = new LineAndShapeRenderer();

            noPlotItems1 = r1.getLegendItems();
            noPlotItems2 = r1.getLegendItems();

            plot1 = new CategoryPlot();
            dataset1 = new DefaultCategoryDataset();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);

            emptyDatasetItems = r1.getLegendItems();

            dataset1.addValue(1.0, "S1", "C1");
            oneSeriesItems = r1.getLegendItems();
        } catch (Exception e) {
            setupException = e;
            r1 = null;
            noPlotItems1 = null;
            noPlotItems2 = null;
            plot1 = null;
            dataset1 = null;
            emptyDatasetItems = null;
            oneSeriesItems = null;
        }
        if (setupException != null) {
            return;
        }

        if (noPlotItems1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-1] semantic mismatch: expected non-null legend items before plot assignment but got null");
        }
        if (noPlotItems1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-before-plot] semantic mismatch: expected 0 but was " + noPlotItems1.getItemCount());
        }
        if (emptyDatasetItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-2] semantic mismatch: expected non-null legend items after empty dataset attachment but got null");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected 0 but was " + emptyDatasetItems.getItemCount());
        }
        if (oneSeriesItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-3] semantic mismatch: expected non-null legend items after adding one series but got null");
        }
        if (oneSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-item] semantic mismatch: expected 1 but was " + oneSeriesItems.getItemCount());
        }
        String actualLabel = oneSeriesItems.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected S1 but was " + actualLabel);
        }

        // Contract check from getPlot()/setPlot(): after plot.setRenderer(r), the renderer has been assigned to that plot.
        // This is a shared-state oracle for the same 'plot' field used by getLegendItems(); a patch that only special-cases output
        // but leaves plot state inconsistent would violate it.
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer.getPlot() to be the assigned plot instance");
        }

        // Read-only hidden-state check: getLegendItems() is documented as returning legend items for the current renderer/plot state;
        // it is a query and should not silently change which plot the renderer is assigned to.
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-readonly-plot] metamorphic violation: getLegendItems() changed renderer plot association");
        }

        LineAndShapeRenderer r2 = null;
        CategoryPlot plot2 = null;
        DefaultCategoryDataset dataset2 = null;
        LegendItemCollection items2 = null;
        Exception relationException = null;
        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(1.0, fuzzRowKey, fuzzColumnKey);
            items2 = r2.getLegendItems();
        } catch (Exception e) {
            relationException = e;
        }
        if (relationException != null) {
            return;
        }

        // Invariant from the failing test generalized by construction:
        // with a non-null dataset containing exactly one series, getLegendItems() must return exactly one legend item for that series.
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items2.getItemCount());
        }
        String label2 = items2.get(0).getLabel();
        if (!fuzzRowKey.equals(label2)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + fuzzRowKey + " but got " + label2);
        }

        // Additional post-condition: the writer-established plot state must agree with the reader after a successful legend query too.
        if (r2.getPlot() != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: expected renderer.getPlot() to remain the assigned plot after getLegendItems()");
        }

        LineAndShapeRenderer r3 = null;
        LegendItemCollection items3a = null;
        LegendItemCollection items3b = null;
        Exception noPlotException = null;
        try {
            r3 = new LineAndShapeRenderer();
            items3a = r3.getLegendItems();
            items3b = r3.getLegendItems();
        } catch (Exception e) {
            noPlotException = e;
        }
        if (noPlotException != null) {
            return;
        }

        // Invariant: without an assigned plot there are no series to report, so the collection is non-null and empty.
        if (items3a == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items3a.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items3a.getItemCount());
        }

        // Idempotence/read-only query check: repeated getLegendItems() on unchanged no-plot state must agree.
        if (items3b == null || items3b.getItemCount() != items3a.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer state disagreed: first="
                    + items3a.getItemCount() + " second=" + (items3b == null ? "null" : String.valueOf(items3b.getItemCount())));
        }
    }
}
```

</details>

---
## [107] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-one-item] semantic mismatch: expected 1 but was 0

---
## [108] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,890 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted oracle from AbstractCategoryItemRendererTests.test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection initialLegend = r.getLegendItems();
        if (initialLegend == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: getLegendItems() returned null before plot assignment");
        }

        // Lifted oracle from the same test:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (initialLegend.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-count] semantic mismatch: expected=0 actual=" + initialLegend.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state oracle on the same field ("plot"):
        // getPlot() must report the plot established by normal construction/wiring.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: renderer plot disagrees with plot.setRenderer() wiring");
        }

        // Lifted oracle from the same test:
        // assertEquals(0, r.getLegendItems().getItemCount());
        LegendItemCollection emptyDatasetLegend = r.getLegendItems();
        if (emptyDatasetLegend == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-not-null] semantic mismatch: getLegendItems() returned null for empty dataset");
        }
        if (emptyDatasetLegend.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-count] semantic mismatch: expected=0 actual=" + emptyDatasetLegend.getItemCount());
        }

        // Generalisation by construction from a known answer:
        // we choose the row key first, then build the dataset around it.
        // The lifted test shows the legend label is the series key ("S1"), so for a
        // one-series dataset the correct legend label is the row key we inserted.
        String chosenSeries = data.consumeAsciiString(16);
        if (chosenSeries == null || chosenSeries.length() == 0) {
            chosenSeries = "S1";
        }
        String chosenCategory = data.consumeAsciiString(16);
        if (chosenCategory == null || chosenCategory.length() == 0) {
            chosenCategory = "C1";
        }
        int value = data.consumeInt(-1000, 1000);

        dataset.addValue((double) value, chosenSeries, chosenCategory);

        // Lifted oracle from the same test, reconstructed on the real API path:
        // LegendItemCollection lic = r.getLegendItems();
        // assertEquals(1, lic.getItemCount());
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-not-null] semantic mismatch: getLegendItems() returned null for one-series dataset");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }

        // Lifted oracle adapted by construction from a known answer:
        // test hard-codes assertEquals("S1", lic.get(0).getLabel()) after inserting row key "S1".
        // Therefore when we insert a one-series dataset with row key chosenSeries, the label must equal chosenSeries.
        String actualLabel = lic.get(0).getLabel();
        if (!chosenSeries.equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label] semantic mismatch: expected=" + chosenSeries + " actual=" + actualLabel);
        }

        // Mandatory post-condition / metamorphic check:
        // Contract observed in getLegendItems(): only the row rendering order changes iteration order.
        // With exactly one visible series, ASCENDING and DESCENDING must yield the same single legend item.
        // This catches a "fix" that merely suppresses the bad branch or silently returns the wrong collection.
        try {
            plot.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection asc = r.getLegendItems();
            plot.setRowRenderingOrder(SortOrder.DESCENDING);
            LegendItemCollection desc = r.getLegendItems();

            if (asc == null || desc == null) {
                return;
            }
            if (asc.getItemCount() != 1 || desc.getItemCount() != 1) {
                throw new RuntimeException("[oracle:single-series-order] metamorphic violation: one-series legend count must stay 1 across row rendering orders ascCount="
                        + asc.getItemCount() + " descCount=" + desc.getItemCount());
            }
            String ascLabel = asc.get(0).getLabel();
            String descLabel = desc.get(0).getLabel();
            if (!chosenSeries.equals(ascLabel) || !chosenSeries.equals(descLabel)) {
                throw new RuntimeException("[oracle:single-series-order] metamorphic violation: one-series legend label must equal the only series key input="
                        + chosenSeries + " ascLabel=" + ascLabel + " descLabel=" + descLabel);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [109] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-populated-count] semantic mismatch: expected=1 actual=0

---
## [110] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,687 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void fail(String id, String msg) {
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer liftedRenderer;
        org.jfree.chart.LegendItemCollection liftedItems0;
        org.jfree.chart.plot.CategoryPlot liftedPlot;
        org.jfree.data.category.DefaultCategoryDataset liftedDataset;
        org.jfree.chart.LegendItemCollection liftedItems1;
        org.jfree.chart.LegendItemCollection liftedItems2;
        try {
            liftedRenderer = new LineAndShapeRenderer();

            liftedItems0 = liftedRenderer.getLegendItems();
            liftedPlot = new org.jfree.chart.plot.CategoryPlot();
            liftedDataset = new org.jfree.data.category.DefaultCategoryDataset();
            liftedPlot.setDataset(liftedDataset);
            liftedPlot.setRenderer(liftedRenderer);
            liftedItems1 = liftedRenderer.getLegendItems();

            liftedDataset.addValue(1.0, "S1", "C1");
            liftedItems2 = liftedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (liftedItems0 == null) {
            fail("lifted-not-null-no-plot", "r.getLegendItems() expected non-null but was null");
        }
        if (liftedItems0.getItemCount() != 0) {
            fail("lifted-empty-no-plot", "expected 0 but was " + liftedItems0.getItemCount());
        }
        if (liftedItems1.getItemCount() != 0) {
            fail("lifted-empty-empty-dataset", "expected 0 but was " + liftedItems1.getItemCount());
        }
        if (liftedItems2.getItemCount() != 1) {
            fail("lifted-single-series-count", "expected 1 but was " + liftedItems2.getItemCount());
        }
        String liftedLabel = null;
        try {
            liftedLabel = liftedItems2.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(liftedLabel)) {
            fail("lifted-single-series-label", "expected S1 but was " + String.valueOf(liftedLabel));
        }

        // Shared-state agreement: setRenderer(plot) establishes the renderer's plot;
        // getPlot() reads that same field, so they must agree for every correct implementation.
        if (liftedRenderer.getPlot() != liftedPlot) {
            fail("plot-state-agreement", "renderer.getPlot() did not return the plot established by plot.setRenderer(renderer)");
        }

        org.jfree.chart.LegendItemCollection repeatItems;
        try {
            repeatItems = liftedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        // Post-condition / read-only check: getLegendItems() is a getter over current plot+dataset state.
        // A throw-deleting or bookkeeping-skipping patch that suppresses real legend construction would violate
        // this observable contract; repeated reads on unchanged state must still report the same one-item legend.
        if (repeatItems == null) {
            fail("repeat-not-null", "second getLegendItems() returned null");
        }
        if (repeatItems.getItemCount() != 1) {
            fail("repeat-single-series-count", "expected 1 on repeated read but was " + repeatItems.getItemCount());
        }
        String repeatLabel;
        try {
            repeatLabel = repeatItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(repeatLabel)) {
            fail("repeat-single-series-label", "expected S1 on repeated read but was " + String.valueOf(repeatLabel));
        }
        if (liftedRenderer.getPlot() != liftedPlot) {
            fail("plot-readonly-after-get", "getLegendItems() changed renderer plot association");
        }

        LineAndShapeRenderer noPlotRenderer;
        org.jfree.chart.LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (noPlotItems == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got "
                    + noPlotItems.getItemCount());
        }

        LineAndShapeRenderer fuzzRenderer;
        org.jfree.chart.plot.CategoryPlot fuzzPlot;
        org.jfree.data.category.DefaultCategoryDataset fuzzDataset;
        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        double v = data.consumeInt(-1000000, 1000000);
        org.jfree.chart.LegendItemCollection fuzzItems;
        try {
            fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot = new org.jfree.chart.plot.CategoryPlot();
            fuzzDataset = new org.jfree.data.category.DefaultCategoryDataset();
            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);
            fuzzDataset.addValue(v, rowKey, columnKey);
            fuzzItems = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (fuzzItems == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got "
                    + fuzzItems.getItemCount());
        }
        String fuzzLabel;
        try {
            fuzzLabel = fuzzItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!rowKey.equals(fuzzLabel)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: expected legend label "
                    + rowKey + " but got " + String.valueOf(fuzzLabel));
        }

        org.jfree.chart.LegendItemCollection fuzzItemsAgain;
        try {
            fuzzItemsAgain = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        // Metamorphic relation on real calls: with unchanged plot/dataset state, repeated getLegendItems()
        // calls must agree on count and label because the method only reports current legend items.
        if (fuzzItemsAgain == null) {
            fail("fuzz-repeat-not-null", "repeated getLegendItems() returned null for unchanged single-series state");
        }
        if (fuzzItemsAgain.getItemCount() != fuzzItems.getItemCount()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fuzz-repeat-agreement] metamorphic violation: repeated getLegendItems() disagreed on itemCount inputRow="
                    + rowKey + " inputColumn=" + columnKey + " lhs=" + fuzzItems.getItemCount()
                    + " rhs=" + fuzzItemsAgain.getItemCount());
        }
        String fuzzLabelAgain;
        try {
            fuzzLabelAgain = fuzzItemsAgain.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!fuzzLabel.equals(fuzzLabelAgain)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fuzz-repeat-agreement] metamorphic violation: repeated getLegendItems() disagreed on label inputRow="
                    + rowKey + " inputColumn=" + columnKey + " lhs=" + String.valueOf(fuzzLabel)
                    + " rhs=" + String.valueOf(fuzzLabelAgain));
        }
        if (fuzzRenderer.getPlot() != fuzzPlot) {
            fail("fuzz-plot-state-agreement", "renderer.getPlot() did not equal plot after plot.setRenderer(renderer)");
        }
    }
}
```

</details>

---
## [111] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-single-series-count] semantic mismatch: expected 1 but was 0

---
## [112] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,790 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer();

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertNotNull(r.getLegendItems());
            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull] semantic mismatch: getLegendItems() returned null for a fresh renderer");
            }

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals(0, r.getLegendItems().getItemCount());
            int initialCount = initial.getItemCount();
            if (initialCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-fresh-count] semantic mismatch: expected=0 actual=" + initialCount);
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            // Shared-state agreement check on field "plot":
            // getPlot() is documented to return "the plot that the renderer has been assigned to",
            // while set via CategoryPlot.setRenderer(...) in normal usage. A patch that merely
            // suppresses behaviour in getLegendItems() must still leave this writer/reader pair consistent.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-reader-writer] semantic mismatch: renderer plot reader/writer disagree expectedSame=true actualSame=false");
            }

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals(0, r.getLegendItems().getItemCount());
            int emptyPlotCount = r.getLegendItems().getItemCount();
            if (emptyPlotCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-count] semantic mismatch: expected=0 actual=" + emptyPlotCount);
            }

            // Lifted exactly from the failing test:
            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals(1, lic.getItemCount());
            int countAfterSeedAdd = lic.getItemCount();
            if (countAfterSeedAdd != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-seed-count] semantic mismatch: expected=1 actual=" + countAfterSeedAdd);
            }

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals("S1", lic.get(0).getLabel());
            String seedLabel = lic.get(0).getLabel();
            if (!"S1".equals(seedLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-seed-label] semantic mismatch: expected=S1 actual=" + escape(seedLabel));
            }

            // Mandatory post-condition / metamorphic check:
            // For a fixed renderer/plot/dataset state, repeated getLegendItems() calls should observe
            // the same legend contents; deleting the real population logic or guarding it away would
            // break this observable state agreement without throwing.
            LegendItemCollection lic2 = r.getLegendItems();
            int count2 = lic2.getItemCount();
            String label2 = count2 > 0 ? lic2.get(0).getLabel() : null;
            if (count2 != countAfterSeedAdd || !safeEquals(seedLabel, label2)) {
                throw new RuntimeException("[oracle:idempotent-read] metamorphic violation: repeated getLegendItems() on unchanged state should agree input=seed lhsCount=" + countAfterSeedAdd + " rhsCount=" + count2 + " lhsLabel=" + escape(seedLabel) + " rhsLabel=" + escape(label2));
            }

            // Trusted generalisation from input itself:
            // We choose the series label first, then construct a one-series dataset whose only visible
            // legend item must recover that exact label.
            String fuzzSeries = data.consumeAsciiString(16);
            if (fuzzSeries == null || fuzzSeries.length() == 0) {
                fuzzSeries = "F";
            }
            String fuzzCategory = data.consumeAsciiString(16);
            if (fuzzCategory == null || fuzzCategory.length() == 0) {
                fuzzCategory = "C";
            }
            int value = data.consumeInt(-1000, 1000);

            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            plot2.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
            dataset2.addValue((double) value, fuzzSeries, fuzzCategory);

            LegendItemCollection single = r2.getLegendItems();
            if (single == null) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-notnull] semantic mismatch: getLegendItems() returned null for constructed one-series dataset");
            }
            if (single.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-count] semantic mismatch: expected=1 actual=" + single.getItemCount() + " series=" + escape(fuzzSeries) + " category=" + escape(fuzzCategory));
            }
            String actualFuzzLabel = single.get(0).getLabel();
            if (!fuzzSeries.equals(actualFuzzLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-label] semantic mismatch: expected=" + escape(fuzzSeries) + " actual=" + escape(actualFuzzLabel));
            }

            // Equivalent-input metamorphic relation:
            // With exactly one visible series, row rendering order cannot change which single legend item exists.
            plot2.setRowRenderingOrder(plot2.getRowRenderingOrder().equals(SortOrder.ASCENDING) ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            LegendItemCollection singleReordered = r2.getLegendItems();
            int reorderedCount = singleReordered.getItemCount();
            String reorderedLabel = reorderedCount > 0 ? singleReordered.get(0).getLabel() : null;
            if (reorderedCount != 1 || !fuzzSeries.equals(reorderedLabel)) {
                throw new RuntimeException("[oracle:single-series-order-invariance] metamorphic violation: one-series legend should be invariant under row order inputSeries=" + escape(fuzzSeries) + " lhsCount=1 rhsCount=" + reorderedCount + " lhsLabel=" + escape(fuzzSeries) + " rhsLabel=" + escape(reorderedLabel));
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
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
## [113] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-seed-count] semantic mismatch: expected=1 actual=0

---
## [114] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,894 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r0;
        try {
            r0 = new LineAndShapeRenderer();
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection itemsNoPlot;
        try {
            itemsNoPlot = r0.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (itemsNoPlot == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-no-plot-not-null] semantic mismatch: expected non-null legend item collection but got null");
        }
        if (itemsNoPlot.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-no-plot-empty] semantic mismatch: expected 0 but got " + itemsNoPlot.getItemCount());
        }

        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S1";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C1";
        }
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        try {
            r = new LineAndShapeRenderer();
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);
        } catch (Throwable t) {
            return;
        }

        // Shared-state agreement check: setRenderer(plot) establishes the renderer's plot,
        // and getPlot() is documented to report "the plot that the renderer has been assigned to".
        if (r.getPlot() != plot) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:plot-assignment] semantic mismatch: expected renderer plot identity to equal assigned plot");
        }

        LegendItemCollection itemsEmptyDataset;
        try {
            itemsEmptyDataset = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (itemsEmptyDataset == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-empty-dataset-not-null] semantic mismatch: expected non-null legend item collection but got null");
        }
        if (itemsEmptyDataset.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-empty-dataset-empty] semantic mismatch: expected 0 but got " + itemsEmptyDataset.getItemCount());
        }

        try {
            dataset.addValue(new Double(value), rowKey, columnKey);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection itemsSingleSeries;
        CategoryPlot plotBefore;
        try {
            plotBefore = r.getPlot();
            itemsSingleSeries = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (itemsSingleSeries == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-single-series-not-null] semantic mismatch: expected non-null legend item collection but got null");
        }
        if (itemsSingleSeries.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but got " + itemsSingleSeries.getItemCount());
        }

        String actualLabel;
        try {
            actualLabel = itemsSingleSeries.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        if (!"S1".equals("S1")) {
            return;
        }
        if (!rowKey.equals(actualLabel)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-single-series-label] semantic mismatch: expected " + rowKey + " but got " + actualLabel);
        }

        // Post-condition / hidden-state check: getLegendItems() is a getter that reports legend items.
        // A throw-deleting or guard-only patch that skips intended work must still not silently mutate the
        // renderer's assigned plot; getPlot() must agree before and after this read-only call.
        CategoryPlot plotAfter = r.getPlot();
        if (plotBefore != plotAfter || plotAfter != plot) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getLegendItems-readonly-plot] semantic mismatch: getLegendItems changed or lost renderer plot association");
        }

        // Metamorphic/post-condition check: for the same one-series dataset, repeated reads must agree.
        // getLegendItems() computes a collection from current plot/dataset state; without intervening mutation,
        // item count and first label must remain the same across calls.
        LegendItemCollection itemsAgain;
        try {
            itemsAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsAgain == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getLegendItems-idempotent-not-null] semantic mismatch: repeated getLegendItems returned null");
        }
        if (itemsAgain.getItemCount() != itemsSingleSeries.getItemCount()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getLegendItems-idempotent-count] metamorphic violation: repeated getLegendItems disagreed on count lhs="
                    + itemsSingleSeries.getItemCount() + " rhs=" + itemsAgain.getItemCount());
        }
        if (itemsAgain.getItemCount() > 0) {
            String labelAgain;
            try {
                labelAgain = itemsAgain.get(0).getLabel();
            } catch (Throwable t) {
                return;
            }
            if ((actualLabel == null && labelAgain != null) || (actualLabel != null && !actualLabel.equals(labelAgain))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getLegendItems-idempotent-label] metamorphic violation: repeated getLegendItems disagreed on first label lhs="
                        + actualLabel + " rhs=" + labelAgain);
            }
        }
    }
}
```

</details>

---
## [115] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-series-count] semantic mismatch: expected 1 but got 0

---
## [116] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,858 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-empty] semantic mismatch: expected itemCount=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract from getPlot()/setPlot(): the renderer reports the plot it has been assigned to.
        // plot.setRenderer(r) is the real public API used to establish that assignment, so a silent
        // bookkeeping-only patch that fails to keep the shared plot field consistent violates this.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot-not-null] semantic mismatch: r.getLegendItems() after plot assignment returned null");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot-count] semantic mismatch: expected itemCount=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-not-null] semantic mismatch: populated legend collection was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-count] semantic mismatch: expected itemCount=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-label] semantic mismatch: expected label=S1 actual=" + label);
        }

        // Metamorphic/post-condition: getLegendItems() is a read-only query over renderer/plot/dataset state.
        // With no intervening mutation, repeated calls must agree on observable contents; a patch that merely
        // avoids the buggy branch or drops bookkeeping can return stale/wrong legend state without throwing.
        LegendItemCollection licAgain;
        try {
            licAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (licAgain == null || licAgain.getItemCount() != lic.getItemCount()
                || licAgain.getItemCount() != 1
                || !"S1".equals(licAgain.get(0).getLabel())) {
            String againLabel = (licAgain != null && licAgain.getItemCount() > 0) ? licAgain.get(0).getLabel() : "<missing>";
            throw new RuntimeException("[oracle:idempotent-read] metamorphic violation: repeated getLegendItems() on unchanged state disagreed firstCount="
                    + lic.getItemCount() + " secondCount=" + (licAgain == null ? -1 : licAgain.getItemCount())
                    + " firstLabel=" + label + " secondLabel=" + againLabel);
        }

        String fuzzColumn = data.consumeAsciiString(16);
        if (fuzzColumn.length() == 0) {
            fuzzColumn = "C";
        }
        int fuzzValue = data.consumeInt(-1000000, 1000000);

        DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
        CategoryPlot plot2 = new CategoryPlot();
        LineAndShapeRenderer r2 = new LineAndShapeRenderer();
        plot2.setDataset(dataset2);
        plot2.setRenderer(r2);

        if (r2.getPlot() != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        dataset2.addValue((double) fuzzValue, "S1", fuzzColumn);
        LegendItemCollection lic2;
        try {
            lic2 = r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        // Generalisation by equivalence to the lifted seed:
        // getLegendItems() returns legend items for series, and this dataset still has exactly one visible
        // series with the same row key "S1"; changing only category/value must not change the legend count
        // or that series label.
        if (lic2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:single-series-not-null] semantic mismatch: legend collection was null for single-series dataset");
        }
        if (lic2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:single-series-count] semantic mismatch: expected itemCount=1 actual=" + lic2.getItemCount() + " column=" + fuzzColumn + " value=" + fuzzValue);
        }
        String label2 = lic2.get(0).getLabel();
        if (!"S1".equals(label2)) {
            throw new FuzzerSecurityIssueLow("[oracle:single-series-label] semantic mismatch: expected label=S1 actual=" + label2 + " column=" + fuzzColumn + " value=" + fuzzValue);
        }
    }
}
```

</details>

---
## [117] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-populated-count] semantic mismatch: expected itemCount=1 actual=0

---
## [118] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,745 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runLiftedTestOracles();
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        LineAndShapeRenderer r;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        String rowKey;
        String columnKey;
        try {
            r = new LineAndShapeRenderer();
            plot = new CategoryPlot();
            dataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey == null || rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey == null || columnKey.length() == 0) {
                columnKey = "C";
            }

            plot.setDataset(dataset);
            plot.setRenderer(r);
            dataset.addValue(1.0, rowKey, columnKey);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection items;
        LegendItemCollection itemsAgain;
        CategoryPlot plotBefore;
        CategoryPlot plotAfter;
        try {
            plotBefore = r.getPlot();
            items = r.getLegendItems();
            plotAfter = r.getPlot();
            itemsAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label = items.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label + " columnKey=" + columnKey);
        }

        // Contract/state-coupling check: setRenderer()/setPlot establishes the renderer's plot; getLegendItems() is a query method over that shared state and should not silently change it.
        if (plotBefore != plot || plotAfter != plot || r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("relation plot-state-preserved violated: expected renderer plot identity to remain the assigned plot before/after getLegendItems but saw before="
                    + id(plotBefore) + " assigned=" + id(plot) + " after=" + id(plotAfter) + " final=" + id(r.getPlot()));
        }

        // Metamorphic idempotence check: for unchanged renderer/plot/dataset state, repeated getLegendItems() calls must agree.
        if (itemsAgain == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-idempotent violated: second getLegendItems() returned null");
        }
        if (itemsAgain.getItemCount() != items.getItemCount()) {
            throw new FuzzerSecurityIssueLow("relation legendItems-idempotent violated: firstCount=" + items.getItemCount() + " secondCount=" + itemsAgain.getItemCount()
                    + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (itemsAgain.getItemCount() > 0) {
            String labelAgain = itemsAgain.get(0).getLabel();
            if (!safeEquals(label, labelAgain)) {
                throw new FuzzerSecurityIssueLow("relation legendItems-idempotent violated: firstLabel=" + label + " secondLabel=" + labelAgain
                        + " rowKey=" + rowKey + " columnKey=" + columnKey);
            }
        }

        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        LegendItemCollection noPlotItemsAgain;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
            noPlotItemsAgain = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + noPlotItems.getItemCount());
        }
        // Hidden-state check: a read-only query on an unassigned renderer must not assign a plot or change the empty result on repetition.
        if (noPlotRenderer.getPlot() != null) {
            throw new FuzzerSecurityIssueLow("relation noPlot-query-does-not-assign-plot violated: getLegendItems() changed plot from null to " + id(noPlotRenderer.getPlot()));
        }
        if (noPlotItemsAgain == null || noPlotItemsAgain.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-idempotent violated: firstCount=0 secondCount="
                    + (noPlotItemsAgain == null ? "null" : String.valueOf(noPlotItemsAgain.getItemCount())));
        }
    }

    private static void runLiftedTestOracles() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull] semantic mismatch: expected non-null legend items but got null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-count] semantic mismatch: expected 0 but got " + initial.getItemCount());
        }
        // Shared-state agreement from the class contract: with no assigned plot, getPlot() must report null, and a question method like getLegendItems() must not change that shared field.
        if (r.getPlot() != null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-plot-state] semantic mismatch: expected null plot but got " + id(r.getPlot()));
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-setplot-agreement] semantic mismatch: expected renderer.getPlot() to be the assigned plot");
        }

        LegendItemCollection emptyAttached = r.getLegendItems();
        if (emptyAttached == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-notnull] semantic mismatch: expected non-null legend items but got null");
        }
        if (emptyAttached.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-count] semantic mismatch: expected 0 but got " + emptyAttached.getItemCount());
        }
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-plot-state] semantic mismatch: expected plot identity to remain assigned after getLegendItems()");
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-notnull] semantic mismatch: expected non-null legend items but got null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but got " + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-label] semantic mismatch: expected S1 but got " + actualLabel);
        }

        // Metamorphic/post-condition check: after adding one series and not mutating state further, repeated getLegendItems() must continue to report the same single legend item.
        LegendItemCollection lic2 = r.getLegendItems();
        if (lic2 == null || lic2.getItemCount() != 1 || lic2.get(0) == null || !"S1".equals(lic2.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-idempotent-read] semantic mismatch: repeated getLegendItems() disagreed with established state; count="
                    + (lic2 == null ? "null" : String.valueOf(lic2.getItemCount()))
                    + " label=" + ((lic2 == null || lic2.getItemCount() == 0 || lic2.get(0) == null) ? "null" : lic2.get(0).getLabel()));
        }
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-post-read-plot-state] semantic mismatch: expected plot identity to remain assigned after repeated getLegendItems()");
        }
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String id(Object o) {
        return o == null ? "null" : o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
```

</details>

---
## [119] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-series-count] semantic mismatch: expected 1 but got 0

---
## [120] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,466 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null] semantic mismatch: getLegendItems() returned null before plot assignment");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-no-plot] semantic mismatch: expected itemCount=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract from getPlot()/setPlot() docs: the renderer reports the plot it has been assigned to.
        // This shared-state check matters because getLegendItems() reads the same 'plot' field; a patch that
        // merely suppresses behavior in getLegendItems() could still leave the renderer/plot coupling wrong.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-coupling] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyAttached = r.getLegendItems();
        if (emptyAttached.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-attached] semantic mismatch: expected itemCount=0 actual=" + emptyAttached.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-count] semantic mismatch: expected itemCount=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-label] semantic mismatch: expected label=S1 actual=" + String.valueOf(label));
        }

        try {
            // Metamorphic relation: with no intervening state change, repeated reads of getLegendItems()
            // for the same renderer/plot/dataset state must agree on observable contents.
            LegendItemCollection lic2 = r.getLegendItems();
            int c1 = lic.getItemCount();
            int c2 = lic2.getItemCount();
            String l1 = c1 > 0 ? lic.get(0).getLabel() : null;
            String l2 = c2 > 0 ? lic2.get(0).getLabel() : null;
            boolean same = c1 == c2 && (l1 == null ? l2 == null : l1.equals(l2));
            if (!same) {
                throw new RuntimeException("[oracle:idempotent-read] metamorphic violation: repeated getLegendItems() changed without state update count1=" + c1 + " count2=" + c2 + " label1=" + String.valueOf(l1) + " label2=" + String.valueOf(l2));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            AbstractCategoryItemRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            int rows = data.consumeInt(0, 3);
            String columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }
            for (int i = 0; i < rows; i++) {
                String rowKey = data.consumeAsciiString(8);
                if (rowKey.length() == 0) {
                    rowKey = "S" + i;
                }
                double value = data.consumeInt(-1000, 1000);
                dataset2.addValue(value, rowKey, columnKey);
                r2.setSeriesVisibleInLegend(i, Boolean.valueOf(data.consumeBoolean()));
            }
            r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [121] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-count] semantic mismatch: expected itemCount=1 actual=0

---
## [122] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,645 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        try {
            // Lifted exactly from AbstractCategoryItemRendererTests.test2947660.
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

            LegendItemCollection itemsNoPlot = r.getLegendItems();
            if (itemsNoPlot == null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() expected non-null but was null");
            }
            if (itemsNoPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-empty-without-plot] semantic mismatch: expected 0 but was "
                        + itemsNoPlot.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            LegendItemCollection itemsEmptyDataset = r.getLegendItems();
            if (itemsEmptyDataset.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-empty-with-plot] semantic mismatch: expected 0 but was "
                        + itemsEmptyDataset.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-single-series-count] semantic mismatch: expected 1 but was "
                        + lic.getItemCount());
            }
            String liftedLabel = lic.get(0).getLabel();
            if (!"S1".equals(liftedLabel)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-single-series-label] semantic mismatch: expected S1 but was "
                        + String.valueOf(liftedLabel));
            }

            // Shared-state agreement check: plot.setRenderer(r) is the public API setup used by the test,
            // and getPlot()/setPlot share the renderer's plot field. A correct implementation must report
            // the same plot that the writer-side setup established.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:plot-state-agreement] semantic mismatch: renderer reported different plot instance");
            }

            // Read-only post-condition on a get* method: getLegendItems() is documented as returning the
            // legend items for the renderer; it should not silently mutate plot assignment or change the
            // observable legend result on identical state. A patch that merely skips intended behavior can
            // violate this stable observable even though no exception occurs.
            LegendItemCollection licAgain = r.getLegendItems();
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:post-readonly-plot] semantic mismatch: getLegendItems() changed renderer plot state");
            }
            if (licAgain == null || licAgain.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:post-repeat-count] semantic mismatch: repeated getLegendItems() expected 1 but was "
                        + (licAgain == null ? "null" : String.valueOf(licAgain.getItemCount())));
            }
            String liftedLabelAgain = licAgain.get(0).getLabel();
            if (!"S1".equals(liftedLabelAgain)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:post-repeat-label] semantic mismatch: repeated getLegendItems() expected S1 but was "
                        + String.valueOf(liftedLabelAgain));
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Exception e) {
            return;
        }

        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection items2;
        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(value, rowKey, columnKey);
            items2 = r2.getLegendItems();
        } catch (Exception e) {
            return;
        }

        // Candidate invariant/generalisation: with a non-null dataset containing exactly one series,
        // getLegendItems() must return exactly one legend item for that series, and its label must match
        // the series row key. This is the same real API path as the failing test, generalized by choosing
        // the known answer first (the row key we inserted).
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got "
                    + items2.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label2 = items2.get(0).getLabel();
        if (!rowKey.equals(label2)) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: expected legend label "
                    + rowKey + " but got " + String.valueOf(label2));
        }

        LegendItemCollection items2Again;
        try {
            items2Again = r2.getLegendItems();
        } catch (Exception e) {
            return;
        }

        // Additional post-condition/metamorphic check: repeated queries on unchanged state must agree.
        if (items2Again == null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-repeat-nonnull] semantic mismatch: second getLegendItems() returned null");
        }
        if (items2Again.getItemCount() != items2.getItemCount()) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-repeat-count] semantic mismatch: first count="
                    + items2.getItemCount() + " second count=" + items2Again.getItemCount()
                    + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label2Again = items2Again.get(0).getLabel();
        if (!rowKey.equals(label2Again)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-repeat-label] semantic mismatch: expected "
                    + rowKey + " but second call returned " + String.valueOf(label2Again));
        }
        if (r2.getPlot() != plot2) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-plot-stable] semantic mismatch: getLegendItems() changed or lost plot association");
        }

        LineAndShapeRenderer r3;
        LegendItemCollection items3;
        try {
            r3 = new LineAndShapeRenderer();
            items3 = r3.getLegendItems();
        } catch (Exception e) {
            return;
        }

        // Candidate invariant/generalisation: without an assigned plot, getLegendItems() is non-null and empty.
        if (items3 == null) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items3.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got "
                    + items3.getItemCount());
        }
    }
}
```

</details>

---
## [123] ⚙️ harness-repair · `attempt_024`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [124] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-single-series-count] semantic mismatch: expected 1 but was 0

---
## [125] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,131 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-notnull] semantic mismatch: getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        /* Contract check on shared state: CategoryItemRenderer#getPlot() returns
           the plot that the renderer has been assigned to, and CategoryPlot#setRenderer(...)
           is the real API path that performs that assignment in normal usage.
           A patch that merely suppresses legend generation or skips bookkeeping
           would still violate this observable state agreement if assignment were broken. */
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-notnull] semantic mismatch: getLegendItems() returned null after plot assignment");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-notnull] semantic mismatch: getLegendItems() returned null after adding data");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        try {
            /* Trusted oracle by construction: we choose the dataset row keys first,
               add exactly one value per row, and with default legend visibility a
               correct getLegendItems() must expose one legend item per dataset row
               for the renderer's dataset. This directly observes the post-condition
               the patched method is supposed to produce; a "fix" that simply returns
               an empty collection or skips dataset traversal breaks it silently. */
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            String sA = nonEmptyAscii(data.consumeAsciiString(8), "A");
            String sB = nonEmptyAscii(data.consumeAsciiString(8), "B");
            String c1 = nonEmptyAscii(data.consumeAsciiString(8), "C1");
            String c2 = nonEmptyAscii(data.consumeAsciiString(8), "C2");
            if (sA.equals(sB)) {
                sB = sB + "_2";
            }
            if (c1.equals(c2)) {
                c2 = c2 + "_2";
            }

            dataset2.addValue(1.0, sA, c1);
            dataset2.addValue(2.0, sB, c2);

            LegendItemCollection lic2 = r2.getLegendItems();
            if (lic2 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-notnull] semantic mismatch: getLegendItems() returned null");
            }
            if (lic2.getItemCount() != dataset2.getRowCount()) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructed-count] semantic mismatch: inputRows=" + dataset2.getRowCount()
                        + " legendCount=" + lic2.getItemCount()
                );
            }

            /* Metamorphic/post-condition relation: because the plot default row rendering
               order is ASCENDING and we inserted rows in order sA then sB, the legend
               labels must agree with those established row keys. This is a real-call vs
               real-state check, not a hand-rolled implementation. */
            String l0 = lic2.get(0).getLabel();
            String l1 = lic2.get(1).getLabel();
            if (!sA.equals(l0) || !sB.equals(l1)) {
                throw new RuntimeException(
                    "[oracle:constructed-label-order] metamorphic violation: legend labels must match dataset row keys in ascending row order"
                        + " sA=" + sA + " sB=" + sB + " lhs0=" + String.valueOf(l0) + " lhs1=" + String.valueOf(l1)
                );
            }

            if (r2.getPlot() != plot2) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-plot-state] semantic mismatch: expected renderer plot identity to match assigned plot");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static String nonEmptyAscii(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}
```

</details>

---
## [126] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-populated-count] semantic mismatch: expected=1 actual=0

---
## [127] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,910 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String fuzzRowKey = data.consumeAsciiString(8);
        if (fuzzRowKey.length() == 0) {
            fuzzRowKey = "S";
        }
        String fuzzColumnKey = data.consumeAsciiString(8);
        if (fuzzColumnKey.length() == 0) {
            fuzzColumnKey = "C";
        }
        double fuzzValue = data.consumeInt(-1000, 1000);

        // Lifted oracle 1 from test2947660:
        // assertNotNull(r.getLegendItems());
        // assertEquals(0, r.getLegendItems().getItemCount());
        LineAndShapeRenderer r1;
        LegendItemCollection itemsNoPlot;
        try {
            r1 = new LineAndShapeRenderer();
            itemsNoPlot = r1.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsNoPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (itemsNoPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected itemCount=0 actual=" + itemsNoPlot.getItemCount());
        }

        // State-coupling check on shared field 'plot':
        // setPlot()/getPlot() must agree on the same field value.
        LineAndShapeRenderer rPlot;
        CategoryPlot explicitPlot;
        CategoryPlot observedPlot;
        try {
            rPlot = new LineAndShapeRenderer();
            explicitPlot = new CategoryPlot();
            rPlot.setPlot(explicitPlot);
            observedPlot = rPlot.getPlot();
        } catch (Throwable t) {
            return;
        }
        if (observedPlot != explicitPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: setPlot/getPlot disagree expectedSame=true actualSame=false");
        }

        // Lifted oracle 2 from test2947660:
        // DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // CategoryPlot plot = new CategoryPlot();
        // plot.setDataset(dataset);
        // plot.setRenderer(r);
        // assertEquals(0, r.getLegendItems().getItemCount());
        LineAndShapeRenderer r2;
        DefaultCategoryDataset dataset2;
        CategoryPlot plot2;
        LegendItemCollection itemsEmptyDataset;
        try {
            r2 = new LineAndShapeRenderer();
            dataset2 = new DefaultCategoryDataset();
            plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            itemsEmptyDataset = r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsEmptyDataset == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (itemsEmptyDataset.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-empty] semantic mismatch: expected itemCount=0 actual=" + itemsEmptyDataset.getItemCount());
        }

        // Lifted oracle 3 from test2947660:
        // dataset.addValue(1.0, "S1", "C1");
        // LegendItemCollection lic = r.getLegendItems();
        // assertEquals(1, lic.getItemCount());
        // assertEquals("S1", lic.get(0).getLabel());
        LineAndShapeRenderer r3;
        DefaultCategoryDataset dataset3;
        CategoryPlot plot3;
        LegendItemCollection lic;
        try {
            r3 = new LineAndShapeRenderer();
            dataset3 = new DefaultCategoryDataset();
            plot3 = new CategoryPlot();
            plot3.setDataset(dataset3);
            plot3.setRenderer(r3);
            dataset3.addValue(1.0, "S1", "C1");
            lic = r3.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-count] semantic mismatch: expected itemCount=1 actual=" + lic.getItemCount());
        }
        String label0;
        try {
            label0 = lic.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(label0)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-label] semantic mismatch: expected label=S1 actual=" + label0);
        }

        // Mandatory generalisation from a known answer:
        // For a real LineAndShapeRenderer attached to a plot with a non-null dataset containing exactly one series,
        // getLegendItems() should report exactly one legend item for that series; this is the same contract the failing
        // test fixes, just with fuzz-chosen non-empty row/column keys.
        LineAndShapeRenderer r4;
        DefaultCategoryDataset dataset4;
        CategoryPlot plot4;
        LegendItemCollection fuzzItems;
        try {
            r4 = new LineAndShapeRenderer();
            dataset4 = new DefaultCategoryDataset();
            plot4 = new CategoryPlot();
            plot4.setDataset(dataset4);
            plot4.setRenderer(r4);
            dataset4.addValue(fuzzValue, fuzzRowKey, fuzzColumnKey);
            fuzzItems = r4.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount());
        }
        String fuzzLabel;
        try {
            fuzzLabel = fuzzItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!fuzzRowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + fuzzRowKey + " but got " + fuzzLabel);
        }

        // Required non-crash post-condition / hidden-state check:
        // getLegendItems() is a query ("Returns the legend item collection") and should not silently mutate the renderer
        // or dataset-visible result. A throw-deleting or guard-adding patch that skips bookkeeping can make repeated reads
        // disagree. So two consecutive calls on unchanged state must yield the same observable count and label.
        LineAndShapeRenderer r5;
        DefaultCategoryDataset dataset5;
        CategoryPlot plot5;
        LegendItemCollection firstRead;
        LegendItemCollection secondRead;
        try {
            r5 = new LineAndShapeRenderer();
            dataset5 = new DefaultCategoryDataset();
            plot5 = new CategoryPlot();
            plot5.setDataset(dataset5);
            plot5.setRenderer(r5);
            dataset5.addValue(1.0, fuzzRowKey, fuzzColumnKey);
            firstRead = r5.getLegendItems();
            secondRead = r5.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (firstRead == null || secondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legend-idempotent-nonnull] semantic mismatch: repeated getLegendItems returned null first=" + (firstRead == null) + " second=" + (secondRead == null));
        }
        int firstCount;
        int secondCount;
        try {
            firstCount = firstRead.getItemCount();
            secondCount = secondRead.getItemCount();
        } catch (Throwable t) {
            return;
        }
        if (firstCount != secondCount) {
            throw new FuzzerSecurityIssueLow("[oracle:legend-idempotent-count] metamorphic violation: repeated getLegendItems on unchanged renderer state differed firstCount=" + firstCount + " secondCount=" + secondCount);
        }
        if (firstCount == 1 && secondCount == 1) {
            String firstLabel;
            String secondLabel;
            try {
                firstLabel = firstRead.get(0).getLabel();
                secondLabel = secondRead.get(0).getLabel();
            } catch (Throwable t) {
                return;
            }
            if (firstLabel == null ? secondLabel != null : !firstLabel.equals(secondLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:legend-idempotent-label] metamorphic violation: repeated getLegendItems on unchanged renderer state differed firstLabel=" + firstLabel + " secondLabel=" + secondLabel);
            }
        }

        // Candidate relation from prompt: no plot => non-null empty collection.
        LineAndShapeRenderer r6;
        LegendItemCollection items6;
        try {
            r6 = new LineAndShapeRenderer();
            items6 = r6.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items6 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items6.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items6.getItemCount());
        }
    }
}
```

</details>

---
## [128] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:test2947660-single-count] semantic mismatch: expected itemCount=1 actual=0

---
## [129] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,254 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-no-plot] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: expected renderer.getPlot()==plot after plot.setRenderer(r), actual renderer.getPlot()=" + r.getPlot() + " plot=" + plot);
        }

        LegendItemCollection emptyWithPlot = r.getLegendItems();
        if (emptyWithPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-plot] semantic mismatch: expected=0 actual=" + emptyWithPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-series] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label] semantic mismatch: expected=S1 actual=" + label);
        }

        try {
            /* Contract/invariant used: getLegendItems() reports legend items from current renderer/plot/dataset state.
               With no intervening state change, a second call must agree with the first; a "fix" that merely skips the
               real legend-building path or returns a stale/empty result violates this observable post-condition. */
            LegendItemCollection lic2 = r.getLegendItems();
            int count2 = lic2.getItemCount();
            String label2 = count2 > 0 ? lic2.get(0).getLabel() : null;
            if (count2 != lic.getItemCount() || (count2 > 0 && !label.equals(label2))) {
                throw new RuntimeException("[oracle:idempotent-legend] metamorphic violation: repeated getLegendItems() without state change must agree input=rowKey:S1,columnKey:C1,value:1.0 lhsCount=" + lic.getItemCount() + " rhsCount=" + count2 + " lhsLabel=" + label + " rhsLabel=" + label2);
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
## [130] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-one-series] semantic mismatch: expected=1 actual=0

---
## [131] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,153 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-not-null] semantic mismatch: expected non-null legend collection but got null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-empty] semantic mismatch: expected 0 legend items without plot but got " + noPlotItems.getItemCount());
        }

        LineAndShapeRenderer seedRenderer;
        CategoryPlot seedPlot;
        DefaultCategoryDataset seedDataset;
        LegendItemCollection seedItemsBeforeAdd;
        LegendItemCollection seedItemsAfterAdd;
        try {
            seedRenderer = new LineAndShapeRenderer();
            seedPlot = new CategoryPlot();
            seedDataset = new DefaultCategoryDataset();

            seedPlot.setDataset(seedDataset);
            seedPlot.setRenderer(seedRenderer);

            seedItemsBeforeAdd = seedRenderer.getLegendItems();
            seedDataset.addValue(1.0, "S1", "C1");
            seedItemsAfterAdd = seedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (seedRenderer.getPlot() != seedPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer.getPlot() to be the assigned plot");
        }
        if (seedItemsBeforeAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-before-not-null] semantic mismatch: expected non-null legend collection before add but got null");
        }
        if (seedItemsBeforeAdd.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-before-empty] semantic mismatch: expected 0 legend items before adding data but got " + seedItemsBeforeAdd.getItemCount());
        }
        if (seedItemsAfterAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-after-not-null] semantic mismatch: expected non-null legend collection after add but got null");
        }
        if (seedItemsAfterAdd.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-after-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got " + seedItemsAfterAdd.getItemCount());
        }
        String seedLabel;
        try {
            seedLabel = seedItemsAfterAdd.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(seedLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-after-label] semantic mismatch: expected label S1 but got " + String.valueOf(seedLabel));
        }

        LegendItemCollection seedItemsAfterSecondRead;
        try {
            seedItemsAfterSecondRead = seedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (seedRenderer.getPlot() != seedPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-stable-after-read] semantic mismatch: getLegendItems() changed renderer plot association");
        }
        if (seedItemsAfterSecondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-not-null] semantic mismatch: repeated getLegendItems() returned null");
        }
        if (seedItemsAfterSecondRead.getItemCount() != seedItemsAfterAdd.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-count-stable] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset changed itemCount from " + seedItemsAfterAdd.getItemCount() + " to " + seedItemsAfterSecondRead.getItemCount());
        }
        String secondLabel;
        try {
            secondLabel = seedItemsAfterSecondRead.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!seedLabel.equals(secondLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-label-stable] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset changed label from " + seedLabel + " to " + secondLabel);
        }

        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        int numeric = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer fuzzRenderer;
        CategoryPlot fuzzPlot;
        DefaultCategoryDataset fuzzDataset;
        LegendItemCollection fuzzItems;
        try {
            fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot = new CategoryPlot();
            fuzzDataset = new DefaultCategoryDataset();
            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);
            fuzzDataset.addValue((double) numeric, rowKey, columnKey);
            fuzzItems = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (fuzzRenderer.getPlot() != fuzzPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-assignment] semantic mismatch: expected renderer.getPlot() to equal assigned plot for fuzz case");
        }
        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey + " value=" + numeric);
        }
        String fuzzLabel;
        try {
            fuzzLabel = fuzzItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!rowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + String.valueOf(fuzzLabel) + " columnKey=" + columnKey + " value=" + numeric);
        }

        LegendItemCollection fuzzItemsAgain;
        try {
            fuzzItemsAgain = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (fuzzItemsAgain == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-repeat-not-null] semantic mismatch: repeated getLegendItems() returned null");
        }
        if (fuzzItemsAgain.getItemCount() != fuzzItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-repeat-count] metamorphic violation: repeated getLegendItems() on unchanged one-series dataset changed itemCount from " + fuzzItems.getItemCount() + " to " + fuzzItemsAgain.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey + " value=" + numeric);
        }
        String fuzzLabelAgain;
        try {
            fuzzLabelAgain = fuzzItemsAgain.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!fuzzLabel.equals(fuzzLabelAgain)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-repeat-label] metamorphic violation: repeated getLegendItems() on unchanged one-series dataset changed label from " + fuzzLabel + " to " + fuzzLabelAgain + " rowKey=" + rowKey + " columnKey=" + columnKey + " value=" + numeric);
        }
    }
}
```

</details>

---
## [132] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-after-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, "S1", "C1") but got 0

---
## [133] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [74] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,029 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-no-plot] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Documented sibling agreement on shared field 'plot':
        // getPlot() "Returns the plot that the renderer has been assigned to";
        // setPlot(CategoryPlot) "Sets the plot that the renderer has been assigned to."
        // CategoryPlot.setRenderer(r) is the real API path that assigns the renderer to the plot,
        // so after that call the renderer must report the same plot. A "fix" that only dodges the
        // wrong branch in getLegendItems but breaks/omits renderer assignment would violate this.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer plot to be the assigned CategoryPlot");
        }

        LegendItemCollection emptyOnAssignedPlot = r.getLegendItems();
        if (emptyOnAssignedPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-empty-dataset] semantic mismatch: expected=0 actual=" + emptyOnAssignedPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-after-add] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label-after-add] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        // Post-condition / metamorphic check:
        // getLegendItems() is a read-only query over the renderer/plot/dataset state. With no state
        // changes between calls, repeated calls must agree on the observable legend contents. A patch
        // that merely makes the buggy branch unreachable or silently skips legend population could
        // break this getter's intended behavior; checking stability ensures we read back the state
        // that was already established by the real API setup above.
        LegendItemCollection licAgain = r.getLegendItems();
        if (licAgain.getItemCount() != lic.getItemCount()) {
            throw new RuntimeException("[oracle:idempotent-legend] metamorphic violation: repeated getLegendItems() changed itemCount lhs=" + lic.getItemCount() + " rhs=" + licAgain.getItemCount());
        }
        if (licAgain.getItemCount() > 0) {
            String labelAgain = licAgain.get(0).getLabel();
            if ((label == null && labelAgain != null) || (label != null && !label.equals(labelAgain))) {
                throw new RuntimeException("[oracle:idempotent-legend-label] metamorphic violation: repeated getLegendItems() changed first label lhs=" + String.valueOf(label) + " rhs=" + String.valueOf(labelAgain));
            }
        }

        try {
            int extraSeries = data.consumeInt(0, 4);
            DefaultCategoryDataset fuzzDataset = new DefaultCategoryDataset();
            CategoryPlot fuzzPlot = new CategoryPlot();
            LineAndShapeRenderer fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);

            if (fuzzRenderer.getPlot() != fuzzPlot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: expected renderer plot to be the assigned CategoryPlot");
            }

            for (int i = 0; i < extraSeries; i++) {
                String row = data.consumeAsciiString(8);
                String col = data.consumeAsciiString(8);
                if (row.length() == 0) {
                    row = "R" + i;
                }
                if (col.length() == 0) {
                    col = "C" + i;
                }
                double v = data.consumeInt(-1000, 1000);
                fuzzDataset.addValue(v, row, col);
            }

            LegendItemCollection a = fuzzRenderer.getLegendItems();
            LegendItemCollection b = fuzzRenderer.getLegendItems();
            if (a.getItemCount() != b.getItemCount()) {
                throw new RuntimeException("[oracle:idempotent-legend-fuzz] metamorphic violation: repeated getLegendItems() changed itemCount lhs=" + a.getItemCount() + " rhs=" + b.getItemCount());
            }
            for (int i = 0; i < a.getItemCount(); i++) {
                String la = a.get(i).getLabel();
                String lb = b.get(i).getLabel();
                if ((la == null && lb != null) || (la != null && !la.equals(lb))) {
                    throw new RuntimeException("[oracle:idempotent-legend-fuzz-label] metamorphic violation: repeated getLegendItems() changed label index=" + i + " lhs=" + String.valueOf(la) + " rhs=" + String.valueOf(lb));
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
## [134] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-count-after-add] semantic mismatch: expected=1 actual=0

---
## [135] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,802 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exactLiftedTestOracle();
        noPlotOracle();
        singleSeriesFuzzOracle(data);
    }

    private static void exactLiftedTestOracle() {
        AbstractCategoryItemRenderer r;
        LegendItemCollection items0;
        LegendItemCollection items1;
        LegendItemCollection items2;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        try {
            r = new LineAndShapeRenderer();

            items0 = r.getLegendItems();
            if (items0 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-initial] semantic mismatch: expected non-null legend collection but was null");
            }
            if (items0.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-initial] semantic mismatch: expected 0 but was " + items0.getItemCount());
            }

            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state-coupling] semantic mismatch: expected getPlot() to return assigned plot instance");
            }

            items1 = r.getLegendItems();
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-readonly-query] metamorphic violation: getLegendItems() is documented as a query over the renderer's assigned plot and should not change the shared plot field; beforeAfterPlotIdentity differed");
            }
            if (items1 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-empty-dataset] semantic mismatch: expected non-null legend collection but was null");
            }
            if (items1.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-empty-dataset] semantic mismatch: expected 0 but was " + items1.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            items2 = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-single-series] semantic mismatch: expected non-null legend collection but was null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-count] semantic mismatch: expected 1 but was " + items2.getItemCount());
        }
        String label = items2.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-label] semantic mismatch: expected S1 but was " + label);
        }

        LegendItemCollection items3;
        try {
            items3 = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items3 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-non-null] metamorphic violation: repeated getLegendItems() returned null");
        }
        int count3 = items3.getItemCount();
        String label3 = count3 > 0 ? items3.get(0).getLabel() : null;
        if (count3 != items2.getItemCount() || (label == null ? label3 != null : !label.equals(label3))) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-query] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset should agree for count and first label; firstCount="
                    + items2.getItemCount() + " secondCount=" + count3 + " firstLabel=" + label + " secondLabel=" + label3);
        }
    }

    private static void noPlotOracle() {
        LineAndShapeRenderer r;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            items = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-not-null] semantic mismatch: expected non-null legend collection but was null");
        }
        if (items.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-empty] semantic mismatch: expected 0 but was " + items.getItemCount());
        }
        if (r.getPlot() != null) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-state] semantic mismatch: expected getPlot() to be null for fresh renderer");
        }
    }

    private static void singleSeriesFuzzOracle(FuzzedDataProvider data) {
        String rowKey = sanitizeKey(data.consumeAsciiString(8), "S");
        String columnKey = sanitizeKey(data.consumeAsciiString(8), "C");
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            plot = new CategoryPlot();
            dataset = new DefaultCategoryDataset();
            plot.setDataset(dataset);
            plot.setRenderer(r);
            dataset.addValue(value, rowKey, columnKey);
            items = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-single-series-not-null] relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-single-series-count] relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount());
        }
        String label = items.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-single-series-label] relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label);
        }

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-state-coupling] semantic mismatch: expected getPlot() to return assigned plot instance");
        }

        LegendItemCollection itemsAgain;
        try {
            itemsAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsAgain == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-non-null] metamorphic violation: repeated getLegendItems() returned null");
        }
        int countAgain = itemsAgain.getItemCount();
        String labelAgain = countAgain > 0 ? itemsAgain.get(0).getLabel() : null;
        if (countAgain != 1 || !rowKey.equals(labelAgain)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-query] metamorphic violation: repeated getLegendItems() on unchanged single-series dataset should still report one item labeled with the series key; count="
                    + countAgain + " label=" + labelAgain + " expectedLabel=" + rowKey);
        }
    }

    private static String sanitizeKey(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}
```

</details>

---
## [136] ⚙️ harness-repair · `attempt_030`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [137] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-single-series-count] semantic mismatch: expected 1 but was 0

---
## [138] ⚙️ coverage · `jazzer coverage dumps`
**output:** **30/30 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov; 30 raw fuzz log(s)**
<details><summary>▸ detail (~16,929 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 1720}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 2214}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 2219}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 2221}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 2216}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 2223}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 2216}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 2219}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 2217}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 2217}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 2217}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 2219}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 2215}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 2220}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 2217}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 2216}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 2223}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 2216}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 2220}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 2216}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 2221}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 2213}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 2218}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 2220}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
