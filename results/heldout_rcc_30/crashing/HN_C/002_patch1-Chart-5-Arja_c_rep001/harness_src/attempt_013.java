package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 32));

        Number[] seenX = new Number[32];
        int seenCount = 0;

        int operations = data.consumeInt(1, 40);
        for (int i = 0; i < operations; i++) {
            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 32));
            }

            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        x = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    case 2:
                        x = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 3:
                        x = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        x = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 7:
                        x = Double.valueOf((double) data.consumeInt(-3, 3));
                        break;
                    default:
                        x = Long.valueOf(data.consumeInt(-3, 3));
                        break;
                }
                if (seenCount < seenX.length) {
                    seenX[seenCount++] = x;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    case 2:
                        y = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 3:
                        y = Double.valueOf(Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 6:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-3, 3));
                        break;
                }
            }

            series.addOrUpdate(x, y);

            if (!allowDuplicateXValues && data.consumeBoolean()) {
                Number y2;
                if (data.consumeBoolean()) {
                    y2 = null;
                } else {
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            y2 = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            y2 = Double.valueOf(Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                            break;
                        case 2:
                            y2 = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 3:
                            y2 = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                            break;
                        case 4:
                            y2 = Byte.valueOf(data.consumeByte());
                            break;
                        default:
                            y2 = Integer.valueOf(data.consumeInt(-3, 3));
                            break;
                    }
                }
                series.addOrUpdate(x, y2);
            }
        }
    }
}