package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int iterations = data.consumeInt(1, 8);

        long[] longEdges = new long[] {
            0L,
            1L,
            -1L,
            2L,
            -2L,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            (long) Integer.MIN_VALUE - 1L,
            (long) Integer.MAX_VALUE + 1L,
            1L << 31,
            -(1L << 31),
            1L << 32,
            -(1L << 32),
            1L << 62,
            -(1L << 62)
        };

        int[] intEdges = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            3,
            -3,
            46340,
            -46340,
            65535,
            -65535
        };

        for (int i = 0; i < iterations; i++) {
            long val1;
            switch (data.consumeInt(0, 7)) {
                case 0: {
                    int hi = data.consumeInt();
                    int lo = data.consumeInt();
                    val1 = (((long) hi) << 32) ^ (lo & 0xffffffffL);
                    break;
                }
                case 1: {
                    val1 = data.consumeInt();
                    break;
                }
                case 2: {
                    byte[] bytes = data.consumeBytes(8);
                    long tmp = 0L;
                    for (int j = 0; j < bytes.length; j++) {
                        tmp = (tmp << 8) | (bytes[j] & 0xffL);
                    }
                    if (data.consumeBoolean()) {
                        int shift = (8 - bytes.length) * 8;
                        if (bytes.length > 0) {
                            tmp = (tmp << shift) >> shift;
                        }
                    }
                    val1 = tmp;
                    break;
                }
                case 3: {
                    String s = data.consumeString(32);
                    long tmp = s.length();
                    for (int j = 0; j < s.length(); j++) {
                        tmp = (tmp * 131) + s.charAt(j);
                    }
                    val1 = data.consumeBoolean() ? -tmp : tmp;
                    break;
                }
                case 4: {
                    val1 = longEdges[data.consumeInt(0, longEdges.length - 1)];
                    break;
                }
                case 5: {
                    int a = data.consumeInt();
                    int b = data.consumeInt();
                    long product = (long) a * (long) b;
                    val1 = data.consumeBoolean() ? product : product + a - b;
                    break;
                }
                case 6: {
                    String s = data.consumeAsciiString(32);
                    long tmp = 0L;
                    for (int j = 0; j < s.length(); j++) {
                        tmp = (tmp << 5) - tmp + s.charAt(j);
                    }
                    val1 = tmp;
                    break;
                }
                default: {
                    byte[] rem = data.consumeRemainingAsBytes();
                    long tmp = rem.length;
                    for (int j = 0; j < rem.length; j++) {
                        tmp ^= (tmp << 7) + (tmp >>> 3) + (rem[j] & 0xffL) + j;
                    }
                    val1 = tmp;
                    break;
                }
            }

            int val2;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    val2 = data.consumeInt();
                    break;
                case 1:
                    val2 = intEdges[data.consumeInt(0, intEdges.length - 1)];
                    break;
                case 2: {
                    byte b = data.consumeByte();
                    val2 = b;
                    break;
                }
                case 3: {
                    String s = data.consumeAsciiString(16);
                    int tmp = s.length();
                    for (int j = 0; j < s.length(); j++) {
                        tmp = (tmp * 33) ^ s.charAt(j);
                    }
                    val2 = data.consumeBoolean() ? -tmp : tmp;
                    break;
                }
                case 4:
                    val2 = data.consumeInt(-3, 3);
                    break;
                case 5: {
                    byte[] bytes = data.consumeBytes(4);
                    int tmp = 0;
                    for (int j = 0; j < bytes.length; j++) {
                        tmp = (tmp << 8) | (bytes[j] & 0xff);
                    }
                    if (data.consumeBoolean() && bytes.length > 0) {
                        int shift = (4 - bytes.length) * 8;
                        tmp = (tmp << shift) >> shift;
                    }
                    val2 = tmp;
                    break;
                }
                case 6:
                    val2 = data.remainingBytes();
                    if (data.consumeBoolean()) {
                        val2 = -val2;
                    }
                    break;
                default:
                    val2 = data.consumeBoolean() ? -1 : 1;
                    break;
            }

            FieldUtils.safeMultiply(val1, val2);
        }
    }
}