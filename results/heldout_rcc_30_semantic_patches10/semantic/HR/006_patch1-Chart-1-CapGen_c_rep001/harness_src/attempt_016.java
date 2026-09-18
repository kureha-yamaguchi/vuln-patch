package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        try {
            runNullValueSeriesMetamorphicOracle(data);
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runNullValueSeriesMetamorphicOracle(FuzzedDataProvider data) {
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();

        try {
            renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        DefaultCategoryDataset numericDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset nullDataset = new DefaultCategoryDataset();
        CategoryPlot numericPlot = new CategoryPlot();
        CategoryPlot nullPlot = new CategoryPlot();

        int seriesCount = data.consumeInt(1, 4);
        int columnCount = data.consumeInt(1, 3);

        boolean anyVisible = false;
        for (int s = 0; s < seriesCount; s++) {
            String rowKey = "S" + s;
            boolean visibleInLegend = data.consumeBoolean();
            renderer.setSeriesVisibleInLegend(s, Boolean.valueOf(visibleInLegend), false);
            renderer.setSeriesVisible(s, Boolean.TRUE, false);
            if (visibleInLegend) {
                anyVisible = true;
            }
            for (int c = 0; c < columnCount; c++) {
                String columnKey = "C" + c;
                int value = data.consumeInt(-1000, 1000);
                numericDataset.setValue(Integer.valueOf(value), rowKey, columnKey);
                nullDataset.setValue(null, rowKey, columnKey);
            }
        }
        if (!anyVisible) {
            renderer.setSeriesVisibleInLegend(0, Boolean.TRUE, false);
            anyVisible = true;
        }

        numericPlot.setDataset(numericDataset);
        numericPlot.setRenderer(renderer);

        LegendItemCollection numericItems;
        try {
            numericItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        numericPlot.setDataset(nullDataset);

        LegendItemCollection nullItems;
        try {
            nullItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        int expectedVisibleSeries = manualVisibleSeriesCount(renderer, nullDataset);
        if (numericItems.getItemCount() != expectedVisibleSeries) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:null-values-manual-count-before] consistency violation: legend item count must equal the number of visible series because getLegendItems() returns items for series, not cells; reported="
                    + numericItems.getItemCount() + " manual=" + expectedVisibleSeries);
        }
        if (nullItems.getItemCount() != expectedVisibleSeries) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:null-values-manual-count-after] consistency violation: legend item count must equal the number of visible series even when item values are null; reported="
                    + nullItems.getItemCount() + " manual=" + expectedVisibleSeries);
        }

        if (numericItems.getItemCount() != nullItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:null-values-count-invariant] metamorphic violation: replacing dataset values with null changed the number of legend items for the same series set; numericCount="
                    + numericItems.getItemCount() + " nullCount=" + nullItems.getItemCount());
        }

        for (int i = 0; i < numericItems.getItemCount(); i++) {
            LegendItem before = numericItems.get(i);
            LegendItem after = nullItems.get(i);
            Object beforeSeriesKey = before.getSeriesKey();
            Object afterSeriesKey = after.getSeriesKey();
            if (beforeSeriesKey == null ? afterSeriesKey != null : !beforeSeriesKey.equals(afterSeriesKey)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:null-values-serieskey-invariant] metamorphic violation: legend series identity changed after replacing dataset values with null; index="
                        + i + " beforeSeriesKey=" + String.valueOf(beforeSeriesKey)
                        + " afterSeriesKey=" + String.valueOf(afterSeriesKey));
            }
            if (before.getSeriesIndex() != after.getSeriesIndex()) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:null-values-seriesindex-invariant] metamorphic violation: legend series index changed after replacing dataset values with null; index="
                        + i + " beforeSeriesIndex=" + before.getSeriesIndex()
                        + " afterSeriesIndex=" + after.getSeriesIndex());
            }
        }

        nullPlot.setDataset(nullDataset);
        LineAndShapeRenderer secondRenderer = new LineAndShapeRenderer();
        for (int s = 0; s < seriesCount; s++) {
            secondRenderer.setSeriesVisibleInLegend(s, renderer.getSeriesVisibleInLegend(s), false);
            secondRenderer.setSeriesVisible(s, renderer.getSeriesVisible(s), false);
        }
        nullPlot.setRenderer(secondRenderer);

        LegendItemCollection secondItems;
        try {
            secondItems = secondRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (secondItems.getItemCount() != nullItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:null-values-fresh-renderer-agreement] consistency violation: two identically configured renderers on the same null-valued dataset must report the same legend item count; first="
                    + nullItems.getItemCount() + " second=" + secondItems.getItemCount());
        }
    }

    private static int manualVisibleSeriesCount(LineAndShapeRenderer renderer, DefaultCategoryDataset dataset) {
        int count = 0;
        int rows = dataset.getRowCount();
        for (int i = 0; i < rows; i++) {
            if (renderer.isSeriesVisibleInLegend(i) && renderer.isSeriesVisible(i)) {
                count++;
            }
        }
        return count;
    }
}