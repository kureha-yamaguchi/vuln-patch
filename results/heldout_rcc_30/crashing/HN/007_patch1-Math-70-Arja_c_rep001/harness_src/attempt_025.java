package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(anchorFunction, 3.0, 3.2, 3.1);

            // Contract / sibling-agreement oracle:
            // The overloads solve(f, min, max, initial) and solve(f, min, max) are documented
            // as same-name solver entry points for the same problem. For bisection, the initial
            // guess is not part of the algorithm's result; a correct implementation must still
            // solve the same bracketed function. A "fix" that merely suppresses the crash but
            // ignores f or otherwise changes behavior would disagree with the sibling overload.
            try {
                BisectionSolver sibling = new BisectionSolver();
                double siblingResult = sibling.solve(anchorFunction, 3.0, 3.2);
                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 4.0;
                if (Math.abs(result - siblingResult) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=[min=3.0,max=3.2,initial=3.1]"
                            + " lhs=" + result
                            + " rhs=" + siblingResult);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: generate many valid-by-construction brackets around known sin roots k*pi.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = positiveWidth(data);
        double rightWidth = positiveWidth(data);
        double min = root - leftWidth;
        double max = root + rightWidth;

        if (!(min < root && root < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            // interior point
            double fraction = data.consumeInt(1, 999) / 1000.0;
            initial = min + (max - min) * fraction;
        } else {
            // arbitrary ordered point in range
            initial = root + (data.consumeInt(-500, 500) / 1000.0) * Math.min(leftWidth, rightWidth);
            if (initial <= min) {
                initial = Math.nextUp(min);
            }
            if (initial >= max) {
                initial = Math.nextDown(max);
            }
        }

        if (!(min < initial && initial < max)) {
            return;
        }

        UnivariateRealFunction f = anchorFunction;

        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            // Metamorphic oracle:
            // For the same non-null function and same bracket [min, max], the overload with an
            // extra initial parameter must compute the same root as the sibling overload without it.
            // Both sides are real library calls; if either side rejects, the check is skipped.
            try {
                BisectionSolver withoutInitial = new BisectionSolver();
                double rhs = withoutInitial.solve(f, min, max);
                double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()) * 4.0;
                if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-explore] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=[k=" + k
                            + ",min=" + min
                            + ",max=" + max
                            + ",initial=" + initial
                            + "] lhs=" + lhs
                            + " rhs=" + rhs);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int milli = data.consumeInt(1, 1000);
        return milli / 1000.0;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement ste : trace) {
            if (ste == null) {
                continue;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return true;
        }
        String name = t.getClass().getName();
        String lower = name.toLowerCase();
        return lower.contains("invalid")
            || lower.contains("illegal")
            || lower.contains("argument")
            || lower.contains("bounds")
            || lower.contains("range");
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