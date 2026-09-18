package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE:
        // Root cause property: the patched overload solve(f, min, max, initial) must forward the
        // provided non-null function into the real solving path. The buggy version instead calls
        // solve(min, max), which uses internal state and crashes with NPE on a fresh solver.
        //
        // Build valid-by-construction intervals around known roots of sin(x) = 0 at k*pi.
        // For delta in (0, pi), sin(root - delta) and sin(root + delta) have opposite signs,
        // so the solver is obligated to handle these inputs.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        int deltaMicros = data.consumeInt(1, 1000000);
        double delta = 1.0e-6 + (deltaMicros / 1000000.0); // in (1e-6, 1.000001)

        double min = root - delta;
        double max = root + delta;

        int pos = data.consumeInt(0, 1000000);
        double initial = min + (max - min) * (pos / 1000000.0);

        runCase(f, min, max, initial, false);
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean checkKnownRoot) {
        // First drive the exact patched entry point.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isOracleFailure(t) || isRootCauseNpeFromPatchedRegion(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Metamorphic / post-condition check:
        // The documented same-name overloads are required to agree on equivalent inputs.
        // For a correct implementation, solve(f, min, max, initial) and solve(f, min, max)
        // must produce the same root on the same valid bracket; deleting the forward to the
        // real overload or otherwise changing behavior would violate this observable contract.
        double withInitial;
        double withoutInitial;
        try {
            BisectionSolver s1 = new BisectionSolver();
            withInitial = s1.solve(f, min, max, initial);

            BisectionSolver s2 = new BisectionSolver();
            withoutInitial = s2.solve(f, min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t) || isRootCauseNpeFromPatchedRegion(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-8) * 4.0;
        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) || Math.abs(withInitial - withoutInitial) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }

        if (checkKnownRoot) {
            double expected = Math.PI;
            if (Double.isNaN(withInitial) || Math.abs(withInitial - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:known-root] metamorphic violation: sin root near pi must be solved accurately"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + withInitial + " rhs=" + expected);
            }
        }
    }

    private static boolean isRootCauseNpeFromPatchedRegion(Throwable t) {
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
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid")
                || name.contains("argument")
                || name.contains("bounds")
                || name.contains("range")
                || name.contains("convergence");
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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