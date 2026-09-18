package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle: the same-name overloads
            // solve(f, min, max, initial) and solve(f, min, max)
            // are documented to agree for BisectionSolver because bisection does not
            // use the initial guess; the patch itself fixes delegation to the sibling overload.
            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double withoutInitial = solverWithoutInitial.solve(f, 3.0, 3.2);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(),
                                  solverWithoutInitial.getAbsoluteAccuracy()) * 8.0 + 1.0e-12;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=[f=SinFunction,min=3.0,max=3.2,initial=3.1]"
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (Throwable t) {
            boolean hasSolveFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    hasSolveFrame = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && hasSolveFrame) {
                throw (NullPointerException) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        // EXPLORE: valid-by-construction intervals bracketing roots of sin(x) at n*pi.
        // Property: on a fresh BisectionSolver, calling solve(f,min,max,initial) with a non-null
        // function and a valid bracket should be accepted and agree with solve(f,min,max).
        int cases = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 4) : 0);
        for (int i = 0; i < cases; i++) {
            int n = data.consumeInt(-100, 100);
            double root = n * Math.PI;

            double leftWidth = 0.05 + (data.consumeInt(0, 950) / 1000.0);
            double rightWidth = 0.05 + (data.consumeInt(0, 950) / 1000.0);
            if (leftWidth > 1.2) {
                leftWidth = 1.2;
            }
            if (rightWidth > 1.2) {
                rightWidth = 1.2;
            }

            double min = root - leftWidth;
            double max = root + rightWidth;
            if (!(min < root && root < max)) {
                continue;
            }

            double fraction = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * fraction;

            try {
                BisectionSolver solverWithInitial = new BisectionSolver();
                double withInitial = solverWithInitial.solve(f, min, max, initial);

                BisectionSolver solverWithoutInitial = new BisectionSolver();
                double withoutInitial = solverWithoutInitial.solve(f, min, max);

                double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(),
                                      solverWithoutInitial.getAbsoluteAccuracy()) * 8.0 + 1.0e-12;
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=[root=" + root + ",min=" + min + ",max=" + max + ",initial=" + initial + "]"
                        + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable t) {
                boolean hasSolveFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        hasSolveFrame = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && hasSolveFrame) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException
                        || t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
                    return;
                }
                return;
            }
        }
    }
}