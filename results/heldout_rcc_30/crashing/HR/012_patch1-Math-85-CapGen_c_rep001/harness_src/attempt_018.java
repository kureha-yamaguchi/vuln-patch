package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorIntervalMassOracle();

        int loops = 1 + Math.max(0, data.consumeInt(0, 6));
        for (int i = 0; i < loops; i++) {
            exploreIntervalMassConsistency(data);
        }
    }

    private static void anchorIntervalMassOracle() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double x = 2.0;

            double pDirect = normal.cumulativeProbability(x);
            double pViaInterval = 0.5 + normal.cumulativeProbability(0.0, x);

            if (Math.abs(pDirect - pViaInterval) > 1.0e-15) {
                throw new RuntimeException(
                    "[oracle:interval-mass-anchor] metamorphic violation: standard-normal CDF must equal 0.5 + P(0,X) for X>=0 inputX="
                        + x + " direct=" + pDirect + " interval=" + pViaInterval);
            }

            try {
                double q = normal.inverseCumulativeProbability(pViaInterval);
                if (Math.abs(q - x) > 1.0e-12) {
                    throw new RuntimeException(
                        "[oracle:interval-inverse-anchor] metamorphic violation: inverse(CDF rebuilt from interval mass) must recover the same x inputX="
                            + x + " p=" + pViaInterval + " q=" + q);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseThrowable(t)) {
                    throw new RuntimeException(
                        "[oracle:interval-inverse-anchor] metamorphic violation: inverse rejected a valid probability rebuilt from the distribution's own interval mass inputX="
                            + x + " p=" + pViaInterval, t);
                }
            }
        } catch (MathException e) {
        }
    }

    private static void exploreIntervalMassConsistency(FuzzedDataProvider data) {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);

            int mode = data.consumeInt(0, 4);
            double x;
            if (mode == 0) {
                x = data.consumeInt(-6, 6);
                if (x == 0.0) {
                    x = 2.0;
                }
            } else if (mode == 1) {
                x = data.consumeInt(-6, 6) + (data.consumeInt(0, 1000) / 1000.0);
                if (x == 0.0) {
                    x = 2.0;
                }
            } else if (mode == 2) {
                x = data.consumeInt(-6, 6) - (data.consumeInt(0, 1000) / 1000.0);
                if (x == 0.0) {
                    x = -2.0;
                }
            } else if (mode == 3) {
                int n = data.consumeInt(1, 6);
                x = data.consumeBoolean() ? n : -n;
            } else {
                x = (data.consumeInt(-6000, 6000)) / 1000.0;
                if (x == 0.0) {
                    x = 1.0;
                }
            }

            double pDirect = normal.cumulativeProbability(x);
            double pViaInterval;
            if (x >= 0.0) {
                pViaInterval = 0.5 + normal.cumulativeProbability(0.0, x);
            } else {
                pViaInterval = 0.5 - normal.cumulativeProbability(x, 0.0);
            }

            if (Math.abs(pDirect - pViaInterval) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:interval-mass-fuzz] metamorphic violation: CDF must agree with decomposition through interval probability inputX="
                        + x + " direct=" + pDirect + " interval=" + pViaInterval);
            }

            if (!(pViaInterval > 0.0 && pViaInterval < 1.0)) {
                return;
            }

            try {
                double q = normal.inverseCumulativeProbability(pViaInterval);

                double pRebuilt;
                if (q >= 0.0) {
                    pRebuilt = 0.5 + normal.cumulativeProbability(0.0, q);
                } else {
                    pRebuilt = 0.5 - normal.cumulativeProbability(q, 0.0);
                }

                if (Math.abs(pRebuilt - pViaInterval) > 1.0e-10) {
                    throw new RuntimeException(
                        "[oracle:interval-rebuild] metamorphic violation: probability rebuilt from inverse result must match the requested probability inputX="
                            + x + " requestedP=" + pViaInterval + " q=" + q + " rebuiltP=" + pRebuilt);
                }

                if (isIntegerLike(x) && x > 0.0 && Math.abs(q - x) > 1.0e-12) {
                    throw new RuntimeException(
                        "[oracle:endpoint-integer-recovery] metamorphic violation: for probability built from an exact integer support point, inverse must recover that same point inputX="
                            + x + " p=" + pViaInterval + " q=" + q);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseThrowable(t) && isValidProbability(pViaInterval)) {
                    throw new RuntimeException(
                        "[oracle:interval-valid-probability] metamorphic violation: inverse rejected a valid probability produced by cumulativeProbability inputX="
                            + x + " p=" + pViaInterval, t);
                }
            }
        } catch (MathException e) {
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
        }
    }

    private static boolean isIntegerLike(double x) {
        return Math.abs(x - Math.rint(x)) <= 0.0;
    }

    private static boolean isValidProbability(double p) {
        return p > 0.0 && p < 1.0 && !Double.isNaN(p);
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NullArgument")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasRelevantFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasRelevantFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method))
                || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls)
                    && "value".equals(method))
                || ("org.apache.commons.math.ConvergenceException".equals(cls)
                    && "<init>".equals(method))
                || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                    && "buildMessage".equals(method))
                || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                    && "createIllegalArgumentException".equals(method))) {
                return true;
            }
        }
        return false;
    }
}