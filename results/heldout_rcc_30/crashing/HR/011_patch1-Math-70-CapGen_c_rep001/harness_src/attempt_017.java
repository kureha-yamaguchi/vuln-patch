package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-4, 4);
        double root = k * Math.PI;

        double d1 = 1e-3 + (data.consumeInt(0, 5000) / 10000.0);
        double d2 = 1e-3 + (data.consumeInt(0, 5000) / 10000.0);

        double min;
        double max;
        switch (data.consumeInt(0, 2)) {
            case 0:
                min = root;
                max = root + d2;
                break;
            case 1:
                min = root - d1;
                max = root;
                break;
            default:
                min = root - d1;
                max = root + d2;
                break;
        }

        double initial;
        switch (data.consumeInt(0, 3)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) * 0.5;
                break;
            default:
                double t = data.consumeInt(0, 1000) / 1000.0;
                initial = min + (max - min) * t;
                break;
        }

        exerciseCase(f, root, min, max, initial);

        double mirror = Math.max(d1, d2);
        double leftMin = root - mirror;
        double leftMax = root;
        double rightMin = root;
        double rightMax = root + mirror;

        try {
            double lhs = solveStored(f, leftMin, leftMax);
            double rhs = solveStored(f, rightMin, rightMax);
            double tol = tolerance();
            if (Math.abs(lhs - root) > tol || Math.abs(rhs - root) > tol || Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:endpoint-duality] metamorphic violation: endpoint-root intervals around the same sin root must agree inputRoot=" + root + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        try {
            double r = solveExplicit(f, 3.0, 3.2, 3.1);
            double tol = tolerance();
            if (Math.abs(r - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-boundary] metamorphic violation: anchored seed should solve sin near pi result=" + r + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exerciseCase(UnivariateRealFunction f, double expectedRoot, double min, double max, double initial) {
        try {
            double explicit = solveExplicit(f, min, max, initial);
            double tol = tolerance();
            if (Math.abs(explicit - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:endpoint-root] metamorphic violation: a correct bisection solve on a valid sin interval bracketing k*pi must return that root expected=" + expectedRoot + " actual=" + explicit + " min=" + min + " max=" + max + " initial=" + initial + " tol=" + tol);
            }

            double stored = solveStored(f, min, max);
            if (Math.abs(stored - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:stored-endpoint-root] metamorphic violation: stored-function solve on the same valid sin interval must return the same known root expected=" + expectedRoot + " actual=" + stored + " min=" + min + " max=" + max + " tol=" + tol);
            }

            if (Math.abs(explicit - stored) > tol) {
                throw new RuntimeException("[oracle:explicit-vs-stored-endpoint] metamorphic violation: explicit-function and stored-function solves with the same real function and interval must agree explicit=" + explicit + " stored=" + stored + " min=" + min + " max=" + max + " initial=" + initial + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double solveExplicit(UnivariateRealFunction f, double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver();
        return solver.solve(f, min, max, initial);
    }

    private static double solveStored(UnivariateRealFunction f, double min, double max)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver();
        solver.f = f;
        return solver.solve(min, max);
    }

    private static double tolerance() {
        return 1e-5;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}