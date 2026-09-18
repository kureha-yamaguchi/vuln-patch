package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] SEED_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SEED_SECONDARY = {
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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkLiftedSeedOracles();
        checkImaginaryEigenvalueArrayIsNull(data);
        checkReversedTridiagonalPreservesEigenvalueMultiset(data);
        checkGetterAgreementAndReadOnlyState(data);
        checkEigenpairResidualContract(data);
    }

    private static void checkLiftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(SEED_MAIN, SEED_SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-shape] semantic mismatch: expectedLength=" +
                REF_EIGENVALUES.length + " actualLength=" +
                (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]);
            }
        }

        for (int i = 0; i < REF_EIGENVECTORS.length; ++i) {
            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector actual = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(actual);
            double norm;
            if (dot < 0) {
                norm = ref.add(actual).getNorm();
            } else {
                norm = ref.subtract(actual).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + norm + " dot=" + dot);
            }
        }

        // Contract/oracle: getRealEigenvalues()/getRealEigenvalue(i) are sibling readers over the same state and must agree.
        for (int i = 0; i < eigenValues.length; i++) {
            double byIndex = decomposition.getRealEigenvalue(i);
            if (Double.doubleToLongBits(byIndex) != Double.doubleToLongBits(eigenValues[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-sibling-reader] semantic mismatch: index=" + i +
                    " arrayValue=" + eigenValues[i] + " indexedValue=" + byIndex);
            }
        }

        // Contract/oracle: these get* methods are readers; corrupt shared state after a read is observable by re-reading.
        double[] before = eigenValues.clone();
        decomposition.getD();
        decomposition.getDeterminant();
        decomposition.getV();
        decomposition.getVT();
        decomposition.getImagEigenvalues();
        double[] after = decomposition.getRealEigenvalues();
        if (after == null || after.length != before.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-shape] semantic mismatch: beforeLength=" +
                before.length + " afterLength=" + (after == null ? -1 : after.length));
        }
        for (int i = 0; i < before.length; i++) {
            if (Double.doubleToLongBits(before[i]) != Double.doubleToLongBits(after[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-readonly-values] semantic mismatch: index=" + i +
                    " before=" + before[i] + " after=" + after[i]);
            }
        }

        // Contract/oracle: an eigen decomposition must satisfy A*v = lambda*v for each reported eigenpair.
        for (int i = 0; i < REF_EIGENVALUES.length; i++) {
            RealVector v = decomposition.getEigenvector(i);
            double lambda = decomposition.getRealEigenvalue(i);
            double residual = tridiagonalResidualNorm(SEED_MAIN, SEED_SECONDARY, v, lambda);
            double scale = residualScale(SEED_MAIN, SEED_SECONDARY, v, lambda);
            if (residual > 1.0e-8 * scale) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenpair-contract] semantic mismatch: index=" + i +
                    " residual=" + residual + " scale=" + scale + " lambda=" + lambda);
            }
        }
    }

    private static void checkImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 6);
        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-20000, 20000) / 20.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-20000, 20000) / 20.0;
        }

        double[] imag;
        try {
            EigenDecompositionImpl dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:imag-null] relation imaginary-eigenvalue-array-is-null violated: expected null but got length " + imag.length);
        }
    }

    private static void checkReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 6);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-100000, 100000) / 100.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-100000, 100000) / 100.0;
        }

        double[] revMain = new double[n];
        double[] revSecondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            revMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < n - 1; i++) {
            revSecondary[i] = secondary[n - 2 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(revMain, revSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != n || ev2.length != n) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:reverse-multiset-shape] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: wrong eigenvalue array shape");
        }

        double[] s1 = ev1.clone();
        double[] s2 = ev2.clone();
        Arrays.sort(s1);
        Arrays.sort(s2);
        for (int i = 0; i < n; i++) {
            double a = s1[i];
            double b = s2[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" +
                    i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static void checkGetterAgreementAndReadOnlyState(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 6);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-50000, 50000) / 50.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-50000, 50000) / 50.0;
        }

        EigenDecompositionImpl dec;
        double[] before;
        double[] after;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            before = dec.getRealEigenvalues().clone();
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getDeterminant();
            dec.getImagEigenvalues();
            after = dec.getRealEigenvalues().clone();
        } catch (Throwable t) {
            return;
        }

        if (before.length != after.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fuzz-readonly-shape] semantic mismatch: beforeLength=" +
                before.length + " afterLength=" + after.length);
        }

        for (int i = 0; i < before.length; i++) {
            if (Double.doubleToLongBits(before[i]) != Double.doubleToLongBits(after[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-readonly-values] semantic mismatch: index=" + i +
                    " before=" + before[i] + " after=" + after[i]);
            }
        }

        // Contract/oracle: same logical eigenvalue reported through array getter and indexed getter must agree.
        for (int i = 0; i < before.length; i++) {
            double byIndex;
            try {
                byIndex = dec.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(byIndex) != Double.doubleToLongBits(before[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-sibling-reader] semantic mismatch: index=" + i +
                    " arrayValue=" + before[i] + " indexedValue=" + byIndex);
            }
        }
    }

    private static void checkEigenpairResidualContract(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 6);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-20000, 20000) / 20.0;
        }
        for (int i = 0; i < n - 1; i++) {
            secondary[i] = data.consumeInt(-20000, 20000) / 20.0;
        }

        double[] eigenvalues;
        RealVector[] eigenvectors = new RealVector[n];
        try {
            EigenDecompositionImpl dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            eigenvalues = dec.getRealEigenvalues();
            if (eigenvalues == null || eigenvalues.length != n) {
                return;
            }
            for (int i = 0; i < n; i++) {
                eigenvectors[i] = dec.getEigenvector(i);
            }
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle: reported eigenpairs of the decomposition satisfy A*v = lambda*v.
        for (int i = 0; i < n; i++) {
            double residual = tridiagonalResidualNorm(main, secondary, eigenvectors[i], eigenvalues[i]);
            double scale = residualScale(main, secondary, eigenvectors[i], eigenvalues[i]);
            if (residual > 1.0e-6 * scale) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:fuzz-eigenpair-contract] metamorphic violation: index=" + i +
                    " residual=" + residual + " scale=" + scale + " lambda=" + eigenvalues[i]);
            }
        }
    }

    private static double tridiagonalResidualNorm(double[] main, double[] secondary, RealVector v, double lambda) {
        double sum = 0.0;
        for (int i = 0; i < main.length; i++) {
            double av = main[i] * v.getEntry(i);
            if (i > 0) {
                av += secondary[i - 1] * v.getEntry(i - 1);
            }
            if (i + 1 < main.length) {
                av += secondary[i] * v.getEntry(i + 1);
            }
            double diff = av - lambda * v.getEntry(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static double residualScale(double[] main, double[] secondary, RealVector v, double lambda) {
        double inf = 0.0;
        for (int i = 0; i < main.length; i++) {
            double row = Math.abs(main[i]);
            if (i > 0) {
                row += Math.abs(secondary[i - 1]);
            }
            if (i + 1 < main.length) {
                row += Math.abs(secondary[i]);
            }
            if (row > inf) {
                inf = row;
            }
        }
        double vNorm = v.getNorm();
        return 1.0 + (inf + Math.abs(lambda)) * Math.max(1.0, vNorm);
    }
}