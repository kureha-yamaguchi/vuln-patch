package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();
        byte e = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes = data.consumeBytes(Math.min(16, data.remainingBytes()));

        long randomLong = (((long) a) << 32) ^ (b & 0xffffffffL);
        long randomLong2 = (((long) c) << 32) ^ (d & 0xffffffffL);
        if (flip) {
            randomLong = -randomLong;
        }

        long bytesLong = 0L;
        for (int i = 0; i < bytes.length && i < 8; i++) {
            bytesLong = (bytesLong << 8) | (bytes[i] & 0xffL);
        }
        if ((e & 1) != 0) {
            bytesLong = -bytesLong;
        }

        long parsed1 = 0L;
        try {
            parsed1 = Long.parseLong(s1.trim());
        } catch (RuntimeException ignored) {
        }

        long parsed2 = 0L;
        try {
            parsed2 = Long.parseLong(s2.trim());
        } catch (RuntimeException ignored) {
        }

        long[] vals1 = new long[] {
            randomLong,
            randomLong2,
            bytesLong,
            parsed1,
            parsed2,
            a,
            b,
            c,
            d,
            e,
            0L,
            1L,
            -1L,
            2L,
            -2L,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            (long) Integer.MAX_VALUE + 1L,
            (long) Integer.MIN_VALUE - 1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            Long.MAX_VALUE / 2L,
            Long.MIN_VALUE / 2L,
            Long.MAX_VALUE / 3L,
            Long.MIN_VALUE / 3L,
            3037000499L,
            -3037000499L
        };

        int[] vals2 = new int[] {
            a,
            b,
            c,
            d,
            e,
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            46340,
            -46340,
            65535,
            -65535
        };

        for (int i = 0; i < vals1.length; i++) {
            FieldUtils.safeMultiply(vals1[i], vals2[i % vals2.length]);
        }

        for (int i = 0; i < vals2.length; i++) {
            FieldUtils.safeMultiply(vals1[i % vals1.length], vals2[i]);
        }

        int extraCalls = data.remainingBytes();
        for (int i = 0; i < extraCalls; i++) {
            int x = data.consumeInt();
            int y = data.consumeInt();
            long v1 = (((long) x) << 32) ^ (y & 0xffffffffL);
            int v2 = data.consumeInt();
            if (data.consumeBoolean()) {
                v1 = -v1;
            }
            FieldUtils.safeMultiply(v1, v2);
            FieldUtils.safeMultiply(v1, 0);
            FieldUtils.safeMultiply(v1, 1);
            FieldUtils.safeMultiply(v1, -1);
        }
    }
}