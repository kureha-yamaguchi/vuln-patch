package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedLcmAssertions();
        checkHelperDerivedLcmOverflow(data);
        checkMulSignSymmetry(data);
    }

    private static void checkLiftedLcmAssertions() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEquals("lifted-lcm-0b", 0, MathUtils.lcm(0, b));
        assertEquals("lifted-lcm-a0", 0, MathUtils.lcm(a, 0));
        assertEquals("lifted-lcm-1b", b, MathUtils.lcm(1, b));
        assertEquals("lifted-lcm-a1", a, MathUtils.lcm(a, 1));
        assertEquals("lifted-lcm-ab", 150, MathUtils.lcm(a, b));
        assertEquals("lifted-lcm-negab", 150, MathUtils.lcm(-a, b));
        assertEquals("lifted-lcm-anegb", 150, MathUtils.lcm(a, -b));
        assertEquals("lifted-lcm-neganeg", 150, MathUtils.lcm(-a, -b));
        assertEquals("lifted-lcm-ac", 2310, MathUtils.lcm(a, c));
        assertEquals("lifted-lcm-scale", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        assertEquals("lifted-lcm-00", 0, MathUtils.lcm(0, 0));

        expectArithmetic("lifted-lcm-min-one", Integer.MIN_VALUE, 1);
        expectArithmetic("lifted-lcm-min-pow20", Integer.MIN_VALUE, 1 << 20);
        expectArithmetic("lifted-lcm-max-maxminus1", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void checkHelperDerivedLcmOverflow(FuzzedDataProvider data) {
        int k = data.consumeInt(0, 30);
        boolean negate = data.consumeBoolean();
        int n = 1 << k;
        if (negate) {
            n = -n;
        }

        int helper;
        try {
            helper = MathUtils.mulAndCheck(Integer.MIN_VALUE / n, n);
        } catch (Throwable t) {
            return;
        }

        long exactMagnitude = Math.abs((long) helper);
        if (exactMagnitude != (1L << 31)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:helper-derived-lcm-overflow] consistency violation: helper exactMagnitude=" +
                exactMagnitude + " helper=" + helper + " n=" + n);
        }

        int observed = 0;
        boolean returned = false;
        Throwable wrong = null;
        try {
            observed = MathUtils.lcm(Integer.MIN_VALUE, n);
            returned = true;
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            wrong = t;
        }

        if (wrong != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:helper-derived-lcm-overflow] consistency violation: wrong exception type for lcm(Integer.MIN_VALUE," +
                n + ")", wrong);
        }

        if (returned) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:helper-derived-lcm-overflow] consistency violation: lcm(Integer.MIN_VALUE," +
                n + ") returned " + observed + " but helper exactMagnitude=" + exactMagnitude);
        }

        observed = 0;
        returned = false;
        wrong = null;
        try {
            observed = MathUtils.lcm(n, Integer.MIN_VALUE);
            returned = true;
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            wrong = t;
        }

        if (wrong != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:helper-derived-lcm-overflow] consistency violation: wrong exception type for lcm(" +
                n + ",Integer.MIN_VALUE)", wrong);
        }

        if (returned) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:helper-derived-lcm-overflow] consistency violation: lcm(" +
                n + ",Integer.MIN_VALUE) returned " + observed + " but helper exactMagnitude=" + exactMagnitude);
        }
    }

    private static void checkMulSignSymmetry(FuzzedDataProvider data) {
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);

        int xy;
        int xNegY;
        int negXY;
        try {
            xy = MathUtils.mulAndCheck(x, y);
            xNegY = MathUtils.mulAndCheck(x, -y);
            negXY = MathUtils.mulAndCheck(-x, y);
        } catch (Throwable t) {
            return;
        }

        if (xNegY != -xy) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:mul-sign-symmetry-safe] metamorphic violation: mulAndCheck(x,-y) == -mulAndCheck(x,y) for safe ints x=" +
                x + " y=" + y + " lhs=" + xNegY + " rhs=" + (-xy));
        }
        if (negXY != -xy) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:mul-sign-symmetry-safe] metamorphic violation: mulAndCheck(-x,y) == -mulAndCheck(x,y) for safe ints x=" +
                x + " y=" + y + " lhs=" + negXY + " rhs=" + (-xy));
        }
    }

    private static void expectArithmetic(String oracleId, int x, int y) {
        try {
            int result = MathUtils.lcm(x, y);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned " + result);
        } catch (ArithmeticException expected) {
        }
    }

    private static void assertEquals(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}