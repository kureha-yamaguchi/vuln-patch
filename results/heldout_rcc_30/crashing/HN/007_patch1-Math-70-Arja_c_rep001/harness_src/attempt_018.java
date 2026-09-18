package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input. On the buggy version, this reaches
        // BisectionSolver.solve(f, min, max, initial) and throws NPE from solve.
        try {
            BisectionSolver solver = new BisectionSolver();
            double r4 = solver.solve(f, 3.0, 3.2, 3.1);

            // Metamorphic/post-condition:
            // The same-name overloads are documented to solve the same problem for the same
            // function and interval; the extra "initial" argument must not change the root
            // found by bisection here. A throw-deleting or branch-skipping patch could avoid
            // the crash yet return a wrong value, so compare against the sibling overload.
            BisectionSolver sibling = new BisectionSolver();
            double r3 = sibling.solve(f, 3.0, 3.2);
            double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 4.0;
            if (!(Double.isNaN(r4) || Double.isNaN(r3)) && Math.abs(r4 - r3) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r4 + " rhs=" + r3);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        int cases = 1 + Math.abs(data.consumeInt(0, 4));
        for (int i = 0; i < cases; i++) {
            double[] interval = makeValidSinBracket(data);
            double min = interval[0];
            double max = interval[1];
            double initial = min + (max - min) * boundedFraction(data);

            try {
                BisectionSolver solver4 = new BisectionSolver();
                double r4 = solver4.solve(f, min, max, initial);

                // Same documented problem, sibling overload agreement on the same valid input.
                BisectionSolver solver3 = new BisectionSolver();
                double r3 = solver3.solve(f, min, max);

                double tol = Math.max(solver4.getAbsoluteAccuracy(), solver3.getAbsoluteAccuracy()) * 4.0;
                if (!(Double.isNaN(r4) || Double.isNaN(r3)) && Math.abs(r4 - r3) > tol) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + r4 + " rhs=" + r3);
                }
            } catch (Throwable t) {
                if (shouldPropagate(t, true)) {
                    throwUnchecked(t);
                }
                return;
            }
        }
    }

    private static double[] makeValidSinBracket(FuzzedDataProvider data) {
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        // Keep the width moderate and non-degenerate; symmetric interval around k*pi
        // guarantees sign change for sin as long as width is in (0, pi).
        int milli = data.consumeInt(50, 3000);
        double width = milli / 1000.0;
        if (width >= Math.PI) {
            width = Math.PI - 0.001;
        }

        double min = root - width;
        double max = root + width;
        if (!(min < max)) {
            min = root - 0.5;
            max = root + 0.5;
        }
        return new double[] { min, max };
    }

    private static double boundedFraction(FuzzedDataProvider data) {
        int v = data.consumeInt(0, 1000000);
        return v / 1000000.0;
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (t instanceof RuntimeException) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return false;
            }
            if (t instanceof NullPointerException && passesThroughPatchedSolve(t)) {
                return true;
            }
            if (t.getClass() == RuntimeException.class && isOracleException(t)) {
                return true;
            }
            return false;
        }
        // Checked exceptions from argument checking / solver rejection are clean post-fix behavior.
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return false;
        }
        return false;
    }

    private static boolean isOracleException(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean passesThroughPatchedSolve(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
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