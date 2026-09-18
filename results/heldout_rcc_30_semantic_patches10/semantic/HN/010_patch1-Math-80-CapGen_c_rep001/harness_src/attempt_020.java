package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracle();
        exactPostConditions();

        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
        oracleDiagonalTridiagonalEigenvaluesEqualDiagonal(data);
    }

    private static void liftedTestOracle() {
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
        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-array-shape] semantic mismatch: expectedLength=" +
                refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue] semantic mismatch: index=" + i + " expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualVector) < 0) {
                norm = refEigenVectors[i].add(actualVector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualVector).getNorm();
            }
            if (!(Math.abs(norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" +
                    norm + " tol=1.0E-5");
            }
        }
    }

    private static void exactPostConditions() {
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

        double[] before = dec.getRealEigenvalues();
        if (before == null) {
            throw new FuzzerSecurityIssueLow("[oracle:read-before] semantic mismatch: getRealEigenvalues returned null");
        }

        RealMatrix d = dec.getD();
        RealMatrix v = dec.getV();
        RealMatrix vt = dec.getVT();
        double determinant = dec.getDeterminant();
        DecompositionSolver solver = dec.getSolver();
        boolean nonSingular = solver.isNonSingular();
        double[] imag = dec.getImagEigenvalues();
        double[] after = dec.getRealEigenvalues();

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-exact] semantic mismatch: expected null actualLength=" + imag.length);
        }

        if (!sameArray(before, after, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-getters-stable] semantic mismatch: getRealEigenvalues changed across read-only getters");
        }

        if (d == null || v == null || vt == null || solver == null) {
            throw new FuzzerSecurityIssueLow("[oracle:non-null-components] semantic mismatch: decomposition component unexpectedly null");
        }

        for (int i = 0; i < before.length; i++) {
            double scalar = dec.getRealEigenvalue(i);
            if (!withinTol(before[i], scalar, 1.0e-12 * Math.max(1.0, Math.max(Math.abs(before[i]), Math.abs(scalar))))) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue] semantic mismatch: index=" + i + " arrayValue=" +
                    before[i] + " scalarValue=" + scalar);
            }

            double diagonal = d.getEntry(i, i);
            if (!withinTol(before[i], diagonal, 1.0e-12 * Math.max(1.0, Math.max(Math.abs(before[i]), Math.abs(diagonal))))) {
                throw new FuzzerSecurityIssueLow("[oracle:d-matrix-diagonal] semantic mismatch: index=" + i + " eigenvalueArray=" +
                    before[i] + " dDiagonal=" + diagonal);
            }

            for (int j = 0; j < before.length; j++) {
                if (i != j) {
                    double off = d.getEntry(i, j);
                    if (!withinTol(0.0, off, 1.0e-12)) {
                        throw new FuzzerSecurityIssueLow("[oracle:d-matrix-offdiag] semantic mismatch: row=" + i + " col=" + j + " expected=0.0 actual=" + off);
                    }
                }
            }
        }

        double detFromEigenvalues = 1.0;
        for (int i = 0; i < before.length; i++) {
            detFromEigenvalues *= before[i];
        }
        double detTol = 1.0e-10 * Math.max(1.0, Math.max(Math.abs(detFromEigenvalues), Math.abs(determinant)));
        if (!withinTol(detFromEigenvalues, determinant, detTol)) {
            throw new FuzzerSecurityIssueLow("[oracle:determinant-product] semantic mismatch: determinant=" +
                determinant + " productOfEigenvalues=" + detFromEigenvalues + " nonSingular=" + nonSingular);
        }

        RealMatrix reconstructed = v.multiply(d).multiply(vt);
        double[][] expectedA = tridiagonalToFull(mainTridiagonal, secondaryTridiagonal);
        double maxDiff = maxAbsDiff(reconstructed, expectedA);
        if (!(maxDiff <= 1.0e-6 * Math.max(1.0, maxAbs(expectedA)))) {
            throw new FuzzerSecurityIssueLow("[oracle:reconstruct-original-matrix] semantic mismatch: maxAbsDiff=" + maxDiff);
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
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-imag-null] semantic mismatch: expected null but got length " + imag.length);
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

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(revMain, revSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != n || ev2.length != n) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-reverse-shape] semantic mismatch: wrong eigenvalue array shape");
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
                throw new FuzzerSecurityIssueLow("[oracle:relation-reverse-multiset] semantic mismatch: index=" + i + " lhs=" + a + " rhs=" + b + " tol=" + tol);
            }
        }
    }

    private static void oracleDiagonalTridiagonalEigenvaluesEqualDiagonal(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 6);
        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-50000, 50000) / 100.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = 0.0;
        }

        double[] actual;
        RealMatrix d;
        try {
            EigenDecompositionImpl dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            actual = dec.getRealEigenvalues();
            d = dec.getD();
        } catch (Throwable t) {
            return;
        }

        if (actual == null || actual.length != n || d == null) {
            throw new FuzzerSecurityIssueLow("[oracle:diagonal-shape] semantic mismatch: expectedLength=" + n +
                " actualLength=" + (actual == null ? -1 : actual.length));
        }

        double[] expected = main.clone();
        double[] sortedActual = actual.clone();
        Arrays.sort(expected);
        Arrays.sort(sortedActual);

        for (int i = 0; i < n; i++) {
            double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(expected[i]), Math.abs(sortedActual[i])));
            if (!withinTol(expected[i], sortedActual[i], tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:diagonal-eigenvalues] semantic mismatch: index=" + i + " expected=" +
                    expected[i] + " actual=" + sortedActual[i]);
            }
        }

        double[] dDiag = new double[n];
        for (int i = 0; i < n; i++) {
            dDiag[i] = d.getEntry(i, i);
        }
        Arrays.sort(dDiag);
        for (int i = 0; i < n; i++) {
            double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(expected[i]), Math.abs(dDiag[i])));
            if (!withinTol(expected[i], dDiag[i], tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:diagonal-dmatrix] semantic mismatch: index=" + i + " expected=" +
                    expected[i] + " dDiagonal=" + dDiag[i]);
            }
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return false;
        }
        return Math.abs(expected - actual) <= tol;
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double t = Math.max(tol, 1.0e-12 * Math.max(1.0, Math.max(Math.abs(a[i]), Math.abs(b[i]))));
            if (!withinTol(a[i], b[i], t)) {
                return false;
            }
        }
        return true;
    }

    private static double[][] tridiagonalToFull(double[] main, double[] secondary) {
        int n = main.length;
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            a[i][i] = main[i];
            if (i + 1 < n) {
                a[i][i + 1] = secondary[i];
                a[i + 1][i] = secondary[i];
            }
        }
        return a;
    }

    private static double maxAbsDiff(RealMatrix m, double[][] expected) {
        double max = 0.0;
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[i].length; j++) {
                max = Math.max(max, Math.abs(m.getEntry(i, j) - expected[i][j]));
            }
        }
        return max;
    }

    private static double maxAbs(double[][] a) {
        double max = 0.0;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                max = Math.max(max, Math.abs(a[i][j]));
            }
        }
        return max;
    }
}