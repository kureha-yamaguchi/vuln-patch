package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted exact oracles from MathUtilsTest.testLcm.
        checkLcmEquals("lcm-0-b", 0, 50, 0);
        checkLcmEquals("lcm-a-0", 30, 0, 0);
        checkLcmEquals("lcm-1-b", 1, 50, 50);
        checkLcmEquals("lcm-a-1", 30, 1, 30);
        checkLcmEquals("lcm-a-b", 30, 50, 150);
        checkLcmEquals("lcm--a-b", -30, 50, 150);
        checkLcmEquals("lcm-a--b", 30, -50, 150);
        checkLcmEquals("lcm--a--b", -30, -50, 150);
        checkLcmEquals("lcm-a-c", 30, 77, 2310);
        checkLcmEquals("lcm-no-intermediate-overflow", (1 << 20) * 3, (1 << 20) * 5, (1 << 20) * 15);
        checkLcmEquals("lcm-0-0", 0, 0, 0);

        // These are exact lifted rejection oracles from the failing test.
        // The buggy build returns Integer.MIN_VALUE instead of throwing for the first two.
        checkLcmThrows("lcm-minvalue-1", Integer.MIN_VALUE, 1);
        checkLcmThrows("lcm-minvalue-1<<20", Integer.MIN_VALUE, 1 << 20);
        checkLcmThrows("lcm-max-maxminus1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);

        // Extra trusted call into the patched gcd code path on fuzzed data without asserting
        // an invented answer. This exercises the patched early-zero branch through real API.
        try {
            int p = data.consumeInt();
            int q = data.consumeInt();
            MathUtils.gcd(p, q);
        } catch (Throwable ignored) {
        }

        // Candidate relation: gcd-zero-identity-safe.
        // Contract used: the gcd behavior exercised by the tests is gcd(a,0)=|a| and gcd(0,a)=|a|
        // for all a whose absolute value is representable, i.e. a != Integer.MIN_VALUE.
        int gzA;
        try {
            gzA = data.consumeInt();
        } catch (Throwable t) {
            return;
        }
        if (gzA == Integer.MIN_VALUE) {
            return;
        }
        int gz1;
        int gz2;
        try {
            gz1 = MathUtils.gcd(gzA, 0);
            gz2 = MathUtils.gcd(0, gzA);
        } catch (Throwable t) {
            return;
        }
        int gzExpected = Math.abs(gzA);
        if (gz1 != gzExpected || gz2 != gzExpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:gcd-zero-identity-safe] relation gcd-zero-identity-safe violated: a=" + gzA
                    + " gcd(a,0)=" + gz1 + " gcd(0,a)=" + gz2 + " expected=" + gzExpected);
        }

        // Candidate relation: gcd-sign-invariant-positive-inputs.
        // Contract used: gcd ignores operand signs; the trusted tests rely on the same sign invariance for lcm,
        // and gcd's implementation normalizes signs before computing the result.
        int gsA;
        int gsB;
        try {
            gsA = data.consumeInt(1, 1_000_000);
            gsB = data.consumeInt(1, 1_000_000);
        } catch (Throwable t) {
            return;
        }
        int gpp;
        int gnp;
        int gpn;
        int gnn;
        try {
            gpp = MathUtils.gcd(gsA, gsB);
            gnp = MathUtils.gcd(-gsA, gsB);
            gpn = MathUtils.gcd(gsA, -gsB);
            gnn = MathUtils.gcd(-gsA, -gsB);
        } catch (Throwable t) {
            return;
        }
        if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:gcd-sign-invariant-positive-inputs] relation gcd-sign-invariant-positive-inputs violated: a="
                    + gsA + " b=" + gsB + " values=" + gpp + "," + gnp + "," + gpn + "," + gnn);
        }

        // Mandatory post-condition / metamorphic check on the patched method itself.
        // Contract used: lcm is sign-invariant (directly lifted from the trusted test), so a patch that merely
        // deletes/guards throws but computes the wrong magnitude/sign will break this observable relation.
        int lsA;
        int lsB;
        try {
            lsA = data.consumeInt(1, 1_000_000);
            lsB = data.consumeInt(1, 1_000_000);
        } catch (Throwable t) {
            return;
        }
        int lpp;
        int lnp;
        int lpn;
        int lnn;
        try {
            lpp = MathUtils.lcm(lsA, lsB);
            lnp = MathUtils.lcm(-lsA, lsB);
            lpn = MathUtils.lcm(lsA, -lsB);
            lnn = MathUtils.lcm(-lsA, -lsB);
        } catch (Throwable t) {
            return;
        }
        if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lcm-sign-invariance] metamorphic violation: lcm sign invariance a=" + lsA + " b=" + lsB
                    + " values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }

        // Additional trusted post-condition on lcm.
        // Contract used: for non-zero inputs, the least common multiple must be a nonnegative common multiple,
        // so the returned value must be divisible by both |a| and |b| whenever the call succeeds.
        int ldA;
        int ldB;
        try {
            ldA = data.consumeInt(1, 46340);
            ldB = data.consumeInt(1, 46340);
        } catch (Throwable t) {
            return;
        }
        int ldiv;
        try {
            ldiv = MathUtils.lcm(ldA, ldB);
        } catch (Throwable t) {
            return;
        }
        if (ldiv < 0 || (ldiv % ldA) != 0 || (ldiv % ldB) != 0) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lcm-common-multiple] metamorphic violation: lcm must be a nonnegative common multiple a="
                    + ldA + " b=" + ldB + " lcm=" + ldiv);
        }
    }

    private static void checkLcmEquals(String id, int a, int b, int expected) {
        final int actual;
        try {
            actual = MathUtils.lcm(a, b);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: unexpected exception for MathUtils.lcm(" + a + "," + b
                    + ")", t);
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: MathUtils.lcm(" + a + "," + b + ") expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static void checkLcmThrows(String id, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for MathUtils.lcm(" + a + ","
                    + b + ") but returned " + actual);
        } catch (ArithmeticException expected) {
            // expected
        }
    }
}