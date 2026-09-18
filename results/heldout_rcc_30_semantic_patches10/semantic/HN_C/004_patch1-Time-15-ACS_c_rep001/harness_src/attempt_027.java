package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int iterations = data.consumeInt(1, 12);
        for (int i = 0; i < iterations; i++) {
            int mode = data.consumeInt(0, 11);

            long val1;
            switch (mode) {
                case 0:
                    val1 = 0L;
                    break;
                case 1:
                    val1 = 1L;
                    break;
                case 2:
                    val1 = -1L;
                    break;
                case 3:
                    val1 = Long.MAX_VALUE;
                    break;
                case 4:
                    val1 = Long.MIN_VALUE;
                    break;
                case 5:
                    val1 = Integer.MAX_VALUE;
                    break;
                case 6:
                    val1 = Integer.MIN_VALUE;
                    break;
                case 7:
                    val1 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    break;
                case 8:
                    byte[] bytes = data.consumeBytes(8);
                    long fromBytes = 0L;
                    for (int j = 0; j < bytes.length; j++) {
                        fromBytes = (fromBytes << 8) | (bytes[j] & 0xffL);
                    }
                    if (bytes.length < 8 && data.consumeBoolean()) {
                        fromBytes = -fromBytes;
                    }
                    val1 = fromBytes;
                    break;
                case 9:
                    String s = data.consumeString(32);
                    long parsed = 0L;
                    boolean neg = false;
                    int start = 0;
                    if (s.length() > 0 && s.charAt(0) == '-') {
                        neg = true;
                        start = 1;
                    }
                    for (int j = start; j < s.length(); j++) {
                        char c = s.charAt(j);
                        if (c >= '0' && c <= '9') {
                            parsed = parsed * 10L + (c - '0');
                        }
                    }
                    val1 = neg ? -parsed : parsed;
                    break;
                case 10:
                    String ascii = data.consumeAsciiString(32);
                    long hash = 1125899906842597L;
                    for (int j = 0; j < ascii.length(); j++) {
                        hash = 31L * hash + ascii.charAt(j);
                    }
                    val1 = hash;
                    break;
                default:
                    val1 = data.consumeBoolean() ? (long) data.consumeInt() : (long) data.consumeByte();
                    break;
            }

            int val2Mode = data.consumeInt(0, 9);
            int val2;
            switch (val2Mode) {
                case 0:
                    val2 = -1;
                    break;
                case 1:
                    val2 = 0;
                    break;
                case 2:
                    val2 = 1;
                    break;
                case 3:
                    val2 = Integer.MAX_VALUE;
                    break;
                case 4:
                    val2 = Integer.MIN_VALUE;
                    break;
                case 5:
                    val2 = 2;
                    break;
                case 6:
                    val2 = -2;
                    break;
                case 7:
                    val2 = data.consumeByte();
                    break;
                case 8:
                    val2 = data.consumeInt(-1000, 1000);
                    break;
                default:
                    val2 = data.consumeInt();
                    break;
            }

            FieldUtils.safeMultiply(val1, val2);

            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                long altVal1 = val1;
                int altVal2 = val2;

                switch (data.consumeInt(0, 5)) {
                    case 0:
                        altVal1 = val2 == 0 ? Long.MAX_VALUE : (Long.MAX_VALUE / val2) + 1L;
                        break;
                    case 1:
                        altVal1 = val2 == 0 ? Long.MIN_VALUE : (Long.MIN_VALUE / (long) (val2 == -1 ? 1 : val2)) - 1L;
                        break;
                    case 2:
                        altVal1 = -val1;
                        break;
                    case 3:
                        altVal2 = data.consumeBoolean() ? -1 : 1;
                        break;
                    case 4:
                        altVal1 = Long.MIN_VALUE;
                        altVal2 = -1;
                        break;
                    default:
                        altVal1 = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
                        altVal2 = data.consumeInt();
                        break;
                }

                FieldUtils.safeMultiply(altVal1, altVal2);
            }
        }
    }
}