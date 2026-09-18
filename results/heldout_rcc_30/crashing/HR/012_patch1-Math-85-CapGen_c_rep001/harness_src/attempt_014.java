package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        endpointRootOrientationOracle(data);
        validationOracle(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209;
            double q = normal.inverseCumulativeProbability(p);
            if (Math.abs(q - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-q2] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 for the standard normal input used by the regression test; p=" + p + " q=" + q);
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

    private static void endpointRootOrientationOracle(FuzzedDataProvider data) {
        int rootInt = data.consumeInt(-1000, 1000);
        double root = (double) rootInt;
        boolean rootOnRight = data.consumeBoolean();
        int maxIterations = data.consumeInt(1, 5);

        PolynomialFunction f = new PolynomialFunction(new double[] { -root, 1.0 });

        double initial;
        double lower;
        double upper;
        double expectedA;
        double expectedB;

        if (rootOnRight) {
            initial = root - 1.0;
            lower = root - 2.0;
            upper = root + 50.0;
            expectedA = root - 2.0;
            expectedB = root;
        } else {
            initial = root + 1.0;
            lower = root - 50.0;
            upper = root + 2.0;
            expectedA = root;
            expectedB = root + 2.0;
        }

        try {
            double[] bracketed = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);

            double a = bracketed[0];
            double b = bracketed[1];
            double fa = f.value(a);
            double fb = f.value(b);

            if (fa * fb > 0.0) {
                throw new RuntimeException("[oracle:endpoint-root-sign] metamorphic violation: returned interval must bracket a root; root=" + root + " a=" + a + " b=" + b + " fa=" + fa + " fb=" + fb);
            }

            if (a != expectedA || b != expectedB) {
                throw new RuntimeException("[oracle:endpoint-root-orient] metamorphic violation: with a linear polynomial and bounds chosen so the first expansion lands exactly on the known root, the shown bracket algorithm must return that first expanded interval; root=" + root + " initial=" + initial + " lower=" + lower + " upper=" + upper + " got=[" + a + "," + b + "] expected=[" + expectedA + "," + expectedB + "]");
            }

            if (!(fa == 0.0 || fb == 0.0)) {
                throw new RuntimeException("[oracle:endpoint-root-exact] metamorphic violation: this input is built so one expanded endpoint is exactly the constructed root of f(x)=x-root; root=" + root + " a=" + a + " b=" + b + " fa=" + fa + " fb=" + fb);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (ConvergenceException e) {
            if (hasFrame(e, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                return;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void validationOracle(FuzzedDataProvider data) {
        UnivariateRealFunction f = new PolynomialFunction(new double[] { 0.0, 1.0 });
        int selector = data.consumeInt(0, 2);
        try {
            if (selector == 0) {
                UnivariateRealSolverUtils.bracket(null, 0.0, -1.0, 1.0, 1);
            } else if (selector == 1) {
                UnivariateRealSolverUtils.bracket(f, 0.0, -1.0, 1.0, data.consumeInt(Integer.MIN_VALUE, 0));
            } else {
                double x = data.consumeInt(-100, 100);
                UnivariateRealSolverUtils.bracket(f, x, x + 1.0, x - 1.0, 1);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrame(t, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")
                || hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("IllegalArgument")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotStrictlyPositive")
                || name.contains("NullArgument");
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if (className.equals(ste.getClassName()) && methodName.equals(ste.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return hasFrame(cause, className, methodName);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}