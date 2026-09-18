package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();
        Comparable remembered = null;

        int initialQueries = data.consumeInt(0, 3);
        for (int i = 0; i < initialQueries; i++) {
            Object query;
            switch (data.consumeInt(0, 9)) {
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
                    query = Byte.valueOf(data.consumeByte());
                    break;
                case 4:
                    query = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 5:
                    query = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 6:
                    query = data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                case 7:
                    query = new Object();
                    break;
                case 8:
                    query = data.consumeBytes(data.consumeInt(0, 16));
                    break;
                default:
                    query = remembered;
                    break;
            }
            frequency.getPct(query);
        }

        int valuesToAdd = data.consumeInt(0, 32);
        for (int i = 0; i < valuesToAdd; i++) {
            Comparable value;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    value = null;
                    break;
                case 1:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    value = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    value = Byte.valueOf(data.consumeByte());
                    break;
                case 4:
                    value = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 5:
                    value = data.consumeString(data.consumeInt(0, 64));
                    break;
                case 6:
                    value = data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 7:
                    value = Short.valueOf((short) data.consumeInt());
                    break;
                default:
                    value = remembered;
                    break;
            }
            remembered = value;
            frequency.addValue(value);

            if (data.consumeBoolean()) {
                Object query;
                switch (data.consumeInt(0, 10)) {
                    case 0:
                        query = value;
                        break;
                    case 1:
                        query = remembered;
                        break;
                    case 2:
                        query = null;
                        break;
                    case 3:
                        query = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        query = Long.valueOf(data.consumeInt());
                        break;
                    case 5:
                        query = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        query = data.consumeString(data.consumeInt(0, 64));
                        break;
                    case 7:
                        query = data.consumeAsciiString(data.consumeInt(0, 64));
                        break;
                    case 8:
                        query = new Object();
                        break;
                    case 9:
                        query = data.consumeRemainingAsBytes();
                        break;
                    default:
                        query = data.consumeRemainingAsString();
                        break;
                }
                frequency.getPct(query);
            }
        }

        int finalQueries = data.consumeInt(1, 8);
        for (int i = 0; i < finalQueries; i++) {
            Object query;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    query = remembered;
                    break;
                case 1:
                    query = null;
                    break;
                case 2:
                    query = Integer.valueOf(data.consumeInt());
                    break;
                case 3:
                    query = Long.valueOf(data.consumeInt());
                    break;
                case 4:
                    query = Byte.valueOf(data.consumeByte());
                    break;
                case 5:
                    query = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 6:
                    query = data.consumeString(data.consumeInt(0, 128));
                    break;
                case 7:
                    query = data.consumeAsciiString(data.consumeInt(0, 128));
                    break;
                case 8:
                    query = Short.valueOf((short) data.consumeInt());
                    break;
                case 9:
                    query = new Object();
                    break;
                case 10:
                    query = data.consumeBytes(data.consumeInt(0, 32));
                    break;
                default:
                    query = data.remainingBytes() > 0
                            ? data.consumeRemainingAsString()
                            : remembered;
                    break;
            }
            frequency.getPct(query);
        }
    }
}