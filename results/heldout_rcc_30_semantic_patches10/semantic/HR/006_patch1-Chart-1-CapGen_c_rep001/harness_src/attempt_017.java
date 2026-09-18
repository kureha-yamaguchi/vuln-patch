package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedAssertions(data);
        runFlipPatchedConditionOnNonzeroSlot(data);
    }

    private static void runLiftedAssertions(FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r;
        LegendItemCollection prePlot;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        LegendItemCollection emptyPlotItems;
        LegendItemCollection singleSeriesItems;

        try {
            r = new LineAndShapeRenderer();
            prePlot = r.getLegendItems();
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);
            emptyPlotItems = r.getLegendItems();
            dataset.addValue(1.0, "S1", "C1");
            singleSeriesItems = r.getLegendItems();
        } catch (Exception e) {
            return;
        }

        if (prePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-not-null] semantic mismatch: expected non-null legend collection before assigning plot");
        }
        if (prePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-zero] semantic mismatch: expected 0 legend items before assigning plot but got " + prePlot.getItemCount());
        }
        if (emptyPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-zero] semantic mismatch: expected 0 legend items for empty dataset but got " + emptyPlotItems.getItemCount());
        }
        if (singleSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got " + singleSeriesItems.getItemCount());
        }
        if (!"S1".equals(singleSeriesItems.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label] semantic mismatch: expected first legend label S1 but got " + singleSeriesItems.get(0).getLabel());
        }
    }

    private static void runFlipPatchedConditionOnNonzeroSlot(FuzzedDataProvider data) {
        String rowA = nonEmptyAscii(data, 6, "A");
        String rowB = distinctNonEmptyAscii(data, 6, "B", rowA);
        String col = nonEmptyAscii(data, 6, "C");
        int v1 = data.consumeInt(-1000, 1000);
        int v2 = data.consumeInt(-1000, 1000);
        boolean descending = data.consumeBoolean();

        AreaRenderer renderer;
        CategoryPlot plot;
        DefaultCategoryDataset dataset0;
        DefaultCategoryDataset dataset1;
        LegendItemCollection items;
        int rendererIndex;
        SortOrder order = descending ? SortOrder.DESCENDING : SortOrder.ASCENDING;

        try {
            renderer = new AreaRenderer();
            plot = new CategoryPlot();
            dataset0 = new DefaultCategoryDataset();
            dataset1 = new DefaultCategoryDataset();

            dataset1.addValue(v1, rowA, col);
            dataset1.addValue(v2, rowB, col);

            plot.setDataset(0, dataset0);
            plot.setRenderer(0, new LineAndShapeRenderer());
            plot.setDataset(1, dataset1);
            plot.setRenderer(1, renderer);
            plot.setRowRenderingOrder(order);

            rendererIndex = plot.getIndexOf(renderer);
            items = renderer.getLegendItems();
        } catch (Exception e) {
            return;
        }

        if (rendererIndex != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-renderer-index] semantic mismatch: expected plot.getIndexOf(renderer) == 1 but got " + rendererIndex);
        }

        /* Contract justification:
         * getLegendItems() iterates over the dataset returned by plot.getDataset(plot.getIndexOf(this)).
         * We deliberately flip the patched condition boundary by placing this renderer in slot 1 while slot 0 also exists.
         * A band-aid fix that only handles the seed arrangement can still return an empty collection here.
         * Because both series are default-visible and dataset1 has exactly two rows, the legend must expose both rows in the plot's row rendering order.
         */
        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-area-not-null] semantic mismatch: expected non-null legend collection for slot-1 renderer");
        }
        if (items.getItemCount() != 2) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-area-count] semantic mismatch: expected 2 legend items for slot-1 renderer with two dataset rows but got " + items.getItemCount());
        }

        String expectedFirst = descending ? rowB : rowA;
        String expectedSecond = descending ? rowA : rowB;
        String actualFirst = items.get(0).getLabel();
        String actualSecond = items.get(1).getLabel();

        if (!expectedFirst.equals(actualFirst)) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-area-order-first] metamorphic violation: row rendering order=" + order + " expected first label=" + expectedFirst + " actualFirst=" + actualFirst);
        }
        if (!expectedSecond.equals(actualSecond)) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-area-order-second] metamorphic violation: row rendering order=" + order + " expected second label=" + expectedSecond + " actualSecond=" + actualSecond);
        }
        if (items.get(0).getDatasetIndex() != rendererIndex || items.get(1).getDatasetIndex() != rendererIndex) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-area-dataset-index] consistency violation: rendererIndex=" + rendererIndex + " item0.datasetIndex=" + items.get(0).getDatasetIndex() + " item1.datasetIndex=" + items.get(1).getDatasetIndex());
        }
        if (items.get(0).getDataset() != dataset1 || items.get(1).getDataset() != dataset1) {
            throw new FuzzerSecurityIssueLow("[oracle:slot1-area-dataset-ref] consistency violation: legend items did not retain the dataset from the renderer's own slot");
        }
    }

    private static String nonEmptyAscii(FuzzedDataProvider data, int maxLen, String fallback) {
        String s = data.consumeAsciiString(Math.max(1, maxLen));
        return (s == null || s.length() == 0) ? fallback : s;
    }

    private static String distinctNonEmptyAscii(FuzzedDataProvider data, int maxLen, String fallback, String other) {
        String s = nonEmptyAscii(data, maxLen, fallback);
        if (s.equals(other)) {
            s = s + "_";
        }
        return s;
    }
}