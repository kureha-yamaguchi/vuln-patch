package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();
        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] extra = data.consumeBytes(Math.min(32, data.remainingBytes()));

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
            by,
            -by,
            s1.length(),
            -s1.length(),
            s2.length(),
            -s2.length(),
            extra.length,
            -extra.length,
            a ^ b,
            a + by,
            b - by,
            flip ? a : -a,
            flip ? b : -b
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

                MathUtils.gcd(x, y);
                MathUtils.gcd(y, x);
                MathUtils.lcm(x, y);
                MathUtils.lcm(y, x);

                if (j + 1 < values.length) {
                    int z = values[j + 1];
                    MathUtils.gcd(MathUtils.gcd(x, y), z);
                    MathUtils.lcm(MathUtils.lcm(x, y), z);
                }
            }
        }

        if (extra.length >= 8) {
            int x = ((extra[0] & 0xff) << 24) | ((extra[1] & 0xff) << 16) | ((extra[2] & 0xff) << 8) | (extra[3] & 0xff);
            int y = ((extra[4] & 0xff) << 24) | ((extra[5] & 0xff) << 16) | ((extra[6] & 0xff) << 8) | (extra[7] & 0xff);
            MathUtils.gcd(x, y);
            MathUtils.lcm(x, y);
        }

        if (data.remainingBytes() > 0) {
            byte[] rest = data.consumeRemainingAsBytes();
            int acc1 = 0;
            int acc2 = 0;
            for (int i = 0; i < rest.length; i++) {
                if ((i & 1) == 0) {
                    acc1 = (acc1 << 1) ^ rest[i];
                } else {
                    acc2 = (acc2 << 1) ^ rest[i];
                }
                MathUtils.gcd(acc1, acc2);
                MathUtils.lcm(acc1, acc2);
            }
        }
    }
}