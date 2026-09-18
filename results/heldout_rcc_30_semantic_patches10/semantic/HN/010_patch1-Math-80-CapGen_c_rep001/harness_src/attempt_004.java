package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] SEED_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SEED_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedSeedOracles();
        runFuzzedExplorationAndMetamorphicChecks(data);
    }

    private static void runLiftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(SEED_MAIN, SEED_SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            assertApprox(
                REF_EIGENVALUES[i],
                eigenValues[i],
                1.0e-3,
                "lifted-eigenvalue-" + i,
                "expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i
            );

            RealVector actualVector = decomposition.getEigenvector(i);
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double normDiff;
            if (dot < 0) {
                normDiff = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                normDiff = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            assertApprox(
                0.0,
                normDiff,
                1.0e-5,
                "lifted-eigenvector-" + i,
                "index=" + i + " dot=" + dot + " normDiff=" + normDiff
            );
        }

        /* Contract used: class javadoc says for symmetric matrices the returned D satisfies A = V D V^T and D is always diagonal;
           getRealEigenvalues() exposes the real eigenvalues, so D's diagonal must agree with them. A patch that merely avoids the bad
           branch or silently corrupts work[] can still return some decomposition object, but this observable agreement must still hold. */
        RealMatrix d = decomposition.getD();
        for (int r = 0; r < REF_EIGENVALUES.length; r++) {
            assertApprox(
                eigenValues[r],
                d.getEntry(r, r),
                1.0e-9,
                "seed-d-diagonal-" + r,
                "diag=" + d.getEntry(r, r) + " eigen=" + eigenValues[r] + " index=" + r
            );
            for (int c = 0; c < REF_EIGENVALUES.length; c++) {
                if (r != c) {
                    assertApprox(
                        0.0,
                        d.getEntry(r, c),
                        1.0e-12,
                        "seed-d-offdiag-" + r + "-" + c,
                        "entry=" + d.getEntry(r, c) + " row=" + r + " col=" + c
                    );
                }
            }
        }

        /* Contract used: getDeterminant() returns the determinant of the matrix, and for an eigen decomposition the determinant equals
           the product of all eigenvalues. This is a direct observable post-condition on the same decomposition result. */
        double expectedDet = 1.0;
        for (int i = 0; i < eigenValues.length; i++) {
            expectedDet *= eigenValues[i];
        }
        double actualDet = decomposition.getDeterminant();
        double detTol = Math.max(1.0e-6, Math.abs(expectedDet) * 1.0e-10);
        assertApprox(
            expectedDet,
            actualDet,
            detTol,
            "seed-determinant",
            "expected=" + expectedDet + " actual=" + actualDet + " tol=" + detTol
        );

        /* Reader stability check: getV/getVT/getD/getSolver are readers/cached accessors; invoking them must not change the already
           established eigenvalues. A throw-deleting or guard-skipping patch that leaves hidden state inconsistent can violate this. */
        double[] before = decomposition.getRealEigenvalues().clone();
        decomposition.getV();
        decomposition.getVT();
        decomposition.getD();
        decomposition.getSolver();
        double[] after = decomposition.getRealEigenvalues();
        if (before.length != after.length) {
            throw new RuntimeException("[oracle:seed-reader-stability] metamorphic violation: eigenvalue array length changed before=" + before.length + " after=" + after.length);
        }
        for (int i = 0; i < before.length; i++) {
            assertApprox(
                before[i],
                after[i],
                0.0,
                "seed-reader-stability-" + i,
                "before=" + before[i] + " after=" + after[i] + " index=" + i
            );
        }
    }

    private static void runFuzzedExplorationAndMetamorphicChecks(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 8);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];

        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 100.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 100.0;
        }

        EigenDecompositionImpl decomp;
        try {
            decomp = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        double[] evals;
        RealMatrix d;
        double det;
        double[] before;
        try {
            evals = decomp.getRealEigenvalues();
            d = decomp.getD();
            det = decomp.getDeterminant();
            before = evals.clone();
        } catch (Throwable t) {
            return;
        }

        /* Contract used: D is diagonal and represents the decomposition's eigenvalues; therefore its diagonal equals getRealEigenvalues()
           and off-diagonal entries are zero for every successful symmetric decomposition, not just the seed. */
        try {
            if (d.getRowDimension() != evals.length || d.getColumnDimension() != evals.length) {
                throw new RuntimeException("[oracle:fuzz-d-shape] metamorphic violation: D dimension disagrees with eigenvalue count rows=" + d.getRowDimension() + " cols=" + d.getColumnDimension() + " evals=" + evals.length);
            }
            for (int r = 0; r < evals.length; r++) {
                double diag = d.getEntry(r, r);
                double tol = Math.max(1.0e-9, Math.abs(evals[r]) * 1.0e-12);
                if (Math.abs(diag - evals[r]) > tol) {
                    throw new RuntimeException("[oracle:fuzz-d-diagonal] metamorphic violation: D diagonal disagrees with eigenvalue index=" + r + " diag=" + diag + " eigen=" + evals[r] + " tol=" + tol);
                }
                for (int c = 0; c < evals.length; c++) {
                    if (r != c) {
                        double off = d.getEntry(r, c);
                        if (Math.abs(off) > 1.0e-12) {
                            throw new RuntimeException("[oracle:fuzz-d-offdiag] metamorphic violation: D not diagonal row=" + r + " col=" + c + " value=" + off);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }

        /* Contract used: determinant equals product of eigenvalues for any successful decomposition. */
        try {
            double prod = 1.0;
            for (int i = 0; i < evals.length; i++) {
                prod *= evals[i];
            }
            if (Double.isFinite(prod) && Double.isFinite(det)) {
                double tol = Math.max(1.0e-6, Math.abs(prod) * 1.0e-8);
                if (Math.abs(prod - det) > tol) {
                    throw new RuntimeException("[oracle:fuzz-determinant] metamorphic violation: determinant disagrees with product of eigenvalues det=" + det + " prod=" + prod + " tol=" + tol);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }

        /* Reader stability check on fuzzed successful inputs: invoking cached readers should not mutate the established eigenvalues. */
        try {
            decomp.getV();
            decomp.getVT();
            decomp.getD();
            decomp.getSolver();
            double[] after = decomp.getRealEigenvalues();
            if (before.length != after.length) {
                throw new RuntimeException("[oracle:fuzz-reader-stability] metamorphic violation: eigenvalue array length changed before=" + before.length + " after=" + after.length);
            }
            for (int i = 0; i < before.length; i++) {
                if (Double.doubleToLongBits(before[i]) != Double.doubleToLongBits(after[i])) {
                    throw new RuntimeException("[oracle:fuzz-reader-stability] metamorphic violation: eigenvalue changed after reader calls index=" + i + " before=" + before[i] + " after=" + after[i]);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void assertApprox(double expected, double actual, double tol, String oracleId, String detail) {
        boolean ok;
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            ok = Double.isNaN(expected) && Double.isNaN(actual);
        } else if (tol == 0.0) {
            ok = Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual);
        } else {
            ok = Math.abs(expected - actual) <= tol;
        }
        if (!ok) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + detail
            );
        }
    }
}