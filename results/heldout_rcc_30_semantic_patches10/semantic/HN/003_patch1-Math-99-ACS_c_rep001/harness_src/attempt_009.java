package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestLcmOracles();
        relationGcdZeroIdentitySafe(data);
        relationGcdSignInvariantPositiveInputs(data);
        relationLcmSignInvariantAndDivisibility(data);
        relationLcmWithOneAndZero(data);
    }

    private static void liftedTestLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("lcm-0-b", 0, MathUtils.lcm(0, b));
        assertEqualsInt("lcm-a-0", 0, MathUtils.lcm(a, 0));
        assertEqualsInt("lcm-1-b", b, MathUtils.lcm(1, b));
        assertEqualsInt("lcm-a-1", a, MathUtils.lcm(a, 1));
        assertEqualsInt("lcm-a-b", 150, MathUtils.lcm(a, b));
        assertEqualsInt("lcm--a-b", 150, MathUtils.lcm(-a, b));
        assertEqualsInt("lcm-a--b", 150, MathUtils.lcm(a, -b));
        assertEqualsInt("lcm--a--b", 150, MathUtils.lcm(-a, -b));
        assertEqualsInt("lcm-a-c", 2310, MathUtils.lcm(a, c));
        assertEqualsInt("lcm-powers-of-two-factor", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        assertEqualsInt("lcm-0-0", 0, MathUtils.lcm(0, 0));

        expectArithmeticException("lcm-min-1", Integer.MIN_VALUE, 1);
        expectArithmeticException("lcm-min-1sh20", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticException("lcm-max-maxm1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
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
                ", gcd(a,0)=" + r1 +
                ", gcd(0,a)=" + r2 +
                ", expected=" + expected);
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
            throw new FuzzerSecurityIssueLow(
                "relation gcd-sign-invariant-positive-inputs violated: a=" + a +
                ", b=" + b +
                ", values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }
    }

    private static void relationLcmSignInvariantAndDivisibility(FuzzedDataProvider data) {
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
                "relation lcm-sign-invariant violated: a=" + a +
                ", b=" + b +
                ", values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }

        // Contract justification: lcm is a common multiple, so for nonzero a,b the result must be divisible by both.
        // A throw-deleting patch around MIN_VALUE handling can silently return a negative/non-representable value that breaks this observable property.
        if (lpp <= 0 || (lpp % a) != 0 || (lpp % b) != 0) {
            throw new FuzzerSecurityIssueLow(
                "relation lcm-divisibility violated: a=" + a +
                ", b=" + b +
                ", lcm=" + lpp +
                ", lcm%a=" + (lpp % a) +
                ", lcm%b=" + (lpp % b));
        }
    }

    private static void relationLcmWithOneAndZero(FuzzedDataProvider data) {
        int x;
        try {
            x = data.consumeInt(-1_000_000, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        int withZeroLeft;
        int withZeroRight;
        try {
            withZeroLeft = MathUtils.lcm(0, x);
            withZeroRight = MathUtils.lcm(x, 0);
        } catch (Throwable t) {
            return;
        }

        if (withZeroLeft != 0 || withZeroRight != 0) {
            throw new FuzzerSecurityIssueLow(
                "relation lcm-zero-annihilator violated: x=" + x +
                ", lcm(0,x)=" + withZeroLeft +
                ", lcm(x,0)=" + withZeroRight);
        }

        if (x == Integer.MIN_VALUE) {
            return;
        }

        int withOneLeft;
        int withOneRight;
        try {
            withOneLeft = MathUtils.lcm(1, x);
            withOneRight = MathUtils.lcm(x, 1);
        } catch (Throwable t) {
            return;
        }

        int expected = Math.abs(x);
        if (withOneLeft != expected || withOneRight != expected) {
            throw new FuzzerSecurityIssueLow(
                "relation lcm-one-identity violated: x=" + x +
                ", lcm(1,x)=" + withOneLeft +
                ", lcm(x,1)=" + withOneRight +
                ", expected=" + expected);
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, int x, int y) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual +
                " for inputs=" + x + "," + y);
        } catch (ArithmeticException expected) {
        }
    }
}