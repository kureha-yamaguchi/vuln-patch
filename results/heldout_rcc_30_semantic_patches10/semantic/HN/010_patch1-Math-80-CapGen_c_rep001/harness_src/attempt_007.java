package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] FIXED_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] FIXED_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] FIXED_REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] FIXED_REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryEigenvalueNullRelation(data);
        runReversePreservesEigenvaluesRelation(data);
        runConstructedDiagonalOracle(data);
    }

    private static void runLiftedOracle() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(FIXED_MAIN, FIXED_SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues == null || eigenValues.length != FIXED_REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:fixed-shape] semantic mismatch: expected eigenvalue array length "
                + FIXED_REF_EIGENVALUES.length + " actual=" + (eigenValues == null ? "null" : eigenValues.length));
        }

        for (int i = 0; i < FIXED_REF_EIGENVALUES.length; ++i) {
            assertCloseFixedEigenvalue(i, FIXED_REF_EIGENVALUES[i], eigenValues[i], 1.0e-3);
        }

        for (int i = 0; i < FIXED_REF_EIGENVECTORS.length; ++i) {
            RealVector actual = decomposition.getEigenvector(i);
            double norm;
            if (FIXED_REF_EIGENVECTORS[i].dotProduct(actual) < 0) {
                norm = FIXED_REF_EIGENVECTORS[i].add(actual).getNorm();
            } else {
                norm = FIXED_REF_EIGENVECTORS[i].subtract(actual).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                if (i == 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-0] semantic mismatch: expected norm 0.0 actual=" + norm);
                } else if (i == 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-1] semantic mismatch: expected norm 0.0 actual=" + norm);
                } else if (i == 2) {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-2] semantic mismatch: expected norm 0.0 actual=" + norm);
                } else if (i == 3) {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-3] semantic mismatch: expected norm 0.0 actual=" + norm);
                } else if (i == 4) {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-4] semantic mismatch: expected norm 0.0 actual=" + norm);
                } else if (i == 5) {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-5] semantic mismatch: expected norm 0.0 actual=" + norm);
                } else {
                    throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvector-6] semantic mismatch: expected norm 0.0 actual=" + norm);
                }
            }
        }

        // Sibling-agreement check: getRealEigenvalues() and getRealEigenvalue(i) report the same decomposition state.
        // A patch that only suppresses the bad branch or corrupts shared work[]/pingPong state can leave these readers inconsistent.
        for (int i = 0; i < eigenValues.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            double arrayValue = eigenValues[i];
            double tol = 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(arrayValue)));
            if (Math.abs(scalar - arrayValue) > tol) {
                if (i == 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-0] semantic mismatch: getRealEigenvalue(0)=" + scalar + " getRealEigenvalues()[0]=" + arrayValue);
                } else if (i == 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-1] semantic mismatch: getRealEigenvalue(1)=" + scalar + " getRealEigenvalues()[1]=" + arrayValue);
                } else if (i == 2) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-2] semantic mismatch: getRealEigenvalue(2)=" + scalar + " getRealEigenvalues()[2]=" + arrayValue);
                } else if (i == 3) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-3] semantic mismatch: getRealEigenvalue(3)=" + scalar + " getRealEigenvalues()[3]=" + arrayValue);
                } else if (i == 4) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-4] semantic mismatch: getRealEigenvalue(4)=" + scalar + " getRealEigenvalues()[4]=" + arrayValue);
                } else if (i == 5) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-5] semantic mismatch: getRealEigenvalue(5)=" + scalar + " getRealEigenvalues()[5]=" + arrayValue);
                } else {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-6] semantic mismatch: getRealEigenvalue(6)=" + scalar + " getRealEigenvalues()[6]=" + arrayValue);
                }
            }
        }

        // Hidden-state/read-only check: get* readers are documented as queries; repeated reads should not mutate the reported eigenvalues.
        double[] before = decomposition.getRealEigenvalues().clone();
        decomposition.getV();
        decomposition.getVT();
        decomposition.getD();
        decomposition.getDeterminant();
        double[] after = decomposition.getRealEigenvalues();
        if (after == null || after.length != before.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-shape] semantic mismatch: beforeLength="
                + before.length + " after=" + (after == null ? "null" : after.length));
        }
        for (int i = 0; i < before.length; i++) {
            double tol = 1.0e-12 * Math.max(1.0, Math.max(Math.abs(before[i]), Math.abs(after[i])));
            if (Math.abs(before[i] - after[i]) > tol) {
                if (i == 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-0] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                } else if (i == 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-1] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                } else if (i == 2) {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-2] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                } else if (i == 3) {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-3] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                } else if (i == 4) {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-4] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                } else if (i == 5) {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-5] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                } else {
                    throw new FuzzerSecurityIssueLow("[oracle:reader-stability-6] semantic mismatch: eigenvalue changed after read-only calls before=" + before[i] + " after=" + after[i]);
                }
            }
        }
    }

    private static void runImaginaryEigenvalueNullRelation(FuzzedDataProvider data) {
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
            throw new FuzzerSecurityIssueLow("[oracle:imag-null] semantic mismatch: expected null imaginary eigenvalue array but got length " + imag.length);
        }
    }

    private static void runReversePreservesEigenvaluesRelation(FuzzedDataProvider data) {
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
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != n || ev2.length != n) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-shape] semantic mismatch: wrong eigenvalue array shape");
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
                if (i == 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-0] metamorphic violation: reversed tridiagonal should preserve eigenvalue multiset at index 0 lhs=" + a + " rhs=" + b);
                } else if (i == 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-1] metamorphic violation: reversed tridiagonal should preserve eigenvalue multiset at index 1 lhs=" + a + " rhs=" + b);
                } else if (i == 2) {
                    throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-2] metamorphic violation: reversed tridiagonal should preserve eigenvalue multiset at index 2 lhs=" + a + " rhs=" + b);
                } else if (i == 3) {
                    throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-3] metamorphic violation: reversed tridiagonal should preserve eigenvalue multiset at index 3 lhs=" + a + " rhs=" + b);
                } else if (i == 4) {
                    throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-4] metamorphic violation: reversed tridiagonal should preserve eigenvalue multiset at index 4 lhs=" + a + " rhs=" + b);
                } else {
                    throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-5] metamorphic violation: reversed tridiagonal should preserve eigenvalue multiset at index 5 lhs=" + a + " rhs=" + b);
                }
            }
        }
    }

    private static void runConstructedDiagonalOracle(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 6);
        double[] diag = new double[n];
        for (int i = 0; i < n; i++) {
            diag[i] = data.consumeInt(-100000, 100000) / 100.0;
        }
        double[] off = new double[Math.max(0, n - 1)];

        EigenDecompositionImpl dec;
        double[] actual;
        try {
            dec = new EigenDecompositionImpl(diag.clone(), off, MathUtils.SAFE_MIN);
            actual = dec.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (actual == null || actual.length != n) {
            throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-shape] metamorphic violation: expected length=" + n
                + " actual=" + (actual == null ? "null" : actual.length));
        }

        double[] expected = diag.clone();
        Arrays.sort(expected);
        reverse(expected);

        for (int i = 0; i < n; i++) {
            double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(expected[i]), Math.abs(actual[i])));
            if (Math.abs(expected[i] - actual[i]) > tol) {
                if (i == 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-0] metamorphic violation: diagonal tridiagonal matrix must expose its diagonal as eigenvalues input="
                        + Arrays.toString(diag) + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
                } else if (i == 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-1] metamorphic violation: diagonal tridiagonal matrix must expose its diagonal as eigenvalues input="
                        + Arrays.toString(diag) + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
                } else if (i == 2) {
                    throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-2] metamorphic violation: diagonal tridiagonal matrix must expose its diagonal as eigenvalues input="
                        + Arrays.toString(diag) + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
                } else if (i == 3) {
                    throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-3] metamorphic violation: diagonal tridiagonal matrix must expose its diagonal as eigenvalues input="
                        + Arrays.toString(diag) + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
                } else if (i == 4) {
                    throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-4] metamorphic violation: diagonal tridiagonal matrix must expose its diagonal as eigenvalues input="
                        + Arrays.toString(diag) + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
                } else {
                    throw new FuzzerSecurityIssueLow("[oracle:constructed-diagonal-5] metamorphic violation: diagonal tridiagonal matrix must expose its diagonal as eigenvalues input="
                        + Arrays.toString(diag) + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
                }
            }
        }

        // For a diagonal symmetric matrix, every reported eigenvector/eigenvalue pair must satisfy A*v = lambda*v.
        // This is a direct post-condition of a correct eigen decomposition and catches silent wrong-state patches.
        for (int i = 0; i < n; i++) {
            RealVector v;
            try {
                v = dec.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double lambda = actual[i];
            double residualNorm = diagonalResidualNorm(diag, v, lambda);
            double tol = 1.0e-7 * Math.max(1.0, Math.abs(lambda));
            if (residualNorm > tol) {
                if (i == 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:eigenpair-equation-0] metamorphic violation: Av=lambda*v residual=" + residualNorm + " lambda=" + lambda + " diag=" + Arrays.toString(diag));
                } else if (i == 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:eigenpair-equation-1] metamorphic violation: Av=lambda*v residual=" + residualNorm + " lambda=" + lambda + " diag=" + Arrays.toString(diag));
                } else if (i == 2) {
                    throw new FuzzerSecurityIssueLow("[oracle:eigenpair-equation-2] metamorphic violation: Av=lambda*v residual=" + residualNorm + " lambda=" + lambda + " diag=" + Arrays.toString(diag));
                } else if (i == 3) {
                    throw new FuzzerSecurityIssueLow("[oracle:eigenpair-equation-3] metamorphic violation: Av=lambda*v residual=" + residualNorm + " lambda=" + lambda + " diag=" + Arrays.toString(diag));
                } else if (i == 4) {
                    throw new FuzzerSecurityIssueLow("[oracle:eigenpair-equation-4] metamorphic violation: Av=lambda*v residual=" + residualNorm + " lambda=" + lambda + " diag=" + Arrays.toString(diag));
                } else {
                    throw new FuzzerSecurityIssueLow("[oracle:eigenpair-equation-5] metamorphic violation: Av=lambda*v residual=" + residualNorm + " lambda=" + lambda + " diag=" + Arrays.toString(diag));
                }
            }
        }
    }

    private static double diagonalResidualNorm(double[] diag, RealVector v, double lambda) {
        double sum = 0.0;
        for (int r = 0; r < diag.length; r++) {
            double diff = diag[r] * v.getEntry(r) - lambda * v.getEntry(r);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static void reverse(double[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            double t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    private static void assertCloseFixedEigenvalue(int i, double expected, double actual, double tol) {
        if (Math.abs(expected - actual) > tol) {
            if (i == 0) {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-0] semantic mismatch: getRealEigenvalues()[0] expected=" + expected + " actual=" + actual + " tol=" + tol);
            } else if (i == 1) {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-1] semantic mismatch: getRealEigenvalues()[1] expected=" + expected + " actual=" + actual + " tol=" + tol);
            } else if (i == 2) {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-2] semantic mismatch: getRealEigenvalues()[2] expected=" + expected + " actual=" + actual + " tol=" + tol);
            } else if (i == 3) {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-3] semantic mismatch: getRealEigenvalues()[3] expected=" + expected + " actual=" + actual + " tol=" + tol);
            } else if (i == 4) {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-4] semantic mismatch: getRealEigenvalues()[4] expected=" + expected + " actual=" + actual + " tol=" + tol);
            } else if (i == 5) {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-5] semantic mismatch: getRealEigenvalues()[5] expected=" + expected + " actual=" + actual + " tol=" + tol);
            } else {
                throw new FuzzerSecurityIssueLow("[oracle:fixed-eigenvalue-6] semantic mismatch: getRealEigenvalues()[6] expected=" + expected + " actual=" + actual + " tol=" + tol);
            }
        }
    }
}