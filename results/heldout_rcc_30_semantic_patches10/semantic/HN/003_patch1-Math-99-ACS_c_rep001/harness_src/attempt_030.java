package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkMandatoryTriggerOracle();
        checkGcdZeroIdentitySafe(data);
        checkGcdSignInvariantPositiveInputs(data);
        checkLcmSignAndZeroOracles(data);
        checkLcmGcdProductRelation(data);
    }

    private static void checkLiftedTestOracles() {
        final int a = 30;
        final int b = 50;
        final int c = 77;

        assertEqualsInt("[oracle:testLcm-0-b]", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, 50)");
        assertEqualsInt("[oracle:testLcm-a-0]", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(30, 0)");
        assertEqualsInt("[oracle:testLcm-1-b]", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, 50)");
        assertEqualsInt("[oracle:testLcm-a-1]", a, MathUtils.lcm(a, 1), "MathUtils.lcm(30, 1)");
        assertEqualsInt("[oracle:testLcm-a-b]", 150, MathUtils.lcm(a, b), "MathUtils.lcm(30, 50)");
        assertEqualsInt("[oracle:testLcm--a-b]", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(-30, 50)");
        assertEqualsInt("[oracle:testLcm-a--b]", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(30, -50)");
        assertEqualsInt("[oracle:testLcm--a--b]", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(-30, -50)");
        assertEqualsInt("[oracle:testLcm-a-c]", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(30, 77)");
        assertEqualsInt("[oracle:testLcm-no-intermediate-overflow]", (1 << 20) * 15,
                MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5), "MathUtils.lcm((1<<20)*3, (1<<20)*5)");
        assertEqualsInt("[oracle:testLcm-0-0]", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        expectArithmeticException("[oracle:testLcm-min-1]", Integer.MIN_VALUE, 1);
        expectArithmeticException("[oracle:testLcm-min-1sh20]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticException("[oracle:testLcm-max-maxMinus1]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void checkMandatoryTriggerOracle() {
        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
        } catch (ArithmeticException expected) {
            return;
        }
        throw new FuzzerSecurityIssueLow(
                "[oracle:ground-truth-trigger] semantic mismatch: MathUtils.lcm(Integer.MIN_VALUE, 1) did not throw ArithmeticException");
    }

    private static void checkGcdZeroIdentitySafe(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
        } catch (Throwable t) {
            return;
        }
        if (a == Integer.MIN_VALUE) {
            return;
        }

        int r1;
        int r2;
        try {
            r1 = MathUtils.gcd(a, 0);
            r2 = MathUtils.gcd(0, a);
        } catch (Throwable t) {
            return;
        }

        int expected = Math.abs(a);
        if (r1 != expected || r2 != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation gcd-zero-identity-safe violated: a=" + a + ", gcd(a,0)=" + r1 + ", gcd(0,a)=" + r2 + ", expected=" + expected);
        }
    }

    private static void checkGcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 1_000_000);
            b = data.consumeInt(1, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        int gpp;
        int gnp;
        int gpn;
        int gnn;
        try {
            gpp = MathUtils.gcd(a, b);
            gnp = MathUtils.gcd(-a, b);
            gpn = MathUtils.gcd(a, -b);
            gnn = MathUtils.gcd(-a, -b);
        } catch (Throwable t) {
            return;
        }

        if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation gcd-sign-invariant-positive-inputs violated: a=" + a + ", b=" + b + ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void checkLcmSignAndZeroOracles(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 1_000_000);
            b = data.consumeInt(1, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        int lpp;
        int lnp;
        int lpn;
        int lnn;
        int l0b;
        int la0;
        try {
            lpp = MathUtils.lcm(a, b);
            lnp = MathUtils.lcm(-a, b);
            lpn = MathUtils.lcm(a, -b);
            lnn = MathUtils.lcm(-a, -b);
            l0b = MathUtils.lcm(0, b);
            la0 = MathUtils.lcm(a, 0);
        } catch (Throwable t) {
            return;
        }

        if (l0b != 0 || la0 != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation lcm-zero-absorbing violated: a=" + a + ", b=" + b + ", lcm(0,b)=" + l0b + ", lcm(a,0)=" + la0);
        }

        if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation lcm-sign-invariant violated: a=" + a + ", b=" + b + ", values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }
    }

    private static void checkLcmGcdProductRelation(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-1_000_000, 1_000_000);
            b = data.consumeInt(-1_000_000, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        int g;
        int l;
        try {
            g = MathUtils.gcd(a, b);
            l = MathUtils.lcm(a, b);
        } catch (Throwable t) {
            return;
        }

        long lhs = ((long) g) * ((long) l);
        long rhs = Math.abs(((long) a) * ((long) b));

        /* Contract justification:
         * For integer arithmetic, gcd(a,b) * lcm(a,b) = |a*b| whenever both gcd and lcm are defined.
         * This is an observable post-condition over two real API calls; a patch that merely deletes the
         * ArithmeticException path or returns a silently wrong lcm value breaks this equality. */
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] relation lcm-gcd-product violated: a=" + a + ", b=" + b + ", gcd=" + g + ", lcm=" + l + ", gcd*lcm=" + lhs + ", abs(a*b)=" + rhs);
        }
    }

    private static void expectArithmeticException(String oracleId, int a, int b) {
        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException expected) {
            return;
        }
        throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException from MathUtils.lcm(" + a + ", " + b + ")");
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }
}