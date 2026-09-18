package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    private static final double PI = Math.PI;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from the regression test.
        runOneCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: build many valid-by-construction intervals that still bracket pi.
        // These inputs are moderate and satisfy the method's documented non-null f precondition.
        // For sin(x), any interval [pi-left, pi+right] with left,right>0 brackets the known root pi.
        double left = boundedPositive(data.consumeInt(), 1.0e-9, 0.5);
        double right = boundedPositive(data.consumeInt(), 1.0e-9, 0.5);

        double min = PI - left;
        double max = PI + right;
        if (!(min < PI && PI < max)) {
            return;
        }

        // Flip positions of the "initial" hint around the interval boundaries and the root.
        // A correct implementation of BisectionSolver must still solve the same valid problem.
        double[] initials = new double[] {
            min,
            max,
            PI,
            clamp(min + (max - min) * boundedUnit(data.consumeInt()), min, max),
            clamp(min + (max - min) * boundedUnit(data.consumeInt()), min, max)
        };

        for (int i = 0; i < initials.length; i++) {
            runOneCase(f, min, max, initials[i], false);
        }

        // Mandatory metamorphic/post-condition oracle:
        // For the same non-null function and the same bracketing interval, varying the "initial"
        // argument must not change the solved root for BisectionSolver; the algorithm uses the
        // interval [min,max] to converge to the same root. A throw-deleting band-aid could return
        // some unrelated default or stale value instead.
        try {
            BisectionSolver solverA = new BisectionSolver();
            BisectionSolver solverB = new BisectionSolver();

            double initialA = clamp(min + (max - min) * 0.01, min, max);
            double initialB = clamp(max - (max - min) * 0.01, min, max);

            double r1 = solverA.solve(f, min, max, initialA);
            double r2 = solverB.solve(f, min, max, initialB);

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:init-invariant] metamorphic violation: same function and bracket with different initial guesses produced different roots input=[" + min + "," + max + "] lhs=" + r1 + " rhs=" + r2);
            }

            // Independent oracle from the input itself:
            // these intervals are intentionally constructed to bracket the known root pi of sin(x).
            if (Math.abs(r1 - PI) > tol || Math.abs(r2 - PI) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solver failed to recover pi from a valid sin bracket input=[" + min + "," + max + "] r1=" + r1 + " r2=" + r2 + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t) || isCheckedMathException(t)) {
                return;
            }
        }
    }

    private static void runOneCase(UnivariateRealFunction f, double min, double max, double initial, boolean exactAnchor) {
        boolean valid = isValidConstructedSinBracket(f, min, max, initial);
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, min, max, initial);

            // Post-condition / oracle:
            // We deliberately construct a valid bracket around the known root pi of sin(x),
            // so a correct implementation must return approximately pi.
            double tol = solver.getAbsoluteAccuracy() * 4.0;
            if (valid && Math.abs(result - PI) > tol) {
                throw new RuntimeException("[oracle:pi-root] metamorphic violation: valid sin bracket did not solve to pi input=[" + min + "," + max + "," + initial + "] result=" + result + " tol=" + tol + (exactAnchor ? " anchor=true" : ""));
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, valid)) {
                throwUnchecked(t);
            }
            if (t instanceof RuntimeException && isOracleRuntime((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t) || isCheckedMathException(t)) {
                return;
            }
        }
    }

    private static boolean isValidConstructedSinBracket(UnivariateRealFunction f, double min, double max, double initial) {
        if (f == null) {
            return false;
        }
        if (!(min < max)) {
            return false;
        }
        if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(initial)) {
            return false;
        }
        if (Double.isInfinite(min) || Double.isInfinite(max) || Double.isInfinite(initial)) {
            return false;
        }
        if (!(min <= initial && initial <= max)) {
            return false;
        }
        return min < PI && PI < max;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isCheckedMathException(Throwable t) {
        return t instanceof FunctionEvaluationException
                || t instanceof MaxIterationsExceededException;
    }

    private static boolean isOracleRuntime(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static double boundedPositive(int v, double min, double max) {
        long nonNegative = v == Integer.MIN_VALUE ? 0L : Math.abs((long) v);
        double fraction = (nonNegative % 1000000L) / 1000000.0;
        return min + (max - min) * fraction;
    }

    private static double boundedUnit(int v) {
        long nonNegative = v == Integer.MIN_VALUE ? 0L : Math.abs((long) v);
        return (nonNegative % 1000000L) / 1000000.0;
    }

    private static double clamp(double x, double min, double max) {
        if (x < min) {
            return min;
        }
        if (x > max) {
            return max;
        }
        return x;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}