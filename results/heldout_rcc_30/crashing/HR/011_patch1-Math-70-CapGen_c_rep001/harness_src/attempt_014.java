package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;

        int leftMillis = data.consumeInt(1, 900);
        int rightMillis = data.consumeInt(1, 900);
        double left = leftMillis / 1000.0;
        double right = rightMillis / 1000.0;

        double min = center - left;
        double max = center + right;

        int mode = data.consumeInt(0, 2);
        double initial;
        if (mode == 0) {
            initial = min;
        } else if (mode == 1) {
            initial = max;
        } else {
            int pos = data.consumeInt(0, 1000);
            initial = min + (max - min) * (pos / 1000.0);
        }

        exerciseCase(center, min, max, initial);
    }

    private static void anchor() {
        double center = Math.PI;
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;
        exerciseCase(center, min, max, initial);
    }

    private static void exerciseCase(double center, double min, double max, double initial) {
        UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver explicitSolver = new BisectionSolver();
            double explicit = explicitSolver.solve(f, min, max, initial);

            BisectionSolver storedBroad = new BisectionSolver(f);
            double broad = storedBroad.solve(min, max);

            double refinedHalfWidth = Math.min(center - min, max - center) / 2.0;
            if (refinedHalfWidth <= 0.0) {
                return;
            }

            double refinedMin = center - refinedHalfWidth;
            double refinedMax = center + refinedHalfWidth;

            BisectionSolver storedRefined = new BisectionSolver(f);
            double refined = storedRefined.solve(refinedMin, refinedMax);

            /*
             * Sound metamorphic oracle:
             * For sin(x), any interval [center-a, center+b] with 0<a,b<pi contains exactly one root at center=k*pi
             * if a,b are kept below 1 here. Both the broad and refined intervals bracket that same unique root.
             * Therefore correct calls through the real public API must converge to the same root, even when the
             * interval is narrowed, and regardless of whether the function is supplied explicitly or via constructor state.
             * A throw-deleting or state-skipping patch can silence the known NPE yet still return a wrong value here.
             */
            double tol = Math.max(explicitSolver.getAbsoluteAccuracy(),
                    Math.max(storedBroad.getAbsoluteAccuracy(), storedRefined.getAbsoluteAccuracy())) * 8.0 + 1e-12;

            if (Math.abs(explicit - refined) > tol) {
                throw new RuntimeException("[oracle:refine-same-root] metamorphic violation: explicit broad solve disagrees with refined same-root solve center="
                        + center + " min=" + min + " max=" + max + " initial=" + initial
                        + " explicit=" + explicit + " refined=" + refined + " tol=" + tol);
            }

            if (Math.abs(broad - refined) > tol) {
                throw new RuntimeException("[oracle:stored-refine-same-root] metamorphic violation: stored broad solve disagrees with refined same-root solve center="
                        + center + " min=" + min + " max=" + max
                        + " broad=" + broad + " refined=" + refined + " tol=" + tol);
            }
        } catch (NullPointerException npe) {
            if (isRootCauseNpe(npe)) {
                throw npe;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        int i;
        for (i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}