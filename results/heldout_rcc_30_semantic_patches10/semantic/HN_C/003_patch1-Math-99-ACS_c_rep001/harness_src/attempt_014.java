package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int[] specials = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            1073741824,
            -1073741824,
            2147483646,
            -2147483647,
            a,
            b,
            c,
            d
        };

        MathUtils.gcd(a, b);
        MathUtils.lcm(a, b);

        MathUtils.gcd(b, a);
        MathUtils.lcm(b, a);

        MathUtils.gcd(a, a);
        MathUtils.lcm(a, a);

        MathUtils.gcd(b, b);
        MathUtils.lcm(b, b);

        MathUtils.gcd(a, 0);
        MathUtils.gcd(0, a);
        MathUtils.gcd(b, 0);
        MathUtils.gcd(0, b);

        MathUtils.lcm(a, 0);
        MathUtils.lcm(0, a);
        MathUtils.lcm(b, 0);
        MathUtils.lcm(0, b);

        MathUtils.gcd(-a, b);
        MathUtils.gcd(a, -b);
        MathUtils.gcd(-a, -b);
        MathUtils.lcm(-a, b);
        MathUtils.lcm(a, -b);
        MathUtils.lcm(-a, -b);

        int limit1 = data.consumeInt(0, specials.length);
        int limit2 = data.consumeInt(0, specials.length);

        for (int i = 0; i < limit1; i++) {
            int x = specials[i];
            MathUtils.gcd(x, a);
            MathUtils.gcd(a, x);
            MathUtils.lcm(x, a);
            MathUtils.lcm(a, x);

            MathUtils.gcd(x, b);
            MathUtils.gcd(b, x);
            MathUtils.lcm(x, b);
            MathUtils.lcm(b, x);
        }

        for (int i = 0; i < limit2; i++) {
            for (int j = 0; j < limit2; j++) {
                int x = specials[i];
                int y = specials[j];
                if (data.consumeBoolean()) {
                    MathUtils.gcd(x, y);
                } else {
                    MathUtils.lcm(x, y);
                }
            }
        }

        int mode = data.consumeInt(0, 7);
        switch (mode) {
            case 0:
                MathUtils.gcd(Integer.MIN_VALUE, 0);
                break;
            case 1:
                MathUtils.gcd(0, Integer.MIN_VALUE);
                break;
            case 2:
                MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
                break;
            case 3:
                MathUtils.gcd(1073741824, Integer.MIN_VALUE);
                break;
            case 4:
                MathUtils.gcd(Integer.MIN_VALUE, 1073741824);
                break;
            case 5:
                MathUtils.lcm(Integer.MIN_VALUE, -1);
                break;
            case 6:
                MathUtils.lcm(Integer.MIN_VALUE, 2);
                break;
            default:
                MathUtils.lcm(1073741824, 2);
                break;
        }

        int extra = data.consumeInt(0, 8);
        for (int i = 0; i < extra; i++) {
            int x = data.consumeInt();
            int y = data.consumeInt();
            if (data.consumeBoolean()) {
                MathUtils.gcd(x, y);
            } else {
                MathUtils.lcm(x, y);
            }
        }
    }
}