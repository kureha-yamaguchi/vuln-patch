package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runExactTestSeed();

        double mean = boundedDouble(data.consumeInt(), 64.0);
        double sd = positiveBoundedDouble(data.consumeInt(), 8.0);
        boolean upperSide = data.consumeBoolean();

        runConstructedEndpointQuantileOracle(mean, sd, upperSide);

        int extraCases = 1 + data.consumeInt(1, 4);
        for (int i = 0; i < extraCases; i++) {
            double mean2 = boundedDouble(data.consumeInt(), 64.0);
            double sd2 = positiveBoundedDouble(data.consumeInt(), 8.0);
            boolean upperSide2 = data.consumeBoolean();
            runConstructedEndpointQuantileOracle(mean2, sd2, upperSide2);
        }
    }

    private static void runExactTestSeed() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionMathFailure(t)) {
                return;
            }
        }
    }

    private static void runConstructedEndpointQuantileOracle(double mean, double sd, boolean upperSide) {
        NormalDistribution normal = new NormalDistributionImpl(mean, sd);
        double xRoot = upperSide ? (mean + sd + 1.0d) : (mean - sd - 1.0d);
        double p;
        try {
            p = normal.cumulativeProbability(xRoot);
        } catch (Throwable t) {
            return;
        }

        if (!(p > 0.0d && p < 1.0d)) {
            return;
        }

        final double x;
        try {
            x = normal.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionMathFailure(t)) {
                throw new RuntimeException("[oracle:quantile-neighbor] inverse rejected probability produced by the same distribution"
                        + " mean=" + mean + " sd=" + sd + " xRoot=" + xRoot + " p=" + p, t);
            }
            return;
        }

        try {
            /* Contract used:
             * inverseCumulativeProbability is the inverse of cumulativeProbability.
             * For p produced as cumulativeProbability(xRoot) on the same NormalDistribution,
             * a correct implementation must return a quantile x whose neighboring doubles
             * bracket p under cumulativeProbability. This cross-check is independent of the
             * exact expected x value and still catches a throw-deleting or wrong-value patch.
             */
            double prev = Math.nextAfter(x, Double.NEGATIVE_INFINITY);
            double next = Math.nextAfter(x, Double.POSITIVE_INFINITY);
            double cPrev = normal.cumulativeProbability(prev);
            double cAt = normal.cumulativeProbability(x);
            double cNext = normal.cumulativeProbability(next);

            if (!(cPrev <= p && p <= cNext)) {
                throw new RuntimeException("[oracle:quantile-neighbor] metamorphic violation: neighbor CDF bracket failed"
                        + " mean=" + mean + " sd=" + sd + " p=" + p
                        + " x=" + x + " cPrev=" + cPrev + " cAt=" + cAt + " cNext=" + cNext);
            }

            double localSpan = Math.max(Math.abs(cNext - cPrev), 1e-15d);
            double err = Math.abs(cAt - p);
            if (err > localSpan * 8.0d) {
                throw new RuntimeException("[oracle:quantile-neighbor] metamorphic violation: returned quantile is not locally consistent"
                        + " mean=" + mean + " sd=" + sd + " p=" + p
                        + " x=" + x + " cAt=" + cAt + " cPrev=" + cPrev + " cNext=" + cNext
                        + " err=" + err + " span=" + localSpan);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isPatchedRegionMathFailure(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(cls)
                    && "<init>".equals(method)) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return isPatchedRegionMathFailure(cause);
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static double boundedDouble(int raw, double scale) {
        return (raw % 1000000) / scale;
    }

    private static double positiveBoundedDouble(int raw, double scale) {
        double v = Math.abs(raw % 1000000) / scale;
        if (v < 0.125d) {
            v += 0.125d;
        }
        return v;
    }
}