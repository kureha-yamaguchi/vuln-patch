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

        series.setMaximumItemCount(data.consumeInt(0, 32));

        int initialAdds = data.consumeInt(0, 24);
        for (int i = 0; i < initialAdds; i++) {
            Number x = consumeNumber(data, false);
            Number y = consumeNumber(data, true);
            if (data.consumeBoolean()) {
                series.add(x, y, data.consumeBoolean());
            } else {
                series.addOrUpdate(x, y);
            }
        }

        int ops = data.consumeInt(1, 32);
        for (int i = 0; i < ops; i++) {
            int action = data.consumeInt(0, 5);
            switch (action) {
                case 0: {
                    Number x = consumeNumber(data, false);
                    Number y = consumeNumber(data, true);
                    series.addOrUpdate(x, y);
                    break;
                }
                case 1: {
                    Number x;
                    if (series.getItemCount() > 0 && data.consumeBoolean()) {
                        int idx = data.consumeInt(0, series.getItemCount() - 1);
                        x = series.getX(idx);
                    } else {
                        x = consumeNumber(data, false);
                    }
                    Number y = consumeNumber(data, true);
                    series.addOrUpdate(x, y);
                    break;
                }
                case 2: {
                    Number x = consumeNumber(data, false);
                    Number y = consumeNumber(data, true);
                    series.add(x, y, data.consumeBoolean());
                    break;
                }
                case 3: {
                    series.setMaximumItemCount(data.consumeInt(0, 32));
                    break;
                }
                case 4: {
                    if (series.getItemCount() > 0) {
                        int idx = data.consumeInt(0, series.getItemCount() - 1);
                        Number x = series.getX(idx);
                        Number y = consumeNumber(data, true);
                        series.addOrUpdate(x, y);
                    }
                    break;
                }
                case 5: {
                    if (series.getItemCount() > 0 && data.consumeBoolean()) {
                        series.remove(data.consumeInt(0, series.getItemCount() - 1));
                    } else {
                        series.clear();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static Number consumeNumber(FuzzedDataProvider data, boolean allowNull) {
        if (allowNull && data.consumeInt(0, 9) == 0) {
            return null;
        }

        switch (data.consumeInt(0, 11)) {
            case 0:
                return Integer.valueOf(data.consumeInt());
            case 1:
                return Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            case 2:
                return Double.valueOf(Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
            case 3:
                return Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
            case 4:
                return Short.valueOf((short) data.consumeInt());
            case 5:
                return Byte.valueOf(data.consumeByte());
            case 6:
                return Integer.valueOf(0);
            case 7:
                return Integer.valueOf(1);
            case 8:
                return Integer.valueOf(-1);
            case 9:
                return Double.valueOf(Double.NaN);
            case 10:
                return Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
            case 11:
            default:
                return Long.valueOf(data.consumeBoolean() ? Long.MIN_VALUE : Long.MAX_VALUE);
        }
    }
}