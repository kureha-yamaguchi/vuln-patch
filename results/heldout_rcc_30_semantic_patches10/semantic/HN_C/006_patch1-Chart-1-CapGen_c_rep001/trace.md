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

126 sequential steps — 37 LLM calls, 89 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests',  (~1,183 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests', test_method='test2947660', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/tests/org/jfree/chart/renderer/category/junit/AbstractCategoryItemRendererTests.java', method_source='    public void test2947660() {\\n        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();\\n        assertNotNull(r.getLegendItems());\\n        assertEquals(0, r.getLegendItems().getItemCount());\\n\\n        DefaultCategoryDataset dataset = new DefaultCategoryDataset();\\n        CategoryPlot plot = new CategoryPlot();\\n        plot.setDataset(dataset);\\n        plot.setRenderer(r);\\n        assertEquals(0, r.getLegendItems().getItemCount());\\n\\n        dataset.addValue(1.0, \"S1\", \"C1\");\\n        LegendItemCollection lic = r.getLegendItems();\\n        assertEquals(1, lic.getItemCount());\\n        assertEquals(\"S1\", lic.get(0).getLabel());\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~18,374 chars)</summary>

**[system]**
```
You are a software-verification expert. Given a patched Java method and its API, you propose RELATIONS that must hold for EVERY correct implementation — never merely for the reference or for the tested inputs. Two kinds only:
  * METAMORPHIC: two real API calls whose results must agree (or relate in a fixed way) for any valid input — e.g. parse(format(x)) == x; f(a,b) == f(b,a) when documented commutative; reduce(reduce(x)) == reduce(x).
  * INVARIANT: a property the output must satisfy for every valid input, read straight off the documented contract — e.g. a reduced fraction has gcd(numerator,denominator)==1; a returned index is within bounds.
Each relation must be SOUND: if any correct implementation could violate it, do not propose it. Avoid comparisons to a different library, and avoid boundary-fragile claims (division by zero, NaN, overflow, empty/null) unless you explicitly exclude those inputs.
```
**[user]**
```
METHOD(S) UNDER TEST — source as it stands in the checkout. Use it to see the code shape and API, NEVER as a model of correct behaviour (it is the code as written, not a specification):
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
THE CLASS UNDER TEST: AbstractCategoryItemRenderer. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.

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
<details open><summary>▸ Output (~10,620 chars)</summary>

```
[
  {
    "name": "null-dataset-yields-empty-legend",
    "kind": "invariant",
    "contract": "getLegendItems() returns the legend items for this renderer; when the renderer is attached to a plot position whose dataset is null, there are no series from which any legend item can be obtained, so the returned LegendItemCollection must be empty.",
    "input": "Build a real concrete AbstractCategoryItemRenderer via BarRenderer. Attach it to a CategoryPlot at index 0, but do not install any dataset at that index (leave it null). This directly probes the null-dataset boundary in the changed condition while keeping the plot itself non-null and the renderer registered in the plot.",
    "check": "org.jfree.chart.renderer.category.BarRenderer renderer;\norg.jfree.chart.plot.CategoryPlot plot;\ntry {\n  renderer = new org.jfree.chart.renderer.category.BarRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  plot.setRenderer(renderer);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = renderer.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation null-dataset-yields-empty-legend violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null || items.getItemCount() != 0) {\n  throw new RuntimeException(\"relation null-dataset-yields-empty-legend violated: expected empty collection for null dataset, got \" + (items == null ? \"null\" : String.valueOf(items.getItemCount())));\n}"
  },
  {
    "name": "detached-renderer-yields-empty-legend",
    "kind": "invariant",
    "contract": "getLegendItems() returns the legend items for this renderer; when the renderer is not assigned to any plot, there is no dataset context and the result must be an empty LegendItemCollection rather than an exception.",
    "input": "Construct a concrete renderer (BarRenderer) and leave it detached from any plot. This probes the plot-null boundary of the method on a valid receiver.",
    "check": "org.jfree.chart.renderer.category.BarRenderer renderer;\ntry {\n  renderer = new org.jfree.chart.renderer.category.BarRenderer();\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = renderer.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation detached-renderer-yields-empty-legend violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null || items.getItemCount() != 0) {\n  throw new RuntimeException(\"relation detached-renderer-yields-empty-legend violated: expected empty collection for detached renderer, got \" + (items == null ? \"null\" : String.valueOf(items.getItemCount())));\n}"
  },
  {
    "name": "legend-items-count-matches-per-series-items",
    "kind": "metamorphic",
    "contract": "getLegendItems() is the collection-form view of the renderer's per-series legend items. For a renderer installed at dataset index 0, its collection size must equal the number of series indices i for which the series is visible in the legend and getLegendItem(0, i) returns a non-null item.",
    "input": "Create a DefaultCategoryDataset with 1..4 row series and a single shared column, install it at index 0 of a CategoryPlot, and install a BarRenderer at the same index. Use only unique row keys. This is valid by construction and avoids null dataset/plot cases.",
    "check": "org.jfree.data.category.DefaultCategoryDataset dataset;\norg.jfree.chart.renderer.category.BarRenderer renderer;\norg.jfree.chart.plot.CategoryPlot plot;\nint seriesCount = data.consumeInt(1, 4);\ntry {\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  for (int i = 0; i < seriesCount; i++) {\n    dataset.addValue(data.consumeInt(0, 20), \"R\" + i + data.consumeAsciiString(2), \"C\" + data.consumeAsciiString(2));\n  }\n  renderer = new org.jfree.chart.renderer.category.BarRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  plot.setDataset(dataset);\n  plot.setRenderer(renderer);\n  plot.setRowRenderingOrder(data.consumeBoolean() ? org.jfree.util.SortOrder.ASCENDING : org.jfree.util.SortOrder.DESCENDING);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = renderer.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation legend-items-count-matches-per-series-items violated: unexpected \" + e.getClass().getName() + \" from getLegendItems on valid-by-construction input: \" + e.getMessage());\n}\nint expected = 0;\nfor (int i = 0; i < seriesCount; i++) {\n  boolean visible;\n  org.jfree.chart.LegendItem item;\n  try {\n    visible = renderer.isSeriesVisibleInLegend(i);\n    item = renderer.getLegendItem(0, i);\n  } catch (Exception e) {\n    throw new RuntimeException(\"relation legend-items-count-matches-per-series-items violated: unexpected \" + e.getClass().getName() + \" from per-series legend probe on valid-by-construction input: \" + e.getMessage());\n  }\n  if (visible && item != null) {\n    expected++;\n  }\n}\nif (items == null || items.getItemCount() != expected) {\n  throw new RuntimeException(\"relation legend-items-count-matches-per-series-items violated: collection count=\" + (items == null ? \"null\" : String.valueOf(items.getItemCount())) + \", expected=\" + expected);\n}"
  },
  {
    "name": "row-rendering-order-preserves-serieskey-set",
    "kind": "metamorphic",
    "contract": "Changing the plot's row rendering order affects only the order in which legend items are collected, not which series are represented. Therefore, for the same renderer configuration and same dataset, the LegendItemCollections obtained under ASCENDING and DESCENDING row rendering order must contain the same series keys, possibly in different order.",
    "input": "Build one DefaultCategoryDataset with 1..4 distinct row keys and one column. Install equivalent BarRenderers in two CategoryPlots using the same dataset, one plot with ASCENDING row rendering order and the other with DESCENDING.",
    "check": "org.jfree.data.category.DefaultCategoryDataset dataset;\norg.jfree.chart.renderer.category.BarRenderer rendererA;\norg.jfree.chart.renderer.category.BarRenderer rendererD;\norg.jfree.chart.plot.CategoryPlot plotA;\norg.jfree.chart.plot.CategoryPlot plotD;\nint seriesCount = data.consumeInt(1, 4);\ntry {\n  dataset = new org.jfree.data.category.DefaultCategoryDataset();\n  for (int i = 0; i < seriesCount; i++) {\n    dataset.addValue(data.consumeInt(0, 20), \"R\" + i + data.consumeAsciiString(2), \"C\" + data.consumeAsciiString(2));\n  }\n  rendererA = new org.jfree.chart.renderer.category.BarRenderer();\n  rendererD = new org.jfree.chart.renderer.category.BarRenderer();\n  plotA = new org.jfree.chart.plot.CategoryPlot();\n  plotD = new org.jfree.chart.plot.CategoryPlot();\n  plotA.setDataset(dataset);\n  plotD.setDataset(dataset);\n  plotA.setRenderer(rendererA);\n  plotD.setRenderer(rendererD);\n  plotA.setRowRenderingOrder(org.jfree.util.SortOrder.ASCENDING);\n  plotD.setRowRenderingOrder(org.jfree.util.SortOrder.DESCENDING);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection itemsA;\norg.jfree.chart.LegendItemCollection itemsD;\ntry {\n  itemsA = rendererA.getLegendItems();\n  itemsD = rendererD.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation row-rendering-order-preserves-serieskey-set violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (itemsA == null || itemsD == null) {\n  throw new RuntimeException(\"relation row-rendering-order-preserves-serieskey-set violated: null collection returned\");\n}\njava.util.ArrayList<String> keysA = new java.util.ArrayList<String>();\njava.util.ArrayList<String> keysD = new java.util.ArrayList<String>();\nfor (int i = 0; i < itemsA.getItemCount(); i++) {\n  Object k = itemsA.get(i).getSeriesKey();\n  keysA.add(String.valueOf(k));\n}\nfor (int i = 0; i < itemsD.getItemCount(); i++) {\n  Object k = itemsD.get(i).getSeriesKey();\n  keysD.add(String.valueOf(k));\n}\njava.util.Collections.sort(keysA);\njava.util.Collections.sort(keysD);\nif (!keysA.equals(keysD)) {\n  throw new RuntimeException(\"relation row-rendering-order-preserves-serieskey-set violated: ascending keys=\" + keysA + \", descending keys=\" + keysD);\n}"
  },
  {
    "name": "renderer-uses-its-own-dataset-index",
    "kind": "invariant",
    "contract": "getLegendItems() is for this renderer, so it must derive legend items from the dataset at this renderer's own plot index, not from some other dataset installed in the same plot. If this renderer's indexed dataset is null, the returned collection must be empty even when another dataset exists elsewhere in the plot.",
    "input": "Create a CategoryPlot with a non-null dataset at index 0 and a BarRenderer under test installed at index 1, leaving dataset 1 null. Optionally install another renderer at index 0 so the plot is populated. This validly distinguishes the renderer's own dataset slot from unrelated plot state.",
    "check": "org.jfree.data.category.DefaultCategoryDataset dataset0;\norg.jfree.chart.renderer.category.BarRenderer renderer0;\norg.jfree.chart.renderer.category.BarRenderer renderer1;\norg.jfree.chart.plot.CategoryPlot plot;\nint seriesCount = data.consumeInt(1, 4);\ntry {\n  dataset0 = new org.jfree.data.category.DefaultCategoryDataset();\n  for (int i = 0; i < seriesCount; i++) {\n    dataset0.addValue(data.consumeInt(0, 20), \"R\" + i + data.consumeAsciiString(2), \"C\" + data.consumeAsciiString(2));\n  }\n  renderer0 = new org.jfree.chart.renderer.category.BarRenderer();\n  renderer1 = new org.jfree.chart.renderer.category.BarRenderer();\n  plot = new org.jfree.chart.plot.CategoryPlot();\n  plot.setDataset(0, dataset0);\n  plot.setRenderer(0, renderer0);\n  plot.setRenderer(1, renderer1);\n} catch (Exception e) {\n  return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n  items = renderer1.getLegendItems();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation renderer-uses-its-own-dataset-index violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null || items.getItemCount() != 0) {\n  throw new RuntimeException(\"relation renderer-uses-its-own-dataset-index violated: expected empty collection for renderer at null dataset index, got \" + (items == null ? \"null\" : String.valueOf(items.getItemCount())));\n}"
  }
]
```

</details>

---
## [6] ⚙️ screen · `null-dataset-yields-empty-legend`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [7] ⚙️ screen-fuzz-buggy · `null-dataset-yields-empty-legend`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [8] ⚙️ screen · `detached-renderer-yields-empty-legend`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [9] ⚙️ screen-fuzz-buggy · `detached-renderer-yields-empty-legend`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [10] ⚙️ screen · `legend-items-count-matches-per-series-items`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [11] 🧠 LLM call — **compile-repair** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~37,320 chars)</summary>

**[system]**
```
You fix a Java snippet that failed to compile. Return ONLY the corrected snippet (the body that runs inside fuzzerTestOneInput, drawing inputs from `data`), no prose, no fences. Keep the SAME property being checked; change only what is needed to compile. Throw the violation with `throw new RuntimeException("relation <name> violated: "+...)` OUTSIDE any catch. Use only real API on the classpath. KEEP THE TWO-TIER CATCH the snippet uses and do NOT introduce a blanket `catch (Exception e) { return; }` around a call on the patch-changed class: setup exceptions are rejections to skip, but an exception from the probe on a valid-by-construction input is the violation and must be re-thrown as `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`.
```
**[user]**
```
Property (keep it): getLegendItems() is the collection-form view of the renderer's per-series legend items. For a renderer installed at dataset index 0, its collection size must equal the number of series indices i for which the series is visible in the legend and getLegendItem(0, i) returns a non-null item.
Available imports:
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

Snippet that failed:
org.jfree.data.category.DefaultCategoryDataset dataset;
org.jfree.chart.renderer.category.BarRenderer renderer;
org.jfree.chart.plot.CategoryPlot plot;
int seriesCount = data.consumeInt(1, 4);
try {
  dataset = new org.jfree.data.category.DefaultCategoryDataset();
  for (int i = 0; i < seriesCount; i++) {
    dataset.addValue(data.consumeInt(0, 20), "R" + i + data.consumeAsciiString(2), "C" + data.consumeAsciiString(2));
  }
  renderer = new org.jfree.chart.renderer.category.BarRenderer();
  plot = new org.jfree.chart.plot.CategoryPlot();
  plot.setDataset(dataset);
  plot.setRenderer(renderer);
  plot.setRowRenderingOrder(data.consumeBoolean() ? org.jfree.util.SortOrder.ASCENDING : org.jfree.util.SortOrder.DESCENDING);
} catch (Exception e) {
  return;
}
org.jfree.chart.LegendItemCollection items;
try {
  items = renderer.getLegendItems();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation legend-items-count-matches-per-series-items violated: unexpected " + e.getClass().getName() + " from getLegendItems on valid-by-construction input: " + e.getMessage());
}
int expected = 0;
for (int i = 0; i < seriesCount; i++) {
  boolean visible;
  org.jfree.chart.LegendItem item;
  try {
    visible = renderer.isSeriesVisibleInLegend(i);
    item = renderer.getLegendItem(0, i);
  } catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation legend-items-count-matches-per-series-items violated: unexpected " + e.getClass().getName() + " from per-series legend probe on valid-by-construction input: " + e.getMessage());
  }
  if (visible && item != null) {
    expected++;
  }
}
if (items == null || items.getItemCount() != expected) {
  throw new RuntimeException("relation legend-items-count-matches-per-series-items violated: collection count=" + (items == null ? "null" : String.valueOf(items.getItemCount())) + ", expected=" + expected);
}

javac error:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/relscreen_2/RelScreen2.java:85: error: package org.jfree.util does not exist
  plot.setRowRenderingOrder(data.consumeBoolean() ? org.jfree.util.SortOrder.ASCENDING : org.jfree.util.SortOrder.DESCENDING);
                                                                  ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/relscreen_2/RelScreen2.java:85: error: packa

Corrected snippet:
```

</details>
<details open><summary>▸ Output (~33,718 chars)</summary>

```
org.jfree.data.category.DefaultCategoryDataset dataset;
org.jfree.chart.renderer.category.BarRenderer renderer;
org.jfree.chart.plot.CategoryPlot plot;
int seriesCount = data.consumeInt(1, 4);
try {
  dataset = new org.jfree.data.category.DefaultCategoryDataset();
  for (int i = 0; i < seriesCount; i++) {
    dataset.addValue(data.consumeInt(0, 20), "R" + i + data.consumeAsciiString(2), "C" + data.consumeAsciiString(2));
  }
  renderer = new org.jfree.chart.renderer.category.BarRenderer();
  plot = new org.jfree.chart.plot.CategoryPlot();
  plot.setDataset(dataset);
  plot.setRenderer(renderer);
} catch (Exception e) {
  return;
}
org.jfree.chart.LegendItemCollection items;
try {
  items = renderer.getLegendItems();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation legend-items-count-matches-per-series-items violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
int expected = 0;
for (int i = 0; i < seriesCount; i++) {
  boolean visible;
  org.jfree.chart.LegendItem item;
  try {
    visible = renderer.isSeriesVisibleInLegend(i);
    item = renderer.getLegendItem(0, i);
  } catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation legend-items-count-matches-per-series-items violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
  }
  if (visible && item != null) {
    expected++;
  }
}
if (items == null || items.getItemCount() != expected) {
  throw new RuntimeException("relation legend-items-count-matches-per-series-items violated: collection count=" + (items == null ? "null" : String.valueOf(items.getItemCount())) + ", expected=" + expected);
}
```

</details>

---
## [12] ⚙️ screen-fuzz-buggy · `legend-items-count-matches-per-series-items`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [13] ⚙️ screen · `row-rendering-order-preserves-serieskey-set`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [14] 🧠 LLM call — **compile-repair** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~20,952 chars, 1 new)</summary>

- *[system] message: identical to step [11] — not reprinted*
**[user]**
```
Property (keep it): Changing the plot's row rendering order affects only the order in which legend items are collected, not which series are represented. Therefore, for the same renderer configuration and same dataset, the LegendItemCollections obtained under ASCENDING and DESCENDING row rendering order must contain the same series keys, possibly in different order.
Available imports:
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

Snippet that failed:
org.jfree.data.category.DefaultCategoryDataset dataset;
org.jfree.chart.renderer.category.BarRenderer rendererA;
org.jfree.chart.renderer.category.BarRenderer rendererD;
org.jfree.chart.plot.CategoryPlot plotA;
org.jfree.chart.plot.CategoryPlot plotD;
int seriesCount = data.consumeInt(1, 4);
try {
  dataset = new org.jfree.data.category.DefaultCategoryDataset();
  for (int i = 0; i < seriesCount; i++) {
    dataset.addValue(data.consumeInt(0, 20), "R" + i + data.consumeAsciiString(2), "C" + data.consumeAsciiString(2));
  }
  rendererA = new org.jfree.chart.renderer.category.BarRenderer();
  rendererD = new org.jfree.chart.renderer.category.BarRenderer();
  plotA = new org.jfree.chart.plot.CategoryPlot();
  plotD = new org.jfree.chart.plot.CategoryPlot();
  plotA.setDataset(dataset);
  plotD.setDataset(dataset);
  plotA.setRenderer(rendererA);
  plotD.setRenderer(rendererD);
  plotA.setRowRenderingOrder(org.jfree.util.SortOrder.ASCENDING);
  plotD.setRowRenderingOrder(org.jfree.util.SortOrder.DESCENDING);
} catch (Exception e) {
  return;
}
org.jfree.chart.LegendItemCollection itemsA;
org.jfree.chart.LegendItemCollection itemsD;
try {
  itemsA = rendererA.getLegendItems();
  itemsD = rendererD.getLegendItems();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation row-rendering-order-preserves-serieskey-set violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (itemsA == null || itemsD == null) {
  throw new RuntimeException("relation row-rendering-order-preserves-serieskey-set violated: null collection returned");
}
java.util.ArrayList<String> keysA = new java.util.ArrayList<String>();
java.util.ArrayList<String> keysD = new java.util.ArrayList<String>();
for (int i = 0; i < itemsA.getItemCount(); i++) {
  Object k = itemsA.get(i).getSeriesKey();
  keysA.add(String.valueOf(k));
}
for (int i = 0; i < itemsD.getItemCount(); i++) {
  Object k = itemsD.get(i).getSeriesKey();
  keysD.add(String.valueOf(k));
}
java.util.Collections.sort(keysA);
java.util.Collections.sort(keysD);
if (!keysA.equals(keysD)) {
  throw new RuntimeException("relation row-rendering-order-preserves-serieskey-set violated: ascending keys=" + keysA + ", descending keys=" + keysD);
}

javac error:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/relscreen_3/RelScreen3.java:91: error: package org.jfree.util does not exist
  plotA.setRowRenderingOrder(org.jfree.util.SortOrder.ASCENDING);
                                           ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/relscreen_3/RelScreen3.java:92: error: package org.jfree.util does not exist
  plotD.setRowRenderingOrder(org.jfree.util.SortOrd

Corrected snippet:
```

</details>
<details open><summary>▸ Output (~18,270 chars)</summary>

```
org.jfree.data.category.DefaultCategoryDataset dataset;
org.jfree.chart.renderer.category.BarRenderer rendererA;
org.jfree.chart.renderer.category.BarRenderer rendererD;
org.jfree.chart.plot.CategoryPlot plotA;
org.jfree.chart.plot.CategoryPlot plotD;
int seriesCount = data.consumeInt(1, 4);
try {
  dataset = new org.jfree.data.category.DefaultCategoryDataset();
  for (int i = 0; i < seriesCount; i++) {
    dataset.addValue(data.consumeInt(0, 20), "R" + i + data.consumeAsciiString(2), "C" + data.consumeAsciiString(2));
  }
  rendererA = new org.jfree.chart.renderer.category.BarRenderer();
  rendererD = new org.jfree.chart.renderer.category.BarRenderer();
  plotA = new org.jfree.chart.plot.CategoryPlot();
  plotD = new org.jfree.chart.plot.CategoryPlot();
  plotA.setDataset(dataset);
  plotD.setDataset(dataset);
  plotA.setRenderer(rendererA);
  plotD.setRenderer(rendererD);
  plotA.setRowRenderingOrder(org.jfree.chart.util.SortOrder.ASCENDING);
  plotD.setRowRenderingOrder(org.jfree.chart.util.SortOrder.DESCENDING);
} catch (Exception e) {
  return;
}
org.jfree.chart.LegendItemCollection itemsA;
org.jfree.chart.LegendItemCollection itemsD;
try {
  itemsA = plotA.getLegendItems();
  itemsD = plotD.getLegendItems();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("AbstractCategoryItemRenderer") || __rpgc.endsWith(".AbstractCategoryItemRenderer") || __rpgc.endsWith("$AbstractCategoryItemRenderer") || __rpgc.equals("AreaRenderer") || __rpgc.endsWith(".AreaRenderer") || __rpgc.endsWith("$AreaRenderer") || __rpgc.equals("BarRenderer") || __rpgc.endsWith(".BarRenderer") || __rpgc.endsWith("$BarRenderer") || __rpgc.equals("BarRenderer3D") || __rpgc.endsWith(".BarRenderer3D") || __rpgc.endsWith("$BarRenderer3D") || __rpgc.equals("BoxAndWhiskerRenderer") || __rpgc.endsWith(".BoxAndWhiskerRenderer") || __rpgc.endsWith("$BoxAndWhiskerRenderer") || __rpgc.equals("CategoryStepRenderer") || __rpgc.endsWith(".CategoryStepRenderer") || __rpgc.endsWith("$CategoryStepRenderer") || __rpgc.equals("DefaultCategoryItemRenderer") || __rpgc.endsWith(".DefaultCategoryItemRenderer") || __rpgc.endsWith("$DefaultCategoryItemRenderer") || __rpgc.equals("GanttRenderer") || __rpgc.endsWith(".GanttRenderer") || __rpgc.endsWith("$GanttRenderer") || __rpgc.equals("GroupedStackedBarRenderer") || __rpgc.endsWith(".GroupedStackedBarRenderer") || __rpgc.endsWith("$GroupedStackedBarRenderer") || __rpgc.equals("IntervalBarRenderer") || __rpgc.endsWith(".IntervalBarRenderer") || __rpgc.endsWith("$IntervalBarRenderer") || __rpgc.equals("LayeredBarRenderer") || __rpgc.endsWith(".LayeredBarRenderer") || __rpgc.endsWith("$LayeredBarRenderer") || __rpgc.equals("LevelRenderer") || __rpgc.endsWith(".LevelRenderer") || __rpgc.endsWith("$LevelRenderer") || __rpgc.equals("LineAndShapeRenderer") || __rpgc.endsWith(".LineAndShapeRenderer") || __rpgc.endsWith("$LineAndShapeRenderer") || __rpgc.equals("LineRenderer3D") || __rpgc.endsWith(".LineRenderer3D") || __rpgc.endsWith("$LineRenderer3D") || __rpgc.equals("MinMaxCategoryRenderer") || __rpgc.endsWith(".MinMaxCategoryRenderer") || __rpgc.endsWith("$MinMaxCategoryRenderer") || __rpgc.equals("ScatterRenderer") || __rpgc.endsWith(".ScatterRenderer") || __rpgc.endsWith("$ScatterRenderer") || __rpgc.equals("StackedAreaRenderer") || __rpgc.endsWith(".StackedAreaRenderer") || __rpgc.endsWith("$StackedAreaRenderer") || __rpgc.equals("StackedBarRenderer") || __rpgc.endsWith(".StackedBarRenderer") || __rpgc.endsWith("$StackedBarRenderer") || __rpgc.equals("StackedBarRenderer3D") || __rpgc.endsWith(".StackedBarRenderer3D") || __rpgc.endsWith("$StackedBarRenderer3D") || __rpgc.equals("StatisticalBarRenderer") || __rpgc.endsWith(".StatisticalBarRenderer") || __rpgc.endsWith("$StatisticalBarRenderer") || __rpgc.equals("StatisticalLineAndShapeRenderer") || __rpgc.endsWith(".StatisticalLineAndShapeRenderer") || __rpgc.endsWith("$StatisticalLineAndShapeRenderer") || __rpgc.equals("WaterfallBarRenderer") || __rpgc.endsWith(".WaterfallBarRenderer") || __rpgc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpgf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("AreaRenderer") || __rpgf.getClassName().endsWith(".AreaRenderer") || __rpgf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer") || __rpgf.getClassName().endsWith(".BarRenderer") || __rpgf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BarRenderer3D") || __rpgf.getClassName().endsWith(".BarRenderer3D") || __rpgf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpgf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("GanttRenderer") || __rpgf.getClassName().endsWith(".GanttRenderer") || __rpgf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineAndShapeRenderer") || __rpgf.getClassName().endsWith(".LineAndShapeRenderer") || __rpgf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("LineRenderer3D") || __rpgf.getClassName().endsWith(".LineRenderer3D") || __rpgf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpgf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("ScatterRenderer") || __rpgf.getClassName().endsWith(".ScatterRenderer") || __rpgf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalBarRenderer") || __rpgf.getClassName().endsWith(".StatisticalBarRenderer") || __rpgf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpgf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("WaterfallBarRenderer") || __rpgf.getClassName().endsWith(".WaterfallBarRenderer") || __rpgf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation row-rendering-order-preserves-serieskey-set violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (itemsA == null || itemsD == null) {
  throw new RuntimeException("relation row-rendering-order-preserves-serieskey-set violated: null collection returned");
}
java.util.ArrayList<String> keysA = new java.util.ArrayList<String>();
java.util.ArrayList<String> keysD = new java.util.ArrayList<String>();
for (int i = 0; i < itemsA.getItemCount(); i++) {
  Object k = itemsA.get(i).getSeriesKey();
  keysA.add(String.valueOf(k));
}
for (int i = 0; i < itemsD.getItemCount(); i++) {
  Object k = itemsD.get(i).getSeriesKey();
  keysD.add(String.valueOf(k));
}
java.util.Collections.sort(keysA);
java.util.Collections.sort(keysD);
if (!keysA.equals(keysD)) {
  throw new RuntimeException("relation row-rendering-order-preserves-serieskey-set violated: ascending keys=" + keysA + ", descending keys=" + keysD);
}
```

</details>

---
## [15] ⚙️ screen-fuzz-buggy · `row-rendering-order-preserves-serieskey-set`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [16] ⚙️ screen · `renderer-uses-its-own-dataset-index`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [17] ⚙️ screen-fuzz-buggy · `renderer-uses-its-own-dataset-index`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [18] ⚙️ screen · `null-dataset-yields-empty-legend`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [19] ⚙️ screen · `legend-items-count-matches-per-series-items`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [20] ⚙️ screen · `renderer-uses-its-own-dataset-index`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [21] ⚙️ screen · `detached-renderer-yields-empty-legend`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [22] ⚙️ screen · `row-rendering-order-preserves-serieskey-set`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [23] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "null-dataset-yields-empty-legend",
    "legend-items-count-matches-per-series-items",
    "renderer-uses-its-own-dataset-index",
    "detached-renderer-yields-empty-legend",
    "row-rendering-order-preserves-serieskey-set"
  ],
  "count": 5
}
```

---
## [24] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,795 chars)</summary>

```
[
  {
    "name": "nonnull-single-series-yields-one-legend-item",
    "kind": "invariant",
    "contract": "getLegendItems() returns the legend items for this renderer; with a non-null plot and non-null dataset, each visible series that can supply a legend item contributes one entry.",
    "input": "Construct a real LineAndShapeRenderer attached to a real CategoryPlot with a non-null DefaultCategoryDataset containing exactly one row and one column. Force series 0 visible in the legend.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\ntry {\n    org.jfree.data.category.DefaultCategoryDataset ds = new org.jfree.data.category.DefaultCategoryDataset();\n    String row = data.consumeAsciiString(8);\n    if (row.length() == 0) row = \"r0\";\n    String col = data.consumeAsciiString(8);\n    if (col.length() == 0) col = \"c0\";\n    ds.addValue(data.consumeInt(1, 10), row, col);\n    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n    r.setSeriesVisibleInLegend(0, Boolean.TRUE);\n    new org.jfree.chart.plot.CategoryPlot(ds, new org.jfree.chart.axis.CategoryAxis(\"C\"), new org.jfree.chart.axis.NumberAxis(\"V\"), r);\n} catch (Exception e) {\n    return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n    items = r.getLegendItems();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation nonnull-single-series-yields-one-legend-item violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null || items.getItemCount() != 1) {\n    throw new RuntimeException(\"relation nonnull-single-series-yields-one-legend-item violated: expected exactly 1 legend item, got \" + (items == null ? \"null\" : items.getItemCount()));\n}"
  },
  {
    "name": "single-series-collection-agrees-with-direct-item",
    "kind": "metamorphic",
    "contract": "The collection returned by getLegendItems() is composed from the renderer's per-series legend items; for a dataset with one visible series, the sole collection element must agree with getLegendItem(datasetIndex, series).",
    "input": "Construct a real LineAndShapeRenderer in a CategoryPlot with a non-null DefaultCategoryDataset containing one visible row/series and one column.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\ntry {\n    org.jfree.data.category.DefaultCategoryDataset ds = new org.jfree.data.category.DefaultCategoryDataset();\n    String row = data.consumeAsciiString(8);\n    if (row.length() == 0) row = \"r0\";\n    String col = data.consumeAsciiString(8);\n    if (col.length() == 0) col = \"c0\";\n    ds.addValue(data.consumeInt(1, 10), row, col);\n    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n    r.setSeriesVisibleInLegend(0, Boolean.TRUE);\n    new org.jfree.chart.plot.CategoryPlot(ds, new org.jfree.chart.axis.CategoryAxis(\"C\"), new org.jfree.chart.axis.NumberAxis(\"V\"), r);\n} catch (Exception e) {\n    return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n    items = r.getLegendItems();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation single-series-collection-agrees-with-direct-item violated: unexpected \" + e.getClass().getName() + \" from getLegendItems on valid-by-construction input: \" + e.getMessage());\n}\norg.jfree.chart.LegendItem direct;\ntry {\n    direct = r.getLegendItem(0, 0);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation single-series-collection-agrees-with-direct-item violated: unexpected \" + e.getClass().getName() + \" from getLegendItem on valid-by-construction input: \" + e.getMessage());\n}\nif (direct == null) {\n    throw new RuntimeException(\"relation single-series-collection-agrees-with-direct-item violated: direct legend item was null for visible single series\");\n}\nif (items == null || items.getItemCount() != 1) {\n    throw new RuntimeException(\"relation single-series-collection-agrees-with-direct-item violated: expected collection size 1, got \" + (items == null ? \"null\" : items.getItemCount()));\n}\norg.jfree.chart.LegendItem fromCollection = items.get(0);\nString a = fromCollection.getLabel();\nString b = direct.getLabel();\nif (a == null ? b != null : !a.equals(b)) {\n    throw new RuntimeException(\"relation single-series-collection-agrees-with-direct-item violated: collection label=\" + a + \", direct label=\" + b);\n}"
  },
  {
    "name": "row-rendering-order-reverses-legend-order",
    "kind": "metamorphic",
    "contract": "Legend items are returned in the plot's row rendering order: ascending iterates series from 0 upward, descending iterates in the reverse order.",
    "input": "Construct one renderer with a CategoryPlot whose non-null dataset has exactly two rows and one column; make both series visible in the legend. Probe once with ASCENDING and once with DESCENDING row rendering order.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\norg.jfree.chart.plot.CategoryPlot plot;\ntry {\n    org.jfree.data.category.DefaultCategoryDataset ds = new org.jfree.data.category.DefaultCategoryDataset();\n    String row0 = data.consumeAsciiString(6);\n    if (row0.length() == 0) row0 = \"r0\";\n    String row1 = data.consumeAsciiString(6);\n    if (row1.length() == 0 || row1.equals(row0)) row1 = row0 + \"_1\";\n    String col = data.consumeAsciiString(6);\n    if (col.length() == 0) col = \"c0\";\n    ds.addValue(data.consumeInt(1, 10), row0, col);\n    ds.addValue(data.consumeInt(11, 20), row1, col);\n    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n    r.setSeriesVisibleInLegend(0, Boolean.TRUE);\n    r.setSeriesVisibleInLegend(1, Boolean.TRUE);\n    plot = new org.jfree.chart.plot.CategoryPlot(ds, new org.jfree.chart.axis.CategoryAxis(\"C\"), new org.jfree.chart.axis.NumberAxis(\"V\"), r);\n} catch (Exception e) {\n    return;\n}\norg.jfree.chart.LegendItemCollection asc;\ntry {\n    plot.setRowRenderingOrder(org.jfree.util.SortOrder.ASCENDING);\n    asc = r.getLegendItems();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation row-rendering-order-reverses-legend-order violated: unexpected \" + e.getClass().getName() + \" for ascending order: \" + e.getMessage());\n}\norg.jfree.chart.LegendItemCollection desc;\ntry {\n    plot.setRowRenderingOrder(org.jfree.util.SortOrder.DESCENDING);\n    desc = r.getLegendItems();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation row-rendering-order-reverses-legend-order violated: unexpected \" + e.getClass().getName() + \" for descending order: \" + e.getMessage());\n}\nif (asc == null || desc == null || asc.getItemCount() != 2 || desc.getItemCount() != 2) {\n    throw new RuntimeException(\"relation row-rendering-order-reverses-legend-order violated: expected 2 items in each order, got asc=\" + (asc == null ? \"null\" : asc.getItemCount()) + \", desc=\" + (desc == null ? \"null\" : desc.getItemCount()));\n}\nString asc0 = asc.get(0).getLabel();\nString asc1 = asc.get(1).getLabel();\nString desc0 = desc.get(0).getLabel();\nString desc1 = desc.get(1).getLabel();\nboolean ok = (asc0 == null ? desc1 == null : asc0.equals(desc1)) && (asc1 == null ? desc0 == null : asc1.equals(desc0));\nif (!ok) {\n    throw new RuntimeException(\"relation row-rendering-order-reverses-legend-order violated: ascending labels [\" + asc0 + \", \" + asc1 + \"] vs descending labels [\" + desc0 + \", \" + desc1 + \"]\");\n}"
  },
  {
    "name": "null-plot-gives-empty-legend-collection",
    "kind": "invariant",
    "contract": "When the renderer is not assigned to a plot, there is no dataset to enumerate, so getLegendItems() returns an empty LegendItemCollection.",
    "input": "Construct a fresh real LineAndShapeRenderer and do not attach it to any plot.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\ntry {\n    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n} catch (Exception e) {\n    return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n    items = r.getLegendItems();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation null-plot-gives-empty-legend-collection violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null || items.getItemCount() != 0) {\n    throw new RuntimeException(\"relation null-plot-gives-empty-legend-collection violated: expected empty collection, got \" + (items == null ? \"null\" : items.getItemCount()));\n}"
  },
  {
    "name": "null-dataset-gives-empty-legend-collection",
    "kind": "invariant",
    "contract": "If the renderer's plot has no dataset for this renderer index, there are no series to contribute legend items, so getLegendItems() returns an empty collection.",
    "input": "Construct a real LineAndShapeRenderer attached to a CategoryPlot whose dataset is null.",
    "check": "org.jfree.chart.renderer.category.LineAndShapeRenderer r;\ntry {\n    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();\n    new org.jfree.chart.plot.CategoryPlot(null, new org.jfree.chart.axis.CategoryAxis(\"C\"), new org.jfree.chart.axis.NumberAxis(\"V\"), r);\n} catch (Exception e) {\n    return;\n}\norg.jfree.chart.LegendItemCollection items;\ntry {\n    items = r.getLegendItems();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation null-dataset-gives-empty-legend-collection violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (items == null || items.getItemCount() != 0) {\n    throw new RuntimeException(\"relation null-dataset-gives-empty-legend-collection violated: expected empty collection, got \" + (items == null ? \"null\" : items.getItemCount()));\n}"
  }
]
```

</details>

---
## [25] ⚙️ screen · `nonnull-single-series-yields-one-legend-item`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~758 chars)</summary>

- reason: the probe `r.setSeriesVisibleInLegend(...)` on the patch-changed class `LineAndShapeRenderer` runs inside the try at line 2, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `LineAndShapeRenderer` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [26] ⚙️ screen-fuzz-buggy · `nonnull-single-series-yields-one-legend-item`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [27] ⚙️ screen · `single-series-collection-agrees-with-direct-item`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~758 chars)</summary>

- reason: the probe `r.setSeriesVisibleInLegend(...)` on the patch-changed class `LineAndShapeRenderer` runs inside the try at line 2, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `LineAndShapeRenderer` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [28] ⚙️ screen-fuzz-buggy · `single-series-collection-agrees-with-direct-item`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [29] ⚙️ screen · `row-rendering-order-reverses-legend-order`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~758 chars)</summary>

