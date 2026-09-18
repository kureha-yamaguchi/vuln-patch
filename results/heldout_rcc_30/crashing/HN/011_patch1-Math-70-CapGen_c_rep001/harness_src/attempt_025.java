package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(3.0d, 3.2d, 3.1d, Math.PI);

        int scenarios = 1;
        if (data.remainingBytes() > 0) {
            scenarios += data.consumeInt(0, 3);
        }

        for (int i = 0; i < scenarios; i++) {
            int k = data.consumeInt(-100, 100);
            double expectedRoot = k * Math.PI;

            double left = data.consumeInt(1, 1000) / 1000.0d;
            double right = data.consumeInt(1, 1000) / 1000.0d;

            double min = expectedRoot - left;
            double max = expectedRoot + right;

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0d);

            runScenario(min, max, initial, expectedRoot);
        }
    }

    private static void runScenario(double min, double max, double initial, double expectedRoot) {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        double result;

        try {
            result = solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(1.0e-6d, solver.getAbsoluteAccuracy() * 8.0d);

        /* Contract/oracle: this solver computes a zero of the supplied function on the interval.
           Here the interval is built around the known zero k*pi of the real library SinFunction,
           so a correct implementation must return approximately that root. A patch that merely
           suppresses the buggy path or ignores the supplied function can return the wrong value
           without throwing, which this oracle catches. */
        if (!Double.isNaN(result) && !Double.isInfinite(result)) {
            if (Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: expected constructed sin root input=min=" + min + ",max=" + max + ",initial=" + initial + " result=" + result + " expected=" + expectedRoot + " tol=" + tol);
            }
        }

        /* Same-name overload agreement: for the same function and interval, solve(f,min,max,initial)
           and solve(f,min,max) are documented sibling entry points to the same computation and must
           agree on the root when both succeed. If either call throws, this check does not apply. */
        try {
            BisectionSolver sibling = new BisectionSolver();
            double siblingResult = sibling.solve(f, min, max);
            double siblingTol = Math.max(tol, sibling.getAbsoluteAccuracy() * 8.0d);
            if (!Double.isNaN(result) && !Double.isInfinite(result)
                    && !Double.isNaN(siblingResult) && !Double.isInfinite(siblingResult)
                    && Math.abs(result - siblingResult) > siblingTol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + result + " rhs=" + siblingResult + " tol=" + siblingTol);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
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
}