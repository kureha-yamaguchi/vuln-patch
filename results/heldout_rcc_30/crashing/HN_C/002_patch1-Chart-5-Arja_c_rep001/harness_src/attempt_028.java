package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        XYSeries series = new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());
        series.setMaximumItemCount(data.consumeInt(0, 32));

        int plannedOps = data.consumeInt(0, 64);
        Number[] seenX = new Number[32];
        int seenCount = 0;

        for (int i = 0; i < plannedOps && data.remainingBytes() >= 0; i++) {
            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int kind = data.consumeInt(0, 11);
                int v = data.consumeInt();
                switch (kind) {
                    case 0:
                        x = Integer.valueOf(v);
                        break;
                    case 1:
                        x = Long.valueOf((long) v);
                        break;
                    case 2:
                        x = Short.valueOf((short) v);
                        break;
                    case 3:
                        x = Byte.valueOf((byte) v);
                        break;
                    case 4:
                        x = Float.valueOf((float) v);
                        break;
                    case 5:
                        x = Double.valueOf((double) v);
                        break;
                    case 6:
                        x = Double.valueOf(v / 3.0d);
                        break;
                    case 7:
                        x = Double.valueOf(Double.NaN);
                        break;
                    case 8:
                        x = Double.valueOf(v < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                        break;
                    case 9:
                        x = new java.math.BigDecimal(data.consumeAsciiString(16).isEmpty() ? "0" : data.consumeAsciiString(16));
                        break;
                    case 10:
                        x = new java.math.BigInteger(data.consumeAsciiString(16).replaceAll("[^0-9-]", "").isEmpty() ? "0" : data.consumeAsciiString(16).replaceAll("[^0-9-]", ""));
                        break;
                    default:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                } else {
                    seenX[data.consumeInt(0, seenX.length - 1)] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int kind = data.consumeInt(0, 10);
                int v = data.consumeInt();
                switch (kind) {
                    case 0:
                        y = Integer.valueOf(v);
                        break;
                    case 1:
                        y = Long.valueOf((long) v);
                        break;
                    case 2:
                        y = Short.valueOf((short) v);
                        break;
                    case 3:
                        y = Byte.valueOf((byte) v);
                        break;
                    case 4:
                        y = Float.valueOf((float) v);
                        break;
                    case 5:
                        y = Double.valueOf((double) v);
                        break;
                    case 6:
                        y = Double.valueOf(v / 7.0d);
                        break;
                    case 7:
                        y = Double.valueOf(Double.NaN);
                        break;
                    case 8:
                        y = Double.valueOf(v < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                        break;
                    case 9:
                        y = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    default:
                        y = new java.math.BigDecimal(Integer.toString(v));
                        break;
                }
            }

            series.addOrUpdate(x, y);

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 32));
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                Number existingX = series.getX(idx);
                Number newY;
                if (data.consumeBoolean()) {
                    newY = null;
                } else {
                    int v = data.consumeInt();
                    newY = data.consumeBoolean() ? Double.valueOf(v / 5.0d) : Integer.valueOf(v);
                }
                series.addOrUpdate(existingX, newY);
            }
        }
    }
}