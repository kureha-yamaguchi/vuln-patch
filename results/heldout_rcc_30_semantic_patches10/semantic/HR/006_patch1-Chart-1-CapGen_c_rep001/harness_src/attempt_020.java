package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedVerbatimTest2947660();
        runPlotAggregationOracle(data);
    }

    private static void runLiftedVerbatimTest2947660() {
        try {
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

            if (r.getLegendItems() == null) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-verbatim-notnull] semantic mismatch: r.getLegendItems() returned null");
            }

            int initialCount = r.getLegendItems().getItemCount();
            if (initialCount != 0) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-verbatim-initial-count] semantic mismatch: expected=0 actual=" + initialCount);
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            int emptyPlotCount = r.getLegendItems().getItemCount();
            if (emptyPlotCount != 0) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-verbatim-empty-plot-count] semantic mismatch: expected=0 actual=" + emptyPlotCount);
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();

            int singleCount = lic.getItemCount();
            if (singleCount != 1) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-verbatim-single-count] semantic mismatch: expected=1 actual=" + singleCount);
            }

            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-verbatim-single-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runPlotAggregationOracle(FuzzedDataProvider data) {
        try {
            String s1 = nonEmptyAscii(data.consumeAsciiString(8), "S1");
            String s2 = nonEmptyAscii(data.consumeAsciiString(8), "S2");
            String c1 = nonEmptyAscii(data.consumeAsciiString(8), "C1");
            String c2 = nonEmptyAscii(data.consumeAsciiString(8), "C2");
            if (s1.equals(s2)) {
                s2 = s1 + "_2";
            }
            if (c1.equals(c2)) {
                c2 = c1 + "_2";
            }

            DefaultCategoryDataset dataset0 = new DefaultCategoryDataset();
            DefaultCategoryDataset dataset1 = new DefaultCategoryDataset();

            dataset0.addValue(data.consumeInt(-1000, 1000), "IGNORED", c1);
            dataset1.addValue(data.consumeInt(-1000, 1000), s1, c1);
            dataset1.addValue(data.consumeInt(-1000, 1000), s1, c2);
            dataset1.addValue(data.consumeInt(-1000, 1000), s2, c1);

            LineAndShapeRenderer renderer0 = new LineAndShapeRenderer();
            LineAndShapeRenderer renderer1 = new LineAndShapeRenderer();

            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(0, dataset0);
            plot.setRenderer(0, renderer0);
            plot.setDataset(1, dataset1);
            plot.setRenderer(1, renderer1);

            LegendItemCollection direct = renderer1.getLegendItems();
            LegendItemCollection aggregated = plot.getLegendItems();

            if (direct == null || aggregated == null) {
                return;
            }

            int index = plot.getIndexOf(renderer1);
            if (index != 1) {
                return;
            }

            int directCount = direct.getItemCount();
            int aggregatedCount = aggregated.getItemCount();

            /*
             * Sound cross-check and strategy (c): the patched condition depends on
             * plot.getDataset(plot.getIndexOf(this)). Here the renderer is placed in
             * slot 1, not the default slot, to flip the boundary around that lookup.
             * For a correct implementation, CategoryPlot aggregates legend items from
             * its renderers, so the legend items contributed by renderer1 through
             * plot.getLegendItems() must match renderer1.getLegendItems().
             * A band-aid that special-cases only the seed path, or mishandles the
             * dataset-null/non-null condition for a non-zero renderer slot, can leave
             * one side wrong while the other observable still exposes the mismatch.
             */
            if (aggregatedCount < directCount) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:plot-aggregate-lower-bound] consistency violation: rendererIndex=" + index
                                + " directCount=" + directCount + " aggregatedCount=" + aggregatedCount);
            }

            boolean foundAll = true;
            for (int i = 0; i < directCount; i++) {
                String want = direct.get(i).getLabel();
                boolean found = false;
                for (int j = 0; j < aggregatedCount; j++) {
                    String have = aggregated.get(j).getLabel();
                    if (safeEquals(want, have)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    foundAll = false;
                    break;
                }
            }
            if (!foundAll) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:plot-aggregate-contains-renderer-items] consistency violation: rendererIndex=" + index
                                + " directCount=" + directCount + " aggregatedCount=" + aggregatedCount);
            }

            /*
             * Independent post-condition from the patched method's own contract:
             * getLegendItems() iterates seriesCount = dataset.getRowCount() and adds
             * at most one legend item per visible series. dataset1 has exactly two
             * row keys (s1, s2), so renderer1's direct legend count must equal 2.
             * This exercises the non-null side of the flipped condition at slot 1.
             */
            if (directCount != 2) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:nonzero-slot-two-series-count] semantic mismatch: expected=2 actual=" + directCount
                                + " rendererIndex=" + index);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static String nonEmptyAscii(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}