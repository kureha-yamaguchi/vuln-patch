package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long fuzzLong = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        int fuzzInt = data.consumeInt();
        int selector = data.consumeInt(0, 15);

        long[] vals1 = new long[] {
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
            fuzzLong,
            -fuzzLong,
            fuzzLong >>> 1,
            ~fuzzLong,
            (long) fuzzInt
        };

        int[] vals2 = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            3,
            -3,
            fuzzInt,
            ~fuzzInt,
            selector == 0 ? -1 : selector == 1 ? 0 : selector == 2 ? 1 : fuzzInt
        };

        int outer = data.consumeInt(1, 8);
        for (int i = 0; i < outer; i++) {
            long val1 = vals1[data.consumeInt(0, vals1.length - 1)];
            int val2 = vals2[data.consumeInt(0, vals2.length - 1)];
            FieldUtils.safeMultiply(val1, val2);
        }

        byte[] extra = data.consumeBytes(Math.min(16, data.remainingBytes()));
        for (int i = 0; i < extra.length; i++) {
            long val1 = vals1[i % vals1.length] + extra[i];
            int val2 = vals2[(i + selector) % vals2.length];
            FieldUtils.safeMultiply(val1, val2);
        }

        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        long strLong = ((long) s1.length() << 32) ^ s2.length();
        int strInt = s1.hashCode() ^ s2.hashCode();
        FieldUtils.safeMultiply(strLong, strInt == 0 ? 1 : strInt);

        if (data.consumeBoolean()) {
            String tail = data.consumeRemainingAsString();
            long tailLong = tail.length() == 0 ? 0L : (((long) tail.charAt(0)) << 48) ^ tail.hashCode();
            int tailInt = tail.length() == 0 ? 0 : tail.charAt(tail.length() - 1);
            FieldUtils.safeMultiply(tailLong, tailInt);
        } else {
            byte[] tail = data.consumeRemainingAsBytes();
            long tailLong = 0L;
            for (int i = 0; i < tail.length && i < 8; i++) {
                tailLong = (tailLong << 8) | (tail[i] & 0xffL);
            }
            int tailInt = tail.length == 0 ? 0 : tail[tail.length - 1];
            FieldUtils.safeMultiply(tailLong, tailInt);
        }
    }
}