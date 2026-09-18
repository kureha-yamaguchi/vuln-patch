package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        int actual;

        actual = MathUtils.lcm(0, b);
        if (actual != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-0-b] semantic mismatch: MathUtils.lcm(0, 50)=" + actual + " expected=0");
        }

        actual = MathUtils.lcm(a, 0);
        if (actual != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-a-0] semantic mismatch: MathUtils.lcm(30, 0)=" + actual + " expected=0");
        }

        actual = MathUtils.lcm(1, b);
        if (actual != b) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-1-b] semantic mismatch: MathUtils.lcm(1, 50)=" + actual + " expected=50");
        }

        actual = MathUtils.lcm(a, 1);
        if (actual != a) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-a-1] semantic mismatch: MathUtils.lcm(30, 1)=" + actual + " expected=30");
        }

        actual = MathUtils.lcm(a, b);
        if (actual != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-a-b] semantic mismatch: MathUtils.lcm(30, 50)=" + actual + " expected=150");
        }

        actual = MathUtils.lcm(-a, b);
        if (actual != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-neg-a-b] semantic mismatch: MathUtils.lcm(-30, 50)=" + actual + " expected=150");
        }

        actual = MathUtils.lcm(a, -b);
        if (actual != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-a-neg-b] semantic mismatch: MathUtils.lcm(30, -50)=" + actual + " expected=150");
        }

        actual = MathUtils.lcm(-a, -b);
        if (actual != 150) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-neg-a-neg-b] semantic mismatch: MathUtils.lcm(-30, -50)=" + actual + " expected=150");
        }

        actual = MathUtils.lcm(a, c);
        if (actual != 2310) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-a-c] semantic mismatch: MathUtils.lcm(30, 77)=" + actual + " expected=2310");
        }

        actual = MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5);
        if (actual != ((1 << 20) * 15)) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-no-intermediate-overflow] semantic mismatch: MathUtils.lcm((1<<20)*3, (1<<20)*5)=" + actual + " expected=" + ((1 << 20) * 15));
        }

        actual = MathUtils.lcm(0, 0);
        if (actual != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-0-0] semantic mismatch: MathUtils.lcm(0, 0)=" + actual + " expected=0");
        }

        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
            throw new FuzzerSecurityIssueLow("[oracle:lcm-min-1-throws] semantic mismatch: MathUtils.lcm(Integer.MIN_VALUE, 1) did not throw ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1 << 20);
            throw new FuzzerSecurityIssueLow("[oracle:lcm-min-pow2-throws] semantic mismatch: MathUtils.lcm(Integer.MIN_VALUE, 1<<20) did not throw ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            MathUtils.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
            throw new FuzzerSecurityIssueLow("[oracle:lcm-maxpair-throws] semantic mismatch: MathUtils.lcm(Integer.MAX_VALUE, Integer.MAX_VALUE - 1) did not throw ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        int relA1;
        try {
            relA1 = data.consumeInt();
        } catch (Exception e) {
            return;
        }
        if (relA1 != Integer.MIN_VALUE) {
            int r1;
            int r2;
            try {
                r1 = MathUtils.gcd(relA1, 0);
                r2 = MathUtils.gcd(0, relA1);
            } catch (Exception e) {
                return;
            }
            int expected = Math.abs(relA1);
            if (r1 != expected || r2 != expected) {
                throw new FuzzerSecurityIssueLow("relation gcd-zero-identity-safe violated: a=" + relA1 + ", gcd(a,0)=" + r1 + ", gcd(0,a)=" + r2 + ", expected=" + expected);
            }
        }

        int relA2;
        int relB2;
        try {
            relA2 = data.consumeInt(1, 1_000_000);
            relB2 = data.consumeInt(1, 1_000_000);
        } catch (Exception e) {
            return;
        }
        int gpp;
        int gnp;
        int gpn;
        int gnn;
        try {
            gpp = MathUtils.gcd(relA2, relB2);
            gnp = MathUtils.gcd(-relA2, relB2);
            gpn = MathUtils.gcd(relA2, -relB2);
            gnn = MathUtils.gcd(-relA2, -relB2);
        } catch (Exception e) {
            return;
        }
        if (!(gpp == gnp && gpp == gpn && gpp == gnn)) {
            throw new FuzzerSecurityIssueLow("relation gcd-sign-invariant-positive-inputs violated: values " + gpp + "," + gnp + "," + gpn + "," + gnn + " for a=" + relA2 + ", b=" + relB2);
        }

        int relA3;
        int relB3;
        try {
            relA3 = data.consumeInt(1, 46340);
            relB3 = data.consumeInt(1, 46340);
        } catch (Exception e) {
            return;
        }
        int lpp;
        int lnp;
        int lpn;
        int lnn;
        try {
            lpp = MathUtils.lcm(relA3, relB3);
            lnp = MathUtils.lcm(-relA3, relB3);
            lpn = MathUtils.lcm(relA3, -relB3);
            lnn = MathUtils.lcm(-relA3, -relB3);
        } catch (Exception e) {
            return;
        }
        if (!(lpp == lnp && lpp == lpn && lpp == lnn)) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-sign-invariant] metamorphic violation: lcm should ignore operand signs for valid positive inputs; a=" + relA3 + ", b=" + relB3 + ", values=" + lpp + "," + lnp + "," + lpn + "," + lnn);
        }

        int relA4;
        try {
            relA4 = data.consumeInt();
        } catch (Exception e) {
            return;
        }
        if (relA4 == Integer.MIN_VALUE) {
            return;
        }
        int lcmWithOne;
        try {
            lcmWithOne = MathUtils.lcm(relA4, 1);
        } catch (Exception e) {
            return;
        }
        int absExpected = Math.abs(relA4);
        if (lcmWithOne != absExpected) {
            throw new FuzzerSecurityIssueLow("[oracle:lcm-one-identity] metamorphic violation: lcm(a,1) should equal |a| for all a != Integer.MIN_VALUE; a=" + relA4 + ", actual=" + lcmWithOne + ", expected=" + absExpected);
        }

        int relA5;
        int relB5;
        try {
            relA5 = data.consumeInt(1, 1000);
            relB5 = data.consumeInt(1, 1000);
        } catch (Exception e) {
            return;
        }
        int g;
        int l;
        try {
            g = MathUtils.gcd(relA5, relB5);
            l = MathUtils.lcm(relA5, relB5);
        } catch (Exception e) {
            return;
        }
        long productRelation = (long) g * (long) l;
        long expectedProduct = (long) relA5 * (long) relB5;
        /* Contract justification: for positive ints where both results are representable,
           gcd(a,b) * lcm(a,b) == a * b. This is an observable post-condition over two real
           API calls; a patch that simply deletes/avoids the ArithmeticException in lcm can
           still return a wrong numeric value and violate this identity without ever throwing. */
        if (productRelation != expectedProduct) {
            throw new FuzzerSecurityIssueLow("[oracle:gcd-lcm-product] metamorphic violation: a=" + relA5 + ", b=" + relB5 + ", gcd=" + g + ", lcm=" + l + ", gcd*lcm=" + productRelation + ", a*b=" + expectedProduct);
        }
    }
}