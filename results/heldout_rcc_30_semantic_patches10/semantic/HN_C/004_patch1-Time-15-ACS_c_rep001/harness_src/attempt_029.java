package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int seedA = data.consumeInt();
        int seedB = data.consumeInt();
        byte seedByte = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] b1 = data.consumeBytes(16);
        byte[] b2 = data.consumeRemainingAsBytes();

        long fromInts1 = (((long) seedA) << 32) ^ (seedB & 0xffffffffL);
        long fromInts2 = (((long) seedB) << 32) ^ (seedA & 0xffffffffL);

        long fromByteArrays1 = 0L;
        for (int i = 0; i < b1.length; i++) {
            fromByteArrays1 = (fromByteArrays1 << 8) ^ (b1[i] & 0xffL);
        }

        long fromByteArrays2 = 0L;
        for (int i = 0; i < b2.length; i++) {
            fromByteArrays2 = (fromByteArrays2 << 8) ^ (b2[i] & 0xffL);
        }

        long fromString1 = 0L;
        for (int i = 0; i < s1.length(); i++) {
            fromString1 = fromString1 * 131 + s1.charAt(i);
        }

        long fromString2 = 0L;
        for (int i = 0; i < s2.length(); i++) {
            fromString2 = fromString2 * 257 + s2.charAt(i);
        }

        if (flip) {
            fromString1 = -fromString1;
            fromByteArrays2 = -fromByteArrays2;
        }

        long[] longVals = new long[] {
            0L,
            1L,
            -1L,
            2L,
            -2L,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            Long.MAX_VALUE / 2L,
            Long.MIN_VALUE / 2L,
            Long.MAX_VALUE / 3L,
            Long.MIN_VALUE / 3L,
            seedA,
            seedB,
            seedByte,
            fromInts1,
            fromInts2,
            fromByteArrays1,
            fromByteArrays2,
            fromString1,
            fromString2,
            fromInts1 + fromByteArrays1,
            fromInts2 - fromByteArrays2,
            ~fromInts1,
            ~fromString1
        };

        int bounded = data.remainingBytes() > 0 ? data.consumeInt(-8, 8) : 0;
        int[] intVals = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            3,
            -3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            seedA,
            seedB,
            seedByte,
            bounded
        };

        for (int i = 0; i < longVals.length; i++) {
            for (int j = 0; j < intVals.length; j++) {
                FieldUtils.safeMultiply(longVals[i], intVals[j]);
            }
        }

        for (int i = 0; i < longVals.length; i++) {
            int derived;
            switch (i % 6) {
                case 0:
                    derived = -1;
                    break;
                case 1:
                    derived = 0;
                    break;
                case 2:
                    derived = 1;
                    break;
                case 3:
                    derived = (int) longVals[i];
                    break;
                case 4:
                    derived = (int) (longVals[i] >>> 32);
                    break;
                default:
                    derived = (int) (longVals[i] ^ seedA ^ seedB);
                    break;
            }
            FieldUtils.safeMultiply(longVals[i], derived);
        }
    }
}