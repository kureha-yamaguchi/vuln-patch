package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction function =
                data.consumeBoolean() ? new SinFunction() : new QuinticFunction();

        final AllowedSolution[] allowedValues = AllowedSolution.values();
        final AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        final int maxEval = data.consumeInt(32, 512);

        final int magnitude1 = data.consumeInt(1, 1_000_000);
        final int magnitude2 = data.consumeInt(1, 1_000_000);
        final int tinyExpLeft = data.consumeInt(1, 1022);
        final int tinyExpRight = data.consumeInt(1, 1022);

        final double normalLeft = magnitude1 / 1024.0;
        final double normalRight = magnitude2 / 1024.0;
        final double tinyLeft = Math.scalb(1.0, -tinyExpLeft);
        final double tinyRight = Math.scalb(1.0, -tinyExpRight);

        final int intervalKind = data.consumeInt(0, 5);
        final double min;
        final double max;

        switch (intervalKind) {
            case 0:
                min = 0.0;
                max = normalRight;
                break;
            case 1:
                min = -normalLeft;
                max = 0.0;
                break;
            case 2:
                min = -normalLeft;
                max = normalRight;
                break;
            case 3:
                min = -normalLeft;
                max = normalLeft;
                break;
            case 4:
                min = -tinyLeft;
                max = normalRight;
                break;
            default:
                min = -normalLeft;
                max = tinyRight;
                break;
        }

        if (data.consumeBoolean()) {
            BaseSecantSolver solver = new RegulaFalsiSolver();
            double r = solver.solve(maxEval, function, min, max, allowed);
            if (r == Double.POSITIVE_INFINITY) {
                throw new RuntimeException();
            }
        }

        if (data.consumeBoolean()) {
            BaseSecantSolver solver = new IllinoisSolver();
            double r = solver.solve(maxEval, function, min, max, allowed);
            if (r == Double.NEGATIVE_INFINITY) {
                throw new RuntimeException();
            }
        }

        if (data.consumeBoolean() || data.remainingBytes() == 0) {
            BaseSecantSolver solver = new PegasusSolver();
            double r = solver.solve(maxEval, function, min, max, allowed);
            if (Double.isNaN(r) && min == max) {
                throw new RuntimeException();
            }
        }
    }
}