package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            runStoredFunctionOverrideConsistency(data);
        }
    }

    private static void anchor() {
        try {
            UnivariateRealFunction f = new SinFunction();
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isKnownRootCause(t)) {
                return;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runStoredFunctionOverrideConsistency(FuzzedDataProvider data) {
        double target = Math.PI + (data.consumeInt(-12000, 12000) / 100000.0);
        if (target < 3.01) {
            target = 3.01;
        } else if (target > 3.19) {
            target = 3.19;
        }
        if (Math.abs(target - Math.PI) < 1.0e-4) {
            target += 0.01;
        }

        double min = 3.0;
        double max = 3.2;
        double initial = 3.0 + (data.consumeInt(0, 20000) / 100000.0);
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        UnivariateRealFunction stored = new SinFunction();
        UnivariateRealFunction parameter = new PolynomialFunction(new double[] { -target, 1.0 });

        double reported;
        try {
            BisectionSolver pollutedReceiver = new BisectionSolver(stored);
            reported = pollutedReceiver.solve(parameter, min, max, initial);
        } catch (Throwable t) {
            if (isKnownRootCause(t) || isCleanRejection(t)) {
                return;
            }
            return;
        }

        double independent;
        try {
            BisectionSolver freshForParameter = new BisectionSolver(parameter);
            independent = freshForParameter.solve(min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = 1.0e-8;
        /* Contract/metamorphic guarantee:
         * solve(UnivariateRealFunction f, ...) must solve for the supplied parameter f.
         * Therefore its result must agree with a fresh solver whose installed function is
         * that same f, even if the receiver was previously constructed with a different one.
         * A band-aid that merely hides the NPE but still consults the stored function breaks this.
         */
        if (Math.abs(reported - independent) > tol) {
            throw new RuntimeException(
                "[oracle:stored-vs-param] metamorphic violation: solve(parameterized-f) disagrees with fresh solver for same f"
                    + " target=" + target
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " lhs=" + reported
                    + " rhs=" + independent);
        }
    }

    private static boolean isKnownRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (StackTraceElement frame : frames) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(frame.getClassName())
                && "solve".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof org.apache.commons.math.ConvergenceException
            || t instanceof org.apache.commons.math.FunctionEvaluationException
            || t instanceof org.apache.commons.math.MaxIterationsExceededException;
    }
}