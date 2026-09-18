package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();

        int family = data.consumeInt(0, 4);
        int count = data.consumeInt(0, 24);

        for (int i = 0; i < count; i++) {
            Comparable<?> value;
            int chosenFamily = data.consumeBoolean() ? family : data.consumeInt(0, 4);
            switch (chosenFamily) {
                case 0:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    value = Long.valueOf(data.consumeInt());
                    break;
                case 2:
                    value = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    value = data.consumeAsciiString(data.consumeInt(0, 16));
                    break;
                default:
                    value = data.consumeString(data.consumeInt(0, 16));
                    break;
            }

            int reps = data.consumeInt(1, 3);
            for (int j = 0; j < reps; j++) {
                freq.addValue(value);
            }
        }

        Object query;
        switch (data.consumeInt(0, 8)) {
            case 0:
                query = null;
                break;
            case 1:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                query = Long.valueOf(data.consumeInt());
                break;
            case 3:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 4:
                query = data.consumeAsciiString(data.consumeInt(0, 32));
                break;
            case 5:
                query = data.consumeString(data.consumeInt(0, 32));
                break;
            case 6:
                query = new Object();
                break;
            case 7:
                query = data.consumeBytes(data.consumeInt(0, 32));
                break;
            default:
                if (data.remainingBytes() > 0) {
                    query = data.consumeRemainingAsString();
                } else {
                    query = "";
                }
                break;
        }

        freq.getPct(query);
    }
}