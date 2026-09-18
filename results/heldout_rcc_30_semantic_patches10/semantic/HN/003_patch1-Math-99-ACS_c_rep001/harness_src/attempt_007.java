package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmOracles();
        runTrustedExceptionOracles();
        runGcdZeroIdentitySafe(data);
        runGcdSignInvariantPositiveInputs(data);
        runLcmSignAndDivisibilityMetamorphic(data);
    }

    private static void runLiftedLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("[oracle:testLcm-0-b]", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, 50)");
        assertEqualsInt("[oracle:testLcm-a-0]", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(30, 0)");
        assertEqualsInt("[oracle:testLcm-1-b]", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, 50)");
        assertEqualsInt("[oracle:testLcm-a-1]", a, MathUtils.lcm(a, 1), "MathUtils.lcm(30, 1)");
        assertEqualsInt("[oracle:testLcm-a-b]", 150, MathUtils.lcm(a, b), "MathUtils.lcm(30, 50)");
        assertEqualsInt("[oracle:testLcm-negA-b]", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(-30, 50)");
        assertEqualsInt("[oracle:testLcm-a-negB]", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(30, -50)");
        assertEqualsInt("[oracle:testLcm-negA-negB]", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(-30, -50)");
        assertEqualsInt("[oracle:testLcm-a-c]", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(30, 77)");
        assertEqualsInt("[oracle:testLcm-large-common-factor]", (1 << 20) * 15,
                MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5),
                "MathUtils.lcm((1<<20)*3, (1<<20)*5)");
        assertEqualsInt("[oracle:testLcm-0-0]", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");
    }

    private static void runTrustedExceptionOracles() {
        expectArithmeticExceptionLcm("[oracle:testLcm-min-1]", Integer.MIN_VALUE, 1);
        expectArithmeticExceptionLcm("[oracle:testLcm-min-1sh20]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticExceptionLcm("[oracle:testLcm-max-maxMinus1]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void runGcdZeroIdentitySafe(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
        } catch (Exception e) {
            return;
        }
        if (a == Integer.MIN_VALUE) {
            return;
        }

        int r1;
        try {
            r1 = MathUtils.gcd(a, 0);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = MathUtils.gcd(0, a);
        } catch (Exception e) {
            return;
        }

        int expected = Math.abs(a);
        if (r1 != expected || r2 != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation gcd-zero-identity-safe violated: a=" + a
                            + ", gcd(a,0)=" + r1
                            + ", gcd(0,a)=" + r2
                            + ", expected=" + expected);
        }
    }

    private static void runGcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 1_000_000);
            b = data.consumeInt(1, 1_000_000);
        } catch (Exception e) {
            return;
        }

        int gpp;
        try {
            gpp = MathUtils.gcd(a, b);
        } catch (Exception e) {
            return;
        }

        int gnp;
        try {
            gnp = MathUtils.gcd(-a, b);
        } catch (Exception e) {
            return;
        }

        int gpn;
        try {
            gpn = MathUtils.gcd(a, -b);
        } catch (Exception e) {
            return;
        }

        int gnn;
        try {
            gnn = MathUtils.gcd(-a, -b);
        } catch (Exception e) {
            return;
        }

        if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation gcd-sign-invariant-positive-inputs violated: a=" + a
                            + ", b=" + b
                            + ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void runLcmSignAndDivisibilityMetamorphic(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 46340);
            b = data.consumeInt(1, 46340);
        } catch (Exception e) {
            return;
        }

        int lpp;
        try {
            lpp = MathUtils.lcm(a, b);
        } catch (Exception e) {
            return;
        }

        int lnp;
        try {
            lnp = MathUtils.lcm(-a, b);
        } catch (Exception e) {
            return;
        }

        int lpn;
        try {
            lpn = MathUtils.lcm(a, -b);
        } catch (Exception e) {
            return;
        }

        int lnn;
        try {
            lnn = MathUtils.lcm(-a, -b);
        } catch (Exception e) {
            return;
        }

        int g;
        try {
            g = MathUtils.gcd(a, b);
        } catch (Exception e) {
            return;
        }

        long expectedProduct = ((long) a / (long) g) * (long) b;

        /* Contract asserted: lcm returns a nonnegative least common multiple.
           Therefore for positive nonzero a,b with representable result, the value must:
           (1) be unchanged by operand sign flips, as the trusted test checks;
           (2) be divisible by both operands;
           (3) satisfy gcd(a,b) * lcm(a,b) == |a*b|.
           A patch that merely deletes overflow throws or silently returns a wrong value
           can violate these observable post-conditions without throwing. */
        if (lpp <= 0
                || lpp != lnp
                || lpp != lpn
                || lpp != lnn
                || (lpp % a) != 0
                || (lpp % b) != 0
                || ((long) g * (long) lpp) != expectedProduct) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation lcm-sign-and-divisibility violated: a=" + a
                            + ", b=" + b
                            + ", gcd=" + g
                            + ", lcm(a,b)=" + lpp
                            + ", lcm(-a,b)=" + lnp
                            + ", lcm(a,-b)=" + lpn
                            + ", lcm(-a,-b)=" + lnn
                            + ", expectedProduct=" + expectedProduct
                            + ", observedProduct=" + ((long) g * (long) lpp));
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual, String what) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + what
                            + " expected=" + expected
                            + " actual=" + actual);
        }
    }

    private static void expectArithmeticExceptionLcm(String oracleId, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException from MathUtils.lcm("
                            + a + ", " + b + ") but returned " + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }
}