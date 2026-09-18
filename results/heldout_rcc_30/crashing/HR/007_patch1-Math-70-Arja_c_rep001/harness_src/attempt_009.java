package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.ConvergenceException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        runShiftedConstructorSolveCheck(data);
        runShiftedPatchedSolveCheck(data);
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double r1 = solver.solve(f, 3.0, 3.2, 3.1);
            double r2 = solver.solve(f, 3.0 + (2.0 * Math.PI), 3.2 + (2.0 * Math.PI), 3.1 + (2.0 * Math.PI));
            double expectedShift = 2.0 * Math.PI;
            double actualShift = r2 - r1;
            double tol = Math.max(1.0e-6, 8.0 * solver.getAbsoluteAccuracy());
            if (Math.abs(actualShift - expectedShift) > tol) {
                throw new RuntimeException("[oracle:anchor-periodicity] metamorphic violation: sin root should shift by 2pi under interval translation input="
                        + "[3.0,3.2]->[" + (3.0 + (2.0 * Math.PI)) + "," + (3.2 + (2.0 * Math.PI)) + "] lhs="
                        + actualShift + " rhs=" + expectedShift);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runShiftedConstructorSolveCheck(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        int k = data.consumeInt(-1000, 1000);
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double root = k * Math.PI;
        double min = root - left;
        double max = root + right;
        double shift = ((data.consumeBoolean() ? 1 : -1) * 2.0 * Math.PI);

        BisectionSolver solverA = new BisectionSolver(f);
        BisectionSolver solverB = new BisectionSolver(f);
        try {
            double r1 = solverA.solve(min, max);
            double r2 = solverB.solve(min + shift, max + shift);

            /* For SinFunction, translating the interval by a full period translates the root by
             * the same amount. A correct solver must preserve this metamorphic relation.
             */
            double tol = Math.max(1.0e-6, 8.0 * Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()));
            double expected = shift;
            double actual = r2 - r1;
            if (Math.abs(actual - expected) > tol) {
                throw new RuntimeException("[oracle:ctor-periodic-shift] metamorphic violation: translated sin interval must translate the reported root by the same period input="
                        + "[" + min + "," + max + "] shift=" + shift + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (ConvergenceException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (RuntimeException e) {
            if (shouldPropagateRootCause(e)) {
                throw e;
            }
        }
    }

    private static void runShiftedPatchedSolveCheck(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        int k = data.consumeInt(-1000, 1000);
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double root = k * Math.PI;
        double min = root - left;
        double max = root + right;
        double span = max - min;
        double ratio1 = data.consumeInt(0, 1000) / 1000.0;
        double ratio2 = data.consumeInt(0, 1000) / 1000.0;
        double initial1 = min + ratio1 * span;
        double initial2 = (min + (2.0 * Math.PI)) + ratio2 * span;

        BisectionSolver solver = new BisectionSolver();
        try {
            double r1 = solver.solve(f, min, max, initial1);
            double r2 = solver.solve(f, min + (2.0 * Math.PI), max + (2.0 * Math.PI), initial2);

            /* This overload solves the supplied non-null function on the given interval.
             * For sin, shifting the entire problem by 2*pi must shift the reported root by 2*pi.
             */
            double expected = 2.0 * Math.PI;
            double actual = r2 - r1;
            double tol = Math.max(1.0e-6, 8.0 * solver.getAbsoluteAccuracy());
            if (Math.abs(actual - expected) > tol) {
                throw new RuntimeException("[oracle:param-periodic-shift] metamorphic violation: translated 4-arg solve must translate the root by 2pi input="
                        + "[" + min + "," + max + "] initials=" + initial1 + "," + initial2 + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (ConvergenceException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (RuntimeException e) {
            if (shouldPropagateRootCause(e)) {
                throw e;
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
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

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
        throw (T) t;
    }
}