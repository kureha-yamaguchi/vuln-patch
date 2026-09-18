package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int a = 30;
        final int b = 50;
        final int c = 77;

        checkEquals("lifted-lcm-0b", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, 50)");
        checkEquals("lifted-lcm-a0", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(30, 0)");
        checkEquals("lifted-lcm-1b", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, 50)");
        checkEquals("lifted-lcm-a1", a, MathUtils.lcm(a, 1), "MathUtils.lcm(30, 1)");
        checkEquals("lifted-lcm-ab", 150, MathUtils.lcm(a, b), "MathUtils.lcm(30, 50)");
        checkEquals("lifted-lcm-nab", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(-30, 50)");
        checkEquals("lifted-lcm-anb", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(30, -50)");
        checkEquals("lifted-lcm-nanb", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(-30, -50)");
        checkEquals("lifted-lcm-ac", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(30, 77)");
        checkEquals("lifted-lcm-powers", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5),
                "MathUtils.lcm((1<<20)*3, (1<<20)*5)");
        checkEquals("lifted-lcm-00", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        expectArithmetic("lifted-ex-min-1", Integer.MIN_VALUE, 1);
        expectArithmetic("lifted-ex-min-2pow20", Integer.MIN_VALUE, 1 << 20);
        expectArithmetic("lifted-ex-max-maxm1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);

        int zeroIdentityA = data.consumeInt();
        if (zeroIdentityA != Integer.MIN_VALUE) {
            try {
                int r1 = MathUtils.gcd(zeroIdentityA, 0);
                int r2 = MathUtils.gcd(0, zeroIdentityA);
                int expected = Math.abs(zeroIdentityA);
                // Contract exercised by the trusted tests: gcd(a,0)=|a| and gcd(0,a)=|a| for all a whose abs is representable.
                if (r1 != expected || r2 != expected) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "relation gcd-zero-identity-safe violated: a=" + zeroIdentityA
                                    + ", gcd(a,0)=" + r1 + ", gcd(0,a)=" + r2 + ", expected=" + expected);
                }
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
        }

        int signA = data.consumeInt(1, 1_000_000);
        int signB = data.consumeInt(1, 1_000_000);
        try {
            int gpp = MathUtils.gcd(signA, signB);
            int gnp = MathUtils.gcd(-signA, signB);
            int gpn = MathUtils.gcd(signA, -signB);
            int gnn = MathUtils.gcd(-signA, -signB);
            // gcd ignores operand sign; the trusted tests for this bug family assert the same sign-invariance pattern.
            if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation gcd-sign-invariant-positive-inputs violated: a=" + signA + ", b=" + signB
                                + ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int lcmA = data.consumeInt(1, 1_000_000);
        int lcmB = data.consumeInt(1, 1_000_000);
        try {
            int lpp = MathUtils.lcm(lcmA, lcmB);
            int lnp = MathUtils.lcm(-lcmA, lcmB);
            int lpn = MathUtils.lcm(lcmA, -lcmB);
            int lnn = MathUtils.lcm(-lcmA, -lcmB);
            // lcm is defined via absolute value in the real implementation and the trusted test fixes that sign changes must not change the nonnegative result.
            // A patch that only deletes/guards the overflow throw can still silently return a sign-dependent wrong value; this post-condition catches that.
            if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation lcm-sign-invariant-positive-inputs violated: a=" + lcmA + ", b=" + lcmB
                                + ", values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int multA = data.consumeInt(1, 10_000);
        int multB = data.consumeInt(1, 10_000);
        try {
            int gcd = MathUtils.gcd(multA, multB);
            int lcm = MathUtils.lcm(multA, multB);
            long lhs = (long) gcd * (long) lcm;
            long rhs = (long) multA * (long) multB;
            // For positive integers, gcd(a,b) * lcm(a,b) == a*b. We fence to small positive inputs so both sides are exact and within long.
            if (lhs != rhs) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation gcd-lcm-product violated: a=" + multA + ", b=" + multB
                                + ", gcd=" + gcd + ", lcm=" + lcm + ", lhs=" + lhs + ", rhs=" + rhs);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual, String what) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String oracleId, int x, int y) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: MathUtils.lcm(" + x + ", " + y
                            + ") expected ArithmeticException actual=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}