package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int i1 = data.consumeInt();
        int i2 = data.consumeInt();
        int i3 = data.consumeInt();
        int i4 = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        byte b1 = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        long fromInts1 = (((long) i1) << 32) ^ (i2 & 0xffffffffL);
        long fromInts2 = (((long) i3) << 32) ^ (i4 & 0xffffffffL);
        long fromBytes1 = 0L;
        for (byte b : bytes1) {
            fromBytes1 = (fromBytes1 << 8) ^ (b & 0xffL);
        }
        long fromBytes2 = 0L;
        for (byte b : bytes2) {
            fromBytes2 = (fromBytes2 * 257L) ^ (b & 0xffL);
        }
        long fromString1 = 0L;
        for (int idx = 0; idx < s1.length(); idx++) {
            fromString1 = (fromString1 * 131L) + s1.charAt(idx);
        }
        long fromString2 = 0L;
        for (int idx = 0; idx < s2.length(); idx++) {
            fromString2 = (fromString2 << 5) - fromString2 + s2.charAt(idx);
        }

        long[] val1s = new long[] {
            0L,
            1L,
            -1L,
            2L,
            -2L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            Long.MAX_VALUE / 2L,
            Long.MIN_VALUE / 2L,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            fromInts1,
            fromInts2,
            fromBytes1,
            fromBytes2,
            fromString1,
            fromString2,
            flip ? -fromInts1 : fromInts1,
            flip ? ~fromBytes2 : fromBytes2,
            ((long) b1),
            (((long) i1) * ((long) i2))
        };

        int[] val2s = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            i1,
            i2,
            i3,
            i4,
            b1,
            s1.length(),
            -s1.length(),
            s2.length(),
            -s2.length()
        };

        BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);
        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);

        for (long val1 : val1s) {
            for (int val2 : val2s) {
                BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf(val2));
                boolean overflow = expected.compareTo(longMin) < 0 || expected.compareTo(longMax) > 0;
                try {
                    long actual = FieldUtils.safeMultiply(val1, val2);
                    if (overflow) {
                        throw new AssertionError("Expected overflow not detected for " + val1 + " * " + val2);
                    }
                    if (actual != expected.longValue()) {
                        throw new AssertionError("Incorrect result for " + val1 + " * " + val2 + ": " + actual);
                    }
                } catch (ArithmeticException e) {
                    if (!overflow) {
                        throw e;
                    }
                }
            }
        }
    }
}