package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int count = data.consumeInt(0, 32);
        for (int i = 0; i < count; i++) {
            int kind = data.consumeInt(0, 7);
            switch (kind) {
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
                    frequency.addValue(data.consumeString(32));
                    break;
                case 4:
                    frequency.addValue(data.consumeAsciiString(32));
                    break;
                case 5:
                    frequency.addValue(Short.valueOf((short) data.consumeInt()));
                    break;
                case 6:
                    frequency.addValue(Byte.valueOf(data.consumeByte()));
                    break;
                default:
                    frequency.addValue(Boolean.valueOf(data.consumeBoolean()));
                    break;
            }
        }

        Object query;
        int queryKind = data.consumeInt(0, 11);
        switch (queryKind) {
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
                query = Short.valueOf((short) data.consumeInt());
                break;
            case 6:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 7:
                query = Boolean.valueOf(data.consumeBoolean());
                break;
            case 8:
                query = null;
                break;
            case 9:
                query = new Object();
                break;
            case 10:
                query = data.consumeRemainingAsString();
                break;
            default:
                query = data.consumeRemainingAsBytes();
                break;
        }

        frequency.getPct(query);
    }
}