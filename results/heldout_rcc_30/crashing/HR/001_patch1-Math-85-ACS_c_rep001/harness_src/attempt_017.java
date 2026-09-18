package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final class SkipCheck extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        verifyLiteralRegressionAnchor(normal);

        int checks = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < checks; i++) {
            int center = 2 + data.consumeInt(-4, 4);
            verifyEndpointNeighborhood(normal, center);

            if (data.consumeBoolean()) {
                int shifted = data.consumeInt(-6, 6);
                verifyEndpointNeighborhood(normal, shifted);
            }
        }
    }

    private static void verifyLiteralRegressionAnchor(NormalDistribution normal) {
        final double p = 0.9772498680518209d;
        final double expected = 2.0d;
        try {
            double q = checkedInverse(normal, p, "literal-anchor");
            if (Math.abs(q - expected) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:literal-endpoint-boundary] metamorphic violation: inverse(CDF(2)) must recover 2 on the documented valid regression input"
                    + " p=" + p + " expected=" + expected + " actual=" + q);
            }
        } catch (SkipCheck ignored) {
            return;
        }
    }

    private static void verifyEndpointNeighborhood(NormalDistribution normal, double x) {
        try {
            double p = normal.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d)) {
                return;
            }

            double pDown = Math.nextAfter(p, 0.0d);
            double pUp = Math.nextAfter(p, 1.0d);
            if (!(pDown >= 0.0d && pUp <= 1.0d && pDown < p && p < pUp)) {
                return;
            }

            double qExact = checkedInverse(normal, p, "exact-x=" + x);
            double qDown = checkedInverse(normal, pDown, "down-x=" + x);
            double qUp = checkedInverse(normal, pUp, "up-x=" + x);

            /* Contract used:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct only valid inputs p = CDF(x), so a correct implementation must recover x.
             * A throw-deleting patch or an overfit boundary check can silently return a wrong quantile here.
             */
            if (Math.abs(qExact - x) > 1.0e-9d) {
                throw new RuntimeException(
                    "[oracle:constructed-fixedpoint-boundary] metamorphic violation: inverse(cumulativeProbability(x)) must recover x"
                    + " x=" + x + " p=" + p + " inverse=" + qExact);
            }

            /* Independent local check around the patched boundary:
             * inverseCumulativeProbability is monotone in p, so for adjacent probabilities around p
             * we must have inverse(pDown) <= inverse(p) <= inverse(pUp).
             * This probes the old/new fa*fb condition exactly at and around an endpoint root.
             */
            if (!(qDown <= qExact && qExact <= qUp)) {
                throw new RuntimeException(
                    "[oracle:ulp-neighborhood-sandwich] metamorphic violation: quantiles must preserve probability order"
                    + " x=" + x
                    + " pDown=" + pDown + " qDown=" + qDown
                    + " p=" + p + " q=" + qExact
                    + " pUp=" + pUp + " qUp=" + qUp);
            }

            double reprobed = normal.cumulativeProbability(qExact);
            if (Math.abs(reprobed - p) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:reprobe-cdf-consistency] consistency violation: recomputed CDF at returned quantile must match original probability"
                    + " x=" + x + " p=" + p + " q=" + qExact + " reprobed=" + reprobed);
            }
        } catch (MathException e) {
            if (isValidationLike(e)) {
                return;
            }
            if (hitsPatchedRegion(e)) {
                throw new RuntimeException(
                    "[oracle:constructed-endpoint-root] metamorphic violation: valid constructed probability triggered bracket-region failure"
                    + " x=" + x, e);
            }
        } catch (SkipCheck ignored) {
            return;
        }
    }

    private static double checkedInverse(NormalDistribution normal, double p, String label) throws SkipCheck {
        try {
            return normal.inverseCumulativeProbability(p);
        } catch (MathException e) {
            if (isValidationLike(e)) {
                throw new SkipCheck();
            }
            if (hitsPatchedRegion(e)) {
                throw new RuntimeException(
                    "[oracle:inverse-on-valid-probability] metamorphic violation: inverseCumulativeProbability failed on valid constructed input"
                    + " label=" + label + " p=" + p, e);
            }
            throw new SkipCheck();
        } catch (IllegalArgumentException e) {
            throw new SkipCheck();
        }
    }

    private static boolean isValidationLike(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name.indexOf("Illegal") >= 0 || name.indexOf("Invalid") >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hitsPatchedRegion(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            StackTraceElement[] trace = c.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement ste = trace[i];
                String cls = ste.getClassName();
                String m = ste.getMethodName();
                if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                        && "bracket".equals(m))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                        && "inverseCumulativeProbability".equals(m))
                    || ("org.apache.commons.math.ConvergenceException".equals(cls)
                        && "<init>".equals(m))
                    || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                        && ("createIllegalArgumentException".equals(m) || "buildMessage".equals(m)))) {
                    return true;
                }
            }
        }
        return false;
    }
}