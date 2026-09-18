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
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
        }

        if (data.consumeBoolean()) {
            renderer.setSeriesVisible(data.consumeInt(0, 5), Boolean.valueOf(data.consumeBoolean()));
        }
        if (data.consumeBoolean()) {
            renderer.setSeriesVisibleInLegend(data.consumeInt(0, 5), Boolean.valueOf(data.consumeBoolean()));
        }

        renderer.getLegendItems();

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        plot.setDomainAxis(new org.jfree.chart.axis.CategoryAxis(data.consumeString(16)));
        plot.setRangeAxis(new org.jfree.chart.axis.NumberAxis(data.consumeString(16)));

        int rendererSlots = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, rendererSlots - 1);

        for (int i = 0; i < rendererSlots; i++) {
            if (i == targetIndex) {
                plot.setRenderer(i, renderer);
            } else {
                AbstractCategoryItemRenderer otherRenderer;
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        otherRenderer = new BarRenderer();
                        break;
                    case 1:
                        otherRenderer = new AreaRenderer();
                        break;
                    default:
                        otherRenderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                        break;
                }
                plot.setRenderer(i, otherRenderer);
            }

            if (i != targetIndex || data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, null);
                } else {
                    org.jfree.data.category.DefaultCategoryDataset ds =
                            new org.jfree.data.category.DefaultCategoryDataset();
                    int rows = data.consumeInt(0, 5);
                    int cols = data.consumeInt(0, 5);
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                            ds.addValue(
                                    value,
                                    "row" + r + "_" + data.consumeString(12),
                                    "col" + c + "_" + data.consumeAsciiString(12));
                        }
                    }
                    plot.setDataset(i, ds);
                }
            }
        }

        if (data.consumeBoolean()) {
            plot.setDataset(targetIndex, null);
        } else {
            org.jfree.data.category.DefaultCategoryDataset ds =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 5);
            int cols = data.consumeInt(0, 5);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    ds.addValue(
                            value,
                            "targetRow" + r + "_" + data.consumeString(12),
                            "targetCol" + c + "_" + data.consumeAsciiString(12));
                }
            }
            plot.setDataset(targetIndex, ds);
        }

        renderer.getLegendItems();
    }
}