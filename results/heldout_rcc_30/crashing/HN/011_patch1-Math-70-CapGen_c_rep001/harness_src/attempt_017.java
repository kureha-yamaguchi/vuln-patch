package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input. On the buggy build this reaches
        // BisectionSolver.solve(f, min, max, initial) and throws NPE from solve().
        callAndHandle(f, 3.0d, 3.2d, 3.1d, true);

        // EXPLORE: construct many valid-by-construction intervals for sin(x)
        // that bracket a known root k*pi. This satisfies the documented
        // precondition that the function argument is non-null, and supplies
        // ordinary moderate numeric inputs through the real public API.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftDelta = 0.05d + (data.consumeInt(0, 950) / 1000.0d);
        double rightDelta = 0.05d + (data.consumeInt(0, 950) / 1000.0d);

        double min = root - leftDelta;
        double max = root + rightDelta;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0d);
        } else {
            initial = root;
        }

        callAndHandle(f, min, max, initial, true);

        // Mandatory oracle: sibling-agreement metamorphic check.
        // Contract basis: the same-name overloads
        //   solve(f, min, max, initial)
        //   solve(f, min, max)
        // are documented to agree where their docs match. For BisectionSolver,
        // the initial value is not part of the bisection computation, so a
        // throw-deleting / branch-skipping "fix" that merely avoids the crash
        // but changes behavior would be caught if these real-library calls disagree.
        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();
        try {
            double withInitial = solverA.solve(f, min, max, initial);
            double withoutInitial = solverB.solve(f, min, max);

            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Double.isInfinite(withInitial) || Double.isInfinite(withoutInitial)) {
                throw new RuntimeException(
                        "[oracle:overload-agree] metamorphic violation: non-finite result input="
                                + "min=" + min + ",max=" + max + ",initial=" + initial
                                + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 10.0d;
            if (Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                        "[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max)"
                                + " input=min=" + min + ",max=" + max + ",initial=" + initial
                                + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (RuntimeException e) {
            if (isOracleRuntime(e)) {
                throw e;
            }
            if (isCleanRejection(e) || !hasSolveFrame(e)) {
                return;
            }
            if (e instanceof NullPointerException) {
                throw e;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (Exception e) {
            return;
        }
    }

    private static void callAndHandle(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (isOracleRuntime(t)) {
                throw t;
            }
            if (isRootCause(t, validByConstruction)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (Exception e) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        return validByConstruction
                && (t instanceof NullPointerException)
                && hasSolveFrame(t);
    }

    private static boolean hasSolveFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement frame : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(frame.getClassName())
                    && "solve".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleRuntime(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        String lower = name.toLowerCase();
        return lower.contains("invalid")
                || lower.contains("illegal")
                || lower.contains("bounds")
                || lower.contains("range")
                || lower.contains("argument");
    }
}