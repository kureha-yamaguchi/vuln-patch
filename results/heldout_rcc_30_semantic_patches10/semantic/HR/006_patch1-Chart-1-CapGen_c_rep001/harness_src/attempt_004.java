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
        runLiftedOracles();
        runSeriesMetadataConsistencyOracle(data);
    }

    private static void runLiftedOracles() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection beforePlot = r.getLegendItems();
        if (beforePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-test2947660] semantic mismatch: expected non-null legend collection before plot assignment");
        }
        if (beforePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-preplot-count-test2947660] semantic mismatch: expected=0 actual=" + beforePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        LegendItemCollection emptyOnPlot = r.getLegendItems();
        if (emptyOnPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-emptyplot-count-test2947660] semantic mismatch: expected=0 actual=" + emptyOnPlot.getItemCount());
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-count-test2947660] semantic mismatch: expected=1 actual=" + lic.getItemCount());
        }
        String actualLabel = lic.get(0).getLabel();
        if (!"S1".equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-single-label-test2947660] semantic mismatch: expected=S1 actual=" + actualLabel);
        }
    }

    private static void runSeriesMetadataConsistencyOracle(FuzzedDataProvider data) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer();
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            SortOrder order = data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING;
            plot.setRowRenderingOrder(order);

            int seriesCount = data.consumeInt(2, 6);
            String columnKey = nonEmpty(data.consumeAsciiString(8), "C");

            for (int i = 0; i < seriesCount; i++) {
                String rowKey = uniqueNonEmpty(data.consumeAsciiString(8), "S", i);
                double value = data.consumeInt(-1000, 1000);
                dataset.addValue(value, rowKey, columnKey);
            }

            LegendItemCollection items = r.getLegendItems();

            int expectedCount = dataset.getRowCount();
            if (items.getItemCount() != expectedCount) {
                throw new FuzzerSecurityIssueLow("[oracle:series-metadata-count-consistency] consistency violation: reportedCount=" + items.getItemCount() + " expectedCount=" + expectedCount + " order=" + order);
            }

            // Contract used for this check:
            // - getLegendItems() iterates series in plot.getRowRenderingOrder() and adds getLegendItem(index, i).
            // - getLegendItem(...) sets item.setSeriesKey(dataset.getRowKey(series)) and item.setSeriesIndex(series).
            // Therefore, for every legend item returned, the item's series metadata must agree with the
            // dataset row reached at that position. A band-aid patch that merely masks the seed symptom can
            // still leave this helper-derived metadata/order wrong, so this cross-check remains meaningful.
            for (int itemPos = 0; itemPos < items.getItemCount(); itemPos++) {
                int expectedSeries = (order.equals(SortOrder.ASCENDING))
                        ? itemPos
                        : (dataset.getRowCount() - 1 - itemPos);

                LegendItem item = items.get(itemPos);
                Comparable expectedKey = dataset.getRowKey(expectedSeries);
                Comparable actualKey = item.getSeriesKey();
                int actualSeries = item.getSeriesIndex();

                if (!expectedKey.equals(actualKey)) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:series-key-order-consistency] consistency violation: itemPos="
                                    + itemPos + " expectedSeries=" + expectedSeries
                                    + " expectedKey=" + expectedKey + " actualKey=" + actualKey
                                    + " order=" + order);
                }
                if (actualSeries != expectedSeries) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:series-index-order-consistency] consistency violation: itemPos="
                                    + itemPos + " expectedSeries=" + expectedSeries
                                    + " actualSeries=" + actualSeries + " order=" + order);
                }
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }

    private static String nonEmpty(String s, String prefix) {
        if (s == null || s.length() == 0) {
            return prefix;
        }
        return s;
    }

    private static String uniqueNonEmpty(String s, String prefix, int i) {
        return nonEmpty(s, prefix) + "_" + i;
    }
}