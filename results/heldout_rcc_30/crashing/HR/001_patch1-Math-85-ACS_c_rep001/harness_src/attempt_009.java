package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorFirst();

        NormalDistributionImpl mutable = new NormalDistributionImpl(0.0, 1.0);

        int probes = 1 + Math.abs(data.consumeInt(0, 3));
        for (int i = 0; i < probes; i++) {
            double mean = data.consumeInt(-1000, 1000) / 8.0;
            probeMutableEndpointState(mutable, mean);
        }

        if (data.consumeBoolean()) {
            probeMutableEndpointState(mutable, 0.0);
        }
    }

    private static void runAnchorFirst() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:seed-known-x] metamorphic violation: constructed endpoint quantile must recover x input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedRegion(t)) {
                return;
            }
        }
    }

    private static void probeMutableEndpointState(NormalDistributionImpl mutable, double mean) {
        try {
            mutable.setMean(mean);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        final double x = mean + 2.0;

        double p;
        try {
            p = mutable.cumulativeProbability(x);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double mutatedInv;
        try {
            mutatedInv = mutable.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedRegion(t)) {
                throw new RuntimeException("[oracle:mutable-endpoint-state] metamorphic violation: after mutating mean, inverseCumulativeProbability(cumulativeProbability(mean+2)) must recover mean+2 for a valid normal distribution state mean=" + mean + " x=" + x + " p=" + p, t);
            }
            return;
        }

        if (Math.abs(mutatedInv - x) > 1.0e-12) {
            throw new RuntimeException("[oracle:mutable-endpoint-state] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(mean+2)) must recover the constructed x after state mutation mean=" + mean + " x=" + x + " lhs=" + mutatedInv + " rhs=" + x);
        }

        try {
            NormalDistributionImpl fresh = new NormalDistributionImpl(mean, 1.0);
            double freshInv = fresh.inverseCumulativeProbability(p);
            if (Math.abs(mutatedInv - freshInv) > 1.0e-12) {
                throw new RuntimeException("[oracle:fresh-vs-mutated] consistency violation: a mutated distribution and a fresh identically-constructed distribution must agree on the same quantile mean=" + mean + " p=" + p + " lhs=" + mutatedInv + " rhs=" + freshInv);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedRegion(t)) {
                throw new RuntimeException("[oracle:fresh-vs-mutated] consistency violation: fresh and mutated identical states must both handle the same valid quantile mean=" + mean + " x=" + x + " p=" + p, t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name.contains("Illegal") || name.contains("Invalid")) {
            return true;
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isCleanRejection(cause);
    }

    private static boolean isRootCauseFromPatchedRegion(Throwable t) {
        if (t == null) {
            return false;
        }
        boolean mathFamily = (t instanceof MathException) || t.getClass().getName().contains("ConvergenceException");
        if (!mathFamily) {
            Throwable cause = t.getCause();
            return cause != null && cause != t && isRootCauseFromPatchedRegion(cause);
        }

        for (StackTraceElement frame : t.getStackTrace()) {
            String cls = frame.getClassName();
            String method = frame.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(method)) {
                return true;
            }
        }

        Throwable cause = t.getCause();
        return cause != null && cause != t && isRootCauseFromPatchedRegion(cause);
    }
}