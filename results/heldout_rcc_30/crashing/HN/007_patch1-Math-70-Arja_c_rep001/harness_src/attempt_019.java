package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            boolean validInput = true;
            try {
                double result = solver.solve(f, 3.0, 3.2, 3.1);
                double expected = Math.PI;
                if (Math.abs(result - expected) > solver.getAbsoluteAccuracy()) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: documented overload agreement/root result for SinFunction near pi input=[3.0,3.2,3.1] lhs=" + result + " rhs=" + expected);
                }
            } catch (Throwable t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                boolean validation = t instanceof IllegalArgumentException
                        || t instanceof NumberFormatException
                        || (t.getClass().getName().startsWith("org.apache.commons.math")
                            && t.getClass().getSimpleName().toLowerCase().contains("invalid"))
                        || (t instanceof FunctionEvaluationException)
                        || (t instanceof MaxIterationsExceededException);
                if (validInput && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (!validation) {
                    return;
                }
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;
        int milliWidth = data.consumeInt(1, 1000);
        double width = milliWidth / 1000.0;
        double min = center - width;
        double max = center + width;
        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);

        {
            BisectionSolver solver = new BisectionSolver();
            boolean validInput = true;
            try {
                solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                boolean validation = t instanceof IllegalArgumentException
                        || t instanceof NumberFormatException
                        || (t.getClass().getName().startsWith("org.apache.commons.math")
                            && (t.getClass().getSimpleName().toLowerCase().contains("invalid")
                                || t.getClass().getSimpleName().toLowerCase().contains("convergence")
                                || t.getClass().getSimpleName().toLowerCase().contains("evaluation")))
                        || (t instanceof FunctionEvaluationException)
                        || (t instanceof MaxIterationsExceededException);
                if (validInput && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (!validation) {
                    return;
                }
            }
        }

        {
            BisectionSolver solverA = new BisectionSolver();
            BisectionSolver solverB = new BisectionSolver();
            Double lhs = null;
            Double rhs = null;

            try {
                lhs = solverA.solve(f, min, max, initial);
            } catch (Throwable t) {
                return;
            }

            try {
                rhs = solverB.solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            if (lhs != null && rhs != null) {
                double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
                if (Double.isNaN(lhs.doubleValue()) || Double.isNaN(rhs.doubleValue()) || Math.abs(lhs.doubleValue() - rhs.doubleValue()) > tol) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: same-name solve overloads are documented to agree on equivalent valid inputs for the same function and interval input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
                }
            }
        }

        {
            BisectionSolver solver = new BisectionSolver();
            double initial2 = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
            Double r1 = null;
            Double r2 = null;

            try {
                r1 = solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                return;
            }

            try {
                r2 = solver.solve(f, min, max, initial2);
            } catch (Throwable t) {
                return;
            }

            if (r1 != null && r2 != null) {
                double tol = solver.getAbsoluteAccuracy();
                if (Double.isNaN(r1.doubleValue()) || Double.isNaN(r2.doubleValue()) || Math.abs(r1.doubleValue() - r2.doubleValue()) > tol) {
                    throw new RuntimeException("[oracle:initial-ignored] metamorphic violation: bisection solve on the same function and bracketing interval must agree across equivalent initial guesses input=[" + min + "," + max + "," + initial + "," + initial2 + "] lhs=" + r1 + " rhs=" + r2);
                }
            }
        }
    }
}