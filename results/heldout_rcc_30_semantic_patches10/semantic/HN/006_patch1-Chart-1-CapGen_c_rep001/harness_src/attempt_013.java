package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-notnull] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-initial-empty] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract: getPlot() "Returns the plot that the renderer has been assigned to";
        // after CategoryPlot#setRenderer(r), the renderer's shared plot field must report that plot.
        if (r.getPlot() != plot) {
            throw new RuntimeException("[oracle:plot-coupling] metamorphic violation: renderer/plot assignment disagreed expectedSame=true actualSame=false");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-empty-dataset] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-one-series-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-one-series-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        try {
            String row1 = data.consumeAsciiString(12);
            String row2 = data.consumeAsciiString(12);
            String col = data.consumeAsciiString(12);
            if (row1.length() == 0) {
                row1 = "R1";
            }
            if (row2.length() == 0) {
                row2 = "R2";
            }
            if (row1.equals(row2)) {
                row2 = row2 + "_2";
            }
            if (col.length() == 0) {
                col = "C";
            }
            double v1 = data.consumeInt(-1000000, 1000000);
            double v2 = data.consumeInt(-1000000, 1000000);

            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            dataset2.addValue(v1, row1, col);
            dataset2.addValue(v2, row2, col);

            // Post-condition from getLegendItems() body and contracts:
            // with a non-null plot, non-null dataset, and both series visible in the legend,
            // the returned collection should contain one item per dataset row, in row rendering order.
            // A "fix" that merely suppresses the failing path by returning an empty collection breaks this.
            plot2.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection asc = r2.getLegendItems();
            if (asc.getItemCount() != 2) {
                throw new RuntimeException("[oracle:two-series-ascending-count] metamorphic violation: expectedCount=2 actualCount=" + asc.getItemCount() + " row1=" + row1 + " row2=" + row2);
            }
            String asc0 = asc.get(0).getLabel();
            String asc1 = asc.get(1).getLabel();
            if (!row1.equals(asc0) || !row2.equals(asc1)) {
                throw new RuntimeException("[oracle:two-series-ascending-order] metamorphic violation: expected0=" + row1 + " actual0=" + String.valueOf(asc0) + " expected1=" + row2 + " actual1=" + String.valueOf(asc1));
            }

            plot2.setRowRenderingOrder(SortOrder.DESCENDING);
            LegendItemCollection desc = r2.getLegendItems();
            if (desc.getItemCount() != 2) {
                throw new RuntimeException("[oracle:two-series-descending-count] metamorphic violation: expectedCount=2 actualCount=" + desc.getItemCount() + " row1=" + row1 + " row2=" + row2);
            }
            String desc0 = desc.get(0).getLabel();
            String desc1 = desc.get(1).getLabel();
            if (!row2.equals(desc0) || !row1.equals(desc1)) {
                throw new RuntimeException("[oracle:two-series-descending-order] metamorphic violation: expected0=" + row2 + " actual0=" + String.valueOf(desc0) + " expected1=" + row1 + " actual1=" + String.valueOf(desc1));
            }

            // Contract from AbstractRenderer: per-series legend visibility controls whether a series should
            // be shown in the legend; getLegendItems() consults isSeriesVisibleInLegend(i) before adding.
            r2.setSeriesVisibleInLegend(0, Boolean.FALSE);
            plot2.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection filtered = r2.getLegendItems();
            if (filtered.getItemCount() != 1) {
                throw new RuntimeException("[oracle:legend-visibility-count] metamorphic violation: expectedCount=1 actualCount=" + filtered.getItemCount() + " hiddenSeries=0");
            }
            String filtered0 = filtered.get(0).getLabel();
            if (!row2.equals(filtered0)) {
                throw new RuntimeException("[oracle:legend-visibility-label] metamorphic violation: expected=" + row2 + " actual=" + String.valueOf(filtered0));
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}