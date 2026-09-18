package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        byte tweak1 = data.consumeByte();
        byte tweak2 = data.consumeByte();
        boolean flipA = data.consumeBoolean();
        boolean flipB = data.consumeBoolean();
        int small = data.consumeInt(-64, 64);
        int pow = data.consumeInt(0, 31);

        String s1 = data.consumeString(16);
        String s2 = data.consumeAsciiString(16);
        byte[] extra = data.consumeBytes(16);
        byte[] tail = data.consumeRemainingAsBytes();

        int fromStrings = s1.hashCode() ^ s2.hashCode();
        int fromExtra = 0;
        for (int i = 0; i < extra.length; i++) {
            fromExtra = (fromExtra << 5) - fromExtra + (extra[i] & 0xff);
        }
        int fromTail = 0;
        for (int i = 0; i < tail.length; i++) {
            fromTail = (fromTail * 131) ^ (tail[i] & 0xff);
        }

        int shifted = (pow == 31) ? Integer.MIN_VALUE : (1 << pow);
        int evenish = a & ~1;
        int oddish = (b | 1);
        int maybeMin = flipA ? Integer.MIN_VALUE : (a ^ tweak1);
        int maybeMax = flipB ? Integer.MAX_VALUE : (b ^ tweak2);

        int[] values = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            a,
            b,
            c,
            d,
            small,
            -small,
            shifted,
            shifted == Integer.MIN_VALUE ? Integer.MIN_VALUE : -shifted,
            evenish,
            oddish,
            fromStrings,
            fromExtra,
            fromTail,
            maybeMin,
            maybeMax,
            a + small,
            b - small,
            a ^ b,
            c | 1,
            d & ~1
        };

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], 0);
            MathUtils.gcd(0, values[i]);
            MathUtils.lcm(values[i], 0);
            MathUtils.lcm(0, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                int x = values[i];
                int y = values[j];

                int g = MathUtils.gcd(x, y);
                int l = MathUtils.lcm(x, y);

                MathUtils.gcd(y, x);
                MathUtils.lcm(y, x);

                MathUtils.gcd(g, l);
                MathUtils.gcd(l, g);

                if (g != Integer.MIN_VALUE) {
                    MathUtils.gcd(x, -g);
                    MathUtils.lcm(y, g);
                }

                if (l != Integer.MIN_VALUE) {
                    MathUtils.gcd(l, x);
                    MathUtils.lcm(g, l);
                }

                if (j + 1 < values.length) {
                    int z = values[j + 1];
                    MathUtils.gcd(MathUtils.gcd(x, y), z);
                    MathUtils.lcm(MathUtils.lcm(x, y), z);
                }
            }
        }
    }
}