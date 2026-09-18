package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long combined = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        String ascii = data.consumeAsciiString(32);
        String any = data.consumeString(32);
        byte[] bytes = data.consumeBytes(16);
        boolean flip = data.consumeBoolean();
        byte b = data.consumeByte();
        int extra = data.consumeInt();
        int selector = data.consumeInt(0, 15);

        long fromAscii = parseLongOrFallback(ascii, combined);
        long fromAny = parseLongOrFallback(any, ~combined);
        long fromBytes = bytesToLong(bytes);

        long[] longVals = new long[] {
            combined,
            fromAscii,
            fromAny,
            fromBytes,
            extra,
            b,
            0L,
            1L,
            -1L,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            flip ? -combined : combined,
            combined ^ fromBytes,
            combined + fromAscii
        };

        int[] intVals = new int[] {
            data.consumeInt(),
            extra,
            b,
            0,
            1,
            -1,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            selector,
            ascii.length(),
            any.length(),
            bytes.length,
            data.remainingBytes()
        };

        for (long val1 : longVals) {
            for (int val2 : intVals) {
                checkSafeMultiply(val1, val2);
            }
        }

        checkSafeMultiply(Long.MIN_VALUE, -1);
        checkSafeMultiply(Long.MIN_VALUE, 0);
        checkSafeMultiply(Long.MIN_VALUE, 1);
        checkSafeMultiply(Long.MAX_VALUE, -1);
        checkSafeMultiply(Long.MAX_VALUE, 1);
        checkSafeMultiply(Long.MAX_VALUE, 2);
        checkSafeMultiply(Long.MIN_VALUE, 2);
        checkSafeMultiply(-1L, Integer.MIN_VALUE);
        checkSafeMultiply(1L, Integer.MIN_VALUE);
        checkSafeMultiply(0L, Integer.MIN_VALUE);
    }

    private static void checkSafeMultiply(long val1, int val2) {
        BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
        boolean fits = expected.bitLength() <= 63;

        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (!fits) {
                throw new AssertionError("Expected overflow for " + val1 + " * " + val2 + " but got " + actual);
            }
            if (actual != expected.longValue()) {
                throw new AssertionError(
                    "Incorrect result for " + val1 + " * " + val2 + ": got " + actual + ", expected " + expected.longValue());
            }
        } catch (ArithmeticException e) {
            if (fits) {
                throw e;
            }
        }
    }

    private static long parseLongOrFallback(String s, long fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static long bytesToLong(byte[] bytes) {
        long v = 0L;
        int len = Math.min(bytes.length, 8);
        for (int i = 0; i < len; i++) {
            v = (v << 8) | (bytes[i] & 0xffL);
        }
        return v;
    }
}