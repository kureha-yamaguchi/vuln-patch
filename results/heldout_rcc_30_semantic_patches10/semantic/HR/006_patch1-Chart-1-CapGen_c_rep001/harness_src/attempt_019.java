package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.jfree.chart.LegendItemCollection prePlotItems;
        org.jfree.chart.LegendItemCollection emptyDatasetItems;
        org.jfree.chart.LegendItemCollection singleSeriesItems;
        try {
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
            prePlotItems = r.getLegendItems();

            org.jfree.data.category.DefaultCategoryDataset dataset =
                    new org.jfree.data.category.DefaultCategoryDataset();
            org.jfree.chart.plot.CategoryPlot plot =
                    new org.jfree.chart.plot.CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);
            emptyDatasetItems = r.getLegendItems();

            dataset.addValue(1.0, "S1", "C1");
            singleSeriesItems = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (prePlotItems == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-not-null] semantic mismatch: expected non-null legend collection before plot attachment but got null");
        }
        if (prePlotItems.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-preplot-zero] semantic mismatch: expected 0 legend items before plot attachment but got "
                            + prePlotItems.getItemCount());
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-empty-dataset-zero] semantic mismatch: expected 0 legend items for empty attached dataset but got "
                            + emptyDatasetItems.getItemCount());
        }
        if (singleSeriesItems.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-single-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got "
                            + singleSeriesItems.getItemCount());
        }
        String liftedLabel = singleSeriesItems.get(0).getLabel();
        if (!"S1".equals(liftedLabel)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-single-label] semantic mismatch: expected label S1 but got "
                            + liftedLabel);
        }

        String rowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        String colKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        int value = data.consumeInt(-1000, 1000);

        org.jfree.chart.LegendItemCollection barItems;
        org.jfree.chart.LegendItem firstBarItem;
        org.jfree.chart.util.StandardGradientPaintTransformer transformer =
                new org.jfree.chart.util.StandardGradientPaintTransformer();
        try {
            BarRenderer renderer = new BarRenderer();
            renderer.setGradientPaintTransformer(transformer);

            org.jfree.data.category.DefaultCategoryDataset dataset =
                    new org.jfree.data.category.DefaultCategoryDataset();
            dataset.addValue(value, rowKey, colKey);

            org.jfree.chart.plot.CategoryPlot plot =
                    new org.jfree.chart.plot.CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(renderer);

            barItems = renderer.getLegendItems();
            firstBarItem = barItems.get(0);
        } catch (Throwable t) {
            return;
        }

        if (barItems == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:bar-transformer-propagation] consistency violation: getLegendItems() returned null");
        }
        if (barItems.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:bar-transformer-propagation] consistency violation: expected exactly one legend item for one BarRenderer series but got "
                            + barItems.getItemCount());
        }
        if (firstBarItem == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:bar-transformer-propagation] consistency violation: first legend item was null");
        }
        if (firstBarItem.getFillPaintTransformer() != transformer) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:bar-transformer-propagation] consistency violation: BarRenderer.getLegendItem() copies its non-null gradientPaintTransformer into the produced LegendItem, so the legend item's transformer must be the same object; expected "
                            + transformer + " but got " + firstBarItem.getFillPaintTransformer());
        }
        if (!rowKey.equals(firstBarItem.getLabel())) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:bar-transformer-propagation] consistency violation: expected BarRenderer legend label to equal the single dataset row key "
                            + rowKey + " but got " + firstBarItem.getLabel());
        }
    }
}