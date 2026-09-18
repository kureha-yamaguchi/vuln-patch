package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static String nonEmptyAscii(FuzzedDataProvider data, int maxLen, String fallback) {
        String s = data.consumeAsciiString(Math.max(1, maxLen));
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }

    private static void fail(String id, String msg) {
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String rowKeyA = nonEmptyAscii(data, 8, "S1");
        String rowKeyB = nonEmptyAscii(data, 8, "S2");
        String colKeyA = nonEmptyAscii(data, 8, "C1");
        String colKeyB = nonEmptyAscii(data, 8, "C2");
        Number valueA = Integer.valueOf(data.consumeInt(-1000, 1000));
        Number valueB = Integer.valueOf(data.consumeInt(-1000, 1000));

        if (rowKeyA.equals(rowKeyB)) {
            rowKeyB = rowKeyB + "_x";
        }
        if (colKeyA.equals(colKeyB)) {
            colKeyB = colKeyB + "_y";
        }

        LineAndShapeRenderer target = new LineAndShapeRenderer();
        AreaRenderer sibling = new AreaRenderer();
        org.jfree.data.category.DefaultCategoryDataset ds1 = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.data.category.DefaultCategoryDataset ds2 = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();

        org.jfree.chart.LegendItemCollection beforeAttach;
        org.jfree.chart.LegendItemCollection withNullGap;
        org.jfree.chart.LegendItemCollection siblingItems;
        org.jfree.chart.LegendItemCollection afterNullAgain;
        org.jfree.chart.LegendItem direct0;
        org.jfree.chart.LegendItem direct1;
        int targetIndex;
        int siblingIndex;

        try {
            beforeAttach = target.getLegendItems();

            ds1.addValue(valueA, rowKeyA, colKeyA);
            ds2.addValue(valueB, rowKeyB, colKeyB);

            plot.setDataset(0, null);
            plot.setRenderer(0, null);

            plot.setDataset(1, ds1);
            plot.setRenderer(1, target);

            plot.setDataset(2, ds2);
            plot.setRenderer(2, sibling);

            withNullGap = target.getLegendItems();
            siblingItems = sibling.getLegendItems();
            direct0 = target.getLegendItem(1, 0);
            direct1 = sibling.getLegendItem(2, 0);
            targetIndex = plot.getIndexOf(target);
            siblingIndex = plot.getIndexOf(sibling);

            plot.setDataset(1, null);
            afterNullAgain = target.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (beforeAttach == null) {
            fail("null-gap-preplot-notnull", "getLegendItems() returned null before plot attachment");
        }

        if (beforeAttach.getItemCount() != 0) {
            fail("null-gap-preplot-zero", "expected 0 legend items before plot attachment but got " + beforeAttach.getItemCount());
        }

        if (target.getPlot() != plot || sibling.getPlot() != plot) {
            fail("null-gap-plot-coupling", "renderer plot association did not match plot after setRenderer");
        }

        if (targetIndex != 1) {
            fail("null-gap-target-index", "expected target renderer index 1 but got " + targetIndex);
        }

        if (siblingIndex != 2) {
            fail("null-gap-sibling-index", "expected sibling renderer index 2 but got " + siblingIndex);
        }

        /* Contract from getLegendItems(): for a renderer assigned to a plot with a non-null dataset,
           it returns the legend items for the series that renderer draws. This is the patched boundary:
           slot 0 is null, but the target renderer's own slot 1 has a non-null dataset and one default-visible row,
           so a guard that only special-cases the seed or confuses neighboring null slots will incorrectly return empty. */
        if (withNullGap == null) {
            fail("null-gap-slot1-notnull", "target getLegendItems() returned null with non-null dataset at slot 1");
        }
        if (withNullGap.getItemCount() != 1) {
            fail("null-gap-slot1-count", "expected 1 legend item at slot 1 with null neighboring slot but got " + withNullGap.getItemCount());
        }
        String targetLabel = withNullGap.get(0).getLabel();
        if (!rowKeyA.equals(targetLabel)) {
            fail("null-gap-slot1-label", "expected target legend label '" + rowKeyA + "' but got '" + targetLabel + "'");
        }
        if (withNullGap.get(0).getDatasetIndex() != 1) {
            fail("null-gap-slot1-dataset-index", "expected target legend datasetIndex 1 but got " + withNullGap.get(0).getDatasetIndex());
        }

        /* Independent sibling oracle on a different real subclass sharing AbstractCategoryItemRenderer.getLegendItems():
           the same superclass method must also work when a different renderer sits at another non-zero slot. */
        if (siblingItems == null) {
            fail("null-gap-sibling-notnull", "sibling getLegendItems() returned null with non-null dataset at slot 2");
        }
        if (siblingItems.getItemCount() != 1) {
            fail("null-gap-sibling-count", "expected 1 sibling legend item at slot 2 but got " + siblingItems.getItemCount());
        }
        String siblingLabel = siblingItems.get(0).getLabel();
        if (!rowKeyB.equals(siblingLabel)) {
            fail("null-gap-sibling-label", "expected sibling legend label '" + rowKeyB + "' but got '" + siblingLabel + "'");
        }
        if (siblingItems.get(0).getDatasetIndex() != 2) {
            fail("null-gap-sibling-dataset-index", "expected sibling legend datasetIndex 2 but got " + siblingItems.get(0).getDatasetIndex());
        }

        /* Consistency cross-check: getLegendItems() is a collection of the per-series legend items that
           getLegendItem(datasetIndex, series) would produce for visible series. For our valid-by-construction
           single-series datasets, the collection count and first item must agree with the direct call. */
        if (direct0 == null) {
            fail("null-gap-direct-target-nonnull", "direct target getLegendItem(1, 0) returned null");
        }
        if (!rowKeyA.equals(direct0.getLabel())) {
            fail("null-gap-direct-target-label", "expected direct target legend label '" + rowKeyA + "' but got '" + direct0.getLabel() + "'");
        }
        if (direct0.getDatasetIndex() != 1) {
            fail("null-gap-direct-target-dataset-index", "expected direct target datasetIndex 1 but got " + direct0.getDatasetIndex());
        }

        if (direct1 == null) {
            fail("null-gap-direct-sibling-nonnull", "direct sibling getLegendItem(2, 0) returned null");
        }
        if (!rowKeyB.equals(direct1.getLabel())) {
            fail("null-gap-direct-sibling-label", "expected direct sibling legend label '" + rowKeyB + "' but got '" + direct1.getLabel() + "'");
        }
        if (direct1.getDatasetIndex() != 2) {
            fail("null-gap-direct-sibling-dataset-index", "expected direct sibling datasetIndex 2 but got " + direct1.getDatasetIndex());
        }

        /* Mandatory post-condition / flipped-boundary check:
           after flipping the target renderer's own dataset from non-null back to null, getLegendItems()
           must become empty again. A band-aid patch that merely suppresses the known symptom, or that
           ignores the renderer's actual slot/dataset association, breaks this observable transition. */
        if (afterNullAgain == null) {
            fail("null-gap-flipback-notnull", "getLegendItems() returned null after dataset was reset to null");
        }
        if (afterNullAgain.getItemCount() != 0) {
            fail("null-gap-flipback-zero", "expected 0 legend items after resetting target dataset to null but got " + afterNullAgain.getItemCount());
        }
    }
}