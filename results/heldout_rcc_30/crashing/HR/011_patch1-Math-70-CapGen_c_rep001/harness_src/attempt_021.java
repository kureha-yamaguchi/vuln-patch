package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        anchorAndExploreExplicitCall(data, sin);
        crossCheckStoredSolveOnDifferentRoot(data, sin);
    }

    private static void anchorAndExploreExplicitCall(FuzzedDataProvider data, UnivariateRealFunction f) {
        exerciseExplicitCall(f, 3.0, 3.2, 3.1, "anchor");

        int variants = 1 + Math.max(0, data.consumeInt(0, 6));
        for (int i = 0; i < variants; i++) {
            int k = data.consumeInt(-8, 8);
            double center = Math.PI + (2.0 * Math.PI * k);
            double leftPad = 0.01 + (data.consumeInt(0, 400) / 1000.0);
            double rightPad = 0.01 + (data.consumeInt(0, 400) / 1000.0);
            double min = center - leftPad;
            double max = center + rightPad;
            double initial;
            if (data.consumeBoolean()) {
                initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
            } else {
                initial = center + ((data.consumeInt(-1000, 1000)) / 100000.0);
                if (initial < min) {
                    initial = min;
                } else if (initial > max) {
                    initial = max;
                }
            }
            exerciseExplicitCall(f, min, max, initial, "explore-explicit");
        }
    }

    private static void exerciseExplicitCall(UnivariateRealFunction f, double min, double max, double initial, String tag) {
        BisectionSolver solver = new BisectionSolver();
        try {
            double rExplicit = solver.solve(f, min, max, initial);

            // Contract from the shown implementation: solve(f,min,max,initial) delegates to solve(f,min,max),
            // so a correct implementation must agree with a fresh explicit solve on the same interval.
            BisectionSolver fresh = new BisectionSolver();
            double rPlain = fresh.solve(f, min, max);

            double tol = Math.max(solver.getAbsoluteAccuracy(), fresh.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(rExplicit - rPlain) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:explicit-delegation] metamorphic violation: delegated explicit solve disagrees with plain explicit solve"
                        + " tag=" + tag
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " explicit=" + rExplicit
                        + " plain=" + rPlain
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:explicit-delegation] root-cause NPE on valid explicit solve"
                        + " tag=" + tag
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial);
            }
        }
    }

    private static void crossCheckStoredSolveOnDifferentRoot(FuzzedDataProvider data, UnivariateRealFunction f) {
        int k1 = data.consumeInt(-6, 6);
        int k2 = data.consumeInt(-6, 6);
        if (k2 == k1) {
            k2 = k1 + 1;
        }

        double c1 = Math.PI + (2.0 * Math.PI * k1);
        double c2 = Math.PI + (2.0 * Math.PI * k2);

        double left1 = 0.02 + (data.consumeInt(0, 300) / 1000.0);
        double right1 = 0.02 + (data.consumeInt(0, 300) / 1000.0);
        double left2 = 0.02 + (data.consumeInt(0, 300) / 1000.0);
        double right2 = 0.02 + (data.consumeInt(0, 300) / 1000.0);

        double min1 = c1 - left1;
        double max1 = c1 + right1;
        double min2 = c2 - left2;
        double max2 = c2 + right2;

        BisectionSolver stored = new BisectionSolver();
        BisectionSolver fresh = new BisectionSolver();

        try {
            stored.solve(f, min1, max1);
            double viaStored = stored.solve(min2, max2);
            double viaFresh = fresh.solve(f, min2, max2);

            // Public-overload consistency: once the function is installed through solve(f,min,max),
            // the overload solve(min,max) must solve that same stored function for any later valid interval.
            // Independent oracle: a fresh solver solving the same function on the same interval.
            double tol = Math.max(stored.getAbsoluteAccuracy(), fresh.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(viaStored - viaFresh) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:stored-other-root] consistency violation: stored solve(min,max) disagrees with fresh explicit solve"
                        + " prime=[" + min1 + "," + max1 + "]"
                        + " target=[" + min2 + "," + max2 + "]"
                        + " stored=" + viaStored
                        + " fresh=" + viaFresh
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:stored-other-root] root-cause NPE while solving valid stored interval"
                        + " prime=[" + min1 + "," + max1 + "]"
                        + " target=[" + min2 + "," + max2 + "]");
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Convergence") || n.contains("Invalid") || n.contains("NoBracketing");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}