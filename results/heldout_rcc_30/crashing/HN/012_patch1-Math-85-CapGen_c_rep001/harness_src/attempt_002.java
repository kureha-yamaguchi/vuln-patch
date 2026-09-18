package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract/oracle used below:
        // For a continuous normal distribution, inverseCumulativeProbability(p) returns x such that
        // cumulativeProbability(x) == p. Therefore, for any valid x we construct ourselves,
        // inv(cdf(x)) must recover x (up to numerical tolerance). A "fix" that merely suppresses
        // the buggy throw in bracket but returns a wrong value would violate this observable relation.

        // ANCHOR: exact regression input from NormalDistributionTest.testMath280
        try {
            NormalDistribution anchor = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double result = anchor.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(seed) should recover known quantile input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }

        // EXPLORE: valid-by-construction inputs satisfying the root-cause property:
        // choose a real normal distribution, compute p = cdf(x) for an exactly constructed x,
        // then feed that p back into inverseCumulativeProbability through the public API.
        // Positive integer offsets make it likely that the internal bracket endpoint lands exactly on the root.
        int cases = 1 + Math.min(8, Math.max(0, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            int meanInt = data.consumeInt(-5, 5);
            int sdSelector = data.consumeInt(0, 3);
            double sd;
            switch (sdSelector) {
                case 0:
                    sd = 0.5d;
                    break;
                case 1:
                    sd = 1.0d;
                    break;
                case 2:
                    sd = 2.0d;
                    break;
                default:
                    sd = 3.0d;
                    break;
            }
            int offset = data.consumeInt(1, 6);
            double x = meanInt + offset;

            try {
                checkRoundTrip(meanInt, sd, x);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
            }
        }
    }

    private static void checkRoundTrip(double mean, double sd, double x) throws MathException {
        NormalDistribution dist = new NormalDistributionImpl(mean, sd);
        double p = dist.cumulativeProbability(x);
        if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
            return;
        }

        double roundTrip = dist.inverseCumulativeProbability(p);
        if (Double.isNaN(roundTrip) || Double.isInfinite(roundTrip) || Math.abs(roundTrip - x) > 1.0e-9d) {
            throw new RuntimeException(
                "[oracle:norm-inv] metamorphic violation: inv(cdf(x)) == x for valid normal-distribution inputs input={mean=" +
                mean + ",sd=" + sd + ",x=" + x + ",p=" + p + "} lhs=" + roundTrip + " rhs=" + x
            );
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid") || name.contains("OutOfRange") || name.contains("NoData")
                    || name.contains("NotPositive") || name.contains("NotStrictlyPositive")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        boolean familyMatch = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                familyMatch = true;
                break;
            }
        }
        if (!familyMatch) {
            return false;
        }
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st == null) {
                continue;
            }
            for (StackTraceElement e : st) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}