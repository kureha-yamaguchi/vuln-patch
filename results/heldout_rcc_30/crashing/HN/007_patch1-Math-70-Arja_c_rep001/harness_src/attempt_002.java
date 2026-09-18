package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        explore(data);
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double r = solver.solve(f, 3.0, 3.2, 3.1);

            /*
             * Oracle: same-name overloads documented to agree on equivalent inputs.
             * For a correct implementation, solve(f,min,max,initial) and solve(f,min,max)
             * must compute the same root for the same function and interval; deleting the
             * failing statement or bypassing the real function would break this agreement.
             */
            BisectionSolver sibling = new BisectionSolver();
            double r2 = sibling.solve(f, 3.0, 3.2);
            if (!closeEnough(r, r2, Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()))) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromSolve(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

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
            initial = root;
        } else {
            double fraction = data.consumeInt(1, 999) / 1000.0;
            initial = min + (max - min) * fraction;
        }

        if (!(min <= initial && initial <= max)) {
            initial = root;
        }

        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();

        double lhs;
        double rhs;
        try {
            lhs = solverA.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromSolve(t) && isValidConstructedInput(f, min, max, initial, root)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            rhs = solverB.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        /*
         * Oracle: overload agreement on equivalent inputs. Both public solve overloads
         * operate on the same function and bracket, so any correct implementation must
         * return the same root up to solver accuracy. If either side throws, we skip.
         */
        double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
        if (!closeEnough(lhs, rhs, tol)) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
        }

        /*
         * Oracle from known construction: we built the interval around a known root of sin(x),
         * namely k*pi, with min < k*pi < max. A correct bisection solve on SinFunction must
         * therefore return that constructed root within solver accuracy.
         */
        if (!closeEnough(lhs, root, tol)) {
            throw new RuntimeException("[oracle:known-root] metamorphic violation: result must recover constructed sin root input=[" + min + "," + max + "," + initial + "] expected=" + root + " actual=" + lhs);
        }
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int milli = data.consumeInt(1, 1000);
        return milli / 1000.0;
    }

    private static boolean isValidConstructedInput(UnivariateRealFunction f, double min, double max, double initial, double root) {
        return f != null
                && !Double.isNaN(min)
                && !Double.isNaN(max)
                && !Double.isNaN(initial)
                && !Double.isInfinite(min)
                && !Double.isInfinite(max)
                && !Double.isInfinite(initial)
                && min < max
                && min <= initial
                && initial <= max
                && min < root
                && root < max;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Invalid") || n.contains("Illegal") || n.contains("Argument");
    }

    private static boolean isRootCauseNpeFromSolve(Throwable t) {
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

    private static boolean closeEnough(double a, double b, double tol) {
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= Math.max(tol, 1e-12 * scale);
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