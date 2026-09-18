package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean[][] configs = new boolean[][] {
            {false, false},
            {false, true},
            {true, false},
            {true, true}
        };

        for (int c = 0; c < configs.length; c++) {
            String key = data.consumeString(32);
            XYSeries series = new XYSeries(key, configs[c][0], configs[c][1]);
            series.setMaximumItemCount(data.consumeInt(0, 8));

            java.util.ArrayList<Number> xPool = new java.util.ArrayList<Number>();
            int operations = data.consumeInt(0, 16);

            for (int i = 0; i < operations; i++) {
                Number x;
                if (!xPool.isEmpty() && data.consumeBoolean()) {
                    x = xPool.get(data.consumeInt(0, xPool.size() - 1));
                } else {
                    int kind = data.consumeInt(0, 11);
                    int v = data.consumeInt();
                    switch (kind) {
                        case 0:
                            x = Byte.valueOf((byte) v);
                            break;
                        case 1:
                            x = Short.valueOf((short) v);
                            break;
                        case 2:
                            x = Integer.valueOf(v);
                            break;
                        case 3:
                            x = Long.valueOf((long) v);
                            break;
                        case 4:
                            x = Float.valueOf((float) v);
                            break;
                        case 5:
                            x = Double.valueOf((double) v);
                            break;
                        case 6:
                            x = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            x = Double.valueOf(Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            x = Double.valueOf(Double.NEGATIVE_INFINITY);
                            break;
                        case 9:
                            x = Integer.valueOf(0);
                            break;
                        case 10:
                            x = Integer.valueOf(-1);
                            break;
                        default:
                            x = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                    }
                    if (xPool.size() < 32) {
                        xPool.add(x);
                    }
                }

                Number y;
                if (data.consumeInt(0, 4) == 0) {
                    y = null;
                } else {
                    int kind = data.consumeInt(0, 12);
                    int v = data.consumeInt();
                    switch (kind) {
                        case 0:
                            y = Byte.valueOf((byte) v);
                            break;
                        case 1:
                            y = Short.valueOf((short) v);
                            break;
                        case 2:
                            y = Integer.valueOf(v);
                            break;
                        case 3:
                            y = Long.valueOf((long) v);
                            break;
                        case 4:
                            y = Float.valueOf((float) v);
                            break;
                        case 5:
                            y = Double.valueOf((double) v);
                            break;
                        case 6:
                            y = Double.valueOf(Double.NaN);
                            break;
                        case 7:
                            y = Double.valueOf(Double.POSITIVE_INFINITY);
                            break;
                        case 8:
                            y = Double.valueOf(Double.NEGATIVE_INFINITY);
                            break;
                        case 9:
                            y = Integer.valueOf(0);
                            break;
                        case 10:
                            y = Integer.valueOf(-1);
                            break;
                        case 11:
                            y = Integer.valueOf(Integer.MAX_VALUE);
                            break;
                        default:
                            y = Integer.valueOf(Integer.MIN_VALUE);
                            break;
                    }
                }

                series.addOrUpdate(x, y);
            }

            if (!xPool.isEmpty()) {
                Number existingX = xPool.get(data.consumeInt(0, xPool.size() - 1));
                Number overwriteY;
                if (data.consumeBoolean()) {
                    overwriteY = null;
                } else {
                    int kind = data.consumeInt(0, 8);
                    int v = data.consumeInt();
                    switch (kind) {
                        case 0:
                            overwriteY = Integer.valueOf(v);
                            break;
                        case 1:
                            overwriteY = Long.valueOf((long) v);
                            break;
                        case 2:
                            overwriteY = Double.valueOf((double) v);
                            break;
                        case 3:
                            overwriteY = Float.valueOf((float) v);
                            break;
                        case 4:
                            overwriteY = Double.valueOf(Double.NaN);
                            break;
                        case 5:
                            overwriteY = Double.valueOf(Double.POSITIVE_INFINITY);
                            break;
                        case 6:
                            overwriteY = Double.valueOf(Double.NEGATIVE_INFINITY);
                            break;
                        case 7:
                            overwriteY = Integer.valueOf(0);
                            break;
                        default:
                            overwriteY = Integer.valueOf(-1);
                            break;
                    }
                }
                series.addOrUpdate(existingX, overwriteY);
            }
        }
    }
}