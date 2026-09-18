package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int rawInt1 = data.consumeInt();
        int rawInt2 = data.consumeInt();
        int rawInt3 = data.consumeInt();
        int rangedInt = data.consumeInt(-3, 3);
        byte rawByte = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        long fromInts1 = (((long) rawInt1) << 32) ^ (rawInt2 & 0xffffffffL);
        long fromInts2 = (((long) rawInt2) << 32) ^ (rawInt3 & 0xffffffffL);

        long fromBytes1 = 0L;
        for (int i = 0; i < bytes1.length; i++) {
            fromBytes1 = (fromBytes1 << 8) ^ (bytes1[i] & 0xffL);
        }

        long fromBytes2 = 0L;
        for (int i = 0; i < bytes2.length; i++) {
            fromBytes2 = (fromBytes2 * 257L) + (bytes2[i] & 0xffL);
        }

        long fromStrings = (((long) s1.hashCode()) << 32) ^ (s2.hashCode() & 0xffffffffL);
        long mixed = fromInts1 ^ fromInts2 ^ fromBytes1 ^ fromBytes2 ^ fromStrings ^ rawByte;
        if (flip) {
            mixed = ~mixed;
        }

        int[] intCandidates = new int[] {
            -1, 0, 1,
            2, -2, 3, -3,
            Integer.MIN_VALUE, Integer.MAX_VALUE,
            rawInt1, rawInt2, rawInt3, rangedInt, rawByte
        };

        long[] longCandidates = new long[] {
            Long.MIN_VALUE, Long.MAX_VALUE,
            Integer.MIN_VALUE, Integer.MAX_VALUE,
            -1L, 0L, 1L, 2L, -2L,
            fromInts1, fromInts2, fromBytes1, fromBytes2, fromStrings, mixed,
            rawInt1, rawInt2, rawInt3, rawByte
        };

        for (int i = 0; i < intCandidates.length; i++) {
            FieldUtils.safeMultiply(longCandidates[i % longCandidates.length], intCandidates[i]);
        }

        int dynamicInt = intCandidates[Math.floorMod(rawInt1, intCandidates.length)];
        if (dynamicInt == -1 || dynamicInt == 0 || dynamicInt == 1) {
            dynamicInt = 2;
        }

        long thresholdPos = Long.MAX_VALUE / dynamicInt;
        long thresholdNeg = Long.MIN_VALUE / dynamicInt;

        FieldUtils.safeMultiply(thresholdPos, dynamicInt);
        FieldUtils.safeMultiply(thresholdNeg, dynamicInt);

        long near1 = thresholdPos + (flip ? 1L : -1L);
        long near2 = thresholdNeg + (flip ? -1L : 1L);

        FieldUtils.safeMultiply(near1, dynamicInt);
        FieldUtils.safeMultiply(near2, dynamicInt);
    }
}