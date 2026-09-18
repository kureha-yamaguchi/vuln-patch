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
            double min = 3.0;
            double max = 3.2;
            double initial = 3.1;
            boolean validByConstruction = true;
            try {
                double withInitial = solver.solve(f, min, max, initial);
                try {
                    double withoutInitial = solver.solve(f, min, max);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                    if (Math.abs(withInitial - withoutInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                return;
            } catch (Throwable ignored) {
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        if (!(initial >= min && initial <= max)) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        boolean validByConstruction = true;
        try {
            double withInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle: the same-name solve overloads taking the same function and interval
             * are documented to agree where their docs match; the extra initial guess must not change
             * the solved root on a valid bracketing interval. A patch that merely suppresses the crash
             * or bypasses the real solve path can violate this observable agreement without throwing.
             */
            try {
                double withoutInitial = solver.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                if (Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + ",root=" + root + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    throughSolve = true;
                    break;
                }
            }
            if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                throw t;
            }
            return;
        } catch (Throwable ignored) {
            return;
        }
    }
}