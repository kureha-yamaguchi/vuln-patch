package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        int parsed1;
        try {
            parsed1 = Integer.parseInt(s1.trim());
        } catch (Throwable t) {
            parsed1 = s1.length();
        }

        int parsed2;
        try {
            parsed2 = Integer.parseInt(s2.trim());
        } catch (Throwable t) {
            parsed2 = s2.hashCode();
        }

        int bytesVal1 = 0;
        for (int i = 0; i < bytes1.length; i++) {
            bytesVal1 = (bytesVal1 << 3) ^ (bytes1[i] & 0xff) ^ i;
        }

        int bytesVal2 = 0;
        for (int i = 0; i < bytes2.length; i++) {
            bytesVal2 = (bytesVal2 << 1) ^ (bytes2[i] & 0xff) ^ (i * 31);
        }

        int[] vals = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE + 1,
            Integer.MAX_VALUE - 1,
            a,
            b,
            c,
            d,
            by,
            -by,
            parsed1,
            parsed2,
            bytesVal1,
            bytesVal2,
            a ^ b,
            a + 1,
            b - 1,
            flip ? a : -a,
            flip ? b : -b
        };

        for (int i = 0; i < vals.length; i++) {
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
                MathUtils.gcd(-x, y);
                MathUtils.gcd(x, -y);

                MathUtils.lcm(x, y);
                MathUtils.lcm(y, x);

                if (j < 8) {
                    MathUtils.gcd(x / 2, y / 2);
                    MathUtils.lcm(x / 2, y / 2);
                }
            }
        }

        int[] powers = new int[] {
            1 << 30,
            -(1 << 30),
            1 << 29,
            -(1 << 29),
            1 << 16,
            -(1 << 16),
            Integer.MIN_VALUE,
            0
        };

        for (int i = 0; i < powers.length; i++) {
            for (int j = 0; j < powers.length; j++) {
                MathUtils.gcd(powers[i], powers[j]);
                MathUtils.lcm(powers[i], powers[j]);
            }
        }

        MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, 0);
        MathUtils.gcd(0, Integer.MIN_VALUE);
        MathUtils.gcd(Integer.MIN_VALUE, by);
        MathUtils.gcd(by, Integer.MIN_VALUE);

        MathUtils.lcm(Integer.MIN_VALUE, 1);
        MathUtils.lcm(1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MIN_VALUE, -1);
        MathUtils.lcm(-1, Integer.MIN_VALUE);
        MathUtils.lcm(Integer.MAX_VALUE, 2);
        MathUtils.lcm(2, Integer.MAX_VALUE);
    }
}