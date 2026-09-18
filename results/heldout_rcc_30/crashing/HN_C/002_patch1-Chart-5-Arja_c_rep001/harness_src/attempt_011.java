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
            series.setMaximumItemCount(data.consumeInt(0, 8));

            int operations = data.consumeInt(0, 24);
            for (int i = 0; i < operations; i++) {
                Number x;
                int xKind = data.consumeInt(0, 11);
                switch (xKind) {
                    case 0:
                        x = Integer.valueOf(0);
                        break;
                    case 1:
                        x = Integer.valueOf(-1);
                        break;
                    case 2:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        x = Long.valueOf(data.consumeInt());
                        break;
                    case 4:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 7: {
                        long bits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                        x = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    }
                    case 8:
                        x = Integer.valueOf(i - (operations / 2));
                        break;
                    case 9:
                        x = Integer.valueOf(series.getItemCount());
                        break;
                    case 10:
                        if (series.getItemCount() > 0 && data.consumeBoolean()) {
                            x = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1)).getX();
                        } else {
                            x = Integer.valueOf(data.consumeInt(-3, 3));
                        }
                        break;
                    default:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                }

                Number y;
                int yKind = data.consumeInt(0, 9);
                switch (yKind) {
                    case 0:
                        y = null;
                        break;
                    case 1:
                        y = Integer.valueOf(0);
                        break;
                    case 2:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        y = Long.valueOf(data.consumeInt());
                        break;
                    case 4:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 5:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 7: {
                        long bits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                        y = Double.valueOf(Double.longBitsToDouble(bits));
                        break;
                    }
                    case 8:
                        y = Integer.valueOf(i);
                        break;
                    default:
                        y = Integer.valueOf(data.consumeInt(-16, 16));
                        break;
                }

                XYDataItem overwritten = series.addOrUpdate(x, y);
                if (overwritten != null) {
                    overwritten.getX();
                    overwritten.getY();
                    overwritten.compareTo(new XYDataItem(x, y));
                }

                if (series.getItemCount() > 0 && data.consumeBoolean()) {
                    XYDataItem item = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1));
                    item.getX();
                    item.getY();
                    series.indexOf(item.getX());
                }
            }

            if (series.getItemCount() > 0) {
                int extraUpdates = data.consumeInt(0, 8);
                for (int i = 0; i < extraUpdates; i++) {
                    Number existingX = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1)).getX();
                    Number newY;
                    int mode = data.consumeInt(0, 4);
                    if (mode == 0) {
                        newY = null;
                    } else if (mode == 1) {
                        newY = Integer.valueOf(data.consumeInt());
                    } else if (mode == 2) {
                        newY = Double.valueOf((double) data.consumeInt());
                    } else if (mode == 3) {
                        newY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    } else {
                        newY = Long.valueOf(data.consumeInt());
                    }
                    series.addOrUpdate(existingX, newY);
                }
            }
        }
    }
}