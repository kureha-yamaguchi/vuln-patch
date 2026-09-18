package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer[] renderers =
                new AbstractCategoryItemRenderer[data.consumeInt(1, 3)];

        for (int i = 0; i < renderers.length; i++) {
            int choice = data.consumeInt(0, 2);
            AbstractCategoryItemRenderer renderer;
            if (choice == 0) {
                renderer = new BarRenderer();
            } else if (choice == 1) {
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
            } else {
                renderer = new AreaRenderer();
            }

            int visConfigs = data.consumeInt(0, 4);
            for (int s = 0; s < visConfigs; s++) {
                int series = data.consumeInt(0, 6);
                int state = data.consumeInt(0, 2);
                Boolean visible;
                if (state == 0) {
                    visible = Boolean.TRUE;
                } else if (state == 1) {
                    visible = Boolean.FALSE;
                } else {
                    visible = null;
                }
                renderer.setSeriesVisibleInLegend(series, visible);
            }

            if (data.consumeBoolean()) {
                renderer.getLegendItems();
            }

            renderers[i] = renderer;
        }

        org.jfree.chart.plot.CategoryPlot plot = new org.jfree.chart.plot.CategoryPlot();

        for (int i = 0; i < renderers.length; i++) {
            org.jfree.data.category.DefaultCategoryDataset dataset = null;

            if (data.consumeBoolean()) {
                dataset = new org.jfree.data.category.DefaultCategoryDataset();
                int rows = data.consumeInt(0, 4);
                int cols = data.consumeInt(0, 4);

                for (int r = 0; r < rows; r++) {
                    Comparable rowKey = "R" + r + ":" + data.consumeAsciiString(8);
                    for (int c = 0; c < cols; c++) {
                        Comparable colKey = "C" + c + ":" + data.consumeAsciiString(8);
                        Number value;
                        if (data.consumeBoolean()) {
                            value = null;
                        } else {
                            value = Integer.valueOf(data.consumeInt());
                        }
                        dataset.addValue(value, rowKey, colKey);
                    }
                }
            }

            plot.setDataset(i, dataset);
            plot.setRenderer(i, renderers[i]);
        }

        if (data.consumeBoolean()) {
            int extraSlot = renderers.length + data.consumeInt(0, 1);
            plot.setDataset(extraSlot, null);
        }

        int target = data.consumeInt(0, renderers.length - 1);
        renderers[target].getLegendItems();
    }
}