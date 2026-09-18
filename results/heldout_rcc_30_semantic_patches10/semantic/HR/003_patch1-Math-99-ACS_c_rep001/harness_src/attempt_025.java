package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedGcdTestOracles();
        runBoundaryCrossApiMagnitudeOracle(data);

        int a = data.consumeInt();
        int b = data.consumeInt();

        try {
            MathUtils.gcd(a, b);
        } catch (ArithmeticException ignored) {
        }

        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException ignored) {
        }
    }

    private static void runLiftedGcdTestOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        checkEquals("lifted-gcd-0-0", 0, MathUtils.gcd(0, 0));
        checkEquals("lifted-gcd-0-b", b, MathUtils.gcd(0, b));
        checkEquals("lifted-gcd-a-0", a, MathUtils.gcd(a, 0));
        checkEquals("lifted-gcd-0-negb", b, MathUtils.gcd(0, -b));
        checkEquals("lifted-gcd-nega-0", a, MathUtils.gcd(-a, 0));

        checkEquals("lifted-gcd-a-b", 10, MathUtils.gcd(a, b));
        checkEquals("lifted-gcd-nega-b", 10, MathUtils.gcd(-a, b));
        checkEquals("lifted-gcd-a-negb", 10, MathUtils.gcd(a, -b));
        checkEquals("lifted-gcd-nega-negb", 10, MathUtils.gcd(-a, -b));

        checkEquals("lifted-gcd-a-c", 1, MathUtils.gcd(a, c));
        checkEquals("lifted-gcd-nega-c", 1, MathUtils.gcd(-a, c));
        checkEquals("lifted-gcd-a-negc", 1, MathUtils.gcd(a, -c));
        checkEquals("lifted-gcd-nega-negc", 1, MathUtils.gcd(-a, -c));

        checkEquals("lifted-gcd-scale", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));

        checkEquals("lifted-gcd-max-zero", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        checkEquals("lifted-gcd-negmax-zero", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        checkEquals("lifted-gcd-minpower", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));

        expectArithmetic("lifted-gcd-min-zero-throws", Integer.MIN_VALUE, 0);
        expectArithmetic("lifted-gcd-zero-min-throws", 0, Integer.MIN_VALUE);
        expectArithmetic("lifted-gcd-min-min-throws", Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private static void runBoundaryCrossApiMagnitudeOracle(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 7);
        int x;
        switch (choice) {
            case 0:
                x = Integer.MIN_VALUE;
                break;
            case 1:
                x = Integer.MIN_VALUE + 1;
                break;
            case 2:
                x = -1 << data.consumeInt(0, 30);
                break;
            case 3:
                x = 1 << data.consumeInt(0, 30);
                break;
            case 4:
                x = data.consumeInt(-1_000_000, 1_000_000);
                break;
            case 5:
                x = Integer.MAX_VALUE;
                break;
            case 6:
                x = -Integer.MAX_VALUE;
                break;
            default:
                x = data.consumeInt();
                break;
        }

        long exactMagnitude = Math.abs((long) x);

        Integer gcdValue = null;
        ArithmeticException gcdException = null;
        try {
            gcdValue = Integer.valueOf(MathUtils.gcd(x, 0));
        } catch (ArithmeticException e) {
            gcdException = e;
        }

        Integer lcmValue = null;
        ArithmeticException lcmException = null;
        try {
            lcmValue = Integer.valueOf(MathUtils.lcm(x, 1));
        } catch (ArithmeticException e) {
            lcmException = e;
        }

        if (exactMagnitude > Integer.MAX_VALUE) {
            // Documented by the lifted tests: the mathematical magnitude 2^31 is not representable as int,
            // so gcd(Integer.MIN_VALUE, 0) and lcm(Integer.MIN_VALUE, 1) must reject.
            if (gcdException == null) {
                throw new FuzzerSecurityIssueLow("[oracle:cross-api-unrepresentable-gcd] semantic mismatch: gcd(" + x + ",0) returned " + gcdValue + " for unrepresentable magnitude " + exactMagnitude);
            }
            if (lcmException == null) {
                throw new FuzzerSecurityIssueLow("[oracle:cross-api-unrepresentable-lcm] semantic mismatch: lcm(" + x + ",1) returned " + lcmValue + " for unrepresentable magnitude " + exactMagnitude);
            }
            return;
        }

        // If either side rejects on a representable magnitude, preserve exception identity as cause.
        if (gcdException != null) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-api-representable-gcd] unexpected ArithmeticException for gcd(" + x + ",0) where |x|=" + exactMagnitude + " fits in int", gcdException);
        }
        if (lcmException != null) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-api-representable-lcm] unexpected ArithmeticException for lcm(" + x + ",1) where |x|=" + exactMagnitude + " fits in int", lcmException);
        }

        int expected = (int) exactMagnitude;

        // For representable |x|, the lifted tests establish gcd(x,0) == |x| and lcm(x,1) == |x|.
        if (gcdValue.intValue() != expected) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-api-abs-gcd] semantic mismatch: gcd(" + x + ",0) expected " + expected + " actual " + gcdValue);
        }
        if (lcmValue.intValue() != expected) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-api-abs-lcm] semantic mismatch: lcm(" + x + ",1) expected " + expected + " actual " + lcmValue);
        }

        // Independent post-condition: two real API calls that both denote |x| must agree.
        if (gcdValue.intValue() != lcmValue.intValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:cross-api-agree] metamorphic violation: gcd(" + x + ",0)=" + gcdValue + " but lcm(" + x + ",1)=" + lcmValue);
        }

        if (x != 0) {
            try {
                int reversed = MathUtils.gcd(0, x);
                if (reversed != gcdValue.intValue()) {
                    throw new FuzzerSecurityIssueLow("[oracle:zero-branch-order-agree] metamorphic violation: gcd(" + x + ",0)=" + gcdValue + " but gcd(0," + x + ")=" + reversed);
                }
            } catch (ArithmeticException ignored) {
            }
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected " + expected + " actual " + actual);
        }
    }

    private static void expectArithmetic(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expecting ArithmeticException but got " + actual + " from gcd(" + p + "," + q + ")");
        } catch (ArithmeticException expected) {
        }
    }
}