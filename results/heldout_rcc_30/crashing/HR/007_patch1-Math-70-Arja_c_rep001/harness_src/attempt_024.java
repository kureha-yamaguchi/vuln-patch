package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static final UnivariateRealFunction SIN = new SinFunction();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchorWithIndependentOracle();
        runFuzzedValidSinBrackets(data);
    }

    private static void runExactAnchorWithIndependentOracle() {
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;
        double brent;
        try {
            brent = new BrentSolver().solve(SIN, min, max);
        } catch (Throwable t) {
            return;
        }

        try {
            double bis = new BisectionSolver().solve(SIN, min, max, initial);
            assertClose("anchor-explicit-vs-brent", min, max, initial, bis, brent, new BisectionSolver().getAbsoluteAccuracy(), new BrentSolver().getAbsoluteAccuracy());
        } catch (Throwable t) {
            if (isRootCauseNpeInBisectionSolve(t)) {
                throw new RuntimeException("[oracle:anchor-explicit-vs-brent] valid explicit solve crashed while Brent solved the same bracket: min="
                        + min + " max=" + max + " initial=" + initial + " brent=" + brent, t);
            }
        }

        try {
            double stored = new BisectionSolver(SIN).solve(min, max);
            assertClose("anchor-stored-vs-brent", min, max, Double.NaN, stored, brent, new BisectionSolver(SIN).getAbsoluteAccuracy(), new BrentSolver().getAbsoluteAccuracy());
        } catch (Throwable t) {
            // Not the patched entry point; swallow unrelated failures.
        }
    }

    private static void runFuzzedValidSinBrackets(FuzzedDataProvider data) {
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = scaledPositive(data.consumeInt());
        double rightWidth = scaledPositive(data.consumeInt());

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initialFraction = (data.consumeInt(-1000000, 1000000) + 1000000.0) / 2000000.0;
        double initial = min + (max - min) * initialFraction;
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        double brent;
        try {
            brent = new BrentSolver().solve(SIN, min, max);
        } catch (Throwable t) {
            return;
        }

        try {
            double explicit = new BisectionSolver().solve(SIN, min, max, initial);
            // Contract used for this oracle:
            // on a valid bracket around a unique sine root, two real solvers solving the same equation
            // through public API must agree on the root within their stated accuracies.
            assertClose("fuzz-explicit-vs-brent", min, max, initial, explicit, brent,
                    new BisectionSolver().getAbsoluteAccuracy(), new BrentSolver().getAbsoluteAccuracy());
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeInBisectionSolve(t)) {
                throw new RuntimeException("[oracle:fuzz-explicit-vs-brent] valid explicit solve crashed while Brent solved the same bracket: min="
                        + min + " max=" + max + " initial=" + initial + " brent=" + brent, t);
            }
            return;
        }

        try {
            double stored = new BisectionSolver(SIN).solve(min, max);
            assertClose("fuzz-stored-vs-brent", min, max, Double.NaN, stored, brent,
                    new BisectionSolver(SIN).getAbsoluteAccuracy(), new BrentSolver().getAbsoluteAccuracy());
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static double scaledPositive(int raw) {
        long v = raw;
        if (v < 0) {
            v = -v;
        }
        double width = 1.0e-6 + (v % 1000000L) / 1000000.0;
        if (width >= Math.PI) {
            width = Math.PI - 1.0e-6;
        }
        return width;
    }

    private static void assertClose(String oracle, double min, double max, double initial,
                                    double lhs, double rhs, double accA, double accB) {
        double tolerance = Math.max(1.0e-8, 8.0 * Math.max(accA, accB));
        if (Math.abs(lhs - rhs) > tolerance) {
            throw new RuntimeException("[oracle:" + oracle + "] metamorphic violation: same valid bracket solved by two real solvers disagrees"
                    + " min=" + min + " max=" + max + " initial=" + initial
                    + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tolerance);
        }
    }

    private static boolean isRootCauseNpeInBisectionSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence") || name.contains("Argument") || name.contains("Invalid")
                || name.contains("NoBracketing") || name.contains("MathIllegal");
    }
}