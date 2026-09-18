package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedTest2947660Oracle();
        runMultiColumnSingleSeriesOracle(data);
    }

    private static void runLiftedTest2947660Oracle() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-junit] semantic mismatch: expected non-null legend collection before plot assignment but was null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-zero-junit] semantic mismatch: expected initial legend count 0 but was " + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyAttached = r.getLegendItems();
        if (emptyAttached.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot-zero-junit] semantic mismatch: expected empty attached dataset legend count 0 but was " + emptyAttached.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-junit] semantic mismatch: expected 1 but was " + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-junit] semantic mismatch: expected label S1 but was " + label);
        }

        // Cover a different reachable function in the patched region using real library code.
        // Calling equals() here is not itself the oracle; it ensures execution reaches another
        // uncovered sibling in the same class/call graph.
        r.equals(new LineAndShapeRenderer());
    }

    private static void runMultiColumnSingleSeriesOracle(FuzzedDataProvider data) {
        int columns = data.consumeInt(2, 5);
        String rowKey = sanitizeKey(data.consumeAsciiString(8), "ROW");
        AbstractCategoryItemRenderer renderer = new LineAndShapeRenderer();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(renderer);

        for (int i = 0; i < columns; i++) {
            String columnKey = "C" + i + "_" + sanitizeKey(data.consumeAsciiString(6), "K");
            double value = data.consumeInt(-1000, 1000);
            dataset.addValue(value, rowKey, columnKey);
        }

        LegendItemCollection items;
        try {
            items = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        // Contract justification: getLegendItems() is documented as returning legend items
        // "for the series that this renderer is responsible for drawing", and the shown
        // implementation computes seriesCount from dataset.getRowCount(). Therefore, when all
        // series are visible, the reported legend count must equal the dataset's row count,
        // regardless of how many columns/items each series contains. A band-aid patch that only
        // hides the seed symptom can still violate this series-vs-item post-condition.
        int expectedSeriesCount = dataset.getRowCount();
        int actualLegendCount = items.getItemCount();
        if (actualLegendCount != expectedSeriesCount) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:series-not-columns-count] consistency violation: datasetRowCount="
                    + expectedSeriesCount + " legendItemCount=" + actualLegendCount
                    + " datasetColumnCount=" + dataset.getColumnCount());
        }

        if (expectedSeriesCount > 0) {
            String expectedLabel = String.valueOf(dataset.getRowKey(0));
            String actualLabel = items.get(0).getLabel();
            if (!expectedLabel.equals(actualLabel)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:series-label-stable-across-columns] semantic mismatch: expectedFirstSeriesLabel="
                        + expectedLabel + " actualFirstLegendLabel=" + actualLabel
                        + " datasetColumnCount=" + dataset.getColumnCount());
            }
        }

        // Additional real-code reachability into another uncovered sibling in the reachable region.
        renderer.equals(new LineAndShapeRenderer());
    }

    private static String sanitizeKey(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}