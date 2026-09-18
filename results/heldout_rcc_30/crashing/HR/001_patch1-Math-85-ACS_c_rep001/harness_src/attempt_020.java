package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeed();

        NormalDistributionImpl dist = new NormalDistributionImpl(0.0, 1.0);
        final double exactX = 2.0;
        final double exactP;
        try {
            exactP = dist.cumulativeProbability(exactX);
        } catch (Throwable t) {
            return;
        }

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int ulps = data.consumeInt(0, 16);
            boolean useExactLeft = data.consumeBoolean();
            boolean useExactRight = data.consumeBoolean();
            int deltaTicks = data.consumeInt(1, 64);

            double leftP = useExactLeft ? exactP : moveUlps(exactP, ulps == 0 ? 1 : ulps, false);
            double rightP = useExactRight ? exactP : moveUlps(exactP, deltaTicks, true);

            if (!(leftP > 0.0 && leftP < 1.0 && rightP > 0.0 && rightP < 1.0)) {
                continue;
            }
            if (!(leftP <= rightP)) {
                double tmp = leftP;
                leftP = rightP;
                rightP = tmp;
            }

            checkQuantileIntervalMass(dist, leftP, rightP);
        }
    }

    private static void runAnchorSeed() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable ignored) {
        }
    }

    private static void checkQuantileIntervalMass(NormalDistributionImpl dist, double pLeft, double pRight) {
        final double xLeft;
        final double xRight;
        try {
            xLeft = dist.inverseCumulativeProbability(pLeft);
            xRight = dist.inverseCumulativeProbability(pRight);
        } catch (Throwable t) {
            if (isValidConstructedProbability(pLeft, pRight) && reachesPatchedRegion(t)) {
                throw new RuntimeException(
                    "[oracle:quantile-interval-mass] valid constructed probability rejected: pLeft="
                        + pLeft + " pRight=" + pRight,
                    t);
            }
            return;
        }

        try {
            /*
             * For any correct continuous distribution, if xLeft = F^{-1}(pLeft) and xRight = F^{-1}(pRight),
             * then the interval probability reported by the same object must satisfy
             * P(xLeft < X <= xRight) = F(xRight) - F(xLeft) = pRight - pLeft.
             * This remains true even if a crash-only patch merely suppresses the throw:
             * the returned quantiles must still be consistent with the distribution's own interval mass.
             */
            double reportedMass = dist.cumulativeProbability(xLeft, xRight);
            double expectedMass = pRight - pLeft;
            double tol = 1.0e-12 + 1.0e-8 * Math.max(Math.abs(expectedMass), Math.abs(reportedMass));
            if (Math.abs(reportedMass - expectedMass) > tol) {
                throw new RuntimeException(
                    "[oracle:quantile-interval-mass] metamorphic violation: interval mass mismatch"
                        + " pLeft=" + pLeft
                        + " pRight=" + pRight
                        + " xLeft=" + xLeft
                        + " xRight=" + xRight
                        + " reportedMass=" + reportedMass
                        + " expectedMass=" + expectedMass);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isValidConstructedProbability(double pLeft, double pRight) {
        return pLeft >= 0.0 && pLeft <= 1.0 && pRight >= 0.0 && pRight <= 1.0 && pLeft <= pRight;
    }

    private static double moveUlps(double value, int steps, boolean up) {
        double v = value;
        for (int i = 0; i < steps; i++) {
            v = up ? Math.nextUp(v) : Math.nextAfter(v, Double.NEGATIVE_INFINITY);
        }
        return v;
    }

    private static boolean reachesPatchedRegion(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            for (StackTraceElement ste : cur.getStackTrace()) {
                String cls = ste.getClassName();
                String method = ste.getMethodName();
                if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                        && "bracket".equals(method))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                        && "inverseCumulativeProbability".equals(method))
                    || ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(method))
                    || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                        && ("buildMessage".equals(method)
                            || "createIllegalArgumentException".equals(method)))) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}