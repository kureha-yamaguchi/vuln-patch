package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));

        Number[] seenX = new Number[32];
        int seenCount = 0;

        int ops = data.consumeInt(0, 32);
        for (int i = 0; i < ops; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }

            Number x;
            if (data.consumeInt(0, 9) == 0) {
                x = null;
            } else if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Double.valueOf(data.consumeBoolean() ? Double.NaN : data.consumeInt());
                        break;
                    case 2:
                        x = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : -0.0d);
                        break;
                    case 3:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        x = Float.valueOf(data.consumeBoolean() ? Float.NEGATIVE_INFINITY : (float) data.consumeInt());
                        break;
                    case 5:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        x = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, 16)));
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Double.valueOf(data.consumeBoolean() ? Double.NaN : data.consumeInt());
                        break;
                    case 2:
                        y = Double.valueOf(data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                        break;
                    case 3:
                        y = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        y = Float.valueOf(data.consumeBoolean() ? Float.NaN : (float) data.consumeInt());
                        break;
                    case 5:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt() / (double) (data.consumeInt(1, 16)));
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (x != null && seenCount < seenX.length && data.consumeBoolean()) {
                seenX[seenCount++] = x;
            }

            if (overwritten != null && data.consumeBoolean()) {
                series.addOrUpdate(overwritten.getX(), overwritten.getY());
            }

            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (data.consumeBoolean()) {
                    series.addOrUpdate(item.getX(), y);
                } else {
                    series.addOrUpdate(item.getX(), item.getY());
                }
            }
        }
    }
}