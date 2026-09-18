package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function = new SinFunction();

        double min = data.consumeInt();
        double max = data.consumeInt();
        double start = data.consumeInt();

        if (data.consumeBoolean()) {
            min = -min;
        }
        if (data.consumeBoolean()) {
            max = -max;
        }
        if (data.consumeBoolean()) {
            start = -start;
        }

        int d1 = data.consumeInt();
        int d2 = data.consumeInt();
        int d3 = data.consumeInt();

        if (data.consumeBoolean()) {
            min /= (double) (Math.abs(d1) + 1);
        }
        if (data.consumeBoolean()) {
            max /= (double) (Math.abs(d2) + 1);
        }
        if (data.consumeBoolean()) {
            start /= (double) (Math.abs(d3) + 1);
        }

        switch (data.consumeInt(0, 8)) {
            case 0:
                min = 0.0;
                break;
            case 1:
                max = 0.0;
                break;
            case 2:
                start = 0.0;
                break;
            case 3:
                min = max;
                break;
            case 4:
                max = min;
                break;
            case 5:
                start = min;
                break;
            case 6:
                start = max;
                break;
            case 7:
                min = start;
                break;
            default:
                break;
        }

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        double absoluteAccuracy = Math.abs((double) data.consumeInt());
        double relativeAccuracy = Math.abs((double) data.consumeInt());
        double functionValueAccuracy = Math.abs((double) data.consumeInt());

        int da = Math.abs(data.consumeInt()) + 1;
        int dr = Math.abs(data.consumeInt()) + 1;
        int df = Math.abs(data.consumeInt()) + 1;

        if (data.consumeBoolean()) {
            absoluteAccuracy /= (double) da;
        }
        if (data.consumeBoolean()) {
            relativeAccuracy /= (double) dr;
        }
        if (data.consumeBoolean()) {
            functionValueAccuracy /= (double) df;
        }

        if (data.consumeBoolean()) {
            absoluteAccuracy = 0.0;
        }
        if (data.consumeBoolean()) {
            relativeAccuracy = 0.0;
        }
        if (data.consumeBoolean()) {
            functionValueAccuracy = 0.0;
        }

        int maxEval = data.consumeInt();
        if (data.consumeBoolean()) {
            maxEval = Math.abs(maxEval);
        }

        AllowedSolution allowed;
        switch (data.consumeInt(0, 4)) {
            case 0:
                allowed = AllowedSolution.ANY_SIDE;
                break;
            case 1:
                allowed = AllowedSolution.LEFT_SIDE;
                break;
            case 2:
                allowed = AllowedSolution.RIGHT_SIDE;
                break;
            case 3:
                allowed = AllowedSolution.BELOW_SIDE;
                break;
            default:
                allowed = AllowedSolution.ABOVE_SIDE;
                break;
        }

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 11)) {
            case 0:
                solver = new RegulaFalsiSolver();
                break;
            case 1:
                solver = new RegulaFalsiSolver(absoluteAccuracy);
                break;
            case 2:
                solver = new RegulaFalsiSolver(relativeAccuracy, absoluteAccuracy);
                break;
            case 3:
                solver = new RegulaFalsiSolver(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
                break;
            case 4:
                solver = new IllinoisSolver();
                break;
            case 5:
                solver = new IllinoisSolver(absoluteAccuracy);
                break;
            case 6:
                solver = new IllinoisSolver(relativeAccuracy, absoluteAccuracy);
                break;
            case 7:
                solver = new IllinoisSolver(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
                break;
            case 8:
                solver = new PegasusSolver();
                break;
            case 9:
                solver = new PegasusSolver(absoluteAccuracy);
                break;
            case 10:
                solver = new PegasusSolver(relativeAccuracy, absoluteAccuracy);
                break;
            default:
                solver = new PegasusSolver(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
                break;
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                solver.solve(maxEval, function, min, max);
                break;
            case 1:
                solver.solve(maxEval, function, min, max, start);
                break;
            case 2:
                solver.solve(maxEval, function, min, max, allowed);
                break;
            default:
                solver.solve(maxEval, function, min, max, start, allowed);
                break;
        }
    }
}