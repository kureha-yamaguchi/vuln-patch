package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkAllowedSolutionIrrelevantForSeedIssue();
        exploreBoundaryVariants(data);
    }

    private static void checkAllowedSolutionIrrelevantForSeedIssue() {
        final int maxEval = 3624;
        final double min = 1.0;
        final double max = 10.0;

        for (AllowedSolution allowed : AllowedSolution.values()) {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean threw = false;
            try {
                solver.solve(maxEval, ISSUE631_FUNCTION, min, max, allowed);
            } catch (TooManyEvaluationsException expected) {
                threw = true;
                // Contract justification:
                // AllowedSolution only selects which side is acceptable once a solution is ready.
                // If the seed issue does not converge within maxEval, changing allowedSolution must not
                // turn non-convergence into a successful solve. A band-aid that only restores ANY_SIDE
                // would still be wrong for the other allowed values.
                double reportedMin = solver.getMin();
                double reportedMax = solver.getMax();
                if (reportedMin != min || reportedMax != max) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:allowed-nonconvergent-state] semantic mismatch: allowed=" + allowed +
                        " reportedMin=" + reportedMin + " reportedMax=" + reportedMax +
                        " expectedMin=" + min + " expectedMax=" + max, expected);
                }
                solver.getAbsoluteAccuracy();
                solver.getRelativeAccuracy();
                solver.getFunctionValueAccuracy();
            } catch (Throwable t) {
                return;
            }
            if (!threw) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:allowed-nonconvergence] semantic mismatch: expected TooManyEvaluationsException for allowed=" +
                    allowed + " on issue631 seed");
            }
        }
    }

    private static void exploreBoundaryVariants(FuzzedDataProvider data) {
        final double root = Math.log(Math.pow(Math.PI, 3.0));
        final int budgetJitter = data.consumeInt(-8, 8);
        final int maxEval = 3624 + budgetJitter;

        double absAccuracy = positiveAccuracy(data);
        double relAccuracy = positiveAccuracy(data);
        double fvalAccuracy = positiveAccuracy(data);

        int intervalChoice = data.consumeInt(0, 5);
        double min = 1.0;
        double max;
        switch (intervalChoice) {
            case 0:
                max = 10.0;
                break;
            case 1:
                max = Math.nextUp(10.0);
                break;
            case 2:
                max = Math.nextAfter(10.0, Double.NEGATIVE_INFINITY);
                break;
            case 3:
                max = root + 1.0 + data.consumeInt(0, 3);
                break;
            case 4:
                max = 10.0 + data.consumeInt(0, 3);
                break;
            default:
                max = Math.nextUp(root + 1.0);
                break;
        }

        if (!(min < root && root < max)) {
            max = 10.0;
        }

        AllowedSolution allowed = allowedFromInt(data.consumeInt());
        RegulaFalsiSolver solver = buildSolver(data.consumeInt(0, 3), relAccuracy, absAccuracy, fvalAccuracy);

        try {
            if (data.consumeBoolean()) {
                double start = min + (max - min) * 0.5;
                solver.solve(Math.max(1, maxEval), ISSUE631_FUNCTION, min, max, start, allowed);
            } else {
                solver.solve(Math.max(1, maxEval), ISSUE631_FUNCTION, min, max, allowed);
            }
        } catch (Throwable ignored) {
        }

        try {
            solver.getMin();
            solver.getMax();
            solver.getAbsoluteAccuracy();
            solver.getRelativeAccuracy();
            solver.getFunctionValueAccuracy();
        } catch (Throwable ignored) {
        }
    }

    private static RegulaFalsiSolver buildSolver(int which, double rel, double abs, double fval) {
        switch (which) {
            case 0:
                return new RegulaFalsiSolver();
            case 1:
                return new RegulaFalsiSolver(abs);
            case 2:
                return new RegulaFalsiSolver(rel, abs);
            default:
                return new RegulaFalsiSolver(rel, abs, fval);
        }
    }

    private static AllowedSolution allowedFromInt(int v) {
        AllowedSolution[] vals = AllowedSolution.values();
        int idx = v % vals.length;
        if (idx < 0) {
            idx += vals.length;
        }
        return vals[idx];
    }

    private static double positiveAccuracy(FuzzedDataProvider data) {
        int pow = data.consumeInt(1, 12);
        return Math.pow(10.0, -pow);
    }
}