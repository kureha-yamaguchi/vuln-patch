package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        byte[] raw = data.consumeBytes(8);
        long assembled = 0L;
        for (int i = 0; i < raw.length; i++) {
            assembled = (assembled << 8) | (raw[i] & 0xffL);
        }
        if (raw.length > 0 && (raw[0] & 0x80) != 0) {
            assembled |= (-1L) << (raw.length * 8);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt(-1, 1);
        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] tailBytes = data.consumeRemainingAsBytes();

        long fromInts = (((long) a) << 32) ^ (b & 0xffffffffL);
        long fromByte = by;
        long fromLengths = ((long) s1.length() << 32) | (s2.length() & 0xffffffffL);
        long fromTail = 0L;
        for (int i = 0; i < tailBytes.length && i < 8; i++) {
            fromTail = (fromTail << 8) | (tailBytes[i] & 0xffL);
        }
        if (tailBytes.length > 0 && tailBytes.length <= 8 && (tailBytes[0] & 0x80) != 0) {
            fromTail |= (-1L) << (Math.min(tailBytes.length, 8) * 8);
        }

        long[] vals1 = new long[] {
            0L,
            1L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            assembled,
            fromInts,
            fromByte,
            fromLengths,
            fromTail,
            flip ? -assembled : assembled,
            flip ? ~fromInts : fromInts,
            (long) a,
            (long) b
        };

        int[] vals2 = new int[] {
            -1,
            0,
            1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            a,
            b,
            c,
            by,
            s1.length(),
            -s1.length(),
            s2.length(),
            -s2.length(),
            tailBytes.length,
            -tailBytes.length
        };

        for (long val1 : vals1) {
            for (int val2 : vals2) {
                FieldUtils.safeMultiply(val1, val2);
            }
        }
    }
}