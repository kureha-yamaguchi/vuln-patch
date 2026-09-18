package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        // ANCHOR: exact failing test input first.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(sin, 3.0, 3.2, 3.1);

            // Contract/oracle:
            // - The failing test asserts this call finds the root at PI.
            // - A correct implementation of BisectionSolver on a valid bracketing interval
            //   around sin(x) = 0 near PI must return a value within solver accuracy of PI.
            double tol = solver.getAbsoluteAccuracy();
            if (Math.abs(result - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: known-root solve should recover PI input=[3.0,3.2,3.1] result=" + result + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE: valid-by-construction inputs with the same root-cause property:
        // non-null real function and a bracketing interval [min,max] containing a known root,
        // with initial inside [min,max].
        int k = data.consumeInt(-100, 100);
        double expectedRoot = k * Math.PI;

        double left = 0.001 + (data.consumeInt(1, 300000) / 100000.0);   // (0.001, 3.001]
        double right = 0.001 + (data.consumeInt(1, 300000) / 100000.0);  // (0.001, 3.001]
        if (left >= Math.PI) {
            left = Math.PI - 0.001;
        }
        if (right >= Math.PI) {
            right = Math.PI - 0.001;
        }

        double min = expectedRoot - left;
        double max = expectedRoot + right;
        double frac = data.consumeInt(0, 100000) / 100000.0;
        double initial = min + frac * (max - min);

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double resultWithInitial = solverWithInitial.solve(sin, min, max, initial);

            // Metamorphic/post-condition:
            // The same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // are documented sibling APIs for the same solve operation; for a correct
            // implementation on the same valid bracketing interval they must agree on
            // the located root up to solver accuracy. A "fix" that merely suppresses
            // the crash but changes behavior will violate this.
            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double resultWithoutInitial = solverWithoutInitial.solve(sin, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());
            if (Math.abs(resultWithInitial - resultWithoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve overloads must agree input=[" + min + "," + max + "," + initial + "] lhs=" + resultWithInitial + " rhs=" + resultWithoutInitial + " tol=" + tol);
            }

            // Oracle from construction:
            // We built the interval around the known root k*PI with width < PI on each side,
            // so sin(min) and sin(max) bracket exactly that root. A correct bisection solve
            // must return that known root within its advertised accuracy.
            if (Math.abs(resultWithInitial - expectedRoot) > solverWithInitial.getAbsoluteAccuracy()) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed bracketing interval should solve to known k*PI input=[" + min + "," + max + "," + initial + "] result=" + resultWithInitial + " expected=" + expectedRoot + " tol=" + solverWithInitial.getAbsoluteAccuracy());
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Invalid") || n.contains("NoBracketing") || n.contains("Convergence");
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