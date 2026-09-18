package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int x = data.consumeInt();
        int y = data.consumeInt();
        int z = data.consumeInt();

        byte[] extra = data.consumeBytes(Math.min(32, data.remainingBytes()));
        int[] values = new int[16 + extra.length * 2];

        int idx = 0;
        values[idx++] = 0;
        values[idx++] = 1;
        values[idx++] = -1;
        values[idx++] = 2;
        values[idx++] = -2;
        values[idx++] = Integer.MAX_VALUE;
        values[idx++] = Integer.MIN_VALUE;
        values[idx++] = 1073741824;
        values[idx++] = -1073741824;
        values[idx++] = x;
        values[idx++] = y;
        values[idx++] = z;
        values[idx++] = x ^ y;
        values[idx++] = x + y;
        values[idx++] = x - y;
        values[idx++] = ~x;

        for (int i = 0; i < extra.length; i++) {
            int b = extra[i];
            values[idx++] = b;
            values[idx++] = b << ((i & 3) * 8);
        }

        for (int i = 0; i < values.length; i++) {
            int a = values[i];

            MathUtils.gcd(a, 0);
            MathUtils.gcd(0, a);
            MathUtils.lcm(a, 0);
            MathUtils.lcm(0, a);

            for (int j = 0; j < values.length; j++) {
                int b = values[j];

                MathUtils.gcd(a, b);
                MathUtils.gcd(b, a);
                MathUtils.lcm(a, b);
                MathUtils.lcm(b, a);

                int g = MathUtils.gcd(a, b);

                MathUtils.gcd(g, a);
                MathUtils.gcd(g, b);
                MathUtils.lcm(g, a);
                MathUtils.lcm(g, b);

                if ((j & 1) == 0) {
                    MathUtils.gcd(a / 2, b);
                    MathUtils.gcd(a, b / 2);
                    MathUtils.lcm(a / 2, b);
                    MathUtils.lcm(a, b / 2);
                }
            }
        }
    }
}