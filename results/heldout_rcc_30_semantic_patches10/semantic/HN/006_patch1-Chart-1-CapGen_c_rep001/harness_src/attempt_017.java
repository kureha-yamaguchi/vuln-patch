package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted oracle from AbstractCategoryItemRendererTests.test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection initialLegend = r.getLegendItems();
        if (initialLegend == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: getLegendItems() returned null before plot assignment");
        }

        // Lifted oracle from the same test:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (initialLegend.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-count] semantic mismatch: expected=0 actual=" + initialLegend.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state oracle on the same field ("plot"):
        // getPlot() must report the plot established by normal construction/wiring.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: renderer plot disagrees with plot.setRenderer() wiring");
        }

        // Lifted oracle from the same test:
        // assertEquals(0, r.getLegendItems().getItemCount());
        LegendItemCollection emptyDatasetLegend = r.getLegendItems();
        if (emptyDatasetLegend == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-not-null] semantic mismatch: getLegendItems() returned null for empty dataset");
        }
        if (emptyDatasetLegend.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-count] semantic mismatch: expected=0 actual=" + emptyDatasetLegend.getItemCount());
        }

        // Generalisation by construction from a known answer:
        // we choose the row key first, then build the dataset around it.
        // The lifted test shows the legend label is the series key ("S1"), so for a
        // one-series dataset the correct legend label is the row key we inserted.
        String chosenSeries = data.consumeAsciiString(16);
        if (chosenSeries == null || chosenSeries.length() == 0) {
            chosenSeries = "S1";
        }
        String chosenCategory = data.consumeAsciiString(16);
        if (chosenCategory == null || chosenCategory.length() == 0) {
            chosenCategory = "C1";
        }
        int value = data.consumeInt(-1000, 1000);

        dataset.addValue((double) value, chosenSeries, chosenCategory);

        // Lifted oracle from the same test, reconstructed on the real API path:
        // LegendItemCollection lic = r.getLegendItems();
        // assertEquals(1, lic.getItemCount());
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-not-null] semantic mismatch: getLegendItems() returned null for one-series dataset");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }

        // Lifted oracle adapted by construction from a known answer:
        // test hard-codes assertEquals("S1", lic.get(0).getLabel()) after inserting row key "S1".
        // Therefore when we insert a one-series dataset with row key chosenSeries, the label must equal chosenSeries.
        String actualLabel = lic.get(0).getLabel();
        if (!chosenSeries.equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label] semantic mismatch: expected=" + chosenSeries + " actual=" + actualLabel);
        }

        // Mandatory post-condition / metamorphic check:
        // Contract observed in getLegendItems(): only the row rendering order changes iteration order.
        // With exactly one visible series, ASCENDING and DESCENDING must yield the same single legend item.
        // This catches a "fix" that merely suppresses the bad branch or silently returns the wrong collection.
        try {
            plot.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection asc = r.getLegendItems();
            plot.setRowRenderingOrder(SortOrder.DESCENDING);
            LegendItemCollection desc = r.getLegendItems();

            if (asc == null || desc == null) {
                return;
            }
            if (asc.getItemCount() != 1 || desc.getItemCount() != 1) {
                throw new RuntimeException("[oracle:single-series-order] metamorphic violation: one-series legend count must stay 1 across row rendering orders ascCount="
                        + asc.getItemCount() + " descCount=" + desc.getItemCount());
            }
            String ascLabel = asc.get(0).getLabel();
            String descLabel = desc.get(0).getLabel();
            if (!chosenSeries.equals(ascLabel) || !chosenSeries.equals(descLabel)) {
                throw new RuntimeException("[oracle:single-series-order] metamorphic violation: one-series legend label must equal the only series key input="
                        + chosenSeries + " ascLabel=" + ascLabel + " descLabel=" + descLabel);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}