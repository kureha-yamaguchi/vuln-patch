package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LineAndShapeRenderer liftedRenderer = new LineAndShapeRenderer();
        LegendItemCollection liftedInitial = liftedRenderer.getLegendItems();
        if (liftedInitial == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-not-null-inline] semantic mismatch: expected non-null legend collection before plot attachment but was null");
        }
        if (liftedRenderer.getLegendItems().getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-pre-plot-zero-inline] semantic mismatch: expected 0 legend items before plot attachment but got "
                    + liftedRenderer.getLegendItems().getItemCount());
        }

        DefaultCategoryDataset liftedDataset = new DefaultCategoryDataset();
        CategoryPlot liftedPlot = new CategoryPlot();
        liftedPlot.setDataset(liftedDataset);
        liftedPlot.setRenderer(liftedRenderer);
        if (liftedRenderer.getLegendItems().getItemCount() != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-empty-dataset-zero-inline] semantic mismatch: expected 0 legend items for attached empty dataset but got "
                    + liftedRenderer.getLegendItems().getItemCount());
        }

        liftedDataset.addValue(1.0, "S1", "C1");
        LegendItemCollection liftedAfterAdd = liftedRenderer.getLegendItems();
        if (liftedAfterAdd == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-single-series-not-null-inline] semantic mismatch: expected non-null legend collection after adding one series but was null");
        }
        if (liftedAfterAdd.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-single-series-count-inline] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but got "
                    + liftedAfterAdd.getItemCount());
        }
        String liftedLabel = liftedAfterAdd.get(0).getLabel();
        if (!"S1".equals(liftedLabel)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-single-series-label-inline] semantic mismatch: expected first legend label \"S1\" but got \"" + liftedLabel + "\"");
        }

        LineAndShapeRenderer toggleRenderer;
        DefaultCategoryDataset toggleDataset;
        CategoryPlot togglePlot;
        String rowKey;
        String colKey;
        int value;
        LegendItemCollection beforeToggle;
        LegendItemCollection afterHide;
        LegendItemCollection afterShow;
        try {
            toggleRenderer = new LineAndShapeRenderer();
            toggleDataset = new DefaultCategoryDataset();
            togglePlot = new CategoryPlot();
            togglePlot.setDataset(toggleDataset);
            togglePlot.setRenderer(toggleRenderer);

            rowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
            if (rowKey.length() == 0) {
                rowKey = "R";
            }
            colKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
            if (colKey.length() == 0) {
                colKey = "C";
            }
            value = data.consumeInt(-1000, 1000);
            toggleDataset.addValue(value, rowKey, colKey);

            beforeToggle = toggleRenderer.getLegendItems();
            toggleRenderer.setSeriesVisibleInLegend(0, Boolean.FALSE);
            afterHide = toggleRenderer.getLegendItems();
            toggleRenderer.setSeriesVisibleInLegend(0, Boolean.TRUE);
            afterShow = toggleRenderer.getLegendItems();
        } catch (Throwable t) {
            return;
        }

        if (beforeToggle == null || afterHide == null || afterShow == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:legend-visibility-toggle] metamorphic violation: getLegendItems() must return a non-null collection throughout visibility toggling but observed before="
                    + (beforeToggle == null ? "null" : String.valueOf(beforeToggle.getItemCount()))
                    + " hidden=" + (afterHide == null ? "null" : String.valueOf(afterHide.getItemCount()))
                    + " shown=" + (afterShow == null ? "null" : String.valueOf(afterShow.getItemCount())));
        }
        /* Contract justification: getLegendItems() builds legend items only for series where
           isSeriesVisibleInLegend(series) is true; toggling that flag for the only series must
           remove and then restore exactly that legend item. A patch that merely suppresses the
           original symptom or short-circuits the method would violate this post-condition. */
        if (beforeToggle.getItemCount() != 1 || afterHide.getItemCount() != 0 || afterShow.getItemCount() != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:legend-visibility-toggle] metamorphic violation: expected counts 1->0->1 across legend visibility toggle but got "
                    + beforeToggle.getItemCount() + "->" + afterHide.getItemCount() + "->" + afterShow.getItemCount());
        }
        String shownLabel = afterShow.get(0).getLabel();
        if (!rowKey.equals(shownLabel)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:legend-visibility-toggle-label] metamorphic violation: expected restored legend label \"" + rowKey
                    + "\" after re-showing the only series but got \"" + shownLabel + "\"");
        }

        LineAndShapeRenderer readOnlyRenderer;
        DefaultCategoryDataset readOnlyDataset;
        CategoryPlot readOnlyPlot;
        String roRowKey;
        String roColKey;
        Number roValueBefore;
        int rowsBefore;
        int colsBefore;
        int legendsFirst;
        int legendsSecond;
        Number roValueAfter;
        int rowsAfter;
        int colsAfter;
        try {
            readOnlyRenderer = new LineAndShapeRenderer();
            readOnlyDataset = new DefaultCategoryDataset();
            readOnlyPlot = new CategoryPlot();
            readOnlyPlot.setDataset(readOnlyDataset);
            readOnlyPlot.setRenderer(readOnlyRenderer);

            roRowKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
            if (roRowKey.length() == 0) {
                roRowKey = "RS";
            }
            roColKey = data.consumeAsciiString(Math.max(1, data.consumeInt(1, 8)));
            if (roColKey.length() == 0) {
                roColKey = "CS";
            }

            readOnlyDataset.addValue(data.consumeInt(-1000, 1000), roRowKey, roColKey);
            rowsBefore = readOnlyDataset.getRowCount();
            colsBefore = readOnlyDataset.getColumnCount();
            roValueBefore = readOnlyDataset.getValue(roRowKey, roColKey);

            legendsFirst = readOnlyRenderer.getLegendItems().getItemCount();
            legendsSecond = readOnlyRenderer.getLegendItems().getItemCount();

            rowsAfter = readOnlyDataset.getRowCount();
            colsAfter = readOnlyDataset.getColumnCount();
            roValueAfter = readOnlyDataset.getValue(roRowKey, roColKey);
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: getLegendItems() is a query ("Returns the legend items"),
           so repeated reads must not mutate the attached dataset's shape or stored value.
           This hidden-state check catches a wrong implementation even if it returns a
           superficially plausible collection once. */
        if (rowsBefore != rowsAfter || colsBefore != colsAfter) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getter-does-not-mutate-dataset-shape] consistency violation: dataset shape changed across getLegendItems() calls from rows="
                    + rowsBefore + ", cols=" + colsBefore + " to rows=" + rowsAfter + ", cols=" + colsAfter);
        }
        double beforeVal = roValueBefore == null ? Double.NaN : roValueBefore.doubleValue();
        double afterVal = roValueAfter == null ? Double.NaN : roValueAfter.doubleValue();
        boolean sameValue = (Double.isNaN(beforeVal) && Double.isNaN(afterVal)) || beforeVal == afterVal;
        if (!sameValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getter-does-not-mutate-dataset-value] consistency violation: dataset value changed across getLegendItems() calls from "
                    + beforeVal + " to " + afterVal);
        }
        if (legendsFirst != legendsSecond) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getter-repeatable-count] consistency violation: repeated getLegendItems() calls on unchanged state disagreed, first="
                    + legendsFirst + " second=" + legendsSecond);
        }
    }
}