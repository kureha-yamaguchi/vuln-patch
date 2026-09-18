package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    private static void fail(String id, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection lic0 = r.getLegendItems();
        if (lic0 == null) {
            fail("test2947660-not-null", "expected non-null legend items without plot but actual was null");
        }
        if (lic0.getItemCount() != 0) {
            fail("test2947660-no-plot-count", "expected 0 but actual was " + lic0.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getPlot() != plot) {
            fail("plot-writer-reader-agree", "expected getPlot() to return the same plot instance assigned by CategoryPlot.setRenderer()");
        }

        LegendItemCollection lic1 = r.getLegendItems();
        if (lic1 == null) {
            fail("test2947660-empty-dataset-not-null", "expected non-null legend items with empty dataset but actual was null");
        }
        if (lic1.getItemCount() != 0) {
            fail("test2947660-empty-dataset-count", "expected 0 but actual was " + lic1.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");

        LegendItemCollection lic2 = r.getLegendItems();
        if (lic2 == null) {
            fail("test2947660-single-series-not-null", "expected non-null legend items after adding one series but actual was null");
        }
        if (lic2.getItemCount() != 1) {
            fail("test2947660-single-series-count", "expected 1 but actual was " + lic2.getItemCount());
        }
        String label = lic2.get(0).getLabel();
        if (!"S1".equals(label)) {
            fail("test2947660-single-series-label", "expected S1 but actual was " + label);
        }

        /* Contract/post-condition: getLegendItems() is a read/query over renderer state; repeated reads on
           unchanged state must agree, and must not disturb the plot link established by CategoryPlot.setRenderer().
           A patch that merely skips work or corrupts bookkeeping can evade one assertion but violate this. */
        LegendItemCollection lic3 = r.getLegendItems();
        if (lic3 == null) {
            fail("legendItems-repeat-not-null", "expected non-null legend items on repeated read but actual was null");
        }
        if (lic3.getItemCount() != lic2.getItemCount()) {
            fail("legendItems-repeat-count", "expected repeated getLegendItems() count " + lic2.getItemCount() + " but actual was " + lic3.getItemCount());
        }
        if (!lic2.get(0).getLabel().equals(lic3.get(0).getLabel())) {
            fail("legendItems-repeat-label", "expected repeated getLegendItems() label " + lic2.get(0).getLabel() + " but actual was " + lic3.get(0).getLabel());
        }
        if (r.getPlot() != plot) {
            fail("plot-stable-after-read", "expected getLegendItems() not to change the assigned plot");
        }
    }
}