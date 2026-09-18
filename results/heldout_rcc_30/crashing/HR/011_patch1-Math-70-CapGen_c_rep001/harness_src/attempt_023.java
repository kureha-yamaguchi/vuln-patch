package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // Anchor: exact trigger from the failing test. On the buggy version this reaches
        // BisectionSolver.solve(f, min, max, initial) and throws NullPointerException.
        BisectionSolver anchor = new BisectionSolver();
        try {
            anchor.solve(f, 3.0, 3.2, 3.1);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }

        // Explore the same root-cause property with many valid-by-construction inputs:
        // a real function is supplied explicitly, and the interval brackets a SinFunction root.
        int k = data.consumeInt(-64, 64);
        double root = k * Math.PI;

        double left = data.consumeInt(1, 1_000_000) / 1_000_000.0;
        double right = data.consumeInt(1, 1_000_000) / 1_000_000.0;
        double min = root - left;
        double max = root + right;

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double fraction = data.consumeInt(0, 1_000_000) / 1_000_000.0;
            initial = min + (max - min) * fraction;
        }

        BisectionSolver explicitSolver = new BisectionSolver();
        BisectionSolver directSolver = new BisectionSolver();

        try {
            double viaInitial = explicitSolver.solve(f, min, max, initial);
            double direct = directSolver.solve(f, min, max);

            // Contract/metamorphic check: for BisectionSolver, the overload with an
            // explicit initial guess must agree with the overload without it, since
            // bisection does not use the initial guess to change the solved root.
            double tol = Math.max(explicitSolver.getAbsoluteAccuracy(), directSolver.getAbsoluteAccuracy()) * 8.0;
            if (Math.abs(viaInitial - direct) > tol) {
                throw new RuntimeException(
                    "[oracle:initial-overload-agreement] metamorphic violation: "
                        + "solve(f,min,max,initial) disagrees with solve(f,min,max)"
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " viaInitial=" + viaInitial
                        + " direct=" + direct
                        + " tol=" + tol);
            }

            // Independent cache-vs-return consistency check.
            double cached = explicitSolver.getResult();
            if (Math.abs(cached - viaInitial) > 0.0) {
                throw new RuntimeException(
                    "[oracle:result-cache-consistency] metamorphic violation: "
                        + "returned root differs from cached result"
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " returned=" + viaInitial
                        + " cached=" + cached);
            }
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}