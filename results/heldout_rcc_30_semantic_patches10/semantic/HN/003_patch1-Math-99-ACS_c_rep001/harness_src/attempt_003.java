package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmOracles();
        runGeneralizedLcmChecks(data);
        runGcdRelations(data);
    }

    private static void runLiftedLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertIntEquals("lcm-0-b", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, " + b + ")");
        assertIntEquals("lcm-a-0", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(" + a + ", 0)");
        assertIntEquals("lcm-1-b", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, " + b + ")");
        assertIntEquals("lcm-a-1", a, MathUtils.lcm(a, 1), "MathUtils.lcm(" + a + ", 1)");
        assertIntEquals("lcm-a-b", 150, MathUtils.lcm(a, b), "MathUtils.lcm(" + a + ", " + b + ")");
        assertIntEquals("lcm--a-b", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(" + (-a) + ", " + b + ")");
        assertIntEquals("lcm-a--b", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(" + a + ", " + (-b) + ")");
        assertIntEquals("lcm--a--b", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(" + (-a) + ", " + (-b) + ")");
        assertIntEquals("lcm-a-c", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(" + a + ", " + c + ")");
        assertIntEquals("lcm-shifted", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5),
                "MathUtils.lcm(" + ((1 << 20) * 3) + ", " + ((1 << 20) * 5) + ")");
        assertIntEquals("lcm-0-0", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        expectArithmetic("lcm-min-1", Integer.MIN_VALUE, 1);
        expectArithmetic("lcm-min-shift", Integer.MIN_VALUE, 1 << 20);
        expectArithmetic("lcm-max-maxm1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void runGeneralizedLcmChecks(FuzzedDataProvider data) {
        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);

        try {
            MathUtils.gcd(x, y);
        } catch (Throwable t) {
            return;
        }

        try {
            MathUtils.lcm(x, y);
        } catch (Throwable t) {
            return;
        }

        // Contract visible in lcm/gcd code and trusted test: lcm ignores operand signs and returns a nonnegative least common multiple.
        // A throw-deleting or wrong-arithmetic patch would often break this observable equality across sign variants.
        int lxy;
        int lnxY;
        int lxNy;
        int lnxNy;
        try {
            lxy = MathUtils.lcm(x, y);
            lnxY = MathUtils.lcm(safeNegate(x), y);
            lxNy = MathUtils.lcm(x, safeNegate(y));
            lnxNy = MathUtils.lcm(safeNegate(x), safeNegate(y));
        } catch (Throwable t) {
            return;
        }
        if (!(lxy == lnxY && lxy == lxNy && lxy == lnxNy)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-sign-invariant] semantic mismatch: x=" + x + " y=" + y
                            + " lcm(x,y)=" + lxy
                            + " lcm(-x,y)=" + lnxY
                            + " lcm(x,-y)=" + lxNy
                            + " lcm(-x,-y)=" + lnxNy);
        }

        int positive = data.consumeInt(1, 1_000_000);
        int factor1 = data.consumeInt(1, 1000);
        int factor2 = data.consumeInt(1, 1000);
        int leftA = positive * factor1;
        int leftB = positive * factor2;

        int gcdVal;
        int lcmVal;
        try {
            gcdVal = MathUtils.gcd(leftA, leftB);
            lcmVal = MathUtils.lcm(leftA, leftB);
        } catch (Throwable t) {
            return;
        }

        // For positive inputs whose products fit in int, gcd(a,b) * lcm(a,b) == a * b.
        // This is a standard arithmetic identity for correct gcd/lcm implementations and directly observes wrong lcm output even when no exception is thrown.
        long lhs = ((long) gcdVal) * ((long) lcmVal);
        long rhs = ((long) leftA) * ((long) leftB);
        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:gcd-lcm-product] metamorphic violation: a=" + leftA + " b=" + leftB
                            + " gcd=" + gcdVal + " lcm=" + lcmVal
                            + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void runGcdRelations(FuzzedDataProvider data) {
        int a = data.consumeInt();
        if (a != Integer.MIN_VALUE) {
            int r1;
            int r2;
            try {
                r1 = MathUtils.gcd(a, 0);
                r2 = MathUtils.gcd(0, a);
            } catch (Throwable t) {
                return;
            }
            int expected = Math.abs(a);
            // Documented/visible behavior from code path and candidate relation: gcd(a,0) = |a| and gcd(0,a) = |a| for all a != Integer.MIN_VALUE.
            if (r1 != expected || r2 != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation gcd-zero-identity-safe violated: a=" + a
                                + ", gcd(a,0)=" + r1
                                + ", gcd(0,a)=" + r2
                                + ", expected=" + expected);
            }
        }

        int p = data.consumeInt(1, 1_000_000);
        int q = data.consumeInt(1, 1_000_000);
        int gpp;
        int gnp;
        int gpn;
        int gnn;
        try {
            gpp = MathUtils.gcd(p, q);
            gnp = MathUtils.gcd(-p, q);
            gpn = MathUtils.gcd(p, -q);
            gnn = MathUtils.gcd(-p, -q);
        } catch (Throwable t) {
            return;
        }
        // Trusted test for gcd/lcm sign handling requires sign-invariance: flipping operand signs must not change gcd for positive magnitudes.
        if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation gcd-sign-invariant-positive-inputs violated: values "
                            + gpp + "," + gnp + "," + gpn + "," + gnn
                            + " for a=" + p + ", b=" + q);
        }
    }

    private static int safeNegate(int v) {
        return v == Integer.MIN_VALUE ? Integer.MIN_VALUE : -v;
    }

    private static void expectArithmetic(String oracleId, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.lcm("
                            + a + ", " + b + ") but got value=" + actual);
        } catch (ArithmeticException expected) {
        }
    }

    private static void assertIntEquals(String oracleId, int expected, int actual, String call) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call
                            + " expected=" + expected + " actual=" + actual);
        }
    }
}