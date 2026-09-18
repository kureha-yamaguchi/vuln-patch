package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        runConstructorVsParameterizedCheck(
                sin,
                3.0d,
                3.2d,
                3.1d,
                "anchor");

        int cases = data.consumeInt(1, 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double left = 0.01d + (data.consumeInt(0, 1500) / 1000.0d);
            double right = 0.01d + (data.consumeInt(0, 1500) / 1000.0d);

            if (left >= (Math.PI / 2.0d)) {
                left = (Math.PI / 2.0d) - 0.01d;
            }
            if (right >= (Math.PI / 2.0d)) {
                right = (Math.PI / 2.0d) - 0.01d;
            }

            double min = root - left;
            double max = root + right;
            double initial;
            if (data.consumeBoolean()) {
                initial = root;
            } else {
                double fraction = data.consumeInt(0, 1000) / 1000.0d;
                initial = min + (max - min) * fraction;
            }

            runConstructorVsParameterizedCheck(
                    sin,
                    min,
                    max,
                    initial,
                    "sin-root-" + i);
        }
    }

    private static void runConstructorVsParameterizedCheck(
            UnivariateRealFunction f, double min, double max, double initial, String tag) {

        if (f == null || !(min < max) || !(initial >= min && initial <= max)) {
            return;
        }

        double ctorResult;
        double ctorStored;
        int ctorIterations;
        double accuracy;

        try {
            BisectionSolver byConstructor = new BisectionSolver(f);
            ctorResult = byConstructor.solve(min, max);
            ctorStored = byConstructor.getResult();
            ctorIterations = byConstructor.getIterationCount();
            accuracy = byConstructor.getAbsoluteAccuracy();

            if (!approximatelyEqual(ctorResult, ctorStored, accuracy)) {
                throw new RuntimeException(
                        "[oracle:stored-result] consistency violation: constructor-path return/result disagree tag="
                                + tag + " min=" + min + " max=" + max + " initial=" + initial
                                + " returned=" + ctorResult + " stored=" + ctorStored
                                + " acc=" + accuracy + " iters=" + ctorIterations);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver byParameter = new BisectionSolver();
            double parameterResult = byParameter.solve(f, min, max, initial);
            double parameterStored = byParameter.getResult();
            int parameterIterations = byParameter.getIterationCount();

            if (!approximatelyEqual(parameterResult, parameterStored, byParameter.getAbsoluteAccuracy())) {
                throw new RuntimeException(
                        "[oracle:stored-result] consistency violation: parameter-path return/result disagree tag="
                                + tag + " min=" + min + " max=" + max + " initial=" + initial
                                + " returned=" + parameterResult + " stored=" + parameterStored
                                + " acc=" + byParameter.getAbsoluteAccuracy()
                                + " iters=" + parameterIterations);
            }

            /*
             * Contract/oracle:
             * The 4-arg overload and the constructor+2-arg path supply the same function and solve
             * the same interval with the same bisection algorithm; the 'initial' value is not used by
             * bisection. Therefore both public API routes must agree on the root for every valid,
             * bracketed interval. A patch that merely suppresses the historical throw but computes or
             * stores the wrong state will violate this equality.
             */
            if (!approximatelyEqual(ctorResult, parameterResult, Math.max(accuracy, byParameter.getAbsoluteAccuracy()))) {
                throw new RuntimeException(
                        "[oracle:init-vs-param] metamorphic violation: equivalent API paths disagree tag="
                                + tag + " min=" + min + " max=" + max + " initial=" + initial
                                + " ctorResult=" + ctorResult + " ctorStored=" + ctorStored
                                + " ctorIters=" + ctorIterations
                                + " paramResult=" + parameterResult + " paramStored=" + parameterStored
                                + " paramIters=" + parameterIterations);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            /*
             * The buggy build throws NPE from BisectionSolver.solve(f,min,max,initial) on valid input.
             * This harness's independent oracle is the disagreement between two equivalent real API
             * paths, so convert only this valid-input failure into an oracle finding instead of
             * re-reporting the already-known raw signature.
             */
            if (isRelevantNullPointer(t)) {
                throw new RuntimeException(
                        "[oracle:init-vs-param] metamorphic violation: parameterized solve threw on valid input while constructor path succeeded tag="
                                + tag + " min=" + min + " max=" + max + " initial=" + initial
                                + " ctorResult=" + ctorResult + " ctorStored=" + ctorStored
                                + " ctorIters=" + ctorIterations, t);
            }
        }
    }

    private static boolean isRelevantNullPointer(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean approximatelyEqual(double a, double b, double accuracy) {
        double tol = Math.max(Math.abs(accuracy) * 4.0d, 1.0e-12d);
        return Math.abs(a - b) <= tol;
    }
}