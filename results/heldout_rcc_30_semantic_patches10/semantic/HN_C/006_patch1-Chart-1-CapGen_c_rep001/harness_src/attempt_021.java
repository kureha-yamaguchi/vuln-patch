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
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();
        int rendererIndex = data.consumeInt(0, 3);
        plot.setRenderer(rendererIndex, renderer);

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int series = data.consumeInt(0, 8);
            renderer.setSeriesVisibleInLegend(series, data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE);
        }

        for (int i = 0; i < data.consumeInt(0, 3); i++) {
            int otherIndex = data.consumeInt(0, 3);
            if (otherIndex != rendererIndex) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        plot.setRenderer(otherIndex, new BarRenderer());
                        break;
                    case 1:
                        plot.setRenderer(otherIndex, new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean()));
                        break;
                    default:
                        plot.setRenderer(otherIndex, new AreaRenderer());
                        break;
                }
            }
        }

        for (int i = 0; i < data.consumeInt(0, 4); i++) {
            int datasetIndex = data.consumeInt(0, 3);
            if (data.consumeBoolean()) {
                org.jfree.data.category.DefaultCategoryDataset ds =
                        new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);
                for (int r = 0; r < rows; r++) {
                    String rowKey = data.consumeString(8);
                    for (int c = 0; c < cols; c++) {
                        String colKey = data.consumeAsciiString(8);
                        Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                        ds.addValue(value, rowKey, colKey);
                    }
                }
                plot.setDataset(datasetIndex, ds);
            } else {
                plot.setDataset(datasetIndex, null);
            }
        }

        if (data.consumeBoolean()) {
            org.jfree.data.category.DefaultCategoryDataset ds =
                    new org.jfree.data.category.DefaultCategoryDataset();
            int rows = data.consumeInt(0, 3);
            int cols = data.consumeInt(0, 3);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeString(6);
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeAsciiString(6);
                    ds.addValue(Integer.valueOf(data.consumeInt()), rowKey, colKey);
                }
            }
            plot.setDataset(rendererIndex, ds);
            renderer.getLegendItems();
        }

        if (data.consumeBoolean()) {
            plot.setDataset(rendererIndex, null);
        }

        renderer.getLegendItems();
    }
}