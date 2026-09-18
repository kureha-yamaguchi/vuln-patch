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
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-00] semantic mismatch: input=(0,0) expected=0 actual=" + actual);
        }

        actual = MathUtils.gcd(0, b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0b] semantic mismatch: input=(0," + b + ") expected=" + b + " actual=" + actual);
        }

        actual = MathUtils.gcd(a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a0] semantic mismatch: input=(" + a + ",0) expected=" + a + " actual=" + actual);
        }

        actual = MathUtils.gcd(0, -b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0negb] semantic mismatch: input=(0," + (-b) + ") expected=" + b + " actual=" + actual);
        }

        actual = MathUtils.gcd(-a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nega0] semantic mismatch: input=(" + (-a) + ",0) expected=" + a + " actual=" + actual);
        }

        actual = MathUtils.gcd(a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ab] semantic mismatch: input=(" + a + "," + b + ") expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-negab] semantic mismatch: input=(" + (-a) + "," + b + ") expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-anegb] semantic mismatch: input=(" + a + "," + (-b) + ") expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-neganegb] semantic mismatch: input=(" + (-a) + "," + (-b) + ") expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-ac] semantic mismatch: input=(" + a + "," + c + ") expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-negac] semantic mismatch: input=(" + (-a) + "," + c + ") expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-anegc] semantic mismatch: input=(" + a + "," + (-c) + ") expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-neganegc] semantic mismatch: input=(" + (-a) + "," + (-c) + ") expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15));
        if (actual != 3 * (1 << 15)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-scale] semantic mismatch: input=(" + (3 * (1 << 20)) + "," + (9 * (1 << 15)) + ") expected=" + (3 * (1 << 15)) + " actual=" + actual);
        }

        actual = MathUtils.gcd(Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-max0] semantic mismatch: input=(" + Integer.MAX_VALUE + ",0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(-Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-negmax0] semantic mismatch: input=(" + (-Integer.MAX_VALUE) + ",0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(1 << 30, -Integer.MIN_VALUE);
        if (actual != (1 << 30)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-minpower] semantic mismatch: input=(" + (1 << 30) + "," + (-Integer.MIN_VALUE) + ") expected=" + (1 << 30) + " actual=" + actual);
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-min-throws-left] semantic mismatch: expecting ArithmeticException for input=(" + Integer.MIN_VALUE + ",0)");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-min-throws-right] semantic mismatch: expecting ArithmeticException for input=(0," + Integer.MIN_VALUE + ")");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-min-throws-both] semantic mismatch: expecting ArithmeticException for input=(" + Integer.MIN_VALUE + "," + Integer.MIN_VALUE + ")");
        } catch (ArithmeticException expected) {
        }

        int delta = data.consumeInt(1, 1024);
        int nearMin = Integer.MIN_VALUE + delta;

        try {
            int g1 = MathUtils.gcd(nearMin, 0);
            int expected = Math.abs(nearMin);
            if (g1 != expected) {
                throw new RuntimeException("[oracle:gcd-near-min-zero] metamorphic violation: documented special case gcd(x,0)==abs(x) for x!=Integer.MIN_VALUE input=" + nearMin + " lhs=" + g1 + " rhs=" + expected);
            }

            int g2 = MathUtils.gcd(0, nearMin);
            if (g2 != expected) {
                throw new RuntimeException("[oracle:gcd-zero-near-min] metamorphic violation: documented special case gcd(0,x)==abs(x) for x!=Integer.MIN_VALUE input=" + nearMin + " lhs=" + g2 + " rhs=" + expected);
            }

            if (g1 != g2) {
                throw new RuntimeException("[oracle:gcd-zero-symmetry-near-min] metamorphic violation: both documented zero-argument special cases must agree input=" + nearMin + " lhs=" + g1 + " rhs=" + g2);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        int lcmInput = nearMin;
        try {
            int l = MathUtils.lcm(lcmInput, 1);
            int expectedLcm = Math.abs(lcmInput);
            if (l != expectedLcm) {
                throw new RuntimeException("[oracle:lcm-near-min-one] metamorphic violation: for x!=0 and x!=Integer.MIN_VALUE, lcm(x,1) must equal abs(x) input=" + lcmInput + " lhs=" + l + " rhs=" + expectedLcm);
            }

            int l2 = MathUtils.lcm(1, lcmInput);
            if (l2 != expectedLcm) {
                throw new RuntimeException("[oracle:lcm-one-near-min] metamorphic violation: commuted lcm(1,x) must equal abs(x) input=" + lcmInput + " lhs=" + l2 + " rhs=" + expectedLcm);
            }

            if (l != l2) {
                throw new RuntimeException("[oracle:lcm-commute-near-min] metamorphic violation: lcm is symmetric input=" + lcmInput + " lhs=" + l + " rhs=" + l2);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        int x = data.consumeInt(-46340, 46340);
        int y = data.consumeInt(-46340, 46340);
        try {
            int intMul = MathUtils.mulAndCheck(x, y);
            long longMul = MathUtils.mulAndCheck((long) x, (long) y);
            if (intMul != (int) longMul) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:mul-overload-agree] consistency violation: x=" + x + " y=" + y + " intMul=" + intMul + " longMul=" + longMul);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        try {
            MathUtils.lcm(data.consumeInt(), data.consumeInt());
        } catch (ArithmeticException ignored) {
        }
    }
}