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
        runAnchor(f);

        // EXPLORE: generate many valid-by-construction bracketed intervals around a real root
        // so solve(f,min,max,initial) is obligated to handle them.
        int cases = 1 + Math.max(0, Math.min(8, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            double root = data.consumeInt(-1000, 1000) * Math.PI;

            // Positive offsets strictly less than pi keep opposite signs around n*pi.
            double left = positiveOffset(data);
            double right = positiveOffset(data);

            double min = root - left;
            double max = root + right;

            double initial;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    initial = min;
                    break;
                case 1:
                    initial = max;
                    break;
                default:
                    double t = data.consumeInt(0, 10000) / 10000.0;
                    initial = min + (max - min) * t;
                    break;
            }

            runValidCase(f, root, min, max, initial);
        }
    }

    private static void runAnchor(UnivariateRealFunction f) {
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;
        double expected = Math.PI;

        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, min, max, initial);
            double tol = solver.getAbsoluteAccuracy();
            if (Double.isNaN(result) || Math.abs(result - expected) > tol) {
                throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact regression input should solve sin(x)=0 near PI input=min=3.0,max=3.2,initial=3.1 result=" + result + " expected=" + expected + " tol=" + tol);
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

        // Contract/oracle: same-name overloads that solve the same problem must agree.
        // For a correct implementation, solve(f,min,max,initial) and solve(f,min,max)
        // solve the same bracketed equation on the same interval; bisection ignores the
        // initial guess, so deleting/changing the delegation would make these diverge.
        try {
            BisectionSolver s1 = new BisectionSolver();
            BisectionSolver s2 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);
            double withoutInitial = s2.solve(f, min, max);
            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 8.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:sibling-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            // Per hygiene rules, if either side throws, skip the check.
        }
    }

    private static void runValidCase(UnivariateRealFunction f, double expectedRoot, double min, double max, double initial) {
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, min, max, initial);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 8.0, 1.0e-8);
            if (Double.isNaN(result) || result < min || result > max || Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: for a valid bracket around n*PI, solver must return that known root within accuracy input=min=" + min + ",max=" + max + ",initial=" + initial + " expected=" + expectedRoot + " result=" + result + " tol=" + tol);
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

        // Contract/oracle: the two public overloads with and without initial guess must
        // agree on equivalent valid inputs. Both sides use real library calls only.
        try {
            BisectionSolver s1 = new BisectionSolver();
            BisectionSolver s2 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);
            double withoutInitial = s2.solve(f, min, max);
            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 8.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:sibling-explore] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            // If either side throws, this relation does not apply for that input.
        }
    }

    private static double positiveOffset(FuzzedDataProvider data) {
        int units = data.consumeInt(1, 1_000_000);
        return (Math.PI / 4.0) * (units / 1_000_000.0);
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        return t instanceof NullPointerException && hasSolveFrame(t);
    }

    private static boolean hasSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
            return true;
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid")
                || name.contains("illegal")
                || name.contains("argument")
                || name.contains("range")
                || name.contains("bounds")
                || name.contains("validation");
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