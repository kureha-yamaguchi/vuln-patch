package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedTest2947660Oracles();

        String rowKey = nonEmpty(data.consumeAsciiString(16), "R");
        String columnKey = nonEmpty(data.consumeAsciiString(16), "C");
        double value = data.consumeInt(-1000, 1000);

        runPlotAssociationEqualityOracle(rowKey, columnKey, value, data.consumeBoolean());
    }

    private static void runLiftedTest2947660Oracles() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-notnull-exact] semantic mismatch: r.getLegendItems() returned null");
        }

        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-zero-initial-exact] semantic mismatch: expected=0 actual="
                    + r.getLegendItems().getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-zero-empty-plot-exact] semantic mismatch: expected=0 actual="
                    + r.getLegendItems().getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();

        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-single-count-verbatim] semantic mismatch: expected=1 actual="
                    + lic.getItemCount());
        }

        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-single-label-verbatim] semantic mismatch: expected=S1 actual=" + label);
        }
    }

    private static void runPlotAssociationEqualityOracle(
            String rowKey, String columnKey, double value, boolean descendingSecondPlot) {
        try {
            LineAndShapeRenderer left = new LineAndShapeRenderer();
            LineAndShapeRenderer right = new LineAndShapeRenderer();

            DefaultCategoryDataset leftDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset rightDataset = new DefaultCategoryDataset();
            leftDataset.addValue(value, rowKey, columnKey);
            rightDataset.addValue(value, rowKey, columnKey);

            CategoryPlot leftPlot = new CategoryPlot();
            CategoryPlot rightPlot = new CategoryPlot();
            leftPlot.setDataset(leftDataset);
            rightPlot.setDataset(rightDataset);
            leftPlot.setRenderer(left);
            rightPlot.setRenderer(right);

            if (descendingSecondPlot) {
                rightPlot.setRowRenderingOrder(SortOrder.DESCENDING);
            } else {
                rightPlot.setRowRenderingOrder(SortOrder.ASCENDING);
            }

            /*
             * Contract / code-backed guarantee:
             * - setPlot()/getPlot() document plot assignment as external association state.
             * - The shown AbstractCategoryItemRenderer.equals(Object) implementation does not
             *   compare the 'plot' field at all; it compares renderer configuration fields only
             *   and then delegates to super.equals(obj).
             * Therefore, attaching equal renderers to different plots (even with different plot
             * row-rendering order) must not change renderer equality. A cover-up patch that only
             * suppresses the known wrong legend count but leaves plot-coupled renderer state
             * inconsistent would violate this independent observable.
             */
            left.getLegendItems();
            right.getLegendItems();

            boolean eqLeftRight = left.equals(right);
            boolean eqRightLeft = right.equals(left);
            if (!eqLeftRight || !eqRightLeft) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:equals-ignores-plot-association] metamorphic violation: "
                        + "leftEqualsRight=" + eqLeftRight
                        + " rightEqualsLeft=" + eqRightLeft
                        + " leftPlotAssigned=" + (left.getPlot() == leftPlot)
                        + " rightPlotAssigned=" + (right.getPlot() == rightPlot)
                        + " rightRowOrder=" + rightPlot.getRowRenderingOrder());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
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