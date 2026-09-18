package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        exerciseValidationPaths(data);
        boundarySandwich(data);
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-boundary] exact failing-test quantile mismatch expected=2.0 actual=" + result);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isInverseBracketRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-boundary] valid standard-normal quantile threw on endpoint-root boundary", t);
            }
        }
    }

    private static void boundarySandwich(FuzzedDataProvider data) {
        int k = data.consumeInt(-8, 8);
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        NormalDistribution normal2 = new NormalDistributionImpl(0, 1);

        final double p0;
        try {
            p0 = normal.cumulativeProbability((double) k);
        } catch (Throwable t) {
            return;
        }

        if (!(p0 > 0.0d && p0 < 1.0d)) {
            return;
        }

        double pLo = Math.nextAfter(p0, 0.0d);
        double pHi = Math.nextAfter(p0, 1.0d);
        if (!(pLo > 0.0d && pHi < 1.0d && pLo < p0 && p0 < pHi)) {
            return;
        }

        double qLo;
        double q0;
        double qHi;
        try {
            qLo = inverseOrOracle(normal, pLo, "lo k=" + k + " p=" + pLo);
            q0 = inverseOrOracle(normal, p0, "mid k=" + k + " p=" + p0);
            qHi = inverseOrOracle(normal, pHi, "hi k=" + k + " p=" + pHi);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        /* For any correct monotone inverse CDF, probabilities ordered as pLo < p0 < pHi
           must produce ordered quantiles qLo <= q0 <= qHi, and since p0 was built as
           cumulativeProbability(k), the returned q0 must lie between the neighboring
           quantiles around that same probability boundary. A throw-deleting patch that
           returns a wrong value instead of signaling the bug breaks this observable order. */
        if (!(qLo <= q0 && q0 <= qHi && qLo <= (double) k && (double) k <= qHi)) {
            throw new RuntimeException("[oracle:boundary-sandwich] metamorphic violation: expected qLo<=q0<=qHi and qLo<=k<=qHi"
                    + " k=" + k + " pLo=" + pLo + " p0=" + p0 + " pHi=" + pHi
                    + " qLo=" + qLo + " q0=" + q0 + " qHi=" + qHi);
        }

        /* Independent cross-check from a second identically constructed object:
           the same quantile q0 must map back to the same probability p0 up to a tiny
           numerical tolerance. */
        try {
            double pBack = normal2.cumulativeProbability(q0);
            if (Math.abs(pBack - p0) > 1.0e-12d) {
                throw new RuntimeException("[oracle:boundary-backcheck] consistency violation: k=" + k
                        + " p0=" + p0 + " q0=" + q0 + " pBack=" + pBack);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Also drive the patched helper directly with a real library function near the flipped condition.
        try {
            SinFunction sin = new SinFunction();
            double[] bracket = UnivariateRealSolverUtils.bracket(sin, 1.0d, 0.0d, 2.0d, 1);
            double fa = sin.value(bracket[0]);
            double fb = sin.value(bracket[1]);
            if (!(fa * fb <= 0.0d)) {
                throw new RuntimeException("[oracle:sin-bracket-sign] metamorphic violation: returned interval does not bracket a root"
                        + " a=" + bracket[0] + " b=" + bracket[1] + " fa=" + fa + " fb=" + fb);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasStackFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                throw new RuntimeException("[oracle:sin-bracket-sign] valid endpoint-root bracket threw", t);
            }
        }
    }

    private static double inverseOrOracle(NormalDistribution normal, double p, String ctx) {
        try {
            return normal.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                throw new RuntimeException("[oracle:boundary-sandwich] unexpected clean rejection on valid probability " + ctx, t);
            }
            if (isInverseBracketRootCause(t)) {
                throw new RuntimeException("[oracle:boundary-sandwich] valid standard-normal quantile lookup threw " + ctx, t);
            }
            throw new RuntimeException("[oracle:boundary-sandwich] unexpected throwable " + ctx, t);
        }
    }

    private static void exerciseValidationPaths(FuzzedDataProvider data) {
        SinFunction sin = new SinFunction();
        try {
            UnivariateRealSolverUtils.bracket(null, 1.0d, 0.0d, 2.0d, 1);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                // Out-of-scope non-validation failure.
            }
        }

        try {
            int badIter = data.consumeBoolean() ? 0 : -Math.abs(data.consumeInt());
            UnivariateRealSolverUtils.bracket(sin, 1.0d, 0.0d, 2.0d, badIter);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                // Out-of-scope non-validation failure.
            }
        }

        try {
            double a = data.consumeInt(-10, 10);
            double b = data.consumeInt(-10, 10);
            double lower = Math.max(a, b);
            double upper = Math.min(a, b);
            double initial = lower;
            UnivariateRealSolverUtils.bracket(sin, initial, lower, upper, 1);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                // Out-of-scope non-validation failure.
            }
        }
    }

    private static boolean isInverseBracketRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasStackFrame(t, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")
                || hasStackFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("IllegalArgument")
                || n.contains("Invalid")
                || n.contains("OutOfRange")
                || n.contains("NoData")
                || n.contains("NullArgument");
    }

    private static boolean hasStackFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement e : t.getStackTrace()) {
            if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            for (StackTraceElement e : cause.getStackTrace()) {
                if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }
}