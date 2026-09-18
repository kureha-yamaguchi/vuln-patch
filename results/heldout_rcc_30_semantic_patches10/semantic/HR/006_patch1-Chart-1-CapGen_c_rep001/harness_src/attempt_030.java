package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedAssertionsFromTest2947660Exactly();
        checkSingleRendererPlotAggregateAgreement(data);
        checkNullBoundaryRoundTripAgainstFreshPlot(data);
    }

    private static void runLiftedAssertionsFromTest2947660Exactly() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection prePlot = r.getLegendItems();
        if (prePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-exact-unique] semantic mismatch: expected non-null legend collection before plot assignment but got null");
        }
        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-zero-exact-unique] semantic mismatch: expected 0 legend items before plot assignment but got " + r.getLegendItems().getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-zero-exact-unique] semantic mismatch: expected 0 legend items for attached empty dataset but got " + r.getLegendItems().getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-exact-unique] semantic mismatch: expected 1 legend item after adding dataset value but got " + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-exact-unique] semantic mismatch: expected label S1 but got " + actualLabel);
        }
    }

    private static void checkSingleRendererPlotAggregateAgreement(FuzzedDataProvider data) {
        LineAndShapeRenderer renderer;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        String rowKey;
        String colKey;
        LegendItemCollection rendererItems;
        LegendItemCollection plotItems;
        try {
            renderer = new LineAndShapeRenderer();
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(renderer);
            rowKey = nonEmptyAscii(data, 8);
            colKey = nonEmptyAscii(data, 8);
            dataset.addValue(data.consumeInt(-1000, 1000), rowKey, colKey);
            rendererItems = renderer.getLegendItems();
            plotItems = plot.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (rendererItems == null || plotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:single-renderer-plot-aggregate] semantic mismatch: expected non-null legend collections rendererItems=" + rendererItems + " plotItems=" + plotItems);
        }

        /* Contract-based sibling agreement:
         * getLegendItems() on CategoryPlot aggregates legend items from its renderers.
         * With exactly one renderer installed for the plot, the plot aggregate must match
         * that renderer's own legend collection. A throw-deleting or overfit patch that
         * masks one surface but leaves aggregation inconsistent would violate this.
         */
        if (rendererItems.getItemCount() != plotItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:single-renderer-plot-aggregate] consistency violation: rendererCount=" + rendererItems.getItemCount() + " plotCount=" + plotItems.getItemCount());
        }

        for (int i = 0; i < rendererItems.getItemCount(); i++) {
            LegendItem ri = rendererItems.get(i);
            LegendItem pi = plotItems.get(i);
            String rLabel = ri.getLabel();
            String pLabel = pi.getLabel();
            if (rLabel == null ? pLabel != null : !rLabel.equals(pLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:single-renderer-plot-aggregate] consistency violation: index=" + i + " rendererLabel=" + rLabel + " plotLabel=" + pLabel);
            }
            if (ri.getDatasetIndex() != pi.getDatasetIndex()) {
                throw new FuzzerSecurityIssueLow("[oracle:single-renderer-plot-aggregate] consistency violation: index=" + i + " rendererDatasetIndex=" + ri.getDatasetIndex() + " plotDatasetIndex=" + pi.getDatasetIndex());
            }
            Comparable rk = ri.getSeriesKey();
            Comparable pk = pi.getSeriesKey();
            if (rk == null ? pk != null : !rk.equals(pk)) {
                throw new FuzzerSecurityIssueLow("[oracle:single-renderer-plot-aggregate] consistency violation: index=" + i + " rendererSeriesKey=" + rk + " plotSeriesKey=" + pk);
            }
        }
    }

    private static void checkNullBoundaryRoundTripAgainstFreshPlot(FuzzedDataProvider data) {
        LineAndShapeRenderer statefulRenderer;
        LineAndShapeRenderer freshRenderer;
        DefaultCategoryDataset dataset;
        CategoryPlot statefulPlot;
        CategoryPlot freshPlot;
        String rowKey;
        String colKey;
        LegendItemCollection afterRestore;
        LegendItemCollection freshItems;
        try {
            rowKey = nonEmptyAscii(data, 8);
            colKey = nonEmptyAscii(data, 8);
            dataset = new DefaultCategoryDataset();
            dataset.addValue(data.consumeInt(-1000, 1000), rowKey, colKey);

            statefulRenderer = new LineAndShapeRenderer();
            statefulPlot = new CategoryPlot();
            statefulPlot.setRenderer(statefulRenderer);
            statefulPlot.setDataset(null);
            statefulPlot.setDataset(dataset);
            afterRestore = statefulRenderer.getLegendItems();

            freshRenderer = new LineAndShapeRenderer();
            freshPlot = new CategoryPlot();
            freshPlot.setRenderer(freshRenderer);
            freshPlot.setDataset(dataset);
            freshItems = freshRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (afterRestore == null || freshItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:null-boundary-roundtrip-fresh] semantic mismatch: expected non-null legend collections afterRestore=" + afterRestore + " freshItems=" + freshItems);
        }

        /* Flipped-condition boundary check:
         * the patch changes behavior exactly at dataset == null vs dataset != null.
         * Crossing that boundary and restoring the same non-null dataset should leave
         * the renderer observationally equivalent to a fresh renderer attached directly
         * to that same dataset. An overfit fix around the seed or a guard that skips the
         * real work after a null transition breaks this equivalence.
         */
        if (afterRestore.getItemCount() != freshItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:null-boundary-roundtrip-fresh] metamorphic violation: restoredCount=" + afterRestore.getItemCount() + " freshCount=" + freshItems.getItemCount());
        }

        for (int i = 0; i < afterRestore.getItemCount(); i++) {
            LegendItem a = afterRestore.get(i);
            LegendItem f = freshItems.get(i);
            String aLabel = a.getLabel();
            String fLabel = f.getLabel();
            if (aLabel == null ? fLabel != null : !aLabel.equals(fLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:null-boundary-roundtrip-fresh] metamorphic violation: index=" + i + " restoredLabel=" + aLabel + " freshLabel=" + fLabel);
            }
            if (a.getSeriesIndex() != f.getSeriesIndex()) {
                throw new FuzzerSecurityIssueLow("[oracle:null-boundary-roundtrip-fresh] metamorphic violation: index=" + i + " restoredSeriesIndex=" + a.getSeriesIndex() + " freshSeriesIndex=" + f.getSeriesIndex());
            }
            Comparable ak = a.getSeriesKey();
            Comparable fk = f.getSeriesKey();
            if (ak == null ? fk != null : !ak.equals(fk)) {
                throw new FuzzerSecurityIssueLow("[oracle:null-boundary-roundtrip-fresh] metamorphic violation: index=" + i + " restoredSeriesKey=" + ak + " freshSeriesKey=" + fk);
            }
        }
    }

    private static String nonEmptyAscii(FuzzedDataProvider data, int maxLen) {
        String s = data.consumeAsciiString(Math.max(1, maxLen));
        if (s == null || s.length() == 0) {
            return "A";
        }
        return s;
    }
}