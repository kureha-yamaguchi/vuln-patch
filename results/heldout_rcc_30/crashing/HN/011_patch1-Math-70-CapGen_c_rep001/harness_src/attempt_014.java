package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double rWithInitial = solver.solve(f, 3.0, 3.2, 3.1);

                /* Contract/oracle:
                 * solve(f,min,max,initial) and solve(f,min,max) are sibling overloads for the
                 * same root-finding operation on the same valid bracket and must agree.
                 */
                BisectionSolver solver2 = new BisectionSolver();
                try {
                    double rNoInitial = solver2.solve(f, 3.0, 3.2);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                    if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                            && Math.abs(rWithInitial - rNoInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                    }
                } catch (Throwable ignored) {
                    return;
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
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double delta = 0.1 + (data.consumeInt(0, 900) / 1000.0);
        double min = root - delta;
        double max = root + delta;
        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        if (!(min < max)) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            double rWithInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle:
             * We build a valid bracket around an exact root of sin(x), namely k*pi, with
             * 0 < delta < pi so min=root-delta and max=root+delta necessarily bracket a root.
             * For the same valid function and interval, the overload with initial guess must
             * agree with the overload without it.
             */
            BisectionSolver solver2 = new BisectionSolver();
            try {
                double rNoInitial = solver2.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                        && Math.abs(rWithInitial - rNoInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-fuzz] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                }
            } catch (Throwable ignored) {
                return;
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
    }
}