package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.function.Sin;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function = new Sin();
        AllowedSolution[] allowedValues = AllowedSolution.values();

        int iterations = 1 + data.consumeInt(0, 2);
        for (int i = 0; i < iterations; i++) {
            BaseSecantSolver solver;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    solver = new RegulaFalsiSolver();
                    break;
                case 1:
                    solver = new IllinoisSolver();
                    break;
                default:
                    solver = new PegasusSolver();
                    break;
            }

            int rootIndex = data.consumeInt(-3, 3);
            double center = rootIndex * Math.PI;

            double width = 0.25 + (data.consumeInt(0, 1200) / 1000.0);
            double min = center - width;
            double max = center + width;

            switch (data.consumeInt(0, 5)) {
                case 0:
                    min = center;
                    break;
                case 1:
                    max = center;
                    break;
                case 2:
                    min = center - (width * 0.5);
                    break;
                case 3:
                    max = center + (width * 0.5);
                    break;
                case 4:
                    min = center - 1.0;
                    max = center + 1.0;
                    break;
                default:
                    break;
            }

            if (min > max) {
                double t = min;
                min = max;
                max = t;
            }
            if (min == max) {
                max = min + 1.0;
            }

            AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];
            int maxEval = 32 + data.consumeInt(0, 224);
            double start = min + ((max - min) * (data.consumeInt(0, 1000) / 1000.0));

            if (data.consumeBoolean()) {
                solver.solve(maxEval, function, min, max, allowed);
            } else {
                solver.solve(maxEval, function, min, max, start, allowed);
            }
        }
    }
}