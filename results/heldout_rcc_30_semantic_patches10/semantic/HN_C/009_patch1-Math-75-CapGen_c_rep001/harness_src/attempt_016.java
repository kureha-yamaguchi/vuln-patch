package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            int kind = data.consumeInt(0, 9);
            Comparable<?> comparableValue;
            Object objectValue;

            switch (kind) {
                case 0:
                    comparableValue = null;
                    objectValue = null;
                    break;
                case 1: {
                    Integer v = Integer.valueOf(data.consumeInt());
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 2: {
                    Long v = Long.valueOf((long) data.consumeInt());
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 3: {
                    Character v = Character.valueOf((char) (data.consumeByte() & 0xff));
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 4: {
                    String v = data.consumeString(data.consumeInt(0, 32));
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 5: {
                    String v = data.consumeAsciiString(data.consumeInt(0, 32));
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 6: {
                    Short v = Short.valueOf((short) data.consumeInt());
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 7: {
                    Byte v = Byte.valueOf(data.consumeByte());
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                case 8: {
                    Float v = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
                default: {
                    Double v = Double.valueOf(Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL)));
                    comparableValue = v;
                    objectValue = v;
                    break;
                }
            }

            switch (data.consumeInt(0, 8)) {
                case 0:
                    frequency.addValue(comparableValue);
                    break;
                case 1:
                    if (objectValue instanceof Integer) {
                        frequency.addValue(((Integer) objectValue).intValue());
                    } else if (objectValue instanceof Long) {
                        frequency.addValue(((Long) objectValue).longValue());
                    } else if (objectValue instanceof Character) {
                        frequency.addValue(((Character) objectValue).charValue());
                    } else {
                        frequency.addValue(comparableValue);
                    }
                    break;
                case 2:
                    if (objectValue instanceof Integer) {
                        frequency.getCount(((Integer) objectValue).intValue());
                    } else if (objectValue instanceof Long) {
                        frequency.getCount(((Long) objectValue).longValue());
                    } else if (objectValue instanceof Character) {
                        frequency.getCount(((Character) objectValue).charValue());
                    } else {
                        frequency.getCount(comparableValue);
                    }
                    break;
                case 3:
                    frequency.getPct(objectValue);
                    break;
                case 4:
                    frequency.getCumFreq(comparableValue);
                    break;
                case 5:
                    frequency.getCumPct(comparableValue);
                    break;
                case 6:
                    frequency.getSumFreq();
                    break;
                case 7:
                    frequency.valuesIterator();
                    break;
                default:
                    frequency.toString();
                    break;
            }
        }

        Object finalValue;
        switch (data.consumeInt(0, 11)) {
            case 0:
                finalValue = null;
                break;
            case 1:
                finalValue = Integer.valueOf(0);
                break;
            case 2:
                finalValue = Integer.valueOf(-1);
                break;
            case 3:
                finalValue = Integer.valueOf(1);
                break;
            case 4:
                finalValue = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                finalValue = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                finalValue = "";
                break;
            case 7:
                finalValue = data.consumeString(data.consumeInt(0, 64));
                break;
            case 8:
                finalValue = data.consumeAsciiString(data.consumeInt(0, 64));
                break;
            case 9:
                finalValue = Short.valueOf((short) data.consumeInt());
                break;
            case 10:
                finalValue = Byte.valueOf(data.consumeByte());
                break;
            default:
                finalValue = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                break;
        }

        frequency.getPct(finalValue);
    }
}