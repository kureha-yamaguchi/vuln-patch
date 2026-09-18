package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer exactRenderer;
        LegendItemCollection initialItems;
        LegendItemCollection emptyDatasetItems;
        LegendItemCollection oneSeriesItems;

        try {
            exactRenderer = new LineAndShapeRenderer();

            initialItems = exactRenderer.getLegendItems();

            DefaultCategoryDataset exactDataset = new DefaultCategoryDataset();
            CategoryPlot exactPlot = new CategoryPlot();
            exactPlot.setDataset(exactDataset);
            exactPlot.setRenderer(exactRenderer);

            emptyDatasetItems = exactRenderer.getLegendItems();

            exactDataset.addValue(1.0, "S1", "C1");
            oneSeriesItems = exactRenderer.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (initialItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (initialItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initialItems.getItemCount());
        }
        if (emptyDatasetItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (emptyDatasetItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-empty] semantic mismatch: expected=0 actual=" + emptyDatasetItems.getItemCount());
        }
        if (oneSeriesItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (oneSeriesItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-count] semantic mismatch: expected=1 actual=" + oneSeriesItems.getItemCount());
        }
        String exactLabel = oneSeriesItems.get(0).getLabel();
        if (!"S1".equals(exactLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-label] semantic mismatch: expected=S1 actual=" + exactLabel);
        }

        LineAndShapeRenderer stateRenderer;
        CategoryPlot statePlot;
        LegendItemCollection stateItems1;
        LegendItemCollection stateItems2;
        int beforeRows;
        int afterRows;
        String stateLabel1;
        String stateLabel2;
        try {
            stateRenderer = new LineAndShapeRenderer();
            statePlot = new CategoryPlot();
            DefaultCategoryDataset stateDataset = new DefaultCategoryDataset();
            statePlot.setDataset(stateDataset);
            statePlot.setRenderer(stateRenderer);

            String rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            String columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }
            double value = data.consumeInt(-1000, 1000);

            stateDataset.addValue(value, rowKey, columnKey);
            beforeRows = stateDataset.getRowCount();
            stateItems1 = stateRenderer.getLegendItems();
            afterRows = stateDataset.getRowCount();
            stateItems2 = stateRenderer.getLegendItems();
            stateLabel1 = stateItems1 != null && stateItems1.getItemCount() > 0 ? stateItems1.get(0).getLabel() : null;
            stateLabel2 = stateItems2 != null && stateItems2.getItemCount() > 0 ? stateItems2.get(0).getLabel() : null;

            if (stateRenderer.getPlot() != statePlot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state-coupling] semantic mismatch: setRenderer/setPlot established plot=" + statePlot + " but getPlot returned=" + stateRenderer.getPlot());
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (stateItems1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (stateItems1.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-count] semantic mismatch: expected=1 actual=" + stateItems1.getItemCount());
        }
        if (stateLabel1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-singleSeries-nonNullDataset-label-null] semantic mismatch: expected non-null label but got null");
        }
        if (!stateLabel1.equals(stateLabel2)) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset must agree on first label lhs=" + stateLabel1 + " rhs=" + stateLabel2);
        }
        if (stateItems2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotence-not-null] semantic mismatch: second getLegendItems() returned null");
        }
        if (stateItems2.getItemCount() != stateItems1.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-idempotence-count] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset must agree on item count lhs=" + stateItems1.getItemCount() + " rhs=" + stateItems2.getItemCount());
        }
        if (beforeRows != afterRows) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-readonly-dataset] metamorphic violation: getLegendItems() is a query and should not change dataset row count before=" + beforeRows + " after=" + afterRows);
        }

        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-not-null] semantic mismatch: expected non-null legend items but got null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:legendItems-noPlot-empty] semantic mismatch: expected=0 actual=" + noPlotItems.getItemCount());
        }
    }
}