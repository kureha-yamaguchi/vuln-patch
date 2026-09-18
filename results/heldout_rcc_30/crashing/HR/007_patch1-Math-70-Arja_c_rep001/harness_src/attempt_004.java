package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-1000, 1000);
        double width = 0.05 + data.consumeInt(0, 899) / 1000.0;
        double frac = data.consumeInt(0, 9999) / 10000.0;
        runConstructedCase(k, width, frac);

        int extraCases = data.consumeInt(0, 3);
        for (int i = 0; i < extraCases; i++) {
            int kk = data.consumeInt(-1000, 1000);
            double ww = 0.05 + data.consumeInt(0, 899) / 1000.0;
            double ff = data.consumeInt(0, 9999) / 10000.0;
            runConstructedCase(kk, ww, ff);
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double r = solver.solve(f, 3.0, 3.2, 3.1);
            checkSolvedState(solver, r, "anchor-4arg");
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runConstructedCase(int k, double width, double frac) {
        UnivariateRealFunction f = new SinFunction();
        double root = k * Math.PI;
        double min = root - width;
        double max = root + width;
        double initial = min + (max - min) * frac;

        BisectionSolver solver4 = new BisectionSolver();
        try {
            double r4 = solver4.solve(f, min, max, initial);
            checkSolvedState(solver4, r4, "state-4arg");
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        BisectionSolver solver2 = new BisectionSolver(f);
        try {
            double r2 = solver2.solve(min, max);
            checkSolvedState(solver2, r2, "state-2arg");
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void checkSolvedState(BisectionSolver solver, double returned, String oracleId)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        double stored;
        try {
            stored = solver.getResult();
        } catch (RuntimeException t) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: successful solve must store an observable result; getResult threw " + t.getClass().getName(), t);
        }

        if (Double.doubleToLongBits(stored) != Double.doubleToLongBits(returned)) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: successful solve must return the same value it stores in solver state lhs=" + returned + " rhs=" + stored);
        }

        int iterations;
        try {
            iterations = solver.getIterationCount();
        } catch (RuntimeException t) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: successful solve must store an observable iteration count; getIterationCount threw " + t.getClass().getName(), t);
        }

        if (iterations < 0) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: successful solve must record a non-negative iteration count count=" + iterations);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}