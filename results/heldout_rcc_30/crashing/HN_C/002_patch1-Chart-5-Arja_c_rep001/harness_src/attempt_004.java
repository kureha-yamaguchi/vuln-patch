package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeAsciiString(32);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 32));

        int poolSize = data.consumeInt(1, 8);
        Number[] xPool = new Number[poolSize];

        for (int i = 0; i < poolSize; i++) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
                case 0:
                    xPool[i] = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    xPool[i] = Long.valueOf(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 2:
                    xPool[i] = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    break;
                case 3:
                    xPool[i] = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                    break;
                case 4:
                    xPool[i] = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    xPool[i] = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    xPool[i] = Integer.valueOf(i);
                    break;
                case 7:
                    xPool[i] = Integer.valueOf(-i);
                    break;
                case 8:
                    xPool[i] = Integer.valueOf(0);
                    break;
                default:
                    xPool[i] = Double.valueOf((double) data.consumeInt());
                    break;
            }
        }

        int ops = data.consumeInt(0, 48);
        for (int i = 0; i < ops; i++) {
            Number x;
            if (data.consumeBoolean()) {
                x = xPool[data.consumeInt(0, xPool.length - 1)];
            } else {
                int kind = data.consumeInt(0, 9);
                switch (kind) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Long.valueOf(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 3:
                        x = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        x = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 7:
                        x = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 8:
                        x = Double.valueOf(Double.NaN);
                        break;
                    default:
                        x = Double.valueOf(0.0d);
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 9);
                switch (yKind) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Long.valueOf(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 3:
                        y = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        y = Integer.valueOf(0);
                        break;
                    case 7:
                        y = Integer.valueOf(-1);
                        break;
                    case 8:
                        y = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    default:
                        y = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                }
            }

            series.addOrUpdate(x, y);
        }

        if (xPool.length > 0) {
            Number x0 = xPool[0];
            series.addOrUpdate(x0, Integer.valueOf(1));
            series.addOrUpdate(x0, null);
            series.addOrUpdate(x0, Double.valueOf(Double.NaN));
        }

        if (xPool.length > 1) {
            Number x1 = xPool[1];
            series.addOrUpdate(x1, Integer.valueOf(-1));
            series.addOrUpdate(x1, Double.valueOf(Double.POSITIVE_INFINITY));
        }

        series.addOrUpdate(Integer.valueOf(0), Integer.valueOf(0));
        series.addOrUpdate(Integer.valueOf(-1), Integer.valueOf(1));
        series.addOrUpdate(Integer.valueOf(1), Integer.valueOf(-1));
        series.addOrUpdate(Double.valueOf(-0.0d), Double.valueOf(0.0d));
        series.addOrUpdate(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN));
    }
}