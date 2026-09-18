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

        int seriesTweaks = data.consumeInt(0, 6);
        for (int i = 0; i < seriesTweaks; i++) {
            int choice = data.consumeInt(0, 2);
            if (choice == 0) {
                renderer.setSeriesVisibleInLegend(i, null);
            } else if (choice == 1) {
                renderer.setSeriesVisibleInLegend(i, Boolean.TRUE);
            } else {
                renderer.setSeriesVisibleInLegend(i, Boolean.FALSE);
            }
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
            return;
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeAsciiString(32));

        if (data.consumeBoolean()) {
            CategoryPlot plot = new CategoryPlot(null, domainAxis, rangeAxis, renderer);
            renderer.getLegendItems();
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int rows = data.consumeInt(0, 4);
        int cols = data.consumeInt(0, 4);
        for (int r = 0; r < rows; r++) {
            String rowKey = data.consumeString(16);
            if (rowKey.length() == 0) {
                rowKey = "r" + r;
            }
            for (int c = 0; c < cols; c++) {
                String colKey = data.consumeAsciiString(16);
                if (colKey.length() == 0) {
                    colKey = "c" + c;
                }
                dataset.addValue(new Integer(data.consumeInt()), rowKey, colKey);
            }
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        if (data.consumeBoolean()) {
            plot.setDataset(null);
        }

        renderer.getLegendItems();
    }
}