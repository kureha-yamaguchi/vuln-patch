package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 3);
        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            if (key == null) {
                key = "";
            }

            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();
            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

            series.setMaximumItemCount(data.consumeInt(0, 16));

            Number[] seenX = new Number[64];
            int seenCount = 0;

            int operations = data.consumeInt(0, 24);
            for (int i = 0; i < operations; i++) {
                if (data.consumeBoolean()) {
                    series.setMaximumItemCount(data.consumeInt(0, 16));
                }

                Number x;
                boolean reuseX = seenCount > 0 && data.consumeBoolean();
                if (reuseX) {
                    x = seenX[data.consumeInt(0, seenCount - 1)];
                } else {
                    int xKind = data.consumeInt(0, 9);
                    switch (xKind) {
                        case 0:
                            x = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            x = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            x = Short.valueOf((short) data.consumeInt());
                            break;
                        case 3:
                            x = Byte.valueOf(data.consumeByte());
                            break;
                        case 4:
                            x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 5:
                            x = Double.valueOf(Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32)
                                            | (((long) data.consumeInt()) & 0xffffffffL)));
                            break;
                        case 6:
                            x = java.math.BigInteger.valueOf((long) data.consumeInt());
                            break;
                        case 7:
                            x = java.math.BigDecimal.valueOf((long) data.consumeInt(), data.consumeInt(0, 6));
                            break;
                        case 8:
                            x = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                        default:
                            x = Integer.valueOf(Integer.MAX_VALUE);
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
                    int yKind = data.consumeInt(0, 9);
                    switch (yKind) {
                        case 0:
                            y = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            y = Long.valueOf((long) data.consumeInt());
                            break;
                        case 2:
                            y = Short.valueOf((short) data.consumeInt());
                            break;
                        case 3:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                        case 4:
                            y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 5:
                            y = Double.valueOf(Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32)
                                            | (((long) data.consumeInt()) & 0xffffffffL)));
                            break;
                        case 6:
                            y = java.math.BigInteger.valueOf((long) data.consumeInt());
                            break;
                        case 7:
                            y = java.math.BigDecimal.valueOf((long) data.consumeInt(), data.consumeInt(0, 6));
                            break;
                        case 8:
                            y = Integer.valueOf(0);
                            break;
                        default:
                            y = Integer.valueOf(-1);
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);

                if (overwritten != null && data.consumeBoolean()) {
                    Number ox = overwritten.getX();
                    Number oy = overwritten.getY();
                    if (ox != null) {
                        series.indexOf(ox);
                    }
                    if (oy != null && data.consumeBoolean()) {
                        series.addOrUpdate(ox, oy);
                    }
                }

                if (series.getItemCount() > 0 && data.consumeBoolean()) {
                    int idx = data.consumeInt(0, series.getItemCount() - 1);
                    XYDataItem item = series.getDataItem(idx);
                    if (item != null && item.getX() != null && data.consumeBoolean()) {
                        series.addOrUpdate(item.getX(), item.getY());
                    }
                }
            }
        }
    }
}