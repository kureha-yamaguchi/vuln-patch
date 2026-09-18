package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from the failing test.
        exerciseAndCheck(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction inputs with the same root-cause property:
        // a non-null function and a bracketing interval around PI for sin(x), so a correct
        // implementation is obligated to handle them and the patched line is exercised.
        int variants = 1 + Math.abs(data.consumeInt(0, 8));
        for (int i = 0; i < variants; i++) {
            double left = 0.000001 + (data.consumeInt(1, 100000) / 100000.0);
            double right = 0.000001 + (data.consumeInt(1, 100000) / 100000.0);
            double min = Math.PI - left;
            double max = Math.PI + right;
            if (!(min < Math.PI && Math.PI < max)) {
                continue;
            }

            double fraction = data.consumeInt(0, 1000000) / 1000000.0;
            double initial = min + (max - min) * fraction;
            if (data.consumeBoolean()) {
                initial = Math.PI;
            }

            exerciseAndCheck(f, min, max, initial, false);
        }
    }

    private static void exerciseAndCheck(UnivariateRealFunction f, double min, double max, double initial, boolean rethrowRootCause) {
        if (f == null) {
            return;
        }
        if (!(min < max)) {
            return;
        }
        if (!(min <= initial && initial <= max)) {
            return;
        }
        if (!(min < Math.PI && Math.PI < max)) {
            return;
        }

        Double withInitial = null;
        try {
            BisectionSolver solver = new BisectionSolver();
            withInitial = solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (rethrowRootCause && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        Double withoutInitial = null;
        try {
            BisectionSolver solver = new BisectionSolver();
            withoutInitial = solver.solve(f, min, max);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        if (withInitial == null || withoutInitial == null) {
            return;
        }

        double tolerance = new BisectionSolver().getAbsoluteAccuracy();

        // Contract used for this oracle:
        // the same-name overloads solve(f, min, max, initial) and solve(f, min, max)
        // are documented to agree where their docs match. For BisectionSolver, the initial
        // guess is not part of the bisection computation, so both real library calls must
        // return the same root on the same valid bracketing interval. A "fix" that merely
        // suppresses the crash but skips using f correctly would violate this agreement.
        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tolerance) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + withInitial + " rhs=" + withoutInitial
            );
        }

        if (Math.abs(withInitial.doubleValue() - Math.PI) > tolerance) {
            throw new RuntimeException(
                "[oracle:pi-root] metamorphic violation: sin(x) on an interval bracketing PI must solve to PI"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + withInitial + " rhs=" + Math.PI
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (e != null
                    && "org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
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
        return name.contains("ConvergenceException")
                || name.contains("EvaluationException")
                || name.contains("Argument")
                || name.contains("Invalid");
    }
}