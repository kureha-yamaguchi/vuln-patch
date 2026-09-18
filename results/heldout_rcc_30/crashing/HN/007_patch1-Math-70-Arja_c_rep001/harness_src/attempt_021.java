package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runScenario(f, 3.0, 3.2, 3.1, true);

        int scenarios = data.consumeInt(1, 4);
        for (int i = 0; i < scenarios; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            double leftDelta = data.consumeInt(50, 1000) / 1000.0;
            double rightDelta = data.consumeInt(50, 1000) / 1000.0;

            double min = root - leftDelta;
            double max = root + rightDelta;

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0);

            runScenario(f, min, max, initial, true);
        }
    }

    private static void runScenario(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        double lhs;
        try {
            BisectionSolver solver = new BisectionSolver();
            lhs = solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrowUnchecked(t);
            }
            return;
        }

        double rhs;
        double tol;
        try {
            BisectionSolver solver = new BisectionSolver();
            rhs = solver.solve(f, min, max);
            tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
        } catch (Throwable t) {
            return;
        }

        if ((Double.isNaN(lhs) && !Double.isNaN(rhs))
                || (!Double.isNaN(lhs) && Double.isNaN(rhs))
                || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}