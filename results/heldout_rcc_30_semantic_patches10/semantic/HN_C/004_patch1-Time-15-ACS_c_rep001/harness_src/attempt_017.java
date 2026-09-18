package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        byte[] longBytes = data.consumeBytes(8);
        long fromBytes = 0L;
        for (byte b : longBytes) {
            fromBytes = (fromBytes << 8) | (b & 0xffL);
        }
        if (longBytes.length > 0 && longBytes.length < 8 && (longBytes[0] & 0x80) != 0) {
            fromBytes |= (-1L) << (longBytes.length * 8);
        }

        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long combined = (((long) hi) << 32) | (lo & 0xffffffffL);

        byte[] intBytes = data.consumeBytes(4);
        int fromIntBytes = 0;
        for (byte b : intBytes) {
            fromIntBytes = (fromIntBytes << 8) | (b & 0xff);
        }
        if (intBytes.length > 0 && intBytes.length < 4 && (intBytes[0] & 0x80) != 0) {
            fromIntBytes |= (-1) << (intBytes.length * 8);
        }

        String ascii = data.consumeAsciiString(32);
        long fromAscii = 0L;
        for (int i = 0; i < ascii.length(); i++) {
            fromAscii = fromAscii * 33 + ascii.charAt(i);
        }
        if (data.consumeBoolean()) {
            fromAscii = -fromAscii;
        }

        byte[] remaining = data.consumeRemainingAsBytes();
        long fromRemaining = 0L;
        for (int i = 0; i < remaining.length && i < 8; i++) {
            fromRemaining = (fromRemaining << 8) | (remaining[i] & 0xffL);
        }
        if (remaining.length > 0 && remaining.length < 8 && (remaining[0] & 0x80) != 0) {
            fromRemaining |= (-1L) << (Math.min(remaining.length, 8) * 8);
        }

        long[] val1Candidates = new long[] {
            0L,
            1L,
            -1L,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            (long) Integer.MIN_VALUE,
            (long) Integer.MAX_VALUE,
            fromBytes,
            combined,
            (long) hi,
            (long) lo,
            fromAscii,
            fromRemaining
        };

        int anyInt = fromIntBytes ^ hi ^ lo;
        int[] val2Candidates = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            anyInt,
            fromIntBytes,
            hi,
            lo,
            data.consumeInt(-3, 3)
        };

        int checks = 1 + data.consumeInt(0, 15);
        for (int i = 0; i < checks; i++) {
            long val1 = val1Candidates[data.consumeInt(0, val1Candidates.length - 1)];
            int val2 = val2Candidates[data.consumeInt(0, val2Candidates.length - 1)];

            BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
            boolean shouldOverflow =
                    expected.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                            || expected.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0;

            try {
                long actual = FieldUtils.safeMultiply(val1, val2);
                if (shouldOverflow) {
                    throw new AssertionError("Expected overflow for " + val1 + " * " + val2 + ", got " + actual);
                }
                long expectedLong = expected.longValue();
                if (actual != expectedLong) {
                    throw new AssertionError(
                            "Incorrect result for " + val1 + " * " + val2 + ": expected " + expectedLong + " but got " + actual);
                }
            } catch (ArithmeticException e) {
                if (!shouldOverflow) {
                    throw e;
                }
            }
        }
    }
}