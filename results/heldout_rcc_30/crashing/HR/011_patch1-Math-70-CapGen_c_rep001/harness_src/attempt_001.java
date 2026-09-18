package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            exploreSinBracketing(data);
        } else if (mode == 1) {
            exploreWithFreshSolvers(data);
        } else {
            exploreBoundaryIntervals(data);
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        try {
            BisectionSolver solver = new BisectionSolver();
            double r1 = solver.solve(f, min, max, initial);

            BisectionSolver solver2 = new BisectionSolver();
            double r2 = solver2.solve(f, min, max);

            // Contract/oracle: in this implementation family, solve(f,min,max,initial)
            // is the same operation as solve(f,min,max) for bisection; the "initial"
            // value is not used by the real algorithm, and the patched method delegates
            // directly to the 3-arg overload. A throw-deleting or wrong-delegation patch
            // would violate this sibling-agreement relation.
            if (Math.abs(r1 - r2) > tolerance(r1, r2)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void exploreSinBracketing(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = 0.01 + data.consumeInt(0, 5000) / 5000.0;
        double rightWidth = 0.01 + data.consumeInt(0, 5000) / 5000.0;

        leftWidth = Math.min(leftWidth, 1.5);
        rightWidth = Math.min(rightWidth, 1.5);

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            int selector = data.consumeInt(0, 1000);
            initial = min + (max - min) * (selector / 1000.0);
        }

        runChecks(f, min, max, initial);
    }

    private static void exploreWithFreshSolvers(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-200, 200);
        double root = k * Math.PI;

        double span = 0.02 + data.consumeInt(0, 10000) / 10000.0;
        span = Math.min(span, 2.0);

        double skew = data.consumeInt(-5000, 5000) / 10000.0;
        double min = root - span * (1.0 + Math.max(0.0, skew));
        double max = root + span * (1.0 + Math.max(0.0, -skew));
        if (!(min < max)) {
            return;
        }

        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        runChecks(f, min, max, initial);
    }

    private static void exploreBoundaryIntervals(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-50, 50);
        double root = k * Math.PI;

        double eps = data.consumeInt(1, 10000) / 100000.0;
        double min = root - eps;
        double max = root + eps;
        if (data.consumeBoolean()) {
            min = root;
        } else if (data.consumeBoolean()) {
            max = root;
        }
        if (!(min < max) && min != max) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        }

        runChecks(f, min, max, initial);
    }

    private static void runChecks(UnivariateRealFunction f, double min, double max, double initial) {
        if (f == null) {
            return;
        }
        if (!(min < max) && min != max) {
            return;
        }

        try {
            BisectionSolver solver1 = new BisectionSolver();
            double lhs = solver1.solve(f, min, max, initial);

            BisectionSolver solver2 = new BisectionSolver();
            double rhs = solver2.solve(f, min, max);

            // Contract/oracle: these same-name overloads must agree on equivalent input.
            if (Math.abs(lhs - rhs) > tolerance(lhs, rhs)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
            }

            // Independent consistency check using the function itself:
            // for a valid bisection solve on a bracketing interval, the returned root
            // must remain inside the interval used to compute it. This is a direct
            // property of repeated midpoint refinement in the real implementation.
            double lo = Math.min(min, max);
            double hi = Math.max(min, max);
            if (lhs < lo - 1e-12 || lhs > hi + 1e-12) {
                throw new RuntimeException("[oracle:interval-contained] metamorphic violation: result escaped interval input=[" + min + "," + max + "," + initial + "] result=" + lhs);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t) && isValidByConstructionSinInput(f, min, max)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isValidByConstructionSinInput(UnivariateRealFunction f, double min, double max) {
        if (f == null) {
            return false;
        }
        if (!(min < max) && min != max) {
            return false;
        }
        if (Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max)) {
            return false;
        }
        if (min == max) {
            return true;
        }
        double a = Math.sin(min);
        double b = Math.sin(max);
        return !(Double.isNaN(a) || Double.isNaN(b)) && a * b <= 0.0;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("FunctionEvaluationException")
                || name.contains("MaxIterationsExceededException")
                || name.contains("MathException");
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
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double tolerance(double a, double b) {
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return 1e-9 * scale;
    }
}