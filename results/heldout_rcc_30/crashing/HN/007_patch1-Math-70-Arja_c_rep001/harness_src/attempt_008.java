package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        int k = data.consumeInt(-100, 100);
        double expectedRoot = k * Math.PI;

        double leftWidth = data.consumeInt(100, 1500) / 1000.0;
        double rightWidth = data.consumeInt(100, 1500) / 1000.0;
        double min = expectedRoot - leftWidth;
        double max = expectedRoot + rightWidth;

        int selector = data.consumeInt(0, 1000);
        double initial;
        if (selector == 0) {
            initial = expectedRoot;
        } else {
            double frac = selector / 1000.0;
            initial = min + (max - min) * frac;
            if (initial <= min) {
                initial = Math.nextUp(min);
            } else if (initial >= max) {
                initial = Math.nextAfter(max, min);
            }
        }

        Double withInitial;
        Double withoutInitial;
        double acc1;
        double acc2;

        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            acc1 = solver1.getAbsoluteAccuracy();

            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
            acc2 = solver2.getAbsoluteAccuracy();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        double tol = Math.max(Math.max(acc1, acc2), 1.0e-6);

        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + withoutInitial);
        }

        if (Math.abs(withInitial.doubleValue() - expectedRoot) > tol) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: solver did not recover the constructed sine root"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + expectedRoot);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            RuntimeException rt = (RuntimeException) t;
            String msg = rt.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw rt;
            }
            if (validByConstruction && rt instanceof NullPointerException && hasBisectionSolveFrame(rt)) {
                throw rt;
            }
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }
    }

    private static boolean hasBisectionSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}