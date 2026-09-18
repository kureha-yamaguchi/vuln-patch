package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int mode = data.consumeInt(0, 5);
        int count = data.consumeInt(0, 32);

        for (int i = 0; i < count; i++) {
            switch (mode) {
                case 0:
                    frequency.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 1:
                    frequency.addValue(Long.valueOf((long) data.consumeInt()));
                    break;
                case 2:
                    frequency.addValue(Character.valueOf((char) (data.consumeByte() & 0xFF)));
                    break;
                case 3:
                    frequency.addValue(data.consumeString(data.consumeInt(0, 32)));
                    break;
                case 4:
                    frequency.addValue(data.consumeAsciiString(data.consumeInt(0, 32)));
                    break;
                case 5:
                    frequency.addValue(Boolean.valueOf(data.consumeBoolean()));
                    break;
                default:
                    break;
            }
        }

        int queries = data.consumeInt(1, 16);
        for (int i = 0; i < queries; i++) {
            Object query;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    query = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    query = Long.valueOf((long) data.consumeInt());
                    break;
                case 2:
                    query = Character.valueOf((char) (data.consumeByte() & 0xFF));
                    break;
                case 3:
                    query = data.consumeString(data.consumeInt(0, 64));
                    break;
                case 4:
                    query = data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 5:
                    query = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    query = null;
                    break;
                case 7:
                    query = data.consumeRemainingAsString();
                    break;
                case 8:
                    query = data.consumeBytes(data.consumeInt(0, 32));
                    break;
                case 9:
                    query = data.consumeRemainingAsBytes();
                    break;
                default:
                    query = Integer.valueOf(0);
                    break;
            }
            frequency.getPct(query);
        }
    }
}