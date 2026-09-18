package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input first.
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: exact regression input should invert to 2.0 input=" + p + " lhs=" + result + " rhs=2.0");
            }
            // Contract/oracle: for a continuous distribution, cumulativeProbability(inverseCumulativeProbability(p))
            // should recover p for valid p in (0,1). A throw-deleting patch that returns a wrong value breaks this.
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: cdf(icdf(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // EXPLORE: build valid distributions and valid probabilities by construction.
        // Root-cause property: choose p = CDF(mu + 2*sigma), so the true inverse is exactly an endpoint
        // that the buggy bracketing code can hit with f(endpoint) == 0.
        int meanTenths = data.consumeInt(-100, 100);
        int sigmaTenths = data.consumeInt(1, 100);
        double mean = meanTenths / 10.0;
        double sigma = sigmaTenths / 10.0;

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sigma);
            double x = mean + 2.0 * sigma;
            double p = normal.cumulativeProbability(x);
            double result = normal.inverseCumulativeProbability(p);

            // Contract/oracle: inverse cumulative is the inverse of cumulative for valid probabilities.
            if (Math.abs(result - x) > 1.0e-9) {
                throw new RuntimeException("[oracle:explore-rt] metamorphic violation: icdf(cdf(x)) == x input=" + x + " lhs=" + result + " rhs=" + x);
            }

            // Equivalent round-trip from the probability side for the same valid input.
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:explore-cdf] metamorphic violation: cdf(icdf(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t == null) {
            return;
        }
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(f.getClassName())
                    && "bracket".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.apache.commons.math.")
                    && (name.contains("Illegal") || name.contains("Invalid") || name.contains("OutOfRange"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}