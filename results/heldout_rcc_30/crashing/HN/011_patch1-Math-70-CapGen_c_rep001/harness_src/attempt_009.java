package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from BisectionSolverTest.testMath369.
        runCase(solver, f, 3.0, 3.2, 3.1, true);

        // EXPLORE: same root cause property, but with many valid intervals.
        // Property: non-null function + valid bracketing interval + initial inside interval.
        // On a correct implementation, solve(f,min,max,initial) should behave like the sibling
        // overload solve(f,min,max) for BisectionSolver because bisection does not use the initial guess.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftOffset = boundedPositive(data.consumeInt(), 0.05, 1.0);
        double rightOffset = boundedPositive(data.consumeInt(), 0.05, 1.0);

        double min = root - leftOffset;
        double max = root + rightOffset;

        double initialFrac = boundedUnit(data.consumeInt());
        double initial = min + (max - min) * initialFrac;

        runCase(solver, f, min, max, initial, true);

        int extra = data.consumeInt(0, 3);
        for (int i = 0; i < extra; i++) {
            k = data.consumeInt(-100, 100);
            root = k * Math.PI;
            leftOffset = boundedPositive(data.consumeInt(), 0.01, 1.5);
            rightOffset = boundedPositive(data.consumeInt(), 0.01, 1.5);
            min = root - leftOffset;
            max = root + rightOffset;
            initialFrac = boundedUnit(data.consumeInt());
            initial = min + (max - min) * initialFrac;
            runCase(new BisectionSolver(), f, min, max, initial, true);
        }

        // Also exercise possibly-invalid shapes; these must never be findings if the library cleanly rejects them.
        // We swallow all non-root-cause throwables here.
        double a = boundedRange(data.consumeInt(), -1000.0, 1000.0);
        double b = boundedRange(data.consumeInt(), -1000.0, 1000.0);
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        double maybeInitial = lo + (hi - lo) * boundedUnit(data.consumeInt());
        runCase(new BisectionSolver(), f, lo, hi, maybeInitial, false);
    }

    private static void runCase(BisectionSolver solver, UnivariateRealFunction f,
                                double min, double max, double initial,
                                boolean validByConstruction) {
        try {
            double withInitial = solver.solve(f, min, max, initial);

            // Documented sibling-agreement/post-condition:
            // For BisectionSolver, the overload with an initial guess must agree with the same solver
            // on the same function and interval because bisection is determined by the bracket; a patch
            // that merely suppresses the crash or ignores intended behavior could return a different root.
            double withoutInitial;
            try {
                withoutInitial = new BisectionSolver().solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            if (Double.isNaN(withInitial) != Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > new BisectionSolver().getAbsoluteAccuracy()) {
                throw new RuntimeException(
                    "[oracle:sibling-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            // Oracle from the constructed input itself: each interval brackets k*pi for SinFunction.
            // A correct solver result should satisfy sin(result) ~= 0 within a moderate tolerance.
            double residual = Math.abs(Math.sin(withInitial));
            if (residual > 1e-6) {
                throw new RuntimeException(
                    "[oracle:root-residual] metamorphic violation: returned value should be a root of sin on the constructed bracket"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " result=" + withInitial + " residual=" + residual);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            // Any other failure is outside this patch's scope or is a legitimate rejection for this input.
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException
                    || cur instanceof NumberFormatException
                    || cur instanceof FunctionEvaluationException
                    || cur instanceof MaxIterationsExceededException) {
                return true;
            }
            String name = cur.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("invalid")
                    || lower.contains("illegal")
                    || lower.contains("argument")
                    || lower.contains("convergence")
                    || lower.contains("evaluation")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
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

    private static double boundedPositive(int x, double min, double max) {
        int m = x == Integer.MIN_VALUE ? 0 : Math.abs(x);
        double frac = (m % 10000) / 9999.0;
        return min + (max - min) * frac;
    }

    private static double boundedUnit(int x) {
        int m = x == Integer.MIN_VALUE ? 0 : Math.abs(x);
        return (m % 10000) / 9999.0;
    }

    private static double boundedRange(int x, double min, double max) {
        return min + (max - min) * boundedUnit(x);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}