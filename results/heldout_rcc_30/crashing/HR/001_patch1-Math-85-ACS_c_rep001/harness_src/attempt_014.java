package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchorFirst();
        runExplore(data);
    }

    private static void runExactAnchorFirst() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact-two] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2 for the documented regression input p="
                        + p + " result=" + result);
            }
            checkMirrorConsistency(normal, p, result, 1.0e-12d);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (MathException e) {
            if (isRootCause(e)) {
                throwUnchecked(e);
            }
        } catch (Exception e) {
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        double mean = boundedDouble(data.consumeInt(), 50.0d);
        double sigma = Math.abs(boundedDouble(data.consumeInt(), 20.0d)) + 0.25d;
        int xi = data.consumeInt(-8, 8);
        double x = xi + boundedDouble(data.consumeInt(), 0.25d);
        boolean useAnchorStandard = data.consumeBoolean();

        if (useAnchorStandard) {
            mean = 0.0d;
            sigma = 1.0d;
            x = 2.0d;
        }

        NormalDistribution dist = new NormalDistributionImpl(mean, sigma);
        try {
            double p = dist.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d)) {
                return;
            }

            double inv = dist.inverseCumulativeProbability(p);

            if (Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException("[oracle:constructed-quantile-recovery] metamorphic violation: for any correct continuous distribution inverseCumulativeProbability(cumulativeProbability(x)) must recover x on constructed valid input"
                        + " mean=" + mean + " sigma=" + sigma + " x=" + x + " p=" + p + " inv=" + inv);
            }

            checkMirrorConsistency(dist, p, inv, 1.0e-9d);

            NormalDistribution fresh = new NormalDistributionImpl(mean, sigma);
            double invFresh = fresh.inverseCumulativeProbability(p);
            if (Math.abs(invFresh - inv) > 1.0e-12d) {
                throw new RuntimeException("[oracle:fresh-recompute-quantile] consistency violation: identically constructed objects must report the same quantile"
                        + " mean=" + mean + " sigma=" + sigma + " p=" + p + " lhs=" + inv + " rhs=" + invFresh);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (MathException e) {
            if (isRootCause(e)) {
                throwUnchecked(e);
            }
        } catch (Exception e) {
        }
    }

    private static void checkMirrorConsistency(NormalDistribution dist, double p, double inv, double tol)
            throws MathException {
        double q = 1.0d - p;
        if (!(q > 0.0d && q < 1.0d)) {
            return;
        }
        double mirror = dist.inverseCumulativeProbability(q);
        double mean = dist.getMean();
        double lhs = inv + mirror;
        double rhs = 2.0d * mean;
        if (Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException("[oracle:mirror-quantile-center] consistency violation: for any normal distribution, symmetric tail quantiles are centered at the mean"
                    + " p=" + p + " inv=" + inv + " q=" + q + " mirror=" + mirror + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static double boundedDouble(int raw, double scale) {
        return (raw % 100000) / 100000.0d * scale;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method))
                    || ("org.apache.commons.math.distribution.NormalDistributionImpl".equals(cls)
                    && "inverseCumulativeProbability".equals(method))) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            for (StackTraceElement ste : cause.getStackTrace()) {
                String cls = ste.getClassName();
                String method = ste.getMethodName();
                if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                        && "bracket".equals(method))
                        || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                        && "inverseCumulativeProbability".equals(method))
                        || ("org.apache.commons.math.distribution.NormalDistributionImpl".equals(cls)
                        && "inverseCumulativeProbability".equals(method))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}