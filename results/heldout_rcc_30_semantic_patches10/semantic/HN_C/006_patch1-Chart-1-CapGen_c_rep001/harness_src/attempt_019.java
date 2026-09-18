package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        switch (data.consumeInt(0, 4)) {
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
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int rendererIndex = data.consumeInt(0, 3);
        CategoryPlot plot = new CategoryPlot();
        plot.setRenderer(rendererIndex, renderer);

        if (data.consumeBoolean()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 8);
            int columnCount = data.consumeInt(0, 8);
            for (int r = 0; r < rowCount; r++) {
                String rowKey = data.consumeString(16);
                for (int c = 0; c < columnCount; c++) {
                    String columnKey = data.consumeAsciiString(16);
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = new Integer(data.consumeInt());
                            break;
                        case 1:
                            value = new Byte(data.consumeByte());
                            break;
                        case 2:
                            value = null;
                            break;
                        default:
                            value = new Integer(data.consumeInt(-1, 1));
                            break;
                    }
                    dataset.addValue(value, rowKey, columnKey);
                }
            }
            plot.setDataset(rendererIndex, dataset);
        } else {
            plot.setDataset(rendererIndex, null);
        }

        int tweaks = data.consumeInt(0, 8);
        for (int i = 0; i < tweaks; i++) {
            int series = data.consumeInt(-2, 12);
            Boolean visible;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    visible = Boolean.TRUE;
                    break;
                case 1:
                    visible = Boolean.FALSE;
                    break;
                default:
                    visible = null;
                    break;
            }
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        renderer.getLegendItems();
    }
}