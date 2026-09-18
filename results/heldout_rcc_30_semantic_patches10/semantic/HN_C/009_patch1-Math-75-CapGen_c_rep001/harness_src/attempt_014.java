package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            frequency = new Frequency();
        } else if (ctorChoice == 1) {
            frequency = new Frequency(String.CASE_INSENSITIVE_ORDER);
        } else {
            frequency = new Frequency(java.util.Collections.reverseOrder());
        }

        int family = data.consumeInt(0, 5);
        int entries = data.consumeInt(0, 32);

        for (int i = 0; i < entries; i++) {
            Comparable value;
            int variant = data.consumeInt(0, 7);

            if (family == 0) {
                switch (variant) {
                    case 0:
                        value = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        value = Integer.valueOf(data.consumeInt(-3, 3));
                        break;
                    case 2:
                        value = Long.valueOf((long) data.consumeInt());
                        break;
                    case 3:
                        value = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        value = Short.valueOf((short) data.consumeInt(-32768, 32767));
                        break;
                    case 5:
                        value = Integer.valueOf(0);
                        break;
                    case 6:
                        value = Integer.valueOf(-1);
                        break;
                    default:
                        value = Integer.valueOf(1);
                        break;
                }
            } else if (family == 1) {
                switch (variant) {
                    case 0:
                        value = data.consumeString(0);
                        break;
                    case 1:
                        value = data.consumeString(8);
                        break;
                    case 2:
                        value = data.consumeAsciiString(8);
                        break;
                    case 3:
                        value = data.consumeAsciiString(1);
                        break;
                    case 4:
                        value = "";
                        break;
                    case 5:
                        value = "A";
                        break;
                    case 6:
                        value = "a";
                        break;
                    default:
                        value = data.consumeRemainingAsString();
                        break;
                }
            } else if (family == 2) {
                switch (variant) {
                    case 0:
                        value = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 1:
                        value = Character.valueOf('\u0000');
                        break;
                    case 2:
                        value = Character.valueOf('\uffff');
                        break;
                    case 3:
                        value = Character.valueOf('A');
                        break;
                    case 4:
                        value = Character.valueOf('a');
                        break;
                    default:
                        value = Character.valueOf((char) data.consumeInt(0, 127));
                        break;
                }
            } else if (family == 3) {
                switch (variant) {
                    case 0:
                        value = Long.valueOf((long) data.consumeInt());
                        break;
                    case 1:
                        value = Long.valueOf(Long.MIN_VALUE);
                        break;
                    case 2:
                        value = Long.valueOf(Long.MAX_VALUE);
                        break;
                    case 3:
                        value = Long.valueOf(0L);
                        break;
                    case 4:
                        value = Long.valueOf(-1L);
                        break;
                    default:
                        value = Long.valueOf(data.consumeInt(-16, 16));
                        break;
                }
            } else if (family == 4) {
                switch (variant) {
                    case 0:
                        value = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        value = data.consumeAsciiString(8);
                        break;
                    case 2:
                        value = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 3:
                        value = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        value = Short.valueOf((short) data.consumeInt(-100, 100));
                        break;
                    default:
                        value = Byte.valueOf(data.consumeByte());
                        break;
                }
            } else {
                switch (variant) {
                    case 0:
                        value = Integer.valueOf(0);
                        break;
                    case 1:
                        value = Integer.valueOf(1);
                        break;
                    case 2:
                        value = Integer.valueOf(-1);
                        break;
                    case 3:
                        value = Integer.valueOf(Integer.MIN_VALUE);
                        break;
                    case 4:
                        value = Integer.valueOf(Integer.MAX_VALUE);
                        break;
                    case 5:
                        value = data.consumeAsciiString(0);
                        break;
                    case 6:
                        value = data.consumeAsciiString(1);
                        break;
                    default:
                        value = data.consumeString(4);
                        break;
                }
            }

            int repeats = data.consumeInt(0, 4);
            for (int j = 0; j < repeats; j++) {
                frequency.addValue(value);
            }
        }

        int queryVariant = data.consumeInt(0, 11);
        Object query;
        switch (queryVariant) {
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
                query = Short.valueOf((short) data.consumeInt(-32768, 32767));
                break;
            case 4:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 5:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                query = data.consumeString(16);
                break;
            case 7:
                query = data.consumeAsciiString(16);
                break;
            case 8:
                query = "";
                break;
            case 9:
                query = "A";
                break;
            case 10:
                query = "a";
                break;
            default:
                query = data.consumeRemainingAsString();
                break;
        }

        frequency.getPct(query);
    }
}