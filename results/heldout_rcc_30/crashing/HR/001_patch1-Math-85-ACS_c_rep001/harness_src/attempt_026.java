package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runRegressionWitness();
        runAffineEndpointConstruction(data);
        exerciseValidationAndDirectBracketPaths(data);
    }

    private static void runRegressionWitness() {
        org.apache.commons.math.distribution.NormalDistributionImpl normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);
        final double p = 0.9772498680518209;
        final double expected = 2.0;
        try {
            double got = normal.inverseCumulativeProbability(p);

            try {
                double witness = normal.cumulativeProbability(expected);
                if (Math.abs(witness - p) > 1.0e-15) {
                    return;
                }
            } catch (Throwable ignored) {
                return;
            }

            if (Math.abs(got - expected) > 1.0e-12) {
                throw new RuntimeException(
                        "[oracle:seed-known-answer] metamorphic violation: inverse(CDF-known-point) must recover the known point"
                                + " p=" + p + " expected=" + expected + " got=" + got);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathRootCauseFromInverseOrBracket(t)) {
                throw new RuntimeException(
                        "[oracle:seed-known-answer] metamorphic violation: valid regression input threw from inverse/bracket"
                                + " p=" + p + " expected=" + expected, t);
            }
        }
    }

    private static void runAffineEndpointConstruction(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000);
        int sigmaNum = data.consumeInt(1, 400);
        int sigmaDen = data.consumeInt(1, 20);
        double sigma = ((double) sigmaNum) / ((double) sigmaDen);

        org.apache.commons.math.distribution.NormalDistributionImpl dist =
                new org.apache.commons.math.distribution.NormalDistributionImpl(mean, sigma);

        double target = mean + (2.0 * sigma);

        final double p;
        try {
            p = dist.cumulativeProbability(target);
        } catch (Throwable t) {
            return;
        }

        if (!(p > 0.5 && p < 1.0)) {
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            // Contract used for this oracle:
            // We construct p from the same real object's cumulativeProbability(target),
            // so inverseCumulativeProbability(p) must recover that target on a correct implementation.
            // A patch that merely suppresses the throw but returns the wrong endpoint violates this.
            if (Math.abs(inv - target) > 1.0e-10) {
                throw new RuntimeException(
                        "[oracle:constructed-affine-endpoint] metamorphic violation: inverse(cdf(target)) must recover target"
                                + " mean=" + mean + " sigma=" + sigma + " p=" + p
                                + " target=" + target + " inverse=" + inv);
            }

            try {
                double reprobed = dist.cumulativeProbability(inv);
                if (Math.abs(reprobed - p) > 1.0e-10) {
                    throw new RuntimeException(
                            "[oracle:constructed-affine-reprobe] consistency violation: cdf(inverse(p)) must reproduce p"
                                    + " mean=" + mean + " sigma=" + sigma + " p=" + p
                                    + " inverse=" + inv + " reprobed=" + reprobed);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathRootCauseFromInverseOrBracket(t)) {
                throw new RuntimeException(
                        "[oracle:constructed-affine-endpoint] metamorphic violation: valid inverse(cdf(target)) threw"
                                + " mean=" + mean + " sigma=" + sigma + " p=" + p + " target=" + target, t);
            }
        }
    }

    private static void exerciseValidationAndDirectBracketPaths(FuzzedDataProvider data) {
        try {
            UnivariateRealSolverUtils.bracket(null, 0.0, 0.0, 1.0, 1);
        } catch (Throwable t) {
            if (!isCleanRejection(t) && isMathRootCauseFromBracket(t)) {
                return;
            }
        }

        try {
            UnivariateRealSolverUtils.bracket(
                    new org.apache.commons.math.analysis.SinFunction(),
                    0.0,
                    0.0,
                    1.0,
                    0);
        } catch (Throwable t) {
            if (!isCleanRejection(t) && isMathRootCauseFromBracket(t)) {
                return;
            }
        }

        double lower = data.consumeInt(-20, 20);
        double upper = lower + data.consumeInt(1, 20);
        double initial = lower + data.consumeInt(0, (int) (upper - lower));
        int chooser = data.consumeInt(0, 1);

        try {
            if (chooser == 0) {
                UnivariateRealSolverUtils.bracket(
                        new org.apache.commons.math.analysis.SinFunction(),
                        initial,
                        lower,
                        upper);
            } else {
                UnivariateRealSolverUtils.bracket(
                        new org.apache.commons.math.analysis.SinFunction(),
                        initial,
                        lower,
                        upper,
                        data.consumeInt(1, 8));
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathRootCauseFromBracket(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid") || name.contains("IllegalArgument")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isMathRootCauseFromInverseOrBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof org.apache.commons.math.MathException && hasInverseOrBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isMathRootCauseFromBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if ((cur instanceof org.apache.commons.math.MathException
                    || cur instanceof org.apache.commons.math.ConvergenceException)
                    && hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasInverseOrBracketFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String c = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(c)
                    && "bracket".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(c)
                    && "inverseCumulativeProbability".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}