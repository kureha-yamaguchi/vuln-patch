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
            a >> 1,
            b >> 1,
            c << 1,
            d << 1,
            -a,
            -b,
            a & b,
            a | b,
            a ^ b
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
            for (int j = 0; j < values.length; j++) {
                int x = values[i];
                int y = values[j];

                MathUtils.gcd(x, y);
                MathUtils.gcd(y, x);
                MathUtils.lcm(x, y);
                MathUtils.lcm(y, x);
            }
        }

        int p = data.consumeBoolean() ? Integer.MIN_VALUE : a;
        int q = data.consumeBoolean() ? Integer.MIN_VALUE : b;
        MathUtils.gcd(p, q);
        MathUtils.lcm(p, q);

        int even1 = data.consumeInt() & ~1;
        int even2 = data.consumeInt() & ~1;
        MathUtils.gcd(even1, even2);
        MathUtils.lcm(even1, even2);

        int pow2ish = 1 << data.consumeInt(0, 30);
        int signedPow2ish = data.consumeBoolean() ? pow2ish : -pow2ish;
        MathUtils.gcd(signedPow2ish, even1);
        MathUtils.gcd(signedPow2ish, even2);
        MathUtils.lcm(signedPow2ish, even1);
        MathUtils.lcm(signedPow2ish, even2);

        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
}