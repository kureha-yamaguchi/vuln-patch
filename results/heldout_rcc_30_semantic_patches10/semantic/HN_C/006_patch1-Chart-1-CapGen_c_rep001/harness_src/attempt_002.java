package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
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
                renderer = new LineAndShapeRenderer(data.consumeBoolean(), data.consumeBoolean());
                break;
            default:
                renderer = new AreaRenderer();
                break;
        }

        for (int i = 0, n = data.consumeInt(0, 8); i < n; i++) {
            renderer.setSeriesVisibleInLegend(
                data.consumeInt(0, 16),
                data.consumeBoolean() ? Boolean.TRUE : Boolean.FALSE
            );
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));

        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int rowCount = data.consumeInt(0, 6);
            int colCount = data.consumeInt(0, 6);
            for (int r = 0; r < rowCount; r++) {
                Comparable rowKey = data.consumeAsciiString(16) + "_" + r;
                for (int c = 0; c < colCount; c++) {
                    Comparable colKey = data.consumeAsciiString(16) + "_" + c;
                    Number value;
                    if (data.consumeBoolean()) {
                        value = null;
                    } else {
                        value = new Integer(data.consumeInt());
                    }
                    dataset.addValue(value, rowKey, colKey);
                }
            }
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
        } else {
            plot.getRenderer().getLegendItems();
        }

        if (data.consumeBoolean()) {
            plot.setDataset(null);
            renderer.getLegendItems();
        }

        if (dataset != null && data.consumeBoolean()) {
            plot.setDataset(dataset);
            renderer.getLegendItems();
        }
    }
}