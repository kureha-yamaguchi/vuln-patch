package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 16));

            int ops = data.consumeInt(0, 24);
            Number[] seenX = new Number[ops > 0 ? ops : 1];
            int seenCount = 0;

            for (int i = 0; i < ops; i++) {
                Number x;
                if (seenCount > 0 && data.consumeBoolean()) {
                    x = seenX[data.consumeInt(0, seenCount - 1)];
                } else {
                    int xKind = data.consumeInt(0, 9);
                    int raw = data.consumeInt();
                    switch (xKind) {
                        case 0:
                            x = Integer.valueOf(raw);
                            break;
                        case 1:
                            x = Long.valueOf((long) raw);
                            break;
                        case 2: {
                            int divisor = data.consumeInt(1, 1024);
                            x = Double.valueOf(((double) raw) / divisor);
                            break;
                        }
                        case 3: {
                            int divisor = data.consumeInt(1, 1024);
                            x = Float.valueOf(((float) raw) / divisor);
                            break;
                        }
                        case 4:
                            x = Short.valueOf((short) raw);
                            break;
                        case 5:
                            x = Byte.valueOf(data.consumeByte());
                            break;
                        case 6:
                            x = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            x = Double.valueOf(raw < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            x = Double.valueOf(-0.0d);
                            break;
                        default:
                            x = Double.valueOf(0.0d);
                            break;
                    }
                    if (seenCount < seenX.length) {
                        seenX[seenCount++] = x;
                    }
                }

                Number y;
                if (data.consumeInt(0, 5) == 0) {
                    y = null;
                } else {
                    int yKind = data.consumeInt(0, 9);
                    int rawY = data.consumeInt();
                    switch (yKind) {
                        case 0:
                            y = Integer.valueOf(rawY);
                            break;
                        case 1:
                            y = Long.valueOf((long) rawY);
                            break;
                        case 2: {
                            int divisor = data.consumeInt(1, 1024);
                            y = Double.valueOf(((double) rawY) / divisor);
                            break;
                        }
                        case 3: {
                            int divisor = data.consumeInt(1, 1024);
                            y = Float.valueOf(((float) rawY) / divisor);
                            break;
                        }
                        case 4:
                            y = Short.valueOf((short) rawY);
                            break;
                        case 5:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                        case 6:
                            y = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            y = Double.valueOf(rawY < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            y = Double.valueOf(-0.0d);
                            break;
                        default:
                            y = Double.valueOf(0.0d);
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }

                if (data.consumeInt(0, 7) == 0) {
                    series.setMaximumItemCount(data.consumeInt(0, 16));
                }
            }

            if (data.consumeBoolean() && seenCount > 0) {
                Number x = seenX[data.consumeInt(0, seenCount - 1)];

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    int rawY = data.consumeInt();
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            y = Integer.valueOf(rawY);
                            break;
                        case 1:
                            y = Long.valueOf((long) rawY);
                            break;
                        case 2:
                            y = Double.valueOf((double) rawY);
                            break;
                        case 3:
                            y = Float.valueOf((float) rawY);
                            break;
                        case 4:
                            y = Double.valueOf(Double.NaN);
                            break;
                        default:
                            y = Double.valueOf(rawY < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                }
            }

            if (data.consumeBoolean()) {
                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    y = Integer.valueOf(data.consumeInt());
                }
                series.addOrUpdate(null, y);
            }
        }
    }
}