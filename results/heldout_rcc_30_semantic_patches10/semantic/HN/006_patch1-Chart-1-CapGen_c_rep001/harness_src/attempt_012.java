package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        liftedTestOracles();
        relationNoPlotEmptyAndNonNull();
        relationSingleSeriesNonNullDataset(data);
    }

    private static void liftedTestOracles() {
        AbstractCategoryItemRenderer r;
        LegendItemCollection itemsNoPlot;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        LegendItemCollection itemsEmptyDataset;
        LegendItemCollection itemsSingleSeries;
        try {
            r = new LineAndShapeRenderer();

            itemsNoPlot = r.getLegendItems();
            if (itemsNoPlot == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-no-plot] semantic mismatch: expected non-null legend items but was null");
            }
            if (itemsNoPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-no-plot] semantic mismatch: expected 0 but was " + itemsNoPlot.getItemCount());
            }

            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            // Shared-state agreement: CategoryPlot.setRenderer(r) establishes the renderer's plot;
            // getPlot() is documented to report the plot assigned to the renderer, so they must agree.
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: expected renderer.getPlot()==assigned plot");
            }

            itemsEmptyDataset = r.getLegendItems();
            if (itemsEmptyDataset.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-with-empty-dataset] semantic mismatch: expected 0 but was " + itemsEmptyDataset.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            itemsSingleSeries = r.getLegendItems();

            if (itemsSingleSeries.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-count] semantic mismatch: expected 1 but was " + itemsSingleSeries.getItemCount());
            }
            if (!"S1".equals(itemsSingleSeries.get(0).getLabel())) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-label] semantic mismatch: expected S1 but was " + itemsSingleSeries.get(0).getLabel());
            }

            // Post-condition / metamorphic check: getLegendItems() is a query method and the setup is unchanged,
            // so repeated calls must agree on the observable legend contents. A "fix" that only skips logic or
            // mutates hidden state during a read would violate this idempotent read-only behaviour.
            LegendItemCollection secondRead = r.getLegendItems();
            if (secondRead == null) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-read-non-null] semantic mismatch: repeated getLegendItems() returned null");
            }
            if (secondRead.getItemCount() != itemsSingleSeries.getItemCount()) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-read-count] semantic mismatch: firstCount="
                        + itemsSingleSeries.getItemCount() + " secondCount=" + secondRead.getItemCount());
            }
            if (secondRead.getItemCount() > 0) {
                String firstLabel = itemsSingleSeries.get(0).getLabel();
                String secondLabel = secondRead.get(0).getLabel();
                if (firstLabel == null ? secondLabel != null : !firstLabel.equals(secondLabel)) {
                    throw new FuzzerSecurityIssueLow("[oracle:idempotent-read-label] semantic mismatch: firstLabel="
                            + firstLabel + " secondLabel=" + secondLabel);
                }
            }
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-plot-state] semantic mismatch: getLegendItems() changed renderer plot association");
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }

    private static void relationNoPlotEmptyAndNonNull() {
        LineAndShapeRenderer r;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            items = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-noPlot-emptyAndNonNull] relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-noPlot-emptyAndNonNull] relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items.getItemCount());
        }
    }

    private static void relationSingleSeriesNonNullDataset(FuzzedDataProvider data) {
        LineAndShapeRenderer r;
        CategoryPlot plot;
        DefaultCategoryDataset dataset;
        String rowKey;
        String columnKey;
        LegendItemCollection items;
        try {
            r = new LineAndShapeRenderer();
            plot = new CategoryPlot();
            dataset = new DefaultCategoryDataset();

            rowKey = data.consumeAsciiString(8);
            if (rowKey.length() == 0) {
                rowKey = "S";
            }
            columnKey = data.consumeAsciiString(8);
            if (columnKey.length() == 0) {
                columnKey = "C";
            }

            plot.setDataset(dataset);
            plot.setRenderer(r);
            dataset.addValue(1.0, rowKey, columnKey);

            items = r.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (items == null) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-singleSeries-nonNullDataset] relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-singleSeries-nonNullDataset] relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + items.getItemCount());
        }
        String label = items.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-singleSeries-nonNullDataset] relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + label);
        }

        // Additional same-state agreement: the renderer was attached through plot.setRenderer(r),
        // so getPlot() must still report that exact plot after the query.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-plot-agreement] relation plot-state agreement violated: renderer.getPlot() != plot after setRenderer/getLegendItems");
        }
    }
}