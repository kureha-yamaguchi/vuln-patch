package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Comparable key;
        if (data.consumeBoolean()) {
            key = data.consumeAsciiString(32);
        } else {
            key = Integer.valueOf(data.consumeInt());
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (data.consumeBoolean()) {
            series.setMaximumItemCount(data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        }

        int ops = data.consumeInt(0, 64);
        for (int i = 0; i < ops; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
            }

            Number x;
            int xKind = data.consumeInt(0, 6);
            switch (xKind) {
                case 0:
                    x = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    x = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    x = Double.valueOf((double) data.consumeInt());
                    break;
                case 3:
                    x = Float.valueOf((float) data.consumeInt());
                    break;
                case 4:
                    x = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    x = Byte.valueOf(data.consumeByte());
                    break;
                default:
                    x = null;
                    break;
            }

            Number y;
            int yKind = data.consumeInt(0, 6);
            switch (yKind) {
                case 0:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    y = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    y = Double.valueOf((double) data.consumeInt());
                    break;
                case 3:
                    y = Float.valueOf((float) data.consumeInt());
                    break;
                case 4:
                    y = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    y = Byte.valueOf(data.consumeByte());
                    break;
                default:
                    y = null;
                    break;
            }

            if (data.consumeBoolean() && series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                Number existingX = series.getX(idx);
                if (data.consumeBoolean()) {
                    x = existingX;
                } else if (existingX != null) {
                    double v = existingX.doubleValue();
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            x = Double.valueOf(v);
                            break;
                        case 1:
                            x = Float.valueOf((float) v);
                            break;
                        case 2:
                            x = Long.valueOf((long) v);
                            break;
                        default:
                            x = Integer.valueOf((int) v);
                            break;
                    }
                }
            }

            series.addOrUpdate(x, y);
        }
    }
}