package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int smallA = data.consumeInt(-4, 4);
        int smallB = data.consumeInt(-4, 4);

        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        int fromBytes1 = 0;
        for (int i = 0; i < bytes1.length; i++) {
            fromBytes1 = (fromBytes1 * 257) ^ (bytes1[i] & 0xff);
        }

        int fromBytes2 = 0;
        for (int i = 0; i < bytes2.length; i++) {
            fromBytes2 = (fromBytes2 * 131) + bytes2[i];
        }

        int fromString1 = s1.hashCode();
        int fromString2 = s2.hashCode();

        int[] values = new int[] {
            a, b, c, d,
            smallA, smallB,
            by,
            fromBytes1, fromBytes2,
            fromString1, fromString2,
            0, 1, -1, 2, -2,
            Integer.MAX_VALUE, Integer.MIN_VALUE,
            Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1,
            1073741824, -1073741824
        };

        if (flip) {
            for (int i = 0; i < values.length; i++) {
                values[i] = ~values[i];
            }
        }

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], values[i]);
            MathUtils.gcd(values[i], 0);
            MathUtils.gcd(0, values[i]);
            MathUtils.lcm(values[i], values[i]);
            MathUtils.lcm(values[i], 0);
            MathUtils.lcm(0, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
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

                int x2 = x >> (j & 31);
                int y2 = y << (i & 7);
                MathUtils.gcd(x2, y2);
                MathUtils.lcm(x2, y2);
            }
        }

        int pairMix1 = a ^ b;
        int pairMix2 = c + d;
        int pairMix3 = fromBytes1 - fromString1;
        int pairMix4 = fromBytes2 ^ fromString2;

        MathUtils.gcd(pairMix1, pairMix2);
        MathUtils.gcd(pairMix3, pairMix4);
        MathUtils.gcd(pairMix1, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, pairMix2);
        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);

        MathUtils.lcm(pairMix1, pairMix2);
        MathUtils.lcm(pairMix3, pairMix4);
        MathUtils.lcm(pairMix1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, pairMix2);
        MathUtils.lcm(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
}