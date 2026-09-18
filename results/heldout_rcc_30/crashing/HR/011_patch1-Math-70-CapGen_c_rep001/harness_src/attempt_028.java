package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from the failing test.
        // On the buggy version this reaches BisectionSolver.solve(f, min, max, initial),
        // which incorrectly delegates to solve(min, max) and dereferences the unset stored function.
        BisectionSolver anchorSolver = new BisectionSolver();
        try {
            anchorSolver.solve(f, 3.0, 3.2, 3.1);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }

        // EXPLORE: same root-cause property as the seed.
        // Build valid bracketing intervals around k*pi for sin(x), with initial inside the interval.
        int k = data.consumeInt(-32, 32);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 1000) / 1000.0);
        double right = 0.01 + (data.consumeInt(0, 1000) / 1000.0);
        double min = root - left;
        double max = root + right;
        double initial = min + (max - min) * (data.consumeInt(0, 1000000) / 1000000.0);

        BisectionSolver solver = new BisectionSolver();
        final double result;
        try {
            result = solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }

        // Post-condition / independent oracle:
        // In BisectionSolver.solve(f, min, max), after each iteration the bracket width halves,
        // and when it returns it has called setResult(m, i) with that iteration count.
        // Therefore any correct successful solve must satisfy:
        //   originalWidth <= absoluteAccuracy * 2^(iterationCount + 1)
        // This checks observable solver state rather than only "no exception".
        int iterations = solver.getIterationCount();
        double width = max - min;
        double bound = solver.getAbsoluteAccuracy() * Math.pow(2.0, iterations + 1);
        if (!(width <= bound * 1.0000000001)) {
            throw new RuntimeException(
                "[oracle:iter-width] metamorphic violation: inconsistent bisection progress"
                    + " width=" + width
                    + " bound=" + bound
                    + " iterations=" + iterations
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " result=" + result);
        }
    }
}