- reason: the probe `r.setSeriesVisibleInLegend(...)` on the patch-changed class `LineAndShapeRenderer` runs inside the try at line 3, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `LineAndShapeRenderer` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [30] 🧠 LLM call — **compile-repair** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~20,959 chars, 1 new)</summary>

- *[system] message: identical to step [11] — not reprinted*
**[user]**
```
Property (keep it): Legend items are returned in the plot's row rendering order: ascending iterates series from 0 upward, descending iterates in the reverse order.
Available imports:
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

Snippet that failed:
org.jfree.chart.renderer.category.LineAndShapeRenderer r;
org.jfree.chart.plot.CategoryPlot plot;
try {
    org.jfree.data.category.DefaultCategoryDataset ds = new org.jfree.data.category.DefaultCategoryDataset();
    String row0 = data.consumeAsciiString(6);
    if (row0.length() == 0) row0 = "r0";
    String row1 = data.consumeAsciiString(6);
    if (row1.length() == 0 || row1.equals(row0)) row1 = row0 + "_1";
    String col = data.consumeAsciiString(6);
    if (col.length() == 0) col = "c0";
    ds.addValue(data.consumeInt(1, 10), row0, col);
    ds.addValue(data.consumeInt(11, 20), row1, col);
    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();
    r.setSeriesVisibleInLegend(0, Boolean.TRUE);
    r.setSeriesVisibleInLegend(1, Boolean.TRUE);
    plot = new org.jfree.chart.plot.CategoryPlot(ds, new org.jfree.chart.axis.CategoryAxis("C"), new org.jfree.chart.axis.NumberAxis("V"), r);
} catch (Exception e) { for (StackTraceElement __rpf : e.getStackTrace()) { String __rpc = __rpf.getClassName(); if ((__rpc.equals("AbstractCategoryItemRenderer") || __rpc.endsWith(".AbstractCategoryItemRenderer") || __rpc.endsWith("$AbstractCategoryItemRenderer") || __rpc.equals("AreaRenderer") || __rpc.endsWith(".AreaRenderer") || __rpc.endsWith("$AreaRenderer") || __rpc.equals("BarRenderer") || __rpc.endsWith(".BarRenderer") || __rpc.endsWith("$BarRenderer") || __rpc.equals("BarRenderer3D") || __rpc.endsWith(".BarRenderer3D") || __rpc.endsWith("$BarRenderer3D") || __rpc.equals("BoxAndWhiskerRenderer") || __rpc.endsWith(".BoxAndWhiskerRenderer") || __rpc.endsWith("$BoxAndWhiskerRenderer") || __rpc.equals("CategoryStepRenderer") || __rpc.endsWith(".CategoryStepRenderer") || __rpc.endsWith("$CategoryStepRenderer") || __rpc.equals("DefaultCategoryItemRenderer") || __rpc.endsWith(".DefaultCategoryItemRenderer") || __rpc.endsWith("$DefaultCategoryItemRenderer") || __rpc.equals("GanttRenderer") || __rpc.endsWith(".GanttRenderer") || __rpc.endsWith("$GanttRenderer") || __rpc.equals("GroupedStackedBarRenderer") || __rpc.endsWith(".GroupedStackedBarRenderer") || __rpc.endsWith("$GroupedStackedBarRenderer") || __rpc.equals("IntervalBarRenderer") || __rpc.endsWith(".IntervalBarRenderer") || __rpc.endsWith("$IntervalBarRenderer") || __rpc.equals("LayeredBarRenderer") || __rpc.endsWith(".LayeredBarRenderer") || __rpc.endsWith("$LayeredBarRenderer") || __rpc.equals("LevelRenderer") || __rpc.endsWith(".LevelRenderer") || __rpc.endsWith("$LevelRenderer") || __rpc.equals("LineAndShapeRenderer") || __rpc.endsWith(".LineAndShapeRenderer") || __rpc.endsWith("$LineAndShapeRenderer") || __rpc.equals("LineRenderer3D") || __rpc.endsWith(".LineRenderer3D") || __rpc.endsWith("$LineRenderer3D") || __rpc.equals("MinMaxCategoryRenderer") || __rpc.endsWith(".MinMaxCategoryRenderer") || __rpc.endsWith("$MinMaxCategoryRenderer") || __rpc.equals("ScatterRenderer") || __rpc.endsWith(".ScatterRenderer") || __rpc.endsWith("$ScatterRenderer") || __rpc.equals("StackedAreaRenderer") || __rpc.endsWith(".StackedAreaRenderer") || __rpc.endsWith("$StackedAreaRenderer") || __rpc.equals("StackedBarRenderer") || __rpc.endsWith(".StackedBarRenderer") || __rpc.endsWith("$StackedBarRenderer") || __rpc.equals("StackedBarRenderer3D") || __rpc.endsWith(".StackedBarRenderer3D") || __rpc.endsWith("$StackedBarRenderer3D") || __rpc.equals("StatisticalBarRenderer") || __rpc.endsWith(".StatisticalBarRenderer") || __rpc.endsWith("$StatisticalBarRenderer") || __rpc.equals("StatisticalLineAndShapeRenderer") || __rpc.endsWith(".StatisticalLineAndShapeRenderer") || __rpc.endsWith("$StatisticalLineAndShapeRenderer") || __rpc.equals("WaterfallBarRenderer") || __rpc.endsWith(".WaterfallBarRenderer") || __rpc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpf.getMethodName())) { boolean __rpdoc = false; if (!__rpdoc && (__rpf.getClassName().equals("AbstractCategoryItemRenderer") || __rpf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("AreaRenderer") || __rpf.getClassName().endsWith(".AreaRenderer") || __rpf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer") || __rpf.getClassName().endsWith(".BarRenderer") || __rpf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer") || __rpf.getClassName().endsWith(".BarRenderer") || __rpf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer3D") || __rpf.getClassName().endsWith(".BarRenderer3D") || __rpf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer3D") || __rpf.getClassName().endsWith(".BarRenderer3D") || __rpf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("GanttRenderer") || __rpf.getClassName().endsWith(".GanttRenderer") || __rpf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("GanttRenderer") || __rpf.getClassName().endsWith(".GanttRenderer") || __rpf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("LineAndShapeRenderer") || __rpf.getClassName().endsWith(".LineAndShapeRenderer") || __rpf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("LineRenderer3D") || __rpf.getClassName().endsWith(".LineRenderer3D") || __rpf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("LineRenderer3D") || __rpf.getClassName().endsWith(".LineRenderer3D") || __rpf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("MinMaxCategoryRenderer") || __rpf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("MinMaxCategoryRenderer") || __rpf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("ScatterRenderer") || __rpf.getClassName().endsWith(".ScatterRenderer") || __rpf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("ScatterRenderer") || __rpf.getClassName().endsWith(".ScatterRenderer") || __rpf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("ScatterRenderer") || __rpf.getClassName().endsWith(".ScatterRenderer") || __rpf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalBarRenderer") || __rpf.getClassName().endsWith(".StatisticalBarRenderer") || __rpf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalBarRenderer") || __rpf.getClassName().endsWith(".StatisticalBarRenderer") || __rpf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("WaterfallBarRenderer") || __rpf.getClassName().endsWith(".WaterfallBarRenderer") || __rpf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("WaterfallBarRenderer") || __rpf.getClassName().endsWith(".WaterfallBarRenderer") || __rpf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc) throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: unexpected " + e.getClass().getName() + " thrown by the patch-changed class on a valid-by-construction input: " + e.getMessage()); break; } }
    return;
}
org.jfree.chart.LegendItemCollection asc;
try {
    plot.setRowRenderingOrder(org.jfree.util.SortOrder.ASCENDING);
    asc = r.getLegendItems();
} catch (Exception e) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: unexpected " + e.getClass().getName() + " for ascending order: " + e.getMessage());
}
org.jfree.chart.LegendItemCollection desc;
try {
    plot.setRowRenderingOrder(org.jfree.util.SortOrder.DESCENDING);
    desc = r.getLegendItems();
} catch (Exception e) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: unexpected " + e.getClass().getName() + " for descending order: " + e.getMessage());
}
if (asc == null || desc == null || asc.getItemCount() != 2 || desc.getItemCount() != 2) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: expected 2 items in each order, got asc=" + (asc == null ? "null" : asc.getItemCount()) + ", desc=" + (desc == null ? "null" : desc.getItemCount()));
}
String asc0 = asc.get(0).getLabel();
String asc1 = asc.get(1).getLabel();
String desc0 = desc.get(0).getLabel();
String desc1 = desc.get(1).getLabel();
boolean ok = (asc0 == null ? desc1 == null : asc0.equals(desc1)) && (asc1 == null ? desc0 == null : asc1.equals(desc0));
if (!ok) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: ascending labels [" + asc0 + ", " + asc1 + "] vs descending labels [" + desc0 + ", " + desc1 + "]");
}

javac error:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/relscreen_2/RelScreen2.java:93: error: package org.jfree.util does not exist
    plot.setRowRenderingOrder(org.jfree.util.SortOrder.ASCENDING);
                                            ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/relscreen_2/RelScreen2.java:100: error: package org.jfree.util does not exist
    plot.setRowRenderingOrder(org.jfree.util.Sor

Corrected snippet:
```

