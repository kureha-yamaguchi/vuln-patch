package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exactLiftedTestOracle();
        noPlotOracle();
        singleSeriesFuzzOracle(data);
    }

    private static void exactLiftedTestOracle() {
        AbstractCategoryItemRenderer r;
        LegendItemCollection items0;
        LegendItemCollection items1;
        LegendItemCollection items2;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        try {
            r = new LineAndShapeRenderer();

            items0 = r.getLegendItems();
            if (items0 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-initial] semantic mismatch: expected non-null legend collection but was null");
            }
            if (items0.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-initial] semantic mismatch: expected 0 but was " + items0.getItemCount());
            }

            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state-coupling] semantic mismatch: expected getPlot() to return assigned plot instance");
            }

            items1 = r.getLegendItems();
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-readonly-query] metamorphic violation: getLegendItems() is documented as a query over the renderer's assigned plot and should not change the shared plot field; beforeAfterPlotIdentity differed");
            }
            if (items1 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-empty-dataset] semantic mismatch: expected non-null legend collection but was null");
            }
            if (items1.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-empty-dataset] semantic mismatch: expected 0 but was " + items1.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            items2 = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-single-series] semantic mismatch: expected non-null legend collection but was null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-count] semantic mismatch: expected 1 but was " + items2.getItemCount());
        }
        String label = items2.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-label] semantic mismatch: expected S1 but was " + label);
        }

        LegendItemCollection items3;
        try {
            items3 = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (items3 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-non-null] metamorphic violation: repeated getLegendItems() returned null");
        }
        int count3 = items3.getItemCount();
        String label3 = count3 > 0 ? items3.get(0).getLabel() : null;
        if (count3 != items2.getItemCount() || (label == null ? label3 != null : !label.equals(label3))) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-query] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset should agree for count and first label; firstCount="
                    + items2.getItemCount() + " secondCount=" + count3 + " firstLabel=" + label + " secondLabel=" + label3);
        }
    }

    private static void noPlotOracle() {
        LineAndShapeRenderer r;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            items = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-not-null] semantic mismatch: expected non-null legend collection but was null");
        }
        if (items.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-empty] semantic mismatch: expected 0 but was " + items.getItemCount());
        }
        if (r.getPlot() != null) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-state] semantic mismatch: expected getPlot() to be null for fresh renderer");
        }
    }

    private static void singleSeriesFuzzOracle(FuzzedDataProvider data) {
        String rowKey = sanitizeKey(data.consumeAsciiString(8), "S");
        String columnKey = sanitizeKey(data.consumeAsciiString(8), "C");
        double value = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer r;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            plot = new CategoryPlot();
            dataset = new DefaultCategoryDataset();
            plot.setDataset(dataset);
            plot.setRenderer(r);
            dataset.addValue(value, rowKey, columnKey);
            items = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-single-series-not-null] relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-single-series-count] relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount());
        }
        String label = items.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-single-series-label] relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label);
        }

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-state-coupling] semantic mismatch: expected getPlot() to return assigned plot instance");
        }

        LegendItemCollection itemsAgain;
        try {
            itemsAgain = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (itemsAgain == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-non-null] metamorphic violation: repeated getLegendItems() returned null");
        }
        int countAgain = itemsAgain.getItemCount();
        String labelAgain = countAgain > 0 ? itemsAgain.get(0).getLabel() : null;
        if (countAgain != 1 || !rowKey.equals(labelAgain)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-query] metamorphic violation: repeated getLegendItems() on unchanged single-series dataset should still report one item labeled with the series key; count="
                    + countAgain + " label=" + labelAgain + " expectedLabel=" + rowKey);
        }
    }

    private static String sanitizeKey(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}