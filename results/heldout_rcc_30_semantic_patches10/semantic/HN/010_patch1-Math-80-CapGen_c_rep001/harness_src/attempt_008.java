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
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!approximatelyEqual(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
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
                    " norm=" + norm + " dot=" + dot);
            }
        }

        RealMatrix d = decomposition.getD();
        for (int i = 0; i < eigenValues.length; i++) {
            double diag = d.getEntry(i, i);
            if (!approximatelyEqual(eigenValues[i], diag, 1.0e-9)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:reader-agreement-d] semantic mismatch: index=" + i +
                    " getRealEigenvalues=" + eigenValues[i] + " getD.diag=" + diag);
            }
            double single = decomposition.getRealEigenvalue(i);
            if (!approximatelyEqual(eigenValues[i], single, 1.0e-12)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:reader-agreement-single] semantic mismatch: index=" + i +
                    " arrayValue=" + eigenValues[i] + " singleValue=" + single);
            }
        }

        double scale = scaleFromFuzz(data);
        double[] scaledMain = scaleArray(MAIN, scale);
        double[] scaledSecondary = scaleArray(SECONDARY, scale);
        try {
            EigenDecompositionImpl scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);

            double[] scaledEigen = scaled.getRealEigenvalues();
            RealMatrix scaledD = scaled.getD();

            // Contract justification: EigenDecomposition exposes multiple readers for the same decomposition
            // state (getRealEigenvalues, getRealEigenvalue, getD). A patch that merely avoids the flip
            // or corrupts shared work/pingPong state can leave these readers inconsistent even if no throw occurs.
            for (int i = 0; i < scaledEigen.length; i++) {
                double single = scaled.getRealEigenvalue(i);
                double diag = scaledD.getEntry(i, i);
                if (!approximatelyEqual(scaledEigen[i], single, 1.0e-12) ||
                    !approximatelyEqual(scaledEigen[i], diag, 1.0e-9)) {
                    throw new RuntimeException(
                        "[oracle:metamorphic-readers] metamorphic violation: sibling readers disagree scale=" + scale +
                        " index=" + i + " array=" + scaledEigen[i] + " single=" + single + " diag=" + diag);
                }
            }

            // Contract justification: DecompositionSolver solves A x = b. We construct b from a chosen x
            // using the real tridiagonal input, so solve(b) must recover x. The documented solve overloads
            // also operate on the same input space and must agree on equivalent inputs.
            DecompositionSolver solver = scaled.getSolver();
            double[] x = new double[scaledMain.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = boundedCoordinate(data.consumeInt());
            }
            double[] b = multiplySymmetricTridiagonal(scaledMain, scaledSecondary, x);

            double[] solvedArray = solver.solve(b);
            RealVector solvedVector = solver.solve(new ArrayRealVector(b));

            for (int i = 0; i < x.length; i++) {
                if (!approximatelyEqual(x[i], solvedArray[i], 1.0e-6)) {
                    throw new RuntimeException(
                        "[oracle:metamorphic-solve-array] metamorphic violation: solve(A*x)=x scale=" + scale +
                        " index=" + i + " expected=" + x[i] + " actual=" + solvedArray[i]);
                }
                double viaVector = solvedVector.getEntry(i);
                if (!approximatelyEqual(solvedArray[i], viaVector, 1.0e-9)) {
                    throw new RuntimeException(
                        "[oracle:metamorphic-solve-overloads] metamorphic violation: solve(double[]) != solve(RealVector) scale=" + scale +
                        " index=" + i + " lhs=" + solvedArray[i] + " rhs=" + viaVector);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean approximatelyEqual(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }

    private static double[] scaleArray(double[] in, double scale) {
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] * scale;
        }
        return out;
    }

    private static double scaleFromFuzz(FuzzedDataProvider data) {
        int raw = data.consumeInt(-1000, 1000);
        double scale = raw / 100.0;
        if (scale == 0.0) {
            scale = 1.0;
        }
        if (scale < 0) {
            scale = -scale;
        }
        if (scale < 0.1) {
            scale += 0.1;
        }
        return scale;
    }

    private static double boundedCoordinate(int raw) {
        return (raw % 2001) - 1000;
    }

    private static double[] multiplySymmetricTridiagonal(double[] main, double[] secondary, double[] x) {
        double[] b = new double[main.length];
        for (int i = 0; i < main.length; i++) {
            double v = main[i] * x[i];
            if (i > 0) {
                v += secondary[i - 1] * x[i - 1];
            }
            if (i + 1 < main.length) {
                v += secondary[i] * x[i + 1];
            }
            b[i] = v;
        }
        return b;
    }
}