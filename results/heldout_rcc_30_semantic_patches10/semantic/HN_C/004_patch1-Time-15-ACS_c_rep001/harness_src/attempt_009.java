package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    private static final BigInteger BIG_LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger BIG_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int anyInt1 = data.consumeInt();
        int anyInt2 = data.consumeInt();
        int boundedInt = data.consumeInt(-8, 8);
        byte anyByte = data.consumeByte();
        boolean forceBoundary = data.consumeBoolean();

        String ascii = data.consumeAsciiString(32);
        String unicode = data.consumeString(32);

        int maxVarBytes = Math.min(16, data.remainingBytes());
        byte[] varBytes = data.consumeBytes(data.consumeInt(0, maxVarBytes));
        byte[] tailBytes = data.consumeRemainingAsBytes();

        long combinedInts = (((long) anyInt1) << 32) ^ (anyInt2 & 0xffffffffL);
        long fromVarBytes = bytesToLong(varBytes);
        long fromTailBytes = bytesToLong(tailBytes);
        long fromAscii = stringToLong(ascii);
        long fromUnicode = stringToLong(unicode);
        long fromByte = anyByte;

        long[] longVals = new long[] {
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            -1L,
            0L,
            1L,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            combinedInts,
            fromVarBytes,
            fromTailBytes,
            fromAscii,
            fromUnicode,
            fromByte
        };

        int[] intVals = new int[] {
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            -2,
            -1,
            0,
            1,
            2,
            boundedInt,
            anyInt1,
            anyInt2,
            anyByte
        };

        if (forceBoundary) {
            checkMultiply(Long.MIN_VALUE, -1);
        }

        for (long v : longVals) {
            for (int m : intVals) {
                checkMultiply(v, m);
            }
        }
    }

    private static void checkMultiply(long val1, int val2) {
        BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
        boolean shouldOverflow = expected.compareTo(BIG_LONG_MIN) < 0 || expected.compareTo(BIG_LONG_MAX) > 0;

        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (shouldOverflow) {
                throw new AssertionError("Expected overflow but got result: " + val1 + " * " + val2 + " = " + actual);
            }
            long exact = expected.longValue();
            if (actual != exact) {
                throw new AssertionError("Incorrect result: " + val1 + " * " + val2 + " = " + actual + ", expected " + exact);
            }
        } catch (ArithmeticException e) {
            if (!shouldOverflow) {
                throw new AssertionError("Unexpected overflow: " + val1 + " * " + val2, e);
            }
        }
    }

    private static long bytesToLong(byte[] bytes) {
        long v = 0L;
        int len = Math.min(8, bytes.length);
        for (int i = 0; i < len; i++) {
            v = (v << 8) | (bytes[i] & 0xffL);
        }
        if (bytes.length > 0 && (bytes[0] & 0x80) != 0 && len < 8) {
            v |= (-1L) << (len * 8);
        }
        return v;
    }

    private static long stringToLong(String s) {
        if (s == null || s.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException ignored) {
            long h = 1125899906842597L;
            for (int i = 0; i < s.length(); i++) {
                h = 31 * h + s.charAt(i);
            }
            return h;
        }
    }
}