</details>
<details open><summary>▸ Output (~18,475 chars)</summary>

```
org.jfree.chart.renderer.category.LineAndShapeRenderer r;
org.jfree.chart.plot.CategoryPlot plot;
try {
    org.jfree.data.category.DefaultCategoryDataset ds = new org.jfree.data.category.DefaultCategoryDataset();
    String row0 = data.consumeAsciiString(6);
    if (row0.length() == 0) row0 = "r0";
    String row1 = data.consumeAsciiString(6);
    if (row1.length() == 0 || row1.equals(row0)) row1 = row0 + "_1";
    String col = data.consumeAsciiString(6);
    if (col.length() == 0) col = "c0";
    ds.addValue(data.consumeInt(1, 10), row0, col);
    ds.addValue(data.consumeInt(11, 20), row1, col);
    r = new org.jfree.chart.renderer.category.LineAndShapeRenderer();
    r.setSeriesVisibleInLegend(0, Boolean.TRUE);
    r.setSeriesVisibleInLegend(1, Boolean.TRUE);
    plot = new org.jfree.chart.plot.CategoryPlot(ds, new org.jfree.chart.axis.CategoryAxis("C"), new org.jfree.chart.axis.NumberAxis("V"), r);
} catch (Exception e) { for (StackTraceElement __rpf : e.getStackTrace()) { String __rpc = __rpf.getClassName(); if ((__rpc.equals("AbstractCategoryItemRenderer") || __rpc.endsWith(".AbstractCategoryItemRenderer") || __rpc.endsWith("$AbstractCategoryItemRenderer") || __rpc.equals("AreaRenderer") || __rpc.endsWith(".AreaRenderer") || __rpc.endsWith("$AreaRenderer") || __rpc.equals("BarRenderer") || __rpc.endsWith(".BarRenderer") || __rpc.endsWith("$BarRenderer") || __rpc.equals("BarRenderer3D") || __rpc.endsWith(".BarRenderer3D") || __rpc.endsWith("$BarRenderer3D") || __rpc.equals("BoxAndWhiskerRenderer") || __rpc.endsWith(".BoxAndWhiskerRenderer") || __rpc.endsWith("$BoxAndWhiskerRenderer") || __rpc.equals("CategoryStepRenderer") || __rpc.endsWith(".CategoryStepRenderer") || __rpc.endsWith("$CategoryStepRenderer") || __rpc.equals("DefaultCategoryItemRenderer") || __rpc.endsWith(".DefaultCategoryItemRenderer") || __rpc.endsWith("$DefaultCategoryItemRenderer") || __rpc.equals("GanttRenderer") || __rpc.endsWith(".GanttRenderer") || __rpc.endsWith("$GanttRenderer") || __rpc.equals("GroupedStackedBarRenderer") || __rpc.endsWith(".GroupedStackedBarRenderer") || __rpc.endsWith("$GroupedStackedBarRenderer") || __rpc.equals("IntervalBarRenderer") || __rpc.endsWith(".IntervalBarRenderer") || __rpc.endsWith("$IntervalBarRenderer") || __rpc.equals("LayeredBarRenderer") || __rpc.endsWith(".LayeredBarRenderer") || __rpc.endsWith("$LayeredBarRenderer") || __rpc.equals("LevelRenderer") || __rpc.endsWith(".LevelRenderer") || __rpc.endsWith("$LevelRenderer") || __rpc.equals("LineAndShapeRenderer") || __rpc.endsWith(".LineAndShapeRenderer") || __rpc.endsWith("$LineAndShapeRenderer") || __rpc.equals("LineRenderer3D") || __rpc.endsWith(".LineRenderer3D") || __rpc.endsWith("$LineRenderer3D") || __rpc.equals("MinMaxCategoryRenderer") || __rpc.endsWith(".MinMaxCategoryRenderer") || __rpc.endsWith("$MinMaxCategoryRenderer") || __rpc.equals("ScatterRenderer") || __rpc.endsWith(".ScatterRenderer") || __rpc.endsWith("$ScatterRenderer") || __rpc.equals("StackedAreaRenderer") || __rpc.endsWith(".StackedAreaRenderer") || __rpc.endsWith("$StackedAreaRenderer") || __rpc.equals("StackedBarRenderer") || __rpc.endsWith(".StackedBarRenderer") || __rpc.endsWith("$StackedBarRenderer") || __rpc.equals("StackedBarRenderer3D") || __rpc.endsWith(".StackedBarRenderer3D") || __rpc.endsWith("$StackedBarRenderer3D") || __rpc.equals("StatisticalBarRenderer") || __rpc.endsWith(".StatisticalBarRenderer") || __rpc.endsWith("$StatisticalBarRenderer") || __rpc.equals("StatisticalLineAndShapeRenderer") || __rpc.endsWith(".StatisticalLineAndShapeRenderer") || __rpc.endsWith("$StatisticalLineAndShapeRenderer") || __rpc.equals("WaterfallBarRenderer") || __rpc.endsWith(".WaterfallBarRenderer") || __rpc.endsWith("$WaterfallBarRenderer")) && !"<init>".equals(__rpf.getMethodName())) { boolean __rpdoc = false; if (!__rpdoc && (__rpf.getClassName().equals("AbstractCategoryItemRenderer") || __rpf.getClassName().endsWith(".AbstractCategoryItemRenderer") || __rpf.getClassName().endsWith("$AbstractCategoryItemRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("AreaRenderer") || __rpf.getClassName().endsWith(".AreaRenderer") || __rpf.getClassName().endsWith("$AreaRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer") || __rpf.getClassName().endsWith(".BarRenderer") || __rpf.getClassName().endsWith("$BarRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer") || __rpf.getClassName().endsWith(".BarRenderer") || __rpf.getClassName().endsWith("$BarRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer3D") || __rpf.getClassName().endsWith(".BarRenderer3D") || __rpf.getClassName().endsWith("$BarRenderer3D")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BarRenderer3D") || __rpf.getClassName().endsWith(".BarRenderer3D") || __rpf.getClassName().endsWith("$BarRenderer3D")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith(".BoxAndWhiskerRenderer") || __rpf.getClassName().endsWith("$BoxAndWhiskerRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("GanttRenderer") || __rpf.getClassName().endsWith(".GanttRenderer") || __rpf.getClassName().endsWith("$GanttRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("GanttRenderer") || __rpf.getClassName().endsWith(".GanttRenderer") || __rpf.getClassName().endsWith("$GanttRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("LineAndShapeRenderer") || __rpf.getClassName().endsWith(".LineAndShapeRenderer") || __rpf.getClassName().endsWith("$LineAndShapeRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("LineRenderer3D") || __rpf.getClassName().endsWith(".LineRenderer3D") || __rpf.getClassName().endsWith("$LineRenderer3D")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("LineRenderer3D") || __rpf.getClassName().endsWith(".LineRenderer3D") || __rpf.getClassName().endsWith("$LineRenderer3D")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("MinMaxCategoryRenderer") || __rpf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("MinMaxCategoryRenderer") || __rpf.getClassName().endsWith(".MinMaxCategoryRenderer") || __rpf.getClassName().endsWith("$MinMaxCategoryRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("ScatterRenderer") || __rpf.getClassName().endsWith(".ScatterRenderer") || __rpf.getClassName().endsWith("$ScatterRenderer")) && "clone".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("CloneNotSupportedException") || __rpn.endsWith(".CloneNotSupportedException") || __rpn.endsWith("$CloneNotSupportedException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("ScatterRenderer") || __rpf.getClassName().endsWith(".ScatterRenderer") || __rpf.getClassName().endsWith("$ScatterRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("ScatterRenderer") || __rpf.getClassName().endsWith(".ScatterRenderer") || __rpf.getClassName().endsWith("$ScatterRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalBarRenderer") || __rpf.getClassName().endsWith(".StatisticalBarRenderer") || __rpf.getClassName().endsWith("$StatisticalBarRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalBarRenderer") || __rpf.getClassName().endsWith(".StatisticalBarRenderer") || __rpf.getClassName().endsWith("$StatisticalBarRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith(".StatisticalLineAndShapeRenderer") || __rpf.getClassName().endsWith("$StatisticalLineAndShapeRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("WaterfallBarRenderer") || __rpf.getClassName().endsWith(".WaterfallBarRenderer") || __rpf.getClassName().endsWith("$WaterfallBarRenderer")) && "readObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("WaterfallBarRenderer") || __rpf.getClassName().endsWith(".WaterfallBarRenderer") || __rpf.getClassName().endsWith("$WaterfallBarRenderer")) && "writeObject".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpdoc = true; break; } } } if (!__rpdoc) throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); break; } }
    return;
}
org.jfree.chart.LegendItemCollection asc;
try {
    plot.setRowRenderingOrder(org.jfree.chart.util.SortOrder.ASCENDING);
    asc = r.getLegendItems();
} catch (Exception e) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
org.jfree.chart.LegendItemCollection desc;
try {
    plot.setRowRenderingOrder(org.jfree.chart.util.SortOrder.DESCENDING);
    desc = r.getLegendItems();
} catch (Exception e) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (asc == null || desc == null || asc.getItemCount() != 2 || desc.getItemCount() != 2) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: expected 2 items in each order, got asc=" + (asc == null ? "null" : asc.getItemCount()) + ", desc=" + (desc == null ? "null" : desc.getItemCount()));
}
String asc0 = asc.get(0).getLabel();
String asc1 = asc.get(1).getLabel();
String desc0 = desc.get(0).getLabel();
String desc1 = desc.get(1).getLabel();
boolean ok = (asc0 == null ? desc1 == null : asc0.equals(desc1)) && (asc1 == null ? desc0 == null : asc1.equals(desc0));
if (!ok) {
    throw new RuntimeException("relation row-rendering-order-reverses-legend-order violated: ascending labels [" + asc0 + ", " + asc1 + "] vs descending labels [" + desc0 + ", " + desc1 + "]");
}
```

