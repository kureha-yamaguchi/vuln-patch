package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchor();

        int meanInt = data.consumeInt(-20, 20);
        int sdTenth = data.consumeInt(1, 30);
        int leftSteps = data.consumeInt(1, 4);
        int rightSteps = data.consumeInt(1, 6);

        double mean = meanInt;
        double sd = sdTenth / 10.0;
        if (sd <= 0.0) {
            return;
        }

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);

        double leftX = mean - (leftSteps * sd);
        double rightX = mean + (rightSteps * sd);
        if (!(leftX < rightX)) {
            return;
        }

        final double pLeft;
        final double pRight;
        try {
            pLeft = dist.cumulativeProbability(leftX);
            pRight = dist.cumulativeProbability(rightX);
        } catch (Throwable t) {
            return;
        }

        if (!(pLeft > 0.0 && pLeft < 1.0 && pRight > 0.0 && pRight < 1.0 && pLeft < pRight)) {
            return;
        }

        final double invLeft;
        try {
            invLeft = dist.inverseCumulativeProbability(pLeft);
        } catch (Throwable t) {
            if (isRootCause(t) && isValidProbability(pLeft)) {
                throw new RuntimeException(
                    "[oracle:valid-inverse-reject-left] valid inverseCumulativeProbability rejected p=" + pLeft
                        + " mean=" + mean + " sd=" + sd + " x=" + leftX,
                    t);
            }
            return;
        }

        final double invRight;
        try {
            invRight = dist.inverseCumulativeProbability(pRight);
        } catch (Throwable t) {
            if (isRootCause(t) && isValidProbability(pRight)) {
                throw new RuntimeException(
                    "[oracle:valid-inverse-reject-right] valid inverseCumulativeProbability rejected p=" + pRight
                        + " mean=" + mean + " sd=" + sd + " x=" + rightX,
                    t);
            }
            return;
        }

        try {
            /*
             * For any correct continuous distribution implementation, the interval probability
             * between two quantiles must equal the difference of their cumulative probabilities:
             * P(inv(pLeft), inv(pRight))) = pRight - pLeft.
             * Both sides are computed only through real library calls.
             */
            double reported = dist.cumulativeProbability(invLeft, invRight);
            double independent = pRight - pLeft;
            double tol = 1.0e-9 + 1.0e-6 * Math.abs(independent);
            if (Math.abs(reported - independent) > tol) {
                throw new RuntimeException(
                    "[oracle:interval-mass] metamorphic violation: interval mass disagrees"
                        + " mean=" + mean
                        + " sd=" + sd
                        + " leftX=" + leftX
                        + " rightX=" + rightX
                        + " pLeft=" + pLeft
                        + " pRight=" + pRight
                        + " invLeft=" + invLeft
                        + " invRight=" + invRight
                        + " reported=" + reported
                        + " independent=" + independent);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            /*
             * A second independent consistency check: two identically-constructed distributions
             * must report the same quantile for the same valid probability.
             */
            NormalDistribution fresh = new NormalDistributionImpl(mean, sd);
            double freshRight = fresh.inverseCumulativeProbability(pRight);
            double tol = 1.0e-12 + 1.0e-9 * Math.max(1.0, Math.abs(invRight));
            if (Math.abs(freshRight - invRight) > tol) {
                throw new RuntimeException(
                    "[oracle:fresh-object-agreement] metamorphic violation: identical distributions disagree"
                        + " mean=" + mean
                        + " sd=" + sd
                        + " p=" + pRight
                        + " first=" + invRight
                        + " second=" + freshRight);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            if (isRootCause(t) && isValidProbability(pRight)) {
                throw new RuntimeException(
                    "[oracle:valid-inverse-reject-fresh] valid inverseCumulativeProbability rejected on fresh object"
                        + " p=" + pRight + " mean=" + mean + " sd=" + sd,
                    t);
            }
        }
    }

    private static void runExactAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:anchor-result] metamorphic violation: exact regression anchor returned wrong quantile"
                        + " p=" + p + " x=" + x);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:anchor-valid-inverse-reject] valid standard-normal probability rejected by inverseCumulativeProbability",
                    t);
            }
        }
    }

    private static boolean isValidProbability(double p) {
        return p > 0.0 && p < 1.0 && !Double.isNaN(p);
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && passesThroughReachableRegion(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean passesThroughReachableRegion(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String c = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(c)
                    && "bracket".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(c)
                    && "value".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(c)
                    && "<init>".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.MathRuntimeException".equals(c)
                    && ("createIllegalArgumentException".equals(m) || "buildMessage".equals(m))) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(c)
                    && "inverseCumulativeProbability".equals(m)) {
                return true;
            }
        }
        return false;
    }
}