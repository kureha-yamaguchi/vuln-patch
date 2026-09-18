package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runCase(f, 3.0, 3.2, 3.1, true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int leftMillis = data.consumeInt(1, 1000);
            int rightMillis = data.consumeInt(1, 1000);
            double left = leftMillis / 1000.0;
            double right = rightMillis / 1000.0;

            double min = root - left;
            double max = root + right;

            double initial;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    initial = min;
                    break;
                case 1:
                    initial = max;
                    break;
                case 2:
                    initial = root;
                    break;
                case 3:
                    initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
                    break;
                default:
                    initial = (min + max) / 2.0;
                    break;
            }

            runCase(f, min, max, initial, false);
        }
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean anchor) {
        if (f == null || !(min <= initial && initial <= max) || !(min < max)) {
            return;
        }

        BisectionSolver solver4 = new BisectionSolver();
        try {
            double r4 = solver4.solve(f, min, max, initial);

            BisectionSolver solver3 = new BisectionSolver();
            double r3;
            try {
                r3 = solver3.solve(f, min, max);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }

            double acc = Math.max(solver4.getAbsoluteAccuracy(), solver3.getAbsoluteAccuracy());

            if (!(Double.isNaN(r4) || Double.isInfinite(r4) || Double.isNaN(r3) || Double.isInfinite(r3))) {
                if (Math.abs(r4 - r3) > acc * 4.0) {
                    throw new RuntimeException(
                        "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracketing interval"
                            + " input=min=" + min + ", max=" + max + ", initial=" + initial
                            + " lhs=" + r4 + ", rhs=" + r3);
                }

                try {
                    double fr4 = f.value(r4);
                    double fr3 = f.value(r3);
                    if (!(Double.isNaN(fr4) || Double.isInfinite(fr4) || Double.isNaN(fr3) || Double.isInfinite(fr3))) {
                        if (Math.abs(fr4) > 1e-4 || Math.abs(fr3) > 1e-4) {
                            throw new RuntimeException(
                                "[oracle:root-quality] metamorphic violation: a successful bisection solve on a valid bracketing interval must return an approximation whose function value is near zero"
                                    + " input=min=" + min + ", max=" + max + ", initial=" + initial
                                    + " lhs=" + r4 + " f(lhs)=" + fr4
                                    + " rhs=" + r3 + " f(rhs)=" + fr3);
                        }
                    }
                } catch (FunctionEvaluationException ignored) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (anchor && isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof FunctionEvaluationException
            || t instanceof MaxIterationsExceededException;
    }

    private static boolean isRootCause(Throwable t) {
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
        throw new RuntimeException(t);
    }
}