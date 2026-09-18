package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runAffineExplore(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);

            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:anchor-exact-value] metamorphic violation: inverseCumulativeProbability must recover the documented regression quantile "
                    + "input=" + p + " result=" + result + " expected=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAffineExplore(FuzzedDataProvider data) {
        int z = data.consumeInt(1, 6);
        int muInt = data.consumeInt(-50, 50);
        int sigmaInt = data.consumeInt(1, 20);

        double mu = (double) muInt;
        double sigma = (double) sigmaInt;

        NormalDistribution standard = new NormalDistributionImpl(0, 1);
        NormalDistribution shifted = new NormalDistributionImpl(mu, sigma);

        double p;
        try {
            double xStandard = (double) z;
            p = standard.cumulativeProbability(xStandard);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double invStandard;
        try {
            invStandard = standard.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                sneakyThrow(t);
            }
            return;
        }

        double invShifted;
        try {
            invShifted = shifted.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                sneakyThrow(t);
            }
            return;
        }

        double expectedShifted = mu + sigma * invStandard;
        double tol = 1.0e-10d * Math.max(1.0d, Math.max(Math.abs(expectedShifted), Math.abs(invShifted)));

        /*
         * Contract/oracle: a NormalDistributionImpl with mean mu and standard deviation sigma
         * is the affine transform of the standard normal, so for the same valid probability p,
         * quantiles must satisfy Q_{mu,sigma}(p) = mu + sigma * Q_{0,1}(p).
         * This is an observable post-condition on the public API; a patch that merely suppresses
         * the bracket failure but returns the wrong quantile will violate it.
         */
        if (Math.abs(invShifted - expectedShifted) > tol) {
            throw new RuntimeException(
                "[oracle:normal-affine-quantile] metamorphic violation: affine quantile relation "
                + "p=" + p
                + " mu=" + mu
                + " sigma=" + sigma
                + " stdInv=" + invStandard
                + " shiftedInv=" + invShifted
                + " expectedShifted=" + expectedShifted
                + " tol=" + tol);
        }

        /*
         * Independent post-condition from the same public API: for p built from the standard
         * distribution's own CDF at an exact integer z, inverseCumulativeProbability must recover
         * that same z. This uses only real library calls and valid-by-construction p in (0,1).
         */
        if (Math.abs(invStandard - (double) z) > 1.0e-12d) {
            throw new RuntimeException(
                "[oracle:cdf-constructed-fixedpoint] metamorphic violation: inverse(CDF(z)) must recover z "
                + "z=" + z + " p=" + p + " inv=" + invStandard);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("IllegalArgument") >= 0 || name.indexOf("NumberFormat") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && stackPassesThroughBracket(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement e = frames[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}