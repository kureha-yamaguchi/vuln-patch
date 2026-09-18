package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exactSeedFirst();

        UnivariateRealFunction f = new SinFunction();

        double left = boundedPositive(data.consumeInt(), 1.0e-6, 1.0);
        double right = boundedPositive(data.consumeInt(), 1.0e-6, 1.0);
        double min = Math.PI - left;
        double max = Math.PI + right;

        double initial = chooseInitial(data, min, max);

        exerciseFreshExplicitSolve(f, min, max, initial);

        if (data.consumeBoolean()) {
            double min2 = Math.PI - boundedPositive(data.consumeInt(), 1.0e-6, 0.25);
            double max2 = Math.PI + boundedPositive(data.consumeInt(), 1.0e-6, 0.25);
            double initial2 = chooseInitial(data, min2, max2);
            exerciseFreshExplicitSolve(f, min2, max2, initial2);
        }
    }

    private static void exactSeedFirst() {
        UnivariateRealFunction f = new SinFunction();
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;
        exerciseFreshExplicitSolve(f, min, max, initial);
    }

    private static void exerciseFreshExplicitSolve(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(f, min, max, initial);

            double acc = solver.getAbsoluteAccuracy();

            if (result < min - acc || result > max + acc) {
                throw new RuntimeException(
                    "[oracle:interval-membership] metamorphic violation: valid bisection result must remain inside the bracketing interval"
                        + " min=" + min + " max=" + max + " result=" + result + " acc=" + acc);
            }

            try {
                double residual = Math.abs(f.value(result));
                /*
                 * For the constructed inputs we always bracket the known simple root pi of sin(x).
                 * A correct bisection solve on this interval must return an approximation whose
                 * residual is small; merely deleting the buggy throw or using the wrong receiver
                 * state can return a point that is still inside the interval but not near a root.
                 */
                double tolerance = Math.max(1.0e-12, acc * 4.0);
                if (residual > tolerance) {
                    throw new RuntimeException(
                        "[oracle:sin-residual] metamorphic violation: explicit solve on a valid bracket around pi returned a point with large residual"
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " result=" + result + " residual=" + residual + " acc=" + acc);
                }
            } catch (FunctionEvaluationException ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeInBisectionSolve(t)) {
                throw new RuntimeException(
                    "[oracle:fresh-explicit-receiver] metamorphic violation: a fresh BisectionSolver must honor the explicit function argument on a valid bracket"
                        + " min=" + min + " max=" + max + " initial=" + initial, t);
            }
        }
    }

    private static boolean isRootCauseNpeInBisectionSolve(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof FunctionEvaluationException
                || t instanceof MaxIterationsExceededException;
    }

    private static double chooseInitial(FuzzedDataProvider data, double min, double max) {
        double mid = 0.5 * (min + max);
        double width = max - min;
        switch (data.consumeInt(0, 6)) {
            case 0:
                return min;
            case 1:
                return max;
            case 2:
                return mid;
            case 3:
                return min + width * 0.01;
            case 4:
                return max - width * 0.01;
            case 5:
                return min - width * 0.25;
            default:
                return max + width * 0.25;
        }
    }

    private static double boundedPositive(int raw, double min, double max) {
        long nonNegative = raw == Integer.MIN_VALUE ? 0L : Math.abs((long) raw);
        double unit = (nonNegative % 1000000L) / 999999.0;
        return min + (max - min) * unit;
    }
}