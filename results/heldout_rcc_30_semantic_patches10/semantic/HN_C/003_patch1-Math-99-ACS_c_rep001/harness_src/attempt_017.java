package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int raw1 = data.consumeInt();
        int raw2 = data.consumeInt();
        int raw3 = data.consumeInt();
        int raw4 = data.consumeInt();

        byte b = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);

        int bytesLen = data.consumeInt(0, Math.min(64, data.remainingBytes()));
        byte[] bytes = data.consumeBytes(bytesLen);
        byte[] rest = data.consumeRemainingAsBytes();

        int fromBytes = 0;
        for (int i = 0; i < bytes.length; i++) {
            fromBytes = (fromBytes * 33) ^ bytes[i];
        }

        int fromRest = 0;
        for (int i = 0; i < rest.length; i++) {
            fromRest = (fromRest * 131) + rest[i];
        }

        int sl1 = s1.length();
        int sl2 = s2.length();

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
            1073741824,
            -1073741824,
            2147483646,
            -2147483647,
            raw1,
            raw2,
            raw3,
            raw4,
            b,
            -b,
            sl1,
            -sl1,
            sl2,
            -sl2,
            fromBytes,
            fromRest,
            fromBytes ^ raw1,
            fromRest ^ raw2,
            flip ? raw3 : -raw3,
            flip ? Integer.MIN_VALUE : Integer.MAX_VALUE
        };

        for (int i = 0; i < values.length; i++) {
            MathUtils.gcd(values[i], 0);
            MathUtils.gcd(0, values[i]);
            MathUtils.lcm(values[i], 0);
            MathUtils.lcm(0, values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                int a = values[i];
                int c = values[j];

                MathUtils.gcd(a, c);
                MathUtils.gcd(c, a);
                MathUtils.lcm(a, c);
                MathUtils.lcm(c, a);

                if ((j & 1) == 0) {
                    MathUtils.gcd(-a, c);
                    MathUtils.gcd(a, -c);
                    MathUtils.lcm(-a, c);
                    MathUtils.lcm(a, -c);
                }
            }
        }

        int[] specials = new int[] {
            Integer.MIN_VALUE, Integer.MIN_VALUE,
            Integer.MIN_VALUE, 0,
            0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, 1,
            1, Integer.MIN_VALUE,
            Integer.MIN_VALUE, -1,
            -1, Integer.MIN_VALUE,
            Integer.MIN_VALUE, 2,
            2, Integer.MIN_VALUE,
            Integer.MAX_VALUE, Integer.MAX_VALUE,
            Integer.MAX_VALUE, Integer.MIN_VALUE,
            1073741824, 1073741824,
            -1073741824, -1073741824,
            raw1, raw1,
            raw2, -raw2
        };

        for (int i = 0; i + 1 < specials.length; i += 2) {
            int a = specials[i];
            int c = specials[i + 1];
            MathUtils.gcd(a, c);
            MathUtils.lcm(a, c);
        }
    }
}