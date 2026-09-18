package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 32));

        int initialOps = data.consumeInt(0, 24);
        Number[] seenX = new Number[initialOps + 8];
        int seenCount = 0;

        for (int i = 0; i < initialOps; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int selector = data.consumeInt(0, 7);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        x = Integer.valueOf(raw);
                        break;
                    case 1:
                        x = Long.valueOf((long) raw);
                        break;
                    case 2:
                        x = Short.valueOf((short) raw);
                        break;
                    case 3:
                        x = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        x = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    default:
                        x = Double.valueOf((double) data.consumeInt(-3, 3));
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int selector = data.consumeInt(0, 6);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        y = Integer.valueOf(raw);
                        break;
                    case 1:
                        y = Long.valueOf((long) raw);
                        break;
                    case 2:
                        y = Short.valueOf((short) raw);
                        break;
                    case 3:
                        y = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        y = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-10, 10));
                        break;
                }
            }

            if (data.consumeBoolean()) {
                series.addOrUpdate(x, y);
            } else {
                series.add(x, y);
            }
        }

        int ops = data.consumeInt(1, 32);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int selector = data.consumeInt(0, 8);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        x = Integer.valueOf(raw);
                        break;
                    case 1:
                        x = Long.valueOf((long) raw);
                        break;
                    case 2:
                        x = Short.valueOf((short) raw);
                        break;
                    case 3:
                        x = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        x = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    case 7:
                        x = Double.valueOf((double) data.consumeInt(-1, 1));
                        break;
                    default:
                        x = Double.valueOf(0.0d);
                        break;
                }
                if (seenCount < seenX.length && data.consumeBoolean()) {
                    seenX[seenCount++] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int selector = data.consumeInt(0, 7);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        y = Integer.valueOf(raw);
                        break;
                    case 1:
                        y = Long.valueOf((long) raw);
                        break;
                    case 2:
                        y = Short.valueOf((short) raw);
                        break;
                    case 3:
                        y = Byte.valueOf((byte) raw);
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(raw));
                        break;
                    case 5:
                        y = Double.valueOf(Double.longBitsToDouble((((long) raw) << 32) ^ (long) data.consumeInt()));
                        break;
                    case 6:
                        y = Double.valueOf((double) data.consumeInt(-100, 100));
                        break;
                    default:
                        y = Integer.valueOf(data.consumeInt(-100, 100));
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (overwritten != null && data.consumeBoolean()) {
                series.addOrUpdate(overwritten.getX(), overwritten.getY());
            }

            if (data.consumeBoolean()) {
                series.indexOf(x);
            }
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                series.getDataItem(data.consumeInt(0, series.getItemCount() - 1));
            }
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 32));
            }
        }
    }
}