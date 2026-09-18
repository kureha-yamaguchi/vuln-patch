package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(32);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        int len1 = s1.length();
        int len2 = s2.length();
        int blen1 = bytes1.length;
        int blen2 = bytes2.length;

        int[] values = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            1 << 30,
            -(1 << 30),
            by,
            -by,
            len1,
            -len1,
            len2,
            -len2,
            blen1,
            -blen1,
            blen2,
            -blen2,
            a,
            b,
            c,
            d,
            a ^ b,
            a | b,
            a & b,
            a + b,
            a - b,
            b - a,
            c + d,
            c - d,
            a << (Math.abs(by) & 31),
            a >> (Math.abs(by) & 31),
            b << (len1 & 31),
            b >> (len2 & 31),
            flip ? a : -a,
            flip ? b : -b
        };

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], values[i]);
            MathUtils.lcm(values[i], values[i]);
            MathUtils.gcd(values[i], 0);
            MathUtils.gcd(0, values[i]);
            MathUtils.lcm(values[i], 0);
            MathUtils.lcm(0, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = i; j < values.length; j++) {
                int x = values[i];
                int y = values[j];

                MathUtils.gcd(x, y);
                MathUtils.gcd(y, x);
                MathUtils.gcd(-x, y);
                MathUtils.gcd(x, -y);

                MathUtils.lcm(x, y);
                MathUtils.lcm(y, x);
                MathUtils.lcm(-x, y);
                MathUtils.lcm(x, -y);
            }
        }

        int pairCount = Math.min(blen1, blen2);
        for (int i = 0; i < pairCount; i++) {
            int x = bytes1[i];
            int y = bytes2[i];
            MathUtils.gcd(x, y);
            MathUtils.lcm(x, y);
            MathUtils.gcd(x << 8, y << 8);
            MathUtils.lcm(x << 8, y << 8);
        }

        if (blen1 >= 4) {
            int x = ((bytes1[0] & 0xff) << 24)
                  | ((bytes1[1] & 0xff) << 16)
                  | ((bytes1[2] & 0xff) << 8)
                  |  (bytes1[3] & 0xff);
            MathUtils.gcd(x, a);
            MathUtils.lcm(x, b);
        }

        if (blen2 >= 4) {
            int y = ((bytes2[0] & 0xff) << 24)
                  | ((bytes2[1] & 0xff) << 16)
                  | ((bytes2[2] & 0xff) << 8)
                  |  (bytes2[3] & 0xff);
            MathUtils.gcd(c, y);
            MathUtils.lcm(d, y);
        }

        if ((a & 1) == 0 && (b & 1) == 0) {
            MathUtils.gcd(a, b);
        }
        if ((c & 1) != 0 || (d & 1) != 0) {
            MathUtils.gcd(c, d);
        }

        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, 1);
        MathUtils.gcd(1, Integer.MIN_VALUE);

        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, -1);
        MathUtils.lcm(-1, Integer.MIN_VALUE);
    }
}