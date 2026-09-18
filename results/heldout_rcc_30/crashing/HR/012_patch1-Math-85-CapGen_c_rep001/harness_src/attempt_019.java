package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeed();

        int k = data.consumeInt(2, 8);
        constructedEndpointAcceptanceOracle(k);

        int signedK = data.consumeBoolean() ? k : -k;
        freshObjectEndpointConsistencyOracle(signedK);

        int loops = data.consumeInt(1, 4);
        for (int i = 0; i < loops; i++) {
            int varied = data.consumeInt(2, 8);
            if (data.consumeBoolean()) {
                varied = -varied;
            }
            constructedEndpointAcceptanceOracle(varied);
        }
    }

    private static void runAnchorSeed() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double q = normal.inverseCumulativeProbability(p);
            if (Math.abs(q - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-seed-post] metamorphic violation: exact seeded normal quantile input=" + p + " got=" + q + " expected=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                return;
            }
        }
    }

    private static void constructedEndpointAcceptanceOracle(int k) {
        try {
            NormalDistribution dist = new NormalDistributionImpl(0, 1);
            double x = (double) k;
            double p = dist.cumulativeProbability(x);
            double q = dist.inverseCumulativeProbability(p);

            if (Math.abs(q - x) > 1.0e-12d) {
                throw new RuntimeException("[oracle:endpoint-accept] metamorphic violation: inverse(cdf(x)) must recover the same endpoint-root x for a valid normal input x=" + x + " p=" + p + " q=" + q);
            }

            NormalDistribution fresh = new NormalDistributionImpl(0, 1);
            double recomputed = fresh.cumulativeProbability(q);
            if (Math.abs(recomputed - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:fresh-recompute] consistency violation: fresh object recomputation of cdf(inverse(p)) disagrees p=" + p + " q=" + q + " recomputed=" + recomputed);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throw new RuntimeException("[oracle:endpoint-accept-throw] metamorphic violation: valid endpoint-root normal quantile threw through patched region k=" + k, t);
            }
        }
    }

    private static void freshObjectEndpointConsistencyOracle(int k) {
        try {
            NormalDistribution first = new NormalDistributionImpl(0, 1);
            NormalDistribution second = new NormalDistributionImpl(0, 1);
            double x = (double) k;
            double p = first.cumulativeProbability(x);
            double q1 = first.inverseCumulativeProbability(p);
            double q2 = second.inverseCumulativeProbability(p);

            if (Math.abs(q1 - q2) > 0.0d) {
                throw new RuntimeException("[oracle:fresh-agree-exact] consistency violation: identically constructed distributions must agree on the same quantile p=" + p + " q1=" + q1 + " q2=" + q2);
            }

            double p1 = first.cumulativeProbability(q1);
            double p2 = second.cumulativeProbability(q2);
            if (Math.abs(p1 - p2) > 1.0e-15d || Math.abs(p1 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:fresh-roundtrip-consistency] consistency violation: two fresh distributions disagree after inverse/cdf recomputation x=" + x + " p=" + p + " p1=" + p1 + " p2=" + p2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throw new RuntimeException("[oracle:fresh-object-throw] metamorphic violation: valid constructed probability threw through patched region k=" + k, t);
            }
        }
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(method))) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return isRootCauseThrowable(cause);
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange")) {
            return true;
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isCleanRejection(cause);
    }
}