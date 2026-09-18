package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        assertLcmEquals("lifted-0-b", 0, 0, b);
        assertLcmEquals("lifted-a-0", 0, a, 0);
        assertLcmEquals("lifted-1-b", b, 1, b);
        assertLcmEquals("lifted-a-1", a, a, 1);
        assertLcmEquals("lifted-a-b", 150, a, b);
        assertLcmEquals("lifted-neg-a-b", 150, -a, b);
        assertLcmEquals("lifted-a-neg-b", 150, a, -b);
        assertLcmEquals("lifted-neg-a-neg-b", 150, -a, -b);
        assertLcmEquals("lifted-a-c", 2310, a, c);
        assertLcmEquals("lifted-large-common-power-two", (1 << 20) * 15, (1 << 20) * 3, (1 << 20) * 5);
        assertLcmEquals("lifted-0-0", 0, 0, 0);

        assertLcmThrowsArithmetic("lifted-min-1-throws", Integer.MIN_VALUE, 1);
        assertLcmThrowsArithmetic("lifted-min-1sh20-throws", Integer.MIN_VALUE, 1 << 20);
        assertLcmThrowsArithmetic("lifted-max-maxminus1-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);

        gcdZeroIdentitySafe(data);
        gcdSignInvariantPositiveInputs(data);
        lcmTrustedMetamorphicChecks(data);
    }

    private static void assertLcmEquals(String id, int expected, int x, int y) {
        final int actual;
        try {
            actual = MathUtils.lcm(x, y);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: unexpected exception from MathUtils.lcm(" + x + "," + y + ")",
                t
            );
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: MathUtils.lcm(" + x + "," + y + ") expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertLcmThrowsArithmetic(String id, int x, int y) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but MathUtils.lcm(" + x + "," + y + ") returned " + actual
            );
        } catch (ArithmeticException expected) {
        }
    }

    private static void gcdZeroIdentitySafe(FuzzedDataProvider data) {
        int a;
        try {
            a = data.consumeInt();
        } catch (Throwable t) {
            return;
        }
        if (a == Integer.MIN_VALUE) {
            return;
        }

        final int r1;
        try {
            r1 = MathUtils.gcd(a, 0);
        } catch (Throwable t) {
            return;
        }

        final int r2;
        try {
            r2 = MathUtils.gcd(0, a);
        } catch (Throwable t) {
            return;
        }

        int expected = Math.abs(a);
        if (r1 != expected || r2 != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:gcd-zero-identity-safe] relation gcd-zero-identity-safe violated: a=" + a +
                " gcd(a,0)=" + r1 + " gcd(0,a)=" + r2 + " expected=" + expected
            );
        }
    }

    private static void gcdSignInvariantPositiveInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 1_000_000);
            b = data.consumeInt(1, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        final int gpp;
        try {
            gpp = MathUtils.gcd(a, b);
        } catch (Throwable t) {
            return;
        }

        final int gnp;
        try {
            gnp = MathUtils.gcd(-a, b);
        } catch (Throwable t) {
            return;
        }

        final int gpn;
        try {
            gpn = MathUtils.gcd(a, -b);
        } catch (Throwable t) {
            return;
        }

        final int gnn;
        try {
            gnn = MathUtils.gcd(-a, -b);
        } catch (Throwable t) {
            return;
        }

        if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:gcd-sign-invariant-positive-inputs] relation gcd-sign-invariant-positive-inputs violated: a=" + a +
                " b=" + b + " values=" + gpp + "," + gnp + "," + gpn + "," + gnn
            );
        }
    }

    private static void lcmTrustedMetamorphicChecks(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(1, 10_000);
            b = data.consumeInt(1, 10_000);
        } catch (Throwable t) {
            return;
        }

        final int lpp;
        try {
            lpp = MathUtils.lcm(a, b);
        } catch (Throwable t) {
            return;
        }

        final int lnp;
        try {
            lnp = MathUtils.lcm(-a, b);
        } catch (Throwable t) {
            return;
        }

        final int lpn;
        try {
            lpn = MathUtils.lcm(a, -b);
        } catch (Throwable t) {
            return;
        }

        final int lnn;
        try {
            lnn = MathUtils.lcm(-a, -b);
        } catch (Throwable t) {
            return;
        }

        final int lsym;
        try {
            lsym = MathUtils.lcm(b, a);
        } catch (Throwable t) {
            return;
        }

        final int g;
        try {
            g = MathUtils.gcd(a, b);
        } catch (Throwable t) {
            return;
        }

        if (!(lpp == lnp && lpp == lpn && lpp == lnn && lpp == lsym)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lcm-sign-symmetry] relation lcm sign/symmetry violated: a=" + a + " b=" + b +
                " values=" + lpp + "," + lnp + "," + lpn + "," + lnn + "," + lsym
            );
        }

        /* Contract guarantee asserted: the lcm returned by MathUtils for nonzero a,b is a common multiple,
           and the implementation computes it from gcd(a,b); a throw-deleting or silent-wrong patch can return
           a value that is no longer divisible by both operands or no longer satisfies gcd*lcm = |a*b|. */
        long absA = Math.abs((long) a);
        long absB = Math.abs((long) b);
        long lhs = ((long) g) * ((long) lpp);
        long rhs = absA * absB;
        if (lpp <= 0 || (lpp % a) != 0 || (lpp % b) != 0 || lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lcm-gcd-product] metamorphic violation: a=" + a + " b=" + b +
                " gcd=" + g + " lcm=" + lpp + " gcd*lcm=" + lhs + " |a*b|=" + rhs
            );
        }

        int oneArg;
        try {
            oneArg = data.consumeInt();
        } catch (Throwable t) {
            return;
        }
        if (oneArg == Integer.MIN_VALUE) {
            return;
        }

        final int l1;
        try {
            l1 = MathUtils.lcm(oneArg, 1);
        } catch (Throwable t) {
            return;
        }

        int expected = Math.abs(oneArg);
        if (l1 != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lcm-one-identity] relation lcm-one-identity violated: a=" + oneArg +
                " lcm(a,1)=" + l1 + " expected=" + expected
            );
        }
    }
}