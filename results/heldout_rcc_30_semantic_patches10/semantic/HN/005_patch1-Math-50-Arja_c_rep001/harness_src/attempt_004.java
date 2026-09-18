package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final double root = solver.solve(3624, issue631, 1.0, 10.0);
            final double expectedBuggyRoot = 3.4341896575482003;
            if (Math.abs(root - expectedBuggyRoot) > 1e-15) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-returned] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but call returned unexpected value actual=" + root +
                    " expectedBuggyReturn=" + expectedBuggyRoot);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-returned] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but call returned actual=" + root +
                " expectedBuggyReturn=" + expectedBuggyRoot);
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        final int chosenMaxEval = data.consumeInt(20, 2000);
        final double relAcc = 1.0e-14;
        final double absAcc = 1.0e-14;

        final int coeff = data.consumeInt(-100, 100);
        final int offset = data.consumeInt(-100, 100);
        final double rootSeed = data.consumeInt(-1000, 1000) / 10.0;
        final double a = (coeff == 0 ? 1.0 : (double) coeff);
        final double b = (double) offset;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return a * (x - rootSeed) + b;
            }
        };

        final double exactRoot = rootSeed - (b / a);
        final double span = data.consumeInt(2, 100);
        double min = exactRoot - span;
        double max = exactRoot + span;
        if (!(min < max) || Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max)) {
            return;
        }

        try {
            final RegulaFalsiSolver mutated = new RegulaFalsiSolver(relAcc, absAcc);
            try {
                mutated.solve(chosenMaxEval, linear, min, max, AllowedSolution.LEFT_SIDE);
            } catch (Throwable ignored) {
                return;
            }

            final double defaultResult;
            final double explicitAnyResult;
            try {
                defaultResult = mutated.solve(chosenMaxEval, linear, min, max);
                explicitAnyResult = new RegulaFalsiSolver(relAcc, absAcc).solve(chosenMaxEval, linear, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable ignored) {
                return;
            }

            if (Double.doubleToLongBits(defaultResult) != Double.doubleToLongBits(explicitAnyResult)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must behave like solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution, and BaseSecantSolver constructors initialize that state to ANY_SIDE; a patch that silently keeps stale allowed state after a previous solve would break this relation inputRoot=" +
                    exactRoot + " min=" + min + " max=" + max + " lhs=" + defaultResult + " rhs=" + explicitAnyResult);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver(relAcc, absAcc);
            final double recovered = solver.solve(chosenMaxEval, linear, min, max, AllowedSolution.ANY_SIDE);
            if (Math.abs(recovered - exactRoot) > 1e-8) {
                throw new RuntimeException(
                    "[oracle:constructed-root] metamorphic violation: function was constructed from a known root, so solve must recover that root within solver tolerance inputRoot=" +
                    exactRoot + " min=" + min + " max=" + max + " lhs=" + recovered + " rhs=" + exactRoot);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}