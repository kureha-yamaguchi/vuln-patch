package org.jfree.chart.renderer.category;

import java.awt.Color;
import java.awt.Paint;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runExactTest2947660ViaUniqueHelper();
        checkCollectionMatchesDirectPaintMetadata(data);
    }

    private static void runExactTest2947660ViaUniqueHelper() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection items0 = r.getLegendItems();
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-verbatim-unique-notnull] semantic mismatch: expected non-null legend collection before plot assignment but was null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-verbatim-unique-preplot-count] semantic mismatch: expected 0 but got " + items0.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection items1 = r.getLegendItems();
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-verbatim-unique-empty-count] semantic mismatch: expected 0 but got " + items1.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-verbatim-unique-single-count] semantic mismatch: expected 1 but got " + lic.getItemCount());
        }
        if (!"S1".equals(lic.get(0).getLabel())) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-verbatim-unique-single-label] semantic mismatch: expected S1 but got " + lic.get(0).getLabel());
        }
    }

    private static void checkCollectionMatchesDirectPaintMetadata(FuzzedDataProvider data) {
        AreaRenderer r;
        DefaultCategoryDataset dataset;
        CategoryPlot plot;
        String rowKey;
        String colKey;
        int value;
        Color fill;
        Color outline;
        LegendItem direct;
        LegendItemCollection collection;

        try {
            r = new AreaRenderer();
            dataset = new DefaultCategoryDataset();
            plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            rowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
            if (rowKey.length() == 0) {
                rowKey = "R";
            }
            colKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
            if (colKey.length() == 0) {
                colKey = "C";
            }
            value = data.consumeInt(-1000, 1000);

            fill = new Color(data.consumeInt(0, 255), data.consumeInt(0, 255), data.consumeInt(0, 255));
            outline = new Color(data.consumeInt(0, 255), data.consumeInt(0, 255), data.consumeInt(0, 255));

            r.setSeriesPaint(0, fill);
            r.setSeriesOutlinePaint(0, outline);
            dataset.addValue(value, rowKey, colKey);

            direct = r.getLegendItem(plot.getIndexOf(r), 0);
            collection = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (direct == null || collection == null) {
            return;
        }

        if (collection.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:direct-collection-paint-count] consistency violation: single visible series should yield exactly one collection item and one direct item; directPresent="
                    + (direct != null) + " collectionCount=" + collection.getItemCount());
        }

        LegendItem fromCollection = collection.get(0);
        Paint directFill = direct.getFillPaint();
        Paint collectionFill = fromCollection.getFillPaint();
        Paint directOutline = direct.getOutlinePaint();
        Paint collectionOutline = fromCollection.getOutlinePaint();

        // Contract guarantee: getLegendItems() builds its result by obtaining each series' LegendItem through getLegendItem(index, i)
        // and adding that item to the collection. Therefore, for a one-series attached plot, the collection's sole item's
        // paint metadata must agree with the direct getLegendItem() result. A band-aid fix that only forces the count/label
        // could still leave the helper-produced legend item metadata inconsistent, so this cross-check remains meaningful.
        if (!samePaint(directFill, collectionFill)) {
            throw new FuzzerSecurityIssueLow("[oracle:direct-collection-fill-paint] consistency violation: direct fill paint and collection fill paint disagree direct="
                    + paintToString(directFill) + " collection=" + paintToString(collectionFill));
        }

        if (!samePaint(directOutline, collectionOutline)) {
            throw new FuzzerSecurityIssueLow("[oracle:direct-collection-outline-paint] consistency violation: direct outline paint and collection outline paint disagree direct="
                    + paintToString(directOutline) + " collection=" + paintToString(collectionOutline));
        }
    }

    private static boolean samePaint(Paint a, Paint b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String paintToString(Paint p) {
        return String.valueOf(p);
    }
}