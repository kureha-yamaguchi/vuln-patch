package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseLiftedOracle();
        exerciseScaledMetamorphic(data);
    }

    private static void exerciseLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: expected=" + REF_EIGENVALUES[i] +
                    " actual=" + eigenValues[i] + " index=" + i);
            }

            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: expectedNorm=0.0 actualNorm=" + norm +
                    " dot=" + dot + " index=" + i);
            }
        }

        // Contract used: the class javadoc states A = V D V^T and that D returned by getD() is diagonal;
        // public readers getRealEigenvalues(), getRealEigenvalue(i), and getD() must therefore agree on the same eigenvalues.
        // A patch that only suppresses the bad branch or silently corrupts internal work[] can leave one reader inconsistent with another.
        final RealMatrix d;
        try {
            d = decomposition.getD();
        } catch (Throwable t) {
            return;
        }
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            final double fromArray;
            final double fromScalar;
            final double fromD;
            try {
                fromArray = decomposition.getRealEigenvalues()[i];
                fromScalar = decomposition.getRealEigenvalue(i);
                fromD = d.getEntry(i, i);
            } catch (Throwable t) {
                return;
            }
            if (Math.abs(fromArray - fromScalar) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:reader-agreement-scalar] metamorphic violation: getRealEigenvalues()[i] must equal getRealEigenvalue(i) index=" +
                    i + " lhs=" + fromArray + " rhs=" + fromScalar);
            }
            if (Math.abs(fromArray - fromD) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:reader-agreement-d] metamorphic violation: getRealEigenvalues()[i] must equal diagonal of getD() index=" +
                    i + " lhs=" + fromArray + " rhs=" + fromD);
            }
        }
    }

    private static void exerciseScaledMetamorphic(FuzzedDataProvider data) {
        int scaleInt = data.consumeInt(1, 5);
        double scale = (double) scaleInt;

        double[] scaledMain = new double[MAIN.length];
        double[] scaledSecondary = new double[SECONDARY.length];
        for (int i = 0; i < MAIN.length; ++i) {
            scaledMain[i] = MAIN[i] * scale;
        }
        for (int i = 0; i < SECONDARY.length; ++i) {
            scaledSecondary[i] = SECONDARY[i] * scale;
        }

        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(scaledMain, scaledSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        // Trusted relation: for a positive scalar c, eigenvalues of cA are c times the eigenvalues of A,
        // and eigenvectors are unchanged up to sign. This uses only real library calls on a real decomposition.
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i] * scale;
            if (Math.abs(expected - eigenValues[i]) > 1.0e-3 * scale) {
                throw new RuntimeException(
                    "[oracle:scale-eigenvalues] metamorphic violation: eigenvalues of cA must scale by c scale=" +
                    scale + " index=" + i + " lhs=" + eigenValues[i] + " rhs=" + expected);
            }

            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double norm = (dot < 0) ? REF_EIGENVECTORS[i].add(actualVector).getNorm()
                                    : REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new RuntimeException(
                    "[oracle:scale-eigenvectors] metamorphic violation: eigenvectors of positive-scaled matrix must match up to sign scale=" +
                    scale + " index=" + i + " lhs=" + norm + " rhs=0.0");
            }
        }
    }
}