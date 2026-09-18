package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
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
                renderer = new LevelRenderer();
                break;
        }

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 8);
            int cols = data.consumeInt(0, 8);
            for (int r = 0; r < rows; r++) {
                String rowKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                if (rowKey == null) {
                    rowKey = "";
                }
                for (int c = 0; c < cols; c++) {
                    String colKey = data.consumeBoolean() ? data.consumeAsciiString(16) : data.consumeString(16);
                    if (colKey == null) {
                        colKey = "";
                    }
                    Number value;
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            value = new Integer(data.consumeInt());
                            break;
                        case 1:
                            value = new Double((double) data.consumeInt());
                            break;
                        case 2:
                            value = new Long((long) data.consumeInt());
                            break;
                        default:
                            value = null;
                            break;
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(
            dataset,
            new CategoryAxis(data.consumeAsciiString(16)),
            new NumberAxis(data.consumeAsciiString(16)),
            renderer
        );

        int tweaks = data.consumeInt(0, 8);
        for (int i = 0; i < tweaks; i++) {
            int series = data.consumeInt(0, 15);
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

        if (data.consumeBoolean()) {
            plot.setDataset(null);
            renderer.getLegendItems();
        }
    }
}