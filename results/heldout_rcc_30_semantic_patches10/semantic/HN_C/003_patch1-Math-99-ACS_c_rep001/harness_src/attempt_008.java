package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int[] vals = new int[] {
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
            1073741824,
            -1073741824,
            46340,
            -46340
        };

        int i1 = (a & Integer.MAX_VALUE) % vals.length;
        int i2 = (b & Integer.MAX_VALUE) % vals.length;
        int i3 = (c & Integer.MAX_VALUE) % vals.length;
        int i4 = (d & Integer.MAX_VALUE) % vals.length;

        MathUtils.gcd(a, b);
        MathUtils.gcd(b, a);
        MathUtils.gcd(a, a);
        MathUtils.gcd(b, b);
        MathUtils.gcd(a, 0);
        MathUtils.gcd(0, b);
        MathUtils.gcd(-a, b);
        MathUtils.gcd(a, -b);
        MathUtils.gcd(-a, -b);

        MathUtils.lcm(a, b);
        MathUtils.lcm(b, a);
        MathUtils.lcm(a, 0);
        MathUtils.lcm(0, b);
        MathUtils.lcm(-a, b);
        MathUtils.lcm(a, -b);
        MathUtils.lcm(-a, -b);

        MathUtils.gcd(vals[i1], vals[i2]);
        MathUtils.gcd(vals[i2], vals[i1]);
        MathUtils.gcd(vals[i3], vals[i4]);
        MathUtils.gcd(vals[i4], vals[i3]);

        MathUtils.lcm(vals[i1], vals[i2]);
        MathUtils.lcm(vals[i2], vals[i1]);
        MathUtils.lcm(vals[i3], vals[i4]);
        MathUtils.lcm(vals[i4], vals[i3]);

        for (int x : vals) {
            MathUtils.gcd(x, 0);
            MathUtils.gcd(0, x);
            MathUtils.gcd(x, x);
            MathUtils.gcd(x, -x);
            MathUtils.lcm(x, 0);
            MathUtils.lcm(0, x);
            MathUtils.lcm(x, x);
            MathUtils.lcm(x, -x);
        }

        for (int x : vals) {
            for (int y : vals) {
                MathUtils.gcd(x, y);
                MathUtils.lcm(x, y);
            }
        }
    }
}