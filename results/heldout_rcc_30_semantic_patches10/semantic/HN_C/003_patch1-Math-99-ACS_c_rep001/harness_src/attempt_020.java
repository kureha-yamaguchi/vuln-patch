package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int[] interesting = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            4,
            -4,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE - 1,
            Integer.MIN_VALUE + 1,
            1073741824,
            -1073741824,
            1431655765,
            -1431655765
        };

        if (data.consumeInt(0, 31) == 0) {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        if (data.consumeInt(0, 31) == 1) {
            MathUtils.lcm(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        int[] values = new int[8];
        for (int i = 0; i < values.length; i++) {
            int raw = data.consumeInt();
            int special = interesting[data.consumeInt(0, interesting.length - 1)];
            int v;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    v = raw;
                    break;
                case 1:
                    v = special;
                    break;
                case 2:
                    v = data.consumeBoolean() ? (raw | 1) : (raw & ~1);
                    break;
                case 3:
                    v = raw ^ special;
                    break;
                case 4:
                    v = raw + special;
                    break;
                case 5:
                    v = raw - special;
                    break;
                case 6:
                    v = (i == 0) ? special : -values[i - 1];
                    break;
                default:
                    v = (i == 0) ? raw : values[i - 1] / 2;
                    break;
            }
            values[i] = v;
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

                if (data.consumeBoolean()) {
                    MathUtils.gcd(-a, b);
                    MathUtils.gcd(a, -b);
                    MathUtils.lcm(-a, b);
                    MathUtils.lcm(a, -b);
                }

                if (data.consumeBoolean()) {
                    int da = a / 2;
                    int db = b / 2;
                    MathUtils.gcd(da, db);
                    MathUtils.lcm(da, db);
                }

                if (data.consumeBoolean()) {
                    int aa = (a & ~1);
                    int bb = (b & ~1);
                    MathUtils.gcd(aa, bb);
                    MathUtils.lcm(aa, bb);
                }

                if (data.consumeBoolean()) {
                    int aa = (a | 1);
                    int bb = (b | 1);
                    MathUtils.gcd(aa, bb);
                    MathUtils.lcm(aa, bb);
                }
            }
        }

        int x = interesting[data.consumeInt(0, interesting.length - 1)];
        int y = interesting[data.consumeInt(0, interesting.length - 1)];
        MathUtils.gcd(x, y);
        MathUtils.lcm(x, y);
    }
}