package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                exploreStandardIntegerQuantile(data);
            } else if (mode == 1) {
                exploreShiftedScaledIntegerQuantile(data);
            } else {
                exploreRoundTripFromProbability(data);
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: known regression input must invert to 2.0 input=" + p + " result=" + result);
            }
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                    throw (RuntimeException) t;
                }
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exploreStandardIntegerQuantile(FuzzedDataProvider data) {
        int k = data.consumeInt(1, 8);
        if (data.consumeBoolean()) {
            k = -k;
        }
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double expectedX = (double) k;
            double p = normal.cumulativeProbability(expectedX);
            double actualX = normal.inverseCumulativeProbability(p);

            /* Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
               These probabilities are valid by construction because they are produced by cumulativeProbability on a real NormalDistribution. */
            if (Math.abs(actualX - expectedX) > 1.0e-9d) {
                throw new RuntimeException("[oracle:int-quantile] metamorphic violation: inverse(cdf(x)) == x input=" + expectedX + " lhs=" + actualX + " rhs=" + expectedX);
            }

            double roundTrip = normal.cumulativeProbability(actualX);
            if (Math.abs(roundTrip - p) > 1.0e-9d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: cdf(inverse(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exploreShiftedScaledIntegerQuantile(FuzzedDataProvider data) {
        double mean = data.consumeInt(-50, 50);
        double sd = data.consumeInt(1, 20);
        int k = data.consumeInt(-6, 6);
        if (k == 0) {
            k = 2;
        }

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);
            double expectedX = mean + (sd * k);
            double p = normal.cumulativeProbability(expectedX);
            double actualX = normal.inverseCumulativeProbability(p);

            /* Contract/oracle: for a valid NormalDistribution with positive standard deviation,
               inverseCumulativeProbability(cumulativeProbability(x)) should recover x. */
            if (Math.abs(actualX - expectedX) > 1.0e-8d) {
                throw new RuntimeException("[oracle:shift-scale] metamorphic violation: inverse(cdf(x)) == x input=" + expectedX + " lhs=" + actualX + " rhs=" + expectedX);
            }

            double roundTrip = normal.cumulativeProbability(actualX);
            if (Math.abs(roundTrip - p) > 1.0e-8d) {
                throw new RuntimeException("[oracle:shift-scale-rt] metamorphic violation: cdf(inverse(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exploreRoundTripFromProbability(FuzzedDataProvider data) {
        double mean = data.consumeInt(-20, 20);
        double sd = data.consumeInt(1, 10);
        int k = data.consumeInt(1, 6);
        if (data.consumeBoolean()) {
            k = -k;
        }

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);
            double x = mean + (sd * k);
            double p = normal.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d)) {
                return;
            }

            double inv = normal.inverseCumulativeProbability(p);
            double p2 = normal.cumulativeProbability(inv);

            /* Contract/oracle: for probabilities strictly inside (0,1), inverseCumulativeProbability returns
               a quantile whose cumulativeProbability matches the original probability. A patch that merely deletes
               the buggy throw but returns a wrong endpoint would violate this observable round-trip relation. */
            if (Math.abs(p2 - p) > 1.0e-9d) {
                throw new RuntimeException("[oracle:p-roundtrip] metamorphic violation: cdf(inverse(p)) == p input=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String cn = cur.getClass().getName();
            if (cn.endsWith("IllegalArgumentException")
                    || cn.endsWith("InvalidRepresentationException")
                    || cn.endsWith("NotPositiveException")
                    || cn.endsWith("OutOfRangeException")
                    || cn.endsWith("NoBracketingException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean hasMathExceptionFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException || "org.apache.commons.math.ConvergenceException".equals(cur.getClass().getName())) {
                hasMathExceptionFamily = true;
            }
        }
        return hasMathExceptionFamily && hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace == null) {
                continue;
            }
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement e = trace[i];
                if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
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