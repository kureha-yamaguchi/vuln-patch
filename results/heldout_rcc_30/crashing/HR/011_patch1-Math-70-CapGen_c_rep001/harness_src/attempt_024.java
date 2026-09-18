package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeed();
        runScaledLinearOracle(data);
    }

    private static void runAnchorSeed() {
        BisectionSolver solver = new BisectionSolver();
        UnivariateRealFunction f = new SinFunction();
        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeInSolve(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void runScaledLinearOracle(FuzzedDataProvider data) {
        double c = data.consumeInt(-1000, 1000) / 10.0;
        double left = 0.1 + (data.consumeInt(0, 500) / 100.0);
        double right = 0.1 + (data.consumeInt(0, 500) / 100.0);
        double min = c - left;
        double max = c + right;

        int scaleInt = data.consumeInt(-20, 20);
        if (scaleInt == 0) {
            scaleInt = 1;
        }
        double scale = scaleInt;

        double initA = chooseBoundaryOrOutsideInitial(data, min, max);
        double initB = chooseBoundaryOrOutsideInitial(data, min, max);

        PolynomialFunction base = new PolynomialFunction(new double[] { -c, 1.0 });
        PolynomialFunction scaled = new PolynomialFunction(new double[] { -scale * c, scale });

        Double rootA = callExplicitSolve(base, min, max, initA);
        Double rootB = callExplicitSolve(scaled, min, max, initB);

        if (rootA == null || rootB == null) {
            return;
        }

        double tol = 1.0e-6;
        /*
         * Sound oracle:
         * For any non-zero scalar k, f(x) and k*f(x) have exactly the same zero set.
         * Both calls use the real public BisectionSolver API on valid-by-construction brackets
         * around the unique root x = c. A patch that merely suppresses the seed NPE but routes
         * the explicit-function overload through the wrong function/state can make these two
         * equivalent problems disagree.
         */
        if (Math.abs(rootA - rootB) > tol) {
            throw new RuntimeException(
                "[oracle:linear-scale-boundary] metamorphic violation: scalar-multiple functions must yield the same root"
                    + " c=" + c
                    + " scale=" + scale
                    + " min=" + min
                    + " max=" + max
                    + " initA=" + initA
                    + " initB=" + initB
                    + " lhs=" + rootA
                    + " rhs=" + rootB);
        }
    }

    private static double chooseBoundaryOrOutsideInitial(FuzzedDataProvider data, double min, double max) {
        double span = max - min;
        double delta = 0.1 + (data.consumeInt(0, 200) / 100.0);
        switch (data.consumeInt(0, 5)) {
            case 0:
                return min;
            case 1:
                return max;
            case 2:
                return (min + max) / 2.0;
            case 3:
                return min - delta;
            case 4:
                return max + delta;
            default:
                return min + span * (data.consumeInt(0, 1000) / 1000.0);
        }
    }

    private static Double callExplicitSolve(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        try {
            return solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isRootCauseNpeInSolve(t)) {
                throw (RuntimeException) t;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Convergence") || name.contains("Invalid") || name.contains("NoBracketing")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseNpeInSolve(Throwable t) {
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
}