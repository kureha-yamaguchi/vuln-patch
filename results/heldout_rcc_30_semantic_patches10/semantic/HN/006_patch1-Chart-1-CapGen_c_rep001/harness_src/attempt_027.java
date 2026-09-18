package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-no-plot] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: expected renderer.getPlot()==plot after plot.setRenderer(r), actual renderer.getPlot()=" + r.getPlot() + " plot=" + plot);
        }

        LegendItemCollection emptyWithPlot = r.getLegendItems();
        if (emptyWithPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-plot] semantic mismatch: expected=0 actual=" + emptyWithPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-series] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label] semantic mismatch: expected=S1 actual=" + label);
        }

        try {
            /* Contract/invariant used: getLegendItems() reports legend items from current renderer/plot/dataset state.
               With no intervening state change, a second call must agree with the first; a "fix" that merely skips the
               real legend-building path or returns a stale/empty result violates this observable post-condition. */
            LegendItemCollection lic2 = r.getLegendItems();
            int count2 = lic2.getItemCount();
            String label2 = count2 > 0 ? lic2.get(0).getLabel() : null;
            if (count2 != lic.getItemCount() || (count2 > 0 && !label.equals(label2))) {
                throw new RuntimeException("[oracle:idempotent-legend] metamorphic violation: repeated getLegendItems() without state change must agree input=rowKey:S1,columnKey:C1,value:1.0 lhsCount=" + lic.getItemCount() + " rhsCount=" + count2 + " lhsLabel=" + label + " rhsLabel=" + label2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}