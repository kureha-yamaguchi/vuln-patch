package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int small = data.consumeInt(-64, 64);
        int maybeBoundary = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        byte singleByte = data.consumeByte();
        byte[] raw = data.consumeBytes(16);
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        String s3 = data.consumeRemainingAsString();

        int fromBytes1 = 0;
        int fromBytes2 = 0;
        for (int i = 0; i < raw.length; i++) {
            fromBytes1 = (fromBytes1 << 3) ^ (raw[i] & 0xff) ^ i;
            fromBytes2 += (raw[i] << (i & 7));
        }

        int fromString1 = s1.hashCode();
        int fromString2 = s2.hashCode();
        int fromString3 = s3.hashCode();

        int[] values = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            3,
            -3,
            4,
            -4,
            8,
            -8,
            16,
            -16,
            31,
            -31,
            32,
            -32,
            63,
            -63,
            64,
            -64,
            127,
            -127,
            255,
            -255,
            1024,
            -1024,
            1 << 30,
            -(1 << 30),
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            a,
            b,
            c,
            small,
            maybeBoundary,
            singleByte,
            -singleByte,
            fromBytes1,
            fromBytes2,
            fromString1,
            fromString2,
            fromString3,
            a ^ b,
            a + b,
            a - b,
            b - a,
            a * (data.consumeBoolean() ? 2 : -2),
            b * (data.consumeBoolean() ? 2 : -2)
        };

        for (int i = 0; i < values.length; i++) {
            int x = values[i];

            MathUtils.gcd(x, 0);
            MathUtils.gcd(0, x);
            MathUtils.gcd(x, x);
            MathUtils.gcd(x, -x);

            MathUtils.lcm(x, 0);
            MathUtils.lcm(0, x);
            MathUtils.lcm(x, 1);
            MathUtils.lcm(1, x);
            MathUtils.lcm(x, -1);
            MathUtils.lcm(-1, x);
        }

        for (int i = 0; i < values.length; i++) {
            int x = values[i];
            int y = values[(i * 7 + 3) % values.length];

            MathUtils.gcd(x, y);
            MathUtils.gcd(y, x);
            MathUtils.gcd(-x, y);
            MathUtils.gcd(x, -y);

            MathUtils.lcm(x, y);
            MathUtils.lcm(y, x);
            MathUtils.lcm(-x, y);
            MathUtils.lcm(x, -y);
        }

        int x1 = values[data.consumeInt(0, values.length - 1)];
        int x2 = values[data.consumeInt(0, values.length - 1)];
        int x3 = values[data.consumeInt(0, values.length - 1)];
        int x4 = values[data.consumeInt(0, values.length - 1)];

        MathUtils.gcd(x1, x2);
        MathUtils.gcd(x3, x4);
        MathUtils.gcd(MathUtils.gcd(x1, x2), x3);

        MathUtils.lcm(x1, x2);
        MathUtils.lcm(x3, x4);
        MathUtils.lcm(MathUtils.gcd(x1, x2), x4);

        if (data.consumeBoolean()) {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
        } else {
            MathUtils.gcd(0, Integer.MIN_VALUE);
        }

        if (data.consumeBoolean()) {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } else {
            MathUtils.gcd(Integer.MIN_VALUE, maybeBoundary);
        }

        if (data.consumeBoolean()) {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
        } else {
            MathUtils.lcm(Integer.MIN_VALUE, -1);
        }
    }
}