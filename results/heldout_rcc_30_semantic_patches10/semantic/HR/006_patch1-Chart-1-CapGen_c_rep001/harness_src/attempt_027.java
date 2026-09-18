package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runVerbatimLiftedTestPairs();
        runBoundaryFlipEqualsOracle(data);
    }

    private static void runVerbatimLiftedTestPairs() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection beforePlot = r.getLegendItems();
        if (beforePlot == null) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:verbatim-notnull-preplot] semantic mismatch: expected non-null legend collection before plot assignment");
        }
        if (beforePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:verbatim-zero-preplot] semantic mismatch: expectedItemCount=0 actualItemCount="
                            + beforePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyPlotItems = r.getLegendItems();
        if (emptyPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:verbatim-zero-empty-dataset] semantic mismatch: expectedItemCount=0 actualItemCount="
                            + emptyPlotItems.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:verbatim-one-single-series] semantic mismatch: expectedItemCount=1 actualItemCount="
                            + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:verbatim-single-label] semantic mismatch: expectedLabel=S1 actualLabel=" + actualLabel);
        }
    }

    private static void runBoundaryFlipEqualsOracle(FuzzedDataProvider data) {
        try {
            LineAndShapeRenderer left = new LineAndShapeRenderer();
            LineAndShapeRenderer right = new LineAndShapeRenderer();

            int series = data.consumeInt(0, 3);
            Boolean visible = data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE;
            Boolean visibleInLegend = data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE;
            left.setSeriesVisible(series, visible, false);
            right.setSeriesVisible(series, visible, false);
            left.setSeriesVisibleInLegend(series, visibleInLegend);
            right.setSeriesVisibleInLegend(series, visibleInLegend);

            boolean lines = data.consumeBoolean();
            boolean shapes = data.consumeBoolean();
            left.setBaseLinesVisible(lines);
            right.setBaseLinesVisible(lines);
            left.setBaseShapesVisible(shapes);
            right.setBaseShapesVisible(shapes);

            if (!left.equals(right) || !right.equals(left)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equals-baseline-symmetric] semantic mismatch: twins configured identically must be equal before plot attachment");
            }

            CategoryPlot nullDatasetPlot = new CategoryPlot();
            nullDatasetPlot.setRenderer(left);

            CategoryPlot emptyDatasetPlot = new CategoryPlot();
            emptyDatasetPlot.setDataset(new DefaultCategoryDataset());
            emptyDatasetPlot.setRenderer(right);

            left.getLegendItems();
            right.getLegendItems();

            // Contract basis: the shown equals(Object) implementation for
            // AbstractCategoryItemRenderer compares renderer configuration fields
            // and delegates to super.equals(obj); it does not compare the plot field.
            // Therefore two identically configured renderers must remain equal even
            // after being attached to different plots and after getLegendItems()
            // reads through opposite sides of the patched dataset-null boundary.
            // A band-aid patch that mutates renderer state or smuggles plot/dataset
            // association into observable equality would violate this post-condition.
            if (!left.equals(right) || !right.equals(left)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equals-after-boundary-flip] metamorphic violation: equal renderers diverged after getLegendItems across null-vs-empty dataset boundary");
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        } catch (Error e) {
            return;
        }
    }
}