package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runLiftedOracles();
            runFuzzedGeneralization(data);
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        }
    }

    private static void runLiftedOracles() {
        AbstractCategoryItemRenderer r;
        LegendItemCollection itemsBeforePlot1;
        LegendItemCollection itemsBeforePlot2;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        LegendItemCollection itemsEmptyDataset;
        LegendItemCollection itemsAfterAdd;

        try {
            r = new LineAndShapeRenderer();

            itemsBeforePlot1 = r.getLegendItems();
            itemsBeforePlot2 = r.getLegendItems();

            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            itemsEmptyDataset = r.getLegendItems();

            dataset.addValue(1.0, "S1", "C1");
            itemsAfterAdd = r.getLegendItems();
        } catch (Exception e) {
            return;
        }

        if (itemsBeforePlot1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null] semantic mismatch: expected non-null legend items before plot assignment but got null");
        }
        if (itemsBeforePlot2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-repeat] semantic mismatch: expected non-null legend items on repeated pre-plot call but got null");
        }
        if (itemsBeforePlot1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pre-plot-count] semantic mismatch: expected 0 legend items before plot assignment but got " + itemsBeforePlot1.getItemCount());
        }
        if (itemsBeforePlot2.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-pre-plot-count-repeat] semantic mismatch: expected 0 legend items on repeated pre-plot call but got " + itemsBeforePlot2.getItemCount());
        }
        if (itemsEmptyDataset == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-not-null] semantic mismatch: expected non-null legend items for empty attached dataset but got null");
        }
        if (itemsEmptyDataset.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-count] semantic mismatch: expected 0 legend items for empty attached dataset but got " + itemsEmptyDataset.getItemCount());
        }
        if (itemsAfterAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-not-null] semantic mismatch: expected non-null legend items after adding one series but got null");
        }
        if (itemsAfterAdd.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got " + itemsAfterAdd.getItemCount());
        }
        String label = itemsAfterAdd.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-series-label] semantic mismatch: expected first legend label \"S1\" after dataset.addValue(1.0, \"S1\", \"C1\") but got " + String.valueOf(label));
        }

        // getPlot()/setPlot share the same field used by getLegendItems(); the plot we attached must be observable back.
        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-coupling] semantic mismatch: setRenderer/setPlot established a plot, but getPlot() did not return the same instance");
        }

        // Consistency cross-check for strategy (b): getLegendItems() reports a collection over the renderer's series.
        // Independently recompute the same count and first label via getLegendItem(index, series) over dataset rows.
        // A band-aid that suppresses the wrong branch or returns a stale cached collection would break this agreement.
        int manualCount = 0;
        String manualFirstLabel = null;
        int datasetIndex = plot.getIndexOf(r);
        int seriesCount = dataset.getRowCount();
        for (int i = 0; i < seriesCount; i++) {
            LegendItem li;
            try {
                li = r.getLegendItem(datasetIndex, i);
            } catch (Exception e) {
                return;
            }
            if (li != null) {
                if (manualFirstLabel == null) {
                    manualFirstLabel = li.getLabel();
                }
                manualCount++;
            }
        }
        if (itemsAfterAdd.getItemCount() != manualCount) {
            throw new FuzzerSecurityIssueLow("[oracle:consistency-count] consistency violation: getLegendItems().getItemCount()=" + itemsAfterAdd.getItemCount() + " manualCountViaGetLegendItem=" + manualCount);
        }
        if (itemsAfterAdd.getItemCount() > 0) {
            String collectionFirstLabel = itemsAfterAdd.get(0).getLabel();
            if (manualFirstLabel == null || !collectionFirstLabel.equals(manualFirstLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:consistency-first-label] consistency violation: collectionFirstLabel=" + String.valueOf(collectionFirstLabel) + " manualFirstLabel=" + String.valueOf(manualFirstLabel));
            }
        }

        // Read-only/idempotence post-condition: getLegendItems() is a question method returning legend items;
        // repeated calls on unchanged state must agree, so a patch that silently mutates internal state is caught.
        LegendItemCollection secondRead;
        try {
            secondRead = r.getLegendItems();
        } catch (Exception e) {
            return;
        }
        if (secondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-not-null] metamorphic violation: repeated getLegendItems() returned null after prior successful read");
        }
        if (secondRead.getItemCount() != itemsAfterAdd.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:idempotent-count] metamorphic violation: repeated getLegendItems() changed itemCount from " + itemsAfterAdd.getItemCount() + " to " + secondRead.getItemCount());
        }
        if (secondRead.getItemCount() > 0) {
            String first1 = itemsAfterAdd.get(0).getLabel();
            String first2 = secondRead.get(0).getLabel();
            if (first1 == null ? first2 != null : !first1.equals(first2)) {
                throw new FuzzerSecurityIssueLow("[oracle:idempotent-label] metamorphic violation: repeated getLegendItems() changed first label from " + String.valueOf(first1) + " to " + String.valueOf(first2));
            }
        }
    }

    private static void runFuzzedGeneralization(FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r;
        DefaultCategoryDataset ds;
        CategoryPlot plot;
        String rowKey;
        String colKey;
        int value;
        LegendItemCollection before;
        LegendItemCollection after;

        try {
            r = new LineAndShapeRenderer();
            ds = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(ds);
            plot.setRenderer(r);

            rowKey = nonEmptyAscii(data, 8, "R");
            colKey = nonEmptyAscii(data, 8, "C");
            value = data.consumeInt(-1000, 1000);

            before = r.getLegendItems();
            ds.addValue(value, rowKey, colKey);
            after = r.getLegendItems();
        } catch (Exception e) {
            return;
        }

        if (before == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-before-not-null] semantic mismatch: expected non-null legend items for attached empty dataset but got null");
        }
        if (after == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-after-not-null] semantic mismatch: expected non-null legend items after first series addition but got null");
        }

        // Generalised from the failing test and the method contract: with one attached default-visible series,
        // that series must contribute exactly one legend item.
        if (before.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-before-count] semantic mismatch: expected 0 legend items before first series addition but got " + before.getItemCount());
        }
        if (after.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-after-count] semantic mismatch: expected 1 legend item after first series addition but got " + after.getItemCount() + " rowKey=" + rowKey + " colKey=" + colKey + " value=" + value);
        }
        String label = after.get(0).getLabel();
        if (!rowKey.equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-after-label] semantic mismatch: expected first legend label " + rowKey + " but got " + String.valueOf(label) + " colKey=" + colKey + " value=" + value);
        }

        // Strategy (b) helper cross-check: collection size should equal independent manual counting via getLegendItem().
        int datasetIndex = plot.getIndexOf(r);
        int manualCount = 0;
        String manualLabel = null;
        for (int i = 0; i < ds.getRowCount(); i++) {
            LegendItem li;
            try {
                li = r.getLegendItem(datasetIndex, i);
            } catch (Exception e) {
                return;
            }
            if (li != null) {
                manualCount++;
                if (manualLabel == null) {
                    manualLabel = li.getLabel();
                }
            }
        }
        if (after.getItemCount() != manualCount) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-consistency-count] consistency violation: legendCollectionCount=" + after.getItemCount() + " manualCountViaGetLegendItem=" + manualCount + " rowKey=" + rowKey + " colKey=" + colKey);
        }
        if (after.getItemCount() == 1) {
            String collectionLabel = after.get(0).getLabel();
            if (manualLabel == null || !collectionLabel.equals(manualLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-consistency-label] consistency violation: collectionLabel=" + String.valueOf(collectionLabel) + " manualLabel=" + String.valueOf(manualLabel) + " rowKey=" + rowKey + " colKey=" + colKey);
            }
        }

        LegendItemCollection again;
        try {
            again = r.getLegendItems();
        } catch (Exception e) {
            return;
        }
        if (again == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-not-null] metamorphic violation: repeated getLegendItems() returned null");
        }
        if (again.getItemCount() != after.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-count] metamorphic violation: repeated getLegendItems() changed itemCount from " + after.getItemCount() + " to " + again.getItemCount());
        }
        if (again.getItemCount() > 0) {
            String label1 = after.get(0).getLabel();
            String label2 = again.get(0).getLabel();
            if (label1 == null ? label2 != null : !label1.equals(label2)) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-idempotent-label] metamorphic violation: repeated getLegendItems() changed first label from " + String.valueOf(label1) + " to " + String.valueOf(label2));
            }
        }

        if (r.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-state] semantic mismatch: renderer did not retain the plot attached by plot.setRenderer()");
        }
    }

    private static String nonEmptyAscii(FuzzedDataProvider data, int maxLen, String fallbackPrefix) {
        String s = data.consumeAsciiString(Math.max(1, maxLen));
        if (s == null || s.length() == 0) {
            return fallbackPrefix;
        }
        return s;
    }
}