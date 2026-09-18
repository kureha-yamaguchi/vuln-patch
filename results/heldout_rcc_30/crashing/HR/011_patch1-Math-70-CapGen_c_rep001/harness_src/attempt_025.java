package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidth = 1.0e-6 + (data.consumeInt(1, 1000000) / 1000000.0) * 0.9;
        double rightWidth = 1.0e-6 + (data.consumeInt(1, 1000000) / 1000000.0) * 0.9;

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initialFraction = data.consumeInt(0, 1000000) / 1000000.0;
        double initial = min + (max - min) * initialFraction;

        UnivariateRealFunction f = new SinFunction();

        try {
            exerciseExplicitAndStored(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        try {
            exerciseExplicitAndStored(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void exerciseExplicitAndStored(UnivariateRealFunction f, double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver explicitSolver = new BisectionSolver();
        double explicit = explicitSolver.solve(f, min, max, initial);

        BisectionSolver storedSolver = new BisectionSolver(f);
        double stored = storedSolver.solve(min, max);

        /*
         * Contract/oracle:
         * These are two public overloads of the same solver on the same function and interval.
         * A correct implementation must solve the same mathematical problem either way.
         * This also steers execution through the uncovered stored-function overload solve(double, double).
         */
        double tol = Math.max(explicitSolver.getAbsoluteAccuracy(), storedSolver.getAbsoluteAccuracy()) * 8.0;
        if (Math.abs(explicit - stored) > tol) {
            throw new RuntimeException(
                    "[oracle:stored-overload-eq] metamorphic violation: explicit and stored overloads disagree"
                            + " min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " explicit=" + explicit
                            + " stored=" + stored
                            + " tol=" + tol);
        }

        /*
         * Independent oracle from the constructed input:
         * We bracket the known root k*pi of sin(x) by construction, so a correct solver must
         * return a point close to that known root on this narrow interval.
         */
        double expected = Math.rint(explicit / Math.PI) * Math.PI;
        if (expected >= min && expected <= max) {
            double knownTol = Math.max(tol, 1.0e-6);
            if (Math.abs(explicit - expected) > knownTol && Math.abs(stored - expected) > knownTol) {
                throw new RuntimeException(
                        "[oracle:sin-known-root] metamorphic violation: neither overload returned the constructed root"
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " expected=" + expected
                                + " explicit=" + explicit
                                + " stored=" + stored
                                + " tol=" + knownTol);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("Argument");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>uncheckedThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }
}