package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer();

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertNotNull(r.getLegendItems());
            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull] semantic mismatch: getLegendItems() returned null for a fresh renderer");
            }

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals(0, r.getLegendItems().getItemCount());
            int initialCount = initial.getItemCount();
            if (initialCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-fresh-count] semantic mismatch: expected=0 actual=" + initialCount);
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            // Shared-state agreement check on field "plot":
            // getPlot() is documented to return "the plot that the renderer has been assigned to",
            // while set via CategoryPlot.setRenderer(...) in normal usage. A patch that merely
            // suppresses behaviour in getLegendItems() must still leave this writer/reader pair consistent.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-reader-writer] semantic mismatch: renderer plot reader/writer disagree expectedSame=true actualSame=false");
            }

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals(0, r.getLegendItems().getItemCount());
            int emptyPlotCount = r.getLegendItems().getItemCount();
            if (emptyPlotCount != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset-count] semantic mismatch: expected=0 actual=" + emptyPlotCount);
            }

            // Lifted exactly from the failing test:
            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals(1, lic.getItemCount());
            int countAfterSeedAdd = lic.getItemCount();
            if (countAfterSeedAdd != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-seed-count] semantic mismatch: expected=1 actual=" + countAfterSeedAdd);
            }

            // Lifted from AbstractCategoryItemRendererTests.test2947660:
            // assertEquals("S1", lic.get(0).getLabel());
            String seedLabel = lic.get(0).getLabel();
            if (!"S1".equals(seedLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-seed-label] semantic mismatch: expected=S1 actual=" + escape(seedLabel));
            }

            // Mandatory post-condition / metamorphic check:
            // For a fixed renderer/plot/dataset state, repeated getLegendItems() calls should observe
            // the same legend contents; deleting the real population logic or guarding it away would
            // break this observable state agreement without throwing.
            LegendItemCollection lic2 = r.getLegendItems();
            int count2 = lic2.getItemCount();
            String label2 = count2 > 0 ? lic2.get(0).getLabel() : null;
            if (count2 != countAfterSeedAdd || !safeEquals(seedLabel, label2)) {
                throw new RuntimeException("[oracle:idempotent-read] metamorphic violation: repeated getLegendItems() on unchanged state should agree input=seed lhsCount=" + countAfterSeedAdd + " rhsCount=" + count2 + " lhsLabel=" + escape(seedLabel) + " rhsLabel=" + escape(label2));
            }

            // Trusted generalisation from input itself:
            // We choose the series label first, then construct a one-series dataset whose only visible
            // legend item must recover that exact label.
            String fuzzSeries = data.consumeAsciiString(16);
            if (fuzzSeries == null || fuzzSeries.length() == 0) {
                fuzzSeries = "F";
            }
            String fuzzCategory = data.consumeAsciiString(16);
            if (fuzzCategory == null || fuzzCategory.length() == 0) {
                fuzzCategory = "C";
            }
            int value = data.consumeInt(-1000, 1000);

            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            plot2.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
            dataset2.addValue((double) value, fuzzSeries, fuzzCategory);

            LegendItemCollection single = r2.getLegendItems();
            if (single == null) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-notnull] semantic mismatch: getLegendItems() returned null for constructed one-series dataset");
            }
            if (single.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-count] semantic mismatch: expected=1 actual=" + single.getItemCount() + " series=" + escape(fuzzSeries) + " category=" + escape(fuzzCategory));
            }
            String actualFuzzLabel = single.get(0).getLabel();
            if (!fuzzSeries.equals(actualFuzzLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-single-label] semantic mismatch: expected=" + escape(fuzzSeries) + " actual=" + escape(actualFuzzLabel));
            }

            // Equivalent-input metamorphic relation:
            // With exactly one visible series, row rendering order cannot change which single legend item exists.
            plot2.setRowRenderingOrder(plot2.getRowRenderingOrder().equals(SortOrder.ASCENDING) ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            LegendItemCollection singleReordered = r2.getLegendItems();
            int reorderedCount = singleReordered.getItemCount();
            String reorderedLabel = reorderedCount > 0 ? singleReordered.get(0).getLabel() : null;
            if (reorderedCount != 1 || !fuzzSeries.equals(reorderedLabel)) {
                throw new RuntimeException("[oracle:single-series-order-invariance] metamorphic violation: one-series legend should be invariant under row order inputSeries=" + escape(fuzzSeries) + " lhsCount=1 rhsCount=" + reorderedCount + " lhsLabel=" + escape(fuzzSeries) + " rhsLabel=" + escape(reorderedLabel));
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}