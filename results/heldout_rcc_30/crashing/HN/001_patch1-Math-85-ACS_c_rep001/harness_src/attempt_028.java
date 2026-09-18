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
                throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact regression input must recover x=2.0 input=0.9772498680518209 result=" + result);
            }
        } catch (Throwable t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection) {
                Throwable c = t.getCause();
                while (c != null && !cleanRejection) {
                    if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                        cleanRejection = true;
                        break;
                    }
                    c = c.getCause();
                }
            }
            if (cleanRejection) {
                return;
            }

            boolean hasMathException = false;
            Throwable cur = t;
            while (cur != null) {
                if (cur instanceof MathException) {
                    hasMathException = true;
                    break;
                }
                cur = cur.getCause();
            }

            boolean passesThroughBracket = false;
            cur = t;
            while (cur != null && !passesThroughBracket) {
                StackTraceElement[] frames = cur.getStackTrace();
                if (frames != null) {
                    for (int i = 0; i < frames.length; i++) {
                        StackTraceElement ste = frames[i];
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                && "bracket".equals(ste.getMethodName())) {
                            passesThroughBracket = true;
                            break;
                        }
                    }
                }
                cur = cur.getCause();
            }

            if (hasMathException && passesThroughBracket) {
                throw new RuntimeException(t);
            }
            return;
        }

        int trials = 1 + data.consumeInt(1, 4);
        for (int i = 0; i < trials; i++) {
            double mean = data.consumeInt(-1000, 1000) / 100.0d;
            double sd = data.consumeInt(1, 1000) / 100.0d;
            int z = data.consumeInt(1, 6);
            int sign = data.consumeBoolean() ? 1 : -1;
            double x = mean + sign * z * sd;

            try {
                NormalDistribution dist = new NormalDistributionImpl(mean, sd);
                double p = dist.cumulativeProbability(x);

                try {
                    double inv = dist.inverseCumulativeProbability(p);

                    // Contract/oracle: for p constructed from the same distribution as p = CDF(x),
                    // a correct inverse must recover x (round-trip through real library code).
                    // A throw-deleting patch at bracket could silently return the wrong value and violate this.
                    double tol = 1.0e-9d * (1.0d + Math.abs(x));
                    if (Math.abs(inv - x) > tol) {
                        throw new RuntimeException("[oracle:cdf-inv-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x inputMean="
                                + mean + " inputSd=" + sd + " x=" + x + " p=" + p + " lhs=" + inv + " rhs=" + x);
                    }
                } catch (Throwable t) {
                    boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                    if (!cleanRejection) {
                        Throwable c = t.getCause();
                        while (c != null && !cleanRejection) {
                            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                                cleanRejection = true;
                                break;
                            }
                            c = c.getCause();
                        }
                    }
                    if (cleanRejection) {
                        continue;
                    }

                    boolean hasMathException = false;
                    Throwable cur = t;
                    while (cur != null) {
                        if (cur instanceof MathException) {
                            hasMathException = true;
                            break;
                        }
                        cur = cur.getCause();
                    }

                    boolean passesThroughBracket = false;
                    cur = t;
                    while (cur != null && !passesThroughBracket) {
                        StackTraceElement[] frames = cur.getStackTrace();
                        if (frames != null) {
                            for (int j = 0; j < frames.length; j++) {
                                StackTraceElement ste = frames[j];
                                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                        && "bracket".equals(ste.getMethodName())) {
                                    passesThroughBracket = true;
                                    break;
                                }
                            }
                        }
                        cur = cur.getCause();
                    }

                    if (hasMathException && passesThroughBracket) {
                        throw new RuntimeException(t);
                    }
                }
            } catch (Throwable t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (!cleanRejection) {
                    Throwable c = t.getCause();
                    while (c != null && !cleanRejection) {
                        if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                            cleanRejection = true;
                            break;
                        }
                        c = c.getCause();
                    }
                }
                if (!cleanRejection) {
                    return;
                }
            }
        }
    }
}