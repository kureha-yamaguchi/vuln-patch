package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmOracles();
        runLiftedExceptionOracles();

        runGcdZeroIdentitySafe(data);
        runGcdSignInvariantPositiveInputs(data);
        runLcmSignInvariantPositiveInputs(data);
        runLcmDivisibilityAndSymmetry(data);
    }

    private static void runLiftedLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("[oracle:lcm-0-b]", 0, MathUtils.lcm(0, b));
        assertEqualsInt("[oracle:lcm-a-0]", 0, MathUtils.lcm(a, 0));
        assertEqualsInt("[oracle:lcm-1-b]", b, MathUtils.lcm(1, b));
        assertEqualsInt("[oracle:lcm-a-1]", a, MathUtils.lcm(a, 1));
        assertEqualsInt("[oracle:lcm-a-b]", 150, MathUtils.lcm(a, b));
        assertEqualsInt("[oracle:lcm--a-b]", 150, MathUtils.lcm(-a, b));
        assertEqualsInt("[oracle:lcm-a--b]", 150, MathUtils.lcm(a, -b));
        assertEqualsInt("[oracle:lcm--a--b]", 150, MathUtils.lcm(-a, -b));
        assertEqualsInt("[oracle:lcm-a-c]", 2310, MathUtils.lcm(a, c));
        assertEqualsInt("[oracle:lcm-power-two-factor]", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        assertEqualsInt("[oracle:lcm-0-0]", 0, MathUtils.lcm(0, 0));
    }

    private static void runLiftedExceptionOracles() {
        expectArithmeticException("[oracle:lcm-min-1]", Integer.MIN_VALUE, 1);
        expectArithmeticException("[oracle:lcm-min-1shift20]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticException("[oracle:lcm-max-maxminus1]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void runGcdZeroIdentitySafe(FuzzedDataProvider data) {
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
        try {
            r1 = MathUtils.gcd(a, 0);
        } catch (Throwable t) {
            return;
        }

        int r2;
        try {
            r2 = MathUtils.gcd(0, a);
        } catch (Throwable t) {
            return;
        }

        int expected = Math.abs(a);
        if (r1 != expected || r2 != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation gcd-zero-identity-safe violated: a=" + a +
                ", gcd(a,0)=" + r1 +
                ", gcd(0,a)=" + r2 +
                ", expected=" + expected);
        }
    }

    private static void runGcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
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
                "[oracle:unnamed-check] relation gcd-sign-invariant-positive-inputs violated: a=" + a +
                ", b=" + b +
                ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void runLcmSignInvariantPositiveInputs(FuzzedDataProvider data) {
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
        try {
            lpp = MathUtils.lcm(a, b);
            lnp = MathUtils.lcm(-a, b);
            lpn = MathUtils.lcm(a, -b);
            lnn = MathUtils.lcm(-a, -b);
        } catch (Throwable t) {
            return;
        }

        if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation lcm-sign-invariant-positive-inputs violated: a=" + a +
                ", b=" + b +
                ", values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }
    }

    private static void runLcmDivisibilityAndSymmetry(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-1_000_000, 1_000_000);
            b = data.consumeInt(-1_000_000, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        if (a == 0 || b == 0) {
            return;
        }

        int l1;
        int l2;
        try {
            l1 = MathUtils.lcm(a, b);
            l2 = MathUtils.lcm(b, a);
        } catch (Throwable t) {
            return;
        }

        if (l1 != l2) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation lcm-symmetry violated: a=" + a +
                ", b=" + b +
                ", lcm(a,b)=" + l1 +
                ", lcm(b,a)=" + l2);
        }

        // Contract-based post-condition: lcm is a common multiple of both inputs.
        // A throw-deleting or wrong-value patch can return a non-multiple while still not throwing.
        if (l1 < 0 || l1 % Math.abs(a) != 0 || l1 % Math.abs(b) != 0) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation lcm-common-multiple violated: a=" + a +
                ", b=" + b +
                ", lcm=" + l1);
        }

        int g;
        try {
            g = MathUtils.gcd(a, b);
        } catch (Throwable t) {
            return;
        }

        long left = ((long) Math.abs(g)) * ((long) l1);
        long right = ((long) Math.abs(a)) * ((long) Math.abs(b));
        if (left != right) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation gcd-lcm-product violated: a=" + a +
                ", b=" + b +
                ", gcd=" + g +
                ", lcm=" + l1 +
                ", lhs=" + left +
                ", rhs=" + right);
        }
    }

    private static void expectArithmeticException(String oracleId, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException but got value " + actual +
                " for MathUtils.lcm(" + a + ", " + b + ")");
        } catch (ArithmeticException expected) {
            // expected
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}