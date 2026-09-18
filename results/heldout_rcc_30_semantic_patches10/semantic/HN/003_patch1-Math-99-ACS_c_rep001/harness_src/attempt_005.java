package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    /*__vpCause*/ private static Throwable __vpCause = null;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmOracles();
        runLiftedExceptionOracles();

        runGcdZeroIdentitySafe(data);
        runGcdSignInvariantPositiveInputs(data);
        runLcmSignInvariantAndSymmetry(data);
        runLcmConstructedFromKnownAnswer(data);
    }

    private static void runLiftedLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        checkEquals("lifted-lcm-0-b", 0, MathUtils.lcm(0, b));
        checkEquals("lifted-lcm-a-0", 0, MathUtils.lcm(a, 0));
        checkEquals("lifted-lcm-1-b", b, MathUtils.lcm(1, b));
        checkEquals("lifted-lcm-a-1", a, MathUtils.lcm(a, 1));
        checkEquals("lifted-lcm-a-b", 150, MathUtils.lcm(a, b));
        checkEquals("lifted-lcm--a-b", 150, MathUtils.lcm(-a, b));
        checkEquals("lifted-lcm-a--b", 150, MathUtils.lcm(a, -b));
        checkEquals("lifted-lcm--a--b", 150, MathUtils.lcm(-a, -b));
        checkEquals("lifted-lcm-a-c", 2310, MathUtils.lcm(a, c));
        checkEquals("lifted-lcm-no-intermediate-overflow", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        checkEquals("lifted-lcm-0-0", 0, MathUtils.lcm(0, 0));
    }

    private static void runLiftedExceptionOracles() {
        expectArithmeticException("lifted-exn-min-1", Integer.MIN_VALUE, 1);
        expectArithmeticException("lifted-exn-min-shift20", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticException("lifted-exn-max-maxminus1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
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
                "relation gcd-zero-identity-safe violated: a=" + a +
                ", gcd(a,0)=" + r1 + ", gcd(0,a)=" + r2 + ", expected=" + expected);
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
                "relation gcd-sign-invariant-positive-inputs violated: a=" + a +
                ", b=" + b + ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void runLcmSignInvariantAndSymmetry(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 1000);
            b = data.consumeInt(1, 1000);
        } catch (Throwable t) {
            return;
        }

        int lpp;
        int lnp;
        int lpn;
        int lnn;
        int lswap;
        try {
            lpp = MathUtils.lcm(a, b);
            lnp = MathUtils.lcm(-a, b);
            lpn = MathUtils.lcm(a, -b);
            lnn = MathUtils.lcm(-a, -b);
            lswap = MathUtils.lcm(b, a);
        } catch (Throwable t) {
            return;
        }

        /* Contract visible in the lifted test: lcm ignores operand signs and is symmetric
           on valid non-overflowing inputs. A patch that merely suppresses the exception path
           but corrupts the arithmetic would break at least one of these equalities. */
        if (!(lpp == lnp && lpp == lpn && lpp == lnn && lpp == lswap)) {
            throw new FuzzerSecurityIssueLow(
                "relation lcm-sign-invariant-and-symmetric violated: a=" + a +
                ", b=" + b + ", values=" + lpp + "," + lnp + "," + lpn + "," + lnn + "," + lswap);
        }
    }

    private static void runLcmConstructedFromKnownAnswer(FuzzedDataProvider data) {
        int k;
        int x;
        int y;
        try {
            k = data.consumeInt(1, 1000);
            x = data.consumeInt(1, 1000);
            y = data.consumeInt(1, 1000);
        } catch (Throwable t) {
            return;
        }

        int a = k * x;
        int b = k * y;
        if (a == 0 || b == 0) {
            return;
        }

        int lcmXY;
        int gcdXY;
        int lhs;
        int rhs;
        try {
            gcdXY = MathUtils.gcd(x, y);
            if (gcdXY <= 0) {
                return;
            }
            lcmXY = MathUtils.lcm(x, y);
            lhs = MathUtils.lcm(a, b);
            rhs = k * lcmXY;
        } catch (Throwable t) {
            return;
        }

        /* Oracle from construction: a=k*x and b=k*y with positive moderate values, so for any
           correct implementation lcm(k*x, k*y) == k*lcm(x,y). The expected rhs is trusted because
           we chose k first and both sides are computed through real library calls on valid inputs. */
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                "relation lcm-constructed-from-known-answer violated: k=" + k +
                ", x=" + x + ", y=" + y + ", a=" + a + ", b=" + b +
                ", lhs=" + lhs + ", rhs=" + rhs + ", gcdXY=" + gcdXY + ", lcmXY=" + lcmXY);
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, int a, int b) {
        boolean threw = false;
        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException expected) { __vpCause = expected;
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.lcm(" +
                a + "," + b + ")", __vpCause);
        }
    }
}