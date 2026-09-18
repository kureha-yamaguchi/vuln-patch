package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();

        double root = data.consumeInt(-1_000_000, 1_000_000) / 1024.0;
        double span = data.consumeInt(1, 1_000_000) / 1024.0;

        double min = root - span;
        double max = root + span;

        PolynomialFunction f = new PolynomialFunction(new double[] { -root, 1.0 });

        double initial;
        switch (data.consumeInt(0, 5)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min - span;
                break;
            case 4:
                initial = max + span;
                break;
            default:
                initial = data.consumeInt() / 1024.0;
                break;
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}