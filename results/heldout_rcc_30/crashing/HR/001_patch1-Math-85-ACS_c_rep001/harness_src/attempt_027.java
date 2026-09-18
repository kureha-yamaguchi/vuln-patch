package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runRegressionAnchor();
        explorePublicBoundary(data);
        explorePatchedConditionDirectly(data);
    }

    private static void runRegressionAnchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:seed-known-answer-exact] metamorphic violation: inverseCumulativeProbability(cdf(2)) should recover 2.0 for the regression seed, got " + result);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                return;
            }
        }
    }

    private static void explorePublicBoundary(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000);
        NormalDistributionImpl dist = new NormalDistributionImpl(mean, 1.0d);
        double x = mean + 2.0d;

        try {
            double pExact = dist.cumulativeProbability(x);

            Double qExact = checkedInverse(dist, pExact);
            if (qExact != null && Math.abs(qExact.doubleValue() - x) > 1.0e-12d) {
                throw new RuntimeException("[oracle:constructed-public-fixedpoint] metamorphic violation: for valid probability p=cdf(x), inverseCumulativeProbability(p) must recover x; x=" + x + " p=" + pExact + " q=" + qExact);
            }

            double pLo = previousFiniteProbability(pExact);
            double pHi = nextFiniteProbability(pExact);

            Double qLo = checkedInverse(dist, pLo);
            Double qHi = checkedInverse(dist, pHi);

            if (qLo != null && qExact != null && qHi != null) {
                if (!(qLo.doubleValue() <= qExact.doubleValue() && qExact.doubleValue() <= qHi.doubleValue())) {
                    throw new RuntimeException("[oracle:ulp-neighbor-order] metamorphic violation: inverseCumulativeProbability must preserve order on neighboring valid probabilities; pLo=" + pLo + " p=" + pExact + " pHi=" + pHi + " qLo=" + qLo + " q=" + qExact + " qHi=" + qHi);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void explorePatchedConditionDirectly(FuzzedDataProvider data) {
        int maxIterations = data.consumeInt(1, 1000);
        SinFunction f = new SinFunction();

        try {
            double[] bracketed = UnivariateRealSolverUtils.bracket(f, 1.0d, 0.0d, 2.0d, maxIterations);

            if (bracketed == null || bracketed.length != 2) {
                throw new RuntimeException("[oracle:exact-endpoint-bracket-shape] metamorphic violation: bracket must return a 2-element interval");
            }

            double a = bracketed[0];
            double b = bracketed[1];
            double fa = f.value(a);
            double fb = f.value(b);

            if (a != 0.0d || b != 2.0d) {
                throw new RuntimeException("[oracle:exact-endpoint-bracket-values] metamorphic violation: with initial=1, bounds=[0,2], and sin(0)=0, the one-step bracket must be [0,2]; got [" + a + "," + b + "]");
            }

            if (!(fa == 0.0d || fb == 0.0d || fa * fb < 0.0d)) {
                throw new RuntimeException("[oracle:exact-endpoint-bracket-contract] metamorphic violation: returned interval must bracket a root, got f(a)=" + fa + " f(b)=" + fb);
            }

            try {
                double solved = new BrentSolver().solve(f, a, b);
                if (Math.abs(solved - 0.0d) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:endpoint-solver-composition] metamorphic violation: solving the returned bracket for sin on [0,2] must recover the endpoint root 0; got " + solved);
                }
            } catch (Throwable solverThrowable) {
                if (!isCleanRejection(solverThrowable)) {
                    if (solverThrowable instanceof RuntimeException) {
                        throw (RuntimeException) solverThrowable;
                    }
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof ConvergenceException && hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                throw new RuntimeException("[oracle:exact-endpoint-accepted] metamorphic violation: a valid interval with an exact endpoint root must be accepted by bracket; initial=1.0 lower=0.0 upper=2.0 maxIterations=" + maxIterations, t);
            }
            if (t instanceof FunctionEvaluationException) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static Double checkedInverse(NormalDistributionImpl dist, double p) {
        try {
            return new Double(dist.inverseCumulativeProbability(p));
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isBracketRootCause(t)) {
                return null;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isBracketRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                if (hasFrame(cur, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")
                        || hasFrame(cur, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double previousFiniteProbability(double p) {
        if (p <= 0.0d) {
            return p;
        }
        long bits = Double.doubleToLongBits(p);
        return Double.longBitsToDouble(bits - 1L);
    }

    private static double nextFiniteProbability(double p) {
        if (p >= 1.0d) {
            return p;
        }
        long bits = Double.doubleToLongBits(p);
        return Double.longBitsToDouble(bits + 1L);
    }
}