package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runExactAnchorNestedCheck();
        runExploreNestedBracketCheck(data);
    }

    private static void runExactAnchorNestedCheck() {
        UnivariateRealFunction f = new SinFunction();

        Double outer = callExplicitSolve(f, 3.0, 3.2, 3.1, "anchor-outer");
        Double inner = callExplicitSolve(f, 3.1, 3.2, 3.15, "anchor-inner");

        if (outer == null || inner == null) {
            return;
        }

        double tol = 1.0e-6 * 16.0;
        /* Contract/oracle:
         * Both calls solve the same real function on nested intervals that bracket the same
         * unique root pi. A correct solver must therefore return the same root up to solver
         * accuracy; merely deleting the throw or diverting control flow would violate this.
         */
        if (Math.abs(outer.doubleValue() - inner.doubleValue()) > tol) {
            throw new RuntimeException(
                "[oracle:nested-explicit] metamorphic violation: nested valid brackets around pi disagree"
                    + " outer=" + outer + " inner=" + inner + " tol=" + tol);
        }
    }

    private static void runExploreNestedBracketCheck(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-8, 8);
        double center = k * Math.PI;

        double outerLeft = 0.05 + (data.consumeInt(0, 400) / 1000.0);
        double outerRight = 0.05 + (data.consumeInt(0, 400) / 1000.0);

        double innerLeft = 0.005 + (data.consumeInt(0, 40) / 1000.0);
        double innerRight = 0.005 + (data.consumeInt(0, 40) / 1000.0);

        if (innerLeft >= outerLeft) {
            innerLeft = outerLeft / 2.0;
        }
        if (innerRight >= outerRight) {
            innerRight = outerRight / 2.0;
        }

        double outerMin = center - outerLeft;
        double outerMax = center + outerRight;
        double innerMin = center - innerLeft;
        double innerMax = center + innerRight;

        double outerInitial = chooseInside(data, outerMin, outerMax);
        double innerInitial = chooseInside(data, innerMin, innerMax);

        Double outer = callExplicitSolve(f, outerMin, outerMax, outerInitial, "fuzz-outer");
        Double inner = callExplicitSolve(f, innerMin, innerMax, innerInitial, "fuzz-inner");

        if (outer == null || inner == null) {
            return;
        }

        double tol = 1.0e-6 * 32.0;
        /* Contract/oracle:
         * These two calls are constructed to be valid by construction: same non-null function,
         * each interval brackets the same exact sin root k*pi, and the inner interval is nested
         * inside the outer one. A correct implementation must converge to the same root on both.
         */
        if (Math.abs(outer.doubleValue() - inner.doubleValue()) > tol) {
            throw new RuntimeException(
                "[oracle:nested-explicit] metamorphic violation: nested valid brackets disagree"
                    + " k=" + k
                    + " outer=[" + outerMin + "," + outerMax + "]"
                    + " inner=[" + innerMin + "," + innerMax + "]"
                    + " outerInitial=" + outerInitial
                    + " innerInitial=" + innerInitial
                    + " outerResult=" + outer
                    + " innerResult=" + inner
                    + " tol=" + tol);
        }
    }

    private static Double callExplicitSolve(
            UnivariateRealFunction f, double min, double max, double initial, String label) {
        BisectionSolver solver = new BisectionSolver();
        try {
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:nested-explicit] metamorphic violation: valid explicit solve threw root-cause exception"
                        + " label=" + label
                        + " interval=[" + min + "," + max + "]"
                        + " initial=" + initial,
                    t);
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        }
    }

    private static double chooseInside(FuzzedDataProvider data, double min, double max) {
        int milli = data.consumeInt(0, 1000);
        double t = milli / 1000.0;
        double v = min + (max - min) * t;
        if (v <= min) {
            v = Math.nextUp(min);
        }
        if (v >= max) {
            v = Math.nextAfter(max, min);
        }
        return v;
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("Argument")
                || name.contains("EvaluationException")
                || t instanceof FunctionEvaluationException
                || t instanceof MaxIterationsExceededException;
    }
}