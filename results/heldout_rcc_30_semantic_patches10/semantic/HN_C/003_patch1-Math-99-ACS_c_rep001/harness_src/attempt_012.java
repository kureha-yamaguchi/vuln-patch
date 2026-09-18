package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int selector = data.consumeInt(0, 7);

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
            1073741824,
            -1073741824,
            2147483646,
            -2147483647
        };

        switch (selector) {
            case 0:
                MathUtils.gcd(a, b);
                MathUtils.lcm(a, b);
                break;

            case 1:
                for (int x : values) {
                    MathUtils.gcd(x, b);
                    MathUtils.gcd(a, x);
                    MathUtils.lcm(x, b);
                    MathUtils.lcm(a, x);
                }
                break;

            case 2:
                for (int x : values) {
                    for (int y : values) {
                        MathUtils.gcd(x, y);
                        MathUtils.lcm(x, y);
                    }
                }
                break;

            case 3:
                MathUtils.gcd(a, a);
                MathUtils.gcd(a, -a);
                MathUtils.gcd(-a, a);
                MathUtils.gcd(0, a);
                MathUtils.gcd(a, 0);

                MathUtils.lcm(a, a);
                MathUtils.lcm(a, -a);
                MathUtils.lcm(-a, a);
                MathUtils.lcm(0, a);
                MathUtils.lcm(a, 0);
                break;

            case 4:
                int x1 = (data.consumeBoolean() ? a : -a);
                int y1 = (data.consumeBoolean() ? b : -b);
                if (data.consumeBoolean()) {
                    x1 = x1 & ~1;
                } else {
                    x1 = x1 | 1;
                }
                if (data.consumeBoolean()) {
                    y1 = y1 & ~1;
                } else {
                    y1 = y1 | 1;
                }
                MathUtils.gcd(x1, y1);
                MathUtils.lcm(x1, y1);
                break;

            case 5:
                int pow = data.consumeInt(0, 31);
                int twoPow = (pow == 31) ? Integer.MIN_VALUE : (1 << pow);
                MathUtils.gcd(twoPow, twoPow);
                MathUtils.gcd(twoPow, 0);
                MathUtils.gcd(0, twoPow);
                MathUtils.lcm(twoPow, 1);
                MathUtils.lcm(twoPow, -1);
                break;

            case 6:
                int r1 = MathUtils.gcd(a, b);
                int r2 = MathUtils.gcd(b, a);
                int r3 = MathUtils.gcd(c, d);
                int r4 = MathUtils.gcd(r1, r3);
                MathUtils.lcm(r2, r4);
                MathUtils.gcd(MathUtils.lcm(a, c), MathUtils.lcm(b, d));
                break;

            default:
                int count = data.consumeInt(0, 16);
                int acc = a;
                for (int i = 0; i < count; i++) {
                    int v;
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            v = data.consumeInt();
                            break;
                        case 1:
                            v = 0;
                            break;
                        case 2:
                            v = Integer.MIN_VALUE;
                            break;
                        case 3:
                            v = Integer.MAX_VALUE;
                            break;
                        case 4:
                            v = data.consumeBoolean() ? 1 : -1;
                            break;
                        default:
                            v = data.consumeBoolean() ? (data.consumeInt() & ~1) : (data.consumeInt() | 1);
                            break;
                    }
                    acc = MathUtils.gcd(acc, v);
                    MathUtils.lcm(acc, v);
                }
                MathUtils.gcd(acc, b);
                MathUtils.lcm(acc, c);
                break;
        }
    }
}