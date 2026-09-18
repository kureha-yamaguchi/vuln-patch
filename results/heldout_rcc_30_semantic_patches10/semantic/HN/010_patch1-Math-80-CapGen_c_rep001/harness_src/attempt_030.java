package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryEigenvaluesInvariant(data);
        runReversedTridiagonalMetamorphic(data);
        runSiblingAndHiddenStateChecks(data);
    }

    private static void runLiftedOracle() {
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
        double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Exception e) {
            return;
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            double actual = eigenValues[i];
            double expected = refEigenValues[i];
            if (Math.abs(actual - expected) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i + " expected=" + expected + " actual=" + actual);
            }
        }

        for (int i = 0; i < refEigenVectors.length; ++i) {
            RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Exception e) {
                return;
            }
            RealVector ref = refEigenVectors[i];
            double norm;
            if (ref.dotProduct(actualVector) < 0) {
                norm = ref.add(actualVector).getNorm();
            } else {
                norm = ref.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" + norm);
            }
        }
    }

    private static void runImaginaryEigenvaluesInvariant(FuzzedDataProvider data) {
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
        } catch (Exception e) {
            return;
        }

        // Contract used as oracle: this symmetric-only decomposition exposes no imaginary spectrum;
        // a patch that merely suppresses a failing branch but leaves state corrupted can violate this observable reader.
        if (imag != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:imaginary-eigenvalues-null] relation imaginary-eigenvalue-array-is-null violated: expected null but got length " + imag.length);
        }
    }

    private static void runReversedTridiagonalMetamorphic(FuzzedDataProvider data) {
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
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(revMain, revSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Exception e) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != n || ev2.length != n) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:reversed-multiset-shape] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: wrong eigenvalue array shape");
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
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:reversed-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static void runSiblingAndHiddenStateChecks(FuzzedDataProvider data) {
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
        double[] before;
        double[] byArray;
        RealMatrix v;
        RealMatrix vt;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            before = dec.getRealEigenvalues();
            byArray = dec.getRealEigenvalues();
            v = dec.getV();
            vt = dec.getVT();
        } catch (Exception e) {
            return;
        }

        // Sibling-agreement oracle: getRealEigenvalue(i) and getRealEigenvalues()[i] document the same eigenvalues.
        for (int i = 0; i < n; i++) {
            double scalar;
            try {
                scalar = dec.getRealEigenvalue(i);
            } catch (Exception e) {
                return;
            }
            double arrayValue = byArray[i];
            double tol = 1e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(arrayValue)));
            if (Math.abs(scalar - arrayValue) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue] metamorphic violation: index=" + i + " scalar=" + scalar + " array=" + arrayValue);
            }
        }

        double[] after;
        try {
            after = dec.getRealEigenvalues();
        } catch (Exception e) {
            return;
        }

        // Hidden-state oracle: getV()/getVT()/getRealEigenvalues() are readers; a correct implementation must not
        // silently mutate the decomposition's reported eigenvalues when answering these questions.
        if (before == null || after == null || before.length != after.length) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:hidden-state-shape] metamorphic violation: beforeLength=" +
                (before == null ? -1 : before.length) + " afterLength=" + (after == null ? -1 : after.length));
        }
        for (int i = 0; i < before.length; i++) {
            double a = before[i];
            double b = after[i];
            double tol = 1e-12 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-values] metamorphic violation: index=" + i +
                    " before=" + a + " after=" + b +
                    " vRows=" + (v == null ? -1 : v.getRowDimension()) +
                    " vtRows=" + (vt == null ? -1 : vt.getRowDimension()));
            }
        }
    }
}