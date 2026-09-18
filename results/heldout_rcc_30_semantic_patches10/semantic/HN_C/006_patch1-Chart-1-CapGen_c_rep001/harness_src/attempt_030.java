package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BarRenderer renderer = new BarRenderer();

        if (data.consumeBoolean()) {
            renderer.setBaseSeriesVisibleInLegend(data.consumeBoolean());
        }

        int visibilityTweaks = data.consumeInt(0, 8);
        for (int i = 0; i < visibilityTweaks; i++) {
            int series = data.consumeInt(0, 20);
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                renderer.setSeriesVisibleInLegend(series, Boolean.TRUE);
            } else if (mode == 1) {
                renderer.setSeriesVisibleInLegend(series, Boolean.FALSE);
            } else {
                renderer.setSeriesVisibleInLegend(series, null);
            }
        }

        if (data.consumeBoolean()) {
            renderer.getLegendItems();
            return;
        }

        CategoryPlot plot = new CategoryPlot();
        int rendererSlot = data.consumeInt(0, 3);
        plot.setRenderer(rendererSlot, renderer);

        int extraDatasets = data.consumeInt(0, 3);
        for (int i = 0; i < extraDatasets; i++) {
            int slot = data.consumeInt(0, 3);
            DefaultCategoryDataset ds = new DefaultCategoryDataset();
            int entries = data.consumeInt(0, 6);
            for (int j = 0; j < entries; j++) {
                String row = data.consumeAsciiString(16);
                String col = data.consumeString(16);
                if (row.length() == 0) {
                    row = "R" + j;
                }
                if (col.length() == 0) {
                    col = "C" + j;
                }
                Number value;
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        value = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        value = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        value = Byte.valueOf(data.consumeByte());
                        break;
                    case 3:
                        value = null;
                        break;
                    default:
                        value = Double.valueOf((double) data.consumeInt());
                        break;
                }
                ds.addValue(value, row, col);
            }
            plot.setDataset(slot, ds);
        }

        if (data.consumeBoolean()) {
            DefaultCategoryDataset target = new DefaultCategoryDataset();
            int rows = data.consumeInt(0, 4);
            int cols = data.consumeInt(0, 4);
            for (int r = 0; r < rows; r++) {
                String row = data.consumeAsciiString(16);
                if (row.length() == 0) {
                    row = "TR" + r;
                }
                for (int c = 0; c < cols; c++) {
                    String col = data.consumeString(16);
                    if (col.length() == 0) {
                        col = "TC" + c;
                    }
                    target.addValue(Integer.valueOf(data.consumeInt()), row, col);
                }
            }
            plot.setDataset(rendererSlot, target);
        } else {
            plot.setDataset(rendererSlot, null);
        }

        renderer.getLegendItems();
    }
}