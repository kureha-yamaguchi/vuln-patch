package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        if (MathUtils.gcd(0, 0) != 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-00] semantic mismatch: MathUtils.gcd(0,0) expected=0 actual=" + MathUtils.gcd(0, 0));
        }

        if (MathUtils.gcd(0, b) != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-0b] semantic mismatch: MathUtils.gcd(0,50) expected=50 actual=" + MathUtils.gcd(0, b));
        }
        if (MathUtils.gcd(a, 0) != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-a0] semantic mismatch: MathUtils.gcd(30,0) expected=30 actual=" + MathUtils.gcd(a, 0));
        }
        if (MathUtils.gcd(0, -b) != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-0negb] semantic mismatch: MathUtils.gcd(0,-50) expected=50 actual=" + MathUtils.gcd(0, -b));
        }
        if (MathUtils.gcd(-a, 0) != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-nega0] semantic mismatch: MathUtils.gcd(-30,0) expected=30 actual=" + MathUtils.gcd(-a, 0));
        }

        if (MathUtils.gcd(a, b) != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-ab] semantic mismatch: MathUtils.gcd(30,50) expected=10 actual=" + MathUtils.gcd(a, b));
        }
        if (MathUtils.gcd(-a, b) != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-negab] semantic mismatch: MathUtils.gcd(-30,50) expected=10 actual=" + MathUtils.gcd(-a, b));
        }
        if (MathUtils.gcd(a, -b) != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-anegb] semantic mismatch: MathUtils.gcd(30,-50) expected=10 actual=" + MathUtils.gcd(a, -b));
        }
        if (MathUtils.gcd(-a, -b) != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-neganegb] semantic mismatch: MathUtils.gcd(-30,-50) expected=10 actual=" + MathUtils.gcd(-a, -b));
        }

        if (MathUtils.gcd(a, c) != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-ac] semantic mismatch: MathUtils.gcd(30,77) expected=1 actual=" + MathUtils.gcd(a, c));
        }
        if (MathUtils.gcd(-a, c) != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-negac] semantic mismatch: MathUtils.gcd(-30,77) expected=1 actual=" + MathUtils.gcd(-a, c));
        }
        if (MathUtils.gcd(a, -c) != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-anegc] semantic mismatch: MathUtils.gcd(30,-77) expected=1 actual=" + MathUtils.gcd(a, -c));
        }
        if (MathUtils.gcd(-a, -c) != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-neganegc] semantic mismatch: MathUtils.gcd(-30,-77) expected=1 actual=" + MathUtils.gcd(-a, -c));
        }

        int shiftedExpected = 3 * (1 << 15);
        int shiftedActual = MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15));
        if (shiftedActual != shiftedExpected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-shifted] semantic mismatch: MathUtils.gcd(3*(1<<20),9*(1<<15)) expected=" + shiftedExpected + " actual=" + shiftedActual);
        }

        int maxZeroActual = MathUtils.gcd(Integer.MAX_VALUE, 0);
        if (maxZeroActual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-max-0] semantic mismatch: MathUtils.gcd(Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + maxZeroActual);
        }
        int negMaxZeroActual = MathUtils.gcd(-Integer.MAX_VALUE, 0);
        if (negMaxZeroActual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-negmax-0] semantic mismatch: MathUtils.gcd(-Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + negMaxZeroActual);
        }
        int minPairActual = MathUtils.gcd(1 << 30, -Integer.MIN_VALUE);
        if (minPairActual != (1 << 30)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-1sh30-min] semantic mismatch: MathUtils.gcd(1<<30,-Integer.MIN_VALUE) expected=" + (1 << 30) + " actual=" + minPairActual);
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-min-0-throws] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE,0) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-0-min-throws] semantic mismatch: MathUtils.gcd(0,Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-min-min-throws] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE,Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        int k = data.consumeInt(-10000, 10000);
        int x = 6 * k;
        int y = 35 * k;

        try {
            int gcdXY = MathUtils.gcd(x, y);
            int expectedGcd = Math.abs(k);
            if (gcdXY != expectedGcd) {
                throw new RuntimeException(
                    "[oracle:gcd-constructed] metamorphic violation: constructed inputs x=6*k and y=35*k with gcd(6,35)=1 must satisfy gcd(x,y)=|k| inputK="
                        + k + " lhs=" + gcdXY + " rhs=" + expectedGcd);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            int lcmXY = MathUtils.lcm(x, y);
            int expectedLcm = Math.abs(210 * k);
            if (lcmXY != expectedLcm) {
                throw new RuntimeException(
                    "[oracle:lcm-constructed] metamorphic violation: lcm is documented as the least common multiple of the absolute values; for x=6*k and y=35*k with gcd(6,35)=1, lcm must be |210*k| inputK="
                        + k + " lhs=" + lcmXY + " rhs=" + expectedLcm);
            }
        } catch (Throwable t) {
            return;
        }

        int p = data.consumeInt(-1000000, 1000000);
        int q = data.consumeInt(-1000000, 1000000);
        try {
            MathUtils.gcd(p, q);
        } catch (Throwable t) {
        }
        try {
            MathUtils.lcm(p, q);
        } catch (Throwable t) {
        }
    }
}