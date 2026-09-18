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
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-00] semantic mismatch: MathUtils.gcd(0,0) expected=0 actual=" + actual);
        }

        actual = MathUtils.gcd(0, b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-0b] semantic mismatch: MathUtils.gcd(0,50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-a0] semantic mismatch: MathUtils.gcd(30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(0, -b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-0negb] semantic mismatch: MathUtils.gcd(0,-50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-nega0] semantic mismatch: MathUtils.gcd(-30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-ab] semantic mismatch: MathUtils.gcd(30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-negab] semantic mismatch: MathUtils.gcd(-30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-anegb] semantic mismatch: MathUtils.gcd(30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-neganegb] semantic mismatch: MathUtils.gcd(-30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-ac] semantic mismatch: MathUtils.gcd(30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-negac] semantic mismatch: MathUtils.gcd(-30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-anegc] semantic mismatch: MathUtils.gcd(30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-neganegc] semantic mismatch: MathUtils.gcd(-30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15));
        if (actual != 3 * (1 << 15)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-pow2] semantic mismatch: MathUtils.gcd(3*(1<<20),9*(1<<15)) expected=" + (3 * (1 << 15)) + " actual=" + actual);
        }

        actual = MathUtils.gcd(Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-max0] semantic mismatch: MathUtils.gcd(Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(-Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-negmax0] semantic mismatch: MathUtils.gcd(-Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(1 << 30, -Integer.MIN_VALUE);
        if (actual != (1 << 30)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-minpair] semantic mismatch: MathUtils.gcd(1<<30,-Integer.MIN_VALUE) expected=" + (1 << 30) + " actual=" + actual);
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-min0-throws] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE,0) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-0min-throws] semantic mismatch: MathUtils.gcd(0,Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-minmin-throws] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE,Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);
        int k = data.consumeInt(1, 1024);

        try {
            int g1 = MathUtils.gcd(x, 0);
            int expectedAbs = Math.abs(x);
            if (g1 != expectedAbs) {
                throw new RuntimeException(
                    "[oracle:meta-gcd-zero] metamorphic violation: documented special case gcd(x,0)==abs(x) for non-MIN x input=" + x + " lhs=" + g1 + " rhs=" + expectedAbs);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            /* Contract/ground-truth guarantee: the lifted test asserts gcd(a,b), gcd(-a,b), gcd(a,-b), gcd(-a,-b)
               are equal. A correct gcd depends on absolute values only, so deleting overflow checks or changing
               sign handling would break this relation without necessarily throwing. */
            int base = MathUtils.gcd(x, y);
            int s1 = MathUtils.gcd(-x, y);
            int s2 = MathUtils.gcd(x, -y);
            int s3 = MathUtils.gcd(-x, -y);
            if (base != s1) {
                throw new RuntimeException(
                    "[oracle:meta-gcd-sign1] metamorphic violation: gcd sign invariance input=(" + x + "," + y + ") lhs=" + base + " rhs=" + s1);
            }
            if (base != s2) {
                throw new RuntimeException(
                    "[oracle:meta-gcd-sign2] metamorphic violation: gcd sign invariance input=(" + x + "," + y + ") lhs=" + base + " rhs=" + s2);
            }
            if (base != s3) {
                throw new RuntimeException(
                    "[oracle:meta-gcd-sign3] metamorphic violation: gcd sign invariance input=(" + x + "," + y + ") lhs=" + base + " rhs=" + s3);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            /* Documented lcm contract: lcm(a,b) is the least common multiple of the absolute values and is 0 iff
               either input is 0. This reaches the second patched function through the real public API. */
            int lx = data.consumeBoolean() ? 0 : data.consumeInt(-1_000_000, 1_000_000);
            int ly = data.consumeBoolean() ? 0 : data.consumeInt(-1_000_000, 1_000_000);
            int lcm = MathUtils.lcm(lx, ly);
            if ((lx == 0 || ly == 0) && lcm != 0) {
                throw new RuntimeException(
                    "[oracle:meta-lcm-zero] metamorphic violation: lcm(x,0)==0 special case input=(" + lx + "," + ly + ") lhs=" + lcm + " rhs=0");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            int shift = Integer.numberOfTrailingZeros(k);
            if (shift < 0) {
                shift = 0;
            }
            if (shift > 30) {
                shift = 30;
            }
            int pow2 = 1 << shift;
            MathUtils.lcm(Integer.MIN_VALUE, pow2);
        } catch (Throwable ignored) {
        }
    }
}