package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        checkExplicitFunctionOverridesStoredState(
                linearRoot(0.25),
                linearRoot(0.75),
                0.0,
                1.0,
                0.5,
                "deterministic");

        int storedMilli = data.consumeInt(100, 400);
        int explicitMilli = data.consumeInt(600, 900);
        double storedRoot = storedMilli / 1000.0;
        double explicitRoot = explicitMilli / 1000.0;

        double min = 0.0;
        double max = 1.0;
        int slot = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (slot / 1000.0);
        if (!(min < initial && initial < max)) {
            initial = 0.5;
        }

        checkExplicitFunctionOverridesStoredState(
                linearRoot(storedRoot),
                linearRoot(explicitRoot),
                min,
                max,
                initial,
                "fuzzed");
    }

    private static UnivariateRealFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
        }
    }

    private static void checkExplicitFunctionOverridesStoredState(
            UnivariateRealFunction storedFunction,
            UnivariateRealFunction explicitFunction,
            double min,
            double max,
            double initial,
            String tag) {

        double contaminatedResult;
        double freshResult;

        try {
            BisectionSolver contaminated = new BisectionSolver(storedFunction);
            contaminatedResult = contaminated.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver fresh = new BisectionSolver();
            freshResult = fresh.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        if (Double.isNaN(contaminatedResult) || Double.isNaN(freshResult)
                || Double.isInfinite(contaminatedResult) || Double.isInfinite(freshResult)) {
            return;
        }

        double tolerance = 1.0e-6;
        if (Math.abs(contaminatedResult - freshResult) > tolerance) {
            throw new RuntimeException(
                    "[oracle:explicit-overrides-state] "
                            + "A correct solve(f,min,max,initial) must use the explicit function argument, "
                            + "so solving the same explicit function on the same interval should agree with "
                            + "solve(f,min,max) on a fresh solver regardless of constructor state. "
                            + "tag=" + tag
                            + " min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " contaminated=" + contaminatedResult
                            + " fresh=" + freshResult);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if (e == null) {
                continue;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null
                && (name.indexOf("ConvergenceException") >= 0
                || name.indexOf("FunctionEvaluationException") >= 0
                || name.indexOf("MaxIterationsExceededException") >= 0);
    }
}