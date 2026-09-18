package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer r0;
        LegendItemCollection items0;
        try {
            r0 = new LineAndShapeRenderer();
            items0 = r0.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-initial] semantic mismatch: getLegendItems() returned null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-initial] semantic mismatch: expected=0 actual=" + items0.getItemCount());
        }

        AbstractCategoryItemRenderer r1;
        DefaultCategoryDataset dataset1;
        CategoryPlot plot1;
        LegendItemCollection items1;
        try {
            r1 = new LineAndShapeRenderer();
            dataset1 = new DefaultCategoryDataset();
            plot1 = new CategoryPlot();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);
            items1 = r1.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-writer-reader-agree] semantic mismatch: set renderer on plot but renderer.getPlot() did not return that plot");
        }
        if (items1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-empty-dataset] semantic mismatch: getLegendItems() returned null");
        }
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-empty-dataset] semantic mismatch: expected=0 actual=" + items1.getItemCount());
        }

        AbstractCategoryItemRenderer r2;
        DefaultCategoryDataset dataset2;
        CategoryPlot plot2;
        LegendItemCollection items2;
        try {
            r2 = new LineAndShapeRenderer();
            dataset2 = new DefaultCategoryDataset();
            plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(1.0, "S1", "C1");
            items2 = r2.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-notnull-singleton] semantic mismatch: getLegendItems() returned null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-singleton-count] semantic mismatch: expected=1 actual=" + items2.getItemCount());
        }
        String label2;
        try {
            label2 = items2.get(0).getLabel();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (!"S1".equals(label2)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-singleton-label] semantic mismatch: expected=S1 actual=" + label2);
        }

        LineAndShapeRenderer r3;
        LegendItemCollection items3 = null;
        Throwable ex3 = null;
        try {
            r3 = new LineAndShapeRenderer();
            items3 = r3.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            ex3 = t;
        }
        if (ex3 != null) {
            return;
        }
        if (items3 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items3.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expectedCount=0 actualCount=" + items3.getItemCount());
        }

        LineAndShapeRenderer r4;
        CategoryPlot plot4;
        DefaultCategoryDataset dataset4;
        LegendItemCollection items4 = null;
        Throwable ex4 = null;
        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        double value = data.consumeInt(-1000000, 1000000);
        try {
            r4 = new LineAndShapeRenderer();
            plot4 = new CategoryPlot();
            dataset4 = new DefaultCategoryDataset();
            plot4.setDataset(dataset4);
            plot4.setRenderer(r4);
            dataset4.addValue(value, rowKey, columnKey);
            items4 = r4.getLegendItems();

            // Contract/post-condition: getLegendItems() is a query for the series this renderer is responsible for.
            // For a renderer attached to a plot with exactly one dataset row, deleting the effectful path or guarding it away
            // would silently return the wrong collection; the observable post-state is one legend item labeled with that row key.
            if (r4.getPlot() != plot4) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-writer-reader-agree] semantic mismatch: expected renderer plot identity to match assigned plot");
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            ex4 = t;
        }
        if (ex4 != null) {
            return;
        }
        if (items4 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (items4.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expectedCount=1 actualCount=" + items4.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey);
        }
        String label4;
        try {
            label4 = items4.get(0).getLabel();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (!rowKey.equals(label4)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expectedLabel=" + rowKey + " actualLabel=" + label4 + " columnKey=" + columnKey);
        }

        LineAndShapeRenderer r5;
        CategoryPlot plot5;
        DefaultCategoryDataset dataset5;
        LegendItemCollection firstRead = null;
        LegendItemCollection secondRead = null;
        Throwable ex5 = null;
        String rowKey2 = data.consumeAsciiString(8);
        if (rowKey2.length() == 0) {
            rowKey2 = "S2";
        }
        String columnKey2 = data.consumeAsciiString(8);
        if (columnKey2.length() == 0) {
            columnKey2 = "C2";
        }
        try {
            r5 = new LineAndShapeRenderer();
            plot5 = new CategoryPlot();
            dataset5 = new DefaultCategoryDataset();
            plot5.setDataset(dataset5);
            plot5.setRenderer(r5);
            dataset5.addValue(1.0, rowKey2, columnKey2);
            firstRead = r5.getLegendItems();
            secondRead = r5.getLegendItems();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            ex5 = t;
        }
        if (ex5 != null) {
            return;
        }
        if (firstRead == null || secondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-idempotence] semantic mismatch: repeated getLegendItems() returned null");
        }
        if (firstRead.getItemCount() != secondRead.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-idempotence] metamorphic violation: repeated getLegendItems() changed count first=" + firstRead.getItemCount() + " second=" + secondRead.getItemCount());
        }
        if (firstRead.getItemCount() == 1 && secondRead.getItemCount() == 1) {
            String l1;
            String l2;
            try {
                l1 = firstRead.get(0).getLabel();
                l2 = secondRead.get(0).getLabel();
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
            if (!l1.equals(l2)) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-idempotence] metamorphic violation: repeated getLegendItems() changed label first=" + l1 + " second=" + l2);
            }
        }
    }
}