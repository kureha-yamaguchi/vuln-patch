package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedJUnitOracle();
        runDatasetReplacementOracle(data);
    }

    private static void runLiftedJUnitOracle() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-junit-not-null] semantic mismatch: expected non-null legend collection before plot assignment but was null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-junit-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection empty = r.getLegendItems();
        if (empty.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-junit-empty-count] semantic mismatch: expected=0 actual=" + empty.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-junit-single-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-junit-single-label] semantic mismatch: expected=S1 actual=" + label);
        }
    }

    private static void runDatasetReplacementOracle(FuzzedDataProvider data) {
        try {
            String row1 = nonEmpty(data.consumeAsciiString(8), "A");
            String col1 = nonEmpty(data.consumeAsciiString(8), "C1");
            String row2 = nonEmpty(data.consumeAsciiString(8), "B");
            String col2 = nonEmpty(data.consumeAsciiString(8), "C2");
            if (row1.equals(row2)) {
                row2 = row2 + "_X";
            }
            if (col1.equals(col2)) {
                col2 = col2 + "_Y";
            }

            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(r);

            // Contract used for this post-condition:
            // getLegendItems() returns legend items "for the series that this renderer is responsible for drawing".
            // getLegendItem() reads the CURRENT plot dataset and uses dataset.getRowKey(series) as the legend label/series key.
            // Therefore, replacing the plot's dataset must replace the visible legend content, and setting the dataset to null
            // must leave no series to report. A throw-deleting or seed-only patch can still leave this state transition wrong.
            plot.setDataset(null);
            LegendItemCollection none = r.getLegendItems();
            if (none == null) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-null-collection] semantic mismatch: expected non-null legend collection after null dataset but was null");
            }
            if (none.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-null-count] metamorphic violation: null dataset should yield zero legend items actual=" + none.getItemCount());
            }

            DefaultCategoryDataset first = new DefaultCategoryDataset();
            first.addValue(1.0, row1, col1);
            plot.setDataset(first);
            LegendItemCollection firstLic = r.getLegendItems();
            if (firstLic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-first-count] metamorphic violation: expected first replacement to expose exactly one series actual=" + firstLic.getItemCount());
            }
            String firstLabel = firstLic.get(0).getLabel();
            if (!row1.equals(firstLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-first-label] metamorphic violation: expected first label=" + row1 + " actual=" + firstLabel);
            }

            DefaultCategoryDataset second = new DefaultCategoryDataset();
            second.addValue(2.0, row2, col2);
            plot.setDataset(second);
            LegendItemCollection secondLic = r.getLegendItems();
            if (secondLic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-second-count] metamorphic violation: expected second replacement to expose exactly one series actual=" + secondLic.getItemCount());
            }
            String secondLabel = secondLic.get(0).getLabel();
            if (!row2.equals(secondLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-second-label] metamorphic violation: dataset replacement should update legend label expected=" + row2 + " actual=" + secondLabel + " previous=" + firstLabel);
            }
            if (row1.equals(row2)) {
                return;
            }
            if (row1.equals(secondLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-label-transition] metamorphic violation: legend label stayed stale across dataset replacement stale=" + row1 + " newExpected=" + row2);
            }

            plot.setDataset(null);
            LegendItemCollection cleared = r.getLegendItems();
            if (cleared.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:replace-clear-count] metamorphic violation: clearing dataset should clear legend items actual=" + cleared.getItemCount());
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }

    private static String nonEmpty(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}