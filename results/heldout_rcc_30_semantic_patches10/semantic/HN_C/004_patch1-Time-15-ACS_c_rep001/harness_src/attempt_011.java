package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        long combined = (((long) a) << 32) ^ (b & 0xffffffffL);

        byte[] raw = data.consumeBytes(Math.min(16, data.remainingBytes()));
        long fromBytes = 0L;
        for (int i = 0; i < raw.length; i++) {
            fromBytes = (fromBytes << 8) | (raw[i] & 0xffL);
        }
        if (raw.length > 0 && (raw[0] & 0x80) != 0) {
            fromBytes |= (-1L) << (raw.length * 8);
        }

        String s1 = data.consumeString(Math.min(32, data.remainingBytes()));
        String s2 = data.consumeAsciiString(Math.min(32, data.remainingBytes()));
        String s3 = data.consumeRemainingAsString();

        long stringDerived = ((long) s1.length() << 48)
                ^ ((long) s2.length() << 32)
                ^ ((long) s3.length() << 16)
                ^ s1.hashCode()
                ^ ((long) s2.hashCode() << 1)
                ^ ((long) s3.hashCode() << 2);

        long[] longVals = new long[] {
                combined,
                fromBytes,
                stringDerived,
                (long) a,
                (long) b,
                -(long) a,
                -(long) b,
                0L,
                1L,
                -1L,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                Long.MAX_VALUE / 2,
                Long.MIN_VALUE / 2,
                3037000499L,
                -3037000499L
        };

        int c = a;
        if ((b & 7) == 0) {
            c = -1;
        } else if ((b & 7) == 1) {
            c = 0;
        } else if ((b & 7) == 2) {
            c = 1;
        } else if ((b & 7) == 3) {
            c = Integer.MAX_VALUE;
        } else if ((b & 7) == 4) {
            c = Integer.MIN_VALUE;
        }

        byte by = data.consumeByte();
        int ranged = data.consumeInt(-8, 8);
        boolean flip = data.consumeBoolean();

        int[] intVals = new int[] {
                c,
                b,
                by,
                ranged,
                flip ? -1 : 1,
                0,
                1,
                -1,
                2,
                -2,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                46340,
                -46340
        };

        for (int i = 0; i < longVals.length; i++) {
            FieldUtils.safeMultiply(longVals[i], intVals[i % intVals.length]);
        }

        for (int i = 0; i < intVals.length; i++) {
            FieldUtils.safeMultiply(longVals[i % longVals.length], intVals[i]);
        }

        long chosenLong = longVals[Math.floorMod(a, longVals.length)];
        int chosenInt = intVals[Math.floorMod(b, intVals.length)];
        FieldUtils.safeMultiply(chosenLong, chosenInt);

        FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        FieldUtils.safeMultiply(Long.MAX_VALUE, 0);
        FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
    }
}