package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double anchorP = 0.9772498680518209d;

        try {
            NormalDistribution anchor = new NormalDistributionImpl(0.0d, 1.0d);
            double x = anchor.inverseCumulativeProbability(anchorP);
            double replay = anchor.cumulativeProbability(x);
            if (Math.abs(x - 2.0d) > 1.0e-12d || Math.abs(replay - anchorP) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-endpoint-recovery] metamorphic violation: inverse/cdf round-trip on the documented valid seed must recover x=2 and p input=" + anchorP + " x=" + x + " replay=" + replay);
            }
        } catch (Throwable t) {
            boolean rooted = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                String cn = st[i].getClassName();
                String mn = st[i].getMethodName();
                if (("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cn) && "inverseCumulativeProbability".equals(mn))
                        || ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cn) && "bracket".equals(mn))) {
                    rooted = true;
                    break;
                }
            }
            if (rooted && t instanceof MathException) {
                throw new RuntimeException("[oracle:anchor-endpoint-recovery] metamorphic violation: valid inverseCumulativeProbability input was rejected along the patched path p=" + anchorP, t);
            }
        }

        try {
            SinFunction sin = new SinFunction();

            boolean sawInvalid = false;
            try {
                UnivariateRealSolverUtils.bracket(null, 0.0d, -1.0d, 1.0d);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    sawInvalid = true;
                }
            }
            if (!sawInvalid) {
                throw new RuntimeException("[oracle:null-function-reject] metamorphic violation: bracket must reject a null function");
            }

            sawInvalid = false;
            try {
                UnivariateRealSolverUtils.bracket(sin, 0.0d, 1.0d, 1.0d);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    sawInvalid = true;
                }
            }
            if (!sawInvalid) {
                throw new RuntimeException("[oracle:invalid-bounds-reject] metamorphic violation: bracket must reject endpoints that do not define an interval");
            }

            sawInvalid = false;
            try {
                UnivariateRealSolverUtils.bracket(sin, 0.0d, -1.0d, 1.0d, 0);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    sawInvalid = true;
                }
            }
            if (!sawInvalid) {
                throw new RuntimeException("[oracle:max-iter-reject] metamorphic violation: bracket must reject non-positive maximumIterations");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
        }

        int trials = 1 + data.consumeInt(1, 6);
        for (int i = 0; i < trials; i++) {
            int meanInt = data.consumeInt(-20, 20);
            int sdInt = data.consumeInt(1, 10);
            int k = data.consumeInt(-6, 6);
            if (k == 0) {
                k = 2;
            }

            double mean = (double) meanInt;
            double sd = (double) sdInt;
            double expectedX = mean + sd * (double) k;

            try {
                NormalDistribution dist = new NormalDistributionImpl(mean, sd);
                double p = dist.cumulativeProbability(expectedX);
                double got = dist.inverseCumulativeProbability(p);
                double replay = dist.cumulativeProbability(got);

                double tolX = 1.0e-10d * Math.max(1.0d, Math.abs(expectedX));
                if (Math.abs(got - expectedX) > tolX) {
                    throw new RuntimeException("[oracle:constructed-endpoint-roundtrip] metamorphic violation: for p built as cdf(expectedX), inverse must recover expectedX input=" + expectedX + " got=" + got + " p=" + p + " mean=" + mean + " sd=" + sd);
                }

                if (Math.abs(replay - p) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:constructed-cdf-replay] metamorphic violation: cdf(inverse(p)) must replay p on a valid constructed probability p=" + p + " replay=" + replay + " x=" + got + " mean=" + mean + " sd=" + sd);
                }

                try {
                    NormalDistribution standard = new NormalDistributionImpl(0.0d, 1.0d);
                    double standardized = standard.inverseCumulativeProbability(p);
                    double recon = mean + sd * standardized;
                    double tolRecon = 1.0e-10d * Math.max(1.0d, Math.abs(expectedX));
                    if (Math.abs(recon - got) > tolRecon) {
                        throw new RuntimeException("[oracle:standardization-agreement] metamorphic violation: shifted/scaled normal inverse must agree with standard-normal inverse after affine reconstruction p=" + p + " got=" + got + " recon=" + recon + " mean=" + mean + " sd=" + sd);
                    }
                } catch (Throwable t) {
                }
            } catch (Throwable t) {
                boolean rooted = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int j = 0; j < st.length; j++) {
                    String cn = st[j].getClassName();
                    String mn = st[j].getMethodName();
                    if (("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cn) && "inverseCumulativeProbability".equals(mn))
                            || ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cn) && "bracket".equals(mn))) {
                        rooted = true;
                        break;
                    }
                }
                if (rooted && t instanceof MathException) {
                    throw new RuntimeException("[oracle:constructed-endpoint-roundtrip] metamorphic violation: valid probability constructed from cumulativeProbability(expectedX) was rejected expectedX=" + expectedX + " mean=" + mean + " sd=" + sd, t);
                }
            }
        }
    }
}