package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = new double[] {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = new double[] {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = new double[] {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = new RealVector[] {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedOracleFromFailingTest();
        readOnlyAndSiblingAgreementOnSeed();
        relationImaginaryEigenvaluesNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedOracleFromFailingTest() {
        final EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvalue] semantic mismatch: expected="
                        + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            final RealVector actualEigenvector = decomposition.getEigenvector(i);
            final double dot = REF_EIGENVECTORS[i].dotProduct(actualEigenvector);
            final double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualEigenvector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualEigenvector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvector] semantic mismatch: expectedNormToReference=0.0 actualNorm="
                        + norm + " index=" + i + " dot=" + dot);
            }
        }
    }

    private static void readOnlyAndSiblingAgreementOnSeed() {
        final EigenDecompositionImpl dec =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] before = dec.getRealEigenvalues();
        final double[] beforeClone = before.clone();
        final double determinantBefore = dec.getDeterminant();

        final double[] after = dec.getRealEigenvalues();
        final double determinantAfter = dec.getDeterminant();

        /* Contract justification: get* methods are readers; repeated reads on the same decomposition
           should not mutate observable state. A patch that only suppresses/skirts the buggy branch can
           still leave corrupted work/pingPong state that later readers expose differently. */
        if (before.length != after.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-length] semantic mismatch: beforeLength=" + before.length
                    + " afterLength=" + after.length);
        }
        for (int i = 0; i < before.length; i++) {
            if (Double.doubleToLongBits(beforeClone[i]) != Double.doubleToLongBits(after[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-readonly-eigenvalues] semantic mismatch: index=" + i
                        + " before=" + beforeClone[i] + " after=" + after[i]);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-determinant] semantic mismatch: before=" + determinantBefore
                    + " after=" + determinantAfter);
        }

        /* Contract justification: getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers
           for the same decomposition and must agree on each index for any correct implementation. */
        for (int i = 0; i < before.length; i++) {
            final double scalar = dec.getRealEigenvalue(i);
            final double arrayValue = after[i];
            if (Double.doubleToLongBits(scalar) != Double.doubleToLongBits(arrayValue)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-sibling-real-eigenvalue] semantic mismatch: scalar="
                        + scalar + " array=" + arrayValue + " index=" + i);
            }
        }

        /* Contract justification: this symmetric-only implementation reports no imaginary eigenvalues;
           getImagEigenvalues() must therefore be null for a valid symmetric tridiagonal decomposition. */
        final double[] imag = dec.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }
    }

    private static void relationImaginaryEigenvaluesNull(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        final double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] semantic mismatch: expected null but got array length "
                    + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        final double[] rMain = new double[n];
        final double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        final double[] ev1;
        final double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reversed-count] semantic mismatch: different eigenvalue counts "
                    + ev1.length + " vs " + ev2.length);
        }

        final double[] a = ev1.clone();
        final double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            final double x = a[i];
            final double y = b[i];
            final double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reversed-multiset] semantic mismatch: index="
                        + i + " lhs=" + x + " rhs=" + y);
            }
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}