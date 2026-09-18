package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction sin =
                new org.apache.commons.math.analysis.SinFunction();

        // Anchor: exact regression input from BisectionSolverTest.testMath369.
        runSolveCase(sin, 3.0, 3.2, 3.1, true);

        int cases = data.consumeInt(1, 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int widthMilli = data.consumeInt(10, 1000);
            double halfWidth = widthMilli / 1000.0;

            double min = root - halfWidth;
            double max = root + halfWidth;

            int fracMilli = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (fracMilli / 1000.0);

            runSolveCase(sin, min, max, initial, true);
        }
    }

    private static void runSolveCase(org.apache.commons.math.analysis.UnivariateRealFunction f,
                                     double min, double max, double initial,
                                     boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract/oracle: same-name overloads with the same documented purpose must agree on
        // equivalent inputs. For a correct implementation, solve(f, min, max, initial) and
        // solve(f, min, max) solve the same bracketed equation on the same function; deleting the
        // function-taking delegation or routing to the wrong overload breaks this observable result.
        try {
            BisectionSolver withInitial = new BisectionSolver();
            double r1 = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double r2 = withoutInitial.solve(f, min, max);

            double tol = Math.max(
                    Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()) * 4.0,
                    1.0e-8);

            if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > tol) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                                + " lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrow(t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && !(t instanceof IllegalArgumentException) && validByConstruction
                && isSolveStack(t) && t instanceof NullPointerException) {
            return true;
        }
        return false;
    }

    private static boolean isSolveStack(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String cn = t.getClass().getName();
        return cn.contains("Invalid")
                || cn.contains("OutOfRange")
                || cn.contains("NoBracketing")
                || cn.contains("NotStrictlyPositive")
                || cn.contains("NotPositive");
    }

    private static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}