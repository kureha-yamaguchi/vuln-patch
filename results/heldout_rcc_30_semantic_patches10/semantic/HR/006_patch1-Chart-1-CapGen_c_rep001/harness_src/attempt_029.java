package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted from AbstractCategoryItemRendererTests.test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection prePlot = r.getLegendItems();
        if (prePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-notnull-preplot] semantic mismatch: getLegendItems() returned null before plot assignment");
        }

        // Lifted from AbstractCategoryItemRendererTests.test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (prePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-count] semantic mismatch: expected=0 actual=" + prePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        // Lifted from AbstractCategoryItemRendererTests.test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        String seedSeries = "S1";
        String seedColumn = "C1";
        dataset.addValue(1.0, seedSeries, seedColumn);
        LegendItemCollection lic = r.getLegendItems();

        // Lifted from AbstractCategoryItemRendererTests.test2947660:
        // assertEquals(1, lic.getItemCount());
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-unique] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }

        // Lifted from AbstractCategoryItemRendererTests.test2947660:
        // assertEquals("S1", lic.get(0).getLabel());
        LegendItem first = lic.get(0);
        String firstLabel = first == null ? null : first.getLabel();
        if (!seedSeries.equals(firstLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-unique] semantic mismatch: expected=S1 actual=" + firstLabel);
        }

        // Mandatory additional consistency check, using real library calls on both sides.
        // Contract basis:
        // - getLegendItems() returns "a (possibly empty) collection of legend items for the series that this renderer is responsible for drawing."
        // - getLegendItem(int datasetIndex, int series) returns the legend item for a specific series.
        // Therefore, when all series are visible, the collection count must equal the number of non-null
        // direct legend items obtainable by enumerating the dataset's series in the renderer's plot slot.
        // A band-aid patch that suppresses or short-circuits collection population can still leave direct
        // per-series legend item generation correct, and this cross-check would catch that disagreement.
        try {
            int fuzzSeriesCount = data.remainingBytes() > 0 ? data.consumeInt(1, 6) : 1;
            for (int s = 0; s < fuzzSeriesCount; s++) {
                String row = data.consumeAsciiString(8);
                if (row.length() == 0) {
                    row = "R" + s;
                }
                String col = data.consumeAsciiString(8);
                if (col.length() == 0) {
                    col = "C" + s;
                }
                int value = data.consumeInt(-1000, 1000);
                dataset.addValue(value, row, col);
            }

            LegendItemCollection collection = r.getLegendItems();
            int datasetIndex = plot.getIndexOf(r);
            int manualCount = 0;
            for (int s = 0; s < dataset.getRowCount(); s++) {
                LegendItem direct = r.getLegendItem(datasetIndex, s);
                if (direct != null) {
                    manualCount++;
                }
            }
            if (collection.getItemCount() != manualCount) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:collection-vs-direct-manual-count] consistency violation: collectionCount="
                        + collection.getItemCount() + " manualCount=" + manualCount
                        + " datasetRowCount=" + dataset.getRowCount() + " datasetIndex=" + datasetIndex);
            }

            // Additional independent consistency check not based on a hard-coded expected value:
            // getItemCount() must agree with empirical retrievability via get(i): exactly indices
            // [0, getItemCount()-1] should succeed, and get(getItemCount()) should not.
            int retrievable = 0;
            while (true) {
                try {
                    LegendItem item = collection.get(retrievable);
                    if (item == null) {
                        break;
                    }
                    retrievable++;
                    if (retrievable > collection.getItemCount() + 2) {
                        break;
                    }
                } catch (RuntimeException ex) {
                    break;
                }
            }
            if (retrievable != collection.getItemCount()) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:count-vs-retrievable-span] consistency violation: reportedCount="
                        + collection.getItemCount() + " retrievableByIndex=" + retrievable);
            }
        } catch (RuntimeException ex) {
            if (ex instanceof FuzzerSecurityIssueLow) {
                throw ex;
            }
            return;
        }
    }
}