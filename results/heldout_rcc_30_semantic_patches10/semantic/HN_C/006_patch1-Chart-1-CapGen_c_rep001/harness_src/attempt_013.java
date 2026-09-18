package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 3)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 10);
            if (data.consumeBoolean()) {
                renderer.setSeriesVisibleInLegend(series, Boolean.valueOf(data.consumeBoolean()));
            } else {
                renderer.setSeriesVisible(series, Boolean.valueOf(data.consumeBoolean()));
            }
        }

        if (data.consumeBoolean()) {
            CategoryPlot plot = new CategoryPlot();
            plot.setRenderer(renderer);

            if (data.consumeBoolean()) {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                int entries = data.consumeInt(0, 20);
                for (int i = 0; i < entries; i++) {
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        case 2:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 3:
                            value = null;
                            break;
                        default:
                            value = Integer.valueOf(data.remainingBytes());
                            break;
                    }
                    String rowKey = data.consumeString(16);
                    String columnKey = data.consumeAsciiString(16);
                    dataset.addValue(value, rowKey, columnKey);
                }
                plot.setDataset(dataset);
            } else {
                plot.setDataset(null);
            }
        }

        renderer.getLegendItems();
    }
}