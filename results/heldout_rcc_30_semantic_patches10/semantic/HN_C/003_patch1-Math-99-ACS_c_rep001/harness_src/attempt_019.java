package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int small = data.consumeInt(-8, 8);
        int smallNonNegative = data.consumeInt(0, 32);
        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] buf1 = data.consumeBytes(16);
        byte[] buf2 = data.consumeRemainingAsBytes();

        int fromBuf1 = 0;
        for (int i = 0; i < buf1.length; i++) {
            fromBuf1 = (fromBuf1 << 5) ^ (buf1[i] & 0xff) ^ i;
        }

        int fromBuf2 = 0;
        for (int i = 0; i < buf2.length; i++) {
            fromBuf2 = (fromBuf2 << 3) ^ (buf2[i] & 0xff) ^ (i * 17);
        }

        int fromStrings1 = s1.hashCode();
        int fromStrings2 = s2.hashCode();

        int[] vals = new int[] {
            a,
            b,
            c,
            small,
            smallNonNegative,
            by,
            -by,
            fromStrings1,
            fromStrings2,
            fromBuf1,
            fromBuf2,
            buf1.length,
            -buf1.length,
            buf2.length,
            -buf2.length,
            a ^ b,
            a + b,
            a - b,
            b - a,
            a | c,
            b & c,
            flip ? Integer.MIN_VALUE : Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            1073741824,
            -1073741824
        };

        MathUtils.gcd(a, b);
        MathUtils.gcd(b, a);
        MathUtils.gcd(a, 0);
        MathUtils.gcd(0, b);
        MathUtils.gcd(a, a);
        MathUtils.gcd(a, -a);
        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, 1);
        MathUtils.gcd(1, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MAX_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(small, smallNonNegative);
        MathUtils.gcd(fromStrings1, fromStrings2);
        MathUtils.gcd(fromBuf1, fromBuf2);

        MathUtils.lcm(a, b);
        MathUtils.lcm(b, a);
        MathUtils.lcm(a, 0);
        MathUtils.lcm(0, b);
        MathUtils.lcm(a, 1);
        MathUtils.lcm(a, -1);
        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, 2);
        MathUtils.lcm(2, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MAX_VALUE, 2);
        MathUtils.lcm(small, smallNonNegative);
        MathUtils.lcm(fromStrings1, fromStrings2);
        MathUtils.lcm(fromBuf1, fromBuf2);

        int limit = data.remainingBytes() % vals.length;
        if (limit < 8) {
            limit = 8;
        }

        for (int i = 0; i < limit; i++) {
            int x = vals[i];
            int y = vals[(i + 1) % vals.length];
            MathUtils.gcd(x, y);
            MathUtils.gcd(y, x);
            MathUtils.lcm(x, y);

            int z = vals[(i + 2) % vals.length];
            MathUtils.gcd(x, z);
            MathUtils.lcm(y, z);
        }
    }
}