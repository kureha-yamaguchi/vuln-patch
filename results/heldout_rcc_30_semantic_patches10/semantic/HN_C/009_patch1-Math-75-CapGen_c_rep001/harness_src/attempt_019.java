package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int count = data.consumeInt(0, 32);
        for (int i = 0; i < count; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    frequency.addValue(data.consumeInt());
                    break;
                case 1:
                    frequency.addValue((long) data.consumeInt());
                    break;
                case 2:
                    frequency.addValue((char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    frequency.addValue(data.consumeString(32));
                    break;
                case 4:
                    frequency.addValue(data.consumeAsciiString(32));
                    break;
                case 5:
                    frequency.addValue(Long.valueOf(data.consumeInt()));
                    break;
                case 6:
                    frequency.addValue(Character.valueOf((char) (data.consumeByte() & 0xff)));
                    break;
                default:
                    frequency.addValue(Integer.valueOf(data.consumeInt()));
                    break;
            }
        }

        Object query;
        switch (data.consumeInt(0, 10)) {
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
                query = data.consumeString(64);
                break;
            case 4:
                query = data.consumeAsciiString(64);
                break;
            case 5:
                query = null;
                break;
            case 6:
                query = new Object();
                break;
            case 7:
                query = data.consumeRemainingAsBytes();
                break;
            case 8:
                query = Boolean.valueOf(data.consumeBoolean());
                break;
            case 9:
                query = Double.valueOf(data.consumeInt());
                break;
            default:
                query = data.consumeRemainingAsString();
                break;
        }

        frequency.getPct(query);
    }
}