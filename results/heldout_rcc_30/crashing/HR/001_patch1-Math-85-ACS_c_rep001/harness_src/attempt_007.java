package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runAnchorThroughPublicApi();

        int root = data.consumeInt(-1000, 1000);
        int scale = data.consumeInt(1, 1000);
        boolean hitRightEndpoint = data.consumeBoolean();
        int extraSpan = data.consumeInt(2, 50);
        int maxIterations = data.consumeInt(1, 1000);

        PolynomialFunction base = new PolynomialFunction(new double[] { -root, 1.0d });
        PolynomialFunction scaled = new PolynomialFunction(new double[] { -((double) scale) * root, (double) scale });

        double initial;
        double lower;
        double upper;

        if (hitRightEndpoint) {
            initial = root - 1.0d;
            lower = root - 2.0d;
            upper = root + extraSpan;
        } else {
            initial = root + 1.0d;
            lower = root - extraSpan;
            upper = root + 2.0d;
        }

        double[] first = mustBracketExactEndpointRoot(base, initial, lower, upper, maxIterations, root, "base");
        double[] second = mustBracketExactEndpointRoot(scaled, initial, lower, upper, maxIterations, root, "scaled");

        if (first != null && second != null) {
            if (Double.doubleToLongBits(first[0]) != Double.doubleToLongBits(second[0])
                    || Double.doubleToLongBits(first[1]) != Double.doubleToLongBits(second[1])) {
                throw new RuntimeException(
                        "[oracle:scale-invariant] metamorphic violation: positive scaling preserves signs and zeroes, so bracket must return the same interval"
                                + " root=" + root
                                + " initial=" + initial
                                + " lower=" + lower
                                + " upper=" + upper
                                + " maxIterations=" + maxIterations
                                + " base=[" + first[0] + "," + first[1] + "]"
                                + " scaled=[" + second[0] + "," + second[1] + "]");
            }
        }
    }

    private static void runAnchorThroughPublicApi() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);
            double p = 0.9772498680518209d;
            double x = normal.inverseCumulativeProbability(p);
            double back = normal.cumulativeProbability(x);
            if (Math.abs(back - p) > 1.0e-12d) {
                throw new RuntimeException(
                        "[oracle:anchor-cdf-inverse] metamorphic violation: for a valid probability p, cdf(invCdf(p)) must recover p"
                                + " p=" + p + " x=" + x + " back=" + back);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasBracketFrame(t)) {
                return;
            }
        }
    }

    private static double[] mustBracketExactEndpointRoot(PolynomialFunction f, double initial, double lower,
            double upper, int maxIterations, int expectedRoot, String tag) {
        final double[] interval;
        try {
            interval = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);
        } catch (Throwable t) {
            if (hasBracketFrame(t)) {
                throw new RuntimeException(
                        "[oracle:endpoint-zero-accepted] unexpected rejection: a correct bracket implementation must accept an interval whose endpoint value is exactly zero"
                                + " tag=" + tag
                                + " root=" + expectedRoot
                                + " initial=" + initial
                                + " lower=" + lower
                                + " upper=" + upper
                                + " maxIterations=" + maxIterations
                                + " thrown=" + t.getClass().getName(),
                        t);
            }
            return null;
        }

        try {
            double fa = f.value(interval[0]);
            double fb = f.value(interval[1]);

            if (!(interval[0] <= expectedRoot && expectedRoot <= interval[1])) {
                throw new RuntimeException(
                        "[oracle:root-contained] metamorphic violation: on success bracket must return an interval containing a root"
                                + " tag=" + tag
                                + " root=" + expectedRoot
                                + " interval=[" + interval[0] + "," + interval[1] + "]"
                                + " f(a)=" + fa
                                + " f(b)=" + fb);
            }

            double product = fa * fb;
            if (!(product <= 0.0d || fa == 0.0d || fb == 0.0d)) {
                throw new RuntimeException(
                        "[oracle:sign-bracket] metamorphic violation: returned interval must bracket a root, so endpoint values must have opposite signs or one must be zero"
                                + " tag=" + tag
                                + " root=" + expectedRoot
                                + " interval=[" + interval[0] + "," + interval[1] + "]"
                                + " f(a)=" + fa
                                + " f(b)=" + fb
                                + " product=" + product);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return null;
        }

        return interval;
    }

    private static boolean hasBracketFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.endsWith("InvalidRepresentationException")
                || n.endsWith("NoDataException")
                || n.endsWith("NullArgumentException")
                || n.endsWith("OutOfRangeException")
                || n.endsWith("NotPositiveException")
                || n.endsWith("NotStrictlyPositiveException")
                || n.endsWith("NumberIsTooLargeException")
                || n.endsWith("NumberIsTooSmallException")
                || n.endsWith("MathRuntimeException")
                || n.endsWith("MathException");
    }
}