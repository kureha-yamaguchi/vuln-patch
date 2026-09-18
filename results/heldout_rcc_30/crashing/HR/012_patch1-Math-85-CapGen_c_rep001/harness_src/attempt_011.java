package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        try {
            UnivariateRealSolverUtils.bracket(null, 0.0, -1.0, 1.0, 1);
        } catch (Throwable t) {
            if (!isValidationException(t)) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
            }
        }

        int rounds = 1 + Math.abs(data.consumeInt(0, 4));
        for (int i = 0; i < rounds; i++) {
            exploreStateFreshAgreement(data);
            exploreEndpointRootFamily(data);
        }
    }

    private static void runAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double p = 0.9772498680518209d;
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-post] metamorphic violation: inverseCumulativeProbability(seed) must recover the documented test value input=" + p + " got=" + x);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void exploreStateFreshAgreement(FuzzedDataProvider data) {
        double mean = scaledInt(data.consumeInt(-2000, 2000), 100.0);
        double sd = 0.1 + scaledInt(data.consumeInt(0, 2000), 100.0);
        int k = data.consumeInt(-4, 4);
        if (k == 0) {
            k = 2;
        }

        NormalDistributionImpl ctorBuilt = new NormalDistributionImpl(mean, sd);
        NormalDistributionImpl setterBuilt = new NormalDistributionImpl();
        try {
            setterBuilt.setMean(mean);
            setterBuilt.setStandardDeviation(sd);
        } catch (Throwable t) {
            if (!isValidationException(t)) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
            }
            return;
        }

        double x = mean + sd * k;
        try {
            double p1 = ctorBuilt.cumulativeProbability(x);
            double p2 = setterBuilt.cumulativeProbability(x);

            if (Math.abs(p1 - p2) > 1.0e-14d) {
                throw new RuntimeException("[oracle:state-fresh-agree] metamorphic violation: equivalent distributions must agree on cumulativeProbability input=" + x + " lhs=" + p1 + " rhs=" + p2);
            }

            double inv1 = ctorBuilt.inverseCumulativeProbability(p1);
            double inv2 = setterBuilt.inverseCumulativeProbability(p1);

            double tol = scaledTolerance(x, inv1, inv2);
            if (Math.abs(inv1 - inv2) > tol) {
                throw new RuntimeException("[oracle:state-fresh-agree] metamorphic violation: constructor-built and setter-built equivalent distributions must agree on inverseCumulativeProbability p=" + p1 + " lhs=" + inv1 + " rhs=" + inv2);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void exploreEndpointRootFamily(FuzzedDataProvider data) {
        double mean = scaledInt(data.consumeInt(-500, 500), 50.0);
        double sd = 0.5 + scaledInt(data.consumeInt(0, 500), 50.0);
        int endpoint = data.consumeBoolean() ? 2 : -2;

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);
        double x = mean + sd * endpoint;
        try {
            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            double tol = scaledTolerance(x, inv, inv);
            if (Math.abs(inv - x) > tol) {
                throw new RuntimeException("[oracle:endpoint-root-affine] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x for valid x built from the same distribution x=" + x + " p=" + p + " inv=" + inv);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isValidationException(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || hasNameFragment(t, "Illegal")
                || hasNameFragment(t, "Invalid");
    }

    private static boolean hasNameFragment(Throwable t, String fragment) {
        Throwable cur = t;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (name.indexOf(fragment) >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                StackTraceElement[] st = cur.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                    if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(e.getClassName())
                            && "inverseCumulativeProbability".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double scaledInt(int v, double div) {
        return ((double) v) / div;
    }

    private static double scaledTolerance(double a, double b, double c) {
        double m = Math.max(1.0d, Math.max(Math.abs(a), Math.max(Math.abs(b), Math.abs(c))));
        return 1.0e-10d * m;
    }

    private static void sneakyThrow(Throwable t) {
        FuzzHarness.<RuntimeException>throwUnchecked(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}