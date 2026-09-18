package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        boolean lines = data.consumeBoolean();
        boolean shapes = data.consumeBoolean();

        AbstractCategoryItemRenderer r;
        try {
            r = new LineAndShapeRenderer(lines, shapes);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            LegendItemCollection initial = r.getLegendItems();
            if (initial == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() expected non-null but was null");
            }
            if (initial.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-initial] semantic mismatch: expected 0 but was " + initial.getItemCount());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        try {
            plot.setDataset(dataset);
            plot.setRenderer(r);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            LegendItemCollection withEmptyDataset = r.getLegendItems();
            if (withEmptyDataset.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot] semantic mismatch: expected 0 but was " + withEmptyDataset.getItemCount());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            // Contract from getPlot()/setPlot(): the renderer reports the plot it has been assigned to.
            // This shared-state check ensures the plot field consulted by getLegendItems agrees with the assigned plot.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state] semantic mismatch: expected renderer plot identity to match assigned plot");
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-count-one] semantic mismatch: expected 1 but was " + lic.getItemCount());
            }
            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-label] semantic mismatch: expected S1 but was " + String.valueOf(label));
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            // Metamorphic/post-condition check: getLegendItems() is a getter over renderer/plot/dataset state,
            // so repeated calls without intervening mutation must agree on observable contents.
            // A patch that silently drops or corrupts bookkeeping can violate this even if no exception is thrown.
            LegendItemCollection first = r.getLegendItems();
            LegendItemCollection second = r.getLegendItems();
            int c1 = first.getItemCount();
            int c2 = second.getItemCount();
            String l1 = c1 > 0 ? first.get(0).getLabel() : null;
            String l2 = c2 > 0 ? second.get(0).getLabel() : null;
            if (c1 != c2 || (l1 == null ? l2 != null : !l1.equals(l2))) {
                throw new RuntimeException("[oracle:getter-idempotence] metamorphic violation: repeated getLegendItems() calls disagreed count1=" + c1 + " count2=" + c2 + " label1=" + String.valueOf(l1) + " label2=" + String.valueOf(l2));
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (data.remainingBytes() > 0) {
            try {
                int extraSeries = data.consumeInt(0, 3);
                for (int i = 0; i < extraSeries; i++) {
                    String row = data.consumeAsciiString(8);
                    String col = data.consumeAsciiString(8);
                    if (row.length() == 0) {
                        row = "R" + i;
                    }
                    if (col.length() == 0) {
                        col = "K" + i;
                    }
                    dataset.addValue((double) data.consumeInt(-1000, 1000), row, col);
                    r.getLegendItems();
                }
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
        }
    }
}