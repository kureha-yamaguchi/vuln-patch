package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestGcdOracles();
        fuzzedGcdAndLcmChecks(data);
    }

    private static void liftedTestGcdOracles() {
        final int a = 30;
        final int b = 50;
        final int c = 77;

        checkEquals("gcd-0-0", 0, MathUtils.gcd(0, 0), "MathUtils.gcd(0, 0)");
        checkEquals("gcd-0-b", b, MathUtils.gcd(0, b), "MathUtils.gcd(0, 50)");
        checkEquals("gcd-a-0", a, MathUtils.gcd(a, 0), "MathUtils.gcd(30, 0)");
        checkEquals("gcd-0-negb", b, MathUtils.gcd(0, -b), "MathUtils.gcd(0, -50)");
        checkEquals("gcd-nega-0", a, MathUtils.gcd(-a, 0), "MathUtils.gcd(-30, 0)");

        checkEquals("gcd-a-b", 10, MathUtils.gcd(a, b), "MathUtils.gcd(30, 50)");
        checkEquals("gcd-nega-b", 10, MathUtils.gcd(-a, b), "MathUtils.gcd(-30, 50)");
        checkEquals("gcd-a-negb", 10, MathUtils.gcd(a, -b), "MathUtils.gcd(30, -50)");
        checkEquals("gcd-nega-negb", 10, MathUtils.gcd(-a, -b), "MathUtils.gcd(-30, -50)");

        checkEquals("gcd-a-c", 1, MathUtils.gcd(a, c), "MathUtils.gcd(30, 77)");
        checkEquals("gcd-nega-c", 1, MathUtils.gcd(-a, c), "MathUtils.gcd(-30, 77)");
        checkEquals("gcd-a-negc", 1, MathUtils.gcd(a, -c), "MathUtils.gcd(30, -77)");
        checkEquals("gcd-nega-negc", 1, MathUtils.gcd(-a, -c), "MathUtils.gcd(-30, -77)");

        checkEquals("gcd-shifted", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)),
                "MathUtils.gcd(3*(1<<20), 9*(1<<15))");

        checkEquals("gcd-max-0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0),
                "MathUtils.gcd(Integer.MAX_VALUE, 0)");
        checkEquals("gcd-negmax-0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0),
                "MathUtils.gcd(-Integer.MAX_VALUE, 0)");
        checkEquals("gcd-1<<30-min", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE),
                "MathUtils.gcd(1<<30, -Integer.MIN_VALUE)");

        expectArithmetic("gcd-min-0", Integer.MIN_VALUE, 0);
        expectArithmetic("gcd-0-min", 0, Integer.MIN_VALUE);
        expectArithmetic("gcd-min-min", Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private static void fuzzedGcdAndLcmChecks(FuzzedDataProvider data) {
        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);

        if (x == 0 && y == 0) {
            int g = MathUtils.gcd(x, y);
            if (g != 0) {
                throw new RuntimeException("[oracle:gcd-zero] metamorphic violation: documented test fixes gcd(0,0)=0 input=(" + x + "," + y + ") actual=" + g);
            }
            MathUtils.lcm(x, y);
            return;
        }

        int gx;
        int gy;
        int gxy;
        int gnegx;
        int gnegy;
        int gnegxy;
        try {
            gx = MathUtils.gcd(x, 0);
            gy = MathUtils.gcd(0, y);
            gxy = MathUtils.gcd(x, y);
            gnegx = MathUtils.gcd(-x, y);
            gnegy = MathUtils.gcd(x, -y);
            gnegxy = MathUtils.gcd(-x, -y);
        } catch (RuntimeException e) {
            return;
        }

        int absX = Math.abs(x);
        int absY = Math.abs(y);

        if (gx != absX) {
            throw new RuntimeException("[oracle:gcd-identity-x] metamorphic violation: test-oracle family shows gcd(a,0)=|a| for representable a input=" + x + " lhs=" + gx + " rhs=" + absX);
        }
        if (gy != absY) {
            throw new RuntimeException("[oracle:gcd-identity-y] metamorphic violation: test-oracle family shows gcd(0,b)=|b| for representable b input=" + y + " lhs=" + gy + " rhs=" + absY);
        }

        if (gxy != gnegx || gxy != gnegy || gxy != gnegxy) {
            throw new RuntimeException("[oracle:gcd-sign] metamorphic violation: lifted test asserts sign-invariance of gcd input=("
                    + x + "," + y + ") lhs=" + gxy + " negx=" + gnegx + " negy=" + gnegy + " negxy=" + gnegxy);
        }

        if (gxy < 0) {
            throw new RuntimeException("[oracle:gcd-nonnegative] metamorphic violation: gcd of absolute values must be non-negative input=(" + x + "," + y + ") actual=" + gxy);
        }

        if (x % gxy != 0 || y % gxy != 0) {
            throw new RuntimeException("[oracle:gcd-divides] metamorphic violation: gcd must divide both inputs input=(" + x + "," + y + ") gcd=" + gxy + " x%gcd=" + (x % gxy) + " y%gcd=" + (y % gxy));
        }

        try {
            int l = MathUtils.lcm(x, y);
            int lSign1 = MathUtils.lcm(-x, y);
            int lSign2 = MathUtils.lcm(x, -y);
            int lSign3 = MathUtils.lcm(-x, -y);

            if (l != lSign1 || l != lSign2 || l != lSign3) {
                throw new RuntimeException("[oracle:lcm-sign] metamorphic violation: lcm is documented over absolute values, so sign variants must agree input=("
                        + x + "," + y + ") lhs=" + l + " negx=" + lSign1 + " negy=" + lSign2 + " negxy=" + lSign3);
            }

            if (x == 0 || y == 0) {
                if (l != 0) {
                    throw new RuntimeException("[oracle:lcm-zero] metamorphic violation: documented contract says lcm(0,x)=0 input=(" + x + "," + y + ") actual=" + l);
                }
                return;
            }

            long product = (long) gxy * (long) l;
            long absMul = Math.abs((long) x * (long) y);

            // Documented contract: lcm(a,b) uses absolute values and gcd/lcm are mathematically linked.
            // A throw-deleting patch in gcd/lcm can silently return a wrong numeric value while still not throwing;
            // this relation exposes that wrong post-state through two real library calls.
            if (product != absMul) {
                throw new RuntimeException("[oracle:gcd-lcm-product] metamorphic violation: gcd(a,b)*lcm(a,b)=|a*b| for non-zero moderate ints input=("
                        + x + "," + y + ") lhs=" + product + " rhs=" + absMul + " gcd=" + gxy + " lcm=" + l);
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual, String call) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: MathUtils.gcd(" + p + ", " + q + ") expected ArithmeticException actual=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}