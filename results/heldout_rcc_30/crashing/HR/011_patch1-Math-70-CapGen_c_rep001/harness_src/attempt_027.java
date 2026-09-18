package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        explore(data);
    }

    private static void runAnchor() {
        try {
            exerciseSinCase(3.0, 3.2, 3.1);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        int n = data.consumeInt(-64, 64);
        double root = n * Math.PI;

        double left = positiveWidth(data);
        double right = positiveWidth(data);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double span = max - min;
            double bias = boundedUnit(data) * span * 0.49;
            initial = root + bias;
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }
        }

        try {
            exerciseSinCase(min, max, initial);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void exerciseSinCase(double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        org.apache.commons.math.analysis.UnivariateRealFunction f =
                new org.apache.commons.math.analysis.SinFunction();

        try {
            double result = solver.solve(f, min, max, initial);

            if (result < min || result > max) {
                throw new RuntimeException(
                        "[oracle:result-in-interval] metamorphic violation: returned root escaped bracket"
                                + " min=" + min + " max=" + max + " result=" + result);
            }

            /*
             * Bisection halves the bracket once per loop and stores the loop index i in setResult(m, i)
             * immediately after the successful halving. Therefore, after a successful solve on an initial
             * interval of width W, a correct implementation must satisfy:
             *     W <= absoluteAccuracy * 2^(iterationCount + 1)
             * A throw-deleting or stale-result patch can return a value without performing the required
             * number of halvings, and then this independently recomputed width/iteration relation breaks.
             */
            int iterations = solver.getIterationCount();
            double accuracy = solver.getAbsoluteAccuracy();
            double width = max - min;
            double achievedBound = accuracy * Math.pow(2.0, iterations + 1);
            if (!(width <= achievedBound * 1.0000000001)) {
                throw new RuntimeException(
                        "[oracle:iter-width] metamorphic violation: bisection iteration count inconsistent"
                                + " width=" + width
                                + " accuracy=" + accuracy
                                + " iterations=" + iterations
                                + " bound=" + achievedBound
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " result=" + result);
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException) {
                return;
            }
            if (t instanceof RuntimeException) {
                return;
            }
            return;
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
                || name.contains("FunctionEvaluation")
                || name.contains("MaxIterationsExceeded")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("NotPositive")
                || name.contains("OutOfRange");
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 4);
        if (selector == 0) {
            return 1.0e-6 * (data.consumeInt(1, 1000));
        } else if (selector == 1) {
            return 1.0e-3 * (data.consumeInt(1, 1000));
        } else if (selector == 2) {
            return 0.1 + data.consumeInt(0, 900) / 1000.0;
        } else if (selector == 3) {
            return 1.0 + data.consumeInt(0, 900) / 1000.0;
        } else {
            return 0.2;
        }
    }

    private static double boundedUnit(FuzzedDataProvider data) {
        return data.consumeInt(-1000000, 1000000) / 1000000.0;
    }
}