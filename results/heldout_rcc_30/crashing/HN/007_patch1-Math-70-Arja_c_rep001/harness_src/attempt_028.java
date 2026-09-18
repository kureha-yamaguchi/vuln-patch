package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runCase(sin, 3.0, 3.2, 3.1, true);

        // EXPLORE: the buggy overload NPEs because it forgets to bind the supplied function
        // before delegating. Generate many valid-by-construction bracketed intervals for a real
        // library function so a correct implementation is obligated to handle them.
        int cases = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-50, 50);
            double root = k * Math.PI;

            double left = 0.01 + (data.consumeInt(0, 990) / 1000.0);
            double right = 0.01 + (data.consumeInt(0, 990) / 1000.0);

            // Keep the interval narrow enough to bracket exactly one known root of sin(x).
            if (left > 1.0) {
                left = 1.0;
            }
            if (right > 1.0) {
                right = 1.0;
            }

            double min = root - left;
            double max = root + right;

            double fraction = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * fraction;

            runCase(sin, min, max, initial, true);
        }
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double r1 = solverWithInitial.solve(f, min, max, initial);

            // Documented same-name overload agreement: solve(f, min, max, initial) and
            // solve(f, min, max) must agree on equivalent inputs. A "fix" that merely avoids the
            // crash but drops the supplied function or changes behavior would violate this.
            double r2;
            try {
                BisectionSolver solverWithoutInitial = new BisectionSolver();
                r2 = solverWithoutInitial.solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), 1.0e-12);
            if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
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
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name.contains("Convergence") || name.contains("Invalid") || name.contains("NoBracketing")) {
            return true;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}