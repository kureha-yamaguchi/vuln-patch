package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final double ISSUE631_ROOT = 3.4341896575482003d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            double root = new RegulaFalsiSolver().solve(3624, f, 1.0d, 10.0d);
            if (Double.doubleToLongBits(root) != Double.doubleToLongBits(ISSUE631_ROOT)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10), but call returned a different value root=" + root
                );
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10), but call returned root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        double leftDelta = 0.1d + (data.consumeInt(0, 4900) / 1000.0d);
        double rightDelta = 0.1d + (data.consumeInt(0, 4900) / 1000.0d);
        double min = ISSUE631_ROOT - leftDelta;
        double max = ISSUE631_ROOT + rightDelta;
        double selector = data.consumeInt(0, 10000) / 10000.0d;
        double start = min + (max - min) * selector;
        int maxEval = data.consumeInt(50, 5000);

        try {
            RegulaFalsiSolver explicitAny = new RegulaFalsiSolver();
            RegulaFalsiSolver implicitAny = new RegulaFalsiSolver();
            double lhs = explicitAny.solve(maxEval, f, min, max, start, AllowedSolution.ANY_SIDE);
            double rhs = implicitAny.solve(maxEval, f, min, max, start);
            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
                throw new RuntimeException(
                    "[oracle:any-side-overload] metamorphic violation: solve(maxEval,f,min,max,start,ANY_SIDE) must equal solve(maxEval,f,min,max,start) because the solver contract says ANY_SIDE is the default allowed solution; input=min=" +
                    min + ",max=" + max + ",start=" + start + ",maxEval=" + maxEval + " lhs=" + lhs + " rhs=" + rhs
                );
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:any-side-overload]")) {
                throw e;
            }
            return;
        }

        try {
            RegulaFalsiSolver mutated = new RegulaFalsiSolver();
            mutated.solve(maxEval, f, min, max, start, AllowedSolution.LEFT_SIDE);
            double afterMutation = mutated.solve(maxEval, f, min, max, start);
            double freshDefault = new RegulaFalsiSolver().solve(maxEval, f, min, max, start);
            if (Double.doubleToLongBits(afterMutation) != Double.doubleToLongBits(freshDefault)) {
                throw new RuntimeException(
                    "[oracle:default-reset] metamorphic violation: the no-AllowedSolution overload must still behave as ANY_SIDE even after a prior call changed solver state, because backwards compatibility requires ANY_SIDE as the default allowed solution; input=min=" +
                    min + ",max=" + max + ",start=" + start + ",maxEval=" + maxEval + " lhs=" + afterMutation + " rhs=" + freshDefault
                );
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:default-reset]")) {
                throw e;
            }
            return;
        }
    }
}