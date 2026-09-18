package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int raw1 = data.consumeInt();
        int raw2 = data.consumeInt();
        int raw3 = data.consumeInt();
        int small = data.consumeInt(-1024, 1024);
        int shift = data.consumeInt(0, 31);
        byte b = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes = data.consumeBytes(16);
        int remaining = data.remainingBytes();

        int fromBytes = 0;
        for (int i = 0; i < bytes.length; i++) {
            fromBytes = (fromBytes << 8) ^ (bytes[i] & 0xff);
        }

        int strHash1 = s1.hashCode();
        int strHash2 = s2.hashCode();
        int powerOfTwo = 1 << shift;
        int evenish = raw2 & ~1;
        int oddish = raw3 | 1;
        int signMixed = flip ? -small : small;
        int byteVal = b;

        int[] values = new int[] {
            0,
            1,
            -1,
            2,
            -2,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            raw1,
            raw2,
            raw3,
            small,
            signMixed,
            evenish,
            oddish,
            powerOfTwo,
            -powerOfTwo,
            fromBytes,
            strHash1,
            strHash2,
            s1.length(),
            -s1.length(),
            s2.length(),
            -s2.length(),
            bytes.length,
            -bytes.length,
            remaining,
            -remaining,
            byteVal,
            -byteVal
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
                MathUtils.lcm(a, c);

                if (i != j) {
                    MathUtils.gcd(c, a);
                    MathUtils.lcm(c, a);
                }

                MathUtils.gcd(-a, c);
                MathUtils.gcd(a, -c);
                MathUtils.gcd(-a, -c);

                MathUtils.lcm(-a, c);
                MathUtils.lcm(a, -c);
                MathUtils.lcm(-a, -c);
            }
        }
    }
}