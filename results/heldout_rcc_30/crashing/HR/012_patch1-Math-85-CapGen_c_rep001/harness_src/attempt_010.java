package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exactSeedOracle();

        double mean = data.consumeInt(-64, 64) / 4.0;
        double delta = data.consumeInt(1, 16) / 16.0;

        endpointHoleOracle(mean, delta);

        int extra = data.consumeInt(1, 3);
        for (int i = 0; i < extra; i++) {
            double m = data.consumeInt(-128, 128) / 8.0;
            double d = data.consumeInt(1, 32) / 32.0;
            endpointHoleOracle(m, d);
        }
    }

    private static void exactSeedOracle() {
        NormalDistribution dist = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209d;

        try {
            double x = dist.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:seed-exact] metamorphic violation: inverse(cdf seed) should recover 2.0 input=" + p + " lhs=" + x + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:seed-exact] metamorphic violation: valid inverse(cdf seed) threw from patched region input=" + p, t);
            }
        }

        try {
            double pFromX = dist.cumulativeProbability(2.0d);
            double x2 = dist.inverseCumulativeProbability(pFromX);
            if (Math.abs(x2 - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:seed-roundtrip-built] metamorphic violation: inverse(cumulativeProbability(2.0)) should recover 2.0 input=" + pFromX + " lhs=" + x2 + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:seed-roundtrip-built] metamorphic violation: valid inverse(cumulativeProbability(2.0)) threw from patched region", t);
            }
        }
    }

    private static void endpointHoleOracle(double mean, double delta) {
        NormalDistribution dist = new NormalDistributionImpl(mean, 1.0d);

        double centerX = mean + 2.0d;
        double leftX = centerX - delta;
        double rightX = centerX + delta;

        double leftP;
        double centerP;
        double rightP;
        try {
            leftP = dist.cumulativeProbability(leftX);
            centerP = dist.cumulativeProbability(centerX);
            rightP = dist.cumulativeProbability(rightX);
        } catch (Throwable t) {
            return;
        }

        Double leftInv = tryInverse(dist, leftP);
        Double centerInv = tryInverse(dist, centerP);
        Double rightInv = tryInverse(dist, rightP);

        if (leftInv == null || rightInv == null) {
            return;
        }

        if (Math.abs(leftInv.doubleValue() - leftX) > 1.0e-9d) {
            throw new RuntimeException("[oracle:endpoint-hole-left] metamorphic violation: inverse(cdf(x-delta)) should recover x-delta input=" + leftP + " lhs=" + leftInv + " rhs=" + leftX);
        }
        if (Math.abs(rightInv.doubleValue() - rightX) > 1.0e-9d) {
            throw new RuntimeException("[oracle:endpoint-hole-right] metamorphic violation: inverse(cdf(x+delta)) should recover x+delta input=" + rightP + " lhs=" + rightInv + " rhs=" + rightX);
        }

        if (centerInv == null) {
            throw new RuntimeException("[oracle:endpoint-hole-mid] metamorphic violation: for a continuous normal distribution, inverse(cumulativeProbability(x)) must accept this valid probability; neighbors succeeded but center failed mean=" + mean + " delta=" + delta + " x=" + centerX + " p=" + centerP + " left=" + leftInv + " right=" + rightInv);
        }

        if (Math.abs(centerInv.doubleValue() - centerX) > 1.0e-9d) {
            throw new RuntimeException("[oracle:endpoint-hole-mid] metamorphic violation: inverse(cdf(x)) should recover x at the patched boundary input=" + centerP + " lhs=" + centerInv + " rhs=" + centerX);
        }

        if (!(leftInv.doubleValue() < centerInv.doubleValue() && centerInv.doubleValue() < rightInv.doubleValue())) {
            throw new RuntimeException("[oracle:endpoint-order] metamorphic violation: inverse CDF of a normal distribution is strictly increasing on probabilities generated from x-delta < x < x+delta mean=" + mean + " delta=" + delta + " lhs=" + leftInv + "," + centerInv + "," + rightInv + " rhs=" + leftX + "," + centerX + "," + rightX);
        }
    }

    private static Double tryInverse(NormalDistribution dist, double p) {
        try {
            return Double.valueOf(dist.inverseCumulativeProbability(p));
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isRootCauseMathException(t)) {
                return null;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        boolean mathFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException || "org.apache.commons.math.ConvergenceException".equals(cur.getClass().getName())) {
                mathFamily = true;
            }
            if (hasBracketFrame(cur)) {
                return mathFamily || cur instanceof MathException;
            }
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(ste.getClassName())
                    && "inverseCumulativeProbability".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}