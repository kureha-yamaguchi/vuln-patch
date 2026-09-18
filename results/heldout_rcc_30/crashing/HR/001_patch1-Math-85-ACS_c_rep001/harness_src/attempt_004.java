package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorSeed();
        exploreBoundaryRootAndCrossCheck(data);
        probeDocumentedRejections(data);
    }

    private static void anchorSeed() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-exact] inverseCumulativeProbability(cdf(2)) should return 2.0 for the failing seed, got " + result);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathExceptionFamily(t) && hasFrameInChain(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                throw new RuntimeException("[oracle:anchor-exact] ground-truth failure reproduced through NormalDistribution seed", t);
            }
        }
    }

    private static void exploreBoundaryRootAndCrossCheck(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        double lowerBound = 0.0;
        double initial = data.consumeInt(0, 1000) / 1000.0;
        double upperBound = 1.10 + (data.consumeInt(0, 180) / 100.0);
        int maximumIterations = 1 + data.consumeInt(0, 20);

        try {
            double[] bracket = UnivariateRealSolverUtils.bracket(f, initial, lowerBound, upperBound, maximumIterations);

            /*
             * Soundness: bracket returns endpoints that bracket a root; for our constructed inputs,
             * sin(0) = 0 and upperBound < pi, so the only root in [0, upperBound] is 0.
             * Therefore any correct solver invoked on the returned bracket must find the same root
             * regardless of the solve overload used. A patch that merely suppresses the throw or
             * returns a wrong interval will violate this agreement even if no exception is thrown.
             */
            try {
                BisectionSolver solver = new BisectionSolver();
                double r1 = solver.solve(f, bracket[0], bracket[1]);
                double r2 = solver.solve(f, bracket[0], bracket[1], 1.0e-12);
                double v1 = f.value(r1);
                double v2 = f.value(r2);

                if (Math.abs(r1 - r2) > 1.0e-8 || Math.abs(v1) > 1.0e-8 || Math.abs(v2) > 1.0e-8) {
                    throw new RuntimeException(
                        "[oracle:solver-agreement] solve overloads disagree or did not find a root: " +
                        "initial=" + initial +
                        " lower=" + lowerBound +
                        " upper=" + upperBound +
                        " maxIter=" + maximumIterations +
                        " bracket=[" + bracket[0] + "," + bracket[1] + "]" +
                        " r1=" + r1 +
                        " r2=" + r2 +
                        " f(r1)=" + v1 +
                        " f(r2)=" + v2
                    );
                }
            } catch (Throwable t) {
                if (!isCleanRejection(t)) {
                    if (t instanceof RuntimeException && startsWithOracle((RuntimeException) t)) {
                        throw (RuntimeException) t;
                    }
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasFrameInChain(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                throw new RuntimeException(
                    "[oracle:boundary-root] valid bracket input hit the fa*fb==0 boundary but still failed: " +
                    "initial=" + initial +
                    " lower=" + lowerBound +
                    " upper=" + upperBound +
                    " maxIter=" + maximumIterations,
                    t
                );
            }
        }
    }

    private static void probeDocumentedRejections(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        int selector = data.consumeInt(0, 2);
        try {
            if (selector == 0) {
                UnivariateRealSolverUtils.bracket(null, 0.5, 0.0, 2.0, 2);
            } else if (selector == 1) {
                int badIterations = -1 - Math.abs(data.consumeInt());
                UnivariateRealSolverUtils.bracket(f, 0.5, 0.0, 2.0, badIterations);
            } else {
                double lower = 1.0;
                double upper = 0.0;
                double initial = 0.5;
                UnivariateRealSolverUtils.bracket(f, initial, lower, upper, 2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isMathExceptionFamily(Throwable t) {
        while (t != null) {
            if (t instanceof MathException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("Validation")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean hasFrameInChain(Throwable t, String className, String methodName) {
        while (t != null) {
            StackTraceElement[] trace = t.getStackTrace();
            if (trace != null) {
                for (int i = 0; i < trace.length; i++) {
                    StackTraceElement e = trace[i];
                    if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean startsWithOracle(RuntimeException t) {
        String m = t.getMessage();
        return m != null && m.startsWith("[oracle:");
    }
}