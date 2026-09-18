package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        exerciseScenario(f, 3.0, 3.2, 3.1, Math.PI, true);

        int n = data.consumeInt(-1000, 1000);
        double expectedRoot = n * Math.PI;

        int deltaMicros = data.consumeInt(1, 1_000_000);
        double delta = deltaMicros / 1_000_000.0;
        if (delta >= (Math.PI / 2.0)) {
            delta = (Math.PI / 2.0) - 1e-6;
        }
        if (delta <= 0.0) {
            delta = 1e-6;
        }

        double min = expectedRoot - delta;
        double max = expectedRoot + delta;

        int initialMicros = data.consumeInt(-deltaMicros, deltaMicros);
        double initial = expectedRoot + (initialMicros / 1_000_000.0);
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        exerciseScenario(f, min, max, initial, expectedRoot, true);
    }

    private static void exerciseScenario(UnivariateRealFunction f, double min, double max, double initial,
                                         double expectedRoot, boolean validByConstruction) {
        BisectionSolver solverWithInitial = new BisectionSolver();
        double withInitial;
        try {
            withInitial = solverWithInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        BisectionSolver solverWithoutInitial = new BisectionSolver();
        double withoutInitial;
        try {
            withoutInitial = solverWithoutInitial.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle: for the constructed interval [n*pi-delta, n*pi+delta] with 0 < delta < pi/2,
        // sin(x) has the known root n*pi in the interval, so a correct solve call must recover that root.
        double tol1 = Math.max(solverWithInitial.getAbsoluteAccuracy() * 4.0, 1e-8);
        if (Double.isNaN(withInitial) || Double.isInfinite(withInitial)
                || Math.abs(withInitial - expectedRoot) > tol1) {
            throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed unique sin root not recovered input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + expectedRoot);
        }

        // Sibling-agreement oracle: solve(f,min,max,initial) and solve(f,min,max) are same-operation overloads;
        // on this valid bracket around a unique root they must agree within solver accuracy.
        double tol2 = Math.max(Math.max(solverWithInitial.getAbsoluteAccuracy(),
                                        solverWithoutInitial.getAbsoluteAccuracy()) * 4.0, 1e-8);
        if (Double.isNaN(withoutInitial) || Double.isInfinite(withoutInitial)
                || Math.abs(withInitial - withoutInitial) > tol2) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagree input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}