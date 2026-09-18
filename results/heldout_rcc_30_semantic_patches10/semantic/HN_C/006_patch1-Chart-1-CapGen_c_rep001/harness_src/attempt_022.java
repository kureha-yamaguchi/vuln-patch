package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.SortOrder;
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
                renderer = new CategoryStepRenderer(data.consumeBoolean());
                break;
            case 4:
                renderer = new LevelRenderer();
                break;
            default:
                renderer = new WaterfallBarRenderer();
                break;
        }

        int seriesFlagCount = data.consumeInt(0, 6);
        for (int i = 0; i < seriesFlagCount; i++) {
            int series = data.consumeInt(0, 8);
            if (data.consumeBoolean()) {
                renderer.setSeriesVisibleInLegend(series, data.consumeBoolean());
            } else {
                renderer.setSeriesVisible(series, data.consumeBoolean());
            }
        }

        CategoryPlot plot = new CategoryPlot();
        if (data.consumeBoolean()) {
            plot.setRowRenderingOrder(data.consumeBoolean() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        }

        int rendererIndex = data.consumeInt(0, 2);
        plot.setRenderer(rendererIndex, renderer);

        int datasetMode = data.consumeInt(0, 3);
        if (datasetMode == 0) {
            plot.setDataset(rendererIndex, null);
        } else if (datasetMode == 1) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 4);
            int cols = data.consumeInt(0, 4);
            for (int r = 0; r < rows; r++) {
                Comparable<?> rowKey = data.consumeAsciiString(8);
                for (int c = 0; c < cols; c++) {
                    Comparable<?> colKey = data.consumeString(8);
                    Number value = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
                    dataset.addValue(value, rowKey, colKey);
                }
            }
            plot.setDataset(rendererIndex, dataset);
        } else if (datasetMode == 2) {
            DefaultCategoryDataset datasetAtOtherIndex = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 6);
            for (int i = 0; i < entries; i++) {
                datasetAtOtherIndex.addValue(
                        data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()),
                        data.consumeAsciiString(8),
                        data.consumeString(8));
            }
            int otherIndex = rendererIndex == 0 ? 1 : 0;
            plot.setDataset(otherIndex, datasetAtOtherIndex);
        } else {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 8);
            for (int i = 0; i < entries; i++) {
                dataset.addValue(
                        Integer.valueOf(data.consumeInt()),
                        data.consumeAsciiString(8),
                        data.consumeString(8));
            }
            plot.setDataset(rendererIndex, dataset);

            if (data.consumeBoolean()) {
                int secondIndex = rendererIndex == 2 ? 1 : 2;
                if (data.consumeBoolean()) {
                    plot.setDataset(secondIndex, null);
                } else {
                    DefaultCategoryDataset second = new DefaultCategoryDataset();
                    int secondEntries = data.consumeInt(0, 4);
                    for (int i = 0; i < secondEntries; i++) {
                        second.addValue(
                                data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()),
                                data.consumeAsciiString(8),
                                data.consumeString(8));
                    }
                    plot.setDataset(secondIndex, second);
                }
            }
        }

        if (data.consumeBoolean()) {
            renderer.setPlot(plot);
        }

        renderer.getLegendItems();
    }
}