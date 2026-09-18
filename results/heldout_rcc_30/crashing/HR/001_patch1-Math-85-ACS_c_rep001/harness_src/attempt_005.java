package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactRegressionAnchor();

        double mean = boundedInt(data.consumeInt(), -50, 50);
        double sigma = 0.25 + boundedInt(data.consumeInt(), 1, 40) / 8.0;

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sigma);

        /*
         * Contract used by the assertions below:
         * inverseCumulativeProbability(p) is the inverse of cumulativeProbability(x)
         * on valid probabilities, so when we first build p from the SAME real
         * distribution's cumulativeProbability at a known x, a correct implementation
         * must accept that p and recover x.
         *
         * We target x = mean + 2*sigma because this is the public-API shape that
         * reaches the patched bracket condition through the real call chain.
         */
        double x1 = mean + sigma;
        double x2 = mean + 2.0 * sigma;

        Double p1 = tryCdf(dist, x1);
        Double p2 = tryCdf(dist, x2);
        if (p1 == null || p2 == null) {
            return;
        }

        if (!(p1.doubleValue() < p2.doubleValue())) {
            return;
        }
        if (!(p1.doubleValue() > 0.0 && p1.doubleValue() < 1.0 && p2.doubleValue() > 0.0 && p2.doubleValue() < 1.0)) {
            return;
        }

        Double q1 = requireInverseForConstructedProbability(dist, p1.doubleValue(), x1, "recover-x1");
        Double q2 = requireInverseForConstructedProbability(dist, p2.doubleValue(), x2, "recover-x2");
        if (q1 == null || q2 == null) {
            return;
        }

        /*
         * Independent consistency cross-check:
         * we obtained the same quantity (the spacing between two quantiles) two
         * independent ways using only real library calls:
         *   reported spacing: inverseCumulativeProbability(CDF(x2)) - inverseCumulativeProbability(CDF(x1))
         *   independent spacing: x2 - x1
         * For any correct implementation these must agree.
         */
        double reportedSpacing = q2.doubleValue() - q1.doubleValue();
        double independentSpacing = x2 - x1;
        if (Math.abs(reportedSpacing - independentSpacing) > 1.0e-10 * Math.max(1.0, Math.abs(independentSpacing))) {
            throw new RuntimeException(
                "[oracle:quantile-spacing] metamorphic violation: inv(CDF(x2))-inv(CDF(x1)) must equal x2-x1"
                    + " mean=" + mean
                    + " sigma=" + sigma
                    + " x1=" + x1
                    + " x2=" + x2
                    + " p1=" + p1
                    + " p2=" + p2
                    + " q1=" + q1
                    + " q2=" + q2
                    + " reportedSpacing=" + reportedSpacing
                    + " independentSpacing=" + independentSpacing);
        }

        /*
         * Additional monotonicity/post-condition check:
         * inverse CDF is monotone, so larger valid probabilities must map to
         * larger-or-equal quantiles.
         */
        if (!(q1.doubleValue() <= q2.doubleValue())) {
            throw new RuntimeException(
                "[oracle:quantile-monotone] metamorphic violation: inverse CDF must be monotone"
                    + " mean=" + mean
                    + " sigma=" + sigma
                    + " p1=" + p1
                    + " p2=" + p2
                    + " q1=" + q1
                    + " q2=" + q2);
        }
    }

    private static void runExactRegressionAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        try {
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:seed-result] metamorphic violation: regression seed must recover quantile 2.0"
                        + " p=" + p
                        + " result=" + result);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:seed-valid-probability] metamorphic violation: inverse CDF rejected a valid probability from the public regression seed",
                    t);
            }
        }
    }

    private static Double tryCdf(NormalDistributionImpl dist, double x) {
        try {
            return new Double(dist.cumulativeProbability(x));
        } catch (Throwable t) {
            return null;
        }
    }

    private static Double requireInverseForConstructedProbability(
            NormalDistributionImpl dist, double p, double expected, String oracleId) {
        try {
            double q = dist.inverseCumulativeProbability(p);
            double tol = 1.0e-10 * Math.max(1.0, Math.abs(expected));
            if (Math.abs(q - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x"
                        + " p=" + p
                        + " expected=" + expected
                        + " actual=" + q);
            }
            return new Double(q);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isRootCauseFromBracket(t)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: valid probability built from cumulativeProbability(x) was rejected by inverseCumulativeProbability"
                        + " p=" + p
                        + " expected=" + expected,
                    t);
            }
            return null;
        }
    }

    private static boolean isRootCauseFromBracket(Throwable t) {
        if (!hasMathExceptionInChain(t)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String m = stack[i].getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                        && "inverseCumulativeProbability".equals(m))) {
                return true;
            }
        }
        Throwable c = t.getCause();
        while (c != null) {
            stack = c.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                String cls = stack[i].getClassName();
                String m = stack[i].getMethodName();
                if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                        || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                            && "inverseCumulativeProbability".equals(m))) {
                    return true;
                }
            }
            c = c.getCause();
        }
        return false;
    }

    private static boolean hasMathExceptionInChain(Throwable t) {
        Throwable c = t;
        while (c != null) {
            if (c instanceof MathException) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable c = t;
        while (c != null) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String n = c.getClass().getName();
            if (n.indexOf("IllegalArgument") >= 0 || n.indexOf("Invalid") >= 0) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }

    private static int boundedInt(int v, int min, int max) {
        long span = (long) max - (long) min + 1L;
        long mod = v % span;
        if (mod < 0) {
            mod += span;
        }
        return (int) (min + mod);
    }
}