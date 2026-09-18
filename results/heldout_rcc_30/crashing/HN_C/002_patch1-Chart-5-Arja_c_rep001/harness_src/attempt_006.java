package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seriesCount = data.consumeInt(1, 4);
        for (int i = 0; i < seriesCount; i++) {
            String key = data.consumeAsciiString(32);
            XYSeries series = new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());
            series.setMaximumItemCount(data.consumeInt(0, 8));

            int setupOps = data.consumeInt(0, 12);
            for (int j = 0; j < setupOps; j++) {
                Number x;
                switch (data.consumeInt(0, 5)) {
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
                    default:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
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
                        default:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                    }
                }

                series.addOrUpdate(x, y);
            }

            int targetCalls = data.consumeInt(1, 6);
            for (int j = 0; j < targetCalls; j++) {
                Number x;
                if (series.getItemCount() > 0 && data.consumeBoolean()) {
                    x = series.getDataItem(data.consumeInt(0, series.getItemCount() - 1)).getX();
                } else if (data.consumeBoolean()) {
                    x = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
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
                        default:
                            x = Byte.valueOf(data.consumeByte());
                            break;
                    }
                }

                Number y;
                if (data.consumeBoolean()) {
                    y = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
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
                        default:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                    }
                }

                series.addOrUpdate(x, y);
            }
        }
    }
}