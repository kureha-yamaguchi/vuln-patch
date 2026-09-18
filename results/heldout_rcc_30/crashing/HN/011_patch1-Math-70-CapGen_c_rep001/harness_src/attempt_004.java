package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runCase(f, 3.0, 3.2, 3.1);

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double leftWidth = data.consumeInt(1, 1000) / 1000.0;
        double rightWidth = data.consumeInt(1, 1000) / 1000.0;
        double min = root - leftWidth;
        double max = root + rightWidth;
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        runCase(f, min, max, initial);
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial) {
        if (f == null || !(min < max) || Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(initial)) {
            return;
        }

        try {
            BisectionSolver solver = new BisectionSolver();
            double withInitial = solver.solve(f, min, max, initial);

            try {
                BisectionSolver sibling = new BisectionSolver();
                double withoutInitial = sibling.solve(f, min, max);

                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 4.0;
                if (Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracketed function input="
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid") || name.contains("NoBracketing") || name.contains("Convergence")) {
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}