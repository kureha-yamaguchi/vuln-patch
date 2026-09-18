package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();
        org.apache.commons.math.analysis.UnivariateRealFunction f = new SinFunction();

        try {
            double anchor = solver.solve(f, 3.0, 3.2, 3.1);
            double expected = Math.PI;
            double tol = solver.getAbsoluteAccuracy();
            if (Double.isNaN(anchor) || Math.abs(anchor - expected) > tol) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: documented bisection solve on SinFunction over [3.0,3.2] should find the root near PI input=[3.0,3.2,3.1] lhs=" + anchor + " rhs=" + expected);
            }

            /*
             * Contract/oracle:
             * The same-name overloads
             *   solve(f, min, max, initial)
             *   solve(f, min, max)
             * are documented to agree where their docs match. For BisectionSolver,
             * on a valid bracketing interval and non-null function, both should solve
             * the same equation on the same interval and return the same root.
             * A throw-deleting/branch-skipping patch that ignores the function or
             * otherwise changes behaviour can violate this even without crashing.
             */
            double anchor2 = solver.solve(f, 3.0, 3.2);
            if (Double.isNaN(anchor2) || Math.abs(anchor - anchor2) > solver.getAbsoluteAccuracy()) {
                throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: equivalent overloads disagree input=[3.0,3.2,3.1] lhs=" + anchor + " rhs=" + anchor2);
            }
        } catch (Throwable t) {
            boolean inSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inSolve = true;
                        break;
                    }
                }
            }

            if (t instanceof NullPointerException && inSolve) {
                throw (NullPointerException) t;
            }

            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.apache.commons.math.FunctionEvaluationException
                    || t instanceof org.apache.commons.math.MaxIterationsExceededException
                    || t instanceof RuntimeException) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                return;
            }
            return;
        }

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;

        double leftWidth = 0.05 + (data.consumeInt(0, 5000) / 10000.0);
        double rightWidth = 0.05 + (data.consumeInt(0, 5000) / 10000.0);

        double min = center - leftWidth;
        double max = center + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            double frac = data.consumeInt(0, 10000) / 10000.0;
            initial = min + frac * (max - min);
        } else {
            initial = center;
        }

        if (!(initial >= min && initial <= max)) {
            initial = center;
        }

        BisectionSolver solver1 = new BisectionSolver();
        BisectionSolver solver2 = new BisectionSolver();

        double rWithInitial;
        try {
            rWithInitial = solver1.solve(f, min, max, initial);
        } catch (Throwable t) {
            boolean inSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inSolve = true;
                        break;
                    }
                }
            }

            if (t instanceof NullPointerException && inSolve) {
                throw (NullPointerException) t;
            }

            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.apache.commons.math.FunctionEvaluationException
                    || t instanceof org.apache.commons.math.MaxIterationsExceededException) {
                return;
            }
            if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                    && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            return;
        }

        double rNoInitial;
        try {
            rNoInitial = solver2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(solver1.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy());

        if (Double.isNaN(rWithInitial) || Double.isInfinite(rWithInitial) || Double.isNaN(rNoInitial) || Double.isInfinite(rNoInitial)) {
            throw new RuntimeException("[oracle:finite-result] metamorphic violation: valid bracketing interval around a sine root should produce finite roots input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
        }

        if (Math.abs(rWithInitial - rNoInitial) > tol) {
            throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent overloads disagree input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
        }

        double target = center;
        if (Math.abs(rWithInitial - target) > Math.max(tol, 1e-6) || Math.abs(rNoInitial - target) > Math.max(tol, 1e-6)) {
            throw new RuntimeException("[oracle:recover-known-root] metamorphic violation: interval was constructed to bracket the known sin root at k*pi input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
        }
    }
}