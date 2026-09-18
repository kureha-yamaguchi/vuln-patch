package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 2)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer();
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        renderer.getLegendItems();

        org.jfree.data.category.DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                String rowKey = "R" + r + "_" + data.consumeAsciiString(8);
                for (int c = 0; c < cols; c++) {
                    String colKey = "C" + c + "_" + data.consumeString(8);
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot(
            dataset,
            new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)),
            new org.jfree.chart.axis.NumberAxis(data.consumeString(16)),
            renderer
        );

        int visibilityTweaks = data.consumeInt(0, 6);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 8);
            int mode = data.consumeInt(0, 2);
            Boolean visible;
            if (mode == 0) {
                visible = Boolean.TRUE;
            } else if (mode == 1) {
                visible = Boolean.FALSE;
            } else {
                visible = null;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset replacement = null;
            if (data.consumeBoolean()) {
                replacement = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = "RR" + r + "_" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = "CC" + c + "_" + data.consumeString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        replacement.addValue(value, rowKey, colKey);
                    }
                }
            }
            plot.setDataset(replacement);
            renderer.getLegendItems();
        }
    }
}