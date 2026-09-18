package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double rootArg = (double) data.consumeInt(-100000, 100000);
        double slopeArg = data.consumeBoolean() ? 1.0 : -1.0;
        PolynomialFunction fArg = new PolynomialFunction(new double[] { -slopeArg * rootArg, slopeArg });

        double rootCtor = (double) data.consumeInt(-100000, 100000);
        double slopeCtor = data.consumeBoolean() ? 1.0 : -1.0;
        PolynomialFunction fCtor = new PolynomialFunction(new double[] { -slopeCtor * rootCtor, slopeCtor });

        BisectionSolver solver;
        if (data.consumeBoolean()) {
            solver = new BisectionSolver();
        } else {
            solver = new BisectionSolver(fCtor);
        }

        double min;
        double max;
        if (data.consumeBoolean()) {
            double leftWidth = (double) data.consumeInt(0, 1024);
            double rightWidth = (double) data.consumeInt(0, 1024);
            min = rootArg - leftWidth;
            max = rootArg + rightWidth;
        } else {
            min = (double) data.consumeInt();
            max = (double) data.consumeInt();
        }

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = rootArg;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = (min + max) / 2.0;
                break;
            default:
                initial = (double) data.consumeInt();
                break;
        }

        try {
            solver.solve(fArg, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            sneakyThrow(e);
        } catch (FunctionEvaluationException e) {
            sneakyThrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}