package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final UnivariateRealSolver issue631Solver = new RegulaFalsiSolver();
        try {
            final double root = issue631Solver.solve(3624, issue631, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exception] semantic mismatch: expected TooManyEvaluationsException but solve returned root="
                    + root + " expectedWrongValueOnBuggyBuild=3.4341896575482003");
        } catch (TooManyEvaluationsException expected) {
            // Correct patched behavior from the lifted test: this call must reject by exhausting evaluations.
        }

        final double absAcc = Math.pow(10.0, -data.consumeInt(4, 8));
        final double relAcc = Math.pow(10.0, -data.consumeInt(4, 8));
        final double fAcc = Math.pow(10.0, -data.consumeInt(4, 8));
        final int maxEval = data.consumeInt(16, 256);

        final RegulaFalsiSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = new RegulaFalsiSolver(absAcc);
                break;
            case 1:
                solver = new RegulaFalsiSolver(relAcc, absAcc);
                break;
            default:
                solver = new RegulaFalsiSolver(relAcc, absAcc, fAcc);
                break;
        }

        final UnivariateRealFunction sqrtTwo = new UnivariateRealFunction() {
            public double value(double x) {
                return x * x - 2.0;
            }
        };

        final AllowedSolution[] allowedValues = AllowedSolution.values();
        final AllowedSolution priorAllowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        try {
            solver.solve(maxEval, sqrtTwo, 0.0, 2.0, priorAllowed);
        } catch (Throwable t) {
            return;
        }

        final double implicitAny;
        try {
            implicitAny = solver.solve(maxEval, sqrtTwo, 0.0, 2.0);
        } catch (Throwable t) {
            return;
        }

        final double explicitAny;
        try {
            explicitAny = solver.solve(maxEval, sqrtTwo, 0.0, 2.0, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        final double tol = Math.max(Math.max(absAcc, relAcc * Math.abs(explicitAny)) * 8.0, 1e-12);
        // Contract justification:
        // BracketedUnivariateRealSolver says ANY_SIDE is the default allowed solution for backwards compatibility.
        // Therefore solve(maxEval,f,min,max) must agree with solve(maxEval,f,min,max,ANY_SIDE),
        // even after a prior call changed the receiver's internal "allowed" field. A patch that merely
        // suppresses/changes behavior in doSolve while leaving stale state can violate this observable equivalence.
        if (Math.abs(implicitAny - explicitAny) > tol) {
            throw new RuntimeException(
                "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must match solve(maxEval,f,min,max,ANY_SIDE)"
                    + " priorAllowed=" + priorAllowed
                    + " implicit=" + implicitAny
                    + " explicit=" + explicitAny
                    + " tol=" + tol);
        }
    }
}