package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        trySolveAndMaybePropagate(new SinFunction(), 3.0, 3.2, 3.1, true);

        // EXPLORE: same root cause property with varied, valid-by-construction inputs.
        UnivariateRealFunction f;
        int which = data.consumeInt(0, 1);
        if (which == 0) {
            f = new SinFunction();

            int k = data.consumeInt(-50, 50);
            double root = k * Math.PI;

            double left = boundedPositive(data.consumeInt(1, 1000) / 1000.0);
            double right = boundedPositive(data.consumeInt(1, 1000) / 1000.0);

            double min = root - left;
            double max = root + right;
            double initial = chooseInitial(data, min, max);

            trySolveAndMaybePropagate(f, min, max, initial, true);
            siblingAgreementOracle(f, min, max, initial);
        } else {
            f = new QuinticFunction();

            double left = boundedPositive(data.consumeInt(1, 1000) / 1000.0);
            double right = boundedPositive(data.consumeInt(1, 1000) / 1000.0);

            double min = -left;
            double max = right;
            double initial = chooseInitial(data, min, max);

            trySolveAndMaybePropagate(f, min, max, initial, true);
            siblingAgreementOracle(f, min, max, initial);
        }
    }

    private static double boundedPositive(double x) {
        if (x <= 0.0) {
            return 0.001;
        }
        if (x > 1.0) {
            return 1.0;
        }
        return x;
    }

    private static double chooseInitial(FuzzedDataProvider data, double min, double max) {
        if (data.consumeBoolean()) {
            return min;
        }
        if (data.consumeBoolean()) {
            return max;
        }
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        return min + (max - min) * fraction;
    }

    private static void trySolveAndMaybePropagate(
            UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (validByConstruction && isRootCauseNpeFromSolve(e)) {
                throw e;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }
    }

    private static boolean isRootCauseNpeFromSolve(Throwable t) {
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

    private static void siblingAgreementOracle(
            UnivariateRealFunction f, double min, double max, double initial) {
        double withInitial;
        double withoutInitial;
        double acc;

        try {
            BisectionSolver s1 = new BisectionSolver();
            withInitial = s1.solve(f, min, max, initial);
            acc = s1.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver s2 = new BisectionSolver();
            withoutInitial = s2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)) {
            return;
        }

        // Documented overload-agreement oracle:
        // the overload solve(f, min, max, initial) should behave like solve(f, min, max)
        // for BisectionSolver because bisection does not use the initial guess; a "fix"
        // that merely suppresses the crash but returns a different result would violate this.
        double tolerance = Math.max(acc * 2.0, 1.0e-12);
        if (Math.abs(withInitial - withoutInitial) > tolerance) {
            throw new RuntimeException(
                    "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max)"
                            + " input=min=" + min
                            + ", max=" + max
                            + ", initial=" + initial
                            + ", function=" + f.getClass().getName()
                            + " lhs=" + withInitial
                            + ", rhs=" + withoutInitial);
        }
    }
}