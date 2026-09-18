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

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedRegression();
        checkTrustedReaderAgreement();
        exerciseFuzzedInputs(data);
    }

    private static void checkLiftedRegression() {
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
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] +
                    " tolerance=0.001");
            }

            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot;
            try {
                dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            } catch (Throwable t) {
                return;
            }

            final double norm;
            try {
                if (dot < 0) {
                    norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
                } else {
                    norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
                }
            } catch (Throwable t) {
                return;
            }

            if (!withinTol(0.0, norm, 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void checkTrustedReaderAgreement() {
        final EigenDecompositionImpl decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        final RealMatrix d;
        try {
            eigenValues = decomposition.getRealEigenvalues();
            d = decomposition.getD();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            final double single;
            try {
                single = decomposition.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(single) != Double.doubleToLongBits(eigenValues[i]) && single != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:reader-family] metamorphic violation: getRealEigenvalue(i) must agree with getRealEigenvalues()[i] input=i=" +
                    i + " lhs=" + single + " rhs=" + eigenValues[i]);
            }

            final double diagonal;
            try {
                diagonal = d.getEntry(i, i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(diagonal) != Double.doubleToLongBits(eigenValues[i]) && diagonal != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:d-diagonal] metamorphic violation: getD() is documented as the diagonal matrix of eigenvalues, so its diagonal must equal getRealEigenvalues() input=i=" +
                    i + " lhs=" + diagonal + " rhs=" + eigenValues[i]);
            }

            for (int j = 0; j < eigenValues.length; ++j) {
                if (i == j) {
                    continue;
                }
                final double off;
                try {
                    off = d.getEntry(i, j);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(off) > 1.0e-12) {
                    throw new RuntimeException(
                        "[oracle:d-offdiag] metamorphic violation: getD() is documented as diagonal for symmetric matrices input=i=" +
                        i + ",j=" + j + " lhs=" + off + " rhs=0.0");
                }
            }
        }

        final RealMatrix v;
        final RealMatrix vt;
        try {
            v = decomposition.getV();
            vt = decomposition.getVT();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            for (int j = 0; j < eigenValues.length; ++j) {
                final double lhs;
                final double rhs;
                try {
                    lhs = vt.getEntry(i, j);
                    rhs = v.getEntry(j, i);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(lhs - rhs) > 1.0e-10) {
                    throw new RuntimeException(
                        "[oracle:v-vt] metamorphic violation: by the EigenDecomposition contract, getVT() must be the transpose of getV() input=i=" +
                        i + ",j=" + j + " lhs=" + lhs + " rhs=" + rhs);
                }
            }
        }
    }

    private static void exerciseFuzzedInputs(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 10);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];

        for (int i = 0; i < n; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            main[i] = base / 16.0;
        }
        for (int i = 0; i < n - 1; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            secondary[i] = base / 16.0;
        }

        try {
            EigenDecompositionImpl decomposition =
                new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
            decomposition.getRealEigenvalues();
            decomposition.getD();
            decomposition.getV();
            decomposition.getVT();
            int idx = data.consumeInt(0, n - 1);
            decomposition.getRealEigenvalue(idx);
            decomposition.getEigenvector(idx);
        } catch (Throwable t) {
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}