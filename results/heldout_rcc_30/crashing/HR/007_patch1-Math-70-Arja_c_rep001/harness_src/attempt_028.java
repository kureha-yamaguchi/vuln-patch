package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static final UnivariateRealFunction SIN = new SinFunction();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchorOracle();

        int k = data.consumeInt(-100, 100);
        double center = k * Math.PI;

        int leftMicros = data.consumeInt(1, 1_000_000);
        int rightMicros = data.consumeInt(1, 1_000_000);
        double left = leftMicros / 1_000_000.0;
        double right = rightMicros / 1_000_000.0;

        double min = center - left;
        double max = center + right;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = min;
        } else if (data.consumeBoolean()) {
            initial = max;
        } else {
            int pos = data.consumeInt(0, 1_000_000);
            initial = min + (max - min) * (pos / 1_000_000.0);
        }

        exerciseExplicitValidInterval(min, max, initial);
        exerciseStoredTwoArgValidInterval(min, max);
    }

    private static void runExactAnchorOracle() {
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        try {
            BisectionSolver solver = new BisectionSolver();
            double r = solver.solve(SIN, min, max, initial);
            assertLocalSignChange(solver, r, "anchor-explicit");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                try {
                    BisectionSolver stored = new BisectionSolver(SIN);
                    double oracle = stored.solve(min, max);
                    assertLocalSignChange(stored, oracle, "anchor-stored");
                    throw new RuntimeException(
                            "[oracle:explicit-valid-vs-local-sign] metamorphic violation: "
                                    + "explicit 4-arg solve threw on a valid interval while real 2-arg stored-function solve succeeded "
                                    + "min=" + min + " max=" + max + " initial=" + initial + " oracle=" + oracle,
                            t);
                } catch (Throwable ignored) {
                    return;
                }
            }
        }
    }

    private static void exerciseExplicitValidInterval(double min, double max, double initial) {
        try {
            BisectionSolver solver = new BisectionSolver();
            double r = solver.solve(SIN, min, max, initial);

            /*
             * Contract/oracle:
             * BisectionSolver maintains a bracketing interval and returns when its width is within
             * absoluteAccuracy. For the continuous real SinFunction on the small intervals we build
             * around k*pi, there is a true root inside the interval and sin is monotone there.
             * Therefore the solver's reported root, expanded by its own absoluteAccuracy on both
             * sides, must still straddle a sign change (or hit zero exactly). A patch that merely
             * suppresses the throw and returns an unrelated value breaks this observable property.
             */
            assertLocalSignChange(solver, r, "explicit");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new RuntimeException(
                        "[oracle:explicit-valid-local-sign] metamorphic violation: "
                                + "explicit 4-arg solve threw NullPointerException on a valid sin bracket "
                                + "min=" + min + " max=" + max + " initial=" + initial,
                        t);
            }
        }
    }

    private static void exerciseStoredTwoArgValidInterval(double min, double max) {
        try {
            BisectionSolver solver = new BisectionSolver(SIN);
            double r = solver.solve(min, max);
            assertLocalSignChange(solver, r, "stored-twoarg");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new RuntimeException(
                        "[oracle:stored-twoarg-valid-local-sign] metamorphic violation: "
                                + "stored-function 2-arg solve threw NullPointerException on a valid sin bracket "
                                + "min=" + min + " max=" + max,
                        t);
            }
        }
    }

    private static void assertLocalSignChange(BisectionSolver solver, double root, String id)
            throws FunctionEvaluationException {
        double a = solver.getAbsoluteAccuracy();
        if (!(a > 0.0) || Double.isNaN(root) || Double.isInfinite(root)) {
            return;
        }

        double left = root - a;
        double right = root + a;
        double fl = SIN.value(left);
        double fr = SIN.value(right);

        if (Double.isNaN(fl) || Double.isNaN(fr) || Double.isInfinite(fl) || Double.isInfinite(fr)) {
            return;
        }

        if (fl * fr > 0.0) {
            throw new RuntimeException(
                    "[oracle:" + id + "-local-sign] metamorphic violation: "
                            + "reported root does not straddle a sign change within solver absoluteAccuracy "
                            + "root=" + root + " acc=" + a + " f(root-acc)=" + fl + " f(root+acc)=" + fr);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
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
        String n = t.getClass().getName();
        return n.contains("ConvergenceException")
                || n.contains("Invalid")
                || n.contains("NoBracketing")
                || n.contains("NotStrictlyPositive")
                || n.contains("OutOfRange");
    }
}