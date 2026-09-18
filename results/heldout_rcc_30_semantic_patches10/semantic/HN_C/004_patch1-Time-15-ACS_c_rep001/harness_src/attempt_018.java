package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkMultiply(Long.MIN_VALUE, -1);

        long fuzzLongA = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long fuzzLongB = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        byte[] bytes = data.consumeBytes(8);
        long fromBytes = 0L;
        for (int i = 0; i < bytes.length; i++) {
            fromBytes = (fromBytes << 8) | (bytes[i] & 0xffL);
        }
        if (bytes.length > 0 && bytes.length < 8 && (bytes[0] & 0x80) != 0) {
            fromBytes |= (-1L) << (bytes.length * 8);
        }

        String ascii = data.consumeAsciiString(32);
        long fromAscii = 0L;
        for (int i = 0; i < ascii.length(); i++) {
            fromAscii = (fromAscii * 131) + ascii.charAt(i);
        }
        if (data.consumeBoolean()) {
            fromAscii = -fromAscii;
        }

        int fuzzInt = data.consumeInt();
        int smallInt = data.consumeInt(-4, 4);

        long[] longCandidates = new long[] {
            Long.MIN_VALUE,
            Long.MIN_VALUE + 1,
            Long.MAX_VALUE,
            Long.MAX_VALUE - 1,
            -1L,
            0L,
            1L,
            2L,
            -2L,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            fuzzLongA,
            fuzzLongB,
            fromBytes,
            fromAscii
        };

        int[] intCandidates = new int[] {
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            -1,
            0,
            1,
            2,
            -2,
            smallInt,
            fuzzInt
        };

        for (int i = 0; i < longCandidates.length; i++) {
            for (int j = 0; j < intCandidates.length; j++) {
                checkMultiply(longCandidates[i], intCandidates[j]);
            }
        }

        int extra = data.consumeInt(0, 32);
        for (int i = 0; i < extra; i++) {
            long val1 = longCandidates[data.consumeInt(0, longCandidates.length - 1)];
            int val2 = intCandidates[data.consumeInt(0, intCandidates.length - 1)];
            if (data.consumeBoolean()) {
                val1 ^= (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            }
            if (data.consumeBoolean()) {
                val2 ^= data.consumeInt();
            }
            checkMultiply(val1, val2);
        }
    }

    private static void checkMultiply(long val1, int val2) {
        BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
        boolean overflow =
                expected.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0 ||
                expected.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0;

        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (overflow) {
                throw new AssertionError("Expected ArithmeticException for " + val1 + " * " + val2 + " but got " + actual);
            }
            long expectedLong = expected.longValue();
            if (actual != expectedLong) {
                throw new AssertionError("Wrong result for " + val1 + " * " + val2 + ": expected " + expectedLong + " but got " + actual);
            }
        } catch (ArithmeticException e) {
            if (!overflow) {
                throw e;
            }
        }
    }
}