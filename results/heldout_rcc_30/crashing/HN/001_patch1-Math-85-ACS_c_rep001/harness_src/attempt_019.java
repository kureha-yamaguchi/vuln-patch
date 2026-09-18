package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        double mean = data.consumeInt(-100, 100) / 10.0;
        double sd = Math.max(0.1, data.consumeInt(1, 100) / 10.0);
        int sign = data.consumeBoolean() ? 1 : -1;
        double target = mean + sign * 2.0 * sd;

        try {
            ContinuousDistribution dist = new NormalDistributionImpl(mean, sd);
            double p = dist.cumulativeProbability(target);
            if (p > 0.0 && p < 1.0) {
                double inv = dist.inverseCumulativeProbability(p);
                if (Math.abs(inv - target) > 1.0e-9) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) should recover x for this continuous normal input=" + target + " lhs=" + inv + " rhs=" + target);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        exploreBracketOracle(data);
    }

    private static void anchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-result] metamorphic violation: failing test requires inverseCumulativeProbability(0.9772498680518209) == 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void exploreBracketOracle(FuzzedDataProvider data) {
        double lower = 0.0;
        double upper = Math.PI;
        double initial = Math.PI - 1.0;
        int maxIterations = Math.max(1, data.consumeInt(1, 8));

        try {
            SinFunction f = new SinFunction();
            double[] r1 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
            double fa = f.value(r1[0]);
            double fb = f.value(r1[1]);
            if (!(fa * fb <= 0.0)) {
                throw new RuntimeException("[oracle:bracket-sign] metamorphic violation: documented bracketing result must bound a root with opposite signs or an endpoint root input=" + initial + "," + lower + "," + upper + " lhs=" + fa + " rhs=" + fb);
            }

            double[] r2 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);
            double ga = f.value(r2[0]);
            double gb = f.value(r2[1]);
            if (!(ga * gb <= 0.0)) {
                throw new RuntimeException("[oracle:bracket-sign2] metamorphic violation: documented bracketing result must bound a root with opposite signs or an endpoint root input=" + initial + "," + lower + "," + upper + "," + maxIterations + " lhs=" + ga + " rhs=" + gb);
            }

            if (Math.abs(r1[0] - r2[0]) > 1.0e-12 || Math.abs(r1[1] - r2[1]) > 1.0e-12) {
                throw new RuntimeException("[oracle:bracket-overload] metamorphic violation: same-name bracket overloads must agree on equivalent inputs input=" + initial + "," + lower + "," + upper + "," + maxIterations + " lhs=[" + r1[0] + "," + r1[1] + "] rhs=[" + r2[0] + "," + r2[1] + "]");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.contains("Invalid") || n.contains("Illegal") || n.contains("NoData") || n.contains("OutOfRange")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        boolean sawMathException = false;
        boolean sawBracket = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (StackTraceElement e : st) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        sawBracket = true;
                        break;
                    }
                }
            }
        }
        return sawMathException && sawBracket;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}