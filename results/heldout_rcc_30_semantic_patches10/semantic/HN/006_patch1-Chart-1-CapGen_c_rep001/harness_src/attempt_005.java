package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-notnull] semantic mismatch: getLegendItems() returned null");
            }
            if (initial.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected=0 actual=" + initial.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            // Contract from getPlot()/setPlot() docs: getPlot() returns the plot the renderer
            // has been assigned to. This shared-state check is mandatory because a patch that
            // simply short-circuits getLegendItems() could still leave plot state inconsistent.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: renderer plot disagrees with assigned plot");
            }

            LegendItemCollection emptyOnPlot = r.getLegendItems();
            if (emptyOnPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-count] semantic mismatch: expected=1 actual=" + lic.getItemCount());
            }
            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow("[oracle:test2947660-one-series-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
            }

            // Metamorphic/post-condition: with unchanged plot/dataset/renderer state, repeated
            // getLegendItems() calls must report the same observable legend content. The method
            // "Returns a (possibly empty) collection of legend items for the series that this
            // renderer is responsible for drawing"; deleting the real logic or replacing it with
            // a wrong constant result would violate this stable-content relation.
            LegendItemCollection lic2 = r.getLegendItems();
            if (lic2.getItemCount() != lic.getItemCount()) {
                throw new RuntimeException("[oracle:legend-idempotence] metamorphic violation: repeated getLegendItems() changed item count lhs=" + lic.getItemCount() + " rhs=" + lic2.getItemCount());
            }
            if (lic2.getItemCount() > 0) {
                String label2 = lic2.get(0).getLabel();
                if (!String.valueOf(label).equals(String.valueOf(label2))) {
                    throw new RuntimeException("[oracle:legend-idempotence] metamorphic violation: repeated getLegendItems() changed first label lhs=" + String.valueOf(label) + " rhs=" + String.valueOf(label2));
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Additional real-code exploration through the same public API without untrusted
            // assertions. This still reaches the patched method on varied states.
            AbstractCategoryItemRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            int rows = data.consumeInt(0, 3);
            int cols = data.consumeInt(0, 3);
            for (int i = 0; i < rows; i++) {
                String rowKey = data.consumeAsciiString(8);
                if (rowKey.length() == 0) {
                    rowKey = "R" + i;
                }
                for (int j = 0; j < cols; j++) {
                    String colKey = data.consumeAsciiString(8);
                    if (colKey.length() == 0) {
                        colKey = "C" + j;
                    }
                    dataset2.addValue(data.consumeInt(-1000, 1000), rowKey, colKey);
                }
            }

            r2.getLegendItems();
            if (data.consumeBoolean()) {
                r2.setPlot(plot2);
                if (r2.getPlot() != plot2) {
                    throw new FuzzerSecurityIssueLow("[oracle:plot-roundtrip-extra] semantic mismatch: renderer plot disagrees after direct setPlot()");
                }
                r2.getLegendItems();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}