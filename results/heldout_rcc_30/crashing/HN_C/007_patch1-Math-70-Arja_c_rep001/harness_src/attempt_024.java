package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.QuinticFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();

        UnivariateRealFunction function;
        switch (data.consumeInt(0, 1)) {
            case 0:
                function = new SinFunction();
                break;
            default:
                function = new QuinticFunction();
                break;
        }

        double[] special = new double[] {
            0.0d,
            -0.0d,
            1.0d,
            -1.0d,
            2.0d,
            -2.0d,
            Double.MIN_VALUE,
            -Double.MIN_VALUE,
            Double.MAX_VALUE,
            -Double.MAX_VALUE,
            Double.MIN_NORMAL,
            -Double.MIN_NORMAL,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NaN
        };

        double min = data.consumeBoolean()
            ? special[data.consumeInt(0, special.length - 1)]
            : (double) data.consumeInt();

        double max = data.consumeBoolean()
            ? special[data.consumeInt(0, special.length - 1)]
            : (double) data.consumeInt();

        double initial = data.consumeBoolean()
            ? special[data.consumeInt(0, special.length - 1)]
            : (double) data.consumeInt();

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        if (data.consumeBoolean()) {
            max = min;
        }

        if (data.consumeBoolean()) {
            initial = min;
        } else if (data.consumeBoolean()) {
            initial = max;
        } else if (data.consumeBoolean()) {
            initial = (min + max) / 2.0d;
        }

        if (data.consumeBoolean()) {
            int delta = data.consumeInt(-3, 3);
            min += delta;
            max -= delta;
        }

        try {
            solver.solve(function, min, max, initial);
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