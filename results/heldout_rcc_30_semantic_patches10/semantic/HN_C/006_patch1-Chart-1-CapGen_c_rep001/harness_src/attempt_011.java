package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        DefaultCategoryDataset dataset = null;
        if (data.consumeBoolean()) {
            dataset = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 16);
            for (int i = 0; i < entries; i++) {
                dataset.addValue(
                    Integer.valueOf(data.consumeInt()),
                    data.consumeString(24),
                    data.consumeAsciiString(24)
                );
            }
        }

        BarRenderer renderer = new BarRenderer();

        int visibilityOps = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityOps; i++) {
            int series = data.consumeInt(0, 12);
            int state = data.consumeInt(0, 2);
            Boolean visible = state == 0 ? Boolean.TRUE : (state == 1 ? Boolean.FALSE : null);
            renderer.setSeriesVisibleInLegend(series, visible);
        }

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        CategoryAxis domainAxis = new CategoryAxis(data.consumeString(32));
        NumberAxis rangeAxis = new NumberAxis(data.consumeString(32));
        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);

        if (data.consumeBoolean()) {
            plot.setDataset(dataset);
        }
        if (data.consumeBoolean()) {
            plot.setRenderer(renderer);
        }

        renderer.getLegendItems();

        int reconfigurations = data.consumeInt(0, 3);
        for (int r = 0; r < reconfigurations; r++) {
            DefaultCategoryDataset nextDataset = null;
            if (data.consumeBoolean()) {
                nextDataset = new DefaultCategoryDataset();
                int entries2 = data.consumeInt(0, 10);
                for (int i = 0; i < entries2; i++) {
                    nextDataset.addValue(
                        Integer.valueOf(data.consumeInt()),
                        data.consumeAsciiString(16),
                        data.consumeString(16)
                    );
                }
            }
            plot.setDataset(nextDataset);
            renderer.getLegendItems();
        }
    }
}