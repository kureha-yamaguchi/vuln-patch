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