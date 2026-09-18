package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int selectorA = data.consumeInt(0, 15);
        int selectorB = data.consumeInt(0, 15);

        int x;
        switch (selectorA) {
            case 0:
                x = a;
                break;
            case 1:
                x = 0;
                break;
            case 2:
                x = 1;
                break;
            case 3:
                x = -1;
                break;
            case 4:
                x = Integer.MAX_VALUE;
                break;
            case 5:
                x = Integer.MIN_VALUE;
                break;
            case 6:
                x = a & Integer.MAX_VALUE;
                break;
            case 7:
                x = -(a & Integer.MAX_VALUE);
                break;
            case 8:
                x = a | 1;
                break;
            case 9:
                x = a & ~1;
                break;
            case 10:
                x = 1 << data.consumeInt(0, 30);
                break;
            case 11:
                x = -(1 << data.consumeInt(0, 30));
                break;
            case 12:
                x = (1 << data.consumeInt(0, 30)) - 1;
                break;
            case 13:
                x = -((1 << data.consumeInt(0, 30)) - 1);
                break;
            case 14:
                x = c;
                break;
            default:
                x = d;
                break;
        }

        int y;
        switch (selectorB) {
            case 0:
                y = b;
                break;
            case 1:
                y = 0;
                break;
            case 2:
                y = 1;
                break;
            case 3:
                y = -1;
                break;
            case 4:
                y = Integer.MAX_VALUE;
                break;
            case 5:
                y = Integer.MIN_VALUE;
                break;
            case 6:
                y = b & Integer.MAX_VALUE;
                break;
            case 7:
                y = -(b & Integer.MAX_VALUE);
                break;
            case 8:
                y = b | 1;
                break;
            case 9:
                y = b & ~1;
                break;
            case 10:
                y = 1 << data.consumeInt(0, 30);
                break;
            case 11:
                y = -(1 << data.consumeInt(0, 30));
                break;
            case 12:
                y = (1 << data.consumeInt(0, 30)) - 1;
                break;
            case 13:
                y = -((1 << data.consumeInt(0, 30)) - 1);
                break;
            case 14:
                y = c;
                break;
            default:
                y = d;
                break;
        }

        MathUtils.gcd(x, y);
        MathUtils.gcd(y, x);
        MathUtils.gcd(-x, y);
        MathUtils.gcd(x, -y);
        MathUtils.gcd(-x, -y);

        MathUtils.lcm(x, y);
        MathUtils.lcm(y, x);
        MathUtils.lcm(-x, y);
        MathUtils.lcm(x, -y);
        MathUtils.lcm(-x, -y);

        int shift = data.consumeInt(0, 30);
        int pow2 = 1 << shift;
        int negPow2 = -pow2;

        MathUtils.gcd(pow2, negPow2);
        MathUtils.lcm(pow2, negPow2);

        if (data.consumeBoolean()) {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
        } else {
            MathUtils.gcd(0, Integer.MIN_VALUE);
        }

        if (data.consumeBoolean()) {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } else {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
        }
    }
}