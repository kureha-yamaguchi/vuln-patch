package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: r.getLegendItems() returned null");
        }
        if (initial.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-no-plot] semantic mismatch: expected=0 actual=" + initial.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // Documented sibling agreement on shared field 'plot':
        // getPlot() "Returns the plot that the renderer has been assigned to";
        // setPlot(CategoryPlot) "Sets the plot that the renderer has been assigned to."
        // CategoryPlot.setRenderer(r) is the real API path that assigns the renderer to the plot,
        // so after that call the renderer must report the same plot. A "fix" that only dodges the
        // wrong branch in getLegendItems but breaks/omits renderer assignment would violate this.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer plot to be the assigned CategoryPlot");
        }

        LegendItemCollection emptyOnAssignedPlot = r.getLegendItems();
        if (emptyOnAssignedPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-empty-dataset] semantic mismatch: expected=0 actual=" + emptyOnAssignedPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-after-add] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-label-after-add] semantic mismatch: expected=S1 actual=" + String.valueOf(label));
        }

        // Post-condition / metamorphic check:
        // getLegendItems() is a read-only query over the renderer/plot/dataset state. With no state
        // changes between calls, repeated calls must agree on the observable legend contents. A patch
        // that merely makes the buggy branch unreachable or silently skips legend population could
        // break this getter's intended behavior; checking stability ensures we read back the state
        // that was already established by the real API setup above.
        LegendItemCollection licAgain = r.getLegendItems();
        if (licAgain.getItemCount() != lic.getItemCount()) {
            throw new RuntimeException("[oracle:idempotent-legend] metamorphic violation: repeated getLegendItems() changed itemCount lhs=" + lic.getItemCount() + " rhs=" + licAgain.getItemCount());
        }
        if (licAgain.getItemCount() > 0) {
            String labelAgain = licAgain.get(0).getLabel();
            if ((label == null && labelAgain != null) || (label != null && !label.equals(labelAgain))) {
                throw new RuntimeException("[oracle:idempotent-legend-label] metamorphic violation: repeated getLegendItems() changed first label lhs=" + String.valueOf(label) + " rhs=" + String.valueOf(labelAgain));
            }
        }

        try {
            int extraSeries = data.consumeInt(0, 4);
            DefaultCategoryDataset fuzzDataset = new DefaultCategoryDataset();
            CategoryPlot fuzzPlot = new CategoryPlot();
            LineAndShapeRenderer fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);

            if (fuzzRenderer.getPlot() != fuzzPlot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: expected renderer plot to be the assigned CategoryPlot");
            }

            for (int i = 0; i < extraSeries; i++) {
                String row = data.consumeAsciiString(8);
                String col = data.consumeAsciiString(8);
                if (row.length() == 0) {
                    row = "R" + i;
                }
                if (col.length() == 0) {
                    col = "C" + i;
                }
                double v = data.consumeInt(-1000, 1000);
                fuzzDataset.addValue(v, row, col);
            }

            LegendItemCollection a = fuzzRenderer.getLegendItems();
            LegendItemCollection b = fuzzRenderer.getLegendItems();
            if (a.getItemCount() != b.getItemCount()) {
                throw new RuntimeException("[oracle:idempotent-legend-fuzz] metamorphic violation: repeated getLegendItems() changed itemCount lhs=" + a.getItemCount() + " rhs=" + b.getItemCount());
            }
            for (int i = 0; i < a.getItemCount(); i++) {
                String la = a.get(i).getLabel();
                String lb = b.get(i).getLabel();
                if ((la == null && lb != null) || (la != null && !la.equals(lb))) {
                    throw new RuntimeException("[oracle:idempotent-legend-fuzz-label] metamorphic violation: repeated getLegendItems() changed label index=" + i + " lhs=" + String.valueOf(la) + " rhs=" + String.valueOf(lb));
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}