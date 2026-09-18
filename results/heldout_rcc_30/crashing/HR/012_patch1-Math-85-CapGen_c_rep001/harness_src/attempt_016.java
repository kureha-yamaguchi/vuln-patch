package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        sineEndpointEqualityOracle(data);
        normalConstructedEndpointOracle(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-seed-value] metamorphic violation: seeded inverseCumulativeProbability should recover 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                return;
            }
        }
    }

    private static void sineEndpointEqualityOracle(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        double initial = data.consumeInt(0, 1000) / 1000.0d;
        double lower = 0.0d;
        double upper = 1.05d + (data.consumeInt(0, 1900) / 1000.0d);
        int maxIterations = 1 + data.consumeInt(0, 50);

        try {
            double[] bracket = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);

            if (bracket == null || bracket.length != 2) {
                throw new RuntimeException("[oracle:sin-endpoint-contract] metamorphic violation: bracket must return two endpoints input=" + initial + "," + lower + "," + upper + " lhs=null rhs=length2");
            }

            double a = bracket[0];
            double b = bracket[1];
            double fa = f.value(a);
            double fb = f.value(b);

            if (!(a >= lower && b <= upper && a <= b)) {
                throw new RuntimeException("[oracle:sin-endpoint-contract] metamorphic violation: returned interval must stay within requested bounds input=" + initial + "," + lower + "," + upper + " lhs=[" + a + "," + b + "] rhs=in-bounds");
            }

            /* Contract of bracket: it returns an interval bracketing a root; endpoint roots are valid too,
               so for a correct implementation f(a) * f(b) must be <= 0 on the returned interval. */
            if ((fa * fb) > 0.0d) {
                throw new RuntimeException("[oracle:sin-endpoint-contract] metamorphic violation: returned interval does not bracket a root input=" + initial + "," + lower + "," + upper + " lhs=" + (fa * fb) + " rhs<=0");
            }

            /* We built an exact endpoint-root case by construction: lower bound is 0 and initial <= 1,
               therefore the first expansion reaches a=0 where sin(0)=0. A throw-deleting patch that
               returns some other interval would violate this observable endpoint-root property. */
            if (Math.abs(a - 0.0d) > 0.0d || Math.abs(fa) > 1.0e-15d) {
                throw new RuntimeException("[oracle:sin-endpoint-pin] metamorphic violation: exact endpoint root at 0 must be preserved input=" + initial + "," + lower + "," + upper + " lhs=a=" + a + ",fa=" + fa + " rhs=a=0,fa=0");
            }
        } catch (ConvergenceException e) {
            throw new RuntimeException("[oracle:sin-endpoint-accept] metamorphic violation: exact endpoint-root input should be accepted, not rejected input=" + initial + "," + lower + "," + upper + " cause=" + e, e);
        } catch (FunctionEvaluationException e) {
            return;
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void normalConstructedEndpointOracle(FuzzedDataProvider data) {
        double mean = data.consumeInt(-100000, 100000) / 1000.0d;
        double sd = data.consumeInt(1, 100000) / 1000.0d;

        NormalDistribution d1 = new NormalDistributionImpl(mean, sd);
        NormalDistribution d2 = new NormalDistributionImpl(mean, sd);

        double x = mean + (2.0d * sd);

        try {
            double p = d1.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d)) {
                return;
            }

            double inv = d1.inverseCumulativeProbability(p);
            double p2 = d2.cumulativeProbability(inv);

            /* Independent consistency check using only real library calls:
               p is obtained from cumulativeProbability(x), then recomputed on a fresh equivalent object
               at inverseCumulativeProbability(p). Any correct implementation must preserve the same
               probability up to numerical tolerance. This still fires if a patch merely suppresses
               the old throw but returns the wrong quantile. */
            double tolP = 1.0e-12d + 1.0e-8d * Math.max(Math.abs(p), Math.abs(p2));
            if (Math.abs(p - p2) > tolP) {
                throw new RuntimeException("[oracle:normal-prob-recompute] metamorphic violation: recomputed probability disagrees after inverse/cdf round-trip input=mean:" + mean + ",sd:" + sd + ",x:" + x + " lhs=" + p2 + " rhs=" + p);
            }

            double tolX = 1.0e-10d + 1.0e-8d * Math.max(Math.abs(x), Math.abs(inv));
            if (Math.abs(inv - x) > tolX) {
                throw new RuntimeException("[oracle:normal-exact-endpoint] metamorphic violation: inverse(cdf(x)) should recover constructed x for interior probability input=mean:" + mean + ",sd:" + sd + ",x:" + x + " lhs=" + inv + " rhs=" + x);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || (t.getClass().getName().contains("Invalid")
                || t.getClass().getName().contains("Illegal"));
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(m))
                    || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls) && "value".equals(m))) {
                return true;
            }
        }
        return false;
    }
}