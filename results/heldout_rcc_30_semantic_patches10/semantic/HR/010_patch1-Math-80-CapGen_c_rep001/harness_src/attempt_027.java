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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleAndStateChecks();
        runImaginaryNullRelation(data);
        runReversePreservesEigenvalueMultiset(data);
    }

    private static void runLiftedOracleAndStateChecks() {
        EigenDecomposition decomposition = new EigenDecompositionImpl(
            TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeReal = decomposition.getRealEigenvalues();
        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            ArrayRealVector refVec = new ArrayRealVector(REF_EIGENVECTORS[i], true);
            double dot = refVec.dotProduct(actualVec);
            double norm = (dot < 0) ? refVec.add(actualVec).getNorm() : refVec.subtract(actualVec).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: expectedNormToBe=0.0 actualNorm=" +
                    norm + " dot=" + dot + " index=" + i);
            }
        }

        double[] afterReal = decomposition.getRealEigenvalues();
        if (beforeReal.length != afterReal.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-real-length] semantic mismatch: lengthBefore=" +
                beforeReal.length + " lengthAfter=" + afterReal.length);
        }
        for (int i = 0; i < beforeReal.length; i++) {
            if (Double.doubleToLongBits(beforeReal[i]) != Double.doubleToLongBits(afterReal[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-real-content] semantic mismatch: read-only getRealEigenvalues changed observable state at index=" +
                    i + " before=" + beforeReal[i] + " after=" + afterReal[i]);
            }
        }

        /* Contract/metamorphic justification:
         * getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers over the same decomposition state,
         * so any correct implementation must report the same real eigenvalue through both APIs.
         * A patch that merely suppresses the buggy path or silently corrupts bookkeeping/work-array state can still
         * return an array while leaving per-index reads inconsistent; this observable agreement check catches that.
         */
        for (int i = 0; i < afterReal.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            if (Math.abs(scalar - afterReal[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(afterReal[i])))) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue] semantic mismatch: getRealEigenvalue(" + i +
                    ")=" + scalar + " getRealEigenvalues()[" + i + "]=" + afterReal[i]);
            }
        }

        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-imag-null] semantic mismatch: expected null imaginary eigenvalue array for symmetric input but got length=" +
                imag.length);
        }

        double determinant1 = decomposition.getDeterminant();
        double determinant2 = decomposition.getDeterminant();
        if (Double.doubleToLongBits(determinant1) != Double.doubleToLongBits(determinant2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: read-only getDeterminant changed observable result before=" +
                determinant1 + " after=" + determinant2);
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

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void runReversePreservesEigenvalueMultiset(FuzzedDataProvider data) {
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
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reverse-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
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
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reverse-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }
}