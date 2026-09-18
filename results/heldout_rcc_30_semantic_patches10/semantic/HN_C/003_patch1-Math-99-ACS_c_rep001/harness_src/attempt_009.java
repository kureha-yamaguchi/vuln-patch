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
        boolean swap = data.consumeBoolean();
        boolean useNegations = data.consumeBoolean();

        int[] values = new int[] {
            a,
            b,
            c,
            d,
            smallA,
            smallB,
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
            536870912,
            -536870912
        };

        int x = swap ? b : a;
        int y = swap ? a : b;

        MathUtils.gcd(x, y);
        MathUtils.gcd(y, x);
        MathUtils.lcm(x, y);
        MathUtils.lcm(y, x);

        if (useNegations) {
            MathUtils.gcd(-x, y);
            MathUtils.gcd(x, -y);
            MathUtils.gcd(-x, -y);
            MathUtils.lcm(-x, y);
            MathUtils.lcm(x, -y);
            MathUtils.lcm(-x, -y);
        }

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
                int p = values[i];
                int q = values[j];

                MathUtils.gcd(p, q);
                MathUtils.gcd(q, p);
                MathUtils.lcm(p, q);
                MathUtils.lcm(q, p);
            }
        }

        int mixed1 = a ^ c;
        int mixed2 = b + d;
        int mixed3 = (smallA == 0) ? Integer.MIN_VALUE : (a / smallA);
        int mixed4 = (smallB == 0) ? Integer.MAX_VALUE : (b / smallB);

        MathUtils.gcd(mixed1, mixed2);
        MathUtils.gcd(mixed3, mixed4);
        MathUtils.lcm(mixed1, mixed2);
        MathUtils.lcm(mixed3, mixed4);
    }
}