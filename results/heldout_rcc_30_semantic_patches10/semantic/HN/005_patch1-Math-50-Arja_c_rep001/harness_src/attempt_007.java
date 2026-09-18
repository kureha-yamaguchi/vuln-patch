package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final int maxEvalSeed = 3624;
        final double min = 1.0;
        final double max = 10.0;
        final double buggyRoot = 3.4341896575482003d;

        Throwable defaultOutcome = null;
        Throwable anySideOutcome = null;

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-default] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException e) {
            defaultOutcome = e;
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max, AllowedSolution.ANY_SIDE);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-anyside] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException e) {
            anySideOutcome = e;
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        if ((defaultOutcome == null) != (anySideOutcome == null)) {
            if (defaultOutcome != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-vs-anyside] semantic mismatch: BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution, so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree on rejection for the same deterministic input",
                    defaultOutcome
                );
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-anyside] semantic mismatch: BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution, so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree on rejection for the same deterministic input",
                anySideOutcome
            );
        }

        final int lowerMaxEval = data.consumeInt(1, maxEvalSeed);

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity] metamorphic violation: the same deterministic solve task rejected with TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " in the lifted test, so it cannot succeed with a smaller evaluation budget maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max, AllowedSolution.ANY_SIDE);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity-anyside] metamorphic violation: with explicit ANY_SIDE, the same deterministic solve task rejected with TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " in the lifted scenario, so it cannot succeed with a smaller evaluation budget maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }
    }
}