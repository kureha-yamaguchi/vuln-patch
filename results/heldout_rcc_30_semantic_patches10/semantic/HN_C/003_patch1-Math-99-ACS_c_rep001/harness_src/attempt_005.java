package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();
        boolean flipA = data.consumeBoolean();
        boolean flipB = data.consumeBoolean();
        int selector1 = data.consumeInt(0, 15);
        int selector2 = data.consumeInt(0, 15);

        int[] seeds = new int[] {
            a,
            b,
            c,
            d,
            flipA ? -a : a,
            flipB ? -b : b,
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
            a & b,
            a | b,
            a ^ b,
            a + 1,
            a - 1,
            b + 1,
            b - 1
        };

        int x = seeds[selector1];
        int y = seeds[selector2];

        MathUtils.gcd(a, b);
        MathUtils.gcd(b, a);
        MathUtils.gcd(a, 0);
        MathUtils.gcd(0, b);
        MathUtils.gcd(x, y);
        MathUtils.gcd(-x, y);
        MathUtils.gcd(x, -y);
        MathUtils.gcd(-x, -y);
        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, 1);
        MathUtils.gcd(1, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, -1);
        MathUtils.gcd(-1, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MAX_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE + 1, Integer.MIN_VALUE);
        MathUtils.gcd(1073741824, 1073741824);
        MathUtils.gcd(-1073741824, -1073741824);

        MathUtils.lcm(a, b);
        MathUtils.lcm(b, a);
        MathUtils.lcm(a, 0);
        MathUtils.lcm(0, b);
        MathUtils.lcm(x, y);
        MathUtils.lcm(-x, y);
        MathUtils.lcm(x, -y);
        MathUtils.lcm(-x, -y);
        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, -1);
        MathUtils.lcm(-1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MAX_VALUE, 2);
        MathUtils.lcm(2, Integer.MAX_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, 2);
        MathUtils.lcm(2, Integer.MIN_VALUE);
        MathUtils.lcm(1073741824, 2);
        MathUtils.lcm(-1073741824, 2);
    }
}