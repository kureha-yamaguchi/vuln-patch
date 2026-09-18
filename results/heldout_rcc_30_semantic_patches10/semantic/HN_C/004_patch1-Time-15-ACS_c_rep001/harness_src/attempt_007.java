package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int rounds = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 4) : 0);

        for (int i = 0; i < rounds; i++) {
            long val1;
            int val2;

            int mode = data.consumeInt(0, 5);
            switch (mode) {
                case 0:
                    val1 = pickSpecialLong(data);
                    val2 = pickSpecialInt(data);
                    break;
                case 1:
                    val1 = nextLong(data);
                    val2 = nextInt(data);
                    break;
                case 2:
                    val1 = nextLong(data);
                    val2 = pickSpecialInt(data);
                    break;
                case 3:
                    val1 = pickSpecialLong(data);
                    val2 = nextInt(data);
                    break;
                case 4:
                    val1 = composeLongFromBytes(data.consumeBytes(8));
                    val2 = composeIntFromBytes(data.consumeBytes(4));
                    break;
                default:
                    String s1 = data.consumeString(32);
                    String s2 = data.consumeAsciiString(16);
                    val1 = stringToLong(s1);
                    val2 = (int) stringToLong(s2);
                    break;
            }

            checkSafeMultiply(val1, val2);
        }

        if (data.remainingBytes() == 0 || data.consumeBoolean()) {
            long val1 = Long.MIN_VALUE;
            int val2 = data.consumeBoolean() ? -1 : pickSpecialInt(data);
            checkSafeMultiply(val1, val2);
        }
    }

    private static void checkSafeMultiply(long val1, int val2) {
        BigInteger product = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
        boolean shouldOverflow =
                product.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                        || product.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0;

        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (shouldOverflow) {
                throw new AssertionError("Expected overflow for " + val1 + " * " + val2 + " but got " + actual);
            }
            if (actual != product.longValue()) {
                throw new AssertionError("Incorrect result for " + val1 + " * " + val2 + ": " + actual);
            }
        } catch (ArithmeticException e) {
            if (!shouldOverflow) {
                throw e;
            }
        }
    }

    private static long nextLong(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 7)) {
            case 0:
                return ((long) data.consumeInt() << 32) ^ (data.consumeInt() & 0xffffffffL);
            case 1:
                return data.consumeInt();
            case 2:
                return composeLongFromBytes(data.consumeBytes(8));
            case 3:
                return composeLongFromBytes(data.consumeRemainingAsBytes());
            case 4:
                return stringToLong(data.consumeRemainingAsString());
            case 5:
                return stringToLong(data.consumeAsciiString(32));
            case 6:
                return pickSpecialLong(data);
            default:
                return (long) data.consumeByte();
        }
    }

    private static int nextInt(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 6)) {
            case 0:
                return data.consumeInt();
            case 1:
                return data.consumeByte();
            case 2:
                return composeIntFromBytes(data.consumeBytes(4));
            case 3:
                return (int) stringToLong(data.consumeAsciiString(16));
            case 4:
                return (int) stringToLong(data.consumeString(16));
            case 5:
                return pickSpecialInt(data);
            default:
                return data.consumeInt(-3, 3);
        }
    }

    private static long pickSpecialLong(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 9)) {
            case 0:
                return 0L;
            case 1:
                return 1L;
            case 2:
                return -1L;
            case 3:
                return Long.MIN_VALUE;
            case 4:
                return Long.MAX_VALUE;
            case 5:
                return Integer.MIN_VALUE;
            case 6:
                return Integer.MAX_VALUE;
            case 7:
                return ((long) Integer.MIN_VALUE) - 1L;
            case 8:
                return ((long) Integer.MAX_VALUE) + 1L;
            default:
                return ((long) data.consumeInt() << 32) | (data.consumeInt() & 0xffffffffL);
        }
    }

    private static int pickSpecialInt(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 9)) {
            case 0:
                return -1;
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return -2;
            case 5:
                return Integer.MIN_VALUE;
            case 6:
                return Integer.MAX_VALUE;
            case 7:
                return data.consumeInt(-3, 3);
            case 8:
                return data.consumeByte();
            default:
                return data.consumeInt();
        }
    }

    private static long composeLongFromBytes(byte[] bytes) {
        long v = 0L;
        int len = bytes.length > 8 ? 8 : bytes.length;
        for (int i = 0; i < len; i++) {
            v = (v << 8) | (bytes[i] & 0xffL);
        }
        if (len > 0 && (bytes[0] & 0x80) != 0) {
            v |= (-1L) << (len * 8);
        }
        return v;
    }

    private static int composeIntFromBytes(byte[] bytes) {
        int v = 0;
        int len = bytes.length > 4 ? 4 : bytes.length;
        for (int i = 0; i < len; i++) {
            v = (v << 8) | (bytes[i] & 0xff);
        }
        if (len > 0 && (bytes[0] & 0x80) != 0) {
            v |= (-1) << (len * 8);
        }
        return v;
    }

    private static long stringToLong(String s) {
        if (s == null || s.length() == 0) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException ignored) {
            long h = 1125899906842597L;
            for (int i = 0; i < s.length(); i++) {
                h = 31L * h + s.charAt(i);
            }
            return h;
        }
    }
}