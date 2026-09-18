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

        int small1 = data.consumeInt(-4, 4);
        int small2 = data.consumeInt(-4, 4);
        int edgeish = data.consumeInt(-1024, 1024);

        int[] values = new int[] {
            a,
            b,
            c,
            d,
            by,
            small1,
            small2,
            edgeish,
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
            a | b,
            a & b,
            ~a,
            ~b,
            flip ? -a : a,
            flip ? -b : b,
            a == Integer.MIN_VALUE ? Integer.MIN_VALUE : Math.abs(a),
            b == Integer.MIN_VALUE ? Integer.MIN_VALUE : -Math.abs(b),
            a << (small1 & 31),
            b << (small2 & 31),
            a >> (small1 & 31),
            b >> (small2 & 31)
        };

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], values[i]);
            MathUtils.lcm(values[i], values[i]);
            MathUtils.gcd(values[i], 0);
            MathUtils.gcd(0, values[i]);
            MathUtils.lcm(values[i], 0);
            MathUtils.lcm(0, values[i]);
            MathUtils.gcd(values[i], 1);
            MathUtils.gcd(1, values[i]);
            MathUtils.gcd(values[i], -1);
            MathUtils.gcd(-1, values[i]);
            MathUtils.lcm(values[i], 1);
            MathUtils.lcm(1, values[i]);
            MathUtils.lcm(values[i], -1);
            MathUtils.lcm(-1, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = i; j < values.length; j++) {
                int x = values[i];
                int y = values[j];

                MathUtils.gcd(x, y);
                MathUtils.gcd(y, x);
                MathUtils.lcm(x, y);
                MathUtils.lcm(y, x);

                MathUtils.gcd(-x, y);
                MathUtils.gcd(x, -y);
                MathUtils.gcd(-x, -y);

                MathUtils.lcm(-x, y);
                MathUtils.lcm(x, -y);
                MathUtils.lcm(-x, -y);
            }
        }

        int[][] targeted = new int[][] {
            {Integer.MIN_VALUE, 0},
            {0, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, 1},
            {1, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, -1},
            {-1, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, 2},
            {2, Integer.MIN_VALUE},
            {1073741824, 1073741824},
            {-1073741824, -1073741824},
            {Integer.MAX_VALUE, Integer.MIN_VALUE + 1},
            {a, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, b},
            {a, b},
            {a, c},
            {b, d},
            {a ^ c, b ^ d}
        };

        for (int i = 0; i < targeted.length; i++) {
            int x = targeted[i][0];
            int y = targeted[i][1];
            MathUtils.gcd(x, y);
            MathUtils.lcm(x, y);
        }
    }
}