package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestLcmOracles();
        relationGcdZeroIdentitySafe(data);
        relationGcdSignInvariantPositiveInputs(data);
        relationLcmSignAndProduct(data);
        relationLcmZeroAbsorbing(data);
        exercisePatchedPath(data);
    }

    private static void liftedTestLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("[oracle:testLcm-0-b]", 0, MathUtils.lcm(0, b));
        assertEqualsInt("[oracle:testLcm-a-0]", 0, MathUtils.lcm(a, 0));
        assertEqualsInt("[oracle:testLcm-1-b]", b, MathUtils.lcm(1, b));
        assertEqualsInt("[oracle:testLcm-a-1]", a, MathUtils.lcm(a, 1));
        assertEqualsInt("[oracle:testLcm-a-b]", 150, MathUtils.lcm(a, b));
        assertEqualsInt("[oracle:testLcm-negA-b]", 150, MathUtils.lcm(-a, b));
        assertEqualsInt("[oracle:testLcm-a-negB]", 150, MathUtils.lcm(a, -b));
        assertEqualsInt("[oracle:testLcm-negA-negB]", 150, MathUtils.lcm(-a, -b));
        assertEqualsInt("[oracle:testLcm-a-c]", 2310, MathUtils.lcm(a, c));
        assertEqualsInt("[oracle:testLcm-no-intermediate-overflow]", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        assertEqualsInt("[oracle:testLcm-0-0]", 0, MathUtils.lcm(0, 0));

        expectArithmeticException("[oracle:testLcm-minvalue-1]", Integer.MIN_VALUE, 1);
        expectArithmeticException("[oracle:testLcm-minvalue-1<<20]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticException("[oracle:testLcm-max-maxMinus1]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void expectArithmeticException(String oracleId, int x, int y) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException but got value actual=" + actual + " for lcm(" + x + "," + y + ")");
        } catch (ArithmeticException expected) {
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void relationGcdZeroIdentitySafe(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
            if (a == Integer.MIN_VALUE) {
                return;
            }
        } catch (Throwable t) {
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
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation gcd-zero-identity-safe violated: a=" + a + " gcd(a,0)=" + r1 + " gcd(0,a)=" + r2 + " expected=" + expected);
        }
    }

    private static void relationGcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
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
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation gcd-sign-invariant-positive-inputs violated: a=" + a + " b=" + b + " values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void relationLcmSignAndProduct(FuzzedDataProvider data) {
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
        int g;
        try {
            lpp = MathUtils.lcm(a, b);
            lnp = MathUtils.lcm(-a, b);
            lpn = MathUtils.lcm(a, -b);
            lnn = MathUtils.lcm(-a, -b);
            g = MathUtils.gcd(a, b);
        } catch (Throwable t) {
            return;
        }

        if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation lcm-sign-invariant violated: a=" + a + " b=" + b + " values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }

        long lhs = (long) g * (long) lpp;
        long rhs = (long) a * (long) b;
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation gcd-lcm-product violated: a=" + a + " b=" + b + " gcd=" + g + " lcm=" + lpp + " lhs=" + lhs + " rhs=" + rhs);
        }

        if (lpp <= 0 || (lpp % a) != 0 || (lpp % b) != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation lcm-postcondition violated: a=" + a + " b=" + b + " lcm=" + lpp);
        }
    }

    private static void relationLcmZeroAbsorbing(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
        } catch (Throwable t) {
            return;
        }

        int r1;
        int r2;
        try {
            r1 = MathUtils.lcm(a, 0);
            r2 = MathUtils.lcm(0, a);
        } catch (Throwable t) {
            return;
        }

        if (r1 != 0 || r2 != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation lcm-zero-absorbing violated: a=" + a + " lcm(a,0)=" + r1 + " lcm(0,a)=" + r2);
        }
    }

    private static void exercisePatchedPath(FuzzedDataProvider data) {
        int x;
        int y;
        try {
            x = data.consumeInt();
            y = data.consumeInt();
        } catch (Throwable t) {
            return;
        }

        try {
            MathUtils.lcm(x, y);
        } catch (Throwable t) {
        }

        try {
            MathUtils.gcd(x, y);
        } catch (Throwable t) {
        }
    }
}