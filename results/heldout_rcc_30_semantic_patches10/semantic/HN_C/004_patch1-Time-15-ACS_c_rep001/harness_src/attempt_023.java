package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long combined = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        byte[] bytes = data.consumeBytes(Math.min(data.remainingBytes(), 16));
        long fromBytes = 0L;
        for (int i = 0; i < bytes.length; i++) {
            fromBytes = (fromBytes << 8) | (bytes[i] & 0xffL);
        }
        if (data.consumeBoolean()) {
            fromBytes = -fromBytes;
        }

        String ascii = data.consumeAsciiString(32);
        String any = data.consumeString(32);
        String rest = data.consumeRemainingAsString();

        long[] val1s = new long[] {
            combined,
            fromBytes,
            0L,
            1L,
            -1L,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            ascii.length(),
            -ascii.length(),
            any.length(),
            -any.length(),
            rest.length(),
            -rest.length()
        };

        int[] val2s = new int[] {
            data.consumeInt(),
            data.consumeByte(),
            data.consumeInt(-3, 3),
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            ascii.length(),
            -ascii.length(),
            any.length(),
            -any.length(),
            rest.length(),
            -rest.length()
        };

        int iterations = 1 + Math.floorMod(data.consumeInt(), 8);
        for (int i = 0; i < iterations; i++) {
            long val1 = val1s[Math.floorMod(data.consumeInt(), val1s.length)];
            int val2 = val2s[Math.floorMod(data.consumeInt(), val2s.length)];

            if (data.consumeBoolean()) {
                val1 = ~val1;
            }
            if (data.consumeBoolean()) {
                val2 = ~val2;
            }
            if (data.consumeBoolean()) {
                val1 ^= (long) data.consumeInt();
            }
            if (data.consumeBoolean()) {
                val2 ^= data.consumeInt(-3, 3);
            }

            FieldUtils.safeMultiply(val1, val2);
        }
    }
}