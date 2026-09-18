package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    private static void oracle(String id, String message) {
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + message);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        // Lifted exactly from AbstractCategoryItemRendererTests.test2947660:
        // assertNotNull(r.getLegendItems());
        LegendItemCollection items0 = r.getLegendItems();
        if (items0 == null) {
            oracle("lifted-not-null", "expected non-null legend collection before plot assignment but got null");
        }

        // Lifted exactly from the test:
        // assertEquals(0, r.getLegendItems().getItemCount());
        if (items0.getItemCount() != 0) {
            oracle("lifted-preplot-zero", "expected 0 legend items before plot assignment but got " + items0.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        // State-coupling check on the shared plot field: setRenderer(r) must assign this plot to the renderer.
        if (r.getPlot() != plot) {
            oracle("plot-coupling", "expected renderer.getPlot() to be the assigned plot");
        }

        // Lifted exactly from the test:
        // assertEquals(0, r.getLegendItems().getItemCount());
        LegendItemCollection items1 = r.getLegendItems();
        if (items1 == null) {
            oracle("lifted-empty-not-null", "expected non-null legend collection for empty attached dataset but got null");
        }
        if (items1.getItemCount() != 0) {
            oracle("lifted-empty-zero", "expected 0 legend items for empty attached dataset but got " + items1.getItemCount());
        }

        // Exact seed setup from the failing test. This reaches the patched line with dataset != null.
        dataset.addValue(1.0, "S1", "C1");

        // Optional data use that still preserves the bug-triggering shape: overwrite the same cell only.
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            dataset.addValue(data.consumeInt(-1000, 1000), "S1", "C1");
        }

        LegendItemCollection lic = r.getLegendItems();

        // Lifted exactly from the test:
        // assertEquals(1, lic.getItemCount());
        if (lic == null) {
            oracle("lifted-single-not-null", "expected non-null legend collection after adding one series but got null");
        }
        if (lic.getItemCount() != 1) {
            oracle("lifted-single-count", "expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got " + lic.getItemCount());
        }

        // Lifted exactly from the test:
        // assertEquals("S1", lic.get(0).getLabel());
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            oracle("lifted-single-label", "expected first legend label 'S1' but got '" + label + "'");
        }

        // Mandatory post-condition / independent cross-check:
        // For a single-series attached dataset, getLegendItems() should expose the same item
        // as direct getLegendItem(datasetIndex, series). A band-aid that only changes the collection
        // count path or hides the bug would violate this agreement.
        int datasetIndex = plot.getIndexOf(r);
        if (datasetIndex != 0) {
            oracle("dataset-index", "expected renderer index 0 but got " + datasetIndex);
        }
        if (lic.get(0).getDatasetIndex() != datasetIndex) {
            oracle("collection-dataset-index", "expected collection item dataset index " + datasetIndex + " but got " + lic.get(0).getDatasetIndex());
        }

        org.jfree.chart.LegendItem direct = r.getLegendItem(datasetIndex, 0);
        if (direct == null) {
            oracle("direct-item-not-null", "expected direct legend item for datasetIndex=0, series=0 but got null");
        }
        if (!"S1".equals(direct.getLabel())) {
            oracle("direct-item-label", "expected direct legend label 'S1' but got '" + direct.getLabel() + "'");
        }
        if (direct.getDatasetIndex() != datasetIndex) {
            oracle("direct-item-dataset-index", "expected direct legend item dataset index " + datasetIndex + " but got " + direct.getDatasetIndex());
        }
        if (!lic.get(0).getLabel().equals(direct.getLabel())) {
            oracle("collection-direct-agreement", "collection/direct legend label disagreement: collection='" + lic.get(0).getLabel() + "' direct='" + direct.getLabel() + "'");
        }
    }
}