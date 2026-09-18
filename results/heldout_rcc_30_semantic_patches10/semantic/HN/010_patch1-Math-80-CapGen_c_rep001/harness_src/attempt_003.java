package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracleFromTest();
        checkImaginaryEigenvaluesNull(data);
        checkReversedTridiagonalPreservesEigenvalueMultiset(data);
        checkSiblingAgreementAndReadOnlyState(data);
    }

    private static void checkLiftedOracleFromTest() {
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

        EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues-shape] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (Math.abs(refEigenValues[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue] semantic mismatch: index=" + i + " expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
            double norm;
            if (refEigenVectors[i].dotProduct(actualVector) < 0) {
                norm = refEigenVectors[i].add(actualVector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualVector).getNorm();
            }
            if (Math.abs(norm - 0.0) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" + norm + " tolerance=1.0E-5 actualVector=" + vectorToString(actualVector));
            }
        }

        // Documented getter consistency/read-only guarantee: getRealEigenvalues() and getRealEigenvalue(i) report the same decomposition state,
        // and getters should not mutate that state. A patch that merely avoids the bad branch but corrupts bookkeeping breaks this agreement.
        double[] before = eigenValues.clone();
        double detBefore;
        double[] after;
        try {
            detBefore = decomposition.getDeterminant();
            for (int i = 0; i < before.length; i++) {
                double single = decomposition.getRealEigenvalue(i);
                if (Math.abs(single - before[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(single), Math.abs(before[i])))) {
                    throw new FuzzerSecurityIssueLow("[oracle:lifted-sibling-agreement] semantic mismatch: index=" + i + " arrayValue=" + before[i] + " scalarValue=" + single);
                }
            }
            decomposition.getV();
            decomposition.getVT();
            after = decomposition.getRealEigenvalues();
            double detAfter = decomposition.getDeterminant();
            if (!sameArray(before, after, 0.0)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-readonly-eigenvalues] semantic mismatch: getters mutated eigenvalues before=" + Arrays.toString(before) + " after=" + Arrays.toString(after));
            }
            if (Double.doubleToLongBits(detBefore) != Double.doubleToLongBits(detAfter)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-readonly-determinant] semantic mismatch: getters mutated determinant before=" + detBefore + " after=" + detAfter);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }

    private static void checkImaginaryEigenvaluesNull(FuzzedDataProvider data) {
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
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-eigenvalue-array-is-null] semantic mismatch: expected null but got length " + imag.length);
        }
    }

    private static void checkReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
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
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != n || ev2.length != n) {
            throw new FuzzerSecurityIssueLow("[oracle:reversed-multiset-shape] semantic mismatch: wrong eigenvalue array shape");
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
                throw new FuzzerSecurityIssueLow("[oracle:reversed-multiset-value] semantic mismatch: index=" + i + " lhs=" + a + " rhs=" + b + " tolerance=" + tol);
            }
        }
    }

    private static void checkSiblingAgreementAndReadOnlyState(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 6);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-50000, 50000) / 50.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-50000, 50000) / 50.0;
        }

        EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double[] evBefore;
        double detBefore;
        RealMatrix dBefore;
        try {
            evBefore = dec.getRealEigenvalues();
            detBefore = dec.getDeterminant();
            dBefore = dec.getD();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (evBefore == null || evBefore.length != n || dBefore == null) {
            return;
        }

        // Sibling-agreement guarantee: getRealEigenvalues() and getRealEigenvalue(i) are two public readers over the same decomposition result.
        // Hidden-state guarantee: get*/is* readers are observational queries; calling getEigenvector/getV/getVT/getD must not silently rewrite eigenvalues or determinant.
        double[] evAfter;
        double detAfter;
        try {
            for (int i = 0; i < n; i++) {
                double scalar = dec.getRealEigenvalue(i);
                double arrayVal = evBefore[i];
                double tol = 1e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(arrayVal)));
                if (Math.abs(scalar - arrayVal) > tol) {
                    throw new FuzzerSecurityIssueLow("[oracle:fuzz-sibling-real-eigenvalue] semantic mismatch: index=" + i + " arrayValue=" + arrayVal + " scalarValue=" + scalar + " n=" + n);
                }
                RealVector v = dec.getEigenvector(i);
                if (v == null || v.getDimension() != n) {
                    throw new FuzzerSecurityIssueLow("[oracle:fuzz-eigenvector-shape] semantic mismatch: index=" + i + " expectedDimension=" + n + " actualDimension=" + (v == null ? -1 : v.getDimension()));
                }
            }

            RealMatrix dAgain = dec.getD();
            RealMatrix vMat = dec.getV();
            RealMatrix vtMat = dec.getVT();
            evAfter = dec.getRealEigenvalues();
            detAfter = dec.getDeterminant();

            if (dAgain == null || vMat == null || vtMat == null) {
                return;
            }

            if (!sameArray(evBefore, evAfter, 0.0)) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-readonly-eigenvalues] semantic mismatch: getters mutated eigenvalues before=" + Arrays.toString(evBefore) + " after=" + Arrays.toString(evAfter) + " n=" + n);
            }
            if (Double.doubleToLongBits(detBefore) != Double.doubleToLongBits(detAfter)) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-readonly-determinant] semantic mismatch: getters mutated determinant before=" + detBefore + " after=" + detAfter + " n=" + n);
            }

            // Matrix-shape post-condition from decomposition API: D, V, and VT all represent the same n x n decomposition objects.
            if (dBefore.getRowDimension() != n || dBefore.getColumnDimension() != n ||
                dAgain.getRowDimension() != n || dAgain.getColumnDimension() != n ||
                vMat.getRowDimension() != n || vMat.getColumnDimension() != n ||
                vtMat.getRowDimension() != n || vtMat.getColumnDimension() != n) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-matrix-shape] semantic mismatch: expectedSquareDimension=" + n
                    + " dBefore=" + dBefore.getRowDimension() + "x" + dBefore.getColumnDimension()
                    + " dAgain=" + dAgain.getRowDimension() + "x" + dAgain.getColumnDimension()
                    + " V=" + vMat.getRowDimension() + "x" + vMat.getColumnDimension()
                    + " VT=" + vtMat.getRowDimension() + "x" + vtMat.getColumnDimension());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
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
            double x = a[i];
            double y = b[i];
            if (tol == 0.0) {
                if (Double.doubleToLongBits(x) != Double.doubleToLongBits(y)) {
                    return false;
                }
            } else {
                if (Math.abs(x - y) > tol) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String vectorToString(RealVector v) {
        if (v == null) {
            return "null";
        }
        double[] values = new double[v.getDimension()];
        for (int i = 0; i < values.length; i++) {
            values[i] = v.getEntry(i);
        }
        return Arrays.toString(values);
    }
}