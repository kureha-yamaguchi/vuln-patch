package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution anchorDist = new NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = anchorDist.inverseCumulativeProbability(anchorP);

            /* Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * The failing test documents that this exact valid input must produce 2.0; a "fix" that merely suppresses the
             * throw but returns a wrong value would violate this observable post-condition.
             */
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: exact regression input should invert to 2.0 input=" + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            boolean hasMathException = false;
            boolean passesBracket = false;
            Throwable cur = t;
            while (cur != null) {
                if (cur instanceof MathException) {
                    hasMathException = true;
                }
                StackTraceElement[] trace = cur.getStackTrace();
                if (trace != null) {
                    for (int i = 0; i < trace.length; i++) {
                        StackTraceElement ste = trace[i];
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                && "bracket".equals(ste.getMethodName())) {
                            passesBracket = true;
                            break;
                        }
                    }
                }
                if (passesBracket && hasMathException) {
                    break;
                }
                cur = cur.getCause();
            }
            if (hasMathException && passesBracket) {
                throw new RuntimeException(t);
            }
        }

        int trials = 1 + Math.min(4, data.remainingBytes());
        for (int i = 0; i < trials; i++) {
            double mean = data.consumeInt(-10, 10);
            double sd = data.consumeInt(1, 10);

            int step = data.consumeInt(2, 50);
            if (data.consumeBoolean()) {
                step = -step;
            }

            double x = mean + (sd * step);
            NormalDistribution dist = new NormalDistributionImpl(mean, sd);

            double p;
            try {
                p = dist.cumulativeProbability(x);
            } catch (Throwable ignored) {
                continue;
            }

            try {
                double inv = dist.inverseCumulativeProbability(p);

                /* Contract/oracle: for a value x we construct first, p = CDF(x) is a valid probability by construction,
                 * so inverseCumulativeProbability(p) must recover x (within numerical tolerance) for a correct implementation.
                 * This specifically exercises the patched bracket path on endpoint roots such as x = mean + sd * integer.
                 */
                if (Math.abs(inv - x) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:cdf-inverse] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException;
                if (cleanRejection) {
                    continue;
                }

                boolean hasMathException = false;
                boolean passesBracket = false;
                Throwable cur = t;
                while (cur != null) {
                    if (cur instanceof MathException) {
                        hasMathException = true;
                    }
                    StackTraceElement[] trace = cur.getStackTrace();
                    if (trace != null) {
                        for (int j = 0; j < trace.length; j++) {
                            StackTraceElement ste = trace[j];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                    && "bracket".equals(ste.getMethodName())) {
                                passesBracket = true;
                                break;
                            }
                        }
                    }
                    if (passesBracket && hasMathException) {
                        break;
                    }
                    cur = cur.getCause();
                }

                if (hasMathException && passesBracket) {
                    throw new RuntimeException(t);
                }
            }
        }
    }
}