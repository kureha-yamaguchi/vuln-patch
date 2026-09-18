package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r0;
        LegendItemCollection items0;
        try {
            r0 = new LineAndShapeRenderer();
            items0 = r0.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items0 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-notnull] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items0.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected 0 but actual=" + items0.getItemCount());
        }

        LineAndShapeRenderer r1;
        CategoryPlot plot1;
        DefaultCategoryDataset dataset1;
        LegendItemCollection items1;
        try {
            r1 = new LineAndShapeRenderer();
            dataset1 = new DefaultCategoryDataset();
            plot1 = new CategoryPlot();
            plot1.setDataset(dataset1);
            plot1.setRenderer(r1);
            items1 = r1.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (r1.getPlot() != plot1) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: plot writer/reader disagree expectedSame=true actualSame=false");
        }
        if (items1 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-attached-notnull] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items1.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-attached-empty] semantic mismatch: expected 0 but actual=" + items1.getItemCount());
        }

        LineAndShapeRenderer r2;
        CategoryPlot plot2;
        DefaultCategoryDataset dataset2;
        LegendItemCollection items2;
        try {
            r2 = new LineAndShapeRenderer();
            dataset2 = new DefaultCategoryDataset();
            plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            dataset2.addValue(1.0, "S1", "C1");
            items2 = r2.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-notnull] semantic mismatch: expected non-null legend collection but got null");
        }
        if (items2.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-itemcount] semantic mismatch: expected 1 but actual=" + items2.getItemCount());
        }
        String label2;
        try {
            label2 = items2.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!"S1".equals(label2)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-label] semantic mismatch: expected=S1 actual=" + String.valueOf(label2));
        }

        LineAndShapeRenderer r3;
        CategoryPlot plot3;
        DefaultCategoryDataset dataset3;
        LegendItemCollection beforeRead;
        LegendItemCollection afterRead;
        String fuzzRow;
        String fuzzCol;
        try {
            r3 = new LineAndShapeRenderer();
            plot3 = new CategoryPlot();
            dataset3 = new DefaultCategoryDataset();
            fuzzRow = data.consumeAsciiString(8);
            if (fuzzRow.length() == 0) {
                fuzzRow = "S";
            }
            fuzzCol = data.consumeAsciiString(8);
            if (fuzzCol.length() == 0) {
                fuzzCol = "C";
            }
            plot3.setDataset(dataset3);
            plot3.setRenderer(r3);
            dataset3.addValue(1.0, fuzzRow, fuzzCol);
            beforeRead = r3.getLegendItems();
            afterRead = r3.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (beforeRead == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (beforeRead.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + beforeRead.getItemCount());
        }
        String fuzzLabel;
        try {
            fuzzLabel = beforeRead.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!fuzzRow.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + fuzzRow + " but got " + fuzzLabel);
        }
        if (afterRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-second-call-notnull] semantic mismatch: expected non-null legend collection on repeated read but got null");
        }
        if (afterRead.getItemCount() != beforeRead.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-repeated-call] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset must agree on itemCount lhs=" + beforeRead.getItemCount() + " rhs=" + afterRead.getItemCount());
        }
        String fuzzLabel2;
        try {
            fuzzLabel2 = afterRead.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!fuzzLabel.equals(fuzzLabel2)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-repeated-label] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset must agree on first label lhs=" + fuzzLabel + " rhs=" + fuzzLabel2);
        }
        if (r3.getPlot() != plot3) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-after-read] semantic mismatch: getLegendItems() should not disturb assigned plot expectedSame=true actualSame=false");
        }

        LineAndShapeRenderer r4;
        LegendItemCollection items4;
        try {
            r4 = new LineAndShapeRenderer();
            items4 = r4.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items4 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items4.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items4.getItemCount());
        }

        LineAndShapeRenderer r5;
        CategoryPlot plot5;
        DefaultCategoryDataset dataset5;
        LegendItemCollection items5a;
        LegendItemCollection items5b;
        try {
            r5 = new LineAndShapeRenderer();
            plot5 = new CategoryPlot();
            dataset5 = new DefaultCategoryDataset();
            plot5.setDataset(dataset5);
            plot5.setRenderer(r5);
            dataset5.addValue(1.0, "S1", "C1");
            items5a = r5.getLegendItems();
            dataset5.addValue(2.0, "S1", "C2");
            items5b = r5.getLegendItems();
        } catch (Throwable e) {
            return;
        }
        if (items5a == null || items5b == null) {
            throw new FuzzerSecurityIssueLow("[oracle:same-series-extra-column-notnull] semantic mismatch: expected non-null legend collections after valid dataset updates");
        }
        /*
         * Contract justification: getLegendItems() reports legend items for series.
         * Adding another value in the same existing series ("S1") changes columns, not series count,
         * so a correct implementation must still report exactly one legend item with the same label.
         * A patch that merely skips the real bookkeeping/path can silently return 0 or stale data here.
         */
        if (items5a.getItemCount() != 1 || items5b.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:same-series-extra-column-count] metamorphic violation: adding a second column in the same series must preserve one legend item before=" + items5a.getItemCount() + " after=" + items5b.getItemCount());
        }
        String l5a;
        String l5b;
        try {
            l5a = items5a.get(0).getLabel();
            l5b = items5b.get(0).getLabel();
        } catch (Throwable e) {
            return;
        }
        if (!"S1".equals(l5a) || !"S1".equals(l5b)) {
            throw new FuzzerSecurityIssueLow("[oracle:same-series-extra-column-label] metamorphic violation: adding a second column in the same series must preserve legend label before=" + l5a + " after=" + l5b);
        }
    }
}