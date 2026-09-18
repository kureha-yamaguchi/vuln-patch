package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key = data.consumeString(32);
        XYSeries series = new XYSeries(key, data.consumeBoolean(), data.consumeBoolean());

        series.setMaximumItemCount(data.consumeInt(0, 16));

        int ops = data.consumeInt(1, 24);
        Number[] seenX = new Number[ops + 4];
        int seenCount = 0;

        for (int i = 0; i < ops; i++) {
            if (data.remainingBytes() <= 0) {
                break;
            }

            if (data.consumeBoolean()) {
                series.setMaximumItemCount(data.consumeInt(0, 16));
            }

            Number x;
            if (seenCount > 0 && data.consumeBoolean()) {
                x = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                int xKind = data.consumeInt(0, 7);
                switch (xKind) {
                    case 0:
                        x = new Integer(data.consumeInt());
                        break;
                    case 1:
                        x = new Long((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        x = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 3:
                        x = new Float(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 4:
                        x = new Short((short) data.consumeInt());
                        break;
                    case 5:
                        x = new Byte(data.consumeByte());
                        break;
                    case 6:
                        x = new Double((double) data.consumeInt());
                        break;
                    default:
                        x = new Long((long) data.consumeInt());
                        break;
                }
                seenX[seenCount++] = x;
            }

            Number y;
            if (data.consumeBoolean()) {
                y = null;
            } else {
                int yKind = data.consumeInt(0, 7);
                switch (yKind) {
                    case 0:
                        y = new Integer(data.consumeInt());
                        break;
                    case 1:
                        y = new Long((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 2:
                        y = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                                ^ (data.consumeInt() & 0xffffffffL)));
                        break;
                    case 3:
                        y = new Float(Float.intBitsToFloat(data.consumeInt()));
                        break;
                    case 4:
                        y = new Short((short) data.consumeInt());
                        break;
                    case 5:
                        y = new Byte(data.consumeByte());
                        break;
                    case 6:
                        y = new Double((double) data.consumeInt());
                        break;
                    default:
                        y = new Long((long) data.consumeInt());
                        break;
                }
            }

            XYDataItem overwritten = series.addOrUpdate(x, y);

            if (overwritten != null && data.consumeBoolean()) {
                overwritten.getX();
                overwritten.getY();
            }

            if (series.getItemCount() > 0) {
                int idx = data.consumeInt(0, series.getItemCount() - 1);
                series.getDataItem(idx);
            }

            if (data.consumeBoolean()) {
                series.indexOf(x);
            }
        }

        if (data.consumeBoolean()) {
            Number finalX;
            if (data.consumeBoolean()) {
                finalX = null;
            } else if (seenCount > 0 && data.consumeBoolean()) {
                finalX = seenX[data.consumeInt(0, seenCount - 1)];
            } else {
                finalX = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                        ^ (data.consumeInt() & 0xffffffffL)));
            }

            Number finalY;
            if (data.consumeBoolean()) {
                finalY = null;
            } else {
                finalY = new Double(Double.longBitsToDouble((((long) data.consumeInt()) << 32)
                        ^ (data.consumeInt() & 0xffffffffL)));
            }

            series.addOrUpdate(finalX, finalY);
        }
    }
}