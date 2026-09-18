package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    /*__vpCause*/ private static Throwable __vpCause = null;

    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int shiftInt = data.consumeInt(-100000, 100000);
        double shift = (double) shiftInt;

        RegulaFalsiSolver seedSolver = new RegulaFalsiSolver();
        Double seedRoot = null;
        try {
            seedRoot = seedSolver.solve(3624, ISSUE631_FUNCTION, 1.0, 10.0);
        } catch (TooManyEvaluationsException expectedOnPatchedBuild) { __vpCause = expectedOnPatchedBuild;
            seedRoot = null;
        } catch (RuntimeException ignored) {
            return;
        }

        if (seedRoot == null) {
            return;
        }

        RegulaFalsiSolver translatedSolver = new RegulaFalsiSolver();
        UnivariateRealFunction translated = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x - shift) - Math.pow(Math.PI, 3.0);
            }
        };

        Double translatedRoot;
        try {
            translatedRoot = translatedSolver.solve(3624, translated, 1.0 + shift, 10.0 + shift);
        } catch (RuntimeException ex) {
            return;
        }

        double min = translatedSolver.getMin();
        double max = translatedSolver.getMax();
        if (min != 1.0 + shift || max != 10.0 + shift) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:translated-bounds-state] consistency violation: " +
                "reportedMin=" + min +
                " expectedMin=" + (1.0 + shift) +
                " reportedMax=" + max +
                " expectedMax=" + (10.0 + shift), __vpCause);
        }

        double expectedTranslatedRoot = seedRoot + shift;
        double atol = translatedSolver.getAbsoluteAccuracy();
        double rtol = translatedSolver.getRelativeAccuracy();
        double tol = Math.max(atol, rtol * Math.max(Math.abs(expectedTranslatedRoot), Math.abs(translatedRoot)));

        double diff = Math.abs(translatedRoot - expectedTranslatedRoot);
        /* Documented guarantee behind this check:
           solving f on [a,b] and solving the translated function g(x)=f(x-shift)
           on [a+shift,b+shift] are equivalent root-finding problems; any correct
           implementation must therefore return roots related by the same shift.
           A band-aid that merely suppresses the known symptom can still leave the
           REGULA_FALSI stagnation workaround active, and that workaround depends on
           abs(x1)/accuracy state, so translation can change the returned value. */
        if (diff > 8.0 * tol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:translation-equiv] metamorphic violation: shift-equivalent solves disagree " +
                "shift=" + shift +
                " seedRoot=" + seedRoot +
                " translatedRoot=" + translatedRoot +
                " expectedTranslatedRoot=" + expectedTranslatedRoot +
                " diff=" + diff +
                " tol=" + (8.0 * tol) +
                " seedMin=" + 1.0 +
                " seedMax=" + 10.0 +
                " translatedMin=" + min +
                " translatedMax=" + max +
                " absAcc=" + atol +
                " relAcc=" + rtol +
                " fAcc=" + translatedSolver.getFunctionValueAccuracy());
        }
    }
}