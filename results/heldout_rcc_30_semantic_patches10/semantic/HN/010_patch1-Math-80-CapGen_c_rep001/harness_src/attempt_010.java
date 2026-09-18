package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN_TRI = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY_TRI = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static RealVector[] refEigenVectors() {
        return new RealVector[] {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(MAIN_TRI.clone(), SECONDARY_TRI.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        RealVector[] refEigenVectors = refEigenVectors();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-ref-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]);
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualVector) < 0) {
                norm = refEigenVectors[i].add(actualVector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualVector).getNorm();
            }
            if (norm > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-ref-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNormToReference<=1.0E-5 actualNorm=" + norm);
            }
        }

        // Sibling-agreement check: getRealEigenvalue(i) and getRealEigenvalues()[i] are two readers of the same decomposition state.
        for (int i = 0; i < eigenValues.length; i++) {
            double single = decomposition.getRealEigenvalue(i);
            if (Math.abs(single - eigenValues[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(single), Math.abs(eigenValues[i])))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue] semantic mismatch: index=" + i +
                    " arrayValue=" + eigenValues[i] + " singleValue=" + single);
            }
        }

        // Hidden-state post-condition: get* methods are read-only queries; a throw-deleting or state-corrupting patch would make later reads change.
        double[] before = decomposition.getRealEigenvalues().clone();
        decomposition.getDeterminant();
        decomposition.getV();
        decomposition.getVT();
        decomposition.getD();
        double[] after = decomposition.getRealEigenvalues();
        for (int i = 0; i < before.length; i++) {
            if (Math.abs(before[i] - after[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(before[i]), Math.abs(after[i])))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:readonly-getters-stable] semantic mismatch: index=" + i +
                    " before=" + before[i] + " after=" + after[i]);
            }
        }

        relationImaginaryEigenvaluesNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
        relationSolverOverloadsAgree(data);
    }

    private static void relationImaginaryEigenvaluesNull(FuzzedDataProvider data) {
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

        // Contract: for this symmetric-only decomposition, getImagEigenvalues() returns null.
        if (imag != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:imaginary-eigenvalue-array-is-null] metamorphic violation: expected null but got length=" + imag.length);
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
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:reversed-tridiagonal-preserves-eigenvalue-multiset] metamorphic violation: wrong eigenvalue array shape");
        }

        double[] s1 = ev1.clone();
        double[] s2 = ev2.clone();
        Arrays.sort(s1);
        Arrays.sort(s2);

        // Reversing a symmetric tridiagonal matrix is a similarity transform, so the real eigenvalue multiset must be preserved.
        for (int i = 0; i < n; i++) {
            double a = s1[i];
            double b = s2[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:reversed-tridiagonal-preserves-eigenvalue-multiset] metamorphic violation: index=" + i +
                    " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static void relationSolverOverloadsAgree(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 4);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(50, 5000) / 10.0 + i;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-50, 50) / 100.0;
        }
        double[] rhs = new double[n];
        for (int i = 0; i < n; i++) {
            rhs[i] = data.consumeInt(-10000, 10000) / 100.0;
        }

        double[] solvedArray;
        double[] solvedVector;
        try {
            EigenDecompositionImpl dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            DecompositionSolver solver = dec.getSolver();
            if (!solver.isNonSingular()) {
                return;
            }
            solvedArray = solver.solve(rhs);
            RealVector rv = solver.solve(new ArrayRealVector(rhs));
            solvedVector = rv.getData();
        } catch (Throwable t) {
            return;
        }

        // Sibling-agreement check: solve(double[]) and solve(RealVector) are overloads for the same linear solve.
        if (solvedArray.length != solvedVector.length) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:solve-overloads-agree] metamorphic violation: lengthArray=" + solvedArray.length +
                " lengthVector=" + solvedVector.length);
        }
        for (int i = 0; i < solvedArray.length; i++) {
            double a = solvedArray[i];
            double b = solvedVector[i];
            double tol = 1e-8 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:solve-overloads-agree] metamorphic violation: index=" + i +
                    " lhs=" + a + " rhs=" + b);
            }
        }
    }
}