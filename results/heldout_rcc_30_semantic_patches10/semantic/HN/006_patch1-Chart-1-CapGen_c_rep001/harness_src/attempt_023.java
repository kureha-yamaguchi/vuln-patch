package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null] semantic mismatch: getLegendItems() returned null before plot assignment");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-no-plot] semantic mismatch: expected itemCount=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract from getPlot()/setPlot() docs: the renderer reports the plot it has been assigned to.
        // This shared-state check matters because getLegendItems() reads the same 'plot' field; a patch that
        // merely suppresses behavior in getLegendItems() could still leave the renderer/plot coupling wrong.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-coupling] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyAttached = r.getLegendItems();
        if (emptyAttached.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-attached] semantic mismatch: expected itemCount=0 actual=" + emptyAttached.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-count] semantic mismatch: expected itemCount=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-label] semantic mismatch: expected label=S1 actual=" + String.valueOf(label));
        }

        try {
            // Metamorphic relation: with no intervening state change, repeated reads of getLegendItems()
            // for the same renderer/plot/dataset state must agree on observable contents.
            LegendItemCollection lic2 = r.getLegendItems();
            int c1 = lic.getItemCount();
            int c2 = lic2.getItemCount();
            String l1 = c1 > 0 ? lic.get(0).getLabel() : null;
            String l2 = c2 > 0 ? lic2.get(0).getLabel() : null;
            boolean same = c1 == c2 && (l1 == null ? l2 == null : l1.equals(l2));
            if (!same) {
                throw new RuntimeException("[oracle:idempotent-read] metamorphic violation: repeated getLegendItems() changed without state update count1=" + c1 + " count2=" + c2 + " label1=" + String.valueOf(l1) + " label2=" + String.valueOf(l2));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            AbstractCategoryItemRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            int rows = data.consumeInt(0, 3);
            String columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }
            for (int i = 0; i < rows; i++) {
                String rowKey = data.consumeAsciiString(8);
                if (rowKey.length() == 0) {
                    rowKey = "S" + i;
                }
                double value = data.consumeInt(-1000, 1000);
                dataset2.addValue(value, rowKey, columnKey);
                r2.setSeriesVisibleInLegend(i, Boolean.valueOf(data.consumeBoolean()));
            }
            r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }
    }
}