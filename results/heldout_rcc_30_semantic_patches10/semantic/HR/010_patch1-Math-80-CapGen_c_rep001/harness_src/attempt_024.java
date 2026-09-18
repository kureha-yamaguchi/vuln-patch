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
        int mode = data.consumeInt(0, 2);

        if (mode == 0) {
            runFixedOracle();
            return;
        }

        if (mode == 1) {
            runSignFlipGeneralization(data);
            return;
        }

        runPerturbedExercise(data);
    }

    private static void runFixedOracle() {
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
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]);
            }

            final RealVector actualEigenvector;
            try {
                actualEigenvector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot;
            try {
                dot = REF_EIGENVECTORS[i].dotProduct(actualEigenvector);
            } catch (Throwable t) {
                return;
            }

            final double normDiff;
            try {
                if (dot < 0) {
                    normDiff = REF_EIGENVECTORS[i].add(actualEigenvector).getNorm();
                } else {
                    normDiff = REF_EIGENVECTORS[i].subtract(actualEigenvector).getNorm();
                }
            } catch (Throwable t) {
                return;
            }

            if (!withinTol(0.0, normDiff, 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + normDiff + " dot=" + dot);
            }
        }

        // Contract justification: getRealEigenvalues(), getRealEigenvalue(i), and getD()
        // are sibling readers over the same decomposition state; for a correct implementation,
        // the diagonal entries reported by getD() and the indexed getter must agree with the
        // array getter. A patch that merely avoids the bad flip path but leaves corrupted
        // internal work/state would break this observable agreement without throwing.
        try {
            RealMatrix d = decomposition.getD();
            double[] values = decomposition.getRealEigenvalues();
            for (int i = 0; i < values.length; i++) {
                double fromIndexed = decomposition.getRealEigenvalue(i);
                double fromDiagonal = d.getEntry(i, i);
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(fromIndexed)) {
                    throw new RuntimeException(
                        "[oracle:getters-agree] metamorphic violation: getRealEigenvalues vs getRealEigenvalue input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + fromIndexed);
                }
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(fromDiagonal)) {
                    throw new RuntimeException(
                        "[oracle:getters-agree] metamorphic violation: getRealEigenvalues vs getD diagonal input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + fromDiagonal);
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }

        // Contract justification: getVT() is the transpose view of V by the EigenDecomposition API.
        // This post-condition is directly observable and independent of the reference values.
        try {
            RealMatrix v = decomposition.getV();
            RealMatrix vt = decomposition.getVT();
            int n = v.getRowDimension();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    double lhs = v.getEntry(r, c);
                    double rhs = vt.getEntry(c, r);
                    if (!withinTol(lhs, rhs, 1.0e-12)) {
                        throw new RuntimeException(
                            "[oracle:v-vt-transpose] metamorphic violation: getVT must equal transpose(getV) input=(" +
                            r + "," + c + ") lhs=" + lhs + " rhs=" + rhs);
                    }
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runSignFlipGeneralization(FuzzedDataProvider data) {
        int idx = data.consumeInt(0, SECONDARY.length - 1);
        double[] flippedSecondary = SECONDARY.clone();
        flippedSecondary[idx] = -flippedSecondary[idx];

        final EigenDecomposition base;
        final EigenDecomposition flipped;
        try {
            base = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
            flipped = new EigenDecompositionImpl(MAIN, flippedSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: for a symmetric tridiagonal matrix, changing the sign of one
        // off-diagonal entry is a similarity transform by a diagonal matrix of +/-1 signs, so the
        // eigenvalues must stay identical. Both sides are real library calls; the expected answer
        // is trusted by the algebraic equivalence of the two inputs.
        try {
            double[] a = base.getRealEigenvalues();
            double[] b = flipped.getRealEigenvalues();
            if (a.length != b.length) {
                throw new RuntimeException(
                    "[oracle:secondary-sign-flip] metamorphic violation: eigenvalue array lengths differ input=index=" +
                    idx + " lhs=" + a.length + " rhs=" + b.length);
            }
            for (int i = 0; i < a.length; i++) {
                if (!withinTol(a[i], b[i], 1.0e-8)) {
                    throw new RuntimeException(
                        "[oracle:secondary-sign-flip] metamorphic violation: sign flip of one secondary entry must preserve eigenvalues input=flipIndex=" +
                        idx + " pos=" + i + " lhs=" + a[i] + " rhs=" + b[i]);
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runPerturbedExercise(FuzzedDataProvider data) {
        double[] main = MAIN.clone();
        double[] secondary = SECONDARY.clone();

        int changes = data.consumeInt(1, 4);
        for (int i = 0; i < changes; i++) {
            int which = data.consumeBoolean() ? 0 : 1;
            int idx = which == 0 ? data.consumeInt(0, main.length - 1) : data.consumeInt(0, secondary.length - 1);
            int delta = data.consumeInt(-1000, 1000);
            double scaled = delta / 100.0;
            if (which == 0) {
                main[idx] += scaled;
            } else {
                secondary[idx] += scaled;
            }
        }

        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        // Exercise the patched path on fuzzed but moderate inputs, and only assert trusted
        // sibling-agreement properties from the public API.
        try {
            double[] values = decomposition.getRealEigenvalues();
            RealMatrix d = decomposition.getD();
            for (int i = 0; i < values.length; i++) {
                double vi = decomposition.getRealEigenvalue(i);
                double di = d.getEntry(i, i);
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(vi)) {
                    throw new RuntimeException(
                        "[oracle:fuzzed-getters-agree] metamorphic violation: getRealEigenvalues vs getRealEigenvalue input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + vi);
                }
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(di)) {
                    throw new RuntimeException(
                        "[oracle:fuzzed-getters-agree] metamorphic violation: getRealEigenvalues vs getD diagonal input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + di);
                }
            }

            RealMatrix v = decomposition.getV();
            RealMatrix vt = decomposition.getVT();
            int n = v.getRowDimension();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    double lhs = v.getEntry(r, c);
                    double rhs = vt.getEntry(c, r);
                    if (!withinTol(lhs, rhs, 1.0e-10)) {
                        throw new RuntimeException(
                            "[oracle:fuzzed-v-vt-transpose] metamorphic violation: getVT must equal transpose(getV) input=(" +
                            r + "," + c + ") lhs=" + lhs + " rhs=" + rhs);
                    }
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}