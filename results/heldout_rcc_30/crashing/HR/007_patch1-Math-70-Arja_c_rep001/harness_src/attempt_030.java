package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.MonitoredFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchorButDoNotReportDuplicateCrash();
        runDispatchAttributionCheck(data);
    }

    private static void runExactAnchorButDoNotReportDuplicateCrash() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0d, 3.2d, 3.1d);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static void runDispatchAttributionCheck(FuzzedDataProvider data) {
        SinFunction base = new SinFunction();

        int k = data.consumeInt(-3, 3);
        double root = k * Math.PI;
        double width = 0.01d + (data.consumeInt(0, 490) / 1000.0d);
        double min = root - width;
        double max = root + width;

        double initial;
        switch (data.consumeInt(0, 5)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) * 0.5d;
                break;
            case 3:
                initial = min + width / 1024.0d;
                break;
            case 4:
                initial = max - width / 1024.0d;
                break;
            default:
                initial = root;
                break;
        }

        double expected;
        try {
            expected = new BisectionSolver().solve(base, min, max);
        } catch (Throwable t) {
            return;
        }

        MonitoredFunction stored = new MonitoredFunction(base);
        MonitoredFunction explicit = new MonitoredFunction(base);

        BisectionSolver solver = new BisectionSolver();
        solver.f = stored;

        int storedBefore = stored.getCallsCount();
        int explicitBefore = explicit.getCallsCount();

        try {
            double actual = solver.solve(explicit, min, max, initial);

            int storedDelta = stored.getCallsCount() - storedBefore;
            int explicitDelta = explicit.getCallsCount() - explicitBefore;

            /*
             * Contract used by this oracle:
             * the 4-arg overload says its parameter "f" is the function to solve.
             * A correct implementation must therefore evaluate the function object
             * passed as that argument, not some stale receiver field. Bisection's
             * real implementation necessarily evaluates the solved function at least
             * once on any successful call.
             */
            if (explicitDelta <= 0 || storedDelta != 0) {
                throw new RuntimeException(
                    "[oracle:dispatch-attribution] metamorphic violation: 4-arg solve did not use the explicit function parameter"
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " explicitDelta=" + explicitDelta
                    + " storedDelta=" + storedDelta);
            }

            /*
             * Independent oracle:
             * solve(f,min,max,initial) and solve(f,min,max) must agree for bisection
             * on the same valid bracket because the implementation ignores any special
             * role of the initial guess and returns the same root for the same function
             * and interval.
             */
            double tol = Math.max(solver.getAbsoluteAccuracy() * 2.0d, 1.0e-12d);
            if (Math.abs(actual - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:boundary-initial-dispatch] metamorphic violation: 4-arg and 3-arg explicit solves disagree"
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " actual=" + actual
                    + " expected=" + expected
                    + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }
}