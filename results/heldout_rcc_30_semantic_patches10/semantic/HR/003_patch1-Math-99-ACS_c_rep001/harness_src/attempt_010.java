package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.fraction.Fraction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedGcdAssertions();

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

        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
        } catch (ArithmeticException ignored) {
        }
        try {
            MathUtils.lcm(1, Integer.MIN_VALUE);
        } catch (ArithmeticException ignored) {
        }

        checkFractionMinDenominatorReduction(data);
    }

    private static void runLiftedGcdAssertions() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("lifted-gcd-00", 0, MathUtils.gcd(0, 0));
        assertEqualsInt("lifted-gcd-0b", b, MathUtils.gcd(0, b));
        assertEqualsInt("lifted-gcd-a0", a, MathUtils.gcd(a, 0));
        assertEqualsInt("lifted-gcd-0negb", b, MathUtils.gcd(0, -b));
        assertEqualsInt("lifted-gcd-nega0", a, MathUtils.gcd(-a, 0));

        assertEqualsInt("lifted-gcd-ab", 10, MathUtils.gcd(a, b));
        assertEqualsInt("lifted-gcd-negab", 10, MathUtils.gcd(-a, b));
        assertEqualsInt("lifted-gcd-anegb", 10, MathUtils.gcd(a, -b));
        assertEqualsInt("lifted-gcd-neganegb", 10, MathUtils.gcd(-a, -b));

        assertEqualsInt("lifted-gcd-ac", 1, MathUtils.gcd(a, c));
        assertEqualsInt("lifted-gcd-negac", 1, MathUtils.gcd(-a, c));
        assertEqualsInt("lifted-gcd-anegc", 1, MathUtils.gcd(a, -c));
        assertEqualsInt("lifted-gcd-neganegc", 1, MathUtils.gcd(-a, -c));

        assertEqualsInt("lifted-gcd-scale", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));

        assertEqualsInt("lifted-gcd-max0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        assertEqualsInt("lifted-gcd-negmax0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        assertEqualsInt("lifted-gcd-minpower", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));

        boolean leftThrows = throwsArithmeticOnGcd(Integer.MIN_VALUE, 0);
        boolean rightThrows = throwsArithmeticOnGcd(0, Integer.MIN_VALUE);
        boolean bothThrows = throwsArithmeticOnGcd(Integer.MIN_VALUE, Integer.MIN_VALUE);

        if (!leftThrows) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-gcd-min-left] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE, 0) must throw ArithmeticException");
        }
        if (!rightThrows) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-gcd-min-right] semantic mismatch: MathUtils.gcd(0, Integer.MIN_VALUE) must throw ArithmeticException");
        }
        if (!bothThrows) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-gcd-min-both] semantic mismatch: MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE) must throw ArithmeticException");
        }
    }

    private static boolean throwsArithmeticOnGcd(int x, int y) {
        try {
            MathUtils.gcd(x, y);
            return false;
        } catch (ArithmeticException expected) {
            return true;
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkFractionMinDenominatorReduction(FuzzedDataProvider data) {
        int k = data.consumeInt(1, 29);
        int numerator = 1 << k;
        int expectedDenominator = 1 << (31 - k);

        try {
            Fraction reduced = Fraction.getReducedFraction(numerator, Integer.MIN_VALUE);
            Fraction canonical = Fraction.getReducedFraction(-1, expectedDenominator);

            int rn = reduced.getNumerator();
            int rd = reduced.getDenominator();
            int cn = canonical.getNumerator();
            int cd = canonical.getDenominator();

            if (rn != cn || rd != cd) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fraction-min-denominator-canonical] metamorphic violation: equivalent fraction reductions disagree k="
                                + k + " reduced=" + rn + "/" + rd + " canonical=" + cn + "/" + cd);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}