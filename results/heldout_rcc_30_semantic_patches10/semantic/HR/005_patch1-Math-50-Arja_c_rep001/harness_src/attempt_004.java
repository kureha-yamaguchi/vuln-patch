package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final double root = solver.solve(3624, issue631Function, 1.0, 10.0);
            if (Double.doubleToLongBits(root) == Double.doubleToLongBits(3.4341896575482003)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-return] semantic mismatch: expected TooManyEvaluationsException but call returned root=" + root
                );
            }
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
        }

        final double root1 = data.consumeInt(-1000, 1000) / 10.0;
        final double root2 = data.consumeInt(-1000, 1000) / 10.0;
        final int maxEval = data.consumeInt(3, 50);

        final UnivariateRealFunction firstFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return x - root1;
            }
        };
        final UnivariateRealFunction secondFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return x - root2;
            }
        };

        final double min1 = root1 - 1.0;
        final double max1 = root1 + 1.0;
        final double min2 = root2 - 1.0;
        final double max2 = root2 + 1.0;

        try {
            final RegulaFalsiSolver reused = new RegulaFalsiSolver();
            reused.solve(maxEval, firstFunction, min1, max1, AllowedSolution.RIGHT_SIDE);
            final double reusedResult = reused.solve(maxEval, secondFunction, min2, max2, AllowedSolution.ANY_SIDE);

            final RegulaFalsiSolver fresh = new RegulaFalsiSolver();
            final double freshResult = fresh.solve(maxEval, secondFunction, min2, max2, AllowedSolution.ANY_SIDE);

            /* Contract justification: solve(...) takes the function, interval, maxEval and allowedSolution
               as explicit arguments, so a prior solve must not change the result of a later equivalent solve.
               Cross-checking a reused solver against a fresh identically-constructed solver is a sound
               consistency check on hidden state reset (allowed/search interval/evaluation bookkeeping). */
            if (Double.doubleToLongBits(reusedResult) != Double.doubleToLongBits(freshResult)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:state-reset] consistency violation: reusedResult=" + reusedResult +
                    " freshResult=" + freshResult +
                    " root2=" + root2 +
                    " maxEval=" + maxEval
                );
            }

            if (Double.doubleToLongBits(freshResult) != Double.doubleToLongBits(root2)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:constructed-root] semantic mismatch: expected=" + root2 +
                    " actual=" + freshResult +
                    " min2=" + min2 +
                    " max2=" + max2
                );
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
        }
    }
}