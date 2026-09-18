package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(canonical p) should equal 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException);
            if (!cleanRejection) {
                boolean sawMathException = false;
                boolean sawBracket = false;
                Throwable cur = t;
                int guard = 0;
                while (cur != null && guard++ < 16) {
                    if (cur instanceof MathException) {
                        sawMathException = true;
                    }
                    StackTraceElement[] st = cur.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                sawBracket = true;
                                break;
                            }
                        }
                    }
                    if (sawBracket && sawMathException) {
                        break;
                    }
                    cur = cur.getCause();
                }
                if (sawMathException && sawBracket) {
                    throwUnchecked(t);
                }
            }
        }

        // EXPLORE:
        // Root-cause property: build a valid probability p from the REAL API as p = CDF(mean + 2*sigma).
        // For a correct implementation, inverseCumulativeProbability(p) must recover mean + 2*sigma.
        // This round-trip is a documented inverse relation for valid probabilities, and catches
        // throw-deleting/wrong-result patches that merely avoid the buggy exception path.
        double mean = data.consumeInt(-1000, 1000) / 10.0d;
        double sigma = (data.consumeInt(1, 1000) / 100.0d) + 0.01d;

        try {
            NormalDistribution dist = new NormalDistributionImpl(mean, sigma);
            double expected = mean + 2.0d * sigma;
            double p = dist.cumulativeProbability(expected);
            double actual = dist.inverseCumulativeProbability(p);
            if (Math.abs(actual - expected) > 1.0e-9d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) should recover x input=" + expected + " lhs=" + actual + " rhs=" + expected + " mean=" + mean + " sigma=" + sigma + " p=" + p);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }

            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException);
            if (cleanRejection) {
                return;
            }

            boolean sawMathException = false;
            boolean sawBracket = false;
            Throwable cur = t;
            int guard = 0;
            while (cur != null && guard++ < 16) {
                if (cur instanceof MathException) {
                    sawMathException = true;
                }
                StackTraceElement[] st = cur.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        StackTraceElement e = st[i];
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                && "bracket".equals(e.getMethodName())) {
                            sawBracket = true;
                            break;
                        }
                    }
                }
                if (sawBracket && sawMathException) {
                    break;
                }
                cur = cur.getCause();
            }

            if (sawMathException && sawBracket) {
                throwUnchecked(t);
            }
        }
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
        throw (T) t;
    }
}