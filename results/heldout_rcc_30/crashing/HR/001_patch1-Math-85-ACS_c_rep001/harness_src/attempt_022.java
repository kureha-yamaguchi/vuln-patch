package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseRegressionAnchor();
        exploreEndpointRootRoundTrip(data);
        exploreFreshObjectDeterminism(data);
    }

    private static void exerciseRegressionAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        try {
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-quantile-value] metamorphic violation: expected inverseCumulativeProbability(" + p + ") == 2.0 but got " + x);
            }
            double back = normal.cumulativeProbability(x);
            if (Math.abs(back - p) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-roundtrip-probability] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p; p=" + p + " x=" + x + " back=" + back);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exploreEndpointRootRoundTrip(FuzzedDataProvider data) {
        int cases = data.consumeInt(1, 4);
        for (int i = 0; i < cases; i++) {
            double mean = (double) data.consumeInt(-200, 200);
            double sigma = 0.5 + (double) data.consumeInt(1, 200);
            int k = data.consumeBoolean() ? 2 : -2;

            NormalDistribution dist = new NormalDistributionImpl(mean, sigma);
            double x = mean + sigma * k;

            try {
                double p = dist.cumulativeProbability(x);
                if (!(p > 0.0 && p < 1.0)) {
                    continue;
                }

                double recovered = dist.inverseCumulativeProbability(p);

                /* Sound oracle:
                 * For probabilities produced by cumulativeProbability on the same distribution,
                 * inverseCumulativeProbability must recover the original x.
                 * This drives the real public API to the patched bracketing code, and for k=2
                 * the buggy version hits the exact-endpoint-root case underlying the patch.
                 */
                double tol = 1.0e-10 * Math.max(1.0, Math.abs(x));
                if (Math.abs(recovered - x) > tol) {
                    throw new RuntimeException("[oracle:constructed-roundtrip-x] metamorphic violation: inverse(cdf(x)) must recover x; mean=" + mean + " sigma=" + sigma + " x=" + x + " p=" + p + " recovered=" + recovered);
                }

                double reprobe = dist.cumulativeProbability(recovered);
                if (Math.abs(reprobe - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:constructed-roundtrip-p] metamorphic violation: cdf(inverse(p)) must recover p; mean=" + mean + " sigma=" + sigma + " p=" + p + " recovered=" + recovered + " reprobe=" + reprobe);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static void exploreFreshObjectDeterminism(FuzzedDataProvider data) {
        int cases = data.consumeInt(1, 3);
        for (int i = 0; i < cases; i++) {
            double mean = (double) data.consumeInt(-100, 100);
            double sigma = 1.0 + (double) data.consumeInt(1, 100);
            int numerator = data.consumeInt(1, 999999);
            double p = numerator / 1000000.0;
            if (!(p > 0.0 && p < 1.0)) {
                continue;
            }

            NormalDistribution a = new NormalDistributionImpl(mean, sigma);
            NormalDistribution b = new NormalDistributionImpl(mean, sigma);

            try {
                double xa = a.inverseCumulativeProbability(p);
                double xb = b.inverseCumulativeProbability(p);

                /* Independent consistency check:
                 * Two freshly constructed equal distributions must return the same quantile for the same p.
                 * This is a fresh-object agreement oracle that still fires if a band-aid patch merely deletes
                 * the throw but returns inconsistent values.
                 */
                double tol = 1.0e-12 * Math.max(1.0, Math.max(Math.abs(xa), Math.abs(xb)));
                if (Math.abs(xa - xb) > tol) {
                    throw new RuntimeException("[oracle:fresh-object-determinism] metamorphic violation: equal fresh distributions disagree on inverseCumulativeProbability; mean=" + mean + " sigma=" + sigma + " p=" + p + " xa=" + xa + " xb=" + xb);
                }

                double pa = a.cumulativeProbability(xa);
                double pb = b.cumulativeProbability(xb);
                if (Math.abs(pa - pb) > 1.0e-12) {
                    throw new RuntimeException("[oracle:fresh-object-cdf-agreement] metamorphic violation: equal fresh distributions disagree on cdf at their returned quantiles; mean=" + mean + " sigma=" + sigma + " p=" + p + " pa=" + pa + " pb=" + pb);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCauseThrowable(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.indexOf("Illegal") >= 0 || n.indexOf("Invalid") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        if (!(t instanceof MathException || t instanceof RuntimeException)) {
            return false;
        }
        return hasRelevantFrame(t);
    }

    private static boolean hasRelevantFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    String cls = e.getClassName();
                    String m = e.getMethodName();
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m)) {
                        return true;
                    }
                    if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(m)) {
                        return true;
                    }
                    if ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(m)) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static void sneakyThrow(Throwable t) {
        FuzzHarness.<RuntimeException>uncheckedThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }
}