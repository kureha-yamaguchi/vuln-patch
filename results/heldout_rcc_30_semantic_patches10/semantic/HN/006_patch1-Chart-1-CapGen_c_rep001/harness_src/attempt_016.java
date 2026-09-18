package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

/* Jazzer harness for AbstractCategoryItemRenderer.getLegendItems() semantic bug. */
public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r1;
        LegendItemCollection noPlotItems1;
        LegendItemCollection noPlotItems2;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection oneSeriesItems;
        String fuzzRowKey = data.consumeAsciiString(8);
        String fuzzColumnKey = data.consumeAsciiString(8);
        if (fuzzRowKey.length() == 0) {
            fuzzRowKey = "S";
        }
        if (fuzzColumnKey.length() == 0) {
            fuzzColumnKey = "C";
        }
        Exception setupException = null;
        try {
            r1 = new LineAndShapeRenderer();

            noPlotItems1 = r1.getLegendItems();
            noPlotItems2 = r1.getLegendItems();

            plot1 = new CategoryPlot();
            dataset1 = new DefaultCategoryDataset();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);

            emptyDatasetItems = r1.getLegendItems();

            dataset1.addValue(1.0, "S1", "C1");
            oneSeriesItems = r1.getLegendItems();
        } catch (Exception e) {
            setupException = e;
            r1 = null;
            noPlotItems1 = null;
            noPlotItems2 = null;
            plot1 = null;
            dataset1 = null;
            emptyDatasetItems = null;
            oneSeriesItems = null;
        }
        if (setupException != null) {
            return;
        }

        if (noPlotItems1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-1] semantic mismatch: expected non-null legend items before plot assignment but got null");
        }
        if (noPlotItems1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-before-plot] semantic mismatch: expected 0 but was " + noPlotItems1.getItemCount());
        }
        if (emptyDatasetItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-2] semantic mismatch: expected non-null legend items after empty dataset attachment but got null");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected 0 but was " + emptyDatasetItems.getItemCount());
        }
        if (oneSeriesItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-3] semantic mismatch: expected non-null legend items after adding one series but got null");
        }
        if (oneSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-item] semantic mismatch: expected 1 but was " + oneSeriesItems.getItemCount());
        }
        String actualLabel = oneSeriesItems.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected S1 but was " + actualLabel);
        }

        // Contract check from getPlot()/setPlot(): after plot.setRenderer(r), the renderer has been assigned to that plot.
        // This is a shared-state oracle for the same 'plot' field used by getLegendItems(); a patch that only special-cases output
        // but leaves plot state inconsistent would violate it.
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer.getPlot() to be the assigned plot instance");
        }

        // Read-only hidden-state check: getLegendItems() is documented as returning legend items for the current renderer/plot state;
        // it is a query and should not silently change which plot the renderer is assigned to.
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-readonly-plot] metamorphic violation: getLegendItems() changed renderer plot association");
        }

        LineAndShapeRenderer r2 = null;
        CategoryPlot plot2 = null;
        DefaultCategoryDataset dataset2 = null;
        LegendItemCollection items2 = null;
        Exception relationException = null;
        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(1.0, fuzzRowKey, fuzzColumnKey);
            items2 = r2.getLegendItems();
        } catch (Exception e) {
            relationException = e;
        }
        if (relationException != null) {
            return;
        }

        // Invariant from the failing test generalized by construction:
        // with a non-null dataset containing exactly one series, getLegendItems() must return exactly one legend item for that series.
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items2.getItemCount());
        }
        String label2 = items2.get(0).getLabel();
        if (!fuzzRowKey.equals(label2)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + fuzzRowKey + " but got " + label2);
        }

        // Additional post-condition: the writer-established plot state must agree with the reader after a successful legend query too.
        if (r2.getPlot() != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: expected renderer.getPlot() to remain the assigned plot after getLegendItems()");
        }

        LineAndShapeRenderer r3 = null;
        LegendItemCollection items3a = null;
        LegendItemCollection items3b = null;
        Exception noPlotException = null;
        try {
            r3 = new LineAndShapeRenderer();
            items3a = r3.getLegendItems();
            items3b = r3.getLegendItems();
        } catch (Exception e) {
            noPlotException = e;
        }
        if (noPlotException != null) {
            return;
        }

        // Invariant: without an assigned plot there are no series to report, so the collection is non-null and empty.
        if (items3a == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items3a.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items3a.getItemCount());
        }

        // Idempotence/read-only query check: repeated getLegendItems() on unchanged no-plot state must agree.
        if (items3b == null || items3b.getItemCount() != items3a.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer state disagreed: first="
                    + items3a.getItemCount() + " second=" + (items3b == null ? "null" : String.valueOf(items3b.getItemCount())));
        }
    }
}