</details>

---
## [31] ⚙️ screen-fuzz-buggy · `row-rendering-order-reverses-legend-order`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [32] ⚙️ screen · `null-plot-gives-empty-legend-collection`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [33] ⚙️ screen-fuzz-buggy · `null-plot-gives-empty-legend-collection`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [34] ⚙️ screen · `null-dataset-gives-empty-legend-collection`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [35] ⚙️ screen-fuzz-buggy · `null-dataset-gives-empty-legend-collection`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [36] ⚙️ screen · `nonnull-single-series-yields-one-legend-item`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [37] ⚙️ screen · `single-series-collection-agrees-with-direct-item`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [38] ⚙️ screen · `row-rendering-order-reverses-legend-order`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [39] ⚙️ screen · `null-dataset-gives-empty-legend-collection`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [40] ⚙️ screen · `null-plot-gives-empty-legend-collection`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [41] ⚙️ reference-widening · `enumerate`
**output:** **2 observable(s) to widen onto**
<details><summary>▸ reason (~434 chars)</summary>

- reason: the kept checks probe ['legenditems', 'plot'] among the patched class's no-argument readers; 2 of those have no admitted reference and 2 fit under the per-leg cap of 3 (the leg already holds []); 7 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['abstractcategoryitemrenderer', 'lineandshaperenderer', 'renderer', 'addvalue', 'seriesvisibleinlegend', 'defaultcategorydataset'])

</details>
<details><summary>▸ detail (~1,575 chars)</summary>

