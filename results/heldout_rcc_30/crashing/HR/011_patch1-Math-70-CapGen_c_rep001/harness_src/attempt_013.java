package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();

        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }

        double leftDelta = data.consumeInt(0, 200) / 1000.0;
        double rightDelta = data.consumeInt(0, 200) / 1000.0;
        double initialShift = data.consumeInt(-200, 200) / 1000.0;

        double min = Math.PI - leftDelta;
        double max = Math.PI + rightDelta;
        if (max <= min) {
            max = min + 0.001;
        }

        double initial = Math.PI + initialShift;
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }
    }
}