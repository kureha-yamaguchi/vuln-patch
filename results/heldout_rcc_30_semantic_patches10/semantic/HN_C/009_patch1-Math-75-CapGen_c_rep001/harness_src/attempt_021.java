package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();

        int kind = data.consumeInt(0, 3);
        int count = data.consumeInt(0, 32);

        Comparable[] seen = new Comparable[count + 4];
        int seenSize = 0;

        switch (kind) {
            case 0: {
                for (int i = 0; i < count; i++) {
                    String v;
                    if (data.consumeBoolean()) {
                        switch (data.consumeInt(0, 5)) {
                            case 0:
                                v = "";
                                break;
                            case 1:
                                v = "a";
                                break;
                            case 2:
                                v = "A";
                                break;
                            case 3:
                                v = "\u0000";
                                break;
                            case 4:
                                v = "\uffff";
                                break;
                            default:
                                v = data.consumeAsciiString(8);
                                break;
                        }
                    } else {
                        v = data.consumeString(8);
                    }
                    freq.addValue(v);
                    seen[seenSize++] = v;
                }
                seen[seenSize++] = data.consumeAsciiString(8);
                seen[seenSize++] = data.consumeString(8);
                break;
            }
            case 1: {
                for (int i = 0; i < count; i++) {
                    Integer v;
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            v = Integer.MIN_VALUE;
                            break;
                        case 1:
                            v = Integer.MAX_VALUE;
                            break;
                        case 2:
                            v = -1;
                            break;
                        case 3:
                            v = 0;
                            break;
                        case 4:
                            v = 1;
                            break;
                        default:
                            v = data.consumeInt();
                            break;
                    }
                    freq.addValue(v);
                    seen[seenSize++] = v;
                }
                seen[seenSize++] = Integer.valueOf(data.consumeInt());
                seen[seenSize++] = Integer.valueOf(0);
                break;
            }
            case 2: {
                for (int i = 0; i < count; i++) {
                    Character v;
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            v = Character.valueOf('\u0000');
                            break;
                        case 1:
                            v = Character.valueOf('\u0001');
                            break;
                        case 2:
                            v = Character.valueOf('A');
                            break;
                        case 3:
                            v = Character.valueOf('a');
                            break;
                        case 4:
                            v = Character.valueOf('\uffff');
                            break;
                        default:
                            v = Character.valueOf((char) (data.consumeByte() & 0xff));
                            break;
                    }
                    freq.addValue(v);
                    seen[seenSize++] = v;
                }
                seen[seenSize++] = Character.valueOf((char) (data.consumeByte() & 0xff));
                seen[seenSize++] = Character.valueOf('\u0000');
                break;
            }
            default: {
                for (int i = 0; i < count; i++) {
                    Byte v;
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            v = Byte.MIN_VALUE;
                            break;
                        case 1:
                            v = Byte.MAX_VALUE;
                            break;
                        case 2:
                            v = (byte) -1;
                            break;
                        case 3:
                            v = (byte) 0;
                            break;
                        case 4:
                            v = (byte) 1;
                            break;
                        default:
                            v = data.consumeByte();
                            break;
                    }
                    freq.addValue(v);
                    seen[seenSize++] = v;
                }
                seen[seenSize++] = Byte.valueOf(data.consumeByte());
                seen[seenSize++] = Byte.valueOf((byte) 0);
                break;
            }
        }

        long sum = freq.getSumFreq();

        for (int i = 0; i < seenSize; i++) {
            Comparable v = seen[i];
            double actual = freq.getPct(v);
            double expected = (sum == 0L) ? Double.NaN : ((double) freq.getCount(v)) / (double) sum;

            if (Double.isNaN(expected)) {
                if (!Double.isNaN(actual)) {
                    throw new IllegalStateException("Expected NaN for empty frequency");
                }
            } else if (Double.compare(actual, expected) != 0) {
                throw new IllegalStateException(
                    "getPct mismatch for value=" + v + " actual=" + actual + " expected=" + expected);
            }
        }

        if (data.consumeBoolean()) {
            double actual = freq.getPct((Object) null);
            double expected = (sum == 0L) ? Double.NaN : 0.0;
            if (Double.isNaN(expected)) {
                if (!Double.isNaN(actual)) {
                    throw new IllegalStateException("Expected NaN for null on empty frequency");
                }
            } else if (Double.compare(actual, expected) != 0) {
                throw new IllegalStateException(
                    "getPct mismatch for null actual=" + actual + " expected=" + expected);
            }
        }
    }
}