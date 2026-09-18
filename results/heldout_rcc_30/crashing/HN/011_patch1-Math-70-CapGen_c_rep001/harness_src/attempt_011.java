package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        exerciseValidInput(f, 3.0d, 3.2d, 3.1d);

        // EXPLORE: generate many valid-by-construction intervals that bracket a real root of sin(x).
        // For sin, every k*pi is a root; choosing min=root-a and max=root+b with 0<a,b<pi/2
        // guarantees opposite signs and a valid interval for bisection.
        int iterations = data.consumeInt(1, 6);
        for (int i = 0; i < iterations; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            int leftMilli = data.consumeInt(1, 1500);
            int rightMilli = data.consumeInt(1, 1500);
            double left = leftMilli / 1000.0d;
            double right = rightMilli / 1000.0d;

            double min = root - left;
            double max = root + right;

            int initSelector = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (initSelector / 1000.0d);

            exerciseValidInput(f, min, max, initial);
        }
    }

    private static void exerciseValidInput(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, min, max, initial);

            // Metamorphic/post-condition:
            // The same-name overloads are documented to agree where their docs match. For BisectionSolver,
            // the 4-arg solve(f,min,max,initial) is supposed to behave like solve(f,min,max); the patch
            // itself restores that delegation. A throw-deleting or wrong-routing patch could return a value
            // different from the real 3-arg entry point without crashing.
            try {
                BisectionSolver solverWithoutInitial = new BisectionSolver();
                double withoutInitial = solverWithoutInitial.solve(f, min, max);

                double tolerance = Math.max(solverWithInitial.getAbsoluteAccuracy(),
                        solverWithoutInitial.getAbsoluteAccuracy()) * 4.0d;
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > tolerance) {
                    throw new RuntimeException(
                            "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable t) {
                // If either side of the relation throws, the relation does not apply to this input.
                if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                throw propagate(t);
            }
            return;
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("DimensionMismatch");
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
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

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}