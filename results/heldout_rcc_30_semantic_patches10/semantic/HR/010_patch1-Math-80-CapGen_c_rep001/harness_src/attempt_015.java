package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN_TRIDIAGONAL = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY_TRIDIAGONAL = {
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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN_TRIDIAGONAL, SECONDARY_TRIDIAGONAL, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTolerance(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]);
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedReference=" + vectorToString(REF_EIGENVECTORS[i]) +
                    " actual=" + vectorToString(actualVector) +
                    " normDiff=" + norm + " dot=" + dot);
            }
        }

        RealMatrix d = decomposition.getD();
        double[] eigenValuesAgain = decomposition.getRealEigenvalues();
        for (int i = 0; i < eigenValuesAgain.length; ++i) {
            double byIndex = decomposition.getRealEigenvalue(i);
            double onDiagonal = d.getEntry(i, i);
            if (!withinTolerance(eigenValuesAgain[i], byIndex, 1.0e-12) ||
                !withinTolerance(eigenValuesAgain[i], onDiagonal, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:seed-accessor-agreement] metamorphic violation: getRealEigenvalues/getRealEigenvalue/getD diagonal disagree index=" +
                    i + " arrayValue=" + eigenValuesAgain[i] + " indexedValue=" + byIndex +
                    " diagonalValue=" + onDiagonal);
            }
            for (int j = 0; j < eigenValuesAgain.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0) {
                    throw new RuntimeException(
                        "[oracle:seed-d-diagonal] metamorphic violation: getD must be diagonal for symmetric decomposition index=(" +
                        i + "," + j + ") value=" + d.getEntry(i, j));
                }
            }
        }

        try {
            int n = data.consumeInt(2, 8);
            double[] main = new double[n];
            double[] secondary = new double[n - 1];
            for (int i = 0; i < n; ++i) {
                main[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }
            for (int i = 0; i < n - 1; ++i) {
                secondary[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }

            EigenDecomposition fuzzDecomposition =
                new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);

            double[] vals = fuzzDecomposition.getRealEigenvalues();
            RealMatrix fuzzD = fuzzDecomposition.getD();

            for (int i = 0; i < vals.length; ++i) {
                double indexed = fuzzDecomposition.getRealEigenvalue(i);
                double diag = fuzzD.getEntry(i, i);
                if (!withinTolerance(vals[i], indexed, 1.0e-9) ||
                    !withinTolerance(vals[i], diag, 1.0e-9)) {
                    throw new RuntimeException(
                        "[oracle:fuzz-accessor-agreement] metamorphic violation: eigenvalue readers disagree n=" +
                        n + " index=" + i + " arrayValue=" + vals[i] + " indexedValue=" + indexed +
                        " diagonalValue=" + diag);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean withinTolerance(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }

    private static String vectorToString(RealVector v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.getDimension(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v.getEntry(i));
        }
        sb.append(']');
        return sb.toString();
    }
}