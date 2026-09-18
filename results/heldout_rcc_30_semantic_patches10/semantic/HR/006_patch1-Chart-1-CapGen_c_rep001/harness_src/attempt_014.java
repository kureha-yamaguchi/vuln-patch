package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedGroundTruthOracle();
        runFlippedBoundaryAndGeneratorOracle(data);
    }

    private static void runLiftedGroundTruthOracle() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-groundtruth-notnull] semantic mismatch: expected non-null legend item collection before plot assignment, actual=null");
        }

        int initialCount = r.getLegendItems().getItemCount();
        if (initialCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-groundtruth-initial-count] semantic mismatch: expected=0 actual=" + initialCount);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        int emptyPlotCount = r.getLegendItems().getItemCount();
        if (emptyPlotCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-groundtruth-empty-count] semantic mismatch: expected=0 actual=" + emptyPlotCount);
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        int singleCount = lic.getItemCount();
        if (singleCount != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-groundtruth-single-count] semantic mismatch: expected=1 actual=" + singleCount);
        }

        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-groundtruth-single-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }
    }

    private static void runFlippedBoundaryAndGeneratorOracle(FuzzedDataProvider data) {
        try {
            String seriesKey = data.consumeAsciiString(8);
            if (seriesKey == null || seriesKey.length() == 0) {
                seriesKey = "S";
            }
            String columnKey = data.consumeAsciiString(8);
            if (columnKey == null || columnKey.length() == 0) {
                columnKey = "C";
            }
            double value = data.consumeInt(-1000, 1000);

            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(r);

            plot.setDataset((CategoryDataset) null);
            LegendItemCollection whenNull = r.getLegendItems();
            int nullCount = whenNull.getItemCount();
            if (nullCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-null-nonnull-boundary] semantic mismatch: expected count 0 when renderer plot slot dataset is null, actual=" + nullCount);
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            plot.setDataset(dataset);
            LegendItemCollection whenEmpty = r.getLegendItems();
            int emptyCount = whenEmpty.getItemCount();
            if (emptyCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-empty-boundary] semantic mismatch: expected count 0 for empty dataset, actual=" + emptyCount);
            }

            dataset.addValue(value, seriesKey, columnKey);
            LegendItemCollection whenOneSeries = r.getLegendItems();
            int oneCount = whenOneSeries.getItemCount();
            if (oneCount != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-single-series-count] semantic mismatch: expected count 1 after adding one series, actual=" + oneCount);
            }

            LegendItem item = whenOneSeries.get(0);
            String expectedLabel = r.getLegendItemLabelGenerator().generateLabel(dataset, 0);
            String actualLabel = item.getLabel();
            if (expectedLabel == null ? actualLabel != null : !expectedLabel.equals(actualLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:generator-label-agreement] metamorphic violation: legend item label must match renderer legend label generator output for the same dataset/series expected=" + String.valueOf(expectedLabel) + " actual=" + String.valueOf(actualLabel));
            }

            if (item.getDataset() != dataset) {
                throw new FuzzerSecurityIssueLow("[oracle:generator-dataset-identity] consistency violation: legend item dataset must be the same dataset instance used to generate the legend item expectedSameInstance=true actualSameInstance=false");
            }

            plot.setDataset((CategoryDataset) null);
            LegendItemCollection afterFlipBack = r.getLegendItems();
            int afterFlipBackCount = afterFlipBack.getItemCount();
            if (afterFlipBackCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:flip-back-to-null] semantic mismatch: expected count 0 after flipping dataset back to null, actual=" + afterFlipBackCount);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}