package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!close(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]
                );
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double norm;
            if (REF_EIGENVECTORS[i].dotProduct(actualVector) < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!close(0.0, norm, 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + norm
                );
            }
        }

        // Documented contract: A = V D V^T, D is diagonal, and scalar/array readers
        // expose the same decomposition state. A patch that only suppresses the bad
        // path but leaves corrupted work/pingPong state can make these public readers disagree.
        checkReadersAgree((EigenDecompositionImpl) decomposition, "seed");

        int scaleInt = data.consumeInt(1, 8);
        double scale = scaleInt;
        try {
            double[] scaledMain = new double[MAIN.length];
            double[] scaledSecondary = new double[SECONDARY.length];
            for (int i = 0; i < MAIN.length; i++) {
                scaledMain[i] = MAIN[i] * scale;
            }
            for (int i = 0; i < SECONDARY.length; i++) {
                scaledSecondary[i] = SECONDARY[i] * scale;
            }

            EigenDecompositionImpl scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);

            double[] scaledEigenValues = scaled.getRealEigenvalues();
            for (int i = 0; i < REF_EIGENVALUES.length; i++) {
                double expected = REF_EIGENVALUES[i] * scale;
                if (!close(expected, scaledEigenValues[i], Math.max(1.0e-3 * scale, 1.0e-6))) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigenvalues] metamorphic violation: positive scaling should scale eigenvalues index=" +
                        i + " inputScale=" + scale + " expected=" + expected + " actual=" + scaledEigenValues[i]
                    );
                }

                RealVector seedVec = decomposition.getEigenvector(i);
                RealVector scaledVec = scaled.getEigenvector(i);
                double alignedNorm;
                if (seedVec.dotProduct(scaledVec) < 0) {
                    alignedNorm = seedVec.add(scaledVec).getNorm();
                } else {
                    alignedNorm = seedVec.subtract(scaledVec).getNorm();
                }
                if (!close(0.0, alignedNorm, 1.0e-5)) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigenvectors] metamorphic violation: positive scaling should preserve eigenvectors up to sign index=" +
                        i + " inputScale=" + scale + " norm=" + alignedNorm
                    );
                }
            }

            checkReadersAgree(scaled, "scaled-" + scaleInt);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkReadersAgree(EigenDecompositionImpl decomposition, String tag) {
        double[] eigenValues = decomposition.getRealEigenvalues();
        RealMatrix d = decomposition.getD();
        RealMatrix v = decomposition.getV();
        RealMatrix vt = decomposition.getVT();

        for (int i = 0; i < eigenValues.length; i++) {
            double byArray = eigenValues[i];
            double byScalar = decomposition.getRealEigenvalue(i);
            double byD = d.getEntry(i, i);

            if (!close(byArray, byScalar, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-array-vs-scalar] metamorphic violation: getRealEigenvalues and getRealEigenvalue must agree tag=" +
                    tag + " index=" + i + " lhs=" + byArray + " rhs=" + byScalar
                );
            }
            if (!close(byArray, byD, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-array-vs-d] metamorphic violation: getD diagonal and eigenvalue readers must agree tag=" +
                    tag + " index=" + i + " lhs=" + byArray + " rhs=" + byD
                );
            }

            for (int j = 0; j < eigenValues.length; j++) {
                if (i != j) {
                    double offDiag = d.getEntry(i, j);
                    if (!close(0.0, offDiag, 0.0)) {
                        throw new RuntimeException(
                            "[oracle:d-must-be-diagonal] metamorphic violation: D must be diagonal tag=" +
                            tag + " row=" + i + " col=" + j + " value=" + offDiag
                        );
                    }
                }

                double vtEntry = vt.getEntry(i, j);
                double transposed = v.getEntry(j, i);
                if (!close(vtEntry, transposed, 1.0e-12)) {
                    throw new RuntimeException(
                        "[oracle:v-vt-transpose] metamorphic violation: getVT must equal transpose(getV) tag=" +
                        tag + " row=" + i + " col=" + j + " lhs=" + vtEntry + " rhs=" + transposed
                    );
                }
            }
        }
    }

    private static boolean close(double expected, double actual, double tol) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return false;
        }
        return Math.abs(expected - actual) <= tol;
    }
}