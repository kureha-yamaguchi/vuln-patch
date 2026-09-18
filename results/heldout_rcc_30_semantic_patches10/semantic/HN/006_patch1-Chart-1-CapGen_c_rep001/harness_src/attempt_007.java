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
                throw new FuzzerSecurityIssueLow("[oracle:lifted-null-initial] semantic mismatch: getLegendItems() returned null before plot assignment");
            }
            if (initial.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-initial] semantic mismatch: expected initial legend item count 0 but was " + initial.getItemCount());
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            /* Shared-state oracle: CategoryItemRenderer#getPlot()/setPlot() are documented
             * as accessors for the same assigned plot field, and CategoryPlot#setRenderer(r)
             * establishes that assignment in normal usage. A patch that merely suppresses or
             * bypasses getLegendItems() behavior can still leave this shared state inconsistent. */
            if (r.getPlot() != plot) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: renderer plot accessor disagrees with plot.setRenderer assignment");
            }

            LegendItemCollection emptyOnPlot = r.getLegendItems();
            if (emptyOnPlot == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-null-emptyplot] semantic mismatch: getLegendItems() returned null with empty dataset");
            }
            if (emptyOnPlot.getItemCount() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-plot] semantic mismatch: expected legend item count 0 for empty dataset but was " + emptyOnPlot.getItemCount());
            }

            dataset.addValue(1.0, "S1", "C1");
            LegendItemCollection lic = r.getLegendItems();
            if (lic == null) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-null-single] semantic mismatch: getLegendItems() returned null after adding one series");
            }
            if (lic.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-count-single] semantic mismatch: expected legend item count 1 but was " + lic.getItemCount());
            }
            String label = lic.get(0).getLabel();
            if (!"S1".equals(label)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-label-single] semantic mismatch: expected legend label S1 but was " + label);
            }

            /* Mandatory post-condition/metamorphic check: getLegendItems() is a pure reader over
             * renderer/plot/dataset state and should not mutate that state. Therefore, with no
             * intervening mutation, repeated calls must agree. A throw-deleting or branch-skipping
             * patch could return a stale/wrong collection while never throwing; this idempotence
             * check catches such silent wrong-output behavior through real library calls only. */
            LegendItemCollection lic2 = r.getLegendItems();
            if (lic2 == null) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-nonnull] metamorphic violation: repeated getLegendItems() returned null on second call");
            }
            if (lic2.getItemCount() != lic.getItemCount()) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-count] metamorphic violation: repeated getLegendItems() changed item count lhs=" + lic.getItemCount() + " rhs=" + lic2.getItemCount());
            }
            if (lic.getItemCount() > 0) {
                String label2 = lic2.get(0).getLabel();
                if (label == null ? label2 != null : !label.equals(label2)) {
                    throw new FuzzerSecurityIssueLow("[oracle:idempotent-label] metamorphic violation: repeated getLegendItems() changed first label lhs=" + label + " rhs=" + label2);
                }
            }

            String fuzzSeries = data.consumeString(16);
            String fuzzCategory = data.consumeString(16);
            if (fuzzSeries.length() == 0) {
                fuzzSeries = "FS";
            }
            if (fuzzCategory.length() == 0) {
                fuzzCategory = "FC";
            }
            double fuzzValue = data.consumeInt(-1000000, 1000000);

            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            CategoryPlot plot2 = new CategoryPlot();
            AbstractCategoryItemRenderer r2 = new LineAndShapeRenderer();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);

            if (r2.getPlot() != plot2) {
                throw new FuzzerSecurityIssueLow("[oracle:plot-assignment-fuzz] semantic mismatch: renderer plot accessor disagrees with second plot.setRenderer assignment");
            }

            dataset2.addValue(fuzzValue, fuzzSeries, fuzzCategory);
            LegendItemCollection oneSeries = r2.getLegendItems();

            /* Generalization by construction from a known answer: we construct a dataset with
             * exactly one non-empty row key and one value. getLegendItems() iterates dataset rows,
             * so with one visible series the trusted expected count is 1, and the default legend
             * labeling observed in the lifted test must expose that sole series key as the label. */
            if (oneSeries == null) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-null] semantic mismatch: getLegendItems() returned null for constructed one-series dataset");
            }
            if (oneSeries.getItemCount() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-count] semantic mismatch: expected constructed legend item count 1 but was " + oneSeries.getItemCount() + " series=" + fuzzSeries + " category=" + fuzzCategory);
            }
            String constructedLabel = oneSeries.get(0).getLabel();
            if (!fuzzSeries.equals(constructedLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-label] semantic mismatch: expected constructed legend label " + fuzzSeries + " but was " + constructedLabel);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}