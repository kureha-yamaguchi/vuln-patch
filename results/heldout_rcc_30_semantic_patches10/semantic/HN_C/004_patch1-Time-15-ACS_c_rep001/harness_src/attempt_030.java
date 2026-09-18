package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int i1 = data.consumeInt();
        int i2 = data.consumeInt();
        int i3 = data.consumeInt();
        int small = data.consumeInt(-3, 3);
        byte b = data.consumeByte();
        boolean flip = data.consumeBoolean();

        int asciiLen = Math.min(32, data.remainingBytes());
        String ascii = data.consumeAsciiString(asciiLen);

        int strLen = Math.min(32, data.remainingBytes());
        String any = data.consumeString(strLen);

        byte[] bytes = data.consumeBytes(Math.min(8, data.remainingBytes()));
        byte[] tail = data.consumeRemainingAsBytes();

        long fromInts = (((long) i1) << 32) ^ (i2 & 0xffffffffL);

        long fromBytes = 0L;
        for (byte x : bytes) {
            fromBytes = (fromBytes << 8) | (x & 0xffL);
        }

        long fromTail = 0L;
        int tailLimit = Math.min(8, tail.length);
        for (int idx = 0; idx < tailLimit; idx++) {
            fromTail = (fromTail << 8) | (tail[idx] & 0xffL);
        }

        long parsedAscii = 0L;
        try {
            if (!ascii.isEmpty()) {
                parsedAscii = Long.parseLong(ascii);
            }
        } catch (NumberFormatException ignored) {
            parsedAscii = ascii.hashCode();
        }

        long parsedAny = 0L;
        try {
            if (!any.isEmpty()) {
                parsedAny = Long.parseLong(any.trim());
            }
        } catch (NumberFormatException ignored) {
            parsedAny = any.hashCode();
        }

        if (flip) {
            fromInts = ~fromInts;
            fromBytes = -fromBytes;
        }

        long[] vals = new long[] {
            0L,
            1L,
            -1L,
            2L,
            -2L,
            (long) Integer.MIN_VALUE,
            (long) Integer.MAX_VALUE,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            Long.MIN_VALUE + 1,
            Long.MAX_VALUE - 1,
            fromInts,
            fromBytes,
            fromTail,
            parsedAscii,
            parsedAny,
            (long) i1,
            (long) i2,
            (long) i3,
            (((long) i3) << 1),
            ((fromInts >>> 1) == 0 ? fromInts : (fromInts >>> 1))
        };

        int[] mults = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            small,
            b,
            i1,
            i2,
            i3,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (long v : vals) {
            for (int m : mults) {
                FieldUtils.safeMultiply(v, m);
            }
        }
    }
}