package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedTestOracle();
        runDifferentReachableFunctionOracle(data);
    }

    private static void runLiftedTestOracle() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection items0 = r.getLegendItems();
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-initial] semantic mismatch: expected non-null legend items but got null");
        }
        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-initial] semantic mismatch: expected 0 but got " + r.getLegendItems().getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-empty-dataset] semantic mismatch: expected 0 but got " + r.getLegendItems().getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-single-series] semantic mismatch: expected 1 but got " + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label-single-series] semantic mismatch: expected S1 but got " + label);
        }
    }

    private static void runDifferentReachableFunctionOracle(FuzzedDataProvider data) {
        try {
            LineAndShapeRenderer a = new LineAndShapeRenderer();
            LineAndShapeRenderer b = new LineAndShapeRenderer();

            int series = data.consumeInt(0, 3);
            boolean visibleInLegend = data.consumeBoolean();
            a.setSeriesVisibleInLegend(series, visibleInLegend);
            b.setSeriesVisibleInLegend(series, visibleInLegend);

            DefaultCategoryDataset ds1 = new DefaultCategoryDataset();
            DefaultCategoryDataset ds2 = new DefaultCategoryDataset();
            String row = nonEmptyAscii(data, 8, "R");
            String col = nonEmptyAscii(data, 8, "C");
            Number value = Integer.valueOf(data.consumeInt(-1000, 1000));
            ds1.addValue(value, row, col);
            ds2.addValue(value, row, col);

            CategoryPlot p1 = new CategoryPlot();
            CategoryPlot p2 = new CategoryPlot();
            p1.setDataset(ds1);
            p2.setDataset(ds2);
            p1.setRenderer(a);
            p2.setRenderer(b);

            a.getLegendItems();

            boolean ab = a.equals(b);
            boolean ba = b.equals(a);

            // equals() is in the reachable region. For any correct implementation, equality is symmetric.
            // A band-aid that perturbs hidden renderer state while serving getLegendItems() can violate this.
            if (ab != ba) {
                throw new FuzzerSecurityIssueLow("[oracle:equals-symmetry] metamorphic violation: a.equals(b)=" + ab + " b.equals(a)=" + ba);
            }

            // Object contract: if equals() says two objects are equal, their hash codes must match.
            // This is an independent post-condition on a different reachable function than getLegendItems().
            if (ab && a.hashCode() != b.hashCode()) {
                throw new FuzzerSecurityIssueLow("[oracle:equals-hash-consistency] consistency violation: equals=true but hashCodes differ lhs=" + a.hashCode() + " rhs=" + b.hashCode());
            }

            // CategoryPlot.getIndexOf(CategoryItemRenderer) is also in the reachable region.
            // After setRenderer(renderer), the plot must report that renderer at slot 0.
            int idx1 = p1.getIndexOf(a);
            int idx2 = p2.getIndexOf(b);
            if (idx1 != 0 || idx2 != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-index-after-set] consistency violation: expected indices 0 and 0 but got " + idx1 + " and " + idx2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return;
        }
    }

    private static String nonEmptyAscii(FuzzedDataProvider data, int maxLen, String fallback) {
        String s = data.consumeAsciiString(Math.max(1, maxLen));
        return s.length() == 0 ? fallback : s;
    }
}