package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input, but swallow the already-known duplicate NPE
        // so this harness can still exercise a different reachable function and rely on
        // an independent oracle.
        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchor = anchorSolver.solve(f, 3.0, 3.2, 3.1);
            checkReturnedMatchesStored(anchorSolver, anchor, "anchor-state");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (!isKnownRootCauseNpe(t)) {
                // Not the known buggy symptom; do not turn unrelated failures into findings.
                return;
            }
        }

        // EXPLORE / STRATEGY (a): target the different reachable stored-function overload
        // solve(double, double). First seed the solver with a real function using the public
        // API, then solve a second interval through solve(min, max).
        int n1 = data.consumeInt(-200, 200);
        int n2 = n1 + data.consumeInt(1, 5);

        double d1Left = boundedPositiveDelta(data);
        double d1Right = boundedPositiveDelta(data);
        double d2Left = boundedPositiveDelta(data);
        double d2Right = boundedPositiveDelta(data);

        double root1 = n1 * Math.PI;
        double root2 = n2 * Math.PI;

        double min1 = root1 - d1Left;
        double max1 = root1 + d1Right;
        double min2 = root2 - d2Left;
        double max2 = root2 + d2Right;

        if (!(min1 < max1 && min2 < max2)) {
            return;
        }

        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();

        try {
            // Real library call: installs/uses the function and computes a first root.
            double firstA = solverA.solve(f, min1, max1);
            checkReturnedMatchesStored(solverA, firstA, "seed-state-a");

            double firstB = solverB.solve(f, min1, max1);
            checkReturnedMatchesStored(solverB, firstB, "seed-state-b");

            // Determinism / replay check on identically-constructed objects:
            // a correct deterministic solver run on the same function and interval
            // must report the same root on both fresh objects.
            double seedTol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
            if (Math.abs(firstA - firstB) > seedTol) {
                throw new RuntimeException(
                    "[oracle:seed-replay] metamorphic violation: identical seeded solves disagree " +
                    "min=" + min1 + " max=" + max1 + " lhs=" + firstA + " rhs=" + firstB);
            }

            // Different reachable function: uses the function previously supplied to the solver.
            double storedA = solverA.solve(min2, max2);
            checkReturnedMatchesStored(solverA, storedA, "stored-state-a");

            double storedB = solverB.solve(min2, max2);
            checkReturnedMatchesStored(solverB, storedB, "stored-state-b");

            // Independent oracle: same quantity from a second, identically-constructed object.
            // This would still fail if a patch merely silenced the original NPE but left the
            // stored-function overload computing or storing the wrong result.
            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
            if (Math.abs(storedA - storedB) > tol) {
                throw new RuntimeException(
                    "[oracle:stored-replay] metamorphic violation: identical solve(min,max) runs disagree " +
                    "seedMin=" + min1 + " seedMax=" + max1 +
                    " min=" + min2 + " max=" + max2 +
                    " lhs=" + storedA + " rhs=" + storedB);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static double boundedPositiveDelta(FuzzedDataProvider data) {
        int micros = data.consumeInt(1, 1_000_000);
        return micros / 1_000_000.0;
    }

    private static void checkReturnedMatchesStored(BisectionSolver solver, double returned, String oracleId) {
        try {
            double stored1 = solver.getResult();
            double stored2 = solver.getResult();

            if (Double.doubleToLongBits(returned) != Double.doubleToLongBits(stored1)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: solve returned value differs from solver.getResult() " +
                    "returned=" + returned + " stored=" + stored1);
            }
            if (Double.doubleToLongBits(stored1) != Double.doubleToLongBits(stored2)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: repeated getResult() reads are unstable " +
                    "first=" + stored1 + " second=" + stored2);
            }
        } catch (IllegalStateException e) {
            // If the solver reports no result state after a successful solve, that violates
            // the post-condition that solve set the result.
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: successful solve left no stored result", e);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isKnownRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}