package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static final UnivariateRealFunction SIN = new SinFunction();
    private static final double TWO_PI = 2.0 * Math.PI;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
        runIterationCountConsistency(data);
    }

    private static void runAnchor() {
        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(SIN, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int k = data.consumeInt(-50, 50);
        double root = k * Math.PI;

        int leftMillis = data.consumeInt(1, 1000);
        int rightMillis = data.consumeInt(1, 1000);
        double min = root - (leftMillis / 1000.0);
        double max = root + (rightMillis / 1000.0);

        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            int pos = data.consumeInt(0, 1000);
            initial = min + (max - min) * (pos / 1000.0);
        } else {
            initial = root;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(SIN, min, max, initial);

            if (!(result >= min && result <= max)) {
                throw new RuntimeException(
                    "[oracle:result-inside-interval] metamorphic violation: bisection result left verified interval"
                        + " input=[" + min + "," + max + "] initial=" + initial + " result=" + result);
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runIterationCountConsistency(FuzzedDataProvider data) {
        int k = data.consumeInt(-50, 50);
        double root = k * Math.PI;

        int leftMicros = data.consumeInt(1, 1_000_000);
        int rightMicros = data.consumeInt(1, 1_000_000);
        double min = root - (leftMicros / 1_000_000.0);
        double max = root + (rightMicros / 1_000_000.0);

        if (!(min < max)) {
            return;
        }

        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);

        BisectionSolver solver = new BisectionSolver(SIN);
        try {
            double result = solver.solve(SIN, min, max, initial);

            double width = max - min;
            double acc = solver.getAbsoluteAccuracy();
            int expectedIterations = expectedIterationCount(width, acc);
            int reportedIterations = solver.getIterationCount();

            /*
             * Contract used for this oracle:
             * Bisection halves the interval on every loop and returns once |max-min| <= absoluteAccuracy.
             * Therefore, for a successful solve on a fixed initial interval, the number of iterations is
             * determined solely by the initial width and the solver's own reported absoluteAccuracy.
             * We compute that same quantity independently from the interval width and compare it to the
             * solver's reported iteration count.
             */
            if (reportedIterations != expectedIterations) {
                throw new RuntimeException(
                    "[oracle:iteration-width-law] metamorphic violation: reported iteration count disagrees with"
                        + " independent width/accuracy computation input=[" + min + "," + max + "]"
                        + " initial=" + initial + " result=" + result
                        + " reported=" + reportedIterations + " expected=" + expectedIterations
                        + " width=" + width + " acc=" + acc);
            }
        } catch (Throwable t) {
            handleThrowable(t, false);
        }
    }

    private static int expectedIterationCount(double width, double accuracy) {
        if (!(width > 0.0) || !(accuracy > 0.0)) {
            return 0;
        }
        if (width <= accuracy) {
            return 0;
        }
        int halvings = 0;
        double w = width;
        while (w > accuracy && halvings < 10_000) {
            w *= 0.5;
            halvings++;
        }
        return Math.max(0, halvings - 1);
    }

    private static void handleThrowable(Throwable t, boolean allowRootCauseConversion) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (allowRootCauseConversion && isRootCauseNpe(t)) {
            throw new RuntimeException(
                "[oracle:explicit-arg-state-independence] metamorphic violation: solve(f,min,max,initial)"
                    + " received a non-null explicit function and a valid bracketing interval for sin(x),"
                    + " but crashed from BisectionSolver.solve instead of using the explicit function"
                    + " argument. This call must not depend on unstored receiver state.",
                t);
        }

        if (isCleanRejection(t)) {
            return;
        }

        return;
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Convergence")
                || name.contains("Evaluation")
                || name.contains("MaxIterations")
                || name.contains("Invalid")
                || name.contains("NoBracketing")) {
                return true;
            }
        }
        return false;
    }
}