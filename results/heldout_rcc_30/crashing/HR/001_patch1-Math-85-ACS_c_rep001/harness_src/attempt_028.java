package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exactRegressionWitness();
        variedTwoSigmaEndpoint(data);
        medianIsMeanOracle(data);
    }

    private static void exactRegressionWitness() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        final double p = 0.9772498680518209;
        try {
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:seed-literal-quantile] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2 on the regression seed input p=" + p + " got=" + x);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException("[oracle:seed-literal-quantile] metamorphic violation: valid regression seed should be accepted and recover 2.0 p=" + p, t);
            }
        }
    }

    private static void variedTwoSigmaEndpoint(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000) + (data.consumeByte() / 32.0);
        double sigma = data.consumeInt(1, 1000) + ((data.consumeByte() & 0xff) / 255.0);
        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sigma);

        double expectedX = mean + 2.0 * sigma;
        double p;
        try {
            p = dist.cumulativeProbability(expectedX);
        } catch (Throwable t) {
            return;
        }

        try {
            double got = dist.inverseCumulativeProbability(p);
            double tol = 1.0e-6 * Math.max(1.0, Math.abs(expectedX));
            if (Math.abs(got - expectedX) > tol) {
                throw new RuntimeException("[oracle:two-sigma-endpoint] metamorphic violation: inverse(cumulativeProbability(mean+2*sigma)) must recover mean+2*sigma mean=" + mean + " sigma=" + sigma + " p=" + p + " expected=" + expectedX + " got=" + got);
            }
            double reprobed;
            try {
                reprobed = dist.cumulativeProbability(got);
            } catch (Throwable t) {
                return;
            }
            if (Math.abs(reprobed - p) > 1.0e-9) {
                throw new RuntimeException("[oracle:two-sigma-cdf-reprobe] consistency violation: returned quantile must map back to the same probability mean=" + mean + " sigma=" + sigma + " p=" + p + " got=" + got + " reprobed=" + reprobed);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException("[oracle:two-sigma-endpoint] metamorphic violation: valid normal inverse input constructed from the same distribution should not fail mean=" + mean + " sigma=" + sigma + " x=" + expectedX + " p=" + p, t);
            }
        }
    }

    private static void medianIsMeanOracle(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000) + (data.consumeByte() / 16.0);
        double sigma = data.consumeInt(1, 1000) + 0.5;
        NormalDistributionImpl a = new NormalDistributionImpl(mean, sigma);
        NormalDistributionImpl b = new NormalDistributionImpl(mean, sigma);

        try {
            double qa = a.inverseCumulativeProbability(0.5);
            double qb = b.inverseCumulativeProbability(0.5);
            double tol = 1.0e-12 * Math.max(1.0, Math.abs(mean));
            if (Math.abs(qa - mean) > tol || Math.abs(qb - mean) > tol || Math.abs(qa - qb) > tol) {
                throw new RuntimeException("[oracle:median-mean-fixedpoint] consistency violation: for any normal distribution the 0.5 quantile is the mean; two identically constructed objects must agree mean=" + mean + " sigma=" + sigma + " qa=" + qa + " qb=" + qb);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException("[oracle:median-mean-fixedpoint] metamorphic violation: valid median query on a normal distribution should not fail mean=" + mean + " sigma=" + sigma, t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid")
                    || name.contains("Illegal")
                    || name.contains("OutOfRange")
                    || name.contains("NoData")
                    || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBracketRootCause(Throwable t) {
        boolean mathFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException || cur.getClass().getName().endsWith("ConvergenceException")) {
                mathFamily = true;
                break;
            }
        }
        if (!mathFamily) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        while (cause != null) {
            for (StackTraceElement ste : cause.getStackTrace()) {
                String cls = ste.getClassName();
                String method = ste.getMethodName();
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method)) {
                    return true;
                }
                if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                        && "inverseCumulativeProbability".equals(method)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}