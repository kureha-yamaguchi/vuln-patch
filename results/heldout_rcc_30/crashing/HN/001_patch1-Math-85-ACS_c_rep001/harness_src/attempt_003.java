package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Local {
            boolean hasBracket(Throwable t) {
                Throwable cur = t;
                while (cur != null) {
                    StackTraceElement[] st = cur.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                return true;
                            }
                        }
                    }
                    cur = cur.getCause();
                }
                return false;
            }

            boolean isValidation(Throwable t) {
                Throwable cur = t;
                while (cur != null) {
                    if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                        return true;
                    }
                    String n = cur.getClass().getName();
                    if (n.contains("IllegalArgument") || n.contains("Invalid") || n.contains("OutOfRange")
                            || n.contains("NotPositive") || n.contains("NotStrictlyPositive")
                            || n.contains("NoData") || n.contains("NullArgument")) {
                        return true;
                    }
                    cur = cur.getCause();
                }
                return false;
            }

            boolean isRootCause(Throwable t) {
                return (t instanceof MathException) && hasBracket(t);
            }

            <T extends Throwable> void sneakyThrow(Throwable t) throws T {
                throw (T) t;
            }

            void maybePropagate(Throwable t) {
                if (isValidation(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    this.<RuntimeException>sneakyThrow(t);
                }
            }
        }
        Local local = new Local();

        try {
            NormalDistribution anchor = new NormalDistributionImpl(0.0, 1.0);
            double anchorP = 0.9772498680518209d;
            double anchorResult = anchor.inverseCumulativeProbability(anchorP);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(0.9772498680518209) must equal 2.0 input="
                        + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            local.maybePropagate(t);
            return;
        }

        int mean = data.consumeInt(-1000, 1000);
        boolean upperSide = data.consumeBoolean();
        double sigma = 1.0d;
        double x = mean + (upperSide ? 2.0d : -2.0d);

        try {
            NormalDistribution dist = new NormalDistributionImpl((double) mean, sigma);
            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * For a valid probability produced by the same distribution, inverseCumulativeProbability(cumulativeProbability(x))
             * should recover x (up to numerical tolerance). This directly catches a patch that merely suppresses the throw
             * or otherwise returns the wrong quantile after reaching bracket with fa*fb == 0.
             */
            if (Double.isNaN(inv) || Double.isInfinite(inv) || Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverse(cdf(x)) must recover x input="
                        + x + " lhs=" + inv + " rhs=" + x + " mean=" + mean + " sigma=" + sigma + " p=" + p);
            }
        } catch (Throwable t) {
            local.maybePropagate(t);
        }
    }
}