package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.QuinticFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        exploreValidSinInputs(data);
        runStatefulExplicitFunctionOracle(data);
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        try {
            double r = solver.solve(f, 3.0, 3.2, 3.1);
            if (Math.abs(r - Math.PI) > solver.getAbsoluteAccuracy() * 4.0) {
                throw new RuntimeException("[oracle:anchor-seed] exact regression seed returned wrong root lhs=" + r + " rhs=" + Math.PI);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:anchor-seed] valid sin seed must solve but hit root-cause NPE", t);
            }
        }
    }

    private static void exploreValidSinInputs(FuzzedDataProvider data) {
        int k = data.consumeInt(-32, 32);
        double root = k * Math.PI;

        double leftWidth = 0.01 + (data.consumeInt(0, 400) / 1000.0);
        double rightWidth = 0.01 + (data.consumeInt(0, 400) / 1000.0);
        double min = root - leftWidth;
        double max = root + rightWidth;

        double frac = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * frac;

        BisectionSolver solver = new BisectionSolver();
        try {
            double r = solver.solve(new SinFunction(), min, max, initial);
            if (Math.abs(r - root) > Math.max(1e-8, solver.getAbsoluteAccuracy() * 8.0)) {
                throw new RuntimeException("[oracle:valid-sin-root] constructed bracket around k*pi should recover that root input=" + k + " lhs=" + r + " rhs=" + root);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:valid-sin-root] valid-by-construction sin bracket triggered root-cause failure k=" + k, t);
            }
        }
    }

    private static void runStatefulExplicitFunctionOracle(FuzzedDataProvider data) {
        double qCenter = 0.5;
        double qLeft = 0.01 + (data.consumeInt(0, 90) / 1000.0);
        double qRight = 0.01 + (data.consumeInt(0, 90) / 1000.0);
        double qMin = qCenter - qLeft;
        double qMax = qCenter + qRight;
        double qInitial = qMin + (qMax - qMin) * (data.consumeInt(0, 1000) / 1000.0);

        BisectionSolver expectedSolver = new BisectionSolver(new QuinticFunction());
        BisectionSolver actualSolver = new BisectionSolver(new SinFunction());

        try {
            actualSolver.solve(3.0, 3.2);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                return;
            }
            return;
        }

        double expected;
        double actual;
        try {
            expected = expectedSolver.solve(qMin, qMax);
            actual = actualSolver.solve(new QuinticFunction(), qMin, qMax, qInitial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:explicit-function-wins-after-state] explicit function overload must not depend on unrelated stored function/state", t);
            }
            return;
        }

        double tol = Math.max(expectedSolver.getAbsoluteAccuracy(), actualSolver.getAbsoluteAccuracy()) * 8.0 + 1e-10;
        if (Math.abs(expected - actual) > tol) {
            throw new RuntimeException(
                "[oracle:explicit-function-wins-after-state] solve(f,min,max,initial) must solve the explicit f; " +
                "same function/interval on a solver storing QuinticFunction disagrees after prior state change. " +
                "expected=" + expected + " actual=" + actual + " interval=[" + qMin + "," + qMax + "] initial=" + qInitial);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n != null) {
                String ln = n.toLowerCase();
                if (ln.contains("invalid")
                        || ln.contains("illegalargument")
                        || ln.contains("outofrange")
                        || ln.contains("noacketing")
                        || ln.contains("convergence")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}