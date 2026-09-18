package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = 1 + Math.abs(data.consumeInt() % 3);
        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 16));

            Number pivotX = consumeNumber(data, false);
            Number pivotY1 = consumeNumber(data, true);
            Number pivotY2 = consumeNumber(data, true);

            series.addOrUpdate(pivotX, pivotY1);
            series.addOrUpdate(pivotX, pivotY2);

            Number smallerX = relatedNumber(pivotX, -1, data);
            Number largerX = relatedNumber(pivotX, 1, data);
            series.addOrUpdate(smallerX, consumeNumber(data, true));
            series.addOrUpdate(largerX, consumeNumber(data, true));

            int ops = data.consumeInt(0, 32);
            for (int i = 0; i < ops; i++) {
                Number x;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        x = pivotX;
                        break;
                    case 1:
                        x = smallerX;
                        break;
                    case 2:
                        x = largerX;
                        break;
                    case 3:
                        x = relatedNumber(pivotX, data.consumeBoolean() ? -1 : 1, data);
                        break;
                    default:
                        x = consumeNumber(data, false);
                        break;
                }
                Number y = consumeNumber(data, true);
                XYDataItem overwritten = series.addOrUpdate(x, y);

                if (overwritten != null && data.consumeBoolean()) {
                    Number ox = overwritten.getX();
                    Number oy = overwritten.getY();
                    series.addOrUpdate(ox, oy);
                }

                if (data.consumeBoolean()) {
                    series.setMaximumItemCount(data.consumeInt(0, 16));
                }
            }

            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                XYDataItem item = series.getDataItem(idx);
                series.addOrUpdate(item.getX(), consumeNumber(data, true));
            }
        }
    }

    private static Number consumeNumber(FuzzedDataProvider data, boolean allowNull) {
        if (allowNull && data.consumeBoolean()) {
            return null;
        }
        switch (data.consumeInt(0, 9)) {
            case 0:
                return Integer.valueOf(data.consumeInt());
            case 1:
                return Long.valueOf(data.consumeInt());
            case 2:
                return Double.valueOf((double) data.consumeInt());
            case 3:
                return Float.valueOf((float) data.consumeInt());
            case 4:
                return Short.valueOf((short) data.consumeInt());
            case 5:
                return Byte.valueOf(data.consumeByte());
            case 6:
                return Double.valueOf(Double.NaN);
            case 7:
                return Double.valueOf(Double.POSITIVE_INFINITY);
            case 8:
                return Double.valueOf(Double.NEGATIVE_INFINITY);
            default:
                return Double.valueOf(data.consumeBoolean() ? 0.0d : -0.0d);
        }
    }

    private static Number relatedNumber(Number base, int delta, FuzzedDataProvider data) {
        double b = base.doubleValue();
        switch (data.consumeInt(0, 5)) {
            case 0:
                return Integer.valueOf((int) b + delta);
            case 1:
                return Long.valueOf((long) b + delta);
            case 2:
                return Double.valueOf(b + delta);
            case 3:
                return Float.valueOf((float) b + delta);
            case 4:
                return Short.valueOf((short) (((int) b) + delta));
            default:
                return Byte.valueOf((byte) (((int) b) + delta));
        }
    }
}