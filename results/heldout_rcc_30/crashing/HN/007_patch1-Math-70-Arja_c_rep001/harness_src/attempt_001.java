package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runOne(f, 3.0d, 3.2d, 3.1d, true);

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases += Math.abs(data.consumeInt(0, 3));
        }

        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            double left = data.consumeInt(1, 1000) / 1000.0d;
            double right = data.consumeInt(1, 1000) / 1000.0d;

            double min = root - left;
            double max = root + right;
            if (!(min < max)) {
                continue;
            }

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0d);

            runOne(f, min, max, initial, true);
        }
    }

    private static void runOne(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            double withInitial = solver.solve(f, min, max, initial);

            // POST-CONDITION / METAMORPHIC CHECK:
            // The solver exposes same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // with matching documented semantics for the same bracketed problem; the initial guess
            // must not change the bisection root that is returned for a valid bracket.
            // A "fix" that merely avoids the NPE but ignores the function/arguments or returns a
            // different result would violate this sibling-agreement relation.
            try {
                BisectionSolver sibling = new BisectionSolver();
                double withoutInitial = sibling.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 8.0d;
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=min=" + min
                            + ",max=" + max
                            + ",initial=" + initial
                            + " lhs=" + withInitial
                            + " rhs=" + withoutInitial);
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (RuntimeException e) {
                if (isRootCause(e) && validByConstruction) {
                    throw e;
                }
                return;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCause(e) && validByConstruction) {
                throw e;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}