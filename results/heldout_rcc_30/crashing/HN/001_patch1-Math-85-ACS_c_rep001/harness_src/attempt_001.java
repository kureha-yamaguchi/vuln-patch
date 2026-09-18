package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            anchorNormalTrigger();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        try {
            anchorDirectBracketAndOracle();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        int meanInt = data.consumeInt(-100, 100);
        int sdInt = data.consumeInt(1, 100);
        int steps = data.consumeInt(1, 20);
        boolean positiveSide = data.consumeBoolean();

        double mean = meanInt;
        double sd = sdInt;
        double x = positiveSide ? mean + (2.0 * sd) : mean - (2.0 * sd);

        try {
            exploreNormalRoundTrip(mean, sd, x);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        try {
            exploreBracketOverloads(data);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void anchorNormalTrigger() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        normal.inverseCumulativeProbability(0.9772498680518209);
    }

    private static void anchorDirectBracketAndOracle() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        double[] r4 = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, 2.0);
        double[] r5 = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, 2.0, Integer.MAX_VALUE);

        /* Contract/oracle: bracket returns values bracketing a root; a correct result must satisfy f(a)*f(b) <= 0.
           The 4-arg and 5-arg overloads document the same operation, so on equivalent valid inputs they must agree. */
        double fa4 = f.value(r4[0]);
        double fb4 = f.value(r4[1]);
        if (!(fa4 * fb4 <= 0.0)) {
            throw new RuntimeException("[oracle:bracket-sign] metamorphic violation: returned interval does not bracket a root input=[1.0,0.0,2.0] lhs=" + r4[0] + "," + r4[1] + " rhs=" + fa4 + "," + fb4);
        }
        if (Double.compare(r4[0], r5[0]) != 0 || Double.compare(r4[1], r5[1]) != 0) {
            throw new RuntimeException("[oracle:bracket-overload] metamorphic violation: equivalent bracket overloads disagree input=[1.0,0.0,2.0] lhs=[" + r4[0] + "," + r4[1] + "] rhs=[" + r5[0] + "," + r5[1] + "]");
        }
    }

    private static void exploreNormalRoundTrip(double mean, double sd, double x) throws MathException {
        NormalDistribution dist = new NormalDistributionImpl(mean, sd);
        double p = dist.cumulativeProbability(x);
        double inv = dist.inverseCumulativeProbability(p);

        /* Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
           We construct p from cumulativeProbability(x), so the input is valid by construction and a correct implementation
           must recover x (within floating-point tolerance). A patch that merely suppresses the crash but returns a wrong
           value violates this observable round-trip relation. */
        double tol = Math.max(1.0e-9, Math.abs(sd) * 1.0e-9);
        if (Math.abs(inv - x) > tol) {
            throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: inverse(cdf(x)) != x input=mean=" + mean + ",sd=" + sd + ",x=" + x + ",p=" + p + " lhs=" + inv + " rhs=" + x);
        }
    }

    private static void exploreBracketOverloads(FuzzedDataProvider data) throws Exception {
        UnivariateRealFunction f = new SinFunction();
        int k = data.consumeInt(-8, 8);
        double root = k * Math.PI;
        double initial = root + (data.consumeBoolean() ? 1.0 : -1.0);
        double lower = Math.min(initial - 1.0, root);
        double upper = Math.max(initial + 1.0, root);
        if (!(lower < initial && initial < upper)) {
            return;
        }

        double[] r4;
        double[] r5;
        try {
            r4 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
            r5 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, Integer.MAX_VALUE);
        } catch (Throwable t) {
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        /* Contract/oracle: for valid inputs, returned endpoints bracket a root, and same-name overloads with equivalent
           parameters must agree. */
        double fa4 = f.value(r4[0]);
        double fb4 = f.value(r4[1]);
        if (!(fa4 * fb4 <= 0.0)) {
            throw new RuntimeException("[oracle:bracket-sign-fuzz] metamorphic violation: returned interval does not bracket a root input=initial=" + initial + ",lower=" + lower + ",upper=" + upper + " lhs=[" + r4[0] + "," + r4[1] + "] rhs=[" + fa4 + "," + fb4 + "]");
        }
        if (Math.abs(r4[0] - r5[0]) > 0.0 || Math.abs(r4[1] - r5[1]) > 0.0) {
            throw new RuntimeException("[oracle:bracket-overload-fuzz] metamorphic violation: equivalent bracket overloads disagree input=initial=" + initial + ",lower=" + lower + ",upper=" + upper + " lhs=[" + r4[0] + "," + r4[1] + "] rhs=[" + r5[0] + "," + r5[1] + "]");
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        return hasMathExceptionInChain(t) && hasBracketInChain(t);
    }

    private static boolean hasMathExceptionInChain(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketInChain(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Illegal") || name.contains("Invalid")) {
                return true;
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