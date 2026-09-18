package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void checkEquals(String id, int expected, int actual, String detail) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual + " " + detail);
        }
    }

    private static void checkArithmeticException(String id, int a, int b, boolean gcd) {
        try {
            if (gcd) {
                MathUtils.gcd(a, b);
            } else {
                MathUtils.lcm(a, b);
            }
        } catch (ArithmeticException expected) {
            return;
        }
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
            "[oracle:" + id + "] semantic mismatch: expecting ArithmeticException for "
                + (gcd ? "gcd" : "lcm") + "(" + a + "," + b + ")");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        checkEquals("gcd-seed-00", 0, MathUtils.gcd(0, 0), "for gcd(0,0)");
        checkEquals("gcd-seed-0b", b, MathUtils.gcd(0, b), "for gcd(0,50)");
        checkEquals("gcd-seed-a0", a, MathUtils.gcd(a, 0), "for gcd(30,0)");
        checkEquals("gcd-seed-0negb", b, MathUtils.gcd(0, -b), "for gcd(0,-50)");
        checkEquals("gcd-seed-nega0", a, MathUtils.gcd(-a, 0), "for gcd(-30,0)");

        checkEquals("gcd-seed-ab", 10, MathUtils.gcd(a, b), "for gcd(30,50)");
        checkEquals("gcd-seed-negab", 10, MathUtils.gcd(-a, b), "for gcd(-30,50)");
        checkEquals("gcd-seed-anegb", 10, MathUtils.gcd(a, -b), "for gcd(30,-50)");
        checkEquals("gcd-seed-neganegb", 10, MathUtils.gcd(-a, -b), "for gcd(-30,-50)");

        checkEquals("gcd-seed-ac", 1, MathUtils.gcd(a, c), "for gcd(30,77)");
        checkEquals("gcd-seed-negac", 1, MathUtils.gcd(-a, c), "for gcd(-30,77)");
        checkEquals("gcd-seed-anegc", 1, MathUtils.gcd(a, -c), "for gcd(30,-77)");
        checkEquals("gcd-seed-neganegc", 1, MathUtils.gcd(-a, -c), "for gcd(-30,-77)");

        checkEquals("gcd-seed-scale", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)),
            "for gcd(3*(1<<20),9*(1<<15))");
        checkEquals("gcd-seed-max", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0),
            "for gcd(Integer.MAX_VALUE,0)");
        checkEquals("gcd-seed-negmax", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0),
            "for gcd(-Integer.MAX_VALUE,0)");
        checkEquals("gcd-seed-minpower", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE),
            "for gcd(1<<30,-Integer.MIN_VALUE)");

        checkArithmeticException("gcd-seed-min-left-throws", Integer.MIN_VALUE, 0, true);
        checkArithmeticException("gcd-seed-min-right-throws", 0, Integer.MIN_VALUE, true);
        checkArithmeticException("gcd-seed-min-both-throws", Integer.MIN_VALUE, Integer.MIN_VALUE, true);

        int fuzz = data.consumeInt(-1_000_000, 1_000_000);
        if (fuzz == 0) {
            fuzz = 1;
        }
        int even = fuzz * 2;

        try {
            int g1 = MathUtils.gcd(even, 0);
            int g2 = MathUtils.gcd(-even, 0);
            checkEquals("gcd-even-sign-canonical", Math.abs(even), g1, "for gcd(2n,0)");
            checkEquals("gcd-even-sign-canonical-neg", Math.abs(even), g2, "for gcd(-2n,0)");
        } catch (ArithmeticException ignored) {
            return;
        }

        try {
            int k = data.consumeInt(1, 1_000_000);
            int left = MathUtils.mulAndCheck(k, 2);
            int right = MathUtils.addAndCheck(k, k);
            if (left != right) {
                throw new RuntimeException(
                    "[oracle:mul-double-via-add] metamorphic violation: mulAndCheck(k,2)==addAndCheck(k,k) input="
                        + k + " lhs=" + left + " rhs=" + right);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        try {
            int p = data.consumeInt(1, 1_000_000);
            int q = data.consumeInt(1, 1_000_000);
            int gpq = MathUtils.gcd(p, q);
            int l = MathUtils.lcm(p, gpq);
            checkEquals(
                "lcm-with-gcd-factor",
                p,
                l,
                "for lcm(p,gcd(p,q)) when gcd(p,q) divides p; contract says lcm is the least common multiple of absolute values");
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}