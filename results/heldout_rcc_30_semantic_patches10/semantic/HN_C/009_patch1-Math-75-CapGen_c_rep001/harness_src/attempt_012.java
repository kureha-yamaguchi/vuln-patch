package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int populationKind = data.consumeInt(0, 4);
        int count = data.consumeInt(0, 32);

        Comparable sameTypeProbe;

        switch (populationKind) {
            case 0:
                sameTypeProbe = data.consumeAsciiString(32);
                for (int i = 0; i < count; i++) {
                    frequency.addValue((Comparable) data.consumeAsciiString(32));
                }
                break;
            case 1:
                sameTypeProbe = Integer.valueOf(data.consumeInt());
                for (int i = 0; i < count; i++) {
                    frequency.addValue((Comparable) Integer.valueOf(data.consumeInt()));
                }
                break;
            case 2:
                sameTypeProbe = Long.valueOf(data.consumeInt());
                for (int i = 0; i < count; i++) {
                    frequency.addValue((Comparable) Long.valueOf(data.consumeInt()));
                }
                break;
            case 3:
                sameTypeProbe = Character.valueOf((char) (data.consumeByte() & 0xff));
                for (int i = 0; i < count; i++) {
                    frequency.addValue((Comparable) Character.valueOf((char) (data.consumeByte() & 0xff)));
                }
                break;
            default:
                sameTypeProbe = Byte.valueOf(data.consumeByte());
                for (int i = 0; i < count; i++) {
                    frequency.addValue((Comparable) Byte.valueOf(data.consumeByte()));
                }
                break;
        }

        if (data.consumeBoolean()) {
            Frequency empty = new Frequency();
            Object emptyQuery;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    emptyQuery = sameTypeProbe;
                    break;
                case 1:
                    emptyQuery = null;
                    break;
                case 2:
                    emptyQuery = data.consumeAsciiString(16);
                    break;
                default:
                    emptyQuery = Integer.valueOf(data.consumeInt());
                    break;
            }
            empty.getPct(emptyQuery);
        }

        int warmups = data.consumeInt(0, 3);
        for (int i = 0; i < warmups; i++) {
            Object q;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    q = sameTypeProbe;
                    break;
                case 1:
                    q = null;
                    break;
                case 2:
                    q = data.consumeAsciiString(32);
                    break;
                case 3:
                    q = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    q = Long.valueOf(data.consumeInt());
                    break;
                default:
                    q = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
            }
            frequency.getPct(q);
        }

        Object finalQuery;
        switch (data.consumeInt(0, 7)) {
            case 0:
                finalQuery = sameTypeProbe;
                break;
            case 1:
                finalQuery = null;
                break;
            case 2:
                finalQuery = data.consumeAsciiString(64);
                break;
            case 3:
                finalQuery = data.consumeString(64);
                break;
            case 4:
                finalQuery = Integer.valueOf(data.consumeInt());
                break;
            case 5:
                finalQuery = Long.valueOf(data.consumeInt());
                break;
            case 6:
                finalQuery = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            default:
                finalQuery = new Object();
                break;
        }

        frequency.getPct(finalQuery);
    }
}