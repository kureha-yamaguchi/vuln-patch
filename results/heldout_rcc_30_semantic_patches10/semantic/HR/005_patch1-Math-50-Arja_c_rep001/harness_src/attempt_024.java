package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final double EXPECTED_BUGGY_ROOT = 3.4341896575482003;
    private static final double ROOT_TOLERANCE = 1e-15;

    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        RegulaFalsiSolver solver = new RegulaFalsiSolver();

        try {
            // Exact reproduction of the real failing test. This reaches BaseSecantSolver.doSolve
            // through the public RegulaFalsiSolver API with a valid bracketing interval [1, 10].
            // On the buggy build, the removed REGULA_FALSI x == x1 adjustment lets the solver
            // return a root instead of exhausting the evaluation budget.
            double root = solver.solve(3624, ISSUE631_FUNCTION, 1.0, 10.0);
            if (Math.abs(root - EXPECTED_BUGGY_ROOT) <= ROOT_TOLERANCE) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-missing-throw] semantic mismatch: expected TooManyEvaluationsException from solver.solve(3624, f, 1, 10) but solver returned the known-buggy root " + root);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-missing-throw] semantic mismatch: expected TooManyEvaluationsException from solver.solve(3624, f, 1, 10) but solver returned " + root);
        } catch (TooManyEvaluationsException expectedOnFixed) {
            // Fixed behavior: continue to an independent consistency oracle.

            int bonusEval = data.consumeInt(1, 2048);
            int successfulMaxEval = 3624 + bonusEval;

            double altMin = data.consumeInt(-20, 3);
            double altMax = data.consumeInt(4, 20);
            if (!(altMin < EXPECTED_BUGGY_ROOT && EXPECTED_BUGGY_ROOT < altMax)) {
                altMin = 0.0;
                altMax = 8.0;
            }

            RegulaFalsiSolver solverA = new RegulaFalsiSolver();
            RegulaFalsiSolver solverB = new RegulaFalsiSolver();

            double rootA;
            double rootB;
            try {
                // Contract justification: the function exp(x) - pi^3 is continuous and strictly
                // increasing, so it has exactly one zero. Any two successful solves on valid
                // bracketing intervals for the same function must converge to that same unique root.
                rootA = solverA.solve(successfulMaxEval, ISSUE631_FUNCTION, 1.0, 10.0);
                rootB = solverB.solve(successfulMaxEval, ISSUE631_FUNCTION, altMin, altMax);
            } catch (RuntimeException ex) {
                return;
            }

            if (Math.abs(rootA - rootB) > 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:unique-root-cross-bracket] metamorphic violation: same strictly-increasing function solved on two valid brackets returned different roots rootA=" +
                    rootA + " rootB=" + rootB + " maxEval=" + successfulMaxEval +
                    " intervalA=[1.0,10.0] intervalB=[" + altMin + "," + altMax + "]", expectedOnFixed);
            }
        }
    }
}