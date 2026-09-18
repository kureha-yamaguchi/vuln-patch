package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int trials = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < trials; i++) {
            int root = data.consumeInt(1, 1000);
            boolean useFourArg = data.consumeBoolean();
            int maxIterations = 1 + data.consumeInt(0, 3);
            bracketThenSolveOracle(root, useFourArg, maxIterations);
        }

        normalScaleOracle(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isKnownInverseRootCause(t)) {
                return;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void bracketThenSolveOracle(int root, boolean useFourArg, int maxIterations) {
        PolynomialFunction f = new PolynomialFunction(new double[] { -root, 1.0 });
        double initial = root - 1.0;
        double lower = root - 2.0;
        double upper = root + 10.0;

        double[] interval;
        try {
            if (useFourArg) {
                interval = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
            } else {
                interval = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                throw new RuntimeException(
                    "[oracle:bracket-solve] valid linear polynomial with an endpoint root should bracket and be solvable, but bracket threw "
                        + t.getClass().getName()
                        + " root=" + root
                        + " initial=" + initial
                        + " lower=" + lower
                        + " upper=" + upper
                        + " useFourArg=" + useFourArg
                        + " maxIterations=" + maxIterations,
                    t);
            }
            return;
        }

        if (interval == null || interval.length != 2) {
            throw new RuntimeException("[oracle:bracket-solve] invalid interval array for root=" + root);
        }

        double a = interval[0];
        double b = interval[1];
        double fa;
        double fb;
        try {
            fa = f.value(a);
            fb = f.value(b);
        } catch (Throwable t) {
            return;
        }

        if (!(a <= root && root <= b) || fa * fb > 0.0) {
            throw new RuntimeException(
                "[oracle:bracket-solve] returned interval does not actually bracket the known root"
                    + " root=" + root
                    + " a=" + a
                    + " b=" + b
                    + " fa=" + fa
                    + " fb=" + fb);
        }

        try {
            BisectionSolver solver = new BisectionSolver();
            double solved = solver.solve(f, a, b);
            if (Math.abs(solved - root) > 1.0e-9) {
                throw new RuntimeException(
                    "[oracle:bracket-solve] solver disagrees with known linear root"
                        + " root=" + root
                        + " solved=" + solved
                        + " a=" + a
                        + " b=" + b);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void normalScaleOracle(FuzzedDataProvider data) {
        int numer = data.consumeInt(1, 999999);
        double p = numer / 1000000.0;
        double mu = data.consumeInt(-1000, 1000);
        double sigma = data.consumeInt(1, 1000) / 10.0;

        try {
            NormalDistribution standard = new NormalDistributionImpl(0, 1);
            NormalDistribution shifted = new NormalDistributionImpl(mu, sigma);

            double x = standard.inverseCumulativeProbability(p);
            double y = shifted.inverseCumulativeProbability(p);
            double expected = mu + sigma * x;
            double tol = 1.0e-8 * Math.max(1.0, Math.abs(expected));

            if (Math.abs(y - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:normal-scale] inverse CDF should respect location-scale transformation"
                        + " p=" + p
                        + " mu=" + mu
                        + " sigma=" + sigma
                        + " std=" + x
                        + " shifted=" + y
                        + " expected=" + expected
                        + " tol=" + tol);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownInverseRootCause(t)) {
                return;
            }
        }
    }

    private static boolean isKnownInverseRootCause(Throwable t) {
        return t instanceof MathException
            && (hasFrame(t, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")
                || hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket"));
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Illegal")
            || name.contains("Invalid")
            || name.contains("OutOfRange")
            || name.contains("NoBracketing");
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st == null) {
                continue;
            }
            for (int i = 0; i < st.length; i++) {
                if (className.equals(st[i].getClassName()) && methodName.equals(st[i].getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }
}