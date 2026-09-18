package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final String[] EIGENVALUE_ORACLE_IDS = {
        "seed-eigenvalue-0",
        "seed-eigenvalue-1",
        "seed-eigenvalue-2",
        "seed-eigenvalue-3",
        "seed-eigenvalue-4",
        "seed-eigenvalue-5",
        "seed-eigenvalue-6"
    };

    private static final String[] EIGENVECTOR_ORACLE_IDS = {
        "seed-eigenvector-0",
        "seed-eigenvector-1",
        "seed-eigenvector-2",
        "seed-eigenvector-3",
        "seed-eigenvector-4",
        "seed-eigenvector-5",
        "seed-eigenvector-6"
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVALUE_ORACLE_IDS[i]
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVECTOR_ORACLE_IDS[i]
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        // Contract justification: these are documented getter/is-reader methods.
        // A patch that merely avoids the failing path but corrupts shared internal state
        // would violate read-only stability; repeated reads must agree for the same object.
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
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
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-random] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
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

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
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
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
                    return false;
                }
            }
        }
        return true;
    }
}