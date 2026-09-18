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

        final RegulaFalsiSolver liftedSolver = new RegulaFalsiSolver();

        try {
            final double root = liftedSolver.solve(3624, f, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException for solver.solve(3624,f,1,10) but got root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        double absAcc = data.consumeBoolean() ? 1.0e-6 : (1.0e-12 + (Math.abs((double) data.consumeInt()) % 1000000.0) * 1.0e-12);
        double relAcc = 1.0e-14 + (Math.abs((double) data.consumeInt()) % 1000000.0) * 1.0e-15;
        double fnAcc = 1.0e-15 + (Math.abs((double) data.consumeInt()) % 1000000.0) * 1.0e-16;

        final int lowSlack = data.consumeInt(0, 2);
        final int highSlack = data.consumeInt(0, 6);
        final double min = 1.0 + lowSlack;
        final double max = 4.0 + highSlack;
        if (!(min < 3.4341896575482003 && 3.4341896575482003 < max)) {
            return;
        }

        final int maxEval = data.consumeInt(4000, 12000);

        final RegulaFalsiSolver solverA = new RegulaFalsiSolver(absAcc);
        final RegulaFalsiSolver solverB = new RegulaFalsiSolver(relAcc, absAcc);
        final RegulaFalsiSolver solverC = new RegulaFalsiSolver(relAcc, absAcc, fnAcc);

        checkAnySideDefaultAgreement(solverA, f, min, max, maxEval, "default-anyside-abs");
        checkAnySideDefaultAgreement(solverB, f, min, max, maxEval, "default-anyside-relabs");
        checkAnySideDefaultAgreement(solverC, f, min, max, maxEval, "default-anyside-relabsfn");
    }

    private static void checkAnySideDefaultAgreement(RegulaFalsiSolver solver,
                                                     UnivariateRealFunction f,
                                                     double min,
                                                     double max,
                                                     int maxEval,
                                                     String oracleId) {
        final double lhs;
        try {
            lhs = solver.solve(maxEval, f, min, max);
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            return;
        }

        final double rhs;
        try {
            /* Contract cited from BracketedUnivariateRealSolver javadoc and BaseSecantSolver constructors:
               backwards compatibility requires ANY_SIDE as the default allowed solution, so solve(maxEval,f,min,max)
               must agree with solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE). A patch that merely suppresses
               throwing or corrupts the shared 'allowed' state violates this observable post-condition. */
            rhs = solver.solve(maxEval, f, min, max, AllowedSolution.ANY_SIDE);
        } catch (RuntimeException ex) { /*__vpRepair*/ if (ex instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ex;
            return;
        }

        if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: default ANY_SIDE overload disagreement input=min=" +
                min + ",max=" + max + ",maxEval=" + maxEval + " lhs=" + lhs + " rhs=" + rhs
            );
        }
    }
}