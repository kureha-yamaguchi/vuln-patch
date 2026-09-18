package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runCase(f, 3.0, 3.2, 3.1, Math.PI);

        int cases = data.remainingBytes() > 0 ? data.consumeInt(1, 4) : 1;
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            // Build valid-by-construction intervals for sin(x): [root-delta, root+delta]
            // with 0 < delta < pi/2, so the interval brackets a real zero at root.
            double delta = (data.consumeInt(1, 1000) / 1000.0) * 1.4 + 1.0e-6;
            double min = root - delta;
            double max = root + delta;

            double initial;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    initial = min;
                    break;
                case 1:
                    initial = max;
                    break;
                case 2:
                    initial = root;
                    break;
                default:
                    double t = data.consumeInt(0, 1000000) / 1000000.0;
                    initial = min + (max - min) * t;
                    break;
            }

            runCase(f, min, max, initial, root);
        }
    }

    private static void runCase(SinFunction f, double min, double max, double initial, double expectedRoot) {
        // Documented guarantee used for the oracle:
        // all solve overloads solve for a zero in [min, max]. For these intervals we construct
        // a real zero of sin(x) inside the bracket by construction, so a correct implementation
        // must return that known root (within solver accuracy). Also, the same-name overloads
        // solve(f,min,max,initial) and solve(f,min,max) must agree on the solved root.
        Double withInitial = null;
        double acc1 = 0.0;
        try {
            BisectionSolver solver = new BisectionSolver();
            withInitial = solver.solve(f, min, max, initial);
            acc1 = solver.getAbsoluteAccuracy();
            if (Double.isNaN(withInitial.doubleValue()) || Double.isInfinite(withInitial.doubleValue())
                    || Math.abs(withInitial.doubleValue() - expectedRoot) > acc1 * 2.0 + 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:known-root] metamorphic violation: known zero in bracket not recovered"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial + ",expectedRoot=" + expectedRoot
                    + " lhs=" + withInitial + ", rhs=" + expectedRoot);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        Double withoutInitial = null;
        double acc2 = 0.0;
        try {
            BisectionSolver solver = new BisectionSolver();
            withoutInitial = solver.solve(f, min, max);
            acc2 = solver.getAbsoluteAccuracy();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (withInitial != null && withoutInitial != null) {
            double tol = Math.max(acc1, acc2) * 2.0 + 1.0e-12;
            if (Double.isNaN(withInitial.doubleValue()) || Double.isNaN(withoutInitial.doubleValue())
                    || Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agree] metamorphic violation: solve overloads disagree"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + ", rhs=" + withoutInitial);
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
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
        return n.contains("Convergence")
                || n.contains("MaxIterationsExceeded")
                || n.contains("FunctionEvaluation")
                || n.contains("Invalid")
                || n.contains("NoBracketing");
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