package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = data.consumeBoolean()
                ? new Frequency()
                : new Frequency(String.CASE_INSENSITIVE_ORDER);

        int ops = data.consumeInt(0, 40);
        for (int i = 0; i < ops; i++) {
            Comparable value;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    value = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    value = Character.valueOf((char) (data.consumeByte() & 0xFF));
                    break;
                case 3:
                    value = Byte.valueOf(data.consumeByte());
                    break;
                case 4:
                    value = Short.valueOf((short) data.consumeInt());
                    break;
                case 5:
                    value = data.consumeString(32);
                    break;
                case 6:
                    value = data.consumeAsciiString(32);
                    break;
                default:
                    value = null;
                    break;
            }

            int action = data.consumeInt(0, 5);
            if (action <= 1) {
                int repeats = data.consumeInt(0, 4);
                for (int r = 0; r < repeats; r++) {
                    freq.addValue(value);
                }
            } else if (action == 2) {
                freq.getCount(value);
            } else if (action == 3) {
                freq.getCumFreq(value);
            } else if (action == 4) {
                freq.getCumPct(value);
            } else {
                freq.getPct(value);
            }
        }

        Object query;
        switch (data.consumeInt(0, 9)) {
            case 0:
                query = null;
                break;
            case 1:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                query = Long.valueOf((long) data.consumeInt());
                break;
            case 3:
                query = Character.valueOf((char) (data.consumeByte() & 0xFF));
                break;
            case 4:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 5:
                query = Short.valueOf((short) data.consumeInt());
                break;
            case 6:
                query = data.consumeString(64);
                break;
            case 7:
                query = data.consumeAsciiString(64);
                break;
            case 8:
                query = data.consumeRemainingAsBytes();
                break;
            default:
                query = new Object();
                break;
        }

        freq.getPct(query);
    }
}