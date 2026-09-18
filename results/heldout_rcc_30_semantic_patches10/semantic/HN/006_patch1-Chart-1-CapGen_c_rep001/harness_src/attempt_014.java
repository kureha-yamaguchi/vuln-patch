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