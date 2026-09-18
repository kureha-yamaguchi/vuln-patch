package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedLcmOracles();
        checkLiftedExceptionalOracles();
        checkGcdZeroIdentitySafe(data);
        checkGcdSignInvariantPositiveInputs(data);
        checkLcmGcdProductRelation(data);
    }

    private static void checkLiftedLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("[oracle:lcm-0-b]", 0, MathUtils.lcm(0, b));
        assertEqualsInt("[oracle:lcm-a-0]", 0, MathUtils.lcm(a, 0));
        assertEqualsInt("[oracle:lcm-1-b]", b, MathUtils.lcm(1, b));
        assertEqualsInt("[oracle:lcm-a-1]", a, MathUtils.lcm(a, 1));
        assertEqualsInt("[oracle:lcm-a-b]", 150, MathUtils.lcm(a, b));
        assertEqualsInt("[oracle:lcm-neg-a-b]", 150, MathUtils.lcm(-a, b));
        assertEqualsInt("[oracle:lcm-a-neg-b]", 150, MathUtils.lcm(a, -b));
        assertEqualsInt("[oracle:lcm-neg-a-neg-b]", 150, MathUtils.lcm(-a, -b));
        assertEqualsInt("[oracle:lcm-a-c]", 2310, MathUtils.lcm(a, c));
        assertEqualsInt("[oracle:lcm-no-intermediate-overflow]", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        assertEqualsInt("[oracle:lcm-0-0]", 0, MathUtils.lcm(0, 0));
    }

    private static void checkLiftedExceptionalOracles() {
        expectArithmeticException("[oracle:lcm-min-1-throws]", Integer.MIN_VALUE, 1);
        expectArithmeticException("[oracle:lcm-min-1sh20-throws]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticException("[oracle:lcm-max-maxminus1-throws]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void checkGcdZeroIdentitySafe(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
        } catch (RuntimeException e) {
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
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation gcd-zero-identity-safe violated: a=" + a +
                ", gcd(a,0)=" + r1 +
                ", gcd(0,a)=" + r2 +
                ", expected=" + expected
            );
        }
    }

    private static void checkGcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, Integer.MAX_VALUE);
            b = data.consumeInt(1, Integer.MAX_VALUE);
        } catch (RuntimeException e) {
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
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation gcd-sign-invariant-positive-inputs violated: a=" + a +
                ", b=" + b +
                ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn
            );
        }
    }

    private static void checkLcmGcdProductRelation(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-1000, 1000);
            b = data.consumeInt(-1000, 1000);
        } catch (RuntimeException e) {
            return;
        }
        if (a == 0 || b == 0) {
            return;
        }

        int g;
        try {
            g = MathUtils.gcd(a, b);
        } catch (Exception e) {
            return;
        }

        int l;
        try {
            l = MathUtils.lcm(a, b);
        } catch (Exception e) {
            return;
        }

        long lhs = ((long) l) * ((long) g);
        long rhs = Math.abs(((long) a) * ((long) b));

        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] relation lcm-gcd-product violated: a=" + a +
                ", b=" + b +
                ", gcd=" + g +
                ", lcm=" + l +
                ", lhs=" + lhs +
                ", rhs=" + rhs
            );
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void expectArithmeticException(String oracleId, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException actualReturn=" + actual
            );
        } catch (ArithmeticException expected) {
        }
    }
}