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

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();
        try {
            double root = solver.solve(3624, issue631, 1.0, 10.0);
            if (Double.doubleToLongBits(root) == Double.doubleToLongBits(3.4341896575482003d)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-seed] semantic mismatch: expected TooManyEvaluationsException but solver returned root=" + root
                );
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-seed] semantic mismatch: expected TooManyEvaluationsException but solver returned unexpected root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
            // Expected on the fixed build for the exact lifted test case.
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        final double chosenRoot = data.consumeInt(-1000, 1000) / 16.0;
        double width = data.consumeInt(1, 1000) / 32.0;
        if (width <= 0.0) {
            width = 1.0;
        }
        final double min = chosenRoot - width;
        final double max = chosenRoot + width;
        final double startValue = data.consumeBoolean() ? min : max;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - chosenRoot;
            }
        };

        final RegulaFalsiSolver oracleSolver = new RegulaFalsiSolver();
        try {
            final double r1 = oracleSolver.solve(128, linear, min, max);
            final double r2 = oracleSolver.solve(128, linear, min, max, AllowedSolution.ANY_SIDE);
            final double r3 = oracleSolver.solve(128, linear, min, max, startValue);
            final double r4 = oracleSolver.solve(128, linear, min, max, startValue, AllowedSolution.ANY_SIDE);

            // Contract justification:
            // - BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution.
            // - The overloads differ only by explicit start value / allowed solution selection.
            // - For f(x)=x-c, c is the exact zero by construction, so every correct solve overload
            //   must return that same exact root on a valid bracketing interval.
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root] metamorphic violation: solve(maxEval,f,min,max) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r1 + " rhs=" + chosenRoot
                );
            }
            if (Double.doubleToLongBits(r2) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root-allowed] metamorphic violation: solve(maxEval,f,min,max,ANY_SIDE) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r2 + " rhs=" + chosenRoot
                );
            }
            if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root-start] metamorphic violation: solve(maxEval,f,min,max,startValue) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r3 + " rhs=" + chosenRoot
                );
            }
            if (Double.doubleToLongBits(r4) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root-start-allowed] metamorphic violation: solve(maxEval,f,min,max,startValue,ANY_SIDE) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r4 + " rhs=" + chosenRoot
                );
            }

            // State-coupling / overload-agreement justification:
            // constructors establish default allowed=ANY_SIDE, and explicit ANY_SIDE should agree
            // with the overloads that rely on that default. A "fix" that merely suppresses the
            // problematic path but corrupts/ignores allowed-solution bookkeeping would break this.
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: default ANY_SIDE overload disagrees with explicit ANY_SIDE input=" +
                    chosenRoot + " lhs=" + r1 + " rhs=" + r2
                );
            }
            if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(r4)) {
                throw new RuntimeException(
                    "[oracle:start-default-any-side] metamorphic violation: startValue overload disagrees with explicit ANY_SIDE input=" +
                    chosenRoot + " lhs=" + r3 + " rhs=" + r4
                );
            }
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r3)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagreed on exact linear root input=" +
                    chosenRoot + " lhs=" + r1 + " rhs=" + r3
                );
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final int fuzzMaxEval = data.consumeInt(1, 5000);
        final double fuzzShift = data.consumeInt(-1000, 1000) / 100.0;
        final double fuzzMin = Math.min(1.0 + fuzzShift, 10.0 + fuzzShift);
        final double fuzzMax = Math.max(1.0 + fuzzShift, 10.0 + fuzzShift);
        final double fuzzStart = data.consumeBoolean() ? fuzzMin : fuzzMax;
        final AllowedSolution[] allowedValues = AllowedSolution.values();
        final AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        try {
            new RegulaFalsiSolver().solve(fuzzMaxEval, issue631, fuzzMin, fuzzMax, fuzzStart, allowed);
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }
    }
}