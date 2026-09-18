package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer renderer;
        LegendItemCollection prePlotItems;
        try {
            renderer = new LineAndShapeRenderer();
            prePlotItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (prePlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-preplot-notnull] semantic mismatch: expected non-null legend items before plot assignment but got null");
        }
        if (prePlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-preplot-zero] semantic mismatch: expected 0 legend items before plot assignment but got " + prePlotItems.getItemCount());
        }

        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        LegendItemCollection emptyItems;
        try {
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(renderer);
            emptyItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (emptyItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-empty-notnull] semantic mismatch: expected non-null legend items for empty attached dataset but got null");
        }
        if (emptyItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-empty-zero] semantic mismatch: expected 0 legend items for empty attached dataset but got " + emptyItems.getItemCount());
        }

        String rowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        if (rowKey.length() == 0) {
            rowKey = "S1";
        }
        String colKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        if (colKey.length() == 0) {
            colKey = "C1";
        }
        int value = data.consumeInt(-1000, 1000);

        int indexBefore;
        SortOrder orderBefore;
        try {
            indexBefore = plot.getIndexOf(renderer);
            orderBefore = plot.getRowRenderingOrder();
            dataset.addValue(value, rowKey, colKey);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection afterItems;
        try {
            afterItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (afterItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-single-notnull] semantic mismatch: expected non-null legend items after adding one series but got null");
        }
        if (afterItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-single-count] semantic mismatch: expected 1 legend item after adding one series but got " + afterItems.getItemCount());
        }
        String actualLabel;
        try {
            actualLabel = afterItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!rowKey.equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-single-label] semantic mismatch: expected label=" + rowKey + " actual=" + actualLabel);
        }

        int indexAfter;
        SortOrder orderAfter;
        try {
            indexAfter = plot.getIndexOf(renderer);
            orderAfter = plot.getRowRenderingOrder();
        } catch (Throwable t) {
            return;
        }

        // getLegendItems() is a query over the renderer/plot state; by contract it returns a collection
        // of legend items and should not mutate the renderer's plot slot or the plot's row rendering order.
        // A band-aid patch that dodges the buggy branch by reassigning renderer state or changing plot order
        // would violate this observable post-condition even if the returned count were forced to look correct.
        if (indexBefore != indexAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:index-purity] metamorphic violation: plot index changed across getLegendItems() before=" + indexBefore + " after=" + indexAfter);
        }
        if (orderBefore != orderAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:row-order-purity] metamorphic violation: row rendering order changed across getLegendItems() before=" + orderBefore + " after=" + orderAfter);
        }
    }
}