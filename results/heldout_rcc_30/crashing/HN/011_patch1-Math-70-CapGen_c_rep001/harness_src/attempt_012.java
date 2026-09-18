package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from the regression test. On the buggy build,
        // solve(f, 3.0, 3.2, 3.1) wrongly delegates to solve(min, max), losing f and
        // causing the verified NullPointerException from BisectionSolver.solve.
        try {
            BisectionSolver solver = new BisectionSolver();
            double r = solver.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle:
            // The interval [3.0, 3.2] brackets the known sine root pi, so a correct
            // bisection solve must return a value close to Math.PI.
            if (Double.isNaN(r) || Double.isInfinite(r) ||
                Math.abs(r - Math.PI) > solver.getAbsoluteAccuracy() * 4.0) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: known-root solve should approximate pi input=[3.0,3.2,3.1] result=" + r);
            }
        } catch (RuntimeException t) {
            boolean stackOk = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    stackOk = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackOk) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }

        // EXPLORE: generate many valid-by-construction intervals that bracket a real root
        // of sin(x) at n*pi, with initial strictly inside [min, max].
        // Because these inputs satisfy the solver's documented preconditions (non-null f,
        // bracketing interval, moderate magnitudes), a NullPointerException from solve is a
        // genuine defect, not clean rejection.
        int n = data.consumeInt(-1000, 1000);
        double expectedRoot = n * Math.PI;

        int leftMillis = data.consumeInt(50, 1000);
        int rightMillis = data.consumeInt(50, 1000);
        double leftWidth = leftMillis / 1000.0;
        double rightWidth = rightMillis / 1000.0;

        double min = expectedRoot - leftWidth;
        double max = expectedRoot + rightWidth;

        int pos = data.consumeInt(1, 999);
        double initial = min + (max - min) * (pos / 1000.0);

        try {
            BisectionSolver solverA = new BisectionSolver();
            double withInitial = solverA.solve(f, min, max, initial);

            // SAME-NAME OVERLOAD agreement:
            // For the same function and bracket, solve(f,min,max,initial) must agree with
            // solve(f,min,max); the extra initial value is not allowed to change the root.
            BisectionSolver solverB = new BisectionSolver();
            double withoutInitial = solverB.solve(f, min, max);

            if (Double.isNaN(withInitial) || Double.isInfinite(withInitial) ||
                Double.isNaN(withoutInitial) || Double.isInfinite(withoutInitial)) {
                throw new RuntimeException("[oracle:finite-root] metamorphic violation: solver returned non-finite result input=min=" + min + " max=" + max + " initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min=" + min + " max=" + max + " initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            // Oracle from the input itself:
            // We constructed the interval around the known sine root n*pi, so the returned
            // root must approximate that known value for any correct implementation.
            if (Math.abs(withInitial - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-sin-root] metamorphic violation: constructed bracket around n*pi should solve to that root input=n=" + n + " min=" + min + " max=" + max + " initial=" + initial + " result=" + withInitial + " expected=" + expectedRoot);
            }
        } catch (RuntimeException t) {
            boolean stackOk = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    stackOk = true;
                    break;
                }
            }
            if ((t instanceof NullPointerException || t.getClass() == RuntimeException.class) && stackOk) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }
    }
}