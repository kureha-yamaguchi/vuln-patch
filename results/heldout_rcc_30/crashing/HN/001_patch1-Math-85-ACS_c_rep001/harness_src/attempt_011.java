package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing-test contract input=0.9772498680518209 result=" + result + " expected=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000) / 10.0d;
        double sd = Math.max(0.1d, data.consumeInt(1, 200) / 10.0d);
        int n = data.consumeInt(-8, 8);
        if (n == 0) {
            n = 2;
        }

        NormalDistribution dist = new NormalDistributionImpl(mean, sd);
        double x = mean + (sd * n);
        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (Throwable t) {
            return;
        }

        if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            if (Double.isNaN(inv) || Double.isInfinite(inv)) {
                throw new RuntimeException("[oracle:norm-inv] metamorphic violation: inverse returned non-finite inputP=" + p + " mean=" + mean + " sd=" + sd + " expectedX=" + x + " actual=" + inv);
            }

            /* Contract asserted:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p by calling the real cumulativeProbability(x), so a correct implementation
             * must return the original x (up to numerical tolerance). A patch that merely suppresses
             * the throw and returns a wrong value breaks this observable relation.
             */
            double tolX = Math.max(1.0e-9d, Math.abs(sd) * 1.0e-9d);
            if (Math.abs(inv - x) > tolX) {
                throw new RuntimeException("[oracle:norm-inv] metamorphic violation: inverse(cdf(x)) != x inputX=" + x + " mean=" + mean + " sd=" + sd + " p=" + p + " actual=" + inv + " tol=" + tolX);
            }

            double back;
            try {
                back = dist.cumulativeProbability(inv);
            } catch (Throwable t) {
                return;
            }

            double tolP = 1.0e-9d;
            if (Double.isNaN(back) || Math.abs(back - p) > tolP) {
                throw new RuntimeException("[oracle:norm-rt] metamorphic violation: cdf(inv(p)) != p mean=" + mean + " sd=" + sd + " x=" + x + " p=" + p + " inv=" + inv + " back=" + back + " tol=" + tolP);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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
            if (name != null) {
                if (name.contains("Illegal") || name.contains("Invalid")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}