package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();
        byte rawShift = data.consumeByte();
        boolean flipA = data.consumeBoolean();
        boolean flipB = data.consumeBoolean();

        int shift = rawShift & 31;

        int aa = flipA ? -a : a;
        int bb = flipB ? -b : b;

        int[] values = new int[] {
            aa,
            bb,
            c,
            d,
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            1 << 30,
            -(1 << 30),
            aa >> (shift == 31 ? 30 : shift),
            bb >> ((shift + 1) & 31),
            aa << (shift & 1),
            bb << ((shift + 1) & 1),
            aa | bb,
            aa & bb,
            aa ^ bb,
            aa + 1,
            aa - 1,
            bb + 1,
            bb - 1
        };

        for (int i = 0; i < values.length - 1; i++) {
            int x = values[i];
            int y = values[i + 1];

            MathUtils.gcd(x, y);
            MathUtils.gcd(y, x);
            MathUtils.gcd(-x, y);
            MathUtils.gcd(x, -y);

            MathUtils.lcm(x, y);
            MathUtils.lcm(y, x);
            MathUtils.lcm(-x, y);
            MathUtils.lcm(x, -y);
        }

        int idx1 = values.length == 0 ? 0 : Math.abs(a % values.length);
        int idx2 = values.length == 0 ? 0 : Math.abs(b % values.length);
        int x = values[idx1];
        int y = values[idx2];

        MathUtils.gcd(x, y);
        MathUtils.lcm(x, y);

        MathUtils.gcd(Integer.MIN_VALUE, x);
        MathUtils.gcd(Integer.MIN_VALUE, y);
        MathUtils.gcd(0, x);
        MathUtils.gcd(y, 0);

        MathUtils.lcm(Integer.MIN_VALUE, x);
        MathUtils.lcm(Integer.MIN_VALUE, y);
        MathUtils.lcm(0, x);
        MathUtils.lcm(y, 0);
    }
}