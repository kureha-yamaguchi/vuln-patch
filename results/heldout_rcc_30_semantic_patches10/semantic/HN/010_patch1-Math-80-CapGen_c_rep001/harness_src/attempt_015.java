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

    private static final double[][] REF_EIGENVECTORS = new double[][] {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        liftedSeedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
        relationConstructKnownDiagonal(data);
    }

    private static void liftedSeedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalues-shape] semantic mismatch: expectedLength="
                    + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw seedEigenvalueAlarm(i, expected, actual);
            }
            final RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVec;
            try {
                actualVec = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double norm;
            if (ref.dotProduct(actualVec) < 0) {
                norm = ref.add(actualVec).getNorm();
            } else {
                norm = ref.subtract(actualVec).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw seedEigenvectorAlarm(i, norm);
            }
        }
    }

    private static FuzzerSecurityIssueLow seedEigenvalueAlarm(int i, double expected, double actual) {
        switch (i) {
            case 0:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-0] semantic mismatch: expected=" + expected + " actual=" + actual);
            case 1:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-1] semantic mismatch: expected=" + expected + " actual=" + actual);
            case 2:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-2] semantic mismatch: expected=" + expected + " actual=" + actual);
            case 3:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-3] semantic mismatch: expected=" + expected + " actual=" + actual);
            case 4:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-4] semantic mismatch: expected=" + expected + " actual=" + actual);
            case 5:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-5] semantic mismatch: expected=" + expected + " actual=" + actual);
            case 6:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-6] semantic mismatch: expected=" + expected + " actual=" + actual);
            default:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-other] semantic mismatch: index=" + i + " expected=" + expected + " actual=" + actual);
        }
    }

    private static FuzzerSecurityIssueLow seedEigenvectorAlarm(int i, double norm) {
        switch (i) {
            case 0:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-0] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            case 1:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-1] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            case 2:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-2] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            case 3:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-3] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            case 4:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-4] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            case 5:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-5] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            case 6:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-6] semantic mismatch: expectedNormToBe0 actualNorm=" + norm);
            default:
                return new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-other] semantic mismatch: index=" + i + " expectedNormToBe0 actualNorm=" + norm);
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] before;
        final double determinantBefore;
        final RealMatrix vBefore;
        final RealMatrix vtBefore;
        final RealMatrix dBefore;
        final DecompositionSolver solverBefore;
        try {
            before = dec.getRealEigenvalues().clone();
            determinantBefore = dec.getDeterminant();
            vBefore = dec.getV();
            vtBefore = dec.getVT();
            dBefore = dec.getD();
            solverBefore = dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] after;
        final double determinantAfter;
        final RealMatrix vAfter;
        final RealMatrix vtAfter;
        final RealMatrix dAfter;
        final DecompositionSolver solverAfter;
        try {
            after = dec.getRealEigenvalues().clone();
            determinantAfter = dec.getDeterminant();
            vAfter = dec.getV();
            vtAfter = dec.getVT();
            dAfter = dec.getD();
            solverAfter = dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < before.length; i++) {
            if (Math.abs(before[i] - after[i]) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:readers-nonmutating-eigenvalues] semantic mismatch: before="
                        + Arrays.toString(before) + " after=" + Arrays.toString(after));
            }
        }
        if (Math.abs(determinantBefore - determinantAfter) > 0.0) {
            throw new FuzzerSecurityIssueLow("[oracle:readers-nonmutating-determinant] semantic mismatch: before="
                    + determinantBefore + " after=" + determinantAfter);
        }

        if (!sameMatrix(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:readers-nonmutating-v] semantic mismatch");
        }
        if (!sameMatrix(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:readers-nonmutating-vt] semantic mismatch");
        }
        if (!sameMatrix(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:readers-nonmutating-d] semantic mismatch");
        }
        if (solverBefore == null || solverAfter == null) {
            throw new FuzzerSecurityIssueLow("[oracle:solver-reader] semantic mismatch: solver null");
        }

        for (int i = 0; i < before.length; i++) {
            final double perIndex;
            try {
                perIndex = dec.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }
            if (Math.abs(before[i] - perIndex) > 0.0) {
                throw siblingRealEigenvalueAlarm(i, before[i], perIndex);
            }
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-seed] semantic mismatch: expectedNull actualLength=" + imag.length);
        }
    }

    private static FuzzerSecurityIssueLow siblingRealEigenvalueAlarm(int i, double arrayValue, double perIndexValue) {
        switch (i) {
            case 0:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-0] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            case 1:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-1] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            case 2:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-2] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            case 3:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-3] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            case 4:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-4] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            case 5:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-5] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            case 6:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-6] semantic mismatch: arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
            default:
                return new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-other] semantic mismatch: index=" + i + " arrayValue=" + arrayValue + " perIndexValue=" + perIndexValue);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 6);
        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-20000, 20000) / 20.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-20000, 20000) / 20.0;
        }

        EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-eigenvalue-array-is-null] relation imaginary-eigenvalue-array-is-null violated: expected null but got length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 6);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-100000, 100000) / 100.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-100000, 100000) / 100.0;
        }
        double[] revMain = new double[n];
        double[] revSecondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            revMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < n - 1; i++) {
            revSecondary[i] = secondary[n - 2 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(revMain, revSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != n || ev2.length != n) {
            throw new FuzzerSecurityIssueLow("[oracle:reversed-tridiagonal-shape] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: wrong eigenvalue array shape");
        }

        double[] s1 = ev1.clone();
        double[] s2 = ev2.clone();
        Arrays.sort(s1);
        Arrays.sort(s2);
        for (int i = 0; i < n; i++) {
            double a = s1[i];
            double b = s2[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new FuzzerSecurityIssueLow("[oracle:reversed-tridiagonal-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at "
                        + i + ": " + a + " vs " + b);
            }
        }
    }

    private static void relationConstructKnownDiagonal(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 6);
        double[] diag = new double[n];
        for (int i = 0; i < n; i++) {
            diag[i] = data.consumeInt(-50000, 50000) / 100.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = 0.0;
        }

        EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(diag, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        double[] ev;
        RealMatrix d;
        try {
            ev = dec.getRealEigenvalues();
            d = dec.getD();
        } catch (Throwable t) {
            return;
        }

        double[] expected = diag.clone();
        double[] actual = ev.clone();
        Arrays.sort(expected);
        Arrays.sort(actual);
        for (int i = 0; i < n; i++) {
            double a = expected[i];
            double b = actual[i];
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new FuzzerSecurityIssueLow("[oracle:diagonal-eigenvalues] metamorphic violation: diagonal matrix eigenvalues must equal diagonal entries expected="
                        + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
            }
        }

        double[] dDiag = new double[n];
        for (int i = 0; i < n; i++) {
            dDiag[i] = d.getEntry(i, i);
        }
        Arrays.sort(dDiag);
        for (int i = 0; i < n; i++) {
            double a = actual[i];
            double b = dDiag[i];
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new FuzzerSecurityIssueLow("[oracle:getd-agrees-eigenvalues] semantic mismatch: eigenvalues="
                        + Arrays.toString(actual) + " dDiagonal=" + Arrays.toString(dDiag));
            }
        }
    }

    private static boolean sameMatrix(RealMatrix a, RealMatrix b, double tol) {
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
                if (Math.abs(a.getEntry(i, j) - b.getEntry(i, j)) > tol) {
                    return false;
                }
            }
        }
        return true;
    }
}