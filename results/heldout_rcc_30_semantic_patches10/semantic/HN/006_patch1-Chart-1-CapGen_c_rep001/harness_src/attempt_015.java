package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted oracle #1 from test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-not-null] semantic mismatch: r.getLegendItems() returned null");
        }

        // Lifted oracle #2 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        int initialCount = initial.getItemCount();
        if (initialCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initialCount);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state agreement check:
        // getPlot()/setPlot() share the renderer's plot field; after CategoryPlot.setRenderer(r),
        // the renderer must report the same plot it has been assigned to.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expectedSamePlot=true actualSamePlot=false");
        }

        // Lifted oracle #3 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        int emptyPlotCount = r.getLegendItems().getItemCount();
        if (emptyPlotCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected=0 actual=" + emptyPlotCount);
        }

        // Consume fuzz data after faithfully reconstructing the seed test, so the exact bug remains reachable.
        String fuzzSeries = data.consumeString(32);
        if (fuzzSeries == null || fuzzSeries.length() == 0) {
            fuzzSeries = "F";
        }
        String fuzzCategory = data.consumeString(32);
        if (fuzzCategory == null || fuzzCategory.length() == 0) {
            fuzzCategory = "C";
        }
        double fuzzValue = data.consumeInt(-1000000, 1000000);

        // Lifted oracle #4 from test2947660:
        // dataset.addValue(1.0, "S1", "C1");
        // LegendItemCollection lic = r.getLegendItems();
        // assertEquals(1, lic.getItemCount());
        // assertEquals("S1", lic.get(0).getLabel());
        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        int itemCount = lic.getItemCount();
        if (itemCount != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-item] semantic mismatch: expected=1 actual=" + itemCount);
        }
        String label0 = lic.get(0).getLabel();
        if (!"S1".equals(label0)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label0));
        }

        // Mandatory post-condition / metamorphic check on the patched method:
        // For a plot with exactly one series, getLegendItems() iterates that one series regardless
        // of rowRenderingOrder; ASCENDING and DESCENDING must therefore produce the same single item.
        // A patch that merely skips the real dataset-processing branch would violate this observable
        // post-condition because the collection would no longer reflect the established dataset state.
        try {
            plot.setRowRenderingOrder(SortOrder.ASCENDING);
            LegendItemCollection asc = r.getLegendItems();
            plot.setRowRenderingOrder(SortOrder.DESCENDING);
            LegendItemCollection desc = r.getLegendItems();

            int ascCount = asc.getItemCount();
            int descCount = desc.getItemCount();
            String ascLabel = ascCount > 0 ? asc.get(0).getLabel() : null;
            String descLabel = descCount > 0 ? desc.get(0).getLabel() : null;

            if (ascCount != descCount || (ascCount > 0 && !safeEquals(ascLabel, descLabel))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:single-series-order] metamorphic violation: single-series legend must be order-invariant"
                                + " ascCount=" + ascCount
                                + " descCount=" + descCount
                                + " ascLabel=" + String.valueOf(ascLabel)
                                + " descLabel=" + String.valueOf(descLabel));
            }
            if (ascCount != 1 || !safeEquals("S1", ascLabel)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:single-series-postcondition] semantic mismatch: established single-series dataset must yield one legend item labeled S1"
                                + " count=" + ascCount
                                + " label=" + String.valueOf(ascLabel));
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }

        // Trusted generalization by construction:
        // We build a fresh renderer/plot/dataset with exactly one non-empty series key chosen first.
        // For the same reason as the lifted seed ("S1" -> label "S1"), a correct implementation must
        // report exactly one legend item for a one-row dataset and preserve that row key as the label.
        try {
            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            dataset2.addValue(fuzzValue, fuzzSeries, fuzzCategory);
            LegendItemCollection lic2 = r2.getLegendItems();
            int count2 = lic2.getItemCount();
            if (count2 != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-series-count] semantic mismatch: expected=1 actual=" + count2 + " series=" + escape(fuzzSeries));
            }
            String label2 = lic2.get(0).getLabel();
            if (!safeEquals(fuzzSeries, label2)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-series-label] semantic mismatch: expected=" + escape(fuzzSeries) + " actual=" + escape(label2));
            }

            // Shared-state agreement repeated on the fuzz-constructed instance.
            if (r2.getPlot() != plot2) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-plot-assignment] semantic mismatch: expectedSamePlot=true actualSamePlot=false");
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}