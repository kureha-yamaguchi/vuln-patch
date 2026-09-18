package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        quantileSlopeOracle(data);
    }

    private static void runAnchor() {
        org.apache.commons.math.distribution.NormalDistributionImpl normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);
        try {
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
        }
    }

    private static void quantileSlopeOracle(FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistributionImpl normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);

        int centerInt = data.consumeInt(-4, 4);
        double centerP;
        try {
            centerP = normal.cumulativeProbability((double) centerInt);
        } catch (Throwable t) {
            return;
        }

        int scale = data.consumeInt(1, 1000);
        double delta = scale / 1000000.0;
        double p = centerP;
        if (p <= delta + 1e-8 || p >= 1.0 - delta - 1e-8) {
            return;
        }

        double qLo;
        double qMid;
        double qHi;
        double pdfMid;
        try {
            qLo = normal.inverseCumulativeProbability(p - delta);
            qMid = normal.inverseCumulativeProbability(p);
            qHi = normal.inverseCumulativeProbability(p + delta);
            pdfMid = normal.density(qMid);
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        if (!(Double.isFinite(qLo) && Double.isFinite(qMid) && Double.isFinite(qHi) && Double.isFinite(pdfMid))) {
            return;
        }
        if (pdfMid <= 0.0) {
            return;
        }

        double lhs = (qHi - qLo) / (2.0 * delta);
        double rhs = 1.0 / pdfMid;
        if (!(Double.isFinite(lhs) && Double.isFinite(rhs))) {
            return;
        }

        double rel = Math.abs(lhs - rhs) / Math.max(1.0, Math.abs(rhs));
        if (rel > 0.08) {
            throw new RuntimeException(
                    "[oracle:quantile-slope] metamorphic violation: "
                            + "for the normal distribution, inverseCumulativeProbability is the inverse of the CDF, "
                            + "so d/dp invCDF(p) = 1 / density(invCDF(p)); "
                            + "central-difference slope disagrees with density-based slope "
                            + "centerInt=" + centerInt
                            + " p=" + p
                            + " delta=" + delta
                            + " qLo=" + qLo
                            + " qMid=" + qMid
                            + " qHi=" + qHi
                            + " lhs=" + lhs
                            + " rhs=" + rhs
                            + " rel=" + rel);
        }
    }

    private static void handleLibraryThrowable(Throwable t, boolean validByConstruction) {
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCauseMathException(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid")
                    || name.contains("OutOfRange") || name.contains("NoData")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        if (!(t instanceof org.apache.commons.math.MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}