package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();

        int mode = data.consumeInt(0, 5);
        int count = data.consumeInt(0, 32);
        Object remembered = null;

        for (int i = 0; i < count; i++) {
            Comparable value;
            switch (mode) {
                case 0:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    value = Long.valueOf((((long) data.consumeInt()) << 32) ^ (long) data.consumeInt());
                    break;
                case 2:
                    value = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    value = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 4:
                    switch (data.consumeInt(0, 6)) {
                        case 0:
                            value = Integer.valueOf(data.consumeInt());
                            break;
                        case 1:
                            value = Long.valueOf((((long) data.consumeInt()) << 32) ^ (long) data.consumeInt());
                            break;
                        case 2:
                            value = Character.valueOf((char) (data.consumeByte() & 0xff));
                            break;
                        case 3:
                            value = data.consumeAsciiString(data.consumeInt(0, 32));
                            break;
                        case 4:
                            value = Short.valueOf((short) data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
                            break;
                        case 5:
                            value = Byte.valueOf(data.consumeByte());
                            break;
                        default:
                            value = Boolean.valueOf(data.consumeBoolean());
                            break;
                    }
                    break;
                default:
                    value = data.consumeAsciiString(data.consumeInt(0, 4));
                    break;
            }

            remembered = value;
            freq.addValue(value);
        }

        Object query;
        switch (data.consumeInt(0, 11)) {
            case 0:
                query = remembered;
                break;
            case 1:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                query = Long.valueOf((((long) data.consumeInt()) << 32) ^ (long) data.consumeInt());
                break;
            case 3:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 4:
                query = data.consumeString(data.consumeInt(0, 64));
                break;
            case 5:
                query = data.consumeAsciiString(data.consumeInt(0, 64));
                break;
            case 6:
                query = Short.valueOf((short) data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
                break;
            case 7:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 8:
                query = Boolean.valueOf(data.consumeBoolean());
                break;
            case 9:
                query = null;
                break;
            case 10:
                query = new Object();
                break;
            default:
                query = data.consumeRemainingAsBytes();
                break;
        }

        freq.getPct(query);
    }
}