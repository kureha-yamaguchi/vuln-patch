package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted oracle from AbstractCategoryItemRendererTests.test2947660.

        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initialItems = r.getLegendItems();
        if (initialItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (initialItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initialItems.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Shared-state agreement check: setPlot()/plot.setRenderer(r) establishes the renderer's plot;
        // getPlot() is documented to report the plot the renderer has been assigned to.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-reader-writer] semantic mismatch: expected renderer.getPlot() to be the assigned plot");
        }

        LegendItemCollection emptyPlotItems = r.getLegendItems();
        if (emptyPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (emptyPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-empty] semantic mismatch: expected=0 actual=" + emptyPlotItems.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        // Post-condition / hidden-state check: getLegendItems() is a getter that should not mutate the
        // renderer's assigned plot; a patch that merely skips the real work or corrupts bookkeeping would
        // violate this observable read-only behavior.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-no-plot-mutation] semantic mismatch: getLegendItems changed renderer plot association");
        }

        // Metamorphic/idempotence check on the same real API:
        // for a fixed renderer/plot/dataset state, repeated getLegendItems() calls must agree on count and label.
        LegendItemCollection lic2 = r.getLegendItems();
        if (lic2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-idempotence-nonnull] semantic mismatch: second getLegendItems returned null");
        }
        if (lic2.getItemCount() != lic.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-idempotence-count] metamorphic violation: repeated getLegendItems() changed item count lhs=" + lic.getItemCount() + " rhs=" + lic2.getItemCount());
        }
        if (lic2.getItemCount() == 1) {
            String label2 = lic2.get(0).getLabel();
            if (!label.equals(label2)) {
                throw new FuzzerSecurityIssueLow("[oracle:getLegendItems-idempotence-label] metamorphic violation: repeated getLegendItems() changed label lhs=" + String.valueOf(label) + " rhs=" + String.valueOf(label2));
            }
        }

        // Generalization on valid-by-construction non-degenerate fuzzed inputs.
        LineAndShapeRenderer fuzzRenderer;
        CategoryPlot fuzzPlot;
        DefaultCategoryDataset fuzzDataset;
        String rowKey;
        String columnKey;
        LegendItemCollection fuzzItems;
        LegendItemCollection fuzzItems2;
        try {
            fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot = new CategoryPlot();
            fuzzDataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }

            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);

            if (fuzzRenderer.getPlot() != fuzzPlot) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-reader-writer] semantic mismatch: expected renderer.getPlot() to equal assigned plot");
            }

            fuzzDataset.addValue(1.0, rowKey, columnKey);
            fuzzItems = fuzzRenderer.getLegendItems();
            fuzzItems2 = fuzzRenderer.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount());
        }
        String fuzzLabel = fuzzItems.get(0).getLabel();
        if (!rowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + String.valueOf(fuzzLabel));
        }
        if (fuzzRenderer.getPlot() != fuzzPlot) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() changed renderer plot association");
        }
        if (fuzzItems2 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: repeated getLegendItems() returned null");
        }
        if (fuzzItems2.getItemCount() != fuzzItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: repeated getLegendItems() changed count lhs=" + fuzzItems.getItemCount() + " rhs=" + fuzzItems2.getItemCount());
        }
        if (fuzzItems2.getItemCount() == 1) {
            String fuzzLabel2 = fuzzItems2.get(0).getLabel();
            if (!rowKey.equals(fuzzLabel2)) {
                throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: repeated getLegendItems() changed label expected=" + rowKey + " actual=" + String.valueOf(fuzzLabel2));
            }
        }

        // Candidate relation: no plot => non-null, empty collection.
        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + noPlotItems.getItemCount());
        }
    }
}