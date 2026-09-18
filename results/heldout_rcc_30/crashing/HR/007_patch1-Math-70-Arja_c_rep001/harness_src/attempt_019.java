package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        // Exact anchor from the failing test: on the buggy build this reaches the patched line
        // and throws NullPointerException from BisectionSolver.solve.
        try {
            new BisectionSolver().solve(sin, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            triage(t, true);
        }

        int n = data.consumeInt(1, 6);
        double center = n * Math.PI;
        double width = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double min = center - width;
        double max = center + width;
        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);

        // Explore many other valid explicit calls that satisfy the preconditions by construction:
        // non-null function and an interval bracketing a real SinFunction root.
        try {
            new BisectionSolver().solve(sin, min, max, initial);
        } catch (Throwable t) {
            triage(t, true);
        }

        // Different reachable function: solve(double, double) on a solver constructed with a real function.
        // Contract used for this metamorphic check:
        // BisectionSolver returns a root of the stored function inside the provided interval.
        // For SinFunction, sin(-x) = -sin(x). On mirrored intervals around ±nπ with width < π/2,
        // each interval contains exactly one corresponding root, so the returned roots must be negatives
        // of each other. A throw-deleting or state-corrupting patch in this region can violate this even
        // if it no longer reproduces the original NPE.
        double mirroredCenter = -center;
        double minNeg = mirroredCenter - width;
        double maxNeg = mirroredCenter + width;

        try {
            BisectionSolver posSolver = new BisectionSolver(sin);
            BisectionSolver negSolver = new BisectionSolver(sin);

            double pos = posSolver.solve(min, max);
            double neg = negSolver.solve(minNeg, maxNeg);

            double tol = Math.max(posSolver.getAbsoluteAccuracy(), negSolver.getAbsoluteAccuracy()) * 8.0;
            if (Math.abs(pos + neg) > tol) {
                throw new RuntimeException(
                    "[oracle:odd-symmetry] metamorphic violation: mirrored SinFunction brackets must yield opposite roots"
                        + " n=" + n
                        + " width=" + width
                        + " pos=" + pos
                        + " neg=" + neg
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            triage(t, false);
        }
    }

    private static void triage(Throwable t, boolean validByConstruction) {
        if (t == null) {
            return;
        }

        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }

        if (t instanceof RuntimeException) {
            if (validByConstruction && isRelevantNullPointer(t)) {
                throw (RuntimeException) t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            return;
        }

        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }

        return;
    }

    private static boolean isRelevantNullPointer(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}