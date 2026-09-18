package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int i1 = data.consumeInt();
        int i2 = data.consumeInt();
        int i3 = data.consumeInt();
        int small = data.consumeInt(-3, 3);
        byte b = data.consumeByte();
        boolean chooseMin = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes = data.consumeBytes(16);
        byte[] remaining = data.consumeRemainingAsBytes();

        long bytesLong = 0L;
        for (int i = 0; i < bytes.length && i < 8; i++) {
            bytesLong = (bytesLong << 8) | (bytes[i] & 0xffL);
        }

        long remainingLong = 0L;
        for (int i = 0; i < remaining.length && i < 8; i++) {
            remainingLong = (remainingLong << 8) | (remaining[i] & 0xffL);
        }

        long parsed1 = 0L;
        try {
            parsed1 = Long.parseLong(s1.trim());
        } catch (NumberFormatException ignored) {
            parsed1 = s1.hashCode();
        }

        long parsed2 = 0L;
        try {
            parsed2 = Long.parseLong(s2.trim());
        } catch (NumberFormatException ignored) {
            parsed2 = s2.hashCode();
        }

        long combined = (((long) i1) << 32) ^ (i2 & 0xffffffffL);
        long[] val1s = new long[] {
            0L,
            1L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            (long) i1,
            (long) i2,
            (long) i3,
            combined,
            bytesLong,
            remainingLong,
            parsed1,
            parsed2,
            chooseMin ? Long.MIN_VALUE : combined
        };

        int[] val2s = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            3,
            -3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            i1,
            i2,
            i3,
            small,
            b
        };

        for (long val1 : val1s) {
            for (int val2 : val2s) {
                java.math.BigInteger exact =
                        java.math.BigInteger.valueOf(val1).multiply(java.math.BigInteger.valueOf(val2));
                boolean overflow =
                        exact.compareTo(java.math.BigInteger.valueOf(Long.MIN_VALUE)) < 0
                                || exact.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0;
                try {
                    long result = FieldUtils.safeMultiply(val1, val2);
                    if (overflow) {
                        throw new IllegalStateException(
                                "safeMultiply failed to throw on overflow: " + val1 + " * " + val2 + " = " + result);
                    }
                    if (result != exact.longValue()) {
                        throw new IllegalStateException(
                                "safeMultiply returned incorrect result: " + val1 + " * " + val2 + " = " + result);
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