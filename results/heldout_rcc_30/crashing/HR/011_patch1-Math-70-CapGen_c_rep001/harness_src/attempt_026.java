package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeedSilently();
        exploreStateMaskedWrongFunction(data);
    }

    private static void runAnchorSeedSilently() {
        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthNpeFromPatchedRegion(t)) {
                return;
            }
        }
    }

    private static void exploreStateMaskedWrongFunction(FuzzedDataProvider data) {
        double center = boundedDouble(data.consumeInt(), -100.0, 100.0);
        double separation = 0.25 + boundedDouble(data.consumeInt(), 0.0, 5.0);
        double halfWidth = separation + 0.5 + boundedDouble(data.consumeInt(), 0.0, 5.0);

        boolean explicitOnRight = data.consumeBoolean();
        double storedRoot = explicitOnRight ? center - separation : center + separation;
        double explicitRoot = explicitOnRight ? center + separation : center - separation;

        double min = Math.min(storedRoot, explicitRoot) - halfWidth;
        double max = Math.max(storedRoot, explicitRoot) + halfWidth;
        if (!(min < max)) {
            return;
        }

        double initial = min + (max - min) * (0.1 + 0.8 * boundedDouble(data.consumeInt(), 0.0, 1.0));

        PolynomialFunction storedFunction = linearRoot(storedRoot);
        PolynomialFunction explicitFunction = linearRoot(explicitRoot);

        BisectionSolver pollutedSolver = new BisectionSolver();
        pollutedSolver.f = storedFunction;

        BisectionSolver freshSolver = new BisectionSolver();

        double pollutedResult;
        double freshResult;
        try {
            pollutedResult = pollutedSolver.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthNpeFromPatchedRegion(t)) {
                return;
            }
            return;
        }

        try {
            freshResult = freshSolver.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            return;
        }

        double acc = Math.max(pollutedSolver.getAbsoluteAccuracy(), freshSolver.getAbsoluteAccuracy());
        double encodedDistance = Math.abs(pollutedResult - explicitRoot);
        double crossDistance = Math.abs(pollutedResult - freshResult);

        /*
         * Contract/oracle:
         * The overload solve(f, min, max, initial) is documented to solve for its explicit
         * function parameter f. For the linear polynomial x - r, the unique root is exactly r.
         * Therefore a correct implementation must return a value within solver accuracy of r,
         * regardless of any unrelated function previously stored on the receiver.
         *
         * Independent cross-check:
         * The same quantity is obtained a second way from a fresh solver via solve(f, min, max).
         * A band-aid that suppresses the known NPE but still uses receiver state instead of the
         * explicit parameter will return the stored function's root and violate both checks.
         */
        if (encodedDistance > 8.0 * acc && crossDistance > 8.0 * acc) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:state-pollution-root] explicit solve ignored its function parameter"
                    + " storedRoot=" + storedRoot
                    + " explicitRoot=" + explicitRoot
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " pollutedResult=" + pollutedResult
                    + " freshResult=" + freshResult
                    + " accuracy=" + acc);
        }

        try {
            double cached = pollutedSolver.getResult();
            if (Math.abs(cached - pollutedResult) > 0.0) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:cached-result-agrees] cached result disagrees with returned root"
                        + " cached=" + cached
                        + " returned=" + pollutedResult);
            }
        } catch (IllegalStateException ignored) {
            return;
        }
    }

    private static PolynomialFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
            || name.contains("Argument")
            || name.contains("Invalid")
            || name.contains("NoBracketing")
            || name.contains("OutOfRange");
    }

    private static boolean isGroundTruthNpeFromPatchedRegion(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long u = raw & 0xffffffffL;
        double unit = u / (double) 0xffffffffL;
        return min + (max - min) * unit;
    }
}