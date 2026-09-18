package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static void expectArithmeticExceptionGcd(int x, int y, String oracleId) {
        try {
            int actual = MathUtils.gcd(x, y);
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] [" + oracleId + "] semantic mismatch: MathUtils.gcd(" + x + "," + y + ") expected ArithmeticException actual=" + actual);
        } catch (ArithmeticException expected) {
            // expected rejection per lifted test
        }
    }

    private static void expectArithmeticExceptionLcm(int x, int y, String oracleId) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] [" + oracleId + "] semantic mismatch: MathUtils.lcm(" + x + "," + y + ") expected ArithmeticException actual=" + actual);
        } catch (ArithmeticException expected) {
            // expected rejection per documented contract
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        int actual;

        actual = MathUtils.gcd(0, 0);
        if (actual != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-00] semantic mismatch: MathUtils.gcd(0,0) expected=0 actual=" + actual);
        }

        actual = MathUtils.gcd(0, b);
        if (actual != b) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-01] semantic mismatch: MathUtils.gcd(0,50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(a, 0);
        if (actual != a) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-02] semantic mismatch: MathUtils.gcd(30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(0, -b);
        if (actual != b) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-03] semantic mismatch: MathUtils.gcd(0,-50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, 0);
        if (actual != a) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-04] semantic mismatch: MathUtils.gcd(-30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(a, b);
        if (actual != 10) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-05] semantic mismatch: MathUtils.gcd(30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, b);
        if (actual != 10) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-06] semantic mismatch: MathUtils.gcd(-30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -b);
        if (actual != 10) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-07] semantic mismatch: MathUtils.gcd(30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -b);
        if (actual != 10) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-08] semantic mismatch: MathUtils.gcd(-30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, c);
        if (actual != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-09] semantic mismatch: MathUtils.gcd(30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, c);
        if (actual != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-10] semantic mismatch: MathUtils.gcd(-30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -c);
        if (actual != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-11] semantic mismatch: MathUtils.gcd(30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -c);
        if (actual != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-12] semantic mismatch: MathUtils.gcd(-30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15));
        if (actual != 3 * (1 << 15)) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-13] semantic mismatch: MathUtils.gcd(3*(1<<20),9*(1<<15)) expected=" + (3 * (1 << 15)) + " actual=" + actual);
        }

        actual = MathUtils.gcd(Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-14] semantic mismatch: MathUtils.gcd(Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(-Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-15] semantic mismatch: MathUtils.gcd(-Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(1 << 30, -Integer.MIN_VALUE);
        if (actual != (1 << 30)) {
            throw new FuzzerSecurityIssueLow("[oracle:testGcd-16] semantic mismatch: MathUtils.gcd(1<<30,-Integer.MIN_VALUE) expected=" + (1 << 30) + " actual=" + actual);
        }

        expectArithmeticExceptionGcd(Integer.MIN_VALUE, 0, "oracle:testGcd-17");
        expectArithmeticExceptionGcd(0, Integer.MIN_VALUE, "oracle:testGcd-18");
        expectArithmeticExceptionGcd(Integer.MIN_VALUE, Integer.MIN_VALUE, "oracle:testGcd-19");

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);

        try {
            int g1 = MathUtils.gcd(x, y);
            int g2 = MathUtils.gcd(-x, y);
            int g3 = MathUtils.gcd(x, -y);
            int g4 = MathUtils.gcd(-x, -y);
            /* Lifted test pairs show gcd is invariant under independently flipping either sign:
               gcd(a,b)==gcd(-a,b)==gcd(a,-b)==gcd(-a,-b). Any correct implementation must preserve this. */
            if (!(g1 == g2 && g1 == g3 && g1 == g4)) {
                throw new RuntimeException("[oracle:gcd-sign-invariance] metamorphic violation: gcd sign invariance input=(" + x + "," + y + ") values=" + g1 + "," + g2 + "," + g3 + "," + g4);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        try {
            if (x != 0 && y != 0) {
                int g = MathUtils.gcd(x, y);
                int l = MathUtils.lcm(x, y);
                long lhs = (long) g * (long) l;
                long rhs = Math.abs((long) x * (long) y);
                /* MathUtils documents lcm(a,b) via (a / gcd(a,b)) * b on absolute values, so for non-zero inputs
                   gcd(a,b) * lcm(a,b) == |a*b| is a guaranteed observable post-condition for every correct implementation. */
                if (lhs != rhs) {
                    throw new RuntimeException("[oracle:gcd-lcm-product] metamorphic violation: gcd(a,b)*lcm(a,b)==|a*b| input=(" + x + "," + y + ") lhs=" + lhs + " rhs=" + rhs + " gcd=" + g + " lcm=" + l);
                }
            } else {
                MathUtils.lcm(x, y);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        int shift = data.consumeInt(0, 30);
        int powerOfTwo = 1 << shift;
        int signedPowerOfTwo = data.consumeBoolean() ? powerOfTwo : -powerOfTwo;

        expectArithmeticExceptionLcm(Integer.MIN_VALUE, signedPowerOfTwo, "oracle:lcm-min-power2-left");
        expectArithmeticExceptionLcm(signedPowerOfTwo, Integer.MIN_VALUE, "oracle:lcm-min-power2-right");
    }
}