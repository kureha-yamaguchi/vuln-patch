package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-8, 8);
        double root = k * Math.PI;

        double width = 0.05 + (data.consumeInt(0, 450) / 1000.0);
        double min = root - width;
        double max = root + width;

        double factor = 0.20 + (data.consumeInt(0, 60) / 100.0);
        if (factor >= 1.0) {
            factor = 0.95;
        }
        double staleRoot = root + (data.consumeBoolean() ? 1.0 : -1.0) * width * factor;
        if (staleRoot <= min || staleRoot >= max) {
            staleRoot = root + (width * 0.5);
            if (staleRoot >= max) {
                staleRoot = root - (width * 0.5);
            }
        }

        double initialFrac = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * initialFrac;

        exerciseCase(min, max, initial, staleRoot);
    }

    private static void anchor() {
        try {
            new BisectionSolver().solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isPatchedRegionNullPointer(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseCase(double min, double max, double initial, double staleRoot) {
        SinFunction explicit = new SinFunction();
        PolynomialFunction stale = new PolynomialFunction(new double[] { -staleRoot, 1.0 });

        try {
            BisectionSolver storedSolver = new BisectionSolver();
            storedSolver.f = stale;
            storedSolver.solve(min, max);
        } catch (Throwable t) {
            if (isOracle(t)) {
                throw (RuntimeException) t;
            }
        }

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.f = stale;
            double result = solver.solve(explicit, min, max, initial);

            if (result < min || result > max) {
                throw new RuntimeException(
                    "[oracle:interval-membership] metamorphic violation: returned root left caller interval"
                        + " min=" + min + " max=" + max + " initial=" + initial + " staleRoot=" + staleRoot
                        + " result=" + result);
            }

            /*
             * Contract used for this oracle: the solver returns an approximation to a root of the
             * function supplied to this call. We build a valid bracketing interval around a known
             * sine root by construction, so a correct implementation must return x with sin(x)
             * close to 0, regardless of any stale function stored on the receiver.
             *
             * This catches a throw-deleting / overfitting patch too: if the method silently uses
             * receiver state instead of the explicit function argument, it can return a root of the
             * stale linear polynomial, where |sin(result)| is not close to 0.
             */
            double residual = Math.abs(explicit.value(result));
            double tolerance = Math.max(1.0e-6, 8.0 * solver.getAbsoluteAccuracy());
            if (residual > tolerance) {
                throw new RuntimeException(
                    "[oracle:explicit-residual] metamorphic violation: returned value does not solve explicit function"
                        + " min=" + min + " max=" + max + " initial=" + initial + " staleRoot=" + staleRoot
                        + " result=" + result + " residual=" + residual + " tolerance=" + tolerance);
            }
        } catch (Throwable t) {
            if (isOracle(t)) {
                throw (RuntimeException) t;
            }
            if (isPatchedRegionNullPointer(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t.getClass().getName().contains("ConvergenceException")
            || t.getClass().getName().contains("FunctionEvaluationException")
            || t.getClass().getName().contains("MaxIterationsExceededException");
    }

    private static boolean isPatchedRegionNullPointer(Throwable t) {
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