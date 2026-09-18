package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runExactSeed(f);
        runFuzzedCases(data, f);
    }

    private static void runExactSeed(UnivariateRealFunction f) {
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        BisectionSolver explicitSolver = new BisectionSolver();
        try {
            double r = explicitSolver.solve(f, min, max, initial);
            assertSubBracket("anchor-explicit", f, min, max, r, explicitSolver.getAbsoluteAccuracy());
        } catch (Throwable t) {
            handleExplicitThrowable("anchor-explicit", t, min, max, initial, true);
        }

        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = f;
        try {
            double r2 = storedSolver.solve(min, max);
            assertSubBracket("anchor-stored-twoarg", f, min, max, r2, storedSolver.getAbsoluteAccuracy());
        } catch (Throwable t) {
            handleNonRootThrowable(t);
        }
    }

    private static void runFuzzedCases(FuzzedDataProvider data, UnivariateRealFunction f) {
        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-50, 50);
            double center = (2.0 * k + 1.0) * Math.PI;
            double halfWidth = 0.05 + (data.consumeInt(0, 2000) / 1000.0) * 0.2;
            double min = center - halfWidth;
            double max = center + halfWidth;
            double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);

            BisectionSolver explicitSolver = new BisectionSolver();
            try {
                double r = explicitSolver.solve(f, min, max, initial);
                assertSubBracket("fuzz-explicit", f, min, max, r, explicitSolver.getAbsoluteAccuracy());
            } catch (Throwable t) {
                handleExplicitThrowable("fuzz-explicit", t, min, max, initial, false);
            }

            BisectionSolver storedSolver = new BisectionSolver();
            storedSolver.f = f;
            try {
                double r2 = storedSolver.solve(min, max);
                assertSubBracket("fuzz-stored-twoarg", f, min, max, r2, storedSolver.getAbsoluteAccuracy());
            } catch (Throwable t) {
                handleNonRootThrowable(t);
            }
        }
    }

    private static void assertSubBracket(String tag, UnivariateRealFunction f, double min, double max, double result, double acc)
            throws FunctionEvaluationException {
        double frMin = f.value(min);
        double frMax = f.value(max);
        double fr = f.value(result);

        if (!(result >= min - acc && result <= max + acc)) {
            throw new RuntimeException("[oracle:subbracket-range] result escaped interval tag=" + tag
                    + " min=" + min + " max=" + max + " result=" + result + " acc=" + acc);
        }

        /* For a correct root solver on this valid-by-construction sin interval, the returned approximation
           must identify a root inside [min,max]. Since the interval is centered on an exact odd multiple
           of pi and narrower than pi, one of [min,result] or [result,max] must still bracket the root:
           the endpoint signs must differ, or result itself is close enough to zero. A throw-deleting patch
           can violate this by returning an arbitrary in-interval value. */
        boolean leftBrackets = frMin == 0.0 || fr == 0.0 || frMin * fr <= 0.0;
        boolean rightBrackets = fr == 0.0 || frMax == 0.0 || fr * frMax <= 0.0;
        boolean nearRoot = Math.abs(fr) <= 4.0 * Math.abs(Math.sin(acc));
        if (!(leftBrackets || rightBrackets || nearRoot)) {
            throw new RuntimeException("[oracle:subbracket-sign] metamorphic violation: no sign-preserving subinterval"
                    + " tag=" + tag + " min=" + min + " max=" + max + " result=" + result
                    + " f(min)=" + frMin + " f(result)=" + fr + " f(max)=" + frMax + " acc=" + acc);
        }
    }

    private static void handleExplicitThrowable(String tag, Throwable t, double min, double max, double initial, boolean anchor) {
        if (isRootCauseNpeInSolve(t)) {
            throw new RuntimeException("[oracle:explicit-valid-subbracket] valid explicit solve failed on accepted bracket"
                    + " tag=" + tag + " min=" + min + " max=" + max + " initial=" + initial, t);
        }
        handleNonRootThrowable(t);
    }

    private static void handleNonRootThrowable(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }
        if (t instanceof RuntimeException) {
            return;
        }
        if (t instanceof Error) {
            return;
        }
    }

    private static boolean isRootCauseNpeInSolve(Throwable t) {
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
}