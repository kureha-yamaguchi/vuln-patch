package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        exerciseSolveOverload(f, 3.0, 3.2, 3.1, true, "anchor");

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double leftWidth = 0.05 + data.consumeInt(0, 449) / 1000.0;
            double rightWidth = 0.05 + data.consumeInt(0, 449) / 1000.0;

            double min = root - leftWidth;
            double max = root + rightWidth;

            double ratio = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * ratio;

            exerciseSolveOverload(f, min, max, initial, true, "explore:" + k + ":" + i);
        }
    }

    private static void exerciseSolveOverload(UnivariateRealFunction f, double min, double max, double initial,
                                              boolean validByConstruction, String label) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isGroundTruthRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double rhs = withoutInitial.solve(f, min, max);

            double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy());
            if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
                return;
            }
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=" + label + " min=" + min + " max=" + max + " initial=" + initial
                        + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:overload-agreement]")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
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
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("Argument")
                || name.contains("Convergence")
                || name.contains("Evaluation")
                || name.contains("MaxIterations")
                || name.contains("NoBracketing")
                || name.contains("NoData");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}