package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkGeneralizedKnownExceptionalCases(data);
        checkGcdZeroIdentitySafe(data);
        checkGcdSignInvariantPositiveInputs(data);
        checkLcmSignAndDivisibilityMetamorphic(data);
    }

    private static void checkLiftedTestOracles() {
        final int a = 30;
        final int b = 50;
        final int c = 77;

        assertEqualsInt("lifted-lcm-0-b", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, 50)");
        assertEqualsInt("lifted-lcm-a-0", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(30, 0)");
        assertEqualsInt("lifted-lcm-1-b", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, 50)");
        assertEqualsInt("lifted-lcm-a-1", a, MathUtils.lcm(a, 1), "MathUtils.lcm(30, 1)");
        assertEqualsInt("lifted-lcm-a-b", 150, MathUtils.lcm(a, b), "MathUtils.lcm(30, 50)");
        assertEqualsInt("lifted-lcm-na-b", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(-30, 50)");
        assertEqualsInt("lifted-lcm-a-nb", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(30, -50)");
        assertEqualsInt("lifted-lcm-na-nb", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(-30, -50)");
        assertEqualsInt("lifted-lcm-a-c", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(30, 77)");
        assertEqualsInt("lifted-lcm-power2-factorized", (1 << 20) * 15,
                MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5),
                "MathUtils.lcm((1<<20)*3, (1<<20)*5)");
        assertEqualsInt("lifted-lcm-0-0", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        // Trusted oracle copied verbatim from the failing test: these inputs must throw ArithmeticException.
        // This specifically catches the buggy behavior where deleting/omitting the throw returns Integer.MIN_VALUE instead.
        assertThrowsArithmetic("lifted-lcm-min-1", Integer.MIN_VALUE, 1);
        assertThrowsArithmetic("lifted-lcm-min-1sh20", Integer.MIN_VALUE, 1 << 20);
        assertThrowsArithmetic("lifted-lcm-max-maxm1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void checkGeneralizedKnownExceptionalCases(FuzzedDataProvider data) {
        int k;
        try {
            k = data.consumeInt(0, 30);
        } catch (Exception e) {
            return;
        }

        int b = 1 << k;

        // Oracle from construction: lcm(Integer.MIN_VALUE, 2^k) == abs(Integer.MIN_VALUE), which is not representable as nonnegative int.
        // The trusted failing test already pins this for k=0 and k=20; all powers of two preserve the same documented exceptional condition.
        assertThrowsArithmetic("constructed-lcm-min-pow2", Integer.MIN_VALUE, b);

        // Symmetry of lcm over arguments is part of the mathematical contract exercised by the test's exact-value checks.
        assertThrowsArithmetic("constructed-lcm-pow2-min", b, Integer.MIN_VALUE);
    }

    private static void checkGcdZeroIdentitySafe(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
            if (a == Integer.MIN_VALUE) {
                return;
            }
        } catch (Exception e) {
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
                    "relation gcd-zero-identity-safe violated: a=" + a
                            + " gcd(a,0)=" + r1
                            + " gcd(0,a)=" + r2
                            + " expected=" + expected);
        }
    }

    private static void checkGcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
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
                    "relation gcd-sign-invariant-positive-inputs violated: a=" + a
                            + " b=" + b
                            + " values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void checkLcmSignAndDivisibilityMetamorphic(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 1_000_000);
            b = data.consumeInt(1, 1_000_000);
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

        // Contract guarantee asserted: the lifted test checks lcm ignores operand signs.
        // A patch that merely deletes the overflow throw could still silently return a negative result; this post-condition catches that.
        if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
            throw new FuzzerSecurityIssueLow(
                    "relation lcm-sign-invariant violated: a=" + a
                            + " b=" + b
                            + " values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }

        // Contract guarantee asserted: for nonzero valid inputs, lcm is a common multiple, so it must be divisible by each operand.
        // This is an observable post-condition on the real API result and catches silent wrong values from throw-deleting patches.
        if (lpp <= 0 || (lpp % a) != 0 || (lpp % b) != 0) {
            throw new FuzzerSecurityIssueLow(
                    "relation lcm-common-multiple violated: a=" + a
                            + " b=" + b
                            + " lcm=" + lpp
                            + " modA=" + (lpp % a)
                            + " modB=" + (lpp % b));
        }
    }

    private static void assertThrowsArithmetic(String oracleId, int x, int y) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.lcm("
                            + x + ", " + y + ") but got value=" + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual, String call) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: call=" + call
                            + " expected=" + expected
                            + " actual=" + actual);
        }
    }
}