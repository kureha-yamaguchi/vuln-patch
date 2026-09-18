package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runRegressionAnchor();

        double mean = boundedDouble(data);
        double std = positiveBoundedDouble(data);

        NormalDistributionImpl freshA = new NormalDistributionImpl(mean, std);
        NormalDistributionImpl freshB = new NormalDistributionImpl(mean, std);
        NormalDistributionImpl reset = new NormalDistributionImpl(0.0, 1.0);

        try {
            reset.setMean(mean + boundedDouble(data));
            reset.setStandardDeviation(positiveBoundedDouble(data));
            reset.setMean(mean);
            reset.setStandardDeviation(std);
        } catch (RuntimeException e) {
            if (isValidationLike(e)) {
                return;
            }
            throw e;
        }

        double x = mean + std * (1.0 + (data.consumeInt(-1000, 1000) / 1000.0));
        double p;
        try {
            p = freshA.cumulativeProbability(x);
        } catch (MathException e) {
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:constructed-probability] valid CDF computation unexpectedly hit root cause at x=" + x + " mean=" + mean + " sd=" + std, e);
            }
            return;
        } catch (RuntimeException e) {
            if (isValidationLike(e)) {
                return;
            }
            throw e;
        }

        if (!(p > 0.0 && p < 1.0)) {
            return;
        }

        try {
            double invA = freshA.inverseCumulativeProbability(p);
            double invB = freshB.inverseCumulativeProbability(p);
            double invReset = reset.inverseCumulativeProbability(p);

            double tol = 1.0e-12 * Math.max(1.0, Math.max(Math.abs(invA), Math.max(Math.abs(invB), Math.abs(invReset))));

            if (Math.abs(invA - invB) > tol) {
                throw new RuntimeException("[oracle:fresh-object-agreement-reset] metamorphic violation: identically constructed distributions disagree on inverse for same valid p=" + p + " invA=" + invA + " invB=" + invB + " mean=" + mean + " sd=" + std);
            }

            if (Math.abs(invA - invReset) > tol) {
                throw new RuntimeException("[oracle:reset-object-agreement] metamorphic violation: distribution mutated away and reset to same parameters disagrees on inverse for same valid p=" + p + " invFresh=" + invA + " invReset=" + invReset + " mean=" + mean + " sd=" + std);
            }

            double cdfAtInvA = freshA.cumulativeProbability(invA);
            double cdfAtInvReset = reset.cumulativeProbability(invReset);
            if (Math.abs(cdfAtInvA - p) > 1.0e-9 || Math.abs(cdfAtInvReset - p) > 1.0e-9) {
                throw new RuntimeException("[oracle:inverse-roundtrip-reset] consistency violation: inverse/CDF roundtrip disagrees with original valid probability p=" + p + " cdfFresh=" + cdfAtInvA + " cdfReset=" + cdfAtInvReset + " mean=" + mean + " sd=" + std);
            }
        } catch (MathException e) {
            if (isValidationLike(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:inverse-root-cause] valid probability built from the distribution's own CDF was rejected p=" + p + " mean=" + mean + " sd=" + std, e);
            }
            return;
        } catch (RuntimeException e) {
            if (isValidationLike(e)) {
                return;
            }
            throw e;
        }
    }

    private static void runRegressionAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-regression-value] metamorphic violation: regression witness should recover x=2.0 but got " + result);
            }
        } catch (MathException e) {
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:anchor-regression-throw] valid regression probability was rejected", e);
            }
        } catch (RuntimeException e) {
            if (!isValidationLike(e)) {
                throw e;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (hasFrame(cur, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")
                    || hasFrame(cur, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")
                    || hasFrame(cur, "org.apache.commons.math.analysis.UnivariateRealSolverUtils", "bracket")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement frame = trace[i];
            if (className.equals(frame.getClassName()) && methodName.equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationLike(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.indexOf("IllegalArgument") >= 0 || n.indexOf("Invalid") >= 0 || n.indexOf("OutOfRange") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double boundedDouble(FuzzedDataProvider data) {
        int numerator = data.consumeInt(-1000000, 1000000);
        int denominator = data.consumeInt(1, 1000);
        return ((double) numerator) / ((double) denominator);
    }

    private static double positiveBoundedDouble(FuzzedDataProvider data) {
        int numerator = data.consumeInt(1, 1000000);
        int denominator = data.consumeInt(1, 1000);
        return ((double) numerator) / ((double) denominator);
    }
}