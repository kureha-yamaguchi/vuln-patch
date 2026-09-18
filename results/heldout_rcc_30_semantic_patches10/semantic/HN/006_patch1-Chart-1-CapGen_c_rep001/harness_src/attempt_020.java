package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r0;
        try {
            r0 = new LineAndShapeRenderer();
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection itemsNoPlot;
        try {
            itemsNoPlot = r0.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (itemsNoPlot == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-no-plot-not-null] semantic mismatch: expected non-null legend item collection but got null");
        }
        if (itemsNoPlot.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-no-plot-empty] semantic mismatch: expected 0 but got " + itemsNoPlot.getItemCount());
        }

        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S1";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C1";
        }
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        try {
            r = new LineAndShapeRenderer();
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);
        } catch (Throwable t) {
            return;
        }

        // Shared-state agreement check: setRenderer(plot) establishes the renderer's plot,
        // and getPlot() is documented to report "the plot that the renderer has been assigned to".
        if (r.getPlot() != plot) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:plot-assignment] semantic mismatch: expected renderer plot identity to equal assigned plot");
        }

        LegendItemCollection itemsEmptyDataset;
        try {
            itemsEmptyDataset = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (itemsEmptyDataset == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-empty-dataset-not-null] semantic mismatch: expected non-null legend item collection but got null");
        }
        if (itemsEmptyDataset.getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-empty-dataset-empty] semantic mismatch: expected 0 but got " + itemsEmptyDataset.getItemCount());
        }

        try {
            dataset.addValue(new Double(value), rowKey, columnKey);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection itemsSingleSeries;
        CategoryPlot plotBefore;
        try {
            plotBefore = r.getPlot();
            itemsSingleSeries = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (itemsSingleSeries == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-single-series-not-null] semantic mismatch: expected non-null legend item collection but got null");
        }
        if (itemsSingleSeries.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but got " + itemsSingleSeries.getItemCount());
        }

        String actualLabel;
        try {
            actualLabel = itemsSingleSeries.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        if (!"S1".equals("S1")) {
            return;
        }
        if (!rowKey.equals(actualLabel)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:test2947660-single-series-label] semantic mismatch: expected " + rowKey + " but got " + actualLabel);
        }

        // Post-condition / hidden-state check: getLegendItems() is a getter that reports legend items.
        // A throw-deleting or guard-only patch that skips intended work must still not silently mutate the
        // renderer's assigned plot; getPlot() must agree before and after this read-only call.
        CategoryPlot plotAfter = r.getPlot();
        if (plotBefore != plotAfter || plotAfter != plot) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getLegendItems-readonly-plot] semantic mismatch: getLegendItems changed or lost renderer plot association");
        }

        // Metamorphic/post-condition check: for the same one-series dataset, repeated reads must agree.
        // getLegendItems() computes a collection from current plot/dataset state; without intervening mutation,
        // item count and first label must remain the same across calls.
        LegendItemCollection itemsAgain;
        try {
            itemsAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsAgain == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getLegendItems-idempotent-not-null] semantic mismatch: repeated getLegendItems returned null");
        }
        if (itemsAgain.getItemCount() != itemsSingleSeries.getItemCount()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getLegendItems-idempotent-count] metamorphic violation: repeated getLegendItems disagreed on count lhs="
                    + itemsSingleSeries.getItemCount() + " rhs=" + itemsAgain.getItemCount());
        }
        if (itemsAgain.getItemCount() > 0) {
            String labelAgain;
            try {
                labelAgain = itemsAgain.get(0).getLabel();
            } catch (Throwable t) {
                return;
            }
            if ((actualLabel == null && labelAgain != null) || (actualLabel != null && !actualLabel.equals(labelAgain))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getLegendItems-idempotent-label] metamorphic violation: repeated getLegendItems disagreed on first label lhs="
                        + actualLabel + " rhs=" + labelAgain);
            }
        }
    }
}