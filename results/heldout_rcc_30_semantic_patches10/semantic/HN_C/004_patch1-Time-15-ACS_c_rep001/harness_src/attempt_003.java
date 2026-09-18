package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long[] longs = new long[16];
        int li = 0;

        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        longs[li++] = 0L;
        longs[li++] = 1L;
        longs[li++] = -1L;
        longs[li++] = Long.MAX_VALUE;
        longs[li++] = Long.MIN_VALUE;
        longs[li++] = Integer.MAX_VALUE;
        longs[li++] = Integer.MIN_VALUE;
        longs[li++] = (((long) a) << 32) ^ (b & 0xffffffffL);
        longs[li++] = (((long) c) << 32) ^ (d & 0xffffffffL);
        longs[li++] = (long) a;
        longs[li++] = (long) b;
        longs[li++] = -((long) c);

        byte[] bytes = data.consumeBytes(Math.min(16, data.remainingBytes()));
        long fromBytes = 0L;
        for (int i = 0; i < bytes.length; i++) {
            fromBytes = (fromBytes << 8) ^ (bytes[i] & 0xffL);
        }
        longs[li++] = fromBytes;
        longs[li++] = ~fromBytes;

        String ascii = data.consumeAsciiString(Math.min(32, data.remainingBytes()));
        longs[li++] = foldString(ascii);
        longs[li++] = -foldString(ascii);

        int[] ints = new int[16];
        int ii = 0;
        ints[ii++] = 0;
        ints[ii++] = 1;
        ints[ii++] = -1;
        ints[ii++] = Integer.MAX_VALUE;
        ints[ii++] = Integer.MIN_VALUE;
        ints[ii++] = data.consumeInt();
        ints[ii++] = data.consumeInt();
        ints[ii++] = data.consumeByte();
        ints[ii++] = -data.consumeByte();
        ints[ii++] = data.consumeInt(-3, 3);
        ints[ii++] = data.consumeInt(-16, 16);
        ints[ii++] = (int) fromBytes;
        ints[ii++] = (int) ~fromBytes;
        ints[ii++] = ascii.length();
        ints[ii++] = -ascii.length();
        ints[ii++] = data.consumeBoolean() ? 2 : -2;

        int rounds = data.remainingBytes() > 0 ? data.consumeInt(1, 24) : 8;
        for (int i = 0; i < rounds; i++) {
            long val1 = longs[(i + ints[Math.floorMod(i, ii)]) & 15];
            int val2 = ints[(i + (int) (longs[Math.floorMod(i, li)] & 15)) & 15];

            if ((i & 1) == 0) {
                val1 = mutateLong(val1, data.consumeBoolean(), data.consumeBoolean());
            } else {
                val2 = mutateInt(val2, data.consumeBoolean(), data.consumeBoolean());
            }

            FieldUtils.safeMultiply(val1, val2);
        }

        for (int i = 0; i < li; i++) {
            FieldUtils.safeMultiply(longs[i], -1);
            FieldUtils.safeMultiply(longs[i], 0);
            FieldUtils.safeMultiply(longs[i], 1);
        }
    }

    private static long foldString(String s) {
        long v = 0L;
        for (int i = 0; i < s.length(); i++) {
            v = (v * 131) + s.charAt(i);
        }
        return v;
    }

    private static long mutateLong(long v, boolean flipSign, boolean nudgeBoundary) {
        if (flipSign) {
            v = -v;
        }
        if (nudgeBoundary) {
            if (v == Long.MAX_VALUE) {
                return Long.MAX_VALUE - 1;
            }
            if (v == Long.MIN_VALUE) {
                return Long.MIN_VALUE + 1;
            }
            return v + 1;
        }
        return v;
    }

    private static int mutateInt(int v, boolean flipSign, boolean nudgeBoundary) {
        if (flipSign) {
            v = -v;
        }
        if (nudgeBoundary) {
            if (v == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE - 1;
            }
            if (v == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE + 1;
            }
            return v + 1;
        }
        return v;
    }
}