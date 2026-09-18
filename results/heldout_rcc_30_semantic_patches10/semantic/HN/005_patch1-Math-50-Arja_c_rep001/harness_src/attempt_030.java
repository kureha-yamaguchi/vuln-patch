package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final double root = solver.solve(3624, issue631Function, 1, 10);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exception] semantic mismatch: expected TooManyEvaluationsException but returned root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
            // Lifted directly from RegulaFalsiSolverTest.testIssue631:
            // for this exact function, interval, and maxEval, the real public API must throw.
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }

        final int chosenRootInt = data.consumeInt(-1000000, 1000000);
        final double expectedRoot = chosenRootInt;
        final double min = expectedRoot - 1.0;
        final double max = expectedRoot + 1.0;
        final double start = data.consumeBoolean() ? expectedRoot - 0.5 : expectedRoot + 0.5;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - expectedRoot;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final double rDefault;
        final double rAllowed;
        final double rStartAllowed;
        final double rStartDefault;
        try {
            rDefault = solver.solve(32, linear, min, max);
            rAllowed = solver.solve(32, linear, min, max, AllowedSolution.ANY_SIDE);
            rStartAllowed = solver.solve(32, linear, min, max, start, AllowedSolution.ANY_SIDE);
            rStartDefault = solver.solve(32, linear, min, max, start);
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }

        // Contract justification:
        // - Bracketed solvers solve for "a value where the function is zero".
        // - Here the input is constructed from a known answer: f(x)=x-expectedRoot, bracketed by [expectedRoot-1, expectedRoot+1].
        //   Any correct solver must return exactly expectedRoot.
        // - BracketedUnivariateRealSolver documents ANY_SIDE as the backwards-compatible default, so the overloads with
        //   implicit default and explicit AllowedSolution.ANY_SIDE must agree.
        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(expectedRoot)
                || Double.doubleToLongBits(rAllowed) != Double.doubleToLongBits(expectedRoot)
                || Double.doubleToLongBits(rStartAllowed) != Double.doubleToLongBits(expectedRoot)
                || Double.doubleToLongBits(rStartDefault) != Double.doubleToLongBits(expectedRoot)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:linear-root] semantic mismatch: expectedRoot=" + expectedRoot
                    + " rDefault=" + rDefault
                    + " rAllowed=" + rAllowed
                    + " rStartAllowed=" + rStartAllowed
                    + " rStartDefault=" + rStartDefault
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAllowed)
                || Double.doubleToLongBits(rStartAllowed) != Double.doubleToLongBits(rStartDefault)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: implicit ANY_SIDE overload disagrees with explicit ANY_SIDE"
                    + " expectedRoot=" + expectedRoot
                    + " rDefault=" + rDefault
                    + " rAllowed=" + rAllowed
                    + " rStartAllowed=" + rStartAllowed
                    + " rStartDefault=" + rStartDefault
            );
        }
    }
}