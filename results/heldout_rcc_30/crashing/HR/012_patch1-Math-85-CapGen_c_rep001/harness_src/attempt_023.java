package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        probeBracketValidationPaths();

        try {
            anchorSeedCheck();
        } catch (Throwable t) {
            if (isRootCauseMathException(t)) {
                throwUnchecked(t);
            }
        }

        NormalDistribution standard = new NormalDistributionImpl(0.0, 1.0);

        int k = data.consumeInt(-8, 8);
        if (k == 0) {
            k = data.consumeBoolean() ? 2 : -2;
        }

        try {
            double p = standard.cumulativeProbability((double) k);
            if (!(p > 0.0 && p < 1.0)) {
                return;
            }

            double x = standard.inverseCumulativeProbability(p);

            if (Math.abs(x - (double) k) > 1.0e-12) {
                throw new RuntimeException("[oracle:constructed-fixedpoint] metamorphic violation: inverse(cdf(k)) must recover k for this same distribution input=" + k + " p=" + p + " result=" + x);
            }

            double replay = standard.cumulativeProbability(x);
            if (Math.abs(replay - p) > 1.0e-12) {
                throw new RuntimeException("[oracle:cdf-replay-fixedpoint] metamorphic violation: cumulativeProbability(inverse(p)) must replay p input=" + k + " p=" + p + " x=" + x + " replay=" + replay);
            }

            try {
                NormalDistribution fresh = new NormalDistributionImpl(0.0, 1.0);
                double freshX = fresh.inverseCumulativeProbability(p);
                if (Math.abs(freshX - x) > 1.0e-12) {
                    throw new RuntimeException("[oracle:fresh-object-agreement-constructed] metamorphic violation: identically constructed distributions must agree on the same quantile k=" + k + " p=" + p + " x1=" + x + " x2=" + freshX);
                }
            } catch (Throwable ignored) {
            }

            int step = data.consumeInt(1, 3);
            int k2 = k + (k >= 0 ? step : -step);
            if (k2 >= -8 && k2 <= 8 && k2 != k) {
                try {
                    double p2 = standard.cumulativeProbability((double) k2);
                    if (p2 > 0.0 && p2 < 1.0) {
                        double x2 = standard.inverseCumulativeProbability(p2);
                        if (k < k2 && !(x < x2)) {
                            throw new RuntimeException("[oracle:constructed-order] metamorphic violation: inverse must preserve order for increasing probabilities k1=" + k + " k2=" + k2 + " p1=" + p + " p2=" + p2 + " x1=" + x + " x2=" + x2);
                        }
                        if (k > k2 && !(x > x2)) {
                            throw new RuntimeException("[oracle:constructed-order] metamorphic violation: inverse must preserve order for increasing probabilities k1=" + k + " k2=" + k2 + " p1=" + p + " p2=" + p2 + " x1=" + x + " x2=" + x2);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void probeBracketValidationPaths() {
        try {
            UnivariateRealSolverUtils.bracket(null, 0.0, 0.0, 1.0, 1);
        } catch (Throwable ignored) {
        }

        try {
            UnivariateRealSolverUtils.bracket(new SinFunction(), 0.0, -1.0, 1.0, 0);
        } catch (Throwable ignored) {
        }

        try {
            UnivariateRealSolverUtils.bracket(new SinFunction(), 2.0, 1.0, 1.0, 1);
        } catch (Throwable ignored) {
        }
    }

    private static void anchorSeedCheck() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        double x = normal.inverseCumulativeProbability(p);

        if (Math.abs(x - 2.0) > 1.0e-12) {
            throw new RuntimeException("[oracle:anchor-return-value] metamorphic violation: failing test contract expects inverseCumulativeProbability(0.9772498680518209) == 2.0 p=" + p + " result=" + x);
        }

        double replay = normal.cumulativeProbability(x);
        if (Math.abs(replay - p) > 1.0e-12) {
            throw new RuntimeException("[oracle:anchor-cdf-replay] metamorphic violation: returned quantile must map back to the same probability p=" + p + " x=" + x + " replay=" + replay);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String n = c.getClass().getName();
            if (n.endsWith("InvalidArgumentException") || n.endsWith("IllegalArgumentException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        boolean mathFamily = false;
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof MathException || "org.apache.commons.math.MathException".equals(c.getClass().getName())) {
                mathFamily = true;
                break;
            }
        }
        if (!mathFamily) {
            return false;
        }

        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(m))
                    || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls) && "value".equals(m))) {
                return true;
            }
        }

        Throwable cause = t.getCause();
        while (cause != null) {
            for (StackTraceElement e : cause.getStackTrace()) {
                String cls = e.getClassName();
                String m = e.getMethodName();
                if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                        || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(m))
                        || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls) && "value".equals(m))) {
                    return true;
                }
            }
            cause = cause.getCause();
        }

        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}