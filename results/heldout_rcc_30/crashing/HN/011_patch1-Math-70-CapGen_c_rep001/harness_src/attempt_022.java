package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchor = anchorSolver.solve(f, 3.0, 3.2, 3.1);
            double expected = Math.PI;
            double tol = Math.max(anchorSolver.getAbsoluteAccuracy(), 1.0e-12);
            if (Math.abs(anchor - expected) > tol) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should solve sin(x)=0 near PI input=[3.0,3.2,3.1] result=" + anchor + " expected=" + expected + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromSolve(t)) {
                throwUnchecked(t);
                return;
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;
        int milli = data.consumeInt(1, 1000);
        double delta = milli / 1000.0;
        double min = root - delta;
        double max = root + delta;

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        if (!(min < max) || initial < min || initial > max) {
            return;
        }

        BisectionSolver solverA = new BisectionSolver();
        double rWithInitial;
        try {
            rWithInitial = solverA.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromSolve(t)) {
                throwUnchecked(t);
                return;
            }
            return;
        }

        BisectionSolver solverB = new BisectionSolver();
        double rWithoutInitial;
        try {
            rWithoutInitial = solverB.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are documented
         * to solve the same bracketed root for the same function and interval; for bisection,
         * the extra initial value is not part of the mathematical result. A throw-deleting or
         * wrong-delegation patch could make the 4-arg overload return a different result while
         * the 3-arg overload remains correct.
         */
        double tol = Math.max(Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()), 1.0e-6);
        if (Double.isNaN(rWithInitial) || Double.isNaN(rWithoutInitial) || Math.abs(rWithInitial - rWithoutInitial) > tol) {
            throw new RuntimeException("[oracle:overload] metamorphic violation: solve overloads disagree input=[k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "] lhs=" + rWithInitial + " rhs=" + rWithoutInitial + " tol=" + tol);
        }

        try {
            double value = f.value(rWithInitial);
            if (rWithInitial < min - tol || rWithInitial > max + tol || Math.abs(value) > 1.0e-4) {
                throw new RuntimeException("[oracle:root] metamorphic violation: returned value should be a root inside the bracket input=[k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "] result=" + rWithInitial + " f(result)=" + value);
            }
        } catch (FunctionEvaluationException ignored) {
        }
    }

    private static boolean isRootCauseFromSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("NoBracketing") || name.contains("Convergence") || name.contains("Argument");
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
        throw (T) t;
    }
}