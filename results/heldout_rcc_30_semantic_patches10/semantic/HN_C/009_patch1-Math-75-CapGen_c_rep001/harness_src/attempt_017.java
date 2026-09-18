package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int count = data.consumeInt(0, 32);
        int mode = data.consumeInt(0, 5);

        for (int i = 0; i < count; i++) {
            switch (mode) {
                case 0:
                    frequency.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 1:
                    frequency.addValue(Long.valueOf(data.consumeInt()));
                    break;
                case 2:
                    frequency.addValue(Character.valueOf((char) (data.consumeByte() & 0xff)));
                    break;
                case 3:
                    frequency.addValue(data.consumeString(data.consumeInt(0, 32)));
                    break;
                case 4:
                    frequency.addValue(data.consumeAsciiString(data.consumeInt(0, 32)));
                    break;
                default:
                    if (data.consumeBoolean()) {
                        frequency.addValue(Integer.valueOf(data.consumeInt()));
                    } else {
                        frequency.addValue(Long.valueOf(data.consumeInt()));
                    }
                    break;
            }
        }

        Object query;
        switch (data.consumeInt(0, 8)) {
            case 0:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 1:
                query = Long.valueOf(data.consumeInt());
                break;
            case 2:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 3:
                query = data.consumeString(data.consumeInt(0, 64));
                break;
            case 4:
                query = data.consumeAsciiString(data.consumeInt(0, 64));
                break;
            case 5:
                query = null;
                break;
            case 6:
                query = data.consumeBytes(data.consumeInt(0, 64));
                break;
            case 7:
                query = data.consumeRemainingAsBytes();
                break;
            default:
                query = data.consumeRemainingAsString();
                break;
        }

        frequency.getPct(query);
    }
}