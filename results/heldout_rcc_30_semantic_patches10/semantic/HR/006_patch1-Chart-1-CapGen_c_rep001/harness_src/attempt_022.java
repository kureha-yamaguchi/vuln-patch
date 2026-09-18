package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedJUnitPairsFromTest2947660();

        String row1 = nonEmpty(data.consumeString(12), "S1");
        String row2 = nonEmpty(data.consumeString(12), "S2");
        if (row1.equals(row2)) {
            row2 = row1 + "_2";
        }
        String col1 = nonEmpty(data.consumeString(12), "C1");
        String col2 = nonEmpty(data.consumeString(12), "C2");
        if (col1.equals(col2)) {
            col2 = col1 + "_2";
        }

        runLegendValueIndependenceMetamorphic(row1, row2, col1, col2,
                bounded(data.consumeInt()),
                bounded(data.consumeInt()),
                bounded(data.consumeInt()),
                bounded(data.consumeInt()));

        exercisePatchedPathWithoutOracle(data, row1, row2, col1, col2);
    }

    private static void runLiftedJUnitPairsFromTest2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection prePlot = r.getLegendItems();
        if (prePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pairs-notnull] semantic mismatch: r.getLegendItems() returned null before plot attachment");
        }
        if (prePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pairs-preplot-count] semantic mismatch: expected=0 actual=" + prePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyPlotItems = r.getLegendItems();
        if (emptyPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pairs-empty-dataset-count] semantic mismatch: expected=0 actual=" + emptyPlotItems.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pairs-single-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pairs-single-label] semantic mismatch: expected=S1 actual=" + String.valueOf(actualLabel));
        }
    }

    private static void runLegendValueIndependenceMetamorphic(
            String row1, String row2, String col1, String col2,
            int a1, int a2, int b1, int b2) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer();
            CategoryPlot plot = new CategoryPlot();

            DefaultCategoryDataset datasetA = new DefaultCategoryDataset();
            datasetA.addValue(a1, row1, col1);
            datasetA.addValue(a2, row2, col1);

            DefaultCategoryDataset datasetB = new DefaultCategoryDataset();
            datasetB.addValue(b1, row1, col2);
            datasetB.addValue(b2, row2, col2);

            plot.setDataset(datasetA);
            plot.setRenderer(r);
            LegendItemCollection itemsA = r.getLegendItems();

            plot.setDataset(datasetB);
            LegendItemCollection itemsB = r.getLegendItems();

            /* Contract justification:
               getLegendItems() iterates over dataset.getRowCount(), and getLegendItem()
               generates labels from the series (row) key. The numeric values are not used
               to decide legend count or labels. Therefore, for two datasets with the same
               row keys in the same order, a correct implementation must report identical
               legend item count and identical labels even if all values and column keys differ.
               A band-aid patch that merely suppresses one symptom could still leave this
               legend-summary relation wrong. */
            if (itemsA.getItemCount() != itemsB.getItemCount()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:value-independence-count] consistency violation: countA="
                                + itemsA.getItemCount() + " countB=" + itemsB.getItemCount());
            }
            for (int i = 0; i < itemsA.getItemCount(); i++) {
                String la = itemsA.get(i).getLabel();
                String lb = itemsB.get(i).getLabel();
                if (la == null ? lb != null : !la.equals(lb)) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:value-independence-labels] metamorphic violation: index="
                                    + i + " labelA=" + String.valueOf(la)
                                    + " labelB=" + String.valueOf(lb));
                }
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }
    }

    private static void exercisePatchedPathWithoutOracle(FuzzedDataProvider data,
            String row1, String row2, String col1, String col2) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
            CategoryPlot plot = new CategoryPlot();
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            int rows = data.consumeInt(1, 3);
            int cols = data.consumeInt(1, 3);
            for (int i = 0; i < rows; i++) {
                String row = (i == 0) ? row1 : (i == 1 ? row2 : nonEmpty(data.consumeAsciiString(8), "R" + i));
                for (int j = 0; j < cols; j++) {
                    String col = (j == 0) ? col1 : (j == 1 ? col2 : nonEmpty(data.consumeAsciiString(8), "C" + j));
                    if (data.consumeBoolean()) {
                        dataset.addValue(bounded(data.consumeInt()), row, col);
                    } else {
                        dataset.addValue((Number) null, row, col);
                    }
                }
            }

            plot.setDataset(dataset);
            plot.setRenderer(r);
            r.getLegendItems();
        } catch (Throwable t) {
        }
    }

    private static int bounded(int x) {
        if (x == Integer.MIN_VALUE) {
            return 0;
        }
        x = Math.abs(x);
        return x % 1000000;
    }

    private static String nonEmpty(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}