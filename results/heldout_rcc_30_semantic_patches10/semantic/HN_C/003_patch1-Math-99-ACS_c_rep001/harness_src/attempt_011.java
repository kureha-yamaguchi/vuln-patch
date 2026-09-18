package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int smallA = data.consumeInt(-8, 8);
        int smallB = data.consumeInt(-8, 8);

        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(32);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        int h1 = s1.hashCode();
        int h2 = s2.hashCode();

        int acc1 = 0;
        for (int i = 0; i < bytes1.length; i++) {
            acc1 = (acc1 * 33) ^ bytes1[i];
        }

        int acc2 = 0;
        for (int i = 0; i < bytes2.length; i++) {
            acc2 = (acc2 * 257) + bytes2[i];
        }

        if (flip) {
            acc1 = -acc1;
        } else {
            acc2 = -acc2;
        }

        int[] values = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE - 1,
            Integer.MIN_VALUE + 1,
            1073741824,
            -1073741824,
            a,
            b,
            c,
            d,
            smallA,
            smallB,
            by,
            -by,
            h1,
            h2,
            acc1,
            acc2,
            a ^ b,
            c ^ d,
            a + smallA,
            b - smallB
        };

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
            }
        }

        int chained1 = MathUtils.gcd(a, b);
        int chained2 = MathUtils.gcd(c, d);
        int chained3 = MathUtils.gcd(chained1, chained2);
        MathUtils.lcm(chained1, chained2);
        MathUtils.gcd(chained3, smallA);
        MathUtils.lcm(chained3, smallB);

        if (bytes1.length > 0 || bytes2.length > 0 || s1.length() > 0 || s2.length() > 0) {
            int mix1 = a ^ h1 ^ acc1;
            int mix2 = b ^ h2 ^ acc2;
            MathUtils.gcd(mix1, mix2);
            MathUtils.lcm(mix1, mix2);
        }
    }
}