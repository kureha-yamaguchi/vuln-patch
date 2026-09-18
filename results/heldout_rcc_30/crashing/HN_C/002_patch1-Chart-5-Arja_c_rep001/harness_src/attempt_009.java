package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 3);
        XYSeries[] seriesArray = new XYSeries[seriesCount];

        for (int s = 0; s < seriesCount; s++) {
            String key;
            if (data.consumeBoolean()) {
                key = data.consumeString(32);
            } else {
                key = data.consumeAsciiString(32);
            }

            boolean autoSort;
            boolean allowDuplicateXValues;

            if (s == 0) {
                autoSort = true;
                allowDuplicateXValues = false;
            } else if (s == 1) {
                autoSort = false;
                allowDuplicateXValues = false;
            } else {
                autoSort = data.consumeBoolean();
                allowDuplicateXValues = data.consumeBoolean();
            }

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 12));
            seriesArray[s] = series;
        }

        int poolSize = data.consumeInt(1, 16);
        Number[] xPool = new Number[poolSize];
        Number[] yPool = new Number[poolSize];

        for (int i = 0; i < poolSize; i++) {
            int a = data.consumeInt();
            int b = data.consumeInt();
            long bits = (((long) a) << 32) ^ (b & 0xffffffffL);

            switch (data.consumeInt(0, 5)) {
                case 0:
                    xPool[i] = Integer.valueOf(a);
                    break;
                case 1:
                    xPool[i] = Long.valueOf(bits);
                    break;
                case 2:
                    xPool[i] = Double.valueOf((double) a);
                    break;
                case 3:
                    xPool[i] = Double.valueOf(Double.longBitsToDouble(bits));
                    break;
                case 4:
                    xPool[i] = Short.valueOf((short) a);
                    break;
                default:
                    xPool[i] = Byte.valueOf((byte) a);
                    break;
            }

            if (data.consumeBoolean()) {
                yPool[i] = null;
            } else {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        yPool[i] = Integer.valueOf(b);
                        break;
                    case 1:
                        yPool[i] = Long.valueOf((((long) b) << 32) ^ (a & 0xffffffffL));
                        break;
                    case 2:
                        yPool[i] = Double.valueOf((double) b);
                        break;
                    case 3:
                        yPool[i] = Double.valueOf(Double.longBitsToDouble(((((long) b) << 32) ^ (a & 0xffffffffL))));
                        break;
                    case 4:
                        yPool[i] = Float.valueOf(Float.intBitsToFloat(b));
                        break;
                    default:
                        yPool[i] = Short.valueOf((short) b);
                        break;
                }
            }
        }

        int operations = data.consumeInt(0, 64);
        for (int op = 0; op < operations; op++) {
            XYSeries series = seriesArray[data.consumeInt(0, seriesArray.length - 1)];

            if (data.consumeInt(0, 9) == 0) {
                series.setMaximumItemCount(data.consumeInt(0, 12));
            }

            Number x;
            if (data.consumeBoolean()) {
                x = xPool[data.consumeInt(0, xPool.length - 1)];
            } else {
                int a = data.consumeInt();
                int b = data.consumeInt();
                long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        x = Integer.valueOf(a);
                        break;
                    case 1:
                        x = Long.valueOf(bits);
                        break;
                    case 2:
                        x = Double.valueOf((double) a);
                        break;
                    case 3:
                        x = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    case 4:
                        x = Short.valueOf((short) a);
                        break;
                    default:
                        x = Byte.valueOf((byte) a);
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = yPool[data.consumeInt(0, yPool.length - 1)];
            } else if (data.consumeBoolean()) {
                y = null;
            } else {
                int a = data.consumeInt();
                int b = data.consumeInt();
                long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        y = Integer.valueOf(a);
                        break;
                    case 1:
                        y = Long.valueOf(bits);
                        break;
                    case 2:
                        y = Double.valueOf((double) a);
                        break;
                    case 3:
                        y = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(a));
                        break;
                    default:
                        y = Short.valueOf((short) a);
                        break;
                }
            }

            XYDataItem result = series.addOrUpdate(x, y);

            if (result != null && data.consumeBoolean()) {
                Number rx = result.getX();
                Number ry = result.getY();
                if (rx != null && data.consumeBoolean()) {
                    series.addOrUpdate(rx, ry);
                }
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (item != null) {
                    series.addOrUpdate(item.getX(), item.getY());
                }
            }
        }
    }
}