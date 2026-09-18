package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = boundedDouble(data.consumeInt(), -50.0, 50.0);
        double sd = boundedPositive(data.consumeInt(), 0.125, 8.0);

        int which = data.consumeInt(0, 2);
        double z;
        if (which == 0) {
            z = 2.0;
        } else if (which == 1) {
            z = -2.0;
        } else {
            int n = data.consumeInt(-6, 6);
            z = (double) n;
        }

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);
        double target = mean + sd * z;

        try {
            double pExact = dist.cumulativeProbability(target);
            if (!(pExact > 0.0 && pExact < 1.0)) {
                return;
            }
            exploreAdjacentProbabilities(dist, target, pExact);
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-exact-2] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 for the documented failing seed; got " + result);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exploreAdjacentProbabilities(NormalDistribution dist, double target, double pExact) {
        double tol = 1.0e-8 * Math.max(1.0, Math.abs(target));

        try {
            double qExact = dist.inverseCumulativeProbability(pExact);
            if (Math.abs(qExact - target) > tol) {
                throw new RuntimeException("[oracle:adjacent-straddle] metamorphic violation: for p=cdf(x), inverseCumulativeProbability(p) must recover x; x=" + target + " p=" + pExact + " q=" + qExact);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            } else {
                return;
            }
        }

        double pBelow = Math.nextAfter(pExact, 0.0);
        double pAbove = Math.nextAfter(pExact, 1.0);
        if (!(pBelow > 0.0 && pBelow < pExact && pExact < pAbove && pAbove < 1.0)) {
            return;
        }

        Double qBelow = tryInverse(dist, pBelow);
        Double qAbove = tryInverse(dist, pAbove);
        if (qBelow == null || qAbove == null) {
            return;
        }

        /*
         * Sound oracle: inverseCumulativeProbability is the inverse of cumulativeProbability
         * on valid probabilities, and the normal CDF is monotone increasing.
         * Therefore for adjacent representable probabilities around p=cdf(target),
         * the returned quantiles must straddle target: qBelow <= target <= qAbove.
         * A patch that merely suppresses the historical throw but returns the wrong side
         * still violates this observable post-condition.
         */
        if (!(qBelow.doubleValue() <= target && target <= qAbove.doubleValue())) {
            throw new RuntimeException("[oracle:adjacent-straddle] metamorphic violation: quantiles for adjacent probabilities must straddle the known target; target=" + target + " pBelow=" + pBelow + " qBelow=" + qBelow + " pExact=" + pExact + " pAbove=" + pAbove + " qAbove=" + qAbove);
        }

        try {
            double back = dist.cumulativeProbability(target);
            if (Math.abs(back - pExact) > 1.0e-15) {
                throw new RuntimeException("[oracle:fresh-cdf-recompute] consistency violation: cumulativeProbability(target) changed across recomputation target=" + target + " first=" + pExact + " second=" + back);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static Double tryInverse(NormalDistribution dist, double p) {
        try {
            return new Double(dist.inverseCumulativeProbability(p));
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            }
            return null;
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                return true;
            }
        }
        if (isCleanRejection(t)) {
            return false;
        }
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasRelevantStack(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasRelevantStack(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            String cls = f.getClassName();
            String m = f.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(m)) {
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
            String n = cur.getClass().getName();
            if (n.indexOf("Invalid") >= 0 || n.indexOf("Illegal") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long spanBits = ((long) Integer.MAX_VALUE) - ((long) Integer.MIN_VALUE);
        double unit = (((double) raw) - (double) Integer.MIN_VALUE) / spanBits;
        return min + (max - min) * unit;
    }

    private static double boundedPositive(int raw, double min, double max) {
        double v = boundedDouble(raw, min, max);
        if (v <= 0.0) {
            return min;
        }
        return v;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>doThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doThrow(Throwable t) throws T {
        throw (T) t;
    }
}