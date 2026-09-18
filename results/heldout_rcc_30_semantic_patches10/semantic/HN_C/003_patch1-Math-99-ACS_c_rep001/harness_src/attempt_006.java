package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();

        int[] special = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            1073741824,
            -1073741824,
            2147483646,
            -2147483647,
            a,
            b
        };

        MathUtils.gcd(a, b);
        MathUtils.gcd(b, a);
        MathUtils.gcd(a, a);
        MathUtils.gcd(b, b);
        MathUtils.gcd(a, 0);
        MathUtils.gcd(0, a);
        MathUtils.gcd(b, 0);
        MathUtils.gcd(0, b);

        MathUtils.lcm(a, b);
        MathUtils.lcm(b, a);
        MathUtils.lcm(a, 0);
        MathUtils.lcm(0, a);
        MathUtils.lcm(b, 0);
        MathUtils.lcm(0, b);

        for (int i = 0; i < special.length; i++) {
            MathUtils.gcd(a, special[i]);
            MathUtils.gcd(special[i], a);
            MathUtils.gcd(b, special[i]);
            MathUtils.gcd(special[i], b);

            MathUtils.lcm(a, special[i]);
            MathUtils.lcm(special[i], a);
            MathUtils.lcm(b, special[i]);
            MathUtils.lcm(special[i], b);
        }

        int pairCount = data.consumeInt(0, 8);
        for (int i = 0; i < pairCount; i++) {
            int x;
            int y;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    x = data.consumeInt();
                    y = data.consumeInt();
                    break;
                case 1:
                    x = special[data.consumeInt(0, special.length - 1)];
                    y = data.consumeInt();
                    break;
                case 2:
                    x = data.consumeInt();
                    y = special[data.consumeInt(0, special.length - 1)];
                    break;
                case 3:
                    x = special[data.consumeInt(0, special.length - 1)];
                    y = special[data.consumeInt(0, special.length - 1)];
                    break;
                case 4:
                    x = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                    y = data.consumeBoolean() ? 0 : (data.consumeBoolean() ? 1 : -1);
                    break;
                default:
                    int shift = data.consumeInt(0, 30);
                    int base = 1 << shift;
                    x = data.consumeBoolean() ? base : -base;
                    y = data.consumeBoolean() ? base : -base;
                    break;
            }

            MathUtils.gcd(x, y);
            MathUtils.gcd(y, x);
            MathUtils.lcm(x, y);
            MathUtils.lcm(y, x);
        }
    }
}