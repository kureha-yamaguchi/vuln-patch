package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exactLiftedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNullForSymmetricInput(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void exactLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!(Math.abs(refEigenValues[i] - eigenValues[i]) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue] semantic mismatch: index=" + i + " expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualEigenvector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualEigenvector) < 0) {
                norm = refEigenVectors[i].add(actualEigenvector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualEigenvector).getNorm();
            }
            if (!(Math.abs(0.0 - norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        EigenDecompositionImpl dec =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double determinantBefore = dec.getDeterminant();
        double[] imagBefore = dec.getImagEigenvalues();
        double[] values1 = dec.getRealEigenvalues();
        double determinantAfter = dec.getDeterminant();
        double[] imagAfter = dec.getImagEigenvalues();
        double[] values2 = dec.getRealEigenvalues();

        /* Contract justification:
         * get* readers are documented as queries; a throw-deleting/branch-skipping patch in flip/processGeneralBlock
         * could leave shared state (work/pingPong-derived outputs) inconsistent across readers.
         * So repeated reads must agree, getRealEigenvalue(i) must match getRealEigenvalues()[i], and
         * this symmetric decomposition must keep imaginary eigenvalues null.
         */
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-imag-null] semantic mismatch: expected null imaginary eigenvalues before/after reads but got before=" +
                (imagBefore == null ? "null" : ("len=" + imagBefore.length)) +
                " after=" + (imagAfter == null ? "null" : ("len=" + imagAfter.length)));
        }
        if (values1.length != values2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-length] semantic mismatch: firstLength=" + values1.length +
                " secondLength=" + values2.length);
        }
        for (int i = 0; i < values1.length; i++) {
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(values2[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-repeat] semantic mismatch: index=" + i + " first=" +
                    values1[i] + " second=" + values2[i]);
            }
            double scalar = dec.getRealEigenvalue(i);
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(scalar)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue] semantic mismatch: index=" + i + " arrayValue=" +
                    values1[i] + " scalarValue=" + scalar);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: determinantBefore=" +
                determinantBefore + " determinantAfter=" + determinantAfter);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNullForSymmetricInput(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reversed-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reversed-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" +
                    i + " x=" + x + " y=" + y + " tol=" + tol);
            }
        }
    }
}