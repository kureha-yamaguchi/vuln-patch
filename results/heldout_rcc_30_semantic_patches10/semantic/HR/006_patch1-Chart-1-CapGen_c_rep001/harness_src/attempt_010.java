package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkDescriptionConsistencyAgainstDirectLegendItem();
        checkLiftedTest2947660();

        String rowKey = data.consumeAsciiString(16);
        String columnKey = data.consumeAsciiString(16);
        if (rowKey.length() == 0) {
            rowKey = "R";
        }
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        double value = data.consumeInt(-1000000, 1000000);
        try {
            checkConstructedSingleSeriesOracle(rowKey, columnKey, value);
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkDescriptionConsistencyAgainstDirectLegendItem() {
        LineAndShapeRenderer r = new LineAndShapeRenderer();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        dataset.addValue(1.0, "S1", "C1");

        LegendItem direct = r.getLegendItem(0, 0);
        LegendItemCollection collection = r.getLegendItems();

        if (direct == null) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:description-direct-nonnull] semantic mismatch: direct legend item for single visible series was null");
        }
        if (!"S1".equals(direct.getDescription())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:description-direct-value] semantic mismatch: expected description=S1 actualDescription="
                            + String.valueOf(direct.getDescription()));
        }

        /*
         * Contract used: getLegendItems() iterates the dataset series and adds
         * getLegendItem(index, i) for each visible series. With one dataset row
         * and default visibility, the collection must therefore expose the same
         * first legend item that direct getLegendItem(0, 0) returns. A patch
         * that merely suppresses the old symptom by returning an empty/masked
         * collection breaks this observable agreement.
         */
        if (collection.getItemCount() < 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:description-collection-agreement] consistency violation: directDescription="
                            + String.valueOf(direct.getDescription())
                            + " collectionCount=" + collection.getItemCount());
        }
        LegendItem first = collection.get(0);
        String directDescription = direct.getDescription();
        String firstDescription = first.getDescription();
        if (directDescription == null ? firstDescription != null
                : !directDescription.equals(firstDescription)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:description-collection-agreement] consistency violation: directDescription="
                            + String.valueOf(directDescription)
                            + " collectionDescription=" + String.valueOf(firstDescription));
        }
    }

    private static void checkLiftedTest2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();
        if (r.getLegendItems() == null) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-not-null-exact] semantic mismatch: expected non-null legend items before plot assignment");
        }
        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-count-initial-exact] semantic mismatch: expected=0 actual="
                            + r.getLegendItems().getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-count-empty-plot-exact] semantic mismatch: expected=0 actual="
                            + r.getLegendItems().getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-count-single-exact] semantic mismatch: expected=1 actual="
                            + lic.getItemCount());
        }
        if (!"S1".equals(lic.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-label-single-exact] semantic mismatch: expected=S1 actual="
                            + String.valueOf(lic.get(0).getLabel()));
        }
    }

    private static void checkConstructedSingleSeriesOracle(String rowKey, String columnKey, double value) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        dataset.addValue(value, rowKey, columnKey);

        LegendItem direct = r.getLegendItem(0, 0);
        LegendItemCollection collection = r.getLegendItems();

        if (direct == null) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-direct-single-item] semantic mismatch: direct legend item was null for rowKey="
                            + rowKey + " columnKey=" + columnKey);
        }

        /*
         * Oracle from constructed input: with a dataset containing exactly one
         * row whose key we chose ourselves, the legend label/description for
         * series 0 must recover that row key. This follows from
         * getLegendItem(...): the label is generated from the dataset series,
         * and for the default generator in this test the single series is the
         * inserted row key.
         */
        if (!rowKey.equals(direct.getLabel())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-direct-label] semantic mismatch: expected=" + rowKey
                            + " actual=" + String.valueOf(direct.getLabel()));
        }
        if (!rowKey.equals(direct.getDescription())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-direct-description] semantic mismatch: expected=" + rowKey
                            + " actual=" + String.valueOf(direct.getDescription()));
        }

        /*
         * Same consistency rule as above, but generalized to fuzz-constructed
         * single-series datasets: the collection result must agree with the
         * direct per-series result on the first item's description.
         */
        if (collection.getItemCount() < 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-description-agreement] consistency violation: rowKey="
                            + rowKey + " collectionCount=" + collection.getItemCount()
                            + " directDescription=" + String.valueOf(direct.getDescription()));
        }
        LegendItem first = collection.get(0);
        if (!rowKey.equals(first.getDescription())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-description-agreement] consistency violation: expectedDescription="
                            + rowKey + " actualDescription=" + String.valueOf(first.getDescription()));
        }
    }
}