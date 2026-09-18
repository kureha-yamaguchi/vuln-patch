package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();
        exerciseCase(anchorFunction, 3.0d, 3.2d, 3.1d, true);

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;
        double delta = 0.1d + (data.consumeInt(0, 900) / 1000.0d);
        double min = root - delta;
        double max = root + delta;

        double initial;
        int initialMode = data.consumeInt(0, 3);
        if (initialMode == 0) {
            initial = (min + max) / 2.0d;
        } else if (initialMode == 1) {
            initial = min + (max - min) * 0.25d;
        } else if (initialMode == 2) {
            initial = min + (max - min) * 0.75d;
        } else {
            int parts = data.consumeInt(1, 1000);
            initial = min + (max - min) * (parts / 1000.0d);
        }

        exerciseCase(new SinFunction(), min, max, initial, true);
    }

    private static void exerciseCase(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        Double withInitial = callSolveWithInitial(f, min, max, initial, validByConstruction);
        if (withInitial == null) {
            return;
        }

        Double withoutInitial = callSolveWithoutInitial(f, min, max, validByConstruction);
        if (withoutInitial == null) {
            return;
        }

        BisectionSolver referenceSolver = new BisectionSolver();
        double tolerance = referenceSolver.getAbsoluteAccuracy();

        /* Contract/oracle:
         * The same-name overloads solve(f, min, max, initial) and solve(f, min, max)
         * solve the same function on the same interval and therefore must return the
         * same root up to solver accuracy for a correct implementation. A "fix" that
         * merely suppresses the crash or skips using the provided function would break
         * this sibling-agreement relation.
         */
        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tolerance) {
            throw new RuntimeException(
                "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + ", f=" + f.getClass().getName()
                    + "} lhs=" + withInitial
                    + " rhs=" + withoutInitial
                    + " tol=" + tolerance);
        }
    }

    private static Double callSolveWithInitial(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return null;
        }
    }

    private static Double callSolveWithoutInitial(UnivariateRealFunction f, double min, double max, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max));
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return null;
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                return true;
            }
        }

        if (isCleanRejection(t)) {
            return false;
        }

        return validByConstruction && (t instanceof NullPointerException) && hasSolveFrame(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            if (c == IllegalArgumentException.class || c == NumberFormatException.class) {
                return true;
            }
            String name = c.getName();
            if (name.startsWith("org.apache.commons.math")) {
                String lower = name.toLowerCase();
                if (lower.contains("argument") || lower.contains("invalid") || lower.contains("illegal")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
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