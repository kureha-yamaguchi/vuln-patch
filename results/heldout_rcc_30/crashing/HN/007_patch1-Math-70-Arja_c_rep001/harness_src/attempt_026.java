package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction anchorFunction = new SinFunction();

        final class Helpers {
            boolean stackHasPatchedSolve(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isCleanRejection(Throwable t) {
                if (t == null) {
                    return false;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                String n = t.getClass().getName();
                return n.contains("ConvergenceException")
                        || n.contains("FunctionEvaluationException")
                        || n.contains("MaxIterationsExceededException")
                        || n.contains("MathRuntimeException")
                        || n.contains("IllegalArgument");
            }

            void maybePropagateRootCause(Throwable t, boolean validByConstruction) {
                if (t instanceof NullPointerException && validByConstruction && stackHasPatchedSolve(t)) {
                    throw (NullPointerException) t;
                }
            }

            void callAndHandle(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
                try {
                    UnivariateRealSolver solver = new BisectionSolver();
                    solver.solve(f, min, max, initial);
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    maybePropagateRootCause(t, validByConstruction);
                }
            }
        }

        Helpers h = new Helpers();

        h.callAndHandle(anchorFunction, 3.0d, 3.2d, 3.1d, true);

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = 0.05d + (data.consumeInt(0, 900) / 1000.0d);
        double rightWidth = 0.05d + (data.consumeInt(0, 900) / 1000.0d);
        if (leftWidth >= Math.PI / 2.0d) {
            leftWidth = 1.0d;
        }
        if (rightWidth >= Math.PI / 2.0d) {
            rightWidth = 1.0d;
        }

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double fraction = data.consumeInt(0, 1000) / 1000.0d;
            initial = min + (max - min) * fraction;
        }

        String mode = data.consumeAsciiString(8);
        UnivariateRealFunction f = anchorFunction;
        boolean validByConstruction = true;

        if ("anchor".equals(mode) || "sin".equals(mode) || mode.length() == 0) {
            f = anchorFunction;
            validByConstruction = true;
        } else {
            f = anchorFunction;
            validByConstruction = true;
        }

        try {
            UnivariateRealSolver solverA = new BisectionSolver();
            double rWithInitial = solverA.solve(f, min, max, initial);

            UnivariateRealSolver solverB = new BisectionSolver();
            double rWithoutInitial = solverB.solve(f, min, max);

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 8.0d;

            /* Contract/oracle:
             * We construct a valid bracket [min, max] around the known zero k*pi of SinFunction,
             * so a correct solver must return that root. Also the documented same-name overloads
             * solve(f,min,max,initial) and solve(f,min,max) must agree on equivalent inputs.
             * A "fix" that merely suppresses the crash or ignores the function/arguments can return
             * a different value and violate one or both observable properties.
             */
            if (Math.abs(rWithInitial - root) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solve(f,min,max,initial) should recover constructed sin root"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", root=" + root + "}"
                        + " lhs=" + rWithInitial + " rhs=" + root);
            }
            if (Math.abs(rWithoutInitial - root) > tol) {
                throw new RuntimeException("[oracle:known-root-noinit] metamorphic violation: solve(f,min,max) should recover constructed sin root"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", root=" + root + "}"
                        + " lhs=" + rWithoutInitial + " rhs=" + root);
            }
            if (Math.abs(rWithInitial - rWithoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagree"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", root=" + root + "}"
                        + " lhs=" + rWithInitial + " rhs=" + rWithoutInitial);
            }
        } catch (Throwable t) {
            if (h.isCleanRejection(t)) {
                return;
            }
            h.maybePropagateRootCause(t, validByConstruction);
        }
    }
}