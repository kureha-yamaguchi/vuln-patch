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

        EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(
                mainTridiagonal,
                secondaryTridiagonal,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            double actual = eigenValues[i];
            double expected = refEigenValues[i];
            if (!(Math.abs(expected - actual) <= 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001 index=" + i
                );
            }
            try {
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
                        "[oracle:lifted-eigenvectors] semantic mismatch: norm=" + norm + " tolerance=1.0E-5 index=" + i + " dot=" + dot
                    );
                }
            } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
                throw finding;
            } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                return;
            }
        }

        try {
            // Documented family consistency: getRealEigenvalues(), getRealEigenvalue(i), and getD()
            // all expose the decomposition result. If a patch merely suppresses the buggy path or
            // leaves corrupted internal work/pingPong state, these sibling readers can disagree.
            RealMatrix d = decomposition.getD();
            double[] all = decomposition.getRealEigenvalues();
            for (int i = 0; i < all.length; i++) {
                double one = decomposition.getRealEigenvalue(i);
                double diag = d.getEntry(i, i);
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(one)) {
                    throw new RuntimeException(
                        "[oracle:sibling-reader-consistency] metamorphic violation: getRealEigenvalues()[i] != getRealEigenvalue(i) index=" + i + " lhs=" + all[i] + " rhs=" + one
                    );
                }
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(diag)) {
                    throw new RuntimeException(
                        "[oracle:diagonal-consistency] metamorphic violation: getD diagonal entry != real eigenvalue index=" + i + " lhs=" + all[i] + " rhs=" + diag
                    );
                }
            }
        } catch (RuntimeException re) { /*__vpRepair*/ if (re instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) re;
            throw re;
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
            double v = data.consumeInt(-1000000, 1000000) / 16.0;
            if (v == 0.0) {
                v = data.consumeBoolean() ? 1.0 : -1.0;
            }
            fuzzSecondary[i] = v;
        }
        try {
            EigenDecomposition fuzzDecomposition = new EigenDecompositionImpl(
                fuzzMain,
                fuzzSecondary,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
            fuzzDecomposition.getRealEigenvalues();
            fuzzDecomposition.getD();
            fuzzDecomposition.getV();
            fuzzDecomposition.getVT();
            if (n > 0) {
                fuzzDecomposition.getRealEigenvalue(0);
                fuzzDecomposition.getEigenvector(0);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}