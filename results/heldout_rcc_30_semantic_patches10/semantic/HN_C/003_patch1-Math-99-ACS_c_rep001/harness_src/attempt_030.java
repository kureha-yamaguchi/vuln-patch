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
            Integer.MAX_VALUE - 1,
            Integer.MIN_VALUE + 1,
            1073741824,
            -1073741824,
            46340,
            -46340
        };

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

        for (int s : specials) {
            MathUtils.gcd(a, s);
            MathUtils.gcd(s, a);
            MathUtils.gcd(b, s);
            MathUtils.gcd(s, b);

            MathUtils.lcm(a, s);
            MathUtils.lcm(s, a);
            MathUtils.lcm(b, s);
            MathUtils.lcm(s, b);
        }

        int selector = data.consumeInt(0, 15);
        switch (selector) {
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
                MathUtils.gcd(Integer.MIN_VALUE, 1);
                break;
            case 4:
                MathUtils.gcd(1, Integer.MIN_VALUE);
                break;
            case 5:
                MathUtils.gcd(Integer.MIN_VALUE, 2);
                break;
            case 6:
                MathUtils.gcd(2, Integer.MIN_VALUE);
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
                MathUtils.lcm(Integer.MIN_VALUE, 2);
                break;
            case 12:
                MathUtils.lcm(2, Integer.MIN_VALUE);
                break;
            case 13:
                MathUtils.lcm(Integer.MIN_VALUE, Integer.MIN_VALUE);
                break;
            case 14:
                MathUtils.gcd(c, d);
                break;
            default:
                MathUtils.lcm(c, d);
                break;
        }

        if (data.consumeBoolean()) {
            int x = a;
            int y = b;
            for (int i = 0; i < 4; i++) {
                MathUtils.gcd(x, y);
                MathUtils.lcm(x, y);
                x = (x >>> 1) ^ c;
                y = (y << 1) ^ d;
            }
        }

        if (data.remainingBytes() > 0) {
            byte[] extra = data.consumeRemainingAsBytes();
            for (byte value : extra) {
                int v = value;
                MathUtils.gcd(v, a);
                MathUtils.gcd(b, v);
                MathUtils.lcm(v, c);
                MathUtils.lcm(d, v);
            }
        }
    }
}