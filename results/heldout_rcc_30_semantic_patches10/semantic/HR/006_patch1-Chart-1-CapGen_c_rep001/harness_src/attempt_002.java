package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracles();
        runFlipConditionBoundaryOracle(data);
    }

    private static void runLiftedOracles() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection beforePlot = r.getLegendItems();
        if (beforePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-baseline] semantic mismatch: getLegendItems() returned null before plot assignment");
        }
        if (beforePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pre-plot-zero] semantic mismatch: expected=0 actual=" + beforePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyDatasetItems = r.getLegendItems();
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-zero] semantic mismatch: expected=0 actual=" + emptyDatasetItems.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-count-groundtruth] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        LegendItem first = lic.get(0);
        String actualLabel = first == null ? null : first.getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-label-groundtruth] semantic mismatch: expected=S1 actual=" + actualLabel);
        }
    }

    private static void runFlipConditionBoundaryOracle(FuzzedDataProvider data) {
        try {
            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(renderer);

            if (renderer.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-plot-reference] semantic mismatch: renderer plot reference not preserved");
            }

            LegendItemCollection nullDatasetItems = renderer.getLegendItems();
            if (nullDatasetItems == null) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-null-dataset-not-null] semantic mismatch: getLegendItems() returned null for null dataset");
            }
            if (nullDatasetItems.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-null-dataset-zero] semantic mismatch: expected=0 actual=" + nullDatasetItems.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            plot.setDataset(dataset);

            int seriesCount = data.consumeInt(1, 4);
            String columnKey = nonEmpty(data.consumeAsciiString(8), "C");
            for (int i = 0; i < seriesCount; i++) {
                String rowKey = nonEmpty(data.consumeAsciiString(8), "S" + i) + "_" + i;
                double value = data.consumeInt(-1000, 1000);
                dataset.addValue(value, rowKey, columnKey);
                renderer.setSeriesVisible(i, Boolean.TRUE, false);
                renderer.setSeriesVisibleInLegend(i, Boolean.TRUE);
            }

            int datasetIndex = plot.getIndexOf(renderer);
            LegendItemCollection actual = renderer.getLegendItems();

            // Contract/consistency oracle: getLegendItems() returns the legend items
            // "for the series that this renderer is responsible for drawing".
            // Independently recompute that same collection membership through the
            // real helper path getLegendItem(index, i) for every series in the plot's
            // dataset. A band-aid patch that merely changes reachability or returns
            // early will break this agreement even if it avoids the known symptom.
            int expectedCount = 0;
            for (int i = 0; i < dataset.getRowCount(); i++) {
                if (renderer.isSeriesVisibleInLegend(i)) {
                    LegendItem item = renderer.getLegendItem(datasetIndex, i);
                    if (item != null) {
                        expectedCount++;
                    }
                }
            }
            if (actual.getItemCount() != expectedCount) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-manual-recompute-count] consistency violation: datasetIndex=" + datasetIndex + " rowCount=" + dataset.getRowCount() + " expected=" + expectedCount + " actual=" + actual.getItemCount());
            }

            for (int i = 0; i < expectedCount; i++) {
                LegendItem fromCollection = actual.get(i);
                LegendItem manual = renderer.getLegendItem(datasetIndex, i);
                String collectionLabel = fromCollection == null ? null : fromCollection.getLabel();
                String manualLabel = manual == null ? null : manual.getLabel();
                if (manual != null && !safeEquals(collectionLabel, manualLabel)) {
                    throw new FuzzerSecurityIssueLow("[oracle:flip-manual-recompute-label] consistency violation: index=" + i + " expected=" + manualLabel + " actual=" + collectionLabel);
                }
                if (fromCollection != null) {
                    if (fromCollection.getDataset() != dataset) {
                        throw new FuzzerSecurityIssueLow("[oracle:flip-legend-dataset-reference] semantic mismatch: legend item dataset reference diverged");
                    }
                    if (fromCollection.getDatasetIndex() != datasetIndex) {
                        throw new FuzzerSecurityIssueLow("[oracle:flip-legend-dataset-index] semantic mismatch: expected=" + datasetIndex + " actual=" + fromCollection.getDatasetIndex());
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }

    private static String nonEmpty(String s, String fallback) {
        return (s == null || s.length() == 0) ? fallback : s;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}