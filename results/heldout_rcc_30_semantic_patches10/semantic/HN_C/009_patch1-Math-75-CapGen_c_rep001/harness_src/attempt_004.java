package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();
        Frequency empty = new Frequency();

        int family = data.consumeInt(0, 4);
        int count = data.consumeInt(0, 32);

        for (int i = 0; i < count; i++) {
            switch (family) {
                case 0:
                    freq.addValue(data.consumeString(data.consumeInt(0, 32)));
                    break;
                case 1:
                    freq.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 2:
                    freq.addValue(Long.valueOf((long) data.consumeInt()));
                    break;
                case 3:
                    freq.addValue(Character.valueOf((char) (data.consumeByte() & 0xff)));
                    break;
                case 4:
                default:
                    freq.addValue(Byte.valueOf(data.consumeByte()));
                    break;
            }
        }

        Object query;
        switch (data.consumeInt(0, 7)) {
            case 0:
                query = null;
                break;
            case 1:
                query = data.consumeString(data.consumeInt(0, 32));
                break;
            case 2:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 3:
                query = Long.valueOf((long) data.consumeInt());
                break;
            case 4:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 5:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 6:
                query = new Object();
                break;
            case 7:
            default:
                query = data.consumeRemainingAsString();
                break;
        }

        empty.getPct(query);

        if (data.consumeBoolean()) {
            freq.getPct(query);
        }

        switch (family) {
            case 0:
                freq.getPct(data.consumeAsciiString(data.consumeInt(0, 32)));
                freq.getPct("");
                break;
            case 1:
                freq.getPct(Integer.valueOf(0));
                freq.getPct(Integer.valueOf(-1));
                freq.getPct(Integer.valueOf(1));
                break;
            case 2:
                freq.getPct(Long.valueOf(0L));
                freq.getPct(Long.valueOf(-1L));
                freq.getPct(Long.valueOf(1L));
                break;
            case 3:
                freq.getPct(Character.valueOf('\u0000'));
                freq.getPct(Character.valueOf('\uffff'));
                break;
            case 4:
            default:
                freq.getPct(Byte.valueOf((byte) 0));
                freq.getPct(Byte.valueOf((byte) -1));
                freq.getPct(Byte.valueOf((byte) 1));
                break;
        }

        if (data.consumeBoolean()) {
            freq.clear();
            freq.getPct(query);
        }
    }
}