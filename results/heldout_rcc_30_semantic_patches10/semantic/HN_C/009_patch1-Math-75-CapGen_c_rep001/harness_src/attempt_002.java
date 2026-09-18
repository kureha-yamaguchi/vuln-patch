package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();

        int preQueries = data.consumeInt(0, 4);
        for (int i = 0; i < preQueries; i++) {
            Object query;
            switch (data.consumeInt(0, 11)) {
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
                    query = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    query = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    query = Short.valueOf((short) data.consumeInt());
                    break;
                case 7:
                    query = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 8:
                    query = data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                case 9:
                    query = data.consumeRemainingAsString();
                    break;
                case 10:
                    query = data.consumeBytes(data.consumeInt(0, 16));
                    break;
                default:
                    query = new Object();
                    break;
            }
            freq.getPct(query);
        }

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            switch (data.consumeInt(0, 9)) {
                case 0:
                    freq.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 1:
                    freq.addValue(Long.valueOf((long) data.consumeInt()));
                    break;
                case 2:
                    freq.addValue(Character.valueOf((char) (data.consumeByte() & 0xFF)));
                    break;
                case 3:
                    freq.addValue(Boolean.valueOf(data.consumeBoolean()));
                    break;
                case 4:
                    freq.addValue(Byte.valueOf(data.consumeByte()));
                    break;
                case 5:
                    freq.addValue(Short.valueOf((short) data.consumeInt()));
                    break;
                case 6:
                    freq.addValue(data.consumeString(data.consumeInt(0, 32)));
                    break;
                case 7:
                    freq.addValue(data.consumeAsciiString(data.consumeInt(0, 32)));
                    break;
                case 8:
                    freq.addValue(null);
                    break;
                default:
                    freq.getPct(data.consumeRemainingAsBytes());
                    break;
            }
        }

        int finalQueries = data.consumeInt(1, 16);
        for (int i = 0; i < finalQueries; i++) {
            Object query;
            switch (data.consumeInt(0, 13)) {
                case 0:
                    query = Integer.valueOf(Integer.MIN_VALUE);
                    break;
                case 1:
                    query = Integer.valueOf(Integer.MAX_VALUE);
                    break;
                case 2:
                    query = Integer.valueOf(0);
                    break;
                case 3:
                    query = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    query = Long.valueOf((long) data.consumeInt());
                    break;
                case 5:
                    query = Character.valueOf((char) (data.consumeByte() & 0xFF));
                    break;
                case 6:
                    query = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 7:
                    query = Byte.valueOf(data.consumeByte());
                    break;
                case 8:
                    query = Short.valueOf((short) data.consumeInt());
                    break;
                case 9:
                    query = data.consumeString(data.consumeInt(0, 64));
                    break;
                case 10:
                    query = data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 11:
                    query = data.remainingBytes() == 0 ? "" : data.consumeRemainingAsString();
                    break;
                case 12:
                    query = data.consumeBytes(data.consumeInt(0, Math.max(0, data.remainingBytes())));
                    break;
                default:
                    query = null;
                    break;
            }
            freq.getPct(query);
        }
    }
}