package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();

        int shift1 = data.consumeInt(0, 31);
        int shift2 = data.consumeInt(0, 31);

        int pow2a = 1 << shift1;
        int pow2b = 1 << shift2;

        int[] values = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            pow2a,
            -pow2a,
            pow2b,
            -pow2b,
            a,
            -a,
            b,
            -b,
            c,
            -c,
            d,
            -d,
            a ^ b,
            c ^ d,
            a + b,
            c - d,
            a & b,
            c | d
        };

        int[][] pairs = new int[][] {
            {Integer.MIN_VALUE, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, 0},
            {0, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, 1},
            {1, Integer.MIN_VALUE},
            {Integer.MIN_VALUE, -1},
            {-1, Integer.MIN_VALUE},
            {0, 0},
            {0, 1},
            {1, 0},
            {a, b},
            {b, a},
            {a, 0},
            {0, b},
            {a, a},
            {b, b},
            {a, -a},
            {b, -b},
            {pow2a, pow2b},
            {-pow2a, -pow2b},
            {a ^ b, c ^ d},
            {a + b, c - d}
        };

        boolean callLcmFirst = data.consumeBoolean();

        if (callLcmFirst) {
            for (int i = 0; i < pairs.length; i++) {
                MathUtils.lcm(pairs[i][0], pairs[i][1]);
                MathUtils.gcd(pairs[i][0], pairs[i][1]);
            }
        } else {
            for (int i = 0; i < pairs.length; i++) {
                MathUtils.gcd(pairs[i][0], pairs[i][1]);
                MathUtils.lcm(pairs[i][0], pairs[i][1]);
            }
        }

        int pairCount = Math.min(values.length, data.remainingBytes() > 0 ? data.consumeInt(1, values.length) : values.length);
        for (int i = 0; i < pairCount; i++) {
            int x = values[i];
            int y = values[values.length - 1 - i];

            MathUtils.gcd(x, y);
            MathUtils.gcd(y, x);
            MathUtils.lcm(x, y);
            MathUtils.lcm(y, x);
        }

        if (data.remainingBytes() > 0) {
            byte[] extra = data.consumeRemainingAsBytes();
            for (int i = 0; i + 7 < extra.length; i += 8) {
                int x = ((extra[i] & 0xff) << 24)
                        | ((extra[i + 1] & 0xff) << 16)
                        | ((extra[i + 2] & 0xff) << 8)
                        | (extra[i + 3] & 0xff);
                int y = ((extra[i + 4] & 0xff) << 24)
                        | ((extra[i + 5] & 0xff) << 16)
                        | ((extra[i + 6] & 0xff) << 8)
                        | (extra[i + 7] & 0xff);

                MathUtils.gcd(x, y);
                MathUtils.lcm(x, y);
            }
        }
    }
}