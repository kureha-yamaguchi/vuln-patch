package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String baseKey = data.consumeString(32);

        XYSeries[] serieses = new XYSeries[] {
            new XYSeries(baseKey + "_ff", false, false),
            new XYSeries(baseKey + "_ft", false, true),
            new XYSeries(baseKey + "_tf", true, false),
            new XYSeries(baseKey + "_tt", true, true)
        };

        for (int i = 0; i < serieses.length; i++) {
            serieses[i].setMaximumItemCount(data.consumeInt(0, 16));
        }

        Number[] seenX = new Number[16];
        int seenCount = 0;

        int operations = data.consumeInt(0, 40);
        for (int op = 0; op < operations; op++) {
            XYSeries series = serieses[data.consumeInt(0, serieses.length - 1)];

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }

            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int xv = data.consumeInt();
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        x = Integer.valueOf(xv);
                        break;
                    case 1:
                        x = Long.valueOf((long) xv);
                        break;
                    case 2:
                        x = Double.valueOf((double) xv);
                        break;
                    case 3:
                        x = Float.valueOf((float) xv);
                        break;
                    case 4:
                        x = Short.valueOf((short) xv);
                        break;
                    case 5:
                        x = Byte.valueOf((byte) xv);
                        break;
                    case 6:
                        x = Double.valueOf(Double.longBitsToDouble((((long) xv) << 32) ^ (xv & 0xffffffffL)));
                        break;
                    default:
                        switch (xv & 3) {
                            case 0:
                                x = Double.valueOf(Double.NaN);
                                break;
                            case 1:
                                x = Double.valueOf(Double.POSITIVE_INFINITY);
                                break;
                            case 2:
                                x = Double.valueOf(Double.NEGATIVE_INFINITY);
                                break;
                            default:
                                x = Double.valueOf(-0.0d);
                                break;
                        }
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                } else if (data.consumeBoolean()) {
                    seenX[data.consumeInt(0, seenX.length - 1)] = x;
                }
            }

            Number y;
            if (data.consumeInt(0, 7) == 0) {
                y = null;
            } else {
                int yv = data.consumeInt();
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = Integer.valueOf(yv);
                        break;
                    case 1:
                        y = Long.valueOf((long) yv);
                        break;
                    case 2:
                        y = Double.valueOf((double) yv);
                        break;
                    case 3:
                        y = Float.valueOf(Float.intBitsToFloat(yv));
                        break;
                    case 4:
                        y = Short.valueOf((short) yv);
                        break;
                    case 5:
                        y = Byte.valueOf((byte) yv);
                        break;
                    case 6:
                        y = Double.valueOf(Double.longBitsToDouble((((long) yv) << 32) ^ (yv & 0xffffffffL)));
                        break;
                    default:
                        switch (yv & 3) {
                            case 0:
                                y = Double.valueOf(Double.NaN);
                                break;
                            case 1:
                                y = Double.valueOf(Double.POSITIVE_INFINITY);
                                break;
                            case 2:
                                y = Double.valueOf(Double.NEGATIVE_INFINITY);
                                break;
                            default:
                                y = Double.valueOf(-0.0d);
                                break;
                        }
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);
            if (overwritten != null) {
                overwritten.getX();
                overwritten.getY();
                overwritten.compareTo(new XYDataItem(x, y));
            }

            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                if (item != null && data.consumeBoolean()) {
                    series.addOrUpdate(item.getX(), y);
                }
            }

            if (data.consumeBoolean()) {
                series.indexOf(x);
            }
        }

        for (int i = 0; i < serieses.length; i++) {
            XYSeries series = serieses[i];
            if (series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                series.addOrUpdate(item.getX(), item.getY());
            } else if (data.remainingBytes() > 0) {
                series.addOrUpdate(Integer.valueOf(data.consumeInt()), null);
            }
        }
    }
}