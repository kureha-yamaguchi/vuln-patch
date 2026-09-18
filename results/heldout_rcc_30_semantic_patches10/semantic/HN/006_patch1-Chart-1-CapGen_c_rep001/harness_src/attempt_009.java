package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        // Lifted oracle pair 1 from test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-not-null] semantic mismatch: getLegendItems() returned null");
        }

        // Lifted oracle pair 2 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state consistency oracle over field 'plot':
        // setPlot()/getPlot() are documented as writer/reader for the same field.
        // A correct implementation must report the plot that was assigned.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-reader-writer] semantic mismatch: expected renderer plot identity to equal assigned plot");
        }

        // Lifted oracle pair 3 from test2947660:
        // assertEquals(0, r.getLegendItems().getItemCount());
        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-not-null] semantic mismatch: getLegendItems() returned null after plot assignment");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        // Consume fuzz data to vary but keep non-degenerate, moderate inputs.
        String series = data.consumeAsciiString(16);
        if (series.length() == 0) {
            series = "S1";
        }
        String category = data.consumeAsciiString(16);
        if (category.length() == 0) {
            category = "C1";
        }
        int value = data.consumeInt(-1000, 1000);

        // Exact reconstruction of the failing test's trusted scenario.
        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();

        // Lifted oracle pair 4 from test2947660:
        // assertEquals(1, lic.getItemCount());
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-not-null] semantic mismatch: getLegendItems() returned null after adding one series");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }

        // Lifted oracle pair 5 from test2947660:
        // assertEquals("S1", lic.get(0).getLabel());
        String firstLabel = lic.get(0).getLabel();
        if (!"S1".equals(firstLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label] semantic mismatch: expected=S1 actual=" + firstLabel);
        }

        // Mandatory post-condition / metamorphic check:
        // getLegendItems() is a reader over current renderer/plot/dataset state.
        // With no intervening state change, repeated calls must agree. A patch that
        // "fixes" the bug by skipping the real enumeration or returning stale/empty
        // data can violate this observable stability even without throwing.
        try {
            LegendItemCollection lic2 = r.getLegendItems();
            if (lic2 == null) {
                return;
            }
            int c1 = lic.getItemCount();
            int c2 = lic2.getItemCount();
            String l1 = c1 > 0 ? lic.get(0).getLabel() : null;
            String l2 = c2 > 0 ? lic2.get(0).getLabel() : null;
            if (c1 != c2 || (l1 == null ? l2 != null : !l1.equals(l2))) {
                throw new RuntimeException("[oracle:idempotent-reader] metamorphic violation: repeated getLegendItems() without state change must agree count/firstLabel lhsCount="
                        + c1 + " rhsCount=" + c2 + " lhsLabel=" + l1 + " rhsLabel=" + l2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Additional trusted exploration by construction: add one more value in the
        // same series under a fuzzed category. Legend items are per series, so adding
        // another category for existing row key must not change the single-series
        // legend count established by the already-trusted "S1" setup.
        try {
            dataset.addValue((double) value, "S1", category);
            LegendItemCollection lic3 = r.getLegendItems();
            if (lic3 == null) {
                return;
            }
            if (lic3.getItemCount() != 1) {
                throw new RuntimeException("[oracle:same-series-extra-category] metamorphic violation: adding another category for the same existing series must preserve one legend item actual="
                        + lic3.getItemCount() + " category=" + category + " value=" + value);
            }
            String label3 = lic3.get(0).getLabel();
            if (!"S1".equals(label3)) {
                throw new RuntimeException("[oracle:same-series-label-stable] metamorphic violation: sole legend label must remain the existing series label expected=S1 actual="
                        + label3 + " category=" + category + " value=" + value);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Exercise an additional real input without asserting an unjustified answer.
        if (series.length() > 0 && !"S1".equals(series)) {
            try {
                dataset.addValue((double) value, series, category + "_2");
                r.getLegendItems();
            } catch (Throwable t) {
                return;
            }
        }
    }
}