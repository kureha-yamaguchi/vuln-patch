package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchor = anchorSolver.solve(sin, 3.0, 3.2, 3.1);
            double expected = Math.PI;
            double tol = Math.max(anchorSolver.getAbsoluteAccuracy(), 1.0e-12);
            if (Math.abs(anchor - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:anchor-return] metamorphic violation: exact regression input should solve sin near pi"
                        + " result=" + anchor + " expected=" + expected + " tol=" + tol);
            }
        } catch (Throwable t) {
            boolean rootCause = t instanceof NullPointerException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                        && "solve".equals(e.getMethodName())) {
                        throw (NullPointerException) t;
                    }
                }
            }
        }

        int k = data.consumeInt(-50, 50);
        double root = k * Math.PI;
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double min = root - left;
        double max = root + right;
        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);

        if (!(min < max) || !(initial >= min && initial <= max)) {
            return;
        }

        Double explicitResult = null;
        Integer explicitIterations = null;
        double explicitAccuracy = 0.0;
        try {
            BisectionSolver explicitSolver = new BisectionSolver();
            explicitResult = new Double(explicitSolver.solve(sin, min, max, initial));
            explicitIterations = new Integer(explicitSolver.getIterationCount());
            explicitAccuracy = explicitSolver.getAbsoluteAccuracy();
        } catch (Throwable t) {
            boolean rootCause = t instanceof NullPointerException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                        && "solve".equals(e.getMethodName())) {
                        throw (NullPointerException) t;
                    }
                }
            }
            return;
        }

        Double twoArgA = null;
        Integer twoArgAIters = null;
        double twoArgAAccuracy = 0.0;
        try {
            BisectionSolver storedA = new BisectionSolver(sin);
            twoArgA = new Double(storedA.solve(min, max));
            twoArgAIters = new Integer(storedA.getIterationCount());
            twoArgAAccuracy = storedA.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        Double twoArgB = null;
        Integer twoArgBIters = null;
        try {
            BisectionSolver storedB = new BisectionSolver(sin);
            twoArgB = new Double(storedB.solve(min, max));
            twoArgBIters = new Integer(storedB.getIterationCount());
        } catch (Throwable t) {
            return;
        }

        /*
         * Correctness guarantee used here:
         * Bisection on the same continuous function, same interval and same configured solver
         * is deterministic; the 4-arg overload should agree with the equivalent stored-function
         * 2-arg overload because the "initial" point is not part of the bisection recurrence.
         * A patch that merely suppresses the NPE but routes through the wrong state/function
         * can still return a mismatching root or bookkeeping, so we cross-check the same quantity
         * via two independent real library executions.
         */
        double tol = Math.max(Math.max(explicitAccuracy, twoArgAAccuracy), 1.0e-12);
        if (Math.abs(explicitResult.doubleValue() - twoArgA.doubleValue()) > tol) {
            throw new RuntimeException(
                "[oracle:explicit-vs-stored2arg-result] metamorphic violation: equivalent solve overloads disagree"
                    + " min=" + min + " max=" + max + " initial=" + initial
                    + " explicit=" + explicitResult.doubleValue()
                    + " stored2arg=" + twoArgA.doubleValue()
                    + " tol=" + tol);
        }

        if (!twoArgA.equals(twoArgB) || !twoArgAIters.equals(twoArgBIters)) {
            throw new RuntimeException(
                "[oracle:twoarg-replay-determinism] consistency violation: identical stored-function solvers disagree"
                    + " min=" + min + " max=" + max
                    + " r1=" + twoArgA + " r2=" + twoArgB
                    + " it1=" + twoArgAIters + " it2=" + twoArgBIters);
        }

        if (!explicitIterations.equals(twoArgAIters)) {
            throw new RuntimeException(
                "[oracle:iteration-replay] consistency violation: equivalent bisection executions should take the same iterations"
                    + " min=" + min + " max=" + max + " initial=" + initial
                    + " explicitIterations=" + explicitIterations
                    + " stored2argIterations=" + twoArgAIters);
        }
    }
}