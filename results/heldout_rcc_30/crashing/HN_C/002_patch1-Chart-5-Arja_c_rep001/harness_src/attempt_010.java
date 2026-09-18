package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 8));

        int operations = data.consumeInt(0, 20);
        for (int i = 0; i < operations; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 8));
            }

            Number x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = Integer.valueOf(data.consumeInt(-3, 3));
                    break;
                case 1:
                    x = Long.valueOf(data.consumeInt(-3, 3));
                    break;
                case 2:
                    x = Double.valueOf((double) data.consumeInt(-3, 3));
                    break;
                case 3:
                    x = Float.valueOf((float) data.consumeInt(-3, 3));
                    break;
                case 4:
                    x = Double.valueOf(Double.NaN);
                    break;
                case 5:
                    x = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                case 6:
                    x = Short.valueOf((short) data.consumeInt(-3, 3));
                    break;
                default:
                    x = new java.math.BigDecimal(Integer.toString(data.consumeInt(-3, 3)));
                    break;
            }

            Number y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = null;
                    break;
                case 1:
                    y = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    y = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    y = Double.valueOf((double) data.consumeInt());
                    break;
                case 4:
                    y = Float.valueOf((float) data.consumeInt());
                    break;
                case 5:
                    y = Double.valueOf(Double.NaN);
                    break;
                case 6:
                    y = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                default:
                    y = new java.math.BigDecimal(Integer.toString(data.consumeInt()));
                    break;
            }

            series.addOrUpdate(x, y);

            if (data.consumeBoolean()) {
                Number y2;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        y2 = null;
                        break;
                    case 1:
                        y2 = Integer.valueOf(data.consumeInt(-10, 10));
                        break;
                    case 2:
                        y2 = Long.valueOf(data.consumeInt(-10, 10));
                        break;
                    case 3:
                        y2 = Double.valueOf((double) data.consumeInt(-10, 10));
                        break;
                    case 4:
                        y2 = Double.valueOf(Double.NaN);
                        break;
                    default:
                        y2 = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                        break;
                }
                series.addOrUpdate(x, y2);
            }

            int count = series.getItemCount();
            if (count > 0) {
                int idx = data.consumeInt(0, count - 1);
                series.getDataItem(idx);
                series.getX(idx);
                series.getY(idx);
            }

            series.indexOf(x);
        }

        if (data.remainingBytes() > 0) {
            Number finalX;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    finalX = Integer.valueOf(data.consumeInt(-1, 1));
                    break;
                case 1:
                    finalX = Double.valueOf((double) data.consumeInt(-1, 1));
                    break;
                case 2:
                    finalX = Double.valueOf(Double.NaN);
                    break;
                case 3:
                    finalX = Double.valueOf(data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                default:
                    finalX = new java.math.BigDecimal(Integer.toString(data.consumeInt(-1, 1)));
                    break;
            }

            Number finalY = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            series.addOrUpdate(finalX, finalY);
        }
    }
}