package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-count] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Contract: getPlot() "Returns the plot that the renderer has been assigned to";
        // setPlot()/plot.setRenderer establish that shared field, so reader and writer must agree.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-coupling] semantic mismatch: expected renderer plot identity to match assigned plot");
        }

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-notnull] semantic mismatch: r.getLegendItems() returned null after plot assignment");
        }
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-count] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-populated-notnull] semantic mismatch: populated legend collection was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-itemcount] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        try {
            LineAndShapeRenderer r2 = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            int seriesCount = data.consumeInt(1, 4);
            String[] expectedLabels = new String[seriesCount];
            for (int i = 0; i < seriesCount; i++) {
                String base = data.consumeAsciiString(8);
                if (base == null || base.length() == 0) {
                    base = "S";
                }
                expectedLabels[i] = base + "_" + i;
                dataset2.addValue(data.consumeInt(-1000, 1000), expectedLabels[i], "C1");
            }

            LegendItemCollection lic2 = r2.getLegendItems();

            // Post-condition from getLegendItems() contract: it returns legend items for the series
            // this renderer is responsible for drawing. For this constructed input, every series is
            // present, visible by default, and has a non-null row key, so a throw-deleting or
            // branch-skipping patch that returns an empty/stale collection violates this observable.
            if (lic2.getItemCount() != seriesCount) {
                throw new FuzzerSecurityIssueLow("[oracle:postcondition-series-count] semantic mismatch: expected=" + seriesCount + " actual=" + lic2.getItemCount());
            }

            for (int i = 0; i < seriesCount; i++) {
                String actualLabel = lic2.get(i).getLabel();
                if (!expectedLabels[i].equals(actualLabel)) {
                    throw new FuzzerSecurityIssueLow("[oracle:postcondition-label-order] semantic mismatch: index=" + i + " expected=" + expectedLabels[i] + " actual=" + String.valueOf(actualLabel));
                }
            }

            LegendItemCollection lic3 = r2.getLegendItems();
            if (lic3.getItemCount() != lic2.getItemCount()) {
                throw new RuntimeException("[oracle:metamorphic-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset changed itemCount lhs=" + lic2.getItemCount() + " rhs=" + lic3.getItemCount());
            }
            for (int i = 0; i < lic2.getItemCount(); i++) {
                String lhs = lic2.get(i).getLabel();
                String rhs = lic3.get(i).getLabel();
                if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:metamorphic-idempotence] metamorphic violation: repeated getLegendItems() on unchanged renderer/plot/dataset changed label index=" + i + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
                }
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}