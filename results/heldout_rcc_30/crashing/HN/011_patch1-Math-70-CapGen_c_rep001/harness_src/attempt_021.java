package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Helper {
            boolean passesThroughPatchedSolve(Throwable t) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isGroundTruthRootCause(Throwable t) {
                return t instanceof NullPointerException && passesThroughPatchedSolve(t);
            }

            boolean isCleanRejection(Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                String n = t.getClass().getName();
                return n.startsWith("org.apache.commons.math.")
                        && (n.contains("Invalid")
                        || n.contains("Illegal")
                        || n.contains("Argument")
                        || n.contains("Convergence")
                        || n.contains("NoBracketing")
                        || n.contains("NotPositive")
                        || n.contains("OutOfRange"));
            }

            void handle(Throwable t) {
                if (isGroundTruthRootCause(t)) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
            }
        }
        Helper h = new Helper();

        {
            BisectionSolver solver = new BisectionSolver();
            SinFunction f = new SinFunction();
            try {
                double r = solver.solve(f, 3.0, 3.2, 3.1);
                if (Math.abs(r - Math.PI) > solver.getAbsoluteAccuracy()) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: exact test input should solve sin(x)=0 near pi input=[3.0,3.2,3.1] lhs=" + r + " rhs=" + Math.PI);
                }
            } catch (Throwable t) {
                h.handle(t);
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;

        int leftMilli = data.consumeInt(1, 1000);
        int rightMilli = data.consumeInt(1, 1000);
        double leftDelta = leftMilli / 1000.0;
        double rightDelta = rightMilli / 1000.0;

        double min = center - leftDelta;
        double max = center + rightDelta;
        double initial;
        if (data.consumeBoolean()) {
            initial = center;
        } else {
            int initMilli = data.consumeInt(-999, 999);
            initial = center + (initMilli / 1000.0) * Math.min(leftDelta, rightDelta);
        }
        if (initial < min) {
            initial = min;
        }
        if (initial > max) {
            initial = max;
        }

        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();
        SinFunction f = new SinFunction();

        Double withInitial = null;
        Double withoutInitial = null;

        try {
            withInitial = solverA.solve(f, min, max, initial);
        } catch (Throwable t) {
            h.handle(t);
            return;
        }

        try {
            withoutInitial = solverB.solve(f, min, max);
        } catch (Throwable t) {
            h.handle(t);
            return;
        }

        if (withInitial == null || withoutInitial == null) {
            return;
        }

        double acc = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());

        if (Math.abs(withInitial - center) > acc) {
            throw new RuntimeException("[oracle:root-center] metamorphic violation: for sin(x) on an interval constructed to bracket k*pi, solve(f,min,max,initial) must return that known root input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + center);
        }

        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > acc) {
            throw new RuntimeException("[oracle:overload-agree] metamorphic violation: same-name solve overloads with equivalent inputs must agree on the root input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + withoutInitial);
        }
    }
}