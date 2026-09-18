package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a0 = data.consumeInt();
        int a1 = data.consumeInt();
        int a2 = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        int a3 = data.consumeInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        byte b0 = data.consumeByte();
        byte b1 = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s0 = data.consumeString(32);
        String s1 = data.consumeAsciiString(32);
        byte[] bytes0 = data.consumeBytes(data.consumeInt(0, 32));
        byte[] bytes1 = data.consumeRemainingAsBytes();

        int[] values = new int[24];
        int i = 0;

        values[i++] = 0;
        values[i++] = 1;
        values[i++] = -1;
        values[i++] = 2;
        values[i++] = -2;
        values[i++] = Integer.MAX_VALUE;
        values[i++] = Integer.MIN_VALUE;
        values[i++] = 1073741824;
        values[i++] = -1073741824;
        values[i++] = a0;
        values[i++] = a1;
        values[i++] = a2;
        values[i++] = a3;
        values[i++] = b0;
        values[i++] = b1;
        values[i++] = s0.hashCode();
        values[i++] = s1.hashCode();
        values[i++] = s0.length();
        values[i++] = -s1.length();

        int acc0 = 0;
        for (int j = 0; j < bytes0.length; j++) {
            acc0 = (acc0 << 5) - acc0 + (bytes0[j] & 0xff);
        }
        values[i++] = acc0;

        int acc1 = 0;
        for (int j = 0; j < bytes1.length; j++) {
            acc1 = (acc1 << 1) ^ (bytes1[j] & 0xff);
        }
        values[i++] = acc1;

        values[i++] = flip ? -a0 : a0;
        values[i++] = a0 ^ a1;
        values[i++] = a2 + a3;

        for (int x = 0; x < values.length; x++) {
            MathUtils.gcd(values[x], values[x]);
            MathUtils.gcd(values[x], 0);
            MathUtils.gcd(0, values[x]);
            MathUtils.lcm(values[x], values[x]);
            MathUtils.lcm(values[x], 0);
            MathUtils.lcm(0, values[x]);
        }

        for (int x = 0; x < values.length; x++) {
            for (int y = 0; y < values.length; y++) {
                int p = values[x];
                int q = values[y];

                MathUtils.gcd(p, q);
                MathUtils.gcd(q, p);
                MathUtils.gcd(-p, q);
                MathUtils.gcd(p, -q);

                MathUtils.lcm(p, q);
                MathUtils.lcm(q, p);
                MathUtils.lcm(-p, q);
                MathUtils.lcm(p, -q);
            }
        }
    }
}