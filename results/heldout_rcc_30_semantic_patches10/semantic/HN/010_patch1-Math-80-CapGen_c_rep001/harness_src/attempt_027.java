package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

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

        EigenDecompositionImpl decomposition;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            double[] eigenValues = decomposition.getRealEigenvalues();
            for (int i = 0; i < refEigenValues.length; ++i) {
                double actual = eigenValues[i];
                double expected = refEigenValues[i];
                if (!(Math.abs(expected - actual) <= 1.0e-3)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i + " expected=" + expected + " actual=" + actual
                    );
                }

                RealVector actualVector = decomposition.getEigenvector(i);
                double dot = refEigenVectors[i].dotProduct(actualVector);
                double norm;
                if (dot < 0) {
                    norm = refEigenVectors[i].add(actualVector).getNorm();
                } else {
                    norm = refEigenVectors[i].subtract(actualVector).getNorm();
                }
                if (!(Math.abs(norm) <= 1.0e-5)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i + " norm=" + norm + " dot=" + dot
                    );
                }
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            // Contract from EigenDecomposition/EigenDecompositionImpl docs:
            // for the decomposed symmetric matrix A, V and D satisfy A = V * D * V^T.
            // A patch that merely skips or corrupts flipIfWarranted effects can still return
            // outputs that no longer reconstruct the input matrix, so this observable post-condition
            // catches silent wrong results even when no exception is thrown.
            double[][] tri = new double[mainTridiagonal.length][mainTridiagonal.length];
            for (int i = 0; i < mainTridiagonal.length; i++) {
                tri[i][i] = mainTridiagonal[i];
                if (i + 1 < mainTridiagonal.length) {
                    tri[i][i + 1] = secondaryTridiagonal[i];
                    tri[i + 1][i] = secondaryTridiagonal[i];
                }
            }
            RealMatrix original = MatrixUtils.createRealMatrix(tri);
            RealMatrix rebuilt = decomposition.getV().multiply(decomposition.getD()).multiply(decomposition.getVT());
            for (int r = 0; r < tri.length; r++) {
                for (int c = 0; c < tri.length; c++) {
                    double lhs = original.getEntry(r, c);
                    double rhs = rebuilt.getEntry(r, c);
                    double tol = 1.0e-6 * Math.max(1.0, Math.abs(lhs));
                    if (Math.abs(lhs - rhs) > tol) {
                        throw new RuntimeException(
                            "[oracle:reconstruct-fixed] metamorphic violation: A=V*D*V^T r=" + r + " c=" + c +
                            " lhs=" + lhs + " rhs=" + rhs
                        );
                    }
                }
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int n = data.consumeInt(2, 8);
        double[] fuzzMain = new double[n];
        double[] fuzzSecondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            fuzzMain[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        for (int i = 0; i < n - 1; i++) {
            fuzzSecondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        try {
            EigenDecompositionImpl triDecomp = new EigenDecompositionImpl(fuzzMain, fuzzSecondary, MathUtils.SAFE_MIN);

            double[][] tri = new double[n][n];
            for (int i = 0; i < n; i++) {
                tri[i][i] = fuzzMain[i];
                if (i + 1 < n) {
                    tri[i][i + 1] = fuzzSecondary[i];
                    tri[i + 1][i] = fuzzSecondary[i];
                }
            }
            RealMatrix full = MatrixUtils.createRealMatrix(tri);
            EigenDecompositionImpl matrixDecomp = new EigenDecompositionImpl(full, MathUtils.SAFE_MIN);

            double[] evTri = triDecomp.getRealEigenvalues();
            double[] evMatrix = matrixDecomp.getRealEigenvalues();

            if (evTri.length != evMatrix.length) {
                throw new RuntimeException(
                    "[oracle:ctor-equivalence-length] metamorphic violation: tridiagonal ctor vs matrix ctor length lhs=" +
                    evTri.length + " rhs=" + evMatrix.length
                );
            }

            for (int i = 0; i < evTri.length; i++) {
                double lhs = evTri[i];
                double rhs = evMatrix[i];
                double tol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(lhs), Math.abs(rhs)));
                if (Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                        "[oracle:ctor-equivalence-eigenvalue] metamorphic violation: tridiagonal ctor vs matrix ctor index=" +
                        i + " lhs=" + lhs + " rhs=" + rhs
                    );
                }
            }

            double detTri = triDecomp.getDeterminant();
            double detMatrix = matrixDecomp.getDeterminant();
            double detTol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(detTri), Math.abs(detMatrix)));
            if (!(Double.isNaN(detTri) && Double.isNaN(detMatrix)) && Math.abs(detTri - detMatrix) > detTol) {
                throw new RuntimeException(
                    "[oracle:ctor-equivalence-determinant] metamorphic violation: tridiagonal ctor vs matrix ctor determinant lhs=" +
                    detTri + " rhs=" + detMatrix
                );
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}