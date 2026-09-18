package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver solver = new BisectionSolver();
            double rWithInitial = solver.solve(f, 3.0, 3.2, 3.1);

            BisectionSolver sibling = new BisectionSolver();
            double rWithoutInitial = sibling.solve(f, 3.0, 3.2);

            double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 8.0;
            if (Math.abs(rWithInitial - rWithoutInitial) > tol) {
                throw new RuntimeException(
                    "[oracle:anchor-sibling] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket"
                        + " lhs=" + rWithInitial
                        + " rhs=" + rWithoutInitial
                        + " min=3.0 max=3.2 initial=3.1");
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        int leftMicros = data.consumeInt(1, 1_000_000);
        int rightMicros = data.consumeInt(1, 1_000_000);
        double left = leftMicros / 1_000_000.0;
        double right = rightMicros / 1_000_000.0;

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 1_000_000) / 1_000_000.0;
        double initial = min + (max - min) * fraction;

        double r1;
        double r2;
        BisectionSolver s1 = new BisectionSolver();
        BisectionSolver s2 = new BisectionSolver();

        try {
            r1 = s1.solve(f, min, max);
            r2 = s2.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 8.0;

        if (Math.abs(r1 - r2) > tol) {
            throw new RuntimeException(
                "[oracle:sibling-agreement] metamorphic violation: equivalent solve overloads disagree"
                    + " inputRoot=" + root
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " lhs=" + r1
                    + " rhs=" + r2);
        }

        if (Math.abs(r1 - root) > tol || Math.abs(r2 - root) > tol) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: for a bracket constructed around the exact root k*pi of sin(x), the solver result must recover that root within solver accuracy"
                    + " k=" + k
                    + " root=" + root
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " solve(f,min,max)=" + r1
                    + " solve(f,min,max,initial)=" + r2
                    + " tol=" + tol);
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
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("ConvergenceException")
                    || name.contains("NoBracketing")
                    || name.contains("Invalid")
                    || name.contains("OutOfRange")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}