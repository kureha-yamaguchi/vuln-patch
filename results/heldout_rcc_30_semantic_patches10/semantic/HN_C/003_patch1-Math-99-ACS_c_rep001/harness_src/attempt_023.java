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
            128,
            -128,
            255,
            -255,
            256,
            -256,
            1023,
            -1023,
            1024,
            -1024,
            65535,
            -65535,
            65536,
            -65536,
            1073741824,
            -1073741824,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE + 1,
            Integer.MAX_VALUE - 1
        };

        int a = data.consumeBoolean() ? data.consumeInt() : interesting[data.consumeInt(0, interesting.length - 1)];
        int b = data.consumeBoolean() ? data.consumeInt() : interesting[data.consumeInt(0, interesting.length - 1)];
        int c = interesting[data.consumeInt(0, interesting.length - 1)];
        int d = interesting[data.consumeInt(0, interesting.length - 1)];
        int small1 = data.consumeInt(-8, 8);
        int small2 = data.consumeInt(-8, 8);

        int sink = 0;

        sink ^= MathUtils.gcd(a, b);
        sink ^= MathUtils.gcd(b, a);
        sink ^= MathUtils.gcd(a, 0);
        sink ^= MathUtils.gcd(0, b);
        sink ^= MathUtils.gcd(c, d);
        sink ^= MathUtils.gcd(c, small1);
        sink ^= MathUtils.gcd(small2, d);
        sink ^= MathUtils.gcd(Integer.MIN_VALUE, b);
        sink ^= MathUtils.gcd(a, Integer.MIN_VALUE);
        sink ^= MathUtils.gcd(Integer.MIN_VALUE, 0);
        sink ^= MathUtils.gcd(0, Integer.MIN_VALUE);
        sink ^= MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        sink ^= MathUtils.gcd(Integer.MAX_VALUE, Integer.MIN_VALUE);
        sink ^= MathUtils.gcd(1073741824, -1073741824);

        sink ^= MathUtils.lcm(a, b);
        sink ^= MathUtils.lcm(b, a);
        sink ^= MathUtils.lcm(a, 0);
        sink ^= MathUtils.lcm(0, b);
        sink ^= MathUtils.lcm(c, d);
        sink ^= MathUtils.lcm(c, small1);
        sink ^= MathUtils.lcm(small2, d);
        sink ^= MathUtils.lcm(Integer.MIN_VALUE, 1);
        sink ^= MathUtils.lcm(1, Integer.MIN_VALUE);
        sink ^= MathUtils.lcm(Integer.MIN_VALUE, -1);
        sink ^= MathUtils.lcm(-1, Integer.MIN_VALUE);
        sink ^= MathUtils.lcm(Integer.MIN_VALUE, 2);
        sink ^= MathUtils.lcm(2, Integer.MIN_VALUE);

        if (sink == 0x5a5a5a5a) {
            throw new RuntimeException("unreachable");
        }
    }
}