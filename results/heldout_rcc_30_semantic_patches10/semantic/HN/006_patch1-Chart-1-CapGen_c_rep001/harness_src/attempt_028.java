package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer noPlotRenderer;
        LegendItemCollection noPlotItems;
        try {
            noPlotRenderer = new LineAndShapeRenderer();
            noPlotItems = noPlotRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (noPlotItems == null) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-not-null] semantic mismatch: expected non-null legend collection but got null");
        }
        if (noPlotItems.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:no-plot-empty] semantic mismatch: expected 0 legend items without plot but got " + noPlotItems.getItemCount());
        }

        LineAndShapeRenderer seedRenderer;
        CategoryPlot seedPlot;
        DefaultCategoryDataset seedDataset;
        LegendItemCollection seedItemsBeforeAdd;
        LegendItemCollection seedItemsAfterAdd;
        try {
            seedRenderer = new LineAndShapeRenderer();
            seedPlot = new CategoryPlot();
            seedDataset = new DefaultCategoryDataset();

            seedPlot.setDataset(seedDataset);
            seedPlot.setRenderer(seedRenderer);

            seedItemsBeforeAdd = seedRenderer.getLegendItems();
            seedDataset.addValue(1.0, "S1", "C1");
            seedItemsAfterAdd = seedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (seedRenderer.getPlot() != seedPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-assignment] semantic mismatch: expected renderer.getPlot() to be the assigned plot");
        }
        if (seedItemsBeforeAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-before-not-null] semantic mismatch: expected non-null legend collection before add but got null");
        }
        if (seedItemsBeforeAdd.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-before-empty] semantic mismatch: expected 0 legend items before adding data but got " + seedItemsBeforeAdd.getItemCount());
        }
        if (seedItemsAfterAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-after-not-null] semantic mismatch: expected non-null legend collection after add but got null");
        }
        if (seedItemsAfterAdd.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-after-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got " + seedItemsAfterAdd.getItemCount());
        }
        String seedLabel;
        try {
            seedLabel = seedItemsAfterAdd.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!"S1".equals(seedLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-after-label] semantic mismatch: expected label S1 but got " + String.valueOf(seedLabel));
        }

        LegendItemCollection seedItemsAfterSecondRead;
        try {
            seedItemsAfterSecondRead = seedRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (seedRenderer.getPlot() != seedPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:plot-stable-after-read] semantic mismatch: getLegendItems() changed renderer plot association");
        }
        if (seedItemsAfterSecondRead == null) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-not-null] semantic mismatch: repeated getLegendItems() returned null");
        }
        if (seedItemsAfterSecondRead.getItemCount() != seedItemsAfterAdd.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-count-stable] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset changed itemCount from " + seedItemsAfterAdd.getItemCount() + " to " + seedItemsAfterSecondRead.getItemCount());
        }
        String secondLabel;
        try {
            secondLabel = seedItemsAfterSecondRead.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!seedLabel.equals(secondLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-label-stable] metamorphic violation: repeated getLegendItems() on unchanged renderer/dataset changed label from " + seedLabel + " to " + secondLabel);
        }

        String rowKey = data.consumeAsciiString(8);
        if (rowKey.length() == 0) {
            rowKey = "S";
        }
        String columnKey = data.consumeAsciiString(8);
        if (columnKey.length() == 0) {
            columnKey = "C";
        }
        int numeric = data.consumeInt(-1000000, 1000000);

        LineAndShapeRenderer fuzzRenderer;
        CategoryPlot fuzzPlot;
        DefaultCategoryDataset fuzzDataset;
        LegendItemCollection fuzzItems;
        try {
            fuzzRenderer = new LineAndShapeRenderer();
            fuzzPlot = new CategoryPlot();
            fuzzDataset = new DefaultCategoryDataset();
            fuzzPlot.setDataset(fuzzDataset);
            fuzzPlot.setRenderer(fuzzRenderer);
            fuzzDataset.addValue((double) numeric, rowKey, columnKey);
            fuzzItems = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (fuzzRenderer.getPlot() != fuzzPlot) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-plot-assignment] semantic mismatch: expected renderer.getPlot() to equal assigned plot for fuzz case");
        }
        if (fuzzItems == null) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: getLegendItems() returned null");
        }
        if (fuzzItems.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected 1 legend item for one-series dataset but got " + fuzzItems.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey + " value=" + numeric);
        }
        String fuzzLabel;
        try {
            fuzzLabel = fuzzItems.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!rowKey.equals(fuzzLabel)) {
            throw new FuzzerSecurityIssueLow("relation legendItems-singleSeries-nonNullDataset violated: expected legend label " + rowKey + " but got " + String.valueOf(fuzzLabel) + " columnKey=" + columnKey + " value=" + numeric);
        }

        LegendItemCollection fuzzItemsAgain;
        try {
            fuzzItemsAgain = fuzzRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (fuzzItemsAgain == null) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-repeat-not-null] semantic mismatch: repeated getLegendItems() returned null");
        }
        if (fuzzItemsAgain.getItemCount() != fuzzItems.getItemCount()) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-repeat-count] metamorphic violation: repeated getLegendItems() on unchanged one-series dataset changed itemCount from " + fuzzItems.getItemCount() + " to " + fuzzItemsAgain.getItemCount() + " rowKey=" + rowKey + " columnKey=" + columnKey + " value=" + numeric);
        }
        String fuzzLabelAgain;
        try {
            fuzzLabelAgain = fuzzItemsAgain.get(0).getLabel();
        } catch (Throwable t) {
            return;
        }
        if (!fuzzLabel.equals(fuzzLabelAgain)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-repeat-label] metamorphic violation: repeated getLegendItems() on unchanged one-series dataset changed label from " + fuzzLabel + " to " + fuzzLabelAgain + " rowKey=" + rowKey + " columnKey=" + columnKey + " value=" + numeric);
        }
    }
}