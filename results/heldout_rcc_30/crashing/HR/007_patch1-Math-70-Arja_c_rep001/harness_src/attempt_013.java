package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int cases = data.consumeInt(1, 4);
        for (int i = 0; i < cases; i++) {
            double[] interval = buildValidSinBracket(data);
            double min = interval[0];
            double max = interval[1];

            double initial;
            if (data.consumeBoolean()) {
                initial = min + (max - min) * 0.5;
            } else {
                double shift = (0.1 + data.consumeInt(0, 500) / 100.0) * (data.consumeBoolean() ? 1.0 : -1.0);
                initial = min + shift;
            }

            runExplicitVsStoredIterationConsistency(min, max, initial);
        }
    }

    private static void anchor() {
        runExplicitVsStoredIterationConsistency(3.0, 3.2, 3.1);
    }

    private static double[] buildValidSinBracket(FuzzedDataProvider data) {
        int k = data.consumeInt(-50, 50);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 90) / 100.0);
        double right = 0.01 + (data.consumeInt(0, 90) / 100.0);

        double min = root - left;
        double max = root + right;
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }
        return new double[] { min, max };
    }

    private static void runExplicitVsStoredIterationConsistency(double min, double max, double initial) {
        UnivariateRealFunction f = new SinFunction();

        BisectionSolver storedSolver = new BisectionSolver(f);
        double expectedResult;
        int expectedIterations;
        try {
            expectedResult = storedSolver.solve(min, max);
            expectedIterations = storedSolver.getIterationCount();
        } catch (Throwable t) {
            return;
        }

        BisectionSolver explicitSolver = new BisectionSolver();
        double actualResult;
        int actualIterations;
        try {
            actualResult = explicitSolver.solve(f, min, max, initial);
            actualIterations = explicitSolver.getIterationCount();
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throw new RuntimeException(
                    "[oracle:iter-explicit-stored] explicit-function overload must behave like the stored-function solve on the same valid bracket; explicit solve threw root-cause exception for min="
                        + min + " max=" + max + " initial=" + initial,
                    t);
            }
            return;
        }

        double tol = Math.max(storedSolver.getAbsoluteAccuracy(), explicitSolver.getAbsoluteAccuracy()) * 2.0;

        if (Math.abs(actualResult - expectedResult) > tol) {
            throw new RuntimeException(
                "[oracle:iter-explicit-stored] metamorphic violation: explicit-function solve and stored-function solve disagree on the same valid bracket"
                    + " input=[" + min + "," + max + "] initial=" + initial
                    + " lhs=" + actualResult + " rhs=" + expectedResult);
        }

        /*
         * Contract rationale: in this class the explicit-function overload is supposed to delegate
         * to the same real bisection implementation as the stored-function overload, differing only
         * in where the function comes from. Therefore, on the same function and interval, both the
         * returned root and the reported iteration count must agree. A throw-deleting or stateful
         * band-aid can preserve the root while leaving bookkeeping wrong, so we check iteration count.
         */
        if (actualIterations != expectedIterations) {
            throw new RuntimeException(
                "[oracle:iter-explicit-stored] metamorphic violation: iteration count differs between equivalent solves"
                    + " input=[" + min + "," + max + "] initial=" + initial
                    + " lhs=" + actualIterations + " rhs=" + expectedIterations
                    + " resultL=" + actualResult + " resultR=" + expectedResult);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}