package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double r = solver.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle: all solve overloads for the same function and bracket
            // must agree on the root; this exact test expects pi within absolute accuracy.
            double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
            if (Math.abs(r - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: exact regression input should solve sin(x)=0 near pi input=[3.0,3.2,3.1] result=" + r + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throwUnchecked(t);
            }
            return;
        }

        int rounds = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 3) : 0);
        for (int i = 0; i < rounds; i++) {
            // EXPLORE: the bug-triggering property is calling solve(f,min,max,initial)
            // with a non-null function and a valid bracket. The buggy code ignores f and
            // dispatches to solve(min,max), which uses an unset internal function and NPEs.
            // Build valid-by-construction sin intervals around k*pi so a correct solver must accept them.
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int deltaMilli = data.consumeInt(100, 1000);
            double delta = deltaMilli / 1000.0; // in [0.1, 1.0], so sin changes sign around k*pi

            double min = root - delta;
            double max = root + delta;

            double initial;
            if (data.consumeBoolean()) {
                initial = root;
            } else {
                int offsetMilli = data.consumeInt(-deltaMilli + 1, deltaMilli - 1);
                initial = root + (offsetMilli / 1000.0);
            }

            Double withInitial;
            try {
                BisectionSolver s1 = new BisectionSolver();
                withInitial = Double.valueOf(s1.solve(f, min, max, initial));
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    continue;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                if (isOracleFailure(t)) {
                    throwUnchecked(t);
                }
                continue;
            }

            Double withoutInitial;
            try {
                BisectionSolver s2 = new BisectionSolver();
                withoutInitial = Double.valueOf(s2.solve(f, min, max));
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    continue;
                }
                if (isOracleFailure(t)) {
                    throwUnchecked(t);
                }
                continue;
            }

            // Documented same-name-overload agreement: solve(f,min,max,initial) and solve(f,min,max)
            // solve the same function on the same bracket. For a correct implementation, the unused
            // initial guess must not change the computed root for bisection. A throw-deleting patch
            // or wrong dispatch would break this equivalence.
            double lhs = withInitial.doubleValue();
            double rhs = withoutInitial.doubleValue();
            double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-9);
            if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on a valid bracket input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }

            // Oracle from known construction: we built a bracket around k*pi for sin(x)=0,
            // so the returned root must be close to that known root.
            if (Math.abs(lhs - root) > Math.max(tol, delta + 1.0e-9) || Math.abs(rhs - root) > Math.max(tol, delta + 1.0e-9)) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solver should return the known root inside the constructed sin bracket input=[" + min + "," + max + "," + initial + "] expectedRoot=" + root + " lhs=" + lhs + " rhs=" + rhs);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("NotStrictlyPositive")
                || name.contains("NotPositive");
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}