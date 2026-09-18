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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double min = 1.0;
        final double max = 10.0;
        final int exactBudget = 3624;
        final double expectedRoot = 3.4341896575482003d;

        RegulaFalsiSolver exactSolver = new RegulaFalsiSolver();
        try {
            double root = exactSolver.solve(exactBudget, ISSUE631_FUNCTION, min, max);

            if (Math.abs(root - expectedRoot) > 1e-15) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-root] semantic mismatch: expected=" + expectedRoot +
                    " actual=" + root);
            }

            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-must-throw] semantic mismatch: exact lifted test input returned root=" +
                root + " evaluations=" + safeEvaluations(exactSolver) + " maxEval=" + exactBudget);
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable ignored) {
            return;
        }

        int extraBudget = data.consumeInt(3625, 5000);
        double startA = min + (max - min) * 0.25;
        double startB = min + (max - min) * 0.75;

        RegulaFalsiSolver solverA = new RegulaFalsiSolver();
        RegulaFalsiSolver solverB = new RegulaFalsiSolver();
        try {
            double a = solverA.solve(extraBudget, ISSUE631_FUNCTION, min, max, startA);
            double b = solverB.solve(extraBudget, ISSUE631_FUNCTION, min, max, startB);

            int evalA = safeEvaluations(solverA);
            int evalB = safeEvaluations(solverB);

            if (evalA > extraBudget) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:evaluation-budget-a] consistency violation: returned normally after exceeding budget evals=" +
                    evalA + " maxEval=" + extraBudget + " root=" + a);
            }
            if (evalB > extraBudget) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:evaluation-budget-b] consistency violation: returned normally after exceeding budget evals=" +
                    evalB + " maxEval=" + extraBudget + " root=" + b);
            }

            if (Math.abs(a - b) != 0.0d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:startvalue-ignored] metamorphic violation: BaseSecantSolver.doSolve reads getMin/getMax but not startValue, so two solve(maxEval,f,min,max,startValue) calls on fresh RegulaFalsiSolver instances must agree for the same function and interval; startA=" +
                    startA + " startB=" + startB + " lhs=" + a + " rhs=" + b +
                    " evalA=" + evalA + " evalB=" + evalB);
            }

            double rel = solverA.getRelativeAccuracy();
            double abs = solverA.getAbsoluteAccuracy();
            double fAtA = ISSUE631_FUNCTION.value(a);
            double closeEnough = Math.max(rel * Math.abs(a), abs);

            if (!(Math.abs(fAtA) <= solverA.getFunctionValueAccuracy() ||
                  Math.abs(max - min) < closeEnough ||
                  Math.abs(a - expectedRoot) <= closeEnough * 4.0)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:returned-root-consistency] consistency violation: normal return should be backed by the solver's own tolerance state; root=" +
                    a + " f(root)=" + fAtA + " rel=" + rel + " abs=" + abs +
                    " closeEnough=" + closeEnough);
            }
        } catch (TooManyEvaluationsException ignored) {
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable ignored) {
        }
    }

    private static int safeEvaluations(RegulaFalsiSolver solver) {
        try {
            return solver.getEvaluations();
        } catch (Throwable t) {
            return -1;
        }
    }
}