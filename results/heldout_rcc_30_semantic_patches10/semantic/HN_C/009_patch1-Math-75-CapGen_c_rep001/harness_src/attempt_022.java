package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int populationMode = data.consumeInt(0, 6);
        int count = data.consumeInt(0, 32);

        for (int i = 0; i < count; i++) {
            switch (populationMode) {
                case 0:
                    frequency.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 1:
                    frequency.addValue(Long.valueOf((long) data.consumeInt()));
                    break;
                case 2:
                    frequency.addValue(Short.valueOf((short) data.consumeInt()));
                    break;
                case 3:
                    frequency.addValue(Byte.valueOf(data.consumeByte()));
                    break;
                case 4:
                    frequency.addValue(Character.valueOf((char) data.consumeInt(0, 65535)));
                    break;
                case 5:
                    frequency.addValue(data.consumeString(data.consumeInt(0, 16)));
                    break;
                case 6:
                    frequency.addValue(data.consumeAsciiString(data.consumeInt(0, 16)));
                    break;
                default:
                    break;
            }
        }

        int extraOps = data.consumeInt(0, 8);
        for (int i = 0; i < extraOps; i++) {
            int op = data.consumeInt(0, 5);
            switch (op) {
                case 0:
                    frequency.getSumFreq();
                    break;
                case 1:
                    switch (populationMode) {
                        case 0:
                            frequency.getCount(Integer.valueOf(data.consumeInt()));
                            break;
                        case 1:
                            frequency.getCount(Long.valueOf((long) data.consumeInt()));
                            break;
                        case 2:
                            frequency.getCount(Short.valueOf((short) data.consumeInt()));
                            break;
                        case 3:
                            frequency.getCount(Byte.valueOf(data.consumeByte()));
                            break;
                        case 4:
                            frequency.getCount(Character.valueOf((char) data.consumeInt(0, 65535)));
                            break;
                        case 5:
                            frequency.getCount(data.consumeString(data.consumeInt(0, 16)));
                            break;
                        case 6:
                            frequency.getCount(data.consumeAsciiString(data.consumeInt(0, 16)));
                            break;
                        default:
                            break;
                    }
                    break;
                case 2:
                    switch (populationMode) {
                        case 0:
                            frequency.getCumFreq(Integer.valueOf(data.consumeInt()));
                            break;
                        case 1:
                            frequency.getCumFreq(Long.valueOf((long) data.consumeInt()));
                            break;
                        case 2:
                            frequency.getCumFreq(Short.valueOf((short) data.consumeInt()));
                            break;
                        case 3:
                            frequency.getCumFreq(Byte.valueOf(data.consumeByte()));
                            break;
                        case 4:
                            frequency.getCumFreq(Character.valueOf((char) data.consumeInt(0, 65535)));
                            break;
                        case 5:
                            frequency.getCumFreq(data.consumeString(data.consumeInt(0, 16)));
                            break;
                        case 6:
                            frequency.getCumFreq(data.consumeAsciiString(data.consumeInt(0, 16)));
                            break;
                        default:
                            break;
                    }
                    break;
                case 3:
                    switch (populationMode) {
                        case 0:
                            frequency.getPct(Integer.valueOf(data.consumeInt()));
                            break;
                        case 1:
                            frequency.getPct(Long.valueOf((long) data.consumeInt()));
                            break;
                        case 2:
                            frequency.getPct(Short.valueOf((short) data.consumeInt()));
                            break;
                        case 3:
                            frequency.getPct(Byte.valueOf(data.consumeByte()));
                            break;
                        case 4:
                            frequency.getPct(Character.valueOf((char) data.consumeInt(0, 65535)));
                            break;
                        case 5:
                            frequency.getPct(data.consumeString(data.consumeInt(0, 16)));
                            break;
                        case 6:
                            frequency.getPct(data.consumeAsciiString(data.consumeInt(0, 16)));
                            break;
                        default:
                            break;
                    }
                    break;
                case 4:
                    switch (populationMode) {
                        case 0:
                            frequency.getCumPct(Integer.valueOf(data.consumeInt()));
                            break;
                        case 1:
                            frequency.getCumPct(Long.valueOf((long) data.consumeInt()));
                            break;
                        case 2:
                            frequency.getCumPct(Short.valueOf((short) data.consumeInt()));
                            break;
                        case 3:
                            frequency.getCumPct(Byte.valueOf(data.consumeByte()));
                            break;
                        case 4:
                            frequency.getCumPct(Character.valueOf((char) data.consumeInt(0, 65535)));
                            break;
                        case 5:
                            frequency.getCumPct(data.consumeString(data.consumeInt(0, 16)));
                            break;
                        case 6:
                            frequency.getCumPct(data.consumeAsciiString(data.consumeInt(0, 16)));
                            break;
                        default:
                            break;
                    }
                    break;
                case 5:
                    frequency.toString();
                    break;
                default:
                    break;
            }
        }

        int queryMode = data.consumeInt(0, 10);
        Object query;
        switch (queryMode) {
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
                query = Short.valueOf((short) data.consumeInt());
                break;
            case 4:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 5:
                query = Character.valueOf((char) data.consumeInt(0, 65535));
                break;
            case 6:
                query = data.consumeString(data.consumeInt(0, 32));
                break;
            case 7:
                query = data.consumeAsciiString(data.consumeInt(0, 32));
                break;
            case 8:
                query = Boolean.valueOf(data.consumeBoolean());
                break;
            case 9:
                query = new String(data.consumeBytes(data.consumeInt(0, 32)));
                break;
            case 10:
                query = data.consumeRemainingAsString();
                break;
            default:
                query = Integer.valueOf(0);
                break;
        }

        frequency.getPct(query);
    }
}