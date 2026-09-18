package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    private static final String[] EIGENVALUE_IDS = {
        "lifted-eigenvalue-0",
        "lifted-eigenvalue-1",
        "lifted-eigenvalue-2",
        "lifted-eigenvalue-3",
        "lifted-eigenvalue-4",
        "lifted-eigenvalue-5",
        "lifted-eigenvalue-6"
    };

    private static final String[] EIGENVECTOR_IDS = {
        "lifted-eigenvector-0",
        "lifted-eigenvector-1",
        "lifted-eigenvector-2",
        "lifted-eigenvector-3",
        "lifted-eigenvector-4",
        "lifted-eigenvector-5",
        "lifted-eigenvector-6"
    };

    private static final String[] SIBLING_REAL_IDS = {
        "sibling-realEigenvalue-0",
        "sibling-realEigenvalue-1",
        "sibling-realEigenvalue-2",
        "sibling-realEigenvalue-3",
        "sibling-realEigenvalue-4",
        "sibling-realEigenvalue-5",
        "sibling-realEigenvalue-6"
    };

    private static final String[] SIBLING_VECTOR_IDS = {
        "sibling-eigenvector-0",
        "sibling-eigenvector-1",
        "sibling-eigenvector-2",
        "sibling-eigenvector-3",
        "sibling-eigenvector-4",
        "sibling-eigenvector-5",
        "sibling-eigenvector-6"
    };

    private static final String[] REVERSE_VALUE_IDS = {
        "reverse-multiset-value-0",
        "reverse-multiset-value-1",
        "reverse-multiset-value-2",
        "reverse-multiset-value-3",
        "reverse-multiset-value-4",
        "reverse-multiset-value-5",
        "reverse-multiset-value-6",
        "reverse-multiset-value-7"
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runReadOnlyAndSiblingChecks();
        runImaginaryNullRelation(data);
        runReversedTridiagonalRelation(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVALUE_IDS[i] + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }

            final RealVector expectedVector = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            final double dot = expectedVector.dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = expectedVector.add(actualVector).getNorm();
            } else {
                norm = expectedVector.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVECTOR_IDS[i] + "] semantic mismatch: expectedNormDistance=0.0 actualNormDistance=" + norm + " tolerance=1.0E-5 dot=" + dot);
            }
        }
    }

    private static void runReadOnlyAndSiblingChecks() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] beforeValues;
        final double determinantBefore;
        final double[] imagBefore;
        try {
            beforeValues = dec.getRealEigenvalues();
            determinantBefore = dec.getDeterminant();
            imagBefore = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        try {
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] afterValues;
        final double determinantAfter;
        final double[] imagAfter;
        try {
            afterValues = dec.getRealEigenvalues();
            determinantAfter = dec.getDeterminant();
            imagAfter = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: get* methods are readers. A throw-deleting or bookkeeping-skipping patch in the decomposition path
           can silently corrupt shared state (work/pingPong-derived results); these read-only calls must still agree before/after. */
        String readOnlyViolation = compareDoubleArrays(beforeValues, afterValues, 0.0, "[oracle:readonly-realEigenvalues] metamorphic violation: get* readers changed real eigenvalues");
        if (readOnlyViolation != null) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + readOnlyViolation);
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-determinant] metamorphic violation: get* readers changed determinant before=" + determinantBefore + " after=" + determinantAfter);
        }
        if ((imagBefore == null) != (imagAfter == null)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-imaginary-nullness] metamorphic violation: get* readers changed imaginary array nullness beforeNull=" + (imagBefore == null) + " afterNull=" + (imagAfter == null));
        }
        if (imagBefore != null) {
            String imagViolation = compareDoubleArrays(imagBefore, imagAfter, 0.0, "[oracle:readonly-imaginary-values] metamorphic violation: get* readers changed imaginary eigenvalues");
            if (imagViolation != null) {
                throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + imagViolation);
            }
        }

        /* Contract justification: family readers for the same decomposition must agree on the same logical data.
           getRealEigenvalues()[i] and getRealEigenvalue(i) report the same eigenvalue; getV().getColumnVector(i) and getEigenvector(i)
           both expose eigenvectors. Shared-state corruption from flipIfWarranted/processGeneralBlock is observable here even if one reader alone looks plausible. */
        for (int i = 0; i < beforeValues.length; i++) {
            final double byIndex;
            final RealVector ev;
            final RealVector col;
            try {
                byIndex = dec.getRealEigenvalue(i);
                ev = dec.getEigenvector(i);
                col = dec.getV().getColumnVector(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.isNaN(beforeValues[i]) != Double.isNaN(byIndex) || Math.abs(beforeValues[i] - byIndex) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:" + SIBLING_REAL_IDS[i] + "] metamorphic violation: getRealEigenvalues()[" + i + "]=" + beforeValues[i] + " getRealEigenvalue(" + i + ")=" + byIndex);
            }
            double dot = ev.dotProduct(col);
            double norm = dot < 0 ? ev.add(col).getNorm() : ev.subtract(col).getNorm();
            if (norm > 1.0e-10) {
                throw new FuzzerSecurityIssueLow("[oracle:" + SIBLING_VECTOR_IDS[i] + "] metamorphic violation: getEigenvector(" + i + ") and getV().getColumnVector(" + i + ") differ by norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: this symmetric-only implementation returns null imaginary eigenvalue arrays.
           A patch that only suppresses the failing path but leaves internal state inconsistent can violate this observable post-condition. */
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversedTridiagonalRelation(FuzzedDataProvider data) {
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

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] ev1;
        final double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + (ev1 == null ? -1 : ev1.length) + " vs " + (ev2 == null ? -1 : ev2.length));
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
                String id = i < REVERSE_VALUE_IDS.length ? REVERSE_VALUE_IDS[i] : "reverse-multiset-value-extra";
                throw new FuzzerSecurityIssueLow("[oracle:" + id + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }

    private static String compareDoubleArrays(double[] a, double[] b, double tol, String prefix) {
        if (a == null || b == null) {
            return prefix + " nullMismatch aNull=" + (a == null) + " bNull=" + (b == null);
        }
        if (a.length != b.length) {
            return prefix + " lengthMismatch aLen=" + a.length + " bLen=" + b.length;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) != Double.isNaN(y)) {
                return prefix + " index=" + i + " a=" + x + " b=" + y;
            }
            if (!Double.isNaN(x) && Math.abs(x - y) > tol) {
                return prefix + " index=" + i + " a=" + x + " b=" + y + " tol=" + tol;
            }
        }
        return null;
    }
}