package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(anchorFunction, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        int iterations = 1 + data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double leftWidth = 0.01 + (data.consumeInt(1, 1000) / 1000.0) * 1.4;
            double rightWidth = 0.01 + (data.consumeInt(1, 1000) / 1000.0) * 1.4;

            double min = root - leftWidth;
            double max = root + rightWidth;
            if (!(min < max)) {
                continue;
            }

            double selector = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * selector;

            UnivariateRealFunction f = new SinFunction();

            try {
                BisectionSolver solver = new BisectionSolver();
                solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                if (shouldPropagateRootCause(t, true)) {
                    throwUnchecked(t);
                }
                continue;
            }

            /* Contract/oracle:
             * The same-name overloads solve(f, min, max, initial) and solve(f, min, max)
             * are documented to agree on equivalent inputs. For BisectionSolver, the initial
             * guess must not change the computed root on a valid bracket. A patch that merely
             * suppresses the crash or skips the real computation can violate this sibling-
             * agreement relation without throwing.
             */
            try {
                BisectionSolver lhsSolver = new BisectionSolver();
                double lhs = lhsSolver.solve(f, min, max, initial);

                BisectionSolver rhsSolver = new BisectionSolver();
                double rhs = rhsSolver.solve(f, min, max);

                double tol = Math.max(lhsSolver.getAbsoluteAccuracy(), rhsSolver.getAbsoluteAccuracy()) + 1e-12;
                if (Double.isNaN(lhs) != Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                if (shouldPropagateRootCause(t, true)) {
                    throwUnchecked(t);
                }
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
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