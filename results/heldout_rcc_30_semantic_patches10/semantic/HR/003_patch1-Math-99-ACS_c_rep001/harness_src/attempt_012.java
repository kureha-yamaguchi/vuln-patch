package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int exp = data.consumeInt(0, 30);
        int pow2 = 1 << exp;
        int b = data.consumeBoolean() ? pow2 : -pow2;

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
        } catch (ArithmeticException ignored) {
        }
        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
        } catch (ArithmeticException ignored) {
        }

        int g;
        try {
            g = MathUtils.gcd(Integer.MIN_VALUE, b);
        } catch (ArithmeticException ignored) {
            return;
        }

        long exactMagnitude = Math.abs(((long) (Integer.MIN_VALUE / g)) * ((long) b));

        try {
            int actual = MathUtils.lcm(Integer.MIN_VALUE, b);

            /*
             * Documented contract: lcm returns the least common multiple of the absolute
             * value of two numbers. Therefore any successful return must be a nonnegative
             * int equal to the exact mathematical magnitude. If the exact magnitude,
             * recomputed independently from the real helper gcd and long arithmetic,
             * exceeds Integer.MAX_VALUE, a correct implementation cannot return any int;
             * it must reject. A patch that merely deletes the overflow throw returns
             * Integer.MIN_VALUE here, violating this post-condition without needing the
             * original exception symptom.
             */
            if (exactMagnitude > Integer.MAX_VALUE) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-helper-magnitude-consistency] consistency violation: lcm returned " + actual
                        + " for inputs a=" + Integer.MIN_VALUE + " b=" + b
                        + " even though exactMagnitude=" + exactMagnitude
                        + " computed from gcd=" + g + " is not representable as a nonnegative int");
            }

            if (actual < 0 || ((long) actual) != exactMagnitude) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-helper-magnitude-consistency] consistency violation: a=" + Integer.MIN_VALUE
                        + " b=" + b + " gcd=" + g + " expectedMagnitude=" + exactMagnitude
                        + " actual=" + actual);
            }
        } catch (ArithmeticException ignored) {
        }
    }
}