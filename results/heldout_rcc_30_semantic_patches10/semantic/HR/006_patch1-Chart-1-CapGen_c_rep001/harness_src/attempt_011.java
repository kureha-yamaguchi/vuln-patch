package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedTest2947660();
        runRowRenderingOrderFlipOracle(data);
    }

    private static void runLiftedTest2947660() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection items0 = r.getLegendItems();
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-seed] semantic mismatch: expected non-null legend collection before plot attachment but actual=null");
        }

        LegendItemCollection items1 = r.getLegendItems();
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-zero-seed] semantic mismatch: expected itemCount=0 before plot attachment but actual=" + items1.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection items2 = r.getLegendItems();
        if (items2.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-empty-dataset-seed] semantic mismatch: expected itemCount=0 for attached empty dataset but actual=" + items2.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-seed] semantic mismatch: expected itemCount=1 after dataset.addValue(1.0, \"S1\", \"C1\") but actual=" + lic.getItemCount());
        }
        if (!"S1".equals(lic.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-seed] semantic mismatch: expected label=S1 but actual=" + lic.get(0).getLabel());
        }
    }

    private static void runRowRenderingOrderFlipOracle(FuzzedDataProvider data) {
        String rowA = nonEmptyAscii(data, "A");
        String rowB = distinctNonEmptyAscii(data, rowA, "B");
        String col = nonEmptyAscii(data, "C");
        int v1 = data.consumeInt(-1000, 1000);
        int v2 = data.consumeInt(-1000, 1000);

        AbstractCategoryItemRenderer ascRenderer;
        AbstractCategoryItemRenderer descRenderer;
        DefaultCategoryDataset ascDataset;
        DefaultCategoryDataset descDataset;
        CategoryPlot ascPlot;
        CategoryPlot descPlot;
        LegendItemCollection ascItems;
        LegendItemCollection descItems;
        LegendItem ascDirect0;
        LegendItem ascDirect1;
        LegendItem descDirect0;
        LegendItem descDirect1;

        try {
            ascRenderer = new LineAndShapeRenderer();
            descRenderer = new LineAndShapeRenderer();

            ascDataset = new DefaultCategoryDataset();
            descDataset = new DefaultCategoryDataset();

            ascDataset.addValue(v1, rowA, col);
            ascDataset.addValue(v2, rowB, col);
            descDataset.addValue(v1, rowA, col);
            descDataset.addValue(v2, rowB, col);

            ascPlot = new CategoryPlot();
            ascPlot.setDataset(ascDataset);
            ascPlot.setRenderer(ascRenderer);
            ascPlot.setRowRenderingOrder(SortOrder.ASCENDING);

            descPlot = new CategoryPlot();
            descPlot.setDataset(descDataset);
            descPlot.setRenderer(descRenderer);
            descPlot.setRowRenderingOrder(SortOrder.DESCENDING);

            ascItems = ascRenderer.getLegendItems();
            descItems = descRenderer.getLegendItems();

            int ascIndex = ascPlot.getIndexOf(ascRenderer);
            int descIndex = descPlot.getIndexOf(descRenderer);
            ascDirect0 = ascRenderer.getLegendItem(ascIndex, 0);
            ascDirect1 = ascRenderer.getLegendItem(ascIndex, 1);
            descDirect0 = descRenderer.getLegendItem(descIndex, 0);
            descDirect1 = descRenderer.getLegendItem(descIndex, 1);
        } catch (Throwable t) {
            return;
        }

        if (ascItems == null || descItems == null || ascDirect0 == null || ascDirect1 == null || descDirect0 == null || descDirect1 == null) {
            return;
        }

        if (ascItems.getItemCount() != 2) {
            throw new FuzzerSecurityIssueLow("[oracle:row-order-asc-count] metamorphic violation: expected 2 legend items for two default-visible series in ascending order but actual=" + ascItems.getItemCount());
        }
        if (descItems.getItemCount() != 2) {
            throw new FuzzerSecurityIssueLow("[oracle:row-order-desc-count] metamorphic violation: expected 2 legend items for two default-visible series in descending order but actual=" + descItems.getItemCount());
        }

        String asc0 = ascItems.get(0).getLabel();
        String asc1 = ascItems.get(1).getLabel();
        String desc0 = descItems.get(0).getLabel();
        String desc1 = descItems.get(1).getLabel();

        if (!rowA.equals(asc0) || !rowB.equals(asc1)) {
            throw new FuzzerSecurityIssueLow("[oracle:row-order-ascending-sequence] metamorphic violation: expected ascending legend labels=[" + rowA + "," + rowB + "] actual=[" + asc0 + "," + asc1 + "]");
        }

        if (!rowB.equals(desc0) || !rowA.equals(desc1)) {
            throw new FuzzerSecurityIssueLow("[oracle:row-order-descending-sequence] metamorphic violation: expected descending legend labels=[" + rowB + "," + rowA + "] actual=[" + desc0 + "," + desc1 + "]");
        }

        // Contract used: getLegendItems() iterates series in the plot's row rendering order and adds getLegendItem(index, i) for each visible series.
        // Therefore the collection order must agree with direct per-series legend items; a patch that merely suppresses the buggy branch or returns
        // an empty/partial collection would break this observable post-condition even if it hid the original seed symptom.
        String ascDirectLabel0 = ascDirect0.getLabel();
        String ascDirectLabel1 = ascDirect1.getLabel();
        String descDirectLabel0 = descDirect0.getLabel();
        String descDirectLabel1 = descDirect1.getLabel();

        if (!ascDirectLabel0.equals(ascItems.get(0).getLabel()) || !ascDirectLabel1.equals(ascItems.get(1).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:direct-vs-collection-ascending] consistency violation: directLabels=[" + ascDirectLabel0 + "," + ascDirectLabel1 + "] collectionLabels=[" + ascItems.get(0).getLabel() + "," + ascItems.get(1).getLabel() + "]");
        }

        if (!descDirectLabel1.equals(descItems.get(0).getLabel()) || !descDirectLabel0.equals(descItems.get(1).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:direct-vs-collection-descending] consistency violation: directLabels(series0,series1)=[" + descDirectLabel0 + "," + descDirectLabel1 + "] collectionLabels=[" + descItems.get(0).getLabel() + "," + descItems.get(1).getLabel() + "]");
        }

        if (ascItems.get(0).getSeriesIndex() != 0 || ascItems.get(1).getSeriesIndex() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:ascending-series-index-order] consistency violation: expected ascending series indices [0,1] actual=[" + ascItems.get(0).getSeriesIndex() + "," + ascItems.get(1).getSeriesIndex() + "]");
        }

        if (descItems.get(0).getSeriesIndex() != 1 || descItems.get(1).getSeriesIndex() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:descending-series-index-order] consistency violation: expected descending series indices [1,0] actual=[" + descItems.get(0).getSeriesIndex() + "," + descItems.get(1).getSeriesIndex() + "]");
        }
    }

    private static String nonEmptyAscii(FuzzedDataProvider data, String fallback) {
        String s = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }

    private static String distinctNonEmptyAscii(FuzzedDataProvider data, String other, String fallback) {
        String s = nonEmptyAscii(data, fallback);
        if (s.equals(other)) {
            return s + "_x";
        }
        return s;
    }
}