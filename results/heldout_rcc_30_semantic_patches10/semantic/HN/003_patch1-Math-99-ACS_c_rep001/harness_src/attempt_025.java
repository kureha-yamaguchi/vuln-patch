package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        int actual;

        actual = MathUtils.gcd(0, 0);
        if (actual != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-00] semantic mismatch: MathUtils.gcd(0,0) expected=0 actual=" + actual);
        }

        actual = MathUtils.gcd(0, b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0b] semantic mismatch: MathUtils.gcd(0,50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a0] semantic mismatch: MathUtils.gcd(30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(0, -b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0nb] semantic mismatch: MathUtils.gcd(0,-50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-na0] semantic mismatch: MathUtils.gcd(-30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ab] semantic mismatch: MathUtils.gcd(30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nab] semantic mismatch: MathUtils.gcd(-30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-anb] semantic mismatch: MathUtils.gcd(30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nanb] semantic mismatch: MathUtils.gcd(-30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ac] semantic mismatch: MathUtils.gcd(30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nac] semantic mismatch: MathUtils.gcd(-30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-anc] semantic mismatch: MathUtils.gcd(30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nanc] semantic mismatch: MathUtils.gcd(-30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15));
        if (actual != 3 * (1 << 15)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-pow2] semantic mismatch: MathUtils.gcd(3*(1<<20),9*(1<<15)) expected=" + (3 * (1 << 15)) + " actual=" + actual);
        }

        actual = MathUtils.gcd(Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-max0] semantic mismatch: MathUtils.gcd(Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(-Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nmax0] semantic mismatch: MathUtils.gcd(-Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(1 << 30, -Integer.MIN_VALUE);
        if (actual != (1 << 30)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-minmix] semantic mismatch: MathUtils.gcd(1<<30,-Integer.MIN_VALUE) expected=" + (1 << 30) + " actual=" + actual);
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ex-min0] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE,0) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ex-0min] semantic mismatch: MathUtils.gcd(0,Integer.MIN_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ex-minmin] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE,Integer.MIN_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);

        try {
            int gxy = MathUtils.gcd(x, y);
            int gyx = MathUtils.gcd(y, x);
            if (gxy != gyx) {
                throw new RuntimeException("[oracle:gcd-symmetry] metamorphic violation: gcd must be symmetric for all integer inputs accepted by the API; input=(" + x + "," + y + ") lhs=" + gxy + " rhs=" + gyx);
            }
        } catch (RuntimeException e) {
            if (e instanceof ArithmeticException) {
                return;
            }
            throw e;
        }

        try {
            int g1 = MathUtils.gcd(x, y);
            int g2 = MathUtils.gcd(-x, y);
            int g3 = MathUtils.gcd(x, -y);
            int g4 = MathUtils.gcd(-x, -y);
            /* Contract justification: the lifted test establishes sign-invariance on concrete inputs,
               and gcd is over absolute values; a patch that suppresses overflow handling can return a
               sign-dependent wrong value, violating this relation without necessarily throwing. */
            if (!(g1 == g2 && g1 == g3 && g1 == g4)) {
                throw new RuntimeException("[oracle:gcd-sign] metamorphic violation: gcd must be invariant under sign changes; input=(" + x + "," + y + ") base=" + g1 + " negx=" + g2 + " negy=" + g3 + " negxy=" + g4);
            }
        } catch (RuntimeException e) {
            if (e instanceof ArithmeticException) {
                return;
            }
            throw e;
        }

        int shift = data.consumeInt(0, 30);
        int pow2 = 1 << shift;
        boolean negatePow2 = data.consumeBoolean();
        int powerOfTwoArg = negatePow2 ? -pow2 : pow2;
        try {
            /* Contract justification from MathUtils.lcm javadoc: lcm(Integer.MIN_VALUE, n) where abs(n)
               is a power of 2 must throw ArithmeticException because the mathematical result is 2^31,
               which is too large for int. A throw-deleting patch would silently return a wrong value. */
            MathUtils.lcm(Integer.MIN_VALUE, powerOfTwoArg);
            throw new RuntimeException("[oracle:lcm-min-pow2] metamorphic violation: expected ArithmeticException for lcm(Integer.MIN_VALUE," + powerOfTwoArg + ")");
        } catch (ArithmeticException expected) {
        }

        try {
            int l1 = MathUtils.lcm(x, y);
            int l2 = MathUtils.lcm(y, x);
            /* Contract justification: least common multiple is commutative by definition ("lcm(a,b)"). */
            if (l1 != l2) {
                throw new RuntimeException("[oracle:lcm-symmetry] metamorphic violation: lcm must be symmetric; input=(" + x + "," + y + ") lhs=" + l1 + " rhs=" + l2);
            }
        } catch (RuntimeException e) {
            if (e instanceof ArithmeticException) {
                return;
            }
            throw e;
        }
    }
}