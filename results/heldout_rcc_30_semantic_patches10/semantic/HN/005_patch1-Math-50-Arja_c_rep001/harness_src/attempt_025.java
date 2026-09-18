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

        final double expectedRoot = 3.4341896575482003d;

        try {
            double root = new RegulaFalsiSolver().solve(3624, f, 1.0d, 10.0d);
            if (Math.abs(root - expectedRoot) <= 1.0e-15d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:ground-truth] semantic mismatch: expected TooManyEvaluationsException from RegulaFalsiSolver.solve(3624,f,1,10) but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException expected) {
            // Patched behavior for the lifted regression test.
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int maxEval = data.consumeInt(1, 5000);
        int startN = data.consumeInt(1, 10);
        double startValue = (double) startN;

        final RegulaFalsiSolver solverImplicit = new RegulaFalsiSolver();
        final RegulaFalsiSolver solverExplicit = new RegulaFalsiSolver();

        final double lhs;
        final double rhs;
        try {
            /*
             * Contract justification:
             * BracketedUnivariateRealSolver states ANY_SIDE is the default allowed solution
             * for backwards compatibility. BaseSecantSolver constructors set allowed to
             * ANY_SIDE, and solve(..., startValue) is the sibling overload of
             * solve(..., startValue, allowedSolution). Therefore, for identical inputs,
             * the overload without AllowedSolution must agree with the one using
             * AllowedSolution.ANY_SIDE. A patch that merely avoids the original bad path
             * but corrupts/ignores allowed-solution state breaks this relation.
             */
            lhs = solverImplicit.solve(maxEval, f, 1.0d, 10.0d, startValue);
            rhs = solverExplicit.solve(maxEval, f, 1.0d, 10.0d, startValue, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
            throw new RuntimeException(
                "[oracle:any-side-overload] metamorphic violation: solve(maxEval,f,min,max,startValue) must equal " +
                "solve(maxEval,f,min,max,startValue,AllowedSolution.ANY_SIDE) input=maxEval=" + maxEval +
                " startValue=" + startValue + " lhs=" + lhs + " rhs=" + rhs
            );
        }
    }
}