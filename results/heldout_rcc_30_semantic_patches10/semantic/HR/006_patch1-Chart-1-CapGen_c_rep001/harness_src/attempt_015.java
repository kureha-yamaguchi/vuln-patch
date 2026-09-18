package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer r = new LineAndShapeRenderer();
        LegendItemCollection itemsBeforePlot;
        try {
            itemsBeforePlot = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsBeforePlot == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-preplot] semantic mismatch: expected non-null legend collection before plot attachment but got null");
        }
        if (itemsBeforePlot.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-preplot] semantic mismatch: expected 0 legend items before plot attachment but got " + itemsBeforePlot.getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        try {
            plot.setDataset(dataset);
            plot.setRenderer(r);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection itemsEmptyDataset;
        try {
            itemsEmptyDataset = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (itemsEmptyDataset == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-empty-dataset] semantic mismatch: expected non-null legend collection with empty dataset but got null");
        }
        if (itemsEmptyDataset.getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-empty-dataset] semantic mismatch: expected 0 legend items with empty dataset but got " + itemsEmptyDataset.getItemCount());
        }

        String rowKey = nonEmptyAscii(data.consumeAsciiString(8), "S1");
        String colKey1 = nonEmptyAscii(data.consumeAsciiString(8), "C1");
        int v1 = data.consumeInt(-1000, 1000);

        try {
            dataset.addValue(v1, rowKey, colKey1);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection firstCallAfterAdd;
        LegendItemCollection secondCallAfterAdd;
        try {
            firstCallAfterAdd = r.getLegendItems();
            secondCallAfterAdd = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (firstCallAfterAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-not-null-single-series] semantic mismatch: expected non-null legend collection after first series addition but got null");
        }
        if (firstCallAfterAdd.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:fresh-collection-snapshot] semantic mismatch: expected 1 legend item after first series addition but got " + firstCallAfterAdd.getItemCount());
        }
        if (firstCallAfterAdd.get(0) == null) {
            throw new FuzzerSecurityIssueLow("[oracle:single-item-present] semantic mismatch: expected legend item at index 0 after first series addition but got null");
        }
        String actualLabel = firstCallAfterAdd.get(0).getLabel();
        if (!rowKey.equals(actualLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:single-item-label] semantic mismatch: expected legend label '" + rowKey + "' but got '" + actualLabel + "'");
        }

        /* Contract from the shown method body: getLegendItems() starts with
           `LegendItemCollection result = new LegendItemCollection();` and returns that
           local variable, so each invocation must return a fresh snapshot object.
           A band-aid patch that caches or reuses a stale collection to mask the wrong
           count would violate this post-condition even if one top-level symptom vanished. */
        if (firstCallAfterAdd == secondCallAfterAdd) {
            throw new FuzzerSecurityIssueLow("[oracle:fresh-collection-instance] metamorphic violation: repeated getLegendItems() calls on unchanged state returned the same collection instance");
        }
        if (secondCallAfterAdd == null) {
            throw new FuzzerSecurityIssueLow("[oracle:repeat-not-null] semantic mismatch: expected non-null legend collection on repeated call but got null");
        }
        if (secondCallAfterAdd.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:repeat-count] semantic mismatch: expected repeated getLegendItems() count 1 but got " + secondCallAfterAdd.getItemCount());
        }
        if (secondCallAfterAdd.get(0) == null) {
            throw new FuzzerSecurityIssueLow("[oracle:repeat-item-present] semantic mismatch: expected repeated legend item at index 0 but got null");
        }
        String repeatedLabel = secondCallAfterAdd.get(0).getLabel();
        if (!rowKey.equals(repeatedLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:repeat-label] semantic mismatch: expected repeated legend label '" + rowKey + "' but got '" + repeatedLabel + "'");
        }

        String secondRowKey = nonEmptyAscii(data.consumeAsciiString(8), "S2");
        if (secondRowKey.equals(rowKey)) {
            secondRowKey = secondRowKey + "_2";
        }
        String colKey2 = nonEmptyAscii(data.consumeAsciiString(8), "C2");
        int v2 = data.consumeInt(-1000, 1000);

        try {
            dataset.addValue(v2, secondRowKey, colKey2);
        } catch (Throwable t) {
            return;
        }

        LegendItemCollection afterSecondSeries;
        try {
            afterSecondSeries = r.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        /* Snapshot post-condition: because getLegendItems() constructs and returns a new
           LegendItemCollection each time, a previously returned collection must remain a
           stable snapshot when the dataset changes later. */
        if (firstCallAfterAdd.getItemCount() != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:old-snapshot-count-stable] post-condition violation: previously returned legend collection changed count after later dataset mutation; oldCount=" + firstCallAfterAdd.getItemCount());
        }
        String oldSnapshotLabel = firstCallAfterAdd.get(0).getLabel();
        if (!rowKey.equals(oldSnapshotLabel)) {
            throw new FuzzerSecurityIssueLow("[oracle:old-snapshot-label-stable] post-condition violation: previously returned legend collection changed label after later dataset mutation; expected='" + rowKey + "' actual='" + oldSnapshotLabel + "'");
        }

        if (afterSecondSeries == null) {
            throw new FuzzerSecurityIssueLow("[oracle:two-series-not-null] semantic mismatch: expected non-null legend collection after second series addition but got null");
        }
        if (afterSecondSeries.getItemCount() != 2) {
            throw new FuzzerSecurityIssueLow("[oracle:two-series-count] semantic mismatch: expected 2 legend items after adding a second series but got " + afterSecondSeries.getItemCount());
        }
    }

    private static String nonEmptyAscii(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}