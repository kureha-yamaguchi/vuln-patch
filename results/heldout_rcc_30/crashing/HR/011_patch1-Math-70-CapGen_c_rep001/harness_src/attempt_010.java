package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    private static final UnivariateRealFunction SIN = new SinFunction();

    private static final class SolveOutcome {
        final double value;
        final int iterations;
        final double accuracy;

        SolveOutcome(double value, int iterations, double accuracy) {
            this.value = value;
            this.iterations = iterations;
            this.accuracy = accuracy;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            double root = Math.PI * data.consumeInt(-20, 20);

            double leftDistance = 0.01 + (data.consumeInt(0, 900) / 1000.0);
            double rightDistance = 0.01 + (data.consumeInt(0, 900) / 1000.0);

            double min = root - leftDistance;
            double max = root + rightDistance;
            if (!(min < max)) {
                continue;
            }

            int initialSelector = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (initialSelector / 1000.0);
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }

            checkStoredAndExplicitAgreeOnIterations(min, max, initial);
        }
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(SIN, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isRootCauseNpeFromBisectionSolve(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkStoredAndExplicitAgreeOnIterations(double min, double max, double initial) {
        try {
            SolveOutcome stored = solveStored(min, max);
            SolveOutcome explicit = solveExplicit(min, max, initial);

            double tolerance = Math.max(stored.accuracy, explicit.accuracy) * 2.0 + 1e-12;

            /*
             * Contract used for this oracle:
             * BisectionSolver.solve(f, min, max, initial) is documented as the same solve operation
             * over the same function and interval, and the implementation in this class delegates to
             * the same bisection routine as the stored-function overload solve(min, max). Therefore,
             * for the same SinFunction and the same [min, max], both construction styles must run the
             * same deterministic algorithm and agree on both the returned root approximation and the
             * iteration count. A patch that merely suppresses the NPE but still consults the wrong
             * function/state can leave one of these observables inconsistent.
             */
            if (Math.abs(stored.value - explicit.value) > tolerance) {
                throw new RuntimeException(
                        "[oracle:iter-consistency] metamorphic violation: stored/explicit result mismatch"
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " stored=" + stored.value
                                + " explicit=" + explicit.value
                                + " tol=" + tolerance);
            }

            if (stored.iterations != explicit.iterations) {
                throw new RuntimeException(
                        "[oracle:iter-consistency] metamorphic violation: stored/explicit iteration mismatch"
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " storedIterations=" + stored.iterations
                                + " explicitIterations=" + explicit.iterations
                                + " storedValue=" + stored.value
                                + " explicitValue=" + explicit.value);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && startsWithOracle((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isRootCauseNpeFromBisectionSolve(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static SolveOutcome solveStored(double min, double max)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver(SIN);
        double value = solver.solve(min, max);
        return new SolveOutcome(value, solver.getIterationCount(), solver.getAbsoluteAccuracy());
    }

    private static SolveOutcome solveExplicit(double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver();
        double value = solver.solve(SIN, min, max, initial);
        return new SolveOutcome(value, solver.getIterationCount(), solver.getAbsoluteAccuracy());
    }

    private static boolean isRootCauseNpeFromBisectionSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("Argument")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("Illegal");
    }

    private static boolean startsWithOracle(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }
}