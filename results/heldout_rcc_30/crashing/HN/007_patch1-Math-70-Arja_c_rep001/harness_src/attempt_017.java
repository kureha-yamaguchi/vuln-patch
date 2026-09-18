package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runValidCase(f, 3.0d, 3.2d, 3.1d, Math.PI, true);

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double left = 0.05d + (Math.abs(data.consumeInt(-1000, 1000)) / 1000.0d) * 1.0d;
        double right = 0.05d + (Math.abs(data.consumeInt(-1000, 1000)) / 1000.0d) * 1.0d;

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double ratio = Math.abs(data.consumeInt(-1000, 1000)) / 1000.0d;
        double initial = min + (max - min) * ratio;
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        runValidCase(f, min, max, initial, root, false);
    }

    private static void runValidCase(UnivariateRealFunction f, double min, double max, double initial, double expectedRoot, boolean anchor) {
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, min, max, initial);

            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double withoutInitial = solverWithoutInitial.solve(f, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());

            /* Contract/oracle:
             * The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are documented
             * as equivalent solver entry points for the same function and interval; a correct
             * implementation must therefore agree on the located root. A patch that merely avoids
             * the crash but skips using the provided function or otherwise returns the wrong answer
             * will violate this sibling-agreement relation.
             */
            if (Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            /* Oracle from constructed input:
             * We built a non-null SinFunction and an interval [root-left, root+right] around the
             * known root k*pi, so a correct solver is obligated to return that root within its
             * advertised absolute accuracy.
             */
            if (Math.abs(withInitial - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solver must recover constructed sin root input=min=" + min + ",max=" + max + ",initial=" + initial + ",expected=" + expectedRoot + " lhs=" + withInitial + " rhs=" + expectedRoot);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (anchor && isWrappedRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.indexOf("invalid") >= 0 || lower.indexOf("illegal") >= 0 || lower.indexOf("argument") >= 0) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        return (t instanceof NullPointerException) && hasSolveFrame(t);
    }

    private static boolean isWrappedRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur != t && (cur instanceof NullPointerException) && hasSolveFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasSolveFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
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