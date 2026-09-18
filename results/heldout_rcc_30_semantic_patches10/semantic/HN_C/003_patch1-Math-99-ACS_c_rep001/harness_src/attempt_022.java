package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();

        int c = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        int d = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);

        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        int fromS1 = s1.hashCode();
        int fromS2 = s2.hashCode();

        int fromBytes1 = 0;
        for (int i = 0; i < bytes1.length; i++) {
            fromBytes1 = (fromBytes1 * 33) ^ bytes1[i];
        }

        int fromBytes2 = 0;
        for (int i = 0; i < bytes2.length; i++) {
            fromBytes2 = (fromBytes2 * 131) + bytes2[i];
        }

        int boundary1;
        switch ((by & 7)) {
            case 0:
                boundary1 = 0;
                break;
            case 1:
                boundary1 = 1;
                break;
            case 2:
                boundary1 = -1;
                break;
            case 3:
                boundary1 = Integer.MAX_VALUE;
                break;
            case 4:
                boundary1 = Integer.MIN_VALUE;
                break;
            case 5:
                boundary1 = 2;
                break;
            case 6:
                boundary1 = -2;
                break;
            default:
                boundary1 = 1 << 30;
                break;
        }

        int boundary2;
        switch ((fromS1 ^ fromS2) & 7) {
            case 0:
                boundary2 = 0;
                break;
            case 1:
                boundary2 = Integer.MIN_VALUE;
                break;
            case 2:
                boundary2 = Integer.MAX_VALUE;
                break;
            case 3:
                boundary2 = 1073741824;
                break;
            case 4:
                boundary2 = -1073741824;
                break;
            case 5:
                boundary2 = 46340;
                break;
            case 6:
                boundary2 = -46340;
                break;
            default:
                boundary2 = by;
                break;
        }

        int[] vals = new int[] {
            a,
            b,
            c,
            d,
            by,
            flip ? -a : a,
            flip ? -b : b,
            fromS1,
            fromS2,
            fromBytes1,
            fromBytes2,
            boundary1,
            boundary2,
            a ^ b,
            a + by,
            b - by,
            a & b,
            a | b,
            a == 0 ? Integer.MIN_VALUE : -a,
            b == 0 ? Integer.MAX_VALUE : -b
        };

        for (int i = 0; i < vals.length; i++) {
            MathUtils.gcd(vals[i], vals[i]);
            MathUtils.lcm(vals[i], vals[i]);
            MathUtils.gcd(vals[i], 0);
            MathUtils.gcd(0, vals[i]);
            MathUtils.lcm(vals[i], 0);
            MathUtils.lcm(0, vals[i]);
        }

        for (int i = 0; i < vals.length; i++) {
            for (int j = 0; j < vals.length; j++) {
                int x = vals[i];
                int y = vals[j];

                MathUtils.gcd(x, y);
                MathUtils.gcd(y, x);

                MathUtils.lcm(x, y);
                MathUtils.lcm(y, x);

                int g = MathUtils.gcd(x, y);
                MathUtils.gcd(g, x);
                MathUtils.gcd(g, y);
                MathUtils.lcm(g, x);
                MathUtils.lcm(g, y);

                int mixed1 = x == 0 ? y : x / (Math.abs((y | 1)));
                int mixed2 = y == 0 ? x : y / (Math.abs((x | 1)));
                MathUtils.gcd(mixed1, mixed2);
                MathUtils.lcm(mixed1, mixed2);
            }
        }

        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(1073741824, 1073741824);
        MathUtils.gcd(-1073741824, -1073741824);
        MathUtils.gcd(1073741824, -1073741824);

        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MAX_VALUE, 1);
        MathUtils.lcm(1, Integer.MAX_VALUE);
        MathUtils.lcm(1073741824, 2);
        MathUtils.lcm(-1073741824, 2);
        MathUtils.lcm(46340, 46341);
    }
}