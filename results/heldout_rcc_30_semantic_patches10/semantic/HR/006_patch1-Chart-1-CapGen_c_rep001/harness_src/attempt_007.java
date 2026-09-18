package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runTwinConsistencyOracle(data);
        runLiftedTestOracle();
    }

    private static void runLiftedTestOracle() {
        AbstractCategoryItemRenderer r;
        try {
            r = new LineAndShapeRenderer();
        } catch (Exception e) {
            return;
        }

        LegendItemCollection items0;
        try {
            items0 = r.getLegendItems();
        } catch (Exception e) {
            return;
        }
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-groundtruth] semantic mismatch: expected non-null legend items but got null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-initial-zero-groundtruth] semantic mismatch: expected initial itemCount=0 actual=" + items0.getItemCount());
        }

        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        try {
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);
        } catch (Exception e) {
            return;
        }

        LegendItemCollection items1;
        try {
            items1 = r.getLegendItems();
        } catch (Exception e) {
            return;
        }
        if (items1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-not-null-groundtruth] semantic mismatch: expected non-null legend items for empty attached dataset but got null");
        }
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-zero-groundtruth] semantic mismatch: expected empty attached dataset itemCount=0 actual=" + items1.getItemCount());
        }

        LegendItemCollection items2;
        try {
            dataset.addValue(1.0, "S1", "C1");
            items2 = r.getLegendItems();
        } catch (Exception e) {
            return;
        }
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-not-null-groundtruth] semantic mismatch: expected non-null legend items after adding one series but got null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-groundtruth] semantic mismatch: expected itemCount=1 actual=" + items2.getItemCount());
        }
        String label;
        try {
            label = items2.get(0).getLabel();
        } catch (Exception e) {
            return;
        }
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-groundtruth] semantic mismatch: expected label=S1 actual=" + label);
        }
    }

    private static void runTwinConsistencyOracle(FuzzedDataProvider data) {
        LineAndShapeRenderer r1;
        LineAndShapeRenderer r2;
        DefaultCategoryDataset d1;
        DefaultCategoryDataset d2;
        CategoryPlot p1;
        CategoryPlot p2;
        try {
            r1 = new LineAndShapeRenderer();
            r2 = new LineAndShapeRenderer();
            d1 = new DefaultCategoryDataset();
            d2 = new DefaultCategoryDataset();
            p1 = new CategoryPlot();
            p2 = new CategoryPlot();
            p1.setDataset(d1);
            p2.setDataset(d2);
            p1.setRenderer(r1);
            p2.setRenderer(r2);
            SortOrder order = data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING;
            p1.setRowRenderingOrder(order);
            p2.setRowRenderingOrder(order);
        } catch (Exception e) {
            return;
        }

        int seriesCount = data.consumeInt(1, 4);
        int visibleCount = 0;
        String[] expectedVisibleLabels = new String[seriesCount];

        try {
            for (int i = 0; i < seriesCount; i++) {
                String rowKey = data.consumeAsciiString(6);
                if (rowKey.length() == 0) {
                    rowKey = "R" + i;
                }
                String colKey = data.consumeAsciiString(6);
                if (colKey.length() == 0) {
                    colKey = "C" + i;
                }
                Number value = Integer.valueOf(data.consumeInt(-1000, 1000));
                d1.addValue(value, rowKey, colKey);
                d2.addValue(value, rowKey, colKey);

                boolean visibleInLegend = data.consumeBoolean();
                r1.setSeriesVisibleInLegend(i, visibleInLegend);
                r2.setSeriesVisibleInLegend(i, visibleInLegend);
                if (visibleInLegend) {
                    expectedVisibleLabels[visibleCount++] = rowKey;
                }
            }
        } catch (Exception e) {
            return;
        }

        // Documented/visible code guarantee: getLegendItems() is a getter that creates a new LegendItemCollection
        // from current plot/dataset/visibility state; it should not mutate renderer equality-relevant state.
        // Using a second identically-constructed renderer provides an independent recomputation path.
        boolean equalBefore;
        try {
            equalBefore = r1.equals(r2) && r2.equals(r1);
        } catch (Exception e) {
            return;
        }
        if (!equalBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:twin-equals-before] consistency violation: identically constructed renderers are not equal before getLegendItems");
        }

        LegendItemCollection c1;
        LegendItemCollection c2;
        try {
            c1 = r1.getLegendItems();
            c2 = r2.getLegendItems();
        } catch (Exception e) {
            return;
        }
        if (c1 == null || c2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:twin-nonnull-after] consistency violation: twin getLegendItems returned null c1=" + c1 + " c2=" + c2);
        }

        boolean equalAfter;
        try {
            equalAfter = r1.equals(r2) && r2.equals(r1);
        } catch (Exception e) {
            return;
        }
        if (!equalAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:twin-getter-no-mutate-equals] consistency violation: getLegendItems changed equality-relevant renderer state");
        }

        if (c1.getItemCount() != c2.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:twin-count-agreement] consistency violation: twin renderers disagree on legend item count lhs=" + c1.getItemCount() + " rhs=" + c2.getItemCount());
        }

        // Independent count oracle: the collection returned by getLegendItems() must contain exactly the
        // default-visible series. We establish visibility directly via setSeriesVisibleInLegend() on the same renderer.
        if (c1.getItemCount() != visibleCount) {
            throw new FuzzerSecurityIssueLow("[oracle:visible-count-recompute] consistency violation: legend item count disagrees with explicitly configured visible series count reported=" + c1.getItemCount() + " recomputed=" + visibleCount);
        }

        for (int i = 0; i < c1.getItemCount(); i++) {
            String l1;
            String l2;
            try {
                l1 = c1.get(i).getLabel();
                l2 = c2.get(i).getLabel();
            } catch (Exception e) {
                return;
            }
            if (l1 == null || l2 == null || !l1.equals(l2)) {
                throw new FuzzerSecurityIssueLow("[oracle:twin-label-agreement] consistency violation: twin renderers disagree on legend label at index " + i + " lhs=" + l1 + " rhs=" + l2);
            }
        }

        // For a correct implementation, each returned legend label must match the corresponding visible row key
        // in row-rendering order; a guard-only patch that suppresses population leaves this independently wrong.
        for (int i = 0; i < visibleCount; i++) {
            int expectedIndex = i;
            if (p1.getRowRenderingOrder().equals(SortOrder.DESCENDING)) {
                expectedIndex = visibleCount - 1 - i;
            }
            String expectedLabel = expectedVisibleLabels[expectedIndex];
            String actualLabel;
            try {
                actualLabel = c1.get(i).getLabel();
            } catch (Exception e) {
                return;
            }
            if (!expectedLabel.equals(actualLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:visible-label-order-recompute] consistency violation: legend label disagrees with visible series order expected=" + expectedLabel + " actual=" + actualLabel + " index=" + i + " order=" + p1.getRowRenderingOrder());
            }
        }
    }
}