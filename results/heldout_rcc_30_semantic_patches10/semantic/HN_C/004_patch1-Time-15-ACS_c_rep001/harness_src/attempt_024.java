package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long combined = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);

        byte[] raw = data.consumeBytes(Math.min(8, data.remainingBytes()));
        long fromBytes = 0L;
        for (int i = 0; i < raw.length; i++) {
            fromBytes = (fromBytes << 8) | (raw[i] & 0xffL);
        }
        if (data.consumeBoolean()) {
            fromBytes = -fromBytes;
        }

        String s1 = data.consumeAsciiString(32);
        String s2 = data.consumeString(32);
        String s3 = data.consumeRemainingAsString();

        long[] vals1 = new long[] {
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            -1L,
            0L,
            1L,
            2L,
            -2L,
            combined,
            fromBytes,
            s1.length(),
            -s1.length(),
            s2.length(),
            -s2.length(),
            s3.length(),
            -s3.length()
        };

        int[] vals2 = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            data.consumeInt(),
            data.consumeInt(-3, 3),
            data.consumeByte(),
            s1.length(),
            -s1.length(),
            s2.length(),
            -s2.length(),
            s3.length(),
            -s3.length()
        };

        checkSafeMultiply(Long.MIN_VALUE, -1);

        int rounds = 1 + Math.floorMod(data.consumeInt(), 32);
        for (int i = 0; i < rounds; i++) {
            long val1 = vals1[Math.floorMod(data.consumeInt(), vals1.length)];
            int val2 = vals2[Math.floorMod(data.consumeInt(), vals2.length)];

            if (data.consumeBoolean()) {
                val1 ^= combined;
            }
            if (data.consumeBoolean()) {
                val1 = ~val1;
            }
            if (data.consumeBoolean()) {
                val2 ^= data.consumeInt(-3, 3);
            }
            if (data.consumeBoolean()) {
                val2 = ~val2;
            }

            checkSafeMultiply(val1, val2);
        }
    }

    private static void checkSafeMultiply(long val1, int val2) {
        BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
        boolean shouldOverflow =
                expected.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0 ||
                expected.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0;

        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (shouldOverflow) {
                throw new AssertionError("Expected ArithmeticException for overflow: " + val1 + " * " + val2 + ", got " + actual);
            }
            long expectedLong = expected.longValue();
            if (actual != expectedLong) {
                throw new AssertionError("Incorrect result for " + val1 + " * " + val2 + ": expected " + expectedLong + " but got " + actual);
            }
        } catch (ArithmeticException e) {
            if (!shouldOverflow) {
                throw e;
            }
        }
    }
}