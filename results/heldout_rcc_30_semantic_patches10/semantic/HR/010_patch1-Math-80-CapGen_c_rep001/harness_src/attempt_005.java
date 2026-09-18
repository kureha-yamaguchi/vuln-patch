package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeInt();

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            // Lifted directly from EigenDecompositionImplTest.testMathpbx02:
            // assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigvals] semantic mismatch: index=" + i + " expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            RealVector refVec = REF_EIGENVECTORS[i];
            double norm;
            if (refVec.dotProduct(actualVec) < 0) {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.add(actualVec).getNorm();
            } else {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.subtract(actualVec).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigvecs] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }

        // Post-condition / sibling-consistency oracle:
        // The class contract says for symmetric matrices getD() is diagonal and getRealEigenvalue(i)
        // reports the eigenvalues of the same decomposition. A patch that merely avoids the bad flip
        // branch or corrupts work[] can silently produce inconsistent readers even if no exception fires.
        RealMatrix d = decomposition.getD();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double fromScalar = decomposition.getRealEigenvalue(i);
            double fromArray = eigenValues[i];
            double fromMatrix = d.getEntry(i, i);
            if (Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromArray) ||
                Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromMatrix)) {
                throw new RuntimeException(
                    "[oracle:reader-agreement] metamorphic violation: getRealEigenvalue/getRealEigenvalues/getD diagonal disagree index=" +
                    i + " scalar=" + fromScalar + " array=" + fromArray + " matrixDiag=" + fromMatrix);
            }
            for (int j = 0; j < REF_EIGENVALUES.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0d) {
                    throw new RuntimeException(
                        "[oracle:diagonal-shape] metamorphic violation: getD must be diagonal for symmetric decomposition i=" +
                        i + " j=" + j + " offDiag=" + d.getEntry(i, j));
                }
            }
        }

        int scaleChoice = data.consumeInt(1, 8);
        double scale = scaleChoice / 4.0d;
        double[] scaledMain = new double[MAIN.length];
        double[] scaledSecondary = new double[SECONDARY.length];
        for (int i = 0; i < MAIN.length; ++i) {
            scaledMain[i] = MAIN[i] * scale;
        }
        for (int i = 0; i < SECONDARY.length; ++i) {
            scaledSecondary[i] = SECONDARY[i] * scale;
        }

        try {
            EigenDecomposition scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);
            double[] scaledEigenValues = scaled.getRealEigenvalues();

            // Metamorphic relation from linear algebra for the real API used here:
            // scaling a matrix by positive scalar s scales all eigenvalues by s and keeps eigenvectors
            // unchanged up to sign. This gives a trusted oracle derived from the seed, while still
            // exercising the patched decomposition path on additional real inputs.
            for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
                double expected = REF_EIGENVALUES[i] * scale;
                double actual = scaledEigenValues[i];
                if (Math.abs(expected - actual) > 1.0e-3 * Math.max(1.0d, scale)) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvals] metamorphic violation: positive scaling should scale eigenvalues inputScale=" +
                        scale + " index=" + i + " expected=" + expected + " actual=" + actual);
                }

                RealVector actualVec = scaled.getEigenvector(i);
                RealVector refVec = REF_EIGENVECTORS[i];
                double norm;
                if (refVec.dotProduct(actualVec) < 0) {
                    norm = refVec.add(actualVec).getNorm();
                } else {
                    norm = refVec.subtract(actualVec).getNorm();
                }
                if (Math.abs(norm) > 1.0e-5) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvecs] metamorphic violation: positive scaling should preserve eigenvectors up to sign inputScale=" +
                        scale + " index=" + i + " norm=" + norm);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}