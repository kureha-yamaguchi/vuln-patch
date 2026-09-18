package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        byte[] raw = data.consumeBytes(8);
        long fromBytes = 0L;
        for (int i = 0; i < raw.length; i++) {
            fromBytes = (fromBytes << 8) | (raw[i] & 0xffL);
        }
        if (raw.length > 0 && data.consumeBoolean()) {
            fromBytes = -fromBytes;
        }

        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long fromInts = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        int bounded = data.consumeInt(-3, 3);
        int any = data.consumeInt();
        byte b = data.consumeByte();

        long[] vals = new long[] {
            0L,
            1L,
            -1L,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            fromBytes,
            fromInts,
            -fromInts,
            fromBytes ^ fromInts,
            data.remainingBytes()
        };

        int[] mults = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            bounded,
            any,
            b
        };

        for (long v : vals) {
            for (int m : mults) {
                long result = FieldUtils.safeMultiply(v, m);

                if (m == 0 && result != 0L) {
                    throw new IllegalStateException("safeMultiply returned non-zero for multiplier 0");
                }
                if (m == 1 && result != v) {
                    throw new IllegalStateException("safeMultiply changed value for multiplier 1");
                }
                if (m == -1) {
                    if (v == Long.MIN_VALUE) {
                        throw new IllegalStateException("Expected overflow for Long.MIN_VALUE * -1");
                    }
                    if (result != -v) {
                        throw new IllegalStateException("Incorrect negation result");
                    }
                }

                if (m != 0) {
                    if (result / m != v) {
                        throw new IllegalStateException("Non-overflowing multiplication invariant violated");
                    }
                }
            }
        }
    }
}