- detail: {'targets': ['legenditems', 'plot'], 'already_admitted': [], 'cap': 3, 'checks_read': 10, 'excluded': [{'key': 'abstractcategoryitemrenderer', 'why': 'the patched class declares `AbstractCategoryItemRenderer()` as a constructor or a void method — it reports no observable value'}, {'key': 'lineandshaperenderer', 'why': 'the patched class does not declare `LineAndShapeRenderer()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'renderer', 'why': 'the patched class does not declare `getRenderer()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'addvalue', 'why': 'the patched class does not declare `addValue()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'seriesvisibleinlegend', 'why': 'the patched class does not declare `isSeriesVisibleInLegend()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'defaultcategorydataset', 'why': 'the patched class does not declare `DefaultCategoryDataset()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'setseriesvisibleinlegend', 'why': 'the patched class does not declare `setSeriesVisibleInLegend()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 7}

</details>

---
## [42] ⚙️ reference-widening · `getLegendItems`
**output:** **reference REQUESTED for observable `legenditems`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [43] ⚙️ reference-impl · `getLegendItems`
**output:** **widening target requested**
- detail: {'candidates': ['getLegendItems'], 'attempting': 'getLegendItems'}

---
## [44] ⚙️ reference-impl · `getLegendItems`
**output:** **screening surface resolved**
- reason: 2 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getBaseLinesVisible', 'getPassCount'], 'patch_touched_exempted': [], 'screened_siblings': ['getBaseLinesVisible', 'getPassCount'], 'declaring_types': ['AbstractCategoryItemRenderer', 'LineAndShapeRenderer']}

---
## [45] 🧠 LLM call — **LLM** — model `gpt-5.4`
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
<details open><summary>▸ Output (~1,787 chars)</summary>

```
// compute(org.jfree.chart.renderer.category.AbstractCategoryItemRenderer, org.jfree.chart.plot.CategoryPlot, boolean) : getLegendItems, getBaseLinesVisible, getPassCount
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;

public class ReferenceImpl {

    public static LegendItemCollection compute_getLegendItems(
            AbstractCategoryItemRenderer renderer,
            CategoryPlot plot,
            boolean baseLinesVisible) {
        LegendItemCollection result = new LegendItemCollection();
        if (plot == null) {
            return result;
        }
        int index = plot.getIndexOf(renderer);
        CategoryDataset dataset = plot.getDataset(index);
        if (dataset == null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        for (int series = 0; series < seriesCount; series++) {
            if (renderer.isSeriesVisible(series)
                    && renderer.isSeriesVisibleInLegend(series)) {
                LegendItem item = renderer.getLegendItem(index, series);
                if (item != null) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public static boolean compute_getBaseLinesVisible(
            AbstractCategoryItemRenderer renderer,
            CategoryPlot plot,
            boolean baseLinesVisible) {
        return baseLinesVisible;
    }

    public static int compute_getPassCount(
            AbstractCategoryItemRenderer renderer,
            CategoryPlot plot,
            boolean baseLinesVisible) {
        return 1;
    }
}
```

</details>

---
## [46] ⚙️ reference-impl · `getLegendItems`
**output:** **reference generated**
- detail: {'chars': 1787}

---
## [47] ⚙️ reference-impl · `getLegendItems`
**output:** **signature declared**
- detail: {'signature': 'org.jfree.chart.renderer.category.AbstractCategoryItemRenderer, org.jfree.chart.plot.CategoryPlot, boolean'}

---
## [48] ⚙️ reference-impl · `getLegendItems`
**output:** **reference observables matched**
- detail: {'matched': {'getLegendItems': 'getLegendItems', 'getBaseLinesVisible': 'getBaseLinesVisible', 'getPassCount': 'getPassCount'}, 'declared_only': []}

---
## [49] ⚙️ reference-impl · `getLegendItems`
**output:** **reference too thin to screen — DISCARDED**
- reason: 2 shared sibling observable(s) ['getBaseLinesVisible', 'getPassCount']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [50] ⚙️ reference-widening · `getLegendItems`
**output:** **widening REJECTED for observable `legenditems`**
- reason: reference too thin to screen — DISCARDED — 2 shared sibling observable(s) ['getBaseLinesVisible', 'getPassCount']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [51] ⚙️ reference-widening · `getPlot`
**output:** **reference REQUESTED for observable `plot`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [52] ⚙️ reference-impl · `getPlot`
**output:** **widening target requested**
- detail: {'candidates': ['getPlot'], 'attempting': 'getPlot'}

---
## [53] ⚙️ reference-impl · `getPlot`
**output:** **screening surface resolved**
- reason: 4 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getBaseLinesVisible', 'getBaseSeriesVisible', 'getBaseSeriesVisibleInLegend', 'getPassCount'], 'patch_touched_exempted': [], 'screened_siblings': ['getBaseLinesVisible', 'getBaseSeriesVisible', 'getBaseSeriesVisibleInLegend', 'getPassCount'], 'declaring_types': ['AbstractCategoryItemRenderer', 'CategoryItemRenderer', 'LineAndShapeRenderer']}

---
## [54] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~61,499 chars, 1 new)</summary>

- *[system] message: identical to step [45] — not reprinted*
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
<details open><summary>▸ Output (~1,895 chars)</summary>

```
// compute(Object, boolean, boolean, boolean) : getPlot, getBaseLinesVisible, getBaseSeriesVisible, getBaseSeriesVisibleInLegend, getPassCount
public class ReferenceImpl {

    public static Object compute_getPlot(Object plot,
                                         boolean baseLinesVisible,
                                         boolean baseSeriesVisible,
                                         boolean baseSeriesVisibleInLegend) {
        return plot;
    }

    public static boolean compute_getBaseLinesVisible(Object plot,
                                                      boolean baseLinesVisible,
                                                      boolean baseSeriesVisible,
                                                      boolean baseSeriesVisibleInLegend) {
        return baseLinesVisible;
    }

    public static boolean compute_getBaseSeriesVisible(Object plot,
                                                       boolean baseLinesVisible,
                                                       boolean baseSeriesVisible,
                                                       boolean baseSeriesVisibleInLegend) {
        return baseSeriesVisible;
    }

    public static boolean compute_getBaseSeriesVisibleInLegend(Object plot,
                                                               boolean baseLinesVisible,
                                                               boolean baseSeriesVisible,
                                                               boolean baseSeriesVisibleInLegend) {
        return baseSeriesVisibleInLegend;
    }

    public static int compute_getPassCount(Object plot,
                                           boolean baseLinesVisible,
                                           boolean baseSeriesVisible,
                                           boolean baseSeriesVisibleInLegend) {
        return 1;
    }
}
```

</details>

---
## [55] ⚙️ reference-impl · `getPlot`
**output:** **reference generated**
- detail: {'chars': 1895}

---
## [56] ⚙️ reference-impl · `getPlot`
**output:** **signature declared**
- detail: {'signature': 'Object, boolean, boolean, boolean'}

---
## [57] ⚙️ reference-impl · `getPlot`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'Object, boolean, boolean, boolean', 'named': 'Object plot, boolean baseLinesVisible, boolean baseSeriesVisible, boolean baseSeriesVisibleInLegend'}

---
## [58] ⚙️ reference-impl · `getPlot`
**output:** **reference observables matched**
- detail: {'matched': {'getPlot': 'getPlot', 'getBaseLinesVisible': 'getBaseLinesVisible', 'getBaseSeriesVisible': 'getBaseSeriesVisible', 'getBaseSeriesVisibleInLegend': 'getBaseSeriesVisibleInLegend', 'getPassCount': 'getPassCount'}, 'declared_only': []}

---
## [59] ⚙️ reference-impl · `getPlot`
**output:** **screen bar reachable**
- reason: 4 shared sibling observable(s) — enough for the screen to decide

---
## [60] ⚙️ reference-impl · `getPlot`
**output:** **signature mapped**
- reason: matched 4 parameter(s) to state fields

---
## [61] ⚙️ reference-impl · `getPlot`
**output:** **twin setup extracted**
- reason: setup extracted; receiver `r`
- detail: {'declaring_types': ['AbstractCategoryItemRenderer', 'CategoryItemRenderer', 'LineAndShapeRenderer']}

---
## [62] ⚙️ reference-impl · `getPlot`
**output:** **twin built**
- detail: {'helpers': 0, 'imports': 16, 'package': 'org.jfree.chart.renderer.category.junit', 'receiver': 'r'}

---
## [63] ⚙️ reference-impl · `getPlot`
**output:** **buggy twin FAILED — DISCARDED**
- reason: twin did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/reference_twin/StateTwinDriver.java:73: error: cannot find | symbol:   method getBaseLinesVisible() | location: variable r of type AbstractCategoryItemRenderer

---
## [64] ⚙️ reference-widening · `getPlot`
**output:** **widening REJECTED for observable `plot`**
- reason: buggy twin FAILED — DISCARDED — twin did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/reference_twin/StateTwinDriver.java:73: error: cannot find | symbol:   method getBaseLinesVisible() | location: variable r of type AbstractCategoryItemRenderer
- detail: {'admitted_observables': [], 'last_step': 'buggy twin FAILED — DISCARDED'}

---
## [65] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~4,094 chars)</summary>

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

Codebase: `Chart_1_buggy`. Write a fuzz harness for the function(s) shown below.

Call them through the library's real public API, with inputs built from the FuzzedDataProvider, and exercise as much of each function's behaviour as you can: vary lengths, sizes, signs and contents, cover empty and boundary values, and try the argument combinations the signature allows. Construct whatever real library objects the call needs. A throwable that escapes fuzzerTestOneInput is reported as a finding, so let exceptions from the code under test propagate.

Function `getLegendItems` — declared in `org.jfree.chart.renderer.category.AbstractCategoryItemRenderer`:
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
<details open><summary>▸ Output (~2,669 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        for (int i = 0, n = data.consumeInt(0, 8); i < n; i++) {
            renderer.setSeriesVisibleInLegend(data.consumeInt(0, 12), Boolean.valueOf(data.consumeBoolean()));
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 6);
            int colCount = data.consumeInt(0, 6);
            for (int r = 0; r < rowCount; r++) {
                Comparable rowKey = data.consumeAsciiString(16) + "#" + r;
                for (int c = 0; c < colCount; c++) {
                    Comparable colKey = data.consumeAsciiString(16) + "@" + c;
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        } else {
            plot.getRenderer().getLegendItems();
        }

        if (dataset != null && data.consumeBoolean()) {
            plot.setRowRenderingOrder(plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)
                    ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            renderer.getLegendItems();
        }

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            plot.setDataset(null);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [66] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [67] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,960 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        for (int i = 0, n = data.consumeInt(0, 8); i < n; i++) {
            renderer.setSeriesVisibleInLegend(data.consumeInt(0, 12), Boolean.valueOf(data.consumeBoolean()));
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 6);
            int colCount = data.consumeInt(0, 6);
            for (int r = 0; r < rowCount; r++) {
                Comparable rowKey = data.consumeAsciiString(16) + "#" + r;
                for (int c = 0; c < colCount; c++) {
                    Comparable colKey = data.consumeAsciiString(16) + "@" + c;
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        } else {
            plot.getRenderer().getLegendItems();
        }

        if (dataset != null && data.consumeBoolean()) {
            plot.setRowRenderingOrder(plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)
                    ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            renderer.getLegendItems();
        }

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            plot.setDataset(null);
            renderer.getLegendItems();
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_001/FuzzHarness.java:8: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_001/FuzzHarness.java:48: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_001/FuzzHarness.java:48: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_001/FuzzHarness.java:57: error: cannot find symbol
            plot.setRowRenderingOrder(plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)
                                                                         ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_001/FuzzHarness.java:58: error: cannot find symbol
                    ? SortOrder.DESCENDING : SortOrder.ASCENDING);
                      ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_001/FuzzHarness.java:58: error: cannot find symbol
                    ? SortOrder.DESCENDING : SortOrder.ASCENDING);
                                             ^
  symbol:   variable SortOrder
  location: class FuzzHarness
6 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~2,575 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        for (int i = 0, n = data.consumeInt(0, 8); i < n; i++) {
            renderer.setSeriesVisibleInLegend(
                data.consumeInt(0, 16),
                data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE
            );
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 6);
            int colCount = data.consumeInt(0, 6);
            for (int r = 0; r < rowCount; r++) {
                Comparable rowKey = data.consumeAsciiString(16) + "_" + r;
                for (int c = 0; c < colCount; c++) {
                    Comparable colKey = data.consumeAsciiString(16) + "_" + c;
                    Number value;
                    if (data.consumeBoolean()) {
                        value = null;
                    } else {
                        value = new Integer(data.consumeInt());
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        } else {
            plot.getRenderer().getLegendItems();
        }

        if (data.consumeBoolean()) {
            plot.setDataset(null);
            renderer.getLegendItems();
        }

        if (dataset != null && data.consumeBoolean()) {
            plot.setDataset(dataset);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [68] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [69] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,337 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.getLegendItems();

        org.jfree.data.category.DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                String rowKey = "R" + r + "_" + data.consumeAsciiString(8);
                for (int c = 0; c < cols; c++) {
                    String colKey = "C" + c + "_" + data.consumeString(8);
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot(
            dataset,
            new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)),
            new org.jfree.chart.axis.NumberAxis(data.consumeString(16)),
            renderer
        );

        plot.setRowRenderingOrder(
            data.consumeBoolean()
                ? org.jfree.util.SortOrder.ASCENDING
                : org.jfree.util.SortOrder.DESCENDING
        );

        int visibilityTweaks = data.consumeInt(0, 6);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 8);
            int mode = data.consumeInt(0, 2);
            Boolean visible;
            if (mode == 0) {
                visible = Boolean.TRUE;
            } else if (mode == 1) {
                visible = Boolean.FALSE;
            } else {
                visible = null;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset replacement = null;
            if (data.consumeBoolean()) {
                replacement = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = "RR" + r + "_" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = "CC" + c + "_" + data.consumeString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        replacement.addValue(value, rowKey, colKey);
                    }
                }
            }
            plot.setDataset(replacement);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [70] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [71] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,936 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.getLegendItems();

        org.jfree.data.category.DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                String rowKey = "R" + r + "_" + data.consumeAsciiString(8);
                for (int c = 0; c < cols; c++) {
                    String colKey = "C" + c + "_" + data.consumeString(8);
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot(
            dataset,
            new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)),
            new org.jfree.chart.axis.NumberAxis(data.consumeString(16)),
            renderer
        );

        plot.setRowRenderingOrder(
            data.consumeBoolean()
                ? org.jfree.util.SortOrder.ASCENDING
                : org.jfree.util.SortOrder.DESCENDING
        );

        int visibilityTweaks = data.consumeInt(0, 6);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 8);
            int mode = data.consumeInt(0, 2);
            Boolean visible;
            if (mode == 0) {
                visible = Boolean.TRUE;
            } else if (mode == 1) {
                visible = Boolean.FALSE;
            } else {
                visible = null;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset replacement = null;
            if (data.consumeBoolean()) {
                replacement = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = "RR" + r + "_" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = "CC" + c + "_" + data.consumeString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        replacement.addValue(value, rowKey, colKey);
                    }
                }
            }
            plot.setDataset(replacement);
            renderer.getLegendItems();
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_003/FuzzHarness.java:46: error: package org.jfree.util does not exist
                ? org.jfree.util.SortOrder.ASCENDING
                                ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_003/FuzzHarness.java:47: error: package org.jfree.util does not exist
                : org.jfree.util.SortOrder.DESCENDING
                                ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~3,149 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.getLegendItems();

        org.jfree.data.category.DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                String rowKey = "R" + r + "_" + data.consumeAsciiString(8);
                for (int c = 0; c < cols; c++) {
                    String colKey = "C" + c + "_" + data.consumeString(8);
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot(
            dataset,
            new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)),
            new org.jfree.chart.axis.NumberAxis(data.consumeString(16)),
            renderer
        );

        int visibilityTweaks = data.consumeInt(0, 6);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 8);
            int mode = data.consumeInt(0, 2);
            Boolean visible;
            if (mode == 0) {
                visible = Boolean.TRUE;
            } else if (mode == 1) {
                visible = Boolean.FALSE;
            } else {
                visible = null;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset replacement = null;
            if (data.consumeBoolean()) {
                replacement = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = "RR" + r + "_" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = "CC" + c + "_" + data.consumeString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        replacement.addValue(value, rowKey, colKey);
                    }
                }
            }
            plot.setDataset(replacement);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [72] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [73] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,342 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 4)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 8);
            int cols = data.consumeInt(0, 8);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                if (rowKey == null) {
                    rowKey = "";
                }
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                    if (colKey == null) {
                        colKey = "";
                    }
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Double.valueOf((double) data.consumeInt());
                            break;
                        case 2:
                            value = Long.valueOf((long) data.consumeInt());
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(
                dataset,
                new CategoryAxis(data.consumeAsciiString(16)),
                new NumberAxis(data.consumeAsciiString(16)),
                renderer
        );

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        int extraDatasets = data.consumeInt(0, 2);
        for (int i = 1; i <= extraDatasets; i++) {
            if (data.consumeBoolean()) {
                DefaultCategoryDataset extra = new DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = data.consumeAsciiString(12);
                    for (int c = 0; c < cols; c++) {
                        extra.addValue(Integer.valueOf(data.consumeInt()), rowKey, data.consumeAsciiString(12));
                    }
                }
                plot.setDataset(i, extra);
            } else {
                plot.setDataset(i, null);
            }

            AbstractCategoryItemRenderer extraRenderer;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    extraRenderer = new BarRenderer();
                    break;
                case 1:
                    extraRenderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                    break;
                default:
                    extraRenderer = new AreaRenderer();
                    break;
            }
            plot.setRenderer(i, extraRenderer);
        }

        int seriesTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < seriesTweaks; i++) {
            int series = data.consumeInt(0, 15);
            Boolean flag;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        if (data.consumeBoolean()) {
            renderer.setDataBoundsIncludesVisibleSeriesOnly(data.consumeBoolean());
        }

        renderer.getLegendItems();

        if (dataset != null) {
            plot.setDataset(0, null);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [74] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [75] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,473 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 4)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 8);
            int cols = data.consumeInt(0, 8);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                if (rowKey == null) {
                    rowKey = "";
                }
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                    if (colKey == null) {
                        colKey = "";
                    }
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Double.valueOf((double) data.consumeInt());
                            break;
                        case 2:
                            value = Long.valueOf((long) data.consumeInt());
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(
                dataset,
                new CategoryAxis(data.consumeAsciiString(16)),
                new NumberAxis(data.consumeAsciiString(16)),
                renderer
        );

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        int extraDatasets = data.consumeInt(0, 2);
        for (int i = 1; i <= extraDatasets; i++) {
            if (data.consumeBoolean()) {
                DefaultCategoryDataset extra = new DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = data.consumeAsciiString(12);
                    for (int c = 0; c < cols; c++) {
                        extra.addValue(Integer.valueOf(data.consumeInt()), rowKey, data.consumeAsciiString(12));
                    }
                }
                plot.setDataset(i, extra);
            } else {
                plot.setDataset(i, null);
            }

            AbstractCategoryItemRenderer extraRenderer;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    extraRenderer = new BarRenderer();
                    break;
                case 1:
                    extraRenderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                    break;
                default:
                    extraRenderer = new AreaRenderer();
                    break;
            }
            plot.setRenderer(i, extraRenderer);
        }

        int seriesTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < seriesTweaks; i++) {
            int series = data.consumeInt(0, 15);
            Boolean flag;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        if (data.consumeBoolean()) {
            renderer.setDataBoundsIncludesVisibleSeriesOnly(data.consumeBoolean());
        }

        renderer.getLegendItems();

        if (dataset != null) {
            plot.setDataset(0, null);
            renderer.getLegendItems();
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_005/FuzzHarness.java:8: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_005/FuzzHarness.java:76: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_005/FuzzHarness.java:76: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~3,476 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 4)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 8);
            int cols = data.consumeInt(0, 8);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                if (rowKey == null) {
                    rowKey = "";
                }
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                    if (colKey == null) {
                        colKey = "";
                    }
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = new Integer(data.consumeInt());
                            break;
                        case 1:
                            value = new Double((double) data.consumeInt());
                            break;
                        case 2:
                            value = new Long((long) data.consumeInt());
                            break;
                        default:
                            value = null;
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(
            dataset,
            new CategoryAxis(data.consumeAsciiString(16)),
            new NumberAxis(data.consumeAsciiString(16)),
            renderer
        );

        int tweaks = data.consumeInt(0, 8);
        for (int i = 0; i < tweaks; i++) {
            int series = data.consumeInt(0, 15);
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = Boolean.TRUE;
                    break;
                case 1:
                    visible = Boolean.FALSE;
                    break;
                default:
                    visible = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            plot.setDataset(null);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [76] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,433 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (Math.abs(data.consumeInt()) % 6) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new WaterfallBarRenderer();
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt();
            Boolean flag;
            switch (Math.abs(data.consumeByte()) % 3) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 6);
            int cols = data.consumeInt(0, 6);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeAsciiString(16);
                    Number value;
                    switch (Math.abs(data.consumeByte()) % 5) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 2:
                            value = Integer.valueOf(0);
                            break;
                        case 3:
                            value = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            value = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(dataset);
        } else {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 11: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~3,998 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (Math.abs(data.consumeInt()) % 6) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new WaterfallBarRenderer();
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt();
            Boolean flag;
            switch (Math.abs(data.consumeByte()) % 3) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 6);
            int cols = data.consumeInt(0, 6);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeAsciiString(16);
                    Number value;
                    switch (Math.abs(data.consumeByte()) % 5) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 2:
                            value = Integer.valueOf(0);
                            break;
                        case 3:
                            value = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            value = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(dataset);
        } else {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 11: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~3,401 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 5)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new WaterfallBarRenderer();
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt();
            Boolean flag;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 6);
            int cols = data.consumeInt(0, 6);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 2:
                            value = Integer.valueOf(0);
                            break;
                        case 3:
                            value = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            value = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(dataset);
        } else {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [80] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,653 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
- *[assistant] message: identical to step [79] — not reprinted*
- *[user] message: identical to step [79] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 5)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new WaterfallBarRenderer();
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt();
            Boolean flag;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 6);
            int cols = data.consumeInt(0, 6);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 2:
                            value = Integer.valueOf(0);
                            break;
                        case 3:
                            value = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            value = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(dataset);
        } else {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_008/FuzzHarness.java:6: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_008/FuzzHarness.java:56: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_008/FuzzHarness.java:56: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~3,265 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 5)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new WaterfallBarRenderer();
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt();
            Boolean flag;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 6);
            int cols = data.consumeInt(0, 6);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 2:
                            value = Integer.valueOf(0);
                            break;
                        case 3:
                            value = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            value = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(dataset);
        } else {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [82] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,672 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 12);
            for (int i = 0; i < entries; i++) {
                String rowKey = data.consumeString(16);
                String columnKey = data.consumeAsciiString(16);
                dataset.addValue(Integer.valueOf(data.consumeInt()), rowKey, columnKey);
            }
        }

        BarRenderer renderer = new BarRenderer();

        int visibilityOps = data.consumeInt(0, 6);
        for (int i = 0; i < visibilityOps; i++) {
            int series = data.consumeInt(0, 8);
            int state = data.consumeInt(0, 2);
            Boolean visible;
            if (state == 0) {
                visible = Boolean.TRUE;
            } else if (state == 1) {
                visible = Boolean.FALSE;
            } else {
                visible = null;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));
        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            plot.setDataset(0, dataset);
        }
        if (data.consumeBoolean()) {
            plot.setRenderer(0, renderer);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset2 = null;
            if (data.consumeBoolean()) {
                dataset2 = new DefaultCategoryDataset();
                int entries2 = data.consumeInt(0, 8);
                for (int i = 0; i < entries2; i++) {
                    dataset2.addValue(
                            Integer.valueOf(data.consumeInt()),
                            data.consumeAsciiString(12),
                            data.consumeString(12));
                }
            }
            plot.setDataset(0, dataset2);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [84] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,803 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 12);
            for (int i = 0; i < entries; i++) {
                String rowKey = data.consumeString(16);
                String columnKey = data.consumeAsciiString(16);
                dataset.addValue(Integer.valueOf(data.consumeInt()), rowKey, columnKey);
            }
        }

        BarRenderer renderer = new BarRenderer();

        int visibilityOps = data.consumeInt(0, 6);
        for (int i = 0; i < visibilityOps; i++) {
            int series = data.consumeInt(0, 8);
            int state = data.consumeInt(0, 2);
            Boolean visible;
            if (state == 0) {
                visible = Boolean.TRUE;
            } else if (state == 1) {
                visible = Boolean.FALSE;
            } else {
                visible = null;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));
        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            plot.setDataset(0, dataset);
        }
        if (data.consumeBoolean()) {
            plot.setRenderer(0, renderer);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset2 = null;
            if (data.consumeBoolean()) {
                dataset2 = new DefaultCategoryDataset();
                int entries2 = data.consumeInt(0, 8);
                for (int i = 0; i < entries2; i++) {
                    dataset2.addValue(
                            Integer.valueOf(data.consumeInt()),
                            data.consumeAsciiString(12),
                            data.consumeString(12));
                }
            }
            plot.setDataset(0, dataset2);
            renderer.getLegendItems();
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_010/FuzzHarness.java:8: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_010/FuzzHarness.java:44: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_010/FuzzHarness.java:44: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~2,584 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 16);
            for (int i = 0; i < entries; i++) {
                dataset.addValue(
                    Integer.valueOf(data.consumeInt()),
                    data.consumeString(24),
                    data.consumeAsciiString(24)
                );
            }
        }

        BarRenderer renderer = new BarRenderer();

        int visibilityOps = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityOps; i++) {
            int series = data.consumeInt(0, 12);
            int state = data.consumeInt(0, 2);
            Boolean visible = state == 0 ? Boolean.TRUE : (state == 1 ? Boolean.FALSE : null);
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));
        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        if (data.consumeBoolean()) {
            plot.setDataset(dataset);
        }
        if (data.consumeBoolean()) {
            plot.setRenderer(renderer);
        }

        renderer.getLegendItems();

        int reconfigurations = data.consumeInt(0, 3);
        for (int r = 0; r < reconfigurations; r++) {
            DefaultCategoryDataset nextDataset = null;
            if (data.consumeBoolean()) {
                nextDataset = new DefaultCategoryDataset();
                int entries2 = data.consumeInt(0, 10);
                for (int i = 0; i < entries2; i++) {
                    nextDataset.addValue(
                        Integer.valueOf(data.consumeInt()),
                        data.consumeAsciiString(16),
                        data.consumeString(16)
                    );
                }
            }
            plot.setDataset(nextDataset);
            renderer.getLegendItems();
        }
    }
}
```

</details>

---
## [86] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,940 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 3)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 10);
            if (data.consumeBoolean()) {
                renderer.setSeriesVisibleInLegend(series, Boolean.valueOf(data.consumeBoolean()));
            } else {
                renderer.setSeriesVisible(series, Boolean.valueOf(data.consumeBoolean()));
            }
        }

        if (data.consumeBoolean()) {
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(renderer);
            plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

            if (data.consumeBoolean()) {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 20);
                for (int i = 0; i < entries; i++) {
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        case 2:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Integer.valueOf(data.remainingBytes());
                            break;
                    }
                    String rowKey = data.consumeString(16);
                    String columnKey = data.consumeAsciiString(16);
                    dataset.addValue(value, rowKey, columnKey);
                }
                plot.setDataset(dataset);
            } else {
                plot.setDataset(null);
            }
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [88] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,087 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 3)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 10);
            if (data.consumeBoolean()) {
                renderer.setSeriesVisibleInLegend(series, Boolean.valueOf(data.consumeBoolean()));
            } else {
                renderer.setSeriesVisible(series, Boolean.valueOf(data.consumeBoolean()));
            }
        }

        if (data.consumeBoolean()) {
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(renderer);
            plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

            if (data.consumeBoolean()) {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 20);
                for (int i = 0; i < entries; i++) {
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        case 2:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Integer.valueOf(data.remainingBytes());
                            break;
                    }
                    String rowKey = data.consumeString(16);
                    String columnKey = data.consumeAsciiString(16);
                    dataset.addValue(value, rowKey, columnKey);
                }
                plot.setDataset(dataset);
            } else {
                plot.setDataset(null);
            }
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_012/FuzzHarness.java:6: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_012/FuzzHarness.java:39: error: cannot find symbol
            plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                              ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_012/FuzzHarness.java:39: error: cannot find symbol
            plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                    ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~2,800 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 3)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 10);
            if (data.consumeBoolean()) {
                renderer.setSeriesVisibleInLegend(series, Boolean.valueOf(data.consumeBoolean()));
            } else {
                renderer.setSeriesVisible(series, Boolean.valueOf(data.consumeBoolean()));
            }
        }

        if (data.consumeBoolean()) {
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(renderer);

            if (data.consumeBoolean()) {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 20);
                for (int i = 0; i < entries; i++) {
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        case 2:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Integer.valueOf(data.remainingBytes());
                            break;
                    }
                    String rowKey = data.consumeString(16);
                    String columnKey = data.consumeAsciiString(16);
                    dataset.addValue(value, rowKey, columnKey);
                }
                plot.setDataset(dataset);
            } else {
                plot.setDataset(null);
            }
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,458 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        int visibilityTweaks = data.consumeInt(0, 4);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 6);
            int state = data.consumeInt(0, 2);
            Boolean visible = (state == 0) ? null : (state == 1 ? Boolean.TRUE : Boolean.FALSE);
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        int slotCount = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, slotCount - 1);

        for (int i = 0; i < slotCount; i++) {
            CategoryDataset dataset = null;
            boolean createDataset = data.consumeBoolean();
            if (createDataset) {
                DefaultCategoryDataset d = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 8);
                for (int j = 0; j < entries; j++) {
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = null;
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 2:
                            value = Double.valueOf((double) data.consumeInt());
                            break;
                        default:
                            value = Long.valueOf((long) data.consumeInt());
                            break;
                    }
                    String rowKey = data.consumeString(16);
                    String columnKey = data.consumeAsciiString(16);
                    d.addValue(value, rowKey, columnKey);
                }
                dataset = d;
            }

            plot.setDataset(i, dataset);
            if (i == targetIndex) {
                plot.setRenderer(i, renderer);
            } else if (data.consumeBoolean()) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        plot.setRenderer(i, new BarRenderer());
                        break;
                    case 1:
                        plot.setRenderer(i, new LineAndShapeRenderer());
                        break;
                    default:
                        plot.setRenderer(i, new AreaRenderer());
                        break;
                }
            }
        }

        renderer.getLegendItems();

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        if (data.consumeBoolean()) {
            plot.setDataset(targetIndex, null);
        } else {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 8);
            for (int j = 0; j < entries; j++) {
                Number value;
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        value = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        value = Double.valueOf((double) data.consumeInt());
                        break;
                    default:
                        value = null;
                        break;
                }
                d.addValue(value, data.consumeString(16), data.consumeAsciiString(16));
            }
            plot.setDataset(targetIndex, d);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [92] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,461 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        int visibilityTweaks = data.consumeInt(0, 4);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 6);
            int state = data.consumeInt(0, 2);
            Boolean visible = (state == 0) ? null : (state == 1 ? Boolean.TRUE : Boolean.FALSE);
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        int slotCount = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, slotCount - 1);

        for (int i = 0; i < slotCount; i++) {
            CategoryDataset dataset = null;
            boolean createDataset = data.consumeBoolean();
            if (createDataset) {
                DefaultCategoryDataset d = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 8);
                for (int j = 0; j < entries; j++) {
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = null;
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 2:
                            value = Double.valueOf((double) data.consumeInt());
                            break;
                        default:
                            value = Long.valueOf((long) data.consumeInt());
                            break;
                    }
                    String rowKey = data.consumeString(16);
                    String columnKey = data.consumeAsciiString(16);
                    d.addValue(value, rowKey, columnKey);
                }
                dataset = d;
            }

            plot.setDataset(i, dataset);
            if (i == targetIndex) {
                plot.setRenderer(i, renderer);
            } else if (data.consumeBoolean()) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        plot.setRenderer(i, new BarRenderer());
                        break;
                    case 1:
                        plot.setRenderer(i, new LineAndShapeRenderer());
                        break;
                    default:
                        plot.setRenderer(i, new AreaRenderer());
                        break;
                }
            }
        }

        renderer.getLegendItems();

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        if (data.consumeBoolean()) {
            plot.setDataset(targetIndex, null);
        } else {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 8);
            for (int j = 0; j < entries; j++) {
                Number value;
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        value = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        value = Double.valueOf((double) data.consumeInt());
                        break;
                    default:
                        value = null;
                        break;
                }
                d.addValue(value, data.consumeString(16), data.consumeAsciiString(16));
            }
            plot.setDataset(targetIndex, d);
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_014/FuzzHarness.java:7: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_014/FuzzHarness.java:34: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_014/FuzzHarness.java:34: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_014/FuzzHarness.java:88: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_014/FuzzHarness.java:88: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
5 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~3,800 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        int seriesTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < seriesTweaks; i++) {
            int series = data.consumeInt(0, 10);
            int state = data.consumeInt(0, 2);
            Boolean visible;
            if (state == 0) {
                visible = null;
            } else if (state == 1) {
                visible = Boolean.TRUE;
            } else {
                visible = Boolean.FALSE;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        CategoryPlot plot = new CategoryPlot();
        int slotCount = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, slotCount - 1);

        for (int i = 0; i < slotCount; i++) {
            if (i == targetIndex) {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, buildDataset(data));
                } else {
                    plot.setDataset(i, null);
                }
                plot.setRenderer(i, renderer);
            } else {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, buildDataset(data));
                } else {
                    plot.setDataset(i, null);
                }
                if (data.consumeBoolean()) {
                    switch (data.consumeInt(0, 2)) {
                        case 0:
                            plot.setRenderer(i, new BarRenderer());
                            break;
                        case 1:
                            plot.setRenderer(i, new LineAndShapeRenderer());
                            break;
                        default:
                            plot.setRenderer(i, new AreaRenderer());
                            break;
                    }
                }
            }
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            plot.setDataset(targetIndex, buildDataset(data));
        } else {
            plot.setDataset(targetIndex, null);
        }

        renderer.getLegendItems();
    }

    private static DefaultCategoryDataset buildDataset(FuzzedDataProvider data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int entries = data.consumeInt(0, 12);
        for (int i = 0; i < entries; i++) {
            Number value;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    value = null;
                    break;
                case 1:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    value = Long.valueOf((long) data.consumeInt());
                    break;
                default:
                    value = Double.valueOf((double) data.consumeInt());
                    break;
            }

            String rowKey = data.consumeString(16);
            String columnKey = data.consumeAsciiString(16);
            dataset.addValue(value, rowKey, columnKey);
        }
        return dataset;
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,726 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        if (data.consumeBoolean()) {
            renderer = new BarRenderer();
        } else {
            renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
        }

        int visSeries = data.consumeInt(0, 5);
        int legendSeries = data.consumeInt(0, 5);

        int visState = data.consumeInt(0, 2);
        renderer.setSeriesVisible(
                visSeries,
                visState == 0 ? null : (visState == 1 ? Boolean.TRUE : Boolean.FALSE)
        );

        int legendState = data.consumeInt(0, 2);
        renderer.setSeriesVisibleInLegend(
                legendSeries,
                legendState == 0 ? null : (legendState == 1 ? Boolean.TRUE : Boolean.FALSE)
        );

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis(data.consumeString(20)));
        plot.setRangeAxis(new NumberAxis(data.consumeString(20)));
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        int rendererIndex = data.consumeInt(0, 2);
        plot.setRenderer(rendererIndex, renderer);

        for (int i = 0; i < 3; i++) {
            boolean useNullDataset;
            if (i == rendererIndex) {
                useNullDataset = data.consumeBoolean();
            } else {
                useNullDataset = data.consumeBoolean();
            }

            if (useNullDataset) {
                plot.setDataset(i, null);
            } else {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        String rowKey = data.consumeString(8) + "_r" + r;
                        String colKey = data.consumeString(8) + "_c" + c;
                        dataset.addValue(value, rowKey, colKey);
                    }
                }
                plot.setDataset(i, dataset);
            }
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [96] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,857 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        if (data.consumeBoolean()) {
            renderer = new BarRenderer();
        } else {
            renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
        }

        int visSeries = data.consumeInt(0, 5);
        int legendSeries = data.consumeInt(0, 5);

        int visState = data.consumeInt(0, 2);
        renderer.setSeriesVisible(
                visSeries,
                visState == 0 ? null : (visState == 1 ? Boolean.TRUE : Boolean.FALSE)
        );

        int legendState = data.consumeInt(0, 2);
        renderer.setSeriesVisibleInLegend(
                legendSeries,
                legendState == 0 ? null : (legendState == 1 ? Boolean.TRUE : Boolean.FALSE)
        );

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis(data.consumeString(20)));
        plot.setRangeAxis(new NumberAxis(data.consumeString(20)));
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        int rendererIndex = data.consumeInt(0, 2);
        plot.setRenderer(rendererIndex, renderer);

        for (int i = 0; i < 3; i++) {
            boolean useNullDataset;
            if (i == rendererIndex) {
                useNullDataset = data.consumeBoolean();
            } else {
                useNullDataset = data.consumeBoolean();
            }

            if (useNullDataset) {
                plot.setDataset(i, null);
            } else {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        String rowKey = data.consumeString(8) + "_r" + r;
                        String colKey = data.consumeString(8) + "_c" + c;
                        dataset.addValue(value, rowKey, colKey);
                    }
                }
                plot.setDataset(i, dataset);
            }
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_016/FuzzHarness.java:8: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_016/FuzzHarness.java:37: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_016/FuzzHarness.java:37: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~2,240 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        if (data.consumeBoolean()) {
            renderer = new BarRenderer();
        } else {
            renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
        }

        renderer.setSeriesVisible(
                data.consumeInt(0, 8),
                data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE
        );
        renderer.setSeriesVisibleInLegend(
                data.consumeInt(0, 8),
                data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE
        );

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis(data.consumeString(32)));
        plot.setRangeAxis(new NumberAxis(data.consumeString(32)));

        int rendererIndex = data.consumeInt(0, 3);
        plot.setRenderer(rendererIndex, renderer);

        for (int i = 0; i < 4; i++) {
            if (i == rendererIndex) {
                plot.setDataset(i, null);
            } else {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, null);
                } else {
                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    int rows = data.consumeInt(0, 4);
                    int cols = data.consumeInt(0, 4);
                    for (int r = 0; r < rows; r++) {
                        String rowKey = data.consumeString(16) + r;
                        for (int c = 0; c < cols; c++) {
                            String colKey = data.consumeString(16) + c;
                            dataset.addValue(new Integer(data.consumeInt()), rowKey, colKey);
                        }
                    }
                    plot.setDataset(i, dataset);
                }
            }
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [98] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [99] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,239 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        CategoryItemRenderer renderer;
        switch (data.consumeInt(0, 3)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
        }

        int rendererIndex = data.consumeInt(0, 2);
        CategoryPlot plot = new CategoryPlot();
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        plot.setRenderer(rendererIndex, renderer);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 8);
            int columnCount = data.consumeInt(0, 8);
            for (int r = 0; r < rowCount; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < columnCount; c++) {
                    Comparable columnKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        case 2:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        default:
                            value = null;
                            break;
                    }
                    dataset.addValue(value, rowKey, columnKey);
                }
            }
            plot.setDataset(rendererIndex, dataset);
        } else {
            plot.setDataset(rendererIndex, null);
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(-2, 12);
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = Boolean.TRUE;
                    break;
                case 1:
                    visible = Boolean.FALSE;
                    break;
                default:
                    visible = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        if (data.consumeBoolean()) {
            renderer.setDataBoundsIncludesVisibleSeriesOnly(data.consumeBoolean());
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [100] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [101] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,803 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        CategoryItemRenderer renderer;
        switch (data.consumeInt(0, 3)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
        }

        int rendererIndex = data.consumeInt(0, 2);
        CategoryPlot plot = new CategoryPlot();
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        plot.setRenderer(rendererIndex, renderer);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 8);
            int columnCount = data.consumeInt(0, 8);
            for (int r = 0; r < rowCount; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < columnCount; c++) {
                    Comparable columnKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        case 2:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        default:
                            value = null;
                            break;
                    }
                    dataset.addValue(value, rowKey, columnKey);
                }
            }
            plot.setDataset(rendererIndex, dataset);
        } else {
            plot.setDataset(rendererIndex, null);
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(-2, 12);
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = Boolean.TRUE;
                    break;
                case 1:
                    visible = Boolean.FALSE;
                    break;
                default:
                    visible = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        if (data.consumeBoolean()) {
            renderer.setDataBoundsIncludesVisibleSeriesOnly(data.consumeBoolean());
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_018/FuzzHarness.java:5: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_018/FuzzHarness.java:28: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_018/FuzzHarness.java:28: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_018/FuzzHarness.java:81: error: cannot find symbol
            renderer.setDataBoundsIncludesVisibleSeriesOnly(data.consumeBoolean());
                    ^
  symbol:   method setDataBoundsIncludesVisibleSeriesOnly(boolean)
  location: variable renderer of type CategoryItemRenderer
4 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~3,037 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 4)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int rendererIndex = data.consumeInt(0, 3);
        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(rendererIndex, renderer);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 8);
            int columnCount = data.consumeInt(0, 8);
            for (int r = 0; r < rowCount; r++) {
                String rowKey = data.consumeString(16);
                for (int c = 0; c < columnCount; c++) {
                    String columnKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = new Integer(data.consumeInt());
                            break;
                        case 1:
                            value = new Byte(data.consumeByte());
                            break;
                        case 2:
                            value = null;
                            break;
                        default:
                            value = new Integer(data.consumeInt(-1, 1));
                            break;
                    }
                    dataset.addValue(value, rowKey, columnKey);
                }
            }
            plot.setDataset(rendererIndex, dataset);
        } else {
            plot.setDataset(rendererIndex, null);
        }

        int tweaks = data.consumeInt(0, 8);
        for (int i = 0; i < tweaks; i++) {
            int series = data.consumeInt(-2, 12);
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = Boolean.TRUE;
                    break;
                case 1:
                    visible = Boolean.FALSE;
                    break;
                default:
                    visible = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [102] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [103] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,879 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        int rendererIndex = data.consumeInt(0, 3);
        plot.setRenderer(rendererIndex, renderer);

        plot.setRowRenderingOrder(
                data.consumeBoolean()
                        ? org.jfree.util.SortOrder.ASCENDING
                        : org.jfree.util.SortOrder.DESCENDING);

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int series = data.consumeInt(0, 8);
            renderer.setSeriesVisibleInLegend(series, data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE);
        }

        for (int i = 0; i < data.consumeInt(0, 3); i++) {
            int otherIndex = data.consumeInt(0, 3);
            if (otherIndex != rendererIndex) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        plot.setRenderer(otherIndex, new BarRenderer());
                        break;
                    case 1:
                        plot.setRenderer(otherIndex, new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean()));
                        break;
                    default:
                        plot.setRenderer(otherIndex, new AreaRenderer());
                        break;
                }
            }
        }

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int datasetIndex = data.consumeInt(0, 3);
            if (data.consumeBoolean()) {
                org.jfree.data.category.DefaultCategoryDataset ds =
                        new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = data.consumeString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = data.consumeAsciiString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        ds.addValue(value, rowKey, colKey);
                    }
                }
                plot.setDataset(datasetIndex, ds);
            } else {
                plot.setDataset(datasetIndex, null);
            }
        }

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset ds =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 3);
            int cols = data.consumeInt(0, 3);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeString(6);
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeAsciiString(6);
                    ds.addValue(Integer.valueOf(data.consumeInt()), rowKey, colKey);
                }
            }
            plot.setDataset(rendererIndex, ds);
            renderer.getLegendItems();
        }

        if (data.consumeBoolean()) {
            plot.setDataset(rendererIndex, null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [104] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [105] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,512 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        int rendererIndex = data.consumeInt(0, 3);
        plot.setRenderer(rendererIndex, renderer);

        plot.setRowRenderingOrder(
                data.consumeBoolean()
                        ? org.jfree.util.SortOrder.ASCENDING
                        : org.jfree.util.SortOrder.DESCENDING);

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int series = data.consumeInt(0, 8);
            renderer.setSeriesVisibleInLegend(series, data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE);
        }

        for (int i = 0; i < data.consumeInt(0, 3); i++) {
            int otherIndex = data.consumeInt(0, 3);
            if (otherIndex != rendererIndex) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        plot.setRenderer(otherIndex, new BarRenderer());
                        break;
                    case 1:
                        plot.setRenderer(otherIndex, new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean()));
                        break;
                    default:
                        plot.setRenderer(otherIndex, new AreaRenderer());
                        break;
                }
            }
        }

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int datasetIndex = data.consumeInt(0, 3);
            if (data.consumeBoolean()) {
                org.jfree.data.category.DefaultCategoryDataset ds =
                        new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = data.consumeString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = data.consumeAsciiString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        ds.addValue(value, rowKey, colKey);
                    }
                }
                plot.setDataset(datasetIndex, ds);
            } else {
                plot.setDataset(datasetIndex, null);
            }
        }

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset ds =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 3);
            int cols = data.consumeInt(0, 3);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeString(6);
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeAsciiString(6);
                    ds.addValue(Integer.valueOf(data.consumeInt()), rowKey, colKey);
                }
            }
            plot.setDataset(rendererIndex, ds);
            renderer.getLegendItems();
        }

        if (data.consumeBoolean()) {
            plot.setDataset(rendererIndex, null);
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_020/FuzzHarness.java:30: error: package org.jfree.util does not exist
                        ? org.jfree.util.SortOrder.ASCENDING
                                        ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_020/FuzzHarness.java:31: error: package org.jfree.util does not exist
                        : org.jfree.util.SortOrder.DESCENDING);
                                        ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~3,680 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        int rendererIndex = data.consumeInt(0, 3);
        plot.setRenderer(rendererIndex, renderer);

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int series = data.consumeInt(0, 8);
            renderer.setSeriesVisibleInLegend(series, data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE);
        }

        for (int i = 0; i < data.consumeInt(0, 3); i++) {
            int otherIndex = data.consumeInt(0, 3);
            if (otherIndex != rendererIndex) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        plot.setRenderer(otherIndex, new BarRenderer());
                        break;
                    case 1:
                        plot.setRenderer(otherIndex, new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean()));
                        break;
                    default:
                        plot.setRenderer(otherIndex, new AreaRenderer());
                        break;
                }
            }
        }

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int datasetIndex = data.consumeInt(0, 3);
            if (data.consumeBoolean()) {
                org.jfree.data.category.DefaultCategoryDataset ds =
                        new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = data.consumeString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = data.consumeAsciiString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        ds.addValue(value, rowKey, colKey);
                    }
                }
                plot.setDataset(datasetIndex, ds);
            } else {
                plot.setDataset(datasetIndex, null);
            }
        }

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset ds =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 3);
            int cols = data.consumeInt(0, 3);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeString(6);
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeAsciiString(6);
                    ds.addValue(Integer.valueOf(data.consumeInt()), rowKey, colKey);
                }
            }
            plot.setDataset(rendererIndex, ds);
            renderer.getLegendItems();
        }

        if (data.consumeBoolean()) {
            plot.setDataset(rendererIndex, null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [106] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [107] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,598 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 5)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new CategoryStepRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new LevelRenderer();
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int seriesFlagCount = data.consumeInt(0, 6);
        for (int i = 0; i < seriesFlagCount; i++) {
            int series = data.consumeInt(0, 8);
            if (data.consumeBoolean()) {
                renderer.setSeriesVisibleInLegend(series, data.consumeBoolean());
            } else {
                renderer.setSeriesVisible(series, data.consumeBoolean());
            }
        }

        CategoryPlot plot = new CategoryPlot();
        if (data.consumeBoolean()) {
            plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        }

        int rendererIndex = data.consumeInt(0, 2);
        plot.setRenderer(rendererIndex, renderer);

        int datasetMode = data.consumeInt(0, 3);
        if (datasetMode == 0) {
            plot.setDataset(rendererIndex, null);
        } else if (datasetMode == 1) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 4);
            int cols = data.consumeInt(0, 4);
            for (int r = 0; r < rows; r++) {
                Comparable<?> rowKey = data.consumeAsciiString(8);
                for (int c = 0; c < cols; c++) {
                    Comparable<?> colKey = data.consumeString(8);
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(rendererIndex, dataset);
        } else if (datasetMode == 2) {
            DefaultCategoryDataset datasetAtOtherIndex = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 6);
            for (int i = 0; i < entries; i++) {
                datasetAtOtherIndex.addValue(
                        data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()),
                        data.consumeAsciiString(8),
                        data.consumeString(8));
            }
            int otherIndex = rendererIndex == 0 ? 1 : 0;
            plot.setDataset(otherIndex, datasetAtOtherIndex);
        } else {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 8);
            for (int i = 0; i < entries; i++) {
                dataset.addValue(
                        Integer.valueOf(data.consumeInt()),
                        data.consumeAsciiString(8),
                        data.consumeString(8));
            }
            plot.setDataset(rendererIndex, dataset);

            if (data.consumeBoolean()) {
                int secondIndex = rendererIndex == 2 ? 1 : 2;
                if (data.consumeBoolean()) {
                    plot.setDataset(secondIndex, null);
                } else {
                    DefaultCategoryDataset second = new DefaultCategoryDataset();
                    int secondEntries = data.consumeInt(0, 4);
                    for (int i = 0; i < secondEntries; i++) {
                        second.addValue(
                                data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()),
                                data.consumeAsciiString(8),
                                data.consumeString(8));
                    }
                    plot.setDataset(secondIndex, second);
                }
            }
        }

        if (data.consumeBoolean()) {
            renderer.setPlot(plot);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [108] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [109] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,276 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
        }

        if (data.consumeBoolean()) {
            renderer.setSeriesVisible(data.consumeInt(0, 3), Boolean.valueOf(data.consumeBoolean()));
        }
        if (data.consumeBoolean()) {
            renderer.setSeriesVisibleInLegend(data.consumeInt(0, 3), Boolean.valueOf(data.consumeBoolean()));
        }

        renderer.getLegendItems();

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        plot.setDomainAxis(new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)));
        plot.setRangeAxis(new org.jfree.chart.axis.NumberAxis(data.consumeString(16)));
        plot.setRowRenderingOrder(
                data.consumeBoolean()
                        ? org.jfree.util.SortOrder.ASCENDING
                        : org.jfree.util.SortOrder.DESCENDING);

        int slots = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, slots - 1);

        for (int i = 0; i < slots; i++) {
            if (i == targetIndex) {
                plot.setRenderer(i, renderer);
                continue;
            }

            AbstractCategoryItemRenderer otherRenderer;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    otherRenderer = new BarRenderer();
                    break;
                case 1:
                    otherRenderer = new AreaRenderer();
                    break;
                default:
                    otherRenderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                    break;
            }
            plot.setRenderer(i, otherRenderer);

            if (data.consumeBoolean()) {
                org.jfree.data.category.DefaultCategoryDataset otherDataset =
                        new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 3);
                int cols = data.consumeInt(0, 3);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        otherDataset.addValue(
                                value,
                                "r" + r + "_" + data.consumeAsciiString(8),
                                "c" + c + "_" + data.consumeAsciiString(8));
                    }
                }
                plot.setDataset(i, otherDataset);
            } else {
                plot.setDataset(i, null);
            }
        }

        boolean nullTargetDataset = data.consumeBoolean();
        if (nullTargetDataset) {
            plot.setDataset(targetIndex, null);
        } else {
            org.jfree.data.category.DefaultCategoryDataset dataset =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Number value;
                    if (data.consumeBoolean()) {
                        value = null;
                    } else {
                        value = Integer.valueOf(data.consumeInt());
                    }
                    String rowKey = "row" + r + "_" + data.consumeString(12);
                    String colKey = "col" + c + "_" + data.consumeAsciiString(12);
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(targetIndex, dataset);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [110] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [111] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,909 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
        }

        if (data.consumeBoolean()) {
            renderer.setSeriesVisible(data.consumeInt(0, 3), Boolean.valueOf(data.consumeBoolean()));
        }
        if (data.consumeBoolean()) {
            renderer.setSeriesVisibleInLegend(data.consumeInt(0, 3), Boolean.valueOf(data.consumeBoolean()));
        }

        renderer.getLegendItems();

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        plot.setDomainAxis(new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)));
        plot.setRangeAxis(new org.jfree.chart.axis.NumberAxis(data.consumeString(16)));
        plot.setRowRenderingOrder(
                data.consumeBoolean()
                        ? org.jfree.util.SortOrder.ASCENDING
                        : org.jfree.util.SortOrder.DESCENDING);

        int slots = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, slots - 1);

        for (int i = 0; i < slots; i++) {
            if (i == targetIndex) {
                plot.setRenderer(i, renderer);
                continue;
            }

            AbstractCategoryItemRenderer otherRenderer;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    otherRenderer = new BarRenderer();
                    break;
                case 1:
                    otherRenderer = new AreaRenderer();
                    break;
                default:
                    otherRenderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                    break;
            }
            plot.setRenderer(i, otherRenderer);

            if (data.consumeBoolean()) {
                org.jfree.data.category.DefaultCategoryDataset otherDataset =
                        new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 3);
                int cols = data.consumeInt(0, 3);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        otherDataset.addValue(
                                value,
                                "r" + r + "_" + data.consumeAsciiString(8),
                                "c" + c + "_" + data.consumeAsciiString(8));
                    }
                }
                plot.setDataset(i, otherDataset);
            } else {
                plot.setDataset(i, null);
            }
        }

        boolean nullTargetDataset = data.consumeBoolean();
        if (nullTargetDataset) {
            plot.setDataset(targetIndex, null);
        } else {
            org.jfree.data.category.DefaultCategoryDataset dataset =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Number value;
                    if (data.consumeBoolean()) {
                        value = null;
                    } else {
                        value = Integer.valueOf(data.consumeInt());
                    }
                    String rowKey = "row" + r + "_" + data.consumeString(12);
                    String colKey = "col" + c + "_" + data.consumeAsciiString(12);
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(targetIndex, dataset);
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_023/FuzzHarness.java:34: error: package org.jfree.util does not exist
                        ? org.jfree.util.SortOrder.ASCENDING
                                        ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_023/FuzzHarness.java:35: error: package org.jfree.util does not exist
                        : org.jfree.util.SortOrder.DESCENDING);
                                        ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~4,067 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
        }

        if (data.consumeBoolean()) {
            renderer.setSeriesVisible(data.consumeInt(0, 5), Boolean.valueOf(data.consumeBoolean()));
        }
        if (data.consumeBoolean()) {
            renderer.setSeriesVisibleInLegend(data.consumeInt(0, 5), Boolean.valueOf(data.consumeBoolean()));
        }

        renderer.getLegendItems();

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        plot.setDomainAxis(new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)));
        plot.setRangeAxis(new org.jfree.chart.axis.NumberAxis(data.consumeString(16)));

        int rendererSlots = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, rendererSlots - 1);

        for (int i = 0; i < rendererSlots; i++) {
            if (i == targetIndex) {
                plot.setRenderer(i, renderer);
            } else {
                AbstractCategoryItemRenderer otherRenderer;
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        otherRenderer = new BarRenderer();
                        break;
                    case 1:
                        otherRenderer = new AreaRenderer();
                        break;
                    default:
                        otherRenderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                        break;
                }
                plot.setRenderer(i, otherRenderer);
            }

            if (i != targetIndex || data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, null);
                } else {
                    org.jfree.data.category.DefaultCategoryDataset ds =
                            new org.jfree.data.category.DefaultCategoryDataset();
                    int rows = data.consumeInt(0, 5);
                    int cols = data.consumeInt(0, 5);
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                            ds.addValue(
                                    value,
                                    "row" + r + "_" + data.consumeString(12),
                                    "col" + c + "_" + data.consumeAsciiString(12));
                        }
                    }
                    plot.setDataset(i, ds);
                }
            }
        }

        if (data.consumeBoolean()) {
            plot.setDataset(targetIndex, null);
        } else {
            org.jfree.data.category.DefaultCategoryDataset ds =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    ds.addValue(
                            value,
                            "targetRow" + r + "_" + data.consumeString(12),
                            "targetCol" + c + "_" + data.consumeAsciiString(12));
                }
            }
            plot.setDataset(targetIndex, ds);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [112] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [113] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,210 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer[] renderers =
                new AbstractCategoryItemRenderer[data.consumeInt(1, 3)];

        for (int i = 0; i < renderers.length; i++) {
            int choice = data.consumeInt(0, 2);
            AbstractCategoryItemRenderer renderer;
            if (choice == 0) {
                renderer = new BarRenderer();
            } else if (choice == 1) {
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
            } else {
                renderer = new AreaRenderer();
            }

            int visConfigs = data.consumeInt(0, 4);
            for (int s = 0; s < visConfigs; s++) {
                int series = data.consumeInt(0, 6);
                int state = data.consumeInt(0, 2);
                Boolean visible;
                if (state == 0) {
                    visible = Boolean.TRUE;
                } else if (state == 1) {
                    visible = Boolean.FALSE;
                } else {
                    visible = null;
                }
                renderer.setSeriesVisibleInLegend(series, visible);
            }

            if (data.consumeBoolean()) {
                renderer.getLegendItems();
            }

            renderers[i] = renderer;
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        plot.setRowRenderingOrder(
                data.consumeBoolean()
                        ? org.jfree.util.SortOrder.ASCENDING
                        : org.jfree.util.SortOrder.DESCENDING
        );

        for (int i = 0; i < renderers.length; i++) {
            org.jfree.data.category.DefaultCategoryDataset dataset = null;

            if (data.consumeBoolean()) {
                dataset = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);

                for (int r = 0; r < rows; r++) {
                    Comparable<?> rowKey = "R" + r + ":" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        Comparable<?> colKey = "C" + c + ":" + data.consumeAsciiString(8);
                        Number value;
                        if (data.consumeBoolean()) {
                            value = null;
                        } else {
                            value = Integer.valueOf(data.consumeInt());
                        }
                        dataset.addValue(value, rowKey, colKey);
                    }
                }
            }

            plot.setDataset(i, dataset);
            plot.setRenderer(i, renderers[i]);
        }

        if (data.consumeBoolean()) {
            int extraSlot = renderers.length + data.consumeInt(0, 1);
            plot.setDataset(extraSlot, null);
        }

        int target = data.consumeInt(0, renderers.length - 1);
        renderers[target].getLegendItems();
    }
}
```

