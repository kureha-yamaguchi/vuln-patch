package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runExactLiftedSeedFromTest2947660();
        checkCrossRendererSingleSeriesAgreement(data);
    }

    private static void runExactLiftedSeedFromTest2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection prePlot = r.getLegendItems();
        if (prePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-notnull-seed] semantic mismatch: expected non-null legend collection before plot attachment but was null");
        }
        if (prePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-zero-seed] semantic mismatch: expected 0 legend items before plot attachment but was " + prePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyAttached = r.getLegendItems();
        if (emptyAttached.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-zero-seed] semantic mismatch: expected 0 legend items for attached empty dataset but was " + emptyAttached.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-seed] semantic mismatch: expected 1 legend item after adding one series but was " + lic.getItemCount());
        }
        LegendItem item = lic.get(0);
        if (!"S1".equals(item.getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-seed] semantic mismatch: expected label S1 but was " + item.getLabel());
        }
    }

    private static void checkCrossRendererSingleSeriesAgreement(FuzzedDataProvider data) {
        String rowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        String colKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        int value = data.consumeInt(-1000, 1000);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try {
            dataset.addValue(value, rowKey, colKey);
        } catch (RuntimeException e) {
            return;
        }

        LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();
        AreaRenderer areaRenderer = new AreaRenderer();
        CategoryPlot linePlot = new CategoryPlot();
        CategoryPlot areaPlot = new CategoryPlot();

        try {
            linePlot.setDataset(dataset);
            linePlot.setRenderer(lineRenderer);
            areaPlot.setDataset(dataset);
            areaPlot.setRenderer(areaRenderer);
        } catch (RuntimeException e) {
            return;
        }

        LegendItemCollection lineItems;
        LegendItemCollection areaItems;
        try {
            lineItems = lineRenderer.getLegendItems();
            areaItems = areaRenderer.getLegendItems();
        } catch (RuntimeException e) {
            return;
        }

        if (lineItems == null || areaItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-renderer-nonnull] consistency violation: lineItems=" + lineItems + " areaItems=" + areaItems);
        }

        if (lineItems.getItemCount() != areaItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-renderer-count] consistency violation: same one-series dataset produced different legend counts across real renderers; lineCount=" + lineItems.getItemCount() + " areaCount=" + areaItems.getItemCount());
        }

        /* Contract basis: getLegendItems() returns the legend items for the series
           the renderer is responsible for drawing. For the same attached dataset with
           exactly one default-visible row, different real renderers should expose the
           same single series in the legend. A band-aid that merely suppresses one
           renderer's symptom would break this cross-renderer agreement. */
        if (lineItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-renderer-single-series-count] consistency violation: expected exactly one legend item for a one-row dataset across real renderers but got " + lineItems.getItemCount());
        }

        LegendItem lineItem;
        LegendItem areaItem;
        try {
            lineItem = lineItems.get(0);
            areaItem = areaItems.get(0);
        } catch (RuntimeException e) {
            return;
        }

        String lineLabel = lineItem.getLabel();
        String areaLabel = areaItem.getLabel();
        if (!rowKey.equals(lineLabel) || !rowKey.equals(areaLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-renderer-label] consistency violation: same one-series dataset should yield row-key legend labels across real renderers; expected=" + rowKey + " lineLabel=" + lineLabel + " areaLabel=" + areaLabel);
        }

        if (linePlot.getIndexOf(lineRenderer) != 0 || areaPlot.getIndexOf(areaRenderer) != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-renderer-plot-index] consistency violation: renderer should remain registered in slot 0 after getLegendItems(); lineIndex=" + linePlot.getIndexOf(lineRenderer) + " areaIndex=" + areaPlot.getIndexOf(areaRenderer));
        }
    }
}