package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int k = data.consumeInt(0, 30);
        boolean negate = data.remainingBytes() > 0 && data.consumeBoolean();
        checkBoundaryFlipPowerOfTwoNeighbor(k, negate);
    }

    private static void checkBoundaryFlipPowerOfTwoNeighbor(int k, boolean negate) {
        int pow2 = 1 << k;
        int n = negate ? -pow2 : pow2;
        int neighbor = Integer.MIN_VALUE + pow2;
        int expectedGcd = pow2;
        long expectedLcmLong = (1L << 31) - (1L << k);
        int expectedLcm = (int) expectedLcmLong;

        /* Contract justification:
         * - The failing test and lcm documentation require lcm(Integer.MIN_VALUE, ±2^k) to throw because the
         *   mathematical result is 2^31, not representable as a nonnegative int.
         * - For the constructed neighbor input a = Integer.MIN_VALUE + 2^k, the value 2^k divides a exactly, so
         *   gcd(a, ±2^k) must be 2^k and lcm(a, ±2^k) must be |a| = 2^31 - 2^k.
         * This flips the patched boundary condition: exactly at MIN_VALUE rejection is required, one step past it
         * a precise non-throwing result is required. A band-aid that only special-cases the seed input or deletes
         * the throw breaks one side of this paired check.
         */
        int actualGcd;
        try {
            actualGcd = MathUtils.gcd(neighbor, n);
        } catch (ArithmeticException e) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-gcd-neighbor] semantic mismatch: gcd(" + neighbor + "," + n
                            + ") should be " + expectedGcd + " because " + expectedGcd
                            + " divides both inputs exactly, but threw " + e.getClass().getName(),
                    e);
        }
        if (actualGcd != expectedGcd) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-gcd-neighbor] semantic mismatch: gcd(" + neighbor + "," + n
                            + ") expected=" + expectedGcd + " actual=" + actualGcd);
        }

        int actualLcm;
        try {
            actualLcm = MathUtils.lcm(neighbor, n);
        } catch (ArithmeticException e) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-lcm-neighbor] semantic mismatch: lcm(" + neighbor + "," + n
                            + ") should be " + expectedLcm + " because " + n
                            + " divides the first argument exactly, but threw " + e.getClass().getName(),
                    e);
        }
        if (actualLcm != expectedLcm) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-lcm-neighbor] semantic mismatch: lcm(" + neighbor + "," + n
                            + ") expected=" + expectedLcm + " actual=" + actualLcm);
        }

        boolean boundaryReturned = false;
        int boundaryValue = 0;
        ArithmeticException arithmetic = null;
        Throwable other = null;
        try {
            boundaryValue = MathUtils.lcm(Integer.MIN_VALUE, n);
            boundaryReturned = true;
        } catch (ArithmeticException e) {
            arithmetic = e;
        } catch (Throwable t) {
            other = t;
        }
        if (other != null) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-lcm-overflow] semantic mismatch: lcm(" + Integer.MIN_VALUE + "," + n
                            + ") should reject with ArithmeticException because the exact result is 2^31, but threw "
                            + other.getClass().getName(),
                    other);
        }
        if (boundaryReturned) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-lcm-overflow] semantic mismatch: lcm(" + Integer.MIN_VALUE + "," + n
                            + ") should throw ArithmeticException because the exact result is 2^31, but returned "
                            + boundaryValue + " while the adjacent valid input lcm(" + neighbor + "," + n
                            + ") correctly has the representable value " + expectedLcm);
        }
        if (arithmetic == null) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:boundary-flip-lcm-overflow] semantic mismatch: missing ArithmeticException for lcm("
                            + Integer.MIN_VALUE + "," + n + ")");
        }
    }
}