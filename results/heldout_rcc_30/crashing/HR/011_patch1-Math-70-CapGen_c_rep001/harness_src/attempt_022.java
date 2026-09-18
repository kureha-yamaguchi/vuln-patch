package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(f, 3.0, 3.2, 3.1);
        } catch (NullPointerException e) {
            boolean fromPatchedRegion = false;
            for (StackTraceElement ste : e.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    fromPatchedRegion = true;
                    break;
                }
            }
            if (!fromPatchedRegion) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        int k = data.consumeInt(-32, 32);
        double root = k * Math.PI;

        int leftMicros = data.consumeInt(1, 1_000_000);
        int rightMicros = data.consumeInt(1, 1_000_000);
        double left = leftMicros / 1_000_000.0;
        double right = rightMicros / 1_000_000.0;

        double min = root - left;
        double max = root + right;

        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            int posMicros = data.consumeInt(0, 1_000_000);
            initial = min + (max - min) * (posMicros / 1_000_000.0);
        } else {
            initial = root;
        }

        if (!(initial >= min && initial <= max)) {
            return;
        }

        try {
            BisectionSolver stored = new BisectionSolver(f);
            double storedResult = stored.solve(min, max);
            double storedReported = stored.getResult();

            BisectionSolver explicit = new BisectionSolver();
            double explicitResult = explicit.solve(f, min, max, initial);
            double explicitReported = explicit.getResult();

            double storedAccuracy = stored.getAbsoluteAccuracy();
            double explicitAccuracy = explicit.getAbsoluteAccuracy();
            double tol = Math.max(8.0 * Math.max(storedAccuracy, explicitAccuracy), 1.0e-6);

            if (Math.abs(storedResult - storedReported) > 0.0) {
                throw new RuntimeException(
                        "[oracle:stored-result-cache] metamorphic violation: solve(min,max) return must equal getResult() "
                                + "min=" + min + " max=" + max + " returned=" + storedResult + " cached=" + storedReported);
            }

            if (Math.abs(explicitResult - explicitReported) > 0.0) {
                throw new RuntimeException(
                        "[oracle:explicit-result-cache] metamorphic violation: solve(f,min,max,initial) return must equal getResult() "
                                + "min=" + min + " max=" + max + " initial=" + initial
                                + " returned=" + explicitResult + " cached=" + explicitReported);
            }

            /*
             * For the same real function and the same bracketing interval containing a unique SinFunction root,
             * solving via the stored-function overload and via the explicit-function overload must agree.
             * This is an equivalence between two public APIs, so a throw-deleting patch that returns the wrong
             * value from the explicit overload is caught even if no exception is thrown.
             */
            if (Math.abs(storedResult - explicitResult) > tol) {
                throw new RuntimeException(
                        "[oracle:stored-explicit-equivalence] metamorphic violation: equivalent bisection entrypoints disagree "
                                + "k=" + k + " min=" + min + " max=" + max + " initial=" + initial
                                + " stored=" + storedResult + " explicit=" + explicitResult + " tol=" + tol);
            }
        } catch (NullPointerException e) {
            return;
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (Throwable t) {
            return;
        }
    }
}