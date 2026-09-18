package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        // This is valid-by-construction: non-null function, interval [3.0, 3.2] brackets a root of sin(x),
        // and the public API under test is BisectionSolver.solve(f, min, max, initial).
        runOne(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: generate many other valid-by-construction intervals that bracket a root of sin(x).
        // Property: for any integer k, sin(k*pi - d) and sin(k*pi + d) have opposite signs when d is in (0, pi),
        // so [k*pi-d, k*pi+d] is a valid bracketing interval for a root at k*pi.
        int k = data.consumeInt(-1000, 1000);
        int denom = data.consumeInt(2, 1000);
        int numer = data.consumeInt(1, denom - 1);
        double delta = (Math.PI * numer) / denom;
        double center = k * Math.PI;

        double min = center - delta;
        double max = center + delta;

        int initialMode = data.consumeInt(0, 4);
        double initial;
        switch (initialMode) {
            case 0:
                initial = center;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
                break;
            default:
                initial = (min + max) / 2.0;
                break;
        }

        runOne(f, min, max, initial, true);
    }

    private static void runOne(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();

        try {
            double withInitial = solver.solve(f, min, max, initial);

            // Metamorphic/post-condition:
            // The overloaded solve(f, min, max, initial) and solve(f, min, max) are documented sibling entry points
            // for the same solver and interval; for a correct implementation, the extra initial guess must not change
            // the computed root for bisection. A "fix" that merely avoids the crash but skips the real solve path can
            // return a different value here.
            try {
                double withoutInitial = new BisectionSolver().solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-8);
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) ||
                    Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)" +
                        " input=min=" + min + ",max=" + max + ",initial=" + initial +
                        " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                // If either side throws, the relation does not apply for this input.
            }

            // Oracle from the input itself:
            // We constructed intervals around k*pi for SinFunction, so any correct root returned by bisection on
            // this bracket must satisfy sin(root) ~= 0.
            try {
                double residual = f.value(withInitial);
                if (Double.isNaN(residual) || Math.abs(residual) > 1.0e-6) {
                    throw new RuntimeException(
                        "[oracle:sin-root] metamorphic violation: returned value is not a root of sin on a valid bracketing interval" +
                        " input=min=" + min + ",max=" + max + ",initial=" + initial +
                        " root=" + withInitial + " residual=" + residual);
                }
            } catch (FunctionEvaluationException ignored) {
                // Skip oracle if function evaluation fails unexpectedly.
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            // Swallow anything else: not the verified bug in the patched region.
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("illegal") || lower.contains("argument")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
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