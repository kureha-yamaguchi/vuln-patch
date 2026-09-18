package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeAsciiString(32);
        if (key.length() == 0) {
            key = "k";
        }

        boolean autoSort = data.consumeBoolean();
        boolean allowDuplicateXValues = data.consumeBoolean();
        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        series.setMaximumItemCount(data.consumeInt(0, 16));

        int initialAdds = data.consumeInt(0, 24);
        for (int i = 0; i < initialAdds; i++) {
            Number x;
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                int xKind = data.consumeInt(0, 7);
                switch (xKind) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt(-8, 8));
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt(-8, 8));
                        break;
                    case 2:
                        x = Short.valueOf((short) data.consumeInt(-8, 8));
                        break;
                    case 3:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        x = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    default:
                        x = Double.valueOf((double) data.consumeInt(-8, 8));
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 7);
                switch (yKind) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        y = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 3:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        y = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        y = Integer.valueOf(data.consumeInt(-16, 16));
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-16, 16));
                        break;
                }
            }

            series.addOrUpdate(x, y);
        }

        int calls = data.consumeInt(1, 8);
        for (int i = 0; i < calls; i++) {
            Number x;
            if (series.getItemCount() > 0 && data.consumeBoolean()) {
                x = series.getX(data.consumeInt(0, series.getItemCount() - 1));
            } else {
                int xKind = data.consumeInt(0, 9);
                switch (xKind) {
                    case 0:
                        x = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    case 1:
                        x = Long.valueOf((long) data.consumeInt(-1, 1));
                        break;
                    case 2:
                        x = Short.valueOf((short) data.consumeInt(-1, 1));
                        break;
                    case 3:
                        x = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        x = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        x = Integer.valueOf(data.consumeInt());
                        break;
                    case 7:
                        x = Double.valueOf((double) data.consumeInt(-4, 4));
                        break;
                    case 8:
                        x = Long.valueOf((long) data.consumeInt());
                        break;
                    default:
                        x = Integer.valueOf(series.getItemCount());
                        break;
                }
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 7);
                switch (yKind) {
                    case 0:
                        y = Integer.valueOf(data.consumeInt(-1, 1));
                        break;
                    case 1:
                        y = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        y = Short.valueOf((short) data.consumeInt());
                        break;
                    case 3:
                        y = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 5:
                        y = Double.valueOf(
                                Double.longBitsToDouble(
                                        (((long) data.consumeInt()) << 32)
                                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 6:
                        y = Integer.valueOf(data.consumeInt());
                        break;
                    default:
                        y = Double.valueOf((double) data.consumeInt(-4, 4));
                        break;
                }
            }

            series.addOrUpdate(x, y);
        }
    }
}