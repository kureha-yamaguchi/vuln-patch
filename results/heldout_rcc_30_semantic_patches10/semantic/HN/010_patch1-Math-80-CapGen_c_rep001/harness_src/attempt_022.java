package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

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

    private static final double[] SEED_REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] SEED_REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        try {
            EigenDecomposition decomposition =
                new EigenDecompositionImpl(SEED_MAIN, SEED_SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);

            double[] eigenValues = decomposition.getRealEigenvalues();
            for (int i = 0; i < SEED_REF_EIGENVALUES.length; ++i) {
                if (!within(eigenValues[i], SEED_REF_EIGENVALUES[i], 1.0e-3)) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:seed-eigenvalues] semantic mismatch: index=" + i +
                        " expected=" + SEED_REF_EIGENVALUES[i] + " actual=" + eigenValues[i]
                    );
                }

                RealVector actualVector = decomposition.getEigenvector(i);
                double dot = SEED_REF_EIGENVECTORS[i].dotProduct(actualVector);
                double norm;
                if (dot < 0) {
                    norm = SEED_REF_EIGENVECTORS[i].add(actualVector).getNorm();
                } else {
                    norm = SEED_REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
                }
                if (!within(norm, 0.0, 1.0e-5)) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:seed-eigenvectors] semantic mismatch: index=" + i +
                        " expectedNorm=0.0 actualNorm=" + norm
                    );
                }
            }

            // Documented sibling-reader agreement: getRealEigenvalues() and getRealEigenvalue(i)
            // expose the same eigenvalues of the same decomposition, so they must agree.
            for (int i = 0; i < eigenValues.length; i++) {
                double single = decomposition.getRealEigenvalue(i);
                if (Double.doubleToLongBits(single) != Double.doubleToLongBits(eigenValues[i])) {
                    throw new RuntimeException(
                        "[oracle:getters-agree-seed] metamorphic violation: getRealEigenvalue(i) must equal getRealEigenvalues()[i] i=" +
                        i + " arrayValue=" + eigenValues[i] + " singleValue=" + single
                    );
                }
            }

            // Class contract says A = V D V^T for the decomposition. A patch that merely avoids
            // the bad branch but leaves wrong work-array state can still return inconsistent V/D/VT.
            // This post-condition reads those public results back and checks they reconstruct the
            // original symmetric tridiagonal matrix.
            checkReconstruction(decomposition, SEED_MAIN, SEED_SECONDARY, 1.0e-4, "reconstruct-seed");
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        int n = data.consumeInt(2, 8);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];

        for (int i = 0; i < n; i++) {
            int base = data.consumeInt(-1000, 1000);
            main[i] = 2000.0 + (base / 10.0);
        }
        for (int i = 0; i < n - 1; i++) {
            int off = data.consumeInt(-50, 50);
            secondary[i] = off / 10.0;
        }

        try {
            EigenDecompositionImpl fuzzed =
                new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);

            // Same documented sibling-reader agreement on a fuzz-constructed input.
            double[] vals = fuzzed.getRealEigenvalues();
            for (int i = 0; i < vals.length; i++) {
                double single = fuzzed.getRealEigenvalue(i);
                if (Double.doubleToLongBits(single) != Double.doubleToLongBits(vals[i])) {
                    throw new RuntimeException(
                        "[oracle:getters-agree-fuzz] metamorphic violation: getRealEigenvalue(i) must equal getRealEigenvalues()[i] i=" +
                        i + " arrayValue=" + vals[i] + " singleValue=" + single
                    );
                }
            }

            // Same A = V D V^T contract on another real call path.
            checkReconstruction(fuzzed, main, secondary, 1.0e-4, "reconstruct-fuzz");
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean within(double actual, double expected, double tol) {
        return Math.abs(actual - expected) <= tol;
    }

    private static void checkReconstruction(EigenDecomposition decomposition,
                                            double[] main,
                                            double[] secondary,
                                            double tol,
                                            String oracleId) {
        RealMatrix v;
        RealMatrix d;
        RealMatrix vt;
        try {
            v = decomposition.getV();
            d = decomposition.getD();
            vt = decomposition.getVT();
        } catch (Throwable t) {
            return;
        }

        int n = main.length;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                double reconstructed = 0.0;
                for (int k = 0; k < n; k++) {
                    reconstructed += v.getEntry(r, k) * d.getEntry(k, k) * vt.getEntry(k, c);
                }

                double expected = 0.0;
                if (r == c) {
                    expected = main[r];
                } else if (r + 1 == c) {
                    expected = secondary[r];
                } else if (c + 1 == r) {
                    expected = secondary[c];
                }

                if (Math.abs(reconstructed - expected) > tol) {
                    throw new RuntimeException(
                        "[oracle:" + oracleId + "] metamorphic violation: A must equal V*D*V^T row=" +
                        r + " col=" + c + " expected=" + expected + " actual=" + reconstructed +
                        " tol=" + tol
                    );
                }
            }
        }
    }
}