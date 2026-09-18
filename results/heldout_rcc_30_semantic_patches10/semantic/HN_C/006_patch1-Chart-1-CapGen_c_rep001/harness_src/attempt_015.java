package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

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

        renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        int seriesTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < seriesTweaks; i++) {
            int series = data.consumeInt(0, 10);
            int state = data.consumeInt(0, 2);
            Boolean visible;
            if (state == 0) {
                visible = null;
            } else if (state == 1) {
                visible = Boolean.TRUE;
            } else {
                visible = Boolean.FALSE;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        CategoryPlot plot = new CategoryPlot();
        int slotCount = data.consumeInt(1, 4);
        int targetIndex = data.consumeInt(0, slotCount - 1);

        for (int i = 0; i < slotCount; i++) {
            if (i == targetIndex) {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, buildDataset(data));
                } else {
                    plot.setDataset(i, null);
                }
                plot.setRenderer(i, renderer);
            } else {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, buildDataset(data));
                } else {
                    plot.setDataset(i, null);
                }
                if (data.consumeBoolean()) {
                    switch (data.consumeInt(0, 2)) {
                        case 0:
                            plot.setRenderer(i, new BarRenderer());
                            break;
                        case 1:
                            plot.setRenderer(i, new LineAndShapeRenderer());
                            break;
                        default:
                            plot.setRenderer(i, new AreaRenderer());
                            break;
                    }
                }
            }
        }

        renderer.getLegendItems();

        if (data.consumeBoolean()) {
            plot.setDataset(targetIndex, buildDataset(data));
        } else {
            plot.setDataset(targetIndex, null);
        }

        renderer.getLegendItems();
    }

    private static DefaultCategoryDataset buildDataset(FuzzedDataProvider data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int entries = data.consumeInt(0, 12);
        for (int i = 0; i < entries; i++) {
            Number value;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    value = null;
                    break;
                case 1:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    value = Long.valueOf((long) data.consumeInt());
                    break;
                default:
                    value = Double.valueOf((double) data.consumeInt());
                    break;
            }

            String rowKey = data.consumeString(16);
            String columnKey = data.consumeAsciiString(16);
            dataset.addValue(value, rowKey, columnKey);
        }
        return dataset;
    }
}