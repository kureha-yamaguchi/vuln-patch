package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        explore(data);
    }

    private static void anchor() {
        runOneCase(3.0d, 3.2d, 3.1d);
    }

    private static void explore(FuzzedDataProvider data) {
        int k = data.consumeInt(-12, 12);
        double root = k * Math.PI;

        double leftDelta = 0.01d + (data.consumeInt(0, 9900) / 10000.0d);
        double rightDelta = 0.01d + (data.consumeInt(0, 9900) / 10000.0d);

        double min = root - leftDelta;
        double max = root + rightDelta;
        if (!(min < max) || !Double.isFinite(min) || !Double.isFinite(max)) {
            return;
        }

        int mode = data.consumeInt(0, 5);
        double initial;
        switch (mode) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = root;
                break;
            case 3:
                initial = (min + max) / 2.0d;
                break;
            case 4:
                initial = min + (max - min) * (data.consumeInt(0, 10000) / 10000.0d);
                break;
            default:
                double edge = data.consumeBoolean() ? min : max;
                double wiggle = (data.consumeInt(0, 1000) / 1000000.0d);
                initial = data.consumeBoolean() ? edge + wiggle : edge - wiggle;
                if (initial < min) {
                    initial = min;
                }
                if (initial > max) {
                    initial = max;
                }
                break;
        }

        runOneCase(min, max, initial);
    }

    private static void runOneCase(double min, double max, double initial) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(initial) || !(min < max)) {
            return;
        }

        org.apache.commons.math.analysis.SinFunction f = new org.apache.commons.math.analysis.SinFunction();

        double baseline;
        BisectionSolver referenceSolver = new BisectionSolver();
        try {
            baseline = referenceSolver.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(1.0e-6d, 8.0d * referenceSolver.getAbsoluteAccuracy());

        BisectionSolver fourArgSolver = new BisectionSolver();
        try {
            double withInitial = fourArgSolver.solve(f, min, max, initial);

            /* Contract: a successful solve returns a root for the supplied function inside the interval.
             * A patch that merely deletes the throw or misroutes the call can return an unrelated value.
             */
            assertRootLooksValid("valid-bracket", min, max, withInitial, tol);

            /* Metamorphic relation on the same mathematical problem:
             * supplying an initial guess to BisectionSolver must not change the solved root for a valid bracketed interval.
             */
            if (Math.abs(withInitial - baseline) > tol) {
                throw new RuntimeException(
                    "[oracle:valid-bracket] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                        + " min=" + min + " max=" + max + " initial=" + initial
                        + " lhs=" + withInitial + " rhs=" + baseline);
            }
        } catch (Throwable t) {
            if (isKnownBugNpe(t)) {
                throw new RuntimeException(
                    "[oracle:valid-bracket] metamorphic violation: fresh 4-arg solve crashed on a valid bracket"
                        + " min=" + min + " max=" + max + " initial=" + initial
                        + " baseline=" + baseline, t);
            }
            return;
        }

        BisectionSolver twoArgSolver = new BisectionSolver();
        try {
            twoArgSolver.f = f;
            double twoArg = twoArgSolver.solve(min, max);

            /* Contract: solve(min,max) uses the receiver's installed function and must return the same root as the
             * explicitly-parameterized solve on the same interval. This also exercises the uncovered 2-arg entry point.
             */
            assertRootLooksValid("twoarg-bracket", min, max, twoArg, tol);

            if (Math.abs(twoArg - baseline) > tol) {
                throw new RuntimeException(
                    "[oracle:twoarg-bracket] metamorphic violation: solve(min,max) disagrees with solve(f,min,max)"
                        + " min=" + min + " max=" + max
                        + " lhs=" + twoArg + " rhs=" + baseline);
            }
        } catch (Throwable t) {
            if (isKnownBugNpe(t)) {
                throw new RuntimeException(
                    "[oracle:twoarg-bracket] metamorphic violation: stored-function 2-arg solve crashed on a valid bracket"
                        + " min=" + min + " max=" + max + " baseline=" + baseline, t);
            }
        }
    }

    private static void assertRootLooksValid(String oracleId, double min, double max, double root, double tol)
            throws org.apache.commons.math.FunctionEvaluationException {
        if (!Double.isFinite(root) || root < min - tol || root > max + tol) {
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: returned root is outside the requested interval"
                    + " min=" + min + " max=" + max + " root=" + root);
        }

        org.apache.commons.math.analysis.SinFunction f = new org.apache.commons.math.analysis.SinFunction();
        double residual = Math.abs(f.value(root));
        if (residual > 1.0e-4d) {
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: returned value is not a root of the supplied function"
                    + " min=" + min + " max=" + max + " root=" + root + " |sin(root)|=" + residual);
        }
    }

    private static boolean isKnownBugNpe(Throwable t) {
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