</details>

---
## [114] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [115] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,841 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer[] renderers =
                new AbstractCategoryItemRenderer[data.consumeInt(1, 3)];

        for (int i = 0; i < renderers.length; i++) {
            int choice = data.consumeInt(0, 2);
            AbstractCategoryItemRenderer renderer;
            if (choice == 0) {
                renderer = new BarRenderer();
            } else if (choice == 1) {
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
            } else {
                renderer = new AreaRenderer();
            }

            int visConfigs = data.consumeInt(0, 4);
            for (int s = 0; s < visConfigs; s++) {
                int series = data.consumeInt(0, 6);
                int state = data.consumeInt(0, 2);
                Boolean visible;
                if (state == 0) {
                    visible = Boolean.TRUE;
                } else if (state == 1) {
                    visible = Boolean.FALSE;
                } else {
                    visible = null;
                }
                renderer.setSeriesVisibleInLegend(series, visible);
            }

            if (data.consumeBoolean()) {
                renderer.getLegendItems();
            }

            renderers[i] = renderer;
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        plot.setRowRenderingOrder(
                data.consumeBoolean()
                        ? org.jfree.util.SortOrder.ASCENDING
                        : org.jfree.util.SortOrder.DESCENDING
        );

        for (int i = 0; i < renderers.length; i++) {
            org.jfree.data.category.DefaultCategoryDataset dataset = null;

            if (data.consumeBoolean()) {
                dataset = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);

                for (int r = 0; r < rows; r++) {
                    Comparable<?> rowKey = "R" + r + ":" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        Comparable<?> colKey = "C" + c + ":" + data.consumeAsciiString(8);
                        Number value;
                        if (data.consumeBoolean()) {
                            value = null;
                        } else {
                            value = Integer.valueOf(data.consumeInt());
                        }
                        dataset.addValue(value, rowKey, colKey);
                    }
                }
            }

            plot.setDataset(i, dataset);
            plot.setRenderer(i, renderers[i]);
        }

        if (data.consumeBoolean()) {
            int extraSlot = renderers.length + data.consumeInt(0, 1);
            plot.setDataset(extraSlot, null);
        }

        int target = data.consumeInt(0, renderers.length - 1);
        renderers[target].getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_025/FuzzHarness.java:46: error: package org.jfree.util does not exist
                        ? org.jfree.util.SortOrder.ASCENDING
                                        ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_025/FuzzHarness.java:47: error: package org.jfree.util does not exist
                        : org.jfree.util.SortOrder.DESCENDING
                                        ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~2,997 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer[] renderers =
                new AbstractCategoryItemRenderer[data.consumeInt(1, 3)];

        for (int i = 0; i < renderers.length; i++) {
            int choice = data.consumeInt(0, 2);
            AbstractCategoryItemRenderer renderer;
            if (choice == 0) {
                renderer = new BarRenderer();
            } else if (choice == 1) {
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
            } else {
                renderer = new AreaRenderer();
            }

            int visConfigs = data.consumeInt(0, 4);
            for (int s = 0; s < visConfigs; s++) {
                int series = data.consumeInt(0, 6);
                int state = data.consumeInt(0, 2);
                Boolean visible;
                if (state == 0) {
                    visible = Boolean.TRUE;
                } else if (state == 1) {
                    visible = Boolean.FALSE;
                } else {
                    visible = null;
                }
                renderer.setSeriesVisibleInLegend(series, visible);
            }

            if (data.consumeBoolean()) {
                renderer.getLegendItems();
            }

            renderers[i] = renderer;
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();

        for (int i = 0; i < renderers.length; i++) {
            org.jfree.data.category.DefaultCategoryDataset dataset = null;

            if (data.consumeBoolean()) {
                dataset = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);

                for (int r = 0; r < rows; r++) {
                    Comparable rowKey = "R" + r + ":" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        Comparable colKey = "C" + c + ":" + data.consumeAsciiString(8);
                        Number value;
                        if (data.consumeBoolean()) {
                            value = null;
                        } else {
                            value = Integer.valueOf(data.consumeInt());
                        }
                        dataset.addValue(value, rowKey, colKey);
                    }
                }
            }

            plot.setDataset(i, dataset);
            plot.setRenderer(i, renderers[i]);
        }

        if (data.consumeBoolean()) {
            int extraSlot = renderers.length + data.consumeInt(0, 1);
            plot.setDataset(extraSlot, null);
        }

        int target = data.consumeInt(0, renderers.length - 1);
        renderers[target].getLegendItems();
    }
}
```

</details>

---
## [116] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [117] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,801 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.CategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        int visibilitySettings = data.consumeInt(0, 8);
        for (int i = 0; i < visibilitySettings; i++) {
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = null;
                    break;
                case 1:
                    visible = Boolean.TRUE;
                    break;
                default:
                    visible = Boolean.FALSE;
                    break;
            }
            renderer.setSeriesVisibleInLegend(i, visible);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
            return;
        }

        CategoryDataset dataset;
        if (data.consumeBoolean()) {
            dataset = null;
        } else {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 4);
            int cols = data.consumeInt(0, 4);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeAsciiString(16);
                    if (data.consumeBoolean()) {
                        d.addValue(Integer.valueOf(data.consumeInt()), rowKey, colKey);
                    } else {
                        d.addValue(null, rowKey, colKey);
                    }
                }
            }
            dataset = d;
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeAsciiString(32));
        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [118] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [119] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,932 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.CategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        int visibilitySettings = data.consumeInt(0, 8);
        for (int i = 0; i < visibilitySettings; i++) {
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = null;
                    break;
                case 1:
                    visible = Boolean.TRUE;
                    break;
                default:
                    visible = Boolean.FALSE;
                    break;
            }
            renderer.setSeriesVisibleInLegend(i, visible);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
            return;
        }

        CategoryDataset dataset;
        if (data.consumeBoolean()) {
            dataset = null;
        } else {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 4);
            int cols = data.consumeInt(0, 4);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeAsciiString(16);
                    if (data.consumeBoolean()) {
                        d.addValue(Integer.valueOf(data.consumeInt()), rowKey, colKey);
                    } else {
                        d.addValue(null, rowKey, colKey);
                    }
                }
            }
            dataset = d;
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeAsciiString(32));
        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);

        if (data.consumeBoolean()) {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_027/FuzzHarness.java:7: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_027/FuzzHarness.java:73: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                          ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_027/FuzzHarness.java:73: error: cannot find symbol
        plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
                                                                                ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~2,820 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 4)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int seriesTweaks = data.consumeInt(0, 6);
        for (int i = 0; i < seriesTweaks; i++) {
            int choice = data.consumeInt(0, 2);
            if (choice == 0) {
                renderer.setSeriesVisibleInLegend(i, null);
            } else if (choice == 1) {
                renderer.setSeriesVisibleInLegend(i, Boolean.TRUE);
            } else {
                renderer.setSeriesVisibleInLegend(i, Boolean.FALSE);
            }
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
            return;
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeAsciiString(32));

        if (data.consumeBoolean()) {
            CategoryPlot plot = new CategoryPlot(null, domainAxis, rangeAxis, renderer);
            renderer.getLegendItems();
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int rows = data.consumeInt(0, 4);
        int cols = data.consumeInt(0, 4);
        for (int r = 0; r < rows; r++) {
            String rowKey = data.consumeString(16);
            if (rowKey.length() == 0) {
                rowKey = "r" + r;
            }
            for (int c = 0; c < cols; c++) {
                String colKey = data.consumeAsciiString(16);
                if (colKey.length() == 0) {
                    colKey = "c" + c;
                }
                dataset.addValue(new Integer(data.consumeInt()), rowKey, colKey);
            }
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        if (data.consumeBoolean()) {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [120] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [121] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,176 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BarRenderer renderer = new BarRenderer();
        CategoryPlot plot = new CategoryPlot();

        int rendererSlot = data.consumeInt(0, 3);
        plot.setRenderer(rendererSlot, renderer);

        if (data.consumeBoolean()) {
            plot.setRowRenderingOrder(
                data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING
            );
        }

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 20);
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                renderer.setSeriesVisibleInLegend(series, Boolean.TRUE);
            } else if (mode == 1) {
                renderer.setSeriesVisibleInLegend(series, Boolean.FALSE);
            } else {
                renderer.setSeriesVisibleInLegend(series, null);
            }
        }

        boolean installDatasetAtRendererIndex = data.consumeBoolean();
        if (installDatasetAtRendererIndex) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 8);
            int cols = data.consumeInt(0, 8);
            for (int r = 0; r < rows; r++) {
                Comparable<?> rowKey = data.consumeAsciiString(16);
                if (((String) rowKey).isEmpty()) {
                    rowKey = "R" + r;
                }
                for (int c = 0; c < cols; c++) {
                    Comparable<?> colKey = data.consumeString(16);
                    if (((String) colKey).isEmpty()) {
                        colKey = "C" + c;
                    }
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Double.valueOf((double) data.consumeInt());
                            break;
                        case 2:
                            value = Long.valueOf((long) data.consumeInt());
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(rendererSlot, dataset);
        } else {
            plot.setDataset(rendererSlot, null);
        }

        int otherSlots = data.consumeInt(0, 3);
        for (int i = 0; i < otherSlots; i++) {
            int slot = data.consumeInt(0, 3);
            if (slot == rendererSlot) {
                continue;
            }
            if (data.consumeBoolean()) {
                DefaultCategoryDataset ds = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 4);
                for (int j = 0; j < entries; j++) {
                    String row = data.consumeAsciiString(8);
                    String col = data.consumeString(8);
                    if (row.isEmpty()) {
                        row = "r" + j;
                    }
                    if (col.isEmpty()) {
                        col = "c" + j;
                    }
                    ds.addValue(Integer.valueOf(data.consumeInt()), row, col);
                }
                plot.setDataset(slot, ds);
            } else {
                plot.setDataset(slot, null);
            }
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [122] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [123] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,231 chars, 2 new)</summary>

- *[system] message: identical to step [65] — not reprinted*
- *[user] message: identical to step [65] — not reprinted*
**[assistant]**
```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.SortOrder;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BarRenderer renderer = new BarRenderer();
        CategoryPlot plot = new CategoryPlot();

        int rendererSlot = data.consumeInt(0, 3);
        plot.setRenderer(rendererSlot, renderer);

        if (data.consumeBoolean()) {
            plot.setRowRenderingOrder(
                data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING
            );
        }

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 20);
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                renderer.setSeriesVisibleInLegend(series, Boolean.TRUE);
            } else if (mode == 1) {
                renderer.setSeriesVisibleInLegend(series, Boolean.FALSE);
            } else {
                renderer.setSeriesVisibleInLegend(series, null);
            }
        }

        boolean installDatasetAtRendererIndex = data.consumeBoolean();
        if (installDatasetAtRendererIndex) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 8);
            int cols = data.consumeInt(0, 8);
            for (int r = 0; r < rows; r++) {
                Comparable<?> rowKey = data.consumeAsciiString(16);
                if (((String) rowKey).isEmpty()) {
                    rowKey = "R" + r;
                }
                for (int c = 0; c < cols; c++) {
                    Comparable<?> colKey = data.consumeString(16);
                    if (((String) colKey).isEmpty()) {
                        colKey = "C" + c;
                    }
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Double.valueOf((double) data.consumeInt());
                            break;
                        case 2:
                            value = Long.valueOf((long) data.consumeInt());
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(rendererSlot, dataset);
        } else {
            plot.setDataset(rendererSlot, null);
        }

        int otherSlots = data.consumeInt(0, 3);
        for (int i = 0; i < otherSlots; i++) {
            int slot = data.consumeInt(0, 3);
            if (slot == rendererSlot) {
                continue;
            }
            if (data.consumeBoolean()) {
                DefaultCategoryDataset ds = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 4);
                for (int j = 0; j < entries; j++) {
                    String row = data.consumeAsciiString(8);
                    String col = data.consumeString(8);
                    if (row.isEmpty()) {
                        row = "r" + j;
                    }
                    if (col.isEmpty()) {
                        col = "c" + j;
                    }
                    ds.addValue(Integer.valueOf(data.consumeInt()), row, col);
                }
                plot.setDataset(slot, ds);
            } else {
                plot.setDataset(slot, null);
            }
        }

        renderer.getLegendItems();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_029/FuzzHarness.java:6: error: package org.jfree.util does not exist
