package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runLiftedTestOracles();
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        LineAndShapeRenderer r;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        String rowKey;
        String columnKey;
        try {
            r = new LineAndShapeRenderer();
            plot = new CategoryPlot();
            dataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey == null || rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey == null || columnKey.length() == 0) {
                columnKey = "C";
            }

            plot.setDataset(dataset);
            plot.setRenderer(r);
            dataset.addValue(1.0, rowKey, columnKey);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection items;
        LegendItemCollection itemsAgain;
        CategoryPlot plotBefore;
        CategoryPlot plotAfter;
        try {
            plotBefore = r.getPlot();
            items = r.getLegendItems();
            plotAfter = r.getPlot();
            itemsAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label = items.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label + " columnKey=" + columnKey);
        }

        // Contract/state-coupling check: setRenderer()/setPlot establishes the renderer's plot; getLegendItems() is a query method over that shared state and should not silently change it.
        if (plotBefore != plot || plotAfter != plot || r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("relation plot-state-preserved violated: expected renderer plot identity to remain the assigned plot before/after getLegendItems but saw before="
                    + id(plotBefore) + " assigned=" + id(plot) + " after=" + id(plotAfter) + " final=" + id(r.getPlot()));
        }

        // Metamorphic idempotence check: for unchanged renderer/plot/dataset state, repeated getLegendItems() calls must agree.
        if (itemsAgain == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-idempotent violated: second getLegendItems() returned null");
        }
        if (itemsAgain.getItemCount() != items.getItemCount()) {
            throw new FuzzerSecurityIssueLow("relation legendItems-idempotent violated: firstCount=" + items.getItemCount() + " secondCount=" + itemsAgain.getItemCount()
                    + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        if (itemsAgain.getItemCount() > 0) {
            String labelAgain = itemsAgain.get(0).getLabel();
            if (!safeEquals(label, labelAgain)) {
                throw new FuzzerSecurityIssueLow("relation legendItems-idempotent violated: firstLabel=" + label + " secondLabel=" + labelAgain
                        + " rowKey=" + rowKey + " columnKey=" + columnKey);
            }
        }

        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        LegendItemCollection noPlotItemsAgain;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
            noPlotItemsAgain = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + noPlotItems.getItemCount());
        }
        // Hidden-state check: a read-only query on an unassigned renderer must not assign a plot or change the empty result on repetition.
        if (noPlotRenderer.getPlot() != null) {
            throw new FuzzerSecurityIssueLow("relation noPlot-query-does-not-assign-plot violated: getLegendItems() changed plot from null to " + id(noPlotRenderer.getPlot()));
        }
        if (noPlotItemsAgain == null || noPlotItemsAgain.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-idempotent violated: firstCount=0 secondCount="
                    + (noPlotItemsAgain == null ? "null" : String.valueOf(noPlotItemsAgain.getItemCount())));
        }
    }

    private static void runLiftedTestOracles() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull] semantic mismatch: expected non-null legend items but got null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-count] semantic mismatch: expected 0 but got " + initial.getItemCount());
        }
        // Shared-state agreement from the class contract: with no assigned plot, getPlot() must report null, and a question method like getLegendItems() must not change that shared field.
        if (r.getPlot() != null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-plot-state] semantic mismatch: expected null plot but got " + id(r.getPlot()));
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-setplot-agreement] semantic mismatch: expected renderer.getPlot() to be the assigned plot");
        }

        LegendItemCollection emptyAttached = r.getLegendItems();
        if (emptyAttached == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-notnull] semantic mismatch: expected non-null legend items but got null");
        }
        if (emptyAttached.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-count] semantic mismatch: expected 0 but got " + emptyAttached.getItemCount());
        }
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-plot-state] semantic mismatch: expected plot identity to remain assigned after getLegendItems()");
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-notnull] semantic mismatch: expected non-null legend items but got null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-count] semantic mismatch: expected 1 but got " + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-series-label] semantic mismatch: expected S1 but got " + actualLabel);
        }

        // Metamorphic/post-condition check: after adding one series and not mutating state further, repeated getLegendItems() must continue to report the same single legend item.
        LegendItemCollection lic2 = r.getLegendItems();
        if (lic2 == null || lic2.getItemCount() != 1 || lic2.get(0) == null || !"S1".equals(lic2.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-idempotent-read] semantic mismatch: repeated getLegendItems() disagreed with established state; count="
                    + (lic2 == null ? "null" : String.valueOf(lic2.getItemCount()))
                    + " label=" + ((lic2 == null || lic2.getItemCount() == 0 || lic2.get(0) == null) ? "null" : lic2.get(0).getLabel()));
        }
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-post-read-plot-state] semantic mismatch: expected plot identity to remain assigned after repeated getLegendItems()");
        }
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String id(Object o) {
        return o == null ? "null" : o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}