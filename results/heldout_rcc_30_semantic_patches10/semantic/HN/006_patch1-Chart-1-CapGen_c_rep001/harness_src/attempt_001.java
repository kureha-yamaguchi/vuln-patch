package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    private static String nonEmptyAscii(FuzzedDataProvider data, String fallback) {
        String s = data.consumeAsciiString(8);
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r1;
        LegendItemCollection noPlotItems;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection singleSeriesItems;
        CategoryPlot observedPlotAfterAttach;
        CategoryPlot observedPlotAfterReads;
        String firstLabel;

        try {
            r1 = new LineAndShapeRenderer();

            noPlotItems = r1.getLegendItems();

            plot1 = new CategoryPlot();
            dataset1 = new DefaultCategoryDataset();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);

            observedPlotAfterAttach = r1.getPlot();
            emptyDatasetItems = r1.getLegendItems();

            dataset1.addValue(1.0, "S1", "C1");
            singleSeriesItems = r1.getLegendItems();
            observedPlotAfterReads = r1.getPlot();
            firstLabel = singleSeriesItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null-no-plot] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-no-plot] semantic mismatch: expected 0 but actual was " + noPlotItems.getItemCount());
        }
        if (observedPlotAfterAttach != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree] semantic mismatch: setRenderer/setPlot established plot identity, but getPlot() returned a different object");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected 0 but actual was " + emptyDatasetItems.getItemCount());
        }
        if (singleSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but actual was " + singleSeriesItems.getItemCount());
        }
        if (!"S1".equals(firstLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-label] semantic mismatch: expected S1 but actual was " + firstLabel);
        }
        /* Contract justification: getLegendItems() is a read/query method ("Returns the legend items...")
           over renderer/plot state, so repeated reads on unchanged state must not silently detach the plot.
           A throw-deleting or branch-skipping patch that corrupts/ignores state would violate this. */
        if (observedPlotAfterReads != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-plot-stable] semantic mismatch: getLegendItems() changed observable plot state");
        }

        String rowKey = nonEmptyAscii(data, "S");
        String columnKey = nonEmptyAscii(data, "C");
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection itemsA;
        LegendItemCollection itemsB;
        CategoryPlot beforeReadPlot;
        CategoryPlot afterReadPlot;
        String labelA;
        String labelB;

        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            beforeReadPlot = r2.getPlot();

            dataset2.addValue(value, rowKey, columnKey);

            itemsA = r2.getLegendItems();
            itemsB = r2.getLegendItems();
            afterReadPlot = r2.getPlot();
            labelA = itemsA.get(0).getLabel();
            labelB = itemsB.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: for a real LineAndShapeRenderer attached to a plot with a non-null dataset
           containing exactly one series, getLegendItems() must report that one series; this is fixed by the
           lifted JUnit oracle and generalised by constructing the single known series ourselves. */
        if (itemsA == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-null] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (itemsA.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-count] semantic mismatch: expected 1 but actual was " + itemsA.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (!rowKey.equals(labelA)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-label] semantic mismatch: expected " + rowKey + " but actual was " + labelA + " columnKey=" + columnKey);
        }

        /* Metamorphic/post-condition justification: getLegendItems() is a read-only query over unchanged state,
           so two consecutive calls must agree on the observable legend count/label, and the plot written by
           CategoryPlot.setRenderer(r) must still be returned by getPlot(). */
        if (itemsB == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-null] semantic mismatch: second getLegendItems() returned null");
        }
        if (itemsB.getItemCount() != itemsA.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-count] metamorphic violation: repeated getLegendItems() on unchanged state disagreed: first=" + itemsA.getItemCount() + " second=" + itemsB.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (!labelA.equals(labelB)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotent-label] metamorphic violation: repeated getLegendItems() on unchanged state disagreed: first=" + labelA + " second=" + labelB + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (beforeReadPlot != plot2 || afterReadPlot != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree-generalized] metamorphic violation: getPlot()/setPlot state disagreed with attached plot");
        }

        LineAndShapeRenderer r3;
        LegendItemCollection noPlotGeneral;
        try {
            r3 = new LineAndShapeRenderer();
            noPlotGeneral = r3.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: without an assigned plot there are no series this renderer is responsible for,
           so getLegendItems() must return a non-null empty collection. */
        if (noPlotGeneral == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-emptyAndNonNull-null] semantic mismatch: expected non-null legend collection but actual was null");
        }
        if (noPlotGeneral.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-emptyAndNonNull-count] semantic mismatch: expected 0 but actual was " + noPlotGeneral.getItemCount());
        }
    }
}