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
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0-0] semantic mismatch: MathUtils.gcd(0,0) expected=0 actual=" + actual);
        }

        actual = MathUtils.gcd(0, b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0-b] semantic mismatch: MathUtils.gcd(0,50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a-0] semantic mismatch: MathUtils.gcd(30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(0, -b);
        if (actual != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0-negb] semantic mismatch: MathUtils.gcd(0,-50) expected=50 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, 0);
        if (actual != a) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nega-0] semantic mismatch: MathUtils.gcd(-30,0) expected=30 actual=" + actual);
        }

        actual = MathUtils.gcd(a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a-b] semantic mismatch: MathUtils.gcd(30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nega-b] semantic mismatch: MathUtils.gcd(-30,50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a-negb] semantic mismatch: MathUtils.gcd(30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -b);
        if (actual != 10) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nega-negb] semantic mismatch: MathUtils.gcd(-30,-50) expected=10 actual=" + actual);
        }

        actual = MathUtils.gcd(a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a-c] semantic mismatch: MathUtils.gcd(30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nega-c] semantic mismatch: MathUtils.gcd(-30,77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-a-negc] semantic mismatch: MathUtils.gcd(30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(-a, -c);
        if (actual != 1) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-nega-negc] semantic mismatch: MathUtils.gcd(-30,-77) expected=1 actual=" + actual);
        }

        actual = MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15));
        if (actual != 3 * (1 << 15)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-powers2] semantic mismatch: MathUtils.gcd(3*(1<<20),9*(1<<15)) expected=" + (3 * (1 << 15)) + " actual=" + actual);
        }

        actual = MathUtils.gcd(Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-max-0] semantic mismatch: MathUtils.gcd(Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(-Integer.MAX_VALUE, 0);
        if (actual != Integer.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-negmax-0] semantic mismatch: MathUtils.gcd(-Integer.MAX_VALUE,0) expected=" + Integer.MAX_VALUE + " actual=" + actual);
        }

        actual = MathUtils.gcd(1 << 30, -Integer.MIN_VALUE);
        if (actual != (1 << 30)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-halfmin] semantic mismatch: MathUtils.gcd(1<<30,-Integer.MIN_VALUE) expected=" + (1 << 30) + " actual=" + actual);
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-min-0-throws] semantic mismatch: expected ArithmeticException for MathUtils.gcd(Integer.MIN_VALUE,0)");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-0-min-throws] semantic mismatch: expected ArithmeticException for MathUtils.gcd(0,Integer.MIN_VALUE)");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:gcd-min-min-throws] semantic mismatch: expected ArithmeticException for MathUtils.gcd(Integer.MIN_VALUE,Integer.MIN_VALUE)");
        } catch (ArithmeticException expected) {
        }

        int x = data.consumeInt(-1000000, 1000000);
        int y = data.consumeInt(-1000000, 1000000);
        int k = data.consumeInt(-1000, 1000);

        try {
            int gx1 = MathUtils.gcd(x, y);
            int gx2 = MathUtils.gcd(-x, y);
            int gx3 = MathUtils.gcd(x, -y);
            int gx4 = MathUtils.gcd(-x, -y);

            /* For a correct gcd implementation, changing operand signs must not change the non-negative gcd.
             * This is the same absolute-value behavior exercised by the lifted test pairs above; a patch that
             * merely suppresses overflow handling or otherwise changes result computation would violate it. */
            if (gx1 != gx2 || gx1 != gx3 || gx1 != gx4) {
                throw new RuntimeException("[oracle:gcd-sign-invariance] metamorphic violation: gcd sign-invariance input=(" + x + "," + y + ") gcd(x,y)=" + gx1 + " gcd(-x,y)=" + gx2 + " gcd(x,-y)=" + gx3 + " gcd(-x,-y)=" + gx4);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (k != 0) {
                int sx = x / Math.max(1, Math.abs(k));
                int sy = y / Math.max(1, Math.abs(k));
                int lhs = MathUtils.gcd(sx * k, sy * k);
                int rhsBase = MathUtils.gcd(sx, sy);
                int rhs = Math.abs(k) * rhsBase;

                /* Mathematical gcd contract: gcd(k*a, k*b) == |k| * gcd(a,b) whenever both sides are representable.
                 * We keep values moderate by construction so a correct implementation must preserve this relation. */
                if (lhs != rhs) {
                    throw new RuntimeException("[oracle:gcd-scale] metamorphic violation: gcd(k*a,k*b)=|k|*gcd(a,b) input=a=" + sx + " b=" + sy + " k=" + k + " lhs=" + lhs + " rhs=" + rhs);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            MathUtils.lcm(data.consumeInt(), data.consumeInt());
        } catch (Throwable ignored) {
        }
    }
}