package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestLcmOracles();
        checkGcdBoundaryRejections();
        checkLcmMinValuePowerOfTwoRejections(data);
        checkGcdLcmConsistency(data);
    }

    private static void checkLiftedTestLcmOracles() {
        try {
            assertEqualsInt("[oracle:lcm-0-b]", 0, MathUtils.lcm(0, 50));
            assertEqualsInt("[oracle:lcm-a-0]", 0, MathUtils.lcm(30, 0));
            assertEqualsInt("[oracle:lcm-1-b]", 50, MathUtils.lcm(1, 50));
            assertEqualsInt("[oracle:lcm-a-1]", 30, MathUtils.lcm(30, 1));
            assertEqualsInt("[oracle:lcm-a-b]", 150, MathUtils.lcm(30, 50));
            assertEqualsInt("[oracle:lcm-neg-a-b]", 150, MathUtils.lcm(-30, 50));
            assertEqualsInt("[oracle:lcm-a-neg-b]", 150, MathUtils.lcm(30, -50));
            assertEqualsInt("[oracle:lcm-neg-a-neg-b]", 150, MathUtils.lcm(-30, -50));
            assertEqualsInt("[oracle:lcm-a-c]", 2310, MathUtils.lcm(30, 77));
            assertEqualsInt("[oracle:lcm-pow2-scale]", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
            assertEqualsInt("[oracle:lcm-0-0]", 0, MathUtils.lcm(0, 0));
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-values] unexpected exception on exact test inputs", t);
        }

        expectArithmeticExceptionLcm("[oracle:lcm-min-1-throws]", Integer.MIN_VALUE, 1);
        expectArithmeticExceptionLcm("[oracle:lcm-min-pow2-throws]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticExceptionLcm("[oracle:lcm-max-maxminus1-throws]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void checkGcdBoundaryRejections() {
        expectArithmeticExceptionGcd("[oracle:gcd-min-0-throws]", Integer.MIN_VALUE, 0);
        expectArithmeticExceptionGcd("[oracle:gcd-0-min-throws]", 0, Integer.MIN_VALUE);
        expectArithmeticExceptionGcd("[oracle:gcd-min-min-throws]", Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private static void checkLcmMinValuePowerOfTwoRejections(FuzzedDataProvider data) {
        int k = data.consumeInt(0, 30);
        boolean neg = data.consumeBoolean();
        int n = 1 << k;
        if (neg) {
            n = -n;
        }

        boolean violated = false;
        String why = null;
        try {
            MathUtils.lcm(Integer.MIN_VALUE, n);
            violated = true;
            why = "completed normally for lcm(Integer.MIN_VALUE," + n + ")";
        } catch (ArithmeticException ok) {
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-minvalue-powerof2-left] wrong exception for lcm(Integer.MIN_VALUE," + n + ")", t);
        }
        if (violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-minvalue-powerof2-left] semantic mismatch: " + why);
        }

        violated = false;
        why = null;
        try {
            MathUtils.lcm(n, Integer.MIN_VALUE);
            violated = true;
            why = "completed normally for lcm(" + n + ",Integer.MIN_VALUE)";
        } catch (ArithmeticException ok) {
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-minvalue-powerof2-right] wrong exception for lcm(" + n + ",Integer.MIN_VALUE)", t);
        }
        if (violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-minvalue-powerof2-right] semantic mismatch: " + why);
        }
    }

    private static void checkGcdLcmConsistency(FuzzedDataProvider data) {
        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);

        if (a == 0 || b == 0) {
            return;
        }

        int g;
        int l;
        try {
            g = MathUtils.gcd(a, b);
            l = MathUtils.lcm(a, b);
        } catch (Throwable t) {
            return;
        }

        long lhs = ((long) g) * ((long) l);
        long rhs = Math.abs(((long) a) * ((long) b));

        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:gcd-lcm-product] consistency violation: gcd(a,b)*lcm(a,b)==abs(a*b) for all non-zero ints when both real API calls succeed; a="
                            + a + " b=" + b + " gcd=" + g + " lcm=" + l + " lhs=" + lhs + " rhs=" + rhs);
        }

        int lSwapped;
        try {
            lSwapped = MathUtils.lcm(b, a);
        } catch (Throwable t) {
            return;
        }

        if (l != lSwapped) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lcm-commutative] metamorphic violation: lcm(a,b) must equal lcm(b,a); a="
                            + a + " b=" + b + " lhs=" + l + " rhs=" + lSwapped);
        }
    }

    private static void expectArithmeticExceptionLcm(String oracleId, int a, int b) {
        boolean violated = false;
        String why = null;
        try {
            int result = MathUtils.lcm(a, b);
            violated = true;
            why = "completed normally with result=" + result + " for lcm(" + a + "," + b + ")";
        } catch (ArithmeticException ok) {
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " wrong exception for lcm(" + a + "," + b + ")", t);
        }
        if (violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + why);
        }
    }

    private static void expectArithmeticExceptionGcd(String oracleId, int a, int b) {
        boolean violated = false;
        String why = null;
        try {
            int result = MathUtils.gcd(a, b);
            violated = true;
            why = "completed normally with result=" + result + " for gcd(" + a + "," + b + ")";
        } catch (ArithmeticException ok) {
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " wrong exception for gcd(" + a + "," + b + ")", t);
        }
        if (violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + why);
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}