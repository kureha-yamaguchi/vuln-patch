package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String fuzzRowKey = data.consumeAsciiString(8);
        if (fuzzRowKey.length() == 0) {
            fuzzRowKey = "S";
        }
        String fuzzColumnKey = data.consumeAsciiString(8);
        if (fuzzColumnKey.length() == 0) {
            fuzzColumnKey = "C";
        }
        double fuzzValue = data.consumeInt(-1000, 1000);

        // Lifted oracle 1 from test2947660:
        // assertNotNull(r.getLegendItems());
        // assertEquals(0, r.getLegendItems().getItemCount());
        LineAndShapeRenderer r1;
        LegendItemCollection itemsNoPlot;
        try {
            r1 = new LineAndShapeRenderer();
            itemsNoPlot = r1.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsNoPlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (itemsNoPlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-initial-empty] semantic mismatch: expected itemCount=0 actual=" + itemsNoPlot.getItemCount());
        }

        // State-coupling check on shared field 'plot':
        // setPlot()/getPlot() must agree on the same field value.
        LineAndShapeRenderer rPlot;
        CategoryPlot explicitPlot;
        CategoryPlot observedPlot;
        try {
            rPlot = new LineAndShapeRenderer();
            explicitPlot = new CategoryPlot();
            rPlot.setPlot(explicitPlot);
            observedPlot = rPlot.getPlot();
        } catch (Throwable t) {
            return;
        }
        if (observedPlot != explicitPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-state-agreement] semantic mismatch: setPlot/getPlot disagree expectedSame=true actualSame=false");
        }

        // Lifted oracle 2 from test2947660:
        // DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // CategoryPlot plot = new CategoryPlot();
        // plot.setDataset(dataset);
        // plot.setRenderer(r);
        // assertEquals(0, r.getLegendItems().getItemCount());
        LineAndShapeRenderer r2;
        DefaultCategoryDataset dataset2;
        CategoryPlot plot2;
        LegendItemCollection itemsEmptyDataset;
        try {
            r2 = new LineAndShapeRenderer();
            dataset2 = new DefaultCategoryDataset();
            plot2 = new CategoryPlot();
            plot2.setDataset(dataset2);
            plot2.setRenderer(r2);
            itemsEmptyDataset = r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsEmptyDataset == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (itemsEmptyDataset.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-emptydataset-empty] semantic mismatch: expected itemCount=0 actual=" + itemsEmptyDataset.getItemCount());
        }

        // Lifted oracle 3 from test2947660:
        // dataset.addValue(1.0, "S1", "C1");
        // LegendItemCollection lic = r.getLegendItems();
        // assertEquals(1, lic.getItemCount());
        // assertEquals("S1", lic.get(0).getLabel());
        LineAndShapeRenderer r3;
        DefaultCategoryDataset dataset3;
        CategoryPlot plot3;
        LegendItemCollection lic;
        try {
            r3 = new LineAndShapeRenderer();
            dataset3 = new DefaultCategoryDataset();
            plot3 = new CategoryPlot();
            plot3.setDataset(dataset3);
            plot3.setRenderer(r3);
            dataset3.addValue(1.0, "S1", "C1");
            lic = r3.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (lic == null) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-nonnull] semantic mismatch: expected non-null legend items but was null");
        }
        if (lic.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-count] semantic mismatch: expected itemCount=1 actual=" + lic.getItemCount());
        }
        String label0;
        try {
            label0 = lic.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(label0)) {
            throw new FuzzerSecurityIssueLow("[oracle:test2947660-single-label] semantic mismatch: expected label=S1 actual=" + label0);
        }

        // Mandatory generalisation from a known answer:
        // For a real LineAndShapeRenderer attached to a plot with a non-null dataset containing exactly one series,
        // getLegendItems() should report exactly one legend item for that series; this is the same contract the failing
        // test fixes, just with fuzz-chosen non-empty row/column keys.
        LineAndShapeRenderer r4;
        DefaultCategoryDataset dataset4;
        CategoryPlot plot4;
        LegendItemCollection fuzzItems;
        try {
            r4 = new LineAndShapeRenderer();
            dataset4 = new DefaultCategoryDataset();
            plot4 = new CategoryPlot();
            plot4.setDataset(dataset4);
            plot4.setRenderer(r4);
            dataset4.addValue(fuzzValue, fuzzRowKey, fuzzColumnKey);
            fuzzItems = r4.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount());
        }
        String fuzzLabel;
        try {
            fuzzLabel = fuzzItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!fuzzRowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + fuzzRowKey + " but got " + fuzzLabel);
        }

        // Required non-crash post-condition / hidden-state check:
        // getLegendItems() is a query ("Returns the legend item collection") and should not silently mutate the renderer
        // or dataset-visible result. A throw-deleting or guard-adding patch that skips bookkeeping can make repeated reads
        // disagree. So two consecutive calls on unchanged state must yield the same observable count and label.
        LineAndShapeRenderer r5;
        DefaultCategoryDataset dataset5;
        CategoryPlot plot5;
        LegendItemCollection firstRead;
        LegendItemCollection secondRead;
        try {
            r5 = new LineAndShapeRenderer();
            dataset5 = new DefaultCategoryDataset();
            plot5 = new CategoryPlot();
            plot5.setDataset(dataset5);
            plot5.setRenderer(r5);
            dataset5.addValue(1.0, fuzzRowKey, fuzzColumnKey);
            firstRead = r5.getLegendItems();
            secondRead = r5.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (firstRead == null || secondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:legend-idempotent-nonnull] semantic mismatch: repeated getLegendItems returned null first=" + (firstRead == null) + " second=" + (secondRead == null));
        }
        int firstCount;
        int secondCount;
        try {
            firstCount = firstRead.getItemCount();
            secondCount = secondRead.getItemCount();
        } catch (Throwable t) {
            return;
        }
        if (firstCount != secondCount) {
            throw new FuzzerSecurityIssueLow("[oracle:legend-idempotent-count] metamorphic violation: repeated getLegendItems on unchanged renderer state differed firstCount=" + firstCount + " secondCount=" + secondCount);
        }
        if (firstCount == 1 && secondCount == 1) {
            String firstLabel;
            String secondLabel;
            try {
                firstLabel = firstRead.get(0).getLabel();
                secondLabel = secondRead.get(0).getLabel();
            } catch (Throwable t) {
                return;
            }
            if (firstLabel == null ? secondLabel != null : !firstLabel.equals(secondLabel)) {
                throw new FuzzerSecurityIssueLow("[oracle:legend-idempotent-label] metamorphic violation: repeated getLegendItems on unchanged renderer state differed firstLabel=" + firstLabel + " secondLabel=" + secondLabel);
            }
        }

        // Candidate relation from prompt: no plot => non-null empty collection.
        LineAndShapeRenderer r6;
        LegendItemCollection items6;
        try {
            r6 = new LineAndShapeRenderer();
            items6 = r6.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items6 == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: getLegendItems() returned null");
        }
        if (items6.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("relation legendItems-noPlot-emptyAndNonNull violated: expected empty legend collection without plot but got " + items6.getItemCount());
        }
    }
}