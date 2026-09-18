package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static boolean hasRelevantSolveFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if (e == null) {
                continue;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        // ANCHOR: exact failing test input. This drives the patched overload on a fresh solver
        // with a valid function and a valid bracketing interval.
        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(sin, 3.0, 3.2, 3.1);
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && hasRelevantSolveFrame(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        // EXPLORE: same root-cause property as the seed, but with many valid intervals around
        // roots of sin(x) shifted by 2*pi*k. A correct implementation must accept these.
        int k = data.consumeInt(-8, 8);
        double shift = 2.0 * Math.PI * k;
        double min = 3.0 + shift;
        double max = 3.2 + shift;
        int frac = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (frac / 1000.0);

        try {
            BisectionSolver exploreSolver = new BisectionSolver();
            exploreSolver.solve(sin, min, max, initial);
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && hasRelevantSolveFrame(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        // Independent oracle on the uncovered sibling entry point solve(double, double):
        // Using stored-function solvers, solve sin(x)=0 on two windows that are exact 2*pi*n
        // translations of each other. Since sin(x + 2*pi*n) = sin(x), and each window is a
        // translated copy of [3.0, 3.2] containing the unique root pi + 2*pi*n, a correct
        // solver must return roots whose difference is exactly that translation, up to solver
        // accuracy. This still detects silent wrong-result patches even if the known NPE is
        // merely suppressed.
        int n1 = data.consumeInt(-6, 6);
        int delta = data.consumeInt(-4, 4);
        if (delta == 0) {
            delta = 1;
        }
        int n2 = n1 + delta;

        double s1 = 2.0 * Math.PI * n1;
        double s2 = 2.0 * Math.PI * n2;
        double min1 = 3.0 + s1;
        double max1 = 3.2 + s1;
        double min2 = 3.0 + s2;
        double max2 = 3.2 + s2;

        double r1;
        double r2;
        try {
            BisectionSolver stored1 = new BisectionSolver(sin);
            BisectionSolver stored2 = new BisectionSolver(sin);
            r1 = stored1.solve(min1, max1);
            r2 = stored2.solve(min2, max2);

            double expectedDelta = 2.0 * Math.PI * (n2 - n1);
            double actualDelta = r2 - r1;
            double tol = Math.max(stored1.getAbsoluteAccuracy(), stored2.getAbsoluteAccuracy()) * 8.0;

            if (Math.abs(actualDelta - expectedDelta) > tol) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:periodic-shift] metamorphic violation: translated root windows for sin "
                                + "must produce roots separated by the same 2*pi shift"
                                + " n1=" + n1
                                + " n2=" + n2
                                + " r1=" + r1
                                + " r2=" + r2
                                + " actualDelta=" + actualDelta
                                + " expectedDelta=" + expectedDelta
                                + " tol=" + tol);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof FuzzerSecurityIssueLow) {
                throw t;
            }
            if (t instanceof NullPointerException && hasRelevantSolveFrame(t)) {
                throw t;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }
    }
}