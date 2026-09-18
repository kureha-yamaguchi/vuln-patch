package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 990) / 1000.0);
        double right = 0.01 + (data.consumeInt(0, 990) / 1000.0);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        exerciseCase(f, min, max, initial);

        if (data.consumeBoolean()) {
            double tighterLeft = left * 0.5;
            double tighterRight = right * 0.5;
            double min2 = root - tighterLeft;
            double max2 = root + tighterRight;
            if (min2 < max2) {
                double fraction2 = data.consumeInt(0, 1000) / 1000.0;
                double initial2 = min2 + (max2 - min2) * fraction2;
                exerciseCase(f, min2, max2, initial2);
            }
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double bisected = solver.solve(f, 3.0, 3.2, 3.1);
            try {
                double brent = new BrentSolver().solve(f, 3.0, 3.2);
                assertClose("anchor-cross-solver", 3.0, 3.2, bisected, brent, tolerance(solver, new BrentSolver()));
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }

            try {
                new BisectionSolver(f).solve(3.0, 3.2);
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseCase(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver explicitSolver = new BisectionSolver();
        double explicit;
        try {
            explicit = explicitSolver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            new BisectionSolver(f).solve(min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
        }

        double brent;
        BrentSolver brentSolver = new BrentSolver();
        try {
            brent = brentSolver.solve(f, min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            return;
        }

        /*
         * Contract used for this oracle:
         * both public solvers are asked to solve the same valid bracketing interval for the same real function.
         * For sin(x) on an interval constructed to contain exactly one root k*pi and no other roots,
         * any correct implementation must approximate that same root.
         * A patch that merely suppresses the explicit-overload NPE but routes to stale/null state can return
         * the wrong quantity; cross-checking against an independent real solver detects that.
         */
        assertClose("cross-solver-single-root", min, max, explicit, brent, tolerance(explicitSolver, brentSolver));

        /*
         * Additional metamorphic check:
         * narrowing a valid bracketing interval around the same unique root must not move the reported root
         * outside the accuracy budget by more than solver tolerances.
         */
        double span = max - min;
        if (span > 0.04) {
            double shrink = span * 0.25;
            double min2 = min + shrink;
            double max2 = max - shrink;
            if (min2 < max2 && min2 < 0.5 * (min + max) && 0.5 * (min + max) < max2) {
                try {
                    double explicit2 = new BisectionSolver().solve(f, min2, max2, 0.5 * (min2 + max2));
                    double tol = Math.max(tolerance(explicitSolver, brentSolver), 1e-6);
                    if (Math.abs(explicit - explicit2) > tol * 8.0) {
                        throw new RuntimeException(
                                "[oracle:interval-shrink-consistency] metamorphic violation: same sin root moved too far"
                                        + " min=" + min + " max=" + max
                                        + " min2=" + min2 + " max2=" + max2
                                        + " lhs=" + explicit + " rhs=" + explicit2);
                    }
                } catch (Throwable t) {
                    if (isRootCauseNpe(t)) {
                        throwUnchecked(t);
                    }
                    if (isOracleFailure(t)) {
                        throw (RuntimeException) t;
                    }
                }
            }
        }
    }

    private static double tolerance(BisectionSolver bisection, BrentSolver brent) {
        double tol = Math.max(bisection.getAbsoluteAccuracy(), brent.getAbsoluteAccuracy());
        if (tol < 1e-8) {
            tol = 1e-8;
        }
        return tol * 16.0;
    }

    private static void assertClose(String oracleId, double min, double max, double lhs, double rhs, double tol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: solver disagreement"
                            + " min=" + min + " max=" + max
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("Convergence") >= 0
                || name.indexOf("Argument") >= 0
                || name.indexOf("Invalid") >= 0
                || name.indexOf("Evaluation") >= 0;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}