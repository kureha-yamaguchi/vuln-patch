package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input first.
        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double r1 = solver.solve(f, 3.0, 3.2, 3.1);
                double r2 = solver.solve(f, 3.0, 3.2);

                // Contract/oracle:
                // - The 4-arg overload with function should behave like the sibling overload on the same function/interval.
                // - For this concrete valid interval around the root of sin(x), the result should be near PI as in the original test.
                double acc = solver.getAbsoluteAccuracy();
                if (Double.isNaN(r1) || Math.abs(r1 - Math.PI) > acc) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: expected root near PI input=[3.0,3.2,3.1] result=" + r1 + " acc=" + acc);
                }
                if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > Math.max(acc, 1.0e-12)) {
                    throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: overloads disagree input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2 + " acc=" + acc);
                }
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                if (t.getClass().getName().startsWith("org.apache.commons.math")) {
                    return;
                }
                throw t;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            }
        }

        // EXPLORE: many valid-by-construction intervals around integer multiples of PI.
        int iters = 1 + Math.max(0, Math.min(8, data.remainingBytes()));
        for (int i = 0; i < iters; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            int leftMicros = data.consumeInt(1, 900000);
            int rightMicros = data.consumeInt(1, 900000);
            double left = leftMicros / 1000000.0;
            double right = rightMicros / 1000000.0;

            double min = root - left;
            double max = root + right;

            int posMicros = data.consumeInt(0, 1000000);
            double initial = min + (max - min) * (posMicros / 1000000.0);

            BisectionSolver solver = new BisectionSolver();
            try {
                double withInitial = solver.solve(f, min, max, initial);
                double withoutInitial = solver.solve(f, min, max);

                // Contract/oracle:
                // These same-name overloads are documented as solving the same function on the same interval;
                // the "initial" guess should not change the mathematical root returned for a valid bracketing interval.
                double acc = solver.getAbsoluteAccuracy();
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > Math.max(acc, 1.0e-12)) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + withoutInitial + " acc=" + acc);
                }

                // Input-known oracle: we constructed the interval to bracket the known root k*PI of sin(x).
                if (Math.abs(withInitial - root) > Math.max(acc, 1.0e-7)) {
                    throw new RuntimeException("[oracle:known-root] metamorphic violation: expected root near constructed multiple of PI input=[" + min + "," + max + "," + initial + "] expected=" + root + " actual=" + withInitial + " acc=" + acc);
                }
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    continue;
                }
                if (t.getClass().getName().startsWith("org.apache.commons.math")) {
                    continue;
                }
                throw t;
            } catch (MaxIterationsExceededException e) {
                continue;
            } catch (FunctionEvaluationException e) {
                continue;
            }
        }
    }
}