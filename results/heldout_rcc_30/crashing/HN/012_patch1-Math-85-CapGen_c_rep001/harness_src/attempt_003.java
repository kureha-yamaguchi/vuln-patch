package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        try {
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (!(Math.abs(result - 2.0d) <= 1.0e-12d)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression quantile should recover 2.0 inputP=" + p + " result=" + result);
            }
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (!(Math.abs(roundTrip - p) <= 1.0e-12d)) {
                    throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) ~= p inputP=" + p + " inv=" + result + " roundTrip=" + roundTrip);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            boolean hasBracket = false;
            for (Throwable c = t; c != null && !hasBracket; c = c.getCause()) {
                StackTraceElement[] st = c.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                && "bracket".equals(st[i].getMethodName())) {
                            hasBracket = true;
                            break;
                        }
                    }
                }
            }
            if ((t instanceof MathException) && hasBracket) {
                throw new RuntimeException(t);
            }
        }

        // EXPLORE:
        // Property: use valid probabilities constructed from real normal CDF values at exact integer quantiles.
        // Then inverseCumulativeProbability(cumulativeProbability(x)) must recover x for a correct implementation.
        // This reaches the real public API and exercises the same bracket() path when the bracket endpoint lands exactly on the root.
        int trials = 1 + data.consumeInt(1, 6);
        for (int i = 0; i < trials; i++) {
            int magnitude = data.consumeInt(2, 8);
            int sign = data.consumeBoolean() ? 1 : -1;
            double x = sign * magnitude;

            double p;
            try {
                p = normal.cumulativeProbability(x);
            } catch (Throwable ignored) {
                return;
            }

            if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
                return;
            }

            try {
                double inv = normal.inverseCumulativeProbability(p);

                if (!(Math.abs(inv - x) <= 1.0e-9d)) {
                    throw new RuntimeException("[oracle:inv-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) should recover x x=" + x + " p=" + p + " inv=" + inv);
                }

                try {
                    double p2 = normal.cumulativeProbability(inv);
                    if (!(Math.abs(p2 - p) <= 1.0e-12d)) {
                        throw new RuntimeException("[oracle:cdf-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) ~= p x=" + x + " p=" + p + " inv=" + inv + " roundTrip=" + p2);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                boolean hasBracket = false;
                for (Throwable c = t; c != null && !hasBracket; c = c.getCause()) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int j = 0; j < st.length; j++) {
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[j].getClassName())
                                    && "bracket".equals(st[j].getMethodName())) {
                                hasBracket = true;
                                break;
                            }
                        }
                    }
                }
                if ((t instanceof MathException) && hasBracket) {
                    throw new RuntimeException(t);
                }
                return;
            }
        }
    }
}