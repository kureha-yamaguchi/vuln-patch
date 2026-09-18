package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 5)) {
            case 0:
                renderer = new BarRenderer();
                break;
            case 1:
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            case 2:
                renderer = new AreaRenderer();
                break;
            case 3:
                renderer = new StackedBarRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new WaterfallBarRenderer();
                break;
            default:
                renderer = new LevelRenderer();
                break;
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt();
            Boolean flag;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    flag = Boolean.TRUE;
                    break;
                case 1:
                    flag = Boolean.FALSE;
                    break;
                default:
                    flag = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, flag);
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        }

        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(renderer);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 6);
            int cols = data.consumeInt(0, 6);
            for (int r = 0; r < rows; r++) {
                Comparable rowKey = data.consumeString(16);
                for (int c = 0; c < cols; c++) {
                    Comparable colKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 2:
                            value = Integer.valueOf(0);
                            break;
                        case 3:
                            value = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            value = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(dataset);
        } else {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}