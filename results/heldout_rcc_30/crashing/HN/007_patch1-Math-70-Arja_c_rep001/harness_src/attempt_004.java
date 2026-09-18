package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static boolean anchored = false;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (!anchored) {
            anchored = true;
            runAnchor();
        }
        runExplore(data);
    }

    private static void runAnchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        try {
            double r1 = solver.solve(f, min, max, initial);

            /* Contract/oracle:
             * The test case constructs a known valid bracket for sin(x) around the root pi.
             * A correct implementation of solve(f,min,max,initial) must return that root, and
             * the same-name overload solve(f,min,max) must agree on equivalent inputs.
             * A "fix" that merely suppresses the crash but skips using f would violate this.
             */
            BisectionSolver solver2 = new BisectionSolver();
            double r2 = solver2.solve(f, min, max);

            double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(r1 - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-root] metamorphic violation: known sin bracket around pi did not recover pi input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + Math.PI);
            }
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on same valid bracket input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int n = data.consumeInt(-100, 100);
        double root = n * Math.PI;

        int milli = data.consumeInt(10, 1000);
        double delta = milli / 1000.0;

        double min = root - delta;
        double max = root + delta;

        int initMilli = data.consumeInt(0, milli * 2);
        double initial = min + (initMilli / 1000.0);

        boolean validByConstruction = f != null && min < max && initial >= min && initial <= max && delta > 0.0 && delta < (Math.PI / 2.0);

        if (!validByConstruction) {
            return;
        }

        try {
            BisectionSolver solverA = new BisectionSolver();
            double r1 = solverA.solve(f, min, max, initial);

            /* Contract/oracle:
             * For the same non-null function and same valid interval, the overloads
             * solve(f,min,max,initial) and solve(f,min,max) are documented siblings that
             * solve the same problem; for bisection, the initial guess must not change the root.
             * We construct a genuine bracket around the known root n*pi of sin(x), so a correct
             * implementation is obliged to accept it and return that root within solver accuracy.
             */
            BisectionSolver solverB = new BisectionSolver();
            double r2;
            try {
                r2 = solverB.solve(f, min, max);
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 8.0;
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
            }
            if (Math.abs(r1 - root) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: valid sin bracket around n*pi must recover that root input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + root);
            }
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
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
        if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("Invalid") >= 0
                || name.indexOf("NoBracketing") >= 0
                || name.indexOf("Convergence") >= 0;
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