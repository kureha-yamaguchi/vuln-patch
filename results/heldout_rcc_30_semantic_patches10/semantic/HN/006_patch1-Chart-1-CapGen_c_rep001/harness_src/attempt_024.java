package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        try {
            // Lifted exactly from AbstractCategoryItemRendererTests.test2947660.
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

            LegendItemCollection itemsNoPlot = r.getLegendItems();
            if (itemsNoPlot == null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() expected non-null but was null");
            }
            if (itemsNoPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-empty-without-plot] semantic mismatch: expected 0 but was "
                        + itemsNoPlot.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            LegendItemCollection itemsEmptyDataset = r.getLegendItems();
            if (itemsEmptyDataset.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-empty-with-plot] semantic mismatch: expected 0 but was "
                        + itemsEmptyDataset.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-single-series-count] semantic mismatch: expected 1 but was "
                        + lic.getItemCount());
            }
            String liftedLabel = lic.get(0).getLabel();
            if (!"S1".equals(liftedLabel)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-single-series-label] semantic mismatch: expected S1 but was "
                        + String.valueOf(liftedLabel));
            }

            // Shared-state agreement check: plot.setRenderer(r) is the public API setup used by the test,
            // and getPlot()/setPlot share the renderer's plot field. A correct implementation must report
            // the same plot that the writer-side setup established.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:plot-state-agreement] semantic mismatch: renderer reported different plot instance");
            }

            // Read-only post-condition on a get* method: getLegendItems() is documented as returning the
            // legend items for the renderer; it should not silently mutate plot assignment or change the
            // observable legend result on identical state. A patch that merely skips intended behavior can
            // violate this stable observable even though no exception occurs.
            LegendItemCollection licAgain = r.getLegendItems();
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:post-readonly-plot] semantic mismatch: getLegendItems() changed renderer plot state");
            }
            if (licAgain == null || licAgain.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:post-repeat-count] semantic mismatch: repeated getLegendItems() expected 1 but was "
                        + (licAgain == null ? "null" : String.valueOf(licAgain.getItemCount())));
            }
            String liftedLabelAgain = licAgain.get(0).getLabel();
            if (!"S1".equals(liftedLabelAgain)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:post-repeat-label] semantic mismatch: repeated getLegendItems() expected S1 but was "
                        + String.valueOf(liftedLabelAgain));
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection items2;
        try {
            r2 = new LineAndShapeRenderer();
            plot2 = new CategoryPlot();
            dataset2 = new DefaultCategoryDataset();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(value, rowKey, columnKey);
            items2 = r2.getLegendItems();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        // Candidate invariant/generalisation: with a non-null dataset containing exactly one series,
        // getLegendItems() must return exactly one legend item for that series, and its label must match
        // the series row key. This is the same real API path as the failing test, generalized by choosing
        // the known answer first (the row key we inserted).
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got "
                    + items2.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label2 = items2.get(0).getLabel();
        if (!rowKey.equals(label2)) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-singleSeries-nonNullDataset violated: expected legend label "
                    + rowKey + " but got " + String.valueOf(label2));
        }

        LegendItemCollection items2Again;
        try {
            items2Again = r2.getLegendItems();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        // Additional post-condition/metamorphic check: repeated queries on unchanged state must agree.
        if (items2Again == null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-repeat-nonnull] semantic mismatch: second getLegendItems() returned null");
        }
        if (items2Again.getItemCount() != items2.getItemCount()) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-repeat-count] semantic mismatch: first count="
                    + items2.getItemCount() + " second count=" + items2Again.getItemCount()
                    + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label2Again = items2Again.get(0).getLabel();
        if (!rowKey.equals(label2Again)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-repeat-label] semantic mismatch: expected "
                    + rowKey + " but second call returned " + String.valueOf(label2Again));
        }
        if (r2.getPlot() != plot2) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:metamorphic-plot-stable] semantic mismatch: getLegendItems() changed or lost plot association");
        }

        LineAndShapeRenderer r3;
        LegendItemCollection items3;
        try {
            r3 = new LineAndShapeRenderer();
            items3 = r3.getLegendItems();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        // Candidate invariant/generalisation: without an assigned plot, getLegendItems() is non-null and empty.
        if (items3 == null) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items3.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                "relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got "
                    + items3.getItemCount());
        }
    }
}