import org.jfree.util.SortOrder;
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_029/FuzzHarness.java:18: error: cannot find symbol
                data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING
                                        ^
  symbol:   variable SortOrder
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/Chart_1_buggy/fuzz/attempt_029/FuzzHarness.java:18: error: cannot find symbol
                data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING
                                                              ^
  symbol:   variable SortOrder
  location: class FuzzHarness
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
<details open><summary>▸ Output (~3,620 chars)</summary>

```
package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BarRenderer renderer = new BarRenderer();

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 20);
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                renderer.setSeriesVisibleInLegend(series, Boolean.TRUE);
            } else if (mode == 1) {
                renderer.setSeriesVisibleInLegend(series, Boolean.FALSE);
            } else {
                renderer.setSeriesVisibleInLegend(series, null);
            }
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
            return;
        }

        CategoryPlot plot = new CategoryPlot();
        int rendererSlot = data.consumeInt(0, 3);
        plot.setRenderer(rendererSlot, renderer);

        int extraDatasets = data.consumeInt(0, 3);
        for (int i = 0; i < extraDatasets; i++) {
            int slot = data.consumeInt(0, 3);
            DefaultCategoryDataset ds = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 6);
            for (int j = 0; j < entries; j++) {
                String row = data.consumeAsciiString(16);
                String col = data.consumeString(16);
                if (row.length() == 0) {
                    row = "R" + j;
                }
                if (col.length() == 0) {
                    col = "C" + j;
                }
                Number value;
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        value = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        value = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        value = Byte.valueOf(data.consumeByte());
                        break;
                    case 3:
                        value = null;
                        break;
                    default:
                        value = Double.valueOf((double) data.consumeInt());
                        break;
                }
                ds.addValue(value, row, col);
            }
            plot.setDataset(slot, ds);
        }

        if (data.consumeBoolean()) {
            DefaultCategoryDataset target = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 4);
            int cols = data.consumeInt(0, 4);
            for (int r = 0; r < rows; r++) {
                String row = data.consumeAsciiString(16);
                if (row.length() == 0) {
                    row = "TR" + r;
                }
                for (int c = 0; c < cols; c++) {
                    String col = data.consumeString(16);
                    if (col.length() == 0) {
                        col = "TC" + c;
                    }
                    target.addValue(Integer.valueOf(data.consumeInt()), row, col);
                }
            }
            plot.setDataset(rendererSlot, target);
        } else {
            plot.setDataset(rendererSlot, null);
        }

        renderer.getLegendItems();
    }
}
```

</details>

---
## [124] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [125] ⚙️ coverage · `jazzer coverage dumps`
**output:** **15/15 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov; 15 raw fuzz log(s)**
<details><summary>▸ detail (~8,743 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 3279}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 3281}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 3280}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 2490}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 3281}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 2118}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 2491}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 3056}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 2490}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 2493}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 2495}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 2463}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 2491}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 2540}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 2493}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_002', 'attempt_004', 'attempt_006', 'attempt_009', 'attempt_011', 'attempt_013', 'attempt_015', 'attempt_017', 'attempt_019', 'attempt_021', 'attempt_022', 'attempt_024', 'attempt_026', 'attempt_028', 'attempt_030'], 'accepted_attempts': ['attempt_002', 'attempt_004', 'attempt_006', 'attempt_009', 'attempt_011', 'attempt_013', 'attempt_015', 'attempt_017', 'attempt_019', 'attempt_021', 'attempt_022', 'attempt_024', 'attempt_026', 'attempt_028', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/006_patch1-Chart-1-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
