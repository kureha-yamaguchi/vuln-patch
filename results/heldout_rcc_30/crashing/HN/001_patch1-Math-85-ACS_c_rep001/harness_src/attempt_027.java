package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double TEST_P = 0.9772498680518209d;
    private static final double TEST_EXPECTED = 2.0d;
    private static final double STRICT_EPS = 1.0e-12d;
    private static final double ROUNDTRIP_EPS = 1.0e-9d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution anchorDist = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double result = anchorDist.inverseCumulativeProbability(TEST_P);
            if (Math.abs(result - TEST_EXPECTED) > STRICT_EPS) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 inputP="
                        + TEST_P + " result=" + result + " expected=" + TEST_EXPECTED);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        int iterations = data.consumeInt(1, 4);
        for (int i = 0; i < iterations; i++) {
            double mean = boundedDouble(data.consumeInt(), -10.0d, 10.0d);
            double sd = boundedPositive(data.consumeInt(), 0.1d, 10.0d);
            int z = data.consumeInt(0, 8);

            NormalDistribution dist = new NormalDistributionImpl(mean, sd);

            try {
                double x = mean + sd * z;
                double p = dist.cumulativeProbability(x);

                if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
                    return;
                }

                double recovered = dist.inverseCumulativeProbability(p);

                /* Contract/oracle:
                 * For a continuous distribution, inverseCumulativeProbability(cumulativeProbability(x))
                 * should recover x (up to numerical tolerance) for valid probabilities.
                 * A patch that merely deletes the throw or otherwise changes the branch to return a
                 * wrong value would violate this observable round-trip relation without crashing.
                 */
                if (Math.abs(recovered - x) > ROUNDTRIP_EPS) {
                    throw new RuntimeException("[oracle:norm-icdf-roundtrip] metamorphic violation: inverse(cdf(x)) != x mean="
                            + mean + " sd=" + sd + " x=" + x + " p=" + p + " recovered=" + recovered);
                }

                double p2 = dist.cumulativeProbability(recovered);
                if (Math.abs(p2 - p) > ROUNDTRIP_EPS) {
                    throw new RuntimeException("[oracle:norm-cdf-consistency] metamorphic violation: cdf(inverse(p)) != p mean="
                            + mean + " sd=" + sd + " x=" + x + " p=" + p + " recovered=" + recovered + " recdf=" + p2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        Throwable cur = t;
        while (cur != null) {
            if (hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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
            if (name != null) {
                if (name.endsWith("InvalidRepresentationException")
                        || name.endsWith("InvalidMatrixException")
                        || name.endsWith("NotPositiveException")
                        || name.endsWith("NotStrictlyPositiveException")
                        || name.endsWith("OutOfRangeException")
                        || name.endsWith("NoBracketingException")
                        || name.endsWith("NullArgumentException")
                        || name.endsWith("IllegalArgumentException")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        double span = max - min;
        long nonNeg = raw & 0x7fffffffL;
        return min + (nonNeg % 1000000L) * (span / 999999.0d);
    }

    private static double boundedPositive(int raw, double min, double max) {
        double v = boundedDouble(raw, min, max);
        return v <= 0.0d ? min : v;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}