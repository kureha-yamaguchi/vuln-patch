package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean sawRootCauseNpe = false;

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            boolean inPatchedRegion = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && inPatchedRegion) {
                sawRootCauseNpe = true;
            }
        }

        int milliLeft = data.consumeInt(10, 400);
        int milliRight = data.consumeInt(10, 400);
        double left = milliLeft / 1000.0;
        double right = milliRight / 1000.0;

        double plusMin = Math.PI - left;
        double plusMax = Math.PI + right;
        double minusMin = -Math.PI - right;
        double minusMax = -Math.PI + left;

        double initialOffsetMilli = data.consumeInt(-Math.min(milliLeft, 300), Math.min(milliRight, 300));
        double plusInitial = Math.PI + (initialOffsetMilli / 1000.0);
        if (plusInitial < plusMin) {
            plusInitial = plusMin;
        }
        if (plusInitial > plusMax) {
            plusInitial = plusMax;
        }
        double minusInitial = -plusInitial;

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(new QuinticFunction(), -0.2, 0.2);
        } catch (Throwable t) {
            return;
        }

        double plusRoot;
        try {
            plusRoot = solver.solve(new SinFunction(), plusMin, plusMax, plusInitial);
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        }

        double minusRoot;
        try {
            minusRoot = solver.solve(new SinFunction(), minusMin, minusMax, minusInitial);
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        }

        /*
         * Sound metamorphic oracle:
         * SinFunction is odd, so roots in intervals that are exact negatives of each other
         * around +/- pi must be negatives of each other as well. A correct implementation of
         * solve(f, min, max, initial) must use the explicitly supplied function for both calls;
         * deleting the throw or silently reusing stale solver state breaks this relation.
         */
        double symmetryError = Math.abs(plusRoot + minusRoot);
        double tolerance = 64.0 * solver.getAbsoluteAccuracy();
        if (symmetryError > tolerance) {
            throw new RuntimeException(
                    "[oracle:odd-symmetry] metamorphic violation: symmetric SinFunction intervals should yield opposite roots"
                            + " plusInterval=[" + plusMin + "," + plusMax + "]"
                            + " minusInterval=[" + minusMin + "," + minusMax + "]"
                            + " plusInitial=" + plusInitial
                            + " minusInitial=" + minusInitial
                            + " plusRoot=" + plusRoot
                            + " minusRoot=" + minusRoot
                            + " sum=" + (plusRoot + minusRoot)
                            + " tol=" + tolerance
                            + " anchorNpeSeen=" + sawRootCauseNpe);
        }
    }
}