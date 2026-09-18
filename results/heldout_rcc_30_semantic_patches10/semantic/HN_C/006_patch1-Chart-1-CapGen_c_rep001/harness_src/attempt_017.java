package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AbstractCategoryItemRenderer renderer;
        if (data.consumeBoolean()) {
            renderer = new BarRenderer();
        } else {
            renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
        }

        renderer.setSeriesVisible(
                data.consumeInt(0, 8),
                data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE
        );
        renderer.setSeriesVisibleInLegend(
                data.consumeInt(0, 8),
                data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE
        );

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis(data.consumeString(32)));
        plot.setRangeAxis(new NumberAxis(data.consumeString(32)));

        int rendererIndex = data.consumeInt(0, 3);
        plot.setRenderer(rendererIndex, renderer);

        for (int i = 0; i < 4; i++) {
            if (i == rendererIndex) {
                plot.setDataset(i, null);
            } else {
                if (data.consumeBoolean()) {
                    plot.setDataset(i, null);
                } else {
                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    int rows = data.consumeInt(0, 4);
                    int cols = data.consumeInt(0, 4);
                    for (int r = 0; r < rows; r++) {
                        String rowKey = data.consumeString(16) + r;
                        for (int c = 0; c < cols; c++) {
                            String colKey = data.consumeString(16) + c;
                            dataset.addValue(new Integer(data.consumeInt()), rowKey, colKey);
                        }
                    }
                    plot.setDataset(i, dataset);
                }
            }
        }

        renderer.getLegendItems();
    }
}