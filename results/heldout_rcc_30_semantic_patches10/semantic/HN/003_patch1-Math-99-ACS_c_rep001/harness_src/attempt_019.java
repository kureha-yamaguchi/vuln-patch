package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        checkEquals("gcd-0-0", 0, MathUtils.gcd(0, 0));
        checkEquals("gcd-0-b", b, MathUtils.gcd(0, b));
        checkEquals("gcd-a-0", a, MathUtils.gcd(a, 0));
        checkEquals("gcd-0-negb", b, MathUtils.gcd(0, -b));
        checkEquals("gcd-nega-0", a, MathUtils.gcd(-a, 0));

        checkEquals("gcd-a-b", 10, MathUtils.gcd(a, b));
        checkEquals("gcd-nega-b", 10, MathUtils.gcd(-a, b));
        checkEquals("gcd-a-negb", 10, MathUtils.gcd(a, -b));
        checkEquals("gcd-nega-negb", 10, MathUtils.gcd(-a, -b));

        checkEquals("gcd-a-c", 1, MathUtils.gcd(a, c));
        checkEquals("gcd-nega-c", 1, MathUtils.gcd(-a, c));
        checkEquals("gcd-a-negc", 1, MathUtils.gcd(a, -c));
        checkEquals("gcd-nega-negc", 1, MathUtils.gcd(-a, -c));

        checkEquals("gcd-shifted", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));

        checkEquals("gcd-max-0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        checkEquals("gcd-negmax-0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        checkEquals("gcd-1<<30-min", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));

        expectArithmetic("gcd-min-0", Integer.MIN_VALUE, 0);
        expectArithmetic("gcd-0-min", 0, Integer.MIN_VALUE);
        expectArithmetic("gcd-min-min", Integer.MIN_VALUE, Integer.MIN_VALUE);

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);

        try {
            int gx = MathUtils.gcd(x, y);
            int gAbs = MathUtils.gcd(Math.abs(x), Math.abs(y));
            if (gx != gAbs) {
                throw new RuntimeException("[oracle:gcd-sign-invariance] metamorphic violation: gcd(x,y) must equal gcd(abs(x),abs(y)) because gcd is defined on absolute values and the test itself lifts all sign variants with identical expected results input=(" + x + "," + y + ") lhs=" + gx + " rhs=" + gAbs);
            }
        } catch (RuntimeException e) {
            if (isOracleSignal(e)) {
                throw e;
            }
            return;
        }

        try {
            int l = MathUtils.lcm(x, y);
            if (x == 0 || y == 0) {
                if (l != 0) {
                    throw new RuntimeException("[oracle:lcm-zero] metamorphic violation: documented special case says lcm(0,x) and lcm(x,0) return 0 input=(" + x + "," + y + ") lhs=" + l + " rhs=0");
                }
            } else {
                int g = MathUtils.gcd(x, y);
                long lhs = (long) g * (long) l;
                long rhs = Math.abs((long) x * (long) y);
                if (lhs != rhs) {
                    throw new RuntimeException("[oracle:gcd-lcm-product] metamorphic violation: for nonzero ints where real library calls succeed, gcd(a,b)*lcm(a,b) must equal abs(a*b); a throw-deleting or wrong-value patch in gcd/lcm breaks this observable post-condition input=(" + x + "," + y + ") lhs=" + lhs + " rhs=" + rhs);
                }
            }
        } catch (RuntimeException e) {
            if (isOracleSignal(e)) {
                throw e;
            }
            return;
        }
    }

    private static void checkEquals(String id, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void expectArithmetic(String id, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expecting ArithmeticException actualReturn=" + actual + " input=(" + p + "," + q + ")"
            );
        } catch (ArithmeticException expected) {
        }
    }

    private static boolean isOracleSignal(RuntimeException e) {
        String name = e.getClass().getName();
        return name.contains("FuzzerSecurityIssue") || (e.getMessage() != null && e.getMessage().startsWith("[oracle:"));
    }
}