package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double CHECK_TOLERANCE = 1.0e-12;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        endpointEqualityDifferentialOracle(2.0, true);

        int mode = data.consumeInt(0, 3);
        double k;
        switch (mode) {
            case 0:
                k = data.consumeInt(2, 8);
                break;
            case 1:
                k = data.consumeInt(2, 8) + 0.0;
                break;
            case 2:
                k = data.consumeInt(2, 8) + 0.5;
                break;
            default:
                k = 2.0 + data.consumeInt(0, 24) / 4.0;
                break;
        }

        endpointEqualityDifferentialOracle(k, false);

        int extraChecks = data.consumeInt(0, 3);
        for (int i = 0; i < extraChecks; i++) {
            double kk = 2.0 + data.consumeInt(0, 24) / 4.0;
            endpointEqualityDifferentialOracle(kk, false);
        }
    }

    private static void endpointEqualityDifferentialOracle(double expectedX, boolean anchor) {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double p = normal.cumulativeProbability(expectedX);

            double qExact;
            try {
                qExact = normal.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseMathException(t)) {
                    throw new RuntimeException(
                            "[oracle:endpoint-mass-zero] inverseCumulativeProbability rejected a valid endpoint-root input"
                                    + " anchor=" + anchor
                                    + " expectedX=" + expectedX
                                    + " p=" + p,
                            t);
                }
                return;
            }

            // Contract behind this check:
            // We constructed p using the same distribution's cumulativeProbability(expectedX).
            // For any correct inverse CDF implementation on a continuous normal distribution,
            // inverseCumulativeProbability(p) must recover the same quantile, so the probability
            // mass between expectedX and the returned value must be zero. A patch that merely
            // suppresses the throw but returns the wrong point breaks this observable post-condition.
            try {
                double lo = Math.min(expectedX, qExact);
                double hi = Math.max(expectedX, qExact);
                double intervalMass = normal.cumulativeProbability(lo, hi);
                if (Math.abs(intervalMass) > CHECK_TOLERANCE) {
                    throw new RuntimeException(
                            "[oracle:endpoint-mass-zero] metamorphic violation: interval mass between constructed endpoint"
                                    + " and recovered quantile must be zero"
                                    + " anchor=" + anchor
                                    + " expectedX=" + expectedX
                                    + " p=" + p
                                    + " qExact=" + qExact
                                    + " intervalMass=" + intervalMass);
                }
            } catch (MathException ignored) {
                return;
            }

            // Flip the patched boundary: exact endpoint root vs probabilities immediately adjacent
            // to it. For a correct monotone inverse CDF, probabilities below/above p must map to
            // quantiles on the corresponding side of the exact endpoint.
            double pDown = Math.nextAfter(p, 0.0);
            double pUp = Math.nextAfter(p, 1.0);

            if (pDown > 0.0 && pUp < 1.0 && pDown < p && p < pUp) {
                try {
                    double qDown = normal.inverseCumulativeProbability(pDown);
                    double qUp = normal.inverseCumulativeProbability(pUp);
                    if (!(qDown <= expectedX + CHECK_TOLERANCE && qUp >= expectedX - CHECK_TOLERANCE)) {
                        throw new RuntimeException(
                                "[oracle:endpoint-neighbor-order] metamorphic violation: neighboring probabilities must bracket"
                                        + " the exact endpoint quantile"
                                        + " anchor=" + anchor
                                        + " expectedX=" + expectedX
                                        + " pDown=" + pDown
                                        + " p=" + p
                                        + " pUp=" + pUp
                                        + " qDown=" + qDown
                                        + " qExact=" + qExact
                                        + " qUp=" + qUp);
                    }
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCauseMathException(t)) {
                        throw new RuntimeException(
                                "[oracle:endpoint-neighbor-order] valid neighboring probability hit endpoint-root bug"
                                        + " anchor=" + anchor
                                        + " expectedX=" + expectedX
                                        + " p=" + p,
                                t);
                    }
                }
            }
        } catch (MathException ignored) {
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(ste.getClassName())
                    && "value".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(ste.getClassName())
                    && "<init>".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.MathRuntimeException".equals(ste.getClassName())
                    && ste.getMethodName() != null
                    && ste.getMethodName().startsWith("createIllegalArgumentException")) {
                return true;
            }
            if ("org.apache.commons.math.MathRuntimeException".equals(ste.getClassName())
                    && "buildMessage".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}