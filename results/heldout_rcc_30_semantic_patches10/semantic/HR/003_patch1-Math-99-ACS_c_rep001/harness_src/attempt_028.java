package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquivalentFormsForGcd(Integer.MIN_VALUE);
        checkEquivalentFormsForLcm(Integer.MIN_VALUE);

        int n = data.consumeInt();
        checkEquivalentFormsForGcd(n);
        checkEquivalentFormsForLcm(n);

        runLiftedGcdAssertions();

        int lcmProbe = data.consumeBoolean() ? 1 : (1 << 20);
        try {
            MathUtils.lcm(Integer.MIN_VALUE, lcmProbe);
        } catch (ArithmeticException ignored) {
        }

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException ignored) {
        }
    }

    private static void runLiftedGcdAssertions() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("lifted-gcd-0-0", 0, MathUtils.gcd(0, 0));
        assertEqualsInt("lifted-gcd-0-b", b, MathUtils.gcd(0, b));
        assertEqualsInt("lifted-gcd-a-0", a, MathUtils.gcd(a, 0));
        assertEqualsInt("lifted-gcd-0-negb", b, MathUtils.gcd(0, -b));
        assertEqualsInt("lifted-gcd-nega-0", a, MathUtils.gcd(-a, 0));

        assertEqualsInt("lifted-gcd-a-b", 10, MathUtils.gcd(a, b));
        assertEqualsInt("lifted-gcd-nega-b", 10, MathUtils.gcd(-a, b));
        assertEqualsInt("lifted-gcd-a-negb", 10, MathUtils.gcd(a, -b));
        assertEqualsInt("lifted-gcd-nega-negb", 10, MathUtils.gcd(-a, -b));

        assertEqualsInt("lifted-gcd-a-c", 1, MathUtils.gcd(a, c));
        assertEqualsInt("lifted-gcd-nega-c", 1, MathUtils.gcd(-a, c));
        assertEqualsInt("lifted-gcd-a-negc", 1, MathUtils.gcd(a, -c));
        assertEqualsInt("lifted-gcd-nega-negc", 1, MathUtils.gcd(-a, -c));

        assertEqualsInt("lifted-gcd-scale", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));

        assertEqualsInt("lifted-gcd-max-zero", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        assertEqualsInt("lifted-gcd-negmax-zero", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        assertEqualsInt("lifted-gcd-minpower", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));

        expectArithmetic("lifted-gcd-min-left-zero-throws", Integer.MIN_VALUE, 0);
        expectArithmetic("lifted-gcd-zero-right-min-throws", 0, Integer.MIN_VALUE);
        expectArithmetic("lifted-gcd-min-both-throws", Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private static void checkEquivalentFormsForGcd(int x) {
        ArithmeticException leftException = null;
        ArithmeticException rightException = null;
        Integer leftValue = null;
        Integer rightValue = null;

        try {
            leftValue = Integer.valueOf(MathUtils.gcd(x, 0));
        } catch (ArithmeticException e) {
            leftException = e;
        }

        try {
            rightValue = Integer.valueOf(MathUtils.gcd(x, x));
        } catch (ArithmeticException e) {
            rightException = e;
        }

        /* Contract used: gcd(x, 0) and gcd(x, x) denote the same mathematical quantity |x|.
           Therefore a correct implementation must either return the same int from both real API
           calls or reject both when |x| is unrepresentable (notably Integer.MIN_VALUE). */
        if ((leftException == null) != (rightException == null)) {
            RuntimeException issue = new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-equivalent-forms] consistency violation: x=" + x
                    + " gcd(x,0)=" + renderResult(leftValue, leftException)
                    + " gcd(x,x)=" + renderResult(rightValue, rightException));
            issue.initCause(leftException != null ? leftException : rightException);
            throw issue;
        }
        if (leftException == null && leftValue.intValue() != rightValue.intValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-equivalent-forms] consistency violation: x=" + x
                    + " gcd(x,0)=" + leftValue + " gcd(x,x)=" + rightValue);
        }
    }

    private static void checkEquivalentFormsForLcm(int x) {
        ArithmeticException leftException = null;
        ArithmeticException rightException = null;
        Integer leftValue = null;
        Integer rightValue = null;

        try {
            leftValue = Integer.valueOf(MathUtils.lcm(x, 1));
        } catch (ArithmeticException e) {
            leftException = e;
        }

        try {
            rightValue = Integer.valueOf(MathUtils.lcm(x, x));
        } catch (ArithmeticException e) {
            rightException = e;
        }

        /* Contract used: lcm(x, 1) and lcm(x, x) both denote |x|. Thus both calls must agree on
           the same returned value, or both reject when that nonnegative result is not representable
           as an int (for Integer.MIN_VALUE). This cross-check exercises lcm's real gcd/mulAndCheck path. */
        if ((leftException == null) != (rightException == null)) {
            RuntimeException issue = new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lcm-equivalent-forms] consistency violation: x=" + x
                    + " lcm(x,1)=" + renderResult(leftValue, leftException)
                    + " lcm(x,x)=" + renderResult(rightValue, rightException));
            issue.initCause(leftException != null ? leftException : rightException);
            throw issue;
        }
        if (leftException == null && leftValue.intValue() != rightValue.intValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lcm-equivalent-forms] consistency violation: x=" + x
                    + " lcm(x,1)=" + leftValue + " lcm(x,x)=" + rightValue);
        }
    }

    private static String renderResult(Integer value, ArithmeticException e) {
        if (e != null) {
            return e.getClass().getName() + "(" + e.getMessage() + ")";
        }
        return String.valueOf(value);
    }

    private static void expectArithmetic(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but got " + actual);
        } catch (ArithmeticException expected) {
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}