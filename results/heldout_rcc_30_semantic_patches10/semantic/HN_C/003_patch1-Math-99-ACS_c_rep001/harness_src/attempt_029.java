package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int small1 = data.consumeInt(-8, 8);
        int small2 = data.consumeInt(-8, 8);

        int[] values = new int[] {
            a,
            b,
            c,
            d,
            small1,
            small2,
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE - 1,
            Integer.MIN_VALUE + 1,
            a ^ b,
            a + b,
            a - b,
            b - a,
            a | 1,
            b | 1,
            a & ~1,
            b & ~1,
            a / 2,
            b / 2
        };

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], 0);
            MathUtils.gcd(0, values[i]);
            MathUtils.lcm(values[i], 0);
            MathUtils.lcm(0, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                int x = values[i];
                int y = values[j];

                int g = MathUtils.gcd(x, y);
                int l = MathUtils.lcm(x, y);

                MathUtils.gcd(y, x);
                MathUtils.lcm(y, x);

                MathUtils.gcd(g, x);
                MathUtils.gcd(g, y);
                MathUtils.lcm(g, (y == 0) ? 1 : y);
                MathUtils.lcm((x == 0) ? 1 : x, g);

                MathUtils.gcd(l, g);
                MathUtils.gcd(l, x);
                MathUtils.gcd(l, y);

                if (g != 0) {
                    MathUtils.gcd(x / g, y / g);
                }

                if (i == j) {
                    MathUtils.gcd(x, x);
                    MathUtils.lcm(x, x);
                    MathUtils.gcd(x, -x);
                    MathUtils.lcm(x, -x);
                }
            }
        }

        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, small1);
        MathUtils.gcd(small1, Integer.MIN_VALUE);

        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, -1);
        MathUtils.lcm(-1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, 2);
        MathUtils.lcm(2, Integer.MIN_VALUE);
    }
}