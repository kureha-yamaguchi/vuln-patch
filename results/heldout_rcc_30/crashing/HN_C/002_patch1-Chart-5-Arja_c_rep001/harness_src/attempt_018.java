package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 3);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(16);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 8));

            int ops = data.consumeInt(1, 16);
            Number[] seenX = new Number[ops + 4];
            int seenCount = 0;

            for (int i = 0; i < ops; i++) {
                Number x;
                if (seenCount > 0 && data.consumeBoolean()) {
                    x = seenX[data.consumeInt(0, seenCount - 1)];
                } else {
                    int xKind = data.consumeInt(0, 6);
                    int xv = data.consumeInt();
                    switch (xKind) {
                        case 0:
                            x = Integer.valueOf(xv);
                            break;
                        case 1:
                            x = Long.valueOf(xv);
                            break;
                        case 2:
                            x = Short.valueOf((short) xv);
                            break;
                        case 3:
                            x = Byte.valueOf((byte) xv);
                            break;
                        case 4:
                            x = Float.valueOf((float) xv);
                            break;
                        case 5:
                            x = Double.valueOf((double) xv);
                            break;
                        default:
                            long bits = (((long) data.consumeInt()) << 32)
                                    | (((long) data.consumeInt()) & 0xffffffffL);
                            x = Double.valueOf(Double.longBitsToDouble(bits));
                            break;
                    }
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int yKind = data.consumeInt(0, 6);
                    int yv = data.consumeInt();
                    switch (yKind) {
                        case 0:
                            y = Integer.valueOf(yv);
                            break;
                        case 1:
                            y = Long.valueOf(yv);
                            break;
                        case 2:
                            y = Short.valueOf((short) yv);
                            break;
                        case 3:
                            y = Byte.valueOf((byte) yv);
                            break;
                        case 4:
                            y = Float.valueOf((float) yv);
                            break;
                        case 5:
                            y = Double.valueOf((double) yv);
                            break;
                        default:
                            long bits = (((long) data.consumeInt()) << 32)
                                    | (((long) data.consumeInt()) & 0xffffffffL);
                            y = Double.valueOf(Double.longBitsToDouble(bits));
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }

                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }

                int itemCount = series.getItemCount();
                if (itemCount > 0) {
                    XYDataItem item = series.getDataItem(data.consumeInt(0, itemCount - 1));
                    item.getX();
                    item.getY();
                }

                series.indexOf(x);
            }

            if (!allowDuplicateXValues && seenCount > 0) {
                Number x = seenX[data.consumeInt(0, seenCount - 1)];
                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int v = data.consumeInt();
                    y = data.consumeBoolean() ? Integer.valueOf(v) : Double.valueOf((double) v);
                }
                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }
            }

            int finalCount = series.getItemCount();
            if (finalCount > 0) {
                XYDataItem first = series.getDataItem(0);
                first.getX();
                first.getY();
                XYDataItem last = series.getDataItem(finalCount - 1);
                last.getX();
                last.getY();
            }
        }
    }
}