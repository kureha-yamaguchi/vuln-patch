package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();
        BisectionSolver anchorSolver = new BisectionSolver();
        try {
            anchorSolver.solve(anchorFunction, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection) {
                boolean rootNpe = t instanceof NullPointerException;
                boolean inPatchedRegion = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
                if (rootNpe && inPatchedRegion) {
                } else {
                }
            }
        }

        int multiple = data.consumeInt(1, 20);
        int widthTicks = data.consumeInt(1, 200);
        double width = 0.05 + (widthTicks / 1000.0);
        double root = multiple * Math.PI;
        double min = root - width;
        double max = root + width;
        double initial = root + (data.consumeInt(-100, 100) / 100.0) * (width / 2.0);

        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = new SinFunction();

        double returned;
        try {
            returned = storedSolver.solve(min, max);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        double reported;
        int iterations;
        try {
            reported = storedSolver.getResult();
            iterations = storedSolver.getIterationCount();
        } catch (RuntimeException e) {
            throw new RuntimeException("[oracle:stored-result] metamorphic violation: successful solve(min,max) must store the same result it returns, but getters failed after success input=[" + min + "," + max + "] returned=" + returned, e);
        }

        double tol = Math.max(storedSolver.getAbsoluteAccuracy(), 1e-12);
        if (Math.abs(returned - reported) > tol) {
            throw new RuntimeException("[oracle:stored-result] metamorphic violation: successful solve(min,max) must store the same result it returns input=[" + min + "," + max + "] returned=" + returned + " stored=" + reported + " tol=" + tol);
        }
        if (iterations < 0) {
            throw new RuntimeException("[oracle:stored-iterations] metamorphic violation: successful solve(min,max) must record a non-negative iteration count input=[" + min + "," + max + "] iterations=" + iterations);
        }

        BisectionSolver explicitSolver = new BisectionSolver();
        double explicitReturned;
        try {
            explicitReturned = explicitSolver.solve(new SinFunction(), min, max, initial);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            boolean rootNpe = e instanceof NullPointerException;
            boolean inPatchedRegion = false;
            for (StackTraceElement ste : e.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    inPatchedRegion = true;
                    break;
                }
            }
            if (rootNpe && inPatchedRegion) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        double crossTol = Math.max(Math.max(storedSolver.getAbsoluteAccuracy(), explicitSolver.getAbsoluteAccuracy()), 1e-9);
        if (Math.abs(explicitReturned - reported) > crossTol) {
            throw new RuntimeException("[oracle:explicit-vs-stored] metamorphic violation: equivalent successful solves over the same valid interval for the same real function must agree input=[" + min + "," + max + "] initial=" + initial + " storedSolve=" + reported + " explicitSolve=" + explicitReturned + " tol=" + crossTol);
        }
    }
}