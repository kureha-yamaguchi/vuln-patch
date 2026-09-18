package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int delta = 1;
        if (data.remainingBytes() > 0) {
            delta = data.consumeInt(1, 32);
        }
        final int maxEval = 3624 - delta;

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        try {
            final double root = solver.solve(maxEval, ISSUE631_FUNCTION, 1.0, 10.0);

            /*
             * Trusted oracle:
             * The pinned upstream test proves that this deterministic solver/function/interval
             * configuration needs MORE than 3624 evaluations and must therefore throw
             * TooManyEvaluationsException at 3624. Since computeObjectiveValue increments the
             * evaluation count on every call, any strictly smaller maxEval must also throw:
             * reducing the budget cannot turn an over-budget run into a successful solve.
             *
             * This is a boundary flip around the seed budget and is independent from merely
             * re-checking the exact pinned 3624 input.
             */
            throw new FuzzerSecurityIssueLow(
                "[oracle:budget-monotonicity] semantic mismatch: solve returned for smaller-than-pinned evaluation budget "
                    + "maxEval=" + maxEval
                    + " root=" + root);
        } catch (TooManyEvaluationsException expected) {
            return;
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            return;
        }
    }
}