package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long base = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        byte[] bytes = data.consumeBytes(Math.min(8, data.remainingBytes()));
        long folded = 0L;
        for (byte b : bytes) {
            folded = (folded << 8) ^ (b & 0xffL);
        }

        String s1 = data.consumeString(Math.min(32, data.remainingBytes()));
        String s2 = data.consumeAsciiString(Math.min(32, data.remainingBytes()));

        long fromStrings = 0L;
        for (int i = 0; i < s1.length(); i++) {
            fromStrings = fromStrings * 131 + s1.charAt(i);
        }
        for (int i = 0; i < s2.length(); i++) {
            fromStrings = fromStrings * 33 + s2.charAt(i);
        }

        if (data.consumeBoolean()) {
            base ^= folded;
        }
        if (data.consumeBoolean()) {
            base ^= fromStrings;
        }
        if (data.consumeBoolean()) {
            base = ~base;
        }

        int fuzzInt = data.consumeInt();
        int boundedInt = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        int fuzzByteAsInt = data.consumeByte();
        String tail = data.consumeRemainingAsString();

        long[] vals1 = new long[] {
            base,
            folded,
            fromStrings,
            0L,
            1L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            base ^ Long.MIN_VALUE,
            tail.hashCode()
        };

        int[] vals2 = new int[] {
            fuzzInt,
            boundedInt,
            fuzzByteAsInt,
            -1,
            0,
            1,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            tail.length(),
            -tail.length()
        };

        BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);
        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);

        for (long val1 : vals1) {
            for (int val2 : vals2) {
                BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
                boolean shouldOverflow = expected.compareTo(longMin) < 0 || expected.compareTo(longMax) > 0;

                try {
                    long actual = FieldUtils.safeMultiply(val1, val2);
                    if (shouldOverflow) {
                        throw new AssertionError("Expected overflow for safeMultiply(" + val1 + ", " + val2 + "), got " + actual);
                    }
                    if (actual != expected.longValue()) {
                        throw new AssertionError("Incorrect result for safeMultiply(" + val1 + ", " + val2 + "): " + actual + " != " + expected.longValue());
                    }
                } catch (ArithmeticException e) {
                    if (!shouldOverflow) {
                        throw e;
                    }
                }
            }
        }
    }
}