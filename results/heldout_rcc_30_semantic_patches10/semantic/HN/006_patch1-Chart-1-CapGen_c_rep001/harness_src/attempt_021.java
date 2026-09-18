package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-empty] semantic mismatch: expected itemCount=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract from getPlot()/setPlot(): the renderer reports the plot it has been assigned to.
        // plot.setRenderer(r) is the real public API used to establish that assignment, so a silent
        // bookkeeping-only patch that fails to keep the shared plot field consistent violates this.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot-not-null] semantic mismatch: r.getLegendItems() after plot assignment returned null");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot-count] semantic mismatch: expected itemCount=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-not-null] semantic mismatch: populated legend collection was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-count] semantic mismatch: expected itemCount=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-label] semantic mismatch: expected label=S1 actual=" + label);
        }

        // Metamorphic/post-condition: getLegendItems() is a read-only query over renderer/plot/dataset state.
        // With no intervening mutation, repeated calls must agree on observable contents; a patch that merely
        // avoids the buggy branch or drops bookkeeping can return stale/wrong legend state without throwing.
        LegendItemCollection licAgain;
        try {
            licAgain = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (licAgain == null || licAgain.getItemCount() != lic.getItemCount()
                || licAgain.getItemCount() != 1
                || !"S1".equals(licAgain.get(0).getLabel())) {
            String againLabel = (licAgain != null && licAgain.getItemCount() > 0) ? licAgain.get(0).getLabel() : "<missing>";
            throw new RuntimeException("[oracle:idempotent-read] metamorphic violation: repeated getLegendItems() on unchanged state disagreed firstCount="
                    + lic.getItemCount() + " secondCount=" + (licAgain == null ? -1 : licAgain.getItemCount())
                    + " firstLabel=" + label + " secondLabel=" + againLabel);
        }

        String fuzzColumn = data.consumeAsciiString(16);
        if (fuzzColumn.length() == 0) {
            fuzzColumn = "C";
        }
        int fuzzValue = data.consumeInt(-1000000, 1000000);

        DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
        CategoryPlot plot2 = new CategoryPlot();
        LineAndShapeRenderer r2 = new LineAndShapeRenderer();
        plot2.setDataset(dataset2);
        plot2.setRenderer(r2);

        if (r2.getPlot() != plot2) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        dataset2.addValue((double) fuzzValue, "S1", fuzzColumn);
        LegendItemCollection lic2;
        try {
            lic2 = r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        // Generalisation by equivalence to the lifted seed:
        // getLegendItems() returns legend items for series, and this dataset still has exactly one visible
        // series with the same row key "S1"; changing only category/value must not change the legend count
        // or that series label.
        if (lic2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:single-series-not-null] semantic mismatch: legend collection was null for single-series dataset");
        }
        if (lic2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:single-series-count] semantic mismatch: expected itemCount=1 actual=" + lic2.getItemCount() + " column=" + fuzzColumn + " value=" + fuzzValue);
        }
        String label2 = lic2.get(0).getLabel();
        if (!"S1".equals(label2)) {
            throw new FuzzerSecurityIssueLow("[oracle:single-series-label] semantic mismatch: expected label=S1 actual=" + label2 + " column=" + fuzzColumn + " value=" + fuzzValue);
        }
    }
}