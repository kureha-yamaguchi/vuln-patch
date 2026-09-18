package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boundaryFlipOracle(0.0, 1.0, 2.0, true);

        int k = data.consumeInt(-8, 8);
        int meanInt = data.consumeInt(-20, 20);
        int sdNumerator = data.consumeInt(1, 20);
        int sdDenominator = data.consumeInt(1, 10);

        double mean = meanInt;
        double sd = ((double) sdNumerator) / ((double) sdDenominator);
        double target = mean + sd * k;

        boundaryFlipOracle(mean, sd, target, false);

        if (data.consumeBoolean()) {
            double target2 = mean - sd * k;
            boundaryFlipOracle(mean, sd, target2, false);
        }
    }

    private static void boundaryFlipOracle(double mean, double sd, double target, boolean anchor) {
        NormalDistributionImpl dist;
        try {
            dist = new NormalDistributionImpl(mean, sd);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                throwUnchecked(t);
            }
            return;
        }

        final double p;
        try {
            p = dist.cumulativeProbability(target);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                throwUnchecked(t);
            }
            return;
        }

        final double qExact;
        try {
            qExact = dist.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:flip-eq] inverseCumulativeProbability rejected a valid-by-construction probability built as cumulativeProbability(target); "
                                + "mean=" + mean + " sd=" + sd + " target=" + target + " p=" + p + " anchor=" + anchor,
                        t);
            }
            if (!isCleanRejection(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // Contract used: inverseCumulativeProbability is the quantile function.
        // For p built from cumulativeProbability(target) on the same distribution,
        // a correct implementation must recover target (up to floating-point tolerance).
        double tol = Math.max(1e-12, Math.abs(target) * 1e-9 + sd * 1e-9);
        if (Math.abs(qExact - target) > tol) {
            throw new RuntimeException(
                    "[oracle:flip-eq] metamorphic violation: inverse(cumulativeProbability(x)) != x"
                            + " mean=" + mean + " sd=" + sd + " target=" + target
                            + " p=" + p + " qExact=" + qExact + " tol=" + tol);
        }

        // Flip the patched boundary: at p exactly equal to a CDF value, the old code
        // mis-handled an endpoint root. Nearby probabilities should bracket the exact quantile.
        double pDown = Math.nextAfter(p, 0.0d);
        double pUp = Math.nextAfter(p, 1.0d);

        if (!(pDown > 0.0d && pDown < 1.0d && pUp > 0.0d && pUp < 1.0d)) {
            return;
        }

        try {
            double qDown = dist.inverseCumulativeProbability(pDown);
            double qUp = dist.inverseCumulativeProbability(pUp);

            if (!(qDown <= qExact && qExact <= qUp)) {
                throw new RuntimeException(
                        "[oracle:flip-neighbor] metamorphic violation: neighboring quantiles do not bracket the exact quantile"
                                + " mean=" + mean + " sd=" + sd + " target=" + target
                                + " pDown=" + pDown + " p=" + p + " pUp=" + pUp
                                + " qDown=" + qDown + " qExact=" + qExact + " qUp=" + qUp);
            }

            if (!(qDown <= target + tol && qUp >= target - tol)) {
                throw new RuntimeException(
                        "[oracle:flip-neighbor] metamorphic violation: neighboring probabilities built around cumulativeProbability(target) do not surround target"
                                + " mean=" + mean + " sd=" + sd + " target=" + target
                                + " pDown=" + pDown + " p=" + p + " pUp=" + pUp
                                + " qDown=" + qDown + " qExact=" + qExact + " qUp=" + qUp
                                + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (!isCleanRejection(t) && !isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasReachableFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasReachableFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                            && "inverseCumulativeProbability".equals(m))
                    || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls) && "value".equals(m))
                    || ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(m))
                    || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                            && ("createIllegalArgumentException".equals(m) || "buildMessage".equals(m)))) {
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
            if (name.contains("Invalid") || name.contains("Illegal") || name.contains("OutOfRange")
                    || name.contains("NoData") || name.contains("NotStrictlyPositive")
                    || name.contains("NullArgument")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}