package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long hiLo = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        byte[] bytes = data.consumeBytes(8);
        long fromBytes = 0L;
        for (byte b : bytes) {
            fromBytes = (fromBytes << 8) | (b & 0xffL);
        }
        if (bytes.length > 0 && (bytes[0] & 0x80) != 0) {
            fromBytes |= (-1L) << (bytes.length * 8);
        }

        String s = data.consumeString(32);
        String ascii = data.consumeAsciiString(32);
        String rest = data.consumeRemainingAsString();

        long parsed = 0L;
        try {
            parsed = Long.parseLong(s);
        } catch (NumberFormatException ignored) {
            parsed = (long) s.hashCode();
        }

        long asciiDerived = (((long) ascii.hashCode()) << 32) ^ (rest.hashCode() & 0xffffffffL);

        int anyInt = data.consumeInt();
        int bounded = data.consumeInt(-2, 2);
        int fromByte = data.consumeByte();
        boolean flip = data.consumeBoolean();

        long[] val1s = new long[] {
            hiLo,
            fromBytes,
            parsed,
            asciiDerived,
            flip ? -hiLo : hiLo,
            flip ? -fromBytes : fromBytes,
            0L,
            1L,
            -1L,
            2L,
            -2L,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            Long.MAX_VALUE / 2,
            Long.MIN_VALUE / 2,
            (long) anyInt,
            (long) bounded,
            (long) fromByte
        };

        int[] val2s = new int[] {
            anyInt,
            bounded,
            fromByte,
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            s.length(),
            -s.length(),
            ascii.length(),
            -ascii.length(),
            rest.length(),
            -rest.length()
        };

        BigInteger longMin = BigInteger.valueOf(Long.MIN_VALUE);
        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);

        for (long val1 : val1s) {
            for (int val2 : val2s) {
                BigInteger expected = BigInteger.valueOf(val1).multiply(BigInteger.valueOf((long) val2));
                boolean shouldOverflow = expected.compareTo(longMin) < 0 || expected.compareTo(longMax) > 0;

                try {
                    long actual = FieldUtils.safeMultiply(val1, val2);
                    if (shouldOverflow) {
                        throw new AssertionError("Expected ArithmeticException for " + val1 + " * " + val2 + " but got " + actual);
                    }
                    if (actual != expected.longValue()) {
                        throw new AssertionError("Incorrect result for " + val1 + " * " + val2 + ": got " + actual + ", expected " + expected.longValue());
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