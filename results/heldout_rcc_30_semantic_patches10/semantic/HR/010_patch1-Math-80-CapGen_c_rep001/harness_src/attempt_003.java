package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryNullInvariant(data);
        runReversalMetamorphic(data);
        runReadOnlyStabilityMetamorphic(data);
    }

    private static void runLiftedOracle() {
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

        final EigenDecomposition decomposition;
        final double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-seed] semantic mismatch: index=" + i + " expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = refEigenVectors[i].dotProduct(actualVector);
            double norm = dot < 0 ? refEigenVectors[i].add(actualVector).getNorm() : refEigenVectors[i].subtract(actualVector).getNorm();
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-seed] semantic mismatch: index=" + i + " expectedNormToReference=0.0 actualNorm=" + norm + " tol=1.0E-5");
            }
        }

        double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }

        double[] first = eigenValues.clone();
        double[] second;
        try {
            second = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (second == null || second.length != first.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues-length] metamorphic violation: firstLength=" + first.length + " secondLength=" + (second == null ? -1 : second.length));
        }
        for (int i = 0; i < first.length; i++) {
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues-value] metamorphic violation: index=" + i + " first=" + first[i] + " second=" + second[i]);
            }
        }
    }

    private static void runImaginaryNullInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null-invariant] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: actualLength=" + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN).getRealEigenvalues();
            ev2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN).getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reversal-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: len1=" + (ev1 == null ? -1 : ev1.length) + " len2=" + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reversal-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }

    private static void runReadOnlyStabilityMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-250000, 250000) / 32.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-250000, 250000) / 32.0;
        }

        EigenDecompositionImpl dec;
        double[] evA;
        double[] evB;
        double detA;
        double detB;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            evA = dec.getRealEigenvalues();
            detA = dec.getDeterminant();
            evB = dec.getRealEigenvalues();
            detB = dec.getDeterminant();
        } catch (Throwable t) {
            return;
        }

        // Read-only/post-condition guarantee: get* methods are observers. A throw-deleting or bookkeeping-skipping patch can leave hidden state such that successive reads disagree.
        if (evA == null || evB == null || evA.length != evB.length) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-length] metamorphic violation: lenA=" + (evA == null ? -1 : evA.length) + " lenB=" + (evB == null ? -1 : evB.length));
        }
        for (int i = 0; i < evA.length; i++) {
            if (Double.doubleToLongBits(evA[i]) != Double.doubleToLongBits(evB[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-value] metamorphic violation: index=" + i + " first=" + evA[i] + " second=" + evB[i]);
            }
        }
        if (Double.doubleToLongBits(detA) != Double.doubleToLongBits(detB)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-determinant] metamorphic violation: first=" + detA + " second=" + detB);
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}