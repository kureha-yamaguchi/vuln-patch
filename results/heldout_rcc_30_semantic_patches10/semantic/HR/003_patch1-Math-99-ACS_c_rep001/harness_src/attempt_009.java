package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmAssertions();
        runHelperConsistencyForMinValueLcm(data);
        exercisePatchedPaths(data);
    }

    private static void runLiftedLcmAssertions() {
        checkEquals("lifted-lcm-0b", 0, MathUtils.lcm(0, 50));
        checkEquals("lifted-lcm-a0", 0, MathUtils.lcm(30, 0));
        checkEquals("lifted-lcm-1b", 50, MathUtils.lcm(1, 50));
        checkEquals("lifted-lcm-a1", 30, MathUtils.lcm(30, 1));
        checkEquals("lifted-lcm-ab", 150, MathUtils.lcm(30, 50));
        checkEquals("lifted-lcm-negab", 150, MathUtils.lcm(-30, 50));
        checkEquals("lifted-lcm-anegb", 150, MathUtils.lcm(30, -50));
        checkEquals("lifted-lcm-neganegb", 150, MathUtils.lcm(-30, -50));
        checkEquals("lifted-lcm-ac", 2310, MathUtils.lcm(30, 77));
        checkEquals("lifted-lcm-powscale", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        checkEquals("lifted-lcm-00", 0, MathUtils.lcm(0, 0));
        expectArithmetic("lifted-lcm-min-one-throws", Integer.MIN_VALUE, 1);
        expectArithmetic("lifted-lcm-min-pow-throws", Integer.MIN_VALUE, 1 << 20);
        expectArithmetic("lifted-lcm-max-maxminus-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void runHelperConsistencyForMinValueLcm(FuzzedDataProvider data) {
        int k = data.consumeInt(0, 30);
        int n = 1 << k;
        if (data.consumeBoolean()) {
            n = -n;
        }

        checkMinValueHelperConsistency("helper-minvalue-left", Integer.MIN_VALUE, n);
        checkMinValueHelperConsistency("helper-minvalue-right", n, Integer.MIN_VALUE);
    }

    private static void checkMinValueHelperConsistency(String oracleId, int a, int b) {
        int reportedGcd;
        int scaled;
        int rawProduct;
        try {
            reportedGcd = MathUtils.gcd(a, b);
            scaled = a / reportedGcd;
            rawProduct = MathUtils.mulAndCheck(scaled, b);
        } catch (Throwable t) {
            return;
        }

        if (rawProduct != Integer.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] consistency violation: constructed power-of-two case should make mulAndCheck(a/gcd(a,b),b)==Integer.MIN_VALUE"
                    + " a=" + a + " b=" + b + " gcd=" + reportedGcd + " scaled=" + scaled + " rawProduct=" + rawProduct);
        }

        int result = 0;
        boolean returned = false;
        Throwable wrong = null;
        try {
            result = MathUtils.lcm(a, b);
            returned = true;
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            wrong = t;
        }

        if (wrong != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] consistency violation: wrong exception kind from lcm after helper computed Integer.MIN_VALUE"
                    + " a=" + a + " b=" + b + " gcd=" + reportedGcd + " scaled=" + scaled + " rawProduct=" + rawProduct,
                wrong);
        }
        if (returned) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] consistency violation: helper path proves abs(mulAndCheck(a/gcd(a,b),b)) is unrepresentable, so lcm must reject"
                    + " a=" + a + " b=" + b + " gcd=" + reportedGcd + " scaled=" + scaled + " rawProduct=" + rawProduct + " lcmResult=" + result);
        }
    }

    private static void exercisePatchedPaths(FuzzedDataProvider data) {
        int x = data.consumeInt();
        int y = data.consumeInt();
        try {
            MathUtils.gcd(x, y);
        } catch (Throwable ignored) {
        }
        try {
            MathUtils.lcm(x, y);
        } catch (Throwable ignored) {
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String oracleId, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}