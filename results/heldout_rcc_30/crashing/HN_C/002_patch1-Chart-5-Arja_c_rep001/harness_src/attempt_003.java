package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);

        for (int s = 0; s < seriesCount; s++) {
            String key = data.consumeAsciiString(32);
            boolean autoSort = data.consumeBoolean();
            boolean allowDuplicateXValues = data.consumeBoolean();

            XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
            series.setMaximumItemCount(data.consumeInt(0, 32));

            int operations = data.consumeInt(1, 24);
            for (int i = 0; i < operations; i++) {
                Number x;
                switch (data.consumeInt(0, 13)) {
                    case 0:
                        x = Integer.valueOf(0);
                        break;
                    case 1:
                        x = Integer.valueOf(1);
                        break;
                    case 2:
                        x = Integer.valueOf(-1);
                        break;
                    case 3:
                        x = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 4:
                        x = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 5:
                        x = Long.valueOf(data.consumeInt());
                        break;
                    case 6:
                        x = Double.valueOf(0.0d);
                        break;
                    case 7:
                        x = Double.valueOf(-0.0d);
                        break;
                    case 8:
                        x = Double.valueOf(Double.NaN);
                        break;
                    case 9:
                        x = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    case 10:
                        x = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                    case 11:
                        x = Double.valueOf((double) data.consumeInt());
                        break;
                    case 12:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    default:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                }

                Number y;
                switch (data.consumeInt(0, 14)) {
                    case 0:
                        y = null;
                        break;
                    case 1:
                        y = Integer.valueOf(0);
                        break;
                    case 2:
                        y = Integer.valueOf(1);
                        break;
                    case 3:
                        y = Integer.valueOf(-1);
                        break;
                    case 4:
                        y = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 5:
                        y = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 6:
                        y = Long.valueOf(data.consumeInt());
                        break;
                    case 7:
                        y = Double.valueOf(0.0d);
                        break;
                    case 8:
                        y = Double.valueOf(-0.0d);
                        break;
                    case 9:
                        y = Double.valueOf(Double.NaN);
                        break;
                    case 10:
                        y = Double.valueOf(Double.POSITIVE_INFINITY);
                        break;
                    case 11:
                        y = Double.valueOf(Double.NEGATIVE_INFINITY);
                        break;
                    case 12:
                        y = Double.valueOf((double) data.consumeInt());
                        break;
                    case 13:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    default:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                }

                if (i > 0 && data.consumeBoolean() && series.getItemCount() > 0) {
                    int existingIndex = data.consumeInt(0, series.getItemCount() - 1);
                    XYDataItem existing = series.getDataItem(existingIndex);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            x = existing.getX();
                            break;
                        case 1:
                            x = Integer.valueOf(existing.getX().intValue());
                            break;
                        case 2:
                            x = Double.valueOf(existing.getX().doubleValue());
                            break;
                        default:
                            x = Long.valueOf(existing.getX().longValue());
                            break;
                    }
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);

                if (overwritten != null && data.consumeBoolean()) {
                    Number ox = overwritten.getX();
                    Number oy = overwritten.getY();
                    if (data.consumeBoolean()) {
                        series.addOrUpdate(ox, oy);
                    }
                }

                if (data.consumeBoolean()) {
                    int count = series.getItemCount();
                    if (count > 0) {
                        XYDataItem item = series.getDataItem(data.consumeInt(0, count - 1));
                        Number rx = item.getX();
                        series.addOrUpdate(rx, y);
                    }
                }

                if (data.consumeBoolean()) {
                    series.setMaximumItemCount(data.consumeInt(0, 32));
                }
            }
        }
    }
}