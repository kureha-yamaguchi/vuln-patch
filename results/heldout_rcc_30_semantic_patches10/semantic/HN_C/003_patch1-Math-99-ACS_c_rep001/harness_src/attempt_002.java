package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int selector = data.consumeInt(0, 15);

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
            a,
            b,
            c,
            d,
            -a,
            -b,
            a >> 1,
            b >> 1,
            a << 1,
            b << 1,
            a & b,
            a | b,
            a ^ b,
            a + b,
            a - b,
            b - a,
            a == 0 ? Integer.MIN_VALUE : a % 1024,
            b == 0 ? Integer.MAX_VALUE : b % 1024
        };

        int x = values[data.consumeInt(0, values.length - 1)];
        int y = values[data.consumeInt(0, values.length - 1)];

        switch (selector) {
            case 0:
                MathUtils.gcd(x, y);
                break;
            case 1:
                MathUtils.lcm(x, y);
                break;
            case 2:
                MathUtils.gcd(Integer.MIN_VALUE, 0);
                break;
            case 3:
                MathUtils.gcd(0, Integer.MIN_VALUE);
                break;
            case 4:
                MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
                break;
            case 5:
                MathUtils.gcd(Integer.MIN_VALUE, x);
                break;
            case 6:
                MathUtils.gcd(x, Integer.MIN_VALUE);
                break;
            case 7:
                MathUtils.lcm(Integer.MIN_VALUE, 1);
                break;
            case 8:
                MathUtils.lcm(1, Integer.MIN_VALUE);
                break;
            case 9:
                MathUtils.lcm(Integer.MIN_VALUE, -1);
                break;
            case 10:
                MathUtils.lcm(-1, Integer.MIN_VALUE);
                break;
            case 11:
                MathUtils.lcm(x, y);
                MathUtils.gcd(x, y);
                break;
            case 12:
                MathUtils.gcd(0, y);
                MathUtils.gcd(x, 0);
                break;
            case 13:
                MathUtils.lcm(0, y);
                MathUtils.lcm(x, 0);
                break;
            case 14:
                MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE / 2);
                MathUtils.lcm(Integer.MIN_VALUE / 2, 2);
                break;
            default:
                for (int i = 0; i < 4; i++) {
                    int p = values[data.consumeInt(0, values.length - 1)];
                    int q = values[data.consumeInt(0, values.length - 1)];
                    MathUtils.gcd(p, q);
                    MathUtils.lcm(p, q);
                }
                break;
        }
    }
}