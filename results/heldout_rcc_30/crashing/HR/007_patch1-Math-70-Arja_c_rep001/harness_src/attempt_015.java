package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorCheck();
        runInitialIndependenceCheck(data);
        runTwoArgCoverage(data);
    }

    private static void runAnchorCheck() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(f, 3.0, 3.2, 3.1);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 4.0, 1.0e-8);
            if (Math.abs(result - Math.PI) > tol) {
                throw new RuntimeException(
                    "[oracle:anchor-pi-result] metamorphic violation: canonical valid explicit solve around pi returned wrong root result="
                        + result + " expected~=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromBisectionSolve(t)) {
                throw new RuntimeException(
                    "[oracle:anchor-valid-explicit] metamorphic violation: valid explicit solve(f,min,max,initial) for sin on [3.0,3.2] threw root-cause NPE",
                    t);
            }
        }
    }

    private static void runInitialIndependenceCheck(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        double leftWidth = scaledUnit(data.consumeInt(1, 1_000_000), 0.001, 0.4);
        double rightWidth = scaledUnit(data.consumeInt(1, 1_000_000), 0.001, 0.4);

        double min = Math.PI - leftWidth;
        double max = Math.PI + rightWidth;

        double initialA = data.consumeBoolean() ? min : max;
        double fraction = scaledUnit(data.consumeInt(0, 1_000_000), 0.0, 1.0);
        double initialB = min + (max - min) * fraction;

        Double resultA = solveExplicitValid(f, min, max, initialA);
        Double resultB = solveExplicitValid(f, min, max, initialB);
        if (resultA == null || resultB == null) {
            return;
        }

        BisectionSolver baselineSolver = new BisectionSolver();
        double baseline;
        try {
            baseline = baselineSolver.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(baselineSolver.getAbsoluteAccuracy() * 8.0, 1.0e-8);

        /*
         * Contract/oracle: for BisectionSolver the 4-arg explicit overload delegates to the same
         * real bisection solve on (f,min,max); the "initial" value is not part of the algorithm's
         * state or stopping condition. Therefore two valid initials in the same interval must
         * produce the same root approximation, and both must agree with the 3-arg explicit solve.
         * A band-aid that merely suppresses the NPE but still consults the wrong receiver state can
         * violate this even when no exception is thrown.
         */
        if (Math.abs(resultA.doubleValue() - resultB.doubleValue()) > tol) {
            throw new RuntimeException(
                "[oracle:initial-independence] metamorphic violation: explicit solve depends on initial "
                    + "min=" + min + " max=" + max
                    + " initialA=" + initialA + " resultA=" + resultA
                    + " initialB=" + initialB + " resultB=" + resultB
                    + " tol=" + tol);
        }
        if (Math.abs(resultA.doubleValue() - baseline) > tol
                || Math.abs(resultB.doubleValue() - baseline) > tol) {
            throw new RuntimeException(
                "[oracle:initial-vs-baseline] metamorphic violation: explicit 4-arg solve disagrees with explicit 3-arg solve "
                    + "min=" + min + " max=" + max
                    + " initialA=" + initialA + " resultA=" + resultA
                    + " initialB=" + initialB + " resultB=" + resultB
                    + " baseline=" + baseline + " tol=" + tol);
        }
    }

    private static void runTwoArgCoverage(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        double leftWidth = scaledUnit(data.consumeInt(1, 1_000_000), 0.001, 0.3);
        double rightWidth = scaledUnit(data.consumeInt(1, 1_000_000), 0.001, 0.3);
        double min = Math.PI - leftWidth;
        double max = Math.PI + rightWidth;

        try {
            BisectionSolver storedSolver = new BisectionSolver(f);
            storedSolver.solve(min, max);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCauseNpeFromBisectionSolve(e)) {
                throw new RuntimeException(
                    "[oracle:twoarg-valid-stored] metamorphic violation: valid stored-function solve(min,max) threw root-cause NPE",
                    e);
            }
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }
    }

    private static Double solveExplicitValid(
            UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        try {
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isRootCauseNpeFromBisectionSolve(t)) {
                throw new RuntimeException(
                    "[oracle:valid-explicit-npe] metamorphic violation: valid explicit solve threw root-cause NPE "
                        + "min=" + min + " max=" + max + " initial=" + initial,
                    t);
            }
            return null;
        }
    }

    private static boolean isRootCauseNpeFromBisectionSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static double scaledUnit(int v, double min, double max) {
        double unit = ((double) v) / 1_000_000.0;
        return min + (max - min) * unit;
    }
}