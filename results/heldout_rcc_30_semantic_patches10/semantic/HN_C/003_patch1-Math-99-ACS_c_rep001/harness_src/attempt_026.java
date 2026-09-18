package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int[] values = new int[] {
            a,
            b,
            c,
            d,
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE - 1,
            Integer.MIN_VALUE + 1,
            a & ~1,
            b & ~1,
            c | 1,
            d | 1,
            -a,
            -b,
            a / 2,
            b / 2
        };

        int x = values[data.consumeInt(0, values.length - 1)];
        int y = values[data.consumeInt(0, values.length - 1)];

        MathUtils.gcd(x, y);
        MathUtils.lcm(x, y);

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], y);
            MathUtils.gcd(x, values[i]);
            MathUtils.lcm(values[i], y);
            MathUtils.lcm(x, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = i; j < values.length; j++) {
                if (data.consumeBoolean()) {
                    MathUtils.gcd(values[i], values[j]);
                } else {
                    MathUtils.lcm(values[i], values[j]);
                }
            }
        }

        MathUtils.gcd(a, 0);
        MathUtils.gcd(0, b);
        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, 1);
        MathUtils.gcd(1, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, -1);
        MathUtils.gcd(-1, Integer.MIN_VALUE);

        MathUtils.lcm(a, 0);
        MathUtils.lcm(0, b);
        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, -1);
        MathUtils.lcm(-1, Integer.MIN_VALUE);
    }
}