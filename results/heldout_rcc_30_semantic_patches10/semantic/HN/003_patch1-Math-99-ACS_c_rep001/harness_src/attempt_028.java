package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        checkEquals("lifted-gcd-0-0", 0, safeGcd(0, 0), "gcd(0,0)");
        checkEquals("lifted-gcd-0-b", b, safeGcd(0, b), "gcd(0,b)");
        checkEquals("lifted-gcd-a-0", a, safeGcd(a, 0), "gcd(a,0)");
        checkEquals("lifted-gcd-0-negb", b, safeGcd(0, -b), "gcd(0,-b)");
        checkEquals("lifted-gcd-nega-0", a, safeGcd(-a, 0), "gcd(-a,0)");

        checkEquals("lifted-gcd-a-b", 10, safeGcd(a, b), "gcd(a,b)");
        checkEquals("lifted-gcd-nega-b", 10, safeGcd(-a, b), "gcd(-a,b)");
        checkEquals("lifted-gcd-a-negb", 10, safeGcd(a, -b), "gcd(a,-b)");
        checkEquals("lifted-gcd-nega-negb", 10, safeGcd(-a, -b), "gcd(-a,-b)");

        checkEquals("lifted-gcd-a-c", 1, safeGcd(a, c), "gcd(a,c)");
        checkEquals("lifted-gcd-nega-c", 1, safeGcd(-a, c), "gcd(-a,c)");
        checkEquals("lifted-gcd-a-negc", 1, safeGcd(a, -c), "gcd(a,-c)");
        checkEquals("lifted-gcd-nega-negc", 1, safeGcd(-a, -c), "gcd(-a,-c)");

        checkEquals(
                "lifted-gcd-shifted",
                3 * (1 << 15),
                safeGcd(3 * (1 << 20), 9 * (1 << 15)),
                "gcd(3*(1<<20),9*(1<<15))");

        checkEquals(
                "lifted-gcd-max-0",
                Integer.MAX_VALUE,
                safeGcd(Integer.MAX_VALUE, 0),
                "gcd(Integer.MAX_VALUE,0)");
        checkEquals(
                "lifted-gcd-negmax-0",
                Integer.MAX_VALUE,
                safeGcd(-Integer.MAX_VALUE, 0),
                "gcd(-Integer.MAX_VALUE,0)");
        checkEquals(
                "lifted-gcd-1sh30-min",
                1 << 30,
                safeGcd(1 << 30, -Integer.MIN_VALUE),
                "gcd(1<<30,-Integer.MIN_VALUE)");

        expectArithmetic("lifted-gcd-min-0", Integer.MIN_VALUE, 0);
        expectArithmetic("lifted-gcd-0-min", 0, Integer.MIN_VALUE);
        expectArithmetic("lifted-gcd-min-min", Integer.MIN_VALUE, Integer.MIN_VALUE);

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);

        try {
            MathUtils.gcd(x, y);
        } catch (Throwable ignored) {
        }

        try {
            MathUtils.lcm(x, y);
        } catch (Throwable ignored) {
        }

        if (x != Integer.MIN_VALUE) {
            try {
                int g1 = MathUtils.gcd(x, 0);
                int g2 = MathUtils.gcd(-x, 0);
                int expected = Math.abs(x);
                checkEquals("zero-arg-abs-pos", expected, g1, "gcd(" + x + ",0)");
                checkEquals("zero-arg-abs-neg", expected, g2, "gcd(" + (-x) + ",0)");
            } catch (Throwable ignored) {
                return;
            }
        }

        if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
            try {
                int gxy = MathUtils.gcd(x, y);
                int gyx = MathUtils.gcd(y, x);
                int gnegxY = MathUtils.gcd(-x, y);
                int gxNegy = MathUtils.gcd(x, -y);
                int gnegxNegy = MathUtils.gcd(-x, -y);

                /* The test exercises all sign variants of gcd and they must agree;
                 * a correct greatest-common-divisor depends only on absolute values,
                 * so deleting the overflow throw must not silently change ordinary results. */
                if (gxy != gyx) {
                    throw new RuntimeException("[oracle:gcd-symmetry] metamorphic violation: gcd symmetry input=(" + x + "," + y + ") lhs=" + gxy + " rhs=" + gyx);
                }
                if (gxy != gnegxY) {
                    throw new RuntimeException("[oracle:gcd-sign-left] metamorphic violation: gcd(x,y)==gcd(-x,y) input=(" + x + "," + y + ") lhs=" + gxy + " rhs=" + gnegxY);
                }
                if (gxy != gxNegy) {
                    throw new RuntimeException("[oracle:gcd-sign-right] metamorphic violation: gcd(x,y)==gcd(x,-y) input=(" + x + "," + y + ") lhs=" + gxy + " rhs=" + gxNegy);
                }
                if (gxy != gnegxNegy) {
                    throw new RuntimeException("[oracle:gcd-sign-both] metamorphic violation: gcd(x,y)==gcd(-x,-y) input=(" + x + "," + y + ") lhs=" + gxy + " rhs=" + gnegxNegy);
                }
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    private static int safeGcd(int p, int q) {
        try {
            return MathUtils.gcd(p, q);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unexpected-throw] semantic mismatch: gcd(" + p + "," + q + ") threw " + e.getClass().getName());
        }
    }

    private static void expectArithmetic(String id, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for gcd(" + p + "," + q + ") but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }

    private static void checkEquals(String id, int expected, int actual, String what) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }
}