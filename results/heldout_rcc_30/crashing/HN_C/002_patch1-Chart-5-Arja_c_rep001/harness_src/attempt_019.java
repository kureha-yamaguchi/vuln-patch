package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(16);
        if (key1.length() == 0) {
            key1 = "S1";
        }

        XYSeries series = new XYSeries(
                key1,
                data.consumeBoolean(),
                data.consumeBoolean());

        series.setMaximumItemCount(data.consumeInt(0, 32));

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            Number x;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    x = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    x = Long.valueOf(data.consumeInt());
                    break;
                case 2:
                    x = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                    break;
                case 3:
                    x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    break;
                case 4:
                    x = Short.valueOf((short) data.consumeInt());
                    break;
                default:
                    x = Byte.valueOf(data.consumeByte());
                    break;
            }

            Number y;
            switch (data.consumeInt(0, 6)) {
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
                    y = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                    break;
                case 4:
                    y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    break;
                case 5:
                    y = Short.valueOf((short) data.consumeInt());
                    break;
                default:
                    y = Byte.valueOf(data.consumeByte());
                    break;
            }

            series.addOrUpdate(x, y);
        }

        String key2 = data.consumeAsciiString(16);
        if (key2.length() == 0) {
            key2 = "BUG";
        }

        XYSeries buggySeries = new XYSeries(key2, true, true);
        buggySeries.setMaximumItemCount(data.consumeInt(1, 16));

        Integer duplicateX = Integer.valueOf(data.consumeInt());

        Number firstY;
        switch (data.consumeInt(0, 5)) {
            case 0:
                firstY = null;
                break;
            case 1:
                firstY = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                firstY = Long.valueOf(data.consumeInt());
                break;
            case 3:
                firstY = Double.valueOf(Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                break;
            case 4:
                firstY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                break;
            default:
                firstY = Byte.valueOf(data.consumeByte());
                break;
        }

        Number secondY;
        switch (data.consumeInt(0, 5)) {
            case 0:
                secondY = null;
                break;
            case 1:
                secondY = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                secondY = Long.valueOf(data.consumeInt());
                break;
            case 3:
                secondY = Double.valueOf(Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
                break;
            case 4:
                secondY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                break;
            default:
                secondY = Byte.valueOf(data.consumeByte());
                break;
        }

        buggySeries.addOrUpdate(duplicateX, firstY);
        buggySeries.addOrUpdate(duplicateX, secondY);
    }
}