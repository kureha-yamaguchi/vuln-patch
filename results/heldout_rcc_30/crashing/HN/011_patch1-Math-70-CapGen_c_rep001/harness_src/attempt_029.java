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
            final double min = 3.0;
            final double max = 3.2;
            final double initial = 3.1;
            final boolean validByConstruction = true;
            double lhs;
            try {
                lhs = solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    String msg = t.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
                    return;
                }
                return;
            }

            double rhs;
            try {
                rhs = new BisectionSolver().solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            double tol = Math.max(solver.getAbsoluteAccuracy(), new BisectionSolver().getAbsoluteAccuracy()) * 8.0;
            if (Double.isFinite(lhs) && Double.isFinite(rhs) && Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:overload-agreement-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + lhs + " rhs=" + rhs);
            }
            if (Double.isFinite(lhs) && Math.abs(lhs - Math.PI) > tol * 4.0) {
                throw new RuntimeException("[oracle:pi-anchor] metamorphic violation: anchored valid sin bracket around PI should solve near PI input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + lhs + " rhs=" + Math.PI);
            }
        }

        {
            int k = data.consumeInt(-50, 50);
            int leftMillis = data.consumeInt(1, 1000);
            int rightMillis = data.consumeInt(1, 1000);
            int fracMillis = data.consumeInt(0, 1000);

            double center = k * Math.PI;
            double min = center - (leftMillis / 1000.0);
            double max = center + (rightMillis / 1000.0);
            double initial = min + ((max - min) * (fracMillis / 1000.0));
            final boolean validByConstruction = f != null && min <= initial && initial <= max;

            BisectionSolver solver = new BisectionSolver();
            double lhs;
            try {
                lhs = solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    String msg = t.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
                    return;
                }
                return;
            }

            double rhs;
            try {
                rhs = new BisectionSolver().solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            /* Contract/oracle:
             * The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are required to agree on the same
             * valid problem instance here: a non-null real function with a bracket [min,max] that contains a root.
             * In BisectionSolver the initial guess is not part of the bisection iteration, and the patched method
             * explicitly delegates to solve(f,min,max). A patch that merely suppresses the crash or skips the real
             * delegation can return a different result without throwing; this observable equality catches that.
             */
            double tol = Math.max(solver.getAbsoluteAccuracy(), new BisectionSolver().getAbsoluteAccuracy()) * 8.0;
            if (Double.isFinite(lhs) && Double.isFinite(rhs) && Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:overload-agreement-fuzz] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + ",k=" + k + " lhs=" + lhs + " rhs=" + rhs);
            }

            double expected = center;
            if (Double.isFinite(lhs) && Math.abs(lhs - expected) > Math.max(tol * 4.0, 1.0e-6)) {
                throw new RuntimeException("[oracle:root-location-fuzz] metamorphic violation: valid sin bracket constructed around k*pi must solve near that known root input=min=" + min + ",max=" + max + ",initial=" + initial + ",k=" + k + " lhs=" + lhs + " rhs=" + expected);
            }
        }
    }
}