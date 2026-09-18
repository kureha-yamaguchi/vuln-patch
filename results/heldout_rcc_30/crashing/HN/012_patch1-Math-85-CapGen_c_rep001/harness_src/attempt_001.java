package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability should recover the documented test value input=" + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                sneakyThrow(t);
            }
            return;
        }

        double x;
        if (data.consumeBoolean()) {
            x = data.consumeInt(-8, 8);
        } else {
            int numerator = data.consumeInt(-8000, 8000);
            int denominator = data.consumeInt(1, 1000);
            x = ((double) numerator) / ((double) denominator);
        }

        if (Double.isNaN(x) || Double.isInfinite(x)) {
            return;
        }

        try {
            double p = normal.cumulativeProbability(x);
            if (Double.isNaN(p) || p < 0.0d || p > 1.0d) {
                return;
            }

            double recovered = normal.inverseCumulativeProbability(p);

            /* Contract/oracle: for a continuous distribution, cumulativeProbability(x) and
             * inverseCumulativeProbability(p) are inverse operations on valid probabilities.
             * We construct p from the real library's cumulativeProbability(x), so p is valid by
             * construction. A patch that merely suppresses the throw in bracket but returns an
             * incorrect endpoint/root will violate this round-trip relation.
             */
            double tolerance = 1.0e-9d;
            if (Math.abs(recovered - x) > tolerance) {
                throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " lhs=" + recovered + " rhs=" + x);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException && !(t instanceof IllegalArgumentException) && !(t instanceof NumberFormatException)) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                return true;
            }
        }
        return isRootCauseMathException(t);
    }

    private static boolean isRootCauseMathException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return false;
            }
            if (cur instanceof MathException && stackHasBracket(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean stackHasBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] frames = cur.getStackTrace();
            if (frames != null) {
                for (int i = 0; i < frames.length; i++) {
                    StackTraceElement f = frames[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(f.getClassName())
                            && "bracket".equals(f.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}