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
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-notnull] semantic mismatch: getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        /* Contract check on shared state: CategoryItemRenderer#getPlot() returns
           the plot that the renderer has been assigned to, and CategoryPlot#setRenderer(...)
           is the real API path that performs that assignment in normal usage.
           A patch that merely suppresses legend generation or skips bookkeeping
           would still violate this observable state agreement if assignment were broken. */
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-notnull] semantic mismatch: getLegendItems() returned null after plot assignment");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-notnull] semantic mismatch: getLegendItems() returned null after adding data");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-populated-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        try {
            /* Trusted oracle by construction: we choose the dataset row keys first,
               add exactly one value per row, and with default legend visibility a
               correct getLegendItems() must expose one legend item per dataset row
               for the renderer's dataset. This directly observes the post-condition
               the patched method is supposed to produce; a "fix" that simply returns
               an empty collection or skips dataset traversal breaks it silently. */
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            String sA = nonEmptyAscii(data.consumeAsciiString(8), "A");
            String sB = nonEmptyAscii(data.consumeAsciiString(8), "B");
            String c1 = nonEmptyAscii(data.consumeAsciiString(8), "C1");
            String c2 = nonEmptyAscii(data.consumeAsciiString(8), "C2");
            if (sA.equals(sB)) {
                sB = sB + "_2";
            }
            if (c1.equals(c2)) {
                c2 = c2 + "_2";
            }

            dataset2.addValue(1.0, sA, c1);
            dataset2.addValue(2.0, sB, c2);

            LegendItemCollection lic2 = r2.getLegendItems();
            if (lic2 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-notnull] semantic mismatch: getLegendItems() returned null");
            }
            if (lic2.getItemCount() != dataset2.getRowCount()) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructed-count] semantic mismatch: inputRows=" + dataset2.getRowCount()
                        + " legendCount=" + lic2.getItemCount()
                );
            }

            /* Metamorphic/post-condition relation: because the plot default row rendering
               order is ASCENDING and we inserted rows in order sA then sB, the legend
               labels must agree with those established row keys. This is a real-call vs
               real-state check, not a hand-rolled implementation. */
            String l0 = lic2.get(0).getLabel();
            String l1 = lic2.get(1).getLabel();
            if (!sA.equals(l0) || !sB.equals(l1)) {
                throw new RuntimeException(
                    "[oracle:constructed-label-order] metamorphic violation: legend labels must match dataset row keys in ascending row order"
                        + " sA=" + sA + " sB=" + sB + " lhs0=" + String.valueOf(l0) + " lhs1=" + String.valueOf(l1)
                );
            }

            if (r2.getPlot() != plot2) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-plot-state] semantic mismatch: expected renderer plot identity to match assigned plot");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static String nonEmptyAscii(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}