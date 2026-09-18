package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracles();
        runBoundaryFlipAndSiblingAgreement(data);
    }

    private static void runLiftedOracles() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection items0;
        try {
            items0 = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-initial] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-initial] semantic mismatch: expected itemCount=0 but got " + items0.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection items1;
        try {
            items1 = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-empty-dataset] semantic mismatch: expected itemCount=0 but got " + items1.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic;
        try {
            lic = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count] semantic mismatch: expected itemCount=1 but got " + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label] semantic mismatch: expected label=S1 but got " + String.valueOf(actualLabel));
        }
    }

    private static void runBoundaryFlipAndSiblingAgreement(FuzzedDataProvider data) {
        String rowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        String colKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        double value = data.consumeInt(-1000, 1000);

        AbstractCategoryItemRenderer renderer = new LineAndShapeRenderer();
        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);

        LegendItemCollection nullDatasetItems;
        try {
            nullDatasetItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (nullDatasetItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:boundary-null-dataset-not-null] semantic mismatch: expected non-null legend collection for null dataset but got null");
        }
        if (renderer.getPlot() != plot) {
            throw new FuzzerSecurityIssueLow("[oracle:state-plot-coupling-direct] semantic mismatch: set plot instance not returned by getPlot()");
        }

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        plot.setDataset(ds);

        LegendItemCollection emptyDatasetItems;
        try {
            emptyDatasetItems = renderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        ds.addValue(value, rowKey, colKey);

        LegendItemCollection afterItems;
        LegendItem directItem;
        int datasetIndex;
        try {
            afterItems = renderer.getLegendItems();
            datasetIndex = plot.getIndexOf(renderer);
            directItem = renderer.getLegendItem(datasetIndex, 0);
        } catch (Throwable t) {
            return;
        }

        /* Contract justification:
         * getLegendItems() builds its collection by iterating visible series and calling getLegendItem(index, i),
         * adding each non-null result. Therefore, on a plot with exactly one series, the collection's sole element
         * must agree with a direct sibling call getLegendItem(plot.getIndexOf(renderer), 0). A band-aid that merely
         * suppresses the known count/label symptom can still leave this helper/collection agreement wrong.
         */
        if (afterItems == null || directItem == null) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-item-present] consistency violation: collection=" + afterItems + " directItem=" + directItem);
        }
        if (afterItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-item-count] consistency violation: expected one collection item for one-series dataset but got " + afterItems.getItemCount());
        }

        LegendItem collected = afterItems.get(0);
        if (collected == null) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-item-nonnull] consistency violation: collection contains null legend item");
        }

        String collectedLabel = collected.getLabel();
        String directLabel = directItem.getLabel();
        if (collectedLabel == null ? directLabel != null : !collectedLabel.equals(directLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-label] consistency violation: collectedLabel=" + collectedLabel + " directLabel=" + directLabel);
        }

        Comparable collectedSeriesKey = collected.getSeriesKey();
        Comparable directSeriesKey = directItem.getSeriesKey();
        if (collectedSeriesKey == null ? directSeriesKey != null : !collectedSeriesKey.equals(directSeriesKey)) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-series-key] consistency violation: collectedSeriesKey=" + collectedSeriesKey + " directSeriesKey=" + directSeriesKey);
        }

        if (collected.getSeriesIndex() != directItem.getSeriesIndex()) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-series-index] consistency violation: collectedSeriesIndex=" + collected.getSeriesIndex() + " directSeriesIndex=" + directItem.getSeriesIndex());
        }

        if (collected.getDatasetIndex() != directItem.getDatasetIndex()) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-dataset-index] consistency violation: collectedDatasetIndex=" + collected.getDatasetIndex() + " directDatasetIndex=" + directItem.getDatasetIndex());
        }

        if (collected.getDataset() != directItem.getDataset()) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-direct-dataset-ref] consistency violation: collectedDataset=" + collected.getDataset() + " directDataset=" + directItem.getDataset());
        }

        /* Strategy (c), flip the patched condition:
         * null dataset and empty non-null dataset sit on opposite sides of the changed dataset==null boundary.
         * For this API both are documented/observed to yield an empty LegendItemCollection. If a patch overfits
         * just one side of the boundary, these neighboring states disagree.
         */
        if (nullDatasetItems.getItemCount() != emptyDatasetItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:boundary-null-vs-empty] metamorphic violation: nullDatasetCount=" + nullDatasetItems.getItemCount() + " emptyDatasetCount=" + emptyDatasetItems.getItemCount());
        }
    }
}