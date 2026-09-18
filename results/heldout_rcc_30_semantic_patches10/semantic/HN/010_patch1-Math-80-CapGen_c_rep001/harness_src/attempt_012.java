package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, org.apache.commons.math.util.MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            double expected = refEigenValues[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i + " expected=" + expected + " actual=" + actual);
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double normDiff;
            if (refEigenVectors[i].dotProduct(actualVector) < 0) {
                normDiff = refEigenVectors[i].add(actualVector).getNorm();
            } else {
                normDiff = refEigenVectors[i].subtract(actualVector).getNorm();
            }
            if (normDiff > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i + " normDiff=" + normDiff);
            }
        }

        // Contract check from the EigenDecomposition API family: getRealEigenvalues(),
        // getRealEigenvalue(i), getD(), and getEigenvector(i) describe the same decomposition state.
        // A patch that merely suppresses or bypasses the flip logic can leave shared state inconsistent,
        // so these sibling readers must still agree after construction.
        RealMatrix d = decomposition.getD();
        for (int i = 0; i < eigenValues.length; ++i) {
            double fromArray = eigenValues[i];
            double fromScalar = decomposition.getRealEigenvalue(i);
            double fromD = d.getEntry(i, i);
            if (Math.abs(fromArray - fromScalar) > 1.0e-12 || Math.abs(fromArray - fromD) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:sibling-agreement] metamorphic violation: decomposition readers disagree index=" + i +
                    " arrayValue=" + fromArray + " scalarValue=" + fromScalar + " dValue=" + fromD);
            }
        }

        // Contract check from the class documentation: A = V D V^T, so each reported eigenvector v_i
        // with eigenvalue lambda_i must satisfy A*v_i = lambda_i*v_i. This observable post-condition
        // catches silent wrong-state fixes that avoid exceptions but return incorrect decomposition data.
        for (int i = 0; i < eigenValues.length; ++i) {
            RealVector v = decomposition.getEigenvector(i);
            double lambda = decomposition.getRealEigenvalue(i);
            double residualSq = 0.0;
            for (int r = 0; r < mainTridiagonal.length; ++r) {
                double av = mainTridiagonal[r] * v.getEntry(r);
                if (r > 0) {
                    av += secondaryTridiagonal[r - 1] * v.getEntry(r - 1);
                }
                if (r + 1 < mainTridiagonal.length) {
                    av += secondaryTridiagonal[r] * v.getEntry(r + 1);
                }
                double diff = av - lambda * v.getEntry(r);
                residualSq += diff * diff;
            }
            double residual = Math.sqrt(residualSq);
            if (residual > 1.0e-4) {
                throw new RuntimeException(
                    "[oracle:eigen-equation] metamorphic violation: A*v=lambda*v index=" + i +
                    " lambda=" + lambda + " residual=" + residual);
            }
        }

        if (data.remainingBytes() <= 0) {
            return;
        }

        double scale;
        try {
            int num = data.consumeInt(1, 1000);
            int den = data.consumeInt(1, 1000);
            scale = num / (double) den;
        } catch (Throwable t) {
            return;
        }

        double[] scaledMain = new double[mainTridiagonal.length];
        double[] scaledSecondary = new double[secondaryTridiagonal.length];
        for (int i = 0; i < scaledMain.length; ++i) {
            scaledMain[i] = mainTridiagonal[i] * scale;
        }
        for (int i = 0; i < scaledSecondary.length; ++i) {
            scaledSecondary[i] = secondaryTridiagonal[i] * scale;
        }

        EigenDecomposition scaled;
        try {
            scaled = new EigenDecompositionImpl(scaledMain, scaledSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        try {
            double[] scaledEigenValues = scaled.getRealEigenvalues();
            for (int i = 0; i < refEigenValues.length; ++i) {
                double lhs = scaledEigenValues[i];
                double rhs = scale * eigenValues[i];
                if (Math.abs(lhs - rhs) > 1.0e-3 * Math.max(1.0, Math.abs(rhs))) {
                    throw new RuntimeException(
                        "[oracle:positive-scaling] metamorphic violation: eigenvalues must scale with a positive scalar" +
                        " scale=" + scale + " index=" + i + " lhs=" + lhs + " rhs=" + rhs);
                }

                RealVector baseV = decomposition.getEigenvector(i);
                RealVector scaledV = scaled.getEigenvector(i);
                double vectorAgreement;
                if (baseV.dotProduct(scaledV) < 0) {
                    vectorAgreement = baseV.add(scaledV).getNorm();
                } else {
                    vectorAgreement = baseV.subtract(scaledV).getNorm();
                }
                if (vectorAgreement > 1.0e-4) {
                    throw new RuntimeException(
                        "[oracle:positive-scaling-vectors] metamorphic violation: eigenvectors should be invariant up to sign under positive scaling" +
                        " scale=" + scale + " index=" + i + " normDiff=" + vectorAgreement);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }
}