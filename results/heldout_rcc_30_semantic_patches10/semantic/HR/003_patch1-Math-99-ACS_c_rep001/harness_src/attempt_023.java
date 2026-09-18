package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.fraction.Fraction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmAndGcdBoundaryOracles();
        checkFractionMultiplyReductionConsistency(data);
    }

    private static void runLiftedLcmAndGcdBoundaryOracles() {
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

        expectArithmeticFromLcm("lifted-lcm-min-one-throws", Integer.MIN_VALUE, 1);
        expectArithmeticFromLcm("lifted-lcm-min-pow-throws", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticFromLcm("lifted-lcm-max-maxminus-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);

        expectArithmeticFromGcd("lifted-gcd-min-left-throws", Integer.MIN_VALUE, 0);
        expectArithmeticFromGcd("lifted-gcd-min-right-throws", 0, Integer.MIN_VALUE);
        expectArithmeticFromGcd("lifted-gcd-min-both-throws", Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private static void checkFractionMultiplyReductionConsistency(FuzzedDataProvider data) {
        int n1 = data.consumeInt(-1000, 1000);
        int d1 = data.consumeInt(1, 1000);
        int n2 = data.consumeInt(-1000, 1000);
        int d2 = data.consumeInt(1, 1000);

        Fraction left;
        Fraction right;
        Fraction product;
        int mulNum;
        int mulDen;
        Fraction independentlyReduced;

        try {
            left = Fraction.getReducedFraction(n1, d1);
            right = Fraction.getReducedFraction(n2, d2);
            product = left.multiply(right);
            mulNum = MathUtils.mulAndCheck(n1, n2);
            mulDen = MathUtils.mulAndCheck(d1, d2);
            independentlyReduced = Fraction.getReducedFraction(mulNum, mulDen);
        } catch (Throwable t) {
            return;
        }

        int productNum;
        int productDen;
        int independentNum;
        int independentDen;
        try {
            productNum = product.getNumerator();
            productDen = product.getDenominator();
            independentNum = independentlyReduced.getNumerator();
            independentDen = independentlyReduced.getDenominator();
        } catch (Throwable t) {
            return;
        }

        /* Fraction.multiply is documented as the product, and Fraction.getReducedFraction
           canonicalizes an equivalent numerator/denominator pair. Recomputing the same product
           through real library calls (mulAndCheck + getReducedFraction) must therefore produce
           the same reduced numerator/denominator for every correct implementation. This would
           still catch a band-aid around lcm/gcd if helper arithmetic or reduction stayed wrong. */
        if (productNum != independentNum || productDen != independentDen) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fraction-multiply-reduction-consistency] consistency violation: " +
                "n1=" + n1 + " d1=" + d1 + " n2=" + n2 + " d2=" + d2 +
                " product=" + productNum + "/" + productDen +
                " independentlyReduced=" + independentNum + "/" + independentDen);
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticFromLcm(String oracleId, int a, int b) {
        boolean violated = false;
        try {
            int actual = MathUtils.lcm(a, b);
            violated = true;
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual +
                    " for lcm(" + a + "," + b + ")");
            }
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void expectArithmeticFromGcd(String oracleId, int a, int b) {
        boolean violated = false;
        try {
            int actual = MathUtils.gcd(a, b);
            violated = true;
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual +
                    " for gcd(" + a + "," + b + ")");
            }
        } catch (ArithmeticException expected) {
            return;
        }
    }
}