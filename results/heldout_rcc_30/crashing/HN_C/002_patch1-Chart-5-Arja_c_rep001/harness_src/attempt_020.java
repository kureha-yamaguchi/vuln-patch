package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));
        series.setNotify(data.consumeBoolean());

        int operations = data.consumeInt(1, 24);

        Number[] xs = new Number[operations + 8];
        Number[] ys = new Number[operations + 8];

        xs[0] = Integer.valueOf(0);
        xs[1] = Integer.valueOf(1);
        xs[2] = Integer.valueOf(-1);
        xs[3] = Integer.valueOf(Integer.MIN_VALUE);
        xs[4] = Integer.valueOf(Integer.MAX_VALUE);
        xs[5] = Double.valueOf(0.0d);
        xs[6] = Double.valueOf(-0.0d);
        xs[7] = Long.valueOf(1L);

        ys[0] = null;
        ys[1] = Integer.valueOf(0);
        ys[2] = Integer.valueOf(1);
        ys[3] = Integer.valueOf(-1);
        ys[4] = Integer.valueOf(Integer.MIN_VALUE);
        ys[5] = Integer.valueOf(Integer.MAX_VALUE);
        ys[6] = Double.valueOf(0.0d);
        ys[7] = Long.valueOf(-1L);

        for (int i = 8; i < xs.length; i++) {
            int selector = data.consumeInt(0, 7);
            int v = data.consumeInt();
            switch (selector) {
                case 0:
                    xs[i] = Integer.valueOf(v);
                    break;
                case 1:
                    xs[i] = Long.valueOf((long) v);
                    break;
                case 2:
                    xs[i] = Double.valueOf((double) v);
                    break;
                case 3:
                    xs[i] = Float.valueOf((float) v);
                    break;
                case 4:
                    xs[i] = Short.valueOf((short) v);
                    break;
                case 5:
                    xs[i] = Byte.valueOf((byte) v);
                    break;
                case 6:
                    xs[i] = Double.valueOf(v / 3.0d);
                    break;
                default:
                    xs[i] = Integer.valueOf(v % 5);
                    break;
            }

            if (data.consumeBoolean()) {
                ys[i] = null;
            } else {
                int ySelector = data.consumeInt(0, 7);
                int yv = data.consumeInt();
                switch (ySelector) {
                    case 0:
                        ys[i] = Integer.valueOf(yv);
                        break;
                    case 1:
                        ys[i] = Long.valueOf((long) yv);
                        break;
                    case 2:
                        ys[i] = Double.valueOf((double) yv);
                        break;
                    case 3:
                        ys[i] = Float.valueOf((float) yv);
                        break;
                    case 4:
                        ys[i] = Short.valueOf((short) yv);
                        break;
                    case 5:
                        ys[i] = Byte.valueOf((byte) yv);
                        break;
                    case 6:
                        ys[i] = Double.valueOf(yv / 7.0d);
                        break;
                    default:
                        ys[i] = Integer.valueOf(yv % 5);
                        break;
                }
            }
        }

        for (int i = 0; i < operations; i++) {
            int mode = data.consumeInt(0, 5);
            Number x;
            Number y;

            switch (mode) {
                case 0:
                    x = xs[data.consumeInt(0, xs.length - 1)];
                    y = ys[data.consumeInt(0, ys.length - 1)];
                    break;
                case 1:
                    x = xs[i % xs.length];
                    y = ys[(i + 1) % ys.length];
                    break;
                case 2:
                    x = xs[Math.max(0, i - 1)];
                    y = ys[i % ys.length];
                    break;
                case 3:
                    x = xs[0];
                    y = ys[i % ys.length];
                    break;
                case 4:
                    x = xs[1];
                    y = ys[(i * 3) % ys.length];
                    break;
                default:
                    x = xs[(i * 5) % xs.length];
                    y = ys[(i * 7) % ys.length];
                    break;
            }

            series.addOrUpdate(x, y);

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }
            if (data.consumeBoolean()) {
                series.setNotify(data.consumeBoolean());
            }
        }

        if (operations > 0) {
            Number duplicateX = xs[data.consumeInt(0, xs.length - 1)];
            Number y1 = ys[data.consumeInt(0, ys.length - 1)];
            Number y2 = ys[data.consumeInt(0, ys.length - 1)];
            series.addOrUpdate(duplicateX, y1);
            series.addOrUpdate(duplicateX, y2);
        }
    }
}